# Plan 06: 타이포그래피 계층 정상화 [parallel]

> 상태: Draft
> 작성일: 2026-03-20

## Why

거의 모든 텍스트에 `font-black`이 적용되어 시각적 계층이 없다. 제목, 본문, 보조 텍스트가 같은 굵기라 사용자가 중요한 정보를 빠르게 스캔할 수 없다. (이슈 #8)

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `frontend/src/pages/interview-feedback-page.tsx` | 페이지 레벨 타이포그래피 조정 |
| `frontend/src/components/feedback/feedback-panel.tsx` | 카드/패널 타이포그래피 조정 (Plan 01과 함께 적용) |
| `frontend/src/components/feedback/timeline-bar.tsx` | Legend 텍스트 조정 |

## 상세

### 타이포그래피 체계

| 역할 | 현재 | 변경 |
|------|------|------|
| 페이지 제목 (h1) | `text-2xl font-extrabold` | `text-2xl font-extrabold` (유지) |
| 섹션 제목 (h2) | `text-lg font-extrabold` | `text-lg font-bold` |
| 점수 숫자 | `text-3xl font-black` | `text-3xl font-black` (유지 — 핵심 강조) |
| 카드 시간 배지 | `text-[10px] font-black` | `text-[10px] font-bold` |
| 카드 질문 | `text-xs font-bold` | `text-sm font-semibold` |
| 섹션 라벨 | `text-xs font-black` | `text-xs font-semibold` |
| 본문/코멘트 | `text-sm font-medium` | `text-sm font-normal` |
| 보조 텍스트 | `text-[10px] font-bold` | `text-[10px] font-medium` |
| CTA/토글 버튼 | `text-xs font-bold` | `text-xs font-semibold` |
| 헤더 네비 | `text-sm font-bold` | `text-sm font-semibold` |
| 헤더 타이틀 | `text-lg font-black` | `text-lg font-bold` |

### 원칙

- `font-black`: 점수 숫자, 페이지 hero 제목에만 사용
- `font-extrabold`: 페이지 제목(h1)에만
- `font-bold`: 섹션 제목(h2), 시간 배지
- `font-semibold`: 라벨, 질문 텍스트, 버튼
- `font-medium`: 보조 텍스트
- `font-normal`: 본문, 코멘트

## 담당 에이전트

- Implement: `frontend` — 클래스 변경 (Plan 01과 병합 가능)
- Review: `designer` — 타이포그래피 계층 검증

## 검증

- 페이지에서 `font-black` 사용이 점수 숫자와 hero 제목에만 한정
- 섹션 간 시각적 계층 차이가 명확
- `progress.md` 상태 업데이트 (Task 6 → Completed)
