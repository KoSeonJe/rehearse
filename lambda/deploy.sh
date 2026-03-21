#!/usr/bin/env bash
set -euo pipefail

LAMBDA_DIR="$(cd "$(dirname "$0")" && pwd)"
COMMON_DIR="$LAMBDA_DIR/common"

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

  # ZIP 생성
  (cd "$tmp_dir" && zip -r "$src_dir/source.zip" . -x "*.pyc" "__pycache__/*" "requirements.txt")

  # 배포
  aws lambda update-function-code \
    --function-name "$func_name" \
    --zip-file "fileb://$src_dir/source.zip" \
    --no-cli-pager

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
