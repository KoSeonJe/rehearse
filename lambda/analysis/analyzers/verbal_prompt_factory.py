"""Position × TechStack별 언어 분석 프롬프트 조립 (v2 — Lane 3 compression)

변경 요약 (vs v1):
- VERBAL_EXPERTISE 전 스택 블록 40~60% 압축 (키워드 나열 → 핵심 영역 구조화)
- MINIMAL_PERSONAS 5개 dict → 1개 label dict + f-string 조립
- SYSTEM_TEMPLATE에서 사용되지 않는 `tone_comment` 필드 제거 (BE 미소비, grep 확인)
- 평가 규칙과 JSON 스키마 중복 제거 및 병합
- feedback_perspective 미지정 시 factory 내부 기본값 "TECHNICAL" (handler.py 기본값과 일치)

Signature 불변:
- build_system_prompt(position, tech_stack, feedback_perspective=None)
- build_user_prompt(position, tech_stack, level, question, transcript, model_answer=None)

JSON 출력 키 불변 (tone_comment 1개 제거):
- filler_word_count, tone_label, comment, attitude_comment
"""
from __future__ import annotations

from analyzers.prompts import KOREAN_INSTRUCTION

DEFAULT_TECH_STACKS: dict[str, str] = {
    "BACKEND": "JAVA_SPRING",
    "FRONTEND": "REACT_TS",
    "DEVOPS": "AWS_K8S",
    "DATA_ENGINEER": "SPARK_AIRFLOW",
    "FULLSTACK": "REACT_SPRING",
}

# persona 라벨만 보관 — f-string으로 조립하여 반복 문자열 제거
_STACK_LABELS: dict[str, str] = {
    "BACKEND_JAVA_SPRING": "백엔드(Java/Spring)",
    "FRONTEND_REACT_TS": "프론트엔드(React/TypeScript)",
    "DEVOPS_AWS_K8S": "데브옵스(AWS/Kubernetes)",
    "DATA_ENGINEER_SPARK_AIRFLOW": "데이터 엔지니어(Spark/Airflow)",
    "FULLSTACK_REACT_SPRING": "풀스택(React + Spring)",
}

# v1 대비 40~60% 압축: 키워드 나열 → 핵심 영역 + 수치 기준 + 구조 패턴
VERBAL_EXPERTISE: dict[str, str] = {
    "BACKEND_JAVA_SPRING": (
        "기술용어 정확성, 성능수치(TPS/응답시간/GC pause) 구체성, 원인→해결→결과 구조.\n"
        "핵심 영역: JVM/GC, JPA(N+1·fetch join·영속성), 트랜잭션 전파·격리, 동시성(락·풀), Spring IoC/AOP."
    ),
    "FRONTEND_REACT_TS": (
        "기술용어 정확성, 성능수치(LCP/CLS/번들) 구체성, UX 개선을 기술로 설명.\n"
        "핵심 영역: React 렌더링(fiber·memo·Concurrent), 상태관리(hooks·Query), Next.js SSR/ISR, 번들 최적화, TypeScript 타입설계."
    ),
    "DEVOPS_AWS_K8S": (
        "인프라 용어 정확성, 가용성 수치(RTO/RPO/SLA) 구체성, 장애→원인→복구→재발방지 구조.\n"
        "핵심 영역: AWS 네트워킹(VPC·ALB), EKS/워크로드(Pod·HPA), IaC(Terraform·Helm·ArgoCD), 관측(Prometheus·OTel), 컨테이너 최적화."
    ),
    "DATA_ENGINEER_SPARK_AIRFLOW": (
        "분산처리 용어 정확성, 처리량/지연 수치(파티션·배치시간) 구체성, 데이터 흐름→변환→적재 구조.\n"
        "핵심 영역: Spark 실행(파티션·셔플·스큐), Airflow DAG·XCom, 저장 포맷(Parquet·Iceberg), 스트리밍(Kafka·워터마크), 웨어하우스/레이크."
    ),
    "FULLSTACK_REACT_SPRING": (
        "클라이언트-서버 경계 명확성, 양측 수치(응답시간·번들) 구체성, 각 영역 용어 정확성.\n"
        "핵심 영역: [FE] React hooks·Query·Next.js·번들, [BE] Spring JPA·트랜잭션·풀·GC, [공통] REST/GraphQL·인증·CI/CD·배포."
    ),
}

FEEDBACK_PERSPECTIVES: dict[str, str] = {
    "TECHNICAL": "피드백 관점: 기술용어 정확성과 개념 깊이 중심. 핵심 영역 용어의 정확/오용 여부.",
    "BEHAVIORAL": "피드백 관점: STAR 기법, 본인 역할 구체성, 수치 성과·사례 포함 여부.",
    "EXPERIENCE": "피드백 관점: 프로젝트 기여도와 기술 의사결정 배경. 선택 이유(대안 비교)와 결과 정량화.",
}

SYSTEM_TEMPLATE = KOREAN_INSTRUCTION + """{persona} 면접 답변의 언어적 커뮤니케이션을 분석합니다.

분석 기준: {verbal_expertise}
{perspective}

평가 규칙:
- filler_word_count: "음/어/그/아/뭐/이제/약간/좀/그러니까" 등장 횟수(정수)
- tone_label ∈ {{PROFESSIONAL|CASUAL|HESITANT|CONFIDENT|VERBOSE}}
- comment: 3줄 "✓ 잘한 점 / △ 보완할 점 / → 개선 방법" (기술 용어 정확성/오용, 답변 간결성, 전달 명확성 포함)
- attitude_comment: 3줄 "✓ 긍정 / △ 부정 / → 개선" (구체적 표현·어투 근거 필수)

JSON만 응답:
{{"filler_word_count":0,"tone_label":"","comment":"","attitude_comment":""}}"""

USER_TEMPLATE = """직무: {position} ({tech_stack}) | 레벨: {level}
질문: {question}
{model_answer_line}답변(STT): {transcript}"""


def _resolve_key(position: str, tech_stack: str | None) -> tuple[str, str]:
    effective_stack = tech_stack or DEFAULT_TECH_STACKS.get(position, "JAVA_SPRING")
    return f"{position}_{effective_stack}", effective_stack


def build_system_prompt(
    position: str,
    tech_stack: str | None,
    feedback_perspective: str | None = None,
) -> str:
    key, _ = _resolve_key(position, tech_stack)

    persona_label = _STACK_LABELS.get(key, f"{position}")
    expertise = VERBAL_EXPERTISE.get(
        key,
        "기술용어 정확성, 수치 구체성, 원인→해결→결과 구조 중심으로 평가.",
    )
    # factory 내부 기본값을 handler.py:350 기본값과 동일하게 맞춤
    perspective_key = feedback_perspective or "TECHNICAL"
    perspective_text = FEEDBACK_PERSPECTIVES.get(
        perspective_key, FEEDBACK_PERSPECTIVES["TECHNICAL"]
    )

    return SYSTEM_TEMPLATE.format(
        persona=persona_label,
        verbal_expertise=expertise,
        perspective=perspective_text,
    )


def build_user_prompt(
    position: str,
    tech_stack: str | None,
    level: str,
    question: str,
    transcript: str,
    model_answer: str | None = None,
) -> str:
    _, effective_stack = _resolve_key(position, tech_stack)
    model_answer_line = f"모범답변: {model_answer}\n" if model_answer else ""
    return USER_TEMPLATE.format(
        position=position,
        tech_stack=effective_stack,
        level=level,
        question=question,
        model_answer_line=model_answer_line,
        transcript=transcript,
    )
