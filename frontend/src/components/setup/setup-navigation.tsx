import type { Step } from '@/constants/setup'

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
        <button
          onClick={onSubmit}
          disabled={!canNext || isLoading}
          className="h-16 w-full rounded-[24px] bg-violet-legacy py-4 text-lg font-black text-white shadow-lg shadow-violet-legacy/20 transition-all active:scale-95 disabled:opacity-50"
        >
          {isLoading ? '면접관이 질문을 생성 중입니다...' : '면접 시작하기'}
        </button>
      ) : (
        <button
          onClick={onNext}
          disabled={!canNext || isLoading}
          className={`h-16 w-full rounded-[24px] py-4 text-lg font-black text-white transition-all active:scale-95 shadow-lg ${
            canNext && !isLoading
              ? 'bg-violet-legacy shadow-violet-legacy/20'
              : 'bg-slate-200 cursor-not-allowed opacity-50'
          }`}
        >
          다음
        </button>
      )}

      {currentStep > 1 && (
        <button
          onClick={onPrev}
          disabled={isLoading}
          className="h-14 w-full rounded-[24px] bg-surface py-3 text-base font-bold text-text-secondary transition-all hover:bg-slate-200 active:scale-95 disabled:opacity-50"
        >
          이전
        </button>
      )}

      {serverError && (
        <p className="mt-2 text-center text-sm font-bold text-error" role="alert">
          {serverError}
        </p>
      )}
    </div>
  )
}
