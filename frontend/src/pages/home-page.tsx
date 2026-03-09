import { Link } from 'react-router-dom'
import { Button } from '@/components/ui/button'

export const HomePage = () => {
  return (
    <main className="flex min-h-screen items-center justify-center bg-gray-50">
      <div className="px-4 text-center sm:px-6">
        <h1 className="text-3xl font-bold text-gray-900 sm:text-4xl">
          Rehearse
        </h1>
        <p className="mx-auto mt-3 max-w-md text-base text-gray-600 sm:text-lg">
          AI 기반 개발자 모의면접 플랫폼
        </p>
        <p className="mx-auto mt-2 max-w-md text-base text-gray-500">
          면접 녹화를 AI가 분석하여 타임스탬프 기반 피드백을 제공합니다.
        </p>
        <div className="mt-8">
          <Link to="/interview/setup">
            <Button variant="cta" className="w-full sm:w-auto">
              면접 시작하기
            </Button>
          </Link>
        </div>
      </div>
    </main>
  )
}
