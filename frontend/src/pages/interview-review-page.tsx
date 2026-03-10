import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useReviewStore } from '../stores/review-store'
import { useFeedbacks } from '../hooks/use-feedback'
import { useVideoSync } from '../hooks/use-video-sync'
import { Logo } from '@/components/ui/logo'
import { Character } from '@/components/ui/character'
import { VideoPlayer } from '../components/review/video-player'
import { FeedbackTimeline } from '../components/review/feedback-timeline'
import { FeedbackPanel } from '../components/review/feedback-panel'

export const InterviewReviewPage = () => {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { data: response, isLoading } = useFeedbacks(id ?? '')
  const { setFeedbacks, reset } = useReviewStore()
  const { videoRef, seekToFeedback } = useVideoSync()
  const [totalDuration, setTotalDuration] = useState(0)

  useEffect(() => {
    if (response?.data?.feedbacks) {
      setFeedbacks(response.data.feedbacks)
    }
  }, [response, setFeedbacks])

  useEffect(() => {
    const video = videoRef.current
    if (!video) return

    const handleLoadedMetadata = () => {
      setTotalDuration(video.duration)
    }

    video.addEventListener('loadedmetadata', handleLoadedMetadata)
    return () => video.removeEventListener('loadedmetadata', handleLoadedMetadata)
  }, [videoRef])

  useEffect(() => {
    return () => reset()
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  if (isLoading) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center bg-white">
        <Character mood="thinking" size={160} className="mx-auto" />
        <p className="mt-4 text-text-secondary">피드백을 불러오는 중...</p>
      </div>
    )
  }

  return (
    <div className="flex min-h-screen flex-col bg-white">
      {/* Header */}
      <header className="sticky top-0 z-50 bg-white/80 backdrop-blur-md px-4 py-4 sm:px-6">
        <div className="mx-auto flex max-w-7xl items-center justify-between">
          <div className="flex items-center gap-3">
            <Logo size={60} />
            <span className="font-black text-text-primary">피드백 리뷰</span>
          </div>
          <div className="flex items-center gap-2 sm:gap-3">
            <button
              onClick={() => navigate(`/interview/${id}/report`)}
              className="hidden rounded-[24px] bg-surface px-6 py-3 text-sm font-bold text-text-secondary transition-all hover:bg-slate-200 active:scale-95 sm:inline-flex"
            >
              종합 리포트
            </button>
            <button
              onClick={() => navigate('/')}
              className="rounded-[24px] bg-accent px-6 py-3 text-sm font-black text-white shadow-lg shadow-accent/20 transition-all active:scale-95"
            >
              홈으로
            </button>
          </div>
        </div>
      </header>

      {/* Main content - 좌60% 비디오 + 우40% 피드백 */}
      <main className="mx-auto flex w-full max-w-7xl flex-1 flex-col gap-6 px-4 py-6 sm:px-6 lg:flex-row">
        {/* 좌측: 비디오 + 타임라인 */}
        <div className="w-full space-y-4 lg:w-3/5">
          <VideoPlayer videoRef={videoRef} />
          <FeedbackTimeline
            totalDuration={totalDuration}
            onSeekToFeedback={seekToFeedback}
          />
        </div>

        {/* 우측: 피드백 패널 */}
        <div className="w-full lg:w-2/5">
          <div className="lg:sticky lg:top-24 lg:max-h-[calc(100vh-8rem)] overflow-y-auto rounded-[32px] bg-surface p-6">
            <h2 className="mb-4 text-sm font-black uppercase tracking-widest text-text-tertiary">
              피드백 ({response?.data?.totalCount ?? 0}개)
            </h2>
            <FeedbackPanel onSeekToFeedback={seekToFeedback} />
          </div>
        </div>
      </main>
    </div>
  )
}
