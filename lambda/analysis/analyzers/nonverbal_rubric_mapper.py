"""plan-11: 비언어 결정론 매퍼.

LLM 호출 없이 verbal/vision 수치 필드를 fluency/confidence_tone/eye_contact_posture/composure
raw 점수(1~3)로 매핑한다. 동일 입력 → 동일 출력 100% 재현성을 보장한다.
"""
from __future__ import annotations

_TONE_HIGH = ("CONFIDENT", "PROFESSIONAL")


class NonverbalRubricMapper:
    def score_turn(
        self,
        verbal: dict | None,
        vision: dict | None,
        prev: dict | None,
        meta: dict | None,
    ) -> dict:
        verbal = verbal or {}
        vision = vision or {}
        meta = meta or {}

        fluency = self._score_fluency(verbal)
        confidence_tone = self._score_confidence_tone(verbal)
        eye_contact_posture = self._score_eye_contact_posture(vision)
        composure = self._score_composure(fluency, confidence_tone, eye_contact_posture, prev, meta)

        return {
            "fluency": fluency,
            "confidence_tone": confidence_tone,
            "eye_contact_posture": eye_contact_posture,
            "composure": composure,
        }

    def _score_fluency(self, verbal: dict) -> int:
        v = verbal.get("filler_word_count")
        if v is None:
            n = 0
        else:
            try:
                n = int(v)
            except (TypeError, ValueError):
                n = 0
        if n >= 6:
            return 1
        if n >= 3:
            return 2
        return 3

    def _score_confidence_tone(self, verbal: dict) -> int:
        tone = verbal.get("tone_label") or "PROFESSIONAL"
        v = verbal.get("speedVariance")
        if v is None:
            variance = 0.0
        else:
            try:
                variance = float(v)
            except (TypeError, ValueError):
                variance = 0.0

        if tone == "HESITANT" or variance >= 0.3:
            return 1
        if tone in _TONE_HIGH and variance < 0.15:
            return 3
        return 2

    def _score_eye_contact_posture(self, vision: dict) -> int:
        v = vision.get("gazeOnCameraRatio")
        if v is None:
            gaze = 0.5
        else:
            try:
                gaze = float(v)
            except (TypeError, ValueError):
                gaze = 0.5

        v = vision.get("postureUnstableCount")
        if v is None:
            posture = 0
        else:
            try:
                posture = int(v)
            except (TypeError, ValueError):
                posture = 0

        if gaze < 0.3 or posture > 5:
            return 1
        if gaze > 0.7 and posture <= 2:
            return 3
        return 2

    def _score_composure(
        self,
        fluency: int,
        confidence_tone: int,
        eye_contact_posture: int,
        prev: dict | None,
        meta: dict,
    ) -> int | None:
        difficulty = (meta.get("difficulty") or "").lower()
        if difficulty not in ("medium", "hard"):
            return None
        if not prev:
            return 3

        prev_fluency = prev.get("fluency")
        prev_confidence_tone = prev.get("confidence_tone")
        prev_eye_contact_posture = prev.get("eye_contact_posture")
        drops = sum([
            fluency < prev_fluency if prev_fluency is not None else False,
            confidence_tone < prev_confidence_tone if prev_confidence_tone is not None else False,
            eye_contact_posture < prev_eye_contact_posture if prev_eye_contact_posture is not None else False,
        ])

        if drops >= 2:
            return 1
        if drops == 1:
            return 2
        return 3
