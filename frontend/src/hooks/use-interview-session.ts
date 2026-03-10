import { useCallback, useEffect, useState } from 'react'
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
  } = useInterviewStore()

  // VAD: 음성 감지 시 자동 녹음 시작/일시정지
  const { isActive: isVadActive, updateAudioLevel } = useVad({
    enabled: vadEnabled,
    onSpeechStart: () => {
      if (phase === 'ready' || phase === 'paused') {
        doStartAnswer()
      }
    },
    onSpeechEnd: () => {
      if (phase === 'recording') {
        stopRecording()
        stt.stop()
        recorder.pause()
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

  // 답변 완료 + 후속질문 요청
  const handleStopAnswer = useCallback(() => {
    setVadEnabled(false)
    stopRecording()
    stt.stop()
    recorder.pause()

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
  }, [stopRecording, stt, recorder, currentQuestionIndex, interview, questions, followUpMutation, addFollowUpQuestion, setFollowUpLoading])

  // 면접 종료
  const handleFinishInterview = useCallback(async () => {
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

  // 클린업
  useEffect(() => {
    return () => {
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
