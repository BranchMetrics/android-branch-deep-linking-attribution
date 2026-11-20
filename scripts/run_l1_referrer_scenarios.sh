#!/usr/bin/env bash
#
# Runs every L1 install-referrer scenario (or the ones named as arguments) one after the other, keeps going
# after a failure, and exits 1 if any failed. Captures land in the current directory as wire-<scenario>.txt.
# Takes the same environment as run_l1_referrer.sh, including L1_REFERRER_SOURCE=play for a real device.

set -uo pipefail

RUNNER="$(cd "$(dirname "$0")" && pwd)/run_l1_referrer.sh"
if [ $# -gt 0 ]; then
  scenarios=("$@")
else
  scenarios=()
  while IFS= read -r name; do scenarios+=("$name"); done < <("$RUNNER" --list)
fi

failed=()
for scenario in "${scenarios[@]}"; do
  SCENARIO="$scenario" "$RUNNER" || failed+=("$scenario")
done

if [ ${#failed[@]} -gt 0 ]; then
  echo "Scenarios that failed to run: ${failed[*]}" >&2
  exit 1
fi
