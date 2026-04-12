import { Filter } from 'lucide-react'
import { POSITION_LABELS, INTERVIEW_TYPE_LABELS } from '@/constants/interview-labels'
import { POSITION_INTERVIEW_TYPES } from '@/constants/interview-labels'
import type { BookmarkStatus } from '@/types/review-bookmark'
import type { Position, InterviewType } from '@/types/interview'

interface ReviewListFilterBarProps {
  status: BookmarkStatus
  onStatusChange: (status: BookmarkStatus) => void
  positionFilter: Position | 'ALL'
  onPositionChange: (position: Position | 'ALL') => void
  interviewTypeFilter: InterviewType | 'ALL'
  onInterviewTypeChange: (type: InterviewType | 'ALL') => void
}

const STATUS_OPTIONS: { value: BookmarkStatus; label: string }[] = [
  { value: 'all', label: '전체' },
  { value: 'in_progress', label: '복습중' },
  { value: 'resolved', label: '복습완료' },
]

const POSITION_ENTRIES = Object.entries(POSITION_LABELS) as [Position, { label: string }][]

const SelectChevron = () => (
  <div className="pointer-events-none absolute right-2.5 top-1/2 -translate-y-1/2">
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width="12"
      height="12"
      viewBox="0 0 24 24"
      fill="none"
      stroke="#64748B"
      strokeWidth="2.5"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <polyline points="6 9 12 15 18 9" />
    </svg>
  </div>
)

export const ReviewListFilterBar = ({
  status,
  onStatusChange,
  positionFilter,
  onPositionChange,
  interviewTypeFilter,
  onInterviewTypeChange,
}: ReviewListFilterBarProps) => {
  const availableTypes: InterviewType[] =
    positionFilter === 'ALL' ? [] : POSITION_INTERVIEW_TYPES[positionFilter]

  const handlePositionChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    onPositionChange(e.target.value as Position | 'ALL')
  }

  const handleTypeChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    onInterviewTypeChange(e.target.value as InterviewType | 'ALL')
  }

  return (
    <div className="mb-6 flex flex-col sm:flex-row sm:items-center gap-3">
      {/* 상태 필터 칩 */}
      <div role="group" aria-label="상태 필터" className="flex items-center gap-1.5">
        {STATUS_OPTIONS.map((option) => {
          const isActive = status === option.value
          return (
            <button
              key={option.value}
              type="button"
              className={`focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#6366F1] focus-visible:ring-offset-2 rounded-full border px-3.5 py-1.5 text-[13px] font-medium transition-colors cursor-pointer ${
                isActive
                  ? 'bg-accent text-white border-accent'
                  : 'bg-white text-[#334155] border-[#E2E8F0] hover:border-[#334155]'
              }`}
              onClick={() => onStatusChange(option.value)}
              aria-pressed={isActive}
            >
              {option.label}
            </button>
          )
        })}
      </div>

      {/* 직무 → 카테고리 드롭다운 */}
      <div className="sm:ml-auto flex items-center gap-2">
        <Filter size={14} className="text-[#64748B]" aria-hidden="true" />

        {/* 직무 선택 */}
        <div className="relative">
          <select
            className="focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#6366F1] focus-visible:ring-offset-2 appearance-none bg-white border border-[#E2E8F0] rounded-xl px-3.5 py-1.5 pr-8 text-[13px] font-medium text-[#334155] cursor-pointer"
            aria-label="직무 필터"
            value={positionFilter}
            onChange={handlePositionChange}
          >
            <option value="ALL">전체 직무</option>
            {POSITION_ENTRIES.map(([key, { label }]) => (
              <option key={key} value={key}>
                {label}
              </option>
            ))}
          </select>
          <SelectChevron />
        </div>

        {/* 면접 카테고리 선택 (직무 선택 시 활성화) */}
        <div className="relative">
          <select
            className="focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#6366F1] focus-visible:ring-offset-2 appearance-none bg-white border border-[#E2E8F0] rounded-xl px-3.5 py-1.5 pr-8 text-[13px] font-medium text-[#334155] cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
            aria-label="카테고리 필터"
            value={interviewTypeFilter}
            onChange={handleTypeChange}
            disabled={positionFilter === 'ALL'}
          >
            <option value="ALL">전체 카테고리</option>
            {availableTypes.map((type) => (
              <option key={type} value={type}>
                {INTERVIEW_TYPE_LABELS[type].label}
              </option>
            ))}
          </select>
          <SelectChevron />
        </div>
      </div>
    </div>
  )
}
