import { useNavigate } from 'react-router-dom'
import { Character } from '@/components/ui/character'
import { Button } from '@/components/ui/button'

const SUGGESTED_TOPICS = ['CS 기초', '프로젝트 경험', 'Java/Spring', 'React/TS', '시스템 설계']

export const EmptyState = () => {
  const navigate = useNavigate()

  return (
    <div className="py-16 text-center">
      <Character mood="happy" size={120} className="mx-auto mb-6" />
      <h2 className="text-xl font-extrabold text-text-primary">아직 면접 기록이 없어요</h2>
      <p className="mt-2 text-sm text-text-secondary">첫 모의 면접을 시작해보세요!</p>

      <Button
        variant="default"
        size="default"
        onClick={() => navigate('/interview/setup')}
        className="mt-8 h-14 w-full max-w-xs mx-auto block"
      >
        새 면접 시작하기
      </Button>

      {/* 추천 주제 칩 (시각적 표시만) */}
      <div className="mt-10">
        <p className="text-xs font-semibold text-text-tertiary mb-3">이런 주제로 시작해보세요</p>
        <div className="flex flex-wrap justify-center gap-2">
          {SUGGESTED_TOPICS.map((topic) => (
            <span
              key={topic}
              className="rounded-badge bg-muted px-3 py-1.5 text-sm font-semibold text-muted-foreground"
            >
              {topic}
            </span>
          ))}
        </div>
      </div>
    </div>
  )
}
