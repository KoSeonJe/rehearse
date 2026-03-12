import { memo, useEffect, useRef, type RefObject } from 'react'

interface AudioLevelIndicatorProps {
  audioLevelRef: RefObject<number>
}

const BARS = 12
const UPDATE_INTERVAL_MS = 100 // ~10fps

export const AudioLevelIndicator = memo(({ audioLevelRef }: AudioLevelIndicatorProps) => {
  const containerRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    let rafId: number
    let lastUpdate = 0

    const tick = () => {
      const now = performance.now()
      if (now - lastUpdate >= UPDATE_INTERVAL_MS) {
        lastUpdate = now
        const level = audioLevelRef.current
        const activeBars = Math.round(level * BARS)

        if (containerRef.current) {
          const children = containerRef.current.children
          for (let i = 0; i < children.length; i++) {
            const bar = children[i] as HTMLElement
            if (i < activeBars) {
              bar.classList.add('bg-success')
              bar.classList.remove('bg-border')
            } else {
              bar.classList.remove('bg-success')
              bar.classList.add('bg-border')
            }
          }
        }
      }
      rafId = requestAnimationFrame(tick)
    }

    rafId = requestAnimationFrame(tick)
    return () => cancelAnimationFrame(rafId)
  }, [audioLevelRef])

  return (
    <div className="flex items-center gap-1.5" role="meter" aria-label="마이크 레벨">
      <svg
        className="h-4 w-4 text-text-tertiary"
        fill="none"
        viewBox="0 0 24 24"
        stroke="currentColor"
        strokeWidth={2}
        aria-hidden="true"
      >
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          d="M12 18.75a6 6 0 006-6v-1.5m-6 7.5a6 6 0 01-6-6v-1.5m6 7.5v3.75m-3.75 0h7.5M12 15.75a3 3 0 01-3-3V4.5a3 3 0 116 0v8.25a3 3 0 01-3 3z"
        />
      </svg>
      <div ref={containerRef} className="flex items-end gap-0.5">
        {Array.from({ length: BARS }).map((_, i) => (
          <div
            key={i}
            className="w-1 rounded-full transition-all duration-75 bg-border"
            style={{ height: `${8 + i * 1.5}px` }}
          />
        ))}
      </div>
    </div>
  )
})

AudioLevelIndicator.displayName = 'AudioLevelIndicator'
