import { create } from 'zustand'
import type {
  Question,
  TranscriptSegment,
  NonVerbalEvent,
  VoiceEvent,
  QuestionAnswer,
  FollowUpResponse,
} from '../types/interview'

export type InterviewPhase = 'preparing' | 'ready' | 'recording' | 'paused' | 'completed'

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
  reset: () => void
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
      phase: 'ready',
      currentQuestionIndex: 0,
    })
  },

  setPhase: (phase) => set({ phase }),

  startRecording: () => {
    const now = Date.now()
    const { currentQuestionIndex, answers } = get()
    const updated = [...answers]
    updated[currentQuestionIndex] = {
      ...updated[currentQuestionIndex],
      startTime: now,
    }
    set({
      phase: 'recording',
      startTime: get().startTime ?? now,
      answers: updated,
    })
  },

  stopRecording: () => {
    const now = Date.now()
    const { currentQuestionIndex, answers } = get()
    const updated = [...answers]
    updated[currentQuestionIndex] = {
      ...updated[currentQuestionIndex],
      endTime: now,
    }
    set({ phase: 'paused', answers: updated })
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
    const { answers, currentQuestionIndex } = get()
    const updated = [...answers]
    updated[currentQuestionIndex] = {
      ...updated[currentQuestionIndex],
      transcripts: [...updated[currentQuestionIndex].transcripts, segment],
    }
    set({ answers: updated, currentTranscript: '' })
  },

  addNonVerbalEvent: (event) => {
    const { nonVerbalEvents, answers, currentQuestionIndex } = get()
    const updated = [...answers]
    updated[currentQuestionIndex] = {
      ...updated[currentQuestionIndex],
      nonVerbalEvents: [...updated[currentQuestionIndex].nonVerbalEvents, event],
    }
    set({
      nonVerbalEvents: [...nonVerbalEvents, event],
      answers: updated,
    })
  },

  addVoiceEvent: (event) => {
    const { voiceEvents, answers, currentQuestionIndex } = get()
    const updated = [...answers]
    updated[currentQuestionIndex] = {
      ...updated[currentQuestionIndex],
      voiceEvents: [...updated[currentQuestionIndex].voiceEvents, event],
    }
    set({
      voiceEvents: [...voiceEvents, event],
      answers: updated,
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

  reset: () => {
    const prevUrl = get().videoBlobUrl
    if (prevUrl) URL.revokeObjectURL(prevUrl)
    set(initialState)
  },
}))
