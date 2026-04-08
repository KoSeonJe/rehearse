# Lane 5 — Vision Analyzer 시선 언급 제거 감사 노트

- **Date**: 2026-04-09
- **Scope**: `lambda/analysis/analyzers/vision_analyzer.py` 프롬프트 + 사후 필터
- **Status**: Hotfix applied (direct commit approved)
- **Owner**: prompt-engineer 에이전트

## 1. 배경과 문제

Vision 분석기는 면접 영상 프레임을 입력받아 아래 JSON을 반환한다.

```
eyeContactLevel  (enum, BE/DB/FE 스키마)
postureLevel     (enum)
expressionLabel  (enum)
positive         (free-text, 1문장)
negative         (free-text, 1문장)
suggestion       (free-text, 1문장)
```

`positive` / `negative` / `suggestion` 는 핸들러에서 `_comment_block`으로
감싸져 그대로 사용자에게 노출된다 (`handler.py:274, 381`).

**증상**: 기존 프롬프트는 "시선이 화면 아래쪽으로 향하는 것은 정상"이라는
면책 문구를 두었을 뿐, free-text 에 시선 관련 표현이 빈번히 등장했다.
LLM 이 `eyeContactLevel` enum 을 "평가 축"으로 해석해 텍스트 요약에서도
자연스럽게 참조한 것이 원인이다.

**제약**: `eyeContactLevel` 필드 자체는 BE/DB/FE (`TimestampFeedback`,
`SaveFeedbackRequest`, `TimestampFeedbackResponse`, FE `delivery-tab`) 가
의존하므로 이번 핫픽스 범위에서는 **제거하지 않는다**.

## 2. 변경 요약

전략은 **(B) 프롬프트 가드 강화 + (C) 사후 필터** 이중 방어.

### 2.1 프롬프트 재설계 (B)

Before (요약):

```
## 주의
- ...
- 노트북/웹캠으로 진행하는 온라인 면접입니다. 시선이 화면 아래쪽으로 향하는 것은 정상.

## 작성 규칙
- positive/negative/suggestion 각 1문장.
- "전반적으로 잘했습니다" 같은 모호한 표현 금지.
```

After (핵심 발췌):

```
## 🚫 최우선 금지 규칙 (위반 시 응답 무효)
텍스트 필드(positive / negative / suggestion)에서는
**시선 관련 내용을 절대 언급하지 마세요.**
- 금지 어휘 예시: 시선, 눈맞춤, 눈빛, 응시, 아이컨택,
  eye contact, eye-contact, gaze, 화면 응시, 카메라 응시, 눈을 마주침 등
- 텍스트는 오직 **자세(어깨/목/상체/손/몸의 방향)** 와
  **표정/감정 상태** 두 축으로만 서술하세요.
- `eyeContactLevel` enum 값은 평가하되, 그 결과를
  **텍스트로 풀어쓰거나 암시하지 마세요.**

## 평가 enum
- eyeContactLevel: ... (enum만 채우고 텍스트로 언급 금지)
- postureLevel: ...
- expressionLabel: ...
```

핵심 변화:

1. 금지 규칙을 프롬프트 **최상단**에 배치 (기존은 `## 주의` 하단에 매몰).
2. 부정 지시 + 긍정 프레이밍을 동시 제공 ("오직 자세와 표정 두 축으로만").
3. enum 스키마 설명란에 `(텍스트로 언급 금지)` 주석 인라인.
4. JSON 스키마 설명 문구에 `시선 언급 금지` 명시적으로 삽입.
5. 작성 규칙 말미에 "다시 강조" 블록으로 **리마인더 2회**.

### 2.2 재시도 리마인더

LLM 이 첫 응답에서 가이드를 위반하면, `_RETRY_REMINDER` 문자열을
system prompt 에 덧붙여 **1회** 재호출한다. 기존 `MAX_RETRIES=3`,
`temperature=0.3`, 모델, 타임아웃은 변경하지 않는다 — 재시도 횟수는
기존 루프의 일부로 흡수된다(`gaze_retry_used` 플래그로 1회만 발생).

### 2.3 사후 필터 (C)

구현한 Python 함수:

- `_has_gaze_mention(result: dict) -> bool`
- `_strip_gaze_mentions(result: dict) -> dict`
- 모듈 레벨 상수: `_GAZE_KEYWORDS`, `_GAZE_PATTERN`, `_GAZE_SAFE_FALLBACK`

처리 흐름 (`analyze_frames` 루프 내):

```
parse_llm_json → _validate_result
↓
_has_gaze_mention ?
  ├─ no  → return result
  └─ yes →
     ├─ 재시도 가능 + 아직 안 썼다면 → system_prompt += _RETRY_REMINDER, continue
     └─ 마지막 시도거나 재시도 소진 → _strip_gaze_mentions 치환 적용
```

`_strip_gaze_mentions` 동작:

1. 문장 단위로 split (한국어 어미 `다|요`, 영어 `. ! ?`).
2. 시선 패턴이 포함되지 않은 문장만 유지.
3. 남은 문장이 0개면 `_GAZE_SAFE_FALLBACK` 의 안전 문장으로 대체.
4. 치환 후에도 패턴이 검출되면 강제로 fallback 으로 교체 (안전망).
5. 디버깅용 `_stripped_fields` 메타를 임시로 담고, caller 가 로깅 후 제거.

### 2.4 Fallback 사전 업데이트

Before:

```python
"positive":   "분석 대상이 확인됐습니다.",
"negative":   "비언어 분석을 수행할 수 없어 기본값으로 설정했습니다.",
"suggestion": "카메라가 얼굴을 잘 비추도록 조정해보세요.",  # 카메라 언급
```

After:

```python
"positive":   "상체 자세와 표정이 전반적으로 안정적으로 유지됐습니다.",
"negative":   "비언어 분석을 충분히 수행하지 못해 자세·표정 세부 평가가 제한됐습니다.",
"suggestion": "어깨를 펴고 상체를 정면으로 유지한 채 연습 녹화를 다시 시도해보세요.",
```

카메라/시선 단어가 모두 제거됐다.

`_GAZE_SAFE_FALLBACK` (치환용 별도 사전) 도 동일하게 posture/expression
만 언급하도록 작성했다.

## 3. Gaze 키워드 리스트

```python
_GAZE_KEYWORDS = (
    # 한국어
    "시선", "눈맞춤", "눈 맞춤", "눈빛", "응시", "주시",
    "아이컨택", "아이 컨택", "아이콘택", "눈을 마주", "눈을 피",
    "화면 응시", "화면을 응시", "화면을 바라", "카메라 응시",
    "카메라를 응시", "카메라를 바라", "카메라에 시선",
    "정면 응시", "정면을 응시",
    # 영어
    "eye contact", "eye-contact", "eyecontact", "eyecontactlevel",
    "eye contact level", "gaze", "looking at the camera",
    "look at the camera", "eyes", "eye ",
)
```

정규식은 `re.escape` 조합 + `re.IGNORECASE` 로 컴파일.

**거짓양성 리스크**: "눈" 만으로는 매칭하지 않지만 `"eye "` (뒤에 공백)
은 영문 단어 eye 단독 사용을 잡는다. "eyes" 도 포함. "응시" 는 다른 맥락
(코드 응시 등)에서 쓰일 일이 없다고 판단해 포함.

## 4. 테스트 결과 (로컬 단위)

4개 케이스에 대한 스모크 테스트 — `_has_gaze_mention` / `_strip_gaze_mentions`:

| case | 입력 특징 | has_gaze | 결과 |
|------|-----------|----------|------|
| 0 | 시선 언급 없음 (한국어) | False | 원문 유지 |
| 1 | positive 에 "화면을 응시" | True | positive 만 safe fallback 치환, 나머지 유지 |
| 2 | 영어, "eye contact" + posture | True | positive 치환, 나머지 유지 |
| 3 | 3필드 모두 gaze (아이컨택/gaze/카메라 응시) | True | 3필드 모두 safe fallback |

모든 케이스에서 잔여 gaze 패턴 없음.

## 5. 테스트 권장 사항 (프로덕션)

1. **샤도우 로그**: 배포 후 CloudWatch 에서 `[Vision] 시선 언급` 로그를
   모니터링해 감지율/재시도율/치환율을 집계한다.
2. **회귀 스냅샷**: 기존 면접 영상 5개에 대해 분석을 재실행하고
   `positive/negative/suggestion` 텍스트를 before/after 비교한다.
3. **E2E**: FE `delivery-tab` 에서 시선 관련 문구가 더 이상 노출되지
   않는지 확인 (DOM 텍스트 스냅샷).
4. **BE 역직렬화**: 치환된 텍스트가 `SaveFeedbackRequest.CommentBlock`
   으로 문제없이 역직렬화 되는지 API 레벨 확인.
5. **Gemini 경로 대조**: 본 변경은 Vision 분석기만 수정. `gemini_analyzer`
   의 `nonverbal`/`attitude` 블록에도 시선 언급 문제가 있는지 별도 감사 필요
   (Lane 5 후속 과제 후보).

## 6. 리스크 및 한계

- **False negative**: LLM 이 "눈길", "눈동자", "look into" 처럼 리스트에
  없는 우회 표현을 쓰면 감지 못 한다. 추가 키워드는 로그 기반으로 점진
  보강 필요.
- **False positive**: "응시" 가 자세 맥락에서 거의 쓰이지 않으므로 낮다
  고 판단. 실사용 모니터링 필요.
- **치환 자연스러움**: `_GAZE_SAFE_FALLBACK` 은 고정 문구이므로 반복 노출
  시 기계적으로 보일 수 있다. 프롬프트 가드 + 재시도가 제대로 먹히면
  치환은 거의 발생하지 않아야 한다.
- **범위 한정**: `eyeContactLevel` enum 자체는 그대로 노출. FE 에서 이
  enum 을 어떻게 시각화하는지는 별도 결정이 필요하다.

## 7. 다음 단계 후보

- `eyeContactLevel` 를 스키마에서 제거할지 결정 (Deep 논의 필요).
- Gemini nonverbal/attitude 프롬프트에서도 동일 가드 적용 검토.
- `_GAZE_KEYWORDS` 를 공용 모듈로 승격해 다른 analyzer 와 공유.
