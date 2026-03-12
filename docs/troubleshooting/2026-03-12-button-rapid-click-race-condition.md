# 답변 시작/완료 버튼 광클 레이스 컨디션 — phase 가드 없이 중복 실행 허용

> **날짜**: 2026-03-12 | **심각도**: High | **상태**: 해결됨

## 증상
- "답변 시작" / "답변 완료" 버튼을 빠르게 연속 클릭하면:
  - STT 인스턴스 중복 생성
  - TTS 덮어쓰기 (전환 문구 겹침)
  - phase 불일치 (recording인데 paused 동작 실행)

## 재현 절차
1. 면접 진행 페이지에서 "답변 시작" 버튼 빠르게 2~3회 클릭
2. 또는 "답변 완료" 버튼 빠르게 2~3회 클릭
3. STT 중복, TTS 겹침, phase 불일치 발생

## 원인 분석
- **직접 원인**: `doStartAnswer`, `handleStopAnswer` 등에서 현재 phase를 확인하지 않아 중복 실행 허용
- **근본 원인**:
  1. **Phase 가드 없음**: 핸들러 진입 시 현재 상태 검증 부재
  2. **버튼 disabled 없음**: TTS 재생 중에도 버튼 클릭 가능
  3. **스페이스바 stale closure**: `useEffect` deps에 `isRecording`을 넣어 React 상태 기반으로 판단 → 빠른 전환 시 이전 렌더의 값 참조

## 해결
- 각 핸들러 진입 시 `useInterviewStore.getState().phase`로 현재 상태를 직접 읽어 유효하지 않은 phase에서 early return:
  - `doStartAnswer`: `ready` / `paused` / `greeting`일 때만 실행
  - `handleStopAnswer`: `recording` / `greeting`일 때만 실행
  - TTS `onEnd`: `paused` / `recording` / `completed`가 아니면 pending action 무시
- "답변 시작" / "답변 완료" 버튼에 `disabled={isTtsSpeaking}` 추가
- 스페이스바 핸들러에서 `useInterviewStore.getState().phase`로 직접 조회하여 stale closure 방지

수정 파일:
- `frontend/src/hooks/use-interview-session.ts` — phase 가드 4곳
- `frontend/src/components/interview/interview-controls.tsx` — disabled + 스페이스바 수정
- `frontend/src/types/interview.ts` — `greeting_tts` 이벤트 타입 추가
- `frontend/src/pages/interview-page.tsx` — 미사용 변수 제거

## 검증
- 답변 시작/완료 빠르게 연타 → 1회만 실행
- TTS 재생 중 버튼 비활성화 확인
- 스페이스바로 토글 시 정상 동작

## 교훈
- 비동기 상태 전환이 있는 UI에서는 React props/state 대신 store를 직접 읽어 최신 상태 기반으로 판단해야 함
- TTS 등 외부 리소스 재생 중에는 반드시 관련 버튼을 disabled 처리
