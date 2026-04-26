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

---

## Session S6 (2026-04-26) — plan-02 + plan-03 코드 구현 (단일 PR 준비)

### 완료
- **plan-02 (Answer Analyzer M1 Step A)**: 신규 8종 (`AnswerAnalysis`/`Claim`/`EvidenceStrength`/`Perspective`/`RecommendedNextAction`/`AnswerAnalyzer`/`AnswerAnalyzerPromptBuilder`/프롬프트 템플릿). `InterviewRuntimeState.recordAnalysis/getAnswerAnalysis` 접근자 추가. L1 FN 가드 (`claims=[] AND answer_quality<=1 ⇒ CLARIFICATION`).
- **plan-03 (Follow-up Generator v3 M1 Step B)**: `GeneratedFollowUp.targetClaimIdx/selectedPerspective` + `FollowUpExchange.selectedPerspective` (FE echo) + `FollowUpResponse.selectedPerspective` echo. `FollowUpService` ANSWER 경로 refactor (STT → AnswerAnalyzer → SKIP cost saver → Step B `chat(ChatRequest)`). 프롬프트 v3 (CONCEPT/EXPERIENCE) ANSWER_ANALYSIS 입력.
- **테스트**: 749 tests pass / 0 failures (baseline 719 + 신규 ~30)
- **단일 PR 결정**: plan-02 단독 머지 시 Step A 출력 소비처 부재 → 데드코드. 단일 BE PR 로 합류.

### 관측 스냅샷
- 브랜치: `feat/answer-analyzer-followup-v3` (squash merge 됨)
- Counter: 749 tests, 0 failures, 0 ignored

### 다음 세션 (S7) Kickoff
```
interview-quality 실행 계획 S7 — PR #353 머지 + 문서 정합화
```
- 시작점: PR `[BE] feat: 답변 분석 단계 분리 + 꼬리질문 작문 정확도 개선`
- 범위: 머지 + plan-02/03/progress/HANDOFF 문서 정합화
- Gate: develop 머지 / 문서 4종 갱신 / plan-04 의존성 해소 확인

---

## Session S7 (2026-04-26) — PR #353 머지 + 문서 정합화

### 완료
- **PR #353 머지**: `state=MERGED`, `mergedAt=2026-04-25T18:16:33Z`, `mergeCommit=be68b0f`, base=`develop`
- **문서 정합화**:
  - `plan-02-answer-analyzer.md` 헤더 `Draft → Completed (#353)` + `## 머지 결과` 섹션 추가
  - `plan-03-followup-generator-v3.md` 동일 처리
  - `progress.md` Phase 1~4 표 02/03 행 `Implemented → Completed (#353)`
  - `HANDOFF.md` 본 S6/S7/S8 섹션 추가 (S5 이후 부재)
  - `plan-04-context-engineering.md` 헤더 `Draft → In Progress` + spec 보강 섹션 추가
- **plan-02 line 1 typo 수정**: `gogo# Plan 02:` → `# Plan 02:`

### 잔여 게이트 (plan-04 와 무관, 독립 트랙 병렬 진행)
- [ ] FE 계약 전달 — `selectedPerspective` echo + `presentToUser` (FE PR)
- [ ] LIVE 골든셋 실행 (`LIVE_TEST=true`)
- [ ] MANUAL_AB 3~5건 (v2 vs v3)
- [ ] 스테이징 Prometheus `rehearse.ai.call.duration_seconds{call.type=~"answer_analyzer|follow_up_generator_v3"}` p95

### plan-04 진입 조건 (충족 ✓)
- 의존성 (00b/00c/00d/01/02/03) 모두 해소
- 인프라 80% 준비 완료 (`ChatMessage.cacheControl`, `ClaudeApiClient.SystemContent.withCaching`, `OpenAiClient.cached_tokens` 파싱, `ResilientAiClient.withAllowMiss` fallback)
- `infra/ai/context/` 디렉토리만 미존재

### 다음 세션 (S8) Kickoff
```
interview-quality 실행 계획 S8 — plan-04 Context Engineering 4-Layer Builder
```
- 시작점: `docs/plans/interview-quality-2026-04-20/plan-04-context-engineering.md`
- 범위: `infra/ai/context/**` 16 클래스 + 테스트 7 + `eval/context/measure_tokens.py`. 5종 caller (`AnswerAnalyzer`/`IntentClassifier`/`FollowUpService`/`ClarifyResponseHandler`/`GiveUpResponseHandler`) 가 `InterviewContextBuilder.build()` 경유로 전환
- Gate:
  - 10턴 세션 평균 입력 토큰 ≤ 8,000 (`measure_tokens.py`)
  - L1 캐시 히트율 ≥ 95% (Claude `cache_read_input_tokens` 메타)
  - 749 → ~775 tests pass / 0 failures
  - 회귀 0 (plan-01/02/03 기존 테스트 그린 유지)
- 브랜치: `feat/plan-04-context-engineering` (origin/develop = `6ac9c5a` 베이스)
- 단일 BE PR — 제목 `[BE] feat(plan-04): Context Engineering 4-layer Builder + Prompt Caching 표준화`

---

## Session S8 (2026-04-26) — plan-04 코드 구현 + PR #354 머지 대기

### 완료
- **신규 코드 (Tasks 1~7)**:
  - L1 `FixedContextLayer` (callType 5종 skeleton + GLOBAL_CORE 보안 규칙)
  - L2 `SessionStateLayer` + `SessionStateSnapshot` record + `InterviewRuntimeState.toSessionStateSnapshot()`
  - L3 `DialogueHistoryLayer` (5턴 슬라이딩 윈도우) + `DialogueCompactor` `@Async("compactionExecutor")` + `CompactionExecutorConfig` (core 2/max 4/queue 50/CallerRunsPolicy/30s shutdown)
  - L4 `FocusLayer` (callType 6종 dispatch with token cap)
  - `InterviewContextBuilder` 진입점 + `ContextBuildRequest`/`BuiltContext` records + `ContextEngineeringProperties` (`@Validated` cross-field check)
  - `ContextEngineeringMetrics` (tokens Histogram + cache_hit_ratio Gauge + compaction_count Counter)
  - `compaction-summarizer.txt` (5-key JSON: covered_topics/user_claims_made/chain_progress_history/perspectives_asked/notable_moments + 보안 규칙 + 2 few-shot)
  - 5 caller refactor (`AnswerAnalyzer:54`/`IntentClassifier:34`/`FollowUpService:147`/`ClarifyResponseHandler:47`/`GiveUpResponseHandler:46`)
- **리뷰 반영** (architect + code 병렬):
  - DialogueCompactor TOCTOU race fix (`tryStartCompaction(windowEnd)` atomic)
  - vestigial `PromptCacheStrategy`/`OpenAi`/`Claude` 어댑터 3종 + 테스트 2종 삭제
  - `CompactionExecutor` graceful shutdown
- **테스트**: 838 / 0 failures / 1 ignored
- **토큰 측정**: avg=609, max=687, min=441 (5 fixture × 10턴, PASS ≤8000)
- **PR #354**: CI 그린, 머지 대기 (사용자 승인 후 squash merge)

### 검증 게이트 현실화
plan-04 spec `## 검증` 표 갱신 — Prometheus 서버 부재 (OBSERVABILITY.md `Out of Scope`)로 §2(24h 캐시 히트율) 게이트 별도 SRE 인프라 PR 후 재실행.

### 잔여 게이트 (다음 세션 S9에서)
- A1. 로컬/EC2 `/actuator/prometheus` 단발 캡처 → `rehearse_ai_context_cache_hit_ratio` > 0
- A2. MANUAL_AB 3~5건 (OpenAI vs Claude 동일 세션, `MANUAL_AB_PROTOCOL.md`)
- A3. Compaction 정성 5세션 (6턴 이상 진행, covered_topics 누락 0건)
- A4. progress.md 04 → Completed + HANDOFF S9 완료 entry

### 이월 (별도 spec, plan-05 진입 전 우선순위 결정 필요)
- **B2 (High)** L2 `asked_perspectives` derive — Resume Track 입력 품질 직접 영향
- B1 L3 동기 compaction fallback
- B3 `max-context-tokens: 8000` 초과 시 강제 compaction
- B4 TokenEstimator 한국어 정확도 (jtokkit)
- B5 `focusHints` sealed-interface
- B6 orphan 4 prompt builders 삭제
- C1/C2 Prometheus + Grafana + Alertmanager (SRE 별건)
- D1~D4 plan-03 잔여 (FE 계약 / LIVE 골든셋 / MANUAL_AB v2/v3 / 스테이징 p95)

### 다음 세션 (S9) Kickoff
```
interview-quality 실행 계획 S9 — plan-04 Gate 통과 + plan-05 (Resume Extractor) 착수 결정
```
- **사전 (S8 종료 시점 상태)**:
  - PR #354 머지 완료 (mergeCommit `ee67201`, 2026-04-26 02:58 UTC)
  - deploy-dev.yml run `24946779778` IN PROGRESS — 세션 시작 시 `gh run view 24946779778 --json status,conclusion` 으로 결과 확인
  - 직전 deploy (#353) FAIL 원인: SSH timeout (인프라 transient). 본 deploy 도 실패 시 `gh run rerun 24946779778` 또는 새 commit push 로 재트리거
- Step 1: A1 단발 캡처 — 로컬 `./gradlew bootRun` + 면접 1세션 10턴 직접 진행 → `curl localhost:8080/actuator/prometheus | grep rehearse_ai_context`
- Step 2: A2 MANUAL_AB 3~5건 — `eval/manual-ab/2026-04-2X-plan-04.md` 작성
- Step 3: A3 Compaction 정성 5세션 — 6턴+ 시나리오 직접 트리거, 결과 캡처
- Step 4: progress.md 04 `Completed (#354)` + HANDOFF S9 완료 entry
- Step 5: B2 (asked_perspectives derive) 별도 spec 작성 후 plan-05 진입 결정
- 참조: `plan-04-context-engineering.md` `## 이월 사항`

