# Plan 01: Gemini 프롬프트에 답변 구조 + 태도 인상 섹션 추가

> 상태: Draft
> 작성일: 2026-04-01

## Why

현재 Gemini 음성 분석 프롬프트는 전사, 언어 평가, 기술 피드백, 음성 특성만 분석한다. "답변을 조리있게 전달했는가"(구조)와 "면접관이 봤을 때 태도/말투가 어떻게 보이는가"(태도 인상) 관점이 빠져 있어, 사용자가 가장 궁금해하는 피드백을 제공하지 못한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `lambda/analysis/analyzers/gemini_analyzer.py` | `_ANSWER_SYSTEM_TEMPLATE`에 structure/attitude 프롬프트 섹션 추가, `_ANSWER_USER_TEMPLATE` JSON 스키마 확장, `_FALLBACK_ANSWER`에 기본값 추가 |

## 상세

### 1. `_ANSWER_SYSTEM_TEMPLATE`에 추가할 프롬프트 섹션

기존 `### 2.5 기술 피드백` 뒤, `### 3. 음성 특성` 앞에 삽입:

```
### 2.6 답변 구조 (structure)
- level: 답변이 논리적이고 알아듣기 쉬운 구조로 전달되었는지 평가
  - GOOD: 서론-본론-결론, 개념→원리→실무적용 등 명확한 흐름. 듣는 사람이 따라가기 쉬움
  - AVERAGE: 대체로 흐름은 있으나 일부 산만하거나 핵심이 늦게 나옴
  - NEEDS_IMPROVEMENT: 주제가 뒤섞이거나 두서없이 나열. 듣는 사람이 따라가기 어려움
- comment: 구조적 관점에서 1-2문장 피드백 (무엇이 좋았는지/어떻게 구조를 잡으면 좋을지)
```

기존 `### 3. 음성 특성` 뒤, `### 4. 종합` **앞**에 삽입 (종합보다 먼저 평가):

```
### 3.5 태도 인상 (attitude)
면접관의 관점에서, 이 지원자의 전반적인 태도와 말투가 어떤 인상을 주는지 **음성 톤과 발화 내용만으로** 평가합니다. (시각적 요소는 별도 Vision 분석에서 다룹니다.)

아래와 같은 관점을 종합적으로 고려하세요:
- 자신감이 있는가, 아니면 불확실하고 망설이는가?
- 진지하고 차분한가, 아니면 지나치게 가볍거나 장난치는 듯한가?
- 당당하게 어필하는가, 아니면 지나치게 겸손하거나 자기비하적인가?
- 면접에 적합한 격식과 태도를 갖추고 있는가?

- comment 작성 규칙: 반드시 아래 3줄 형식으로 작성:
  ✓ {면접관에게 긍정적으로 보이는 점 1문장}
  △ {면접관에게 부정적으로 보일 수 있는 점 1문장}
  → {구체적 개선 방법 1문장}

주의: 단순히 "자신감이 부족합니다"가 아니라, 구체적으로 어떤 표현/어투에서 그렇게 느껴지는지 근거를 제시하세요.
예시: "△ '~인 것 같아요', '아마~' 등의 표현이 반복되어 확신이 부족한 인상을 줍니다"
```

기존 `### 4. 종합` 프롬프트에 다음 문구 추가:
```
주의: 태도 인상은 이미 attitude 블록에서 평가했으므로 여기서 반복하지 마세요. 언어 + 음성을 종합한 내용 품질 피드백만 작성하세요.
```

### 2. `_ANSWER_USER_TEMPLATE` JSON 스키마 확장

기존:
```
{{"transcript":"","verbal":{{"comment":""}},"technical":...,"vocal":...,"overallComment":""}}
```

변경 (**attitude를 overallComment 앞에 배치** — Gemini가 태도를 먼저 평가한 뒤 종합 작성):
```
{{"transcript":"","verbal":{{"comment":""}},"structure":{{"level":"","comment":""}},"technical":...,"vocal":...,"attitude":{{"comment":""}},"overallComment":""}}
```

### 3. 기존 프롬프트 품질 개선

기존 섹션들의 코멘트 품질도 함께 개선한다:

**(a) `### 2. 언어 평가 (verbal)` comment 가이드 보강:**

기존:
```
- comment 작성 규칙: 반드시 아래 3줄 형식으로 작성:
  ✓ {{잘한 점 1문장}}
  △ {{보완할 점 1문장}}
  → {{구체적 개선 방법 1문장}}
```

변경 (구체성 요구 추가):
```
- comment 작성 규칙: 반드시 아래 3줄 형식으로 작성:
  ✓ {{잘한 점 1문장 — 구체적으로 어떤 개념/표현이 좋았는지}}
  △ {{보완할 점 1문장 — 빠졌거나 부정확한 구체적 내용}}
  → {{개선 방법 1문장 — "~를 추가하면 좋겠습니다" 형태로 실행 가능하게}}

주의: "전반적으로 잘 답변했습니다" 같은 모호한 피드백 금지. 반드시 답변에서 언급된 구체적 내용을 인용하세요.
```

**(b) `### 3. 음성 특성 (vocal)` comment 가이드 보강:**

기존 comment 규칙 뒤에 추가:
```
주의: "속도가 적절합니다" 같은 단순 반복 금지. 어떤 부분에서 빨랐는지/느렸는지, 어떤 표현에서 자신감이 느껴졌는지 구체적으로 언급하세요.
```

### 4. `_FALLBACK_ANSWER`에 기본값 추가

```python
"structure": {"level": "AVERAGE", "comment": ""},
"attitude": {"comment": ""},
```

## 담당 에이전트

- Implement: `backend` — Lambda 프롬프트 수정
- Review: `code-reviewer` — 프롬프트 품질, JSON 스키마 일관성

## 검증

- Gemini API에 테스트 오디오 전송 → 응답에 `structure.level`, `structure.comment`, `attitude.comment` 포함 확인
- `structure.level`이 GOOD/AVERAGE/NEEDS_IMPROVEMENT 중 하나인지 확인
- `attitude.comment`가 ✓△→ 3줄 형식인지 확인
- 기존 필드(transcript, verbal, technical, vocal, overallComment)가 깨지지 않는지 확인
- `progress.md` 상태 업데이트 (Task 1 → Completed)
