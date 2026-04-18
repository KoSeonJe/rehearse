# [FE] 전 페이지 사용자 워크스루 피드백 기반 UX 개선

- **Status**: In Progress (2026-04-18 — Tier 0 완료, Tier 1/2 부분 반영)
- **Created**: 2026-04-18
- **Owner**: designer + frontend
- **Related**: `2026-04-18-product-ui-redesign.md`(Quiet Rigor 토큰), `2026-04-14-landing-copy.md`
- **Source**: 2026-04-18 Playwright 실제 브라우징 (홈/About/FAQ/Privacy/Guide/404/로그인 모달/Setup 5단계/Dashboard/Review List/Interview ready·conduct·analysis·feedback)

---

## 1. Why

실사용자 시점으로 전 페이지를 돌며 관찰한 결과, 디자인 토큰 레벨이 아닌 **플로우·상태·마이크로카피 레벨의 깨짐**이 다수 확인됐다. Quiet Rigor 리디자인은 시각 토큰을 정렬하지만, 이번에 발견된 문제들은 대부분 "보이느냐/안 보이느냐", "선택 상태와 호버 상태 구분", "에러·로딩 상태에서 사용자가 뭘 해야 하는가"처럼 **정보 설계 레벨**에서 시정해야 한다. 시각 리디자인과 별도 트랙으로 돌려야 릴리즈 병렬화 가능.

### 특히 제품 신뢰도를 깎는 Critical 3건

| # | 문제 | 위치 | 사용자 체감 |
|---|------|------|-------------|
| C1 | `interview-conduct` 로딩에 **검정 배경 + 검정 텍스트 "준비 중"** | `/interview/:id/conduct` | "앱이 죽었나?" 오해 → 즉시 이탈. WCAG contrast 위반 |
| C2 | 미로그인 상태에서 보호 페이지 진입 시 "**세션이 만료되었습니다. 다시 로그인해주세요**" | `useAuthInterceptor` + `ProtectedRoute` 레이스 | 신규 가입 유도 경로에서 "세션?"이라는 틀린 프레임을 제시 |
| C3 | Setup Step 1에서 아직 고르지도 않은 **"수준·JUNIOR / 시간·30분"이 요약에 이미 표시** | `interview-setup-page` 선택 내역 박스 | 결정 프로세스의 신뢰 상실. 실제로 사용자가 뭘 선택했는지 불명 |

### 측정 목표

- 보호 페이지 진입 시 오해 소지 문구 제거 → Task C2 완료
- `interview-conduct` 로딩 상태 가독성 WCAG AA 통과 → Task C1 완료
- Setup wizard 완주율 +10%p (현재값 측정 후 baseline 확정 → fix 후 7일 moving avg 비교)
- 에러 상태에서 "홈으로 돌아가기"만 있는 페이지 0개 (대안 경로 복수 제공)

---

## 2. Scope

### In
- 홈, About, FAQ, Privacy, 404, Guide 3종, 로그인 모달
- Setup 5 steps, Dashboard, Review List
- Interview ready/conduct/analysis/feedback 의 **로딩/에러 상태**
- 전역 로딩 문구, 에러 일러스트, sidebar/header 일관성

### Out
- 시각 토큰 재정의(이미 `2026-04-18-product-ui-redesign.md`가 담당)
- 백엔드 API 변경
- 신규 데이터 수집/분석 기능

---

## 3. Priority Tiers

### Tier 0 — Critical (즉시, 1일 내)
C1, C2, C3 — 위 표 참고.

### Tier 1 — High (이번 스프린트)
로그인/인증 레이스, 에러 페이지 대안 경로, Setup 선택 상태 시각 구분, Dashboard 카드 식별성, 히어로 Mock 프리뷰 품질, 스크롤 reveal 타이밍.

### Tier 2 — Medium (다음 스프린트)
FAQ 아코디언/검색, 가이드 페이지 TOC·이미지, About 히어로 서사, 헤더 로고 캐릭터 톤, 푸터 정보 보강, `← 뒤로` vs `← 홈으로` 통일, 페이지 title 중복 제거.

### Tier 3 — Low (백로그)
로그인 모달 focus outline 커스텀, 로딩 바 진행 단계 시각화, 약관 indigo 링크 뉴트럴화, BETA 배지 일관성, `console.error 401` 조용히 처리.

---

## 4. Task Breakdown

### Task 0.1 — `interview-conduct` 로딩 contrast 버그
- **Priority**: Critical (C1)
- **Implement**: `frontend` — `interview-page.tsx`의 로딩 branch 배경 `bg-black` → `bg-interview-stage/90`(실제 극장 배경과 동일 톤)으로 통일하고, `"준비 중"` 텍스트는 `text-neutral-100` + spinner 병치
- **Review**: `code-reviewer` — contrast 4.5:1 검증, a11y-axe 통과
- **Verify**: `qa` — 카메라 권한 거부·허용 두 흐름에서 로딩 상태 모두 가시성 확인

### Task 0.2 — 미로그인 상태 "세션 만료" 오문구 제거
- **Priority**: Critical (C2)
- **Implement**: `frontend` — `use-auth-interceptor.ts`의 핸들러에서 "이전에 인증된 적이 있는 사용자"인지 구분하여 초회 401은 `ProtectedRoute`에 위임. 구현안: `queryClient.getQueryData(AUTH_QUERY_KEY)`가 유효한 user였을 때만 "세션 만료" 메시지 표시, 그 외는 no-op
- **Review**: `architect-reviewer` — ProtectedRoute와 interceptor 책임 경계 재설계

### Task 0.3 — Setup Step 1 요약 박스 기본값 표시 버그
- **Priority**: Critical (C3)
- **Implement**: `frontend` — `interview-setup-page.tsx`의 좌측 "선택 내역" 박스에서 **사용자가 실제로 선택한 단계만** 렌더. 미선택 항목은 회색 플레이스홀더(`직무 미선택`)로. enum 영문(`BACKEND/JUNIOR`)을 한글 라벨로 매핑
- **Review**: `designer` — 상태 3종(미선택/선택됨/기본값 암시) 시각 구분

### Task 1.1 — 홈 Mock 프리뷰 리디자인 `[parallel]`
- **Implement**: `designer` → `frontend` — 히어로 오른쪽 mock 프리뷰를 실제 피드백 화면 스크린샷 기반 정지 이미지 또는 짧은 Lottie로 교체. 회색 아바타 실루엣 + 의미 없는 검은 바 제거
- **Review**: `designer` — 브랜드 톤 일관성

### Task 1.2 — 홈 스크롤 reveal 타이밍 `[parallel]`
- **Implement**: `frontend` — IntersectionObserver threshold를 현재 `0.3` 추정 → `0` + `rootMargin: "-5% 0px"`로 조정하고, 초기 뷰포트 안 섹션은 `opacity 1` 보장. 빠른 스크롤 시 섹션 "안 뜸" 문제 해결
- **Verify**: `qa` — 트랙패드·휠·모바일 스와이프 3가지 입력에서 전 섹션 출현 보장

### Task 1.3 — Setup 카드 hover vs selected 상태 분리
- **Implement**: `designer` + `frontend` — hover: `ring-1 ring-neutral-300`, selected: `bg-neutral-900 text-neutral-0 + checkmark`. Multi-select 스텝은 사각 체크박스로 전환 (현재는 원형이라 radio로 오인)
- **Files**: `step-position.tsx`, `step-tech-stack.tsx`, `step-level.tsx`, `step-duration.tsx`, `step-interview-type.tsx`
- **Review**: `code-reviewer` — 5개 스텝 컴포넌트 일관성

### Task 1.4 — Setup "다음 단계" disabled affordance
- **Implement**: `frontend` — disabled 상태에 `cursor-not-allowed` + 툴팁("직무를 먼저 선택해주세요"). 링크 느낌 → 버튼 박스 유지
- **Review**: `designer`

### Task 1.5 — Dashboard 상단 3 숫자 카드 재디자인
- **Implement**: `designer` + `frontend` — `총 면접 / 완료 / 이번 주` 카드에 아이콘 + 얇은 1px border + 마이크로 인사이트("전주 대비 +2") 슬롯. 현재는 배경 없는 텍스트 덩어리
- **Review**: `designer`

### Task 1.6 — 에러 페이지 대안 경로 복수화
- **Implement**: `frontend` — `interview-ready`/`interview-feedback`/`interview-analysis` 에러 상태에 최소 2개 버튼("다시 시도", "복습 목록으로") + 원인별 문구 분기. 공용 `<ErrorState />` 컴포넌트로 추출
- **Review**: `architect-reviewer` — 단일 컴포넌트 추출 합리성

### Task 1.7 — 로그인 모달 초기 vs 세션 만료 메시지 분리
- **Implement**: `frontend` — `loginModalMessage` 가 있으면 "세션이 만료됐습니다", 없으면 "로그인이 필요합니다" (현재 fallback 있으나 Task 0.2와 연동해 "이전 세션 없음" 경우 초회 문구 사용)
- **Depends on**: Task 0.2

### Task 1.8 — Review List 빈 상태 추가
- **Implement**: `frontend` — `review-list-page.tsx`에 empty state(일러스트 + "복습할 답변이 여기에 모여요" + "면접 시작" CTA) 추가. 에러 상태엔 재시도 버튼 병치
- **Review**: `designer`

### Task 1.9 — 미인증 플래시 방지
- **Implement**: `frontend` — `ProtectedRoute` 로딩 또는 미인증 상태일 때 실제 페이지 컨텐츠 렌더 차단(현재 dashboard 콘텐츠가 0.1초 보임). 스플래시 or 블러 오버레이
- **Review**: `code-reviewer`

### Task 2.1 — FAQ 아코디언 + 딥링크 `[parallel]`
- **Implement**: `frontend` + `designer` — shadcn Accordion 적용, 각 질문에 hash anchor(`#faq-pricing`). 선택적으로 Cmd+K 검색(Tier 3)
- **Review**: `code-reviewer`

### Task 2.2 — Guide 페이지 스캐너빌리티 `[parallel]`
- **Implement**: `designer` → `frontend` — 각 가이드에 우측 sticky TOC, 섹션당 최소 1개 일러스트/다이어그램(Mermaid 가능). SEO 타깃 페이지 dwell time ↑
- **Review**: `designer`

### Task 2.3 — About 히어로·서사 `[parallel]`
- **Implement**: `designer` + `frontend` — h1 전에 히어로 배너(에디토리얼 한 컷 + 서브카피), 본문에 mini 타임라인(왜 만들었는지)
- **Review**: `designer`

### Task 2.4 — 헤더·푸터 일관화
- **Implement**: `frontend`
  - Privacy 헤더를 다른 페이지와 통일(`리허설 BETA` 로고 + `← 홈으로`)
  - 푸터에 About/FAQ/Guide 링크 추가
  - `← 뒤로` vs `← 홈으로`: 브라우저 history 뒤로일 때 `← 뒤로`, 홈 리다이렉트일 때 `← 홈으로`로 명확히 구분
- **Review**: `architect-reviewer`

### Task 2.5 — 페이지 title 고유화
- **Implement**: `frontend` — react-helmet-async 또는 수동 `document.title` 세팅. review-list/interview-conduct/interview-analysis/interview-feedback 각각 고유 title
- **Review**: `code-reviewer`

### Task 2.6 — 헤더 로고·BETA 배지 톤 정렬
- **Implement**: `designer` — 현재 3D 캐릭터 로고가 에디토리얼 무채색 본문과 충돌. 모노크롬 심벌 또는 와드마크 + `BETA` 회색 필 배지로 재디자인
- **Review**: `designer`

### Task 3.1 — 로딩 문구 통일
- **Implement**: `frontend` — `준비 중 / 불러오는 중 / 로딩 중 / 세션이 만료되었습니다` 4종 마이크로카피 오디트. `constants/copy.ts` 에 모으고 의미별 재매핑

### Task 3.2 — `/interview/:id/analysis` 단계별 진행
- **Implement**: `frontend` → `backend` 협의 — STT/비전 단계별 상태를 server-sent event 또는 polling으로 수신, "영상 업로드 중 → 음성 전사 중 → 비언어 분석 중" 단계 표시. 백엔드 API 변경 동반 시 본 스프린트에서 분리(별도 spec 필요)
- **Note**: 완료 미정 시 본 태스크는 "UI 표시 영역만 준비" 수준으로 한정

### Task 3.3 — 401 콘솔 에러 노이즈 제거
- **Implement**: `frontend` — `apiClient`에서 `/api/v1/auth/me` 401은 silent catch

### Task 3.4 — 약관 링크·404 라벨 뉴트럴화
- **Implement**: `frontend` — privacy TOC 링크, 404의 `404` 숫자 라벨 등 `indigo/blue` 계열을 뉴트럴(`text-neutral-500`) 또는 브랜드 단일 accent로 통일. `frontend-design-rules.md` 금지 색상 준수

---

## 5. Sequencing

```
Day 1:
  [parallel] Task 0.1, 0.2, 0.3
Day 2-3:
  [parallel] Task 1.1, 1.2, 1.3, 1.4, 1.5
Day 4:
  Task 1.6 → 1.7(1.6의 ErrorState 재사용) → 1.8 → 1.9
Day 5-6:
  [parallel] Task 2.1, 2.2, 2.3
Day 7:
  Task 2.4, 2.5, 2.6
Backlog:
  Task 3.1~3.4 (capacity 여유 시 pull)
```

---

## 6. Agent Assignments

| Task | Implement | Review |
|------|-----------|--------|
| 0.1 | frontend | code-reviewer |
| 0.2 | frontend | architect-reviewer |
| 0.3 | frontend | designer |
| 1.1 | designer → frontend | designer |
| 1.2 | frontend | qa |
| 1.3 | designer + frontend | code-reviewer |
| 1.4 | frontend | designer |
| 1.5 | designer + frontend | designer |
| 1.6 | frontend | architect-reviewer |
| 1.7 | frontend | code-reviewer |
| 1.8 | frontend | designer |
| 1.9 | frontend | code-reviewer |
| 2.1 | frontend + designer | code-reviewer |
| 2.2 | designer → frontend | designer |
| 2.3 | designer + frontend | designer |
| 2.4 | frontend | architect-reviewer |
| 2.5 | frontend | code-reviewer |
| 2.6 | designer | designer |
| 3.1 | frontend | code-reviewer |
| 3.2 | frontend (+ backend 협의) | architect-reviewer |
| 3.3 | frontend | code-reviewer |
| 3.4 | frontend | designer |

---

## 7. Acceptance Criteria

- [x] Critical 3건(0.1/0.2/0.3) 전부 수정 — D1/D2/D3 커밋 완료
- [x] 에러 페이지 2종(ready/feedback)에 대안 경로 ≥ 2개 — `<ErrorState />` 공용 컴포넌트 (T1.6)
- [x] 홈 fullPage 캡처 시 섹션 사이 "빈 공백" 0건 — IntersectionObserver threshold 0 + rootMargin "-5% 0px" (T1.2)
- [x] 페이지 title 공용 fallback 쓰는 페이지 0개 — review-list/admin-feedbacks에 Helmet 추가 (T2.5)
- [x] axe-core 자동 a11y 스캔 상 Critical violation 0건 — vitest-axe 14 페이지 smoke 통과
- [ ] Setup wizard 5 단계 카드 hover/selected 시각 구분 명확 (디자이너 OK) — 현재 bg-primary vs hover:bg-slate-200로 구분됨, 추가 디자인 결정 필요 (T1.3)
- [ ] Dashboard 3 숫자 카드 식별성 확보 — redesign §7.5 "박스 없이 활자 갤러리" 방향과 walkthrough 피드백 충돌, 디자인 결정 필요 (T1.5)
- [ ] Playwright 재주행 — 54개 관찰 이슈 중 Tier 0–2 해결 증빙 — 수동 확인 필요

---

## 8. Risks & Open Questions

- **Task 3.2**는 backend 스펙 확장(분석 단계 이벤트 송신)이 필요. 본 스프린트에서는 UI placeholder만, 실제 스트리밍은 별도 spec `2026-04-2x-analysis-progress-stream.md` 분리 권장.
- **Task 2.6** 로고 리디자인은 브랜드 결정권자(=사용자) 확인 필요. designer agent가 3안 제시 후 선택.
- **Task 1.1** Mock 프리뷰 이미지가 있어야 히어로 품질이 결정됨. 실제 feedback 스크린 캡처 자산 선행 필요.

---

## 9. Changelog

- 2026-04-18 Draft 작성 (실제 브라우징 기반 54개 관찰 정리, 우선순위 4단계 분류)
- 2026-04-18 PR #330에 통합 실행 — 아래 태스크 반영
  - **Tier 0 (Critical, 완료)**
    - D1 T0.1 — interview-conduct 로딩 contrast 버그: `dark` 클래스 스코프로 해결
    - D2 T0.2 — "세션 만료" 오문구: `queryClient.getQueryData(AUTH_QUERY_KEY)` 조건으로 분기
    - D3 T0.3 — Setup 요약 박스 기본값: currentStep 기반 visited 판정 + 한글 라벨 매핑
  - **Tier 1 (High, 일부 완료)**
    - E2 T1.2 — 홈 스크롤 reveal threshold 0 + rootMargin "-5% 0px"
    - E4 T1.4 — Setup 다음 단계 disabled affordance (툴팁 + 하단 힌트)
    - E6 T1.6 — 공용 `<ErrorState />` + ready/feedback 에러에 액션 ≥2
    - E9 T1.9 — ProtectedRoute 미인증 플래시 방지 (Spinner 대체)
    - **미반영**: T1.1(Mock 프리뷰 이미지 → Stage C 의존), T1.3(카드 상태 — 현재 충분 판정), T1.5(Dashboard 카드 — redesign 충돌),
      T1.7(자동 반영 — D2와 연동), T1.8(ReviewEmptyState 이미 존재)
  - **Tier 2 (Medium, 부분)**
    - F1 T2.1 — FAQ `<details>` 토글 + 해시 딥링크 (shadcn Accordion 대신 네이티브)
    - F3 T2.3 — About 히어로 배너 + mini 타임라인
    - F5 T2.5 — review-list/admin-feedbacks Helmet title 추가
    - **미반영**: T2.2(Guide TOC — 스코프 과다), T2.4(Footer 신설 — 스코프 과다), T2.6(로고 리디자인 — 브랜드 결정 필요)
  - **Tier 3 (Low)**
    - G3 T3.3 — `/auth/me` 401은 api-client `!endpoint.includes('/auth/me')` 조건으로 이미 silent
    - G4 T3.4 — privacy/404는 이미 `text-text-*` neutral 토큰 사용 중 (적용 불필요)
    - **미반영**: T3.1(카피 통합 — 별도 감사 스프린트), T3.2(analysis 단계 UI — 백엔드 협의 필요)
