# Plan 11: AdminFeedbackController 테스트 `[parallel]`

> 상태: Completed
> 작성일: 2026-04-13

## Why

관리자 피드백 API의 헤더 기반 인증, 페이지네이션 경계값, 응답 구조 검증. 현재 테스트 0%.

## 의존성

- 선행: Plan 01-04 (컨벤션 확립)
- 후행: 없음

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `src/test/.../servicefeedback/controller/AdminFeedbackControllerTest.java` | 신규 생성 (~10 tests, @WebMvcTest) |

## 상세

테스트 유형: Slice (`@WebMvcTest(AdminFeedbackController.class)`)
설정: `@TestPropertySource(properties = "app.admin.password=test-pass")`
Mock: `ServiceFeedbackService` (`@MockitoBean`)

| # | 테스트 메서드 | 시나리오 | 기대 결과 |
|---|-------------|---------|----------|
| 1 | `getAdminFeedbacks_validPassword_returns200` | 올바른 비밀번호 | 200 OK + Page 반환 |
| 2 | `getAdminFeedbacks_missingHeader_throwsException` | X-Admin-Password 누락 | 에러 응답 |
| 3 | `getAdminFeedbacks_nullHeader_throwsException` | 헤더 null | 에러 응답 |
| 4 | `getAdminFeedbacks_wrongPassword_throwsException` | 잘못된 비밀번호 | INVALID_PASSWORD |
| 5 | `getAdminFeedbacks_defaultPagination_page0Size20` | 파라미터 미지정 | page=0, size=20 |
| 6 | `getAdminFeedbacks_customPagination_applied` | page=2, size=10 | 해당 값 적용 |
| 7 | `getAdminFeedbacks_sizeZero_clampedTo1` | size=0 | Math.max(0,1) = 1 |
| 8 | `getAdminFeedbacks_sizeNegative_clampedTo1` | size=-5 | Math.max(-5,1) = 1 |
| 9 | `getAdminFeedbacks_sizeOverMax_clampedTo100` | size=500 | Math.min(500,100) = 100 |
| 10 | `getAdminFeedbacks_emptyResult_returnsEmptyPage` | 데이터 없음 | 빈 Page 반환 |

## 담당 에이전트

- Implement: `test-engineer` — 1개 파일 작성
- Review: `code-reviewer` — 페이지네이션 경계값 커버리지

## 검증

- [ ] `./gradlew test --tests "AdminFeedbackControllerTest"` 통과
- [ ] size 클램핑 (min 1, max 100) 검증 포함
- [ ] X-Admin-Password 헤더 기반 인증 검증 포함
- [ ] `progress.md` 상태 업데이트 (Plan 11 → Completed)
