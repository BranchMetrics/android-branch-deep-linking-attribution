#!/usr/bin/env bash
#
# L1 instrumented test orchestration for the Branch SDK TestBed.
#
# This script is invoked by reactivecircus/android-emulator-runner@v2 from
# .github/workflows/sdk-l1-validation.yml. The action's `script:` input splits
# on newlines and runs each line as a separate `/usr/bin/sh -c <line>`, so
# variables, `set -eu`, and multi-line constructs do NOT survive between
# lines. Keeping the logic in a real bash file (single invocation) restores
# normal shell semantics (pipefail, multi-line if/then/else, variable scope).
#
# Usage:
#   ./scripts/run_l1_instrumented.sh "$MOBILEBOOST_API_KEY"
#
# The API key may also be supplied via the MOBILEBOOST_API_KEY env var; the
# positional argument takes precedence when both are present.

set -euo pipefail

MOBILEBOOST_API_KEY="${1:-${MOBILEBOOST_API_KEY:-}}"

TARGET_APK="Branch-SDK-TestBed/build/outputs/apk/debug/Branch-SDK-TestBed-debug.apk"
TEST_APK="Branch-SDK-GPTDriver/build/outputs/apk/debug/Branch-SDK-GPTDriver-debug.apk"
TARGET_PKG="io.branch.branchandroidtestbed"
TEST_PKG="io.branch.gptdriver"
RUNNER="androidx.test.runner.AndroidJUnitRunner"
TEST_CLASS="io.branch.gptdriver.tests.LinkCreationDeterministicTest"

adb wait-for-device
adb shell input keyevent 82 || true

echo "Installing target APK: $TARGET_APK"
adb install -r -t "$TARGET_APK"
echo "Installing test APK: $TEST_APK"
adb install -r -t "$TEST_APK"

# Run via am instrument directly (no orchestrator) so the target package
# remains installed after the run, allowing run-as to read its private files.
# -w = wait for completion and stream results to stdout
# -r = raw output (parseable) so we can grep the final status
INSTRUMENT_LOG=instrument.log
adb shell am instrument -w -r \
  -e class "$TEST_CLASS" \
  -e MOBILEBOOST_API_KEY "$MOBILEBOOST_API_KEY" \
  "$TEST_PKG/$RUNNER" | tee "$INSTRUMENT_LOG"

# `am instrument` exits 0 even when tests fail; inspect the output instead.
if grep -qE "^INSTRUMENTATION_CODE: -1$" "$INSTRUMENT_LOG" && \
   ! grep -qE "^INSTRUMENTATION_STATUS: stack=" "$INSTRUMENT_LOG"; then
  echo "Instrumentation reported success."
else
  echo "Instrumentation reported failures or did not complete cleanly."
  cat "$INSTRUMENT_LOG"
  exit 1
fi

# Pull logs while the target package is still installed.
adb shell "run-as $TARGET_PKG cat /data/user/0/$TARGET_PKG/files/branchlogs.txt" > branchlogs.txt
wc -l branchlogs.txt
