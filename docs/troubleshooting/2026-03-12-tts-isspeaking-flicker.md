# TTS isSpeaking 깜빡임 — cancel() 후 비동기 onend가 false→true 갭 유발

> **날짜**: 2026-03-12 | **심각도**: Medium | **상태**: 해결됨

## 증상
- "답변 시작" → 1~2초 내 "답변 완료" 클릭 시, "AI 면접관이 말하고 있어요" 표시 중간에 버튼이 갑자기 나타났다 사라지는 UI 깜빡임 발생
- 전환 TTS → 질문 TTS 연속 재생 시에도 동일한 깜빡임

## 재현 절차
1. 면접 진행 페이지에서 "답변 시작" 클릭
2. 1~2초 내 "답변 완료" 클릭
3. 전환 TTS 재생 시작 직전, 버튼이 순간 노출됨 (isSpeaking이 false→true 깜빡임)

## 원인 분석
- **직접 원인**: `speak()` 내부에서 `window.speechSynthesis.cancel()`을 호출하면, 이전 utterance의 `onend`가 **비동기**로 발동 → `setIsSpeaking(false)` → 새 utterance의 `onstart` → `setIsSpeaking(true)`. 이 false→true 갭 사이에 React 렌더가 발생하여 버튼이 순간 노출됨
- **근본 원인**: `onend`/`onerror` 콜백이 어떤 utterance에서 발동된 것인지 구분하지 않아, 이미 교체된 utterance의 이벤트가 현재 상태를 오염시킴

발생 시나리오:
```
1. handleStopAnswer → tts.stop() → setIsSpeaking(false) + cancel()
2. tts.speak(전환문구) → cancel() → new utterance → speak()
3. React 렌더: isSpeaking=false → 버튼 노출! (깜빡임)
4. 이전 utterance onend → setIsSpeaking(false) (비동기)
5. 새 utterance onstart → setIsSpeaking(true) → "AI 말하는 중" 복귀
```

## 해결
- `utteranceIdRef` (counter ref) 도입으로 stale 콜백 방지
- `speak()` 호출 시 ID를 증가시키고, `cancel()` 전에 `setIsSpeaking(true)` 즉시 설정 → false 갭 제거
- `onend`/`onerror`에서 현재 ID와 비교하여 불일치 시 무시
- `stop()` 시 ID를 증가시켜 기존 콜백 무효화

수정 파일:
- `frontend/src/hooks/use-tts.ts` — utteranceIdRef 도입, speak/stop/onend/onerror 수정

## 검증
- "답변 시작" → 1초 후 "답변 완료" → "AI 면접관이 말하고 있어요" 표시 안정적 (깜빡임 없음)
- 전환 TTS → 질문 TTS 연속 재생 시 버튼 깜빡임 없음
- `tts.stop()` 후 즉시 `tts.speak()` 호출 시 isSpeaking이 항상 true 유지

## 교훈
- Web Speech API의 `cancel()`은 이전 utterance의 `onend`를 비동기로 발동시킴 — 단순 boolean 플래그로는 race condition 방지 불가
- 비동기 콜백이 있는 리소스를 교체할 때는 반드시 ID/generation 기반으로 stale 콜백을 구분해야 함
