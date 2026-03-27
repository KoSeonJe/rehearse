# Plan 01: VT Executor 정의 + 글로벌 VT 비활성화

> 상태: Draft
> 작성일: 2026-03-27

## Why

Tomcat 전체를 VT로 돌릴 필요 없이, `CompletableFuture`로 명시적 VT executor에 작업을 제출한다. 글로벌 `enabled=true`를 끄고 VT executor Bean을 정의하여 사용처를 코드 레벨에서 추적 가능하게 한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/main/java/com/rehearse/api/global/config/AsyncConfig.java` | VT executor Bean 정의, `@EnableAsync` 유지 |
| `backend/src/main/resources/application.yml` | `spring.threads.virtual.enabled: false` |

## 상세

### AsyncConfig.java

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String VT_EXECUTOR = "vtExecutor";

    @Bean(VT_EXECUTOR)
    public Executor vtExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
```

- `Executors.newVirtualThreadPerTaskExecutor()`: 작업마다 새 VT 생성, 풀 크기 제한 없음
- Bean 이름 상수화 (`VT_EXECUTOR`)로 타이포 방지
- `@EnableAsync` 유지 — 향후 `@Async` 필요 시 사용 가능

### application.yml

```yaml
spring:
  threads:
    virtual:
      enabled: false  # Tomcat은 PT 유지, VT는 명시적 executor로만 사용
```

## 담당 에이전트

- Implement: `backend` — 설정 변경 + Bean 정의
- Review: `architect-reviewer` — 스레드 모델 전환 일관성 검증

## 검증

- 애플리케이션 정상 기동 확인
- `vtExecutor` Bean 등록 확인
- Tomcat이 PT로 동작하는지 확인 (`Thread.currentThread().isVirtual() == false`)
- `progress.md` 상태 업데이트 (Task 1 → Completed)
