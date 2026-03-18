import { create } from 'zustand'
import type {
  Question,
  TranscriptSegment,
  QuestionAnswer,
  FollowUpResponse,
  FollowUpExchange,
  InterviewEvent,
  QuestionSetData,
  AnswerTimestamp,
  UploadState,
} from '@/types/interview'

export const MAX_FOLLOWUP_ROUNDS = 3

export type InterviewPhase = 'preparing' | 'greeting' | 'ready' | 'recording' | 'paused' | 'completed'

interface InterviewState {
  interviewId: number | null
  questions: Question[]
  currentQuestionIndex: number
  phase: InterviewPhase

  // 질문세트 상태
  questionSets: QuestionSetData[]
  currentQuestionSetIndex: number
  questionSetAnswers: Map<number, AnswerTimestamp[]>
  uploadStatus: Map<number, UploadState>

  startTime: number | null
  elapsedTime: number

  videoBlob: Blob | null
  videoBlobUrl: string | null

  currentTranscript: string
  answers: QuestionAnswer[]

  followUpHistory: Map<number, FollowUpExchange[]>
  currentFollowUp: FollowUpResponse | null
  followUpRound: number
  followUpTranscriptOffset: number
  isFollowUpLoading: boolean

  questionSetRecordingStartTime: number | null

  greetingCompleted: boolean
  autoTransitionMessage: string | null
  interviewEvents: InterviewEvent[]
}

interface InterviewActions {
  setInterview: (id: number, questions: Question[]) => void
  setPhase: (phase: InterviewPhase) => void
  startRecording: (timestamp?: number) => void
  stopRecording: () => void
  nextQuestion: () => void
  prevQuestion: () => void
  goToQuestion: (index: number) => void

  // 질문세트 액션
  setQuestionSets: (sets: QuestionSetData[]) => void
  nextQuestionSet: () => void
  addAnswerTimestamp: (questionSetId: number, answer: AnswerTimestamp) => void
  setUploadStatus: (questionSetId: number, status: UploadState) => void
  setQuestionSetRecordingStartTime: (time: number) => void
  addQuestionToSet: (setIndex: number, question: QuestionSetData['questions'][number]) => void

  setCurrentTranscript: (text: string) => void
  addTranscript: (segment: TranscriptSegment) => void
  clearTranscripts: (questionIndex: number) => void

  setVideoBlob: (blob: Blob) => void
  setElapsedTime: (time: number) => void
  completeInterview: () => void
  setCurrentFollowUp: (followUp: FollowUpResponse | null) => void
  completeFollowUpRound: (answerText: string) => void
  resetFollowUpState: () => void
  setFollowUpLoading: (loading: boolean) => void
  setAutoTransitionMessage: (msg: string | null) => void
  addInterviewEvent: (event: InterviewEvent) => void
  reset: () => void
}

const updateCurrentAnswer = (
  state: InterviewState,
  updater: (answer: QuestionAnswer) => QuestionAnswer,
): QuestionAnswer[] => {
  const updated = [...state.answers]
  updated[state.currentQuestionIndex] = updater(updated[state.currentQuestionIndex])
  return updated
}

const initialState: InterviewState = {
  interviewId: null,
  questions: [],
  currentQuestionIndex: 0,
  phase: 'preparing',

  questionSets: [],
  currentQuestionSetIndex: 0,
  questionSetAnswers: new Map(),
  uploadStatus: new Map(),

  startTime: null,
  elapsedTime: 0,

  videoBlob: null,
  videoBlobUrl: null,

  currentTranscript: '',
  answers: [],

  followUpHistory: new Map(),
  currentFollowUp: null,
  followUpRound: 0,
  followUpTranscriptOffset: 0,
  isFollowUpLoading: false,

  questionSetRecordingStartTime: null,

  greetingCompleted: false,
  autoTransitionMessage: null,
  interviewEvents: [],
}

export const useInterviewStore = create<InterviewState & InterviewActions>()((set, get) => ({
  ...initialState,

  setInterview: (id, questions) => {
    const answers: QuestionAnswer[] = questions.map((_, index) => ({
      questionIndex: index,
      startTime: 0,
      endTime: 0,
      transcripts: [],
    }))

    set({
      interviewId: id,
      questions,
      answers,
      phase: 'greeting',
      currentQuestionIndex: 0,
    })
  },

  setPhase: (phase) => set({ phase }),

  startRecording: (timestamp?: number) => {
    const now = timestamp ?? Date.now()
    const state = get()
    set({
      phase: 'recording',
      startTime: state.startTime ?? now,
      answers: updateCurrentAnswer(state, (a) => ({ ...a, startTime: now })),
    })
  },

  stopRecording: () => {
    const now = Date.now()
    const state = get()
    set({
      phase: 'paused',
      answers: updateCurrentAnswer(state, (a) => ({ ...a, endTime: now })),
    })
  },

  nextQuestion: () => {
    const { currentQuestionIndex, questions } = get()
    if (currentQuestionIndex < questions.length - 1) {
      set({ currentQuestionIndex: currentQuestionIndex + 1 })
    }
  },

  prevQuestion: () => {
    const { currentQuestionIndex } = get()
    if (currentQuestionIndex > 0) {
      set({ currentQuestionIndex: currentQuestionIndex - 1 })
    }
  },

  goToQuestion: (index) => {
    const { questions } = get()
    if (index >= 0 && index < questions.length) {
      set({ currentQuestionIndex: index })
    }
  },

  setCurrentTranscript: (text) => set({ currentTranscript: text }),

  addTranscript: (segment) => {
    const state = get()
    set({
      answers: updateCurrentAnswer(state, (a) => ({
        ...a,
        transcripts: [...a.transcripts, segment],
      })),
      currentTranscript: '',
    })
  },

  clearTranscripts: (questionIndex) => {
    const state = get()
    const updated = [...state.answers]
    if (updated[questionIndex]) {
      updated[questionIndex] = { ...updated[questionIndex], transcripts: [] }
    }
    set({ answers: updated, currentTranscript: '' })
  },

  setVideoBlob: (blob) => {
    const prevUrl = get().videoBlobUrl
    if (prevUrl) URL.revokeObjectURL(prevUrl)
    set({ videoBlob: blob, videoBlobUrl: URL.createObjectURL(blob) })
  },

  setElapsedTime: (time) => set({ elapsedTime: time }),

  completeInterview: () => set({ phase: 'completed' }),

  setCurrentFollowUp: (followUp) => {
    if (followUp !== null) {
      const state = get()
      const currentAnswer = state.answers[state.currentQuestionIndex]
      const offset = currentAnswer?.transcripts.filter((t) => t.isFinal).length ?? 0
      set({ currentFollowUp: followUp, followUpTranscriptOffset: offset })
    } else {
      set({ currentFollowUp: followUp })
    }
  },

  completeFollowUpRound: (answerText) => {
    const { currentQuestionIndex, currentFollowUp, followUpHistory, followUpRound } = get()
    if (!currentFollowUp) return

    const history = new Map(followUpHistory)
    const existing = history.get(currentQuestionIndex) ?? []
    history.set(currentQuestionIndex, [
      ...existing,
      {
        question: currentFollowUp.question,
        answer: answerText,
        type: currentFollowUp.type,
      },
    ])
    set({
      followUpHistory: history,
      followUpRound: followUpRound + 1,
      currentFollowUp: null,
    })
  },

  resetFollowUpState: () => set({ currentFollowUp: null, followUpRound: 0, followUpTranscriptOffset: 0 }),

  setFollowUpLoading: (loading) => set({ isFollowUpLoading: loading }),

  setQuestionSets: (sets) => set({ questionSets: sets, currentQuestionSetIndex: 0 }),

  nextQuestionSet: () => {
    const { currentQuestionSetIndex, questionSets } = get()
    if (currentQuestionSetIndex < questionSets.length - 1) {
      set({
        currentQuestionSetIndex: currentQuestionSetIndex + 1,
        followUpHistory: new Map(),
        currentFollowUp: null,
        followUpRound: 0,
        followUpTranscriptOffset: 0,
        questionSetRecordingStartTime: null,
      })
    }
  },

  setQuestionSetRecordingStartTime: (time) => set({ questionSetRecordingStartTime: time }),

  addQuestionToSet: (setIndex, question) => {
    const sets = [...get().questionSets]
    sets[setIndex] = { ...sets[setIndex], questions: [...sets[setIndex].questions, question] }
    set({ questionSets: sets })
  },

  addAnswerTimestamp: (questionSetId, answer) => {
    const answers = new Map(get().questionSetAnswers)
    const existing = answers.get(questionSetId) ?? []
    answers.set(questionSetId, [...existing, answer])
    set({ questionSetAnswers: answers })
  },

  setUploadStatus: (questionSetId, status) => {
    const statuses = new Map(get().uploadStatus)
    statuses.set(questionSetId, status)
    set({ uploadStatus: statuses })
  },

  setAutoTransitionMessage: (msg) => set({ autoTransitionMessage: msg }),

  addInterviewEvent: (event) => {
    const { interviewEvents } = get()
    set({ interviewEvents: [...interviewEvents, event] })
  },

  reset: () => {
    const prevUrl = get().videoBlobUrl
    if (prevUrl) URL.revokeObjectURL(prevUrl)
    set(initialState)
  },
}))
