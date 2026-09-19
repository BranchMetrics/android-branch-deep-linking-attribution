"""Unit tests for the L1 wire-validation script.

Run from the repo root:

    python -m unittest scripts.test_validate_l1_logs
"""

import io
import os
import sys
import os
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


class V3ContractTests(unittest.TestCase):
    """The /v3 endpoints were skipped by the old `/v1/*` path scope, so nothing asserted on the
    beta's actual wire. Catches a /v3 payload losing a required field — the previous scope would
    have passed it silently."""

    def test_v3_missing_required_field_fails(self):
        errors, _ = _run_validation("v3_missing_anon_id.txt")
        self.assertTrue(
            any("/v3/deeplink" in e and "'anon_id'" in e for e in errors),
            f"Expected a named anon_id error on /v3/deeplink, got: {errors}",
        )


class NoMandatoryEndpointTests(unittest.TestCase):
    """No endpoint is globally mandatory, /v3/events/open included.

    A correct beta capture can contain no install and no init at all: e.g. a standalone
    link-creation call. What each scenario must emit belongs in a per-scenario contract
    (assert_contract), not in a global rule. No existing test covered the absence of that
    rule, which is why removing it could have gone unnoticed.
    """

    def test_capture_without_install_is_valid(self):
        errors, _ = _run_validation("no_install.txt")
        self.assertEqual(
            [], errors, f"A capture without an install must be valid, got: {errors}"
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


def _capture(*uris):
    """A normalized capture with no payload — enough for counts and order."""
    return [{"uri": u, "url": "https://h" + u, "request": {}} for u in uris]


class AssertionEngineTests(unittest.TestCase):
    """counts and order, ported from the iOS line so both platforms assert
    the same way. The engine holds no endpoint name of its own."""

    def _contract(self, counts=None, order=()):
        return {"counts": counts or {}, "order": order, "fields": {}}

    def test_exact_count_satisfied(self):
        entries = _capture("/a", "/a")
        self.assertEqual(v.assert_contract(entries, self._contract({"/a": 2})), [])

    def test_too_many_fails(self):
        errors = v.assert_contract(_capture("/a", "/a"), self._contract({"/a": 1}))
        self.assertEqual(len(errors), 1, errors)
        self.assertIn("captured 2", errors[0])

    def test_count_zero_forbids_the_endpoint(self):
        errors = v.assert_contract(_capture("/a"), self._contract({"/a": 0}))
        self.assertIn("must not be captured", errors[0])

    def test_unlisted_endpoints_are_unconstrained(self):
        self.assertEqual(v.assert_contract(_capture("/a", "/b"), self._contract({"/a": 1})), [])

    def test_order_holds_when_other_traffic_interleaves(self):
        entries = _capture("/a", "/x", "/b")
        self.assertEqual(v.assert_contract(entries, self._contract(order=(("/a", "/b"),))), [])

    def test_order_violated_when_later_never_follows(self):
        entries = _capture("/b", "/a")
        errors = v.assert_contract(entries, self._contract(order=(("/a", "/b"),)))
        self.assertIn("after", errors[0])

    def test_order_is_fail_closed_when_an_endpoint_is_absent(self):
        errors = v.assert_contract(_capture("/a"), self._contract(order=(("/a", "/b"),)))
        self.assertEqual(len(errors), 1, errors)

    def test_a_launch_open_before_the_resolution_does_not_violate_order(self):
        # Why occurs_after is relative. On both betas the launch open precedes
        # the link resolution, and the attributed open follows it.
        entries = _capture("/v3/events/open", "/v3/deeplink", "/v3/events/open")
        contract = self._contract(order=(("/v3/deeplink", "/v3/events/open"),))
        self.assertEqual(v.assert_contract(entries, contract), [])


class FieldPresenceEngineTests(unittest.TestCase):
    """`fields` counts how many of an endpoint's requests carry a field.
    Presence only — is_present is the whole test."""

    def _entries(self, *payloads):
        return [{"uri": "/e", "url": "https://h/e", "request": p} for p in payloads]

    def _contract(self, fields):
        return {"counts": {}, "order": (), "fields": {"/e": fields}}

    def test_exact_field_count_satisfied(self):
        self.assertEqual(
            v.assert_contract(self._entries({"tok": "a"}, {}), self._contract({"tok": 1})), []
        )

    def test_too_many_carriers_fails(self):
        errors = v.assert_contract(
            self._entries({"tok": "a"}, {"tok": "b"}), self._contract({"tok": 1})
        )
        self.assertIn("2 of 2 did", errors[0])

    def test_zero_forbids_the_field(self):
        errors = v.assert_contract(self._entries({"tok": "a"}), self._contract({"tok": 0}))
        self.assertIn("may carry", errors[0])

    def test_empty_string_does_not_count_as_carrying(self):
        # is_present treats "" as absent; a cleared identifier must not read
        # as present.
        self.assertEqual(
            v.assert_contract(self._entries({"tok": ""}), self._contract({"tok": 0})), []
        )

    def test_the_v2_shape_is_resolved(self):
        # lookup_field reaches under user_data, which is where Android nests
        # device fields on /v2/event/*.
        self.assertEqual(
            v.assert_contract(self._entries({"user_data": {"tok": "a"}}), self._contract({"tok": 1})), []
        )


class UnknownScenarioTests(unittest.TestCase):
    def test_an_unknown_name_is_refused_by_name(self):
        with self.assertRaises(v.UnknownScenario) as ctx:
            v.contract_for("C9")
        self.assertIn("C9", str(ctx.exception))



class RetryCollapseTests(unittest.TestCase):
    """A retry re-sends the same branch_sdk_request_unique_id, so counting
    attempts would inflate an exact count whenever the network is flaky."""

    def _entry(self, uri, request_id=None):
        req = {} if request_id is None else {"branch_sdk_request_unique_id": request_id}
        return {"uri": uri, "url": "https://h" + uri, "request": req}

    def test_a_repeated_request_id_counts_once(self):
        entries = [self._entry("/a", "id-1"), self._entry("/a", "id-1")]
        self.assertEqual(len(v.collapse_retries(entries)), 1)

    def test_distinct_ids_are_both_kept(self):
        entries = [self._entry("/a", "id-1"), self._entry("/a", "id-2")]
        self.assertEqual(len(v.collapse_retries(entries)), 2)

    def test_entries_without_the_field_are_kept(self):
        # A request predating EMT-4198's stamping must not be dropped.
        entries = [self._entry("/a"), self._entry("/a")]
        self.assertEqual(len(v.collapse_retries(entries)), 2)

    def test_a_retried_capture_satisfies_an_exact_count(self):
        # The regression this exists to prevent: a flaky runner turning a
        # correct capture red on count.
        entries = [self._entry("/a", "id-1"), self._entry("/a", "id-1")]
        contract = {"counts": {"/a": 1}, "order": (), "fields": {}}
        self.assertEqual(v.assert_contract(v.collapse_retries(entries), contract), [])

    def test_without_collapsing_the_same_capture_would_fail(self):
        # Pins why the collapse is load-bearing rather than decorative.
        entries = [self._entry("/a", "id-1"), self._entry("/a", "id-1")]
        contract = {"counts": {"/a": 1}, "order": (), "fields": {}}
        self.assertTrue(v.assert_contract(entries, contract))


class HarnessContractTests(unittest.TestCase):
    """The one contract the available measurement sustains.

    Asserted through assert_contract rather than the CLI on purpose: the
    fixture is a real capture from before EMT-4198 stamped the request
    identifiers, so it still fails the per-request field checks. Those
    failures are the defect that ticket fixes, and they are not what this
    contract is about."""

    def _entries(self):
        return v.collapse_retries(v.parse_branch_logs(_fixture("harness_mixed_session.txt")))

    def test_the_measured_capture_satisfies_the_contract(self):
        errors = v.assert_contract(self._entries(), v.contract_for("harness"))
        self.assertEqual(errors, [], f"Unexpected errors: {errors}")

    def test_the_contract_traces_to_the_capture_it_was_written_from(self):
        # Every count in the contract must be a fact about the fixture, not a
        # number someone liked. This is the check that would have caught a
        # contract written from the ticket text.
        entries = self._entries()
        uris = [e["uri"] for e in entries]
        for endpoint, expected in v.contract_for("harness")["counts"].items():
            self.assertEqual(uris.count(endpoint), expected, endpoint)

    def test_a_missing_deeplink_fails(self):
        entries = [e for e in self._entries() if e["uri"] != "/v3/deeplink"]
        errors = v.assert_contract(entries, v.contract_for("harness"))
        self.assertTrue(any("/v3/deeplink" in e for e in errors), errors)

    def test_hardware_id_appearing_on_link_creation_fails(self):
        # The EMT-4199 signal. Android strips hardware_id on /v1/url today; if
        # that changes the gate must notice rather than pass quietly.
        entries = self._entries()
        for e in entries:
            if e["uri"] == "/v1/url":
                e["request"]["hardware_id"] = "something"
        errors = v.assert_contract(entries, v.contract_for("harness"))
        self.assertTrue(any("hardware_id" in e for e in errors), errors)

    def test_the_registry_holds_only_measured_contracts(self):
        # Replaces the empty-registry pin, which died the moment a contract
        # existed. Every entry must name a fixture that exists.
        for name in v.SCENARIO_CONTRACTS:
            self.assertTrue(
                os.path.exists(_fixture(f"{name}_mixed_session.txt"))
                or os.path.exists(_fixture(f"{name}.txt")),
                f"contract '{name}' has no fixture backing it",
            )
