#!/usr/bin/env python3
"""
verify_play_integrity.py — Gateway-side POC for Android Play Integrity verification.

Simulates exactly what the Branch backend would do after receiving a v1/install request
that contains a play_integrity_token, nonce, and ecdh_public_key.

Usage:
    # Option 1: Provide response body (easiest)
    python3 verify_play_integrity.py \
        --response-body '{"play_integrity_token":"...","nonce":"...","ecdh_public_key":"...","branch_key":"...",...}' \
        --package "io.branch.branchandroidtestbed" \
        --credentials "/path/to/service-account.json"

    # Option 2: Provide individual fields
    python3 verify_play_integrity.py \
        --token   "<play_integrity_token>" \
        --nonce   "<base64_random_nonce_from_sdk>" \
        --ecdh    "<base64_ecdh_public_key_from_sdk>" \
        --package "io.branch.branchandroidtestbed" \
        --credentials "/path/to/service-account.json" \
        --body    '{"branch_key":"key_live_...","os":"Android",...}'

Dependencies:
    pip3 install google-auth
"""

import argparse
import base64
import hashlib
import json
import sys
import time
from urllib import request as urllib_request
from urllib.error import HTTPError

try:
    from google.auth.transport.requests import Request
    from google.oauth2 import service_account
except ImportError:
    print("❌ Missing required dependency: google-auth")
    print("   Install with: pip3 install google-auth")
    sys.exit(1)


# ── Canonical query string ─────────────────────────────────────────────────────
# Must match Android SDK: sorted keys, "key=value&key=value" format

def canonical_query_string(body: dict) -> str:
    """
    Mirrors AppAttestation.canonicalQueryString() — sorted keys, key=value& format.

    For nested objects (dicts, lists), uses JSON serialization to match Android's
    JSONObject.toString() behavior.
    """
    def serialize_value(v):
        if isinstance(v, (dict, list)):
            # Use JSON serialization for nested objects (no spaces, sorted keys for dicts)
            return json.dumps(v, separators=(',', ':'), sort_keys=True)
        elif isinstance(v, bool):
            # JSON boolean lowercase (true/false), not Python (True/False)
            return 'true' if v else 'false'
        elif v is None:
            return 'null'
        else:
            return str(v)

    return "&".join(f"{k}={serialize_value(v)}" for k, v in sorted(body.items()))


# ── Nonce re-derivation ────────────────────────────────────────────────────────
# SHA-256(canonical_utf8 || ecdh_pub_bytes || random_nonce_bytes)
# Then base64url-encode (no padding) — this is what was sent to Play Integrity.

def derive_expected_nonce(body_without_pi_fields: dict, ecdh_pub_b64: str, random_nonce_b64: str) -> str:
    canonical = canonical_query_string(body_without_pi_fields).encode("utf-8")
    ecdh_bytes = base64.b64decode(ecdh_pub_b64)
    nonce_bytes = base64.b64decode(random_nonce_b64)

    digest = hashlib.sha256(canonical + ecdh_bytes + nonce_bytes).digest()
    return base64.urlsafe_b64encode(digest).rstrip(b"=").decode("ascii")


# ── Google Play Integrity API call ─────────────────────────────────────────────

def decode_integrity_token(package_name: str, token: str, credentials_path: str) -> dict:
    """
    Call Play Integrity API to decrypt the token.
    Requires service account credentials (API keys are not supported).
    """
    print(f"  📄 Loading credentials from: {credentials_path}")

    # Load service account credentials
    credentials = service_account.Credentials.from_service_account_file(
        credentials_path,
        scopes=['https://www.googleapis.com/auth/playintegrity']
    )

    print(f"  🔑 Service account: {credentials.service_account_email}")
    print(f"  🌐 Project ID: {credentials.project_id}")
    print(f"  📦 Package name: {package_name}")
    print(f"  🔐 Scope: https://www.googleapis.com/auth/playintegrity")

    # Get access token
    print(f"  🔄 Requesting OAuth2 access token...")
    credentials.refresh(Request())
    access_token = credentials.token
    print(f"  ✅ Access token obtained (expires in ~3600s)")
    print(f"  🎫 Token preview: {access_token[:50]}...")

    # Call API
    url = f"https://playintegrity.googleapis.com/v1/{package_name}:decodeIntegrityToken"
    payload = json.dumps({"integrity_token": token}).encode("utf-8")

    print(f"  📡 Calling Play Integrity API...")
    print(f"  🔗 URL: {url}")
    print(f"  📊 Token length: {len(token)} chars")

    req = urllib_request.Request(
        url,
        data=payload,
        headers={
            "Content-Type": "application/json",
            "Authorization": f"Bearer {access_token}"
        },
        method="POST"
    )
    try:
        with urllib_request.urlopen(req, timeout=15) as resp:
            print(f"  ✅ API call successful (HTTP {resp.status})")
            return json.loads(resp.read())
    except HTTPError as e:
        body = e.read().decode("utf-8")
        print(f"\n❌ Google API error {e.code}: {body}")
        print(f"\n💡 Troubleshooting:")
        print(f"   - Verify service account is in project: {credentials.project_id}")
        print(f"   - Check that Play Integrity API is enabled")
        print(f"   - Ensure app is linked to this GCP project in Play Console")
        print(f"   - Wait 2-3 minutes after granting permissions")
        sys.exit(1)


# ── Verification ───────────────────────────────────────────────────────────────

FRESHNESS_WINDOW_MS = 10 * 60 * 1000  # 10 minutes

PI_FIELDS = {"play_integrity_token", "nonce", "ecdh_public_key"}  # strip before re-deriving


def verify(decoded: dict, package_name: str, expected_nonce_b64url: str) -> dict:
    payload = decoded.get("tokenPayloadExternal", {})
    request_details  = payload.get("requestDetails", {})
    app_integrity    = payload.get("appIntegrity", {})
    device_integrity = payload.get("deviceIntegrity", {})
    account_details  = payload.get("accountDetails", {})

    token_nonce   = request_details.get("nonce", "")
    token_package = request_details.get("requestPackageName", "")
    token_ts_ms   = int(request_details.get("timestampMillis", 0))
    age_ms        = int(time.time() * 1000) - token_ts_ms
    device_verdicts = device_integrity.get("deviceRecognitionVerdict", [])

    nonce_ok    = token_nonce == expected_nonce_b64url
    package_ok  = token_package == package_name
    fresh_ok    = 0 <= age_ms <= FRESHNESS_WINDOW_MS
    integrity_ok = any(v in device_verdicts for v in
                       ("MEETS_DEVICE_INTEGRITY", "MEETS_STRONG_INTEGRITY"))

    return {
        "passed": nonce_ok and package_ok and fresh_ok and integrity_ok,
        "checks": {
            "nonce_binding":    {"ok": nonce_ok,    "expected": expected_nonce_b64url, "got": token_nonce},
            "package_name":     {"ok": package_ok,  "expected": package_name,          "got": token_package},
            "freshness":        {"ok": fresh_ok,     "age_seconds": age_ms // 1000,    "limit_seconds": FRESHNESS_WINDOW_MS // 1000},
            "device_integrity": {"ok": integrity_ok, "verdicts": device_verdicts},
        },
        "info": {
            "app_recognition_verdict": app_integrity.get("appRecognitionVerdict", ""),
            "app_licensing_verdict":   account_details.get("appLicensingVerdict", ""),
            "app_version_code":        app_integrity.get("versionCode", ""),
        }
    }


# ── Response body parsing ──────────────────────────────────────────────────────

def parse_response_body(response_body: dict) -> dict:
    """
    Parse Play Integrity fields from response body.

    Expected fields:
      - play_integrity_token: encrypted token from Play Integrity API
      - nonce: base64 random nonce
      - ecdh_public_key: base64 ECDH public key

    Returns dict with parsed fields or raises error if missing.
    """
    required_fields = ["play_integrity_token", "nonce", "ecdh_public_key"]

    missing = [f for f in required_fields if f not in response_body]
    if missing:
        print(f"❌ Missing required fields in response body: {', '.join(missing)}")
        print(f"\n💡 Response body should contain:")
        print(f"   • play_integrity_token: encrypted token from Play Integrity API")
        print(f"   • nonce: base64 random nonce")
        print(f"   • ecdh_public_key: base64 ECDH public key")
        sys.exit(1)

    return {
        "token": response_body["play_integrity_token"],
        "nonce": response_body["nonce"],
        "ecdh": response_body["ecdh_public_key"]
    }


# ── Main ───────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(
        description="Gateway-side Play Integrity POC verifier",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Use response body (easiest):
  python3 verify_play_integrity.py --response-body '{"play_integrity_token":"...","nonce":"...","ecdh_public_key":"...",...}' --package "..." --credentials "..."

  # Use individual fields:
  python3 verify_play_integrity.py --token "..." --nonce "..." --ecdh "..." --package "..." --credentials "..." --body '{...}'
"""
    )

    parser.add_argument("--response-body", help="JSON response body containing all Play Integrity fields")
    parser.add_argument("--token",       help="play_integrity_token from SDK")
    parser.add_argument("--nonce",       help="base64 random nonce from SDK (nonce field)")
    parser.add_argument("--ecdh",        help="base64 ECDH public key from SDK (ecdh_public_key field)")
    parser.add_argument("--package",     required=True, help="App package name")
    parser.add_argument("--credentials", required=True, help="Path to service account JSON credentials")
    parser.add_argument("--body",        default="{}",  help="JSON request body (without PI fields) for nonce re-derivation")
    args = parser.parse_args()

    print("\n" + "="*70)
    print("  Play Integrity Verification - Google API Mode")
    print("="*70)

    # Parse inputs
    if args.response_body:
        print("\n── Parsing response body ────────────────────────────────────────")
        response_body = json.loads(args.response_body)

        parsed = parse_response_body(response_body)
        token = parsed["token"]
        nonce = parsed["nonce"]
        ecdh = parsed["ecdh"]

        # Use response body as request body (will strip PI fields)
        body = response_body

        print(f"  ✅ Token: {token[:50]}...")
        print(f"  ✅ Nonce: {nonce[:32]}...")
        print(f"  ✅ ECDH pub: {ecdh[:32]}...")
    else:
        # Individual arguments
        if not all([args.token, args.nonce, args.ecdh]):
            print("❌ Error: Either provide --response-body OR all of: --token, --nonce, --ecdh")
            sys.exit(1)

        token = args.token
        nonce = args.nonce
        ecdh = args.ecdh
        body = json.loads(args.body)

    # Strip Play Integrity fields that were appended after nonce was computed
    body_clean = {k: v for k, v in body.items() if k not in PI_FIELDS}

    print("\n── Step 1: Re-derive expected nonce ─────────────────────────────")
    expected_nonce = derive_expected_nonce(body_clean, ecdh, nonce)
    print(f"  Canonical string: {canonical_query_string(body_clean)[:120]}...")
    print(f"  Expected nonce (base64url): {expected_nonce}")

    print("\n── Step 2: Call Google decodeIntegrityToken API ─────────────────")
    decoded = decode_integrity_token(args.package, token, args.credentials)
    print("\n  ✅ Token decrypted successfully!")
    print("\n  📋 Raw response:")
    print(json.dumps(decoded, indent=4))

    print("\n── Step 3: Verify payload ───────────────────────────────────────")
    result = verify(decoded, args.package, expected_nonce)

    for check, detail in result["checks"].items():
        status = "✅" if detail["ok"] else "❌"
        print(f"  {status} {check}: {detail}")

    print("\n── Info fields ──────────────────────────────────────────────────")
    for k, v in result["info"].items():
        print(f"  {k}: {v}")

    overall = "✅ PASSED" if result["passed"] else "❌ FAILED"
    print(f"\n── Overall result: {overall} ─────────────────────────────────────")
    print("="*70 + "\n")
    sys.exit(0 if result["passed"] else 1)


if __name__ == "__main__":
    main()
