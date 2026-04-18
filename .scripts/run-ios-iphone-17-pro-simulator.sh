#!/bin/sh
set -eu

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"

"$SCRIPT_DIR/run-ios-target.sh" --simulator "iPhone 17 Pro" "$@"
