import { useEffect, useRef } from 'react'
import { useQuery, useMutation } from '@tanstack/react-query'
import { apiClient, ApiError } from '@/lib/api-client'
import type { ApiResponse, InterviewReport } from '@/types/interview'

type ReportStatus = 'loading' | 'analyzing' | 'generating' | 'failed' | 'ready' | 'error'

export const useReport = (interviewId: string) => {
  const hasTriggeredPost = useRef(false)

  const postMutation = useMutation({
    mutationFn: () =>
      apiClient.post<ApiResponse<InterviewReport>>(
        `/api/v1/interviews/${interviewId}/report`,
      ),
  })
  const mutateRef = useRef(postMutation.mutate)
  mutateRef.current = postMutation.mutate

  const query = useQuery({
    queryKey: ['report', interviewId],
    queryFn: () =>
      apiClient.get<ApiResponse<InterviewReport>>(
        `/api/v1/interviews/${interviewId}/report`,
      ),
    enabled: !!interviewId,
    retry: (failureCount, error) => {
      // 500 에러 → 즉시 폴링 중단 (리포트 생성 실패)
      if (error instanceof ApiError && error.status >= 500) return false
      // 202 (REPORT_GENERATING) → 폴링으로 재시도, 최대 24회 (2분)
      if (error instanceof ApiError && error.status === 202) return failureCount < 24
      // 409 (ANALYSIS_NOT_COMPLETED) → 폴링으로 재시도, 최대 60회 (5분)
      if (error instanceof ApiError && error.status === 409) return failureCount < 60
      // 그 외 에러 → 3회까지 retry
      return failureCount < 3
    },
    retryDelay: (failureCount, error) => {
      // 202/409 → 5초 간격 폴링
      if (error instanceof ApiError && (error.status === 202 || error.status === 409)) {
        return 5000
      }
      return Math.min(1000 * 2 ** failureCount, 10000)
    },
  })

  // 첫 202 응답 시 POST 트리거 1회 실행 (리포트 생성 백업 경로)
  useEffect(() => {
    if (
      !hasTriggeredPost.current &&
      query.isError &&
      query.error instanceof ApiError &&
      query.error.status === 202
    ) {
      hasTriggeredPost.current = true
      mutateRef.current()
    }
  }, [query.isError, query.error])

  const reportStatus: ReportStatus = (() => {
    if (query.data?.data) return 'ready'
    if (query.isLoading) return 'loading'
    if (query.isError && query.error instanceof ApiError) {
      if (query.error.status >= 500) return 'failed'
      if (query.error.status === 409) return 'analyzing'
      if (query.error.status === 202) return 'generating'
    }
    if (query.isError) return 'error'
    return 'loading'
  })()

  return {
    ...query,
    report: query.data?.data ?? null,
    reportStatus,
  }
}
