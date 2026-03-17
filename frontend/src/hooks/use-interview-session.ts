import { useCallback, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { useInterviewStore } from '@/stores/interview-store'
import { useUpdateInterviewStatus } from '@/hooks/use-interviews'
import { useTts } from '@/hooks/use-tts'
import { useThinkingTimeDetector } from '@/hooks/use-thinking-time-detector'
import { useInterviewEventRecorder } from '@/hooks/use-interview-event-recorder'
import { useInterviewGreeting } from '@/hooks/use-interview-greeting'
import { useAnswerFlow } from '@/hooks/use-answer-flow'
import { saveVideoBlob } from '@/lib/video-storage'
import type { Question, TranscriptSegment, QuestionSetData } from '@/types/interview'

interface UseInterviewSessionParams {
  interviewId: string
  interview: { id: number; status: string; questions: Question[]; questionSets?: QuestionSetData[] } | undefined
  mediaStream: {
    stream: MediaStream | null
    isActive: boolean
    start: () => Promise<void>
    stop: () => void
  }
  recorder: {
    isRecording: boolean
    start: (stream: MediaStream) => void
    stop: () => Promise<Blob>
    pause: () => void
    resume: () => void
    restart: (stream: MediaStream) => Promise<Blob>
  }
  stt: {
    interimText: string
    isSupported: boolean
    start: (questionIndex: number) => void
    stop: () => void
    onFinalResult: (callback: (segment: TranscriptSegment) => void) => void
  }
}

export const useInterviewSession = ({
  interviewId,
  interview,
  mediaStream,
  recorder,
  stt,
}: UseInterviewSessionParams) => {
  const navigate = useNavigate()
  const updateStatus = useUpdateInterviewStatus()
  const pendingTtsActionRef = useRef<(() => void) | null>(null)
  const greetingPhaseRef = useRef(false)
  const {
    questions,
    currentQuestionIndex,
    phase,
    questionSets,
    currentQuestionSetIndex,
    setInterview,
    setQuestionSets,
    setCurrentTranscript,
    addTranscript,
    setVideoBlob,
    completeInterview,
    setAutoTransitionMessage,
    addInterviewEvent,
    reset,
  } = useInterviewStore()

  // 마운트 시 스토어 초기화 (재진입 대응)
  useEffect(() => {
    reset()
  }, [reset])

  // 이벤트 레코더
  const { recordEvent, getEvents, startRecording: startEventRecording } = useInterviewEventRecorder()

  // TTS 훅
  const tts = useTts({
    onStart: () => {
      stt.stop()
    },
    onEnd: () => {
      // 전환 TTS 완료 후 예약된 액션 실행 (nextQuestion / finish)
      if (pendingTtsActionRef.current) {
        const state = useInterviewStore.getState()
        if (state.phase !== 'paused' && state.phase !== 'recording' && state.phase !== 'completed') {
          pendingTtsActionRef.current = null
          return
        }
        const action = pendingTtsActionRef.current
        pendingTtsActionRef.current = null
        action()
        return
      }

      // 일반 TTS(질문 읽기) 끝나면 STT 재시작
      const state = useInterviewStore.getState()
      if (state.phase === 'recording') {
        stt.start(state.currentQuestionIndex)
      }
    },
  })

  // Greeting 흐름 (인사 TTS + 자기소개 완료 전환)
  const { completeGreeting } = useInterviewGreeting({
    tts,
    recordEvent,
    mediaStreamIsActive: mediaStream.isActive,
    greetingPhaseRef,
  })

  // 답변 시작/완료 + 전환 로직
  const { handleStartAnswer, handleStopAnswer, s3Upload } = useAnswerFlow({
    interview,
    mediaStream,
    recorder,
    stt,
    tts,
    recordEvent,
    startEventRecording,
    greetingPhaseRef,
    completeGreeting,
    pendingTtsActionRef,
  })

  // 생각 시간 감지
  useThinkingTimeDetector({
    interimText: stt.interimText,
    enabled: phase === 'recording',
    onThinkingTimeRequested: () => {
      recordEvent('thinking_time_requested', currentQuestionIndex)
      tts.speak('네, 천천히 생각하세요. 준비되면 말씀해 주세요.')
      setAutoTransitionMessage('생각 시간 모드 — 충분히 생각하세요')
      setTimeout(() => {
        setAutoTransitionMessage(null)
      }, 3000)
    },
  })

  // 면접 데이터 로드 + 질문세트 설정
  useEffect(() => {
    if (interview && phase === 'preparing') {
      setInterview(interview.id, interview.questions)
      if (interview.questionSets?.length) {
        setQuestionSets(interview.questionSets)
      }
    }
  }, [interview, phase, setInterview, setQuestionSets])

  // 질문 변경 시 TTS로 읽기 (첫 질문은 greeting에서 처리하므로 제외)
  useEffect(() => {
    if (phase === 'ready' || phase === 'paused') {
      // 첫 질문은 greeting → ready 전환 시 이미 TTS 재생됨
      if (currentQuestionIndex === 0 && phase === 'ready') return

      const question = questions[currentQuestionIndex]
      if (question) {
        tts.speak(question.content)
        recordEvent('question_read_tts', currentQuestionIndex)
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentQuestionIndex, questions])

  // STT 콜백 등록
  useEffect(() => {
    stt.onFinalResult((segment: TranscriptSegment) => {
      addTranscript(segment)
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [stt.onFinalResult, addTranscript])

  // interim text 동기화
  useEffect(() => {
    setCurrentTranscript(stt.interimText)
  }, [stt.interimText, setCurrentTranscript])

  // 카메라 시작
  const handlePrepare = useCallback(async () => {
    await mediaStream.start()
  }, [mediaStream])

  useEffect(() => {
    if ((phase === 'greeting' || phase === 'ready') && !mediaStream.isActive) {
      handlePrepare()
    }
  }, [phase, mediaStream.isActive, handlePrepare])

  // 상태를 IN_PROGRESS로 변경
  useEffect(() => {
    if ((phase === 'greeting' || phase === 'ready') && interview?.status === 'READY') {
      updateStatus.mutate({ id: interview.id, data: { status: 'IN_PROGRESS' } })
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [phase, interview?.status, interviewId, updateStatus])

  // 면접 종료 (내부용)
  const handleFinishInterviewInternal = useCallback(async () => {
    if (!interview) return

    tts.stop()
    stt.stop()
    recordEvent('interview_finish', currentQuestionIndex)

    // 이벤트를 스토어에 저장
    const events = getEvents()
    events.forEach((e) => addInterviewEvent(e))

    const blob = await recorder.stop()
    setVideoBlob(blob)

    // 질문세트가 있으면 마지막 세트 업로드는 answer-flow에서 처리
    // 폴백: IndexedDB에 전체 blob 저장
    saveVideoBlob(interview.id, blob).catch(() => {})
    completeInterview()

    mediaStream.stop()

    updateStatus.mutate(
      { id: interview.id, data: { status: 'COMPLETED' } },
      {
        onSuccess: () => {
          navigate(`/interview/${interview.id}/complete`)
        },
        onError: () => {
          navigate(`/interview/${interview.id}/complete`)
        },
      },
    )
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [stt, recorder, setVideoBlob, completeInterview, updateStatus, interviewId, mediaStream, navigate, tts, recordEvent, currentQuestionIndex, getEvents, addInterviewEvent])

  // 폴백: "면접 종료" 버튼 (중도 포기 또는 시간 초과)
  const isFinishingRef = useRef(false)
  const handleFinishInterview = useCallback(async () => {
    if (isFinishingRef.current) return
    isFinishingRef.current = true
    pendingTtsActionRef.current = null
    await handleFinishInterviewInternal()
  }, [handleFinishInterviewInternal])

  // 클린업
  useEffect(() => {
    return () => {
      pendingTtsActionRef.current = null
      tts.stop()
      mediaStream.stop()
      stt.stop()
      reset()
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return {
    handlePrepare,
    handleStartAnswer,
    handleStopAnswer,
    handleFinishInterview,
    isTtsSpeaking: tts.isSpeaking,
    s3Upload,
    questionSets,
    currentQuestionSetIndex,
  }
}
