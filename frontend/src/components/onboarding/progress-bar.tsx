import { TOTAL_STEPS } from './constants'

export const ProgressBar = ({ step }: { step: number }) => {
  const progress = ((step + 1) / TOTAL_STEPS) * 100

  return (
    <div className="h-1 w-full bg-border">
      <div
        className="h-full bg-accent transition-all duration-300 ease-out"
        style={{ width: `${progress}%` }}
      />
    </div>
  )
}
