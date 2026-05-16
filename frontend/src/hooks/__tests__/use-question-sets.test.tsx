import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { apiClient } from '@/lib/api-client'
import { useAllQuestionSetStatuses } from '@/hooks/use-question-sets'
import type {
  ApiResponse,
  QuestionSetData,
  QuestionSetStatusResponse,
} from '@/types/interview'

vi.mock('@/lib/api-client', () => ({
  apiClient: {
    get: vi.fn(),
  },
}))

const mockedApiGet = vi.mocked(apiClient.get)

const wrap = () => {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  })
  const Wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  )
  Wrapper.displayName = 'QueryWrapper'
  return Wrapper
}

const buildStatus = (
  analysisStatus: QuestionSetStatusResponse['analysisStatus'],
): ApiResponse<QuestionSetStatusResponse> => ({
  success: true,
  data: {
    id: 1,
    analysisStatus,
    convertStatus: 'COMPLETED',
    fileStatus: 'UPLOADED',
    isVerbalCompleted: analysisStatus === 'COMPLETED',
    isNonverbalCompleted: analysisStatus === 'COMPLETED',
    fullyReady: analysisStatus === 'COMPLETED',
    failureReason: null,
  },
  message: null,
})

const questionSets: QuestionSetData[] = [
  {
    id: 1,
    category: 'cs-fundamental',
    orderIndex: 0,
    analysisStatus: 'PENDING',
    failureReason: null,
    questions: [],
  },
]

describe('useAllQuestionSetStatuses', () => {
  beforeEach(() => {
    mockedApiGet.mockReset()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('analysisStatus 가 PENDING → ANALYZING → COMPLETED 점진 응답 시 단일 출처로 종료 상태 도달', async () => {
    mockedApiGet
      .mockResolvedValueOnce(buildStatus('PENDING'))
      .mockResolvedValueOnce(buildStatus('ANALYZING'))
      .mockResolvedValue(buildStatus('COMPLETED'))

    const { result } = renderHook(
      () => useAllQuestionSetStatuses(1, questionSets, true),
      { wrapper: wrap() },
    )

    await waitFor(() => {
      expect(result.current[0].data?.data.analysisStatus).toBe('PENDING')
    })

    // refetchInterval 폴링 발동 — 추가 호출이 점진 응답 sequence 따라 진행
    await waitFor(
      () => {
        expect(result.current[0].data?.data.analysisStatus).toBe('COMPLETED')
      },
      { timeout: 15_000 },
    )

    expect(mockedApiGet).toHaveBeenCalledWith(
      '/api/v1/interviews/1/question-sets/1/status',
    )
  }, 20_000)

  it('응답에 delivery 키 부재해도 analysisStatus 단일 출처로 정상 동작 (회귀)', async () => {
    // delivery.* 의존 없이 analysisStatus 만으로 종료 판정 — Phase 2b 응답 schema 회귀
    mockedApiGet.mockResolvedValue(buildStatus('COMPLETED'))

    const { result } = renderHook(
      () => useAllQuestionSetStatuses(1, questionSets, true),
      { wrapper: wrap() },
    )

    await waitFor(() => {
      expect(result.current[0].data?.data.analysisStatus).toBe('COMPLETED')
    })

    const response = result.current[0].data?.data
    expect(response).toBeDefined()
    expect(response).not.toHaveProperty('delivery')
  })

  it('모든 status COMPLETED 도달 시 폴링 자동 종료 (refetchInterval false)', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    mockedApiGet.mockResolvedValue(buildStatus('COMPLETED'))

    const { result } = renderHook(
      () => useAllQuestionSetStatuses(1, questionSets, true),
      { wrapper: wrap() },
    )

    await waitFor(() => {
      expect(result.current[0].data?.data.analysisStatus).toBe('COMPLETED')
    })

    const callCountAtTerminal = mockedApiGet.mock.calls.length

    // terminal 도달 후 폴링 간격을 충분히 초과해도 추가 호출 없어야 함
    await vi.advanceTimersByTimeAsync(20_000)

    expect(mockedApiGet.mock.calls.length).toBe(callCountAtTerminal)
  })
})
