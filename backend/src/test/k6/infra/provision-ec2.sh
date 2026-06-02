#!/usr/bin/env bash
# loadtest 전용 EC2(t4g.medium, arm64 AL2023) 프로비저닝. (인스턴스 타입은 config.env LOADTEST_INSTANCE_TYPE)
#
# ★ 2단계 게이트 ★
#   기본 = plan 출력만 (run-instances 미실행). 비용 발생 작업.
#   실제 생성은 사용자 명시 확인 후:  APPLY=true ./provision-ec2.sh
#
# 사전: source config.env  (AMI/SG/키페어/subnet 등 채움)
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

: "${AWS_REGION:?config.env 의 AWS_REGION 미설정}"
: "${LOADTEST_INSTANCE_TYPE:?LOADTEST_INSTANCE_TYPE 미설정}"
: "${LOADTEST_KEY_NAME:?LOADTEST_KEY_NAME 미설정 — EC2 키페어 이름(사용자 결정)}"
: "${LOADTEST_SECURITY_GROUP_ID:?LOADTEST_SECURITY_GROUP_ID 미설정 — SG ID(사용자 결정)}"
: "${LOADTEST_SUBNET_ID:?LOADTEST_SUBNET_ID 미설정 — 서브넷 ID(사용자 결정)}"
: "${LOADTEST_AMI_SSM_PARAM:?LOADTEST_AMI_SSM_PARAM 미설정}"
: "${LOADTEST_NAME_TAG:=rehearse-loadtest}"

echo "[1/2] arm64 AL2023 AMI 조회 (SSM): $LOADTEST_AMI_SSM_PARAM"
AMI_ID="$(aws ssm get-parameters \
  --region "$AWS_REGION" \
  --names "$LOADTEST_AMI_SSM_PARAM" \
  --query 'Parameters[0].Value' --output text)"
echo "    AMI_ID=$AMI_ID"

RUN_ARGS=(
  --region "$AWS_REGION"
  --image-id "$AMI_ID"
  --instance-type "$LOADTEST_INSTANCE_TYPE"
  --key-name "$LOADTEST_KEY_NAME"
  --security-group-ids "$LOADTEST_SECURITY_GROUP_ID"
  --subnet-id "$LOADTEST_SUBNET_ID"
  --associate-public-ip-address
  --user-data "file://$HERE/user-data.sh"
  --block-device-mappings '[{"DeviceName":"/dev/xvda","Ebs":{"VolumeSize":20,"VolumeType":"gp3","DeleteOnTermination":true}}]'
  --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=$LOADTEST_NAME_TAG},{Key=purpose,Value=loadtest}]"
  --count 1
)

echo "[2/2] run-instances plan:"
echo "    aws ec2 run-instances ${RUN_ARGS[*]}"

if [[ "${APPLY:-false}" != "true" ]]; then
  echo
  echo ">>> PLAN ONLY. 실제 생성 금지. 검증 위해 --dry-run 실행:"
  set +e
  aws ec2 run-instances "${RUN_ARGS[@]}" --dry-run
  rc=$?
  set -e
  echo ">>> (--dry-run 은 'DryRunOperation' 메시지면 권한·파라미터 정상. rc=$rc)"
  echo ">>> 실제 생성하려면 비용 발생 확인 후:  APPLY=true $0"
  exit 0
fi

echo ">>> APPLY=true — run-instances 실행 (비용 발생)"
INSTANCE_ID="$(aws ec2 run-instances "${RUN_ARGS[@]}" \
  --query 'Instances[0].InstanceId' --output text)"
echo "INSTANCE_ID=$INSTANCE_ID"
echo ">>> config.env 의 LOADTEST_INSTANCE_ID 에 $INSTANCE_ID 기록하세요."
