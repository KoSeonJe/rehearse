import type { TechnicalDimensionFeedback } from '@/types/interview'

interface RubricDimensionCardProps {
  dimension: TechnicalDimensionFeedback
}

const formatScore = (score: number | null): string => {
  if (score === null) return '평가 제외'
  return `${score}점`
}

const RubricDimensionCard = ({ dimension }: RubricDimensionCardProps) => {
  return (
    <section className="border border-gray-200 bg-white p-4">
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
  )
}

export default RubricDimensionCard
