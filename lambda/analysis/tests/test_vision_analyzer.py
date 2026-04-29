"""plan-11a: vision_analyzer gazeOnCameraRatio / postureUnstableCount 필드 검증."""
import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

import pytest


@pytest.fixture(autouse=True)
def _stub_env(monkeypatch):
    monkeypatch.setenv("OPENAI_API_KEY", "test")
    monkeypatch.setenv("GEMINI_API_KEY", "test")


class TestFallbackContainsNumericFields:
    def test_fallback_has_gaze_ratio_default(self):
        from analyzers.vision_analyzer import _FALLBACK
        assert _FALLBACK["gazeOnCameraRatio"] == 0.5

    def test_fallback_has_posture_unstable_count_default(self):
        from analyzers.vision_analyzer import _FALLBACK
        assert _FALLBACK["postureUnstableCount"] == 0


class TestValidateResultClampNumericFields:
    def test_normal_values_pass_through(self):
        from analyzers.vision_analyzer import _validate_result
        result = _validate_result({
            "eyeContactLevel": "GOOD",
            "postureLevel": "GOOD",
            "expressionLabel": "CONFIDENT",
            "gazeOnCameraRatio": 0.85,
            "postureUnstableCount": 3,
            "positive": "p", "negative": "n", "suggestion": "s",
        })
        assert result["gazeOnCameraRatio"] == 0.85
        assert result["postureUnstableCount"] == 3

    def test_missing_fields_use_default(self):
        from analyzers.vision_analyzer import _validate_result, _FALLBACK
        result = _validate_result({
            "eyeContactLevel": "GOOD",
            "postureLevel": "GOOD",
            "expressionLabel": "NEUTRAL",
            "positive": "p", "negative": "n", "suggestion": "s",
        })
        assert result["gazeOnCameraRatio"] == _FALLBACK["gazeOnCameraRatio"]
        assert result["postureUnstableCount"] == _FALLBACK["postureUnstableCount"]

    def test_out_of_range_float_clamped(self):
        from analyzers.vision_analyzer import _validate_result
        result = _validate_result({
            "eyeContactLevel": "GOOD",
            "postureLevel": "GOOD",
            "expressionLabel": "NEUTRAL",
            "gazeOnCameraRatio": 1.5,
            "postureUnstableCount": -2,
            "positive": "p", "negative": "n", "suggestion": "s",
        })
        assert result["gazeOnCameraRatio"] == 1.0
        assert result["postureUnstableCount"] == 0

    def test_negative_float_clamped(self):
        from analyzers.vision_analyzer import _validate_result
        result = _validate_result({
            "eyeContactLevel": "AVERAGE",
            "postureLevel": "AVERAGE",
            "expressionLabel": "NEUTRAL",
            "gazeOnCameraRatio": -0.3,
            "postureUnstableCount": 200,
            "positive": "p", "negative": "n", "suggestion": "s",
        })
        assert result["gazeOnCameraRatio"] == 0.0
        assert result["postureUnstableCount"] == 100

    def test_invalid_string_falls_back_to_default(self):
        from analyzers.vision_analyzer import _validate_result, _FALLBACK
        result = _validate_result({
            "eyeContactLevel": "AVERAGE",
            "postureLevel": "AVERAGE",
            "expressionLabel": "NEUTRAL",
            "gazeOnCameraRatio": "not_a_number",
            "postureUnstableCount": "abc",
            "positive": "p", "negative": "n", "suggestion": "s",
        })
        assert result["gazeOnCameraRatio"] == _FALLBACK["gazeOnCameraRatio"]
        assert result["postureUnstableCount"] == _FALLBACK["postureUnstableCount"]


class TestSystemPromptIncludesNumericFieldInstructions:
    def test_system_prompt_documents_gaze_ratio(self):
        from analyzers.vision_analyzer import _SYSTEM_PROMPT
        assert "gazeOnCameraRatio" in _SYSTEM_PROMPT

    def test_system_prompt_documents_posture_unstable_count(self):
        from analyzers.vision_analyzer import _SYSTEM_PROMPT
        assert "postureUnstableCount" in _SYSTEM_PROMPT
