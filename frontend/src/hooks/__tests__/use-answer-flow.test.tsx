import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import type { MutableRefObject } from 'react'
import { useAnswerFlow } from '@/hooks/use-answer-flow'
import { useInterviewStore } from '@/stores/interview-store'
import type { Question, QuestionSetData, FollowUpResponse, ApiResponse } from '@/types/interview'

// 외부 경계 mock — 네트워크 / 업로드. 내부 훅 / 스토어는 실제 사용.
const mutateAsyncMock = vi.fn()
const cancelRequestMock = vi.fn()

vi.mock('@/hooks/use-interviews', () => ({
  useFollowUpQuestion: () => ({
    mutateAsync: mutateAsyncMock,
    cancelRequest: cancelRequestMock,
  }),
}))

vi.mock('@/hooks/use-s3-upload', () => ({
  useS3Upload: () => ({
    upload: vi.fn(),
    progress: 0,
    isUploading: false,
  }),
}))

const buildResponse = (data: Partial<FollowUpResponse>): ApiResponse<FollowUpResponse> => ({
  success: true,
  message: null,
  data: {
    questionId: 0,
    question: '',
    ttsQuestion: null,
    reason: '',
    type: 'DEEP_DIVE',
    skip: false,
    followUpExhausted: false,
    ...data,
  } as FollowUpResponse,
})

const buildQuestion = (index: number): Question => ({
  id: index + 1,
  content: `질문 ${index}`,
  ttsContent: null,
  category: 'CS',
  order: index,
})

const buildQuestionSet = (id: number): QuestionSetData => ({
  id,
  category: 'CS',
  orderIndex: 0,
  analysisStatus: 'PENDING',
  failureReason: null,
  questions: [
    {
      id: 1,
      questionType: 'MAIN',
      questionText: '질문 0',
      modelAnswer: null,
      orderIndex: 0,
    },
  ],
})

const buildParams = () => {
  const greetingPhaseRef: MutableRefObject<boolean> = { current: false }
  const pendingTtsActionRef: MutableRefObject<(() => void) | null> = { current: null }

  return {
    interview: {
      id: 99,
      publicId: 'pub-1',
      status: 'IN_PROGRESS',
      questionSets: [buildQuestionSet(12)],
    },
    mediaStream: { stream: null },
    recorder: {
      isRecording: false,
      start: vi.fn(),
      stop: vi.fn().mockResolvedValue(new Blob()),
      pause: vi.fn(),
      resume: vi.fn(),
      restart: vi.fn().mockResolvedValue(new Blob()),
    },
    audioCapture: {
      start: vi.fn(),
      stop: vi.fn().mockResolvedValue(new Blob([], { type: 'audio/webm' })),
    },
    tts: {
      speak: vi.fn(),
      stop: vi.fn(),
    },
    recordEvent: vi.fn(),
    startEventRecording: vi.fn(),
    greetingPhaseRef,
    completeGreeting: vi.fn(),
    pendingTtsActionRef,
    registerUploadPromise: vi.fn(),
  }
}

const seedRecordingState = (overdue: boolean) => {
  useInterviewStore.getState().reset()
  const questions = [buildQuestion(0)]
  useInterviewStore.getState().setInterview(99, questions)
  useInterviewStore.getState().setQuestionSets([buildQuestionSet(12)])
  // 답변 진행 상태로 진입.
  useInterviewStore.setState({ phase: 'recording' })
  useInterviewStore.getState().setTimeOverdue(overdue)
}

describe('useAnswerFlow — 시간 만료 / followUp 소진 시 BE 호출 강제', () => {
  beforeEach(() => {
    mutateAsyncMock.mockReset()
    cancelRequestMock.mockReset()
  })

  afterEach(() => {
    useInterviewStore.getState().reset()
  })

  it('isTimeOverdue=true && hasAnswer=false → mutateAsync 호출 + payload 에 terminate:true', async () => {
    seedRecordingState(true)
    mutateAsyncMock.mockResolvedValue(
      buildResponse({ skip: true, followUpExhausted: true }),
    )

    const params = buildParams()
    const { result } = renderHook(() => useAnswerFlow(params))

    await act(async () => {
      await result.current.handleStopAnswer()
    })

    expect(mutateAsyncMock).toHaveBeenCalledTimes(1)
    const payload = mutateAsyncMock.mock.calls[0][0] as {
      data: { terminate?: boolean; answerText?: string }
    }
    expect(payload.data.terminate).toBe(true)
    expect(payload.data.answerText).toBe('')
  })

  it('followUpExhausted=true && isTimeOverdue=true → BE 호출 강제 + terminate:true', async () => {
    seedRecordingState(true)
    useInterviewStore.getState().setFollowUpExhausted(true)

    mutateAsyncMock.mockResolvedValue(
      buildResponse({ skip: true, followUpExhausted: true }),
    )

    const params = buildParams()
    const { result } = renderHook(() => useAnswerFlow(params))

    await act(async () => {
      await result.current.handleStopAnswer()
    })

    expect(mutateAsyncMock).toHaveBeenCalledTimes(1)
    const payload = mutateAsyncMock.mock.calls[0][0] as {
      data: { terminate?: boolean }
    }
    expect(payload.data.terminate).toBe(true)
  })

  it('정상 답변 (isTimeOverdue=false && hasAnswer=true) → terminate:false', async () => {
    seedRecordingState(false)
    // 답변 텍스트 주입
    useInterviewStore.setState((s) => ({
      answers: s.answers.map((a, i) =>
        i === 0
          ? {
              ...a,
              transcripts: [
                { questionIndex: 0, text: '정상 답변입니다', startTime: 0, endTime: 100, isFinal: true },
              ],
            }
          : a,
      ),
    }))

    mutateAsyncMock.mockResolvedValue(
      buildResponse({
        questionId: 5,
        question: '추가 설명 부탁드려요',
        skip: false,
        followUpExhausted: false,
        type: 'DEEP_DIVE',
      }),
    )

    const params = buildParams()
    const { result } = renderHook(() => useAnswerFlow(params))

    await act(async () => {
      await result.current.handleStopAnswer()
    })

    expect(mutateAsyncMock).toHaveBeenCalledTimes(1)
    const payload = mutateAsyncMock.mock.calls[0][0] as {
      data: { terminate?: boolean; answerText?: string }
    }
    expect(payload.data.terminate).toBe(false)
    expect(payload.data.answerText).toBe('정상 답변입니다')
  })

  it('isTimeOverdue=false && hasAnswer=false && followUpExhausted=true → BE 호출 안 함', async () => {
    seedRecordingState(false)
    useInterviewStore.getState().setFollowUpExhausted(true)

    const params = buildParams()
    const { result } = renderHook(() => useAnswerFlow(params))

    await act(async () => {
      await result.current.handleStopAnswer()
    })

    expect(mutateAsyncMock).not.toHaveBeenCalled()
  })

  it('종료 분기 진입 후 isTimeOverdue 가 false 로 리셋된다', async () => {
    seedRecordingState(true)
    mutateAsyncMock.mockResolvedValue(
      buildResponse({ skip: true, followUpExhausted: true }),
    )

    const params = buildParams()
    const { result } = renderHook(() => useAnswerFlow(params))

    await act(async () => {
      await result.current.handleStopAnswer()
    })

    expect(useInterviewStore.getState().isTimeOverdue).toBe(false)
  })
})
