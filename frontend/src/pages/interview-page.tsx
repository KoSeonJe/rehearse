import { useState, useEffect, useCallback, useMemo } from 'react'
import { Helmet } from 'react-helmet-async'
import { useParams, useNavigate } from 'react-router-dom'
import { useInterviewStore, MAX_FOLLOWUP_ROUNDS, type InterviewPhase } from '@/stores/interview-store'
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

import { InterviewerAvatar } from '@/components/interview/interviewer-avatar'
import { VideoPreview } from '@/components/interview/video-preview'
import { InterviewControls } from '@/components/interview/interview-controls'
import { InterviewTimer } from '@/components/interview/interview-timer'
import { FinishingOverlay } from '@/components/interview/finishing-overlay'
import { UploadRecoveryDialog } from '@/components/interview/upload-recovery-dialog'

const FOLLOW_UP_TYPE_LABELS: Record<string, string> = {
  DEEP_DIVE: '심화',
  CLARIFICATION: '명확화',
  CHALLENGE: '반론',
  APPLICATION: '적용',
}

// ─── 로딩 화면 ───────────────────────────────────────────────────────────────

const LoadingScreen = () => (
  <div className="flex min-h-screen items-center justify-center bg-interview-stage">
    <div className="text-center space-y-4">
      <div className="h-px w-24 bg-foreground/10 mx-auto overflow-hidden">
        <div className="h-full bg-foreground/30 animate-progress-loading" />
      </div>
      <p className="text-xs font-medium text-foreground/40 tracking-widest uppercase">준비 중</p>
    </div>
  </div>
)

// ─── REC 라벨 (상단 좌측 고정) ───────────────────────────────────────────────

interface RecLabelProps {
  visible: boolean
}

const RecLabel = ({ visible }: RecLabelProps) => (
  <div
    className="flex items-center gap-1.5 px-2 py-1"
    role="status"
    aria-live="polite"
    aria-label={visible ? '녹화 중' : '녹화 대기'}
  >
    <span
      className={`block w-2 h-2 rounded-full bg-signal-record transition-opacity duration-700 ${
        visible ? 'opacity-100' : 'opacity-0'
      }`}
      aria-hidden="true"
    />
    <span
      className={`text-[11px] font-semibold tracking-widest text-signal-record uppercase transition-opacity duration-700 ${
        visible ? 'opacity-100' : 'opacity-0'
      }`}
    >
      REC
    </span>
  </div>
)

// ─── 질문 표시 영역 ──────────────────────────────────────────────────────────

interface QuestionDisplayProps {
  isGreeting: boolean
  currentQuestion: { content: string } | undefined
  currentFollowUp: { question: string; type: string } | null
  isFollowUpLoading: boolean
  followUpRound: number
  timeWarning: boolean
  autoTransitionMessage: string | null
}

const QuestionDisplay = ({
  isGreeting,
  currentQuestion,
  currentFollowUp,
  isFollowUpLoading,
  followUpRound,
  timeWarning,
  autoTransitionMessage,
}: QuestionDisplayProps) => (
  <div className="flex-1 flex flex-col gap-3 overflow-y-auto">
    {/* 시간 경고 */}
    {timeWarning && (
      <div className="px-3 py-2 rounded bg-signal-warning-bg border border-signal-warning/20 animate-fade-in">
        <span className="text-xs font-medium text-signal-warning">마무리할 시간입니다</span>
      </div>
    )}

    {/* 자동 전환 메시지 */}
    {autoTransitionMessage && (
      <div className="px-3 py-2 rounded bg-foreground/5 border border-foreground/10 animate-fade-in">
        <span className="text-xs text-foreground/60">{autoTransitionMessage}</span>
      </div>
    )}

    {/* 메인 질문 */}
    {!currentFollowUp && !isFollowUpLoading && (
      <div className="animate-fade-in">
        <p className="text-sm font-medium leading-relaxed text-foreground/80">
          {isGreeting ? '간단하게 자기소개를 해주세요' : currentQuestion?.content}
        </p>
      </div>
    )}

    {/* 후속 질문 로딩 */}
    {isFollowUpLoading && (
      <div className="flex items-center gap-2 animate-fade-in">
        <div className="h-3 w-3 animate-spin rounded-full border border-foreground/30 border-t-transparent" />
        <span className="text-xs text-foreground/40">후속 질문 생성 중</span>
      </div>
    )}

    {/* 후속 질문 */}
    {currentFollowUp && !isFollowUpLoading && (
      <div className="animate-fade-in">
        <div className="flex items-center gap-1.5 mb-2">
          <span className="rounded-xs bg-accent-editorial-bg px-1.5 py-0.5 text-[10px] font-semibold uppercase tracking-wider text-accent-editorial">
            후속 {followUpRound + 1}/{MAX_FOLLOWUP_ROUNDS}
          </span>
          <span className="rounded-xs bg-accent-editorial-bg px-1.5 py-0.5 text-[10px] font-semibold uppercase tracking-wider text-accent-editorial">
            {FOLLOW_UP_TYPE_LABELS[currentFollowUp.type] ?? currentFollowUp.type}
          </span>
        </div>
        <p className="text-sm font-medium leading-relaxed text-foreground/80">
          {currentFollowUp.question}
        </p>
      </div>
    )}
  </div>
)

// ─── 우측 Rail (lg+) ─────────────────────────────────────────────────────────

interface RightRailProps {
  isGreeting: boolean
  currentQuestion: { content: string } | undefined
  currentFollowUp: { question: string; type: string } | null
  isFollowUpLoading: boolean
  followUpRound: number
  timeWarning: boolean
  autoTransitionMessage: string | null
  phase: InterviewPhase
  startTime: number | null
  durationMinutes: number | null | undefined
  questions: { content: string }[]
  currentQuestionIndex: number
  isTtsSpeaking: boolean
  onTimeWarning: () => void
  onTimeExpired: () => void
  onStartAnswer: () => void
  onStopAnswer: () => void
  onFinishInterview: () => void
  onRequestFinish: () => void
}

const RightRail = ({
  isGreeting,
  currentQuestion,
  currentFollowUp,
  isFollowUpLoading,
  followUpRound,
  timeWarning,
  autoTransitionMessage,
  phase,
  startTime,
  durationMinutes,
  questions,
  currentQuestionIndex,
  isTtsSpeaking,
  onTimeWarning,
  onTimeExpired,
  onStartAnswer,
  onStopAnswer,
  onFinishInterview,
  onRequestFinish,
}: RightRailProps) => (
  <aside
    className="hidden md:flex flex-col md:w-64 lg:w-72 xl:w-80 h-full border-l border-foreground/8 bg-interview-stage"
    aria-label="면접 컨트롤"
  >
    {/* 헤더: 진행 현황 */}
    <div className="px-4 py-4 border-b border-foreground/8">
      <p className="text-[10px] font-semibold tracking-widest uppercase text-foreground/30 mb-1">
        AI 모의 면접
      </p>
      {!isGreeting && questions.length > 0 && (
        <p className="text-xs text-foreground/50 font-tabular">
          {currentQuestionIndex + 1} / {questions.length}
        </p>
      )}
    </div>

    {/* 타이머 */}
    <div className="px-4 py-3 border-b border-foreground/8">
      <p className="text-[10px] font-semibold tracking-widest uppercase text-foreground/30 mb-1">
        {durationMinutes ? '남은 시간' : '경과 시간'}
      </p>
      <InterviewTimer
        startTime={startTime}
        durationMinutes={durationMinutes}
        onTimeWarning={onTimeWarning}
        onTimeExpired={onTimeExpired}
      />
    </div>

    {/* 질문 디스플레이 */}
    <div className="flex-1 px-4 py-4 overflow-y-auto">
      <p className="text-[10px] font-semibold tracking-widest uppercase text-foreground/30 mb-2">
        현재 질문
      </p>
      <QuestionDisplay
        isGreeting={isGreeting}
        currentQuestion={currentQuestion}
        currentFollowUp={currentFollowUp}
        isFollowUpLoading={isFollowUpLoading}
        followUpRound={followUpRound}
        timeWarning={timeWarning}
        autoTransitionMessage={autoTransitionMessage}
      />
    </div>

    {/* 하단 컨트롤 — 항상 visible */}
    <div className="px-4 py-4 border-t border-foreground/8 flex flex-col gap-2">
      <InterviewControls
        phase={phase}
        isTtsSpeaking={isTtsSpeaking}
        isFollowUpLoading={isFollowUpLoading}
        onStartAnswer={onStartAnswer}
        onStopAnswer={onStopAnswer}
        onFinishInterview={onFinishInterview}
      />
      {/* 종료 버튼 — 항상 visible, 44px 이상 hitbox */}
      {phase !== 'finishing' && phase !== 'completed' && (
        <button
          onClick={onRequestFinish}
          className="cursor-pointer w-full h-11 min-w-[44px] rounded-full border border-foreground/20 text-sm font-medium text-foreground/50 transition-colors duration-[var(--duration-fast)] hover:border-signal-record/50 hover:text-signal-record active:scale-95"
        >
          면접 종료
        </button>
      )}
    </div>
  </aside>
)

// ─── 하단 Bar (모바일 < md) ──────────────────────────────────────────────────

interface MobileBarProps {
  phase: InterviewPhase
  isTtsSpeaking: boolean
  isFollowUpLoading: boolean
  isGreeting: boolean
  currentQuestion: { content: string } | undefined
  currentFollowUp: { question: string; type: string } | null
  startTime: number | null
  durationMinutes: number | null | undefined
  onTimeWarning: () => void
  onTimeExpired: () => void
  onStartAnswer: () => void
  onStopAnswer: () => void
  onFinishInterview: () => void
  onRequestFinish: () => void
}

const MobileBar = ({
  phase,
  isTtsSpeaking,
  isFollowUpLoading,
  isGreeting,
  currentQuestion,
  currentFollowUp,
  startTime,
  durationMinutes,
  onTimeWarning,
  onTimeExpired,
  onStartAnswer,
  onStopAnswer,
  onFinishInterview,
  onRequestFinish,
}: MobileBarProps) => (
  <div
    className="md:hidden flex items-center justify-between px-3 border-t border-foreground/8 bg-interview-stage"
    style={{ height: 56, minHeight: 56 }}
    aria-label="면접 컨트롤"
  >
    {/* 좌: 타이머 또는 질문 축약 */}
    <div className="flex items-center gap-2 min-w-0 flex-1">
      <InterviewTimer
        startTime={startTime}
        durationMinutes={durationMinutes}
        onTimeWarning={onTimeWarning}
        onTimeExpired={onTimeExpired}
      />
      <span className="text-foreground/20 text-xs">|</span>
      <span className="text-xs text-foreground/40 truncate max-w-[120px]">
        {currentFollowUp
          ? '후속 질문'
          : isGreeting
            ? '자기소개'
            : currentQuestion?.content}
      </span>
    </div>

    {/* 우: 컨트롤 + 종료 */}
    <div className="flex items-center gap-2 shrink-0">
      <InterviewControls
        phase={phase}
        isTtsSpeaking={isTtsSpeaking}
        isFollowUpLoading={isFollowUpLoading}
        onStartAnswer={onStartAnswer}
        onStopAnswer={onStopAnswer}
        onFinishInterview={onFinishInterview}
      />
      {phase !== 'finishing' && phase !== 'completed' && (
        <button
          onClick={onRequestFinish}
          className="cursor-pointer h-11 min-w-[44px] px-3 rounded-full border border-foreground/20 text-xs font-medium text-foreground/50 transition-colors duration-[var(--duration-fast)] hover:border-signal-record/50 hover:text-signal-record active:scale-95"
        >
          종료
        </button>
      )}
    </div>
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
          className="cursor-pointer h-11 min-w-[44px] px-4 rounded-full bg-primary text-sm font-medium text-primary-foreground transition-colors duration-[var(--duration-fast)] hover:bg-primary/90 active:scale-95"
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
  const followUpRound = useInterviewStore((s) => s.followUpRound)

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
    <div className="flex h-screen flex-col bg-interview-stage text-foreground overflow-hidden">
      <Helmet>
        <title>면접 진행 중 - 리허설</title>
        <meta name="robots" content="noindex, nofollow" />
      </Helmet>

      {/* ── 메인 레이아웃: 비디오 full-bleed + 우측 rail ── */}
      <div className="flex flex-1 overflow-hidden">

        {/* ── 비디오 영역 (full-bleed) ── */}
        <main className="relative flex-1 overflow-hidden">

          {/* AI 면접관 타일 — 완전 full-bleed, ring/glow 없음 */}
          <div className="absolute inset-0 flex items-center justify-center bg-interview-stage">
            <InterviewerAvatar mood={avatarMood} size={200} />
          </div>

          {/* 상단 좌측: REC 라벨 고정 */}
          <div className="absolute top-3 left-3 z-10">
            <RecLabel visible={phase === 'recording'} />
          </div>

          {/* 상단 좌측: AI 면접관 명패 */}
          <div className="absolute bottom-3 left-3 z-10 flex items-center gap-2 px-2 py-1 bg-foreground/6 backdrop-blur-sm rounded">
            {isTtsSpeaking && (
              <svg className="w-3 h-3 text-foreground/60" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                <path d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3z" />
                <path d="M17 11c0 2.76-2.24 5-5 5s-5-2.24-5-5H5c0 3.53 2.61 6.43 6 6.92V21h2v-3.08c3.39-.49 6-3.39 6-6.92h-2z" />
              </svg>
            )}
            <span className="text-[11px] font-medium text-foreground/60">AI 면접관</span>
          </div>

          {/* 사용자 비디오 PIP (우하단) */}
          <div className="absolute bottom-4 right-4 w-[240px] md:w-[280px] aspect-video rounded overflow-hidden shadow-lg z-20 ring-1 ring-foreground/10">
            <VideoPreview stream={mediaStream.stream} />
            <div className="absolute bottom-1.5 left-2 flex items-center gap-1 z-10">
              {phase === 'recording' && (
                <svg className="w-2.5 h-2.5 text-signal-record" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                  <path d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3z" />
                  <path d="M17 11c0 2.76-2.24 5-5 5s-5-2.24-5-5H5c0 3.53 2.61 6.43 6 6.92V21h2v-3.08c3.39-.49 6-3.39 6-6.92h-2z" />
                </svg>
              )}
              <span className="text-[10px] font-medium text-foreground/50">나</span>
            </div>
          </div>

          {/* 모바일 전용: 질문 caption (비디오 하단) */}
          <div className="md:hidden absolute bottom-16 left-0 right-0 px-3 z-10">
            {!currentFollowUp && !isFollowUpLoading && (
              <div className="bg-foreground/6 backdrop-blur-sm rounded px-3 py-2">
                <p className="text-xs font-medium text-foreground/70 line-clamp-2 text-center">
                  {isGreeting ? '간단하게 자기소개를 해주세요' : currentQuestion?.content}
                </p>
              </div>
            )}
            {currentFollowUp && !isFollowUpLoading && (
              <div className="bg-foreground/6 backdrop-blur-sm rounded px-3 py-2">
                <p className="text-xs font-medium text-foreground/70 line-clamp-2 text-center">
                  {currentFollowUp.question}
                </p>
              </div>
            )}
          </div>
        </main>

        {/* ── 우측 Rail (md+) ── */}
        <RightRail
          isGreeting={isGreeting}
          currentQuestion={currentQuestion}
          currentFollowUp={currentFollowUp}
          isFollowUpLoading={isFollowUpLoading}
          followUpRound={followUpRound}
          timeWarning={timeWarning}
          autoTransitionMessage={autoTransitionMessage}
          phase={phase}
          startTime={startTime}
          durationMinutes={interview.durationMinutes}
          questions={questions}
          currentQuestionIndex={currentQuestionIndex}
          isTtsSpeaking={isTtsSpeaking}
          onTimeWarning={() => setTimeWarning(true)}
          onTimeExpired={handleTimeExpired}
          onStartAnswer={handleStartAnswer}
          onStopAnswer={handleStopAnswer}
          onFinishInterview={handleFinishInterview}
          onRequestFinish={() => setShowFinishDialog(true)}
        />
      </div>

      {/* ── 하단 Bar (모바일 < md, 56px 고정) ── */}
      <MobileBar
        phase={phase}
        isTtsSpeaking={isTtsSpeaking}
        isFollowUpLoading={isFollowUpLoading}
        isGreeting={isGreeting}
        currentQuestion={currentQuestion}
        currentFollowUp={currentFollowUp}
        startTime={startTime}
        durationMinutes={interview.durationMinutes}
        onTimeWarning={() => setTimeWarning(true)}
        onTimeExpired={handleTimeExpired}
        onStartAnswer={handleStartAnswer}
        onStopAnswer={handleStopAnswer}
        onFinishInterview={handleFinishInterview}
        onRequestFinish={() => setShowFinishDialog(true)}
      />

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
