import { MessageSquarePlus } from 'lucide-react'
import { Logo } from '@/components/ui/logo'
import { BetaBadge } from '@/components/ui/beta-badge'
import { Button } from '@/components/ui/button'
import type { AuthUser } from '@/stores/auth-store'

interface DashboardHeaderProps {
  user: AuthUser | null
  onLogout: () => void
  onFeedbackClick?: () => void
}

export const DashboardHeader = ({ user, onLogout, onFeedbackClick }: DashboardHeaderProps) => {
  return (
    <header className="lg:hidden sticky top-0 z-50 bg-background/95 backdrop-blur-md border-b border-border shadow-sm">
      <div className="flex h-16 items-center justify-between px-5">
        <div className="flex items-center gap-2">
          <Logo size={80} />
          <span className="text-xl font-extrabold tracking-tight text-text-primary">
            리허설
          </span>
          <BetaBadge size="md" />
        </div>
        <div className="flex items-center gap-3">
          <span className="hidden text-sm font-medium text-text-secondary sm:block">
            {user?.name}
          </span>
          {onFeedbackClick && (
            <Button
              variant="ghost"
              size="icon"
              onClick={onFeedbackClick}
              aria-label="피드백 보내기"
              className="rounded-xl border border-border text-text-secondary hover:text-violet-legacy"
            >
              <MessageSquarePlus size={18} />
            </Button>
          )}
          <Button
            variant="secondary"
            size="sm"
            onClick={onLogout}
            className="rounded-xl"
          >
            로그아웃
          </Button>
        </div>
      </div>
    </header>
  )
}
