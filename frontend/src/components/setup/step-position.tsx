import { POSITIONS } from '@/constants/setup'
import { POSITION_LABELS } from '@/constants/interview-labels'
import type { Position } from '@/types/interview'

interface StepPositionProps {
  position: Position | null
  isLoading: boolean
  onSelect: (p: Position) => void
}

export const StepPosition = ({ position, isLoading, onSelect }: StepPositionProps) => {
  return (
    <section className="motion-safe:animate-fadeIn">
      <h1 className="text-3xl font-extrabold tracking-tighter text-text-primary sm:text-4xl">
        어떤 직무를 준비하고 계신가요?
      </h1>
      <p className="mt-3 text-base font-medium text-text-secondary">
        직무에 맞는 면접 유형을 추천해드립니다
      </p>

      {/* TODO(design): variant 판단 보류 — 선택 카드 패턴(active/inactive 조건부 스타일), 사용자 확인 필요 */}
      <div className="mt-10 grid grid-cols-2 gap-3 sm:grid-cols-3">
        {POSITIONS.map((p) => (
          <button
            key={p}
            onClick={() => onSelect(p)}
            disabled={isLoading}
            className={`flex flex-col items-center gap-2 rounded-2xl p-5 transition-colors active:scale-95 ${
              position === p
                ? 'bg-primary text-primary-foreground shadow-lg shadow-primary/20'
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
