import { useQuery } from '@tanstack/react-query'
import { getSessionFeedback } from '@/api/session-feedback'

export const useSessionFeedback = (interviewId: number, enabled: boolean) => {
  return useQuery({
    queryKey: ['sessionFeedback', interviewId],
    queryFn: () => getSessionFeedback(interviewId),
    enabled: enabled && interviewId > 0,
    staleTime: 1000 * 60 * 5,
    retry: false,
  })
}
