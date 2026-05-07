# #405 questionset 패키지 통합 Tech Spec

## Scope

Backend package refactor only. FE, DB migration, API contract, Lambda route 변경 없음.

## Design

- `backend/src/main/java/com/rehearse/api/domain/questionset/**` 를 `backend/src/main/java/com/rehearse/api/domain/question/**` 하위 동일 계층으로 이동한다.
- `backend/src/test/java/com/rehearse/api/domain/questionset/**` 를 `backend/src/test/java/com/rehearse/api/domain/question/**` 하위 동일 계층으로 이동한다.
- 모든 Java package 선언/import/FQCN 직접 참조를 `com.rehearse.api.domain.question` 기준으로 바꾼다.
- `QuestionSetController` 의 `@RequestMapping` 과 `InternalQuestionSetController` 의 internal path 는 그대로 유지한다.
- 테이블명 `question_set`, `question_set_analysis`, entity class name, DTO 필드명은 유지한다.
- docs/domain 과 backend AGENTS 도메인 맵에서 questionset 별도 도메인 표현을 제거한다.

## Trade-offs

- 클래스명 유지: API/DB/도메인 용어 안정성이 높고 변경 범위가 package refactor 로 제한된다.
- package 만 통합: import churn 은 발생하지만 wire contract 와 persistence contract 변경 위험이 없다.
- 과거 handoff 문서는 기록성 문서로 남긴다. 최신 운영 문서와 코드에는 old package 참조가 남지 않게 한다.

## Verification

- `rg "com\\.rehearse\\.api\\.domain\\.questionset|domain/questionset|backend/.*/domain/questionset" backend docs`
- `cd backend && ./gradlew compileJava`
- `cd backend && ./gradlew test`

## Public Interfaces

변경 없음.

- Public API: `/api/v1/interviews/{interviewId}/question-sets/{questionSetId}/...`
- Internal API: `/api/internal/interviews/{interviewId}/question-sets/{questionSetId}/...`
- DB schema: unchanged
- DTO JSON schema: unchanged
