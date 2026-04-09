import { useCallback, useEffect, useRef, useState, type SetStateAction } from 'react'
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

  // 언마운트 가드 — 종료 진행 중 페이지 이동/핫리로드 시 setState 및 orphan Promise 방지.
  const mountedRef = useRef(true)

  // 질문세트별 in-flight 업로드 Promise — 면접 종료 시 Promise.allSettled 로 대기.
  // fire-and-forget 패턴 대신 실제 Promise 를 추적해 폴링 루프 안티패턴을 제거한다.
  const uploadPromisesRef = useRef<Map<number, Promise<void>>>(new Map())

  const registerUploadPromise = useCallback((questionSetId: number, promise: Promise<void>) => {
    const wrapped = promise.finally(() => {
      // 완료된 Promise 는 Map 에서 제거 — 다음 종료 시 stale 참조가 남지 않도록.
      if (uploadPromisesRef.current.get(questionSetId) === wrapped) {
        uploadPromisesRef.current.delete(questionSetId)
      }
    })
    uploadPromisesRef.current.set(questionSetId, wrapped)
  }, [])

  // 면접 종료 진행 상황 — FinishingOverlay 에서 구독.
  // null 이면 오버레이 숨김, 값이 있으면 "안전하게 종료 중" UI 표시.
  const [finishingProgress, setFinishingProgressRaw] = useState<{
    stage: 'uploading' | 'saving' | 'finalizing'
    total: number
    completed: number
  } | null>(null)

  // 언마운트 이후 setState 는 무시 — 페이지 이동 중 finally 콜백 등에서 안전하게 호출 가능.
  const setFinishingProgress = useCallback(
    (update: SetStateAction<typeof finishingProgress>) => {
      if (!mountedRef.current) return
      setFinishingProgressRaw(update)
    },
    [],
  )

  // 업로드 복구 실패 시 사용자 확인 요청 — UploadRecoveryDialog 가 구독.
  // resolve(true) → 그래도 종료 / resolve(false) → 면접 계속.
  const [uploadFailureState, setUploadFailureStateRaw] = useState<{
    failedCount: number
    resolve: (proceed: boolean) => void
  } | null>(null)

  // 언마운트 시 pending resolve 를 false 로 풀기 위해 최신 상태를 ref 로도 보관.
  const uploadFailureStateRef = useRef<{
    failedCount: number
    resolve: (proceed: boolean) => void
  } | null>(null)

  const setUploadFailureState = useCallback(
    (update: SetStateAction<typeof uploadFailureState>) => {
      if (!mountedRef.current) return
      setUploadFailureStateRaw((prev) => {
        const next = typeof update === 'function' ? update(prev) : update
        uploadFailureStateRef.current = next
        return next
      })
    },
    [],
  )
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
    registerUploadPromise,
  })

  // 면접 데이터 로드 + 질문세트 설정 (questionSets에서 Question[] 도출)
  // 이어하기: 이미 업로드/분석 진행된 세트(analysisStatus !== 'PENDING')는 건너뛰고
  // 첫 번째 PENDING 세트부터 시작한다.
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
      const firstPendingIdx = qs.findIndex((qSet) => qSet.analysisStatus === 'PENDING')
      const startIdx = firstPendingIdx >= 0 ? firstPendingIdx : 0
      const isResume = startIdx > 0
      setInterview(interview.id, derivedQuestions, startIdx)
      if (qs.length) {
        setQuestionSets(qs)
      }
      // 이어하기: 자기소개(greeting)는 이미 완료 → ready로 바로 전환
      if (isResume) {
        useInterviewStore.getState().setPhase('ready')
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
  //
  // 단계 요약:
  //   1) 현재 세트 업로드 선점 (중도포기 경로만) — registerUploadPromise 경유
  //   2) in-flight 업로드 Promise 전부 대기 (Promise.allSettled) — 폴링 루프 제거
  //   3) 실패/미시작 세트만 IndexedDB 에서 복구 재시도
  //   4) 여전히 실패한 세트가 있으면 UploadRecoveryDialog 로 사용자 확인
  //   5) skipRemaining → completeInterview → navigate
  const handleFinishInterviewInternal = useCallback(async () => {
    if (!interview) return

    tts.stop()
    const state = useInterviewStore.getState()
    const isFromFinishing = state.phase === 'finishing'
    recordEvent('interview_finish', state.currentQuestionIndex)

    // 이벤트를 스토어에 저장
    const events = getEvents()
    events.forEach((e) => addInterviewEvent(e))

    // 오버레이 선표시 — 실제 작업량은 이후 단계에서 설정
    setFinishingProgress({ stage: 'uploading', total: 0, completed: 0 })

    // ── 1. 현재 세트 업로드 선점 (중도포기 경로) ─────────────────────
    // finishing phase: 정상 종료 경로 — recorder 는 이미 stop 되었고 마지막 세트 업로드도
    // handleQuestionSetComplete 에서 이미 등록됨. 별도 처리 불필요.
    // 중도 포기: recorder.stop() 후 현재 세트의 업로드를 registerUploadPromise 경로로 등록.
    if (!isFromFinishing) {
      let blob: Blob
      try {
        blob = await recorder.stop()
      } catch {
        blob = new Blob([], { type: 'video/webm' })
      }
      setVideoBlob(blob)

      const freshState = useInterviewStore.getState()
      const hasQs = freshState.questionSets.length > 0
      if (hasQs) {
        const currentSet = freshState.questionSets[freshState.currentQuestionSetIndex]
        if (currentSet) {
          const answers = freshState.questionSetAnswers.get(currentSet.id) ?? []
          const hasAnswers = answers.length > 0
          const currentStatus = freshState.uploadStatus.get(currentSet.id)
          const alreadyUploaded = currentStatus === 'completed' || currentStatus === 'uploading'

          if (hasAnswers && !alreadyUploaded) {
            // 'uploading' 선점 — 다른 경로에서 중복 업로드 못 하게 가드
            setUploadStatus(currentSet.id, 'uploading')

            // 현재 세트 업로드 Promise 를 만들고 세션에 등록 — 아래 2 단계에서 함께 대기
            const currentUploadPromise = (async () => {
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
            })()
            registerUploadPromise(currentSet.id, currentUploadPromise)
          }
        }
      } else {
        saveVideoBlob(interview.id, blob).catch((err: unknown) => {
          console.error('[S3 업로드] 실패:', err)
        })
      }
    }

    // ── 2. in-flight 업로드 Promise 전부 대기 ─────────────────────
    // fire-and-forget 안티패턴 제거: 실제 Promise 를 allSettled 로 기다림.
    // 진행 중인 업로드가 하나씩 끝날 때마다 finishingProgress.completed 증가 → 오버레이 실시간 업데이트.
    // allSettled 사용 이유: 내부 Promise 는 현재 catch 로 모든 에러를 흡수하지만, 향후
    // 에러가 새어 나올 경우에도 전체 대기가 short-circuit 되지 않도록 방어.
    const inFlightEntries = Array.from(uploadPromisesRef.current.entries())
    if (inFlightEntries.length > 0) {
      setFinishingProgress({ stage: 'uploading', total: inFlightEntries.length, completed: 0 })
      await Promise.allSettled(
        inFlightEntries.map(([, p]) =>
          p.finally(() => {
            setFinishingProgress((prev) =>
              prev ? { ...prev, completed: prev.completed + 1 } : prev,
            )
          }),
        ),
      )
    }

    // ── 3. 실패 / 미시작 세트만 IndexedDB 에서 복구 재시도 ──────────
    // 'uploading' 은 위 Promise 대기 이후 'completed' 또는 'failed' 로 정착되어 있어야 함.
    // 그럼에도 'uploading' 이 남아 있다면 예기치 않은 경합이므로 방어적으로 복구 대상에서 제외.
    const afterCurrentState = useInterviewStore.getState()
    if (afterCurrentState.questionSets.length > 0) {
      const pendingPriorSets = afterCurrentState.questionSets.filter((qs) => {
        const status = afterCurrentState.uploadStatus.get(qs.id)
        if (status === 'completed' || status === 'uploading') return false
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

      // ── 4. 여전히 실패한 세트가 있으면 UploadRecoveryDialog 로 사용자 확인 ──
      const finalState = useInterviewStore.getState()
      const stillPending = finalState.questionSets.filter((qs) => {
        const hasAnswers = (finalState.questionSetAnswers.get(qs.id) ?? []).length > 0
        if (!hasAnswers) return false
        const status = finalState.uploadStatus.get(qs.id)
        return status !== 'completed'
      })
      if (stillPending.length > 0) {
        // 모달 표시 + 사용자 결정 대기 — 오버레이는 임시로 숨겨 모달을 방해하지 않도록
        setFinishingProgress(null)
        const proceed = await new Promise<boolean>((resolve) => {
          setUploadFailureState({ failedCount: stillPending.length, resolve })
        })
        setUploadFailureState(null)
        if (!proceed) {
          // 사용자 취소 — 상태 변경 없이 면접 화면 유지, 재시도 허용
          isFinishingRef.current = false
          return
        }
        // 진행 결정 → 오버레이 복구
        setFinishingProgress({ stage: 'saving', total: 0, completed: 0 })
      }
    }

    // ── 5. 미응답 세트 SKIPPED 처리 ────────────────────────────────
    setFinishingProgress({ stage: 'saving', total: 0, completed: 0 })
    const hasQs = state.questionSets.length > 0
    if (hasQs) {
      try {
        await skipRemaining.mutateAsync(interview.id)
      } catch (err) {
        console.error('[면접종료] 미응답 세트 스킵 실패:', err)
      }
    }

    setFinishingProgress({ stage: 'finalizing', total: 0, completed: 0 })
    completeInterview()
    mediaStream.stop()

    updateStatus.mutate(
      { id: interview.id, data: { status: 'COMPLETED' } },
      {
        onSuccess: () => {
          setFinishingProgress(null)
          if (!interview.publicId) {
            console.error('[면접종료] publicId가 없습니다')
            navigate('/', { replace: true })
            return
          }
          navigate(`/interview/${interview.publicId}/analysis`, { replace: true })
        },
        onError: () => {
          setFinishingProgress(null)
          if (!interview.publicId) {
            console.error('[면접종료] publicId가 없습니다')
            navigate('/', { replace: true })
            return
          }
          navigate(`/interview/${interview.publicId}/analysis`, { replace: true })
        },
      },
    )
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [recorder, setVideoBlob, setUploadStatus, completeInterview, updateStatus, interview?.publicId, interviewId, mediaStream, navigate, tts, recordEvent, getEvents, addInterviewEvent, skipRemaining, s3UploadForFinish, registerUploadPromise])

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
    // ref 캡처 — effect cleanup 실행 시점에 ref 가 변해 있을 수 있으나
    // Map 인스턴스 자체는 훅 lifetime 동안 고정이므로 지금 시점 참조를 그대로 사용한다.
    const uploadPromises = uploadPromisesRef.current
    return () => {
      mountedRef.current = false
      // 언마운트 시 pending 사용자 확인 모달이 있으면 "계속하기(false)" 로 resolve 해서
      // handleFinishInterviewInternal 의 await 가 영구 hang 되지 않도록 한다.
      if (uploadFailureStateRef.current) {
        try {
          uploadFailureStateRef.current.resolve(false)
        } catch {
          // noop
        }
        uploadFailureStateRef.current = null
      }
      uploadPromises.clear()
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
    // 종료 진행 UX 용 상태 — interview-page.tsx 가 구독해 오버레이/모달을 렌더
    finishingProgress,
    uploadFailureState,
  }
}
