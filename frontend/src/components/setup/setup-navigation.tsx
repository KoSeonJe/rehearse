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
    <div className="mt-14 space-y-4">
      {/* 다음/시작 — 인라인 텍스트 링크 */}
      <div className="flex items-center justify-between">
        {currentStep > 1 ? (
          <button
            type="button"
            onClick={onPrev}
            disabled={isLoading}
            className="text-sm font-medium text-muted-foreground underline underline-offset-4 decoration-muted-foreground/40 hover:text-foreground hover:decoration-foreground/60 transition-colors disabled:opacity-40 disabled:cursor-not-allowed focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-foreground/30 rounded-sm"
          >
            ← 이전 단계
          </button>
        ) : (
          <span aria-hidden="true" />
        )}

        {isSubmitStep ? (
          <button
            type="button"
            onClick={onSubmit}
            disabled={!canNext || isLoading}
            className="text-sm font-bold text-accent-editorial underline underline-offset-4 decoration-accent-editorial/50 hover:decoration-accent-editorial transition-colors disabled:opacity-40 disabled:cursor-not-allowed focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent-editorial/30 rounded-sm"
          >
            {isLoading ? '생성 중...' : '면접 시작하기 →'}
          </button>
        ) : (
          <button
            type="button"
            onClick={onNext}
            disabled={!canNext || isLoading}
            className="text-sm font-bold text-accent-editorial underline underline-offset-4 decoration-accent-editorial/50 hover:decoration-accent-editorial transition-colors disabled:opacity-40 disabled:cursor-not-allowed focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent-editorial/30 rounded-sm"
          >
            다음 단계 →
          </button>
        )}
      </div>

      {serverError && (
        <p className="text-center text-sm font-bold text-error" role="alert">
          {serverError}
        </p>
      )}
    </div>
  )
}
