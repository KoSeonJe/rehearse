import { memo, useEffect, useRef } from 'react'
import { formatTimeFull } from '@/lib/format-utils'

interface InterviewTimerProps {
  startTime: number | null
  durationMinutes?: number | null
  onTick?: (elapsed: number) => void
  onTimeWarning?: () => void
  onTimeExpired?: () => void
}

const TIME_WARNING_THRESHOLD_MS = 120_000

export const InterviewTimer = memo(({ startTime, durationMinutes, onTick, onTimeWarning, onTimeExpired }: InterviewTimerProps) => {
  const displayRef = useRef<HTMLSpanElement>(null)
  const onTickRef = useRef(onTick)
  const onTimeWarningRef = useRef(onTimeWarning)
  const onTimeExpiredRef = useRef(onTimeExpired)
  const warningFiredRef = useRef(false)
  const expiredFiredRef = useRef(false)

  useEffect(() => {
    onTickRef.current = onTick
    onTimeWarningRef.current = onTimeWarning
    onTimeExpiredRef.current = onTimeExpired
  })

  useEffect(() => {
    if (!startTime) return
    warningFiredRef.current = false
    expiredFiredRef.current = false

    const totalMs = durationMinutes ? durationMinutes * 60 * 1000 : null

    const tick = () => {
      const elapsed = Date.now() - startTime

      if (totalMs) {
        const remaining = totalMs - elapsed
        if (displayRef.current) {
          displayRef.current.textContent = formatTimeFull(Math.max(0, remaining))
          // 2분 이하 경고 스타일
          if (remaining <= TIME_WARNING_THRESHOLD_MS && remaining > 0) {
            displayRef.current.classList.add('text-warning')
            displayRef.current.classList.remove('text-text-secondary')
          } else if (remaining <= 0) {
            displayRef.current.classList.add('text-error')
            displayRef.current.classList.remove('text-text-secondary', 'text-warning')
          }
        }
        if (remaining <= TIME_WARNING_THRESHOLD_MS && !warningFiredRef.current) {
          warningFiredRef.current = true
          onTimeWarningRef.current?.()
        }
        if (remaining <= 0 && !expiredFiredRef.current) {
          expiredFiredRef.current = true
          onTimeExpiredRef.current?.()
        }
      } else {
        if (displayRef.current) {
          displayRef.current.textContent = formatTimeFull(elapsed)
        }
      }

      onTickRef.current?.(elapsed)
    }

    tick()
    const intervalId = setInterval(tick, 1000)
    return () => clearInterval(intervalId)
  }, [startTime, durationMinutes])

  return (
    <span
      ref={displayRef}
      role="timer"
      aria-label={durationMinutes ? '남은 면접 시간' : '면접 경과 시간'}
      className="font-mono text-base tabular-nums text-text-secondary transition-colors duration-700"
    >
      {durationMinutes ? formatTimeFull(durationMinutes * 60 * 1000) : '00:00'}
    </span>
  )
})

InterviewTimer.displayName = 'InterviewTimer'
