import { useParams, useNavigate } from 'react-router-dom'
import { BackLink } from '@/components/ui/back-link'
import { Button } from '@/components/ui/button'
import { QuestionCard } from '@/components/interview/question-card'
import { QuestionCardSkeleton } from '@/components/interview/question-card-skeleton'
import {
  useInterview,
  useCreateInterview,
  useUpdateInterviewStatus,
} from '@/hooks/use-interviews'
import { LEVEL_LABELS, INTERVIEW_TYPE_LABELS } from '@/types/interview'

export const InterviewReadyPage = () => {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()

  const {
    data: response,
    isLoading,
    isError,
    error,
  } = useInterview(id ?? '')

  const updateStatus = useUpdateInterviewStatus()
  const createInterview = useCreateInterview()

  const interview = response?.data

  const handleStartInterview = () => {
    if (!interview) return

    updateStatus.mutate(
      { id: interview.id, data: { status: 'IN_PROGRESS' } },
      {
        onSuccess: () => {
          navigate(`/interview/${interview.id}/conduct`)
        },
      },
    )
  }

  const handleRegenerateQuestions = () => {
    if (!interview) return

    createInterview.mutate(
      {
        position: interview.position,
        level: interview.level,
        interviewType: interview.interviewType,
      },
      {
        onSuccess: (newResponse) => {
          navigate(`/interview/${newResponse.data.id}/ready`, {
            replace: true,
          })
        },
      },
    )
  }

  const summaryText = interview
    ? `${interview.position} · ${LEVEL_LABELS[interview.level].label} · ${INTERVIEW_TYPE_LABELS[interview.interviewType].label}`
    : ''

  if (isError) {
    const is404 = error?.message?.includes('404')

    return (
      <div className="min-h-screen bg-gray-50">
        <header className="px-4 pt-6 sm:px-6 lg:px-8">
          <BackLink to="/interview/setup" label="설정으로 돌아가기" />
        </header>
        <main className="mx-auto max-w-2xl px-4 pt-12 text-center sm:px-6">
          <p className="text-lg text-gray-900">
            {is404
              ? '세션을 찾을 수 없습니다.'
              : '오류가 발생했습니다. 다시 시도해주세요.'}
          </p>
          <div className="mt-6">
            <Button
              variant="secondary"
              onClick={() => navigate(is404 ? '/' : 0 as never)}
            >
              {is404 ? '홈으로 돌아가기' : '다시 시도'}
            </Button>
          </div>
        </main>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="px-4 pt-6 sm:px-6 lg:px-8">
        <BackLink to="/interview/setup" label="설정으로 돌아가기" />
      </header>

      <main className="mx-auto max-w-2xl px-4 pb-8 pt-6 sm:px-6">
        <section>
          {isLoading ? (
            <>
              <div className="h-8 w-48 animate-pulse rounded bg-gray-200" aria-hidden="true" />
              <div className="mt-2 h-5 w-64 animate-pulse rounded bg-gray-200" aria-hidden="true" />
            </>
          ) : (
            <>
              <h1 className="text-2xl font-semibold text-gray-900">
                면접 준비 완료
              </h1>
              <p className="mt-1 text-sm text-gray-500">{summaryText}</p>
            </>
          )}
        </section>

        <section className="mt-6" aria-label="면접 질문 목록">
          <h2 className="sr-only">면접 질문 목록</h2>
          {isLoading && (
            <div aria-live="polite" className="sr-only">
              질문을 불러오는 중입니다
            </div>
          )}
          <ol className="flex flex-col gap-4">
            {isLoading
              ? Array.from({ length: 5 }, (_, i) => (
                  <QuestionCardSkeleton key={i} />
                ))
              : interview?.questions
                  .slice()
                  .sort((a, b) => a.order - b.order)
                  .map((question) => (
                    <QuestionCard key={question.id} question={question} />
                  ))}
          </ol>
        </section>

        {!isLoading && (
          <div className="mb-8 mt-8">
            <Button
              variant="cta"
              fullWidth
              loading={updateStatus.isPending}
              onClick={handleStartInterview}
            >
              {updateStatus.isPending ? '시작 중...' : '면접 시작'}
            </Button>
            <Button
              variant="ghost"
              fullWidth
              loading={createInterview.isPending}
              onClick={handleRegenerateQuestions}
              className="mt-3"
            >
              {createInterview.isPending
                ? '생성 중...'
                : '질문 다시 생성'}
            </Button>
            {(updateStatus.isError || createInterview.isError) && (
              <p
                className="mt-2 text-center text-sm text-red-600"
                role="alert"
              >
                오류가 발생했습니다. 다시 시도해주세요.
              </p>
            )}
          </div>
        )}
      </main>
    </div>
  )
}
