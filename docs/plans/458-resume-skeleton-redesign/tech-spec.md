# Tech Spec — Resume 면접 표현력 재설계 + 답변 reactive 꼬리질문

> **작성자**: backend agent (메인 세션 위임 전 초안)
> **답하는 질문**: 어떻게? 구조 / 데이터 / Trade-off
> **승인 게이트**: ★ 사용자 명시 승인 후 implement.md 진입 ★

---

## Why → Goal (1줄 미러)

이력서 메타 (사용 기술 / 본인 역할 / 시스템 구성 / 결정 사유) 가 면접 산출물까지 살아남고, 응시자 답변에서 노출된 새 단서를 다음 질문이 인용하도록 한다. phase 2 = standard 트랙 답변 분석기 재활용. 산출물은 Live LLM E2E 회귀 + fixture 기반 어휘군 매칭으로 검증.

---

## Evidence

### 현재 구조 (확인)

- `domain/resume/entity/ResumeSkeleton.java` record: `(resumeId, fileHash, candidateLevel, targetDomain, projects, interrogationPriorityMap)`. 깊이 신호 메타 (사용 기술 / 본인 역할 / 시스템 구성 / 결정 사유) **부재**.
- `domain/resume/entity/Project.java` record: `(projectId, projectName, claims, implicitCsTopics)`. 메타 필드 부재.
- `domain/resume/entity/ResumeClaim.java` record: `(claimId, text, claimType, priority, depthHooks)`. `claimId` = LLM 합성 (extractor prompt 단계). `depthHooks` = extractor 가 생성하나 production read 0.
- `infra/ai/dto/ExtractedResumeSkeleton.java:52,61-62` — `claim_id`, `depth_hooks` JSON 매핑.
- `domain/resume/service/ResumeInterviewPlanValidator.java:23,42,58-63` — `claimId` 기반 plan↔skeleton 정합성 검증 (Set 매칭).
- `infra/ai/prompt/RubricScorerPromptBuilder.java:179` — `claim.claimId()` 를 rubric 본문 라벨 `[id]` 로 inject.
- `infra/ai/prompt/ResumeChainInterrogatorPromptBuilder.java:33-50` — chain interrogator build 시그니처 = `(projectName, chainTopic, currentLevel, answerQuality, userAnswer, consecutiveStayCount)`. **`AnswerAnalysis` 컨텍스트 미수신**.
- `domain/resume/service/InterrogationModeHandler.java:32-37,83` — `AnswerAnalysis analysis` 파라미터 이미 받지만 `analysis.answerQuality()` 외 unused.
- `domain/resume/service/ResumeInterviewOrchestrator.java:78-101` — `turnAnalysisPipeline.analyze(...)` 매 turn 호출 → standard `AnswerAnalyzer` 가 `claims / missingPerspectives / unstatedAssumptions / recommendedNextAction` 전부 산출 → `dispatchByMode` 로 전달. Phase 2 wiring = prompt 까지만.
- `domain/resume/entity/ResumeSkeletonEntity` = `skeleton_json` 컬럼 (JSON). **신규 필드 추가 = DDL 없음**.
- `domain/resume/entity/InterviewPlan` = `interview_plan_json` 컬럼 (JSON 추정).

### 외부 레퍼런스

- `backend/.claude/rules/conventions.md` — Flyway DDL only / `@Transactional` / Lombok / 로깅.
- `backend/.claude/rules/testing.md` — E2E 1건 + Live 1건 / Service Integration / `@DisplayName` 한국어 / TestFixtures.
- 인접 plan `435-resume-model-answer-quality` — 모범답변 fallback 톤 (현 브랜치). 본 plan = 표현력 인프라 + reactive 꼬리질문. **경계**: #435 의 fallback 본문 (`ResumeFallbackModelAnswers`) / opener 프롬프트 / responder 프롬프트 변경 0. 본 plan 은 extractor / planner / chain interrogator prompt + record schema 만 손댐.

### 사용자 발화 (특정 결정 근거)

- "기존 꼬리질문에서 그 정보를 이력서 스켈레톤 정보만 주면 될걸? 재활용 좋다" → phase 2 = standard `AnswerAnalyzer` 재활용 확정.
- "phase0은 하지마" → 보이스 검증 단계 제거.
- "비동기란? 같은 일반론 OK, 거기서 끝나는게 아쉽다" → 일반론 prompt 자체 제거 비목표.

### 추정 / 미확인 가정

- (추정) 운영 Skeleton row 누적 ~수백 건 미만. backfill 비용 < 재추출 비용. 사용자 확인 필요.
- (추정) `ResumeSkeletonEntity` JSON 역직렬화 = Jackson `@JsonIgnoreProperties` 또는 default unknown=ignore. 신규 필드 부재 row 호환 가능. 마이그레이션 절에서 재확인.
- (가정) `RubricScorerPromptBuilder` 의 `[claimId]` 라벨 제거 후 `[1]/[2]` sequential idx 로 대체해도 LLM rubric 평가 영향 미미. 회귀 fixture 비교 생략 (사용자 확정). 사후 운영 점수 분포 모니터링.

---

## Trade-offs

### Option A (채택): standard `AnswerAnalyzer` 재활용 + Skeleton 메타 확장

- 장점:
  - Resume orchestrator 가 이미 `turnAnalysisPipeline.analyze` 호출 → wiring 확장 폭 최소.
  - Phase 2 변경 사이즈 1/3 (전용 신설 대비). 단일 분석기 = 회귀 표면 좁음.
  - Skeleton JSON 컬럼 → 메타 필드 추가 시 DDL 없음.
- 단점:
  - standard analyzer 가 resume 도메인 시그널 (이력서 어휘) 모름. 인용 정합성 = chain interrogator prompt 단계에서 Skeleton 컨텍스트 + analysis 결과 동시 inject 로 확보 필요.
  - claimId LLM 합성 제거 시 rubric / validator 양쪽 동시 변경.
- 사유:
  - 사용자 명시 결정 (재활용 우선).
  - YAGNI — resume 전용 분석기 신설은 standard 재활용 검증 후 부족분 한정 보강.
  - Phase 2 = chain interrogator prompt 의 ANSWER_ANALYSIS + RESUME_SKELETON 두 컨텍스트로 충분.

### Option B (폐기): resume 전용 답변 분석기 신설

- 장점: resume 도메인 어휘 (프로젝트명 / 사용 기술) 직접 인지.
- 단점:
  - 신설 분석기 1개 + 전용 prompt template 1개 + DTO record 1개 + Service Integration / Domain Unit / Live E2E 테스트 세트 = 변경 파일 ≥ 12 (standard analyzer 재활용 = 변경 파일 ~5).
  - resume / standard 양 분석기 결과 schema 분기점 발생 (`AnswerAnalysis` vs `ResumeAnswerAnalysis`) → orchestrator / handler / prompt builder 도메인별 분기 = 코드 중복.
  - 분석기 2개 LLM 호출 변경 시 양쪽 회귀 동시 검증 필요. 회귀 표면 ↑.
- 폐기 사유: 사용자 결정 + YAGNI. resume 도메인 어휘 인식은 chain interrogator prompt 의 RESUME_SKELETON_CONTEXT inject 로 충분.

### Option C (폐기): claimId 결정적 해시 (text → SHA1[:8])

- 장점: 식별자 형태 유지 → rubric/validator 코드 변경 폭 ↓.
- 단점: text 변경 시 id 변동. 안정적 식별 어려움 본질 미해결.
- 폐기 사유: text 매칭 (Option A 일부) 가 단순. 식별자 자체 폐기.

---

## Architecture

### Phase 1: 표현력 인프라

```
[ResumeExtractor LLM]
    ↓ extract (prompt template 갱신)
[ExtractedResumeSkeleton DTO]
    ├ Project: + techStack, + role, + architecture, + decisions  (신규 메타)
    ├ ResumeClaim: - claimId, - depthHooks                        (제거)
    └ Project.implicitCsTopics: 그대로
    ↓ map to record
[ResumeSkeleton] (record)
    ↓ persist as skeleton_json
[ResumeSkeletonEntity.skeletonJson] (JSON 컬럼, DDL 없음)

[ResumeInterviewPlanner LLM]
    ↓ generate (prompt instruction: skeleton claim text 만 사용)
[GeneratedInterviewPlan DTO]
    └ playground_phase.expected_claims_coverage = List<String> (skeleton claim text 그대로)
    ↓ map to record
[InterviewPlan]
    ↓ ResumeInterviewPlanValidator
       ├ validateChainReferences (유지 — chainId 결정적 합성)
       └ validateClaimCoverage (text 매칭 + graceful drop, 신규 룰)
```

영향 영역 (5곳):
1. **Extractor DTO / record**:
   - `ExtractedResumeSkeleton` DTO: `depth_hooks` / `claim_id` 필드 제거 + 메타 필드 4종 추가.
   - `ResumeClaim` record: `claimId`, `depthHooks` 제거.
   - `Project` record: `techStack`, `role`, `architecture`, `decisions` 추가.
2. **Extractor prompt** (`resume-extractor.txt`): depth_hooks / claim_id instruction 제거 + 메타 4종 추출 instruction + 1요소 형식 example 추가 (architecture 한 문장 / decisions element = "X vs Y → X 채택, 사유").
3. **Planner prompt** (`resume-interview-planner.txt:44,55,89`): "Skeleton 의 claim_id 사용" → "Skeleton claims 의 text 만 사용. 새 합성 / paraphrase 금지" 로 변경. `expected_claims_coverage` 출력 = text 문자열.
4. **Validator** (`ResumeInterviewPlanValidator`): claim text 정확 매칭 + 부분 매칭 (substring or Jaccard ≥ 0.6) graceful 룰. 매칭 실패 = WARN 로그 + element drop (면접 진행 막지 않음). hard fail = chain 만 (기존 `ORPHAN_CHAIN`). `ORPHAN_CLAIM` 은 fail 대신 drop 카운터 로그로 전환.
5. **Rubric** (`RubricScorerPromptBuilder:179`): `[claimId]` → sequential `[i]` (1-based, project 내). claims 배열 순서 의존.

**runtime 영향 (변경 0)**: `PlaygroundModeHandler:75-82` 가 받는 `expectedClaims` = 이미 `List<String>` → 의미만 변동 (id → text). 코드 변경 0.

**가드 보강 사유** (사용자 결정 D1): planner LLM 이 이력서에 없는 claim text 자유 합성 = 면접관이 응시자가 안 한 일 물어봄 위험. validator 유지 + text 매칭 + graceful drop 으로 1차 차단. planner prompt 도 instruction 강화 = 2중 가드.

### Phase 2: reactive 꼬리질문 (standard 재활용, chain interrogator only)

```
[ResumeInterviewOrchestrator.processUserTurnInternal]
    ↓ turnAnalysisPipeline.analyze(..)        (이미 호출 중, 변경 없음)
[AnswerAnalysis] = claims + missingPerspectives + unstatedAssumptions
                 + answerQuality + recommendedNextAction
    ↓ dispatchByMode → interrogationHandler.handle(.., analysis, skeleton, ..)
[InterrogationModeHandler.handle]
    ↓ resultGenerator.generateInterrogation(.., analysis, skeleton, ..)   (신규 인자)
[ResumeQuestionResultGenerator.generateInterrogation]
    ↓ chainInterrogatorPromptBuilder.build(.., analysis, skeletonProjectMeta, ..)
[ResumeChainInterrogatorPromptBuilder.build]
    ↓ FocusHints.ResumeChainInterrogatorHints (필드 확장)
    ↓ executeJson with prompt
[resume-chain-interrogator.txt] prompt
    + ANSWER_ANALYSIS block (claims / missing / assumptions / next_action)
    + RESUME_SKELETON_CONTEXT block (project meta 4종 from Skeleton)
    + 기존 chainTopic / level / consecutiveStay
    ↓
[InterrogationResult]: 다음 질문이 응시자 답변 어휘 인용
```

**inject 범위 (사용자 결정)**: chain interrogator only. opener / playground responder = 변경 없음.
- 사유: opener = 답변 이전 첫 질문 / playground = 워밍업 (이미 expectedClaims inject 중) / interrogator = 심화 단계 답변 깊이 추적 = phase 2 핵심.

영향 영역 (5곳):
1. `ResumeChainInterrogatorPromptBuilder.build` 시그니처 확장 — `AnswerAnalysis analysis`, `ProjectMeta skeletonProjectMeta` 추가.
2. `FocusHints.ResumeChainInterrogatorHints` (record) 필드 추가 — analysis 4종 (claims/missing/assumptions/nextAction) + 메타 4종 (techStack/role/architecture/decisions).
3. `FocusLayer.java:45` 의 `ResumeChainInterrogatorHints` 렌더러 확장 — 신규 필드 직렬화 + cap 적용.
4. `ResumeQuestionResultGenerator.generateInterrogation` 시그니처 확장 — 호출 site 1곳 (`InterrogationModeHandler.handle:51-56`).
5. `InterrogationModeHandler.handle` 호출 시 `analysis` 그대로 전달 (이미 보유) + `skeleton` 의 project meta 추출 후 전달.

prompt template `resume-chain-interrogator.txt` 갱신: ANSWER_ANALYSIS 블록 + RESUME_SKELETON_CONTEXT 블록 추가 + instruction "응시자 답변 claims / missing 단서 활용해 다음 질문 작성, skeleton 메타 어휘 인용 권장".

---

## Data Model

### 스키마 변경

**없음**. `resume_skeleton.skeleton_json` / `interview_plan.plan_json` 컬럼이 JSON 형태 → record 필드 변경은 DDL 불필요.

### Record 변경 (Java)

```java
// AS-IS
public record Project(
    String projectId,
    String projectName,
    List<ResumeClaim> claims,
    List<ChainTopic> implicitCsTopics
) {}

// TO-BE
public record Project(
    String projectId,
    String projectName,
    List<String> techStack,        // 신규 — 사용 기술
    String role,                    // 신규 — 본인 역할
    String architecture,            // 신규 — 시스템 구성
    List<String> decisions,         // 신규 — 결정 사유
    List<ResumeClaim> claims,
    List<ChainTopic> implicitCsTopics
) {}

// AS-IS
public record ResumeClaim(
    String claimId,
    String text,
    ClaimType claimType,
    int priority,
    List<String> depthHooks
) {}

// TO-BE
public record ResumeClaim(
    String text,
    ClaimType claimType,
    int priority
) {}
```

### 운영 데이터 호환

- Jackson 역직렬화: `@JsonIgnoreProperties(ignoreUnknown = true)` 로 기존 row 의 `claim_id` / `depth_hooks` 무시. 신규 메타 필드는 기존 row 에서 `null` / 빈 list (canonical constructor null-safe 처리).
- 운영 Plan row 의 `expectedClaimsCoverage` 가 LLM 합성 claimId 보유 → claimId 폐기 후 매칭 불가. **마이그레이션 정책 = 사용자 결정 (아래 분기 결정 절)**.

---

## API Contract

### 외부 노출 영향

**없음**. 본 plan = BE 단독. Resume 면접 endpoint (`POST /api/v1/interviews/{id}/follow-up` 등) 의 request / response DTO 시그니처 변경 없음. Skeleton / Plan 은 서버 내부 도메인.

FE 노출 여부 확인 = implement 진입 시 1차 task 로 grep 확정.

---

## Verification (완료 판정)

`backend/.claude/rules/testing.md` 매핑.

### Domain Unit (≥ 60%)

- [ ] `ResumeClaim` record canonical constructor: text null/blank 검증.
- [ ] `Project` record canonical constructor: techStack/decisions null → empty list, role/architecture null → blank 처리.
- [ ] `ResumeInterviewPlanValidator`:
  - chain reference 정합성 (`ORPHAN_CHAIN`) 회귀 통과.
  - claim coverage text 매칭 — 정확 매칭 통과 / 부분 매칭 (substring) 통과 / 매칭 실패 시 WARN 로그 + drop (hard fail X) 검증.

### Service Integration (20-25%)

- [ ] **Phase 2 prompt argument capture**: `InterrogationModeHandler` → real `ResumeQuestionResultGenerator` → real `ResumeChainInterrogatorPromptBuilder` → **AiClient (Mock)**. Mock argument capture 로 LLM 에 전달된 user message 에 ANSWER_ANALYSIS 블록 (claims / missingPerspectives / unstatedAssumptions / nextAction 4종) + RESUME_SKELETON_CONTEXT 블록 (techStack / role / architecture / decisions 4종) 포함 검증. `verify(...)` 사용 X — message body 단언만.
- [ ] **Phase 1 planner prompt**: `ResumeInterviewPlanner` → real planner prompt builder → AiClient (Mock). Mock argument capture 로 prompt 에 "Skeleton claim text 만 사용" instruction 포함 검증.

### E2E Live LLM (≤ 5%, `@EnabledIfEnvironmentVariable RUN_LIVE_API=true`)

- [ ] **fixture 3+ 건** (서로 다른 이력서: 백엔드/풀스택/임베디드) × Live LLM:
  - phase 1 추출 회귀: 추출 결과 JSON 의 techStack/role/architecture/decisions 4종 모두 비어있지 않음 + claim_id/depth_hooks 부재.
  - phase 1 산출물 어휘: 면접 산출물 (질문 또는 모범답변) 에 이력서 메타 어휘군 포함 ≥ 80% (3건 중 ≥ 2건 통과).
  - phase 2 reactive: 응시자 답변에 사전 chain 외 어휘 inject 한 시나리오에서 다음 chain interrogator 질문이 해당 어휘군 인용 (3건 중 ≥ 2건 통과).
  - LLM 비결정성 완충 = 어휘군 매칭 (정확 어휘 + 동의어 set).

### Fixture sample (Verification 합의 게이트)

```
fixture 1 — 백엔드 이력서:
- 이력서 어휘 set: {Redis, 캐시, Cache-Aside, TTL, MySQL}
- 동의어 매핑: Redis ↔ {Redis, 인메모리 캐시}, TTL ↔ {TTL, 만료시간}
- phase 1 통과 임계: 5개 어휘 중 4개 이상 면접 산출물에 등장
- phase 2 답변 inject: "처음엔 Memcached 도 검토했지만 TTL 정책 필요해서 Redis 채택"
- phase 2 통과 임계: 다음 질문에 {Memcached, TTL 정책} 중 1개 이상 인용
```

implement 단계 1차 task = fixture 2,3 명세 + TestFixtures 추가.

### 빌드 / 정적

- [ ] `./gradlew build` 통과.
- [ ] `depthHooks` / `claimId` 잔존 grep 0건 (코드 + prompt template + DTO).
- [ ] ArchUnit 룰 통과.

### 회귀

- [ ] 기존 resume track Live LLM E2E (`ResumePlaygroundLiveLlmE2ETest` 등) 통과.
- [ ] standard 트랙 Live LLM E2E 회귀 통과 (재활용 분석기 동일 — 영향 표면 최소).
- [ ] Extractor 단독 Live E2E (fixture 1건) — 4종 메타 출력 + claim_id/depth_hooks 미출력.

**Rubric scoring 회귀 생략 (사용자 확정)**: `[claimId]` → `[1]/[2]` sequential idx 변경은 라벨 형식 변경만. AI 평가에 미세 영향 가능하나 점수 회귀 fixture 비교 생략. 사후 운영에서 점수 분포 이상 발견 시 별도 plan 으로 대응.

---

## Pre / Post State

### Pre (현재)

```
ResumeSkeleton meta: candidateLevel + targetDomain + projects (claims + implicitCsTopics) + priorityMap
Project meta: projectId + projectName + claims + implicitCsTopics  (사용 기술 / 역할 / 구성 부재)
ResumeClaim: claimId (LLM 합성) + text + claimType + priority + depthHooks (read 0)
InterviewPlan.playground_phase.expected_claims_coverage: List<claimId>  (skeleton cross-ref)
chain interrogator prompt: chainTopic + level + answerQuality + userAnswer  (analysis / skeleton 컨텍스트 부재)
RubricScorerPromptBuilder: [claimId] 라벨 inject
ResumeInterviewPlanValidator: chainId 매칭 + claimId Set 매칭
운영 Skeleton row: claim_id / depth_hooks 보유
운영 Plan row: expected_claims_coverage = LLM 합성 id 보유
```

### Post (구현 후)

```
ResumeSkeleton meta: 동일
Project meta: + techStack, + role, + architecture, + decisions
ResumeClaim: text + claimType + priority  (claimId / depthHooks 삭제)
InterviewPlan.playground_phase.expected_claims_coverage: List<String> (claim text 직접 embed, self-contained)
chain interrogator prompt: + ANSWER_ANALYSIS (claims/missing/assumptions/next_action) + RESUME_SKELETON_CONTEXT (project meta) + 기존
RubricScorerPromptBuilder: [1]/[2]/... sequential idx
ResumeInterviewPlanValidator: chainId 매칭 (유지) + claim text 매칭 (신규, graceful drop)
운영 Skeleton row: claim_id / depth_hooks 잔존 → Jackson ignore-unknown 으로 무시
운영 Plan row: 기존 id 문자열 = validator graceful drop → playground prompt 가이드 일부 잃음 (LLM fatal X). 신규 면접부터 신규 추출/plan 적용
```

---

## 위험 / 마이그레이션 / 롤백

### 위험

1. **운영 Plan row 영향 — 진행 중 면접** — 기존 Plan row 의 `expectedClaimsCoverage` = LLM 합성 id 문자열 (예: `__example_proj___c1`). validator text 매칭 도입 시 이 row 들은 매칭 실패 → graceful drop → playground prompt 에 빈 `expectedClaims` inject. 면접관 prompt 가 일부 가이드 잃음 (LLM 동작에 fatal 영향 없음). 신규 면접 = 신규 Plan 추출 → 영향 0. **graceful drop 룰이 진행 중 면접 보호**.
2. **text hallucination 가드** — planner LLM 이 skeleton 외 자유 합성 시도 시 validator text 매칭 (정확 / substring) 으로 1차 차단 + planner prompt instruction 강화 = 2중 가드. graceful drop = 면접 진행 막지 않음.
3. **chain interrogator prompt token spike** — ANSWER_ANALYSIS + RESUME_SKELETON_CONTEXT inject 로 input 토큰 ↑. MVP 단계 사용자 부재 = 운영 영향 미미 가정 → **사전 게이트 폐기 (사용자 결정)**. spike 측정도 별도 task 화 X. 운영 cap 초과 시 5xx 발생하면 사후 대응 (prompt 재설계). 신규 inject 로 인한 latency 영향은 회귀 Live E2E 통과 여부로 간접 관찰.
4. **Live LLM E2E 비결정성** — 어휘군 매칭 + N-of-M 통과 룰로 완충 (#435 패턴 재사용).
5. **standard analyzer 재활용 — resume 도메인 어휘 인지 부족** — standard analyzer 는 일반 면접 분석 prompt 사용. resume 어휘 인용 정합성은 chain interrogator prompt 의 RESUME_SKELETON_CONTEXT inject 에 전적 의존. fixture 검증 필수.

### NF 11 점검

| NF | 영향 / 결정 |
|----|------------|
| 영향 범위 | BE 단독. FE 시그니처 영향 0 (Skeleton/Plan = 내부 도메인). |
| 정합성 | 트랜잭션 강함 — extractor / planner 결과 persist = 기존 트랜잭션 경계 유지. JSON 컬럼 단일 row. |
| 실시간성 | 면접 turn 응답 시간 = chain interrogator LLM 호출 latency 포함. 신규 inject 로 input token ↑ → latency 미세 ↑ 가능. MVP 사용자 부재 → 사전 측정 생략, 회귀 Live E2E 로 간접 관찰. |
| 부하 | 매 turn LLM 호출 = 기존과 동일 (호출 수 증가 X, prompt 크기만 ↑). MVP 단계 비용 측정 생략. |
| **동시성** | 동일 사용자 동시 재추출 race = `ResumeSkeletonPersister` / `ResumeInterviewPlanner` 기존 락 동일 (변경 없음). 신규 동시성 영역 없음. |
| 마이그레이션 | DDL 0. JSON ignore-unknown 양방향. 운영 Plan row = graceful drop 으로 진행 중 면접 보호. |
| **외부 의존** | extractor LLM / planner LLM / chain interrogator LLM 3종 prompt 동시 변경. 모델 / temperature / max-tokens 설정 = `application-*.yml` 그대로 유지 (변경 0). 회귀 표면 = prompt 레벨. |
| 보안 | OWASP A03 (Injection) — prompt template 내 `expected_claims_coverage` text 가 LLM 출력 → 다음 prompt inject. text sanitize 불필요 (LLM ↔ LLM, 사용자 입력 직접 X). 민감정보 로깅 X. |
| **관찰성** | token 사용량 메트릭 = MVP 단계 측정 생략. 운영 알림 신설 X. validator graceful drop 발생 = WARN 로그 + drop count 키-값 (`droppedClaimCount=N`) 명시 (D4 통과 기준 모니터링 근거). |
| 롤백 | 코드 revert 단일 PR. 운영 row 영향 0 (ignore-unknown 양방향). |
| 검증 | testing.md 매핑 — Domain Unit (record / validator) / Service Integration (prompt argument capture) / E2E Live LLM (fixture 3+) / Extractor 단독 Live E2E (1건). |

### 마이그레이션 전략

- **DDL 없음** — Flyway V47 추가 X.
- **JSON 역직렬화 호환** — `ExtractedResumeSkeleton` / `ResumeSkeleton` / `GeneratedInterviewPlan` 레코드에 `@JsonIgnoreProperties(ignoreUnknown=true)` 명시. 기존 row 의 `claim_id` / `depth_hooks` 자동 무시.
- **운영 row 정책 (사용자 결정 D2)** — deprecate. 신규 면접 시점부터 신규 추출 / plan 적용. 기존 진행 중 면접 = graceful drop 으로 보호 (위험 1 참조).

### 컨벤션 매핑 (`backend/.claude/rules/conventions.md`)

- Flyway: DDL 0건 — DDL only 룰 무관.
- `@Transactional`: 신규 트랜잭션 경계 0. validator / handler / generator = 기존 경계 유지.
- Lombok: 신규 Lombok 사용 0. 기존 record / `@Slf4j` 그대로.
- 로깅: 신규 로그 = validator graceful drop WARN 1곳 (한국어 + `droppedClaimCount` key=value). 민감정보 X.
- Entity 직접 반환 X. 신규 Response DTO 0.

### 롤백

- phase 1 PR revert: 신규 메타 필드 / 신규 validator 룰 / planner prompt / extractor prompt / rubric 라벨 동시 revert. 운영 row 영향 0 (ignore-unknown 양방향).
- phase 2 PR revert: chain interrogator prompt builder / handler / generator 시그니처 / FocusHints / FocusLayer / prompt template 동시 revert.
- phase 1 / phase 2 단계 분리 머지 (D4 결정) → 각 PR 독립 revert 가능. blast radius 분리.

---

## 분기 결정 (사용자 확정)

### D1. claim 매핑 룰 — **validator 유지 + text 매칭 + graceful drop** (사용자 확정)

근본 검증: `expectedClaimsCoverage` runtime 소비처 = `PlaygroundModeHandler:75-82` 의 `List<String>` inject 1곳.

초안에서 "validator 자체 제거" 안 검토했으나, 사용자 우려 = "안 한 일 물어보면 안 된다". planner LLM text 자유 합성 위험 차단 위해 validator 유지 결정.

**채택**:
- `expected_claims_coverage` = claim text 직접 embed (id 폐기는 동일).
- validator `validateClaimCoverage` = text 정확 매칭 + 부분 매칭 (substring 또는 Jaccard ≥ 0.6) 으로 변경.
- 매칭 실패 = WARN 로그 + element drop (graceful, hard fail X). 면접 진행은 막지 않음.
- planner prompt 도 instruction 강화 ("Skeleton claim text 만 사용, 새 합성 / paraphrase 금지").
- chain validator (`projectId+topic` 결정적 합성) = 그대로 유지 (`ORPHAN_CHAIN`).
- `ORPHAN_CLAIM` enum = drop 카운터 로그 키로 전환 (또는 enum 유지 + 사용 안 함, surgical 룰 따라 implement 단계 결정).

### D2. 운영 row 정책 — **deprecate + 신규부터 적용** (사용자 확정)

기존 row 그대로 (Jackson `@JsonIgnoreProperties(ignoreUnknown=true)` 양방향 호환). 신규 면접 시작 시 새 추출 / plan 부터 신규 스키마 적용. 사용자 행동 강제 / batch 백필 없음.

### D3. Project 메타 필드 — **4종 분리** (사용자 확정)

```java
public record Project(
    String projectId,
    String projectName,
    List<String> techStack,
    String role,
    String architecture,
    List<String> decisions,
    List<ResumeClaim> claims,
    List<ChainTopic> implicitCsTopics
) {}
```

extractor prompt 도 4종 출력 instruction 추가. chain interrogator prompt 의 RESUME_SKELETON_CONTEXT 블록에 4종 inject.

추가 schema 명확화 (extractor prompt example block):
- `architectureSummary` (한 문장 ~80자) — 현 record 는 `architecture: String` 단일. example 로 1요소 형식 가이드.
- `decisions` element 형식 = `"X vs Y → X 채택, 사유 1줄"` (LLM 출력 변동성 ↓).

### D4. PR 분리 — **phase 1 단독 머지 → 1주 관찰 → phase 2** (사용자 확정)

- **phase 1 PR**: extractor / record / planner prompt / validator / rubric. 운영 row 영향 큼 → 단독 머지 후 회귀 관찰.
- **관찰 기간 1주**: validator drop count / 추출 결과 메타 필드 누락률 / Live E2E 회귀 / 운영 5xx 모니터.
- **phase 2 진입 통과 기준 (사용자 확정)**:
  - **신규 row validator graceful drop = 0건** (1주 관찰 동안). drop = LLM hallucination 직접 지표 → 0 = phase 1 prompt 안정 = phase 2 inject 신뢰 가능.
  - 운영 5xx 회귀 0건.
  - 추출 결과 4종 메타 (techStack/role/architecture/decisions) 누락 row 0건.
  - 미달 시 phase 1 prompt instruction 보강 후 관찰 연장. phase 2 PR 보류.
- **phase 2 PR**: chain interrogator prompt builder + analysis wiring + FocusHints/FocusLayer.
- 사유: blast radius 분리. phase 1 = LLM schema 변경 = 회귀 표면 큼. 단독 관찰 후 phase 2 안전.

---

## 분기 결정 (BE/FE)

- [x] **단일 영역 / phase 분리 PR** → `implement-phase1.md` + `implement-phase2.md` (BE only, PR 2단계).
- [ ] BE+FE 동시 — 해당 없음.
- [ ] BE 선행 강제 — 해당 없음.

implement.md 분리 (D4):
- `implement-phase1.md` = 표현력 인프라. PR1 머지 → 1주 관찰.
- `implement-phase2.md` = reactive 꼬리질문. phase 1 관찰 통과 후 진입.
