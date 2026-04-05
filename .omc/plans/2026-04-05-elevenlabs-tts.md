# ElevenLabs TTS 연동

**Status**: In Progress

## Why

Web Speech API는 브라우저 내장 TTS로 음질이 어색하고 브라우저/OS마다 다름.
ElevenLabs API를 통해 자연스러운 한국어 AI 면접관 목소리를 제공한다.

## Goal

- 기존 Web Speech API → ElevenLabs API로 교체
- `use-tts.ts` 외부 인터페이스 유지 (speak/stop/isSpeaking) → 호출부 변경 없음
- API 키는 백엔드에서만 관리 (프론트 노출 금지)

## 아키텍처

```
FE: speak(text)
  → POST /api/v1/tts { text }  [JWT 인증]
  → BE: ElevenLabsTtsService → ElevenLabs API
  → 오디오 바이트 반환 (audio/mpeg)
  → FE: Blob URL 생성 → Audio 재생
```

## 구현 범위

### BE
- `ElevenLabsTtsService` — ElevenLabs streaming TTS API 호출
- `TtsController` — POST /api/v1/tts 엔드포인트
- `application-dev.yml` — elevenlabs 설정 추가

### FE
- `use-tts.ts` 교체 — 백엔드 호출 + Audio 재생
- 인터페이스 유지: `{ speak, speakWhenReady, stop, isSpeaking, isAvailable }`

## ElevenLabs API

- Endpoint: `POST https://api.elevenlabs.io/v1/text-to-speech/{voiceId}`
- Model: `eleven_multilingual_v2` (한국어 지원)
- Response: `audio/mpeg` 바이트 스트림

## 설정값

```yaml
elevenlabs:
  api-key: ${ELEVENLABS_API_KEY}
  voice-id: ${ELEVENLABS_VOICE_ID:cgSgspJ2msm6clMCkdW9}  # Rachel (다국어)
  model-id: eleven_multilingual_v2
```