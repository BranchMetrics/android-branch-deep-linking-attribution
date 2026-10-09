"""Shared paths for the L1 validator tests: the fixture directory and which fixture
holds which scenario's capture."""

import os

FIXTURE_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "fixtures")

# Every contract in the registry must appear here, and every entry must name a
# file that exists. That binding is what the registry guard checks.
SCENARIO_FIXTURES = {
    "organic_open": "organic_open.txt",
    "cold_firstInstall": "cold_firstInstall.txt",
    "cold_https": "cold_https.txt",
    "link_generation": "link_generation.txt",
    "warm_https_onNewIntent": "warm_https_onNewIntent.txt",
    "warm_uriScheme": "warm_uriScheme.txt",
}


def fixture_path(name):
    return os.path.join(FIXTURE_DIR, name)
