# Plan 03b: Input 컴포넌트 shadcn 교체

> 상태: Draft
> 작성일: 2026-04-17

## Why

`text-input.tsx` 커스텀 구현 + 페이지 내 `<input>` 인라인이 혼재. shadcn `Input`으로 표준화해 스타일/포커스/에러 표현을 일원화한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `frontend/src/components/ui/text-input.tsx` | shadcn `Input` 래핑 또는 대체 |
| `frontend/src/components/**/*.tsx` | 인라인 `<input>` 교체 |
| `frontend/src/pages/**/*.tsx` | 페이지 내 `<input>` 교체 |

## 상세

### 1. shadcn Input / Label 설치

```bash
cd frontend
npx shadcn@latest add input label
```

### 2. 교체 범위

- `text-input.tsx`: shadcn `Input`으로 내부 구현 교체, 기존 외부 API(props) 최대한 유지
- 검색 인풋, 로그인 이메일/비밀번호, 프로필 편집, 피드백 입력 등

### 3. 제약

- `placeholder`는 **한국어 유지** (프로젝트 규칙)
- `onChange`, `value`, `defaultValue`, 유효성 로직 **변경 금지**
- `aria-label`, `aria-invalid` 등 접근성 속성 보존
- 에러 상태 표시는 `destructive` 토큰 활용

## 담당 에이전트

- Implement: `frontend` — 컴포넌트 교체
- Review: `code-reviewer` — 폼 검증 동작 회귀, a11y 유지

## 검증

- `npm run lint/build/test` green
- 수동 스모크: 로그인, 검색, 프로필 편집 입력 동작 확인
- placeholder가 영어로 바뀌지 않았는지 grep 확인
- `progress.md` Task 3b → Completed

## 체크포인트

변경 파일 수 보고 → 사용자 승인 후 Phase 3c 진입.
