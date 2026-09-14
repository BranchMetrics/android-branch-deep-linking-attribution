"""Per-scenario contract tests for the L1 wire-validation script (harness, H2, ...).

Split out of test_validate_l1_logs.py, which keeps parsing, the assertion engine, and
retry-collapse -- this PR took the combined file from 274 to 326 lines, over the cap.

Run from the repo root:

    python -m unittest scripts.test_validate_l1_logs_contracts
"""

import io
import os
import sys
import unittest
from contextlib import redirect_stdout

THIS_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, THIS_DIR)

import validate_l1_logs as v  # noqa: E402
from test_validate_l1_logs import _fixture  # noqa: E402

# Every contract in the registry must appear here, and every entry must name a file that exists.
# Replaces the filename guess the registry check used before H2: a guess reads fine until a
# fixture's name doesn't match the contract's, which is exactly h2_hot_urischeme.txt against "H2".
SCENARIO_FIXTURES = {
    "harness": "harness_mixed_session.txt",
    "H2": "h2_hot_urischeme.txt",
}


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
        # Every contract must be bound to a fixture that exists, through the explicit map rather
        # than a filename guess.
        for name in v.SCENARIO_CONTRACTS:
            self.assertIn(name, SCENARIO_FIXTURES, f"contract '{name}' has no fixture mapping")
            self.assertTrue(
                os.path.exists(_fixture(SCENARIO_FIXTURES[name])),
                f"contract '{name}' maps to a missing fixture",
            )

    def test_the_fixture_map_holds_no_entry_without_a_contract(self):
        # The other direction. A mapping left behind after its contract was removed is dead
        # weight that reads as coverage.
        for name in SCENARIO_FIXTURES:
            self.assertIn(name, v.SCENARIO_CONTRACTS, f"'{name}' maps a fixture to no contract")


class H2ContractTests(unittest.TestCase):
    """H2 hot_uriScheme. Measured from a real H2HotUriSchemeWireTest run against
    6.0.0-beta.0 at 3fe6a6f9, after the driver's clearing step -- the fixture already reflects
    the clear, not the bare launch that precedes it -- then trimmed to the fields
    validate_l1_logs.py reads. partner_data, identity, instrumentation and several timestamps
    (latest_install_time, latest_update_time, previous_update_time) were dropped; the first check
    against one of those on H2 needs a fresh capture, not this fixture. branch_sdk_request_unique_id
    stays real-shaped for parity with harness_mixed_session.txt's convention -- collapse_retries
    only needs the two values distinct, a placeholder would satisfy that equally."""

    def _entries(self):
        return v.collapse_retries(v.parse_branch_logs(_fixture(SCENARIO_FIXTURES["H2"])))

    def test_the_measured_capture_satisfies_the_contract(self):
        errors = v.assert_contract(self._entries(), v.contract_for("H2"))
        self.assertEqual(errors, [], f"Unexpected errors: {errors}")

    def test_the_contract_traces_to_the_capture_it_was_written_from(self):
        # Every count in the contract must be a fact about the fixture, not a number the ticket
        # text predicted.
        entries = self._entries()
        uris = [e["uri"] for e in entries]
        for endpoint, expected in v.contract_for("H2")["counts"].items():
            self.assertEqual(uris.count(endpoint), expected, endpoint)

    def test_a_second_open_fails_the_contract(self):
        # The acceptance criterion itself: the contract must reject a capture carrying a second
        # open, which is what "exactly 1x" is for.
        entries = self._entries()
        first_open = next(e for e in entries if e["uri"] == "/v3/events/open")
        duplicated = entries + [dict(first_open, request=dict(first_open["request"]))]
        errors = v.assert_contract(duplicated, v.contract_for("H2"))
        self.assertTrue(
            any("/v3/events/open" in e for e in errors),
            f"H2 accepted a capture with a second open: {errors}",
        )

    def test_a_missing_deeplink_fails(self):
        entries = [e for e in self._entries() if e["uri"] != "/v3/deeplink"]
        errors = v.assert_contract(entries, v.contract_for("H2"))
        self.assertTrue(any("/v3/deeplink" in e for e in errors), errors)

    def test_the_fixture_passes_through_validate_entries_with_the_scenario_contract(self):
        # What CI actually runs is `validate_l1_logs.py branchlogs.txt --scenario H2`, which calls
        # validate_entries(entries, contract) on the raw parse, not assert_contract directly on a
        # pre-collapsed list. The tests above never exercise validate_entries or its per-request
        # field checks, so a regression there could pass every test above and still fail CI.
        entries = v.parse_branch_logs(_fixture(SCENARIO_FIXTURES["H2"]))
        buf = io.StringIO()
        with redirect_stdout(buf):
            errors = v.validate_entries(entries, v.contract_for("H2"))
        self.assertEqual(errors, [], f"Unexpected errors: {errors}")


if __name__ == "__main__":
    unittest.main()
