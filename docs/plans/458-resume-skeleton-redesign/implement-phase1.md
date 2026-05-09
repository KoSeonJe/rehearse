# Implement — Resume 표현력 인프라 (Phase 1, BE)

> **작성자**: 메인 세션 (`/create-implement-plan` 스킬)
> **답하는 질문**: 어떤 순서로 실행?
> **승인 게이트**: ★ 사용자 명시 승인 후 코드 작성 ★
> **연관**: `tech-spec.md` D4 = phase 분리 PR. 본 파일 = phase 1 단독. phase 2 (`implement-phase2.md`) = phase 1 머지 + 1주 관찰 통과 후 작성.

---

## Phase / Step 개요

| Phase | 제목 | 구현 에이전트 | 예상 PR | 의존 |
|-------|------|--------------|--------|------|
| 1 | Schema 재정렬 (record / DTO + Jackson 호환) | `backend` | #N | - |
| 2 | Extractor 갱신 (prompt + Live E2E) | `backend` | #N | Phase 1 |
| 3 | Planner / Validator / Rubric 재정렬 | `backend` | #N | Phase 1 |
| 4 | 통합 회귀 + 빌드 / grep | `backend` | #N | Phase 2, 3 |

> 분리 임계 (Task 8+ / 단일 Phase 50줄+) 미달 → 단일 파일 유지. 단일 PR 머지.

---

## Phase 1: Schema 재정렬 (record / DTO + Jackson 호환)

- **구현**: `backend` — record schema 변경 + 운영 row 양방향 호환

### 변경 파일

- `backend/src/main/java/com/rehearse/api/domain/resume/entity/ResumeClaim.java` — `claimId` / `depthHooks` 필드 제거. canonical constructor null/blank 검증 (`text` blank → throw, `claimType` null → throw, `priority` 음수 X).
- `backend/src/main/java/com/rehearse/api/domain/resume/entity/Project.java` — `techStack: List<String>` / `role: String` / `architecture: String` / `decisions: List<String>` 4종 추가. canonical constructor null-safe (List null → empty, String null → blank).
- `backend/src/main/java/com/rehearse/api/infra/ai/dto/ExtractedResumeSkeleton.java` — DTO record 동일 schema 갱신. `@JsonIgnoreProperties(ignoreUnknown = true)` class-level 적용.
- `backend/src/main/java/com/rehearse/api/domain/resume/entity/ResumeSkeleton.java` — Jackson 역직렬화 호환 검증 (필요 시 `@JsonIgnoreProperties(ignoreUnknown=true)` 추가).
- `backend/src/main/java/com/rehearse/api/domain/resume/entity/InterviewPlan.java` — `expectedClaimsCoverage` 의미 변경 (id → text), 시그니처 `List<String>` 동일. Jackson 역직렬화 호환 검증.

### 핵심 로직 / 변경 요약

```java
// ResumeClaim — claimId / depthHooks 제거 + null/blank 검증
public record ResumeClaim(String text, ClaimType claimType, int priority) {
    public ResumeClaim {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("ResumeClaim.text blank");
        if (claimType == null) throw new IllegalArgumentException("ResumeClaim.claimType null");
        if (priority < 0) throw new IllegalArgumentException("ResumeClaim.priority negative");
    }
}

// Project — 메타 4종 추가 + null-safe
public record Project(
    String projectId, String projectName,
    List<String> techStack, String role, String architecture, List<String> decisions,
    List<ResumeClaim> claims, List<ChainTopic> implicitCsTopics
) {
    public Project {
        techStack = techStack == null ? List.of() : List.copyOf(techStack);
        decisions = decisions == null ? List.of() : List.copyOf(decisions);
        role = role == null ? "" : role;
        architecture = architecture == null ? "" : architecture;
    }
}
```

운영 row 호환:
- 기존 row 의 `claim_id` / `depth_hooks` JSON 키 = `@JsonIgnoreProperties(ignoreUnknown=true)` 로 무시.
- 기존 row 의 `Project` JSON 에 `techStack` / `role` / `architecture` / `decisions` 부재 = canonical constructor 가 null-safe 처리.

### 의존
- 선행 phase: 없음
- 외부 의존: Jackson (이미 의존)

### Verification Hook
- 명령:
  ```bash
  ./gradlew test --tests "com.rehearse.api.domain.resume.entity.*"
  ```
- 통과 기준:
  - `ResumeClaim` text/claimType/priority 검증 케이스 모두 green
  - `Project` 메타 null-safe 케이스 모두 green
  - Jackson round-trip 테스트 (구버전 JSON → 신 record / 신 record → JSON) green

### 커밋 메시지 (예상)
```
feat(BE): ResumeSkeleton 메타 4종 추가 + claimId/depthHooks 폐기 (record schema)
```

---

## Phase 2: Extractor 갱신 (prompt + Live E2E)

- **구현**: `backend` — extractor prompt JSON schema 갱신 + 단독 Live E2E

### 변경 파일

- `backend/src/main/resources/prompts/template/resume/resume-extractor.txt` — instruction 변경:
  - 제거: `claim_id` 합성 / `depth_hooks` 추출 instruction
  - 추가: 메타 4종 (`techStack` / `role` / `architecture` / `decisions`) 추출 instruction
  - 추가: example block — `architecture` = 한 문장 (~80자), `decisions` element = `"X vs Y → X 채택, 사유 1줄"` 형식 가이드 (LLM 출력 변동성 ↓)
- `backend/src/test/java/com/rehearse/api/e2e/ResumeExtractorLiveLlmE2ETest.java` (신규 또는 기존 보강) — Live E2E 1건:
  - `@EnabledIfEnvironmentVariable(named = "RUN_LIVE_API", matches = "true")`
  - 단언: 추출 결과의 `techStack` / `role` / `architecture` / `decisions` 4종 모두 비어있지 않음 + `claim_id` / `depth_hooks` 부재 (DTO record 에도 잔존 0)
- `backend/src/test/java/com/rehearse/api/support/fixtures/ResumeFixtures.java` (보강) — fixture 1건 (백엔드 이력서, tech-spec Fixture sample 절 참조: Redis / 캐시 / Cache-Aside / TTL / MySQL)

### 핵심 로직 / 변경 요약

prompt JSON schema example:
```jsonc
{
  "projects": [{
    "projectId": "...",
    "projectName": "...",
    "techStack": ["Spring Boot", "Redis", "MySQL"],
    "role": "백엔드 단독 / API + 캐시 레이어 설계",
    "architecture": "Spring Boot REST API + Redis Cache-Aside + MySQL",
    "decisions": ["Memcached vs Redis → Redis 채택, TTL 정책 필요"],
    "claims": [{"text": "...", "claimType": "...", "priority": 1}],
    "implicitCsTopics": [...]
  }]
}
```

### 의존
- 선행 phase: Phase 1 (record / DTO schema)
- 외부 의존: GPT-4o-mini (LLM)

### Verification Hook
- 명령:
  ```bash
  RUN_LIVE_API=true ./gradlew test --tests "ResumeExtractorLiveLlmE2ETest"
  ```
- 통과 기준: 1 fixture × 1 run pass. 4종 메타 비어있지 않음 + claim_id / depth_hooks 부재.
- 관찰 가능 동작: extractor LLM 출력 JSON 에 메타 4종 등장 + 폐기 키 부재.

### 커밋 메시지 (예상)
```
feat(BE): resume-extractor prompt 메타 4종 추출 + claim_id/depth_hooks 폐기
```

---

## Phase 3: Planner / Validator / Rubric 재정렬

- **구현**: `backend` — claim 식별 text 기반 전환 + graceful drop 가드

### 변경 파일

- `backend/src/main/resources/prompts/template/resume/resume-interview-planner.txt` (line ~44, 55, 89) — instruction 변경:
  - 기존: "Skeleton 의 claim_id 사용" / "expected_claims_coverage = id 배열"
  - 신규: "Skeleton claims 의 text 만 사용. 새 합성 / paraphrase 금지" / "expected_claims_coverage = text 문자열 배열"
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeInterviewPlanValidator.java` — `validateClaimCoverage` 변경:
  - 기존: `claimId` Set 매칭 (line 58-67)
  - 신규: claim text 정확 매칭 → 실패 시 substring or Jaccard ≥ 0.6 → 실패 시 WARN 로그 + element drop (graceful, hard fail X)
  - chain validator (`projectId+topic` 결정적 합성) = 그대로 유지 (`ORPHAN_CHAIN`)
- `backend/src/main/java/com/rehearse/api/domain/resume/exception/ResumeErrorCode.java` — `ORPHAN_CLAIM` enum 정리 (drop 카운터 로그 키로 전환 또는 deprecate. surgical 룰 = 외부 사용처 grep 후 결정).
- `backend/src/main/java/com/rehearse/api/infra/ai/prompt/RubricScorerPromptBuilder.java` (line ~179) — rubric 본문 라벨 형식:
  - 기존: `[claimId] claim text`
  - 신규: `[1] claim text` / `[2] claim text` (1-based, project 내 `claims` 배열 순서)

### 핵심 로직 / 변경 요약

```java
// validateClaimCoverage — graceful drop
private void validateClaimCoverage(InterviewPlan plan, ResumeSkeleton skeleton) {
    Map<String, Set<String>> skeletonClaimsByProject = collectClaimTextsByProject(skeleton);
    int droppedClaimCount = 0;
    for (PlaygroundPhase phase : plan.playgroundPhases()) {
        Set<String> skeletonClaims = skeletonClaimsByProject.get(phase.projectId());
        if (skeletonClaims == null) continue;
        for (String expected : phase.expectedClaimsCoverage()) {
            if (!matchesAny(expected, skeletonClaims)) {
                droppedClaimCount++;
                log.warn("[ResumePlanValidator] claim coverage 매칭 실패 → drop: projectId={} expected={}",
                    phase.projectId(), expected);
            }
        }
    }
    if (droppedClaimCount > 0) {
        log.warn("[ResumePlanValidator] graceful drop 합계 droppedClaimCount={}", droppedClaimCount);
    }
}

// matchesAny: 정확 → substring → Jaccard ≥ 0.6
private boolean matchesAny(String expected, Set<String> skeletonClaims) {
    if (skeletonClaims.contains(expected)) return true;
    return skeletonClaims.stream().anyMatch(c ->
        c.contains(expected) || expected.contains(c) || jaccardSimilarity(expected, c) >= 0.6
    );
}
```

```java
// RubricScorerPromptBuilder — sequential idx
for (int i = 0; i < project.claims().size(); i++) {
    ResumeClaim claim = project.claims().get(i);
    sb.append("[").append(i + 1).append("] ").append(claim.text()).append("\n");
}
```

### 의존
- 선행 phase: Phase 1 (record schema 신규 — `ResumeClaim` claimId 부재)
- 외부 의존: 없음

### Verification Hook
- 명령:
  ```bash
  ./gradlew test --tests "ResumeInterviewPlanValidatorTest"
  ./gradlew test --tests "*RubricScorer*"
  ./gradlew test --tests "ResumeInterviewPlannerServiceIntegrationTest" # 또는 동등 신규
  ```
- 통과 기준:
  - validator: 정확 매칭 통과 / 부분 매칭 (substring + Jaccard) 통과 / 매칭 실패 graceful drop + WARN 로그 + `droppedClaimCount` 키 검증
  - rubric: sequential idx 라벨링 단언 (`[1]` / `[2]`) — Live LLM 점수 회귀 비교 X (사용자 결정 P1-E)
  - planner Service Integration: real `ResumeInterviewPlannerPromptBuilder` + AiClient Mock argument capture 로 prompt 에 "Skeleton claim text 만 사용" instruction 포함 검증. `verify(...)` 사용 X.

### 커밋 메시지 (예상)
```
feat(BE): planner prompt instruction Skeleton claim text 만 사용
feat(BE): ResumeInterviewPlanValidator text 매칭 + graceful drop
refactor(BE): RubricScorer 라벨 claimId → sequential idx
```

(또는 단일 통합 커밋: `feat(BE): Resume claim 식별 text 기반 전환 (planner / validator / rubric)`)

---

## Phase 4: 통합 회귀 + 빌드 / grep

- **구현**: `backend` — 빌드 / grep / Live E2E 회귀

### 변경 파일

- `backend/src/test/java/com/rehearse/api/e2e/ResumePlaygroundLiveLlmE2ETest.java` — 회귀 검증 (코드 변경 0 또는 어휘군 단언 보강 1건).
- `backend/src/test/java/com/rehearse/api/support/fixtures/ResumeFixtures.java` — fixture 2, 3 추가 (풀스택 / 임베디드, tech-spec Fixture sample 절 명세 기반 신규 작성).
- `backend/src/test/java/com/rehearse/api/e2e/ResumeExpressivenessLiveLlmE2ETest.java` (신규) — phase 1 산출물 어휘 회귀 (3 fixture × 어휘군 매칭, 3건 중 ≥2 통과).

### 핵심 로직 / 변경 요약

회귀 단언 패턴 (#435 model_answer Live LLM E2E 패턴 재사용):
- 어휘군 set + 동의어 매핑 (예: Redis ↔ {Redis, 인메모리 캐시}, TTL ↔ {TTL, 만료시간})
- 5개 어휘 중 ≥4 면접 산출물 등장 = fixture 통과
- 3 fixture × ≥2 fixture 통과 = 회귀 합격

운영 모니터링 가이드 (P2 흡수 — Rubric 감지 채널):
- rubric scoring 운영 점수 분포 = 별도 메트릭 / 대시보드 부재. **사용자 클레임 채널 + 회귀 fixture 재실행** 으로 사후 감지.
- validator graceful drop = WARN 로그 `droppedClaimCount=N` key=value. dev/prod docker log grep 으로 D4 통과 기준 (drop=0) 모니터링.

### 의존
- 선행 phase: Phase 2, 3
- 외부 의존: GPT-4o-mini (Live LLM)

### Verification Hook
- 명령:
  ```bash
  ./gradlew build
  grep -rn "depthHooks\|claimId" backend/src/main backend/src/main/resources/prompts \
    | grep -v "test/" | grep -v ".bak"
  RUN_LIVE_API=true ./gradlew test --tests "Resume*LiveLlm*"
  ```
- 통과 기준:
  - `./gradlew build` 통과
  - grep 결과 0건 (`depthHooks` / `claimId` 잔존 없음)
  - 기존 `ResumePlaygroundLiveLlmE2ETest` 회귀 통과
  - 신규 `ResumeExpressivenessLiveLlmE2ETest` 3 fixture × ≥2 통과
  - 표준 트랙 Live LLM E2E 회귀 통과 (재활용 분석기 영향 표면 최소)

### 커밋 메시지 (예상)
```
test(BE): Resume 표현력 인프라 통합 회귀 + Live E2E 어휘군 매칭 (3 fixture)
```

---

## 통합 Verification

전체 Phase 1-4 완료 판정. tech-spec.md Verification 섹션 참조.

- [ ] tech-spec.md Verification 항목 모두 통과 (Domain Unit / Service Integration / E2E Live LLM / 빌드 / 정적 / 회귀)
- [ ] **Rubric scoring 회귀 생략 (사용자 확정 P1-E)** — fixture 재채점 비교 X. 사후 운영 점수 분포 모니터링.
- [ ] D4 phase 진입 통과 기준 (별도 PR 단계, 본 phase 1 머지 후):
  - [ ] 운영 신규 row 의 validator graceful drop = 0건 (1주 관찰)
  - [ ] 운영 5xx 회귀 0건
  - [ ] 추출 결과 4종 메타 누락 row 0건

## 리뷰 게이트 (MANDATORY)

구현 완료 직후 지정 리뷰어 실행 강제. 스킵 = 위반.

- [ ] 지정 리뷰어 실행 (구현 완료 직후 — 메인 세션 책임)
  - BE only → `code-reviewer-backend`
- [ ] 컨벤션 위반 0건 (`backend/.claude/rules/conventions.md` + `testing.md`)
- [ ] Critical / Major 지적 = fix 반영 후 재리뷰
- [ ] Pre/Post State diff 일치 (tech-spec Pre/Post State 절 참조)
