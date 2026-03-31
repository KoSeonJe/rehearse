import { useRef, useEffect, useState } from 'react'
import type { TimestampFeedback, QuestionWithAnswer } from '@/types/interview'
import ContentTab from '@/components/feedback/content-tab'
import DeliveryTab from '@/components/feedback/delivery-tab'

const ANSWER_TYPE_LABELS: Record<string, string> = {
  MAIN: '원본 답변',
  FOLLOWUP: '후속 질문',
}

const ANSWER_TYPE_BADGE_COLORS: Record<string, string> = {
  MAIN: 'bg-accent/10 text-accent',
  FOLLOWUP: 'bg-blue-50 text-blue-600',
}

const FILLER_WORDS = ['음', '어', '그', '아', '에', '그러니까', '뭐', '이제', '저기']

const highlightFillers = (text: string): React.ReactNode[] => {
  const pattern = new RegExp(`(${FILLER_WORDS.join('|')})`, 'g')
  const parts = text.split(pattern)
  return parts.map((part, i) =>
    FILLER_WORDS.includes(part) ? (
      <span key={i} className="font-extrabold text-accent">
        {part}
      </span>
    ) : (
      <span key={i}>{part}</span>
    ),
  )
}

type FeedbackTab = 'content' | 'delivery'

interface FeedbackCardProps {
  feedback: TimestampFeedback
  isActive: boolean
  question: QuestionWithAnswer | undefined
  onSeek: (ms: number) => void
}

const FeedbackCard = ({ feedback, isActive, question, onSeek }: FeedbackCardProps) => {
  const cardRef = useRef<HTMLDivElement>(null)
  const [showModelAnswer, setShowModelAnswer] = useState(false)
  const [showTranscript, setShowTranscript] = useState(false)
  const [activeTab, setActiveTab] = useState<FeedbackTab>('content')

  useEffect(() => {
    if (isActive && cardRef.current) {
      cardRef.current.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
    }
  }, [isActive])

  const formatTime = (ms: number): string => {
    const s = Math.floor(ms / 1000)
    const m = Math.floor(s / 60)
    return `${m}:${(s % 60).toString().padStart(2, '0')}`
  }

  const isDeliveryAvailable = feedback.delivery !== null

  return (
    <div
      ref={cardRef}
      data-feedback-id={feedback.id}
      className={`rounded-2xl border p-5 transition-all cursor-pointer ${
        isActive
          ? 'border-accent/40 bg-accent/5 shadow-sm'
          : 'border-border bg-white hover:border-border/80'
      }`}
      onClick={() => onSeek(feedback.startMs)}
    >
      {/* 1. 시간 배지 + 유형 배지 */}
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2">
          <span className="text-[10px] font-bold uppercase tracking-widest text-accent">
            {formatTime(feedback.startMs)} — {formatTime(feedback.endMs)}
          </span>
          {feedback.questionType && (
            <span
              className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${
                ANSWER_TYPE_BADGE_COLORS[feedback.questionType] ?? 'bg-surface text-text-tertiary'
              }`}
            >
              {ANSWER_TYPE_LABELS[feedback.questionType] ?? feedback.questionType}
            </span>
          )}
        </div>
        {!feedback.isAnalyzed && (
          <span className="text-[10px] font-medium text-text-tertiary">미분석</span>
        )}
      </div>

      {/* 2. 질문 텍스트 */}
      {question && (
        <p className="text-sm font-semibold text-text-secondary mb-3">
          Q. {question.questionText}
        </p>
      )}

      {/* 3. 탭 영역 */}
      <div className="mb-3" onClick={(e) => e.stopPropagation()}>
        {/* 탭 헤더 */}
        <div className="flex gap-1 mb-3 border-b border-border">
          <button
            onClick={() => setActiveTab('content')}
            className={`pb-2 px-1 text-xs font-semibold transition-colors ${
              activeTab === 'content'
                ? 'text-accent border-b-2 border-accent -mb-px'
                : 'text-text-tertiary hover:text-text-secondary'
            }`}
          >
            답변 내용
          </button>
          <button
            onClick={() => setActiveTab('delivery')}
            disabled={!isDeliveryAvailable}
            className={`pb-2 px-1 text-xs font-semibold transition-colors ${
              activeTab === 'delivery'
                ? 'text-accent border-b-2 border-accent -mb-px'
                : isDeliveryAvailable
                  ? 'text-text-tertiary hover:text-text-secondary'
                  : 'text-text-tertiary/40 cursor-not-allowed'
            }`}
          >
            전달력
          </button>
        </div>

        {/* 탭 콘텐츠 */}
        {activeTab === 'content' && (
          <ContentTab content={feedback.content} />
        )}
        {activeTab === 'delivery' && (
          <DeliveryTab delivery={feedback.delivery} />
        )}
      </div>

      {/* 4. 답변 텍스트 토글 */}
      {feedback.transcript && (
        <div
          className="border-t border-border/50 pt-3"
          onClick={(e) => e.stopPropagation()}
        >
          <button
            onClick={() => setShowTranscript(!showTranscript)}
            className="text-xs font-semibold text-accent hover:underline"
          >
            {showTranscript ? '답변 텍스트 접기' : '답변 텍스트 보기'}
          </button>
          {showTranscript && (
            <div className="mt-2 rounded-xl bg-surface p-4">
              <p className="text-sm leading-relaxed text-text-primary">
                {highlightFillers(feedback.transcript)}
              </p>
              {feedback.delivery?.vocal?.fillerWordCount !== null &&
                feedback.delivery?.vocal?.fillerWordCount !== undefined &&
                feedback.delivery.vocal.fillerWordCount > 0 && (
                  <p className="mt-2 text-[10px] font-semibold text-accent">
                    필러워드 {feedback.delivery.vocal.fillerWordCount}회 감지
                  </p>
                )}
            </div>
          )}
        </div>
      )}

      {/* 5. 모범답변 토글 */}
      {question?.modelAnswer && (
        <div
          className="mt-3 border-t border-border/50 pt-3"
          onClick={(e) => e.stopPropagation()}
        >
          <button
            onClick={() => setShowModelAnswer(!showModelAnswer)}
            className="text-xs font-semibold text-accent hover:underline"
          >
            {showModelAnswer ? '모범답변 접기' : '모범답변 비교'}
          </button>
          {showModelAnswer && (
            <div className="mt-2 rounded-xl bg-success/5 border border-success/10 p-4">
              <p className="text-xs text-text-secondary leading-relaxed">{question.modelAnswer}</p>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

interface FeedbackPanelProps {
  feedbacks: TimestampFeedback[]
  questions: QuestionWithAnswer[]
  activeFeedbackId: number | null
  onSeek: (ms: number) => void
}

export const FeedbackPanel = ({
  feedbacks,
  questions,
  activeFeedbackId,
  onSeek,
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

  return (
    <div className="flex flex-col h-full">
      <div className="space-y-3 overflow-y-auto flex-1">
        {feedbacks.length === 0 ? (
          <div className="rounded-2xl bg-surface p-8 text-center">
            <p className="text-sm font-semibold text-text-tertiary">피드백이 없습니다</p>
          </div>
        ) : (
          feedbacks.map((fb) => (
            <FeedbackCard
              key={fb.id}
              feedback={fb}
              isActive={fb.id === activeFeedbackId}
              question={findQuestion(fb)}
              onSeek={onSeek}
            />
          ))
        )}
      </div>
    </div>
  )
}
