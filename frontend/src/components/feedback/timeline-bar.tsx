import type { TimestampFeedback } from '@/types/interview'

const ANSWER_TYPE_COLORS: Record<string, string> = {
  MAIN: 'bg-accent',
  FOLLOWUP_1: 'bg-blue-400',
  FOLLOWUP_2: 'bg-violet-400',
  FOLLOWUP_3: 'bg-teal-400',
}

const ANSWER_TYPE_LABELS: Record<string, string> = {
  MAIN: '원본',
  FOLLOWUP_1: '후속 1',
  FOLLOWUP_2: '후속 2',
  FOLLOWUP_3: '후속 3',
}

const getScoreColor = (feedback: TimestampFeedback): string => {
  const score = feedback.verbalScore
  if (score === null) return 'bg-border'
  if (score >= 80) return 'bg-success'
  if (score >= 50) return 'bg-yellow-400'
  return 'bg-error'
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
              } ${getScoreColor(fb)}`}
              style={{ left: `${left}%`, width: `${Math.max(width, 0.5)}%` }}
              onClick={() => onSeek(fb.startMs)}
              title={`${ANSWER_TYPE_LABELS[fb.answerType] ?? fb.answerType} (${Math.round(fb.startMs / 1000)}s)`}
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
        {Object.entries(ANSWER_TYPE_COLORS).map(([type, color]) => {
          const hasFeedback = feedbacks.some((f) => f.answerType === type)
          if (!hasFeedback) return null
          return (
            <div key={type} className="flex items-center gap-1.5">
              <div className={`h-2.5 w-2.5 rounded-sm ${color}`} />
              <span className="text-[10px] font-bold text-text-tertiary">
                {ANSWER_TYPE_LABELS[type] ?? type}
              </span>
            </div>
          )
        })}
        <div className="flex items-center gap-1.5">
          <div className="h-2.5 w-2.5 rounded-sm bg-success" />
          <span className="text-[10px] font-bold text-text-tertiary">80+</span>
        </div>
        <div className="flex items-center gap-1.5">
          <div className="h-2.5 w-2.5 rounded-sm bg-yellow-400" />
          <span className="text-[10px] font-bold text-text-tertiary">50~79</span>
        </div>
        <div className="flex items-center gap-1.5">
          <div className="h-2.5 w-2.5 rounded-sm bg-error" />
          <span className="text-[10px] font-bold text-text-tertiary">~49</span>
        </div>
      </div>
    </div>
  )
}
