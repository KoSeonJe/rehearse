# Admin Question Pool Status Column Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 질문 풀 어드민 테이블의 `상태` 열 텍스트가 세로로 줄바꿈되지 않고 한 줄 가로 텍스트로 표시되게 한다.

**Architecture:** 기존 `/admin/question-pool` 테이블 구조를 유지하고 `상태` 헤더/셀에만 폭과 줄바꿈 방지 스타일을 추가한다. 모바일 카드 UI와 API 동작은 변경하지 않는다.

**Tech Stack:** React 18, TypeScript, Tailwind CSS, Vitest/Testing Library.

---

## Context

- Issue: #498
- Branch: `fix/498-admin-question-pool-status-column`
- Worktree: `/private/tmp/rehearse-498-fe`
- Target file: `frontend/src/pages/admin-question-pool-page.tsx`

## Task 1: 상태 열 가로 정렬 수정

**Files:**
- Modify: `frontend/src/pages/admin-question-pool-page.tsx`
- Test: `frontend/src/pages/__tests__/admin-question-pool-page.test.tsx`

- [ ] **Step 1: Write the failing test**

Add an assertion to the existing render test that verifies the status cell uses a no-wrap class:

```tsx
expect(screen.getByText('활성')).toHaveClass('whitespace-nowrap')
```

- [ ] **Step 2: Run the focused test to verify it fails**

Run:

```bash
cd frontend
npm run test -- src/pages/__tests__/admin-question-pool-page.test.tsx
```

Expected: FAIL because the status cell currently lacks `whitespace-nowrap`.

- [ ] **Step 3: Apply minimal table style fix**

Change only the desktop table status column:

```tsx
<th className="w-16 whitespace-nowrap px-4 py-3 text-left font-semibold text-text-secondary">상태</th>
...
<td className="w-16 whitespace-nowrap px-4 py-3 text-text-secondary">{statusLabel(item.isActive)}</td>
```

This keeps `활성`/`비활성` on one line and prevents the status column from collapsing to one-character width.

- [ ] **Step 4: Run focused verification**

Run:

```bash
cd frontend
npm run test -- src/pages/__tests__/admin-question-pool-page.test.tsx
```

Expected: PASS.

- [ ] **Step 5: Run frontend checks**

Run:

```bash
cd frontend
npm run lint
npm run build
```

Expected: PASS. If build fails with local prerender `listen EPERM`, rerun with the same command under approved sandbox escalation.

- [ ] **Step 6: Commit**

Run:

```bash
git add docs/plans/498-admin-question-pool-status-column/implement-fe.md frontend/src/pages/admin-question-pool-page.tsx frontend/src/pages/__tests__/admin-question-pool-page.test.tsx
git commit -m "fix(FE): 질문 풀 상태 열 줄바꿈 방지"
```

## Review Notes

- Scope is FE-only.
- No API or DB changes.
- No mobile card behavior changes.
