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
  QuestionGenerationStatus,
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
        credentials: 'include',
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

const POLL_STATUSES: QuestionGenerationStatus[] = ['PENDING', 'GENERATING']

export const useInterview = (id: string) => {
  return useQuery({
    queryKey: ['interviews', id],
    queryFn: () =>
      apiClient.get<ApiResponse<InterviewSession>>(
        `/api/v1/interviews/${id}`,
      ),
    staleTime: Infinity,
    enabled: !!id,
    refetchInterval: (query) => {
      const status = query.state.data?.data?.questionGenerationStatus
      if (status && POLL_STATUSES.includes(status)) {
        return 2000
      }
      return false
    },
  })
}

const UUID_REGEX =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

// 분석/피드백 페이지 전용 — 질문 생성 완료 후 사용하므로 폴링 불필요
export const useInterviewByPublicId = (publicId: string) => {
  return useQuery({
    queryKey: ['interviews', 'public', publicId],
    queryFn: () =>
      apiClient.get<ApiResponse<InterviewSession>>(
        `/api/v1/interviews/by-public-id/${publicId}`,
      ),
    staleTime: Infinity,
    enabled: !!publicId && UUID_REGEX.test(publicId),
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

export const useRetryQuestions = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: number) =>
      apiClient.post<ApiResponse<InterviewSession>>(
        `/api/v1/interviews/${id}/retry-questions`,
      ),
    onSuccess: (_result, id) => {
      void queryClient.invalidateQueries({
        queryKey: ['interviews', String(id)],
      })
    },
  })
}

export const useSkipRemainingQuestionSets = () => {
  return useMutation({
    mutationFn: (interviewId: number) =>
      apiClient.post<ApiResponse<void>>(
        `/api/v1/interviews/${interviewId}/skip-remaining`,
      ),
  })
}

export const useFollowUpQuestion = () => {
  return useMutation({
    mutationFn: async ({
      id,
      data,
      audioBlob,
    }: {
      id: number
      data: FollowUpRequest
      audioBlob?: Blob
    }) => {
      const formData = new FormData()
      const requestBlob = new Blob([JSON.stringify(data)], {
        type: 'application/json',
      })
      formData.append('request', requestBlob)

      if (audioBlob && audioBlob.size > 0) {
        formData.append('audio', audioBlob, 'answer.webm')
      }

      const response = await fetch(`${API_BASE_URL}/api/v1/interviews/${id}/follow-up`, {
        method: 'POST',
        body: formData,
        credentials: 'include',
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

      return response.json() as Promise<ApiResponse<FollowUpResponse>>
    },
  })
}
