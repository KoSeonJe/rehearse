"""handler 의 에러 분류기 회귀 검증."""
import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

import pytest


@pytest.fixture(autouse=True)
def _stub_env(monkeypatch):
    monkeypatch.setenv("OPENAI_API_KEY", "test")
    monkeypatch.setenv("GEMINI_API_KEY", "test")


class TestClassifyErrorSchemaMissing:
    def test_schema_missing_fields_classified(self):
        from handler import _classify_error
        e = RuntimeError("SCHEMA_MISSING_FIELDS: speed_variance not present")
        assert _classify_error(e) == "SCHEMA_MISSING_FIELDS"

    def test_existing_classifications_unchanged(self):
        from handler import _classify_error
        assert _classify_error(TimeoutError("timeout")) == "TIMEOUT"
        assert _classify_error(RuntimeError("openai 429 too many requests")) == "API_ERROR"
        assert _classify_error(RuntimeError("gemini quota")) == "API_ERROR"
        assert _classify_error(RuntimeError("ffmpeg invalid")) == "TRANSCRIPTION_ERROR"
        assert _classify_error(RuntimeError("vision frame missing")) == "VISION_ERROR"
        assert _classify_error(RuntimeError("unrelated")) == "INTERNAL_ERROR"
