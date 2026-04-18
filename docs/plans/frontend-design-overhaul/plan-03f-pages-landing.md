# Plan 03f: 랜딩 페이지(home) 레이아웃 재구성

> 상태: Draft
> 작성일: 2026-04-17

## Why

Primitive 교체(Phase 3a~e)만으로는 페이지 레이아웃/typography hierarchy/섹션 spacing이 DESIGN.md(Cal.com 모노크롬) 기준에 도달하지 않는다. 랜딩은 첫인상을 결정하므로 가장 먼저 재구성한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `frontend/src/pages/home/**` | hero, feature 블록, CTA, footer 섹션 레이아웃 재정렬 |
| `frontend/src/components/home/**/*.tsx` | feature 카드, hero 서브 컴포넌트 재정렬 |

## 상세

### 대상 섹션

1. **Hero**: 헤드라인(영문 Cal Sans 64px / 한글 Pretendard) + 서브 카피(Pretendard) + primary CTA
2. **Feature 블록**: 3-column 아이콘 그리드 **금지** (frontend-design-rules.md) → 비대칭/스토리텔링 레이아웃
3. **Proof / Trust 섹션** (있다면): 근거 있는 콘텐츠만, "Loved by 10,000+" 같은 가공된 social proof 금지
4. **CTA 섹션**: 단일 primary CTA
5. **Footer**: 공통 레이아웃

### 변경 범위

- **레이아웃 그리드**: DESIGN.md 80~96px 섹션 spacing 적용
- **Typography scale**:
  - Hero headline: Cal Sans 64px / 1.10 / weight 600 (영문), Pretendard 50~56px (한글)
  - Section heading: Cal Sans 48px / 1.10
  - Feature heading: 24px
  - Body: Pretendard 16~18px
- **색상**: 모노크롬 토큰 (`--background`, `--foreground`, `--muted-foreground`)
- **하드코딩 제거**: `#6366F1`, pure black/white 치환
- **Aceternity 자리만 준비** (Phase 4에서 실제 효과 삽입)

### 비변경

- 라우팅, 상태, API, 이벤트 핸들러, 분석 이벤트 로깅

## 담당 에이전트

- Implement: `frontend` + `designer` — 레이아웃·typography 구성
- Review: `designer` — DESIGN.md 원칙 준수, 셀프체크 9질문
- Review: `code-reviewer` — 코드 품질, 회귀

## 검증

- `npm run lint/build/test` green
- 스크린샷 before/after 첨부
- Lighthouse 모바일 Performance 85+ 유지
- frontend-design-rules 셀프체크 9질문 통과 (랜딩)
- CTA 클릭 → 기존 라우팅 그대로 동작
- `progress.md` Task 3f → Completed

## 체크포인트

before/after 스크린샷 + 회귀 여부 보고 → 사용자 승인 후 Phase 3g 진입.
