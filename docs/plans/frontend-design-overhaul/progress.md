# 프론트엔드 디자인 전환 — 진행 상황

> 상태: Draft
> 작성일: 2026-04-17

## 태스크 상태

| # | 태스크 | 상태 | 비고 |
|---|--------|------|------|
| 1 | Audit (Phase 1) | Completed | `docs/design-audit.md` 생성, 코드 수정 없음 |
| 2 | Tokens + shadcn init (Phase 2) | Completed | CSS 변수 + shadcn init + Cal Sans 도입 |
| 3a | Button 교체 | Completed | shadcn Button 재정의 + 전체 교체 완료 |
| 3b | Input 교체 | Completed | shadcn Input/Label 설치 + TextInput 어댑터 재정의 + password-protected-route 교체 |
| 3c | Dialog 교체 | Draft | 3b 완료 후 |
| 3d | Card 교체 | Draft | 3c 완료 후 |
| 3e | Form 표준화 | Draft | [optional] audit 결과 기반 |
| 3f | 랜딩 페이지 레이아웃 | Draft | home |
| 3g | 대시보드/리뷰 리스트 레이아웃 | Draft | dashboard, review-list |
| 3h | 인터뷰 진행 레이아웃 | Draft | setup, ready, interview-page |
| 3i | 피드백/분석 레이아웃 | Draft | feedback, analysis |
| 3j | 정보/정적 페이지 | Draft | about, faq, guide, privacy, admin, 404 |
| 4 | Aceternity hero | Draft | [optional] 랜딩 hero 1곳 |
| 5 | 일관성 감사 | Draft | `docs/consistency-issues.md` |

## 체크포인트 원칙

- 각 태스크 종료 시 **사용자 리뷰 + git commit + 다음 진입 승인** 필수
- "한 번에 전 페이지/전 컴포넌트" 금지
- 판단 애매 건은 **옵션 제시 후 사용자 결정**

## 커밋 네이밍 규칙 (공통)

Phase별 커밋 메시지는 아래 패턴으로 통일한다 (한국어 conventional commits).

- Primitive 교체 (3a~3e): `{type}(fe): {scope} shadcn 교체`
  - 예) `fix(fe): 로그인 흐름 Button shadcn 교체`
  - 예) `fix(fe): 대시보드 Card shadcn 교체`
- 페이지 레이아웃 재구성 (3f~3j): `refactor(fe): {page} 레이아웃 재정렬`
  - 예) `refactor(fe): home hero 레이아웃 재정렬`
- 토큰/인프라 (Phase 2): `chore(fe): {subject}`
  - 예) `chore(fe): shadcn init + 디자인 토큰 도입`
- 감사 문서 (Phase 1, 5): `docs: {subject}`
  - 예) `docs: 디자인 상태 진단(audit) 결과 추가`

## 페이지별 세부 상태 (3f~3j)

| Phase | 페이지 | 상태 |
|-------|--------|------|
| 3f | home | Draft |
| 3g | dashboard | Draft |
| 3g | review-list | Draft |
| 3h | interview-setup | Draft |
| 3h | interview-ready | Draft |
| 3h | interview-page | Draft |
| 3i | interview-feedback | Draft |
| 3i | interview-analysis | Draft |
| 3j | about | Draft |
| 3j | faq | Draft |
| 3j | guide | Draft |
| 3j | privacy-policy | Draft |
| 3j | admin-feedbacks | Draft |
| 3j | not-found | Draft |

## 진행 로그

### 2026-04-17
- 플랜 디렉토리 생성: `docs/plans/frontend-design-overhaul/`
- 생성 파일: requirements.md + plan-01~05 + progress.md (16개)
- 승인 방향:
  - 폰트: Pretendard 유지 + Cal Sans Display 추가
  - Accent: Electric Violet 제거 → 모노크롬 전환
  - shadcn: Phase 2 말 init
  - 패키지: npm 유지
- Plan 01 Audit 완료: docs/design-audit.md 생성
- Plan 02 완료: CSS 변수 + shadcn init + Cal Sans 도입
- Plan 03a 완료: Button shadcn 교체 (커밋 8건, 판단 보류 12건 — 실측 `grep -rn "TODO(design)" frontend/src` 기준)
  - 교체 완료: home CTA/header, login-modal 닫기, dashboard 전반, setup-navigation, interview-ready, interview-analysis, admin-feedbacks 페이지네이션
  - 판단 보류: login OAuth 브랜드 버튼, sidebar nav active 스타일, step 선택 카드, feedback 탭/타임라인/플레이어, interview-page 스튜디오 UI, review-coach-mark 퍼플 그라디언트, review-list-filter-bar 칩
- Plan 03b 완료: Input shadcn 교체 (커밋 2건, 판단 보류 0건)
  - shadcn Input/Label 설치 + TextInput 어댑터 재정의 (1건)
  - password-protected-route 관리자 인증 Input 교체 + a11y 개선 (1건)
  - 인라인 유지(special): type="file" 1건, type="range" 1건
