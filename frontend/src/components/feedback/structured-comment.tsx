interface StructuredCommentProps {
  comment: string
}

const LINE_STYLES: Record<string, string> = {
  '✓': 'text-green-600',
  '△': 'text-orange-500',
  '→': 'text-blue-600',
}

const StructuredComment = ({ comment }: StructuredCommentProps) => {
  const lines = comment.split('\n').filter((line) => line.trim().length > 0)

  return (
    <div className="space-y-1">
      {lines.map((line, idx) => {
        const trimmed = line.trim()
        const prefix = Object.keys(LINE_STYLES).find((p) => trimmed.startsWith(p))

        if (prefix !== undefined) {
          return (
            <p key={idx} className={`text-xs leading-relaxed ${LINE_STYLES[prefix]}`}>
              {trimmed}
            </p>
          )
        }

        return (
          <p key={idx} className="text-xs leading-relaxed text-text-secondary">
            {trimmed}
          </p>
        )
      })}
    </div>
  )
}

export default StructuredComment
