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
  ttsContent: string | null
  category: string
  order: number
}

// 질문세트 관련 타입 (Sprint 0 파이프라인)

// BE com.rehearse.api.domain.question.entity.QuestionType 와 정확히 일치.
// 표준 트랙 (BEHAVIORAL / TECH 계열) 과 RESUME 트랙이 각자 다른 enum 으로 직렬화된다.
export type QuestionType =
  | 'TECH_MAIN'
  | 'TECH_FOLLOWUP'
  | 'BEHAVIORAL_MAIN'
  | 'BEHAVIORAL_FOLLOWUP'
  | 'RESUME_OPENER'
  | 'RESUME_PLAYGROUND'
  | 'RESUME_INTERROGATION'

export type AnalysisStatus = 'PENDING' | 'PENDING_UPLOAD' | 'EXTRACTING' | 'ANALYZING' | 'FINALIZING' | 'COMPLETED' | 'PARTIAL' | 'FAILED' | 'SKIPPED'

export type ConvertStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'

export type FileStatus = 'PENDING' | 'UPLOADED' | 'FAILED'

export interface QuestionDetail {
  id: number
  questionType: QuestionType
  questionText: string
  ttsText?: string | null
  bestAnswer: string | null
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
  convertStatus: ConvertStatus | null
  fileStatus: FileStatus | null
  isVerbalCompleted: boolean
  isNonverbalCompleted: boolean
  fullyReady: boolean
  failureReason: string | null
}

export interface QuestionWithAnswer {
  questionId: number
  questionType: string
  questionText: string
  bestAnswer: string | null
  startMs: number | null
  endMs: number | null
}

export interface QuestionsWithAnswersResponse {
  questions: QuestionWithAnswer[]
}

// 피드백 뷰어 타입 (Sprint 0 Task 10)

export type FeedbackLevel = 'GOOD' | 'AVERAGE' | 'NEEDS_IMPROVEMENT'

export interface CommentBlock {
  positive: string | null
  negative: string | null
  suggestion: string | null
}

export const isCommentBlockEmpty = (block: CommentBlock | null | undefined): boolean => {
  if (block === null || block === undefined) return true
  const fields = [block.positive, block.negative, block.suggestion]
  return fields.every((v) => v === null || v === undefined || v.trim().length === 0)
}

export interface NonverbalFeedback {
  eyeContactLevel: FeedbackLevel | null
  postureLevel: FeedbackLevel | null
  expressionLabel: string | null
  nonverbalComment: CommentBlock | null
}

export interface VocalFeedback {
  fillerWords: string | null
  fillerWordCount: number | null
  speechPace: string | null
  toneConfidenceLevel: FeedbackLevel | null
  emotionLabel: string | null
  vocalComment: CommentBlock | null
}

export interface DeliveryFeedback {
  nonverbal: NonverbalFeedback | null
  vocal: VocalFeedback | null
  attitudeComment: CommentBlock | null
}

export interface TechnicalDimensionFeedback {
  dimension: string
  score: number | null
  observation: string | null
  evidenceQuote: string | null
}

export type RubricCategory = 'TECHNICAL' | 'EXPERIENCE' | 'BEHAVIORAL'

export interface TechnicalFeedback {
  rubricCategory: RubricCategory | null
  rubricId: string
  levelFlag: string | null
  dimensions: TechnicalDimensionFeedback[]
}

export interface TimestampFeedback {
  id: number
  questionId: number | null
  questionType: string | null
  questionText: string | null
  bestAnswer: string | null
  startMs: number
  endMs: number
  transcript: string | null
  delivery: DeliveryFeedback | null
  technicalFeedback: TechnicalFeedback | null
  overallComment: CommentBlock | null
  isAnalyzed: boolean
}

export interface QuestionSetFeedbackResponse {
  id: number
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

export type FollowUpType =
  | 'DEEP_DIVE'
  | 'CLARIFICATION'
  | 'CHALLENGE'
  | 'APPLICATION'

export interface FollowUpExchange {
  question: string
  answerText: string
  type: FollowUpType
  followUpType?: FollowUpType
}

export interface FollowUpRequest {
  questionSetId: number
  questionContent: string
  answerText?: string
  previousExchanges?: Array<{ question: string; answerText: string; followUpType?: FollowUpType }>
  // 사용자 시간 만료 후 답변 완료 시점에 면접 종료 의사 신호 — BE 가 followUpExhausted=true 응답
  terminate?: boolean
}

export interface FollowUpResponse {
  questionId: number
  question: string
  ttsQuestion?: string | null
  reason: string
  type: FollowUpType
  answerText?: string
  bestAnswer?: string | null
  skip: boolean
  skipReason?: string | null
  followUpExhausted?: boolean
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

// 대시보드 목록/통계 타입

export interface InterviewListItem {
  id: number
  publicId: string
  position: Position
  positionDetail: string | null
  interviewTypes: InterviewType[]
  csSubTopics: string[]
  durationMinutes: number
  answerCount: number
  status: InterviewStatus
  createdAt: string
}

export interface InterviewListResponse {
  content: InterviewListItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface InterviewStats {
  totalCount: number
  completedCount: number
  thisWeekCount: number
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
