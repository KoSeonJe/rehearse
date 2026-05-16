import type {
  NonverbalFeedback,
  RubricCategory,
  TechnicalFeedback,
} from '@/types/interview'
import RubricDimensionCard from '@/components/feedback/rubric-dimension-card'

interface ContentTabProps {
  technicalFeedback: TechnicalFeedback | null
  nonverbalFeedback: NonverbalFeedback | null
  questionType: string | null
}

interface RubricCategoryCopy {
  title: string
  emptyMessage: string
}

const FALLBACK_COPY: RubricCategoryCopy = {
  title: '기술 피드백',
  emptyMessage: '해당 턴은 평가 대상이 아닙니다.',
}

const OPENER_COPY = {
  emptyMessage: '이 단계는 채점 대상이 아닙니다.',
  secondary: '면접 도입 단계 답변은 점수 채점에 사용되지 않습니다.',
} as const

const RESUME_OPENER_QUESTION_TYPE = 'RESUME_OPENER'
const NONVERBAL_DIMENSION_COUNT = 3

const resolveCopy = (rubricCategory: RubricCategory | null): RubricCategoryCopy => {
  switch (rubricCategory) {
    case 'TECHNICAL':
      return { title: '기술 피드백', emptyMessage: '기술 피드백은 아직 준비 중입니다.' }
    case 'EXPERIENCE':
      return { title: '경험 평가', emptyMessage: '경험 평가는 아직 준비 중입니다.' }
    case 'BEHAVIORAL':
      return { title: '경험/협업', emptyMessage: '경험/협업 피드백은 아직 준비 중입니다.' }
    case null:
      return FALLBACK_COPY
    default:
      return FALLBACK_COPY
  }
}

interface VerbalSectionProps {
  technicalFeedback: TechnicalFeedback | null
}

const VerbalSection = ({ technicalFeedback }: VerbalSectionProps) => {
  const copy = resolveCopy(technicalFeedback?.rubricCategory ?? null)
  const isCardable =
    technicalFeedback !== null &&
    technicalFeedback.dimensions.length > 0 &&
    (technicalFeedback.rubricCategory === 'TECHNICAL' ||
      technicalFeedback.rubricCategory === 'EXPERIENCE' ||
      technicalFeedback.rubricCategory === 'BEHAVIORAL')

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
          <RubricDimensionCard key={dimension.dimension} dimension={dimension} />
        ))}
      </div>
    </div>
  )
}

interface NonverbalSectionProps {
  nonverbalFeedback: NonverbalFeedback | null
}

const NonverbalSection = ({ nonverbalFeedback }: NonverbalSectionProps) => {
  if (nonverbalFeedback === null) {
    return (
      <div className="space-y-2 px-6 py-6">
        <p className="text-[15px] font-bold text-gray-900">비언어 평가</p>
        <p className="text-[13px] leading-relaxed text-gray-400">
          분석 실패 — 점수 없음
        </p>
      </div>
    )
  }

  const isPartial = nonverbalFeedback.dimensions.length < NONVERBAL_DIMENSION_COUNT

  return (
    <div className="space-y-4 px-6 py-6">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="text-[15px] font-bold text-gray-900">비언어 평가</p>
        <span className="font-tabular text-[12px] font-medium text-gray-400">
          {nonverbalFeedback.rubricId}
        </span>
      </div>

      {isPartial && (
        <p className="text-[13px] leading-relaxed text-gray-400">
          이번 답변 비언어 분석 일부 실패
        </p>
      )}

      <div className="space-y-3">
        {nonverbalFeedback.dimensions.map((dimension) => (
          <RubricDimensionCard key={dimension.dimension} dimension={dimension} />
        ))}
      </div>
    </div>
  )
}

const ContentTab = ({
  technicalFeedback,
  nonverbalFeedback,
  questionType,
}: ContentTabProps) => {
  if (questionType === RESUME_OPENER_QUESTION_TYPE) {
    return (
      <div className="px-6 py-10 text-center">
        <p className="text-[15px] font-bold text-gray-900">{OPENER_COPY.emptyMessage}</p>
        <p className="mt-2 text-[13px] leading-relaxed text-gray-400">
          {OPENER_COPY.secondary}
        </p>
      </div>
    )
  }

  return (
    <div className="divide-y divide-gray-100">
      <VerbalSection technicalFeedback={technicalFeedback} />
      <NonverbalSection nonverbalFeedback={nonverbalFeedback} />
    </div>
  )
}

export default ContentTab
