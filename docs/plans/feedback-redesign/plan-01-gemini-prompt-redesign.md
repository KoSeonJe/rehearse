# Plan 01: Gemini 프롬프트 재설계

> 상태: Draft
> 작성일: 2026-03-30

## Why

현재 Gemini 프롬프트가 `verbal.score`(0-100)를 생성하지만 판단 기준이 애매하고, 답변 내용의 기술적 정확성/코칭 피드백이 없다. 면접 유형(기술/경험/이력서)에 따른 피드백 분기도 없어 모든 유형에 동일한 관점으로 분석한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `lambda/analysis/analyzers/gemini_analyzer.py` | 응답 구조 변경 (score 제거, technical 블록 추가, toneConfidenceLevel 라벨 전환, 코멘트 이모지+불릿 포맷) |
| `lambda/analysis/analyzers/verbal_prompt_factory.py` | 유형별 분기 프롬프트 추가 (기술/경험/이력서), SYSTEM_TEMPLATE 재설계 |
| `lambda/analysis/analyzers/prompts.py` | 공통 프롬프트 상수 변경 (필요 시) |

## 상세

### 1. 유형별 프롬프트 분기

`verbal_prompt_factory.py`에 InterviewType → 피드백 관점 매핑 추가:

```python
FEEDBACK_PERSPECTIVES = {
    "TECHNICAL": """
## 기술 피드백 관점
- accuracyIssues: 답변에서 기술적으로 틀리거나 부정확한 내용을 찾아 지적. claim(사용자가 말한 내용)과 correction(정확한 내용)을 쌍으로 제시
- coaching.structure: 개념→원리→실무적용 순서로 설명 구조 코칭
- coaching.improvement: 빠진 핵심 개념, 보충하면 좋을 내용 제시
""",
    "BEHAVIORAL": """
## 경험 피드백 관점
- accuracyIssues: 사용하지 않음 (빈 배열)
- coaching.structure: STAR 기법(상황→과제→행동→결과) 적용 여부 코칭
- coaching.improvement: 본인 역할/기여의 구체성, 수치화 가능 여부 제시
""",
    "RESUME_BASED": """
## 이력서 기반 피드백 관점
- accuracyIssues: 사용하지 않음 (빈 배열)
- coaching.structure: 프로젝트 배경→본인 역할→기술적 의사결정→결과 흐름 코칭
- coaching.improvement: 기술 선택 이유(대안 비교), 기여도 명확성 제시
""",
}

INTERVIEW_TYPE_TO_PERSPECTIVE = {
    "CS_FUNDAMENTAL": "TECHNICAL",
    "LANGUAGE_FRAMEWORK": "TECHNICAL",
    "SYSTEM_DESIGN": "TECHNICAL",
    "FULLSTACK_STACK": "TECHNICAL",
    "UI_FRAMEWORK": "TECHNICAL",
    "BROWSER_PERFORMANCE": "TECHNICAL",
    "INFRA_CICD": "TECHNICAL",
    "CLOUD": "TECHNICAL",
    "DATA_PIPELINE": "TECHNICAL",
    "SQL_MODELING": "TECHNICAL",
    "BEHAVIORAL": "BEHAVIORAL",
    "RESUME_BASED": "RESUME_BASED",
}
```

### 2. Gemini 응답 구조 변경

```json
{
  "transcript": "...",
  "verbal": {
    "comment": "✓ 잘한 점\n△ 보완할 점\n→ 개선 방법"
  },
  "technical": {
    "accuracyIssues": [
      { "claim": "사용자가 말한 내용", "correction": "정확한 내용" }
    ],
    "coaching": {
      "structure": "답변 구조 코칭",
      "improvement": "구체적 개선 방향"
    }
  },
  "vocal": {
    "fillerWords": [],
    "speechPace": "빠름|적절|느림",
    "toneConfidenceLevel": "GOOD|AVERAGE|NEEDS_IMPROVEMENT",
    "emotionLabel": "자신감|긴장|평온|불안",
    "comment": "✓ 잘한 점\n△ 보완할 점\n→ 개선 방법"
  },
  "overallComment": ""
}
```

### 3. 코멘트 포맷 프롬프트 지시

모든 comment 필드에 아래 지시 추가:
```
comment 작성 규칙:
- 반드시 아래 3줄 형식으로 작성:
  ✓ {잘한 점 1문장}
  △ {보완할 점 1문장}
  → {구체적 개선 방법 1문장}
- 각 줄은 이모지로 시작, 간결하게 작성
```

### 4. interviewType 전달 경로

현재 Lambda는 BE 내부 API로부터 질문 데이터를 받는다. `interviewType`을 함께 전달해야 한다:
- Lambda가 BE 내부 API 호출 시 응답에서 `interviewType` 조회
- `answers_data`에서 추출하여 Gemini/Verbal 프롬프트에 전달

## 담당 에이전트

- Implement: `backend` — Lambda 프롬프트 재설계 + 응답 파싱 수정
- Review: `code-reviewer` — 프롬프트 품질, 응답 파싱 안전성

## 검증

- Gemini에 기술/경험/이력서 각 유형 샘플 질문+답변을 입력하여 응답 구조 확인
- accuracyIssues가 기술 유형에서만 채워지는지 확인
- 코멘트가 이모지+불릿 포맷으로 나오는지 확인
- 기존 폴백(verbal_analyzer)도 동일 포맷으로 응답하는지 확인
- `progress.md` 상태 업데이트 (Task 1 → Completed)
