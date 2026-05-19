"""plan-13 + plan-484 Phase 2a: Lambda timestamp feedback 영역 키 페이로드 회귀."""
import os
import sys
import types

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))


def _install_sdk_stubs():
    boto3 = types.ModuleType("boto3")
    boto3.client = lambda *_args, **_kwargs: object()
    sys.modules.setdefault("boto3", boto3)

    google = types.ModuleType("google")
    genai_mod = types.ModuleType("google.genai")
    genai_types = types.ModuleType("google.genai.types")

    class _Client:
        def __init__(self, *args, **kwargs):
            self.args = args
            self.kwargs = kwargs

    class _GenerateContentConfig:
        _is_stub = True

        def __init__(self, *args, **kwargs):
            self.args = args
            self.kwargs = kwargs

    class _UploadFileConfig:
        _is_stub = True

        def __init__(self, *args, **kwargs):
            self.args = args
            self.kwargs = kwargs

    genai_mod.Client = _Client
    genai_mod.types = genai_types
    genai_mod._is_stub = True
    genai_types.GenerateContentConfig = _GenerateContentConfig
    genai_types.UploadFileConfig = _UploadFileConfig
    google.genai = genai_mod
    sys.modules.setdefault("google", google)
    sys.modules.setdefault("google.genai", genai_mod)
    sys.modules.setdefault("google.genai.types", genai_types)

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

FREE_TEXT_KEYS = {
    "nonverbalComment",
    "vocalComment",
    "attitudeComment",
    "overallComment",
}

RAW_KEYS = {
    "speechPace",
    "toneConfidenceLevel",
    "emotionLabel",
    "speedVariance",
    "eyeContactLevel",
    "postureLevel",
    "expressionLabel",
    "gazeOnCameraRatio",
    "postureUnstableCount",
}


def _gemini_response_with_dimensions():
    return {
        "transcript": "음 캐시를 먼저 확인합니다",
        "vocal": {
            "fillerWords": ["음"],
            "fillerWordCount": 1,
        },
        "nonverbalDimensions": {
            "fluency": {
                "score": 2,
                "observation": "도입부 필러로 잠시 끊깁니다.",
                "evidence_quote": "음 캐시를",
            },
            "confidence_tone": {
                "score": 3,
                "observation": "'~합니다' 단정형 어미로 마무리됩니다.",
                "evidence_quote": "확인합니다",
            },
        },
    }


def _vision_response_with_dimension():
    return {
        "nonverbalDimensions": {
            "eye_contact_posture": {
                "score": 3,
                "observation": "어깨가 수평을 유지하고 시선이 카메라 정면에 머뭅니다.",
                "evidence_quote": "캐시를 먼저",
            }
        }
    }


def test_gemini_prompt_requests_dimension_schema():
    """plan-484 Phase 2a: gemini prompt = dimension 채점 (자유서술 / raw 삭제)."""
    _install_sdk_stubs()
    import analyzers.gemini_analyzer as gemini_analyzer

    system_template = gemini_analyzer._ANSWER_SYSTEM_TEMPLATE
    user_template = gemini_analyzer._ANSWER_USER_TEMPLATE

    assert "nonverbalDimensions" in user_template
    assert "fluency" in user_template
    assert "confidence_tone" in user_template
    assert "fillerWords" in user_template
    assert "fillerWordCount" in user_template

    assert '"overall"' not in user_template
    assert '"verbal"' not in user_template
    assert '"technical"' not in user_template
    assert "accuracyIssues" not in user_template
    assert "### 3. verbal" not in system_template
    assert "### 4. technical" not in system_template

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


def test_handler_drops_nonverbal_rubric_mapper():
    _install_sdk_stubs()
    import handler

    assert not hasattr(handler, "_RUBRIC_MAPPER")
    assert "NonverbalRubricMapper" not in dir(handler)


def test_gemini_pipeline_assembles_area_split_score(monkeypatch):
    _install_sdk_stubs()
    import handler

    monkeypatch.setattr(handler, "_safe_gemini_audio", lambda *args, **kwargs: _gemini_response_with_dimensions())
    monkeypatch.setattr(handler, "_safe_vision", lambda *_args, **_kwargs: _vision_response_with_dimension())
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
    fb = feedbacks[0]

    assert CONTENT_KEYS.isdisjoint(fb.keys())
    assert FREE_TEXT_KEYS.isdisjoint(fb.keys())
    assert RAW_KEYS.isdisjoint(fb.keys())

    assert fb["transcript"] == "음 캐시를 먼저 확인합니다"
    assert fb["fillerWords"] == ["음"]
    assert fb["fillerWordCount"] == 1

    vocal_dims = fb["nonverbalScore"]["vocal"]["dimensions"]
    assert [d["dimension_ref"] for d in vocal_dims] == ["fluency", "confidence_tone"]
    assert vocal_dims[0]["score"] == 2
    assert vocal_dims[1]["evidence_quote"] == "확인합니다"

    vision_dims = fb["nonverbalScore"]["vision"]["dimensions"]
    assert [d["dimension_ref"] for d in vision_dims] == ["eye_contact_posture"]
    assert vision_dims[0]["score"] == 3


def test_gemini_pipeline_partial_failure_vocal_only(monkeypatch):
    _install_sdk_stubs()
    import handler

    monkeypatch.setattr(handler, "_safe_gemini_audio", lambda *args, **kwargs: _gemini_response_with_dimensions())
    monkeypatch.setattr(handler, "_safe_vision", lambda *_args, **_kwargs: None)
    monkeypatch.setattr(handler, "_filter_frames_for_range", lambda frame_paths, *_args: frame_paths)

    feedbacks, verbal_ok, nonverbal_ok = handler._run_gemini_pipeline(
        answers=[{"questionId": 1, "startMs": 0, "endMs": 1000, "questionText": "Q"}],
        audio_paths=["/tmp/a.wav"],
        frame_paths=["/tmp/f.jpg"],
        video_duration_ms=1000,
    )

    assert verbal_ok is True
    assert nonverbal_ok is False
    score = feedbacks[0]["nonverbalScore"]
    assert "vocal" in score and "vision" not in score


def test_gemini_pipeline_partial_failure_vision_only(monkeypatch):
    """답변 단위 Gemini 실패 + Vision 정상 — 영역 키 분리 정확성."""
    _install_sdk_stubs()
    import handler

    # 답변 2개 중 첫 번째만 Gemini 실패. 전체 실패 분기 (RuntimeError) 회피.
    gemini_outputs = [None, _gemini_response_with_dimensions()]
    call_count = {"n": 0}

    def gemini_side_effect(*_args, **_kwargs):
        idx = call_count["n"]
        call_count["n"] += 1
        return gemini_outputs[idx]

    monkeypatch.setattr(handler, "_safe_gemini_audio", gemini_side_effect)
    monkeypatch.setattr(handler, "_safe_vision", lambda *_args, **_kwargs: _vision_response_with_dimension())
    monkeypatch.setattr(handler, "_filter_frames_for_range", lambda frame_paths, *_args: frame_paths)

    feedbacks, verbal_ok, nonverbal_ok = handler._run_gemini_pipeline(
        answers=[
            {"questionId": 1, "startMs": 0, "endMs": 500, "questionText": "Q1"},
            {"questionId": 2, "startMs": 500, "endMs": 1000, "questionText": "Q2"},
        ],
        audio_paths=["/tmp/a1.wav", "/tmp/a2.wav"],
        frame_paths=["/tmp/f.jpg"],
        video_duration_ms=1000,
    )

    assert verbal_ok is True
    assert nonverbal_ok is True

    # 첫 답변: gemini=None → vision 만 잔존
    score_a = feedbacks[0]["nonverbalScore"]
    assert score_a is not None and "vision" in score_a and "vocal" not in score_a

    # 두 번째 답변: 둘 다 정상 → 양쪽 잔존
    score_b = feedbacks[1]["nonverbalScore"]
    assert "vocal" in score_b and "vision" in score_b


def test_gemini_pipeline_no_dimensions_yields_null_score(monkeypatch):
    """답변 단위 vocal/vision 모두 부재 → nonverbalScore=None."""
    _install_sdk_stubs()
    import handler

    # 답변 2개. 두 번째는 정상 (전체 None 분기 회피), 첫 번째는 양쪽 부재.
    gemini_outputs = [None, _gemini_response_with_dimensions()]
    vision_outputs = [None, _vision_response_with_dimension()]
    gemini_idx = {"n": 0}
    vision_idx = {"n": 0}

    def gemini_side_effect(*_args, **_kwargs):
        i = gemini_idx["n"]
        gemini_idx["n"] += 1
        return gemini_outputs[i]

    def vision_side_effect(*_args, **_kwargs):
        i = vision_idx["n"]
        vision_idx["n"] += 1
        return vision_outputs[i]

    monkeypatch.setattr(handler, "_safe_gemini_audio", gemini_side_effect)
    monkeypatch.setattr(handler, "_safe_vision", vision_side_effect)
    monkeypatch.setattr(handler, "_filter_frames_for_range", lambda frame_paths, *_args: frame_paths)

    feedbacks, _, _ = handler._run_gemini_pipeline(
        answers=[
            {"questionId": 1, "startMs": 0, "endMs": 500, "questionText": "Q1"},
            {"questionId": 2, "startMs": 500, "endMs": 1000, "questionText": "Q2"},
        ],
        audio_paths=["/tmp/a1.wav", "/tmp/a2.wav"],
        frame_paths=["/tmp/f.jpg"],
        video_duration_ms=1000,
    )

    assert feedbacks[0]["nonverbalScore"] is None
    assert feedbacks[1]["nonverbalScore"] is not None


def test_legacy_pipeline_yields_null_score(monkeypatch):
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

    fb = feedbacks[0]
    assert CONTENT_KEYS.isdisjoint(fb.keys())
    assert FREE_TEXT_KEYS.isdisjoint(fb.keys())
    assert RAW_KEYS.isdisjoint(fb.keys())
    assert fb["transcript"] == "캐시를 먼저 확인합니다"
    assert fb["nonverbalScore"] is None
