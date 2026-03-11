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

// 면접 이벤트 타입 (타임스탬프 기록용)

export type InterviewEventType =
  | 'question_start'
  | 'question_read_tts'
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

export const POSITION_LABELS: Record<Position, { label: string; description: string }> = {
  BACKEND: { label: '백엔드', description: '서버, API, 데이터베이스' },
  FRONTEND: { label: '프론트엔드', description: 'UI/UX, 브라우저, 반응형' },
  DEVOPS: { label: '데브옵스', description: 'CI/CD, 인프라, 클라우드' },
  DATA_ENGINEER: { label: '데이터 엔지니어', description: '파이프라인, ETL, 분석' },
  FULLSTACK: { label: '풀스택', description: '프론트 + 백엔드 통합' },
}

export const LEVEL_LABELS: Record<Level, { label: string; description: string; hint: string }> = {
  JUNIOR: { label: '주니어', description: '0-3년차', hint: '기본 개념 이해도 중심으로 질문합니다' },
  MID: { label: '미드', description: '3-7년차', hint: '실무 적용과 문제 해결 경험을 물어봅니다' },
  SENIOR: { label: '시니어', description: '7년차 이상', hint: '아키텍처 판단력과 기술 리더십을 평가합니다' },
}

export const INTERVIEW_TYPE_LABELS: Record<
  InterviewType,
  { label: string; description: string }
> = {
  CS_FUNDAMENTAL: { label: 'CS 기초', description: '자료구조, 알고리즘, OS, 네트워크, DB' },
  BEHAVIORAL: { label: '경험/협업', description: '팀 협업, 갈등 해결, 프로젝트 경험' },
  RESUME_BASED: { label: '이력서 기반', description: '이력서/포트폴리오 맞춤 질문' },
  JAVA_SPRING: { label: 'Java/Spring', description: 'JVM, Spring IoC/AOP, JPA' },
  FULLSTACK_JS: { label: 'JS 풀스택', description: 'Node.js + React, API 설계, DB 연동, 배포' },
  SYSTEM_DESIGN: { label: '시스템 설계', description: '아키텍처, 스케일링, 트레이드오프' },
  REACT_COMPONENT: { label: 'React/컴포넌트', description: '컴포넌트 설계, 상태 관리, 최적화' },
  BROWSER_PERFORMANCE: { label: '브라우저/웹 성능', description: '렌더링, 번들 최적화, Core Web Vitals' },
  INFRA_CICD: { label: '인프라/CI-CD', description: 'Docker, K8s, 파이프라인' },
  CLOUD: { label: '클라우드', description: 'AWS/GCP/Azure, 서버리스, IaC' },
  DATA_PIPELINE: { label: '데이터 파이프라인', description: 'ETL/ELT, 스트리밍, Spark' },
  SQL_MODELING: { label: 'SQL/모델링', description: '쿼리 최적화, 정규화, ERD' },
}

export const CS_SUB_TOPIC_LABELS: Record<CsSubTopic, string> = {
  DATA_STRUCTURE: '자료구조/알고리즘',
  OS: '운영체제',
  NETWORK: '네트워크',
  DATABASE: '데이터베이스',
}

export const POSITION_INTERVIEW_TYPES: Record<Position, InterviewType[]> = {
  BACKEND: ['CS_FUNDAMENTAL', 'JAVA_SPRING', 'SYSTEM_DESIGN', 'BEHAVIORAL', 'RESUME_BASED'],
  FRONTEND: ['CS_FUNDAMENTAL', 'REACT_COMPONENT', 'BROWSER_PERFORMANCE', 'BEHAVIORAL', 'RESUME_BASED'],
  DEVOPS: ['CS_FUNDAMENTAL', 'INFRA_CICD', 'CLOUD', 'BEHAVIORAL', 'RESUME_BASED'],
  DATA_ENGINEER: ['CS_FUNDAMENTAL', 'DATA_PIPELINE', 'SQL_MODELING', 'BEHAVIORAL', 'RESUME_BASED'],
  FULLSTACK: ['CS_FUNDAMENTAL', 'FULLSTACK_JS', 'REACT_COMPONENT', 'SYSTEM_DESIGN', 'BEHAVIORAL', 'RESUME_BASED'],
}
