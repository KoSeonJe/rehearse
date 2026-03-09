import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useReviewStore } from '../stores/review-store'
import { useFeedbacks } from '../hooks/use-feedback'
import { useVideoSync } from '../hooks/use-video-sync'
import VideoPlayer from '../components/review/video-player'
import FeedbackTimeline from '../components/review/feedback-timeline'
import FeedbackPanel from '../components/review/feedback-panel'

const InterviewReviewPage = () => {
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
      <div className="flex min-h-screen items-center justify-center bg-gray-50">
        <p className="text-slate-500">피드백을 불러오는 중...</p>
      </div>
    )
  }

  return (
    <div className="flex min-h-screen flex-col bg-gray-50">
      {/* Header */}
      <header className="border-b border-slate-200 bg-white px-6 py-4">
        <div className="mx-auto flex max-w-7xl items-center justify-between">
          <div className="flex items-center gap-3">
            <h1 className="text-lg font-bold text-slate-900">DevLens</h1>
            <span className="text-sm text-slate-500">피드백 리뷰</span>
          </div>
          <div className="flex items-center gap-3">
            <button
              onClick={() => navigate(`/interview/${id}/report`)}
              className="rounded-lg border border-slate-200 px-4 py-2 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-50"
            >
              종합 리포트
            </button>
            <button
              onClick={() => navigate('/')}
              className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-slate-800"
            >
              홈으로
            </button>
          </div>
        </div>
      </header>

      {/* Main content - 좌60% 비디오 + 우40% 피드백 */}
      <main className="mx-auto flex w-full max-w-7xl flex-1 gap-6 px-6 py-6">
        {/* 좌측: 비디오 + 타임라인 */}
        <div className="w-3/5 space-y-4">
          <VideoPlayer videoRef={videoRef} />
          <FeedbackTimeline
            totalDuration={totalDuration}
            onSeekToFeedback={seekToFeedback}
          />
        </div>

        {/* 우측: 피드백 패널 */}
        <div className="w-2/5">
          <div className="sticky top-6 max-h-[calc(100vh-8rem)] overflow-y-auto rounded-2xl border border-slate-200 bg-white p-4">
            <h2 className="mb-4 text-sm font-semibold text-slate-900">
              피드백 ({response?.data?.totalCount ?? 0}개)
            </h2>
            <FeedbackPanel onSeekToFeedback={seekToFeedback} />
          </div>
        </div>
      </main>
    </div>
  )
}

export default InterviewReviewPage
