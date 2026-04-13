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

const STATUS_BADGE_CONFIG: Record<InterviewStatus, { label: string; className: string }> = {
  READY: {
    label: '준비됨',
    className: 'bg-blue-100 text-blue-700',
  },
  IN_PROGRESS: {
    label: '진행 중',
    className: 'bg-warning-light text-warning',
  },
  COMPLETED: {
    label: '완료',
    className: 'bg-success-light text-success',
  },
}

const CATEGORY_BADGE_CLASS: Record<InterviewType, string> = {
  CS_FUNDAMENTAL: 'bg-slate-100 text-slate-700',
  BEHAVIORAL: 'bg-pink-100 text-pink-700',
  RESUME_BASED: 'bg-rose-100 text-rose-700',
  LANGUAGE_FRAMEWORK: 'bg-indigo-100 text-indigo-700',
  SYSTEM_DESIGN: 'bg-violet-100 text-violet-700',
  SQL_MODELING: 'bg-amber-100 text-amber-700',
  DATA_PIPELINE: 'bg-orange-100 text-orange-700',
  UI_FRAMEWORK: 'bg-sky-100 text-sky-700',
  BROWSER_PERFORMANCE: 'bg-cyan-100 text-cyan-700',
  FULLSTACK_STACK: 'bg-teal-100 text-teal-700',
  INFRA_CICD: 'bg-emerald-100 text-emerald-700',
  CLOUD: 'bg-lime-100 text-lime-700',
}

const StatusBadge = ({ status }: StatusBadgeProps) => {
  const { label, className } = STATUS_BADGE_CONFIG[status]

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
    if (interview.status === 'COMPLETED') {
      navigate(`/interview/${interview.publicId}/feedback`)
    } else if (interview.status === 'READY' || interview.status === 'IN_PROGRESS') {
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
                답변 수
              </th>
              <th className="py-3 px-4 text-center text-xs font-semibold text-accent/70 uppercase tracking-wide">
                날짜
              </th>
              <th className="py-3 px-4 text-center text-xs font-semibold text-accent/70 uppercase tracking-wide">
                상태
              </th>
            </tr>
          </thead>
          <tbody>
            {interviews.map((interview) => {
              const isInProgress = interview.status === 'IN_PROGRESS'
              const positionLabel =
                POSITION_LABELS[interview.position]?.label ?? interview.position
              const typeBadges = interview.interviewTypes
                .slice(0, 2)
                .map((t: InterviewType) => ({
                  type: t,
                  label: INTERVIEW_TYPE_LABELS[t]?.label ?? t,
                }))

              return (
                <tr
                  key={interview.id}
                  onClick={() => handleRowClick(interview)}
                  className={`group border-b border-border/50 last:border-0 transition-colors hover:bg-accent-light/50 cursor-pointer ${
                    isInProgress ? 'opacity-80' : ''
                  }`}
                  title={isInProgress ? '클릭하여 면접 이어하기' : undefined}
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
                      {typeBadges.map(({ type, label }) => (
                        <span
                          key={type}
                          className={`rounded-badge px-2 py-0.5 text-xs font-semibold ${CATEGORY_BADGE_CLASS[type] ?? 'bg-slate-100 text-slate-700'}`}
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

                  {/* 답변 수 */}
                  <td className="py-4 px-4 text-center">
                    <span className="text-sm text-text-secondary">{interview.answerCount}</span>
                  </td>

                  {/* 날짜 */}
                  <td className="py-4 px-4 text-center">
                    <span className="text-sm text-text-tertiary">
                      {formatDate(interview.createdAt)}
                    </span>
                  </td>

                  {/* 상태 (hover/focus 시 우측 끝에 삭제 버튼이 absolute로 떠서 표시됨) */}
                  <td className="relative py-4 px-4 text-center">
                    <StatusBadge status={interview.status} />
                    <button
                      onClick={(e) => handleDeleteClick(e, interview.id)}
                      aria-label="면접 삭제"
                      disabled={deletingId === interview.id}
                      className="absolute right-3 top-1/2 -translate-y-1/2 opacity-0 group-hover:opacity-100 focus-visible:opacity-100 p-1.5 text-text-tertiary hover:text-error transition-all duration-150 cursor-pointer rounded-lg disabled:opacity-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent"
                    >
                      <Trash2 size={15} />
                    </button>
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
