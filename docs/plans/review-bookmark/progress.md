# 복습 북마크 (Review Bookmark) — 진행 상황

## 태스크 상태

| # | 태스크 | 상태 | 담당 | 비고 |
|---|--------|------|------|------|
| 1-BE | QuestionCategory → InterviewType 전환 — Backend PR | Draft | `backend` + `architect-reviewer` + `database-architect` | `[blocking]` 먼저 머지 |
| 1-FE | QuestionCategory 참조 제거 — Frontend PR | Draft | `frontend` + `code-reviewer` | `[blocking]` BE 머지 후 즉시 |
| 2 | review_bookmark 테이블 + ReviewBookmark 엔티티 | Draft | `backend` + `database-architect` | Task 1 완료 후 진행 |
| 3 | Service + Controller + DTO (API) | In Review | `backend` + `code-reviewer` | 구현 완료, 리뷰 반영 완료, 테스트 통과. 커밋/PR 대기 |
| 4 | UI/UX HTML 프로토타입 | Completed | `designer` | `prototypes/review-list-page.html` + `prototypes/README.md` 확정. InterviewType 6그룹 매핑, WCAG AA 토큰 결정 |
| 5 | 피드백 페이지 북마크 버튼 + 코치마크 + 토스트 | Completed | `frontend-developer` | types/api/hook/3 컴포넌트 + feedback-panel/interview-feedback-page 통합. lint+build PASS |
| 6 | 전역 `/review-list` 페이지 + 사이드바 진입점 | Completed | `frontend-developer` | 상수/페이지/6 컴포넌트 + 사이드바 통합. lint+build PASS |
| 7 | 테스트 보강 (단위/통합/E2E) | Completed (automated) | `test-engineer` | BE Repo 3건 추가 (CASCADE / findBookmarkPairs / findOwnerIdById), FE 14건 추가 (bookmark-toggle / filter-bar / empty-state). 수동 E2E 는 follow-up |

## 실행 의존 그래프

```
Task 1-BE (blocking: Flyway V21, Backend 전용)
   │
   ▼
Task 1-FE (blocking: BE 머지 후 즉시 FE category 참조 제거)
   │
   ▼
Task 2 (Flyway V22 + reviewbookmark 엔티티)
   │
   ├──▶ Task 3 (API) ────┐
   └──▶ Task 4 (UI 프로토타입) ─┤
                                 ├──▶ Task 5 (피드백 페이지 버튼) ─┐
                                 └──▶ Task 6 (전역 /review-list 페이지) ─┤
                                                                          └──▶ Task 7 (테스트 보강)
```

## 진행 로그

### 2026-04-12
- 요구사항 정의 및 플랜 문서 작성
- 브레인스토밍을 통해 다음 핵심 결정 확정:
  - **수동 북마크** 방식 선택 (AI 자동 판정 배제)
  - **시도(attempt) 단위** 저장 (질문 단위 머지 배제)
  - **독립 도메인** `reviewbookmark/` 신설
  - **`resolved_at` NULL** 방식으로 상태 표현 (enum status 불필요)
  - **`QuestionCategory` → InterviewType 전환** (컬럼 유지 + 값 전환, 선행 리팩토링)
  - **HTML 프로토타입 선행** — 구현 전 `designer` 에이전트가 UI 확정
  - **전역 `/review-list` 풀페이지** + 사이드바 진입점
- 생성 파일:
  - `docs/plans/review-bookmark/requirements.md`
  - `docs/plans/review-bookmark/plan-01-category-refactor.md`
  - `docs/plans/review-bookmark/plan-02-schema-and-entity.md`
  - `docs/plans/review-bookmark/plan-03-api.md`
  - `docs/plans/review-bookmark/plan-04-ui-prototype.md`
  - `docs/plans/review-bookmark/plan-05-feedback-button.md`
  - `docs/plans/review-bookmark/plan-06-review-list-page.md`
  - `docs/plans/review-bookmark/plan-07-tests.md`
  - `docs/plans/review-bookmark/progress.md`

### 2026-04-12 (초안 검토 반영)
다음 결함을 발견하고 플랜에 반영:
- **[Critical]** Plan 02의 `@EntityGraph` 경로 오류 수정 — `TimestampFeedback.question`이 nullable이므로 Interview 조인은 `questionSetFeedback` 경로로 변경
- **[Critical]** Plan 03의 `GET /exists` 응답 구조를 `{items: [{timestampFeedbackId, bookmarkId}]}`로 변경 — DELETE 시 bookmarkId 필요
- **[Medium]** Plan 01을 BE PR / FE PR 두 개로 분리 — 기존 category 참조 프론트 파일 7개 발견 (`types/interview.ts`, `interview-analysis-page.tsx`, `interview-feedback-page.tsx`, `use-interview-session.ts`, `question-display.tsx`, `question-card.tsx`, `key-features-section.tsx`)
- **[Medium]** Plan 03 소유권 검사를 엔티티 traversal 대신 `findOwnerIdById` Repository 쿼리로 변경 — User 프록시 로딩 회피
- **[Medium]** Plan 01에 QuestionPool / CsSubTopic / QuestionCacheKeyGenerator 사전 조사 단계를 필수로 추가
- **[Low]** Flyway 버전 V21 (Plan 01), V22 (Plan 02)로 명시 — 현재 최신 V20 확인 완료
- **[Low]** Plan 06의 삭제 후 "되돌리기" 토스트 제거, 확인 모달로 대체 — hard delete 복구의 상태 불일치 위험
- **[Low]** `App.tsx` → `app.tsx` 파일명 소문자 수정 — 실제 파일명 확인
