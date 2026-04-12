import { apiClient } from '@/lib/api-client'
import type { ApiResponse } from '@/types/interview'
import type {
  BookmarkExistsResponse,
  BookmarkStatus,
  ReviewBookmarkListResponse,
  ReviewBookmarkResponse,
} from '@/types/review-bookmark'

export const createBookmark = async (
  timestampFeedbackId: number,
): Promise<ReviewBookmarkResponse> => {
  const res = await apiClient.post<ApiResponse<ReviewBookmarkResponse>>(
    '/api/review-bookmarks',
    { timestampFeedbackId },
  )
  return res.data
}

export const deleteBookmark = async (bookmarkId: number): Promise<void> => {
  await apiClient.delete<void>(`/api/review-bookmarks/${bookmarkId}`)
}

export const listBookmarks = async (
  status: BookmarkStatus,
): Promise<ReviewBookmarkListResponse> => {
  const res = await apiClient.get<ApiResponse<ReviewBookmarkListResponse>>(
    `/api/review-bookmarks?status=${status}`,
  )
  return res.data
}

export const updateBookmarkStatus = async (
  id: number,
  resolved: boolean,
): Promise<ReviewBookmarkResponse> => {
  const res = await apiClient.patch<ApiResponse<ReviewBookmarkResponse>>(
    `/api/review-bookmarks/${id}/status`,
    { resolved },
  )
  return res.data
}

export const findBookmarkExists = async (
  tsfIds: number[],
): Promise<BookmarkExistsResponse> => {
  const params = tsfIds.map((id) => `timestampFeedbackIds=${id}`).join('&')
  const res = await apiClient.get<ApiResponse<BookmarkExistsResponse>>(
    `/api/review-bookmarks/exists?${params}`,
  )
  return res.data
}
