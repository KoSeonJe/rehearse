# Plan 02: Vision 프롬프트 재설계

> 상태: Draft
> 작성일: 2026-03-30

## Why

GPT-4o Vision이 `eye_contact_score`(0-100), `posture_score`(0-100)를 생성하지만, 점수 기준이 모호하고 사용자에게 의미 전달이 어렵다. 3단계 라벨로 대체하고 코멘트 가독성을 개선해야 한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `lambda/analysis/analyzers/vision_analyzer.py` | score → 라벨 전환, 코멘트 이모지+불릿 포맷, 폴백값 변경 |

## 상세

### 프롬프트 변경

**Before:**
```
1. eye_contact_score(0-100): 90+=안정응시, 70+=간헐흐트러짐, ...
2. posture_score(0-100): 90+=바른자세유지, 70+=간헐구부정, ...
```

**After:**
```
1. eyeContactLevel: GOOD(안정적 응시) | AVERAGE(간헐적 흐트러짐) | NEEDS_IMPROVEMENT(자주 딴 곳 응시/불안정)
2. postureLevel: GOOD(바른 자세 유지) | AVERAGE(간헐적 구부정) | NEEDS_IMPROVEMENT(지속적 불안정/흔들림)
3. expressionLabel: CONFIDENT | ENGAGED | NEUTRAL | NERVOUS | UNCERTAIN (유지)
```

### 응답 구조

```json
{
  "eyeContactLevel": "GOOD|AVERAGE|NEEDS_IMPROVEMENT",
  "postureLevel": "GOOD|AVERAGE|NEEDS_IMPROVEMENT",
  "expressionLabel": "CONFIDENT|ENGAGED|NEUTRAL|NERVOUS|UNCERTAIN",
  "comment": "✓ 잘한 점\n△ 보완할 점\n→ 개선 방법"
}
```

### 폴백값 변경

```python
_FALLBACK = {
    "eyeContactLevel": "AVERAGE",
    "postureLevel": "AVERAGE",
    "expressionLabel": "NEUTRAL",
    "comment": "✓ 분석 대상 확인됨\n△ 비언어 분석을 수행할 수 없어 기본값 설정\n→ 카메라가 얼굴을 잘 비추도록 조정해보세요",
}
```

### _validate_result 수정

```python
def _validate_result(result: dict) -> dict:
    valid_levels = {"GOOD", "AVERAGE", "NEEDS_IMPROVEMENT"}
    valid_labels = {"CONFIDENT", "NERVOUS", "NEUTRAL", "ENGAGED", "UNCERTAIN"}

    validated = {}
    validated["eyeContactLevel"] = (
        result.get("eyeContactLevel", "AVERAGE")
        if result.get("eyeContactLevel") in valid_levels
        else "AVERAGE"
    )
    validated["postureLevel"] = (
        result.get("postureLevel", "AVERAGE")
        if result.get("postureLevel") in valid_levels
        else "AVERAGE"
    )
    validated["expressionLabel"] = (
        result.get("expressionLabel", "NEUTRAL")
        if result.get("expressionLabel") in valid_labels
        else "NEUTRAL"
    )
    validated["comment"] = result.get("comment") or _FALLBACK["comment"]
    return validated
```

## 담당 에이전트

- Implement: `backend` — Vision 프롬프트 수정 + 검증 로직 변경
- Review: `code-reviewer` — 라벨 검증 로직, 폴백 안전성

## 검증

- Vision 분석 결과가 3단계 라벨로 나오는지 확인
- 유효하지 않은 라벨이 들어올 때 `AVERAGE`로 폴백되는지 확인
- 코멘트가 이모지+불릿 포맷인지 확인
- `progress.md` 상태 업데이트 (Task 2 → Completed)
