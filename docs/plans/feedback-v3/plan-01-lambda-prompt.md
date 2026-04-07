# Plan 01: Lambda 프롬프트·핸들러 정형 JSON 전환

> 상태: Draft
> 작성일: 2026-04-07

## Why

`requirements.md`의 핵심 결정 1·2 실행. LLM이 `comment` 한 덩어리 문자열 대신 `{positive, negative, suggestion}` 정형 객체를 반환하게 만들어, FE의 prefix 파싱 의존을 폐기한다. 5종 코멘트(verbal/vocal/attitude/nonverbal/overall) 전부에 동일 스키마 적용.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `lambda/analysis/analyzers/vision_analyzer.py` | `_SYSTEM_PROMPT` 재작성 (`comment` → `positive/negative/suggestion`), `_validate_result`/`_FALLBACK` 갱신 |
| `lambda/analysis/analyzers/gemini_analyzer.py` | `_ANSWER_SYSTEM_TEMPLATE` 4 블록(verbal/vocal/attitude/overall) 모두 정형 객체로 전환, `_FALLBACK_ANSWER` 갱신 |
| `lambda/analysis/handler.py` | BE 저장 페이로드 매핑부 — LLM이 반환한 객체를 그대로 BE 동일 필드명에 객체로 전송. 기존 ✓△→ 합성 코드 제거 |

## 상세

### 1. `vision_analyzer.py`

`_SYSTEM_PROMPT`를 다음 골자로 재작성:

```python
_SYSTEM_PROMPT = KOREAN_INSTRUCTION + """면접 영상 프레임의 비언어적 커뮤니케이션만 평가합니다. 답변 내용은 평가하지 않습니다.

## 평가 enum (모두 영어 코드값으로 응답)
- eyeContactLevel: GOOD | AVERAGE | NEEDS_IMPROVEMENT
- postureLevel: GOOD | AVERAGE | NEEDS_IMPROVEMENT
- expressionLabel: CONFIDENT | ENGAGED | NEUTRAL | NERVOUS | UNCERTAIN

## 주의
- 여러 프레임의 평균 경향 평가. 이상치에 과도한 가중치 금지.
- 사람 미확인 시 AVERAGE로 응답.
- 노트북/웹캠으로 진행하는 온라인 면접입니다. 시선이 화면 아래쪽으로 향하는 것은 정상.

## 응답 형식 (정확히 이 JSON 스키마, 다른 키 추가 금지)
{
  "eyeContactLevel": "GOOD|AVERAGE|NEEDS_IMPROVEMENT",
  "postureLevel": "GOOD|AVERAGE|NEEDS_IMPROVEMENT",
  "expressionLabel": "CONFIDENT|ENGAGED|NEUTRAL|NERVOUS|UNCERTAIN",
  "positive": "잘한 점 1문장 (구체적 근거 포함)",
  "negative": "보완할 점 1문장 (구체적 근거 포함)",
  "suggestion": "개선 방법 1문장 (실행 가능한 형태)"
}

## 작성 규칙
- positive/negative/suggestion 각 1문장. 두 문장 이상 금지.
- "전반적으로 잘했습니다" 같은 모호한 표현 금지. 반드시 구체적 행동/표정/자세를 인용.
- 보완점이 거의 없어도 작은 점이라도 1문장 작성. null·빈 문자열 금지."""
```

`_FALLBACK` 갱신:

```python
_FALLBACK = {
    "eyeContactLevel": "AVERAGE",
    "postureLevel": "AVERAGE",
    "expressionLabel": "NEUTRAL",
    "positive": "분석 대상이 확인됐습니다.",
    "negative": "비언어 분석을 수행할 수 없어 기본값으로 설정했습니다.",
    "suggestion": "카메라가 얼굴을 잘 비추도록 조정해보세요.",
}
```

`_validate_result`에 `positive`/`negative`/`suggestion` 키 존재 검증 추가. 누락 시 폴백값으로 채움.

### 2. `gemini_analyzer.py`

`_ANSWER_SYSTEM_TEMPLATE`의 verbal/vocal/attitude/overall 각 블록에서 `comment` 키를 폐기하고 `positive`/`negative`/`suggestion` 3필드를 강제. 응답 형식 예시도 동일하게 갱신:

```python
_ANSWER_USER_TEMPLATE = """직무: {position} ({tech_stack}) | 레벨: {level}
<user_data>
질문: {question}
{model_answer_line}</user_data>
## 응답 형식
반드시 아래 JSON 형식으로만 응답:
{{
  "transcript": "",
  "verbal": {{"positive":"","negative":"","suggestion":""}},
  "technical": {{"accuracyIssues":[],"coaching":{{"structure":"","improvement":""}}}},
  "vocal": {{
    "fillerWords": [],
    "speechPace": "",
    "toneConfidenceLevel": "",
    "emotionLabel": "",
    "positive": "",
    "negative": "",
    "suggestion": ""
  }},
  "attitude": {{"positive":"","negative":"","suggestion":""}},
  "overall": {{"positive":"","negative":"","suggestion":""}}
}}"""
```

`_FALLBACK_ANSWER` 갱신:

```python
_FALLBACK_ANSWER = {
    "transcript": "",
    "verbal": {
        "positive": "분석을 시도했습니다.",
        "negative": "음성 분석에 실패했습니다.",
        "suggestion": "다시 시도해 주세요.",
    },
    "technical": {"accuracyIssues": [], "coaching": {"structure": "", "improvement": ""}},
    "vocal": {
        "fillerWords": [],
        "speechPace": "적절",
        "toneConfidenceLevel": "AVERAGE",
        "emotionLabel": "평온",
        "positive": "",
        "negative": "",
        "suggestion": "",
    },
    "attitude": {"positive": None, "negative": None, "suggestion": None},
    "overall": {"positive": "", "negative": "", "suggestion": ""},
}
```

각 블록의 작성 규칙도 ✓/△/→ 표기를 모두 제거하고 "positive/negative/suggestion 각 1문장" 형태로 통일.

### 3. `handler.py`

BE 저장 페이로드 매핑부 (`fb["verbalComment"] = verbal.get("comment")` 같은 라인들)를 다음과 같이 변경:

```python
# Before
fb["verbalComment"] = verbal.get("comment")
fb["vocalComment"] = vocal.get("comment")
fb["attitudeComment"] = attitude.get("comment")
fb["nonverbalComment"] = vision.get("comment")
fb["overallComment"] = gemini.get("overallComment", "")

# After
fb["verbalComment"]    = _comment_block(verbal)
fb["vocalComment"]     = _comment_block(vocal)
fb["attitudeComment"]  = _comment_block(attitude)
fb["nonverbalComment"] = _comment_block(vision)
fb["overallComment"]   = _comment_block(gemini.get("overall"))
```

`_comment_block` 헬퍼:

```python
def _comment_block(src: dict | None) -> dict | None:
    if not src:
        return None
    return {
        "positive":   src.get("positive"),
        "negative":   src.get("negative"),
        "suggestion": src.get("suggestion"),
    }
```

`_build_overall_comment` 등 ✓△→ 합성 헬퍼는 모두 제거.

> ⚠️ Lambda는 BE에 JSON으로 POST하므로 dict를 그대로 넣으면 됨. BE DTO가 `CommentBlock` 객체로 받게 된다 (Plan 02 참조).

## 담당 에이전트

- Implement: `executor` (sonnet) — Python 프롬프트 작성 + 핸들러 매핑
- Review: `code-reviewer` — 프롬프트 강도, fallback 안전성

## 검증

- `lambda/analysis/handler.py`를 모의 입력으로 실행해 신규 페이로드가 5종 모두 객체 형태로 채워지는지 print 확인
- `vision_analyzer.analyze_frames` / `gemini_analyzer.analyze_answer_audio` 단위 호출 (실제 또는 mocking)로 신규 키만 반환되는지
- `lambda/scripts/deploy.sh` 로 dev 배포 후 실제 면접 1회 녹화 시 CloudWatch 로그에서 신규 페이로드 형태 확인
- `progress.md` 상태 업데이트 (Task 1 → Completed)
