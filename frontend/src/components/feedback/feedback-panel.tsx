import { useState } from 'react'
import type { TimestampFeedback, QuestionWithAnswer } from '@/types/interview'
import ContentTab from '@/components/feedback/content-tab'
import BookmarkToggleButton from '@/components/feedback/bookmark-toggle-button'

const ANSWER_TYPE_LABELS: Record<string, string> = {
  TECH_MAIN: '원본 답변',
  TECH_FOLLOWUP: '후속 질문',
  BEHAVIORAL_MAIN: '원본 답변',
  BEHAVIORAL_FOLLOWUP: '후속 질문',
  RESUME_OPENER: '도입 질문',
  RESUME_PLAYGROUND: '경험 탐색',
  RESUME_INTERROGATION: '심화 질문',
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

interface FeedbackCardProps {
  feedback: TimestampFeedback
  question: QuestionWithAnswer | undefined
  onSeek: (ms: number) => void
  interviewId: number
  bookmarkIdsByTsfId: Map<number, number>
}

const FeedbackCard = ({ feedback, question, onSeek, interviewId, bookmarkIdsByTsfId }: FeedbackCardProps) => {
  const [showBestAnswer, setShowBestAnswer] = useState(false)

  const fillerWordCount = feedback.fillerWordCount

  const formatTime = (ms: number): string => {
    const s = Math.floor(ms / 1000)
    const m = Math.floor(s / 60)
    return `${m}:${(s % 60).toString().padStart(2, '0')}`
  }

  const answerTypeLabel = feedback.questionType
    ? (ANSWER_TYPE_LABELS[feedback.questionType] ?? feedback.questionType)
    : null

  const handleSeek = () => onSeek(feedback.startMs)
  const handleKeyDown = (e: React.KeyboardEvent<HTMLDivElement>) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault()
      handleSeek()
    }
  }

  return (
    <div
      data-feedback-id={feedback.id}
      role="button"
      tabIndex={0}
      aria-label={`${formatTime(feedback.startMs)} 구간으로 이동`}
      className="rounded-2xl bg-card overflow-hidden transition-colors cursor-pointer shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand"
      onClick={handleSeek}
      onKeyDown={handleKeyDown}
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
      {(feedback.transcript !== null || question?.bestAnswer) && (
        <div className="mx-6 mb-4 flex flex-col gap-3" onClick={(e) => e.stopPropagation()}>
          {feedback.transcript !== null && (
            <div className="rounded-xl bg-gray-50 p-5">
              <p className="text-[13px] font-bold text-gray-500 mb-2">내 답변</p>
              <div className="max-h-48 overflow-y-auto">
                <p className="text-[15px] leading-[1.8] text-gray-600">
                  {highlightFillers(feedback.transcript)}
                </p>
              </div>
              {fillerWordCount !== null && fillerWordCount > 0 && (
                <p className="mt-2 text-[13px] text-gray-400">
                  습관어 {fillerWordCount}회 감지
                </p>
              )}
            </div>
          )}
          {question?.bestAnswer && (
            <div className="rounded-xl bg-blue-50 overflow-hidden">
              <button
                onClick={() => setShowBestAnswer(!showBestAnswer)}
                className="w-full px-5 py-3 flex items-center justify-between"
              >
                <span className="text-[13px] font-bold text-blue-500">모범 답변</span>
                <span className="text-[13px] text-blue-400">
                  {showBestAnswer ? '접기' : '펼치기'}
                </span>
              </button>
              {showBestAnswer && (
                <div className="px-5 pb-5">
                  <div className="max-h-48 overflow-y-auto">
                    <p className="text-[15px] leading-[1.8] text-blue-700/70">
                      {question.bestAnswer}
                    </p>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      <div onClick={(e) => e.stopPropagation()}>
        <ContentTab
          technicalFeedback={feedback.technicalFeedback}
          nonverbalFeedback={feedback.nonverbalFeedback}
          questionType={feedback.questionType}
        />
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
          className="rounded-2xl bg-card p-8 text-center shadow-sm"
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
