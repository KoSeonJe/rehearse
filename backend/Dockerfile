# Stage 1: Build
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Gradle wrapper + 설정 파일만 먼저 복사 (의존성 캐싱)
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# 소스 복사 + 빌드
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jre

WORKDIR /app

# 보안: non-root 사용자
# uid/gid를 1000으로 고정해 EC2 호스트 ubuntu(uid 1000)가 소유한
# gcp-credentials.json(chmod 600) 바인드 마운트 파일을 읽을 수 있게 한다.
# eclipse-temurin:21-jre(Ubuntu 24.04 기반)에 이미 uid/gid 1000의 기본 ubuntu 사용자가 존재하므로 먼저 제거.
RUN userdel -r ubuntu 2>/dev/null || true \
    && groupadd -g 1000 appuser \
    && useradd -u 1000 -g appuser -m appuser

COPY --from=builder /app/build/libs/*.jar app.jar

RUN chown appuser:appuser app.jar
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
