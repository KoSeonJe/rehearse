#!/usr/bin/env python3
"""
Haiku 가 일부 영어 토큰을 그대로 남긴 경우를 보정하는 결정론적 후처리.

- tts_mapping.json 의 tts_content 에 대해 주요 영어 기술 토큰을 한국어 발음으로 치환.
- 괄호 안 내용(e.g. (HOC))은 그대로 유지 — 단, 괄호 없는 위치에만 치환되도록 word-boundary 처리.
- @Annotation 패턴은 "어노테이션 <phonetic>" 로 변환.

사용법:
    python3 scripts/seed/postprocess_tts.py \
        --mapping scripts/seed/out/tts_mapping.json \
        --out scripts/seed/out/tts_mapping.json
"""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

# 우선순위 높은 순으로 (긴 것 먼저)
PHONETICS: list[tuple[str, str]] = [
    # 프레임워크/라이브러리 (긴 것 먼저)
    ("Spring Boot", "스프링 부트"),
    ("Spring Framework", "스프링 프레임워크"),
    ("Spring Cloud", "스프링 클라우드"),
    ("Spring Security", "스프링 시큐리티"),
    ("Spring Data", "스프링 데이터"),
    ("Node.js", "노드제이에스"),
    ("NestJS", "네스트제이에스"),
    ("FastAPI", "패스트에이피아이"),
    ("Django", "장고"),
    ("TypeORM", "타입오알엠"),
    ("SQLAlchemy", "에스큐엘 알케미"),
    ("GraphQL", "그래프큐엘"),
    ("WebSocket", "웹소켓"),
    ("WebFlux", "웹플럭스"),
    ("Kubernetes", "쿠버네티스"),
    ("CloudFront", "클라우드프론트"),
    ("DynamoDB", "다이나모디비"),
    ("MongoDB", "몽고디비"),
    ("PostgreSQL", "포스트그레스큐엘"),
    ("MySQL", "마이에스큐엘"),
    ("InnoDB", "이노디비"),
    ("ElasticSearch", "엘라스틱서치"),
    ("Elasticsearch", "엘라스틱서치"),
    ("Prometheus", "프로메테우스"),
    ("Grafana", "그라파나"),
    ("Terraform", "테라폼"),
    ("Jenkins", "젠킨스"),
    ("TypeScript", "타입스크립트"),
    ("JavaScript", "자바스크립트"),
    ("Higher-Order Component", "하이어 오더 컴포넌트"),
    ("Tree-shaking", "트리 셰이킹"),
    ("Tree shaking", "트리 셰이킹"),
    # 브랜드
    ("Kotlin", "코틀린"),
    ("Python", "파이썬"),
    ("Pinia", "피니아"),
    ("Zustand", "주스탄드"),
    ("Redis", "레디스"),
    ("Kafka", "카프카"),
    ("Docker", "도커"),
    ("Lambda", "람다"),
    ("Nginx", "엔진엑스"),
    ("Redux", "리덕스"),
    ("Vuex", "뷰엑스"),
    ("GraphQL", "그래프큐엘"),
    ("Vite", "바이트"),
    ("Webpack", "웹팩"),
    ("Jest", "제스트"),
    ("Vitest", "바이테스트"),
    ("React", "리액트"),
    ("Vue", "뷰"),
    ("Java", "자바"),
    ("Hook", "훅"),
    # 약어 (짧은 것은 단어 경계로만 매칭)
]

# 단어 경계 기반 약어 치환 (대소문자 구분 — 정확히 일치할 때만)
ABBREV_EXACT: list[tuple[str, str]] = [
    ("CI/CD", "씨아이 씨디"),
    ("CI", "씨아이"),
    ("CD", "씨디"),
    ("IoC", "아이오씨"),
    ("DI", "디아이"),
    ("MVC", "엠브이씨"),
    ("API", "에이피아이"),
    ("REST", "레스트"),
    ("HTTP", "에이치티티피"),
    ("HTTPS", "에이치티티피에스"),
    ("SQL", "에스큐엘"),
    ("JPA", "제이피에이"),
    ("ORM", "오알엠"),
    ("JWT", "제이더블유티"),
    ("CORS", "코어스"),
    ("TCP", "티씨피"),
    ("UDP", "유디피"),
    ("DNS", "디엔에스"),
    ("IP", "아이피"),
    ("URL", "유알엘"),
    ("JSON", "제이슨"),
    ("XML", "엑스엠엘"),
    ("CSS", "씨에스에스"),
    ("HTML", "에이치티엠엘"),
    ("CQRS", "씨큐알에스"),
    ("E2E", "이투이"),
    ("SSE", "에스에스이"),
    ("SQS", "에스큐에스"),
    ("R2DBC", "알투디비씨"),
    ("UI", "유아이"),
    ("UX", "유엑스"),
    ("SPA", "에스피에이"),
    ("SSR", "에스에스알"),
    ("CSR", "씨에스알"),
    ("AWS", "에이더블유에스"),
    ("K8s", "쿠버네티스"),
    ("GPU", "지피유"),
    ("CPU", "씨피유"),
    ("RAM", "램"),
    ("OAuth2", "오오쓰 투"),
    ("OAuth", "오오쓰"),
    ("SaaS", "사스"),
    ("IaaS", "아이에이에스"),
    ("PaaS", "파스"),
    ("VPC", "브이피씨"),
    ("ECR", "이씨알"),
    ("ECS", "이씨에스"),
    ("EKS", "이케이에스"),
    ("S3", "에스쓰리"),
    ("EC2", "이씨투"),
    ("RDS", "알디에스"),
    ("HA", "에이치에이"),
    ("DR", "디알"),
    ("i18n", "국제화"),
    ("a11y", "접근성"),
    ("TTL", "티티엘"),
    ("TLS", "티엘에스"),
    ("SSL", "에스에스엘"),
    ("QPS", "큐피에스"),
    ("RPS", "알피에스"),
    ("SLA", "에스엘에이"),
    ("SLO", "에스엘오"),
    ("SLI", "에스엘아이"),
    ("MTTR", "엠티티알"),
    ("CDN", "씨디엔"),
    ("OS", "오에스"),
]


ANNOT_RE = re.compile(r"@([A-Z][A-Za-z0-9]+)")
# Haiku 가 이미 "어노테이션 <이름>" 으로 변환한 경우를 "<이름> 어노테이션" 으로 뒤집기
# 한국어/영문 토큰 모두 커버 (예: "어노테이션 트랜잭셔널", "어노테이션 RequestMapping")
ANNOT_REVERSE_RE = re.compile(r"어노테이션\s+([A-Za-z가-힣][A-Za-z가-힣0-9]*)")


def split_and_preserve_parens(text: str) -> list[tuple[str, bool]]:
    """문자열을 (chunk, is_protected) 리스트로 분할. 괄호 안은 protected=True."""
    out: list[tuple[str, bool]] = []
    buf = []
    depth = 0
    for ch in text:
        if ch == "(":
            if depth == 0 and buf:
                out.append(("".join(buf), False))
                buf = []
            buf.append(ch)
            depth += 1
        elif ch == ")":
            buf.append(ch)
            depth -= 1
            if depth == 0:
                out.append(("".join(buf), True))
                buf = []
        else:
            buf.append(ch)
    if buf:
        out.append(("".join(buf), depth > 0))
    return out


def convert_phrase(text: str, original_content: str = "") -> str:
    # 1) @Annotation 이 원문에 있을 때만 tts 안의 "어노테이션 X" 를 "X 어노테이션" 으로 뒤집기
    annot_names = ANNOT_RE.findall(original_content)
    for name in annot_names:
        # "어노테이션 <이름>" → "<이름> 어노테이션"
        pattern = re.compile(r"어노테이션\s+" + re.escape(name) + r"\b")
        text = pattern.sub(f"{name} 어노테이션", text)
    # 2) tts 안에 여전히 @X 가 남아있는 경우 "X 어노테이션" 으로 (Java 영문 어노테이션)
    text = ANNOT_RE.sub(lambda m: f"{m.group(1)} 어노테이션", text)
    # 2b) @<한글> 패턴 (Haiku 가 @를 남기고 이름만 번역한 경우)
    text = re.sub(r"@([\uAC00-\uD7A3]+)", lambda m: f"{m.group(1)} 어노테이션", text)
    # 2) 긴 용어 치환 (괄호 밖에서만)
    parts = split_and_preserve_parens(text)
    result_parts = []
    for chunk, protected in parts:
        if protected:
            result_parts.append(chunk)
            continue
        for src, dst in PHONETICS:
            # 단어 경계 대략적 처리: 앞뒤가 알파벳/숫자 아닐 때만
            pattern = re.compile(r"(?<![A-Za-z0-9])" + re.escape(src) + r"(?![A-Za-z0-9])")
            chunk = pattern.sub(dst, chunk)
        # 약어: 정확한 단어 경계 매칭
        for src, dst in ABBREV_EXACT:
            pattern = re.compile(r"(?<![A-Za-z0-9])" + re.escape(src) + r"(?![A-Za-z0-9])")
            chunk = pattern.sub(dst, chunk)
        result_parts.append(chunk)
    return "".join(result_parts)


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--mapping", required=True)
    ap.add_argument("--out", required=True)
    args = ap.parse_args()

    data = json.loads(Path(args.mapping).read_text("utf-8"))
    changed = 0
    for key, entry in data.items():
        old = entry["tts_content"]
        new = convert_phrase(old, entry.get("content", ""))
        if new != old:
            entry["tts_content"] = new
            changed += 1

    Path(args.out).write_text(json.dumps(data, ensure_ascii=False, indent=2), "utf-8")
    print(f"total={len(data)}, post-processed={changed}")


if __name__ == "__main__":
    main()
