# 프론트엔드 디자인 전환 — 요구사항 정의

> 상태: Draft
> 작성일: 2026-04-17

## Why

현재 Rehearse 프론트엔드는 다음 문제를 안고 있다.

1. **AI slop 규칙 정면 위반**: `accent: #6366F1`(Electric Violet)이 `.claude/rules/frontend-design-rules.md`의 "퍼플 primary 금지"와 충돌.
2. **디자인 기준 불일치**: `DESIGN.md`는 Cal.com 기반 모노크롬 시스템을 규정하지만, 실제 구현은 Toss 스타일(rounded 24px, 보라 accent, shadow-toss) 위에 Pretendard + 커스텀 UI 13개를 조합한 상태.
3. **토큰 거버넌스 부재**: shadcn/ui 미도입. CSS 변수 레이어 없음. 페이지마다 색상/spacing/radius가 하드코딩되어 드리프트 발생.
4. **페이지 간 일관성 부족**: 14개 페이지 각각 조금씩 다른 spacing·typography scale 사용.

### Decision Framework

- **Why?** 디자인 시스템이 기준 문서(DESIGN.md)와 괴리되어 있고, 퍼플 accent로 AI-generated SaaS 외형에 수렴 중.
- **Goal**: DESIGN.md(Cal.com 모노크롬) 방향으로 수렴. shadcn/ui를 primitive 표준으로 도입. 14페이지 레이아웃을 DESIGN.md spacing/typography scale에 맞춰 재정렬. 마지막에 일관성 감사로 드리프트 방지.
- **Evidence**: Explore 에이전트 사전 스캔 결과 — 13개 커스텀 UI(ui/ 디렉토리), 14개 페이지, tailwind.config.js의 `accent: #6366F1`, Pretendard + JetBrains Mono(Inter 미사용), shadcn 미설치.
- **Trade-offs**:
  - 포기: Toss스러운 rounded-24px 버튼 / Electric Violet 브랜드 인상 / Pretendard 단일 톤
  - 선택: Cal.com 모노크롬 + Pretendard(한글) + Cal Sans Display(영문 히어로) 하이브리드 + shadcn 점진 도입
  - 고려한 대안: (A) 퍼플 유지 + 사용 범위만 축소 — 규칙 위반 지속 / (B) 현재 커스텀 UI 유지 + 토큰만 재정비 — shadcn 생태계 이점 포기 / (C) 완전 전환(채택)

## 목표

1. DESIGN.md 기반 CSS 변수 토큰 시스템 (light/dark) 정의·적용
2. shadcn/ui로 primitive(Button/Input/Dialog/Card/Form) 표준화
3. 14개 페이지 전부 DESIGN.md spacing/typography 체계로 재정렬
4. frontend-design-rules.md 셀프체크 9질문 × 14페이지 매트릭스 통과
5. 랜딩 hero에 포인트 모션 1개(Aceternity) — 그 외 금지

**성공 기준**:
- `docs/design-audit.md`, `docs/consistency-issues.md` 생성 완료
- `npm run lint/build/test` 모든 Phase에서 green
- 기능 회귀 0건(수동 스모크 기준)
- 퍼플 accent 하드코딩 0건(`grep -r "#6366F1"` = 0)

## 아키텍처 / 설계

```
Design tokens (CSS vars, light/dark)
  └── tailwind.config.js (vars 참조형)
        └── shadcn/ui primitives (Button, Input, Dialog, Card, Form)
              └── Feature components (layout, home, dashboard, interview, feedback, ...)
                    └── Pages (14 routes)
```

- 레이어 간 방향은 단방향(tokens ← tailwind ← shadcn ← feature ← page)
- 하위 레이어가 상위 토큰을 참조만 하고 하드코딩하지 않도록 Phase 5에서 감사

## Scope

- **In**:
  - `frontend/src/**/*` 전체
  - `frontend/tailwind.config.js`, `frontend/src/index.css`
  - `frontend/components.json`(신규), `frontend/src/components/ui/` 재구성
  - 14개 페이지 레이아웃/typography/spacing
- **Out**:
  - backend, lambda, 인프라 변경
  - 라우팅·상태관리·API 호출·이벤트 핸들러 변경
  - 신규 기능 추가
  - `.env`, CI/CD 변경

## 제약조건 / 환경

- 패키지 매니저: **npm** 유지 (`npm run dev/build/lint/test`)
- 접근성: WCAG 대비 4.5:1 이상 유지
- 성능: Lighthouse 모바일 Performance 85+ 유지
- 모션: `prefers-reduced-motion` 대응 필수
- 언어: 한국어 서비스 — placeholder/에러 메시지 모두 한국어
- 커밋: 한국어 conventional commits, BE/FE PR 분리 규칙 준수
- 금기:
  - 퍼플(#6366F1/indigo/violet) primary
  - pure black/white(#000/#fff)
  - Inter/Roboto/Arial/Open Sans
  - `transition-all` 남발
  - Lorem ipsum, 영어 placeholder
  - Aceternity 페이지당 최대 1-2개 초과 사용
  - "한 번에 전 페이지" 리팩토링

## 관련 문서

- `/Users/koseonje/dev/devlens/DESIGN.md`
- `/Users/koseonje/dev/devlens/.claude/rules/frontend-design-rules.md`
- `/Users/koseonje/dev/devlens/docs/plans/Agents.md`
