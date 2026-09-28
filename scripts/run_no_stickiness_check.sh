#!/usr/bin/env bash
#
# Runs the no_stickiness driver once and reports pass/fail/not_run as a step
# output. A real bash file for the same reason run_l1_instrumented.sh is one:
# the emulator-runner action's `script:` field splits on newlines and loses
# shell state between them.
#
# Never fails the job itself: writes result/reason and always exits 0, so a
# later workflow step can decide whether to fail the job on the outcome.
#
# Usage: ./scripts/run_no_stickiness_check.sh
# Env:
#   RUN_TIMEOUT_S   seconds for the hard wrapper around am instrument (default 180)

set -uo pipefail

TARGET_APK="Branch-SDK-TestBed/build/outputs/apk/debug/Branch-SDK-TestBed-debug.apk"
TEST_APK="Branch-SDK-GPTDriver/build/outputs/apk/debug/Branch-SDK-GPTDriver-debug.apk"
TARGET_PKG="io.branch.branchandroidtestbed"
TEST_PKG="io.branch.gptdriver"
RUNNER="androidx.test.runner.AndroidJUnitRunner"
TEST_CLASS="io.branch.gptdriver.tests.NoStickinessReturn"
RUN_TIMEOUT_S="${RUN_TIMEOUT_S:-180}"
INSTRUMENT_LOG="no-stickiness-instrument.log"

emit() {
  local result="$1"
  local reason="$2"
  echo "NO_STICKINESS result=${result} reason=${reason}"
  if [ -n "${GITHUB_OUTPUT:-}" ]; then
    {
      echo "result=${result}"
      echo "reason=${reason}"
    } >> "$GITHUB_OUTPUT"
  else
    echo "result=${result}"
    echo "reason=${reason}"
  fi
}

adb wait-for-device
echo "Installing target APK: $TARGET_APK"
adb install -r -t "$TARGET_APK"
echo "Installing test APK: $TEST_APK"
adb install -r -t "$TEST_APK"

timeout "$RUN_TIMEOUT_S" adb shell am instrument -w -r \
  -e class "$TEST_CLASS" \
  "$TEST_PKG/$RUNNER" > "$INSTRUMENT_LOG" 2>&1
instrument_rc=$?

if [ "$instrument_rc" -eq 124 ]; then
  emit "fail" "am instrument did not complete within ${RUN_TIMEOUT_S}s"
  exit 0
fi

result=$(grep -oE '^INSTRUMENTATION_STATUS: result=.*' "$INSTRUMENT_LOG" | tail -1 | sed 's/^INSTRUMENTATION_STATUS: result=//')
reason=$(grep -oE '^INSTRUMENTATION_STATUS: reason=.*' "$INSTRUMENT_LOG" | tail -1 | sed 's/^INSTRUMENTATION_STATUS: reason=//')

if [ -z "$result" ]; then
  echo "No result= status line in the instrument output:" >&2
  cat "$INSTRUMENT_LOG" >&2
  emit "fail" "no result line from the instrumented run"
  exit 0
fi

emit "$result" "${reason:-none}"
