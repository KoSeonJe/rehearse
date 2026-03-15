# FE 코드 리팩토링

- **Status**: Completed
- **Created**: 2026-03-10
- **Branch**: refactor/fe-cleanup

## 목표
MVP 완료 후 프론트엔드 코드 품질 개선. API 계약 변경 없음.

## 작업 항목

### F1: 온보딩 페이지 컴포넌트 분리
- `pages/onboarding-page.tsx` (571줄) → 서브컴포넌트 분리
- `StepJobField`, `StepDeviceTest`, `StepGuide`, `ProgressBar` → `components/onboarding/`
- `useDeviceTest` → `hooks/use-device-test.ts`
- 타입/상수 → `components/onboarding/constants.ts`, `components/onboarding/types.ts`

### F2: 면접 페이지 로직 추출
- `pages/interview-page.tsx` (266줄) 7개 useEffect + 콜백
- → `hooks/use-interview-session.ts` 커스텀 훅으로 추출

### F3: 인라인 SVG 아이콘 분리
- `home-page.tsx`, `onboarding-page.tsx`의 인라인 SVG
- → `components/icons/` 재사용 컴포넌트

### F4: export 스타일 통일
- `export default` → named export 통일 (23개 파일)
- `app.tsx` import 업데이트

### F5: interview-store DRY
- 반복되는 answers 업데이트 패턴 → 헬퍼 함수 추출

### F6: FE 테스트 기반
- `api-client` 테스트, 주요 훅 테스트 추가

## 검증
- `npm run build` 성공
- `npm run lint` 통과
