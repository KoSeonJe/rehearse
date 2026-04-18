import type { CommentBlock } from '@/types/interview'

const COMMENT_STYLES = {
  positive: { bg: 'bg-emerald-50/60', label: 'text-emerald-600' },
  negative: { bg: 'bg-amber-50/60', label: 'text-amber-600' },
  suggestion: { bg: 'bg-muted', label: 'text-muted-foreground' },
} as const

type CommentType = keyof typeof COMMENT_STYLES

interface StructuredCommentProps {
  block: CommentBlock | null | undefined
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
  if (!block) return null

  const items: Array<{ type: CommentType; label: string; body: string }> = []
  if (block.positive && block.positive.trim().length > 0) {
    items.push({ type: 'positive', label: positiveLabel, body: block.positive.trim() })
  }
  if (block.negative && block.negative.trim().length > 0) {
    items.push({ type: 'negative', label: negativeLabel, body: block.negative.trim() })
  }
  if (block.suggestion && block.suggestion.trim().length > 0) {
    items.push({ type: 'suggestion', label: suggestionLabel, body: block.suggestion.trim() })
  }

  if (items.length === 0) return null

  return (
    <div className="space-y-3">
      {items.map((item, idx) => (
        <div key={idx} className={`rounded-xl px-4 py-3 ${COMMENT_STYLES[item.type].bg}`}>
          <p className={`text-[13px] font-bold mb-1 ${COMMENT_STYLES[item.type].label}`}>
            {item.label}
          </p>
          <p className="text-[14px] leading-[1.7] text-gray-600">{item.body}</p>
        </div>
      ))}
    </div>
  )
}

export default StructuredComment
