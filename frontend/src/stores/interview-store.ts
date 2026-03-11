import { create } from 'zustand'
import type {
  Question,
  TranscriptSegment,
  NonVerbalEvent,
  VoiceEvent,
  QuestionAnswer,
  FollowUpResponse,
  InterviewEvent,
} from '../types/interview'

export type InterviewPhase = 'preparing' | 'greeting' | 'ready' | 'recording' | 'paused' | 'completed'

interface InterviewState {
  interviewId: number | null
  questions: Question[]
  currentQuestionIndex: number
  phase: InterviewPhase

  startTime: number | null
  elapsedTime: number

  videoBlob: Blob | null
  videoBlobUrl: string | null

  currentTranscript: string
  answers: QuestionAnswer[]

  nonVerbalEvents: NonVerbalEvent[]
  voiceEvents: VoiceEvent[]

  followUpQuestions: Map<number, FollowUpResponse>
  isFollowUpLoading: boolean

  autoTransitionMessage: string | null
  interviewEvents: InterviewEvent[]
}

interface InterviewActions {
  setInterview: (id: number, questions: Question[]) => void
  setPhase: (phase: InterviewPhase) => void
  startRecording: () => void
  stopRecording: () => void
  nextQuestion: () => void
  prevQuestion: () => void
  goToQuestion: (index: number) => void

  setCurrentTranscript: (text: string) => void
  addTranscript: (segment: TranscriptSegment) => void
  addNonVerbalEvent: (event: NonVerbalEvent) => void
  addVoiceEvent: (event: VoiceEvent) => void

  setVideoBlob: (blob: Blob) => void
  setElapsedTime: (time: number) => void
  completeInterview: () => void
  addFollowUpQuestion: (questionIndex: number, followUp: FollowUpResponse) => void
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

  startTime: null,
  elapsedTime: 0,

  videoBlob: null,
  videoBlobUrl: null,

  currentTranscript: '',
  answers: [],

  nonVerbalEvents: [],
  voiceEvents: [],

  followUpQuestions: new Map(),
  isFollowUpLoading: false,

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
      nonVerbalEvents: [],
      voiceEvents: [],
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

  startRecording: () => {
    const now = Date.now()
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

  addNonVerbalEvent: (event) => {
    const state = get()
    set({
      nonVerbalEvents: [...state.nonVerbalEvents, event],
      answers: updateCurrentAnswer(state, (a) => ({
        ...a,
        nonVerbalEvents: [...a.nonVerbalEvents, event],
      })),
    })
  },

  addVoiceEvent: (event) => {
    const state = get()
    set({
      voiceEvents: [...state.voiceEvents, event],
      answers: updateCurrentAnswer(state, (a) => ({
        ...a,
        voiceEvents: [...a.voiceEvents, event],
      })),
    })
  },

  setVideoBlob: (blob) => {
    const prevUrl = get().videoBlobUrl
    if (prevUrl) URL.revokeObjectURL(prevUrl)
    set({ videoBlob: blob, videoBlobUrl: URL.createObjectURL(blob) })
  },

  setElapsedTime: (time) => set({ elapsedTime: time }),

  completeInterview: () => set({ phase: 'completed' }),

  addFollowUpQuestion: (questionIndex, followUp) => {
    const { followUpQuestions } = get()
    const updated = new Map(followUpQuestions)
    updated.set(questionIndex, followUp)
    set({ followUpQuestions: updated })
  },

  setFollowUpLoading: (loading) => set({ isFollowUpLoading: loading }),

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
