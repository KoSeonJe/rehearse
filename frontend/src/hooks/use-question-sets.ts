import { useMutation, useQueries, useQuery } from '@tanstack/react-query'
import { apiClient } from '@/lib/api-client'
import type {
  ApiResponse,
  SaveAnswersRequest,
  UploadUrlRequest,
  UploadUrlResponse,
  QuestionSetStatusResponse,
  QuestionsWithAnswersResponse,
  QuestionSetData,
  QuestionSetFeedbackResponse,
} from '@/types/interview'

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

// 모든 질문세트의 상태를 병렬 폴링
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
      refetchInterval: enabled ? 5000 : false,
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
