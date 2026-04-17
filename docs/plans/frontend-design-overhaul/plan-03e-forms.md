# Plan 03e: Form 표준화 (react-hook-form + shadcn Form) [optional]

> 상태: Draft
> 작성일: 2026-04-17

## Why

폼이 존재한다면 `react-hook-form` + shadcn `Form`으로 validation/에러 메시지/a11y를 일원화. Phase 1 audit에서 기존 폼 수가 적다고 판단되면 skip 가능한 optional 단계.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| (audit 결과 기반) | shadcn `Form` 도입 여부 및 대상 결정 |
| `frontend/package.json` | `react-hook-form`, `@hookform/resolvers`, `zod` (미설치 시) |

## 상세

### 조건 분기

- **폼 수 ≥ 3개 + 유효성 복잡도 높음**: `Form` 도입 진행
- **폼 수 < 3 or 단순**: skip, 기존 구조 유지

### 도입 시 작업

```bash
cd frontend
npx shadcn@latest add form
npm i react-hook-form @hookform/resolvers zod
```

- 각 폼을 `useForm` + `zodResolver` + shadcn `Form`/`FormField`로 재구성
- 에러 메시지는 한국어 유지
- submit 핸들러 로직 불변

## 담당 에이전트

- Implement: `frontend` — 폼 재구성
- Review: `code-reviewer` — 유효성 로직, 에러 메시지, a11y

## 검증

- audit 결과 기반 의사결정 기록
- 도입 시: `npm run lint/build/test` green, 수동 스모크
- skip 시: `progress.md`에 "Skip 사유" 기록
- `progress.md` Task 3e → Completed 또는 Skipped

## 체크포인트

도입/skip 결정 보고 → 사용자 승인 후 Phase 3f 진입.
