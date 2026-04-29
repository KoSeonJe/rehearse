"""plan-11: D11~D14 결정론 매퍼.

LLM 호출 없이 verbal/vision 수치 필드를 D11~D14 raw 점수(1~3)로 매핑한다.
동일 입력 → 동일 출력 100% 재현성을 보장한다.
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

        d11 = self._score_d11(verbal)
        d12 = self._score_d12(verbal)
        d13 = self._score_d13(vision)
        d14 = self._score_d14(d11, d12, d13, prev, meta)

        return {"d11": d11, "d12": d12, "d13": d13, "d14": d14}

    def _score_d11(self, verbal: dict) -> int:
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

    def _score_d12(self, verbal: dict) -> int:
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

    def _score_d13(self, vision: dict) -> int:
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

    def _score_d14(
        self,
        d11: int,
        d12: int,
        d13: int,
        prev: dict | None,
        meta: dict,
    ) -> int | None:
        difficulty = (meta.get("difficulty") or "").lower()
        if difficulty not in ("medium", "hard"):
            return None
        if not prev:
            return 3

        p11 = prev.get("d11")
        p12 = prev.get("d12")
        p13 = prev.get("d13")
        drops = sum([
            d11 < p11 if p11 is not None else False,
            d12 < p12 if p12 is not None else False,
            d13 < p13 if p13 is not None else False,
        ])

        if drops >= 2:
            return 1
        if drops == 1:
            return 2
        return 3
