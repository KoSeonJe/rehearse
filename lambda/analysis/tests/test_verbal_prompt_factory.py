"""plan-11a: verbal_prompt_factory speed_variance 필드 확장 검증."""
import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from analyzers.verbal_prompt_factory import (
    SYSTEM_TEMPLATE,
    build_system_prompt,
)


class TestSpeedVarianceSchema:
    def test_system_template_response_schema_includes_speed_variance(self):
        assert '"speed_variance"' in SYSTEM_TEMPLATE

    def test_system_template_includes_speed_variance_instruction(self):
        assert "speed_variance:" in SYSTEM_TEMPLATE
        assert "발화 속도의 분산" in SYSTEM_TEMPLATE

    def test_existing_fields_unchanged(self):
        for key in ('"filler_word_count"', '"tone_label"', '"comment"', '"attitude_comment"'):
            assert key in SYSTEM_TEMPLATE

    def test_build_system_prompt_emits_speed_variance(self):
        prompt = build_system_prompt("BACKEND", "JAVA_SPRING")
        assert '"speed_variance"' in prompt
        assert "0.0" in prompt or "0~1" in prompt or "0.0~1.0" in prompt
