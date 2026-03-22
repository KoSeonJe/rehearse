import { useState, useEffect, useCallback, useMemo } from 'react'
import { useParams } from 'react-router-dom'
import { useInterviewStore, MAX_FOLLOWUP_ROUNDS } from '@/stores/interview-store'
import { useInterview } from '@/hooks/use-interviews'
import { useMediaStream } from '@/hooks/use-media-stream'
import { useMediaRecorder } from '@/hooks/use-media-recorder'
import { useInterviewSession } from '@/hooks/use-interview-session'
import { InterviewerAvatar } from '@/components/interview/interviewer-avatar'
import { VideoPreview } from '@/components/interview/video-preview'
import { InterviewControls } from '@/components/interview/interview-controls'
import { InterviewTimer } from '@/components/interview/interview-timer'

const FOLLOW_UP_TYPE_LABELS: Record<string, string> = {
  DEEP_DIVE: '심화',
  CLARIFICATION: '명확화',
  CHALLENGE: '반론',
  APPLICATION: '적용',
}

export const InterviewPage = () => {
  const { id } = useParams<{ id: string }>()
  const interviewId = id ?? ''

  const { data: response } = useInterview(interviewId)
  const interview = response?.data

  const questions = useInterviewStore((s) => s.questions)
  const currentQuestionIndex = useInterviewStore((s) => s.currentQuestionIndex)
  const phase = useInterviewStore((s) => s.phase)
  const startTime = useInterviewStore((s) => s.startTime)
  const greetingCompleted = useInterviewStore((s) => s.greetingCompleted)
  const autoTransitionMessage = useInterviewStore((s) => s.autoTransitionMessage)
  const currentFollowUp = useInterviewStore((s) => s.currentFollowUp)
  const isFollowUpLoading = useInterviewStore((s) => s.isFollowUpLoading)
  const followUpRound = useInterviewStore((s) => s.followUpRound)

  const mediaStream = useMediaStream()
  const recorder = useMediaRecorder()

  const {
    handleStartAnswer,
    handleStopAnswer,
    handleFinishInterview,
    handleTimeExpired,
    isTtsSpeaking,
  } = useInterviewSession({
    interviewId,
    interview,
    mediaStream,
    recorder,
  })
  const [timeWarning, setTimeWarning] = useState(false)
  const [showFinishDialog, setShowFinishDialog] = useState(false)

  const handleEscKey = useCallback((e: KeyboardEvent) => {
    if (e.key === 'Escape' && showFinishDialog) {
      setShowFinishDialog(false)
    }
  }, [showFinishDialog])

  useEffect(() => {
    if (showFinishDialog) {
      document.addEventListener('keydown', handleEscKey)
      return () => document.removeEventListener('keydown', handleEscKey)
    }
  }, [showFinishDialog, handleEscKey])

  const currentQuestion = questions[currentQuestionIndex]
  const isGreeting = !greetingCompleted && (phase === 'greeting' || (phase === 'recording' && currentQuestionIndex === 0))

  const avatarMood = useMemo(() => {
    if (isTtsSpeaking) return 'speaking' as const
    if (isFollowUpLoading) return 'thinking' as const
    if (phase === 'recording') return 'listening' as const
    return 'neutral' as const
  }, [isTtsSpeaking, isFollowUpLoading, phase])

  if (!interview || (!currentQuestion && !isGreeting)) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-studio-bg">
        <div className="text-center space-y-4">
          <div className="h-1 w-24 bg-studio-surface-elevated rounded-full mx-auto overflow-hidden">
            <div className="h-full bg-meet-green animate-progress-loading" />
          </div>
          <p className="text-xs font-medium text-studio-text-secondary">면접 준비 중...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="flex h-screen flex-col bg-studio-bg text-studio-text overflow-hidden">

      {/* ── Video Area (Google Meet PIP 레이아웃) ── */}
      <main className="flex-1 relative p-2 overflow-hidden">

        {/* AI Interviewer Tile — 전체 화면 */}
        <div className={`h-full w-full relative rounded-lg overflow-hidden bg-[#1a1a1a] transition-all duration-500 ${
          isTtsSpeaking ? 'ring-2 ring-meet-green' : phase === 'recording' ? 'ring-2 ring-meet-red animate-rec-pulse' : ''
        }`}>
          {/* Avatar centered */}
          <div className="absolute inset-0 flex items-center justify-center">
            <div className={`absolute w-[320px] h-[320px] rounded-full blur-[120px] transition-all duration-1000 pointer-events-none ${
              phase === 'recording' ? 'bg-meet-red/10 scale-105' : isTtsSpeaking ? 'bg-blue-500/15 scale-110' : 'bg-blue-500/5 scale-100'
            }`} />
            <InterviewerAvatar mood={avatarMood} size={200} />
          </div>

          {/* Name label — bottom-left */}
          <div className="absolute bottom-3 left-3 flex items-center gap-2 px-3 py-1 bg-black/60 rounded z-20">
            {isTtsSpeaking && (
              <svg className="w-3.5 h-3.5 text-meet-green" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3z" />
                <path d="M17 11c0 2.76-2.24 5-5 5s-5-2.24-5-5H5c0 3.53 2.61 6.43 6 6.92V21h2v-3.08c3.39-.49 6-3.39 6-6.92h-2z" />
              </svg>
            )}
            <span className="text-[13px] font-medium text-white">AI 면접관</span>
          </div>

          {/* Question overlay — bottom-center caption style */}
          <div className="absolute bottom-12 left-1/2 -translate-x-1/2 w-full max-w-2xl px-4 z-10">
            {/* Warnings */}
            {timeWarning && (
              <div className="mb-2 flex justify-center">
                <div className="rounded-md bg-[#F9AB00]/20 px-4 py-1.5 animate-fade-in">
                  <span className="text-sm font-medium text-[#F9AB00]">마무리할 시간입니다 - 곧 면접이 종료됩니다</span>
                </div>
              </div>
            )}
            {autoTransitionMessage && (
              <div className="mb-2 flex justify-center">
                <div className="rounded-md bg-blue-500/20 px-4 py-1.5 animate-fade-in">
                  <span className="text-sm font-medium text-blue-400">{autoTransitionMessage}</span>
                </div>
              </div>
            )}

            {/* Main question */}
            {!currentFollowUp && !isFollowUpLoading && (
              <div className="rounded-lg bg-black/70 backdrop-blur-sm px-6 py-4">
                <p className="text-base md:text-lg font-medium leading-relaxed text-center text-white">
                  {isGreeting ? '간단하게 자기소개를 해주세요' : currentQuestion?.content}
                </p>
              </div>
            )}

            {/* Follow-up loading */}
            {isFollowUpLoading && (
              <div className="rounded-lg bg-black/70 backdrop-blur-sm px-6 py-4 animate-fade-in">
                <div className="flex items-center justify-center gap-3">
                  <div className="h-4 w-4 animate-spin rounded-full border-2 border-blue-400 border-t-transparent" />
                  <span className="text-sm font-medium text-blue-400">후속 질문 생성 중...</span>
                </div>
              </div>
            )}

            {/* Follow-up question */}
            {currentFollowUp && !isFollowUpLoading && (
              <div className="rounded-lg bg-black/70 backdrop-blur-sm px-6 py-4 animate-fade-in">
                <div className="mb-2 flex items-center justify-center gap-2">
                  <span className="rounded bg-blue-500/30 px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider text-blue-300">
                    후속 {followUpRound + 1}/{MAX_FOLLOWUP_ROUNDS}
                  </span>
                  <span className="rounded bg-blue-500/30 px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider text-blue-300">
                    {FOLLOW_UP_TYPE_LABELS[currentFollowUp.type] ?? currentFollowUp.type}
                  </span>
                </div>
                <p className="text-base md:text-lg font-medium leading-relaxed text-center text-white">
                  {currentFollowUp.question}
                </p>
              </div>
            )}
          </div>
        </div>

        {/* User Video — PIP (우하단 소형 오버레이) */}
        <div className={`absolute bottom-4 right-4 w-[300px] aspect-video rounded-lg overflow-hidden shadow-2xl z-30 transition-all duration-300 ${
          phase === 'recording' ? 'ring-2 ring-meet-green' : 'ring-1 ring-white/10'
        }`}>
          <VideoPreview stream={mediaStream.stream} />
          {/* Name label */}
          <div className="absolute bottom-2 left-2 flex items-center gap-1.5 px-2 py-0.5 bg-black/60 rounded z-20">
            {phase === 'recording' && (
              <svg className="w-3 h-3 text-meet-green" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3z" />
                <path d="M17 11c0 2.76-2.24 5-5 5s-5-2.24-5-5H5c0 3.53 2.61 6.43 6 6.92V21h2v-3.08c3.39-.49 6-3.39 6-6.92h-2z" />
              </svg>
            )}
            <span className="text-[11px] font-medium text-white">나</span>
          </div>
        </div>
      </main>

      {/* ── Bottom Control Bar (Google Meet 스타일) ── */}
      <div className="flex items-center justify-between px-4 py-3 bg-studio-bg">

        {/* Left: Meeting info */}
        <div className="flex items-center gap-3 min-w-[200px]">
          <InterviewTimer
            startTime={startTime}
            durationMinutes={interview.durationMinutes}
            onTimeWarning={() => setTimeWarning(true)}
            onTimeExpired={handleTimeExpired}
          />
          <span className="text-[13px] text-studio-text-secondary">|</span>
          <span className="text-[13px] text-studio-text-secondary">AI 모의 면접</span>
        </div>

        {/* Center: Controls */}
        <InterviewControls
          phase={phase}
          isTtsSpeaking={isTtsSpeaking}
          isFollowUpLoading={isFollowUpLoading}
          onStartAnswer={handleStartAnswer}
          onStopAnswer={handleStopAnswer}
          onFinishInterview={handleFinishInterview}
        />

        {/* Right: Leave button (Google Meet: red pill, far right) */}
        <div className="flex justify-end min-w-[200px]">
          <button
            onClick={() => setShowFinishDialog(true)}
            className="cursor-pointer flex items-center gap-2 h-10 px-5 rounded-full bg-meet-red text-sm font-medium text-white transition-all active:scale-95"
          >
            <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
              <path d="M12 9c-1.6 0-3.15.25-4.6.72v3.1c0 .39-.23.74-.56.9-.98.49-1.87 1.12-2.66 1.85-.18.18-.43.28-.7.28-.28 0-.53-.11-.71-.29L.29 13.08c-.18-.17-.29-.42-.29-.7 0-.28.11-.53.29-.71C3.34 8.78 7.46 7 12 7s8.66 1.78 11.71 4.67c.18.18.29.43.29.71 0 .28-.11.53-.29.71l-2.48 2.48c-.18.18-.43.29-.71.29-.27 0-.52-.1-.7-.28-.79-.73-1.68-1.36-2.66-1.85-.33-.16-.56-.5-.56-.9v-3.1C15.15 9.25 13.6 9 12 9z" />
            </svg>
            면접 종료
          </button>
        </div>
      </div>

      {/* ── Finish Dialog ── */}
      {showFinishDialog && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/60"
          onClick={() => setShowFinishDialog(false)}
          role="dialog"
          aria-modal="true"
          aria-labelledby="finish-dialog-title"
        >
          <div
            className="w-full max-w-sm mx-4 rounded-2xl bg-[#2c2c2c] border border-[#3c4043] p-6 shadow-2xl animate-fade-in"
            onClick={(e) => e.stopPropagation()}
          >
            <h2 id="finish-dialog-title" className="text-base font-medium text-white mb-2">면접을 종료하시겠습니까?</h2>
            <p className="text-sm text-studio-text-secondary leading-relaxed mb-6">
              답변하지 않은 질문은 분석되지 않습니다.
            </p>
            <div className="flex justify-end gap-2">
              <button
                onClick={() => setShowFinishDialog(false)}
                className="cursor-pointer h-9 px-4 rounded-full text-sm font-medium text-blue-400 hover:bg-blue-400/10 transition-all"
              >
                계속하기
              </button>
              <button
                onClick={() => {
                  setShowFinishDialog(false)
                  handleFinishInterview()
                }}
                className="cursor-pointer h-9 px-4 rounded-full bg-blue-500 text-sm font-medium text-white transition-all hover:bg-blue-600 active:scale-95"
              >
                종료하기
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
