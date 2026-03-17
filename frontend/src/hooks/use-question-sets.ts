import { useMutation, useQuery } from '@tanstack/react-query'
import { apiClient } from '@/lib/api-client'
import type {
  ApiResponse,
  SaveAnswersRequest,
  UploadUrlRequest,
  UploadUrlResponse,
  QuestionSetStatusResponse,
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
