# API: 이력서 수집 (ingest)

> Endpoint: 별도 endpoint 없음 (도메인 내부 액션)
> Action: 업로드된 이력서 파일에서 텍스트 추출 + LLM 으로 skeleton 구조화 → `resume_skeleton` 1행 INSERT
> 관련 테이블: `resume_skeleton` (write, INSERT-only)
> 관련 외부 의존: OpenAI GPT-4o-mini (primary) / Claude Haiku (fallback) — `ResumeExtractionService`

---

## 입력

| 위치 | 필드 | 타입 | 제약 | 의미 |
|------|------|------|------|------|
| arg | `interviewId` | Long | required | 부착 대상 인터뷰 |
| arg | `userId` | Long | required | 인터뷰 소유자 (이미 검증된 값) |
| arg | `file` | MultipartFile | optional | PDF 원본 (`ingest` 진입 시) |
| arg | `extractedText` | String | optional | FE 사전 추출 텍스트 (`ingestExtractedText` 진입 시) |

> `file` / `extractedText` 둘 중 하나만 사용. 두 진입점은 호출자 측에서 분기.

---

## 출력 (정상)

| 필드 | 타입 | 의미 |
|------|------|------|
| `resumeSkeletonId` | Long | INSERT 된 `resume_skeleton.id` |
| `interviewId` | Long | 부착 대상 인터뷰 |

부수 효과: 인터뷰 1건당 skeleton 1행 부착 (V28 UNIQUE). PDF 원본 / 추출 텍스트는 메모리 폐기 (DB 미보존).

## 출력 (실패)

| 코드 | 에러 코드 | 조건 |
|------|----------|------|
| 400 | `INVALID_FILE_EMPTY` | 업로드 파일 size = 0 |
| 400 | `INVALID_FILE_TOO_LARGE` | 파일 > 5MB (`MAX_FILE_SIZE`) |
| 400 | `INVALID_FILE_TYPE` | content-type 비 PDF / encrypted PDF / magic byte 불일치 |
| 422 | `EMPTY_RESUME_TEXT` | 추출 텍스트 길이 < 50자 (`MIN_RESUME_TEXT_LENGTH`) |
| 502 | `AI_CLIENT_ERROR` / `AI_PARSE_FAILED` / `AI_EMPTY_RESPONSE` / `AI_RESPONSE_INVALID` | 추출 LLM 응답 결함 |
| 503 | `AI_SERVICE_UNAVAILABLE` | OpenAI + Claude 모두 실패 |
| 504 | `AI_TIMEOUT` | LLM 응답 타임아웃 |
| 429 | `RATE_LIMITED` | Resilience4j rate limit |

---

## 흐름

### 1. 파일 또는 텍스트 수신
- `ResumeIngestionService.ingest(file, ...)` 또는 `ingestExtractedText(text, ...)` 진입.
- 파일 진입 시 `PdfTextExtractor.extract` 로 PDF → 텍스트 변환. 5MB / 5000자 제한 적용.
- 텍스트 진입 시 추출 단계 skip. 길이 검증만 수행.

### 2. 텍스트 정합성 검사
- 길이 < 50 → `EMPTY_RESUME_TEXT` (422).
- 통과 시 다음 단계.

### 3. 파일 해시 계산
- `FileHasher.sha256(text)` 로 해시 산출. 로그 키 (8자 마스킹) + DB `file_hash` 컬럼 저장.

### 4. LLM skeleton 추출
- `ResumeExtractionService.extract(text)` 호출.
- Provider chain: GPT-4o-mini → Claude Haiku (fallback) — `ResilientAiClient` 경유.
- Sampling: temperature 0.2, maxTokens 4096, response_format = `json_object`.
- Parse: `AiResponseParser.parseOrRetry` — 1차 실패 시 schema-hint 1회 재호출 → 2차 실패 시 `AI_PARSE_FAILED`.
- 응답 후처리: `implicit_cs_topic.confidence < 0.3` (`MIN_CONFIDENCE_THRESHOLD`) 항목 drop.

### 5. 저장
- `ResumeSkeletonPersister.persist(interviewId, skeleton)` — `resume_skeleton` INSERT.
- `interview_id` UNIQUE (V28). 중복 시 `DataIntegrityViolationException` catch + 재조회 (idempotent — 구 skeleton 반환).

### 6. 응답
- skeleton id 반환. 후속 단계 (plan preparation) 는 별도 호출자 책임.

---

## 외부 호출 상세

- Provider: GPT-4o-mini (primary) → Claude Haiku (fallback)
- Timeout: connect 5s, read 60s (provider 별 yml)
- Retry:
  - OpenAI 내부 retry 2회 (4xx 즉시 throw, 429/5xx/network 만 재시도, 1s/2s backoff)
  - Claude 내부 retry 3회 (동일 정책)
  - `ResilientAiClient` fallback layer 1회 (OpenAI 실패 → Claude 1회)
  - worst path 외부 호출 = 5회
- Parse 실패 시: 1차 실패 → schema-hint 1회 재호출 → 2차 실패 시 `AI_PARSE_FAILED`
- `CLIENT_ERROR` / `PARSE_FAILED` 는 fallback 진입 안 함 (즉시 throw)
- Rate limit: `resilience4j.ratelimiter.instances.openai-api / claude-api` (prod 30/s / 20/s)

---

## 조건 / 엣지

| 조건 | 동작 |
|------|------|
| 같은 interviewId 로 동시 ingest 호출 | `resume_skeleton.interview_id` UNIQUE → 후행 호출 DB 제약 위반 → `ResumeSkeletonPersister` 가 catch 후 재조회 (구 skeleton 반환) |
| PDF 텍스트 추출 결과 5000자 초과 | 앞 5000자 까지만 사용 (`MAX_TEXT_LENGTH`) — 사용자 알림 없음 (silent truncation) |
| 추출 텍스트 길이 < 50 | 422 `EMPTY_RESUME_TEXT` (LLM 호출 안 함) |
| OpenAI 5xx → Claude 5xx | 503 `AI_SERVICE_UNAVAILABLE` (이중 장애 ERROR 로그) |
| LLM 응답 schema 위반 1회 | schema-hint 재호출 |
| LLM 응답 schema 위반 2회 연속 | 502 `AI_PARSE_FAILED` |
| OpenAI 4xx (non-429) | 즉시 502 `AI_CLIENT_ERROR` (fallback 진입 X) |
| `implicit_cs_topic.confidence` < 0.3 | 해당 항목만 drop, 나머지 저장 |
| 이미 skeleton 존재한 인터뷰에 재호출 (다른 fileHash) | UNIQUE 충돌 → 구 skeleton 반환 (이력서 교체 미지원) |
| Encrypted / Scan-only PDF | PDFBox IOException → 400 `INVALID_FILE_TYPE` |

---

## 상태 전이
N/A — `resume_skeleton` INSERT-only, mutable status 없음.

---

## 관찰성

- **로그**: `[ResumeIngestionService]` / `[ResumeExtractionService]` / `[PdfTextExtractor]`
  - key fields: `interviewId`, `userId`, `fileHashPrefix(8)`, `textLength`, `provider`, `model`, `latencyMs`
  - 민감 정보 (이력서 본문) 로깅 금지
- **메트릭** (`AiCallMetrics`):
  - `rehearse.ai.call.duration{call.type=RESUME_EXTRACTOR, model, provider, cache.hit, fallback, outcome}`
  - `rehearse.ai.parse.fail.total{stage=first|second, call.type=RESUME_EXTRACTOR}`
  - `rehearse.ai.call.tokens.{input, output, cached.read, cached.write}`
- **알람**: 미정 (Issue #408)

---

## 연관 의존성

| 패키지 / 클래스 | 역할 | 관계 |
|----------------|------|------|
| `com.rehearse.api.domain.resume.service.ResumeIngestionService` | ingest 진입점 | calls |
| `com.rehearse.api.domain.resume.service.ResumeExtractionService` | LLM skeleton 추출 (temp 0.2, maxTokens 4096) | calls |
| `com.rehearse.api.domain.resume.service.ResumeSkeletonPersister` | DB persist + idempotent 재조회 | calls — persister |
| `com.rehearse.api.infra.ai.PdfTextExtractor` | PDF → text (5MB / 5000자) | calls |
| `com.rehearse.api.global.util.FileHasher` | SHA-256 해시 | calls |
| `com.rehearse.api.infra.ai.ResilientAiClient` | OpenAI / Claude 이중화 | calls — primary + fallback |
| `com.rehearse.api.infra.ai.AiResponseParser` | JSON parse + schema-hint retry | calls |
| `com.rehearse.api.infra.ai.prompt.ResumeExtractorPromptBuilder` | extract prompt 구성 | calls |
| `com.rehearse.api.domain.interview.service.InterviewCreationService` | 인터뷰 생성 시 ingest 트리거 | called-by |
| `com.rehearse.api.domain.question.service.ResumeTrackInitiator` | resume 트랙 초기화 시 ingest 트리거 (캐시 hit 시 skip) | called-by |

---

## 정책 출처

- 비즈니스 룰: `docs/domain/resume/schema.md#resume_skeleton` (인터뷰 1:1 / INSERT-only / CASCADE)
- 임계값: `ResumeIngestionService.MIN_RESUME_TEXT_LENGTH=50`, `PdfTextExtractor.MAX_FILE_SIZE=5MB / MAX_TEXT_LENGTH=5000`, `ResumeExtractionService.MIN_CONFIDENCE_THRESHOLD=0.3`
- LLM sampling: `ResumeExtractionService` 내 상수 (temperature 0.2 / maxTokens 4096)
- Retry / fallback: `ResilientAiClient` / `AiResponseParser.parseOrRetry`
- ❓ 잔존 결정 항목: Issue #408
  - A2 PDF 영구보존 정책
  - A4 ingestion 진입점 권한 검증 위치
  - C3 `ExtractedResumeSkeleton` SchemaExampleRegistry 등록 여부
