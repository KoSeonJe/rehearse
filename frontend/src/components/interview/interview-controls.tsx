import { memo, useEffect } from 'react'
import { useInterviewStore } from '@/stores/interview-store'

interface InterviewControlsProps {
  phase: 'preparing' | 'greeting' | 'ready' | 'recording' | 'paused' | 'finishing' | 'completed'
  isTtsSpeaking?: boolean
  isFollowUpLoading?: boolean
  onStartAnswer: () => void
  onStopAnswer: () => void
  onFinishInterview: () => void
}

export const InterviewControls = memo(({
  phase,
  isTtsSpeaking,
  isFollowUpLoading,
  onStartAnswer,
  onStopAnswer,
  onFinishInterview,
}: InterviewControlsProps) => {
  const isRecording = phase === 'recording'

  // 스페이스바로 답변 완료 (store에서 직접 읽어 stale closure 방지)
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.code !== 'Space' || e.repeat) return
      const currentPhase = useInterviewStore.getState().phase
      if (currentPhase !== 'recording') return
      e.preventDefault()
      onStopAnswer()
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [onStopAnswer])

  return (
    <div className="fixed bottom-0 left-0 right-0 z-20 bg-white px-6 pb-10 pt-6 sm:relative sm:bg-transparent sm:px-0 sm:pb-0 sm:pt-0">
      <div className="mx-auto max-w-2xl">
        <div className="flex flex-col items-center gap-2">
          {/* Space 힌트 — recording 시에만 */}
          {isRecording && (
            <span className="text-xs font-bold text-text-tertiary">Space로도 완료</span>
          )}

          {/* 단일 버튼 영역 — 항상 같은 위치 */}
          {phase === 'finishing' ? (
            <div className="flex flex-col items-center gap-4">
              <p className="text-sm font-bold text-text-secondary">수고하셨습니다! 면접이 끝났습니다.</p>
              <button
                className="h-12 rounded-[16px] bg-accent px-8 text-sm font-extrabold text-white transition-all active:scale-95"
                onClick={onFinishInterview}
              >
                면접 종료하기
              </button>
            </div>
          ) : isTtsSpeaking ? (
            <div className="flex items-center gap-2 rounded-full bg-surface px-5 py-2.5">
              <div className="h-2 w-2 rounded-full bg-accent animate-pulse" />
              <span className="text-sm font-bold text-text-secondary">AI 면접관이 말하고 있어요</span>
            </div>
          ) : isRecording ? (
            <button
              className="h-12 rounded-[16px] bg-text-primary px-8 text-sm font-extrabold text-white transition-all active:scale-95 disabled:opacity-50 disabled:pointer-events-none"
              disabled={isTtsSpeaking}
              onClick={onStopAnswer}
            >
              답변 완료
            </button>
          ) : phase === 'completed' ? (
            <button
              className="h-12 rounded-[16px] bg-accent px-8 text-sm font-extrabold text-white transition-all active:scale-95"
              onClick={onFinishInterview}
            >
              리포트 보기
            </button>
          ) : phase !== 'preparing' ? (
            <button
              className="h-12 rounded-[16px] bg-text-primary px-8 text-sm font-extrabold text-white transition-all active:scale-95 disabled:opacity-50 disabled:pointer-events-none"
              disabled={isTtsSpeaking || isFollowUpLoading}
              onClick={onStartAnswer}
            >
              {isFollowUpLoading ? '후속 질문 생성 중...' : '답변 시작'}
            </button>
          ) : null}
        </div>
      </div>
    </div>
  )
})

InterviewControls.displayName = 'InterviewControls'
