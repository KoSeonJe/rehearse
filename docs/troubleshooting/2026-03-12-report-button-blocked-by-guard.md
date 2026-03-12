# "리포트 보기" 버튼 동작 불가 — phase 가드가 completed 상태를 차단

> **날짜**: 2026-03-12 | **심각도**: High | **상태**: 해결됨

## 증상
- 마지막 질문 답변 완료 → 종료 TTS → "리포트 보기" 버튼 클릭 → 아무 동작 없음
- 리포트 페이지로 이동 불가

## 재현 절차
1. 면접 진행 중 마지막 질문에 답변
2. "답변 완료" 클릭 → 종료 TTS 재생 → phase가 `completed`로 전환
3. "리포트 보기" 버튼 클릭 → 아무 반응 없음

## 원인 분석
- **직접 원인**: 광클 방지를 위해 추가한 `if (currentPhase === 'completed') return` 가드가 "리포트 보기" 버튼까지 차단
- **근본 원인**: "리포트 보기" 버튼은 `phase === 'completed'`일 때만 표시되는데, `handleFinishInterview`가 정확히 그 phase에서 early return하므로 절대 동작 불가. phase 기반 가드가 "중복 실행 방지"와 "정상 실행"을 구분하지 못함

## 해결
- phase 기반 가드(`if (phase === 'completed') return`)를 `isFinishingRef` (실행 중 플래그)로 교체
- 중복 실행만 방지하고, completed 상태에서의 정상 호출은 허용

수정 파일:
- `frontend/src/hooks/use-interview-session.ts` — handleFinishInterview의 가드 교체

## 검증
- 마지막 질문 답변 완료 → 종료 TTS → "리포트 보기" 버튼 → 리포트 페이지 정상 이동
- "리포트 보기" 광클 → 1회만 실행 (isFinishingRef 방어)

## 교훈
- phase 가드를 추가할 때 해당 phase에서 정상적으로 호출되는 케이스가 있는지 반드시 확인
- "중복 실행 방지"는 phase보다 실행 중 플래그(ref)가 더 정확한 도구
