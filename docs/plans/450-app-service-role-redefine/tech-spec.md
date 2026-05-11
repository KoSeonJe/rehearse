# Tech Spec — App Service 역할 재정의 및 도메인별 점진 리팩토링

> **작성자**: backend agent (Claude)
> **답하는 질문**: 어떻게? 구조 / API / 데이터 / Trade-off
> **승인 게이트**: ★ 사용자 명시 승인 후 implement.md 진입 ★
> **관련 Plan**: `docs/plans/450-app-service-role-redefine/product-spec.md`
> **관련 Issue**: #450

---

## Why → Goal (1줄 미러)

App Service 가 비즈니스 흐름 + cross-domain 호출 + 데이터 가공 혼재 → 흐름 파악 / 도메인 경계 흐림. 본 작업으로 App Service = 조립자 한정, 비즈로직 = 책임 명사 Domain Service 추출, cross-domain 조회 = `*Finder` 단일 진입점 강제.

## Evidence

**현재 구조**:
- `backend/src/main/java/com/rehearse/api/domain/interview/service/` — 20 클래스 (App Service / Domain Service 혼재).
- `FollowUpService.java:43-219` — 220줄 / 의존 13개 / cross-domain `*Persister` / `*RuntimeCache` / `*Planner` 직접 주입.
- `InterviewQueryService.java:31-105` — controller 직결 read-only App Service. `QuestionSetRepository` cross-domain 주입.
- `ReviewBookmarkQueryService.java` — 동일 패턴 (read-only App Service 변형).
- Cross-domain Repository 직접 주입 = 전 도메인 광범위:
  - `interview` 7건 (`FollowUpTransactionHandler`, `InterviewCompletionService`, `InterviewDeletionService` 5건, `InterviewQueryService`, `InterviewService` 2건).
  - `question` 8건 (`AnalysisScheduler`, `InternalQuestionSetService`, `QuestionGenerationTransactionHandler`, `QuestionSetService` 5건).
  - `resume` 4건 (`ResumeInterviewOrchestrator`, `ResumeQuestionPersister` 2건, `ResumeTurnEventPublisher`).
  - `feedback` 3건 (`FeedbackService` 2건, `TimestampFeedbackBatch`).
  - `reviewbookmark` 1건 (`ReviewBookmarkService` → `TimestampFeedbackRepository`).
  - `servicefeedback` 2건 (`InterviewRepository`, `UserRepository`).
- `backend/.claude/rules/conventions.md` line 77-91 (클래스 접미사 표) — `*Finder` 항목 부재. line 47 "Cross-domain: app service → 하위 Repository 직접 접근 허용" 룰 존재 (본 Epic 에서 강화 대상).

**컨벤션**:
- `backend/.claude/rules/conventions.md` line 109-118 (계층 책임 표 — App Service = 트랜잭션 경계 + 조립).
- `backend/.claude/rules/testing.md` Domain Unit / Service Integration 카테고리 매핑.

**유사 spec**:
- `docs/plans/405-questionset-package-unification/` (question 도메인 패키지 정리 선행 plan).
- `docs/plans/460-domain-naming-cleanup/` (도메인 명명 정리 선행 plan).

**사용자 결정** (product-spec + 본 tech-spec 결정 게이트):
- Pilot 도메인 = `interview`. Cross-domain Repository 정리 본 Epic 포함. 패키지 분리 X.
- 명명 = 책임 명사 유지 + cross-domain 조회 진입점만 `*Finder` 통일.
- Phase 2 도메인 = 한번에 일괄 변경 후 단일 PR.
- G2 측정 = 사용자 직접 검토 (수치 지표 X).
- `*QueryService` → `*Service` 통합 (도메인당 App Service 1개).
- ArchUnit 도입 X (수동 리뷰).

**추정 / 미확인 가정**:
- Phase 2 도메인 (resume / question / feedback / user / file / reviewbookmark / servicefeedback) 의 책임 추출 분량은 Pilot 결과 패턴 적용 후 측정. 정확한 클래스 단위 분해 계획은 implement.md 작성 시 확정.
- `auth` 도메인 = `domain/auth/service/` 디렉토리 부재 → 본 Epic 대상 제외 (OAuth = `global/security/oauth2/`).

## Trade-offs

### Option A (채택): Phase 0+1 통합 PR → Phase 2 단일 PR (PR 2개)
- **장점**:
  - Phase 0 룰 텍스트가 Phase 1 Pilot 코드로 즉시 검증 → 비현실 룰 위험 ↓.
  - Phase 2 단일 PR = 일괄 변경 (사용자 결정).
  - Pilot 결과 검증 후 Phase 2 진입 → 거대 PR 회귀 위험 분산.
- **단점**:
  - Phase 0+1 PR 도 중간 사이즈. 룰 + Pilot 동시 리뷰.
- **사유**: 사용자 결정 "Phase 2 일괄" + Pilot 의미 ("결과물이 베스트 프렉티스 기준") 동시 충족.

### Option B (폐기): Phase 0 / 1 / 2 별 PR 3개
- **장점**: 룰 머지 빠름 / 리뷰 단위 작음.
- **폐기 사유**: Phase 0 룰만 머지 시 코드 변경 없음 → 룰 검증 어려움. 비현실 룰 위험.

### Option C (폐기): 전체 단일 PR
- **장점**: 사용자 발화 "일괄" 의 가장 단순 해석.
- **폐기 사유**: Pilot 의미 ("후속 도메인 참고 기준") 상실 + PR diff 폭주 (룰 + Pilot + 7개 도메인).

## Architecture

### Phase 0+1 — 룰 보강 + interview Pilot (PR 1)

#### Phase 0: `conventions.md` 룰 보강

**변경 위치**: `backend/.claude/rules/conventions.md`.

**보강 내용**:
1. **"클래스 접미사" 표 (line 77-91)** 에 `*Finder` 행 추가:
   ```
   | Finder | `*Finder` | `InterviewFinder` (cross-domain 조회 진입점) |
   ```
2. **"패키지 구조" 도메인 패키지 룰 (line 47)** cross-domain 정책 강화:
   - 변경 전: `Cross-domain: app service → 같은 aggregate 상위 → 하위 만 (하위 → 상위 금지). Repository 직접 접근 허용. 도메인 간 이벤트 통신 가능.`
   - 변경 후: `Cross-domain: app service / domain service 는 타 도메인 데이터 조회 시 해당 도메인 *Finder 경유 강제. Repository / *Persister / *RuntimeCache / *Planner 직접 주입 금지. 도메인 간 이벤트 통신 가능.`
3. **"계층 책임" 표 (line 109-118)** 다음 행 추가:
   ```
   | Finder | 자기 도메인 단순 lookup (read 전용 cross-domain 진입점) | 비즈로직 / write (저장·갱신) / DTO 변환 |
   ```
4. **"Service 책임 분리" 섹션 신설** (line 109 위 또는 아래):
   - App Service 책임: 트랜잭션 경계, Domain Service / `*Finder` / Repository 조립, 흐름 분기.
   - App Service 금지: HTTP 객체 의존, 깊은 비즈로직 (메서드 본문 책임 혼재), cross-domain Repository / `*Persister` / `*RuntimeCache` 직접 주입.
   - Domain Service 책임: 도메인 로직, 단일 책임 명사 클래스.
   - **`*Finder` 룰**: read 전용 진입점. entity 반환 (DTO 변환 X). write 흐름 (생성·저장·캐시 갱신) 흡수 금지 — 호출자가 해당 도메인 App Service 호출.
   - `*QueryService` 패턴 폐기 명시 — read-only App Service 도 `*Service` 통일.
   - **Before-After 예시 2쌍**:
     - 예시 1: cross-domain Repository 직접 주입 → `*Finder` 경유.
     - 예시 2: 거대 App Service 메서드 → Domain Service 추출.

#### Phase 1: interview Pilot

**대상 클래스** (20개):
- App Service: `InterviewService`, `InterviewCreationService`, `InterviewCompletionService`, `InterviewDeletionService`, `InterviewQueryService` (→ `InterviewService` 흡수), `FollowUpService`.
- Domain Service: `InterviewFinder`, `AudioTurnAnalyzer`, `AnswerAnalyzer`, `FollowUpQuestionWriter`, `FollowUpTransactionHandler`, `TurnAnalysisPipeline`, `TextFallbackTurnAnalyzer`, `InterviewRuntimeStateCache`, `InterviewRetryRecorder`, `InterviewTurnPolicy`, `InterviewTurnPolicyResolver`, `StandardFollowUpPolicy`, `ResumeTrackPolicy`.

**변경 시퀀스**:

```
1. InterviewQueryService → InterviewService 흡수
   - getInterview / getInterviewByPublicId / getInterviews / getStats 4 메서드 InterviewService 이동
   - InterviewQueryService 파일 삭제
   - controller 참조 InterviewService 로 전환

2. ResumeFinder 신설 (resume 도메인) — read 전용
   - 위치: backend/.../resume/service/ResumeFinder.java
   - 책임: cross-domain read 진입점 (entity 반환). write / 생성 / 캐시 갱신 책임 X.
   - 메서드:
     · findSkeletonByInterviewId(interviewId) → Optional<ResumeSkeleton> (runtime cache miss 시 DB 조회까지만, 신규 생성 X)
     · findInterviewPlan(interviewId) → Optional<InterviewPlan> (runtime cache + DB 조회만, 생성 fallback X)
   - 내부 의존 (read 만): ResumeSkeletonRepository, ResumeSkeletonRuntimeCache (read API), InterviewPlanRepository, InterviewPlanRuntimeCache (read API)
   - write 흐름은 resume App Service 가 담당 — 아래 항목 3 의 `ResumeInterviewOrchestrator` (또는 신설 App Service 메서드) 가 `ResumeInterviewPlanner` / `*Persister` / `*RuntimeCache` write API 조립

3. Resume App Service rename + write 진입점 정비 (resume 도메인)
   - `ResumeInterviewOrchestrator` → `ResumeInterviewService` rename (컨벤션 일치: App Service = `*Service`)
   - rename 후 `ensureInterviewPlan(interviewId, skeleton, durationMinutes)` 메서드 추가:
     · plan 부재 시 ResumeInterviewPlanner 호출 + InterviewPlanPersister.save + InterviewPlanRuntimeCache 갱신
     · plan 존재 시 그대로 반환
   - ResumeSkeletonPersister / InterviewPlanPersister / 두 RuntimeCache / ResumeInterviewPlanner 는 resume 도메인 내부 책임 명사 Domain Service 로 유지

4. FollowUpService 책임 추출
   - generateFollowUp() 흐름 정리:
     · audioFile 가드 → 기존 유지
     · context 로드 → followUpTransactionHandler
     · runtime state init → runtimeStateStore
     · Resume 라우팅 분기 → ResumeRoutePolicy (신설 Domain Service)
     · 분기 1: Resume 트랙 →
         - skeleton 조회 = ResumeFinder.findSkeletonByInterviewId
         - plan 조회 = ResumeFinder.findInterviewPlan → Optional.empty 시 ResumeInterviewService.ensureInterviewPlan 호출
         - ResumeInterviewService.processUserTurn 호출
     · 분기 2: 표준 트랙 → audioTurnAnalyzer.analyze → SKIP 분기 또는 followUpQuestionWriter.write
   - 추출 Domain Service:
     · ResumeRoutePolicy — isResumeTrack 로직 (현 FollowUpService.isResumeTrack)
     · FollowUpSkipHandler — analyzer SKIP / Step B SKIP 공통 처리 (현 handleAnalyzerSkip + skip 분기)
     · FollowUpResponseBuilder — buildAnswerResponse (정적 메서드 → record 또는 utility)
   - cross-domain 의존 제거 검증 (FollowUpService 기준):
     · resumeInterviewService (= rename 후 ResumeInterviewService): 유지 (resume App Service 호출 — 도메인 간 정상)
     · resumeSkeletonStore (= ResumeSkeletonPersister): 제거 → ResumeFinder 경유 (read)
     · interviewPlanStore (= InterviewPlanPersister): 제거 → ResumeFinder (read) + resume App Service (write) 경유
     · resumeSkeletonCache (= ResumeSkeletonRuntimeCache): 제거 → ResumeFinder 경유 (read)
     · interviewPlanCache (= InterviewPlanRuntimeCache): 제거 → ResumeFinder (read) + resume App Service (write) 경유
     · resumeInterviewPlanner: 제거 → resume App Service 가 내부 조립

5. FollowUpTransactionHandler cross-domain 제거
   - QuestionRepository / QuestionSetRepository 주입 → QuestionFinder 신설 후 경유 (Phase 1 안에 신설)
   - 또는 Phase 2 일괄 변경 시 처리 (Pilot 범위 결정 필요)

6. InterviewCompletionService / InterviewDeletionService cross-domain 제거
   - 다수 타 도메인 Repository 주입 → 각 도메인 *Finder 경유로 전환
   - 단, InterviewDeletionService 의 *Repository 5개 중 일부는 삭제 호출 (Persister 책임) → 해당 도메인 *Service 또는 *Persister 위임으로 전환
```

**Phase 1 흐름도**:
```
[InterviewController]
    ↓
[InterviewService (=조립자 + read API)]
    ↓
[InterviewFinder]  [Domain Services]   [ResumeFinder (cross-domain)]
                   - AudioTurnAnalyzer       ↓
                   - FollowUpQuestionWriter  [resume 도메인 내부]
                   - InterviewRuntimeStateCache
                   - ResumeRoutePolicy (신설)
                   - FollowUpSkipHandler (신설)
```

### Phase 2 — Rollout 일괄 (PR 2)

**대상 도메인** (7개): `resume`, `question`, `feedback`, `user`, `file`, `reviewbookmark`, `servicefeedback`.

**도메인별 변경 요약**:

| 도메인 | 신설 | 흡수 / 폐기 | Cross-domain 제거 대상 |
|---|---|---|---|
| `resume` | `ResumeFinder` (Phase 1 에서 신설됨 — 활용) | — | `ResumeInterviewOrchestrator.questionSetRepository`, `ResumeQuestionPersister` 2건, `ResumeTurnEventPublisher.questionSetRepository` |
| `question` | `QuestionFinder`, `QuestionSetFinder` | — | `AnalysisScheduler.fileMetadataRepository`, `QuestionGenerationTransactionHandler.interviewRepository`, `QuestionSetService` 5건 (`fileMetadataRepository` 등) |
| `feedback` | `FeedbackFinder` | — | `FeedbackService` 2건 (`QuestionSetAnalysisRepository`, `QuestionSetRepository`), `TimestampFeedbackBatch.questionRepository` |
| `user` | `UserFinder` | — | (cross-domain 없음 — 단순 정리만) |
| `file` | `FileFinder` | — | (자기 도메인만 — 단순 정리) |
| `reviewbookmark` | `ReviewBookmarkFinder` (이미 존재 — 활용) | `ReviewBookmarkQueryService` → `ReviewBookmarkService` 흡수 | `ReviewBookmarkService.timestampFeedbackRepository` |
| `servicefeedback` | (불필요 — cross-domain 진입 부재) | — | `ServiceFeedbackService.interviewRepository`, `userRepository` |

**`*Finder` 메서드 시그니처 룰**:
- **read 전용**. 생성 / 저장 / 캐시 갱신 흡수 금지 — 호출자가 해당 도메인 App Service 호출.
- 기본 = `findById(Long id)`, `findById<ColName>(...)` 같은 단순 lookup. 부재 = `Optional` 반환 (생성 fallback X).
- 인가 = `findByIdAndValidateOwner(Long id, Long userId)` 권장 (entity.verifyOwnedBy 호출).
- 비즈니스 조회 (필터 / 페이징 / 통계) 는 `*Finder` 가 아닌 App Service 또는 Domain Service.
- DTO 변환은 `*Finder` 가 하지 않음 (entity 반환).

**거대 App Service 책임 추출** (도메인별):
- `QuestionSetService` (215줄 / 의존 10) — read API + 데이터 조립 혼재 → `QuestionSetAssembler` 강화 + 책임 명사 추출. 정확 분해 = implement.md.
- `FeedbackService`, `ServiceFeedbackService` 등 — 동일 패턴 검토. 정확 분해 = implement.md.

## Data Model

**변경 없음**. 리팩토링 only — DB 스키마 / Entity 필드 / 컬럼 변경 없음.

## API Contract

**변경 없음**. 외부 동작 (HTTP endpoint / request / response / error code) 동등 유지. 회귀 검증 = 기존 테스트 + dev 수동 시연.

## Verification (완료 판정)

### Phase 0+1 (PR 1)
- [ ] `./gradlew test` — `interview` 도메인 전체 테스트 그린 + 추출 Domain Service 단위 테스트 추가.
- [ ] `./gradlew build` — 컴파일 / 린트 통과.
- [ ] 추출 Domain Service 단위 테스트 (testing.md Domain Unit):
  - `ResumeRoutePolicyTest` — Resume 트랙 식별 분기 케이스 (state 기반 / interview type 기반).
  - `FollowUpSkipHandlerTest` — analyzer SKIP / Step B SKIP 분기.
  - `ResumeFinderTest` — Domain Unit (Repository 실제 주입 또는 Mock — testing.md 결정 가이드 따름).
- [ ] Service Integration 회귀 (기존 테스트 그린):
  - 인터뷰 시작 / FollowUp 생성 / Resume 라우팅 / 인터뷰 종료.
- [ ] dev 환경 수동 시연 — 인터뷰 1회 완주 (CS + Resume 두 트랙 각 1회).
- [ ] 사용자 직접 검토 통과 — `FollowUpService` / `InterviewService` (흡수 후) / `ResumeFinder` / 추출 Domain Service 코드 사용자 확인.
- [ ] 코드 검증:
  - `interview` 도메인 App Service 가 cross-domain Repository / `*Persister` / `*RuntimeCache` / `*Planner` 직접 주입 = 0 (grep 으로 확인).
  - `InterviewQueryService` 파일 부재.
  - `conventions.md` `*Finder` 표 행 추가 + "Service 책임 분리" 섹션 신설 + Before-After 예시 2쌍.

### Phase 2 (PR 2)
- [ ] `./gradlew test` 전 도메인 그린 + 도메인별 추출 Domain Service 단위 테스트.
- [ ] `./gradlew build` 통과.
- [ ] Service Integration 회귀 (전 도메인):
  - 이력서 업로드 → 인터뷰 → 피드백 / 북마크 / 서비스 피드백 / 사용자 조회 / 파일 조회 각 1회.
- [ ] dev 환경 수동 시연 — 각 도메인 핵심 플로우 1회씩.
- [ ] 사용자 직접 검토 통과 — 7개 도메인 변경 사용자 확인.
- [ ] 코드 검증:
  - 전 도메인 App Service 가 cross-domain Repository / `*Persister` / `*RuntimeCache` / `*Planner` 직접 주입 = 0 (grep 전수).
  - `ReviewBookmarkQueryService` 파일 부재.
  - 7개 도메인 `*Finder` 보유 (해당 도메인이 다른 도메인에서 조회 받는 경우만 — `user`, `file` 같은 진입 없는 도메인은 신설 불필요 — implement.md 단계 확정).

## Pre / Post State

### Pre (현재)
- `interview/service/` 20 클래스 — App Service / Domain Service 혼재 + cross-domain Repository / `*Persister` / `*RuntimeCache` / `*Planner` 직접 주입 7건.
- `FollowUpService` 220줄 / 의존 13개.
- `InterviewQueryService` (read-only App Service 변형) + `ReviewBookmarkQueryService` 존재.
- `conventions.md` 클래스 접미사 표 `*Finder` 부재 + cross-domain Repository 직접 접근 허용 룰 (line 47).
- 전 도메인 cross-domain Repository 직접 주입 위반 ≥ 25건.

### Post (구현 후)
- `interview/service/` App Service = `InterviewService` 단일 (조립자 + read API 통합) + Domain Service 책임 명사 다수.
- `FollowUpService` = 흐름 조립자 한정 (의존 ≤ ~7, cross-domain `*Persister`/`*RuntimeCache`/`*Planner` 직접 주입 0).
- `resume/service/ResumeInterviewOrchestrator` → `ResumeInterviewService` rename + `ensureInterviewPlan` 메서드 추가. resume 도메인 App Service 명명 컨벤션 일치.
- `resume/service/ResumeFinder` 신설 (read 전용 cross-domain 진입점).
- `*QueryService` 패턴 전 도메인 폐기.
- `conventions.md` "Service 책임 분리" 섹션 신설 + `*Finder` 접미사 표 등재 + cross-domain Repository 금지 룰 강화 + Before-After 예시 2쌍.
- 전 도메인 cross-domain 조회 = `*Finder` 경유. Repository / `*Persister` / `*RuntimeCache` / `*Planner` 직접 주입 = 0.

## 위험 / 마이그레이션 / 롤백

**위험**:
- **Phase 2 거대 PR diff** — 7개 도메인 일괄 변경. 리뷰 부담 + 회귀 미발견 위험.
  - 완화: Pilot (Phase 0+1) 결과 사용자 직접 검토 통과 후 진입. dev 수동 시연 7회 (도메인별). 단위 테스트 + Service Integration 회귀 강제.
- **`InterviewQueryService` 흡수 후 `InterviewService` 비대화** — 현 `InterviewService` 5.8K + read API 4 메서드 흡수.
  - 완화: 책임 명사 Domain Service 추출 (예: `InterviewProjection` / `InterviewStatsCalculator`) 가 필요하면 implement.md 단계 결정.
- **`ResumeInterviewOrchestrator` rename 광범위 참조 갱신** — 클래스명 변경으로 호출자 / 테스트 / 로그 메시지 참조 일괄 수정 필요.
  - 완화: rename refactor 도구 (IDE) 사용. grep 으로 잔존 참조 0건 검증 (`rg "ResumeInterviewOrchestrator" backend/src/`).

**마이그레이션 전략**: 단순 코드 리팩 — 점진 마이그레이션 / dual-write 불필요. PR 머지 시점 = 전환 시점.

**롤백 시나리오**: 코드 revert 100% 복구 가능 (DB 변경 없음). PR revert 머지로 즉시 원복.

## 분기 결정

- [x] **단일 영역 → `implement.md` 1개**

BE only — `frontend` / `lambda` 영향 없음 (HTTP API 동등). 단 PR 2개 분리 (Phase 0+1 / Phase 2) 운영. implement.md 안에 Phase 별 task 분리.
