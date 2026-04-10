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

-- 추가 27문항 (MID +9, SENIOR +18)

-- MID 추가 9문항

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Django REST Framework에서 커스텀 Serializer 검증 로직을 구현하는 방법은?', 'DJANGO',
 'validate_<field_name>(self, value) 메서드로 특정 필드의 개별 검증을, validate(self, data) 메서드로 여러 필드 간 상호 의존적 검증을 구현합니다. 검증 실패 시 serializers.ValidationError를 raise하면 DRF가 400 응답으로 자동 변환합니다. Serializer에 validators=[UniqueValidator(queryset=User.objects.all())]처럼 필드 수준 Validator를 선언적으로 추가합니다. UniqueTogetherValidator로 복합 유니크 제약을 표현하고, 커스텀 Validator 클래스를 __call__ 메서드로 구현하면 재사용 가능한 검증 로직을 만들 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Python의 asyncio에서 Task와 gather, wait의 차이점을 설명해주세요.', 'ASYNC',
 'asyncio.create_task()는 코루틴을 즉시 이벤트 루프에 스케줄링하며 Task 객체를 반환합니다. asyncio.gather(*coros)는 여러 코루틴을 동시에 실행하고 모든 결과를 리스트로 반환하며, return_exceptions=True로 예외를 결과로 처리합니다. asyncio.wait(tasks)는 FIRST_COMPLETED, FIRST_EXCEPTION, ALL_COMPLETED 중 완료 조건을 선택할 수 있어 일부 완료 시 처리를 시작하는 경우에 적합합니다. TaskGroup(Python 3.11+)은 구조화된 동시성을 지원하여 그룹 내 작업 중 하나라도 실패하면 나머지를 취소하고 예외를 전파합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'FastAPI의 Dependency Injection 시스템을 심층적으로 설명해주세요.', 'FASTAPI',
 'FastAPI의 Depends()는 의존성 함수를 라우터 파라미터에 선언하면 프레임워크가 자동으로 실행하고 결과를 주입합니다. yield를 사용하는 의존성은 컨텍스트 매니저처럼 동작하여 요청 전후 리소스를 초기화/정리합니다. 의존성은 중첩 가능하며, 같은 요청 내에서 동일 의존성 인스턴스는 기본적으로 한 번만 실행됩니다(캐싱). use_cache=False로 매 주입마다 새로 실행하도록 강제할 수 있습니다. @app.dependency_overrides를 활용하면 테스트에서 실제 DB 의존성을 인메모리 구현으로 교체하여 격리된 테스트를 구성합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Django의 signals(신호)는 무엇이며 언제 사용해야 하나요?', 'DJANGO',
 'Django signals는 특정 동작(모델 저장, 삭제, 요청 처리)이 발생했을 때 연결된 리시버 함수를 호출하는 발행-구독 메커니즘입니다. post_save, pre_delete, m2m_changed, request_started 같은 내장 시그널이 있으며, Signal()로 커스텀 시그널을 정의합니다. @receiver(post_save, sender=User) 데코레이터로 리시버를 등록하고 AppConfig.ready()에서 임포트하여 등록이 실행되도록 합니다. 트랜잭션 외부에서 실행되는 post_save와 달리 side effect(이메일, 캐시 갱신)는 transaction.on_commit()에서 처리하는 것이 안전합니다. 과도한 시그널 사용은 코드 흐름을 추적하기 어렵게 하므로 명시적 서비스 레이어 호출을 우선 검토합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Python에서 컨텍스트 매니저(Context Manager)를 직접 구현하는 방법은?', 'PYTHON_CORE',
 '컨텍스트 매니저는 __enter__와 __exit__ 메서드를 구현한 클래스 또는 @contextmanager 데코레이터를 사용한 제너레이터 함수로 만듭니다. __exit__(self, exc_type, exc_val, exc_tb)에서 예외를 처리하고 True를 반환하면 예외가 억제됩니다. @contextmanager는 yield 이전이 __enter__, 이후가 __exit__에 해당하며 try-finally로 정리 로직을 보장합니다. DB 연결, 파일, 락(Lock), 트랜잭션 등 명확한 시작-종료가 있는 리소스 관리에 활용합니다. contextlib.suppress()로 특정 예외를 무시하는 컨텍스트 매니저를 간단히 생성하고, contextlib.ExitStack으로 동적 개수의 컨텍스트 매니저를 관리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Django의 ORM에서 select_related와 prefetch_related의 차이와 사용 시나리오는?', 'ORM',
 'select_related()는 SQL JOIN으로 ForeignKey 및 OneToOne 관계를 한 번의 쿼리로 가져옵니다. 관계 객체가 하나인 경우에 적합하며, depth 파라미터로 중첩 관계 깊이를 제어합니다. prefetch_related()는 별도 쿼리를 추가 실행하여 ManyToMany 및 역방향 ForeignKey 관계를 가져오고 Python에서 조인합니다. Prefetch(''orders'', queryset=Order.objects.filter(status=''active''))로 사전 로딩 쿼리를 커스터마이즈합니다. 관계 깊이가 깊거나 레코드 수가 많은 ManyToMany에는 prefetch_related가, 단일 FK 관계에는 select_related가 일반적으로 더 효율적입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Pydantic v2의 주요 변경 사항과 FastAPI에서의 활용 방법은?', 'FASTAPI',
 'Pydantic v2는 Rust로 구현된 core validator로 v1 대비 5~50배 빠른 검증 속도를 제공합니다. model_validator(mode=''before''/''after'')로 모델 전체에 적용되는 검증 로직을 정의하고, field_validator()로 필드 수준 검증을 처리합니다. model_config = ConfigDict(strict=True)로 엄격한 타입 검사를 활성화하고 암묵적 형변환을 방지합니다. computed_field 데코레이터로 검증 후 계산 결과를 필드처럼 직렬화합니다. model_serializer로 직렬화 동작을 전면 커스터마이즈하고, model_dump(mode=''json'')으로 JSON 직렬화 가능한 딕셔너리를 반환합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Python에서 멀티프로세싱(multiprocessing)과 멀티스레딩(threading)의 차이와 선택 기준은?', 'PYTHON_CORE',
 'GIL로 인해 CPython의 멀티스레딩은 I/O 바운드 작업에서는 병렬 실행이 가능하지만, CPU 바운드 작업에서는 한 번에 하나의 스레드만 실행됩니다. multiprocessing 모듈은 별도 프로세스로 GIL을 피해 CPU 바운드 작업의 진정한 병렬 실행을 지원합니다. ProcessPoolExecutor와 ThreadPoolExecutor를 concurrent.futures 인터페이스로 동일하게 사용하여 필요 시 교체합니다. 프로세스 간 데이터 공유는 Queue, Pipe, 공유 메모리(SharedMemory)를 사용합니다. NumPy 연산은 내부적으로 GIL을 해제하므로 멀티스레딩으로도 병렬 실행이 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:MID:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Django의 미들웨어(Middleware) 동작 원리와 커스텀 미들웨어 작성 방법은?', 'DJANGO',
 'Django 미들웨어는 요청-응답 처리 파이프라인에서 각 레이어가 순서대로 적용되는 훅 시스템입니다. 미들웨어 클래스는 __init__(get_response)으로 초기화하고, __call__(request)에서 get_response(request) 전후에 처리 로직을 구현합니다. process_view, process_exception, process_template_response 훅으로 특정 단계에 개입할 수 있습니다. MIDDLEWARE 설정의 순서가 중요하며, 요청 시에는 위에서 아래로, 응답 시에는 아래에서 위로 적용됩니다. 인증 컨텍스트 주입, 요청 로깅, CORS 처리, 테넌트 식별 등에 미들웨어를 활용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- SENIOR 추가 18문항

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Django에서 대규모 트래픽을 처리하기 위한 수평 확장 전략을 설명해주세요.', 'DJANGO',
 'Django 애플리케이션을 stateless하게 유지하여 여러 인스턴스가 동일하게 동작하도록 세션을 Redis에 저장하고 미디어 파일을 S3로 오프로딩합니다. Gunicorn 워커를 CPU 코어 × 2 + 1로 설정하고, gevent 또는 uvicorn worker로 비동기 처리를 지원합니다. 읽기 트래픽은 DB Read Replica로 분산하고, django-db-router로 쿼리 타입에 따라 마스터/레플리카를 선택합니다. Memcached 또는 Redis 캐시를 뷰 레벨(@cache_page), 템플릿 단편, 쿼리셋 레벨로 계층화하여 DB 부하를 줄입니다. nginx의 upstream 로드 밸런싱과 Django의 ALLOWED_HOSTS 설정으로 여러 인스턴스에 트래픽을 분배합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Python에서 타입 힌트와 mypy를 활용한 정적 타입 검사 전략은?', 'PYTHON_CORE',
 'mypy의 strict 모드(--strict)는 disallow_untyped_defs, disallow_any_generics, warn_return_any 등을 활성화하여 타입 커버리지를 최대화합니다. TypeVar와 Generic[T]로 재사용 가능한 제네릭 클래스를 정의합니다. TypedDict로 딕셔너리 키-값 타입을 선언하고, Literal 타입으로 허용 값을 제한합니다. Protocol로 구조적 서브타이핑을 활용하여 상속 없이 덕 타이핑을 타입 안전하게 표현합니다. overload 데코레이터로 동일 함수의 다양한 시그니처를 선언하고, TYPE_CHECKING 블록으로 순환 임포트를 방지합니다. CI에서 mypy --check-untyped-defs를 실행하여 타입 오류를 조기에 차단합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'FastAPI에서 백그라운드 태스크와 Celery의 역할을 비교하고 선택 기준을 설명해주세요.', 'FASTAPI',
 'FastAPI의 BackgroundTasks는 응답 반환 후 동일 프로세스에서 간단한 부작용(이메일 발송, 로그 기록)을 비동기로 실행합니다. 재시도, 스케줄링, 분산 실행, 태스크 모니터링이 필요한 경우 Celery가 적합합니다. BackgroundTasks는 설정이 거의 없고 즉시 사용 가능하지만, 프로세스 재시작 시 실행 중인 작업이 손실됩니다. Celery는 브로커(Redis/RabbitMQ)에 태스크를 영속화하여 Worker 장애 시에도 재시도합니다. FastAPI + arq(asyncio 기반 태스크 큐)는 Celery보다 경량이면서도 영속성을 지원하는 중간 선택지입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Python의 메타클래스(Metaclass)를 활용한 프레임워크 설계 패턴은?', 'PYTHON_CORE',
 '메타클래스는 클래스의 클래스로, 클래스 생성 시점에 개입하여 클래스 구조를 수정하거나 메타 정보를 처리합니다. class MyModel(metaclass=ModelMeta)처럼 선언하면 클래스 정의 시 ModelMeta.__new__ 또는 __init_subclass__가 호출됩니다. Django ORM이 Meta 내부 클래스와 필드 디스크립터를 처리하는 핵심 메커니즘이 메타클래스입니다. __init_subclass__는 Python 3.6+에서 메타클래스 없이 서브클래스 등록 패턴을 구현하는 더 간단한 대안입니다. ABCMeta는 abstractmethod 검사를 메타클래스로 구현합니다. 메타클래스는 ORM, 직렬화 프레임워크, 플러그인 레지스트리 같은 프레임워크 인프라에서 주로 사용하고 일반 애플리케이션 코드에서는 남용을 피합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Django에서 Full-Text Search를 구현하는 방법과 ElasticSearch 연동 전략은?', 'DJANGO',
 'PostgreSQL 백엔드에서 django.contrib.postgres.search의 SearchVector와 SearchQuery로 전문 검색을 구현하고, SearchRank로 관련도 순 정렬을 합니다. SearchVectorField를 모델에 추가하고 post_save 시그널로 tsvector를 갱신하거나 DB 트리거를 사용합니다. 대규모 검색이 필요하면 elasticsearch-dsl-py(또는 elasticsearch-py)로 ES 인덱스와 Django 모델을 동기화합니다. django-elasticsearch-dsl은 @django_db_init_handler와 signal을 활용하여 모델 저장/삭제 시 ES 인덱스를 자동 갱신합니다. 검색 인덱스 동기화 지연이 허용되는 경우 Celery 태스크로 비동기 인덱싱하여 쓰기 성능을 보호합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Python에서 제너레이터와 이터레이터 프로토콜의 고급 활용 방법은?', 'PYTHON_CORE',
 '이터레이터 프로토콜은 __iter__()와 __next__()를 구현하며, StopIteration으로 종료를 알립니다. 제너레이터 함수는 yield로 이터레이터를 간결하게 구현합니다. yield from으로 서브 제너레이터에 위임하고 값을 투명하게 전달합니다. send()로 제너레이터에 값을 전달하는 코루틴 패턴을 구현하며, throw()로 예외를 주입합니다. itertools 모듈의 chain, islice, groupby, tee로 제너레이터를 조합하여 메모리 효율적인 데이터 파이프라인을 구성합니다. 대용량 DB 레코드를 처리할 때 QuerySet.iterator()와 제너레이터를 결합하면 메모리 사용량을 O(batch_size)로 유지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'FastAPI에서 OAuth2와 JWT를 활용한 완전한 인증/인가 시스템 설계 방법은?', 'FASTAPI',
 'OAuth2PasswordBearer와 OAuth2AuthorizationCodeBearer로 토큰 획득 흐름을 선언하면 Swagger UI에 자동으로 인증 UI가 생성됩니다. JWT Access Token은 짧은 만료 시간(15분)으로 설정하고, Refresh Token은 HttpOnly 쿠키에 장기 저장합니다. get_current_user() 의존성으로 토큰 검증과 사용자 조회를 처리하고, 권한 제어는 get_current_active_user(user=Depends(get_current_user)) 의존성 체이닝으로 확장합니다. 토큰 갱신 엔드포인트에서 Refresh Token을 검증하고 새 Access Token을 발급합니다. 토큰 블랙리스트를 Redis에 관리하여 로그아웃 시 즉각적인 토큰 무효화를 구현합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Python의 __slots__를 활용한 메모리 최적화와 성능 개선 방법은?', 'PYTHON_CORE',
 '__slots__ = [''x'', ''y'']를 클래스에 선언하면 인스턴스 __dict__ 대신 고정된 슬롯에 속성을 저장하여 인스턴스당 수백 바이트의 메모리를 절약합니다. 수백만 개의 인스턴스를 생성하는 경우 __slots__로 메모리 사용량을 40~50% 줄일 수 있습니다. 속성 접근이 __dict__ 조회보다 빠르므로 속성 접근이 빈번한 경우 성능도 향상됩니다. 상속 시 부모 클래스에 __dict__가 있으면 자식에서 __slots__를 선언해도 __dict__가 생성되므로 전체 계층에 __slots__를 적용해야 효과가 있습니다. dataclass와 __slots__를 결합하려면 @dataclass(slots=True)(Python 3.10+)를 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Django에서 GraphQL API를 Strawberry 또는 Graphene으로 구현하는 방법은?', 'DJANGO',
 'Strawberry는 Python 타입 힌트와 @strawberry.type, @strawberry.field 데코레이터로 GraphQL 스키마를 정의하는 현대적 라이브러리입니다. Django 통합을 위해 strawberry-graphql-django로 Django 모델에서 GraphQL 타입을 자동 생성합니다. N+1 문제 해결을 위해 Strawberry의 DataLoader를 사용하여 관계 조회를 배치 처리합니다. Graphene-Django는 DjangoObjectType으로 모델에서 타입을 자동 생성하지만, Strawberry가 타입 안전성과 코드 간결성이 더 높습니다. Mutation에서 input 타입과 payload 타입을 분리하고, Error Union으로 성공/실패를 타입 안전하게 표현합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Python 기반 서비스의 분산 추적(Distributed Tracing)과 OpenTelemetry 통합 방법은?', 'FASTAPI',
 'opentelemetry-sdk와 opentelemetry-instrumentation-fastapi로 FastAPI 애플리케이션의 HTTP 요청을 자동 계측합니다. opentelemetry-instrumentation-sqlalchemy, opentelemetry-instrumentation-redis로 DB와 캐시 호출을 추적합니다. Tracer를 서비스에 주입하여 with tracer.start_as_current_span(''operation-name'') as span: 블록으로 커스텀 스팬을 추가합니다. span.set_attribute()로 비즈니스 맥락(user_id, order_id)을 트레이스에 포함시킵니다. OTLP exporter로 Jaeger나 Tempo에 트레이스를 전송하고, 분산 트레이스로 마이크로서비스 간 요청 흐름과 병목을 분석합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Django의 커스텀 ORM Manager와 QuerySet으로 도메인 로직을 캡슐화하는 방법은?', 'ORM',
 '커스텀 Manager를 정의하여 자주 사용하는 필터 조건을 named 메서드로 제공합니다(예: Order.objects.pending()). QuerySet 메서드를 체이닝하여 빌더 패턴을 구현합니다. Manager와 QuerySet을 분리하여 Manager.from_queryset(CustomQuerySet)으로 두 레이어의 역할을 명확히 합니다. 소프트 삭제(soft delete)를 커스텀 Manager로 구현하면 모든 쿼리에 is_deleted=False 조건이 자동 적용됩니다. use_in_migrations = True를 Manager에 설정하면 마이그레이션에서도 커스텀 Manager를 사용합니다. 도메인 쿼리 로직을 View/Serializer가 아닌 Manager에 집중시켜 재사용성과 테스트 가능성을 높입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Python에서 함수형 프로그래밍 기법(함수 합성, 커링, 부분 적용)의 실용적 활용은?', 'PYTHON_CORE',
 'functools.partial()로 함수의 일부 인자를 고정한 새 함수를 생성하여 재사용 가능한 함수 특화 버전을 만듭니다. functools.reduce()와 map, filter를 조합하여 데이터 변환 파이프라인을 구성합니다. 함수 합성(compose)은 Python 표준에 없으므로 functools.reduce(lambda f, g: lambda *a: f(g(*a)), funcs)로 구현합니다. toolz 라이브러리는 curry(), compose(), pipe()로 함수형 프로그래밍을 지원합니다. Django ORM의 Q 객체 조합이나 복잡한 필터 조건 동적 생성에 함수형 접근을 적용하면 가독성이 높아집니다. 순수 함수(부작용 없음)로 비즈니스 로직을 작성하면 단위 테스트가 간결해집니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'FastAPI와 Redis를 활용한 실시간 알림 및 SSE(Server-Sent Events) 구현 방법은?', 'FASTAPI',
 'FastAPI에서 StreamingResponse와 async generator로 SSE 엔드포인트를 구현합니다. Redis Pub/Sub를 asyncio-redis 또는 aioredis로 구독하고, 새 메시지를 수신할 때마다 SSE 포맷(data: ...\n\n)으로 클라이언트에 스트리밍합니다. 클라이언트 연결 해제 시 asyncio.CancelledError를 처리하여 Redis 구독을 해제합니다. 각 사용자의 고유 채널(user:{user_id}:notifications)에 메시지를 발행하여 개인화된 알림을 전달합니다. Nginx에서 SSE를 프록시할 때 proxy_buffering off와 X-Accel-Buffering: no 헤더 설정이 필요합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Django 프로젝트의 CI/CD 파이프라인과 자동화 테스트 전략을 설명해주세요.', 'DJANGO',
 'pytest-django로 단위/통합 테스트를 작성하고, @pytest.fixture로 DB 픽스처를 관리합니다. factory_boy로 테스트 데이터를 동적 생성하여 픽스처 파일 유지 부담을 줄입니다. pytest-cov로 커버리지를 측정하고 80% 이상을 CI 합격 기준으로 설정합니다. GitHub Actions에서 테스트 DB를 서비스 컨테이너(PostgreSQL)로 구동하고, 병렬 테스트(pytest-xdist)로 실행 시간을 단축합니다. 스테이징 배포 후 Playwright 또는 httpx로 E2E 스모크 테스트를 자동 실행하여 배포 성공을 검증합니다. 블루-그린 배포로 새 버전을 검증 후 트래픽을 전환하고, Django의 check 프레임워크로 배포 전 설정 오류를 사전 탐지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Python 비동기 애플리케이션에서 데이터베이스 커넥션 풀 관리 전략은?', 'ASYNC',
 'asyncpg는 PostgreSQL용 고성능 비동기 드라이버로 연결 풀을 내장하며, min_size와 max_size로 풀 크기를 조정합니다. SQLAlchemy 2.0 async engine은 create_async_engine(url, pool_size=20, max_overflow=10)으로 커넥션 풀을 구성합니다. 요청당 세션 생명주기 관리를 FastAPI 의존성으로 구현하여 요청 완료 후 반드시 세션이 반환되도록 합니다. pool_pre_ping=True로 유휴 연결의 유효성을 사용 전에 검사하여 stale 연결 오류를 방지합니다. Kubernetes 환경에서 PgBouncer로 커넥션 풀링을 DB 앞단에서 처리하면 애플리케이션 인스턴스 수가 많아도 DB 연결 수를 제한할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Python에서 의존성 주입(Dependency Injection) 컨테이너를 구현하거나 활용하는 방법은?', 'PYTHON_CORE',
 'dependency-injector 라이브러리는 Container 클래스에 providers.Singleton, providers.Factory, providers.Resource로 의존성을 선언하고 @inject 데코레이터로 주입합니다. FastAPI의 Depends()가 가장 실용적인 DI 구현체로, 요청 스코프의 의존성 생명주기를 관리합니다. 수동 DI는 컴포지션 루트에서 객체 그래프를 직접 조립하는 방식으로 라이브러리 없이 테스트 가능성을 유지합니다. 인터페이스를 Protocol로 정의하고 구현체를 교체하면 테스트에서 모의 객체로 대체가 용이합니다. 의존성 역전 원칙(DIP)을 적용하여 고수준 모듈이 저수준 구현에 직접 의존하지 않도록 추상 레이어를 설계합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Django 마이그레이션 전략과 무중단 스키마 변경 방법을 설명해주세요.', 'DJANGO',
 '무중단 컬럼 추가는 null=True로 새 컬럼을 추가하고 데이터를 채운 뒤 Not Null 제약을 추가하는 3단계 마이그레이션으로 진행합니다. 컬럼 이름 변경은 새 컬럼 추가 → 데이터 복사 → 코드 전환 → 구 컬럼 삭제의 Expand-Contract 패턴으로 수행합니다. 대용량 테이블의 인덱스 생성은 AddIndex(condition=''...'')의 concurrent=True 옵션(PostgreSQL)으로 테이블 잠금 없이 처리합니다. RunSQL과 RunPython을 마이그레이션에 조합하여 복잡한 데이터 변환을 처리합니다. 마이그레이션 파일을 스쿼시(squash)하여 누적된 마이그레이션을 정기적으로 통합하고, 각 마이그레이션에 역방향(reverse) 코드를 작성하여 롤백 가능성을 유지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('BACKEND:SENIOR:PYTHON_DJANGO:LANGUAGE_FRAMEWORK', 'Python에서 성능 프로파일링과 최적화를 위한 도구와 기법을 설명해주세요.', 'PYTHON_CORE',
 'cProfile과 pstats로 함수별 호출 횟수와 누적 실행 시간을 분석하고, snakeviz로 콜 그래프를 시각화합니다. line_profiler(@profile 데코레이터)로 라인 단위 실행 시간을 측정하여 핫스팟 코드를 정밀하게 찾습니다. memory_profiler로 메모리 사용량 증가를 추적하고, tracemalloc으로 메모리 할당 출처를 분석합니다. timeit 모듈로 마이크로 벤치마크를 수행하고, pytest-benchmark으로 회귀 성능 변화를 추적합니다. NumPy 연산 최적화는 vectorization(루프 제거)과 broadcasting으로, 문자열 처리는 join()과 리스트 컴프리헨션으로 속도를 개선합니다. Cython이나 mypyc으로 타입 힌트가 있는 코드를 C 확장으로 컴파일하면 CPython 대비 수십 배 성능 향상을 얻습니다.',
 'MODEL_ANSWER', TRUE, NOW());
