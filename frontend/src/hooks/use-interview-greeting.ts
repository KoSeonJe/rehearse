import { useCallback, useEffect, type MutableRefObject } from 'react'
import { useInterviewStore } from '@/stores/interview-store'
import type { InterviewEventType } from '@/types/interview'

interface UseInterviewGreetingParams {
  tts: {
    speak: (text: string) => void
    speakWhenReady: (text: string) => void
  }
  recordEvent: (type: InterviewEventType, questionIndex: number) => void
  mediaStreamIsActive: boolean
  greetingPhaseRef: MutableRefObject<boolean>
}

export const useInterviewGreeting = ({
  tts,
  recordEvent,
  mediaStreamIsActive,
  greetingPhaseRef,
}: UseInterviewGreetingParams) => {
  const phase = useInterviewStore((s) => s.phase)

  // greeting phase 진입 시 인사 TTS (mediaStream 준비 후 시작)
  useEffect(() => {
    if (phase === 'greeting' && mediaStreamIsActive) {
      greetingPhaseRef.current = true
      tts.speakWhenReady(
        '안녕하세요, 오늘 면접을 진행하게 된 AI 면접관입니다. 면접을 시작하기 전에 간단하게 자기소개 부탁드리겠습니다.',
      )
      recordEvent('greeting_tts', 0)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [phase, mediaStreamIsActive])

  // 자기소개 완료 → ready 전환 + 첫 질문 TTS
  const completeGreeting = useCallback(() => {
    greetingPhaseRef.current = false
    useInterviewStore.setState({ phase: 'ready', greetingCompleted: true })
    const state = useInterviewStore.getState()
    const firstQuestion = state.questions[0]
    if (firstQuestion) {
      tts.speak(
        `네, 감사합니다. 그럼 본격적으로 면접을 시작하겠습니다. 첫 번째 질문입니다. ${firstQuestion.content}`,
      )
      recordEvent('question_read_tts', 0)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tts.speak, recordEvent])

  return { completeGreeting }
}
