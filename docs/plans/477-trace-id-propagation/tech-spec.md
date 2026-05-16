# Tech Spec — 분산 추적 ID (traceId) 전 구간 통일 + 로그 일원화

> **작성자**: backend agent
> **답하는 질문**: 어떻게? 구조 / API / 데이터 / Trade-off
> **승인 게이트**: ★ 사용자 명시 승인 후 implement.md 진입 ★
> **선행 문서**: `product-spec.md` (수용 기준 / hop 정의 / phase 분리)

---

## Why → Goal (1줄 미러)

운영자가 사용자 액션 1건의 BE → S3 → Lambda → BE 콜백 → 비동기 후속 처리 로그를 **단일 traceId 1개**로 CloudWatch 단일 콘솔에서 cross-group 1회 쿼리 만에 시간순 재구성한다 (현재는 두 콘솔 + 사람 짜맞춤). dev / prod 환경은 분리된 로그 그룹.

---

## Evidence

### 현재 구조 (Pre)

- **Hop 1 (BE 진입, 일반 사용자 API)**: traceId MDC 미적재. `InternalApiKeyFilter` 는 `/api/internal/**` 만 가드 → 사용자 진입 hop 누락.
- **Hop 2 (S3 객체)**: `AwsS3Service#generatePutPresignedUrl(s3Key, contentType)` (`backend/src/main/java/com/rehearse/api/infra/aws/AwsS3Service.java:34-46`) 가 `PutObjectRequest` 에 metadata 미주입 → 업로드된 객체에 식별자 부재.
- **Hop 3 (Lambda 진입)**: `lambda/analysis/handler.py:80` 가 `f"{interview_id}-{question_set_id}-{uuid4().hex[:8]}"` 형태로 **자체 생성** → BE 발급 액션과 단절. convert Lambda (`lambda/convert/handler.py:28`) 동일 패턴.
- **Hop 4 (Lambda → BE 콜백 송신)**: `lambda/analysis/api_client.py:32` 가 `X-Correlation-Id` 헤더 송신. convert 동일 (`lambda/convert/api_client.py:28`).
- **Hop 5 (BE 콜백 수신)**: `InternalApiKeyFilter.java:23-24,48-50` 가 `X-Correlation-Id` → MDC key `correlationId` 로 적재. logback 패턴 `[%X{correlationId:-}]` (`logback-spring.xml:5`).
- **Hop 6 (BE 내부 비동기 리스너)**: 6개 `eventPublisher.publishEvent` 호출 + 4개 listener 메서드 (실제 클래스 3 + 메서드 4):
  - 발행: `ResumeTurnEventPublisher.java:50` / `FeedbackService.java:48` / `InterviewCreationService.java:63` / `InterviewService.java:97` / `InterviewCompletionService.java:55` / `FollowUpTransactionHandler.java:166`
  - 이벤트 record: `QuestionGenerationRequestedEvent` / `InterviewCompletedEvent` / `TurnCompletedEvent` / `DeliveryEnrichmentRequestedEvent` (`domain/{interview,feedback}/.../event/`)
  - listener: `QuestionGenerationEventHandler` (1 메서드, `vtExecutor`) / `RubricScoringEventListener` (1 메서드, `rubricScoringExecutor`) / `SessionFeedbackEventListener` (2 메서드, `sessionFeedbackExecutor`)
  - executor: `AsyncConfig.java` (`vtExecutor`, `DelegatingSecurityContextExecutor` 만 wrap, MDC 전파 X) / `RubricScoringExecutorConfig.java` / `SessionFeedbackExecutorConfig.java` (둘 다 plain `Executors.newThreadPerTaskExecutor`, MDC wrap 없음)
- **로그 적재**: EC2 docker stdout (BE) + CloudWatch Logs (Lambda) 분산. `backend/docker-compose.yml` (dev) / `docker-compose.prod.yml` (prod) 모두 logging driver 미설정 → json-file 기본.
- **AWS Lambda 함수 분리**: `rehearse-analysis-{dev,prod}`, `rehearse-convert-{dev,prod}` 이미 별도 함수. 자체 log group 보유.
- **FE 업로드**: `frontend/src/hooks/use-s3-upload.ts:56-58` 가 `xhr.open('PUT', url)` + `Content-Type` 헤더만 송신. 추가 헤더 미사용.

### 회귀 grep 인벤토리

`X-Correlation-Id` / `correlationId` / `MDC_CORRELATION` 등장 위치 (전수):

| 영역 | 파일 | 비고 |
|------|------|------|
| BE main | `logback-spring.xml:5` | 로그 패턴 |
| BE main | `InternalApiKeyFilter.java:23-24,48-50,71` | 헤더 / MDC key |
| BE test | (0건) | 통합 테스트 미정. 신규 작성 영역 |
| Lambda analysis | `handler.py:73,80-82` / `api_client.py:21-32` / `tests/conftest.py:43` / `tests/test_lambda_content_removal.py:46` | `_correlation_id` / `set_correlation_id` / 헤더 |
| Lambda convert | `handler.py:28-30,74` / `api_client.py:17-28` | 동일 |

→ 영향 범위 명시. BE 코드 2 파일, Lambda 코드 6 파일, BE 테스트 0 파일.

### 외부 레퍼런스

- AWS Java SDK v2 `PutObjectRequest.metadata(Map)` 동작:
  - presigned PUT URL 생성 시 user metadata 키는 SignedHeader 에 포함 (`x-amz-meta-trace-id` 형태). 클라이언트가 PUT 시 **반드시 같은 헤더로 송신**해야 SignatureDoesNotMatch 회피.
  - 검증: AWS SDK 공식 문서 + Stack Overflow `presigned-url metadata` 다수 보고. FE 가 헤더 송신 누락 시 PUT 403 → 즉시 실패 회귀로 검출.
- AWS Docker `awslogs` log driver: ECS / EC2 일반 Docker 동일 동작. EC2 IAM Role 에 `logs:CreateLogStream` + `logs:PutLogEvents` 권한 필요.
- CloudWatch Logs Insights cross-group: `SOURCE 'logGroup1' | 'logGroup2' | filter @message like /traceId/` 형태 가능.

### 사용자 발화 (특정 결정 근거)

- "Option C 채택. metadata + FE 헤더 송신. S3 key / DB 컬럼 / parse_raw_key 무변경" (사용자 결정).
- "Sunset window 없음. 헤더 단일화 즉시. fallback 미도입. WARN spike = 의도된 trade-off" (사용자 결정).
- "Executor wrap 회귀 차단 = 통합 테스트 3개 (executor 별 1). BeanPostProcessor / ArchUnit 도입 X" (사용자 결정).
- "convert Lambda 동반 포함" (사용자 결정).
- "Hop 6 = 페이로드 traceId 필드 (신뢰 기준). Executor wrap 은 보조" (사용자 결정).
- "awslogs driver = `docker-compose.yml` + `docker-compose.prod.yml` 둘 다" (사용자 결정).
- "`backend/docker-compose.yml` = dev EC2 전용. 로컬 개발자는 `backend/docker-compose.local.yml` (MySQL 단독) 사용. awslogs driver 적용 시 로컬 영향 없음 → compose override 분리 불필요" (사용자 결정).

### 추정 / 미확인 가정

- **EC2 IAM Role 권한**: 현재 IAM Role 에 CloudWatch Logs write 권한 부재 가능성. PR-C 진입 시 사용자 확인 필요. 부재 시 IAM Role 정책 추가 또는 별도 access key 결정.
- **CloudWatch metric filter alert (sunset 위반 탐지)**: SNS topic / 알람 채널 부재 시 PR-C 에서 사용자에게 채널 선택 질문.
- **`rehearse-analysis` Lambda log group 명**: 현재 함수명 = log group 명 (`/aws/lambda/rehearse-analysis-dev` 등). 변경 없음 가정.

---

## 용어 정의 — "fallback" 두 종류 분리

본 문서 내 "fallback" 은 두 종류가 혼재. 혼동 금지:

| 종류 | 정의 | 본 plan 적용 |
|------|------|-------------|
| **헤더 dual-pattern fallback** | `X-Correlation-Id` (구) 와 `X-Trace-Id` (신) 둘 다 인식. transitional sunset window 코드 | **없음** — 즉시 단일화 (`X-Trace-Id` 만 인식). 사용자 명시 결정 |
| **MDC UUID fallback** | 헤더 부재 / 패턴 위반 시 UUID 자체 생성 후 MDC 적재 + WARN. 요청 reject 하지 않고 정상 처리 | **있음** — 운영 가시성 유지 / in-flight 호출 mismatch 흡수 |

이하 본문에서 "fallback" 단독 등장 시 = **MDC UUID fallback**. "헤더 fallback" / "dual-pattern fallback" 표기는 명시적으로 종류 1을 지칭.

---

## 본질 challenge — 더 단순한 대안 검토

> **시니어 self-review**: "이 설계, 과한가? 더 단순한 길은?"

| 대안 | 단순도 | 채택 X 사유 |
|------|--------|------------|
| **Lambda 자체 ID 유지 + BE 발급 ID 와 매핑 테이블** | 코드 변경 적음 | 매핑 테이블 운영 부담. cross-group 쿼리 시 JOIN 필요. 운영자 1회 쿼리 목표 위반 |
| **S3 key 에 traceId 임베드** (예: `interviews/raw/.../trace-{uuid}.webm`) | 추가 인프라 0 | DB `file_metadata.s3_key` 정합 / convert Lambda output key derive 로직 / FE 표시 영향 큼. 사용자 명시 X |
| **EventBridge event detail 에 traceId 주입** | infra-level | Lambda S3 직접 트리거 형태 변경 필요. EventBridge custom event 도입 부담. 본 epic scope 외 |
| **Option C (채택)**: metadata + FE 헤더 송신 | S3 key / DB 무변경. presigned 추가 인자만 | Lambda head_object 1회 호출 추가 비용 (~5ms × 호출당 1회). 허용. FE 헤더 누락 시 PUT 403 즉시 회귀 (회복 가능) |

결론: Option C 가 영향 범위 최소. parse_raw_key / s3_key 패턴 / DB 컬럼 전부 무변경. 인프라 추가 (EventBridge / 매핑 테이블) 도 없음.

---

## Trade-offs

### Option A (채택) — S3 metadata + FE 헤더 송신 + 헤더 단일화 즉시 + 이벤트 페이로드 traceId 필드

- 장점:
  - S3 key / DB `file_metadata.s3_key` / `parse_raw_key` 정규식 전부 무변경 → 영향 범위 최소
  - 이벤트 페이로드 traceId 필드 = 신뢰 기준 (Executor wrap 누락 시에도 traceId 유지)
  - sunset window 없음 → 코드 복잡도 낮음 (fallback 분기 / dual-pattern 부재)
- 단점:
  - BE + Lambda 동시 배포 ordering 필수. 어긋남 시 WARN spike + fallback UUID
  - FE 가 PUT 시 헤더 누락하면 S3 SignatureDoesNotMatch 403 (의존성 추가)
  - Lambda 진입마다 `head_object` 1회 추가 (~5ms)
- 사유:
  - 사용자 명시 결정 (개념 일원화 우선, sunset 비용 회피)
  - PUT 403 회귀는 통합 시점 즉시 검출 가능 (정상 / 비정상 이분 명확)
  - WARN spike = 의도된 trade-off (CloudWatch metric filter 로 가시화)

### Option B (폐기) — sunset window + 헤더 fallback (`X-Correlation-Id` 우선, `X-Trace-Id` 폴백)

- 장점:
  - BE / Lambda 배포 ordering 무관
  - 기존 운영자 추적 동선 (검색 패턴) 호환
- 단점:
  - 코드 복잡도 ↑ (filter 2 branch / Lambda 2 header). 명명 일원화 목표 (Goal 3) 위반
  - sunset 후 회수 PR 필요 (별도 작업 분기)
- 폐기 사유: 사용자 명시 ("개념 일원화 우선 / 호환 보장 X 의도된 결정"). product-spec Acceptance Criteria phase 1 마지막 항목 = "개념 일원화 영향 공지 완료".

### Option C (폐기) — Lambda 자체 ID 유지 + BE 발급 ID 와 매핑 테이블

- 장점: 코드 변경 적음
- 단점: 매핑 테이블 운영 / cross-group JOIN 비용 / 운영자 1회 쿼리 목표 위반
- 폐기 사유: Goal "단일 식별자 1회 쿼리" 정면 위반

---

## Architecture

### 시퀀스 (Hop 1 → 6)

```
[Hop 1] 사용자 진입 API (예: POST /api/v1/interviews/{}/question-sets/{}/upload-url)
   ├─ TraceIdFilter — Authorization 기반 일반 API 진입 hop. X-Trace-Id 헤더 있으면 사용, 없으면 생성
   ├─ MDC.put("traceId", traceId)
   ├─ Controller / QuestionSetService.generateUploadUrl
   │    └─ AwsS3Service.generatePutPresignedUrl(s3Key, contentType, traceId)
   │         └─ PutObjectRequest.metadata({"trace-id": traceId})  ← signed header
   └─ Response Body: UploadUrlResponse { uploadUrl, s3Key, fileMetadataId, traceId }

[FE] useS3Upload(blob, presignedUrl, traceId)
   └─ xhr.setRequestHeader('x-amz-meta-trace-id', traceId)
       └─ S3 PUT (signed header 일치 → 200)

[Hop 2] S3 객체
   └─ Metadata { "trace-id": <BE 발급 traceId> }

[S3 ObjectCreated → EventBridge → Lambda]

[Hop 3] Lambda 진입 (analysis / convert 공통)
   ├─ s3.head_object(Bucket, Key) → Metadata['trace-id']
   ├─ 부재 시 fallback UUID + WARN("missing trace-id in S3 metadata: key=...")
   └─ api_client.set_trace_id(trace_id)

[Hop 4] Lambda → BE 콜백 송신
   └─ httpx.put(url, headers={"X-Trace-Id": trace_id, ...})

[Hop 5] BE 콜백 수신
   ├─ InternalApiKeyFilter — X-Trace-Id 추출
   ├─ MDC.put("traceId", traceId)
   └─ Controller → Service → eventPublisher.publishEvent(...)

[convert Lambda 만] transcoded MP4 출력
   └─ s3.copy_object(MetadataDirective='REPLACE', Metadata={"trace-id": trace_id})

[Hop 6] BE 내부 비동기 (AFTER_COMMIT listener)
   ├─ 이벤트 발행 시 MDC.get("traceId") 캡처 → record 의 traceId 필드 set
   ├─ Async executor 진입 (vtExecutor / rubricScoringExecutor / sessionFeedbackExecutor)
   │    └─ MdcContextExecutor wrap — 호출자 MDC snapshot → task thread 복원
   └─ listener 진입 첫 줄: MDC.put("traceId", event.traceId())  ← 신뢰 기준
        (Executor wrap 보조 + 페이로드 필드 우선)
```

### 컴포넌트 변경 맵

| Layer | 신규 / 변경 | 책임 |
|-------|-------------|------|
| `global/config/TraceIdFilter` | 신규 | 일반 사용자 API (`/api/v1/**` 모든 인증 진입점) — `X-Trace-Id` 헤더 사용 또는 발급. MDC 적재. `/api/internal/**` 은 `InternalApiKeyFilter` 가 담당 (filter 책임 분리) |
| `global/config/InternalApiKeyFilter` | 변경 | 헤더명 / MDC key `X-Trace-Id` / `traceId` 로 rename. **헤더 dual-pattern fallback 미도입** (`X-Correlation-Id` 인식 X — 즉시 단일화). 헤더 부재 / 패턴 위반 시 **MDC UUID fallback** (WARN + UUID 생성 후 MDC 적재 + 정상 통과, reject X) |
| `logback-spring.xml` | 변경 | `[%X{correlationId:-}]` → `[%X{traceId:-}]` |
| `global/util/MdcContextExecutor` | 신규 | Executor 데코레이터. 호출자 MDC snapshot → task thread 복원. Throwable safe |
| `global/config/AsyncConfig` | 변경 | `vtExecutor` bean = `MdcContextExecutor.wrap(DelegatingSecurityContextExecutor(VT))` |
| `global/config/RubricScoringExecutorConfig` | 변경 | 동일 wrap |
| `global/config/SessionFeedbackExecutorConfig` | 변경 | 동일 wrap |
| `infra/aws/S3Service` (port) | 변경 | `generatePutPresignedUrl(String s3Key, String contentType, String traceId)` 시그니처 |
| `infra/aws/AwsS3Service` | 변경 | (1) `PutObjectRequest.metadata(Map.of("trace-id", traceId))`. (2) `retriggerUploadEvent(s3Key, traceId)` 의 `CopyObjectRequest` 에 `metadataDirective("REPLACE")` 유지 + `metadata(Map.of("trace-id", traceId))` 추가 (REPLACE 시 신규 metadata 명시 안 하면 손실) |
| `infra/aws/MockS3Service` | 변경 | 두 시그니처 추가만 (동작 무변경) |
| `domain/question/service/InternalQuestionSetService:142` | 변경 | `retriggerUploadEvent` 호출 전 `MDC.get("traceId")` 캡처 (AFTER_COMMIT 람다 outer 변수로 snapshot) → 매개변수 전달 |
| `domain/question/dto/UploadUrlResponse` | 변경 | `traceId` 필드 추가 (Builder) |
| `domain/question/service/QuestionSetService#generateUploadUrl` | 변경 | `MDC.get("traceId")` 추출 → S3Service + Response 에 전달 |
| 이벤트 record 4종 | 변경 | `traceId` 필드 추가 (record 컴포넌트). 정적 팩토리 보유한 `TurnCompletedEvent` 는 팩토리 시그니처 갱신 + 호출부 동시 수정 |
| 이벤트 발행자 6곳 | 변경 | 발행 직전 `MDC.get("traceId")` 캡처 → 이벤트 record 의 traceId 필드 주입 |
| 4 listener (3 클래스) | 변경 | 진입 첫 줄 `MDC.put("traceId", event.traceId())` + finally `MDC.remove("traceId")` |
| `backend/docker-compose.yml` | 변경 | `logging.driver: awslogs` + dev log group |
| `backend/docker-compose.prod.yml` | 변경 | 동일. prod log group |
| `lambda/analysis/api_client.py` | 변경 | `_correlation_id` → `_trace_id`. 헤더명 `X-Trace-Id` |
| `lambda/analysis/handler.py` | 변경 | 자체 생성 제거. `head_object` → metadata 추출. fallback UUID + WARN |
| `lambda/analysis/tests/conftest.py` + `test_lambda_content_removal.py` | 변경 | `set_correlation_id` → `set_trace_id` |
| `lambda/convert/api_client.py` | 변경 | 동일 |
| `lambda/convert/handler.py` | 변경 | 자체 생성 제거 + head_object + transcoded 출력 시 `s3.copy_object(MetadataDirective='REPLACE', Metadata={"trace-id": trace_id})` |
| `frontend/src/hooks/use-s3-upload.ts` | 변경 | `upload(blob, presignedUrl, traceId)` 시그니처. `xhr.setRequestHeader('x-amz-meta-trace-id', traceId)` |
| FE 호출부 (3 site) | 변경 | `UploadUrlResponse.traceId` 받아 useS3Upload 에 전달. 인벤토리: `frontend/src/hooks/use-answer-flow.ts:166`, `frontend/src/hooks/use-interview-session.ts:293`, `frontend/src/hooks/use-interview-session.ts:366` |
| `backend/AGENTS.md` | 변경 (PR-D) | "신규 executor bean 추가 시 MdcContextExecutor wrap + 통합 테스트 1개 추가" 1줄 명시 |

---

## Data Model

DB 스키마 변경 **없음**. S3 object metadata 채널 추가 (DDL 무관, 운영 metadata).

```
S3 object user metadata
─────────────────────────
trace-id: <8-32자 ASCII>   ← signed header (PUT 시 FE 가 'x-amz-meta-trace-id' 헤더로 송신)
```

이벤트 record 필드 추가 (코드 레벨, DB 무관):

```java
// 변경 전
public record QuestionGenerationRequestedEvent(Long interviewId, ...) {}

// 변경 후
public record QuestionGenerationRequestedEvent(
    String traceId,
    Long interviewId, ...
) {}
```

4 이벤트 record 모두 동일 패턴 (`traceId` 첫 컴포넌트).

---

## API Contract

### 1. 사용자 진입 API (Hop 1) — 모든 사용자 노출 엔드포인트

**요청 헤더** (선택):
```
X-Trace-Id: <8-32자 ASCII, [a-zA-Z0-9-]>   # 부재 시 BE 생성
```

**검증 룰** (TraceIdFilter):
- 부재 → BE 가 `UUID.randomUUID().toString().replace("-", "").substring(0, 16)` 생성
- 길이 8-32 / `[a-zA-Z0-9-]` 외 → WARN + 폐기 → 신규 생성 (reject X)
- MDC `traceId` 적재. 응답 헤더 `X-Trace-Id` 동봉 (관찰성)

### 2. POST `/api/v1/interviews/{interviewId}/question-sets/{questionSetId}/upload-url`

**Request** (변경 없음):
```json
{ "contentType": "video/webm" }
```

**Response 200** (변경: `traceId` 추가):
```json
{
  "success": true,
  "data": {
    "uploadUrl": "https://...",
    "s3Key": "interviews/raw/2026/05/15/.../....webm",
    "fileMetadataId": 123,
    "traceId": "a1b2c3d4e5f6g7h8"
  }
}
```

### 3. FE → S3 PUT (presigned URL)

```
PUT <presignedUrl>
Content-Type: video/webm
x-amz-meta-trace-id: <traceId>      # 신규 - 필수 (signed header 일치)
<binary body>
```

부재 시 S3 403 SignatureDoesNotMatch.

### 4. Lambda → BE 콜백 (Hop 4 → Hop 5)

**요청 헤더** (변경):
```
X-Internal-Api-Key: ...
X-Trace-Id: <S3 metadata 에서 추출한 traceId>
```

기존 `X-Correlation-Id` 폐기. fallback 미지원.

### 에러

- 사용자 API hop 1 → traceId 부재 / 패턴 위반 시 reject X (WARN + 생성)
- 내부 API hop 5 → 동일 (WARN + fallback UUID)
- S3 PUT → FE 헤더 누락 시 S3 자체 403 SignatureDoesNotMatch. BE 응답 코드 무관

---

## Verification (완료 판정)

### PR-A (헤더 단일화 + MDC rename + 일반 사용자 API filter)

- [ ] **Unit / Domain Unit**
  - `TraceIdFilterTest` — 헤더 있음 / 없음 / 패턴 위반 3 케이스 MDC 적재 검증
  - `InternalApiKeyFilterTest` — `X-Trace-Id` 헤더 적재 / 부재 시 fallback UUID 적재
- [ ] **Lambda 단위**
  - `lambda/analysis/tests/test_api_client.py` (신규) — `set_trace_id` + 헤더 송신 검증
- [ ] **빌드 / 린트**: `./gradlew compileJava`, `cd lambda/analysis && pytest`
- [ ] **회귀 grep 0건**: `correlationId` / `X-Correlation-Id` / `set_correlation_id` BE main + lambda 양쪽 0건
- [ ] **logback 패턴**: `%X{traceId:-}` 단일

### PR-B (S3 metadata 채널 + FE 헤더 송신 + Lambda head_object)

- [ ] **Service Integration**
  - `QuestionSetServiceIntegrationTest#generateUploadUrl_traceId_propagation` — MDC traceId 주어진 상태에서 generateUploadUrl 호출 → Response.traceId 일치 + (Mock S3 검증으로) PutObjectRequest metadata 에 trace-id 키 포함 검증
- [ ] **Infra Integration**
  - `AwsS3ServiceIntegrationTest#generatePutPresignedUrl_includesTraceMetadata_inSignedHeader` — 실제 S3Presigner 가 생성한 URL 의 signed headers 에 `x-amz-meta-trace-id` 포함 검증
- [ ] **FE Integration**
  - `frontend/src/hooks/__tests__/use-s3-upload.test.tsx#PUT_요청에_trace_id_헤더_포함` — XHR mock 으로 `x-amz-meta-trace-id` 헤더 검증
- [ ] **Lambda 단위**
  - `lambda/analysis/tests/test_handler.py#handler_extracts_trace_id_from_s3_metadata` — boto3 stub `head_object` 응답 metadata 에서 추출
  - `lambda/analysis/tests/test_handler.py#handler_uses_fallback_uuid_when_metadata_absent` — WARN 출력 + fallback UUID 사용
  - `lambda/convert/tests/test_handler.py` (신규 또는 기존 갱신) — 동일 + `s3.copy_object` MetadataDirective 검증
- [ ] **수동 E2E**: dev 환경 1회 인터뷰 답변 업로드 → S3 콘솔에서 객체 metadata 확인 → Lambda CloudWatch 로그에서 동일 traceId 확인
- [ ] **hop 2 실 S3 metadata 캡처 (수동 1회)**: `aws s3api head-object --bucket <dev-bucket> --key <dev 업로드 객체 키>` 실행 → `Metadata.trace-id` 값 확인 + 캡처. 자동 통합 테스트 (signed header 검증) 는 URL 레벨만 검증하므로, 실 S3 객체에 metadata 가 최종 저장되는지 별도 수동 검증 필수. 캡처 결과 `docs/plans/477-trace-id-propagation/` 폴더 archive 권장 (선택)

### PR-C (CloudWatch awslogs driver)

- [ ] dev / prod compose 양쪽 `logging.driver: awslogs` 적용
- [ ] EC2 IAM Role 에 `logs:CreateLogStream` + `logs:PutLogEvents` 확인 (사용자 사전 점검 필요)
- [ ] dev 1회 컨테이너 재기동 → `/rehearse/backend/dev` 로그 그룹 생성 / 적재 확인
- [ ] CloudWatch Logs Insights 에서 `/rehearse/backend/dev` + `/aws/lambda/rehearse-analysis-dev` 동시 선택 → traceId 단일 값으로 hop 1, 3, 4, 5 결과 시간순 반환
- [ ] CloudWatch metric filter alert — 패턴 `"missing or invalid X-Trace-Id"` 5분 ≥1 → SNS 알람 (사용자가 SNS topic 결정)

### PR-D (Hop 6 — ApplicationEvent traceId + Executor wrap)

- [ ] **Service Integration** (Executor wrap)
  - `VtExecutorMdcPropagationTest#호출자_traceId_가_task_thread_에_복원된다` — `@Autowired @Qualifier("vtExecutor")` Executor 에 task submit → CompletableFuture 결과로 task thread 의 `MDC.get("traceId")` 일치 검증
  - `RubricScoringExecutorMdcPropagationTest#동일`
  - `SessionFeedbackExecutorMdcPropagationTest#동일`
- [ ] **Domain Unit**
  - `MdcContextExecutorTest` — snapshot 캡처 / 복원 / finally clear / task 예외 시 clear 4 케이스
- [ ] **Service Integration** (이벤트 페이로드)
  - `RubricScoringEventListenerIntegrationTest#listener_가_payload_의_traceId_를_MDC_에_복원한다` — Spring 컨텍스트 + 실 listener 호출, MDC 결과 검증
  - 동일 패턴 3 listener 메서드 추가 (`QuestionGenerationEventHandler`, `SessionFeedbackEventListener` 2 메서드)
- [ ] **회귀 가드**: `backend/AGENTS.md` 에 "신규 executor bean 추가 시 MdcContextExecutor wrap + 통합 테스트 1개 추가" 1줄 명시

### 통합 (4 PR 종료 후 phase 1 + phase 2 수용 기준 매핑)

| product-spec 항목 | 검증 PR |
|-------------------|---------|
| Hop 1 진입 로그 traceId 등장 | PR-A + PR-C |
| Hop 2 S3 객체 metadata 보유 | PR-B |
| Hop 3 Lambda 첫 로그 동일 traceId | PR-B |
| Hop 4-5 동일 traceId (헤더 단일화) | PR-A |
| EC2 stdout → CloudWatch (dev/prod 분리) | PR-C |
| cross-group 1회 쿼리 hop 1, 3, 4, 5 시간순 반환 | PR-C |
| 개념 일원화 공지 완료 | PR-A 머지 직후 (운영 노트 / 사용자 알림) |
| Hop 6 동일 traceId | PR-D |
| 4 비동기 진입점 traceId 일치 | PR-D |
| 신규 비동기 진입점 회귀 탐지 | PR-D (executor 통합 테스트 3 + AGENTS.md 안내) |

---

## Pre / Post State

### Pre

- BE 진입: hop 1 MDC 미적재 (사용자 API filter 부재)
- S3 객체: metadata 미주입
- Lambda: 자체 생성 ID (BE 발급과 단절)
- BE 콜백 수신: `X-Correlation-Id` → MDC `correlationId`
- 비동기 listener: MDC 미복원 (호출자 traceId 단절)
- 로그 적재: EC2 docker stdout + CloudWatch Logs 분산
- compose: logging driver 미설정

### Post

- BE 진입: TraceIdFilter — `X-Trace-Id` 사용 또는 발급, MDC `traceId`
- S3 객체: metadata `trace-id` 보유 (FE PUT 헤더 송신 강제)
- Lambda: S3 head_object → metadata 추출. 자체 생성 제거 (fallback 만)
- BE 콜백 수신: `X-Trace-Id` → MDC `traceId`. fallback 미도입
- 비동기 listener: 이벤트 record `traceId` 필드 + MdcContextExecutor wrap 이중화
- 로그 적재: EC2 stdout → CloudWatch (`/rehearse/backend/{dev,prod}`)
- compose: `logging.driver: awslogs` (dev / prod 양쪽)

### Diff 핵심

```
# 헤더 / MDC key
- X-Correlation-Id / correlationId
+ X-Trace-Id / traceId

# S3 PUT request
- PutObjectRequest.builder().bucket(b).key(k).contentType(ct).build()
+ PutObjectRequest.builder().bucket(b).key(k).contentType(ct)
+     .metadata(Map.of("trace-id", traceId)).build()

# 이벤트 record
- public record QuestionGenerationRequestedEvent(Long interviewId, ...)
+ public record QuestionGenerationRequestedEvent(String traceId, Long interviewId, ...)

# Executor bean
- @Bean Executor vtExecutor() { return new DelegatingSecurityContextExecutor(...); }
+ @Bean Executor vtExecutor() { return MdcContextExecutor.wrap(new DelegatingSecurityContextExecutor(...)); }

# Lambda handler
- correlation_id = f"{interview_id}-{question_set_id}-{uuid4().hex[:8]}"
+ trace_id = _extract_trace_id_from_s3_metadata(bucket, key) or _fallback_uuid()
```

---

## 위험 / 마이그레이션 / 롤백

### 위험

| 위험 | 발생 조건 | 영향 | 대응 |
|------|----------|------|------|
| BE / Lambda 배포 ordering 어긋남 | PR-A 머지 후 Lambda 배포 지연 | in-flight 호출에서 헤더 mismatch → fallback UUID 사용, 한 액션 traceId 분리 (WARN) | PR-A 머지와 동시에 Lambda safe-deploy 실행. CloudWatch metric filter 로 가시화 |
| FE PUT 헤더 누락 | PR-B 머지 후 FE 미배포 | S3 SignatureDoesNotMatch 403 → 업로드 전체 실패 | PR-B = BE+FE 동시 변경. FE merge 가 BE merge 와 동시 배포. 단계별 배포 시 BE 후 / FE 전 windows 에서 업로드 실패 (의도). FE 머지 우선 검토 권고 |
| `head_object` 호출 비용 증가 | Lambda 진입마다 | S3 1 API call (~5ms) | 허용. 향후 EventBridge custom event 대안 검토 |
| 이벤트 record 컴포넌트 추가 = 기존 호출부 컴파일 에러 | record 인 경우 강제 | 전부 호출부 동시 수정 필요 | PR-D 한 PR 안에서 발행자 6곳 동시 변경 (atomic) |
| EC2 IAM Role 권한 부재 | awslogs driver 시작 실패 | 컨테이너 재기동 실패 | PR-C 진입 전 사용자 확인. IAM Role 정책 부재 시 별도 PR / 결정 게이트 |

### 마이그레이션 전략

- **PR-A**: 헤더 단일화. BE 와 Lambda 동시 배포 강제. in-flight 호출은 fallback UUID 로 처리 (의도)
- **PR-B**: S3 metadata + FE 헤더. BE merge → FE merge → 둘 다 prod 배포 동시. 부분 배포 windows 에서 신규 업로드 실패 가능 (FE 미배포 상태에서 BE 만 배포 시 = FE 가 traceId 미수신 → 헤더 미송신 → 403). 짧은 windows 권고
- **PR-C**: awslogs driver. 컨테이너 재기동 1회 (다운타임 < 1분). nginx 502 잠시
- **PR-D**: 코드 only. 배포 ordering 무관

### 롤백

| PR | 롤백 절차 |
|----|----------|
| PR-A | BE: 이전 이미지로 ECS / EC2 컨테이너 교체. Lambda: 이전 alias 로 전환 (`lambda-safe-deploy.sh` 가 자동 처리) |
| PR-B | BE / FE / Lambda 모두 이전 버전 동시 롤백. S3 객체에 남은 metadata 는 무해 (Lambda 가 미사용 시 무시) |
| PR-C | compose `logging.driver` 라인 제거 + 컨테이너 재기동. CloudWatch 로그 그룹은 유지 (운영 시 별도 삭제) |
| PR-D | 이전 이미지로 컨테이너 교체. 이벤트 페이로드 traceId 필드는 Spring 가 무시 (record 호환 X 이므로 동일 PR 내 발행자 / listener 모두 함께 롤백 강제) |

---

## 분기 결정

- [x] **BE+FE+Lambda 분리** (PR-B 만 BE+FE 결합)
  - PR-A: BE + Lambda 동시 배포. FE 변경 없음
  - PR-B: BE + FE + Lambda 동시 배포 (강결합 — FE 헤더 송신 필수)
  - PR-C: 인프라 / BE 만
  - PR-D: BE 만

> BE+FE 동시 변경 PR (PR-B) → `implement-be.md` + `implement-fe.md` 분리 후보. 단 본 작업은 BE / FE / Lambda 가 PR 단위로 동시 변경되는 강결합 패턴이라 통합 `implement.md` 한 파일로 phase 별 영역 명시 (BE / FE / Lambda) 구성. tasks/ 폴더로 PR 별 상세 분리.

---

## 컨벤션 정합

- **트랜잭션**: 신규 코드 모두 `@Transactional(readOnly=true)` 기본 — 단 본 작업은 트랜잭션 변경 없음 (signature / 로그 / 헤더 only)
- **Entity 반환 금지**: 변경 없음 (DTO 만 변경)
- **로깅**: 한국어 + key=value placeholder. fallback / WARN 메시지 `로그 한국어` 룰 적용
  - 예: `log.warn("X-Trace-Id 헤더 부재 또는 패턴 위반 - fallback UUID 사용: path={}, given={}", path, given);`
- **Lombok**: 변경 record 는 Lombok 무관. DTO `UploadUrlResponse` 는 기존 `@Getter @Builder` 유지
- **이벤트 페이로드 규약** (`conventions.md` §Event): "식별자 + 행위 + 속성 + 이벤트시간 그 외 금지". traceId 는 운영 메타데이터로 해석 — 사용자 결정 사항 (예외 인정). conventions.md 본문 갱신 없이 본 plan 한정 예외로 진행. (← **사용자 결정 필요 시 질문 트리거 후보**)
- **Filter 패키지**: TraceIdFilter 위치 = `global/config/` (기존 `InternalApiKeyFilter` 와 동거)
- **테스트 카테고리**:
  - TraceIdFilterTest / MdcContextExecutorTest = Domain Unit (Mock 없음, jakarta servlet mock 만)
  - QuestionSetServiceIntegrationTest = Service Integration (외부 S3 Mock)
  - AwsS3ServiceIntegrationTest = Infra Integration (S3Presigner 단독, Mock 통합 default ON)
  - Executor Mdc Propagation 3종 = Service Integration (Spring 컨텍스트 + 실 Executor bean)
  - 이벤트 listener 진입 Mdc 복원 검증 4종 = Service Integration
- **비동기 테스트 동기화 패턴**: `java.util.concurrent.CountDownLatch` + `AtomicReference` (JDK 표준). Awaitility / LogCaptor 의존성 미도입 (`build.gradle.kts` 부재 + 기존 BE 테스트 정합 `GlobalRateLimiterFilterTest` / `QuestionGenerationLockTest` 와 동일 패턴)

---

## 미확인 / 사용자 결정 필요 항목

> implement.md 진입 전 명시 결정 필요. `AskUserQuestion` 트리거 후보.

1. **이벤트 페이로드 traceId 필드 = conventions.md 룰 예외 인정 여부**
   - 현 룰: "식별자 + 행위 + 속성 + 이벤트시간 그 외 금지"
   - 본 plan: traceId 는 운영 메타데이터. 룰 예외로 본 plan 한정 진행 vs conventions.md 본문 갱신 (운영 메타 추가 허용 명시)
2. **EC2 IAM Role CloudWatch Logs 권한 현황**
   - dev / prod IAM Role 에 `logs:CreateLogStream` + `logs:PutLogEvents` 보유 확인 필요 (사용자 AWS 콘솔 점검)
   - 부재 시 IAM 정책 추가 PR 별도 진행
3. **CloudWatch metric filter alert SNS topic**
   - 기존 운영 알람 SNS topic 존재 여부 / 신규 생성 필요 여부
4. **`/api/v1/**` 외 사용자 진입 hop 범위**
   - 현재 `/api/internal/**` 외 모든 사용자 진입을 TraceIdFilter 대상으로 가정. auth callback (`/oauth2/...`) / health check 등 제외 범위 명시 필요
5. **TraceIdFilter 응답 헤더 `X-Trace-Id` 동봉 여부**
   - 동봉 시 FE / 운영자 디버깅 편의 ↑. CORS expose-headers 추가 필요 가능. 미동봉 시 FE 가 응답 body `traceId` 만으로 처리
