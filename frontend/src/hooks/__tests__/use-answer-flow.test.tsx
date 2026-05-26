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

// 빈 Blob(size=0)=답변 없음, 내용 있는 Blob(size>0)=답변 있음.
// hasAnswer 판정이 audioBlob.size 기준으로 단일화됨.
const EMPTY_AUDIO_BLOB = new Blob([], { type: 'audio/webm' })
const buildAnswerAudioBlob = () => new Blob(['voice'], { type: 'audio/webm' })

const buildParams = (audioBlob: Blob = EMPTY_AUDIO_BLOB) => {
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
      stop: vi.fn().mockResolvedValue(audioBlob),
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
      data: Record<string, unknown>
    }
    expect(payload.data.terminate).toBe(true)
    // answerText 필드는 더 이상 송신하지 않는다 (BE transcript 단일 소스).
    expect(payload.data).not.toHaveProperty('answerText')
  })

  it('직전 응답이 followUpExhausted=true 였어도 다음 답변에 follow-up API 가 다시 호출된다', async () => {
    // 회귀 가드: FE 가 followUpExhausted 신호로 BE 호출 게이트를 막던 결함 해결.
    // 마지막 follow-up 답변이 BE 에 미도달하는 문제를 막기 위해 게이트를 제거했다.
    seedRecordingState(false)

    mutateAsyncMock.mockResolvedValue(
      buildResponse({ skip: true, followUpExhausted: true }),
    )

    // 음성 답변 존재 → hasAnswer=true.
    const params = buildParams(buildAnswerAudioBlob())
    const { result } = renderHook(() => useAnswerFlow(params))

    await act(async () => {
      await result.current.handleStopAnswer()
    })

    expect(mutateAsyncMock).toHaveBeenCalledTimes(1)
  })

  it('정상 답변 (isTimeOverdue=false && hasAnswer=true) → terminate:false, answerText 미송신', async () => {
    seedRecordingState(false)

    mutateAsyncMock.mockResolvedValue(
      buildResponse({
        questionId: 5,
        question: '추가 설명 부탁드려요',
        skip: false,
        followUpExhausted: false,
        type: 'DEEP_DIVE',
      }),
    )

    // 음성 답변 존재 → hasAnswer=true.
    const params = buildParams(buildAnswerAudioBlob())
    const { result } = renderHook(() => useAnswerFlow(params))

    await act(async () => {
      await result.current.handleStopAnswer()
    })

    expect(mutateAsyncMock).toHaveBeenCalledTimes(1)
    const payload = mutateAsyncMock.mock.calls[0][0] as {
      data: Record<string, unknown>
      audioBlob?: Blob
    }
    expect(payload.data.terminate).toBe(false)
    expect(payload.data).not.toHaveProperty('answerText')
    // 답변 텍스트 신뢰 소스는 audioBlob → BE STT/transcript.
    expect(payload.audioBlob).toBeInstanceOf(Blob)
  })

  it('isTimeOverdue=false && hasAnswer=false → BE 호출 안 함', async () => {
    seedRecordingState(false)

    const params = buildParams()
    const { result } = renderHook(() => useAnswerFlow(params))

    await act(async () => {
      await result.current.handleStopAnswer()
    })

    expect(mutateAsyncMock).not.toHaveBeenCalled()
  })

  it('skip=true 응답 → SKIP_TRANSITION_PHRASES 로 다음 질문 안내 발화 (transitionToNext skipPhrase=true)', async () => {
    // 마지막 질문이면 CLOSING, 세트 끝이면 SET_TRANSITION 으로 분기되므로,
    // "같은 세트 내 다음 메인 질문" 상황 (main 질문 2개 + currentIndex=0) 으로 시드해야 SKIP 분기 도달.
    const multiMainSet: QuestionSetData = {
      id: 12,
      category: 'CS',
      orderIndex: 0,
      analysisStatus: 'PENDING',
      failureReason: null,
      questions: [
        { id: 1, questionType: 'TECH_MAIN', questionText: '질문 0', bestAnswer: null, orderIndex: 0 },
        { id: 2, questionType: 'TECH_MAIN', questionText: '질문 1', bestAnswer: null, orderIndex: 1 },
      ],
    }
    useInterviewStore.getState().reset()
    useInterviewStore.getState().setInterview(99, [buildQuestion(0), buildQuestion(1)])
    useInterviewStore.getState().setQuestionSets([multiMainSet])
    useInterviewStore.setState({ phase: 'recording' })

    mutateAsyncMock.mockResolvedValue(
      buildResponse({ skip: true, followUpExhausted: false }),
    )

    // 음성 답변 존재 → BE 호출 트리거.
    const params = buildParams(buildAnswerAudioBlob())
    const { result } = renderHook(() => useAnswerFlow(params))

    await act(async () => {
      await result.current.handleStopAnswer()
    })

    // skip 분기 → SKIP_TRANSITION_PHRASES 사용 검증. 메인 질문 분기는 CLOSING_PHRASES 가 아닌 SKIP 멘트.
    const SKIP_PHRASES = [
      '네. 알겠습니다. 그럼 다음 질문으로 넘어가 볼게요.',
      '네. 좋습니다. 다른 주제로 넘어가겠습니다.',
      '네. 그럼 다음 질문 드리겠습니다.',
    ]
    expect(params.tts.speak).toHaveBeenCalledTimes(1)
    const spokenText = (params.tts.speak as ReturnType<typeof vi.fn>).mock.calls[0][0]
    expect(SKIP_PHRASES).toContain(spokenText)
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
  }

  // 음성 답변 존재 → hasAnswer=true → BE 호출 트리거.
  const buildParamsForSet = (qSet: QuestionSetData) => ({
    ...buildParams(buildAnswerAudioBlob()),
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
