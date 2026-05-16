"""BE-02 RubricScorerResponseValidator 정책 단일 출처. drift 방지용 동일 룰.

- score ∈ {1, 2, 3}
- observation @NotBlank + 한국어 음절 [가-힣] 1+
- evidence_quote @NotBlank + whitespace 정규화 후 substring(userAnswer)
"""
from __future__ import annotations

import re
from dataclasses import dataclass
from enum import Enum
from typing import Optional


_KOREAN_SYLLABLE = re.compile(r"[가-힣]")
_WHITESPACE = re.compile(r"\s+")


class Violation(str, Enum):
    INVALID_SCORE = "InvalidScore"
    MISSING_OBSERVATION = "MissingObservation"
    NON_KOREAN_OBSERVATION = "NonKoreanObservation"
    MISSING_EVIDENCE = "MissingEvidence"
    EVIDENCE_NOT_IN_TRANSCRIPT = "EvidenceNotInTranscript"


@dataclass(frozen=True)
class ValidationResult:
    valid: bool
    violation: Optional[Violation] = None
    field: Optional[str] = None

    @classmethod
    def passed(cls) -> "ValidationResult":
        return cls(True, None, None)

    @classmethod
    def violated(cls, violation: Violation, field: str) -> "ValidationResult":
        return cls(False, violation, field)


def validate(
    dimension_key: str,
    score,
    observation,
    evidence_quote,
    user_answer,
    stage: str = "verbal",
) -> ValidationResult:
    if not isinstance(score, int) or isinstance(score, bool) or score < 1 or score > 3:
        return ValidationResult.violated(Violation.INVALID_SCORE, "score")

    if not isinstance(observation, str) or not observation.strip():
        return ValidationResult.violated(Violation.MISSING_OBSERVATION, "observation")

    if not _KOREAN_SYLLABLE.search(observation):
        return ValidationResult.violated(Violation.NON_KOREAN_OBSERVATION, "observation")

    if not isinstance(evidence_quote, str) or not evidence_quote.strip():
        return ValidationResult.violated(Violation.MISSING_EVIDENCE, "evidence_quote")

    # vision stage: evidence_quote = frame 자체 인용 (timestamp / frame 묘사). transcript substring 강제 X.
    if stage == "vision":
        return ValidationResult.passed()

    normalized_answer = _normalize(user_answer)
    normalized_quote = _normalize(evidence_quote)
    if not normalized_quote or normalized_quote not in normalized_answer:
        return ValidationResult.violated(Violation.EVIDENCE_NOT_IN_TRANSCRIPT, "evidence_quote")

    return ValidationResult.passed()


def _normalize(text) -> str:
    if not isinstance(text, str):
        return ""
    return _WHITESPACE.sub(" ", text).strip()
