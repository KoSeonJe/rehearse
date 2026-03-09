import type { TimestampFeedback } from '../../types/interview'

interface TimelineMarkerProps {
  feedback: TimestampFeedback
  totalDuration: number
  isSelected: boolean
  onClick: () => void
}

const CATEGORY_COLORS: Record<string, string> = {
  VERBAL: 'bg-blue-500',
  NON_VERBAL: 'bg-orange-500',
  CONTENT: 'bg-green-500',
}

const TimelineMarker = ({ feedback, totalDuration, isSelected, onClick }: TimelineMarkerProps) => {
  const position = totalDuration > 0 ? (feedback.timestampSeconds / totalDuration) * 100 : 0

  return (
    <button
      onClick={onClick}
      className={`absolute top-1/2 -translate-y-1/2 h-3 w-3 rounded-full transition-transform ${
        CATEGORY_COLORS[feedback.category] ?? 'bg-slate-500'
      } ${isSelected ? 'scale-150 ring-2 ring-white' : 'hover:scale-125'}`}
      style={{ left: `${position}%` }}
      title={feedback.content}
    />
  )
}

export default TimelineMarker
