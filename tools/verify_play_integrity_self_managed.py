#!/usr/bin/env python3
"""
verify_play_integrity_self_managed.py — Local Play Integrity verification with self-managed keys.

This tool decrypts Play Integrity tokens LOCALLY using your downloaded encryption keys,
avoiding Google API calls and quota constraints.

Setup (following Google's official instructions):
1. Generate RSA key pair (2048-bit as required by Google):
   openssl genrsa -aes128 -out private.pem 2048
   openssl rsa -in private.pem -pubout > public.pem

2. Upload public.pem to Play Console:
   Play Console → App → Test and release → App integrity → Play Integrity API → Settings
   → Classic requests → Response encryption → Edit → "Manage and download"

3. Download the encrypted key bundle from Play Console (e.g., api_keys.enc)
   DO NOT decrypt it manually - this script will decrypt it for you.

Encryption Flow:
1. Play Console encrypts an AES-256 key with your RSA public key → api_keys.enc
2. Play Integrity tokens have CEK (Content Encryption Key) wrapped with that AES-256 key (A256KW)
3. This script:
   a. Decrypts api_keys.enc with your RSA private key → gets AES-256 key
   b. Unwraps CEK from token using AES-256 key
   c. Decrypts token payload using CEK

Usage:
    # Option 1: With pre-decrypted api_keys.txt (easiest - recommended)
    python3 verify_play_integrity_self_managed.py \
        --response-body '{"play_integrity_token":"...","nonce":"...","ecdh_public_key":"...",...}' \
        --package "io.branch.branchandroidtestbed" \
        --private-key "/path/to/private.pem" \
        --decryption-key "/path/to/api_keys.txt"

    # Option 2: With encrypted .enc file (will decrypt with RSA)
    python3 verify_play_integrity_self_managed.py \
        --response-body '...' \
        --package "io.branch.branchandroidtestbed" \
        --private-key "/path/to/private.pem" \
        --decryption-key "/path/to/api_keys.enc" \
        --password "your_password"

    # Option 3: With raw base64 key string
    python3 verify_play_integrity_self_managed.py \
        --response-body '...' \
        --package "io.branch.branchandroidtestbed" \
        --private-key "/path/to/private.pem" \
        --decryption-key "b9LQ4V50jOdFvIzNOzDKSuWmm5b+1KWcYXDwgkvW/Lo="

    # Individual token fields:
    python3 verify_play_integrity_self_managed.py \
        --token "..." --nonce "..." --ecdh "..." \
        --package "io.branch.branchandroidtestbed" \
        --private-key "/path/to/private.pem" \
        --decryption-key "/path/to/api_keys.txt" \
        --body '{...}'

Dependencies:
    pip3 install cryptography
"""

import argparse
import base64
import getpass
import hashlib
import json
import sys
import time
from typing import Dict, Any, Optional

try:
    from cryptography.hazmat.primitives import hashes, serialization
    from cryptography.hazmat.primitives.asymmetric import ec, rsa, padding
    from cryptography.hazmat.primitives.ciphers.aead import AESGCM
    from cryptography.hazmat.primitives.keywrap import aes_key_unwrap
    from cryptography.hazmat.backends import default_backend
    from cryptography.hazmat.primitives.asymmetric.types import PrivateKeyTypes
except ImportError:
    print("❌ Missing required dependency: cryptography")
    print("   Install with: pip3 install cryptography")
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


# ── Local token decryption ─────────────────────────────────────────────────────

def load_private_key(key_path: str, password: Optional[str] = None) -> PrivateKeyTypes:
    """Load private key (RSA or EC) from PEM file, with optional password support."""
    print(f"  🔑 Loading private key from: {key_path}")

    with open(key_path, 'rb') as f:
        key_data = f.read()

    # Convert password to bytes if provided
    password_bytes = password.encode('utf-8') if password else None

    try:
        private_key = serialization.load_pem_private_key(
            key_data,
            password=password_bytes,
            backend=default_backend()
        )
    except TypeError as e:
        if "password" in str(e).lower():
            # Key is encrypted but no password provided
            print(f"\n❌ Private key is encrypted and requires a password")
            print(f"   Use --password option or the script will prompt you")

            # Prompt for password interactively
            try:
                password_input = getpass.getpass("   Enter private key password: ")
                password_bytes = password_input.encode('utf-8')
                private_key = serialization.load_pem_private_key(
                    key_data,
                    password=password_bytes,
                    backend=default_backend()
                )
            except Exception as inner_e:
                print(f"❌ Failed to load private key with provided password: {inner_e}")
                raise
        else:
            raise

    # Accept both RSA and EC keys
    if isinstance(private_key, rsa.RSAPrivateKey):
        key_size = private_key.key_size
        print(f"  ✅ RSA private key loaded (key size: {key_size} bits)")
        if key_size != 2048:
            print(f"  ⚠️  Warning: Google recommends 2048-bit RSA keys, but got {key_size} bits")
    elif isinstance(private_key, ec.EllipticCurvePrivateKey):
        print(f"  ✅ EC private key loaded (curve: {private_key.curve.name})")
    else:
        raise ValueError(f"Key must be RSA (2048-bit) or EC (prime256v1/secp256r1), got {type(private_key)}")

    return private_key


def load_decryption_key_bundle(key_bundle_path: str) -> bytes:
    """
    Load the decryption key bundle.

    Supports three formats:
    1. Pre-decrypted text file (api_keys.txt) with DECRYPTION_KEY=<base64>
    2. Encrypted binary file (.enc) that needs RSA decryption
    3. Raw base64 string passed directly
    """
    # Check if it's a file path or raw base64 string
    if key_bundle_path.startswith(('AAAA', 'AQAA', 'AgAA', 'AwAA')) or '/' not in key_bundle_path:
        # Might be raw base64 string
        try:
            decoded = base64.b64decode(key_bundle_path)
            if len(decoded) == 32:  # AES-256 key
                print(f"  📦 Using raw base64 decryption key (already decrypted)")
                print(f"  ✅ Decryption key loaded ({len(decoded)} bytes)")
                return decoded
        except:
            pass

    # It's a file path
    print(f"  📦 Loading decryption key bundle from: {key_bundle_path}")

    # Check file extension
    if key_bundle_path.endswith('.txt'):
        # Pre-decrypted text file format
        with open(key_bundle_path, 'r') as f:
            content = f.read()

        # Parse DECRYPTION_KEY=<base64> format
        for line in content.split('\n'):
            if line.startswith('DECRYPTION_KEY='):
                key_b64 = line.split('=', 1)[1].strip()
                key_bytes = base64.b64decode(key_b64)
                print(f"  ✅ Pre-decrypted key bundle loaded ({len(key_bytes)} bytes)")
                return key_bytes

        raise ValueError("DECRYPTION_KEY not found in text file")

    else:
        # Encrypted binary file (.enc)
        with open(key_bundle_path, 'rb') as f:
            bundle = f.read()
        print(f"  ✅ Encrypted key bundle loaded ({len(bundle)} bytes, needs RSA decryption)")
        return bundle


def decrypt_key_bundle(encrypted_bundle: bytes, private_key: rsa.RSAPrivateKey) -> bytes:
    """
    Decrypt the key bundle using RSA-OAEP.

    The key bundle from Play Console is encrypted with your RSA public key.
    This function decrypts it to get the actual AES-256 key used for token decryption.
    """
    print(f"  🔓 Decrypting key bundle with RSA-OAEP...")

    try:
        decrypted_key = private_key.decrypt(
            encrypted_bundle,
            padding.OAEP(
                mgf=padding.MGF1(algorithm=hashes.SHA256()),
                algorithm=hashes.SHA256(),
                label=None
            )
        )
        print(f"  ✅ Key bundle decrypted successfully ({len(decrypted_key)} bytes)")
        return decrypted_key
    except Exception as e:
        print(f"\n❌ Failed to decrypt key bundle: {e}")
        print(f"   This usually means:")
        print(f"   1. The private key doesn't match the public key uploaded to Play Console")
        print(f"   2. The key bundle file is corrupted or incorrect")
        raise


def decrypt_token_locally(
    token: str,
    private_key: PrivateKeyTypes,
    decryption_key: Optional[bytes]
) -> dict:
    """
    Decrypt Play Integrity token locally using self-managed keys.

    The token is a JWE (JSON Web Encryption) in compact serialization format:
    header.encrypted_key.iv.ciphertext.tag

    Google Play Integrity uses:
    - Key encryption: A256KW (AES-256 Key Wrap) using the decrypted key bundle
    - Content encryption: AES-256-GCM

    Flow:
    1. Decrypt the key bundle (encrypted with your RSA public key) using RSA private key
    2. Use the decrypted AES-256 key to unwrap the CEK from the token
    3. Use the CEK to decrypt the token payload

    Note: This is a POC implementation. Production implementations should
    use a proper JWE library or follow Google's exact specification.
    """
    print(f"  🔐 Decrypting token locally (length: {len(token)} chars)")

    try:
        # The token is in JWE compact serialization format
        # Format: header.encrypted_key.iv.ciphertext.tag
        parts = token.split('.')

        if len(parts) != 5:
            raise ValueError(f"Invalid JWE token format: expected 5 parts, got {len(parts)}")

        header_b64, encrypted_cek_b64, iv_b64, ciphertext_b64, tag_b64 = parts

        print(f"  📊 JWE token structure:")
        print(f"     Header: {len(header_b64)} chars")
        print(f"     Encrypted CEK: {len(encrypted_cek_b64)} chars")
        print(f"     IV: {len(iv_b64)} chars")
        print(f"     Ciphertext: {len(ciphertext_b64)} chars")
        print(f"     Tag: {len(tag_b64)} chars")

        # Decode components (base64url without padding)
        def b64url_decode(data: str) -> bytes:
            # Add padding if needed
            padding_needed = 4 - (len(data) % 4)
            if padding_needed != 4:
                data += '=' * padding_needed
            return base64.urlsafe_b64decode(data)

        header = json.loads(b64url_decode(header_b64).decode('utf-8'))
        encrypted_cek = b64url_decode(encrypted_cek_b64)
        iv = b64url_decode(iv_b64)
        ciphertext = b64url_decode(ciphertext_b64)
        auth_tag = b64url_decode(tag_b64)

        print(f"  📋 JWE Header: {header}")

        # Check the algorithm
        alg = header.get('alg')
        enc = header.get('enc')

        if alg != 'A256KW':
            raise ValueError(f"Unsupported key encryption algorithm: {alg} (expected A256KW)")

        if enc != 'A256GCM':
            raise ValueError(f"Unsupported content encryption algorithm: {enc} (expected A256GCM)")

        # Unwrap the Content Encryption Key (CEK) using the decryption key
        if decryption_key is None:
            raise ValueError("Decryption key bundle is required for A256KW algorithm")

        print(f"  🔓 Unwrapping CEK with AES-256-KW...")
        print(f"     Decryption key length: {len(decryption_key)} bytes")
        print(f"     Encrypted CEK length: {len(encrypted_cek)} bytes")

        # Use AES Key Unwrap to decrypt the CEK
        try:
            cek = aes_key_unwrap(decryption_key, encrypted_cek, backend=default_backend())
            print(f"  ✅ CEK unwrapped successfully ({len(cek)} bytes)")
        except Exception as unwrap_error:
            print(f"\n❌ AES Key Unwrap failed: {unwrap_error}")
            print(f"\n💡 This usually means:")
            print(f"   1. Wrong decryption key (doesn't match the key used to encrypt this token)")
            print(f"   2. The token was not encrypted with your uploaded public key")
            print(f"   3. The decryption key from Play Console doesn't match this app/package")
            print(f"\n   Debugging info:")
            print(f"   • Decryption key (base64): {base64.b64encode(decryption_key).decode()}")
            print(f"   • Encrypted CEK (base64): {base64.b64encode(encrypted_cek).decode()}")
            raise

        # Decrypt the ciphertext using AES-GCM
        print(f"  🔐 Decrypting payload with AES-256-GCM...")

        # Construct AAD (Additional Authenticated Data) = ASCII(BASE64URL(JWE Protected Header))
        aad = header_b64.encode('ascii')

        # Combine ciphertext and tag for AES-GCM
        ciphertext_with_tag = ciphertext + auth_tag

        aesgcm = AESGCM(cek)
        plaintext = aesgcm.decrypt(iv, ciphertext_with_tag, aad)

        # Parse JSON payload
        payload = json.loads(plaintext.decode('utf-8'))
        print(f"  ✅ Token decrypted successfully!")

        return payload

    except Exception as e:
        print(f"\n❌ Decryption failed: {e}")
        print(f"\n💡 Troubleshooting:")
        print(f"   1. Verify the decryption key bundle is correctly decrypted")
        print(f"   2. Ensure the token was generated by Play Integrity API")
        print(f"   3. Check that your app is configured with the correct public key in Play Console")
        raise


# ── Verification ───────────────────────────────────────────────────────────────

FRESHNESS_WINDOW_MS = 10 * 60 * 1000  # 10 minutes

PI_FIELDS = {"play_integrity_token", "nonce", "ecdh_public_key"}  # strip before re-deriving


def verify(decoded: dict, package_name: str, expected_nonce_b64url: str) -> dict:
    """Verify the decrypted token payload."""
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
        print(f"\n📋 Fields actually present in response body ({len(response_body)} total):")
        # Show first 20 field names to help debug
        for i, key in enumerate(sorted(response_body.keys())[:20]):
            value = response_body[key]
            # Truncate long values for readability
            value_str = str(value)[:50] + "..." if len(str(value)) > 50 else str(value)
            print(f"   • {key}: {value_str}")
        if len(response_body) > 20:
            print(f"   ... and {len(response_body) - 20} more fields")
        print(f"\n💡 These fields are added by BranchFraudDefense.performAttestationCheck()")
        print(f"   Make sure you've:")
        print(f"   1. Enabled fraud defense: BranchFraudDefense.getInstance(context).startFraudDefenseSystem()")
        print(f"   2. Set the provider: branch.setFraudDefenseProvider(fraudDefense)")
        print(f"   3. Captured the request body AFTER attestation is performed")
        sys.exit(1)

    return {
        "token": response_body["play_integrity_token"],
        "nonce": response_body["nonce"],
        "ecdh": response_body["ecdh_public_key"]
    }


# ── Main ───────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(
        description="Self-managed Play Integrity POC verifier",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Use response body with RSA key (easiest):
  python3 verify_play_integrity_self_managed.py \
    --response-body '{"play_integrity_token":"...","nonce":"...","ecdh_public_key":"...",...}' \
    --package "io.branch.branchandroidtestbed" \
    --private-key "./private.pem" \
    --decryption-key "./api_keys.enc"

  # With encrypted private key (will prompt for password):
  python3 verify_play_integrity_self_managed.py \
    --response-body '...' \
    --package "io.branch.branchandroidtestbed" \
    --private-key "./private.pem" \
    --decryption-key "./api_keys.enc"

  # Or provide password on command line (less secure):
  python3 verify_play_integrity_self_managed.py \
    --response-body '...' \
    --package "io.branch.branchandroidtestbed" \
    --private-key "./private.pem" \
    --decryption-key "./api_keys.enc" \
    --password "yourpassword"

  # Use individual fields:
  python3 verify_play_integrity_self_managed.py \
    --token "..." --nonce "..." --ecdh "..." \
    --package "io.branch.branchandroidtestbed" \
    --private-key "./private.pem" \
    --decryption-key "./api_keys.enc" \
    --body '{...}'
"""
    )

    parser.add_argument("--response-body",   help="JSON response body containing all Play Integrity fields")
    parser.add_argument("--token",           help="play_integrity_token from SDK")
    parser.add_argument("--nonce",           help="base64 random nonce from SDK")
    parser.add_argument("--ecdh",            help="base64 ECDH public key from SDK")
    parser.add_argument("--package",         required=True, help="App package name")
    parser.add_argument("--private-key",     required=True, help="Path to RSA private key PEM file (2048-bit as per Google docs)")
    parser.add_argument("--password",        help="Password for encrypted private key (will prompt if not provided)")
    parser.add_argument("--decryption-key",  required=True, help="Decryption key: path to .enc file, path to api_keys.txt, or raw base64 string")
    parser.add_argument("--body",            default="{}",  help="JSON request body")
    args = parser.parse_args()

    print("\n" + "="*70)
    print("  Play Integrity Verification - Self-Managed Keys Mode")
    print("  (No Google API calls - local decryption only)")
    print("="*70)

    # Parse inputs
    if args.response_body:
        print("\n── Parsing response body ────────────────────────────────────────")
        try:
            response_body = json.loads(args.response_body)
        except json.JSONDecodeError as e:
            print(f"❌ Failed to parse response body as JSON: {e}")
            sys.exit(1)

        parsed = parse_response_body(response_body)
        token = parsed["token"]
        nonce = parsed["nonce"]
        ecdh = parsed["ecdh"]

        # Validate extracted values are not empty
        if not token or not nonce or not ecdh:
            print(f"❌ Extracted values are empty!")
            print(f"   Token: {'<empty>' if not token else 'OK'}")
            print(f"   Nonce: {'<empty>' if not nonce else 'OK'}")
            print(f"   ECDH public key: {'<empty>' if not ecdh else 'OK'}")
            sys.exit(1)

        # Use response body as request body (will strip PI fields)
        body = response_body

        print(f"  ✅ Extracted Play Integrity fields:")
        print(f"     Token length: {len(token)} chars")
        print(f"     Token preview: {token[:80]}...")
        print(f"     Nonce length: {len(nonce)} chars")
        print(f"     Nonce: {nonce[:64]}...")
        print(f"     ECDH public key length: {len(ecdh)} chars")
        print(f"     ECDH public key: {ecdh[:64]}...")
        print(f"  ✅ Found {len(body)} total fields in response body")
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

    print("\n── Step 2: Load and decrypt encryption keys ────────────────────")
    private_key = load_private_key(args.private_key, args.password)

    # Load and decrypt the key bundle to get the AES-256 key
    if not args.decryption_key:
        print("❌ Error: --decryption-key is required")
        print("   Options:")
        print("   1. Pre-decrypted text file (api_keys.txt)")
        print("   2. Encrypted binary file (.enc)")
        print("   3. Raw base64 string")
        sys.exit(1)

    key_bundle = load_decryption_key_bundle(args.decryption_key)

    # Check if the key bundle needs RSA decryption (256 bytes = RSA-2048 encrypted)
    if len(key_bundle) == 256:
        # Encrypted - needs RSA decryption
        if not isinstance(private_key, rsa.RSAPrivateKey):
            print("❌ Error: Encrypted key bundle requires RSA private key")
            print(f"   Got: {type(private_key)}")
            sys.exit(1)

        decryption_key = decrypt_key_bundle(key_bundle, private_key)
    elif len(key_bundle) == 32:
        # Already decrypted AES-256 key
        print("  ✅ Using pre-decrypted AES-256 key (no RSA decryption needed)")
        decryption_key = key_bundle
    else:
        print(f"❌ Error: Invalid key bundle size: {len(key_bundle)} bytes")
        print(f"   Expected: 256 bytes (encrypted) or 32 bytes (decrypted AES-256)")
        sys.exit(1)

    print("\n── Step 3: Decrypt token locally ────────────────────────────────")
    print("  ℹ️  Token format: JWE (JSON Web Encryption)")
    print("      Key encryption: A256KW (AES-256 Key Wrap)")
    print("      Content encryption: AES-256-GCM")

    try:
        decoded = decrypt_token_locally(token, private_key, decryption_key)
        print("\n  📋 Decrypted payload:")
        print(json.dumps(decoded, indent=4))
    except Exception as e:
        print(f"\n❌ Failed to decrypt token: {e}")
        print("\n💡 Common issues:")
        print("   1. Wrong private key - must match the public key uploaded to Play Console")
        print("   2. Wrong decryption key bundle - must be the .enc file downloaded from Play Console")
        print("   3. Token not generated with your uploaded public key")
        print("   4. Incorrect password for the private key")
        print("\n   Make sure:")
        print("   • Your app is using the public key uploaded to Play Console")
        print("   • The token was generated by Play Integrity API (not manually created)")
        print("   • The decryption key bundle (.enc) matches your app/package")
        sys.exit(1)

    print("\n── Step 4: Verify payload ───────────────────────────────────────")
    result = verify(decoded, args.package, expected_nonce)

    for check, detail in result["checks"].items():
        status = "✅" if detail["ok"] else "❌"
        print(f"  {status} {check}: {detail}")

    print("\n── Info fields ──────────────────────────────────────────────────")
    for k, v in result["info"].items():
        print(f"  {k}: {v}")

    overall = "✅ PASSED" if result["passed"] else "❌ FAILED"
    print(f"\n── Overall result: {overall} ─────────────────────────────────────")

    print("\n✨ Advantages of self-managed keys:")
    print("   • No Google API quota constraints")
    print("   • Lower latency (no external API call)")
    print("   • Works offline after initial setup")
    print("   • Full control over decryption infrastructure")
    print("   • No dependency on Google's decryption API")
    print("\n⚠️  Trade-offs:")
    print("   • Must securely manage and rotate keys")
    print("   • Requires secure backend infrastructure")
    print("   • App must be published on Google Play")
    print("   • Need to securely store private key and decryption key bundle")
    print("\n📚 Encryption scheme used:")
    print("   • Token format: JWE (JSON Web Encryption)")
    print("   • Key wrap: A256KW (AES-256 Key Wrap)")
    print("   • Content encryption: AES-256-GCM")
    print("   • RSA key: Used to decrypt the key bundle, not the token directly")
    print("="*70 + "\n")

    sys.exit(0 if result["passed"] else 1)


if __name__ == "__main__":
    main()
