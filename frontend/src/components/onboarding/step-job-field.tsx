import { JOB_OPTIONS } from './constants'
import type { JobField } from './types'

interface StepJobFieldProps {
  selected: JobField | null
  onSelect: (field: JobField) => void
}

export const StepJobField = ({ selected, onSelect }: StepJobFieldProps) => {
  return (
    <div className="flex flex-col items-center">
      <h1 className="text-2xl font-semibold text-text-primary">
        어떤 면접을 준비하세요?
      </h1>
      <p className="mt-2 text-sm text-text-secondary">
        직무 분야를 선택해주세요
      </p>

      <div
        role="radiogroup"
        aria-label="직무 분야 선택"
        className="mt-8 grid w-full max-w-md grid-cols-2 gap-3 sm:grid-cols-3"
      >
        {JOB_OPTIONS.map((option) => (
          <button
            key={option.id}
            type="button"
            role="radio"
            aria-checked={selected === option.id}
            onClick={() => onSelect(option.id)}
            className={[
              'flex flex-col items-center gap-2 rounded-card border p-4 transition-all duration-150',
              'hover:border-accent/40 hover:bg-accent-light/30',
              'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2',
              selected === option.id
                ? 'border-accent bg-accent-light/20 text-accent'
                : 'border-border bg-surface text-text-secondary',
            ].join(' ')}
          >
            <span className="flex h-10 w-10 items-center justify-center">
              {option.icon}
            </span>
            <span className="text-sm font-medium">{option.label}</span>
          </button>
        ))}
      </div>
    </div>
  )
}
