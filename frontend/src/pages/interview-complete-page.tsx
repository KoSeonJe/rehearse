import { useNavigate, useParams } from 'react-router-dom'
import { Character } from '@/components/ui/character'

export const InterviewCompletePage = () => {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()

  const handleViewReport = () => {
    navigate(`/interview/${id}/report`)
  }

  const handleGoHome = () => {
    navigate('/')
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-white px-4 text-text-primary sm:px-6">
      <div className="w-full max-w-md space-y-8 text-center">
        <Character mood="happy" size={200} className="mx-auto" />
        <div className="space-y-2">
          <h1 className="text-3xl font-extrabold tracking-tighter text-text-primary">면접이 완료되었습니다</h1>
          <p className="text-base font-medium text-text-secondary">
            수고하셨습니다! 종합 리포트에서 결과를 확인하세요.
          </p>
        </div>
        <div className="space-y-3">
          <button
            onClick={handleViewReport}
            className="h-16 w-full rounded-[24px] bg-accent font-black text-lg text-white transition-all active:scale-95"
          >
            종합 리포트 보기
          </button>
          <button
            onClick={handleGoHome}
            className="h-16 w-full rounded-[24px] border border-border bg-surface font-bold text-text-primary transition-all active:scale-95"
          >
            홈으로 돌아가기
          </button>
        </div>
      </div>
    </div>
  )
}
