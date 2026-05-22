"""
Layer 1 wire-validation for the Branch Android SDK.

Parses branchlogs.txt (captured during the L1 instrumented run), extracts each
wire request, and asserts the SDK is emitting every device/SDK field that must
be on the wire. Presence-only check — a missing field fails the run; field
contents are not type-checked.

On success the validator prints the full payload for every captured request
(with sensitive values masked) plus a per-field check table so reviewers can
verify what actually went over the wire — no more silent passes when a value
is wrong.

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
    "hardware_id",
]

# Endpoint-specific additions on top of REQUIRED_COMMON.
#
# connection_type lives in /v1/install + /v1/open only — Android's
# `DeviceInfo.java:115` adds it inside `if (serverRequest.isInitializationOrEventRequest())`
# so the field is intentionally absent from `/v1/url` (a CreateUrl request,
# not an init/event one). Validating it on /v1/url would surface as a false
# positive against a real CI capture.
REQUIRED_PER_ENDPOINT = {
    "/v1/install": ["connection_type", "is_hardware_id_real", "first_install_time"],
    "/v1/open": ["connection_type", "randomized_device_token", "randomized_bundle_token"],
    "/v1/url": [],
}

# Fields whose values must never appear unredacted in CI logs.
SENSITIVE_FIELDS = {
    "branch_key",
    "hardware_id",
    "randomized_device_token",
    "randomized_bundle_token",
    "device_fingerprint_id",
    "google_advertising_id",
    "idfa",
    "idfv",
    "anon_id",
    "developer_identity",
    "identity",
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


def mask_payload(payload):
    """Deep copy with values for SENSITIVE_FIELDS replaced by ***MASKED***."""
    masked = {}
    for key, value in payload.items():
        if key in SENSITIVE_FIELDS and is_present(value):
            masked[key] = "***MASKED***"
        elif isinstance(value, dict):
            masked[key] = mask_payload(value)
        else:
            masked[key] = value
    return masked


def display_value(field, value):
    """Return the value as it should appear in the per-field table."""
    if field in SENSITIVE_FIELDS and is_present(value):
        return "***MASKED***"
    return value


def validate_request(entry, idx, total):
    """Print the full payload + per-field table for one request. Return a
    list of error strings (empty when everything required is present).

    Required-field checks are scoped to `/v1/*` endpoints — that's the L1
    contract. Non-v1 endpoints (e.g. `/v2/event/*`) use a different schema
    (device fields under `user_data`, different identity fields) and are
    out of L1's enforcement scope; the validator still dumps their payload
    for visibility but does not fail the run."""
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

    masked = mask_payload(request)
    print("Full payload (sensitive values masked):")
    print(json.dumps(masked, indent=2, sort_keys=True))
    print()

    if not uri.startswith("/v1/"):
        print(f"(Non-v1 endpoint; required-field checks skipped per L1 scope)")
        return errors

    fields = REQUIRED_COMMON + REQUIRED_PER_ENDPOINT.get(uri, [])
    print(f"Required fields ({len(fields)}):")
    for field in fields:
        value = lookup_field(request, field)
        present = is_present(value)
        marker = "✓" if present else "✗"
        if present:
            print(f"  {marker} {field:<35} {display_value(field, value)}")
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

    found_paths = [e["uri"] for e in entries]
    if "/v1/install" not in found_paths:
        errors.append("Mandatory endpoint '/v1/install' was not captured.")

    if "/v1/open" not in found_paths:
        print(
            "Note: '/v1/open' not present in capture. Expected in a normal "
            "install+open flow, but not enforced here."
        )

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
