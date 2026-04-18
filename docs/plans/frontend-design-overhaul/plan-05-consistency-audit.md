# Plan 05: 디자인 일관성 감사 (최종)

> 상태: Draft
> 작성일: 2026-04-17

## Why

Phase 2~4 작업 후에도 누락된 하드코딩, 예외 케이스, 페이지별 드리프트가 남을 수 있다. 최종 감사로 이슈 목록을 만들고, 수정 우선순위는 사용자가 결정한다(수정 자체는 이 플랜 범위 밖).

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `docs/consistency-issues.md` | 감사 결과 이슈 목록 (심각도/위치/수정 범위) |

## 상세

### 감사 체크리스트

1. **하드코딩 색상 잔존**
   - `grep -rEn "#[0-9a-fA-F]{3,8}" frontend/src`
   - `rgb(`, `rgba(`, `hsl(` 직접 사용
   - Tailwind 임의값 `bg-[#..]` 패턴
2. **폰트 규칙 위반**
   - `Inter`, `Roboto`, `Arial`, `Open Sans`, `Lato` 문자열 잔존
3. **`transition-all` 남발**
   - `className`에 `transition-all` 직접 사용 전수
4. **pure black/white**
   - `#000`, `#fff`, `#000000`, `#ffffff`, `bg-black`, `bg-white`, `text-black`, `text-white` 잔존
5. **영어 placeholder / Lorem ipsum**
   - `placeholder=".*[A-Za-z]"`에서 한국어가 아닌 것
   - `Lorem ipsum`, `Product Name`, `Lorem Company` 문자열
6. **legacy 토큰 잔존 확인**
   - `grep -rn "violet-legacy" frontend/src` = 0건
   - `grep -rn "#6366F1" frontend` = 0건 (대소문자 무시)
   - `tailwind.config.js`에서 `violet-legacy` 정의도 제거됨
7. **접근성 자동 검증**
   - `npx @axe-core/cli` 또는 Playwright axe 통합으로 14페이지 스캔
   - WCAG 2.1 AA 기준 violation 기록 (심각도 critical/serious는 모두 H 우선순위)
   - Lighthouse a11y 점수 90+ 유지
8. **frontend-design-rules.md 셀프체크 9질문 × 14페이지 매트릭스**
   - 질문 1: primary가 퍼플/indigo인가?
   - 질문 2: 폰트가 Inter/Roboto인가?
   - 질문 3: hero 아래 3-column 아이콘 그리드인가?
   - 질문 4: 모든 카드에 backdrop-blur인가?
   - 질문 5: 다크모드에 pure black/white?
   - 질문 6: `transition-all` 남용?
   - 질문 7: Lorem ipsum / 영어 placeholder 잔존?
   - 질문 8: 회사명만 바꾸면 다른 SaaS로 써도 되는 느낌?
   - 질문 9: "기억에 남는 하나"를 명명 가능한가?

   14페이지: home, dashboard, interview-page, interview-setup, interview-ready, interview-feedback, interview-analysis, review-list, about, admin-feedbacks, faq, guide, privacy-policy, not-found

### 결과 문서 포맷

```markdown
# Consistency Audit Issues

## Summary
- 총 이슈 N건 (H: x, M: y, L: z)
- 페이지별 통과율 테이블

## 이슈 목록

### H1. 퍼플 accent 잔존
- 위치: `frontend/src/pages/xxx/...`:line
- 현재: `#6366F1`
- 목표: `hsl(var(--primary))`
- 심각도: H
- 예상 수정 범위: S
```

## 담당 에이전트

- Implement: `code-reviewer` (Opus) — 전수 grep + 매트릭스 작성
- Review: 사용자 — 우선순위 결정

## 검증

- `docs/consistency-issues.md` 생성
- 14페이지 × 9질문 매트릭스 모두 채워짐
- 자동 체크 결과 기록: 5개 grep(색상/폰트/transition/black-white/placeholder) + legacy 토큰 + axe-core
- `violet-legacy` 사용처 0건 확인 후 `tailwind.config.js`/`index.css`에서도 정의 제거 완료
- axe-core critical/serious violation 0건 (또는 잔존 시 이슈 등록)
- **수정은 수행하지 않음** — 사용자가 우선순위 확정 후 별도 작업
- `progress.md` Task 5 → Completed

## 체크포인트

이슈 목록 보고 → 사용자가 어디까지 수정할지 결정 → 후속 작업은 별도 스펙으로.
