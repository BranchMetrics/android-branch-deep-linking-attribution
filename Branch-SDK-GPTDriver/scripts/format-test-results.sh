#!/usr/bin/env bash
#
# format-test-results.sh
#
# Parses a `./gradlew :Branch-SDK-GPTDriver:connectedAndroidTest` log file and
# emits a single markdown table row summarizing the run, useful for pasting
# into review notes or PR descriptions after a local run.
#
# Usage:
#   ./format-test-results.sh <path-to-gradle-log>
#
# Optional environment overrides (auto-detected from the log when possible):
#   SDK_VERSION   — version string to print in the row (default: <unknown>)
#   DESTINATION   — destination string to print in the row (default: parsed
#                   from the log or <unknown>)
#   VARIANT       — variant name (e.g. debug) to print as a small note
#
# Output: one markdown table row, e.g.
#   | 2026-04-15 | 5.20.3 | Pixel 5 / API 34 | ✅ pass | 16 | 0 | 0 | 12m 07s |
#
# Exit code: 0 always (so the helper never breaks a CI pipeline). The row's
# Result column reflects the gradle exit when present in the log via
# "gradle exit=N" or "BUILD SUCCESSFUL" / "BUILD FAILED".

set -uo pipefail

LOG_FILE="${1:-}"

if [[ -z "$LOG_FILE" ]]; then
  echo "usage: $0 <path-to-gradle-log>" >&2
  exit 0
fi

if [[ ! -f "$LOG_FILE" ]]; then
  echo "error: log file not found: $LOG_FILE" >&2
  exit 0
fi

# ---- counts ----
# Gradle connectedAndroidTest emits lines shaped like:
#   io.branch.gptdriver.tests.LinkCreationHybridTest > createBranchLink_generatesValidUrl[Pixel_5_API_34(AVD) - 14] PASSED
#   io.branch.gptdriver.tests.LinkCreationHybridTest > createBranchLink_generatesValidUrl[Pixel_5_API_34(AVD) - 14] FAILED
#   io.branch.gptdriver.tests.LinkCreationHybridTest > createBranchLink_generatesValidUrl[Pixel_5_API_34(AVD) - 14] SKIPPED
# In Test Orchestrator mode the FAILED token is wrapped in ANSI colour codes:
#   ] <ESC>[31mFAILED <ESC>[0m
# We strip ANSI escape sequences first so both plain and coloured outputs match.
# The closing bracket anchor avoids counting "BUILD FAILED" or "> Task :... FAILED"
# as per-test failures.

# Strip ANSI escape sequences into a temp copy that all counts below share.
STRIPPED_LOG=$(mktemp -t mb-format-results.XXXXXX)
trap 'rm -f "$STRIPPED_LOG"' EXIT
# shellcheck disable=SC2016
# Drop CSI sequences (ESC [ ... final-byte). BSD sed on macOS interprets
# \x1B / $'\e' as the ESC literal; Linux sed via GNU coreutils works the same.
sed -E $'s/\x1B\\[[0-9;]*[A-Za-z]//g' "$LOG_FILE" > "$STRIPPED_LOG"

# `grep -c` always outputs a number on its own; the `|| true` swallows the
# non-zero exit code that grep returns when there are zero matches, so this
# never triggers `set -e` style behaviour from a calling script. The trailing
# match allows an optional space after the status token (gradle adds one
# before the ANSI reset).
PASSED=$(grep -cE "\] PASSED ?$" "$STRIPPED_LOG" 2>/dev/null || true)
FAILED=$(grep -cE "\] FAILED ?$" "$STRIPPED_LOG" 2>/dev/null || true)
SKIPPED=$(grep -cE "\] SKIPPED ?$" "$STRIPPED_LOG" 2>/dev/null || true)
PASSED=${PASSED:-0}
FAILED=${FAILED:-0}
SKIPPED=${SKIPPED:-0}

# ---- fallback / authoritative counts from gradle summary ----
# Test Orchestrator prints per-failure lines but NOT per-pass lines, so the
# counts above can undercount passes. The gradle progress line is the
# authoritative source when present:
#   Pixel_8_34(AVD) - 14 Tests 38/38 completed. (0 skipped) (6 failed)
# If we can find it, it always wins. Also fall back to the older summary:
#   16 tests completed, 0 failed, 0 skipped
PROGRESS_LINE=$(grep -E "Tests [0-9]+/[0-9]+ completed\. \([0-9]+ skipped\) \([0-9]+ failed\)" "$STRIPPED_LOG" 2>/dev/null | tail -1 || true)
if [[ -n "$PROGRESS_LINE" ]]; then
  TOTAL=$(echo "$PROGRESS_LINE" | sed -E 's/.*Tests [0-9]+\/([0-9]+) completed.*/\1/')
  SKIPPED=$(echo "$PROGRESS_LINE" | sed -E 's/.*\(([0-9]+) skipped\).*/\1/')
  FAILED=$(echo "$PROGRESS_LINE" | sed -E 's/.*\(([0-9]+) failed\).*/\1/')
  PASSED=$((TOTAL - FAILED - SKIPPED))
elif [[ "$PASSED" -eq 0 ]] && [[ "$FAILED" -eq 0 ]]; then
  SUMMARY_LINE=$(grep -E "[0-9]+ tests? completed" "$STRIPPED_LOG" 2>/dev/null | tail -1 || true)
  if [[ -n "$SUMMARY_LINE" ]]; then
    TOTAL=$(echo "$SUMMARY_LINE" | sed -E 's/^([0-9]+) tests? completed.*/\1/')
    FAILED=$(echo "$SUMMARY_LINE" | sed -nE 's/.*, ([0-9]+) failed.*/\1/p')
    SKIPPED=$(echo "$SUMMARY_LINE" | sed -nE 's/.*, ([0-9]+) skipped.*/\1/p')
    FAILED=${FAILED:-0}
    SKIPPED=${SKIPPED:-0}
    PASSED=$((TOTAL - FAILED - SKIPPED))
  fi
fi

# ---- runtime ----
# Gradle prints either:
#   BUILD SUCCESSFUL in 12m 7s
#   BUILD FAILED in 12m 7s
BUILD_LINE=$(grep -E "^BUILD (SUCCESSFUL|FAILED) in " "$STRIPPED_LOG" 2>/dev/null | tail -1 || true)
if [[ -n "$BUILD_LINE" ]]; then
  RUNTIME_STR=$(echo "$BUILD_LINE" | sed -E 's/^BUILD (SUCCESSFUL|FAILED) in (.+)$/\2/' | xargs)
else
  RUNTIME_STR="<unknown>"
fi

# ---- result emoji ----
EXIT_LINE=$(grep -E "^gradle exit=" "$STRIPPED_LOG" 2>/dev/null | tail -1)
if [[ -n "$EXIT_LINE" ]]; then
  EXIT_CODE=$(echo "$EXIT_LINE" | sed -E 's/^gradle exit=([0-9]+).*/\1/')
elif echo "$BUILD_LINE" | grep -q "BUILD SUCCESSFUL"; then
  EXIT_CODE="0"
elif echo "$BUILD_LINE" | grep -q "BUILD FAILED"; then
  EXIT_CODE="1"
else
  EXIT_CODE=""
fi

if [[ "$FAILED" -eq 0 ]] && [[ "$EXIT_CODE" == "0" ]]; then
  RESULT="✅ pass"
elif [[ "$FAILED" -gt 0 || "$EXIT_CODE" == "1" ]]; then
  RESULT="❌ fail"
else
  RESULT="⚠️ unknown"
fi

# ---- destination (parse from log if not provided) ----
if [[ -z "${DESTINATION:-}" ]]; then
  # Preferred format: per-test bracket descriptor
  #   [Pixel_5_API_34(AVD) - 14]
  # Fallback format: gradle progress / banner lines
  #   Starting 38 tests on Pixel_8_34(AVD) - 14
  #   Pixel_8_34(AVD) - 14 Tests 0/38 completed. ...
  # The trailing number is the Android OS version. On an all-pass run the
  # Test Orchestrator suppresses per-test `[...]` lines so only the
  # progress form remains.
  DEST_RAW=""
  BRACKETED=$(grep -oE "\[[A-Za-z0-9_]+\([A-Z]+\) - [0-9]+\]" "$STRIPPED_LOG" 2>/dev/null | head -1 || true)
  if [[ -n "$BRACKETED" ]]; then
    DEST_RAW=$(echo "$BRACKETED" | sed -E 's/^\[([A-Za-z0-9_]+)\([A-Z]+\) - ([0-9]+)\]$/\1|\2/')
  else
    BARE=$(grep -oE "[A-Za-z0-9_]+\([A-Z]+\) - [0-9]+" "$STRIPPED_LOG" 2>/dev/null | head -1 || true)
    if [[ -n "$BARE" ]]; then
      DEST_RAW=$(echo "$BARE" | sed -E 's/^([A-Za-z0-9_]+)\([A-Z]+\) - ([0-9]+)$/\1|\2/')
    fi
  fi

  if [[ -n "$DEST_RAW" ]]; then
    DEVICE_NAME=$(echo "$DEST_RAW" | cut -d'|' -f1 | tr '_' ' ')
    OS_VERSION=$(echo "$DEST_RAW" | cut -d'|' -f2)
    DESTINATION="${DEVICE_NAME} / Android ${OS_VERSION}"
  else
    DESTINATION="<unknown>"
  fi
fi

# ---- variant (parse from log if not provided) ----
if [[ -z "${VARIANT:-}" ]]; then
  # Look for a task name like :Branch-SDK-GPTDriver:connectedDebugAndroidTest
  VARIANT_LINE=$(grep -oE ":Branch-SDK-GPTDriver:connected[A-Z][a-zA-Z]*AndroidTest" "$STRIPPED_LOG" 2>/dev/null | head -1 || true)
  if [[ -n "$VARIANT_LINE" ]]; then
    VARIANT=$(echo "$VARIANT_LINE" | sed -E 's/.*:connected([A-Z][a-zA-Z]*)AndroidTest/\1/' | tr '[:upper:]' '[:lower:]')
  else
    VARIANT=""
  fi
fi

# ---- date (today, ISO) ----
RUN_DATE=$(date -u +%Y-%m-%d)

# ---- SDK version ----
SDK_VERSION="${SDK_VERSION:-<unknown>}"

# ---- emit row ----
printf "| %s | %s | %s | %s | %s | %s | %s | %s |\n" \
  "$RUN_DATE" \
  "$SDK_VERSION" \
  "$DESTINATION" \
  "$RESULT" \
  "$PASSED" \
  "$FAILED" \
  "$SKIPPED" \
  "$RUNTIME_STR"

# ---- helpful stderr summary so the user knows which table to paste into ----
{
  echo ""
  echo "Run date:    $RUN_DATE"
  echo "Variant:     ${VARIANT:-<not parsed>}"
  echo "Destination: $DESTINATION"
  echo "Pass / Fail / Skip: $PASSED / $FAILED / $SKIPPED"
  echo "Runtime:     $RUNTIME_STR"
  echo "Result:      $RESULT"
} >&2
