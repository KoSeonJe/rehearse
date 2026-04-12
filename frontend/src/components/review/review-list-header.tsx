interface ReviewListHeaderProps {
  total: number
}

export const ReviewListHeader = ({ total }: ReviewListHeaderProps) => {
  return (
    <header className="mb-6">
      <div className="flex items-baseline gap-3 mb-1">
        <h1 className="text-[22px] sm:text-[26px] font-extrabold text-text-primary">복습 목록</h1>
        <span className="text-[15px] font-semibold text-text-tertiary">{total}개</span>
      </div>
      <p className="text-[14px] text-text-tertiary">
        담아둔 답변을 카테고리별로 모아 보고, 복습 상태를 추적하세요.
      </p>
    </header>
  )
}
