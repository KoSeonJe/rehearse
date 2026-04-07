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

const EXPRESSION_LABELS: Record<string, string> = {
  CONFIDENT: '자신감',
  ENGAGED: '몰입',
  NEUTRAL: '평온',
  NERVOUS: '긴장',
  UNCERTAIN: '혼란',
}

export const formatExpressionLabel = (label: string | null): string => {
  if (label === null) return '—'
  return EXPRESSION_LABELS[label] ?? label
}
