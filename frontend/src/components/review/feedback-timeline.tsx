import { useReviewStore } from '../../stores/review-store'
import TimelineMarker from './timeline-marker'

interface FeedbackTimelineProps {
  totalDuration: number
  onSeekToFeedback: (feedbackId: number) => void
}

const formatTime = (seconds: number): string => {
  const m = Math.floor(seconds / 60)
  const s = Math.floor(seconds % 60)
  return `${m}:${s.toString().padStart(2, '0')}`
}

const FeedbackTimeline = ({ totalDuration, onSeekToFeedback }: FeedbackTimelineProps) => {
  const { feedbacks, currentTime, selectedFeedbackId } = useReviewStore()

  const progress = totalDuration > 0 ? (currentTime / totalDuration) * 100 : 0

  return (
    <div className="space-y-2">
      <div className="relative h-6 rounded-full bg-slate-100">
        {/* 진행 바 */}
        <div
          className="absolute left-0 top-0 h-full rounded-full bg-slate-200 transition-[width]"
          style={{ width: `${progress}%` }}
        />
        {/* 마커들 */}
        {feedbacks.map((feedback) => (
          <TimelineMarker
            key={feedback.id}
            feedback={feedback}
            totalDuration={totalDuration}
            isSelected={feedback.id === selectedFeedbackId}
            onClick={() => onSeekToFeedback(feedback.id)}
          />
        ))}
      </div>
      <div className="flex justify-between text-xs text-slate-400">
        <span>{formatTime(currentTime)}</span>
        <span>{formatTime(totalDuration)}</span>
      </div>
      {/* 범례 */}
      <div className="flex gap-4 text-xs text-slate-500">
        <span className="flex items-center gap-1">
          <span className="h-2 w-2 rounded-full bg-blue-500" /> 언어적
        </span>
        <span className="flex items-center gap-1">
          <span className="h-2 w-2 rounded-full bg-orange-500" /> 비언어적
        </span>
        <span className="flex items-center gap-1">
          <span className="h-2 w-2 rounded-full bg-green-500" /> 내용
        </span>
      </div>
    </div>
  )
}

export default FeedbackTimeline
