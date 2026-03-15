# STT 엔진 변경 검토 (Web Speech API → Gemini / OpenAI)

- **Status**: `Planned`
- **Priority**: Low (MVP 이후)
- **Created**: 2026-03-12

---

## Why

현재 Web Speech API(브라우저 내장)를 사용 중인데, 다음 한계가 있음:

1. **브라우저 제한**: Chrome 계열에서만 안정적 동작 (Firefox/Safari 미지원 또는 불안정)
2. **한국어 정확도**: Google 서버 기반이라 괜찮지만, Gemini/Whisper 대비 낮을 수 있음
3. **제어 불가**: 모델 선택, 커스터마이징 불가 (Google이 내부적으로 처리)
4. **네트워크 에러 핸들링**: 65초 세션 제한, 네트워크 에러 시 재시도 로직 필요

## Goal

- STT 엔진을 Gemini Live API 또는 OpenAI Whisper/Realtime API로 교체
- 한국어 인식 정확도 향상 + 크로스 브라우저 지원
- 실시간 중간결과(자막) 기능 유지

## 후보 비교

| | Web Speech API (현재) | Gemini Live API | OpenAI Realtime API |
|---|---|---|---|
| 비용 | 무료 | 유료 | 유료 |
| 실시간 중간결과 | O | O (서버 스트리밍) | O |
| 한국어 정확도 | 보통 | 높음 | 높음 |
| 구현 방식 | 브라우저 네이티브 | WebSocket 스트리밍 | WebSocket 스트리밍 |
| 브라우저 제한 | Chrome 계열만 | 없음 | 없음 |
| API 키 노출 위험 | 없음 | 백엔드 프록시 필요 | 백엔드 프록시 필요 |

## Trade-offs

- **비용 발생**: 무료 → 유료 전환 (사용량 기반 과금)
- **구현 복잡도 증가**: 백엔드 프록시 + WebSocket 스트리밍 구현 필요
- **인프라 부담**: 오디오 스트리밍 트래픽 처리

## 구현 시 필요한 작업

### Backend
1. STT 프록시 WebSocket 엔드포인트 추가 (API 키 프론트엔드 노출 금지 원칙)
2. 선택한 STT API 클라이언트 통합
3. 오디오 스트림 → STT API 중계 로직

### Frontend
1. 마이크 오디오를 PCM/WebM으로 캡처하여 백엔드 WebSocket으로 전송
2. `use-speech-recognition.ts` 훅을 새 WebSocket 기반으로 교체
3. 기존 인터페이스(`start`, `stop`, `interimText`, `onFinalResult`) 유지하여 상위 컴포넌트 변경 최소화

### Agent 할당 (구현 시)
- Implement (BE): `backend` — WebSocket 프록시 엔드포인트
- Implement (FE): `frontend` — 오디오 스트리밍 + 훅 교체
- Review: `architect-reviewer` — 스트리밍 아키텍처 검증
- Review: `code-reviewer` — 보안 (API 키), 에러 핸들링

## 결정 필요 사항

- [ ] Gemini vs OpenAI 중 어느 쪽을 사용할지
- [ ] 비용 허용 범위
- [ ] 우선순위 (크로스 브라우저 vs 정확도 중 무엇이 더 중요한지)
