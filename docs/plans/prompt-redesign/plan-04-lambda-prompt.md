# Phase 4: [Lambda] 언어/비언어 프롬프트 최적화

> **상태**: TODO
> **브랜치**: `feat/lambda-verbal-prompt`
> **PR**: PR-4 → develop
> **의존**: Phase 3 (PR-3 머지 후 — Internal API에서 position/techStack/level 제공)

---

## 개요

현재 Lambda의 `verbal_analyzer.py`는 범용 "면접 언어 분석 전문가" 프롬프트를 사용한다.
Phase 4에서는:

1. **VerbalPromptFactory** 도입 — Position × TechStack별 MINIMAL 페르소나 + 키워드 사전 조립
2. **v3 토큰 최적화** — 루브릭 압축, JSON 스키마 압축 (언어 분석 ~42% 절감)
3. **비언어 분석** — 직무 무관이므로 JSON 스키마 압축만 적용 (~44% 절감)

### 토큰 절감 예상

| 프롬프트 | 현재 | 최적화 | 절약률 |
|---------|------|-------|-------|
| 언어 분석 | ~720 tok | ~419 tok | 42% |
| 비언어 분석 | ~468 tok | ~263 tok | 44% |

---

## Task 4-1: VerbalPromptFactory 생성

- **Implement**: `backend` (Lambda Python)
- **Review**: `code-reviewer` — 프롬프트 품질, 키워드 사전 정확성

### 파일

- 신규: `lambda/analysis/analyzers/verbal_prompt_factory.py`

### 구현 상세

```python
"""Position × TechStack별 언어 분석 프롬프트 조립 (v3 최적화)"""

# Position별 기본 TechStack 매핑 (BE에서 null인 경우 사용)
DEFAULT_TECH_STACKS = {
    "BACKEND": "JAVA_SPRING",
    "FRONTEND": "REACT_TS",
    "DEVOPS": "AWS_K8S",
    "DATA_ENGINEER": "SPARK_AIRFLOW",
    "FULLSTACK": "REACT_SPRING",
}

# MINIMAL 페르소나 (v3 전략 1: 1문장)
MINIMAL_PERSONAS = {
    "BACKEND_JAVA_SPRING": "백엔드(Java/Spring) 면접 답변의 언어적 커뮤니케이션을 분석합니다.",
    "FRONTEND_REACT_TS": "프론트엔드(React/TypeScript) 면접 답변의 언어적 커뮤니케이션을 분석합니다.",
    "DEVOPS_AWS_K8S": "데브옵스(AWS/Kubernetes) 면접 답변의 언어적 커뮤니케이션을 분석합니다.",
    "DATA_ENGINEER_SPARK_AIRFLOW": "데이터 엔지니어(Spark/Airflow) 면접 답변의 언어적 커뮤니케이션을 분석합니다.",
    "FULLSTACK_REACT_SPRING": "풀스택(React + Spring) 면접 답변의 언어적 커뮤니케이션을 분석합니다.",
}

# 스택별 verbal expertise (키워드 사전 — 축약 금지)
VERBAL_EXPERTISE = {
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

    # DEVOPS_AWS_K8S, DATA_ENGINEER_SPARK_AIRFLOW, FULLSTACK_REACT_SPRING도 동일 구조로 추가
    # (background/prompt-redesign.md §3.3, §3.4, §3.5 + overlay 키워드 참조)
}

SYSTEM_TEMPLATE = """{minimal_persona}

## 전문 분야
{verbal_expertise}

## 평가
1. verbal_score(0-100): 90+=핵심정확+기술깊이, 70+=대체로양호, 50+=핵심빗나감, 30+=이해부족/오류, 0+=무관
   평가요소: 핵심답변포함, 구조화(STAR), 기술키워드정확성, 논리흐름, 구체적수치/사례
2. filler_word_count: "음","어","그","아","뭐","이제","약간","좀","그러니까" 등장 횟수
3. tone_label: PROFESSIONAL|CASUAL|HESITANT|CONFIDENT|VERBOSE
4. comment: 3-4문장. 키워드 사전 기반으로 기술 용어 정확성/오용도 comment에 포함하여 피드백

## 응답
JSON만 응답:
{{"verbal_score":0,"filler_word_count":0,"tone_label":"","tone_comment":"","comment":""}}"""

USER_TEMPLATE = """직무: {position} ({tech_stack}) | 레벨: {level}
질문: {question}
{model_answer_line}답변(STT): {transcript}"""


def build_system_prompt(position: str, tech_stack: str | None) -> str:
    effective_stack = tech_stack or DEFAULT_TECH_STACKS.get(position, "JAVA_SPRING")
    key = f"{position}_{effective_stack}"

    persona = MINIMAL_PERSONAS.get(key, f"{position} 면접 답변의 언어적 커뮤니케이션을 분석합니다.")
    expertise = VERBAL_EXPERTISE.get(key, "")

    return SYSTEM_TEMPLATE.format(
        minimal_persona=persona,
        verbal_expertise=expertise,
    )


def build_user_prompt(position: str, tech_stack: str | None, level: str,
                      question: str, transcript: str,
                      model_answer: str | None = None) -> str:
    effective_stack = tech_stack or DEFAULT_TECH_STACKS.get(position, "JAVA_SPRING")
    model_answer_line = f"모범답변: {model_answer}\n" if model_answer else ""
    return USER_TEMPLATE.format(
        position=position, tech_stack=effective_stack, level=level,
        question=question, model_answer_line=model_answer_line,
        transcript=transcript,
    )
```

---

## Task 4-2: verbal_analyzer.py 수정

- **Implement**: `backend` (Lambda Python)
- **Review**: `code-reviewer`

### 파일

- 수정: `lambda/analysis/analyzers/verbal_analyzer.py`

### 변경 사항

1. **import 추가**: `from analyzers.verbal_prompt_factory import build_system_prompt, build_user_prompt`

2. **analyze_verbal 시그니처 확장**:

```python
def analyze_verbal(question_text: str, transcript: str,
                   position: str | None = None,
                   tech_stack: str | None = None,
                   level: str | None = None,
                   model_answer: str | None = None) -> dict:
```

3. **프롬프트 조립 변경** (L50~L57):

```python
# 기존: 하드코딩 _SYSTEM_PROMPT + 직접 구성 user_prompt
# 변경: factory 사용 (position/tech_stack이 없으면 기존 범용 프롬프트 폴백)
if position:
    system_prompt = build_system_prompt(position, tech_stack)
    user_prompt = build_user_prompt(position, tech_stack, level or "JUNIOR",
                                     question_text, transcript, model_answer)
else:
    system_prompt = _SYSTEM_PROMPT  # 기존 범용 프롬프트 (하위 호환)
    user_prompt = f"질문: {question_text}\n답변(STT): {transcript}"
```

4. **기존 `_SYSTEM_PROMPT`**: 삭제하지 않고 유지 (position=None 폴백용)

---

## Task 4-3: handler.py 수정 — interview context 전달 + 시그니처 체인

- **Implement**: `backend` (Lambda Python)
- **Review**: `code-reviewer`

> **[H6 수정]** 실제 verbal 호출은 `_build_timestamp_feedbacks` → `_safe_verbal` → `analyze_verbal` 체인이다.
> 3개 함수 모두 시그니처 변경이 필요하다.

### 파일

- 수정: `lambda/analysis/handler.py`

### 변경 사항

#### 1. answers API 응답에서 context 추출 (`_run_pipeline` 내부)

```python
answers_data = _fetch_answers(interview_id, question_set_id)
position = answers_data.get("position")       # "BACKEND" 등
tech_stack = answers_data.get("techStack")     # "JAVA_SPRING" 등 or None
level = answers_data.get("level")              # "JUNIOR" 등
```

#### 2. `_build_timestamp_feedbacks` 시그니처 변경 (L119)

```python
# 기존
def _build_timestamp_feedbacks(answers, stt_result, vision_result, video_duration_ms):

# 변경: interview context 파라미터 추가
def _build_timestamp_feedbacks(answers, stt_result, vision_result, video_duration_ms,
                                position=None, tech_stack=None, level=None):
```

호출처도 변경:
```python
feedbacks = _build_timestamp_feedbacks(
    answers, stt_result, vision_result, video_duration_ms,
    position=position, tech_stack=tech_stack, level=level
)
```

#### 3. `_safe_verbal` 시그니처 변경 (L239 근처)

```python
# 기존
def _safe_verbal(question_text, transcript):

# 변경
def _safe_verbal(question_text, transcript, position=None, tech_stack=None,
                 level=None, model_answer=None):
    try:
        return analyze_verbal(question_text, transcript,
                              position=position, tech_stack=tech_stack,
                              level=level, model_answer=model_answer)
    except Exception as e:
        print(f"[Verbal] 분석 실패: {e}")
        return None
```

#### 4. `_build_timestamp_feedbacks` 내부 호출 변경 (L141)

```python
# 기존
verbal = _safe_verbal(question_text, transcript)

# 변경
verbal = _safe_verbal(question_text, transcript,
                      position=position, tech_stack=tech_stack,
                      level=level, model_answer=answer.get("modelAnswer"))
```

---

## Task 4-4: 비언어 분석 프롬프트 최적화

- **Implement**: `backend` (Lambda Python)
- **Review**: `code-reviewer`

### 파일

- 수정: `lambda/analysis/analyzers/vision_analyzer.py`

### 변경 사항

비언어 분석은 **직무/스택 무관** (독립 전문가)이므로 페르소나 변경 없음.
v3 최적화만 적용:

1. **`_SYSTEM_PROMPT` 교체** (L13~L34):

```python
_SYSTEM_PROMPT = """면접 영상 프레임의 비언어적 커뮤니케이션만 평가합니다. 답변 내용은 평가하지 않습니다.

## 평가
1. eye_contact_score(0-100): 90+=안정응시, 70+=간헐흐트러짐, 50+=자주딴곳, 30+=불안정, 0+=미응시
2. posture_score(0-100): 90+=바른자세유지, 70+=간헐구부정, 50+=불안정/흔듦, 30+=지속구부정, 0+=부적절
3. expression_label: CONFIDENT(자신감) | ENGAGED(몰입) | NEUTRAL(무표정) | NERVOUS(긴장) | UNCERTAIN(혼란)

## 주의
- 여러 프레임의 평균 경향 평가. 이상치에 과도한 가중치 금지.
- 사람 미확인 시 점수 50, comment에 설명.
- 한국 면접 문화 고려(차분함 긍정적).

## 응답
JSON만 응답:
{"eye_contact_score":0,"posture_score":0,"expression_label":"","comment":"한국어 2-3문장"}"""
```

변경 포인트:
- 역할 설명 축약: 2문장 → 1문장 (~30 tok 절약)
- 루브릭 압축: 5단계 상세 → 1줄 키워드 (~120 tok 절약)
- 주의사항 압축 (~30 tok)
- JSON 스키마 1줄 압축 (~25 tok)

2. **`_validate_result` 유지** — JSON 파싱 후 검증 로직은 변경 없음

---

## 검증

### 수동 테스트

```bash
# Lambda 로컬 실행 (Docker 또는 직접)
cd lambda/analysis
python -c "
from analyzers.verbal_prompt_factory import build_system_prompt
print(build_system_prompt('BACKEND', 'JAVA_SPRING'))
print('---')
print(build_system_prompt('FRONTEND', None))  # 기본 스택 REACT_TS
print('---')
print(build_system_prompt('BACKEND', None))   # 기본 스택 JAVA_SPRING
"
```

### 하위 호환 테스트

```python
# position=None → 기존 범용 프롬프트 사용
from analyzers.verbal_analyzer import analyze_verbal
result = analyze_verbal("테스트 질문", "테스트 답변")
# → 기존과 동일하게 동작해야 함
```

### E2E 테스트

1. 면접 생성 (techStack=JAVA_SPRING)
2. 녹화 + S3 업로드
3. Lambda 트리거 확인
4. Lambda 로그에서 verbal_analyzer가 JAVA_SPRING 키워드 사전을 사용하는지 확인
5. 피드백에 `keyword_usage` 필드 포함 확인

### Lambda 배포

```bash
# analysis Lambda 소스 레이어 재배포
cd lambda/analysis
zip -r source.zip *.py analyzers/ -x __pycache__/**
aws lambda update-function-code --function-name rehearse-analysis-dev \
    --zip-file fileb://source.zip
```

---

## JSON 응답 스키마

> **[C2 결정]** `keyword_usage` 별도 필드는 MVP에서 추가하지 않는다 (설계 판단 D8).
> 키워드 사전 기반 분석 결과는 `comment` 필드에 자연어로 포함된다.
> 기존 `tone_label`, `tone_comment`도 현재 BE에서 저장하지 않는 필드이므로 동일하게 처리.
> 추후 피드백 뷰어 고도화 시 keyword/tone 저장을 별도 Phase로 진행.

### verbal 응답 형식 (기존과 동일한 키 구조)

```json
{
  "verbal_score": 75,
  "filler_word_count": 3,
  "tone_label": "PROFESSIONAL",
  "tone_comment": "격식체를 잘 유지하며 명확하게 답변했습니다.",
  "comment": "N+1 문제를 fetch join과 EntityGraph로 해결하는 방법을 정확히 설명했습니다. 핵심 키워드(영속성 컨텍스트, 지연 로딩)를 적절히 사용했으나, BatchSize 설정에 대한 언급이 부족합니다."
}
```

> `comment`에 키워드 정확성/오용 피드백이 자연어로 포함됨 — BE 저장 체인 변경 불필요
