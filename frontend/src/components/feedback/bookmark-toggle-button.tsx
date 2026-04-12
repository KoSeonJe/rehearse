import { useRef, useState } from 'react'
import { ListChecks, ListPlus } from 'lucide-react'
import { useCreateBookmark, useDeleteBookmark } from '@/hooks/use-review-bookmarks'
import { ApiError } from '@/lib/api-client'
import ReviewCoachMark from '@/components/feedback/review-coach-mark'
import ReviewToast from '@/components/common/review-toast'

const TOAST_SEEN_PREFIX = 'rehearse:review-toast-seen:'

interface BookmarkToggleButtonProps {
  timestampFeedbackId: number
  interviewId: number
  bookmarkId: number | undefined
}

const BookmarkToggleButton = ({
  timestampFeedbackId,
  interviewId,
  bookmarkId,
}: BookmarkToggleButtonProps) => {
  const isBookmarked = bookmarkId !== undefined
  const buttonRef = useRef<HTMLButtonElement>(null)
  const [isAnimating, setIsAnimating] = useState(false)
  const [showToast, setShowToast] = useState(false)

  const createBookmark = useCreateBookmark()
  const deleteBookmark = useDeleteBookmark()

  const isPending = createBookmark.isPending || deleteBookmark.isPending

  const triggerAnimation = () => {
    const prefersReducedMotion = window.matchMedia(
      '(prefers-reduced-motion: reduce)',
    ).matches
    if (prefersReducedMotion) return

    setIsAnimating(true)
    setTimeout(() => setIsAnimating(false), 180)
  }

  const handleClick = (e: React.MouseEvent) => {
    e.stopPropagation()

    if (isPending) return

    if (isBookmarked && bookmarkId !== undefined) {
      triggerAnimation()
      deleteBookmark.mutate(
        {
          bookmarkId,
          timestampFeedbackId,
          interviewId,
        },
        {
          onError: (error) => {
            if (!(error instanceof ApiError)) return
            // Non-409 errors are handled globally; nothing extra here
          },
        },
      )
    } else {
      triggerAnimation()
      createBookmark.mutate(
        {
          timestampFeedbackId,
          interviewId,
        },
        {
          onSuccess: () => {
            if (localStorage.getItem(`${TOAST_SEEN_PREFIX}${timestampFeedbackId}`) !== '1') {
              setShowToast(true)
            }
          },
          onError: (error) => {
            if (error instanceof ApiError && error.status === 409) {
              // Server says already bookmarked — state synced in hook, no toast
              return
            }
            // For other errors the hook handles rollback; could add error toast here
          },
        },
      )
    }
  }

  return (
    <div className="relative flex-shrink-0">
      <button
        ref={buttonRef}
        type="button"
        aria-pressed={isBookmarked}
        aria-label={isBookmarked ? '복습 목록에서 제거' : '복습 목록에 담기'}
        disabled={isPending}
        onClick={handleClick}
        className={[
          'flex items-center gap-1.5 rounded-full px-3.5 py-2 text-[13px] transition-colors',
          'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#6366F1] focus-visible:ring-offset-2',
          'disabled:cursor-not-allowed disabled:opacity-60',
          isAnimating ? 'bookmark-pop' : '',
          isBookmarked
            ? 'border border-[#C7D2FE] font-bold'
            : 'border border-[#E2E8F0] bg-white font-medium text-[#64748B] hover:border-[#6366F1] hover:bg-[#EEF2FF] hover:text-[#6366F1]',
        ]
          .filter(Boolean)
          .join(' ')}
        style={
          isBookmarked
            ? { backgroundColor: '#EEF2FF', color: '#4F46E5' }
            : undefined
        }
      >
        {isBookmarked ? (
          <ListChecks size={15} aria-hidden="true" />
        ) : (
          <ListPlus size={15} aria-hidden="true" />
        )}
        <span className="hidden sm:inline">
          {isBookmarked ? '복습 목록에 담김' : '복습 목록에 담기'}
        </span>
      </button>

      <ReviewCoachMark anchorRef={buttonRef} onButtonClick={handleClick} />

      {showToast && (
        <ReviewToast
          timestampFeedbackId={timestampFeedbackId}
          onDismiss={() => setShowToast(false)}
        />
      )}

      <style>{`
        @keyframes bookmark-pop {
          0%   { transform: scale(1); }
          40%  { transform: scale(1.15); }
          100% { transform: scale(1); }
        }
        .bookmark-pop {
          animation: bookmark-pop 0.18s ease-out forwards;
        }
        @media (prefers-reduced-motion: reduce) {
          .bookmark-pop {
            animation: none;
          }
        }
      `}</style>
    </div>
  )
}

export default BookmarkToggleButton
