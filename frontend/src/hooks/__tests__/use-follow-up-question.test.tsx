import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { renderHook, act, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { useFollowUpQuestion } from '@/hooks/use-interviews'
import type { FollowUpRequest } from '@/types/interview'

const createWrapper = () => {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  )
}

const buildResponse = (body: object) =>
  new Response(JSON.stringify(body), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  })

describe('useFollowUpQuestion — terminate 신호 payload 직렬화', () => {
  let fetchSpy: ReturnType<typeof vi.fn>

  beforeEach(() => {
    fetchSpy = vi.fn()
    vi.stubGlobal('fetch', fetchSpy)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  const defaultData: FollowUpRequest = {
    questionSetId: 12,
    questionContent: '프로젝트에서 가장 어려웠던 점은?',
    answerText: '메모리 누수 디버깅이 어려웠습니다.',
    previousExchanges: [],
  }

  const extractRequestJson = async (call: unknown): Promise<Record<string, unknown>> => {
    const init = (call as [string, RequestInit])[1]
    const formData = init.body as FormData
    const requestPart = formData.get('request')
    if (!(requestPart instanceof Blob)) throw new Error('request part missing')
    return JSON.parse(await requestPart.text()) as Record<string, unknown>
  }

  it('terminate=true 동봉 시 request payload 에 terminate:true 직렬화된다', async () => {
    fetchSpy.mockResolvedValue(
      buildResponse({
        success: true,
        data: {
          questionId: null,
          question: null,
          ttsQuestion: null,
          reason: null,
          type: null,
          skip: true,
          presentToUser: false,
          followUpExhausted: true,
        },
      }),
    )

    const { result } = renderHook(() => useFollowUpQuestion(), { wrapper: createWrapper() })

    await act(async () => {
      await result.current.mutateAsync({
        id: 99,
        data: { ...defaultData, terminate: true },
      })
    })

    await waitFor(() => expect(fetchSpy).toHaveBeenCalledTimes(1))
    const json = await extractRequestJson(fetchSpy.mock.calls[0])

    expect(json.terminate).toBe(true)
    expect(json.questionSetId).toBe(12)
  })

  it('terminate 미지정 시 request payload 에 terminate 필드 부재 (BE default false)', async () => {
    fetchSpy.mockResolvedValue(
      buildResponse({
        success: true,
        data: {
          questionId: 1,
          question: '추가 설명 부탁드려요',
          ttsQuestion: null,
          reason: '심화',
          type: 'DEEP_DIVE',
          skip: false,
          followUpExhausted: false,
        },
      }),
    )

    const { result } = renderHook(() => useFollowUpQuestion(), { wrapper: createWrapper() })

    await act(async () => {
      await result.current.mutateAsync({
        id: 99,
        data: defaultData,
      })
    })

    await waitFor(() => expect(fetchSpy).toHaveBeenCalledTimes(1))
    const json = await extractRequestJson(fetchSpy.mock.calls[0])

    expect(json.terminate).toBeUndefined()
  })

  it('terminate=false 명시 시 request payload 에 terminate:false 직렬화된다', async () => {
    fetchSpy.mockResolvedValue(
      buildResponse({
        success: true,
        data: {
          questionId: 1,
          question: 'q',
          ttsQuestion: null,
          reason: 'r',
          type: 'DEEP_DIVE',
          skip: false,
          followUpExhausted: false,
        },
      }),
    )

    const { result } = renderHook(() => useFollowUpQuestion(), { wrapper: createWrapper() })

    await act(async () => {
      await result.current.mutateAsync({
        id: 99,
        data: { ...defaultData, terminate: false },
      })
    })

    await waitFor(() => expect(fetchSpy).toHaveBeenCalledTimes(1))
    const json = await extractRequestJson(fetchSpy.mock.calls[0])

    expect(json.terminate).toBe(false)
  })
})
