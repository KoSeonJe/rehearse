import type { Question, InterviewType } from '@/types/interview'
import { INTERVIEW_TYPE_LABELS } from '@/constants/interview-labels'

interface QuestionCardProps {
  question: Question
}

export const QuestionCard = ({ question }: QuestionCardProps) => {
  return (
    <li className="rounded-card border border-border bg-surface p-5">
      <div className="flex items-center gap-3">
        <span className="inline-flex h-7 w-7 items-center justify-center rounded-full bg-violet-legacy text-sm font-medium text-white">
          {question.order}
        </span>
        {question.category && (
          <span className="rounded-badge bg-background px-2 py-0.5 text-xs font-medium text-text-secondary">
            {INTERVIEW_TYPE_LABELS[question.category as InterviewType]?.label ?? question.category}
          </span>
        )}
      </div>
      <p className="mt-3 text-base leading-relaxed text-text-primary">
        {question.content}
      </p>
    </li>
  )
}
