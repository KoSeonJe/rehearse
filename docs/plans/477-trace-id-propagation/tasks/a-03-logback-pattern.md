# Task A3 — logback-spring.xml 패턴 변경

> **PR**: PR-A
> **영역**: BE
> **선행 Task**: A1, A2 (MDC key 가 traceId 로 통일된 후 적용)

---

## 변경 파일

- `backend/src/main/resources/logback-spring.xml` — **변경**. 로그 패턴 `[%X{correlationId:-}]` → `[%X{traceId:-}]`.

---

## 핵심 로직

```xml
<!-- 변경 전 (line 5 근방) -->
<pattern>%d{HH:mm:ss.SSS} [%thread] %-5level [%X{correlationId:-}] %logger{36} - %msg%n</pattern>

<!-- 변경 후 -->
<pattern>%d{HH:mm:ss.SSS} [%thread] %-5level [%X{traceId:-}] %logger{36} - %msg%n</pattern>
```

> 패턴 외 다른 logback 설정 (appender, level, async) 변경 X — Surgical Changes.

---

## 테스트

별도 테스트 없음. A1 + A2 의 filter 테스트가 MDC `traceId` key 사용 보장.

회귀 grep:
```bash
grep -rn "correlationId" backend/src/main/resources backend/src/main/java
```
결과 0건 (또는 의도된 코멘트 잔존 0건).

---

## 의존

- 선행: A1, A2
- 외부: 없음

---

## Verification Hook

- 명령: `./gradlew bootRun --args='--spring.profiles.active=local'` (로컬 부트 1회)
- 통과 기준: 부팅 로그 라인에 `[<traceId or 빈값>]` 패턴 표시. `correlationId` 흔적 0건.
- 관찰 가능 동작: A1 통과 시 자동 검증됨

---

## 커밋 메시지 (예상)

```
chore(BE): logback 패턴 traceId 로 rename
```
