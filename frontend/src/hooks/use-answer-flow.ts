import { useCallback, type MutableRefObject } from 'react'
import { useInterviewStore, MAX_FOLLOWUP_ROUNDS } from '@/stores/interview-store'
import { useFollowUpQuestion } from '@/hooks/use-interviews'
import { useS3Upload } from '@/hooks/use-s3-upload'
import { apiClient } from '@/lib/api-client'
import { saveVideoBlob, deleteVideoBlob } from '@/lib/video-storage'
import type {
  InterviewEventType,
  ApiResponse,
  UploadUrlResponse,
  QuestionSetData,
} from '@/types/interview'

const TRANSITION_PHRASES = [
  '네, 다음 질문 드리겠습니다.',
  '네, 알겠습니다. 다음 질문입니다.',
  '잘 들었습니다. 다음 질문 드릴게요.',
  '네, 감사합니다. 다음 질문입니다.',
]

const SET_TRANSITION_PHRASES = [
  '네, 다음 주제로 넘어가겠습니다.',
  '좋습니다. 다음 질문 세트를 시작하겠습니다.',
  '잘 답변해주셨습니다. 다른 주제로 넘어가 보죠.',
]

const CLOSING_PHRASES = [
  '네, 감사합니다. 이것으로 면접을 마치겠습니다. 수고하셨습니다.',
  '네, 잘 들었습니다. 면접을 마치겠습니다. 수고하셨습니다.',
]

const pickRandom = (arr: string[]) => arr[Math.floor(Math.random() * arr.length)]

interface UseAnswerFlowParams {
  interview: { id: number; publicId: string; status: string; questionSets?: QuestionSetData[] } | undefined
  mediaStream: { stream: MediaStream | null }
  recorder: {
    isRecording: boolean
    start: (stream: MediaStream) => void
    stop: () => Promise<Blob>
    pause: () => void
    resume: () => void
    restart: (stream: MediaStream) => Promise<Blob>
  }
  audioCapture: {
    start: (stream: MediaStream) => void
    stop: () => Promise<Blob>
  }
  tts: {
    speak: (text: string) => void
    stop: () => void
  }
  recordEvent: (type: InterviewEventType, questionIndex: number) => void
  startEventRecording: () => void
  greetingPhaseRef: MutableRefObject<boolean>
  completeGreeting: () => void
  pendingTtsActionRef: MutableRefObject<(() => void) | null>
}

export const useAnswerFlow = ({
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
}: UseAnswerFlowParams) => {
  const followUpMutation = useFollowUpQuestion()
  const s3Upload = useS3Upload()

  const {
    startRecording,
    stopRecording,
    setCurrentFollowUp,
    completeFollowUpRound,
    resetFollowUpState,
    setFollowUpLoading,
    nextQuestion,
    nextQuestionSet,
    addAnswerTimestamp,
    setUploadStatus,
    setPhase,
    setQuestionSetRecordingStartTime,
    addQuestionToSet,
  } = useInterviewStore()

  const hasQuestionSets = !!interview?.questionSets?.length

  // 현재 답변 텍스트 수집 — 후속질문 중엔 offset 이후 transcript만 반환
  const getCurrentAnswerText = useCallback(() => {
    const state = useInterviewStore.getState()
    const currentAnswer = state.answers[state.currentQuestionIndex]
    const offset = state.currentFollowUp !== null ? state.followUpTranscriptOffset : 0
    return currentAnswer?.transcripts
      .filter((t) => t.isFinal)
      .slice(offset)
      .map((t) => t.text)
      .join(' ') ?? ''
  }, [])

  // 질문세트 완료 시 업로드 파이프라인 (백그라운드)
  const handleQuestionSetComplete = useCallback(async (questionSetId: number) => {
    if (!interview || !mediaStream.stream) return

    const state = useInterviewStore.getState()
    const isLastSet = state.currentQuestionSetIndex >= state.questionSets.length - 1

    // 1. recorder stop → blob
    let blob: Blob
    if (isLastSet) {
      blob = await recorder.stop()
    } else {
      blob = await recorder.restart(mediaStream.stream)
    }

    // 2. IndexedDB 폴백 저장
    await saveVideoBlob(interview.id, blob, questionSetId).catch(() => {})

    // 3. 답변 타임스탬프 저장
    const answers = state.questionSetAnswers.get(questionSetId) ?? []
    if (answers.length > 0) {
      try {
        await apiClient.post(
          `/api/v1/interviews/${interview.id}/question-sets/${questionSetId}/answers`,
          { answers },
        )
      } catch {
        // 답변 저장 실패 시에도 업로드는 시도
      }
    }

    // 4. Presigned URL 발급 + S3 업로드
    setUploadStatus(questionSetId, 'uploading')
    try {
      const urlRes = await apiClient.post<ApiResponse<UploadUrlResponse>>(
        `/api/v1/interviews/${interview.id}/question-sets/${questionSetId}/upload-url`,
        { contentType: blob.type || 'video/webm' },
      )
      await s3Upload.upload(blob, urlRes.data.uploadUrl)
      setUploadStatus(questionSetId, 'completed')
      // 업로드 성공 시 IndexedDB 임시 데이터 정리
      deleteVideoBlob(interview.id, questionSetId).catch(() => {})
    } catch {
      setUploadStatus(questionSetId, 'failed')
    }
  }, [interview, mediaStream.stream, recorder, s3Upload, setUploadStatus])

  // 질문세트 내 마지막 질문인지 확인
  const isLastQuestionInSet = useCallback(() => {
    if (!hasQuestionSets) return false
    const state = useInterviewStore.getState()
    const currentSet = state.questionSets[state.currentQuestionSetIndex]
    if (!currentSet) return false

    // 현재 질문이 현재 세트의 마지막인지 계산
    // questions는 모든 세트의 질문이 flat하게 들어있으므로,
    // 현재 세트까지의 질문 수 합산으로 경계를 판단
    const questionsBeforeCurrentSet = state.currentQuestionSetIndex
    const mainQuestionCount = currentSet.questions.filter(q => q.questionType === 'MAIN').length
    const lastIndexInSet = questionsBeforeCurrentSet + mainQuestionCount - 1
    return state.currentQuestionIndex >= lastIndexInSet
  }, [hasQuestionSets])

  // 다음 질문 또는 다음 세트 또는 종료로 전환
  const transitionToNext = useCallback((isLast: boolean) => {
    const state = useInterviewStore.getState()
    const isSetEnd = hasQuestionSets && isLastQuestionInSet()
    const isLastSet = state.currentQuestionSetIndex >= state.questionSets.length - 1

    if (isLast || (isSetEnd && isLastSet)) {
      // 면접 종료 → finishing phase로 전환 (사용자가 [면접 종료하기] 클릭 대기)
      pendingTtsActionRef.current = async () => {
        if (hasQuestionSets) {
          const currentSet = state.questionSets[state.currentQuestionSetIndex]
          await handleQuestionSetComplete(currentSet.id).catch((err: unknown) => {
            console.error('[S3 업로드] 질문세트 업로드 실패:', err)
          })
        }
        setPhase('finishing')
      }
      tts.speak(pickRandom(CLOSING_PHRASES))
    } else if (isSetEnd && !isLastSet) {
      // 질문세트 전환
      const currentSet = state.questionSets[state.currentQuestionSetIndex]
      pendingTtsActionRef.current = async () => {
        handleQuestionSetComplete(currentSet.id).catch((err: unknown) => {
          console.error('[S3 업로드] 질문세트 업로드 실패:', err)
        })
        nextQuestionSet()
        nextQuestion()
      }
      tts.speak(pickRandom(SET_TRANSITION_PHRASES))
    } else {
      // 같은 세트 내 다음 질문
      pendingTtsActionRef.current = () => nextQuestion()
      tts.speak(pickRandom(TRANSITION_PHRASES))
    }
  }, [pendingTtsActionRef, setPhase, nextQuestion, nextQuestionSet, tts, hasQuestionSets, isLastQuestionInSet, handleQuestionSetComplete])

  // 실제 답변 시작 로직
  const doStartAnswer = useCallback(() => {
    const { phase: currentPhase, currentQuestionIndex, questionSetRecordingStartTime } = useInterviewStore.getState()
    if (currentPhase !== 'ready' && currentPhase !== 'paused' && currentPhase !== 'greeting') return
    if (!mediaStream.stream) return
    // 단일 timestamp로 녹화 시작 시간과 답변 시작 시간 통일
    const now = Date.now()
    if (hasQuestionSets && questionSetRecordingStartTime === null) {
      setQuestionSetRecordingStartTime(now)
    }
    startRecording(now)
    if (!recorder.isRecording) {
      recorder.start(mediaStream.stream)
      startEventRecording()
    }
    if (mediaStream.stream) audioCapture.start(mediaStream.stream)
    recordEvent('answer_start', currentQuestionIndex)
  }, [mediaStream.stream, startRecording, recorder, audioCapture, recordEvent, startEventRecording, hasQuestionSets, setQuestionSetRecordingStartTime])

  // "답변 완료" 버튼 — 후속질문 멀티라운드 흐름
  const handleStopAnswer = useCallback(async () => {
    const state = useInterviewStore.getState()
    if (state.phase !== 'recording' && state.phase !== 'greeting') return
    pendingTtsActionRef.current = null
    tts.stop()
    const stopTime = Date.now()
    stopRecording()
    recordEvent('manual_stop', state.currentQuestionIndex)

    // greeting 중 자기소개 완료 → ready로 전환 + 첫 질문 TTS
    if (greetingPhaseRef.current) {
      audioCapture.stop()
      // 자기소개 녹화 blob 폐기 — MediaRecorder 정지
      if (recorder.isRecording) {
        await recorder.stop()
      }
      // 다음 doStartAnswer() 호출 시 새로 설정되도록 리셋
      setQuestionSetRecordingStartTime(null)
      completeGreeting()
      return
    }

    // 후속질문용 오디오 캡처 (await)
    const audioBlob = await audioCapture.stop()

    // 현재 답변 텍스트 수집
    const answerText = getCurrentAnswerText()

    // 질문세트가 있으면 답변 타임스탬프 기록
    if (hasQuestionSets) {
      const currentSetForTs = state.questionSets[state.currentQuestionSetIndex]
      if (currentSetForTs) {
        const currentAnswer = state.answers[state.currentQuestionIndex]

        // 후속질문 답변이면 후속질문의 questionId 사용, 아니면 MAIN 질문 ID
        let targetQuestionId: number | undefined
        if (state.currentFollowUp?.questionId) {
          targetQuestionId = state.currentFollowUp.questionId
        } else {
          const questionsBeforeSet = state.currentQuestionSetIndex
          const questionInSetIndex = state.currentQuestionIndex - questionsBeforeSet
          const questionDetail = currentSetForTs.questions[questionInSetIndex]
          targetQuestionId = questionDetail?.id
        }

        if (targetQuestionId && state.questionSetRecordingStartTime !== null) {
          const recordingStart = state.questionSetRecordingStartTime
          addAnswerTimestamp(currentSetForTs.id, {
            questionId: targetQuestionId,
            startMs: Math.max(0, (currentAnswer?.startTime ?? 0) - recordingStart),
            endMs: Math.max(0, stopTime - recordingStart),
          })
        }
      }
    }

    // 후속질문에 대한 답변이었는지 기록 (히스토리 저장은 API 응답 후)
    const wasFollowUp = !!state.currentFollowUp

    // 후속질문 라운드 확인
    const updatedState = useInterviewStore.getState()
    const canDoMoreFollowUps = updatedState.followUpRound < MAX_FOLLOWUP_ROUNDS
    const isLastQuestion = state.currentQuestionIndex >= state.questions.length - 1

    // 현재 질문세트 ID 가져오기 — updatedState 사용으로 클로저 캡처 문제 방지
    const currentSet = hasQuestionSets
      ? updatedState.questionSets[updatedState.currentQuestionSetIndex]
      : undefined

    const hasAnswer = answerText.trim() || (audioBlob && audioBlob.size > 0)

    if (canDoMoreFollowUps && hasAnswer && interview) {
      // 후속질문 요청 → 응답 대기 → TTS로 읽기
      setFollowUpLoading(true)
      try {
        const history = updatedState.followUpHistory.get(state.currentQuestionIndex) ?? []
        const previousExchanges = history.map((e) => ({
          question: e.question,
          answer: e.answer,
        }))

        const res = await followUpMutation.mutateAsync({
          id: interview.id,
          data: {
            questionSetId: currentSet?.id ?? 0,
            questionContent: state.questions[state.currentQuestionIndex].content,
            answerText,
            previousExchanges,
          },
          audioBlob: audioBlob && audioBlob.size > 0 ? audioBlob : undefined,
        })
        setFollowUpLoading(false)

        // API 응답에서 Whisper STT 결과를 받아 히스토리에 저장
        if (wasFollowUp) {
          completeFollowUpRound(res.data.answerText || answerText)
        }

        setCurrentFollowUp(res.data)

        // 후속질문의 questionId를 QuestionSetData에 동적 추가 (답변 타임스탬프용)
        if (currentSet && res.data.questionId) {
          addQuestionToSet(updatedState.currentQuestionSetIndex, {
            id: res.data.questionId,
            questionType: 'FOLLOWUP',
            questionText: res.data.question,
            modelAnswer: res.data.modelAnswer ?? null,
            referenceType: 'CS',
            orderIndex: currentSet.questions.length,
          })
        }

        tts.speak(res.data.question)
      } catch (err) {
        console.error('[후속질문] 생성 실패:', err)
        // 실패 시에도 히스토리 기록 (빈 텍스트라도)
        if (wasFollowUp) {
          completeFollowUpRound(answerText)
        }
        setFollowUpLoading(false)
        resetFollowUpState()
        transitionToNext(isLastQuestion)
      }
    } else {
      // 후속질문 라운드 종료 → 마지막 라운드 히스토리 저장
      if (wasFollowUp) {
        completeFollowUpRound(answerText)
      }
      resetFollowUpState()
      transitionToNext(isLastQuestion)
    }
  }, [
    stopRecording, audioCapture, tts, recordEvent,
    greetingPhaseRef, completeGreeting, pendingTtsActionRef,
    getCurrentAnswerText, completeFollowUpRound, addAnswerTimestamp,
    setFollowUpLoading, setCurrentFollowUp, resetFollowUpState,
    followUpMutation, interview, transitionToNext, hasQuestionSets,
    addQuestionToSet, recorder, setQuestionSetRecordingStartTime,
  ])

  return {
    handleStartAnswer: doStartAnswer,
    handleStopAnswer,
    s3Upload,
  }
}
