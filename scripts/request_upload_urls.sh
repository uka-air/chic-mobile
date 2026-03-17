#!/usr/bin/env bash
set -euo pipefail

PRESIGN_URL="https://chic-conversation-analyzer.onrender.com/api/v1/presigns"
RAW_AUDIO_URL="https://chic-conversation-analyzer.onrender.com/api/v1/raw_audios"

presign_response="$(curl -sS -X POST "$PRESIGN_URL" -H 'Content-Type: application/json' -d '{}')"

echo "Presign response: $presign_response"

key="$(python3 -c 'import json,sys; print(json.loads(sys.argv[1]).get("key",""))' "$presign_response")"
if [[ -z "$key" ]]; then
  echo "Error: could not read key from presign response" >&2
  exit 1
fi

upload_response="$(curl -sS -X POST "$RAW_AUDIO_URL" -H 'Content-Type: application/json' -d "{\"key\":\"$key\"}")"

echo "Raw audio response: $upload_response"
