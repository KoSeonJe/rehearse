-- backend-python-django.sql
-- Python/Django/FastAPI 백엔드 면접 질문 Pool 시딩
-- 총 90문항: JUNIOR 30 / MID 30 / SENIOR 30
-- cache_key: BACKEND:{Level}:PYTHON_DJANGO:LANGUAGE_FRAMEWORK

-- ============================================================
-- JUNIOR (30문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Python의 GIL(Global Interpreter Lock)이란 무엇인가요?', 'PYTHON_CORE',
 'GIL은 CPython 인터프리터에서 한 번에 하나의 스레드만 Python 바이트코드를 실행할 수 있도록 제한하는 뮤텍스입니다. 멀티스레드 환경에서도 CPU 바운드 작업은 GIL로 인해 실제 병렬 실행이 불가능하며, I/O 바운드 작업은 GIL이 릴리즈되므로 스레드 병렬성이 어느 정도 유효합니다. GIL은 CPython의 메모리 관리(참조 카운팅) 안전성을 보장하기 위해 존재하며, PyPy나 Jython 같은 대안 구현체에는 GIL이 없습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Python 데코레이터(Decorator)의 기본 개념과 사용법을 설명해주세요.', 'PYTHON_CORE',
 '데코레이터는 함수나 클래스를 인자로 받아 기능을 추가하거나 변환한 후 반환하는 고차 함수입니다. @syntax_sugar 문법으로 함수 선언 위에 적용하며, 내부적으로 wrapped_func = decorator(original_func) 와 동일합니다. 로깅, 인증 확인, 실행 시간 측정, 캐싱 등 횡단 관심사를 핵심 로직과 분리할 때 유용하며, functools.wraps를 사용하면 래핑된 함수의 메타데이터(이름, 독스트링)를 보존할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Django와 FastAPI의 주요 차이점은 무엇인가요?', 'DJANGO',
 'Django는 ORM, 인증, 어드민, 폼 처리 등 풀스택 기능을 내장한 배터리 포함형 프레임워크로, 빠른 개발과 관리자 페이지가 필요한 프로젝트에 적합합니다. FastAPI는 ASGI 기반의 비동기 웹 프레임워크로, Pydantic을 통한 자동 요청/응답 검증과 OpenAPI 문서 자동 생성이 강점이며 성능이 뛰어납니다. Django는 동기 중심이지만 Django 3.1+부터 비동기 뷰를 지원하고, FastAPI는 async/await가 기본이므로 I/O 집약적 API 서버에 유리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Django의 MTV 아키텍처를 설명해주세요.', 'DJANGO',
 'MTV는 Model-Template-View의 약자로, MVC 패턴을 Django 방식으로 재해석한 아키텍처입니다. Model은 데이터베이스 구조와 비즈니스 로직을 담당하고, Template은 HTML 렌더링을 담당하며, View는 HTTP 요청을 처리하고 Model에서 데이터를 가져와 Template에 전달하는 역할을 합니다. MVC의 Controller에 해당하는 것이 Django의 View이며, MVC의 View에 해당하는 것이 Django의 Template입니다. URLconf가 URL을 View 함수에 매핑합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Python의 async/await 기본 문법과 동작 방식을 설명해주세요.', 'ASYNC',
 'async def로 선언된 함수는 코루틴(coroutine)을 반환하며, await 키워드로 다른 코루틴이나 awaitable 객체의 완료를 기다립니다. I/O 대기 시 현재 코루틴이 일시 중단되고 이벤트 루프가 다른 코루틴을 실행하는 협력적 멀티태스킹(Cooperative Multitasking) 방식입니다. asyncio.run()으로 이벤트 루프를 시작하고, asyncio.gather()나 asyncio.create_task()로 여러 코루틴을 동시 실행합니다. 단, await 없이 CPU 바운드 작업을 하면 이벤트 루프가 차단되므로 CPU 집약 작업은 executor에서 실행해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Python의 @property 데코레이터 사용법과 이점은?', 'PYTHON_CORE',
 '@property는 클래스의 메서드를 속성처럼 접근할 수 있게 해주는 내장 데코레이터입니다. getter는 @property로, setter는 @attr.setter로, deleter는 @attr.deleter로 선언하여 속성 접근/수정/삭제에 대한 로직을 캡슐화합니다. 직접 인스턴스 변수를 노출하는 대신 @property를 사용하면 유효성 검증, 계산된 속성, 접근 로깅 등을 외부 인터페이스 변경 없이 추가할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Django ORM의 기본 사용법과 QuerySet이란 무엇인가요?', 'ORM',
 'Django ORM은 Python 객체를 SQL로 변환하는 추상화 계층으로, Model.objects.filter(), all(), get(), create() 등의 메서드로 DB를 조작합니다. QuerySet은 DB에서 가져온 객체의 컬렉션으로 Lazy Evaluation 특성을 가져, 실제 평가(반복, 슬라이싱, list() 변환, if문 등)가 일어나기 전까지 SQL이 실행되지 않습니다. filter()와 exclude()로 조건을 추가하고, order_by(), values(), annotate() 등으로 가공하며, 체이닝이 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'FastAPI에서 의존성 주입(Dependency Injection)을 어떻게 구현하나요?', 'FASTAPI',
 'FastAPI의 Depends() 함수를 통해 의존성 주입을 구현합니다. 경로 함수 파라미터에 Depends(의존성_함수)를 선언하면, FastAPI가 자동으로 의존성 함수를 호출하고 결과를 주입합니다. DB 세션, 인증 토큰 검증, 공통 쿼리 파라미터 처리 등을 재사용 가능한 의존성으로 분리할 수 있습니다. 의존성 함수도 async def로 선언 가능하며, yield를 사용하면 요청 전/후 처리(컨텍스트 매니저)도 구현할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Python의 리스트 컴프리헨션과 제너레이터 표현식의 차이는 무엇인가요?', 'PYTHON_CORE',
 '리스트 컴프리헨션([x for x in iterable])은 즉시 전체 리스트를 메모리에 생성합니다. 제너레이터 표현식((x for x in iterable))은 이터레이터를 반환하여 값을 하나씩 지연 생성하므로 메모리를 훨씬 적게 사용합니다. 대용량 데이터 처리나 모든 값이 필요하지 않은 경우 제너레이터가 유리하며, sum()이나 any() 같이 이터레이터를 소비하는 함수에는 제너레이터를 바로 전달할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Python 제너레이터(Generator)란 무엇이며 어떻게 사용하나요?', 'PYTHON_CORE',
 '제너레이터는 yield 키워드를 사용하여 값을 순차적으로 반환하는 특수 함수로, 호출 시 제너레이터 이터레이터 객체를 반환합니다. next()를 호출할 때마다 yield까지 실행하고 일시 중단 후 값을 반환하며, 함수 상태를 유지합니다. 대용량 파일 읽기, 무한 수열, 데이터 파이프라인 등 모든 데이터를 메모리에 올리지 않고 순차 처리할 때 효율적입니다. async def + yield로 비동기 제너레이터도 구현 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Django에서 URL 패턴을 설정하는 방법을 설명해주세요.', 'DJANGO',
 'Django의 urls.py에서 urlpatterns 리스트에 path() 또는 re_path() 함수로 URL 패턴을 등록합니다. path(''users/<int:pk>/'', views.user_detail)처럼 꺽쇠 괄호로 URL 파라미터 타입과 이름을 지정합니다. include()로 앱별 URLs를 분리하여 주 urls.py에서 임포트하면 모듈화된 URL 관리가 가능합니다. name 파라미터로 URL에 이름을 부여하면 reverse()나 템플릿의 {% url %} 태그로 URL을 하드코딩 없이 참조할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Django REST Framework(DRF)의 Serializer 역할은 무엇인가요?', 'DJANGO',
 'DRF Serializer는 복잡한 Django 모델 인스턴스나 QuerySet을 JSON 등으로 직렬화하거나, 요청 데이터를 파이썬 객체로 역직렬화하고 검증하는 역할을 합니다. ModelSerializer는 Model 필드를 기반으로 자동으로 Serializer 필드를 생성하여 보일러플레이트를 줄여줍니다. validated_data 속성으로 검증 통과 데이터에 접근하고, .save()로 생성/수정을 처리합니다. 필드 레벨 validate_<field>() 메서드와 객체 레벨 validate() 메서드로 커스텀 검증 로직을 추가할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'FastAPI에서 Pydantic 모델을 이용한 요청/응답 검증을 설명해주세요.', 'FASTAPI',
 'FastAPI는 Pydantic BaseModel을 상속한 클래스로 요청 바디와 응답 스키마를 정의합니다. 경로 함수 파라미터에 Pydantic 모델 타입을 선언하면 FastAPI가 자동으로 JSON 파싱과 타입 검증을 수행하며, 검증 실패 시 422 Unprocessable Entity를 반환합니다. response_model 파라미터로 응답 스키마를 지정하면 반환 데이터가 자동으로 필터링/직렬화됩니다. Field()로 필드별 유효성 규칙(min_length, regex 등)을 선언하고, OpenAPI 문서에도 자동 반영됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Python의 *args와 **kwargs는 무엇인가요?', 'PYTHON_CORE',
 '*args는 가변 위치 인수를 튜플로 수집하여 함수가 임의 개수의 위치 인수를 받을 수 있게 합니다. **kwargs는 가변 키워드 인수를 딕셔너리로 수집하여 임의 개수의 키워드 인수를 받을 수 있게 합니다. 함수 호출 시에도 *iterable로 시퀀스를 풀어 위치 인수로 전달하고, **mapping으로 딕셔너리를 키워드 인수로 전달할 수 있습니다. 데코레이터 작성 시 wrapping 함수에서 (*args, **kwargs)를 사용하면 원본 함수의 시그니처를 그대로 위임할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Django에서 마이그레이션(Migration)이란 무엇이며 어떻게 관리하나요?', 'DJANGO',
 '마이그레이션은 Django ORM 모델 변경사항을 데이터베이스 스키마에 반영하기 위한 변경 이력 파일입니다. makemigrations 명령어로 모델 변경사항을 감지하여 마이그레이션 파일을 생성하고, migrate 명령어로 실제 DB에 적용합니다. 각 마이그레이션 파일은 이전 마이그레이션과의 의존성을 가지므로 버전 관리에 포함해야 합니다. showmigrations로 적용 현황을 확인하고, migrate app 0001처럼 특정 버전으로 롤백할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Python의 클래스와 인스턴스 변수의 차이점은?', 'PYTHON_CORE',
 '클래스 변수는 클래스 정의 내 메서드 외부에 선언되어 모든 인스턴스가 공유하며, ClassName.attr 또는 self.attr으로 접근할 수 있습니다. 인스턴스 변수는 __init__ 메서드 내 self.attr = value로 선언되어 각 인스턴스마다 독립된 값을 가집니다. 가변 클래스 변수(list, dict)를 인스턴스에서 수정하면 모든 인스턴스에 영향을 주므로, 인스턴스별로 독립된 컬렉션이 필요하면 반드시 인스턴스 변수로 선언해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Django의 settings.py에서 환경별 설정을 어떻게 분리하나요?', 'DJANGO',
 '환경별 설정 분리는 base.py(공통), dev.py(개발), prod.py(운영)로 파일을 나누고 dev/prod에서 base를 임포트하는 방식이 일반적입니다. DJANGO_SETTINGS_MODULE 환경변수로 활성 설정 파일을 지정합니다. 민감 정보(SECRET_KEY, DB 비밀번호, API 키)는 설정 파일에 하드코딩하지 않고, python-decouple이나 os.environ.get()으로 환경변수에서 읽거나 .env 파일을 활용합니다. .env 파일은 반드시 .gitignore에 추가하여 버전 관리에서 제외해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'FastAPI의 경로 파라미터, 쿼리 파라미터, 요청 바디 처리 방법을 설명해주세요.', 'FASTAPI',
 '경로 파라미터는 @app.get(''/items/{item_id}'')처럼 경로에 선언하고 함수 파라미터에 동일 이름으로 받으며, 타입 힌트로 자동 변환됩니다. 쿼리 파라미터는 경로에 없는 단순 타입 파라미터를 선언하면 자동으로 쿼리 스트링으로 처리합니다. 요청 바디는 Pydantic BaseModel 타입 파라미터로 선언하면 자동으로 JSON 바디에서 파싱됩니다. Query(), Path(), Body() 함수로 기본값, 검증 규칙, OpenAPI 문서 메타데이터를 추가할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Python의 예외 처리(try/except/finally)를 설명해주세요.', 'PYTHON_CORE',
 'try 블록에 예외가 발생할 수 있는 코드를 작성하고, except ExceptionType as e로 특정 예외를 처리합니다. 여러 except 절로 다양한 예외 유형을 처리하며, Exception을 상속하여 커스텀 예외 클래스를 정의할 수 있습니다. else 절은 예외가 발생하지 않았을 때 실행되고, finally 절은 예외 발생 여부와 무관하게 항상 실행되어 리소스 해제에 사용됩니다. 너무 광범위한 except Exception:은 예상치 못한 오류를 숨길 수 있으므로 구체적인 예외 타입을 명시하는 것이 좋습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Django의 Admin 인터페이스를 어떻게 설정하고 활용하나요?', 'DJANGO',
 'admin.py에서 admin.site.register(Model) 또는 @admin.register(Model) 데코레이터로 모델을 어드민에 등록합니다. ModelAdmin 클래스를 상속하여 list_display, search_fields, list_filter, readonly_fields 등을 설정하면 관리자 인터페이스를 커스터마이징할 수 있습니다. 어드민 사이트는 is_staff=True인 사용자만 접근 가능하며, superuser는 createsuperuser 명령어로 생성합니다. 운영 환경에서는 어드민 URL을 기본 /admin/에서 변경하고 IP 제한이나 추가 인증을 적용하는 것이 보안상 권장됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Python의 딕셔너리 컴프리헨션과 집합 컴프리헨션을 설명해주세요.', 'PYTHON_CORE',
 '딕셔너리 컴프리헨션은 {key: value for item in iterable if condition} 문법으로 딕셔너리를 생성하며, 기존 딕셔너리를 변환하거나 두 리스트를 하나의 딕셔너리로 합칠 때 유용합니다. 집합 컴프리헨션은 {expr for item in iterable}로 중복 없는 집합을 생성합니다. 컴프리헨션은 가독성 높은 함수형 데이터 변환을 가능하게 하지만, 지나치게 복잡한 표현식은 오히려 가독성을 해치므로 단계별 처리 또는 함수로 분리하는 것이 좋습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Django의 모델 관계(ForeignKey, ManyToMany)를 설명해주세요.', 'ORM',
 'ForeignKey는 N:1 관계를 구현하며 참조 테이블의 PK를 외래 키로 저장합니다. on_delete 파라미터로 참조 대상 삭제 시 동작(CASCADE, SET_NULL, PROTECT 등)을 설정해야 합니다. ManyToManyField는 N:M 관계를 중간 테이블로 구현하며, through 파라미터로 중간 테이블 모델을 직접 정의하여 추가 필드를 설정할 수 있습니다. OneToOneField는 1:1 관계로 Django의 User 모델을 Profile 모델로 확장할 때 주로 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Python의 with 문(Context Manager)은 어떻게 동작하나요?', 'PYTHON_CORE',
 'with 문은 __enter__와 __exit__ 메서드를 구현한 컨텍스트 매니저를 활용하여 리소스 관리를 자동화합니다. with 블록 진입 시 __enter__가 호출되고, 블록 종료(정상/예외 무관)시 __exit__가 호출되어 파일 닫기, DB 연결 해제, 락 해제 등을 보장합니다. @contextlib.contextmanager 데코레이터를 사용하면 yield 기반의 제너레이터 함수로 간단히 컨텍스트 매니저를 구현할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'FastAPI에서 HTTP 예외 처리를 어떻게 구현하나요?', 'FASTAPI',
 'FastAPI에서는 HTTPException(status_code=404, detail="Not Found")를 raise하여 HTTP 오류 응답을 반환합니다. @app.exception_handler(ExceptionType)으로 전역 예외 핸들러를 등록하여 커스텀 예외 클래스를 HTTP 응답으로 변환할 수 있습니다. Pydantic 검증 실패(RequestValidationError)의 기본 응답 포맷도 exception_handler로 오버라이드하여 통일된 에러 응답 포맷을 유지합니다. 에러 응답에 커스텀 헤더를 추가하는 경우 headers 파라미터를 HTTPException에 전달합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Python의 타입 힌트(Type Hints)란 무엇이며 왜 사용하나요?', 'PYTHON_CORE',
 'Python 3.5+부터 지원하는 타입 힌트는 변수, 함수 파라미터, 반환값의 타입을 명시하는 선택적 주석입니다. 런타임에 타입을 강제하지는 않지만, mypy 같은 정적 분석 도구로 타입 오류를 사전에 발견하고, IDE에서 자동완성과 오류 감지를 지원합니다. FastAPI는 타입 힌트를 기반으로 요청 검증과 OpenAPI 문서를 자동 생성하므로 정확한 타입 선언이 중요합니다. Optional[str], List[int], Dict[str, Any], Union[str, int] 등 typing 모듈과 Python 3.10+의 built-in 타입 문법을 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Django에서 정적 파일(static files)과 미디어 파일(media files)의 차이는?', 'DJANGO',
 '정적 파일은 CSS, JS, 이미지 등 소스코드에 포함된 변경 없는 파일로, STATIC_URL과 STATICFILES_DIRS에서 관리하며 collectstatic 명령어로 STATIC_ROOT에 수집합니다. 미디어 파일은 사용자가 업로드한 파일로 MEDIA_URL과 MEDIA_ROOT로 설정하며, FileField/ImageField가 MEDIA_ROOT 아래에 파일을 저장합니다. 개발 환경에서는 Django가 직접 서빙하지만, 운영 환경에서는 Nginx나 S3+CloudFront를 통해 서빙하는 것이 성능과 보안 측면에서 권장됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'FastAPI에서 CORS 설정 방법을 설명해주세요.', 'FASTAPI',
 'FastAPI에서 CORS는 Starlette의 CORSMiddleware를 app.add_middleware()로 등록하여 설정합니다. allow_origins에 허용할 출처 목록(["https://example.com"]), allow_methods에 허용 HTTP 메서드, allow_headers에 허용 헤더를 지정합니다. 개발 환경에서는 편의상 allow_origins=["*"]를 사용하지만 운영에서는 특정 도메인만 허용해야 합니다. 인증 쿠키나 Authorization 헤더를 포함하는 경우 allow_credentials=True와 구체적인 allow_origins를 함께 설정해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Python 가상 환경(venv, virtualenv)을 사용하는 이유는?', 'PYTHON_CORE',
 '가상 환경은 프로젝트별로 독립된 Python 패키지 공간을 제공하여, 프로젝트 A가 requests 2.x를, 프로젝트 B가 requests 3.x를 사용할 때 충돌 없이 관리할 수 있습니다. python -m venv .venv로 생성하고 활성화하면 pip install이 전역이 아닌 가상 환경에 설치됩니다. requirements.txt나 pyproject.toml(Poetry)로 의존성을 기록하여 동료나 서버에서 동일 환경을 재현합니다. Docker 컨테이너를 사용하는 경우에도 이미지 내 별도 가상 환경을 구성하는 것이 패키지 격리에 유리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Django의 signals 기본 개념을 설명해주세요.', 'DJANGO',
 'Django Signals는 특정 이벤트 발생 시 연결된 핸들러 함수를 자동 호출하는 옵저버 패턴 구현입니다. post_save, pre_save, post_delete, pre_delete, m2m_changed 등의 내장 시그널이 있으며, Signal() 클래스로 커스텀 시그널도 정의할 수 있습니다. @receiver(post_save, sender=User) 데코레이터로 핸들러를 등록하면 User 저장 후 자동으로 Profile 생성 등의 작업을 수행합니다. 시그널은 테스트 어려움과 암시적 동작으로 디버깅이 어려울 수 있으므로 명시적 서비스 계층 호출이 더 권장되는 경우도 많습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Python의 람다(lambda) 함수와 일반 함수의 차이는?', 'PYTHON_CORE',
 'lambda 함수는 이름 없는 익명 함수로 lambda 인자: 표현식 형태로 단일 표현식만 포함할 수 있습니다. 일반 def 함수와 달리 문(statement)을 포함할 수 없고, 반환값이 항상 표현식 평가 결과입니다. sorted(), map(), filter() 등 고차 함수에 간단한 변환 로직을 전달할 때 유용합니다. 단, 복잡한 로직에 lambda를 남용하면 가독성이 떨어지므로 간단한 경우에만 사용하고 재사용이 필요하거나 복잡한 경우 def로 정의합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:JUNIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Django에서 인증과 세션 관리는 어떻게 이루어지나요?', 'DJANGO',
 'Django는 세션 기반 인증을 기본으로 제공하며, django.contrib.auth와 django.contrib.sessions가 핵심 모듈입니다. login() 함수로 세션에 사용자 정보를 저장하고, logout()으로 세션을 삭제합니다. @login_required 데코레이터나 LoginRequiredMixin으로 뷰의 접근을 인증된 사용자로 제한합니다. REST API 개발 시에는 세션 대신 JWT(djangorestframework-simplejwt) 또는 Token 인증(DRF의 TokenAuthentication)을 사용하는 것이 stateless 설계에 적합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- ============================================================
-- MID (30문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Python의 GIL을 우회하는 방법과 멀티프로세싱 접근 방법을 설명해주세요.', 'PYTHON_CORE',
 'GIL은 스레드 병렬성을 차단하므로 CPU 바운드 작업의 병렬화에는 multiprocessing 모듈로 별도 프로세스를 생성하는 것이 효과적입니다. 각 프로세스는 별도의 GIL을 가져 진정한 병렬 실행이 가능하지만 프로세스 간 통신(Queue, Pipe, Manager)과 메모리 오버헤드가 존재합니다. ProcessPoolExecutor로 멀티프로세싱을 간편하게 활용할 수 있으며, concurrent.futures 인터페이스로 ThreadPoolExecutor와 동일하게 사용합니다. I/O 바운드 작업은 asyncio나 ThreadPoolExecutor, CPU 바운드는 ProcessPoolExecutor가 적합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', '파라미터를 받는 데코레이터(Parameterized Decorator)를 어떻게 구현하나요?', 'PYTHON_CORE',
 '파라미터 데코레이터는 데코레이터를 반환하는 함수(데코레이터 팩토리)를 만들어 구현합니다. 외부 함수가 파라미터를 받고, 내부 함수가 데코레이터이며, 가장 안쪽 함수가 실제 래퍼(wrapper)입니다. @retry(max_attempts=3, delay=1.0) 같은 형태로 사용하며, 파라미터는 클로저를 통해 내부 함수에서 접근합니다. functools.wraps로 원본 함수 메타데이터를 보존하는 것이 필수입니다. 클래스로 데코레이터를 구현하면(__call__ 메서드 활용) 상태를 더 명확하게 관리할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Django Middleware의 동작 방식과 커스텀 미들웨어 작성 방법을 설명해주세요.', 'DJANGO',
 'Django Middleware는 요청(request)과 응답(response) 처리 파이프라인의 중간 처리 계층으로, settings.py의 MIDDLEWARE 리스트 순서대로 적용됩니다. 요청 시에는 리스트 상단에서 하단 순서로, 응답 시에는 하단에서 상단 순서로 처리됩니다. 커스텀 미들웨어는 __init__(get_response)와 __call__(request) 메서드를 구현하는 클래스로 작성하며, request/response 처리 전후에 로직을 삽입합니다. 인증 토큰 검증, 요청 로깅, 속도 제한, 요청 ID 추적 등에 미들웨어를 활용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Django ORM의 QuerySet Lazy Evaluation과 평가 시점을 상세히 설명해주세요.', 'ORM',
 'QuerySet은 생성, 필터, 체이닝 시에는 SQL이 실행되지 않고, 실제 데이터가 필요한 시점에만 DB를 조회하는 Lazy Evaluation을 사용합니다. 평가 시점은 반복(for loop), 슬라이싱([0:5]), bool 변환(if qs:), len(), list(), repr() 등이며, 평가 후에는 결과를 캐싱합니다. 같은 QuerySet을 두 번 순회하면 첫 평가 시 캐싱된 결과를 사용하지만, 필터를 새로 추가하면 새 QuerySet이 생성됩니다. 이 특성을 이해해야 N+1 문제 예방과 쿼리 최적화 타이밍을 적절히 설계할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Django의 select_related와 prefetch_related의 차이와 사용 시나리오는?', 'ORM',
 'select_related는 SQL JOIN을 사용하여 ForeignKey 또는 OneToOne 관계의 연관 객체를 단일 쿼리로 가져옵니다. prefetch_related는 별도 쿼리를 실행하고 Python 레벨에서 연관 객체를 결합하며, ManyToMany 및 reverse ForeignKey(related_manager)에 사용합니다. select_related는 1:1 또는 N:1 관계에서 JOIN이 효율적이지만, ManyToMany에서 JOIN하면 결과 행이 곱으로 증가할 수 있어 prefetch_related가 적합합니다. Prefetch 객체를 사용하면 prefetch 시 적용할 필터나 정렬을 커스터마이징할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Django의 Signal 패턴과 실제 서비스에서의 트레이드오프를 설명해주세요.', 'DJANGO',
 'Signal은 모델 저장/삭제 이벤트에 반응하여 후처리를 분리하는 편리한 메커니즘이지만, 암시적으로 실행되어 코드 흐름 추적이 어렵고 테스트 복잡성이 증가합니다. 여러 Signal이 연쇄적으로 연결되면 예상치 못한 부작용이 발생할 수 있으며, 비동기 처리가 기본이 아니므로 무거운 작업을 Signal에 넣으면 응답 시간이 증가합니다. 명시적 Service 계층에서 메서드를 직접 호출하는 방식이 테스트 용이성과 코드 가독성 측면에서 종종 더 나으며, Signal은 앱 간 결합을 줄여야 하는 경우나 서드파티 앱 확장에 적합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'FastAPI의 BackgroundTasks와 Celery의 차이와 선택 기준은?', 'FASTAPI',
 'BackgroundTasks는 FastAPI에 내장된 경량 비동기 처리로, HTTP 응답을 반환한 후 같은 프로세스에서 태스크를 실행합니다. 재시도, 태스크 큐 모니터링, 스케줄링이 없으며 서버 재시작 시 실행 중인 태스크가 유실될 수 있습니다. Celery는 분산 태스크 큐로 Redis/RabbitMQ 브로커를 사용하여 별도 워커 프로세스에서 실행하고, 재시도, 태스크 결과 저장, 주기적 태스크(Celery Beat), 모니터링(Flower)을 지원합니다. 이메일 발송 같은 단순 처리는 BackgroundTasks로 충분하지만, 실패 복구가 필요하거나 태스크 볼륨이 큰 경우 Celery를 선택합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Python의 동시성과 병렬성의 차이를 설명하고, 각 모듈을 비교해주세요.', 'ASYNC',
 '동시성(Concurrency)은 여러 작업이 겹쳐서 진행되는 것처럼 보이는 개념(시분할)이고, 병렬성(Parallelism)은 여러 작업이 물리적으로 동시에 실행되는 것입니다. Python에서 asyncio는 단일 스레드 이벤트 루프 기반 동시성으로 I/O 바운드 작업에 효율적입니다. threading은 OS 스레드를 사용하지만 GIL로 CPU 병렬 실행이 제한되며 I/O 바운드에 적합합니다. multiprocessing은 별도 프로세스를 생성하여 진정한 병렬 실행이 가능하므로 CPU 바운드에 적합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Pydantic v2의 주요 변경사항과 모델 유효성 검증 방법을 설명해주세요.', 'FASTAPI',
 'Pydantic v2는 Rust로 재작성된 core 라이브러리로 v1 대비 5~50배 성능이 향상됐습니다. model_validator와 field_validator 데코레이터 API가 변경됐으며, @validator(deprecated)에서 @field_validator로 마이그레이션이 필요합니다. model_config = ConfigDict(...)로 설정을 정의하며, model_validate(), model_dump()가 dict()와 parse_obj()를 대체합니다. Discriminated Unions, Annotated 타입 활용, strict mode 등 강화된 타입 안전성이 특징입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Django에서 커스텀 Manager를 사용하는 이유와 방법은?', 'ORM',
 'Manager는 Model.objects와 같이 QuerySet을 반환하는 인터페이스로, 커스텀 Manager를 통해 모델별 공통 쿼리 로직을 캡슐화합니다. Manager에 커스텀 메서드를 추가하거나 get_queryset()을 오버라이드하여 기본 QuerySet에 항상 적용할 필터(예: 소프트 삭제 된 레코드 제외)를 선언합니다. 여러 Manager를 정의할 때 첫 번째로 선언된 Manager가 기본 Manager가 되며 마이그레이션 시 사용됩니다. 이를 통해 뷰나 API에서 반복되는 .filter(is_active=True) 같은 조건을 제거하고 책임을 모델 계층에 집중합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Python의 메타클래스(Metaclass)란 무엇이며 어떻게 활용되나요?', 'PYTHON_CORE',
 '메타클래스는 "클래스의 클래스"로, 클래스 객체가 생성되는 방식을 제어합니다. Python에서 기본 메타클래스는 type이며, type(name, bases, namespace)로 동적으로 클래스를 생성합니다. class Meta: 또는 metaclass=MyMeta로 커스텀 메타클래스를 지정하면 __new__나 __init_subclass__에서 클래스 생성을 가로채 속성 추가, 메서드 검증, 레지스트리 등록 등을 자동화합니다. Django ORM의 Model 클래스가 메타클래스를 활용하여 필드 정의를 DB 스키마로 변환하는 대표적인 예입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'FastAPI에서 OAuth2 + JWT 인증을 구현하는 방법을 설명해주세요.', 'FASTAPI',
 'FastAPI에는 OAuth2PasswordBearer와 OAuth2PasswordRequestForm이 내장되어 있어 JWT 인증을 손쉽게 구현할 수 있습니다. /token 엔드포인트에서 자격증명 확인 후 python-jose로 JWT를 발급하고, 보호 엔드포인트에서 Depends(oauth2_scheme)로 토큰을 추출하여 검증합니다. get_current_user 의존성 함수를 Depends()로 주입하면 인증이 필요한 모든 엔드포인트에서 코드 중복 없이 사용자 정보를 받을 수 있습니다. 토큰 만료, 알고리즘(HS256/RS256), Secret Key 관리 등 보안 요소를 환경변수로 분리해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Django에서 캐싱 전략과 Redis 활용 방법을 설명해주세요.', 'DJANGO',
 'Django의 캐시 프레임워크는 메모리, 파일시스템, Memcached, Redis 등 다양한 백엔드를 지원하며 settings.py의 CACHES에 설정합니다. cache.set(key, value, timeout), cache.get(key) API로 직접 캐시를 제어하거나, @cache_page(60*15) 데코레이터로 뷰 응답 전체를 캐싱합니다. Django REST Framework와 함께 cache.get_or_set() 패턴으로 QuerySet 캐시를 구현하고, 데이터 변경 시 cache.delete() 또는 cache.delete_pattern()으로 무효화합니다. django-redis를 사용하면 Redis 고급 기능(TTL 조회, 원자 연산)도 활용 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Python에서 소프트 삭제(Soft Delete) 패턴을 구현하는 방법은?', 'ORM',
 '소프트 삭제는 레코드를 실제 삭제하는 대신 deleted_at timestamp 또는 is_deleted boolean 컬럼을 true로 마킹하여 논리적으로 삭제하는 패턴입니다. Django에서는 커스텀 Manager의 get_queryset()에 filter(deleted_at__isnull=True)를 적용하여 일반 쿼리에서 삭제된 레코드를 자동 제외합니다. Model의 delete() 메서드를 오버라이드하여 실제 삭제 대신 deleted_at을 설정하고, 감사 로그나 규정 준수 요구사항에 대응합니다. 복원 기능(restore)과 완전 삭제(hard_delete) 메서드도 함께 제공합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Python의 asyncio.gather와 asyncio.create_task의 차이를 설명해주세요.', 'ASYNC',
 'asyncio.gather(*coros)는 여러 코루틴을 동시에 실행하고 모두 완료될 때까지 대기하며, 결과 리스트를 입력 순서대로 반환합니다. asyncio.create_task()는 코루틴을 태스크로 즉시 예약하되 완료를 기다리지 않으며, 나중에 await task나 gather로 결과를 수집합니다. create_task는 태스크 취소(task.cancel()), 완료 콜백 추가(add_done_callback)가 가능합니다. gather에서 하나의 코루틴이 예외를 던지면 다른 코루틴도 취소되므로(return_exceptions=True로 변경 가능), 독립적 실패 처리가 필요하면 개별 task에 try/except를 적용해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Django의 커스텀 User 모델을 설계할 때 고려사항은 무엇인가요?', 'DJANGO',
 '프로젝트 시작 시 AbstractBaseUser 또는 AbstractUser를 상속하여 커스텀 User 모델을 정의하는 것이 강력히 권장됩니다. 프로젝트 중간에 기본 User 모델을 변경하면 마이그레이션이 매우 복잡해집니다. AbstractUser는 기본 필드를 모두 유지하면서 추가 필드만 더할 때, AbstractBaseUser는 완전히 새로운 인증 시스템이 필요할 때 사용합니다. AUTH_USER_MODEL = ''accounts.User''를 settings.py에 설정해야 하며, get_user_model()로 모델을 참조하여 교체 가능성을 유지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Python의 데이터클래스(dataclass)와 Pydantic 모델의 차이점은?', 'PYTHON_CORE',
 '@dataclass는 Python 3.7+ 표준 라이브러리로 __init__, __repr__, __eq__를 자동 생성하는 간단한 데이터 컨테이너입니다. 타입 어노테이션을 사용하지만 런타임 타입 검증이 없습니다. Pydantic BaseModel은 타입 힌트 기반 런타임 검증과 직렬화/역직렬화를 제공하며, 외부 입력 검증(API 요청, 설정 파일)에 적합합니다. FastAPI와 결합 시 자동 OpenAPI 문서 생성과 요청 검증을 제공합니다. 도메인 내부 순수 데이터 구조에는 dataclass, API 경계의 입출력 스키마에는 Pydantic을 사용하는 것이 일반적입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'FastAPI에서 WebSocket을 구현하는 방법을 설명해주세요.', 'FASTAPI',
 'FastAPI에서 @app.websocket(''/ws'') 데코레이터와 WebSocket 타입 파라미터로 WebSocket 엔드포인트를 선언합니다. websocket.accept()로 연결을 수락하고, receive_text()/receive_json()으로 메시지를 받으며, send_text()/send_json()으로 메시지를 전송합니다. WebSocketDisconnect 예외를 처리하여 클라이언트 연결 해제를 감지합니다. 여러 클라이언트에게 브로드캐스트하려면 ConnectionManager 클래스를 만들어 활성 연결 목록을 관리합니다. Starlette 기반이므로 일반 HTTP와 WebSocket을 동일한 서버에서 처리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Django의 F() 표현식과 Q() 표현식을 언제 사용하나요?', 'ORM',
 'F() 표현식은 Python으로 값을 가져오지 않고 DB 컬럼 값을 직접 참조하여 SQL에서 연산을 수행합니다. 조회수 증가 같은 연산에 F(''views'') + 1을 사용하면 레이스 컨디션 없이 원자적으로 업데이트됩니다. Q() 표현식은 ORM 쿼리에서 OR(|), AND(&), NOT(~) 복합 조건을 표현합니다. 예를 들어 filter(Q(status=''A'') | Q(status=''B''))처럼 기본 AND만 지원하는 filter()에서 OR 조건을 구성하거나, 동적으로 조건을 조합할 때 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Python의 메모리 관리와 참조 카운팅(Reference Counting) 방식을 설명해주세요.', 'PYTHON_CORE',
 'CPython은 각 객체에 참조 카운트를 유지하며, 카운트가 0이 되면 즉시 메모리를 해제합니다. sys.getrefcount()로 현재 참조 수를 확인할 수 있습니다. 순환 참조(A가 B를 참조하고 B가 A를 참조)는 참조 카운팅으로 해제되지 않으므로, Python의 가비지 컬렉터(gc 모듈)가 주기적으로 순환 참조를 탐지하고 해제합니다. weakref 모듈로 약한 참조를 사용하면 순환 참조 없이 객체를 참조할 수 있습니다. 대규모 데이터 처리 시 del 키워드와 gc.collect()로 명시적 메모리 해제를 유도할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Django에서 트랜잭션 처리 방법을 설명해주세요.', 'DJANGO',
 'Django에서 transaction.atomic()은 블록 내 모든 DB 작업을 하나의 트랜잭션으로 처리합니다. 데코레이터(@transaction.atomic)나 컨텍스트 매니저(with transaction.atomic():)로 사용하며, 예외 발생 시 블록 전체가 롤백됩니다. 중첩 transaction.atomic()은 savepoint를 생성하여 부분 롤백을 지원합니다. on_commit() 콜백으로 트랜잭션 커밋 후에 이메일 발송이나 캐시 무효화 같은 작업을 실행하여, 롤백 시 불필요한 부작용을 방지합니다. 데이터베이스 기본값은 AUTOCOMMIT이므로, 여러 쿼리를 원자적으로 처리하려면 반드시 atomic()을 명시해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- ============================================================
-- SENIOR (30문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'asyncio 이벤트 루프의 내부 동작 원리를 설명해주세요.', 'ASYNC',
 '이벤트 루프는 완료 가능한 I/O 이벤트를 등록하고(selector 기반), 실행 가능한 코루틴들을 큐에서 꺼내 실행하는 단일 스레드 기반의 태스크 스케줄러입니다. I/O 작업(소켓 읽기/쓰기) 시 코루틴은 Future를 await하며 일시 중단하고, OS의 epoll/kqueue/select를 통해 I/O 완료 통보를 받으면 해당 코루틴을 다시 큐에 넣습니다. 이 협력적 멀티태스킹 방식은 스레드 전환 오버헤드 없이 수만 개의 동시 연결을 처리할 수 있습니다. uvloop는 libuv 기반의 drop-in 교체재로 asyncio 기본 루프보다 2~4배 빠른 성능을 제공합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Python 3.13 Free-Threading(GIL 없는 CPython) 실험적 기능과 영향은?', 'PYTHON_CORE',
 'Python 3.13에서 PEP 703으로 --disable-gil 옵션의 실험적 Free-Threading 빌드가 도입됐습니다. GIL 제거 시 멀티스레드에서 CPU 바운드 작업의 진정한 병렬 실행이 가능해지지만, 참조 카운팅이 스레드 안전하지 않으므로 원자적 연산으로 교체되어 단일 스레드 성능이 다소 저하됩니다. 기존 C 확장 라이브러리들이 스레드 안전하지 않을 수 있어 호환성 검증이 필요하며, Python 3.14에서 정식 지원 예정입니다. 이 변화가 현실화되면 asyncio 대신 threading 모듈로 CPU 병렬화가 가능해져 Python 비동기 생태계에 근본적 변화가 예상됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Django에서 대규모 데이터셋의 쿼리를 최적화하는 고급 기법을 설명해주세요.', 'ORM',
 'values()와 values_list()로 필요한 컬럼만 SELECT하여 ORM 객체 생성 오버헤드를 제거합니다. iterator()를 사용하면 QuerySet 캐싱 없이 서버사이드 커서로 메모리 효율적으로 대용량 데이터를 처리합니다. only()와 defer()로 로딩할 필드를 제어하고, annotate()와 aggregate()로 집계를 DB에서 처리합니다. 복잡한 쿼리는 ORM 대신 connection.cursor()로 raw SQL을 실행하고, 대규모 삽입은 bulk_create(objs, batch_size=1000)를 활용합니다. django-debug-toolbar로 개발 중 쿼리 수와 실행 시간을 실시간으로 분석합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'FastAPI 기반 마이크로서비스 아키텍처 설계 시 고려사항은?', 'FASTAPI',
 '각 서비스는 독립적으로 배포 가능하도록 DB를 분리하고, 서비스 간 통신은 동기 HTTP(httpx)나 비동기 메시지(Kafka/RabbitMQ)로 처리합니다. FastAPI의 APIRouter로 도메인별 라우터를 모듈화하고, Depends()로 서비스 간 공통 인증/인가 로직을 공유합니다. 서비스 디스커버리는 Consul이나 K8s Service를 활용하고, API Gateway(Kong, Traefik)로 Rate Limiting, JWT 검증, 로깅을 중앙화합니다. 각 서비스의 FastAPI 앱에 /health와 /metrics 엔드포인트를 표준화하여 모니터링 플랫폼과 통합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Python에서 Protocol과 ABC(Abstract Base Class)의 차이와 사용 시나리오는?', 'PYTHON_CORE',
 'ABC는 명시적 상속을 요구하여 인터페이스를 구현하고, @abstractmethod로 반드시 구현해야 할 메서드를 강제합니다. Protocol(Python 3.8+, PEP 544)은 구조적 서브타이핑(덕 타이핑)을 지원하여, 상속 없이도 필요한 메서드/속성을 가진 클래스는 호환됩니다. Protocol은 기존 라이브러리 타입과 통합 시 상속 없이도 타입 호환성을 선언할 수 있어 유연합니다. 런타임 타입 체크가 필요하면 ABC, 정적 분석 도구(mypy)를 통한 타입 안전성만 필요하면 Protocol이 더 유연합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Celery의 작업 실패 처리, 재시도, 멱등성 설계 방법은?', 'DJANGO',
 '@celery.task(bind=True, max_retries=3, default_retry_delay=60)로 재시도 설정을 선언하고, 예외 처리에서 self.retry(exc=e, countdown=backoff)로 지수 백오프 재시도를 구현합니다. 멱등성 보장을 위해 태스크 ID나 DB 유니크 제약을 활용하여 중복 실행이 동일 결과를 내도록 설계합니다. 최종 실패한 태스크는 Dead Letter Queue로 이동하거나 DB에 실패 상태를 기록합니다. Celery Chord/Chain/Group으로 복잡한 태스크 워크플로우를 구성하고, Flower나 Prometheus exporter로 태스크 성공/실패율을 모니터링합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Django에서 멀티 테넌시(Multi-Tenancy) 아키텍처를 구현하는 방법은?', 'DJANGO',
 '멀티 테넌시 구현 방식은 세 가지입니다. 첫째, 공유 스키마에 tenant_id 컬럼을 추가하는 방식은 간단하지만 실수로 테넌트 데이터가 섞일 위험이 있습니다. 둘째, PostgreSQL 스키마 분리(django-tenant-schemas)는 각 테넌트가 별도 스키마를 가져 격리성이 높고 DB 레벨 분리가 명확합니다. 셋째, 별도 DB 인스턴스 방식은 가장 강한 격리를 제공하지만 관리 비용이 높습니다. django-tenants 라이브러리가 도메인 기반 테넌트 식별과 스키마 자동 전환을 지원합니다. 어떤 방식이든 테넌트 ID 검증 미들웨어로 요청마다 테넌트를 식별하고 컨텍스트로 전파합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Python의 디스크립터(Descriptor) 프로토콜을 설명해주세요.', 'PYTHON_CORE',
 '디스크립터는 __get__, __set__, __delete__ 중 하나 이상을 구현한 객체로, 클래스 속성으로 사용될 때 속성 접근을 가로채는 프로토콜입니다. @property, @classmethod, @staticmethod가 모두 내부적으로 디스크립터로 구현됩니다. __set__을 구현한 데이터 디스크립터는 인스턴스 __dict__보다 우선순위가 높고, __get__만 구현한 비데이터 디스크립터는 __dict__가 우선입니다. Django ORM의 Field 클래스가 디스크립터를 사용하여 model.field 접근 시 DB 컬럼 값 조회를 가로채는 것이 대표적 활용 예입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'FastAPI와 SQLAlchemy 2.0 async ORM을 통합할 때의 패턴과 세션 관리 방법은?', 'FASTAPI',
 'SQLAlchemy 2.0의 AsyncSession과 create_async_engine을 AsyncGenerator 기반 의존성으로 구현하여 FastAPI Depends()에 주입합니다. async with AsyncSession() 컨텍스트 매니저로 요청당 세션 생명주기를 관리하고, yield를 사용하는 get_db() 의존성에서 예외 시 rollback, 정상 시 commit을 보장합니다. selectinload, joinedload로 비동기 Eager Loading을 설정하고, N+1 문제를 방지합니다. Connection Pool 설정(pool_size, max_overflow, pool_timeout)을 적절히 조정하여 데이터베이스 연결 고갈을 방지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Python 기반 고성능 API 서버의 배포 스택 최적화 방법은?', 'FASTAPI',
 'Uvicorn(ASGI 서버) + Gunicorn(프로세스 매니저) 조합으로 멀티 워커를 운영하며, --workers는 CPU 코어 수 × 2 + 1이 일반적입니다. uvloop을 이벤트 루프로 설정하여 I/O 처리 속도를 향상시킵니다. 응답 압축(GZipMiddleware), HTTP/2, Connection Keep-Alive 설정으로 네트워크 오버헤드를 줄입니다. Redis를 활용한 응답 캐싱과 Rate Limiting을 API Gateway 레벨에서 구현하고, CDN으로 정적 컨텐츠를 오프로딩합니다. Prometheus + Grafana로 RPS, 지연시간 p95/p99, 에러율을 모니터링하며 병목 지점을 조기 식별합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Python에서 이벤트 소싱(Event Sourcing) 패턴을 구현하는 방법은?', 'PYTHON_CORE',
 '이벤트 소싱은 상태 변경을 이벤트(불변 로그)로 기록하고, 이벤트를 순서대로 재생하여 현재 상태를 도출하는 패턴입니다. Python에서 Pydantic으로 이벤트 스키마를 정의하고, PostgreSQL의 JSONB 컬럼이나 전용 이벤트 스토어(EventStoreDB)에 이벤트를 추가만 합니다(append-only). Aggregate는 이벤트 목록을 받아 apply(event) 메서드로 상태를 재구성하며, Snapshot으로 재생 성능을 최적화합니다. 이벤트 스트림을 Kafka로 발행하면 다른 서비스에서 이벤트를 구독하여 읽기 모델(Read Model)을 독립적으로 구성하는 CQRS와 자연스럽게 결합됩니다.',
 'MODEL_ANSWER', TRUE, NOW());
