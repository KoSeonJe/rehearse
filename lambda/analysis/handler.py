import json
import os
import shutil
import traceback
import urllib.parse
from uuid import uuid4

import boto3

import api_client
from api_client import get_answers, update_progress, save_feedback, backup_to_s3
from config import Config
from extractors.ffmpeg_extractor import extract_audio, extract_frames, get_video_duration_ms
from analyzers.stt_analyzer import transcribe
from analyzers.vision_analyzer import analyze_frames
from analyzers.verbal_analyzer import analyze_verbal

WORK_DIR = "/tmp/analysis"


def lambda_handler(event, context):
    """S3 PutObject 이벤트 → 분석 파이프라인 (FFmpeg + Whisper + Vision + LLM)"""
    detail = event.get("detail", {})
    bucket = detail.get("bucket", {}).get("name", "")
    key = urllib.parse.unquote_plus(detail.get("object", {}).get("key", ""))

    print(f"[Analysis] Triggered: bucket={bucket}, key={key}")

    if not key.startswith("videos/") or not key.endswith(".webm"):
        print(f"[Analysis] Skipping non-video key: {key}")
        return {"statusCode": 200, "body": "Skipped"}

    interview_id, question_set_id = _parse_ids_from_key(key)
    if not interview_id or not question_set_id:
        print(f"[Analysis] ID 파싱 실패: {key}")
        return {"statusCode": 400, "body": "Invalid key format"}

    try:
        return _run_pipeline(interview_id, question_set_id, bucket, key)
    except Exception as e:
        error_msg = str(e)
        error_detail = traceback.format_exc()
        print(f"[Analysis] 파이프라인 실패: {error_msg}\n{error_detail}")
        _safe_update_progress(
            interview_id, question_set_id, "FAILED",
            failure_reason=_classify_error(e),
            failure_detail=f"{error_msg}\n\n{error_detail[:1800]}",
        )
        return {"statusCode": 500, "body": json.dumps({"error": error_msg})}
    finally:
        api_client.set_correlation_id(None)
        _cleanup()


def _run_pipeline(interview_id: int, question_set_id: int, bucket: str, key: str) -> dict:
    correlation_id = f"{interview_id}-{question_set_id}-{uuid4().hex[:8]}"
    api_client.set_correlation_id(correlation_id)
    print(f"[Analysis] 파이프라인 시작: interview={interview_id}, qs={question_set_id}, correlation_id={correlation_id}")

    # 1. 멱등성 체크
    answers_data = get_answers(interview_id, question_set_id)
    analysis_status = answers_data.get("analysisStatus", "")
    answers = answers_data.get("answers", [])

    if analysis_status == "COMPLETED":
        print(f"[Analysis] 이미 완료됨 — 스킵")
        return {"statusCode": 200, "body": "Already completed"}

    update_progress(interview_id, question_set_id, "STARTED")

    # 2. S3에서 영상 다운로드
    os.makedirs(WORK_DIR, exist_ok=True)
    video_path = os.path.join(WORK_DIR, "video.webm")
    s3 = boto3.client("s3")
    s3.download_file(bucket, key, video_path)
    print(f"[Analysis] 영상 다운로드 완료: {key}")

    # 3. FFmpeg: 오디오 + 프레임 추출
    update_progress(interview_id, question_set_id, "EXTRACTING")
    audio_path = extract_audio(video_path, WORK_DIR)
    frame_paths = extract_frames(video_path, WORK_DIR)
    video_duration_ms = get_video_duration_ms(video_path)

    # 4. Whisper STT
    update_progress(interview_id, question_set_id, "STT_PROCESSING")
    stt_result = _safe_stt(audio_path)

    # 5. GPT-4o Vision 비언어 분석
    update_progress(interview_id, question_set_id, "NONVERBAL_ANALYZING")
    vision_result = _safe_vision(frame_paths)

    # 6. GPT-4o LLM 언어 분석 (답변별)
    update_progress(interview_id, question_set_id, "VERBAL_ANALYZING")
    timestamp_feedbacks = _build_timestamp_feedbacks(
        answers, stt_result, vision_result, video_duration_ms
    )

    # 7. 종합 점수 계산 + 피드백 저장
    update_progress(interview_id, question_set_id, "FINALIZING")
    overall_score, overall_comment = _compute_overall(timestamp_feedbacks)

    feedback_payload = {
        "questionSetScore": overall_score,
        "questionSetComment": overall_comment,
        "timestampFeedbacks": timestamp_feedbacks,
    }

    try:
        save_feedback(interview_id, question_set_id, feedback_payload)
    except Exception as e:
        print(f"[Analysis] 피드백 저장 API 실패, S3 백업 시도: {e}")
        backup_to_s3(interview_id, question_set_id, feedback_payload)
        raise

    print(f"[Analysis] 파이프라인 완료: interview={interview_id}, qs={question_set_id}")
    return {"statusCode": 200, "body": json.dumps({"message": "Analysis complete"})}


def _build_timestamp_feedbacks(
    answers: list[dict],
    stt_result: dict | None,
    vision_result: dict | None,
    video_duration_ms: int,
) -> list[dict]:
    feedbacks = []
    stt_text = stt_result["full_text"] if stt_result else ""
    stt_segments = stt_result["segments"] if stt_result else []

    for answer in answers:
        start_ms = answer["startMs"]
        end_ms = answer["endMs"]
        question_text = answer.get("questionText", "")
        question_id = answer.get("questionId")

        # 해당 답변 구간의 STT 텍스트 추출
        transcript = _extract_transcript_for_range(stt_segments, start_ms, end_ms)
        if not transcript and stt_text and len(answers) == 1:
            transcript = stt_text

        # 언어 분석
        verbal = _safe_verbal(question_text, transcript)

        fb = {
            "questionId": question_id,
            "startMs": start_ms,
            "endMs": end_ms,
            "transcript": transcript or "",
        }

        if verbal:
            fb["verbalScore"] = verbal.get("verbal_score")
            fb["verbalComment"] = verbal.get("comment")
            fb["fillerWordCount"] = verbal.get("filler_word_count", 0)

        if vision_result:
            fb["eyeContactScore"] = vision_result.get("eye_contact_score")
            fb["postureScore"] = vision_result.get("posture_score")
            fb["expressionLabel"] = vision_result.get("expression_label")
            fb["nonverbalComment"] = vision_result.get("comment")

        # 종합 코멘트
        fb["overallComment"] = _build_overall_comment(verbal, vision_result)

        feedbacks.append(fb)

    return feedbacks


def _extract_transcript_for_range(
    segments: list[dict], start_ms: int, end_ms: int
) -> str:
    texts = []
    for seg in segments:
        seg_start = seg.get("start_ms", 0)
        seg_end = seg.get("end_ms", 0)
        if seg_end > start_ms and seg_start < end_ms:
            texts.append(seg["text"])
    return " ".join(texts)


def _compute_overall(feedbacks: list[dict]) -> tuple[int, str]:
    verbal_scores = [f["verbalScore"] for f in feedbacks if f.get("verbalScore") is not None]
    nonverbal_scores = []
    for f in feedbacks:
        scores = [s for s in [f.get("eyeContactScore"), f.get("postureScore")] if s is not None]
        if scores:
            nonverbal_scores.append(sum(scores) / len(scores))

    avg_verbal = sum(verbal_scores) / len(verbal_scores) if verbal_scores else 50
    avg_nonverbal = sum(nonverbal_scores) / len(nonverbal_scores) if nonverbal_scores else 50

    overall = int(avg_verbal * 0.6 + avg_nonverbal * 0.4)

    parts = []
    if verbal_scores:
        parts.append(f"언어 분석 평균 {int(avg_verbal)}점")
    if nonverbal_scores:
        parts.append(f"비언어 분석 평균 {int(avg_nonverbal)}점")
    parts.append(f"종합 {overall}점입니다.")

    comment = "전반적으로 " + ", ".join(parts)
    if overall >= 80:
        comment += " 좋은 면접 수행을 보여주셨습니다."
    elif overall >= 60:
        comment += " 몇 가지 개선 포인트가 있습니다."
    else:
        comment += " 연습을 통해 개선이 필요합니다."

    return overall, comment


def _build_overall_comment(verbal: dict | None, vision: dict | None) -> str:
    parts = []
    if verbal and verbal.get("comment"):
        parts.append(verbal["comment"])
    if verbal and verbal.get("tone_comment"):
        parts.append(f"[말투] {verbal['tone_comment']}")
    if vision and vision.get("comment"):
        parts.append(vision["comment"])
    return " ".join(parts) if parts else "분석 결과를 확인해주세요."


def _safe_stt(audio_path: str) -> dict | None:
    try:
        return transcribe(audio_path)
    except Exception as e:
        print(f"[Analysis] STT 실패 (비언어만 분석): {e}")
        return None


def _safe_vision(frame_paths: list[str]) -> dict | None:
    try:
        return analyze_frames(frame_paths)
    except Exception as e:
        print(f"[Analysis] Vision 분석 실패 (언어만 분석): {e}")
        return None


def _safe_verbal(question_text: str, transcript: str) -> dict | None:
    try:
        return analyze_verbal(question_text, transcript)
    except Exception as e:
        print(f"[Analysis] Verbal 분석 실패: {e}")
        return None


def _safe_update_progress(interview_id, question_set_id, progress, **kwargs):
    try:
        update_progress(interview_id, question_set_id, progress, **kwargs)
    except Exception as e:
        print(f"[Analysis] 진행 상태 업데이트 실패: {e}")


def _parse_ids_from_key(key: str) -> tuple[int | None, int | None]:
    """S3 key에서 interviewId, questionSetId 파싱.

    Expected format: videos/{interviewId}/qs_{questionSetId}.webm
    """
    try:
        parts = key.split("/")
        interview_id = int(parts[1])
        filename = parts[2]  # qs_{id}.webm
        qs_id = int(filename.split("_")[1].split(".")[0])
        return interview_id, qs_id
    except (IndexError, ValueError):
        return None, None


def _cleanup():
    if os.path.exists(WORK_DIR):
        shutil.rmtree(WORK_DIR, ignore_errors=True)


def _classify_error(e: Exception) -> str:
    """에러를 표준 분류 코드로 변환"""
    error_str = str(e).lower()
    if isinstance(e, TimeoutError) or 'timeout' in error_str:
        return "TIMEOUT"
    if 'openai' in error_str or '429' in error_str or '503' in error_str:
        return "API_ERROR"
    if 'ffmpeg' in error_str or 'whisper' in error_str:
        return "TRANSCRIPTION_ERROR"
    if 'vision' in error_str or 'frame' in error_str:
        return "VISION_ERROR"
    return "INTERNAL_ERROR"
