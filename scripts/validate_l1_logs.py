"""
Validates Branch SDK L1 wire payloads captured from branchlogs.txt.

Source of truth: the BranchLogger verbose sink emits paired lines for every
wire request just before HTTP send:

    posting to https://api2.branch.io/v1/install
    Post value = {"hardware_id":"...","sdk":"android5.21.1","branch_key":"key_test_...",...}

The "Post value =" JSON is the exact payload sent on the wire (after the SDK's
internal addCommonParams step, which injects `sdk` and `branch_key`).

We previously parsed `IBranchRequestTracingCallback`'s "URI Sent to Branch:"
blocks, but that callback fires on a pre-wire snapshot and does NOT include
`sdk` / `branch_key`. This script switches to the BranchLogger output so we
validate what actually leaves the device.
"""

import json
import os
import re
import sys
from urllib.parse import urlparse


POSTING_PREFIX = "posting to "
POST_VALUE_PREFIX = "Post value = "


def parse_branch_logs(file_path):
    """
    Walks branchlogs.txt line by line. Tracks the most recent `posting to <url>`
    line and, when a `Post value = {...}` line follows, pairs them into an
    entry containing the endpoint path and parsed JSON payload.

    Defensive: skips (with a warning) any `Post value =` that has no preceding
    URL in scope, and any malformed JSON. Never crashes on a single bad block.
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

                # Tolerant extraction: take everything after the prefix to EOL.
                payload_str = line[len(POST_VALUE_PREFIX):].strip()
                try:
                    payload = json.loads(payload_str)
                except json.JSONDecodeError as e:
                    print(
                        f"Warning: line {line_no}: failed to parse JSON after "
                        f"'Post value = ': {e}. Raw: {payload_str[:200]}..."
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


def _is_str(v):
    return isinstance(v, str) and len(v) > 0


def _is_bool(v):
    return isinstance(v, bool)


def _is_number(v):
    # Exclude bool (which is a subclass of int in Python).
    return isinstance(v, (int, float)) and not isinstance(v, bool)


def validate_entries(entries):
    """
    Validates the captured wire payloads. Returns a list of error strings;
    empty list = success.
    """
    errors = []

    if entries is None:
        errors.append("Log file could not be read.")
        return errors

    if not entries:
        errors.append("No Branch SDK wire requests were captured in the logs.")
        return errors

    print(f"Captured {len(entries)} Branch wire requests. Validating...")

    found_paths = [e["uri"] for e in entries]

    # /v1/install is mandatory. /v1/open is expected but not strictly required
    # here — some test flows may exercise install-only. If /v1/open is present,
    # it's validated below; if absent, we only note it.
    if "/v1/install" not in found_paths:
        errors.append("Mandatory endpoint '/v1/install' was not captured.")

    if "/v1/open" not in found_paths:
        print(
            "Note: '/v1/open' not present in capture. Expected in a normal "
            "install+open flow, but not enforced here."
        )

    for i, entry in enumerate(entries, start=1):
        uri = entry["uri"]
        req = entry["request"]

        print(f"[{i}] Validating {uri}...")

        if not isinstance(req, dict):
            errors.append(f"Request {i} ({uri}): payload is not a JSON object")
            continue

        # Common required fields on every wire request.
        sdk = req.get("sdk")
        if not _is_str(sdk):
            errors.append(f"Request {i} ({uri}): missing or invalid 'sdk' (got {sdk!r})")
        # We don't pin the SDK version, but a sanity check on the prefix is cheap.
        elif not sdk.startswith("android"):
            errors.append(
                f"Request {i} ({uri}): 'sdk' should start with 'android' (got {sdk!r})"
            )

        branch_key = req.get("branch_key")
        if not _is_str(branch_key):
            errors.append(
                f"Request {i} ({uri}): missing or invalid 'branch_key' "
                f"(got {branch_key!r})"
            )
        elif not (branch_key.startswith("key_test_") or branch_key.startswith("key_live_")):
            errors.append(
                f"Request {i} ({uri}): 'branch_key' must start with 'key_test_' "
                f"or 'key_live_' (got {branch_key!r})"
            )

        hardware_id = req.get("hardware_id")
        if not _is_str(hardware_id):
            errors.append(
                f"Request {i} ({uri}): missing or invalid 'hardware_id' "
                f"(got {hardware_id!r})"
            )

        # Endpoint-specific required fields.
        if uri == "/v1/install":
            if not _is_bool(req.get("is_hardware_id_real")):
                errors.append(
                    f"Request {i} (/v1/install): missing or non-boolean "
                    f"'is_hardware_id_real'"
                )
            if not _is_number(req.get("first_install_time")):
                errors.append(
                    f"Request {i} (/v1/install): missing or non-numeric "
                    f"'first_install_time'"
                )

        if uri == "/v1/open":
            if not _is_str(req.get("randomized_device_token")):
                errors.append(
                    f"Request {i} (/v1/open): missing or invalid "
                    f"'randomized_device_token'"
                )
            if not _is_str(req.get("randomized_bundle_token")):
                errors.append(
                    f"Request {i} (/v1/open): missing or invalid "
                    f"'randomized_bundle_token'"
                )

    return errors


def main():
    log_file_path = sys.argv[1] if len(sys.argv) > 1 else "branchlogs.txt"

    entries = parse_branch_logs(log_file_path)

    if entries is None:
        # parse_branch_logs already printed the error.
        print("\n--- VALIDATION FAILED ---")
        print(f"FAILED: Log file not found at {log_file_path}")
        sys.exit(1)

    # Distinguish empty file (file exists but no captures) from missing file.
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

    print("\n--- VALIDATION PASSED ---")
    print(f"Summary: {len(entries)} wire request(s) captured:")
    for entry in entries:
        print(f"  - {entry['uri']}")
    sys.exit(0)


if __name__ == "__main__":
    main()
