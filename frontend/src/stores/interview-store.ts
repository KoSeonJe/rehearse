import { create } from 'zustand'

interface InterviewState {
  isRecording: boolean
  currentQuestionIndex: number
  setRecording: (isRecording: boolean) => void
  setCurrentQuestionIndex: (index: number) => void
  reset: () => void
}

const initialState = {
  isRecording: false,
  currentQuestionIndex: 0,
}

export const useInterviewStore = create<InterviewState>((set) => ({
  ...initialState,
  setRecording: (isRecording) => set({ isRecording }),
  setCurrentQuestionIndex: (index) => set({ currentQuestionIndex: index }),
  reset: () => set(initialState),
}))
