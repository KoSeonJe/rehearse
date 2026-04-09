# 피드백 페이지 v2 — 요구사항 정의

> 상태: Draft
> 작성일: 2026-04-01

## Why

### 1. Why? — 어떤 문제를 해결하는가?

현재 피드백 페이지에 세 가지 문제가 있다:

1. **피드백 관점이 부족하다** — "답변 내용" 탭에는 기술 정확성과 코칭만 있고, "답변을 조리있게 구조적으로 잘 전달했는가"에 대한 피드백이 없다. "전달력" 탭에는 시선/자세/속도/자신감 같은 개별 지표만 있고, "면접관이 봤을 때 이 사람의 태도/말투가 어떻게 보이는가"라는 종합적 인상 피드백이 없다.

2. **텍스트가 너무 작아 가독성이 떨어진다** — 섹션 라벨이 `text-[10px]`(10px), 코멘트 본문이 `text-xs`(12px)로 매우 작다. 피드백을 읽기 위해 눈을 찡그려야 한다.

3. **Vision 프롬프트가 노트북 면접 환경을 고려하지 않는다** — 노트북 화면을 보느라 시선이 카메라 아래를 향하는 것을 부정적으로 평가한다.

### 2. Goal — 구체적인 결과물과 성공 기준

- 2탭 구조 유지하되 내용을 재편: "기술 분석" / "자세·말투 분석"
- "기술 분석" 탭에 **답변 구조** 피드백 추가 (조리있게 말했는가)
- "자세·말투 분석" 탭에 **태도 인상** 피드백 추가 (면접관 관점의 종합적 태도/말투 인상)
- 전체 텍스트 크기 1단계 상향 (10px→12px, 12px→14px)
- Vision 프롬프트에 노트북 시선 보정 추가

### 3. Evidence — 근거

- 사용자 피드백: "기술적으로 맞는 건가?", "답변이 구조적으로 잘 맞는가?", "자신감이 있는가?", "까부는 것 같은가?", "절면서 말하는가?" 등 구체적 관점 요청
- 현재 피드백은 개별 지표(시선 GOOD, 자세 AVERAGE)만 나열하고 "면접관 눈에 어떻게 보이는가"라는 종합 인상이 없음
- 노트북 면접에서 시선이 아래를 향하는 것은 불가피하나 현재 프롬프트가 이를 부정 평가함

### 4. Trade-offs — 포기하는 것과 고려한 대안

| 결정 | 선택 | 대안 | 대안 거부 이유 |
|------|------|------|---------------|
| 탭 수 | 2탭 유지 | 3탭 (내용/태도/비언어 분리) | 탭이 많으면 클릭 부담, 2탭이 더 자연스러움 |
| 태도 피드백 방식 | 자연어 코멘트 (종합 인상) | 진중함/비굴함 등 개별 레벨 분리 | 과도하게 세분화하면 AI 판단이 부정확, 자연어가 더 유용 |
| 답변 구조 피드백 | 레벨(GOOD/AVG/NI) + 코멘트 | 코멘트만 | 레벨이 있어야 한눈에 파악 가능 |
| 시선 피드백 | 약화 (노트북 보정) | 완전 제거 | 고개를 완전히 돌리는 경우는 여전히 유효 |

## 목표

| 항목 | Before | After |
|------|--------|-------|
| 탭 구조 | "답변 내용" / "전달력" | "기술 분석" / "자세·말투 분석" |
| 탭1 내용 | verbalComment + accuracyIssues + coaching | verbalComment + **structureLevel/Comment** + accuracyIssues + coaching |
| 탭2 내용 | 비언어(시선/자세/표정) + 음성(속도/자신감/감정/필러워드) | **태도 인상 코멘트** + 자신감 + 표정/자세 + 속도/필러워드 |
| 태도 피드백 | 없음 | 면접관 관점 종합 인상 (자연어 코멘트) |
| 답변 구조 피드백 | 없음 | GOOD/AVERAGE/NEEDS_IMPROVEMENT + 코멘트 |
| 텍스트 크기 | 10px/12px | 12px/14px |
| 시선 평가 | 카메라 아래 = 부정 | 노트북 화면 응시는 정상으로 간주 |

## 아키텍처 / 설계

### 변경 흐름

```
Lambda (프롬프트 변경) → BE (엔티티 + DTO 변경) → FE (탭 재편 + 가독성 개선)
```

### 2탭 재편 구조

```
┌─────────────────┬─────────────────────┐
│   기술 분석      │   자세·말투 분석      │
└─────────────────┴─────────────────────┘

탭1 "기술 분석":
  ├─ 답변 평가 (verbalComment: ✓△→)
  ├─ 답변 구조 (structureLevel 배지 + structureComment) ← 신규
  ├─ 기술 오류 (accuracyIssues)
  └─ 코칭 (coaching)

탭2 "자세·말투 분석":
  ├─ 태도 인상 (attitudeComment: ✓△→) ← 신규
  │   "면접관이 봤을 때 이 사람은 ___하게 보인다"
  ├─ 자신감 (toneConfidenceLevel 배지)
  ├─ 표정 (expressionLabel 배지)
  ├─ 자세 (postureLevel 배지)
  ├─ 시선 (eyeContactLevel 배지, "온라인 면접 기준" 부가 라벨)
  ├─ 말속도 (speechPace 배지)
  └─ 필러워드 (fillerWords 태그 + count)
```

### DB 변경 (신규 3개 nullable 컬럼)

```sql
ALTER TABLE timestamp_feedback ADD COLUMN structure_level VARCHAR(20);      -- 답변 구조 레벨
ALTER TABLE timestamp_feedback ADD COLUMN structure_comment TEXT;           -- 답변 구조 코멘트
ALTER TABLE timestamp_feedback ADD COLUMN attitude_comment TEXT;            -- 태도 인상 코멘트
```

### Gemini 응답 JSON 변경

기존 필드에 2개 블록 추가:

```json
{
  "transcript": "...",
  "verbal": { "comment": "✓ ... △ ... → ..." },
  "structure": {
    "level": "GOOD|AVERAGE|NEEDS_IMPROVEMENT",
    "comment": "답변 구조에 대한 1-2문장 피드백"
  },
  "technical": { "accuracyIssues": [...], "coaching": {...} },
  "vocal": { "fillerWords": [], "speechPace": "", "toneConfidenceLevel": "", "emotionLabel": "", "comment": "" },
  "attitude": {
    "comment": "✓ ... △ ... → ..."
  },
  "overallComment": ""
}
```

### Vision 프롬프트 보정

```
- 중요: 노트북/웹캠 온라인 면접입니다.
  화면을 보느라 시선이 카메라 아래를 향하는 것은 정상.
  시선이 아래로 향하는 것만으로 부정적 평가 금지.
  고개를 완전히 돌리는 경우에만 시선 문제로 판단.
```

## Scope

- **In**:
  - Lambda: Gemini 프롬프트에 structure/attitude 섹션 추가
  - Lambda: Vision 프롬프트에 노트북 시선 보정 추가
  - Lambda: Verbal 폴백 프롬프트에 structure/attitude 추가
  - Lambda: handler.py에서 신규 필드 매핑
  - BE: DB 마이그레이션 (3개 컬럼 추가)
  - BE: Entity + DTO 필드 추가
  - BE: 응답 구조 변경 (content에 structure 추가, delivery → 재편)
  - FE: 타입 변경 + 탭 재편 + 가독성 개선

- **Out**:
  - 진중함/비굴함 등 개별 태도 레벨 (자연어 코멘트로 대체)
  - 탭 추가 (2탭 유지)
  - 분석 대기 페이지 변경
  - 면접 간 성장 트래킹

## 제약조건 / 환경

- Gemini 2.5 Flash JSON 응답 길이 증가 (structure + attitude 추가로 ~200 토큰)
- 기존 분석 데이터 하위 호환: structure/attitude가 null인 경우 FE에서 미표시
- GPT-4o Vision은 비언어만 담당 — structure/attitude와 무관
- `feedback-redesign` 플랜(Draft)과 동일 영역이지만, 해당 플랜은 미구현 상태이므로 이 플랜이 대체

## 의존성 그래프

### 개발 순서

```
Plan 1 (Gemini 프롬프트) ──┐
Plan 2 (Vision 프롬프트) ──┼── [parallel] ──→ Plan 3 (Lambda handler) ──→ Plan 4 (BE) ──→ Plan 5 (FE)
```

> Plan 4(BE)는 독립 개발 가능하지만, Plan 1~3이 완료되어야 Lambda 배포 시 통합 테스트 가능.

### 배포 순서 (개발 순서와 다름!)

```
PR-1 [BE] (Plan 4) ──→ Lambda deploy.sh (Plan 1,2,3) ──→ PR-2 [FE] (Plan 5)
```

> BE를 먼저 배포하는 이유: 신규 3개 컬럼이 nullable이므로 Lambda가 아직 보내지 않아도 기존 데이터에 영향 없음. Lambda 배포 후부터 신규 필드가 채워지기 시작.

## PR 전략

| # | PR | 브랜치 | 선행 조건 |
|---|-----|--------|-----------|
| 1 | `[BE] feat: 피드백 v2 — 답변 구조 + 태도 인상 필드 추가` | `feat/feedback-v2-be` | 없음 |
| 2 | Lambda 직접 배포 (`deploy.sh`) | — | PR-1 머지 + BE 배포 |
| 3 | `[FE] feat: 피드백 v2 — 2탭 재편 + 가독성 개선` | `feat/feedback-v2-fe` | PR-1 머지 |
