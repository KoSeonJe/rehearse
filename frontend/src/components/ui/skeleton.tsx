interface SkeletonProps {
  className?: string
}

export const Skeleton = ({ className = '' }: SkeletonProps) => {
  return (
    <div
      className={`animate-pulse rounded bg-border ${className}`}
      aria-hidden="true"
    />
  )
}
