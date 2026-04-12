import { Sidebar } from '@/components/dashboard/sidebar'
import { useAuth } from '@/hooks/use-auth'
import { useLogout } from '@/hooks/use-logout'

interface AppShellProps {
  children: React.ReactNode
  header?: React.ReactNode
}

export const AppShell = ({ children, header }: AppShellProps) => {
  const { user } = useAuth()
  const logout = useLogout()

  return (
    <div className="min-h-screen bg-background text-text-primary">
      <Sidebar user={user} onLogout={logout} />

      <div className="lg:ml-60">
        {header}
        <main className="px-5 py-8 lg:px-10 lg:py-10">
          <div className="mx-auto max-w-6xl">
            {children}
          </div>
        </main>
      </div>
    </div>
  )
}
