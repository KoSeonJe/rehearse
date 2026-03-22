#!/usr/bin/env bash
set -euo pipefail

LAMBDA_DIR="$(cd "$(dirname "$0")" && pwd)"
COMMON_DIR="$LAMBDA_DIR/common"

validate_python() {
  local dir="$1"
  local name="$2"
  local has_error=0

  echo "--- [$name] Python 검증 시작 ---"

  # 1. 구문 검사 (모든 .py 파일)
  while IFS= read -r -d '' pyfile; do
    if ! python3 -c "import ast; ast.parse(open('$pyfile').read())" 2>/dev/null; then
      echo "  ✗ 구문 오류: $pyfile"
      python3 -c "import py_compile; py_compile.compile('$pyfile', doraise=True)" 2>&1 | tail -3
      has_error=1
    fi
  done < <(find "$dir" -name '*.py' -print0)

  # 2. f-string 내 JSON 중괄호 오용 탐지
  while IFS= read -r -d '' pyfile; do
    if grep -Pn 'f""".*"[a-z_]+":\s*[0-9]' "$pyfile" >/dev/null 2>&1; then
      echo "  ✗ f-string 내 JSON 리터럴 감지 (중괄호 이스케이프 필요): $pyfile"
      grep -Pn 'f""".*"[a-z_]+":\s*[0-9]' "$pyfile"
      has_error=1
    fi
  done < <(find "$dir" -name '*.py' -print0)

  if [ "$has_error" -eq 1 ]; then
    echo "--- [$name] 검증 실패 — 배포 중단 ---"
    return 1
  fi
  echo "--- [$name] 검증 통과 ---"
}

deploy_function() {
  local name="$1"
  local func_name="rehearse-${name}-dev"
  local src_dir="$LAMBDA_DIR/$name"
  local tmp_dir
  tmp_dir=$(mktemp -d)

  echo "=== Deploying $func_name ==="

  # 소스 복사
  cp -r "$src_dir/"* "$tmp_dir/"
  rm -f "$tmp_dir/source.zip" 2>/dev/null || true
  rm -rf "$tmp_dir/.omc" 2>/dev/null || true

  # common 모듈 인라인 (최신 버전 덮어쓰기)
  cp "$COMMON_DIR/retry.py" "$tmp_dir/retry.py"

  # 배포 전 검증
  validate_python "$tmp_dir" "$name"

  # ZIP 생성
  (cd "$tmp_dir" && zip -r "$src_dir/source.zip" . -x "*.pyc" "__pycache__/*" "requirements.txt")

  # 배포
  aws lambda update-function-code \
    --function-name "$func_name" \
    --zip-file "fileb://$src_dir/source.zip" \
    --no-cli-pager

  # 배포 후 smoke test
  echo "--- [$name] smoke test ---"
  local wait_sec=3
  echo "  대기 ${wait_sec}초..."
  sleep "$wait_sec"
  local result
  result=$(aws lambda invoke \
    --function-name "$func_name" \
    --payload '{"detail":{"bucket":{"name":"__test__"},"object":{"key":"__test__"}}}' \
    --cli-binary-format raw-in-base64-out \
    /tmp/_deploy_smoke_test.json 2>&1)
  if echo "$result" | grep -q '"FunctionError"'; then
    echo "  ✗ smoke test 실패 — INIT 에러 가능성"
    cat /tmp/_deploy_smoke_test.json
    echo ""
    echo "  롤백이 필요할 수 있습니다."
  else
    echo "  ✓ smoke test 통과"
  fi

  # 정리
  rm -rf "$tmp_dir"
  echo "=== $func_name deployed ==="
}

case "${1:-all}" in
  analysis) deploy_function analysis ;;
  convert)  deploy_function convert ;;
  all)      deploy_function analysis; deploy_function convert ;;
  *)        echo "Usage: $0 [analysis|convert|all]"; exit 1 ;;
esac
