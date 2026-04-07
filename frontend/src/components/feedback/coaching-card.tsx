import type { CoachingResponse } from '@/types/interview'

interface CoachingCardProps {
  coaching: CoachingResponse
}

const CoachingCard = ({ coaching }: CoachingCardProps) => {
  if (coaching.structure === null && coaching.improvement === null) return null

  return (
    <div className="space-y-4">
      {coaching.structure !== null && (
        <div className="rounded-xl bg-gray-50 p-4">
          <p className="text-[13px] font-bold text-gray-500 mb-1.5">답변 구조</p>
          <p className="text-[15px] leading-[1.7] text-gray-700">{coaching.structure}</p>
        </div>
      )}
      {coaching.improvement !== null && (
        <div className="rounded-xl bg-gray-50 p-4">
          <p className="text-[13px] font-bold text-gray-500 mb-1.5">설득력 높이기</p>
          <p className="text-[15px] leading-[1.7] text-gray-700">{coaching.improvement}</p>
        </div>
      )}
    </div>
  )
}

export default CoachingCard
