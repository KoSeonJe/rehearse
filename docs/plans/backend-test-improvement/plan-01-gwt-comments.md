# Plan 01: Given-When-Then 주석 추가 `[parallel]`

> 상태: Completed
> 작성일: 2026-04-13

## Why

기존 테스트의 56%(24파일)가 `// given`, `// when`, `// then` 섹션 주석이 없어 테스트 구조 파악이 어렵다. 프로젝트 컨벤션(`project-conventions.md` 규칙 6)에 필수로 정의되어 있다.

## 의존성

- 선행: 없음
- 후행: Plan 05-15 (컨벤션 패턴 확립)

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `src/test/.../questionset/entity/AnalysisStatusTest.java` | GWT 주석 추가 (30 메서드) |
| `src/test/.../questionset/entity/ConvertStatusTest.java` | GWT 주석 추가 |
| `src/test/.../questionset/entity/QuestionSetAnalysisTest.java` | GWT 주석 추가 |
| `src/test/.../questionset/controller/QuestionSetControllerTest.java` | GWT 주석 추가 |
| `src/test/.../questionset/controller/InternalQuestionSetControllerTest.java` | GWT 주석 추가 |
| `src/test/.../questionset/dto/TimestampFeedbackResponseTest.java` | GWT 주석 추가 |
| `src/test/.../interview/controller/InterviewControllerTest.java` | GWT 주석 추가 |
| `src/test/.../interview/service/InterviewServiceTest.java` | GWT 주석 추가 |
| `src/test/.../interview/service/InterviewQueryServiceTest.java` | GWT 주석 추가 |
| `src/test/.../interview/entity/InterviewTest.java` | GWT 주석 추가 |
| `src/test/.../interview/entity/TechStackTest.java` | GWT 주석 추가 |
| `src/test/.../reviewbookmark/controller/ReviewBookmarkControllerTest.java` | GWT 주석 추가 |
| `src/test/.../reviewbookmark/entity/ReviewBookmarkEntityTest.java` | GWT 주석 추가 |
| `src/test/.../reviewbookmark/service/ReviewBookmarkServiceTest.java` | GWT 주석 추가 |
| `src/test/.../reviewbookmark/service/ReviewBookmarkQueryServiceTest.java` | GWT 주석 추가 |
| `src/test/.../reviewbookmark/repository/ReviewBookmarkRepositoryTest.java` | GWT 주석 추가 |
| `src/test/.../servicefeedback/controller/ServiceFeedbackControllerTest.java` | GWT 주석 추가 |
| `src/test/.../servicefeedback/service/ServiceFeedbackServiceTest.java` | GWT 주석 추가 |
| `src/test/.../infra/ai/FollowUpPromptBuilderTest.java` | GWT 주석 추가 |
| `src/test/.../infra/ai/PersonaResolverTest.java` | GWT 주석 추가 |
| `src/test/.../infra/ai/QuestionCountCalculatorTest.java` | GWT 주석 추가 |
| `src/test/.../infra/ai/QuestionGenerationPromptBuilderTest.java` | GWT 주석 추가 |
| `src/test/.../RehearseApiApplicationTest.java` | GWT 주석 추가 |
| `src/test/.../infra/google/GoogleTtsClassLoadingTest.java` | GWT 주석 추가 |

## 상세

- 모든 `@Test` 메서드에 `// given`, `// when`, `// then` 섹션 주석 삽입
- 한 줄짜리 assertion 테스트(예: 상태 전이 체크)는 `// when & then` 형식 허용
- 테스트 로직, assertion, mock 설정 **절대 변경 금지** — 주석만 추가

## 담당 에이전트

- Implement: `test-engineer` — 24개 파일에 GWT 주석 삽입
- Review: `code-reviewer` — 로직 변경 없음 확인

## 검증

- [ ] 24개 파일의 모든 `@Test` 메서드에 `// given`, `// when`, `// then` (또는 `// when & then`) 존재
- [ ] `./gradlew test` 전체 통과
- [ ] `git diff --stat`에서 `.java` 파일만 변경, 테스트 외 파일 변경 없음
- [ ] `progress.md` 상태 업데이트 (Plan 01 → Completed)
