import { Button } from '../ui/button'

interface InterviewControlsProps {
  phase: 'preparing' | 'ready' | 'recording' | 'paused' | 'completed'
  currentIndex: number
  totalQuestions: number
  onStartAnswer: () => void
  onStopAnswer: () => void
  onNextQuestion: () => void
  onPrevQuestion: () => void
  onFinishInterview: () => void
}

const InterviewControls = ({
  phase,
  currentIndex,
  totalQuestions,
  onStartAnswer,
  onStopAnswer,
  onNextQuestion,
  onPrevQuestion,
  onFinishInterview,
}: InterviewControlsProps) => {
  const isFirst = currentIndex === 0
  const isLast = currentIndex === totalQuestions - 1
  const isRecording = phase === 'recording'

  return (
    <div className="space-y-4">
      <div className="flex justify-center gap-3">
        {phase === 'ready' || phase === 'paused' ? (
          <Button variant="cta" onClick={onStartAnswer}>
            {phase === 'ready' ? '답변 시작' : '답변 계속'}
          </Button>
        ) : isRecording ? (
          <Button variant="secondary" onClick={onStopAnswer}>
            답변 완료
          </Button>
        ) : null}

        {phase === 'paused' && isLast && (
          <Button variant="primary" onClick={onFinishInterview}>
            면접 종료
          </Button>
        )}
      </div>

      <div className="flex items-center justify-between">
        <button
          onClick={onPrevQuestion}
          disabled={isFirst || isRecording}
          className="text-sm text-slate-500 transition-colors hover:text-slate-900 disabled:cursor-not-allowed disabled:opacity-30"
        >
          ← 이전 질문
        </button>

        <div className="flex gap-1.5">
          {Array.from({ length: totalQuestions }).map((_, i) => (
            <div
              key={i}
              className={`h-1.5 w-6 rounded-full transition-colors ${
                i === currentIndex
                  ? 'bg-slate-900'
                  : i < currentIndex
                    ? 'bg-slate-400'
                    : 'bg-slate-200'
              }`}
            />
          ))}
        </div>

        <button
          onClick={onNextQuestion}
          disabled={isLast || isRecording}
          className="text-sm text-slate-500 transition-colors hover:text-slate-900 disabled:cursor-not-allowed disabled:opacity-30"
        >
          다음 질문 →
        </button>
      </div>
    </div>
  )
}

export default InterviewControls
