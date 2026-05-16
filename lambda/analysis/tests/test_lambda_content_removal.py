"""plan-13: Lambda output is delivery-only for timestamp feedback."""
import os
import sys
import types

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))


def _install_sdk_stubs():
    boto3 = types.ModuleType("boto3")
    boto3.client = lambda *_args, **_kwargs: object()
    sys.modules.setdefault("boto3", boto3)

    google = types.ModuleType("google")
    generativeai = types.ModuleType("google.generativeai")
    generativeai.configure = lambda *_args, **_kwargs: None

    class _GenerationConfig:
        def __init__(self, *args, **kwargs):
            self.args = args
            self.kwargs = kwargs

    class _GenerativeModel:
        def __init__(self, *args, **kwargs):
            self.args = args
            self.kwargs = kwargs

    generativeai.GenerationConfig = _GenerationConfig
    generativeai.GenerativeModel = _GenerativeModel
    google.generativeai = generativeai
    sys.modules.setdefault("google", google)
    sys.modules.setdefault("google.generativeai", generativeai)

    openai = types.ModuleType("openai")
    openai.OpenAI = object
    openai.RateLimitError = RuntimeError
    openai.APIError = RuntimeError
    openai.AuthenticationError = RuntimeError
    sys.modules.setdefault("openai", openai)

    api_client = types.ModuleType("api_client")
    api_client.get_answers = lambda *_args, **_kwargs: {}
    api_client.update_progress = lambda *_args, **_kwargs: None
    api_client.save_feedback = lambda *_args, **_kwargs: None
    api_client.backup_to_s3 = lambda *_args, **_kwargs: None
    api_client.set_correlation_id = lambda *_args, **_kwargs: None
    sys.modules.setdefault("api_client", api_client)


CONTENT_KEYS = {
    "verbalComment",
    "accuracyIssues",
    "coachingStructure",
    "coachingImprovement",
}


def test_gemini_prompt_requests_dimension_schema():
    """plan-484 Phase 2a: gemini prompt = dimension 채점 (자유서술 / raw 삭제)."""
    _install_sdk_stubs()
    import analyzers.gemini_analyzer as gemini_analyzer

    system_template = gemini_analyzer._ANSWER_SYSTEM_TEMPLATE
    user_template = gemini_analyzer._ANSWER_USER_TEMPLATE

    # 신규 dimension 스키마 유지
    assert "nonverbalDimensions" in user_template
    assert "fluency" in user_template
    assert "confidence_tone" in user_template
    assert "fillerWords" in user_template
    assert "fillerWordCount" in user_template

    # plan-13 비스코프 (verbal/technical/accuracy) 잔존 부재
    assert '"overall"' not in user_template
    assert '"verbal"' not in user_template
    assert '"technical"' not in user_template
    assert "accuracyIssues" not in user_template
    assert "### 3. verbal" not in system_template
    assert "### 4. technical" not in system_template

    # plan-484 Phase 2a 자유서술 + raw 삭제 강제
    for forbidden in (
        "attitude",
        "overall_delivery",
        "speedVariance",
        "speechPace",
        "toneConfidenceLevel",
        "emotionLabel",
    ):
        assert forbidden not in system_template, f"system 에 legacy 키 {forbidden} 잔존"
        assert forbidden not in user_template, f"user 에 legacy 키 {forbidden} 잔존"


def test_legacy_verbal_prompt_factory_contract_is_default_stacks_only():
    _install_sdk_stubs()
    import analyzers.verbal_prompt_factory as verbal_prompt_factory

    assert verbal_prompt_factory.DEFAULT_TECH_STACKS == {
        "BACKEND": "JAVA_SPRING",
        "FRONTEND": "REACT_TS",
        "DEVOPS": "AWS_K8S",
        "DATA_ENGINEER": "SPARK_AIRFLOW",
        "FULLSTACK": "REACT_SPRING",
    }
    assert not hasattr(verbal_prompt_factory, "SYSTEM_TEMPLATE")
    assert not hasattr(verbal_prompt_factory, "build_system_prompt")
    assert not hasattr(verbal_prompt_factory, "build_user_prompt")


def test_handler_has_no_legacy_openai_verbal_pipeline():
    _install_sdk_stubs()
    import handler

    assert not hasattr(handler, "analyze_verbal")
    assert not hasattr(handler, "_safe_verbal")
    assert not hasattr(handler, "_legacy_string_to_block")
    assert not hasattr(handler, "_tone_label_to_level")


def test_gemini_pipeline_omits_lambda_content_fields(monkeypatch):
    _install_sdk_stubs()
    import handler

    monkeypatch.setattr(
        handler,
        "_safe_gemini_audio",
        lambda *args, **kwargs: {
            "transcript": "음 캐시를 먼저 확인합니다",
            "vocal": {
                "fillerWords": ["음"],
                "speechPace": "적절",
                "toneConfidenceLevel": "GOOD",
                "emotionLabel": "자신감",
                "speedVariance": 0.1,
                "positive": "단정형 어미가 유지됩니다.",
                "negative": "",
                "suggestion": "",
            },
            "attitude": {
                "positive": "경어가 유지됩니다.",
                "negative": "",
                "suggestion": "",
            },
            "overall_delivery": {
                "positive": "말끝이 분명합니다.",
                "negative": "",
                "suggestion": "",
            },
        },
    )
    monkeypatch.setattr(
        handler,
        "_safe_vision",
        lambda *_args, **_kwargs: {
            "eyeContactLevel": "GOOD",
            "postureLevel": "AVERAGE",
            "expressionLabel": "CONFIDENT",
            "gazeOnCameraRatio": 0.8,
            "postureUnstableCount": 1,
            "positive": "시선이 유지됩니다.",
            "negative": "",
            "suggestion": "",
        },
    )
    monkeypatch.setattr(handler, "_filter_frames_for_range", lambda frame_paths, *_args: frame_paths)

    feedbacks, verbal_ok, nonverbal_ok = handler._run_gemini_pipeline(
        answers=[
            {
                "questionId": 1,
                "startMs": 0,
                "endMs": 1000,
                "questionText": "캐시 전략을 설명해 주세요.",
                "difficulty": "medium",
            }
        ],
        audio_paths=["/tmp/a.wav"],
        frame_paths=["/tmp/f.jpg"],
        video_duration_ms=1000,
        position="BACKEND",
        tech_stack="JAVA_SPRING",
        level="MID",
    )

    assert verbal_ok is True
    assert nonverbal_ok is True
    feedback = feedbacks[0]
    assert CONTENT_KEYS.isdisjoint(feedback.keys())
    assert feedback["overallComment"]["positive"] == "말끝이 분명합니다."
    assert "nonverbalScore" in feedback


def test_legacy_pipeline_omits_lambda_content_fields(monkeypatch):
    _install_sdk_stubs()
    import handler

    monkeypatch.setattr(
        handler,
        "_safe_stt",
        lambda *_args, **_kwargs: {
            "full_text": "캐시를 먼저 확인합니다",
            "segments": [{"start_ms": 0, "end_ms": 1000, "text": "캐시를 먼저 확인합니다"}],
        },
    )
    monkeypatch.setattr(
        handler,
        "_safe_vision",
        lambda *_args, **_kwargs: {
            "eyeContactLevel": "GOOD",
            "postureLevel": "AVERAGE",
            "expressionLabel": "CONFIDENT",
            "gazeOnCameraRatio": 0.8,
            "postureUnstableCount": 1,
            "positive": "시선이 유지됩니다.",
            "negative": "",
            "suggestion": "",
        },
    )
    monkeypatch.setattr(handler, "_filter_frames_for_range", lambda frame_paths, *_args: frame_paths)
    monkeypatch.setattr(handler, "update_progress", lambda *_args, **_kwargs: None)

    feedbacks = handler._run_legacy_pipeline(
        answers=[
            {
                "questionId": 1,
                "startMs": 0,
                "endMs": 1000,
                "questionText": "캐시 전략을 설명해 주세요.",
                "difficulty": "medium",
            }
        ],
        audio_path="/tmp/a.wav",
        frame_paths=["/tmp/f.jpg"],
        video_duration_ms=1000,
        interview_id=1,
        question_set_id=1,
        skip_analyzing_update=True,
    )

    feedback = feedbacks[0]
    assert CONTENT_KEYS.isdisjoint(feedback.keys())
    assert feedback["overallComment"] is None
    assert feedback["transcript"] == "캐시를 먼저 확인합니다"
    assert "vocalComment" not in feedback
    assert "fillerWordCount" not in feedback
    assert feedback["attitudeComment"] is None
    assert "nonverbalScore" in feedback
