"""548 PR1: 비언어 분석 출력을 main 평면 코멘트 형식으로 복원한 회귀 검증.

- gemini_analyzer: transcript/verbal/technical/vocal/attitude/overall 평면 섹션 반환 (차원점수 無).
- handler._run_gemini_pipeline: SaveFeedbackRequest.TimestampFeedbackItem 평면 필드로 조립.
- handler._comment_block / _legacy_string_to_block: CommentBlock dict 변환.
"""
import importlib
import os
import sys
import types

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

import pytest


@pytest.fixture(autouse=True)
def _stub_env(monkeypatch):
    monkeypatch.setenv("OPENAI_API_KEY", "test")
    monkeypatch.setenv("GEMINI_API_KEY", "test")


class _FakeModels:
    def __init__(self, response_text):
        self._response_text = response_text
        self.calls = []

    def generate_content(self, *, model, contents, config):
        self.calls.append({"model": model, "contents": contents, "config": config})
        return types.SimpleNamespace(text=self._response_text)


class _FakeFiles:
    def upload(self, *, file, config):
        return types.SimpleNamespace(name="files/fake-audio")

    def delete(self, *, name):
        return None


class _FakeGeminiClient:
    def __init__(self, response_text):
        self.models = _FakeModels(response_text)
        self.files = _FakeFiles()


_GEMINI_FLAT_RESPONSE = (
    '{"transcript":"캐시 전략은 read-through 패턴을 적용했습니다.",'
    '"vocal":{"fillerWords":["음","어"],"speechPace":"적절",'
    '"toneConfidenceLevel":"GOOD","emotionLabel":"자신감",'
    '"positive":"단정형 어미가 일관됩니다.","negative":"필러가 일부 있습니다.","suggestion":"호흡을 두세요."},'
    '"verbal":{"positive":"핵심 용어를 정확히 호명했습니다.","negative":"예시가 부족합니다.","suggestion":"수치를 더하세요."},'
    '"technical":{"accuracyIssues":[{"claim":"redis","correction":"in-memory"}],'
    '"coaching":{"structure":"개념→원리","improvement":"보충 개념"}},'
    '"attitude":{"positive":"존댓말이 일관됩니다.","negative":"회피 표현이 있습니다.","suggestion":"되물어보세요."},'
    '"overall":{"positive":"전달이 명확합니다.","negative":"긴장이 보입니다.","suggestion":"연습하세요."}}'
)


def test_gemini_returns_flat_sections(monkeypatch):
    import analyzers.gemini_analyzer as mod
    importlib.reload(mod)

    monkeypatch.setattr(mod, "_get_client", lambda: _FakeGeminiClient(_GEMINI_FLAT_RESPONSE))

    result = mod.analyze_answer_audio("/tmp/a.mp3", "캐시 전략을 설명하세요.", position="BACKEND")

    assert set(result.keys()) == {"transcript", "vocal", "verbal", "technical", "attitude", "overall"}
    assert "nonverbalDimensions" not in result
    assert result["vocal"]["toneConfidenceLevel"] == "GOOD"
    assert result["vocal"]["speechPace"] == "적절"
    assert result["vocal"]["emotionLabel"] == "자신감"
    assert result["vocal"]["fillerWords"] == ["음", "어"]
    assert result["technical"]["coaching"]["structure"] == "개념→원리"


def test_gemini_fallback_is_flat():
    import analyzers.gemini_analyzer as mod
    importlib.reload(mod)

    fallback = mod._FALLBACK_ANSWER
    assert "nonverbalDimensions" not in fallback
    assert set(fallback.keys()) == {"transcript", "verbal", "technical", "vocal", "attitude", "overall"}


def test_handler_gemini_pipeline_assembles_flat_fields(monkeypatch):
    import handler
    importlib.reload(handler)

    gemini = {
        "transcript": "캐시 전략을 설명했습니다.",
        "vocal": {
            "fillerWords": ["음", "어"],
            "speechPace": "적절",
            "toneConfidenceLevel": "GOOD",
            "emotionLabel": "자신감",
            "positive": "단정형 어미.", "negative": "필러 존재.", "suggestion": "호흡.",
        },
        "verbal": {"positive": "용어 정확.", "negative": "예시 부족.", "suggestion": "수치 추가."},
        "technical": {
            "accuracyIssues": [{"claim": "redis", "correction": "in-memory"}],
            "coaching": {"structure": "개념→원리", "improvement": "보충"},
        },
        "attitude": {"positive": "존댓말.", "negative": "회피.", "suggestion": "되묻기."},
        "overall": {"positive": "명확.", "negative": "긴장.", "suggestion": "연습."},
    }
    vision = {
        "eyeContactLevel": "GOOD",
        "postureLevel": "AVERAGE",
        "expressionLabel": "CONFIDENT",
        "positive": "어깨 수평.", "negative": "표정 정적.", "suggestion": "끄덕임.",
    }

    monkeypatch.setattr(handler, "_safe_gemini_audio", lambda *a, **k: gemini)
    monkeypatch.setattr(handler, "_safe_vision", lambda *a, **k: vision)
    monkeypatch.setattr(handler, "_filter_frames_for_range", lambda *a, **k: ["f.jpg"])

    answers = [{"questionId": 10, "startMs": 0, "endMs": 1000, "questionText": "Q"}]
    feedbacks, verbal_ok, nonverbal_ok = handler._run_gemini_pipeline(
        answers, ["/tmp/a.mp3"], ["f.jpg"], 1000,
        position="BACKEND", tech_stack="JAVA_SPRING", level="JUNIOR",
    )

    assert verbal_ok is True
    assert nonverbal_ok is True
    fb = feedbacks[0]

    # 차원점수 형식 부재
    assert "nonverbalScore" not in fb

    # content (verbal/technical) 평면 필드
    assert fb["verbalComment"] == {"positive": "용어 정확.", "negative": "예시 부족.", "suggestion": "수치 추가."}
    assert fb["accuracyIssues"] == '[{"claim": "redis", "correction": "in-memory"}]'
    assert fb["coachingStructure"] == "개념→원리"
    assert fb["coachingImprovement"] == "보충"

    # delivery (nonverbal/vocal) 평면 필드
    assert fb["eyeContactLevel"] == "GOOD"
    assert fb["postureLevel"] == "AVERAGE"
    assert fb["expressionLabel"] == "CONFIDENT"
    assert fb["nonverbalComment"]["positive"] == "어깨 수평."
    assert fb["fillerWords"] == ["음", "어"]
    assert fb["fillerWordCount"] == 2
    assert fb["speechPace"] == "적절"
    assert fb["toneConfidenceLevel"] == "GOOD"
    assert fb["emotionLabel"] == "자신감"
    assert fb["vocalComment"]["positive"] == "단정형 어미."

    # attitude / overall
    assert fb["attitudeComment"]["positive"] == "존댓말."
    assert fb["overallComment"]["positive"] == "명확."


def test_comment_block_norm_and_empty():
    import handler
    importlib.reload(handler)

    block = handler._comment_block({"positive": "  ok  ", "negative": "", "suggestion": None})
    assert block == {"positive": "ok", "negative": None, "suggestion": None}
    assert handler._comment_block(None) is None
    assert handler._comment_block({"positive": "", "negative": None, "suggestion": ""}) is None


def test_legacy_string_to_block():
    import handler
    importlib.reload(handler)

    assert handler._legacy_string_to_block("✓ 좋음") == {
        "positive": "✓ 좋음", "negative": None, "suggestion": None
    }
    assert handler._legacy_string_to_block(None) is None
    assert handler._legacy_string_to_block("   ") is None
