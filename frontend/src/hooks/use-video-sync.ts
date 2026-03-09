import { useCallback, useRef, useEffect } from 'react'
import { useReviewStore } from '../stores/review-store'

export const useVideoSync = () => {
  const videoRef = useRef<HTMLVideoElement>(null)
  const { setCurrentTime, setIsPlaying, selectFeedback } = useReviewStore()
  const { feedbacks } = useReviewStore()

  useEffect(() => {
    const video = videoRef.current
    if (!video) return

    const handleTimeUpdate = () => {
      setCurrentTime(video.currentTime)
    }

    const handlePlay = () => setIsPlaying(true)
    const handlePause = () => setIsPlaying(false)

    video.addEventListener('timeupdate', handleTimeUpdate)
    video.addEventListener('play', handlePlay)
    video.addEventListener('pause', handlePause)

    return () => {
      video.removeEventListener('timeupdate', handleTimeUpdate)
      video.removeEventListener('play', handlePlay)
      video.removeEventListener('pause', handlePause)
    }
  }, [setCurrentTime, setIsPlaying])

  const seekTo = useCallback((time: number) => {
    const video = videoRef.current
    if (!video) return
    video.currentTime = time
    setCurrentTime(time)
  }, [setCurrentTime])

  const seekToFeedback = useCallback((feedbackId: number) => {
    const feedback = feedbacks.find((f) => f.id === feedbackId)
    if (!feedback) return
    seekTo(feedback.timestampSeconds)
    selectFeedback(feedbackId)
  }, [feedbacks, seekTo, selectFeedback])

  return {
    videoRef,
    seekTo,
    seekToFeedback,
  }
}
