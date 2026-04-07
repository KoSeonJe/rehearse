import { Fragment, type ReactNode } from 'react'
import { isCommentBlockEmpty, type ContentFeedback } from '@/types/interview'
import StructuredComment from '@/components/feedback/structured-comment'
import AccuracyIssues from '@/components/feedback/accuracy-issues'
import CoachingCard from '@/components/feedback/coaching-card'

interface ContentTabProps {
  content: ContentFeedback | null
}

const ContentTab = ({ content }: ContentTabProps) => {
  if (content === null) {
    return (
      <div className="px-6 py-10 text-center">
        <p className="text-[15px] text-gray-300">AI가 분석하고 있어요</p>
      </div>
    )
  }

  const hasVerbalComment = !isCommentBlockEmpty(content.verbalComment)
  const hasAccuracyIssues = content.accuracyIssues.length > 0
  const hasCoaching =
    content.coaching !== null &&
    (content.coaching.structure !== null || content.coaching.improvement !== null)

  if (!hasVerbalComment && !hasAccuracyIssues && !hasCoaching) {
    return (
      <div className="px-6 py-10 text-center">
        <p className="text-[15px] text-gray-300">내용 분석 정보가 없습니다</p>
      </div>
    )
  }

  const sections: ReactNode[] = []

  if (hasVerbalComment && content.verbalComment !== null) {
    sections.push(
      <div className="px-6 py-6">
        <p className="text-[15px] font-bold text-gray-900 mb-1">답변 내용을 분석했어요</p>
        <p className="text-[13px] text-gray-400 mb-4">
          기술적으로 맞게 답변했는지, 빠진 내용은 없는지 살펴봤어요.
        </p>
        <StructuredComment
          block={content.verbalComment}
          positiveLabel="잘한 점"
          negativeLabel="아쉬운 점"
          suggestionLabel="이렇게 말하면 더 좋아요"
        />
      </div>,
    )
  }

  if (hasAccuracyIssues) {
    sections.push(
      <div className="px-6 py-6">
        <p className="text-[15px] font-bold text-gray-900 mb-1">틀린 내용이 있었어요</p>
        <p className="text-[13px] text-gray-400 mb-4">
          면접관이 바로 알아챌 수 있는 부분이에요. 꼭 정정해두세요.
        </p>
        <AccuracyIssues issues={content.accuracyIssues} />
      </div>,
    )
  }

  if (hasCoaching && content.coaching !== null) {
    sections.push(
      <div className="px-6 py-6">
        <p className="text-[15px] font-bold text-gray-900 mb-1">다음엔 이렇게 해보세요</p>
        <p className="text-[13px] text-gray-400 mb-4">
          같은 내용이라도 전달 방식을 바꾸면 훨씬 좋은 답변이 됩니다.
        </p>
        <CoachingCard coaching={content.coaching} />
      </div>,
    )
  }

  return (
    <>
      {sections.map((section, idx) => (
        <Fragment key={idx}>
          {idx > 0 && <div className="mx-6 border-t border-gray-100" />}
          {section}
        </Fragment>
      ))}
    </>
  )
}

export default ContentTab
