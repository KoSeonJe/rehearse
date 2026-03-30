import type { CoachingResponse } from '@/types/interview'

interface CoachingCardProps {
  coaching: CoachingResponse
}

const CoachingCard = ({ coaching }: CoachingCardProps) => {
  if (coaching.structure === null && coaching.improvement === null) return null

  return (
    <div className="space-y-2">
      <span className="text-[10px] font-bold uppercase tracking-widest text-accent">
        💡 코칭
      </span>
      <div className="rounded-xl bg-accent/5 border border-accent/10 p-3 space-y-2">
        {coaching.structure !== null && (
          <div className="space-y-0.5">
            <p className="text-[10px] font-semibold text-accent uppercase tracking-wide">구조</p>
            <p className="text-xs text-text-secondary leading-relaxed">{coaching.structure}</p>
          </div>
        )}
        {coaching.improvement !== null && (
          <div className="space-y-0.5">
            <p className="text-[10px] font-semibold text-accent uppercase tracking-wide">개선 방향</p>
            <p className="text-xs text-text-secondary leading-relaxed">{coaching.improvement}</p>
          </div>
        )}
      </div>
    </div>
  )
}

export default CoachingCard
