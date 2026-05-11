# Implement — App Service 역할 재정의 및 도메인별 점진 리팩토링

> **작성자**: 구현 agent (Claude)
> **답하는 질문**: 어떤 순서로 실행?
> **사용 시점**: 단일 영역 작업 (BE only — frontend / lambda 영향 없음)
> **승인 게이트**: ★ 사용자 명시 승인 후 코드 작성 ★
> **관련**: `docs/plans/450-app-service-role-redefine/product-spec.md`, `tech-spec.md` / Issue #450

---

## Phase / Step 개요

| Phase | 제목 | 구현 에이전트 | 예상 PR | 의존 |
|-------|------|--------------|--------|------|
| 1 | `conventions.md` 룰 보강 | `backend` | PR1 | - |
| 2 | `InterviewQueryService` → `InterviewService` 흡수 | `backend` | PR1 | Phase 1 (룰 기준) |
| 3 | `ResumeFinder` 신설 (read 전용) | `backend` | PR1 | Phase 1 |
| 4 | `ResumeInterviewOrchestrator` → `ResumeInterviewService` rename + `ensureInterviewPlan` | `backend` | PR1 | Phase 3 |
| 5 | `FollowUpService` 책임 추출 + cross-domain 의존 제거 | `backend` | PR1 | Phase 3, Phase 4 |
| 6 | Phase 2 Rollout — 7 도메인 일괄 | `backend` | PR2 (PR1 머지 후) | PR1 머지 |

> 단일 implement.md 유지. 6 Phase / 본문 50줄 미만 예상.

PR 분리:
- **PR1** (`feat/be/450-app-service-pilot`) = Phase 1~5 (룰 보강 + interview Pilot)
- **PR2** (`feat/be/450-app-service-rollout`) = Phase 6 (7 도메인 일괄)

---

## Phase 1: `conventions.md` 룰 보강

- **구현**: `backend` — 컨벤션 문서 보강 (코드 변경 없음, 룰 텍스트 input)

### 변경 파일
- `backend/.claude/rules/conventions.md` — 다음 4건 보강:
  1. "클래스 접미사" 표 (line 77-91) `*Finder` 행 추가
  2. "패키지 구조" 도메인 패키지 룰 (line 47) cross-domain 정책 강화 — `*Finder` 경유 강제, Repository / `*Persister` / `*RuntimeCache` / `*Planner` 직접 주입 금지
  3. "계층 책임" 표 (line 109-118) Finder 행 추가 — read 전용 cross-domain 진입점
  4. "Service 책임 분리" 섹션 신설 — App Service / Domain Service / `*Finder` 룰 + `*QueryService` 패턴 폐기 명시 + Before-After 예시 2쌍

### 핵심 로직 / 변경 요약
- tech-spec.md "Phase 0" 섹션 변경 위치 / 보강 내용 그대로 적용.
- Before-After 예시 1 = cross-domain Repository 직접 주입 → `*Finder` 경유 (코드 스니펫 5-10줄).
- Before-After 예시 2 = 거대 App Service 메서드 → Domain Service 추출 (코드 스니펫 5-10줄).

### 의존
- 선행: 없음
- 외부 의존: 없음

### Verification Hook
- 명령: 없음 (문서 변경)
- 통과 기준: 사용자 직접 검토 통과 (룰 단독 읽고 Phase 2~6 적용 가능 — product-spec G1)

### 커밋 메시지 (예상)
```
docs(BE): App Service 역할 분리 룰 + *Finder 접미사 컨벤션 보강
```

---

## Phase 2: `InterviewQueryService` → `InterviewService` 흡수

- **구현**: `backend` — `*QueryService` 패턴 폐기, read-only App Service 통합

### 변경 파일
- `backend/src/main/java/com/rehearse/api/domain/interview/service/InterviewService.java` — read API 4 메서드 (`getInterview`, `getInterviewByPublicId`, `getInterviews`, `getStats`) 흡수
- `backend/src/main/java/com/rehearse/api/domain/interview/service/InterviewQueryService.java` — 삭제
- `backend/src/main/java/com/rehearse/api/domain/interview/controller/InterviewController.java` — `InterviewQueryService` 참조 → `InterviewService` 로 전환
- `backend/src/test/java/com/rehearse/api/domain/interview/service/InterviewQueryServiceTest.java` (존재 시) — `InterviewServiceTest` 로 케이스 이동 또는 통합

### 핵심 로직 / 변경 요약
- `InterviewQueryService` 메서드 4개 시그니처 그대로 `InterviewService` 이동.
- `@Transactional(readOnly = true)` 메서드 단위 유지.
- `QuestionSetRepository` cross-domain 직접 주입 = Phase 6 Rollout 에서 `QuestionSetFinder` 경유로 전환 (현 Phase = 위치 이동만, 의존 정리는 Phase 6 합류).

### 의존
- 선행: Phase 1 (룰 기준 확보)
- 외부 의존: 없음

### Verification Hook
- 명령: `./gradlew test --tests "com.rehearse.api.domain.interview.*"`
- 통과 기준: interview 도메인 전체 테스트 green
- 관찰: `rg "InterviewQueryService" backend/src/` = 0건

### 커밋 메시지 (예상)
```
refactor(BE): InterviewQueryService 를 InterviewService 에 흡수
```

---

## Phase 3: `ResumeFinder` 신설 + `ResumeSkeletonCodec` 분리 (read 전용)

- **구현**: `backend` — resume 도메인 cross-domain read 진입점 신설 + JSON 변환 책임 분리

### 변경 파일
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeFinder.java` — 신규
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeSkeletonCodec.java` — 신규 (JSON ↔ VO 변환 책임)
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeSkeletonPersister.java` — Codec 위임으로 전환 (인라인 ObjectMapper 호출 제거)
- `backend/src/main/java/com/rehearse/api/domain/resume/repository/ResumeSkeletonRepository.java` — `existsByInterviewId` 메서드 추가 (존재 확인 시 Codec 미호출 경로)
- `backend/src/test/java/com/rehearse/api/domain/resume/service/ResumeFinderTest.java` — 신규 (Domain Unit)

### 핵심 로직 / 변경 요약
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumeFinder {
    private final ResumeSkeletonRepository resumeSkeletonRepository;
    private final ResumeSkeletonCodec resumeSkeletonCodec;
    private final InterviewPlanRepository interviewPlanRepository;
    private final InterviewPlanRuntimeCache interviewPlanRuntimeCache;

    public Optional<ResumeSkeleton> findSkeletonByInterviewId(Long interviewId) {
        return resumeSkeletonRepository.findByInterviewId(interviewId)
                .map(resumeSkeletonCodec::deserialize);
    }

    public boolean existsSkeletonByInterviewId(Long interviewId) {
        return resumeSkeletonRepository.existsByInterviewId(interviewId);
    }

    public Optional<InterviewPlan> findInterviewPlan(Long interviewId) {
        InterviewPlan cached = interviewPlanRuntimeCache.read(interviewId);
        if (cached != null) return Optional.of(cached);
        return interviewPlanRepository.findByInterviewId(interviewId);
    }
}
```
- **read 전용**. 생성 / 저장 / 캐시 write 흡수 금지 (Phase 1 룰).
- **InterviewPlanCodec 미신설**: `InterviewPlan` Entity `projectPlans` 필드는 `@Convert(ProjectPlanListJsonConverter.class)` 로 JPA 자동 직렬화 → 별도 Codec 불필요 (simplicity rule).
- `ResumeSkeletonCodec` 만 신설: `ResumeSkeletonEntity.skeletonJson` String 컬럼 ↔ `ResumeSkeleton` VO record 변환 필요.

### 의존
- 선행: Phase 1
- 외부 의존: 기존 `ResumeSkeletonRepository`, `InterviewPlanRepository`, `InterviewPlanRuntimeCache` (read API)

### Verification Hook
- 명령: `./gradlew test --tests "ResumeFinderTest"`
- 통과 기준: skeleton / plan 부재 시 `Optional.empty()` / 존재 시 entity 반환 / cache hit 케이스 검증
- 관찰: `ResumeFinder` 가 Persister / Planner write API 의존 = 0

### 커밋 메시지 (예상)
```
feat(BE): resume 도메인 ResumeFinder 신설 (cross-domain read 진입점)
```

---

## Phase 4: `ResumeInterviewOrchestrator` → `ResumeInterviewService` rename

- **구현**: `backend` — App Service 명명 컨벤션 일치 (자가복구 `ensureInterviewPlan` 미신설 / 폐기)

### 변경 파일
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeInterviewOrchestrator.java` → `ResumeInterviewService.java` rename
- 클래스 / 빈 이름 참조 전수 갱신:
  - `FollowUpService` 등 호출자 (grep 전수)
  - 테스트 / 로그 메시지
- `backend/src/test/java/com/rehearse/api/domain/resume/service/ResumeInterviewOrchestratorTest.java` → `ResumeInterviewServiceTest.java`

### 핵심 로직 / 변경 요약
- IDE refactor (rename) 사용. 자동 참조 갱신.
- **`ensureInterviewPlan` 메서드 미신설** — invariant 기반 fail-fast 채택:
  - `ResumePlanPreparationService.prepare()` 가 인터뷰 시작 전 plan 생성 보장 (선행 invariant).
  - 런타임 자가복구는 결함 은폐 가능성 ↑ → `FollowUpService` 가 plan 부재 시 `BusinessException(RESUME_PLAN_NOT_READY)` throw 로 표면화.
  - `InterviewRuntimeStateCache.getIfPresent(Long) → Optional` 추가 (cross-domain read 용). 기존 `get(Long)` = throw 유지 (자기 도메인 invariant).
  - `InterviewPlanRuntimeCache.read` = `getIfPresent` 사용 → state 미seed 시 `Optional.empty` 반환 (생성 흐름 호출 X).

### 의존
- 선행: Phase 3 (ResumeFinder 신설 후 read/write 경계 명확화)
- 외부 의존: 없음 (자가복구 폐기로 `ResumeInterviewPlanner` / `InterviewPlanPersister` 의존 신설 회피)

### Verification Hook
- 명령: `./gradlew test --tests "ResumeInterviewServiceTest"` + `rg "ResumeInterviewOrchestrator" backend/src/` = 0건
- 통과 기준: rename 후 전체 컴파일 + 기존 테스트 green
- 관찰: resume 도메인 manual smoke (Resume 트랙 인터뷰 1회)

### 커밋 메시지 (예상)
```
refactor(BE): ResumeInterviewOrchestrator → ResumeInterviewService rename
refactor(BE): ResumeInterviewService.ensureInterviewPlan 폐기 + 부재 시 BusinessException
```

---

## Phase 5: `FollowUpService` 책임 추출 + cross-domain 의존 제거

- **구현**: `backend` — Pilot 핵심. 거대 App Service 분해 + cross-domain `*Persister` / `*RuntimeCache` / `*Planner` 직접 주입 제거

### 변경 파일
- `backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpService.java` — 의존 13 → ~7, 책임 추출, `ResumeFinder` + `ResumeInterviewService` 경유로 전환
- `backend/src/main/java/com/rehearse/api/domain/interview/service/InterviewService.java` — cross-domain `QuestionSetRepository` / `ResumeSkeletonRepository` 제거 → `QuestionSetFinder` / `ResumeFinder` 경유 (`existsSkeletonByInterviewId` 사용)
- `backend/src/main/java/com/rehearse/api/domain/question/service/QuestionSetFinder.java` — 신규 (Phase 6 예정이었으나 PR1 InterviewService 정리 함께 선행)
- 신규 Domain Service:
  - `backend/src/main/java/com/rehearse/api/domain/interview/service/ResumeRoutePolicy.java` — `isResumeTrack` 로직
  - `backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpSkipHandler.java` — analyzer SKIP / Step B SKIP 공통 처리
  - `backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpResponseBuilder.java` — `buildAnswerResponse` (record 또는 utility)
- 테스트:
  - `backend/src/test/java/com/rehearse/api/domain/interview/service/ResumeRoutePolicyTest.java` — Domain Unit
  - `backend/src/test/java/com/rehearse/api/domain/interview/service/FollowUpSkipHandlerTest.java` — Domain Unit
  - `backend/src/test/java/com/rehearse/api/domain/interview/service/FollowUpServiceTest.java` — Service Integration (회귀 + 신규 분기: plan 부재 시 `BusinessException(RESUME_PLAN_NOT_READY)` throw)

### 핵심 로직 / 변경 요약
- `generateFollowUp()` 흐름 정리:
  1. audioFile 가드 (유지)
  2. context 로드 (followUpTransactionHandler 유지)
  3. runtime state init (runtimeStateStore 유지)
  4. Resume 라우팅 분기 (`ResumeRoutePolicy.isResumeTrack`)
  5. 분기 1 (Resume): `ResumeFinder.findSkeletonByInterviewId` + `ResumeFinder.findInterviewPlan` → empty 시 `BusinessException(RESUME_PLAN_NOT_READY)` throw (자가복구 X) → 정상 시 `ResumeInterviewService.processUserTurn`
  6. 분기 2 (표준): `audioTurnAnalyzer.analyze` → SKIP 분기 (`FollowUpSkipHandler`) 또는 `followUpQuestionWriter.write`
- 제거 의존: `resumeSkeletonStore`, `interviewPlanStore`, `resumeSkeletonCache`, `interviewPlanCache`, `resumeInterviewPlanner` (5개).

### 의존
- 선행: Phase 3 (ResumeFinder), Phase 4 (ResumeInterviewService.ensureInterviewPlan)
- 외부 의존: 없음

### Verification Hook
- 명령:
  - `./gradlew test --tests "FollowUpServiceTest"`
  - `./gradlew test --tests "ResumeRoutePolicyTest"`
  - `./gradlew test --tests "FollowUpSkipHandlerTest"`
  - `./gradlew build`
- 통과 기준: 모든 신규 / 회귀 테스트 green + 컴파일 / 린트 통과
- 관찰 / cross-domain 직접 주입 검증:
  - `rg "ResumeSkeletonPersister|InterviewPlanPersister|ResumeSkeletonRuntimeCache|InterviewPlanRuntimeCache|ResumeInterviewPlanner" backend/src/main/java/com/rehearse/api/domain/interview/` = 0건
- dev 환경 수동 시연: CS 트랙 / Resume 트랙 각 1회 인터뷰 완주

### 커밋 메시지 (예상)
```
refactor(BE): FollowUpService 책임 추출 + cross-domain 의존 ResumeFinder/Service 경유
```

---

## Phase 6: Phase 2 Rollout — 7 도메인 일괄 (PR2)

- **구현**: `backend` — 7 도메인 cross-domain Repository 직접 주입 제거 + `*Finder` 신설 + `*QueryService` 흡수

### 변경 파일 (도메인별)

#### resume
- `backend/.../resume/service/ResumeInterviewService.java` — `questionSetRepository` 제거 → `QuestionSetFinder` 경유
- `backend/.../resume/service/ResumeQuestionPersister.java` — cross-domain Repository 2건 제거 → 해당 도메인 Finder 경유
- `backend/.../resume/service/ResumeTurnEventPublisher.java` — `questionSetRepository` 제거 → `QuestionSetFinder` 경유

#### question
- `backend/.../question/service/QuestionFinder.java` — 신규
- `backend/.../question/service/QuestionSetFinder.java` — PR1 (Phase 5) 에서 선행 신설됨 (InterviewService cross-domain 정리 시점). Phase 6 에서는 잔여 호출처 통합만.
- `backend/.../question/service/AnalysisScheduler.java` — `fileMetadataRepository` 제거 → `FileFinder` 경유
- `backend/.../question/service/QuestionGenerationTransactionHandler.java` — `interviewRepository` 제거 → `InterviewFinder` 경유
- `backend/.../question/service/QuestionSetService.java` — cross-domain Repository 5건 제거 → Finder 경유

#### feedback
- `backend/.../feedback/service/FeedbackFinder.java` — 신규
- `backend/.../feedback/service/FeedbackService.java` — `QuestionSetAnalysisRepository`, `QuestionSetRepository` 제거 → Finder 경유
- `backend/.../feedback/service/TimestampFeedbackBatch.java` — `questionRepository` 제거 → `QuestionFinder` 경유

#### user
- `backend/.../user/service/UserFinder.java` — 신규 (다른 도메인이 user 조회 시 사용)

#### file
- `backend/.../file/service/FileFinder.java` — 신규 (`AnalysisScheduler` 등이 file 조회 시 사용)

#### reviewbookmark
- `backend/.../reviewbookmark/service/ReviewBookmarkService.java` — `ReviewBookmarkQueryService` 메서드 흡수 + `timestampFeedbackRepository` 제거 → `FeedbackFinder` 경유
- `backend/.../reviewbookmark/service/ReviewBookmarkQueryService.java` — 삭제
- controller / 테스트 참조 갱신

#### servicefeedback
- `backend/.../servicefeedback/service/ServiceFeedbackService.java` — `interviewRepository`, `userRepository` 제거 → `InterviewFinder`, `UserFinder` 경유

#### interview (Phase 1~5 잔여)
- `backend/.../interview/service/FollowUpTransactionHandler.java` — `QuestionRepository`, `QuestionSetRepository` 제거 → `QuestionFinder`, `QuestionSetFinder` 경유
- `backend/.../interview/service/InterviewCompletionService.java` — cross-domain Repository 제거 → Finder 경유
- `backend/.../interview/service/InterviewDeletionService.java` — cross-domain Repository 5건 제거 → Finder 경유 (read) + 해당 도메인 `*Persister` / `*Service` 위임 (write)
- `backend/.../interview/service/InterviewService.java` — Phase 2 흡수 후 cross-domain `QuestionSetRepository` 제거 → `QuestionSetFinder` 경유
- `backend/.../interview/service/InterviewFinder.java` — 기존 Finder (위치 / 책임 점검만)

### 핵심 로직 / 변경 요약
- 도메인별 `*Finder` 신설 (`QuestionFinder`, `QuestionSetFinder`, `FeedbackFinder`, `UserFinder`, `FileFinder`) — Phase 1 룰 (read 전용 / Optional 반환 / DTO 변환 X / 인가 메서드 권장) 적용.
- 거대 App Service (`QuestionSetService` 215줄, `FeedbackService`, `ServiceFeedbackService` 등) 책임 명사 Domain Service 추출은 도메인 단위 사용자 직접 검토 시 분해 추가.
- commit 단위 = 도메인별 분리 (커밋 7개 + interview 잔여 1개 = 8 commit 권장).

### 의존
- 선행: PR1 (Phase 1~5) 머지 — Pilot 결과 검증 후 진입
- 외부 의존: 없음

### Verification Hook
- 명령:
  - `./gradlew test` — 전체 테스트 green
  - `./gradlew build`
  - `rg "Repository\b" backend/src/main/java/com/rehearse/api/domain/ -t java | rg -v "package |import |/repository/"` 후 cross-domain 직접 주입 0 수동 확인 (또는 도메인별 grep — `rg "QuestionSetRepository|QuestionRepository" backend/src/main/java/com/rehearse/api/domain/interview/`)
  - `rg "ReviewBookmarkQueryService" backend/src/` = 0건
- 통과 기준: 전 도메인 그린 + cross-domain 직접 주입 0 + `*QueryService` 잔존 0 + `*Finder` read 전용 / Optional 반환 일관
- dev 환경 수동 시연: 이력서 업로드 / 인터뷰 (CS+Resume) / 피드백 / 북마크 / 서비스 피드백 / 사용자 조회 / 파일 조회 각 1회
- 사용자 직접 검토 통과 (product-spec G2)

### 커밋 메시지 (예상)
```
refactor(BE): resume 도메인 cross-domain Repository 직접 주입 제거
refactor(BE): question 도메인 *Finder 신설 + cross-domain Repository 제거
refactor(BE): feedback 도메인 *Finder 신설 + cross-domain Repository 제거
refactor(BE): user/file 도메인 *Finder 신설
refactor(BE): ReviewBookmarkQueryService 흡수 + cross-domain Repository 제거
refactor(BE): servicefeedback 도메인 cross-domain Repository 제거
refactor(BE): interview 도메인 잔여 cross-domain Repository 제거 (FollowUpTransactionHandler / Completion / Deletion)
```

---

## 통합 Verification

- [ ] tech-spec.md Verification 섹션 (Phase 0+1 / Phase 2) 항목 모두 통과
- [ ] PR1 머지 후 dev 환경 1회 smoke (Resume 트랙 인터뷰 1회) 거쳐 Pilot 검증 → PR2 진입 게이트
- [ ] PR2 머지 후 dev 환경 7 도메인 수동 시연

## 리뷰 게이트 (MANDATORY)

구현 완료 직후 지정 리뷰어 실행 강제. 스킵 = 위반.

- [ ] 지정 리뷰어 실행 (구현 완료 직후 — 메인 세션 책임)
  - PR1, PR2 각각 `code-reviewer-backend` 호출
- [ ] 컨벤션 위반 0건 (`backend/.claude/rules/conventions.md` + `testing.md`)
- [ ] Critical / Major 지적 = fix 반영 후 재리뷰
- [ ] Pre/Post State diff 일치 (tech-spec.md "Pre / Post State")
