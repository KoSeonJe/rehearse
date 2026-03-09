import { useQuery, useMutation } from '@tanstack/react-query'
import { apiClient } from '@/lib/api-client'
import type {
  ApiResponse,
  FeedbackListResponse,
  GenerateFeedbackRequest,
} from '@/types/interview'

export const useGenerateFeedback = () => {
  return useMutation({
    mutationFn: ({
      interviewId,
      data,
    }: {
      interviewId: number
      data: GenerateFeedbackRequest
    }) =>
      apiClient.post<ApiResponse<FeedbackListResponse>>(
        `/api/v1/interviews/${interviewId}/feedbacks`,
        data,
      ),
  })
}

export const useFeedbacks = (interviewId: string) => {
  return useQuery({
    queryKey: ['feedbacks', interviewId],
    queryFn: () =>
      apiClient.get<ApiResponse<FeedbackListResponse>>(
        `/api/v1/interviews/${interviewId}/feedbacks`,
      ),
    enabled: !!interviewId,
  })
}
