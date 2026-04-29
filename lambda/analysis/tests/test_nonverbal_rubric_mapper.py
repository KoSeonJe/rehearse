"""plan-11: D11~D14 결정론 매퍼 검증."""
import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

import pytest

from analyzers.nonverbal_rubric_mapper import NonverbalRubricMapper


@pytest.fixture
def mapper():
    return NonverbalRubricMapper()


class TestDeterminism:
    @pytest.mark.parametrize("verbal,vision,prev,meta", [
        (
            {"filler_word_count": 4, "tone_label": "PROFESSIONAL", "speedVariance": 0.1},
            {"gazeOnCameraRatio": 0.8, "postureUnstableCount": 1},
            {"d11": 2, "d12": 2, "d13": 2},
            {"difficulty": "medium"},
        ),
        (
            {"filler_word_count": 0, "tone_label": "CONFIDENT", "speedVariance": 0.05},
            {"gazeOnCameraRatio": 0.9, "postureUnstableCount": 0},
            None,
            {"difficulty": "hard"},
        ),
        (
            {"filler_word_count": 10, "tone_label": "HESITANT", "speedVariance": 0.5},
            {"gazeOnCameraRatio": 0.1, "postureUnstableCount": 8},
            {"d11": 3, "d12": 3, "d13": 3},
            {"difficulty": "easy"},
        ),
    ])
    def test_same_input_yields_same_output(self, mapper, verbal, vision, prev, meta):
        first = mapper.score_turn(verbal, vision, prev, meta)
        second = mapper.score_turn(verbal, vision, prev, meta)
        assert first == second


class TestD11FillerBoundaries:
    @pytest.mark.parametrize("filler_count,expected", [
        (2, 3),
        (3, 2),
        (5, 2),
        (6, 1),
    ])
    def test_d11_filler_word_thresholds(self, mapper, filler_count, expected):
        result = mapper.score_turn(
            verbal={"filler_word_count": filler_count, "tone_label": "PROFESSIONAL"},
            vision={"gazeOnCameraRatio": 0.5, "postureUnstableCount": 0},
            prev=None,
            meta={"difficulty": "easy"},
        )
        assert result["d11"] == expected


class TestD12ToneAndSpeedVariance:
    @pytest.mark.parametrize("tone,variance,expected", [
        ("HESITANT", 0.5, 1),
        ("CONFIDENT", 0.1, 3),
        ("PROFESSIONAL", 0.2, 2),
    ])
    def test_d12_combinations(self, mapper, tone, variance, expected):
        result = mapper.score_turn(
            verbal={"filler_word_count": 0, "tone_label": tone, "speedVariance": variance},
            vision={"gazeOnCameraRatio": 0.5, "postureUnstableCount": 0},
            prev=None,
            meta={"difficulty": "easy"},
        )
        assert result["d12"] == expected


class TestD13GazeAndPosture:
    @pytest.mark.parametrize("gaze,posture,expected", [
        (0.2, 3, 1),
        (0.8, 1, 3),
        (0.5, 3, 2),
    ])
    def test_d13_combinations(self, mapper, gaze, posture, expected):
        result = mapper.score_turn(
            verbal={"filler_word_count": 0, "tone_label": "PROFESSIONAL"},
            vision={"gazeOnCameraRatio": gaze, "postureUnstableCount": posture},
            prev=None,
            meta={"difficulty": "easy"},
        )
        assert result["d13"] == expected


class TestD14CrossDimensional:
    def test_two_drops_yields_one(self, mapper):
        result = mapper.score_turn(
            verbal={"filler_word_count": 6, "tone_label": "HESITANT", "speedVariance": 0.5},
            vision={"gazeOnCameraRatio": 0.5, "postureUnstableCount": 0},
            prev={"d11": 3, "d12": 3, "d13": 3},
            meta={"difficulty": "medium"},
        )
        assert result["d11"] == 1
        assert result["d12"] == 1
        assert result["d14"] == 1

    def test_one_drop_yields_two(self, mapper):
        result = mapper.score_turn(
            verbal={"filler_word_count": 6, "tone_label": "PROFESSIONAL", "speedVariance": 0.2},
            vision={"gazeOnCameraRatio": 0.5, "postureUnstableCount": 0},
            prev={"d11": 2, "d12": 2, "d13": 2},
            meta={"difficulty": "medium"},
        )
        assert result["d11"] == 1
        assert result["d12"] == 2
        assert result["d13"] == 2
        assert result["d14"] == 2

    def test_no_drops_yields_three(self, mapper):
        result = mapper.score_turn(
            verbal={"filler_word_count": 4, "tone_label": "PROFESSIONAL", "speedVariance": 0.2},
            vision={"gazeOnCameraRatio": 0.5, "postureUnstableCount": 0},
            prev={"d11": 2, "d12": 2, "d13": 2},
            meta={"difficulty": "medium"},
        )
        assert result["d14"] == 3


class TestD14Disabled:
    def test_easy_difficulty_returns_none(self, mapper):
        result = mapper.score_turn(
            verbal={"filler_word_count": 0, "tone_label": "PROFESSIONAL"},
            vision={"gazeOnCameraRatio": 0.5, "postureUnstableCount": 0},
            prev={"d11": 3, "d12": 3, "d13": 3},
            meta={"difficulty": "easy"},
        )
        assert result["d14"] is None

    def test_missing_difficulty_returns_none(self, mapper):
        result = mapper.score_turn(
            verbal={"filler_word_count": 0, "tone_label": "PROFESSIONAL"},
            vision={"gazeOnCameraRatio": 0.5, "postureUnstableCount": 0},
            prev=None,
            meta={},
        )
        assert result["d14"] is None


class TestNoneSafe:
    def test_empty_inputs_use_fallback(self, mapper):
        result = mapper.score_turn({}, {}, None, {})
        assert result["d11"] == 3
        assert result["d12"] == 3
        assert result["d13"] == 2
        assert result["d14"] is None

    def test_none_inputs_do_not_raise(self, mapper):
        result = mapper.score_turn(None, None, None, None)
        assert set(result.keys()) == {"d11", "d12", "d13", "d14"}

    def test_partial_verbal_uses_defaults(self, mapper):
        result = mapper.score_turn(
            verbal={"filler_word_count": 4},
            vision={},
            prev=None,
            meta={"difficulty": "hard"},
        )
        assert result["d11"] == 2
        assert result["d12"] == 3
        assert result["d13"] == 2

    def test_zero_gaze_scores_d13_one(self, mapper):
        # gaze=0.0 은 0.5 default 로 둔갑하면 안 됨 → D13=1 이어야 함
        result = mapper.score_turn(
            verbal={"filler_word_count": 0, "tone_label": "PROFESSIONAL"},
            vision={"gazeOnCameraRatio": 0.0, "postureUnstableCount": 0},
            prev=None,
            meta={"difficulty": "easy"},
        )
        assert result["d13"] == 1
