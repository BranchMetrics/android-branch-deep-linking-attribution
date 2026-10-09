"""One test per kind of rule in the warm contracts, so deleting any rule turns a test red.

Run from the repo root:

    python -m unittest scripts.test_warm_contract_rules

Each case starts from the scenario's own fixture, which satisfies its contract, breaks
exactly the property one rule states, and asserts the contract names it. A rule nothing
here breaks could be loosened or removed with every test still green; that was the state
of 19 of the 20 warm rules before this file."""

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
}
ORDER = (("/v3/deeplink", "/v3/events/open"),)
FIELDS = {
    "warm_https_onNewIntent": {
        "/v3/events/open": {"randomized_bundle_token": 2, "link_data": 1},
        "/v1/url": {"hardware_id": 0},
        "/v3/deeplink": {"android_app_link_url": 1, "external_intent_uri": 1},
    },
    "warm_uriScheme": {
        "/v3/events/open": {"randomized_bundle_token": 2},
        "/v1/url": {"hardware_id": 0},
        "/v3/deeplink": {"android_app_link_url": 0, "external_intent_uri": 1},
    },
}


def _entries(scenario):
    path = os.path.join(THIS_DIR, "fixtures", WARM[scenario])
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
        for scenario in WARM:
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
        for scenario in WARM:
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
        for scenario in WARM:
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


class WarmFieldRules(unittest.TestCase):
    def test_each_field_rule_fails_when_its_count_is_broken(self):
        # For every (endpoint, field, expected): strip the field from all of that
        # endpoint's requests (fails any expected above zero), and set it on all of
        # them (fails any expected below the number of requests).
        for scenario in WARM:
            for endpoint, rules in FIELDS[scenario].items():
                for field, expected in rules.items():
                    size = sum(1 for e in _entries(scenario) if e["uri"] == endpoint)
                    needle = f"'{field}'"
                    if expected > 0:
                        entries = _entries(scenario)
                        for e in entries:
                            if e["uri"] == endpoint:
                                e["request"].pop(field, None)
                        with self.subTest(scenario=scenario, endpoint=endpoint, field=field, change="strip"):
                            errors = _errors(entries, scenario)
                            self.assertTrue(any(needle in e and endpoint in e for e in errors), errors)
                    if expected < size:
                        entries = _entries(scenario)
                        for e in entries:
                            if e["uri"] == endpoint:
                                e["request"][field] = "x"
                        with self.subTest(scenario=scenario, endpoint=endpoint, field=field, change="set"):
                            errors = _errors(entries, scenario)
                            self.assertTrue(any(needle in e and endpoint in e for e in errors), errors)


class WarmScenariosAreSeparated(unittest.TestCase):
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
