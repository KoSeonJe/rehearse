import { useCallback, useMemo, useRef, type ReactNode } from 'react'
import { Helmet } from 'react-helmet-async'
import { useNavigate, useParams } from 'react-router-dom'
import { useQueries, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '@/lib/api-client'
import { Button } from '@/components/ui/button'
import { useInterviewByPublicId } from '@/hooks/use-interviews'
import { useQuestionSetFeedback, useQuestionsWithAnswers } from '@/hooks/use-question-sets'
import { useFeedbackSync } from '@/hooks/use-feedback-sync'
import { useBookmarkExistsForInterview } from '@/hooks/use-review-bookmarks'
import { type VideoPlayerHandle } from '@/components/feedback/video-player'
import { FeedbackPanel } from '@/components/feedback/feedback-panel'
import { VideoDock } from '@/components/feedback/video-dock'
import { FeedbackOnboardingCallout } from '@/components/feedback/feedback-onboarding-callout'
import { Character } from '@/components/ui/character'
import { UtilityBar } from '@/components/layout/utility-bar'
import { PageGrid } from '@/components/layout/page-grid'
import { ChapterMarker } from '@/components/layout/chapter-marker'
import { StickyOutline, type OutlineItem } from '@/components/layout/sticky-outline'
import { POSITION_LABELS, INTERVIEW_TYPE_LABELS } from '@/constants/interview-labels'
import type {
  AnalysisStatus,
  ApiResponse,
  InterviewSession,
  InterviewType,
  QuestionSetFeedbackResponse,
  TimestampFeedback,
  QuestionWithAnswer,
} from '@/types/interview'

// ---------------------------------------------------------------------------
// Info band (thin, editorial — replaces old InterviewInfoBar card)
// ---------------------------------------------------------------------------
interface InfoBandProps {
  interview: InterviewSession
}

const InfoBand = ({ interview }: InfoBandProps) => {
  const positionLabel = POSITION_LABELS[interview.position]?.label ?? interview.position
  const createdDate = new Date(interview.createdAt).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })

  return (
    <div className="border-b border-foreground/8 px-4 md:px-8 lg:px-12 py-2">
      <div className="mx-auto flex max-w-canvas flex-wrap items-center gap-x-4 gap-y-1">
        <span className="font-tabular text-[13px] font-semibold text-foreground">
          {positionLabel}
        </span>
        {interview.positionDetail && (
          <>
            <span className="text-muted-foreground/40" aria-hidden="true">·</span>
            <span className="text-[13px] text-muted-foreground">{interview.positionDetail}</span>
          </>
        )}
        {interview.interviewTypes.length > 0 && (
          <>
            <span className="text-muted-foreground/40" aria-hidden="true">·</span>
            <span className="text-[13px] text-muted-foreground">
              {interview.interviewTypes
                .map((t) => INTERVIEW_TYPE_LABELS[t]?.label ?? t)
                .join(' / ')}
            </span>
          </>
        )}
        <span className="text-muted-foreground/40" aria-hidden="true">·</span>
        <span className="font-tabular text-[13px] text-muted-foreground">
          {interview.durationMinutes}분
        </span>
        <span className="text-muted-foreground/40" aria-hidden="true">·</span>
        <span className="font-tabular text-[13px] text-muted-foreground">{createdDate}</span>
      </div>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Failure messages
// ---------------------------------------------------------------------------
const failureMessages: Record<string, string> = {
  TIMEOUT: '분석 시간이 초과되었습니다. 다시 시도해주세요.',
  API_ERROR: '외부 서비스 연결에 실패했습니다.',
  TRANSCRIPTION_ERROR: '음성 인식 처리 중 오류가 발생했습니다.',
  VISION_ERROR: '영상 분석 중 오류가 발생했습니다.',
  ZOMBIE_TIMEOUT: '분석이 시간 내 완료되지 않았습니다.',
  INTERNAL_ERROR: '분석 중 오류가 발생했습니다.',
}

// ---------------------------------------------------------------------------
// Outline item builder (extracted from QuestionList logic)
// ---------------------------------------------------------------------------
interface PlayableQuestion extends QuestionWithAnswer {
  startMs: number
  endMs: number
}

const isPlayable = (q: QuestionWithAnswer): q is PlayableQuestion =>
  q.startMs !== null && q.endMs !== null

function buildOutlineItems(
  questions: QuestionWithAnswer[],
  feedbacks: TimestampFeedback[],
): OutlineItem[] {
  const playable = questions.filter(isPlayable)
  const sorted = [...playable].sort((a, b) => a.startMs - b.startMs)

  const labeled = sorted.reduce<{
    items: Array<{ q: PlayableQuestion; label: string }>
    mainCounter: number
    followupCounter: number
  }>(
    (acc, q) => {
      const isFollowup = q.questionType === 'FOLLOWUP'
      if (!isFollowup) {
        const nextMain = acc.mainCounter + 1
        return {
          items: [...acc.items, { q, label: `Q${nextMain}` }],
          mainCounter: nextMain,
          followupCounter: 0,
        }
      }
      const nextFollowup = acc.followupCounter + 1
      const parentMain = acc.mainCounter || 1
      return {
        items: [...acc.items, { q, label: `Q${parentMain}-${nextFollowup}` }],
        mainCounter: acc.mainCounter,
        followupCounter: nextFollowup,
      }
    },
    { items: [], mainCounter: 0, followupCounter: 0 },
  ).items

  return labeled.map(({ q, label }, idx) => {
    const fb = feedbacks.find((f) => f.startMs >= q.startMs && f.startMs < q.endMs)
    return {
      id: fb ? `feedback-${fb.id}` : `q-${q.questionId}`,
      label,
      index: idx + 1,
      hasIssue: false,
    }
  })
}

// ---------------------------------------------------------------------------
// QuestionSetSection
// ---------------------------------------------------------------------------
interface QuestionSetSectionProps {
  interviewId: number
  questionSetId: number
  category: string
  index: number
  analysisStatus: AnalysisStatus
  failureReason?: string | null
  bookmarkIdsByTsfId: Map<number, number>
  isFirstSection: boolean
}

const QuestionSetSection = ({
  interviewId,
  questionSetId,
  category,
  index,
  analysisStatus,
  failureReason,
  bookmarkIdsByTsfId,
  isFirstSection,
}: QuestionSetSectionProps) => {
  const videoRef = useRef<VideoPlayerHandle>(null)
  const queryClient = useQueryClient()

  const shouldFetchFeedback = analysisStatus === 'COMPLETED' || analysisStatus === 'PARTIAL'
  const { data: feedbackRes, isLoading: feedbackLoading } = useQuestionSetFeedback(
    interviewId,
    questionSetId,
    shouldFetchFeedback,
  )
  const { data: questionsRes } = useQuestionsWithAnswers(
    interviewId,
    questionSetId,
    shouldFetchFeedback,
  )

  const feedback = feedbackRes?.data
  const feedbacks = feedback?.timestampFeedbacks ?? []
  const questions = questionsRes?.data?.questions ?? []

  const { activeFeedbackId, selectedFeedbackId, currentTimeMs, videoDurationMs, seekTo } =
    useFeedbackSync(videoRef, feedbacks, questions)

  const handleUrlExpired = useCallback(() => {
    void queryClient.invalidateQueries({
      queryKey: ['questionSetFeedback', interviewId, questionSetId],
    })
  }, [queryClient, interviewId, questionSetId])

  const feedbackMaxMs = feedbacks.length > 0 ? Math.max(...feedbacks.map((f) => f.endMs)) : 0
  const durationMs =
    videoDurationMs > 0 ? Math.max(videoDurationMs, feedbackMaxMs) : feedbackMaxMs

  const categoryLabel = INTERVIEW_TYPE_LABELS[category as InterviewType]?.label ?? category

  // ── FAILED ──────────────────────────────────────────────────────────────
  if (analysisStatus === 'FAILED') {
    return (
      <section className="col-span-4 md:col-span-8 lg:col-span-12 space-y-6">
        <ChapterMarker index={index + 1} title={categoryLabel} label="분석 실패" />
        <div className="border border-destructive/20 bg-destructive/5 p-6">
          <p className="text-sm font-semibold text-destructive mb-2">
            이 질문세트의 분석에 실패했습니다
          </p>
          <p className="text-[13px] text-muted-foreground">
            {failureReason
              ? (failureMessages[failureReason] ?? failureReason)
              : '알 수 없는 오류가 발생했습니다.'}
          </p>
        </div>
      </section>
    )
  }

  // ── 미완료 상태 ──────────────────────────────────────────────────────────
  if (analysisStatus !== 'COMPLETED' && analysisStatus !== 'PARTIAL') {
    const analysisInProgressBody = (
      <div className="border border-foreground/8 p-8 text-center space-y-4">
        <Character mood="thinking" size={80} className="mx-auto" />
        <div>
          <p className="text-sm font-semibold text-foreground">분석이 진행 중이에요</p>
          <p className="text-[13px] text-muted-foreground mt-1">
            영상을 분석하고 피드백을 생성하고 있습니다
          </p>
        </div>
        <div className="h-1 w-32 bg-primary/20 rounded-full mx-auto overflow-hidden">
          <div className="h-full bg-primary animate-progress-loading" />
        </div>
      </div>
    )

    const statusConfig: Record<string, { subtitle: string; body: ReactNode }> = {
      PENDING: {
        subtitle: '업로드 대기 중',
        body: (
          <div className="border border-foreground/8 p-8 text-center space-y-3">
            <p className="text-sm font-semibold text-muted-foreground">
              영상 업로드를 기다리고 있어요
            </p>
            <p className="text-[13px] text-muted-foreground">
              면접 영상이 업로드되면 자동으로 분석이 시작됩니다
            </p>
          </div>
        ),
      },
      PENDING_UPLOAD: {
        subtitle: '업로드 대기 중',
        body: (
          <div className="border border-foreground/8 p-8 text-center space-y-3">
            <p className="text-sm font-semibold text-muted-foreground">
              영상 업로드를 기다리고 있어요
            </p>
            <p className="text-[13px] text-muted-foreground">
              면접 영상이 업로드되면 자동으로 분석이 시작됩니다
            </p>
          </div>
        ),
      },
      EXTRACTING: { subtitle: '영상 처리 중', body: analysisInProgressBody },
      ANALYZING: { subtitle: '분석 진행 중', body: analysisInProgressBody },
      FINALIZING: { subtitle: '피드백 생성 중', body: analysisInProgressBody },
      SKIPPED: {
        subtitle: '건너뜀',
        body: (
          <div className="border border-foreground/8 p-8 text-center">
            <p className="text-[13px] text-muted-foreground">이 질문세트는 건너뛰었습니다</p>
          </div>
        ),
      },
    }

    const config = statusConfig[analysisStatus] ?? {
      subtitle: '대기 중',
      body: (
        <div className="border border-foreground/8 p-8 text-center animate-pulse">
          <p className="text-[13px] text-muted-foreground">분석이 아직 완료되지 않았습니다</p>
        </div>
      ),
    }

    return (
      <section className="col-span-4 md:col-span-8 lg:col-span-12 space-y-6">
        <ChapterMarker index={index + 1} title={categoryLabel} label={config.subtitle} />
        {config.body}
      </section>
    )
  }

  // ── 로딩 / 데이터 없음 ───────────────────────────────────────────────────
  if (feedbackLoading) {
    return (
      <div className="col-span-4 md:col-span-8 lg:col-span-12 border border-foreground/8 p-8 animate-pulse">
        <div className="h-6 w-48 bg-muted rounded mb-4" />
        <div className="h-40 bg-muted/60 rounded" />
      </div>
    )
  }

  if (!feedback) {
    return (
      <div className="col-span-4 md:col-span-8 lg:col-span-12 border border-foreground/8 p-8 text-center">
        <p className="text-[13px] text-muted-foreground">피드백을 불러올 수 없습니다</p>
      </div>
    )
  }

  // ── COMPLETED / PARTIAL ─────────────────────────────────────────────────
  // OutlineItem id는 `feedback-{feedbackId}` or `q-{questionId}`
  const outlineItems = buildOutlineItems(questions, feedbacks)
  const activeOutlineId = selectedFeedbackId !== null ? `feedback-${selectedFeedbackId}` : ''

  // onSelect: outlineId → startMs → seekTo
  const handleOutlineSelect = (id: string) => {
    const fbIdStr = id.replace('feedback-', '')
    const fb = feedbacks.find((f) => String(f.id) === fbIdStr)
    if (fb) seekTo(fb.startMs)
  }

  return (
    <section className="col-span-4 md:col-span-8 lg:col-span-12">
      {/* Chapter header — full width */}
      <ChapterMarker
        index={index + 1}
        title={categoryLabel}
        label={`${feedbacks.length}개 구간`}
      />

      {/* Onboarding callout — first section only */}
      {isFirstSection && (
        <div className="mb-8">
          <FeedbackOnboardingCallout />
        </div>
      )}

      {/* Outline nav — all 3 variants always mounted, CSS controls visibility */}
      <StickyOutline.TabBar
        items={outlineItems}
        activeId={activeOutlineId}
        onSelect={handleOutlineSelect}
      />

      {/* 3-pane editorial grid (spec §4.3):
          xl: 2 (outline) + 6 (reading) + 4 (video) = 12
          lg:             8 (reading) + 4 (video)
          md/sm:         single column; mobile outline via Sheet */}
      <div className="grid grid-cols-4 gap-x-4 md:grid-cols-8 md:gap-x-5 lg:grid-cols-12 lg:gap-x-6 mt-6">
        {/* Left rail — outline (xl only; CSS hides below xl) */}
        <StickyOutline.Desktop
          items={outlineItems}
          activeId={activeOutlineId}
          onSelect={handleOutlineSelect}
        />

        {/* Middle — reading column */}
        <div className="col-span-4 md:col-span-8 lg:col-span-8 xl:col-span-6 space-y-8">
          {feedback.questionSetComment && (
            <blockquote className="border-l-2 border-accent-editorial/50 pl-4 text-[1.0625rem]/[1.65] text-muted-foreground not-italic">
              {feedback.questionSetComment}
            </blockquote>
          )}

          <FeedbackPanel
            feedbacks={feedbacks}
            questions={questions}
            selectedFeedbackId={selectedFeedbackId}
            onSeek={seekTo}
            interviewId={interviewId}
            bookmarkIdsByTsfId={bookmarkIdsByTsfId}
          />
        </div>

        {/* Right rail — video dock (lg+ col-4) */}
        <VideoDock
          streamingUrl={feedback.streamingUrl}
          fallbackUrl={feedback.fallbackUrl}
          feedbacks={feedbacks}
          durationMs={durationMs}
          currentTimeMs={currentTimeMs}
          activeFeedbackId={activeFeedbackId}
          onSeek={seekTo}
          onUrlExpired={handleUrlExpired}
          videoRef={videoRef}
        />

        {/* Mobile outline — fixed-position Sheet, outside grid flow */}
        <StickyOutline.MobileSheet
          items={outlineItems}
          activeId={activeOutlineId}
          onSelect={handleOutlineSelect}
        />
      </div>
    </section>
  )
}

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------
export const InterviewFeedbackPage = () => {
  const { publicId } = useParams<{ publicId: string }>()
  const navigate = useNavigate()
  const { data: response, isLoading } = useInterviewByPublicId(publicId ?? '')
  const interview = response?.data
  const questionSets = interview?.questionSets ?? []

  const completedQs = questionSets.filter(
    (qs) => qs.analysisStatus === 'COMPLETED' || qs.analysisStatus === 'PARTIAL',
  )
  const feedbackResults = useQueries({
    queries: completedQs.map((qs) => ({
      queryKey: ['questionSetFeedback', interview?.id ?? 0, qs.id],
      queryFn: () =>
        apiClient.get<ApiResponse<QuestionSetFeedbackResponse>>(
          `/api/v1/interviews/${interview?.id ?? 0}/question-sets/${qs.id}/feedback`,
        ),
      enabled: !!interview && (qs.analysisStatus === 'COMPLETED' || qs.analysisStatus === 'PARTIAL'),
      staleTime: Infinity,
    })),
  })

  const allTsfIds = useMemo<number[]>(() => {
    return feedbackResults.flatMap((r) => {
      const tsfs = r.data?.data?.timestampFeedbacks
      if (!tsfs) return []
      return tsfs.map((tsf) => tsf.id)
    })
  }, [feedbackResults])

  const { bookmarkIdMap } = useBookmarkExistsForInterview(interview?.id ?? 0, allTsfIds)

  // ── 로딩 ─────────────────────────────────────────────────────────────────
  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <div className="text-center space-y-4">
          <Character mood="thinking" size={120} className="mx-auto" />
          <div className="h-1 w-24 bg-primary/20 rounded-full mx-auto overflow-hidden">
            <div className="h-full bg-primary animate-progress-loading" />
          </div>
          <p className="font-tabular text-[11px] font-semibold uppercase tracking-widest text-muted-foreground">
            피드백 로딩 중
          </p>
        </div>
      </div>
    )
  }

  // ── 데이터 없음 ──────────────────────────────────────────────────────────
  if (!interview || questionSets.length === 0) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center bg-background px-5">
        <Character mood="confused" size={140} className="mb-8" />
        <h1 className="text-2xl font-bold tracking-tight text-foreground text-center">
          피드백을 불러올 수 없습니다
        </h1>
        <Button
          variant="default"
          size="lg"
          onClick={() => navigate('/')}
          className="mt-10 w-full max-w-xs"
        >
          홈으로 돌아가기
        </Button>
      </div>
    )
  }

  const positionLabel = POSITION_LABELS[interview.position]?.label ?? interview.position

  // ── 대시보드 액션 버튼 ────────────────────────────────────────────────────
  const dashboardAction = (
    <button
      type="button"
      onClick={() => navigate('/dashboard', { replace: true })}
      aria-label="대시보드로 이동"
      className="flex h-11 w-11 items-center justify-center rounded-sm text-muted-foreground hover:text-foreground transition-colors duration-[var(--duration-fast)]"
    >
      <svg
        width="16"
        height="16"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.75"
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden="true"
      >
        <path d="M19 12H5M12 5l-7 7 7 7" />
      </svg>
    </button>
  )

  return (
    <div className="min-h-screen bg-background pb-32">
      <Helmet>
        <title>면접 피드백 — {positionLabel} · 리허설</title>
        <meta name="robots" content="noindex, nofollow" />
      </Helmet>

      {/* Utility bar */}
      <UtilityBar chapter="FEEDBACK" actions={dashboardAction} />

      {/* Info band — thin, tabular, no card chrome */}
      <InfoBand interview={interview} />

      {/* Page title header */}
      <PageGrid as="div" className="mt-10 mb-2">
        <div className="col-span-4 md:col-span-8 lg:col-span-12">
          <p className="font-tabular text-[11px] font-semibold uppercase tracking-[0.1em] text-accent-editorial mb-2">
            Timestamp Feedback Review
          </p>
          <h1 className="text-[2rem] md:text-[2.5rem] font-bold leading-[1.10] tracking-[-0.02em] text-foreground">
            {positionLabel} 면접 피드백
          </h1>
        </div>
      </PageGrid>

      {/* Question set sections */}
      <PageGrid as="main" className="mt-4 gap-y-20">
        {questionSets
          .filter((qs) => qs.analysisStatus !== 'SKIPPED')
          .map((qs, idx) => (
            <QuestionSetSection
              key={qs.id}
              interviewId={interview.id}
              questionSetId={qs.id}
              category={qs.category}
              index={idx}
              analysisStatus={qs.analysisStatus}
              failureReason={qs.failureReason}
              bookmarkIdsByTsfId={bookmarkIdMap}
              isFirstSection={idx === 0}
            />
          ))}
      </PageGrid>
    </div>
  )
}
