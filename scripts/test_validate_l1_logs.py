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

    def test_missing_wifi_fails_only_for_wifi(self):
        errors, _ = _run_validation("missing_wifi.txt")
        self.assertEqual(
            errors,
            ["Request 1 (/v1/install): missing required field 'wifi'"],
        )


class InstallRequiredTests(unittest.TestCase):
    def test_capture_without_install_fails(self):
        errors, _ = _run_validation("no_install.txt")
        self.assertTrue(
            any("'/v1/install' was not captured" in e for e in errors),
            f"Expected install-missing error, got: {errors}",
        )

    def test_capture_without_install_fails_only_for_install(self):
        errors, _ = _run_validation("no_install.txt")
        self.assertEqual(errors, ["Mandatory endpoint '/v1/install' was not captured."])


class MetaAnonIdTests(unittest.TestCase):
    def test_happy_path_has_meta_anon_id_on_install_and_open(self):
        entries = v.parse_branch_logs(_fixture("happy_path.txt"))
        for entry in entries:
            self.assertTrue(
                entry["request"]["anon_id"].startswith(v.META_ANON_ID_PREFIX),
                f"{entry['uri']} anon_id lacks Meta prefix",
            )

    def test_missing_anon_id_fails_with_named_error(self):
        errors, _ = _run_validation("missing_anon_id.txt")
        self.assertEqual(
            errors,
            ["Request 1 (/v1/install): missing required field 'anon_id'"],
        )

    def test_legacy_anon_id_fails_prefix_check(self):
        errors, _ = _run_validation("legacy_anon_id.txt")
        self.assertEqual(len(errors), 2, f"Expected install+open errors, got: {errors}")
        for err, uri in zip(errors, ["/v1/install", "/v1/open"]):
            self.assertIn(uri, err)
            self.assertIn("does not start with 'XZ'", err)


if __name__ == "__main__":
    unittest.main()
