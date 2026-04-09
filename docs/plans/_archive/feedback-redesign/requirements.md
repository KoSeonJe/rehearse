# 피드백 리디자인 — 요구사항 정의

> 상태: Draft
> 작성일: 2026-03-30

## Why

### 1. Why? — 어떤 문제를 해결하는가?

현재 피드백 시스템에 세 가지 문제가 있다:

1. **점수의 판단 기준이 애매하다** — `verbal_score 72`, `eyeContactScore 65` 같은 숫자가 나오지만, "좋은 건지 나쁜 건지" 기준이 없고, LLM이 매번 다른 기준으로 채점하여 일관성이 떨어진다.
2. **답변 내용에 대한 기술적 피드백이 없다** — 현재는 "언어적 커뮤니케이션"(STAR 구조, 필러워드, 말투)만 분석하고, "기술적으로 틀린 내용", "빠진 핵심 개념" 같은 답변 품질 피드백이 없다.
3. **피드백 코멘트의 가독성이 떨어진다** — 3~4문장 텍스트 덩어리가 카테고리 구분 없이 나열되어, 핵심을 파악하기 어렵다.

### 2. Goal — 구체적인 결과물과 성공 기준

- 점수(0-100)를 제거하고 3단계 라벨(`좋음`/`보통`/`개선 필요`)로 대체
- 면접 유형별 기술 피드백 추가 (정확성 검증 + 코칭)
- 피드백 코멘트를 `✓ 잘한 점` / `△ 보완할 점` / `→ 개선 방법` 이모지+불릿 포맷으로 구조화
- 피드백 패널을 "답변 내용 분석" / "전달력 분석" 탭으로 분리

### 3. Evidence — 근거

- 사용자 피드백: "점수가 뭘 의미하는지 모르겠다", "어떻게 고쳐야 하는지 모르겠다"
- 현재 Gemini 프롬프트에 `verbal_score` 채점 기준이 있지만 LLM 특성상 일관된 정량 평가가 어려움
- 모범답변은 이미 생성되지만 단순 텍스트 노출만 하고 비교 분석이 없음

### 4. Trade-offs — 포기하는 것과 고려한 대안

| 결정 | 선택 | 대안 | 대안 거부 이유 |
|------|------|------|---------------|
| 점수 표현 | 3단계 라벨 | 숫자 점수 유지 | LLM 채점 일관성 부족, 사용자 혼란 |
| 기술 피드백 생성 | Gemini 프롬프트 확장 | BE에서 Claude 별도 호출 | 파이프라인 복잡도 증가, 추가 비용 |
| 모범답변 대비 비교 분석 | 제거 (코칭으로 대체) | 비교 분석 UI 구현 | 과도한 복잡도, 코칭이 더 실용적 |
| 피드백 구조 | 탭 분리 | 카드 내 섹션 구분 | 정보량이 많아져 탭이 더 가독성 좋음 |

## 목표

| 항목 | Before | After |
|------|--------|-------|
| 점수 | 0-100 정수 (verbalScore, eyeContactScore, postureScore, toneConfidence) | 3단계 라벨 `GOOD`/`AVERAGE`/`NEEDS_IMPROVEMENT` |
| 기술 피드백 | 없음 | `accuracyIssues[]` + `coaching{structure, improvement}` |
| 코멘트 포맷 | 자유 텍스트 | `✓ 잘한 점` / `△ 보완할 점` / `→ 개선 방법` |
| 면접 유형 인식 | 없음 (범용 분석) | TECHNICAL / BEHAVIORAL / RESUME_BASED 3그룹 분기 |
| FE 구조 | 단일 패널 (Technical/Nonverbal/Vocal 섹션) | 탭 분리 (답변 내용 분석 / 전달력 분석) |

## 아키텍처 / 설계

### 변경 흐름

```
Lambda (Gemini/GPT-4o 프롬프트 변경)
  → BE (엔티티 + DTO 변경)
    → FE (피드백 패널 탭 UI 리디자인)
```

### Gemini 응답 구조 변경 (Before → After)

**Before:**
```json
{
  "transcript": "...",
  "verbal": { "score": 0, "comment": "" },
  "vocal": { "fillerWords": [], "speechPace": "", "toneConfidence": 0, "emotionLabel": "", "comment": "" },
  "overallComment": ""
}
```

**After:**
```json
{
  "transcript": "...",
  "verbal": {
    "comment": "✓ ... / △ ... / → ..."
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
    "speechPace": "",
    "toneConfidenceLevel": "GOOD|AVERAGE|NEEDS_IMPROVEMENT",
    "emotionLabel": "",
    "comment": "✓ ... / △ ... / → ..."
  },
  "overallComment": ""
}
```

### Vision 응답 구조 변경

**Before:**
```json
{ "eye_contact_score": 72, "posture_score": 85, "expression_label": "CONFIDENT", "comment": "..." }
```

**After:**
```json
{ "eyeContactLevel": "GOOD", "postureLevel": "AVERAGE", "expressionLabel": "CONFIDENT", "comment": "✓ ... / △ ... / → ..." }
```

### 면접 유형별 기술 피드백 분기

| 그룹 | InterviewType | 피드백 관점 |
|------|--------------|------------|
| 기술 지식 | CS_FUNDAMENTAL, LANGUAGE_FRAMEWORK, SYSTEM_DESIGN, FULLSTACK_STACK, UI_FRAMEWORK, BROWSER_PERFORMANCE, INFRA_CICD, CLOUD, DATA_PIPELINE, SQL_MODELING | 기술 정확성 검증 + 개념 깊이 코칭 |
| 경험/협업 | BEHAVIORAL | STAR 구조 + 구체성 + 역할 명확성 코칭 |
| 이력서 기반 | RESUME_BASED | 경험 구체성 + 기여도 + 기술적 의사결정 코칭 |

### FE 탭 구조

```
┌─────────────────┬─────────────────┐
│  답변 내용 분석  │  전달력 분석     │
└─────────────────┴─────────────────┘
```

- **탭 1 (답변 내용 분석)**: verbal comment + 기술 정확성 오류 목록 + 코칭
- **탭 2 (전달력 분석)**: 비언어(시선/자세/표정 라벨 배지 + comment) + 음성(속도/자신감/감정 라벨 배지 + 필러워드 + comment)

### 3단계 라벨 체계

| 라벨 | 영문 키 | 적용 대상 |
|------|---------|----------|
| 좋음 | `GOOD` | eyeContactLevel, postureLevel, toneConfidenceLevel |
| 보통 | `AVERAGE` | eyeContactLevel, postureLevel, toneConfidenceLevel |
| 개선 필요 | `NEEDS_IMPROVEMENT` | eyeContactLevel, postureLevel, toneConfidenceLevel |

### 코멘트 포맷

모든 comment 필드는 이모지+불릿 포맷:
```
✓ 잘한 점 설명
△ 보완할 점 설명
→ 구체적 개선 방법
```

## Scope

- **In**:
  - Lambda: Gemini/GPT-4o/verbal_analyzer 프롬프트 재설계
  - Lambda: handler.py 매핑 수정
  - BE: 엔티티 + DTO 필드 변경 (score 제거, technical 추가, 라벨 전환)
  - BE: API 응답 변경
  - FE: 피드백 패널 탭 UI 리디자인
  - DB: 마이그레이션 (score 컬럼 nullable, 새 컬럼 추가)

- **Out**:
  - 모범답변 대비 비교 분석 UI
  - 면접 간 성장 트래킹 / 히스토리 비교
  - questionSetScore 대체 (제거만 함)
  - 분석 대기 페이지 변경

## 제약조건 / 환경

- Gemini 2.5 Flash의 JSON 응답 토큰 한도 고려 (기술 피드백 추가로 응답 길이 증가)
- GPT-4o Vision은 비언어만 담당 — 기술 피드백과 무관
- Lambda 환경변수에 interviewType 정보를 BE→Lambda 전달 경로 확보 필요
- 기존 분석 데이터와의 하위 호환: 이미 분석된 피드백은 score가 있고 technical이 없음 → FE에서 graceful fallback 필요

## 의존성 그래프

```
개발:
Plan 1 (Gemini) ──┐
Plan 2 (Vision) ──┼── [parallel] ──→ Plan 4 (handler) ──→ Plan 5 (BE) ──→ Plan 6 (FE)
Plan 3 (Verbal) ──┘

배포 순서 (필수):
PR-1 [BE] ──→ Lambda deploy.sh ──→ PR-2 [FE]
```

## PR 전략

| # | PR | 브랜치 | 선행 조건 |
|---|-----|--------|-----------|
| 1 | `[BE] refactor: 피드백 점수→라벨 전환 + 기술 피드백 필드 추가` | `feat/feedback-redesign-be` | 없음 |
| 2 | Lambda 직접 배포 (`deploy.sh`) | — | PR-1 머지 + BE 배포 |
| 3 | `[FE] refactor: 피드백 패널 탭 분리 + 라벨 배지 + 기술 피드백 UI` | `feat/feedback-redesign-fe` | PR-1 머지 |
