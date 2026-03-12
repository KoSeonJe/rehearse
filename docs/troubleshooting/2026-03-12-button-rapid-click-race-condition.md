# 답변 시작/완료 버튼 광클 레이스 컨디션

## 문제상황

"답변 시작" / "답변 완료" 버튼을 빠르게 연속 클릭하면:
- STT 인스턴스 중복 생성
- TTS 덮어쓰기 (전환 문구 겹침)
- phase 불일치 (recording인데 paused 동작 실행)

## 원인

1. **Phase 가드 없음**: `doStartAnswer`, `handleStopAnswer` 등에서 현재 phase를 확인하지 않아 중복 실행
2. **버튼 disabled 없음**: TTS 재생 중에도 버튼 클릭 가능
3. **스페이스바 stale closure**: `useEffect` deps에 `isRecording`을 넣어 React 상태 기반으로 판단 → 빠른 전환 시 이전 렌더의 값 참조

## 해결

### 1. Phase 가드 (use-interview-session.ts)

각 핸들러 진입 시 `useInterviewStore.getState().phase`로 현재 상태를 직접 읽어 유효하지 않은 phase에서 early return:

- `doStartAnswer`: `ready` / `paused` / `greeting`일 때만 실행
- `handleStopAnswer`: `recording` / `greeting`일 때만 실행
- `handleFinishInterview`: `completed`면 무시
- TTS `onEnd`: `paused` / `recording` / `completed`가 아니면 pending action 무시

### 2. 버튼 disabled (interview-controls.tsx)

"답변 시작" / "답변 완료" 버튼에 `disabled={isTtsSpeaking}` + `disabled:opacity-50 disabled:pointer-events-none`

### 3. 스페이스바 store 직접 읽기 (interview-controls.tsx)

`isRecording` props 대신 `useInterviewStore.getState().phase`로 현재 phase를 직접 조회하여 stale closure 방지

## 수정 파일

- `frontend/src/hooks/use-interview-session.ts` — phase 가드 4곳
- `frontend/src/components/interview/interview-controls.tsx` — disabled + 스페이스바 수정
- `frontend/src/types/interview.ts` — `greeting_tts` 이벤트 타입 추가
- `frontend/src/pages/interview-page.tsx` — 미사용 변수 제거
