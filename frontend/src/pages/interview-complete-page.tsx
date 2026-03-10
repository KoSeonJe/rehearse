import { useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useInterviewStore } from '../stores/interview-store'
import { useGenerateFeedback } from '../hooks/use-feedback'
import { Character } from '@/components/ui/character'
import { Button } from '@/components/ui/button'
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
    <div className="flex min-h-screen items-center justify-center bg-background px-4 sm:px-6">
      <div className="w-full max-w-md space-y-8 text-center">
        {generateFeedback.isPending && (
          <>
            <Character mood="thinking" size={100} className="mx-auto" />
            <div className="space-y-2">
              <h1 className="text-2xl font-bold text-text-primary">AI 피드백 생성 중</h1>
              <p className="text-sm text-text-secondary">
                면접 답변을 분석하고 있습니다. 잠시만 기다려주세요.
              </p>
            </div>
            <div className="mx-auto h-1.5 w-48 overflow-hidden rounded-full bg-border">
              <div className="h-full animate-pulse rounded-full bg-accent" style={{ width: '60%' }} />
            </div>
          </>
        )}

        {generateFeedback.isSuccess && (
          <>
            <Character mood="happy" size={100} className="mx-auto" />
            <div className="space-y-2">
              <h1 className="text-2xl font-bold text-text-primary">피드백 생성 완료</h1>
              <p className="text-sm text-text-secondary">
                {generateFeedback.data.data.totalCount}개의 피드백이 생성되었습니다.
              </p>
            </div>
            <div className="space-y-3">
              <Button
                variant="primary"
                fullWidth
                onClick={handleViewReview}
              >
                타임스탬프 피드백 리뷰
              </Button>
              <Button
                variant="secondary"
                fullWidth
                onClick={handleViewReport}
              >
                종합 리포트 보기
              </Button>
            </div>
          </>
        )}

        {generateFeedback.isError && (
          <>
            <Character mood="confused" size={100} className="mx-auto" />
            <div className="space-y-2">
              <h1 className="text-2xl font-bold text-text-primary">피드백 생성 실패</h1>
              <p className="text-sm text-text-secondary">
                피드백 생성 중 오류가 발생했습니다. 다시 시도해주세요.
              </p>
            </div>
            <Button
              variant="secondary"
              onClick={() => navigate('/')}
            >
              홈으로 돌아가기
            </Button>
          </>
        )}
      </div>
    </div>
  )
}

export default InterviewCompletePage
