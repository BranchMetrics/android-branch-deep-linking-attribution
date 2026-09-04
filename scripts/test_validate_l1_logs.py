"""Unit tests for the L1 wire-validation script.

Run from the repo root:

    python -m unittest scripts.test_validate_l1_logs
"""

import io
import os
import sys
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
