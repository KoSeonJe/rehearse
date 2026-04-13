# Plan 10: TtsController + AdminController + ReviewBookmarkFinder 테스트 `[parallel]`

> 상태: Completed
> 작성일: 2026-04-13

## Why

Phase 3. TTS/Admin 컨트롤러의 HTTP 계약 검증(테스트 0%), ReviewBookmarkFinder의 Finder 패턴 검증.

## 의존성

- 선행: Plan 01-04 (컨벤션 확립)
- 후행: 없음

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `src/test/.../tts/controller/TtsControllerTest.java` | 신규 생성 (~7 tests, @WebMvcTest) |
| `src/test/.../admin/controller/AdminControllerTest.java` | 신규 생성 (~5 tests, @WebMvcTest) |
| `src/test/.../reviewbookmark/service/ReviewBookmarkFinderTest.java` | 신규 생성 (~6 tests, Unit) |

## 상세

### TtsControllerTest

테스트 유형: Slice (`@WebMvcTest(TtsController.class)`)
설정: `@TestPropertySource(properties = "google.tts.enabled=true")`
Mock: `TtsService` (`@MockitoBean`)

| # | 테스트 메서드 | 시나리오 | 기대 결과 |
|---|-------------|---------|----------|
| 1 | `synthesize_validText_returns200WithAudioMpeg` | 정상 요청 | 200 + audio/mpeg |
| 2 | `synthesize_checkContentTypeHeader_audioMpeg` | 헤더 검증 | Content-Type: audio/mpeg |
| 3 | `synthesize_checkContentLengthHeader` | 헤더 검증 | Content-Length 일치 |
| 4 | `synthesize_invalidRequest_returns400` | 빈 텍스트 | 400 BAD_REQUEST |
| 5 | `synthesize_serviceException_returns500` | TtsService 예외 | 500 INTERNAL_SERVER_ERROR |

### AdminControllerTest

테스트 유형: Slice (`@WebMvcTest(AdminController.class)`)
설정: `@TestPropertySource(properties = "app.admin.password=test-pass")`
Mock: 없음 (Controller 내부에서 @Value로 비밀번호 비교)

| # | 테스트 메서드 | 시나리오 | 기대 결과 |
|---|-------------|---------|----------|
| 1 | `verify_correctPassword_returns200` | 올바른 비밀번호 | 200 OK |
| 2 | `verify_wrongPassword_throwsException` | 잘못된 비밀번호 | BusinessException |
| 3 | `verify_emptyPassword_returns400` | 빈 문자열 | 400 BAD_REQUEST |
| 4 | `verify_nullPassword_returns400` | null | 400 BAD_REQUEST |
| 5 | `verify_caseSensitive_exactMatch` | 대소문자 구분 | 불일치 시 에러 |

### ReviewBookmarkFinderTest

테스트 유형: Unit (`@ExtendWith(MockitoExtension.class)`)
Mock: `ReviewBookmarkRepository`

| # | 테스트 메서드 | 시나리오 | 기대 결과 |
|---|-------------|---------|----------|
| 1 | `findById_exists_returnsBookmark` | 존재 | ReviewBookmark 반환 |
| 2 | `findById_notFound_throwsReviewBookmarkException` | 미존재 | BOOKMARK_NOT_FOUND |
| 3 | `findByIdAndValidateOwner_validOwner_returnsBookmark` | 소유자 일치 | 정상 반환 |
| 4 | `findByIdAndValidateOwner_notFound_throwsException` | 미존재 | BOOKMARK_NOT_FOUND |
| 5 | `findByIdAndValidateOwner_invalidOwner_throwsException` | 소유자 불일치 | 권한 예외 |
| 6 | `findByIdAndValidateOwner_callsVerifyOwnedBy` | 호출 확인 | verifyOwnedBy 호출 |

## 담당 에이전트

- Implement: `test-engineer` — 3개 파일 작성
- Review: `code-reviewer` — HTTP 응답 형식, 보안 어노테이션 정합성

## 검증

- [ ] `./gradlew test --tests "TtsControllerTest"` 통과
- [ ] `./gradlew test --tests "AdminControllerTest"` 통과
- [ ] `./gradlew test --tests "ReviewBookmarkFinderTest"` 통과
- [ ] TtsControllerTest에 Content-Type: audio/mpeg 검증 포함
- [ ] `progress.md` 상태 업데이트 (Plan 10 → Completed)
