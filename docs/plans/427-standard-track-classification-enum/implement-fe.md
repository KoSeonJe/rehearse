# Implement (Frontend) — STANDARD 트랙 분류 메타 enum 단일 출처화 (FE 정합)

> **작성자**: frontend agent (Staff Engineer 페르소나 — Claude)
> **답하는 질문**: FE 어떤 순서로 실행?
> **승인 게이트**: ★ tech-spec.md API contract 승인 후 시작 ★ / ★ FE 진입 = BE PR1.5 머지 후 ★
> **product-spec / tech-spec**: `./product-spec.md` / `./tech-spec.md`
> **Issue**: #427 (Epic, P2)

---

## Phase 0: API Contract 확인 + Mock 셋업

`tech-spec.md#api-contract` 의 schema 확정 확인.

- [ ] Phase 1 / Phase 2 = 응답 shape 동등 (FE 변경 없음)
- [ ] Phase 3 = `QuestionDetailResponse.referenceType` 필드 제거 (BE PR4 후행). FE 는 BE 응답에서 해당 필드 무시 / 미사용 코드 제거
- [ ] FE 사용처 = 단일 (`use-answer-flow.ts:378` `referenceType: 'CS'` 하드코딩)
- [ ] Mock 셋업 = 별도 불필요 (BE 응답 shape 변경 없음 / FE 변경 = type 정의 + dead 하드코딩 제거)

미합의 → STOP.

---

## Phase / Step 개요

| Phase | 제목 | 구현 | 예상 PR | 의존 |
|-------|------|------|--------|------|
| 1 | `ReferenceType` type + `QuestionDetail.referenceType` 필드 + `use-answer-flow.ts:378` 하드코딩 제거 | `frontend` | PR3 | BE PR1.5 머지 |

> 단일 Phase. 분리 임계 미초과.

---

## Phase 1: ReferenceType type / 필드 / 하드코딩 일괄 제거

- **구현**: `frontend` — BE enum (`MODEL_ANSWER`/`GUIDE`) 과 의미 충돌하던 FE `ReferenceType` (5종 카테고리) 제거. 단일 사용처 = 하드코딩 → 동시 제거.

### 변경 파일

**Type 정의 (1)**:
- `frontend/src/types/interview.ts`
  - line 57 영역 — `export type ReferenceType = 'RESUME' | 'CS' | 'TECH' | 'BEHAVIORAL' | 'SYSTEM_DESIGN'` 정의 삭제
  - line 71 영역 — `interface QuestionDetail` (또는 동등 타입) 의 `referenceType?: ReferenceType` 필드 삭제

**사용처 (1)**:
- `frontend/src/hooks/use-answer-flow.ts`
  - line 378 영역 — `addQuestionToSet({ ..., referenceType: 'CS' })` 또는 동등 호출의 `referenceType` 키 제거

**잔여 grep 정리**:
- `grep -rn "ReferenceType\|referenceType" frontend/src` → 실제 사용처 0건 확인 (테스트 fixture / Mock / 타 컴포넌트 import 잔존 시 동시 정리)
- `grep -rn "import.*ReferenceType" frontend/src` → 0건 확인 (import 잔존 = TS 컴파일 에러)

### 핵심 로직

```
순수 dead code + 충돌 타입 제거. 동작 변경 없음.

순서:
1) frontend/src/hooks/use-answer-flow.ts:378 의 referenceType 키 삭제 (사용처 우선)
2) frontend/src/types/interview.ts 의 QuestionDetail.referenceType 필드 삭제
3) frontend/src/types/interview.ts 의 ReferenceType type 정의 삭제
4) grep 으로 잔여 import / 사용 0건 확인
5) npm run lint / npm run test / npm run build 통과 확인
```

### 의존
- 선행: BE PR1.5 머지 (`implement-be.md` Phase 1.6 완료) — Phase 3 머지 순서 = FE 선행 → BE 후행
- 외부: 없음 (BE 응답 shape Phase 3 BE PR4 머지 전까지 `referenceType` 잔존 — FE 가 무시)

### Verification

- `npm run lint` — `any` 도입 0건 / unused import 0건
- `npm run test` — 기존 테스트 전체 green. `use-answer-flow.test.ts` 의 `addQuestionToSet` 호출 케이스에서 `referenceType` 누락 정상 통과 (AC-6)
- `npm run build` — TS strict 빌드 통과 (AC-6)
- `grep -rn "ReferenceType\b" frontend/src` → 0건 (BE enum 과 동명 type 잔존 X)
- `grep -rn "referenceType" frontend/src` → 0건 (필드 / 키 잔존 X)
- 통과 기준: 위 모든 verification 통과 + 회귀 0건 (답변 / 꼬리질문 / 피드백 화면 동등)

### 관찰 가능 동작 (수동)
- FE PR3 머지 후 (BE PR4 머지 전 window) dev 환경 인터뷰 1회 진행 — BE 응답에 `referenceType` 잔존 상태에서 FE 정상 동작 확인 (AC-6).
- BE PR4 머지 후 dev 환경 4 트랙 (CS / Behavioral / SystemDesign / Resume) 1회씩 진행 — 답변 / 꼬리질문 / 피드백 화면 변경 전 동등.

### 커밋 메시지
```
refactor(FE): ReferenceType 타입 + dead 하드코딩 제거
```

---

## Phase 4: BE 통합 (별도 Phase 불필요)

- 본 Epic FE 변경 = type / 필드 / 하드코딩 제거뿐. BE 머지 후 mock 제거 단계 없음.
- BE PR4 머지 직후 dev 환경 회귀 1회로 통합 검증 종료.

### Verification
- [ ] BE PR4 머지 후 dev 4 트랙 인터뷰 1회씩 통과
- [ ] BE 응답 `referenceType` 키 부재 + FE 정상 동작 (AC-1 / AC-6)
- [ ] tech-spec.md Verification 통과

## 통합 Verification

- [ ] `tech-spec.md#verification` Phase 3 (FE) 항목 통과
- [ ] BE 통합 후 회귀 체크 (BE PR4 머지 후 dev 4 트랙 정상)
- [ ] AC-6 / AC-1 통과 (`product-spec.md`)

## 리뷰 게이트 (MANDATORY)

구현 완료 직후 지정 리뷰어 실행 강제. 스킵 = 위반.

- [ ] `code-reviewer-frontend` 실행 (구현 완료 직후 — 메인 세션 책임)
- [ ] BE+FE 동시 작업 (Phase 3) = `code-reviewer-backend` 와 **병렬** 호출 (단일 메시지 multiple tool_use)
- [ ] 컨벤션 위반 0건 (`frontend/.claude/rules/conventions.md` + `architecture.md` + `testing.md`)
- [ ] Critical / Major 지적 = fix 반영 후 재리뷰
- [ ] Pre/Post State diff 일치 (`tech-spec.md#pre--post-state` 기준)
