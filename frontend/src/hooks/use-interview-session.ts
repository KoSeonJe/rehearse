import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useInterviewStore } from '../stores/interview-store'
import { useUpdateInterviewStatus, useFollowUpQuestion } from '../hooks/use-interviews'
import { useVad } from './use-vad'
import type { Question, TranscriptSegment, VoiceEvent } from '../types/interview'

interface UseInterviewSessionParams {
  interviewId: string
  interview: { id: number; status: string; questions: Question[] } | undefined
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
  }
  stt: {
    interimText: string
    isSupported: boolean
    start: (questionIndex: number) => void
    stop: () => void
    onFinalResult: (callback: (segment: TranscriptSegment) => void) => void
  }
  audio: {
    audioLevel: number
    start: (stream: MediaStream) => void
    stop: () => void
    onVoiceEvent: (callback: (event: VoiceEvent) => void) => void
  }
}

export const useInterviewSession = ({
  interviewId,
  interview,
  mediaStream,
  recorder,
  stt,
  audio,
}: UseInterviewSessionParams) => {
  const navigate = useNavigate()
  const updateStatus = useUpdateInterviewStatus()
  const followUpMutation = useFollowUpQuestion()
  const [vadEnabled, setVadEnabled] = useState(false)
  const autoTransitionTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  const {
    questions,
    currentQuestionIndex,
    phase,
    setInterview,
    startRecording,
    stopRecording,
    setCurrentTranscript,
    addTranscript,
    addVoiceEvent,
    setVideoBlob,
    completeInterview,
    addFollowUpQuestion,
    setFollowUpLoading,
    setAutoTransitionMessage,
    nextQuestion,
  } = useInterviewStore()

  // 답변 처리 로직 (후속질문 요청)
  const processAnswer = useCallback(() => {
    const currentAnswer = useInterviewStore.getState().answers[currentQuestionIndex]
    const answerText = currentAnswer?.transcripts
      .filter((t) => t.isFinal)
      .map((t) => t.text)
      .join(' ')

    if (answerText && interview) {
      setFollowUpLoading(true)
      followUpMutation.mutate(
        {
          id: interview.id,
          data: {
            questionContent: questions[currentQuestionIndex].content,
            answerText,
          },
        },
        {
          onSuccess: (res) => {
            addFollowUpQuestion(currentQuestionIndex, res.data)
            setFollowUpLoading(false)
          },
          onError: () => {
            setFollowUpLoading(false)
          },
        },
      )
    }
  }, [currentQuestionIndex, interview, questions, followUpMutation, addFollowUpQuestion, setFollowUpLoading])

  // VAD: 음성 감지 시 자동 녹음 시작/일시정지
  const { isActive: isVadActive, updateAudioLevel } = useVad({
    enabled: vadEnabled,
    onSpeechStart: () => {
      // 자동 전환 타이머가 있으면 취소 (다시 말하기 시작)
      if (autoTransitionTimerRef.current) {
        clearTimeout(autoTransitionTimerRef.current)
        autoTransitionTimerRef.current = null
        setAutoTransitionMessage(null)
      }
      if (phase === 'ready' || phase === 'paused') {
        doStartAnswer()
      }
    },
    onSpeechEnd: () => {
      if (phase === 'recording') {
        stopRecording()
        stt.stop()
        recorder.pause()
        processAnswer()

        const state = useInterviewStore.getState()
        const isLastQuestion = state.currentQuestionIndex >= state.questions.length - 1

        // 2.5초 후 자동 전환
        setAutoTransitionMessage(
          isLastQuestion ? '면접을 마무리합니다...' : '다음 질문으로 넘어갑니다...',
        )

        autoTransitionTimerRef.current = setTimeout(() => {
          setAutoTransitionMessage(null)
          autoTransitionTimerRef.current = null

          if (isLastQuestion) {
            handleFinishInterviewInternal()
          } else {
            nextQuestion()
          }
        }, 2500)
      }
    },
  })

  // 오디오 레벨을 VAD에 전달
  useEffect(() => {
    updateAudioLevel(audio.audioLevel)
  }, [audio.audioLevel, updateAudioLevel])

  // 면접 데이터 로드
  useEffect(() => {
    if (interview && phase === 'preparing') {
      setInterview(interview.id, interview.questions)
    }
  }, [interview, phase, setInterview])

  // STT 콜백 등록
  useEffect(() => {
    stt.onFinalResult((segment: TranscriptSegment) => {
      addTranscript(segment)
    })
  }, [stt, addTranscript])

  // 음성 이벤트 콜백 등록
  useEffect(() => {
    audio.onVoiceEvent((event: VoiceEvent) => {
      addVoiceEvent(event)
    })
  }, [audio, addVoiceEvent])

  // interim text 동기화
  useEffect(() => {
    setCurrentTranscript(stt.interimText)
  }, [stt.interimText, setCurrentTranscript])

  // 카메라 시작
  const handlePrepare = useCallback(async () => {
    await mediaStream.start()
  }, [mediaStream])

  useEffect(() => {
    if (phase === 'ready' && !mediaStream.isActive) {
      handlePrepare()
    }
  }, [phase, mediaStream.isActive, handlePrepare])

  // 상태를 IN_PROGRESS로 변경
  useEffect(() => {
    if (phase === 'ready' && interview?.status === 'READY') {
      updateStatus.mutate({ id: interview.id, data: { status: 'IN_PROGRESS' } })
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [phase, interview?.status, interviewId, updateStatus])

  // 실제 답변 시작 로직
  const doStartAnswer = useCallback(() => {
    if (!mediaStream.stream) return
    startRecording()
    if (!recorder.isRecording) {
      recorder.start(mediaStream.stream)
    } else {
      recorder.resume()
    }
    stt.start(currentQuestionIndex)
    audio.start(mediaStream.stream)
  }, [mediaStream.stream, startRecording, recorder, stt, audio, currentQuestionIndex])

  // "준비 완료" 버튼 → VAD 활성화 + 답변 시작
  const handleStartAnswer = useCallback(() => {
    setVadEnabled(true)
    doStartAnswer()
  }, [doStartAnswer])

  // 수동 답변 완료 + 후속질문 요청 (자동 이동 없음)
  const handleStopAnswer = useCallback(() => {
    // 자동 전환 타이머가 있으면 취소
    if (autoTransitionTimerRef.current) {
      clearTimeout(autoTransitionTimerRef.current)
      autoTransitionTimerRef.current = null
      setAutoTransitionMessage(null)
    }

    setVadEnabled(false)
    stopRecording()
    stt.stop()
    recorder.pause()
    processAnswer()
  }, [stopRecording, stt, recorder, processAnswer, setAutoTransitionMessage])

  // 면접 종료 (내부용 — VAD 자동 전환에서 호출)
  const handleFinishInterviewInternal = useCallback(async () => {
    if (!interview) return

    setVadEnabled(false)
    stt.stop()
    audio.stop()

    const blob = await recorder.stop()
    setVideoBlob(blob)
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
  }, [stt, audio, recorder, setVideoBlob, completeInterview, updateStatus, interviewId, mediaStream, navigate])

  // 면접 종료 (수동 — 버튼 클릭)
  const handleFinishInterview = useCallback(async () => {
    // 자동 전환 타이머가 있으면 취소
    if (autoTransitionTimerRef.current) {
      clearTimeout(autoTransitionTimerRef.current)
      autoTransitionTimerRef.current = null
      setAutoTransitionMessage(null)
    }

    await handleFinishInterviewInternal()
  }, [handleFinishInterviewInternal, setAutoTransitionMessage])

  // 클린업
  useEffect(() => {
    return () => {
      if (autoTransitionTimerRef.current) {
        clearTimeout(autoTransitionTimerRef.current)
      }
      mediaStream.stop()
      audio.stop()
      stt.stop()
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return {
    handlePrepare,
    handleStartAnswer,
    handleStopAnswer,
    handleFinishInterview,
    isVadActive,
  }
}
