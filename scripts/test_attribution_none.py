"""Attribution level NONE: the tiered required fields and the attribution_none contract.

Run from the repo root:

    python -m unittest scripts.test_attribution_none
"""

import io
import os
import sys
import unittest
from contextlib import redirect_stdout

THIS_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, THIS_DIR)

import validate_l1_logs as v  # noqa: E402
from l1_fixtures import fixture_path as _fixture  # noqa: E402


def _run_validation(fixture_name):
    entries = v.parse_branch_logs(_fixture(fixture_name))
    buf = io.StringIO()
    with redirect_stdout(buf):
        errors = v.validate_entries(entries)
    return errors, buf.getvalue()


def _quiet(fn, *args):
    with redirect_stdout(io.StringIO()):
        return fn(*args)


class AttributionTierTests(unittest.TestCase):
    """Required fields tiered by the request's own cpp_level, as on iOS.
    At NONE the SDK strips the device identifiers before sending."""

    NONE_FIXTURE = "attribution_none.txt"
    STRIPPED_AT_NONE = {"local_ip", "anon_id", "first_install_time", "is_hardware_id_real"}

    def _deeplink(self, fixture):
        entries = v.parse_branch_logs(_fixture(fixture))
        return [e for e in entries if e["uri"] == "/v3/deeplink"]

    def test_a_none_resolve_passes_required_fields(self):
        errors, _ = _run_validation(self.NONE_FIXTURE)
        self.assertEqual(errors, [], f"Unexpected errors: {errors}")

    def test_every_other_level_still_requires_anon_id(self):
        for level in ("FULL", "REDUCED", "MINIMAL", None):
            with self.subTest(cpp_level=level):
                entries = self._deeplink("cold_https.txt")
                entries[0]["request"].pop("anon_id")
                if level is not None:
                    entries[0]["request"]["cpp_level"] = level
                errors = _quiet(v.validate_entries, entries)
                self.assertTrue(any("'anon_id'" in e for e in errors), errors)

    def test_none_drops_exactly_the_four_stripped_fields(self):
        full = set(v.required_fields_for("/v3/deeplink", {"cpp_level": "FULL"}))
        for level in ("NONE", "none"):
            with self.subTest(cpp_level=level):
                none = set(v.required_fields_for("/v3/deeplink", {"cpp_level": level}))
                self.assertEqual(full - none, self.STRIPPED_AT_NONE)

    def test_always_fields_survive_every_level(self):
        for request in ({}, {"cpp_level": "FULL"}, {"cpp_level": "REDUCED"},
                        {"cpp_level": "MINIMAL"}, {"cpp_level": "NONE"}):
            with self.subTest(request=request):
                fields = v.required_fields_for("/v3/deeplink", request)
                for field in ("branch_key", "sdk", "wifi"):
                    self.assertIn(field, fields)

    def test_a_none_resolve_without_tracking_disabled_fails(self):
        entries = self._deeplink(self.NONE_FIXTURE)
        entries[0]["request"].pop("tracking_disabled")
        errors = _quiet(v.validate_entries, entries)
        self.assertTrue(any("'tracking_disabled'" in e for e in errors), errors)


class AttributionNoneContractTests(unittest.TestCase):
    """attribution_none: at level NONE the link resolve goes out stripped and
    marked, and no open follows it."""

    def _entries(self, fixture):
        return v.collapse_retries(v.parse_branch_logs(_fixture(fixture)))

    def test_an_open_fails_attribution_none(self):
        opened = next(e for e in self._entries("cold_https.txt") if e["uri"] == "/v3/events/open")
        entries = self._entries("attribution_none.txt") + [opened]
        errors = v.assert_contract(entries, v.contract_for("attribution_none"))
        self.assertIn("'/v3/events/open' must not be captured", " ".join(errors))

    def test_a_resolve_that_keeps_the_device_token_fails_attribution_none(self):
        entries = self._entries("attribution_none.txt")
        entries[0]["request"]["randomized_device_token"] = "2222222222222222222"
        errors = v.assert_contract(entries, v.contract_for("attribution_none"))
        self.assertIn("No '/v3/deeplink' request may carry 'randomized_device_token'", " ".join(errors))

    def test_a_failed_resolve_fails_attribution_none(self):
        errors = _quiet(
            v.validate_entries, self._entries("attribution_none.txt"),
            v.contract_for("attribution_none"), [], v.SCENARIO_LINK_MARKERS.get("attribution_none"),
        )
        self.assertTrue(any("none" in e for e in errors), errors)


class AttributionNoneRuleTests(unittest.TestCase):
    """One case per rule in the attribution_none contract, restated here on purpose so
    deleting a rule turns a test red. The contract is not in the warm rule table because
    it has no order pair and no open to template a surplus from."""

    def _entries(self):
        return v.collapse_retries(v.parse_branch_logs(_fixture("attribution_none.txt")))

    def _errors(self, entries):
        return v.assert_contract(entries, v.contract_for("attribution_none"))

    def test_the_contract_is_the_table(self):
        contract = v.contract_for("attribution_none")
        self.assertEqual(contract["counts"], {"/v3/deeplink": 1, "/v3/events/open": 0})
        self.assertEqual(contract["order"], ())
        self.assertEqual(
            contract["fields"],
            {"/v3/deeplink": {"tracking_disabled": 1, "randomized_device_token": 0,
                              "randomized_bundle_token": 0, "hardware_id": 0, "anon_id": 0}},
        )

    def test_a_second_resolve_fails_the_deeplink_count(self):
        entries = self._entries()
        errors = self._errors(entries + [dict(entries[0])])
        self.assertTrue(any(e.startswith("Expected 1 '/v3/deeplink'") for e in errors), errors)

    def test_no_resolve_fails_the_deeplink_count(self):
        errors = self._errors([])
        self.assertTrue(any(e.startswith("Expected 1 '/v3/deeplink'") for e in errors), errors)

    def test_each_identifier_that_must_be_stripped_fails_when_present(self):
        for field in ("randomized_bundle_token", "hardware_id", "anon_id"):
            with self.subTest(field=field):
                entries = self._entries()
                entries[0]["request"][field] = "x"
                errors = self._errors(entries)
                self.assertTrue(any(f"'{field}'" in e for e in errors), errors)

    def test_a_resolve_not_marked_tracking_disabled_fails(self):
        entries = self._entries()
        entries[0]["request"].pop("tracking_disabled")
        errors = self._errors(entries)
        self.assertTrue(any("'tracking_disabled'" in e for e in errors), errors)


if __name__ == "__main__":
    unittest.main()
