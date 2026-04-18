import { Progress } from '@/components/ui/progress'
import type { Step } from '@/constants/setup'

interface SetupProgressBarProps {
  currentStep: Step
  totalSteps: number
}

export const SetupProgressBar = ({ currentStep, totalSteps }: SetupProgressBarProps) => {
  const percent = Math.round((currentStep / totalSteps) * 100)

  return (
    <div className="mb-12">
      <div className="flex items-center justify-between mb-2">
        {Array.from({ length: totalSteps }, (_, i) => i + 1).map((step) => (
          <div key={step} className="flex items-center gap-1">
            <div
              className={`flex h-8 w-8 items-center justify-center rounded-full text-xs font-black transition-colors ${
                step < currentStep
                  ? 'bg-brand text-brand-foreground'
                  : step === currentStep
                    ? 'bg-brand text-brand-foreground shadow-lg shadow-brand/25 ring-2 ring-brand/20 ring-offset-2 ring-offset-background'
                    : 'bg-surface text-text-tertiary'
              }`}
            >
              {step < currentStep ? (
                <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z" />
                </svg>
              ) : (
                step
              )}
            </div>
          </div>
        ))}
      </div>
      <Progress value={percent} aria-label="면접 설정 진행 상황" className="h-1 bg-surface" />
      <p className="mt-2 text-right text-[11px] font-bold text-text-tertiary">
        {currentStep} / {totalSteps}
      </p>
    </div>
  )
}
