export type BookmarkStatus = 'all' | 'in_progress' | 'resolved'

export interface ReviewBookmarkResponse {
  id: number
  timestampFeedbackId: number
  resolvedAt: string | null
  createdAt: string
}

export interface ReviewBookmarkListItem {
  id: number
  timestampFeedbackId: number
  questionText: string | null
  modelAnswer: string | null
  transcript: string | null
  coachingImprovement: string | null
  interviewType: string
  interviewPosition: string
  interviewPositionDetail: string | null
  interviewDate: string
  createdAt: string
  resolvedAt: string | null
}

export interface BookmarkIdPair {
  timestampFeedbackId: number
  bookmarkId: number
}

export interface BookmarkExistsResponse {
  items: BookmarkIdPair[]
}

export interface ReviewBookmarkListResponse {
  items: ReviewBookmarkListItem[]
  total: number
}
