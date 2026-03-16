# Task 6: STT 전환

## Status: Not Started

## Why

현재 Web Speech API의 한국어 인식 정확도가 낮아 면접 답변 텍스트 변환이 부정확함. 이는 후속질문 생성, 피드백 품질에 직접적 영향. AI 기반 STT로 전환하여 인식 정확도를 대폭 개선해야 함.

## Issues

| # | 제목 | 타입 |
|---|------|------|
| #47 | 음성인식 정확도 개선 (AI 기반 STT 전환 검토) | enhancement |

## 의존성

- Task 1 (영상 파이프라인) 완료 후 시작 — 연속 녹화 방식에서 오디오 스트림 추출 방식이 결정되어야 함
- Task 2 (AI 질문) 완료 후 시작 — 후속질문이 STT 결과에 의존

## 구현 계획

### Phase 1: 리서치 (구현 전)

STT 옵션 비교:

| 옵션 | 장점 | 단점 | 비용 |
|------|------|------|------|
| Whisper API (OpenAI) | 높은 한국어 정확도, 검증됨 | 비용, 레이턴시 | ~$0.006/min |
| Google Cloud STT | 실시간 스트리밍 지원 | 설정 복잡 | ~$0.006/15s |
| Claude Audio (Anthropic) | 기존 인프라 활용 | 아직 제한적 | TBD |
| Whisper.cpp (로컬) | 무료, 프라이버시 | 브라우저 성능 | 무료 |

결정 기준:
1. 한국어 인식 정확도
2. 실시간 인식(interim results) 필요 여부
3. 비용
4. 구현 복잡도

### Phase 2: 구현

#### 옵션 A — 클라이언트 사이드 (Whisper.cpp WASM)
- **PR**: FE 1개
- `use-speech-recognition.ts` → Whisper WASM 모듈로 교체
- 오디오 청크를 주기적으로 Whisper에 전달
- interim results 시뮬레이션

#### 옵션 B — 서버 사이드 (Whisper API / Google STT)
- **PR**: BE 1개 + FE 1개
- BE: 오디오 수신 엔드포인트 + STT API 호출 + 결과 반환
- FE: 오디오 스트림을 BE로 전송 + 결과 수신

관련 파일:
- `frontend/src/hooks/use-speech-recognition.ts`
- `frontend/src/hooks/use-answer-flow.ts` (STT 결과 소비)
- (서버사이드 선택 시) `backend/src/.../SttService.java` (신규)

**Agent**: `frontend` (리서치 + 구현), `architect-reviewer` + `code-reviewer` (리뷰)

## Acceptance Criteria

- [ ] STT 옵션 비교 리서치 문서 작성 완료
- [ ] 선택된 STT 솔루션 통합 완료
- [ ] 한국어 인식 정확도가 Web Speech API 대비 체감 향상
- [ ] 기존 후속질문/피드백 플로우와 호환
- [ ] 실시간 또는 준실시간 텍스트 표시
- [ ] 에러 핸들링 (네트워크 실패, 모델 로드 실패 등)
