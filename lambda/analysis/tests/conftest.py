"""Shared test stubs for Lambda modules with deployment-only SDK imports."""
import sys
import types


def pytest_configure():
    boto3 = types.ModuleType("boto3")
    boto3.client = lambda *_args, **_kwargs: object()
    sys.modules.setdefault("boto3", boto3)

    # 실 SDK 가 설치돼 있으면 stub 으로 덮지 않는다 — schema 호환성 검증 테스트가 실 SDK 를 필요로 함.
    try:
        import importlib
        importlib.import_module("google.genai")
    except Exception:
        google = types.ModuleType("google")
        genai_mod = types.ModuleType("google.genai")
        genai_types = types.ModuleType("google.genai.types")

        class _Client:
            def __init__(self, *args, **kwargs):
                self.args = args
                self.kwargs = kwargs

        class _GenerateContentConfig:
            _is_stub = True

            def __init__(self, *args, **kwargs):
                self.args = args
                self.kwargs = kwargs

        class _UploadFileConfig:
            _is_stub = True

            def __init__(self, *args, **kwargs):
                self.args = args
                self.kwargs = kwargs

        genai_mod.Client = _Client
        genai_mod.types = genai_types
        genai_mod._is_stub = True
        genai_types.GenerateContentConfig = _GenerateContentConfig
        genai_types.UploadFileConfig = _UploadFileConfig
        google.genai = genai_mod
        sys.modules.setdefault("google", google)
        sys.modules.setdefault("google.genai", genai_mod)
        sys.modules.setdefault("google.genai.types", genai_types)

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
