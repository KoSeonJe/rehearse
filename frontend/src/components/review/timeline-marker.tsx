import type { TimestampFeedback } from '../../types/interview'

interface TimelineMarkerProps {
  feedback: TimestampFeedback
  totalDuration: number
  isSelected: boolean
  onClick: () => void
}

const CATEGORY_COLORS: Record<string, string> = {
  VERBAL: 'bg-info',
  NON_VERBAL: 'bg-warning',
  CONTENT: 'bg-success',
}

const formatTime = (seconds: number): string => {
  const m = Math.floor(seconds / 60)
  const s = Math.floor(seconds % 60)
  return `${m}:${s.toString().padStart(2, '0')}`
}

export const TimelineMarker = ({ feedback, totalDuration, isSelected, onClick }: TimelineMarkerProps) => {
  const position = totalDuration > 0 ? (feedback.timestampSeconds / totalDuration) * 100 : 0

  return (
    <button
      onClick={onClick}
      aria-label={`${formatTime(feedback.timestampSeconds)} 피드백`}
      className="absolute top-1/2 -translate-y-1/2 flex h-6 w-6 items-center justify-center focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2"
      style={{ left: `${position}%`, marginLeft: '-12px' }}
      title={feedback.content}
    >
      <span
        className={`block h-3 w-3 rounded-full transition-transform ${
          CATEGORY_COLORS[feedback.category] ?? 'bg-text-secondary'
        } ${isSelected ? 'scale-150 ring-2 ring-white' : 'group-hover:scale-125 hover:scale-125'}`}
      />
    </button>
  )
}

