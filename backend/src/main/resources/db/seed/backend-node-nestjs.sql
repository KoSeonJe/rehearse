-- backend-node-nestjs.sql
-- Node.js/NestJS 백엔드 면접 질문 Pool 시딩
-- 총 90문항: JUNIOR 30 / MID 30 / SENIOR 30
-- cache_key: BACKEND:{Level}:NODE_NESTJS:LANGUAGE_FRAMEWORK

-- ============================================================
-- JUNIOR (30문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'Node.js 이벤트 루프(Event Loop)란 무엇인가요?', '노드제이에스 이벤트 루프(이벤트 루프)란 무엇인가요?', 'NODEJS_CORE',
 'Node.js의 이벤트 루프는 싱글 스레드에서 비동기 I/O를 처리하기 위한 핵심 메커니즘입니다. JavaScript는 싱글 스레드로 동작하지만, 이벤트 루프가 완료된 비동기 콜백을 큐에서 꺼내 실행함으로써 논블로킹 처리를 구현합니다. 파일 읽기, 네트워크 요청 등 I/O 작업은 libuv의 스레드 풀로 위임하고, 완료 시 이벤트 루프가 콜백을 실행합니다. 이 구조 덕분에 Node.js는 적은 리소스로 다수의 동시 연결을 처리할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', '이벤트 루프의 6가지 페이즈를 설명해주세요.', '이벤트 루프의 6가지 페이즈를 설명해주세요.', 'NODEJS_CORE',
 '이벤트 루프는 timers(setTimeout/setInterval 콜백), pending callbacks(I/O 오류 콜백), idle/prepare(내부용), poll(I/O 이벤트 대기/처리), check(setImmediate 콜백), close callbacks(소켓 close 이벤트) 순서로 순환합니다. 각 페이즈에는 큐가 있어 해당 페이즈의 모든 콜백을 소진한 후 다음 페이즈로 이동합니다. poll 페이즈는 I/O 이벤트를 대기하는 핵심 단계로, 큐가 비어 있으면 setImmediate가 있으면 check 페이즈로, timer가 있으면 timers 페이즈로 이동합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', '콜스택(Call Stack)과 콜백 큐(Callback Queue)의 차이는 무엇인가요?', '콜스택(콜 스택)과 콜백 큐(콜백 큐)의 차이는 무엇인가요?', 'NODEJS_CORE',
 '콜스택은 현재 실행 중인 함수 호출을 LIFO(Last In First Out) 순서로 추적하는 자료구조로, 동기 코드 실행에 사용됩니다. 콜백 큐(Task Queue)는 비동기 작업이 완료된 후 실행을 기다리는 콜백 함수를 저장하는 FIFO 큐입니다. 이벤트 루프는 콜스택이 비어 있을 때 콜백 큐에서 콜백을 꺼내 콜스택에 넣어 실행합니다. Microtask 큐(Promise, queueMicrotask)는 Task 큐보다 우선순위가 높아 콜스택이 비워질 때마다 먼저 처리됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'Promise와 Callback의 차이점은 무엇인가요?', '프로미스와 콜백의 차이점은 무엇인가요?', 'ASYNC',
 'Callback은 비동기 작업 완료 시 호출할 함수를 인자로 전달하는 방식으로, 중첩이 깊어지면 Callback Hell(콜백 지옥)이 발생합니다. Promise는 비동기 작업의 최종 완료 또는 실패를 나타내는 객체로, .then()과 .catch()를 체인하여 가독성 높은 비동기 흐름을 구성합니다. Promise는 Pending, Fulfilled, Rejected 세 상태를 가지며 한 번 상태가 결정되면 변경되지 않습니다. async/await는 Promise를 기반으로 동기 코드처럼 읽히는 비동기 코드를 작성할 수 있게 해주는 문법적 설탕입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'async/await를 사용할 때의 에러 처리 방법은?', '에이싱크/어웨이트를 사용할 때의 에러 처리 방법은?', 'ASYNC',
 'async/await에서 에러 처리는 try/catch 블록으로 수행하며, await 표현식에서 발생한 rejected Promise를 catch로 잡을 수 있습니다. 여러 await 중 어느 것에서 에러가 발생했는지 구분하려면 각각 try/catch로 감싸거나, 에러 객체의 타입으로 분기합니다. async 함수에서 처리되지 않은 에러는 rejected Promise로 반환되므로, 호출자에서 catch나 try/catch로 처리해야 합니다. process.on(''unhandledRejection'')으로 처리되지 않은 Promise 거부를 전역으로 감지하고 로깅할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS의 모듈(Module), 컨트롤러(Controller), 서비스(Service) 구조를 설명해주세요.', '네스트제이에스의 모듈(Module), 컨트롤러(Controller), 서비스(Service) 구조를 설명해주세요.', 'NESTJS',
 'NestJS는 Angular에서 영감을 받은 모듈 기반 아키텍처를 사용합니다. Module은 관련 컴포넌트를 캡슐화하는 단위로 @Module 데코레이터로 imports, controllers, providers, exports를 선언합니다. Controller는 HTTP 요청을 라우팅하고 요청/응답을 처리하며, 비즈니스 로직은 Service에 위임합니다. Service는 @Injectable 데코레이터로 DI 컨테이너에 등록되어 비즈니스 로직과 데이터 접근을 담당합니다. 이 3계층 구조가 관심사 분리와 테스트 용이성을 보장합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS의 의존성 주입(Dependency Injection)은 어떻게 동작하나요?', '네스트제이에스의 의존성 주입(Dependency Injection)은 어떻게 동작하나요?', 'NESTJS',
 'NestJS의 DI는 TypeScript 타입 정보와 reflect-metadata를 사용하여 생성자 파라미터의 타입을 토큰으로 Provider를 조회합니다. @Injectable()로 Provider를 선언하고 Module의 providers에 등록하면 NestJS IoC 컨테이너가 인스턴스를 생성하고 의존성을 주입합니다. 기본적으로 싱글톤 스코프로 모듈당 하나의 인스턴스가 유지됩니다. @Inject(TOKEN) 데코레이터로 토큰 기반 주입도 가능하며, Custom Provider(useValue, useFactory, useClass)로 다양한 주입 방식을 지원합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'Node.js에서 CommonJS(require)와 ES Modules(import)의 차이점은?', '노드제이에스에서 CommonJS(require)와 ES Modules(import)의 차이점은?', 'NODEJS_CORE',
 'CommonJS는 Node.js 기본 모듈 시스템으로 require()로 동기적으로 모듈을 로드하고 module.exports로 내보냅니다. ES Modules(ESM)은 JavaScript 표준 모듈 시스템으로 import/export 구문을 사용하며 정적 분석이 가능해 트리쉐이킹에 유리합니다. ESM은 비동기 로딩, 순환 의존성 처리 방식, 모듈 스코프가 다르며 Node.js 12+에서 .mjs 확장자나 package.json의 "type":"module"로 사용 가능합니다. NestJS는 TypeScript를 사용하므로 컴파일 후 CJS 또는 ESM 출력을 선택할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS에서 데코레이터(Decorator)란 무엇이며 어떤 종류가 있나요?', '네스트제이에스에서 데코레이터(Decorator)란 무엇이며 어떤 종류가 있나요?', 'NESTJS',
 'NestJS 데코레이터는 TypeScript 데코레이터를 활용하여 클래스, 메서드, 파라미터에 메타데이터를 부착하고 동작을 변경하는 어노테이션입니다. 클래스 데코레이터(@Module, @Controller, @Injectable, @Guard)는 클래스의 역할을 선언합니다. 메서드 데코레이터(@Get, @Post, @UseGuards, @UseInterceptors)는 라우팅과 미들웨어를 설정합니다. 파라미터 데코레이터(@Param, @Body, @Query, @Req, @Res)는 요청 데이터를 추출합니다. 커스텀 데코레이터는 createParamDecorator()로 생성합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS에서 DTO(Data Transfer Object)와 Validation Pipe를 어떻게 사용하나요?', '네스트제이에스에서 디티오(데이터 트랜스퍼 오브젝트)와 벨리데이션 파이프를 어떻게 사용하나요?', 'NESTJS',
 'DTO는 class-validator와 class-transformer 데코레이터(@IsString, @IsEmail, @IsNotEmpty 등)로 검증 규칙을 선언한 TypeScript 클래스입니다. ValidationPipe를 전역으로 app.useGlobalPipes(new ValidationPipe())에 등록하면, @Body() DTO 파라미터에서 자동으로 검증을 수행하고 실패 시 400 Bad Request를 반환합니다. whitelist: true 옵션으로 DTO에 없는 프로퍼티를 자동 제거하고, transform: true로 plain 객체를 DTO 클래스 인스턴스로 변환합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'Node.js의 npm과 패키지 관리에 대해 설명해주세요.', '노드제이에스의 npm과 패키지 관리에 대해 설명해주세요.', 'NODEJS_CORE',
 'npm(Node Package Manager)은 Node.js의 기본 패키지 관리자로 package.json으로 의존성을 관리합니다. dependencies는 런타임 필요 패키지, devDependencies는 개발/빌드 시에만 필요한 패키지를 구분합니다. package-lock.json은 설치된 모든 패키지의 정확한 버전을 잠가 환경 간 일관성을 보장하므로 버전 관리에 포함해야 합니다. npm ci는 lock 파일을 기준으로 정확히 설치하므로 CI/CD 환경에서 npm install 대신 권장됩니다. yarn이나 pnpm은 성능과 디스크 효율성이 개선된 대안입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS에서 환경 변수(Environment Variables)를 어떻게 관리하나요?', '네스트제이에스에서 환경 변수(엔바이런먼트 벨리에이블즈)를 어떻게 관리하나요?', 'NESTJS',
 '@nestjs/config의 ConfigModule.forRoot()로 .env 파일을 로드하고 ConfigService를 DI로 주입받아 process.env 대신 configService.get()으로 환경변수에 접근합니다. Joi나 zod로 환경변수 스키마 유효성 검증을 설정하면 필수 환경변수 누락 시 애플리케이션 시작을 실패시켜 조기에 문제를 발견합니다. 환경별 .env.development, .env.production 파일을 분리하고, 민감 정보는 AWS Secrets Manager나 환경 주입 방식을 사용합니다. .env 파일은 .gitignore에 반드시 추가해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'TypeScript의 인터페이스(Interface)와 타입 별칭(Type Alias)의 차이점은?', '타입스크립트의 인터페이스(인터페이스)와 타입 별칭(타입 앨리어스)의 차이점은?', 'NODEJS_CORE',
 '인터페이스는 객체 형태를 선언하며 선언 병합(declaration merging)이 가능하여 같은 이름으로 여러 번 선언해 확장할 수 있습니다. 타입 별칭은 객체뿐 아니라 유니온, 인터섹션, 튜플, 프리미티브 등 모든 타입에 이름을 부여할 수 있습니다. extends로 인터페이스를 확장할 수 있고, 타입 별칭은 & 인터섹션으로 구성합니다. 클래스에서 구현(implements)은 인터페이스와 타입 별칭 모두 가능합니다. 객체 구조에는 인터페이스를, 유니온/인터섹션 같은 복합 타입에는 타입 별칭을 사용하는 것이 일반적입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS에서 예외 처리는 어떻게 하나요?', '네스트제이에스에서 예외 처리는 어떻게 하나요?', 'NESTJS',
 'NestJS는 HttpException과 이를 상속하는 NotFoundException, BadRequestException, UnauthorizedException 등의 내장 예외 클래스를 제공합니다. 서비스나 컨트롤러에서 throw new NotFoundException(''User not found'')처럼 예외를 던지면 NestJS의 내장 글로벌 예외 필터가 적절한 HTTP 응답으로 변환합니다. @Catch() 데코레이터와 ExceptionFilter 인터페이스를 구현하여 커스텀 예외 필터를 만들고, @UseFilters() 또는 app.useGlobalFilters()로 등록하면 일관된 에러 응답 포맷을 유지할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS에서 TypeORM을 사용한 기본 CRUD 구현 방법은?', '네스트제이에스에서 타입오알엠을 사용한 기본 CRUD 구현 방법은?', 'NESTJS',
 '@nestjs/typeorm의 TypeOrmModule.forFeature([Entity])로 모듈에 엔티티를 등록하고, @InjectRepository(Entity)로 Repository를 주입받습니다. repository.find(), findOneBy(), save(), delete()로 기본 CRUD를 구현합니다. @Entity(), @Column(), @PrimaryGeneratedColumn() 데코레이터로 엔티티 클래스를 정의하고, 관계는 @ManyToOne, @OneToMany, @OneToOne, @ManyToMany로 선언합니다. TypeORM의 QueryBuilder로 복잡한 쿼리를 구성하거나 @Query()로 raw SQL을 실행합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'Node.js에서 require 캐싱은 어떻게 동작하나요?', '노드제이에스에서 require 캐싱은 어떻게 동작하나요?', 'NODEJS_CORE',
 'Node.js는 require()로 로드된 모듈을 require.cache 객체에 캐싱합니다. 같은 경로의 모듈을 여러 번 require()해도 처음 한 번만 실행되고 이후에는 캐시된 exports 객체를 반환합니다. 이 때문에 모듈은 상태를 가진 싱글톤처럼 동작하며, 모듈 수준 변수는 애플리케이션 생명주기 동안 유지됩니다. delete require.cache[require.resolve(''./module'')]로 캐시를 강제 초기화할 수 있으며, 주로 테스트 환경에서 모듈 재로드가 필요할 때 활용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS에서 CORS 설정 방법을 설명해주세요.', '네스트제이에스에서 코어스 설정 방법을 설명해주세요.', 'NESTJS',
 'NestJS에서는 app.enableCors() 메서드로 CORS를 활성화합니다. origin, methods, allowedHeaders, credentials 등의 옵션을 객체로 전달하여 세부 설정을 구성합니다. 운영 환경에서는 origin을 특정 프론트엔드 도메인 배열이나 함수로 지정하여 허용 출처를 제한합니다. 인증 쿠키나 Authorization 헤더를 사용하는 경우 credentials: true를 설정하고 origin에 와일드카드(*)를 사용하면 브라우저가 거부하므로 구체적인 도메인을 명시해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS의 Pipe란 무엇이며 어떤 역할을 하나요?', '네스트제이에스의 Pipe란 무엇이며 어떤 역할을 하나요?', 'NESTJS',
 'Pipe는 컨트롤러 라우트 핸들러 실행 전에 입력 데이터를 변환하거나 검증하는 미들웨어입니다. ParseIntPipe, ParseUUIDPipe 같은 내장 파이프로 경로 파라미터를 자동 변환하고, ValidationPipe로 DTO 검증을 수행합니다. PipeTransform 인터페이스를 구현하여 커스텀 파이프를 작성할 수 있으며, @UsePipes() 데코레이터로 컨트롤러, 메서드, 파라미터 레벨에 적용하거나 app.useGlobalPipes()로 전역 적용합니다. 변환과 검증을 한 레이어에서 처리하여 컨트롤러 코드를 간결하게 유지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'Node.js에서 Buffer와 Stream의 개념을 설명해주세요.', '노드제이에스에서 버퍼와 스트림의 개념을 설명해주세요.', 'NODEJS_CORE',
 'Buffer는 바이너리 데이터를 처리하기 위한 고정 크기 메모리 구조로, 파일 I/O나 네트워크 데이터를 원시 바이트로 처리할 때 사용합니다. Stream은 데이터를 청크(chunk) 단위로 순차적으로 처리하는 인터페이스로, Readable, Writable, Duplex, Transform 네 가지 타입이 있습니다. 대용량 파일 처리 시 전체를 메모리에 올리는 대신 Stream으로 처리하면 메모리 사용량을 일정하게 유지할 수 있습니다. pipe() 메서드로 Readable 스트림을 Writable 스트림에 연결하여 파이프라인을 구성합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS에서 Guard란 무엇이며 어떻게 구현하나요?', '네스트제이에스에서 가드란 무엇이며 어떻게 구현하나요?', 'NESTJS',
 'Guard는 요청이 라우트 핸들러에 도달할지 여부를 결정하는 컴포넌트로, canActivate() 메서드가 true를 반환하면 허용, false이면 403 Forbidden을 반환합니다. CanActivate 인터페이스를 구현하고 @Injectable()을 선언하여 Guard를 작성합니다. @UseGuards(JwtAuthGuard)로 컨트롤러나 메서드에 적용하거나, app.useGlobalGuards()로 전역 적용합니다. ExecutionContext를 통해 HTTP, WebSocket, RPC 등 다양한 컨텍스트의 요청 정보에 접근할 수 있습니다. JWT 인증 검증이 Guard의 가장 일반적인 활용 사례입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'JavaScript의 클로저(Closure)란 무엇인가요?', '자바스크립트의 클로저(클로저)란 무엇인가요?', 'NODEJS_CORE',
 '클로저는 함수가 자신이 선언된 렉시컬 스코프의 변수를 기억하고 접근할 수 있는 특성입니다. 내부 함수가 외부 함수의 변수를 참조하면, 외부 함수 실행이 끝난 후에도 해당 변수가 가비지 컬렉션되지 않고 클로저를 통해 접근 가능합니다. 데이터 은닉(private 변수 패턴), 팩토리 함수, 이벤트 핸들러에서 상태 유지 등에 활용됩니다. 단, 클로저가 대용량 객체를 참조하면 메모리 누수로 이어질 수 있으므로 주의해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS에서 Interceptor의 역할과 사용법을 설명해주세요.', '네스트제이에스에서 인터셉터의 역할과 사용법을 설명해주세요.', 'NESTJS',
 'Interceptor는 AOP 방식으로 요청/응답 파이프라인에 로직을 삽입하는 컴포넌트로, NestInterceptor 인터페이스의 intercept() 메서드를 구현합니다. 요청 전후 처리(로깅, 실행시간 측정), 응답 변환(통일 응답 포맷 래핑), 예외 매핑, 캐싱 등에 활용합니다. intercept()에서 next.handle()은 Observable을 반환하며, RxJS 연산자(map, catchError, tap 등)로 요청/응답을 가공합니다. @UseInterceptors(TransformInterceptor)로 컨트롤러나 메서드에 적용하거나 전역 적용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'Node.js의 process 객체 주요 프로퍼티와 메서드는?', '노드제이에스의 process 객체 주요 프로퍼티와 메서드는?', 'NODEJS_CORE',
 'process.env는 환경변수 객체로 설정값과 시크릿에 접근합니다. process.argv는 커맨드라인 인수 배열로 [node, script, ...args] 형태입니다. process.exit(code)는 프로세스를 종료하며 0은 정상, 비제로는 오류를 나타냅니다. process.on(''uncaughtException'')과 process.on(''unhandledRejection'')으로 처리되지 않은 오류를 전역에서 감지합니다. process.nextTick(callback)은 현재 이터레이션의 마지막에 콜백을 실행하며 I/O 이벤트보다 우선순위가 높습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS에서 Swagger를 이용한 API 문서화 방법은?', '네스트제이에스에서 스웨거를 이용한 에이피아이 문서화 방법은?', 'NESTJS',
 '@nestjs/swagger의 SwaggerModule.createDocument()와 SwaggerModule.setup()으로 Swagger UI를 설정합니다. @ApiTags(), @ApiOperation(), @ApiResponse(), @ApiProperty() 데코레이터로 컨트롤러와 DTO에 API 메타데이터를 선언하면 자동으로 OpenAPI 스펙이 생성됩니다. @ApiProperty({ example: ''test@test.com'' })으로 예시값을 추가하고, @ApiBearerAuth()로 JWT 인증 헤더를 문서화합니다. CLI 플러그인(nest-cli.json에 @nestjs/swagger/plugin 추가)을 사용하면 TypeScript 타입으로부터 자동으로 문서를 생성합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'JavaScript의 프로토타입(Prototype) 기반 상속을 설명해주세요.', '자바스크립트의 프로토타입(프로토타입) 기반 상속을 설명해주세요.', 'NODEJS_CORE',
 'JavaScript는 클래스 기반이 아닌 프로토타입 기반 언어로, 모든 객체는 [[Prototype]] 내부 슬롯으로 다른 객체(프로토타입)를 참조합니다. 속성/메서드 조회 시 객체 자신에 없으면 프로토타입 체인을 따라 Object.prototype까지 올라가 검색합니다. Object.create(proto)로 프로토타입을 지정한 객체를 생성하거나, ES6 class 문법으로 프로토타입 상속을 더 직관적으로 표현합니다. class의 extends와 super도 내부적으로 프로토타입 체인을 설정합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS에서 미들웨어(Middleware)를 구현하는 방법은?', '네스트제이에스에서 미들웨어(미들웨어)를 구현하는 방법은?', 'NESTJS',
 'NestJS 미들웨어는 Express/Fastify 미들웨어와 유사하게 요청/응답 사이클의 앞단에서 실행됩니다. NestMiddleware 인터페이스를 구현하거나 함수형으로 작성하며, @Module의 configure() 메서드에서 MiddlewareConsumer로 라우트에 적용합니다. forRoutes()로 특정 경로나 컨트롤러에만 적용하거나, forRoutes(''*'')로 모든 라우트에 적용합니다. 요청 로깅, IP 필터링, 바디 파싱 등의 전처리에 활용되며, Guard나 Interceptor보다 이른 시점에 실행됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'Node.js에서 비동기 파일 읽기를 구현하는 방법은?', '노드제이에스에서 비동기 파일 읽기를 구현하는 방법은?', 'NODEJS_CORE',
 'fs 모듈의 readFile()은 콜백 방식, promises.readFile()은 Promise 기반으로 파일을 비동기 읽기합니다. async/await와 fs.promises.readFile(path, ''utf8'')을 사용하면 동기 코드처럼 읽기 쉬운 비동기 파일 읽기를 구현합니다. 대용량 파일은 fs.createReadStream()으로 스트림 기반 처리를 사용하면 메모리 효율적입니다. 파일 경로는 __dirname 또는 path.join()을 사용하여 플랫폼 독립적으로 구성하고, 오류 처리(파일 없음, 권한 없음)를 반드시 포함해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS 프로젝트의 기본 폴더 구조는 어떻게 구성하나요?', '네스트제이에스 프로젝트의 기본 폴더 구조는 어떻게 구성하나요?', 'NESTJS',
 'NestJS CLI(nest new)로 생성된 기본 구조는 src/ 하위에 app.module.ts(루트 모듈), main.ts(진입점)가 있습니다. 도메인별로 users/, auth/, products/ 등의 디렉토리를 만들고 각각 module, controller, service, entity, dto 파일을 포함시킵니다. common/ 또는 shared/ 디렉토리에 공통 데코레이터, 필터, 인터셉터, 미들웨어, 파이프를 모아둡니다. 각 도메인 모듈을 AppModule에 import하는 구조로 확장성 있는 아키텍처를 구성합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'JavaScript의 이벤트 위임(Event Delegation) 패턴이란?', '자바스크립트의 이벤트 위임(Event Delegation) 패턴이란?', 'NODEJS_CORE',
 '이벤트 위임은 이벤트 버블링을 활용하여 부모 요소 하나에 이벤트 리스너를 등록하고, 이벤트가 발생한 자식 요소를 event.target으로 식별하여 처리하는 패턴입니다. 동적으로 추가/제거되는 자식 요소에 개별 리스너를 붙이는 대신 부모 하나에 등록하므로 메모리 효율이 높고 관리가 용이합니다. Node.js의 EventEmitter도 유사하게 이벤트 이름 기반으로 리스너를 등록하고 emit()으로 이벤트를 발생시키는 패턴을 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- ============================================================
-- MID (30문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'setImmediate와 setTimeout(0)의 실행 순서 차이를 설명해주세요.', '셋이미디에이트와 셋타임아웃(0)의 실행 순서 차이를 설명해주세요.', 'NODEJS_CORE',
 'setTimeout(fn, 0)은 이벤트 루프의 timers 페이즈에서 실행되고, setImmediate는 check 페이즈에서 실행됩니다. 메인 모듈(비 I/O 컨텍스트)에서는 타이머 정밀도에 따라 순서가 비결정적입니다. I/O 콜백 내부에서는 항상 setImmediate가 setTimeout(fn, 0)보다 먼저 실행됩니다. process.nextTick은 이벤트 루프 페이즈와 무관하게 현재 작업 완료 직후 마이크로태스크 큐에서 실행되어 가장 빠릅니다. 실행 순서 이해는 콜백 스케줄링과 재귀 호출 시 스택 오버플로우 방지에 중요합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'Promise.all, Promise.race, Promise.allSettled, Promise.any의 차이점은?', '프로미스 올, 프로미스 레이스, 프로미스 올Settled, 프로미스 에니의 차이점은?', 'ASYNC',
 'Promise.all은 모든 Promise가 fulfilled되어야 결과 배열을 반환하며, 하나라도 rejected되면 즉시 reject됩니다. Promise.race는 가장 먼저 settled된(fulfilled 또는 rejected) Promise의 결과를 반환합니다. Promise.allSettled(ES2020)는 모든 Promise가 settled될 때까지 기다려 각각의 {status, value/reason} 객체 배열을 반환하여 일부 실패해도 나머지 결과를 얻을 수 있습니다. Promise.any(ES2021)는 하나라도 fulfilled되면 반환하고, 모두 rejected되면 AggregateError를 던집니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS의 Middleware, Guard, Interceptor, Pipe, Filter의 실행 순서와 각 역할 차이는?', '네스트제이에스의 미들웨어, 가드, 인터셉터, 파이프, 필터의 실행 순서와 각 역할 차이는?', 'NESTJS',
 '실행 순서는 Middleware → Guard → Interceptor(pre) → Pipe → Controller Handler → Interceptor(post) → Exception Filter(오류 시) 입니다. Middleware는 Express 수준의 전처리(로깅, 바디 파싱), Guard는 인증/인가 결정, Interceptor는 요청/응답 변환(로깅, 응답 포맷), Pipe는 입력 데이터 변환/검증, ExceptionFilter는 예외를 HTTP 응답으로 변환하는 역할입니다. 각 레이어가 명확한 단일 책임을 가지며, 적절한 레이어에 로직을 배치하면 재사용성이 높아집니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'Node.js Worker Thread와 Child Process의 차이점은?', '노드제이에스 워커 스레드와 차일드 프로세스의 차이점은?', 'NODEJS_CORE',
 'Child Process는 완전히 별도의 Node.js 프로세스를 생성하여 메모리를 완전히 분리하고, IPC 채널이나 stdio로 통신합니다. Worker Thread(Node.js 10.5+)는 동일 프로세스 내 별도 스레드에서 JavaScript를 실행하며, SharedArrayBuffer로 메모리를 공유할 수 있어 스레드 간 데이터 전달이 빠릅니다. Worker Thread는 CPU 집약적 작업(이미지 처리, 암호화)을 메인 이벤트 루프 차단 없이 처리하는 데 적합합니다. 두 방법 모두 Node.js의 GIL 없는 병렬 처리를 가능하게 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS에서 JWT 인증을 Passport와 함께 구현하는 방법은?', '네스트제이에스에서 제이더블유티 인증을 파스포트와 함께 구현하는 방법은?', 'NESTJS',
 '@nestjs/passport와 passport-jwt를 사용하여 JWT 전략을 구현합니다. PassportStrategy(Strategy)를 상속한 JwtStrategy 클래스에서 validate() 메서드로 토큰 페이로드를 검증하고 사용자를 반환합니다. AuthGuard(''jwt'')를 상속한 JwtAuthGuard를 @UseGuards()에 적용하여 보호된 라우트를 설정합니다. @nestjs/jwt의 JwtService로 토큰을 생성하고, AuthModule에서 JwtModule.registerAsync()로 시크릿과 만료 시간을 설정합니다. 리프레시 토큰 전략은 별도 PassportStrategy로 구현합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS에서 마이크로서비스 통신 패턴(Message Pattern, Event Pattern)을 설명해주세요.', '네스트제이에스에서 마이크로서비스 통신 패턴(메시지 패턴, 이벤트 패턴)을 설명해주세요.', 'NESTJS',
 'NestJS 마이크로서비스는 @nestjs/microservices로 TCP, Redis, NATS, Kafka, RabbitMQ 등 다양한 트랜스포트를 지원합니다. @MessagePattern(''cmd'')은 요청-응답(Request-Response) 패턴으로 클라이언트가 응답을 기다립니다. @EventPattern(''event'')은 이벤트 발행-구독 패턴으로 응답이 없는 단방향 통신입니다. ClientProxy(ClientsModule으로 주입)의 send()는 Message Pattern, emit()은 Event Pattern에 사용합니다. Hybrid 앱으로 HTTP와 마이크로서비스를 동시에 운영하는 것이 일반적입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'TypeScript의 제네릭(Generics)을 NestJS에서 활용하는 방법은?', '타입스크립트의 제네릭(제네릭스)을 네스트제이에스에서 활용하는 방법은?', 'NODEJS_CORE',
 '제네릭은 타입을 파라미터처럼 전달하여 타입 안전하면서도 재사용 가능한 코드를 작성합니다. NestJS에서 페이지네이션 응답, 공통 응답 래퍼(ApiResponse<T>)를 제네릭으로 정의하면 엔티티마다 별도 DTO를 만들지 않아도 됩니다. Repository<Entity> 패턴처럼 공통 CRUD 로직을 제네릭 기반 BaseService<T>로 추상화할 수 있습니다. Conditional Type, Mapped Type(Partial<T>, Required<T>, Pick<T,K>), Template Literal Type 등 고급 제네릭 기법으로 타입 유틸리티를 구성합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS에서 캐싱(CacheManager)을 구현하는 방법은?', '네스트제이에스에서 캐싱(캐시매니저)을 구현하는 방법은?', 'NESTJS',
 '@nestjs/cache-manager와 cache-manager를 사용하여 메모리 또는 Redis 기반 캐시를 구현합니다. CacheModule.registerAsync()로 Redis 스토어를 설정하고, @UseInterceptors(CacheInterceptor)를 GET 엔드포인트에 적용하면 응답이 자동 캐싱됩니다. @CacheKey()와 @CacheTTL()로 키와 만료 시간을 커스터마이징합니다. 더 세밀한 제어가 필요할 때는 CACHE_MANAGER 토큰으로 CacheManager를 주입받아 cacheManager.get(), cacheManager.set(), cacheManager.del()을 직접 호출합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'Node.js의 메모리 누수를 디버깅하는 방법은?', '노드제이에스의 메모리 누수를 디버깅하는 방법은?', 'PERFORMANCE',
 '메모리 누수는 해제되지 않는 참조(이벤트 리스너 미제거, 클로저 내 대용량 객체 참조, 캐시 무한 증가)로 발생합니다. --inspect 플래그로 Chrome DevTools에 연결하여 힙 스냅샷을 비교하고, 증가하는 객체 타입을 식별합니다. process.memoryUsage()로 rss, heapUsed, heapTotal을 주기적으로 로깅하여 메모리 추세를 모니터링합니다. clinic.js(clinic heap, clinic doctor)는 Node.js 전용 성능 분석 도구로 메모리 누수와 이벤트 루프 블로킹을 시각화합니다. EventEmitter.setMaxListeners()로 리스너 제한을 설정하면 누수 경고를 받을 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS에서 WebSocket 게이트웨이 구현 방법을 설명해주세요.', '네스트제이에스에서 웹소켓 게이트웨이 구현 방법을 설명해주세요.', 'NESTJS',
 '@WebSocketGateway() 데코레이터로 WebSocket 게이트웨이를 선언하고, @SubscribeMessage(''event'') 데코레이터로 특정 이벤트 핸들러를 등록합니다. OnGatewayInit, OnGatewayConnection, OnGatewayDisconnect 인터페이스로 연결 라이프사이클을 처리합니다. @WebSocketServer()로 Socket.io 서버 인스턴스를 주입받아 브로드캐스트(server.emit())를 수행합니다. Guard, Interceptor, Pipe도 WebSocket 게이트웨이에 적용 가능하며, platform-socket.io 또는 platform-ws 어댑터 중 선택할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS에서 Custom Decorator를 생성하고 활용하는 방법은?', '네스트제이에스에서 커스텀 데코레이터를 생성하고 활용하는 방법은?', 'NESTJS',
 'createParamDecorator(factory)로 컨트롤러 파라미터 데코레이터를 생성하여 요청에서 원하는 데이터를 추출합니다. SetMetadata(key, value)와 Reflector를 조합하면 메서드/클래스에 메타데이터를 부착하고 Guard나 Interceptor에서 읽어 조건부 처리를 구현합니다. 예를 들어 @Roles(''admin'') 데코레이터는 SetMetadata로 역할을 기록하고, RolesGuard에서 Reflector로 메타데이터를 읽어 권한을 검증합니다. 여러 데코레이터를 applyDecorators()로 합성하여 재사용 가능한 복합 데코레이터를 만들 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'Node.js에서 Cluster 모듈을 사용한 멀티 코어 활용 방법은?', '노드제이에스에서 클러스터 모듈을 사용한 멀티 코어 활용 방법은?', 'NODEJS_CORE',
 'Cluster 모듈은 마스터 프로세스가 CPU 코어 수만큼 워커 프로세스를 fork()하여 동일 포트를 공유하며 요청을 분산 처리합니다. 마스터에서 cluster.isMaster로 분기하여 워커를 생성하고, 워커 종료 시 cluster.on(''exit'')에서 새 워커를 재시작합니다. 각 워커는 독립된 이벤트 루프와 메모리를 가지며 PM2 같은 프로세스 매니저가 클러스터 모드를 편리하게 관리합니다. 컨테이너 환경에서는 클러스터 대신 여러 컨테이너 복제본과 로드 밸런서를 사용하는 방식이 더 일반적입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS에서 트랜잭션 처리를 TypeORM과 함께 구현하는 방법은?', '네스트제이에스에서 트랜잭션 처리를 타입오알엠과 함께 구현하는 방법은?', 'NESTJS',
 'TypeORM 트랜잭션은 DataSource.transaction(async manager => {...}) 방식으로 트랜잭션 엔티티 매니저를 제공받아 처리합니다. NestJS에서는 @InjectDataSource()로 DataSource를 주입받아 트랜잭션 블록 내에서 여러 Repository 작업을 수행합니다. QueryRunner를 사용하면 트랜잭션 시작/커밋/롤백을 수동으로 제어하여 복잡한 흐름을 구성할 수 있습니다. 서비스 메서드에 @Transaction() 커스텀 데코레이터를 적용하여 트랜잭션 처리를 분리하거나, cls-hooked로 요청 컨텍스트에 트랜잭션을 전파하는 방식도 활용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS에서 Rate Limiting을 구현하는 방법은?', '네스트제이에스에서 레이트 리미팅을 구현하는 방법은?', 'NESTJS',
 '@nestjs/throttler 패키지로 Rate Limiting을 구현합니다. ThrottlerModule.forRoot([{ttl: 60, limit: 100}])를 AppModule에 등록하고, ThrottlerGuard를 전역 Guard로 적용하면 기본 설정으로 모든 엔드포인트에 Rate Limiting이 적용됩니다. @Throttle()로 특정 엔드포인트의 제한을 오버라이드하고, @SkipThrottle()로 제외합니다. 분산 환경에서는 메모리 스토리지 대신 ThrottlerStorageRedisService로 Redis를 스토리지로 사용하여 인스턴스 간 제한 카운터를 공유해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'JavaScript의 이벤트 버블링(Event Bubbling)과 캡처링(Capturing) 차이는?', '자바스크립트의 이벤트 버블링(이벤트 버블링)과 캡처링(캡처링) 차이는?', 'NODEJS_CORE',
 '이벤트 버블링은 이벤트가 발생한 요소에서 부모 요소로 거슬러 올라가는 전파 방식으로 기본 동작입니다. 이벤트 캡처링은 반대로 최상위 부모에서 이벤트 발생 요소 방향으로 내려오는 전파 방식으로, addEventListener의 세 번째 파라미터를 true로 설정합니다. event.stopPropagation()으로 버블링/캡처링을 중단하고, event.preventDefault()로 기본 동작을 막습니다. Node.js의 EventEmitter는 브라우저와 달리 이벤트 캡처링/버블링이 없으며 emit() 호출 시 해당 이벤트의 모든 리스너를 직접 순서대로 실행합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS에서 File Upload를 구현하는 방법은?', '네스트제이에스에서 파일 업로드를 구현하는 방법은?', 'NESTJS',
 '@nestjs/platform-express의 FileInterceptor와 Multer를 사용하여 파일 업로드를 처리합니다. @UploadedFile() 데코레이터로 업로드된 파일 객체를 받고, MulterOptions으로 허용 파일 타입, 크기 제한을 설정합니다. S3 업로드는 multer-s3로 Multer 스토리지를 S3로 변경하거나, 파일을 메모리에 받은 후 AWS SDK로 S3에 업로드합니다. 파일 확장자와 MIME 타입을 fileFilter로 검증하고, 원본 파일명을 uuid로 교체하여 경로 조작 공격을 방지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'TypeScript의 데코레이터 실행 순서를 설명해주세요.', '타입스크립트의 데코레이터 실행 순서를 설명해주세요.', 'NODEJS_CORE',
 '클래스 내 데코레이터는 파라미터 → 메서드 → 접근자 → 프로퍼티 → 클래스 순서로 평가됩니다. 같은 메서드에 여러 데코레이터가 있으면 선언 순서의 반대(아래에서 위)로 실행됩니다. @A @B method()는 B → A 순서로 실행됩니다. 데코레이터 팩토리(@A(args))의 경우 평가(팩토리 호출)는 위에서 아래, 실행(반환된 함수 적용)은 아래에서 위입니다. NestJS에서 여러 Guard를 @UseGuards(A, B)로 선언하면 A → B 순서로 실행됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS에서 테스트 작성 방법(단위 테스트, e2e 테스트)을 설명해주세요.', '네스트제이에스에서 테스트 작성 방법(단위 테스트, 이투이 테스트)을 설명해주세요.', 'NESTJS',
 '단위 테스트는 Test.createTestingModule()로 테스트용 모듈을 생성하고, 의존성을 jest.fn() 또는 createMock()으로 모킹합니다. moduleRef.get(Service)로 서비스 인스턴스를 가져와 메서드를 테스트합니다. e2e 테스트는 @nestjs/testing의 INestApplication을 생성하고 supertest로 실제 HTTP 요청을 시뮬레이션합니다. 테스트 DB(SQLite 인메모리)를 사용하거나 testcontainers로 실제 MySQL 컨테이너를 테스트에 활용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- ============================================================
-- SENIOR (30문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', '이벤트 루프 블로킹의 원인과 해결 방법을 설명해주세요.', '이벤트 루프 블로킹의 원인과 해결 방법을 설명해주세요.', 'PERFORMANCE',
 '이벤트 루프 블로킹은 CPU 집약적 동기 작업(대용량 JSON 파싱, 암호화, 이미지 처리), 동기 파일 I/O, 긴 동기 루프가 원인입니다. 해결책으로 Worker Thread에서 CPU 집약 작업을 처리하거나, setImmediate/process.nextTick으로 장시간 루프를 청크로 분할합니다. clinic.js의 clinic doctor로 이벤트 루프 지연을 시각화하고 병목을 찾습니다. 마이크로태스크(Promise) 폭풍 현상은 재귀적 Promise 체인이 이벤트 루프 I/O 처리를 지연시킬 수 있으므로 주의합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'Worker Thread와 Cluster를 각각 언제 사용해야 하는지 비교 설명해주세요.', '워커 스레드와 클러스터를 각각 언제 사용해야 하는지 비교 설명해주세요.', 'NODEJS_CORE',
 'Worker Thread는 단일 프로세스 내에서 CPU 집약적 작업(이미지/비디오 처리, 암호화, 머신러닝 추론)을 분리하여 메인 이벤트 루프를 보호할 때 사용합니다. SharedArrayBuffer와 Atomics로 스레드 간 효율적인 데이터 공유가 가능합니다. Cluster는 여러 프로세스로 동일 서버를 복제하여 요청 처리량(RPS)을 CPU 코어 수에 비례하게 수평 확장합니다. 컨테이너 기반 배포에서는 Cluster 대신 수평 Pod 확장(K8s HPA)이 더 선호됩니다. 대규모 서비스에서는 CPU 바운드 작업을 별도 Worker 서비스로 분리하는 것이 아키텍처적으로 더 나은 선택입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS 기반 대규모 서비스의 모듈 아키텍처 설계 원칙을 설명해주세요.', '네스트제이에스 기반 대규모 서비스의 모듈 아키텍처 설계 원칙을 설명해주세요.', 'NESTJS',
 '도메인별 모듈(UserModule, OrderModule)로 분리하고 Core 모듈(인증, 로깅, DB 설정)은 AppModule에서 forRoot로 한 번만 임포트합니다. 공유 유틸리티는 SharedModule(forRoot/forFeature 패턴)로 구성하여 여러 도메인에서 재사용합니다. 각 모듈의 exports를 최소화하여 모듈 간 결합도를 낮추고, 의존성 방향이 단방향이 되도록 설계합니다. Dynamic Module(forRootAsync)으로 비동기 설정 로딩을 지원하고, forFeature 패턴으로 모듈별 필요한 리소스(Repository, 설정 네임스페이스)를 제공합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS의 CQRS 패턴 구현 방법과 이점을 설명해주세요.', '네스트제이에스의 씨큐알에스 패턴 구현 방법과 이점을 설명해주세요.', 'NESTJS',
 '@nestjs/cqrs는 Command, Query, Event 버스를 제공하여 쓰기(Command)와 읽기(Query) 모델을 분리합니다. CommandHandler와 QueryHandler를 @CommandHandler/@QueryHandler로 선언하고, CommandBus/QueryBus를 주입하여 execute합니다. EventBus로 도메인 이벤트를 발행하고 @EventsHandler로 이벤트를 처리합니다. 쓰기 모델과 읽기 모델을 별도 DB나 Aggregate로 분리하면 각각 독립적으로 스케일링 가능하며, 복잡한 도메인 로직의 응집도를 높입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'Node.js 애플리케이션의 성능 프로파일링과 최적화 전략은?', '노드제이에스 애플리케이션의 성능 프로파일링과 최적화 전략은?', 'PERFORMANCE',
 '--prof 플래그로 V8 프로파일러를 활성화하고 node --prof-process로 분석하거나, clinic.js(flame, doctor, heap)로 이벤트 루프 지연, 메모리 누수, CPU 핫스팟을 시각화합니다. 응답 시간 최적화는 쿼리 N+1 제거, Redis 캐싱, 응답 압축(compression), HTTP/2를 적용합니다. Fastify는 Express보다 2~3배 빠른 HTTP 처리량을 제공하므로 NestJS의 HTTP 어댑터를 Fastify로 변경하는 것을 고려합니다. k6나 autocannon으로 부하 테스트를 수행하여 임계 부하에서의 지연 시간과 에러율을 측정합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS에서 분산 추적(Distributed Tracing)과 관찰 가능성(Observability)을 구현하는 방법은?', '네스트제이에스에서 분산 추적(분산 추적)과 관찰 가능성(옵저버빌리티)을 구현하는 방법은?', 'NESTJS',
 'OpenTelemetry SDK를 Node.js 애플리케이션에 통합하여 트레이스, 메트릭, 로그를 표준 포맷으로 수집합니다. @opentelemetry/instrumentation-nestjs-core와 @opentelemetry/instrumentation-http로 HTTP 요청과 NestJS 라이프사이클을 자동 계측합니다. Jaeger나 Zipkin으로 분산 트레이스를 시각화하여 마이크로서비스 간 요청 흐름과 병목을 분석합니다. pino 로거로 구조화된 JSON 로그를 생성하고 correlation ID를 모든 로그에 포함시켜 요청 추적을 용이하게 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'Node.js의 V8 엔진 최적화 메커니즘(JIT, Hidden Class)을 설명해주세요.', '노드제이에스의 브이에이트 엔진 최적화 메커니즘(제이아이티, 히든 클래스)을 설명해주세요.', 'NODEJS_CORE',
 'V8은 JIT(Just-In-Time) 컴파일러로 자주 실행되는 코드를 기계어로 컴파일하여 인터프리터 대비 성능을 향상시킵니다. Hidden Class는 객체 구조를 추적하여 속성 접근을 최적화하는 내부 메커니즘으로, 동일한 순서로 속성을 추가한 객체들은 같은 Hidden Class를 공유합니다. 객체 생성 후 동적으로 속성을 추가하거나 삭제하면 Hidden Class가 분기되어 최적화가 해제(Deoptimization)됩니다. 배열의 경우 같은 타입 요소를 유지하는 Packed SMI Array, Packed Double Array가 Holey Array보다 최적화됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS에서 다중 데이터베이스 연결과 데이터소스 전략을 설명해주세요.', '네스트제이에스에서 다중 데이터베이스 연결과 데이터소스 전략을 설명해주세요.', 'NESTJS',
 'TypeOrmModule.forRootAsync()를 여러 번 name 옵션으로 구분하여 복수의 DB 커넥션을 설정합니다. @InjectRepository(Entity, ''connection-name'')으로 특정 연결의 Repository를 주입받습니다. 읽기/쓰기 분리(Read Replica) 설정 시 쓰기는 마스터, 읽기는 레플리카로 라우팅하여 DB 부하를 분산합니다. 멀티 테넌트 환경에서 요청별로 DataSource를 동적 전환하는 경우 TypeORM의 ConnectionManager나 DataSource 풀을 직접 관리하여 테넌트별 DB를 격리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS 마이크로서비스에서 Saga 패턴을 구현하는 방법은?', '네스트제이에스 마이크로서비스에서 사가 패턴을 구현하는 방법은?', 'NESTJS',
 'NestJS의 EventBus와 CommandBus를 활용하여 Orchestration Saga를 구현합니다. Saga 클래스는 @Saga() 데코레이터로 이벤트 스트림(Observable)을 구독하고, 다음 실행할 Command를 반환합니다. 각 단계 성공 이벤트에 반응하여 다음 Command를 발행하고, 실패 이벤트에서 보상 Command(Compensating Transaction)를 발행합니다. 분산 환경에서는 Kafka 이벤트 스트림과 결합하여 서비스 간 Saga를 구현하고, Saga 상태를 DB에 영속화하여 재시작 시 복구합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'Node.js 애플리케이션의 보안 강화 방법을 종합적으로 설명해주세요.', '노드제이에스 애플리케이션의 보안 강화 방법을 종합적으로 설명해주세요.', 'NESTJS',
 'helmet 미들웨어로 HTTP 보안 헤더(CSP, HSTS, X-Frame-Options)를 설정하고, hpp로 HTTP 파라미터 오염을 방지합니다. SQL Injection은 TypeORM/Prisma의 파라미터 바인딩으로, XSS는 DOMPurify나 class-sanitizer로 입력 데이터를 검증합니다. Rate Limiting(@nestjs/throttler)과 Bot 감지(captcha)로 무차별 대입 공격을 방어합니다. JWT 시크릿은 환경변수로 관리하고, Argon2나 bcrypt로 비밀번호를 해싱합니다. OWASP Top 10을 기준으로 정기적인 보안 감사와 npm audit으로 의존성 취약점을 점검합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS에서 Event Sourcing과 CQRS를 결합한 아키텍처를 설계하는 방법은?', '네스트제이에스에서 이벤트 소싱과 씨큐알에스를 결합한 아키텍처를 설계하는 방법은?', 'NESTJS',
 'Aggregate는 이벤트를 apply() 메서드로 처리하여 상태를 재구성하며, 새 이벤트를 uncommittedEvents 목록에 추가합니다. EventStore(PostgreSQL의 events 테이블)에 이벤트를 추가 전용으로 저장하고, Aggregate ID로 이벤트를 재생하여 현재 상태를 복원합니다. 쓰기 후 EventBus로 이벤트를 발행하면 Projection Handler가 읽기 모델(ElasticSearch, Redis)을 비동기로 업데이트합니다. 성능 최적화를 위해 N개 이벤트마다 Snapshot을 저장하여 재생 시간을 단축합니다. 이 구조는 감사 로그, 시계열 분석, 시스템 상태 복원에 강점을 가집니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- 추가 32문항 (JUNIOR +1, MID +12, SENIOR +19)

-- JUNIOR 추가 1문항

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS의 Exception Filter는 무엇이며 어떻게 사용하나요?', '네스트제이에스의 Exception 필터는 무엇이며 어떻게 사용하나요?', 'NESTJS',
 'Exception Filter는 컨트롤러나 서비스에서 발생한 예외를 캐치하여 클라이언트에게 일관된 에러 응답을 반환하는 레이어입니다. @Catch(HttpException) 데코레이터와 ExceptionFilter 인터페이스를 구현하여 특정 예외 타입을 처리합니다. @UseFilters(new HttpExceptionFilter())로 특정 컨트롤러나 라우터에 적용하거나, app.useGlobalFilters()로 전역 적용합니다. 기본 제공되는 HttpException은 status와 message를 인자로 받으며, NotFoundException, BadRequestException 등의 서브클래스가 제공됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- MID 추가 12문항

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'Node.js의 Streams API와 파이프라이닝(pipeline)을 활용한 대용량 데이터 처리 방법은?', '노드제이에스의 스트림스 에이피아이와 파이프라이닝(파이프라이닝)을 활용한 대용량 데이터 처리 방법은?', 'NODEJS_CORE',
 'Node.js Streams는 Readable, Writable, Duplex, Transform 네 가지 타입으로 데이터를 청크 단위로 처리하여 전체 데이터를 메모리에 올리지 않고 대용량 파일을 처리합니다. stream.pipeline(src, transform, dest, callback)은 파이프라인의 에러와 정리를 자동으로 처리하며, pipe()보다 안전합니다. Transform 스트림을 구현하여 CSV 파싱, 압축, 암호화를 파이프라인에 삽입합니다. stream/promises의 pipeline()은 Promise를 반환하여 async/await와 함께 사용할 수 있습니다. backpressure를 존중하지 않으면 메모리 사용량이 무제한으로 증가하므로 Writable의 write() 반환값을 확인하고 drain 이벤트를 처리해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS에서 커스텀 데코레이터를 만들고 메타데이터를 활용하는 방법은?', '네스트제이에스에서 커스텀 데코레이터를 만들고 메타데이터를 활용하는 방법은?', 'NESTJS',
 'createParamDecorator()로 요청 객체에서 특정 데이터를 추출하는 파라미터 데코레이터를 만들고, @CurrentUser()처럼 컨트롤러 파라미터에 적용합니다. SetMetadata()로 라우터나 클래스에 메타데이터를 부착하고, Reflector를 Guard나 Interceptor에서 주입받아 메타데이터를 읽습니다. @Roles(''admin'')처럼 선언하고 Guard에서 reflector.getAllAndOverride(ROLES_KEY, [context.getHandler(), context.getClass()])로 역할을 확인합니다. applyDecorators()로 여러 데코레이터를 합성하여 재사용 가능한 복합 데코레이터를 만들 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'TypeScript의 Mapped Types와 Conditional Types를 활용한 유틸리티 타입 설계 방법은?', '타입스크립트의 맵드 타입스와 컨디셔널 타입스를 활용한 유틸리티 타입 설계 방법은?', 'NODEJS_CORE',
 'Mapped Types는 기존 타입의 각 프로퍼티를 변환하는 타입으로, { [K in keyof T]: ... } 문법을 사용합니다. Partial<T>, Required<T>, Readonly<T>, Record<K, V>가 내장 Mapped Types입니다. Conditional Types는 T extends U ? X : Y 형태로 조건에 따라 타입을 분기합니다. infer 키워드로 조건부 타입 내에서 타입을 추론합니다. 예를 들어 type UnwrapPromise<T> = T extends Promise<infer U> ? U : T로 Promise의 내부 타입을 추출합니다. NestJS에서 DTO 변환 타입(CreateDto에서 UpdateDto)을 자동 생성하는 데 활용할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS에서 WebSocket Gateway를 구현하는 방법과 주의사항은?', '네스트제이에스에서 웹소켓 Gateway를 구현하는 방법과 주의사항은?', 'NESTJS',
 '@WebSocketGateway() 데코레이터로 WebSocket 서버를 선언하고, @SubscribeMessage(''message'') 핸들러로 클라이언트 메시지를 처리합니다. @WebSocketServer()로 socket.io 서버 인스턴스를 주입받아 broadcast나 특정 룸에 메시지를 보낼 수 있습니다. Guard, Interceptor, Pipe를 HTTP와 동일하게 WebSocket에 적용할 수 있습니다. 수평 확장 환경에서 여러 서버 인스턴스 간 메시지 전달을 위해 @nestjs/platform-socket.io의 Redis Adapter를 설정합니다. 연결 수가 많은 경우 핑/퐁 타임아웃과 최대 연결 수를 설정하여 좀비 연결을 정리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'Node.js에서 Buffer와 ArrayBuffer의 차이점과 바이너리 데이터 처리 방법은?', '노드제이에스에서 Buffer와 어레이버퍼의 차이점과 바이너리 데이터 처리 방법은?', 'NODEJS_CORE',
 'Buffer는 Node.js에서 고정 크기 바이너리 데이터를 다루는 클래스로 Uint8Array를 확장합니다. ArrayBuffer는 WebAPI 표준으로 브라우저와 Node.js 모두에서 사용되며, Buffer는 ArrayBuffer의 Node.js 전용 구현입니다. Buffer.alloc(size)로 제로 초기화된 버퍼를 생성하고, Buffer.from(array/string/arrayBuffer)으로 기존 데이터로부터 생성합니다. readUInt32LE, writeInt16BE 등의 메서드로 다양한 바이트 순서(Endianness)로 숫자를 읽고 씁니다. 바이너리 프로토콜(MessagePack, Protocol Buffers) 파싱이나 파일 I/O에서 Buffer를 직접 다루는 경우가 많습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS에서 동적 모듈(Dynamic Module)을 만드는 방법과 사용 사례는?', '네스트제이에스에서 동적 모듈(다이나믹 모듈)을 만드는 방법과 사용 사례는?', 'NESTJS',
 'Dynamic Module은 register(), forRoot(), forFeature(), forRootAsync() 같은 정적 메서드로 설정을 받아 모듈을 동적으로 구성합니다. ModuleMetadata를 반환하는 정적 메서드에서 providers, exports를 동적으로 결정합니다. forRootAsync()는 useFactory 패턴으로 비동기 설정(환경 변수 검증, 외부 설정 서버)을 지원합니다. ConfigModule, TypeOrmModule, CacheModule이 Dynamic Module의 대표 예시입니다. 라이브러리를 제작할 때 Dynamic Module 패턴으로 설정 가능한 재사용 모듈을 설계하면 각 프로젝트에서 필요에 맞게 커스터마이즈할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'TypeScript의 Generics를 활용한 NestJS 공통 서비스 레이어 설계 방법은?', '타입스크립트의 제네릭스를 활용한 네스트제이에스 공통 서비스 레이어 설계 방법은?', 'NODEJS_CORE',
 '제네릭 Repository 패턴을 구현하여 공통 CRUD 로직을 abstract class BaseRepository<T, ID>로 추출합니다. 이를 확장한 UserRepository는 도메인 특화 메서드만 추가하면 됩니다. 공통 응답 타입 PageResult<T>를 제네릭으로 선언하면 모든 페이지네이션 엔드포인트에서 재사용합니다. class-transformer의 plainToInstance<T>(cls, plain)으로 타입 안전한 객체 변환 유틸을 작성합니다. 제네릭 제약(T extends Entity)으로 Repository에 전달 가능한 타입을 제한하여 런타임 오류를 컴파일 타임에 차단합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS에서 마이크로서비스 패턴(Message Pattern, Event Pattern)의 차이점은?', '네스트제이에스에서 마이크로서비스 패턴(메시지 패턴, 이벤트 패턴)의 차이점은?', 'NESTJS',
 'Message Pattern(@MessagePattern)은 요청-응답(Request-Response) 방식으로 클라이언트가 응답을 기다립니다. ClientProxy.send()로 메시지를 보내고 Observable 또는 Promise로 응답을 받습니다. Event Pattern(@EventPattern)은 단방향 발행-구독(Publish-Subscribe) 방식으로 응답을 기다리지 않습니다. ClientProxy.emit()으로 이벤트를 발행하고 처리가 완료되지 않아도 계속 진행합니다. 명령(도메인 작업 실행, 데이터 조회)에는 Message Pattern, 알림(이벤트 발생 통보)에는 Event Pattern을 사용하는 것이 CQRS 원칙에 부합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'Node.js에서 메모리 누수를 탐지하고 해결하는 방법은?', '노드제이에스에서 메모리 누수를 탐지하고 해결하는 방법은?', 'NODEJS_CORE',
 '메모리 누수의 주요 원인은 클로저로 유지되는 대형 객체, 해제되지 않는 이벤트 리스너, 커지는 캐시, 전역 배열/맵에 쌓이는 데이터입니다. --inspect 플래그로 Chrome DevTools를 연결하여 힙 스냅샷을 찍고 비교하면 누수 객체를 특정할 수 있습니다. clinic.js heap으로 시간에 따른 힙 증가 패턴을 시각화합니다. EventEmitter의 리스너는 더 이상 필요 없을 때 removeListener()나 once()로 등록하여 누수를 방지합니다. WeakMap과 WeakRef를 활용하면 참조 유지 없이 객체와 연관 데이터를 저장하여 GC가 회수할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS에서 OpenAPI(Swagger) 문서를 자동 생성하는 방법과 best practice는?', '네스트제이에스에서 오픈에이피아이(스웨거) 문서를 자동 생성하는 방법과 best practice는?', 'NESTJS',
 '@nestjs/swagger의 DocumentBuilder로 API 제목, 버전, 인증 스키마를 설정하고 SwaggerModule.createDocument()로 OpenAPI 스펙을 생성합니다. @ApiTags, @ApiOperation, @ApiResponse로 엔드포인트를 문서화합니다. DTO에 @ApiProperty({ description, example, required })를 추가하면 요청/응답 스키마가 자동 생성됩니다. @ApiExtraModels()로 직접 참조되지 않는 타입을 문서에 포함시키고, $ref로 참조합니다. CLI Plugin(nest-cli.json의 plugins)을 활성화하면 @ApiProperty를 명시하지 않아도 TypeScript 타입에서 스키마를 자동 추론합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'TypeScript의 strict 모드와 주요 컴파일러 옵션들을 설명해주세요.', '타입스크립트의 strict 모드와 주요 컴파일러 옵션들을 설명해주세요.', 'NODEJS_CORE',
 'strict: true는 strictNullChecks, strictFunctionTypes, strictBindCallApply, noImplicitAny, strictPropertyInitialization을 한 번에 활성화합니다. strictNullChecks는 null/undefined를 별개의 타입으로 취급하여 NPE 유사 오류를 컴파일 타임에 차단합니다. noImplicitAny는 타입을 추론하지 못한 곳에 자동으로 any를 허용하지 않습니다. exactOptionalPropertyTypes는 optional 프로퍼티에 undefined 명시 할당을 구분합니다. noUncheckedIndexedAccess는 배열/인덱스 접근의 결과 타입에 undefined를 추가하여 범위 초과 접근을 강제로 체크하게 합니다. NestJS 프로젝트에서 strict 모드를 활성화하면 런타임 오류를 크게 줄일 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS에서 Health Check와 Readiness/Liveness Probe를 구현하는 방법은?', '네스트제이에스에서 헬스 체크와 Readiness/라이브니스 프로브를 구현하는 방법은?', 'NESTJS',
 '@nestjs/terminus 패키지로 /health 엔드포인트를 구성하고 DB, Redis, 외부 API 상태를 포함한 헬스 체크를 구현합니다. HealthIndicator를 구현하여 커스텀 헬스 체크 로직을 추가합니다. Kubernetes에서 Liveness Probe는 애플리케이션 재시작 여부를 판단하고, Readiness Probe는 트래픽 수신 준비 상태를 판단합니다. 시작 초기화 중에는 readiness를 false로, DB 연결 확인 후 true로 변경합니다. 헬스 체크 엔드포인트는 인증을 제외하고 Rate Limiting을 적용하여 외부 접근은 허용하되 DDoS를 방지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- SENIOR 추가 19문항

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'Node.js 애플리케이션의 Zero-Downtime 배포 전략을 설명해주세요.', '노드제이에스 애플리케이션의 제로 다운타임 배포 전략을 설명해주세요.', 'PERFORMANCE',
 'PM2의 cluster 모드에서 --wait-ready와 --listen-timeout 옵션으로 프로세스 교체 시 새 프로세스가 준비된 후 구 프로세스를 종료하여 무중단 배포를 구현합니다. Kubernetes Rolling Update와 Readiness Probe를 조합하면 새 Pod가 준비되기 전까지 트래픽을 보내지 않습니다. SIGTERM 수신 시 process.on(''SIGTERM'')에서 새 요청 수신을 중단하고 처리 중인 요청이 완료될 때까지 대기하는 Graceful Shutdown을 구현합니다. HTTP Keep-Alive 연결을 서버 종료 전에 server.closeIdleConnections()로 닫아 연결 유지 클라이언트의 갑작스러운 연결 끊김을 방지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS에서 테넌트별 DB 격리를 구현하는 멀티 테넌시 아키텍처를 설명해주세요.', '네스트제이에스에서 테넌트별 DB 격리를 구현하는 멀티 테넌시 아키텍처를 설명해주세요.', 'NESTJS',
 '요청 헤더(X-Tenant-Id) 또는 서브도메인에서 테넌트를 식별하는 미들웨어를 구성하고, AsyncLocalStorage로 요청 컨텍스트에 테넌트 정보를 전파합니다. 스키마 기반 격리는 테넌트별 PostgreSQL 스키마를 동적으로 전환하는 TypeORM 커스텀 Repository로 구현합니다. 별도 DB 인스턴스 방식은 DataSource 풀을 테넌트 ID로 인덱싱하고, 요청마다 해당 DataSource를 선택합니다. 마이그레이션은 모든 테넌트 스키마에 순차 또는 병렬로 적용하는 스크립트를 관리합니다. 새 테넌트 온보딩 시 스키마 생성과 초기 마이그레이션을 트랜잭션으로 처리하여 부분 실패를 방지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'Node.js에서 CPU 바운드 작업을 Worker Thread로 오프로딩하는 패턴을 설명해주세요.', '노드제이에스에서 씨피유 바운드 작업을 워커 스레드로 오프로딩하는 패턴을 설명해주세요.', 'NODEJS_CORE',
 'Worker Thread는 isMainThread로 메인/워커를 구분하고, parentPort.on(''message'')로 데이터를 받아 처리 후 parentPort.postMessage()로 결과를 반환합니다. Worker 생성 비용을 줄이기 위해 Worker Thread Pool(piscina 라이브러리)을 사용하여 미리 생성된 워커에 작업을 분배합니다. SharedArrayBuffer와 Atomics로 메인 스레드와 워커 간에 복사 없이 대용량 데이터를 공유합니다. workerData로 초기 데이터를 전달하고, transferList로 ArrayBuffer 소유권을 이전하여 데이터 복사 없이 워커로 전달합니다. Worker 내부에서 예외 발생 시 worker.on(''error'') 핸들러로 메인 스레드에서 처리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS 기반 서비스의 부하 테스트와 성능 벤치마킹 방법을 설명해주세요.', '네스트제이에스 기반 서비스의 부하 테스트와 성능 벤치마킹 방법을 설명해주세요.', 'PERFORMANCE',
 'k6와 autocannon으로 HTTP 부하 테스트를 수행하고 RPS, 지연 시간 p50/p95/p99, 에러율을 측정합니다. k6 스크립트에서 가상 사용자(VU) 수를 단계적으로 증가시켜 스케일링 한계를 찾습니다. clinic.js flame으로 CPU 핫스팟을 시각화하고, doctor로 이벤트 루프 지연과 비효율적인 I/O 패턴을 진단합니다. 부하 테스트 중 --inspect로 연결한 Chrome DevTools에서 실시간 힙 사용량과 CPU 프로파일을 수집합니다. 테스트 환경에서 Prometheus 메트릭을 수집하고 Grafana 대시보드로 시각화하여 배포 전후 성능 회귀를 탐지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'TypeScript의 타입 시스템을 활용한 도메인 모델링과 타입 안전성 확보 전략은?', '타입스크립트의 타입 시스템을 활용한 도메인 모델링과 타입 안전성 확보 전략은?', 'NODEJS_CORE',
 'Branded/Nominal Types로 string/number 기반 도메인 식별자(UserId, OrderId)를 컴파일 타임에 구분합니다. type UserId = string & { readonly __brand: unique symbol }처럼 선언하여 일반 string 할당을 차단합니다. Discriminated Union으로 상태 머신을 표현하면 특정 상태에서만 존재하는 필드를 타입 안전하게 접근합니다. Template Literal Types로 이벤트 이름, API 경로 패턴을 타입으로 제약합니다. const assertion(as const)으로 배열이나 객체를 리터럴 타입으로 좁혀 런타임 값을 타입으로 활용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS에서 플러그인 아키텍처와 동적 기능 확장 패턴을 설명해주세요.', '네스트제이에스에서 플러그인 아키텍처와 동적 기능 확장 패턴을 설명해주세요.', 'NESTJS',
 'Dynamic Module의 forFeature() 패턴으로 각 기능 모듈이 필요한 서브 기능(레포지토리, 설정 네임스페이스)을 동적으로 등록합니다. ModuleRef를 사용하면 DI 컨테이너에서 임의의 Provider를 동적으로 조회하여 플러그인 형태로 기능을 주입합니다. Strategy 패턴 + 팩토리를 결합하여 런타임에 알고리즘 구현체를 교체합니다. APP_INTERCEPTOR, APP_GUARD, APP_PIPE 토큰으로 전역 Provider를 동적으로 등록합니다. 마이크로서비스 Transport(TCP, Redis, NATS)를 런타임 환경 변수로 결정하는 Dynamic Module 패턴으로 배포 환경에 따라 메시지 브로커를 교체합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'Node.js에서 암호화와 보안 관련 모범 사례를 설명해주세요.', '노드제이에스에서 암호화와 보안 관련 모범 사례를 설명해주세요.', 'NODEJS_CORE',
 'node:crypto 모듈의 scrypt 또는 bcrypt로 비밀번호를 해싱하고, crypto.timingSafeEqual()로 타이밍 어택을 방지하며 문자열을 비교합니다. AES-256-GCM으로 데이터를 대칭 암호화하고, 인증 태그(auth tag)로 무결성을 검증합니다. HTTPS 강제를 위해 helmet의 HSTS를 설정하고, 민감 데이터를 환경 변수나 AWS Secrets Manager로 관리합니다. crypto.randomBytes()로 예측 불가능한 토큰을 생성하고, Math.random()은 보안 목적에 절대 사용하지 않습니다. npm audit과 Snyk으로 의존성 취약점을 정기 점검하고, package-lock.json을 커밋하여 의존성을 고정합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS에서 GraphQL Federation 아키텍처를 구현하는 방법은?', '네스트제이에스에서 그래프큐엘 페더레이션 아키텍처를 구현하는 방법은?', 'NESTJS',
 '@nestjs/graphql과 @apollo/subgraph로 각 서비스를 GraphQL 서브그래프(Subgraph)로 구성하고, Apollo Gateway가 여러 서브그래프를 하나의 통합 스키마로 합칩니다. @Directive(''@key(fields: "id")'')로 엔티티의 기본 키를 선언하고, resolveReference() 메서드로 다른 서비스에서 해당 엔티티를 조회하는 방법을 구현합니다. @ResolveField로 다른 서브그래프의 타입을 확장하여 서비스 간 데이터를 연결합니다. Subscription은 서브그래프별로 구현하고 Gateway에서 합산합니다. 각 서브그래프는 독립 배포 가능하며 스키마 변경 시 Gateway에 자동으로 반영됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'Node.js의 AsyncLocalStorage를 활용한 요청 컨텍스트 전파 패턴은?', '노드제이에스의 에이싱크로컬스토리지를 활용한 요청 컨텍스트 전파 패턴은?', 'NODEJS_CORE',
 'AsyncLocalStorage는 비동기 작업 체인 전체에 데이터를 전파하는 API로, ThreadLocal의 Node.js 동등 개념입니다. 미들웨어에서 asyncLocalStorage.run(store, callback)으로 요청 컨텍스트(요청 ID, 사용자 정보, 트레이스 ID)를 설정합니다. 이후 같은 요청 체인의 어디서든 asyncLocalStorage.getStore()로 접근 가능합니다. NestJS에서 클로저 모듈(cls-hooked 또는 직접 AsyncLocalStorage 래퍼)로 @Injectable() 서비스처럼 컨텍스트를 주입받을 수 있습니다. 로그에 요청 ID를 자동으로 포함시켜 분산 트레이싱의 correlation ID를 전파하는 데 가장 일반적으로 활용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS 기반 서비스의 Circuit Breaker 패턴 구현과 Resilience 전략은?', '네스트제이에스 기반 서비스의 써킷 브레이커 패턴 구현과 레질리언스 전략은?', 'NESTJS',
 'Circuit Breaker는 외부 서비스 장애 시 요청을 차단하여 연쇄 장애(Cascade Failure)를 방지하는 패턴입니다. opossum 라이브러리로 CircuitBreaker를 생성하고 실패 임계값, 타임아웃, 반열림(half-open) 요청 수를 설정합니다. NestJS Interceptor로 Circuit Breaker를 래핑하면 서비스 레이어 변경 없이 적용 가능합니다. Fallback 함수를 등록하여 회로 열림 상태에서 캐시된 응답이나 기본값을 반환합니다. Prometheus에 circuitBreaker.fallbackCount, circuitBreaker.rejectCount 메트릭을 수집하고 알림을 설정하여 외부 의존성 장애를 조기에 감지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'TypeScript와 NestJS에서 Domain-Driven Design(DDD)을 구현하는 방법은?', '타입스크립트와 네스트제이에스에서 도메인 드리븐 디자인(디디디)을 구현하는 방법은?', 'NESTJS',
 '도메인 레이어를 프레임워크 의존성 없이 순수 TypeScript 클래스(Entity, ValueObject, AggregateRoot, DomainService)로 구성합니다. ValueObject는 생성자를 private으로 막고 정적 팩토리 메서드(Email.from(value))로만 생성하여 불변성을 보장합니다. AggregateRoot는 도메인 이벤트를 내부 배열에 쌓고, Infrastructure 레이어에서 저장 후 이벤트를 발행합니다. Repository 인터페이스를 도메인에 정의하고 구현은 Infrastructure(TypeORM Repository)에서 담당합니다. NestJS 모듈 구조를 Bounded Context와 일치시켜 각 컨텍스트가 독립적인 모듈로 관리되도록 설계합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'Node.js 런타임의 진단 채널(Diagnostics Channel)과 계측(Instrumentation) 기법은?', '노드제이에스 런타임의 진단 채널(다이애그노스틱스 채널)과 계측(인스트루멘테이션) 기법은?', 'NODEJS_CORE',
 'Node.js 16+의 diagnostics_channel 모듈은 라이브러리 내부 이벤트를 low-overhead로 구독하는 메커니즘입니다. channel.publish(data)로 이벤트를 발행하고, channel.subscribe(onMessage)로 구독합니다. OpenTelemetry의 Auto-Instrumentation이 http, net, dns 등의 핵심 모듈을 diagnostics_channel로 계측합니다. 커스텀 계측이 필요한 경우 diagnostics_channel로 비즈니스 이벤트를 발행하고, APM 에이전트가 구독하여 트레이스를 수집합니다. PerformanceObserver로 GC 이벤트, HTTP 요청 타이밍, 이벤트 루프 지연을 고해상도로 측정합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS에서 이벤트 기반 아키텍처와 Outbox 패턴을 구현하는 방법은?', '네스트제이에스에서 이벤트 기반 아키텍처와 아웃박스 패턴을 구현하는 방법은?', 'NESTJS',
 'Outbox 패턴은 DB 트랜잭션 내에서 도메인 이벤트를 outbox 테이블에 함께 저장하여, 커밋 성공 시에만 이벤트가 발행됨을 보장합니다. @TransactionalEventListener(phase: TransactionPhase.AFTER_COMMIT)와 함께 outbox 테이블 조회 + 메시지 브로커 발행을 구현합니다. 메시지 브로커 발행 실패 시 outbox 레코드를 published=false로 유지하고, 스케줄러가 미발행 이벤트를 재시도합니다. Debezium CDC(Change Data Capture)를 사용하면 outbox 테이블 변경을 자동으로 Kafka로 스트리밍하여 폴링 없이 구현합니다. 이벤트 발행에 멱등성 키를 포함시켜 중복 발행 시 소비자가 중복 처리를 방지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'Node.js에서 스트리밍 JSON 파싱과 대용량 데이터 직렬화 최적화 방법은?', '노드제이에스에서 스트리밍 제이슨 파싱과 대용량 데이터 직렬화 최적화 방법은?', 'NODEJS_CORE',
 'JSON.parse()는 전체 문자열을 메모리에 올려야 하므로 대용량 JSON에 부적합합니다. stream-json 라이브러리는 JSON을 스트리밍으로 파싱하여 대용량 JSON 배열에서 개별 객체만 추출할 수 있습니다. fast-json-stringify는 JSON 스키마로 직렬화 함수를 사전 컴파일하여 JSON.stringify()보다 2~5배 빠르게 동작합니다. Fastify의 응답 직렬화는 fast-json-stringify를 활용하여 고성능 HTTP 응답을 생성합니다. NDJSON(Newline-Delimited JSON)은 각 줄이 독립 JSON 객체인 형식으로 readline 인터페이스로 라인별 파싱이 가능하여 스트리밍 데이터 교환에 적합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS와 Prisma ORM을 결합한 타입 안전한 데이터 접근 레이어 설계는?', '네스트제이에스와 프리즈마 오알엠을 결합한 타입 안전한 데이터 접근 레이어 설계는?', 'NESTJS',
 'Prisma는 schema.prisma에서 모델을 정의하면 타입 안전한 PrismaClient를 자동 생성하며, NestJS에서 PrismaService로 래핑하여 @Injectable()로 제공합니다. Prisma Client의 $transaction([])으로 여러 쿼리를 배치 트랜잭션으로 실행하고, $transaction(async (prisma) => {})으로 인터랙티브 트랜잭션을 구현합니다. Prisma의 select와 include로 필요한 필드만 조회하여 over-fetching을 방지합니다. Prisma Middleware로 soft delete, 감사 로그, 테넌트 필터링을 AOP 방식으로 구현합니다. DB 스키마 변경은 prisma migrate dev로 마이그레이션 파일을 생성하고, CI에서 prisma migrate deploy로 자동 적용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'Node.js 기반 서비스의 카오스 엔지니어링(Chaos Engineering) 적용 방법은?', '노드제이에스 기반 서비스의 카오스 엔지니어링(카오스 엔지니어링) 적용 방법은?', 'PERFORMANCE',
 '카오스 엔지니어링은 의도적으로 장애를 주입하여 시스템의 회복 탄력성을 검증합니다. chaos-utils 또는 직접 구현한 미들웨어로 임의 지연, 네트워크 오류, 서비스 오류를 주입합니다. toxiproxy로 네트워크 레벨에서 지연, 패킷 손실, 대역폭 제한을 시뮬레이션합니다. Steady State를 정의(정상 RPS, 에러율)하고 카오스 실험 전후로 측정하여 회복 여부를 검증합니다. Circuit Breaker, Retry with Backoff, Timeout, Bulkhead 패턴이 카오스 주입 상황에서 예상대로 동작하는지 확인합니다. Kubernetes 환경에서 Chaos Mesh로 Pod 종료, CPU/메모리 부하 주입을 자동화합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'NestJS에서 플러그인 기반 인증/인가 시스템을 설계하는 방법은?', '네스트제이에스에서 플러그인 기반 인증/인가 시스템을 설계하는 방법은?', 'NESTJS',
 'Passport.js를 @nestjs/passport로 통합하여 여러 인증 전략(JWT, OAuth2, SAML, LDAP)을 Strategy 패턴으로 교체 가능하게 구성합니다. CASL(casl.js)이나 커스텀 RBAC으로 세밀한 권한 제어(Can User do Action on Resource)를 구현합니다. @CheckPolicies() 데코레이터와 PolicyGuard를 조합하여 라우터별 접근 정책을 선언적으로 정의합니다. 권한 데이터를 DB에서 관리하여 재배포 없이 권한을 변경하고, Redis에 캐싱하여 매 요청마다 DB를 조회하는 오버헤드를 줄입니다. 감사 로그(Audit Log) Interceptor로 모든 권한 결정과 중요 액션을 기록하여 보안 감사에 활용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'TypeScript 모노레포에서 NestJS 백엔드와 공유 패키지를 관리하는 전략은?', '타입스크립트 모노레포에서 네스트제이에스 백엔드와 공유 패키지를 관리하는 전략은?', 'NODEJS_CORE',
 'Turborepo 또는 Nx로 모노레포를 관리하고, 백엔드 앱과 공유 라이브러리(dto, constants, utils)를 별도 패키지로 분리합니다. 공유 DTO 패키지를 class-validator 데코레이터로 구성하면 프론트엔드와 백엔드가 동일한 유효성 검사 규칙을 공유합니다. tsconfig의 paths 매핑으로 패키지를 로컬에서 직접 참조하고, 빌드 시에만 npm 패키지로 해석합니다. Turborepo의 빌드 캐시로 변경된 패키지에 의존하는 것만 재빌드하여 CI 시간을 단축합니다. 버전 관리는 Changesets로 패키지별 변경 이력을 추적하고 배포 시 자동 버전 범프를 수행합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:NODE_NESTJS:LANGUAGE_FRAMEWORK', 'Node.js에서 Custom Protocol과 바이너리 직렬화(Protocol Buffers, MessagePack) 활용 방법은?', '노드제이에스에서 Custom 프로토콜과 바이너리 직렬화(프로토콜 버퍼스, 메시지팩) 활용 방법은?', 'NODEJS_CORE',
 'Protocol Buffers(protobuf)는 구조화된 데이터를 JSON 대비 3~10배 작은 바이너리로 직렬화합니다. @grpc/proto-loader와 @grpc/grpc-js로 NestJS에서 gRPC 서버를 구성하고 .proto 파일로 서비스 계약을 정의합니다. MessagePack은 JSON과 호환되는 바이너리 포맷으로 @msgpack/msgpack 라이브러리를 사용합니다. NestJS 마이크로서비스의 Custom Serializer로 msgpack을 구현하면 메시지 페이로드 크기를 줄여 네트워크 비용을 절감합니다. 내부 서비스 간 고빈도 통신에는 gRPC, 외부 공개 API에는 REST/JSON을 병용하는 것이 일반적입니다.',
 'MODEL_ANSWER', TRUE, NOW());
