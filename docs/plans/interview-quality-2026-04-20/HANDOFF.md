# Interview Quality 2026-04-20 — Session Handoff Log

세션 간 컨텍스트 인계 기록. 마스터 플랜: `/Users/koseonje/.claude/plans/interview-quality-jolly-flask.md`.

---

## Session S1 (2026-04-20) — plan-00a Codebase Inventory

### 완료
- **산출물 3종 커밋 대상**:
  - `docs/plans/interview-quality-2026-04-20/INVENTORY.md` (380L)
  - `docs/plans/interview-quality-2026-04-20/TEST_BASELINE.md` (249L)
  - `docs/plans/interview-quality-2026-04-20/IMPACT_MAP.md` (364L)
- `progress.md` 00a `Draft → Completed`, REMEDIATION `M4` / `Missing PdfTextExtractor` ✅
- `plan-00a-codebase-inventory.md` 헤더 `Completed` 전환
- **테스트 베이스라인**: `./gradlew test` → 606 tests / 0 failures / 0 ignored / 56s (BUILD SUCCESSFUL)
- **IMPACT_MAP 15개 plan 커버**: 00b, 00c, 00d, 00e, 01, 02, 03, 04, 05, 06, 07, 08, 09, 10, 11

### 핵심 교정 사항 (후속 세션에서 plan 본문 edit 필요)
- `InterviewTurnService` 실존 X → 실제 진입점 **`FollowUpService.generateFollowUp(Long id, Long userId, FollowUpRequest request, MultipartFile audioFile)` at `FollowUpService.java:31`** (plan-01/07 본문에 적용)
- `InterviewSession` 실존 X → aggregate root는 `Interview` entity. 런타임 상태는 plan-00c의 `InterviewRuntimeState` (신규)로 분리 (plan-02/06 본문)
- `PdfTextExtractor` **기존 클래스 확장** (`String extract(MultipartFile)`, MAX_TEXT_LENGTH=5000) — plan-05 "신규 생성" 기재 교정 필요
- plan-07 `resume-chain-interrogator.txt`: `fact_check_flag` / `fact_check_note` 필드 삭제 + "Out of scope" 주석
- plan-08 rubric 9개 YAML은 `backend/src/main/resources/rubric/` **신설 디렉토리**
- `ReferenceType` enum 실제 값은 `MODEL_ANSWER / GUIDE` (TODO 03의 `CONCEPT / EXPERIENCE`와 다름 — plan-08 매핑 주의)

### 미해결 / 이월
- **JaCoCo 미설정**: `backend/build.gradle.kts`에 `jacoco` 플러그인 없음. 커버리지 baseline 미측정. TEST_BASELINE.md에 템플릿 제시함 — 별도 PR로 추가 검토 (S1 범위 밖)
- plan-01/02/05/06/07/08 본문 내 "생성/수정 파일" 표를 IMPACT_MAP 기준으로 일괄 교정하는 작업은 각 plan 실행 직전(S4/S5/S7/S8/S9a)에 executor가 해당 plan PR에 포함

### 관측 스냅샷
- Gradle test log: `/tmp/gradle-test-S1.log` (로컬)
- Test report: `backend/build/reports/tests/test/index.html`
- Counter: 606 tests, 0 failures, 0 ignored

### PR
- 제목: `[BE] docs: interview-quality S1 — 코드베이스 인벤토리 + 테스트 베이스라인 + 영향도 맵`
- Base: `develop`
- 변경: md 3종 신규 + progress.md / plan-00a 헤더 edit + HANDOFF.md 신규 = 6 파일

---

## Session S2 (2026-04-20) — plan-00b AiClient Generalization

### 완료
- **신규 생성 파일 5종**:
  - `backend/src/main/java/com/rehearse/api/infra/ai/dto/ChatMessage.java`
  - `backend/src/main/java/com/rehearse/api/infra/ai/dto/CachePolicy.java`
  - `backend/src/main/java/com/rehearse/api/infra/ai/dto/ResponseFormat.java`
  - `backend/src/main/java/com/rehearse/api/infra/ai/dto/ChatRequest.java`
  - `backend/src/main/java/com/rehearse/api/infra/ai/dto/ChatResponse.java`
  - `backend/src/main/java/com/rehearse/api/config/AiFeatureProperties.java`
- **수정 파일 7종**:
  - `backend/src/main/java/com/rehearse/api/infra/ai/AiClient.java` — `chat(ChatRequest)` 추가
  - `backend/src/main/java/com/rehearse/api/infra/ai/OpenAiClient.java` — `chat()` 구현, modelOverride, response_format 지원
  - `backend/src/main/java/com/rehearse/api/infra/ai/ClaudeApiClient.java` — `chat()` 구현, SYSTEM→system 배열 분리, cacheControl ephemeral, executeClaudeRequest() 공용 메서드 추출
  - `backend/src/main/java/com/rehearse/api/infra/ai/ResilientAiClient.java` — `chat()` + `fallbackChat()` (allowMiss=true)
  - `backend/src/main/java/com/rehearse/api/infra/ai/MockAiClient.java` — `chat()` stub
  - `backend/src/main/java/com/rehearse/api/infra/ai/AiResponseParser.java` — `parseWithRetry()` 추가
  - `backend/src/main/resources/application.yml` — `/actuator/refresh` 노출 + `rehearse.features.*` 기본값 false
  - `backend/build.gradle.kts` — `spring-cloud-context:4.1.4` 추가
- **신규 테스트 3종**:
  - `backend/src/test/java/com/rehearse/api/infra/ai/AiClientChatTest.java` (13개 테스트)
  - `backend/src/test/java/com/rehearse/api/infra/ai/ResilientAiClientFallbackTest.java` (7개 테스트)
  - `backend/src/test/java/com/rehearse/api/infra/ai/AiResponseParserRetryTest.java` (6개 테스트)
- **테스트 결과**: 634 tests / 0 failures / 0 ignored (baseline 606 → +28)
- **REMEDIATION 체크**: C1 ✅ / C3 ✅ / M5 ✅ / Missing JSON 파싱 폴백 ✅ / Missing Feature flag runtime ✅

### 핵심 설계 결정
- `AiClientChatTest` 는 `MockAiClient` 단위 테스트로 구현 (test profile 에 `claude.api-key: test-key` 설정 → `ResilientAiClient` 활성화로 `@SpringBootTest` 불가)
- `ClaudeApiClient.chat()` 는 기존 재시도 로직을 `executeClaudeRequest(ClaudeRequest, String, int)` 공용 메서드로 추출해 공유
- `OpenAiClient.chat()` 는 응답의 Usage 를 `ChatResponse.Usage.empty()` 로 반환 (OpenAI Prompt Caching 은 자동이므로 캐시 토큰 별도 추적 불필요)
- `ResilientAiClient` 의 기존 3개 도메인 메서드는 그대로 유지 — 새 `chat()` 만 추가하여 회귀 0

### 미해결 / 이월
- **OpenAI chat() Usage 상세화**: 현재 `Usage.empty()` 반환. 필요 시 `OpenAiResponse.Usage` 파싱해 채울 수 있음 — plan-00d Observability 에서 다룰 것
- **Micrometer 태그**: plan spec 에 `Timer.builder("rehearse.ai.call.duration")` 명시됐으나 plan-00d 범위로 이월 (plan-00b 에서 구현 시 테스트 환경 Prometheus 의존 추가 필요)

### 관측 스냅샷
- Test report: `backend/build/reports/tests/test/index.html`
- Counter: 634 tests, 0 failures, 0 ignored

### S2 Post-Completed — 리뷰 반영 리팩터 (2026-04-20, eager-zebra)

S2 머지 전 리뷰 피드백(SRP 분리 + 계측 분리 + 테스트 hermetic) 반영으로 구조 재정리. 중단 후 복구 시점 27 fail → **0 fail / 643 tests (+9 from 634)**.

#### 구조 변경
- `AbstractAiClient` 신설 — legacy 3-메서드(`generateQuestions/generateFollowUpQuestion/generateFollowUpWithAudio`) 공통 위임 로직을 상위 클래스로 이동. 구현체는 `chat()` 만 구현.
- `infra/ai/adapter/QuestionGenerationAdapter`, `FollowUpGenerationAdapter` 신설 — 프롬프트 빌드 + `ChatRequest` 변환 + `parseOrRetry()` 를 어댑터 컴포넌트로 분리 (SRP).
- `infra/ai/metrics/AiCallMetrics` 추출 — `ResilientAiClient` 안에 있던 Micrometer `Timer` 계측을 전담 컴포넌트로 분리(plan-00d 범위 이월 해제 일부).
- `AiResponseParser.parseOrRetry(ChatResponse, Class, AiClient, ChatRequest)` 고수준 오버로드 추가 — 어댑터에서 스키마 힌트 재호출을 위임.
- `OpenAiResponse.PromptTokensDetails` 추가 — `cached_tokens` 매핑으로 `ChatResponse.Usage` 채움.
- `OpenAiClient`: `@Value("${openai.base-url:...}")` 훅 추가 — 테스트가 WireMock URL 을 property override 로 주입 가능.
- `application.yml`: `management.endpoints.web.exposure.include` 에 `prometheus` 추가.

#### 신규 테스트
- `OpenAiChatModelOverrideTest` (3) — WireMock 기반 modelOverride POST body 검증. `http2PlainDisabled(true)` 로 JDK HttpClient HTTP/2 RST_STREAM 회피.
- `ActuatorRefreshIntegrationTest` (4) — `@SpringBootTest` + `@ActiveProfiles("test")` 로 `AiFeatureProperties` @RefreshScope 빈/`ContextRefresher.refresh()` 검증.
- `adapter/QuestionGenerationAdapterTest` (3), `adapter/FollowUpGenerationAdapterTest` — 어댑터 단위 계약 검증.
- `AiCallMetricsTest` — 계측 타이머 태그 검증.

#### 복구한 27 fail 버킷 (approach)
1. `ResilientAiClientTest` legacy Nested 3클래스(`GenerateQuestions/GenerateFollowUp/GenerateFollowUpWithAudio`) 제거 → Initialization + LegacyDelegation 스모크 1nested(3 test) 로 치환. 상세 fallback 분기는 `ResilientAiClientFallbackTest` 가 전담.
2. `ResilientAiClientFallbackTest`: `new RuntimeException(...)` → `new RetryableApiException(...)` 로 교체. 설계 원칙(plan-00b M4: 네트워크/API 오류만 fallback, 프로그래밍 오류 rethrow) 유지.
3. `OpenAiChatModelOverrideTest`: `OpenAiClient` baseUrl 주입 훅 추가 + WireMock `http2PlainDisabled(true)`.
4. `ActuatorRefreshIntegrationTest`: `@ActiveProfiles("test")` 추가 — 기본 profile 의 `data-local.sql` 시드가 enum `InterviewType` 와 충돌(`'CS'` ∉ enum). test profile 은 create-drop + sql-init 비활성.
5. `QuestionGenerationAdapterTest`: Mockito strict mode 의 duplicate `parseOrRetry` stub 제거.

#### 테스트 카운트
- Final: **643 tests / 0 failures / 0 ignored** (baseline 606 → +37)

### 다음 세션 (S3) Kickoff
```
interview-quality 실행 계획 S3 재개 — plan-00c Session State Persistence
```
- 시작점: `docs/plans/interview-quality-2026-04-20/plan-00c-session-state-persistence.md`
- 범위: Flyway V24~V27 + InterviewRuntimeState + InterviewLockService + Caffeine
- Gate: 신규 마이그레이션 적용, ResilientAiClient / FollowUpService 기존 테스트 그린

---

### 다음 세션 (S2) Kickoff
```
interview-quality 실행 계획 S2 재개 — plan-00b AiClient Generalization
```
- 시작점: `docs/plans/interview-quality-2026-04-20/plan-00b-aiclient-generalization.md`
- 범위: PR #1 `[BE] feat(ai): AiClient.chat() 범용 메서드 + @RefreshScope + JSON 파싱 재시도`
- 선행 확인: `IMPACT_MAP.md` §plan-00b 섹션 + `INVENTORY.md` §1 AI Infrastructure
- Gate: modelOverride · fallback cache-miss · `/actuator/refresh` 3종 통합 테스트 그린, 기존 3개 도메인 메서드 어댑터 경유 시 회귀 0
