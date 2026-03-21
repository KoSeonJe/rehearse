# Phase 1: [BE] 기반 구조 — TechStack, DB, YAML, PersonaResolver

> **상태**: TODO
> **브랜치**: `feat/prompt-persona-foundation`
> **PR**: PR-1 → develop
> **의존**: 없음 (첫 번째 Phase)

---

## Task 1-1: TechStack enum 생성

- **Implement**: `backend`
- **Review**: `architect-reviewer` — Position 매핑, 기본 스택 로직

### 파일

- 신규: `backend/src/main/java/com/rehearse/api/domain/interview/entity/TechStack.java`

### 구현 상세

```java
package com.rehearse.api.domain.interview.entity;

public enum TechStack {
    // Backend
    JAVA_SPRING("Java/Spring Boot", Position.BACKEND, true),
    PYTHON_DJANGO("Python/Django·FastAPI", Position.BACKEND, false),
    NODE_NESTJS("Node.js/NestJS", Position.BACKEND, false),
    GO("Go", Position.BACKEND, false),
    KOTLIN_SPRING("Kotlin/Spring Boot", Position.BACKEND, false),

    // Frontend
    REACT_TS("React/TypeScript", Position.FRONTEND, true),
    VUE_TS("Vue.js/TypeScript", Position.FRONTEND, false),
    SVELTE("Svelte/SvelteKit", Position.FRONTEND, false),
    ANGULAR("Angular", Position.FRONTEND, false),

    // DevOps
    AWS_K8S("AWS/Kubernetes", Position.DEVOPS, true),
    GCP("GCP", Position.DEVOPS, false),
    AZURE("Azure", Position.DEVOPS, false),

    // Data Engineer
    SPARK_AIRFLOW("Spark/Airflow", Position.DATA_ENGINEER, true),
    FLINK("Flink", Position.DATA_ENGINEER, false),
    DBT_SNOWFLAKE("dbt/Snowflake", Position.DATA_ENGINEER, false),

    // Fullstack
    REACT_SPRING("React + Spring Boot", Position.FULLSTACK, true),
    REACT_NODE("React + Node.js", Position.FULLSTACK, false),
    NEXTJS_FULLSTACK("Next.js Fullstack", Position.FULLSTACK, false);

    private final String displayName;
    private final Position allowedPosition;
    private final boolean isDefault;

    // 생성자, getter

    public static TechStack getDefaultForPosition(Position position) {
        return Arrays.stream(values())
            .filter(ts -> ts.allowedPosition == position && ts.isDefault)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("기본 TechStack 없음: " + position));
    }

    public boolean isAllowedFor(Position position) {
        return this.allowedPosition == position;
    }
}
```

### 기본 스택 매핑

| Position | 기본 TechStack | displayName |
|----------|---------------|-------------|
| BACKEND | JAVA_SPRING | Java/Spring Boot |
| FRONTEND | REACT_TS | React/TypeScript |
| DEVOPS | AWS_K8S | AWS/Kubernetes |
| DATA_ENGINEER | SPARK_AIRFLOW | Spark/Airflow |
| FULLSTACK | REACT_SPRING | React + Spring Boot |

---

## Task 1-2: DB 마이그레이션 V9

- **Implement**: `backend`
- **Review**: `architect-reviewer` — 하위 호환성

### 파일

- 신규: `backend/src/main/resources/db/migration/V9__add_tech_stack_column.sql`

### SQL

```sql
-- V9: interview 테이블에 tech_stack 컬럼 추가
-- nullable: 기존 데이터는 NULL → getEffectiveTechStack()에서 Position 기본 스택으로 폴백
ALTER TABLE interview ADD COLUMN tech_stack VARCHAR(30) NULL;
```

> 현재 최신 마이그레이션: V8__drop_report_tables.sql

---

## Task 1-3: Interview 엔티티에 techStack 필드 추가

- **Implement**: `backend`
- **Review**: `architect-reviewer` — 폴백 로직

### 파일

- 수정: `backend/src/main/java/com/rehearse/api/domain/interview/entity/Interview.java`

### 변경 사항

1. **필드 추가** (L30 근처, position 아래):
```java
@Enumerated(EnumType.STRING)
@Column(length = 30)
private TechStack techStack;  // nullable — null이면 Position 기본 스택
```

2. **Builder 파라미터 추가** (L78):
```java
@Builder
public Interview(Position position, String positionDetail, InterviewLevel level,
                 List<InterviewType> interviewTypes, List<String> csSubTopics,
                 Integer durationMinutes, TechStack techStack) {
    // ... 기존 코드
    this.techStack = techStack;
}
```

3. **폴백 메서드 추가**:
```java
public TechStack getEffectiveTechStack() {
    return techStack != null ? techStack : TechStack.getDefaultForPosition(this.position);
}
```

4. **Validation**: techStack이 null이 아닌 경우 position과 호환되는지 검증은 Service 레이어에서 수행 (엔티티는 단순 저장)

---

## Task 1-4: YAML 프로필 구조 생성

- **Implement**: `backend`
- **Review**: `architect-reviewer` — merge 규칙 정합성, 프롬프트 품질

### 디렉토리 구조

```
backend/src/main/resources/prompts/
├── base/
│   ├── backend.yaml
│   ├── frontend.yaml
│   ├── devops.yaml
│   ├── data-engineer.yaml
│   └── fullstack.yaml
├── overlay/
│   ├── backend/
│   │   └── java-spring.yaml       ← MVP (Phase 1)
│   ├── frontend/
│   │   └── react-ts.yaml          ← MVP
│   ├── devops/
│   │   └── aws-k8s.yaml           ← MVP
│   ├── data-engineer/
│   │   └── spark-airflow.yaml     ← MVP
│   └── fullstack/
│       └── react-spring.yaml      ← MVP
└── template/
    ├── question-generation.txt
    ├── follow-up.txt
    ├── verbal-analysis.txt
    └── nonverbal-analysis.txt
```

### Base YAML 스키마 (예: backend.yaml)

```yaml
# 출처: docs/plans/prompt-redesign/background/prompt-redesign.md §3.1
persona_block: |
  당신은 한국 IT 기업에서 10년 이상 경력의 백엔드 시니어 개발자 면접관입니다.
  서버 사이드 아키텍처 설계, 대규모 트래픽 처리, 데이터 정합성 보장에 대한 깊은 이해를 가지고 있습니다.
  기술 스택에 관계없이 다음 역량을 중요하게 평가합니다:
  - API 설계의 일관성과 확장성
  - 동시성 제어와 데이터 정합성 보장 전략
  - 장애 대응 경험과 운영 안정성에 대한 감각
  - 성능 병목을 진단하고 해결하는 체계적 접근

evaluation_perspective: |
  - 코드 레벨: 동시성 제어, 트랜잭션 관리, 예외 처리 전략, 테스트 작성 습관
  - 아키텍처 레벨: API 설계 원칙, 서비스 간 통신, 데이터 일관성 전략
  - 운영 레벨: 장애 대응 경험, 성능 병목 진단, 모니터링/로깅 전략
  - 성장 레벨: 기술 선택의 근거, 레거시 개선 경험, 코드 리뷰 문화

follow_up_depth: |
  후속 질문에서 다음 방향으로 깊이를 추구하세요:
  - 동시성/스레드 안전성 → 실제 장애 사례, 해결 방법
  - DB 쿼리 → 실행 계획 분석 경험, 인덱스 설계 판단 근거
  - API 설계 → 버전 관리, 하위 호환성, 에러 응답 규격
  - 캐시 → 캐시 무효화 전략, 일관성 유지
```

### Overlay YAML 스키마 (예: java-spring.yaml)

```yaml
# 출처: docs/plans/prompt-redesign/background/prompt-redesign.md §4.1
# + prompt-optimized.md §7.6
persona:
  full: |
    특히 Java/Kotlin 언어와 Spring Boot 에코시스템에 깊은 전문성을 가지고 있습니다.
    JVM 내부 동작, Spring IoC/AOP/트랜잭션 관리, JPA/Hibernate ORM에 대한
    실무 경험이 풍부하며, Spring Security, Spring Cloud 기반 MSA 설계/운영 경험이 있습니다.
  medium: "백엔드(Java/Spring) 시니어 면접관. API 설계, 동시성, 장애 대응, JVM/JPA 심화를 중시합니다."
  minimal: "백엔드(Java/Spring) 면접 답변의 언어적 커뮤니케이션을 분석합니다."

interview_type_guide:
  CS_FUNDAMENTAL: "OS, 네트워크, 자료구조. Java 관점 실무 연결. (예: TCP handshake → HikariCP 커넥션 풀)"
  BEHAVIORAL: "장애 대응, 코드 리뷰, 기술 부채 등 Java/Spring 팀 맥락 경험 질문"
  RESUME_BASED: "이력서의 Spring 프로젝트, JPA, 성능 개선 수치 기반 질문"
  JAVA_SPRING: "JVM 메모리/GC, Spring IoC/AOP, @Transactional 전파, JPA N+1, 동시성 제어"
  SYSTEM_DESIGN: "Java/Spring 기반 대규모 설계. Spring Cloud Gateway, Resilience4j, Kafka 맥락"

follow_up_depth_append: |
  Java/Spring 심화:
  - JVM → GC 로그, 힙 덤프, 메모리 릭
  - 트랜잭션 → 전파 속성 실수, Saga 패턴
  - JPA → N+1 전략 비교, 벌크 연산, 2차 캐시
  - 동시성 → synchronized vs Lock, @Version

verbal_expertise: |
  Java/Spring 답변 분석 기준:
  - JVM, Spring, JPA 기술 용어의 정확한 사용
  - 성능 수치(TPS, 응답시간, GC pause time) 구체적 언급
  - "원인→해결→결과" 구조 설명 능력

  키워드 사전:
  JVM, GC, 힙 메모리, 메타스페이스, 스레드 풀, HikariCP, 커넥션 풀,
  트랜잭션 격리 수준, @Transactional, 전파 속성, 롤백,
  영속성 컨텍스트, 1차 캐시, 지연 로딩, 즉시 로딩, N+1, fetch join, EntityGraph,
  Spring IoC, 빈 스코프, AOP, 프록시, CGLIB,
  @Version, 낙관적 락, 비관적 락, 데드락,
  Spring Cloud, 서킷 브레이커, Resilience4j, Spring Kafka
```

> 나머지 4개 base + 4개 overlay YAML은 `background/prompt-redesign.md` §3.2~3.5, §4.4~4.5 참조하여 동일 구조로 작성

---

## Task 1-5: PersonaResolver + 관련 레코드

- **Implement**: `backend`
- **Review**: `architect-reviewer` — SOLID, merge 로직, YAML 캐싱

### 파일

- 신규: `backend/src/main/java/com/rehearse/api/infra/ai/persona/BaseProfile.java`
- 신규: `backend/src/main/java/com/rehearse/api/infra/ai/persona/StackOverlay.java`
- 신규: `backend/src/main/java/com/rehearse/api/infra/ai/persona/ResolvedProfile.java`
- 신규: `backend/src/main/java/com/rehearse/api/infra/ai/persona/PersonaDepth.java`
- 신규: `backend/src/main/java/com/rehearse/api/infra/ai/persona/PersonaResolver.java`
- 신규: `backend/src/main/java/com/rehearse/api/infra/ai/persona/ProfileYamlLoader.java`

### BaseProfile (record)

```java
public record BaseProfile(
    String personaBlock,
    String evaluationPerspective,
    String followUpDepth
) {}
```

### StackOverlay (record)

```java
public record StackOverlay(
    String fullPersona,       // APPEND용
    String mediumPersona,     // 축약 페르소나
    String minimalPersona,    // 최소 페르소나
    Map<String, String> interviewTypeGuideMap,  // REPLACE용 (유형→가이드)
    String followUpDepthAppend,  // APPEND용
    String verbalExpertise       // REPLACE용
) {}
```

### ResolvedProfile (record)

```java
public record ResolvedProfile(
    String fullPersona,           // base.persona + overlay.fullPersona
    String mediumPersona,         // overlay.mediumPersona
    String minimalPersona,        // overlay.minimalPersona
    String evaluationPerspective, // base.evaluationPerspective (KEEP)
    Map<String, String> interviewTypeGuideMap,  // overlay 교체
    String followUpDepth,         // base + overlay append
    String verbalExpertise        // overlay 교체
) {
    public String getPersona(PersonaDepth depth) {
        return switch (depth) {
            case FULL -> fullPersona;
            case MEDIUM -> mediumPersona;
            case MINIMAL -> minimalPersona;
        };
    }

    /**
     * overlay가 없을 때 base만으로 프로필 생성.
     * mediumPersona/minimalPersona는 base의 첫 문장 축약으로 생성.
     */
    public static ResolvedProfile fromBaseOnly(BaseProfile base) {
        String firstSentence = base.personaBlock().split("\n")[0];
        return new ResolvedProfile(
            base.personaBlock(),                  // fullPersona = base만
            firstSentence,                         // mediumPersona = 첫 문장
            firstSentence.substring(0, Math.min(firstSentence.length(), 50)) + " 면접 답변을 분석합니다.",
            base.evaluationPerspective(),
            Map.of(),                              // interviewTypeGuideMap = 빈 Map (base에는 없음)
            base.followUpDepth(),
            ""                                     // verbalExpertise = 없음
        );
    }
}
```

### PersonaDepth (enum)

```java
public enum PersonaDepth {
    FULL,     // 질문 생성용
    MEDIUM,   // 후속 질문용
    MINIMAL   // 언어 분석용
}
```

### PersonaResolver (@Component)

```java
@Component
public class PersonaResolver {

    private final Map<Position, BaseProfile> baseProfiles;
    private final Map<TechStack, StackOverlay> stackOverlays;

    public PersonaResolver(ProfileYamlLoader loader) {
        this.baseProfiles = loader.loadBaseProfiles();     // 5개
        this.stackOverlays = loader.loadStackOverlays();   // 5개 (MVP)
    }

    public ResolvedProfile resolve(Position position, TechStack techStack) {
        BaseProfile base = baseProfiles.get(position);

        // [M3] base profile null 방어 — YAML 로딩 실패 시 NPE 방지
        if (base == null) {
            log.warn("Base profile 없음: position={}, 기본 fallback 사용", position);
            base = DEFAULT_BASE_PROFILE; // 하드코딩된 최소 프로필 (상수)
        }

        StackOverlay overlay = stackOverlays.get(techStack);

        if (overlay == null) {
            // overlay 없으면 base만으로 동작 (확장 시 overlay 추가 전까지)
            return ResolvedProfile.fromBaseOnly(base);
        }

        return new ResolvedProfile(
            base.personaBlock() + "\n" + overlay.fullPersona(),   // APPEND
            overlay.mediumPersona(),
            overlay.minimalPersona(),
            base.evaluationPerspective(),                          // KEEP
            overlay.interviewTypeGuideMap(),                        // REPLACE
            base.followUpDepth() + "\n" + overlay.followUpDepthAppend(), // APPEND
            overlay.verbalExpertise()                               // REPLACE
        );
    }

    // YAML 로딩 실패 시 최소 동작 보장용
    private static final BaseProfile DEFAULT_BASE_PROFILE = new BaseProfile(
        "당신은 한국 IT 기업의 시니어 개발자 면접관입니다.",
        "- 기술적 정확성\n- 논리적 사고력\n- 실무 적용 능력",
        "후속 질문에서 기술적 깊이를 추구하세요."
    );
}
```

### ProfileYamlLoader

- `resources/prompts/base/*.yaml` → `Map<Position, BaseProfile>`
- `resources/prompts/overlay/**/*.yaml` → `Map<TechStack, StackOverlay>`
- SnakeYAML 또는 Jackson YAML 사용 (Spring Boot에 포함)
- 시작 시 1회 로드, 캐싱 (immutable Map)

---

## Merge 규칙 요약 (참조: prompt-redesign.md §1.3)

| 프로필 필드 | Merge 전략 | 이유 |
|------------|-----------|------|
| personaBlock | APPEND (base + overlay) | 공통 역량 유지 + 스택 전문성 추가 |
| evaluationPerspective | KEEP (base) | 직무 공통 관점 |
| interviewTypeGuide | REPLACE (overlay) | 스택별 기술 심화 필요 |
| followUpDepth | APPEND (base + overlay) | 공통 방향 유지 + 스택별 심화 추가 |
| verbalExpertise | REPLACE (overlay) | 키워드 사전이 스택마다 완전히 다름 |

---

## 검증

### 단위 테스트

- 신규: `backend/src/test/java/com/rehearse/api/infra/ai/persona/PersonaResolverTest.java`
  - 5 Position × 기본 TechStack merge 정확성
  - overlay null → base only 폴백
  - `getPersona(FULL/MEDIUM/MINIMAL)` 반환값 검증

- 신규: `backend/src/test/java/com/rehearse/api/domain/interview/entity/TechStackTest.java`
  - `getDefaultForPosition()` 5개 Position 각각 테스트
  - `isAllowedFor()` 호환/비호환 케이스

- 수정: `backend/src/test/java/com/rehearse/api/domain/interview/entity/InterviewTest.java`
  - `getEffectiveTechStack()` — techStack=null → 기본 스택
  - `getEffectiveTechStack()` — techStack=PYTHON_DJANGO → 그대로 반환

### DB 마이그레이션 검증

```bash
# H2 개발 환경에서 Flyway 마이그레이션 실행
cd backend && ./gradlew bootRun
# 또는
cd backend && ./gradlew test  # 테스트 시 자동 마이그레이션
```

### YAML 로딩 검증

- ProfileYamlLoader 테스트: 10개 YAML 파일 로딩 성공 확인
- 잘못된 YAML 형식 시 적절한 예외 발생 확인

---

## 병렬 가능

- Task 1-1 (enum + DB + entity) ↔ Task 1-4 (YAML 파일 작성): **독립적, 병렬 가능**
- Task 1-5 (PersonaResolver): Task 1-1 + Task 1-4 완료 후 진행
