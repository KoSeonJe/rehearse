export type SessionFeedbackStatus = 'PRELIMINARY' | 'COMPLETE'

export interface SessionFeedbackOverall {
  dimensionScores: Record<string, number> | null
  levelAssessment: string | null
  narrative: string | null
  coverage: string | null
}

export interface SessionFeedbackStrength {
  dimension: string
  observation: string
  whyMatters: string | null
}

export interface SessionFeedbackGap {
  dimension: string
  observation: string
  levelGap: string | null
  concreteAction: string | null
}

export interface SessionFeedbackDelivery {
  fillerWords: string | null
  tonePattern: string | null
  action: string | null
}

export interface SessionFeedbackWeekPlan {
  priority: number
  topic: string
  resources: string[]
  practice: string | null
}

export interface SessionFeedbackData {
  id: number
  interviewId: number
  status: SessionFeedbackStatus
  overall: SessionFeedbackOverall | null
  strengths: SessionFeedbackStrength[] | null
  gaps: SessionFeedbackGap[] | null
  delivery: SessionFeedbackDelivery | null
  weekPlan: SessionFeedbackWeekPlan[] | null
  coverage: string | null
  deliveryRetryable: boolean
  lastFailureReason: string | null
  retryAttempts: number
  createdAt: string
  updatedAt: string
}
