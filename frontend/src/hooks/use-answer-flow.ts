import { useCallback, type MutableRefObject } from 'react'
import { useInterviewStore, MAX_FOLLOWUP_ROUNDS } from '@/stores/interview-store'
import { useFollowUpQuestion } from '@/hooks/use-interviews'
import type { Question, InterviewEventType } from '@/types/interview'

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

interface UseAnswerFlowParams {
  interview: { id: number; status: string; questions: Question[] } | undefined
  mediaStream: { stream: MediaStream | null }
  recorder: {
    isRecording: boolean
    start: (stream: MediaStream) => void
    pause: () => void
    resume: () => void
  }
  stt: {
    start: (questionIndex: number) => void
    stop: () => void
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
  stt,
  tts,
  recordEvent,
  startEventRecording,
  greetingPhaseRef,
  completeGreeting,
  pendingTtsActionRef,
}: UseAnswerFlowParams) => {
  const followUpMutation = useFollowUpQuestion()
  const {
    startRecording,
    stopRecording,
    setCurrentFollowUp,
    completeFollowUpRound,
    resetFollowUpState,
    setFollowUpLoading,
    nextQuestion,
    completeInterview,
  } = useInterviewStore()

  // 현재 답변 텍스트 수집
  const getCurrentAnswerText = useCallback(() => {
    const state = useInterviewStore.getState()
    const currentAnswer = state.answers[state.currentQuestionIndex]
    return currentAnswer?.transcripts
      .filter((t) => t.isFinal)
      .map((t) => t.text)
      .join(' ') ?? ''
  }, [])

  // 다음 질문 또는 종료로 전환
  const transitionToNext = useCallback((isLast: boolean) => {
    if (isLast) {
      pendingTtsActionRef.current = () => completeInterview()
      tts.speak(pickRandom(CLOSING_PHRASES))
    } else {
      pendingTtsActionRef.current = () => nextQuestion()
      tts.speak(pickRandom(TRANSITION_PHRASES))
    }
  }, [pendingTtsActionRef, completeInterview, nextQuestion, tts])

  // 실제 답변 시작 로직
  const doStartAnswer = useCallback(() => {
    const { phase: currentPhase, currentQuestionIndex } = useInterviewStore.getState()
    if (currentPhase !== 'ready' && currentPhase !== 'paused' && currentPhase !== 'greeting') return
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
  }, [mediaStream.stream, startRecording, recorder, stt, recordEvent, startEventRecording])

  // "답변 완료" 버튼 — 후속질문 멀티라운드 흐름
  const handleStopAnswer = useCallback(async () => {
    const state = useInterviewStore.getState()
    if (state.phase !== 'recording' && state.phase !== 'greeting') return
    pendingTtsActionRef.current = null
    tts.stop()
    stopRecording()
    stt.stop()
    recorder.pause()
    recordEvent('manual_stop', state.currentQuestionIndex)

    // greeting 중 자기소개 완료 → ready로 전환 + 첫 질문 TTS
    if (greetingPhaseRef.current) {
      completeGreeting()
      return
    }

    // 현재 답변 텍스트 수집
    const answerText = getCurrentAnswerText()

    // 후속질문에 대한 답변이었으면 히스토리에 저장
    if (state.currentFollowUp) {
      completeFollowUpRound(answerText)
    }

    // 후속질문 라운드 확인 (completeFollowUpRound 후 갱신된 상태)
    const updatedState = useInterviewStore.getState()
    const canDoMoreFollowUps = updatedState.followUpRound < MAX_FOLLOWUP_ROUNDS
    const isLastQuestion = state.currentQuestionIndex >= state.questions.length - 1

    if (canDoMoreFollowUps && answerText.trim() && interview) {
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
            questionContent: state.questions[state.currentQuestionIndex].content,
            answerText,
            previousExchanges,
          },
        })
        setFollowUpLoading(false)
        setCurrentFollowUp(res.data)
        tts.speak(res.data.question)
      } catch {
        setFollowUpLoading(false)
        resetFollowUpState()
        transitionToNext(isLastQuestion)
      }
    } else {
      // 후속질문 라운드 종료 → 다음 메인 질문 전환
      resetFollowUpState()
      transitionToNext(isLastQuestion)
    }
  }, [
    stopRecording, stt, recorder, tts, recordEvent,
    greetingPhaseRef, completeGreeting, pendingTtsActionRef,
    getCurrentAnswerText, completeFollowUpRound,
    setFollowUpLoading, setCurrentFollowUp, resetFollowUpState,
    followUpMutation, interview, transitionToNext,
  ])

  return {
    handleStartAnswer: doStartAnswer,
    handleStopAnswer,
  }
}
