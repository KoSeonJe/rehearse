# 면접 진행 페이지 성능 최적화 + 녹화 영상 영속화

- **Status**: Completed
- **Date**: 2026-03-13
- **Scope**: Frontend only

---

## Why

면접 진행 페이지(`/interview/:id/conduct`)에서 두 가지 문제 발생:

1. **영상 렉/깜빡임**: 3개의 60fps RAF 루프 동시 실행 + 빈번한 Zustand 상태 업데이트(~10-15회/초)가 InterviewPage 전체를 리렌더 → VideoPreview 깜빡임
2. **녹화 영상 미표시**: review 페이지의 VideoPlayer가 Zustand의 `videoBlobUrl`(메모리 전용)을 읽는데, 새로고침/스토어 초기화 시 유실

---

## 근본 원인 분석

### 렉/깜빡임 원인 체인

```
useAudioAnalyzer RAF(60fps) → setAudioLevel(~10fps) → Zustand 업데이트
                                                          ↓
STT interim(~3-5/초) → setCurrentTranscript → Zustand 업데이트
                                                          ↓
InterviewPage 전체 리렌더 (~10-15/초) → VideoPreview 리렌더 → 영상 깜빡임
```

### 동시 실행 RAF 루프 3개

| # | 위치 | 역할 | 필요성 |
|---|------|------|--------|
| 1 | `use-audio-analyzer.ts` | FFT 분석 + audioLevelRef 갱신 | 필수 |
| 2 | `use-vad.ts` | 음성 감지 로직 | 필수 |
| 3 | `use-interview-session.ts` | audioLevelRef → VAD 브릿지 | **불필요** (제거 대상) |

---

## 해결 방법

### Phase 1: 핵심 성능 최적화

#### Task 1: 브릿지 RAF 루프 제거 + VAD 직접 ref 참조
- `use-vad.ts`: `UseVadOptions`에 `audioLevelRef: RefObject<number>` 추가, 내부 audioLevelRef/updateAudioLevel 제거
- `use-interview-session.ts`: 브릿지 useEffect(RAF) 삭제, useVad 호출 시 `audioLevelRef` 직접 전달
- **효과**: RAF 3개 → 2개

#### Task 2: audioLevel 상태 업데이트 제거 → ref 기반 렌더링
- `use-audio-analyzer.ts`: `useState(audioLevel)` 및 `setAudioLevel()` 제거, ref만 반환
- `audio-level-indicator.tsx`: props를 `audioLevelRef`로 변경, 내부 RAF(~10fps)로 DOM 직접 업데이트, `React.memo` 적용
- `interview-page.tsx`: `<AudioLevelIndicator audioLevelRef={audio.audioLevelRef} />`로 변경
- **효과**: ~10fps Zustand 업데이트 완전 제거

#### Task 3: currentTranscript 구독 분리 + InterviewPage 구독 최적화
- `transcript-display.tsx`: 내부에서 `useInterviewStore` 직접 구독 (currentTranscript, answers), props 제거
- `interview-page.tsx`: 전체 스토어 구독 → 개별 selector로 세분화 (currentTranscript, answers 구독 제거)
- **효과**: InterviewPage 리렌더 초당 ~10-15회 → phase/question 변경 시에만

#### Task 4: VideoPreview React.memo 적용
- `video-preview.tsx`: `React.memo()` 래핑
- **효과**: 부모 리렌더에 의한 불필요한 리렌더 차단

### Phase 2: 추가 최적화

#### Task 5: useAudioAnalyzer RAF → 30fps 제한
- `use-audio-analyzer.ts`: `performance.now()` 기반 33ms 스로틀 적용
- **효과**: 오디오 분석 CPU 사용량 ~50% 감소

#### Task 6: InterviewControls/InterviewTimer memo 적용
- 두 컴포넌트에 `React.memo` 적용

### Phase 3: 녹화 영상 영속화

#### Task 7: IndexedDB 기반 영상 저장/로드
- `lib/video-storage.ts` (신규): `saveVideoBlob`, `loadVideoBlob`, `deleteVideoBlob` (raw IndexedDB API)
- `use-interview-session.ts`: `setVideoBlob(blob)` 직후 `saveVideoBlob(interview.id, blob)` 호출
- `video-player.tsx`: Zustand fallback + IndexedDB 로드 로직 추가
- `interview-review-page.tsx`: VideoPlayer에 `interviewId` 전달

---

## 변경 파일 목록

| 파일 | 변경 유형 |
|------|----------|
| `lib/video-storage.ts` | 신규 생성 |
| `hooks/use-vad.ts` | audioLevelRef 외부 주입, updateAudioLevel 제거 |
| `hooks/use-audio-analyzer.ts` | useState 제거, 30fps 스로틀 |
| `hooks/use-interview-session.ts` | 브릿지 RAF 제거, IndexedDB 저장 추가 |
| `components/interview/audio-level-indicator.tsx` | ref 기반 DOM 업데이트 |
| `components/interview/transcript-display.tsx` | 내부 Zustand 구독 |
| `components/interview/video-preview.tsx` | React.memo |
| `components/interview/interview-controls.tsx` | React.memo |
| `components/interview/interview-timer.tsx` | React.memo |
| `components/review/video-player.tsx` | IndexedDB fallback |
| `pages/interview-page.tsx` | 개별 selector, props 정리 |
| `pages/interview-review-page.tsx` | interviewId 전달 |

---

## 결과

| 지표 | Before | After |
|------|--------|-------|
| 동시 RAF 루프 | 3개 (60fps × 3) | 2개 (VAD 60fps + Audio 30fps) |
| InterviewPage 리렌더/초 | ~10-15회 | ~0회 (이벤트 시에만) |
| VideoPreview 리렌더/초 | ~10-15회 | stream 변경 시에만 |
| 녹화 영상 | 새로고침 시 유실 | IndexedDB 영속 |
