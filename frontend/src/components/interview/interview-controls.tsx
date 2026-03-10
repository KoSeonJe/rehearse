import { Button } from '../ui/button'

interface InterviewControlsProps {
  phase: 'preparing' | 'ready' | 'recording' | 'paused' | 'completed'
  currentIndex: number
  totalQuestions: number
  isVadActive?: boolean
  onStartAnswer: () => void
  onStopAnswer: () => void
  onNextQuestion: () => void
  onPrevQuestion: () => void
  onFinishInterview: () => void
}

export const InterviewControls = ({
  phase,
  currentIndex,
  totalQuestions,
  isVadActive,
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
    <div className="fixed bottom-0 left-0 right-0 z-20 border-t border-border bg-surface/95 px-4 py-4 backdrop-blur-sm sm:relative sm:border-0 sm:bg-transparent sm:px-0 sm:py-0 sm:backdrop-blur-none">
      <div className="mx-auto max-w-3xl space-y-4">
        <div className="flex items-center justify-center gap-3">
          {phase === 'ready' ? (
            <Button variant="cta" onClick={onStartAnswer}>
              준비 완료
            </Button>
          ) : phase === 'paused' ? (
            <>
              <Button variant="cta" onClick={onStartAnswer}>
                답변 계속
              </Button>
              {isLast && (
                <Button variant="primary" onClick={onFinishInterview}>
                  면접 종료
                </Button>
              )}
            </>
          ) : isRecording ? (
            <div className="flex items-center gap-3">
              {isVadActive && (
                <span className="flex items-center gap-2 text-xs text-text-secondary">
                  <span className="h-2 w-2 animate-pulse rounded-full bg-error" />
                  음성 감지 중
                </span>
              )}
              <Button variant="secondary" onClick={onStopAnswer}>
                답변 완료
              </Button>
            </div>
          ) : null}
        </div>

        <div className="flex items-center justify-between">
          <button
            onClick={onPrevQuestion}
            disabled={isFirst || isRecording}
            className="rounded-button text-sm text-text-secondary transition-colors hover:text-text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-30"
          >
            <span aria-hidden="true">← </span>이전 질문
          </button>

          <div className="flex gap-1.5" aria-label={`질문 ${currentIndex + 1} / ${totalQuestions}`} role="status">
            {Array.from({ length: totalQuestions }).map((_, i) => (
              <div
                key={i}
                className={`h-1.5 w-6 rounded-full transition-colors ${
                  i === currentIndex
                    ? 'bg-accent'
                    : i < currentIndex
                      ? 'bg-text-tertiary'
                      : 'bg-border'
                }`}
              />
            ))}
          </div>

          <button
            onClick={onNextQuestion}
            disabled={isLast || isRecording}
            className="rounded-button text-sm text-text-secondary transition-colors hover:text-text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-30"
          >
            다음 질문<span aria-hidden="true"> →</span>
          </button>
        </div>
      </div>
    </div>
  )
}
