---
name: lambda-deploy
description: AWS Lambda 안전 배포 스킬. 코드 검증 → 버전 발행 → smoke test → alias 전환 → 실패 시 자동 롤백. "/lambda-deploy", "람다 배포", "lambda 배포" 시 트리거.
---

# Lambda 안전 배포 스킬

Lambda 코드 변경 후 **장애 없이** 안전하게 배포한다.
핵심: 버전 발행 + alias 기반 전환 + smoke test + 자동 롤백

## 대상 Lambda

| 이름 | 함수명 | 역할 |
|------|--------|------|
| analysis | `rehearse-analysis-dev` | 영상 분석 (FFmpeg + Whisper + Vision + LLM) |
| convert | `rehearse-convert-dev` | WebM → MP4 변환 (MediaConvert) |

## 트리거

- `/lambda-deploy [analysis|convert|all]`
- "람다 배포", "lambda 배포", "Lambda 배포해줘"

인자 없으면 사용자에게 대상을 물어본다.

## 실행 절차

아래 모든 Step을 **순서대로** 실행한다. 각 함수를 `{name}`, 함수명을 `rehearse-{name}-dev`로 치환.

### Step 1: 변경 사항 확인

```bash
git diff --name-only HEAD~3..HEAD -- lambda/{name}/
```

변경 파일이 없으면 사용자에게 "변경 사항이 없는데 강제 배포할까요?" 확인.

### Step 2: 코드 검증 (Pre-flight)

대상 디렉토리 모든 `.py` 파일:

**2-1. Python 구문 검사**
```bash
find lambda/{name} lambda/common -name '*.py' -exec python3 -c "import ast; ast.parse(open('{}').read())" \;
```
하나라도 실패 → **배포 중단**.

**2-2. f-string 내 JSON 중괄호 오용**
```bash
grep -Prn 'f""".*"[a-z_]+":\s*[0-9]' lambda/{name}/
```
감지 → **배포 중단**.

### Step 3: 패키징

```bash
LAMBDA_DIR="$(pwd)/lambda"
TMP_DIR=$(mktemp -d)

cp -r "$LAMBDA_DIR/{name}/"* "$TMP_DIR/"
rm -f "$TMP_DIR/source.zip" 2>/dev/null || true
rm -rf "$TMP_DIR/__pycache__" 2>/dev/null || true

# common 모듈 인라인
cp "$LAMBDA_DIR/common/retry.py" "$TMP_DIR/retry.py"

# ZIP
(cd "$TMP_DIR" && zip -qr "$LAMBDA_DIR/{name}/source.zip" . -x "*.pyc" "__pycache__/*" "requirements.txt")
```

### Step 4: 현재 live 버전 백업

```bash
CURRENT_VERSION=$(aws lambda get-alias \
  --function-name "rehearse-{name}-dev" \
  --name "live" \
  --query 'FunctionVersion' \
  --output text 2>/dev/null || echo "NONE")
```

- `NONE` → 최초 배포 (alias 없음)
- 그 외 → 롤백 대상으로 기록

### Step 5: 코드 업로드 + 버전 발행

```bash
# $LATEST에 업로드
aws lambda update-function-code \
  --function-name "rehearse-{name}-dev" \
  --zip-file "fileb://lambda/{name}/source.zip" \
  --no-cli-pager

# 완료 대기
aws lambda wait function-updated \
  --function-name "rehearse-{name}-dev"

# 불변 버전 발행
NEW_VERSION=$(aws lambda publish-version \
  --function-name "rehearse-{name}-dev" \
  --description "deploy $(date +%Y-%m-%dT%H:%M:%S)" \
  --query 'Version' \
  --output text)
```

### Step 6: Smoke Test (alias 전환 전!)

**새 버전을 qualifier로 직접 호출** — live alias에 영향 없음:

```bash
aws lambda invoke \
  --function-name "rehearse-{name}-dev" \
  --qualifier "$NEW_VERSION" \
  --payload '{"detail":{"bucket":{"name":"__test__"},"object":{"key":"__test__"}}}' \
  --cli-binary-format raw-in-base64-out \
  /tmp/_smoke_result.json
```

**검증 기준:**

| 항목 | 통과 조건 |
|------|----------|
| FunctionError | 응답에 없어야 함 |
| StatusCode | `200` |
| body | `"Skipped"` (테스트 페이로드 → 핸들러가 `videos/` 불일치로 스킵) |

- **통과** → Step 7 진행
- **실패** → Step 7 건너뛰고 **롤백 리포트** 출력 (아래 참조)

### Step 7: Alias 전환 (smoke 통과 시에만)

```bash
# alias 없으면 생성, 있으면 업데이트
if [ "$CURRENT_VERSION" = "NONE" ]; then
  aws lambda create-alias \
    --function-name "rehearse-{name}-dev" \
    --name "live" \
    --function-version "$NEW_VERSION" \
    --no-cli-pager
else
  aws lambda update-alias \
    --function-name "rehearse-{name}-dev" \
    --name "live" \
    --function-version "$NEW_VERSION" \
    --no-cli-pager
fi
```

### Step 8: 정리 + 리포트

임시 파일 정리 후 결과 출력.

**성공 시:**
```
✅ Lambda 배포 완료
  함수:  rehearse-{name}-dev
  버전:  v{CURRENT} → v{NEW}
  alias: live → v{NEW}

  검증: 구문 ✓ | f-string ✓ | smoke test ✓

  롤백 명령 (문제 발생 시):
    aws lambda update-alias --function-name "rehearse-{name}-dev" --name "live" --function-version "{CURRENT}"
```

**실패 시 (smoke test 실패):**
```
⚠️ 배포 실패 — 자동 롤백 완료
  실패 버전: v{NEW} (alias 미전환)
  현재 서빙: v{CURRENT} (변경 없음)
  원인: {에러 내용}
  조치: 코드 수정 후 다시 /lambda-deploy
```

## all 배포

`all`이면 analysis → convert **순차 실행**. 하나 실패 시 나머지 중단.

## 최초 배포 시 안내

Step 4에서 `CURRENT_VERSION=NONE`이면 (최초 alias 생성), 배포 완료 후 다음 안내:

```
⚠️ 최초 배포 — EventBridge 타겟 확인 필요
  rehearse-video-uploaded-dev 규칙의 타겟이 $LATEST를 가리키고 있을 수 있습니다.
  live alias ARN으로 변경이 필요합니다:
    arn:aws:lambda:ap-northeast-2:776735194358:function:rehearse-{name}-dev:live
  변경하시겠습니까?
```

사용자가 Y이면 `aws events list-targets-by-rule` → `aws events put-targets`로 업데이트.

## 안전 장치 요약

| 단계 | 장치 | 효과 |
|------|------|------|
| Step 2 | 구문 + f-string 검증 | 런타임 오류 사전 차단 |
| Step 4 | 현재 버전 백업 | 롤백 대상 확보 |
| Step 5 | 버전 발행 | 불변 스냅샷 |
| Step 6 | alias 전환 **전** smoke test | 장애 사전 차단 |
| Step 7 | alias 기반 전환 | 즉시 롤백 가능 |
| 실패 시 | alias 미전환 | 기존 버전 계속 서빙 = 자동 롤백 |
