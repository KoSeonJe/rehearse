# Implement (Backend) — {제목}

> **작성자**: backend agent
> **답하는 질문**: BE 어떤 순서로 실행?
> **승인 게이트**: ★ tech-spec.md API contract 승인 후 시작 ★

---

## Phase 0: API Contract 확인

`tech-spec.md#api-contract` 의 요청/응답 schema 확정 여부 확인. FE 와 합의된 상태인지.

- [ ] Endpoint 경로 / 메서드 합의됨
- [ ] Request / Response schema 합의됨
- [ ] Error 코드 매핑 합의됨

미합의 → 즉시 STOP. tech-spec 갱신 + 사용자 승인 재요청.

---

## Phase / Step 개요

| Phase | 제목 | 구현 | 예상 PR | 의존 |
|-------|------|------|--------|------|
| 1 | 도메인 / Entity | `backend` | #N | Phase 0 |
| 2 | Repository / Service | `backend` | #N+1 | Phase 1 |
| 3 | Controller + Validation | `backend` | #N+2 | Phase 2 |
| 4 | 마이그레이션 / 이벤트 / 통합 테스트 | `backend` | #N+3 | Phase 3 |

> Task 8개+ → `tasks/be-{NN}-{slug}.md` 분리.

---

## Phase 1: {제목}

- **구현**: `backend` — {영역 책임 1줄}

### 변경 파일
- `backend/src/main/java/.../Xxx.java`

### 핵심 로직
- 단계별 요약 + 의사코드.

### 의존
- 선행: Phase 0 (contract 합의)
- 외부: (없음 / lib)

### Verification
- `./gradlew test --tests XxxTest`
- Testcontainers MySQL 통합 테스트

### 커밋 메시지
```
feat(BE): xxx Entity 추가
```

---

## Phase 2-N: ...

(동일 구조)

---

## FE 와 통합 시점

- BE 머지 직후 FE 측에 알림 (Issue 댓글 / Slack)
- FE 가 mock 제거 + 실제 API 연동
- 통합 테스트 (E2E) 양측 통과 확인

## 통합 Verification

- [ ] tech-spec.md Verification 통과
- [ ] FE 통합 후 회귀 체크

## 리뷰 게이트 (MANDATORY)

구현 완료 직후 지정 리뷰어 실행 강제. 스킵 = 위반.

- [ ] `code-reviewer-backend` 실행 (구현 완료 직후 — 메인 세션 책임)
- [ ] BE+FE 동시 작업 시 `code-reviewer-frontend` 와 **병렬** 호출 (단일 메시지 multiple tool_use)
- [ ] 컨벤션 위반 0건 (`backend/.claude/rules/conventions.md` + `testing.md`)
- [ ] Critical / Major 지적 = fix 반영 후 재리뷰
- [ ] Pre/Post State diff 일치
