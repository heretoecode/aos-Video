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

if [[ "$phase" == preview ]]; then
  # Real warm relaunch, distinct from force-stop/cold restart above.
  adb shell input keyevent 3
  adb shell am start -W -n "$package/$activity"
  sleep 2
  adb shell pidof "$package" | grep -q '[0-9]'
  # Open non-exported Settings through its real navigation control.
  adb shell uiautomator dump /sdcard/nova-home.xml
  adb pull /sdcard/nova-home.xml ../startup-diagnostics/preview-home.xml
  python3 - <<'PYUI'
import re,subprocess,xml.etree.ElementTree as ET
root=ET.parse('../startup-diagnostics/preview-home.xml')
node=next(n for n in root.iter('node') if n.get('text')=='Settings' or n.get('content-desc')=='Settings')
x1,y1,x2,y2=map(int,re.findall(r'\d+',node.get('bounds')))
subprocess.run(['adb','shell','input','tap',str((x1+x2)//2),str((y1+y2)//2)],check=True)
PYUI
  # Focusable-in-touch-mode TV controls take focus on the first tap; activate
  # that focused control with the remote's centre key.
  adb shell input keyevent 23
  sleep 3
  adb shell dumpsys activity activities > ../startup-diagnostics/preview-settings-activities.txt
  adb exec-out screencap -p > ../startup-diagnostics/preview-settings.png
  adb shell uiautomator dump /sdcard/nova-settings.xml
  adb pull /sdcard/nova-settings.xml ../startup-diagnostics/preview-settings.xml
  adb logcat -d > ../startup-diagnostics/preview-settings-logcat.txt
  grep -q "mResumedActivity.*VideoSettingsActivity" ../startup-diagnostics/preview-settings-activities.txt
  if grep -q 'FATAL EXCEPTION' ../startup-diagnostics/preview-settings-logcat.txt; then exit 1; fi
  adb shell pidof "$package" | grep -q '[0-9]'
fi

if [[ "$phase" == preview ]]; then
  python3 - <<'PYSET'
import re,subprocess,xml.etree.ElementTree as ET
root=ET.parse('../startup-diagnostics/preview-settings.xml')
node=next(n for n in root.iter('node') if n.get('text')=='Home & Discovery')
x1,y1,x2,y2=map(int,re.findall(r'\d+',node.get('bounds')))
subprocess.run(['adb','shell','input','tap',str((x1+x2)//2),str((y1+y2)//2)],check=True)
PYSET
  sleep 1
  adb exec-out screencap -p > ../startup-diagnostics/preview-settings-home-category.png
  adb shell uiautomator dump /sdcard/nova-settings-home.xml
  adb pull /sdcard/nova-settings-home.xml ../startup-diagnostics/preview-settings-home-category.xml
  adb logcat -d > ../startup-diagnostics/preview-settings-category-logcat.txt
  if grep -q 'FATAL EXCEPTION' ../startup-diagnostics/preview-settings-category-logcat.txt; then exit 1; fi
fi

if [[ "$phase" == preview ]]; then
  # Bounded checks of changed routes, not an exhaustive remote-navigation matrix.
  python3 - <<'PYMORE'
import re, subprocess, time, xml.etree.ElementTree as ET
from pathlib import Path
out=Path('../startup-diagnostics')
def adb(*args):
 return subprocess.run(['adb',*args],check=True,stdout=subprocess.PIPE).stdout
def capture(name):
 # Accessibility can briefly return a null root during a window transition.
 # Never reuse a stale dump, and retain crash diagnostics if retries fail.
 for attempt in range(3):
  adb('shell','rm','-f','/sdcard/nova-check.xml')
  adb('shell','uiautomator','dump','/sdcard/nova-check.xml')
  result=subprocess.run(['adb','shell','cat','/sdcard/nova-check.xml'],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
  if result.returncode==0 and result.stdout.strip():
   try: root=ET.fromstring(result.stdout)
   except ET.ParseError: pass
   else:
    (out/(name+'.xml')).write_bytes(result.stdout)
    (out/(name+'.png')).write_bytes(adb('exec-out','screencap','-p'))
    return root
  logs=adb('logcat','-d');(out/(name+'-logcat.txt')).write_bytes(logs)
  assert b'FATAL EXCEPTION' not in logs, 'Crash during UI capture'
  adb('shell','pidof','org.courville.nova.markpreview')
  time.sleep(1)
 raise AssertionError('Accessibility root unavailable after bounded retries: '+name)
def target(root,label,activate=False):
 n=next(n for n in root.iter('node') if n.get('text')==label or n.get('content-desc')==label)
 x1,y1,x2,y2=map(int,re.findall(r'\d+',n.get('bounds')))
 adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
 if activate: adb('shell','input','keyevent','23')
 time.sleep(.6)
root=capture('settings-check')
for category in ['Subtitles','Video & Audio','Streaming','About']:
 target(root,category)
 root=capture('settings-'+category.lower().replace(' & ','-'))
 if category=='Subtitles':
  assert any('OpenSubtitles' in n.get('text','') for n in root.iter('node')), 'Subtitles category lost credentials'
# Return to each library via the actual top navigation, then exercise the new local query.
target(root,'Movies',True);root=capture('navigation-movies')
assert sum(n.get('text')=='Movies' for n in root.iter('node'))>=2, 'Movies route/header desynchronised'
target(root,'TV shows',True);root=capture('navigation-tv')
assert any(n.get('text')=='TV Shows' for n in root.iter('node')), 'TV library failed to open'
target(root,'Search',True);root=capture('search-empty')
query=next(n for n in root.iter('node') if n.get('class')=='android.widget.EditText')
x1,y1,x2,y2=map(int,re.findall(r'\d+',query.get('bounds')))
adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));adb('shell','input','text','nova40-smoke-no-match');time.sleep(1)
adb('shell','input','keyevent','66');time.sleep(.4)
root=capture('search-query')
assert any(n.get('text')=='No matching library titles' for n in root.iter('node')), 'Local query failed'
logs=adb('logcat','-d');(out/'targeted-routes-logcat.txt').write_bytes(logs)
assert b'FATAL EXCEPTION' not in logs, 'Crash during changed-route smoke check'
PYMORE
fi
