#!/usr/bin/env python3
"""
RESUME 트랙 model_answer 품질 측정 (정량 4종 + LLM-as-judge 정성).

본 스크립트는 단일 시점 (현재 작업 트리) 의 RESUME 3 모드 model_answer 품질을 측정해
마크다운 리포트를 산출한다. before/after 비교는 사용자가 두 시점 (예: 변경 전 commit /
변경 후 commit) 에서 각각 1회 실행 후 두 리포트를 비교하는 방식으로 운영한다.

리포트 헤더에 실행 timestamp + git commit SHA 를 포함하므로 어느 시점 결과인지 추적 가능.

## 측정 대상

5 fixture × 3 mode (OPENER / PLAYGROUND / INTERROGATION) — 각 fixture 의 call_type 매칭.
- session-resume-1 = resume_playground_opener
- session-resume-2 = resume_playground_responder
- session-resume-3 = resume_chain_interrogator (L3)
- session-resume-4 = resume_playground_opener
- session-resume-5 = resume_chain_interrogator (L3)

## 정량 메트릭 (4종)

1. 분량 200~300자 비율 (목표 ≥80%)
2. 가이드 톤 어구 출현률 (목표 0%)
3. 사용자 맥락 키워드 (projectName / chain topic / USER_ANSWER 핵심 명사) 포함률 (목표 ≥80%)
4. 구조 단서 (STAR / Situation / Task / Action / Result / 결정 / 근거 / 트레이드오프 / 학습) 포함률 (목표 ≥80%)

## 정성 메트릭 (LLM-as-judge)

Claude Sonnet 으로 4 criteria × 3 등급 (잘 / 보통 / 나쁨) 평가.
criteria = (a) 분량 적절성 (b) 1인칭 톤 일관성 (c) 사용자 맥락 반영 (d) 구조 단서 포함.
통과율 = (잘 + 보통) / 전체. 목표 ≥70%.

## 실행

```bash
export OPENAI_API_KEY=sk-...
export ANTHROPIC_API_KEY=sk-ant-...
python3 backend/eval/context/run_model_answer_quality.py
```

산출: backend/eval/context/reports/model-answer-quality-{YYYYMMDD-HHMMSS}.md

## 비용

5 fixture × 1 LLM 호출 (gpt-4o-mini) + 5 judge 호출 (claude sonnet) ≈ $0.10~0.30 / 회.
"""

from __future__ import annotations

import json
import os
import subprocess
import sys
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path
from typing import Optional

# ---------------------------------------------------------------------------
# 상수
# ---------------------------------------------------------------------------

GUIDE_TONE_PHRASES = ["해보세요", "하면 좋습니다", "이야기해보세요", "설명해보세요"]
FIRST_PERSON_ENDINGS = ["했습니다", "경험이 있습니다", "을 적용했습니다", "을 진행했습니다", "을 측정했습니다"]
STRUCTURE_SIGNALS = ["STAR", "Situation", "Task", "Action", "Result", "결정", "근거", "트레이드오프", "학습"]

LENGTH_MIN = 200
LENGTH_MAX = 300

LLM_MODEL = "gpt-4o-mini"
JUDGE_MODEL = "claude-sonnet-4-5-20250929"

ROOT = Path(__file__).resolve().parents[2]  # backend/
FIXTURES_DIR = Path(__file__).resolve().parent / "fixtures"
REPORTS_DIR = Path(__file__).resolve().parent / "reports"
TEMPLATE_DIR = ROOT / "src" / "main" / "resources" / "prompts" / "template" / "resume"


# ---------------------------------------------------------------------------
# 데이터 클래스
# ---------------------------------------------------------------------------

@dataclass
class CaseResult:
    fixture_name: str
    call_type: str
    model_answer: str
    project_name: Optional[str]
    chain_topic: Optional[str]
    user_answer_nouns: list = field(default_factory=list)
    judge_grades: dict = field(default_factory=dict)  # criteria → 잘/보통/나쁨
    judge_rationale: str = ""

    @property
    def length(self) -> int:
        return len(self.model_answer)

    @property
    def length_ok(self) -> bool:
        return LENGTH_MIN <= self.length <= LENGTH_MAX

    @property
    def has_guide_tone(self) -> bool:
        return any(p in self.model_answer for p in GUIDE_TONE_PHRASES)

    @property
    def has_context_keyword(self) -> bool:
        keywords = []
        if self.project_name:
            keywords.append(self.project_name)
        if self.chain_topic:
            keywords.append(self.chain_topic)
        keywords.extend(self.user_answer_nouns)
        return any(kw in self.model_answer for kw in keywords if kw)

    @property
    def has_structure_signal(self) -> bool:
        return any(s in self.model_answer for s in STRUCTURE_SIGNALS)


# ---------------------------------------------------------------------------
# 프롬프트 빌더 (Java InterviewContextBuilder + ResumePromptBuilder 미러)
# ---------------------------------------------------------------------------

def load_template(call_type: str) -> str:
    filename = call_type.replace("_", "-") + ".txt"
    p = TEMPLATE_DIR / filename
    return p.read_text(encoding="utf-8")


def build_user_prompt(session: dict) -> str:
    call_type = session["call_type"]
    hints = session.get("focusHints") or {}
    runtime = session.get("runtimeState") or {}

    if call_type == "resume_playground_opener":
        return (
            "<<<PROJECT_INFO>>>\n" + (hints.get("projectInfo") or "") +
            "\n<<<END_PROJECT_INFO>>>\n\n"
            "<<<OPENER_QUESTION>>>\n" + (hints.get("openerQuestion") or "") +
            "\n<<<END_OPENER_QUESTION>>>\n\n"
            "위 정보를 기반으로 Playground 오프너 질문을 JSON 한 객체로만 응답하세요."
        )
    if call_type == "resume_playground_responder":
        return (
            "<<<EXPECTED_CLAIMS>>>\n" + (hints.get("expectedClaims") or "") +
            "\n<<<END_EXPECTED_CLAIMS>>>\n\n"
            "<<<USER_ANSWER>>>\n" + (hints.get("userAnswer") or "") +
            "\n<<<END_USER_ANSWER>>>\n\n"
            f"PLAYGROUND_TURN_COUNT: {hints.get('playgroundTurnCount', runtime.get('playgroundTurns', 0))}\n"
            f"CUMULATIVE_UTTERANCE_LENGTH: {hints.get('cumulativeUtteranceLength', 0)}\n\n"
            "위 입력으로 Responder 결정과 다음 질문을 JSON 한 객체로만 응답하세요."
        )
    if call_type == "resume_chain_interrogator":
        return (
            "<<<PROJECT_NAME>>>\n" + (hints.get("projectName") or "") +
            "\n<<<END_PROJECT_NAME>>>\n\n"
            "<<<CURRENT_CHAIN>>>\n" + (hints.get("currentChain") or "") +
            "\n<<<END_CURRENT_CHAIN>>>\n\n"
            f"<<<CURRENT_LEVEL>>>\n{hints.get('currentLevel', runtime.get('currentLevel', 'L1'))}\n<<<END_CURRENT_LEVEL>>>\n\n"
            f"<<<ANSWER_QUALITY>>>\n{hints.get('answerQuality', 0)}\n<<<END_ANSWER_QUALITY>>>\n\n"
            "<<<USER_ANSWER>>>\n" + (hints.get("userAnswer") or "") +
            "\n<<<END_USER_ANSWER>>>\n\n"
            f"<<<CONSECUTIVE_STAY_COUNT>>>\n{hints.get('consecutiveStayCount', 0)}\n<<<END_CONSECUTIVE_STAY_COUNT>>>\n\n"
            "위 chain 상태에서 LEVEL_UP/LEVEL_STAY/CHAIN_SWITCH 결정과 다음 질문을 JSON 한 객체로만 응답하세요."
        )
    raise ValueError(f"unsupported call_type: {call_type}")


# ---------------------------------------------------------------------------
# OpenAI / Anthropic 호출
# ---------------------------------------------------------------------------

def call_openai(system_prompt: str, user_prompt: str) -> str:
    try:
        from openai import OpenAI
    except ImportError:
        sys.exit("[error] openai 패키지 미설치 — pip install openai")

    client = OpenAI()
    resp = client.chat.completions.create(
        model=LLM_MODEL,
        messages=[
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt},
        ],
        temperature=0.7,
        max_tokens=1200,
        response_format={"type": "json_object"},
    )
    content = resp.choices[0].message.content or ""
    try:
        parsed = json.loads(content)
        return parsed.get("model_answer") or ""
    except json.JSONDecodeError:
        return ""


def call_judge(model_answer: str, fixture_name: str, call_type: str,
               project_name: Optional[str], chain_topic: Optional[str]) -> tuple[dict, str]:
    try:
        from anthropic import Anthropic
    except ImportError:
        sys.exit("[error] anthropic 패키지 미설치 — pip install anthropic")

    client = Anthropic()
    rubric = (
        "당신은 한국어 개발자 기술 면접 model_answer 품질 평가자입니다. "
        "아래 model_answer 출력을 4 criteria 로 평가하세요. 각 criteria 는 '잘' / '보통' / '나쁨' 3 등급.\n\n"
        "## 평가 criteria\n"
        "(a) length: 한국어 200~300자 범위에 적합한가\n"
        "(b) tone: 1인칭 답변 예시 톤 ('~했습니다', '~경험이 있습니다') 일관성 — 가이드 톤 ('~해보세요') 부재\n"
        "(c) context: 사용자 맥락 (projectName / chain topic / 직전 발화 핵심 명사) 반영 여부\n"
        "(d) structure: 답변 구조 단서 (STAR / 결정 근거 / 트레이드오프 / 학습 중 1+) 포함 여부\n\n"
        "## 출력 형식 (JSON 한 객체만, 마크다운 / 코드펜스 금지)\n"
        '{"length": "잘|보통|나쁨", "tone": "잘|보통|나쁨", "context": "잘|보통|나쁨", "structure": "잘|보통|나쁨", "rationale": "각 criteria 평가 근거 1~2문장"}\n\n'
        f"## 평가 대상\n"
        f"fixture: {fixture_name}\n"
        f"call_type: {call_type}\n"
        f"project_name: {project_name or '(없음)'}\n"
        f"chain_topic: {chain_topic or '(없음)'}\n\n"
        f"## model_answer\n{model_answer}\n"
    )
    resp = client.messages.create(
        model=JUDGE_MODEL,
        max_tokens=600,
        messages=[{"role": "user", "content": rubric}],
    )
    raw = resp.content[0].text if resp.content else ""
    try:
        parsed = json.loads(raw)
        grades = {k: parsed.get(k, "나쁨") for k in ("length", "tone", "context", "structure")}
        rationale = parsed.get("rationale", "")
        return grades, rationale
    except json.JSONDecodeError:
        return {"length": "나쁨", "tone": "나쁨", "context": "나쁨", "structure": "나쁨"}, f"(parse_failed) {raw[:200]}"


# ---------------------------------------------------------------------------
# 보조 — fixture 메타 추출
# ---------------------------------------------------------------------------

def extract_user_answer_nouns(session: dict) -> list:
    hints = session.get("focusHints") or {}
    project_info = hints.get("projectInfo") or ""
    user_answer = hints.get("userAnswer") or ""
    nouns = []
    # OPENER fixture = projectInfo 의 스택 키워드
    for tech in ("Spring Boot", "Redis", "Redisson", "Kafka", "MySQL", "WebSocket"):
        if tech in project_info or tech in user_answer:
            nouns.append(tech)
    # 도메인 명사 — fixture 별 수동 매핑보다는 휴리스틱
    for kw in ("결제 시스템", "동시성", "분산락", "partition", "hot room", "hot key", "채팅", "메시지 브로커"):
        if kw in user_answer:
            nouns.append(kw)
    return nouns


def extract_project_name(session: dict) -> Optional[str]:
    hints = session.get("focusHints") or {}
    if hints.get("projectName"):
        return hints["projectName"]
    project_info = hints.get("projectInfo") or ""
    if "프로젝트명:" in project_info:
        # "프로젝트명: X. 기간:" 패턴
        after = project_info.split("프로젝트명:", 1)[1]
        return after.split(".", 1)[0].strip()
    return None


def extract_chain_topic(session: dict) -> Optional[str]:
    runtime = session.get("runtimeState") or {}
    tracker = runtime.get("chainStateTracker") or {}
    return tracker.get("currentChainTopic")


# ---------------------------------------------------------------------------
# 리포트 산출
# ---------------------------------------------------------------------------

def get_commit_sha() -> str:
    try:
        return subprocess.check_output(
            ["git", "rev-parse", "--short", "HEAD"],
            cwd=ROOT, stderr=subprocess.DEVNULL
        ).decode().strip()
    except Exception:
        return "unknown"


def render_report(results: list, timestamp: str, commit_sha: str) -> str:
    n = len(results)
    if n == 0:
        return "# Model Answer Quality Report\n\n(no results)\n"

    length_pass = sum(1 for r in results if r.length_ok)
    guide_violation = sum(1 for r in results if r.has_guide_tone)
    context_pass = sum(1 for r in results if r.has_context_keyword)
    structure_pass = sum(1 for r in results if r.has_structure_signal)

    judge_count = {c: {"잘": 0, "보통": 0, "나쁨": 0} for c in ("length", "tone", "context", "structure")}
    for r in results:
        for c, grade in r.judge_grades.items():
            if grade in judge_count[c]:
                judge_count[c][grade] += 1

    lines = []
    lines.append("# Model Answer Quality Report")
    lines.append("")
    lines.append(f"- timestamp: {timestamp}")
    lines.append(f"- commit: {commit_sha}")
    lines.append(f"- fixture count: {n}")
    lines.append(f"- LLM model: {LLM_MODEL}")
    lines.append(f"- judge model: {JUDGE_MODEL}")
    lines.append("")
    lines.append("## 정량 메트릭")
    lines.append("")
    lines.append("| 메트릭 | 통과 | 비율 | 목표 |")
    lines.append("|---|---|---|---|")
    lines.append(f"| 분량 200~300자 | {length_pass}/{n} | {length_pass*100//n}% | ≥80% |")
    lines.append(f"| 가이드 톤 부재 | {n - guide_violation}/{n} | {(n - guide_violation)*100//n}% | 100% (출현 0%) |")
    lines.append(f"| 맥락 키워드 포함 | {context_pass}/{n} | {context_pass*100//n}% | ≥80% |")
    lines.append(f"| 구조 단서 포함 | {structure_pass}/{n} | {structure_pass*100//n}% | ≥80% |")
    lines.append("")
    lines.append("## 정성 메트릭 (LLM-as-judge)")
    lines.append("")
    lines.append("| criteria | 잘 | 보통 | 나쁨 | 통과율 (잘+보통) |")
    lines.append("|---|---|---|---|---|")
    for c in ("length", "tone", "context", "structure"):
        good = judge_count[c]["잘"]
        ok = judge_count[c]["보통"]
        bad = judge_count[c]["나쁨"]
        pass_pct = (good + ok) * 100 // n if n > 0 else 0
        lines.append(f"| {c} | {good} | {ok} | {bad} | {pass_pct}% |")
    lines.append("")
    lines.append("## 케이스별 출력 샘플")
    lines.append("")
    for r in results:
        lines.append(f"### {r.fixture_name} ({r.call_type})")
        lines.append("")
        lines.append(f"- length: {r.length} chars (range_ok={r.length_ok})")
        lines.append(f"- guide_tone: {r.has_guide_tone}")
        lines.append(f"- context_keyword: {r.has_context_keyword} (project={r.project_name}, chain={r.chain_topic}, nouns={r.user_answer_nouns})")
        lines.append(f"- structure_signal: {r.has_structure_signal}")
        lines.append(f"- judge: {r.judge_grades}")
        if r.judge_rationale:
            lines.append(f"- judge rationale: {r.judge_rationale}")
        lines.append("")
        lines.append("```")
        lines.append(r.model_answer)
        lines.append("```")
        lines.append("")
    return "\n".join(lines) + "\n"


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main():
    if not os.environ.get("OPENAI_API_KEY"):
        sys.exit("[error] OPENAI_API_KEY 환경변수 필요 — RESUME 프롬프트 LLM 호출")
    if not os.environ.get("ANTHROPIC_API_KEY"):
        sys.exit("[error] ANTHROPIC_API_KEY 환경변수 필요 — judge 호출")

    fixtures = sorted(FIXTURES_DIR.glob("session-resume-*.json"))
    if not fixtures:
        sys.exit(f"[error] fixture 없음: {FIXTURES_DIR}")

    print(f"[info] {len(fixtures)} fixture 발견 — model_answer 품질 측정 시작")
    results: list[CaseResult] = []
    for fx in fixtures:
        session = json.loads(fx.read_text(encoding="utf-8"))
        call_type = session["call_type"]
        print(f"  [{fx.name}] call_type={call_type}", flush=True)

        system_prompt = load_template(call_type)
        user_prompt = build_user_prompt(session)
        model_answer = call_openai(system_prompt, user_prompt)

        if not model_answer:
            print(f"    [warn] model_answer 누락 — skip judge", flush=True)
            results.append(CaseResult(
                fixture_name=fx.name, call_type=call_type, model_answer="",
                project_name=extract_project_name(session),
                chain_topic=extract_chain_topic(session),
                user_answer_nouns=extract_user_answer_nouns(session),
                judge_grades={"length": "나쁨", "tone": "나쁨", "context": "나쁨", "structure": "나쁨"},
                judge_rationale="(model_answer 빈 출력 — judge 미실행)",
            ))
            continue

        project_name = extract_project_name(session)
        chain_topic = extract_chain_topic(session)
        user_answer_nouns = extract_user_answer_nouns(session)
        grades, rationale = call_judge(model_answer, fx.name, call_type, project_name, chain_topic)

        results.append(CaseResult(
            fixture_name=fx.name, call_type=call_type, model_answer=model_answer,
            project_name=project_name, chain_topic=chain_topic,
            user_answer_nouns=user_answer_nouns,
            judge_grades=grades, judge_rationale=rationale,
        ))

    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    file_stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    commit_sha = get_commit_sha()
    REPORTS_DIR.mkdir(parents=True, exist_ok=True)
    report_path = REPORTS_DIR / f"model-answer-quality-{file_stamp}.md"
    report_path.write_text(render_report(results, timestamp, commit_sha), encoding="utf-8")
    print(f"[done] 리포트 산출: {report_path}")


if __name__ == "__main__":
    main()
