#!/usr/bin/env bash
#
# capture-test-logs.sh
#
# Runs the Branch-SDK-GPTDriver hybrid suite while streaming `adb logcat` to
# a timestamped file. After the Gradle task finishes, prints the paths you
# most likely want to inspect: the captured log, the Gradle HTML report, the
# JUnit XMLs, and a grep hint for the MobileBoost session URLs that the
# gptdriver-lib SDK prints during each AI step.
#
# Usage:
#   ./capture-test-logs.sh                             # run the full suite
#   ./capture-test-logs.sh -Pandroid.testInstrumentationRunnerArguments.class=... # pass-through
#
# Requires: adb on PATH, a connected device/emulator, and a configured
# MOBILEBOOST_API_KEY (see README "Secret management").
#
# Exits with the Gradle task's exit code so this wrapper is safe to use in CI.

set -uo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(cd "$SCRIPT_DIR/../.." && pwd)
MODULE="Branch-SDK-GPTDriver"
TIMESTAMP=$(date -u +%Y%m%d-%H%M%S)
OUT_DIR="$REPO_ROOT/$MODULE/build/test-logs"
LOG_FILE="$OUT_DIR/logcat-${TIMESTAMP}.log"
GRADLE_LOG="$OUT_DIR/gradle-${TIMESTAMP}.log"

mkdir -p "$OUT_DIR"

if ! command -v adb >/dev/null 2>&1; then
  echo "error: adb not found on PATH" >&2
  exit 2
fi

if ! adb get-state >/dev/null 2>&1; then
  echo "error: no adb device/emulator connected (adb get-state failed)" >&2
  exit 2
fi

echo "==> clearing logcat buffer"
adb logcat -c

echo "==> starting logcat capture -> $LOG_FILE"
adb logcat -v threadtime > "$LOG_FILE" &
LOGCAT_PID=$!
trap '[[ -n "${LOGCAT_PID:-}" ]] && kill "$LOGCAT_PID" 2>/dev/null || true' EXIT

echo "==> running :$MODULE:connectedDebugAndroidTest"
(
  cd "$REPO_ROOT"
  ./gradlew ":$MODULE:connectedDebugAndroidTest" "$@" 2>&1 | tee "$GRADLE_LOG"
)
GRADLE_EXIT=${PIPESTATUS[0]}

# Stop logcat before printing the summary so the file is flushed.
kill "$LOGCAT_PID" 2>/dev/null || true
LOGCAT_PID=""
wait 2>/dev/null || true

HTML_REPORT="$REPO_ROOT/$MODULE/build/reports/androidTests/connected/debug/index.html"
JUNIT_XML_DIR="$REPO_ROOT/$MODULE/build/outputs/androidTest-results/connected"
SESSION_URLS=$(grep -oE "https://app\.mobileboost\.io/[A-Za-z0-9/_-]+" "$LOG_FILE" 2>/dev/null | sort -u || true)

{
  echo ""
  echo "=== Test run artifacts ==="
  echo "Gradle exit:        $GRADLE_EXIT"
  echo "Logcat:             $LOG_FILE"
  echo "Gradle log:         $GRADLE_LOG"
  echo "HTML report:        $HTML_REPORT"
  echo "JUnit XML dir:      $JUNIT_XML_DIR"
  echo ""
  if [[ -n "$SESSION_URLS" ]]; then
    echo "MobileBoost sessions (unique, from logcat):"
    echo "$SESSION_URLS" | sed 's/^/  /'
  else
    echo "MobileBoost sessions: none found in logcat. Extract manually with:"
    echo "  grep -oE \"https://app.mobileboost.io/[A-Za-z0-9/_-]+\" \"$LOG_FILE\" | sort -u"
  fi
  echo ""
  echo "Startup crash check (expect 0):"
  printf "  NoClassDefFoundError.*CollectionsKt : %s\n" \
    "$(grep -c "NoClassDefFoundError.*CollectionsKt" "$LOG_FILE" 2>/dev/null || echo 0)"
  printf "  androidx.startup.StartupException   : %s\n" \
    "$(grep -c "androidx.startup.StartupException" "$LOG_FILE" 2>/dev/null || echo 0)"
} >&2

exit "$GRADLE_EXIT"
