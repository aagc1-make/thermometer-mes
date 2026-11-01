#!/usr/bin/env bash
set -eu
API="http://127.0.0.1:8081"
CAP=$(curl -s "$API/captchaImage")
UUID=$(printf '%s' "$CAP" | sed -n 's/.*"uuid":"\([^"]*\)".*/\1/p')
CODE=$(/e/dev/redis/redis-cli -h 127.0.0.1 -p 6379 get "captcha_codes:$UUID" 2>/dev/null | tr -d '"\r\n')
TOKEN=$(curl -s -X POST "$API/login" -H 'Content-Type: application/json' \
  -d "{\"username\":\"admin\",\"password\":\"admin123\",\"code\":\"$CODE\",\"uuid\":\"$UUID\"}" \
  | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
export TOKEN
export API
export WO="${1:-919029}"
cd "$(dirname "$0")/.."
node scripts/check-pipeline-state.mjs
