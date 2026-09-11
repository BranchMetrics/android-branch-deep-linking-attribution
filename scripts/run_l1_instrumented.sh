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
if [[ -z "$MOBILEBOOST_API_KEY" ]]; then
  echo "Error: MOBILEBOOST_API_KEY is required (provide as positional arg or env var)" >&2
  exit 1
fi

TARGET_APK="Branch-SDK-TestBed/build/outputs/apk/debug/Branch-SDK-TestBed-debug.apk"
TEST_APK="Branch-SDK-GPTDriver/build/outputs/apk/debug/Branch-SDK-GPTDriver-debug.apk"
TARGET_PKG="io.branch.branchandroidtestbed"
TEST_PKG="io.branch.gptdriver"
RUNNER="androidx.test.runner.AndroidJUnitRunner"
# Both default to what this script did before they existed, so a caller that
# sets neither is unaffected. TEST_CLASS selects the instrumented class to
# drive; OUTPUT_LOG names where the pulled capture lands. One scenario per
# invocation needs both, because the capture file accumulates across launches.
TEST_CLASS="${TEST_CLASS:-io.branch.gptdriver.tests.LinkCreationDeterministicTest}"
OUTPUT_LOG="${OUTPUT_LOG:-branchlogs.txt}"

# Both default off, so a caller that sets neither gets exactly the previous
# behaviour. CI starts from a fresh emulator, so a single-scenario job has
# nothing to inherit and needs neither.
#
# CLEAR_LOG truncates the capture file. It matters only when a job drives more
# than one scenario: CustomBranchApp.saveLogToFile opens the file in append
# mode and nothing truncates it, so the second scenario would be judged against
# the first one's traffic as well.
#
# WIPE_FIRST runs `pm clear`. `adb install -r` preserves app data, so on a
# device that already holds state this is the only way to ask for a clean one.
#
# It is inert on CI, and that is measured rather than assumed: run 33446080403
# drove the same class twice, the second with WIPE_FIRST=1, and the captures
# were identical -- same count, same token sequence, first request carrying
# none on both. The emulator is created fresh for every job, so the run before
# the wipe was already a first install and there was nothing to clear.
#
# So this exists for local reproduction on a persistent emulator, not to
# produce the first-install scenario in CI, which comes free. It removes the
# log as a side effect, which is why the two are separate switches: a scenario
# that must keep its install still needs a clean log.
CLEAR_LOG="${CLEAR_LOG:-0}"
WIPE_FIRST="${WIPE_FIRST:-0}"

# COLD_SCENARIO delivers that scenario's link cold: ScenarioLinkGenerator runs
# in its own invocation, then the link starts the stopped app from the host.
# COLD_WIPE=1 clears app data between the two, for a first install. LINK_LOG
# keeps the generator's capture, which is where /v1/url now is.
COLD_SCENARIO="${COLD_SCENARIO:-}"
COLD_WIPE="${COLD_WIPE:-0}"
COLD_SETTLE_S="${COLD_SETTLE_S:-12}"
LINK_LOG="${LINK_LOG:-}"
if [ -n "${GITHUB_RUN_ID:-}" ]; then
  DEFAULT_RUN_ID="${GITHUB_RUN_ID}-${GITHUB_RUN_ATTEMPT:-1}"
else
  DEFAULT_RUN_ID="local-$(date +%s)"
fi
L1_RUN_ID="${L1_RUN_ID:-$DEFAULT_RUN_ID}"

adb wait-for-device
adb shell input keyevent 82 || true

echo "Installing target APK: $TARGET_APK"
adb install -r -t "$TARGET_APK"
echo "Installing test APK: $TEST_APK"
adb install -r -t "$TEST_APK"

if [ "$WIPE_FIRST" = "1" ]; then
  echo "Wiping app data so the SDK sees a first install"
  adb shell pm clear "$TARGET_PKG"
elif [ "$CLEAR_LOG" = "1" ]; then
  echo "Truncating the capture file"
  adb shell "run-as $TARGET_PKG sh -c 'rm -f /data/user/0/$TARGET_PKG/files/branchlogs.txt'" || true
fi

# Run via am instrument directly (no orchestrator) so the target package
# remains installed after the run, allowing run-as to read its private files.
# -w = wait for completion and stream results to stdout
# -r = raw output (parseable) so we can grep the final status
INSTRUMENT_LOG=instrument.log
CAPTURE="/data/user/0/$TARGET_PKG/files/branchlogs.txt"

# Arguments: the test class, then any extra `-e key value` pairs.
run_instrumented() {
  local test_class="$1"
  shift
  adb shell am instrument -w -r \
    -e class "$test_class" \
    -e MOBILEBOOST_API_KEY "$MOBILEBOOST_API_KEY" \
    "$@" \
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
}

# Pull logs while the target package is still installed.
pull_capture() {
  adb shell "run-as $TARGET_PKG cat $CAPTURE" > "$1"
  wc -l "$1"
}

if [ -z "$COLD_SCENARIO" ]; then
  run_instrumented "$TEST_CLASS"
  pull_capture "$OUTPUT_LOG"
  exit 0
fi

run_instrumented io.branch.gptdriver.tests.ScenarioLinkGenerator \
  -e L1_SCENARIO "$COLD_SCENARIO" -e L1_RUN_ID "$L1_RUN_ID"
LINK_URL=$(tr -d '\r' < "$INSTRUMENT_LOG" | sed -n 's/^INSTRUMENTATION_STATUS: l1_link_url=//p' | head -n 1)
if [ -z "$LINK_URL" ]; then
  echo "ScenarioLinkGenerator reported no link." >&2
  exit 1
fi
if [ -n "$LINK_LOG" ]; then
  pull_capture "$LINK_LOG"
fi

if [ "$COLD_WIPE" = "1" ]; then
  echo "Wiping app data so the link arrives on a first install"
  adb shell pm clear "$TARGET_PKG"
else
  adb shell "run-as $TARGET_PKG sh -c 'rm -f $CAPTURE'" || true
fi
adb shell am force-stop "$TARGET_PKG"

app_pid() {
  adb shell pidof "$TARGET_PKG" | tr -d '\r' || true
}
for _ in 1 2 3 4 5; do
  [ -z "$(app_pid)" ] && break
  sleep 1
done
if [ -n "$(app_pid)" ]; then
  echo "$TARGET_PKG is still running after force-stop." >&2
  exit 1
fi

# Resolved against the package, not a named component, so the manifest still
# has to declare the link's host. One quoted string, so the device shell
# cannot split the URL.
echo "Delivering $LINK_URL cold for $COLD_SCENARIO"
adb shell "am start -W -a android.intent.action.VIEW -d '$LINK_URL' $TARGET_PKG" \
  | tr -d '\r' | tee am-start.log
LAUNCH_STATE=$(sed -n 's/^LaunchState: //p' am-start.log)
if [ -z "$LAUNCH_STATE" ]; then
  echo "am start printed no LaunchState, so the launch cannot be confirmed cold." >&2
  exit 1
elif [ "$LAUNCH_STATE" != "COLD" ]; then
  echo "Expected a cold launch, got $LAUNCH_STATE." >&2
  exit 1
fi

sleep "$COLD_SETTLE_S"
pull_capture "$OUTPUT_LOG"
