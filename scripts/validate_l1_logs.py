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

    posting to https://api2.branch.io/v3/events/open
    Post value = {"hardware_id":"...","sdk":"android5.21.1",...}

Also asserts the Secure SDK's `branch_sdk_secure_context` block on the init
request (EMT-4096). Presence and mutual exclusivity only — no signature or
attestation is verified here; see tools/verify_android_key_attestation.py.
"""

import json
import os
import re
import sys
from urllib.parse import urlparse

POSTING_PREFIX = "posting to "
POST_VALUE_PREFIX = "Post value = "

# BranchLogger splits any message over MAX_LOG_CHUNK (3500) chars into
# `[chunk i/n] <slice>` lines. It does this *before* choosing a sink
# (BranchLogger.kt platformLog), so branchlogs.txt written through
# Branch.enableLogging(callback, ...) is chunked exactly like logcat is —
# this is not a logcat-only artifact. A `/v3/events/open` carrying an
# attestation cert chain runs ~6 KB, so the init request is precisely the
# one that splits, and reassembling it is what makes the L1 gate work at
# all on the Secure SDK line.
#
# platformLog repeats the message's leading `[...]` run on every chunk, but
# the two wire lines start with plain text, so there is nothing to repeat and
# the marker leads the line. Anchored accordingly.
CHUNK_RE = re.compile(r"\[chunk (\d+)/(\d+)\]\s?")

# The init request. Every session starts here, fresh install included —
# ServerRequestRegisterInstall (/v1/install) is no longer used for init, so a
# capture lacking this endpoint means initialization never went out.
MANDATORY_ENDPOINT = "/v3/events/open"

# Endpoints whose required fields are enforced. /v3/events/open qualifies because
# RequestOpen does not override getBranchRemoteAPIVersion(), so it uses the same
# top-level V1 param layout. /v3/events/standard and /custom come from
# ServerRequestLogEvent, which *is* V2 (device fields under user_data) — a
# different schema, tracked separately.
ENFORCED_PREFIXES = ("/v1/", "/v3/events/open")

# Endpoints that carry branch_sdk_secure_context. These are the ones that opt into
# signing by overriding ServerRequest.applySecureContext: RequestOpen,
# ServerRequestLogEvent, ServerRequestGetLATD and ServerRequestCreateUrl.
#
# Separate from ENFORCED_PREFIXES on purpose. Device/SDK field checks are the L1
# contract and only apply where that schema holds; the secure context is checked
# wherever it is sent, whatever the surrounding payload looks like.
SIGNED_ENDPOINTS = (
    "/v3/events/open",
    "/v3/events/standard",
    "/v3/events/custom",
    "/v1/url",
    "/v1/cpid/latd",
)

# branch_sdk_secure_context keys — mirror of SecureContextFields in the secure SDK.
SECURE_CONTEXT = "branch_sdk_secure_context"
CONTEXT_KEY = "context_key"
INITIALIZATION_CONTEXT = "initialization_context"
ACTIVITY_CONTEXT = "activity_context"

# Layer 1. Exactly one attestation form appears: attestation_object on a device
# with hardware Key Attestation support, play_integrity_token otherwise.
INIT_CONTEXT_REQUIRED = ["client_public_key", "challenge"]
ATTESTATION_FORMS = ["attestation_object", "play_integrity_token"]

# Layer 2 + 3, carried by every request once the device is registered.
ACTIVITY_CONTEXT_REQUIRED = ["request_signature", "nonce"]

# Required on every enforced request. Lookup tolerates user_data nesting
# (device fields are top-level on /v1/* and /v3/events/open, nested under
# user_data on /v3/events/standard and /custom).
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
# connection_type is only emitted on init/event requests, so /v1/url
# (a CreateUrl request) legitimately lacks it.
REQUIRED_PER_ENDPOINT = {
    "/v1/install": ["connection_type", "is_hardware_id_real", "first_install_time"],
    "/v1/open": ["connection_type", "randomized_device_token", "randomized_bundle_token"],
    "/v1/url": [],
    # Serves both a fresh install and a repeat open, so only the fields
    # ServerRequestInitSession always writes are required. randomized_device_token
    # and randomized_bundle_token are deliberately absent: the server mints them in
    # the open *response* for a fresh install.
    #
    # connection_type is NOT required here despite being required on /v1/open.
    # Verified against a real capture: v3/events/open does not carry it.
    "/v3/events/open": ["is_hardware_id_real", "first_install_time"],
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
    chunk_buf = None
    chunk_next = 0
    chunk_total = 0

    with open(file_path, "r", encoding="utf-8", errors="replace") as f:
        for line_no, raw in enumerate(f, start=1):
            line = raw.rstrip("\n")

            # Reassemble a chunked message before matching anything on it.
            # The `[chunk i/n] ` marker is dropped and the slices are
            # concatenated in order, reproducing the original message byte
            # for byte.
            chunk = CHUNK_RE.match(line)
            if chunk:
                index, total = int(chunk.group(1)), int(chunk.group(2))
                body = line[chunk.end():]

                if index == 1:
                    chunk_buf, chunk_next, chunk_total = body, 2, total
                elif chunk_buf is not None and index == chunk_next and total == chunk_total:
                    chunk_buf += body
                    chunk_next += 1
                else:
                    # Out of order or interleaved with another message's
                    # chunks. Dropping beats concatenating the wrong slices
                    # into JSON that parses but describes no real request.
                    print(
                        f"Warning: line {line_no}: unexpected chunk "
                        f"{index}/{total}; discarding partial message."
                    )
                    chunk_buf, chunk_next, chunk_total = None, 0, 0
                    continue

                if index < chunk_total:
                    continue

                line, chunk_buf, chunk_next, chunk_total = chunk_buf, None, 0, 0

            # BranchRemoteInterface emits both wire lines unprefixed, and
            # CustomBranchApp writes the message verbatim, so the marker is
            # at the start of the line. Anything ahead of it means the format
            # changed — fail loudly rather than parse a capture as zero
            # requests, which reads identically to an SDK that sent nothing.
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


def validate_secure_context(request, idx, uri, require_initialization):
    """Check the `branch_sdk_secure_context` block. Presence and mutual
    exclusivity only — nothing here verifies a signature or an attestation.

    `require_initialization` is set for the first /v3/events/open in the capture:
    that is the one request that must carry Layer 1. Later requests carry
    activity_context instead, and the two are never sent together."""
    errors = []
    ctx = request.get(SECURE_CONTEXT)

    if not isinstance(ctx, dict):
        if require_initialization:
            errors.append(
                f"Request {idx} ({uri}): missing '{SECURE_CONTEXT}' — the init "
                f"request must carry the device attestation"
            )
        else:
            print(f"  (no {SECURE_CONTEXT}; not required on this request)")
        return errors

    print(f"{SECURE_CONTEXT}:")

    present = is_present(ctx.get(CONTEXT_KEY))
    print(f"  {'✓' if present else '✗'} {CONTEXT_KEY:<33} "
          f"{ctx.get(CONTEXT_KEY) if present else 'MISSING'}")
    if not present:
        errors.append(f"Request {idx} ({uri}): missing '{SECURE_CONTEXT}.{CONTEXT_KEY}'")

    init = ctx.get(INITIALIZATION_CONTEXT)
    activity = ctx.get(ACTIVITY_CONTEXT)

    if isinstance(init, dict) and isinstance(activity, dict):
        errors.append(
            f"Request {idx} ({uri}): carries both {INITIALIZATION_CONTEXT} and "
            f"{ACTIVITY_CONTEXT}; exactly one is sent per request"
        )

    if require_initialization and not isinstance(init, dict):
        errors.append(
            f"Request {idx} ({uri}): missing '{INITIALIZATION_CONTEXT}' on the "
            f"init request"
        )

    if isinstance(init, dict):
        errors.extend(_check_block(init, INIT_CONTEXT_REQUIRED, idx, uri,
                                   INITIALIZATION_CONTEXT))
        forms = [f for f in ATTESTATION_FORMS if is_present(init.get(f))]
        for f in ATTESTATION_FORMS:
            hit = f in forms
            print(f"  {'✓' if hit else '·'} {INITIALIZATION_CONTEXT}.{f:<12} "
                  f"{'present' if hit else 'absent'}")
        if len(forms) != 1:
            errors.append(
                f"Request {idx} ({uri}): expected exactly one of "
                f"{'/'.join(ATTESTATION_FORMS)}, found {len(forms)}"
            )

    if isinstance(activity, dict):
        errors.extend(_check_block(activity, ACTIVITY_CONTEXT_REQUIRED, idx, uri,
                                   ACTIVITY_CONTEXT))

    return errors


def _check_block(block, required, idx, uri, label):
    """Presence-check `required` keys inside one secure-context sub-block."""
    errors = []
    for field in required:
        value = block.get(field)
        ok = is_present(value)
        shown = value if ok else "MISSING"
        if isinstance(shown, str) and len(shown) > 40:
            shown = shown[:37] + "..."
        print(f"  {'✓' if ok else '✗'} {label}.{field:<20} {shown}")
        if not ok:
            errors.append(f"Request {idx} ({uri}): missing '{label}.{field}'")
    return errors


def validate_request(entry, idx, total, require_initialization=False):
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

    print("Full payload:")
    print(json.dumps(request, indent=2, sort_keys=True))
    print()

    if uri.startswith(ENFORCED_PREFIXES):
        fields = REQUIRED_COMMON + REQUIRED_PER_ENDPOINT.get(uri, [])
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
        print()
    else:
        print("(Device/SDK field checks skipped: outside the L1 schema)")

    if uri in SIGNED_ENDPOINTS:
        errors.extend(validate_secure_context(request, idx, uri, require_initialization))
    else:
        print("(Unsigned endpoint; no secure context expected)")

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
    if MANDATORY_ENDPOINT not in found_paths:
        errors.append(f"Mandatory endpoint '{MANDATORY_ENDPOINT}' was not captured.")

    if "/v1/install" in found_paths:
        errors.append(
            "'/v1/install' was captured. Init moved to "
            f"'{MANDATORY_ENDPOINT}' for fresh installs too, so this endpoint "
            "should no longer appear."
        )

    # Layer 1 is only expected on a fresh install. A device that is already
    # registered sends activity_context on its opens instead, which is correct and
    # must not fail the run. A fresh install is identifiable from the request alone:
    # it has no randomized_bundle_token, because the server mints that in the open
    # response.
    first_init = next(
        (i for i, e in enumerate(entries, start=1)
         if e["uri"] == MANDATORY_ENDPOINT
         and isinstance(e["request"], dict)
         and "randomized_bundle_token" not in e["request"]),
        None,
    )
    if first_init is None:
        print(
            "Note: no fresh-install open in this capture (every open carries a "
            "randomized_bundle_token), so initialization_context is not required. "
            "Reinstall the app to capture Layer 1."
        )

    for i, entry in enumerate(entries, start=1):
        errors.extend(
            validate_request(entry, i, len(entries), require_initialization=(i == first_init))
        )

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
