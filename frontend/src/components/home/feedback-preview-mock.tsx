import { MockAvatar } from '@/components/home/mock-avatar'

type FeedbackPreviewVariant = 'hero' | 'proof'

interface FeedbackPreviewMockProps {
  variant?: FeedbackPreviewVariant
  className?: string
}

interface Segment {
  leftPct: number
  widthPct: number
  kind: 'main' | 'followup'
}

const SEGMENTS: Segment[] = [
  { leftPct: 6, widthPct: 18, kind: 'main' },
  { leftPct: 28, widthPct: 12, kind: 'followup' },
  { leftPct: 44, widthPct: 22, kind: 'main' },
  { leftPct: 70, widthPct: 14, kind: 'main' },
]

const PLAYHEAD_PCT = 52

const FILLER_WORDS = ['어', '음', '그']

const MY_ANSWER = '그러니까 저는 저번 프로젝트에서 어 테이블 인덱스를 바꿔서 음 쿼리 속도를 개선했고 그 다음에는 캐싱 전략을 고민했는데…'

/**
 * 실제 피드백 페이지 구조를 축소 재현한 랜딩용 Mock.
 * 기반: frontend/src/components/feedback/feedback-panel.tsx, timeline-bar.tsx, structured-comment.tsx
 * - 탭: "내 답변은 어땠을까" 활성, "어떤 인상을 줬을까" 비활성 표시만
 * - StructuredComment: 잘한 점 / 아쉬운 점 / 이렇게 말하면 더 좋아요
 * - TimelineBar: 원본/후속질문 segment 블록 (이름표 마커 없음)
 */
export const FeedbackPreviewMock = ({
  variant = 'hero',
  className = '',
}: FeedbackPreviewMockProps) => {
  const isProof = variant === 'proof'

  const FeedbackCard = (
    <div className="rounded-2xl bg-card shadow-sm overflow-hidden">
      {/* 헤더 */}
      <div className="px-5 pt-5 pb-4">
        <div className="flex items-center gap-3">
          <span className="font-tabular text-[13px] font-bold text-foreground">
            0:42 — 1:18
          </span>
          <span className="text-[13px] text-muted-foreground">원본 답변</span>
        </div>
        <p className="mt-3 text-[15px] font-bold leading-snug text-foreground">
          Q. 최근 프로젝트에서 겪은 기술적 문제와 해결 과정을 설명해 주세요.
        </p>
      </div>

      {/* 내 답변 박스 */}
      <div className="mx-5 mb-4 rounded-xl bg-muted/60 p-4">
        <p className="text-[11px] font-bold text-muted-foreground mb-1.5">내 답변</p>
        <p className="text-[13px] leading-[1.7] text-foreground/70">
          {MY_ANSWER.split(/(\s+)/).map((tok, i) =>
            FILLER_WORDS.includes(tok) ? (
              <span key={i} className="font-bold text-foreground underline decoration-foreground/30">
                {tok}
              </span>
            ) : (
              <span key={i}>{tok}</span>
            ),
          )}
        </p>
        <p className="mt-2 text-[11px] text-muted-foreground">습관어 7회 감지</p>
      </div>

      {/* 탭 */}
      <div className="px-5 border-b border-foreground/8 flex gap-5">
        <span className="pb-3 text-[13px] font-bold text-foreground border-b-2 border-foreground -mb-px">
          내 답변은 어땠을까
        </span>
        <span className="pb-3 text-[13px] font-medium text-foreground/30">
          어떤 인상을 줬을까
        </span>
      </div>

      {/* StructuredComment 3블록 */}
      <div className="px-5 py-5 space-y-2.5">
        <div className="rounded-xl bg-emerald-50/60 px-4 py-3">
          <p className="text-[12px] font-bold text-emerald-600 mb-0.5">잘한 점</p>
          <p className="text-[13px] leading-[1.6] text-foreground/75">
            구체적인 기술 선택 과정을 단계별로 설명해, 문제 해결 흐름이 잘 드러났어요.
          </p>
        </div>
        <div className="rounded-xl bg-amber-50/60 px-4 py-3">
          <p className="text-[12px] font-bold text-amber-600 mb-0.5">아쉬운 점</p>
          <p className="text-[13px] leading-[1.6] text-foreground/75">
            습관어(“어”, “음”)가 답변 중반 이후 반복돼, 확신 있는 말투가 약해졌어요.
          </p>
        </div>
        <div className="rounded-xl bg-slate-50 px-4 py-3">
          <p className="text-[12px] font-bold text-slate-500 mb-0.5">
            이렇게 말하면 더 좋아요
          </p>
          <p className="text-[13px] leading-[1.6] text-foreground/75">
            “인덱스를 바꿔서 쿼리 속도를 40% 줄였습니다”처럼 결과 수치를 먼저 꺼내면 더 명료해져요.
          </p>
        </div>
      </div>
    </div>
  )

  const VideoDock = (
    <div className="rounded-2xl bg-card shadow-sm overflow-hidden">
      <div className="relative aspect-video w-full overflow-hidden bg-muted/40 text-foreground">
        <MockAvatar className="absolute inset-0 h-full w-full" />
        {/* 재생 컨트롤 자리 */}
        <div className="absolute inset-0 flex items-center justify-center">
          <div className="flex h-10 w-10 items-center justify-center rounded-full bg-background/85 shadow-sm">
            <div className="ml-0.5 h-0 w-0 border-y-[6px] border-l-[9px] border-y-transparent border-l-foreground" />
          </div>
        </div>
        <div className="absolute bottom-2 right-3 font-tabular text-[10px] font-semibold text-foreground/60">
          0:42 / 2:30
        </div>
      </div>

      {/* Timeline + Legend */}
      <div className="px-3 py-3 space-y-2">
        <div className="relative h-6 w-full rounded-lg bg-muted/70 overflow-hidden">
          {SEGMENTS.map((seg, i) => (
            <div
              key={i}
              className={`absolute top-1 bottom-1 rounded-md ${
                seg.kind === 'followup' ? 'bg-foreground/25' : 'bg-foreground'
              } ${i === 0 ? 'ring-2 ring-foreground ring-offset-1 ring-offset-card' : ''}`}
              style={{ left: `${seg.leftPct}%`, width: `${seg.widthPct}%` }}
            />
          ))}
          <div
            className="absolute top-0 bottom-0 w-0.5 bg-foreground z-10"
            style={{ left: `${PLAYHEAD_PCT}%` }}
          />
        </div>
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-1.5">
            <span className="h-2 w-2 rounded-sm bg-foreground" />
            <span className="text-[10px] font-medium text-muted-foreground">원본</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="h-2 w-2 rounded-sm bg-foreground/25" />
            <span className="text-[10px] font-medium text-muted-foreground">후속질문</span>
          </div>
        </div>
      </div>
    </div>
  )

  // 상단 브라우저 크롬
  const Chrome = (
    <div className="flex items-center justify-between border-b border-foreground/8 px-4 py-2.5 bg-surface">
      <div className="flex items-center gap-1.5">
        <span className="h-2 w-2 rounded-full bg-foreground/12" />
        <span className="h-2 w-2 rounded-full bg-foreground/12" />
        <span className="h-2 w-2 rounded-full bg-foreground/12" />
      </div>
      <span className="font-tabular text-[10px] font-semibold tracking-tight text-foreground/40">
        rehearse.co.kr/interview/feedback
      </span>
      <span className="w-10" />
    </div>
  )

  return (
    <div
      aria-hidden="true"
      className={`relative w-full overflow-hidden rounded-3xl border border-border bg-background shadow-lg ${className}`}
    >
      {Chrome}
      <div className="p-4 md:p-5 bg-surface/60">
        {isProof ? (
          <div className="grid grid-cols-1 gap-4 lg:grid-cols-5">
            <div className="lg:col-span-3">{FeedbackCard}</div>
            <div className="lg:col-span-2">{VideoDock}</div>
          </div>
        ) : (
          <div className="space-y-3">
            {/* 히어로 variant: VideoDock을 먼저(작게) + FeedbackCard */}
            <div className="grid grid-cols-5 gap-3">
              <div className="col-span-2">{VideoDock}</div>
              <div className="col-span-3 flex items-center">
                <div className="w-full rounded-xl bg-muted/50 px-3 py-2.5">
                  <p className="text-[11px] font-bold text-muted-foreground">Q. 최근 프로젝트 문제</p>
                  <p className="mt-1 font-tabular text-[10px] text-muted-foreground">0:42 — 1:18 · 원본 답변</p>
                </div>
              </div>
            </div>
            {FeedbackCard}
          </div>
        )}
      </div>
    </div>
  )
}
