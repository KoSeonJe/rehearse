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

const InterviewTimer = ({ startTime, onTick }: InterviewTimerProps) => {
  const displayRef = useRef<HTMLSpanElement>(null)
  const rafRef = useRef<number | null>(null)

  useEffect(() => {
    if (!startTime) return

    const tick = () => {
      const elapsed = Date.now() - startTime
      if (displayRef.current) {
        displayRef.current.textContent = formatTime(elapsed)
      }
      onTick?.(elapsed)
      rafRef.current = requestAnimationFrame(tick)
    }

    tick()

    return () => {
      if (rafRef.current) cancelAnimationFrame(rafRef.current)
    }
  }, [startTime, onTick])

  return (
    <span
      ref={displayRef}
      className="font-mono text-sm tabular-nums text-text-secondary"
    >
      00:00
    </span>
  )
}

export default InterviewTimer
