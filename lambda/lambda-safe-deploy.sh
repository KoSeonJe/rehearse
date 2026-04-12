#!/usr/bin/env bash
#
# Lambda 안전 배포 스크립트
# 코드 검증 → 버전 발행 → smoke test → alias 전환 → 실패 시 자동 롤백
#
# Usage: ./scripts/lambda-safe-deploy.sh [analysis|convert|all]
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
LAMBDA_DIR="$PROJECT_DIR/lambda"
COMMON_DIR="$LAMBDA_DIR/common"
REGION="ap-northeast-2"
ACCOUNT_ID="776735194358"
ALIAS_NAME="live"

# 색상
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info()  { echo -e "${BLUE}[INFO]${NC} $1" >&2; }
log_ok()    { echo -e "${GREEN}[  ✓ ]${NC} $1" >&2; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $1" >&2; }
log_fail()  { echo -e "${RED}[FAIL]${NC} $1" >&2; }
log_step()  { echo -e "\n${BLUE}━━━ Step $1: $2 ━━━${NC}" >&2; }

# ─── Step 1: 코드 검증 ────────────────────────────────────────────

validate_python() {
  local dir="$1"
  local name="$2"
  local has_error=0

  log_step "1" "코드 검증 ($name)"

  # 1-1. 구문 검사
  while IFS= read -r -d '' pyfile; do
    if ! python3 -c "import ast; ast.parse(open('$pyfile').read())" 2>/dev/null; then
      log_fail "구문 오류: $pyfile"
      python3 -c "import py_compile; py_compile.compile('$pyfile', doraise=True)" 2>&1 | tail -3
      has_error=1
    fi
  done < <(find "$dir" -name '*.py' -print0)

  # 1-2. f-string 내 JSON 중괄호 오용
  while IFS= read -r -d '' pyfile; do
    if grep -Pn 'f""".*"[a-z_]+":\s*[0-9]' "$pyfile" >/dev/null 2>&1; then
      log_fail "f-string 내 JSON 리터럴 감지: $pyfile"
      grep -Pn 'f""".*"[a-z_]+":\s*[0-9]' "$pyfile"
      has_error=1
    fi
  done < <(find "$dir" -name '*.py' -print0)

  if [ "$has_error" -eq 1 ]; then
    log_fail "검증 실패 — 배포 중단"
    return 1
  fi
  log_ok "코드 검증 통과"
}

# ─── Step 2: 패키징 ───────────────────────────────────────────────

package_function() {
  local name="$1"
  local src_dir="$LAMBDA_DIR/$name"
  local tmp_dir
  tmp_dir=$(mktemp -d)

  log_step "2" "패키징 ($name)"

  cp -r "$src_dir/"* "$tmp_dir/"
  rm -f "$tmp_dir/source.zip" 2>/dev/null || true
  rm -rf "$tmp_dir/.omc" "$tmp_dir/__pycache__" 2>/dev/null || true

  # common 모듈 인라인
  cp "$COMMON_DIR/retry.py" "$tmp_dir/retry.py"

  # ZIP 생성
  (cd "$tmp_dir" && zip -qr "$src_dir/source.zip" . -x "*.pyc" "__pycache__/*" "requirements.txt")

  log_ok "패키징 완료: $src_dir/source.zip"
  echo "$tmp_dir"
}

# ─── Step 3: 현재 버전 백업 ───────────────────────────────────────

get_current_version() {
  local func_name="$1"

  log_step "3" "현재 live 버전 확인"

  local current_version
  current_version=$(aws lambda get-alias \
    --function-name "$func_name" \
    --name "$ALIAS_NAME" \
    --query 'FunctionVersion' \
    --output text 2>/dev/null || echo "NONE")

  if [ "$current_version" = "NONE" ]; then
    log_warn "live alias 없음 (최초 배포)"
  else
    log_info "현재 live 버전: v$current_version"
  fi

  echo "$current_version"
}

# ─── Step 4: 배포 + 버전 발행 ────────────────────────────────────

deploy_and_publish() {
  local name="$1"
  local func_name="$2"

  log_step "4" "배포 + 버전 발행"

  # $LATEST에 코드 업로드
  log_info "코드 업로드 중..."
  aws lambda update-function-code \
    --function-name "$func_name" \
    --zip-file "fileb://$LAMBDA_DIR/$name/source.zip" \
    --no-cli-pager > /dev/null

  # 업데이트 완료 대기
  log_info "업데이트 완료 대기 중..."
  aws lambda wait function-updated \
    --function-name "$func_name"

  # 새 버전 발행
  local new_version
  new_version=$(aws lambda publish-version \
    --function-name "$func_name" \
    --description "deploy $(date +%Y-%m-%dT%H:%M:%S)" \
    --query 'Version' \
    --output text)

  log_ok "새 버전 발행: v$new_version"
  echo "$new_version"
}

# ─── Step 5: Smoke Test ──────────────────────────────────────────

smoke_test() {
  local func_name="$1"
  local version="$2"
  local result_file="/tmp/_deploy_smoke_${version}.json"

  log_step "5" "Smoke Test (v$version)"

  # 새 버전을 qualifier로 직접 호출 (live alias 영향 없음)
  local invoke_output
  invoke_output=$(aws lambda invoke \
    --function-name "$func_name" \
    --qualifier "$version" \
    --payload '{"detail":{"bucket":{"name":"__test__"},"object":{"key":"__test__"}}}' \
    --cli-binary-format raw-in-base64-out \
    "$result_file" 2>&1)

  # FunctionError 체크
  if echo "$invoke_output" | grep -q '"FunctionError"'; then
    log_fail "Smoke test 실패 — INIT/런타임 오류"
    echo ""
    echo "--- Lambda 응답 ---"
    cat "$result_file" 2>/dev/null || true
    echo ""
    rm -f "$result_file"
    return 1
  fi

  # StatusCode 체크
  if ! echo "$invoke_output" | grep -q '"StatusCode": 200'; then
    log_fail "Smoke test 실패 — StatusCode != 200"
    echo "$invoke_output"
    rm -f "$result_file"
    return 1
  fi

  # 응답 body 체크 (테스트 페이로드이므로 Skipped 기대)
  local response
  response=$(cat "$result_file" 2>/dev/null || echo "")
  if echo "$response" | grep -q "Skipped"; then
    log_ok "Smoke test 통과 (Skipped — 정상)"
  elif echo "$response" | grep -q '"statusCode": 200'; then
    log_ok "Smoke test 통과 (statusCode: 200)"
  else
    log_warn "예상치 못한 응답 (배포는 계속):"
    cat "$result_file"
    echo ""
  fi

  rm -f "$result_file"
  return 0
}

# ─── Step 6: Alias 전환 ──────────────────────────────────────────

switch_alias() {
  local func_name="$1"
  local new_version="$2"
  local current_version="$3"

  log_step "6" "Alias 전환"

  if [ "$current_version" = "NONE" ]; then
    aws lambda create-alias \
      --function-name "$func_name" \
      --name "$ALIAS_NAME" \
      --function-version "$new_version" \
      --no-cli-pager > /dev/null
    log_ok "live alias 생성: v$new_version"
  else
    aws lambda update-alias \
      --function-name "$func_name" \
      --name "$ALIAS_NAME" \
      --function-version "$new_version" \
      --no-cli-pager > /dev/null
    log_ok "live alias 전환: v$current_version → v$new_version"
  fi
}

# ─── 배포 리포트 ─────────────────────────────────────────────────

print_report() {
  local name="$1"
  local func_name="$2"
  local current_version="$3"
  local new_version="$4"

  echo ""
  echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo -e "${GREEN}  ✅ Lambda 배포 완료${NC}"
  echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo ""
  echo "  함수:   $func_name"
  if [ "$current_version" = "NONE" ]; then
    echo "  버전:   (신규) → v$new_version"
  else
    echo "  버전:   v$current_version → v$new_version"
  fi
  echo "  alias:  $ALIAS_NAME → v$new_version"
  echo ""
  echo "  롤백 명령 (문제 발생 시):"
  if [ "$current_version" != "NONE" ]; then
    echo "    aws lambda update-alias \\"
    echo "      --function-name \"$func_name\" \\"
    echo "      --name \"$ALIAS_NAME\" \\"
    echo "      --function-version \"$current_version\""
  else
    echo "    (최초 배포 — 이전 버전 없음)"
  fi
  echo ""
}

print_failure_report() {
  local name="$1"
  local func_name="$2"
  local current_version="$3"
  local new_version="$4"

  echo ""
  echo -e "${RED}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo -e "${RED}  ⚠️  배포 실패 — 롤백 완료${NC}"
  echo -e "${RED}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo ""
  echo "  실패 버전:  v$new_version (alias 미전환)"
  if [ "$current_version" != "NONE" ]; then
    echo "  현재 서빙:  v$current_version (변경 없음)"
  else
    echo "  현재 서빙:  \$LATEST (alias 미생성)"
  fi
  echo ""
  echo "  조치: 코드 수정 후 다시 실행"
  echo "    ./scripts/lambda-safe-deploy.sh $name"
  echo ""
}

# ─── 메인: 단일 함수 배포 ────────────────────────────────────────

deploy_function() {
  local name="$1"
  local func_name="rehearse-${name}-dev"

  echo ""
  echo -e "${BLUE}╔══════════════════════════════════════════╗${NC}"
  echo -e "${BLUE}║  Deploying: $func_name${NC}"
  echo -e "${BLUE}╚══════════════════════════════════════════╝${NC}"

  # Step 1: 코드 검증
  validate_python "$LAMBDA_DIR/$name" "$name"

  # Step 2: 패키징
  local tmp_dir
  tmp_dir=$(package_function "$name")

  # Step 3: 현재 버전 백업
  local current_version
  current_version=$(get_current_version "$func_name")

  # Step 4: 배포 + 버전 발행
  local new_version
  new_version=$(deploy_and_publish "$name" "$func_name")

  # Step 5: Smoke test
  if smoke_test "$func_name" "$new_version"; then
    # Step 6: Alias 전환
    switch_alias "$func_name" "$new_version" "$current_version"
    print_report "$name" "$func_name" "$current_version" "$new_version"
  else
    print_failure_report "$name" "$func_name" "$current_version" "$new_version"
    rm -rf "$tmp_dir"
    return 1
  fi

  # 정리
  rm -rf "$tmp_dir"
}

# ─── 엔트리포인트 ────────────────────────────────────────────────

main() {
  local target="${1:-}"

  if [ -z "$target" ]; then
    echo "Usage: $0 [analysis|convert|all]"
    echo ""
    echo "  analysis  — rehearse-analysis-dev 배포"
    echo "  convert   — rehearse-convert-dev 배포"
    echo "  all       — 둘 다 순차 배포 (analysis → convert)"
    exit 1
  fi

  case "$target" in
    analysis)
      deploy_function analysis
      ;;
    convert)
      deploy_function convert
      ;;
    all)
      deploy_function analysis
      deploy_function convert
      ;;
    *)
      log_fail "알 수 없는 대상: $target"
      echo "Usage: $0 [analysis|convert|all]"
      exit 1
      ;;
  esac
}

main "$@"
