"""Unit tests for the L1 wire-validation script.

Run from the repo root:

    python -m unittest scripts.test_validate_l1_logs
"""

import io
import os
import sys
import unittest
from contextlib import redirect_stdout

THIS_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, THIS_DIR)

import validate_l1_logs as v  # noqa: E402

FIXTURE_DIR = os.path.join(THIS_DIR, "fixtures")


def _fixture(name):
    return os.path.join(FIXTURE_DIR, name)


def _run_validation(fixture_name):
    entries = v.parse_branch_logs(_fixture(fixture_name))
    buf = io.StringIO()
    with redirect_stdout(buf):
        errors = v.validate_entries(entries)
    return errors, buf.getvalue()


class ParseBranchLogsTests(unittest.TestCase):
    def test_returns_none_when_file_missing(self):
        self.assertIsNone(v.parse_branch_logs(_fixture("does_not_exist.txt")))

    def test_parses_paired_posting_and_post_value(self):
        entries = v.parse_branch_logs(_fixture("v3_happy_path.txt"))
        self.assertEqual(len(entries), 2)
        self.assertEqual(entries[0]["uri"], "/v3/events/open")
        self.assertEqual(entries[1]["uri"], "/v1/url")


class ChunkedMessageTests(unittest.TestCase):
    """BranchLogger splits messages over 3500 chars before picking a sink, so
    branchlogs.txt chunks exactly like logcat does. The init request carries
    the attestation cert chain and is the one that always splits.

    Chunking is driven by length, not by tags: the init request below is 6002
    chars and splits in two with no tag in sight. The wire lines carry no
    leading [tags] for platformLog to repeat, so the marker leads the line."""

    def test_chunked_init_request_is_reassembled(self):
        entries = v.parse_branch_logs(_fixture("v3_chunked_init.txt"))
        self.assertEqual(len(entries), 1)
        self.assertEqual(entries[0]["uri"], "/v3/events/open")

    def test_reassembled_payload_carries_the_attestation_object(self):
        """The tail is the point: chunk 1 alone is unterminated JSON, and
        dropping it made the gate report the init request as never sent."""
        entries = v.parse_branch_logs(_fixture("v3_chunked_init.txt"))
        init = entries[0]["request"]["branch_sdk_secure_context"]
        attestation = init["initialization_context"]["attestation_object"]
        self.assertGreater(len(attestation), 3500)

    def test_chunked_capture_validates_clean(self):
        """A split payload must reach the same verdict as an unsplit one."""
        errors, _ = _run_validation("v3_chunked_init.txt")
        self.assertEqual(errors, [], f"Unexpected errors: {errors}")

    def test_out_of_order_chunks_are_discarded(self):
        """Concatenating the wrong slices could yield JSON that parses but
        describes no request the SDK ever sent. Drop instead."""
        with open(_fixture("v3_chunked_init.txt"), encoding="utf-8") as f:
            lines = f.read().splitlines()
        scrambled = [lines[0], lines[2], lines[1]]  # chunk 2 before chunk 1

        tmp = os.path.join(FIXTURE_DIR, "_tmp_scrambled_chunks.txt")
        with open(tmp, "w", encoding="utf-8") as f:
            f.write("\n".join(scrambled) + "\n")
        try:
            buf = io.StringIO()
            with redirect_stdout(buf):
                entries = v.parse_branch_logs(tmp)
        finally:
            os.remove(tmp)

        self.assertEqual(entries, [])
        self.assertIn("unexpected chunk", buf.getvalue())


class HappyPathTests(unittest.TestCase):
    def test_hardware_attestation_capture_has_no_errors(self):
        errors, _ = _run_validation("v3_happy_path.txt")
        self.assertEqual(errors, [], f"Unexpected errors: {errors}")

    def test_play_integrity_capture_has_no_errors(self):
        """A device without hardware Key Attestation support is equally valid."""
        errors, _ = _run_validation("v3_play_integrity.txt")
        self.assertEqual(errors, [], f"Unexpected errors: {errors}")

class MissingFieldTests(unittest.TestCase):
    def test_missing_wifi_fails_with_named_error(self):
        errors, _ = _run_validation("missing_wifi.txt")
        self.assertTrue(
            any("missing required field 'wifi'" in e for e in errors),
            f"Expected wifi-missing error, got: {errors}",
        )


class InitRequestRequiredTests(unittest.TestCase):
    def test_capture_without_events_open_fails(self):
        errors, _ = _run_validation("no_install.txt")
        self.assertTrue(
            any("'/v3/events/open' was not captured" in e for e in errors),
            f"Expected init-missing error, got: {errors}",
        )

    def test_legacy_v1_install_is_flagged(self):
        """Init no longer uses /v1/install; seeing it means the fix regressed."""
        errors, _ = _run_validation("v1_install_present.txt")
        self.assertTrue(
            any("'/v1/install' was captured" in e for e in errors),
            f"Expected legacy-install error, got: {errors}",
        )


class EventEndpointSecureContextTests(unittest.TestCase):
    """v3/events/standard and /custom are signed, so their secure context is
    checked even though their device fields use a different schema (EMT-4245)."""

    def test_signed_event_endpoints_pass(self):
        errors, out = _run_validation("v3_events_signed.txt")
        self.assertEqual(errors, [], f"Unexpected errors: {errors}")
        self.assertIn("Device/SDK field checks skipped", out)

    def test_missing_signature_on_standard_event_fails(self):
        errors, _ = _run_validation("v3_standard_unsigned.txt")
        self.assertTrue(
            any("missing 'activity_context.request_signature'" in e for e in errors),
            f"Expected signature-missing error, got: {errors}",
        )

    def test_custom_event_without_secure_context_is_reported(self):
        """Not an error on its own: only the init request must carry a context."""
        _, out = _run_validation("v3_custom_no_context.txt")
        self.assertIn("no branch_sdk_secure_context", out)


class FreshInstallDetectionTests(unittest.TestCase):
    def test_registered_device_open_does_not_require_initialization(self):
        """An open from a registered device carries activity_context, not Layer 1.
        It has a randomized_bundle_token, which the server only mints after the
        first open, so it must not be held to the fresh-install rule."""
        errors, out = _run_validation("v3_registered_device_open.txt")
        self.assertEqual(errors, [], f"Unexpected errors: {errors}")
        self.assertIn("no fresh-install open in this capture", out)


class SecureContextTests(unittest.TestCase):
    def test_missing_secure_context_on_init_fails(self):
        errors, _ = _run_validation("v3_no_secure_context.txt")
        self.assertTrue(
            any("missing 'branch_sdk_secure_context'" in e for e in errors),
            f"Expected secure-context-missing error, got: {errors}",
        )

    def test_missing_challenge_fails(self):
        errors, _ = _run_validation("v3_missing_challenge.txt")
        self.assertTrue(
            any("missing 'initialization_context.challenge'" in e for e in errors),
            f"Expected challenge-missing error, got: {errors}",
        )

    def test_no_attestation_form_fails(self):
        errors, _ = _run_validation("v3_no_attestation.txt")
        self.assertTrue(
            any("expected exactly one of" in e and "found 0" in e for e in errors),
            f"Expected zero-attestation error, got: {errors}",
        )

    def test_both_attestation_forms_fail(self):
        """Hardware and Play Integrity are alternatives, never both."""
        errors, _ = _run_validation("v3_both_attestations.txt")
        self.assertTrue(
            any("expected exactly one of" in e and "found 2" in e for e in errors),
            f"Expected both-attestations error, got: {errors}",
        )

    def test_activity_context_validated_on_later_requests(self):
        """The signed /v1/url in the happy path carries Layer 2, not Layer 1."""
        _, out = _run_validation("v3_happy_path.txt")
        self.assertIn("activity_context.request_signature", out)
        self.assertIn("activity_context.nonce", out)


if __name__ == "__main__":
    unittest.main()
