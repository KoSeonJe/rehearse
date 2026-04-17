import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Trash2 } from 'lucide-react'
import { Card } from '@/components/ui/card'
import type { InterviewListItem, InterviewStatus, InterviewType } from '@/types/interview'
import { POSITION_LABELS, INTERVIEW_TYPE_LABELS } from '@/constants/interview-labels'
import { DeleteConfirmDialog } from './delete-confirm-dialog'

interface StatusBadgeProps {
  status: InterviewStatus
}

const StatusBadge = ({ status }: StatusBadgeProps) => {
  const config: Record<InterviewStatus, { label: string; className: string }> = {
    COMPLETED: {
      label: '완료',
      className: 'bg-success-light text-success',
    },
    READY: {
      label: '준비됨',
      className: 'bg-blue-100 text-blue-700',
    },
    IN_PROGRESS: {
      label: '진행 중',
      className: 'bg-warning-light text-warning',
    },
  }

  const { label, className } = config[status]

  return (
    <span
      className={`rounded-badge px-2.5 py-0.5 text-xs font-bold ${className}`}
    >
      {label}
    </span>
  )
}

const formatDate = (iso: string): string => {
  const d = new Date(iso)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

interface InterviewCardProps {
  interview: InterviewListItem
  onDelete: (id: number) => void
  isDeleting: boolean
}

export const InterviewCard = ({ interview, onDelete, isDeleting }: InterviewCardProps) => {
  const navigate = useNavigate()
  const [showDeleteDialog, setShowDeleteDialog] = useState(false)

  const isInProgress = interview.status === 'IN_PROGRESS'

  const handleCardClick = () => {
    if (isInProgress) return
    if (interview.status === 'COMPLETED') {
      navigate(`/interview/${interview.publicId}/feedback`)
    } else if (interview.status === 'READY') {
      navigate(`/interview/${interview.id}/ready`)
    }
  }

  const handleDeleteClick = (e: React.MouseEvent) => {
    e.stopPropagation()
    setShowDeleteDialog(true)
  }

  const handleDeleteConfirm = () => {
    onDelete(interview.id)
    setShowDeleteDialog(false)
  }

  const positionLabel = POSITION_LABELS[interview.position]?.label ?? interview.position
  const typeLabels = interview.interviewTypes.map(
    (t: InterviewType) => INTERVIEW_TYPE_LABELS[t]?.label ?? t,
  )

  return (
    <>
      <Card
        onClick={handleCardClick}
        className={`bg-surface p-5 border border-border transition-colors duration-200 ${
          isInProgress
            ? 'opacity-60 cursor-default shadow-sm'
            : 'cursor-pointer shadow-sm hover:shadow-md'
        }`}
        title={isInProgress ? '면접이 진행 중입니다' : undefined}
      >
        {/* 1행: 포지션 */}
        <h3 className="font-bold text-text-primary">
          {positionLabel}
          {interview.positionDetail ? ` · ${interview.positionDetail}` : ''}
        </h3>

        {/* 2행: 태그 + 메타 */}
        <div className="mt-3 flex flex-wrap items-center gap-2">
          {typeLabels.map((label) => (
            <span
              key={label}
              className="rounded-badge bg-violet-legacy-light px-2.5 py-0.5 text-xs font-semibold text-violet-legacy"
            >
              {label}
            </span>
          ))}
          <span className="text-xs text-text-tertiary">{interview.durationMinutes}분</span>
          <span className="text-xs text-text-tertiary">답변 {interview.answerCount}개</span>
        </div>

        {/* 3행: 날짜 + 상태 배지 + 삭제 */}
        <div className="mt-4 flex items-center justify-between">
          <span className="text-xs text-text-tertiary">{formatDate(interview.createdAt)}</span>
          <div className="flex items-center gap-2">
            <StatusBadge status={interview.status} />
            <button
              onClick={handleDeleteClick}
              aria-label="면접 삭제"
              className="p-2 text-text-tertiary hover:text-error transition-colors duration-200 cursor-pointer rounded-lg"
            >
              <Trash2 size={16} />
            </button>
          </div>
        </div>
      </Card>

      <DeleteConfirmDialog
        isOpen={showDeleteDialog}
        onConfirm={handleDeleteConfirm}
        onCancel={() => setShowDeleteDialog(false)}
        isPending={isDeleting}
      />
    </>
  )
}
