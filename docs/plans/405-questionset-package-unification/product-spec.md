# #405 questionset 패키지 통합 Product Spec

## Why

`questionset` 은 사용자에게 노출되는 별도 제품 도메인이 아니라 question 도메인의 녹화/분석 단위이다. 현재 Java package 와 도메인 문서에서 `question` 과 `questionset` 이 분리되어 있어 코드 탐색, 도메인 의존성 설명, 신규 작업 범위 판단이 불필요하게 갈라진다.

## Goal

- `com.rehearse.api.domain.questionset` Java package 를 제거한다.
- `QuestionSet*`, `AnalysisStatus`, `ConvertStatus` 등 클래스명과 도메인 용어는 유지한다.
- API path, DB 테이블/컬럼/FK, DTO JSON schema, Lambda callback path 는 변경하지 않는다.
- 문서에서 `questionset` 을 별도 도메인으로 표현하지 않고 `question` 도메인 하위 책임으로 정리한다.

## Acceptance Criteria

- backend 운영/테스트 코드에 `com.rehearse.api.domain.questionset` import/package 선언이 남지 않는다.
- backend 파일 경로에 `domain/questionset` 디렉터리가 남지 않는다.
- `/question-sets` public/internal API path 는 기존 테스트로 유지 확인된다.
- `compileJava` 와 backend test 가 통과한다.
