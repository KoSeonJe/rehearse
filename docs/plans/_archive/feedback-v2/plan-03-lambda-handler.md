# Plan 03: Lambda handler 신규 필드 매핑

> 상태: Draft
> 작성일: 2026-04-01

## Why

Plan 01에서 Gemini 프롬프트가 `structure`와 `attitude` 블록을 새로 생성하지만, handler.py의 피드백 조립 로직이 이 필드를 BE API로 전달하지 않으면 데이터가 유실된다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `lambda/analysis/handler.py` | Gemini 경로 + Legacy 경로에서 신규 필드 매핑 추가 + 기본값 초기화 |
| `lambda/analysis/analyzers/verbal_prompt_factory.py` | `SYSTEM_TEMPLATE`의 JSON 스키마 + 평가 섹션에 structure/attitude 추가 |
| `lambda/analysis/analyzers/verbal_analyzer.py` | `_SYSTEM_PROMPT`의 JSON 스키마에 structure/attitude 키 추가 + 파싱 대응 |

## 상세

### 1. handler.py — Gemini 경로: 기본값 초기화 + 필드 추출

**`_run_gemini_pipeline`의 피드백 조립부** (현재 line 240-270):

fb dict 생성 직후(line 244)에 기본값 None 설정 → `_safe_gemini_audio()`가 None 반환해도 키 보장:

```python
# fb dict 초기화 (line 244 직후, if gemini: 블록 바깥)
fb["structureLevel"] = None
fb["structureComment"] = None
fb["attitudeComment"] = None
```

`if gemini:` 블록 안, `fb["coachingImprovement"]` (line 261) 뒤에 삽입:

```python
# Gemini 응답에서 신규 필드 추출
structure = gemini.get("structure", {})
fb["structureLevel"] = structure.get("level")
fb["structureComment"] = structure.get("comment")

attitude = gemini.get("attitude", {})
fb["attitudeComment"] = attitude.get("comment")
```

### 2. handler.py — Legacy 경로: `_build_timestamp_feedbacks` 함수 수정

**`_build_timestamp_feedbacks`** (현재 line 312-375)도 동일하게 수정해야 한다.

fb dict 생성 직후(line 351)에 기본값 설정:

```python
# fb dict 초기화 (line 351 직후)
fb["structureLevel"] = None
fb["structureComment"] = None
fb["attitudeComment"] = None
```

`if verbal:` 블록 안(현재 line 353-363), `fb["coachingImprovement"]` 뒤에 삽입:

```python
# verbal 결과에서 신규 필드 추출 (if verbal: 블록 안에 있어야 함!)
fb["structureLevel"] = verbal.get("structure_level")
fb["structureComment"] = verbal.get("structure_comment")
fb["attitudeComment"] = verbal.get("attitude_comment")
```

> **주의**: Legacy 경로에서 `verbal`이 None이면 `if verbal:` 블록을 안 타므로, 바깥에서 기본값 None을 설정해둬야 BE API 전송 시 키 누락이 발생하지 않는다.

### 4. verbal_prompt_factory.py — `SYSTEM_TEMPLATE` 수정

**수정 대상**: `SYSTEM_TEMPLATE` (line 109-123)만 수정. `FEEDBACK_PERSPECTIVES`는 수정 불필요.

**(a) `## 평가` 섹션에 추가** (기존 `4. comment` 뒤):

```
5. structure_level: 답변 구조 평가
   - GOOD: 서론-본론-결론 등 명확한 흐름
   - AVERAGE: 대체로 흐름 있으나 일부 산만
   - NEEDS_IMPROVEMENT: 두서없이 나열
6. structure_comment: 구조적 관점 1-2문장 피드백
7. attitude_comment: 면접관 관점 태도/말투 인상 (음성 톤과 발화 내용 기반). 3줄 형식: ✓ 긍정 / △ 부정 / → 개선. 구체적 근거(어떤 표현/어투) 제시 필수.
```

**(b) JSON 응답 스키마 변경**:

기존:
```
{{"filler_word_count":0,"tone_label":"","tone_comment":"","comment":""}}
```

변경:
```
{{"filler_word_count":0,"tone_label":"","tone_comment":"","comment":"","structure_level":"","structure_comment":"","attitude_comment":""}}
```

### 5. verbal_analyzer.py — `_SYSTEM_PROMPT` 수정

**주의**: `verbal_prompt_factory.py`는 position이 있을 때 사용되고, `verbal_analyzer.py`의 `_SYSTEM_PROMPT`는 position이 없는 폴백 경로에서 사용된다. **둘 다 수정해야 한다.**

`verbal_analyzer.py`의 `_SYSTEM_PROMPT` (line 15-38)에도 동일하게:
- 평가 항목에 `structure_level`, `structure_comment`, `attitude_comment` 추가
- JSON 응답 스키마에 해당 키 추가

파싱 로직(`analyze_verbal()`)에서는 GPT 응답의 새 키를 그대로 반환 (별도 매핑 불필요).

## 담당 에이전트

- Implement: `backend` — Lambda 코드 수정
- Review: `code-reviewer` — 필드 매핑 누락 확인, null 안전성

## 검증

- Gemini 경로: 분석 완료 후 BE API로 전송되는 payload에 `structureLevel`, `structureComment`, `attitudeComment` 포함 확인
- Legacy 경로: 동일 확인
- 필드가 null인 경우 (Gemini가 누락) 에러 없이 null로 전송되는지 확인
- `progress.md` 상태 업데이트 (Task 3 → Completed)
