import { LEVELS } from '@/constants/setup'
import { LEVEL_LABELS } from '@/constants/interview-labels'
import type { Level } from '@/types/interview'

interface StepLevelProps {
  level: Level | null
  isLoading: boolean
  onSelect: (l: Level) => void
}

export const StepLevel = ({ level, isLoading, onSelect }: StepLevelProps) => {
  return (
    <section className="motion-safe:animate-fadeIn">
      <h1 className="text-3xl font-extrabold tracking-tighter text-text-primary sm:text-4xl">
        경력 수준을 선택해주세요
      </h1>
      <p className="mt-3 text-base font-medium text-text-secondary">
        레벨에 따라 질문 난이도가 달라집니다
      </p>

      {/* TODO(design): variant 판단 보류 — 선택 카드 패턴(active/inactive 조건부 스타일), 사용자 확인 필요 */}
      <div className="mt-10 space-y-3">
        {LEVELS.map((l) => (
          <button
            key={l}
            onClick={() => onSelect(l)}
            disabled={isLoading}
            className={`flex w-full items-center justify-between rounded-2xl p-6 text-left transition-colors active:scale-[0.98] ${
              level === l
                ? 'bg-primary text-primary-foreground shadow-lg shadow-primary/20'
                : 'bg-surface text-text-primary hover:bg-slate-200'
            }`}
          >
            <div className="flex flex-col gap-1">
              <div className="flex items-center gap-2">
                <span className="text-lg font-extrabold">{LEVEL_LABELS[l].label}</span>
                <span
                  className={`text-xs font-bold ${
                    level === l ? 'text-white/60' : 'text-text-tertiary'
                  }`}
                >
                  {LEVEL_LABELS[l].description}
                </span>
              </div>
              <span
                className={`text-[13px] font-medium ${
                  level === l ? 'text-white/80' : 'text-text-secondary'
                }`}
              >
                {LEVEL_LABELS[l].hint}
              </span>
            </div>
            {level === l && (
              <div className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-white/20 text-white">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z" />
                </svg>
              </div>
            )}
          </button>
        ))}
      </div>
    </section>
  )
}
