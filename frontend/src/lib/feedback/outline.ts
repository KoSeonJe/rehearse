import type { OutlineItem } from '@/components/layout/sticky-outline'
import type { QuestionWithAnswer, TimestampFeedback } from '@/types/interview'

export interface PlayableQuestion extends QuestionWithAnswer {
  startMs: number
  endMs: number
}

export const isPlayable = (q: QuestionWithAnswer): q is PlayableQuestion =>
  q.startMs !== null && q.endMs !== null

export function buildOutlineItems(
  questions: QuestionWithAnswer[],
  feedbacks: TimestampFeedback[],
): OutlineItem[] {
  const playable = questions.filter(isPlayable)
  const sorted = [...playable].sort((a, b) => a.startMs - b.startMs)

  const labeled = sorted.reduce<{
    items: Array<{ q: PlayableQuestion; label: string }>
    mainCounter: number
    followupCounter: number
  }>(
    (acc, q) => {
      const isFollowup = q.questionType === 'FOLLOWUP'
      if (!isFollowup) {
        const nextMain = acc.mainCounter + 1
        return {
          items: [...acc.items, { q, label: `Q${nextMain}` }],
          mainCounter: nextMain,
          followupCounter: 0,
        }
      }
      const nextFollowup = acc.followupCounter + 1
      const parentMain = acc.mainCounter || 1
      return {
        items: [...acc.items, { q, label: `Q${parentMain}-${nextFollowup}` }],
        mainCounter: acc.mainCounter,
        followupCounter: nextFollowup,
      }
    },
    { items: [], mainCounter: 0, followupCounter: 0 },
  ).items

  return labeled.map(({ q, label }, idx) => {
    const fb = feedbacks.find((f) => f.startMs >= q.startMs && f.startMs < q.endMs)
    return {
      id: fb ? `feedback-${fb.id}` : `q-${q.questionId}`,
      label,
      index: idx + 1,
      hasIssue: false,
    }
  })
}
