import { useEffect, useMemo, useRef } from 'react'
import { useReviewStore } from '../../stores/review-store'
import type { TimestampFeedback } from '../../types/interview'

interface FeedbackPanelProps {
  onSeekToFeedback: (feedbackId: number) => void
}

const SEVERITY_STYLES: Record<string, { bg: string; text: string; label: string }> = {
  INFO: { bg: 'bg-info-light border-info/30', text: 'text-info', label: '정보' },
  WARNING: { bg: 'bg-warning-light border-warning/30', text: 'text-warning', label: '주의' },
  SUGGESTION: { bg: 'bg-success-light border-success/30', text: 'text-success', label: '제안' },
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
  const cardRef = useRef<HTMLButtonElement>(null)
  const severity = SEVERITY_STYLES[feedback.severity] ?? SEVERITY_STYLES.INFO

  useEffect(() => {
    if (isActive && cardRef.current) {
      cardRef.current.scrollIntoView({
        behavior: 'smooth',
        block: 'nearest',
      })
    }
  }, [isActive])

  return (
    <button
      ref={cardRef}
      onClick={onClick}
      className={`w-full rounded-card border-l-4 border p-4 text-left transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2 ${severity.bg} ${
        isSelected ? 'ring-2 ring-accent' : ''
      } ${isActive ? 'border-l-accent opacity-100 shadow-sm' : 'border-l-transparent opacity-50'}`}
    >
      <div className="mb-2 flex items-center gap-2">
        <span className="text-xs font-medium text-text-secondary">
          {formatTime(feedback.timestampSeconds)}
        </span>
        <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${severity.text} ${severity.bg}`}>
          {severity.label}
        </span>
        <span className="rounded-full bg-background px-2 py-0.5 text-xs font-medium text-text-secondary">
          {CATEGORY_LABELS[feedback.category] ?? feedback.category}
        </span>
      </div>
      <p className="text-sm leading-relaxed text-text-primary">{feedback.content}</p>
      {feedback.suggestion && (
        <p className="mt-2 text-xs leading-relaxed text-text-secondary">
          {feedback.suggestion}
        </p>
      )}
    </button>
  )
}

export const FeedbackPanel = ({ onSeekToFeedback }: FeedbackPanelProps) => {
  const { feedbacks, currentTime, selectedFeedbackId } = useReviewStore()

  const sortedFeedbacks = useMemo(
    () => [...feedbacks].sort((a, b) => a.timestampSeconds - b.timestampSeconds),
    [feedbacks],
  )

  if (feedbacks.length === 0) {
    return (
      <div className="flex h-full items-center justify-center">
        <p className="text-sm text-text-secondary">피드백이 없습니다</p>
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
