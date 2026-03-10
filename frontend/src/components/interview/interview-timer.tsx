import { useEffect, useRef } from 'react'

interface InterviewTimerProps {
  startTime: number | null
  onTick?: (elapsed: number) => void
}

const formatTime = (ms: number): string => {
  const totalSeconds = Math.floor(ms / 1000)
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60

  const pad = (n: number) => n.toString().padStart(2, '0')

  return hours > 0
    ? `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`
    : `${pad(minutes)}:${pad(seconds)}`
}

export const InterviewTimer = ({ startTime, onTick }: InterviewTimerProps) => {
  const displayRef = useRef<HTMLSpanElement>(null)
  const onTickRef = useRef(onTick)
  onTickRef.current = onTick

  useEffect(() => {
    if (!startTime) return

    const tick = () => {
      const elapsed = Date.now() - startTime
      if (displayRef.current) {
        displayRef.current.textContent = formatTime(elapsed)
      }
      onTickRef.current?.(elapsed)
    }

    tick()
    const intervalId = setInterval(tick, 1000)

    return () => {
      clearInterval(intervalId)
    }
  }, [startTime])

  return (
    <span
      ref={displayRef}
      role="timer"
      aria-label="면접 경과 시간"
      className="font-mono text-sm tabular-nums text-text-secondary"
    >
      00:00
    </span>
  )
}

