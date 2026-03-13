import { useCallback, type MutableRefObject } from 'react'
import { useInterviewStore } from '@/stores/interview-store'
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
    questions,
    currentQuestionIndex,
    startRecording,
    stopRecording,
    addFollowUpQuestion,
    setFollowUpLoading,
    nextQuestion,
    completeInterview,
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

  // 실제 답변 시작 로직
  const doStartAnswer = useCallback(() => {
    const currentPhase = useInterviewStore.getState().phase
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
  }, [mediaStream.stream, startRecording, recorder, stt, currentQuestionIndex, recordEvent, startEventRecording])

  // "답변 완료" 버튼 — 전환 로직 통합
  const handleStopAnswer = useCallback(() => {
    const state = useInterviewStore.getState()
    if (state.phase !== 'recording' && state.phase !== 'greeting') return
    pendingTtsActionRef.current = null
    tts.stop()
    stopRecording()
    stt.stop()
    recorder.pause()
    recordEvent('manual_stop', currentQuestionIndex)

    // greeting 중 자기소개 완료 → ready로 전환 + 첫 질문 TTS
    if (greetingPhaseRef.current) {
      completeGreeting()
      return
    }

    // 일반 질문: 답변 처리 + 전환 TTS → 다음 질문/종료
    processAnswer()

    const isLastQuestion = state.currentQuestionIndex >= state.questions.length - 1

    if (isLastQuestion) {
      pendingTtsActionRef.current = () => {
        completeInterview()
      }
      tts.speak(pickRandom(CLOSING_PHRASES))
    } else {
      pendingTtsActionRef.current = () => nextQuestion()
      tts.speak(pickRandom(TRANSITION_PHRASES))
    }
  }, [stopRecording, stt, recorder, processAnswer, recordEvent, currentQuestionIndex, tts, nextQuestion, completeInterview, greetingPhaseRef, completeGreeting, pendingTtsActionRef])

  return {
    handleStartAnswer: doStartAnswer,
    handleStopAnswer,
  }
}
