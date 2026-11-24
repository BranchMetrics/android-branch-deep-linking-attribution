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
#   ./scripts/run_l1_instrumented.sh

set -euo pipefail

TARGET_APK="Branch-SDK-TestBed/build/outputs/apk/debug/Branch-SDK-TestBed-debug.apk"
TEST_APK="Branch-SDK-GPTDriver/build/outputs/apk/debug/Branch-SDK-GPTDriver-debug.apk"
TARGET_PKG="io.branch.branchandroidtestbed"
TEST_PKG="io.branch.gptdriver"
RUNNER="androidx.test.runner.AndroidJUnitRunner"
# A caller that sets neither is unaffected: TEST_CLASS keeps its original
# default, OUTPUT_LOG is defaulted below. TEST_CLASS selects the instrumented
# class to drive; OUTPUT_LOG names where the pulled capture lands. One
# scenario per invocation needs both: the capture accumulates across launches.
#
# The L1 drivers share one package, so TEST_CLASS takes a bare class name. A
# name that already carries a package is passed through untouched.
TEST_CLASS_PACKAGE="${TEST_CLASS_PACKAGE:-$TEST_PKG.tests}"
TEST_CLASS="${TEST_CLASS:-LinkCreationDeterministicTest}"
case "$TEST_CLASS" in
  *.*) ;;
  *) TEST_CLASS="$TEST_CLASS_PACKAGE.$TEST_CLASS" ;;
esac
OUTPUT_LOG="${OUTPUT_LOG:-}"
# TEST_METHODS="first second" runs those methods of TEST_CLASS, each in its own
# instrumentation, in order, and force-stops the app between them, so each one
# starts in a new process.
TEST_METHODS="${TEST_METHODS:-}"

# Both default off. CLEAR_LOG truncates the capture file, which the app only
# ever appends to. WIPE_FIRST runs `pm clear`, since `adb install -r` keeps app
# data; it also removes the capture file.
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
# COLD_LINK_URL, when set, is delivered as the link and the generator is skipped.
COLD_LINK_URL="${COLD_LINK_URL:-}"
# L1_ATTRIBUTION_LEVEL=NONE makes the generator set NONE after the link exists; it is the
# only level supported. With TEST_METHODS it is passed to each method instead. WIPE_AFTER=1
# clears app data on exit, so no later scenario inherits it. Both act only in a cold run that
# generates its link, or with TEST_METHODS.
L1_ATTRIBUTION_LEVEL="${L1_ATTRIBUTION_LEVEL:-}"
WIPE_AFTER="${WIPE_AFTER:-0}"
if [ -n "$L1_ATTRIBUTION_LEVEL" ] && [ "$L1_ATTRIBUTION_LEVEL" != "NONE" ]; then
  echo "L1_ATTRIBUTION_LEVEL supports only NONE, got $L1_ATTRIBUTION_LEVEL" >&2
  exit 1
fi
if [ -n "$TEST_METHODS" ] && [ -n "$COLD_SCENARIO" ]; then
  echo "TEST_METHODS runs TEST_CLASS, so it cannot be combined with COLD_SCENARIO" >&2
  exit 1
fi
if [ -n "$L1_ATTRIBUTION_LEVEL" ] && [ -z "$TEST_METHODS" ] && \
   { [ -z "$COLD_SCENARIO" ] || [ -n "$COLD_LINK_URL" ]; }; then
  echo "L1_ATTRIBUTION_LEVEL needs COLD_SCENARIO without COLD_LINK_URL, so the generator runs, or TEST_METHODS" >&2
  exit 1
fi
if [ "$WIPE_AFTER" = "1" ] && [ -z "$COLD_SCENARIO" ] && [ -z "$TEST_METHODS" ]; then
  echo "WIPE_AFTER=1 needs COLD_SCENARIO or TEST_METHODS" >&2
  exit 1
fi
if [ -n "$L1_ATTRIBUTION_LEVEL" ] && [ "$COLD_WIPE" = "1" ]; then
  echo "L1_ATTRIBUTION_LEVEL needs the generation run's tokens; COLD_WIPE would erase them" >&2
  exit 1
fi
if [ -n "$L1_ATTRIBUTION_LEVEL" ] && [ -n "$COLD_SCENARIO" ] && [ "$WIPE_FIRST" != "1" ]; then
  echo "L1_ATTRIBUTION_LEVEL needs WIPE_FIRST=1 so the level starts from a clean install" >&2
  exit 1
fi

# A cold run names its capture after its scenario, so a caller states the
# name once. Anything else keeps this script's original default.
if [ -n "$COLD_SCENARIO" ]; then
  OUTPUT_LOG="${OUTPUT_LOG:-wire-$COLD_SCENARIO.txt}"
else
  OUTPUT_LOG="${OUTPUT_LOG:-branchlogs.txt}"
fi

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

app_pid() {
  adb shell pidof "$TARGET_PKG" | tr -d '\r' || true
}

# Stops the app and waits until its process is gone.
force_stop_app() {
  adb shell am force-stop "$TARGET_PKG"
  for _ in 1 2 3 4 5; do
    [ -z "$(app_pid)" ] && return 0
    sleep 1
  done
  echo "$TARGET_PKG is still running after force-stop." >&2
  exit 1
}

LEVEL_ARGS=()
if [ -n "$L1_ATTRIBUTION_LEVEL" ]; then
  LEVEL_ARGS=(-e L1_ATTRIBUTION_LEVEL "$L1_ATTRIBUTION_LEVEL")
fi
if [ "$WIPE_AFTER" = "1" ]; then
  trap 'adb shell pm clear "$TARGET_PKG" || true' EXIT
fi

if [ -n "$TEST_METHODS" ]; then
  first=1
  for method in $TEST_METHODS; do
    [ "$first" = "1" ] || force_stop_app
    first=0
    run_instrumented "$TEST_CLASS#$method" ${LEVEL_ARGS[@]+"${LEVEL_ARGS[@]}"}
  done
  pull_capture "$OUTPUT_LOG"
  exit 0
fi

if [ -z "$COLD_SCENARIO" ]; then
  run_instrumented "$TEST_CLASS"
  pull_capture "$OUTPUT_LOG"
  exit 0
fi

LINK_URL="$COLD_LINK_URL"
if [ -z "$LINK_URL" ]; then
  run_instrumented "$TEST_CLASS_PACKAGE.ScenarioLinkGenerator" \
    -e L1_SCENARIO "$COLD_SCENARIO" -e L1_RUN_ID "$L1_RUN_ID" ${LEVEL_ARGS[@]+"${LEVEL_ARGS[@]}"}
  LINK_URL=$(tr -d '\r' < "$INSTRUMENT_LOG" | sed -n 's/^INSTRUMENTATION_STATUS: l1_link_url=//p' | head -n 1)
fi
if [ -z "$LINK_URL" ]; then
  echo "ScenarioLinkGenerator reported no link." >&2
  exit 1
fi

# Stopped before the capture is read or cleared, so no write from the
# generation run can land after it.
force_stop_app

if [ -n "$LINK_LOG" ]; then
  pull_capture "$LINK_LOG"
fi

if [ "$COLD_WIPE" = "1" ]; then
  echo "Wiping app data so the link arrives on a first install"
  adb shell pm clear "$TARGET_PKG"
else
  adb shell "run-as $TARGET_PKG sh -c 'rm -f $CAPTURE'" || true
fi

# Explicit-package delivery bypasses App Link verification, so the API level does not change the receiver.
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
# The validator checks that the link the app got and the open's link_data are this run's.
printf '%s\n' "$L1_RUN_ID" > "$OUTPUT_LOG.run_id"
