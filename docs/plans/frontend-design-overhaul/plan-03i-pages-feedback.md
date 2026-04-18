# Plan 03i: 피드백 / 분석 페이지 레이아웃 재구성

> 상태: Draft
> 작성일: 2026-04-17

## Why

피드백 페이지는 Rehearse의 차별점인 타임스탬프 피드백 UI와 비언어 분석 리포트를 담는다. 데이터 밀도가 높아 typography hierarchy와 색상 체계가 가장 엄격하게 필요한 화면.

## 대상 페이지

- `frontend/src/pages/interview-feedback/**` (프로젝트 내 hot path: 11회 히트)
- `frontend/src/pages/interview-analysis/**`

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `frontend/src/pages/interview-feedback/**` | 타임스탬프 UI, 피드백 카드, 비디오 영역 재구성 |
| `frontend/src/pages/interview-analysis/**` | 리포트 섹션, 차트/지표 가독성 정렬 |
| `frontend/src/components/feedback/**` | 타임스탬프 칩, 점수 표시 |

## 상세

### interview-feedback

1. **비디오 플레이어 + 타임스탬프 트랙**: 하단 또는 사이드 트랙, 클릭 시 seek
2. **피드백 카드 리스트**: 질문별 카드, 시각적 계층
3. **AI 총평 섹션**: body typography

### interview-analysis

1. **요약 지표 카드**: 점수/등급 표시 — pure black/white 지양, muted-foreground 대비 확보
2. **비언어 분석 결과**: 제스처/표정/시선 — 아이콘 outline만 쓰지 말고 질감 혼합(frontend-design-rules)
3. **개선 포인트 리스트**: 순서 있는 `<ol>` + heading 계층
4. **차트 색상**: 모노크롬 + accent 1개, 퍼플/네온 금지

### 변경 범위

- Typography scale (리포트 heading/body/caption)
- 데이터 시각화 색상 — 단색 계조 우선
- 타임스탬프 칩 radius/padding 토큰화
- 하드코딩 색상 제거

### 비변경

- 비디오 플레이어 로직, seek 제어
- 피드백 데이터 구조, 정렬, 필터 로직
- 차트 라이브러리 교체 금지(색상만 토큰으로)

## 담당 에이전트

- Implement: `frontend` + `designer`
- Review: `designer` — 데이터 밀도/가독성
- Review: `code-reviewer` — seek/플레이어 회귀

## 검증

- `npm run lint/build/test` green
- 수동 스모크: 피드백 카드 클릭 → 비디오 seek, 분석 리포트 스크롤
- 차트 색상이 토큰 기반인지 확인
- 접근성: 차트 지표에 대체 텍스트/표 제공
- `progress.md` Task 3i → Completed

## 체크포인트

스크린샷 + seek 동작 확인 보고 → 사용자 승인 후 Phase 3j 진입.
