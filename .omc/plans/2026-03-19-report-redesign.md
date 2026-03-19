# 리포트 생성 시점 수정 + 리포트 페이지 전면 리디자인

- **Status**: Completed
- **Date**: 2026-03-19
- **Branch**: `fix/audio-recording-playback` (현재 브랜치)

## Why

1. Lambda가 `trigger_report()` 호출 실패 시 복구 불가 → FE에서 REPORT_NOT_FOUND만 받음
2. 리포트 UI가 경쟁 서비스 대비 단순 → 전면 리디자인 필요
3. 기존 DTO만으로 UI 개선 가능 (확장 불필요)

## Part 1: [BE] 이벤트 기반 리포트 자동 생성 + 방어적 조회

### 변경 사항
- `AllAnalysisCompletedEvent` 발행 (saveFeedback에서 모든 분석 완료 시)
- `@Async @EventListener`로 리포트 자동 생성
- `getReport()` 방어적 응답: 분석 미완료(400), 생성 중(202), 존재(200)

### 파일
| 파일 | 변경 |
|------|------|
| 신규: `AllAnalysisCompletedEvent.java` | 이벤트 클래스 |
| `InternalQuestionSetService.java` | 이벤트 발행 로직 |
| `ReportService.java` | EventListener + 방어적 getReport |
| `ReportErrorCode.java` | REPORT_GENERATING 추가 |
| `ReportController.java` | 202 응답 분기 |

## Part 2: [FE] 리포트 페이지 전면 리디자인

### 변경 사항
- 원형 게이지 + 등급 배지 + 카운트업 애니메이션
- 강점/개선점 2컬럼 카드
- 점수 인사이트 카드
- CTA 섹션 (타임스탬프 리뷰/새 면접/홈)
- 리포트 생성 중 폴링 UI

### 파일
| 파일 | 변경 |
|------|------|
| `interview-report-page.tsx` | 전면 리디자인 |
| `use-report.ts` | 폴링 로직 추가 |
