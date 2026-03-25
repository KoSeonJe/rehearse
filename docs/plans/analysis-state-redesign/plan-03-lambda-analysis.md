# Plan 03: Lambda Analysis 수정

> 상태: Draft
> 작성일: 2026-03-24

## Why

Backend의 AnalysisProgress enum이 삭제되고 AnalysisStatus로 통합되므로, Lambda에서 update_progress 호출 시 전달하는 상태 문자열을 변경해야 한다. 또한 save_feedback payload에 isVerbalCompleted/isNonverbalCompleted를 추가해야 한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `lambda/analysis/handler.py` | update_progress 상태값 변경. save_feedback payload에 isVerbalCompleted/isNonverbalCompleted 추가. 레거시 progress 값 제거 |
| `lambda/analysis/api_client.py` | update_progress 함수 파라미터명 변경 (progress → status) |

## 상세

### handler.py 상태값 변경

```python
# 변경 전 → 변경 후
update_progress(..., "STARTED")              → 제거 (BE가 이미 PENDING_UPLOAD 상태, Lambda 시작 시 불필요)
update_progress(..., "EXTRACTING")           → 유지 (PENDING_UPLOAD → EXTRACTING 전이)
update_progress(..., "ANALYZING")            → 유지
update_progress(..., "FINALIZING")           → 유지
update_progress(..., "STT_PROCESSING")       → update_progress(..., "ANALYZING")   # 레거시 폴백: 분석 단계로 통합
update_progress(..., "NONVERBAL_ANALYZING")  → 제거 (ANALYZING에 이미 포함)
update_progress(..., "FAILED")               → 유지
```

**주의**: 기존 `handler.py:81`의 `update_progress("STARTED")`를 제거해야 함.
`handler.py:89`의 `update_progress("EXTRACTING")`이 첫 상태 업데이트가 됨.
두 번 호출하면 EXTRACTING → EXTRACTING 전이가 불가하여 IllegalStateException 발생.

### 모델 단위 재시도 (Gemini + Vision)

API 장애 시 개별 답변이 아니라 **모델 전체**가 실패한다. Lambda 내부에서 전체 실패한 모델만 1회 재시도한다.

```python
def _run_gemini_pipeline(answers, audio_paths, frame_paths, ...):
    with ThreadPoolExecutor(max_workers=6) as executor:
        gemini_futures = [executor.submit(_safe_gemini_audio, ...) for ...]
        vision_futures = [executor.submit(_safe_vision, ...) for ...]

        gemini_results = [f.result() for f in gemini_futures]
        vision_results = [f.result() for f in vision_futures]

    # Gemini 전체 실패 → 1회 재시도 (2초 대기)
    if all(r is None for r in gemini_results):
        print("[Analysis] Gemini 전체 실패, 2초 후 1회 재시도")
        time.sleep(2)
        with ThreadPoolExecutor(max_workers=3) as executor:
            gemini_futures = [executor.submit(_safe_gemini_audio, ...) for ...]
            gemini_results = [f.result() for f in gemini_futures]

        # 재시도도 전체 실패 → 폴백 (Whisper+GPT-4o)
        if all(r is None for r in gemini_results):
            raise RuntimeError("Gemini retry failed — triggering fallback")

    # Vision 전체 실패 → 1회 재시도 (2초 대기)
    if all(r is None for r in vision_results):
        print("[Analysis] Vision 전체 실패, 2초 후 1회 재시도")
        time.sleep(2)
        with ThreadPoolExecutor(max_workers=3) as executor:
            vision_futures = [executor.submit(_safe_vision, ...) for ...]
            vision_results = [f.result() for f in vision_futures]
        # 재시도도 실패 → vision_results는 전부 None → PARTIAL로 저장

    return gemini_results, vision_results
```

**재시도 흐름 정리:**

```
Gemini 전체 실패 → 2초 대기 → 재시도 → 성공: 계속 진행
                                     → 실패: Whisper+GPT-4o 폴백
Vision 전체 실패 → 2초 대기 → 재시도 → 성공: COMPLETED
                                     → 실패: PARTIAL (언어 피드백만 제공)
```

### save_feedback payload 변경

```python
# 재시도 후 최종 결과로 판별
verbal_ok = any(r is not None for r in gemini_results)
nonverbal_ok = any(r is not None for r in vision_results)

feedback_payload = {
    "questionSetScore": overall_score,
    "questionSetComment": overall_comment,
    "timestampFeedbacks": timestamp_feedbacks,
    "isVerbalCompleted": verbal_ok,        # 신규
    "isNonverbalCompleted": nonverbal_ok,  # 신규
}
```

### 레거시 폴백 경로 수정

```python
# _run_legacy_pipeline 내부
# 변경 전:
update_progress(interview_id, question_set_id, "STT_PROCESSING")
# ...
update_progress(interview_id, question_set_id, "NONVERBAL_ANALYZING")

# 변경 후:
update_progress(interview_id, question_set_id, "ANALYZING")
# NONVERBAL_ANALYZING 호출 제거 (ANALYZING에 포함)
```

### api_client.py 변경

```python
# 변경 전
def update_progress(interview_id, question_set_id, progress, ...):
    body = {"progress": progress}

# 변경 후
def update_progress(interview_id, question_set_id, status, ...):
    body = {"status": status}
```

## 담당 에이전트

- Implement: `backend` (Lambda Python)
- Review: `code-reviewer` — 폴백 경로 정합성, payload 구조

## 검증

- Gemini 경로 정상: EXTRACTING → ANALYZING → FINALIZING → COMPLETED
- Vision 전체 실패 → 재시도 → 성공: COMPLETED
- Vision 전체 실패 → 재시도 → 실패: PARTIAL (isNonverbalCompleted=false)
- Gemini 전체 실패 → 재시도 → 성공: COMPLETED
- Gemini 전체 실패 → 재시도 → 실패: 폴백(Whisper+GPT-4o) 전환
- 레거시 폴백 경로: EXTRACTING → ANALYZING → FINALIZING (STT_PROCESSING, NONVERBAL_ANALYZING 제거 확인)
- `progress.md` 상태 업데이트 (Task 3 → Completed)
