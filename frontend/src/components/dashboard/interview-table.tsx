import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Trash2 } from 'lucide-react'
import type { InterviewListItem, InterviewStatus, InterviewType } from '@/types/interview'
import { POSITION_LABELS, INTERVIEW_TYPE_LABELS } from '@/constants/interview-labels'
import { DeleteConfirmDialog } from './delete-confirm-dialog'
import { EmptyState } from './empty-state'

interface StatusBadgeProps {
  status: InterviewStatus
}

const StatusBadge = ({ status }: StatusBadgeProps) => {
  const config: Record<InterviewStatus, { label: string; className: string }> = {
    COMPLETED: {
      label: '완료',
      className: 'bg-accent text-white',
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
    <span className={`rounded-badge px-2.5 py-0.5 text-xs font-bold ${className}`}>
      {label}
    </span>
  )
}

const formatDate = (iso: string): string => {
  const d = new Date(iso)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const InterviewTableSkeleton = () => (
  <div className="animate-pulse">
    {[...Array(4)].map((_, i) => (
      <div key={i} className="flex items-center gap-4 py-4 px-4 border-b border-border/50">
        <div className="h-4 w-36 bg-border/50 rounded-lg" />
        <div className="h-4 w-24 bg-border/50 rounded-lg" />
        <div className="h-4 w-12 bg-border/50 rounded-lg" />
        <div className="h-4 w-8 bg-border/50 rounded-lg" />
        <div className="h-4 w-24 bg-border/50 rounded-lg" />
        <div className="h-5 w-14 bg-border/50 rounded-badge" />
      </div>
    ))}
  </div>
)

interface InterviewTableProps {
  interviews: InterviewListItem[]
  isLoading: boolean
  onDelete: (id: number) => void
  deletingId: number | null
}

export const InterviewTable = ({
  interviews,
  isLoading,
  onDelete,
  deletingId,
}: InterviewTableProps) => {
  const navigate = useNavigate()
  const [dialogId, setDialogId] = useState<number | null>(null)

  if (isLoading) {
    return (
      <div className="rounded-card bg-surface overflow-hidden border border-border shadow-toss">
        <InterviewTableSkeleton />
      </div>
    )
  }

  if (interviews.length === 0) {
    return <EmptyState />
  }

  const handleRowClick = (interview: InterviewListItem) => {
    if (interview.status === 'IN_PROGRESS') return
    if (interview.status === 'COMPLETED') {
      navigate(`/interview/${interview.publicId}/feedback`)
    } else if (interview.status === 'READY') {
      navigate(`/interview/${interview.id}/ready`)
    }
  }

  const handleDeleteClick = (e: React.MouseEvent, id: number) => {
    e.stopPropagation()
    setDialogId(id)
  }

  const handleDeleteConfirm = () => {
    if (dialogId !== null) {
      onDelete(dialogId)
      setDialogId(null)
    }
  }

  const dialogInterview = interviews.find((i) => i.id === dialogId) ?? null

  return (
    <>
      <div className="rounded-card bg-surface overflow-hidden border border-border shadow-toss">
        <table className="w-full">
          <thead>
            <tr className="border-b border-border bg-accent-light">
              <th className="py-3 px-4 text-center text-xs font-semibold text-accent/70 uppercase tracking-wide">
                포지션
              </th>
              <th className="py-3 px-4 text-center text-xs font-semibold text-accent/70 uppercase tracking-wide">
                카테고리
              </th>
              <th className="py-3 px-4 text-center text-xs font-semibold text-accent/70 uppercase tracking-wide">
                설정 시간
              </th>
              <th className="py-3 px-4 text-center text-xs font-semibold text-accent/70 uppercase tracking-wide">
                질문 수
              </th>
              <th className="py-3 px-4 text-center text-xs font-semibold text-accent/70 uppercase tracking-wide">
                날짜
              </th>
              <th className="py-3 px-4 text-center text-xs font-semibold text-accent/70 uppercase tracking-wide">
                상태
              </th>
              <th className="py-3 px-4 text-center text-xs font-semibold text-accent/70 uppercase tracking-wide">
                액션
              </th>
            </tr>
          </thead>
          <tbody>
            {interviews.map((interview) => {
              const isInProgress = interview.status === 'IN_PROGRESS'
              const isDeletable = interview.status !== 'COMPLETED'
              const positionLabel =
                POSITION_LABELS[interview.position]?.label ?? interview.position
              const typeLabels = interview.interviewTypes
                .slice(0, 2)
                .map((t: InterviewType) => INTERVIEW_TYPE_LABELS[t]?.label ?? t)

              return (
                <tr
                  key={interview.id}
                  onClick={() => handleRowClick(interview)}
                  className={`group border-b border-border/50 last:border-0 transition-colors ${
                    isInProgress
                      ? 'opacity-60 cursor-default'
                      : 'hover:bg-accent-light/50 cursor-pointer'
                  }`}
                  title={isInProgress ? '면접이 진행 중입니다' : undefined}
                >
                  {/* 포지션 */}
                  <td className="py-4 px-4 text-center">
                    <span className="text-sm font-semibold text-text-primary">
                      {positionLabel}
                      {interview.positionDetail ? ` · ${interview.positionDetail}` : ''}
                    </span>
                  </td>

                  {/* 카테고리 */}
                  <td className="py-4 px-4">
                    <div className="flex flex-wrap justify-center gap-1">
                      {typeLabels.map((label) => (
                        <span
                          key={label}
                          className="rounded-badge bg-accent-light px-2 py-0.5 text-xs font-semibold text-accent"
                        >
                          {label}
                        </span>
                      ))}
                      {interview.interviewTypes.length > 2 && (
                        <span className="text-xs text-text-tertiary self-center">
                          +{interview.interviewTypes.length - 2}
                        </span>
                      )}
                    </div>
                  </td>

                  {/* 설정 시간 */}
                  <td className="py-4 px-4 text-center">
                    <span className="text-sm text-text-secondary">
                      {interview.durationMinutes}분
                    </span>
                  </td>

                  {/* 질문 수 */}
                  <td className="py-4 px-4 text-center">
                    <span className="text-sm text-text-secondary">{interview.questionCount}</span>
                  </td>

                  {/* 날짜 */}
                  <td className="py-4 px-4 text-center">
                    <span className="text-sm text-text-tertiary">
                      {formatDate(interview.createdAt)}
                    </span>
                  </td>

                  {/* 상태 */}
                  <td className="py-4 px-4 text-center">
                    <StatusBadge status={interview.status} />
                  </td>

                  {/* 액션 */}
                  <td className="py-4 px-4">
                    <div className="flex items-center justify-center gap-1">
                      {isDeletable && (
                        <button
                          onClick={(e) => handleDeleteClick(e, interview.id)}
                          aria-label="면접 삭제"
                          disabled={deletingId === interview.id}
                          className="opacity-0 group-hover:opacity-100 p-1.5 text-text-tertiary hover:text-error transition-all duration-150 cursor-pointer rounded-lg disabled:opacity-50"
                        >
                          <Trash2 size={15} />
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>

      <DeleteConfirmDialog
        isOpen={dialogId !== null}
        onConfirm={handleDeleteConfirm}
        onCancel={() => setDialogId(null)}
        isPending={dialogInterview !== null && deletingId === dialogId}
      />
    </>
  )
}
