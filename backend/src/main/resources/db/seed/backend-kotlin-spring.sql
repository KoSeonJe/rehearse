-- backend-kotlin-spring.sql
-- Kotlin/Spring 백엔드 면접 질문 Pool 시딩
-- 총 90문항: JUNIOR 30 / MID 30 / SENIOR 30
-- cache_key: BACKEND:{Level}:KOTLIN_SPRING:LANGUAGE_FRAMEWORK

-- ============================================================
-- JUNIOR (30문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin과 Java의 주요 차이점은 무엇인가요?', 'KOTLIN_CORE',
 'Kotlin은 JVM에서 실행되며 Java와 100% 상호 운용이 가능하지만, Null Safety, Data Class, Extension Function, Coroutine, 간결한 문법 등 현대적인 기능을 추가로 제공합니다. Java에서 자주 발생하는 NullPointerException을 컴파일 시점에 타입 시스템으로 방지하고, data class로 equals/hashCode/toString/copy를 자동 생성합니다. 또한 세미콜론이 불필요하고, 타입 추론이 강력하며, 함수형 프로그래밍 지원이 풍부합니다. Google이 Android 공식 언어로 채택하였으며, 서버사이드에서도 Spring Boot와의 결합이 강력합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin의 Null Safety 시스템을 설명해주세요.', 'KOTLIN_CORE',
 'Kotlin은 타입 시스템에서 nullable 타입(String?)과 non-nullable 타입(String)을 명시적으로 구분합니다. non-nullable 변수에 null을 할당하면 컴파일 오류가 발생하여 NPE를 예방합니다. nullable 값에 접근할 때는 안전 호출 연산자(?.)로 null이면 null을 반환하거나, 엘비스 연산자(?:)로 기본값을 제공합니다. let, also 같은 스코프 함수와 조합하면 null 체크 코드를 간결하게 작성할 수 있으며, !! 연산자는 null이 아님을 확신할 때만 사용하고 남용을 피해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin의 Data Class란 무엇이며 일반 클래스와 어떻게 다른가요?', 'KOTLIN_CORE',
 'data class는 equals(), hashCode(), toString(), copy(), componentN() 메서드를 컴파일러가 자동으로 생성하는 클래스입니다. 주 생성자의 val/var 프로퍼티를 기준으로 동등성(equals)과 해시를 계산하므로 값 기반 비교가 가능합니다. copy() 메서드로 일부 프로퍼티만 변경한 새 인스턴스를 쉽게 생성할 수 있어 불변 객체 패턴에 적합합니다. DTO나 도메인 값 객체에 주로 사용하지만, JPA Entity에는 equals/hashCode를 직접 관리해야 하므로 data class를 지양하는 경향이 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin의 Extension Function이란 무엇이며 어떻게 활용하나요?', 'KOTLIN_CORE',
 'Extension Function은 기존 클래스를 상속하거나 수정하지 않고도 메서드를 추가하는 기능입니다. fun String.isPalindrome(): Boolean { ... } 처럼 수신자 타입을 prefix로 선언하여 정의하고, 해당 타입의 인스턴스에서 메서드처럼 호출합니다. 실제로는 정적 메서드로 컴파일되므로 다형성은 지원되지 않으며, 수신자 타입의 private 멤버에는 접근할 수 없습니다. Spring 프로젝트에서 외부 라이브러리 클래스나 Java 클래스에 편의 메서드를 추가할 때 매우 유용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin 코루틴의 기본 개념을 설명해주세요.', 'COROUTINE',
 '코루틴은 비동기 작업을 동기 코드처럼 읽히게 작성할 수 있는 경량 동시성 프레임워크입니다. OS 스레드보다 훨씬 적은 비용으로 수만 개의 코루틴을 동시에 실행할 수 있으며, suspend 키워드로 코루틴을 일시 중단할 수 있는 지점을 표시합니다. launch는 결과를 반환하지 않는 코루틴을 시작하고, async는 Deferred를 반환하여 await()로 결과를 받습니다. 코루틴은 스레드에 묶이지 않아 여러 스레드에 걸쳐 실행될 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin 코루틴에서 launch와 async의 차이점은 무엇인가요?', 'COROUTINE',
 'launch는 결과를 반환하지 않는 코루틴 빌더로 Job 객체를 반환하며, 독립적인 작업(이벤트 처리, 부작용)을 시작할 때 사용합니다. async는 Deferred<T>를 반환하는 코루틴 빌더로, await()를 호출하여 결과값을 받습니다. async/await 패턴은 여러 비동기 작업을 병렬로 실행하고 모든 결과를 모아야 할 때 유용합니다. 예를 들어 두 API를 동시에 호출하여 결과를 결합하려면 두 개의 async를 시작하고 각각 await()하면 됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin의 Scope Functions(let, run, with, apply, also)을 설명해주세요.', 'KOTLIN_CORE',
 'let은 수신 객체를 람다 인수(it)로 전달하고 람다 결과를 반환하며, null safe 처리(?.let)에 주로 사용합니다. run은 수신 객체를 this로 접근하고 람다 결과를 반환하며, 객체 초기화와 결과 계산에 사용합니다. with는 수신 객체를 this로 접근하고 람다 결과를 반환하지만 확장 함수가 아닙니다. apply는 수신 객체를 this로 접근하고 수신 객체 자체를 반환하며, 객체 초기화/설정에 사용합니다. also는 수신 객체를 it으로 전달하고 수신 객체를 반환하며, 부작용(로깅, 유효성 검사)에 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin의 val과 var의 차이점은 무엇인가요?', 'KOTLIN_CORE',
 'val은 재할당이 불가능한 읽기 전용 변수(Read-only)를 선언하며, Java의 final 변수에 해당합니다. var는 재할당 가능한 변수를 선언합니다. val이 불변 객체를 보장하지는 않으며, val로 선언된 List도 내부 요소를 변경하는 메서드가 있으면 내용이 변경됩니다. 함수형 프로그래밍 원칙과 Kotlin 코드 스타일에서는 가능한 val을 사용하여 불변성을 선호하고, var는 꼭 필요한 경우에만 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 Sealed Class와 Enum Class의 차이점은?', 'KOTLIN_CORE',
 'Enum Class는 동일 타입의 고정된 상수 집합을 정의하며 각 상수는 클래스의 단일 인스턴스입니다. Sealed Class는 제한된 계층 구조의 클래스 집합을 정의하며, 각 서브클래스는 다른 상태나 데이터를 가질 수 있습니다. 서브클래스가 data class, object, class 등 다양한 형태를 가질 수 있어 각 상태마다 다른 프로퍼티를 포함할 수 있습니다. when 표현식에서 Sealed Class의 모든 서브클래스를 처리하면 else 브랜치가 불필요하여 새 서브클래스 추가 시 컴파일러가 처리하지 않은 케이스를 경고합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin과 Spring Boot를 함께 사용할 때 설정에서 주의할 점은?', 'SPRING_KOTLIN',
 'Kotlin 클래스는 기본적으로 final이므로 Spring AOP(@Transactional, @Cacheable 등)에서 CGLIB 프록시 생성이 실패합니다. kotlin-spring 컴파일러 플러그인을 적용하면 @Component, @Transactional 등 Spring 어노테이션이 붙은 클래스를 자동으로 open으로 처리합니다. kotlin-jpa 플러그인은 JPA 엔티티를 위한 파라미터 없는 기본 생성자를 자동 생성합니다. @ConfigurationProperties 클래스에는 @ConstructorBinding으로 불변 설정 바인딩을 사용할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin의 when 표현식은 Java의 switch와 어떻게 다른가요?', 'KOTLIN_CORE',
 'Kotlin의 when은 값을 반환하는 표현식으로 사용할 수 있어 변수에 할당 가능합니다. Java switch와 달리 타입 체크(is String), 범위(in 1..10), 복수 값(1, 2), 임의 조건(x > 0) 등 다양한 조건을 지원합니다. Sealed Class나 Enum과 함께 사용하면 모든 브랜치를 커버하지 않으면 컴파일 오류가 발생하여 안전성을 높입니다. break 문이 필요 없고 폴스루(fall-through)가 없으며, subject 없이 if-else 대체로도 사용할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 object 키워드의 세 가지 사용법을 설명해주세요.', 'KOTLIN_CORE',
 '첫째, Object Declaration은 싱글톤 클래스를 선언합니다(object MySingleton). 둘째, Companion Object는 클래스 내부에 선언하여 Java의 static 멤버와 유사하게 사용합니다. 팩토리 메서드 패턴 구현에 활용됩니다. 셋째, Object Expression은 익명 클래스의 인스턴스를 생성하며 Java의 anonymous inner class에 해당합니다. Companion Object는 실제로 싱글톤 인스턴스이므로 인터페이스를 구현하거나 확장 함수를 가질 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin의 고차 함수(Higher-Order Function)란 무엇인가요?', 'KOTLIN_CORE',
 '고차 함수는 함수를 파라미터로 받거나 함수를 반환하는 함수입니다. Kotlin에서 함수 타입은 (Int, Int) -> Boolean 형태로 표현하며, 람다나 함수 참조를 전달합니다. filter, map, reduce 등 컬렉션 처리 함수가 대표적인 고차 함수입니다. inline 키워드를 사용하면 고차 함수 호출 시 람다 객체 생성과 가상 디스패치 비용이 제거되어 성능이 향상되며, noinline과 crossinline으로 인라인 범위를 제어합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 List, MutableList, Set, Map 컬렉션의 특징은?', 'KOTLIN_CORE',
 'Kotlin 컬렉션은 불변(immutable)과 가변(mutable)으로 분리됩니다. listOf(), setOf(), mapOf()는 읽기 전용 컬렉션을 반환하고, mutableListOf(), mutableSetOf(), mutableMapOf()는 수정 가능한 컬렉션을 반환합니다. 읽기 전용 컬렉션은 변경 메서드가 없어 실수로 수정하는 것을 방지하지만, 내부적으로 Java의 ArrayList 등이므로 캐스팅하면 수정이 가능합니다. 진정한 불변 컬렉션이 필요하면 Guava의 ImmutableList 등을 사용해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 인터페이스와 추상 클래스의 차이점은?', 'KOTLIN_CORE',
 'Kotlin 인터페이스는 상태(필드)를 가질 수 없지만 메서드의 기본 구현을 제공할 수 있습니다. 클래스는 여러 인터페이스를 구현할 수 있지만, 추상 클래스는 하나만 상속할 수 있습니다. 추상 클래스는 상태(프로퍼티)를 가지고, 생성자를 선언할 수 있으며, abstract 키워드로 구현을 강제합니다. 공통 상태나 생성자 로직이 필요하면 추상 클래스를, 타입 계층이나 다형성 계약 정의에는 인터페이스를 우선 고려합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 예외 처리가 Java와 다른 점은 무엇인가요?', 'KOTLIN_CORE',
 'Kotlin에는 Checked Exception이 없습니다. Java에서 IOException, SQLException 등은 반드시 try/catch하거나 throws 선언이 필요하지만, Kotlin은 모든 예외가 Unchecked로 처리됩니다. 이로 인해 불필요한 예외 처리 보일러플레이트가 줄어들지만, 예외가 발생할 수 있는 코드를 명시적으로 문서화하는 것이 중요합니다. @Throws 어노테이션을 사용하면 Java와의 상호운용 시 Checked Exception으로 동작하게 할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 String Template(문자열 템플릿)을 어떻게 사용하나요?', 'KOTLIN_CORE',
 'Kotlin의 문자열 템플릿은 $변수명이나 ${표현식}으로 문자열 내에 변수나 표현식을 삽입합니다. Java의 String.format()이나 + 연산보다 간결하고 가독성이 높습니다. 삼중 따옴표(""")로 여러 줄 문자열을 선언하고 trimIndent()로 들여쓰기를 제거할 수 있어 SQL, JSON 등의 멀티라인 문자열 작성에 유용합니다. 복잡한 표현식은 ${user.name.uppercase()} 형태로 중괄호 안에 작성합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Spring Boot에서 Kotlin으로 REST API를 작성할 때의 장점은?', 'SPRING_KOTLIN',
 'Kotlin의 데이터 클래스로 DTO를 간결하게 선언하고, Null Safety로 NPE 위험을 컴파일 시점에 방지합니다. Builder 패턴 없이도 named argument와 default parameter로 객체를 생성하고, copy()로 불변 DTO를 간편하게 변환합니다. Spring WebFlux와 결합 시 코루틴으로 비동기 코드를 동기처럼 작성할 수 있으며, Kotlin DSL로 타입 안전한 설정을 구성합니다. @SpringBootApplication 등 Spring 어노테이션은 Kotlin에서도 동일하게 사용하며, 기존 Java Spring 코드와 혼용도 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin의 타입 추론(Type Inference)이란 무엇인가요?', 'KOTLIN_CORE',
 '타입 추론은 컴파일러가 변수나 표현식의 타입을 초기화 값에서 자동으로 결정하는 기능입니다. val name = "Kotlin"은 String 타입으로, val count = 42는 Int 타입으로 추론됩니다. 함수 반환 타입도 단일 표현식 함수(= 구문)에서는 추론됩니다. 그러나 공개 API 함수의 반환 타입은 명시적으로 선언하여 코드 읽는 사람과 API 계약을 명확히 하는 것이 좋습니다. 타입 추론 덕분에 Java보다 훨씬 적은 코드로 동일한 타입 안전성을 확보할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 함수형 프로그래밍 스타일로 컬렉션을 처리하는 방법은?', 'FUNCTIONAL',
 'Kotlin은 filter, map, flatMap, reduce, fold, groupBy, sortedBy, take, drop 등 풍부한 컬렉션 처리 함수를 제공합니다. 체이닝으로 데이터 변환 파이프라인을 구성하며, 읽기 쉬운 선언적 코드를 작성할 수 있습니다. asSequence()로 Lazy 시퀀스로 전환하면 대용량 컬렉션에서 중간 결과 컬렉션 생성 없이 최적화된 처리가 가능합니다. 함수 참조(::방식)로 기존 함수를 고차 함수 인수로 전달할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 JPA 엔티티를 설계할 때 주의사항은?', 'SPRING_KOTLIN',
 'JPA는 엔티티에 파라미터 없는 기본 생성자를 요구하는데, Kotlin 클래스는 기본적으로 기본 생성자가 없으므로 kotlin-jpa 플러그인을 사용하거나 직접 선언합니다. Data class는 equals/hashCode를 주 생성자 파라미터 기반으로 생성하므로 id로만 동등성을 비교해야 하는 JPA 엔티티에는 적합하지 않습니다. Kotlin 클래스는 기본 final이므로 kotlin-spring 플러그인이나 open 키워드로 CGLIB 프록시 생성을 허용해야 합니다. @Column(nullable = false)와 Kotlin non-nullable 타입을 일치시켜 DB 제약과 Null Safety를 모두 활용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin의 구조 분해(Destructuring Declaration)를 설명해주세요.', 'KOTLIN_CORE',
 '구조 분해는 하나의 객체를 여러 변수로 분해하여 할당하는 문법입니다. data class의 componentN() 메서드를 기반으로 동작하며, val (name, age) = person 처럼 사용합니다. Map.Entry도 구조 분해가 가능하여 for ((key, value) in map) 루프를 간결하게 작성합니다. 함수에서 Pair나 data class를 반환하면 호출자에서 구조 분해로 여러 값을 편리하게 받을 수 있습니다. 필요 없는 값은 _ 언더스코어로 무시할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 inline class(value class)란 무엇인가요?', 'KOTLIN_CORE',
 '@JvmInline @value class는 단일 프로퍼티를 래핑하는 클래스로, 컴파일 시 런타임에서 래퍼 클래스 인스턴스가 제거되고 내부 타입으로 대체되어 성능 오버헤드가 없습니다. String을 래핑한 UserId(val id: String)를 선언하면 타입 시스템에서 UserId와 String이 구분되어 실수로 일반 String을 UserId 파라미터에 전달하는 것을 방지합니다. 도메인 원시 타입(Primitive Obsession) 문제를 해결하고 타입 안전성을 높이는 데 유용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 operator overloading(연산자 오버로딩)을 어떻게 구현하나요?', 'KOTLIN_CORE',
 'Kotlin은 operator 키워드로 특정 함수를 연산자에 매핑합니다. plus()는 + 연산자, minus()는 - 연산자, times()는 * 연산자에 해당합니다. equals()/hashCode()는 == 연산자, compareTo()는 <, > 비교 연산자를 지원합니다. 컬렉션의 get()/set()은 [] 인덱스 연산자에 매핑됩니다. contains()는 in 연산자에, invoke()는 () 함수 호출 연산자에 해당합니다. 직관적인 도메인 객체 연산을 표현할 때 유용하지만, 의미가 불명확한 연산자 오버로딩은 가독성을 해칩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 object expression과 람다의 차이는?', 'KOTLIN_CORE',
 'object expression은 특정 인터페이스나 추상 클래스를 즉석에서 구현하는 익명 클래스의 인스턴스를 생성합니다. 람다는 단일 추상 메서드(SAM) 인터페이스를 간결하게 구현하는 함수 리터럴입니다. Kotlin에서는 함수형 인터페이스(fun interface)에 람다를 직접 사용할 수 있으며, Java의 SAM 인터페이스에도 람다를 전달할 수 있습니다. 여러 메서드가 있는 인터페이스 구현에는 object expression이 필요합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 lazy initialization(지연 초기화)을 구현하는 방법은?', 'KOTLIN_CORE',
 'by lazy { } 위임은 val 프로퍼티의 값을 처음 접근할 때 딱 한 번만 초기화합니다. 기본적으로 스레드 안전(LazyThreadSafetyMode.SYNCHRONIZED)하지만, 단일 스레드 환경에서는 LazyThreadSafetyMode.NONE으로 동기화 오버헤드를 제거할 수 있습니다. lateinit var는 non-nullable var 프로퍼티의 초기화를 나중으로 미루며, 초기화 전 접근 시 UninitializedPropertyAccessException이 발생합니다. isInitialized로 초기화 여부를 확인할 수 있습니다. by lazy는 불변 싱글톤에, lateinit var는 DI로 주입받는 값에 적합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 Elvis 연산자(?:)의 활용법을 설명해주세요.', 'KOTLIN_CORE',
 'Elvis 연산자(?:)는 좌측 표현식이 null이면 우측 값을 반환하고, null이 아니면 좌측 값을 반환합니다. null 기본값 제공(val name = user?.name ?: "Anonymous"), null 시 예외 발생(?: throw IllegalArgumentException()), null 시 early return(val user = findUser() ?: return) 세 가지 패턴으로 주로 활용됩니다. Java의 Optional.orElse()나 삼항 연산자보다 간결하게 null 처리를 표현할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin의 주 생성자(Primary Constructor)와 부 생성자(Secondary Constructor)의 차이는?', 'KOTLIN_CORE',
 '주 생성자는 클래스 헤더에 선언되며 class User(val name: String, val email: String) 형태로 프로퍼티 선언과 초기화를 동시에 합니다. init 블록으로 주 생성자 초기화 로직을 추가할 수 있습니다. 부 생성자는 constructor 키워드로 클래스 내부에 선언하며 : this(...)로 반드시 주 생성자에 위임해야 합니다. Kotlin에서는 default parameter를 활용하면 대부분의 경우 부 생성자가 불필요합니다. Java와의 상호운용을 위해 @JvmOverloads를 주 생성자에 선언하면 각 기본값 조합의 오버로드 메서드가 생성됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- ============================================================
-- MID (30문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin 코루틴의 suspend 함수 원리를 설명해주세요.', 'COROUTINE',
 'suspend 함수는 코루틴 컨텍스트에서만 호출 가능하며, 컴파일 시 Continuation 파라미터가 추가된 상태 머신으로 변환됩니다. 각 suspend 지점에서 함수 실행 상태(로컬 변수, 재개 지점)를 Continuation 객체에 저장하고 스레드를 반환합니다. I/O 완료나 타이머 만료 시 Continuation이 재개되어 저장된 상태에서 실행이 계속됩니다. 이 Continuation Passing Style(CPS) 변환 덕분에 콜백 없이 동기 코드처럼 작성된 비동기 코드가 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'CoroutineScope와 CoroutineContext, Dispatcher의 역할을 설명해주세요.', 'COROUTINE',
 'CoroutineContext는 코루틴 실행 환경을 구성하는 요소들의 집합으로, Dispatcher, Job, CoroutineExceptionHandler 등이 포함됩니다. Dispatcher는 코루틴이 실행될 스레드를 결정합니다. Dispatchers.Default는 CPU 집약 작업용(공유 스레드 풀), Dispatchers.IO는 I/O 블로킹 작업용(확장 스레드 풀), Dispatchers.Main은 UI 스레드용입니다. CoroutineScope는 코루틴 생명주기를 관리하며 스코프가 취소되면 내부 모든 코루틴이 취소됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin 코루틴에서 Structured Concurrency란 무엇인가요?', 'COROUTINE',
 'Structured Concurrency는 코루틴이 항상 스코프 내에서 생성되며, 부모 스코프가 종료될 때까지 자식 코루틴이 완료됨을 보장하는 원칙입니다. 부모 코루틴이 취소되면 모든 자식 코루틴이 취소되어 리소스 누수를 방지합니다. coroutineScope { } 빌더는 내부 코루틴이 모두 완료될 때까지 기다리고, 하나라도 예외가 발생하면 나머지를 취소합니다. GlobalScope 사용은 구조화된 동시성을 무시하므로 가능한 피해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin 코루틴의 취소(Cancellation) 메커니즘과 협력적 취소란?', 'COROUTINE',
 '코루틴 취소는 Job.cancel()로 시작하며, 취소 신호는 다음 suspend 지점에서 CancellationException을 던져 전파됩니다. 협력적 취소(Cooperative Cancellation)는 코루틴이 취소에 협력해야 한다는 의미로, suspend 함수 호출이나 isActive 체크 없이 긴 계산 루프를 실행하면 취소가 동작하지 않습니다. 취소 불가능한 블록이 필요하면 withContext(NonCancellable) { }을 사용합니다. finally 블록은 취소 시에도 실행되어 리소스 정리에 활용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin의 Sequence와 일반 컬렉션의 차이점과 성능 비교는?', 'FUNCTIONAL',
 '일반 컬렉션의 filter/map은 즉시 평가(Eager Evaluation)로 각 연산마다 새 컬렉션을 생성합니다. Sequence는 지연 평가(Lazy Evaluation)로 최종 연산(toList, find 등)이 호출될 때 각 요소에 대해 모든 중간 연산을 순차적으로 처리합니다. 대용량 컬렉션에서 중간 컬렉션 생성이 없어 메모리 효율적이고, 최종 연산 전에 조기 종료(find, first)가 가능합니다. 소규모 컬렉션이나 간단한 연산에서는 Sequence 초기화 오버헤드로 오히려 느릴 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Spring WebFlux와 Kotlin 코루틴을 함께 사용하는 방법을 설명해주세요.', 'SPRING_KOTLIN',
 'Spring WebFlux 5.2+ 부터 suspend 함수를 컨트롤러 메서드로 사용할 수 있어 Mono/Flux를 직접 다루지 않고도 반응형 API를 구현합니다. @RestController의 핸들러 메서드를 suspend fun으로 선언하면 Spring이 내부적으로 코루틴 어댑터를 통해 Mono로 변환합니다. Flow<T>로 스트리밍 응답을 구현하며, Spring Data R2DBC와 코루틴을 결합하면 완전한 비동기 논블로킹 스택을 구성합니다. withContext(Dispatchers.IO)로 블로킹 코드를 감싸 이벤트 루프 차단을 방지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin의 제네릭 타입 파라미터에서 in/out 공변성(Variance)을 설명해주세요.', 'KOTLIN_CORE',
 'out 키워드(공변, Covariance)는 제네릭 타입이 생산자 역할만 하며, T의 서브타입을 허용합니다. List<out Animal>은 List<Dog>을 받을 수 있지만 추가는 불가합니다. in 키워드(반공변, Contravariance)는 소비자 역할만 하며, T의 슈퍼타입을 허용합니다. Comparable<in T>처럼 데이터를 받아 처리하는 경우에 사용합니다. reified 타입 파라미터는 inline 함수에서만 사용 가능하며, 런타임에 타입 정보를 유지하여 is T 타입 체크가 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 DSL(Domain Specific Language)을 구현하는 방법은?', 'KOTLIN_CORE',
 'Kotlin DSL은 수신자가 있는 람다(lambda with receiver), 확장 함수, infix 함수, 연산자 오버로딩을 조합하여 구현합니다. @DslMarker 어노테이션으로 DSL 스코프를 제한하여 중첩 DSL 빌더에서 외부 빌더 메서드가 잘못 호출되는 것을 방지합니다. Kotlin DSL의 대표 사례는 Gradle Kotlin DSL, Ktor 라우팅, Exposed ORM, Spring Security DSL입니다. 도메인 전문가도 읽을 수 있는 표현력 높은 설정 API를 제공할 때 유용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 Result 타입과 함수형 에러 처리 패턴을 설명해주세요.', 'FUNCTIONAL',
 'Result<T>는 성공(Success)과 실패(Failure)를 표현하는 내장 타입으로, runCatching { }으로 예외를 Result로 래핑합니다. getOrElse, getOrThrow, onSuccess, onFailure로 결과를 처리합니다. 함수형 에러 처리를 위해 Arrow 라이브러리의 Either<Error, Value>를 사용하면 왼쪽은 실패, 오른쪽은 성공으로 타입 안전한 에러 처리를 구성합니다. map, flatMap으로 체인을 구성하고, fold로 양쪽 케이스를 처리합니다. 예외 기반보다 명시적인 에러 타입 설계가 API 계약을 명확하게 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 delegation(위임) 패턴 구현 방법을 설명해주세요.', 'KOTLIN_CORE',
 'Kotlin은 by 키워드로 인터페이스 구현을 다른 객체에 위임하는 클래스 위임을 지원합니다. class MyList<T>(private val delegate: MutableList<T>) : MutableList<T> by delegate 처럼 선언하면 위임 객체의 모든 메서드를 자동으로 구현합니다. 프로퍼티 위임은 by 키워드로 getValue/setValue 연산자를 구현한 객체에 위임합니다. by lazy, by Delegates.observable, by Delegates.vetoable가 내장 프로퍼티 위임입니다. 이를 통해 상속 없이도 기존 클래스에 기능을 추가하거나 데코레이터 패턴을 간결하게 구현합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 Flow와 SharedFlow, StateFlow의 차이점은?', 'COROUTINE',
 'Flow는 cold 스트림으로 각 collect마다 독립적으로 스트림이 시작됩니다. SharedFlow는 hot 스트림으로 여러 collector가 같은 스트림을 공유하며, replay 파라미터로 새 collector에게 이전 값을 전달합니다. StateFlow는 현재 상태를 유지하는 hot 스트림으로 항상 초기값이 있고, 같은 값 연속 발행 시 skip합니다. UI 상태 관리에는 StateFlow, 이벤트 발행에는 SharedFlow(replay=0), 데이터 처리 파이프라인에는 Flow를 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 Channel과 Flow의 차이와 사용 시나리오는?', 'COROUTINE',
 'Channel은 코루틴 간 데이터를 전달하는 통신 프리미티브로, producer-consumer 패턴에 직접 사용합니다. 버퍼 크기(BUFFERED, RENDEZVOUS, UNLIMITED)와 onFull 전략(SUSPEND, DROP_OLDEST, DROP_LATEST)을 설정할 수 있습니다. Flow는 선언적 데이터 스트림으로 operators 체인으로 처리하고 backpressure를 자동으로 처리합니다. 코루틴 간 직접 통신에는 Channel, 데이터 변환 파이프라인에는 Flow를 선호합니다. Channel은 hot이므로 receiver가 없으면 데이터가 쌓이는 반면 Flow는 cold여서 consume 없이 데이터가 생성되지 않습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 테스트 코드 작성 시 코루틴 처리 방법은?', 'COROUTINE',
 'kotlinx-coroutines-test의 runTest { }는 테스트용 코루틴 스코프를 제공하며 delay()를 즉시 건너뛰어 실행 시간 없이 빠르게 테스트합니다. TestCoroutineScheduler로 가상 시간을 제어하여 delay 기반 로직을 단위 테스트합니다. Dispatchers.setMain(testDispatcher)으로 Main 디스패처를 테스트 디스패처로 교체하고, 테스트 후 Dispatchers.resetMain()으로 복원합니다. turbine 라이브러리는 Flow 테스트를 위한 expectItem(), expectComplete(), expectError() 등의 어설션을 제공합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 Exposed ORM 프레임워크 기본 사용법은?', 'SPRING_KOTLIN',
 'Exposed는 JetBrains에서 만든 Kotlin 전용 경량 ORM으로 두 가지 API를 제공합니다. DSL 방식은 Table 객체를 정의하고 select, insert, update, deleteWhere 등의 타입 안전한 쿼리를 Kotlin 코드로 작성합니다. DAO 방식은 Entity 클래스로 객체 지향적 DB 접근을 제공합니다. Coroutine 지원을 위해 exposed-kotlin-dao와 함께 newSuspendedTransaction { }을 사용합니다. JPA와 달리 LazyLoading 없이 명시적 쿼리로 동작하므로 N+1 문제가 없고 동작이 예측 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- ============================================================
-- SENIOR (30문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Java Virtual Thread(Project Loom)와 Kotlin 코루틴을 비교 설명해주세요.', 'COROUTINE',
 'Java Virtual Thread는 JVM 레벨의 경량 스레드로 기존 스레드 블로킹 코드를 변경 없이 사용하면서 높은 동시성을 확보합니다. Kotlin 코루틴은 언어 레벨의 협력적 동시성으로 suspend 함수와 구조화된 동시성을 통해 명시적 비동기 코드를 작성합니다. Virtual Thread는 기존 Java/블로킹 라이브러리와 완벽 호환되고, 코루틴은 더 세밀한 동시성 제어와 취소, Flow 기반 스트리밍이 가능합니다. Spring Boot 3.2+에서 Virtual Thread와 코루틴을 함께 사용할 수 있으며, 두 기술이 배타적이지 않습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin 코루틴의 Job 생명주기와 SupervisorJob의 차이를 설명해주세요.', 'COROUTINE',
 'Job은 New → Active → Completing → Completed, 또는 Cancelling → Cancelled 상태를 거칩니다. 일반 Job에서 자식 코루틴이 예외로 실패하면 형제 코루틴들도 취소됩니다. SupervisorJob은 자식 실패가 부모나 다른 자식에게 전파되지 않아 독립적인 자식 코루틴 관리가 가능합니다. supervisorScope { }로 일시적 SupervisorJob 스코프를 생성하고, 각 자식의 예외를 독립적으로 처리합니다. CoroutineExceptionHandler를 SupervisorJob 스코프에 등록하여 전역 예외를 처리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin Multiplatform에서 코루틴을 공통 코드로 사용할 때의 고려사항은?', 'COROUTINE',
 'Kotlin Multiplatform(KMP)에서 kotlinx-coroutines-core는 JVM, JS, Native 모두에서 동작하지만 플랫폼별 주의사항이 있습니다. iOS(Native)에서는 메인 스레드에서만 코루틴을 관리하거나 strict memory model을 준수해야 합니다. Dispatchers.Main은 플랫폼별로 다르게 구현되며, kotlinx.coroutines.Dispatchers.Main을 사용하면 각 플랫폼에 맞게 동작합니다. expect/actual 메커니즘으로 플랫폼별 디스패처나 스레드 관련 구현을 분리합니다. Flow는 multiplatform에서 동작하며 공통 비즈니스 로직의 데이터 스트림으로 활용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin 코루틴 성능 모니터링과 디버깅 방법을 설명해주세요.', 'COROUTINE',
 'JVM 옵션 -Dkotlinx.coroutines.debug을 설정하면 스레드 이름에 코루틴 이름이 포함되어 디버깅이 편리합니다. CoroutineName 컨텍스트로 코루틴에 의미있는 이름을 부여합니다. CoroutineScope 내 활성 Job 수를 모니터링하여 리소스 누수를 감지합니다. IntelliJ IDEA의 코루틴 디버거는 일시 중단된 코루틴 스택을 시각화합니다. Micrometer와 통합하여 코루틴 지연 시간, 활성 코루틴 수를 메트릭으로 수집합니다. 성능 테스트 시 Dispatchers.Default의 스레드 풀 포화 여부를 확인하고, CPU 바운드와 I/O 바운드 코루틴의 Dispatcher를 적절히 분리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin의 컴파일러 플러그인과 Symbol Processing(KSP) 활용 방법은?', 'KOTLIN_CORE',
 'KSP(Kotlin Symbol Processing)는 KAPT(Kotlin Annotation Processing Tool)의 경량 대안으로 Kotlin-native 코드 생성 프레임워크입니다. KAPT가 자바 stubs를 생성하는 방식과 달리 KSP는 Kotlin 타입 시스템을 직접 분석하여 2배 이상 빠릅니다. Room, Moshi, Hilt 등이 KSP를 지원하며, Processor 인터페이스를 구현하여 커스텀 코드 생성기를 작성합니다. KSP로 보일러플레이트 코드(Mapper, Factory, Builder)를 컴파일 타임에 자동 생성하여 런타임 리플렉션 비용을 제거합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin과 Spring을 사용한 함수형 아키텍처(Hexagonal Architecture) 설계 방법은?', 'SPRING_KOTLIN',
 '헥사고날 아키텍처에서 도메인 모델은 순수 Kotlin 객체(sealed class, data class)로 구성하고 프레임워크 의존성을 배제합니다. Port는 인터페이스(interface)로 정의하고, Adapter(Spring Controller, Repository)는 Port를 구현합니다. 유스케이스는 인터페이스 없이 suspend fun으로 구현하고 Spring @Service로 등록합니다. Kotlin의 sealed class Result<T>로 유스케이스 반환 타입을 모델링하면 성공/실패 처리가 타입 안전합니다. 도메인 코어를 별도 모듈로 분리하면 Spring 없이도 도메인 단위 테스트가 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 Arrow 라이브러리를 활용한 함수형 프로그래밍 패턴은?', 'FUNCTIONAL',
 'Arrow는 Kotlin에서 함수형 프로그래밍 추상화(Either, Option, IO, Lens, Monad)를 제공하는 라이브러리입니다. Either<Error, Value>로 타입 안전한 에러 처리를 구현하고, 체이닝 시 왼쪽(오류)이 있으면 단락(short-circuit)됩니다. Option<T>는 Kotlin의 nullable 타입과 유사하지만 함수형 조합이 더 용이합니다. Lens와 Prism으로 중첩된 불변 데이터 구조를 타입 안전하게 수정합니다. Arrow의 Saga 패턴 구현체로 분산 트랜잭션의 보상 로직을 선언적으로 정의합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin 기반 마이크로서비스에서 Ktor와 Spring Boot의 선택 기준은?', 'SPRING_KOTLIN',
 'Ktor는 JetBrains에서 만든 Kotlin-first 비동기 웹 프레임워크로 코루틴이 기본이며 경량화된 마이크로서비스에 적합합니다. Spring Boot보다 시작 시간이 빠르고 메모리 사용량이 낮아 서버리스나 컨테이너 환경에 유리합니다. 단, Spring의 광범위한 생태계(Security, Data, Batch, Cloud)가 없어 기능을 직접 구현해야 하는 경우가 많습니다. 기존 Spring 인프라(팀 경험, 운영 도구)가 있으면 Spring Boot + Kotlin이 생산성이 높고, 완전히 새로운 경량 서비스에는 Ktor를 고려합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 Context Receiver를 활용한 의존성 관리 패턴을 설명해주세요.', 'KOTLIN_CORE',
 'Context Receiver(Kotlin 1.6 실험 기능)는 함수가 여러 리시버 컨텍스트를 요구함을 선언하는 기능으로, with(ctx) 블록 없이 여러 스코프의 함수를 조합합니다. context(Logger, Database) fun saveUser(user: User) { ... } 형태로 선언하면 두 컨텍스트가 모두 제공된 경우에만 호출 가능합니다. 의존성 주입 없이 타입 시스템으로 런타임 의존성을 표현하는 Effect System 패턴의 Kotlin 구현에 활용됩니다. Spring DI와 결합하면 인터페이스 없이도 의존성 경계를 컴파일 타임에 강제할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin Native와 GraalVM Native Image를 이용한 Spring Boot 네이티브 컴파일의 장단점은?', 'SPRING_KOTLIN',
 'Spring Boot 3 + GraalVM Native Image는 AOT(Ahead-of-Time) 컴파일로 JVM 없이 실행되는 네이티브 바이너리를 생성합니다. 시작 시간이 밀리초 단위로 단축되고 메모리 사용량이 크게 감소하여 서버리스와 컨테이너 환경에서 비용 절감 효과가 큽니다. 단, 빌드 시간이 길고(분 단위), 리플렉션/동적 클래스 로딩 사용에 제약이 있어 일부 라이브러리가 네이티브 힌트 추가를 요구합니다. Kotlin 클래스의 final 기본값과 코루틴은 GraalVM과 호환성이 좋으며, Spring AOT Engine이 필요한 힌트를 자동 생성하는 범위가 점차 확대되고 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 대규모 팀 개발 시 코드 품질 관리 전략은?', 'KOTLIN_CORE',
 'ktlint와 detekt를 CI 파이프라인에 통합하여 코드 스타일 일관성과 코드 품질을 자동 검증합니다. detekt의 커스텀 룰로 프로젝트 아키텍처 규칙(Service가 Repository만 사용하는지 등)을 정적 분석으로 강제합니다. 팀 코딩 컨벤션을 .editorconfig와 intellij formatter 설정으로 공유합니다. API 안정성을 위해 Binary Compatibility Validator를 사용하여 공개 API 변경을 추적합니다. Kotlin API 가이드라인(함수형 스타일 선호, 불변성 우선, null 처리 일관성)을 팀 공유 문서로 관리하고 코드 리뷰에서 지속적으로 피드백합니다.',
 'MODEL_ANSWER', TRUE, NOW());
