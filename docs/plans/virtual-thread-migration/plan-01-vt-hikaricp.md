# Plan 01: Virtual Thread 활성화 + HikariCP 업그레이드

> 상태: Draft
> 작성일: 2026-03-23

## Why

Spring Boot 3.4.3 번들 HikariCP 5.x는 내부에 `synchronized` 블록이 있어 Virtual Thread가 carrier thread에 고정(pinning)된다. VT의 이점을 살리려면 HikariCP 6.x(`ReentrantLock` 전환)로 업그레이드하고, VT를 활성화해야 한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/build.gradle.kts` | HikariCP 6.2.1 의존성 추가, bootRun pinning 진단 JVM 옵션 |
| `backend/src/main/resources/application-prod.yml` | `spring.threads.virtual.enabled=true`, HikariCP 풀 설정 |

## 상세

### build.gradle.kts

```kotlin
// dependencies 블록에 추가
implementation("com.zaxxer:HikariCP:6.2.1")

// 파일 끝에 추가
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    jvmArgs("-Djdk.tracePinnedThreads=short")
}
```

### application-prod.yml

기존 설정 유지하면서 아래 항목 추가:

```yaml
spring:
  threads:
    virtual:
      enabled: true
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 10
      connection-timeout: 3000
      max-lifetime: 1200000
      leak-detection-threshold: 30000
```

## 담당 에이전트

- Implement: `backend` — 의존성 + 설정 변경
- Review: `architect-reviewer` — pinning 방지 검증, HikariCP 호환성

## 검증

- `./gradlew dependencies | grep -i hikari` → 6.2.1 확인
- `application-prod.yml`에 `spring.threads.virtual.enabled: true` 존재
- bootRun 실행 시 pinning 경고 미발생 확인
- `progress.md` 상태 업데이트 (Task 1 → Completed)
