# Task A4 — Lambda analysis 헤더명 / 변수명 rename

> **PR**: PR-A
> **영역**: Lambda
> **선행 Task**: 없음 (BE 와 병렬 가능. 동일 PR 머지 강제)

---

## 변경 파일

- `lambda/analysis/api_client.py` — **변경**. `_correlation_id` → `_trace_id`. `set_correlation_id()` → `set_trace_id()`. 송신 헤더 `X-Correlation-Id` → `X-Trace-Id`.
- `lambda/analysis/handler.py` — **변경**. 변수명 `correlation_id` → `trace_id`. 자체 생성 표현 명칭 변경만 (자체 생성 로직 자체 제거는 B4 에서 처리).
- `lambda/analysis/tests/conftest.py` — **변경**. fixture 이름 / 변수명 rename.
- `lambda/analysis/tests/test_lambda_content_removal.py` — **변경**. `set_correlation_id` 호출 → `set_trace_id`.

> **참고**: tech-spec.md §Evidence 회귀 grep 인벤토리 — analysis 4 파일.

---

## 핵심 로직

```python
# api_client.py (변경부)
class AnalysisApiClient:
    def __init__(self, base_url: str, api_key: str):
        self._base_url = base_url
        self._api_key = api_key
        self._trace_id: str | None = None

    def set_trace_id(self, trace_id: str) -> None:
        self._trace_id = trace_id

    def _headers(self) -> dict:
        headers = {
            "X-Internal-Api-Key": self._api_key,
            "Content-Type": "application/json",
        }
        if self._trace_id:
            headers["X-Trace-Id"] = self._trace_id
        return headers
```

```python
# handler.py (변경부 - 이번 task 는 rename 만)
trace_id = f"{interview_id}-{question_set_id}-{uuid4().hex[:8]}"  # B4 에서 제거 예정
api_client.set_trace_id(trace_id)
logger.info("analysis 시작 trace_id=%s interview_id=%s", trace_id, interview_id)
```

> handler.py 의 **자체 생성 로직 제거**는 B4 에서 처리. A4 는 변수명 / 헤더명 rename 만.

---

## 테스트

**카테고리**: Lambda 단위 (pytest)

- `tests/test_api_client.py` (신규 또는 기존 확장):
  - `set_trace_id` 호출 후 `_headers()` 결과에 `X-Trace-Id` 포함 검증
  - `set_trace_id` 미호출 시 `X-Trace-Id` 키 부재 검증
- `tests/test_lambda_content_removal.py`:
  - 기존 케이스 변수명 / 메서드명 rename 반영

---

## 의존

- 선행: 없음
- 외부: pytest, requests / httpx (기존)

---

## Verification Hook

- 명령: `cd lambda/analysis && pytest tests/test_api_client.py tests/test_lambda_content_removal.py`
- 통과 기준: 모든 케이스 green
- 회귀 grep: `grep -rn "correlation_id\|X-Correlation-Id\|set_correlation_id" lambda/analysis/` 결과 0건

---

## 커밋 메시지 (예상)

```
refactor(lambda): analysis 헤더명 X-Trace-Id 로 단일화
```
