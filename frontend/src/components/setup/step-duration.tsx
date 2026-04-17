import { DURATION_PRESETS } from '@/constants/setup'

interface StepDurationProps {
  durationMinutes: number
  isLoading: boolean
  onSelect: (minutes: number) => void
}

export const StepDuration = ({ durationMinutes, isLoading, onSelect }: StepDurationProps) => {
  return (
    <section className="motion-safe:animate-fadeIn">
      <p className="font-mono text-[10px] font-black uppercase tracking-[0.2em] text-violet-legacy mb-3">
        Step 3 — Duration
      </p>
      <h1 className="text-3xl font-extrabold tracking-tighter text-text-primary sm:text-4xl">
        면접 시간을 선택해주세요
      </h1>
      <p className="mt-3 text-base font-medium text-text-secondary">
        시간에 맞춰 질문 수가 자동으로 조정됩니다
      </p>

      <div className="mt-10 grid grid-cols-2 gap-3">
        {DURATION_PRESETS.map((preset) => (
          <button
            key={preset.minutes}
            onClick={() => onSelect(preset.minutes)}
            disabled={isLoading}
            className={`flex flex-col items-center gap-2 rounded-[20px] p-6 transition-all active:scale-95 ${
              durationMinutes === preset.minutes
                ? 'bg-violet-legacy text-white shadow-lg shadow-violet-legacy/20'
                : 'bg-surface text-text-primary hover:bg-slate-200'
            }`}
          >
            <span className="text-3xl font-extrabold">{preset.label}</span>
            <span
              className={`text-sm font-medium ${
                durationMinutes === preset.minutes ? 'text-white/80' : 'text-text-secondary'
              }`}
            >
              {preset.description}
            </span>
          </button>
        ))}
      </div>

      <p className="mt-6 text-center text-xs font-medium text-text-tertiary">
        면접 종료 2분 전에 마무리 안내가 표시됩니다
      </p>
    </section>
  )
}
