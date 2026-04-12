# S3 Key Schema 재설계 — 요구사항 정의

> 상태: Draft
> 작성일: 2026-04-12
> **후행 프로젝트**: [`docs/plans/prod-environment-setup/`](../prod-environment-setup/requirements.md) (운영서버 구축 — 본 작업 완료 후 착수)

---

## ⚠️ 선행 작업 명시

**본 작업은 `prod-environment-setup`(운영서버 구축)의 선행 작업이다.**

운영서버를 깨끗한 버킷에서 출발시키기 위해 **prod 구축 전에** S3 키 구조를 재정립한다. 현재 스키마(`videos/{interviewId}/qs_{questionSetId}.webm`) 그대로 prod를 시작하면 EventBridge 규칙·Lifecycle 정책·파생 아티팩트 관리 등 후속 설계가 전부 취약한 기반 위에 쌓인다. 본 작업을 선행 완료하면:

- prod S3 버킷은 신규 스키마로만 시작 → **prod 마이그레이션 부담 0**
- `prod-environment-setup/plan-03-s3-eventbridge-mediaconvert.md`의 EventBridge 규칙을 단순 prefix 기반으로 명확히 정의 가능 (현재 `suffix: .webm` 의존 제거)
- Lifecycle 정책을 artifact 타입별로 분리 가능 (원본/파생/임시 백업/피드백 각각)
- parser 회귀 리스크를 dev에서 전부 흡수한 뒤 prod에 투입

**순서**: `s3-key-schema-redesign` (dev 검증) → `prod-environment-setup` (prod 구축)

---

## Why

### 1. Why? (현재 스키마의 문제)

현재 S3 키는 3곳에서 생성되며 다음 구조로 흩어져 있다:

| 용도 | 현재 키 | 생성 위치 |
|---|---|---|
| 원본 녹화 | `videos/{interviewId}/qs_{questionSetId}.webm` | `backend/.../QuestionSetService.java:74` |
| 변환본(MP4) | `videos/{interviewId}/qs_{questionSetId}.mp4` | `lambda/convert/handler.py:44` (`rsplit` 치환) |
| 피드백 백업 | `analysis-backup/{interviewId}/qs_{questionSetId}.json` | `lambda/analysis/api_client.py:80` |

**7가지 부실한 점**:

1. **원본·파생 혼재** — `videos/` prefix에 WebM 원본과 MP4 변환본이 공존. EventBridge 규칙은 `suffix: .webm`로만 분기 → 스팩상 MP4를 실수로 webm 확장자로 올리면 무한 루프 위험. Lifecycle 정책도 원본(장기 보관)과 파생(짧은 TTL)을 분리 적용 불가
2. **날짜 파티션 없음** — 수십만 객체 도달 시 `aws s3 ls`, Athena 쿼리, Lifecycle 필터, S3 Inventory 전부 비효율. 실무에서 가장 흔하게 후회하는 누락
3. **재녹화 시 overwrite** — `qs_{id}.webm` 고정이라 재시도가 원본 덮어씀. S3 Versioning만 의존하면 키 자체로 버전 식별 불가 (versionId는 감사 로그에서 가독성 최악)
4. **불변 식별자 부재** — UUID/sha 없이 DB 시퀀스(`interviewId`)만 사용. `correlation_id` ↔ S3 객체 1:1 매핑 불가능 → 장애 조사 시 "어떤 업로드가 해당 correlation_id인가" 추적 어려움
5. **artifact 타입 명시성 부족** — `videos/`만 봐서는 원본인지 처리본인지 키에서 즉시 판별 불가
6. **파싱 취약** — `lambda/.../handler.py`의 `key.split("/")[1]` 방식이 prefix 변경에 전면 종속. Lambda 2개(convert/analysis)가 동시에 깨질 수 있는 **연쇄 실패 지점**
7. **analysis 임시 산출물 경로 부재** — frames / audio / transcripts는 `/tmp`만 사용해 디버깅 시 재현 불가. 실패 케이스 보존용 prefix 설계 없음

### 2. Goal

신규 S3 키 스키마가 다음을 충족한다:

- 원본 / 파생 / 피드백 / 임시 산출물 / DB백업을 **최상위 prefix로 완전 분리**
- 날짜 파티션(`YYYY/MM/DD`) 포함 → Lifecycle·쿼리·Inventory 자동화 기반
- 불변 UUID 리프로 재녹화·재시도 히스토리 보존
- EventBridge 규칙은 **prefix만으로 완전 분기** (suffix 의존 제거)
- Lifecycle 정책이 artifact별 독립 적용
- Lambda 파싱 로직 정규식 기반 + **신규 스키마 단일 지원** (구 경로 코드 제거)
- correlation_id ↔ S3 object 1:1 매핑

**성공 기준**:

- dev 환경에서 신규 스키마로 면접 1회 E2E 완주 (녹화 → 변환 → 분석 → 피드백)
- Lambda는 **신규 스키마만 처리**, 레거시 키는 `Skipped`로 즉시 반환 (처리 경로 미진입)
- dev 레거시 S3 객체 전량 삭제 후 `videos/`·`analysis-backup/` prefix 카운트 0
- Lambda `[Skipped]` 로그 24h 모니터링 중 0건에 수렴
- prod-environment-setup 구축 시 신규 스키마만 사용

### 3. Evidence (2026-04-12 실측)

- `backend/src/main/java/com/rehearse/api/domain/questionset/service/QuestionSetService.java:74`
  ```java
  String s3Key = String.format("videos/%d/qs_%d.webm", interviewId, questionSetId);
  ```
- `lambda/convert/handler.py:19` — `if not key.startswith("videos/") or not key.endswith(".webm")`
- `lambda/convert/handler.py:44` — `output_key = key.rsplit(".", 1)[0] + ".mp4"` → 같은 prefix에 mp4 생성
- `lambda/convert/handler.py:77-89`, `lambda/analysis/handler.py:511-523` — `_parse_ids_from_key`: split 기반 fragile parsing
- `lambda/analysis/api_client.py:80` — `key = f"analysis-backup/{interview_id}/qs_{question_set_id}.json"`
- `backend/.../file/entity/FileMetadata.java:33-37` — `s3_key VARCHAR(500)`, `streaming_s3_key VARCHAR(500)` → 신규 키(예상 70~90자) 수용 충분
- `backend/.../file/entity/FileType.java:3-4` — 현재 `VIDEO` 단일 enum. 파생 타입 추가 여지 확인 필요(별건)

### 4. Trade-offs

- **Lambda 구/신 병행 파서 기각** — 초안에서는 전환 기간 Lambda가 양쪽 키를 모두 파싱하는 호환 모드를 검토했으나 기각. 사유:
  - "3주 후 레거시 제거 PR" 약속이 실무에서 거의 지켜지지 않음 → 영구 dead code
  - `is_legacy` 분기가 parser/derive/handler/테스트에 전염되어 코드·테스트 매트릭스 2배
  - 구 경로가 처리 중 실패 시 dev DB 상태 오염 → 디버깅 분기 증가
  - 단순성·명료성이 단기 편의보다 우선 (사용자 결정)
- **대안: 드레인·삭제·원샷 컷오버 채택** — Plan 04에서 dev 환경 freeze → in-flight drain → 레거시 S3/DB 일괄 삭제 → Lambda + 백엔드 동시 배포 → E2E 검증. 다운타임 발생하지만 dev는 실사용자 없고, "오래 걸리더라도 신규 전용" 사용자 지시와 정합
- **코드 변경 3곳** (백엔드 1, Lambda 2). 범위는 작지만 회귀 테스트 필수
- **기존 dev 데이터 폐기** — dev는 테스트 데이터이므로 소실 허용. 대안(백필 배치)은 비용 대비 효용 낮음
- **버전 prefix(`v1/`) 기각** — 현 시점 depth 증가 불필요. 향후 변경 시 `raw/` → `raw-v2/` 식으로 artifact 접미사 활용
- **사용자 ID 키 포함 기각** — 로그/PR/Slack 공유 시 PII 노출 우려. `interviewId`만 포함하고 DB 조회로 역추적
- **Hash sharding 기각** — Rehearse 규모(~수만 객체)는 S3 자동 파티셔닝으로 충분
- **UTC 기준 날짜** — 로컬 타임존(Asia/Seoul) 대비 데이터 파이프라인 표준 적합
- **총 소요 3~5일** — Plan 01(문서) → 02·03 병렬(2~3일) → 04 컷오버(반나절) + 24h 안정화. "오래 걸리더라도" 반영

## 목표

위 Goal 섹션 참조.

## 아키텍처 / 설계

### 신규 스키마

```
interviews/raw/YYYY/MM/DD/{interviewId}/{questionSetId}/{uuid}.webm
interviews/mp4/YYYY/MM/DD/{interviewId}/{questionSetId}/{uuid}.mp4
interviews/frames/YYYY/MM/DD/{interviewId}/{questionSetId}/{uuid}/frame-NNNN.jpg    (선택 백업)
interviews/audio/YYYY/MM/DD/{interviewId}/{questionSetId}/{uuid}/answer-NNN.mp3     (선택 백업)
interviews/feedback/YYYY/MM/DD/{interviewId}/{questionSetId}/{uuid}.json            (backup_to_s3)
db-backups/YYYY/MM/DD/rehearse-<env>-<timestamp>.sql.gz                             (prod plan-14 연동)
```

**세그먼트 의미**:

| 순서 | 세그먼트 | 의미 |
|---|---|---|
| 1 | `interviews` / `db-backups` | 도메인 최상위 prefix |
| 2 | `raw` / `mp4` / `frames` / `audio` / `feedback` | artifact 타입 (5종) |
| 3 | `YYYY/MM/DD` | 날짜 파티션 (UTC 업로드 시각 기준) |
| 4 | `{interviewId}` | 도메인 엔티티 ID — 디버깅 시 grep 가능 |
| 5 | `{questionSetId}` | 하위 엔티티 ID |
| 6 | `{uuid}` | 불변 식별자 (재녹화 보존) |
| 7 | `.ext` | content-type 단서 |

**핵심 불변식**: 같은 QuestionSet의 원본·변환·피드백은 **동일 `{uuid}`를 공유**한다 → 추적 용이.

**예**:
```
interviews/raw/2026/04/12/123/456/9c8b8a2d3f1e.webm     (원본)
interviews/mp4/2026/04/12/123/456/9c8b8a2d3f1e.mp4      (같은 uuid로 변환)
interviews/feedback/2026/04/12/123/456/9c8b8a2d3f1e.json (피드백 백업)
```

### Lifecycle 정책 (prefix별 독립 적용)

| Prefix | Standard | Transition 1 | Transition 2 | Expiration |
|---|---|---|---|---|
| `interviews/raw/` | 0~30d | 30d → Standard-IA | 90d → Glacier IR | — (영속) |
| `interviews/mp4/` | 0~90d | 90d → Standard-IA | — | — |
| `interviews/frames/` | — | — | — | **7d Expire** |
| `interviews/audio/` | — | — | — | **7d Expire** |
| `interviews/feedback/` | 0~30d | 30d → Standard-IA | — | — |
| `db-backups/` | prod-environment-setup/plan-14 정책 참조 | | | |

### EventBridge 규칙

```json
{
  "source": ["aws.s3"],
  "detail-type": ["Object Created"],
  "detail": {
    "bucket": { "name": ["rehearse-videos-{env}"] },
    "object": { "key": [{ "prefix": "interviews/raw/" }] }
  }
}
```

→ suffix `.webm` 체크 불필요. `interviews/raw/` prefix는 **원본 WebM만 수용하는 계약** (스키마 규약).

### 파싱 로직 (Lambda) — 신규 전용

```python
import re

# 신규 키 단일 패턴 — 레거시 파서 미존재
RAW_KEY_PATTERN = re.compile(
    r"^interviews/raw/"
    r"(?P<year>\d{4})/(?P<month>\d{2})/(?P<day>\d{2})/"
    r"(?P<interview_id>\d+)/(?P<qs_id>\d+)/"
    r"(?P<uuid>[a-f0-9]{12})\.webm$"
)

def parse_raw_key(key: str):
    m = RAW_KEY_PATTERN.match(key)
    if not m:
        return None  # handler는 즉시 200 Skipped 반환
    return {
        "interview_id": int(m.group("interview_id")),
        "qs_id": int(m.group("qs_id")),
        "uuid": m.group("uuid"),
    }

def derive_mp4_key(raw_key: str) -> str:
    """interviews/raw/...webm → interviews/mp4/...mp4"""
    return (
        raw_key.replace("interviews/raw/", "interviews/mp4/", 1)
        .rsplit(".", 1)[0]
        + ".mp4"
    )
```

**정책**: 매칭 실패(레거시 `videos/`, mp4, 기타) → 처리 경로 미진입. 관련 처리 흐름은 Lambda 코드에서 **제거**하고, 전환 시점의 데이터 문제는 Plan 04의 드레인·삭제 절차로 운영 레벨에서 해결한다.

### 백엔드 키 생성 유틸

`backend/src/main/java/com/rehearse/api/infra/aws/S3KeyGenerator.java` (신규):

```java
@Component
public class S3KeyGenerator {

    private static final DateTimeFormatter PARTITION_FMT =
        DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneOffset.UTC);

    public String generateRawVideoKey(long interviewId, long questionSetId) {
        String date = PARTITION_FMT.format(Instant.now());
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return String.format(
            "interviews/raw/%s/%d/%d/%s.webm",
            date, interviewId, questionSetId, uuid
        );
    }
}
```

## Scope

### In
- 신규 키 생성 유틸(백엔드): `S3KeyGenerator`
- `QuestionSetService.java:74` 수정 → 유틸 사용
- Lambda `convert/handler.py` prefix 체크 + 정규식 파서 + MP4 출력 키 계산
- Lambda `analysis/handler.py` 동일
- Lambda `analysis/api_client.py::backup_to_s3` 경로 수정 (`interviews/feedback/...`)
- Lambda는 **신규 스키마만** 처리 — 레거시 코드 경로 미존재
- dev 환경 freeze + in-flight drain + 레거시 S3/DB 일괄 삭제 + 원샷 컷오버 (Plan 04)
- dev E2E 회귀 검증 + 24h 안정화 모니터링
- `prod-environment-setup/` 3개 문서 업데이트 (본 작업 Task 4 완료 시점)

### Out
- 구 dev 데이터 백필 이관 — 폐기 허용 (Lifecycle 만료 대기)
- prod S3 버킷 생성 — `prod-environment-setup/plan-03`에서 수행 (단 신규 스키마 기반으로 진행)
- S3 Versioning 정책 변경 — 기존 유지
- 사용자 ID 키 포함 — PII 이유 기각
- Hash sharding — 규모 불필요
- `FileType` enum 확장 (VIDEO 외) — 별건

## 제약조건 / 환경

- **타임존**: UTC (백엔드 `S3KeyGenerator` + Lambda parser 모두 UTC 기준). 로컬 시간 Asia/Seoul과 분리
- **UUID 포맷**: hyphen 제거한 hex 12자 (`toString().replace("-","").substring(0,12)`) → 약 48-bit 엔트로피, 충돌 확률 무시 가능한 범위 (일일 수만 개 수준)
- **Lambda 처리 대상**: 신규 스키마만. 매칭 실패 키는 `{"statusCode":200,"body":"Skipped"}` 즉시 반환 — 처리 경로 미진입
- **컷오버 다운타임**: dev 메인터넌스 윈도우 ~90분 (freeze + drain 20분 + 삭제 10분 + 배포 10분 + E2E 검증 25분). 팀 공지 24시간 전 필수
- **레거시 데이터 소실 허용**: dev S3 `videos/`, `analysis-backup/` prefix 및 `file_metadata`의 레거시 행 일괄 삭제. dev는 테스트 데이터라 복구 불필요
- **`file_metadata.s3_key VARCHAR(500)`**: 신규 키 예상 길이 ~75자, 컬럼 여유 충분 (DB 마이그레이션 불필요)
- **FE 영향 없음**: FE는 presigned URL만 받으며 키 형식을 알지 못함. 회귀 없음

## Task 분할

| # | Plan 문서 | 태스크 | 태그 | 의존 |
|---|---|---|---|---|
| 1 | `plan-01-key-schema-definition.md` | 키 스키마·Lifecycle·EventBridge 규격 확정 (SSOT) | `[blocking]` | — |
| 2 | `plan-02-backend-key-generator.md` | 백엔드 `S3KeyGenerator` 유틸 + `QuestionSetService` 수정 | `[parallel]` | 1 |
| 3 | `plan-03-lambda-handlers-update.md` | Lambda convert/analysis 신규 전용 파서 + `backup_to_s3` 경로 수정 + Skipped 처리 | `[parallel]` | 1 |
| 4 | `plan-04-dev-rollout-validation.md` | dev freeze·drain·레거시 삭제·원샷 컷오버·E2E·24h 모니터링 + prod 문서 업데이트 | `[blocking]` | 2, 3 |

Task 2·3은 **병렬 가능** (BE/Lambda 독립).

## 후행 영향 (prod-environment-setup 업데이트 대상)

본 작업 Task 4 완료 시 다음 prod 문서를 갱신한다:

- `docs/plans/prod-environment-setup/requirements.md` — 선행 작업 명시 + Task 3 의존성 추가
- `docs/plans/prod-environment-setup/plan-03-s3-eventbridge-mediaconvert.md` — 예제 prefix를 `interviews/raw/`로 교체, EventBridge 패턴 갱신, Lifecycle 정책 5-prefix 분리
- `docs/plans/prod-environment-setup/progress.md` — 선행 작업 링크 + 완료 조건 추가
