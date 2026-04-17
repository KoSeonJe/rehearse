import { useCallback, useEffect, useState } from 'react'
import { Star } from 'lucide-react'
import { useSubmitServiceFeedback } from '@/hooks/use-service-feedback'
import type { FeedbackSource } from '@/types/service-feedback'

interface ServiceFeedbackModalProps {
  isOpen: boolean
  onClose: () => void
  source: FeedbackSource
}

const MIN_CONTENT_LENGTH = 10

export const ServiceFeedbackModal = ({
  isOpen,
  onClose,
  source,
}: ServiceFeedbackModalProps) => {
  const [rating, setRating] = useState<number | undefined>(undefined)
  const [hoverRating, setHoverRating] = useState<number | undefined>(undefined)
  const [content, setContent] = useState('')

  const { mutate: submitFeedback, isPending } = useSubmitServiceFeedback()

  const resetAndClose = useCallback(() => {
    setRating(undefined)
    setHoverRating(undefined)
    setContent('')
    onClose()
  }, [onClose])

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !isPending) resetAndClose()
    }
    if (isOpen) {
      document.addEventListener('keydown', handleKeyDown)
    }
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [isOpen, isPending, resetAndClose])

  if (!isOpen) return null

  const handleStarClick = (starIndex: number) => {
    if (rating === starIndex) {
      setRating(undefined)
    } else {
      setRating(starIndex)
    }
  }

  const handleSubmit = () => {
    if (content.length < MIN_CONTENT_LENGTH || isPending) return
    submitFeedback(
      { content, rating, source },
      { onSuccess: () => resetAndClose() },
    )
  }

  const isContentValid = content.length >= MIN_CONTENT_LENGTH
  const displayRating = hoverRating ?? rating

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm px-4"
      onClick={isPending ? undefined : resetAndClose}
    >
      <div
        className="bg-white rounded-card p-6 shadow-toss-lg max-w-md w-full mx-4"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-labelledby="feedback-dialog-title"
      >
        <h2
          id="feedback-dialog-title"
          className="text-base font-extrabold text-text-primary"
        >
          서비스 피드백
        </h2>
        <p className="mt-1 text-sm text-text-secondary">
          Rehearse를 사용하시면서 느낀 점을 알려주세요
        </p>

        {/* 별점 */}
        <div className="mt-4 flex items-center gap-1">
          {[1, 2, 3, 4, 5].map((star) => (
            <button
              key={star}
              type="button"
              onClick={() => handleStarClick(star)}
              onMouseEnter={() => setHoverRating(star)}
              onMouseLeave={() => setHoverRating(undefined)}
              className="cursor-pointer transition-transform hover:scale-110 active:scale-95"
              aria-label={`별점 ${star}점`}
            >
              <Star
                size={24}
                className={
                  displayRating !== undefined && star <= displayRating
                    ? 'fill-warning text-warning'
                    : 'text-gray-300'
                }
              />
            </button>
          ))}
          <span className="ml-2 text-xs text-text-secondary">선택사항</span>
        </div>

        {/* textarea */}
        <div className="mt-4">
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="서비스에 대한 의견을 자유롭게 남겨주세요 (최소 10자)"
            rows={4}
            className="w-full resize-none rounded-button border border-border px-3 py-2 text-sm text-text-primary placeholder:text-text-secondary focus:outline-none focus:ring-2 focus:ring-violet-legacy/50 focus:border-violet-legacy transition-all"
          />
          <p
            className={`mt-1 text-xs text-right ${isContentValid ? 'text-text-secondary' : 'text-red-500'}`}
          >
            {content.length}/{MIN_CONTENT_LENGTH}자
          </p>
        </div>

        {/* 버튼 */}
        <div className="mt-4 flex gap-3">
          {source === 'AUTO_POPUP' && (
            <button
              type="button"
              onClick={resetAndClose}
              disabled={isPending}
              className="flex-1 h-11 rounded-button border border-border text-sm font-bold text-text-secondary hover:bg-surface transition-all cursor-pointer disabled:opacity-50"
            >
              나중에
            </button>
          )}
          <button
            type="button"
            onClick={handleSubmit}
            disabled={!isContentValid || isPending}
            className="flex-1 h-11 rounded-button bg-violet-legacy text-white text-sm font-bold hover:opacity-90 active:scale-95 transition-all cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isPending ? '보내는 중...' : '보내기'}
          </button>
        </div>
      </div>
    </div>
  )
}
