# Task 6: 분석 Lambda — FFmpeg + Whisper + Vision + LLM 파이프라인

## Status: Not Started

## Issue: #84

## Why

서버 사이드 분석이 새 파이프라인의 핵심. 클라이언트 MediaPipe/Web Audio 대신 Lambda에서 Whisper STT + GPT-4o Vision 비언어 분석 + LLM 언어 분석을 수행.

상세 흐름: `docs/architecture/recording-analysis-pipeline.md` 3단계 A. 분석 Lambda

## 의존성

- 선행: Task 2 (Lambda 인프라), Task 3 (외부 API), Task 4 (내부 API)
- 후행: Task 9 (분석 결과를 FE가 폴링)

## 구현 계획

### PR 1: [Lambda] 분석 파이프라인 (Python)

**입력:** S3 이벤트 (영상 업로드 알림)

**처리 단계:**

1. **멱등성 체크**: 내부 API로 analysis_status 확인 → PENDING_UPLOAD 아니면 스킵
2. **상태 업데이트**: progress = STARTED
3. **영상 다운로드**: S3에서 /tmp로 다운로드
4. **FFmpeg 추출**: progress = EXTRACTING
   - 오디오: WAV 추출
   - 프레임: 3초 간격 JPEG 추출
5. **Whisper STT**: progress = STT_PROCESSING
   - 오디오 → OpenAI Whisper API → 타임스탬프 포함 텍스트
6. **비언어 분석**: progress = NONVERBAL_ANALYZING
   - 프레임 5~10장씩 배치 → GPT-4o Vision API (low detail)
   - 시선, 표정, 자세 평가
7. **언어 분석**: progress = VERBAL_ANALYZING
   - STT 텍스트 → GPT-4o → 논리성, 키워드, 필러워드, 말투, 발화속도 평가
   - (6, 7은 asyncio.gather로 병렬 실행)
8. **종합 평가**: progress = FINALIZING
   - 질문세트 점수 + 코멘트 생성
9. **결과 저장**: 내부 API POST /feedback → analysis_status = COMPLETED

**실패 처리:**
- 각 단계별 3회 재시도
- 실패 시 progress = FAILED + failure_reason + failure_detail
- API 서버 호출 실패 시 S3에 JSON 백업

**기술 스택:**
- Python 3.12, boto3, ffmpeg-python, openai, httpx
- Lambda Layer: FFmpeg 바이너리

- Implement: `backend` (Python Lambda)
- Review: `architect-reviewer` + `code-reviewer`

## Acceptance Criteria

- [ ] S3 이벤트로 자동 트리거
- [ ] 멱등성: 중복 이벤트 시 스킵
- [ ] 단계별 progress 업데이트 (7단계)
- [ ] Whisper STT 결과에 타임스탬프 포함
- [ ] Vision API 비언어 분석 결과 생성
- [ ] LLM 언어 분석 결과 생성
- [ ] 분석 결과 API 서버에 정상 저장
- [ ] 실패 시 FAILED + 사유 기록
