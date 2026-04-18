import { ListChecks, ListPlus } from 'lucide-react'

interface ReviewEmptyStateProps {
  isFiltered?: boolean
}

export const ReviewEmptyState = ({ isFiltered = false }: ReviewEmptyStateProps) => {
  if (isFiltered) {
    return (
      <div className="flex flex-col items-center justify-center py-16 text-center rounded-2xl bg-card shadow-sm">
        <ListChecks
          size={36}
          className="mb-4 text-border"
          strokeWidth={1.5}
          aria-hidden="true"
        />
        <p className="text-[15px] font-medium text-text-secondary mb-1">
          해당 조건의 답변이 없어요.
        </p>
        <p className="text-[13px] text-text-tertiary">
          필터를 변경하거나 전체 목록을 확인해 보세요.
        </p>
      </div>
    )
  }

  return (
    <div className="flex flex-col items-center justify-center py-24 text-center">
      <ListChecks
        size={48}
        className="mb-5 text-border"
        strokeWidth={1.5}
        aria-hidden="true"
      />
      <p className="text-[16px] font-semibold text-text-secondary mb-2">
        아직 담긴 답변이 없어요.
      </p>
      <p className="text-[14px] text-text-tertiary leading-relaxed max-w-xs">
        면접 피드백에서{' '}
        <span className="inline-flex items-center gap-1 align-middle mx-0.5 px-1.5 py-0.5 rounded-md bg-background text-text-secondary text-[12px] font-medium">
          <ListPlus size={11} aria-hidden="true" />
          복습 목록에 담기
        </span>{' '}
        버튼을 눌러 답변을 담아보세요.
      </p>
    </div>
  )
}
