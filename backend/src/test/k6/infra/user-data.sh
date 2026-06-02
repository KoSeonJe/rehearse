#!/bin/bash
# loadtest EC2 부트스트랩 (Amazon Linux 2023, arm64).
# run-instances --user-data file://user-data.sh 로 주입. 최초 1회 실행.
set -euxo pipefail

# Docker + compose plugin 설치
dnf update -y
dnf install -y docker
systemctl enable --now docker
usermod -aG docker ec2-user

DOCKER_CLI_PLUGINS=/usr/local/lib/docker/cli-plugins
mkdir -p "$DOCKER_CLI_PLUGINS"
curl -fsSL "https://github.com/docker/compose/releases/download/v2.29.7/docker-compose-linux-aarch64" \
  -o "$DOCKER_CLI_PLUGINS/docker-compose"
chmod +x "$DOCKER_CLI_PLUGINS/docker-compose"

# Java 21 (arm64) — 호스트 JVM 으로 loadtest jar 구동
dnf install -y java-21-amazon-corretto-headless

# 작업 디렉토리
mkdir -p /home/ec2-user/loadtest
chown -R ec2-user:ec2-user /home/ec2-user/loadtest

echo "loadtest bootstrap done: docker=$(docker --version), java=$(java -version 2>&1 | head -1)"
