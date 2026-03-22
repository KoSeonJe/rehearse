import { memo, useEffect, useRef } from 'react'
import { useInterviewStore } from '@/stores/interview-store'
import type { InterviewPhase } from '@/stores/interview-store'

const formatElapsed = (seconds: number) => {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

interface InterviewControlsProps {
  phase: InterviewPhase
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
  const currentFollowUp = useInterviewStore((s) => s.currentFollowUp)

  // 답변 경과 시간
  const elapsedRef = useRef<HTMLSpanElement>(null)

  useEffect(() => {
    if (!isRecording) {
      if (elapsedRef.current) elapsedRef.current.textContent = ''
      return
    }
    const start = Date.now()
    if (elapsedRef.current) elapsedRef.current.textContent = '00:00'
    const id = setInterval(() => {
      if (elapsedRef.current) {
        elapsedRef.current.textContent = formatElapsed(Math.floor((Date.now() - start) / 1000))
      }
    }, 1000)
    return () => clearInterval(id)
  }, [isRecording])

  // 스페이스바로 답변 완료
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

  const isWaiting = isTtsSpeaking || isFollowUpLoading

  // Finishing phase
  if (phase === 'finishing') {
    return (
      <div className="flex items-center gap-3">
        <span className="text-sm text-studio-text-secondary">수고하셨습니다!</span>
        <button
          className="cursor-pointer h-10 px-5 rounded-full bg-blue-500 text-sm font-medium text-white transition-all hover:bg-blue-600 active:scale-95"
          onClick={onFinishInterview}
        >
          면접 종료하기
        </button>
      </div>
    )
  }

  // Completed phase
  if (phase === 'completed') {
    return (
      <div className="flex items-center gap-3">
        <button
          className="cursor-pointer h-10 px-5 rounded-full bg-blue-500 text-sm font-medium text-white transition-all hover:bg-blue-600 active:scale-95"
          onClick={onFinishInterview}
        >
          피드백 보기
        </button>
      </div>
    )
  }

  return (
    <div className="flex items-center gap-3">
      {/* REC indicator */}
      {isRecording && (
        <div className="flex items-center gap-1.5 px-2">
          <div className="h-2 w-2 rounded-full bg-meet-red animate-pulse" />
          <span className="text-xs font-medium text-meet-red">REC</span>
          <span ref={elapsedRef} className="font-mono text-xs font-medium tabular-nums text-studio-text" />
        </div>
      )}

      {/* Answer control */}
      {isWaiting ? (
        <div className="flex items-center gap-2 px-3 py-2 rounded-full bg-studio-surface">
          <div className="h-2 w-2 rounded-full bg-blue-400 animate-pulse" />
          <span className="text-xs text-studio-text-secondary">
            {isFollowUpLoading ? '분석 중...' : 'AI 발언 중'}
          </span>
        </div>
      ) : isRecording ? (
        /* 답변 종료 — 빨간 pill 버튼 + 텍스트 */
        <button
          className="cursor-pointer h-10 px-5 rounded-full bg-meet-red flex items-center gap-2 text-sm font-medium text-white transition-all hover:brightness-110 active:scale-95"
          onClick={onStopAnswer}
          aria-label="답변 완료"
          title="답변 완료 (Space)"
        >
          <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
            <rect x="6" y="6" width="12" height="12" rx="2" />
          </svg>
          답변 종료
        </button>
      ) : phase !== 'preparing' ? (
        /* 답변 시작 — 초록 pill 버튼 + 텍스트 */
        <button
          className="cursor-pointer h-10 px-5 rounded-full bg-meet-green flex items-center gap-2 text-sm font-medium text-white transition-all hover:brightness-110 active:scale-95 disabled:opacity-40 disabled:pointer-events-none"
          disabled={isWaiting}
          onClick={onStartAnswer}
          aria-label={currentFollowUp ? '후속 질문에 답변하기' : '답변 시작'}
          title="Space 키로도 시작"
        >
          <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
            <path d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3z" />
            <path d="M17 11c0 2.76-2.24 5-5 5s-5-2.24-5-5H5c0 3.53 2.61 6.43 6 6.92V21h2v-3.08c3.39-.49 6-3.39 6-6.92h-2z" />
          </svg>
          {currentFollowUp ? '후속 답변' : '답변 시작'}
        </button>
      ) : null}
    </div>
  )
})

InterviewControls.displayName = 'InterviewControls'
