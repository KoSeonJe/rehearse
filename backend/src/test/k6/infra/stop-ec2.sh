#!/usr/bin/env bash
# loadtest EC2 중지 (비용 절감 — 측정 종료 후). terminate 아님(데이터/AMI 보존).
# 기본 = plan 출력. 실제 중지:  APPLY=true ./stop-ec2.sh
# 사전: source config.env
set -euo pipefail
: "${AWS_REGION:?}"; : "${LOADTEST_INSTANCE_ID:?LOADTEST_INSTANCE_ID 미설정}"

echo "plan: aws ec2 stop-instances --region $AWS_REGION --instance-ids $LOADTEST_INSTANCE_ID"
if [[ "${APPLY:-false}" != "true" ]]; then
  echo ">>> PLAN ONLY. 실제 중지:  APPLY=true $0"
  exit 0
fi

aws ec2 stop-instances --region "$AWS_REGION" --instance-ids "$LOADTEST_INSTANCE_ID"
aws ec2 wait instance-stopped --region "$AWS_REGION" --instance-ids "$LOADTEST_INSTANCE_ID"
echo "STOPPED. (EBS 비용 소액 유지. 완전 제거는 terminate — 사용자만 수행)"
