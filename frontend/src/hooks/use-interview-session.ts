import { useCallback, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { useInterviewStore } from '../stores/interview-store'
import { useUpdateInterviewStatus, useFollowUpQuestion } from '../hooks/use-interviews'
import { useVad } from './use-vad'
import { useTts } from './use-tts'
import { useThinkingTimeDetector } from './use-thinking-time-detector'
import { useInterviewEventRecorder } from './use-interview-event-recorder'
import type { Question, TranscriptSegment, VoiceEvent } from '../types/interview'

const TRANSITION_PHRASES = [
  '네, 다음 질문 드리겠습니다.',
  '네, 알겠습니다. 다음 질문입니다.',
  '잘 들었습니다. 다음 질문 드릴게요.',
  '네, 감사합니다. 다음 질문입니다.',
]

const CLOSING_PHRASES = [
  '네, 감사합니다. 이것으로 면접을 마치겠습니다. 수고하셨습니다.',
  '네, 잘 들었습니다. 면접을 마치겠습니다. 수고하셨습니다.',
]

const pickRandom = (arr: string[]) => arr[Math.floor(Math.random() * arr.length)]

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
    audioLevelRef: React.RefObject<number>
    start: (stream: MediaStream) => Promise<void>
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
  const pendingTtsActionRef = useRef<(() => void) | null>(null)
  const greetingPhaseRef = useRef(false)
  const ttsEndTimeRef = useRef(0)
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
    addInterviewEvent,
    reset,
  } = useInterviewStore()

  // 마운트 시 스토어 초기화 (재진입 대응)
  useEffect(() => {
    reset()
  }, [reset])

  // 이벤트 레코더
  const { recordEvent, getEvents, startRecording: startEventRecording } = useInterviewEventRecorder()

  // VAD 활성화: phase 기반 파생 (별도 state 불필요)
  const vadEnabled = phase === 'greeting' || phase === 'ready' || phase === 'recording' || phase === 'paused'

  // TTS 훅
  const tts = useTts({
    onStart: () => {
      // TTS 재생 중 STT/VAD 에코 방지 — VAD는 enabled=false가 아니라 phase 기반이므로
      // STT만 정지 (VAD는 TTS 중 audioLevel이 낮으므로 자연스럽게 비활성)
      stt.stop()
    },
    onEnd: () => {
      ttsEndTimeRef.current = Date.now()

      // 전환 TTS 완료 후 예약된 액션 실행 (nextQuestion / finish)
      if (pendingTtsActionRef.current) {
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

  // 실제 답변 시작 로직
  const doStartAnswer = useCallback(() => {
    if (!mediaStream.stream) return
    startRecording()
    if (!recorder.isRecording) {
      recorder.start(mediaStream.stream)
      startEventRecording()
    } else {
      recorder.resume()
    }
    stt.start(currentQuestionIndex)
    recordEvent('answer_start', currentQuestionIndex)
  }, [mediaStream.stream, startRecording, recorder, stt, currentQuestionIndex, recordEvent, startEventRecording])

  // VAD: 음성 감지 시 자동 녹음 시작/일시정지
  const { isActive: isVadActive, updateAudioLevel } = useVad({
    enabled: vadEnabled && !tts.isSpeaking,
    onSpeechStart: () => {
      // TTS 종료 직후 잔향 무시 (에코 방지 — 300ms 이내 감지는 무시)
      if (ttsEndTimeRef.current && Date.now() - ttsEndTimeRef.current < 300) {
        return
      }

      // 예약된 전환 액션이 있으면 취소 (다시 말하기 시작)
      pendingTtsActionRef.current = null

      const currentPhase = useInterviewStore.getState().phase
      if (currentPhase === 'greeting') {
        // greeting 중 음성 감지: 녹음 시작 (자기소개 녹음)
        doStartAnswer()
      } else if (currentPhase === 'ready' || currentPhase === 'paused') {
        doStartAnswer()
      }
    },
    onSpeechEnd: () => {
      // VAD onSpeechEnd는 자동 전환에 사용하지 않음 — "답변 완료" 버튼으로만 전환
    },
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

  // 오디오 레벨을 VAD에 전달 (ref 기반 — React 렌더 사이클 우회)
  useEffect(() => {
    let rafId: number
    const syncLevel = () => {
      updateAudioLevel(audio.audioLevelRef.current)
      rafId = requestAnimationFrame(syncLevel)
    }
    rafId = requestAnimationFrame(syncLevel)
    return () => cancelAnimationFrame(rafId)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [updateAudioLevel])

  // greeting phase 진입 시 인사 TTS (mediaStream 준비 후 시작)
  useEffect(() => {
    if (phase === 'greeting' && mediaStream.isActive) {
      greetingPhaseRef.current = true
      tts.speakWhenReady(
        '안녕하세요, 오늘 면접을 진행하게 된 AI 면접관입니다. 면접을 시작하기 전에 간단하게 자기소개 부탁드리겠습니다.',
      )
      recordEvent('greeting_tts', 0)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [phase, mediaStream.isActive])

  // phase가 greeting/ready가 되면 오디오 분석기 미리 시작 (VAD가 음성 감지할 수 있도록)
  useEffect(() => {
    if ((phase === 'greeting' || phase === 'ready' || phase === 'paused') && mediaStream.stream) {
      audio.start(mediaStream.stream)
    }
    // audio.start는 useCallback([])으로 안정적 — audio 객체 전체를 deps에 넣으면
    // audioLevel 변경마다 재실행되어 AudioContext가 무한 생성됨
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [phase, mediaStream.stream, audio.start])

  // 면접 데이터 로드
  useEffect(() => {
    if (interview && phase === 'preparing') {
      setInterview(interview.id, interview.questions)
    }
  }, [interview, phase, setInterview])

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

  // 음성 이벤트 콜백 등록
  useEffect(() => {
    audio.onVoiceEvent((event: VoiceEvent) => {
      addVoiceEvent(event)
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [audio.onVoiceEvent, addVoiceEvent])

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
    audio.stop()
    recordEvent('interview_finish', currentQuestionIndex)

    // 이벤트를 스토어에 저장
    const events = getEvents()
    events.forEach((e) => addInterviewEvent(e))

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
  }, [stt, audio, recorder, setVideoBlob, completeInterview, updateStatus, interviewId, mediaStream, navigate, tts, recordEvent, currentQuestionIndex, getEvents, addInterviewEvent])

  // "답변 완료" 버튼 — 전환 로직 통합 (VAD 자동 전환 대체)
  const handleStopAnswer = useCallback(() => {
    pendingTtsActionRef.current = null
    tts.stop()
    stopRecording()
    stt.stop()
    recorder.pause()
    recordEvent('manual_stop', currentQuestionIndex)

    const state = useInterviewStore.getState()

    // greeting 중 자기소개 완료 → ready로 전환 + 첫 질문 TTS
    if (greetingPhaseRef.current) {
      greetingPhaseRef.current = false
      useInterviewStore.setState({ phase: 'ready' })
      const firstQuestion = state.questions[0]
      if (firstQuestion) {
        tts.speak(`네, 감사합니다. 그럼 본격적으로 면접을 시작하겠습니다. 첫 번째 질문입니다. ${firstQuestion.content}`)
        recordEvent('question_read_tts', 0)
      }
      return
    }

    // 일반 질문: 답변 처리 + 전환 TTS → 다음 질문/종료
    processAnswer()

    const isLastQuestion = state.currentQuestionIndex >= state.questions.length - 1

    if (isLastQuestion) {
      pendingTtsActionRef.current = () => handleFinishInterviewInternal()
      tts.speak(pickRandom(CLOSING_PHRASES))
    } else {
      pendingTtsActionRef.current = () => nextQuestion()
      tts.speak(pickRandom(TRANSITION_PHRASES))
    }
  }, [stopRecording, stt, recorder, processAnswer, recordEvent, currentQuestionIndex, tts, nextQuestion, handleFinishInterviewInternal])

  // 폴백: "면접 종료" 버튼 (중도 포기 또는 시간 초과)
  const handleFinishInterview = useCallback(async () => {
    pendingTtsActionRef.current = null
    await handleFinishInterviewInternal()
  }, [handleFinishInterviewInternal])

  // 클린업
  useEffect(() => {
    return () => {
      pendingTtsActionRef.current = null
      tts.stop()
      mediaStream.stop()
      audio.stop()
      stt.stop()
      reset()
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return {
    handlePrepare,
    handleStartAnswer: doStartAnswer,
    handleStopAnswer,
    handleFinishInterview,
    isVadActive,
    isTtsSpeaking: tts.isSpeaking,
  }
}
