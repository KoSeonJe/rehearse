# Impact Map — Interview Quality 2026-04-20

> 실측일: 2026-04-20
> Source: plan-00a (plan-00b ~ plan-11 각 spec 직접 판독)
> Status: Completed
> 경로 기준: 절대 경로 (`/Users/koseonje/dev/devlens/...`)

---

## plan-00b AiClient Generalization

### 신규 생성
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/infra/ai/dto/ChatRequest.java` — 범용 채팅 요청 record (messages/modelOverride/temperature/maxTokens/cachePolicy/responseFormat/callType)
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/infra/ai/dto/ChatResponse.java` — 범용 채팅 응답 record (content/usage/cacheRead/provider/fallbackUsed)
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/infra/ai/dto/ChatMessage.java` — 메시지 단위 (role/content/cacheControl)
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/infra/ai/dto/CachePolicy.java` — 캐시 정책 (providerCache/allowMiss)
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/config/AiFeatureProperties.java` — @ConfigurationProperties("rehearse.features") + @RefreshScope
- `/Users/koseonje/dev/devlens/backend/src/test/java/com/rehearse/api/infra/ai/AiClientChatTest.java` — 범용 chat 계약 테스트
- `/Users/koseonje/dev/devlens/backend/src/test/java/com/rehearse/api/infra/ai/ResilientAiClientFallbackTest.java` — Fallback 캐시 정책 degrade 검증
- `/Users/koseonje/dev/devlens/backend/src/test/java/com/rehearse/api/infra/ai/AiResponseParserRetryTest.java` — JSON 파싱 실패 재호출 검증

### 수정
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/infra/ai/AiClient.java` — 범용 `chat(ChatRequest): ChatResponse` 메서드 추가; 기존 3개 메서드는 default method로 위임
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/infra/ai/OpenAiClient.java` — `chat()` 구현, modelOverride 지원, 메시지 순서 보장
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/infra/ai/ClaudeApiClient.java` — `chat()` 구현, cache_control: ephemeral 마킹 지원
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/infra/ai/ResilientAiClient.java` — fallback 경로에 CachePolicy.allowMiss=true 자동 설정
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/infra/ai/MockAiClient.java` — `chat()` stub 추가
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/infra/ai/AiResponseParser.java` — `parseWithRetry()` 추가 (스키마 실패 시 1회 재호출)
- `/Users/koseonje/dev/devlens/backend/src/main/resources/application.yml` — management.endpoints.web.exposure.include에 refresh 추가
- `/Users/koseonje/dev/devlens/backend/build.gradle.kts` — spring-cloud-starter 최소 의존성 추가

### ⚠️ 교정 사항
- 없음 (plan-00b 본문 클래스/경로 정확)

---

## plan-00c Session State Persistence Design

### 신규 생성
- `/Users/koseonje/dev/devlens/docs/plans/interview-quality-2026-04-20/STATE_DESIGN.md` — 4계층 상태 분류 + 저장소 결정 + 동시성 정책 문서
- `/Users/koseonje/dev/devlens/backend/src/main/resources/db/migration/V24__create_resume_skeleton.sql` — resume_skeleton 테이블
- `/Users/koseonje/dev/devlens/backend/src/main/resources/db/migration/V25__create_interview_plan.sql` — interview_plan 테이블
- `/Users/koseonje/dev/devlens/backend/src/main/resources/db/migration/V26__create_rubric_score.sql` — rubric_score 테이블 (rubric_id/scored_dimensions/level_flag 컬럼 포함)
- `/Users/koseonje/dev/devlens/backend/src/main/resources/db/migration/V27__create_session_feedback.sql` — session_feedback 테이블
- `/Users/koseonje/dev/devlens/backend/src/main/resources/db/migration/rollback/V24__rollback.sql` — 롤백 스크립트
- `/Users/koseonje/dev/devlens/backend/src/main/resources/db/migration/rollback/V25__rollback.sql` — 롤백 스크립트
- `/Users/koseonje/dev/devlens/backend/src/main/resources/db/migration/rollback/V26__rollback.sql` — 롤백 스크립트
- `/Users/koseonje/dev/devlens/backend/src/main/resources/db/migration/rollback/V27__rollback.sql` — 롤백 스크립트
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/interview/runtime/InterviewRuntimeState.java` — 턴 루프 워킹 메모리 POJO (coveredClaims/activeChain/turnAnalysisCache)
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/interview/runtime/InterviewRuntimeStateStore.java` — Caffeine 기반 in-memory store (interviewId → RuntimeState, 세션 상한 기반 TTL)

### 수정
- `/Users/koseonje/dev/devlens/backend/build.gradle.kts` — Caffeine 의존성 추가 확인 (없으면 추가)

### ⚠️ 교정 사항
- plan-00c 본문 "InterviewSession 엔티티 신설 안 함" 명시 — `Interview` aggregate가 L1/L2 담당, 런타임은 `InterviewRuntimeState` POJO로 분리. 다른 plan 문서에서 `InterviewSession` 참조 시 `InterviewRuntimeState` / `Interview`로 교정 필요.

---

## plan-00d Observability + Eval Smoke

### 신규 생성
- `/Users/koseonje/dev/devlens/eval/golden-sets/smoke/gs_s01_happy_path.yaml` — smoke 케이스 1
- `/Users/koseonje/dev/devlens/eval/golden-sets/smoke/gs_s02_happy_path.yaml` — smoke 케이스 2
- `/Users/koseonje/dev/devlens/eval/golden-sets/smoke/gs_s03_happy_path.yaml` — smoke 케이스 3
- `/Users/koseonje/dev/devlens/eval/golden-sets/smoke/gs_s04_clarify.yaml` — smoke 케이스 4 (clarify)
- `/Users/koseonje/dev/devlens/eval/golden-sets/smoke/gs_s05_clarify.yaml` — smoke 케이스 5 (give_up)
- `/Users/koseonje/dev/devlens/eval/judges/j1-followup-relevance.txt` — J1 Judge 초안 (plan-10에서 완성)
- `/Users/koseonje/dev/devlens/eval/scripts/smoke.py` — 골든셋 5개 × J1 실행, 실패 시 exit 1
- `/Users/koseonje/dev/devlens/eval/README.md` — eval 사용법
- `/Users/koseonje/dev/devlens/eval/requirements.txt` — openai, anthropic, pyyaml
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/infra/ai/metrics/AiCallMetrics.java` — Micrometer Timer + Counter 래퍼
- `/Users/koseonje/dev/devlens/docs/plans/interview-quality-2026-04-20/OBSERVABILITY.md` — Grafana 쿼리 예시 + Alert 임계치

### 수정
- `/Users/koseonje/dev/devlens/backend/src/main/resources/application.yml` — management.metrics.export.prometheus.enabled: true 확인 및 활성화

### ⚠️ 교정 사항
- `.github/workflows/eval-smoke.yml` — plan 본문에서 "선택, 미구현도 가능"으로 명시, **Out of Scope**. IMPACT_MAP에서 제외.

---

## plan-00e Feedback Migration Strategy

### 신규 생성
- `/Users/koseonje/dev/devlens/docs/plans/interview-quality-2026-04-20/FEEDBACK_DOMAIN.md` — 5개 결정 문서 (병존/비동기/Admin API/기존 불변/서브패키지 경로)

### 수정
- 없음 (이 plan은 코드 변경 없음 — 문서 결정만)

### ⚠️ 교정 사항
- plan-00e 본문 검증 항목 4번: `InterviewCompletedEvent` 존재 여부 → plan-09 spec 확인 결과, plan-09가 "존재 확인 후 없으면 신규" 생성 책임. plan-00e 범위는 결정 문서만.

---

## plan-01 Intent Classifier

### 신규 생성
- `/Users/koseonje/dev/devlens/backend/src/main/resources/prompts/template/intent-classifier.txt` — 3-intent 분류 프롬프트 (ANSWER/CLARIFY_REQUEST/GIVE_UP)
- `/Users/koseonje/dev/devlens/backend/src/main/resources/prompts/template/clarify-response.txt` — CLARIFY 분기용 재설명 프롬프트
- `/Users/koseonje/dev/devlens/backend/src/main/resources/prompts/template/giveup-response.txt` — GIVE_UP 분기용 프롬프트
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/infra/ai/prompt/IntentClassifierPromptBuilder.java` — 분류기 프롬프트 빌더
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/infra/ai/prompt/ClarifyResponsePromptBuilder.java` — CLARIFY 응답 빌더
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/infra/ai/prompt/GiveUpResponsePromptBuilder.java` — GIVE_UP 응답 빌더
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/interview/vo/IntentType.java` — enum (ANSWER/CLARIFY_REQUEST/GIVE_UP)
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/interview/service/IntentClassifier.java` — 의도 분류 서비스 (AiClient.chat() 사용)

### 수정
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpService.java` — `generateFollowUp()` (line 31) 진입부에 `intentClassifier.classify()` 분기 삽입
- `/Users/koseonje/dev/devlens/backend/src/main/resources/application.yml` — rehearse.features.intent-classifier.* flag 추가

### ⚠️ 교정 사항
- plan-01 본문: "InterviewTurnService" 언급 없음 (올바르게 FollowUpService 사용). 다른 plan 참조 시 `InterviewTurnService` → `FollowUpService.generateFollowUp() at line 31` 교정.

---

## plan-02 Answer Analyzer

### 신규 생성
- `/Users/koseonje/dev/devlens/backend/src/main/resources/prompts/template/follow-up-step-a-analyzer.txt` — 답변 분석 전용 프롬프트
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/infra/ai/prompt/AnswerAnalyzerPromptBuilder.java` — 분석 프롬프트 빌더
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/interview/AnswerAnalysis.java` — record (claims/missing_perspectives/unstated_assumptions/answer_quality/recommended_next_action)
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/interview/Claim.java` — record (text/depth_score/evidence_strength/topic_tag)
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/interview/service/AnswerAnalyzer.java` — 분석 서비스 (callType="answer_analyzer")

### 수정
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/interview/runtime/InterviewRuntimeState.java` — (plan-00c 신규 생성 후) 분석 결과 캐시 필드 추가
- `/Users/koseonje/dev/devlens/backend/src/main/resources/application.yml` — rehearse.features.follow-up-pipeline.step-a-enabled flag 추가

### ⚠️ 교정 사항
- plan-02 본문: "InterviewSession 클래스는 실재하지 않음" 주석 올바름. `InterviewSession` → `InterviewRuntimeStateStore` (plan-00c 산출)로 확정.
- `AnswerAnalysis`, `Claim`의 패키지: plan 본문에서 `domain/interview/` 직하로 기술. 실제 구현 시 `domain/interview/vo/` 또는 별도 패키지 검토 필요.

---

## plan-03 Follow-up Generator v3

### 신규 생성
- 없음 (기존 파일 수정만)

### 수정
- `/Users/koseonje/dev/devlens/backend/src/main/resources/prompts/template/follow-up-concept.txt` — v2 → v3 (Step B 구조, Step A 분석 입력 수용)
- `/Users/koseonje/dev/devlens/backend/src/main/resources/prompts/template/follow-up-experience.txt` — v2 → v3 (동일)
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/infra/ai/prompt/FollowUpPromptBuilder.java` — Step A 결과를 ANSWER_ANALYSIS 섹션으로 주입
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpService.java` — Step A 호출 → 결과 주입 → Step B 호출 순으로 재구성
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/interview/FollowUpQuestion.java` (경로 확인 필요) — targetClaimIdx/selectedPerspective/reason 필드 추가

### ⚠️ 교정 사항
- plan-03 본문: `domain/interview/FollowUpService.java` 및 `domain/interview/FollowUpQuestion.java` 로 표기. 실제 서비스는 `domain/interview/service/FollowUpService.java`. `FollowUpQuestion`은 `domain/interview/` 직하 또는 `dto/`에 위치할 것으로 추정 — 구현 시 실제 경로 확인 필요.

---

## plan-04 Context Engineering 4-layer Builder

### 신규 생성
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/infra/ai/context/InterviewContextBuilder.java` — 4-layer 조립 진입점
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/infra/ai/context/layer/FixedContextLayer.java` — L1: System + Skeleton, 캐시 마킹
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/infra/ai/context/layer/SessionStateLayer.java` — L2: 200-500 tokens JSON 상태
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/infra/ai/context/layer/DialogueHistoryLayer.java` — L3: 슬라이딩 윈도우 5턴 + compaction
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/infra/ai/context/layer/FocusLayer.java` — L4: JIT 렌더링
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/infra/ai/context/compaction/DialogueCompactor.java` — 대화 압축기
- `/Users/koseonje/dev/devlens/backend/src/main/resources/prompts/template/compaction-summarizer.txt` — 압축 요약 프롬프트
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/infra/ai/context/cache/OpenAiCacheAdapter.java` — 자동 캐싱(고정 블록 순서 보장)
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/infra/ai/context/cache/ClaudeCacheAdapter.java` — cache_control: ephemeral 마킹

### 수정
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/infra/ai/ResilientAiClient.java` — 요청 조립 시 ContextBuilder 결과 사용
- `/Users/koseonje/dev/devlens/backend/src/main/resources/application.yml` — rehearse.features.context-engineering.* flag 추가

### ⚠️ 교정 사항
- 없음

---

## plan-05 Resume Extractor

### 신규 생성
- `/Users/koseonje/dev/devlens/backend/src/main/resources/prompts/template/resume/resume-extractor.txt` — Phase 1 추출 프롬프트
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/infra/ai/prompt/ResumeExtractorPromptBuilder.java` — 추출 프롬프트 빌더
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/resume/ResumeIngestionService.java` — PDF 텍스트 정규화 + 언어 감지 + 섹션 분리
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/resume/ResumeExtractionService.java` — 텍스트 → Skeleton 변환
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/resume/domain/ResumeSkeleton.java` — Skeleton record
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/resume/domain/Project.java` — 프로젝트 도메인
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/resume/domain/ResumeClaim.java` — resume 전용 claim (plan-02 Claim과 분리)
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/resume/domain/InterrogationChain.java` — L1 WHAT→L2 HOW→L3 WHY_MECH→L4 TRADEOFF 4단계 체인
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/resume/ResumeSkeletonCache.java` — 세션 스코프 캐시 (2h TTL)

### 수정
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/infra/ai/PdfTextExtractor.java` — 정규화 파이프라인 추가 (RemoveControlChars/CollapseWhitespace/FixKoreanTokenBreaks/RemoveHeaderFooter/ExtractByColumn)

### ⚠️ 교정 사항
- plan-05 본문: `PdfTextExtractor.java` → **기존 클래스 수정** (신규 생성 아님). 경로: `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/infra/ai/PdfTextExtractor.java`

---

## plan-06 Resume Interview Planner

### 신규 생성
- `/Users/koseonje/dev/devlens/backend/src/main/resources/prompts/template/resume/resume-interview-planner.txt` — 플래너 프롬프트
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/infra/ai/prompt/ResumeInterviewPlannerPromptBuilder.java` — 플래너 프롬프트 빌더
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/resume/ResumeInterviewPlanner.java` — 세션 플래너 서비스
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/resume/domain/InterviewPlan.java` — Plan 도메인 (session_plan_id/project_plans/estimated_duration_min)
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/resume/domain/ProjectPlan.java` — playground_phase + interrogation_phase

### 수정
- 없음

### ⚠️ 교정 사항
- plan-06 본문: `domain/interview/InterviewSession.java` 수정 항목으로 `InterviewPlan` 참조 추가 기술 → `InterviewSession` 미존재. 실제 구현 시 `InterviewRuntimeState` (plan-00c) 또는 별도 캐시에 `InterviewPlan` 참조 보관으로 대체 필요.

---

## plan-07 Resume Orchestrator

### 신규 생성
- `/Users/koseonje/dev/devlens/backend/src/main/resources/prompts/template/resume/resume-playground-opener.txt` — 놀이터 오프너 프롬프트
- `/Users/koseonje/dev/devlens/backend/src/main/resources/prompts/template/resume/resume-playground-responder.txt` — 놀이터 응답 + 전환 판단 프롬프트
- `/Users/koseonje/dev/devlens/backend/src/main/resources/prompts/template/resume/resume-chain-interrogator.txt` — 심문 Step B 변형 (LEVEL_UP/STAY/CHAIN_SWITCH, fact_check_flag 없음)
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/infra/ai/prompt/ResumePlaygroundPromptBuilder.java` — 놀이터 프롬프트 빌더
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/infra/ai/prompt/ResumeChainInterrogatorPromptBuilder.java` — 심문 프롬프트 빌더
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/resume/ResumeInterviewOrchestrator.java` — 이력서 면접 메인 진입점
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/resume/PlaygroundModeHandler.java` — 놀이터 모드 처리
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/resume/InterrogationModeHandler.java` — 심문 모드 처리
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/resume/ChainStateTracker.java` — 세션 내 체인 진행 상태
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/resume/domain/ResumeMode.java` — enum (PLAYGROUND/INTERROGATION)

### 수정
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpService.java` — Interview에 resumeSkeletonId 있으면 `ResumeInterviewOrchestrator`로 위임 분기 추가

### ⚠️ 교정 사항
- plan-07 본문: 진입점을 "InterviewTurnService" 로 표기하는 참조 → 실제는 `FollowUpService.generateFollowUp() at line 31`. plan-07 스펙에는 `FollowUpService` 올바르게 표기됨.
- plan-07 본문: `resume-chain-interrogator.txt` JSON 스키마에서 `fact_check_flag`, `fact_check_note` **삭제** 확정. 프롬프트에 "이력서-답변 사실 불일치 감지/기록 안 함" 명시.
- plan-07 검증 항목 5번: `"InterviewTurnServiceTest"` → 실재하지 않음. 실제는 `FollowUpServiceTest`. 검증 커맨드: `./gradlew test --tests "FollowUpServiceTest"`.

---

## plan-08 Rubric Family Scorer

### 신규 생성 (YAML 리소스 9개)
- `/Users/koseonje/dev/devlens/backend/src/main/resources/rubric/_dimensions.yaml` — 10차원 마스터 (D1~D10)
- `/Users/koseonje/dev/devlens/backend/src/main/resources/rubric/_mapping.yaml` — 선언적 rubric_id 매핑 규칙
- `/Users/koseonje/dev/devlens/backend/src/main/resources/rubric/concept-cs-fundamental-rubric.yaml` — CS_FUNDAMENTAL (4차원)
- `/Users/koseonje/dev/devlens/backend/src/main/resources/rubric/lang-fw-backend-rubric.yaml` — LANGUAGE_FRAMEWORK+BACKEND (5차원)
- `/Users/koseonje/dev/devlens/backend/src/main/resources/rubric/lang-fw-frontend-rubric.yaml` — LANGUAGE_FRAMEWORK/UI_FRAMEWORK+FRONTEND (5차원)
- `/Users/koseonje/dev/devlens/backend/src/main/resources/rubric/experience-backend-rubric.yaml` — EXPERIENCE+backend (5차원)
- `/Users/koseonje/dev/devlens/backend/src/main/resources/rubric/experience-collaboration-rubric.yaml` — BEHAVIORAL (4차원)
- `/Users/koseonje/dev/devlens/backend/src/main/resources/rubric/resume-backend-rubric.yaml` — RESUME_BASED/ResumeTrack (5차원, mode-aware)
- `/Users/koseonje/dev/devlens/backend/src/main/resources/rubric/fallback-generic-rubric.yaml` — 매핑 실패 fallback (3차원)

### 신규 생성 (프롬프트)
- `/Users/koseonje/dev/devlens/backend/src/main/resources/prompts/template/turn-rubric-scorer.txt` — DIMENSIONS_TO_SCORE 파라미터화 채점 프롬프트

### 신규 생성 (Java — `domain/feedback/rubric/`)
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/feedback/rubric/RubricDimension.java` — record (id/name/description/scoring)
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/feedback/rubric/RubricFamily.java` — 싱글톤. _dimensions.yaml + _mapping.yaml 로드 결과
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/feedback/rubric/Rubric.java` — record (rubricId/usesDimensions/perTurnRules/levelExpectations)
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/feedback/rubric/DimensionRef.java` — ref/weight/conditional
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/feedback/rubric/DimensionScore.java` — record (score/observation/evidenceQuote)
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/feedback/rubric/RubricScore.java` — rubricId/scoredDimensions/dimensionScores/levelFlag
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/feedback/rubric/RubricLoader.java` — @Component, YAML 로드 + resolveFor(Question, QuestionSet, Interview)
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/feedback/rubric/RubricScorer.java` — @Service, AiClient.chat() 호출 (callType="rubric_scorer")
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/feedback/rubric/entity/RubricScoreEntity.java` — V26 매핑 JPA Entity
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/feedback/rubric/repository/RubricScoreRepository.java` — findByInterviewIdOrderByTurnId() 등
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/feedback/rubric/dto/RubricScoreResponse.java` — DTO (Entity 직접 반환 금지)

### 신규 생성 (테스트)
- `/Users/koseonje/dev/devlens/backend/src/test/java/com/rehearse/api/domain/feedback/rubric/RubricLoaderTest.java` — QuestionSetCategory 12개 매핑 실패 0건 검증
- `/Users/koseonje/dev/devlens/backend/src/test/java/com/rehearse/api/domain/feedback/rubric/RubricScorerTest.java` — DIMENSIONS_TO_SCORE 외 차원 null 반환 검증

### 수정
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpService.java` — 턴 종료 hook에서 RubricScorer.score() 호출 + V26 저장
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/resume/ResumeInterviewOrchestrator.java` — (plan-07 신규) 턴 종료 시 ResumeMode+currentLevel 전달하며 RubricScorer.score() 호출

### ⚠️ 교정 사항
- rubric 리소스 디렉토리: `backend/src/main/resources/rubric/` 신설 (기존 없음)
- plan-08 본문: `_dimensions.yaml`은 plan-11에서 D11~D14 추가 수정 예정. 초기 생성 시 D1~D10만 포함, 주석으로 "D11~D14는 plan-11에서 추가" 명시 권장.

---

## plan-09 Feedback Synthesizer

### 신규 생성
- `/Users/koseonje/dev/devlens/backend/src/main/resources/prompts/template/session-feedback-synthesizer.txt` — 5섹션 강제 종합 프롬프트
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/infra/ai/prompt/SessionFeedbackSynthesizerPromptBuilder.java` — 종합 프롬프트 빌더
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/feedback/session/SessionFeedbackService.java` — @Service, 세션 종료 hook + partial-first 생성 (callType="feedback_synthesizer")
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/feedback/session/entity/SessionFeedback.java` — JPA Entity (V27 매핑, status: PRELIMINARY/COMPLETE)
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/feedback/session/repository/SessionFeedbackRepository.java` — JPA Repository
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/feedback/session/dto/SessionFeedbackResponse.java` — 5섹션 DTO
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/feedback/session/controller/AdminSessionFeedbackController.java` — GET /api/admin/interviews/{id}/session-feedback
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/interview/event/InterviewCompletedEvent.java` — 존재 확인 후 없으면 신규 생성

### 수정
- `/Users/koseonje/dev/devlens/backend/src/main/resources/application.yml` — rehearse.features.feedback-rubric.synthesizer-model 추가

### ⚠️ 교정 사항
- plan-09 전제: `InterviewCompletedEvent` 존재 여부는 plan-00a INVENTORY 확인 항목 — 현재 인벤토리에서 `event/` 서브패키지 존재 확인됨, 이벤트 클래스 목록은 소스 직접 확인 필요.
- plan-09는 기존 `FeedbackService` 경로를 수정하지 않음 — `SessionFeedbackService`가 `InterviewCompletedEvent`의 독립 리스너로 붙음.

---

## plan-10 Eval Harness Lite

### 신규 생성
- `/Users/koseonje/dev/devlens/eval/golden-sets/v1/concept/backend/` (디렉토리) — 10개 YAML (happy_path 5 + clarify 2 + give_up 2 + shallow 1)
- `/Users/koseonje/dev/devlens/eval/golden-sets/v1/experience/backend/` (디렉토리) — 10개 YAML
- `/Users/koseonje/dev/devlens/eval/golden-sets/v1/resume/backend/` (디렉토리) — 10개 YAML (Resume Track 전용)
- `/Users/koseonje/dev/devlens/eval/judges/j1-followup-relevance.txt` — J1 Judge 완성본 (plan-00d 초안 → 이 plan에서 완성)
- `/Users/koseonje/dev/devlens/eval/judges/j2-intent-handling.txt` — J2 Judge (intent_accuracy/response_quality)
- `/Users/koseonje/dev/devlens/eval/judges/j3-feedback-rubric-adherence.txt` — J3 Judge (has_observations/is_concrete/level_calibration/delivery_separation/category_dimension_fit/cross_category_pattern)
- `/Users/koseonje/dev/devlens/eval/scripts/run_eval.py` — 골든셋 실행 + Judge 호출 + 리포트 생성
- `/Users/koseonje/dev/devlens/eval/scripts/measure_judge_reliability.py` — 수동 라벨 vs Judge 일치율 측정
- `/Users/koseonje/dev/devlens/eval/reports/.gitkeep` — 실행 결과 저장 디렉토리 생성
- `/Users/koseonje/dev/devlens/eval/requirements.txt` — openai, anthropic, pyyaml (plan-00d와 통합)
- `/Users/koseonje/dev/devlens/eval/README.md` — 전체 eval 사용법 (plan-00d README 확장)

### 수정
- 없음

### ⚠️ 교정 사항
- plan-00d에서 생성한 `eval/judges/j1-followup-relevance.txt`는 초안. plan-10이 완성본으로 덮어씀 — 순서 의존성 주의.
- CI 자동화 (GitHub Actions) **Out of Scope** — 로컬 수동 실행만.

---

## plan-11 Nonverbal Rubric

### 신규 생성 (Lambda Python)
- `/Users/koseonje/dev/devlens/lambda/analysis/analyzers/nonverbal_rubric_mapper.py` — 결정론적 threshold 매퍼 (D11~D14, LLM 호출 0)
- `/Users/koseonje/dev/devlens/lambda/analysis/tests/test_nonverbal_rubric_mapper.py` — 경계값 + 결정론 + D14 교차 분석 테스트

### 신규 생성 (Backend YAML 리소스)
- `/Users/koseonje/dev/devlens/backend/src/main/resources/rubric/nonverbal-rubric.yaml` — D11~D14 base_weight + per_turn_rules + level_expectations
- `/Users/koseonje/dev/devlens/backend/src/main/resources/rubric/nonverbal-context-weights.yaml` — category/track/mode/difficulty 별 multiplier
- `/Users/koseonje/dev/devlens/backend/src/main/resources/rubric/nonverbal-improvement-actions.yaml` — D11~D14 × level 개선 액션 템플릿

### 신규 생성 (Backend Java — `domain/feedback/rubric/nonverbal/`)
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/feedback/rubric/nonverbal/NonverbalRubricScorer.java` — @Service, Lambda 이벤트 핸들러에서 호출, context_weights 적용
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/feedback/rubric/nonverbal/NonverbalTurnScore.java` — record (D11~D14 + raw_signals + appliedContextMultiplier)
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/feedback/rubric/nonverbal/NonverbalContextWeightsLoader.java` — YAML 로드 + resolve(questionCategory, track, mode, difficulty)
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/feedback/rubric/nonverbal/entity/NonverbalScoreEntity.java` — V28 매핑 JPA Entity
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/feedback/rubric/nonverbal/repository/NonverbalScoreRepository.java` — JPA Repository
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/feedback/rubric/nonverbal/dto/NonverbalScoreResponse.java` — DTO

### 신규 생성 (Flyway)
- `/Users/koseonje/dev/devlens/backend/src/main/resources/db/migration/V28__create_nonverbal_score.sql` — nonverbal_score 테이블
- `/Users/koseonje/dev/devlens/backend/src/main/resources/db/migration/rollback/V28__rollback.sql` — 롤백 스크립트

### 수정 (plan-08 산출물)
- `/Users/koseonje/dev/devlens/backend/src/main/resources/rubric/_dimensions.yaml` — D11~D14 4개 차원 추가 (category: nonverbal)
- `/Users/koseonje/dev/devlens/backend/src/main/resources/rubric/_mapping.yaml` — always_apply: nonverbal-rubric 규칙 추가

### 수정 (Lambda)
- `/Users/koseonje/dev/devlens/lambda/analysis/analyzers/verbal_prompt_factory.py` — 출력 스키마에 speed_variance 추가
- `/Users/koseonje/dev/devlens/lambda/analysis/analyzers/vision_analyzer.py` — 출력 스키마에 gaze_on_camera_ratio, posture_unstable_count 추가 (없다면)
- `/Users/koseonje/dev/devlens/lambda/analysis/handler.py` — nonverbal_rubric_mapper.score_turn() 호출 후 nonverbal_score 필드로 첨부

### 수정 (plan-09 산출물)
- `/Users/koseonje/dev/devlens/backend/src/main/java/com/rehearse/api/domain/feedback/session/SessionFeedbackService.java` — Delivery 섹션에 D11~D14 점수 + 개선 액션 주입
- `/Users/koseonje/dev/devlens/backend/src/main/resources/prompts/template/session-feedback-synthesizer.txt` — 비언어 섹션을 차원별 구조로 강제

### ⚠️ 교정 사항
- plan-11 전제: `vision_analyzer.py`의 gaze/posture 필드 존재 여부를 plan-00a INVENTORY에서 확인하도록 명시 — 현재 INVENTORY에서 Lambda 분석 코드 상세는 수집 범위 외. plan-11 실행 시 `lambda/analysis/analyzers/vision_analyzer.py` 직접 확인 필수.
- plan-11은 `_dimensions.yaml`과 `_mapping.yaml`이 plan-08에서 먼저 생성된 후 수정. 의존 순서: plan-08 완료 후 plan-11 착수.
