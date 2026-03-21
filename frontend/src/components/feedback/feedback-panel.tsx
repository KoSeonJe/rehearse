import { useRef, useEffect, useState } from 'react'
import type { TimestampFeedback, QuestionWithAnswer } from '@/types/interview'

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

  const hasNonverbal = feedback.expressionLabel !== null

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
      {/* 1. Time badge + type badge */}
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2">
          <span className="text-[10px] font-bold uppercase tracking-widest text-accent">
            {formatTime(feedback.startMs)} — {formatTime(feedback.endMs)}
          </span>
          {feedback.questionType && (
            <span className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${ANSWER_TYPE_BADGE_COLORS[feedback.questionType] ?? 'bg-surface text-text-tertiary'}`}>
              {ANSWER_TYPE_LABELS[feedback.questionType] ?? feedback.questionType}
            </span>
          )}
        </div>
        {!feedback.isAnalyzed && (
          <span className="text-[10px] font-medium text-text-tertiary">미분석</span>
        )}
      </div>

      {/* 2. Question text */}
      {question && (
        <p className="text-sm font-semibold text-text-secondary mb-3">Q. {question.questionText}</p>
      )}

      {/* 3. Feedback comments */}
      <div className="space-y-2 mb-3">
        {/* 언어 코멘트 */}
        {feedback.verbalComment && (
          <p className="text-xs text-text-secondary leading-relaxed">{feedback.verbalComment}</p>
        )}

        {/* 비언어 정보 (점수 없이 라벨/코멘트만) */}
        {hasNonverbal && (
          <div className="flex flex-wrap gap-4 pt-1">
            <span className="text-xs font-semibold text-text-primary">비언어</span>
            {feedback.expressionLabel && (
              <div className="flex items-center gap-1.5">
                <span className="text-xs font-medium text-text-tertiary">표정</span>
                <span className="text-xs font-semibold text-text-primary">
                  {feedback.expressionLabel}
                </span>
              </div>
            )}
          </div>
        )}

        {/* 비언어 코멘트 */}
        {feedback.nonverbalComment && (
          <p className="text-xs text-text-secondary leading-relaxed">{feedback.nonverbalComment}</p>
        )}
      </div>

      {/* 4. Overall comment */}
      {feedback.overallComment && (
        <div className="border-t border-border/50 pt-3 mb-3">
          <p className="text-xs font-semibold text-text-primary mb-1">종합 코멘트</p>
          <p className="text-sm font-normal text-text-primary leading-relaxed">
            {feedback.overallComment}
          </p>
        </div>
      )}

      {/* 5. Transcript (collapsed by default) */}
      {feedback.transcript && (
        <div className="border-t border-border/50 pt-3">
          <button
            onClick={(e) => {
              e.stopPropagation()
              setShowTranscript(!showTranscript)
            }}
            className="text-xs font-semibold text-accent hover:underline"
          >
            {showTranscript ? '답변 텍스트 접기' : '답변 텍스트 보기'}
          </button>
          {showTranscript && (
            <div className="mt-2 rounded-xl bg-surface p-4" onClick={(e) => e.stopPropagation()}>
              <p className="text-sm leading-relaxed text-text-primary">
                {highlightFillers(feedback.transcript)}
              </p>
              {feedback.fillerWordCount !== null && feedback.fillerWordCount > 0 && (
                <p className="mt-2 text-[10px] font-semibold text-accent">
                  필러워드 {feedback.fillerWordCount}회 감지
                </p>
              )}
            </div>
          )}
        </div>
      )}

      {/* 6. Model answer (collapsed by default) */}
      {question?.modelAnswer && (
        <div className="mt-3 border-t border-border/50 pt-3">
          <button
            onClick={(e) => {
              e.stopPropagation()
              setShowModelAnswer(!showModelAnswer)
            }}
            className="text-xs font-semibold text-accent hover:underline"
          >
            {showModelAnswer ? '모범답변 접기' : '모범답변 비교'}
          </button>
          {showModelAnswer && (
            <div className="mt-2 rounded-xl bg-success/5 border border-success/10 p-4" onClick={(e) => e.stopPropagation()}>
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
      {/* Cards — 전체 피드백 시간순 표시 */}
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
