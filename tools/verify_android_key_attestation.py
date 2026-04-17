#!/usr/bin/env python3
"""
verify_android_key_attestation.py — Local verification for Android Key Attestation.

Simulates what the Branch backend would do to verify Android Key Attestation
from a v1/install request that contains app_ecdh_pub, key_attestation_chain, nonce,
and ecdh_public_key.

Mirrors the Kotlin AppAttestation.verifyLocally() function.

Checks performed:
  1. Certificate chain validity (each cert signed by next)
  2. Challenge binding (attestation challenge matches recomputed SHA-256)
  3. Public key match (leaf cert public key matches app_ecdh_pub)
  4. Security level (STRONGBOX, TRUSTED_ENVIRONMENT, SOFTWARE)

Checks skipped (require server infrastructure):
  - Root CA trust verification
  - Certificate revocation (CRL)
  - Boot state, package name (deep ASN.1 parsing)
  - Nonce replay protection

Usage:
    # Option 1: Provide response body (easiest)
    python3 verify_android_key_attestation.py \
        --response-body '{"app_ecdh_pub":"...","key_attestation_chain":["..."],"nonce":"...","ecdh_public_key":"...","branch_key":"...",...}'

    # Option 2: Provide individual fields
    python3 verify_android_key_attestation.py \
        --app-ecdh-pub "<base64_public_key_from_leaf_cert>" \
        --chain "<base64_cert_1>" "<base64_cert_2>" "<base64_cert_3>" \
        --nonce "<base64_random_nonce>" \
        --ecdh-pub "<base64_ecdh_public_key>" \
        --body '{"branch_key":"key_live_...","os":"Android",...}'

Dependencies:
    pip3 install cryptography
"""

import argparse
import base64
import hashlib
import json
import sys
from typing import List, Optional

try:
    from cryptography import x509
    from cryptography.hazmat.backends import default_backend
    from cryptography.hazmat.primitives.asymmetric import ec
except ImportError:
    print("❌ Missing required dependency: cryptography")
    print("   Install with: pip3 install cryptography")
    sys.exit(1)


# ── Constants ──────────────────────────────────────────────────────────────────

# Android Key Attestation extension OID
ATTESTATION_EXTENSION_OID = "1.3.6.1.4.1.11129.2.1.17"

# Attestation fields to strip before re-deriving challenge
ATTESTATION_FIELDS = {"app_ecdh_pub", "key_attestation_chain", "nonce", "ecdh_public_key"}

# DER tags
TAG_OCTET_STRING = 0x04
TAG_SEQUENCE = 0x30
TAG_ENUMERATED = 0x0A


# ── Canonical query string ────────────────────────────────────────────────────

def canonical_query_string(body: dict) -> str:
    """Mirrors AppAttestation.canonicalQueryString() — sorted keys, key=value& format."""
    return "&".join(f"{k}={v}" for k, v in sorted(body.items()))


# ── Challenge derivation ───────────────────────────────────────────────────────

def build_challenge(random_nonce_bytes: bytes, request_body: dict, ecdh_pub_bytes: bytes) -> bytes:
    """
    Derives the 32-byte attestation challenge:
      SHA-256(canonicalQueryStringBytes || ecdhPublicKeyBytes || randomNonceBytes)

    Mirrors AppAttestation.buildChallenge() in Kotlin.
    """
    canonical_bytes = canonical_query_string(request_body).encode("utf-8")
    return hashlib.sha256(canonical_bytes + ecdh_pub_bytes + random_nonce_bytes).digest()


# ── DER parsing helpers ────────────────────────────────────────────────────────

class DerElement:
    def __init__(self, tag: int, value: bytes):
        self.tag = tag
        self.value = value


def der_read_length(buf: bytes, offset: int) -> tuple[int, int]:
    """Parse DER length field. Returns (length, bytes_consumed)."""
    first = buf[offset]
    if first < 0x80:
        return first, 1

    num_len_bytes = first & 0x7F
    length = int.from_bytes(buf[offset + 1:offset + 1 + num_len_bytes], "big")
    return length, 1 + num_len_bytes


def der_read_element(buf: bytes, offset: int) -> tuple[DerElement, int]:
    """Read one DER TLV. Returns (DerElement, next_offset)."""
    tag = buf[offset]
    length, len_bytes = der_read_length(buf, offset + 1)
    value_offset = offset + 1 + len_bytes
    value = buf[value_offset:value_offset + length]
    return DerElement(tag, value), value_offset + length


def read_sequence_element(seq_content: bytes, index: int) -> Optional[DerElement]:
    """Walk SEQUENCE content and return element at given index."""
    pos = 0
    i = 0
    while pos < len(seq_content):
        elem, next_pos = der_read_element(seq_content, pos)
        if i == index:
            return elem
        i += 1
        pos = next_pos
    return None


def extract_attestation_extension_element(cert: x509.Certificate, index: int) -> Optional[bytes]:
    """
    Extract element at [index] from Android Key Attestation extension.

    Extension structure (OID 1.3.6.1.4.1.11129.2.1.17):
      SEQUENCE {
        [0] INTEGER attestationVersion
        [1] ENUMERATED attestationSecurityLevel  ← index=1
        [2] INTEGER keymasterVersion
        [3] ENUMERATED keymasterSecurityLevel
        [4] OCTET STRING attestationChallenge    ← index=4
        [5] OCTET STRING uniqueId
        [6] SEQUENCE softwareEnforced
        [7] SEQUENCE hardwareEnforced
      }
    """
    try:
        ext_value = cert.extensions.get_extension_for_oid(
            x509.ObjectIdentifier(ATTESTATION_EXTENSION_OID)
        ).value.value
    except x509.ExtensionNotFound:
        return None

    # Extension value is wrapped: OCTET STRING { SEQUENCE { ... } }
    outer, _ = der_read_element(ext_value, 0)
    if outer.tag != TAG_OCTET_STRING:
        return None

    seq, _ = der_read_element(outer.value, 0)
    if seq.tag != TAG_SEQUENCE:
        return None

    elem = read_sequence_element(seq.value, index)
    return elem.value if elem else None


def extract_attestation_challenge(cert: x509.Certificate) -> Optional[bytes]:
    """Extract attestationChallenge (element 4) from attestation extension."""
    return extract_attestation_extension_element(cert, 4)


def extract_security_level(cert: x509.Certificate) -> str:
    """
    Extract attestationSecurityLevel (element 1) from attestation extension.

    SecurityLevel enum values:
      0 = SOFTWARE
      1 = TRUSTED_ENVIRONMENT
      2 = STRONGBOX
    """
    level_bytes = extract_attestation_extension_element(cert, 1)
    if not level_bytes:
        return "UNKNOWN"

    level = level_bytes[0]
    return {0: "SOFTWARE", 1: "TRUSTED_ENVIRONMENT", 2: "STRONGBOX"}.get(level, "UNKNOWN")


# ── Certificate chain verification ─────────────────────────────────────────────

def verify_certificate_chain(chain: List[x509.Certificate]) -> tuple[bool, Optional[str]]:
    """
    Verify each certificate in chain is signed by the next.
    Returns (is_valid, error_message).
    """
    for i in range(len(chain) - 1):
        try:
            # Verify cert[i] is signed by cert[i+1]
            cert = chain[i]
            issuer_cert = chain[i + 1]

            # Use public key from issuer cert to verify signature
            issuer_public_key = issuer_cert.public_key()

            if isinstance(issuer_public_key, ec.EllipticCurvePublicKey):
                # For EC keys, verify using cryptography's built-in verification
                from cryptography.hazmat.primitives.asymmetric.utils import Prehashed
                from cryptography.hazmat.primitives import hashes
                from cryptography.x509 import verification

                # Simple signature verification
                # Note: This is basic verification - production should check all certificate fields
                issuer_public_key.verify(
                    cert.signature,
                    cert.tbs_certificate_bytes,
                    ec.ECDSA(cert.signature_hash_algorithm)
                )
            else:
                # For RSA keys
                issuer_public_key.verify(
                    cert.signature,
                    cert.tbs_certificate_bytes,
                    cert.signature_algorithm_parameters,
                    cert.signature_hash_algorithm
                )

        except Exception as e:
            return False, f"Chain verification failed at index {i}: {e}"

    return True, None


# ── Main verification ──────────────────────────────────────────────────────────

def verify_attestation(
    app_ecdh_pub_b64: str,
    chain_b64_list: List[str],
    nonce_b64: str,
    ecdh_pub_b64: str,
    request_body: dict
) -> dict:
    """
    Verify Android Key Attestation data.

    Returns verification result dict with:
      - passed: bool
      - security_level: str
      - checks: dict of individual check results
    """
    try:
        # 1. Decode certificate chain
        print(f"  📜 Decoding certificate chain ({len(chain_b64_list)} certificates)...")
        chain = []
        for i, cert_b64 in enumerate(chain_b64_list):
            cert_der = base64.b64decode(cert_b64)
            cert = x509.load_der_x509_certificate(cert_der, default_backend())
            chain.append(cert)
            subject_cn = cert.subject.get_attributes_for_oid(x509.NameOID.COMMON_NAME)
            subject_name = subject_cn[0].value if subject_cn else str(cert.subject)
            print(f"     [{i}] {subject_name}")

        leaf = chain[0]

        # 2. Verify certificate chain
        print(f"\n  🔗 Verifying certificate chain...")
        chain_valid, chain_error = verify_certificate_chain(chain)
        if chain_valid:
            print(f"     ✅ All certificates properly signed")
        else:
            print(f"     ❌ Chain verification failed: {chain_error}")

        # 3. Strip attestation fields from body and rebuild challenge
        print(f"\n  🔑 Re-deriving attestation challenge...")
        random_nonce_bytes = base64.b64decode(nonce_b64)
        ecdh_pub_bytes = base64.b64decode(ecdh_pub_b64)

        body_for_challenge = {k: v for k, v in request_body.items()
                             if k not in ATTESTATION_FIELDS}
        expected_challenge = build_challenge(random_nonce_bytes, body_for_challenge, ecdh_pub_bytes)

        canonical = canonical_query_string(body_for_challenge)
        print(f"     Canonical string: {canonical[:100]}...")
        print(f"     Expected challenge: {expected_challenge.hex()[:64]}...")

        # 4. Extract and verify attestation challenge from leaf cert
        print(f"\n  🎯 Verifying challenge binding...")
        cert_challenge = extract_attestation_challenge(leaf)
        challenge_matches = False

        if cert_challenge:
            print(f"     Cert challenge:     {cert_challenge.hex()[:64]}...")
            challenge_matches = cert_challenge == expected_challenge
            if challenge_matches:
                print(f"     ✅ Challenge matches!")
            else:
                print(f"     ❌ Challenge mismatch - possible replay or substitution")
        else:
            print(f"     ❌ Could not extract attestation challenge from certificate")

        # 5. Verify public key match
        print(f"\n  🔐 Verifying public key binding...")
        pub_from_cert = leaf.public_key().public_bytes(
            encoding=serialization.Encoding.DER,
            format=serialization.PublicFormat.SubjectPublicKeyInfo
        )
        pub_from_request = base64.b64decode(app_ecdh_pub_b64)
        public_key_matches = pub_from_cert == pub_from_request

        if public_key_matches:
            print(f"     ✅ Leaf certificate public key matches app_ecdh_pub")
        else:
            print(f"     ❌ Public key mismatch")
            print(f"        Cert:    {pub_from_cert[:32].hex()}...")
            print(f"        Request: {pub_from_request[:32].hex()}...")

        # 6. Extract security level
        print(f"\n  🛡️  Extracting security level...")
        security_level = extract_security_level(leaf)
        security_emoji = {
            "STRONGBOX": "🔒",
            "TRUSTED_ENVIRONMENT": "🔐",
            "SOFTWARE": "⚠️",
            "UNKNOWN": "❓"
        }.get(security_level, "❓")
        print(f"     {security_emoji} Security level: {security_level}")

        if security_level == "SOFTWARE":
            print(f"     ⚠️  WARNING: SOFTWARE attestation should be rejected in production!")

        # Build result
        passed = chain_valid and challenge_matches and public_key_matches

        return {
            "passed": passed,
            "security_level": security_level,
            "checks": {
                "chain_valid": {
                    "ok": chain_valid,
                    "error": chain_error
                },
                "challenge_matches": {
                    "ok": challenge_matches,
                    "expected": expected_challenge.hex() if expected_challenge else None,
                    "got": cert_challenge.hex() if cert_challenge else None
                },
                "public_key_matches": {
                    "ok": public_key_matches,
                    "cert_key_preview": pub_from_cert[:32].hex(),
                    "request_key_preview": pub_from_request[:32].hex()
                }
            }
        }

    except Exception as e:
        return {
            "passed": False,
            "security_level": "UNKNOWN",
            "checks": {},
            "error": str(e)
        }


# ── Response body parsing ──────────────────────────────────────────────────────

def parse_response_body(response_body: dict) -> Optional[dict]:
    """
    Parse attestation fields from response body.

    Expected fields:
      - app_ecdh_pub: base64 public key from leaf cert
      - key_attestation_chain: list of base64 certificates
      - nonce: base64 random nonce
      - ecdh_public_key: base64 ECDH public key

    Returns dict with parsed fields or None if missing required fields.
    """
    required_fields = ["app_ecdh_pub", "key_attestation_chain", "nonce", "ecdh_public_key"]

    missing = [f for f in required_fields if f not in response_body]
    if missing:
        print(f"❌ Missing required fields in response body: {', '.join(missing)}")
        return None

    return {
        "app_ecdh_pub": response_body["app_ecdh_pub"],
        "chain": response_body["key_attestation_chain"],
        "nonce": response_body["nonce"],
        "ecdh_pub": response_body["ecdh_public_key"]
    }


# ── Main ───────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(
        description="Android Key Attestation verifier (mirrors Kotlin verifyLocally)",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Use response body (easiest):
  python3 verify_android_key_attestation.py --response-body '{"app_ecdh_pub":"...","key_attestation_chain":[...],...}'

  # Use individual fields:
  python3 verify_android_key_attestation.py --app-ecdh-pub "..." --chain "..." "..." --nonce "..." --ecdh-pub "..." --body '{...}'
"""
    )

    parser.add_argument("--response-body", help="JSON response body containing all attestation fields")
    parser.add_argument("--app-ecdh-pub", help="Base64 public key from leaf certificate (app_ecdh_pub field)")
    parser.add_argument("--chain", nargs="+", help="Base64 certificate chain (key_attestation_chain field)")
    parser.add_argument("--nonce", help="Base64 random nonce (nonce field)")
    parser.add_argument("--ecdh-pub", help="Base64 ECDH public key (ecdh_public_key field)")
    parser.add_argument("--body", default="{}", help="JSON request body (without attestation fields)")

    args = parser.parse_args()

    print("\n" + "="*70)
    print("  Android Key Attestation Verification")
    print("  (Local verification - mirrors Kotlin verifyLocally)")
    print("="*70)

    # Parse inputs
    if args.response_body:
        print("\n── Parsing response body ────────────────────────────────────────")
        response_body = json.loads(args.response_body)

        parsed = parse_response_body(response_body)
        if not parsed:
            print("\n💡 Response body should contain:")
            print("   • app_ecdh_pub: base64 public key from leaf cert")
            print("   • key_attestation_chain: array of base64 certificates")
            print("   • nonce: base64 random nonce")
            print("   • ecdh_public_key: base64 ECDH public key")
            sys.exit(1)

        app_ecdh_pub = parsed["app_ecdh_pub"]
        chain = parsed["chain"]
        nonce = parsed["nonce"]
        ecdh_pub = parsed["ecdh_pub"]

        # Use response body as request body (will strip attestation fields)
        request_body = response_body

        print(f"  ✅ Found {len(chain)} certificates in chain")
        print(f"  ✅ Nonce: {nonce[:32]}...")
        print(f"  ✅ ECDH pub: {ecdh_pub[:32]}...")

    else:
        # Individual arguments
        if not all([args.app_ecdh_pub, args.chain, args.nonce, args.ecdh_pub]):
            print("❌ Error: Either provide --response-body OR all of:")
            print("   --app-ecdh-pub, --chain, --nonce, --ecdh-pub")
            sys.exit(1)

        app_ecdh_pub = args.app_ecdh_pub
        chain = args.chain
        nonce = args.nonce
        ecdh_pub = args.ecdh_pub
        request_body = json.loads(args.body)

    # Verify
    print("\n── Verification Steps ───────────────────────────────────────────")
    result = verify_attestation(app_ecdh_pub, chain, nonce, ecdh_pub, request_body)

    print("\n" + "="*70)
    print("  Verification Result")
    print("="*70)

    if "error" in result:
        print(f"❌ Verification failed with error: {result['error']}")
        sys.exit(1)

    print(f"\n  Security Level: {result['security_level']}")
    print(f"\n  Individual Checks:")
    for check_name, check_detail in result["checks"].items():
        status = "✅" if check_detail["ok"] else "❌"
        print(f"    {status} {check_name}")
        if not check_detail["ok"] and "error" in check_detail and check_detail["error"]:
            print(f"       Error: {check_detail['error']}")

    overall = "✅ PASSED" if result["passed"] else "❌ FAILED"
    print(f"\n  Overall Result: {overall}")

    print("\n" + "="*70)
    print("  Notes")
    print("="*70)
    print("  ✅ Checks performed:")
    print("     • Certificate chain validity")
    print("     • Challenge binding (SHA-256 match)")
    print("     • Public key binding")
    print("     • Security level extraction")
    print("\n  ⚠️  Checks skipped (require server infrastructure):")
    print("     • Root CA trust verification")
    print("     • Certificate revocation (CRL)")
    print("     • Boot state verification")
    print("     • Package name verification")
    print("     • Nonce replay protection")
    print("="*70 + "\n")

    sys.exit(0 if result["passed"] else 1)


if __name__ == "__main__":
    # Import here to avoid issues with missing imports in argument parsing
    from cryptography.hazmat.primitives import serialization
    main()
