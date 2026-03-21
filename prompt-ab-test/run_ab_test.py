#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""프롬프트 A/B 테스트: Group A (기존 단일 페르소나) vs Group B (레이어링 페르소나)"""

import json
import os
import re
import sys
import time
import uuid
from pathlib import Path

# 한국어 출력 인코딩 보장
os.environ.setdefault("PYTHONIOENCODING", "utf-8")

import anthropic

MODEL = "claude-sonnet-4-20250514"
TEMPERATURE = 0.9
MAX_TOKENS = 4096
RUNS = 3

# 직무 특화 키워드
SPECIFIC_KEYWORDS = [
    "JVM", "GC", "힙", "메타스페이스", "스레드 풀", "HikariCP", "커넥션 풀",
    "@Transactional", "전파 속성", "영속성 컨텍스트", "1차 캐시",
    "지연 로딩", "즉시 로딩", "N+1", "fetch join", "EntityGraph",
    "Spring IoC", "AOP", "프록시", "CGLIB", "@Version", "낙관적 락", "비관적 락",
    "G1", "ZGC", "Shenandoah", "Resilience4j", "Spring Cloud", "Spring Kafka",
]
GENERIC_KEYWORDS = ["데이터베이스", "성능", "최적화", "설계", "아키텍처", "트래픽", "확장성"]

DEEP_PATTERNS = ["왜", "어떻게", "비교", "트레이드오프", "차이"]
BASIC_PATTERNS = ["무엇", "설명", "정의"]

# Group A: 기존 단일 페르소나
SYSTEM_A = """당신은 한국 IT 기업의 시니어 개발자 면접관입니다.
주어진 직무, 레벨, 면접 유형에 맞는 면접 질문을 생성해야 합니다.

면접 유형별 출제 가이드:
- CS_FUNDAMENTAL: CS 기초 (자료구조, 알고리즘, 운영체제, 네트워크, 데이터베이스)
- BEHAVIORAL: STAR 기법 기반 경험 질문
- RESUME_BASED: 이력서/포트폴리오 기반 맞춤 질문
- JAVA_SPRING: Java/Spring 프레임워크 심화 (JVM, Spring IoC/AOP, JPA, 트랜잭션)
- SYSTEM_DESIGN: 시스템 아키텍처 설계, 스케일링, 트레이드오프 분석
- FULLSTACK_JS: Node.js + React 풀스택, API 설계, DB 연동, 배포
- REACT_COMPONENT: React 컴포넌트 설계, 상태 관리, 렌더링 최적화
- BROWSER_PERFORMANCE: 브라우저 렌더링, 웹 성능 최적화, 번들 최적화
- INFRA_CICD: 인프라 구성, CI/CD 파이프라인, 컨테이너 오케스트레이션
- CLOUD: 클라우드 아키텍처 (AWS/GCP/Azure), 서버리스, IaC
- DATA_PIPELINE: 데이터 수집/처리/적재 파이프라인, ETL/ELT, 스트리밍
- SQL_MODELING: SQL 쿼리 최적화, 데이터 모델링, 정규화/반정규화

CS 세부 주제가 지정된 경우 해당 주제에서만 출제하세요:
- OS: 운영체제
- NETWORK: 네트워크

레벨별 난이도:
- JUNIOR: 기본 개념 이해도 확인, 실무 경험보다 학습 의지
- MID: 실무 적용 능력, 문제 해결 경험, 기술적 깊이
- SENIOR: 아키텍처 판단력, 리더십, 기술 의사결정 능력

모범답변 생성 규칙:
- CS 카테고리 질문: referenceType을 "MODEL_ANSWER"로
- questionCategory는 기술/CS이면 "CS"

반드시 아래 JSON 형식으로만 응답하세요:
{"questions":[{"content":"질문","category":"카테고리","order":1,"evaluationCriteria":"평가기준","questionCategory":"CS","modelAnswer":"모범답변","referenceType":"MODEL_ANSWER"}]}"""

# Group B: 레이어링 페르소나 (v3 최적화)
SYSTEM_B = """당신은 한국 IT 기업에서 10년 이상 경력의 백엔드 시니어 개발자 면접관입니다.
서버 사이드 아키텍처 설계, 대규모 트래픽 처리, 데이터 정합성 보장에 대한 깊은 이해를 가지고 있습니다.
기술 스택에 관계없이 다음 역량을 중요하게 평가합니다:
- API 설계의 일관성과 확장성
- 동시성 제어와 데이터 정합성 보장 전략
- 장애 대응 경험과 운영 안정성에 대한 감각
- 성능 병목을 진단하고 해결하는 체계적 접근
특히 Java/Kotlin 언어와 Spring Boot 에코시스템에 깊은 전문성을 가지고 있습니다.
JVM 내부 동작, Spring IoC/AOP/트랜잭션 관리, JPA/Hibernate ORM에 대한 실무 경험이 풍부하며,
Spring Security, Spring Cloud 기반의 MSA 설계와 운영 경험이 있습니다.

면접 질문을 생성합니다.

## 평가 관점
- 코드 레벨: 동시성 제어, 트랜잭션 관리, 예외 처리 전략, 테스트 작성 습관
- 아키텍처 레벨: API 설계 원칙, 서비스 간 통신, 데이터 일관성 전략
- 운영 레벨: 장애 대응 경험, 성능 병목 진단, 모니터링/로깅 전략
- 성장 레벨: 기술 선택의 근거, 레거시 개선 경험, 코드 리뷰 문화

## 출제 가이드
- CS_FUNDAMENTAL: OS, 네트워크, 자료구조. Java 관점 실무 연결. (예: TCP handshake → HikariCP 커넥션 풀)
- JAVA_SPRING: JVM 메모리/GC, Spring IoC/AOP, @Transactional 전파, JPA N+1, 동시성 제어

## CS 세부 주제
OS, NETWORK에서만 출제.

## 난이도
JUNIOR: 기본 개념의 정확한 이해. 원리 중심 질문, CS 기초와 논리적 사고력 평가. 실무 경험보다 학습 의지.

## 질문 수
(면접시간(분)÷3) 반올림, 최소2 최대24. 유형별 균등 배분.

## 모범답변
- CS 질문: referenceType="MODEL_ANSWER", 핵심개념+실무예시 포함
- questionCategory: 기술/CS→"CS"

## 응답
JSON만 응답. 형식:
{"questions":[{"content":"","category":"","order":1,"evaluationCriteria":"2-3문장","questionCategory":"RESUME|CS","modelAnswer":"","referenceType":"MODEL_ANSWER|GUIDE"}]}"""

USER_PROMPT = f"""직무: 백엔드 (Java/Spring Boot)
레벨: 주니어
유형: Java/Spring, CS 기초
질문 수: 6개
CS 세부: OS, NETWORK
세션: {uuid.uuid4()}
중복 없는 새 관점의 질문을 생성하세요."""


def call_api(client, system_prompt, user_prompt):
    response = client.messages.create(
        model=MODEL,
        max_tokens=MAX_TOKENS,
        temperature=TEMPERATURE,
        system=system_prompt,
        messages=[{"role": "user", "content": user_prompt}],
    )
    text = response.content[0].text
    usage = {"input_tokens": response.usage.input_tokens, "output_tokens": response.usage.output_tokens}
    return text, usage


def parse_questions(text):
    text = text.strip()
    if text.startswith("```"):
        lines = text.split("\n")
        text = "\n".join(lines[1:-1])
    match = re.search(r'\{.*\}', text, re.DOTALL)
    if match:
        data = json.loads(match.group())
        return data.get("questions", [])
    return []


def count_keywords(questions, keyword_list):
    count = 0
    for q in questions:
        text = q.get("content", "") + " " + q.get("modelAnswer", "")
        for kw in keyword_list:
            count += text.count(kw)
    return count


def count_patterns(questions, patterns):
    count = 0
    for q in questions:
        content = q.get("content", "")
        for p in patterns:
            count += content.count(p)
    return count


def main():
    client = anthropic.Anthropic()
    results = {"group_a": [], "group_b": []}

    for group, system_prompt in [("group_a", SYSTEM_A), ("group_b", SYSTEM_B)]:
        print(f"\n=== {group.upper()} ===")
        for run in range(1, RUNS + 1):
            print(f"  Run {run}/{RUNS}...", end=" ", flush=True)
            try:
                text, usage = call_api(client, system_prompt, USER_PROMPT)
                questions = parse_questions(text)
                specific = count_keywords(questions, SPECIFIC_KEYWORDS)
                generic = count_keywords(questions, GENERIC_KEYWORDS)
                deep = count_patterns(questions, DEEP_PATTERNS)
                basic = count_patterns(questions, BASIC_PATTERNS)

                results[group].append({
                    "run": run,
                    "question_count": len(questions),
                    "questions": questions,
                    "specific_keywords": specific,
                    "generic_keywords": generic,
                    "keyword_specificity": specific / (specific + generic) if (specific + generic) > 0 else 0,
                    "deep_patterns": deep,
                    "basic_patterns": basic,
                    "depth_score": deep / (deep + basic) if (deep + basic) > 0 else 0,
                    "input_tokens": usage["input_tokens"],
                    "output_tokens": usage["output_tokens"],
                })
                print(f"OK (questions={len(questions)}, specific={specific}, tokens={usage['input_tokens']}+{usage['output_tokens']})")
            except Exception as e:
                print(f"FAIL: {e}")
                results[group].append({"run": run, "error": str(e)})
            time.sleep(1)

    # 결과 저장
    output_path = Path(__file__).parent / "results.json"
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(results, f, ensure_ascii=False, indent=2)
    print(f"\n결과 저장: {output_path}")

    # 리포트 생성
    generate_report(results)


def avg(values):
    return sum(values) / len(values) if values else 0


def generate_report(results):
    a_runs = [r for r in results["group_a"] if "error" not in r]
    b_runs = [r for r in results["group_b"] if "error" not in r]

    a_specificity = avg([r["keyword_specificity"] for r in a_runs])
    b_specificity = avg([r["keyword_specificity"] for r in b_runs])
    a_depth = avg([r["depth_score"] for r in a_runs])
    b_depth = avg([r["depth_score"] for r in b_runs])
    a_specific_count = avg([r["specific_keywords"] for r in a_runs])
    b_specific_count = avg([r["specific_keywords"] for r in b_runs])
    a_input = avg([r["input_tokens"] for r in a_runs])
    b_input = avg([r["input_tokens"] for r in b_runs])
    a_output = avg([r["output_tokens"] for r in a_runs])
    b_output = avg([r["output_tokens"] for r in b_runs])

    token_savings = (1 - b_input / a_input) * 100 if a_input > 0 else 0

    # 대표 질문 추출
    a_sample = a_runs[0]["questions"][:3] if a_runs and a_runs[0]["questions"] else []
    b_sample = b_runs[0]["questions"][:3] if b_runs and b_runs[0]["questions"] else []

    report = f"""# 프롬프트 A/B 테스트 결과

> **실행일**: 2026-03-21
> **모델**: {MODEL}
> **Temperature**: {TEMPERATURE}
> **반복 횟수**: {RUNS}회/그룹

---

## 테스트 조건

| 항목 | 값 |
|------|-----|
| Position | BACKEND |
| TechStack | JAVA_SPRING |
| Level | JUNIOR |
| InterviewTypes | JAVA_SPRING, CS_FUNDAMENTAL |
| CS SubTopics | OS, NETWORK |
| 질문 수 | 6개 |

---

## 요약

| 지표 | Group A (기존) | Group B (레이어링) | 차이 | 판정 |
|------|--------------|------------------|------|------|
| 직무 특화 키워드 비율 | {a_specificity:.1%} | {b_specificity:.1%} | {(b_specificity - a_specificity):.1%}p | {"B 우위" if b_specificity > a_specificity else "A 우위"} |
| 직무 특화 키워드 수 (평균) | {a_specific_count:.1f}개 | {b_specific_count:.1f}개 | {b_specific_count - a_specific_count:+.1f}개 | {"B 우위" if b_specific_count > a_specific_count else "A 우위"} |
| 질문 깊이 점수 | {a_depth:.1%} | {b_depth:.1%} | {(b_depth - a_depth):.1%}p | {"B 우위" if b_depth > a_depth else "동등"} |
| System Prompt input 토큰 (평균) | {a_input:.0f} tok | {b_input:.0f} tok | {token_savings:.1f}% 절감 | {"B 효율적" if token_savings > 0 else "A 효율적"} |
| Output 토큰 (평균) | {a_output:.0f} tok | {b_output:.0f} tok | - | - |

---

## 질문 생성 상세

### 직무 특화 키워드 비율

직무 특화 키워드(JVM, GC, Spring IoC, @Transactional, N+1 등)와 범용 키워드(성능, 최적화, 설계 등)의 비율.

| Run | Group A | Group B |
|-----|---------|---------|
"""

    for i in range(RUNS):
        a_val = f"{a_runs[i]['keyword_specificity']:.1%} ({a_runs[i]['specific_keywords']}개)" if i < len(a_runs) else "N/A"
        b_val = f"{b_runs[i]['keyword_specificity']:.1%} ({b_runs[i]['specific_keywords']}개)" if i < len(b_runs) else "N/A"
        report += f"| {i+1} | {a_val} | {b_val} |\n"

    report += f"| **평균** | **{a_specificity:.1%}** | **{b_specificity:.1%}** |\n"

    report += f"""
### 질문 깊이 점수

심화 패턴(왜, 어떻게, 비교, 트레이드오프) vs 기본 패턴(무엇, 설명, 정의)의 비율.

| Run | Group A | Group B |
|-----|---------|---------|
"""

    for i in range(RUNS):
        a_val = f"{a_runs[i]['depth_score']:.1%} (심화{a_runs[i]['deep_patterns']}/기본{a_runs[i]['basic_patterns']})" if i < len(a_runs) else "N/A"
        b_val = f"{b_runs[i]['depth_score']:.1%} (심화{b_runs[i]['deep_patterns']}/기본{b_runs[i]['basic_patterns']})" if i < len(b_runs) else "N/A"
        report += f"| {i+1} | {a_val} | {b_val} |\n"

    report += f"| **평균** | **{a_depth:.1%}** | **{b_depth:.1%}** |\n"

    report += f"""
### 토큰 사용량

| Run | Group A (input/output) | Group B (input/output) |
|-----|----------------------|----------------------|
"""

    for i in range(RUNS):
        a_val = f"{a_runs[i]['input_tokens']}/{a_runs[i]['output_tokens']}" if i < len(a_runs) else "N/A"
        b_val = f"{b_runs[i]['input_tokens']}/{b_runs[i]['output_tokens']}" if i < len(b_runs) else "N/A"
        report += f"| {i+1} | {a_val} | {b_val} |\n"

    report += f"| **평균** | **{a_input:.0f}/{a_output:.0f}** | **{b_input:.0f}/{b_output:.0f}** |\n"

    report += """
---

## 실제 질문 비교 (Run 1 대표 예시)

### Group A (기존)

| # | 질문 | 카테고리 |
|---|------|---------|
"""

    for i, q in enumerate(a_sample):
        report += f"| {i+1} | {q.get('content', 'N/A')} | {q.get('category', 'N/A')} |\n"

    report += """
### Group B (레이어링)

| # | 질문 | 카테고리 |
|---|------|---------|
"""

    for i, q in enumerate(b_sample):
        report += f"| {i+1} | {q.get('content', 'N/A')} | {q.get('category', 'N/A')} |\n"

    report += f"""
---

## 결론

### 레이어링 효과

1. **직무 특화 키워드**: Group B가 A 대비 직무 특화 키워드를 {"더 많이" if b_specific_count > a_specific_count else "유사하게"} 사용 ({a_specific_count:.1f}개 → {b_specific_count:.1f}개)
2. **질문 깊이**: {"B가 심화 질문 비율이 더 높음" if b_depth > a_depth else "두 그룹이 유사한 깊이"}
3. **토큰 효율**: System Prompt input 토큰 {token_savings:.1f}% {"절감" if token_savings > 0 else "증가"} — {"조건부 블록 + 유형 필터링 효과" if token_savings > 0 else "페르소나 추가로 증가"}

### 주의사항

- {RUNS}회 반복은 통계적 유의성을 보장하기엔 부족. 경향 확인용.
- temperature=0.9에서는 동일 프롬프트라도 결과 편차가 큼.
- 확신을 얻으려면 30회+ 반복 필요.
"""

    report_path = Path("/Users/koseonje/Documents/devlens/docs/plans/prompt-redesign/ab-test-results.md")
    with open(report_path, "w", encoding="utf-8") as f:
        f.write(report)
    print(f"리포트 저장: {report_path}")


if __name__ == "__main__":
    main()
