# Plan 03: @Nested 그룹화 — 대형 파일 (10+ 메서드) `[parallel]`

> 상태: Completed
> 작성일: 2026-04-13
> 수정일: 2026-04-13 — PersonaResolverTest 실측 9개 메서드로 Plan 04로 이동, 12개 파일로 축소

## Why

10개 이상 테스트 메서드를 가진 12개 파일이 flat 구조로 작성되어 있다. 메서드/시나리오별 @Nested 그룹화로 구조적 가독성 확보. 컨벤션(`best-practices.md` 규칙 5) 위반.

## 의존성

- 선행: 없음
- 후행: Plan 05-15 (컨벤션 패턴 확립)

## 생성/수정 파일

| 파일 | 메서드 수 | @Nested 그룹 기준 |
|------|:---------:|------------------|
| `AnalysisStatusTest.java` | 30 | 소스 상태별 (PENDING, EXTRACTING, ...) + isTerminal |
| `ReviewBookmarkControllerTest.java` | 17 | 엔드포인트별 (POST, DELETE, GET, PATCH) |
| `QuestionSetAnalysisTest.java` | 17 | 메서드별 (startAnalysis, completeAnalysis, retry, ...) |
| `InternalQuestionSetServiceTest.java` | 14 | 메서드별 (updateProgress, saveFeedback, saveRetryAnalysis) |
| `ServiceFeedbackServiceTest.java` | 12 | 메서드별 |
| `ServiceFeedbackControllerTest.java` | 12 | 엔드포인트별 |
| `ReviewBookmarkEntityTest.java` | 12 | 행위별 (markResolved, reopen, verifyOwnedBy) |
| `ReviewBookmarkServiceTest.java` | 11 | 메서드별 (create, delete, updateStatus) |
| `QuestionSetServiceTest.java` | 10 | 메서드별 |
| `InterviewControllerTest.java` | 10 | 엔드포인트별 |
| `InternalQuestionSetControllerTest.java` | 10 | 엔드포인트별 |
| `ConvertStatusTest.java` | 10 | 전이 규칙별 |

## 상세

- 테스트 로직 **절대 변경 금지** — 클래스 구조만 재배치
- @Nested 클래스에 `@DisplayName` 필수 (한국어, 메서드명 또는 시나리오명)
- 기존 헬퍼 메서드는 외부 클래스에 유지 (inner class에서 접근 가능)
- `@BeforeEach` 등 설정이 있으면 @Nested 내부에도 필요 시 복사 또는 외부 유지

## 담당 에이전트

- Implement: `test-engineer` — 12개 파일 @Nested 구조 전환
- Review: `code-reviewer` — 그룹 기준 적절성, 로직 무변경 확인

## 검증

- [ ] 12개 파일 모두 @Nested 적용
- [ ] 각 @Nested 클래스에 `@DisplayName` 존재
- [ ] `./gradlew test` 전체 통과
- [ ] 테스트 메서드 총 수 변동 없음
- [ ] `progress.md` 상태 업데이트 (Plan 03 → Completed)
