#!/usr/bin/env bash
#
# One L1 install-referrer scenario: the app starts with attribution level NONE on a fresh install that came
# from a real Branch link click, then the scenario's steps run, then the user consents.
#
# Usage:
#   SCENARIO=<name> ./scripts/run_l1_referrer.sh     run one scenario
#   ./scripts/run_l1_referrer.sh --list               print the scenario names
#
# Where the Play install referrer comes from (L1_REFERRER_SOURCE):
#   test-play-store (default, CI): an emulator without Google Play, with Branch-SDK-TestPlayStore installed
#     (adb install --force-queryable) and the debug TestBed installed. The script clicks a test link and sets
#     the test Play Store to return Branch's referrer for that click.
#   play (real device, local only): Google Play returns the referrer. The script clicks a link of an app
#     published on Play, opens that app's Play page with the click's referrer, installs the debug TestBed
#     built under that app's package, and taps Open in Play. It needs:
#       L1_PLAY_PACKAGE     the published app's package; build the TestBed with -Pl1ApplicationId=<it>
#       L1_PLAY_LINK_HOST   that app's Branch link domain, e.g. https://example.app.link
#       L1_PLAY_BRANCH_KEY  that app's Branch key; written only to the app's private files on the device
#       L1_APK              optional, the TestBed APK to install (default: the debug build output)
#
# Other inputs: L1_RUN_ID (default: the GitHub run, else a timestamp), OUTPUT_LOG (default
# wire-<scenario>.txt), WAIT_S (default 45). With more than one device attached, set ANDROID_SERIAL.
#
# Writes OUTPUT_LOG, plus OUTPUT_LOG.click_id and OUTPUT_LOG.run_id for
# validate_l1_logs.py --click-id and --run-id.

set -euo pipefail

SCENARIOS=(
  none_referrer_install
  none_referrer_install_reduced
  none_referrer_browser_return
  none_referrer_background_death
  none_referrer_foreground_death
  none_referrer_foreground_death_uri_call
  none_referrer_second_link
  none_referrer_app_passes_click_id
  none_organic_install
)
if [ "${1:-}" = "--list" ]; then
  printf '%s\n' "${SCENARIOS[@]}"
  exit 0
fi

SCENARIO="${SCENARIO:?SCENARIO is required (see --list)}"
case " ${SCENARIOS[*]} " in
  *" $SCENARIO "*) ;;
  *) echo "Unknown scenario: $SCENARIO" >&2; exit 2 ;;
esac

SOURCE="${L1_REFERRER_SOURCE:-test-play-store}"
ACTIVITY=io.branch.branchandroidtestbed.MainActivity
RECEIVER=io.branch.branchandroidtestbed.L1ConsentReceiver
APK="${L1_APK:-Branch-SDK-TestBed/build/outputs/apk/debug/Branch-SDK-TestBed-debug.apk}"
case "$SOURCE" in
  test-play-store)
    PKG=io.branch.branchandroidtestbed
    LINK_HOST=https://bnctestbed.test-app.link
    ;;
  play)
    PKG="${L1_PLAY_PACKAGE:?L1_PLAY_PACKAGE is required with L1_REFERRER_SOURCE=play}"
    LINK_HOST="${L1_PLAY_LINK_HOST:?L1_PLAY_LINK_HOST is required with L1_REFERRER_SOURCE=play}"
    : "${L1_PLAY_BRANCH_KEY:?L1_PLAY_BRANCH_KEY is required with L1_REFERRER_SOURCE=play}"
    ;;
  *) echo "Unknown L1_REFERRER_SOURCE: $SOURCE (test-play-store or play)" >&2; exit 2 ;;
esac

if [ -n "${GITHUB_RUN_ID:-}" ]; then
  DEFAULT_RUN_ID="${GITHUB_RUN_ID}-${GITHUB_RUN_ATTEMPT:-1}"
else
  DEFAULT_RUN_ID="local-$(date +%s)"
fi
L1_RUN_ID="${L1_RUN_ID:-$DEFAULT_RUN_ID}"
OUTPUT_LOG="${OUTPUT_LOG:-wire-$SCENARIO.txt}"
WAIT_S="${WAIT_S:-45}"
CONSENT=FULL
[ "$SCENARIO" = none_referrer_install_reduced ] && CONSENT=REDUCED
CALLBACK='^Deep link (params|error)'
OPEN_POSTED='^posting to https://[^ ]*/v3/events/open'
CLICK_ID=""

say() { echo "[$SCENARIO] $*"; }
capture() { adb shell "run-as $PKG cat files/branchlogs.txt" 2> /dev/null | tr -d '\r' || true; }
save_capture() {
  capture > "$OUTPUT_LOG"
  printf '%s\n' "$CLICK_ID" > "$OUTPUT_LOG.click_id"
  printf '%s\n' "$L1_RUN_ID" > "$OUTPUT_LOG.run_id"
}
fail() {
  say "FAILED: $*" >&2
  save_capture
  exit 1
}
count() { capture | grep -c -E "$1" || true; }
# Waits until more than $2 capture lines match $1.
wait_for() {
  local i
  for ((i = 0; i < WAIT_S; i++)); do
    [ "$(count "$1")" -gt "$2" ] && return 0
    sleep 1
  done
  fail "nothing matched '$1' within ${WAIT_S}s"
}
app_pid() { adb shell pidof "$PKG" 2> /dev/null | tr -d '\r' || true; }
wait_until_gone() {
  local i
  for ((i = 0; i < 10; i++)); do
    [ -z "$(app_pid)" ] && return 0
    sleep 1
  done
  fail "the app's process is still running"
}
# $1: the intent arguments for `am start`, as one string for the device shell.
launch_cold() {
  local out state
  out=$(adb shell "am start -W $1" | tr -d '\r')
  state=$(printf '%s\n' "$out" | sed -n 's/^LaunchState: //p')
  [ "$state" = COLD ] || fail "expected a cold launch, got '${state:-none}': $out"
}
deliver() { adb shell "am start -W -a android.intent.action.VIEW -d '$1' -n $PKG/$ACTIVITY" > /dev/null; }
home() {
  adb shell input keyevent KEYCODE_HOME
  sleep 3
}
link() { echo "$LINK_HOST/?l1_scenario=$SCENARIO&l1_run_id=$L1_RUN_ID&l1_link=$1&%24android_deepview=false"; }
# Clicks $1 as Chrome on the device would, and prints Branch's Play referrer for the click.
click_referrer() {
  local ua redirect
  ua="Mozilla/5.0 (Linux; Android $(adb shell getprop ro.build.version.release | tr -d '\r'); $(adb shell getprop ro.product.model | tr -d '\r')) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
  redirect=$(curl -s -m 20 -A "$ua" -o /dev/null -w '%{redirect_url}' "$1")
  python3 -c 'import re, sys, urllib.parse
m = re.search(r"S\.market_referrer=([^;]+)", sys.argv[1])
print(urllib.parse.unquote(m.group(1)) if m else "")' "$redirect"
}
write_launch_config() {
  local tmp
  tmp=$(mktemp)
  echo "attribution_level=NONE" > "$tmp"
  if [ "$SCENARIO" = none_referrer_foreground_death_uri_call ]; then echo "deep_link_call=uri" >> "$tmp"; fi
  if [ "$SOURCE" = play ]; then echo "branch_key=$L1_PLAY_BRANCH_KEY" >> "$tmp"; fi
  adb push "$tmp" /data/local/tmp/l1_launch.properties > /dev/null 2>&1
  rm -f "$tmp"
  adb shell "run-as $PKG sh -c 'mkdir -p files && cp /data/local/tmp/l1_launch.properties files/'"
  adb shell rm -f /data/local/tmp/l1_launch.properties
}
# Taps the first view whose text is $1; prints nothing and returns 1 when there is none.
tap_text() {
  local bounds
  bounds=$(adb exec-out uiautomator dump /dev/tty 2> /dev/null | python3 -c 'import re, sys
m = re.search(r"text=\"" + re.escape(sys.argv[1]) + r"\"[^>]*?bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"", sys.stdin.read())
print("" if not m else "%d %d" % ((int(m.group(1)) + int(m.group(3))) // 2, (int(m.group(2)) + int(m.group(4))) // 2))' "$1")
  [ -n "$bounds" ] || return 1
  adb shell input tap $bounds
}

# 1. The referrer for a real click.
if [ "$SCENARIO" = none_organic_install ]; then
  REFERRER="utm_source=google-play&utm_medium=organic"
else
  REFERRER=$(click_referrer "$(link install)")
  CLICK_ID=$(printf '%s\n' "$REFERRER" | sed -n 's/.*link_click_id=\([0-9][0-9]*\).*/\1/p')
  [ -n "$CLICK_ID" ] || { say "FAILED: the click returned no link_click_id in its Play referrer" >&2; exit 1; }
fi
say "source=$SOURCE run=$L1_RUN_ID referrer: $REFERRER"

# 2. A fresh install whose Play referrer is that one, starting at NONE.
if [ "$SOURCE" = test-play-store ]; then
  adb shell dumpsys package com.android.vending | tr -d '\r' | grep -q 'versionName=test-play-store' \
    || { say "FAILED: com.android.vending is not Branch-SDK-TestPlayStore" >&2; exit 1; }
  adb shell pm path "$PKG" > /dev/null || { say "FAILED: $PKG is not installed" >&2; exit 1; }
  adb shell "am broadcast --include-stopped-packages -n com.android.vending/.ConfigureReferrer --ez reset true --es referrer '$REFERRER' --el delay_ms 0" > /dev/null
  adb shell pm clear "$PKG" > /dev/null
else
  [ -f "$APK" ] || { say "FAILED: no APK at $APK" >&2; exit 1; }
  if adb shell dumpsys package com.android.vending | tr -d '\r' | grep -q 'versionName=test-play-store'; then
    say "FAILED: this device has Branch-SDK-TestPlayStore, not Google Play" >&2
    exit 1
  fi
  adb uninstall "$PKG" > /dev/null 2>&1 || true
  adb shell am force-stop com.android.vending
  PLAY_URL="https://play.google.com/store/apps/details?id=$PKG&referrer=$(python3 -c 'import sys, urllib.parse; print(urllib.parse.quote(sys.argv[1], safe=""))' "$REFERRER")"
  adb shell input keyevent KEYCODE_WAKEUP
  adb shell "am start -a android.intent.action.VIEW -d '$PLAY_URL' -p com.android.vending" > /dev/null
  sleep 8
  adb install -r -t "$APK" > /dev/null
  [ "$(adb shell pm list packages "$PKG" | tr -d '\r')" = "package:$PKG" ] || { say "FAILED: $APK did not install as $PKG" >&2; exit 1; }
  # Android can restore an app's backed-up data on install.
  adb shell pm clear "$PKG" > /dev/null
fi
write_launch_config

# 3. The first launch: from the launcher (in play mode, Open in Play), or, in
# none_referrer_app_passes_click_id, a URI that carries the referrer's click id.
before=$(count "$CALLBACK")
if [ "$SCENARIO" = none_referrer_app_passes_click_id ]; then
  launch_cold "-a android.intent.action.VIEW -d 'branchtest://open?link_click_id=$CLICK_ID' -n $PKG/$ACTIVITY"
elif [ "$SOURCE" = play ]; then
  opened=0
  for _ in 1 2 3 4 5; do
    if tap_text Open; then opened=1; break; fi
    sleep 2
  done
  [ "$opened" = 1 ] || launch_cold "-a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n $PKG/$ACTIVITY"
else
  launch_cold "-a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n $PKG/$ACTIVITY"
fi
wait_for "$CALLBACK" "$before"

# 4. The scenario's steps before consent. Each step that makes the app call requestDeepLinkData waits for
# its callback.
relaunch() {
  local before
  before=$(count "$CALLBACK")
  launch_cold "-a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n $PKG/$ACTIVITY"
  wait_for "$CALLBACK" "$before"
}
case "$SCENARIO" in
  none_referrer_browser_return)
    home
    before=$(count "$CALLBACK")
    deliver "branchtest://oauth?code=l1"
    wait_for "$CALLBACK" "$before"
    ;;
  none_referrer_background_death)
    home
    adb shell am kill "$PKG"
    wait_until_gone
    relaunch
    ;;
  none_referrer_foreground_death | none_referrer_foreground_death_uri_call)
    adb shell am force-stop "$PKG"
    wait_until_gone
    relaunch
    ;;
  none_referrer_second_link)
    home
    before=$(count "$CALLBACK")
    deliver "$(link second)"
    wait_for "$CALLBACK" "$before"
    ;;
esac

# 5. Consent, then the open it sends.
before=$(count "$OPEN_POSTED")
adb shell am broadcast -n "$PKG/$RECEIVER" --es level "$CONSENT" > /dev/null
wait_for "$OPEN_POSTED" "$before"
sleep 4

# 6. Proof the SDK read this run's referrer; without it the scenario tested nothing.
expected="utm_medium=organic"
[ -n "$CLICK_ID" ] && expected="link_click_id=$CLICK_ID"
capture | grep -E 'fetchInstallReferrer (resumeWith )?got result' | grep -q -F "$expected" \
  || fail "the SDK never read the referrer this run set ($expected)"

save_capture
say "captured $(wc -l < "$OUTPUT_LOG" | tr -d ' ') lines to $OUTPUT_LOG"
