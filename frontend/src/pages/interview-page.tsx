import { useState, useEffect, useCallback } from 'react'
import { useParams } from 'react-router-dom'
import { useInterviewStore, MAX_FOLLOWUP_ROUNDS } from '@/stores/interview-store'
import { useInterview } from '@/hooks/use-interviews'
import { useMediaStream } from '@/hooks/use-media-stream'
import { useMediaRecorder } from '@/hooks/use-media-recorder'
import { useInterviewSession } from '@/hooks/use-interview-session'
import { Logo } from '@/components/ui/logo'
import { AudioWaveform } from '@/components/interview/audio-waveform'
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

  // 개별 selector로 구독 — 빈번한 변경에 의한 리렌더 차단
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

  // ESC 키로 다이얼로그 닫기
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

  if (!interview || (!currentQuestion && !isGreeting)) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-white">
        <div className="text-center space-y-4">
          <div className="h-1 w-24 bg-accent/20 rounded-full mx-auto overflow-hidden">
            <div className="h-full bg-accent animate-progress-loading" />
          </div>
          <p className="font-mono text-[10px] font-black uppercase tracking-widest text-accent">AI 스튜디오 준비 중</p>
        </div>
      </div>
    )
  }

  return (
    <div className="flex min-h-screen flex-col bg-white text-text-primary">
      {/* Header */}
      <header className="px-6 py-6 flex items-center justify-between border-b border-border">
        <div className="flex items-center gap-4">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-accent shadow-lg shadow-accent/20">
            <Logo size={24} />
          </div>
          <h1 className="text-base font-black uppercase tracking-widest text-text-primary">AI 모의 면접</h1>
        </div>
        <div className="flex items-center gap-6">
          {(phase === 'ready' || phase === 'recording' || phase === 'paused') && (
            <button
              onClick={() => setShowFinishDialog(true)}
              className="px-4 py-2 text-xs font-bold text-text-secondary hover:text-error border border-border hover:border-error/30 rounded-full transition-all"
            >
              면접 종료
            </button>
          )}
          <InterviewTimer
            startTime={startTime}
            durationMinutes={interview.durationMinutes}
            onTimeWarning={() => setTimeWarning(true)}
            onTimeExpired={handleTimeExpired}
          />
        </div>
      </header>

      {/* Dual-View Layout */}
      <main className="flex-1 flex flex-col lg:flex-row gap-6 p-6 overflow-hidden">
        {/* Left: AI Interviewer Section */}
        <div className="flex-1 relative rounded-[32px] overflow-hidden bg-surface border border-border shadow-toss-lg">
          <div className="absolute inset-0 bg-gradient-to-br from-slate-50 via-white to-indigo-50/30" />

          {/* AI Waveform */}
          <div className="absolute inset-0 flex items-center justify-center">
            <div className="relative z-10 text-center">
              <AudioWaveform isSpeaking={isTtsSpeaking} size={240} />
              <div className="mt-8 space-y-2">
                <p className="font-mono text-[10px] font-black text-accent uppercase tracking-[0.3em]">AI 면접관</p>
                <div className="flex justify-center gap-1">
                  {[1, 2, 3].map(i => <div key={i} className="h-1 w-4 bg-accent/20 rounded-full" />)}
                </div>
              </div>
            </div>
          </div>

          {/* HUD Overlay Elements */}
          <div className="absolute inset-0 z-20 pointer-events-none p-8 flex flex-col justify-between">
            <div className="flex justify-end items-start gap-2">
              <div className="rounded-full bg-accent px-4 py-1.5 shadow-lg shadow-accent/20">
                <span className="text-[10px] font-black uppercase tracking-widest text-white">
                  {isGreeting ? '자기소개' : `질문 ${currentQuestionIndex + 1}`}
                </span>
              </div>
            </div>

            <div className="max-w-xl mx-auto w-full">
              {/* Time Warning Banner */}
              {timeWarning && (
                <div className="mb-4 flex justify-center">
                  <div className="rounded-full bg-warning/10 border border-warning/20 px-5 py-2 shadow-toss animate-fade-in">
                    <span className="text-sm font-bold text-warning">마무리할 시간입니다 - 곧 면접이 종료됩니다</span>
                  </div>
                </div>
              )}
              {/* Auto Transition Toast */}
              {autoTransitionMessage && (
                <div className="mb-4 flex justify-center">
                  <div className="rounded-full bg-accent/10 border border-accent/20 px-5 py-2 shadow-toss animate-fade-in">
                    <span className="text-sm font-bold text-accent">{autoTransitionMessage}</span>
                  </div>
                </div>
              )}
              {/* 메인질문 — 후속질문 로딩 중이거나 후속질문이 있으면 숨김 */}
              {!currentFollowUp && !isFollowUpLoading && (
                <div className="rounded-[24px] bg-white border border-border p-8 shadow-toss">
                  <p className="text-xl md:text-2xl font-extrabold leading-relaxed text-center tracking-tight text-text-primary">
                    {isGreeting ? '간단하게 자기소개를 해주세요' : currentQuestion?.content}
                  </p>
                </div>
              )}

              {/* 후속질문 로딩 — 답변 분석 중 */}
              {isFollowUpLoading && (
                <div className="rounded-[24px] bg-white/80 border border-accent/20 p-6 shadow-toss animate-fade-in">
                  <div className="flex items-center justify-center gap-3">
                    <div className="h-4 w-4 animate-spin rounded-full border-2 border-accent border-t-transparent" />
                    <span className="text-sm font-bold text-accent">후속 질문 생성 중...</span>
                  </div>
                </div>
              )}

              {/* 후속질문 카드 */}
              {currentFollowUp && !isFollowUpLoading && (
                <div className="rounded-[24px] bg-white border border-accent/30 p-6 shadow-toss animate-fade-in">
                  <div className="mb-3 flex items-center justify-center gap-2">
                    <span className="rounded-full bg-accent/10 px-3 py-1 text-[10px] font-black uppercase tracking-widest text-accent">
                      후속 질문 {followUpRound}/{MAX_FOLLOWUP_ROUNDS}
                    </span>
                    <span className="rounded-full bg-accent/10 px-3 py-1 text-[10px] font-black uppercase tracking-widest text-accent">
                      {FOLLOW_UP_TYPE_LABELS[currentFollowUp.type] ?? currentFollowUp.type}
                    </span>
                  </div>
                  <p className="text-lg md:text-xl font-extrabold leading-relaxed text-center tracking-tight text-text-primary">
                    {currentFollowUp.question}
                  </p>
                </div>
              )}

            </div>
          </div>
        </div>

        {/* Right: User Preview & Volume */}
        <div className="w-full lg:w-[400px] flex flex-col gap-6">
          {/* User Video */}
          <div className="relative aspect-video rounded-[32px] overflow-hidden bg-surface border border-border shadow-toss">
            <VideoPreview stream={mediaStream.stream} isRecording={phase === 'recording'} />
            <div className="absolute top-4 right-4">
              <div className="flex items-center gap-2 px-3 py-1.5 bg-white/80 backdrop-blur-md rounded-full border border-border">
                <div className={`h-1.5 w-1.5 rounded-full ${phase === 'recording' ? 'bg-error animate-pulse' : 'bg-text-tertiary'}`} />
                <span className="text-[10px] font-black uppercase tracking-tighter text-text-primary">{phase === 'recording' ? '답변 중' : '대기'}</span>
              </div>
            </div>
          </div>

          {/* Volume Indicator Area (replaced transcript) */}
          <div className="flex-1 rounded-[32px] bg-surface border border-border p-6 flex flex-col gap-4 overflow-hidden">
            <div className="flex items-center justify-between">
              <span className="text-[10px] font-black text-text-tertiary uppercase tracking-widest">음성 감지</span>
              <div className="flex gap-1">
                {[1, 2, 3].map(i => <div key={i} className="h-1 w-1 rounded-full bg-text-tertiary/30" />)}
              </div>
            </div>
            <div className="flex-1 flex items-center justify-center">
              {phase === 'recording' ? (
                <div className="text-center space-y-4">
                  <div className="flex items-end justify-center gap-1 h-16">
                    {[35, 65, 50, 80, 45, 70, 40].map((h, i) => (
                      <div
                        key={i}
                        className="w-2 rounded-full bg-accent animate-pulse"
                        style={{
                          height: `${h}%`,
                          animationDelay: `${i * 0.1}s`,
                          animationDuration: `${0.4 + i * 0.07}s`,
                        }}
                      />
                    ))}
                  </div>
                  <p className="text-xs font-bold text-text-secondary">음성을 녹음하고 있어요</p>
                  <p className="text-[10px] text-text-tertiary">답변 완료 후 AI가 분석합니다</p>
                </div>
              ) : (
                <div className="text-center space-y-2">
                  <div className="flex items-end justify-center gap-1 h-16">
                    {Array.from({ length: 7 }).map((_, i) => (
                      <div key={i} className="w-2 h-1 rounded-full bg-border" />
                    ))}
                  </div>
                  <p className="text-xs font-bold text-text-tertiary">답변을 시작하면 녹음이 시작됩니다</p>
                </div>
              )}
            </div>
          </div>
        </div>
      </main>

      {/* Floating Controls — 좌측 패널 기준 가운데 정렬 */}
      <div className="px-6 pb-6">
        <div className="flex flex-col lg:flex-row gap-6">
          <div className="flex-1">
            <InterviewControls
              phase={phase}
              isTtsSpeaking={isTtsSpeaking}
              isFollowUpLoading={isFollowUpLoading}
              onStartAnswer={handleStartAnswer}
              onStopAnswer={handleStopAnswer}
              onFinishInterview={handleFinishInterview}
            />
          </div>
          <div className="hidden lg:block lg:w-[400px]" />
        </div>
      </div>

      {/* 면접 종료 확인 다이얼로그 */}
      {showFinishDialog && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm"
          onClick={() => setShowFinishDialog(false)}
          role="dialog"
          aria-modal="true"
          aria-labelledby="finish-dialog-title"
        >
          <div
            className="w-full max-w-sm mx-4 rounded-[24px] bg-white p-8 shadow-toss-lg animate-fade-in"
            onClick={(e) => e.stopPropagation()}
          >
            <h2 id="finish-dialog-title" className="text-lg font-extrabold text-text-primary tracking-tight mb-2">면접을 종료하시겠습니까?</h2>
            <p className="text-sm text-text-secondary leading-relaxed mb-6">
              답변하지 않은 질문은 분석되지 않습니다.
            </p>
            <div className="flex gap-3">
              <button
                onClick={() => setShowFinishDialog(false)}
                className="flex-1 h-12 rounded-2xl border border-border text-sm font-bold text-text-secondary transition-all active:scale-95"
              >
                계속하기
              </button>
              <button
                onClick={() => {
                  setShowFinishDialog(false)
                  handleFinishInterview()
                }}
                className="flex-1 h-12 rounded-2xl bg-error text-sm font-bold text-white transition-all active:scale-95"
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
