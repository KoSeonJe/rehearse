# Plan 04: Lambda prod 함수 (`rehearse-{analysis,convert}-prod`) 구성

> 상태: Draft
> 작성일: 2026-04-12
> 선행: Plan 03 (S3 + EventBridge)

## Why

dev Lambda 함수(`rehearse-analysis-dev`, `rehearse-convert-dev`)는 `S3_BUCKET=rehearse-videos-dev`, `API_SERVER_URL=http://54.180.188.135:80`, dev OPENAI 키로 세팅되어 있다. prod 영상을 처리하려면 **환경변수만 다른** 별도 함수가 필요하다. 코드 자체는 동일하므로 패키지는 재사용하되, 함수명·환경변수·IAM Role·alias만 분리한다.

또한 `lambda/analysis/config.py:8`과 `lambda/convert/config.py:9`의 기본값이 `"rehearse-videos-dev"`로 하드코딩되어 있어, 환경변수 누락 시 치명적이다. 함수 생성 시 `S3_BUCKET`을 반드시 설정하고, 배포 스크립트가 이 값을 검증하도록 보완한다.

## 생성/수정 파일

| 자원 / 파일 | 작업 |
|---|---|
| Lambda 함수 `rehearse-analysis-prod` | 신규 생성 |
| Lambda 함수 `rehearse-convert-prod` | 신규 생성 |
| Lambda alias `live` (각 함수) | 신규 생성 |
| IAM Role `rehearse-lambda-prod-role` | 신규 생성 |
| **MediaConvert 기존 Role** (`MEDIACONVERT_ROLE`) | **prod S3 버킷 권한 추가 (인라인 정책 수정)** |
| `lambda/lambda-safe-deploy.sh` | `--env prod` 인자 지원 확장 |
| `lambda/analysis/README.md`, `lambda/convert/README.md` | prod 배포 절차 문서화 |

## 상세

### 함수 1: `rehearse-analysis-prod`

| 항목 | 값 |
|---|---|
| 런타임 | Python 3.12 |
| 메모리 | 2048 MB |
| Timeout | 900 s (15분) |
| 아키텍처 | x86_64 또는 arm64 (dev와 동일하게) |
| Layer | `ffmpeg-static:1` (dev와 동일 layer ARN 재사용) |
| Handler | `handler.lambda_handler` |
| 패키지 | `lambda/analysis/` zip (dev와 동일 코드) |
| IAM Role | `rehearse-lambda-prod-role` |

#### 환경변수

| 키 | 값 |
|---|---|
| `API_SERVER_URL` | `https://api.rehearse.co.kr` |
| `INTERNAL_API_KEY` | **prod 전용 신규 키** (32+ chars, `.env.prod`와 동일 값) |
| `S3_BUCKET` | `rehearse-videos-prod` |
| `OPENAI_API_KEY` | prod 전용 OpenAI 키 |
| `USE_GEMINI` | `true` (dev와 동일 정책, 변경 시 별도 결정) |
| `GEMINI_API_KEY` | prod 전용 Gemini 키 |
| `GEMINI_MODEL` | `gemini-2.5-flash` (dev와 동일) |
| `FFMPEG_PATH` | `/opt/bin/ffmpeg` (layer 경로) |
| `FFPROBE_PATH` | `/opt/bin/ffprobe` |

### 함수 2: `rehearse-convert-prod`

| 항목 | 값 |
|---|---|
| 런타임 | Python 3.12 |
| 메모리 | 256 MB |
| Timeout | 300 s |
| Handler | `handler.lambda_handler` |
| 패키지 | `lambda/convert/` zip |
| IAM Role | `rehearse-lambda-prod-role` |

#### 환경변수

| 키 | 값 |
|---|---|
| `API_SERVER_URL` | `https://api.rehearse.co.kr` |
| `INTERNAL_API_KEY` | 위와 동일 |
| `S3_BUCKET` | `rehearse-videos-prod` |
| `MEDIACONVERT_ENDPOINT` | 기존 리전 엔드포인트 (dev와 공용) |
| `MEDIACONVERT_ROLE` | 기존 Role ARN (dev와 공용, 단 prod 버킷 권한 보강 여부 확인) |

### IAM Role `rehearse-lambda-prod-role`

**Trust**: `lambda.amazonaws.com`
**관리형 정책**: `AWSLambdaBasicExecutionRole`
**커스텀 인라인 정책**:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:GetObject", "s3:PutObject", "s3:DeleteObject", "s3:ListBucket"],
      "Resource": [
        "arn:aws:s3:::rehearse-videos-prod",
        "arn:aws:s3:::rehearse-videos-prod/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": ["mediaconvert:CreateJob", "mediaconvert:GetJob"],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": ["iam:PassRole"],
      "Resource": "<MEDIACONVERT_ROLE ARN>"
    },
    {
      "Sid": "DlqSendMessage",
      "Effect": "Allow",
      "Action": ["sqs:SendMessage"],
      "Resource": "arn:aws:sqs:ap-northeast-2:776735194358:rehearse-event-dlq-prod"
    }
  ]
}
```

**Lambda On-failure Destination** (Event Invoke Config):
```bash
aws lambda put-function-event-invoke-config \
  --function-name rehearse-analysis-prod \
  --qualifier live \
  --destination-config '{
    "OnFailure": {
      "Destination": "arn:aws:sqs:ap-northeast-2:776735194358:rehearse-event-dlq-prod"
    }
  }' \
  --maximum-retry-attempts 2
# rehearse-convert-prod도 동일 적용
```

**중요**: dev Role과 분리, prod Role이 dev 버킷 접근 불가하도록 보장.

### MediaConvert Role prod 버킷 권한 보강 (필수, 검증 후 패치)

MediaConvert 기존 Role(`MEDIACONVERT_ROLE`)은 dev 버킷만 허용할 가능성이 높다. `rehearse-convert-prod` Lambda가 job 제출 시 Role은 **MediaConvert 서비스**가 assume하며, MediaConvert가 Input(읽기) · Output(쓰기)을 수행할 때 이 Role의 S3 권한으로 `rehearse-videos-prod`에 접근한다. 권한 누락 시 job은 `ACCESS_DENIED`로 실패하고 컷오버 E2E Scenario 4(면접 완주)가 중단된다.

#### Step 1: 기존 정책 조회
```bash
# MEDIACONVERT_ROLE 이름 확인 (ARN 끝 부분)
ROLE_NAME=$(basename $MEDIACONVERT_ROLE_ARN)

# 연결된 inline policy 목록
aws iam list-role-policies --role-name "$ROLE_NAME"
# 관리형 정책 목록
aws iam list-attached-role-policies --role-name "$ROLE_NAME"

# 각 inline 정책 내용 확인
aws iam get-role-policy --role-name "$ROLE_NAME" --policy-name <name>
```

#### Step 2: prod 버킷 Resource 추가

기존 정책의 `Resource` 배열에 prod 버킷을 **추가** (dev 권한은 절대 제거하지 말 것 — dev 환경 회귀):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:GetObject", "s3:PutObject", "s3:ListBucket"],
      "Resource": [
        "arn:aws:s3:::rehearse-videos-dev",
        "arn:aws:s3:::rehearse-videos-dev/*",
        "arn:aws:s3:::rehearse-videos-prod",
        "arn:aws:s3:::rehearse-videos-prod/*"
      ]
    }
  ]
}
```

```bash
aws iam put-role-policy \
  --role-name "$ROLE_NAME" \
  --policy-name rehearse-mediaconvert-s3 \
  --policy-document file://mediaconvert-s3-policy.json
```

#### Step 3: 검증
```bash
# 정책 최신 확인
aws iam get-role-policy --role-name "$ROLE_NAME" --policy-name rehearse-mediaconvert-s3

# dev 권한 유지 확인
aws iam simulate-principal-policy \
  --policy-source-arn $MEDIACONVERT_ROLE_ARN \
  --action-names s3:GetObject \
  --resource-arns arn:aws:s3:::rehearse-videos-dev/test.webm
# → allowed

# prod 권한 추가 확인
aws iam simulate-principal-policy \
  --policy-source-arn $MEDIACONVERT_ROLE_ARN \
  --action-names s3:PutObject \
  --resource-arns arn:aws:s3:::rehearse-videos-prod/test.mp4
# → allowed
```

**주의**: Role 정책 변경은 즉시 전파되지만, 일부 MediaConvert 캐시 경로에 수초 지연 가능. plan-13 컷오버 직전이 아닌 **plan-04 실행 시점에 적용** + 24h 안정화 후 컷오버 권장.

### Alias `live`

- 두 함수 모두 `live` alias 생성, 초기 `$LATEST` 포인트
- EventBridge target 및 향후 모든 호출자는 `arn:aws:lambda:...:function:rehearse-analysis-prod:live` 형태로 alias 경유
- 배포 시 버전 발행 후 alias 전환 (lambda-safe-deploy 방식)

### 배포 스크립트 확장 (`lambda/lambda-safe-deploy.sh`)

**현재 상태 확인 필요**: 스크립트가 `rehearse-analysis-dev`를 하드코딩하는지, 또는 인자로 받는지 plan 실행 시 재확인.

**목표 인터페이스**:
```bash
./lambda/lambda-safe-deploy.sh --env prod analysis
./lambda/lambda-safe-deploy.sh --env prod convert
./lambda/lambda-safe-deploy.sh --env prod all
```

**수정 포인트**:
- `FUNCTION_NAME_SUFFIX` 변수 (`-dev` / `-prod`)를 `--env` 인자로 치환
- prod 실행 시 추가 확인 프롬프트 (`Are you sure? [y/N]`)
- smoke test payload는 환경별로 분리: `test-events/analysis-dev.json` / `test-events/analysis-prod.json`
- alias 전환 후 CloudWatch 알람 연동 확인 (plan-14 선행 여부)

**대안**: 스크립트 확장이 복잡하면 `lambda/lambda-safe-deploy-prod.sh` 복제본 생성 후 변수만 prod로 변경. 선택은 실제 스크립트 구조 확인 후 결정.

### 초기 배포 절차

1. `lambda/analysis/` 및 `lambda/convert/` 디렉토리에서 zip 패키징
2. `aws lambda create-function` 으로 신규 함수 생성 (환경변수·Role·Layer 포함)
3. alias `live` 생성, `$LATEST` 포인트
4. EventBridge target 연결 (plan-03 규칙 → prod analysis 함수 alias)
5. 더미 이벤트로 `aws lambda invoke --qualifier live` 테스트

## 담당 에이전트

- Implement: `devops-engineer` — 함수 생성, Role, alias, 스크립트 확장
- Review: `code-reviewer` — 스크립트 변경 (하드코딩 제거), 보안 경계

## 검증

- `aws lambda get-function --function-name rehearse-analysis-prod` → 함수 메타데이터 반환
- `aws lambda get-function-configuration --function-name rehearse-analysis-prod --query 'Environment.Variables'` → `S3_BUCKET=rehearse-videos-prod` 포함 확인
- `aws lambda list-aliases --function-name rehearse-analysis-prod` → `live` alias 존재
- `aws lambda invoke --function-name rehearse-analysis-prod --qualifier live --payload '{"test":true}' /tmp/out.json` → smoke test 성공
- `lambda/lambda-safe-deploy.sh --env prod analysis` 드라이런 성공
- CloudWatch Logs `/aws/lambda/rehearse-analysis-prod` 생성 확인
- **MediaConvert Role**: `iam simulate-principal-policy`로 `rehearse-videos-prod` Put/Get 모두 `allowed` + `rehearse-videos-dev` 회귀 없음 확인
- EventBridge rule target이 Lambda alias ARN(`:live` 접미)으로 설정 확인:
  `aws events list-targets-by-rule --rule rehearse-video-uploaded-prod` → Arn에 `:live` 포함
- `progress.md` Task 4 → Completed
