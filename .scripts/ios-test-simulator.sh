#!/usr/bin/env bash

# Prints the UDID of the iPhone simulator the iOS tests run on: the newest installed
# iPhone 18 Pro or iPhone 17 Pro, else any iPhone. Prints nothing when no iPhone is installed.
# Keep in sync with SwiftCloudSyncTest.findSimulatorId, which applies the same preference.

set -euo pipefail

xcrun simctl list devices available --json | python3 -I -c '
import json, sys

def version(runtime):
    parts = runtime.split(".SimRuntime.iOS-")[-1].split("-")[:2]
    return [int(part) if part.isdigit() else 0 for part in parts]

devices = json.load(sys.stdin)["devices"]
runtimes = sorted((r for r in devices if ".SimRuntime.iOS-" in r), key=version, reverse=True)
iphones = [d for r in runtimes for d in devices[r] if d["name"].startswith("iPhone")]
for name in ("iPhone 18 Pro", "iPhone 17 Pro"):
    match = next((d for d in iphones if d["name"] == name), None)
    if match:
        print(match["udid"])
        sys.exit()
if iphones:
    print(iphones[0]["udid"])
'
