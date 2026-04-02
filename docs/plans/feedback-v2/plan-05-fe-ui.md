# Plan 05: FE 2탭 재편 + 가독성 개선

> 상태: Draft
> 작성일: 2026-04-01

## Why

현재 "답변 내용" / "전달력" 탭을 "기술 분석" / "자세·말투 분석"으로 재편하고, 신규 피드백(답변 구조, 태도 인상)을 표시해야 한다. 또한 전체 텍스트가 너무 작아 가독성이 떨어진다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `frontend/src/types/interview.ts` | ContentFeedback에 structure 필드 추가, DeliveryFeedback에 attitudeComment 추가 |
| `frontend/src/components/feedback/content-tab.tsx` | 탭 이름 "기술 분석", 답변 구조 섹션 추가 |
| `frontend/src/components/feedback/delivery-tab.tsx` | 탭 이름 "자세·말투 분석", 태도 인상 섹션 추가, 시선 약화 |
| `frontend/src/components/feedback/feedback-panel.tsx` | 탭 라벨 변경, 텍스트 크기 업 |
| `frontend/src/components/feedback/structured-comment.tsx` | 텍스트 크기 `text-xs` → `text-sm` |
| `frontend/src/components/feedback/level-badge.tsx` | 텍스트 크기 `text-[10px]` → `text-xs` |
| `frontend/src/components/feedback/accuracy-issues.tsx` | 텍스트 크기 업 |
| `frontend/src/components/feedback/coaching-card.tsx` | 텍스트 크기 업 |

## 상세

### 1. 타입 변경 (`interview.ts`)

```typescript
interface ContentFeedback {
  verbalComment: string | null
  structureLevel: FeedbackLevel | null   // 신규 — 기존 FeedbackLevel 타입 재사용
  structureComment: string | null        // 신규
  accuracyIssues: AccuracyIssue[]
  coaching: CoachingResponse | null
}

interface DeliveryFeedback {
  nonverbal: NonverbalFeedback | null
  vocal: VocalFeedback | null
  attitudeComment: string | null     // 신규
}
```

> `structureLevel`은 기존 `FeedbackLevel` 타입(`'GOOD' | 'AVERAGE' | 'NEEDS_IMPROVEMENT'`)을 재사용하여 타입 안전성 확보.

### 2. content-tab.tsx — "기술 분석" 탭

기존 "답변 평가" 섹션 아래에 "답변 구조" 섹션 추가:

```
┌─ 답변 평가 ─────────────────────┐
│ ✓ 잘한 점  △ 개선점  → 제안     │
│ (verbalComment)                  │
└──────────────────────────────────┘
┌─ 답변 구조 ─────────────────────┐  ← 신규
│ [GOOD 배지]                      │
│ structureComment 텍스트          │
└──────────────────────────────────┘
┌─ 기술 오류 ─────────────────────┐
│ (accuracyIssues)                 │
└──────────────────────────────────┘
┌─ 코칭 ─────────────────────────┐
│ (coaching)                       │
└──────────────────────────────────┘
```

### 3. delivery-tab.tsx — "자세·말투 분석" 탭

최상단에 "태도 인상" 섹션 추가 + 기존 섹션 라벨 크기 변경:

```
┌─ 태도 인상 ─────────────────────┐  ← 신규
│ ✓ 차분하고 진지한 태도           │
│ △ "~것 같아요" 반복으로 확신 부족│
│ → 단정적 표현으로 바꿔보세요     │
└──────────────────────────────────┘
┌─ 비언어 ────────────────────────┐
│ [시선 GOOD (온라인 면접 기준)]   │  ← 시선에 부가 라벨
│ [자세 AVERAGE]  [표정 CONFIDENT] │
│ nonverbalComment                 │
└──────────────────────────────────┘
┌─ 음성 ──────────────────────────┐
│ [속도 적절] [자신감 GOOD]        │
│ [감정 자신감] 필러워드: 음, 어   │
│ vocalComment                     │
└──────────────────────────────────┘
```

시선 배지에 "(온라인 면접 기준)" 텍스트를 작은 글씨로 부가.

**기존 `emotionLabel`(감정) 배지는 유지**한다. 음성 섹션에서 자신감/속도/감정/필러워드 배지를 그대로 표시.

**delivery-tab.tsx 자체의 가독성 변경**:
- 섹션 라벨("비언어", "음성"): `text-[10px]` → `text-xs`
- 태도 인상 섹션 라벨: `text-xs font-bold uppercase tracking-widest text-violet-500`

### 4. feedback-panel.tsx — 탭 라벨 + 크기

탭 버튼 텍스트 변경:
- "답변 내용" → "기술 분석"
- "전달력" → "자세·말투"

카드 전체 크기 업:
- 시간 배지: `text-[10px]` → `text-xs`
- 탭 버튼: `text-xs` → `text-sm`
- 질문 텍스트: `text-sm` → `text-base`
- 카드 패딩: `p-5` → `p-6`
- 카드 간격: `space-y-3` → `space-y-4`

### 5. 공통 컴포넌트 가독성 개선

| 컴포넌트 | 변경 |
|---------|------|
| `structured-comment.tsx` | 줄 `text-xs` → `text-sm`, `leading-relaxed` 추가 |
| `level-badge.tsx` | 라벨/값 `text-[10px]` → `text-xs`, 패딩 `px-2 py-0.5` → `px-2.5 py-1` |
| `accuracy-issues.tsx` | 라벨 `text-[10px]` → `text-xs`, 내용 `text-xs` → `text-sm` |
| `coaching-card.tsx` | 라벨 `text-[10px]` → `text-xs`, 내용 `text-xs` → `text-sm` |

### 6. 하위 호환

`content-tab.tsx`: `structureLevel`/`structureComment`가 null이면 "답변 구조" 섹션 미표시.
`delivery-tab.tsx`: `attitudeComment`가 null이면 "태도 인상" 섹션 미표시.

## 담당 에이전트

- Implement: `frontend` — 타입 변경 + 컴포넌트 수정
- Review: `code-reviewer` — 코드 품질, null 처리
- Review: `designer` — UI/UX 일관성, 가독성

## 검증

- 새 분석 결과: "기술 분석" 탭에 답변 구조 섹션 표시 확인
- 새 분석 결과: "자세·말투" 탭에 태도 인상 섹션 표시 확인
- 기존 분석 결과 (structure/attitude null): 해당 섹션 미표시, 나머지 정상 렌더링
- 텍스트 크기: 10px→12px, 12px→14px 적용 확인
- 시선 배지에 "(온라인 면접 기준)" 부가 라벨 표시 확인
- `progress.md` 상태 업데이트 (Task 5 → Completed)
