import type { Step } from '@/constants/setup'
import { Button } from '@/components/ui/button'

interface SetupNavigationProps {
  currentStep: Step
  isSubmitStep: boolean
  canNext: boolean
  isLoading: boolean
  serverError: string | null
  onNext: () => void
  onPrev: () => void
  onSubmit: () => void
}

export const SetupNavigation = ({
  currentStep,
  isSubmitStep,
  canNext,
  isLoading,
  serverError,
  onNext,
  onPrev,
  onSubmit,
}: SetupNavigationProps) => {
  return (
    <div className="mt-16 space-y-3">
      {isSubmitStep ? (
        <Button
          variant="default"
          size="lg"
          fullWidth
          onClick={onSubmit}
          disabled={!canNext || isLoading}
          loading={isLoading}
          className="rounded-3xl font-black"
        >
          {isLoading ? '면접관이 질문을 생성 중입니다...' : '면접 시작하기'}
        </Button>
      ) : (
        <Button
          variant="default"
          size="lg"
          fullWidth
          onClick={onNext}
          disabled={!canNext || isLoading}
          className="rounded-3xl font-black"
        >
          다음
        </Button>
      )}

      {currentStep > 1 && (
        <Button
          variant="secondary"
          size="lg"
          fullWidth
          onClick={onPrev}
          disabled={isLoading}
          className="rounded-3xl h-14 text-base bg-surface hover:bg-slate-200 border-none shadow-none"
        >
          이전
        </Button>
      )}

      {serverError && (
        <p className="mt-2 text-center text-sm font-bold text-error" role="alert">
          {serverError}
        </p>
      )}
    </div>
  )
}
