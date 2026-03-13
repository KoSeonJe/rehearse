# Frontend 컨벤션

> 유지보수와 협업을 위한 프론트엔드 코딩 규칙.
> 공통 규칙(브랜치, 커밋, PR)은 `docs/conventions.md` 참조.
> 실전 구현 가이드는 `CODING_GUIDE.md` 참조.

---

## 파일/네이밍

| 대상 | 규칙 | 예시 |
|------|------|------|
| 파일명 | kebab-case | `interview-setup-page.tsx` |
| 컴포넌트 | PascalCase | `InterviewSetupPage` |
| 훅 | camelCase + `use` 접두사 | `useCreateInterview` |
| 타입/인터페이스 | PascalCase | `InterviewResponse` |
| 상수 | UPPER_SNAKE_CASE | `LEVEL_LABELS` |
| CSS 클래스 | Tailwind 유틸리티 | 인라인 사용 |

## 디렉토리 구조

```
frontend/src/
├── components/
│   ├── ui/              # 범용 재사용 (Button, TextInput, etc.)
│   ├── interview/       # 면접 도메인 컴포넌트
│   ├── review/          # 피드백 리뷰 도메인
│   └── mediapipe/       # MediaPipe 도메인
├── hooks/               # 커스텀 훅 (use-*.ts)
├── stores/              # Zustand 스토어 (*-store.ts)
├── lib/                 # 유틸리티, API 클라이언트
├── pages/               # 페이지 컴포넌트 (*-page.tsx)
└── types/               # TypeScript 타입 정의 (*.ts)
```

## 컴포넌트 패턴

```typescript
// Props 인터페이스는 컴포넌트 파일 내부에 정의
interface ButtonProps {
  variant?: 'primary' | 'secondary' | 'ghost' | 'cta';
  disabled?: boolean;
  children: React.ReactNode;
}

// 화살표 함수 + 함수형 컴포넌트
const Button = ({ variant = 'primary', disabled, children }: ButtonProps) => {
  return <button>...</button>;
};

export default Button;
```

**금지:**
- `any` 타입
- `console.log` (커밋 전 제거)
- barrel export (`index.ts`) — 직접 import
- class 컴포넌트

## 상태 관리

| 범위 | 도구 | 용도 |
|------|------|------|
| 서버 상태 | TanStack Query | API 데이터, 캐싱, 재시도 |
| 전역 클라이언트 | Zustand | 면접 진행 상태, 비디오 플레이어 |
| 로컬 | useState/useReducer | 폼 입력, UI 토글 |

## API 연동 패턴

```typescript
// hooks/use-*.ts에 커스텀 훅으로 캡슐화
const useCreateInterview = () => {
  return useMutation({
    mutationFn: (data: CreateInterviewRequest) =>
      apiClient.post<InterviewResponse>('/api/v1/interviews', data),
  });
};
```

## 디자인 시스템

- 디자인 토큰: `.omc/notepads/team/design-tokens.md` 참조
- 색상: slate 모노톤 (하드코딩 금지, Tailwind 클래스 사용)
- 간격: 8px 그리드 (Tailwind spacing scale)
- 폰트: Pretendard (한국어 최적화)
- 반응형: 모바일 퍼스트 (`sm:`, `md:`, `lg:` 접두사)
- 접근성: WCAG 2.1 AA, focus-visible, aria 속성
