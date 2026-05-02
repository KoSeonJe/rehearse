import { apiClient } from '@/lib/api-client'
import type { ApiResponse } from '@/types/interview'
import type { SessionFeedbackData } from '@/types/session-feedback'

export const getSessionFeedback = async (interviewId: number): Promise<SessionFeedbackData> => {
  const res = await apiClient.get<ApiResponse<SessionFeedbackData>>(
    `/api/v1/interviews/${interviewId}/session-feedback`,
  )
  return res.data
}
