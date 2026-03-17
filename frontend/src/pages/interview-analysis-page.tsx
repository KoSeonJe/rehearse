import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useInterview } from '@/hooks/use-interviews'
import { useAllQuestionSetStatuses, useQuestionsWithAnswers } from '@/hooks/use-question-sets'
import { Logo } from '@/components/ui/logo'
import { Character } from '@/components/ui/character'
import type { AnalysisProgress, AnalysisStatus } from '@/types/interview'

const PROGRESS_LABELS: Record<string, string> = {
  STARTED: '분석 준비 중...',
  EXTRACTING: '음성/영상 추출 중...',
  STT_PROCESSING: '음성을 텍스트로 변환 중...',
  VERBAL_ANALYZING: '답변 내용을 분석 중...',
  NONVERBAL_ANALYZING: '표정과 자세를 분석 중...',
  FINALIZING: '종합 평가를 생성 중...',
  FAILED: '분석 실패',
}

const PROGRESS_ORDER: AnalysisProgress[] = [
  'STARTED',
  'EXTRACTING',
  'STT_PROCESSING',
  'VERBAL_ANALYZING',
  'NONVERBAL_ANALYZING',
  'FINALIZING',
]

const getProgressPercent = (status: AnalysisStatus, progress: AnalysisProgress | null): number => {
  if (status === 'COMPLETED') return 100
  if (status === 'FAILED') return 0
  if (status === 'PENDING' || status === 'PENDING_UPLOAD') return 0
  if (!progress) return 5
  const idx = PROGRESS_ORDER.indexOf(progress)
  if (idx === -1) return 5
  return Math.round(((idx + 1) / PROGRESS_ORDER.length) * 90) + 5
}

const getStatusLabel = (status: AnalysisStatus, progress: AnalysisProgress | null): string => {
  if (status === 'COMPLETED') return '분석 완료'
  if (status === 'FAILED') return '분석 실패'
  if (status === 'PENDING') return '업로드 대기 중...'
  if (status === 'PENDING_UPLOAD') return '업로드 중...'
  if (status === 'ANALYZING' && progress) return PROGRESS_LABELS[progress] ?? '분석 중...'
  return '분석 중...'
}

interface ModelAnswerTabProps {
  interviewId: number
  questionSetId: number
  category: string
}

const ModelAnswerTab = ({ interviewId, questionSetId, category }: ModelAnswerTabProps) => {
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

export const InterviewAnalysisPage = () => {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const interviewId = id ?? ''

  const { data: response } = useInterview(interviewId)
  const interview = response?.data
  const questionSets = interview?.questionSets ?? []

  const [activeTab, setActiveTab] = useState<'status' | 'answers'>('status')

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

  const allCompleted = hasQuestionSets && statuses.every((s) => s?.analysisStatus === 'COMPLETED')
  const hasFailed = statuses.some((s) => s?.analysisStatus === 'FAILED')
  const completedCount = statuses.filter((s) => s?.analysisStatus === 'COMPLETED').length

  // 모든 완료 시 리포트 페이지로 자동 전환
  useEffect(() => {
    if (allCompleted) {
      const timer = setTimeout(() => {
        navigate(`/interview/${interviewId}/report`)
      }, 1500)
      return () => clearTimeout(timer)
    }
  }, [allCompleted, navigate, interviewId])

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

  return (
    <div className="min-h-screen bg-white">
      {/* Header */}
      <header className="sticky top-0 z-50 bg-white/80 backdrop-blur-md px-5 pt-6 pb-4 border-b border-border">
        <div className="mx-auto flex max-w-3xl items-center justify-between">
          <div className="flex items-center gap-2" onClick={() => navigate('/')} role="button">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-accent shadow-lg shadow-accent/20">
              <Logo size={24} />
            </div>
            <span className="text-lg font-black tracking-tight text-text-primary">분석 중</span>
          </div>
          <div className="text-sm font-bold text-text-secondary">
            {completedCount}/{questionSets.length} 완료
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-3xl px-5 pt-10 pb-32">
        {/* Hero */}
        <section className="text-center mb-10">
          <Character mood={allCompleted ? 'happy' : 'thinking'} size={140} className="mx-auto mb-6" />
          {allCompleted ? (
            <>
              <h1 className="text-2xl font-extrabold tracking-tighter text-text-primary">분석이 완료되었습니다!</h1>
              <p className="mt-2 text-base font-medium text-text-secondary">리포트 페이지로 이동합니다...</p>
            </>
          ) : (
            <>
              <h1 className="text-2xl font-extrabold tracking-tighter text-text-primary">AI가 면접 영상을 분석하고 있습니다</h1>
              <p className="mt-2 text-base font-medium text-text-secondary">약 2~5분 정도 소요됩니다. 이 페이지를 닫아도 분석은 계속됩니다.</p>
            </>
          )}
        </section>

        {/* Tab */}
        <div className="flex gap-1 bg-surface rounded-2xl p-1 mb-8">
          <button
            onClick={() => setActiveTab('status')}
            className={`flex-1 py-3 rounded-xl text-sm font-bold transition-all ${
              activeTab === 'status' ? 'bg-white shadow-toss text-text-primary' : 'text-text-tertiary'
            }`}
          >
            분석 현황
          </button>
          <button
            onClick={() => setActiveTab('answers')}
            className={`flex-1 py-3 rounded-xl text-sm font-bold transition-all ${
              activeTab === 'answers' ? 'bg-white shadow-toss text-text-primary' : 'text-text-tertiary'
            }`}
          >
            모범답변
          </button>
        </div>

        {/* Status Tab */}
        {activeTab === 'status' && (
          <div className="space-y-4">
            {questionSets.map((qs, idx) => {
              const status = statuses[idx]
              const analysisStatus = status?.analysisStatus ?? qs.analysisStatus
              const progress = status?.analysisProgress as AnalysisProgress | null ?? null
              const percent = getProgressPercent(analysisStatus, progress)
              const label = getStatusLabel(analysisStatus, progress)
              const isCompleted = analysisStatus === 'COMPLETED'
              const isFailed = analysisStatus === 'FAILED'

              return (
                <div
                  key={qs.id}
                  className={`rounded-[24px] border p-6 transition-all ${
                    isCompleted ? 'bg-success/5 border-success/20' :
                    isFailed ? 'bg-error/5 border-error/20' :
                    'bg-surface border-border'
                  }`}
                >
                  <div className="flex items-center justify-between mb-3">
                    <div className="flex items-center gap-3">
                      <div className={`flex h-8 w-8 items-center justify-center rounded-full text-xs font-black text-white ${
                        isCompleted ? 'bg-success' : isFailed ? 'bg-error' : 'bg-accent'
                      }`}>
                        {isCompleted ? '✓' : isFailed ? '!' : idx + 1}
                      </div>
                      <div>
                        <p className="text-sm font-bold text-text-primary">질문세트 {idx + 1}</p>
                        <p className="text-xs text-text-tertiary">{qs.category} · {qs.questions.length}문항</p>
                      </div>
                    </div>
                    <span className={`text-xs font-bold ${
                      isCompleted ? 'text-success' : isFailed ? 'text-error' : 'text-accent'
                    }`}>
                      {isCompleted ? '완료' : isFailed ? '실패' : `${percent}%`}
                    </span>
                  </div>

                  {/* Progress bar */}
                  <div className="h-1.5 w-full bg-border/50 rounded-full overflow-hidden mb-2">
                    <div
                      className={`h-full rounded-full transition-all duration-500 ${
                        isCompleted ? 'bg-success' : isFailed ? 'bg-error' : 'bg-accent'
                      }`}
                      style={{ width: `${percent}%` }}
                    />
                  </div>

                  <div className="flex items-center justify-between">
                    <p className={`text-xs font-medium ${
                      isCompleted ? 'text-success' : isFailed ? 'text-error' : 'text-text-secondary'
                    }`}>
                      {label}
                    </p>

                    {/* COMPLETED: 미리보기 링크 */}
                    {isCompleted && (
                      <button
                        onClick={() => navigate(`/interview/${interviewId}/feedback`)}
                        className="text-xs font-bold text-accent hover:underline"
                      >
                        결과 미리보기 →
                      </button>
                    )}

                    {/* FAILED: 재시도 버튼 (BE 클라이언트 retry API 추가 시 연동) */}
                    {isFailed && (
                      <button
                        onClick={() => {
                          // TODO: BE에 클라이언트용 retry API 추가 후 연동
                          window.location.reload()
                        }}
                        className="text-xs font-bold text-error hover:underline"
                      >
                        재시도
                      </button>
                    )}
                  </div>

                  {isFailed && status?.failureReason && (
                    <p className="mt-2 text-xs text-error/70">{status.failureReason}</p>
                  )}
                </div>
              )
            })}
          </div>
        )}

        {/* Model Answers Tab */}
        {activeTab === 'answers' && (
          <div className="space-y-6">
            {questionSets.map((qs) => (
              <ModelAnswerTab
                key={qs.id}
                interviewId={interview.id}
                questionSetId={qs.id}
                category={qs.category}
              />
            ))}
          </div>
        )}

        {/* 부분 실패 시 CTA */}
        {hasFailed && completedCount > 0 && (
          <div className="mt-8 rounded-[24px] bg-surface border border-border p-6 text-center">
            <p className="text-sm font-bold text-text-primary mb-4">
              일부 질문세트 분석이 실패했습니다. 완료된 결과를 먼저 확인하시겠습니까?
            </p>
            <button
              onClick={() => navigate(`/interview/${interviewId}/report`)}
              className="h-12 px-8 rounded-2xl bg-accent font-bold text-white text-sm transition-all active:scale-95"
            >
              완료된 결과 보기
            </button>
          </div>
        )}
      </main>
    </div>
  )
}
