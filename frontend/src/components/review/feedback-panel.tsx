import { useMemo } from 'react'
import { useReviewStore } from '../../stores/review-store'
import type { TimestampFeedback } from '../../types/interview'

interface FeedbackPanelProps {
  onSeekToFeedback: (feedbackId: number) => void
}

const SEVERITY_STYLES: Record<string, { bg: string; text: string; label: string }> = {
  INFO: { bg: 'bg-blue-50 border-blue-200', text: 'text-blue-700', label: '정보' },
  WARNING: { bg: 'bg-amber-50 border-amber-200', text: 'text-amber-700', label: '주의' },
  SUGGESTION: { bg: 'bg-green-50 border-green-200', text: 'text-green-700', label: '제안' },
}

const CATEGORY_LABELS: Record<string, string> = {
  VERBAL: '언어적',
  NON_VERBAL: '비언어적',
  CONTENT: '내용',
}

const formatTime = (seconds: number): string => {
  const m = Math.floor(seconds / 60)
  const s = Math.floor(seconds % 60)
  return `${m}:${s.toString().padStart(2, '0')}`
}

const FeedbackCard = ({
  feedback,
  isActive,
  isSelected,
  onClick,
}: {
  feedback: TimestampFeedback
  isActive: boolean
  isSelected: boolean
  onClick: () => void
}) => {
  const severity = SEVERITY_STYLES[feedback.severity] ?? SEVERITY_STYLES.INFO

  return (
    <button
      onClick={onClick}
      className={`w-full rounded-xl border p-4 text-left transition-all ${severity.bg} ${
        isSelected ? 'ring-2 ring-slate-900' : ''
      } ${isActive ? 'opacity-100' : 'opacity-60'}`}
    >
      <div className="mb-2 flex items-center gap-2">
        <span className="text-xs font-medium text-slate-500">
          {formatTime(feedback.timestampSeconds)}
        </span>
        <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${severity.text} ${severity.bg}`}>
          {severity.label}
        </span>
        <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-600">
          {CATEGORY_LABELS[feedback.category] ?? feedback.category}
        </span>
      </div>
      <p className="text-sm leading-relaxed text-slate-800">{feedback.content}</p>
      {feedback.suggestion && (
        <p className="mt-2 text-xs leading-relaxed text-slate-500">
          💡 {feedback.suggestion}
        </p>
      )}
    </button>
  )
}

const FeedbackPanel = ({ onSeekToFeedback }: FeedbackPanelProps) => {
  const { feedbacks, currentTime, selectedFeedbackId } = useReviewStore()

  const sortedFeedbacks = useMemo(
    () => [...feedbacks].sort((a, b) => a.timestampSeconds - b.timestampSeconds),
    [feedbacks],
  )

  if (feedbacks.length === 0) {
    return (
      <div className="flex h-full items-center justify-center">
        <p className="text-sm text-slate-500">피드백이 없습니다</p>
      </div>
    )
  }

  return (
    <div className="space-y-3 overflow-y-auto">
      {sortedFeedbacks.map((feedback) => (
        <FeedbackCard
          key={feedback.id}
          feedback={feedback}
          isActive={Math.abs(currentTime - feedback.timestampSeconds) < 10}
          isSelected={feedback.id === selectedFeedbackId}
          onClick={() => onSeekToFeedback(feedback.id)}
        />
      ))}
    </div>
  )
}

export default FeedbackPanel
