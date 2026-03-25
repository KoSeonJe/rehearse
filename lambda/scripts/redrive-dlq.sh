#!/bin/bash
# DLQ 메시지를 읽고 retry-analysis API로 재처리
# Usage: ./redrive-dlq.sh <queue-url> [api-url]
set -euo pipefail

QUEUE_URL="${1:?Usage: $0 <queue-url> [api-url]}"
API_URL="${2:-${API_SERVER_URL:-https://api-dev.rehearse.co.kr}}"
API_KEY="${INTERNAL_API_KEY:?INTERNAL_API_KEY 환경변수가 필요합니다}"
REGION="ap-northeast-2"

echo "DLQ Redrive 시작: $QUEUE_URL → $API_URL"

processed=0
skipped=0

while true; do
  msg=$(aws sqs receive-message --queue-url "$QUEUE_URL" --max-number-of-messages 1 --region "$REGION" 2>/dev/null)
  body=$(echo "$msg" | grep -o '"Body":"[^"]*"' | head -1 | cut -d'"' -f4 || echo "")
  [ -z "$body" ] && break

  receipt=$(echo "$msg" | grep -o '"ReceiptHandle":"[^"]*"' | cut -d'"' -f4)

  # S3 key 추출 (detail.object.key)
  key=$(echo "$body" | grep -o '"key":"[^"]*"' | cut -d'"' -f4 || echo "")

  if [ -z "$key" ] || [[ ! "$key" == videos/*/qs_*.webm ]]; then
    echo "  [SKIP] 유효하지 않은 메시지: $body"
    skipped=$((skipped + 1))
    aws sqs delete-message --queue-url "$QUEUE_URL" --receipt-handle "$receipt" --region "$REGION"
    continue
  fi

  interview_id=$(echo "$key" | cut -d'/' -f2)
  qs_id=$(echo "$key" | cut -d'/' -f3 | sed 's/qs_//' | sed 's/.webm//')

  echo "  [RETRY] interview=$interview_id, qs=$qs_id"
  http_code=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST "$API_URL/api/internal/interviews/$interview_id/question-sets/$qs_id/retry-analysis" \
    -H "X-Internal-Api-Key: $API_KEY" \
    -H "Content-Type: application/json")

  if [ "$http_code" = "200" ]; then
    echo "    → 성공 (HTTP $http_code)"
    processed=$((processed + 1))
  else
    echo "    → 실패 (HTTP $http_code) — 메시지 유지"
    skipped=$((skipped + 1))
    continue  # 메시지 삭제하지 않음 (visibility timeout 후 재시도)
  fi

  aws sqs delete-message --queue-url "$QUEUE_URL" --receipt-handle "$receipt" --region "$REGION"
done

echo ""
echo "Redrive 완료: 처리=$processed, 스킵=$skipped"
