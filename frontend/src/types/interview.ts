export type Position = 'BACKEND' | 'FRONTEND' | 'DEVOPS' | 'DATA_ENGINEER' | 'FULLSTACK'

export type TechStack =
  | 'JAVA_SPRING'
  | 'PYTHON_DJANGO'
  | 'NODE_NESTJS'
  | 'GO'
  | 'KOTLIN_SPRING'
  | 'REACT_TS'
  | 'VUE_TS'
  | 'SVELTE'
  | 'ANGULAR'
  | 'AWS_K8S'
  | 'GCP'
  | 'AZURE'
  | 'SPARK_AIRFLOW'
  | 'FLINK'
  | 'DBT_SNOWFLAKE'
  | 'REACT_SPRING'
  | 'REACT_NODE'
  | 'NEXTJS_FULLSTACK'

export type Level = 'JUNIOR' | 'MID' | 'SENIOR'

export type InterviewType =
  | 'CS_FUNDAMENTAL'
  | 'BEHAVIORAL'
  | 'RESUME_BASED'
  | 'LANGUAGE_FRAMEWORK'
  | 'FULLSTACK_STACK'
  | 'SYSTEM_DESIGN'
  | 'UI_FRAMEWORK'
  | 'BROWSER_PERFORMANCE'
  | 'INFRA_CICD'
  | 'CLOUD'
  | 'DATA_PIPELINE'
  | 'SQL_MODELING'

export type CsSubTopic = 'DATA_STRUCTURE' | 'OS' | 'NETWORK' | 'DATABASE'

export type InterviewStatus = 'READY' | 'IN_PROGRESS' | 'COMPLETED'

export type QuestionGenerationStatus = 'PENDING' | 'GENERATING' | 'COMPLETED' | 'FAILED'

export interface Question {
  id: number
  content: string
  category: string
  order: number
}

// 질문세트 관련 타입 (Sprint 0 파이프라인)

export type QuestionType = 'MAIN' | 'FOLLOWUP'

export type ReferenceType = 'RESUME' | 'CS' | 'TECH' | 'BEHAVIORAL' | 'SYSTEM_DESIGN'

export type AnalysisStatus = 'PENDING' | 'PENDING_UPLOAD' | 'ANALYZING' | 'COMPLETED' | 'FAILED' | 'SKIPPED'

export type FileStatus = 'PENDING' | 'UPLOADED' | 'CONVERTING' | 'CONVERTED' | 'FAILED'

export interface QuestionDetail {
  id: number
  questionType: QuestionType
  questionText: string
  modelAnswer: string | null
  referenceType: ReferenceType
  orderIndex: number
}

export interface QuestionSetData {
  id: number
  category: string
  orderIndex: number
  analysisStatus: AnalysisStatus
  failureReason: string | null
  questions: QuestionDetail[]
}

export interface AnswerTimestamp {
  questionId: number
  startMs: number
  endMs: number
}

export interface UploadUrlRequest {
  contentType: string
}

export interface UploadUrlResponse {
  uploadUrl: string
  s3Key: string
  fileMetadataId: number
}

export interface SaveAnswersRequest {
  answers: AnswerTimestamp[]
}

export interface QuestionSetStatusResponse {
  id: number
  analysisStatus: AnalysisStatus
  analysisProgress: string | null
  fileStatus: FileStatus | null
  failureReason: string | null
}

export type AnalysisProgress =
  | 'STARTED'
  | 'EXTRACTING'
  | 'STT_PROCESSING'
  | 'VERBAL_ANALYZING'
  | 'NONVERBAL_ANALYZING'
  | 'FINALIZING'
  | 'FAILED'

export interface QuestionWithAnswer {
  questionId: number
  questionType: string
  questionText: string
  modelAnswer: string | null
  startMs: number | null
  endMs: number | null
}

export interface QuestionsWithAnswersResponse {
  questions: QuestionWithAnswer[]
}

// 피드백 뷰어 타입 (Sprint 0 Task 10)

export interface TimestampFeedback {
  id: number
  questionId: number | null
  questionType: string | null
  startMs: number
  endMs: number
  transcript: string | null
  verbalScore: number | null
  verbalComment: string | null
  fillerWordCount: number | null
  eyeContactScore: number | null
  postureScore: number | null
  expressionLabel: string | null
  nonverbalComment: string | null
  overallComment: string | null
  isAnalyzed: boolean
}

export interface QuestionSetFeedbackResponse {
  id: number
  questionSetScore: number
  questionSetComment: string
  streamingUrl: string | null
  fallbackUrl: string | null
  timestampFeedbacks: TimestampFeedback[]
}

export type UploadState = 'pending' | 'uploading' | 'completed' | 'failed'

export interface InterviewSession {
  id: number
  publicId: string
  position: Position
  positionDetail?: string | null
  level: Level
  interviewTypes: InterviewType[]
  csSubTopics?: CsSubTopic[]
  techStack?: TechStack | null
  durationMinutes: number
  status: InterviewStatus
  questionGenerationStatus: QuestionGenerationStatus
  failureReason?: string | null
  questionSets: QuestionSetData[]
  createdAt: string
}

export interface CreateInterviewRequest {
  position: Position
  positionDetail?: string
  level: Level
  interviewTypes: InterviewType[]
  durationMinutes: number
  csSubTopics?: CsSubTopic[]
  techStack?: TechStack
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

// 후속 질문 관련 타입

export type FollowUpType = 'DEEP_DIVE' | 'CLARIFICATION' | 'CHALLENGE' | 'APPLICATION'

export interface FollowUpExchange {
  question: string
  answer: string
  type: FollowUpType
}

export interface FollowUpRequest {
  questionSetId: number
  questionContent: string
  answerText?: string
  previousExchanges?: Array<{ question: string; answer: string }>
}

export interface FollowUpResponse {
  questionId: number
  question: string
  reason: string
  type: FollowUpType
  answerText?: string
  modelAnswer?: string | null
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
