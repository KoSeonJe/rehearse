import { useMutation, useQueries, useQuery } from '@tanstack/react-query'
import { apiClient } from '@/lib/api-client'
import type {
  AnalysisStatus,
  ApiResponse,
  SaveAnswersRequest,
  UploadUrlRequest,
  UploadUrlResponse,
  QuestionSetStatusResponse,
  QuestionsWithAnswersResponse,
  QuestionSetData,
  QuestionSetFeedbackResponse,
} from '@/types/interview'

const STATUS_POLL_INTERVAL_MS = 5000

const TERMINAL_ANALYSIS_STATUSES: ReadonlySet<AnalysisStatus> = new Set([
  'COMPLETED',
  'PARTIAL',
  'FAILED',
  'SKIPPED',
])

const isTerminalStatus = (status: AnalysisStatus | undefined): boolean =>
  status !== undefined && TERMINAL_ANALYSIS_STATUSES.has(status)

export const useSaveAnswers = (interviewId: number, questionSetId: number) => {
  return useMutation({
    mutationFn: (data: SaveAnswersRequest) =>
      apiClient.post<ApiResponse<void>>(
        `/api/v1/interviews/${interviewId}/question-sets/${questionSetId}/answers`,
        data,
      ),
  })
}

export const useGenerateUploadUrl = (interviewId: number, questionSetId: number) => {
  return useMutation({
    mutationFn: (data: UploadUrlRequest) =>
      apiClient.post<ApiResponse<UploadUrlResponse>>(
        `/api/v1/interviews/${interviewId}/question-sets/${questionSetId}/upload-url`,
        data,
      ),
  })
}

export const useQuestionSetStatus = (
  interviewId: number,
  questionSetId: number,
  enabled = false,
) => {
  return useQuery({
    queryKey: ['questionSetStatus', interviewId, questionSetId],
    queryFn: () =>
      apiClient.get<ApiResponse<QuestionSetStatusResponse>>(
        `/api/v1/interviews/${interviewId}/question-sets/${questionSetId}/status`,
      ),
    enabled,
    refetchInterval: enabled ? 3000 : false,
  })
}

// 모든 질문세트의 상태를 병렬 폴링 (analysisStatus terminal 도달 시 자동 종료)
export const useAllQuestionSetStatuses = (
  interviewId: number,
  questionSets: QuestionSetData[],
  enabled = false,
) => {
  return useQueries({
    queries: questionSets.map((qs) => ({
      queryKey: ['questionSetStatus', interviewId, qs.id],
      queryFn: () =>
        apiClient.get<ApiResponse<QuestionSetStatusResponse>>(
          `/api/v1/interviews/${interviewId}/question-sets/${qs.id}/status`,
        ),
      enabled,
      refetchInterval: (query: { state: { data?: ApiResponse<QuestionSetStatusResponse> } }) => {
        if (!enabled) return false
        const status = query.state.data?.data.analysisStatus
        return isTerminalStatus(status) ? false : STATUS_POLL_INTERVAL_MS
      },
    })),
  })
}

// 질문세트별 피드백 조회
export const useQuestionSetFeedback = (
  interviewId: number,
  questionSetId: number,
  enabled = true,
) => {
  return useQuery({
    queryKey: ['questionSetFeedback', interviewId, questionSetId],
    queryFn: () =>
      apiClient.get<ApiResponse<QuestionSetFeedbackResponse>>(
        `/api/v1/interviews/${interviewId}/question-sets/${questionSetId}/feedback`,
      ),
    enabled,
    staleTime: Infinity,
  })
}

// 분석 재시도
export const useRetryAnalysis = () => {
  return useMutation({
    mutationFn: ({ interviewId, questionSetId }: { interviewId: number; questionSetId: number }) =>
      apiClient.post<ApiResponse<void>>(
        `/api/v1/interviews/${interviewId}/question-sets/${questionSetId}/retry-analysis`,
      ),
  })
}

// 질문세트별 모범답변 조회
export const useQuestionsWithAnswers = (
  interviewId: number,
  questionSetId: number,
  enabled = false,
) => {
  return useQuery({
    queryKey: ['questionsWithAnswers', interviewId, questionSetId],
    queryFn: () =>
      apiClient.get<ApiResponse<QuestionsWithAnswersResponse>>(
        `/api/v1/interviews/${interviewId}/question-sets/${questionSetId}/questions-with-answers`,
      ),
    enabled,
    staleTime: Infinity,
  })
}
