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
| 3c | Dialog 교체 | Completed | shadcn Dialog/AlertDialog 교체 완료 (커밋 4건, 판단 보류 3건) |
| 3d | Card 교체 | Completed | shadcn Card 설치 + 7개 영역 교체 (커밋 7건, 이관 5건) |
| 3e | Form 표준화 | Skipped | 폼 2개(password-protected-route, interview-setup-page) < 3 기준, 유효성 로직 단순 — react-hook-form 도입 이득 미미 |
| 3f | 랜딩 페이지 레이아웃 | Completed | home (보수 범위) |
| 3g | 대시보드/리뷰 리스트 레이아웃 | Completed | dashboard, review-list |
| 3h | 인터뷰 진행 레이아웃 | Completed | setup, ready, interview-page |
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
| 3f | home | Completed |
| 3g | dashboard | Completed |
| 3g | review-list | Completed |
| 3h | interview-setup | Completed |
| 3h | interview-ready | Completed |
| 3h | interview-page | Completed |
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
- Plan 03c 완료: Dialog/AlertDialog shadcn 교체 (커밋 4건, 판단 보류 3건)
  - 교체 완료: login-modal(Dialog), delete-confirm-dialog(AlertDialog), service-feedback-modal(Dialog)
  - 판단 보류(TODO(plan-03c)): upload-recovery-dialog, interview-page finish-dialog, exit-guard — 스튜디오 다크 테마 영역, Phase 3a 범위로 디자인 검수 후 교체
- Plan 03b 완료: Input shadcn 교체 (커밋 2건, 판단 보류 0건)
  - shadcn Input/Label 설치 + TextInput 어댑터 재정의 (1건)
  - password-protected-route 관리자 인증 Input 교체 + a11y 개선 (1건)
  - 인라인 유지(special): type="file" 1건, type="range" 1건
- Plan 03d 완료: Card shadcn 교체 (커밋 7건, 판단 보류/이관 7건)
  - 교체 완료: SelectionCard, 대시보드(stats-cards/interview-card/interview-table/interview-list), 홈(pain-points/before-you-start/metrics), 리뷰(review-bookmark-card), 인터뷰(question-card/question-display/question-card-skeleton), 어드민(admin-feedbacks), password-protected-route
  - shadow-toss → shadow-sm/shadow-md, rounded-card → rounded-lg(shadcn default)
  - Phase 3f/3i 이관(TODO 마킹): hero-section rounded-[32px], journey-section rounded-[32px], dev-tailored-section rounded-[32px], login-modal DialogContent rounded-[28px], sidebar shadow-toss
- Plan 03e Skip: 프로젝트 내 폼 2개(password-protected-route, interview-setup-page) 모두 단순 제어형 + 로컬 상태 기반. react-hook-form + zod 도입은 ROI 부족 — Phase 5 재평가
  - Plan 03c 이관: delete-confirm-dialog/service-feedback-modal의 rounded-card/shadow-toss-lg (Dialog 스타일)
- Plan 03f 완료 (보수 범위): 홈 퍼플 제거 + 3-col 그리드 해체 + 레이블 모노크롬화 (커밋 5건)
  - 커밋 1 (7dba625): 홈 랜딩 퍼플 하드코딩 제거 — SVG stroke/fill #6366F1 → currentColor, violet-legacy → 모노크롬 토큰 (9파일)
  - 커밋 2 (239e539): pain-points-section 3-column 그리드 → 세로 stack (번호+아이콘 좌측 / 콘텐츠 우측)
  - 커밋 3 (95d63a9): 홈 font-mono 레이블 → text-xs font-semibold uppercase tracking-wider text-muted-foreground (6파일)
  - 커밋 4 (66c0d4e): Hero Aceternity 삽입 위치 TODO 주석 + h1 violet-legacy 잔여 제거
  - 커밋 5 (e5578a4): home-page.tsx violet-legacy 잔여 4곳 제거
  - 이관 항목: Hero typography 재구성(64px Cal Sans 등) → Phase 4/5, Aceternity 인터랙션 → Plan 04
- Plan 03g 완료 (보수 범위): 대시보드/리뷰 퍼플 제거 + shadow/색상 토큰 정합 (커밋 3건)
  - 커밋 1 (e7d882a): 대시보드/리뷰 퍼플 하드코딩 제거 — interview-table indigo/violet 배지 → bg-secondary, sidebar #EEF2FF/#4F46E5 → violet-legacy 토큰, filter-bar #6366F1 focus-ring ×3 → ring-ring, #334155/#E2E8F0/bg-white → 토큰
  - 커밋 2 (d428ac8): sidebar shadow-toss → shadow-sm (TODO(plan-03g) 해소)
  - 커밋 3 (06be281): dashboard-header bg-white/95 → bg-background/95, review-empty-state bg-white → bg-card, answer-comparison-view border-slate-100/bg-slate-50/text-slate-400 → 토큰
  - 이관 항목: delete-confirm-dialog/service-feedback-modal bg-white/shadow-toss-lg → TODO(plan-05) 마킹 유지
- Plan 03h 완료 (보수 범위): setup/ready 퍼플 제거, 레이블 모노크롬화, transition-all 정리, 토큰 정합. interview-page 스튜디오 UI 보존 (커밋 4건)
  - 커밋 1 (5f188c7): 인터뷰 setup/ready 퍼플/violet 하드코딩 제거 — ready-page h1/태그/스피너/재시도, resume-upload 드래그/아이콘, speaker-test-row 들려요 버튼, mic-test-row 레벨 바, audio-waveform idle fill, question-card/display 번호 뱃지, step-tech-stack 기본 뱃지, step-interview-type 체크박스/CS 태그, step-level 체크 아이콘 (10파일)
  - 커밋 2 (b730824): setup font-mono 레이블 → text-xs font-semibold uppercase tracking-wider text-muted-foreground (step 5개 + ready-page, 6파일)
  - 커밋 3 (783c83c): transition-all → 구체 속성 — setup 선택 카드/progress-bar/resume-upload + camera/mic/speaker-test-row + interviewer-avatar (11파일)
  - 커밋 4 (1b7c1a5): bg-white → bg-background/bg-card 토큰 정합 — setup-page/ready-page + 3개 test-row (5파일)
  - interview-page.tsx 변경 라인: 0 (스튜디오 다크 UI 완전 보존)
  - 의도 잔존(Phase 5): setup step active bg-violet-legacy, setup-progress-bar bg-violet-legacy
