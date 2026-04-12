# Plan 03: Lambda convert/analysis parser (신규 스키마 전용) + `backup_to_s3` 경로 수정

> 상태: Draft
> 작성일: 2026-04-12
> 선행: Plan 01 (SSOT 규격 확정)
> 병렬 가능: Plan 02 (백엔드)와 독립적으로 진행 가능
> **정책**: Lambda는 **신규 스키마만 처리**한다. 레거시 키는 **코드 경로 자체를 두지 않는다**. 전환은 Plan 04의 드레인·삭제·원샷 컷오버 방식으로 수행.

## Why

Lambda 함수 2개(`rehearse-convert-*`, `rehearse-analysis-*`)가 현재 `key.startswith("videos/")` + `split("/")[1]` 방식으로 키를 파싱하고 있다. 이 코드는 다음 문제를 가진다:

1. **prefix 변경에 전면 종속** — `interviews/raw/` 도입 시 두 함수가 즉시 깨진다
2. **MP4 출력 키 유도 로직이 fragile** — `key.rsplit(".",1)[0] + ".mp4"`로 같은 prefix에 변환본 생성 → 신규 스키마는 `interviews/raw/` → `interviews/mp4/`로 prefix 치환 필요
3. **`backup_to_s3` 하드코딩** — `f"analysis-backup/{interview_id}/qs_{question_set_id}.json"`을 `interviews/feedback/...`로 교체

### 왜 구/신 병행 파서를 넣지 않는가 (Decision Log)

초안에서는 전환 기간 동안 Lambda가 구·신 키 두 패턴을 모두 파싱하는 호환 모드를 검토했으나, 다음 이유로 **전부 기각**한다:

- **장기적 코드 부채** — "3주 후 레거시 제거 PR" 약속은 실무에서 거의 지켜지지 않고 영구히 남는다. dead code 누적 + 파서 분기 버그 유발
- **`is_legacy` 분기의 오염 범위** — parser뿐 아니라 `derive_mp4_key`·`derive_feedback_key`·handler의 로깅·테스트·smoke 페이로드까지 **두 벌**을 유지해야 함. 변경 비용 2배
- **테스트 매트릭스 폭증** — 구/신 × convert/analysis × normal/failure = 최소 8개 경로. 컷오버 이후 유지보수 부담
- **사고 표면 증가** — 구 파서가 레거시 키를 받아 처리 중 실패하면 dev 환경 DB 상태 오염 (PARTIAL/FAILED). 디버깅 시 "구 경로인지 신 경로인지" 매번 판단해야 함
- **"신규 스키마만" 명시된 사용자 결정** — 단순성·명료성이 단기 편의보다 우선

대신 **Plan 04**에서 dev 환경에 드레인 + 레거시 데이터 일괄 삭제 + 원샷 컷오버를 수행해 legacy 경로가 실행될 일이 애초에 없도록 만든다. "코드가 아니라 운영 절차로 해결"하는 방식.

### 미매칭 키 처리 원칙

신규 스키마와 일치하지 않는 키는 Lambda 코드에서 **조용히 스킵**한다 (`return 200 Skipped`). 예외 발생 금지·처리 경로 없음. 이유:

- EventBridge 재시도 / DLQ로 빠지지 않도록 명시적 성공 반환
- CloudWatch 로그에 `[Skipped] key=...` 형태로 1줄만 기록 → 알림 폭증 방지
- 드레인 기간 중 혹시 남은 레거시 이벤트가 도착해도 처리되지 않고 조용히 버려짐 (dev only, 실사용자 영향 0)
- prod는 신규 스키마로만 시작하므로 미매칭 키가 도달하는 것 자체가 **이상 신호** → 알림은 Plan 04의 후속 모니터링에서 CloudWatch Metric Filter로 별도 설정

이 태스크는 `s3_keys.py` 모듈을 두 Lambda 패키지(`lambda/analysis/`, `lambda/convert/`)에 **독립 사본**으로 배치하고 handler를 전환한다. 공용 디렉토리(`lambda/shared/`)·Lambda Layer는 사용하지 않는다 (사유: 아래 "패키지 구조 결정" 섹션).

## 생성/수정 파일

| 파일 | 작업 |
|---|---|
| `lambda/analysis/s3_keys.py` | 신규 작성 (Lambda analysis 패키지 내 사본, SSOT 구현) |
| `lambda/convert/s3_keys.py` | 신규 작성 (Lambda convert 패키지 내 사본, analysis와 bit-for-bit 동일) |
| `lambda/analysis/handler.py` | parser 로직을 `s3_keys.py` 호출로 교체 (`:35, :39, :511-523`) |
| `lambda/convert/handler.py` | 동일 (`:19, :23, :44, :77-89`) |
| `lambda/analysis/api_client.py` | `backup_to_s3` 경로 수정 (`:78-87`) |
| `lambda/analysis/handler.py` | `:155` — `backup_to_s3(parsed, feedback_payload)` 호출 시그니처 변경 |
| `lambda/analysis/tests/` | **신규 디렉토리 생성** (현재 미존재) + `__init__.py` |
| `lambda/convert/tests/` | **신규 디렉토리 생성** (현재 미존재) + `__init__.py` |
| `lambda/analysis/tests/test_s3_keys.py` | 신규 단위 테스트 |
| `lambda/convert/tests/test_s3_keys.py` | 동일 |

**Lambda shared layer 대안**: AWS Lambda Layer로 공용 코드 배포하는 방식도 있지만 배포·버전 관리 복잡도 증가. 본 플랜에서는 **단순 파일 사본** 채택 (두 Lambda는 각자 zip 패키징).

**`lambda/shared/` 디렉토리 미사용 명시**: 초안에 `lambda/shared/s3_keys.py`를 두는 안이 있었으나 Option A(사본) 채택에 따라 공용 디렉토리는 **생성하지 않는다**. SSOT은 `docs/architecture/s3-key-schema.md`(Plan 01 산출물)이고, 코드 drift는 아래 검증 단계의 `diff lambda/analysis/s3_keys.py lambda/convert/s3_keys.py`로 차단한다.

**테스트 디렉토리 현재 상태**: `lambda/analysis/tests/`, `lambda/convert/tests/`는 **현재 리포에 존재하지 않는다**. 본 태스크에서 디렉토리·`__init__.py` 생성까지 포함한다. pytest 실행 경로는 각 Lambda 패키지 루트를 `rootdir`로 설정 (기존 lambda 빌드 스크립트가 패키지 독립이므로 통합 conftest 불필요).

## 상세

### `lambda/{analysis,convert}/s3_keys.py` (양 패키지 bit-for-bit 동일 사본)

```python
"""S3 키 파싱·유도 유틸. docs/architecture/s3-key-schema.md v1.0 규격 구현.

레거시 키(videos/, analysis-backup/)는 이 모듈에서 처리하지 않는다.
전환은 Plan 04의 드레인·삭제·원샷 컷오버 절차로 수행한다.
"""
import re
from dataclasses import dataclass
from typing import Optional

# 신규 스키마 (v1) — 단일 패턴
RAW_KEY_PATTERN = re.compile(
    r"^interviews/raw/"
    r"(?P<year>\d{4})/(?P<month>\d{2})/(?P<day>\d{2})/"
    r"(?P<interview_id>\d+)/(?P<qs_id>\d+)/"
    r"(?P<uuid>[a-f0-9]{12})\.webm$"
)


@dataclass(frozen=True)
class ParsedKey:
    interview_id: int
    question_set_id: int
    uuid: str
    raw_key: str


def parse_raw_key(key: str) -> Optional[ParsedKey]:
    """원본 webm 키를 파싱한다. 신규 스키마(v1)만 지원.

    매칭 실패 시 None — handler는 이 경우 200 Skipped로 반환해야 한다.
    """
    m = RAW_KEY_PATTERN.match(key)
    if not m:
        return None

    return ParsedKey(
        interview_id=int(m.group("interview_id")),
        question_set_id=int(m.group("qs_id")),
        uuid=m.group("uuid"),
        raw_key=key,
    )


def derive_mp4_key(parsed: ParsedKey) -> str:
    """원본 raw 키에서 MP4 변환본 키를 유도한다.

    interviews/raw/.../{uuid}.webm → interviews/mp4/.../{uuid}.mp4
    """
    return (
        parsed.raw_key.replace("interviews/raw/", "interviews/mp4/", 1)
        .rsplit(".", 1)[0]
        + ".mp4"
    )


def derive_feedback_key(parsed: ParsedKey) -> str:
    """피드백 백업 키를 유도한다.

    interviews/raw/.../{uuid}.webm → interviews/feedback/.../{uuid}.json
    """
    return (
        parsed.raw_key.replace("interviews/raw/", "interviews/feedback/", 1)
        .rsplit(".", 1)[0]
        + ".json"
    )
```

**핵심 불변식**: `parse_raw_key()`가 `None`을 반환하면 handler는 **즉시 200 Skipped**로 반환한다. 절대 처리 경로로 넘어가지 않는다. 이는 드레인 기간 중 혹시 도달한 잔여 레거시 이벤트에 대한 방어선이기도 하다.

### `lambda/convert/handler.py` 수정

**Before** (`:19, :23, :44`):
```python
if not key.startswith("videos/") or not key.endswith(".webm"):
    return {...}

interview_id, question_set_id = _parse_ids_from_key(key)
...
output_key = key.rsplit(".", 1)[0] + ".mp4"
```

**After**:
```python
from s3_keys import parse_raw_key, derive_mp4_key

parsed = parse_raw_key(key)
if parsed is None:
    # 신규 스키마 미매칭 → 즉시 종료. 처리 경로 진입 금지.
    print(f"[Convert][Skipped] Non-matching key (not interviews/raw/v1): {key}")
    return {"statusCode": 200, "body": "Skipped: not a v1 raw key"}

interview_id = parsed.interview_id
question_set_id = parsed.question_set_id
output_key = derive_mp4_key(parsed)
```

기존 `_parse_ids_from_key` 함수 제거(`:77-89`). 정규식 파서가 대체.

### `lambda/analysis/handler.py` 수정

`:35` 필터 + `:39` parser + `:511-523` 함수를 동일 패턴으로 교체. `parse_raw_key()` 하나로 통합.

### `lambda/analysis/api_client.py::backup_to_s3` 수정

**Before** (`:78-`):
```python
def backup_to_s3(interview_id: int, question_set_id: int, data: dict) -> None:
    s3 = boto3.client("s3")
    key = f"analysis-backup/{interview_id}/qs_{question_set_id}.json"
    s3.put_object(...)
```

**문제**: 이 함수는 현재 `handler.py:155`에서 호출되지만 `parsed` 객체를 받지 않고 int 2개만 받음. `{uuid}`와 날짜 파티션을 복원할 수 없다.

**해결**: 시그니처를 변경해 `parsed: ParsedKey`를 받거나 `raw_key: str`을 받도록 수정:

```python
def backup_to_s3(parsed: ParsedKey, data: dict) -> None:
    s3 = boto3.client("s3")
    key = derive_feedback_key(parsed)
    s3.put_object(
        Bucket=Config.S3_BUCKET,
        Key=key,
        Body=json.dumps(data, ensure_ascii=False).encode("utf-8"),
        ContentType="application/json",
    )
    print(f"[Analysis] 피드백 S3 백업 완료: {key}")
```

**호출부 수정** (`handler.py:155`):
```python
# Before
backup_to_s3(interview_id, question_set_id, feedback_payload)

# After
backup_to_s3(parsed, feedback_payload)
```

`parsed`는 `_run_pipeline()` 진입 시 이미 존재하므로 함수 시그니처에 추가 전달.

### 단위 테스트 `lambda/{analysis,convert}/tests/test_s3_keys.py`

```python
import pytest
from s3_keys import parse_raw_key, derive_mp4_key, derive_feedback_key


class TestParseRawKey:
    def test_v1_format(self):
        key = "interviews/raw/2026/04/12/123/456/9c8b8a2d3f1e.webm"
        parsed = parse_raw_key(key)
        assert parsed.interview_id == 123
        assert parsed.question_set_id == 456
        assert parsed.uuid == "9c8b8a2d3f1e"
        assert parsed.raw_key == key

    # 레거시 키는 전부 None 반환 (처리 경로 미존재)
    def test_legacy_videos_prefix_returns_none(self):
        assert parse_raw_key("videos/123/qs_456.webm") is None

    def test_legacy_analysis_backup_returns_none(self):
        assert parse_raw_key("analysis-backup/123/qs_456.json") is None

    def test_mp4_key_rejected(self):
        assert parse_raw_key("interviews/mp4/2026/04/12/1/1/abc123def456.mp4") is None

    def test_feedback_key_rejected(self):
        assert parse_raw_key("interviews/feedback/2026/04/12/1/1/abc123def456.json") is None

    def test_garbage_key_rejected(self):
        assert parse_raw_key("random/garbage.txt") is None

    def test_invalid_date_partition_rejected(self):
        # 날짜 자리가 숫자가 아님
        assert parse_raw_key("interviews/raw/abcd/04/12/1/1/abc123def456.webm") is None

    def test_short_uuid_rejected(self):
        # UUID 길이 미달
        assert parse_raw_key("interviews/raw/2026/04/12/1/1/abc.webm") is None

    def test_uppercase_uuid_rejected(self):
        # 대문자 hex 금지 (규격상 lowercase)
        assert parse_raw_key("interviews/raw/2026/04/12/1/1/ABC123DEF456.webm") is None


class TestDeriveMp4Key:
    def test_v1_format(self):
        parsed = parse_raw_key("interviews/raw/2026/04/12/123/456/abc123def456.webm")
        assert derive_mp4_key(parsed) == "interviews/mp4/2026/04/12/123/456/abc123def456.mp4"


class TestDeriveFeedbackKey:
    def test_v1_format(self):
        parsed = parse_raw_key("interviews/raw/2026/04/12/123/456/abc123def456.webm")
        assert derive_feedback_key(parsed) == "interviews/feedback/2026/04/12/123/456/abc123def456.json"
```

**주의**: 레거시 키에 대한 "처리 성공" 테스트는 **절대 추가하지 않는다**. legacy 경로가 코드에 없다는 사실 자체를 테스트로 보장한다.

### 배포 절차

**Plan 04의 드레인·삭제 컷오버 단계에서 실행**. 본 plan은 코드 준비만 담당한다.

1. `lambda/lambda-safe-deploy.sh --env dev analysis` → 새 버전 publish (alias 미전환)
2. Smoke test 페이로드 2종:
   - **신규 유효 키** (정상 파싱 → 처리 진입): `interviews/raw/2026/04/12/1/1/000000000001.webm`
   - **미매칭 키** (Skipped 확인): `videos/1/qs_1.webm` → 응답 `{"statusCode":200,"body":"Skipped: not a v1 raw key"}` 확인
3. 신규 키는 **처리 성공**, 미매칭 키는 **Skipped 반환 + 처리 경로 미진입** 로그 확인 시 alias 전환
4. `convert` 함수 동일 절차

### 패키지 구조 결정

**옵션 A (채택)**: `lambda/analysis/s3_keys.py`, `lambda/convert/s3_keys.py` 각자 파일 사본
- 장점: Lambda zip 독립성, Layer 운영 불필요
- 단점: 코드 중복 (수정 시 양쪽 동시 변경 필수, CI에서 diff 검증 가능)

**옵션 B (기각)**: Lambda Layer로 `rehearse-shared-{env}` 배포
- 장점: 단일 소스
- 단점: Layer 버전 관리, 배포 스크립트 추가 복잡도, 런타임 의존성 증가

**옵션 A 선택 이유**: 2개 함수만 공유하는 규모에서 Layer 도입 오버헤드 대비 효용 낮음. CI에 `diff lambda/analysis/s3_keys.py lambda/convert/s3_keys.py` 검증 1줄 추가로 drift 방지.

## 담당 에이전트

- Implement: `devops-engineer` 또는 `backend` — Lambda Python 수정, 단위 테스트, 배포
- Review: `code-reviewer` — 정규식 견고성, 호환성 파서, 배포 스크립트

## 검증

- `lambda/analysis/tests/__init__.py`, `lambda/convert/tests/__init__.py` 생성 확인
- `pytest lambda/analysis/tests/test_s3_keys.py` 전 케이스 통과
- `pytest lambda/convert/tests/test_s3_keys.py` 전 케이스 통과
- `diff lambda/analysis/s3_keys.py lambda/convert/s3_keys.py` → 차이 없음 (CI drift 가드). CI 스크립트에 이 1줄 추가
- `grep -rn "videos/\|analysis-backup/\|is_legacy\|LEGACY" lambda/analysis/ lambda/convert/ --exclude-dir=tests` → 매치 0건 (legacy 잔재 없음 확인). **`-r` 플래그 필수** (디렉토리 재귀), 테스트 파일에는 legacy 키 문자열이 rejection 케이스로 포함되므로 제외
- `grep -rn "_parse_ids_from_key" lambda/analysis/ lambda/convert/` → 매치 0건 (구 함수 제거 확인)
- `grep -rn "lambda/shared" docs/plans/s3-key-schema-redesign/` → Plan 01의 개념 언급 외 실제 디렉토리 미존재 확인 (`ls lambda/shared 2>&1 | grep -q "No such"`)
- `lambda/lambda-safe-deploy.sh --env dev analysis` 새 버전 publish 성공 (alias 미전환 상태로 대기 — Plan 04에서 전환)
- `lambda/lambda-safe-deploy.sh --env dev convert` 동일
- 수동 invoke 2종 (미버전 상태):
  ```bash
  # 1) 신규 유효 키 → 처리 경로 진입
  aws lambda invoke --function-name rehearse-analysis-dev:<new-version> \
    --payload '{"detail":{"bucket":{"name":"rehearse-videos-dev"},"object":{"key":"interviews/raw/2026/04/12/1/1/000000000001.webm"}}}' \
    /tmp/out.json
  # → 200, 이후 파이프라인 실행 (S3에 더미 파일 미존재 시 download 실패는 예상)

  # 2) 레거시 키 → Skipped (처리 경로 미진입)
  aws lambda invoke --function-name rehearse-analysis-dev:<new-version> \
    --payload '{"detail":{"bucket":{"name":"rehearse-videos-dev"},"object":{"key":"videos/1/qs_1.webm"}}}' \
    /tmp/out.json
  # → 200 {"body":"Skipped: not a v1 raw key"}
  ```
- CloudWatch Logs에서 Skipped 로그만 1줄 찍히고 후속 처리 로그 0건 확인
- `progress.md` Task 3 → Completed (단, alias 전환은 Plan 04의 컷오버 단계에서 집행)
