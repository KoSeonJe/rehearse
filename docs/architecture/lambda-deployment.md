# Lambda 안전 배포 전략 (Alias 기반)

> 적용 일자: 2026-04-07
> 적용 함수: `rehearse-analysis-dev`, `rehearse-convert-dev`

## Why

### 이전 배포 방식의 문제

기존에는 EventBridge가 Lambda 함수 ARN을 **alias/version qualifier 없이** 직접 가리켰다.

```
EventBridge ──invoke──▶ rehearse-analysis-dev   (= 항상 $LATEST 호출)
                              ▲
                              │ update-function-code
                          [새 코드]
```

이 구조에서는:

1. **`update-function-code` 실행 = 즉시 프로덕션 반영**
   `$LATEST`만 존재하고, EventBridge가 그것을 직접 호출하므로 코드 업로드 순간 모든 트래픽이 새 코드로 라우팅된다.

2. **smoke test가 사후 확인밖에 못 됨**
   문제를 발견해도 이미 라이브 트래픽이 새 코드를 받고 있다.

3. **롤백이 느림**
   이전 버전이 어디에도 보존되지 않으므로, 롤백하려면:
   - git에서 이전 코드 체크아웃
   - 다시 패키징
   - `update-function-code`로 재업로드
   롤백이 완료되기까지 수 분 동안 장애가 지속된다.

4. **불변 버전 스냅샷 부재**
   "어제 배포한 그 코드"가 어디에도 없다. 감사/조사가 어렵다.

### 해결: Alias + Version 분리

Lambda의 **publish-version**과 **alias** 기능을 사용해 다음 구조로 전환했다.

```
                           ┌──▶ v1  ◀── live (alias)
EventBridge ──invoke──▶ rehearse-analysis-dev:live
                           └──▶ v2  (대기 — smoke test 중)
                                 ▲
                                 │ publish-version
                             $LATEST
                                 ▲
                                 │ update-function-code
                             [새 코드]
```

핵심 요점:

- **EventBridge는 함수 ARN 대신 `:live` alias ARN**을 호출한다.
- `$LATEST`에 코드를 올려도 **live 트래픽에는 아무 영향이 없다.**
- 새 버전(v2)을 발행한 뒤 `--qualifier`로 직접 호출해 smoke test → **live는 여전히 v1.**
- 통과 시에만 `update-alias live → v2` 한 번의 원자적 전환.
- 실패 시 alias가 그대로 v1을 가리키므로 **자동 롤백** (사용자 트래픽 영향 0).
- 이전 버전들은 불변 스냅샷으로 영구 보존된다.

## Before / After 비교

| 항목 | Before (직접 ARN) | After (alias 경유) |
|------|-------------------|--------------------|
| EventBridge 타겟 | `...:rehearse-analysis-dev` | `...:rehearse-analysis-dev:live` |
| 코드 업로드 시점 | **즉시 live 반영** | live 영향 없음 ($LATEST만 변경) |
| 버전 발행 | 없음 | `publish-version`으로 불변 스냅샷 생성 |
| smoke test 의미 | 사후 확인 (이미 live) | **사전 게이트** (live=이전 버전) |
| 트래픽 전환 | `update-function-code` 부수효과 | `update-alias` 명시적 전환 |
| 롤백 | 코드 재업로드 (수 분, 그동안 장애) | `update-alias` 한 줄 (즉시) |
| 이전 코드 보존 | 없음 (git 의존) | Lambda 버전으로 영구 보존 |

## 배포 흐름 (Step-by-Step)

### Pre-flight (Step 1~2)

1. 변경 파일 확인 (`git diff --name-only`)
2. 코드 검증
   - Python `ast.parse` 구문 검사
   - f-string 내 JSON 중괄호 오용 패턴 검출

### 배포 (Step 3~7)

3. **패키징**
   `lambda/{name}/` + `lambda/common/retry.py` → `source.zip`

4. **현재 live 버전 백업** (롤백 대상 확보)
   ```bash
   CURRENT=$(aws lambda get-alias \
     --function-name rehearse-analysis-dev --name live \
     --query FunctionVersion --output text)
   ```

5. **코드 업로드 + 버전 발행**
   ```bash
   aws lambda update-function-code \
     --function-name rehearse-analysis-dev \
     --zip-file fileb://lambda/analysis/source.zip
   aws lambda wait function-updated --function-name rehearse-analysis-dev

   NEW=$(aws lambda publish-version \
     --function-name rehearse-analysis-dev \
     --query Version --output text)
   ```
   이 시점에 `$LATEST`와 v{NEW}는 동일 코드. **live alias는 여전히 v{CURRENT}를 가리킨다.**

6. **Smoke test (alias 전환 전!)**
   `--qualifier`로 새 버전을 직접 호출. live 트래픽 영향 없음.
   ```bash
   aws lambda invoke \
     --function-name rehearse-analysis-dev \
     --qualifier "$NEW" \
     --payload '{"detail":{"bucket":{"name":"__test__"},"object":{"key":"__test__"}}}' \
     --cli-binary-format raw-in-base64-out \
     /tmp/result.json
   ```
   검증: `StatusCode=200` + `body="Skipped"` + `FunctionError` 부재.
   - **통과** → Step 7
   - **실패** → 종료. live는 이전 버전 그대로 = **자동 롤백.**

7. **Alias 전환 (smoke 통과 시에만)**
   ```bash
   aws lambda update-alias \
     --function-name rehearse-analysis-dev \
     --name live \
     --function-version "$NEW"
   ```
   원자적 전환. 이 순간부터 EventBridge가 새 버전을 호출.

### 롤백

문제 발견 시:

```bash
aws lambda update-alias \
  --function-name rehearse-analysis-dev \
  --name live \
  --function-version "$CURRENT"
```

한 줄, 즉시 반영. 이전 코드는 불변 버전으로 살아 있으므로 안전.

## 인프라 변경 사항 (1회성 마이그레이션)

이전 구조 → alias 구조 전환을 위해 수행한 작업 (2026-04-07):

1. **첫 버전 발행**
   `aws lambda publish-version` → `v1` 생성

2. **`live` alias 생성**
   `aws lambda create-alias --name live --function-version 1`

3. **EventBridge용 Lambda permission 추가** (alias ARN 대상)
   ```bash
   aws lambda add-permission \
     --function-name rehearse-analysis-dev \
     --qualifier live \
     --statement-id eventbridge-invoke-analysis-live \
     --action lambda:InvokeFunction \
     --principal events.amazonaws.com \
     --source-arn arn:aws:events:ap-northeast-2:776735194358:rule/rehearse-video-uploaded-dev
   ```
   > 기존 unqualified ARN용 permission(`eventbridge-invoke-analysis`)은 그대로 유지. 정리는 추후.

4. **EventBridge 타겟 교체**
   ```bash
   aws events put-targets \
     --rule rehearse-video-uploaded-dev \
     --targets '[{"Id":"analysis-lambda","Arn":"arn:aws:lambda:ap-northeast-2:776735194358:function:rehearse-analysis-dev:live"}]'
   ```

`rehearse-convert-dev`도 동일 단계로 마이그레이션 완료 (코드 변경 없이 현재 코드를 v1로 발행 → alias 생성 → permission 추가 → EB 타겟 교체).

## 안전 장치 요약

| 단계 | 장치 | 효과 |
|------|------|------|
| Step 2 | 구문 + f-string 검증 | 런타임 오류 사전 차단 |
| Step 4 | 현재 버전 백업 | 롤백 대상 확보 |
| Step 5 | `publish-version` | 불변 스냅샷 |
| Step 6 | alias 전환 **전** smoke test | 장애 사전 차단 (live=이전 버전) |
| Step 7 | alias 기반 전환 | 원자적, 즉시 롤백 가능 |
| 실패 시 | alias 미전환 | 기존 버전 계속 서빙 = **자동 롤백** |

## 운영 명령어 모음

```bash
# 현재 live 버전 확인
aws lambda get-alias --function-name rehearse-analysis-dev --name live \
  --query FunctionVersion --output text

# 발행된 모든 버전 목록
aws lambda list-versions-by-function --function-name rehearse-analysis-dev \
  --query 'Versions[*].[Version,LastModified,Description]' --output table

# 특정 버전으로 롤백
aws lambda update-alias --function-name rehearse-analysis-dev \
  --name live --function-version <VERSION>

# EventBridge 현재 타겟 확인
aws events list-targets-by-rule --rule rehearse-video-uploaded-dev \
  --query 'Targets[*].[Id,Arn]' --output text
```

## 관련 문서

- 자동화 스킬: `.claude/skills/lambda-deploy/SKILL.md`
- 인프라 현황: `docs/architecture/infrastructure-status.md`
- 분석 파이프라인: `docs/architecture/recording-analysis-pipeline.md`
