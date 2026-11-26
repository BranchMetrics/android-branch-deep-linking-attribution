#!/usr/bin/env bash
#
# One L1 late_duplicate_intent scenario: one tap on a link is credited once. A link opens the app, then the
# user and Android rebuild the screen that link opened, and only the user's new taps may send a link.
# Driven from the host, because Android has to end the app's process, which would end an instrumentation
# running in it.
#
# Usage:
#   SCENARIO=<name> ./scripts/run_l1_late_duplicate_intent.sh    run one scenario
#   ./scripts/run_l1_late_duplicate_intent.sh --list              print the scenario names
#
# late_duplicate_intent: MainActivity calls requestDeepLinkData(this).
# late_duplicate_intent_uri_call: MainActivity calls requestDeepLinkData(getIntent().getData()).
#
# The steps, and what MainActivity does in each:
#   1. Link A opens the stopped app.                                   onCreate, a new tap
#   2. The user turns the phone, then turns it back.                   onCreate twice
#   3. Home, then back from recents while the app still runs.          no call
#   4. Home, Android ends the app's process, back from recents.        onCreate
#   5. Link B arrives while the app is open.                           onNewIntent, a new tap
#   6. The user turns the phone, then turns it back.                   onCreate twice
#   7. Home, Android ends the app's process, back from recents.        onCreate
#   8. Home, Android ends the app's process, the user taps link C.     onCreate, then onNewIntent, a new tap
#   9. The user taps link A again.                                     onNewIntent, a new tap
#  10. The user opens the app from its icon, which starts a new screen; onCreate, no link
#      Home, Android ends the app's process, the user taps link D.     onCreate, then onNewIntent, a new tap
# This script fails when a step did not happen as described. validate_l1_logs.py judges what was sent.
#
# Needs the debug TestBed installed, with the screen unlocked. With more than one device attached, set
# ANDROID_SERIAL. The rotation setting is put back on exit. Other inputs: L1_RUN_ID (default: the GitHub
# run, else a timestamp), OUTPUT_LOG (default wire-<scenario>.txt), WAIT_S (default 30).
#
# Writes OUTPUT_LOG, plus OUTPUT_LOG.run_id for validate_l1_logs.py --run-id.

set -euo pipefail

SCENARIOS=(
  late_duplicate_intent
  late_duplicate_intent_uri_call
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

PKG=io.branch.branchandroidtestbed
ACTIVITY=io.branch.branchandroidtestbed.MainActivity
LINK_HOST=https://bnctestbed.test-app.link
if [ -n "${GITHUB_RUN_ID:-}" ]; then
  DEFAULT_RUN_ID="${GITHUB_RUN_ID}-${GITHUB_RUN_ATTEMPT:-1}"
else
  DEFAULT_RUN_ID="local-$(date +%s)"
fi
L1_RUN_ID="${L1_RUN_ID:-$DEFAULT_RUN_ID}"
OUTPUT_LOG="${OUTPUT_LOG:-wire-$SCENARIO.txt}"
WAIT_S="${WAIT_S:-30}"
QUIET_S=6
FINAL_QUIET_S=12
CALL='^requestDeepLinkData called for URI: '
OPEN_POSTED='^posting to https://[^ ]*/v3/events/open'
POSTED='^posting to '

say() { echo "[$SCENARIO] $*"; }
capture() { adb shell "run-as $PKG cat files/branchlogs.txt" 2> /dev/null | tr -d '\r' || true; }
save_capture() {
  capture > "$OUTPUT_LOG"
  printf '%s\n' "$L1_RUN_ID" > "$OUTPUT_LOG.run_id"
}
fail() {
  say "FAILED: $*" >&2
  save_capture
  exit 1
}
count() { capture | grep -c -E "$1" || true; }
last_call_uri() { capture | sed -n "s/$CALL//p" | tail -n 1; }
app_pid() { adb shell pidof "$PKG" 2> /dev/null | tr -d '\r' || true; }
link() { echo "$LINK_HOST/?l1_scenario=$SCENARIO&l1_run_id=$L1_RUN_ID&l1_link=$1&%24android_deepview=false"; }

# Waits until more than $2 capture lines match $1; fails after WAIT_S unless $3 is "soft".
wait_for() {
  local i
  for ((i = 0; i < WAIT_S; i++)); do
    [ "$(count "$1")" -gt "$2" ] && return 0
    sleep 1
  done
  [ "${3:-}" = soft ] || fail "nothing matched '$1' within ${WAIT_S}s"
}
# Returns once no request has been posted for $1 seconds.
settle() {
  local quiet=$1 last now still=0 i
  last=$(count "$POSTED")
  for ((i = 0; i < WAIT_S * 2; i++)); do
    sleep 1
    now=$(count "$POSTED")
    if [ "$now" = "$last" ]; then
      still=$((still + 1))
      [ "$still" -ge "$quiet" ] && return 0
    else
      last=$now
      still=0
    fi
  done
  fail "requests were still being posted after $((WAIT_S * 2))s"
}
wake() {
  adb shell input keyevent KEYCODE_WAKEUP
  # Only a lock screen without a pattern, PIN or password goes away; with one, a step fails on the app
  # not being in front.
  adb shell wm dismiss-keyguard > /dev/null 2>&1 || true
}
# The first line of `adb shell $1` output that matches $2. Reads all of it: with pipefail, a reader that stops
# early fails the pipe.
first_match() { adb shell "$1" | tr -d '\r' | { grep -o -E "$2" || true; } | sed -n 1p; }
# Android 10 and later print topResumedActivity first; older versions print only mResumedActivity.
main_resumed() {
  case "$(first_match 'dumpsys activity activities' '(topResumedActivity|mResumedActivity)[=:] ?ActivityRecord\{[^}]*\}')" in
    *"$PKG/.MainActivity"*) return 0 ;;
  esac
  return 1
}
await_main_resumed() {
  local i
  for ((i = 0; i < 10; i++)); do
    main_resumed && return 0
    sleep 1
  done
  fail "$1"
}
home() {
  adb shell input keyevent KEYCODE_HOME
  # Longer than ProcessLifecycleOwner's 700 ms stop delay, so the SDK has seen the app go to the background.
  sleep 3
}
wait_until_gone() {
  local i
  for ((i = 0; i < 10; i++)); do
    [ -z "$(app_pid)" ] && return 0
    sleep 1
  done
  fail "the app's process is still running"
}
# Ends the backgrounded app's process the way Android does to free memory. Android only ends it once it counts
# as in the background, which can lag Home on a slow device, so ask again.
end_app_process() {
  local i
  for ((i = 0; i < 10; i++)); do
    adb shell am kill "$PKG"
    sleep 1
    [ -z "$(app_pid)" ] && return 0
  done
  fail "the app's process is still running"
}
# Opens recents and taps the task card nearest the middle of the screen, which is the app just left.
back_from_recents() {
  local xy="" i
  wake
  adb shell input keyevent KEYCODE_APP_SWITCH
  # Recents takes longer to appear on some devices, and the card only shows once it has.
  for ((i = 0; i < 10; i++)); do
    sleep 1
    xy=$(recent_task_card)
    [ -n "$xy" ] && break
  done
  [ -n "$xy" ] || fail "recents showed no task card"
  sleep 1
  adb shell input tap $xy
  await_main_resumed "MainActivity is not in front after tapping its card in recents"
}
# The middle of the task card nearest the middle of the recents screen, or nothing.
recent_task_card() {
  adb exec-out uiautomator dump /dev/tty 2> /dev/null | python3 -c 'import re, sys
dump = sys.stdin.read()
box = r"bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\""
width = max([int(m.group(3)) for m in re.finditer(box, dump)] or [0])
cards = [tuple(map(int, m.groups())) for m in re.finditer(r"id/snapshot\"[^>]*?" + box, dump)]
if cards:
    l, t, r, b = min(cards, key=lambda c: abs((c[0] + c[2]) / 2 - width / 2))
    print("%d %d" % ((l + r) // 2, (t + b) // 2))' || true
}

# Rotation: wm user-rotation where the device has it, else the settings it writes.
ORIGINAL_AUTO=$(adb shell settings get system accelerometer_rotation | tr -d '\r')
ORIGINAL_ROTATION=$(adb shell settings get system user_rotation | tr -d '\r')
# True when `wm user-rotation` ran; older Android versions don't have it.
wm_rotation() {
  case "$(adb shell wm user-rotation "$@" 2>&1 | tr -d '\r')" in
    *[Uu]nknown* | *[Ee]rror* | *[Uu]sage*) return 1 ;;
  esac
}
restore_rotation() {
  if [ "$ORIGINAL_AUTO" = 1 ]; then
    wm_rotation free || adb shell settings put system accelerometer_rotation 1 || true
  else
    wm_rotation lock "${ORIGINAL_ROTATION:-0}" || {
      adb shell settings put system user_rotation "${ORIGINAL_ROTATION:-0}"
      adb shell settings put system accelerometer_rotation 0
    } || true
  fi
}
trap restore_rotation EXIT
set_rotation() {
  if ! wm_rotation lock "$1"; then
    adb shell settings put system accelerometer_rotation 0
    adb shell settings put system user_rotation "$1"
  fi
}
current_rotation() { first_match 'dumpsys window displays' 'mCurrentRotation=[^ ]*' | sed 's/.*=//'; }

# One requestDeepLinkData call: $1 is what it should be called with, a link or "a link" for any link but
# none. Waits for its open, without failing, since a missing open is for the validator to report.
await_call() {
  local before_calls=$2 before_opens=$3 got
  wait_for "$CALL" "$before_calls"
  got=$(last_call_uri)
  if [ "$1" = "a link" ]; then
    [ -n "$got" ] && [ "$got" != null ] || fail "MainActivity called requestDeepLinkData without a link"
  else
    [ "$got" = "$1" ] || fail "MainActivity called requestDeepLinkData with '$got', expected '$1'"
  fi
  wait_for "$OPEN_POSTED" "$before_opens" soft
  settle "$QUIET_S"
  say "  call with $got"
}
step_turn() {
  local calls opens rotation
  calls=$(count "$CALL"); opens=$(count "$OPEN_POSTED")
  wake
  set_rotation "$1"
  await_call "$2" "$calls" "$opens"
  rotation=$(current_rotation)
  [ "$rotation" = "ROTATION_$(( $1 * 90 ))" ] || fail "the screen is at $rotation, not rotation $1"
  [ "$(count "$CALL")" = $((calls + 1)) ] || fail "turning the phone made more than one call"
}
step_deliver() {
  local calls opens
  calls=$(count "$CALL"); opens=$(count "$OPEN_POSTED")
  wake
  adb shell "am start -W -a android.intent.action.VIEW -d '$1' -n $PKG/$ACTIVITY" > /dev/null
  await_call "$1" "$calls" "$opens"
}
step_return_running() {
  local calls posts pid
  home
  pid=$(app_pid)
  [ -n "$pid" ] || fail "the app's process ended on Home"
  calls=$(count "$CALL"); posts=$(count "$POSTED")
  back_from_recents
  settle "$FINAL_QUIET_S"
  [ "$(app_pid)" = "$pid" ] || fail "the app's process changed, so the app was not still running"
  [ "$(count "$CALL")" = "$calls" ] || fail "MainActivity called requestDeepLinkData, so it was rebuilt, not resumed"
  say "  no call, $(( $(count "$POSTED") - posts )) request(s) posted"
}
step_return_after_death() {
  local calls opens
  home
  end_app_process
  calls=$(count "$CALL"); opens=$(count "$OPEN_POSTED")
  back_from_recents
  await_call "a link" "$calls" "$opens"
}
# Android rebuilds the screen, whose onCreate calls with the intent that opened it ($2: "a link", or "null"
# for the icon), then hands it link $1 through onNewIntent.
step_link_after_death() {
  local calls opens uris first
  home
  end_app_process
  calls=$(count "$CALL"); opens=$(count "$OPEN_POSTED")
  wake
  adb shell "am start -W -a android.intent.action.VIEW -d '$1' -n $PKG/$ACTIVITY" > /dev/null
  wait_for "$CALL" $((calls + 1))
  uris=$(capture | sed -n "s/$CALL//p" | tail -n 2)
  first=$(printf '%s\n' "$uris" | sed -n 1p)
  if [ "$2" = null ]; then
    [ "$first" = null ] || fail "the rebuilt screen's call was '$first', not the icon's null"
  else
    [ "$first" != "$1" ] && [ "$first" != null ] || fail "the rebuilt screen's call was '$first', not its own link"
  fi
  [ "$(printf '%s\n' "$uris" | sed -n 2p)" = "$1" ] || fail "the second call was '$(printf '%s\n' "$uris" | sed -n 2p)', not '$1'"
  wait_for "$OPEN_POSTED" $((opens + 1)) soft
  settle "$QUIET_S"
  say "  calls with $first, then $1"
}
# A launcher start that clears the task, so the screen's intent is the icon's, as after opening the app from its
# icon on a new day.
step_open_from_icon() {
  local calls opens
  calls=$(count "$CALL"); opens=$(count "$OPEN_POSTED")
  wake
  adb shell am start -W -f 0x10008000 -a android.intent.action.MAIN -c android.intent.category.LAUNCHER \
    -n "$PKG/$ACTIVITY" > /dev/null
  await_call null "$calls" "$opens"
}

say "run $L1_RUN_ID"
adb shell pm path "$PKG" > /dev/null || { say "FAILED: $PKG is not installed" >&2; exit 1; }
adb shell pm clear "$PKG" > /dev/null
if [ "$SCENARIO" = late_duplicate_intent_uri_call ]; then
  adb shell "run-as $PKG sh -c 'mkdir -p files && echo deep_link_call=uri > files/l1_launch.properties'"
fi
set_rotation 0

# The app is installed and has run once, so link A opens an installed app, as cold_https does.
wake
adb shell am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n "$PKG/$ACTIVITY" > /dev/null
wait_for "$OPEN_POSTED" 0
settle "$QUIET_S"
adb shell am force-stop "$PKG"
wait_until_gone
adb shell "run-as $PKG rm -f files/branchlogs.txt"

A=$(link A)
B=$(link B)
C=$(link C)
D=$(link D)

say "1. link A opens the stopped app"
wake
out=$(adb shell "am start -W -a android.intent.action.VIEW -d '$A' -n $PKG/$ACTIVITY" | tr -d '\r')
state=$(printf '%s\n' "$out" | sed -n 's/^LaunchState: //p')
[ "$state" = COLD ] || fail "expected a cold launch, got '${state:-none}': $out"
await_call "$A" 0 0

say "2. the user turns the phone, then turns it back"
step_turn 1 "$A"
step_turn 0 "$A"

say "3. Home, then back from recents while the app still runs"
step_return_running

say "4. Home, Android ends the app's process, back from recents"
step_return_after_death

say "5. link B arrives while the app is open"
step_deliver "$B"

say "6. the user turns the phone, then turns it back"
step_turn 1 "$B"
step_turn 0 "$B"

say "7. Home, Android ends the app's process, back from recents"
step_return_after_death

say "8. Home, Android ends the app's process, the user taps link C"
step_link_after_death "$C" "a link"

say "9. the user taps link A again"
step_deliver "$A"

say "10. the user opens the app from its icon; Home, Android ends the app's process, the user taps link D"
step_open_from_icon
step_link_after_death "$D" null
settle "$FINAL_QUIET_S"

save_capture
say "captured $(wc -l < "$OUTPUT_LOG" | tr -d ' ') lines to $OUTPUT_LOG"
