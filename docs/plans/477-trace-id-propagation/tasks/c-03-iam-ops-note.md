# Task C3 — EC2 IAM 정책 + 운영 노트 + metric filter / alarm 셋업

> **PR**: PR-C
> **영역**: infra + docs
> **선행 Task**: C1, C2

---

## 변경 파일

- `docs/operations/aws-cloudwatch-traceid.md` (또는 동등 ops 경로) — **신규**. IAM 정책 / retention / metric filter / alarm CLI 절차 운영 노트.
- (AWS 콘솔 수동) EC2 IAM Role inline policy 추가
- (AWS 콘솔 수동) CloudWatch log group retention 정책 설정
- (AWS 콘솔 수동) Metric filter + Alarm 생성

---

## 핵심 로직

### IAM 정책

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:ap-northeast-2:*:log-group:/rehearse/backend/*"
    }
  ]
}
```

### 운영 명령

```bash
# IAM 정책 부착 (dev / prod EC2 Role 각각)
aws iam put-role-policy \
  --role-name {ec2-role-name} \
  --policy-name RehearseBackendCloudWatchLogs \
  --policy-document file://policy.json

# Retention 설정 (14일 기본 — 사용자 결정 가능)
aws logs put-retention-policy \
  --log-group-name /rehearse/backend/dev \
  --retention-in-days 14
aws logs put-retention-policy \
  --log-group-name /rehearse/backend/prod \
  --retention-in-days 14

# Metric filter — WARN spike 가시화
aws logs put-metric-filter \
  --log-group-name /rehearse/backend/prod \
  --filter-name MissingTraceIdHeader \
  --filter-pattern '"X-Trace-Id 헤더 부재 또는 패턴 위반"' \
  --metric-transformations \
      metricName=MissingTraceIdHeader,metricNamespace=Rehearse/TraceId,metricValue=1

# Alarm — 5분 ≥1 → SNS 알람
aws cloudwatch put-metric-alarm \
  --alarm-name RehearseMissingTraceId-prod \
  --metric-name MissingTraceIdHeader \
  --namespace Rehearse/TraceId \
  --period 300 \
  --threshold 1 \
  --evaluation-periods 1 \
  --comparison-operator GreaterThanOrEqualToThreshold \
  --alarm-actions {sns-topic-arn}
```

> **사용자 결정 필요 항목** (tech-spec.md §미확인 #3): SNS topic 존재 / 신규 생성 / 미설정 (alarm OK 만 가시화).

---

## 운영 노트 내용

`docs/operations/aws-cloudwatch-traceid.md` 에 다음 포함:

1. IAM 정책 JSON + 부착 절차
2. 로그 그룹 명명: `/rehearse/backend/{dev,prod}` + `/aws/lambda/rehearse-analysis-{dev,prod}` + `/aws/lambda/rehearse-convert-{dev,prod}`
3. Retention 정책 (현재 14일, 변경 시 절차)
4. Metric filter / Alarm 설정 명령
5. Logs Insights cross-group 쿼리 예제:
   ```
   fields @timestamp, @logStream, @message
   | filter @message like /<traceId>/
   | sort @timestamp asc
   | limit 100
   ```
6. 운영자 trace 추적 절차 (사용자 액션 → 응답 헤더 / body 의 traceId 채취 → Logs Insights 쿼리)

---

## 의존

- 선행: C1, C2
- 외부: AWS IAM / CloudWatch 권한 (사용자 사전 점검 — tech-spec.md §미확인 #2)

---

## Verification Hook

- 명령: 운영자 직접:
  - `aws logs describe-log-groups --log-group-name-prefix /rehearse/backend/` → dev + prod 그룹 존재
  - `aws logs describe-metric-filters --log-group-name /rehearse/backend/prod` → filter 등록 확인
  - `aws cloudwatch describe-alarms --alarm-names RehearseMissingTraceId-prod` → alarm OK 상태
  - 시뮬레이션: Lambda 임시 `X-Trace-Id` 누락 호출 1건 → 5분 내 alarm ALARM 전환 확인
- 통과 기준: 위 4가지 + Logs Insights cross-group 쿼리로 hop 1, 3, 4, 5 시간순 반환

---

## 커밋 메시지 (예상)

```
docs(ops): traceId CloudWatch IAM / metric filter / alarm 운영 노트
```
