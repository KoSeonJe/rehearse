# Plan 04: Lambda handler.py 매핑 수정

> 상태: Draft
> 작성일: 2026-03-30

## Why

Gemini/Vision/Verbal 프롬프트의 응답 구조가 변경되므로, handler.py에서 BE 내부 API로 전달하는 매핑 로직을 새 구조에 맞게 수정해야 한다. 또한 interviewType을 Lambda에 전달하는 경로를 확보해야 한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `lambda/analysis/handler.py` | Gemini/Verbal/Vision 응답 → BE API 페이로드 매핑 수정, interviewType 수신 및 프롬프트 전달 |
| `lambda/analysis/api_client.py` | BE 내부 API 호출 시 새 필드 매핑 (필요 시) |

## 상세

### 1. interviewType 전달 경로

BE 내부 API(`/internal/question-sets/{id}/answers`) 응답에서 `interviewType`을 수신하여 Gemini/Verbal 프롬프트에 전달:

```python
interview_type = answers_data.get("interviewType", "CS_FUNDAMENTAL")
```

### 2. Gemini 응답 매핑 변경

**Before:**
```python
verbal_score = gemini_result["verbal"]["score"]
verbal_comment = gemini_result["verbal"]["comment"]
```

**After:**
```python
verbal_comment = gemini_result["verbal"]["comment"]
accuracy_issues = gemini_result.get("technical", {}).get("accuracyIssues", [])
coaching_structure = gemini_result.get("technical", {}).get("coaching", {}).get("structure", "")
coaching_improvement = gemini_result.get("technical", {}).get("coaching", {}).get("improvement", "")
```

### 3. Vision 응답 매핑 변경

**Before:**
```python
eye_contact_score = vision_result["eye_contact_score"]
posture_score = vision_result["posture_score"]
```

**After:**
```python
eye_contact_level = vision_result["eyeContactLevel"]
posture_level = vision_result["postureLevel"]
```

### 4. Vocal 매핑 변경

**Before:**
```python
tone_confidence = vocal["toneConfidence"]  # 숫자
```

**After:**
```python
tone_confidence_level = vocal["toneConfidenceLevel"]  # 라벨
```

### 5. BE 내부 API 페이로드 변경

SaveFeedbackRequest에 전달하는 필드를 새 구조에 맞게 변경. score 필드 제거, technical 필드 추가, 라벨 필드 전환.

## 담당 에이전트

- Implement: `backend` — 매핑 로직 수정
- Review: `code-reviewer` — 필드 누락 없이 매핑되는지, None/빈값 안전 처리

## 검증

- 전체 파이프라인 E2E 테스트 (녹화 → 분석 → BE 저장)
- BE에 저장된 피드백 데이터가 새 구조와 일치하는지 DB 직접 확인
- `progress.md` 상태 업데이트 (Task 4 → Completed)
