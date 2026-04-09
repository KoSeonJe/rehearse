export type FeedbackSource = 'AUTO_POPUP' | 'VOLUNTARY'

export interface CreateServiceFeedbackRequest {
  content: string
  rating?: number
  source: FeedbackSource
}

export interface FeedbackNeedCheckResponse {
  needsFeedback: boolean
}

export interface AdminFeedbackItem {
  id: number
  userId: number
  userName: string
  userEmail: string
  content: string
  rating: number | null
  source: FeedbackSource
  completedCountSnapshot: number
  createdAt: string
}

export interface AdminFeedbackListResponse {
  content: AdminFeedbackItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
