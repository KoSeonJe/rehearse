import { Sparkles } from 'lucide-react'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog'
import { Badge } from '@/components/ui/badge'
import { getDimensionLabel } from '@/lib/feedback/dimension-label'
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

const SectionHeader = ({ label, caption }: { label: string; caption?: string }) => (
  <div className={caption ? 'mb-1' : 'mb-4'}>
    <p className="text-[11px] font-semibold tracking-[0.10em] uppercase text-brand">
      {label}
    </p>
    {caption && (
      <p className="text-[12px] text-muted-foreground mt-0.5 mb-4">{caption}</p>
    )}
  </div>
)

const DimensionScoreRow = ({ name, score }: { name: string; score: number }) => {
  const clamped = Math.max(1, Math.min(5, Math.round(score)))
  return (
    <div className="flex items-center justify-between gap-4 py-1.5 border-b border-border last:border-b-0 last:pb-0">
      <span className="text-[13px] text-foreground/80 leading-none">
        {getDimensionLabel(name)}
      </span>
      <div className="flex items-center gap-2.5">
        <div className="flex items-center gap-1" aria-label={`${clamped}점 / 5점`}>
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
        <div className="flex items-baseline gap-0.5">
          <span className="text-[15px] font-bold text-foreground/40 tabular-nums min-w-[1rem] text-right">
            {clamped}
          </span>
          <span className="text-[11px] text-muted-foreground">/5</span>
        </div>
      </div>
    </div>
  )
}

const StrengthItem = ({ item }: { item: SessionFeedbackStrength }) => (
  <div className="px-4 py-3 bg-brand-bg/40 rounded-md">
    <p className="text-[14px] font-bold text-foreground mb-1.5">{item.dimension}</p>
    <p className="text-[14px] text-foreground/85 leading-[1.65]">{item.observation}</p>
    {item.whyMatters && (
      <p className="mt-1.5 text-[13px] text-muted-foreground leading-[1.60]">
        {item.whyMatters}
      </p>
    )}
  </div>
)

const GapItem = ({ item }: { item: SessionFeedbackGap }) => (
  <div className="px-4 py-3 bg-accent-editorial-bg/60 rounded-md">
    <div className="flex items-center gap-2 mb-1.5">
      <p className="text-[14px] font-bold text-foreground">{item.dimension}</p>
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
      <p className="mt-2 text-[13px] text-accent-editorial leading-[1.60] font-medium">
        → {item.concreteAction}
      </p>
    )}
  </div>
)

const WeekPlanItem = ({ item }: { item: SessionFeedbackWeekPlan }) => (
  <div className="border-b border-border pb-4 last:border-b-0 last:pb-0">
    <div className="flex items-start gap-3">
      <span className="h-7 w-7 rounded-full bg-brand-bg text-brand text-[14px] font-bold flex items-center justify-center flex-shrink-0 mt-0.5">
        {item.priority}
      </span>
      <div className="flex-1 min-w-0">
        <p className="text-[14px] font-bold text-foreground mb-1.5">{item.topic}</p>
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
      {/* 이번 면접 한줄 평가 */}
      {data.overall && (
        <section>
          <SectionHeader label="이번 면접 한줄 평가" />
          <div className="space-y-3">
            {data.overall.levelAssessment && (
              <Badge className="bg-brand-bg text-brand border-0 font-semibold text-[12px] px-2.5 py-0.5 rounded-full">
                {data.overall.levelAssessment}
              </Badge>
            )}
            {data.overall.narrative && (
              <p className="border-l-2 border-accent-editorial pl-4 text-[15px] text-foreground leading-[1.65]">
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

      {/* 이번 면접 분야별 점수 */}
      {dimensionEntries.length > 0 && (
        <>
          <hr className="border-border" />
          <section>
            <SectionHeader
              label="이번 면접 분야별 점수"
              caption="5점 만점 · 면접 답변에서 평가한 분야별 평균"
            />
            <div>
              {dimensionEntries.map(([name, score]) => (
                <DimensionScoreRow key={name} name={name} score={score} />
              ))}
            </div>
          </section>
        </>
      )}

      {/* 잘한 점 */}
      {data.strengths && data.strengths.length > 0 && (
        <>
          <hr className="border-border" />
          <section>
            <SectionHeader label="잘한 점" />
            <div className="space-y-3">
              {data.strengths.map((item, idx) => (
                <StrengthItem key={idx} item={item} />
              ))}
            </div>
          </section>
        </>
      )}

      {/* 다음에 챙길 점 */}
      {data.gaps && data.gaps.length > 0 && (
        <>
          <hr className="border-border" />
          <section>
            <SectionHeader label="개선할 점" />
            <div className="space-y-3">
              {data.gaps.map((item, idx) => (
                <GapItem key={idx} item={item} />
              ))}
            </div>
          </section>
        </>
      )}

      {/* 이번 주 추천 학습 */}
      {data.weekPlan && data.weekPlan.length > 0 && (
        <>
          <hr className="border-border" />
          <section>
            <SectionHeader label="이번 주 추천 학습" />
            <div className="space-y-4">
              {data.weekPlan.map((item, idx) => (
                <WeekPlanItem key={idx} item={item} />
              ))}
            </div>
          </section>
        </>
      )}

      {/* 말하기 습관 */}
      {hasDelivery && data.delivery && (
        <>
          <hr className="border-border" />
          <section>
            <SectionHeader label="말하기 습관" />
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
          <DialogDescription className="text-[12px] text-muted-foreground mt-1">
            면접 답변을 코치가 짚어준 노트입니다
          </DialogDescription>
        </DialogHeader>
        <div className="overflow-y-auto max-h-[80vh] px-6 py-6">{renderBody()}</div>
      </DialogContent>
    </Dialog>
  )
}
