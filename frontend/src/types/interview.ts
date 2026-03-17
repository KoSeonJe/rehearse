export type Position = 'BACKEND' | 'FRONTEND' | 'DEVOPS' | 'DATA_ENGINEER' | 'FULLSTACK'

export type Level = 'JUNIOR' | 'MID' | 'SENIOR'

export type InterviewType =
  | 'CS_FUNDAMENTAL'
  | 'BEHAVIORAL'
  | 'RESUME_BASED'
  | 'JAVA_SPRING'
  | 'FULLSTACK_JS'
  | 'SYSTEM_DESIGN'
  | 'REACT_COMPONENT'
  | 'BROWSER_PERFORMANCE'
  | 'INFRA_CICD'
  | 'CLOUD'
  | 'DATA_PIPELINE'
  | 'SQL_MODELING'

export type CsSubTopic = 'DATA_STRUCTURE' | 'OS' | 'NETWORK' | 'DATABASE'

export type InterviewStatus = 'READY' | 'IN_PROGRESS' | 'COMPLETED'

export interface Question {
  id: number
  content: string
  category: string
  order: number
}

export interface InterviewSession {
  id: number
  position: Position
  positionDetail?: string | null
  level: Level
  interviewTypes: InterviewType[]
  csSubTopics?: CsSubTopic[]
  durationMinutes: number
  status: InterviewStatus
  questions: Question[]
  createdAt: string
}

export interface CreateInterviewRequest {
  position: Position
  positionDetail?: string
  level: Level
  interviewTypes: InterviewType[]
  durationMinutes: number
  csSubTopics?: CsSubTopic[]
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

export interface FollowUpExchange {
  question: string
  answer: string
  type: FollowUpType
}

export interface FollowUpRequest {
  questionContent: string
  answerText: string
  previousExchanges?: Array<{ question: string; answer: string }>
}

export interface FollowUpResponse {
  question: string
  reason: string
  type: FollowUpType
}

// 면접 진행 관련 타입

export interface TranscriptSegment {
  questionIndex: number
  text: string
  startTime: number
  endTime: number
  isFinal: boolean
}

export interface QuestionAnswer {
  questionIndex: number
  startTime: number
  endTime: number
  transcripts: TranscriptSegment[]
}

// 면접 이벤트 타입 (타임스탬프 기록용)

export type InterviewEventType =
  | 'question_start'
  | 'question_read_tts'
  | 'greeting_tts'
  | 'answer_start'
  | 'answer_end'
  | 'thinking_time_requested'
  | 'silence_detected'
  | 'auto_transition'
  | 'manual_stop'
  | 'interview_finish'

export interface InterviewEvent {
  type: InterviewEventType
  elapsedMs: number
  questionIndex: number
  metadata?: Record<string, unknown>
}
