"""BE-02 RubricScorerResponseValidator 와 동일 fixture — 정책 drift 회귀."""
import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from analyzers.dimension_validator import validate, Violation


class TestKoreanObservation:
    def test_english_only_violates_non_korean(self):
        r = validate("technical_depth", 2, "Good answer with technical depth", "TPS 10000",
                     "TPS 10000 을 달성했습니다")
        assert r.valid is False
        assert r.violation == Violation.NON_KOREAN_OBSERVATION
        assert r.field == "observation"

    def test_english_with_korean_syllable_passes(self):
        r = validate("technical_depth", 2, "TPS 수치 언급으로 구체적 답변", "TPS 10000",
                     "TPS 10000 을 달성했습니다")
        assert r.valid is True

    def test_jamo_only_violates_non_korean(self):
        r = validate("technical_depth", 2, "ㄱㄴㄷ ㅏㅑㅓ", "TPS 10000",
                     "TPS 10000 을 달성했습니다")
        assert r.valid is False
        assert r.violation == Violation.NON_KOREAN_OBSERVATION


class TestEvidenceSubstring:
    def test_rubric_anchor_quote_violates_not_in_transcript(self):
        r = validate("experience_concreteness", 1, "구체성 부족", "팀에서 했어요 수준",
                     "TPS 10000 을 달성했습니다")
        assert r.valid is False
        assert r.violation == Violation.EVIDENCE_NOT_IN_TRANSCRIPT
        assert r.field == "evidence_quote"

    def test_substring_after_whitespace_normalization_passes(self):
        r = validate("technical_depth", 3, "구체적 수치 인용",
                     "TPS   10000  을\n달성",
                     "저는 작년에 TPS 10000 을 달성했습니다")
        assert r.valid is True


class TestScoreRange:
    def test_score_zero_violates_invalid(self):
        r = validate("technical_depth", 0, "구체적 답변", "TPS 10000",
                     "TPS 10000 을 달성했습니다")
        assert r.valid is False
        assert r.violation == Violation.INVALID_SCORE
        assert r.field == "score"

    def test_score_four_violates_invalid(self):
        r = validate("technical_depth", 4, "구체적 답변", "TPS 10000",
                     "TPS 10000 을 달성했습니다")
        assert r.valid is False
        assert r.violation == Violation.INVALID_SCORE

    def test_score_non_integer_violates(self):
        r = validate("technical_depth", "2", "구체적 답변", "TPS 10000",
                     "TPS 10000 을 달성했습니다")
        assert r.valid is False
        assert r.violation == Violation.INVALID_SCORE


class TestObservationBlank:
    def test_null_observation_violates_missing(self):
        r = validate("technical_depth", 2, None, "TPS 10000", "TPS 10000 을 달성했습니다")
        assert r.valid is False
        assert r.violation == Violation.MISSING_OBSERVATION
        assert r.field == "observation"

    def test_empty_observation_violates_missing(self):
        r = validate("technical_depth", 2, "   ", "TPS 10000", "TPS 10000 을 달성했습니다")
        assert r.valid is False
        assert r.violation == Violation.MISSING_OBSERVATION


class TestEvidenceMissing:
    def test_null_evidence_violates_missing(self):
        r = validate("technical_depth", 2, "구체적 답변", None, "TPS 10000 을 달성했습니다")
        assert r.valid is False
        assert r.violation == Violation.MISSING_EVIDENCE
        assert r.field == "evidence_quote"

    def test_blank_evidence_violates_missing(self):
        r = validate("technical_depth", 2, "구체적 답변", "   ", "TPS 10000 을 달성했습니다")
        assert r.valid is False
        assert r.violation == Violation.MISSING_EVIDENCE
