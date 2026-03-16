# Task 5: 종합 리포트 강화

## Status: Not Started

## Why

종합 리포트는 서비스의 핵심 가치 전달 포인트. 현재 점수 + 요약 + 강점/보완점만으로는 빈약하여 사용자가 서비스 가치를 느끼지 못함. 영역별 점수 체계와 풍부한 정보로 차별화 필요.

## Issues

| # | 제목 | 타입 |
|---|------|------|
| #62 | 종합 리포트 정보 보강 | enhancement |
| #63 | 점수 세부 기준 및 영역별 점수 체계 도입 | enhancement |
| #61 | 종합 리포트 페이지 UI/UX 개선 | enhancement |

## 의존성

- Task 4 (피드백 강화) 완료 후 시작 — 모범 답변, 비언어 데이터가 리포트에 반영되어야 함

## 구현 계획

### PR 1: [BE] — 리포트 정보 보강 + 영역별 점수 (#62, #63)

1. **영역별 점수 체계** (#63)
   - `GeneratedReport`에 `categoryScores: Map<String, Integer>` 추가
   - 점수 영역 정의:
     - 내용 정확도 (30%) — 기술적 정확성, 핵심 키워드
     - 답변 구조 (20%) — 논리적 흐름, STAR 기법
     - 의사소통 (20%) — 말하기 속도, 명확성
     - 비언어적 표현 (15%) — 시선, 자세, 표정
     - 전반적 인상 (15%) — 자신감, 열정, 태도
   - Claude 프롬프트에 채점 기준 명시
   - `InterviewReport` 엔티티에 영역별 점수 컬럼 추가

2. **리포트 정보 보강** (#62)
   - 질문별 상세 분석: 각 질문에 대한 답변 평가, 키워드 매칭
   - 답변 구조 분석: STAR 기법 활용 여부
   - 음성 분석 요약: 평균 WPM, 침묵 비율
   - 비언어 분석 요약: 시선 이탈 횟수, 자세 안정도
   - 종합 등급: S/A/B/C/D
   - 개선 액션 플랜: 구체적 연습 방법 제안
   - `ClaudePromptBuilder.buildReportSystemPrompt()` 확장

관련 파일:
- `backend/src/.../ClaudePromptBuilder.java`
- `backend/src/.../ReportService.java`
- `backend/src/.../InterviewReport.java`
- `backend/src/.../GeneratedReport.java`

**Agent**: `backend` (구현), `architect-reviewer` (리뷰)

### PR 2: [FE] — 리포트 페이지 UI/UX 개선 (#61, #63-FE)

1. **점수 시각화**
   - 레이더 차트 또는 게이지 차트로 영역별 점수 표시
   - 각 영역 클릭 시 상세 피드백 표시

2. **레이아웃 개선**
   - 카드 기반 레이아웃, 아이콘 활용
   - 질문별 상세 분석 섹션
   - 비언어 분석 요약 시각화

3. **CTA 개선**
   - "다시 면접 보기", "피드백 상세 보기" 등 다양한 액션
   - 인쇄/공유 기능 고려

관련 파일:
- `frontend/src/pages/interview-report-page.tsx`
- `frontend/src/components/review/score-card.tsx`

**Agent**: `frontend` (구현), `code-reviewer` + `designer` (리뷰)

## Acceptance Criteria

- [ ] 5개 영역별 점수가 리포트에 포함됨
- [ ] 질문별 상세 분석 (답변 평가, 키워드) 표시
- [ ] 종합 등급 (S~D) 부여
- [ ] 개선 액션 플랜 제공
- [ ] 레이더/게이지 차트로 점수 시각화
- [ ] 카드 기반 레이아웃으로 정보 구조화
- [ ] CTA 버튼 다양화
