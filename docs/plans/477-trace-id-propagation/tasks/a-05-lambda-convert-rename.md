# Task A5 — Lambda convert 헤더명 / 변수명 rename

> **PR**: PR-A
> **영역**: Lambda
> **선행 Task**: 없음 (A4 와 병렬 가능. 동일 PR 머지 강제)

---

## 변경 파일

- `lambda/convert/api_client.py` — **변경**. A4 와 동일 패턴 (`_correlation_id` → `_trace_id`, 송신 헤더 `X-Correlation-Id` → `X-Trace-Id`).
- `lambda/convert/handler.py` — **변경**. 변수명 rename 만 (자체 생성 제거는 B5).
- `lambda/convert/tests/` — 기존 테스트 있으면 rename. 부재 시 본 task 범위 X (B5 에서 신규 작성).

> **참고**: tech-spec.md §Evidence 회귀 grep 인벤토리 — convert 2 파일.

---

## 핵심 로직

A4 와 패턴 동일. convert Lambda 도 BE 콜백 송신 시 `X-Trace-Id` 헤더 단일화.

```python
# lambda/convert/api_client.py
class ConvertApiClient:
    def __init__(self, base_url: str, api_key: str):
        self._base_url = base_url
        self._api_key = api_key
        self._trace_id: str | None = None

    def set_trace_id(self, trace_id: str) -> None:
        self._trace_id = trace_id

    def _headers(self) -> dict:
        headers = {"X-Internal-Api-Key": self._api_key, "Content-Type": "application/json"}
        if self._trace_id:
            headers["X-Trace-Id"] = self._trace_id
        return headers
```

---

## 테스트

**카테고리**: Lambda 단위 (pytest). 기존 테스트 없으면 본 task 범위 X — B5 에서 head_object / copy_object 와 함께 신규 작성.

---

## 의존

- 선행: 없음
- 외부: pytest

---

## Verification Hook

- 명령: `cd lambda/convert && pytest` (기존 테스트 존재 시)
- 회귀 grep: `grep -rn "correlation_id\|X-Correlation-Id\|set_correlation_id" lambda/convert/` 결과 0건

---

## 커밋 메시지 (예상)

```
refactor(lambda): convert 헤더명 X-Trace-Id 로 단일화
```
