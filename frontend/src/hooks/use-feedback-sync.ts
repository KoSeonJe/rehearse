import { useCallback, useEffect, useState } from 'react'
import type { VideoPlayerHandle } from '@/components/feedback/video-player'
import type { TimestampFeedback } from '@/types/interview'

export const useFeedbackSync = (
  videoRef: React.RefObject<VideoPlayerHandle | null>,
  feedbacks: TimestampFeedback[],
) => {
  const [activeFeedbackId, setActiveFeedbackId] = useState<number | null>(null)
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
      setActiveFeedbackId(active?.id ?? null)
    }, 200)

    return () => clearInterval(interval)
  }, [videoRef, feedbacks])

  const seekTo = useCallback(
    (ms: number) => {
      if (videoRef.current) {
        videoRef.current.seekTo(ms)
      }
    },
    [videoRef],
  )

  return { activeFeedbackId, currentTimeMs, videoDurationMs, seekTo }
}
