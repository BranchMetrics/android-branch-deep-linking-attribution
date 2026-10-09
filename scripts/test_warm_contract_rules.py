"""One test per kind of rule in the warm and hot contracts, so deleting any rule turns a test red.

Run from the repo root:

    python -m unittest scripts.test_warm_contract_rules

Each case starts from the scenario's own fixture, which satisfies its contract, breaks
exactly the property one rule states, and asserts the contract names it. The failure
class this catches and no other test does is a single rule being deleted or weakened:
test_scenario_contracts checks that a fixture passes and that a few chosen defects fail,
so a contract with one rule removed still passes both. That is why this overlaps them on
purpose and restates the expected rules below instead of reading them from the contract."""

import os
import sys
import unittest

THIS_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, THIS_DIR)

import validate_l1_logs as v  # noqa: E402

WARM = {
    "warm_https_onNewIntent": "warm_https_onNewIntent.txt",
    "warm_uriScheme": "warm_uriScheme.txt",
}
# Every contract whose rules are tabled below: the two warm ones and hot_uriScheme.
RULED = {**WARM, "hot_uriScheme": "hot_uriScheme.txt"}


# The rules each warm contract is expected to hold, restated on purpose. The cases below
# break these properties, so deleting a rule from the contract has to turn something red;
# iterating the contract itself would delete the rule and its test together.
COUNTS = {
    "warm_https_onNewIntent": {
        "/v3/deeplink": 2,
        "/v3/events/open": 2,
        "/v1/url": 1,
        "/v3/events/custom": 2,
        "/v1/install": 0,
    },
    "warm_uriScheme": {
        "/v3/deeplink": 2,
        "/v3/events/open": 2,
        "/v1/url": 1,
        "/v3/events/custom": 2,
        "/v1/install": 0,
    },
    "hot_uriScheme": {
        "/v3/deeplink": 1,
        "/v3/events/open": 1,
    },
}
ORDER = (("/v3/deeplink", "/v3/events/open"),)
FIELDS = {
    "warm_https_onNewIntent": {
        "/v3/events/open": {"randomized_bundle_token": 2},
        "/v3/events/open[-1]": {"link_data": 1},
        "/v1/url": {"hardware_id": 0},
        "/v3/deeplink": {"android_app_link_url": 1, "external_intent_uri": 1},
    },
    "warm_uriScheme": {
        "/v3/events/open": {"randomized_bundle_token": 2},
        "/v1/url": {"hardware_id": 0},
        "/v3/deeplink": {"android_app_link_url": 0, "external_intent_uri": 1},
    },
    "hot_uriScheme": {
        "/v3/deeplink": {"android_app_link_url": 0, "external_intent_uri": 1},
    },
}


def _entries(scenario):
    path = os.path.join(THIS_DIR, "fixtures", RULED[scenario])
    return v.collapse_retries(v.parse_branch_logs(path))


def _count_error(errors, endpoint, expected):
    """The count rule's own message, not a field rule that happens to name the endpoint."""
    if expected == 0:
        return any(e.startswith(f"'{endpoint}' must not be captured") for e in errors)
    return any(e.startswith(f"Expected {expected} '{endpoint}'") for e in errors)


def _errors(entries, scenario):
    return v.assert_contract(entries, v.contract_for(scenario))


class WarmContractsAreTheTable(unittest.TestCase):
    def test_the_table_above_is_the_contract(self):
        # A rule added to a contract has to be added to the table, which is where
        # the cases that break it are generated from.
        for scenario in RULED:
            contract = v.contract_for(scenario)
            with self.subTest(scenario=scenario):
                self.assertEqual(contract["counts"], COUNTS[scenario])
                self.assertEqual(contract["order"], ORDER)
                self.assertEqual(contract["fields"], FIELDS[scenario])


class WarmCountRules(unittest.TestCase):
    def test_one_request_too_many_or_too_few_fails_each_count(self):
        # Every endpoint in `counts`, in both directions. A zero is only broken
        # upward, and /v1/install is a zero the beta can never send, so the extra
        # request is a copy of another entry under that name.
        for scenario in RULED:
            for endpoint, expected in COUNTS[scenario].items():
                entries = _entries(scenario)
                template = entries[0]
                mine = [e for e in entries if e["uri"] == endpoint]
                with self.subTest(scenario=scenario, endpoint=endpoint, change="extra"):
                    extra = entries + [dict(mine[0] if mine else template, uri=endpoint)]
                    self.assertTrue(
                        _count_error(_errors(extra, scenario), endpoint, expected),
                        f"a surplus {endpoint} must fail {scenario}",
                    )
                if expected:
                    with self.subTest(scenario=scenario, endpoint=endpoint, change="missing"):
                        fewer = [e for e in entries if e["uri"] != endpoint]
                        self.assertTrue(
                            _count_error(_errors(fewer, scenario), endpoint, expected),
                            f"no {endpoint} at all must fail {scenario}",
                        )


class WarmOrderRules(unittest.TestCase):
    def test_every_open_before_every_deeplink_fails(self):
        # The one order pair is (deeplink, open). Opens first, deeplinks after,
        # keeps every count intact, so only the order rule can see it.
        for scenario in RULED:
            entries = _entries(scenario)
            opens = [e for e in entries if e["uri"] == "/v3/events/open"]
            rest = [e for e in entries if e["uri"] != "/v3/events/open"]
            deeplinks = [e for e in rest if e["uri"] == "/v3/deeplink"]
            others = [e for e in rest if e["uri"] != "/v3/deeplink"]
            reordered = opens + others + deeplinks
            with self.subTest(scenario=scenario):
                errors = _errors(reordered, scenario)
                self.assertTrue(
                    any("after a '/v3/deeplink'" in e for e in errors), errors
                )


def _selected(entries, endpoint_key):
    """The requests a `fields` key judges: its endpoint, narrowed by a position."""
    endpoint, position = v.split_position(endpoint_key)
    matching = [e for e in entries if e["uri"] == endpoint]
    return matching if position is None else [matching[position]]


class WarmFieldRules(unittest.TestCase):
    def test_each_field_rule_fails_when_its_count_is_broken(self):
        # For every (endpoint, field, expected): strip the field from the requests
        # the rule judges (fails any expected above zero), and set it on all of
        # them (fails any expected below the number of requests).
        for scenario in RULED:
            for endpoint, rules in FIELDS[scenario].items():
                for field, expected in rules.items():
                    size = len(_selected(_entries(scenario), endpoint))
                    needle = f"'{field}'"
                    if expected > 0:
                        entries = _entries(scenario)
                        for e in _selected(entries, endpoint):
                            e["request"].pop(field, None)
                        with self.subTest(scenario=scenario, endpoint=endpoint, field=field, change="strip"):
                            errors = _errors(entries, scenario)
                            self.assertTrue(any(needle in e and endpoint in e for e in errors), errors)
                    if expected < size:
                        entries = _entries(scenario)
                        for e in _selected(entries, endpoint):
                            e["request"][field] = "x"
                        with self.subTest(scenario=scenario, endpoint=endpoint, field=field, change="set"):
                            errors = _errors(entries, scenario)
                            self.assertTrue(any(needle in e and endpoint in e for e in errors), errors)


class WarmLinkDataPosition(unittest.TestCase):
    """link_data belongs to the tapped link's open, the last of the two, not to
    either open. A count of one of two cannot say which, so the rule is positional."""

    def _opens(self, entries):
        return [e for e in entries if e["uri"] == "/v3/events/open"]

    def test_link_data_only_on_the_bare_launch_open_fails(self):
        # The bare launch's open carries it and the tap's open lost it. One open
        # of two still carries link_data, so a count alone passes this.
        entries = _entries("warm_https_onNewIntent")
        opens = self._opens(entries)
        opens[0]["request"]["link_data"] = opens[-1]["request"].pop("link_data")
        errors = _errors(entries, "warm_https_onNewIntent")
        self.assertTrue(any("link_data" in e for e in errors), errors)

    def test_link_data_on_both_opens_passes(self):
        # Extra attribution on the bare launch is not the defect this guards.
        entries = _entries("warm_https_onNewIntent")
        opens = self._opens(entries)
        opens[0]["request"]["link_data"] = opens[-1]["request"]["link_data"]
        self.assertEqual(_errors(entries, "warm_https_onNewIntent"), [])

    def test_the_real_fixture_carries_it_on_the_tap_only(self):
        opens = self._opens(_entries("warm_https_onNewIntent"))
        self.assertNotIn("link_data", opens[0]["request"])
        self.assertIn("link_data", opens[-1]["request"])

    def test_a_position_with_no_request_fails_instead_of_passing(self):
        entries = [e for e in _entries("warm_https_onNewIntent") if e["uri"] != "/v3/events/open"]
        errors = _errors(entries, "warm_https_onNewIntent")
        self.assertTrue(any("/v3/events/open[-1]" in e for e in errors), errors)


class WarmScenariosAreSeparated(unittest.TestCase):
    def test_an_https_delivery_fails_hot_uriScheme(self):
        # An https link rides external_intent_uri as well as android_app_link_url, so
        # the hot contract needs android_app_link_url at 0 to refuse it. The tapped
        # link's own deeplink and open, taken from the https capture, are that delivery.
        entries = _entries("warm_https_onNewIntent")
        deeplink = [e for e in entries if e["uri"] == "/v3/deeplink"][-1]
        open_ = [e for e in entries if e["uri"] == "/v3/events/open"][-1]
        errors = _errors([deeplink, open_], "hot_uriScheme")
        self.assertTrue(any("android_app_link_url" in e for e in errors), errors)

    def test_an_https_link_on_the_hot_fixture_fails_hot_uriScheme(self):
        entries = _entries("hot_uriScheme")
        deeplink = next(e for e in entries if e["uri"] == "/v3/deeplink")
        link = "https://bnctestbed.test-app.link/fixture-link"
        deeplink["request"]["android_app_link_url"] = link
        deeplink["request"]["external_intent_uri"] = link
        errors = _errors(entries, "hot_uriScheme")
        self.assertTrue(any("android_app_link_url" in e for e in errors), errors)

    def test_each_warm_capture_fails_the_other_warm_contract(self):
        # The two share counts and order. What tells them apart is the entry
        # point: an https link rides android_app_link_url, a scheme link does not.
        for capture, contract in (
            ("warm_https_onNewIntent", "warm_uriScheme"),
            ("warm_uriScheme", "warm_https_onNewIntent"),
        ):
            with self.subTest(capture=capture, contract=contract):
                errors = _errors(_entries(capture), contract)
                self.assertTrue(any("android_app_link_url" in e for e in errors), errors)

    def test_a_returning_device_with_no_token_fails_both_warm_scenarios(self):
        # On this beta an install is an open with no token, and these scenarios
        # exist for a device that already has one.
        for scenario in WARM:
            entries = _entries(scenario)
            for e in entries:
                if e["uri"] == "/v3/events/open":
                    e["request"].pop("randomized_bundle_token", None)
            with self.subTest(scenario=scenario):
                errors = _errors(entries, scenario)
                self.assertTrue(any("randomized_bundle_token" in e for e in errors), errors)


if __name__ == "__main__":
    unittest.main()
