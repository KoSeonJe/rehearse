import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ListChecks, ListPlus } from 'lucide-react'
import { useCreateBookmark, useDeleteBookmark } from '@/hooks/use-review-bookmarks'
import { ApiError } from '@/lib/api-client'
import { showReviewToast } from '@/components/common/review-toast'
import { TOAST_SEEN_PREFIX } from '@/constants/review-bookmark'

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
  const [isAnimating, setIsAnimating] = useState(false)
  const navigate = useNavigate()

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
              showReviewToast(timestampFeedbackId, navigate)
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
        type="button"
        aria-pressed={isBookmarked}
        aria-label={isBookmarked ? '복습 목록에서 제거' : '복습 목록에 담기'}
        disabled={isPending}
        onClick={handleClick}
        className={[
          'flex items-center gap-1.5 rounded-full px-3.5 py-2 text-[13px] transition-colors',
          'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
          'disabled:cursor-not-allowed disabled:opacity-60',
          isAnimating ? 'animate-bookmark-pop motion-reduce:animate-none' : '',
          isBookmarked
            ? 'border border-primary/30 font-bold bg-muted text-primary'
            : 'border border-border bg-card font-medium text-text-secondary hover:border-primary hover:bg-muted hover:text-primary',
        ]
          .filter(Boolean)
          .join(' ')}
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

    </div>
  )
}

export default BookmarkToggleButton
