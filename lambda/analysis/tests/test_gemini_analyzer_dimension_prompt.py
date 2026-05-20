"""plan-484 Phase 2a: gemini_analyzer dimension 채점 prompt + fallback 스키마 검증."""
import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

import pytest


@pytest.fixture(autouse=True)
def _stub_env(monkeypatch):
    monkeypatch.setenv("OPENAI_API_KEY", "test")
    monkeypatch.setenv("GEMINI_API_KEY", "test")


class TestSystemPromptDimensionContent:
    def test_prompt_documents_fluency_dimension(self):
        from analyzers.gemini_analyzer import _ANSWER_SYSTEM_TEMPLATE
        assert "fluency" in _ANSWER_SYSTEM_TEMPLATE

    def test_prompt_documents_confidence_tone_dimension(self):
        from analyzers.gemini_analyzer import _ANSWER_SYSTEM_TEMPLATE
        assert "confidence_tone" in _ANSWER_SYSTEM_TEMPLATE

    def test_prompt_requires_score_observation_evidence(self):
        from analyzers.gemini_analyzer import _ANSWER_SYSTEM_TEMPLATE
        assert "score" in _ANSWER_SYSTEM_TEMPLATE
        assert "observation" in _ANSWER_SYSTEM_TEMPLATE
        assert "evidence_quote" in _ANSWER_SYSTEM_TEMPLATE

    def test_prompt_includes_rubric_keywords(self):
        """rubric YAML 가이드 인용 — 필러 / 더듬 / 속도 키워드 포함."""
        from analyzers.gemini_analyzer import _ANSWER_SYSTEM_TEMPLATE
        assert "필러" in _ANSWER_SYSTEM_TEMPLATE
        assert "더듬" in _ANSWER_SYSTEM_TEMPLATE
        assert "속도" in _ANSWER_SYSTEM_TEMPLATE

    def test_prompt_omits_legacy_raw_keys(self):
        from analyzers.gemini_analyzer import _ANSWER_SYSTEM_TEMPLATE
        for forbidden in (
            "speechPace",
            "toneConfidenceLevel",
            "emotionLabel",
            "speedVariance",
        ):
            assert forbidden not in _ANSWER_SYSTEM_TEMPLATE, f"raw 산출 지시 {forbidden} 잔존"

    def test_prompt_omits_legacy_free_text_section_titles(self):
        from analyzers.gemini_analyzer import _ANSWER_SYSTEM_TEMPLATE
        # 자유서술 3섹션 본문 제거 확인.
        for forbidden_section in (
            "### 3. attitude",
            "### 4. overall_delivery",
            "overall_delivery",
            "attitude",
        ):
            assert forbidden_section not in _ANSWER_SYSTEM_TEMPLATE, (
                f"자유서술 섹션 {forbidden_section} 잔존"
            )

    def test_prompt_preserves_filler_word_responsibility(self):
        """필러 예외 — fillerWords / fillerWordCount 산출 책임 유지."""
        from analyzers.gemini_analyzer import _ANSWER_SYSTEM_TEMPLATE
        assert "fillerWords" in _ANSWER_SYSTEM_TEMPLATE
        assert "fillerWordCount" in _ANSWER_SYSTEM_TEMPLATE

    def test_prompt_preserves_user_answer_security_marker(self):
        """OWASP A03 — 사용자 발화 영역 마커 보존."""
        from analyzers.gemini_analyzer import _ANSWER_SYSTEM_TEMPLATE, _ANSWER_USER_TEMPLATE
        full = _ANSWER_SYSTEM_TEMPLATE + _ANSWER_USER_TEMPLATE
        assert "<<<USER_ANSWER>>>" in full
        assert "<<<END_USER_ANSWER>>>" in full


class TestUserTemplateSchema:
    def test_user_template_documents_new_dimension_schema(self):
        from analyzers.gemini_analyzer import _ANSWER_USER_TEMPLATE
        assert "nonverbalDimensions" in _ANSWER_USER_TEMPLATE
        assert "fluency" in _ANSWER_USER_TEMPLATE
        assert "confidence_tone" in _ANSWER_USER_TEMPLATE

    def test_user_template_omits_legacy_keys(self):
        from analyzers.gemini_analyzer import _ANSWER_USER_TEMPLATE
        for forbidden in (
            "speechPace",
            "toneConfidenceLevel",
            "emotionLabel",
            "speedVariance",
            "attitude",
            "overall_delivery",
        ):
            assert forbidden not in _ANSWER_USER_TEMPLATE, (
                f"legacy 키 {forbidden} user template 에 잔존"
            )


class TestFallbackSchema:
    def test_fallback_exposes_two_nonverbal_dimensions(self):
        from analyzers.gemini_analyzer import _FALLBACK_ANSWER
        dims = _FALLBACK_ANSWER["nonverbalDimensions"]
        assert set(dims.keys()) == {"fluency", "confidence_tone"}
        for name, body in dims.items():
            assert body["score"] in (1, 2, 3), f"{name}.score invalid"
            assert isinstance(body["observation"], str) and body["observation"], (
                f"{name}.observation empty"
            )
            # BE @NotBlank 통과용 placeholder — 빈 문자열 금지.
            assert body["evidence_quote"] == "오디오 분석 제한"

    def test_fallback_keeps_filler_fields(self):
        from analyzers.gemini_analyzer import _FALLBACK_ANSWER
        assert _FALLBACK_ANSWER["vocal"]["fillerWords"] == []
        assert _FALLBACK_ANSWER["vocal"]["fillerWordCount"] == 0

    def test_fallback_omits_legacy_free_text_and_raw_keys(self):
        from analyzers.gemini_analyzer import _FALLBACK_ANSWER
        # vocal 내부에 raw 측정치 / 자유서술 키 부재.
        vocal_keys = set(_FALLBACK_ANSWER["vocal"].keys())
        forbidden_vocal = {
            "speechPace", "toneConfidenceLevel", "emotionLabel", "speedVariance",
            "positive", "negative", "suggestion",
        }
        assert forbidden_vocal.isdisjoint(vocal_keys), (
            f"vocal 에 legacy 키 잔존: {forbidden_vocal & vocal_keys}"
        )
        # 최상위 자유서술 섹션 부재.
        for forbidden in ("attitude", "overall_delivery"):
            assert forbidden not in _FALLBACK_ANSWER, (
                f"최상위 자유서술 섹션 {forbidden} 잔존"
            )


class TestGeminiStructuredOutputSchema:
    def test_schema_requires_top_level_fields(self):
        from analyzers.gemini_analyzer import _GEMINI_ANSWER_SCHEMA
        assert set(_GEMINI_ANSWER_SCHEMA["required"]) == {
            "transcript", "vocal", "nonverbalDimensions",
        }

    def test_schema_dimensions_require_core_fields(self):
        from analyzers.gemini_analyzer import _GEMINI_ANSWER_SCHEMA
        dims = _GEMINI_ANSWER_SCHEMA["properties"]["nonverbalDimensions"]["properties"]
        for key in ("fluency", "confidence_tone"):
            props = dims[key]["properties"]
            assert props["score"]["type"] == "integer"
            assert set(dims[key]["required"]) == {
                "score", "observation", "evidence_quote",
            }

    def test_schema_vocal_requires_filler_fields(self):
        from analyzers.gemini_analyzer import _GEMINI_ANSWER_SCHEMA
        vocal = _GEMINI_ANSWER_SCHEMA["properties"]["vocal"]
        assert set(vocal["required"]) == {"fillerWords", "fillerWordCount"}

    def test_schema_dimension_objects_are_independent(self):
        # _DIMENSION_SCHEMA 공유 시 mutate 사이드이펙트 위험 — 별도 인스턴스 보장.
        from analyzers.gemini_analyzer import _GEMINI_ANSWER_SCHEMA
        dims = _GEMINI_ANSWER_SCHEMA["properties"]["nonverbalDimensions"]["properties"]
        assert dims["fluency"] is not dims["confidence_tone"]


class TestGeminiSchemaSdkCompatibility:
    def test_schema_serializes_via_real_sdk(self):
        # google.genai 가 schema dict 를 GenerateContentConfig 로 예외 없이 받아들이는지 실 SDK 로 확인.
        import pytest
        types = pytest.importorskip("google.genai.types")
        if getattr(types.GenerateContentConfig, "_is_stub", False):
            pytest.skip("google.genai 실 SDK 부재 — 호환성 검증 skip")
        from analyzers.gemini_analyzer import _GEMINI_ANSWER_SCHEMA
        types.GenerateContentConfig(
            temperature=0.3,
            response_mime_type="application/json",
            response_json_schema=_GEMINI_ANSWER_SCHEMA,
        )
