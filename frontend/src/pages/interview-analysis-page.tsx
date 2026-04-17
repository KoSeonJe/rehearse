import { useCallback, useEffect, useMemo, useState } from 'react'
import type { InterviewType } from '@/types/interview'
import { INTERVIEW_TYPE_LABELS } from '@/constants/interview-labels'
import { Helmet } from 'react-helmet-async'
import { useNavigate, useParams } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import { useInterviewByPublicId } from '@/hooks/use-interviews'
import { useAllQuestionSetStatuses, useQuestionsWithAnswers, useRetryAnalysis } from '@/hooks/use-question-sets'
import { Logo } from '@/components/ui/logo'
import { Character } from '@/components/ui/character'
import { Button } from '@/components/ui/button'

interface ModelAnswerSectionProps {
  interviewId: number
  questionSetId: number
  category: string
}

const ModelAnswerSection = ({ interviewId, questionSetId, category }: ModelAnswerSectionProps) => {
  const { data } = useQuestionsWithAnswers(interviewId, questionSetId, true)
  const questions = data?.data?.questions ?? []

  if (questions.length === 0) return null

  return (
    <div className="space-y-3">
      <h4 className="text-xs font-black uppercase tracking-widest text-text-tertiary">{INTERVIEW_TYPE_LABELS[category as InterviewType]?.label ?? category}</h4>
      {questions.map((q) => (
        <div key={q.questionId} className="rounded-2xl bg-card border border-border p-5">
          <p className="text-sm font-bold text-text-primary mb-2">{q.questionText}</p>
          {q.modelAnswer ? (
            <p className="text-sm text-text-secondary leading-relaxed">{q.modelAnswer}</p>
          ) : (
            <p className="text-sm text-text-tertiary italic">모범답변이 없습니다</p>
          )}
        </div>
      ))}
    </div>
  )
}

const PROGRESS_STEPS = [
  { key: 'PENDING_UPLOAD', label: '대기', fullLabel: '업로드 대기 중' },
  { key: 'EXTRACTING', label: '추출', fullLabel: '영상 처리 중' },
  { key: 'ANALYZING', label: '분석', fullLabel: 'AI가 답변을 분석 중' },
  { key: 'FINALIZING', label: '생성', fullLabel: '종합 피드백 생성 중' },
] as const

const getProgressIndex = (analysisStatus: string | null): number => {
  if (!analysisStatus) return -1
  return PROGRESS_STEPS.findIndex((s) => s.key === analysisStatus)
}

const getProgressLabel = (analysisStatus: string | null): string => {
  if (!analysisStatus) return '대기 중'
  const step = PROGRESS_STEPS.find((s) => s.key === analysisStatus)
  if (step) return step.fullLabel
  return '대기 중'
}

interface AnalysisStatusFloatProps {
  hasQuestionSets: boolean
  isAnalyzing: boolean
  allCompleted: boolean
  hasFailed: boolean
  completedCount: number
  skippedCount: number
  totalCount: number
  isRetrying: boolean
  onRetry: () => void
  onNavigateFeedback: () => void
  statuses: Array<{ analysisStatus: string; convertStatus: string | null; failureReason: string | null } | null>
  questionSets: Array<{ id: number; category: string }>
}

const AnalysisStatusFloat = ({
  hasQuestionSets,
  isAnalyzing,
  allCompleted,
  hasFailed,
  completedCount,
  skippedCount,
  totalCount,
  isRetrying,
  onRetry,
  onNavigateFeedback,
  statuses,
  questionSets,
}: AnalysisStatusFloatProps) => {
  if (!hasQuestionSets) return null
  if (!isAnalyzing && !allCompleted && !hasFailed) return null

  return (
    <div className="fixed bottom-4 right-4 z-40 w-72 animate-fade-in" role="status" aria-live="polite">
      <div className="rounded-2xl bg-card border border-border shadow-xl p-5">
        {isAnalyzing && (
          <div className="space-y-3">
            <div className="flex items-center gap-3">
              <div className="h-5 w-5 animate-spin rounded-full border-2 border-violet-legacy border-t-transparent flex-shrink-0" />
              <p className="text-sm font-bold text-text-primary">AI가 영상을 분석 중...</p>
            </div>
            <p className="text-xs text-text-secondary">
              {completedCount} / {totalCount - skippedCount} 완료
            </p>
            {/* 질문세트별 스텝 프로그레스 */}
            <div className="mt-2 space-y-3">
              {statuses.map((status, idx) => {
                if (!status || status.analysisStatus === 'COMPLETED' || status.analysisStatus === 'SKIPPED') return null
                const label = questionSets[idx]?.category ? INTERVIEW_TYPE_LABELS[questionSets[idx].category as InterviewType]?.label ?? questionSets[idx].category : `세트 ${idx + 1}`
                const currentStep = getProgressIndex(status.analysisStatus)
                const progressLabel = getProgressLabel(status.analysisStatus)

                return (
                  <div key={questionSets[idx]?.id ?? idx} className="space-y-1.5">
                    <div className="flex items-center justify-between text-xs">
                      <span className="font-semibold text-text-primary">{label}</span>
                      <span className="font-bold text-violet-legacy">{progressLabel}</span>
                    </div>
                    <div
                      className="flex items-center gap-1"
                      role="progressbar"
                      aria-valuenow={currentStep + 1}
                      aria-valuemin={0}
                      aria-valuemax={PROGRESS_STEPS.length}
                      aria-label={`${label} ${progressLabel}`}
                    >
                      {PROGRESS_STEPS.map((step, stepIdx) => (
                        <div key={step.key} className="flex items-center flex-1">
                          <div
                            className={`h-1.5 w-full rounded-full transition-colors duration-500 ${
                              stepIdx <= currentStep
                                ? 'bg-violet-legacy'
                                : 'bg-border'
                            } ${stepIdx === currentStep ? 'animate-pulse' : ''}`}
                          />
                        </div>
                      ))}
                    </div>
                  </div>
                )
              })}
            </div>
          </div>
        )}

        {allCompleted && (
          <div className="space-y-3">
            <div className="flex items-center gap-3">
              <div className="flex h-5 w-5 items-center justify-center rounded-full bg-violet-legacy text-[10px] font-black text-white flex-shrink-0">
                ✓
              </div>
              <p className="text-sm font-bold text-text-primary">분석 완료!</p>
            </div>
            <Button
              variant="default"
              size="sm"
              fullWidth
              onClick={onNavigateFeedback}
              className="rounded-xl"
            >
              피드백 보러가기
            </Button>
          </div>
        )}

        {hasFailed && (
          <div className="space-y-3">
            <div className="flex items-center gap-3">
              <div className="flex h-5 w-5 items-center justify-center rounded-full bg-error text-[10px] font-black text-white flex-shrink-0">
                !
              </div>
              <p className="text-sm font-bold text-text-primary">일부 피드백 생성 실패</p>
            </div>
            <div className="flex items-center gap-2">
              <Button
                variant="secondary"
                size="sm"
                onClick={onRetry}
                disabled={isRetrying}
                loading={isRetrying}
                className="flex-1 h-10 rounded-xl"
              >
                {isRetrying ? '재시도 중...' : '재시도'}
              </Button>
              {completedCount > 0 && (
                <Button
                  variant="default"
                  size="sm"
                  onClick={onNavigateFeedback}
                  className="flex-1 h-10 rounded-xl"
                >
                  완료된 결과 보기
                </Button>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

export const InterviewAnalysisPage = () => {
  const { publicId } = useParams<{ publicId: string }>()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const interviewPublicId = publicId ?? ''

  const { data: response } = useInterviewByPublicId(interviewPublicId)
  const interview = response?.data
  const questionSets = useMemo(() => interview?.questionSets ?? [], [interview?.questionSets])

  // 모든 질문세트 상태 폴링 (5초 간격)
  const hasQuestionSets = questionSets.length > 0
  const statusQueries = useAllQuestionSetStatuses(
    interview?.id ?? 0,
    questionSets,
    hasQuestionSets && !!interview,
  )

  const statuses = statusQueries.map((q) => q.data?.data ?? null)

  const retryMutation = useRetryAnalysis()
  const [isRetrying, setIsRetrying] = useState(false)

  const handleRetryAll = useCallback(async () => {
    if (!interview || isRetrying) return
    setIsRetrying(true)
    const failedSets = questionSets
      .map((qs, idx) => ({ qs, idx }))
      .filter(({ idx }) => statuses[idx]?.analysisStatus === 'FAILED' || statuses[idx]?.analysisStatus === 'PARTIAL' || statuses[idx]?.convertStatus === 'FAILED')

    await Promise.allSettled(
      failedSets.map(({ qs, idx }) =>
        retryMutation.mutateAsync(
          { interviewId: interview.id, questionSetId: qs.id },
        ).then(() => statusQueries[idx].refetch()).catch(() => {}),
      ),
    )
    setIsRetrying(false)
  }, [interview, isRetrying, questionSets, statuses, retryMutation, statusQueries])

  const isTerminal = (s: typeof statuses[number]) =>
    s?.analysisStatus === 'COMPLETED' || s?.analysisStatus === 'PARTIAL' || s?.analysisStatus === 'FAILED' || s?.analysisStatus === 'SKIPPED'
  const allTerminal = hasQuestionSets && statuses.every(isTerminal)
  const completedCount = statuses.filter((s) => s?.analysisStatus === 'COMPLETED' || s?.analysisStatus === 'PARTIAL').length
  const allCompleted = allTerminal && completedCount > 0 && !statuses.some((s) => s?.analysisStatus === 'FAILED')
  const hasFailed = statuses.some((s) => s?.analysisStatus === 'FAILED' || s?.analysisStatus === 'PARTIAL' || s?.convertStatus === 'FAILED')
  const isAnalyzing = hasQuestionSets && statuses.some(
    (s) => ['PENDING', 'PENDING_UPLOAD', 'EXTRACTING', 'ANALYZING', 'FINALIZING'].includes(s?.analysisStatus ?? ''),
  )

  // 페이지 이탈 시 알림 권한 요청 + 완료 시 알림
  useEffect(() => {
    if (hasQuestionSets && !allCompleted) {
      if ('Notification' in window && Notification.permission === 'default') {
        Notification.requestPermission()
      }
    }
  }, [hasQuestionSets, allCompleted])

  useEffect(() => {
    if (allCompleted && 'Notification' in window && Notification.permission === 'granted') {
      if (document.hidden) {
        new Notification('리허설 - 분석 완료', {
          body: '면접 분석이 완료되었습니다. 결과를 확인하세요!',
        })
      }
    }
  }, [allCompleted])

  if (!interview) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <div className="text-center space-y-4">
          <div className="h-1 w-24 bg-violet-legacy/20 rounded-full mx-auto overflow-hidden">
            <div className="h-full bg-violet-legacy animate-progress-loading" />
          </div>
          <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">로딩 중</p>
        </div>
      </div>
    )
  }

  // 질문세트가 없는 레거시 면접 → 기존 완료 페이지 동작
  if (!hasQuestionSets) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background px-4 text-text-primary">
        <div className="w-full max-w-md space-y-8 text-center">
          <Character mood="happy" size={200} className="mx-auto" />
          <div className="space-y-2">
            <h1 className="text-3xl font-extrabold tracking-tighter text-text-primary">면접이 완료되었습니다</h1>
            <p className="text-base font-medium text-text-secondary">
              수고하셨습니다!
            </p>
          </div>
          <Button
            variant="default"
            size="lg"
            fullWidth
            onClick={() => navigate('/')}
            className="rounded-[24px] font-black text-lg"
          >
            홈으로 돌아가기
          </Button>
        </div>
      </div>
    )
  }

  const skippedCount = statuses.filter((s) => s?.analysisStatus === 'SKIPPED').length
  const allSkipped = hasQuestionSets && statuses.length > 0 && statuses.every((s) => s?.analysisStatus === 'SKIPPED')

  return (
    <div className="min-h-screen bg-background">
      <Helmet>
        <title>면접 분석 중 - 리허설</title>
        <meta name="robots" content="noindex, nofollow" />
      </Helmet>
      {/* Header */}
      <header className="sticky top-0 z-50 bg-background/80 backdrop-blur-md px-5 pt-6 pb-4 border-b border-border">
        <div className="mx-auto flex max-w-3xl items-center justify-between">
          <div className="flex items-center gap-2" onClick={() => navigate('/')} role="button">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-violet-legacy shadow-lg shadow-violet-legacy/20">
              <Logo size={24} />
            </div>
            <span className="text-lg font-black tracking-tight text-text-primary">면접 완료</span>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-3xl px-5 pt-8 pb-32">
        {allSkipped ? (
          <section className="flex flex-col items-center justify-center py-16 text-center">
            <Character mood="confused" size={160} className="mb-8" />
            <h2 className="text-2xl font-extrabold tracking-tight text-text-primary mb-2">
              진행한 질문이 없습니다
            </h2>
            <p className="text-sm text-text-secondary mb-8">
              면접이 일찍 종료되었습니다.
            </p>
            <button
              onClick={() => navigate('/')}
              className="h-14 w-full max-w-xs rounded-[24px] bg-violet-legacy font-bold text-white transition-transform active:scale-95"
            >
              대시보드로 이동
            </button>
          </section>
        ) : (
          <section>
            <h2 className="text-lg font-extrabold tracking-tight text-text-primary mb-6">모범답변</h2>
            <div className="space-y-6">
              {questionSets.map((qs, idx) => {
                const status = statuses[idx]
                if (status?.analysisStatus === 'SKIPPED') return null
                return (
                  <ModelAnswerSection
                    key={qs.id}
                    interviewId={interview.id}
                    questionSetId={qs.id}
                    category={qs.category}
                  />
                )
              })}
            </div>
          </section>
        )}
      </main>

      <AnalysisStatusFloat
        hasQuestionSets={hasQuestionSets}
        isAnalyzing={isAnalyzing}
        allCompleted={allCompleted}
        hasFailed={hasFailed}
        completedCount={completedCount}
        skippedCount={skippedCount}
        totalCount={statuses.length}
        isRetrying={isRetrying}
        onRetry={handleRetryAll}
        onNavigateFeedback={async () => {
          await queryClient.invalidateQueries({ queryKey: ['interviews'] })
          navigate(`/interview/${interviewPublicId}/feedback`, { replace: true })
        }}
        statuses={statuses}
        questionSets={questionSets}
      />
    </div>
  )
}
