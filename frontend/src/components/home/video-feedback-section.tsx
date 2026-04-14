import { Character } from '@/components/ui/character'
import { useFadeInOnScroll } from '@/hooks/use-fade-in-on-scroll'

const PlayIcon = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <path d="M8 5.14v13.72L19 12 8 5.14z" fill="currentColor" />
  </svg>
)

export const VideoFeedbackSection = () => {
  const { ref, style } = useFadeInOnScroll<HTMLElement>()

  return (
    <section
      ref={ref}
      style={style}
      aria-labelledby="video-feedback-heading"
      className="bg-surface py-20 md:py-28"
    >
      <div className="mx-auto max-w-6xl px-5 md:px-8">

        {/* 헤딩 */}
        <div className="mb-12 text-center">
          <p className="font-mono text-[10px] font-black uppercase tracking-[0.2em] text-accent mb-4">
            VIDEO + FEEDBACK
          </p>
          <h2
            id="video-feedback-heading"
            className="text-3xl md:text-4xl font-extrabold tracking-tighter text-text-primary"
          >
            영상과 함께 제공되는 피드백
          </h2>
          <p className="mt-4 text-base text-text-secondary font-medium">
            각 답변의 정확한 순간에 피드백을 고정합니다.
          </p>
        </div>

        {/* 본문 목업 (정적) — items-stretch 로 좌/우 같은 높이 */}
        <div
          className="grid grid-cols-1 gap-6 md:grid-cols-5 md:items-stretch"
          aria-hidden="true"
        >
          {/* 좌측: 영상 + 타임라인 + 질문 목록 (flex col, 질문 목록이 bottom 까지 채움) */}
          <div className="md:col-span-3 flex flex-col gap-4 h-full">
            {/* 영상 영역 — 가상 인물(Character) */}
            <div className="relative aspect-video rounded-[20px] overflow-hidden border border-border bg-gradient-to-br from-slate-100 via-white to-indigo-50/40">
              <div className="absolute inset-0 bg-[radial-gradient(circle_at_50%_50%,rgba(99,102,241,0.08),transparent_70%)]" />
              <div className="absolute inset-0 flex items-center justify-center">
                <Character mood="happy" size={160} />
              </div>
            </div>

            {/* 재생 컨트롤 + 진행 바 */}
            <div>
              <div className="flex items-center gap-3">
                <div className="h-6 w-6 rounded-full flex items-center justify-center text-text-primary">
                  <PlayIcon />
                </div>
                <div className="flex-1 relative h-1 rounded-full bg-border">
                  <div className="absolute left-0 top-0 h-full w-[18%] rounded-full bg-text-tertiary" />
                  <div className="absolute top-1/2 -translate-y-1/2 -translate-x-1/2 left-[18%] w-3 h-3 rounded-full bg-white border-2 border-text-tertiary shadow" />
                </div>
                <span className="font-mono text-[11px] font-bold text-text-tertiary shrink-0">0:00 / 0:36</span>
              </div>
              {/* 세그먼트 바 (원본 / 후속질문 시각화) */}
              <div className="mt-3 relative h-2.5">
                <div className="absolute left-0 top-0 h-full rounded bg-accent" style={{ width: '30%' }} />
                <div className="absolute top-0 h-full rounded bg-accent/60" style={{ left: '76%', width: '8%' }} />
              </div>
              <div className="mt-2 flex items-center gap-4 px-0.5">
                <div className="flex items-center gap-1.5">
                  <span className="w-3 h-3 rounded-sm bg-accent" />
                  <span className="text-[10px] font-bold text-text-tertiary">원본</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <span className="w-3 h-3 rounded-sm bg-accent/60" />
                  <span className="text-[10px] font-bold text-text-tertiary">후속질문</span>
                </div>
              </div>
            </div>

            {/* 질문 목록 카드 — 하단까지 채움 */}
            <div className="flex-1 rounded-2xl bg-white border border-border p-5">
              <p className="text-sm font-extrabold text-text-primary mb-4">질문 목록</p>
              <div className="space-y-2">
                {/* Q1 */}
                <div className="flex items-center gap-3 p-3 rounded-xl bg-accent/5 border border-accent/20">
                  <span className="shrink-0 inline-flex h-7 w-7 items-center justify-center rounded-md bg-accent/10 text-accent text-[11px] font-black">Q1</span>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-bold text-text-primary truncate">메시지 큐(Message Queue)란 무엇이고, 왜 사용하나요?</p>
                    <p className="font-mono text-[11px] font-medium text-text-tertiary mt-0.5">0:00 ~ 0:11</p>
                  </div>
                  <span className="shrink-0 inline-flex items-center rounded-md bg-accent px-2.5 py-1 text-[11px] font-bold text-white">선택</span>
                </div>
                {/* Q1-1 후속 (들여쓰기 + 트리 커넥터) */}
                <div className="flex items-center gap-3 pl-6">
                  <span className="shrink-0 inline-flex h-7 min-w-[38px] px-1 items-center justify-center rounded-md bg-surface text-text-secondary text-[11px] font-black">Q1-1</span>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-text-secondary leading-snug line-clamp-2">메시지 큐가 비동기적으로 데이터를 교환한다고 하셨는데, 이 방식이 시스템의 확장성과 장애 복구에 어떤 이점을 제공하나요?</p>
                    <p className="font-mono text-[11px] font-medium text-text-tertiary mt-0.5">0:28 ~ 0:31</p>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* 우측: 피드백 패널 — flex col, h-full */}
          <div className="md:col-span-2 flex flex-col gap-4 h-full">
            {/* 질문 */}
            <h3 className="text-base font-extrabold text-text-primary leading-snug">
              Q. 메시지 큐(Message Queue)란 무엇이고, 왜 사용하나요?
            </h3>

            {/* 내 답변 */}
            <div className="rounded-xl bg-surface p-4 border border-border">
              <p className="text-[11px] font-extrabold text-text-primary mb-2">내 답변</p>
              <p className="text-xs text-text-secondary leading-relaxed">
                메시지 큐는 프로세스와 서비스 간에 이런 데이터를 비동기적으로 교환하기 위한 중간 우체통의 역할을 합니다.
              </p>
              <p className="mt-2 text-[10px] font-medium text-text-tertiary">습관어 1회 감지</p>
            </div>

            {/* 모범 답변 (접힘) */}
            <div className="rounded-xl bg-blue-50/60 border border-blue-100 p-3 flex items-center justify-between">
              <span className="text-xs font-bold text-accent">모범 답변</span>
              <span className="text-[11px] font-bold text-accent">펼치기</span>
            </div>

            {/* 탭 */}
            <div className="flex items-center gap-5 border-b border-border">
              <span className="pb-2 text-xs font-extrabold text-text-primary border-b-2 border-text-primary">내 답변은 어땠을까</span>
              <span className="pb-2 text-xs font-medium text-text-tertiary">어떤 인상을 줬을까</span>
            </div>

            {/* 분석 타이틀 */}
            <div>
              <p className="text-sm font-extrabold text-text-primary">답변 내용을 분석했어요</p>
              <p className="text-[11px] text-text-tertiary mt-0.5">기술적으로 맞게 답변했는지, 빠진 내용은 없는지 살펴봤어요.</p>
            </div>

            {/* 잘한 점 */}
            <div className="rounded-xl bg-emerald-50/70 border border-emerald-100 p-3">
              <p className="text-[11px] font-black text-emerald-600 mb-1">잘한 점</p>
              <p className="text-xs text-text-secondary leading-relaxed">
                메시지 큐의 정의를 '프로세스와 서비스 간에 데이터를 비동기적으로 교환하기 위한 중간 우체통'이라는 구절을 통해 핵심 개념을 간결하게 설명합니다.
              </p>
            </div>

            {/* 아쉬운 점 */}
            <div className="rounded-xl bg-amber-50/70 border border-amber-100 p-3">
              <p className="text-[11px] font-black text-amber-600 mb-1">아쉬운 점</p>
              <p className="text-xs text-text-secondary leading-relaxed">
                메시지 큐를 '왜 사용하나요?'라는 질문의 후반부에 대한 답변이 전혀 포함되지 않아 답변의 분량이 부족하고 불완전하게 들립니다.
              </p>
            </div>

            {/* 이렇게 말하면 더 좋아요 */}
            <div className="rounded-xl bg-white border border-border p-3">
              <p className="text-[11px] font-extrabold text-text-primary mb-1">이렇게 말하면 더 좋아요</p>
              <p className="text-xs text-text-secondary leading-relaxed">
                질문의 모든 요소를 파악하여 메시지 큐의 사용 목적(비동기 처리, 디커플링 등)을 구체적인 예시와 함께 설명하여 답변을 확장해야 합니다.
              </p>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
