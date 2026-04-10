# Plan 04: 프레임워크/기술 스택 시드 데이터 작성

> 상태: Draft
> 작성일: 2026-04-10

## Why

LANGUAGE_FRAMEWORK, UI_FRAMEWORK, INFRA_CICD, CLOUD, FULLSTACK_STACK은 position-specific 타입으로, 포지션+레벨+스택 조합별로 별도 캐시키가 필요하다. 기본 + 인기 스택 9종을 시딩하여 주요 사용자층의 첫 면접 경험을 개선한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/main/resources/db/seed/backend-java-spring.sql` | LANGUAGE_FRAMEWORK 30×3레벨 = 90개 |
| `backend/src/main/resources/db/seed/backend-python-django.sql` | LANGUAGE_FRAMEWORK 30×3레벨 = 90개 |
| `backend/src/main/resources/db/seed/backend-node-nestjs.sql` | LANGUAGE_FRAMEWORK 30×3레벨 = 90개 |
| `backend/src/main/resources/db/seed/backend-kotlin-spring.sql` | LANGUAGE_FRAMEWORK 30×3레벨 = 90개 |
| `backend/src/main/resources/db/seed/frontend-react-ts.sql` | LANGUAGE_FRAMEWORK 90 + UI_FRAMEWORK 90 = 180개 |
| `backend/src/main/resources/db/seed/frontend-vue-ts.sql` | LANGUAGE_FRAMEWORK 30×3레벨 = 90개 |
| `backend/src/main/resources/db/seed/devops-aws-k8s.sql` | INFRA_CICD 90 + CLOUD 90 = 180개 |
| `backend/src/main/resources/db/seed/fullstack-react-spring.sql` | FULLSTACK_STACK 30×3레벨 = 90개 |

## 상세

### 캐시키 매핑

| 스택 | InterviewType | cache_key 패턴 | 레벨별 질문 |
|------|---------------|---------------|-----------|
| JAVA_SPRING | LANGUAGE_FRAMEWORK | `BACKEND:{Level}:JAVA_SPRING:LANGUAGE_FRAMEWORK` | 30개 |
| PYTHON_DJANGO | LANGUAGE_FRAMEWORK | `BACKEND:{Level}:PYTHON_DJANGO:LANGUAGE_FRAMEWORK` | 30개 |
| NODE_NESTJS | LANGUAGE_FRAMEWORK | `BACKEND:{Level}:NODE_NESTJS:LANGUAGE_FRAMEWORK` | 30개 |
| KOTLIN_SPRING | LANGUAGE_FRAMEWORK | `BACKEND:{Level}:KOTLIN_SPRING:LANGUAGE_FRAMEWORK` | 30개 |
| REACT_TS | LANGUAGE_FRAMEWORK | `FRONTEND:{Level}:REACT_TS:LANGUAGE_FRAMEWORK` | 30개 |
| REACT_TS | UI_FRAMEWORK | `FRONTEND:{Level}:REACT_TS:UI_FRAMEWORK` | 30개 |
| VUE_TS | LANGUAGE_FRAMEWORK | `FRONTEND:{Level}:VUE_TS:LANGUAGE_FRAMEWORK` | 30개 |
| AWS_K8S | INFRA_CICD | `DEVOPS:{Level}:AWS_K8S:INFRA_CICD` | 30개 |
| AWS_K8S | CLOUD | `DEVOPS:{Level}:AWS_K8S:CLOUD` | 30개 |
| REACT_SPRING | FULLSTACK_STACK | `FULLSTACK:{Level}:REACT_SPRING:FULLSTACK_STACK` | 30개 |

### 질문 소스 (웹 리서치 기반)

#### Java/Spring Boot (가장 빈출)

**JUNIOR**: Spring Boot vs Spring Framework, @SpringBootApplication 역할, IoC/DI 개념, Bean 생명주기, @Autowired vs 생성자 주입, Auto Configuration, application.yml 설정, @Configuration vs @Component, Embedded Tomcat, Spring Boot Starter
**MID**: 싱글톤과 Bean Scope, AOP 원리, @Transactional 동작(프록시), 전파 레벨(Propagation), Spring Security Authentication/Authorization, Filter vs Interceptor, JWT 인증, JPA N+1 문제, Lazy/Eager Loading, 순환 참조 해결
**SENIOR**: BeanFactory vs ApplicationContext, CGLIB vs JDK Proxy, @Transactional과 Private 메서드, Auto Configuration 조건부 Bean(@ConditionalOnClass), Event-Driven Architecture, WebClient vs RestTemplate, JPA cascade/orphanRemoval, Actuator 메트릭, 대규모 배치 처리

#### Python/Django·FastAPI

**JUNIOR**: GIL 개념, 데코레이터 기본, Django vs FastAPI, MTV 아키텍처, async/await, @property, ORM 기초, FastAPI DI, 리스트 컴프리헨션, 제너레이터
**MID**: GIL 우회(멀티프로세싱), 파라미터 데코레이터, Middleware, Queryset Lazy Evaluation, select_related vs prefetch_related, Signal 패턴, Pydantic 검증, 동시성 vs 병렬성
**SENIOR**: asyncio 이벤트 루프 내부, GIL 완전 우회(PyPy/Cython), 복잡한 쿼리 최적화, Celery vs background task, Python 3.13 Free-Threading

#### Node.js/NestJS

**JUNIOR**: 이벤트 루프 개념, 이벤트 루프 6페이즈, 콜스택 vs 콜백큐, Promise vs Callback, async/await, NestJS 개념, NestJS DI, Module/Controller/Service 구조
**MID**: setImmediate vs setTimeout(0), Promise.all/race, NestJS Middleware vs Interceptor, Exception Filter, 순환 의존성, Worker Thread, Guards/decorators 인증, process.nextTick
**SENIOR**: 이벤트 루프 우선순위 상세, Worker Thread vs Cluster, 메모리 누수 디버깅/프로파일링, NestJS 캐싱 전략, Stream 비동기 처리

#### Kotlin/Spring Boot

**JUNIOR**: Kotlin vs Java 차이, Null Safety, Data Class, Extension Function, 코루틴 기본, launch vs async, Scope Functions, val vs var
**MID**: suspend 함수, CoroutineScope/Context, Dispatcher.IO vs Default, 코루틴 취소, Sequences vs Collections, Structured Concurrency, Kotlin Spring 코루틴 주의점, CoroutineExceptionHandler, Sealed Class
**SENIOR**: Virtual Thread 비교, Channel/Flow/SharedFlow, Job 생명주기, Kotlin Multiplatform 코루틴, 코루틴 성능 모니터링/디버깅

#### React/TypeScript

**JUNIOR**: Virtual DOM, Reconciliation, 클래스 vs 함수형 컴포넌트, Hook 규칙, useState 동작, useEffect 의존성 배열, useEffect vs useLayoutEffect, props drilling, key props 역할, Context API
**MID**: useMemo vs useCallback, React.memo, 얕은 비교, useReducer, 커스텀 Hook 설계, 배치 업데이트, useRef vs useState, 렌더링 최적화 5가지, Redux vs Zustand vs Context, TypeScript 제네릭 Props
**SENIOR**: Fiber 아키텍처, Concurrent Rendering/Suspense, Server Component, React DevTools Profiler, TypeScript strict mode 타입 안전성

#### React/TypeScript - UI_FRAMEWORK

**JUNIOR**: JSX 문법, 컴포넌트 스타일링 방법(CSS Modules/styled-components/Tailwind), 이벤트 핸들링, 폼 제어 컴포넌트, 조건부 렌더링, 리스트 렌더링
**MID**: 디자인 시스템 구축, Compound Component 패턴, Headless UI, 접근성(a11y), 반응형 설계, 애니메이션(Framer Motion), Storybook 활용
**SENIOR**: 마이크로 프론트엔드, 웹 컴포넌트, 성능 측정(Core Web Vitals), 번들 사이즈 최적화, Tree-shaking, Code Splitting 전략

#### Vue.js/TypeScript

**JUNIOR**: Vue 2 vs Vue 3, 반응성 시스템, Options vs Composition API, ref vs reactive, computed vs watch, v-model, 라이프사이클 훅, Props/Emit, Slot, Pinia vs Vuex
**MID**: Composition API custom hook, Provide/Inject, 템플릿 컴파일, readonly/shallowRef, Pinia store 설계, v-if vs v-show 성능, key 중요성, Teleport, TypeScript Pinia 타이핑
**SENIOR**: 반응성 프록시 제약, 복잡한 Composition API 재사용, Tree-shaking/번들 최적화, HMR, memo/defineAsyncComponent

#### AWS/Kubernetes - INFRA_CICD

**JUNIOR**: CI/CD 개념, GitHub Actions 기본, Docker 기초(이미지/컨테이너), Dockerfile 작성, docker-compose, 환경 변수 관리, 배포 전략(Blue-Green/Rolling/Canary)
**MID**: 멀티스테이지 빌드, ECS vs EKS vs Fargate, Kubernetes Deployment/ReplicaSet/StatefulSet, K8s Service 종류, ConfigMap/Secret, HPA/VPA, Helm 차트
**SENIOR**: GitOps(ArgoCD), 서비스 메시(Istio), K8s 네트워크 정책, 리소스 할당/QoS, 멀티 클러스터 관리, 카나리 배포 자동화

#### AWS/Kubernetes - CLOUD

**JUNIOR**: EC2/S3/RDS/Lambda 핵심, VPC 개념, IAM 역할, CloudFront, 리전/AZ, 보안그룹 vs NACL, S3 스토리지 클래스
**MID**: ALB/NLB 차이, CloudWatch/CloudTrail, Auto Scaling 정책, Lambda 콜드 스타트, IAM Role/Policy, ElastiCache 캐싱 전략, SQS vs SNS vs Kinesis, RDS Proxy
**SENIOR**: 멀티 리전 DR 전략, EventBridge 이벤트 아키텍처, 서버리스 설계 원칙, 비용 최적화 전략, Well-Architected Framework

#### Fullstack - REACT_SPRING

**JUNIOR**: 프론트엔드-백엔드 통신(REST), JSON 직렬화, CORS 설정, 인증/인가 기초(JWT), API 설계 기본
**MID**: BFF 패턴, API 버저닝, WebSocket 실시간 통신, SSR vs CSR vs SSG, GraphQL 기초, 모노레포 관리
**SENIOR**: 마이크로 프론트엔드 + MSA 통합, 풀스택 성능 최적화, E2E 테스트 전략, 배포 파이프라인 설계, 기술 스택 선택 근거

### INSERT 형식 (Plan 06 이후 스키마 기준)

```sql
INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('{Position}:{Level}:{TechStack}:{InterviewType}', '{질문}', '{세부카테고리}',
 '{모범 답변}',
 'MODEL_ANSWER', TRUE, NOW());
```

> Plan 06에서 `question_order`, `evaluation_criteria`, `follow_up_strategy`, `quality_score` 4개 컬럼이 삭제되므로 포함하지 않음.
> `INSERT IGNORE`로 중복 실행 시 에러 방지.

## 담당 에이전트

- Implement: `backend` — SQL 파일 생성 (스택별 병렬 가능)
- Review: `code-reviewer` — 데이터 품질, cache_key 정확성

## 검증

- 각 SQL 파일 실행 성공 확인
- cache_key별 COUNT 확인
- 전체 약 810개 질문 적재 확인
- cache_key 형식이 QuestionCacheKeyGenerator.generate()와 일치하는지 확인
- `progress.md` 상태 업데이트 (Task 4 → Completed)
