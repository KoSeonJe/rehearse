# 백엔드 코드 품질 종합 리팩토링 — 요구사항 정의

> 상태: Draft
> 작성일: 2026-04-13

## Why

### 1. 어떤 문제를 해결하는가?

백엔드 코드베이스(190개 클래스)를 전체 점검한 결과, **두 축**에서 코드 품질 이슈가 발견되었다:

**A. 도메인 패키지 구조 문제**
- `questionset` 도메인이 38개 클래스로 비대 — question/answer, analysis, feedback 3개 관심사가 한 패키지에 혼재
- `questionpool`이 독립 도메인이지만 실제로는 interview의 질문 생성 전용 (외부 소비자 0)
- `tts`가 도메인 레이어에 있지만 실체는 순수 인프라 통합 (interface 1줄 + controller)

**B. 서비스 레이어 코드 스멜**
- Service God Class: InterviewService(의존성 9개), InternalQuestionSetService(8개), QuestionSetService(8개)
- Transaction Script: 서비스에서 도메인 객체를 직접 조립 (saveFeedback 22줄 빌더 체인)
- Feature Envy: `analysis.getQuestionSet().getFileMetadata().getS3Key()` 같은 getter 체인
- 장문 메서드(30줄+), 메서드 오버로딩 3중 변형 등

### 2. 구체적인 결과물은? 성공 기준은?

| 기준 | Before | After |
|------|--------|-------|
| questionset 클래스 수 | 38개 (비대) | ~12개 (core만) |
| top-level 도메인 수 | 10개 (부적절 분류 포함) | 11개 (적절 분류) |
| InterviewService 의존성 | 9개 | 5개 |
| InternalQuestionSetService 의존성 | 8개 | 7개 |
| 30줄 초과 서비스 메서드 | 5개 | 0개 |
| QuestionPoolService 오버로딩 | 6개 (3×2) | 2개 |

### 3. 근거 데이터나 리서치는?

- 코드베이스 탐색 결과 (190개 클래스 전수 분석)
- DDD 전술 패턴 안티패턴 체크리스트 (Transaction Script, Feature Envy, God Class)
- 프로젝트 자체 컨벤션 문서 (CONVENTIONS.md, CODING_GUIDE.md, TEST_STRATEGY.md)

### 4. 포기하는 것은? 고려한 대안은?

- **Value Object 도입 (제외)**: InterviewId, UserId 등 원시 타입 VO 래핑은 전체 코드베이스 영향이 커서 별도 이니셔티브
- **admin 도메인 통합 (제외)**: 3개 클래스뿐이라 긴급하지 않음
- **Mock 구현체 이동 (제외)**: `@ConditionalOnMissingBean` 프로파일 패턴으로 충분히 동작 중

---

## 현재 강점 (건드리지 않을 것)

- Entity 행위 메서드, 불변 컬렉션 반환, `@Setter` 없음
- Rich Enum (InterviewStatus 상태전이, InterviewType CacheStrategy)
- Controller → DTO 변환, Finder 패턴, ErrorCode 프레임워크
- `@Transactional(readOnly=true)` 클래스 기본값 패턴

---

## 이슈 종합

### A. 패키지 구조 이슈

| # | 이슈 | 심각도 | 설명 |
|---|------|--------|------|
| A1 | questionset 비대 (38개 클래스) | HIGH | question/answer, analysis, feedback 3개 관심사 혼재 |
| A2 | questionpool이 독립 도메인 | MEDIUM | interview 질문 생성 전용, 외부 소비자 없음 |
| A3 | tts가 도메인에 위치 | MEDIUM | 순수 인프라 통합, 구현체는 이미 infra에 위치 |
| A4 | infra.ai.dto 평탄 구조 | LOW | 11개 DTO가 프로바이더 구분 없이 혼재 |

### B. 서비스 품질 이슈

| # | 이슈 | 심각도 | 위치 |
|---|------|--------|------|
| B1 | Service God Class | HIGH | InterviewService(9deps), QuestionSetService(8), InternalQuestionSetService(8) |
| B2 | Transaction Script | HIGH | InternalQuestionSetService.saveFeedback(), QuestionSetService.saveAnswers() |
| B3 | Feature Envy | HIGH | InternalQuestionSetService.updateConvertStatus/retryAnalysis |
| B4 | 장문 메서드 (30줄+) | MEDIUM | QuestionGenerationService, QuestionPoolService |
| B5 | 메서드 오버로딩 3중 변형 | MEDIUM | QuestionPoolService.isPoolSufficient×3, selectFromPool×3 |
| B6 | @Transactional 컨벤션 불일치 | MEDIUM | ReviewBookmarkService |
| B7 | QuestionSet/Question 생성 코드 중복 | MEDIUM | QuestionGenerationService.provideCacheable/FreshQuestions |

---

## Scope

- **In**: 패키지 구조 재편 (A1~A4) + 서비스 레이어 품질 개선 (B1~B7)
- **Out**: Value Object 도입, admin 통합, Mock 구현체 이동, 프론트엔드

---

## 제약조건

### 리팩토링 안전 규칙 (필수 준수)

> **테스트 없는 코드는 리팩토링하지 않는다.**
>
> 리팩토링 대상 클래스에 테스트가 없으면, **반드시 테스트를 먼저 작성**한 뒤 리팩토링한다.
> 이는 리팩토링으로 인한 회귀 버그를 방지하기 위한 필수 전제조건이다.

### 테스트 커버리지 현황 (2026-04-13 기준)

#### 테스트 있음 ✅ — 즉시 리팩토링 가능

| 대상 클래스 | 테스트 파일 |
|------------|-----------|
| InterviewService | InterviewServiceTest |
| QuestionSetService | QuestionSetServiceTest |
| InternalQuestionSetService | InternalQuestionSetServiceTest |
| QuestionPoolService | QuestionPoolServiceTest |
| QuestionGenerationService | QuestionGenerationServiceTest |
| ReviewBookmarkService | ReviewBookmarkServiceTest |
| InterviewCompletionService | InterviewCompletionServiceTest |
| FollowUpService | FollowUpServiceTest |
| FollowUpTransactionHandler | FollowUpTransactionHandlerTest |
| QuestionGenerationEventHandler | QuestionGenerationEventHandlerTest |
| QuestionGenerationTransactionHandler | QuestionGenerationTransactionHandlerTest |
| InterviewFinder | InterviewFinderTest |
| InterviewCreationService | InterviewCreationServiceTest |
| InterviewQueryService | InterviewQueryServiceTest |
| CacheableQuestionProvider | CacheableQuestionProviderTest |
| FreshQuestionProvider | FreshQuestionProviderTest |
| KeywordMatcher | KeywordMatcherTest |
| QuestionGenerationLock | QuestionGenerationLockTest |
| ReviewBookmarkFinder | ReviewBookmarkFinderTest |
| ReviewBookmarkQueryService | ReviewBookmarkQueryServiceTest |
| AnalysisScheduler | AnalysisSchedulerTest |
| ServiceFeedbackService | ServiceFeedbackServiceTest |
| UserService | UserServiceTest |
| InternalFileService | InternalFileServiceTest |
| Interview (entity) | InterviewTest |
| QuestionSet (entity) | QuestionSetTest |
| QuestionSetAnalysis (entity) | QuestionSetAnalysisTest |
| AnalysisStatus (entity) | AnalysisStatusTest |
| ConvertStatus (entity) | ConvertStatusTest |
| FileMetadata (entity) | FileMetadataTest |
| QuestionPool (entity) | QuestionPoolTest |
| ReviewBookmark (entity) | ReviewBookmarkEntityTest |
| TechStack (entity) | TechStackTest |
| QuestionDistribution (vo) | QuestionDistributionTest |

#### 테스트 없음 ❌ — 테스트 작성 후 리팩토링

| 대상 클래스 | 필요한 테스트 | 관련 Wave |
|------------|-------------|----------|
| TimestampFeedback (entity) | TimestampFeedbackTest | Wave 3 (매퍼 생성 전 필요) |
| QuestionSetFeedback (entity) | QuestionSetFeedbackTest | Wave 1 (패키지 이동 전 필요) |
| Question (entity) | QuestionTest | Wave 1 (패키지 이동 전 필요) |
| QuestionAnswer (entity) | QuestionAnswerTest | Wave 1 (패키지 이동 전 필요) |

### 기타 제약조건

- 점진적 리팩토링: 각 Wave는 독립적으로 배포 가능한 단위
- Wave 간 의존성 순서 준수
- 기존 컨벤션(CONVENTIONS.md, CODING_GUIDE.md) 준수
- 모든 Wave 완료 후 `./gradlew test` 전체 통과 필수

---

## 실행 순서 및 의존성

```
Wave 1 (questionset 도메인 분리)          ← 패키지 구조 기반 [blocking]
   ↓
Wave 2 (questionpool→interview, tts→infra) ← Wave 1 안정화 후 [blocking]
   ↓
Wave 3 (엔티티 행위 메서드 추가)            ← 서비스 리팩토링 기반 [blocking]
   ↓
Wave 4 (서비스 God Class 분해)             ← Wave 3 메서드 사용 [blocking]
   ↓
Wave 5 (장문 메서드 + 컨벤션)              ← 내부 Task [parallel] 가능
   ↓
Wave 6 (infra.ai.dto 정리)               [optional]
```

---

## 예상 최종 도메인 구조

```
domain/
├── interview/           (core + generation/pool 포함)
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── event/
│   ├── exception/
│   ├── repository/
│   ├── service/
│   ├── vo/
│   └── generation/      ← questionpool 흡수
│       ├── service/
│       └── pool/
├── questionset/         (core만 — ~12개)
├── question/            ← NEW (~9개)
├── analysis/            ← NEW (~8개)
├── feedback/            ← NEW (~9개)
├── reviewbookmark/      (유지)
├── servicefeedback/     (유지)
├── file/                (유지)
├── user/                (유지)
├── auth/                (유지)
└── admin/               (유지)

infra/
├── ai/
│   └── dto/
│       ├── claude/
│       └── openai/
├── aws/
├── google/
└── tts/                 ← domain에서 이동
```
