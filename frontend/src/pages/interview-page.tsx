import { useState, useEffect, useCallback, useMemo } from 'react'
import { Helmet } from 'react-helmet-async'
import { useParams, useNavigate } from 'react-router-dom'
import { useInterviewStore, type InterviewPhase } from '@/stores/interview-store'
import { useInterview } from '@/hooks/use-interviews'
import { useMediaStream } from '@/hooks/use-media-stream'
import { useMediaRecorder } from '@/hooks/use-media-recorder'
import { useInterviewSession } from '@/hooks/use-interview-session'
import { useInterviewExitGuard } from '@/hooks/use-interview-exit-guard'
import { ApiError } from '@/lib/api-client'

// 이탈 가드가 활성화되는 phase (positive whitelist — preparing/completed는 제외)
const GUARD_ACTIVE_PHASES: ReadonlySet<InterviewPhase> = new Set([
  'greeting',
  'ready',
  'recording',
  'paused',
  'finishing',
])

import { VideoPreview } from '@/components/interview/video-preview'
import { InterviewControls } from '@/components/interview/interview-controls'
import { InterviewTimer } from '@/components/interview/interview-timer'
import { FinishingOverlay } from '@/components/interview/finishing-overlay'
import { UploadRecoveryDialog } from '@/components/interview/upload-recovery-dialog'

/** 마이크 아이콘 — nameplate·dock에서 공용 사용 */
const MicIcon = ({ className }: { className?: string }) => (
  <svg className={className} viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
    <path d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3z" />
    <path d="M17 11c0 2.76-2.24 5-5 5s-5-2.24-5-5H5c0 3.53 2.61 6.43 6 6.92V21h2v-3.08c3.39-.49 6-3.39 6-6.92h-2z" />
  </svg>
)

// ─── 로딩 화면 ───────────────────────────────────────────────────────────────

const LoadingScreen = () => (
  /* interview-page와 동일하게 `dark` 클래스로 스코프 강제 — 라이트 테마에서도
     스테이지 텍스트가 off-white로 해석되도록 보장 (C1 contrast 버그 수정). */
  <div className="dark flex min-h-screen items-center justify-center bg-interview-stage">
    <div className="text-center space-y-4">
      <div className="h-px w-24 bg-foreground/20 mx-auto overflow-hidden">
        <div className="h-full bg-foreground/60 animate-progress-loading" />
      </div>
      <div className="flex items-center justify-center gap-2">
        <div className="h-3 w-3 animate-spin rounded-full border border-foreground/40 border-t-transparent" aria-hidden="true" />
        <p className="text-sm font-medium text-foreground">준비 중</p>
      </div>
    </div>
  </div>
)

// ─── 질문 Caption (비디오 하단 중앙 오버레이) ─────────────────────────────────

interface QuestionCaptionProps {
  isGreeting: boolean
  currentQuestion: { content: string } | undefined
  currentFollowUp: { question: string; type: string } | null
  isFollowUpLoading: boolean
  timeWarning: boolean
  autoTransitionMessage: string | null
}

const QuestionCaption = ({
  isGreeting,
  currentQuestion,
  currentFollowUp,
  isFollowUpLoading,
  timeWarning,
  autoTransitionMessage,
}: QuestionCaptionProps) => (
  <div className="pointer-events-none absolute bottom-24 left-1/2 z-10 w-[calc(100vw-32px)] max-w-3xl -translate-x-1/2 space-y-2">
    {timeWarning && (
      <div className="pointer-events-auto rounded-lg bg-signal-warning-bg/90 backdrop-blur-sm border border-signal-warning/25 px-4 py-2 text-center animate-fade-in">
        <span className="text-xs font-medium text-signal-warning">마무리할 시간입니다</span>
      </div>
    )}

    {autoTransitionMessage && (
      <div className="pointer-events-auto rounded-lg bg-interview-stage/85 backdrop-blur-md ring-1 ring-white/5 px-4 py-2 text-center animate-fade-in">
        <span className="text-xs text-foreground/70">{autoTransitionMessage}</span>
      </div>
    )}

    {isFollowUpLoading ? (
      <div className="pointer-events-auto flex items-center justify-center gap-2 rounded-xl bg-interview-stage/85 backdrop-blur-md ring-1 ring-white/5 px-5 py-3 animate-fade-in">
        <div className="h-3 w-3 animate-spin rounded-full border border-foreground/30 border-t-transparent" />
        <span className="text-xs text-foreground/50">후속 질문 생성 중</span>
      </div>
    ) : currentFollowUp ? (
      <div className="pointer-events-auto rounded-xl bg-interview-stage/85 backdrop-blur-md ring-1 ring-white/5 px-5 py-3 text-center animate-fade-in">
        <p className="text-sm md:text-base font-medium leading-relaxed text-foreground line-clamp-3">
          {currentFollowUp.question}
        </p>
      </div>
    ) : (
      <div className="pointer-events-auto rounded-xl bg-interview-stage/85 backdrop-blur-md ring-1 ring-white/5 px-5 py-3 text-center animate-fade-in">
        <p className="text-sm md:text-base font-medium leading-relaxed text-foreground line-clamp-3">
          {isGreeting ? '간단하게 자기소개를 해주세요' : currentQuestion?.content}
        </p>
      </div>
    )}
  </div>
)

// ─── 종료 다이얼로그 ──────────────────────────────────────────────────────────

interface FinishDialogProps {
  onCancel: () => void
  onConfirm: () => void
}

const FinishDialog = ({ onCancel, onConfirm }: FinishDialogProps) => (
  <div
    className="fixed inset-0 z-50 flex items-center justify-center bg-black/70"
    onClick={onCancel}
    role="dialog"
    aria-modal="true"
    aria-labelledby="finish-dialog-title"
  >
    <div
      className="w-full max-w-sm mx-4 rounded-2xl bg-card border border-foreground/10 p-6 shadow-lg animate-fade-in"
      onClick={(e) => e.stopPropagation()}
    >
      <h2 id="finish-dialog-title" className="text-base font-semibold text-foreground mb-2">
        면접을 종료하시겠습니까?
      </h2>
      <p className="text-sm text-muted-foreground leading-relaxed mb-6">
        답변하지 않은 질문은 분석되지 않습니다.
      </p>
      <div className="flex justify-end gap-2">
        <button
          onClick={onCancel}
          className="cursor-pointer h-11 min-w-[44px] px-4 rounded-full text-sm font-medium text-muted-foreground hover:text-foreground hover:bg-foreground/6 transition-colors duration-[var(--duration-fast)]"
        >
          계속하기
        </button>
        <button
          onClick={onConfirm}
          className="cursor-pointer h-11 min-w-[44px] px-4 rounded-full bg-brand text-sm font-medium text-brand-foreground transition-colors duration-[var(--duration-fast)] hover:bg-brand-hover active:scale-95"
        >
          종료하기
        </button>
      </div>
    </div>
  </div>
)

// ─── 이탈 가드 다이얼로그 ────────────────────────────────────────────────────

interface ExitGuardDialogProps {
  phase: InterviewPhase
  onDismiss: () => void
  onConfirmExit: () => void
}

const ExitGuardDialog = ({ phase, onDismiss, onConfirmExit }: ExitGuardDialogProps) => (
  <div
    className="fixed inset-0 z-[60] flex items-center justify-center bg-black/75"
    role="dialog"
    aria-modal="true"
    aria-labelledby="exit-guard-title"
    aria-describedby="exit-guard-desc"
  >
    <div className="w-full max-w-sm mx-4 rounded-2xl bg-card border border-foreground/10 p-6 shadow-lg animate-fade-in">
      <h2 id="exit-guard-title" className="text-base font-semibold text-foreground mb-2">
        {phase === 'finishing' ? '면접 종료 처리 중입니다' : '면접을 나가시겠습니까?'}
      </h2>
      <p id="exit-guard-desc" className="text-sm text-muted-foreground leading-relaxed mb-6">
        {phase === 'finishing'
          ? '안전하게 저장 중이에요. 잠시만 기다려주세요.'
          : '지금 나가면 녹화된 답변이 저장되지 않을 수 있어요. 정식으로 종료하시겠습니까?'}
      </p>
      <div className="flex justify-end gap-2">
        <button
          onClick={onDismiss}
          className="cursor-pointer h-11 min-w-[44px] px-4 rounded-full text-sm font-medium text-muted-foreground hover:text-foreground hover:bg-foreground/6 transition-colors duration-[var(--duration-fast)]"
        >
          계속 면접
        </button>
        {phase !== 'finishing' && (
          <button
            onClick={onConfirmExit}
            className="cursor-pointer h-11 min-w-[44px] px-4 rounded-full bg-signal-record text-sm font-medium text-white transition-colors duration-[var(--duration-fast)] hover:brightness-110 active:scale-95"
          >
            면접 종료하기
          </button>
        )}
      </div>
    </div>
  </div>
)

// ─── InterviewPage ────────────────────────────────────────────────────────────

export const InterviewPage = () => {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const interviewId = id ?? ''

  // 이전 세션의 store 잔상 제거
  useEffect(() => {
    const parsed = Number(interviewId)
    if (!parsed) return
    const storedId = useInterviewStore.getState().interviewId
    if (storedId !== parsed) {
      useInterviewStore.getState().reset()
    }
  }, [interviewId])

  const { data: response, error } = useInterview(interviewId)
  const interview = response?.data

  useEffect(() => {
    if (error instanceof ApiError && error.status === 404) {
      navigate('/dashboard', { replace: true })
      return
    }
    if (!interview) return
    if (interview.status === 'COMPLETED') {
      if (interview.publicId) {
        navigate(`/interview/${interview.publicId}/feedback`, { replace: true })
      } else {
        navigate('/dashboard', { replace: true })
      }
    }
  }, [interview, error, navigate])

  const questions = useInterviewStore((s) => s.questions)
  const currentQuestionIndex = useInterviewStore((s) => s.currentQuestionIndex)
  const phase = useInterviewStore((s) => s.phase)
  const startTime = useInterviewStore((s) => s.startTime)
  const greetingCompleted = useInterviewStore((s) => s.greetingCompleted)
  const autoTransitionMessage = useInterviewStore((s) => s.autoTransitionMessage)
  const currentFollowUp = useInterviewStore((s) => s.currentFollowUp)
  const isFollowUpLoading = useInterviewStore((s) => s.isFollowUpLoading)

  const mediaStream = useMediaStream()
  const recorder = useMediaRecorder()

  const {
    handleStartAnswer,
    handleStopAnswer,
    handleFinishInterview,
    handleTimeExpired,
    isTtsSpeaking,
    finishingProgress,
    uploadFailureState,
  } = useInterviewSession({
    interviewId,
    interview,
    mediaStream,
    recorder,
  })

  const [timeWarning, setTimeWarning] = useState(false)
  const [showFinishDialog, setShowFinishDialog] = useState(false)
  const [isExitConfirmed, setIsExitConfirmed] = useState(false)

  const { blocked: exitBlocked, dismiss: dismissExit } = useInterviewExitGuard({
    active:
      interview != null &&
      interview.status === 'IN_PROGRESS' &&
      GUARD_ACTIVE_PHASES.has(phase),
    suppress: uploadFailureState !== null || isExitConfirmed,
  })

  const handleEscKey = useCallback(
    (e: KeyboardEvent) => {
      if (e.key === 'Escape' && showFinishDialog) {
        setShowFinishDialog(false)
      }
    },
    [showFinishDialog],
  )

  useEffect(() => {
    if (showFinishDialog) {
      document.addEventListener('keydown', handleEscKey)
      return () => document.removeEventListener('keydown', handleEscKey)
    }
  }, [showFinishDialog, handleEscKey])

  const currentQuestion = questions[currentQuestionIndex]
  const isGreeting =
    !greetingCompleted &&
    (phase === 'greeting' || (phase === 'recording' && currentQuestionIndex === 0))

  const avatarMood = useMemo(() => {
    if (isTtsSpeaking) return 'speaking' as const
    if (isFollowUpLoading) return 'thinking' as const
    if (phase === 'recording') return 'listening' as const
    return 'neutral' as const
  }, [isTtsSpeaking, isFollowUpLoading, phase])

  const handleConfirmFinish = useCallback(() => {
    setShowFinishDialog(false)
    handleFinishInterview()
  }, [handleFinishInterview])

  const handleConfirmExit = useCallback(() => {
    setIsExitConfirmed(true)
    dismissExit()
    void (async () => {
      try {
        await handleFinishInterview()
      } finally {
        setIsExitConfirmed(false)
      }
    })()
  }, [dismissExit, handleFinishInterview])

  if (!interview || (!currentQuestion && !isGreeting)) {
    return <LoadingScreen />
  }

  return (
    /* interview-page는 테마 무관 강제 다크 (몰입 모드). `dark` 클래스 스코프로
       내부의 text-foreground/muted-foreground가 다크 팔레트(off-white)로 해석되도록 한다.
       라이트 테마 사용자가 스테이지 진입 시 텍스트가 invisible 되는 이슈 방지. */
    <div className="dark flex h-screen flex-col bg-interview-stage text-foreground overflow-hidden">
      <Helmet>
        <title>면접 진행 중 - 리허설</title>
        <meta name="robots" content="noindex, nofollow" />
      </Helmet>

      {/* ── 메인 스테이지: 화상통화 레이아웃 (spotlight + PIP + floating dock) ── */}
      <main className="relative flex-1 overflow-hidden">
        {/* AI 면접관 — full-bleed spotlight 화면 */}
        <div className="absolute inset-0 bg-interview-stage">
          <img
            src="/images/interviewer-avatar.png"
            alt=""
            aria-hidden="true"
            className="h-full w-full object-cover"
          />
          {/* 하단 그라데이션 — nameplate·caption·dock 가독성 확보 */}
          <div className="absolute inset-x-0 bottom-0 h-2/5 bg-gradient-to-t from-interview-stage via-interview-stage/60 to-transparent pointer-events-none" />

          {/* Active speaker glow — AI 말할 때 teal, thinking dots */}
          {avatarMood === 'speaking' && (
            <div className="absolute inset-0 pointer-events-none ring-[3px] ring-inset ring-brand/40 motion-safe:animate-[fade-in_1.2s_ease-in-out_infinite_alternate] motion-reduce:opacity-70" />
          )}
          {avatarMood === 'thinking' && (
            <div className="absolute bottom-36 left-1/2 -translate-x-1/2 flex items-center gap-1.5 z-10">
              {[0, 1, 2].map(i => (
                <div
                  key={i}
                  className="h-2 w-2 rounded-full bg-foreground/70 animate-bounce"
                  style={{ animationDelay: `${i * 0.15}s` }}
                />
              ))}
            </div>
          )}
        </div>

        {/* AI 면접관 Nameplate — 좌하단 (Meet/Zoom 스타일) */}
        <div className="absolute bottom-6 left-6 z-10 flex items-center gap-1.5 px-2.5 py-1.5 rounded-md bg-interview-stage/70 backdrop-blur-sm ring-1 ring-white/5">
          <MicIcon className={`w-3 h-3 ${isTtsSpeaking ? 'text-brand animate-pulse' : 'text-foreground/50'}`} />
          <span className="text-[11px] font-medium text-foreground/80">AI 면접관</span>
        </div>

        {/* 사용자 비디오 PIP (우하단, dock 위쪽) — 녹화 중 red glow */}
        <div
          className={`absolute bottom-28 right-4 md:bottom-28 md:right-6 w-[200px] md:w-[320px] aspect-video rounded-lg overflow-hidden shadow-2xl z-20 transition-[box-shadow,--tw-ring-color] duration-[var(--duration-normal)] ${
            phase === 'recording'
              ? 'ring-2 ring-signal-record shadow-[0_0_24px_rgba(200,50,42,0.35)]'
              : 'ring-1 ring-white/10'
          }`}
        >
          <VideoPreview stream={mediaStream.stream} />
          {/* 사용자 Nameplate */}
          <div className="absolute bottom-1.5 left-1.5 z-10 flex items-center gap-1 px-2 py-1 rounded bg-interview-stage/70 backdrop-blur-sm">
            <MicIcon className={`w-2.5 h-2.5 ${phase === 'recording' ? 'text-signal-record animate-pulse' : 'text-foreground/50'}`} />
            <span className="text-[10px] font-medium text-foreground/80">나</span>
          </div>
        </div>

        {/* 질문 Subtitle Bar — 하단 중앙 (dock 위쪽에 떠있음) */}
        <QuestionCaption
          isGreeting={isGreeting}
          currentQuestion={currentQuestion}
          currentFollowUp={currentFollowUp}
          isFollowUpLoading={isFollowUpLoading}
          timeWarning={timeWarning}
          autoTransitionMessage={autoTransitionMessage}
        />

        {/* Floating Control Dock — 하단 중앙 Meet/FaceTime 스타일 pill */}
        <div className="absolute bottom-5 left-1/2 -translate-x-1/2 z-30 flex items-center gap-2 md:gap-3 px-3 py-2 rounded-full bg-interview-stage/85 backdrop-blur-md ring-1 ring-white/10 shadow-2xl">
          {/* 타이머 + 진행도 + REC dot */}
          <div className="flex items-center gap-2 pl-1 pr-3">
            <span className="relative flex h-2 w-2" aria-hidden="true">
              {phase === 'recording' && (
                <span className="absolute inset-0 rounded-full bg-signal-record animate-ping opacity-60" />
              )}
              <span className={`relative h-2 w-2 rounded-full ${phase === 'recording' ? 'bg-signal-record' : 'bg-foreground/30'}`} />
            </span>
            <InterviewTimer
              startTime={startTime}
              durationMinutes={interview.durationMinutes}
              onTimeWarning={() => setTimeWarning(true)}
              onTimeExpired={handleTimeExpired}
            />
            {!isGreeting && questions.length > 0 && (
              <>
                <span className="hidden text-xs text-foreground/15 sm:inline">·</span>
                <span className="hidden font-tabular text-xs text-foreground/50 sm:inline">
                  Q{currentQuestionIndex + 1}/{questions.length}
                </span>
              </>
            )}
          </div>

          <span className="h-6 w-px bg-white/10" aria-hidden="true" />

          {/* 답변 컨트롤 */}
          <InterviewControls
            phase={phase}
            isTtsSpeaking={isTtsSpeaking}
            isFollowUpLoading={isFollowUpLoading}
            onStartAnswer={handleStartAnswer}
            onStopAnswer={handleStopAnswer}
            onFinishInterview={handleFinishInterview}
          />

          {phase !== 'finishing' && phase !== 'completed' && (
            <>
              <span className="h-6 w-px bg-white/10" aria-hidden="true" />
              <button
                onClick={() => setShowFinishDialog(true)}
                className="cursor-pointer h-10 min-w-[44px] rounded-full border border-signal-record/30 px-3 md:px-4 text-xs md:text-sm font-medium text-signal-record/90 transition-colors duration-[var(--duration-fast)] hover:bg-signal-record/10 hover:border-signal-record hover:text-signal-record active:scale-95"
              >
                <span className="hidden md:inline">면접 종료</span>
                <span className="md:hidden">종료</span>
              </button>
            </>
          )}
        </div>
      </main>

      {/* ── 종료 확인 다이얼로그 ── */}
      {showFinishDialog && (
        <FinishDialog
          onCancel={() => setShowFinishDialog(false)}
          onConfirm={handleConfirmFinish}
        />
      )}

      {/* ── 면접 안전하게 종료 중 오버레이 ── */}
      <FinishingOverlay
        open={finishingProgress !== null}
        stage={finishingProgress?.stage ?? 'uploading'}
        total={finishingProgress?.total ?? 0}
        completed={finishingProgress?.completed ?? 0}
      />

      {/* ── 업로드 복구 실패 시 사용자 확인 다이얼로그 ── */}
      <UploadRecoveryDialog
        open={uploadFailureState !== null}
        failedCount={uploadFailureState?.failedCount ?? 0}
        onConfirm={() => uploadFailureState?.resolve(true)}
        onCancel={() => uploadFailureState?.resolve(false)}
      />

      {/* ── 뒤로가기 이탈 가드 다이얼로그 ── */}
      {exitBlocked && (
        <ExitGuardDialog
          phase={phase}
          onDismiss={dismissExit}
          onConfirmExit={handleConfirmExit}
        />
      )}
    </div>
  )
}
