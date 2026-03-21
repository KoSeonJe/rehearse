# Plan 02: 스킵 질문 완전 숨김

> 상태: Draft
> 작성일: 2026-03-20
> 우선순위: P1
> 태그: [parallel] (Phase A)

## Why

사용자가 면접 중 스킵한 질문이 피드백/분석 페이지에 여전히 노출된다.
BE는 이미 SKIPPED 질문세트를 피드백 응답에서 필터링하고 있으나, FE에서 별도로 표시하는 부분이 남아있다. 스킵한 질문은 분석 대상이 아니므로 UI에서 완전히 제거한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `frontend/.../pages/interview-feedback-page.tsx` | SKIPPED 상태 질문세트 필터링 |
| `frontend/.../pages/interview-analysis-page.tsx` | SKIPPED 상태 질문세트 필터링 |

## 상세

1. **피드백 페이지** (`interview-feedback-page.tsx`)
   - 질문세트 목록 렌더링 시 `status === 'SKIPPED'` 인 항목 제외
   - 타임라인에서도 SKIPPED 질문세트 관련 마커 제거

2. **분석 페이지** (`interview-analysis-page.tsx`)
   - 분석 결과 목록에서 SKIPPED 질문세트 필터링
   - 전체 질문 수 카운트에서도 제외

3. **필터링 위치**
   - 데이터 fetch 후 컴포넌트 렌더링 전 `.filter()` 적용
   - BE 응답 구조는 변경하지 않음 (BE는 이미 필터링 중이나 방어적으로 FE에서도 처리)

## 담당 에이전트

- Implement: `frontend` — 두 페이지에서 SKIPPED 필터링 적용
- Review: `code-reviewer` — 필터링 누락 없는지 코드 품질 검증

## 검증

- 스킵한 질문이 피드백 페이지에 표시되지 않는지 확인
- 스킵한 질문이 분석 페이지에 표시되지 않는지 확인
- 스킵하지 않은 질문은 정상 표시되는지 확인
- `progress.md` 상태 업데이트 (Task 2 → Completed)
