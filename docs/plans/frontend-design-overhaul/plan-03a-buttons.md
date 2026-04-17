# Plan 03a: Button 컴포넌트 shadcn 교체

> 상태: Completed
> 작성일: 2026-04-17

## Why

Button이 UI 전반에서 가장 많이 사용되며 variant 매핑 규칙이 명확해 shadcn 전환의 첫 단계로 적합. 이후 Phase 3b~e 작업의 패턴을 확립한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `frontend/src/components/ui/button.tsx` | shadcn `Button`으로 재정의 (기존 props 호환 시도) |
| `frontend/src/pages/**/*.tsx` | 페이지 내 인라인 `<button>` → shadcn `Button` 교체 |
| `frontend/src/components/**/*.tsx` | feature 컴포넌트 내 버튼 교체 |

## 상세

### 1. shadcn Button 설치

```bash
cd frontend
npx shadcn@latest add button
```

### 2. Variant 매핑 규칙

| 기존 용도 | shadcn variant |
|----------|---------------|
| primary / main CTA | `default` |
| secondary | `secondary` |
| text-only / link-like | `ghost` |
| 파괴적(삭제/탈퇴) | `destructive` |
| 아이콘만 | `ghost` + `size="icon"` |
| **판단 애매** | **그대로 두고 사용자 질문** |

### 3. 교체 순서 (기능별 커밋)

1. `components/layout/` (헤더/푸터의 공통 버튼)
2. `components/auth/` + `login-modal.tsx` (로그인 흐름)
3. `pages/home` (랜딩 CTA)
4. `pages/dashboard` + `pages/review-list`
5. `pages/interview-setup`, `interview-ready`, `interview-page`
6. `pages/interview-feedback`, `interview-analysis`
7. `pages/about`, `faq`, `guide`, `privacy-policy`, `admin-feedbacks`, `not-found`

각 단계마다 독립 커밋:
```
fix(fe): 로그인 흐름 Button shadcn 교체
fix(fe): 대시보드 Button shadcn 교체
...
```

### 4. 제약

- 이벤트 핸들러, `onClick`, `disabled`, form submit 동작 **변경 금지**
- `className` prop은 유지 (사용처 커스텀 스타일 보존)
- 판단 애매한 케이스는 **원형 그대로 두고** 주석 `// TODO(design): variant 판단 보류 — 사용자 확인` + 사용자에게 질문 리스트 제출

## 담당 에이전트

- Implement: `frontend` — 컴포넌트 교체
- Review: `code-reviewer` — 타입/lint, 기능 회귀 검토

## 검증

- `npm run lint` green
- `npm run build` green (타입 에러 0)
- `npm run test` green
- 수동 스모크: 각 커밋 후 해당 페이지 버튼 클릭/submit 동작 확인
- `grep -rn "<button" frontend/src` 결과 감소 (shadcn `<Button>` 외 인라인 잔존이 의도된 곳만)
- 판단 보류 건 리스트 제출
- `progress.md` Task 3a → Completed

## 체크포인트

변경 파일 수 + 판단 보류 건만 보고 → 사용자 승인 후 Phase 3b 진입.
