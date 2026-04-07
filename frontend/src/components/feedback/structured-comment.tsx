import type { CommentBlock } from '@/types/interview'

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

  const items: Array<{ label: string; body: string }> = []
  if (block.positive && block.positive.trim().length > 0) {
    items.push({ label: positiveLabel, body: block.positive.trim() })
  }
  if (block.negative && block.negative.trim().length > 0) {
    items.push({ label: negativeLabel, body: block.negative.trim() })
  }
  if (block.suggestion && block.suggestion.trim().length > 0) {
    items.push({ label: suggestionLabel, body: block.suggestion.trim() })
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
