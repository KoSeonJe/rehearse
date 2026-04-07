import type { QuestionWithAnswer, TimestampFeedback } from '@/types/interview'

interface QuestionListProps {
  questions: QuestionWithAnswer[]
  feedbacks: TimestampFeedback[]
  activeFeedbackId: number | null
  onSeek: (ms: number) => void
}

interface PlayableQuestion extends QuestionWithAnswer {
  startMs: number
  endMs: number
}

const formatTime = (ms: number): string => {
  const s = Math.floor(ms / 1000)
  const m = Math.floor(s / 60)
  return `${m}:${(s % 60).toString().padStart(2, '0')}`
}

const isPlayable = (q: QuestionWithAnswer): q is PlayableQuestion =>
  q.startMs !== null && q.endMs !== null

export const QuestionList = ({
  questions,
  feedbacks,
  activeFeedbackId,
  onSeek,
}: QuestionListProps) => {
  const playable = questions.filter(isPlayable)
  if (playable.length === 0) return null

  const sorted = [...playable].sort((a, b) => a.startMs - b.startMs)

  const findFeedback = (q: PlayableQuestion): TimestampFeedback | undefined =>
    feedbacks.find((fb) => fb.startMs >= q.startMs && fb.startMs < q.endMs)

  // 메인 질문 번호 매기기 (Q1, Q2…), 후속질문은 가장 가까운 직전 메인의 자식
  let mainCounter = 0
  let followupCounter = 0
  const labeled = sorted.map((q) => {
    const isFollowup = q.questionType === 'FOLLOWUP'
    if (!isFollowup) {
      mainCounter += 1
      followupCounter = 0
      return { q, label: `Q${mainCounter}`, isFollowup: false }
    }
    followupCounter += 1
    return { q, label: `Q${mainCounter || 1}-${followupCounter}`, isFollowup: true }
  })

  return (
    <div className="rounded-2xl bg-white border border-gray-100 p-5">
      <p className="text-[13px] font-bold text-gray-400 mb-3">질문 목록</p>
      <ol className="space-y-2">
        {labeled.map(({ q, label, isFollowup }, idx) => {
          const fb = findFeedback(q)
          const isActive = fb !== undefined && fb.id === activeFeedbackId

          return (
            <li key={`${q.questionId}-${idx}`}>
              <button
                type="button"
                onClick={() => onSeek(q.startMs)}
                className={`w-full text-left flex items-start gap-3 p-3 rounded-xl transition-colors ${
                  isActive ? 'bg-gray-50' : 'hover:bg-gray-50'
                } ${isFollowup ? 'pl-9 relative' : ''}`}
              >
                {isFollowup && (
                  <span
                    className={`absolute left-5 top-3 bottom-3 w-px ${
                      isActive ? 'bg-[#FF6B5B]' : 'bg-gray-200'
                    }`}
                    aria-hidden
                  />
                )}
                <span
                  className={`text-[13px] font-bold mt-0.5 flex-shrink-0 tabular-nums ${
                    isActive ? 'text-[#FF6B5B]' : 'text-gray-400'
                  }`}
                >
                  {label}
                </span>
                <div className="flex-1 min-w-0">
                  <p
                    className={`leading-snug ${
                      isFollowup
                        ? 'text-[13px] text-gray-700'
                        : 'text-[14px] font-bold text-gray-900'
                    }`}
                  >
                    {q.questionText}
                  </p>
                  <p className="text-[12px] text-gray-400 mt-1 tabular-nums">
                    {formatTime(q.startMs)} ~ {formatTime(q.endMs)}
                  </p>
                </div>
                {isActive && (
                  <span className="text-[11px] font-bold text-[#FF6B5B] bg-[#FFF1EE] px-2 py-1 rounded-md flex-shrink-0">
                    선택
                  </span>
                )}
              </button>
            </li>
          )
        })}
      </ol>
    </div>
  )
}

export default QuestionList
