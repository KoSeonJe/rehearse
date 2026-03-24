# Virtual Thread Migration — 요구사항 정의

> 상태: Draft
> 작성일: 2026-03-23

## Why

### 1. Why? — 어떤 문제를 해결하는가?

`InterviewService.generateFollowUp()`이 `@Transactional` 안에서 외부 API(Whisper STT + Claude)를 호출하여 DB 커넥션을 **60~90초** 점유한다. HikariCP 기본 풀 크기(10)에서 동시 사용자 10명만 되어도 커넥션 풀 고갈 → 전체 서비스 장애로 이어진다.

또한 현재 Platform Thread 기반 Tomcat(기본 200 스레드)은 외부 API 대기 중 OS 스레드를 블로킹하므로, 동시 요청 200개를 초과하면 스레드 풀 포화가 발생한다.

### 2. Goal — 구체적인 결과물과 성공 기준

| 지표 | 현재 | 목표 |
|------|------|------|
| DB 커넥션 점유 시간 (generateFollowUp) | ~90초 | ~40ms |
| 동시 처리 가능 요청 수 | ~200 (Tomcat 스레드 제한) | 1000+ (VT) |
| 외부 API 동시성 제어 | 없음 (무제한) | Claude 20 req/s, Whisper 10 req/s |
| WhisperService timeout | 없음 (무한 대기) | connect 5s, read 60s |

### 3. Evidence — 근거 데이터

- `InterviewService.java:149-207` — `@Transactional` 안에서 `sttService.transcribe()` (line 159) + `aiClient.generateFollowUpQuestion()` (line 184) 호출 확인
- `QuestionGenerationService.java` — 이미 Phase A/B/C 트랜잭션 분리 패턴 적용 완료 (동일 패턴 재사용 가능)
- HikariCP 5.x `synchronized` 블록 → Virtual Thread pinning 발생 ([HikariCP #2111](https://github.com/brettwooldridge/HikariCP/issues/2111)), 6.0+에서 `ReentrantLock` 전환
- `WhisperService.java:32` — `new RestTemplate()` timeout 미설정

### 4. Trade-offs — 포기하는 것과 고려한 대안

| 선택 | 대안 | 대안 제외 이유 |
|------|------|----------------|
| Virtual Thread | WebClient/Reactive | Reactive 전환은 전체 코드 리라이트 필요, 학습 곡선 높음 |
| HikariCP 6.2.1 수동 지정 | Spring Boot 번들 버전 유지 | 5.x는 VT pinning 발생, 근본적 해결 불가 |
| Resilience4j RateLimiter | 스레드 풀 크기로 암묵적 제한 | VT 환경에서 스레드 풀 제한 무의미 |
| 트랜잭션 분리 (별도 Bean) | self-injection | self-injection은 안티패턴, 테스트 어려움 |

## 아키텍처

### 변경 전
```
[HTTP 요청] → [Tomcat Platform Thread]
  → @Transactional 시작 (DB 커넥션 획득)
    → DB 조회 (~20ms)
    → Whisper STT API 호출 (~30s) ← 커넥션 점유 중
    → Claude API 호출 (~30s)      ← 커넥션 점유 중
    → DB 저장 (~20ms)
  → @Transactional 종료 (DB 커넥션 반환)
  // 총 커넥션 점유: ~60초
```

### 변경 후
```
[HTTP 요청] → [Virtual Thread]
  → Whisper STT API 호출 (~30s)  ← 트랜잭션 없음, 커넥션 미사용
  → @Transactional(readOnly) 시작
    → DB 조회 + 검증 (~20ms)
  → @Transactional 종료
  → Claude API 호출 (~30s)       ← 트랜잭션 없음, 커넥션 미사용
  → @Transactional 시작
    → DB 저장 (~20ms)
  → @Transactional 종료
  // 총 커넥션 점유: ~40ms
```

## Scope

### In
- Virtual Thread 활성화 (`spring.threads.virtual.enabled=true`)
- HikariCP 6.2.1 업그레이드 (pinning 방지)
- `generateFollowUp()` 트랜잭션 범위 분리
- AsyncConfig 커스텀 executor 제거 (VT executor 전환)
- Resilience4j RateLimiter (외부 API 동시성 제어)
- WhisperService RestTemplate timeout 설정
- 단위/통합 테스트
- 부하테스트 (TX 분리 전후 + VT 전후 비교)
- 부하테스트 결과 문서화

### Out
- WebClient/Reactive 전환
- 분산 트레이싱 (Micrometer Tracing)
- 운영 환경 배포
- 질문 생성 서비스 트랜잭션 분리 (이미 완료)

## 제약조건

- Java 21, Spring Boot 3.4.3, Gradle Kotlin DSL (`build.gradle.kts`)
- `@Transactional` 프록시 기반 → 같은 클래스 내부 호출 시 트랜잭션 미적용 → 별도 Bean 분리 필수
- `InterviewService` 클래스 레벨 `@Transactional(readOnly = true)` 존재
- `ClaudeApiClient`는 `AiClient` 인터페이스 구현 → `@RateLimiter`는 구현 클래스에 적용
- `WhisperService`는 `@ConditionalOnExpression`으로 조건부 로드
