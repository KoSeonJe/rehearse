# Plan 01: 키 스키마 · Lifecycle · EventBridge 규격 확정 (SSOT)

> 상태: Draft
> 작성일: 2026-04-12

## Why

본 태스크는 코드 변경 없이 **키 설계의 Single Source of Truth(SSOT) 문서를 `docs/architecture/`에 고정**하는 작업이다. 이 규격이 확정되지 않으면 Task 2(백엔드)와 Task 3(Lambda)가 서로 다른 해석으로 진행될 위험이 있다. 규격 문서가 먼저 나와야 병렬 구현이 안전하다.

또한 이 문서는 향후 신규 artifact 추가(예: `interviews/transcripts/`), 키 버전 변경, Lifecycle 정책 조정 시 변경 요청(change request)의 시작점이 된다.

## 생성/수정 파일

| 파일 | 작업 |
|---|---|
| `docs/architecture/s3-key-schema.md` | 신규 작성 (SSOT) |
| `docs/architecture/infrastructure-status.md` | S3 섹션에 `s3-key-schema.md` 링크 추가 |

## 상세

### `docs/architecture/s3-key-schema.md` 내용 구성

#### 1. 스키마 정의 (표 + BNF)

```
<key>             ::= <interviews-key> | <db-backup-key>
<interviews-key>  ::= "interviews/" <artifact> "/" <date-partition> "/" <entity-path> "/" <leaf>
<artifact>        ::= "raw" | "mp4" | "frames" | "audio" | "feedback"
<date-partition>  ::= <YYYY> "/" <MM> "/" <DD>   ; UTC
<entity-path>     ::= <interview-id> "/" <question-set-id>
<leaf>            ::= <uuid> "." <ext> | <uuid> "/" <sub-leaf>
<uuid>            ::= 12-char lowercase hex (no hyphens)
<ext>             ::= "webm" | "mp4" | "json" | "jpg" | "mp3"
<db-backup-key>   ::= "db-backups/" <date-partition> "/rehearse-" <env> "-" <timestamp> ".sql.gz"
```

#### 2. 각 artifact별 설명·생성 주체·소비 주체

| Artifact | 확장자 | 생성자 | 소비자 | 설명 |
|---|---|---|---|---|
| `raw` | `.webm` | 브라우저 MediaRecorder (presigned PUT) | `rehearse-convert-*`, `rehearse-analysis-*` Lambda | 면접 원본 녹화 |
| `mp4` | `.mp4` | `rehearse-convert-*` Lambda (MediaConvert) | 프론트 스트리밍 `<video>` | 재생용 변환본 |
| `frames` | `.jpg` | `rehearse-analysis-*` Lambda (FFmpeg, 선택 백업) | 디버깅 도구 | 실패 재현용 임시 백업, 7일 후 자동 삭제 |
| `audio` | `.mp3` | `rehearse-analysis-*` Lambda (FFmpeg, 선택 백업) | 디버깅 도구 | Gemini audio 재현용 임시 백업, 7일 후 자동 삭제 |
| `feedback` | `.json` | `rehearse-analysis-*` Lambda (`backup_to_s3`) | 복구 스크립트 | BE 저장 실패 시 피드백 payload 백업 |

#### 3. 불변식 (Invariants)

- **I1 (파생 uuid 공유)**: 특정 `raw` 객체 1건에서 유도된 `mp4` / `feedback` 파생 아티팩트는 해당 `raw`와 동일한 `{uuid}`를 가진다. 재녹화는 새 `raw` 객체이고 새 `{uuid}`를 발급받는다 — 과거 녹화의 uuid와 공유하지 않는다.
- **I2 (uuid 불변)**: `{uuid}`는 `raw` 키 생성 시점에 결정되고 해당 `raw` 객체의 수명 동안 변경 금지. 재녹화 시에는 새 `raw`·새 `uuid`로 독립된 객체 집합을 형성한다.
- **I3 (날짜 파티션 복제)**: 파생 키의 날짜 파티션은 `raw` 키 값을 그대로 복제한다 (cross-day 변환 발생해도 raw 기준 유지). Lambda의 현재 시각을 사용하면 안 된다.
- **I4 (raw 전용)**: `interviews/raw/` prefix 객체는 **원본 WebM만 허용** — MP4/기타 업로드 금지. 위반 시 EventBridge 무한 루프 위험.
- **I5 (엔티티 ID 양수)**: `{interviewId}`, `{questionSetId}`는 양의 정수(long). 음수·0 금지.
- **I6 (PII 금지)**: 이메일·이름·토큰 등 PII는 키 어느 세그먼트에도 포함 금지. 역추적은 DB 조회로 수행.

#### 4. Lifecycle 정책 (5-prefix 분리)

| Prefix | 0~Nd | Transition 1 | Transition 2 | Expiration |
|---|---|---|---|---|
| `interviews/raw/` | Standard 0~30d | 30d → Standard-IA | 90d → Glacier IR | — |
| `interviews/mp4/` | Standard 0~90d | 90d → Standard-IA | — | — |
| `interviews/frames/` | — | — | — | **7d Expire** |
| `interviews/audio/` | — | — | — | **7d Expire** |
| `interviews/feedback/` | Standard 0~30d | 30d → Standard-IA | — | — |

**적용 대상**: `rehearse-videos-dev`, `rehearse-videos-prod` 양쪽 동일. prod 쪽은 `prod-environment-setup/plan-03`에서 프로비저닝.

#### 5. EventBridge 규칙 템플릿

```json
{
  "source": ["aws.s3"],
  "detail-type": ["Object Created"],
  "detail": {
    "bucket": { "name": ["<BUCKET_NAME>"] },
    "object": { "key": [{ "prefix": "interviews/raw/" }] }
  }
}
```

`<BUCKET_NAME>` 치환:
- dev: `rehearse-videos-dev` → `rehearse-video-uploaded-dev`
- prod: `rehearse-videos-prod` → `rehearse-video-uploaded-prod`

**Targets**: `rehearse-analysis-{env}:live` (alias) + `rehearse-convert-{env}:live`

#### 6. 파싱 규약 (Lambda + 임의 consumer)

```python
RAW_KEY_PATTERN = re.compile(
    r"^interviews/raw/(?P<year>\d{4})/(?P<month>\d{2})/(?P<day>\d{2})/"
    r"(?P<interview_id>\d+)/(?P<qs_id>\d+)/(?P<uuid>[a-f0-9]{12})\.webm$"
)
```

Java side (`backend`):
```java
private static final Pattern RAW_KEY_PATTERN = Pattern.compile(
    "^interviews/raw/(\\d{4})/(\\d{2})/(\\d{2})/(\\d+)/(\\d+)/([a-f0-9]{12})\\.webm$"
);
```

**레거시 패턴**(호환성 기간):
```
^videos/(?P<interview_id>\d+)/qs_(?P<qs_id>\d+)\.webm$
```

#### 7. UUID 생성 규약

- **길이**: 12자 (hex lowercase)
- **생성**: `UUID.randomUUID().toString().replace("-","").substring(0,12)` (Java) / `uuid4().hex[:12]` (Python)
- **엔트로피**: ~48-bit. 일일 수만 개 객체 기준 충돌 무시 가능. 규모 초과 시 16자로 확장 (SSOT 업데이트 필요)
- **재사용 금지**: 같은 QuestionSet 재녹화 시 새 UUID 발급 → 과거 녹화 S3 객체 보존

#### 8. 파생 키 유도 공식

```
raw_key  = interviews/raw/{Y}/{M}/{D}/{iid}/{qsid}/{uuid}.webm
mp4_key  = interviews/mp4/{Y}/{M}/{D}/{iid}/{qsid}/{uuid}.mp4
fb_key   = interviews/feedback/{Y}/{M}/{D}/{iid}/{qsid}/{uuid}.json
frame_base = interviews/frames/{Y}/{M}/{D}/{iid}/{qsid}/{uuid}/
audio_base = interviews/audio/{Y}/{M}/{D}/{iid}/{qsid}/{uuid}/
```

**Python 참조 구현**:
```python
def derive_mp4_key(raw_key: str) -> str:
    return raw_key.replace("interviews/raw/", "interviews/mp4/", 1).rsplit(".", 1)[0] + ".mp4"

def derive_feedback_key(raw_key: str) -> str:
    return raw_key.replace("interviews/raw/", "interviews/feedback/", 1).rsplit(".", 1)[0] + ".json"
```

#### 9. 레거시 → 신규 매핑 (개념 참조용)

| 레거시 | 신규 |
|---|---|
| `videos/{iid}/qs_{qsid}.webm` | `interviews/raw/YYYY/MM/DD/{iid}/{qsid}/{uuid}.webm` |
| `videos/{iid}/qs_{qsid}.mp4` | `interviews/mp4/YYYY/MM/DD/{iid}/{qsid}/{uuid}.mp4` |
| `analysis-backup/{iid}/qs_{qsid}.json` | `interviews/feedback/YYYY/MM/DD/{iid}/{qsid}/{uuid}.json` |

**중요 — Lambda는 이 매핑을 코드로 구현하지 않는다**: Plan 03 결정(신규 전용 파서, 레거시 코드 경로 미존재)에 따라 Lambda는 레거시 키를 처리하지 않는다. 위 표는 순수 개념 참조이며, 전환은 Plan 04의 드레인·삭제·원샷 컷오버로 수행한다. 레거시 키가 Lambda에 도달하면 `parse_raw_key()`가 `None`을 반환하고 handler는 즉시 `200 Skipped`로 종료한다.

#### 10. 변경 이력(Changelog) 섹션

문서 하단에 `## Changelog` 섹션을 두고 향후 스키마 변경 시 append. 현재 버전: `v1.0 (2026-04-12)`.

## 담당 에이전트

- Implement: `documentation-expert` 또는 `writer` — SSOT 문서 작성
- Review: `architect-reviewer` — 불변식 충돌 검토, 세그먼트 설계 적절성

## 검증

- `docs/architecture/s3-key-schema.md` 존재
- 문서에 BNF / 불변식 6개 / Lifecycle 5-prefix 표 / EventBridge JSON / 파싱 정규식 / UUID 규약 / 파생 키 공식 / 레거시 매핑 / Changelog 전부 포함
- `docs/architecture/infrastructure-status.md`에 링크 추가
- 팀 리뷰 — Task 2·3 착수 전 승인
- `progress.md` Task 1 → Completed
