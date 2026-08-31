"""
Layer 1 wire-validation for the Branch Android SDK.

Parses branchlogs.txt (captured during the L1 instrumented run), extracts each
wire request, and asserts the SDK is emitting every device/SDK field that must
be on the wire. Presence-only check — a missing field fails the run; field
contents are not type-checked.

On success the validator prints the full payload for every captured request
plus a per-field check table so reviewers can verify what actually went over
the wire — no more silent passes when a value is wrong.

Source of truth for the parser: the BranchLogger verbose sink emits paired
lines for every wire request just before HTTP send:

    posting to https://api2.branch.io/v1/install
    Post value = {"hardware_id":"...","sdk":"android5.21.1",...}
"""

import argparse
import json
import os
import sys
from urllib.parse import urlparse

POSTING_PREFIX = "posting to "
POST_VALUE_PREFIX = "Post value = "

# Required on every captured /v1/* request. Lookup tolerates v2 user_data
# nesting (Android emits device fields top-level on v1, nested under
# user_data on /v2/event/*; only /v1/* is in L1 scope today).
REQUIRED_COMMON = [
    "branch_key",
    "sdk",
    "branch_sdk_request_timestamp",
    "branch_sdk_request_unique_id",
    "brand",
    "model",
    "os",
    "os_version",
    "country",
    "language",
    "local_ip",
    "screen_dpi",
    "screen_height",
    "screen_width",
    "wifi",
    "ui_mode",
]

# hardware_id is deliberately not common. ServerRequestCreateUrl builds the
# payload and then removes anon_id, is_hardware_id_real and hardware_id before
# sending, so requiring it everywhere fails /v1/url on a correct SDK. Whether
# that removal is right is EMT-4199, open with the server team: iOS sends all
# three on the same endpoint, on both the beta and the release line. Until that
# is answered this layer asserts the field only where both platforms agree it
# belongs, rather than encoding one side of an undecided question.

# Endpoint-specific additions on top of REQUIRED_COMMON.
# connection_type is only emitted on init/event requests, so /v1/url
# (a CreateUrl request) legitimately lacks it.
# An endpoint absent from this table has no L1 contract yet: its payload is printed but nothing is
# asserted. The beta does not emit /v1/install or /v1/open, but this validator also gates master
# PRs, where both are the live init path — so their contracts stay. An endpoint simply not present
# in a capture is not an error; what each scenario must emit belongs in a per-scenario contract.
# The beta's two-request open flow: /v3/deeplink resolves the link, /v3/events/open attributes.
# They share a contract because they carry the same device block; measured on 6.0.0-beta.0, these
# three plus REQUIRED_COMMON are present in every occurrence of both, with and without a resolved
# link.
#
# Deliberately not required:
#   android_app_link_url  — only when the resolution was driven by a URL.
#   link_data             — only on an open that follows a resolved link.
#   randomized_device_token / randomized_bundle_token — the backend issues these once a device is
#     known, so a first-ever open carries neither. iOS excludes them for the same reason.
#   connection_type       — in the iOS common list, absent from both /v3 payloads here.
REQUIRED_V3_SESSION = ["anon_id", "first_install_time", "is_hardware_id_real"]

# An endpoint absent from this table has no L1 contract yet: its payload is printed but nothing is
# asserted. The beta does not emit /v1/install or /v1/open, but this validator also gates master
# PRs, where both are the live init path — so their contracts stay. An endpoint simply not present
# in a capture is not an error; what each scenario must emit belongs in a per-scenario contract.
#
# /v2/event/* is still uncontracted. Its schema nests the device block under user_data, which
# lookup_field already resolves, but it carries no `wifi` — a REQUIRED_COMMON field — so a v2
# contract cannot be expressed by extending the common list. iOS gives v2 its own complete list
# instead. That mechanism change is out of scope here.
REQUIRED_PER_ENDPOINT = {
    "/v1/install": ["connection_type", "is_hardware_id_real", "first_install_time", "hardware_id"],
    "/v1/open": ["connection_type", "randomized_device_token", "randomized_bundle_token", "hardware_id"],
    "/v1/url": [],
    "/v3/deeplink": REQUIRED_V3_SESSION,
    "/v3/events/open": REQUIRED_V3_SESSION,
}


def parse_branch_logs(file_path):
    """Walk branchlogs.txt and pair each `posting to <url>` with the next
    `Post value = {...}`. Returns list of {uri, url, request}, or None when
    the file is missing.
    """
    if not os.path.exists(file_path):
        print(f"Error: Log file not found at {file_path}")
        return None

    entries = []
    pending_url = None

    with open(file_path, "r", encoding="utf-8", errors="replace") as f:
        for line_no, raw in enumerate(f, start=1):
            line = raw.rstrip("\n")

            if line.startswith(POSTING_PREFIX):
                pending_url = line[len(POSTING_PREFIX):].strip()
                continue

            if line.startswith(POST_VALUE_PREFIX):
                if pending_url is None:
                    print(
                        f"Warning: line {line_no}: 'Post value =' with no "
                        f"preceding 'posting to <url>'. Skipping."
                    )
                    continue

                payload_str = line[len(POST_VALUE_PREFIX):].strip()
                try:
                    payload = json.loads(payload_str)
                except json.JSONDecodeError as e:
                    print(
                        f"Warning: line {line_no}: failed to parse JSON "
                        f"after 'Post value = ': {e}"
                    )
                    pending_url = None
                    continue

                try:
                    path = urlparse(pending_url).path or pending_url
                except Exception:
                    path = pending_url

                entries.append({"uri": path, "url": pending_url, "request": payload})
                pending_url = None

    return entries


def lookup_field(request, field):
    """Return value at top-level, else under user_data. Used so the validator
    keeps working if a future endpoint nests device fields under user_data."""
    if field in request:
        return request[field]
    user_data = request.get("user_data")
    if isinstance(user_data, dict) and field in user_data:
        return user_data[field]
    return None


def is_present(value):
    """A field is considered present when it has a non-null, non-empty value."""
    if value is None:
        return False
    if isinstance(value, str) and value == "":
        return False
    return True


def validate_request(entry, idx, total):
    """Print the full payload + per-field table for one request. Return a
    list of error strings (empty when everything required is present).

    Required-field checks apply to any endpoint with an entry in
    REQUIRED_PER_ENDPOINT. The beta emits `/v3/deeplink` and
    `/v3/events/open`, which the `/v1/*` scope skipped silently. An
    endpoint absent from the table has no L1 contract yet; the validator
    dumps its payload for visibility but does not fail the run."""
    errors = []
    uri = entry["uri"]
    url = entry["url"]
    request = entry["request"]

    print()
    print("=" * 64)
    print(f"[{idx}/{total}] {uri} — POST {url}")
    print("=" * 64)

    if not isinstance(request, dict):
        errors.append(f"Request {idx} ({uri}): payload is not a JSON object")
        return errors

    print("Full payload:")
    print(json.dumps(request, indent=2, sort_keys=True))
    print()

    if uri not in REQUIRED_PER_ENDPOINT:
        print(f"(No L1 contract for this endpoint; required-field checks skipped)")
        return errors

    fields = REQUIRED_COMMON + REQUIRED_PER_ENDPOINT[uri]
    print(f"Required fields ({len(fields)}):")
    for field in fields:
        value = lookup_field(request, field)
        present = is_present(value)
        marker = "✓" if present else "✗"
        if present:
            print(f"  {marker} {field:<35} {value}")
        else:
            print(f"  {marker} {field:<35} MISSING")
            errors.append(f"Request {idx} ({uri}): missing required field '{field}'")

    return errors


# Set at construction and re-sent unchanged on every attempt, so a repeated
# value marks a retry rather than a second logical request.
REQUEST_ID_FIELD = "branch_sdk_request_unique_id"


def collapse_retries(entries):
    """Drop retry attempts so a capture holds one entry per logical request.

    The capture line is written in `BranchRemoteInterface.makeRestfulPost`
    (`posting to` / `Post value =`), which runs once per attempt, so a flaky
    network inflates every count. That is the opposite of what exact counts
    are for, and this runner currently fails with socket timeouts, which is
    precisely the condition that produces retries.

    iOS collapses on `retryNumber`. That field does not work here: it is
    stamped in `BranchAsyncNetworkLayer`, while the captured line comes from
    the legacy interface, so no payload in a real capture carries it —
    verified against the capture from run 32502951452, 0 of 8 requests.
    `branch_sdk_request_unique_id` is fixed at construction and re-sent
    unchanged, so a repeat of it is a retry.

    Entries without the field are kept: a request that predates EMT-4198's
    stamping is not silently dropped."""
    seen = set()
    kept = []
    for entry in entries:
        request = entry.get("request")
        request_id = request.get(REQUEST_ID_FIELD) if isinstance(request, dict) else None
        if isinstance(request_id, str) and request_id:
            if request_id in seen:
                continue
            seen.add(request_id)
        kept.append(entry)
    return kept


# What the wire must look like after a scenario ran. All endpoint names live
# here rather than in the checks, so the same checks serve this line's capture
# and the iOS one.
#
#   counts  endpoint -> exact number of requests. 0 forbids the endpoint.
#           An endpoint absent from counts is unconstrained.
#   order   (earlier, later) pairs. Relative, not adjacency: a request
#           between the two does not violate it.
#   fields  endpoint -> field -> exact number of that endpoint's requests
#           carrying the field. Same counting as `counts`, one level down;
#           0 forbids. Presence only, never a value comparison.
#           It exists because an endpoint count cannot see a request changing
#           character: on 6.0.0-beta.0 the install is a /v3/events/open like
#           any other, so a first install and a launch on an installed device
#           put the same endpoints on the wire in the same order.
#
# Ported from the iOS line, where the same engine gates 4.0.0-beta.0. Kept
# byte-compatible on purpose: a contract that reads differently per platform
# is a parity gap wearing a helper's clothes.
SCENARIO_CONTRACTS = {
    # Not a test-plan scenario. This is what the harness drives today: one run
    # that resolves two links, creates one, and fires a custom event. The plan
    # scenarios (C1, C3, N1) need one capture each and the runner is not
    # producing those yet, so contracting them would mean writing from the
    # ticket text rather than from a measurement.
    #
    # Measured from run 32502951452 on 6.0.0-beta.0. It earns its place by
    # pinning the shape the gate sees now: if the harness or the SDK changes
    # what a run emits, this goes red and someone looks.
    #
    # hardware_id at 0 on /v1/url is the measured fact, not an omission.
    # ServerRequestCreateUrl removes it, identically on beta and master, while
    # iOS sends it on the same endpoint. Which platform is right is EMT-4199,
    # open with the server team. Asserting the absence means the gate turns red
    # the moment Android's behaviour changes, which a comment naming the ticket
    # would not do.
    "harness": {
        "counts": {
            "/v3/deeplink": 2,
            "/v3/events/open": 4,
            "/v1/url": 1,
            "/v2/event/custom": 1,
        },
        "order": (("/v3/deeplink", "/v3/events/open"),),
        "fields": {"/v1/url": {"hardware_id": 0}},
    },
}


class UnknownScenario(Exception):
    """Raised for a scenario name with no contract."""


def contract_for(scenario):
    """Return the contract for `scenario`, or raise UnknownScenario."""
    try:
        return SCENARIO_CONTRACTS[scenario]
    except KeyError:
        known = ", ".join(sorted(SCENARIO_CONTRACTS)) or "(none defined yet)"
        raise UnknownScenario(
            f"No contract for scenario '{scenario}'. Known scenarios: {known}"
        )


def occurs_after(uris, earlier, later):
    """True when some `later` request appears after some `earlier` one.

    Relative, not adjacency: unrelated traffic between the two does not
    violate it. Deliberately not "the first `later` follows the first
    `earlier`" — a launch open legitimately precedes a link resolution, so
    that reading would fail a correct capture.

    Fail-closed: if either endpoint is missing the order is not satisfied."""
    for index, uri in enumerate(uris):
        if uri == earlier and later in uris[index + 1:]:
            return True
    return False


def assert_contract(entries, contract):
    """Check a normalized capture against a scenario contract.

    Returns a list of error strings, empty when the capture satisfies it.
    Holds no endpoint name of its own: every value compared comes from the
    contract, so the same checks serve either platform's capture."""
    errors = []
    uris = [entry["uri"] for entry in entries]

    for endpoint, expected in sorted(contract["counts"].items()):
        actual = uris.count(endpoint)
        if actual == expected:
            continue
        if expected == 0:
            errors.append(
                f"'{endpoint}' must not be captured for this scenario, "
                f"but appeared {actual} time(s)."
            )
        else:
            errors.append(
                f"Expected {expected} '{endpoint}' request(s), captured {actual}."
            )

    for earlier, later in contract["order"]:
        if not occurs_after(uris, earlier, later):
            errors.append(f"Expected a '{later}' request after a '{earlier}' one.")

    for endpoint, fields in sorted(contract.get("fields", {}).items()):
        matching = [e for e in entries if e["uri"] == endpoint]
        for field, expected in sorted(fields.items()):
            actual = sum(
                1 for e in matching if is_present(lookup_field(e["request"], field))
            )
            if actual == expected:
                continue
            if expected == 0:
                errors.append(
                    f"No '{endpoint}' request may carry '{field}', "
                    f"but {actual} of {len(matching)} did."
                )
            else:
                errors.append(
                    f"Expected {expected} of the '{endpoint}' request(s) to carry "
                    f"'{field}', but {actual} of {len(matching)} did."
                )

    return errors


def validate_entries(entries, contract=None):
    """Check every request's required fields, and the capture against
    `contract` when one is given. Returns aggregated errors."""
    errors = []

    if not entries:
        errors.append("No Branch SDK wire requests were captured in the logs.")
        return errors

    print(f"Captured {len(entries)} Branch wire requests. Validating...")

    # No endpoint is mandatory. A correct beta capture contains no install at all: on
    # 6.0.0-beta.0 the first launch resolves through requestDeepLinkData and the open goes to
    # /v3/events/open, so requiring /v1/install failed every correct run. What each scenario must
    # emit belongs in a per-scenario contract, not in a global rule.

    if contract is not None:
        errors.extend(assert_contract(collapse_retries(entries), contract))

    for i, entry in enumerate(entries, start=1):
        errors.extend(validate_request(entry, i, len(entries)))

    return errors


def main():
    parser = argparse.ArgumentParser(description="Validate a Branch SDK wire capture.")
    parser.add_argument(
        "log_file", nargs="?", default="branchlogs.txt",
        help="capture to validate (default: branchlogs.txt)",
    )
    parser.add_argument(
        "--scenario", default=None,
        help="which scenario produced this capture; selects its contract. "
             "Omitted, only the per-request required fields are asserted.",
    )
    args = parser.parse_args()
    log_file_path = args.log_file

    entries = parse_branch_logs(log_file_path)

    if entries is None:
        print("\n--- VALIDATION FAILED ---")
        print(f"FAILED: Log file not found at {log_file_path}")
        sys.exit(1)

    try:
        if os.path.getsize(log_file_path) == 0:
            print("\n--- VALIDATION FAILED ---")
            print("FAILED: Log file is empty; no Branch SDK wire requests were captured.")
            sys.exit(1)
    except OSError:
        pass

    try:
        contract = contract_for(args.scenario) if args.scenario else None
    except UnknownScenario as e:
        print("\n--- VALIDATION FAILED ---")
        print(f"FAILED: {e}")
        sys.exit(1)

    errors = validate_entries(entries, contract)

    if errors:
        print("\n--- VALIDATION FAILED ---")
        for err in errors:
            print(f"FAILED: {err}")
        sys.exit(1)

    print(f"\n--- VALIDATION PASSED ({len(entries)}/{len(entries)} requests valid) ---")
    sys.exit(0)


if __name__ == "__main__":
    main()
