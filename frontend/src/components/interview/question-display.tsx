import type { FollowUpResponse, InterviewType, Question } from '@/types/interview'
import { INTERVIEW_TYPE_LABELS } from '@/constants/interview-labels'

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

export const QuestionDisplay = ({ question, currentIndex, totalCount, followUp, isFollowUpLoading }: QuestionDisplayProps) => {
  return (
    <div className="sticky top-20 z-10 space-y-3">
      <div className="rounded-card border border-border bg-surface p-6 shadow-sm">
        <div className="mb-3 flex items-center gap-3">
          <span className="flex h-8 w-8 items-center justify-center rounded-full bg-violet-legacy text-sm font-bold text-white">
            {currentIndex + 1}
          </span>
          <span className="text-sm text-text-secondary">
            {currentIndex + 1} / {totalCount}
          </span>
          {question.category && (
            <span className="rounded-badge bg-background px-3 py-1 text-xs font-medium text-text-secondary">
              {INTERVIEW_TYPE_LABELS[question.category as InterviewType]?.label ?? question.category}
            </span>
          )}
        </div>
        <p className="text-lg leading-relaxed font-medium text-text-primary">{question.content}</p>
      </div>

      {isFollowUpLoading && (
        <div className="rounded-card border border-info/30 bg-info-light p-4">
          <div className="flex items-center gap-2">
            <div className="h-4 w-4 animate-spin rounded-full border-2 border-info border-t-transparent" />
            <span className="text-sm text-info">질문을 생각하고 있습니다...</span>
          </div>
        </div>
      )}

      {followUp && !isFollowUpLoading && (
        <div className="rounded-card border border-info/30 bg-info-light p-5">
          <div className="mb-2 flex items-center gap-2">
            <span className="rounded-badge bg-info/10 px-2.5 py-0.5 text-xs font-medium text-info">
              후속 질문
            </span>
            <span className="rounded-badge bg-info/10 px-2.5 py-0.5 text-xs font-medium text-info">
              {FOLLOW_UP_TYPE_LABELS[followUp.type] ?? followUp.type}
            </span>
          </div>
          <p className="text-base leading-relaxed font-medium text-text-primary">{followUp.question}</p>
          <p className="mt-2 text-sm text-text-secondary">{followUp.reason}</p>
        </div>
      )}
    </div>
  )
}
