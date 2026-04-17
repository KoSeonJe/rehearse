import type { Question, InterviewType } from '@/types/interview'
import { INTERVIEW_TYPE_LABELS } from '@/constants/interview-labels'
import { Card } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'

interface QuestionCardProps {
  question: Question
}

export const QuestionCard = ({ question }: QuestionCardProps) => {
  return (
    <Card className="border border-border bg-surface p-5 shadow-sm" role="listitem">
      <div className="flex items-center gap-3">
        <span className="inline-flex h-7 w-7 items-center justify-center rounded-full bg-foreground text-sm font-medium text-background">
          {question.order}
        </span>
        {question.category && (
          <Badge
            variant="outline"
            className="rounded-badge bg-background text-text-secondary"
          >
            {INTERVIEW_TYPE_LABELS[question.category as InterviewType]?.label ?? question.category}
          </Badge>
        )}
      </div>
      <p className="mt-3 text-base leading-relaxed text-text-primary">
        {question.content}
      </p>
    </Card>
  )
}
