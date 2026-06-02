#!/usr/bin/env bash
# loadtest EC2 기동 (비용 발생 — 측정 시에만).
# 기본 = plan 출력. 실제 기동:  APPLY=true ./start-ec2.sh
# 사전: source config.env
set -euo pipefail
: "${AWS_REGION:?}"; : "${LOADTEST_INSTANCE_ID:?LOADTEST_INSTANCE_ID 미설정(run-instances 후 기록)}"

echo "plan: aws ec2 start-instances --region $AWS_REGION --instance-ids $LOADTEST_INSTANCE_ID"
if [[ "${APPLY:-false}" != "true" ]]; then
  echo ">>> PLAN ONLY. 실제 기동:  APPLY=true $0"
  exit 0
fi

aws ec2 start-instances --region "$AWS_REGION" --instance-ids "$LOADTEST_INSTANCE_ID"
aws ec2 wait instance-running --region "$AWS_REGION" --instance-ids "$LOADTEST_INSTANCE_ID"
PUB_IP="$(aws ec2 describe-instances --region "$AWS_REGION" --instance-ids "$LOADTEST_INSTANCE_ID" \
  --query 'Reservations[0].Instances[0].PublicIpAddress' --output text)"
echo "RUNNING. PublicIp=$PUB_IP"
echo ">>> config.env 의 LOADTEST_SSH_HOST 에 $PUB_IP 기록(인스턴스 재기동 시 IP 변동 주의)."
