# Tech Spec — Question Pool Admin Detail Management

> **작성자**: frontend/backend agent
> **답하는 질문**: 질문 풀 어드민 상세/수정/비활성화를 어떻게 추가할 것인가
> **승인 게이트**: 사용자 명시 승인 완료 후 구현

---

## Why → Goal

질문 풀 row의 전체 내용을 확인하고 잘못된 row를 수정/비활성화할 수 있게 한다.

## Evidence

- Issue #507: 질문 풀 어드민 상세 모달 및 수정/비활성화 관리.
- 기존 BE API는 `GET`, `POST`만 제공한다.
- `QuestionPool` 엔티티에는 `isActive`와 `deactivate()`가 이미 있어 비활성화 삭제 모델과 맞다.
- 사용자 결정: 삭제는 물리 삭제가 아니라 비활성화 삭제로 진행한다.

## Trade-offs

### Option A (채택) — 비활성화 삭제
- 장점: row를 보존해 운영 실수 복구와 추적이 가능하다.
- 단점: 목록에는 비활성 row가 남을 수 있어 필터 사용이 필요하다.
- 사유: 기존 `isActive` 필드/필터와 일관된다.

### Option B (폐기) — 물리 삭제
- 장점: DB와 화면에서 row가 완전히 사라진다.
- 단점: 운영 실수 복구가 어렵고 기존 활성 상태 모델을 우회한다.
- 폐기 사유: 사용자 결정과 기존 모델에 맞지 않는다.

## Architecture

```
[AdminQuestionPoolPage]
  ├─ row click → detail modal
  ├─ update mutation → PATCH /api/v1/admin/question-pools/{id}
  ├─ single deactivate → PATCH /api/v1/admin/question-pools/{id}/deactivate
  └─ bulk deactivate → PATCH /api/v1/admin/question-pools/deactivate

[AdminQuestionPoolController]
  → [AdminQuestionPoolService]
    → [QuestionPoolRepository]
      → [QuestionPool.update/deactivate]
```

## Data Model

DB schema 변경 없음.

`QuestionPool` 변경:
- `update(cacheKey, content, ttsContent, category, bestAnswer, isActive)`
- 기존 `deactivate()` 재사용

## API Contract

### `PATCH /api/v1/admin/question-pools/{id}`

Header: `X-Admin-Password`

Request:
```json
{
  "cacheKey": "JUNIOR:CS_FUNDAMENTAL",
  "content": "질문 본문",
  "ttsContent": "TTS 문구",
  "category": "운영체제",
  "bestAnswer": "모범답안",
  "isActive": true
}
```

Response 200:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "cacheKey": "JUNIOR:CS_FUNDAMENTAL",
    "content": "질문 본문",
    "ttsContent": "TTS 문구",
    "category": "운영체제",
    "bestAnswer": "모범답안",
    "isActive": true,
    "createdAt": "2026-05-16T10:30:00"
  }
}
```

### `PATCH /api/v1/admin/question-pools/{id}/deactivate`

Header: `X-Admin-Password`

Response 200: 수정된 row 응답. `isActive=false`.

### `PATCH /api/v1/admin/question-pools/deactivate`

Header: `X-Admin-Password`

Request:
```json
{ "ids": [1, 2, 3] }
```

Response 200:
```json
{ "success": true, "data": null }
```

## Verification

- [ ] `cd backend && ./gradlew test --tests "com.rehearse.api.domain.question.controller.AdminQuestionPoolControllerTest"`
- [ ] `cd backend && ./gradlew test --tests "com.rehearse.api.domain.question.service.AdminQuestionPoolServiceTest"`
- [ ] `cd frontend && npm run test -- src/pages/__tests__/admin-question-pool-page.test.tsx`
- [ ] `cd frontend && npm run lint`

## Pre / Post State

### Pre
- 목록 row는 클릭해도 상세 내용을 볼 수 없다.
- 질문 풀 API는 목록/생성만 지원한다.
- 수정/삭제 운영 액션이 없다.

### Post
- row 클릭으로 상세 모달이 열린다.
- 상세 모달에서 수정/단일 비활성화가 가능하다.
- 목록에서 선택 row 일괄 비활성화가 가능하다.
- BE가 수정/비활성화 API를 제공한다.

## 위험 / 롤백

- 위험: row 클릭과 checkbox 클릭 이벤트가 충돌할 수 있다.
- 완화: checkbox click은 row click propagation을 중단한다.
- 롤백: 신규 PATCH endpoint, FE mutations, 상세 모달/선택 UI를 제거한다.
