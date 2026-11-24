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


# Every contract in the registry must appear here, and every entry must name a
# file that exists. That binding is what the registry guard checks.
SCENARIO_FIXTURES = {
    "organic_open": "organic_open.txt",
    "cold_firstInstall": "cold_firstInstall.txt",
    "cold_https": "cold_https.txt",
    "link_generation": "link_generation.txt",
    "none_referrer_install": "none_referrer_install.txt",
    "none_referrer_install_reduced": "none_referrer_install_reduced.txt",
    "none_referrer_browser_return": "none_referrer_browser_return.txt",
    "none_referrer_background_death": "none_referrer_background_death.txt",
    "none_referrer_foreground_death": "none_referrer_foreground_death.txt",
    "none_referrer_foreground_death_uri_call": "none_referrer_foreground_death_uri_call.txt",
    "none_referrer_second_link": "none_referrer_second_link.txt",
    "none_referrer_app_passes_click_id": "none_referrer_app_passes_click_id.txt",
    "none_organic_install": "none_organic_install.txt",
}


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
        entries = v.parse_branch_logs(_fixture("happy_path.txt"))
        self.assertEqual(len(entries), 2)
        self.assertEqual(entries[0]["uri"], "/v1/install")
        self.assertEqual(entries[1]["uri"], "/v1/open")


class HappyPathTests(unittest.TestCase):
    def test_no_errors(self):
        errors, _ = _run_validation("happy_path.txt")
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
    """The global rule requiring /v1/install in every capture is gone.

    A correct beta capture contains no install at all: on 6.0.0-beta.0 the first launch resolves
    through requestDeepLinkData and the open goes to /v3/events/open. The old rule failed every
    correct beta run. No existing test covered the absence of that rule, which is why removing it
    could have gone unnoticed.
    """

    def test_capture_without_install_is_valid(self):
        errors, _ = _run_validation("no_install.txt")
        self.assertEqual(
            [], errors, f"A capture without an install must be valid, got: {errors}"
        )


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


class ContractRegistryTests(unittest.TestCase):
    """The binding between a contract and the capture it was written from."""

    def test_every_contract_names_a_fixture_that_exists(self):
        # Replaces the empty-registry pin, which died the moment a contract
        # existed, and the filename guess that replaced it. An explicit map
        # is what makes "written from a measurement" checkable: a contract
        # added from the ticket text has nothing to point at here.
        for name in v.SCENARIO_CONTRACTS:
            self.assertIn(name, SCENARIO_FIXTURES, f"contract '{name}' has no fixture mapping")
            self.assertTrue(
                os.path.exists(_fixture(SCENARIO_FIXTURES[name])),
                f"contract '{name}' maps to a missing fixture",
            )

    def test_the_map_holds_no_entry_without_a_contract(self):
        # The other direction. A fixture mapping left behind after its
        # contract was removed is dead weight that reads as coverage.
        for name in SCENARIO_FIXTURES:
            self.assertIn(name, v.SCENARIO_CONTRACTS, f"'{name}' maps a fixture to no contract")

    def test_every_link_marker_names_a_contract(self):
        for name in v.SCENARIO_LINK_MARKERS:
            self.assertIn(name, v.SCENARIO_CONTRACTS, f"marker '{name}' has no contract")

    def test_every_marker_stamps_its_own_name(self):
        # The value, not the key. A marker stamping a name other than its own
        # fails only on a live emulator run, where the SDK stamps the key.
        for name, marker in v.SCENARIO_LINK_MARKERS.items():
            with self.subTest(scenario=name):
                self.assertEqual(marker["l1_scenario"], name)

    def test_contracts_carry_only_the_keys_ios_has(self):
        for name, contract in v.SCENARIO_CONTRACTS.items():
            with self.subTest(scenario=name):
                self.assertEqual(set(contract), {"counts", "order", "fields"})


class ScenarioContractTests(unittest.TestCase):
    """organic_open is a measured capture less the EMT-4136 duplicate open.
    cold_firstInstall, cold_https and link_generation are cold
    captures."""

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
            if v.contract_for(scenario)["counts"].get("/v3/deeplink") == 0:
                continue  # Nothing to remove: the contract forbids the endpoint.
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

    def test_hardware_id_on_link_creation_fails_link_generation(self):
        # The EMT-4199 signal. /v1/url lives only in the generation capture.
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

    def test_an_open_without_link_data_fails_each_cold_scenario(self):
        # The open's link_data credits the link; without it the deep link still
        # works, so nothing else in the contract notices.
        for scenario in ("cold_firstInstall", "cold_https"):
            entries = self._entries(scenario)
            for e in entries:
                if e["uri"] == "/v3/events/open":
                    e["request"].pop("link_data")
            with self.subTest(scenario=scenario):
                errors = v.assert_contract(entries, v.contract_for(scenario))
                self.assertTrue(any("link_data" in e for e in errors), errors)

    def test_an_open_with_link_data_fails_each_launch_without_a_link(self):
        # api-open credits any link_data an open carries, so a launch with no
        # link must not send one.
        stale = {"+clicked_branch_link": True, "~referring_link": "https://bnctestbed.test-app.link/stale"}
        for scenario in ("organic_open", "link_generation"):
            entries = self._entries(scenario)
            for e in entries:
                if e["uri"] == "/v3/events/open":
                    e["request"]["link_data"] = stale
            with self.subTest(scenario=scenario):
                errors = v.assert_contract(entries, v.contract_for(scenario))
                self.assertTrue(any("link_data" in e for e in errors), errors)

    def test_each_cold_open_credits_its_own_link(self):
        for scenario in ("cold_firstInstall", "cold_https"):
            with self.subTest(scenario=scenario):
                expected = v.SCENARIO_LINK_MARKERS[scenario]
                self.assertEqual(v.assert_open_link(self._entries(scenario), expected), [])

    def test_the_run_id_joins_the_marker(self):
        self.assertEqual(
            v.marker_for("cold_https", "fixture-run"),
            {"l1_scenario": "cold_https", "l1_run_id": "fixture-run"},
        )
        self.assertEqual(v.marker_for("cold_https"), v.SCENARIO_LINK_MARKERS["cold_https"])
        self.assertIsNone(v.marker_for("organic_open", "fixture-run"))

    def test_each_cold_scenario_passes_with_its_own_run_id(self):
        for scenario in ("cold_firstInstall", "cold_https"):
            marker = v.marker_for(scenario, "fixture-run")
            with self.subTest(scenario=scenario):
                self.assertEqual(v.assert_resolved(self._resolved(scenario), marker), [])
                self.assertEqual(v.assert_open_link(self._entries(scenario), marker), [])

    def test_a_link_from_another_run_fails(self):
        # Runs repeat with the same scenario names, so only the run id tells
        # this run's link from one left over by an earlier run.
        for scenario in ("cold_firstInstall", "cold_https"):
            marker = v.marker_for(scenario, "a-later-run")
            with self.subTest(scenario=scenario):
                resolved_errors = v.assert_resolved(self._resolved(scenario), marker)
                open_errors = v.assert_open_link(self._entries(scenario), marker)
                self.assertTrue(any("l1_run_id" in e for e in resolved_errors), resolved_errors)
                self.assertTrue(any("l1_run_id" in e for e in open_errors), open_errors)

    def test_an_open_crediting_another_link_fails_validation(self):
        # The app got its own link, but the open credits a different one: the
        # resolved rule passes, so only the open's link_data can catch it.
        entries = self._entries("cold_https")
        for e in entries:
            if e["uri"] == "/v3/events/open":
                e["request"]["link_data"] = dict(e["request"]["link_data"], l1_scenario="cold_firstInstall")
        errors = v.validate_entries(
            entries,
            v.contract_for("cold_https"),
            self._resolved("cold_https"),
            v.SCENARIO_LINK_MARKERS["cold_https"],
        )
        self.assertTrue(any("open whose link_data" in e for e in errors), errors)


class ReferrerScenarioTests(unittest.TestCase):
    """The install-referrer scenarios. Fixtures are emulator captures with the click id, run id, key and
    device ids replaced by fixed values."""

    CLICK_ID = "1111111111111111111"
    RUN_ID = "fixture-run"

    def _entries(self, scenario):
        return v.collapse_retries(v.parse_branch_logs(_fixture(SCENARIO_FIXTURES[scenario])))

    def _resolved(self, scenario):
        return v.parse_resolved_params(_fixture(SCENARIO_FIXTURES[scenario]))

    def _referrer_errors(self, scenario, entries=None, resolved=None, run_id=RUN_ID, click_id=CLICK_ID):
        return v.assert_referrer_scenario(
            self._entries(scenario) if entries is None else entries,
            self._resolved(scenario) if resolved is None else resolved,
            scenario, run_id, click_id,
        )

    def _first(self, entries, uri):
        return next(e for e in entries if e["uri"] == uri)

    def test_every_referrer_scenario_has_a_contract(self):
        for name in v.SCENARIO_REFERRER_LINKS:
            self.assertIn(name, v.SCENARIO_CONTRACTS)

    def test_each_fixture_passes_the_referrer_check(self):
        for scenario in v.SCENARIO_REFERRER_LINKS:
            click_id = None if scenario == "none_organic_install" else self.CLICK_ID
            with self.subTest(scenario=scenario):
                self.assertEqual(self._referrer_errors(scenario, click_id=click_id), [])

    def test_each_fixture_passes_the_per_request_check(self):
        # Their /v3/deeplink has no local_ip, anon_id, first_install_time or is_hardware_id_real.
        for scenario in v.SCENARIO_REFERRER_LINKS:
            with self.subTest(scenario=scenario):
                errors, _ = _run_validation(SCENARIO_FIXTURES[scenario])
                self.assertEqual(errors, [])

    def test_without_tracking_disabled_the_stripped_fields_are_required(self):
        entry = self._first(self._entries("none_referrer_install"), "/v3/deeplink")
        entry["request"].pop("tracking_disabled")
        with redirect_stdout(io.StringIO()):
            errors = v.validate_request(entry, 1, 1)
        self.assertTrue(any("local_ip" in e for e in errors), errors)

    def test_a_device_id_on_the_none_deeplink_fails(self):
        for field in ("hardware_id", "advertising_ids", "anon_id", "local_ip"):
            entries = self._entries("none_referrer_install")
            self._first(entries, "/v3/deeplink")["request"][field] = "sent"
            with self.subTest(field=field):
                errors = v.assert_contract(entries, v.contract_for("none_referrer_install"))
                self.assertTrue(any(field in e for e in errors), errors)

    def test_the_raw_referrer_on_the_none_deeplink_fails(self):
        entries = self._entries("none_referrer_install")
        self._first(entries, "/v3/deeplink")["request"]["install_referrer_extras"] = f"link_click_id={self.CLICK_ID}"
        errors = v.assert_contract(entries, v.contract_for("none_referrer_install"))
        self.assertTrue(any("install_referrer_extras" in e for e in errors), errors)

    def test_a_none_deeplink_without_the_click_id_fails(self):
        entries = self._entries("none_referrer_install")
        self._first(entries, "/v3/deeplink")["request"].pop("link_identifier")
        errors = v.assert_contract(entries, v.contract_for("none_referrer_install"))
        errors += self._referrer_errors("none_referrer_install", entries=entries)
        self.assertTrue(any("link_identifier" in e for e in errors), errors)

    def test_a_different_click_id_fails(self):
        errors = self._referrer_errors("none_referrer_install", click_id="999")
        for field in ("link_identifier", "install_referrer_extras", "link_click_id"):
            with self.subTest(field=field):
                self.assertTrue(any(field in e for e in errors), errors)

    def test_the_click_id_sent_again_fails_the_uri_call_scenario(self):
        scenario = "none_referrer_foreground_death_uri_call"
        entries = self._entries(scenario)
        first = self._first(entries, "/v3/deeplink")
        again = dict(first, request=dict(first["request"], branch_sdk_request_unique_id="sent-again"))
        errors = v.assert_contract(entries + [again], v.contract_for(scenario))
        self.assertTrue(any("/v3/deeplink" in e for e in errors), errors)

    def test_an_open_without_the_install_link_fails(self):
        for scenario in ("none_referrer_browser_return", "none_referrer_background_death",
                         "none_referrer_foreground_death"):
            entries = self._entries(scenario)
            self._first(entries, "/v3/events/open")["request"].pop("link_data")
            with self.subTest(scenario=scenario):
                errors = v.assert_contract(entries, v.contract_for(scenario))
                errors += self._referrer_errors(scenario, entries=entries)
                self.assertTrue(any("link_data" in e for e in errors), errors)

    def test_another_runs_links_fail(self):
        errors = self._referrer_errors("none_referrer_install", run_id="another-run")
        self.assertTrue(any("in that order" in e for e in errors), errors)
        self.assertTrue(any("link_data" in e for e in errors), errors)

    def test_the_second_link_must_follow_the_install_link(self):
        scenario = "none_referrer_second_link"
        errors = self._referrer_errors(scenario, resolved=list(reversed(self._resolved(scenario))))
        self.assertTrue(any("in that order" in e for e in errors), errors)

    def test_the_open_must_carry_the_newest_link(self):
        scenario = "none_referrer_second_link"
        entries = self._entries(scenario)
        consent_open = self._first(entries, "/v3/events/open")["request"]
        consent_open["link_data"] = dict(consent_open["link_data"], l1_link="install")
        errors = self._referrer_errors(scenario, entries=entries)
        self.assertTrue(any("'second' link" in e for e in errors), errors)

    def test_an_organic_open_with_a_link_fails(self):
        entries = self._entries("none_organic_install")
        linked = self._first(self._entries("none_referrer_install"), "/v3/events/open")
        entries = [linked if e["uri"] == "/v3/events/open" else e for e in entries]
        errors = v.assert_contract(entries, v.contract_for("none_organic_install"))
        errors += self._referrer_errors("none_organic_install", entries=entries, click_id=None)
        for field in ("link_data", "link_identifier"):
            with self.subTest(field=field):
                self.assertTrue(any(field in e for e in errors), errors)
