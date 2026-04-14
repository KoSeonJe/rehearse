# Plan 16: BETA 배지 로고 근처 노출

> 상태: Completed (2026-04-14)
> 작성일: 2026-04-14

## Why

Rehearse는 prod 런칭 시점에도 여전히 베타 단계다. 사용자가 이를 인지하지 못하면 다음 문제가 생긴다:

- 완성도 기대치 불일치로 불만 유발 (특히 오류·느린 피드백)
- 데이터 손실 리스크에 대한 사용자 동의·자각 부족
- 피드백 수집 창구로 사용되고 있다는 컨텍스트 누락

로고 바로 옆에 `BETA` 배지를 노출해 사용자가 **서비스 진입 시 즉시 베타임을 인식**하도록 한다. 개인정보 처리방침(plan-17)의 베타 고지와 톤을 맞춘다.

## 생성/수정 파일

| 파일 | 작업 |
|---|---|
| `frontend/src/components/ui/beta-badge.tsx` | 신규 — BetaBadge 컴포넌트 |
| `frontend/src/pages/home-page.tsx` | "리허설" 뒤 `<BetaBadge size="md" />` 삽입 |
| `frontend/src/components/dashboard/dashboard-header.tsx` | "리허설" 뒤 `<BetaBadge size="md" />` 삽입 |
| `frontend/src/components/dashboard/sidebar.tsx` | "리허설" 뒤 `<BetaBadge size="sm" />` 삽입 |
| `frontend/src/components/ui/login-modal.tsx` | "리허설" 뒤 `<BetaBadge size="sm" />` 삽입 |

## 상세

### 컴포넌트 API

```tsx
interface BetaBadgeProps {
  size?: 'sm' | 'md'  // sm=9px/작은 로고 (sidebar/login-modal), md=10px/큰 로고 (home/dashboard-header)
  className?: string
}
```

스타일: `bg-primary/10 text-primary border border-primary/30 rounded-full font-bold uppercase tracking-wider`
a11y: `aria-label="베타 서비스"` + `title="정식 출시 전 베타 서비스입니다"`

### 배지 크기 기준

| 사용처 | 로고 크기 | 배지 size |
|---|---|---|
| home-page 헤더 | 80px | `md` |
| dashboard-header | 80px | `md` |
| sidebar | 36px | `sm` |
| login-modal | 56px | `sm` |

## 담당 에이전트

- Implement: `frontend` (executor 위임 완료)

## 검증

- `cd frontend && npm run lint` 통과 ✅
- `cd frontend && npm run build` 통과 ✅
- 4개 화면에서 배지 노출 (home / dashboard / sidebar / login modal)
- 반응형 한 줄 유지 (줄바꿈 없음)
