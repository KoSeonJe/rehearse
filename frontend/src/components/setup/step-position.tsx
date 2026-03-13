import { POSITIONS } from '@/hooks/use-interview-setup'
import { POSITION_LABELS } from '@/types/interview'
import type { Position } from '@/types/interview'

interface StepPositionProps {
  position: Position | null
  isLoading: boolean
  onSelect: (p: Position) => void
}

export const StepPosition = ({ position, isLoading, onSelect }: StepPositionProps) => {
  return (
    <section className="motion-safe:animate-fadeIn">
      <p className="font-mono text-[10px] font-black uppercase tracking-[0.2em] text-accent mb-3">
        Step 1 — Position
      </p>
      <h1 className="text-3xl font-extrabold tracking-tighter text-text-primary sm:text-4xl">
        어떤 직무를 준비하고 계신가요?
      </h1>
      <p className="mt-3 text-base font-medium text-text-secondary">
        직무에 맞는 면접 유형을 추천해드립니다
      </p>

      <div className="mt-10 grid grid-cols-2 gap-3 sm:grid-cols-3">
        {POSITIONS.map((p) => (
          <button
            key={p}
            onClick={() => onSelect(p)}
            disabled={isLoading}
            className={`flex flex-col items-center gap-2 rounded-[20px] p-5 transition-all active:scale-95 ${
              position === p
                ? 'bg-accent text-white shadow-lg shadow-accent/20'
                : 'bg-surface text-text-primary hover:bg-slate-200'
            }`}
          >
            <span className="text-base font-extrabold">{POSITION_LABELS[p].label}</span>
            <span
              className={`text-[11px] font-medium ${
                position === p ? 'text-white/80' : 'text-text-tertiary'
              }`}
            >
              {POSITION_LABELS[p].description}
            </span>
          </button>
        ))}
      </div>
    </section>
  )
}
