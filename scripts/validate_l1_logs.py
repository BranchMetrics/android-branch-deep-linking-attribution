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


def validate_entries(entries):
    """Run validate_request on every entry plus the top-level
    /v1/install-must-be-present check. Returns aggregated errors."""
    errors = []

    if not entries:
        errors.append("No Branch SDK wire requests were captured in the logs.")
        return errors

    print(f"Captured {len(entries)} Branch wire requests. Validating...")

    # No endpoint is mandatory. A correct beta capture contains no install at all: on
    # 6.0.0-beta.0 the first launch resolves through requestDeepLinkData and the open goes to
    # /v3/events/open, so requiring /v1/install failed every correct run. What each scenario must
    # emit belongs in a per-scenario contract, not in a global rule.

    for i, entry in enumerate(entries, start=1):
        errors.extend(validate_request(entry, i, len(entries)))

    return errors


def main():
    log_file_path = sys.argv[1] if len(sys.argv) > 1 else "branchlogs.txt"

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

    errors = validate_entries(entries)

    if errors:
        print("\n--- VALIDATION FAILED ---")
        for err in errors:
            print(f"FAILED: {err}")
        sys.exit(1)

    print(f"\n--- VALIDATION PASSED ({len(entries)}/{len(entries)} requests valid) ---")
    sys.exit(0)


if __name__ == "__main__":
    main()
