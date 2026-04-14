interface BetaBadgeProps {
  size?: 'sm' | 'md'
  className?: string
}

export const BetaBadge = ({ size = 'md', className = '' }: BetaBadgeProps) => {
  const sizeClass =
    size === 'md'
      ? 'text-xs px-2 py-0.5'
      : 'text-[10px] px-1.5 py-0.5'

  return (
    <span
      className={`bg-primary/10 text-primary border border-primary/30 rounded-full font-bold tracking-wider uppercase ${sizeClass} ${className}`}
      aria-label="베타 서비스"
      title="정식 출시 전 베타 서비스입니다"
    >
      BETA
    </span>
  )
}
