import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import { useVerifyAdminPassword } from '@/hooks/use-service-feedback'

const SESSION_KEY = 'admin-authenticated'

export const PasswordProtectedRoute = () => {
  const [password, setPassword] = useState('')
  const [error, setError] = useState(false)
  const [authenticated, setAuthenticated] = useState(
    () => sessionStorage.getItem(SESSION_KEY) === 'true',
  )

  const { mutate: verify, isPending } = useVerifyAdminPassword()

  if (authenticated) return <Outlet />

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!password || isPending) return

    verify(password, {
      onSuccess: () => {
        sessionStorage.setItem(SESSION_KEY, 'true')
        setAuthenticated(true)
      },
      onError: () => {
        setError(true)
      },
    })
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-background">
      <form
        onSubmit={handleSubmit}
        className="bg-surface rounded-card p-6 shadow-toss-lg max-w-sm w-full mx-4"
      >
        <h2 className="text-base font-extrabold text-text-primary">
          관리자 인증
        </h2>
        <p className="mt-1 text-sm text-text-secondary">
          비밀번호를 입력해주세요
        </p>
        <input
          type="password"
          value={password}
          onChange={(e) => {
            setPassword(e.target.value)
            setError(false)
          }}
          placeholder="비밀번호"
          className="mt-4 w-full rounded-button border border-border px-3 py-2 text-sm text-text-primary placeholder:text-text-secondary focus:outline-none focus:ring-2 focus:ring-accent/50 focus:border-accent transition-all"
          autoFocus
        />
        {error && (
          <p className="mt-1 text-xs text-red-500">
            비밀번호가 올바르지 않습니다
          </p>
        )}
        <button
          type="submit"
          disabled={isPending || !password}
          className="mt-4 w-full h-11 rounded-button bg-accent text-white text-sm font-bold hover:opacity-90 active:scale-95 transition-all cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isPending ? '확인 중...' : '확인'}
        </button>
      </form>
    </div>
  )
}
