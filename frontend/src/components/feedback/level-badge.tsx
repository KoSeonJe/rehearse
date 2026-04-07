import type { FeedbackLevel } from '@/types/interview'

const LEVEL_LABELS: Record<FeedbackLevel, string> = {
  GOOD: '좋음',
  AVERAGE: '보통',
  NEEDS_IMPROVEMENT: '개선 필요',
}

export const formatFeedbackLevel = (level: FeedbackLevel | null): string => {
  if (level === null) return '—'
  return LEVEL_LABELS[level]
}

interface LevelBadgeProps {
  label: string
  value: string | null
  bg?: 'white' | 'gray'
}

const LevelBadge = ({ label, value, bg = 'white' }: LevelBadgeProps) => {
  const bgClass = bg === 'white' ? 'bg-white' : 'bg-gray-50'
  return (
    <div className={`${bgClass} rounded-xl p-3 text-center`}>
      <p className="text-[12px] text-gray-400 mb-1">{label}</p>
      <p className="text-[15px] font-bold text-gray-900">{value ?? '—'}</p>
    </div>
  )
}

export default LevelBadge
