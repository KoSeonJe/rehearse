import { Badge } from '@/components/ui/badge'
import { cn } from '@/lib/utils'

interface BetaBadgeProps {
  size?: 'sm' | 'md'
  className?: string
}

export const BetaBadge = ({ size = 'md', className = '' }: BetaBadgeProps) => {
  return (
    <Badge
      variant="outline"
      className={cn(
        'border-primary/30 bg-primary/10 text-primary font-bold tracking-wider uppercase rounded-full',
        size === 'sm' ? 'text-[10px] px-1.5 py-0.5' : 'text-xs px-2 py-0.5',
        className,
      )}
      aria-label="베타 서비스"
      title="정식 출시 전 베타 서비스입니다"
    >
      BETA
    </Badge>
  )
}
