# Plan 02: @DisplayName 누락 보완 `[parallel]`

> 상태: Completed
> 작성일: 2026-04-13

## Why

3개 테스트 파일이 `@DisplayName` 없이 작성되어 있다. 컨벤션 규칙 3 위반. 테스트 실패 시 어떤 비즈니스 규칙이 깨졌는지 즉시 파악하려면 한국어 `@DisplayName`이 필수.

## 의존성

- 선행: 없음
- 후행: Plan 05-15 (컨벤션 패턴 확립)

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `src/test/.../infra/google/GoogleTtsClassLoadingTest.java` | @DisplayName 추가 |
| `src/test/.../RehearseApiApplicationTest.java` | @DisplayName 추가 |
| `src/test/.../questionset/dto/TimestampFeedbackResponseTest.java` | @DisplayName 추가 |

## 상세

- 모든 `@Test` 메서드에 한국어 `@DisplayName` 추가
- 비즈니스 의도를 서술 (메서드명 번역이 아닌 의도 설명)
- 테스트 로직 변경 금지

## 담당 에이전트

- Implement: `test-engineer` — 한국어 @DisplayName 추가
- Review: `code-reviewer` — 한국어 표현 적절성 확인

## 검증

- [ ] 3개 파일의 모든 `@Test`에 한국어 `@DisplayName` 존재
- [ ] `./gradlew test` 통과
- [ ] `progress.md` 상태 업데이트 (Plan 02 → Completed)
