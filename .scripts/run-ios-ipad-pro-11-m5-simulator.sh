#!/bin/sh
set -eu

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"

"$SCRIPT_DIR/run-ios-target.sh" --simulator "iPad Pro 11-inch (M5)" "$@"
