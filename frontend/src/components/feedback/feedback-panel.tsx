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

      {/* 3. 기술 분석 섹션 */}
      {feedback.technical && (
        <div className="mb-3 rounded-xl bg-surface p-3 space-y-2">
          <div className="flex items-center gap-1.5">
            <span className="text-[10px] font-bold uppercase tracking-widest text-accent">기술 분석</span>
            {feedback.technical.verbalScore !== null && (
              <span className="text-[10px] font-semibold text-text-tertiary">
                언어 점수 {feedback.technical.verbalScore}
              </span>
            )}
          </div>
          {feedback.technical.verbalComment && (
            <p className="text-xs text-text-secondary leading-relaxed">{feedback.technical.verbalComment}</p>
          )}
        </div>
      )}

      {/* 4. 비언어 분석 섹션 */}
      {feedback.nonverbal && (
        <div className="mb-3 rounded-xl bg-surface p-3 space-y-2">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="text-[10px] font-bold uppercase tracking-widest text-blue-500">비언어 분석</span>
            {feedback.nonverbal.eyeContactScore !== null && (
              <span className="text-[10px] font-semibold text-text-tertiary">
                시선 {feedback.nonverbal.eyeContactScore}
              </span>
            )}
            {feedback.nonverbal.postureScore !== null && (
              <span className="text-[10px] font-semibold text-text-tertiary">
                자세 {feedback.nonverbal.postureScore}
              </span>
            )}
            {feedback.nonverbal.expressionLabel && (
              <span className="text-[10px] font-semibold text-text-tertiary">
                표정 {feedback.nonverbal.expressionLabel}
              </span>
            )}
          </div>
          {feedback.nonverbal.nonverbalComment && (
            <p className="text-xs text-text-secondary leading-relaxed">{feedback.nonverbal.nonverbalComment}</p>
          )}
        </div>
      )}

      {/* 4.5 음성 특성 섹션 */}
      {feedback.vocal && (feedback.vocal.toneConfidence !== null || feedback.vocal.speechPace || feedback.vocal.emotionLabel) && (
        <div className="mb-3 rounded-xl bg-surface p-3 space-y-2">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="text-[10px] font-bold uppercase tracking-widest text-purple-500">음성 분석</span>
            {feedback.vocal.toneConfidence !== null && (
              <span className="text-[10px] font-semibold text-text-tertiary">
                자신감 {feedback.vocal.toneConfidence}
              </span>
            )}
            {feedback.vocal.speechPace && (
              <span className="text-[10px] font-semibold text-text-tertiary">
                속도 {feedback.vocal.speechPace}
              </span>
            )}
            {feedback.vocal.emotionLabel && (
              <span className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${
                feedback.vocal.emotionLabel === '자신감' ? 'bg-green-50 text-green-600' :
                feedback.vocal.emotionLabel === '긴장' ? 'bg-orange-50 text-orange-600' :
                feedback.vocal.emotionLabel === '불안' ? 'bg-red-50 text-red-600' :
                'bg-blue-50 text-blue-600'
              }`}>
                {feedback.vocal.emotionLabel}
              </span>
            )}
          </div>
          {feedback.vocal.fillerWords && feedback.vocal.fillerWords.length > 0 && (
            <div className="flex items-center gap-1.5 flex-wrap">
              <span className="text-[10px] font-semibold text-text-tertiary">필러워드:</span>
              {feedback.vocal.fillerWords.map((word, idx) => (
                <span key={idx} className="rounded-md bg-accent/10 px-1.5 py-0.5 text-[10px] font-bold text-accent">
                  {word}
                </span>
              ))}
            </div>
          )}
          {feedback.vocal.vocalComment && (
            <p className="text-xs text-text-secondary leading-relaxed">{feedback.vocal.vocalComment}</p>
          )}
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
              {feedback.technical && feedback.technical.fillerWordCount !== null && feedback.technical.fillerWordCount > 0 && (
                <p className="mt-2 text-[10px] font-semibold text-accent">
                  필러워드 {feedback.technical.fillerWordCount}회 감지
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
