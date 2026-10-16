"""Each scenario contract against the fixture it was written from, and its negatives.

Run from the repo root:

    python -m unittest scripts.test_scenario_contracts
"""

import os
import sys
import unittest

THIS_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, THIS_DIR)

import validate_l1_logs as v  # noqa: E402
from l1_fixtures import SCENARIO_FIXTURES, fixture_path as _fixture  # noqa: E402


class ScenarioContractTests(unittest.TestCase):
    """organic_open is a measured capture less the EMT-4136 duplicate open.
    cold_firstInstall, cold_https and link_generation are cold captures.

    The two warm fixtures are the real requests of the passing Layer 1 run on 3eb17bc0
    (sdk-l1-validation.yml, workflow_dispatch, API 30, sdk_gphone_x86_64), read back from the
    job log of its validate-wire-logs job. Nothing was added by hand. What was removed:
    every field the validator never reads; and the identifiers were replaced with the
    placeholders the other fixtures use (hardware_id, anon_id, both tokens, branch key,
    request ids, the link, which is also all that link_data keeps).
    Each warm scenario carries two opens, and an https link carries both
    android_app_link_url and external_intent_uri.
    The only thing a warm launch does that a cold one does not is background and
    foreground the app; that is a coincidence these fixtures record, not a cause
    they establish."""

    def _entries(self, scenario):
        path = _fixture(SCENARIO_FIXTURES[scenario])
        return v.collapse_retries(v.parse_branch_logs(path))

    def _resolved(self, scenario):
        return v.parse_resolved_params(_fixture(SCENARIO_FIXTURES[scenario]))

    def _errors(self, capture_scenario, contract_scenario):
        return v.assert_contract(
            self._entries(capture_scenario), v.contract_for(contract_scenario)
        )

    def test_each_fixture_satisfies_its_own_contract(self):
        for scenario in SCENARIO_FIXTURES:
            with self.subTest(scenario=scenario):
                errors = self._errors(scenario, scenario)
                self.assertEqual(errors, [], f"{scenario}: {errors}")

    def test_each_count_is_a_fact_about_its_fixture(self):
        # The check that would catch a contract written from the plan text
        # rather than from a capture.
        for scenario in SCENARIO_FIXTURES:
            uris = [e["uri"] for e in self._entries(scenario)]
            for endpoint, expected in v.contract_for(scenario)["counts"].items():
                with self.subTest(scenario=scenario, endpoint=endpoint):
                    self.assertEqual(uris.count(endpoint), expected)

    def test_the_duplicate_open_fails_every_scenario(self):
        # The negative control these contracts exist for. Putting the EMT-4136
        # duplicate back is exactly the wire as it stands today, so each
        # contract must reject it. If one of these ever passes, the contract
        # has drifted back onto the defect.
        for scenario in SCENARIO_FIXTURES:
            entries = self._entries(scenario)
            first_open = next(e for e in entries if e["uri"] == "/v3/events/open")
            duplicated = entries + [dict(first_open, request=dict(first_open["request"]))]
            with self.subTest(scenario=scenario):
                errors = v.assert_contract(duplicated, v.contract_for(scenario))
                self.assertTrue(
                    any("/v3/events/open" in e for e in errors),
                    f"{scenario} accepted the duplicate open: {errors}",
                )

    def test_a_missing_endpoint_fails_every_scenario(self):
        # The counterpart to the duplicate-open case: too few is a defect the
        # same way too many is. Carried over from the harness contract's
        # coverage, which this class replaces.
        for scenario in SCENARIO_FIXTURES:
            entries = [e for e in self._entries(scenario) if e["uri"] != "/v3/deeplink"]
            with self.subTest(scenario=scenario):
                errors = v.assert_contract(entries, v.contract_for(scenario))
                self.assertTrue(any("/v3/deeplink" in e for e in errors), errors)

    def test_a_first_install_that_reads_as_a_returning_device_fails_cold_firstInstall(self):
        # The Android shape of EMT-4027: nothing is treated as an install, so
        # every open carries the token. Counts and order are unchanged by that
        # defect, which is why the field rule has to exist.
        entries = self._entries("cold_firstInstall")
        for e in entries:
            if e["uri"] == "/v3/events/open":
                e["request"]["randomized_bundle_token"] = "1111111111111111111"
        errors = v.assert_contract(entries, v.contract_for("cold_firstInstall"))
        self.assertTrue(any("randomized_bundle_token" in e for e in errors), errors)

    def test_a_missing_token_fails_cold_https(self):
        entries = self._entries("cold_https")
        opens = [e for e in entries if e["uri"] == "/v3/events/open"]
        opens[0]["request"].pop("randomized_bundle_token")
        errors = v.assert_contract(entries, v.contract_for("cold_https"))
        self.assertTrue(any("randomized_bundle_token" in e for e in errors), errors)

    def test_cold_https_and_cold_firstInstall_are_separated_by_the_token(self):
        # Each capture must fail the other's contract on the token count.
        for capture, contract in (
            ("cold_firstInstall", "cold_https"),
            ("cold_https", "cold_firstInstall"),
        ):
            errors = self._errors(capture, contract)
            with self.subTest(capture=capture, contract=contract):
                self.assertTrue(any("randomized_bundle_token" in e for e in errors), errors)

    def test_an_install_fails_warm_https_onNewIntent(self):
        # The ticket's one explicit ask for this group: assert the absence of
        # install, because a presence-only check would not catch a warm launch
        # that emitted one. Zero in the contract is the assertion; this is the
        # proof it can fail.
        entries = self._entries("warm_https_onNewIntent")
        first = entries[0]
        with_install = entries + [dict(first, uri="/v1/install")]
        errors = v.assert_contract(with_install, v.contract_for("warm_https_onNewIntent"))
        self.assertTrue(
            any("/v1/install" in e for e in errors),
            f"an install in a warm capture must fail the contract, got: {errors}",
        )

    def test_a_tap_whose_open_lost_link_data_fails_warm_https_onNewIntent(self):
        # The tapped link is credited through link_data on its open. When the
        # deeplink request fails, RequestDeepLink sends the open without it, and
        # counts, order and the token all still read the same. Only this rule
        # sees it.
        entries = self._entries("warm_https_onNewIntent")
        opens = [e for e in entries if e["uri"] == "/v3/events/open"]
        self.assertIn("link_data", opens[-1]["request"], "the fixture's tap must carry link_data")
        opens[-1]["request"].pop("link_data")
        errors = v.assert_contract(entries, v.contract_for("warm_https_onNewIntent"))
        self.assertTrue(any("link_data" in e for e in errors), errors)

    def test_hardware_id_on_link_creation_fails_link_generation(self):
        # The EMT-4199 signal. link_generation is where /v1/url is the point of
        # the capture; the warm contracts count one as well.
        entries = self._entries("link_generation")
        for e in entries:
            if e["uri"] == "/v1/url":
                e["request"]["hardware_id"] = "something"
        errors = v.assert_contract(entries, v.contract_for("link_generation"))
        self.assertTrue(any("hardware_id" in e for e in errors), errors)

    def test_link_generation_inside_a_cold_capture_fails(self):
        # A /v1/url in cold_firstInstall or cold_https means the link was
        # generated in the process it was delivered to, which is the warm
        # shape these replaced.
        link = next(e for e in self._entries("link_generation") if e["uri"] == "/v1/url")
        for scenario in ("cold_firstInstall", "cold_https"):
            with self.subTest(scenario=scenario):
                errors = v.assert_contract(self._entries(scenario) + [link], v.contract_for(scenario))
                self.assertTrue(any("/v1/url" in e for e in errors), errors)

    def test_a_link_that_never_reached_the_sdk_fails(self):
        for scenario in ("cold_firstInstall", "cold_https"):
            entries = self._entries(scenario)
            for e in entries:
                e["request"].pop("android_app_link_url", None)
            with self.subTest(scenario=scenario):
                errors = v.assert_contract(entries, v.contract_for(scenario))
                self.assertTrue(any("android_app_link_url" in e for e in errors), errors)

    def test_each_cold_scenario_resolves_its_own_link(self):
        for scenario in ("cold_firstInstall", "cold_https"):
            with self.subTest(scenario=scenario):
                expected = v.SCENARIO_LINK_MARKERS[scenario]
                self.assertEqual(v.assert_resolved(self._resolved(scenario), expected), [])

    def test_a_shared_link_fails_the_resolved_rule(self):
        # One scenario's resolution judged against the other's marker: what a
        # link shared between the two would look like.
        for capture, marker in (
            ("cold_firstInstall", "cold_https"),
            ("cold_https", "cold_firstInstall"),
        ):
            with self.subTest(capture=capture, marker=marker):
                errors = v.assert_resolved(
                    self._resolved(capture), v.SCENARIO_LINK_MARKERS[marker]
                )
                self.assertTrue(any("l1_scenario" in e for e in errors), errors)

    def test_a_shared_link_fails_validation(self):
        # The same failure through validate_entries, the path main() takes.
        errors = v.validate_entries(
            self._entries("cold_https"),
            v.contract_for("cold_firstInstall"),
            self._resolved("cold_https"),
            v.SCENARIO_LINK_MARKERS["cold_firstInstall"],
        )
        self.assertTrue(any("l1_scenario" in e for e in errors), errors)

    def test_no_resolution_fails_the_resolved_rule(self):
        errors = v.assert_resolved([], v.SCENARIO_LINK_MARKERS["cold_firstInstall"])
        self.assertTrue(any("none" in e for e in errors), errors)

    def test_organic_open_forbids_nothing_it_did_not_measure(self):
        # organic_open carries no `fields` rule on purpose. The property the
        # scenario is about is that the open carries no link data, and the run
        # these were derived from reported the token rather than the link
        # payload. The counts still earn their place: they catch a second open
        # reappearing.
        self.assertEqual(v.contract_for("organic_open")["fields"], {})


if __name__ == "__main__":
    unittest.main()
