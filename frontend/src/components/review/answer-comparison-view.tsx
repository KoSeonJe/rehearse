import { Sparkles } from 'lucide-react'

interface AnswerComparisonViewProps {
  transcript: string | null
  modelAnswer: string | null
  coachingImprovement: string | null
}

export const AnswerComparisonView = ({
  transcript,
  modelAnswer,
  coachingImprovement,
}: AnswerComparisonViewProps) => {
  return (
    <div className="border-t border-border">
      {/* 좌우 비교 패널 */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-3 px-5 pt-4 pb-4">
        {/* 내 답변 */}
        <div className="rounded-xl bg-muted p-4">
          <p className="text-[12px] font-bold text-text-tertiary mb-2 uppercase tracking-wide">
            내 답변
          </p>
          {transcript !== null && transcript.length > 0 ? (
            <p className="text-[14px] leading-[1.75] text-text-secondary">{transcript}</p>
          ) : (
            <p className="text-[14px] leading-[1.75] text-text-tertiary italic">
              분석 결과 준비 중이에요.
            </p>
          )}
        </div>

        {/* 모범 답변 */}
        <div className="rounded-xl bg-muted p-4">
          <p className="text-[12px] font-bold text-text-tertiary mb-2 uppercase tracking-wide">
            모범 답변
          </p>
          {modelAnswer !== null && modelAnswer.length > 0 ? (
            <p className="text-[14px] leading-[1.75] text-text-secondary">{modelAnswer}</p>
          ) : (
            <p className="text-[14px] leading-[1.75] text-text-tertiary italic">
              모범 답변이 제공되지 않은 질문입니다.
            </p>
          )}
        </div>
      </div>

      {/* AI 코칭 요약 */}
      {coachingImprovement !== null && coachingImprovement.length > 0 && (
        <div className="mx-5 mb-5 rounded-xl border border-border px-4 py-3">
          <div className="flex items-center gap-2 mb-1.5">
            <Sparkles size={13} className="text-primary" aria-hidden="true" />
            <span className="text-[12px] font-bold text-primary">AI 코칭</span>
          </div>
          <p className="text-[13px] text-text-tertiary leading-relaxed">{coachingImprovement}</p>
        </div>
      )}
    </div>
  )
}
