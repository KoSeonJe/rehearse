interface InterviewWebcamMockProps {
  className?: string
}

/**
 * 랜딩 히어로용 웹캠 뷰 Mock.
 * 실제 면접 진행 중 사용자가 보는 화면의 축소 재현 —
 * 면접자 일러스트 배경 + REC 상태 + 질문 caption.
 *
 * FeedbackPreviewMock(피드백 페이지 재현)과 역할을 분리해 히어로/증빙 섹션
 * 시각 중복을 제거한다.
 */
export const InterviewWebcamMock = ({ className = '' }: InterviewWebcamMockProps) => (
  <div
    aria-hidden="true"
    className={`relative w-full overflow-hidden rounded-3xl border border-border bg-background shadow-lg ${className}`}
  >
    {/* 상단 브라우저 크롬 */}
    <div className="flex items-center justify-between border-b border-foreground/8 px-4 py-2.5 bg-surface">
      <div className="flex items-center gap-1.5">
        <span className="h-2 w-2 rounded-full bg-foreground/12" />
        <span className="h-2 w-2 rounded-full bg-foreground/12" />
        <span className="h-2 w-2 rounded-full bg-foreground/12" />
      </div>
      <span className="font-tabular text-[10px] font-semibold tracking-tight text-foreground/40">
        rehearse.co.kr/interview/conduct
      </span>
      <span className="w-10" />
    </div>

    {/* 웹캠 뷰 — 면접자 일러스트를 실제 스트림처럼 채움 */}
    <div className="relative aspect-[4/3] w-full overflow-hidden bg-white">
      <img
        src="/images/interviewee-placeholder.png"
        alt=""
        className="absolute inset-0 h-full w-full object-cover"
        aria-hidden="true"
      />

      {/* 상단 좌측: REC 라벨 */}
      <div className="absolute top-4 left-4 flex items-center gap-1.5 rounded-full bg-background/80 px-2.5 py-1 shadow-sm">
        <span className="relative flex h-2 w-2">
          <span className="absolute inset-0 rounded-full bg-signal-record animate-ping opacity-60" />
          <span className="relative h-2 w-2 rounded-full bg-signal-record" />
        </span>
        <span className="font-tabular text-[10px] font-bold tracking-wider text-foreground/80">
          REC
        </span>
      </div>

      {/* 상단 우측: 타이머 */}
      <div className="absolute top-4 right-4 rounded-full bg-background/80 px-2.5 py-1 shadow-sm">
        <span className="font-tabular text-[10px] font-bold tracking-wide text-foreground/80">
          12:34
        </span>
      </div>

      {/* 하단 중앙: 질문 Caption (실제 페이지와 동일 구조) */}
      <div className="pointer-events-none absolute bottom-5 left-1/2 w-[calc(100%-32px)] max-w-md -translate-x-1/2">
        <div className="rounded-lg bg-foreground/75 px-4 py-2.5 text-center shadow-lg backdrop-blur-sm">
          <p className="text-[11px] md:text-[13px] font-semibold leading-relaxed text-background line-clamp-2">
            최근 프로젝트에서 겪은 기술적 문제와 해결 과정을 설명해 주세요.
          </p>
        </div>
      </div>
    </div>
  </div>
)
