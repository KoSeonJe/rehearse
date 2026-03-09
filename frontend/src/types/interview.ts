export interface Question {
  id: string
  content: string
  category: string
  order: number
}

export interface InterviewSession {
  id: string
  status: 'READY' | 'IN_PROGRESS' | 'COMPLETED'
  questions: Question[]
  createdAt: string
}

export interface TimestampFeedback {
  id: string
  timestampSeconds: number
  category: 'VERBAL' | 'NON_VERBAL' | 'CONTENT'
  content: string
  severity: 'INFO' | 'WARNING' | 'SUGGESTION'
}

export interface InterviewReport {
  id: string
  sessionId: string
  overallScore: number
  feedbacks: TimestampFeedback[]
  summary: string
  improvements: string[]
}
