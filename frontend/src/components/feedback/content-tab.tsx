import type { FeedbackPerspective, TechnicalFeedback } from '@/types/interview'

interface ContentTabProps {
  technicalFeedback: TechnicalFeedback | null
}

interface PerspectiveCopy {
  title: string
  emptyMessage: string
}

const formatScore = (score: number | null): string => {
  if (score === null) return '평가 제외'
  return `${score}점`
}

const resolveCopy = (perspective: FeedbackPerspective | null): PerspectiveCopy => {
  switch (perspective) {
    case 'TECHNICAL':
      return { title: '기술 피드백', emptyMessage: '기술 피드백은 아직 준비 중입니다.' }
    case 'EXPERIENCE':
      return { title: '경험 평가', emptyMessage: '경험 평가는 아직 준비 중입니다.' }
    case 'BEHAVIORAL':
    case null:
      return { title: '기술 피드백', emptyMessage: '해당 턴은 평가 대상이 아닙니다.' }
  }
}

const ContentTab = ({ technicalFeedback }: ContentTabProps) => {
  const copy = resolveCopy(technicalFeedback?.perspective ?? null)
  const isCardable =
    technicalFeedback !== null &&
    technicalFeedback.dimensions.length > 0 &&
    (technicalFeedback.perspective === 'TECHNICAL' ||
      technicalFeedback.perspective === 'EXPERIENCE')

  if (!isCardable) {
    return (
      <div className="px-6 py-10 text-center">
        <p className="text-[15px] font-bold text-gray-900">{copy.emptyMessage}</p>
        <p className="mt-2 text-[13px] leading-relaxed text-gray-400">
          답변 내용 분석이 완료되면 이 구간에 표시됩니다.
        </p>
      </div>
    )
  }

  return (
    <div className="space-y-4 px-6 py-6">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="text-[15px] font-bold text-gray-900">{copy.title}</p>
        <span className="font-tabular text-[12px] font-medium text-gray-400">
          {technicalFeedback.rubricId}
        </span>
      </div>

      <div className="space-y-3">
        {technicalFeedback.dimensions.map((dimension) => (
          <section
            key={dimension.dimension}
            className="border border-gray-200 bg-white p-4"
          >
            <div className="flex items-center justify-between gap-3">
              <span className="font-tabular text-[12px] font-semibold text-gray-500">
                {dimension.dimension}
              </span>
              <span className="font-tabular text-[13px] font-bold text-gray-900">
                {formatScore(dimension.score)}
              </span>
            </div>

            {dimension.observation !== null && (
              <p className="mt-3 text-[14px] leading-relaxed text-gray-800">
                {dimension.observation}
              </p>
            )}

            {dimension.evidenceQuote !== null && (
              <blockquote className="mt-3 border-l-2 border-gray-300 pl-3 text-[13px] leading-relaxed text-gray-500">
                {dimension.evidenceQuote}
              </blockquote>
            )}
          </section>
        ))}
      </div>
    </div>
  )
}

export default ContentTab
