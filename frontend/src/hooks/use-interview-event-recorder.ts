import { useCallback, useRef } from 'react'
import type { InterviewEvent, InterviewEventType } from '@/types/interview'

export const useInterviewEventRecorder = () => {
  const eventsRef = useRef<InterviewEvent[]>([])
  const recordingStartTimeRef = useRef<number | null>(null)

  const startRecording = useCallback(() => {
    recordingStartTimeRef.current = Date.now()
    eventsRef.current = []
  }, [])

  const recordEvent = useCallback(
    (
      type: InterviewEventType,
      questionIndex: number,
      metadata?: Record<string, unknown>,
    ) => {
      const startTime = recordingStartTimeRef.current
      if (!startTime) return

      const elapsedMs = Date.now() - startTime

      eventsRef.current.push({
        type,
        elapsedMs,
        questionIndex,
        metadata,
      })
    },
    [],
  )

  const getEvents = useCallback((): InterviewEvent[] => {
    return [...eventsRef.current]
  }, [])

  return { recordEvent, getEvents, startRecording }
}
