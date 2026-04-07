interface StructuredCommentProps {
  comment: string
  positiveLabel?: string
  negativeLabel?: string
  suggestionLabel?: string
}

type Prefix = '✓' | '△' | '→'

const PREFIXES: readonly Prefix[] = ['✓', '△', '→'] as const

const StructuredComment = ({
  comment,
  positiveLabel = '잘한 점',
  negativeLabel = '아쉬운 점',
  suggestionLabel = '이렇게 말하면 더 좋아요',
}: StructuredCommentProps) => {
  const labelByPrefix: Record<Prefix, string> = {
    '✓': positiveLabel,
    '△': negativeLabel,
    '→': suggestionLabel,
  }

  const lines = comment.split('\n').filter((line) => line.trim().length > 0)

  return (
    <div className="space-y-3">
      {lines.map((line, idx) => {
        const trimmed = line.trim()
        const prefix = PREFIXES.find((p) => trimmed.startsWith(p))

        if (prefix !== undefined) {
          const body = trimmed.slice(prefix.length).trim()
          return (
            <div key={idx}>
              <p className="text-[13px] font-bold text-gray-500 mb-1">{labelByPrefix[prefix]}</p>
              <p className="text-[15px] leading-[1.7] text-gray-700">{body}</p>
            </div>
          )
        }

        return (
          <p key={idx} className="text-[15px] leading-[1.7] text-gray-700">
            {trimmed}
          </p>
        )
      })}
    </div>
  )
}

export default StructuredComment
