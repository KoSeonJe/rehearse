import { useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useInterviewStore } from '../stores/interview-store'
import { useGenerateFeedback } from '../hooks/use-feedback'
import type { AnswerData } from '../types/interview'

const InterviewCompletePage = () => {
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
    <div className="flex min-h-screen items-center justify-center bg-gray-50">
      <div className="w-full max-w-md space-y-8 text-center">
        {generateFeedback.isPending && (
          <>
            <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-slate-100">
              <div className="h-8 w-8 animate-spin rounded-full border-3 border-slate-300 border-t-slate-900" />
            </div>
            <div className="space-y-2">
              <h1 className="text-2xl font-bold text-slate-900">AI 피드백 생성 중</h1>
              <p className="text-sm text-slate-500">
                면접 답변을 분석하고 있습니다. 잠시만 기다려주세요.
              </p>
            </div>
            <div className="mx-auto h-1.5 w-48 overflow-hidden rounded-full bg-slate-100">
              <div className="h-full animate-pulse rounded-full bg-slate-900" style={{ width: '60%' }} />
            </div>
          </>
        )}

        {generateFeedback.isSuccess && (
          <>
            <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-green-100">
              <svg className="h-8 w-8 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
              </svg>
            </div>
            <div className="space-y-2">
              <h1 className="text-2xl font-bold text-slate-900">피드백 생성 완료</h1>
              <p className="text-sm text-slate-500">
                {generateFeedback.data.data.totalCount}개의 피드백이 생성되었습니다.
              </p>
            </div>
            <div className="space-y-3">
              <button
                onClick={handleViewReview}
                className="w-full rounded-xl bg-slate-900 px-6 py-3.5 text-sm font-medium text-white transition-colors hover:bg-slate-800"
              >
                타임스탬프 피드백 리뷰
              </button>
              <button
                onClick={handleViewReport}
                className="w-full rounded-xl border border-slate-200 bg-white px-6 py-3.5 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-50"
              >
                종합 리포트 보기
              </button>
            </div>
          </>
        )}

        {generateFeedback.isError && (
          <>
            <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-red-100">
              <svg className="h-8 w-8 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </div>
            <div className="space-y-2">
              <h1 className="text-2xl font-bold text-slate-900">피드백 생성 실패</h1>
              <p className="text-sm text-slate-500">
                피드백 생성 중 오류가 발생했습니다. 다시 시도해주세요.
              </p>
            </div>
            <button
              onClick={() => navigate('/')}
              className="rounded-xl border border-slate-200 bg-white px-6 py-3 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-50"
            >
              홈으로 돌아가기
            </button>
          </>
        )}
      </div>
    </div>
  )
}

export default InterviewCompletePage
