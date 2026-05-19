"""Shared test stubs for Lambda modules with deployment-only SDK imports."""
import sys
import types


def pytest_configure():
    boto3 = types.ModuleType("boto3")
    boto3.client = lambda *_args, **_kwargs: object()
    sys.modules.setdefault("boto3", boto3)

    # 실 SDK 가 설치돼 있으면 stub 으로 덮지 않는다 — protos.Schema 호환성 검증 테스트가 실 SDK 를 필요로 함.
    try:
        import importlib
        importlib.import_module("google.generativeai")
    except Exception:
        google = types.ModuleType("google")
        generativeai = types.ModuleType("google.generativeai")
        generativeai.configure = lambda *_args, **_kwargs: None

        class _GenerationConfig:
            def __init__(self, *args, **kwargs):
                self.args = args
                self.kwargs = kwargs

        class _GenerativeModel:
            def __init__(self, *args, **kwargs):
                self.args = args
                self.kwargs = kwargs

        generativeai.GenerationConfig = _GenerationConfig
        generativeai.GenerativeModel = _GenerativeModel
        google.generativeai = generativeai
        sys.modules.setdefault("google", google)
        sys.modules.setdefault("google.generativeai", generativeai)

    openai = types.ModuleType("openai")
    openai.OpenAI = object
    openai.RateLimitError = RuntimeError
    openai.APIError = RuntimeError
    openai.AuthenticationError = RuntimeError
    sys.modules.setdefault("openai", openai)

    api_client = types.ModuleType("api_client")
    api_client.get_answers = lambda *_args, **_kwargs: {}
    api_client.update_progress = lambda *_args, **_kwargs: None
    api_client.save_feedback = lambda *_args, **_kwargs: None
    api_client.backup_to_s3 = lambda *_args, **_kwargs: None
    api_client.set_correlation_id = lambda *_args, **_kwargs: None
    sys.modules.setdefault("api_client", api_client)
