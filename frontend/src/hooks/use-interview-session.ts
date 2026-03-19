import { useCallback, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { useInterviewStore } from '@/stores/interview-store'
import { useUpdateInterviewStatus, useSkipRemainingQuestionSets } from '@/hooks/use-interviews'
import { useTts } from '@/hooks/use-tts'
import { useInterviewEventRecorder } from '@/hooks/use-interview-event-recorder'
import { useInterviewGreeting } from '@/hooks/use-interview-greeting'
import { useAnswerFlow } from '@/hooks/use-answer-flow'
import { useAudioCapture } from '@/hooks/use-audio-capture'
import { saveVideoBlob, deleteVideoBlob } from '@/lib/video-storage'
import { useS3Upload } from '@/hooks/use-s3-upload'
import { apiClient } from '@/lib/api-client'
import type { QuestionSetData, ApiResponse, UploadUrlResponse } from '@/types/interview'

interface UseInterviewSessionParams {
  interviewId: string
  interview: { id: number; status: string; questionSets?: QuestionSetData[] } | undefined
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
}

export const useInterviewSession = ({
  interviewId,
  interview,
  mediaStream,
  recorder,
}: UseInterviewSessionParams) => {
  const navigate = useNavigate()
  const updateStatus = useUpdateInterviewStatus()
  const skipRemaining = useSkipRemainingQuestionSets()
  const s3UploadForFinish = useS3Upload()
  const pendingTtsActionRef = useRef<(() => void) | null>(null)
  const greetingPhaseRef = useRef(false)
  const audioCapture = useAudioCapture()
  const {
    questions,
    currentQuestionIndex,
    phase,
    questionSets,
    currentQuestionSetIndex,
    setInterview,
    setQuestionSets,
    setVideoBlob,
    completeInterview,
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
      // TTS 재생 중에는 별도 처리 불필요 (STT 제거됨)
    },
    onEnd: () => {
      // 전환 TTS 완료 후 예약된 액션 실행 (nextQuestion / finishing)
      if (pendingTtsActionRef.current) {
        const state = useInterviewStore.getState()
        const actionablePhases = new Set(['paused', 'recording', 'finishing'])
        if (!actionablePhases.has(state.phase)) {
          pendingTtsActionRef.current = null
          return
        }
        const action = pendingTtsActionRef.current
        pendingTtsActionRef.current = null
        action()
        return
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
    audioCapture,
    tts,
    recordEvent,
    startEventRecording,
    greetingPhaseRef,
    completeGreeting,
    pendingTtsActionRef,
  })

  // 면접 데이터 로드 + 질문세트 설정 (questionSets에서 Question[] 도출)
  useEffect(() => {
    if (interview && phase === 'preparing') {
      const qs = interview.questionSets ?? []
      const derivedQuestions = qs.map((qSet, idx) => {
        const mainQ = qSet.questions.find((q) => q.questionType === 'MAIN')
        return {
          id: mainQ?.id ?? qSet.id,
          content: mainQ?.questionText ?? '',
          category: qSet.category,
          order: idx,
        }
      })
      setInterview(interview.id, derivedQuestions)
      if (qs.length) {
        setQuestionSets(qs)
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
    const state = useInterviewStore.getState()
    const isFromFinishing = state.phase === 'finishing'
    recordEvent('interview_finish', state.currentQuestionIndex)

    // 이벤트를 스토어에 저장
    const events = getEvents()
    events.forEach((e) => addInterviewEvent(e))

    // finishing phase: 정상 종료 경로 — recorder는 이미 stop되고 S3 업로드도 완료/진행 중
    // 그 외 (중도 포기): recorder stop + S3 업로드 필요
    if (!isFromFinishing) {
      let blob: Blob
      try {
        blob = await recorder.stop()
      } catch {
        blob = new Blob([], { type: 'video/webm' })
      }
      setVideoBlob(blob)

      const hasQs = state.questionSets.length > 0
      if (hasQs) {
        const currentSet = state.questionSets[state.currentQuestionSetIndex]
        if (currentSet) {
          const answers = state.questionSetAnswers.get(currentSet.id) ?? []
          const hasAnswers = answers.length > 0

          if (hasAnswers) {
            await saveVideoBlob(interview.id, blob, currentSet.id).catch(() => {})

            try {
              await apiClient.post(
                `/api/v1/interviews/${interview.id}/question-sets/${currentSet.id}/answers`,
                { answers },
              )
            } catch (err) {
              console.error('[면접종료] 답변 타임스탬프 저장 실패:', err)
            }

            try {
              const urlRes = await apiClient.post<ApiResponse<UploadUrlResponse>>(
                `/api/v1/interviews/${interview.id}/question-sets/${currentSet.id}/upload-url`,
                { contentType: blob.type || 'video/webm' },
              )
              await s3UploadForFinish.upload(blob, urlRes.data.uploadUrl)
              deleteVideoBlob(interview.id, currentSet.id).catch(() => {})
            } catch (err) {
              console.error('[면접종료] S3 업로드 실패:', err)
            }
          }
        }
      } else {
        saveVideoBlob(interview.id, blob).catch(() => {})
      }
    }

    // 미응답 세트 SKIPPED 처리 (finishing/중도 포기 공통)
    const hasQs = state.questionSets.length > 0
    if (hasQs) {
      try {
        await skipRemaining.mutateAsync(interview.id)
      } catch (err) {
        console.error('[면접종료] 미응답 세트 스킵 실패:', err)
      }
    }

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
  }, [recorder, setVideoBlob, completeInterview, updateStatus, interviewId, mediaStream, navigate, tts, recordEvent, getEvents, addInterviewEvent, skipRemaining, s3UploadForFinish])

  // 시간 만료 → recorder/audioCapture 정지 + finishing phase 전환
  const handleTimeExpired = useCallback(() => {
    const state = useInterviewStore.getState()
    if (state.phase === 'finishing' || state.phase === 'completed') return
    tts.stop()
    pendingTtsActionRef.current = null
    recorder.pause()
    audioCapture.stop()
    useInterviewStore.getState().setPhase('finishing')
  }, [tts, recorder, audioCapture])

  // 폴백: "면접 종료" 버튼 (중도 포기 또는 finishing phase에서 클릭)
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
      reset()
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return {
    handlePrepare,
    handleStartAnswer,
    handleStopAnswer,
    handleFinishInterview,
    handleTimeExpired,
    isTtsSpeaking: tts.isSpeaking,
    s3Upload,
    questionSets,
    currentQuestionSetIndex,
  }
}
