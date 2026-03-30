import type { DeliveryFeedback } from '@/types/interview'
import LevelBadge from '@/components/feedback/level-badge'
import StructuredComment from '@/components/feedback/structured-comment'

interface DeliveryTabProps {
  delivery: DeliveryFeedback | null
}

const DeliveryTab = ({ delivery }: DeliveryTabProps) => {
  if (delivery === null) {
    return (
      <div className="rounded-xl bg-surface p-4 text-center">
        <p className="text-xs text-text-tertiary">분석 대기 중</p>
      </div>
    )
  }

  const { nonverbal, vocal } = delivery

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

  return (
    <div className="space-y-3">
      {/* 비언어 섹션 */}
      {nonverbal !== null && (
        <div className="rounded-xl bg-surface p-3 space-y-2">
          <span className="text-[10px] font-bold uppercase tracking-widest text-blue-500">
            비언어
          </span>
          <div className="flex items-center gap-2 flex-wrap">
            <LevelBadge label="시선" level={nonverbal.eyeContactLevel} />
            <LevelBadge label="자세" level={nonverbal.postureLevel} />
            {nonverbal.expressionLabel !== null && (
              <div className="flex items-center gap-1.5">
                <span className="text-[10px] font-semibold text-text-tertiary">표정</span>
                <span className="rounded-full bg-blue-50 px-2 py-0.5 text-[10px] font-bold text-blue-600">
                  {nonverbal.expressionLabel}
                </span>
              </div>
            )}
          </div>
          {nonverbal.nonverbalComment !== null && (
            <StructuredComment comment={nonverbal.nonverbalComment} />
          )}
        </div>
      )}

      {/* 음성 섹션 */}
      {vocal !== null && (
        <div className="rounded-xl bg-surface p-3 space-y-2">
          <span className="text-[10px] font-bold uppercase tracking-widest text-purple-500">
            음성
          </span>
          <div className="flex items-center gap-2 flex-wrap">
            {vocal.speechPace !== null && (
              <div className="flex items-center gap-1.5">
                <span className="text-[10px] font-semibold text-text-tertiary">속도</span>
                <span className="rounded-full bg-surface border border-border px-2 py-0.5 text-[10px] font-bold text-text-secondary">
                  {vocal.speechPace}
                </span>
              </div>
            )}
            <LevelBadge label="자신감" level={vocal.toneConfidenceLevel} />
            {vocal.emotionLabel !== null && (
              <div className="flex items-center gap-1.5">
                <span className="text-[10px] font-semibold text-text-tertiary">감정</span>
                <span className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${
                  vocal.emotionLabel === '자신감' ? 'bg-green-50 text-green-600' :
                  vocal.emotionLabel === '긴장' ? 'bg-orange-50 text-orange-600' :
                  vocal.emotionLabel === '불안' ? 'bg-red-50 text-red-600' :
                  'bg-blue-50 text-blue-600'
                }`}>
                  {vocal.emotionLabel}
                </span>
              </div>
            )}
          </div>

          {/* 필러워드 태그 */}
          {(() => {
            const words = parseFillerWords(vocal.fillerWords)
            if (words.length === 0) return null
            return (
              <div className="flex items-center gap-1.5 flex-wrap">
                <span className="text-[10px] font-semibold text-text-tertiary">필러워드</span>
                {words.map((word, idx) => (
                  <span
                    key={idx}
                    className="rounded-md bg-accent/10 px-1.5 py-0.5 text-[10px] font-bold text-accent"
                  >
                    {word}
                  </span>
                ))}
                {vocal.fillerWordCount !== null && vocal.fillerWordCount > 0 && (
                  <span className="text-[10px] font-semibold text-accent">
                    ({vocal.fillerWordCount}회)
                  </span>
                )}
              </div>
            )
          })()}

          {vocal.vocalComment !== null && (
            <StructuredComment comment={vocal.vocalComment} />
          )}
        </div>
      )}

      {nonverbal === null && vocal === null && (
        <div className="rounded-xl bg-surface p-4 text-center">
          <p className="text-xs text-text-tertiary">전달력 분석 정보가 없습니다</p>
        </div>
      )}
    </div>
  )
}

export default DeliveryTab
