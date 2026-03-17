import { useRef, useEffect, useState } from 'react'
import type { TimestampFeedback, QuestionWithAnswer } from '@/types/interview'

const ANSWER_TYPE_ORDER = ['MAIN', 'FOLLOWUP_1', 'FOLLOWUP_2', 'FOLLOWUP_3']
const ANSWER_TYPE_LABELS: Record<string, string> = {
  MAIN: '원본 답변',
  FOLLOWUP_1: '후속 질문 1',
  FOLLOWUP_2: '후속 질문 2',
  FOLLOWUP_3: '후속 질문 3',
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

const ScoreBadge = ({ label, score }: { label: string; score: number | null }) => {
  if (score === null) return null
  const color = score >= 80 ? 'text-success' : score >= 50 ? 'text-yellow-600' : 'text-error'
  return (
    <div className="flex items-center gap-1.5">
      <span className="text-xs text-text-tertiary">{label}</span>
      <span className={`text-xs font-black ${color}`}>{score}</span>
    </div>
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
      {/* Time badge */}
      <div className="flex items-center justify-between mb-3">
        <span className="text-[10px] font-black uppercase tracking-widest text-accent">
          {formatTime(feedback.startMs)} — {formatTime(feedback.endMs)}
        </span>
        {!feedback.isAnalyzed && (
          <span className="text-[10px] font-bold text-text-tertiary">미분석</span>
        )}
      </div>

      {/* Question text */}
      {question && (
        <p className="text-xs font-bold text-text-secondary mb-2">Q. {question.questionText}</p>
      )}

      {/* Transcript */}
      {feedback.transcript && (
        <div className="mb-4 rounded-xl bg-surface p-4">
          <p className="text-xs font-bold text-text-tertiary mb-1">답변 텍스트</p>
          <p className="text-sm leading-relaxed text-text-primary">
            {highlightFillers(feedback.transcript)}
          </p>
          {feedback.fillerWordCount !== null && feedback.fillerWordCount > 0 && (
            <p className="mt-2 text-[10px] font-bold text-accent">
              필러워드 {feedback.fillerWordCount}회 감지
            </p>
          )}
        </div>
      )}

      {/* Scores */}
      {feedback.isAnalyzed && (
        <div className="space-y-3">
          {/* Verbal */}
          {feedback.verbalScore !== null && (
            <div>
              <div className="flex items-center gap-2 mb-1">
                <span className="text-xs font-black text-text-primary">언어 분석</span>
                <ScoreBadge label="" score={feedback.verbalScore} />
              </div>
              {feedback.verbalComment && (
                <p className="text-xs text-text-secondary leading-relaxed">{feedback.verbalComment}</p>
              )}
            </div>
          )}

          {/* Nonverbal */}
          {(feedback.eyeContactScore !== null || feedback.postureScore !== null || feedback.expressionLabel !== null) && (
            <div>
              <p className="text-xs font-black text-text-primary mb-1">비언어 분석</p>
              <div className="flex flex-wrap gap-3 mb-1">
                <ScoreBadge label="시선" score={feedback.eyeContactScore} />
                <ScoreBadge label="자세" score={feedback.postureScore} />
                {feedback.expressionLabel && (
                  <div className="flex items-center gap-1.5">
                    <span className="text-xs text-text-tertiary">표정</span>
                    <span className="text-xs font-bold text-text-primary">{feedback.expressionLabel}</span>
                  </div>
                )}
              </div>
              {feedback.nonverbalComment && (
                <p className="text-xs text-text-secondary leading-relaxed">{feedback.nonverbalComment}</p>
              )}
            </div>
          )}

          {/* Overall */}
          {feedback.overallComment && (
            <div className="border-t border-border/50 pt-3">
              <p className="text-xs font-black text-text-primary mb-1">종합 코멘트</p>
              <p className="text-sm font-medium text-text-primary leading-relaxed">{feedback.overallComment}</p>
            </div>
          )}
        </div>
      )}

      {/* Model answer toggle */}
      {question?.modelAnswer && (
        <div className="mt-3 border-t border-border/50 pt-3">
          <button
            onClick={(e) => {
              e.stopPropagation()
              setShowModelAnswer(!showModelAnswer)
            }}
            className="text-xs font-bold text-accent hover:underline"
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
  const availableTypes = ANSWER_TYPE_ORDER.filter((type) =>
    feedbacks.some((f) => f.answerType === type),
  )
  const [activeTab, setActiveTab] = useState(availableTypes[0] ?? 'MAIN')

  const filtered = feedbacks.filter((f) => f.answerType === activeTab)

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
      {/* Tabs */}
      {availableTypes.length > 1 && (
        <div className="flex gap-1 bg-surface rounded-xl p-1 mb-4 shrink-0">
          {availableTypes.map((type) => (
            <button
              key={type}
              onClick={() => setActiveTab(type)}
              className={`flex-1 py-2 rounded-lg text-xs font-bold transition-all ${
                activeTab === type
                  ? 'bg-white shadow-toss text-text-primary'
                  : 'text-text-tertiary'
              }`}
            >
              {ANSWER_TYPE_LABELS[type] ?? type}
            </button>
          ))}
        </div>
      )}

      {/* Cards */}
      <div className="space-y-3 overflow-y-auto flex-1">
        {filtered.length === 0 ? (
          <div className="rounded-2xl bg-surface p-8 text-center">
            <p className="text-sm font-bold text-text-tertiary">피드백이 없습니다</p>
          </div>
        ) : (
          filtered.map((fb) => (
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
