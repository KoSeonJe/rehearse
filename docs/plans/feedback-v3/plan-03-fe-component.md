# Plan 03: FE 타입·StructuredComment·매퍼·탭

> 상태: Draft
> 작성일: 2026-04-07

## Why

`requirements.md`의 결정 1·3 실행. BE 응답이 `CommentBlock` 객체로 바뀌므로 FE 타입과 컴포넌트도 갈아엎는다. `structured-comment.tsx`의 prefix 파싱 로직을 폐기하고, `expressionLabel` 영어 enum을 한글로 매핑한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `frontend/src/types/interview.ts` | `CommentBlock` 타입 추가, `ContentFeedback`/`NonverbalFeedback`/`VocalFeedback`/`DeliveryFeedback`의 `*Comment` 필드를 `string` → `CommentBlock` 으로 교체 |
| `frontend/src/components/feedback/structured-comment.tsx` | prefix 파싱 로직 완전 폐기, props를 `block: CommentBlock` 으로 단순화 |
| `frontend/src/components/feedback/format-feedback-level.ts` | `formatExpressionLabel` 함수 추가 |
| `frontend/src/components/feedback/delivery-tab.tsx` | `LevelBadge label="표정"` 호출부에 `formatExpressionLabel` 적용, `<StructuredComment>` 호출부 3곳을 `block` props로 전환 |
| `frontend/src/components/feedback/content-tab.tsx` | `<StructuredComment>` 호출부를 `block` props로 전환 |

## 상세

### 1. `interview.ts`

```ts
// 신규
export interface CommentBlock {
  positive: string | null
  negative: string | null
  suggestion: string | null
}

// 변경
export interface ContentFeedback {
  verbalComment: CommentBlock | null         // string → CommentBlock
  accuracyIssues: AccuracyIssue[]
  coaching: CoachingResponse | null
}

export interface NonverbalFeedback {
  eyeContactLevel: FeedbackLevel | null
  postureLevel: FeedbackLevel | null
  expressionLabel: string | null
  nonverbalComment: CommentBlock | null      // string → CommentBlock
}

export interface VocalFeedback {
  fillerWords: string | null
  fillerWordCount: number | null
  speechPace: string | null
  toneConfidenceLevel: FeedbackLevel | null
  emotionLabel: string | null
  vocalComment: CommentBlock | null          // string → CommentBlock
}

export interface DeliveryFeedback {
  nonverbal: NonverbalFeedback | null
  vocal: VocalFeedback | null
  attitudeComment: CommentBlock | null       // string → CommentBlock
}
```

`overallComment`가 응답에 포함된다면 `TimestampFeedback` 또는 `QuestionSetFeedbackResponse` 어딘가에 동일하게 `CommentBlock` 으로 추가.

### 2. `structured-comment.tsx`

prefix 파싱 로직(`split('\n')`, `PREFIXES`)을 완전히 제거하고 단순 필드 렌더로 교체:

```tsx
import type { CommentBlock } from '@/types/interview'

interface StructuredCommentProps {
  block: CommentBlock | null
  positiveLabel?: string
  negativeLabel?: string
  suggestionLabel?: string
}

const StructuredComment = ({
  block,
  positiveLabel = '잘한 점',
  negativeLabel = '아쉬운 점',
  suggestionLabel = '이렇게 말하면 더 좋아요',
}: StructuredCommentProps) => {
  if (block === null) return null

  const items: Array<{ label: string; body: string }> = []
  if (block.positive !== null && block.positive.trim().length > 0) {
    items.push({ label: positiveLabel, body: block.positive })
  }
  if (block.negative !== null && block.negative.trim().length > 0) {
    items.push({ label: negativeLabel, body: block.negative })
  }
  if (block.suggestion !== null && block.suggestion.trim().length > 0) {
    items.push({ label: suggestionLabel, body: block.suggestion })
  }

  if (items.length === 0) return null

  return (
    <div className="space-y-3">
      {items.map((item, idx) => (
        <div key={idx}>
          <p className="text-[13px] font-bold text-gray-500 mb-1">{item.label}</p>
          <p className="text-[15px] leading-[1.7] text-gray-700">{item.body}</p>
        </div>
      ))}
    </div>
  )
}

export default StructuredComment
```

### 3. `format-feedback-level.ts`

기존 `formatFeedbackLevel` 옆에 `formatExpressionLabel` 추가:

```ts
const EXPRESSION_LABELS: Record<string, string> = {
  CONFIDENT: '자신감',
  ENGAGED: '몰입',
  NEUTRAL: '평온',
  NERVOUS: '긴장',
  UNCERTAIN: '혼란',
}

export const formatExpressionLabel = (label: string | null): string => {
  if (label === null) return '—'
  return EXPRESSION_LABELS[label] ?? label
}
```

### 4. `delivery-tab.tsx`

```tsx
import { formatFeedbackLevel, formatExpressionLabel } from '@/components/feedback/format-feedback-level'

// 87행 부근
<LevelBadge
  label="표정"
  value={formatExpressionLabel(nonverbal.expressionLabel)}
  bg="gray"
/>

// StructuredComment 호출 3곳 모두 block props로 전환
<StructuredComment
  block={attitudeComment}
  positiveLabel="좋은 인상"
  negativeLabel="신경 쓰면 좋을 부분"
  suggestionLabel="이렇게 바꿔보세요"
/>

<StructuredComment
  block={nonverbal.nonverbalComment}
  positiveLabel="잘한 점"
  negativeLabel="아쉬운 점"
  suggestionLabel="이렇게 해보세요"
/>

<StructuredComment
  block={vocal.vocalComment}
  positiveLabel="잘한 점"
  negativeLabel="아쉬운 점"
  suggestionLabel="이렇게 해보세요"
/>
```

`hasAttitude`/`hasNonverbal`/`hasVocal` null 체크는 block 객체가 null인지 검사하도록 갱신.

### 5. `content-tab.tsx`

```tsx
<StructuredComment
  block={content.verbalComment}
  positiveLabel="잘한 점"
  negativeLabel="아쉬운 점"
  suggestionLabel="이렇게 말하면 더 좋아요"
/>
```

`hasVerbalComment` null 체크 갱신 (CommentBlock null 또는 모든 필드가 null인 경우).

## 담당 에이전트

- Implement: `frontend` — 타입, 컴포넌트, 매퍼 전부
- Review: `code-reviewer` — 타입 안전성, null 처리
- Review: `designer` — 라벨 텍스트·간격이 기존 UX와 일관되는지

## 검증

- `npm --prefix frontend run lint` 통과
- `npm --prefix frontend run build` 타입 체크 통과
- 로컬 dev 서버 (`npm --prefix frontend run dev`) 에서 mock 데이터로 5종 코멘트 모두 3블록 렌더 확인:
  - block의 모든 필드가 채워진 경우
  - `negative`/`suggestion`이 null인 경우 → 해당 블록만 숨김
  - block 자체가 null인 경우 → 컴포넌트 미렌더
- BE+Lambda dev 배포 후 새 면접 1회 녹화 → 5종 모두 정상 렌더 + 표정 배지 한글 표시 확인
- `progress.md` 상태 업데이트 (Task 3 → Completed)
