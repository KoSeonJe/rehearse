import { Helmet } from 'react-helmet-async'
import { Link } from 'react-router-dom'

export const NotFoundPage = () => {
  return (
    <>
      <Helmet>
        <title>페이지를 찾을 수 없습니다 | 리허설</title>
        <meta name="robots" content="noindex, nofollow" />
      </Helmet>
      <main className="flex min-h-screen flex-col items-center justify-center gap-6 bg-white px-5 text-center text-text-primary">
        <p className="text-sm font-semibold text-text-tertiary">404</p>
        <h1 className="text-2xl font-bold md:text-3xl">페이지를 찾을 수 없어요</h1>
        <p className="max-w-md text-sm text-text-secondary md:text-base">
          요청하신 주소가 변경되었거나 존재하지 않습니다.
        </p>
        <Link
          to="/"
          className="rounded-xl border border-border px-5 py-2.5 text-sm font-medium text-text-primary transition-colors hover:bg-surface"
        >
          홈으로 돌아가기
        </Link>
      </main>
    </>
  )
}
