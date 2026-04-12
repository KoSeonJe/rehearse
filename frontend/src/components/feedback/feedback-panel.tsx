import { useState } from 'react'
import type { TimestampFeedback, QuestionWithAnswer } from '@/types/interview'
import ContentTab from '@/components/feedback/content-tab'
import DeliveryTab from '@/components/feedback/delivery-tab'
import BookmarkToggleButton from '@/components/feedback/bookmark-toggle-button'

const ANSWER_TYPE_LABELS: Record<string, string> = {
  MAIN: '원본 답변',
  FOLLOWUP: '후속 질문',
}

const FILLER_WORDS = ['음', '어', '그', '아', '그러니까', '뭐', '이제', '약간', '좀']

const highlightFillers = (text: string): React.ReactNode[] => {
  const fillerSet = new Set(FILLER_WORDS)
  const tokens = text.split(/(\s+)/)
  return tokens.map((token, i) =>
    fillerSet.has(token) ? (
      <span key={i} className="font-bold text-gray-900 underline decoration-gray-300">
        {token}
      </span>
    ) : (
      <span key={i}>{token}</span>
    ),
  )
}

type FeedbackTab = 'content' | 'delivery'

interface FeedbackCardProps {
  feedback: TimestampFeedback
  question: QuestionWithAnswer | undefined
  onSeek: (ms: number) => void
  interviewId: number
  bookmarkIdsByTsfId: Map<number, number>
}

const FeedbackCard = ({ feedback, question, onSeek, interviewId, bookmarkIdsByTsfId }: FeedbackCardProps) => {
  const [showModelAnswer, setShowModelAnswer] = useState(false)
  const [activeTab, setActiveTab] = useState<FeedbackTab>('content')

  const isDeliveryAvailable =
    feedback.delivery !== null &&
    (feedback.delivery.nonverbal !== null ||
      feedback.delivery.vocal !== null ||
      feedback.delivery.attitudeComment !== null)

  const effectiveTab: FeedbackTab =
    activeTab === 'delivery' && !isDeliveryAvailable ? 'content' : activeTab

  const formatTime = (ms: number): string => {
    const s = Math.floor(ms / 1000)
    const m = Math.floor(s / 60)
    return `${m}:${(s % 60).toString().padStart(2, '0')}`
  }

  const answerTypeLabel = feedback.questionType
    ? (ANSWER_TYPE_LABELS[feedback.questionType] ?? feedback.questionType)
    : null

  return (
    <div
      data-feedback-id={feedback.id}
      className="rounded-2xl bg-white overflow-hidden transition-all cursor-pointer shadow-sm"
      onClick={() => onSeek(feedback.startMs)}
    >
      {/* 헤더 */}
      <div className="px-6 pt-6 pb-5">
        <div className="flex items-center gap-3 mb-4">
          <span className="text-[13px] font-bold text-gray-900 tabular-nums">
            {formatTime(feedback.startMs)} — {formatTime(feedback.endMs)}
          </span>
          {answerTypeLabel !== null && (
            <span className="text-[13px] text-gray-400">{answerTypeLabel}</span>
          )}
          <div className="ml-auto flex items-center gap-2">
            <BookmarkToggleButton
              timestampFeedbackId={feedback.id}
              interviewId={interviewId}
              bookmarkId={bookmarkIdsByTsfId.get(feedback.id)}
            />
            {!feedback.isAnalyzed && (
              <span className="text-[13px] text-gray-300">미분석</span>
            )}
          </div>
        </div>
        {question && (
          <p className="text-[17px] font-bold text-gray-900 leading-snug">
            Q. {question.questionText}
          </p>
        )}
      </div>

      {/* 답변 텍스트 + 모범답변 */}
      {(feedback.transcript !== null || question?.modelAnswer) && (
        <div className="mx-6 mb-4 flex flex-col gap-3" onClick={(e) => e.stopPropagation()}>
          {feedback.transcript !== null && (
            <div className="rounded-xl bg-gray-50 p-5">
              <p className="text-[13px] font-bold text-gray-500 mb-2">내 답변</p>
              <div className="max-h-48 overflow-y-auto">
                <p className="text-[15px] leading-[1.8] text-gray-600">
                  {highlightFillers(feedback.transcript)}
                </p>
              </div>
              {feedback.delivery?.vocal?.fillerWordCount !== null &&
                feedback.delivery?.vocal?.fillerWordCount !== undefined &&
                feedback.delivery.vocal.fillerWordCount > 0 && (
                  <p className="mt-2 text-[13px] text-gray-400">
                    습관어 {feedback.delivery.vocal.fillerWordCount}회 감지
                  </p>
                )}
            </div>
          )}
          {question?.modelAnswer && (
            <div className="rounded-xl bg-blue-50 overflow-hidden">
              <button
                onClick={() => setShowModelAnswer(!showModelAnswer)}
                className="w-full px-5 py-3 flex items-center justify-between"
              >
                <span className="text-[13px] font-bold text-blue-500">모범 답변</span>
                <span className="text-[13px] text-blue-400">
                  {showModelAnswer ? '접기' : '펼치기'}
                </span>
              </button>
              {showModelAnswer && (
                <div className="px-5 pb-5">
                  <div className="max-h-48 overflow-y-auto">
                    <p className="text-[15px] leading-[1.8] text-blue-700/70">
                      {question.modelAnswer}
                    </p>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* 탭 */}
      <div
        className="px-6 flex gap-4 border-b border-gray-100"
        onClick={(e) => e.stopPropagation()}
      >
        <button
          onClick={() => setActiveTab('content')}
          className={`pb-3 border-b-2 text-[14px] transition-colors ${
            effectiveTab === 'content'
              ? 'font-bold text-gray-900 border-gray-900'
              : 'font-medium text-gray-400 border-transparent hover:text-gray-600'
          }`}
        >
          내 답변은 어땠을까
        </button>
        <button
          onClick={() => setActiveTab('delivery')}
          disabled={!isDeliveryAvailable}
          className={`pb-3 border-b-2 text-[14px] transition-colors ${
            effectiveTab === 'delivery'
              ? 'font-bold text-gray-900 border-gray-900'
              : isDeliveryAvailable
                ? 'font-medium text-gray-400 border-transparent hover:text-gray-600'
                : 'font-medium text-gray-300 border-transparent cursor-not-allowed'
          }`}
        >
          어떤 인상을 줬을까
        </button>
      </div>

      {/* 탭 컨텐츠 */}
      <div onClick={(e) => e.stopPropagation()}>
        {effectiveTab === 'content' && <ContentTab content={feedback.content} />}
        {effectiveTab === 'delivery' && <DeliveryTab delivery={feedback.delivery} />}
      </div>

    </div>
  )
}

interface FeedbackPanelProps {
  feedbacks: TimestampFeedback[]
  questions: QuestionWithAnswer[]
  selectedFeedbackId: number | null
  onSeek: (ms: number) => void
  interviewId: number
  bookmarkIdsByTsfId: Map<number, number>
}

export const FeedbackPanel = ({
  feedbacks,
  questions,
  selectedFeedbackId,
  onSeek,
  interviewId,
  bookmarkIdsByTsfId,
}: FeedbackPanelProps) => {
  const findQuestion = (fb: TimestampFeedback): QuestionWithAnswer | undefined => {
    return questions.find(
      (q) =>
        q.startMs !== null &&
        q.endMs !== null &&
        fb.startMs >= q.startMs &&
        fb.startMs < q.endMs,
    )
  }

  const selectedFeedback =
    selectedFeedbackId !== null ? feedbacks.find((fb) => fb.id === selectedFeedbackId) : undefined

  if (feedbacks.length === 0 || selectedFeedback === undefined) {
    return (
      <div className="flex flex-col h-full">
        <div
          className="rounded-2xl bg-white p-8 text-center shadow-sm"
        >
          <p className="text-[15px] text-gray-300">피드백이 없습니다</p>
        </div>
      </div>
    )
  }

  return (
    <div className="flex flex-col h-full">
      <FeedbackCard
        key={selectedFeedback.id}
        feedback={selectedFeedback}
        question={findQuestion(selectedFeedback)}
        onSeek={onSeek}
        interviewId={interviewId}
        bookmarkIdsByTsfId={bookmarkIdsByTsfId}
      />
    </div>
  )
}
