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
import { VideoPlayer, type VideoPlayerHandle } from '@/components/feedback/video-player'
import { TimelineBar } from '@/components/feedback/timeline-bar'
import { QuestionList } from '@/components/feedback/question-list'
import { FeedbackPanel } from '@/components/feedback/feedback-panel'
import {
  ReviewTutorialProvider,
  ReviewTutorialStack,
} from '@/components/feedback/review-coach-mark'
import { Logo } from '@/components/ui/logo'
import { Character } from '@/components/ui/character'
import { POSITION_LABELS, INTERVIEW_TYPE_LABELS } from '@/constants/interview-labels'
import type { AnalysisStatus, ApiResponse, InterviewSession, InterviewType, QuestionSetFeedbackResponse } from '@/types/interview'

interface InterviewInfoBarProps {
  interview: InterviewSession
}

const InterviewInfoBar = ({ interview }: InterviewInfoBarProps) => {
  const positionLabel = POSITION_LABELS[interview.position]?.label ?? interview.position
  const createdDate = new Date(interview.createdAt).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })

  return (
    <div className="border-b border-border bg-surface px-5 py-3">
      <div className="mx-auto flex max-w-6xl flex-wrap items-center gap-x-4 gap-y-1 md:px-8">
        <span className="text-sm font-bold text-text-primary">{positionLabel}</span>
        {interview.positionDetail && (
          <>
            <span className="text-text-tertiary">·</span>
            <span className="text-sm font-medium text-text-secondary">{interview.positionDetail}</span>
          </>
        )}
        {interview.interviewTypes.length > 0 && (
          <>
            <span className="text-text-tertiary">·</span>
            <div className="flex flex-wrap gap-1">
              {interview.interviewTypes.map((type) => (
                <span
                  key={type}
                  className="rounded-full bg-violet-legacy/10 px-2 py-0.5 text-xs font-semibold text-violet-legacy"
                >
                  {INTERVIEW_TYPE_LABELS[type]?.label ?? type}
                </span>
              ))}
            </div>
          </>
        )}
        <span className="text-text-tertiary">·</span>
        <span className="text-xs font-medium text-text-tertiary">{interview.durationMinutes}분</span>
        <span className="text-text-tertiary">·</span>
        <span className="text-xs font-medium text-text-tertiary">{createdDate}</span>
      </div>
    </div>
  )
}

const failureMessages: Record<string, string> = {
  TIMEOUT: '분석 시간이 초과되었습니다. 다시 시도해주세요.',
  API_ERROR: '외부 서비스 연결에 실패했습니다.',
  TRANSCRIPTION_ERROR: '음성 인식 처리 중 오류가 발생했습니다.',
  VISION_ERROR: '영상 분석 중 오류가 발생했습니다.',
  ZOMBIE_TIMEOUT: '분석이 시간 내 완료되지 않았습니다.',
  INTERNAL_ERROR: '분석 중 오류가 발생했습니다.',
}

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

const QuestionSetSection = ({ interviewId, questionSetId, category, index, analysisStatus, failureReason, bookmarkIdsByTsfId, isFirstSection }: QuestionSetSectionProps) => {
  const videoRef = useRef<VideoPlayerHandle>(null)
  const queryClient = useQueryClient()

  const shouldFetchFeedback = analysisStatus === 'COMPLETED' || analysisStatus === 'PARTIAL'
  const { data: feedbackRes, isLoading: feedbackLoading } = useQuestionSetFeedback(
    interviewId, questionSetId, shouldFetchFeedback,
  )
  const { data: questionsRes } = useQuestionsWithAnswers(interviewId, questionSetId, shouldFetchFeedback)

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

  // Use actual video duration as timeline base; fall back to feedback endMs
  const feedbackMaxMs = feedbacks.length > 0
    ? Math.max(...feedbacks.map((f) => f.endMs))
    : 0
  const durationMs = videoDurationMs > 0
    ? Math.max(videoDurationMs, feedbackMaxMs)
    : feedbackMaxMs

  // FAILED 상태 UI
  if (analysisStatus === 'FAILED') {
    return (
      <section className="space-y-6">
        <div className="flex items-center gap-3">
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-error/10 text-xs font-bold text-error">
            {index + 1}
          </div>
          <div>
            <h2 className="text-lg font-bold tracking-tight text-text-primary">{INTERVIEW_TYPE_LABELS[category as InterviewType]?.label ?? category}</h2>
            <p className="text-xs text-error font-semibold">분석 실패</p>
          </div>
        </div>
        <div className="rounded-2xl border border-error/20 bg-error/5 p-6 text-center">
          <p className="text-sm font-bold text-error mb-2">이 질문세트의 분석에 실패했습니다</p>
          <p className="text-xs text-text-tertiary">
            {failureReason ? (failureMessages[failureReason] ?? failureReason) : '알 수 없는 오류가 발생했습니다.'}
          </p>
        </div>
      </section>
    )
  }

  // 분석 미완료 상태
  if (analysisStatus !== 'COMPLETED' && analysisStatus !== 'PARTIAL') {
    const analysisInProgressBody = (
      <div className="rounded-2xl bg-surface p-8 text-center space-y-4">
        <Character mood="thinking" size={80} className="mx-auto" />
        <div>
          <p className="text-sm font-semibold text-text-primary">분석이 진행 중이에요</p>
          <p className="text-xs text-text-tertiary mt-1">영상을 분석하고 피드백을 생성하고 있습니다</p>
        </div>
        <div className="h-1 w-32 bg-violet-legacy/20 rounded-full mx-auto overflow-hidden">
          <div className="h-full bg-violet-legacy animate-progress-loading" />
        </div>
      </div>
    )

    const statusConfig: Record<string, { subtitle: string; body: ReactNode }> = {
      PENDING: {
        subtitle: '업로드 대기 중',
        body: (
          <div className="rounded-2xl bg-surface p-8 text-center space-y-3">
            <p className="text-sm font-semibold text-text-secondary">영상 업로드를 기다리고 있어요</p>
            <p className="text-xs text-text-tertiary">면접 영상이 업로드되면 자동으로 분석이 시작됩니다</p>
          </div>
        ),
      },
      PENDING_UPLOAD: {
        subtitle: '업로드 대기 중',
        body: (
          <div className="rounded-2xl bg-surface p-8 text-center space-y-3">
            <p className="text-sm font-semibold text-text-secondary">영상 업로드를 기다리고 있어요</p>
            <p className="text-xs text-text-tertiary">면접 영상이 업로드되면 자동으로 분석이 시작됩니다</p>
          </div>
        ),
      },
      EXTRACTING: {
        subtitle: '영상 처리 중',
        body: analysisInProgressBody,
      },
      ANALYZING: {
        subtitle: '분석 진행 중',
        body: analysisInProgressBody,
      },
      FINALIZING: {
        subtitle: '피드백 생성 중',
        body: analysisInProgressBody,
      },
      SKIPPED: {
        subtitle: '건너뜀',
        body: (
          <div className="rounded-2xl bg-surface p-8 text-center">
            <p className="text-sm font-semibold text-text-tertiary">이 질문세트는 건너뛰었습니다</p>
          </div>
        ),
      },
    }

    const config = statusConfig[analysisStatus] ?? {
      subtitle: '대기 중',
      body: (
        <div className="rounded-2xl bg-surface p-8 text-center animate-pulse">
          <p className="text-sm font-semibold text-text-tertiary">분석이 아직 완료되지 않았습니다</p>
        </div>
      ),
    }

    return (
      <section className="space-y-6">
        <div className="flex items-center gap-3">
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-border text-xs font-bold text-text-tertiary">
            {index + 1}
          </div>
          <div>
            <h2 className="text-lg font-bold tracking-tight text-text-primary">{INTERVIEW_TYPE_LABELS[category as InterviewType]?.label ?? category}</h2>
            <p className="text-xs text-text-tertiary font-medium">{config.subtitle}</p>
          </div>
        </div>
        {config.body}
      </section>
    )
  }

  if (feedbackLoading) {
    return (
      <div className="rounded-[32px] bg-surface p-8 animate-pulse">
        <div className="h-6 w-48 bg-border/50 rounded-lg mb-4" />
        <div className="h-40 bg-border/30 rounded-2xl" />
      </div>
    )
  }

  if (!feedback) {
    return (
      <div className="rounded-[32px] bg-surface border border-border p-8 text-center">
        <p className="text-sm font-bold text-text-tertiary">피드백을 불러올 수 없습니다</p>
      </div>
    )
  }

  const shouldTriggerTutorial =
    isFirstSection && feedbacks.length > 0 && selectedFeedbackId === feedbacks[0].id

  return (
    <ReviewTutorialProvider shouldTrigger={shouldTriggerTutorial}>
      <section className="space-y-6">
        {/* Header */}
        <div className="flex items-center gap-3">
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-violet-legacy text-xs font-bold text-white">
            {index + 1}
          </div>
          <div>
            <h2 className="text-lg font-bold tracking-tight text-text-primary">{INTERVIEW_TYPE_LABELS[category as InterviewType]?.label ?? category}</h2>
            <p className="text-xs text-text-tertiary">{feedbacks.length}개 구간 분석</p>
          </div>
        </div>

        {/* Comment */}
        {feedback.questionSetComment && (
          <div className="rounded-2xl bg-surface p-5">
            <p className="text-sm font-medium text-text-secondary leading-relaxed">{feedback.questionSetComment}</p>
          </div>
        )}

        {/* Content: Video (left) + Feedback (right) */}
        <div className="flex flex-col lg:flex-row gap-6">
          {/* 좌측: Video + Timeline */}
          <div className="lg:w-[60%] space-y-4 lg:sticky lg:top-20 lg:self-start">
            <VideoPlayer
              ref={videoRef}
              streamingUrl={feedback.streamingUrl}
              fallbackUrl={feedback.fallbackUrl}
              onUrlExpired={handleUrlExpired}
            />
            <TimelineBar
              feedbacks={feedbacks}
              durationMs={durationMs}
              currentTimeMs={currentTimeMs}
              activeFeedbackId={activeFeedbackId}
              onSeek={seekTo}
            />
            <div data-tutorial-anchor="question-list">
              <QuestionList
                questions={questions}
                feedbacks={feedbacks}
                selectedFeedbackId={selectedFeedbackId}
                onSeek={seekTo}
              />
            </div>
          </div>

          {/* 우측: Feedback Panel */}
          <div className="lg:w-[40%]">
            <FeedbackPanel
              feedbacks={feedbacks}
              questions={questions}
              selectedFeedbackId={selectedFeedbackId}
              onSeek={seekTo}
              interviewId={interviewId}
              bookmarkIdsByTsfId={bookmarkIdsByTsfId}
            />
          </div>
        </div>

        <ReviewTutorialStack />
      </section>
    </ReviewTutorialProvider>
  )
}

export const InterviewFeedbackPage = () => {
  const { publicId } = useParams<{ publicId: string }>()
  const navigate = useNavigate()
  const { data: response, isLoading } = useInterviewByPublicId(publicId ?? '')
  const interview = response?.data
  const questionSets = interview?.questionSets ?? []

  // Batch-fetch all feedback to collect tsfIds for the bookmark exists query.
  // Uses the same query keys as QuestionSetSection → TanStack Query deduplicates
  // network calls; sections that have already loaded hit the cache immediately.
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

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-white">
        <div className="text-center space-y-4">
          <Character mood="thinking" size={120} className="mx-auto" />
          <div className="h-1 w-24 bg-violet-legacy/20 rounded-full mx-auto overflow-hidden">
            <div className="h-full bg-violet-legacy animate-progress-loading" />
          </div>
          <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">피드백 로딩 중</p>
        </div>
      </div>
    )
  }

  if (!interview || questionSets.length === 0) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center bg-white px-5">
        <Character mood="confused" size={140} className="mb-8" />
        <h1 className="text-2xl font-extrabold tracking-tighter text-text-primary text-center">
          피드백을 불러올 수 없습니다
        </h1>
        <Button
          variant="default"
          size="lg"
          onClick={() => navigate('/')}
          className="mt-10 w-full max-w-xs rounded-[24px]"
        >
          홈으로 돌아가기
        </Button>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-white pb-32">
      <Helmet>
        <title>면접 피드백 - 리허설</title>
        <meta name="robots" content="noindex, nofollow" />
      </Helmet>
      {/* Header */}
      <header className="sticky top-0 z-[60] bg-white/80 backdrop-blur-md px-5 pt-6 pb-4 border-b border-border">
        <div className="mx-auto flex max-w-6xl items-center justify-between">
          {/* TODO(design): variant 판단 보류 — 로고+텍스트 조합 헤더 버튼, 레이아웃 영향 검토 필요 */}
          <button className="flex items-center gap-2" onClick={() => navigate('/dashboard', { replace: true })}>
            <Logo size={60} />
            <span className="text-lg font-bold tracking-tight text-text-primary">타임스탬프 리뷰</span>
          </button>
          <div className="flex items-center gap-3">
            <button
              onClick={() => navigate('/dashboard', { replace: true })}
              className="flex items-center gap-1 text-sm font-semibold text-text-secondary hover:text-text-primary"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M19 12H5M12 5l-7 7 7 7" />
              </svg>
              대시보드
            </button>
          </div>
        </div>
      </header>

      {/* 면접 정보 요약 바 */}
      <InterviewInfoBar interview={interview} />

      <main className="mx-auto max-w-6xl px-5 pt-10 md:px-8">
        {/* Hero */}
        <section className="text-center mb-12">
          <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-3">
            Timestamp Feedback Review
          </p>
          <h1 className="text-2xl font-extrabold tracking-tighter text-text-primary sm:text-3xl">
            영상을 보며 정밀 교정하기
          </h1>
          <p className="mt-2 text-base font-medium text-text-secondary">
            피드백이 생성된 정확한 시점을 확인하고, 나의 답변을 다시 체크해보세요.
          </p>
        </section>

        {/* Question Set Sections */}
        <div className="space-y-16">
          {questionSets.filter(qs => qs.analysisStatus !== 'SKIPPED').map((qs, idx) => (
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
        </div>
      </main>
    </div>
  )
}
