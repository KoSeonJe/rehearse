import type { Step } from '@/constants/setup'

interface SetupNavigationProps {
  currentStep: Step
  isSubmitStep: boolean
  canNext: boolean
  isLoading: boolean
  serverError: string | null
  getDisabledHint: (step: Step) => string
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
  getDisabledHint,
  onNext,
  onPrev,
  onSubmit,
}: SetupNavigationProps) => {
  const disabledHint = !canNext && !isLoading ? getDisabledHint(currentStep) : null

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
            title={disabledHint ?? undefined}
            aria-describedby={disabledHint ? 'setup-disabled-hint' : undefined}
            className="text-sm font-bold text-brand underline underline-offset-4 decoration-brand/50 hover:decoration-brand transition-colors disabled:opacity-40 disabled:cursor-not-allowed focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/30 rounded-sm"
          >
            {isLoading ? '생성 중...' : '면접 시작하기 →'}
          </button>
        ) : (
          <button
            type="button"
            onClick={onNext}
            disabled={!canNext || isLoading}
            title={disabledHint ?? undefined}
            aria-describedby={disabledHint ? 'setup-disabled-hint' : undefined}
            className="text-sm font-bold text-brand underline underline-offset-4 decoration-brand/50 hover:decoration-brand transition-colors disabled:opacity-40 disabled:cursor-not-allowed focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/30 rounded-sm"
          >
            다음 단계 →
          </button>
        )}
      </div>

      {/* disabled 상태에서 진행 불가 사유를 시각적으로 노출 — 툴팁이 모바일에서 안 뜨는 한계 보완 */}
      {disabledHint && (
        <p
          id="setup-disabled-hint"
          className="text-right text-xs text-muted-foreground"
          role="status"
        >
          {disabledHint}
        </p>
      )}

      {serverError && (
        <p className="text-center text-sm font-bold text-error" role="alert">
          {serverError}
        </p>
      )}
    </div>
  )
}
