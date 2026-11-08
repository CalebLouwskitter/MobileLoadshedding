#!/usr/bin/env bash
# Lifecycle smoke test for the development APK on one connected Android phone.
#
# NOT YET RUN: the session that wrote this had no Unity editor, Android SDK or device.
# Run it from the repository root with exactly one device attached and USB debugging on:
#   GameUnity/Tools/device-smoke.sh [path/to/IdleLoadshedding-dev.apk]
#
# It installs the APK, turns on airplane mode, times the first boot, lets you play the first cut
# and buy an upgrade, then force-stops, background-kills and relaunches the app, comparing the
# "[ILS] commit" and "[ILS] boot" summary lines the game writes to logcat.
set -euo pipefail

APK="${1:-GameUnity/Builds/Android/IdleLoadshedding-dev.apk}"
PKG="com.prog7314.idleloadshedding.dev"

command -v adb >/dev/null || { echo "adb not found (install Android platform-tools)"; exit 1; }
[ -f "$APK" ] || { echo "APK not found: $APK"; exit 1; }
[ "$(adb devices | grep -c -w device)" -eq 1 ] || { echo "Attach exactly one device"; adb devices; exit 1; }

restore_network() { adb shell cmd connectivity airplane-mode disable >/dev/null 2>&1 || true; }
trap restore_network EXIT

launch() { adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1; }

wait_for_log() { # $1 pattern, $2 timeout seconds; prints the matching line
  local deadline=$(( $(date +%s) + $2 ))
  while [ "$(date +%s)" -lt "$deadline" ]; do
    local line
    line="$(adb logcat -d -s Unity | grep -F "$1" | tail -n 1 || true)"
    if [ -n "$line" ]; then echo "$line"; return 0; fi
    sleep 0.5
  done
  return 1
}

field() { # $1 line, $2 key -> value
  echo "$1" | tr ' ' '\n' | grep "^$2=" | head -n 1 | cut -d= -f2-
}

echo "== Device: $(adb shell getprop ro.product.model | tr -d '\r') (Android $(adb shell getprop ro.build.version.release | tr -d '\r'))"
echo "== Install $APK"
adb uninstall "$PKG" >/dev/null 2>&1 || true
adb install -r "$APK"

echo "== Airplane mode on"
adb shell cmd connectivity airplane-mode enable

echo "== Fresh install boot (target: decision within 60 s)"
adb logcat -c
start=$(date +%s)
launch
boot="$(wait_for_log "[ILS] boot" 60)" || { echo "FAIL: no boot line within 60 s"; exit 1; }
echo "PASS: booted in $(( $(date +%s) - start )) s -> $boot"
[ "$(field "$boot" phase)" = "prep" ] && echo "PASS: first boot waits at the outage decision" || echo "CHECK: phase is $(field "$boot" phase)"

echo
echo "== On the phone: pick a plan, Run the cut, open Upgrades, buy one, then Save as standing policy."
read -r -p "Press Enter when the upgrade and policy are saved... "
commit="$(adb logcat -d -s Unity | grep -F "[ILS] commit" | tail -n 1 || true)"
[ -n "$commit" ] || { echo "FAIL: no commit line found"; exit 1; }
echo "Last commit: $commit"

check_restore() { # $1 label
  local line
  line="$(wait_for_log "[ILS] boot" 60)" || { echo "FAIL ($1): no boot line"; return 1; }
  echo "Boot: $line"
  local ok=1
  for key in upgrades policy; do
    if [ "$(field "$line" "$key")" = "$(field "$commit" "$key")" ]; then echo "PASS ($1): $key restored"; else echo "FAIL ($1): $key differs"; ok=0; fi
  done
  local before after
  before="$(field "$commit" cash)"; after="$(field "$line" cash)"
  if [ "$after" -ge "$before" ]; then echo "PASS ($1): cash $before -> $after (only idle income added)"; else echo "FAIL ($1): cash went down"; ok=0; fi
  echo "INFO ($1): lights before $(field "$commit" lights), after $(field "$line" lights)"
  [ "$ok" -eq 1 ]
}

echo "== Force-stop and relaunch (airplane mode still on)"
adb logcat -c
adb shell am force-stop "$PKG"
sleep 2
launch
check_restore "force-stop"

echo "== Home, background kill, relaunch"
adb shell input keyevent KEYCODE_HOME
sleep 3
adb logcat -c
adb shell am kill "$PKG"
sleep 2
launch
check_restore "background kill"

echo "== Save slot files (may be hidden by scoped storage on some devices)"
adb shell ls -l "/sdcard/Android/data/$PKG/files/saves/slot-1" 2>/dev/null || adb shell run-as "$PKG" ls -l files/saves/slot-1 2>/dev/null || echo "(not readable from adb on this device)"

echo
echo "Manual follow-ups: lock the screen near a cut and resume; set the device clock back and forward;"
echo "play ten minutes with five background cycles while recording 'adb shell dumpsys meminfo $PKG' and frame times."
