import type { ContentFeedback } from '@/types/interview'
import StructuredComment from '@/components/feedback/structured-comment'
import AccuracyIssues from '@/components/feedback/accuracy-issues'
import CoachingCard from '@/components/feedback/coaching-card'

interface ContentTabProps {
  content: ContentFeedback | null
}

const ContentTab = ({ content }: ContentTabProps) => {
  if (content === null) {
    return (
      <div className="rounded-xl bg-surface p-4 text-center">
        <p className="text-xs text-text-tertiary">분석 대기 중</p>
      </div>
    )
  }

  const hasAccuracyIssues = content.accuracyIssues.length > 0
  const hasCoaching = content.coaching !== null

  return (
    <div className="space-y-3">
      {content.verbalComment !== null && (
        <div className="rounded-xl bg-surface p-3">
          <StructuredComment comment={content.verbalComment} />
        </div>
      )}

      {hasAccuracyIssues && (
        <div className="rounded-xl bg-surface p-3">
          <AccuracyIssues issues={content.accuracyIssues} />
        </div>
      )}

      {hasCoaching && content.coaching !== null && (
        <div className="rounded-xl bg-surface p-3">
          <CoachingCard coaching={content.coaching} />
        </div>
      )}

      {content.verbalComment === null && !hasAccuracyIssues && !hasCoaching && (
        <div className="rounded-xl bg-surface p-4 text-center">
          <p className="text-xs text-text-tertiary">내용 분석 정보가 없습니다</p>
        </div>
      )}
    </div>
  )
}

export default ContentTab
