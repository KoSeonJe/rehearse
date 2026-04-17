# Plan 03h: 인터뷰 진행 페이지 레이아웃 재구성

> 상태: Draft
> 작성일: 2026-04-17

## Why

인터뷰 흐름(setup → ready → interview)은 서비스 핵심 UX. 녹화 중 UI는 명확한 시각 계층과 낮은 인지 부하가 필수. DESIGN.md 모노크롬은 녹화 중 시각적 노이즈를 줄이는 데 유리.

## 대상 페이지

- `frontend/src/pages/interview-setup/**`
- `frontend/src/pages/interview-ready/**`
- `frontend/src/pages/interview-page/**`

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| 위 3개 페이지 디렉토리 전체 | 스텝 progress, 녹화 UI, 질문/답변 영역 재정렬 |
| `frontend/src/components/setup/**` | 스텝 위저드 컴포넌트 |
| `frontend/src/components/interview/**` | 녹화 컨트롤, 타이머, 질문 표시 |

## 상세

### 페이지별 대상 섹션

**interview-setup**
- 스텝 progress indicator (shadcn Progress + 토큰 적용)
- 이력서 업로드 / 직무 선택 / 난이도 UI — Form 표준화(3e 결과) 활용
- 네비게이션 버튼(Prev/Next) — 3a 결과

**interview-ready**
- 카메라/마이크 프리뷰
- 체크리스트(카메라 활성/마이크 레벨/네트워크)
- "시작" primary CTA — 강조는 CTA에만

**interview-page**
- 질문 표시 영역 (큰 typography, 읽기 편한 line-height)
- 녹화 상태 인디케이터 (`rec-pulse` 기존 애니 유지, 단 다른 모션과 겹치지 않게)
- 타이머
- 제어 버튼(일시정지/종료) — `destructive` variant는 종료에만

### 변경 범위

- 레이아웃 그리드, 섹션 spacing
- Typography scale (질문 영역은 DESIGN.md sub-heading/body 범위)
- 색상: 퍼플 제거, `rec-pulse`의 빨간색은 녹화 의미로 유지
- `transition-all` 사용처 정리 → `transition-colors`/`transition-transform`만

### 비변경

- MediaRecorder 로직
- 질문 상태머신
- 타이머 로직
- S3 업로드 흐름

## 담당 에이전트

- Implement: `frontend` + `designer`
- Review: `designer` — 인지 부하 관점 평가
- Review: `code-reviewer` — 녹화 흐름 회귀

## 검증

- `npm run lint/build/test` green
- 수동 스모크: setup → ready → interview 전체 흐름 녹화 1회
- **E2E 회귀 1회 (필수)**: setup 진입 → 카메라/마이크 권한 → 녹화 시작 → 질문 응답 → 녹화 종료 → S3 업로드 완료 → 피드백 페이지 진입까지 단일 세션으로 완주
  - video element `ref`, IntersectionObserver, MediaRecorder 이벤트 리스너 회귀 없음 확인
  - 녹화 실패/업로드 실패 시 콘솔 에러 0건
- 모션 겹침 체크: 동일 뷰포트에 `rec-pulse` + 다른 무한 애니 공존 금지
- `prefers-reduced-motion` 환경에서 비필수 애니 정지
- `progress.md` Task 3h → Completed

## 체크포인트

전체 흐름 녹화 영상/스크린샷 보고 → 사용자 승인 후 Phase 3i 진입.
