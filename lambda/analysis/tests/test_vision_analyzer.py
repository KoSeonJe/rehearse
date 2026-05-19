"""plan-484 Phase 2a: vision_analyzer dimension 채점 prompt + 응답 스키마 검증."""
import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

import pytest


@pytest.fixture(autouse=True)
def _stub_env(monkeypatch):
    monkeypatch.setenv("OPENAI_API_KEY", "test")
    monkeypatch.setenv("GEMINI_API_KEY", "test")


class TestFallbackSchema:
    def test_fallback_exposes_eye_contact_posture_dimension(self):
        from analyzers.vision_analyzer import _FALLBACK
        ecp = _FALLBACK["nonverbalDimensions"]["eye_contact_posture"]
        assert ecp["score"] in (1, 2, 3)
        assert isinstance(ecp["observation"], str) and ecp["observation"]
        # BE @NotBlank 통과용 placeholder — 빈 문자열 금지.
        assert ecp["evidence_quote"] == "프레임 분석 제한"

    def test_fallback_omits_raw_legacy_keys(self):
        from analyzers.vision_analyzer import _FALLBACK
        # 자유서술 + raw 5 키 모두 제거됐는지 확인.
        forbidden = {
            "positive", "negative", "suggestion",
            "eyeContactLevel", "postureLevel", "expressionLabel",
            "gazeOnCameraRatio", "postureUnstableCount",
        }
        assert forbidden.isdisjoint(_FALLBACK.keys())
        assert forbidden.isdisjoint(_FALLBACK["nonverbalDimensions"].keys())


class TestValidateResultDimensionSchema:
    def test_well_formed_result_passes_through(self):
        from analyzers.vision_analyzer import _validate_result
        result = _validate_result({
            "nonverbalDimensions": {
                "eye_contact_posture": {
                    "score": 3,
                    "observation": "어깨가 수평을 유지합니다.",
                    "evidence_quote": "캐시 전략은",
                }
            }
        })
        ecp = result["nonverbalDimensions"]["eye_contact_posture"]
        assert ecp["score"] == 3
        assert ecp["observation"] == "어깨가 수평을 유지합니다."
        assert ecp["evidence_quote"] == "캐시 전략은"

    def test_missing_dimensions_returns_fallback(self):
        from analyzers.vision_analyzer import _validate_result, _FALLBACK
        result = _validate_result({})
        assert result == _FALLBACK or result["nonverbalDimensions"]["eye_contact_posture"]["score"] == \
            _FALLBACK["nonverbalDimensions"]["eye_contact_posture"]["score"]

    def test_invalid_score_falls_back_to_default(self):
        from analyzers.vision_analyzer import _validate_result, _FALLBACK
        result = _validate_result({
            "nonverbalDimensions": {
                "eye_contact_posture": {
                    "score": 5,
                    "observation": "어깨 흔들림이 잦습니다.",
                    "evidence_quote": "",
                }
            }
        })
        fb_score = _FALLBACK["nonverbalDimensions"]["eye_contact_posture"]["score"]
        assert result["nonverbalDimensions"]["eye_contact_posture"]["score"] == fb_score

    def test_missing_observation_uses_fallback_observation(self):
        from analyzers.vision_analyzer import _validate_result, _FALLBACK
        result = _validate_result({
            "nonverbalDimensions": {
                "eye_contact_posture": {
                    "score": 2,
                    "observation": "",
                    "evidence_quote": "",
                }
            }
        })
        fb_obs = _FALLBACK["nonverbalDimensions"]["eye_contact_posture"]["observation"]
        assert result["nonverbalDimensions"]["eye_contact_posture"]["observation"] == fb_obs


class TestSystemPromptDimensionContent:
    def test_prompt_documents_eye_contact_posture_dimension(self):
        from analyzers.vision_analyzer import _SYSTEM_PROMPT
        assert "eye_contact_posture" in _SYSTEM_PROMPT

    def test_prompt_includes_rubric_keywords(self):
        from analyzers.vision_analyzer import _SYSTEM_PROMPT
        # rubric YAML 가이드 인용 — 시선·자세·표정 통합 채점 키워드.
        assert "시선" in _SYSTEM_PROMPT
        assert "자세" in _SYSTEM_PROMPT
        assert "표정" in _SYSTEM_PROMPT

    def test_prompt_requires_score_observation_evidence(self):
        from analyzers.vision_analyzer import _SYSTEM_PROMPT
        assert "score" in _SYSTEM_PROMPT
        assert "observation" in _SYSTEM_PROMPT
        assert "evidence_quote" in _SYSTEM_PROMPT

    def test_prompt_omits_legacy_raw_keys(self):
        from analyzers.vision_analyzer import _SYSTEM_PROMPT
        for forbidden in (
            "eyeContactLevel",
            "postureLevel",
            "expressionLabel",
            "gazeOnCameraRatio",
            "postureUnstableCount",
        ):
            assert forbidden not in _SYSTEM_PROMPT, f"raw 산출 지시 {forbidden} 잔존"

    def test_prompt_omits_legacy_free_text_keys(self):
        from analyzers.vision_analyzer import _SYSTEM_PROMPT
        # 자유서술 산출 지시 잔존 시 응답 스키마가 깨진다.
        # "positive 자유서술"·"negative 자유서술" 형태는 금지.
        # 단 'positive' 단어 자체는 다른 맥락 (긍정 신호 묘사) 으로 사용 가능하므로 키 형태만 검사.
        for forbidden in ('"positive"', '"negative"', '"suggestion"'):
            assert forbidden not in _SYSTEM_PROMPT, f"자유서술 키 {forbidden} 잔존"

    def test_prompt_specifies_frame_evidence_quote(self):
        """P0-1: vision evidence_quote 룰 = frame 자체 인용 (transcript substring 강제 해제)."""
        from analyzers.vision_analyzer import _SYSTEM_PROMPT
        # frame 인용 형식 키워드 포함
        assert "프레임 자체 인용" in _SYSTEM_PROMPT or "프레임" in _SYSTEM_PROMPT
        # transcript substring 강제 문구 부재
        assert "transcript 의 substring" not in _SYSTEM_PROMPT


class TestAnalyzeFramesSignature:
    def test_analyze_frames_omits_transcript_kwarg(self):
        """G3 정정 (사용자 결정 2026-05-17): vision evidence_quote = frame 자체 인용.
        transcript 인자 폐지."""
        from analyzers.vision_analyzer import analyze_frames
        import inspect
        sig = inspect.signature(analyze_frames)
        assert "transcript" not in sig.parameters

    def test_analyze_frames_returns_fallback_when_no_frames(self):
        from analyzers.vision_analyzer import analyze_frames, _FALLBACK
        result = analyze_frames([])
        ecp = result["nonverbalDimensions"]["eye_contact_posture"]
        fb = _FALLBACK["nonverbalDimensions"]["eye_contact_posture"]
        assert ecp["score"] == fb["score"]
        assert ecp["observation"] == fb["observation"]


class TestVisionStructuredOutputSchema:
    def test_response_format_uses_json_schema_strict(self):
        from analyzers.vision_analyzer import _VISION_RESPONSE_FORMAT
        assert _VISION_RESPONSE_FORMAT["type"] == "json_schema"
        js = _VISION_RESPONSE_FORMAT["json_schema"]
        assert js["strict"] is True
        assert js["name"] == "vision_nonverbal"

    def test_schema_enforces_score_enum_and_min_length(self):
        from analyzers.vision_analyzer import _VISION_SCHEMA
        ecp = _VISION_SCHEMA["properties"]["nonverbalDimensions"]["properties"]["eye_contact_posture"]
        props = ecp["properties"]
        assert props["score"]["enum"] == [1, 2, 3]
        assert props["observation"]["minLength"] == 1
        assert props["evidence_quote"]["minLength"] == 1
        assert ecp["additionalProperties"] is False
        assert set(ecp["required"]) == {"score", "observation", "evidence_quote"}
