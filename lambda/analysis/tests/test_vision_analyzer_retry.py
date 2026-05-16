"""BE-04: vision_analyzer dimension 검증 + retry 1회 + omit 회귀."""
import sys
import os
import types

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))


class _FakeChatCompletions:
    def __init__(self, responses):
        self._responses = list(responses)
        self.calls = []

    def create(self, **kwargs):
        self.calls.append(kwargs)
        raw = self._responses.pop(0)
        if isinstance(raw, Exception):
            raise raw
        message = types.SimpleNamespace(content=raw)
        choice = types.SimpleNamespace(message=message)
        return types.SimpleNamespace(choices=[choice])


class _FakeClient:
    def __init__(self, responses):
        self.chat = types.SimpleNamespace(completions=_FakeChatCompletions(responses))


def _import_module():
    import importlib
    import analyzers.vision_analyzer as mod
    importlib.reload(mod)
    return mod


def test_no_violation_returns_result_as_is():
    mod = _import_module()
    result = {
        "nonverbalDimensions": {
            "eye_contact_posture": {
                "score": 3,
                "observation": "어깨가 수평을 유지합니다.",
                "evidence_quote": "캐시 전략은",
            }
        }
    }
    client = _FakeClient([])
    enforced = mod._enforce_dimension_validity(client, content=[], result=result, transcript="캐시 전략은")
    assert enforced is result
    assert client.chat.completions.calls == []


def test_first_violation_retry_succeeds_returns_dimension():
    mod = _import_module()
    initial = {
        "nonverbalDimensions": {
            "eye_contact_posture": {
                "score": 3,
                "observation": "stable",
                "evidence_quote": "캐시 전략은",
            }
        }
    }
    retry_json = (
        '{"nonverbalDimensions":{"eye_contact_posture":{'
        '"score":3,"observation":"어깨가 수평을 유지하고 상체가 정면을 향합니다.",'
        '"evidence_quote":"캐시 전략은"}}}'
    )
    client = _FakeClient([retry_json])
    enforced = mod._enforce_dimension_validity(client, content=[], result=initial, transcript="캐시 전략은")
    ecp = enforced["nonverbalDimensions"]["eye_contact_posture"]
    assert ecp["score"] == 3
    assert "어깨" in ecp["observation"]


def test_retry_persistent_failure_omits_dimension():
    mod = _import_module()
    initial = {
        "nonverbalDimensions": {
            "eye_contact_posture": {
                "score": 3,
                "observation": "stable",
                "evidence_quote": "",
            }
        }
    }
    retry_json = (
        '{"nonverbalDimensions":{"eye_contact_posture":{'
        '"score":5,"observation":"english only","evidence_quote":""}}}'
    )
    client = _FakeClient([retry_json])
    enforced = mod._enforce_dimension_validity(client, content=[], result=initial, transcript="캐시")
    assert enforced == {"nonverbalDimensions": {}}


def test_retry_call_failure_omits_dimension():
    mod = _import_module()
    initial = {
        "nonverbalDimensions": {
            "eye_contact_posture": {
                "score": 3,
                "observation": "stable",
                "evidence_quote": "",
            }
        }
    }
    client = _FakeClient([RuntimeError("network blip")])
    enforced = mod._enforce_dimension_validity(client, content=[], result=initial, transcript="캐시")
    assert enforced == {"nonverbalDimensions": {}}


def test_missing_dimension_section_omits_immediately():
    mod = _import_module()
    initial = {"nonverbalDimensions": {}}
    client = _FakeClient([])
    enforced = mod._enforce_dimension_validity(client, content=[], result=initial, transcript="캐시")
    assert enforced == {"nonverbalDimensions": {}}
    assert client.chat.completions.calls == []
