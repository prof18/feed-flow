#!/usr/bin/env bash
# Regenerate the desktop reader-mode ES5 bundles (readability-es5.js,
# turndown-es5.js). Pass --check to verify the committed bundles are up to date
# instead of rewriting them. See tools/reader-parser-bundle/README.md.
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
proj_dir="$script_dir/../tools/reader-parser-bundle"

cd "$proj_dir"

if [ ! -d node_modules ]; then
  npm install
fi

if [ "${1:-}" = "--check" ]; then
  npm run verify
else
  npm run build
fi
