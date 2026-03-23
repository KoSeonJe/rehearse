# Gemini 네이티브 오디오 분석 전환 — 진행 상황

## 태스크 상태

| # | 태스크 | 상태 | 의존성 | 비고 |
|---|--------|------|--------|------|
| 1 | Gemini SDK Lambda Layer + 환경변수 | Draft | - | [blocking] |
| 2 | FFmpeg 답변 구간별 오디오 추출 | Draft | - | [parallel: Task 3] |
| 3 | Gemini 오디오 분석기 구현 | Draft | Task 1 | [parallel: Task 2] |
| 4 | handler.py 파이프라인 재구조화 | Draft | Task 2, 3 | |
| 5 | Backend 피드백 스키마 확장 | Draft | - | [parallel: Task 2~4] |
| 6 | Frontend 음성 피드백 UI + 프로그레스 | Draft | Task 5 | |

## 진행 로그

### 2026-03-23
- 요구사항 정의 및 계획 문서 작성
- 기존 코드 vs 스펙 검증 완료 (7개 항목)
- 핵심 발견: Lambda 트리거 단위 = 질문세트 (최대 3답변), 15답변 아님
- 미결정 사항 5개 식별 (requirements.md 참고)
