import { POSITION_TECH_STACKS, TECH_STACK_LABELS } from '@/constants/interview-labels'
import type { Position, TechStack } from '@/types/interview'

interface StepTechStackProps {
  position: Position
  techStack: TechStack | null
  isLoading: boolean
  onSelect: (techStack: TechStack | null) => void
}

export const StepTechStack = ({ position, techStack, isLoading, onSelect }: StepTechStackProps) => {
  const availableStacks = POSITION_TECH_STACKS[position]
  const defaultStack = availableStacks[0]

  return (
    <section className="motion-safe:animate-fadeIn">
      <p className="font-mono text-[10px] font-black uppercase tracking-[0.2em] text-violet-legacy mb-3">
        Step 2 — Tech Stack
      </p>
      <h1 className="text-3xl font-extrabold tracking-tighter text-text-primary sm:text-4xl">
        기술 스택을 선택해주세요
      </h1>
      <p className="mt-3 text-base font-medium text-text-secondary">
        선택하지 않으면{' '}
        <strong className="text-text-primary">{TECH_STACK_LABELS[defaultStack].label}</strong>
        이 기본 적용됩니다
      </p>

      <div className="mt-10 grid grid-cols-2 gap-3 sm:grid-cols-3">
        {/* TODO(design): variant 판단 보류 — 선택 카드 패턴(active/inactive 조건부 스타일), 사용자 확인 필요 */}
        {availableStacks.map((stack) => {
          const { label, description } = TECH_STACK_LABELS[stack]
          const isSelected = techStack === stack
          const isDefault = stack === defaultStack

          return (
            <button
              key={stack}
              onClick={() => onSelect(isSelected ? null : stack)}
              disabled={isLoading}
              aria-pressed={isSelected}
              className={`flex flex-col items-start gap-1.5 rounded-[20px] p-5 transition-all active:scale-95 ${
                isSelected
                  ? 'bg-violet-legacy text-white shadow-lg shadow-violet-legacy/20'
                  : 'bg-surface text-text-primary hover:bg-slate-200'
              }`}
            >
              <div className="flex w-full items-center justify-between gap-1">
                <span className="text-base font-extrabold leading-tight">{label}</span>
                {isDefault && (
                  <span
                    className={`shrink-0 rounded-full px-2 py-0.5 text-[10px] font-bold ${
                      isSelected
                        ? 'bg-white/20 text-white'
                        : 'bg-violet-legacy/10 text-violet-legacy'
                    }`}
                  >
                    기본
                  </span>
                )}
              </div>
              <span
                className={`text-[11px] font-medium leading-snug ${
                  isSelected ? 'text-white/80' : 'text-text-tertiary'
                }`}
              >
                {description}
              </span>
            </button>
          )
        })}
      </div>

      {!techStack && (
        <p className="mt-6 text-center text-xs text-text-tertiary">
          기본 스택({TECH_STACK_LABELS[defaultStack].label})으로 면접이 진행됩니다
        </p>
      )}
    </section>
  )
}
