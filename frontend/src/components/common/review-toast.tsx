import { useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'

const TOAST_SEEN_PREFIX = 'rehearse:review-toast-seen:'
const AUTO_DISMISS_MS = 3000

interface ReviewToastProps {
  timestampFeedbackId: number
  onDismiss: () => void
}

const ReviewToast = ({ timestampFeedbackId, onDismiss }: ReviewToastProps) => {
  const navigate = useNavigate()
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => {
    try {
      localStorage.setItem(`${TOAST_SEEN_PREFIX}${timestampFeedbackId}`, '1')
    } catch {
      // localStorage unavailable — ignore
    }

    timerRef.current = setTimeout(() => {
      onDismiss()
    }, AUTO_DISMISS_MS)

    return () => {
      if (timerRef.current !== null) {
        clearTimeout(timerRef.current)
      }
    }
  }, [onDismiss])

  const handleNavigate = () => {
    if (timerRef.current !== null) {
      clearTimeout(timerRef.current)
    }
    onDismiss()
    navigate('/review-list')
  }

  return (
    <div
      role="status"
      aria-live="polite"
      aria-atomic="true"
      className="fixed bottom-6 right-6 z-50 flex max-w-sm items-center gap-3 rounded-2xl bg-[#0F172A] px-5 py-3.5 text-white"
      style={{
        boxShadow: '0 8px 24px rgba(0,0,0,0.18)',
        animation: 'review-toast-in 0.25s ease-out forwards',
      }}
    >
      <div
        className="flex h-6 w-6 flex-shrink-0 items-center justify-center rounded-full"
        style={{ backgroundColor: '#6366F1' }}
        aria-hidden="true"
      >
        <svg
          xmlns="http://www.w3.org/2000/svg"
          width="13"
          height="13"
          viewBox="0 0 24 24"
          fill="none"
          stroke="white"
          strokeWidth="3"
          strokeLinecap="round"
          strokeLinejoin="round"
          aria-hidden="true"
        >
          <polyline points="20 6 9 17 4 12" />
        </svg>
      </div>

      <div className="min-w-0 flex-1">
        <p className="text-[13px] font-medium leading-snug">
          복습 목록에 담겼어요.
        </p>
      </div>

      <button
        type="button"
        onClick={handleNavigate}
        className="flex-shrink-0 text-[13px] font-bold underline underline-offset-2 transition-opacity hover:opacity-70 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white focus-visible:ring-offset-2 focus-visible:ring-offset-[#0F172A]"
        style={{ color: '#6366F1' }}
      >
        보러가기
      </button>

      <style>{`
        @keyframes review-toast-in {
          from { transform: translateY(12px); opacity: 0; }
          to   { transform: translateY(0);    opacity: 1; }
        }
        @media (prefers-reduced-motion: reduce) {
          @keyframes review-toast-in {
            from { opacity: 0; }
            to   { opacity: 1; }
          }
        }
      `}</style>
    </div>
  )
}

export default ReviewToast
