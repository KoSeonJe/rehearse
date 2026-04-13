# Plan 04: @Nested 그룹화 — 중형 파일 (5-9 메서드) + lenient().when() 수정 `[parallel]`

> 상태: Completed
> 작성일: 2026-04-13
> 수정일: 2026-04-13 — 실측 기반 수정: 5개 미만 메서드 파일 6개 제외, PersonaResolverTest(9개) Plan 03에서 이동

## Why

5-9개 메서드를 가진 11개 파일에 @Nested 적용. 추가로 `InternalQuestionSetServiceTest.java:421`의 `lenient().when()` → BDDMockito 전환 (컨벤션 규칙 4 위반).

> **규칙: 4개 이하 메서드 파일은 @Nested 생략**
> 그룹화할 대상이 부족하여 불필요한 들여쓰기만 증가한다. GWT 주석 + @DisplayName만으로 충분.

## 의존성

- 선행: 없음
- 후행: Plan 05-15 (컨벤션 패턴 확립)

## @Nested 제외 파일 (4개 이하 메서드)

아래 파일은 메서드 수가 부족하여 @Nested 대상에서 제외한다. Plan 01(GWT) + Plan 02(@DisplayName)만 적용.

| 파일 | 실측 메서드 수 | 제외 사유 |
|------|:---:|------|
| `QuestionCountCalculatorTest.java` | 1 | 그룹화 불가 |
| `InterviewQueryServiceTest.java` | 2 | 그룹화 불가 |
| `InterviewRepositoryTest.java` | 2 | 그룹화 불가 |
| `InterviewTest.java` | 3 | 그룹화 불필요 |
| `UserServiceTest.java` | 4 | 경계값, 의미 있는 그룹 구성 어려움 |
| `InternalFileControllerTest.java` | 4 | 경계값, 의미 있는 그룹 구성 어려움 |

## 생성/수정 파일

| 파일 | 메서드 수 | 작업 |
|------|:---:|------|
| `PersonaResolverTest.java` | 9 | @Nested 추가 (Plan 03에서 이동, 실측 9개) |
| `FollowUpPromptBuilderTest.java` | 8 | @Nested 추가 |
| `ReviewBookmarkRepositoryTest.java` | 8 | @Nested 추가 |
| `FollowUpServiceTest.java` | 7 | @Nested 추가 |
| `FollowUpTransactionHandlerTest.java` | 7 | @Nested 추가 |
| `InternalFileServiceTest.java` | 7 | @Nested 추가 |
| `TechStackTest.java` | 7 | @Nested 추가 |
| `QuestionGenerationPromptBuilderTest.java` | 6 | @Nested 추가 |
| `InterviewServiceTest.java` | 6 | @Nested 추가 |
| `ReviewBookmarkQueryServiceTest.java` | 6 | @Nested 추가 |
| `InterviewCompletionServiceTest.java` | 5 | @Nested 추가 |
| `InternalQuestionSetServiceTest.java` | — | `lenient().when()` → `given()` 전환 (line 421) |

## 상세

- @Nested 구조화 규칙은 Plan 03과 동일
- `lenient().when()` 수정:
  ```java
  // BEFORE
  lenient().when(interview.getId()).thenReturn(100L);
  // AFTER
  given(interview.getId()).willReturn(100L);
  ```
- strict stubbing 영향이 있으면 해당 given을 사용하는 테스트에서 실제 호출 여부 확인

## 담당 에이전트

- Implement: `test-engineer` — 11개 파일 @Nested 구조화 + BDDMockito 수정
- Review: `code-reviewer` — lenient 제거 시 strict stubbing 영향 확인

## 검증

- [ ] 대상 11개 파일 @Nested 적용
- [ ] 제외 6개 파일에 @Nested 미적용 확인
- [ ] `lenient().when()` 0건 (`grep -r "lenient().when" src/test/` 결과 없음)
- [ ] `./gradlew test` 전체 통과
- [ ] `progress.md` 상태 업데이트 (Plan 04 → Completed)
