import { useNavigate, useLocation } from 'react-router-dom'
import { LayoutDashboard, User } from 'lucide-react'
import { Logo } from '@/components/ui/logo'
import type { AuthUser } from '@/stores/auth-store'

interface SidebarProps {
  user: AuthUser | null
  onLogout: () => void
}

export const Sidebar = ({ user, onLogout }: SidebarProps) => {
  const navigate = useNavigate()
  const location = useLocation()

  const isDashboardActive = location.pathname === '/' || location.pathname === '/dashboard'

  return (
    <aside className="hidden lg:flex flex-col fixed left-0 top-0 h-screen w-60 bg-surface border-r border-border shadow-toss z-40">
      {/* 로고 영역 */}
      <div className="flex items-center gap-2.5 px-5 py-5 border-b border-border/50">
        <Logo size={36} />
        <span className="text-lg font-extrabold tracking-tight text-text-primary">리허설</span>
      </div>

      {/* 네비게이션 */}
      <nav className="flex-1 px-3 py-4 flex flex-col gap-1">
        <button
          onClick={() => navigate('/')}
          className={`flex items-center gap-3 w-full px-3 py-2.5 rounded-xl text-sm font-medium transition-colors cursor-pointer ${
            isDashboardActive
              ? 'bg-accent-light text-accent font-bold'
              : 'text-text-secondary hover:bg-border/40 hover:text-text-primary'
          }`}
        >
          <LayoutDashboard size={18} />
          대시보드
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
          <button
            onClick={onLogout}
            className="text-xs text-text-tertiary hover:text-text-secondary transition-colors cursor-pointer"
          >
            로그아웃
          </button>
        </div>
      </div>
    </aside>
  )
}
