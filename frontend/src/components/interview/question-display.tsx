import type { Question } from '../../types/interview'

interface QuestionDisplayProps {
  question: Question
  currentIndex: number
  totalCount: number
}

const QuestionDisplay = ({ question, currentIndex, totalCount }: QuestionDisplayProps) => {
  return (
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
  )
}

export default QuestionDisplay
