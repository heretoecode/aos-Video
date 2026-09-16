#!/usr/bin/env bash
set -euo pipefail
apk=$1
phase=$2
package=org.courville.nova.markpreview
activity=com.archos.mediacenter.video.leanback.MainActivityLeanback
mkdir -p ../startup-diagnostics
build_tools=$(find "$ANDROID_HOME/build-tools" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -1)
"$build_tools/apksigner" verify --print-certs "$apk" | tee "../startup-diagnostics/$phase-certificate.txt"
grep -qi "$(cat ../preview-certificate-sha256.txt)" "../startup-diagnostics/$phase-certificate.txt"
adb install -r -g "$apk"
set_preferences() {
  adb shell am force-stop "$package"
  adb shell run-as "$package" mkdir -p shared_prefs
  printf '%s\n' "<?xml version=\"1.0\" encoding=\"utf-8\"?><map><boolean name=\"try_new_ui\" value=\"$1\"/><int name=\"user_defined_density\" value=\"320\"/><boolean name=\"user_defined_density_confirmed\" value=\"true\"/><string name=\"uimode_leanback\">tv</string><string name=\"uimode\">2</string></map>" | adb shell "run-as $package sh -c 'cat > shared_prefs/${package}_preferences.xml'"
}
check_start() {
  label=$1
  adb shell am force-stop "$package"
  adb logcat -c
  adb shell am start -W -n "$package/$activity"
  sleep 15
  adb logcat -d > "../startup-diagnostics/$label-logcat.txt"
  adb shell dumpsys activity activities > "../startup-diagnostics/$label-activities.txt"
  adb exec-out screencap -p > "../startup-diagnostics/$label.png"
  python3 -c 'import struct,sys; w,h=struct.unpack(">II",open(sys.argv[1],"rb").read(24)[16:24]); print("Captured panel:",w,h); assert w>=1280 and w>h, "Expected a landscape TV panel"' "../startup-diagnostics/$label.png"
  if grep -q 'FATAL EXCEPTION' "../startup-diagnostics/$label-logcat.txt"; then
    cat "../startup-diagnostics/$label-logcat.txt"
    exit 1
  fi
  adb shell pidof "$package" | grep -q '[0-9]'
  grep -q "$activity" "../startup-diagnostics/$label-activities.txt"
}
if [[ "$phase" == debug ]]; then
  set_preferences false
  check_start classic-clean
  set_preferences true
fi
if [[ "$phase" == preview ]]; then
  set_preferences true
fi
check_start "$phase"
if [[ "$phase" == preview ]]; then
  # Reuse this APK to check reinstall and saved-Preview startup without a second build.
  adb shell am force-stop "$package"
  adb install -r -g "$apk"
  adb shell run-as "$package" cat "shared_prefs/${package}_preferences.xml" | grep -q 'name="try_new_ui" value="true"'
fi
check_start "$phase-restart"
