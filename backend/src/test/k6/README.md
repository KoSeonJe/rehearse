# 부하테스트 실행 가이드

## 사전 준비

```bash
# 1. Docker MySQL 시작
docker run -d --name mysql-loadtest \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=loadtest \
  -e MYSQL_DATABASE=rehearse_loadtest \
  mysql:8.0

# MySQL ready 대기 (~30초)
until docker exec mysql-loadtest mysqladmin ping -h localhost -u root -ploadtest 2>/dev/null; do sleep 2; done

# 2. Mock API 서버 시작 (Claude 2초, Whisper 1초 지연)
python3 backend/src/test/k6/mock-server.py &

# 3. Backend 시작 (loadtest 프로필)
cd backend
SPRING_PROFILES_ACTIVE=loadtest ./gradlew bootRun \
  --args='--spring.config.additional-location=file:src/test/resources/'
```

## 테스트 데이터 생성

Backend 시작 후, 테스트용 면접 + 질문세트 생성:

```bash
# 면접 생성
curl -s -X POST http://localhost:8080/api/v1/interviews \
  -H "Content-Type: application/json" \
  -d '{"position":"BACKEND","level":"JUNIOR","interviewTypes":["LANGUAGE_FRAMEWORK"],"durationMinutes":30}' | jq .

# 질문 생성 완료 대기 후 면접 ID/질문세트 ID 확인
curl -s http://localhost:8080/api/v1/interviews/{ID} | jq '.data.questionSets[0].id'
```

## 시나리오 실행

### 시나리오 A: TX 분리 효과 (동시 10/20/50명)
```bash
k6 run --env SCENARIO=A \
       --env BASE_URL=http://localhost:8080 \
       --env INTERVIEW_ID=1 \
       --env QUESTION_SET_ID=1 \
       backend/src/test/k6/follow-up-load-test.js
```

### 시나리오 B: VT 도입 효과 (Ramp 10→200명)

**Platform Thread (비교군)**:
```bash
# application-loadtest.yml에서 VIRTUAL_THREAD_ENABLED=false로 설정 후 Backend 재시작
VIRTUAL_THREAD_ENABLED=false SPRING_PROFILES_ACTIVE=loadtest ./gradlew bootRun \
  --args='--spring.config.additional-location=file:src/test/resources/'

k6 run --env SCENARIO=B \
       --env BASE_URL=http://localhost:8080 \
       --env INTERVIEW_ID=1 \
       --env QUESTION_SET_ID=1 \
       --out json=results-platform-thread.json \
       backend/src/test/k6/follow-up-load-test.js
```

**Virtual Thread (실험군)**:
```bash
# Backend 재시작 (VT 기본 활성화)
SPRING_PROFILES_ACTIVE=loadtest ./gradlew bootRun \
  --args='--spring.config.additional-location=file:src/test/resources/'

k6 run --env SCENARIO=B \
       --env BASE_URL=http://localhost:8080 \
       --env INTERVIEW_ID=1 \
       --env QUESTION_SET_ID=1 \
       --out json=results-virtual-thread.json \
       backend/src/test/k6/follow-up-load-test.js
```

### 시나리오 C: RateLimiter 스트레스 (50 req/s)
```bash
k6 run --env SCENARIO=C \
       --env BASE_URL=http://localhost:8080 \
       --env INTERVIEW_ID=1 \
       --env QUESTION_SET_ID=1 \
       backend/src/test/k6/follow-up-load-test.js
```

## Actuator 메트릭 조회

```bash
# HikariCP 활성 커넥션
curl -s http://localhost:8080/actuator/metrics/hikaricp.connections.active | jq .

# JVM 스레드 수
curl -s http://localhost:8080/actuator/metrics/jvm.threads.live | jq .

# Resilience4j RateLimiter
curl -s http://localhost:8080/actuator/metrics/resilience4j.ratelimiter.available.permissions | jq .
```

## 정리

```bash
docker rm -f mysql-loadtest
pkill -f mock-server.py
```
