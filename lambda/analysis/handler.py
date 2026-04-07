from __future__ import annotations

import json
import os
import re
import shutil
import time
import traceback
import urllib.parse
from concurrent.futures import ThreadPoolExecutor
from uuid import uuid4

import boto3

import api_client
from api_client import get_answers, update_progress, save_feedback, backup_to_s3
from config import Config
from extractors.ffmpeg_extractor import extract_audio, extract_answer_audios, extract_frames, get_video_duration_ms
from analyzers.gemini_analyzer import analyze_answer_audio
from analyzers.stt_analyzer import transcribe_chunked
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
            failure_detail=_sanitize_error_detail(error_msg),
        )
        raise
    finally:
        api_client.set_correlation_id(None)
        _cleanup()


def _run_pipeline(interview_id: int, question_set_id: int, bucket: str, key: str) -> dict:
    correlation_id = f"{interview_id}-{question_set_id}-{uuid4().hex[:8]}"
    api_client.set_correlation_id(correlation_id)
    print(f"[Analysis] 파이프라인 시작: interview={interview_id}, qs={question_set_id}, correlation_id={correlation_id}")

    answers_data = get_answers(interview_id, question_set_id)
    analysis_status = answers_data.get("analysisStatus", "")
    answers = answers_data.get("answers", [])

    if analysis_status == "COMPLETED":
        print(f"[Analysis] 이미 완료됨 — 스킵")
        return {"statusCode": 200, "body": "Already completed"}

    if not answers:
        print(f"[Analysis] 답변 없음 — 스킵")
        return {"statusCode": 200, "body": "No answers"}

    position = answers_data.get("position")
    tech_stack = answers_data.get("techStack")
    level = answers_data.get("level")

    # STARTED 호출 제거 — BE가 이미 PENDING_UPLOAD 상태, EXTRACTING이 첫 상태 업데이트

    os.makedirs(WORK_DIR, exist_ok=True)
    video_path = os.path.join(WORK_DIR, "video.webm")
    s3 = boto3.client("s3")
    s3.download_file(bucket, key, video_path)
    print(f"[Analysis] 영상 다운로드 완료: {key}")

    update_progress(interview_id, question_set_id, "EXTRACTING")
    frame_paths = extract_frames(video_path, WORK_DIR)
    video_duration_ms = get_video_duration_ms(video_path)

    audio_path = None
    answer_audio_paths = None

    if Config.USE_GEMINI:
        # Gemini 경로: 답변별 mp3 추출
        audio_dir = os.path.join(WORK_DIR, "answer_audios")
        answer_audio_paths = extract_answer_audios(video_path, answers, audio_dir)
    else:
        # 폴백 경로: 전체 WAV 추출 (기존)
        audio_path = extract_audio(video_path, WORK_DIR)

    if Config.USE_GEMINI and answer_audio_paths:
        update_progress(interview_id, question_set_id, "ANALYZING")
        try:
            timestamp_feedbacks, verbal_ok, nonverbal_ok = _run_gemini_pipeline(
                answers, answer_audio_paths, frame_paths, video_duration_ms,
                position=position, tech_stack=tech_stack, level=level,
            )
        except Exception as e:
            print(f"[Analysis] Gemini 파이프라인 실패, Whisper+GPT-4o 폴백: {e}")
            # 폴백: Gemini 아티팩트 정리 후 기존 경로
            audio_dir = os.path.join(WORK_DIR, "answer_audios")
            if os.path.exists(audio_dir):
                shutil.rmtree(audio_dir, ignore_errors=True)
            audio_path = extract_audio(video_path, WORK_DIR)
            timestamp_feedbacks = _run_legacy_pipeline(
                answers, audio_path, frame_paths, video_duration_ms,
                interview_id, question_set_id,
                position=position, tech_stack=tech_stack, level=level,
                skip_analyzing_update=True,
            )
            # 레거시 폴백은 이미 ANALYZING 상태이므로 중복 호출 스킵
            verbal_ok = any(f.get("verbalComment") is not None for f in timestamp_feedbacks)
            nonverbal_ok = any(f.get("eyeContactLevel") is not None for f in timestamp_feedbacks)
    else:
        # 레거시 경로 (USE_GEMINI=false 또는 answer_audio_paths가 빈 경우)
        if audio_path is None:
            audio_path = extract_audio(video_path, WORK_DIR)
        timestamp_feedbacks = _run_legacy_pipeline(
            answers, audio_path, frame_paths, video_duration_ms,
            interview_id, question_set_id,
            position=position, tech_stack=tech_stack, level=level,
        )
        verbal_ok = any(f.get("verbalComment") is not None for f in timestamp_feedbacks)
        nonverbal_ok = any(f.get("eyeContactLevel") is not None for f in timestamp_feedbacks)

    update_progress(interview_id, question_set_id, "FINALIZING")

    overall_comment = _compute_overall(timestamp_feedbacks)

    feedback_payload = {
        "questionSetComment": overall_comment,
        "timestampFeedbacks": timestamp_feedbacks,
        "isVerbalCompleted": verbal_ok,
        "isNonverbalCompleted": nonverbal_ok,
    }

    try:
        save_feedback(interview_id, question_set_id, feedback_payload)
    except Exception as e:
        print(f"[Analysis] 피드백 저장 API 실패, S3 백업 시도: {e}")
        backup_to_s3(interview_id, question_set_id, feedback_payload)
        raise

    print(f"[Analysis] 파이프라인 완료: interview={interview_id}, qs={question_set_id}, verbal={verbal_ok}, nonverbal={nonverbal_ok}")
    return {"statusCode": 200, "body": json.dumps({"message": "Analysis complete"})}


def _run_gemini_pipeline(
    answers, audio_paths, frame_paths, video_duration_ms,
    position=None, tech_stack=None, level=None,
) -> tuple[list[dict], bool, bool]:
    """Gemini 음성 + Vision 비언어를 병렬 실행한다. 모델 단위 재시도 포함."""

    with ThreadPoolExecutor(max_workers=6) as executor:
        # Gemini 음성 분석 (답변별)
        gemini_futures = [
            executor.submit(
                _safe_gemini_audio,
                audio_paths[i],
                answer.get("questionText", ""),
                position, tech_stack, level,
                answer.get("modelAnswer"),
                answer.get("feedbackPerspective", "TECHNICAL"),
            )
            for i, answer in enumerate(answers)
        ]

        # Vision 비언어 분석 (답변별)
        vision_futures = [
            executor.submit(
                _safe_vision,
                _filter_frames_for_range(frame_paths, answer["startMs"], answer["endMs"]),
            )
            for answer in answers
        ]

        gemini_results = [f.result() for f in gemini_futures]
        vision_results = [f.result() for f in vision_futures]

    # Gemini 전체 실패 → 2초 대기 → 1회 재시도
    if all(r is None for r in gemini_results):
        print("[Analysis] Gemini 전체 실패, 2초 후 1회 재시도")
        time.sleep(2)
        with ThreadPoolExecutor(max_workers=3) as executor:
            gemini_futures = [
                executor.submit(
                    _safe_gemini_audio,
                    audio_paths[i],
                    answer.get("questionText", ""),
                    position, tech_stack, level,
                    answer.get("modelAnswer"),
                    answer.get("feedbackPerspective", "TECHNICAL"),
                )
                for i, answer in enumerate(answers)
            ]
            gemini_results = [f.result() for f in gemini_futures]

        # 재시도도 전체 실패 → 폴백 (Whisper+GPT-4o)
        if all(r is None for r in gemini_results):
            raise RuntimeError("Gemini retry failed — triggering fallback")

    # Vision 전체 실패 → 2초 대기 → 1회 재시도
    if all(r is None for r in vision_results):
        print("[Analysis] Vision 전체 실패, 2초 후 1회 재시도")
        time.sleep(2)
        with ThreadPoolExecutor(max_workers=3) as executor:
            vision_futures = [
                executor.submit(
                    _safe_vision,
                    _filter_frames_for_range(frame_paths, answer["startMs"], answer["endMs"]),
                )
                for answer in answers
            ]
            vision_results = [f.result() for f in vision_futures]
        # 재시도도 실패 → vision_results는 전부 None → PARTIAL로 저장

    verbal_ok = any(r is not None for r in gemini_results)
    nonverbal_ok = any(r is not None for r in vision_results)

    # 피드백 조립
    timestamp_feedbacks = []
    for i, answer in enumerate(answers):
        gemini = gemini_results[i]
        vision = vision_results[i]

        fb = {
            "questionId": answer.get("questionId"),
            "startMs": answer["startMs"],
            "endMs": answer["endMs"],
            "transcript": gemini.get("transcript", "") if gemini else "",
            "attitudeComment": None,
        }

        if gemini:
            verbal = gemini.get("verbal", {})
            vocal = gemini.get("vocal", {})
            technical = gemini.get("technical", {})
            fb["verbalComment"] = _comment_block(verbal)
            filler_list = vocal.get("fillerWords", [])
            fb["fillerWords"] = filler_list
            fb["fillerWordCount"] = len(filler_list) if isinstance(filler_list, list) else 0
            fb["speechPace"] = vocal.get("speechPace")
            fb["toneConfidenceLevel"] = vocal.get("toneConfidenceLevel")
            fb["emotionLabel"] = vocal.get("emotionLabel")
            fb["vocalComment"] = _comment_block(vocal)
            fb["accuracyIssues"] = json.dumps(technical.get("accuracyIssues", []), ensure_ascii=False)
            fb["coachingStructure"] = technical.get("coaching", {}).get("structure", "")
            fb["coachingImprovement"] = technical.get("coaching", {}).get("improvement", "")

            attitude = gemini.get("attitude", {})
            fb["attitudeComment"] = _comment_block(attitude)

        if vision:
            fb["eyeContactLevel"] = vision.get("eyeContactLevel")
            fb["postureLevel"] = vision.get("postureLevel")
            fb["expressionLabel"] = vision.get("expressionLabel")
            fb["nonverbalComment"] = _comment_block(vision)

        fb["overallComment"] = _comment_block(gemini.get("overall")) if gemini else None
        timestamp_feedbacks.append(fb)

    return timestamp_feedbacks, verbal_ok, nonverbal_ok


def _run_legacy_pipeline(
    answers, audio_path, frame_paths, video_duration_ms,
    interview_id, question_set_id,
    position=None, tech_stack=None, level=None,
    skip_analyzing_update=False,
) -> list[dict]:
    """기존 Whisper+GPT-4o 파이프라인 (폴백용)."""

    if not skip_analyzing_update:
        update_progress(interview_id, question_set_id, "ANALYZING")
    stt_result = _safe_stt(audio_path)

    timestamp_feedbacks = _build_timestamp_feedbacks(
        answers, stt_result, frame_paths, video_duration_ms,
        position=position, tech_stack=tech_stack, level=level,
    )

    return timestamp_feedbacks


def _safe_gemini_audio(
    audio_path, question_text,
    position=None, tech_stack=None, level=None, model_answer=None,
    feedback_perspective=None,
) -> dict | None:
    try:
        return analyze_answer_audio(
            audio_path, question_text,
            position=position, tech_stack=tech_stack,
            level=level, model_answer=model_answer,
            feedback_perspective=feedback_perspective,
        )
    except Exception as e:
        print(f"[Analysis] Gemini 음성 분석 실패: {e}")
        return None


def _build_timestamp_feedbacks(
    answers: list[dict],
    stt_result: dict | None,
    frame_paths: list[str],
    video_duration_ms: int,
    position: str | None = None,
    tech_stack: str | None = None,
    level: str | None = None,
) -> list[dict]:
    feedbacks = []
    stt_text = stt_result["full_text"] if stt_result else ""
    stt_segments = stt_result["segments"] if stt_result else []

    for answer in answers:
        start_ms = answer["startMs"]
        end_ms = answer["endMs"]
        question_text = answer.get("questionText", "")
        question_id = answer.get("questionId")

        transcript = _extract_transcript_for_range(stt_segments, start_ms, end_ms)
        if not transcript and stt_text and len(answers) == 1:
            transcript = stt_text

        # 답변별 프레임 필터링 + Vision 분석
        answer_frames = _filter_frames_for_range(frame_paths, start_ms, end_ms)
        vision_result = _safe_vision(answer_frames) if answer_frames else None

        verbal = _safe_verbal(
            question_text, transcript,
            position=position, tech_stack=tech_stack,
            level=level, model_answer=answer.get("modelAnswer"),
            feedback_perspective=answer.get("feedbackPerspective", "TECHNICAL"),
        )

        fb = {
            "questionId": question_id,
            "startMs": start_ms,
            "endMs": end_ms,
            "transcript": transcript or "",
            "attitudeComment": None,
        }

        if verbal:
            # 레거시 verbal_analyzer는 ✓△→ string을 반환 → CommentBlock 객체로 래핑
            # (BE는 CommentBlock POJO로 역직렬화하므로 string을 그대로 보내면 400)
            fb["verbalComment"] = _legacy_string_to_block(verbal.get("comment"))
            fb["fillerWordCount"] = verbal.get("filler_word_count", 0)
            fb["fillerWords"] = []
            fb["speechPace"] = ""
            fb["toneConfidenceLevel"] = _tone_label_to_level(verbal.get("tone_label"))
            fb["emotionLabel"] = ""
            fb["vocalComment"] = None
            fb["accuracyIssues"] = "[]"
            fb["coachingStructure"] = ""
            fb["coachingImprovement"] = ""

            fb["attitudeComment"] = _legacy_string_to_block(verbal.get("attitude_comment"))

        if vision_result:
            fb["eyeContactLevel"] = vision_result.get("eyeContactLevel")
            fb["postureLevel"] = vision_result.get("postureLevel")
            fb["expressionLabel"] = vision_result.get("expressionLabel")
            fb["nonverbalComment"] = _comment_block(vision_result)

        fb["overallComment"] = None

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


def _compute_overall(feedbacks: list[dict]) -> str:
    """라벨 기반 종합 코멘트를 생성한다."""
    verbal_count = sum(1 for f in feedbacks if f.get("verbalComment"))
    nonverbal_count = sum(1 for f in feedbacks if f.get("eyeContactLevel"))
    total = len(feedbacks)

    parts = []
    if verbal_count:
        parts.append(f"언어 분석 {verbal_count}/{total}개 완료")
    if nonverbal_count:
        parts.append(f"비언어 분석 {nonverbal_count}/{total}개 완료")

    if not parts:
        return "분석 결과를 확인해주세요."

    comment = "전반적으로 " + ", ".join(parts) + "되었습니다."
    return comment


def _tone_label_to_level(tone_label: str | None) -> str:
    """verbal_analyzer의 tone_label을 3단계 라벨로 변환한다."""
    if tone_label in ("PROFESSIONAL", "CONFIDENT"):
        return "GOOD"
    if tone_label in ("CASUAL", "VERBOSE"):
        return "AVERAGE"
    if tone_label == "HESITANT":
        return "NEEDS_IMPROVEMENT"
    return "AVERAGE"



def _comment_block(src: dict | None) -> dict | None:
    if not src:
        return None

    def _norm(v):
        if v is None:
            return None
        s = str(v).strip()
        return s or None

    block = {
        "positive":   _norm(src.get("positive")),
        "negative":   _norm(src.get("negative")),
        "suggestion": _norm(src.get("suggestion")),
    }
    if all(v is None for v in block.values()):
        return None
    return block


def _legacy_string_to_block(text) -> dict | None:
    """레거시 verbal_analyzer ✓△→ string을 CommentBlock dict로 변환.

    줄 단위 prefix 파싱이 아니라 raw 전체를 positive에 싣는다.
    BE의 parseCommentBlock fallback과 동일한 패턴 (legacy raw → positive only).
    """
    if text is None:
        return None
    s = str(text).strip()
    if not s:
        return None
    return {"positive": s, "negative": None, "suggestion": None}


def _safe_stt(audio_path: str) -> dict | None:
    try:
        return transcribe_chunked(audio_path, WORK_DIR)
    except Exception as e:
        print(f"[Analysis] STT 실패 (비언어만 분석): {e}")
        return None


def _safe_vision(frame_paths: list[str]) -> dict | None:
    try:
        return analyze_frames(frame_paths)
    except Exception as e:
        print(f"[Analysis] Vision 분석 실패 (언어만 분석): {e}")
        return None


def _safe_verbal(
    question_text: str,
    transcript: str,
    position: str | None = None,
    tech_stack: str | None = None,
    level: str | None = None,
    model_answer: str | None = None,
    feedback_perspective: str | None = None,
) -> dict | None:
    try:
        return analyze_verbal(
            question_text, transcript,
            position=position, tech_stack=tech_stack,
            level=level, model_answer=model_answer,
            feedback_perspective=feedback_perspective,
        )
    except Exception as e:
        print(f"[Analysis] Verbal 분석 실패: {e}")
        return None


def _safe_update_progress(interview_id, question_set_id, status, **kwargs):
    try:
        update_progress(interview_id, question_set_id, status, **kwargs)
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


def _get_frame_time_ms(frame_path: str) -> int:
    """frame_0001.jpg → 0ms, frame_0002.jpg → 3000ms"""
    index = int(os.path.basename(frame_path).split("_")[1].split(".")[0])
    return (index - 1) * Config.FRAME_INTERVAL_SEC * 1000


def _filter_frames_for_range(frame_paths: list[str], start_ms: int, end_ms: int) -> list[str]:
    """주어진 시간 범위에 해당하는 프레임만 필터링"""
    return [p for p in frame_paths if start_ms <= _get_frame_time_ms(p) < end_ms]


def _cleanup():
    if os.path.exists(WORK_DIR):
        shutil.rmtree(WORK_DIR, ignore_errors=True)


def _sanitize_error_detail(error_msg: str) -> str:
    """에러 메시지에서 내부 URL, 파일 경로 등 민감 정보를 제거한다."""
    sanitized = re.sub(r'https?://[^\s]+', '[REDACTED_URL]', error_msg)
    sanitized = re.sub(r'/[^\s:]+\.py', '[REDACTED_PATH]', sanitized)
    return sanitized[:500]


def _classify_error(e: Exception) -> str:
    error_str = str(e).lower()
    if isinstance(e, TimeoutError) or 'timeout' in error_str:
        return "TIMEOUT"
    if 'openai' in error_str or '429' in error_str or '503' in error_str:
        return "API_ERROR"
    if 'gemini' in error_str or 'google' in error_str:
        return "API_ERROR"
    if 'ffmpeg' in error_str or 'whisper' in error_str:
        return "TRANSCRIPTION_ERROR"
    if 'vision' in error_str or 'frame' in error_str:
        return "VISION_ERROR"
    return "INTERNAL_ERROR"
