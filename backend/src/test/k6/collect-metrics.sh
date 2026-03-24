#!/bin/bash
# Actuator 메트릭 1초 간격 폴링 — k6 테스트와 병렬 실행
# Usage: ./collect-metrics.sh [output.csv] [base_url]

OUTPUT=${1:-metrics.csv}
BASE_URL=${2:-http://localhost:8080}

echo "timestamp,active_connections,pending_connections,live_threads" > "$OUTPUT"
echo "Collecting metrics to $OUTPUT (Ctrl+C to stop)..."

while true; do
  TS=$(date +%s)
  ACTIVE=$(curl -s "$BASE_URL/actuator/metrics/hikaricp.connections.active" 2>/dev/null | python3 -c "import sys,json; print(json.load(sys.stdin)['measurements'][0]['value'])" 2>/dev/null || echo "0")
  PENDING=$(curl -s "$BASE_URL/actuator/metrics/hikaricp.connections.pending" 2>/dev/null | python3 -c "import sys,json; print(json.load(sys.stdin)['measurements'][0]['value'])" 2>/dev/null || echo "0")
  THREADS=$(curl -s "$BASE_URL/actuator/metrics/jvm.threads.live" 2>/dev/null | python3 -c "import sys,json; print(json.load(sys.stdin)['measurements'][0]['value'])" 2>/dev/null || echo "0")
  echo "$TS,$ACTIVE,$PENDING,$THREADS" >> "$OUTPUT"
  sleep 1
done
