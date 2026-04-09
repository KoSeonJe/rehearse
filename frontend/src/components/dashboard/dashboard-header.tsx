import { MessageSquarePlus } from 'lucide-react'
import { Logo } from '@/components/ui/logo'
import type { AuthUser } from '@/stores/auth-store'

interface DashboardHeaderProps {
  user: AuthUser | null
  onLogout: () => void
  onFeedbackClick?: () => void
}

export const DashboardHeader = ({ user, onLogout, onFeedbackClick }: DashboardHeaderProps) => {
  return (
    <header className="lg:hidden sticky top-0 z-50 bg-white/95 backdrop-blur-md border-b border-border shadow-sm">
      <div className="flex h-16 items-center justify-between px-5">
        <div className="flex items-center gap-2">
          <Logo size={80} />
          <span className="text-xl font-extrabold tracking-tight text-text-primary">
            리허설
          </span>
        </div>
        <div className="flex items-center gap-3">
          <span className="hidden text-sm font-medium text-text-secondary sm:block">
            {user?.name}
          </span>
          {onFeedbackClick && (
            <button
              onClick={onFeedbackClick}
              className="rounded-xl border border-border p-2 text-text-secondary transition-colors hover:bg-surface hover:text-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-border cursor-pointer"
              aria-label="피드백 보내기"
            >
              <MessageSquarePlus size={18} />
            </button>
          )}
          <button
            onClick={onLogout}
            className="rounded-xl border border-border px-4 py-2 text-sm font-medium text-text-secondary transition-colors hover:bg-surface focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-border cursor-pointer"
          >
            로그아웃
          </button>
        </div>
      </div>
    </header>
  )
}
