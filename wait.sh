#!/bin/bash

export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools:$PATH"

while true; do
  BOOTUP=$(adb shell getprop init.svc.bootanim | grep -oe '[a-z]\+')
  if [[ "$BOOTUP" = "stopped" ]]; then
    break
  fi

  echo "Got: '$BOOTUP', waiting for 'stopped'"
  sleep 5
done
