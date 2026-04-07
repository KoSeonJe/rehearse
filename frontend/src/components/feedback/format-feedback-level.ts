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
