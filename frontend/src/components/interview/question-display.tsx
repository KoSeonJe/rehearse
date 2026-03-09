import type { Question, FollowUpResponse } from '../../types/interview'

interface QuestionDisplayProps {
  question: Question
  currentIndex: number
  totalCount: number
  followUp?: FollowUpResponse
  isFollowUpLoading?: boolean
}

const FOLLOW_UP_TYPE_LABELS: Record<string, string> = {
  DEEP_DIVE: '심화',
  CLARIFICATION: '명확화',
  CHALLENGE: '반론',
  APPLICATION: '적용',
}

const QuestionDisplay = ({ question, currentIndex, totalCount, followUp, isFollowUpLoading }: QuestionDisplayProps) => {
  return (
    <div className="space-y-3">
      <div className="rounded-2xl border border-slate-200 bg-white p-6">
        <div className="mb-3 flex items-center gap-3">
          <span className="flex h-8 w-8 items-center justify-center rounded-full bg-slate-900 text-sm font-bold text-white">
            {currentIndex + 1}
          </span>
          <span className="text-sm text-slate-500">
            {currentIndex + 1} / {totalCount}
          </span>
          {question.category && (
            <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600">
              {question.category}
            </span>
          )}
        </div>
        <p className="text-lg leading-relaxed font-medium text-slate-900">{question.content}</p>
      </div>

      {isFollowUpLoading && (
        <div className="rounded-2xl border border-blue-100 bg-blue-50 p-4">
          <div className="flex items-center gap-2">
            <div className="h-4 w-4 animate-spin rounded-full border-2 border-blue-500 border-t-transparent" />
            <span className="text-sm text-blue-700">후속 질문을 생성하고 있습니다...</span>
          </div>
        </div>
      )}

      {followUp && !isFollowUpLoading && (
        <div className="rounded-2xl border border-blue-100 bg-blue-50 p-5">
          <div className="mb-2 flex items-center gap-2">
            <span className="rounded-full bg-blue-100 px-2.5 py-0.5 text-xs font-medium text-blue-700">
              후속 질문
            </span>
            <span className="rounded-full bg-blue-100 px-2.5 py-0.5 text-xs font-medium text-blue-700">
              {FOLLOW_UP_TYPE_LABELS[followUp.type] ?? followUp.type}
            </span>
          </div>
          <p className="text-base leading-relaxed font-medium text-slate-900">{followUp.question}</p>
          <p className="mt-2 text-sm text-slate-500">{followUp.reason}</p>
        </div>
      )}
    </div>
  )
}

export default QuestionDisplay
