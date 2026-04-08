import { useCallback, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { useInterviewStore } from '@/stores/interview-store'
import { useUpdateInterviewStatus, useSkipRemainingQuestionSets } from '@/hooks/use-interviews'
import { useTts } from '@/hooks/use-tts'
import { useInterviewEventRecorder } from '@/hooks/use-interview-event-recorder'
import { useInterviewGreeting } from '@/hooks/use-interview-greeting'
import { useAnswerFlow } from '@/hooks/use-answer-flow'
import { useAudioCapture } from '@/hooks/use-audio-capture'
import { saveVideoBlob, deleteVideoBlob, loadVideoBlob } from '@/lib/video-storage'
import { useS3Upload } from '@/hooks/use-s3-upload'
import { apiClient } from '@/lib/api-client'
import type { QuestionSetData, ApiResponse, UploadUrlResponse } from '@/types/interview'

interface UseInterviewSessionParams {
  interviewId: string
  interview: { id: number; publicId: string; status: string; questionSets?: QuestionSetData[] } | undefined
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
  const isFinishingRef = useRef(false)
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
    setUploadStatus,
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
  const { handleStartAnswer, handleStopAnswer, cancelFollowUp, s3Upload } = useAnswerFlow({
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

      // recorder.stop 대기 사이 addAnswerTimestamp 가 추가됐을 수 있어 최신 상태 재조회
      const freshState = useInterviewStore.getState()
      const hasQs = freshState.questionSets.length > 0
      if (hasQs) {
        const currentSet = freshState.questionSets[freshState.currentQuestionSetIndex]
        if (currentSet) {
          const answers = freshState.questionSetAnswers.get(currentSet.id) ?? []
          const hasAnswers = answers.length > 0

          // 이미 업로드 완료/진행 중인 경우 중복 업로드 방지
          // (confirm 취소 후 재시도 케이스 + 답변 완료 직후 종료로 uploadAsync 가 먼저 선점한 케이스)
          const currentStatus = freshState.uploadStatus.get(currentSet.id)
          const alreadyUploaded = currentStatus === 'completed' || currentStatus === 'uploading'
          if (hasAnswers && !alreadyUploaded) {
            setUploadStatus(currentSet.id, 'uploading')
            await saveVideoBlob(interview.id, blob, currentSet.id).catch((err: unknown) => {
              console.error('[S3 업로드] 로컬 저장 실패:', err)
            })

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
              setUploadStatus(currentSet.id, 'completed')
              deleteVideoBlob(interview.id, currentSet.id).catch(() => {})
            } catch (err) {
              console.error('[면접종료] S3 업로드 실패:', err)
              setUploadStatus(currentSet.id, 'failed')
            }
          }
        }
      } else {
        saveVideoBlob(interview.id, blob).catch((err: unknown) => {
          console.error('[S3 업로드] 실패:', err)
        })
      }
    }

    // ── 이전 세트 업로드 복구 루프 ─────────────────────────────────
    // 시나리오: qs 456 답변 완료 → 전환 TTS → handleQuestionSetComplete 의 fire-and-forget
    // uploadAsync 가 시작되는 사이 사용자가 즉시 "면접 종료" 클릭 → 이전 세트의 업로드가
    // 완료되지 못한 채 페이지 이동 → S3 파일 없음 → PENDING_UPLOAD 영구 고정.
    // 이를 막기 위해 COMPLETED 전환 전에 모든 이전 세트의 uploadStatus 를 점검하고
    // IndexedDB 의 blob 으로 재업로드를 시도한다.

    // 1단계: 진행 중('uploading')인 이전 세트가 있으면 최대 10초간 대기 — 원본 fire-and-forget
    //        업로드가 자연 종료될 수 있도록 한다 (중복 업로드 & /answers 중복 POST 회피).
    //        10초는 일반적인 LTE/3G 에서 20MB 블롭 + 지수백오프 한 사이클을 수용하는 보수값.
    const isPriorUploading = () => {
      const s = useInterviewStore.getState()
      return s.questionSets.some((qs, idx) => {
        if (!isFromFinishing && idx === s.currentQuestionSetIndex) return false
        return s.uploadStatus.get(qs.id) === 'uploading'
      })
    }
    if (isPriorUploading()) {
      for (let i = 0; i < 100; i++) {
        await new Promise((resolve) => setTimeout(resolve, 100))
        if (!isPriorUploading()) break
      }
    }

    const afterCurrentState = useInterviewStore.getState()
    if (afterCurrentState.questionSets.length > 0) {
      const pendingPriorSets = afterCurrentState.questionSets.filter((qs, idx) => {
        // 중도포기 경로에서 현재 세트는 위 블록에서 이미 처리됨 → 이중 업로드 방지
        if (!isFromFinishing && idx === afterCurrentState.currentQuestionSetIndex) return false
        const status = afterCurrentState.uploadStatus.get(qs.id)
        // 'completed' 는 건너뛰고, 'uploading' 은 위 대기 루프 이후에도 여전하면 죽은 것으로 간주해 재시도
        if (status === 'completed') return false
        const answers = afterCurrentState.questionSetAnswers.get(qs.id) ?? []
        return answers.length > 0
      })

      for (const qs of pendingPriorSets) {
        try {
          const recoveredBlob = await loadVideoBlob(interview.id, qs.id)
          if (!recoveredBlob || recoveredBlob.size === 0) {
            console.warn('[면접종료] IndexedDB blob 없음 — 재업로드 포기:', qs.id)
            setUploadStatus(qs.id, 'failed')
            continue
          }
          // loadVideoBlob 대기 중 원본 fire-and-forget 이 성공했을 수 있음 → 재확인
          const recheckStatus = useInterviewStore.getState().uploadStatus.get(qs.id)
          if (recheckStatus === 'completed') continue
          setUploadStatus(qs.id, 'uploading')
          const answers = afterCurrentState.questionSetAnswers.get(qs.id) ?? []
          try {
            await apiClient.post(
              `/api/v1/interviews/${interview.id}/question-sets/${qs.id}/answers`,
              { answers },
            )
          } catch (err) {
            console.error('[면접종료] 재업로드 타임스탬프 저장 실패:', qs.id, err)
          }
          const urlRes = await apiClient.post<ApiResponse<UploadUrlResponse>>(
            `/api/v1/interviews/${interview.id}/question-sets/${qs.id}/upload-url`,
            { contentType: recoveredBlob.type || 'video/webm' },
          )
          await s3UploadForFinish.upload(recoveredBlob, urlRes.data.uploadUrl)
          setUploadStatus(qs.id, 'completed')
          deleteVideoBlob(interview.id, qs.id).catch(() => {})
        } catch (err) {
          console.error('[면접종료] 이전 세트 재업로드 실패:', qs.id, err)
          setUploadStatus(qs.id, 'failed')
        }
      }

      // 재업로드 시도 후에도 실패한 세트가 있으면 사용자에게 경고
      const finalState = useInterviewStore.getState()
      const stillPending = finalState.questionSets.filter((qs) => {
        const hasAnswers = (finalState.questionSetAnswers.get(qs.id) ?? []).length > 0
        if (!hasAnswers) return false
        const status = finalState.uploadStatus.get(qs.id)
        return status !== 'completed'
      })
      if (stillPending.length > 0) {
        const proceed = window.confirm(
          `답변 영상 ${stillPending.length}개가 업로드되지 않았습니다.\n` +
          `네트워크를 확인한 뒤 다시 시도하는 것을 권장합니다.\n\n` +
          `그래도 면접을 종료하시겠습니까? (종료 후에는 해당 답변의 피드백이 생성되지 않을 수 있습니다)`,
        )
        if (!proceed) {
          // 사용자 취소 — 상태 변경 없이 면접 화면 유지, 재시도 허용
          isFinishingRef.current = false
          return
        }
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
          if (!interview.publicId) {
            console.error('[면접종료] publicId가 없습니다')
            navigate('/')
            return
          }
          navigate(`/interview/${interview.publicId}/analysis`)
        },
        onError: () => {
          if (!interview.publicId) {
            console.error('[면접종료] publicId가 없습니다')
            navigate('/')
            return
          }
          navigate(`/interview/${interview.publicId}/analysis`)
        },
      },
    )
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [recorder, setVideoBlob, setUploadStatus, completeInterview, updateStatus, interview?.publicId, interviewId, mediaStream, navigate, tts, recordEvent, getEvents, addInterviewEvent, skipRemaining, s3UploadForFinish])

  // 시간 만료 → recorder/audioCapture 정지 + finishing phase 전환
  const handleTimeExpired = useCallback(() => {
    const state = useInterviewStore.getState()
    if (state.phase === 'finishing' || state.phase === 'completed') return
    pendingTtsActionRef.current = null
    tts.stop()
    // in-flight 후속질문 mutation 이 있다면 abort (응답 뒤늦게 와도 상태 오염 방지)
    cancelFollowUp()
    // Always Recording: recorder는 finishing → stop()에서 정리됨
    audioCapture.stop()
    useInterviewStore.getState().setPhase('finishing')
  }, [tts, audioCapture, cancelFollowUp])

  // 면접 종료 에스케이프 해치 — 어떤 중간 상태에서도 매끄럽게 종료
  // 호출 경로:
  //   1) finishing phase 에서 [면접 종료하기] 버튼 클릭 (정상 종료)
  //   2) recording/paused/ready/greeting 중 상시 노출된 [종료] 버튼 클릭 (중도 포기)
  //   3) 후속질문 생성/TTS 재생/질문 전환 중 에스케이프
  const handleFinishInterview = useCallback(async () => {
    if (isFinishingRef.current) return
    isFinishingRef.current = true

    // 0. 예약된 TTS onEnd 액션 제거 (뒤늦게 와도 실행되지 않도록)
    pendingTtsActionRef.current = null

    // 1. TTS 즉시 중단 (세대 가드에 의해 유령 발화 차단)
    tts.stop()

    // 2. in-flight 후속질문 mutation abort + 로딩 플래그 정리
    cancelFollowUp()

    // 3. 기존 정상 종료 흐름 (recorder stop, S3 업로드, status COMPLETED, navigate)
    await handleFinishInterviewInternal()
  }, [handleFinishInterviewInternal, tts, cancelFollowUp])

  // 클린업
  useEffect(() => {
    return () => {
      pendingTtsActionRef.current = null
      tts.stop()
      cancelFollowUp()
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
