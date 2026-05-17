# Product Spec — Admin Home

## Why

어드민 기능이 서비스 피드백 관리와 질문 풀 관리로 늘어나면서, 관리자가 각 URL을 직접 알고 접근해야 한다.

## Goal

관리자 비밀번호 인증 후 접근 가능한 `/admin` 메인 화면에서 주요 어드민 기능으로 바로 이동할 수 있다.

## Scope

- `/admin` 런처 페이지를 추가한다.
- 서비스 피드백 관리와 질문 풀 관리 진입 링크를 제공한다.
- 기존 `/admin/feedbacks`, `/admin/question-pool` 직접 접근은 유지한다.
- FE 테스트, a11y smoke, build로 검증한다.

## Acceptance Criteria

- [ ] 관리자가 `/admin` 접근 시 기존 관리자 비밀번호 인증을 거친다.
- [ ] 인증 후 `/admin`에서 서비스 피드백 관리 링크를 볼 수 있다.
- [ ] 인증 후 `/admin`에서 질문 풀 관리 링크를 볼 수 있다.
- [ ] 서비스 피드백 관리 링크는 `/admin/feedbacks`로 이동한다.
- [ ] 질문 풀 관리 링크는 `/admin/question-pool`로 이동한다.
- [ ] 기존 `/admin/feedbacks`, `/admin/question-pool` 직접 접근은 유지된다.
