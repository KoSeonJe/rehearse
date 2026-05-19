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
      questionType: 'TECH_MAIN',
      questionText: '질문 0',
      bestAnswer: null,
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

// 트랙 컨텍스트별 follow-up 질문이 BE QuestionType enum 과 정합한 값으로 store 에 저장되는지 검증.
// 회귀: 이전엔 모든 트랙에서 'FOLLOWUP' (BE 미존재) 로 저장 → 검색 분기 미스.
describe('useAnswerFlow — 트랙별 follow-up questionType 정합', () => {
  beforeEach(() => {
    mutateAsyncMock.mockReset()
    cancelRequestMock.mockReset()
  })

  afterEach(() => {
    useInterviewStore.getState().reset()
  })

  const buildSetWithMainType = (
    id: number,
    category: string,
    mainType: 'TECH_MAIN' | 'BEHAVIORAL_MAIN' | 'RESUME_OPENER',
  ): QuestionSetData => ({
    id,
    category,
    orderIndex: 0,
    analysisStatus: 'PENDING',
    failureReason: null,
    questions: [
      {
        id: 1,
        questionType: mainType,
        questionText: '메인 질문',
        bestAnswer: null,
        orderIndex: 0,
      },
    ],
  })

  const seedRecordingForFollowUp = (qSet: QuestionSetData) => {
    useInterviewStore.getState().reset()
    useInterviewStore.getState().setInterview(99, [buildQuestion(0)])
    useInterviewStore.getState().setQuestionSets([qSet])
    useInterviewStore.setState({ phase: 'recording' })
    useInterviewStore.setState((s) => ({
      answers: s.answers.map((a, i) =>
        i === 0
          ? {
              ...a,
              transcripts: [
                { questionIndex: 0, text: '답변', startTime: 0, endTime: 100, isFinal: true },
              ],
            }
          : a,
      ),
    }))
  }

  const buildParamsForSet = (qSet: QuestionSetData) => ({
    ...buildParams(),
    interview: {
      id: 99,
      publicId: 'pub-1',
      status: 'IN_PROGRESS',
      questionSets: [qSet],
    },
  })

  it('TECH 트랙 (TECH_MAIN) → follow-up 응답이 store 에 TECH_FOLLOWUP 으로 추가된다', async () => {
    const qSet = buildSetWithMainType(12, 'CS_FUNDAMENTAL', 'TECH_MAIN')
    seedRecordingForFollowUp(qSet)
    mutateAsyncMock.mockResolvedValue(
      buildResponse({
        questionId: 555,
        question: '추가 설명 부탁드려요',
        skip: false,
        followUpExhausted: false,
      }),
    )

    const { result } = renderHook(() => useAnswerFlow(buildParamsForSet(qSet)))
    await act(async () => {
      await result.current.handleStopAnswer()
    })

    const stored = useInterviewStore.getState().questionSets[0].questions
    expect(stored).toHaveLength(2)
    expect(stored[1].questionType).toBe('TECH_FOLLOWUP')
    expect(stored[1].id).toBe(555)
  })

  it('BEHAVIORAL 트랙 (BEHAVIORAL_MAIN) → follow-up 응답이 store 에 BEHAVIORAL_FOLLOWUP 으로 추가된다', async () => {
    const qSet = buildSetWithMainType(13, 'BEHAVIORAL', 'BEHAVIORAL_MAIN')
    seedRecordingForFollowUp(qSet)
    mutateAsyncMock.mockResolvedValue(
      buildResponse({
        questionId: 777,
        question: '그 갈등을 어떻게 해결하셨나요',
        skip: false,
        followUpExhausted: false,
      }),
    )

    const { result } = renderHook(() => useAnswerFlow(buildParamsForSet(qSet)))
    await act(async () => {
      await result.current.handleStopAnswer()
    })

    const stored = useInterviewStore.getState().questionSets[0].questions
    expect(stored).toHaveLength(2)
    expect(stored[1].questionType).toBe('BEHAVIORAL_FOLLOWUP')
    expect(stored[1].id).toBe(777)
  })

  it('RESUME 트랙 (RESUME_OPENER) → follow-up 응답이 store 에 TECH_FOLLOWUP 으로 추가된다 (BE fallback 정합)', async () => {
    const qSet = buildSetWithMainType(14, 'RESUME_BASED', 'RESUME_OPENER')
    seedRecordingForFollowUp(qSet)
    mutateAsyncMock.mockResolvedValue(
      buildResponse({
        questionId: 999,
        question: '그 프로젝트 규모는?',
        skip: false,
        followUpExhausted: false,
      }),
    )

    const { result } = renderHook(() => useAnswerFlow(buildParamsForSet(qSet)))
    await act(async () => {
      await result.current.handleStopAnswer()
    })

    const stored = useInterviewStore.getState().questionSets[0].questions
    expect(stored).toHaveLength(2)
    expect(stored[1].questionType).toBe('TECH_FOLLOWUP')
    expect(stored[1].id).toBe(999)
  })
})
