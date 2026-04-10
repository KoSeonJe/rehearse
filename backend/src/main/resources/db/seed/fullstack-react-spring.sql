-- fullstack-react-spring.sql
-- Fullstack React+Spring 시드 데이터 (90문항: JUNIOR30, MID30, SENIOR30)
-- cache_key: FULLSTACK:{Level}:REACT_SPRING:FULLSTACK_STACK

-- ============================================================
-- JUNIOR (30문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:JUNIOR:REACT_SPRING:FULLSTACK_STACK', 'React에서 Spring Boot API를 호출하는 기본 방법을 설명해주세요.', 'API_DESIGN',
 'fetch() 또는 axios를 사용해 Spring Boot가 제공하는 REST API 엔드포인트에 HTTP 요청을 보냅니다. 기본적으로 React 컴포넌트의 useEffect 훅에서 API를 호출하고 useState로 응답 데이터와 로딩/에러 상태를 관리합니다. 실제 프로젝트에서는 TanStack Query(react-query)를 사용해 캐싱, 재시도, 로딩 상태를 자동으로 처리하는 것이 권장됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:JUNIOR:REACT_SPRING:FULLSTACK_STACK', 'Spring Boot에서 REST API를 설계할 때 HTTP 메서드를 어떻게 선택하나요?', 'API_DESIGN',
 'GET은 리소스 조회(멱등성, 부수효과 없음), POST는 리소스 생성, PUT은 리소스 전체 수정(멱등성), PATCH는 부분 수정, DELETE는 삭제에 사용합니다. 리소스 URI는 명사를 사용하고(/users, /orders), 행위는 HTTP 메서드로 표현합니다. 응답 상태 코드도 의미에 맞게 200(OK), 201(Created), 204(No Content), 400(Bad Request), 404(Not Found), 409(Conflict) 등을 구분해 반환해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:JUNIOR:REACT_SPRING:FULLSTACK_STACK', 'JSON 직렬화/역직렬화란 무엇이며 Spring Boot에서 어떻게 처리되나요?', 'DATA_FORMAT',
 '직렬화(Serialization)는 Java 객체를 JSON 문자열로 변환하는 것이고, 역직렬화는 JSON을 Java 객체로 변환하는 것입니다. Spring Boot는 Jackson 라이브러리를 기본으로 포함하며 @ResponseBody 또는 @RestController에서 객체를 반환하면 자동으로 JSON으로 변환됩니다. @JsonProperty, @JsonIgnore, @JsonFormat 어노테이션으로 직렬화 동작을 세밀하게 제어할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:JUNIOR:REACT_SPRING:FULLSTACK_STACK', 'CORS(Cross-Origin Resource Sharing)란 무엇이며 Spring Boot에서 어떻게 설정하나요?', 'CORS',
 'CORS는 다른 출처(도메인, 포트, 프로토콜)에서 리소스를 요청할 때 브라우저가 적용하는 보안 정책입니다. Spring Boot에서는 @CrossOrigin 어노테이션을 컨트롤러나 메서드에 적용하거나, WebMvcConfigurer를 구현해 addCorsMappings()로 전역 설정합니다. 허용할 origins, methods, headers를 명시하며, 프로덕션에서는 * 대신 실제 프론트엔드 도메인만 허용해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:JUNIOR:REACT_SPRING:FULLSTACK_STACK', 'JWT(JSON Web Token)의 구조와 인증 흐름을 설명해주세요.', 'AUTHENTICATION',
 'JWT는 Header(알고리즘), Payload(클레임), Signature 세 부분이 Base64URL로 인코딩되어 점(.)으로 구분된 토큰입니다. 사용자가 로그인하면 서버가 JWT를 발급하고, 이후 요청마다 Authorization: Bearer {token} 헤더로 토큰을 전송합니다. 서버는 Signature를 검증해 토큰의 무결성을 확인하며, Payload를 복호화해 사용자 정보와 권한을 파악합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:JUNIOR:REACT_SPRING:FULLSTACK_STACK', 'Spring Boot에서 JWT 기반 인증을 구현하는 기본 흐름을 설명해주세요.', 'AUTHENTICATION',
 'Spring Security에 JwtAuthenticationFilter를 추가해 모든 요청의 Authorization 헤더에서 토큰을 추출하고 검증합니다. 로그인 엔드포인트(/auth/login)에서 자격증명을 검증 후 Access Token과 Refresh Token을 발급합니다. 토큰 검증에는 jjwt 라이브러리를 주로 사용하며, 서명 키는 환경변수로 관리하고 코드에 하드코딩하지 않아야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:JUNIOR:REACT_SPRING:FULLSTACK_STACK', 'React에서 인증 상태를 관리하는 방법을 설명해주세요.', 'AUTHENTICATION',
 'JWT를 localStorage 또는 HttpOnly 쿠키에 저장하며, 보안을 위해 HttpOnly 쿠키가 권장됩니다. 전역 인증 상태는 Context API 또는 Zustand로 관리하고, 로그인/로그아웃 액션과 현재 사용자 정보를 포함합니다. 인증이 필요한 페이지는 PrivateRoute 컴포넌트로 감싸 미인증 사용자를 로그인 페이지로 리다이렉트합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:JUNIOR:REACT_SPRING:FULLSTACK_STACK', 'Spring Boot API의 입력값 검증(Validation)을 어떻게 처리하나요?', 'API_DESIGN',
 'Jakarta Bean Validation(javax.validation)의 @NotNull, @NotBlank, @Size, @Email 등의 어노테이션을 DTO 필드에 선언합니다. 컨트롤러 메서드 파라미터에 @Valid 또는 @Validated를 붙이면 자동으로 검증이 수행됩니다. 검증 실패 시 MethodArgumentNotValidException이 발생하며, @ControllerAdvice로 전역 핸들러를 만들어 일관된 에러 응답을 반환합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:JUNIOR:REACT_SPRING:FULLSTACK_STACK', '개발 환경과 프로덕션 환경을 분리하는 방법을 설명해주세요.', 'DATA_FORMAT',
 'Spring Boot는 application.properties 또는 application.yml에서 spring.profiles.active로 활성 프로파일을 지정하고, application-dev.yml, application-prod.yml로 환경별 설정을 분리합니다. React(Vite)에서는 .env.development, .env.production 파일로 API URL 등 환경별 변수를 관리합니다. 민감한 정보(DB 비밀번호, JWT 시크릿)는 환경변수나 Secret Manager를 통해 주입하고 소스 코드에 포함하지 않습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:JUNIOR:REACT_SPRING:FULLSTACK_STACK', 'React에서 API 요청 시 로딩과 에러 상태를 처리하는 방법을 설명해주세요.', 'API_DESIGN',
 'useState로 isLoading, error, data 세 가지 상태를 관리하고, try/catch/finally 패턴으로 API 호출 전 isLoading을 true, 완료 후 false로 변경합니다. TanStack Query를 사용하면 { data, isLoading, isError, error } 를 자동으로 제공해 보일러플레이트를 줄입니다. 에러 타입별로 사용자에게 적절한 메시지를 표시하고, 재시도 버튼을 제공하는 것이 좋은 UX입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:JUNIOR:REACT_SPRING:FULLSTACK_STACK', 'Spring Boot에서 페이지네이션 API를 구현하는 방법을 설명해주세요.', 'API_DESIGN',
 'Spring Data JPA의 Pageable 인터페이스를 컨트롤러 파라미터로 받으면 page, size, sort 쿼리 파라미터를 자동으로 바인딩합니다. Repository에서 Page<T> 또는 Slice<T>를 반환하면 데이터, 전체 개수, 현재 페이지 등의 메타 정보가 포함됩니다. React에서는 TanStack Query의 useInfiniteQuery로 무한 스크롤, usePaginatedQuery로 페이지 기반 UI를 구현합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:JUNIOR:REACT_SPRING:FULLSTACK_STACK', 'HTTP 클라이언트로 axios를 사용하는 이유와 기본 설정 방법을 설명해주세요.', 'API_DESIGN',
 'axios는 fetch에 비해 요청/응답 인터셉터, 자동 JSON 변환, 요청 취소, 타임아웃 설정 등 편의 기능을 제공합니다. axios 인스턴스를 생성해 baseURL, timeout, 공통 헤더를 설정하고 인터셉터로 JWT 토큰을 자동으로 Authorization 헤더에 추가합니다. 응답 인터셉터에서 401 에러를 감지해 토큰 갱신 로직을 처리하는 패턴이 일반적입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:JUNIOR:REACT_SPRING:FULLSTACK_STACK', 'Spring Boot의 ResponseEntity를 활용한 응답 제어 방법을 설명해주세요.', 'API_DESIGN',
 'ResponseEntity<T>를 반환하면 HTTP 상태 코드, 헤더, 바디를 모두 제어할 수 있습니다. ResponseEntity.ok(data), ResponseEntity.created(location).body(data), ResponseEntity.noContent().build() 등의 빌더 메서드로 직관적으로 구성합니다. 팀 컨벤션으로 통일된 응답 포맷 DTO(ApiResponse<T>)를 만들어 모든 API가 일관된 구조를 반환하도록 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:JUNIOR:REACT_SPRING:FULLSTACK_STACK', '프론트엔드에서 API 타입을 TypeScript로 안전하게 관리하는 방법을 설명해주세요.', 'DATA_FORMAT',
 'Spring Boot API의 요청/응답 DTO와 동일한 구조를 TypeScript interface 또는 type으로 프론트엔드에 정의합니다. OpenAPI 스펙을 Spring Boot에서 자동 생성(Springdoc OpenAPI)하고 openapi-typescript로 TypeScript 타입을 자동 생성해 수동 동기화 오류를 방지합니다. API 응답에 제네릭 타입 ApiResponse<T>를 정의해 data, status, message 래퍼를 일관되게 처리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:JUNIOR:REACT_SPRING:FULLSTACK_STACK', 'Spring Boot에서 전역 예외 처리를 구현하는 방법을 설명해주세요.', 'API_DESIGN',
 '@ControllerAdvice와 @ExceptionHandler를 조합해 전역 예외 처리 클래스를 만들고, 특정 예외 타입별 응답 형식을 정의합니다. EntityNotFoundException은 404, ValidationException은 400, AccessDeniedException은 403 등 의미에 맞는 상태 코드를 반환합니다. 에러 응답 DTO에 에러 코드(예: USER_001), 메시지, 타임스탬프를 포함해 프론트엔드에서 에러 종류를 식별하기 쉽게 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:JUNIOR:REACT_SPRING:FULLSTACK_STACK', 'React에서 폼 데이터를 서버에 전송하는 방법을 설명해주세요.', 'API_DESIGN',
 'React Hook Form 또는 제어 컴포넌트로 폼 상태를 관리하고, 제출 시 객체를 JSON으로 직렬화해 POST 요청을 보냅니다. 파일 업로드는 FormData 객체를 사용하고 Content-Type을 multipart/form-data로 설정합니다. 클라이언트 사이드 검증(Zod, Yup)으로 서버 요청 전 사용자에게 즉시 피드백을 제공하고, 서버 에러도 폼 필드별로 표시합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:JUNIOR:REACT_SPRING:FULLSTACK_STACK', 'Spring Security에서 인증과 인가의 차이를 설명해주세요.', 'AUTHENTICATION',
 '인증(Authentication)은 사용자가 누구인지 확인하는 과정으로 로그인, JWT 검증 등이 해당합니다. 인가(Authorization)는 인증된 사용자가 특정 자원에 접근할 권한이 있는지 확인하는 과정입니다. Spring Security에서 인증은 AuthenticationManager, 인가는 SecurityFilterChain의 authorizeHttpRequests()와 @PreAuthorize 어노테이션으로 설정합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:JUNIOR:REACT_SPRING:FULLSTACK_STACK', 'React에서 API 요청을 커스텀 훅으로 추상화하는 방법을 설명해주세요.', 'API_DESIGN',
 'useUsers(), useCreateUser() 같은 커스텀 훅으로 API 호출 로직을 컴포넌트에서 분리합니다. TanStack Query를 기반으로 useQuery, useMutation을 래핑하면 캐시 키 관리, 옵션 설정을 일관되게 유지할 수 있습니다. 이 패턴은 컴포넌트가 데이터 페칭 세부사항에 의존하지 않게 해 테스트와 유지보수를 용이하게 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:JUNIOR:REACT_SPRING:FULLSTACK_STACK', 'Spring Boot에서 Swagger(OpenAPI)를 설정하는 방법을 설명해주세요.', 'API_DESIGN',
 'springdoc-openapi-starter-webmvc-ui 의존성을 추가하면 /v3/api-docs와 /swagger-ui.html 엔드포인트가 자동으로 생성됩니다. @Operation, @Parameter, @ApiResponse 어노테이션으로 API 문서를 보강하고, @Schema로 DTO 필드 설명을 추가합니다. 프로덕션에서는 보안을 위해 Swagger UI 엔드포인트에 접근 제한을 설정하는 것이 권장됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:JUNIOR:REACT_SPRING:FULLSTACK_STACK', 'Refresh Token을 활용한 토큰 갱신 흐름을 설명해주세요.', 'AUTHENTICATION',
 'Access Token은 짧은 만료 시간(15분~1시간)을, Refresh Token은 긴 만료 시간(7~30일)을 가집니다. Access Token이 만료되면 클라이언트는 Refresh Token으로 /auth/refresh 엔드포인트를 호출해 새 Access Token을 발급받습니다. axios 인터셉터에서 401 응답을 감지해 자동으로 토큰 갱신을 시도하고, 갱신 실패 시 로그인 페이지로 이동합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:JUNIOR:REACT_SPRING:FULLSTACK_STACK', 'Spring Boot에서 파일 업로드 API를 구현하는 방법을 설명해주세요.', 'API_DESIGN',
 '컨트롤러 메서드에서 MultipartFile을 파라미터로 받고, application.yml에 spring.servlet.multipart.max-file-size로 최대 크기를 설정합니다. 파일은 로컬 저장소, S3 같은 오브젝트 스토리지, DB BLOB 중 요구사항에 맞는 곳에 저장합니다. 응답으로 업로드된 파일의 접근 URL을 반환하고, 파일 타입과 크기 검증은 서버에서 반드시 수행해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:JUNIOR:REACT_SPRING:FULLSTACK_STACK', 'React에서 날짜/시간 데이터를 서버와 교환하는 방법을 설명해주세요.', 'DATA_FORMAT',
 '서버와 클라이언트 간 날짜는 ISO 8601 형식(yyyy-MM-ddTHH:mm:ssZ) 문자열로 주고받는 것이 권장됩니다. Spring Boot에서 LocalDateTime은 @JsonFormat(pattern = "yyyy-MM-dd''T''HH:mm:ss")으로 직렬화 형식을 지정합니다. React에서는 date-fns 또는 dayjs로 UTC 문자열을 사용자 로컬 시간으로 변환해 표시하며, 시간대(timezone) 처리에 주의해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:JUNIOR:REACT_SPRING:FULLSTACK_STACK', 'Spring Boot에서 환경변수와 설정값을 관리하는 방법을 설명해주세요.', 'DATA_FORMAT',
 '@Value("${property.name}") 또는 @ConfigurationProperties를 사용해 application.yml 값을 주입받습니다. 민감한 정보는 환경변수로 제공하고 ${DB_PASSWORD:default_value} 형식으로 기본값을 지정합니다. Spring Cloud Config 또는 AWS Parameter Store를 통해 중앙화된 설정 관리가 가능하며, 설정 변경 시 재배포 없이 갱신하는 아키텍처도 고려할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:JUNIOR:REACT_SPRING:FULLSTACK_STACK', 'React에서 전역 에러 처리를 구현하는 방법을 설명해주세요.', 'API_DESIGN',
 'Error Boundary 컴포넌트로 렌더링 에러를 포착하고 fallback UI를 표시합니다. API 에러는 axios 응답 인터셉터에서 공통 처리하며, 토스트 알림으로 사용자에게 에러를 표시합니다. TanStack Query의 QueryClient에 onError 옵션을 설정하면 모든 쿼리 에러를 한 곳에서 처리할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:JUNIOR:REACT_SPRING:FULLSTACK_STACK', 'Spring Boot에서 데이터베이스 연결을 설정하는 방법을 설명해주세요.', 'API_DESIGN',
 'spring-boot-starter-data-jpa와 MySQL 드라이버 의존성을 추가하고, application.yml에 spring.datasource.url, username, password를 설정합니다. HikariCP가 기본 커넥션 풀로 설정되며, maximum-pool-size, connection-timeout 등을 서비스 규모에 맞게 조정합니다. 개발 환경에서는 H2 인메모리 DB를 사용해 외부 DB 없이 개발하고, spring.jpa.hibernate.ddl-auto=validate로 프로덕션에서 스키마 자동 변경을 방지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:JUNIOR:REACT_SPRING:FULLSTACK_STACK', '프론트엔드 개발 시 백엔드 API를 Mock하는 방법을 설명해주세요.', 'API_DESIGN',
 'MSW(Mock Service Worker)를 사용하면 실제 네트워크 요청을 가로채 모의 응답을 반환해 백엔드 없이 프론트엔드 개발이 가능합니다. 핸들러를 실제 API 스펙과 동일하게 작성하면 백엔드 완성 후 MSW 제거만으로 실제 API로 전환됩니다. json-server는 JSON 파일로 간단한 REST API 서버를 빠르게 구성할 수 있는 대안입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:JUNIOR:REACT_SPRING:FULLSTACK_STACK', 'Spring Boot에서 로깅을 설정하고 관리하는 방법을 설명해주세요.', 'API_DESIGN',
 'Spring Boot는 Logback을 기본 로거로 사용하며 SLF4J 인터페이스를 통해 접근합니다. application.yml에서 logging.level.root, logging.level.com.yourpackage로 패키지별 로그 레벨을 설정합니다. 프로덕션에서는 JSON 형식의 구조화 로그를 출력해 ELK Stack이나 CloudWatch로 수집하기 용이하게 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:JUNIOR:REACT_SPRING:FULLSTACK_STACK', 'React에서 URL 쿼리 파라미터를 관리하는 방법을 설명해주세요.', 'API_DESIGN',
 'React Router의 useSearchParams 훅으로 쿼리 파라미터를 읽고 업데이트합니다. 검색어, 필터, 정렬, 페이지 번호 같은 상태를 URL에 저장하면 페이지 공유와 브라우저 뒤로 가기가 올바르게 동작합니다. nuqs 같은 라이브러리를 사용하면 쿼리 파라미터를 타입 안전하게 관리하고 기본값 설정도 편리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:JUNIOR:REACT_SPRING:FULLSTACK_STACK', 'HTTPS와 HTTP의 차이점과 Spring Boot에서 HTTPS를 적용하는 방법을 설명해주세요.', 'CORS',
 'HTTPS는 TLS/SSL 암호화를 통해 데이터 전송을 보호하며, JWT 토큰이나 개인정보가 네트워크에서 평문으로 노출되지 않습니다. Spring Boot에서는 server.ssl.key-store, key-store-password, key-store-type을 설정해 HTTPS를 활성화합니다. 실제 프로덕션에서는 Nginx나 AWS ALB를 SSL 종료 지점으로 사용하고 백엔드는 HTTP로 통신하는 구조가 일반적입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:JUNIOR:REACT_SPRING:FULLSTACK_STACK', 'Spring Boot에서 DTO와 Entity를 분리해야 하는 이유를 설명해주세요.', 'API_DESIGN',
 'Entity를 직접 API 응답으로 노출하면 불필요한 내부 정보가 유출되거나 양방향 관계로 인한 무한 직렬화 문제가 발생합니다. DTO는 API 계약을 명확히 정의하고, Entity 변경이 API 스펙에 영향을 주지 않도록 격리합니다. 요청(RequestDTO)과 응답(ResponseDTO)을 분리하면 입력 검증과 출력 포맷을 독립적으로 제어할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());


-- ============================================================
-- MID (30문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:MID:REACT_SPRING:FULLSTACK_STACK', 'BFF(Backend for Frontend) 패턴이란 무엇이며 언제 도입하나요?', 'ARCHITECTURE',
 'BFF는 프론트엔드 전용 API 게이트웨이로, 여러 마이크로서비스의 데이터를 집계하고 변환해 프론트엔드에 최적화된 응답을 제공합니다. 모바일 앱과 웹 앱의 요구사항이 다를 때 각각 별도 BFF를 두어 독립적으로 발전시킬 수 있습니다. 단, BFF가 비즈니스 로직을 포함하지 않도록 주의하며, 오케스트레이션과 변환만 담당해야 유지보수성이 유지됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:MID:REACT_SPRING:FULLSTACK_STACK', 'REST API 버저닝 전략을 설명하고 각 방식의 장단점을 비교해주세요.', 'ARCHITECTURE',
 'URL 경로 버저닝(/api/v1/users)은 직관적이고 캐싱 친화적이지만 URL이 길어집니다. 헤더 버저닝(Accept: application/vnd.api.v2+json)은 URL이 깔끔하지만 브라우저 테스트가 어렵습니다. 쿼리 파라미터 버저닝(/api/users?version=2)은 선택적으로 사용할 수 있지만 캐싱 키가 복잡해집니다. 실무에서는 URL 경로 방식이 가장 널리 사용되며, Breaking Change가 있을 때만 버전을 올리고 Non-Breaking Change는 기존 버전에서 처리하는 것이 권장됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:MID:REACT_SPRING:FULLSTACK_STACK', 'Spring Boot에서 WebSocket을 구현하는 방법을 설명해주세요.', 'REALTIME',
 'spring-boot-starter-websocket 의존성을 추가하고 @EnableWebSocketMessageBroker로 STOMP 메시지 브로커를 설정합니다. 클라이언트는 SockJS + StompJS로 /ws 엔드포인트에 연결하고, 토픽(/topic)과 큐(/queue)를 구독합니다. 실시간 알림, 채팅, 대시보드 업데이트에 적합하며, 수평 확장 시 Redis Pub/Sub를 메시지 브로커로 사용해 다중 서버 인스턴스 간 메시지를 동기화합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:MID:REACT_SPRING:FULLSTACK_STACK', 'SSR, CSR, SSG의 차이와 React+Spring 조합에서 각 방식의 적용 시점을 설명해주세요.', 'ARCHITECTURE',
 'CSR(Client-Side Rendering)은 React SPA로 초기 로드 후 클라이언트에서 렌더링하며, Spring API가 데이터를 제공합니다. SSR은 Next.js가 서버에서 HTML을 생성해 SEO와 초기 로딩 성능이 중요한 페이지에 적합합니다. SSG는 빌드 타임에 HTML을 생성해 블로그, 문서처럼 자주 바뀌지 않는 콘텐츠에 최적입니다. 대시보드처럼 인증이 필요하고 SEO가 불필요한 페이지는 CSR, 랜딩 페이지는 SSG나 SSR을 선택합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:MID:REACT_SPRING:FULLSTACK_STACK', 'Spring Boot에서 캐싱을 구현하는 방법을 설명해주세요.', 'ARCHITECTURE',
 '@EnableCaching + @Cacheable("users") 어노테이션으로 메서드 결과를 캐싱하고, @CacheEvict로 데이터 변경 시 캐시를 무효화합니다. 기본 ConcurrentHashMap 캐시는 분산 환경에서 서버 간 일관성이 없으므로, 프로덕션에서는 Redis를 캐시 스토어로 사용합니다. TTL(Time-To-Live)을 적절히 설정하고, 캐시 키를 주의 깊게 설계해 파라미터별 캐시가 올바르게 분리되도록 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:MID:REACT_SPRING:FULLSTACK_STACK', 'React에서 Optimistic Update를 구현하는 방법을 설명해주세요.', 'REALTIME',
 'Optimistic Update는 서버 응답을 기다리지 않고 UI를 먼저 업데이트해 반응성을 높이는 패턴입니다. TanStack Query의 useMutation에서 onMutate로 캐시를 즉시 업데이트하고, onError에서 이전 상태로 롤백합니다. 좋아요, 체크박스, 간단한 폼 제출처럼 서버 실패 확률이 낮고 즉각적인 피드백이 중요한 인터랙션에 적합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:MID:REACT_SPRING:FULLSTACK_STACK', '풀스택 애플리케이션의 통합 테스트(Integration Test) 전략을 설명해주세요.', 'TESTING',
 'Spring Boot에서 @SpringBootTest + @Transactional로 실제 DB를 사용하는 통합 테스트를 작성하고, Testcontainers로 Docker 기반 MySQL 컨테이너를 테스트에서 사용합니다. React에서는 MSW로 API를 모킹한 컴포넌트 통합 테스트를 작성합니다. E2E는 Playwright/Cypress로 실제 브라우저에서 프론트-백 연동 전체를 검증하며, CI에서 자동 실행합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:MID:REACT_SPRING:FULLSTACK_STACK', 'Spring Boot 애플리케이션의 @Transactional 동작 원리를 설명해주세요.', 'ARCHITECTURE',
 '@Transactional은 AOP 프록시를 통해 메서드 실행 전 트랜잭션을 시작하고, 정상 완료 시 commit, 런타임 예외 발생 시 rollback합니다. propagation 속성으로 중첩 트랜잭션 동작을 제어하며, REQUIRED(기본)는 기존 트랜잭션에 참여하고 없으면 새로 생성합니다. 같은 클래스 내부 메서드 호출 시 프록시를 거치지 않아 @Transactional이 동작하지 않는 자기 호출(self-invocation) 문제에 주의해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:MID:REACT_SPRING:FULLSTACK_STACK', 'GraphQL과 REST를 비교하고 React+Spring 환경에서 GraphQL을 적용하는 방법을 설명해주세요.', 'ARCHITECTURE',
 'REST는 고정된 엔드포인트에서 서버가 정의한 형태를 반환하지만, GraphQL은 클라이언트가 필요한 필드만 선택적으로 요청해 Over-fetching/Under-fetching을 해결합니다. Spring Boot에서 spring-graphql로 스키마를 정의하고 @QueryMapping, @MutationMapping으로 리졸버를 구현합니다. React에서는 Apollo Client 또는 urql로 쿼리를 작성하며, @graphql-codegen으로 스키마에서 TypeScript 타입을 자동 생성합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:MID:REACT_SPRING:FULLSTACK_STACK', '모노레포(Monorepo)에서 React와 Spring Boot를 함께 관리하는 전략을 설명해주세요.', 'ARCHITECTURE',
 'pnpm workspace + Turborepo로 프론트엔드 패키지들을 관리하고, Gradle 멀티프로젝트로 Spring Boot 모듈을 구성합니다. 공통 타입 정의, API 스펙(OpenAPI), 테스트 유틸리티를 별도 패키지로 분리해 FE/BE가 공유합니다. CI에서 변경된 패키지만 빌드/테스트하는 incremental build로 파이프라인 속도를 유지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:MID:REACT_SPRING:FULLSTACK_STACK', 'React와 Spring Boot 애플리케이션에서 E2E 테스트를 작성하는 방법을 설명해주세요.', 'TESTING',
 'Playwright로 실제 브라우저에서 사용자 시나리오를 테스트하며, Spring Boot는 별도 프로파일(test)로 H2 또는 Testcontainers MySQL을 사용합니다. 테스트 데이터는 @Sql 또는 API를 통해 각 테스트 전에 설정하고 후에 정리합니다. 인증이 필요한 페이지는 storageState로 세션을 저장해 로그인 단계를 재사용하고, CI에서 Docker Compose로 전체 스택을 구동 후 테스트를 실행합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:MID:REACT_SPRING:FULLSTACK_STACK', 'Spring Boot의 JPA N+1 문제를 해결하는 방법을 설명해주세요.', 'ARCHITECTURE',
 'N+1 문제는 연관 엔티티를 lazy loading으로 가져올 때 N개의 추가 쿼리가 발생하는 현상입니다. Fetch Join(JPQL의 JOIN FETCH)으로 연관 데이터를 한 번에 조회하거나, @EntityGraph로 즉시 로딩 범위를 선언합니다. 대규모 컬렉션은 BatchSize 설정으로 IN 쿼리로 배치 로딩하고, 복잡한 조회는 QueryDSL 또는 네이티브 쿼리를 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:MID:REACT_SPRING:FULLSTACK_STACK', 'React Query(TanStack Query)의 캐시 전략과 invalidation 방법을 설명해주세요.', 'ARCHITECTURE',
 'TanStack Query는 쿼리 키를 기준으로 캐시를 관리하며, staleTime과 gcTime으로 데이터 신선도와 메모리 보유 시간을 제어합니다. 데이터 변경(mutation) 후 queryClient.invalidateQueries({ queryKey: [''users''] })로 관련 캐시를 무효화해 자동 재요청을 트리거합니다. setQueryData로 mutation 응답으로 캐시를 직접 업데이트하면 재요청 없이 UI를 갱신하는 Optimistic Update도 구현 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:MID:REACT_SPRING:FULLSTACK_STACK', 'Spring Boot에서 Rate Limiting을 구현하는 방법을 설명해주세요.', 'ARCHITECTURE',
 'Bucket4j 라이브러리로 토큰 버킷 알고리즘 기반 Rate Limiting을 구현하며, Redis를 백엔드로 사용하면 분산 환경에서도 일관성을 유지합니다. Resilience4j의 @RateLimiter 어노테이션으로 선언적으로 적용하거나, Spring Security 필터 체인에서 IP/사용자별로 제한할 수 있습니다. 429 Too Many Requests 응답에 Retry-After 헤더를 포함해 클라이언트가 대기 시간을 알 수 있게 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:MID:REACT_SPRING:FULLSTACK_STACK', 'Spring Boot에서 비동기 처리와 이벤트 기반 아키텍처를 구현하는 방법을 설명해주세요.', 'ARCHITECTURE',
 '@Async로 메서드를 비동기 실행하고 CompletableFuture를 반환해 비차단 처리를 구현합니다. ApplicationEventPublisher와 @EventListener로 도메인 이벤트를 발행/구독해 컴포넌트 간 결합도를 낮춥니다. 장기 실행 작업은 Spring Batch 또는 @Scheduled로 처리하고, 외부 시스템과는 Kafka나 RabbitMQ를 통해 비동기 메시지를 교환합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:MID:REACT_SPRING:FULLSTACK_STACK', 'React에서 상태 관리 솔루션을 선택하는 기준을 설명해주세요.', 'ARCHITECTURE',
 '서버 상태(API 데이터)는 TanStack Query, 전역 클라이언트 상태(인증, 테마)는 Zustand 또는 Jotai, 컴포넌트 지역 상태는 useState/useReducer로 분리하는 것이 권장됩니다. Redux는 강력한 DevTools와 미들웨어 생태계가 있지만 보일러플레이트가 많아 중간 규모 이하에서는 과도합니다. 상태를 서버 상태와 클라이언트 상태로 명확히 분리하면 각 도구의 강점을 최대한 활용할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:MID:REACT_SPRING:FULLSTACK_STACK', '풀스택 애플리케이션 배포를 위한 Docker 컨테이너화 방법을 설명해주세요.', 'DEPLOYMENT',
 'Spring Boot는 멀티스테이지 Dockerfile로 빌드 단계(JDK)와 실행 단계(JRE 또는 distroless)를 분리해 이미지 크기를 최소화합니다. React 빌드 결과물은 Nginx 이미지에 복사해 정적 파일을 서빙합니다. docker-compose.yml로 프론트엔드, 백엔드, DB를 하나의 네트워크로 연결해 로컬 개발 환경을 구성하고, .env 파일로 환경별 설정을 주입합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:MID:REACT_SPRING:FULLSTACK_STACK', 'Spring Boot에서 데이터베이스 마이그레이션을 관리하는 방법을 설명해주세요.', 'DEPLOYMENT',
 'Flyway 또는 Liquibase로 버전 관리된 마이그레이션 스크립트를 관리합니다. Flyway는 V1__create_table.sql 형식의 파일을 순서대로 실행하며, 이미 실행된 스크립트는 체크섬으로 변경 여부를 감지합니다. 모든 스키마 변경은 마이그레이션 파일로 관리하고 롤백 가능하게 작성하며, 프로덕션에서는 ddl-auto=validate로 자동 스키마 변경을 금지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:MID:REACT_SPRING:FULLSTACK_STACK', 'React와 Spring Boot 간 CSRF 보호를 구현하는 방법을 설명해주세요.', 'TESTING',
 'SPA에서는 쿠키가 아닌 Authorization 헤더로 JWT를 전송하므로 CSRF 공격에 덜 취약하지만, 쿠키 기반 세션을 사용하면 CSRF 보호가 필요합니다. Spring Security의 CsrfToken을 API 응답 헤더나 쿠키로 제공하고, React에서 모든 상태 변경 요청(POST/PUT/DELETE)에 X-CSRF-TOKEN 헤더를 포함합니다. SameSite=Strict 또는 Lax 쿠키 정책을 설정하면 대부분의 CSRF 공격을 방어합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:MID:REACT_SPRING:FULLSTACK_STACK', 'Spring Boot에서 API 문서를 자동 생성하고 프론트엔드에서 활용하는 방법을 설명해주세요.', 'ARCHITECTURE',
 'springdoc-openapi로 OpenAPI 3.0 스펙을 자동 생성하고, /v3/api-docs.yaml로 스펙 파일을 노출합니다. openapi-typescript 또는 orval CLI로 스펙에서 TypeScript 타입과 API 클라이언트 코드를 자동 생성해 수동 타입 정의 오류를 제거합니다. CI 파이프라인에서 스펙 생성을 자동화하면 백엔드 변경 시 프론트엔드 타입이 자동으로 동기화됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:MID:REACT_SPRING:FULLSTACK_STACK', 'React에서 Server-Sent Events(SSE)를 활용한 실시간 업데이트 구현 방법을 설명해주세요.', 'REALTIME',
 'SSE는 서버가 클라이언트로 단방향 스트림을 푸시하는 HTTP 기반 프로토콜로, WebSocket보다 단순하고 HTTP 인프라와 호환됩니다. Spring Boot에서 SseEmitter를 반환하고 emitter.send()로 이벤트를 전송합니다. React에서는 EventSource API 또는 TanStack Query의 스트리밍 지원으로 구독하며, 연결 끊김 시 자동 재연결 로직을 구현합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:MID:REACT_SPRING:FULLSTACK_STACK', '풀스택 애플리케이션의 로깅과 모니터링 전략을 설명해주세요.', 'DEPLOYMENT',
 'Spring Boot에서 구조화 로그(JSON)와 MDC(Mapped Diagnostic Context)로 요청 ID를 로그에 포함해 분산 추적을 가능하게 합니다. 프론트엔드 에러는 Sentry로 캡처하고, 백엔드는 Spring Boot Actuator + Micrometer로 Prometheus/Grafana에 메트릭을 노출합니다. 사용자 행동 분석은 Amplitude나 Mixpanel, 인프라 모니터링은 CloudWatch나 Datadog로 각 목적에 맞는 도구를 분리해 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:MID:REACT_SPRING:FULLSTACK_STACK', 'Spring Boot에서 이벤트 소싱(Event Sourcing)을 구현하는 방법을 설명해주세요.', 'ARCHITECTURE',
 '이벤트 소싱은 상태를 직접 저장하는 대신 상태 변경 이벤트를 순서대로 저장하고 재생해 현재 상태를 도출합니다. Spring에서 ApplicationEvent를 도메인 이벤트로 활용하고, 이벤트를 별도 event_store 테이블에 직렬화해 영속화합니다. CQRS와 결합하면 이벤트 스트림에서 다양한 읽기 모델을 파생시킬 수 있으며, 감사 로그와 타임 트래블 디버깅이 자연스럽게 지원됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:MID:REACT_SPRING:FULLSTACK_STACK', 'React에서 Suspense와 Concurrent Features를 활용하는 방법을 설명해주세요.', 'ARCHITECTURE',
 'React 18의 Concurrent 렌더링은 렌더링을 중단하고 더 급한 업데이트를 먼저 처리할 수 있습니다. useTransition()으로 비긴급 상태 업데이트를 표시하고 isPending으로 로딩 인디케이터를 표시합니다. useDeferredValue()는 무거운 컴포넌트의 렌더링을 지연시켜 입력 응답성을 유지하며, Suspense와 조합하면 데이터 로딩 중 fallback을 자연스럽게 표시할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:MID:REACT_SPRING:FULLSTACK_STACK', 'Spring Boot에서 멀티테넌시(Multi-tenancy)를 구현하는 방법을 설명해주세요.', 'ARCHITECTURE',
 '멀티테넌시는 하나의 애플리케이션이 여러 조직(테넌트)의 데이터를 격리해 제공하는 아키텍처입니다. 테넌트 격리 전략으로 DB 분리(테넌트별 DB), 스키마 분리(테넌트별 schema), 행 분리(테넌트 ID 컬럼) 방식이 있습니다. Spring의 AbstractRoutingDataSource로 요청별 테넌트 컨텍스트에 따라 동적으로 DataSource를 전환하며, ThreadLocal로 테넌트 컨텍스트를 전파합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:MID:REACT_SPRING:FULLSTACK_STACK', 'React에서 Code Splitting과 Lazy Loading을 최적화하는 방법을 설명해주세요.', 'DEPLOYMENT',
 'React.lazy()와 Suspense로 컴포넌트 단위 지연 로딩을 구현하고, 라우트 레벨 스플리팅으로 페이지별 청크를 분리합니다. 동적 import()의 webpackPrefetch/webpackPreload 힌트로 사용자가 방문할 가능성이 높은 청크를 브라우저 유휴 시간에 미리 로드합니다. Vite에서는 rollupOptions.output.manualChunks로 vendor 라이브러리를 별도 청크로 분리해 장기 캐싱 효율을 높입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:MID:REACT_SPRING:FULLSTACK_STACK', 'Spring Boot에서 Soft Delete를 구현하는 방법을 설명해주세요.', 'ARCHITECTURE',
 'Soft Delete는 레코드를 물리적으로 삭제하지 않고 deleted_at 컬럼에 삭제 시각을 기록해 논리적으로 삭제하는 패턴입니다. @SQLRestriction("deleted_at IS NULL") 어노테이션으로 JPA 쿼리에 자동 필터를 적용해 삭제된 데이터가 조회되지 않도록 합니다. 감사 추적, 데이터 복구, 참조 무결성 유지가 필요한 도메인에 적합하며, 실제 삭제가 필요한 경우 별도 배치로 처리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:MID:REACT_SPRING:FULLSTACK_STACK', 'React에서 접근성(Accessibility)을 고려한 폼 컴포넌트를 구현하는 방법을 설명해주세요.', 'TESTING',
 'label과 input을 htmlFor/id로 연결하거나 aria-labelledby로 연결해 스크린 리더가 필드를 올바르게 읽을 수 있게 합니다. 에러 메시지는 aria-describedby로 입력 필드와 연결하고 aria-invalid="true"로 유효성 상태를 전달합니다. 폼 제출 중에는 버튼을 disabled하고 aria-busy로 로딩 상태를 알리며, role="alert"로 동적으로 추가되는 에러 메시지를 스크린 리더에 즉시 전달합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:MID:REACT_SPRING:FULLSTACK_STACK', 'Spring Boot에서 Batch 처리를 구현하는 방법을 설명해주세요.', 'ARCHITECTURE',
 'Spring Batch는 Job → Step → (ItemReader → ItemProcessor → ItemWriter) 구조로 대용량 데이터를 청크 단위로 처리합니다. @EnableBatchProcessing으로 활성화하고, chunk(1000)으로 트랜잭션 단위를 설정해 중간 실패 시 해당 청크만 롤백됩니다. JobRepository가 실행 이력을 관리해 재시작 시 마지막 성공 지점부터 재개하는 restart 기능을 지원합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:MID:REACT_SPRING:FULLSTACK_STACK', 'React에서 Virtual Scrolling을 구현하는 방법을 설명해주세요.', 'DEPLOYMENT',
 '@tanstack/react-virtual 또는 react-window를 사용해 수천 개의 리스트 아이템 중 뷰포트에 보이는 항목만 DOM에 렌더링합니다. 각 아이템의 높이가 가변적인 경우 measureElement로 동적 크기를 측정하고, 스크롤 위치에 따라 렌더링 범위를 계산합니다. 가상 스크롤은 DOM 노드 수를 일정하게 유지해 메모리 사용량과 렌더링 비용을 크게 줄이며, 무한 스크롤과 결합해 대용량 데이터를 부드럽게 처리합니다.',
 'MODEL_ANSWER', TRUE, NOW());


-- ============================================================
-- SENIOR (30문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:SENIOR:REACT_SPRING:FULLSTACK_STACK', '마이크로 프론트엔드와 MSA를 연동하는 아키텍처를 설계하는 방법을 설명해주세요.', 'ARCHITECTURE',
 '마이크로 프론트엔드는 Module Federation으로 각 팀의 FE 앱을 독립 배포하고, 앱 셸이 런타임에 원격 모듈을 로드합니다. 각 마이크로 프론트엔드는 해당 도메인의 MSA 서비스와만 통신하며 독립성을 유지합니다. 공통 디자인 시스템, 인증 컨텍스트, 공유 라우팅 상태는 앱 셸에서 관리하고 Custom Events 또는 공유 store로 마이크로 앱 간 통신을 최소화합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:SENIOR:REACT_SPRING:FULLSTACK_STACK', '풀스택 애플리케이션의 성능 최적화 전략을 전체 계층에 걸쳐 설명해주세요.', 'PERFORMANCE',
 '프론트엔드에서는 코드 스플리팅, 이미지 최적화, CDN 활용, React 메모이제이션으로 렌더링 성능을 개선합니다. API 계층에서는 응답 압축(gzip/brotli), HTTP/2 멀티플렉싱, 적절한 Cache-Control 헤더로 네트워크 효율을 높입니다. 백엔드에서는 DB 인덱싱, N+1 해결, Redis 캐싱, Connection Pool 튜닝으로 처리 성능을 극대화합니다. 각 계층의 병목을 APM과 프로파일러로 측정하고 가장 임팩트가 큰 부분부터 최적화합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:SENIOR:REACT_SPRING:FULLSTACK_STACK', '풀스택 E2E 테스트 전략과 테스트 피라미드를 설명해주세요.', 'DEPLOYMENT',
 '테스트 피라미드의 기반은 단위 테스트(Spring JUnit, React Vitest)이며 가장 많고 빠릅니다. 중간 계층은 통합 테스트로 Spring에서 @SpringBootTest + Testcontainers, React에서 MSW + Testing Library입니다. 상단의 E2E 테스트는 Playwright로 핵심 사용자 시나리오만 커버해 유지 비용을 낮춥니다. CI에서 단위/통합은 PR마다, E2E는 main 브랜치 머지 후 실행하는 전략이 효율적입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:SENIOR:REACT_SPRING:FULLSTACK_STACK', 'Spring Boot MSA에서 서비스 간 통신 방식을 비교하고 선택 기준을 설명해주세요.', 'ARCHITECTURE',
 '동기 통신(REST, gRPC)은 즉각적인 응답이 필요한 경우에 사용하며, gRPC는 높은 처리량과 타입 안전성을 제공합니다. 비동기 통신(Kafka, RabbitMQ)은 느슨한 결합, 피크 부하 완충, 작업 실패 시 재처리에 유리합니다. Saga 패턴으로 분산 트랜잭션을 관리하고, Circuit Breaker(Resilience4j)로 장애 전파를 차단합니다. 서비스 메시(Istio, Linkerd)를 도입하면 서비스 간 관찰성과 트래픽 제어를 코드 변경 없이 구현할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:SENIOR:REACT_SPRING:FULLSTACK_STACK', '대규모 트래픽을 처리하기 위한 Spring Boot 스케일링 전략을 설명해주세요.', 'PERFORMANCE',
 '수평 확장(Scale Out)을 위해 애플리케이션을 Stateless하게 설계하고 세션을 Redis에 저장합니다. Spring WebFlux로 리액티브 프로그래밍을 적용하면 적은 스레드로 높은 동시성을 처리할 수 있습니다. DB 병목은 읽기 전용 레플리카로 Read/Write 분리, 샤딩, CQRS 패턴으로 해결합니다. Kubernetes의 HPA(Horizontal Pod Autoscaler)로 트래픽에 따라 인스턴스를 자동 조정합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:SENIOR:REACT_SPRING:FULLSTACK_STACK', 'Zero-downtime 배포 전략을 풀스택 컨텍스트에서 설명해주세요.', 'DEPLOYMENT',
 'Blue-Green 배포는 두 개의 동일 환경을 교차 전환해 롤백이 빠릅니다. Canary 배포는 새 버전을 소수 사용자에게 먼저 적용해 위험을 줄입니다. DB 마이그레이션은 구버전과 신버전 모두 호환되도록 단계별로 진행하며(Expand-Contract 패턴), 프론트엔드 배포는 CDN 캐시 무효화와 청크 파일 버저닝으로 사용자가 구버전 JS를 실행하지 않도록 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:SENIOR:REACT_SPRING:FULLSTACK_STACK', '풀스택 애플리케이션의 보안 취약점과 방어 전략을 설명해주세요.', 'SECURITY',
 'OWASP Top 10 기준으로 SQL Injection은 JPA 파라미터 바인딩, XSS는 React의 자동 이스케이핑과 CSP 헤더, IDOR는 소유권 검증 로직으로 방어합니다. Spring Security에서 모든 엔드포인트를 기본 차단하고 명시적 허용 목록을 관리하는 화이트리스트 전략을 사용합니다. 의존성 취약점은 OWASP Dependency-Check와 Dependabot으로 자동 스캔하고, JWT 시크릿 키 관리는 AWS Secrets Manager 등 전용 서비스를 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:SENIOR:REACT_SPRING:FULLSTACK_STACK', 'CQRS 패턴을 Spring Boot와 React 풀스택에 적용하는 방법을 설명해주세요.', 'ARCHITECTURE',
 'CQRS(Command Query Responsibility Segregation)는 읽기(Query)와 쓰기(Command) 모델을 분리합니다. Spring Boot에서 쓰기는 트랜잭션 DB를, 읽기는 Redis 캐시나 읽기 전용 레플리카에서 처리합니다. 쓰기 후 이벤트를 발행해 읽기 모델을 비동기 업데이트(Eventual Consistency)하며, React에서 useMutation 후 invalidateQueries로 읽기 캐시를 갱신하는 TanStack Query 패턴이 CQRS 개념과 자연스럽게 대응됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:SENIOR:REACT_SPRING:FULLSTACK_STACK', 'API Gateway 패턴을 도입하는 이유와 구현 방법을 설명해주세요.', 'ARCHITECTURE',
 'API Gateway는 인증, 라우팅, 로드밸런싱, Rate Limiting, 로깅을 중앙화해 각 마이크로서비스에서 이를 개별 구현하지 않아도 됩니다. Spring Cloud Gateway로 라우팅 규칙을 정의하고 필터로 공통 처리를 추가합니다. 프론트엔드는 단일 Gateway 엔드포인트만 알면 되므로 백엔드 서비스 토폴로지 변경에 영향받지 않습니다. AWS API Gateway, Kong, Nginx 같은 외부 솔루션도 선택 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:SENIOR:REACT_SPRING:FULLSTACK_STACK', '분산 시스템에서 데이터 일관성을 보장하는 전략을 설명해주세요.', 'ARCHITECTURE',
 'CAP 정리에 따라 분산 시스템은 일관성(Consistency)과 가용성(Availability) 중 하나를 선택해야 합니다. Saga 패턴의 Choreography 방식은 이벤트 기반으로 서비스 간 트랜잭션을 조율하고, Orchestration 방식은 중앙 오케스트레이터가 단계를 관리합니다. Outbox Pattern으로 DB 저장과 메시지 발행의 원자성을 보장하고, Idempotency Key로 중복 처리를 방지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:SENIOR:REACT_SPRING:FULLSTACK_STACK', 'React 앱의 Core Web Vitals 최적화 방법을 설명해주세요.', 'PERFORMANCE',
 'LCP(Largest Contentful Paint)는 주요 이미지 preload, 서버 응답 시간 단축, 리소스 우선순위 힌트로 개선합니다. INP(Interaction to Next Paint)는 긴 JavaScript 태스크를 분할(scheduler.yield())하고 이벤트 핸들러를 경량화합니다. CLS(Cumulative Layout Shift)는 이미지/비디오에 크기를 명시하고 폰트 display:optional 또는 swap으로 레이아웃 이동을 방지합니다. Lighthouse CI와 web-vitals 라이브러리로 실사용 데이터(RUM)를 수집해 지속 모니터링합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:SENIOR:REACT_SPRING:FULLSTACK_STACK', 'Spring Boot의 Circuit Breaker 패턴 구현과 폴백 전략을 설명해주세요.', 'ARCHITECTURE',
 'Resilience4j의 @CircuitBreaker로 외부 서비스 호출을 보호합니다. Closed → Open → Half-Open 상태 전환 임계값(실패율, 슬로우 콜 비율)을 서비스 SLA에 맞게 설정합니다. fallbackMethod를 지정해 Circuit이 Open일 때 캐시된 데이터 반환, 기본값 응답, 격하 서비스 호출 등의 폴백을 구현합니다. Actuator /actuator/circuitbreakers로 상태를 모니터링하고 Prometheus/Grafana로 시각화합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:SENIOR:REACT_SPRING:FULLSTACK_STACK', '풀스택 애플리케이션의 접근 제어(RBAC/ABAC)를 구현하는 방법을 설명해주세요.', 'SECURITY',
 'RBAC(Role-Based Access Control)는 Spring Security의 @PreAuthorize("hasRole(''ADMIN'')")로 역할 기반 접근을 제어합니다. ABAC(Attribute-Based Access Control)는 사용자, 리소스, 환경 속성을 조합한 정책으로 "자신이 생성한 리소스만 수정 가능" 같은 세밀한 제어를 구현합니다. React에서는 인가 상태를 기반으로 UI 요소를 조건부 렌더링하지만, 서버에서의 재검증이 반드시 수행되어야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:SENIOR:REACT_SPRING:FULLSTACK_STACK', '기술 스택 선택 시 고려해야 할 트레이드오프를 풀스택 관점에서 설명해주세요.', 'ARCHITECTURE',
 '프레임워크 선택은 팀의 기존 역량, 커뮤니티 생태계, 장기 유지보수 비용, 성능 요구사항을 종합적으로 고려합니다. Spring Boot는 검증된 안정성과 풍부한 생태계를 제공하지만, 시작 속도와 메모리 사용에서 Quarkus나 Micronaut에 비해 불리합니다. React는 최대 생태계를 가지지만 의사결정 피로(라이브러리 선택)가 높고, Next.js는 풀스택을 단일 프레임워크로 통합합니다. 팀 크기, 도메인 복잡도, 성능 목표를 기준으로 결정하고 그 근거를 ADR로 기록합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:SENIOR:REACT_SPRING:FULLSTACK_STACK', 'Feature Flag를 풀스택에서 구현하고 활용하는 방법을 설명해주세요.', 'DEPLOYMENT',
 'Feature Flag는 코드 배포와 기능 출시를 분리해 트렁크 기반 개발과 점진적 롤아웃을 가능하게 합니다. Spring Boot에서 Unleash, LaunchDarkly, 또는 DB 기반 플래그 테이블로 서버 사이드 제어를 구현합니다. React에서는 API로 플래그를 조회하거나 SSR 시 HTML에 포함해 깜빡임 없이 적용합니다. A/B 테스트, Canary 배포, 긴급 기능 비활성화(kill switch) 등 다양한 용도로 활용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:SENIOR:REACT_SPRING:FULLSTACK_STACK', 'Spring Boot 애플리케이션의 관찰성(Observability) 3가지 요소를 설명해주세요.', 'DEPLOYMENT',
 '관찰성은 로그(Logs), 메트릭(Metrics), 트레이스(Traces) 세 가지 기둥으로 구성됩니다. 로그는 Logback + JSON 구조화 로그로 ELK Stack에 수집하고, 메트릭은 Micrometer + Prometheus로 Grafana에 시각화합니다. 분산 추적은 Micrometer Tracing(Brave/OpenTelemetry)으로 요청 ID를 서비스 전파하고 Zipkin이나 Jaeger에서 트레이스를 확인합니다. OpenTelemetry를 표준으로 채택하면 벤더 종속성 없이 다양한 백엔드를 사용할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:SENIOR:REACT_SPRING:FULLSTACK_STACK', '풀스택 애플리케이션의 인증에서 OAuth2.0과 OIDC를 활용하는 방법을 설명해주세요.', 'SECURITY',
 'OAuth2.0은 권한 위임 프로토콜이고 OIDC는 그 위에 신원 확인 계층을 추가한 표준입니다. Spring Boot에서 spring-security-oauth2-resource-server로 JWT를 검증하거나, spring-boot-starter-oauth2-client로 소셜 로그인을 구현합니다. React에서는 PKCE 플로우를 사용해 SPA에서 안전하게 Authorization Code를 교환하며, 토큰은 HttpOnly 쿠키로 저장해 XSS로부터 보호합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:SENIOR:REACT_SPRING:FULLSTACK_STACK', '풀스택 개발팀의 API 계약(Contract) 관리와 Consumer-Driven Contract Testing을 설명해주세요.', 'DEPLOYMENT',
 'Consumer-Driven Contract Testing은 프론트엔드(소비자)가 API 계약을 정의하고, 백엔드(공급자)가 해당 계약을 충족하는지 자동 검증하는 패턴입니다. Pact 프레임워크로 React에서 Pact 파일을 생성하고 Pact Broker에 게시하면, Spring Boot CI에서 이를 검증합니다. 이를 통해 API 스펙 위반을 배포 전에 발견하고, FE/BE 팀이 독립적으로 개발할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:SENIOR:REACT_SPRING:FULLSTACK_STACK', '풀스택 애플리케이션의 데이터 보호(GDPR/PIPA)를 구현하는 방법을 설명해주세요.', 'SECURITY',
 '개인정보는 수집 목적을 명시하고 최소한의 데이터만 저장하며, 보관 기간이 지나면 자동 삭제하는 정책을 구현합니다. Spring Boot에서 개인정보 필드는 암호화 저장(AES-256)하고, 마스킹된 형태로만 API에서 반환합니다. 데이터 삭제 요청(right to be forgotten) API를 구현하고, 접근 로그를 감사(audit) 테이블에 기록합니다. React에서는 개인정보 수집 동의 UI와 쿠키 동의 배너를 구현합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:SENIOR:REACT_SPRING:FULLSTACK_STACK', '시니어 풀스택 개발자로서 기술 부채를 관리하고 레거시 시스템을 현대화하는 전략을 설명해주세요.', 'ARCHITECTURE',
 '기술 부채를 가시화하기 위해 SonarQube, CodeClimate로 코드 품질을 정량 측정하고 팀에 공유합니다. Strangler Fig 패턴으로 레거시 시스템을 한 번에 교체하지 않고, 새 기능은 새 아키텍처로 구현하며 점진적으로 이전합니다. 테스트가 없는 레거시 코드는 리팩토링 전에 Characterization Test(현재 동작을 기록하는 테스트)를 작성해 안전망을 확보합니다. 기술 부채 작업을 백로그에 포함시켜 비즈니스 기능 개발과 함께 지속적으로 처리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:SENIOR:REACT_SPRING:FULLSTACK_STACK', 'Spring Boot에서 DDD(Domain-Driven Design)를 적용하는 방법을 설명해주세요.', 'ARCHITECTURE',
 'DDD는 비즈니스 도메인을 중심으로 소프트웨어를 설계하며 Bounded Context, Aggregate, Entity, Value Object, Domain Service로 모델링합니다. Aggregate Root는 트랜잭션 경계를 정의하며 외부에서 Aggregate 내부 객체를 직접 접근하지 못하도록 합니다. 도메인 이벤트를 ApplicationEventPublisher로 발행해 Bounded Context 간 결합을 줄이고, Repository는 Aggregate 단위로만 정의합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:SENIOR:REACT_SPRING:FULLSTACK_STACK', 'React 아키텍처에서 Atomic Design 패턴을 적용하는 방법을 설명해주세요.', 'ARCHITECTURE',
 'Atomic Design은 컴포넌트를 Atoms(기본 UI), Molecules(Atom 조합), Organisms(복잡한 UI 섹션), Templates(레이아웃), Pages(실제 데이터가 연결된 페이지) 5계층으로 구성합니다. 하위 계층은 순수 UI만 담당하고 데이터 페칭은 Organisms 이상에서 처리합니다. 단, 계층 구분이 모호해지면 과도한 추상화가 될 수 있으므로 팀 규모와 컴포넌트 재사용 빈도를 고려해 적용 범위를 조정합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:SENIOR:REACT_SPRING:FULLSTACK_STACK', '대규모 Spring Boot 서비스의 DB 성능 튜닝 전략을 설명해주세요.', 'PERFORMANCE',
 'EXPLAIN ANALYZE로 슬로우 쿼리를 분석하고, 복합 인덱스를 쿼리 패턴에 맞게 설계합니다. JPA의 1차/2차 캐시를 이해하고 @QueryHints로 읽기 전용 쿼리에 읽기 전용 Hint를 적용해 스냅샷 저장 오버헤드를 제거합니다. 대용량 데이터 삽입은 JDBC batch insert(spring.jpa.properties.hibernate.jdbc.batch_size)로 처리하고, 읽기 집약 서비스는 MySQL 읽기 레플리카로 Read/Write를 분리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:SENIOR:REACT_SPRING:FULLSTACK_STACK', 'React와 Spring Boot를 사용한 실시간 협업 기능 구현 아키텍처를 설명해주세요.', 'ARCHITECTURE',
 '실시간 협업은 WebSocket(STOMP) 또는 WebRTC를 기반으로 구현하며, 동시 편집 충돌은 OT(Operational Transformation) 또는 CRDT 알고리즘으로 해결합니다. Spring Boot에서 Redis Pub/Sub를 통해 다중 서버 인스턴스 간 메시지를 브로드캐스트하고, 현재 접속자 관리는 Redis의 TTL 키로 처리합니다. React에서는 Yjs 같은 CRDT 라이브러리와 y-websocket Provider를 연결해 선언적으로 협업 상태를 관리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:SENIOR:REACT_SPRING:FULLSTACK_STACK', 'Spring Boot 서비스의 장애 대응(Incident Response) 프로세스를 설명해주세요.', 'DEPLOYMENT',
 '장애 감지는 Grafana 알람 또는 PagerDuty로 자동화하고, Runbook에 따라 즉각 대응 절차를 실행합니다. 즉각 완화(mitigation)로 트래픽 차단, 이전 버전 롤백, Feature Flag 비활성화를 수행하고 근본 원인 분석은 분산 트레이스와 로그로 진행합니다. 장애 후 Post-mortem에서 타임라인, 영향 범위, 근본 원인, 재발 방지 액션 아이템을 문서화하고 Blame-free 문화를 유지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:SENIOR:REACT_SPRING:FULLSTACK_STACK', '풀스택 애플리케이션의 멀티 리전 배포 전략을 설명해주세요.', 'DEPLOYMENT',
 '멀티 리전 배포는 지연 시간 최소화와 재해 복구를 위해 사용자와 가까운 리전에서 서비스를 제공합니다. Active-Active 구성은 모든 리전이 트래픽을 처리하며, DB 동기화에 Global Database(Aurora Global) 또는 양방향 레플리케이션이 필요합니다. Active-Passive는 장애 시 Failover만 담당해 구성이 단순합니다. React 정적 파일은 CloudFront 같은 글로벌 CDN으로 배포하고, Route53 지리적 라우팅으로 사용자를 가장 가까운 리전으로 유도합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:SENIOR:REACT_SPRING:FULLSTACK_STACK', 'Spring Boot에서 멱등성(Idempotency)을 보장하는 API 설계 방법을 설명해주세요.', 'SECURITY',
 '멱등성은 동일한 요청을 여러 번 실행해도 결과가 동일한 성질입니다. 클라이언트가 Idempotency-Key 헤더로 고유 키를 전송하면 서버가 Redis에 처리 결과를 캐싱해 동일 키의 중복 요청을 첫 응답으로 반환합니다. 결제, 주문 생성 같은 부수 효과가 큰 API에서 네트워크 재시도로 인한 중복 처리를 방지하며, 키의 TTL을 충분히 길게 설정해 재시도 윈도우를 커버합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:SENIOR:REACT_SPRING:FULLSTACK_STACK', '풀스택 애플리케이션의 성능 테스트와 부하 테스트 방법을 설명해주세요.', 'DEPLOYMENT',
 'k6 또는 Gatling으로 시나리오 기반 부하 테스트를 작성하고, 목표 TPS와 응답 시간 SLA를 기준으로 임계값을 설정합니다. 부하 테스트는 Smoke(최소 부하), Load(예상 부하), Stress(한계 부하), Spike(급격한 증가) 네 단계로 구분해 실행합니다. Spring Boot Actuator의 /actuator/metrics와 JVM 프로파일러(async-profiler)로 부하 중 병목 지점을 식별하고, DB 슬로우 쿼리 로그와 연계해 전체 스택 성능 프로파일을 완성합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:SENIOR:REACT_SPRING:FULLSTACK_STACK', 'React와 Spring 기반 서비스에서 A/B 테스트를 구현하는 방법을 설명해주세요.', 'DEPLOYMENT',
 'A/B 테스트는 Feature Flag 인프라 위에 사용자를 실험 그룹에 할당하는 로직을 추가해 구현합니다. Spring Boot에서 사용자 ID 기반 해시로 결정론적 그룹 배정을 수행하고 실험 컨텍스트를 JWT 또는 응답 헤더로 전달합니다. React에서는 실험 컨텍스트를 받아 해당 변형(variant)의 UI를 렌더링하며, 전환율과 사용 지표는 분석 서비스로 전송합니다. 실험 결과의 통계적 유의성을 확인한 후 승패를 결정하고 Feature Flag를 정리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('FULLSTACK:SENIOR:REACT_SPRING:FULLSTACK_STACK', '시니어 풀스택 개발자로서 팀의 기술적 의사결정 프로세스를 어떻게 이끄나요?', 'ARCHITECTURE',
 'RFC(Request for Comments) 문서로 중요한 기술 결정을 제안하고 팀 리뷰를 거쳐 ADR(Architecture Decision Record)로 결정을 기록합니다. 기술 선택은 팀의 현재 역량, 장기 유지보수 비용, 커뮤니티 생태계, 비즈니스 요구사항을 균형 있게 고려합니다. 불필요한 복잡성을 경계하고 "지금 당장 필요한가?"를 기준으로 결정하며, 결정 근거를 문서화해 미래 팀원이 맥락을 이해하고 재검토할 수 있게 합니다.',
 'MODEL_ANSWER', TRUE, NOW());
