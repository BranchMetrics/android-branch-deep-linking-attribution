"""The late_duplicate_intent contracts against their fixtures, a link credited twice, and a new tap lost.

Run from the repo root:

    python -m unittest scripts.test_late_duplicate_intent
"""

import io
import os
import sys
import unittest
from contextlib import redirect_stdout

THIS_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, THIS_DIR)

import validate_l1_logs as v  # noqa: E402
from l1_fixtures import SCENARIO_FIXTURES, fixture_path as _fixture  # noqa: E402

SCENARIOS = ("late_duplicate_intent", "late_duplicate_intent_uri_call")
DEEPLINK = "/v3/deeplink"
OPEN = "/v3/events/open"


class LateDuplicateIntentTests(unittest.TestCase):
    """Both fixtures are captures of local run_l1_late_duplicate_intent.sh runs on an API 34 emulator
    (sdk_gphone64_arm64), trimmed to the fields the validator reads, with the identifiers swapped for the
    placeholders the other fixtures use."""

    def _entries(self, scenario):
        return v.collapse_retries(v.parse_branch_logs(_fixture(SCENARIO_FIXTURES[scenario])))

    def _errors(self, scenario, entries):
        """What CI computes for the capture, the link marker included."""
        resolved = v.parse_resolved_params(_fixture(SCENARIO_FIXTURES[scenario]))
        with redirect_stdout(io.StringIO()):
            return v.validate_entries(entries, v.contract_for(scenario), resolved, v.marker_for(scenario))

    def _nth(self, entries, uri, index):
        return [e for e in entries if e["uri"] == uri][index]

    def _old_link(self, scenario):
        return f"https://bnctestbed.test-app.link/fixture-{scenario}"

    def test_each_fixture_passes_with_its_link_marker(self):
        for scenario in SCENARIOS:
            with self.subTest(scenario=scenario):
                self.assertEqual(self._errors(scenario, self._entries(scenario)), [])

    def test_a_rebuild_that_sends_the_old_link_fails(self):
        # After a turn of the phone (1) or after Android ended the app (3, 8, 12).
        for scenario in SCENARIOS:
            for index in (1, 3, 8, 12):
                entries = self._entries(scenario)
                self._nth(entries, DEEPLINK, index)["request"]["android_app_link_url"] = self._old_link(scenario)
                with self.subTest(scenario=scenario, index=index):
                    errors = self._errors(scenario, entries)
                    self.assertTrue(
                        any(f"'{DEEPLINK}[{index}]'" in e and "android_app_link_url" in e for e in errors), errors
                    )

    def test_a_rebuild_whose_open_credits_the_link_fails(self):
        for scenario in SCENARIOS:
            entries = self._entries(scenario)
            self._nth(entries, OPEN, 7)["request"]["link_data"] = {
                "+clicked_branch_link": True,
                "~referring_link": self._old_link(scenario),
                "l1_scenario": scenario,
            }
            with self.subTest(scenario=scenario):
                errors = self._errors(scenario, entries)
                self.assertTrue(any(f"'{OPEN}[7]'" in e and "link_data" in e for e in errors), errors)

    def test_a_skipped_open_for_a_new_tap_fails(self):
        # Link C's open, which the SDK used to skip while the rebuilt screen's open was waiting.
        for scenario in SCENARIOS:
            entries = self._entries(scenario)
            entries.remove(self._nth(entries, OPEN, 9))
            with self.subTest(scenario=scenario):
                errors = self._errors(scenario, entries)
                self.assertTrue(any(f"'{OPEN}' request(s), captured 13" in e for e in errors), errors)

    def test_a_new_tap_sent_without_its_link_fails(self):
        # Link D, tapped after Android ended an app opened from its icon.
        for scenario in SCENARIOS:
            entries = self._entries(scenario)
            self._nth(entries, OPEN, 13)["request"].pop("link_data")
            with self.subTest(scenario=scenario):
                errors = self._errors(scenario, entries)
                self.assertTrue(any(f"'{OPEN}[13]'" in e for e in errors), errors)

    def test_a_pair_sent_by_the_return_while_running_fails(self):
        # The return comes back with no new intent, so it sends nothing. A pair sent then sits after the second
        # turn's open.
        for scenario in SCENARIOS:
            entries = self._entries(scenario)
            at = entries.index(self._nth(entries, OPEN, 2)) + 1
            extra = []
            for uri, rid in ((DEEPLINK, "return-deeplink"), (OPEN, "return-open")):
                source = self._nth(entries, uri, 2)
                extra.append(dict(source, request=dict(source["request"], branch_sdk_request_unique_id=rid)))
            with self.subTest(scenario=scenario):
                errors = self._errors(scenario, entries[:at] + extra + entries[at:])
                self.assertTrue(any(f"'{DEEPLINK}' request(s), captured 15" in e for e in errors), errors)
                self.assertTrue(any(f"'{OPEN}' request(s), captured 15" in e for e in errors), errors)


if __name__ == "__main__":
    unittest.main()
