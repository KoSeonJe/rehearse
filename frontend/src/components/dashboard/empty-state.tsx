import { useNavigate } from 'react-router-dom'
import { Character } from '@/components/ui/character'

export const EmptyState = () => {
  const navigate = useNavigate()

  return (
    <div className="py-20 text-center">
      <Character mood="happy" size={120} className="mx-auto mb-6" />
      <h2 className="text-xl font-extrabold text-text-primary">아직 면접 기록이 없어요</h2>
      <p className="mt-2 text-sm text-text-secondary">첫 모의 면접을 시작해보세요!</p>
      <button
        onClick={() => navigate('/interview/setup')}
        className="mt-8 h-14 w-full max-w-xs rounded-button bg-accent font-bold text-white hover:bg-accent-hover active:scale-95 transition-all cursor-pointer mx-auto block"
      >
        새 면접 시작하기
      </button>
    </div>
  )
}
