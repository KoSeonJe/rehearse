import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import { useVerifyAdminPassword } from '@/hooks/use-service-feedback'
import { Input } from '@/components/ui/input'
import { Card } from '@/components/ui/card'

const SESSION_KEY = 'admin-password'

export const PasswordProtectedRoute = () => {
  const [password, setPassword] = useState('')
  const [error, setError] = useState(false)
  const [authenticated, setAuthenticated] = useState(
    () => !!sessionStorage.getItem(SESSION_KEY),
  )

  const { mutate: verify, isPending } = useVerifyAdminPassword()

  if (authenticated) return <Outlet />

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!password || isPending) return

    verify(password, {
      onSuccess: () => {
        sessionStorage.setItem(SESSION_KEY, password)
        setAuthenticated(true)
      },
      onError: () => {
        setError(true)
      },
    })
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-background">
      <Card
        className="bg-surface p-6 shadow-md max-w-sm w-full mx-4 border border-border"
        // TODO(design): form을 Card로 감싸는 패턴 — Phase 3f 레이아웃 정리 시 재검토
      >
        <form onSubmit={handleSubmit}>
        <h2 className="text-base font-extrabold text-text-primary">
          관리자 인증
        </h2>
        <p className="mt-1 text-sm text-text-secondary">
          비밀번호를 입력해주세요
        </p>
        <Input
          type="password"
          value={password}
          onChange={(e) => {
            setPassword(e.target.value)
            setError(false)
          }}
          placeholder="비밀번호"
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? 'admin-password-error' : undefined}
          className="mt-4 w-full rounded-button border border-border px-3 py-2 text-sm text-text-primary h-auto placeholder:text-text-secondary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-legacy/50 focus-visible:border-violet-legacy transition-colors"
          autoFocus
        />
        {error && (
          <p id="admin-password-error" className="mt-1 text-xs text-red-500" role="alert">
            비밀번호가 올바르지 않습니다
          </p>
        )}
        <button
          type="submit"
          disabled={isPending || !password}
          className="mt-4 w-full h-11 rounded-button bg-violet-legacy text-white text-sm font-bold hover:opacity-90 active:scale-95 transition-all cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isPending ? '확인 중...' : '확인'}
        </button>
        </form>
      </Card>
    </div>
  )
}
