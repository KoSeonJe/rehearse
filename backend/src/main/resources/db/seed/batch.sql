-- ================================================================
-- 그룹 A 통합 추가 시드 데이터
-- 16~25회차 통합 파일
-- ================================================================

-- ============================================================
-- 16회차: backend-kotlin-spring 추가분 (+30, 레벨당 10)
-- cache_key: BACKEND:{L}:KOTLIN_SPRING:LANGUAGE_FRAMEWORK
-- ============================================================

-- JUNIOR (10문항)







INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin과 Java의 상호운용성은 어떻게 보장되나요?', '코틀린과 자바의 상호운용성은 어떻게 보장되나요?', 'KOTLIN_CORE',
 'Kotlin은 JVM 바이트코드로 컴파일되므로 Java와 100퍼센트 상호운용이 가능합니다. Kotlin 코드에서 Java 라이브러리를 직접 호출할 수 있고, 반대로 Java 코드에서 Kotlin 클래스를 사용할 수도 있습니다. 다만 몇 가지 주의점이 있는데, Kotlin의 nullable 타입은 Java에서 플랫폼 타입으로 취급되어 null 안전성이 보장되지 않습니다. JvmStatic, JvmField, JvmOverloads 같은 어노테이션을 사용하면 Java에서 Kotlin 코드를 더 자연스럽게 사용할 수 있습니다. Spring Framework은 Kotlin을 공식 지원하며, 생성자 주입, 확장 함수 기반 DSL, 코루틴 지원 등 Kotlin에 최적화된 기능을 제공합니다. 기존 Java Spring 프로젝트에 Kotlin을 점진적으로 도입하는 것도 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 sealed class란 무엇이고 어떻게 활용하나요?', '코틀린에서 실드 클래스란 무엇이고 어떻게 활용하나요?', 'KOTLIN_CORE',
 'sealed class는 상속할 수 있는 하위 클래스를 같은 파일 내로 제한하는 추상 클래스입니다. 이를 통해 타입 계층을 닫힌 집합으로 정의할 수 있어서, when 표현식에서 모든 경우를 완전하게 처리했는지 컴파일러가 검증합니다. 예를 들어 API 호출 결과를 Success, Error, Loading 같은 sealed class로 정의하면, 상태 처리 시 else 분기 없이 모든 경우를 명시적으로 다룰 수 있습니다. 각 하위 클래스는 서로 다른 프로퍼티를 가질 수 있어서, Success에는 데이터를, Error에는 에러 메시지를 포함시킬 수 있습니다. 도메인 이벤트, 커맨드 패턴, 에러 처리 등에서 매우 유용하며, enum과 달리 각 경우가 서로 다른 상태를 가질 수 있다는 점이 강력합니다.',
 'MODEL_ANSWER', TRUE, NOW());


-- MID (10문항)

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin의 Flow와 Java의 Reactive Streams의 차이를 설명해주세요.', '코틀린의 플로우와 자바의 리액티브 스트림즈의 차이를 설명해 주세요.', 'COROUTINE',
 'Flow는 Kotlin 코루틴 기반의 비동기 스트림 처리 API로, 콜드 스트림입니다. 수집이 시작될 때만 데이터를 생산하며, suspend 함수를 기반으로 하여 일반적인 코루틴 코드처럼 순차적으로 작성할 수 있습니다. Reactive Streams의 Publisher나 Reactor의 Flux와 유사한 역할을 하지만, 별도의 연산자 체인 없이 일반 제어 흐름 구문을 사용할 수 있어서 학습 곡선이 낮습니다. Spring WebFlux에서 코루틴을 사용하면 Mono는 suspend 함수로, Flux는 Flow로 자연스럽게 매핑됩니다. SharedFlow와 StateFlow는 핫 스트림으로 여러 수집자에게 데이터를 브로드캐스트할 수 있습니다. 배압은 Flow의 collect가 suspend 함수이므로 자연스럽게 처리되어, 별도의 배압 전략을 설정할 필요가 없습니다.',
 'MODEL_ANSWER', TRUE, NOW());


INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin Spring에서 생성자 주입과 lateinit의 트레이드오프를 설명해주세요.', '코틀린 스프링에서 생성자 주입과 레이트이닛의 트레이드오프를 설명해 주세요.', 'KOTLIN_SPRING',
 'Kotlin에서 Spring 빈 주입은 주로 생성자 주입을 사용하며, val 프로퍼티로 선언하여 불변성을 보장합니다. 클래스에 생성자가 하나면 Autowired를 생략할 수 있어서 매우 간결합니다. lateinit var는 필드 주입에 사용되며, 선언 시점에 초기화하지 않고 나중에 주입됩니다. 생성자 주입의 장점은 불변성 보장, 필수 의존성 명시, 순환 참조 조기 발견, 테스트 시 생성자로 직접 주입 가능 등입니다. lateinit의 장점은 코드가 짧고, 순환 참조가 있을 때 임시 해결책이 되며, 테스트에서 특정 필드만 교체하기 쉽습니다. 그러나 lateinit는 초기화 전 접근 시 UninitializedPropertyAccessException이 발생하고, nullable이 아닌데 null 안전성을 컴파일러가 보장하지 못합니다. 실무에서는 생성자 주입을 기본으로 사용하는 것이 권장됩니다.',
 'MODEL_ANSWER', TRUE, NOW());




INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin Spring에서 R2DBC를 활용한 논블로킹 데이터 접근 방법을 설명해주세요.', '코틀린 스프링에서 알투디비씨를 활용한 논블로킹 데이터 접근 방법을 설명해 주세요.', 'KOTLIN_SPRING',
 'R2DBC는 관계형 데이터베이스에 대한 리액티브 논블로킹 접근을 제공하는 API입니다. Spring Data R2DBC와 Kotlin 코루틴을 결합하면 suspend 함수 기반의 논블로킹 데이터베이스 접근이 가능합니다. 리포지토리 인터페이스에서 반환 타입을 Flow로 선언하면 비동기 스트림으로 데이터를 처리하고, suspend 함수로 선언하면 단건 조회를 논블로킹으로 처리합니다. JPA와 달리 영속성 컨텍스트가 없으므로 엔티티 상태 관리를 직접 해야 하며, 지연 로딩도 지원되지 않습니다. 복잡한 쿼리는 DatabaseClient를 사용하여 직접 SQL을 작성합니다. R2DBC는 IO 바운드 작업이 많은 서비스에서 스레드 효율을 크게 높여주지만, 학습 곡선이 있고 JPA에 비해 기능이 제한적이므로 적합한 시나리오를 잘 판단해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());


INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 코루틴의 동시성 제어를 위한 Mutex와 Semaphore 사용법을 설명해주세요.', '코틀린에서 코루틴의 동시성 제어를 위한 뮤텍스와 세마포어 사용법을 설명해 주세요.', 'COROUTINE',
 '코루틴 환경에서는 기존 Java의 synchronized나 ReentrantLock이 스레드를 블로킹하므로 적합하지 않습니다. 대신 kotlinx.coroutines.sync 패키지의 Mutex를 사용하면 코루틴을 일시 중지시키는 방식으로 상호 배제를 구현합니다. Mutex의 withLock 확장 함수로 임계 구역을 안전하게 보호할 수 있습니다. Semaphore는 동시에 실행될 수 있는 코루틴의 수를 제한하여, 외부 API 호출이나 데이터베이스 접근의 동시성을 제어합니다. Channel을 사용하면 생산자-소비자 패턴으로 데이터를 안전하게 전달할 수 있고, actor 패턴으로 상태를 캡슐화하여 동시성 문제를 원천적으로 방지할 수 있습니다. 각 도구의 특성을 이해하고 상황에 맞게 선택하는 것이 중요합니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- SENIOR (10문항)
INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin 코루틴 기반의 고성능 마이크로서비스를 설계할 때 고려사항을 설명해주세요.', '코틀린 코루틴 기반의 고성능 마이크로서비스를 설계할 때 고려사항을 설명해 주세요.', 'COROUTINE',
 '코루틴 기반 마이크로서비스 설계의 핵심은 논블로킹 스택의 일관성입니다. 하나의 블로킹 호출이 전체 이벤트 루프를 막을 수 있으므로, 데이터베이스는 R2DBC, HTTP 클라이언트는 WebClient, 캐시는 Lettuce의 리액티브 API를 사용해야 합니다. 불가피한 블로킹 호출은 Dispatchers.IO에서 실행하여 이벤트 루프를 보호합니다. 구조적 동시성을 활용하여 요청 처리 중 생성된 모든 코루틴이 요청 완료 시 정리되도록 보장하고, supervisorScope로 독립적인 작업의 실패 격리를 구현합니다. 디스패처 풀 크기를 모니터링하여 병목을 탐지하고, 코루틴 디버깅을 위해 코루틴 이름을 지정하고 MDC 컨텍스트를 전파합니다. 부하 테스트로 코루틴 환경의 처리량과 지연 시간 특성을 파악하고, 기존 스레드 풀 모델과 비교하여 실질적 이점을 검증해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Java에서 Kotlin으로 대규모 Spring 프로젝트를 점진적으로 마이그레이션하는 전략을 설명해주세요.', '자바에서 코틀린으로 대규모 스프링 프로젝트를 점진적으로 마이그레이션하는 전략을 설명해 주세요.', 'KOTLIN_SPRING',
 '대규모 프로젝트의 Kotlin 마이그레이션은 점진적 접근이 필수입니다. 첫째 단계로 빌드 시스템에 Kotlin 플러그인을 추가하고, 새로운 코드부터 Kotlin으로 작성합니다. Java와 Kotlin이 같은 모듈에서 공존할 수 있으므로 기존 코드를 건드리지 않아도 됩니다. 둘째, 테스트 코드를 먼저 Kotlin으로 전환하면 위험 부담 없이 팀이 언어에 익숙해질 수 있습니다. 셋째, DTO와 유틸리티부터 변환하여 data class와 확장 함수의 이점을 빠르게 체감합니다. IntelliJ의 Java to Kotlin 변환기를 활용하되, 변환 후 Kotlin 관용적 코드로 정리하는 과정이 필요합니다. 주의할 점으로 Kotlin의 null 안전성이 Java 경계에서 깨질 수 있으므로, Java에서 넘어오는 값에 적절한 null 처리를 추가해야 합니다. 팀 전체의 Kotlin 역량 수준을 고려하여 마이그레이션 속도를 조절하고, 코드 리뷰에서 Kotlin 관용구를 점진적으로 도입합니다.',
 'MODEL_ANSWER', TRUE, NOW());









-- ============================================================
-- 17회차: backend-node-nestjs 추가분 (+30, 레벨당 10)
-- cache_key: BACKEND:{L}:NODE_NESTJS:LANGUAGE_FRAMEWORK
-- ============================================================

-- JUNIOR (10문항)










-- MID (10문항)


INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS에서 TypeORM과 Prisma의 차이점과 선택 기준을 설명해주세요.', '네스트제이에스에서 타입오알엠과 프리즈마의 차이점과 선택 기준을 설명해 주세요.', 'DATABASE',
 'TypeORM은 전통적인 Active Record와 Data Mapper 패턴을 지원하는 ORM으로, 데코레이터 기반 엔티티 정의와 마이그레이션을 제공합니다. NestJS와의 통합이 잘 되어있고, JPA에 익숙한 개발자에게 친숙합니다. 다만 복잡한 쿼리에서 타입 추론이 약하고, 릴레이션 관리가 번거로울 수 있습니다. Prisma는 스키마 파일에서 타입을 생성하는 코드 제너레이션 방식으로, 강력한 타입 안전성과 자동 완성을 제공합니다. 마이그레이션도 스키마 변경에서 자동 생성되어 편리합니다. 다만 복잡한 조인이나 서브쿼리 표현에 한계가 있고, N+1 문제에 주의해야 합니다. 선택 기준으로 타입 안전성과 개발자 경험을 우선하면 Prisma를, 유연한 쿼리와 기존 ORM 경험을 활용하려면 TypeORM을 권장합니다.',
 'MODEL_ANSWER', TRUE, NOW());






INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS에서 스케줄링과 큐 기반 작업 처리 방법을 설명해주세요.', '네스트제이에스에서 스케줄링과 큐 기반 작업 처리 방법을 설명해 주세요.', 'NESTJS_CORE',
 'NestJS에서 스케줄링은 schedule 패키지의 Cron, Interval, Timeout 데코레이터로 구현합니다. Cron 표현식으로 정기적인 배치 작업을 정의하고, Interval로 일정 간격의 반복 작업을, Timeout으로 일회성 지연 작업을 설정합니다. 큐 기반 작업 처리에는 Bull 또는 BullMQ 라이브러리를 사용하며, Redis를 백엔드로 사용합니다. Process 데코레이터로 큐의 작업을 처리하는 프로세서를 정의하고, InjectQueue로 큐에 작업을 추가합니다. 큐의 장점으로 작업 재시도, 지연 실행, 우선순위 설정, 진행률 추적이 가능하며, 별도의 프로세스에서 작업을 실행하여 메인 서버의 응답 시간에 영향을 주지 않습니다. 이메일 발송, 이미지 처리, 데이터 동기화 같은 시간이 오래 걸리는 작업에 큐를 활용하는 것이 일반적입니다.',
 'MODEL_ANSWER', TRUE, NOW());


-- SENIOR (10문항)
INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'Node.js 애플리케이션의 수평 확장(Scale-out) 전략과 상태 관리 방법을 설명해주세요.', '노드제이에스 애플리케이션의 수평 확장 전략과 상태 관리 방법을 설명해 주세요.', 'NODEJS_CORE',
 '수평 확장의 핵심은 애플리케이션을 무상태로 설계하는 것입니다. 세션 데이터를 Redis 같은 외부 저장소에 보관하고, 파일 업로드는 S3 같은 객체 저장소를 사용하며, 캐시도 공유 캐시 서버에 저장합니다. Node.js의 cluster 모듈로 단일 서버에서 멀티 프로세스를 실행하여 CPU 코어를 활용하고, PM2를 사용하면 클러스터 관리와 무중단 재시작이 편리합니다. 컨테이너 환경에서는 쿠버네티스의 HPA로 트래픽에 따라 파드 수를 자동 조절합니다. 웹소켓 연결이 있는 경우 sticky 세션이나 Redis 어댑터를 사용하여 인스턴스 간 메시지를 공유합니다. 분산 작업 처리를 위해 BullMQ 같은 큐 시스템을 도입하고, 분산 락으로 중복 실행을 방지합니다. 이벤트 기반 통신에는 Redis Pub/Sub이나 Kafka를 활용하여 인스턴스 간 이벤트를 전파합니다.',
 'MODEL_ANSWER', TRUE, NOW());



INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS에서 GraphQL을 구현할 때 코드 퍼스트와 스키마 퍼스트 접근의 트레이드오프를 설명해주세요.', '네스트제이에스에서 그래프큐엘을 구현할 때 코드 퍼스트와 스키마 퍼스트 접근의 트레이드오프를 설명해 주세요.', 'NESTJS_CORE',
 '코드 퍼스트 접근은 TypeScript 클래스와 데코레이터에서 GraphQL 스키마를 자동 생성합니다. 타입이 한 곳에서 관리되어 일관성이 유지되고, TypeScript의 타입 체크를 활용할 수 있으며, NestJS의 DI와 자연스럽게 통합됩니다. 그러나 데코레이터가 많아지면 코드가 장황해질 수 있습니다. 스키마 퍼스트는 SDL로 스키마를 먼저 정의하고 타입을 생성합니다. GraphQL 스키마 설계가 프론트엔드 팀과의 계약이 되어 협업에 유리하고, 스키마 자체의 가독성이 높습니다. 그러나 스키마와 리졸버 간 동기화를 수동으로 관리해야 합니다. 선택 기준으로 백엔드 주도 개발이면 코드 퍼스트가, 프론트엔드와의 계약 기반 개발이면 스키마 퍼스트가 적합합니다. 성능 측면에서는 DataLoader로 N+1 문제를 해결하고, 복잡도 분석으로 과도한 쿼리를 방지하는 것이 공통 과제입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS 기반 마이크로서비스에서 트랜잭션 관리와 데이터 일관성 유지 전략을 설명해주세요.', '네스트제이에스 기반 마이크로서비스에서 트랜잭션 관리와 데이터 일관성 유지 전략을 설명해 주세요.', 'NESTJS_CORE',
 '마이크로서비스 환경에서 분산 트랜잭션은 단일 데이터베이스 트랜잭션으로 처리할 수 없으므로 다른 접근이 필요합니다. Saga 패턴이 대표적으로, NestJS의 CQRS 모듈의 이벤트 핸들러와 Saga를 활용하여 구현합니다. 코레오그래피 방식에서는 각 서비스가 이벤트를 발행하고 구독하여 자율적으로 작업을 수행하며, 오케스트레이션 방식에서는 중앙 서비스가 전체 흐름을 제어합니다. 멱등성을 보장하기 위해 각 작업에 고유 ID를 부여하고 중복 처리를 방지하는 메커니즘을 구현합니다. 보상 트랜잭션으로 실패 시 이전 단계를 되돌리는 로직을 각 서비스에 구현하고, 데드레터 큐로 처리 불가능한 메시지를 격리합니다. Outbox 패턴으로 로컬 트랜잭션과 이벤트 발행의 원자성을 보장하면 이벤트 유실을 방지할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());





