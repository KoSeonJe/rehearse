import { useSearchParams, Link } from 'react-router-dom'
import { Logo } from '@/components/ui/logo'

const GitHubIcon = () => (
  <svg viewBox="0 0 24 24" fill="currentColor" className="h-5 w-5" aria-hidden="true">
    <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0 0 24 12c0-6.63-5.37-12-12-12z" />
  </svg>
)

const GoogleIcon = () => (
  <svg viewBox="0 0 24 24" className="h-5 w-5" aria-hidden="true">
    <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
    <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
    <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
    <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
  </svg>
)

export const LoginPage = () => {
  const [searchParams] = useSearchParams()
  const apiUrl = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

  const getOAuthUrl = (provider: 'github' | 'google') => {
    const redirect = searchParams.get('redirect')
    const base = `${apiUrl}/oauth2/authorization/${provider}`
    return redirect ? `${base}?redirect=${encodeURIComponent(redirect)}` : base
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-surface px-4 py-12">
      <div className="w-full max-w-md">

        {/* 카드 */}
        <div className="rounded-[32px] bg-white px-10 py-12 shadow-toss-lg">

          {/* 로고 */}
          <div className="mb-8 flex items-center gap-2">
            <Logo size={80} />
            <span className="text-xl font-extrabold tracking-tight text-text-primary">
              리허설
            </span>
          </div>

          {/* 제목 */}
          <div className="mb-8">
            <h1 className="text-2xl font-extrabold tracking-tight text-text-primary">
              리허설 시작하기
            </h1>
            <p className="mt-2 text-base text-text-secondary">
              면접 준비를 위해 로그인이 필요합니다
            </p>
          </div>

          {/* OAuth 버튼 */}
          <div className="flex flex-col gap-3">
            {/* GitHub */}
            <a
              href={getOAuthUrl('github')}
              className="flex w-full items-center justify-center gap-3 rounded-2xl bg-[#24292e] px-6 py-4 text-base font-semibold text-white transition-all hover:bg-[#1c2128] active:scale-[0.98] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#24292e] focus-visible:ring-offset-2"
              aria-label="GitHub 계정으로 로그인"
            >
              <GitHubIcon />
              GitHub로 계속하기
            </a>

            {/* Google */}
            <a
              href={getOAuthUrl('google')}
              className="flex w-full items-center justify-center gap-3 rounded-2xl border border-border bg-white px-6 py-4 text-base font-semibold text-text-primary transition-all hover:bg-surface active:scale-[0.98] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-border focus-visible:ring-offset-2"
              aria-label="Google 계정으로 로그인"
            >
              <GoogleIcon />
              Google로 계속하기
            </a>
          </div>

          {/* 안내 */}
          <p className="mt-6 text-center text-sm text-text-tertiary">
            처음 방문이라면 자동으로 계정이 만들어져요.
          </p>
        </div>

        {/* 홈으로 */}
        <div className="mt-6 text-center">
          <Link
            to="/"
            className="text-sm text-text-secondary transition-colors hover:text-text-primary"
          >
            ← 리허설 홈으로 돌아가기
          </Link>
        </div>
      </div>
    </div>
  )
}
