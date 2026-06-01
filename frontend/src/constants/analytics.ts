export const ANALYTICS_EVENT = {
  LOGIN: 'login',
  INTERVIEW_CREATE: 'interview_create',
  INTERVIEW_START: 'interview_start',
  FEEDBACK_VIEWED: 'feedback_viewed',
} as const

export type AnalyticsEvent = (typeof ANALYTICS_EVENT)[keyof typeof ANALYTICS_EVENT]

export const FEEDBACK_VIEWED_TRACKED_PREFIX = 'rehearse:ga-feedback-viewed:'
