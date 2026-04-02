"""Position × TechStack별 언어 분석 프롬프트 조립 (v3 최적화)"""
from __future__ import annotations

from analyzers.prompts import KOREAN_INSTRUCTION

DEFAULT_TECH_STACKS: dict[str, str] = {
    "BACKEND": "JAVA_SPRING",
    "FRONTEND": "REACT_TS",
    "DEVOPS": "AWS_K8S",
    "DATA_ENGINEER": "SPARK_AIRFLOW",
    "FULLSTACK": "REACT_SPRING",
}

MINIMAL_PERSONAS: dict[str, str] = {
    "BACKEND_JAVA_SPRING": "백엔드(Java/Spring) 면접 답변의 언어적 커뮤니케이션을 분석합니다.",
    "FRONTEND_REACT_TS": "프론트엔드(React/TypeScript) 면접 답변의 언어적 커뮤니케이션을 분석합니다.",
    "DEVOPS_AWS_K8S": "데브옵스(AWS/Kubernetes) 면접 답변의 언어적 커뮤니케이션을 분석합니다.",
    "DATA_ENGINEER_SPARK_AIRFLOW": "데이터 엔지니어(Spark/Airflow) 면접 답변의 언어적 커뮤니케이션을 분석합니다.",
    "FULLSTACK_REACT_SPRING": "풀스택(React + Spring) 면접 답변의 언어적 커뮤니케이션을 분석합니다.",
}

VERBAL_EXPERTISE: dict[str, str] = {
    "BACKEND_JAVA_SPRING": """Java/Spring 답변 분석 기준:
- JVM, Spring, JPA 기술 용어의 정확한 사용
- 성능 수치(TPS, 응답시간, GC pause time) 구체적 언급
- "원인→해결→결과" 구조 설명 능력

키워드 사전:
JVM, GC, 힙 메모리, 메타스페이스, 스레드 풀, HikariCP, 커넥션 풀,
트랜잭션 격리 수준, @Transactional, 전파 속성, 롤백,
영속성 컨텍스트, 1차 캐시, 지연 로딩, 즉시 로딩, N+1, fetch join, EntityGraph,
Spring IoC, 빈 스코프, AOP, 프록시, CGLIB,
@Version, 낙관적 락, 비관적 락, 데드락,
Spring Cloud, 서킷 브레이커, Resilience4j, Spring Kafka""",

    "FRONTEND_REACT_TS": """React/TypeScript 답변 분석 기준:
- React, Next.js, TypeScript 관련 기술 용어의 정확한 사용
- 성능 수치(LCP, CLS, 번들 사이즈)의 구체적 언급
- 사용자 경험 개선을 기술적으로 설명하는 능력

키워드 사전:
Virtual DOM, 재조정(Reconciliation), fiber, Concurrent Mode,
Suspense, useTransition, useDeferredValue, 서버 컴포넌트,
useState, useEffect, useRef, useMemo, useCallback, useReducer,
React.memo, React.lazy, ErrorBoundary,
Context, Zustand, Jotai, TanStack Query, SWR,
Next.js, App Router, SSR, SSG, ISR, 미들웨어, 서버 액션,
LCP, FID, INP, CLS, TTI, TTFB,
코드 스플리팅, 트리 셰이킹, 번들 분석, 청크,
TypeScript, 제네릭, 유틸리티 타입, 타입 가드, 타입 좁히기""",

    "DEVOPS_AWS_K8S": """AWS/Kubernetes 답변 분석 기준:
- 클라우드 인프라, 컨테이너 오케스트레이션 용어의 정확한 사용
- 가용성/복구 수치(RTO, RPO, SLA, 99.9%) 구체적 언급
- "장애→원인→복구→재발방지" 구조 설명 능력

키워드 사전:
VPC, 서브넷, 보안 그룹, IGW, NAT Gateway, Route 53, CloudFront,
EKS, ECS, Fargate, EC2, Auto Scaling, ALB, NLB,
IAM, 역할, 정책, 최소 권한, 신뢰 정책,
Terraform, Helm, ArgoCD, GitOps, Flux,
Pod, Deployment, StatefulSet, DaemonSet, HPA, VPA, CRD,
Prometheus, Grafana, Loki, OpenTelemetry, PagerDuty,
RDS, Aurora, ElastiCache, SQS, SNS, Kinesis,
Docker, 컨테이너 레이어, 멀티스테이지 빌드, 이미지 최적화""",

    "DATA_ENGINEER_SPARK_AIRFLOW": """Spark/Airflow 답변 분석 기준:
- 분산 처리, 데이터 파이프라인 용어의 정확한 사용
- 처리량/지연 수치(TPS, 처리 시간, 파티션 수) 구체적 언급
- "데이터 흐름→변환→적재" 구조 설명 능력

키워드 사전:
Spark, RDD, DataFrame, Dataset, SparkSQL, Catalyst 옵티마이저,
파티션, 셔플, 스테이지, 태스크, 드라이버, 익스큐터,
브로드캐스트 조인, 스큐, 재파티셔닝, persist/cache,
Airflow, DAG, Operator, Task, XCom, 스케줄러, 메타DB,
Parquet, ORC, Avro, Delta Lake, Iceberg, Hudi,
Kafka, 파티션, 컨슈머 그룹, 오프셋, 리밸런싱,
Spark Streaming, 마이크로 배치, 워터마크, 지연 데이터,
Redshift, BigQuery, Snowflake, dbt, Glue, EMR""",

    "FULLSTACK_REACT_SPRING": """React + Spring 풀스택 답변 분석 기준:
- 프론트엔드/백엔드 연계 및 각 영역 기술 용어의 정확한 사용
- 성능·확장성 수치(응답시간, 번들 사이즈, TPS) 구체적 언급
- 클라이언트-서버 경계를 명확히 구분한 설명 능력

키워드 사전:
[React] Virtual DOM, fiber, hooks, Zustand, TanStack Query, Next.js, SSR, SSG, TypeScript,
코드 스플리팅, LCP, CLS, 번들 최적화,
[Spring] Spring IoC, AOP, @Transactional, JPA, N+1, fetch join,
HikariCP, GC, 스레드 풀, 커넥션 풀,
[공통] REST, GraphQL, WebSocket, CORS, CSRF, JWT, OAuth2,
API Gateway, BFF, MSA, 이벤트 드리븐, 메시지 큐,
CI/CD, Docker, 무중단 배포, 블루-그린, 카나리""",
}

FEEDBACK_PERSPECTIVES: dict[str, str] = {
    "TECHNICAL": """## 기술 피드백 관점
- comment에 기술 용어 정확성, 개념 깊이를 중심으로 피드백
- 키워드 사전의 용어가 정확히 사용되었는지 확인""",
    "BEHAVIORAL": """## 경험 피드백 관점
- comment에 STAR 기법 적용 여부, 본인 역할의 구체성을 중심으로 피드백
- 수치화된 성과, 구체적 사례 포함 여부 확인""",
    "EXPERIENCE": """## 이력서 기반 피드백 관점
- comment에 프로젝트 기여도, 기술적 의사결정 배경을 중심으로 피드백
- 기술 선택 이유(대안 비교), 결과의 정량화 여부 확인""",
}

SYSTEM_TEMPLATE = KOREAN_INSTRUCTION + """{minimal_persona}

## 전문 분야
{verbal_expertise}

{feedback_perspective}
## 평가
1. filler_word_count: "음","어","그","아","뭐","이제","약간","좀","그러니까" 등장 횟수
2. tone_label: PROFESSIONAL|CASUAL|HESITANT|CONFIDENT|VERBOSE
3. tone_comment: 1-2문장의 말투 피드백
4. comment: 3줄 형식으로 작성. ✓ 잘한 점 / △ 보완할 점 / → 개선 방법. 키워드 사전 기반으로 기술 용어 정확성/오용도 포함
5. attitude_comment: 면접관 관점 태도/말투 인상. 3줄 형식: ✓ 긍정 / △ 부정 / → 개선. 구체적 근거(어떤 표현/어투) 제시 필수.

## 응답
JSON만 응답:
{{"filler_word_count":0,"tone_label":"","tone_comment":"","comment":"","attitude_comment":""}}"""

USER_TEMPLATE = """직무: {position} ({tech_stack}) | 레벨: {level}
질문: {question}
{model_answer_line}답변(STT): {transcript}"""


def build_system_prompt(position: str, tech_stack: str | None, feedback_perspective: str | None = None) -> str:
    effective_stack = tech_stack or DEFAULT_TECH_STACKS.get(position, "JAVA_SPRING")
    key = f"{position}_{effective_stack}"

    persona = MINIMAL_PERSONAS.get(key, f"{position} 면접 답변의 언어적 커뮤니케이션을 분석합니다.")
    expertise = VERBAL_EXPERTISE.get(key, "")
    perspective_text = FEEDBACK_PERSPECTIVES.get(feedback_perspective, "") if feedback_perspective else ""

    return SYSTEM_TEMPLATE.format(
        minimal_persona=persona,
        verbal_expertise=expertise,
        feedback_perspective=perspective_text,
    )


def build_user_prompt(
    position: str,
    tech_stack: str | None,
    level: str,
    question: str,
    transcript: str,
    model_answer: str | None = None,
) -> str:
    effective_stack = tech_stack or DEFAULT_TECH_STACKS.get(position, "JAVA_SPRING")
    model_answer_line = f"모범답변: {model_answer}\n" if model_answer else ""
    return USER_TEMPLATE.format(
        position=position,
        tech_stack=effective_stack,
        level=level,
        question=question,
        model_answer_line=model_answer_line,
        transcript=transcript,
    )
