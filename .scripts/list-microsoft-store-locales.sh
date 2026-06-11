#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${1:-$HOME/.feedflow/microsoft-store.env}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Microsoft Store env file not found: $ENV_FILE" >&2
  echo "Expected MS_STORE_TENANT_ID, MS_STORE_CLIENT_ID, MS_STORE_CLIENT_SECRET, and MS_STORE_APP_ID." >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

required_vars=(
  MS_STORE_TENANT_ID
  MS_STORE_CLIENT_ID
  MS_STORE_CLIENT_SECRET
  MS_STORE_APP_ID
)

for var_name in "${required_vars[@]}"; do
  if [[ -z "${!var_name:-}" ]]; then
    echo "Missing required value in $ENV_FILE: $var_name" >&2
    exit 1
  fi
done

token="$(
  curl -sS -X POST "https://login.microsoftonline.com/${MS_STORE_TENANT_ID}/oauth2/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "grant_type=client_credentials" \
    --data-urlencode "client_id=${MS_STORE_CLIENT_ID}" \
    --data-urlencode "client_secret=${MS_STORE_CLIENT_SECRET}" \
    --data-urlencode "resource=https://manage.devcenter.microsoft.com" \
    | python3 -c 'import sys,json; print(json.load(sys.stdin)["access_token"])'
)"

app_json="$(
  curl -sS \
    -H "Authorization: Bearer ${token}" \
    -H "Content-Type: application/json" \
    "https://manage.devcenter.microsoft.com/v1.0/my/applications/${MS_STORE_APP_ID}"
)"

submission_id="$(
  printf '%s' "$app_json" | python3 -c 'import sys,json; app=json.load(sys.stdin); sub=(app.get("pendingApplicationSubmission") or app.get("lastPublishedApplicationSubmission") or {}); print(sub.get("id", ""))'
)"

if [[ -z "$submission_id" ]]; then
  echo "No pending or last published Microsoft Store submission found for ${MS_STORE_APP_ID}." >&2
  exit 1
fi

submission_json="$(
  curl -sS \
    -H "Authorization: Bearer ${token}" \
    -H "Content-Type: application/json" \
    "https://manage.devcenter.microsoft.com/v1.0/my/applications/${MS_STORE_APP_ID}/submissions/${submission_id}"
)"

printf '%s' "$submission_json" | python3 -c '
import json
import sys

submission = json.load(sys.stdin)
locales = sorted((submission.get("listings") or {}).keys())
print("\n".join(locales))
'
