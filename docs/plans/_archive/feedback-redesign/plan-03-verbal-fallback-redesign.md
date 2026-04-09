# Plan 03: Verbal 폴백 프롬프트 재설계

> 상태: Draft
> 작성일: 2026-03-30

## Why

Gemini 실패 시 GPT-4o `verbal_analyzer`가 폴백으로 동작한다. Gemini 프롬프트와 동일한 응답 구조(score 제거, 이모지+불릿 코멘트)로 맞춰야 BE/FE에서 일관되게 처리할 수 있다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `lambda/analysis/analyzers/verbal_analyzer.py` | _SYSTEM_PROMPT 변경 (score 제거, 코멘트 포맷 변경) |
| `lambda/analysis/analyzers/verbal_prompt_factory.py` | SYSTEM_TEMPLATE 변경 (score 제거, 코멘트 포맷 변경) |

## 상세

### verbal_analyzer.py _SYSTEM_PROMPT 변경

**제거:**
```
1. 답변 논리성 (verbal_score: 0-100)
```

**변경:**
- `verbal_score` 필드 제거
- `comment`를 이모지+불릿 포맷으로 지시
- 응답 JSON에서 `verbal_score` 키 삭제

**변경 후 응답:**
```json
{
  "filler_word_count": 3,
  "tone_label": "PROFESSIONAL",
  "tone_comment": "✓ 격식체 유지\n△ ...\n→ ...",
  "comment": "✓ 잘한 점\n△ 보완할 점\n→ 개선 방법"
}
```

### verbal_prompt_factory.py SYSTEM_TEMPLATE 변경

**제거:**
```
1. verbal_score(0-100): 90+=핵심정확+기술깊이, ...
   평가요소: 핵심답변포함, 구조화(STAR), 기술키워드정확성, 논리흐름, 구체적수치/사례
```

**변경:**
- score 관련 평가 기준 제거
- comment에 이모지+불릿 포맷 지시 추가

## 담당 에이전트

- Implement: `backend` — 폴백 프롬프트 수정
- Review: `code-reviewer` — Gemini 응답과의 구조 일관성 확인

## 검증

- verbal_analyzer 단독 호출 시 score 없이 응답하는지 확인
- 코멘트가 이모지+불릿 포맷인지 확인
- `progress.md` 상태 업데이트 (Task 3 → Completed)
