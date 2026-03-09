import type { Question } from '@/types/interview'

interface QuestionCardProps {
  question: Question
}

export const QuestionCard = ({ question }: QuestionCardProps) => {
  return (
    <li className="rounded-lg border border-gray-200 bg-white p-5">
      <div className="flex items-center gap-3">
        <span className="inline-flex h-7 w-7 items-center justify-center rounded-full bg-slate-900 text-sm font-medium text-white">
          {question.order}
        </span>
        <span className="rounded bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-600">
          {question.category}
        </span>
      </div>
      <p className="mt-3 text-base leading-relaxed text-gray-900">
        {question.content}
      </p>
    </li>
  )
}
