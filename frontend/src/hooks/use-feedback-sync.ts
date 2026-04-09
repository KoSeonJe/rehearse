import { useCallback, useEffect, useMemo, useState } from 'react'
import type { VideoPlayerHandle } from '@/components/feedback/video-player'
import type { QuestionWithAnswer, TimestampFeedback } from '@/types/interview'

export const useFeedbackSync = (
  videoRef: React.RefObject<VideoPlayerHandle | null>,
  feedbacks: TimestampFeedback[],
  questions: QuestionWithAnswer[],
) => {
  const [activeFeedbackId, setActiveFeedbackId] = useState<number | null>(null)
  const [stickyFeedbackId, setStickyFeedbackId] = useState<number | null>(null)
  const [currentTimeMs, setCurrentTimeMs] = useState(0)
  const [videoDurationMs, setVideoDurationMs] = useState(0)

  // Poll video time and compute active feedback
  useEffect(() => {
    const interval = setInterval(() => {
      if (!videoRef.current) return
      const ms = videoRef.current.getCurrentTimeMs()
      setCurrentTimeMs(ms)

      const dMs = videoRef.current.getDurationMs()
      if (dMs > 0) setVideoDurationMs(dMs)

      const active = feedbacks.find((fb) => ms >= fb.startMs && ms < fb.endMs)
      const nextId = active?.id ?? null
      setActiveFeedbackId(nextId)
      // 재생 구간을 벗어나도 마지막으로 본 피드백을 유지(패널 깜빡임 방지)
      if (nextId !== null) setStickyFeedbackId(nextId)
    }, 200)

    return () => clearInterval(interval)
  }, [videoRef, feedbacks])

  // 영상 재생 위치와 무관하게 패널/질문목록에 표시할 "선택된" 피드백 id.
  // 재생 위치 매치가 있으면 그 값, 없으면 첫 메인 질문의 피드백을 기본값으로 사용한다.
  const defaultFeedbackId = useMemo<number | null>(() => {
    if (feedbacks.length === 0) return null

    const mainPlayables = questions.filter(
      (q) => q.questionType !== 'FOLLOWUP' && q.startMs !== null && q.endMs !== null,
    )
    const firstMainQuestion = [...mainPlayables].sort(
      (a, b) => (a.startMs ?? 0) - (b.startMs ?? 0),
    )[0]

    if (firstMainQuestion && firstMainQuestion.startMs !== null && firstMainQuestion.endMs !== null) {
      const startMs = firstMainQuestion.startMs
      const endMs = firstMainQuestion.endMs
      const match = feedbacks.find((fb) => fb.startMs >= startMs && fb.startMs < endMs)
      if (match) return match.id
    }

    return feedbacks[0]?.id ?? null
  }, [feedbacks, questions])

  // 우선순위: 현재 재생 중 구간 > 마지막으로 본 구간(sticky) > 첫 메인질문 기본값
  const selectedFeedbackId = activeFeedbackId ?? stickyFeedbackId ?? defaultFeedbackId

  const seekTo = useCallback(
    (ms: number) => {
      if (videoRef.current) {
        videoRef.current.seekTo(ms)
      }
    },
    [videoRef],
  )

  return { activeFeedbackId, selectedFeedbackId, currentTimeMs, videoDurationMs, seekTo }
}
