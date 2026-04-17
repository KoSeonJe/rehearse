# Plan 03c: Dialog/Modal shadcn 교체

> 상태: Completed
> 작성일: 2026-04-17

## Why

`login-modal.tsx` 등 커스텀 모달이 포커스 트랩/ESC 처리/접근성 속성을 각기 다르게 구현. Radix 기반 shadcn `Dialog`/`AlertDialog`로 통일해 접근성과 일관성을 확보.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `frontend/src/components/ui/login-modal.tsx` | shadcn `Dialog` 기반 재구현 |
| 기타 모달 컴포넌트 | Dialog/AlertDialog 선택해 교체 |

## 상세

### 1. shadcn Dialog / AlertDialog 설치

```bash
cd frontend
npx shadcn@latest add dialog alert-dialog
```

### 2. 매핑 규칙

| 용도 | shadcn |
|------|--------|
| 일반 모달 (로그인, 정보 입력) | `Dialog` |
| 확인성 모달 (삭제, 탈퇴) | `AlertDialog` |

### 3. 제약

- 열림/닫힘 상태 관리 로직 **변경 금지**
- `onOpenChange`, `open` 제어형 prop 유지
- 내부 본문은 Phase 3a/3b에서 이미 교체된 Button/Input을 그대로 사용

## 담당 에이전트

- Implement: `frontend` — 모달 재구현
- Review: `code-reviewer` — 포커스 트랩, ESC 닫힘, scroll lock, a11y 속성

## 검증

- `npm run lint/build/test` green
- 수동 스모크: 로그인 모달 열림/ESC 닫힘/바깥 클릭 닫힘/포커스 복귀
- 키보드 네비게이션(Tab/Shift+Tab) 루프 확인
- `progress.md` Task 3c → Completed

## 체크포인트

변경 파일 수 보고 → 사용자 승인 후 Phase 3d 진입.
