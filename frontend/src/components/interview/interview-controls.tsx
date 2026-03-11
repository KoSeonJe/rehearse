import { useEffect } from 'react'

interface InterviewControlsProps {
  phase: 'preparing' | 'greeting' | 'ready' | 'recording' | 'paused' | 'completed'
  currentIndex: number
  totalQuestions: number
  isVadActive?: boolean
  isTtsSpeaking?: boolean
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
  isTtsSpeaking,
  onStopAnswer,
  onNextQuestion,
  onPrevQuestion,
  onFinishInterview,
}: InterviewControlsProps) => {
  const isFirst = currentIndex === 0
  const isLast = currentIndex === totalQuestions - 1
  const isRecording = phase === 'recording'

  // 스페이스바로 답변 완료
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.code === 'Space' && isRecording && !e.repeat) {
        e.preventDefault()
        onStopAnswer()
      }
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [isRecording, onStopAnswer])

  return (
    <div className="fixed bottom-0 left-0 right-0 z-20 bg-white px-6 pb-10 pt-6 sm:relative sm:bg-transparent sm:px-0 sm:pb-0 sm:pt-0">
      <div className="mx-auto max-w-2xl space-y-10">
        {/* Main Action Area */}
        <div className="flex items-center justify-center">
          {phase === 'ready' ? (
            <div className="flex flex-col items-center gap-3">
              <div className="flex items-center gap-2 rounded-full bg-surface px-5 py-2.5">
                {isTtsSpeaking ? (
                  <>
                    <div className="h-2 w-2 rounded-full bg-accent animate-pulse" />
                    <span className="text-sm font-bold text-text-secondary">AI 면접관이 질문을 읽고 있어요</span>
                  </>
                ) : (
                  <>
                    <div className="h-2 w-2 rounded-full bg-success animate-pulse" />
                    <span className="text-sm font-bold text-text-secondary">말씀하시면 자동으로 시작됩니다</span>
                  </>
                )}
              </div>
            </div>
          ) : phase === 'paused' ? (
            <div className="flex w-full flex-col items-center gap-3 sm:w-auto sm:flex-row">
              <div className="flex items-center gap-2 rounded-full bg-surface px-5 py-2.5">
                {isTtsSpeaking ? (
                  <>
                    <div className="h-2 w-2 rounded-full bg-accent animate-pulse" />
                    <span className="text-sm font-bold text-text-secondary">AI 면접관이 질문을 읽고 있어요</span>
                  </>
                ) : (
                  <>
                    <div className="h-2 w-2 rounded-full bg-success animate-pulse" />
                    <span className="text-sm font-bold text-text-secondary">말씀하시면 자동으로 이어갑니다</span>
                  </>
                )}
              </div>
              {isLast && (
                <button
                  className="h-12 rounded-[16px] bg-surface px-8 text-sm font-extrabold text-text-primary transition-all active:scale-95"
                  onClick={onFinishInterview}
                >
                  종료하고 결과 보기
                </button>
              )}
            </div>
          ) : isRecording ? (
            <div className="flex flex-col items-center gap-6">
              <div className="flex items-center gap-2.5 rounded-full bg-surface px-5 py-2">
                <div className="h-2 w-2 rounded-full bg-error animate-pulse" />
                <span className="text-xs font-black text-text-primary tracking-tight">
                  {isVadActive ? '목소리 감지됨' : '열심히 듣고 있어요'}
                </span>
              </div>
              <div className="flex items-center gap-4">
                <button
                  className="h-12 rounded-[16px] bg-text-primary px-8 text-sm font-extrabold text-white transition-all active:scale-95"
                  onClick={onStopAnswer}
                >
                  답변 완료
                </button>
                <span className="text-[10px] font-bold text-text-tertiary hidden sm:inline">
                  Space로도 완료
                </span>
              </div>
            </div>
          ) : null}
        </div>

        {/* Toss-style Navigation & Progress */}
        <div className="flex items-center justify-between">
          <button
            onClick={onPrevQuestion}
            disabled={isFirst || isRecording}
            className="text-sm font-bold text-text-tertiary transition-colors hover:text-text-primary disabled:opacity-0"
          >
            이전
          </button>

          <div className="flex items-center gap-1.5" role="status">
            {Array.from({ length: totalQuestions }).map((_, i) => (
              <div
                key={i}
                className={`h-1 rounded-full transition-all duration-500 ${
                  i === currentIndex
                    ? 'w-10 bg-accent'
                    : i < currentIndex
                      ? 'w-2 bg-text-primary'
                      : 'w-2 bg-border'
                }`}
              />
            ))}
          </div>

          <button
            onClick={onNextQuestion}
            disabled={isLast || isRecording}
            className="text-sm font-bold text-text-tertiary transition-colors hover:text-text-primary disabled:opacity-0"
          >
            다음
          </button>
        </div>
      </div>
    </div>
  )
}
