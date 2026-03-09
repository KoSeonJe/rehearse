export type Level = 'JUNIOR' | 'MID' | 'SENIOR'

export type InterviewType = 'CS' | 'SYSTEM_DESIGN' | 'BEHAVIORAL'

export type InterviewStatus = 'READY' | 'IN_PROGRESS' | 'COMPLETED'

export interface Question {
  id: number
  content: string
  category: string
  order: number
}

export interface InterviewSession {
  id: number
  position: string
  level: Level
  interviewType: InterviewType
  status: InterviewStatus
  questions: Question[]
  createdAt: string
}

export interface CreateInterviewRequest {
  position: string
  level: Level
  interviewType: InterviewType
}

export interface UpdateInterviewStatusRequest {
  status: 'IN_PROGRESS' | 'COMPLETED'
}

export interface UpdateInterviewStatusResponse {
  id: number
  status: InterviewStatus
}

export interface ApiResponse<T> {
  success: boolean
  data: T
  message: string | null
}

export interface ApiErrorResponse {
  success: false
  status: number
  code: string
  message: string
  errors: Array<{ field: string; value: string; reason: string }>
  timestamp: string
}

export interface TimestampFeedback {
  id: number
  timestampSeconds: number
  category: 'VERBAL' | 'NON_VERBAL' | 'CONTENT'
  severity: 'INFO' | 'WARNING' | 'SUGGESTION'
  content: string
  suggestion: string | null
}

export interface FeedbackListResponse {
  interviewId: number
  feedbacks: TimestampFeedback[]
  totalCount: number
}

export interface AnswerData {
  questionIndex: number
  questionContent: string
  answerText: string
  nonVerbalSummary?: string
  voiceSummary?: string
}

export interface GenerateFeedbackRequest {
  answers: AnswerData[]
}

export interface InterviewReport {
  id: number
  interviewId: number
  overallScore: number
  summary: string
  strengths: string[]
  improvements: string[]
  feedbackCount: number
}

// 후속 질문 관련 타입

export type FollowUpType = 'DEEP_DIVE' | 'CLARIFICATION' | 'CHALLENGE' | 'APPLICATION'

export interface FollowUpRequest {
  questionContent: string
  answerText: string
  nonVerbalSummary?: string
}

export interface FollowUpResponse {
  question: string
  reason: string
  type: FollowUpType
}

// 면접 진행 관련 타입

export type NonVerbalEventType = 'gaze' | 'expression' | 'posture' | 'voice'
export type Severity = 'high' | 'medium' | 'low'

export interface NonVerbalEvent {
  timestamp: number
  type: NonVerbalEventType
  severity: Severity
  data: {
    description: string
    value?: number
    duration?: number
  }
}

export interface TranscriptSegment {
  questionIndex: number
  text: string
  startTime: number
  endTime: number
  isFinal: boolean
}

export interface VoiceEvent {
  timestamp: number
  type: 'silence' | 'fast_speech' | 'low_volume' | 'high_volume'
  duration: number
  value?: number
}

export interface QuestionAnswer {
  questionIndex: number
  startTime: number
  endTime: number
  transcripts: TranscriptSegment[]
  nonVerbalEvents: NonVerbalEvent[]
  voiceEvents: VoiceEvent[]
}

export const LEVEL_LABELS: Record<Level, { label: string; description: string }> = {
  JUNIOR: { label: '주니어', description: '0-3년차' },
  MID: { label: '미드', description: '3-7년차' },
  SENIOR: { label: '시니어', description: '7년차 이상' },
}

export const INTERVIEW_TYPE_LABELS: Record<
  InterviewType,
  { label: string; description: string }
> = {
  CS: { label: 'CS 기초', description: '자료구조, 알고리즘, OS, 네트워크, DB' },
  SYSTEM_DESIGN: {
    label: '시스템 설계',
    description: '아키텍처, 스케일링, 트레이드오프',
  },
  BEHAVIORAL: {
    label: 'Behavioral',
    description: '경험, 협업, 문제 해결 (STAR)',
  },
}
