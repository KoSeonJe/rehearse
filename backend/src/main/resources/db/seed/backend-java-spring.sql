-- backend-java-spring.sql
-- Java/Spring Boot 백엔드 면접 질문 Pool 시딩
-- 총 90문항: JUNIOR 30 / MID 30 / SENIOR 30
-- cache_key: BACKEND:{Level}:JAVA_SPRING:LANGUAGE_FRAMEWORK

-- ============================================================
-- JUNIOR (30문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring Boot와 Spring Framework의 차이점은 무엇인가요?', '스프링 부트와 스프링 프레임워크의 차이점은 무엇인가요?', 'SPRING_BOOT',
 'Spring Framework는 IoC 컨테이너, AOP, MVC 등 핵심 기능을 제공하는 기반 프레임워크이며, 개발자가 직접 의존성과 설정을 구성해야 합니다. Spring Boot는 Spring Framework 위에서 Auto Configuration, Starter 의존성, Embedded Server 등을 통해 설정을 최소화하고 빠른 개발을 가능하게 합니다. 즉 Spring Boot는 Spring Framework의 애플리케이션 개발 편의성을 극대화한 상위 계층 도구입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', '@SpringBootApplication 어노테이션은 어떤 역할을 하나요?', '스프링부트애플리케이션 어노테이션은 어떤 역할을 하나요?', 'SPRING_BOOT',
 '@SpringBootApplication은 @SpringBootConfiguration, @EnableAutoConfiguration, @ComponentScan 세 어노테이션을 합친 메타 어노테이션입니다. @ComponentScan이 현재 패키지 하위의 컴포넌트를 스캔하고, @EnableAutoConfiguration이 classpath 기반으로 Bean을 자동 구성하며, @SpringBootConfiguration은 @Configuration을 상속하여 설정 클래스임을 선언합니다. 이를 통해 메인 클래스 하나만으로 Spring 애플리케이션이 구동됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'IoC(Inversion of Control)와 DI(Dependency Injection)의 개념을 설명해주세요.', '아이오씨와 디아이의 개념을 설명해주세요.', 'SPRING_CORE',
 'IoC는 객체의 생성과 생명주기 관리를 개발자가 아닌 프레임워크(컨테이너)가 담당하는 설계 원칙입니다. DI는 IoC를 구현하는 방법 중 하나로, 객체가 직접 의존 객체를 생성하는 대신 외부에서 주입받는 방식입니다. Spring 컨테이너가 Bean을 생성하고 의존관계를 주입함으로써 객체 간 결합도를 낮추고 테스트 용이성을 높입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring Bean의 생명주기를 설명해주세요.', '스프링 빈의 생명주기를 설명해주세요.', 'SPRING_CORE',
 'Spring Bean은 컨테이너 시작 시 인스턴스 생성 → 의존관계 주입 → 초기화 콜백(@PostConstruct 또는 afterPropertiesSet) → 사용 → 소멸 콜백(@PreDestroy 또는 destroy) → 소멸 순서로 관리됩니다. 컨테이너가 종료될 때 Scope에 따라 소멸 처리가 달라지며, Singleton Bean은 컨테이너 종료 시 함께 소멸됩니다. 초기화/소멸 메서드를 활용해 리소스 연결이나 해제 로직을 배치할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', '@Autowired와 생성자 주입의 차이점은 무엇이며, 생성자 주입이 권장되는 이유는 무엇인가요?', '오토와이어드 어노테이션과 생성자 주입의 차이점은 무엇이며, 생성자 주입이 권장되는 이유는 무엇인가요?', 'SPRING_CORE',
 '@Autowired 필드 주입은 편리하지만 final 키워드를 사용할 수 없어 불변성을 보장하지 못하고, 리플렉션 기반이라 테스트 시 Mock 주입이 어렵습니다. 생성자 주입은 객체 생성 시점에 의존성이 주입되어 불변 객체 생성이 가능하고, 순환 참조를 컴파일/기동 시점에 감지할 수 있습니다. 또한 테스트 코드에서 new 키워드로 직접 의존성을 전달할 수 있어 테스트 용이성이 높아지므로 Spring에서 공식적으로 생성자 주입을 권장합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring Boot Auto Configuration은 어떻게 동작하나요?', '스프링 부트 오토 컨피그레이션은 어떻게 동작하나요?', 'SPRING_BOOT',
 'Auto Configuration은 @EnableAutoConfiguration이 활성화되면 META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports(구버전은 spring.factories)에 등록된 구성 클래스를 로드합니다. 각 구성 클래스는 @ConditionalOnClass, @ConditionalOnMissingBean 등의 조건 어노테이션으로 해당 라이브러리 또는 Bean 존재 여부를 확인한 후 자동 Bean을 등록합니다. 개발자가 직접 Bean을 등록하면 @ConditionalOnMissingBean에 의해 자동 구성 Bean은 등록되지 않아 커스터마이징이 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'application.yml과 application.properties의 차이는 무엇인가요?', 'application.와이엠엘과 application.프로퍼티즈의 차이는 무엇인가요?', 'SPRING_BOOT',
 '두 파일 모두 Spring Boot 애플리케이션 설정을 위한 파일이지만, application.yml은 YAML 형식으로 계층 구조를 들여쓰기로 표현하여 가독성이 높고 중복을 줄일 수 있습니다. application.properties는 key=value 형식의 단순 구조로 익숙하고 배우기 쉽습니다. 실무에서는 복잡한 설정이 많아질수록 yml이 선호되며, 두 파일을 혼용할 경우 Spring Boot 버전에 따라 우선순위가 다를 수 있으므로, 프로젝트에서 하나의 형식만 사용하는 것이 혼란을 방지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', '@Configuration과 @Component의 차이점은 무엇인가요?', '컨피그레이션 어노테이션과 컴포넌트 어노테이션의 차이점은 무엇인가요?', 'SPRING_CORE',
 '@Component는 일반 Bean 등록 어노테이션으로 컴포넌트 스캔 대상이 됩니다. @Configuration은 @Component를 포함하면서 추가로 CGLIB 프록시로 래핑되어, 클래스 내부에서 @Bean 메서드를 직접 호출해도 싱글톤 보장이 됩니다. 즉 @Configuration 내의 @Bean 메서드들은 컨테이너를 통해 관리되므로 매번 새 인스턴스를 반환하지 않지만, @Component 내에서 @Bean 메서드를 직접 호출하면 새 객체가 생성됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring Boot의 내장 톰캣(Embedded Tomcat)은 무엇이며 왜 사용하나요?', '스프링 부트의 내장 톰캣은 무엇이며 왜 사용하나요?', 'SPRING_BOOT',
 '내장 톰캣은 별도 웹서버 설치 없이 JAR 파일 하나로 실행 가능한 독립 실행형 애플리케이션을 만들 수 있게 해줍니다. spring-boot-starter-web 의존성을 추가하면 Tomcat이 자동으로 포함되어 java -jar 명령어만으로 서버가 구동됩니다. 이는 개발 환경 구성을 단순화하고, Docker 컨테이너 기반 배포에서도 서버 설치 없이 이미지를 구성할 수 있어 클라우드 환경에 적합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring Boot Starter란 무엇인가요?', '스프링 부트 스타터란 무엇인가요?', 'SPRING_BOOT',
 'Spring Boot Starter는 특정 기능을 사용하기 위한 의존성 묶음(BOM)입니다. 예를 들어 spring-boot-starter-web 하나만 추가하면 Spring MVC, Embedded Tomcat, Jackson 등 필요한 라이브러리들이 호환 버전으로 함께 추가됩니다. 개발자가 버전 충돌을 직접 관리할 필요 없이 Starter가 검증된 의존성 조합을 제공하여 생산성을 높입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', '@RequestMapping 어노테이션의 역할과 사용법을 설명해주세요.', '리퀘스트매핑 어노테이션의 역할과 사용법을 설명해주세요.', 'SPRING_MVC',
 '@RequestMapping은 HTTP 요청 URL, 메서드, 헤더 등을 컨트롤러 메서드에 매핑하는 어노테이션입니다. 클래스 레벨에 선언하면 공통 URL 접두사를 지정하고, 메서드 레벨에 선언하면 세부 경로와 HTTP 메서드를 지정합니다. 실무에서는 메서드별로 @GetMapping, @PostMapping, @PutMapping, @DeleteMapping 등 전용 어노테이션을 사용하는 것이 가독성 측면에서 더 권장됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', '@RestController와 @Controller의 차이점은 무엇인가요?', '레스트컨트롤러 어노테이션과 컨트롤러 어노테이션의 차이점은 무엇인가요?', 'SPRING_MVC',
 '@Controller는 뷰(View)를 반환하는 전통적인 MVC 컨트롤러로, 반환값이 ViewResolver를 통해 HTML 페이지로 렌더링됩니다. @RestController는 @Controller와 @ResponseBody를 합친 어노테이션으로, 모든 메서드의 반환값이 자동으로 JSON/XML 형태로 HTTP 응답 바디에 직렬화됩니다. REST API를 개발할 때는 @RestController를 사용하고, 서버사이드 렌더링이 필요한 경우 @Controller를 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', '@ResponseBody 어노테이션은 어떤 역할을 하나요?', '리스폰스바디 어노테이션은 어떤 역할을 하나요?', 'SPRING_MVC',
 '@ResponseBody는 컨트롤러 메서드의 반환값을 HTTP 응답 바디에 직접 쓰도록 지시하는 어노테이션입니다. Spring은 HttpMessageConverter를 사용하여 반환 객체를 JSON, XML 등의 형식으로 직렬화합니다. @RestController를 사용하면 클래스 내 모든 메서드에 @ResponseBody가 적용된 것과 동일하므로, REST API 컨트롤러에서는 개별 메서드마다 선언할 필요가 없습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'DispatcherServlet의 역할과 Spring MVC 요청 처리 흐름을 설명해주세요.', '디스패처서블릿의 역할과 스프링 엠브이씨 요청 처리 흐름을 설명해주세요.', 'SPRING_MVC',
 'DispatcherServlet은 Spring MVC의 프론트 컨트롤러로, 모든 HTTP 요청을 받아 적절한 핸들러(컨트롤러)로 라우팅합니다. 요청이 들어오면 HandlerMapping으로 컨트롤러를 찾고, HandlerAdapter로 메서드를 호출하며, 반환값을 ViewResolver로 처리하거나 @ResponseBody로 직접 응답합니다. 이 중앙집중식 구조 덕분에 인터셉터, 예외처리, 메시지 변환 등 공통 처리를 한 곳에서 관리할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', '@PathVariable과 @RequestParam의 차이점은 무엇인가요?', '패스배리어블 어노테이션과 리퀘스트파람 어노테이션의 차이점은 무엇인가요?', 'SPRING_MVC',
 '@PathVariable은 URL 경로 변수(예: /users/{id})에서 값을 추출하며 RESTful URL 설계에 주로 사용됩니다. @RequestParam은 쿼리 스트링(예: /users?name=kim)이나 form 데이터에서 값을 추출합니다. 리소스 식별에는 @PathVariable을, 필터링이나 페이지네이션 같은 선택적 파라미터에는 @RequestParam을 사용하는 것이 REST 설계 관례에 부합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', '@Valid 어노테이션을 이용한 입력 검증은 어떻게 동작하나요?', '밸리드 어노테이션을 이용한 입력 검증은 어떻게 동작하나요?', 'SPRING_MVC',
 '@Valid는 Jakarta Bean Validation을 활성화하는 어노테이션으로, DTO 필드에 선언된 @NotNull, @Size, @Email 등의 제약 조건을 컨트롤러 진입 전에 검증합니다. 검증 실패 시 MethodArgumentNotValidException이 발생하며, @ExceptionHandler나 @ControllerAdvice로 일관된 오류 응답을 반환할 수 있습니다. 서버 경계에서 입력 데이터를 검증함으로써 비즈니스 로직 계층으로 잘못된 데이터가 유입되는 것을 방지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', '@ExceptionHandler와 @ControllerAdvice를 이용한 전역 예외 처리를 설명해주세요.', '익셉션핸들러 어노테이션과 컨트롤러어드바이스 어노테이션을 이용한 전역 예외 처리를 설명해주세요.', 'SPRING_MVC',
 '@ExceptionHandler는 특정 컨트롤러 내에서 발생한 예외를 처리하는 메서드에 선언합니다. @ControllerAdvice는 모든 컨트롤러에 걸친 전역 예외 처리 클래스에 선언하며, 내부에 @ExceptionHandler 메서드를 정의하면 애플리케이션 전체 예외를 한 곳에서 처리할 수 있습니다. 이를 통해 각 컨트롤러에 중복된 예외 처리 코드를 제거하고 일관된 에러 응답 포맷을 유지할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring 프로파일(Profile)이란 무엇이며 어떻게 활용하나요?', '스프링 프로파일이란 무엇이며 어떻게 활용하나요?', 'SPRING_BOOT',
 'Spring 프로파일은 환경(개발/테스트/운영)에 따라 다른 설정이나 Bean을 활성화하는 기능입니다. application-dev.yml, application-prod.yml처럼 프로파일별 설정 파일을 분리하거나, @Profile("dev") 어노테이션으로 특정 환경에서만 Bean을 등록할 수 있습니다. 활성 프로파일은 spring.profiles.active 속성이나 환경 변수로 지정하며, 이를 통해 데이터베이스 연결 정보, 외부 API URL 등 환경별 설정을 안전하게 관리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Lombok이란 무엇이며 주요 어노테이션을 설명해주세요.', '롬복이란 무엇이며 주요 어노테이션을 설명해주세요.', 'SPRING_BOOT',
 'Lombok은 Java 보일러플레이트 코드(getter, setter, toString, equals 등)를 어노테이션으로 자동 생성하는 라이브러리입니다. @Getter/@Setter는 접근자 메서드를, @AllArgsConstructor/@NoArgsConstructor는 생성자를, @Builder는 빌더 패턴을, @Data는 getter/setter/toString/equals 모두를 생성합니다. 코드 간결성을 높이지만, @Data의 경우 불필요한 setter까지 생성되므로 JPA Entity에서는 @Getter와 필요한 생성자만 선택적으로 사용하는 것이 권장됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'HTTP 상태 코드 200, 201, 400, 401, 403, 404, 500의 의미를 설명해주세요.', '에이치티티피 상태 코드 200, 201, 400, 401, 403, 404, 500의 의미를 설명해주세요.', 'SPRING_MVC',
 '200 OK는 성공적인 요청, 201 Created는 리소스 생성 성공을 의미합니다. 400 Bad Request는 클라이언트의 잘못된 요청(입력 검증 실패 등), 401 Unauthorized는 인증 미완료, 403 Forbidden은 인증됐지만 권한 없음을 나타냅니다. 404 Not Found는 리소스 없음, 500 Internal Server Error는 서버 내부 오류를 의미하며, REST API 설계 시 각 상황에 맞는 상태 코드를 반환하는 것이 클라이언트와의 명확한 통신을 위해 중요합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'JPA에서 Entity 클래스를 설계할 때 주의해야 할 점은 무엇인가요?', '제이피에이에서 엔티티 클래스를 설계할 때 주의해야 할 점은 무엇인가요?', 'JPA',
 'Entity 클래스는 @Entity 어노테이션과 식별자(@Id)가 필수이며, 기본 생성자(no-args constructor)가 반드시 있어야 합니다. @Setter를 남용하지 않고 변경 메서드를 Entity 내부에 캡슐화하여 도메인 로직을 응집시키는 것이 권장됩니다. 또한 연관관계 매핑(@OneToMany, @ManyToOne) 시 Cascade와 FetchType을 신중하게 설정해야 하며, equals/hashCode는 id 기반으로만 정의해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring Data JPA의 JpaRepository가 제공하는 기본 메서드들은 무엇인가요?', '스프링 데이터 제이피에이의 제이피에이리포지토리가 제공하는 기본 메서드들은 무엇인가요?', 'JPA',
 'JpaRepository는 CrudRepository와 PagingAndSortingRepository를 상속하여 save, findById, findAll, deleteById 등의 기본 CRUD 메서드를 제공합니다. 메서드 이름 기반 쿼리 생성(findByEmail, findByNameContaining 등)을 통해 별도 구현 없이 쿼리를 자동 생성할 수 있습니다. 복잡한 쿼리는 @Query 어노테이션으로 JPQL이나 네이티브 SQL을 직접 작성할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'REST API에서 GET과 POST의 차이점은 무엇인가요?', '레스트 에이피아이에서 겟과 포스트의 차이점은 무엇인가요?', 'SPRING_MVC',
 'GET은 리소스 조회에 사용되며 요청 데이터가 URL 쿼리 스트링에 포함되어 브라우저 캐싱이 가능하고 멱등성이 보장됩니다. POST는 리소스 생성이나 서버 상태 변경에 사용되며 요청 바디에 데이터를 담아 전송하고 캐싱되지 않습니다. GET 요청은 여러 번 호출해도 동일 결과를 반환해야 하지만, POST는 호출마다 새 리소스가 생성될 수 있으므로 목적에 맞는 메서드를 선택하는 것이 중요합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', '@Service, @Repository, @Controller 어노테이션의 차이는 무엇인가요?', '서비스 어노테이션, 리포지토리 어노테이션, 컨트롤러 어노테이션의 차이는 무엇인가요?', 'SPRING_CORE',
 '세 어노테이션 모두 @Component를 상속하여 컴포넌트 스캔 대상이 되지만, 각자 계층을 명시하는 역할 차이가 있습니다. @Controller는 웹 계층 컨트롤러, @Service는 비즈니스 로직 계층, @Repository는 데이터 접근 계층임을 나타냅니다. @Repository는 추가로 Spring의 예외 변환(DataAccessException) 기능이 적용되어 JPA, JDBC 등의 기술별 예외를 통합 예외로 변환하는 부가 기능이 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'application.yml에서 환경변수를 어떻게 주입하나요?', 'application.와이엠엘에서 환경변수를 어떻게 주입하나요?', 'SPRING_BOOT',
 'application.yml에서 ${VARIABLE_NAME} 또는 ${VARIABLE_NAME:default_value} 문법으로 환경변수를 참조할 수 있습니다. 예를 들어 spring.datasource.password: ${DB_PASSWORD:secret}처럼 작성하면 환경변수 DB_PASSWORD가 없을 때 default_value가 사용됩니다. 운영 환경에서는 민감한 정보(DB 비밀번호, API 키 등)를 코드에 하드코딩하지 않고 환경변수나 AWS Secrets Manager 등 외부 저장소에서 주입받아야 보안 원칙에 부합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring에서 @Transactional 어노테이션의 기본적인 역할은 무엇인가요?', '스프링에서 트랜잭셔널 어노테이션의 기본적인 역할은 무엇인가요?', 'SPRING_DATA',
 '@Transactional은 해당 메서드 실행을 하나의 트랜잭션으로 묶어, 메서드가 정상 완료되면 커밋하고 예외가 발생하면 롤백합니다. AOP 기반 프록시로 동작하여 개발자가 트랜잭션 시작/종료 코드를 직접 작성할 필요가 없습니다. 기본적으로 RuntimeException(unchecked)에서만 롤백되며, Checked Exception은 명시적으로 rollbackFor 속성을 지정해야 롤백됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'DTO(Data Transfer Object)를 사용하는 이유는 무엇인가요?', '디티오를 사용하는 이유는 무엇인가요?', 'SPRING_MVC',
 'DTO는 계층 간 데이터 전달을 위한 객체로, Entity를 직접 API 응답에 노출하면 발생하는 문제를 해결합니다. Entity에는 비즈니스 로직, 연관관계 등 내부 구현이 담겨 있어 직접 노출 시 민감 정보 유출이나 의도치 않은 연관 데이터 직렬화가 발생할 수 있습니다. DTO를 통해 API 스펙과 내부 도메인 모델을 분리함으로써 API 변경 시 Entity를 수정할 필요가 없고, 클라이언트에게 필요한 데이터만 선택적으로 제공할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring에서 Bean을 등록하는 방법은 어떤 것들이 있나요?', '스프링에서 빈을 등록하는 방법은 어떤 것들이 있나요?', 'SPRING_CORE',
 'Bean 등록 방법은 크게 세 가지입니다. 첫째, @Component 및 이를 상속한 @Service, @Repository, @Controller를 클래스에 선언하고 컴포넌트 스캔을 활용하는 방법입니다. 둘째, @Configuration 클래스 내에서 @Bean 메서드를 선언하여 명시적으로 등록하는 방법으로, 외부 라이브러리 클래스를 Bean으로 등록할 때 주로 사용합니다. 셋째, Spring Boot Auto Configuration을 통해 조건부로 자동 등록되는 방법이 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'ResponseEntity를 반환하는 이유는 무엇인가요?', '리스폰스엔티티를 반환하는 이유는 무엇인가요?', 'SPRING_MVC',
 'ResponseEntity는 HTTP 응답의 상태 코드, 헤더, 바디를 모두 제어할 수 있는 래퍼 클래스입니다. 단순히 객체를 반환하면 기본 200 상태 코드만 설정되지만, ResponseEntity를 사용하면 리소스 생성 시 201, 조건부 응답 시 204 등 적절한 상태 코드를 명시적으로 지정할 수 있습니다. REST API 설계에서 HTTP 의미론을 정확하게 구현하기 위해 ResponseEntity 사용이 권장됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring MVC에서 JSON 직렬화/역직렬화는 어떻게 이루어지나요?', '스프링 엠브이씨에서 제이슨 직렬화/역직렬화는 어떻게 이루어지나요?', 'SPRING_MVC',
 'Spring MVC는 HttpMessageConverter 인터페이스를 통해 객체를 HTTP 메시지로 변환합니다. spring-boot-starter-web에 포함된 Jackson 라이브러리의 MappingJackson2HttpMessageConverter가 기본으로 등록되어, @ResponseBody나 @RestController에서 반환된 객체를 JSON으로 직렬화하고, @RequestBody로 수신된 JSON을 Java 객체로 역직렬화합니다. @JsonProperty, @JsonIgnore 등 Jackson 어노테이션으로 직렬화 동작을 커스터마이징할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring Boot에서 H2 인메모리 데이터베이스를 사용하는 이유와 설정 방법은?', '스프링 부트에서 에이치투 인메모리 데이터베이스를 사용하는 이유와 설정 방법은?', 'SPRING_BOOT',
 'H2 인메모리 DB는 별도 설치 없이 애플리케이션 내부에서 동작하는 경량 데이터베이스로, 개발/테스트 환경에서 MySQL 없이 빠르게 애플리케이션을 구동할 수 있습니다. build.gradle에 runtimeOnly ''com.h2database:h2'' 의존성을 추가하고, spring.datasource.url: jdbc:h2:mem:testdb 설정으로 활성화됩니다. spring.h2.console.enabled: true를 설정하면 브라우저에서 SQL 실행 콘솔에 접근할 수 있어 개발 편의성이 높습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Java의 Optional 클래스는 무엇이며 Spring 개발에서 어떻게 활용하나요?', '자바의 옵셔널 클래스는 무엇이며 스프링 개발에서 어떻게 활용하나요?', 'SPRING_CORE',
 'Optional은 null이 될 수 있는 값을 감싸는 컨테이너 클래스로, NPE(NullPointerException) 방지와 명시적인 null 처리를 위해 Java 8에서 도입됐습니다. Spring Data JPA의 findById 메서드는 Optional<T>를 반환하며, orElseThrow()로 값이 없을 때 예외를 발생시키거나 orElse()로 기본값을 제공할 수 있습니다. 리포지토리 반환값에는 Optional을 활용하되, Optional을 DTO 필드나 파라미터 타입으로는 사용하지 않는 것이 관례입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring Boot 프로젝트에서 패키지 구조를 어떻게 설계하는 것이 좋은가요?', '스프링 부트 프로젝트에서 패키지 구조를 어떻게 설계하는 것이 좋은가요?', 'SPRING_BOOT',
 '일반적으로 기능(도메인) 기반 패키지 구조가 계층 기반보다 확장에 유리합니다. 예를 들어 com.company.app.domain.user 하위에 controller, service, repository, entity, dto 패키지를 두면 관련 코드가 응집됩니다. 공통 설정이나 인증, 전역 예외 처리 등은 global 또는 common 패키지에 분리합니다. 외부 서비스 연동은 infra 패키지에 격리하여 비즈니스 로직과 기술 구현 세부사항을 분리하는 것이 유지보수성을 높입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Flyway나 Liquibase 같은 DB 마이그레이션 도구를 사용하는 이유는 무엇인가요?', '플라이웨이나 리큐베이스 같은 디비 마이그레이션 도구를 사용하는 이유는 무엇인가요?', 'SPRING_DATA',
 'DB 마이그레이션 도구는 데이터베이스 스키마 변경 이력을 버전으로 관리하여, 팀원들이 항상 동일한 스키마를 유지하고 환경별(개발/스테이징/운영) 스키마 동기화를 자동화합니다. Flyway는 V1__init.sql 같은 순서 있는 SQL 파일을 실행하고 이력 테이블에 적용 여부를 기록하여 중복 실행을 방지합니다. JPA의 ddl-auto: create-drop은 운영 환경에서 사용하면 데이터가 삭제될 수 있으므로, 운영에서는 반드시 마이그레이션 도구를 사용해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- ============================================================
-- MID (30문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring Bean의 싱글톤 스코프와 멀티스레드 환경에서의 주의점을 설명해주세요.', '스프링 빈의 싱글톤 스코프와 멀티스레드 환경에서의 주의점을 설명해주세요.', 'SPRING_CORE',
 'Spring Bean은 기본적으로 싱글톤 스코프로 컨테이너당 하나의 인스턴스만 생성됩니다. 멀티스레드 환경에서 여러 요청이 동시에 하나의 Bean 인스턴스를 공유하므로, 인스턴스 변수(상태)를 가지면 경쟁 조건이 발생합니다. 따라서 Service, Repository 클래스는 상태를 가지지 않는 무상태(stateless)로 설계해야 하며, 요청별로 상태가 필요한 경우 @Scope("prototype") 또는 지역 변수를 사용해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'AOP(Aspect-Oriented Programming)의 원리와 Spring AOP에서의 동작 방식을 설명해주세요.', '에이오피의 원리와 스프링 에이오피에서의 동작 방식을 설명해주세요.', 'SPRING_CORE',
 'AOP는 로깅, 트랜잭션, 보안 등 횡단 관심사를 핵심 비즈니스 로직에서 분리하는 프로그래밍 패러다임입니다. Spring AOP는 런타임에 프록시 객체를 생성하여 대상 Bean을 감싸고, 메서드 호출 전후에 Advice(부가 로직)를 실행합니다. 인터페이스가 있으면 JDK Dynamic Proxy를, 없으면 CGLIB를 사용하며, @Aspect와 @Around/@Before/@After 어노테이션으로 포인트컷과 어드바이스를 선언적으로 정의합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:JAVA_SPRING:LANGUAGE_FRAMEWORK', '@Transactional이 프록시 방식으로 동작하는 것의 의미와 주의사항은 무엇인가요?', '트랜잭셔널 어노테이션이 프록시 방식으로 동작하는 것의 의미와 주의사항은 무엇인가요?', 'SPRING_DATA',
 '@Transactional은 AOP 프록시가 트랜잭션 시작/종료를 감싸는 방식으로 동작합니다. 따라서 같은 클래스 내부에서 this.method()로 @Transactional 메서드를 직접 호출하면 프록시를 거치지 않아 트랜잭션이 적용되지 않는 Self-invocation 문제가 발생합니다. 또한 private 메서드는 프록시에서 오버라이딩이 불가능하므로 @Transactional이 동작하지 않으며, protected 이상의 접근 제어자가 필요합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring @Transactional의 전파 레벨(Propagation) 종류와 차이점을 설명해주세요.', '스프링 트랜잭셔널 어노테이션의 전파 레벨 종류와 차이점을 설명해주세요.', 'SPRING_DATA',
 'REQUIRED(기본값)는 기존 트랜잭션이 있으면 참여하고 없으면 새로 생성합니다. REQUIRES_NEW는 항상 새 트랜잭션을 시작하고 기존 트랜잭션을 일시 중단하여, 외부 트랜잭션 롤백과 무관하게 독립 커밋이 가능합니다. NESTED는 SavePoint를 이용해 중첩 트랜잭션을 생성하여 부분 롤백이 가능하며, SUPPORTS는 트랜잭션이 있으면 참여하고 없으면 없이 실행됩니다. 트랜잭션 전파를 잘못 설정하면 의도치 않은 롤백이나 독립 커밋이 발생하므로 신중히 선택해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring Security에서 인증(Authentication)과 인가(Authorization)의 차이를 설명해주세요.', '스프링 시큐리티에서 인증과 인가의 차이를 설명해주세요.', 'SPRING_SECURITY',
 '인증은 "당신이 누구인가"를 검증하는 과정으로, 사용자가 제출한 자격증명(아이디/비밀번호, 토큰 등)의 유효성을 확인합니다. 인가는 인증된 사용자가 "무엇을 할 수 있는가"를 제어하는 과정으로, ROLE_USER, ROLE_ADMIN 같은 권한에 따라 리소스 접근을 허용하거나 거부합니다. Spring Security에서 AuthenticationManager가 인증을 처리하고, SecurityFilterChain의 authorizeHttpRequests 설정이 인가를 담당합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring Security의 Filter와 Spring Interceptor의 차이점은 무엇인가요?', '스프링 시큐리티의 필터와 스프링 인터셉터의 차이점은 무엇인가요?', 'SPRING_SECURITY',
 'Filter는 Servlet 컨테이너 레벨에서 동작하여 Spring MVC DispatcherServlet보다 먼저 실행되며, Spring Context에 접근할 수 없습니다(단, DelegatingFilterProxy를 통해 Spring Bean 사용 가능). Interceptor는 Spring MVC 레벨에서 DispatcherServlet 이후에 동작하며 Spring Context의 Bean을 자유롭게 사용할 수 있습니다. 인증/인가, CORS, XSS 필터링 같은 보안 처리는 Filter에서, 로그인 여부 확인이나 요청 로깅 같은 비즈니스 관련 처리는 Interceptor에서 담당하는 것이 일반적입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'JWT 기반 인증에서 Access Token과 Refresh Token의 역할과 보안 고려사항은?', '제이더블유티 기반 인증에서 액세스 토큰과 리프레시 토큰의 역할과 보안 고려사항은?', 'SPRING_SECURITY',
 'Access Token은 짧은 유효기간(15분~1시간)을 가지며 API 요청 시 인증 수단으로 사용됩니다. Refresh Token은 긴 유효기간(7일~30일)을 가지며 Access Token 만료 시 재발급에만 사용됩니다. Refresh Token은 DB에 저장하여 탈취 시 무효화할 수 있어야 하며, HttpOnly Cookie로 전송하여 XSS 공격에 의한 탈취를 방지해야 합니다. Access Token은 Stateless로 서버에 저장하지 않으므로, 강제 로그아웃이 필요한 경우 블랙리스트 방식이나 짧은 유효기간 전략이 필요합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'JPA N+1 문제란 무엇이며 어떻게 해결하나요?', '제이피에이 엔플러스원 문제란 무엇이며 어떻게 해결하나요?', 'JPA',
 'N+1 문제는 1개의 쿼리로 N개의 엔티티를 조회한 후, 연관 엔티티에 접근할 때 N개의 추가 쿼리가 발생하는 성능 문제입니다. Lazy Loading에서 흔히 발생하며, 예를 들어 Order 100개를 조회하면 각 Order의 Member를 가져오기 위해 100번의 추가 쿼리가 실행됩니다. 해결 방법으로는 JPQL의 fetch join(@Query에 JOIN FETCH 사용), @EntityGraph, Batch Size 설정(IN 쿼리로 묶음 조회), 또는 DTO 프로젝션을 직접 사용하는 방법이 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'JPA의 Lazy Loading과 Eager Loading의 차이와 권장 설정은 무엇인가요?', '제이피에이의 레이지 로딩과 이거 로딩의 차이와 권장 설정은 무엇인가요?', 'JPA',
 'Lazy Loading은 연관 엔티티가 실제로 접근될 때 쿼리를 실행하고, Eager Loading은 부모 엔티티 조회 시 연관 엔티티를 즉시 함께 조회합니다. @ManyToOne, @OneToOne은 기본이 EAGER이고 @OneToMany, @ManyToMany는 기본이 LAZY입니다. 실무에서는 모든 연관관계를 LAZY로 설정하고, 필요한 경우에만 fetch join으로 명시적으로 함께 조회하는 것이 권장됩니다. EAGER는 예측 불가한 쿼리 발생과 불필요한 데이터 조회로 성능 문제를 야기할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring에서 순환 참조(Circular Dependency) 문제는 왜 발생하며 어떻게 해결하나요?', '스프링에서 순환 참조 문제는 왜 발생하며 어떻게 해결하나요?', 'SPRING_CORE',
 '순환 참조는 A Bean이 B를 주입받고 B Bean이 A를 주입받는 상황으로, 생성자 주입에서는 컨테이너 시작 시점에 BeanCurrentlyInCreationException으로 즉시 감지됩니다. 해결 방법은 첫째 설계 개선으로, 공통 의존성을 별도 C Bean으로 추출하여 순환 구조를 제거하는 것이 가장 좋습니다. 불가피한 경우 @Lazy 주입으로 지연 초기화하거나, Setter 주입(권장하지 않음)을 사용할 수 있으며, Spring Boot 2.6 이후 기본적으로 순환 참조 감지가 강화되어 시작 시 예외를 던집니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'QueryDSL을 사용하는 이유와 장점은 무엇인가요?', '쿼리디에스엘을 사용하는 이유와 장점은 무엇인가요?', 'JPA',
 'QueryDSL은 Java 코드로 타입 안전한 동적 쿼리를 작성할 수 있는 라이브러리입니다. Spring Data JPA의 메서드 이름 기반 쿼리는 단순 조건에 적합하지만 복잡한 동적 쿼리(조건이 여러 개이거나 null 가능)를 작성하기 어렵습니다. QueryDSL은 컴파일 시점에 오타와 타입 오류를 잡아주며, BooleanBuilder나 BooleanExpression으로 조건을 조합하여 가독성 높은 동적 쿼리를 작성할 수 있어 복잡한 검색 기능 구현에 유리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring의 이벤트(ApplicationEvent) 메커니즘을 설명해주세요.', '스프링의 이벤트 메커니즘을 설명해주세요.', 'SPRING_CORE',
 'Spring 이벤트는 퍼블리셔(ApplicationEventPublisher)가 이벤트를 발행하면 리스너(@EventListener 또는 ApplicationListener 구현체)가 이를 처리하는 옵저버 패턴 구현입니다. 이를 통해 도메인 로직(예: 회원 가입) 완료 후 이메일 발송, 알림 등을 결합도 없이 처리할 수 있습니다. 기본적으로 동기 처리이며, @Async와 함께 사용하면 비동기 이벤트 처리가 가능합니다. 단, 동일 트랜잭션에서 처리되므로 이벤트 리스너에서 예외 발생 시 트랜잭션이 롤백됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring Cache 추상화(@Cacheable)와 활용 방법을 설명해주세요.', '스프링 캐시 추상화와 활용 방법을 설명해주세요.', 'SPRING_CORE',
 '@Cacheable은 메서드 반환값을 캐시에 저장하고, 동일 파라미터로 재호출 시 메서드를 실행하지 않고 캐시된 값을 반환하는 AOP 기반 캐시 추상화입니다. 실제 캐시 구현체는 EhCache, Redis, Caffeine 등으로 교체 가능하며, spring-boot-starter-cache와 캐시 구현체 의존성만 추가하면 됩니다. @CachePut은 항상 메서드를 실행하고 결과를 캐시에 저장하며, @CacheEvict는 캐시를 무효화합니다. 캐시 키 설계와 적절한 TTL 설정이 중요합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring Security의 SecurityFilterChain 설정 방법을 설명해주세요.', '스프링 시큐리티의 시큐리티필터체인 설정 방법을 설명해주세요.', 'SPRING_SECURITY',
 'Spring Boot 3.x / Spring Security 6.x에서는 SecurityFilterChain Bean을 @Configuration 클래스에서 정의합니다. HttpSecurity를 사용해 csrf, sessionManagement, authorizeHttpRequests, addFilterBefore 등을 메서드 체인으로 설정합니다. JWT 인증을 위해 JwtAuthenticationFilter를 UsernamePasswordAuthenticationFilter 앞에 추가하고, 공개 API는 permitAll(), 인증 필요 API는 authenticated()로 설정합니다. Lambda DSL 방식(람다로 설정)이 Spring Security 6에서 권장됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring의 @Async 어노테이션 동작 방식과 주의사항을 설명해주세요.', '스프링의 에이싱크 어노테이션 동작 방식과 주의사항을 설명해주세요.', 'SPRING_CORE',
 '@Async는 메서드를 별도 스레드에서 비동기 실행하도록 하는 AOP 기반 어노테이션으로, @EnableAsync를 활성화해야 동작합니다. @Transactional과 같이 프록시 방식이므로 같은 클래스 내 Self-invocation 시 비동기가 적용되지 않습니다. 반환 타입은 void 또는 Future<T>/CompletableFuture<T>이어야 하며, ThreadPoolTaskExecutor를 빈으로 등록하여 스레드 풀 설정을 조정하지 않으면 SimpleAsyncTaskExecutor가 기본 사용되어 매번 새 스레드를 생성하는 성능 문제가 발생합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'JPA의 더티 체킹(Dirty Checking) 메커니즘을 설명해주세요.', '제이피에이의 더티 체킹 메커니즘을 설명해주세요.', 'JPA',
 '더티 체킹은 JPA가 영속성 컨텍스트 내 엔티티의 상태를 추적하여, 트랜잭션 커밋 시점에 변경된 필드를 자동으로 UPDATE 쿼리를 생성하는 기능입니다. 영속 상태의 엔티티를 조회한 후 setter로 값을 변경하면, 별도 save() 호출 없이 트랜잭션 종료 시 자동으로 DB에 반영됩니다. 단, @Transactional 범위 내에서만 동작하며, 준영속 상태(detached)나 영속성 컨텍스트 외부에서는 더티 체킹이 적용되지 않습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring의 RestTemplate과 WebClient의 차이점은 무엇인가요?', '스프링의 레스트템플릿과 웹클라이언트의 차이점은 무엇인가요?', 'SPRING_CORE',
 'RestTemplate은 동기 방식의 HTTP 클라이언트로 요청 완료까지 스레드가 블로킹되며, Spring 5.0부터 유지보수 모드로 전환되었습니다. WebClient는 Spring WebFlux에서 제공하는 비동기/논블로킹 HTTP 클라이언트로, 적은 스레드로 높은 동시성을 처리할 수 있습니다. Spring MVC 프로젝트에서도 WebClient를 사용할 수 있으며(Reactor 의존성 필요), 신규 프로젝트에서는 WebClient 사용이 권장됩니다. WebClient는 .block() 메서드로 동기 방식으로도 사용 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'JPA의 @OneToMany 매핑에서 양방향 연관관계 설정 시 주의사항은 무엇인가요?', '제이피에이의 원투메니 매핑에서 양방향 연관관계 설정 시 주의사항은 무엇인가요?', 'JPA',
 '양방향 연관관계에서는 외래 키를 관리하는 연관관계의 주인(mappedBy가 없는 쪽)만 DB를 실제 수정합니다. 주인이 아닌 쪽(mappedBy 선언 쪽)에서만 데이터를 변경해도 DB에 반영되지 않으므로, 양쪽 모두에 편의 메서드(addComment 등)로 데이터를 설정해야 합니다. @OneToMany는 기본 LAZY이지만, toString이나 Jackson 직렬화에서 무한 루프가 발생할 수 있으므로 @JsonManagedReference/@JsonBackReference 또는 @JsonIgnore를 사용하거나 DTO를 통해 직렬화해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring Boot Actuator의 역할과 주요 엔드포인트를 설명해주세요.', '스프링 부트 액추에이터의 역할과 주요 엔드포인트를 설명해주세요.', 'SPRING_BOOT',
 'Spring Boot Actuator는 운영 환경에서 애플리케이션 상태를 모니터링하고 관리하기 위한 엔드포인트를 제공합니다. /actuator/health는 서비스 가용성 확인, /actuator/metrics는 JVM 메모리/스레드/HTTP 요청 통계, /actuator/env는 설정 값, /actuator/info는 애플리케이션 정보를 제공합니다. 보안상 민감한 엔드포인트(/actuator/env, /actuator/beans 등)는 management.endpoints.web.exposure.include로 선택적으로 노출하고, Spring Security로 접근 제어를 해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring의 @Scheduled를 이용한 스케줄링 작업 설정 방법은?', '스프링의 스케줄드 어노테이션을 이용한 스케줄링 작업 설정 방법은?', 'SPRING_CORE',
 '@EnableScheduling을 @Configuration 클래스에 선언한 후, @Scheduled 어노테이션으로 스케줄 작업을 설정합니다. fixedRate는 이전 실행 시작 기준으로 반복, fixedDelay는 이전 실행 종료 기준으로 반복하며, cron 표현식으로 특정 시간대 실행을 지정할 수 있습니다. 기본적으로 단일 스레드에서 실행되므로 오래 걸리는 작업은 @Async와 함께 사용하거나 ThreadPoolTaskScheduler를 설정해야 합니다. 멀티 인스턴스 환경에서는 ShedLock 같은 분산 락 라이브러리로 중복 실행을 방지해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring의 CORS(Cross-Origin Resource Sharing) 설정 방법을 설명해주세요.', '스프링의 코어스 설정 방법을 설명해주세요.', 'SPRING_MVC',
 'CORS는 브라우저의 동일 출처 정책으로 인해 다른 도메인 API 호출이 차단될 때, 서버가 허용 출처를 응답 헤더로 명시하는 메커니즘입니다. Spring에서는 @CrossOrigin 어노테이션으로 컨트롤러별 설정, WebMvcConfigurer의 addCorsMappings로 전역 설정, 또는 SecurityFilterChain의 cors 설정을 통해 구성할 수 있습니다. 운영 환경에서는 allowedOrigins를 특정 프론트엔드 도메인만으로 제한하고 * 사용을 피해야 하며, 인증이 필요한 경우 allowCredentials를 true로 설정해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'JPA 영속성 컨텍스트의 1차 캐시 동작을 설명해주세요.', '제이피에이 영속성 컨텍스트의 일차 캐시 동작을 설명해주세요.', 'JPA',
 '1차 캐시는 영속성 컨텍스트 내부에 엔티티를 Map<@Id, Entity> 형태로 저장하는 메모리 캐시입니다. 동일 트랜잭션 내에서 같은 ID로 엔티티를 조회하면 DB 쿼리 없이 1차 캐시에서 반환되어 조회 성능이 향상됩니다. 또한 동일 트랜잭션에서 같은 ID로 조회한 객체는 항상 동일 인스턴스(==)임이 보장됩니다. 단, 1차 캐시는 트랜잭션 범위에서만 유효하며 애플리케이션 전체 공유 캐시는 2차 캐시를 별도로 설정해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring에서 파일 업로드 처리 방식과 멀티파트 설정을 설명해주세요.', '스프링에서 파일 업로드 처리 방식과 멀티파트 설정을 설명해주세요.', 'SPRING_MVC',
 'Spring MVC는 MultipartFile 인터페이스로 업로드 파일을 처리하며, @RequestParam("file") MultipartFile file로 파라미터를 받습니다. application.yml에서 spring.servlet.multipart.max-file-size와 max-request-size로 파일 크기를 제한하고, spring.servlet.multipart.enabled로 멀티파트 처리를 활성화합니다. 파일은 로컬 저장 대신 S3 같은 오브젝트 스토리지에 업로드하고 URL을 DB에 저장하는 방식이 확장성 측면에서 권장됩니다. 파일 확장자와 MIME 타입을 검증하여 보안 위협을 방지해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring에서 페이지네이션을 구현하는 방법을 설명해주세요.', '스프링에서 페이지네이션을 구현하는 방법을 설명해주세요.', 'SPRING_DATA',
 'Spring Data JPA는 Pageable 인터페이스를 통해 페이지네이션을 지원합니다. 컨트롤러에서 @PageableDefault(size = 20) Pageable pageable 파라미터로 page, size, sort 쿼리 파라미터를 자동 바인딩합니다. Repository 메서드에서 Page<T> findAll(Pageable pageable)을 사용하면 총 개수 쿼리(COUNT)와 데이터 쿼리가 함께 실행됩니다. 카운트 쿼리가 비용이 클 경우 countQuery를 @Query로 분리하거나, 커서 기반 페이지네이션으로 변경하는 것이 성능에 유리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring의 의존성 주입에서 @Primary와 @Qualifier의 역할은?', '스프링의 의존성 주입에서 프라이머리 어노테이션과 퀄리파이어 어노테이션의 역할은?', 'SPRING_CORE',
 '같은 타입의 Bean이 여러 개 등록된 경우 @Autowired 시 NoUniqueBeanDefinitionException이 발생합니다. @Primary는 여러 후보 중 기본으로 주입될 Bean을 지정하며, @Qualifier("beanName")은 특정 Bean을 이름으로 선택하여 주입합니다. @Primary는 전역적 기본값을 설정할 때, @Qualifier는 특정 주입 지점에서 명시적으로 Bean을 선택할 때 사용합니다. 두 어노테이션이 충돌하면 @Qualifier가 더 우선순위를 가집니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'JPA에서 벌크 업데이트(@Modifying) 사용 시 주의사항은 무엇인가요?', '제이피에이에서 벌크 업데이트 사용 시 주의사항은 무엇인가요?', 'JPA',
 '@Modifying은 @Query와 함께 사용하여 JPQL UPDATE/DELETE 쿼리를 실행하는 어노테이션입니다. 벌크 연산은 영속성 컨텍스트를 우회하여 DB에 직접 반영되므로, 실행 후 1차 캐시에 남아 있는 데이터와 DB 상태가 불일치하게 됩니다. 이를 해결하기 위해 @Modifying(clearAutomatically = true)로 쿼리 실행 후 영속성 컨텍스트를 자동으로 비우거나, EntityManager.clear()를 명시적으로 호출해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring의 MessageSource를 이용한 다국어(i18n) 처리를 설명해주세요.', '스프링의 메시지소스를 이용한 다국어 처리를 설명해주세요.', 'SPRING_MVC',
 'MessageSource는 로케일에 따른 메시지를 외부 파일(messages.properties, messages_ko.properties 등)에서 읽어 제공하는 Spring 인터페이스입니다. 에러 메시지, 알림 메시지 등을 코드 내 하드코딩하지 않고 프로퍼티 파일로 분리하면 다국어 지원과 메시지 변경이 용이합니다. @Valid 검증 실패 메시지도 ValidationMessages.properties를 통해 커스터마이징할 수 있으며, Accept-Language 헤더나 LocaleContextHolder로 로케일을 결정합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring Data JPA에서 Projection을 사용하는 이유와 방법은?', '스프링 데이터 제이피에이에서 프로젝션을 사용하는 이유와 방법은?', 'JPA',
 'Projection은 엔티티 전체가 아닌 특정 컬럼만 조회하여 불필요한 데이터 조회를 줄이는 기법입니다. 인터페이스 기반 Projection은 조회할 필드명 getter를 인터페이스에 선언하면 Spring Data가 자동으로 프록시를 생성합니다. 클래스 기반 Projection(DTO Projection)은 생성자 파라미터에 맞는 JPQL SELECT를 실행하여 더 높은 성능을 제공합니다. @Query와 함께 DTO Projection을 사용하면 필요한 데이터만 SELECT하므로 N+1 문제 예방과 쿼리 성능 최적화에 유용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring에서 테스트 슬라이스 어노테이션(@WebMvcTest, @DataJpaTest)의 역할은?', '스프링에서 테스트 슬라이스 어노테이션의 역할은?', 'SPRING_BOOT',
 '@WebMvcTest는 웹 계층(컨트롤러, 필터, 인터셉터)만 로드하는 슬라이스 테스트 어노테이션으로, Spring Security와 MockMvc를 사용하여 HTTP 요청/응답을 테스트합니다. 서비스, 리포지토리 빈은 로드되지 않으므로 @MockBean으로 모킹해야 합니다. @DataJpaTest는 JPA 관련 Bean만 로드하고 기본적으로 인메모리 DB를 사용하여 Repository 계층을 테스트합니다. 각 슬라이스 테스트는 전체 컨텍스트를 로드하는 @SpringBootTest보다 훨씬 빠르게 실행되어 단위 테스트에 가까운 속도를 제공합니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- ============================================================
-- SENIOR (30문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'BeanFactory와 ApplicationContext의 차이점과 ApplicationContext의 부가 기능을 설명해주세요.', '빈팩토리와 애플리케이션컨텍스트의 차이점과 애플리케이션컨텍스트의 부가 기능을 설명해주세요.', 'SPRING_CORE',
 'BeanFactory는 Bean 등록, 조회, 생명주기 관리의 기본 IoC 컨테이너 인터페이스이며, ApplicationContext는 BeanFactory를 상속하여 국제화(MessageSource), 이벤트 발행(ApplicationEventPublisher), 리소스 로딩(ResourceLoader), 환경변수 접근(EnvironmentCapable) 등의 부가 기능을 제공합니다. ApplicationContext는 시작 시점에 모든 싱글톤 Bean을 미리 초기화하므로 초기 구동은 느리지만 런타임 성능이 안정적입니다. 실무에서는 항상 ApplicationContext를 사용하며, BeanFactory를 직접 사용하는 경우는 거의 없습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'CGLIB Proxy와 JDK Dynamic Proxy의 차이점과 Spring Boot 3.x의 기본 설정은?', '씨지리브 프록시와 제이디케이 다이내믹 프록시의 차이점과 스프링 부트 삼점엑스의 기본 설정은?', 'SPRING_CORE',
 'JDK Dynamic Proxy는 인터페이스 기반 프록시로 java.lang.reflect.Proxy를 사용하며, 반드시 인터페이스가 있어야 합니다. CGLIB는 바이트코드 조작으로 클래스를 상속하여 프록시를 생성하므로 인터페이스 없이도 동작하지만, final 클래스나 final 메서드는 프록시 불가합니다. Spring Boot 3.x(Spring 6.x)에서는 기본적으로 CGLIB를 사용하며(proxyTargetClass=true), @Transactional이나 @Async 대상 클래스에 final 키워드나 final 메서드가 있으면 CGLIB 프록시 생성에 실패합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', '@Transactional이 private 메서드에 동작하지 않는 이유와 대안을 설명해주세요.', '트랜잭셔널 어노테이션이 프라이빗 메서드에 동작하지 않는 이유와 대안을 설명해주세요.', 'SPRING_DATA',
 'CGLIB와 JDK Dynamic Proxy 모두 대상 메서드를 오버라이딩하거나 인터페이스를 구현하는 방식으로 프록시를 생성하므로, private 메서드는 오버라이딩 불가로 @Transactional이 적용되지 않습니다. 또한 퍼블릭 메서드에서 private 메서드를 직접 호출하면 프록시를 거치지 않는 Self-invocation이 발생합니다. 해결책으로는 해당 메서드를 별도 Bean으로 분리하거나, ApplicationContext에서 Self Bean을 주입받아 호출하거나(일반적으로 비권장), AspectJ의 컴파일 타임 위빙을 사용하는 방법이 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring Boot Auto Configuration의 조건부 Bean 등록 메커니즘을 상세히 설명해주세요.', '스프링 부트 오토 컨피그레이션의 조건부 빈 등록 메커니즘을 상세히 설명해주세요.', 'SPRING_BOOT',
 'Auto Configuration 클래스는 @AutoConfiguration(previously @Configuration)에 @Conditional 계열 어노테이션으로 조건을 선언합니다. @ConditionalOnClass는 특정 클래스가 classpath에 있을 때, @ConditionalOnMissingBean은 해당 Bean이 없을 때, @ConditionalOnProperty는 특정 프로퍼티가 설정됐을 때만 Bean을 등록합니다. Spring Boot 3.x에서는 AutoConfiguration.imports 파일에 등록된 순서와 @AutoConfiguration(after=...) 조건으로 순서를 제어하며, 개발자가 커스텀 Starter를 만들 때도 동일한 메커니즘을 활용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring에서 Event-Driven Architecture를 구현할 때 트랜잭션 경계 문제를 어떻게 해결하나요?', '스프링에서 이벤트-드리븐 아키텍처를 구현할 때 트랜잭션 경계 문제를 어떻게 해결하나요?', 'SPRING_CORE',
 '기본 @EventListener는 발행자와 같은 트랜잭션에서 동기 실행되어, 리스너 예외가 발행자 트랜잭션을 롤백합니다. @TransactionalEventListener(phase=AFTER_COMMIT)를 사용하면 트랜잭션 커밋 이후에 이벤트를 처리하여, DB 저장이 확정된 데이터를 리스너에서 안전하게 사용할 수 있습니다. 단, AFTER_COMMIT 리스너는 새 트랜잭션이 없으므로 DB 작업이 필요하면 @Transactional(propagation=REQUIRES_NEW)을 함께 선언해야 합니다. 외부 시스템(이메일, Slack) 연동이나 도메인 간 결합 제거에 유용하게 활용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'JPA Cascade와 OrphanRemoval의 차이와 적절한 사용 시나리오를 설명해주세요.', '제이피에이 캐스케이드와 오팬리무벌의 차이와 적절한 사용 시나리오를 설명해주세요.', 'JPA',
 'CascadeType.PERSIST는 부모 엔티티 저장 시 자식도 함께 저장하고, CascadeType.REMOVE는 부모 삭제 시 자식도 삭제합니다. ALL은 모든 Cascade 타입을 적용합니다. orphanRemoval=true는 부모와의 연관관계가 끊어진(컬렉션에서 제거된) 자식 엔티티를 자동으로 삭제합니다. CascadeType.REMOVE와 orphanRemoval의 차이는, REMOVE는 부모가 삭제될 때만 동작하고 orphanRemoval은 컬렉션에서 제거될 때도 삭제합니다. 부모에 완전히 의존하는 생명주기를 가진 자식(예: Order-OrderItem) 에만 적용하고, 다른 엔티티와 공유되는 관계에서는 사용하면 안 됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring Boot 애플리케이션의 대규모 배치 처리를 Spring Batch로 설계할 때 고려사항은?', '스프링 부트 애플리케이션의 대규모 배치 처리를 스프링 배치로 설계할 때 고려사항은?', 'SPRING_BOOT',
 'Spring Batch는 Job-Step-Chunk 구조로 대용량 데이터 처리를 청크 단위로 나눠 처리하며, 각 Chunk는 독립 트랜잭션으로 처리되어 실패 시 해당 청크만 롤백합니다. ItemReader(DB 커서 또는 페이지 방식), ItemProcessor(변환/필터), ItemWriter(DB 저장/파일 출력) 구성요소를 SRP에 맞게 분리합니다. 멀티 인스턴스 환경에서 JobRepository를 공유 DB에 두어 이중 실행을 방지하고, 파티셔닝으로 병렬 처리 성능을 높입니다. Job 파라미터로 실행 단위를 구분하며, 실패 지점부터 재시작(restartable) 설계가 중요합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring에서 Optimistic Lock과 Pessimistic Lock의 차이와 적용 시나리오는?', '스프링에서 옵티미스틱 락과 페시미스틱 락의 차이와 적용 시나리오는?', 'JPA',
 'Optimistic Lock은 @Version 어노테이션으로 버전 컬럼을 두어, 업데이트 시 버전이 불일치하면 OptimisticLockException을 던지는 충돌 후 감지 방식입니다. 충돌이 드문 경우(대부분의 웹 애플리케이션)에 적합하며 DB 락 없이 동시성을 처리합니다. Pessimistic Lock은 DB 레벨의 SELECT FOR UPDATE로 실제 락을 걸어 충돌 자체를 방지하는 방식으로, 충돌이 빈번하거나 데이터 정확성이 매우 중요한 경우(재고 차감, 포인트 지급)에 적합합니다. Pessimistic Lock은 데드락과 성능 저하 위험이 있으므로 락 범위를 최소화하고 타임아웃을 설정해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring WebFlux와 Spring MVC의 차이점 및 선택 기준은?', '스프링 웹플럭스와 스프링 엠브이씨의 차이점 및 선택 기준은?', 'SPRING_CORE',
 'Spring MVC는 스레드-요청 1:1 매핑의 블로킹 I/O 모델로, 요청마다 스레드가 할당되어 높은 동시성에서 스레드 풀 고갈 문제가 발생할 수 있습니다. Spring WebFlux는 Reactor를 기반으로 한 비동기 논블로킹 모델로 적은 스레드로 높은 동시성을 처리하지만, 코드 복잡도가 높고 디버깅이 어렵습니다. 대부분의 비즈니스 애플리케이션은 JPA 같은 블로킹 I/O를 사용하므로 WebFlux 도입 시 이점이 제한적이며, 완전한 반응형 스택(R2DBC, WebClient)이 필요한 실시간 스트리밍이나 고동시성 요구사항에서 WebFlux를 선택합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring Boot 애플리케이션의 메모리 튜닝과 GC 설정 전략은?', '스프링 부트 애플리케이션의 메모리 튜닝과 지씨 설정 전략은?', 'SPRING_BOOT',
 'Java 21 이전에는 G1GC가 기본이며, 대규모 힙에서 ZGC나 ShenandoahGC가 낮은 지연 시간을 제공합니다. Heap 크기는 -Xms와 -Xmx를 같은 값으로 설정하여 GC 오버헤드를 줄이고, 컨테이너 환경에서는 -XX:MaxRAMPercentage로 비율로 지정합니다. Spring Boot Actuator의 /metrics와 JVM 메트릭(jvm.memory.used, jvm.gc.pause)을 Prometheus/Grafana로 모니터링하여 GC 빈도와 힙 사용량 트렌드를 분석합니다. Virtual Thread(Java 21 Loom)를 사용하면 스레드 풀 튜닝 없이도 높은 동시성이 가능하며 Spring Boot 3.2+에서 공식 지원됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring에서 멀티 모듈 프로젝트를 구성할 때의 장점과 의존성 설계 원칙은?', '스프링에서 멀티 모듈 프로젝트를 구성할 때의 장점과 의존성 설계 원칙은?', 'SPRING_BOOT',
 '멀티 모듈 구조는 도메인 경계를 모듈로 분리하여 잘못된 의존성 방향을 컴파일 시점에 차단하고, 빌드 캐싱으로 변경된 모듈만 재빌드하여 CI/CD 속도를 개선합니다. 의존성은 단방향으로 흘러야 하며(api-module → domain-module → common-module), 순환 의존성은 절대 허용하지 않습니다. Gradle 멀티 프로젝트에서 api, core(domain), infra 모듈을 분리하면 나중에 MSA 전환 시 각 모듈을 독립 서비스로 추출하기 용이합니다. 공통 설정(컨벤션 플러그인)은 buildSrc나 convention plugin으로 중앙화합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring에서 분산 트랜잭션(SAGA 패턴)이 필요한 상황과 구현 방법은?', '스프링에서 분산 트랜잭션이 필요한 상황과 구현 방법은?', 'SPRING_DATA',
 '마이크로서비스 환경에서 여러 서비스에 걸친 데이터 일관성이 필요하지만 2PC(2-Phase Commit)는 가용성 저하로 실용적이지 않을 때 SAGA 패턴을 적용합니다. Choreography SAGA는 각 서비스가 이벤트를 발행하고 다음 서비스가 구독하는 방식으로, 결합도가 낮지만 흐름 추적이 어렵습니다. Orchestration SAGA는 중앙 오케스트레이터가 각 서비스의 트랜잭션을 순차 호출하고 실패 시 보상 트랜잭션을 실행하는 방식으로, 흐름 제어가 명확합니다. Spring 생태계에서는 Axon Framework나 Eventuate Tram이 SAGA 구현을 지원합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'JPA의 2차 캐시(Second Level Cache) 동작 방식과 적용 전략은?', '제이피에이의 이차 캐시 동작 방식과 적용 전략은?', 'JPA',
 '2차 캐시는 영속성 컨텍스트를 초월하여 여러 트랜잭션과 세션에서 공유되는 애플리케이션 수준 캐시입니다. Hibernate의 EhCache나 Caffeine을 2차 캐시 구현체로 설정하고 @Cacheable 어노테이션을 엔티티에 선언합니다. 변경이 드문 참조 데이터(코드 테이블, 설정 정보)에는 효과적이지만, 자주 변경되는 데이터에 적용하면 캐시 무효화 오버헤드와 데이터 불일치 위험이 있습니다. MSA 환경에서는 각 인스턴스의 캐시 동기화 문제가 있으므로 Redis 같은 공유 캐시로 대체하는 것이 일반적입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring Cloud Gateway와 API Gateway 패턴의 역할을 설명해주세요.', '스프링 클라우드 게이트웨이와 에이피아이 게이트웨이 패턴의 역할을 설명해주세요.', 'SPRING_CORE',
 'API Gateway는 마이크로서비스 앞에 위치하여 단일 진입점 역할을 하며, 라우팅, 인증/인가, 로드 밸런싱, Rate Limiting, 서킷 브레이커 등의 횡단 관심사를 처리합니다. Spring Cloud Gateway는 Spring WebFlux 기반 비동기 게이트웨이로, RouteLocator나 application.yml로 라우팅 규칙을 정의하고 GatewayFilter로 요청/응답 변환, 인증 토큰 검증을 수행합니다. 클라이언트는 개별 서비스 위치를 모르고 게이트웨이 URL만 알면 되므로 서비스 내부 토폴로지 변경에 유연합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring 애플리케이션에서 메시지 큐(Kafka/RabbitMQ)를 활용한 비동기 처리 설계를 설명해주세요.', '스프링 애플리케이션에서 메시지 큐를 활용한 비동기 처리 설계를 설명해주세요.', 'SPRING_CORE',
 'Spring Kafka의 @KafkaListener나 Spring AMQP의 @RabbitListener로 메시지 소비자를 선언하고, KafkaTemplate/RabbitTemplate으로 메시지를 발행합니다. 비동기 처리를 통해 주문 처리 → 알림 발송 → 재고 차감을 분리하여 각 서비스 장애가 서로 영향을 주지 않습니다. 멱등성 처리(중복 메시지 방어), Dead Letter Queue(처리 실패 메시지 격리), 메시지 순서 보장(파티셔닝 키) 설계가 중요합니다. 트랜잭션과 메시지 발행의 원자성을 보장하기 위해 Transactional Outbox 패턴을 적용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring Security OAuth2 Resource Server와 Authorization Server의 차이를 설명해주세요.', '스프링 시큐리티 오오스투 리소스 서버와 어썬라이제이션 서버의 차이를 설명해주세요.', 'SPRING_SECURITY',
 'Authorization Server는 사용자 인증 후 Access Token을 발급하는 주체이며(Spring Authorization Server 라이브러리), Resource Server는 토큰을 검증하여 보호된 자원에 대한 접근을 허용하는 서버입니다. spring-security-oauth2-resource-server로 JWT 검증을 설정하면, 요청 헤더의 Bearer 토큰을 jwk-set-uri의 공개키로 검증하거나 직접 Secret으로 검증합니다. MSA 환경에서 각 서비스가 Resource Server로 동작하여 중앙 Auth 서버에서 발급한 토큰을 독립적으로 검증합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Resilience4j를 활용한 Circuit Breaker 패턴 구현 방법을 설명해주세요.', '레질리언스포제이를 활용한 서킷 브레이커 패턴 구현 방법을 설명해주세요.', 'SPRING_CORE',
 'Circuit Breaker는 외부 서비스 장애가 연쇄 전파되는 것을 방지하는 패턴으로, CLOSED(정상), OPEN(차단), HALF_OPEN(탐색) 세 상태를 가집니다. Resilience4j의 @CircuitBreaker(name="service", fallbackMethod="fallback")로 선언하고, 실패율(failureRateThreshold), 슬라이딩 윈도우 크기, OPEN 상태 유지 시간 등을 설정합니다. Fallback 메서드에서 캐시 응답이나 기본값을 반환하여 부분 장애 시에도 시스템이 기능을 유지합니다. Retry, Bulkhead, Rate Limiter를 조합하여 외부 서비스 호출의 복원력을 높입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring Batch에서 Partitioning과 Multi-Threading의 차이와 사용 시나리오는?', '스프링 배치에서 파티셔닝과 멀티-스레딩의 차이와 사용 시나리오는?', 'SPRING_BOOT',
 'Partitioning은 데이터를 여러 파티션으로 분할하고 각 파티션을 독립적인 Step Worker가 처리하는 방식으로, 데이터를 ID 범위나 파일별로 나눠 병렬 처리합니다. Multi-Threading Step은 단일 Step 내에서 청크 처리를 멀티스레드로 실행하는 방식으로, 순서 보장이 필요 없는 데이터에 적합합니다. Partitioning은 각 파티션이 완전히 독립적인 재시작이 가능하여 대용량 배치에 안정적이고, Multi-Thread Step은 간단하지만 스레드 안전한 ItemReader가 필요합니다. 원격 파티셔닝(Remote Partitioning)으로 여러 서버에 분산 처리도 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Domain-Driven Design(DDD)을 Spring Boot 프로젝트에 적용하는 방법은?', '도메인-드리븐 디자인을 스프링 부트 프로젝트에 적용하는 방법은?', 'SPRING_CORE',
 'DDD에서 Bounded Context를 Spring 모듈이나 패키지로 매핑하고, 각 컨텍스트 내에 Entity, Value Object, Aggregate, Repository, Domain Service를 정의합니다. Aggregate Root만 외부에서 직접 접근 가능하게 하고 내부 엔티티는 Aggregate Root를 통해서만 변경합니다. Application Service(Spring @Service)는 Usecase를 조율하고 도메인 로직은 Entity와 Domain Service에 위치시킵니다. 컨텍스트 간 통신은 Domain Event로 결합도를 낮추며, Infrastructure(JPA Repository, 외부 API 클라이언트)는 인터페이스로 격리하여 포트-어댑터 패턴을 적용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring Boot 애플리케이션의 성능 프로파일링과 병목 분석 방법은?', '스프링 부트 애플리케이션의 성능 프로파일링과 병목 분석 방법은?', 'SPRING_BOOT',
 'Spring Boot Actuator와 Micrometer를 통해 HTTP 요청 지연시간, DB 쿼리 실행 시간, JVM 메트릭을 Prometheus로 수집하고 Grafana로 시각화합니다. Slow Query는 spring.jpa.properties.hibernate.format_sql 및 p6spy로 실행 쿼리와 바인딩 파라미터를 로깅하여 분석합니다. APM 도구(Elastic APM, Datadog, Pinpoint)로 분산 추적을 통해 트랜잭션별 병목 지점을 시각화합니다. 부하 테스트(Gatling, k6)로 임계 부하에서의 응답 시간, 에러율, 스레드 풀 고갈 여부를 측정하여 튜닝 방향을 결정합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Java 21 Virtual Thread와 Spring Boot 3.2+의 통합 방법 및 한계점은?', '자바 이십일 버추얼 스레드와 스프링 부트 삼점이플러스의 통합 방법 및 한계점은?', 'SPRING_BOOT',
 'Java 21 Virtual Thread(Project Loom)는 JVM이 관리하는 경량 스레드로, 블로킹 I/O 중에 Carrier Thread를 반납하여 적은 OS 스레드로 높은 동시성을 처리합니다. Spring Boot 3.2+에서 spring.threads.virtual.enabled: true 설정으로 Tomcat과 @Async에서 Virtual Thread를 활성화합니다. 블로킹 코드를 그대로 사용하면서 비동기 수준의 처리량을 얻을 수 있어 마이그레이션 비용이 낮습니다. 단, synchronized 블록은 Carrier Thread를 점유(pinning)하므로 ReentrantLock으로 교체해야 하며, CPU 바운드 작업은 Virtual Thread보다 일반 스레드 풀이 효율적입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring 애플리케이션에서 분산 캐싱(Redis Cluster)과 Cache Aside 패턴 구현 방법은?', '스프링 애플리케이션에서 분산 캐싱과 캐시 어사이드 패턴 구현 방법은?', 'SPRING_CORE',
 'Cache Aside 패턴은 애플리케이션이 캐시 읽기 실패(Cache Miss) 시 DB에서 조회하고 캐시에 저장한 후 반환하는 방식으로, 캐시와 DB 사이의 결합을 최소화합니다. Spring의 @Cacheable은 Cache Aside를 자동으로 구현하며, Redis CacheManager와 spring-boot-starter-data-redis로 연결합니다. Cache Stampede(대규모 Cache Miss 동시 발생) 방어로 Redis Lock이나 Probabilistic Early Expiration 기법을 적용합니다. Redis Cluster 환경에서 @Cacheable의 key 설계 시 해시 슬롯 분산을 고려하고, 직렬화 포맷으로 JSON(GenericJackson2JsonRedisSerializer)을 사용하여 가독성과 버전 호환성을 확보합니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- 추가 5문항 (목표 90문항 달성)

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring Security에서 Method Security(@PreAuthorize, @PostAuthorize)의 동작 원리와 SpEL 활용을 설명해주세요.', '스프링 시큐리티에서 메서드 시큐리티의 동작 원리와 스펠 활용을 설명해주세요.', 'SPRING_SECURITY',
 '@PreAuthorize는 메서드 실행 전, @PostAuthorize는 실행 후 권한을 검사합니다. @EnableMethodSecurity(prePostEnabled=true)로 활성화하며, AOP 프록시가 메서드 호출을 가로챕니다. SpEL로 hasRole(''ADMIN''), hasAuthority(''READ''), #userId == authentication.principal.id처럼 메서드 파라미터와 인증 객체를 결합한 세밀한 권한 제어가 가능합니다. @PostFilter와 @PreFilter로 컬렉션 단위의 필터링도 지원합니다. Self-invocation 시 프록시를 거치지 않아 Method Security가 동작하지 않으므로 주의합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'JPA의 @EntityGraph를 활용한 N+1 문제 해결 방법과 JPQL fetch join의 차이를 설명해주세요.', '제이피에이의 엔티티그래프를 활용한 엔플러스원 문제 해결 방법과 제이피큐엘 페치 조인의 차이를 설명해주세요.', 'JPA',
 '@EntityGraph는 지연 로딩으로 설정된 연관관계를 특정 쿼리에서만 즉시 로딩으로 오버라이드합니다. @NamedEntityGraph로 엔티티에 그래프를 정의하거나 @EntityGraph(attributePaths={"orders"})로 리포지토리 메서드에 직접 선언합니다. JPQL fetch join은 쿼리를 직접 작성하여 유연하지만 페이지네이션과 함께 사용하면 HibernateJpaDialect 경고(firstResult/maxResults specified with collection fetch)가 발생합니다. 컬렉션 fetch join 시 DISTINCT가 필요하고, 둘 이상의 컬렉션에는 MultipleBagFetchException이 발생하므로 Set 사용이나 별도 쿼리 분리가 필요합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring Boot에서 Redis를 활용한 분산 락(Distributed Lock) 구현 방법을 설명해주세요.', '스프링 부트에서 레디스를 활용한 분산 락 구현 방법을 설명해주세요.', 'SPRING_CORE',
 '분산 환경에서 여러 인스턴스가 동일 자원에 접근할 때 DB 락이나 synchronized만으로는 부족하여 분산 락이 필요합니다. Redis의 SET key value NX PX timeout 명령으로 원자적으로 락을 획득하고 expire로 데드락을 방지합니다. Redisson 라이브러리는 RLock 인터페이스로 재진입 락, Fair Lock, Read-Write Lock을 제공하고 watchdog으로 락 만료를 자동 연장합니다. Lettuce로 직접 구현 시 스핀 락은 Redis에 부하를 주므로 Redisson의 pub/sub 기반 락 해제 알림을 활용하는 것이 권장됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring Boot에서 예외 처리 전략과 커스텀 예외 클래스 설계 방법을 설명해주세요.', '스프링 부트에서 예외 처리 전략과 커스텀 예외 클래스 설계 방법을 설명해주세요.', 'SPRING_MVC',
 '커스텀 예외는 RuntimeException을 상속하고 에러 코드와 메시지를 포함하는 기반 클래스(BusinessException)를 만들고, 도메인별로 세분화(UserNotFoundException, InsufficientBalanceException)합니다. @RestControllerAdvice에서 @ExceptionHandler(BusinessException.class)로 잡아 에러 코드와 메시지를 포함한 일관된 응답 DTO를 반환합니다. @ResponseStatus 대신 ResponseEntity로 HTTP 상태 코드를 명시적으로 제어합니다. 예측하지 못한 Exception은 별도 핸들러에서 500을 반환하고 내부 정보가 노출되지 않도록 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:JAVA_SPRING:LANGUAGE_FRAMEWORK', 'Spring의 @Transactional 격리 수준 설정과 각 수준에서 발생하는 문제를 설명해주세요.', '스프링의 트랜잭셔널 어노테이션 격리 수준 설정과 각 수준에서 발생하는 문제를 설명해주세요.', 'SPRING_DATA',
 '@Transactional(isolation=Isolation.READ_COMMITTED)처럼 격리 수준을 설정하며 기본값은 DEFAULT(DB 기본값 사용)입니다. READ_UNCOMMITTED는 Dirty Read가 발생하여 실무에서 사용하지 않습니다. READ_COMMITTED는 Dirty Read 방지, Non-Repeatable Read 발생 가능합니다. REPEATABLE_READ(InnoDB 기본값)는 Non-Repeatable Read 방지, Phantom Read 가능합니다. SERIALIZABLE은 모든 이상 현상을 방지하지만 동시성이 크게 저하됩니다. InnoDB는 REPEATABLE_READ에서 Gap Lock으로 Phantom Read도 부분적으로 방지합니다.',
 'MODEL_ANSWER', TRUE, NOW());
