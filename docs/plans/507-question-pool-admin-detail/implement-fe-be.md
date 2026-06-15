# Implement (FE/BE) — Question Pool Admin Detail Management

> **작성자**: frontend/backend agent
> **답하는 질문**: 어떤 순서로 실행?
> **승인 게이트**: 사용자 명시 승인 완료 후 시작

---

## Phase 1: Backend API

- `UpdateQuestionPoolRequest`, `DeactivateQuestionPoolsRequest` 추가
- `QuestionPool.update(...)` 추가
- `AdminQuestionPoolService.update/deactivate/deactivateAll` 추가
- `AdminQuestionPoolController` PATCH endpoint 3종 추가
- Controller/Service 테스트 추가

## Phase 2: Frontend API

- `UpdateQuestionPoolRequest` 타입 추가
- update/deactivate/bulkDeactivate mutation hook 추가
- 성공 시 `admin-question-pools` query invalidate

## Phase 3: Frontend UI

- row click 상세 모달 추가
- 상세 모달 field 표시
- 수정 모드와 저장 액션 추가
- 단일 비활성화 액션 추가
- checkbox 선택과 선택 비활성화 액션 추가
- 모바일 카드도 동일 동작 지원

## Phase 4: Verification

- `cd backend && ./gradlew test --tests "com.rehearse.api.domain.question.controller.AdminQuestionPoolControllerTest"`
- `cd backend && ./gradlew test --tests "com.rehearse.api.domain.question.service.AdminQuestionPoolServiceTest"`
- `cd frontend && npm run test -- src/pages/__tests__/admin-question-pool-page.test.tsx`
- `cd frontend && npm run lint`
