import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useInterviewStore } from '@/stores/interview-store'
import { useGenerateFeedback } from '@/hooks/use-feedback'
import { Character } from '@/components/ui/character'
import type { AnswerData } from '@/types/interview'

const ANALYSIS_STEPS = [
  '답변 내용 분석 중...',
  '비언어적 패턴 확인 중...',
  '음성 톤 분석 중...',
  '피드백 생성 중...',
]

const AnalysisProgress = () => {
  const [currentStep, setCurrentStep] = useState(0)

  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentStep((prev) =>
        prev < ANALYSIS_STEPS.length - 1 ? prev + 1 : prev,
      )
    }, 3000)
    return () => clearInterval(timer)
  }, [])

  return (
    <div className="w-full max-w-xs space-y-3">
      {ANALYSIS_STEPS.map((step, i) => (
        <div
          key={step}
          className={`flex items-center gap-3 transition-opacity duration-300 ${
            i <= currentStep ? 'opacity-100' : 'opacity-30'
          }`}
        >
          {i < currentStep ? (
            <span className="flex h-5 w-5 items-center justify-center rounded-full bg-accent text-xs text-white">
              ✓
            </span>
          ) : i === currentStep ? (
            <span className="h-5 w-5 animate-spin rounded-full border-2 border-accent border-t-transparent" />
          ) : (
            <span className="h-5 w-5 rounded-full border-2 border-border" />
          )}
          <span className={`text-sm ${i <= currentStep ? 'text-text-primary' : 'text-text-tertiary'}`}>
            {step}
          </span>
        </div>
      ))}
    </div>
  )
}

export const InterviewCompletePage = () => {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const interviewId = Number(id)

  const { questions, answers } = useInterviewStore()
  const generateFeedback = useGenerateFeedback()

  useEffect(() => {
    if (!id || generateFeedback.isPending || generateFeedback.isSuccess || generateFeedback.isError) return
    if (answers.length === 0) return

    const answerDataList: AnswerData[] = answers.map((answer, index) => {
      const question = questions[index]
      const answerText = answer.transcripts
        .filter((t) => t.isFinal)
        .map((t) => t.text)
        .join(' ')

      const nonVerbalSummary = answer.nonVerbalEvents.length > 0
        ? answer.nonVerbalEvents.map((e) => `${e.type}: ${e.data.description}`).join(', ')
        : undefined

      const voiceSummary = answer.voiceEvents.length > 0
        ? answer.voiceEvents.map((e) => `${e.type}(${e.duration}ms)`).join(', ')
        : undefined

      return {
        questionIndex: index,
        questionContent: question?.content ?? '',
        answerText: answerText || '(답변 없음)',
        nonVerbalSummary,
        voiceSummary,
      }
    })

    generateFeedback.mutate({
      interviewId,
      data: { answers: answerDataList },
    })
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id])

  const handleViewReview = () => {
    navigate(`/interview/${id}/review`)
  }

  const handleViewReport = () => {
    navigate(`/interview/${id}/report`)
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-white px-4 text-text-primary sm:px-6">
      <div className="w-full max-w-md space-y-8 text-center">
        {generateFeedback.isPending && (
          <>
            <Character mood="thinking" size={200} className="mx-auto" />
            <div className="space-y-2">
              <h1 className="text-3xl font-extrabold tracking-tighter text-text-primary">AI 피드백 생성 중</h1>
              <p className="text-base font-medium text-text-secondary">
                면접 답변을 분석하고 있습니다. 잠시만 기다려주세요.
              </p>
            </div>
            <div className="flex justify-center">
              <AnalysisProgress />
            </div>
          </>
        )}

        {generateFeedback.isSuccess && (
          <>
            <Character mood="happy" size={200} className="mx-auto" />
            <div className="space-y-2">
              <h1 className="text-3xl font-extrabold tracking-tighter text-text-primary">피드백 생성 완료</h1>
              <p className="text-base font-medium text-text-secondary">
                {generateFeedback.data.data.totalCount}개의 피드백이 생성되었습니다.
              </p>
            </div>
            <div className="space-y-3">
              <button
                onClick={handleViewReview}
                className="h-16 w-full rounded-[24px] bg-accent font-black text-lg text-white transition-all active:scale-95"
              >
                타임스탬프 피드백 리뷰
              </button>
              <button
                onClick={handleViewReport}
                className="h-16 w-full rounded-[24px] border border-border bg-surface font-bold text-text-primary transition-all active:scale-95"
              >
                종합 리포트 보기
              </button>
            </div>
          </>
        )}

        {generateFeedback.isError && (
          <>
            <Character mood="confused" size={160} className="mx-auto" />
            <div className="space-y-2">
              <h1 className="text-3xl font-extrabold tracking-tighter text-text-primary">피드백 생성 실패</h1>
              <p className="text-base font-medium text-text-secondary">
                피드백 생성 중 오류가 발생했습니다. 다시 시도해주세요.
              </p>
            </div>
            <button
              onClick={() => navigate('/')}
              className="h-16 w-full rounded-[24px] border border-border bg-surface font-bold text-text-primary transition-all active:scale-95"
            >
              홈으로 돌아가기
            </button>
          </>
        )}
      </div>
    </div>
  )
}
