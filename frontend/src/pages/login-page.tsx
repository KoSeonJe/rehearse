import { Link, useSearchParams } from 'react-router-dom'
import { Logo } from '@/components/ui/logo'

const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

export const LoginPage = () => {
  const [searchParams] = useSearchParams()
  const redirect = searchParams.get('redirect') ?? '/interview/setup'

  const handleGithubLogin = () => {
    window.location.href = `${API_URL}/oauth2/authorization/github?redirect=${encodeURIComponent(redirect)}`
  }

  const handleGoogleLogin = () => {
    window.location.href = `${API_URL}/oauth2/authorization/google?redirect=${encodeURIComponent(redirect)}`
  }

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-surface px-4">
      <div className="w-full max-w-md rounded-[32px] bg-white p-10 shadow-toss-lg">
        {/* 로고 */}
        <div className="mb-8 flex flex-col items-center gap-2">
          <Logo size={72} />
          <span className="text-xl font-extrabold tracking-tight text-text-primary">리허설</span>
        </div>

        {/* 제목 */}
        <div className="mb-8 text-center">
          <h1 className="text-2xl font-extrabold tracking-tight text-text-primary">
            리허설 시작하기
          </h1>
          <p className="mt-2 text-sm font-medium text-text-secondary">
            면접 준비를 위해 로그인이 필요합니다
          </p>
        </div>

        {/* OAuth 버튼 */}
        <div className="flex flex-col gap-3">
          <button
            onClick={handleGithubLogin}
            className="flex w-full items-center justify-center gap-3 rounded-2xl px-6 py-4 text-sm font-bold text-white transition-all active:scale-95"
            style={{ backgroundColor: '#24292e' }}
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="white">
              <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0 0 24 12c0-6.63-5.37-12-12-12z" />
            </svg>
            GitHub로 계속하기
          </button>

          <button
            onClick={handleGoogleLogin}
            className="flex w-full items-center justify-center gap-3 rounded-2xl border border-border bg-white px-6 py-4 text-sm font-bold text-text-primary transition-all hover:bg-surface active:scale-95"
          >
            <svg width="20" height="20" viewBox="0 0 24 24">
              <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
              <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
              <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z" />
              <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
            </svg>
            Google로 계속하기
          </button>
        </div>

        {/* 안내 문구 */}
        <p className="mt-6 text-center text-xs font-medium text-text-tertiary">
          처음 방문이라면 자동으로 계정이 만들어져요.
        </p>

        {/* 회원가입 유도 */}
        <div className="mt-3 text-center">
          <p className="text-xs text-text-tertiary">
            계정이 없으신가요?{' '}
            <span
              className="cursor-pointer font-bold text-accent underline underline-offset-2 hover:text-accent-hover"
              onClick={handleGithubLogin}
            >
              GitHub으로 회원가입
            </span>
            {' 또는 '}
            <span
              className="cursor-pointer font-bold text-accent underline underline-offset-2 hover:text-accent-hover"
              onClick={handleGoogleLogin}
            >
              Google로 회원가입
            </span>
          </p>
        </div>

        {/* 홈으로 */}
        <div className="mt-6 text-center">
          <Link
            to="/"
            className="text-xs font-medium text-text-tertiary hover:text-text-secondary"
          >
            ← 리허설 홈으로 돌아가기
          </Link>
        </div>
      </div>
    </div>
  )
}
