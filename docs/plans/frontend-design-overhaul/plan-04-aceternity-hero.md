# Plan 04: Aceternity 포인트 효과 (랜딩 hero 1곳)

> 상태: Draft
> 작성일: 2026-04-17

## Why

모노크롬 중심 디자인은 정교함은 있지만 첫인상이 밋밋할 수 있다. 랜딩 hero **단 1곳**에 절제된 모션 포인트를 추가해 기억점을 만든다. 페이지당 2개 이상 금지(frontend-design-rules).

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `frontend/src/components/ui/` (신규) | 선택된 Aceternity 컴포넌트 포트 |
| `frontend/src/pages/home/**` | hero 섹션에 포인트 적용 |

## 상세

### 접근 경로 (Lean vs Full)

**Lean 경로 (권장 기본값)**
1. Spotlight 1종만 POC 구현 후 랜딩 hero에 적용
2. 사용자 피드백 수집 → 만족 시 그대로 확정
3. 불만족 시에만 아래 Full 경로로 전환

**Full 경로 (Lean에서 교체가 필요한 경우만)**
- 아래 3종 중 나머지 2종도 구현·시연 → 사용자 선택

### 옵션 3종 비교

| 옵션 | 설명 | 성능 | 모노크롬 정합성 | 추천 |
|------|------|------|----------------|------|
| **Spotlight** | 커서 추적 스포트라이트 그레이디언트 | 낮음(정적) | 높음 | ✅ Lean 기본 |
| **BackgroundBeams** | 방사형 beams 애니메이션 | 중간 | 중간 | Full only |
| **TextGenerateEffect** | 헤드라인 타이핑 애니 | 낮음 | 높음 | 대안 |

### 필수 조건

- `prefers-reduced-motion: reduce` 환경 → 정적 폴백
- 모바일 저사양 대응:
  - `matchMedia('(max-width: 768px)').matches` 시 단순 정적 버전
  - 또는 `navigator.hardwareConcurrency < 4` 분기
- 랜딩 hero 내 `glow-pulse`, `ripple` 등 다른 무한 애니 **동시 노출 금지**
- 퍼플 그레이디언트 금지 — 모노크롬 값만 사용
- 프로덕션 번들에 Aceternity 전체가 들어가지 않도록 필요 컴포넌트만 포트

### 도입 흐름

**Lean (기본)**
1. Spotlight만 `components/ui/`에 포트 (외부 의존 최소화)
2. 접근성/성능 조건 구현 → 랜딩 hero 적용
3. Phase 3f 결과와 충돌 없는지 확인 → 사용자 피드백 수집
4. 수용 시 종료. 거부 시 Full 경로로 전환

**Full (조건부)**
1. 나머지 2종 옵션을 구현/목업으로 시연 → 사용자 선택
2. 선택된 컴포넌트로 교체 → 재검증

## 담당 에이전트

- Implement: `designer` — 모션 구현, 옵션 시연
- Review: `frontend` — 성능, 번들 사이즈, reduced-motion

## 검증

- Lighthouse 모바일 Performance 85+ 유지 (적용 전/후 비교)
- DevTools `prefers-reduced-motion` emulate → 정적 상태 확인
- 모바일(360px, throttling 4x CPU) 체감 끊김 없음
- 번들 사이즈 증가 `< 30KB gz`
- 랜딩 hero 외 다른 페이지에 해당 효과 미사용(grep)
- `progress.md` Task 4 → Completed 또는 Skipped

## 체크포인트

옵션 시연 → 사용자 선택 → 적용 후 구조 요약/스크린샷 보고.
