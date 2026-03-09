import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { BackLink } from '@/components/ui/back-link'
import { Button } from '@/components/ui/button'
import { TextInput } from '@/components/ui/text-input'
import { SelectionCard } from '@/components/ui/selection-card'
import { useCreateInterview } from '@/hooks/use-interviews'
import { ApiError } from '@/lib/api-client'
import type { Level, InterviewType } from '@/types/interview'
import { LEVEL_LABELS, INTERVIEW_TYPE_LABELS } from '@/types/interview'

const LEVELS: Level[] = ['JUNIOR', 'MID', 'SENIOR']
const INTERVIEW_TYPES: InterviewType[] = ['CS', 'SYSTEM_DESIGN', 'BEHAVIORAL']

export const InterviewSetupPage = () => {
  const navigate = useNavigate()
  const createInterview = useCreateInterview()

  const [position, setPosition] = useState('')
  const [level, setLevel] = useState<Level | null>(null)
  const [interviewType, setInterviewType] = useState<InterviewType | null>(null)
  const [serverError, setServerError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  const isValid =
    position.trim().length > 0 && level !== null && interviewType !== null
  const isLoading = createInterview.isPending

  const handleSubmit = () => {
    if (!isValid || isLoading || !level || !interviewType) return

    setServerError(null)
    setFieldErrors({})

    createInterview.mutate(
      {
        position: position.trim(),
        level,
        interviewType,
      },
      {
        onSuccess: (response) => {
          navigate(`/interview/${response.data.id}/ready`)
        },
        onError: (error) => {
          if (error instanceof ApiError) {
            if (error.errors.length > 0) {
              const errors: Record<string, string> = {}
              for (const err of error.errors) {
                errors[err.field] = err.reason
              }
              setFieldErrors(errors)
            } else {
              setServerError(
                error.message || '오류가 발생했습니다. 다시 시도해주세요.',
              )
            }
          } else {
            setServerError('오류가 발생했습니다. 다시 시도해주세요.')
          }
        },
      },
    )
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="px-4 pt-6 sm:px-6 lg:px-8">
        <BackLink to="/" />
      </header>

      <main className="mx-auto max-w-lg px-4 pb-8 pt-6 sm:px-6">
        <section>
          <h1 className="text-2xl font-semibold text-gray-900">면접 설정</h1>
          <p className="mt-1 text-sm text-gray-500">
            맞춤형 면접 질문을 생성합니다.
          </p>
        </section>

        <section className="mt-8">
          <h2 className="sr-only">직무 입력</h2>
          <TextInput
            label="직무"
            placeholder="예: 백엔드 개발자"
            value={position}
            onChange={(e) => setPosition(e.target.value)}
            maxLength={100}
            disabled={isLoading}
            required
            error={fieldErrors['position']}
          />
        </section>

        <section className="mt-6">
          <h2 className="sr-only">레벨 선택</h2>
          <span className="mb-1.5 block text-sm font-medium text-gray-700">
            레벨
          </span>
          <div
            role="radiogroup"
            aria-label="레벨 선택"
            className="grid grid-cols-2 gap-3 sm:grid-cols-3"
          >
            {LEVELS.map((l) => (
              <SelectionCard
                key={l}
                value={l}
                label={LEVEL_LABELS[l].label}
                description={LEVEL_LABELS[l].description}
                selected={level === l}
                disabled={isLoading}
                onSelect={() => setLevel(l)}
              />
            ))}
          </div>
        </section>

        <section className="mt-6">
          <h2 className="sr-only">면접 유형 선택</h2>
          <span className="mb-1.5 block text-sm font-medium text-gray-700">
            면접 유형
          </span>
          <div
            role="radiogroup"
            aria-label="면접 유형 선택"
            className="flex flex-col gap-3"
          >
            {INTERVIEW_TYPES.map((t) => (
              <SelectionCard
                key={t}
                value={t}
                label={INTERVIEW_TYPE_LABELS[t].label}
                description={INTERVIEW_TYPE_LABELS[t].description}
                selected={interviewType === t}
                disabled={isLoading}
                onSelect={() => setInterviewType(t)}
              />
            ))}
          </div>
        </section>

        <div className="mb-8 mt-8">
          <Button
            variant="primary"
            fullWidth
            disabled={!isValid}
            loading={isLoading}
            onClick={handleSubmit}
          >
            {isLoading ? '질문 생성 중...' : '질문 생성하기'}
          </Button>
          {serverError && (
            <p className="mt-2 text-center text-sm text-red-600" role="alert">
              {serverError}
            </p>
          )}
        </div>
      </main>
    </div>
  )
}
