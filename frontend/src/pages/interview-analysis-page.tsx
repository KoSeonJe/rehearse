import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useInterview } from '@/hooks/use-interviews'
import { useAllQuestionSetStatuses, useQuestionsWithAnswers, useRetryAnalysis } from '@/hooks/use-question-sets'
import { Logo } from '@/components/ui/logo'
import { Character } from '@/components/ui/character'

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
      <h4 className="text-xs font-black uppercase tracking-widest text-text-tertiary">{category}</h4>
      {questions.map((q) => (
        <div key={q.questionId} className="rounded-2xl bg-white border border-border p-5">
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
}: AnalysisStatusFloatProps) => {
  if (!hasQuestionSets) return null
  if (!isAnalyzing && !allCompleted && !hasFailed) return null

  return (
    <div className="fixed bottom-4 right-4 z-40 w-72 animate-fade-in" role="status" aria-live="polite">
      <div className="rounded-2xl bg-white border border-border shadow-xl p-5">
        {isAnalyzing && (
          <div className="space-y-3">
            <div className="flex items-center gap-3">
              <div className="h-5 w-5 animate-spin rounded-full border-2 border-accent border-t-transparent flex-shrink-0" />
              <p className="text-sm font-bold text-text-primary">AI가 영상을 분석 중...</p>
            </div>
            <p className="text-xs text-text-secondary">
              {completedCount + skippedCount} / {totalCount} 완료
            </p>
          </div>
        )}

        {allCompleted && (
          <div className="space-y-3">
            <div className="flex items-center gap-3">
              <div className="flex h-5 w-5 items-center justify-center rounded-full bg-accent text-[10px] font-black text-white flex-shrink-0">
                ✓
              </div>
              <p className="text-sm font-bold text-text-primary">분석 완료!</p>
            </div>
            <button
              onClick={onNavigateFeedback}
              className="w-full h-10 rounded-xl bg-accent text-sm font-bold text-white transition-all active:scale-95"
            >
              피드백 보러가기
            </button>
          </div>
        )}

        {hasFailed && (
          <div className="space-y-3">
            <div className="flex items-center gap-3">
              <div className="flex h-5 w-5 items-center justify-center rounded-full bg-error text-[10px] font-black text-white flex-shrink-0">
                !
              </div>
              <p className="text-sm font-bold text-text-primary">일부 분석 실패</p>
            </div>
            <div className="flex items-center gap-2">
              <button
                onClick={onRetry}
                disabled={isRetrying}
                className="flex-1 h-10 rounded-xl border border-border text-sm font-bold text-text-primary transition-all active:scale-95 disabled:opacity-50"
              >
                {isRetrying ? '재시도 중...' : '재시도'}
              </button>
              {completedCount > 0 && (
                <button
                  onClick={onNavigateFeedback}
                  className="flex-1 h-10 rounded-xl bg-accent text-sm font-bold text-white transition-all active:scale-95"
                >
                  완료된 결과 보기
                </button>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

export const InterviewAnalysisPage = () => {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const interviewId = id ?? ''

  const { data: response } = useInterview(interviewId)
  const interview = response?.data
  const questionSets = interview?.questionSets ?? []

  // 모든 질문세트 상태 폴링 (5초 간격)
  const hasQuestionSets = questionSets.length > 0
  const statusQueries = useAllQuestionSetStatuses(
    interview?.id ?? 0,
    questionSets,
    hasQuestionSets,
  )

  const statuses = useMemo(() =>
    statusQueries.map((q) => q.data?.data ?? null),
    [statusQueries],
  )

  const retryMutation = useRetryAnalysis()
  const [isRetrying, setIsRetrying] = useState(false)

  const handleRetryAll = useCallback(async () => {
    if (!interview || isRetrying) return
    setIsRetrying(true)
    const failedSets = questionSets
      .map((qs, idx) => ({ qs, idx }))
      .filter(({ idx }) => statuses[idx]?.analysisStatus === 'FAILED')

    await Promise.allSettled(
      failedSets.map(({ qs, idx }) =>
        retryMutation.mutateAsync(
          { interviewId: interview.id, questionSetId: qs.id },
        ).then(() => statusQueries[idx].refetch()).catch(() => {}),
      ),
    )
    setIsRetrying(false)
  }, [interview, isRetrying, questionSets, statuses, retryMutation, statusQueries])

  const allTerminal = hasQuestionSets && statuses.every((s) =>
    s?.analysisStatus === 'COMPLETED' || s?.analysisStatus === 'SKIPPED',
  )
  const completedCount = statuses.filter((s) => s?.analysisStatus === 'COMPLETED').length
  const allCompleted = allTerminal && completedCount > 0
  const hasFailed = statuses.some((s) => s?.analysisStatus === 'FAILED')
  const isAnalyzing = hasQuestionSets && statuses.some(
    (s) => s?.analysisStatus === 'ANALYZING' || s?.analysisStatus === 'PENDING' || s?.analysisStatus === 'PENDING_UPLOAD',
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
      <div className="flex min-h-screen items-center justify-center bg-white">
        <div className="text-center space-y-4">
          <div className="h-1 w-24 bg-accent/20 rounded-full mx-auto overflow-hidden">
            <div className="h-full bg-accent animate-progress-loading" />
          </div>
          <p className="font-mono text-[10px] font-black uppercase tracking-widest text-accent">로딩 중</p>
        </div>
      </div>
    )
  }

  // 질문세트가 없는 레거시 면접 → 기존 완료 페이지 동작
  if (!hasQuestionSets) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-white px-4 text-text-primary">
        <div className="w-full max-w-md space-y-8 text-center">
          <Character mood="happy" size={200} className="mx-auto" />
          <div className="space-y-2">
            <h1 className="text-3xl font-extrabold tracking-tighter text-text-primary">면접이 완료되었습니다</h1>
            <p className="text-base font-medium text-text-secondary">
              수고하셨습니다! 종합 리포트에서 결과를 확인하세요.
            </p>
          </div>
          <button
            onClick={() => navigate(`/interview/${interviewId}/report`)}
            className="h-16 w-full rounded-[24px] bg-accent font-black text-lg text-white transition-all active:scale-95"
          >
            종합 리포트 보기
          </button>
        </div>
      </div>
    )
  }

  const skippedCount = statuses.filter((s) => s?.analysisStatus === 'SKIPPED').length

  return (
    <div className="min-h-screen bg-white">
      {/* Header */}
      <header className="sticky top-0 z-50 bg-white/80 backdrop-blur-md px-5 pt-6 pb-4 border-b border-border">
        <div className="mx-auto flex max-w-3xl items-center justify-between">
          <div className="flex items-center gap-2" onClick={() => navigate('/')} role="button">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-accent shadow-lg shadow-accent/20">
              <Logo size={24} />
            </div>
            <span className="text-lg font-black tracking-tight text-text-primary">면접 완료</span>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-3xl px-5 pt-8 pb-32">
        {/* 모범답변 — 항상 표시 */}
        <section>
          <h2 className="text-lg font-extrabold tracking-tight text-text-primary mb-6">모범답변</h2>
          <div className="space-y-6">
            {questionSets.map((qs) => (
              <ModelAnswerSection
                key={qs.id}
                interviewId={interview.id}
                questionSetId={qs.id}
                category={qs.category}
              />
            ))}
          </div>
        </section>
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
        onNavigateFeedback={() => navigate(`/interview/${interviewId}/feedback`)}
      />
    </div>
  )
}
