"""plan-11a: handler 신규 수치 필드 파싱 + default fallback + 에러 분류 검증."""
import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

import pytest


@pytest.fixture(autouse=True)
def _stub_env(monkeypatch):
    monkeypatch.setenv("OPENAI_API_KEY", "test")
    monkeypatch.setenv("GEMINI_API_KEY", "test")


class TestCoerceClampedFloat:
    def test_normal_value(self):
        from handler import _coerce_clamped_float
        assert _coerce_clamped_float(0.7, 0.0, 1.0, default=0.5) == 0.7

    def test_none_returns_default(self):
        from handler import _coerce_clamped_float
        assert _coerce_clamped_float(None, 0.0, 1.0, default=0.5) == 0.5

    def test_string_returns_default(self):
        from handler import _coerce_clamped_float
        assert _coerce_clamped_float("nan_text", 0.0, 1.0, default=0.5) == 0.5

    def test_above_max_clamped(self):
        from handler import _coerce_clamped_float
        assert _coerce_clamped_float(1.5, 0.0, 1.0, default=0.5) == 1.0

    def test_below_min_clamped(self):
        from handler import _coerce_clamped_float
        assert _coerce_clamped_float(-0.3, 0.0, 1.0, default=0.5) == 0.0

    def test_nan_returns_default(self):
        from handler import _coerce_clamped_float
        assert _coerce_clamped_float(float("nan"), 0.0, 1.0, default=0.5) == 0.5

    def test_int_input_coerced(self):
        from handler import _coerce_clamped_float
        assert _coerce_clamped_float(1, 0.0, 1.0, default=0.5) == 1.0

    def test_bool_returns_default(self):
        from handler import _coerce_clamped_float
        assert _coerce_clamped_float(True, 0.0, 1.0, default=0.5) == 0.5
        assert _coerce_clamped_float(False, 0.0, 1.0, default=0.5) == 0.5


class TestCoerceClampedInt:
    def test_normal_value(self):
        from handler import _coerce_clamped_int
        assert _coerce_clamped_int(5, 0, 100, default=0) == 5

    def test_none_returns_default(self):
        from handler import _coerce_clamped_int
        assert _coerce_clamped_int(None, 0, 100, default=0) == 0

    def test_above_max_clamped(self):
        from handler import _coerce_clamped_int
        assert _coerce_clamped_int(500, 0, 100, default=0) == 100

    def test_below_min_clamped(self):
        from handler import _coerce_clamped_int
        assert _coerce_clamped_int(-2, 0, 100, default=0) == 0

    def test_string_returns_default(self):
        from handler import _coerce_clamped_int
        assert _coerce_clamped_int("abc", 0, 100, default=0) == 0

    def test_float_string_coerced(self):
        from handler import _coerce_clamped_int
        assert _coerce_clamped_int("3.7", 0, 100, default=0) == 3

    def test_bool_returns_default(self):
        from handler import _coerce_clamped_int
        assert _coerce_clamped_int(True, 0, 100, default=0) == 0
        assert _coerce_clamped_int(False, 0, 100, default=0) == 0


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
