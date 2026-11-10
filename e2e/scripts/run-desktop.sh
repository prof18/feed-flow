#!/usr/bin/env bash
# Runs the desktop E2E suite (macOS, local only). See e2e/desktop/README.md.
# Usage: e2e/scripts/run-desktop.sh [smoke|regression|desktoponly|all]
set -euo pipefail

cd "$(dirname "$0")/../.."
./gradlew --quiet --console=plain :desktopApp:desktopE2eTest -Pfeedflow.e2eSuite="${1:-all}"
