# Plan 03: S3 `rehearse-videos-prod` + EventBridge + MediaConvert 구성 (v1.0 스키마)

> 상태: Draft
> 작성일: 2026-04-12
> **선행 프로젝트**: [`../s3-key-schema-redesign/`](../s3-key-schema-redesign/requirements.md) — dev 검증 완료 후 착수
> **SSOT**: [`docs/architecture/s3-key-schema.md`](../../architecture/s3-key-schema.md) v1.0

## Why

prod 환경의 면접 녹화 영상은 dev와 완전히 분리된 버킷에 저장되어야 한다. 동일 버킷 사용 시 (1) Lifecycle 정책 충돌, (2) dev 실험 데이터로 인한 실사용자 영상 오염, (3) Lambda 트리거 혼선이 발생한다. 또한 `lambda/analysis/config.py:8`과 `lambda/convert/config.py:9`의 기본값이 `rehearse-videos-dev`로 하드코딩되어 있어, prod Lambda에는 반드시 `S3_BUCKET=rehearse-videos-prod` 환경변수 오버라이드가 필요하다 (plan-04에서 설정).

본 플랜은 `s3-key-schema-redesign` 완료 후 착수하며, 모든 prefix/규격은 `docs/architecture/s3-key-schema.md` v1.0을 **단일 소스로 참조**한다. prod S3 버킷은 처음부터 신규 스키마(`interviews/raw/...`, `interviews/mp4/...`, `interviews/feedback/...` 등)만 사용하므로 마이그레이션 부담이 없다.

## 생성/수정 파일

| 자원 / 파일 | 작업 |
|---|---|
| S3 버킷 `rehearse-videos-prod` | 신규 생성 |
| 버킷 정책, CORS, Lifecycle, Versioning | 설정 |
| EventBridge 규칙 `rehearse-video-uploaded-prod` | 신규 생성 |
| MediaConvert | 기존 엔드포인트/Role 재사용 (리전 단위 리소스) |
| `docs/architecture/infrastructure-status.md` | prod S3/EventBridge 항목 추가 |

## 상세

### S3 버킷 `rehearse-videos-prod`

| 설정 | 값 |
|---|---|
| Region | `ap-northeast-2` |
| Block Public Access | 전부 ON (public 차단) |
| Versioning | Enabled (실수 삭제 복구용) |
| 서버사이드 암호화 | SSE-S3 (AES-256) |
| 객체 소유권 | Bucket owner enforced (ACL 비활성) |

#### 버킷 정책 (최소 권한)

- **prod EC2 Role** (`rehearse-prod-ec2-role`) — R/W (plan-02에서 연결)
- **prod Lambda Role** (`rehearse-lambda-prod-role`) — R/W (plan-04에서 연결)
- dev 자격은 명시적 거부 불필요(기본 Deny), 하지만 문서로 명시

#### CORS 규칙

```json
[
  {
    "AllowedOrigins": [
      "https://rehearse.co.kr",
      "https://www.rehearse.co.kr"
    ],
    "AllowedMethods": ["GET", "PUT", "POST"],
    "AllowedHeaders": ["*"],
    "ExposeHeaders": ["ETag"],
    "MaxAgeSeconds": 3600
  }
]
```

**주의**: `https://api.rehearse.co.kr`은 백엔드가 presigned URL을 발급하는 경로라 CORS origin에 포함될 필요 없음. 브라우저는 프론트 도메인에서 S3로 직접 PUT.

#### Lifecycle 정책 (v1.0 스키마 5-prefix + 공통 규칙)

SSOT: [`docs/architecture/s3-key-schema.md`](../../architecture/s3-key-schema.md) Lifecycle 섹션과 정확히 일치해야 함.

| Rule | Prefix | 전환 | Expiration |
|---|---|---|---|
| `raw-archive` | `interviews/raw/` | 30d → Standard-IA, 90d → Glacier IR | — (영속) |
| `mp4-archive` | `interviews/mp4/` | 90d → Standard-IA | — |
| `frames-expire` | `interviews/frames/` | — | **7d** |
| `audio-expire` | `interviews/audio/` | — | **7d** |
| `feedback-archive` | `interviews/feedback/` | 30d → Standard-IA | — |
| `db-backups-archive` | `db-backups/` | plan-14 정책 참조 | |
| `incomplete-multipart-cleanup` | (전체) | — | **7d** 미완료 멀티파트 업로드 삭제 |

**레거시 prefix 부재 확인**: `videos/`, `analysis-backup/`, `raw/`, `thumbs/` 등 구 스키마 prefix가 버킷에 생성되지 않아야 함 (신규 버킷은 처음부터 v1.0).

#### EventBridge 활성화

버킷 Properties → EventBridge notifications: **Enabled** (EventBridge 규칙이 S3 이벤트 수신 가능하도록)

### EventBridge 규칙 `rehearse-video-uploaded-prod`

**이벤트 패턴** (v1.0 신규 스키마 전용):
```json
{
  "source": ["aws.s3"],
  "detail-type": ["Object Created"],
  "detail": {
    "bucket": { "name": ["rehearse-videos-prod"] },
    "object": {
      "key": [{ "prefix": "interviews/raw/" }]
    }
  }
}
```

**차이점**: dev에서 쓰던 `suffix: .webm` 체크가 **제거**됨. `interviews/raw/` prefix는 스키마 규약상 WebM 원본만 수용하므로 suffix 필터 불필요. 이는 `s3-key-schema-redesign` Task 1의 SSOT 규격을 그대로 따른다.

**Target**: Lambda `rehearse-analysis-prod` (plan-04 산출물, alias `live` 지정) + `rehearse-convert-prod:live`

**Input Transformer**: dev `rehearse-video-uploaded-dev` 규칙을 복제 후 `-dev` → `-prod`, prefix를 `interviews/raw/`로 통일. Lambda handler는 정규식 기반 파서로 payload의 key를 파싱한다 (처리 경로는 handler 내부).

**DLQ 정책** (2계층):

1. **EventBridge Rule DLQ** — 규칙이 target Lambda를 **호출하지 못한 경우** (권한 오류, target 불가) 이벤트 저장.
   - SQS `rehearse-event-dlq-prod` 신규 생성 (FIFO 아님, 표준 큐, 메시지 보존 14일)
   - EventBridge → SQS `SendMessage` 권한: EventBridge 서비스 주체가 SQS 큐 정책에서 허용되어야 함:
     ```json
     {
       "Version": "2012-10-17",
       "Statement": [{
         "Effect": "Allow",
         "Principal": { "Service": "events.amazonaws.com" },
         "Action": "sqs:SendMessage",
         "Resource": "arn:aws:sqs:ap-northeast-2:776735194358:rehearse-event-dlq-prod",
         "Condition": {
           "ArnEquals": { "aws:SourceArn": "arn:aws:events:ap-northeast-2:776735194358:rule/rehearse-video-uploaded-prod" }
         }
       }]
     }
     ```
   - Rule target 설정 시 `DeadLetterConfig.Arn`에 SQS ARN 지정

2. **Lambda async invoke DLQ** — Lambda가 호출되었으나 **핸들러 내부에서 예외** 발생 시 (retry 소진 후) 저장. Lambda function configuration의 `DeadLetterConfig` 또는 Event Invoke Config의 On-failure Destination에 동일 SQS 또는 별도 큐 지정. plan-04에서 Lambda Role에 `sqs:SendMessage` 권한 추가 필요.

**권장**: 초기에는 큐 1개(`rehearse-event-dlq-prod`)로 통합하고, 메시지에 source(EventBridge/Lambda) 메타데이터를 attribute로 기록해 origin 구분. 운영 경험 누적 후 큐 분리 검토.

**알람**: CloudWatch 알람으로 `ApproximateNumberOfMessagesVisible > 0`을 SNS로 통지 (plan-14 연동).

### MediaConvert 재사용

MediaConvert 엔드포인트(`MEDIACONVERT_ENDPOINT`)와 IAM Role(`MEDIACONVERT_ROLE`)은 **리전 단위** 자원이므로 dev prod 공용 사용 가능. `rehearse-convert-prod` Lambda가 기존 엔드포인트로 job 제출만 하면 됨. 별도 자원 생성 없음.

**주의**: MediaConvert job이 dev 버킷과 prod 버킷을 동시에 접근할 수 있도록 Role에 두 버킷 권한이 모두 있는지 확인 (현재 Role이 dev만 가질 가능성 있음 → plan-04에서 검증·보완).

### Dev 버킷과의 관계

- `rehearse-videos-dev`는 그대로 유지 (변경 없음)
- 이벤트 규칙 `rehearse-video-uploaded-dev`도 그대로 유지
- prod 규칙 추가 시 dev 규칙 영향 0 (두 규칙은 이벤트 패턴의 bucket 이름으로 완전 분기)

## 담당 에이전트

- Implement: `devops-engineer` — AWS 콘솔/CLI 자원 생성
- Review: `architect-reviewer` — 이벤트 흐름, IAM 경계

## 검증

- `aws s3 ls s3://rehearse-videos-prod` → (빈 버킷) 성공
- `aws s3api get-bucket-cors --bucket rehearse-videos-prod` → prod 도메인만 포함
- `aws s3api get-bucket-versioning --bucket rehearse-videos-prod` → `Enabled`
- `aws s3api get-bucket-notification-configuration --bucket rehearse-videos-prod` → EventBridge Enabled
- `aws s3api get-bucket-lifecycle-configuration --bucket rehearse-videos-prod` → 5-prefix Rule 전부 존재, SSOT 규격 일치
- `aws events list-rules --name-prefix rehearse-video-uploaded-prod` → 규칙 존재
- `aws events describe-rule --name rehearse-video-uploaded-prod` → EventPattern이 `interviews/raw/` prefix만 포함, suffix 필터 없음
- 더미 파일 업로드 테스트 (신규 스키마):
  ```bash
  # v1.0 스키마 키 포맷으로 업로드
  echo "dummy" > /tmp/test.webm
  DATE=$(date -u +%Y/%m/%d)
  aws s3 cp /tmp/test.webm \
    s3://rehearse-videos-prod/interviews/raw/$DATE/1/1/000000000001.webm
  # CloudWatch Logs `/aws/lambda/rehearse-analysis-prod`에서 호출 기록 확인 (Lambda는 plan-04 완료 후)
  # 업로드 후 정리
  aws s3 rm s3://rehearse-videos-prod/interviews/raw/$DATE/1/1/000000000001.webm
  ```
- 레거시 prefix 생성 방지 확인:
  ```bash
  # 레거시 prefix로 업로드 시도 → EventBridge 무시 (Lambda 호출 0)
  aws s3 cp /tmp/test.webm s3://rehearse-videos-prod/videos/1/qs_1.webm
  # CloudWatch Logs에 호출 기록 없음 확인 (prefix 미매칭)
  aws s3 rm s3://rehearse-videos-prod/videos/1/qs_1.webm
  ```
- `progress.md` Task 3 → Completed
