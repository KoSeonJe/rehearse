import type { FeedbackLevel } from '@/types/interview'

const LEVEL_STYLES: Record<FeedbackLevel, string> = {
  GOOD: 'bg-green-50 text-green-600',
  AVERAGE: 'bg-yellow-50 text-yellow-600',
  NEEDS_IMPROVEMENT: 'bg-red-50 text-red-600',
}

const LEVEL_LABELS: Record<FeedbackLevel, string> = {
  GOOD: '좋음',
  AVERAGE: '보통',
  NEEDS_IMPROVEMENT: '개선 필요',
}

interface LevelBadgeProps {
  label: string
  level: FeedbackLevel | null
  subtitle?: string
}

const LevelBadge = ({ label, level, subtitle }: LevelBadgeProps) => {
  if (level === null) return null

  return (
    <div className="flex items-center gap-1.5">
      <span className="text-xs font-semibold text-text-tertiary">{label}</span>
      <span className={`rounded-full px-2.5 py-1 text-xs font-bold ${LEVEL_STYLES[level]}`}>
        {LEVEL_LABELS[level]}
      </span>
      {subtitle && (
        <span className="text-[10px] text-text-tertiary">({subtitle})</span>
      )}
    </div>
  )
}

export default LevelBadge
