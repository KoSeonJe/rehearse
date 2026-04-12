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

export type QuestionType = 'MAIN' | 'FOLLOWUP'

export type ReferenceType = 'RESUME' | 'CS' | 'TECH' | 'BEHAVIORAL' | 'SYSTEM_DESIGN'

export type AnalysisStatus = 'PENDING' | 'PENDING_UPLOAD' | 'EXTRACTING' | 'ANALYZING' | 'FINALIZING' | 'COMPLETED' | 'PARTIAL' | 'FAILED' | 'SKIPPED'

export type ConvertStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'

export type FileStatus = 'PENDING' | 'UPLOADED' | 'FAILED'

export interface QuestionDetail {
  id: number
  questionType: QuestionType
  questionText: string
  ttsText: string | null
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
  modelAnswer: string | null
  startMs: number | null
  endMs: number | null
}

export interface QuestionsWithAnswersResponse {
  questions: QuestionWithAnswer[]
}

// 피드백 뷰어 타입 (Sprint 0 Task 10)

export type FeedbackLevel = 'GOOD' | 'AVERAGE' | 'NEEDS_IMPROVEMENT'

export interface AccuracyIssue {
  claim: string
  correction: string
}

export interface CoachingResponse {
  structure: string | null
  improvement: string | null
}

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

export interface ContentFeedback {
  verbalComment: CommentBlock | null
  accuracyIssues: AccuracyIssue[]
  coaching: CoachingResponse | null
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

export interface TimestampFeedback {
  id: number
  questionId: number | null
  questionType: string | null
  questionText: string | null
  modelAnswer: string | null
  startMs: number
  endMs: number
  transcript: string | null
  content: ContentFeedback | null
  delivery: DeliveryFeedback | null
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
  ttsQuestion: string | null
  reason: string
  type: FollowUpType
  answerText?: string
  modelAnswer?: string | null
  /**
   * AI가 답변 불충분("모르겠다", 공백 등)으로 꼬리질문 생성을 포기한 경우 true.
   * skip=true일 때 BE는 questionId/question/reason/type/modelAnswer를 null로 내려보내지만,
   * 이 경우 클라이언트는 해당 응답을 store에 저장하지 않고 즉시 다음 메인 질문으로 진행해야 한다.
   * 따라서 위 필드들은 skip=false 케이스 기준으로 non-null로 선언되어 있으며, skip 분기 이후에는 접근하지 말 것.
   */
  skip: boolean
  skipReason?: string | null
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
