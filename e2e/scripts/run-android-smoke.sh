#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

E2E_ANDROID_SUITES=smoke "$SCRIPT_DIR/run-android.sh"
