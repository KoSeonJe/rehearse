import { useCallback, useRef } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient, ApiError } from '@/lib/api-client'
import { convertBlobToWav } from '@/utils/audio-converter'
import type {
  ApiResponse,
  InterviewSession,
  InterviewListItem,
  InterviewListResponse,
  InterviewStats,
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
  // 면접 종료/언마운트 등 외부 요청으로 in-flight 요청을 취소할 수 있도록
  // 훅 레벨에서 AbortController 를 보관한다.
  const abortRef = useRef<AbortController | null>(null)

  const mutation = useMutation({
    mutationFn: async ({
      id,
      data,
      audioBlob,
    }: {
      id: number
      data: FollowUpRequest
      audioBlob?: Blob
    }) => {
      // 직전 요청이 살아있으면 먼저 abort (직렬 보장)
      abortRef.current?.abort()
      const ac = new AbortController()
      abortRef.current = ac

      const formData = new FormData()
      const requestBlob = new Blob([JSON.stringify(data)], {
        type: 'application/json',
      })
      formData.append('request', requestBlob)

      if (audioBlob && audioBlob.size > 0) {
        const wavBlob = await convertBlobToWav(audioBlob)
        formData.append('audio', wavBlob, 'answer.wav')
      }

      const response = await fetch(`${API_BASE_URL}/api/v1/interviews/${id}/follow-up`, {
        method: 'POST',
        body: formData,
        credentials: 'include',
        signal: ac.signal,
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
    onSettled: () => {
      abortRef.current = null
    },
  })

  // 외부에서 호출 가능한 abort 함수 — 면접 종료/언마운트 시 사용.
  // TanStack Query 가 미래에 `cancel` 을 추가할 가능성을 피해 cancelRequest 로 명명.
  // useCallback 으로 감싸고 abortRef 는 호출 시점에만 read 하므로 렌더 중 ref 접근이 아님.
  const cancelRequest = useCallback(() => {
    abortRef.current?.abort()
    abortRef.current = null
  }, [])

  // eslint-disable-next-line react-hooks/refs -- cancelRequest 는 호출 시점에만 ref 를 read 하는 이벤트 핸들러
  return Object.assign(mutation, { cancelRequest })
}

export const useInterviews = () => {
  return useQuery({
    queryKey: ['interviews', 'list'],
    queryFn: () =>
      apiClient.get<ApiResponse<InterviewListResponse>>('/api/v1/interviews'),
  })
}

export const useInterviewStats = () => {
  return useQuery({
    queryKey: ['interviews', 'stats'],
    queryFn: () =>
      apiClient.get<ApiResponse<InterviewStats>>('/api/v1/interviews/stats'),
  })
}

export const useDeleteInterview = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: number) =>
      apiClient.delete<ApiResponse<void>>(`/api/v1/interviews/${id}`),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['interviews', 'list'] })
      void queryClient.invalidateQueries({ queryKey: ['interviews', 'stats'] })
    },
  })
}

// InterviewListItem을 re-export (대시보드 컴포넌트에서 사용)
export type { InterviewListItem }
