import { useState } from 'react'
import { ChevronDown, Circle, CircleCheck, Trash2 } from 'lucide-react'
import { useUpdateBookmarkStatus, useDeleteBookmark } from '@/hooks/use-review-bookmarks'
import { DeleteConfirmDialog } from '@/components/dashboard/delete-confirm-dialog'
import { AnswerComparisonView } from '@/components/review/answer-comparison-view'
import { POSITION_LABELS } from '@/constants/interview-labels'
import type { ReviewBookmarkListItem, BookmarkStatus } from '@/types/review-bookmark'
import type { Position } from '@/types/interview'

interface ReviewBookmarkCardProps {
  item: ReviewBookmarkListItem
  currentStatusFilter: BookmarkStatus
}

const formatDate = (dateStr: string): string => {
  const date = new Date(dateStr)
  return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}`
}

const formatInterviewTitle = (position: string, detail: string | null): string => {
  const label = POSITION_LABELS[position as Position]?.label ?? position
  return detail ? `${label} · ${detail}` : label
}

export const ReviewBookmarkCard = ({ item, currentStatusFilter }: ReviewBookmarkCardProps) => {
  const [isExpanded, setIsExpanded] = useState(false)
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false)

  const isResolved = item.resolvedAt !== null
  const updateStatus = useUpdateBookmarkStatus()
  const deleteBookmark = useDeleteBookmark()

  const handleCardClick = () => {
    setIsExpanded((prev) => !prev)
  }

  const handleToggleResolved = (e: React.MouseEvent) => {
    e.stopPropagation()
    updateStatus.mutate({
      id: item.id,
      resolved: !isResolved,
      status: currentStatusFilter,
    })
  }

  const handleDeleteClick = (e: React.MouseEvent) => {
    e.stopPropagation()
    setIsDeleteDialogOpen(true)
  }

  const handleDeleteConfirm = () => {
    deleteBookmark.mutate(
      {
        bookmarkId: item.id,
        timestampFeedbackId: item.timestampFeedbackId,
      },
      {
        onSettled: () => {
          setIsDeleteDialogOpen(false)
        },
      },
    )
  }

  const handleDeleteCancel = () => {
    setIsDeleteDialogOpen(false)
  }

  return (
    <>
      <article
        className={`rounded-card overflow-hidden transition-shadow duration-200 ${
          isResolved
            ? 'bg-success-light border border-success/20'
            : 'bg-surface border border-border shadow-toss hover:shadow-toss-lg'
        }`}
      >
        <button
          type="button"
          className="focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2 w-full text-left px-5 py-4 flex items-center gap-3"
          onClick={handleCardClick}
          aria-expanded={isExpanded}
        >
          <div className="flex-1 min-w-0">
            <p className={`text-[15px] font-semibold leading-snug line-clamp-2 mb-2 ${
              isResolved ? 'text-text-secondary' : 'text-text-primary'
            }`}>
              {item.questionText ?? '질문 텍스트가 없습니다.'}
            </p>
            <div className="flex flex-wrap items-center gap-2">
              <span className="text-[12px] text-text-tertiary">
                {formatInterviewTitle(item.interviewPosition, item.interviewPositionDetail)}
              </span>
              <span className="text-border">·</span>
              <span className="text-[12px] text-text-tertiary">{formatDate(item.interviewDate)}</span>
            </div>
          </div>

          <div className="flex-shrink-0 flex items-center gap-1.5">
            {isResolved ? (
              <button
                type="button"
                className="focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2 flex items-center gap-1.5 rounded-full bg-success text-white px-3 py-1.5 text-[13px] font-semibold border border-success hover:bg-[#059669] transition-colors whitespace-nowrap"
                aria-pressed={true}
                aria-label="복습중으로 되돌리기"
                onClick={handleToggleResolved}
                disabled={updateStatus.isPending}
              >
                <CircleCheck size={15} strokeWidth={2.2} aria-hidden="true" />
                복습완료
              </button>
            ) : (
              <button
                type="button"
                className="focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2 flex items-center gap-1.5 rounded-full border border-border bg-surface px-3 py-1.5 text-[13px] font-medium text-text-tertiary hover:border-success hover:text-success hover:bg-success-light transition-colors whitespace-nowrap"
                aria-pressed={false}
                aria-label="복습완료로 표시"
                onClick={handleToggleResolved}
                disabled={updateStatus.isPending}
              >
                <Circle size={15} strokeWidth={2.2} aria-hidden="true" />
                복습완료하기
              </button>
            )}

            <button
              type="button"
              className="focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2 w-8 h-8 flex items-center justify-center rounded-xl text-border hover:text-accent-hover hover:bg-accent-light transition-colors"
              aria-label="북마크 삭제"
              onClick={handleDeleteClick}
            >
              <Trash2 size={15} strokeWidth={2} aria-hidden="true" />
            </button>

            <ChevronDown
              size={16}
              strokeWidth={2}
              className={`flex-shrink-0 text-border transition-transform duration-200 ${isExpanded ? 'rotate-180' : ''}`}
              aria-hidden="true"
            />
          </div>
        </button>

        {isExpanded && (
          <div className="animate-fade-in">
            <AnswerComparisonView
              transcript={item.transcript}
              modelAnswer={item.modelAnswer}
              coachingImprovement={item.coachingImprovement}
            />
          </div>
        )}
      </article>

      <DeleteConfirmDialog
        isOpen={isDeleteDialogOpen}
        onConfirm={handleDeleteConfirm}
        onCancel={handleDeleteCancel}
        isPending={deleteBookmark.isPending}
      />
    </>
  )
}
