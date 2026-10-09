"""Guards over the committed scenario fixtures and the contract table.

Run from the repo root:

    python -m unittest scripts.test_scenario_artifact_guards
"""

import os
import re
import sys
import unittest

THIS_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, THIS_DIR)

import validate_l1_logs as v  # noqa: E402
from l1_fixtures import SCENARIO_FIXTURES, fixture_path as _fixture  # noqa: E402


class ScenarioArtifactGuards(unittest.TestCase):
    """Two mistakes this suite could have caught, turned into checks.

    A scenario fixture was once committed as the raw 658-line capture, carrying the
    emulator's identifiers and a live-shaped branch key, and only the neighbouring files
    revealed it. A scenario contract was once proposed identical to another's, which would
    have shipped a capture that asserted nothing the other did not, and only asking what it
    added revealed that. Neither needed a person.

    Scoped to the scenario fixtures. The harness fixtures under the same directory are
    hand-written inputs for the field-presence tests, not captures, and are deliberately
    outside this shape."""

    FIXTURE_BRANCH_KEY = "key_live_fixtureFixtureFixtureFi"

    # What a scenario fixture may carry in the fields that identify a device or a link.
    # Placeholders only: this repo is public, and a capture comes off a real emulator.
    PLACEHOLDERS = {
        "anon_id": {"fixture-anon-id"},
        "hardware_id": {"00000000fixture"},
        "randomized_bundle_token": {"1111111111111111111"},
        "randomized_device_token": {"2222222222222222222"},
        # The emulator's own address, and organic_open's older placeholder. Neither
        # routes anywhere.
        "local_ip": {"10.0.2.16", "10.0.0.1"},
    }
    # Request ids are unique per request, so they are a pattern, not a set: the zero UUID
    # with a counter in the last group, then the capture's date-hour suffix.
    PLACEHOLDER_PATTERNS = {
        "branch_sdk_request_unique_id": re.compile(r"00000000-0000-4000-8000-0{8}\d{4}-\d{10}"),
    }
    LINK_PREFIXES = ("https://bnctestbed.test-app.link/fixture-", "branchtest://")

    def _keep_set(self):
        """Derived from the validator, not restated here, so the two cannot drift.

        Plus app_version, the one name the validator never reads (the cold fixtures
        already carried it). Every other kept field is one a rule or a required list
        names, external_intent_uri and link_data among them."""
        keep = set()
        for name in dir(v):
            if not name.startswith("REQUIRED"):
                continue
            value = getattr(v, name)
            if isinstance(value, list):
                keep |= {str(x) for x in value}
            elif isinstance(value, dict):
                for inner in value.values():
                    if isinstance(inner, list):
                        keep |= {str(x) for x in inner}
        for contract in v.SCENARIO_CONTRACTS.values():
            for rules in contract["fields"].values():
                keep |= set(rules)
        return keep | {"app_version"}

    def _lines(self, fixture_name):
        with open(_fixture(fixture_name), encoding="utf-8") as fh:
            return [line for line in fh.read().splitlines() if line.strip()]

    def _payloads(self, fixture_name):
        """Parsed with the validator's own parser. Never empty: a parser that stopped
        matching would otherwise make every guard below pass on nothing."""
        payloads = [e["request"] for e in v.parse_branch_logs(_fixture(fixture_name))]
        postings = [l for l in self._lines(fixture_name) if l.startswith(v.POSTING_PREFIX)]
        self.assertGreater(len(payloads), 0, f"{fixture_name} parsed to no requests")
        self.assertEqual(
            len(payloads), len(postings), f"{fixture_name}: a request did not parse"
        )
        return payloads

    def test_scenario_fixtures_hold_only_wire_pairs(self):
        # What a raw capture fails: log lines the validator never reads.
        for scenario, fixture in SCENARIO_FIXTURES.items():
            lines = self._lines(fixture)
            stray = [l for l in lines if not l.startswith((v.POSTING_PREFIX, v.POST_VALUE_PREFIX, v.RESOLVED_PREFIX))]
            with self.subTest(scenario=scenario):
                self.assertGreater(len(lines), 0, f"{fixture} is empty")
                self.assertEqual(stray, [], f"{fixture} holds lines that are not wire pairs")

    def test_scenario_fixtures_carry_no_field_outside_the_keep_set(self):
        keep = self._keep_set()
        for scenario, fixture in SCENARIO_FIXTURES.items():
            extra = set()
            for payload in self._payloads(fixture):
                extra |= set(payload) - keep
            with self.subTest(scenario=scenario):
                self.assertEqual(
                    extra, set(), f"{fixture} carries fields the validator never reads: {sorted(extra)}"
                )

    def test_scenario_fixtures_carry_only_the_branch_key_constant(self):
        # The one that would have caught a test-mode key going into a public repo.
        for scenario, fixture in SCENARIO_FIXTURES.items():
            payloads = self._payloads(fixture)
            keys = [p.get("branch_key") for p in payloads]
            with self.subTest(scenario=scenario):
                self.assertEqual(
                    keys, [self.FIXTURE_BRANCH_KEY] * len(payloads),
                    f"{fixture}: every request must carry the fixture branch key and no other",
                )

    def test_scenario_fixtures_carry_only_placeholder_values(self):
        # The names alone do not make a capture safe to commit: an emulator's
        # hardware_id, anon_id and tokens ride in fields the keep set allows.
        for scenario, fixture in SCENARIO_FIXTURES.items():
            for index, payload in enumerate(self._payloads(fixture)):
                for field, allowed in self.PLACEHOLDERS.items():
                    if field in payload:
                        with self.subTest(scenario=scenario, request=index, field=field):
                            self.assertIn(payload[field], allowed, f"{fixture}: {field} is not a placeholder")
                for field, pattern in self.PLACEHOLDER_PATTERNS.items():
                    if field in payload:
                        with self.subTest(scenario=scenario, request=index, field=field):
                            self.assertRegex(payload[field], pattern, f"{fixture}: {field} is not a placeholder")
                for field in ("android_app_link_url", "external_intent_uri"):
                    if field in payload:
                        with self.subTest(scenario=scenario, request=index, field=field):
                            self.assertTrue(
                                payload[field].startswith(self.LINK_PREFIXES),
                                f"{fixture}: {field} is not a fixture link",
                            )
                if "link_data" in payload:
                    with self.subTest(scenario=scenario, request=index, field="link_data"):
                        link_data = payload["link_data"]
                        self.assertLessEqual(
                            set(link_data), {"+clicked_branch_link", "~referring_link"},
                            f"{fixture}: link_data carries more than the two keys the rule reads",
                        )
                        self.assertTrue(link_data["~referring_link"].startswith(self.LINK_PREFIXES))

    def test_scenario_fixtures_resolve_only_fixture_runs(self):
        # The "Deep link params:" lines are logged by the TestBed from the resolved
        # link, so they can carry whatever the link carried.
        for scenario, fixture in SCENARIO_FIXTURES.items():
            for params in v.parse_resolved_params(_fixture(fixture)):
                with self.subTest(scenario=scenario):
                    self.assertLessEqual(set(params), {"+clicked_branch_link", "l1_run_id", "l1_scenario"})
                    self.assertEqual(params.get("l1_run_id"), "fixture-run")

    def test_no_two_scenarios_share_a_contract(self):
        # A scenario whose contract equals another's asserts nothing that one does not,
        # however different the driver looks. The two warm scenarios are the near miss
        # this exists for: same counts, same order, separated only by which field
        # carries the URI.
        names = sorted(v.SCENARIO_CONTRACTS)
        for i, first in enumerate(names):
            for second in names[i + 1:]:
                with self.subTest(pair=f"{first}/{second}"):
                    self.assertNotEqual(
                        v.SCENARIO_CONTRACTS[first],
                        v.SCENARIO_CONTRACTS[second],
                        f"{first} and {second} carry the same contract",
                    )


if __name__ == "__main__":
    unittest.main()
