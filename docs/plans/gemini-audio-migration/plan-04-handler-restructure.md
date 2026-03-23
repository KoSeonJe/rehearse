# Plan 04: handler.py 파이프라인 재구조화

> 상태: Draft
> 작성일: 2026-03-23

## Why

현재 handler는 완전 동기식으로 STT → Vision → Verbal을 순차 실행한다. Gemini 음성 분석과 Vision 비언어 분석은 서로 독립적이므로 병렬 실행하면 총 소요 시간을 단축할 수 있다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `lambda/analysis/handler.py` | 파이프라인 재구조화 (병렬 실행 + Gemini 통합) |

## 상세

### 실행 흐름 변경

**Before (순차)**:
```
STARTED → EXTRACTING → STT_PROCESSING → NONVERBAL_ANALYZING → FINALIZING
  전체 WAV 추출 → Whisper STT → 답변별 Vision → 답변별 Verbal → 가중 평균
```

**After (병렬)**:
```
STARTED → EXTRACTING → ANALYZING → FINALIZING
  답변별 mp3 추출 → ┬─ Gemini 음성 × N (병렬)
  답변별 프레임 추출   └─ Vision × N (병렬)
                      → Gemini 종합 평가 × 1
```

### 병렬 실행 방식

Lambda에서 asyncio 또는 ThreadPoolExecutor 사용:

```python
from concurrent.futures import ThreadPoolExecutor

with ThreadPoolExecutor(max_workers=6) as executor:
    # Gemini 음성 분석 (최대 3건)
    gemini_futures = [
        executor.submit(_safe_gemini_audio, audio_path, question, context)
        for audio_path, question, context in zip(audio_paths, questions, contexts)
    ]
    # Vision 비언어 분석 (최대 3건)
    vision_futures = [
        executor.submit(_safe_vision, frames)
        for frames in answer_frame_lists
    ]

    gemini_results = [f.result() for f in gemini_futures]
    vision_results = [f.result() for f in vision_futures]
```

### Progress 상태 변경

- `STT_PROCESSING`, `VERBAL_ANALYZING`, `NONVERBAL_ANALYZING` → `ANALYZING` 1개로 통합
- BE `AnalysisProgress` enum에 `ANALYZING` 추가 필요

### 기존 코드 보존

- `stt_analyzer.py`, `verbal_analyzer.py`는 삭제하지 않음
- import만 `gemini_analyzer`로 변경
- 폴백 시 기존 함수 호출 가능하도록 유지

### 피드백 조립 변경

```python
for i, answer in enumerate(answers):
    gemini = gemini_results[i]
    vision = vision_results[i]

    fb = {
        "questionId": answer["questionId"],
        "startMs": answer["startMs"],
        "endMs": answer["endMs"],
        # Gemini 결과 매핑
        "transcript": gemini["transcript"],
        "verbalScore": gemini["verbal"]["score"],
        "verbalComment": gemini["verbal"]["comment"],
        "fillerWordCount": gemini["vocal"]["fillerWordCount"],
        # 신규 음성 필드
        "fillerWords": gemini["vocal"]["fillerWords"],
        "speechPace": gemini["vocal"]["speechPace"],
        "toneConfidence": gemini["vocal"]["toneConfidence"],
        "emotionLabel": gemini["vocal"]["emotionLabel"],
        "vocalComment": gemini["vocal"]["comment"],
        # Vision 결과 (기존 유지)
        "eyeContactScore": vision["eye_contact_score"],
        "postureScore": vision["posture_score"],
        "expressionLabel": vision["expression_label"],
        "nonverbalComment": vision["comment"],
        "overallComment": gemini["overallComment"],
    }
```

## 담당 에이전트

- Implement: `executor` — 파이프라인 재구조화
- Review: `architect-reviewer` — 병렬 실행 구조, 에러 전파
- Review: `code-reviewer` — 스레드 안전성, 리소스 관리

## 검증

- 기존 S3 영상으로 E2E 테스트
- Gemini 음성 + Vision 비언어가 병렬 실행되는지 로그 확인
- 총 소요 시간 측정 (CloudWatch REPORT)
- 실패 시 개별 폴백 동작 확인
- `progress.md` 상태 업데이트 (Task 4 → Completed)
