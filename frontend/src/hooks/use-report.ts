import { useQuery } from '@tanstack/react-query'
import { apiClient } from '@/lib/api-client'
import type { ApiResponse, InterviewReport } from '@/types/interview'

export const useReport = (interviewId: string) => {
  return useQuery({
    queryKey: ['report', interviewId],
    queryFn: () =>
      apiClient.get<ApiResponse<InterviewReport>>(
        `/api/v1/interviews/${interviewId}/report`,
      ),
    enabled: !!interviewId,
  })
}
