#!/usr/bin/env bash
# loadtest jar 빌드(arch-neutral) → EC2 scp → arm64 JVM 구동.
# JVM heap 주입 지점 = LOADTEST_JAVA_OPTS (-Xmx768m, tech-spec 베이스라인).
#
# 기본 = plan 출력 (빌드/scp/원격구동 미실행). 실제 배포:  APPLY=true ./deploy-app.sh
# 사전: source config.env  +  EC2 running(start-ec2.sh)  +  EC2 에서 loadtest 인프라 compose 기동 가정.
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(cd "$HERE/../../../.." && pwd)"   # backend/

: "${LOADTEST_SSH_HOST:?LOADTEST_SSH_HOST 미설정(start 후 PublicIp)}"
: "${LOADTEST_SSH_USER:=ec2-user}"
: "${LOADTEST_SSH_KEY_PATH:?LOADTEST_SSH_KEY_PATH 미설정(.pem 경로)}"
: "${LOADTEST_JAVA_OPTS:=-Xmx768m -Xms768m}"

REMOTE_DIR="/home/${LOADTEST_SSH_USER}/loadtest"
JAR_GLOB="$BACKEND_DIR/build/libs/*.jar"
LOADTEST_YML="$BACKEND_DIR/src/test/resources/application-loadtest.yml"
SSH="ssh -i $LOADTEST_SSH_KEY_PATH -o StrictHostKeyChecking=accept-new ${LOADTEST_SSH_USER}@${LOADTEST_SSH_HOST}"

echo "=== 배포 plan ==="
echo "1) 빌드:  (cd $BACKEND_DIR && ./gradlew clean bootJar -x test)"
echo "         JVM 바이트코드는 arch-neutral → 빌드 머신 arch 무관, EC2 arm64 JVM 에서 구동."
echo "2) scp :  scp -i \$KEY \$JAR  ${LOADTEST_SSH_USER}@${LOADTEST_SSH_HOST}:${REMOTE_DIR}/app.jar"
echo "         scp -i \$KEY $LOADTEST_YML  …:${REMOTE_DIR}/application-loadtest.yml  (외부 config 부팅 필수키)"
echo "3) 원격 구동:"
echo "         JAVA_OPTS='$LOADTEST_JAVA_OPTS' \\"
echo "         nohup java \$JAVA_OPTS \\"
echo "           -jar ${REMOTE_DIR}/app.jar \\"
echo "           --spring.profiles.active=loadtest \\"
echo "           --spring.config.additional-location=file:${REMOTE_DIR}/ \\"
echo "           > ${REMOTE_DIR}/app.log 2>&1 &"
echo "         (loadtest 인프라 compose 는 EC2 상에서 사전 기동: mysql+wiremock localhost)"

if [[ "${APPLY:-false}" != "true" ]]; then
  echo
  echo ">>> PLAN ONLY. 빌드/scp/구동 미실행. 실제 배포:  APPLY=true $0"
  echo ">>> (사전: EC2 running + config.env 의 SSH_HOST/KEY 채움)"
  exit 0
fi

echo ">>> APPLY=true — 배포 실행"
( cd "$BACKEND_DIR" && ./gradlew clean bootJar -x test )
JAR_PATH="$(ls -t $JAR_GLOB | grep -v -- '-plain.jar' | head -1)"
echo "JAR=$JAR_PATH"
[[ -f "$LOADTEST_YML" ]] || { echo "ERROR: application-loadtest.yml 없음: $LOADTEST_YML"; exit 1; }

$SSH "mkdir -p ${REMOTE_DIR}"
scp -i "$LOADTEST_SSH_KEY_PATH" -o StrictHostKeyChecking=accept-new \
  "$JAR_PATH" "${LOADTEST_SSH_USER}@${LOADTEST_SSH_HOST}:${REMOTE_DIR}/app.jar"
scp -i "$LOADTEST_SSH_KEY_PATH" -o StrictHostKeyChecking=accept-new \
  "$LOADTEST_YML" "${LOADTEST_SSH_USER}@${LOADTEST_SSH_HOST}:${REMOTE_DIR}/application-loadtest.yml"

# 원격 구동: SSH 채널 종료 시 SIGHUP 으로 백그라운드 java 가 죽는 문제 회피.
#   launcher 스크립트를 REMOTE_DIR 에 생성 → setsid 로 세션 분리 후 nohup 실행(SSH 종료와 독립).
$SSH "cat > ${REMOTE_DIR}/run-app.sh <<'LAUNCHER'
#!/usr/bin/env bash
pkill -f 'app.jar' || true
sleep 2
cd ${REMOTE_DIR}
nohup java $LOADTEST_JAVA_OPTS \\
  -jar ${REMOTE_DIR}/app.jar \\
  --spring.profiles.active=loadtest \\
  --spring.config.additional-location=file:${REMOTE_DIR}/ \\
  > ${REMOTE_DIR}/app.log 2>&1 &
echo \"started pid=\$!\"
LAUNCHER
chmod +x ${REMOTE_DIR}/run-app.sh
rm -f ${REMOTE_DIR}/app.log
nohup setsid ${REMOTE_DIR}/run-app.sh > ${REMOTE_DIR}/launch.log 2>&1 < /dev/null &
sleep 4
cat ${REMOTE_DIR}/launch.log"
echo ">>> 구동 확인:  curl http://${LOADTEST_SSH_HOST}:8080/actuator/health"
