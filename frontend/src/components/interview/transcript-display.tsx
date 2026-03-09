interface TranscriptDisplayProps {
  interimText: string
  finalTexts: string[]
}

const TranscriptDisplay = ({ interimText, finalTexts }: TranscriptDisplayProps) => {
  const hasContent = finalTexts.length > 0 || interimText

  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4">
      <div className="mb-2 flex items-center gap-2">
        <span className="text-xs font-medium text-slate-500">실시간 자막</span>
        {interimText && <span className="h-2 w-2 animate-pulse rounded-full bg-emerald-500" />}
      </div>
      <div className="min-h-[3rem] text-sm leading-relaxed text-slate-700">
        {hasContent ? (
          <>
            {finalTexts.map((text, i) => (
              <span key={i}>{text} </span>
            ))}
            {interimText && <span className="text-slate-400">{interimText}</span>}
          </>
        ) : (
          <span className="text-slate-400">답변을 시작하면 자막이 표시됩니다...</span>
        )}
      </div>
    </div>
  )
}

export default TranscriptDisplay
