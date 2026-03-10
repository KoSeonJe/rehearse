# BE 코드 리팩토링

- **Status**: Completed
- **Created**: 2026-03-10
- **Branch**: refactor/be-cleanup

## 목표
MVP 완료 후 백엔드 코드 품질 개선. API 계약 변경 없음.

## 작업 항목

### B1: ClaudeApiClient 책임 분리
- `ClaudeApiClient.java` (287줄) → 3개 클래스로 분리
- `ClaudePromptBuilder`: 프롬프트 구성 (system/user prompt 빌드)
- `ClaudeResponseParser`: JSON 파싱 (extractJson, parseJsonResponse)
- `ClaudeApiClient`: HTTP 호출만 담당

### B2: 공통 Interview 조회 추출
- `InterviewService`, `FeedbackService`, `ReportService` 3곳에 중복된 `findInterviewById`
- → 공통 메서드 추출 (InterviewRepository에 default 메서드 또는 별도 헬퍼)

### B3: 도메인 에러코드 Enum
- 문자열 리터럴 ("INTERVIEW_001" 등) → `ErrorCode` 인터페이스 + 도메인별 Enum
- `InterviewErrorCode`, `FeedbackErrorCode`, `ReportErrorCode`, `AiErrorCode`

### B4: ReportService 트랜잭션 수정
- `getReport()`가 readOnly 트랜잭션 내에서 `generateAndSaveReport()` (write) 호출
- → `getReport()`에 `@Transactional` 명시하여 write 허용

### B5: ReportService 테스트
- `ReportServiceTest.java` 신규 작성
- 리포트 생성, 캐싱(이미 존재 시), 빈 피드백 엣지케이스

### B6: 컨트롤러 테스트 보강
- `FeedbackControllerTest`, `ReportControllerTest` 신규 작성

## 검증
- `./gradlew build` 성공 (컴파일 + 전체 테스트)
