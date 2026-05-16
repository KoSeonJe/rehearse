"""BE-04: gemini_analyzer dimension 검증 + retry 1회 + omit 회귀."""
import sys
import os
import types

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))


class _FakeModel:
    """generate_content 가 미리 정해둔 응답 리스트를 순서대로 반환하는 fake."""

    def __init__(self, responses):
        self._responses = list(responses)
        self.calls = []

    def generate_content(self, payload):
        self.calls.append(payload)
        text = self._responses.pop(0)
        return types.SimpleNamespace(text=text)


def _import_module():
    import importlib
    import analyzers.gemini_analyzer as mod
    importlib.reload(mod)
    return mod


def test_no_violation_returns_result_as_is():
    mod = _import_module()
    result = {
        "transcript": "캐시 전략은 read-through 패턴을 적용했습니다.",
        "nonverbalDimensions": {
            "fluency": {
                "score": 3,
                "observation": "필러 없이 단정형 어미로 마무리됩니다.",
                "evidence_quote": "read-through 패턴을 적용했습니다",
            },
            "confidence_tone": {
                "score": 3,
                "observation": "단정형 어미가 일관됩니다.",
                "evidence_quote": "적용했습니다",
            },
        },
    }
    fake = _FakeModel([])
    enforced = mod._enforce_dimension_validity(fake, audio_file=None, user_prompt="P", result=result)
    assert enforced is result
    assert fake.calls == []


def test_first_violation_retry_succeeds_replaces_dimension():
    mod = _import_module()
    initial = {
        "transcript": "캐시 전략은 read-through 패턴을 적용했습니다.",
        "nonverbalDimensions": {
            "fluency": {
                "score": 3,
                "observation": "필러 없이 단정형 어미로 마무리됩니다.",
                "evidence_quote": "read-through 패턴을 적용했습니다",
            },
            "confidence_tone": {
                "score": 3,
                "observation": "Good tone",
                "evidence_quote": "적용했습니다",
            },
        },
    }
    retry_json = (
        '{"transcript":"캐시 전략은 read-through 패턴을 적용했습니다.",'
        '"nonverbalDimensions":{'
        '"confidence_tone":{"score":3,'
        '"observation":"단정형 어미가 일관되고 끝맺음이 명확합니다.",'
        '"evidence_quote":"적용했습니다"}}}'
    )
    fake = _FakeModel([retry_json])
    enforced = mod._enforce_dimension_validity(fake, audio_file=None, user_prompt="P", result=initial)
    assert enforced["nonverbalDimensions"]["confidence_tone"]["score"] == 3
    assert "끝맺음" in enforced["nonverbalDimensions"]["confidence_tone"]["observation"]
    assert enforced["nonverbalDimensions"]["fluency"]["score"] == 3
    assert len(fake.calls) == 1


def test_retry_persistent_failure_omits_dimension():
    mod = _import_module()
    initial = {
        "transcript": "캐시 전략은 read-through 패턴을 적용했습니다.",
        "nonverbalDimensions": {
            "fluency": {
                "score": 3,
                "observation": "필러 없이 단정형 어미로 마무리됩니다.",
                "evidence_quote": "read-through 패턴을 적용했습니다",
            },
            "confidence_tone": {
                "score": 5,
                "observation": "good",
                "evidence_quote": "",
            },
        },
    }
    retry_json = (
        '{"transcript":"캐시 전략은 read-through 패턴을 적용했습니다.",'
        '"nonverbalDimensions":{'
        '"confidence_tone":{"score":7,"observation":"still bad","evidence_quote":""}}}'
    )
    fake = _FakeModel([retry_json])
    enforced = mod._enforce_dimension_validity(fake, audio_file=None, user_prompt="P", result=initial)
    assert "fluency" in enforced["nonverbalDimensions"]
    assert "confidence_tone" not in enforced["nonverbalDimensions"]


def test_retry_call_failure_omits_dimension():
    mod = _import_module()
    initial = {
        "transcript": "캐시 전략은 read-through 패턴을 적용했습니다.",
        "nonverbalDimensions": {
            "fluency": {
                "score": 3,
                "observation": "필러 없이 단정형 어미로 마무리됩니다.",
                "evidence_quote": "read-through 패턴을 적용했습니다",
            },
            "confidence_tone": {
                "score": 3,
                "observation": "bad",
                "evidence_quote": "",
            },
        },
    }

    class _ExplodingModel:
        def __init__(self):
            self.calls = 0

        def generate_content(self, _):
            self.calls += 1
            raise RuntimeError("network blip")

    bad = _ExplodingModel()
    enforced = mod._enforce_dimension_validity(bad, audio_file=None, user_prompt="P", result=initial)
    assert "confidence_tone" not in enforced["nonverbalDimensions"]
    assert bad.calls == 1


def test_missing_dimensions_section_triggers_retry_then_omit_all():
    mod = _import_module()
    initial = {"transcript": "캐시 전략은 read-through 패턴을 적용했습니다."}
    fake = _FakeModel(['{"transcript":"","nonverbalDimensions":{}}'])
    enforced = mod._enforce_dimension_validity(fake, audio_file=None, user_prompt="P", result=initial)
    assert enforced["nonverbalDimensions"] == {}
