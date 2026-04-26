# Interview Quality 2026-04-20 — 진행 상황

## 태스크 상태

### Phase 0 (W1-W2) — Critic Remediation 선행 인프라

| # | 태스크 | 주차 | 상태 | 의존성 | 비고 |
|---|--------|------|------|--------|------|
| 00a | Codebase Inventory `[blocking]` | W1 초 | Completed | — | 실제 클래스/테스트/영향도 맵 — INVENTORY/TEST_BASELINE/IMPACT_MAP 머지 (S1, 2026-04-20) |
| 00b | AiClient Generalization `[blocking]` | W1 후 | Completed | 00a | C1+C3+M5+Missing(JSON 폴백) 근본 해결 (S2, 2026-04-20). `@RefreshScope`/`AiFeatureProperties`는 2026-04-23 철거 예정 (PR B) |
| 00c | Session State Persistence `[parallel:00b]` | W2 초 | Completed | 00a | C2+Missing(동시성, 메모리) 해결. Flyway V24~V27 (S3, 2026-04-21) |
| 00d | Observability `[parallel:00c]` | W2 후 | Completed | 00a | M2+Missing(APM) 해결. `OBSERVABILITY.md` + Timer 6 태그 + Counter 4 종(input/output/cached.read/cached.write) + `micrometer-registry-prometheus` 의존성 (S3c, 2026-04-24). 코드 기반은 S2(#336)+S3(#338) 선행, S3c(#347) 로 완결 |
| 00e | Feedback Migration Strategy `[parallel:00d]` | W2 후 | Completed | 00a | M6 해결. `FEEDBACK_DOMAIN.md` 결정 문서 작성 (S3b, 2026-04-24). `InterviewCompletedEvent` 부재 확인 → plan-09 에서 신규 도입으로 교정 |
| 00f | Interview Turn Policy Abstraction `[parallel:00c]` | W2 | Completed | 00a | `MAX_FOLLOWUP_ROUNDS=2` 하드코딩 제거 → `InterviewTurnPolicy` Strategy + `Standard`/`ResumeTrackPolicy`(7턴 skeleton) + Resolver. 663 tests pass (S3b, 2026-04-24). plan-07 선행 unblocked |

### Phase 1~4 (W3-W7) — 기존 플랜 (전제 인프라 위에서)

| # | 태스크 | 주차 | 상태 | 의존성 | 비고 |
|---|--------|------|------|--------|------|
| 01 | Intent Classifier (**4-intent**) | W3 | **Superseded by plan-15** (S9, 2026-04-27) | 00a,00b,00d | 2026-04-25 Phase A 구현 완료 후 dev 실측 confidence 0.0 매번 발생 → plan-15 audio chat 통합으로 대체. 클래스는 Claude fallback 경로 유지. Phase B 폐기 |
| 02 | Answer Analyzer (M1 Step A) `[parallel:03]` | W4 | **Superseded by plan-15** (S9, 2026-04-27) | 01, 00c | PR #353 머지 (`be68b0f`) 후 dev 실측 Claim 파싱 502 60% / latency 3.3s SLA 위반 → plan-15 통합. 클래스는 Claude fallback 경로 유지 |
| 15 | Audio Turn Analyzer (4-call → 2-call) `[blocking]` | W4 | **Code Merged / Gates Pending** (#358) | 01, 02 | PR #358 머지 (`a5c06ee`, 2026-04-27). 868 tests / 0 fail. dev 검증 게이트(502 0건 / 200 avg ≤5500ms / skip rate <20% / STT 정확도) 다음 세션 |
| 03 | Follow-up Generator v3 (M1 Step B) `[parallel:02]` | W4 | Completed (#353) | 02 계약 | P0. Step A → Step B 결합. ANSWER 경로 refactor + selectedPerspective echo + target_claim_idx (S6, 2026-04-26). PR #353 머지 (S7, 2026-04-25 18:16 UTC). 749 tests pass |
| 04 | Context Engineering 4-layer `[blocking]` | W5 | Code Merged / Gates Pending (#354) | 00b,00c | PR #354 머지. 검증 §1·§3 통과. §2(Prometheus 24h) 인프라 부재로 별도 SRE PR 이월. §4(MANUAL_AB)·§5(Compaction 정성) 다음 세션. 후속 B/C/D 항목은 plan-04 spec 참조 |
| 05 | Resume Extractor (Phase 1) `[parallel:06]` | W5 | Draft | 04, 00b | GPT-4o 호출은 00b의 modelOverride 사용. Dynamic Pacing: duration 무관 최대 추출 (2026-04-22) |
| 06 | Resume Interview Planner (Phase 2) `[parallel:05]` | W5 | Draft | 04, 00c | InterviewPlan 영속화는 V25. **Dynamic Pacing 재설계 (2026-04-22)**: duration 스케일링 폐기, priority 랭킹만 |
| 07 | Resume Orchestrator (Phase 3) | W6 | Draft | 04,05,06,00f | fact_check_flag 삭제 + 실제 진입점 명시 필요. `ResumeTrackPolicy` 에 `ChainStateTracker` 주입(00f skeleton 활용). **WRAP_UP 모드 + ClockWatcher + Resume Exclusivity Rule 추가 (2026-04-22)** |
| 08 | Rubric Family Scorer (10차원 × 7 rubric) | W7 | Draft | 02, 00c | **TODO 03 개정반영 — 전면 재작성**. `_dimensions.yaml` 마스터 + `_mapping.yaml` + 7개 rubric YAML. 작업량 1주 → 1~1.5주 |
| 09 | Feedback Synthesizer (M3 세션 종합) | W7 | Draft | 08, 00e | FEEDBACK_DOMAIN.md 결정 소비 |
| 11a | Lambda Nonverbal Schema Prerequisite `[blocking:11]` | W7 초 | Draft | 00a | **신규 (2026-04-21 VERIFICATION_REPORT §D3 대응)**. Gemini 프롬프트 3개 수치 필드 확장(`speed_variance` / `gaze_on_camera_ratio` / `posture_unstable_count`). plan-11 착수 전 필수 |
| 11 | Nonverbal Rubric (D11~D14 결정론 매퍼) `[parallel:08]` | W7 후 | Draft | 11a, 00a, 00c, 00e, 08 | TODO 09 반영 추가. Lambda Python mapper + backend context_weights. plan-09 선행 |
| 13 | Lambda Content Removal `[blocking:08,09]` | W7 후 | Draft | 08, 09 배포 + STAGING G1~G3 + MANUAL_AB_PROTOCOL 3~5건 통과 | **신규 (2026-04-22)**. Lambda `verbal`/`technical` 블록 제거, `TimestampFeedback` 컬럼 4개 drop (V29 — plan-11 V28 이후 순서), Rubric/Synthesizer를 Content 유일 소스로 확정. Content/Delivery 책임 경계 확정. 2026-04-23 flag-on 대신 ECR 단일 cut-over 로 갱신 |

## 진행 로그

### 2026-04-27 (S10 — plan-05 선행 spec 갱신 + audio chat P1 정리)

- plan-15 머지 후 P1 3건 정리 (별도 PR `refactor/audio-chat-resilience-cleanup`):
  - `ResilientAiClient.chatWithAudio` fallback 분기 인프라 일원화 (caller `AudioTurnAnalyzer` 단순화)
  - `TurnAnalysisResult` `dto/` → `vo/` 이동 (도메인 VO 위치 일관성)
  - `OpenAiClient` chat/audio body 빌딩 중복 제거
- plan-05 선행 작업:
  - plan-04 머지 (#354 ee67201) 반영해 plan-05 §"검증 5" 실 클래스명 + 메서드 시그니처 갱신
    - `InterviewContextBuilder.build(ContextBuildRequest)` → `FixedContextLayer.build()` → `SKELETON_BY_CALL_TYPE.getOrDefault("resume_extractor")` 경로 확정
    - `ContextBuildRequest` 필드 구조 (callType/runtimeState/exchanges/focusHints/providerHint) 확인
    - `BuiltContext` 반환 구조 (messages/tokenEstimate/perLayerTokens) 확인
  - `ResumeSkeletonCache` 신설 안 함 결정 — `InterviewRuntimeStateStore` 재사용 (plan-05 §"저장 정책" 반영)
    - `InterviewRuntimeState.resumeSkeletonCache` 필드(`CachedResumeSkeleton` 인터페이스) 이미 존재 확인
    - `ResumeSkeleton implements CachedResumeSkeleton` 구현으로 기존 Caffeine 2h TTL 캐시 재사용
  - PDF 라이브러리 확인: `PDFBox 3.0.4` (build.gradle.kts L73), 추가 의존성 불필요, `setSortByPosition(true)` 로 2단 컬럼 지원
  - plan-05 §"생성/수정 파일" 갱신: `ResumeSkeletonCache.java` → `FixedContextLayer.java` 수정으로 교체
  - IMPACT_MAP.md plan-05 항목 갱신 (동일 내용 반영)

### 2026-04-27 (S9 — Audio Turn Analyzer 통합 (4-call → 2-call) — plan-15 + PR #358 머지 + 후속 리팩토링)

- **PR #358** (`[BE] feat: 면접 한 턴을 단일 audio 호출로 분석해 응답 시간·실패율 단축`):
  - 브랜치 `feat/audio-turn-analyzer` (develop 분기)
  - **머지 완료**: 2026-04-27, mergeCommit `a5c06ee`, base=develop, squash + delete-branch
  - **deploy-dev.yml**: run `24964656303` queued → 진행 중. 다음 세션 첫 명령: `gh run view 24964656303 --json status,conclusion`
  - CI 통과 (Backend CI 1m13s / Frontend CI 5s)
- **dev 실측 진단** (2026-04-26 docker logs + actuator/prometheus, 면접 1세션 5턴):
  - 후속질문 0건. 502 AI_005 3건 (`Claim` deserialize 실패: AI 가 string 배열 반환), skip 2건 (Step B 자체 판단)
  - IntentClassifier confidence 0.0 매번 → forceAnswer fallback (분류기 무력화)
  - Latency 실측: intent_classifier 1.2s / answer_analyzer **3.3s** (SLA 1.5s 위반) / generate_followup 4.2s / **endpoint 200 avg 6.07s** (Aggregate SLA 4s 위반) / endpoint 502 16s
- **근본 원인**: 4-call 직렬 (`STT+무의미한 Step B v1` + `IntentClassifier` + `AnswerAnalyzer` + `Step B v3`) — plan-01:64-77 의 "STT 분리 비용" 가정을 audio chat 통합으로 무효화
- **plan-15 신규** (`docs/plans/interview-quality-2026-04-20/plan-15-audio-turn-analyzer.md`):
  - Primary: `gpt-4o-mini-audio-preview` 단일 호출로 transcribe + intent + answer_analysis 통합 (`response_format = json_schema strict`)
  - Fallback (Claude audio 미지원): 기존 `IntentClassifier` + `AnswerAnalyzer` 직렬 호출 (단순화 안)
  - Step B v3 그대로 유지 (4단계 → 1단계만 신규)
  - Step B 프롬프트 skip 룰 엄격화 (`follow-up-concept.txt`/`follow-up-experience.txt`) 동봉
- **plan-01/02 status header 업데이트**: `Superseded by plan-15` 명시. 클래스는 Claude fallback 경로에서 유지
- **신규 코드** (PR #358 안 commit `086b9b2` + `a9e955d` + `b2c605e`):
  - 도메인: `AnswerAnalysis.empty(turnId)` + `applyL1FalseNegativeGuard(intent)` 메서드, `AskedPerspectives` VO, `AiErrorCode.triggersAudioFallback()`
  - 신규 빈: `AudioTurnAnalyzer` (audio chat 단일 호출), `TextFallbackTurnAnalyzer` (STT + IntentClassifier + AnswerAnalyzer 직렬), `FollowUpQuestionWriter` (Step B 작문 책임), `IntentDispatcher` (handler 라우팅 + 미등록 throw)
  - infra: `AiClient.chatWithAudio`, `OpenAiClient` audio model 호출, `application.yml` audio-model 설정
  - 프롬프트: `audio-turn-analyzer.txt` 신규, `follow-up-concept/experience.txt` skip 룰 엄격화
- **post-impl 리뷰 P0 반영**: L1 FN 가드 이중 적용 차단(fallback epilogue bypass), audio 파일 크기 가드(10MB), parseOrRetry → parseJsonResponse(audio 컨텍스트 보존), fallback 진입 카운터 추가, TurnAnalysisResult 누락 필드 안전 채움
- **후속 리팩토링** (commit `a9e955d`, `b2c605e`): AudioTurnAnalyzer 183→110줄 / FollowUpService 235→130줄. 책임 분리 + 도메인 메서드 추출. 단위 테스트 3종 신규
- **테스트**: 868 tests / 0 failures / 1 skipped (baseline 838 + 신규 30)
- **검증 게이트** (dev 배포 후 직접 진행 — 다음 세션):
  - `http_server_requests_seconds{uri=".../follow-up",status="502"}` count = **0**
  - 200 응답 avg ≤ **5500ms** (현재 6.07s → 목표)
  - skip rate < **20%** (현재 100%)
  - `rehearse_followup_skip_total{reason="audio_chat_fallback_to_stt"}` 비율 (낮을수록 audio 모델 정상)
  - 한국어 STT 정확도 정성 평가 (5세션). 미달 시 `gpt-4o-audio-preview` (full) 모델 변경 검토
- **이월 항목 (P1, 별도 PR)**:
  - ResilientAiClient.chatWithAudio 비대칭 추상화 — 인프라 레벨 fallback (caller 부담 제거)
  - TurnAnalysisResult 위치 정리 (`dto/` → `vo/`)
  - OpenAiClient chatWithAudio 코드 중복 정리 (legacy `generateFollowUpWithAudio` 와의 통합)

### 2026-04-26 (S8 — plan-04 Context Engineering 4-Layer 구현 + PR #354)

- **PR #354** (`[BE] feat: 면접 매 턴 LLM 입력 토큰을 90% 줄이고 시스템 프롬프트를 캐시`):
  - 브랜치 `feat/plan-04-context-engineering` (origin/develop = `6ac9c5a` 베이스)
  - CI 통과 (Backend CI / Frontend CI 모두 SUCCESS)
  - **머지 완료**: 2026-04-26 02:58 UTC, mergeCommit `ee67201`, base=`develop`, squash + delete-branch
  - **deploy-dev.yml**: run `24946779778` 진행 중 (세션 종료 시점). 다음 세션 첫 명령: `gh run view 24946779778 --json status,conclusion`
- **신규 코드**:
  - `infra/ai/context/**` 14 클래스 + 테스트 7 클래스 (L1 Fixed / L2 SessionState / L3 DialogueHistory + Async Compactor / L4 Focus / InterviewContextBuilder + AnswerAnalysisJsonRenderer)
  - `compaction-summarizer.txt` 프롬프트 + `CompactionExecutorConfig` (graceful shutdown 30s)
  - `ContextEngineeringProperties` + `application.yml` `rehearse.context-engineering` 블록
  - `ContextEngineeringMetrics` (tokens / cache_hit_ratio / compaction_count 3 Histogram/Gauge/Counter)
  - 5 caller 통합 (`AnswerAnalyzer`/`IntentClassifier`/`FollowUpService`/`ClarifyResponseHandler`/`GiveUpResponseHandler`)
- **테스트**: 838 tests / 0 failures / 1 ignored (baseline 749 + 신규 ~89)
- **토큰 측정**: `python3 eval/context/measure_tokens.py` PASS — avg=609, max=687, min=441 (5 fixture × 10턴, 4-char/tok 휴리스틱)
- **리뷰 반영** (architect-reviewer + code-reviewer 병렬):
  - TOCTOU race fix: `InterviewRuntimeState.tryStartCompaction(windowEnd)` atomic add 로 게이트
  - 사용 안 되는 `PromptCacheStrategy`/`OpenAi`/`Claude` 어댑터 3종 + 테스트 2종 삭제 (캐시 마킹은 `ChatMessage.cacheControl` + `ClaudeApiClient.SystemContent.withCaching` 에 이미 와이어링)
  - `CompactionExecutor` graceful shutdown (`setWaitForTasksToCompleteOnShutdown(true)` + 30s)
- **검증 게이트 현실화** (`plan-04-context-engineering.md` 검증 표 갱신):
  - §1 평균 입력 토큰 ≤8000: ✅ PASS
  - §2 캐시 히트율 ≥95% (24h): ⚠️ DEFERRED — Prometheus 서버 부재. 별도 SRE 인프라 PR 후 재실행
  - §3 회귀 0: ✅ PASS (838 tests)
  - §4 MANUAL_AB 3~5건: ⏳ 다음 세션
  - §5 Compaction 정성 5세션: ⏳ 다음 세션
  - §6 progress.md Completed: §4·§5 통과 후
- **이월 항목** (`plan-04-context-engineering.md` `## 이월 사항` 섹션):
  - A: 게이트 §2/§4/§5 (다음 세션 직접 진행)
  - B: 내부 미구현 6종 (B2 asked_perspectives derive 가 plan-05 진입 전 High 우선순위)
  - C: Prometheus 인프라 자체 (SRE 별건)
  - D: plan-03 잔여 게이트 4종 (병렬 트랙)

### 2026-04-26 (S7 — PR #353 머지 + 문서 정합화)

- **PR #353 머지** (`[BE] feat: 답변 분석 단계 분리 + 꼬리질문 작문 정확도 개선`):
  - `mergedAt=2026-04-25T18:16:33Z`, `mergeCommit=be68b0f`, base=`develop`, squash merge
  - 749 tests pass / 0 failures (baseline 719 + 신규 ~30)
- **문서 정합화** (이번 세션):
  - `plan-02-answer-analyzer.md` 헤더 Completed 전환 + `## 머지 결과` 섹션
  - `plan-03-followup-generator-v3.md` 동일 처리
  - 본 progress.md Phase 1~4 표 02/03 행 `Implemented → Completed (#353)`
  - `HANDOFF.md` S6/S7/S8 Kickoff 섹션 추가 (S5 이후 부재 해소)
  - `plan-04-context-engineering.md` 헤더 `Draft → In Progress (S8)` + spec 보강
  - `plan-02` line 1 typo (`gogo# Plan 02:` → `# Plan 02:`) 수정
- **잔여 게이트** (plan-04 와 무관, 독립 트랙 병렬 진행):
  - [ ] FE 계약 전달 — `selectedPerspective` echo + `presentToUser` (FE PR)
  - [ ] LIVE 골든셋 실행 (`LIVE_TEST=true`)
  - [ ] MANUAL_AB 3~5건 (v2 vs v3)
  - [ ] 스테이징 Prometheus `rehearse.ai.call.duration_seconds{call.type=~"answer_analyzer|follow_up_generator_v3"}` p95
- **plan-04 의존성** (00b/00c/00d/01/02/03) 모두 해소 — S8 즉시 착수 (`feat/plan-04-context-engineering` 브랜치, develop = `6ac9c5a` 베이스)

### 2026-04-26 (S6 — plan-02/03 코드 구현 + 단일 PR 준비)

- **plan-02 코드 구현 완료** (브랜치 `feat/plan-02-answer-analyzer`, 16 files, +892/-2 머지 전):
  - `AnswerAnalysis` (`implements TurnAnalysis`) + `Claim` + `EvidenceStrength`/`Perspective`/`RecommendedNextAction` enum 분리
  - `AnswerAnalyzer` 서비스: `AiClient.chat()` callType=`answer_analyzer`, temp 0.2, max 800, JSON_OBJECT
  - L1 FN 가드: `claims=[] AND answer_quality<=1 → CLARIFICATION` 강제
  - `InterviewRuntimeState.recordAnalysis/getAnswerAnalysis` 접근자 (instanceof 안전 캐스팅)
  - 프롬프트 템플릿 `follow-up-step-a-analyzer.txt`: 보안 규칙 + delimiter + 4 few-shot
  - `application.yml`: `rehearse.answer-analyzer.enabled: true`
  - 테스트 4 클래스 (record 검증 + 캐시 접근 + L1 가드 + 프롬프트 빌더)
- **plan-03 코드 구현 완료** (동일 브랜치 추가 커밋):
  - `GeneratedFollowUp` + `targetClaimIdx`/`selectedPerspective` (snake_case Jackson 매핑)
  - `FollowUpExchange.selectedPerspective` (옵션 A) + `FollowUpResponse.selectedPerspective` echo
  - `FollowUpContext.currentMainQuestionId` (`FollowUpTransactionHandler` 에서 MAIN question id 도출)
  - `FollowUpPromptBuilder.buildUserPromptWithAnalysis` — ANSWER_ANALYSIS 블록 직렬화 (claims/missing_perspectives/unstated_assumptions/recommended_next_action/asked_perspectives)
  - `follow-up-concept.txt` v3: ANSWER_ANALYSIS 입력 명세 + target_claim_idx 우선순위 + JSON 스키마 확장 (CONCEPT 모드 selected_perspective=null 강제)
  - `follow-up-experience.txt` v3: ANSWER_ANALYSIS + selected_perspective 7-enum 우선순위 + asked_perspectives 중복 금지
  - `FollowUpService` ANSWER 경로 refactor: STT 재사용 → AnswerAnalyzer.analyze → SKIP 권고 시 cost saver → `aiClient.chat(ChatRequest)` Step B → parseOrRetry → save. callType=`follow_up_generator_v3`, temp 0.6, max 1024, JSON_OBJECT
  - askedPerspectives 추출: `previousExchanges.selectedPerspective` 관대 파싱 + distinct
- **plan-02 V{XX} FK migration → plan-08 이관**: `interview_turn` 테이블 부재로 본 PR 범위 외. plan-08 `전제` 섹션에 흡수, plan-02 spec 본문 취소선 처리
- **테스트 결과**: 749 tests pass / 0 failures (baseline 719 + 신규 ~30). `FollowUpServiceTest` 7 시나리오 + `FollowUpServiceIntentBranchTest` ANSWER 경로 Step A/B mock 추가 + 핸들러 3 테스트 FollowUpContext 시그니처 갱신
- **잔여 게이트**:
  - [ ] FE 계약 전달 — `FollowUpRequest.FollowUpExchange.selectedPerspective` 입력 + `FollowUpResponse.selectedPerspective` echo 사용
  - [ ] LIVE 골든셋 (Step A depth_score ±1 / target_claim_idx 범위 / selected_perspective 중복)
  - [ ] MANUAL_AB 3~5 건 (v2 vs v3 claim 타겟팅 정확도)
  - [ ] 스테이징 Prometheus `rehearse.ai.call.duration_seconds{call.type=~"answer_analyzer|follow_up_generator_v3"}` p95 확인

### 2026-04-25 (S5 — plan-02/03 단일 PR 착수 — 문서 정합화)

- **전략**: plan-02 (Answer Analyzer M1 Step A) + plan-03 (Follow-up Generator v3 M1 Step B) 단일 BE PR 머지. plan-02 단독 머지 시 Step A 출력 소비처 부재 → 데드코드. 실행계획 사용자 인계 plan ([Plan02 + Plan03 단일 PR 실행계획])
- **plan-02 문서 patch (3건)**:
  - 경로 정정: `domain/interview/runtime/InterviewRuntimeState.java` → `domain/interview/entity/InterviewRuntimeState.java` (실측 위치)
  - `AnswerAnalysis implements TurnAnalysis` 명시 (마커 인터페이스 기존 존재, plan-08 Rubric 과 캐시 키 통합 의도)
  - `InterviewRuntimeState.recordAnalysis()/getAnswerAnalysis()` 메서드 추가를 본 PR 범위로 명시 (`turnAnalysisCache` 필드는 plan-00c 에서 이미 존재, 본 PR 은 접근자만 추가)
- **plan-03 GeneratedFollowUp 위치 확인**: `infra/ai/dto/GeneratedFollowUp.java` 일치, patch 불필요
- **다음**: plan-02 코드 구현 → plan-03 코드 구현 → 통합 테스트 → 리뷰 → 단일 PR

### 2026-04-25 (S4 — plan-01 Phase A 구현 완료)

- **전략**: plan-01 + plan-02 + plan-03 단일 PR 머지 목표. Phase A (L1 post-hoc) → 리뷰·검증 → Phase B (L2/L3 통합) 순차 진행. 실행계획 `~/.claude/plans/plan01-compressed-lark.md`
- **STT 처리 결정**: post-hoc 채택 (사용자 결정). `generateFollowUpWithAudio` 결과의 `answerText` 로 intent classify → OFF_TOPIC/CLARIFY/GIVE_UP 이면 생성된 follow-up 버리고 handler 응답 덮어씀. Trade-off: OFF_TOPIC SLA `≤500ms + LLM 0회` 는 미달성 (GPT-4o-audio 호출 1회 이미 발생) — spec 에 기재됨. 구조 개편은 별도 plan 으로 이월
- **OFF_TOPIC 연속 감지**: `FollowUpRequest.previousExchanges` 재활용. `FollowUpExchange.followUpType` 메타필드 우선 사용, `OffTopicMarker.CONNECTOR` 텍스트 매칭은 backward-compat fallback. `OffTopicEscalationDetector` 컴포넌트로 분리 (지식 응집). FE 가 `followUpType=OFF_TOPIC_REDIRECT` 를 다음 턴 `previousExchanges` 에 채워주면 정확한 카운팅 보장
- **구현 파일** (신규 19 + 수정 5):
  - VO: `domain/interview/vo/IntentType` (4-intent enum), `IntentResult` (record + `forceAnswer()` fallback 플래그 + `of()` 정상 팩토리)
  - Config: `global/config/IntentClassifierProperties` (`@ConfigurationProperties` + `@Validated` + record, `fallback-on-low-confidence:0.7` / `off-topic-consecutive-limit:3`). `RehearseApiApplication` 에 `@ConfigurationPropertiesScan("com.rehearse.api.global.config")` 로 자동 스캔
  - Prompt: `infra/ai/prompt/{IntentClassifier,ClarifyResponse,GiveUpResponse}PromptBuilder` + `resources/prompts/template/{intent-classifier,clarify-response,giveup-response}.txt`. **보안 규칙** 섹션 + `<<<USER_UTTERANCE>>>` / `<<<MAIN_QUESTION>>>` / `<<<PREVIOUS_TURN>>>` delimiter 로 prompt injection boundary 확보. few-shot: ANSWER 4 / CLARIFY 2 / GIVE_UP 3 / OFF_TOPIC 2 + ANSWER negative (`"잘 모르지만 ~ 같아요"` → ANSWER)
  - Service: `domain/interview/service/{IntentClassifier, OffTopicResponseHandler, ClarifyResponseHandler, GiveUpResponseHandler, OffTopicEscalationDetector}` 모두 `IntentResponseHandler` 인터페이스 구현 + `IntentBranchInput` record 통합 입력. Classifier 는 `AiClient.chat()` 경유 (callType="intent_classifier", temperature=0.1, maxTokens=200, JSON_OBJECT). `IntentClassificationResponse.intent: String` + `parseIntent()` 로 대/소문자·하이픈 관대 처리. confidence < 0.7 또는 파싱 실패·`Exception` → `forceAnswer` fallback. Clarify/GiveUp 은 `RuntimeException` 만 catch (InterruptedException 미삼킴), 실패 시 안전 fallback (`CLARIFY_FALLBACK`/`GIVE_UP_FALLBACK` 고정 메시지)
  - OffTopicResponseHandler: LLM 없음. 리드인 풀 4개 + 고정 connector + `Math.floorMod(Long.hashCode(interviewId ^ turnIndex*31), 4)` 명시 산식. OFF_TOPIC 연속 escalation 결정도 핸들러 내부에서 처리 (GiveUpResponseHandler 위임)
  - OffTopicMarker: `CONNECTOR`/`FOLLOW_UP_TYPE`/`SKIP_REASON` 상수 별도 클래스로 분리 (Handler ↔ Detector 양방향 결합 제거)
  - FollowUpService: `IntentResponseHandler` Strategy `Map<IntentType, Handler>` 자동 등록(`@PostConstruct`) → `dispatchIntentBranch` 가 한 줄 dispatch. PR #348 `InterviewTurnPolicy` 패턴과 일관. ANSWER 는 그대로 기존 저장 경로
  - DTO: `FollowUpResponse.presentToUser: boolean` 신규 + `aiSkip()`/`intentBranch(payload)` 정적 팩토리 추출 (빌드 중복 5→1). `FollowUpExchange.followUpType` 필드 추가 (escalation 메타 카운팅용, optional). **FE 계약 변경 필요 — Phase A 머지 전 FE 팀 전달**
- **리뷰 피드백 반영** (architect-reviewer + code-reviewer 병렬 — 각 조건부 승인, Blocker 4 + High 2 + Medium 5 해결):
  - Blocker 1: `presentToUser` 계약 추가
  - Blocker 2: prompt injection delimiter + 시스템 보안 규칙 (3 builder 전부)
  - Blocker 3: LIVE 골든셋 단일 집계 `accuracy >= 0.90` 로 flaky 예방
  - Blocker 4: IntentType String 수신 + 관대한 파싱 (대소문자·하이픈 허용)
  - High 1: `generateFollowUp()` 메서드 추출
  - High 2: `OffTopicEscalationDetector` 컴포넌트 분리
  - Medium: `@ConfigurationProperties` 승격 + `global/config` 이동 + `@Validated`, `IntentResult.fallback` 플래그, `catch(RuntimeException)` 좁히기 + stacktrace, Clarify/GiveUp graceful fallback, GIVE_UP few-shot 3개 + negative 1개 보강, FollowUpService Phase 라벨 주석 제거
- **테스트**: 706 → 719 (+13 신규). 0 failures / 0 errors / 1 skipped (LIVE 골든셋 `@EnabledIfEnvironmentVariable("LIVE_TEST","true")` 정상 skip)
- **잔여 Phase A 게이트**:
  - [ ] 로컬 bootRun 4분기 수동 curl 검증 (OpenAI API 키 필요)
  - [ ] 스테이징 배포 후 Prometheus `rehearse.ai.call.duration_seconds{call.type="intent_classifier"}` p95 확인
  - [ ] LIVE 골든셋 실행 (`LIVE_TEST=true ./gradlew test --tests IntentClassifierGoldenSetLiveTest`, accuracy ≥ 0.90)
  - [ ] MANUAL_AB_PROTOCOL 3~5건 (OFF_TOPIC 2 + META 1 포함)
  - [ ] FE 팀에 `presentToUser` 계약 전달
- **Phase B 진입 조건**: 위 5건 전원 통과 후 plan-02 Answer Analyzer + plan-03 Follow-up Generator v3 구현 착수 (동일 PR)
- **이월 이슈 (Phase B 후보)**:
  - `FollowUpExchange.skipReason` 필드 왕복 계약 (현재 marker 문자열 감지)
  - `FollowUpResponseType` enum 도입 (type 문자열 정리)
  - 3 PromptBuilder 템플릿 로드 공통화 (L2/L3 추가 시점)
  - STT 분리 구조 개편 (OFF_TOPIC 진정한 ≤500ms SLA 복구)

### 2026-04-24 (plan-01 4-intent 확장 결정 — 문서 갱신)

- **결정**: `IntentType` 을 3-intent (ANSWER/CLARIFY_REQUEST/GIVE_UP) → **4-intent** (+ **OFF_TOPIC**) 로 확장. META 발화는 OFF_TOPIC 에 통합.
- **근거**: OFF_TOPIC 을 앞단 L1 에서 직접 분기하면 plan-02 Analyzer(L2) + plan-03 Follow-up(L3) 호출을 완전 생략 → 사용자 path p95 ≤ 500ms. 기존 3-intent 축소안의 "빈 답변 해석 LLM 낭비" 를 근본 제거.
- **OFF_TOPIC handler 는 LLM 미호출** — 객관·중립 리드인 풀(4개, 겉치레 호응 금지) + 고정 connector `"질문에 대한 답변을 적절히 해주세요."` + 원 mainQuestion 템플릿 조립. 비용 0, 지연 최소, 톤 예측성. `Math.floorMod(Objects.hash(sessionId, turnIndex), pool.size())` 결정적 선택.
- **턴 소비 정책**: OFF_TOPIC 은 round counter 증가 **안 함**, `currentMainQuestion` 유지. 다음 ANSWER 시 원 질문 + 새 답변 조합으로 정상 L1→L2→L3 파이프라인 동작 → 꼬리질문 원 주제 기반 생성. 연속 3회 OFF_TOPIC 시 GIVE_UP 경로 escalation (`rehearse.intent-classifier.off-topic-consecutive-limit: 3`).
- **plan-02 Step A 가드 유지**: `claims=[] && answer_quality<=1 ⇒ CLARIFICATION` 을 "L1 분류기 False Negative 안전망" 으로 목적 재정의 (defense in depth). 삭제하지 않음.
- **갱신 문서**: plan-01, plan-02, REMEDIATION.md, MANUAL_AB_PROTOCOL.md, requirements.md, 본 progress.md (6개).
- **검증 골든셋**: 20개 → **25개** (OFF_TOPIC 5개 추가 — META 3 / 무관 2). Intent 전체 정확도 ≥ 90%, OFF_TOPIC 자체 정확도 ≥ 80%.
- **plan-03 은 변경 없음** — `recommended_next_action` 시그널 consume 로직 그대로.
- **코드 구현은 S4+ 에서** — 본 결정은 문서 전용 갱신.

### 2026-04-24 (S3b — plan-00f Interview Turn Policy 추상화 완료)

- **신규 패키지 `domain/interview/policy/`** 5 파일:
  - `InterviewTrack` enum (`CS`, `LANGUAGE`, `RESUME`)
  - `InterviewTurnPolicy` 인터페이스
  - `StandardFollowUpPolicy` — `@Value("${rehearse.interview.policy.standard.max-follow-up-rounds:2}")` 주입, CS/Language 행위 무변경
  - `ResumeTrackPolicy` — 7턴 하드 상한 skeleton (plan-07 `ChainStateTracker` 주입으로 확장 예정)
  - `InterviewTurnPolicyResolver` — `Interview.getTrack()` switch 라우팅
- **`Interview.getTrack()` 파생 메서드** — `interviewTypes.contains(RESUME_BASED)` 시 `RESUME`, 나머지 `CS`. DB 컬럼 추가 0
- **`FollowUpTransactionHandler` 리팩터** — `MAX_FOLLOWUP_ROUNDS` 상수 + `validateFollowUpRoundLimit()` 삭제. `turnPolicyResolver.resolve(interview).assertCanContinue(...)` 위임
- **`application.yml`** — `rehearse.interview.policy.standard.max-follow-up-rounds: 2` 블록 추가
- **테스트 4 건** (신규 3 + 수정 1):
  - `StandardFollowUpPolicyTest` (6) — 0/1/2/3턴 경계값 + 3 설정 튜닝 검증
  - `ResumeTrackPolicySkeletonTest` (3) — 6턴 허용 / 7턴 예외
  - `InterviewTurnPolicyResolverTest` (3) — CS / RESUME_BASED / LANGUAGE_FRAMEWORK 라우팅
  - `FollowUpTransactionHandlerTest` 수정 — `loadFollowUpContext_maxFollowUpExceeded` 를 policy Mock 기반으로 재작성 (행위 무변경, 동일 에러 code)
- **실측 교정**: 원 plan `interview.getResumeSkeletonId() != null` 분기는 실체 필드 부재 → `Interview.getTrack()` + `InterviewType.RESUME_BASED` 기반 판정으로 교정
- **`./gradlew test` 결과**: 663 tests / 0 failures / 0 errors / 0 ignored
- **plan-07 unblocked**: `ResumeTrackPolicy` skeleton 에 `ChainStateTracker` 주입하면 plan-07 Resume Orchestrator 구현 가능

### 2026-04-24 (S3b — plan-00e Feedback Migration 결정 완료)

- `FEEDBACK_DOMAIN.md` 신규 — 결정 1~6 확정 (병존 / partial-first / Admin API / InterviewCompletedEvent 신규 / 패키지 경로 / flag 없음)
- **실측 교정**: `InterviewCompletedEvent` 가 현재 코드에 없음(grep 0건) → plan-09 에서 신규 도입 + `InterviewCompletionService.complete()` 에서 `ApplicationEventPublisher` 경유 발행으로 결정 4 갱신
- **패키지 경로 확정**: aa88a96 리팩터 반영 → `domain/feedback/session/{controller,service,entity,repository,dto}` 서브패키지 신설
- **Out of Scope**: plan-09 코드 구현은 S9+ 에서. 본 세션은 결정 문서만
- 후속 세션 (S4) 착수 가능: plan-01 Intent Classifier (단, 00d/00f 선행)

### 2026-04-24 (S3c — plan-00d 완결 — Counter 4 종 + prometheus registry)

- **시그니처 확정**: plan 본문 초안의 4-arg `recordChat(callType, model, provider, Supplier)` → 실구현 2-arg `recordChat(String, Callable<ChatResponse>)` 로 **실구현 정답** 채택. 근거: `ResilientAiClient` fallback 시 provider/model 은 응답 수신 후 확정 → 호출 전 pre-tagging 불가. `ChatResponse.provider()`/`model()` 후추출이 정확.
- **토큰 Counter 4 종 구현** (`AiCallMetrics.recordChat()` finally 블록):
  - `rehearse.ai.call.tokens.input` (Usage.inputTokens)
  - `rehearse.ai.call.tokens.output` (Usage.outputTokens)
  - `rehearse.ai.call.tokens.cached.read` (Usage.cacheReadTokens — OpenAI `prompt_tokens_details.cached_tokens` / Claude `cache_read_input_tokens`)
  - `rehearse.ai.call.tokens.cached.write` (Usage.cacheWriteTokens — Claude `cache_creation_input_tokens`)
  - 태그: `call.type` / `provider` / `model` (Timer 와 동일 키 → 쿼리 간소화)
  - 0 토큰은 미등록 — 무의미 시리즈 Prometheus 누적 방지
  - 예외 경로(outcome=failure)에서는 Counter 미기록 (Timer 만)
- **의존성 추가**: `backend/build.gradle.kts` 에 `runtimeOnly("io.micrometer:micrometer-registry-prometheus")`. Spring Boot 3.x 는 `spring-boot-starter-actuator` 만으로는 `/actuator/prometheus` 엔드포인트 노출 안 함 → 현재 설정 그대로면 Grafana scraping 전면 실패. **관측 인프라 실작동을 위한 필수 의존성**.
- **테스트 추가**: `AiCallMetricsTest` 에 5 케이스 추가 — input/output Counter 증가 / cached read·write 분리 / 0 토큰 미등록 / 예외 경로 Counter 미기록 / 복수 호출 누적.
- **문서 갱신**: `OBSERVABILITY.md` Counter 표 4 종 + PromQL 쿼리 6 종 (provider 별 비용, 캐시 절감률, cache_write 추이) + 의존성 메모. `plan-00d-observability.md` 시그니처·파일 목록·검증 항목 교정.
- **라이브 검증**: `./gradlew bootRun --args='--spring.profiles.active=local --spring.sql.init.mode=never'` 로 실제 기동 → `/actuator/prometheus` HTTP 200 확인. Caffeine 캐시 메트릭 6 종 (`cache_gets_total` / `cache_evictions_total` / `cache_eviction_weight_total` / `cache_puts_total` / `cache_size`) 노출 확인.
- **문서 오류 수정**: Caffeine 메트릭 실제 이름을 확인해 OBSERVABILITY.md 의 `rehearse_runtime_state_cache_*` 쿼리를 `cache_*{cache="rehearse.runtime.state"}` 로 전면 교체. Micrometer `CaffeineCacheMetrics.monitor()` 의 세 번째 인자는 metric prefix 가 아니라 `cache` 태그 값 — 플랜 문서 초안 가정이 잘못됐음을 실측으로 확정.
- **AI 메트릭 노출 검증 보류**: 로컬 실 AI 호출(API 키) 없이는 `rehearse_ai_call_*` Lazy 등록 안 됨 → 스테이징 배포 후 실 호출 1 회로 검증 예정 (`OBSERVABILITY.md §검증 스냅샷` 부록 업데이트).
- **실제 머지 결과**: #347 (00d S3c) 먼저 머지, #346 (00e) 은 별도 PR 대신 #348 에 cherry-pick 으로 통합 후 close — 최종 develop 에는 #347 + #348 2 개 squash 커밋만 남음. S3 마일스톤(00c/00d/00e/00f) 전부 Completed.

### 2026-04-24 (S3b — plan-00d Observability 1차 — docs 초안)

- `OBSERVABILITY.md` 신규. AI 호출 Timer(`rehearse.ai.call.duration` + 태그 6 종) + Caffeine 캐시 메트릭(`rehearse.runtime.state.*` 5 종) 계약 정의
- Grafana/PromQL 쿼리 레퍼런스 7 종 (p95/fallback/캐시/토큰/실패율/Runtime State 히트율·eviction)
- Alert 임계치 5 종 가이드 (실제 Alertmanager 설정은 인프라 별건)
- 배포 중 회귀 감지 체크리스트 3 단계 (10 분/1 시간/1 일) — plan-01~ 롤아웃 시 공식 레퍼런스
- **실측 확인**: `application.yml` management 설정 이미 `prometheus` 노출 중. 추가 수정 0
- **권한 제한**: 로컬 `bootRun` 실행 불가 → 라이브 스냅샷 캡처는 스테이징 배포 후 부록으로 추가 예정 (문서 §검증 스냅샷)
- **이월**: Counter 3 종(`tokens.input/output/cached`) 실제 구현은 plan-04 Context Engineering PR 에서 `ChatResponse.Usage` 파싱과 함께 권장

### 2026-04-23 (A/B 측정 인프라 축소 + Feature Flag 전면 제거)

플랜 본체 착수 전, 측정·롤백 인프라가 본체(LLM 품질 개선)보다 복잡해지는 위험을 검토하고 다음 결정 적용.

- **plan-10 Eval Harness Lite 전체 삭제**: 골든셋 30 + Judge 3(J1/J2/J3) + 수동 라벨 20건 + Cohen's κ 검증 인프라를 구축 비용 대비 효용 낮아 폐기
- **plan-12 Feature Flag Cleanup 전체 삭제**: flag 자체를 없애므로 cleanup 대상도 소멸
- **plan-00d Smoke Eval 부분 삭제**: `eval/golden-sets/smoke/`, `eval/judges/j1-*`, `eval/scripts/smoke.py` 등 5개 golden + J1 초안 폐기. Micrometer `AiCallMetrics` + `OBSERVABILITY.md` + Runtime State 캐시 메트릭만 유지 → `plan-00d-observability.md` 로 개명
- **STAGING_QUALITY_CHECKLIST v2 개정**: G4(레벨 밴드) / G5(Judge 일치율) / 10건 라벨링 절차 / J3 교차 검증 전부 삭제. G1~G3 자동 게이트만 유지. 제목 "Cut-over Gate" → "Automated Validation". 정성 비교는 `MANUAL_AB_PROTOCOL.md` 에 위임
- **`MANUAL_AB_PROTOCOL.md` 신규**: ECR 이미지 2개(before/after) 를 2개 포트(8081/8082) 에 병렬 기동 → 동일 녹화본 3~5건 투입 → JSON diff 수동 비교. 각 plan PR 머지 전 실행
- **Feature Flag runtime toggle 전면 제거**: plan-01/03/04/05/07/08/09/11/13 전반의 `rehearse.features.*` 언급 삭제. S2(plan-00b) 에서 이미 머지된 `AiFeatureProperties` / `@RefreshScope` / `/actuator/refresh` / `spring-cloud-context` 의존성은 **PR B (`[BE] refactor(ai): Feature Flag runtime toggle 철거`)** 에서 별도 철거. `ChatRequest.modelOverride` 는 모델 선택 자체 가치로 유지. 롤백 수단은 ECR 이미지 태그 + 세션 스토어 캐시 퍼지로 일원화
- **requirements.md Goal 표 개정**: Judge 기반 지표(J1/J2/J3, Intent Accuracy 등) 를 "수동 비교 판정" 으로 교체. 객관 측정 가능 지표(토큰, 캐시 히트율, 매핑 정확도) 는 유지
- **이유**: "신버전이 구버전보다 진짜 나아졌는가" 판정은 ECR 2개 병렬 기동 + 수동 diff 3~5건 으로 충분하며, 자동 Judge / runtime flag 는 본체 복잡도 상승 대비 이득이 부족. Grafana APM 은 회귀의 "정량" 감지에 유지

### 2026-04-22 (plan-13 신규 — Lambda Content Removal / Content·Delivery 책임 경계 확정)

`lambda/analysis/analyzers/gemini_analyzer.py` 가 단일 프롬프트로 `verbal`(답변 구조), `technical`(정확성/코칭), `vocal`, `attitude`, `overall` 5개 블록을 생성 중이며, FE `content-tab.tsx` 가 이를 가공 없이 렌더 중임을 확인. plan-08 Rubric Scorer 도입 시 **같은 답변을 Gemini + Rubric 이 이중 LLM 호출로 평가**하는 구조가 굳어질 위험 확인.

- **구조적 문제**: Gemini 는 `questionSetCategory` / `intentType` / `resumeMode` / `currentChainLevel` / resume 체인 컨텍스트를 받지 않아 레벨·의도 기준 정확성 판정 원천 불가. Rubric D1~D10 중 D2/D3/D4/D6 4차원이 Lambda `verbal`+`technical` 과 중복. Lambda `verbal` 블록 6개 축(용어 정확, 수치 구체, 논리 구조, 주제 이탈, 분량, 전달 명확성) 전부 D3/D4/D6 로 흡수됨 → 고유 가치 없음.
- **결정**: Lambda = Delivery Analyzer (AV-grounded only, `transcript`+`vocal`+`attitude`+`vision`+`overall_delivery`), Rubric = Content Analyzer 단독. 이중 평가 금지.
- **사용자 결정**: (1) 빠른 전환 (dual-read 단계 생략, plan-08/09 flag-off 배포 + 스테이징 품질 검수 후 flag-on 과 동시에 Lambda content 제거), (2) `verbal` 블록 완전 제거 (D3가 흡수), (3) DB 컬럼 바로 drop (V28, 과거 인터뷰 Content 탭은 "데이터 없음" 허용).
- **신규 plan-13 Lambda Content Removal 생성** — cut-over 시 Lambda 프롬프트·handler 정리, Backend DTO/Entity/Mapper 제거, FE content-tab 재설계, V28 migration 드롭, 플래그 on 동시 적용.
- **plan-08 개정**: Why 에 "Content 평가 유일 소스" 명시 + plan-13 연계 섹션 추가. 본 plan 범위를 "기술 내용 루브릭(D1~D10) 만" 으로 명시.
- **plan-09 개정**: 입력 스키마 `VERBAL_ANALYSIS` → `DELIVERY_ANALYSIS` 개명, `TURN_SCORES[].status: OK|FAILED` 필드 추가, `overall.coverage` 출력 필드 추가 (Rubric 실패 투명성), 작문 원칙에 "Content/Delivery 소스 엄격 분리" 강제 추가 (정규식 검증), Delivery 섹션은 delivery_analysis 에서만 / Content 섹션은 turn_scores 에서만 인용. cross-modal signal 은 `overall.narrative` 연성 관찰로만 허용 (차원 점수 수정 금지).
- **plan-11a 개정**: Out of Scope 에 "verbal/technical 블록 제거는 plan-13" 명시.
- **plan-11 개정**: 연계 섹션에 plan-13 / plan-08 경계 명시 (기술 D1~D10 vs 비언어 D11~D14 섞지 말 것).

이유: 이중 LLM 호출 구조가 굳기 전에 경계 확정. Rubric 품질 실패 시 Lambda fallback 이 존재하면 품질 드리프트가 은폐됨 → coverage 투명성과 fallback 금지로 품질 회복 루프 확보. 코드 수정 0건 — plan-13 구현 PR 에서 소화.

### 2026-04-22 (Resume Track 스펙 보완 — Dynamic Pacing + Exclusivity)

구현 착수 전 plan-05/06/07 Draft 허점 3개 차단:

- **plan-06 Dynamic Pacing 재설계**: duration 별 스케일링 테이블 폐기. Planner 는 모든 chain 을 priority 랭킹만 수행. `allocated_time_min` / `max_turns` / `estimated_duration_min` 필드 제거, `duration_hint_min` 만 남김 — opener 톤 조정에만 사용
- **plan-07 WRAP_UP 모드 추가**: `PLAYGROUND → INTERROGATION → WRAP_UP` 3단계 FSM. `ClockWatcher` 로 `remaining_time ≤ 2분` 전이. 새 chain 시작 금지, 현재 chain 완결 허용, 회고 질문 pool. `rehearse.features.resume-track.wrap-up-threshold-min` flag
- **plan-07 Resume Exclusivity Rule**: `resumeFile != null` 이면 `interviewTypes = {RESUME_BASED}` 강제. 위반 시 BE 400 + `RESUME_EXCLUSIVITY_VIOLATION`. FE 는 RESUME_BASED 선택 시 다른 카드 disabled + 자동 해제. Defense in depth
- **plan-05 최대 추출 원칙 명시**: Extractor 는 duration 무관 전체 Skeleton 추출 (이력서당 1회 비용 고정)
- **STATE_DESIGN.md `currentLevel` 의미 확정**: 사용자 레벨(junior/mid/senior), Chain level(L1~L4) 과 무관. Chain level 은 항상 `activeChain.size()` 로 도출. 혼동 금지 note 추가. `startedAt: Instant` 필드 추가 (ClockWatcher 용)

이유: 사용자 페이스 적응 불가 + 심층 체인 연속성 보호 + `currentLevel`/Chain level 오해석 위험을 구현 이전에 차단. 코드 수정 0건 — 각 plan 구현 PR 에서 소화.

### 2026-04-21 (S3 — plan-00c Session State Persistence 완료)

- Flyway V24~V27 마이그레이션 + rollback SQL 작성 (resume_skeleton / interview_plan / rubric_score / session_feedback)
- `InterviewRuntimeState` POJO — thread-safe 컬렉션(ConcurrentHashMap, CopyOnWriteArrayList, AtomicInteger) 기반 L3 런타임 상태 POJO
- `InterviewRuntimeStateStore` — Caffeine 2h idle TTL / max 10,000 / recordStats / Micrometer 메트릭(hits/misses/evictions) 노출
- `InterviewLockService` — 자체 구현 StripedLock(256 stripe ReentrantLock 배열). Guava 의존성 없음
- `build.gradle.kts` — `com.github.ben-manes.caffeine:caffeine` 의존성 추가
- `STATE_DESIGN.md` — 4계층 분류 + 결정사항(D1~D5) + 후속 plan 소비 방법 정식 문서화
- 신규 테스트 12개 추가: `InterviewRuntimeStateStoreTest`(7) + `InterviewLockServiceTest`(5)
- 결정: H2 호환 불필요 — test 프로파일은 Flyway disabled + create-drop. JSON 컬럼은 MySQL 전용
- 결정: StripedLock 자체 구현 채택 — Guava Striped 대비 외부 의존성 0, 구현 20줄 이내
- `./gradlew test` 전체 통과 확인 (baseline 606 + 신규 12 = 618 예상)

### 2026-04-21 (plan-00f 추가 — Interview Turn Policy Abstraction)

- `FollowUpTransactionHandler.MAX_FOLLOWUP_ROUNDS=2` 하드코딩이 plan-07 Resume 트랙(최대 7턴) 을 차단하는 설계 결함 발견
- `InterviewTurnPolicy` Strategy 도입 plan 작성: `StandardFollowUpPolicy` (CS/Language, 행위 무변경) + `ResumeTrackPolicy` skeleton + `InterviewTurnPolicyResolver`
- plan-07 의존성에 plan-00f 추가, `ChainStateTracker` 주입은 plan-07 에서 완성
- plan-00c 와 병렬 실행 가능 (W2)

### 2026-04-21 (검증 리포트 반영 — 문서 교정)

VERIFICATION_REPORT.md 작성 후 Critical/Major 문서 교정 적용:

- **A-F1 모델 드리프트 제거** (`plan-05`): `modelOverride="gpt-4o"` 하드코딩 → `application.yml` 기본 `gpt-4o-mini` 유지. flag 경유 선택적 업그레이드로 변경
- **B-F1 Aggregate Latency SLA 추가** (`plan-01/02/03/08`): 턴 파이프라인 aggregate p95 ≤ 4000ms 규약. plan-01 에 canonical 섹션 정의, plan-02/03 은 참조. Rubric Scorer(plan-08) 는 `@TransactionalEventListener(AFTER_COMMIT)` 비동기 post-turn 으로 명시 → SLA 제외
- **C-F1/F2/F3 실체 불일치 교정**: plan-03 `GeneratedFollowUp` 경로 확정, plan-06 `InterviewRuntimeState` 로 전환, plan-07 `FollowUpService`/`FollowUpServiceTest` 로 교정
- **D-F2 plan-11a 신규 생성**: Lambda Gemini 프롬프트 3개 수치 필드 확장(`speed_variance` / `gaze_on_camera_ratio` / `posture_unstable_count`). plan-11 의 `[blocking]` 선행 조건으로 편입. plan-11 본문에서 Lambda 프롬프트 확장 항목 → plan-11a 로 이관
- **D-F4 Lambda Error Handling** (`plan-09`): failure_reason → 처리 매트릭스 + admin 재시도 엔드포인트 + 사용자 "일시 오류" 배너 정책. 모든 실패는 `deliveryRetryable=true` 기본 (영구 실패 금지)

### 2026-04-20 (S1 — plan-00a Codebase Inventory 완료)
- `INVENTORY.md` (380L), `TEST_BASELINE.md` (249L), `IMPACT_MAP.md` (364L) 생성
- `./gradlew test` baseline: **606 tests / 0 failures / 0 ignored / 56s** (JaCoCo 미설정 — 추후 추가 권장)
- 주요 교정: `InterviewSession`/`InterviewTurnService` 실존 X → `Interview` entity + `FollowUpService.generateFollowUp(Long,Long,FollowUpRequest,MultipartFile):31` / `PdfTextExtractor` 기존 확장 / plan-07 `fact_check_flag` 삭제 대상 확정
- IMPACT_MAP 15개 plan (00b~11) 각각 신규/수정 파일 절대 경로 확정
- 다음 세션: S2 — plan-00b AiClient Generalization (`[BE] feat(ai): AiClient.chat() 범용 메서드 + @RefreshScope + JSON 파싱 재시도`)

### 2026-04-20 (초기 플래닝)
- `docs/todo/2026-04-20/` 7개 TODO 문서 분석 완료
- `docs/plans/interview-quality-2026-04-20/` 스펙 디렉토리 생성
- `requirements.md` 작성: Why/Goal/Evidence/Trade-offs + 4주 로드맵 + Out of scope 명시
- plan-01~10 (Phase 1~4) 초안 작성 완료
- **Critic 에이전트 리뷰** 실시 → 조건부 승인 판정 (Critical 3 + Major 6 + Missing 7)
- `REMEDIATION.md` 작성: 근본 원인별(RC1~RC7) 해결 전략
- **Phase 0 추가**: plan-00a~00e 5개 신규 (W1-W2 선행 배치)
- 로드맵 4주 → **7주 재산정** (critic M1 반영)
- `requirements.md` 로드맵 섹션 갱신
- 의존성 그래프 재정리

### 해결 체크리스트 (REMEDIATION.md 동기)
- [x] C1 AiClient 범용화 (00b) — chat(ChatRequest) 추가, 3개 도메인 메서드 어댑터 보존 (S2)
- [x] C2 DB 영속화/Flyway (00c) — V24~V27 + InterviewRuntimeStateStore + InterviewLockService (S3)
- [x] C3 호출별 모델 선택 (00b) — ChatRequest.modelOverride 지원 (S2)
- [ ] M1 7주 재산정 (이 문서)
- [x] M2 W1-W3 회귀 방어 (00d) — `OBSERVABILITY.md` 작성 (S3b, 2026-04-24). Grafana/PromQL 쿼리 7 종 + Alert 임계치 5 종 + 배포 회귀 감지 체크리스트
- [ ] M3 4-intent 확장 (OFF_TOPIC 분리, META 통합, LLM-free handler) — plan-01/02 문서 갱신 완료 (2026-04-24), 구현 대기
- [x] M4 실제 클래스명 정정 — plan-00a 인벤토리 완료 (S1). plan-01/07/08 본문 edit은 각 plan 실행 직전 해당 PR에 포함 (IMPACT_MAP 교정 사항 참조)
- [x] M5 Fallback 캐시 정책 (00b) — ResilientAiClient.fallbackChat() allowMiss=true 자동 적용 (S2)
- [x] M6 Feedback 관계 (00e) — `FEEDBACK_DOMAIN.md` 작성 (S3b, 2026-04-24). 병존 aggregate + partial-first + Admin API + InterviewCompletedEvent 신규 도입 결정
- [x] Missing PdfTextExtractor 재사용 — 기존 클래스 확인 (infra/ai/PdfTextExtractor.java, `extract(MultipartFile)`). IMPACT_MAP plan-05 수정 항목으로 기록
- [x] Missing APM 메트릭 표준 (00d) — Micrometer 태그 6 종(`call.type` / `model` / `provider` / `cache.hit` / `fallback` / `outcome`) + Caffeine 캐시 메트릭 5 종 문서화. 구현은 S2/S3 머지 완료
- [x] Missing Feature flag runtime — **의도적 제거 (2026-04-23)**. S2 에서 @RefreshScope/AiFeatureProperties 구현 완료됐으나 ECR 이미지 롤백으로 대체 결정 → PR B 에서 철거 예정
- [x] Missing 동시성 제어 (00c InterviewLockService) — StripedLock 256 자체 구현 (S3)
- [x] Missing JSON 파싱 폴백 (00b) — AiResponseParser.parseWithRetry() 추가 (S2)
- [x] ~~Minor plan-10 수동 라벨~~ — plan-10 전체 삭제 (2026-04-23)
- [ ] Addendum 비언어 루브릭 (plan-11) — TODO 09 반영. D11~D14 결정론 매퍼 + context_weights + V28
- [x] ~~Flag Cleanup (plan-12)~~ — flag 자체 제거로 plan-12 폐기 (2026-04-23)
