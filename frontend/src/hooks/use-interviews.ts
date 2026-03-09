import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '@/lib/api-client'
import type {
  ApiResponse,
  InterviewSession,
  CreateInterviewRequest,
  UpdateInterviewStatusRequest,
  UpdateInterviewStatusResponse,
  FollowUpRequest,
  FollowUpResponse,
} from '@/types/interview'

export const useCreateInterview = () => {
  return useMutation({
    mutationFn: (data: CreateInterviewRequest) =>
      apiClient.post<ApiResponse<InterviewSession>>(
        '/api/v1/interviews',
        data,
      ),
  })
}

export const useInterview = (id: string) => {
  return useQuery({
    queryKey: ['interviews', id],
    queryFn: () =>
      apiClient.get<ApiResponse<InterviewSession>>(
        `/api/v1/interviews/${id}`,
      ),
    staleTime: Infinity,
    enabled: !!id,
  })
}

export const useUpdateInterviewStatus = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: number
      data: UpdateInterviewStatusRequest
    }) =>
      apiClient.patch<ApiResponse<UpdateInterviewStatusResponse>>(
        `/api/v1/interviews/${id}/status`,
        data,
      ),
    onSuccess: (_result, variables) => {
      void queryClient.invalidateQueries({
        queryKey: ['interviews', String(variables.id)],
      })
    },
  })
}

export const useFollowUpQuestion = () => {
  return useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: number
      data: FollowUpRequest
    }) =>
      apiClient.post<ApiResponse<FollowUpResponse>>(
        `/api/v1/interviews/${id}/follow-up`,
        data,
      ),
  })
}
