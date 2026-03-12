import { useMemo } from 'react'
import { useInterviewStore } from '../../stores/interview-store'

export const TranscriptDisplay = () => {
  const interimText = useInterviewStore((s) => s.currentTranscript)
  const currentAnswer = useInterviewStore((s) => s.answers[s.currentQuestionIndex])

  const finalTexts = useMemo(
    () => currentAnswer?.transcripts.filter((t) => t.isFinal).map((t) => t.text) ?? [],
    [currentAnswer],
  )

  const hasContent = finalTexts.length > 0 || interimText

  return (
    <div className="rounded-card border border-border bg-surface p-4">
      <div className="mb-2 flex items-center gap-2">
        <span className="text-xs font-medium text-text-secondary">실시간 자막</span>
        {interimText && <span className="h-2 w-2 animate-pulse rounded-full bg-success" />}
      </div>
      <div className="min-h-[3rem] text-sm leading-relaxed text-text-primary">
        {hasContent ? (
          <>
            {finalTexts.map((text, i) => (
              <span key={i}>{text} </span>
            ))}
            {interimText && <span className="text-text-tertiary">{interimText}</span>}
          </>
        ) : (
          <span className="text-text-tertiary">답변을 시작하면 자막이 표시됩니다...</span>
        )}
      </div>
    </div>
  )
}
