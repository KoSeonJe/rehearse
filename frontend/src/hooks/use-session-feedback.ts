import { useQuery } from '@tanstack/react-query'
import { getSessionFeedback } from '@/api/session-feedback'
import { buildMockSessionFeedback } from '@/mocks/session-feedback-mock'

const USE_MOCK = import.meta.env.DEV

export const useSessionFeedback = (interviewId: number, enabled: boolean) =>
  useQuery({
    queryKey: ['sessionFeedback', interviewId, USE_MOCK ? 'mock' : 'live'],
    queryFn: () =>
      USE_MOCK
        ? Promise.resolve(buildMockSessionFeedback(interviewId))
        : getSessionFeedback(interviewId),
    enabled: enabled && interviewId > 0,
    staleTime: 1000 * 60 * 5,
    retry: false,
  })
