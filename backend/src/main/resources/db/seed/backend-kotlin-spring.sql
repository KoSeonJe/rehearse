-- backend-kotlin-spring.sql
-- Kotlin/Spring 백엔드 면접 질문 Pool 시딩
-- 총 90문항: JUNIOR 30 / MID 30 / SENIOR 30
-- cache_key: BACKEND:{Level}:KOTLIN_SPRING:LANGUAGE_FRAMEWORK

-- ============================================================
-- JUNIOR (30문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin과 Java의 주요 차이점은 무엇인가요?', '코틀린과 자바의 주요 차이점은 무엇인가요?', 'KOTLIN_CORE',
 'Kotlin은 JVM에서 실행되며 Java와 100% 상호 운용이 가능하지만, Null Safety, Data Class, Extension Function, Coroutine, 간결한 문법 등 현대적인 기능을 추가로 제공합니다. Java에서 자주 발생하는 NullPointerException을 컴파일 시점에 타입 시스템으로 방지하고, data class로 equals/hashCode/toString/copy를 자동 생성합니다. 또한 세미콜론이 불필요하고, 타입 추론이 강력하며, 함수형 프로그래밍 지원이 풍부합니다. Google이 Android 공식 언어로 채택하였으며, 서버사이드에서도 Spring Boot와의 결합이 강력합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin의 Null Safety 시스템을 설명해주세요.', '코틀린의 널 세이프티 시스템을 설명해주세요.', 'KOTLIN_CORE',
 'Kotlin은 타입 시스템에서 nullable 타입(String?)과 non-nullable 타입(String)을 명시적으로 구분합니다. non-nullable 변수에 null을 할당하면 컴파일 오류가 발생하여 NPE를 예방합니다. nullable 값에 접근할 때는 안전 호출 연산자(?.)로 null이면 null을 반환하거나, 엘비스 연산자(?:)로 기본값을 제공합니다. let, also 같은 스코프 함수와 조합하면 null 체크 코드를 간결하게 작성할 수 있으며, !! 연산자는 null이 아님을 확신할 때만 사용하고 남용을 피해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin의 Data Class란 무엇이며 일반 클래스와 어떻게 다른가요?', '코틀린의 데이터 클래스란 무엇이며 일반 클래스와 어떻게 다른가요?', 'KOTLIN_CORE',
 'data class는 equals(), hashCode(), toString(), copy(), componentN() 메서드를 컴파일러가 자동으로 생성하는 클래스입니다. 주 생성자의 val/var 프로퍼티를 기준으로 동등성(equals)과 해시를 계산하므로 값 기반 비교가 가능합니다. copy() 메서드로 일부 프로퍼티만 변경한 새 인스턴스를 쉽게 생성할 수 있어 불변 객체 패턴에 적합합니다. DTO나 도메인 값 객체에 주로 사용하지만, JPA Entity에는 equals/hashCode를 직접 관리해야 하므로 data class를 지양하는 경향이 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin의 Extension Function이란 무엇이며 어떻게 활용하나요?', '코틀린의 익스텐션 펑션이란 무엇이며 어떻게 활용하나요?', 'KOTLIN_CORE',
 'Extension Function은 기존 클래스를 상속하거나 수정하지 않고도 메서드를 추가하는 기능입니다. fun String.isPalindrome(): Boolean { ... } 처럼 수신자 타입을 prefix로 선언하여 정의하고, 해당 타입의 인스턴스에서 메서드처럼 호출합니다. 실제로는 정적 메서드로 컴파일되므로 다형성은 지원되지 않으며, 수신자 타입의 private 멤버에는 접근할 수 없습니다. Spring 프로젝트에서 외부 라이브러리 클래스나 Java 클래스에 편의 메서드를 추가할 때 매우 유용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin 코루틴의 기본 개념을 설명해주세요.', '코틀린 코루틴의 기본 개념을 설명해주세요.', 'COROUTINE',
 '코루틴은 비동기 작업을 동기 코드처럼 읽히게 작성할 수 있는 경량 동시성 프레임워크입니다. OS 스레드보다 훨씬 적은 비용으로 수만 개의 코루틴을 동시에 실행할 수 있으며, suspend 키워드로 코루틴을 일시 중단할 수 있는 지점을 표시합니다. launch는 결과를 반환하지 않는 코루틴을 시작하고, async는 Deferred를 반환하여 await()로 결과를 받습니다. 코루틴은 스레드에 묶이지 않아 여러 스레드에 걸쳐 실행될 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin 코루틴에서 launch와 async의 차이점은 무엇인가요?', '코틀린 코루틴에서 launch와 async의 차이점은 무엇인가요?', 'COROUTINE',
 'launch는 결과를 반환하지 않는 코루틴 빌더로 Job 객체를 반환하며, 독립적인 작업(이벤트 처리, 부작용)을 시작할 때 사용합니다. async는 Deferred<T>를 반환하는 코루틴 빌더로, await()를 호출하여 결과값을 받습니다. async/await 패턴은 여러 비동기 작업을 병렬로 실행하고 모든 결과를 모아야 할 때 유용합니다. 예를 들어 두 API를 동시에 호출하여 결과를 결합하려면 두 개의 async를 시작하고 각각 await()하면 됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin의 Scope Functions(let, run, with, apply, also)을 설명해주세요.', '코틀린의 스코프 펑션즈(let, run, with, apply, also)을 설명해주세요.', 'KOTLIN_CORE',
 'let은 수신 객체를 람다 인수(it)로 전달하고 람다 결과를 반환하며, null safe 처리(?.let)에 주로 사용합니다. run은 수신 객체를 this로 접근하고 람다 결과를 반환하며, 객체 초기화와 결과 계산에 사용합니다. with는 수신 객체를 this로 접근하고 람다 결과를 반환하지만 확장 함수가 아닙니다. apply는 수신 객체를 this로 접근하고 수신 객체 자체를 반환하며, 객체 초기화/설정에 사용합니다. also는 수신 객체를 it으로 전달하고 수신 객체를 반환하며, 부작용(로깅, 유효성 검사)에 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin의 val과 var의 차이점은 무엇인가요?', '코틀린의 val과 var의 차이점은 무엇인가요?', 'KOTLIN_CORE',
 'val은 재할당이 불가능한 읽기 전용 변수(Read-only)를 선언하며, Java의 final 변수에 해당합니다. var는 재할당 가능한 변수를 선언합니다. val이 불변 객체를 보장하지는 않으며, val로 선언된 List도 내부 요소를 변경하는 메서드가 있으면 내용이 변경됩니다. 함수형 프로그래밍 원칙과 Kotlin 코드 스타일에서는 가능한 val을 사용하여 불변성을 선호하고, var는 꼭 필요한 경우에만 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 Sealed Class와 Enum Class의 차이점은?', '코틀린에서 실드 클래스와 이넘 클래스의 차이점은?', 'KOTLIN_CORE',
 'Enum Class는 동일 타입의 고정된 상수 집합을 정의하며 각 상수는 클래스의 단일 인스턴스입니다. Sealed Class는 제한된 계층 구조의 클래스 집합을 정의하며, 각 서브클래스는 다른 상태나 데이터를 가질 수 있습니다. 서브클래스가 data class, object, class 등 다양한 형태를 가질 수 있어 각 상태마다 다른 프로퍼티를 포함할 수 있습니다. when 표현식에서 Sealed Class의 모든 서브클래스를 처리하면 else 브랜치가 불필요하여 새 서브클래스 추가 시 컴파일러가 처리하지 않은 케이스를 경고합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin과 Spring Boot를 함께 사용할 때 설정에서 주의할 점은?', '코틀린과 스프링 부트를 함께 사용할 때 설정에서 주의할 점은?', 'SPRING_KOTLIN',
 'Kotlin 클래스는 기본적으로 final이므로 Spring AOP(@Transactional, @Cacheable 등)에서 CGLIB 프록시 생성이 실패합니다. kotlin-spring 컴파일러 플러그인을 적용하면 @Component, @Transactional 등 Spring 어노테이션이 붙은 클래스를 자동으로 open으로 처리합니다. kotlin-jpa 플러그인은 JPA 엔티티를 위한 파라미터 없는 기본 생성자를 자동 생성합니다. Spring Boot 3.x에서는 생성자가 하나인 경우 @ConstructorBinding이 불필요하며, 주 생성자 기반으로 불변 설정 바인딩이 자동 적용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin의 when 표현식은 Java의 switch와 어떻게 다른가요?', '코틀린의 when 표현식은 자바의 switch와 어떻게 다른가요?', 'KOTLIN_CORE',
 'Kotlin의 when은 값을 반환하는 표현식으로 사용할 수 있어 변수에 할당 가능합니다. Java switch와 달리 타입 체크(is String), 범위(in 1..10), 복수 값(1, 2), 임의 조건(x > 0) 등 다양한 조건을 지원합니다. Sealed Class나 Enum과 함께 사용하면 모든 브랜치를 커버하지 않으면 컴파일 오류가 발생하여 안전성을 높입니다. break 문이 필요 없고 폴스루(fall-through)가 없으며, subject 없이 if-else 대체로도 사용할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 object 키워드의 세 가지 사용법을 설명해주세요.', '코틀린에서 object 키워드의 세 가지 사용법을 설명해주세요.', 'KOTLIN_CORE',
 '첫째, Object Declaration은 싱글톤 클래스를 선언합니다(object MySingleton). 둘째, Companion Object는 클래스 내부에 선언하여 Java의 static 멤버와 유사하게 사용합니다. 팩토리 메서드 패턴 구현에 활용됩니다. 셋째, Object Expression은 익명 클래스의 인스턴스를 생성하며 Java의 anonymous inner class에 해당합니다. Companion Object는 실제로 싱글톤 인스턴스이므로 인터페이스를 구현하거나 확장 함수를 가질 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin의 고차 함수(Higher-Order Function)란 무엇인가요?', '코틀린의 고차 함수(하이어 오더 펑션)란 무엇인가요?', 'KOTLIN_CORE',
 '고차 함수는 함수를 파라미터로 받거나 함수를 반환하는 함수입니다. Kotlin에서 함수 타입은 (Int, Int) -> Boolean 형태로 표현하며, 람다나 함수 참조를 전달합니다. filter, map, reduce 등 컬렉션 처리 함수가 대표적인 고차 함수입니다. inline 키워드를 사용하면 고차 함수 호출 시 람다 객체 생성과 가상 디스패치 비용이 제거되어 성능이 향상되며, noinline과 crossinline으로 인라인 범위를 제어합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 List, MutableList, Set, Map 컬렉션의 특징은?', '코틀린에서 List, MutableList, Set, Map 컬렉션의 특징은?', 'KOTLIN_CORE',
 'Kotlin 컬렉션은 불변(immutable)과 가변(mutable)으로 분리됩니다. listOf(), setOf(), mapOf()는 읽기 전용 컬렉션을 반환하고, mutableListOf(), mutableSetOf(), mutableMapOf()는 수정 가능한 컬렉션을 반환합니다. 읽기 전용 컬렉션은 변경 메서드가 없어 실수로 수정하는 것을 방지하지만, 내부적으로 Java의 ArrayList 등이므로 캐스팅하면 수정이 가능합니다. 진정한 불변 컬렉션이 필요하면 Guava의 ImmutableList 등을 사용해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 인터페이스와 추상 클래스의 차이점은?', '코틀린에서 인터페이스와 추상 클래스의 차이점은?', 'KOTLIN_CORE',
 'Kotlin 인터페이스는 backing field가 있는 상태를 가질 수 없지만, 추상 프로퍼티나 커스텀 getter를 가진 프로퍼티 및 메서드의 기본 구현을 제공할 수 있습니다. 클래스는 여러 인터페이스를 구현할 수 있지만, 추상 클래스는 하나만 상속할 수 있습니다. 추상 클래스는 상태(프로퍼티)를 가지고, 생성자를 선언할 수 있으며, abstract 키워드로 구현을 강제합니다. 공통 상태나 생성자 로직이 필요하면 추상 클래스를, 타입 계층이나 다형성 계약 정의에는 인터페이스를 우선 고려합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 예외 처리가 Java와 다른 점은 무엇인가요?', '코틀린에서 예외 처리가 자바와 다른 점은 무엇인가요?', 'KOTLIN_CORE',
 'Kotlin에는 Checked Exception이 없습니다. Java에서 IOException, SQLException 등은 반드시 try/catch하거나 throws 선언이 필요하지만, Kotlin은 모든 예외가 Unchecked로 처리됩니다. 이로 인해 불필요한 예외 처리 보일러플레이트가 줄어들지만, 예외가 발생할 수 있는 코드를 명시적으로 문서화하는 것이 중요합니다. @Throws 어노테이션을 사용하면 Java와의 상호운용 시 Checked Exception으로 동작하게 할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 String Template(문자열 템플릿)을 어떻게 사용하나요?', '코틀린에서 String Template(문자열 템플릿)을 어떻게 사용하나요?', 'KOTLIN_CORE',
 'Kotlin의 문자열 템플릿은 $변수명이나 ${표현식}으로 문자열 내에 변수나 표현식을 삽입합니다. Java의 String.format()이나 + 연산보다 간결하고 가독성이 높습니다. 삼중 따옴표(""")로 여러 줄 문자열을 선언하고 trimIndent()로 들여쓰기를 제거할 수 있어 SQL, JSON 등의 멀티라인 문자열 작성에 유용합니다. 복잡한 표현식은 ${user.name.uppercase()} 형태로 중괄호 안에 작성합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Spring Boot에서 Kotlin으로 REST API를 작성할 때의 장점은?', '스프링 부트에서 코틀린으로 레스트 에이피아이를 작성할 때의 장점은?', 'SPRING_KOTLIN',
 'Kotlin의 데이터 클래스로 DTO를 간결하게 선언하고, Null Safety로 NPE 위험을 컴파일 시점에 방지합니다. Builder 패턴 없이도 named argument와 default parameter로 객체를 생성하고, copy()로 불변 DTO를 간편하게 변환합니다. Spring WebFlux와 결합 시 코루틴으로 비동기 코드를 동기처럼 작성할 수 있으며, Kotlin DSL로 타입 안전한 설정을 구성합니다. @SpringBootApplication 등 Spring 어노테이션은 Kotlin에서도 동일하게 사용하며, 기존 Java Spring 코드와 혼용도 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin의 타입 추론(Type Inference)이란 무엇인가요?', '코틀린의 타입 추론(타입 인퍼런스)이란 무엇인가요?', 'KOTLIN_CORE',
 '타입 추론은 컴파일러가 변수나 표현식의 타입을 초기화 값에서 자동으로 결정하는 기능입니다. val name = "Kotlin"은 String 타입으로, val count = 42는 Int 타입으로 추론됩니다. 함수 반환 타입도 단일 표현식 함수(= 구문)에서는 추론됩니다. 그러나 공개 API 함수의 반환 타입은 명시적으로 선언하여 코드 읽는 사람과 API 계약을 명확히 하는 것이 좋습니다. 타입 추론 덕분에 Java보다 훨씬 적은 코드로 동일한 타입 안전성을 확보할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 함수형 프로그래밍 스타일로 컬렉션을 처리하는 방법은?', '코틀린에서 함수형 프로그래밍 스타일로 컬렉션을 처리하는 방법은?', 'FUNCTIONAL',
 'Kotlin은 filter, map, flatMap, reduce, fold, groupBy, sortedBy, take, drop 등 풍부한 컬렉션 처리 함수를 제공합니다. 체이닝으로 데이터 변환 파이프라인을 구성하며, 읽기 쉬운 선언적 코드를 작성할 수 있습니다. asSequence()로 Lazy 시퀀스로 전환하면 대용량 컬렉션에서 중간 결과 컬렉션 생성 없이 최적화된 처리가 가능합니다. 함수 참조(::방식)로 기존 함수를 고차 함수 인수로 전달할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 JPA 엔티티를 설계할 때 주의사항은?', '코틀린에서 제이피에이 엔티티를 설계할 때 주의사항은?', 'SPRING_KOTLIN',
 'JPA는 엔티티에 파라미터 없는 기본 생성자를 요구하는데, Kotlin 클래스는 기본적으로 기본 생성자가 없으므로 kotlin-jpa 플러그인을 사용하거나 직접 선언합니다. Data class는 equals/hashCode를 주 생성자 파라미터 기반으로 생성하므로 id로만 동등성을 비교해야 하는 JPA 엔티티에는 적합하지 않습니다. Kotlin 클래스는 기본 final이므로 kotlin-spring 플러그인이나 open 키워드로 CGLIB 프록시 생성을 허용해야 합니다. @Column(nullable = false)와 Kotlin non-nullable 타입을 일치시켜 DB 제약과 Null Safety를 모두 활용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin의 구조 분해(Destructuring Declaration)를 설명해주세요.', '코틀린의 구조 분해(디스트럭처링 디클러레이션)를 설명해주세요.', 'KOTLIN_CORE',
 '구조 분해는 하나의 객체를 여러 변수로 분해하여 할당하는 문법입니다. data class의 componentN() 메서드를 기반으로 동작하며, val (name, age) = person 처럼 사용합니다. Map.Entry도 구조 분해가 가능하여 for ((key, value) in map) 루프를 간결하게 작성합니다. 함수에서 Pair나 data class를 반환하면 호출자에서 구조 분해로 여러 값을 편리하게 받을 수 있습니다. 필요 없는 값은 _ 언더스코어로 무시할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 inline class(value class)란 무엇인가요?', '코틀린에서 인라인 클래스(벨류 클래스)란 무엇인가요?', 'KOTLIN_CORE',
 '@JvmInline @value class는 단일 프로퍼티를 래핑하는 클래스로, 컴파일 시 런타임에서 래퍼 클래스 인스턴스가 제거되고 내부 타입으로 대체되어 성능 오버헤드가 없습니다. String을 래핑한 UserId(val id: String)를 선언하면 타입 시스템에서 UserId와 String이 구분되어 실수로 일반 String을 UserId 파라미터에 전달하는 것을 방지합니다. 도메인 원시 타입(Primitive Obsession) 문제를 해결하고 타입 안전성을 높이는 데 유용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 operator overloading(연산자 오버로딩)을 어떻게 구현하나요?', '코틀린에서 오퍼레이터 오버로딩(연산자 오버로딩)을 어떻게 구현하나요?', 'KOTLIN_CORE',
 'Kotlin은 operator 키워드로 특정 함수를 연산자에 매핑합니다. plus()는 + 연산자, minus()는 - 연산자, times()는 * 연산자에 해당합니다. equals()/hashCode()는 == 연산자, compareTo()는 <, > 비교 연산자를 지원합니다. 컬렉션의 get()/set()은 [] 인덱스 연산자에 매핑됩니다. contains()는 in 연산자에, invoke()는 () 함수 호출 연산자에 해당합니다. 직관적인 도메인 객체 연산을 표현할 때 유용하지만, 의미가 불명확한 연산자 오버로딩은 가독성을 해칩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 object expression과 람다의 차이는?', '코틀린에서 오브젝트 익스프레션과 람다의 차이는?', 'KOTLIN_CORE',
 'object expression은 특정 인터페이스나 추상 클래스를 즉석에서 구현하는 익명 클래스의 인스턴스를 생성합니다. 람다는 단일 추상 메서드(SAM) 인터페이스를 간결하게 구현하는 함수 리터럴입니다. Kotlin에서는 함수형 인터페이스(fun interface)에 람다를 직접 사용할 수 있으며, Java의 SAM 인터페이스에도 람다를 전달할 수 있습니다. 여러 메서드가 있는 인터페이스 구현에는 object expression이 필요합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 lazy initialization(지연 초기화)을 구현하는 방법은?', '코틀린에서 레이지 이니셜라이제이션(지연 초기화)을 구현하는 방법은?', 'KOTLIN_CORE',
 'by lazy { } 위임은 val 프로퍼티의 값을 처음 접근할 때 딱 한 번만 초기화합니다. 기본적으로 스레드 안전(LazyThreadSafetyMode.SYNCHRONIZED)하지만, 단일 스레드 환경에서는 LazyThreadSafetyMode.NONE으로 동기화 오버헤드를 제거할 수 있습니다. lateinit var는 non-nullable var 프로퍼티의 초기화를 나중으로 미루며, 초기화 전 접근 시 UninitializedPropertyAccessException이 발생합니다. isInitialized로 초기화 여부를 확인할 수 있습니다. by lazy는 불변 싱글톤에, lateinit var는 DI로 주입받는 값에 적합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 Elvis 연산자(?:)의 활용법을 설명해주세요.', '코틀린에서 Elvis 연산자(?:)의 활용법을 설명해주세요.', 'KOTLIN_CORE',
 'Elvis 연산자(?:)는 좌측 표현식이 null이면 우측 값을 반환하고, null이 아니면 좌측 값을 반환합니다. null 기본값 제공(val name = user?.name ?: "Anonymous"), null 시 예외 발생(?: throw IllegalArgumentException()), null 시 early return(val user = findUser() ?: return) 세 가지 패턴으로 주로 활용됩니다. Java의 Optional.orElse()나 삼항 연산자보다 간결하게 null 처리를 표현할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin의 주 생성자(Primary Constructor)와 부 생성자(Secondary Constructor)의 차이는?', '코틀린의 주 생성자(프라이머리 컨스트럭터)와 부 생성자(세컨더리 컨스트럭터)의 차이는?', 'KOTLIN_CORE',
 '주 생성자는 클래스 헤더에 선언되며 class User(val name: String, val email: String) 형태로 프로퍼티 선언과 초기화를 동시에 합니다. init 블록으로 주 생성자 초기화 로직을 추가할 수 있습니다. 부 생성자는 constructor 키워드로 클래스 내부에 선언하며 : this(...)로 반드시 주 생성자에 위임해야 합니다. Kotlin에서는 default parameter를 활용하면 대부분의 경우 부 생성자가 불필요합니다. Java와의 상호운용을 위해 @JvmOverloads를 주 생성자에 선언하면 각 기본값 조합의 오버로드 메서드가 생성됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- ============================================================
-- MID (30문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin 코루틴의 suspend 함수 원리를 설명해주세요.', '코틀린 코루틴의 suspend 함수 원리를 설명해주세요.', 'COROUTINE',
 'suspend 함수는 코루틴 컨텍스트에서만 호출 가능하며, 컴파일 시 Continuation 파라미터가 추가된 상태 머신으로 변환됩니다. 각 suspend 지점에서 함수 실행 상태(로컬 변수, 재개 지점)를 Continuation 객체에 저장하고 스레드를 반환합니다. I/O 완료나 타이머 만료 시 Continuation이 재개되어 저장된 상태에서 실행이 계속됩니다. 이 Continuation Passing Style(CPS) 변환 덕분에 콜백 없이 동기 코드처럼 작성된 비동기 코드가 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'CoroutineScope와 CoroutineContext, Dispatcher의 역할을 설명해주세요.', '코루틴스코프와 코루틴컨텍스트, 디스패처의 역할을 설명해주세요.', 'COROUTINE',
 'CoroutineContext는 코루틴 실행 환경을 구성하는 요소들의 집합으로, Dispatcher, Job, CoroutineExceptionHandler 등이 포함됩니다. Dispatcher는 코루틴이 실행될 스레드를 결정합니다. Dispatchers.Default는 CPU 집약 작업용(공유 스레드 풀), Dispatchers.IO는 I/O 블로킹 작업용(확장 스레드 풀), Dispatchers.Main은 UI 스레드용입니다. CoroutineScope는 코루틴 생명주기를 관리하며 스코프가 취소되면 내부 모든 코루틴이 취소됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin 코루틴에서 Structured Concurrency란 무엇인가요?', '코틀린 코루틴에서 스트럭처드 컨커런시란 무엇인가요?', 'COROUTINE',
 'Structured Concurrency는 코루틴이 항상 스코프 내에서 생성되며, 부모 스코프가 종료될 때까지 자식 코루틴이 완료됨을 보장하는 원칙입니다. 부모 코루틴이 취소되면 모든 자식 코루틴이 취소되어 리소스 누수를 방지합니다. coroutineScope { } 빌더는 내부 코루틴이 모두 완료될 때까지 기다리고, 하나라도 예외가 발생하면 나머지를 취소합니다. GlobalScope 사용은 구조화된 동시성을 무시하므로 가능한 피해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin 코루틴의 취소(Cancellation) 메커니즘과 협력적 취소란?', '코틀린 코루틴의 취소(Cancellation) 메커니즘과 협력적 취소란?', 'COROUTINE',
 '코루틴 취소는 Job.cancel()로 시작하며, 취소 신호는 다음 suspend 지점에서 CancellationException을 던져 전파됩니다. 협력적 취소(Cooperative Cancellation)는 코루틴이 취소에 협력해야 한다는 의미로, suspend 함수 호출이나 isActive 체크 없이 긴 계산 루프를 실행하면 취소가 동작하지 않습니다. 취소 불가능한 블록이 필요하면 withContext(NonCancellable) { }을 사용합니다. finally 블록은 취소 시에도 실행되어 리소스 정리에 활용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin의 Sequence와 일반 컬렉션의 차이점과 성능 비교는?', '코틀린의 Sequence와 일반 컬렉션의 차이점과 성능 비교는?', 'FUNCTIONAL',
 '일반 컬렉션의 filter/map은 즉시 평가(Eager Evaluation)로 각 연산마다 새 컬렉션을 생성합니다. Sequence는 지연 평가(Lazy Evaluation)로 최종 연산(toList, find 등)이 호출될 때 각 요소에 대해 모든 중간 연산을 순차적으로 처리합니다. 대용량 컬렉션에서 중간 컬렉션 생성이 없어 메모리 효율적이고, 최종 연산 전에 조기 종료(find, first)가 가능합니다. 소규모 컬렉션이나 간단한 연산에서는 Sequence 초기화 오버헤드로 오히려 느릴 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Spring WebFlux와 Kotlin 코루틴을 함께 사용하는 방법을 설명해주세요.', '스프링 웹플럭스와 코틀린 코루틴을 함께 사용하는 방법을 설명해주세요.', 'SPRING_KOTLIN',
 'Spring WebFlux 5.2+ 부터 suspend 함수를 컨트롤러 메서드로 사용할 수 있어 Mono/Flux를 직접 다루지 않고도 반응형 API를 구현합니다. @RestController의 핸들러 메서드를 suspend fun으로 선언하면 Spring이 내부적으로 코루틴 어댑터를 통해 Mono로 변환합니다. Flow<T>로 스트리밍 응답을 구현하며, Spring Data R2DBC와 코루틴을 결합하면 완전한 비동기 논블로킹 스택을 구성합니다. withContext(Dispatchers.IO)로 블로킹 코드를 감싸 이벤트 루프 차단을 방지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin의 제네릭 타입 파라미터에서 in/out 공변성(Variance)을 설명해주세요.', '코틀린의 제네릭 타입 파라미터에서 in/out 공변성(Variance)을 설명해주세요.', 'KOTLIN_CORE',
 'out 키워드(공변, Covariance)는 제네릭 타입이 생산자 역할만 하며, T의 서브타입을 허용합니다. List<out Animal>은 List<Dog>을 받을 수 있지만 추가는 불가합니다. in 키워드(반공변, Contravariance)는 소비자 역할만 하며, T의 슈퍼타입을 허용합니다. Comparable<in T>처럼 데이터를 받아 처리하는 경우에 사용합니다. reified 타입 파라미터는 inline 함수에서만 사용 가능하며, 런타임에 타입 정보를 유지하여 is T 타입 체크가 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 DSL(Domain Specific Language)을 구현하는 방법은?', '코틀린에서 DSL(도메인 스페시픽 랭귀지)을 구현하는 방법은?', 'KOTLIN_CORE',
 'Kotlin DSL은 수신자가 있는 람다(lambda with receiver), 확장 함수, infix 함수, 연산자 오버로딩을 조합하여 구현합니다. @DslMarker 어노테이션으로 DSL 스코프를 제한하여 중첩 DSL 빌더에서 외부 빌더 메서드가 잘못 호출되는 것을 방지합니다. Kotlin DSL의 대표 사례는 Gradle Kotlin DSL, Ktor 라우팅, Exposed ORM, Spring Security DSL입니다. 도메인 전문가도 읽을 수 있는 표현력 높은 설정 API를 제공할 때 유용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 Result 타입과 함수형 에러 처리 패턴을 설명해주세요.', '코틀린에서 Result 타입과 함수형 에러 처리 패턴을 설명해주세요.', 'FUNCTIONAL',
 'Result<T>는 성공(Success)과 실패(Failure)를 표현하는 내장 타입으로, runCatching { }으로 예외를 Result로 래핑합니다. getOrElse, getOrThrow, onSuccess, onFailure로 결과를 처리합니다. 함수형 에러 처리를 위해 Arrow 라이브러리의 Either<Error, Value>를 사용하면 왼쪽은 실패, 오른쪽은 성공으로 타입 안전한 에러 처리를 구성합니다. map, flatMap으로 체인을 구성하고, fold로 양쪽 케이스를 처리합니다. 예외 기반보다 명시적인 에러 타입 설계가 API 계약을 명확하게 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 delegation(위임) 패턴 구현 방법을 설명해주세요.', '코틀린에서 delegation(위임) 패턴 구현 방법을 설명해주세요.', 'KOTLIN_CORE',
 'Kotlin은 by 키워드로 인터페이스 구현을 다른 객체에 위임하는 클래스 위임을 지원합니다. class MyList<T>(private val delegate: MutableList<T>) : MutableList<T> by delegate 처럼 선언하면 위임 객체의 모든 메서드를 자동으로 구현합니다. 프로퍼티 위임은 by 키워드로 getValue/setValue 연산자를 구현한 객체에 위임합니다. by lazy, by Delegates.observable, by Delegates.vetoable가 내장 프로퍼티 위임입니다. 이를 통해 상속 없이도 기존 클래스에 기능을 추가하거나 데코레이터 패턴을 간결하게 구현합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 Flow와 SharedFlow, StateFlow의 차이점은?', '코틀린에서 Flow와 SharedFlow, StateFlow의 차이점은?', 'COROUTINE',
 'Flow는 cold 스트림으로 각 collect마다 독립적으로 스트림이 시작됩니다. SharedFlow는 hot 스트림으로 여러 collector가 같은 스트림을 공유하며, replay 파라미터로 새 collector에게 이전 값을 전달합니다. StateFlow는 현재 상태를 유지하는 hot 스트림으로 항상 초기값이 있고, 같은 값 연속 발행 시 skip합니다. UI 상태 관리에는 StateFlow, 이벤트 발행에는 SharedFlow(replay=0), 데이터 처리 파이프라인에는 Flow를 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 Channel과 Flow의 차이와 사용 시나리오는?', '코틀린에서 Channel과 Flow의 차이와 사용 시나리오는?', 'COROUTINE',
 'Channel은 코루틴 간 데이터를 전달하는 통신 프리미티브로, producer-consumer 패턴에 직접 사용합니다. 버퍼 크기(BUFFERED, RENDEZVOUS, UNLIMITED)와 onFull 전략(SUSPEND, DROP_OLDEST, DROP_LATEST)을 설정할 수 있습니다. Flow는 선언적 데이터 스트림으로 operators 체인으로 처리하고 backpressure를 자동으로 처리합니다. 코루틴 간 직접 통신에는 Channel, 데이터 변환 파이프라인에는 Flow를 선호합니다. Channel은 hot이므로 receiver가 없으면 데이터가 쌓이는 반면 Flow는 cold여서 consume 없이 데이터가 생성되지 않습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 테스트 코드 작성 시 코루틴 처리 방법은?', '코틀린에서 테스트 코드 작성 시 코루틴 처리 방법은?', 'COROUTINE',
 'kotlinx-coroutines-test의 runTest { }는 테스트용 코루틴 스코프를 제공하며 delay()를 즉시 건너뛰어 실행 시간 없이 빠르게 테스트합니다. TestCoroutineScheduler로 가상 시간을 제어하여 delay 기반 로직을 단위 테스트합니다. Dispatchers.setMain(testDispatcher)으로 Main 디스패처를 테스트 디스패처로 교체하고, 테스트 후 Dispatchers.resetMain()으로 복원합니다. turbine 라이브러리는 Flow 테스트를 위한 expectItem(), expectComplete(), expectError() 등의 어설션을 제공합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 Exposed ORM 프레임워크 기본 사용법은?', '코틀린에서 익스포즈드 오알엠 프레임워크 기본 사용법은?', 'SPRING_KOTLIN',
 'Exposed는 JetBrains에서 만든 Kotlin 전용 경량 ORM으로 두 가지 API를 제공합니다. DSL 방식은 Table 객체를 정의하고 select, insert, update, deleteWhere 등의 타입 안전한 쿼리를 Kotlin 코드로 작성합니다. DAO 방식은 Entity 클래스로 객체 지향적 DB 접근을 제공합니다. Coroutine 지원을 위해 exposed-kotlin-dao와 함께 newSuspendedTransaction { }을 사용합니다. JPA와 달리 LazyLoading 없이 명시적 쿼리로 동작하므로 N+1 문제가 없고 동작이 예측 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- ============================================================
-- SENIOR (30문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Java Virtual Thread(Project Loom)와 Kotlin 코루틴을 비교 설명해주세요.', '자바 버추얼 스레드(프로젝트 룸)와 코틀린 코루틴을 비교 설명해주세요.', 'COROUTINE',
 'Java Virtual Thread는 JVM 레벨의 경량 스레드로 기존 스레드 블로킹 코드를 변경 없이 사용하면서 높은 동시성을 확보합니다. Kotlin 코루틴은 언어 레벨의 협력적 동시성으로 suspend 함수와 구조화된 동시성을 통해 명시적 비동기 코드를 작성합니다. Virtual Thread는 기존 Java/블로킹 라이브러리와 완벽 호환되고, 코루틴은 더 세밀한 동시성 제어와 취소, Flow 기반 스트리밍이 가능합니다. Spring Boot 3.2+에서 Virtual Thread와 코루틴을 함께 사용할 수 있으며, 두 기술이 배타적이지 않습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin 코루틴의 Job 생명주기와 SupervisorJob의 차이를 설명해주세요.', '코틀린 코루틴의 Job 생명주기와 SupervisorJob의 차이를 설명해주세요.', 'COROUTINE',
 'Job은 New → Active → Completing → Completed, 또는 Cancelling → Cancelled 상태를 거칩니다. 일반 Job에서 자식 코루틴이 예외로 실패하면 형제 코루틴들도 취소됩니다. SupervisorJob은 자식 실패가 부모나 다른 자식에게 전파되지 않아 독립적인 자식 코루틴 관리가 가능합니다. supervisorScope { }로 일시적 SupervisorJob 스코프를 생성하고, 각 자식의 예외를 독립적으로 처리합니다. CoroutineExceptionHandler를 SupervisorJob 스코프에 등록하여 전역 예외를 처리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin Multiplatform에서 코루틴을 공통 코드로 사용할 때의 고려사항은?', '코틀린 Multiplatform에서 코루틴을 공통 코드로 사용할 때의 고려사항은?', 'COROUTINE',
 'Kotlin Multiplatform(KMP)에서 kotlinx-coroutines-core는 JVM, JS, Native 모두에서 동작하지만 플랫폼별 주의사항이 있습니다. iOS(Native)에서는 메인 스레드에서만 코루틴을 관리하거나 strict memory model을 준수해야 합니다. Dispatchers.Main은 플랫폼별로 다르게 구현되며, kotlinx.coroutines.Dispatchers.Main을 사용하면 각 플랫폼에 맞게 동작합니다. expect/actual 메커니즘으로 플랫폼별 디스패처나 스레드 관련 구현을 분리합니다. Flow는 multiplatform에서 동작하며 공통 비즈니스 로직의 데이터 스트림으로 활용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin 코루틴 성능 모니터링과 디버깅 방법을 설명해주세요.', '코틀린 코루틴 성능 모니터링과 디버깅 방법을 설명해주세요.', 'COROUTINE',
 'JVM 옵션 -Dkotlinx.coroutines.debug을 설정하면 스레드 이름에 코루틴 이름이 포함되어 디버깅이 편리합니다. CoroutineName 컨텍스트로 코루틴에 의미있는 이름을 부여합니다. CoroutineScope 내 활성 Job 수를 모니터링하여 리소스 누수를 감지합니다. IntelliJ IDEA의 코루틴 디버거는 일시 중단된 코루틴 스택을 시각화합니다. Micrometer와 통합하여 코루틴 지연 시간, 활성 코루틴 수를 메트릭으로 수집합니다. 성능 테스트 시 Dispatchers.Default의 스레드 풀 포화 여부를 확인하고, CPU 바운드와 I/O 바운드 코루틴의 Dispatcher를 적절히 분리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin의 컴파일러 플러그인과 Symbol Processing(KSP) 활용 방법은?', '코틀린의 컴파일러 플러그인과 Symbol Processing(KSP) 활용 방법은?', 'KOTLIN_CORE',
 'KSP(Kotlin Symbol Processing)는 KAPT(Kotlin Annotation Processing Tool)의 경량 대안으로 Kotlin-native 코드 생성 프레임워크입니다. KAPT가 자바 stubs를 생성하는 방식과 달리 KSP는 Kotlin 타입 시스템을 직접 분석하여 2배 이상 빠릅니다. Room, Moshi, Hilt 등이 KSP를 지원하며, Processor 인터페이스를 구현하여 커스텀 코드 생성기를 작성합니다. KSP로 보일러플레이트 코드(Mapper, Factory, Builder)를 컴파일 타임에 자동 생성하여 런타임 리플렉션 비용을 제거합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin과 Spring을 사용한 함수형 아키텍처(Hexagonal Architecture) 설계 방법은?', '코틀린과 스프링을 사용한 함수형 아키텍처(Hexagonal Architecture) 설계 방법은?', 'SPRING_KOTLIN',
 '헥사고날 아키텍처에서 도메인 모델은 순수 Kotlin 객체(sealed class, data class)로 구성하고 프레임워크 의존성을 배제합니다. Port는 인터페이스(interface)로 정의하고, Adapter(Spring Controller, Repository)는 Port를 구현합니다. 유스케이스는 인터페이스 없이 suspend fun으로 구현하고 Spring @Service로 등록합니다. Kotlin의 sealed class Result<T>로 유스케이스 반환 타입을 모델링하면 성공/실패 처리가 타입 안전합니다. 도메인 코어를 별도 모듈로 분리하면 Spring 없이도 도메인 단위 테스트가 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 Arrow 라이브러리를 활용한 함수형 프로그래밍 패턴은?', '코틀린에서 애로우 라이브러리를 활용한 함수형 프로그래밍 패턴은?', 'FUNCTIONAL',
 'Arrow는 Kotlin에서 함수형 프로그래밍 추상화(Either, Option, IO, Lens, Monad)를 제공하는 라이브러리입니다. Either<Error, Value>로 타입 안전한 에러 처리를 구현하고, 체이닝 시 왼쪽(오류)이 있으면 단락(short-circuit)됩니다. Option<T>는 Kotlin의 nullable 타입과 유사하지만 함수형 조합이 더 용이합니다. Lens와 Prism으로 중첩된 불변 데이터 구조를 타입 안전하게 수정합니다. Arrow의 Saga 패턴 구현체로 분산 트랜잭션의 보상 로직을 선언적으로 정의합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin 기반 마이크로서비스에서 Ktor와 Spring Boot의 선택 기준은?', '코틀린 기반 마이크로서비스에서 케이터와 스프링 부트의 선택 기준은?', 'SPRING_KOTLIN',
 'Ktor는 JetBrains에서 만든 Kotlin-first 비동기 웹 프레임워크로 코루틴이 기본이며 경량화된 마이크로서비스에 적합합니다. Spring Boot보다 시작 시간이 빠르고 메모리 사용량이 낮아 서버리스나 컨테이너 환경에 유리합니다. 단, Spring의 광범위한 생태계(Security, Data, Batch, Cloud)가 없어 기능을 직접 구현해야 하는 경우가 많습니다. 기존 Spring 인프라(팀 경험, 운영 도구)가 있으면 Spring Boot + Kotlin이 생산성이 높고, 완전히 새로운 경량 서비스에는 Ktor를 고려합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 Context Receiver를 활용한 의존성 관리 패턴을 설명해주세요.', '코틀린에서 Context Receiver를 활용한 의존성 관리 패턴을 설명해주세요.', 'KOTLIN_CORE',
 'Context Receiver(Kotlin 1.6 실험 기능)는 함수가 여러 리시버 컨텍스트를 요구함을 선언하는 기능으로, with(ctx) 블록 없이 여러 스코프의 함수를 조합합니다. context(Logger, Database) fun saveUser(user: User) { ... } 형태로 선언하면 두 컨텍스트가 모두 제공된 경우에만 호출 가능합니다. 의존성 주입 없이 타입 시스템으로 런타임 의존성을 표현하는 Effect System 패턴의 Kotlin 구현에 활용됩니다. Spring DI와 결합하면 인터페이스 없이도 의존성 경계를 컴파일 타임에 강제할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin Native와 GraalVM Native Image를 이용한 Spring Boot 네이티브 컴파일의 장단점은?', '코틀린 Native와 그랄브이엠 네이티브 이미지를 이용한 스프링 부트 네이티브 컴파일의 장단점은?', 'SPRING_KOTLIN',
 'Spring Boot 3 + GraalVM Native Image는 AOT(Ahead-of-Time) 컴파일로 JVM 없이 실행되는 네이티브 바이너리를 생성합니다. 시작 시간이 밀리초 단위로 단축되고 메모리 사용량이 크게 감소하여 서버리스와 컨테이너 환경에서 비용 절감 효과가 큽니다. 단, 빌드 시간이 길고(분 단위), 리플렉션/동적 클래스 로딩 사용에 제약이 있어 일부 라이브러리가 네이티브 힌트 추가를 요구합니다. Kotlin 클래스의 final 기본값과 코루틴은 GraalVM과 호환성이 좋으며, Spring AOT Engine이 필요한 힌트를 자동 생성하는 범위가 점차 확대되고 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 대규모 팀 개발 시 코드 품질 관리 전략은?', '코틀린에서 대규모 팀 개발 시 코드 품질 관리 전략은?', 'KOTLIN_CORE',
 'ktlint와 detekt를 CI 파이프라인에 통합하여 코드 스타일 일관성과 코드 품질을 자동 검증합니다. detekt의 커스텀 룰로 프로젝트 아키텍처 규칙(Service가 Repository만 사용하는지 등)을 정적 분석으로 강제합니다. 팀 코딩 컨벤션을 .editorconfig와 intellij formatter 설정으로 공유합니다. API 안정성을 위해 Binary Compatibility Validator를 사용하여 공개 API 변경을 추적합니다. Kotlin API 가이드라인(함수형 스타일 선호, 불변성 우선, null 처리 일관성)을 팀 공유 문서로 관리하고 코드 리뷰에서 지속적으로 피드백합니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- 추가 37문항 (JUNIOR +2, MID +16, SENIOR +19)

-- JUNIOR 추가 2문항

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin의 lazy 프로퍼티 위임이란 무엇이며 어떻게 동작하나요?', '코틀린의 lazy 프로퍼티 위임이란 무엇이며 어떻게 동작하나요?', 'KOTLIN_CORE',
 'by lazy { }는 프로퍼티 초기화를 처음 접근 시까지 지연시키는 위임 패턴입니다. 기본적으로 LazyThreadSafetyMode.SYNCHRONIZED로 동작하여 멀티스레드 환경에서도 초기화가 한 번만 실행됩니다. 성능이 중요한 단일 스레드 환경에서는 LazyThreadSafetyMode.NONE을 사용하여 동기화 오버헤드를 제거할 수 있습니다. 무거운 객체 초기화(DB 연결, 파일 파싱)를 필요한 시점까지 미루어 애플리케이션 시작 시간을 단축하는 데 유용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 String Template과 multiline 문자열 사용법을 설명해주세요.', '코틀린에서 String Template과 multiline 문자열 사용법을 설명해주세요.', 'KOTLIN_CORE',
 'Kotlin String Template은 $ 기호로 변수나 ${표현식}을 문자열에 직접 삽입하는 기능입니다. Java의 문자열 연결(+)보다 가독성이 높고, 내부적으로 StringBuilder로 최적화됩니다. 삼중 따옴표(""" """)로 선언한 Raw String은 이스케이프 없이 줄바꿈과 특수문자를 포함할 수 있어 JSON, SQL, HTML 템플릿 작성에 편리합니다. trimIndent()를 사용하면 들여쓰기를 제거하여 코드 정렬을 유지하면서 깔끔한 문자열을 만들 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- MID 추가 16문항

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin의 inline class(value class)가 무엇이며 어떤 이점이 있나요?', '코틀린의 인라인 클래스(벨류 클래스)가 무엇이며 어떤 이점이 있나요?', 'KOTLIN_CORE',
 'value class는 래퍼 클래스의 타입 안전성을 유지하면서 런타임에 래핑 비용이 없는 경량 클래스입니다. @JvmInline @value class UserId(val id: Long)처럼 단일 프로퍼티를 가진 클래스를 선언하면 컴파일러가 대부분의 사용 위치에서 내부 타입으로 인라인 처리합니다. String이나 Long으로 혼용될 수 있는 도메인 식별자(UserId, OrderId)를 별도 타입으로 구분하여 파라미터 순서 실수를 컴파일 타임에 차단합니다. JPA Entity의 ID 타입이나 도메인 원시 타입(이메일, 전화번호)에 활용하면 타입 안전성과 성능을 동시에 얻을 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 reified 타입 파라미터란 무엇이며 언제 사용하나요?', '코틀린에서 reified 타입 파라미터란 무엇이며 언제 사용하나요?', 'KOTLIN_CORE',
 'Kotlin은 JVM의 타입 소거로 인해 일반적으로 제네릭 타입 파라미터에 런타임에 접근할 수 없습니다. inline 함수에서 reified 키워드를 타입 파라미터에 붙이면 컴파일러가 호출 지점에 코드를 인라인 삽입하여 런타임에 실제 타입 정보를 유지합니다. inline fun <reified T> parseJson(json: String): T = objectMapper.readValue(json, T::class.java)처럼 T::class에 접근할 수 있습니다. Spring의 RestTemplate.getForObject<MyDto>() 같은 확장 함수가 reified를 활용한 대표적 예입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin 코루틴의 Dispatcher 종류와 각각의 사용 시나리오를 설명해주세요.', '코틀린 코루틴의 디스패처 종류와 각각의 사용 시나리오를 설명해주세요.', 'COROUTINE',
 'Dispatchers.Default는 CPU 바운드 작업(정렬, 파싱, 계산)을 위한 디스패처로 CPU 코어 수에 비례한 스레드 풀을 사용합니다. Dispatchers.IO는 블로킹 I/O(파일 읽기, JDBC, 외부 API)를 위한 디스패처로 최대 64개의 스레드로 확장됩니다. Dispatchers.Main은 UI 프레임워크의 메인 스레드에서 실행되며 Android와 JavaFX에서 사용합니다. Dispatchers.Unconfined는 특정 스레드에 묶이지 않아 테스트나 특수 목적에만 사용합니다. Spring WebFlux + 코루틴 환경에서는 withContext(Dispatchers.IO) { }로 블로킹 코드를 감싸야 반응형 스레드 풀을 블로킹하지 않습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 구조화된 동시성(Structured Concurrency)의 개념과 이점을 설명해주세요.', '코틀린에서 구조화된 동시성(스트럭처드 컨커런시)의 개념과 이점을 설명해주세요.', 'COROUTINE',
 '구조화된 동시성은 코루틴의 생명주기가 반드시 정의한 스코프 내에 포함되어야 한다는 원칙입니다. 부모 코루틴이 취소되면 자식 코루틴도 자동으로 취소되어 리소스 누수를 방지합니다. coroutineScope { }는 모든 자식 코루틴이 완료될 때까지 반환하지 않으며, 자식 중 하나가 실패하면 나머지 자식도 취소됩니다. GlobalScope는 구조화된 동시성을 벗어나 애플리케이션 생명주기 전체에서 실행되므로 사용을 피해야 합니다. Spring의 @Bean으로 등록한 CoroutineScope를 사용하면 애플리케이션 종료 시 스코프가 취소되어 정상적으로 정리됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 Flow의 중간 연산자와 종단 연산자의 차이를 설명해주세요.', '코틀린에서 Flow의 중간 연산자와 종단 연산자의 차이를 설명해주세요.', 'COROUTINE',
 '중간 연산자(intermediate operators)는 Flow를 반환하여 파이프라인을 구성하며, 새로운 collector가 구독하기 전까지 실행되지 않습니다. map, filter, flatMapConcat, transform, onEach, buffer, conflate, flowOn이 대표적 중간 연산자입니다. 종단 연산자(terminal operators)는 Flow 수집을 시작하는 suspend 함수로 collect, toList, first, single, fold, reduce가 있습니다. flowOn()은 업스트림의 실행 컨텍스트를 변경하는 중간 연산자로, 데이터 생성과 처리를 다른 Dispatcher에서 실행할 때 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Spring Boot에서 Kotlin @ConfigurationProperties를 사용하는 방법은?', '스프링 부트에서 코틀린 컨피그레이션프로퍼티즈를 어노테이션 사용하는 방법은?', 'SPRING_KOTLIN',
 '@ConfigurationProperties(prefix = "app")와 data class를 함께 사용하면 application.yml의 설정 값을 타입 안전하게 바인딩할 수 있습니다. Spring Boot 2.2+ 이상에서는 @ConstructorBinding과 함께 사용하면 불변(val) 프로퍼티에도 바인딩이 가능합니다. @EnableConfigurationProperties(AppProperties::class) 또는 @ConfigurationPropertiesScan으로 빈을 등록합니다. 중첩 data class로 계층적 설정 구조를 자연스럽게 표현할 수 있으며, kotlin-spring 플러그인이 있으면 별도 open 선언 없이 프록시가 동작합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Spring Data JPA에서 Kotlin과 함께 사용할 때 자주 발생하는 문제와 해결 방법은?', '스프링 데이터 제이피에이에서 코틀린과 함께 사용할 때 자주 발생하는 문제와 해결 방법은?', 'SPRING_KOTLIN',
 'JPA 엔티티는 파라미터 없는 기본 생성자가 필요한데, Kotlin 클래스는 기본 생성자가 없으므로 kotlin-jpa 플러그인이 이를 자동 생성합니다. JPA 엔티티에 data class를 사용하면 equals/hashCode가 id 포함 모든 필드를 비교하여 프록시 객체와 동등성 비교가 깨질 수 있습니다. 지연 로딩(Lazy Loading)을 위해 CGLIB 프록시가 필요한 연관 엔티티 필드는 var로 선언해야 하며 kotlin-allopen 플러그인이 필요합니다. @ManyToOne(fetch = LAZY) 관계 필드는 nullable 타입으로 선언하거나 lateinit var를 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin의 타입 별칭(typealias)과 인라인 클래스의 차이점은?', '코틀린의 타입 별칭(typealias)과 인라인 클래스의 차이점은?', 'KOTLIN_CORE',
 'typealias는 기존 타입에 다른 이름을 부여하는 컴파일 타임 기능으로, 런타임에는 완전히 동일한 타입입니다. typealias UserId = Long으로 선언해도 UserId와 Long은 런타임에 구분되지 않아 혼용이 가능합니다. value class는 런타임에도 별개의 타입(JVM 바이트코드에서 별도 처리)으로 관리되어 타입 안전성을 보장합니다. typealias는 복잡한 함수 타입(typealias Predicate<T> = (T) -> Boolean)이나 제네릭 타입을 단순화하는 목적으로 주로 사용하고, 실질적인 타입 구분이 필요하면 value class를 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 연산자 오버로딩(Operator Overloading)을 어떻게 활용하나요?', '코틀린에서 연산자 오버로딩(오퍼레이터 오버로딩)을 어떻게 활용하나요?', 'KOTLIN_CORE',
 'Kotlin은 operator fun으로 +, -, *, /, %, ==, compareTo, get, set, invoke 등 미리 정해진 연산자를 오버로딩합니다. operator fun plus(other: Money): Money처럼 선언하면 money1 + money2 문법이 동작합니다. 도메인 모델(Money, Duration, Coordinate)에서 수학적 연산이 자연스러울 때 사용하면 가독성이 높아집니다. compareTo를 구현하면 < > <= >= 비교와 정렬이 가능하고, Comparable<T>를 함께 구현하는 것이 권장됩니다. 남용하면 코드 가독성이 저하되므로 직관적인 의미가 없는 타입에는 사용을 자제합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Spring WebFlux와 Kotlin 코루틴을 함께 사용할 때의 패턴을 설명해주세요.', '스프링 웹플럭스와 코틀린 코루틴을 함께 사용할 때의 패턴을 설명해주세요.', 'SPRING_KOTLIN',
 'Spring WebFlux는 반응형(Reactive) 프로그래밍 모델이지만 Kotlin 코루틴 통합을 통해 suspend fun으로 컨트롤러 메서드를 작성할 수 있습니다. @GetMapping suspend fun handler(): ResponseEntity<Dto>처럼 선언하면 Spring이 코루틴을 Mono/Flux로 자동 변환합니다. Mono<T>.awaitSingle(), Flux<T>.asFlow()로 반응형 타입과 코루틴 타입을 상호 변환합니다. R2DBC(반응형 DB 드라이버)와 코루틴을 결합하면 완전한 논블로킹 DB 접근이 가능합니다. 기존 블로킹 코드는 withContext(Dispatchers.IO) { }로 감싸서 반응형 스레드 풀을 보호해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 DSL(Domain-Specific Language)을 어떻게 설계하나요?', '코틀린에서 DSL(Domain-Specific Language)을 어떻게 설계하나요?', 'KOTLIN_CORE',
 'Kotlin DSL은 확장 함수, 람다 with receiver, 연산자 오버로딩을 결합하여 특정 도메인에 최적화된 선언적 API를 구축합니다. fun buildHtml(block: HtmlBuilder.() -> Unit): Html처럼 람다 with receiver를 사용하면 블록 내부에서 수신자의 멤버에 직접 접근할 수 있습니다. @DslMarker 어노테이션으로 DSL 스코프를 표시하면 중첩 DSL에서 외부 스코프 멤버의 암묵적 접근을 방지합니다. Gradle Kotlin DSL, kotlinx.html, Ktor 라우팅이 대표적인 Kotlin DSL 활용 예시입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin과 Spring Security를 결합한 JWT 인증 구현 방법은?', '코틀린과 스프링 시큐리티를 결합한 제이더블유티 인증 구현 방법은?', 'SPRING_KOTLIN',
 'Spring Security 설정을 Kotlin DSL(@Configuration class와 SecurityFilterChain)로 작성하면 Java보다 간결합니다. JwtAuthenticationFilter를 OncePerRequestFilter로 구현하고, 요청 헤더에서 토큰을 추출하여 검증합니다. io.jsonwebtoken(JJWT) 라이브러리를 사용하여 토큰 생성(sign, compact)과 파싱(parseClaimsJws)을 처리합니다. 코루틴 기반 WebFlux 환경에서는 ServerSecurityContextRepository를 구현하여 비동기 인증 컨텍스트를 관리합니다. Kotlin의 확장 함수로 UserDetails에서 커스텀 Claims를 추출하는 유틸을 정의하면 코드 중복을 줄일 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 coroutineScope와 withContext의 차이점은 무엇인가요?', '코틀린에서 코루틴스코프와 withContext의 차이점은 무엇인가요?', 'COROUTINE',
 'coroutineScope { }는 새로운 코루틴 스코프를 생성하고 내부에서 여러 자식 코루틴을 병렬로 실행할 수 있으며, 모든 자식이 완료될 때까지 현재 코루틴을 일시 중단합니다. withContext(Dispatcher) { }는 현재 코루틴의 실행 컨텍스트(주로 Dispatcher)를 전환하며 단일 블록을 다른 Dispatcher에서 실행합니다. coroutineScope는 병렬 분해(parallel decomposition)에, withContext는 컨텍스트 전환(IO 작업 분리)에 사용합니다. 두 함수 모두 suspend 함수이며 구조화된 동시성을 유지하지만, withContext는 launch처럼 새 코루틴을 생성하지 않고 현재 코루틴의 컨텍스트만 교체합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 Nothing 타입은 어떤 경우에 사용되나요?', '코틀린에서 Nothing 타입은 어떤 경우에 사용되나요?', 'KOTLIN_CORE',
 'Nothing은 값이 존재하지 않음을 나타내는 타입으로, 함수가 정상적으로 반환되지 않는다는 것을 컴파일러에 알립니다. throw 표현식의 반환 타입이 Nothing이므로, 엘비스 연산자 오른쪽에서 throw를 사용하면 타입 시스템이 null이 아님을 추론합니다. fun fail(message: String): Nothing = throw IllegalStateException(message)처럼 항상 예외를 던지는 함수의 반환 타입으로 선언합니다. Nothing?은 null만 가능한 타입으로 nullable 타입의 결론 없는 분기에서 나타납니다. when 표현식에서 모든 브랜치가 Nothing을 반환하면 when이 표현식으로 사용되어도 컴파일러가 unreachable 코드를 감지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Spring Boot에서 Kotlin으로 이벤트 기반 아키텍처를 구현하는 방법은?', '스프링 부트에서 코틀린으로 이벤트 기반 아키텍처를 구현하는 방법은?', 'SPRING_KOTLIN',
 'Spring의 ApplicationEvent와 ApplicationEventPublisher를 사용하여 도메인 이벤트를 발행하고 처리합니다. data class로 이벤트 클래스를 정의하면 불변 이벤트를 간결하게 표현할 수 있습니다. @TransactionalEventListener로 트랜잭션 커밋 후 이벤트를 처리하여 데이터 일관성을 보장합니다. 코루틴 환경에서는 coroutineScope가 포함된 @Async 리스너 대신 ApplicationEventPublisher를 suspend fun 내에서 호출하거나 Channel/Flow로 이벤트를 전파합니다. 서비스 간 느슨한 결합을 위해 도메인 이벤트 발행 패턴을 적용하면 서비스 레이어가 단순해집니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- SENIOR 추가 19문항

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 타입 안전한 Builder 패턴을 설계하는 방법을 설명해주세요.', '코틀린에서 타입 안전한 Builder 패턴을 설계하는 방법을 설명해주세요.', 'KOTLIN_CORE',
 'Kotlin DSL 스타일의 Builder는 람다 with receiver와 apply/also 패턴으로 구현합니다. data class를 직접 수정하는 대신 별도의 Builder 클래스에서 mutable 필드를 수집하고, build() 메서드에서 불변 객체를 반환합니다. @DslMarker로 스코프 마커를 정의하면 중첩 빌더에서 외부 빌더 함수가 암묵적으로 호출되는 것을 컴파일 타임에 방지합니다. 필수 파라미터는 빌더 생성자에서 받아 누락 시 컴파일 오류가 발생하도록 하고, 선택 파라미터만 DSL 블록으로 구성합니다. Kotlin의 named argument와 default parameter로 간단한 경우에는 별도 Builder 없이도 동일한 효과를 낼 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin 코루틴에서 예외 처리 전략과 CoroutineExceptionHandler 동작 원리를 설명해주세요.', '코틀린 코루틴에서 예외 처리 전략과 CoroutineExceptionHandler 동작 원리를 설명해주세요.', 'COROUTINE',
 '코루틴 예외는 두 가지 경로로 전파됩니다. launch로 시작된 코루틴의 예외는 부모 스코프로 전파되며 CoroutineExceptionHandler가 처리합니다. async 코루틴의 예외는 Deferred에 저장되고 await() 호출 시점에 발생합니다. CoroutineExceptionHandler는 SupervisorJob 또는 최상위 코루틴 스코프에만 효과가 있으며, 자식 스코프에서는 예외가 부모로 전파되기 때문에 효과가 없습니다. try-catch로 suspend 함수의 예외를 직접 처리하는 것이 가장 명확한 방법이며, runCatching { }은 Result<T>로 예외를 감싸 함수형 스타일로 처리할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 불변 도메인 모델 설계 시 deep copy 전략을 어떻게 적용하나요?', '코틀린에서 불변 도메인 모델 설계 시 deep copy 전략을 어떻게 적용하나요?', 'KOTLIN_CORE',
 'data class의 copy()는 얕은 복사(shallow copy)만 제공하므로, 중첩 객체가 있는 경우 내부 컬렉션까지 재귀적으로 복사해야 진정한 불변 복사본이 됩니다. 중첩된 불변 컬렉션(listOf, mapOf)을 사용하면 내부 요소 변경이 불가능하므로 shallow copy만으로도 안전합니다. 복잡한 도메인 모델에서는 copy() 체이닝(order.copy(items = order.items + newItem))으로 함수형 업데이트를 표현합니다. Arrow 라이브러리의 optics(Lens, Prism)를 사용하면 깊이 중첩된 불변 구조체의 특정 필드를 타입 안전하게 업데이트할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin과 Spring을 사용한 이벤트 소싱(Event Sourcing) 구현 패턴을 설명해주세요.', '코틀린과 스프링을 사용한 이벤트 소싱(Event Sourcing) 구현 패턴을 설명해주세요.', 'SPRING_KOTLIN',
 'Kotlin sealed class로 도메인 이벤트 계층을 선언하면 when 표현식으로 모든 이벤트 타입을 망라적으로 처리할 수 있습니다. Aggregate Root는 apply(event: DomainEvent) 메서드로 상태를 갱신하고, 미커밋 이벤트를 내부 목록에 쌓습니다. Spring Data JPA의 @DomainEvents와 @AfterDomainEventPublication으로 저장 후 이벤트를 자동 발행합니다. 이벤트는 append-only 테이블에 JSON으로 저장하고, aggregate_id + sequence_number로 이벤트를 재생하여 현재 상태를 복원합니다. Snapshot 전략으로 N개 이벤트마다 상태를 저장하여 재생 비용을 O(1)에 가깝게 유지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin 기반 서비스의 그레이스풀 셧다운(Graceful Shutdown)을 구현하는 방법은?', '코틀린 기반 서비스의 그레이스풀 셧다운(Graceful Shutdown)을 구현하는 방법은?', 'SPRING_KOTLIN',
 'Spring Boot 2.3+의 server.shutdown=graceful 설정으로 HTTP 요청 처리 중인 요청이 완료될 때까지 종료를 지연합니다. 코루틴 기반 애플리케이션에서는 @PreDestroy 또는 SmartLifecycle에서 CoroutineScope를 cancel하여 실행 중인 코루틴이 취소 신호를 받도록 합니다. Kafka Consumer는 pause() → drain → close() 순서로 정상 종료하여 메시지 손실을 방지합니다. Job.cancelAndJoin()으로 모든 활성 코루틴이 완료될 때까지 대기합니다. Kubernetes의 terminationGracePeriodSeconds와 스프링의 spring.lifecycle.timeout-per-shutdown-phase를 동기화하여 Pod 종료 타임아웃 내에 셧다운이 완료되도록 설계합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 타입 클래스(Typeclass) 패턴을 모방하는 방법과 실용적 활용 사례는?', '코틀린에서 타입 클래스(Typeclass) 패턴을 모방하는 방법과 실용적 활용 사례는?', 'FUNCTIONAL',
 '타입 클래스는 Haskell에서 유래한 패턴으로, Kotlin에서는 인터페이스와 Companion Object, 확장 함수를 조합하여 ad-hoc 다형성을 구현합니다. interface Eq<A> { fun A.eq(other: A): Boolean }처럼 인터페이스를 정의하고, 특정 타입에 대한 구현을 object로 제공합니다. Arrow kt의 typeclasses(Functor, Monad, Semigroup)가 대표적 구현체입니다. 실용적 활용으로 직렬화 전략(Serializable<T>), 동등성 정의(Eq<T>), 정렬 기준(Ord<T>)을 타입에 따라 별도로 제공하여 확장에 열려있는 설계를 구현합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Spring Boot + Kotlin 애플리케이션의 메모리 최적화 전략을 설명해주세요.', '스프링 부트 + 코틀린 애플리케이션의 메모리 최적화 전략을 설명해주세요.', 'SPRING_KOTLIN',
 'JVM 시작 시 -XX:+UseG1GC 또는 -XX:+UseZGC를 선택하고, -Xms와 -Xmx를 동일하게 설정하여 힙 리사이징 오버헤드를 제거합니다. Spring Bean의 초기화를 lazy로 변경(spring.main.lazy-initialization=true)하면 사용하지 않는 Bean이 메모리를 차지하지 않습니다. 코루틴은 스레드 대비 매우 적은 메모리를 사용하지만, 대용량 데이터를 코루틴 컨텍스트에 보관하면 누수가 발생할 수 있습니다. Spring Data의 Pageable로 대용량 쿼리를 페이지로 처리하고, Sequence를 활용한 지연 평가로 컬렉션 중간 객체 생성을 최소화합니다. Kotlin의 object 싱글톤과 companion object는 클래스 로더 생명주기 동안 메모리를 점유하므로 상태를 최소화합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin 코루틴 기반의 분산 락(Distributed Lock) 구현 방법은?', '코틀린 코루틴 기반의 분산 락(Distributed Lock) 구현 방법은?', 'COROUTINE',
 'Redis의 SET NX PX 명령(Redlock 알고리즘)으로 분산 락을 구현하고, Spring Data Redis의 Lettuce 클라이언트를 코루틴 어댑터로 래핑하여 suspend 함수로 락 획득/해제를 처리합니다. 락 획득 함수를 withLock(key, ttl) { } DSL로 추상화하면 락 해제 누락을 방지합니다. 코루틴 취소 시에도 락이 해제되도록 try-finally 블록으로 해제 로직을 보호합니다. 락 갱신(refresh) 로직을 백그라운드 코루틴으로 구현하여 작업 시간이 TTL을 초과할 때 락이 만료되지 않도록 합니다. 단일 Redis 노드 장애에 대비하여 RedLock(3~5개 Redis 인스턴스)을 사용하는 것이 프로덕션 권장 사항입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 함수형 도메인 모델링의 장점과 구현 패턴은?', '코틀린에서 함수형 도메인 모델링의 장점과 구현 패턴은?', 'FUNCTIONAL',
 '함수형 도메인 모델링은 도메인 상태를 불변 value object로 표현하고, 상태 전이를 순수 함수로 정의하여 부작용을 격리합니다. Kotlin sealed class로 도메인 상태 머신(OrderStatus.Pending, OrderStatus.Confirmed)을 표현하면 유효하지 않은 상태 전이를 컴파일 타임에 차단합니다. Result<T, E>나 Either<Error, Value>로 도메인 규칙 위반을 예외 없이 반환 타입으로 명시합니다. 도메인 로직을 프레임워크 의존성 없는 순수 함수로 작성하면 Spring 없이도 빠른 단위 테스트가 가능합니다. 부작용(DB 저장, 이벤트 발행)을 함수 외부로 밀어내는 Ports & Adapters 패턴과 자연스럽게 결합됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin과 Spring을 사용한 멀티모듈 프로젝트 구조 설계 원칙은?', '코틀린과 스프링을 사용한 멀티모듈 프로젝트 구조 설계 원칙은?', 'SPRING_KOTLIN',
 'Gradle 멀티모듈로 domain, application, infrastructure, api 레이어를 독립 모듈로 분리합니다. domain 모듈은 순수 Kotlin 코드만 포함하고 Spring 의존성을 가지지 않아 빠른 단위 테스트가 가능합니다. infrastructure 모듈이 domain 인터페이스(Port)를 구현하고, api 모듈이 application 레이어를 통해 유스케이스를 호출합니다. 모듈 간 의존성 방향을 단방향으로 강제하고, buildSrc나 convention plugin으로 각 모듈의 공통 Gradle 설정을 중앙화합니다. 빌드 캐시와 병렬 빌드를 활성화하여 대규모 멀티모듈에서도 빌드 시간을 단축합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin 코루틴과 Spring Batch를 결합한 대용량 데이터 처리 전략은?', '코틀린 코루틴과 스프링 배치를 결합한 대용량 데이터 처리 전략은?', 'SPRING_KOTLIN',
 'Spring Batch의 ItemReader/ItemProcessor/ItemWriter를 코루틴과 결합하여 I/O 대기 시간을 최소화합니다. 여러 파티션을 coroutineScope 내 async로 병렬 처리하면 파티션 수만큼 처리량이 향상됩니다. Flow.chunked(chunkSize)로 대용량 스트림을 청크 단위로 처리하고, buffer() 연산자로 생산/소비 속도 차이를 완충합니다. 코루틴의 취소 메커니즘을 활용하여 Job 중단 요청 시 현재 청크를 완료한 후 안전하게 종료합니다. 처리 결과를 Channel로 집계하고 Dispatchers.Default와 Dispatchers.IO를 분리하여 CPU 처리와 DB 쓰기를 파이프라인으로 구성합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 Continuation Passing Style(CPS)과 suspend 함수의 내부 구현 원리는?', '코틀린에서 컨티뉴에이션 패싱 스타일(씨피에스)과 suspend 함수의 내부 구현 원리는?', 'COROUTINE',
 'Kotlin 컴파일러는 suspend 함수를 CPS(Continuation Passing Style)로 변환하여 JVM 바이트코드로 컴파일합니다. suspend fun foo(): T는 컴파일 시 fun foo(continuation: Continuation<T>): Any?로 변환되며, Continuation은 코루틴의 나머지 실행 흐름과 현재 상태를 담은 스택 프레임입니다. 함수가 일시 중단될 때 COROUTINE_SUSPENDED 특수 값을 반환하고, 재개 시 continuation.resumeWith(result)를 호출하여 중단 지점부터 실행을 재개합니다. 이 State Machine 기반 변환이 코루틴이 스레드를 점유하지 않고도 중단/재개가 가능한 핵심 원리입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin 기반 서비스에서 Saga 패턴과 보상 트랜잭션을 구현하는 방법은?', '코틀린 기반 서비스에서 사가 패턴과 보상 트랜잭션을 구현하는 방법은?', 'SPRING_KOTLIN',
 'Orchestration Saga에서 Saga Orchestrator는 각 단계의 성공/실패에 따라 다음 커맨드 또는 보상 커맨드를 발행합니다. Kotlin sealed class로 SagaStep(Pending, Succeeded, Compensating, Failed)을 표현하면 단계 상태 전이를 타입 안전하게 관리합니다. 각 단계의 실행과 보상 함수를 쌍으로 정의하고 Stack에 쌓아, 실패 시 역순으로 보상을 실행합니다. Kafka 또는 Spring ApplicationEvent로 단계 이벤트를 발행하고, @TransactionalEventListener로 트랜잭션 커밋 후 이벤트를 전파합니다. Saga 상태를 DB에 영속화하여 서비스 재시작 후에도 중단된 Saga를 재개합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 컴파일 타임 의존성 검증을 위한 코드 설계 패턴은?', '코틀린에서 컴파일 타임 의존성 검증을 위한 코드 설계 패턴은?', 'KOTLIN_CORE',
 '타입 시스템을 활용하여 잘못된 상태가 생성되지 못하게 하는 "Make Illegal States Unrepresentable" 원칙을 Kotlin sealed class로 구현합니다. 인터페이스와 제네릭으로 상태 전이를 타입 레벨에서 강제합니다(예: fun confirm(order: Order.Draft): Order.Confirmed). 빌더 패턴의 필수 파라미터를 타입으로 인코딩(Step1, Step2 인터페이스 체인)하면 불완전한 빌더 사용을 컴파일 타임에 차단합니다. detekt 커스텀 룰과 KSP 프로세서로 아키텍처 규칙(Service가 다른 Service를 직접 호출하지 않음, Repository를 Controller에서 직접 사용하지 않음)을 정적 분석으로 강제합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 캐싱 전략과 Spring Cache 추상화를 코루틴 환경에서 적용하는 방법은?', '코틀린에서 캐싱 전략과 스프링 캐시 추상화를 코루틴 환경에서 적용하는 방법은?', 'SPRING_KOTLIN',
 'Spring의 @Cacheable은 기본적으로 코루틴을 지원하지 않으므로, suspend fun에 직접 적용 시 Proxy 처리가 어렵습니다. CacheManager를 직접 주입하여 캐시 조회/저장 로직을 suspend 함수 내에서 명시적으로 구현하는 방법이 안정적입니다. Redis 기반 캐시는 Spring Data Redis의 코루틴 어댑터(ReactiveRedisTemplate.awaitFirstOrNull())를 사용합니다. Cache-Aside 패턴을 코루틴 확장 함수로 추상화하면 getOrPut(key) { expensiveQuery() } 형태의 DSL을 구성할 수 있습니다. 분산 캐시 환경에서 Cache Stampede를 방지하기 위해 Mutex나 원자적 조건부 SET NX를 활용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin의 인라인 함수(inline function)가 성능에 미치는 영향과 주의사항을 설명해주세요.', '코틀린의 인라인 함수(inline function)가 성능에 미치는 영향과 주의사항을 설명해주세요.', 'KOTLIN_CORE',
 'inline 함수는 호출 지점에 함수 본문이 복사 삽입되어 함수 호출 오버헤드와 람다 객체 생성 비용이 제거됩니다. 특히 고차 함수에서 람다를 매번 익명 클래스로 박싱하는 JVM 비용을 없애므로 반복 호출 코드 경로에서 효과적입니다. reified 타입 파라미터는 inline 함수에서만 사용 가능합니다. 단점으로 인라인 함수의 본문이 모든 호출 지점에 복사되므로 큰 함수를 inline으로 선언하면 바이트코드 크기가 증가합니다. noinline 파라미터는 인라인 처리를 특정 람다에서 제외하고, crossinline은 non-local return을 허용하지 않는 람다에 적용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin 코루틴으로 대용량 실시간 스트리밍 API를 구현하는 방법은?', '코틀린 코루틴으로 대용량 실시간 스트리밍 에이피아이를 구현하는 방법은?', 'COROUTINE',
 'Spring WebFlux + Kotlin 코루틴에서 Flux<ServerSentEvent<T>>를 Flow<T>로 반환하면 SSE(Server-Sent Events) 스트리밍 API를 구현할 수 있습니다. callbackFlow { }로 외부 이벤트 소스(WebSocket, Redis Pub/Sub, Kafka)를 Flow로 변환합니다. buffer(Channel.BUFFERED)로 생산 속도와 소비 속도 차이를 완충하고, conflate()로 느린 소비자에게 최신 값만 전달하는 backpressure 전략을 적용합니다. 클라이언트 연결 해제 시 Flow가 취소되어 업스트림 구독도 자동으로 해제됩니다. 대규모 팬아웃은 SharedFlow를 허브로 활용하여 단일 소스 스트림을 다수 클라이언트에 효율적으로 분배합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 결과 타입(Result<T>)을 활용한 에러 처리 아키텍처를 설명해주세요.', '코틀린에서 결과 타입(Result<T>)을 활용한 에러 처리 아키텍처를 설명해주세요.', 'KOTLIN_CORE',
 'Kotlin 표준 라이브러리의 Result<T>는 성공값 또는 예외를 감싸는 컨테이너로, runCatching { }으로 예외를 Result로 변환합니다. map, flatMap, recover, onFailure 등의 변환 함수로 예외 전파 없이 파이프라인을 구성합니다. 도메인 에러를 표현하기 위해 sealed class DomainError + Either<DomainError, T>를 사용하면 예외 없이 실패 사유를 타입으로 전달합니다. 서비스 레이어에서 Either를 반환하고, 컨트롤러에서 fold { left -> errorResponse } { right -> successResponse }로 HTTP 응답을 결정합니다. 이 패턴은 예외 계층을 명시적으로 드러내어 호출자가 반드시 에러를 처리하도록 강제합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 Zero-Cost Abstraction을 달성하기 위한 기법과 사례를 설명해주세요.', '코틀린에서 제로 코스트 앱스트랙션을 달성하기 위한 기법과 사례를 설명해주세요.', 'KOTLIN_CORE',
 'Zero-Cost Abstraction은 추상화 비용이 직접 구현과 동일한 수준임을 의미합니다. Kotlin에서 inline 함수로 고차 함수 비용을 제거하고, value class로 래퍼 클래스 박싱 비용을 제거합니다. const val은 컴파일 타임에 인라인되어 런타임 프로퍼티 접근 비용이 없습니다. Sequence와 Flow의 지연 평가는 중간 컬렉션 생성 없이 파이프라인을 처리하여 메모리 효율을 높입니다. 코루틴의 CPS 변환은 State Machine으로 컴파일되어 별도 스레드 없이 동시성을 구현하는 Zero-Cost Abstraction의 대표 예입니다. detekt의 성능 관련 룰셋으로 런타임 비용이 높은 패턴을 정적 분석으로 감지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- MID 보완 1문항 (총 30문항 달성)

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:KOTLIN_SPRING:LANGUAGE_FRAMEWORK', 'Kotlin에서 Sequence와 List의 차이점과 Sequence를 사용해야 하는 경우는?', '코틀린에서 Sequence와 List의 차이점과 Sequence를 사용해야 하는 경우는?', 'KOTLIN_CORE',
 'List는 즉시 평가(eager evaluation)로 각 중간 연산마다 새로운 컬렉션을 생성합니다. Sequence는 지연 평가(lazy evaluation)로 종단 연산이 호출될 때 각 요소를 파이프라인을 통해 하나씩 처리하며 중간 컬렉션을 생성하지 않습니다. 요소 수가 많거나 중간 연산이 많을 때 Sequence가 메모리와 성능에 유리합니다. asSequence()로 기존 컬렉션을 Sequence로 변환하고, generateSequence()로 무한 Sequence를 만들 수 있습니다. 단, 요소 수가 적거나 연산이 단순할 때는 Sequence의 래핑 오버헤드가 오히려 느릴 수 있으므로 벤치마크로 확인하는 것이 좋습니다.',
 'MODEL_ANSWER', TRUE, NOW());
