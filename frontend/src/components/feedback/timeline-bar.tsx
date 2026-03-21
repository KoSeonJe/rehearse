import type { TimestampFeedback } from '@/types/interview'

const getTypeColor = (feedback: TimestampFeedback): string => {
  if (feedback.questionType === 'FOLLOWUP') return 'bg-blue-400'
  return 'bg-accent'
}

interface TimelineBarProps {
  feedbacks: TimestampFeedback[]
  durationMs: number
  currentTimeMs: number
  activeFeedbackId: number | null
  onSeek: (ms: number) => void
}

export const TimelineBar = ({
  feedbacks,
  durationMs,
  currentTimeMs,
  activeFeedbackId,
  onSeek,
}: TimelineBarProps) => {
  if (durationMs <= 0) return null

  const indicatorPercent = Math.min((currentTimeMs / durationMs) * 100, 100)

  return (
    <div className="space-y-2">
      {/* Timeline */}
      <div className="relative h-8 w-full rounded-lg bg-surface overflow-hidden">
        {feedbacks.map((fb) => {
          const left = (fb.startMs / durationMs) * 100
          const width = ((fb.endMs - fb.startMs) / durationMs) * 100
          const isActive = fb.id === activeFeedbackId

          return (
            <button
              key={fb.id}
              className={`absolute top-1 bottom-1 rounded-md transition-all cursor-pointer ${
                isActive ? 'ring-2 ring-accent ring-offset-1 z-10' : 'hover:brightness-110'
              } ${getTypeColor(fb)}`}
              style={{ left: `${left}%`, width: `${Math.max(width, 0.5)}%` }}
              onClick={() => onSeek(fb.startMs)}
              title={`${fb.questionType === 'FOLLOWUP' ? '후속질문' : '원본'} (${Math.round(fb.startMs / 1000)}s)`}
            />
          )
        })}

        {/* Playhead indicator */}
        <div
          className="absolute top-0 bottom-0 w-0.5 bg-text-primary z-20 pointer-events-none"
          style={{ left: `${indicatorPercent}%` }}
        />
      </div>

      {/* Legend */}
      <div className="flex flex-wrap gap-3">
        <div className="flex items-center gap-1.5">
          <div className="h-2.5 w-2.5 rounded-sm bg-accent" />
          <span className="text-[10px] font-medium text-text-tertiary">원본</span>
        </div>
        {feedbacks.some((f) => f.questionType === 'FOLLOWUP') && (
          <div className="flex items-center gap-1.5">
            <div className="h-2.5 w-2.5 rounded-sm bg-blue-400" />
            <span className="text-[10px] font-medium text-text-tertiary">후속질문</span>
          </div>
        )}
      </div>
    </div>
  )
}
