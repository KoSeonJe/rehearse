import { create } from 'zustand'
import type { TimestampFeedback } from '../types/interview'

interface ReviewState {
  currentTime: number
  feedbacks: TimestampFeedback[]
  selectedFeedbackId: number | null
  isPlaying: boolean
}

interface ReviewActions {
  setCurrentTime: (time: number) => void
  setFeedbacks: (feedbacks: TimestampFeedback[]) => void
  selectFeedback: (id: number | null) => void
  setIsPlaying: (playing: boolean) => void
  reset: () => void
}

const initialState: ReviewState = {
  currentTime: 0,
  feedbacks: [],
  selectedFeedbackId: null,
  isPlaying: false,
}

export const useReviewStore = create<ReviewState & ReviewActions>()((set) => ({
  ...initialState,

  setCurrentTime: (time) => set({ currentTime: time }),

  setFeedbacks: (feedbacks) => set({ feedbacks }),

  selectFeedback: (id) => set({ selectedFeedbackId: id }),

  setIsPlaying: (playing) => set({ isPlaying: playing }),

  reset: () => set(initialState),
}))
