import { useCallback, useRef } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import { useInterview } from '@/hooks/use-interviews'
import { useQuestionSetFeedback, useQuestionsWithAnswers } from '@/hooks/use-question-sets'
import { useFeedbackSync } from '@/hooks/use-feedback-sync'
import { VideoPlayer, type VideoPlayerHandle } from '@/components/feedback/video-player'
import { TimelineBar } from '@/components/feedback/timeline-bar'
import { FeedbackPanel } from '@/components/feedback/feedback-panel'
import { Logo } from '@/components/ui/logo'
import { Character } from '@/components/ui/character'
import type { AnalysisStatus } from '@/types/interview'

interface QuestionSetSectionProps {
  interviewId: number
  questionSetId: number
  category: string
  index: number
  analysisStatus: AnalysisStatus
  failureReason?: string | null
}

const QuestionSetSection = ({ interviewId, questionSetId, category, index, analysisStatus, failureReason }: QuestionSetSectionProps) => {
  const videoRef = useRef<VideoPlayerHandle>(null)
  const queryClient = useQueryClient()

  const { data: feedbackRes, isLoading: feedbackLoading } = useQuestionSetFeedback(
    interviewId, questionSetId, analysisStatus === 'COMPLETED',
  )
  const { data: questionsRes } = useQuestionsWithAnswers(interviewId, questionSetId, analysisStatus === 'COMPLETED')

  const feedback = feedbackRes?.data
  const feedbacks = feedback?.timestampFeedbacks ?? []
  const questions = questionsRes?.data?.questions ?? []

  const { activeFeedbackId, currentTimeMs, seekTo } = useFeedbackSync(videoRef, feedbacks)

  const handleUrlExpired = useCallback(() => {
    void queryClient.invalidateQueries({
      queryKey: ['questionSetFeedback', interviewId, questionSetId],
    })
  }, [queryClient, interviewId, questionSetId])

  // Estimate total duration from last feedback endMs
  const durationMs = feedbacks.length > 0
    ? Math.max(...feedbacks.map((f) => f.endMs))
    : 0

  // FAILED 상태 UI
  if (analysisStatus === 'FAILED') {
    return (
      <section className="space-y-6">
        <div className="flex items-center gap-3">
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-error/10 text-xs font-black text-error">
            {index + 1}
          </div>
          <div>
            <h2 className="text-lg font-extrabold tracking-tight text-text-primary">{category}</h2>
            <p className="text-xs text-error font-bold">분석 실패</p>
          </div>
        </div>
        <div className="rounded-2xl border border-error/20 bg-error/5 p-6 text-center">
          <p className="text-sm font-bold text-error mb-2">이 질문세트의 분석에 실패했습니다</p>
          {failureReason && (
            <p className="text-xs text-text-tertiary">{failureReason}</p>
          )}
        </div>
      </section>
    )
  }

  // 분석 미완료 상태
  if (analysisStatus !== 'COMPLETED') {
    return (
      <section className="space-y-6">
        <div className="flex items-center gap-3">
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-border text-xs font-black text-text-tertiary">
            {index + 1}
          </div>
          <div>
            <h2 className="text-lg font-extrabold tracking-tight text-text-primary">{category}</h2>
            <p className="text-xs text-text-tertiary font-bold">분석 대기 중</p>
          </div>
        </div>
        <div className="rounded-2xl bg-surface p-8 text-center animate-pulse">
          <p className="text-sm font-bold text-text-tertiary">분석이 아직 완료되지 않았습니다</p>
        </div>
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

  return (
    <section className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-accent text-xs font-black text-white">
            {index + 1}
          </div>
          <div>
            <h2 className="text-lg font-extrabold tracking-tight text-text-primary">{category}</h2>
            <p className="text-xs text-text-tertiary">{feedbacks.length}개 구간 분석</p>
          </div>
        </div>
        <div className="text-right">
          <p className="text-3xl font-black tracking-tighter text-text-primary">{feedback.questionSetScore}</p>
          <p className="text-[10px] font-bold text-text-tertiary">/ 100</p>
        </div>
      </div>

      {/* Comment */}
      {feedback.questionSetComment && (
        <div className="rounded-2xl bg-surface p-5">
          <p className="text-sm font-medium text-text-secondary leading-relaxed">{feedback.questionSetComment}</p>
        </div>
      )}

      {/* Content: Video + Feedback */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        {/* Left: Video + Timeline */}
        <div className="space-y-4">
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
        </div>

        {/* Right: Feedback Panel */}
        <div className="lg:max-h-[500px] lg:overflow-y-auto">
          <FeedbackPanel
            feedbacks={feedbacks}
            questions={questions}
            activeFeedbackId={activeFeedbackId}
            onSeek={seekTo}
          />
        </div>
      </div>
    </section>
  )
}

export const InterviewFeedbackPage = () => {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { data: response, isLoading } = useInterview(id ?? '')
  const interview = response?.data
  const questionSets = interview?.questionSets ?? []

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-white">
        <div className="text-center space-y-4">
          <Character mood="thinking" size={120} className="mx-auto" />
          <div className="h-1 w-24 bg-accent/20 rounded-full mx-auto overflow-hidden">
            <div className="h-full bg-accent animate-progress-loading" />
          </div>
          <p className="font-mono text-[10px] font-black uppercase tracking-widest text-accent">피드백 로딩 중</p>
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
        <button
          className="mt-10 h-16 w-full max-w-xs rounded-[24px] bg-accent font-black text-white active:scale-95"
          onClick={() => navigate('/')}
        >
          홈으로 돌아가기
        </button>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-white pb-32">
      {/* Header */}
      <header className="sticky top-0 z-50 bg-white/80 backdrop-blur-md px-5 pt-6 pb-4 border-b border-border">
        <div className="mx-auto flex max-w-6xl items-center justify-between">
          <div className="flex items-center gap-2" onClick={() => navigate('/')} role="button">
            <Logo size={60} />
            <span className="text-lg font-black tracking-tight text-text-primary">타임스탬프 리뷰</span>
          </div>
          <div className="flex items-center gap-3">
            <button
              onClick={() => navigate(`/interview/${id}/report`)}
              className="text-sm font-bold text-text-secondary hover:text-text-primary"
            >
              리포트
            </button>
            <button
              onClick={() => navigate('/')}
              className="text-sm font-bold text-text-secondary hover:text-text-primary"
            >
              닫기
            </button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-6xl px-5 pt-10 md:px-8">
        {/* Hero */}
        <section className="text-center mb-12">
          <p className="font-mono text-[10px] font-black uppercase tracking-[0.2em] text-accent mb-3">
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
          {questionSets.map((qs, idx) => (
            <QuestionSetSection
              key={qs.id}
              interviewId={interview.id}
              questionSetId={qs.id}
              category={qs.category}
              index={idx}
              analysisStatus={qs.analysisStatus}
              failureReason={qs.failureReason}
            />
          ))}
        </div>
      </main>
    </div>
  )
}
