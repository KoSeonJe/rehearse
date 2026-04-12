import { useCallback, useEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'

const COACH_SEEN_KEY = 'rehearse:review-coach-seen-v1'
const SHOW_DELAY_MS = 600
const AUTO_DISMISS_MS = 8000
const ARROW_LEFT_OFFSET = 28
const GAP = 10

interface ReviewCoachMarkProps {
  anchorRef: React.RefObject<HTMLButtonElement | null>
}

interface PopoverPosition {
  top: number
  left: number
  arrowLeft: number
}

const ReviewCoachMark = ({ anchorRef }: ReviewCoachMarkProps) => {
  const [isVisible, setIsVisible] = useState(false)
  const [position, setPosition] = useState<PopoverPosition | null>(null)
  const popoverRef = useRef<HTMLDivElement>(null)
  const autoTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const showTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  const hasSeen = (): boolean => {
    try {
      return localStorage.getItem(COACH_SEEN_KEY) === '1'
    } catch {
      return true
    }
  }

  const markSeen = () => {
    try {
      localStorage.setItem(COACH_SEEN_KEY, '1')
    } catch {
      // localStorage unavailable — treat as seen to avoid re-showing
    }
  }

  const dismiss = useCallback(() => {
    setIsVisible(false)
    markSeen()
    if (autoTimerRef.current !== null) {
      clearTimeout(autoTimerRef.current)
      autoTimerRef.current = null
    }
  }, [])

  const updatePosition = useCallback(() => {
    const anchor = anchorRef.current
    if (!anchor) return
    const rect = anchor.getBoundingClientRect()
    setPosition({
      top: rect.bottom + GAP,
      left: rect.left,
      arrowLeft: ARROW_LEFT_OFFSET,
    })
  }, [anchorRef])

  useEffect(() => {
    if (hasSeen()) return

    showTimerRef.current = setTimeout(() => {
      if (!hasSeen()) {
        setIsVisible(true)
        autoTimerRef.current = setTimeout(dismiss, AUTO_DISMISS_MS)
      }
    }, SHOW_DELAY_MS)

    return () => {
      if (showTimerRef.current !== null) clearTimeout(showTimerRef.current)
      if (autoTimerRef.current !== null) clearTimeout(autoTimerRef.current)
    }
  }, [dismiss])

  useEffect(() => {
    if (!isVisible) return
    updatePosition()
    window.addEventListener('scroll', updatePosition, true)
    window.addEventListener('resize', updatePosition)
    return () => {
      window.removeEventListener('scroll', updatePosition, true)
      window.removeEventListener('resize', updatePosition)
    }
  }, [isVisible, updatePosition])

  useEffect(() => {
    if (!isVisible) return

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        dismiss()
        anchorRef.current?.focus()
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [isVisible, dismiss, anchorRef])

  useEffect(() => {
    if (!isVisible) return

    const handlePointerDown = (e: PointerEvent) => {
      const target = e.target as Node
      if (
        popoverRef.current &&
        !popoverRef.current.contains(target) &&
        anchorRef.current &&
        !anchorRef.current.contains(target)
      ) {
        dismiss()
      }
    }

    document.addEventListener('pointerdown', handlePointerDown)
    return () => document.removeEventListener('pointerdown', handlePointerDown)
  }, [isVisible, dismiss, anchorRef])

  if (!isVisible || !position) return null

  return createPortal(
    <div
      ref={popoverRef}
      role="dialog"
      aria-label="복습 목록 안내"
      aria-modal="false"
      className="fixed z-50 w-64 min-w-[240px] rounded-2xl bg-white p-4 shadow-lg"
      style={{
        top: position.top,
        left: position.left,
      }}
    >
      <div
        aria-hidden="true"
        style={{
          position: 'absolute',
          top: '-8px',
          left: `${position.arrowLeft}px`,
          width: 0,
          height: 0,
          borderLeft: '8px solid transparent',
          borderRight: '8px solid transparent',
          borderBottom: '8px solid #fff',
          filter: 'drop-shadow(0 -1px 1px rgba(0,0,0,0.06))',
        }}
      />

      <p className="mb-3 text-[14px] font-medium leading-[1.6] text-text-secondary">
        답변을 다시 꺼내 보고 싶을 때,
        <br />
        여기를 눌러 담아두세요.
      </p>

      <div className="flex justify-end">
        <button
          type="button"
          onClick={() => dismiss()}
          className="rounded-full bg-accent px-4 py-1.5 text-[13px] font-bold text-white transition-opacity hover:opacity-90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#6366F1] focus-visible:ring-offset-2"
        >
          알겠어요
        </button>
      </div>
    </div>,
    document.body,
  )
}

export default ReviewCoachMark
