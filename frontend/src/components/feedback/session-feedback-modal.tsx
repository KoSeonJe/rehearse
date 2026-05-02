import { Sparkles } from 'lucide-react'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Badge } from '@/components/ui/badge'
import type {
  SessionFeedbackData,
  SessionFeedbackGap,
  SessionFeedbackStrength,
  SessionFeedbackWeekPlan,
} from '@/types/session-feedback'

interface SessionFeedbackModalProps {
  isOpen: boolean
  onClose: () => void
  data: SessionFeedbackData | undefined
  isLoading: boolean
  isError: boolean
}

// ---------------------------------------------------------------------------
// Section header — brand teal overline, 섹션 identity 부여
// ---------------------------------------------------------------------------
const SectionHeader = ({ label }: { label: string }) => (
  <p className="text-[11px] font-semibold tracking-[0.10em] uppercase text-brand mb-4">
    {label}
  </p>
)

// ---------------------------------------------------------------------------
// Dimension score row — 1~5점 dot indicator + Fraunces 숫자
// ---------------------------------------------------------------------------
const DimensionScoreRow = ({ name, score }: { name: string; score: number }) => {
  const clamped = Math.max(1, Math.min(5, Math.round(score)))
  return (
    <div className="flex items-center justify-between gap-4 py-1.5 border-b border-border last:border-b-0 last:pb-0">
      <span className="text-[13px] text-foreground/80 leading-none">{name}</span>
      <div className="flex items-center gap-2.5">
        <div className="flex items-center gap-1" aria-label={`${score}점 / 5점`}>
          {Array.from({ length: 5 }).map((_, i) => (
            <span
              key={i}
              className={
                i < clamped
                  ? 'h-1.5 w-1.5 rounded-full bg-brand'
                  : 'h-1.5 w-1.5 rounded-full bg-border'
              }
            />
          ))}
        </div>
        <span className="font-serif text-[15px] font-bold text-foreground/40 tabular-nums min-w-[1rem] text-right">
          {clamped}
        </span>
      </div>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Strength item
// ---------------------------------------------------------------------------
const StrengthItem = ({ item }: { item: SessionFeedbackStrength }) => (
  <div className="border-b border-border pb-4 last:border-b-0 last:pb-0">
    <p className="text-[12px] font-semibold tracking-[0.04em] uppercase text-brand mb-1.5">
      {item.dimension}
    </p>
    <p className="text-[14px] text-foreground/80 leading-[1.65]">{item.observation}</p>
    {item.whyMatters && (
      <p className="mt-1.5 text-[13px] text-muted-foreground leading-[1.60]">{item.whyMatters}</p>
    )}
  </div>
)

// ---------------------------------------------------------------------------
// Gap item
// ---------------------------------------------------------------------------
const GapItem = ({ item }: { item: SessionFeedbackGap }) => (
  <div className="border-b border-border pb-4 last:border-b-0 last:pb-0">
    <div className="flex items-center gap-2 mb-1.5">
      <p className="text-[12px] font-semibold tracking-[0.04em] uppercase text-brand">
        {item.dimension}
      </p>
      {item.levelGap && (
        <Badge
          variant="outline"
          className="bg-muted text-foreground font-mono text-[11px] border-0 px-1.5 py-0"
        >
          {item.levelGap}
        </Badge>
      )}
    </div>
    <p className="text-[14px] text-foreground/80 leading-[1.65]">{item.observation}</p>
    {item.concreteAction && (
      <p className="mt-2 text-[13px] text-brand leading-[1.60] font-medium">
        → {item.concreteAction}
      </p>
    )}
  </div>
)

// ---------------------------------------------------------------------------
// Week plan item
// ---------------------------------------------------------------------------
const WeekPlanItem = ({ item }: { item: SessionFeedbackWeekPlan }) => (
  <div className="border-b border-border pb-4 last:border-b-0 last:pb-0">
    <div className="flex items-start gap-3">
      <span className="font-serif text-[22px] font-bold text-foreground/30 mt-0.5 tabular-nums min-w-[1.5rem] leading-none">
        {item.priority}
      </span>
      <div className="flex-1 min-w-0">
        <p className="text-[13px] font-semibold text-foreground mb-1.5">{item.topic}</p>
        {item.resources.length > 0 && (
          <ul className="space-y-0.5 mb-2">
            {item.resources.map((resource, idx) => (
              <li key={idx} className="text-[13px] text-muted-foreground leading-[1.60]">
                · {resource}
              </li>
            ))}
          </ul>
        )}
        {item.practice && (
          <p className="text-[13px] text-brand leading-[1.60] font-medium">
            → {item.practice}
          </p>
        )}
      </div>
    </div>
  </div>
)

// ---------------------------------------------------------------------------
// Modal body states
// ---------------------------------------------------------------------------
const LoadingBody = () => (
  <div className="flex flex-col items-center justify-center py-16 gap-4">
    <span
      className="h-6 w-6 rounded-full border-2 border-foreground/20 border-t-foreground animate-spin"
      aria-hidden="true"
    />
    <p className="text-[14px] text-muted-foreground">코치 노트 불러오는 중...</p>
  </div>
)

const ErrorBody = () => (
  <div className="py-12 text-center space-y-2">
    <p className="text-[14px] font-semibold text-foreground">피드백을 불러올 수 없습니다</p>
    <p className="text-[13px] text-muted-foreground">잠시 후 다시 시도해주세요.</p>
  </div>
)

const PreliminaryBody = () => (
  <div className="flex flex-col items-center justify-center py-16 gap-4">
    <div className="h-1 w-32 bg-brand/15 rounded-full overflow-hidden">
      <div className="h-full bg-brand animate-progress-loading" />
    </div>
    <p className="text-[14px] text-muted-foreground">코치 노트를 작성 중입니다...</p>
    <p className="text-[12px] text-muted-foreground/60">분석이 완료되면 자동으로 표시됩니다.</p>
  </div>
)

// ---------------------------------------------------------------------------
// Complete body — renders all sections
// ---------------------------------------------------------------------------
const CompleteBody = ({ data }: { data: SessionFeedbackData }) => {
  const hasDelivery =
    data.delivery &&
    (data.delivery.fillerWords || data.delivery.tonePattern || data.delivery.action)

  const dimensionEntries =
    data.overall?.dimensionScores
      ? Object.entries(data.overall.dimensionScores)
      : []

  return (
    <div className="space-y-8">
      {/* 총평 */}
      {data.overall && (
        <section>
          <SectionHeader label="총평" />
          <div className="space-y-3">
            {data.overall.levelAssessment && (
              <Badge className="bg-brand-bg text-brand border-0 font-semibold text-[12px] px-2.5 py-0.5 rounded-full">
                {data.overall.levelAssessment}
              </Badge>
            )}
            {data.overall.narrative && (
              <p className="text-[15px] text-foreground leading-[1.65]">
                {data.overall.narrative}
              </p>
            )}
            {data.overall.coverage && (
              <p className="text-[12px] text-muted-foreground leading-[1.60]">
                {data.overall.coverage}
              </p>
            )}
          </div>
        </section>
      )}

      {/* Dimension Scores */}
      {dimensionEntries.length > 0 && (
        <>
          <hr className="border-border" />
          <section>
            <SectionHeader label="Dimension Scores" />
            <div>
              {dimensionEntries.map(([name, score]) => (
                <DimensionScoreRow key={name} name={name} score={score} />
              ))}
            </div>
          </section>
        </>
      )}

      {/* 강점 */}
      {data.strengths && data.strengths.length > 0 && (
        <>
          <hr className="border-border" />
          <section>
            <SectionHeader label="강점" />
            <div className="space-y-4">
              {data.strengths.map((item, idx) => (
                <StrengthItem key={idx} item={item} />
              ))}
            </div>
          </section>
        </>
      )}

      {/* 개선점 */}
      {data.gaps && data.gaps.length > 0 && (
        <>
          <hr className="border-border" />
          <section>
            <SectionHeader label="개선점" />
            <div className="space-y-4">
              {data.gaps.map((item, idx) => (
                <GapItem key={idx} item={item} />
              ))}
            </div>
          </section>
        </>
      )}

      {/* 1주 학습 계획 */}
      {data.weekPlan && data.weekPlan.length > 0 && (
        <>
          <hr className="border-border" />
          <section>
            <SectionHeader label="1주 학습 계획" />
            <div className="space-y-4">
              {data.weekPlan.map((item, idx) => (
                <WeekPlanItem key={idx} item={item} />
              ))}
            </div>
          </section>
        </>
      )}

      {/* 딜리버리 피드백 */}
      {hasDelivery && data.delivery && (
        <>
          <hr className="border-border" />
          <section>
            <SectionHeader label="딜리버리 피드백" />
            <div className="space-y-3">
              {data.delivery.fillerWords && (
                <div>
                  <p className="text-[12px] font-semibold tracking-[0.04em] uppercase text-brand mb-1">
                    습관어
                  </p>
                  <p className="text-[14px] text-foreground/80 leading-[1.65]">
                    {data.delivery.fillerWords}
                  </p>
                </div>
              )}
              {data.delivery.tonePattern && (
                <div>
                  <p className="text-[12px] font-semibold tracking-[0.04em] uppercase text-brand mb-1">
                    말투 패턴
                  </p>
                  <p className="text-[14px] text-foreground/80 leading-[1.65]">
                    {data.delivery.tonePattern}
                  </p>
                </div>
              )}
              {data.delivery.action && (
                <p className="text-[13px] text-brand leading-[1.60] font-medium">
                  → {data.delivery.action}
                </p>
              )}
            </div>
          </section>
        </>
      )}
    </div>
  )
}

// ---------------------------------------------------------------------------
// Modal
// ---------------------------------------------------------------------------
export const SessionFeedbackModal = ({
  isOpen,
  onClose,
  data,
  isLoading,
  isError,
}: SessionFeedbackModalProps) => {
  const renderBody = () => {
    if (isLoading) return <LoadingBody />
    if (isError) return <ErrorBody />
    if (!data) return <ErrorBody />
    if (data.status === 'PRELIMINARY') return <PreliminaryBody />
    return <CompleteBody data={data} />
  }

  return (
    <Dialog open={isOpen} onOpenChange={(open) => { if (!open) onClose() }}>
      <DialogContent className="max-w-xl w-full p-0 gap-0 overflow-hidden">
        <DialogHeader className="px-6 pt-6 pb-4 border-b border-border">
          <DialogTitle className="flex items-center gap-2 text-[16px] font-semibold text-foreground">
            코치 노트
            <Sparkles size={14} className="text-brand" aria-hidden="true" />
          </DialogTitle>
        </DialogHeader>
        <div className="overflow-y-auto max-h-[80vh] px-6 py-6">{renderBody()}</div>
      </DialogContent>
    </Dialog>
  )
}
