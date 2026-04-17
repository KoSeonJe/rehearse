import { useNavigate, useLocation } from 'react-router-dom'
import { LayoutDashboard, ListChecks, User } from 'lucide-react'
import { Logo } from '@/components/ui/logo'
import { BetaBadge } from '@/components/ui/beta-badge'
import { Button } from '@/components/ui/button'
import type { AuthUser } from '@/stores/auth-store'

interface SidebarProps {
  user: AuthUser | null
  onLogout: () => void
}

export const Sidebar = ({ user, onLogout }: SidebarProps) => {
  const navigate = useNavigate()
  const location = useLocation()

  const isDashboardActive = location.pathname === '/' || location.pathname === '/dashboard'
  const isReviewListActive = location.pathname === '/review-list'

  return (
    <aside className="hidden lg:flex flex-col fixed left-0 top-0 h-screen w-60 bg-surface border-r border-border shadow-toss z-40">
      {/* 로고 영역 */}
      <div className="flex items-center gap-2.5 px-5 py-5 border-b border-border/50">
        <Logo size={36} />
        <span className="text-lg font-extrabold tracking-tight text-text-primary">리허설</span>
        <BetaBadge size="sm" />
      </div>

      {/* 네비게이션 */}
      <nav className="flex-1 px-3 py-4 flex flex-col gap-1">
        {/* TODO(design): variant 판단 보류 — active/inactive 조건부 스타일, 사용자 확인 필요 */}
        <button
          onClick={() => navigate('/')}
          className={`flex items-center gap-3 w-full px-3 py-2.5 rounded-xl text-sm font-medium transition-colors cursor-pointer ${
            isDashboardActive
              ? 'bg-violet-legacy-light text-violet-legacy font-bold'
              : 'text-text-secondary hover:bg-border/40 hover:text-text-primary'
          }`}
        >
          <LayoutDashboard size={18} />
          대시보드
        </button>

        <button
          onClick={() => navigate('/review-list')}
          className={`flex items-center gap-3 w-full px-3 py-2.5 rounded-xl text-sm font-medium transition-colors cursor-pointer ${
            isReviewListActive
              ? 'bg-[#EEF2FF] text-[#4F46E5] font-semibold'
              : 'text-text-secondary hover:bg-border/40 hover:text-text-primary'
          }`}
          aria-current={isReviewListActive ? 'page' : undefined}
        >
          <ListChecks size={18} />
          복습 목록
        </button>

      </nav>

      {/* 유저 프로필 */}
      <div className="px-3 py-4 border-t border-border/50">
        <div className="flex items-center gap-3 px-2 py-2">
          <div className="flex-shrink-0 w-8 h-8 rounded-full bg-border flex items-center justify-center">
            <User size={16} className="text-text-tertiary" />
          </div>
          <span className="flex-1 text-sm font-medium text-text-primary truncate">
            {user?.name ?? ''}
          </span>
          <Button
            variant="ghost"
            size="sm"
            onClick={onLogout}
            className="text-xs text-text-tertiary hover:text-text-secondary px-2 py-1 h-auto"
          >
            로그아웃
          </Button>
        </div>
      </div>
    </aside>
  )
}
