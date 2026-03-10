import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient, ApiError } from '@/lib/api-client'
import type {
  ApiResponse,
  InterviewSession,
  CreateInterviewRequest,
  UpdateInterviewStatusRequest,
  UpdateInterviewStatusResponse,
  FollowUpRequest,
  FollowUpResponse,
} from '@/types/interview'

const API_BASE_URL = import.meta.env.VITE_API_URL || ''

export const useCreateInterview = () => {
  return useMutation({
    mutationFn: async ({
      request,
      resumeFile,
    }: {
      request: CreateInterviewRequest
      resumeFile?: File | null
    }) => {
      const formData = new FormData()

      const requestBlob = new Blob([JSON.stringify(request)], {
        type: 'application/json',
      })
      formData.append('request', requestBlob)

      if (resumeFile) {
        formData.append('resumeFile', resumeFile)
      }

      const response = await fetch(`${API_BASE_URL}/api/v1/interviews`, {
        method: 'POST',
        body: formData,
      })

      if (!response.ok) {
        let errorBody
        try {
          errorBody = await response.json()
        } catch {
          throw new ApiError(response.status, {
            success: false,
            status: response.status,
            code: 'UNKNOWN_ERROR',
            message: response.statusText || '알 수 없는 오류가 발생했습니다.',
            errors: [],
            timestamp: new Date().toISOString(),
          })
        }
        throw new ApiError(response.status, errorBody)
      }

      return response.json() as Promise<ApiResponse<InterviewSession>>
    },
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
