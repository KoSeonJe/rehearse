import { Fragment, type ReactNode } from 'react'
import { isCommentBlockEmpty, type DeliveryFeedback } from '@/types/interview'
import LevelBadge from '@/components/feedback/level-badge'
import {
  formatExpressionLabel,
  formatFeedbackLevel,
} from '@/components/feedback/format-feedback-level'
import StructuredComment from '@/components/feedback/structured-comment'

interface DeliveryTabProps {
  delivery: DeliveryFeedback | null
}

const parseFillerWords = (raw: string | null): string[] => {
  if (raw === null) return []
  try {
    const parsed: unknown = JSON.parse(raw)
    if (Array.isArray(parsed)) {
      return parsed.filter((w): w is string => typeof w === 'string')
    }
    return []
  } catch {
    return []
  }
}

const DeliveryTab = ({ delivery }: DeliveryTabProps) => {
  if (delivery === null) {
    return (
      <div className="px-6 py-10 text-center">
        <p className="text-[15px] text-gray-300">AI가 분석하고 있어요</p>
      </div>
    )
  }

  const { nonverbal, vocal, attitudeComment } = delivery
  const hasAttitude = !isCommentBlockEmpty(attitudeComment)
  const hasNonverbal = nonverbal !== null
  const hasVocal = vocal !== null

  if (!hasAttitude && !hasNonverbal && !hasVocal) {
    return (
      <div className="px-6 py-10 text-center">
        <p className="text-[15px] text-gray-300">자세·말투 분석 정보가 없습니다</p>
      </div>
    )
  }

  const fillerWords = vocal !== null ? parseFillerWords(vocal.fillerWords) : []
  const fillerCount = vocal?.fillerWordCount ?? 0

  const sections: ReactNode[] = []

  if (hasAttitude && attitudeComment !== null) {
    sections.push(
      <div className="px-6 py-6">
        <p className="text-[15px] font-bold text-gray-900 mb-1">면접관에게 이런 인상을 줬어요</p>
        <p className="text-[13px] text-gray-400 mb-4">
          말투와 어휘 선택에서 느껴지는 전반적인 태도를 분석했어요.
        </p>
        <StructuredComment
          block={attitudeComment}
          positiveLabel="좋은 인상"
          negativeLabel="신경 쓰면 좋을 부분"
          suggestionLabel="이렇게 바꿔보세요"
        />
      </div>,
    )
  }

  if (hasNonverbal && nonverbal !== null) {
    sections.push(
      <div className="px-6 py-6">
        <p className="text-[15px] font-bold text-gray-900 mb-1">표정과 자세를 살펴봤어요</p>
        <p className="text-[13px] text-gray-400 mb-4">
          영상에서 보이는 시선, 자세, 표정을 분석했어요.
        </p>

        <div className="grid grid-cols-3 gap-3 mb-4">
          <LevelBadge
            label="시선"
            value={formatFeedbackLevel(nonverbal.eyeContactLevel)}
            bg="gray"
          />
          <LevelBadge
            label="자세"
            value={formatFeedbackLevel(nonverbal.postureLevel)}
            bg="gray"
          />
          <LevelBadge
            label="표정"
            value={formatExpressionLabel(nonverbal.expressionLabel)}
            bg="gray"
          />
        </div>

        <StructuredComment
          block={nonverbal.nonverbalComment}
          positiveLabel="잘한 점"
          negativeLabel="아쉬운 점"
          suggestionLabel="이렇게 해보세요"
        />
      </div>,
    )
  }

  if (hasVocal && vocal !== null) {
    sections.push(
      <div className="px-6 py-6">
        <p className="text-[15px] font-bold text-gray-900 mb-1">목소리를 분석했어요</p>
        <p className="text-[13px] text-gray-400 mb-4">
          말하는 속도, 자신감, 불필요한 습관어를 체크했어요.
        </p>

        <div className="grid grid-cols-3 gap-3 mb-4">
          <LevelBadge label="속도" value={vocal.speechPace} bg="gray" />
          <LevelBadge
            label="자신감"
            value={formatFeedbackLevel(vocal.toneConfidenceLevel)}
            bg="gray"
          />
          <LevelBadge label="감정" value={vocal.emotionLabel} bg="gray" />
        </div>

        {fillerCount > 0 && fillerWords.length > 0 && (
          <div className="rounded-xl bg-gray-50 p-4 mb-4">
            <p className="text-[13px] font-bold text-gray-900 mb-1">
              습관어가 {fillerCount}회 감지됐어요
            </p>
            <p className="text-[13px] text-gray-400 mb-3">
              자신도 모르게 반복하는 말이에요. 줄이면 훨씬 매끄럽게 들려요.
            </p>
            <div className="flex flex-wrap gap-2">
              {fillerWords.map((word, idx) => (
                <span
                  key={idx}
                  className="rounded-lg bg-card border border-border px-3 py-1.5 text-[13px] font-bold text-text-primary"
                >
                  {word}
                </span>
              ))}
            </div>
          </div>
        )}

        <StructuredComment
          block={vocal.vocalComment}
          positiveLabel="잘한 점"
          negativeLabel="아쉬운 점"
          suggestionLabel="이렇게 해보세요"
        />
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

export default DeliveryTab
