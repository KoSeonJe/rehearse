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
    enforced = mod._enforce_dimension_validity(client, content=[], result=result)
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
    enforced = mod._enforce_dimension_validity(client, content=[], result=initial)
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
    enforced = mod._enforce_dimension_validity(client, content=[], result=initial)
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
    enforced = mod._enforce_dimension_validity(client, content=[], result=initial)
    assert enforced == {"nonverbalDimensions": {}}


def test_missing_dimension_section_omits_immediately():
    mod = _import_module()
    initial = {"nonverbalDimensions": {}}
    client = _FakeClient([])
    enforced = mod._enforce_dimension_validity(client, content=[], result=initial)
    assert enforced == {"nonverbalDimensions": {}}
    assert client.chat.completions.calls == []


def test_analyze_frames_skips_enforce_retry_when_fallback(monkeypatch, tmp_path):
    """P1-2: _validate_result fallback 분기 진입 시 _enforce_dimension_validity 호출 0회 (retry LLM 비용 방지)."""
    mod = _import_module()

    # LLM 응답이 nonverbalDimensions 섹션을 누락한 경우 → _validate_result 가 fallback 으로 대체.
    malformed_json = '{"wrong":"shape"}'
    fake_client = _FakeClient([malformed_json])
    monkeypatch.setattr(mod, "OpenAI", lambda **kwargs: fake_client)

    enforce_calls = []
    original_enforce = mod._enforce_dimension_validity

    def _spy_enforce(*args, **kwargs):
        enforce_calls.append((args, kwargs))
        return original_enforce(*args, **kwargs)

    monkeypatch.setattr(mod, "_enforce_dimension_validity", _spy_enforce)

    # 프레임 1장만 있는 시나리오 — 인코딩만 통과하면 됨.
    frame_path = tmp_path / "frame.jpg"
    frame_path.write_bytes(b"\xff\xd8\xff")  # JPEG magic. 인코딩만 통과.

    result = mod.analyze_frames([str(frame_path)])

    # LLM 1회만 호출 (retry skip).
    assert len(fake_client.chat.completions.calls) == 1
    # enforce 도 호출되지 않음.
    assert enforce_calls == []
    # 결과는 fallback.
    fb = mod._FALLBACK["nonverbalDimensions"]["eye_contact_posture"]
    assert result["nonverbalDimensions"]["eye_contact_posture"]["score"] == fb["score"]
