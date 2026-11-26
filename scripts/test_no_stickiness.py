"""The no_stickiness contracts against their fixtures, and an old link coming back.

Run from the repo root:

    python -m unittest scripts.test_no_stickiness
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

OLD_LINK = "https://bnctestbed.test-app.link/fixture-no_stickiness"
OLD_LINK_DATA = {"+clicked_branch_link": True, "~referring_link": OLD_LINK, "l1_scenario": "no_stickiness"}


class NoStickinessTests(unittest.TestCase):
    """Both fixtures are captures of local N2NoStickinessWireTest runs on an API 34 emulator
    (sdk_gphone64_arm64), trimmed to the fields the validator reads, with the identifiers
    swapped for the placeholders the other fixtures use. no_stickiness_none's relaunch sent
    nothing, so its capture ends with the delivery."""

    def _entries(self, scenario):
        return v.collapse_retries(v.parse_branch_logs(_fixture(SCENARIO_FIXTURES[scenario])))

    def _errors(self, scenario, entries):
        """What CI computes for the capture, the link marker included."""
        resolved = v.parse_resolved_params(_fixture(SCENARIO_FIXTURES[scenario]))
        with redirect_stdout(io.StringIO()):
            return v.validate_entries(entries, v.contract_for(scenario), resolved, v.marker_for(scenario))

    def _last(self, entries, uri):
        return [e for e in entries if e["uri"] == uri][-1]

    def test_each_fixture_passes_with_its_link_marker(self):
        for scenario in ("no_stickiness", "no_stickiness_none"):
            with self.subTest(scenario=scenario):
                self.assertEqual(self._errors(scenario, self._entries(scenario)), [])

    def test_the_old_link_on_the_relaunch_deeplink_fails(self):
        for field in ("android_app_link_url", "external_intent_uri"):
            entries = self._entries("no_stickiness")
            self._last(entries, "/v3/deeplink")["request"][field] = OLD_LINK
            with self.subTest(field=field):
                errors = self._errors("no_stickiness", entries)
                self.assertTrue(any("'/v3/deeplink[-1]'" in e and field in e for e in errors), errors)

    def test_the_old_link_on_the_relaunch_open_fails(self):
        entries = self._entries("no_stickiness")
        self._last(entries, "/v3/events/open")["request"]["link_data"] = dict(OLD_LINK_DATA)
        errors = self._errors("no_stickiness", entries)
        self.assertTrue(any("'/v3/events/open[-1]'" in e and "link_data" in e for e in errors), errors)

    def test_a_deeplink_and_open_sent_by_the_return_fail(self):
        # The return comes back with no new intent, so it sends nothing. A pair sent then sits
        # between the delivery and the relaunch.
        entries = self._entries("no_stickiness")
        delivery_open = [e for e in entries if e["uri"] == "/v3/events/open"][1]
        at = entries.index(delivery_open) + 1
        extra = []
        for uri, rid in (("/v3/deeplink", "return-deeplink"), ("/v3/events/open", "return-open")):
            source = self._last(entries, uri)
            extra.append(dict(source, request=dict(source["request"], branch_sdk_request_unique_id=rid)))
        errors = self._errors("no_stickiness", entries[:at] + extra + entries[at:])
        self.assertTrue(any("'/v3/deeplink'" in e for e in errors), errors)
        self.assertTrue(any("'/v3/events/open'" in e for e in errors), errors)

    def test_a_relaunch_deeplink_at_none_fails(self):
        # At NONE a launch's deeplink is sent only when it carries a link, so one sent by the
        # relaunch means the old link stayed in prefs.
        entries = self._entries("no_stickiness_none")
        delivery = self._last(entries, "/v3/deeplink")
        relaunch = dict(delivery, request=dict(delivery["request"], branch_sdk_request_unique_id="relaunch"))
        errors = self._errors("no_stickiness_none", entries + [relaunch])
        self.assertTrue(any("'/v3/deeplink'" in e for e in errors), errors)

    def test_a_link_that_never_reached_the_open_fails_no_stickiness(self):
        # Without the delivery's credit the checks after it would pass on nothing.
        entries = self._entries("no_stickiness")
        [e for e in entries if e["uri"] == "/v3/events/open"][1]["request"].pop("link_data")
        errors = self._errors("no_stickiness", entries)
        self.assertTrue(any("'/v3/events/open[1]'" in e for e in errors), errors)


if __name__ == "__main__":
    unittest.main()
