import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '@/lib/api-client'
import type { ApiResponse } from '@/types/interview'
import type {
  CreateServiceFeedbackRequest,
  FeedbackNeedCheckResponse,
  AdminFeedbackListResponse,
} from '@/types/service-feedback'

export const useFeedbackNeedCheck = () => {
  return useQuery({
    queryKey: ['feedback-need-check'],
    queryFn: () =>
      apiClient.get<ApiResponse<FeedbackNeedCheckResponse>>(
        '/api/v1/service-feedbacks/need-check',
      ),
    staleTime: 5 * 60 * 1000,
  })
}

export const useSubmitServiceFeedback = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: CreateServiceFeedbackRequest) =>
      apiClient.post<ApiResponse<void>>('/api/v1/service-feedbacks', data),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['feedback-need-check'] })
    },
  })
}

export const useAdminFeedbacks = (page: number, size: number) => {
  return useQuery({
    queryKey: ['admin-feedbacks', page, size],
    queryFn: () =>
      apiClient.get<ApiResponse<AdminFeedbackListResponse>>(
        `/api/v1/admin/feedbacks?page=${page}&size=${size}`,
        { headers: { 'X-Admin-Password': sessionStorage.getItem('admin-password') ?? '' } },
      ),
  })
}

export const useVerifyAdminPassword = () => {
  return useMutation({
    mutationFn: (password: string) =>
      apiClient.post<ApiResponse<void>>('/api/v1/admin/verify', { password }),
  })
}
