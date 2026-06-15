export interface AdminQuestionPoolItem {
  id: number
  cacheKey: string
  content: string
  ttsContent: string | null
  category: string | null
  bestAnswer: string | null
  isActive: boolean
  createdAt: string
}

export interface AdminQuestionPoolListResponse {
  content: AdminQuestionPoolItem[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export interface AdminQuestionPoolFilters {
  cacheKey: string
  category: string
  isActive: '' | 'true' | 'false'
  keyword: string
}

export interface CreateQuestionPoolRequest {
  cacheKey: string
  content: string
  ttsContent: string
  category: string
  bestAnswer: string
}

export interface UpdateQuestionPoolRequest extends CreateQuestionPoolRequest {
  isActive: boolean
}
