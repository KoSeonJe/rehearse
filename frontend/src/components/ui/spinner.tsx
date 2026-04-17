interface SpinnerProps {
  className?: string
}

export const Spinner = ({ className = 'h-5 w-5' }: SpinnerProps) => {
  return (
    <div
      className={`animate-spin rounded-full border-2 border-violet-legacy border-t-transparent ${className}`}
      role="status"
      aria-label="로딩 중"
    >
      <span className="sr-only">로딩 중</span>
    </div>
  )
}
