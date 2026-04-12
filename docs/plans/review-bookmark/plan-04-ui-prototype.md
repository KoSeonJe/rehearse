# Plan 04: UI/UX HTML 프로토타입 제작

> 상태: Draft
> 작성일: 2026-04-12
> 선행: Plan 02 완료 (데이터 모델/API 계약 확정 직후 바로 진행 가능, Plan 03과 병렬 `[parallel]`)

## Why

복습 북마크 기능의 UI/UX 품질이 기능 채택률을 결정한다. 실제 코드로 구현하기 전에 HTML 프로토타입으로 배치 · 색상 · 인터랙션을 확정하면, 이후 React 컴포넌트 구현 단계에서 의사결정 비용이 크게 줄고 리뷰 이터레이션이 짧아진다.

`designer` 에이전트와 `frontend-design` 스킬을 활용해 **두 개의 HTML 프로토타입 파일**을 생성한다. 이 파일들이 이후 Plan 05, Plan 06 구현의 시각적 소스 오브 트루스가 된다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `docs/plans/review-bookmark/prototypes/feedback-bookmark-button.html` | 피드백 페이지 카드의 북마크 버튼 + 코치마크 popover + 토스트 정적 프로토타입 |
| `docs/plans/review-bookmark/prototypes/review-list-page.html` | 전역 `/review-list` 풀페이지 정적 프로토타입 (카테고리 섹션 + 카드 + 확장 비교 뷰 + 빈 상태) |
| `docs/plans/review-bookmark/prototypes/README.md` | 두 프로토타입의 의사결정/스펙 요약, 구현 시 참고사항 |

## 상세

### Prototype 1 — feedback-bookmark-button.html

현재 `FeedbackCard`의 실제 레이아웃을 재현한 상태에서 북마크 버튼의 **3가지 상태**를 나란히 보여주는 정적 페이지:
1. 미북마크 기본 상태 (`ListPlus` + "복습 북마크에 담기" pill)
2. 북마크 완료 상태 (`ListChecks` + "복습 북마크에 담김" + `#FFF1EE` 배경 + `#D94A3A` 텍스트)
3. 최초 사용자 코치마크 popover가 열린 상태 (화살표 + 안내 문구 + "알겠어요" CTA)
4. 토스트 (bottom-right, Undo 링크 포함)

**필수 디자인 원칙**:
- 기존 카드 톤(흰색, 옅은 그림자 `0 1px 3px rgba(0,0,0,0.06)`, `rounded-2xl`) 일관
- 포인트 컬러 `#FF6B5B` 절제된 사용
- 텍스트는 `gray-900 / gray-400 / gray-600` 위계 유지
- `ListPlus → ListChecks` 아이콘 토글 (Lucide React 기준)
- WCAG AA 색 대비(`#FFF1EE` 배경 × `#D94A3A` 텍스트) 검증
- 모바일(360px), 태블릿(768px), 데스크톱(1280px) 반응형 3종 스냅샷

### Prototype 2 — review-list-page.html

전역 풀페이지의 **완성 상태 + 빈 상태** 두 뷰를 한 HTML 파일 내에서 탭/섹션 분리로 시연:

**완성 상태 구성**:
- 상단 헤더: "복습 북마크 (7개)" + 한 줄 설명
- 필터 바: 상태 필터 칩(전체 / 연습 중 / 해결됨) + 카테고리 드롭다운
- 카테고리 섹션: `InterviewType` 기반 UI 상위 그룹 5~6개 제안 (designer 에이전트 확정)
  - 예시 그룹화 (designer가 확정 권한): `CS 기초(CS_FUNDAMENTAL) / 시스템 설계(SYSTEM_DESIGN) / 언어·프레임워크(LANGUAGE_FRAMEWORK, UI_FRAMEWORK, BROWSER_PERFORMANCE, FULLSTACK_STACK) / 인프라·클라우드(INFRA_CICD, CLOUD) / 데이터(DATA_PIPELINE, SQL_MODELING) / 행동·경험(BEHAVIORAL, RESUME_BASED)`
- 카드: 질문 텍스트 + 면접 날짜 + 해결 상태 배지 + 우상단 "해결됨 토글" + "삭제"
- 카드 확장: 클릭 시 내 답변(`transcript`)과 모범 답변(`modelAnswer`) **좌우 2열 비교**
- AI 코칭 요약(`coachingImprovement`) 작게 하단 보조 텍스트

**빈 상태**:
- 중앙 안내 + 아이콘 + 한 줄 CTA ("면접 피드백에서 담아보세요")

### Prototype 3 — README.md

프로토타입 확정 후 다음을 정리한다:
- 최종 아이콘/레이블/컬러 토큰
- `InterviewType → UI 상위 그룹` 매핑 표 (Plan 06 구현 시 그대로 사용)
- 각 컴포넌트별 Tailwind 클래스 요약
- 접근성 체크리스트 (`aria-pressed`, `aria-label`, 포커스 링, `role="dialog"`, `role="status"`)
- Plan 05, Plan 06에서 참고할 핵심 의사결정

## 담당 에이전트

- Implement: `designer` — HTML 프로토타입 제작, 디자인 토큰 확정, UI 상위 그룹 매핑 결정
- Review: `ui-ux-designer` — 연구 근거 기반 UX 검증 (NN Group 등), 접근성 검증, 미학/레이아웃 비판
- Review: `frontend-developer` — Tailwind 클래스 재사용성, 실제 React 구현 가능성

### 활용 스킬
- `frontend-design` 스킬 — 고품질 프론트 프로토타입 가이드
- `ui-ux-pro-max` 스킬 — 팔레트/폰트/차트 레퍼런스 (옵션)

## 검증

- 두 HTML 파일이 브라우저에서 독립 실행되어 모든 상태(기본/토글/코치마크/토스트/빈 상태) 시각 확인 가능
- WCAG AA 색 대비 자동 검사(크롬 devtools axe 또는 동등 도구)
- 모바일 360 / 태블릿 768 / 데스크톱 1280 3종 뷰포트에서 레이아웃 깨짐 없음
- `prototypes/README.md`에 `InterviewType → UI 상위 그룹` 매핑과 디자인 토큰 최종본 명시
- `progress.md` 상태 업데이트 (Task 4 → Completed)

## 열린 결정 (designer 에이전트가 확정)

1. 코치마크 popover 구체 문구 최종본
2. 카드 확장 시 좌우 비교 vs 상하 비교 (공간 효율 vs 가독성)
3. 카테고리 UI 상위 그룹 최종 이름/그룹핑 (5~6개)
4. "해결됨" 배지 색상 (녹색 vs 중성 회색)
5. 빈 상태 일러스트/아이콘 여부 (또는 텍스트 only)
