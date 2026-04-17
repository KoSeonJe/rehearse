import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type CSSProperties,
  type ReactNode,
} from 'react'
import { createPortal } from 'react-dom'
import { ChevronDown } from 'lucide-react'
import { COACH_SEEN_KEY } from '@/constants/review-bookmark'

const SHOW_DELAY_MS = 600
const STEP_TRANSITION_MS = 180
const SCROLL_AUTO_DISMISS_PX = 120

type StepId = 'followup' | 'bookmark' | 'scroll'

interface StepConfig {
  id: StepId
  title: string
  body: string
  storageKey: string
  /** DOM selector for anchor; empty for steps without a visual anchor. */
  anchorSelector: string
}

const STEPS: readonly StepConfig[] = [
  {
    id: 'followup',
    title: '다른 질문도 눌러볼 수 있어요',
    body: '질문 목록에서 원하는 항목을 클릭하면 해당 답변 피드백으로 바로 이동해요.',
    storageKey: 'rehearse:review-coach-followup-seen-v1',
    anchorSelector: '[data-tutorial-anchor="question-list"]',
  },
  {
    id: 'bookmark',
    title: '답변을 복습 목록에 담아보세요',
    body: '다시 꺼내 보고 싶은 답변은 이 버튼으로 복습 목록에 저장할 수 있어요.',
    storageKey: 'rehearse:review-coach-bookmark-seen-v1',
    anchorSelector: '[data-tutorial-anchor="bookmark-button"]',
  },
  {
    id: 'scroll',
    title: '스크롤하면 다음 질문 세트',
    body: '화면을 아래로 내리면 다음 질문 세트 피드백이 이어서 나와요.',
    storageKey: 'rehearse:review-coach-scroll-seen-v1',
    anchorSelector: '',
  },
] as const

const TOTAL_STEPS = STEPS.length

// ---------------------------------------------------------------------------
// Context
// ---------------------------------------------------------------------------

interface ReviewTutorialContextValue {
  shouldTrigger: boolean
}

const ReviewTutorialContext = createContext<ReviewTutorialContextValue>({
  shouldTrigger: false,
})

interface ProviderProps {
  shouldTrigger: boolean
  children: ReactNode
}

export const ReviewTutorialProvider = ({ shouldTrigger, children }: ProviderProps) => {
  const value = useMemo(() => ({ shouldTrigger }), [shouldTrigger])
  return (
    <ReviewTutorialContext.Provider value={value}>{children}</ReviewTutorialContext.Provider>
  )
}

// ---------------------------------------------------------------------------
// Storage helpers
// ---------------------------------------------------------------------------

const safeRead = (key: string): string | null => {
  try {
    return localStorage.getItem(key)
  } catch {
    return null
  }
}

const safeWrite = (key: string, value: string): void => {
  try {
    localStorage.setItem(key, value)
  } catch {
    // localStorage unavailable — ignore
  }
}

const hasLegacyMaster = (): boolean => safeRead(COACH_SEEN_KEY) === '1'
const isStepSeen = (step: StepConfig): boolean => safeRead(step.storageKey) === '1'
const markStepSeen = (step: StepConfig): void => safeWrite(step.storageKey, '1')

const firstUnseenIndex = (): number => {
  if (hasLegacyMaster()) return -1
  return STEPS.findIndex((s) => !isStepSeen(s))
}

// ---------------------------------------------------------------------------
// Main stack
// ---------------------------------------------------------------------------

export const ReviewTutorialStack = () => {
  const { shouldTrigger } = useContext(ReviewTutorialContext)
  const [isMounted, setIsMounted] = useState(false)
  const [currentIdx, setCurrentIdx] = useState(-1)
  const [anchorRect, setAnchorRect] = useState<DOMRect | null>(null)
  const scrollStartYRef = useRef<number>(0)
  // Latch once started — tutorial persists through downstream shouldTrigger
  // flips (e.g. user clicks a followup and selectedFeedbackId changes).
  const hasStartedRef = useRef(false)

  const currentStep: StepConfig | null =
    currentIdx >= 0 && currentIdx < TOTAL_STEPS ? STEPS[currentIdx] : null

  const measure = useCallback((selector: string) => {
    if (!selector) {
      setAnchorRect(null)
      return
    }
    const el = document.querySelector(selector)
    const rect = el?.getBoundingClientRect() ?? null
    setAnchorRect((prev) => {
      // Skip re-render when geometry is unchanged — critical because this
      // runs every animation frame.
      if (!prev && !rect) return prev
      if (
        prev &&
        rect &&
        prev.left === rect.left &&
        prev.top === rect.top &&
        prev.width === rect.width &&
        prev.height === rect.height
      ) {
        return prev
      }
      return rect
    })
  }, [])

  // Initial mount: pick first unseen step after delay.
  useEffect(() => {
    if (!shouldTrigger || hasStartedRef.current) return
    const firstIdx = firstUnseenIndex()
    if (firstIdx === -1) return

    const timer = window.setTimeout(() => {
      hasStartedRef.current = true
      setCurrentIdx(firstIdx)
      setIsMounted(true)
      measure(STEPS[firstIdx].anchorSelector)
    }, SHOW_DELAY_MS)

    return () => window.clearTimeout(timer)
  }, [shouldTrigger, measure])

  // Track the anchor every frame so the highlight stays wrapped around the
  // target during scroll, sticky layout shifts, and resize. The measure()
  // shallow-compare above ensures no-op frames don't cause re-renders.
  useEffect(() => {
    if (!isMounted || !currentStep) return
    const selector = currentStep.anchorSelector
    if (!selector) return

    let raf = 0
    const tick = () => {
      measure(selector)
      raf = window.requestAnimationFrame(tick)
    }
    raf = window.requestAnimationFrame(tick)
    return () => window.cancelAnimationFrame(raf)
  }, [isMounted, currentStep, measure])

  // Scroll step: auto-dismiss once user scrolls enough.
  useEffect(() => {
    if (!isMounted || currentStep?.id !== 'scroll') return
    scrollStartYRef.current = window.scrollY

    const onScroll = () => {
      if (Math.abs(window.scrollY - scrollStartYRef.current) > SCROLL_AUTO_DISMISS_PX) {
        markStepSeen(currentStep)
        setIsMounted(false)
      }
    }
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [isMounted, currentStep])

  const advance = useCallback(() => {
    if (currentStep) markStepSeen(currentStep)

    const nextIdx = STEPS.findIndex((s, i) => i > currentIdx && !isStepSeen(s))
    if (nextIdx === -1) {
      setIsMounted(false)
      return
    }

    // Hide → measure → show so the highlight re-attaches smoothly.
    setCurrentIdx(-1)
    setAnchorRect(null)
    window.setTimeout(() => {
      setCurrentIdx(nextIdx)
      measure(STEPS[nextIdx].anchorSelector)
    }, STEP_TRANSITION_MS)
  }, [currentStep, currentIdx, measure])

  const skipAll = useCallback(() => {
    for (const s of STEPS) markStepSeen(s)
    setIsMounted(false)
  }, [])

  // Intentionally do NOT gate on shouldTrigger here — once the mount effect
  // has started the tutorial we keep it alive until the user dismisses it,
  // even if upstream props (e.g. selectedFeedbackId) change.
  if (!isMounted || !currentStep) return null

  const stepNumber = currentIdx + 1
  const isLast = stepNumber === TOTAL_STEPS

  return createPortal(
    <>
      {currentStep.anchorSelector && anchorRect && <AnchorHighlight rect={anchorRect} />}
      <TutorialDock
        step={currentStep}
        stepNumber={stepNumber}
        totalSteps={TOTAL_STEPS}
        isLast={isLast}
        onAdvance={advance}
        onSkip={skipAll}
      />
    </>,
    document.body,
  )
}

// ---------------------------------------------------------------------------
// Anchor highlight — stacked rings that breathe and radiate.
// ---------------------------------------------------------------------------

interface AnchorHighlightProps {
  rect: DOMRect
}

const AnchorHighlight = ({ rect }: AnchorHighlightProps) => {
  const pad = 8
  const baseStyle: CSSProperties = {
    position: 'fixed',
    left: rect.left - pad,
    top: rect.top - pad,
    width: rect.width + pad * 2,
    height: rect.height + pad * 2,
    borderRadius: 18,
    zIndex: 49,
    pointerEvents: 'none',
  }

  return (
    <>
      {/* Single breathing accent ring — subtle 'click me' affordance. */}
      <div
        aria-hidden="true"
        style={baseStyle}
        className="ring-2 ring-[#6366F1]/70 animate-tutorial-ring motion-reduce:animate-none"
      />
    </>
  )
}

// ---------------------------------------------------------------------------
// Tutorial dock — fixed at bottom-right, carries the step message.
// ---------------------------------------------------------------------------

interface TutorialDockProps {
  step: StepConfig
  stepNumber: number
  totalSteps: number
  isLast: boolean
  onAdvance: () => void
  onSkip: () => void
}

const TutorialDock = ({
  step,
  stepNumber,
  totalSteps,
  isLast,
  onAdvance,
  onSkip,
}: TutorialDockProps) => {
  const showBounce = step.id === 'scroll'

  return (
    <div
      role="dialog"
      aria-label={step.title}
      aria-modal="false"
      style={{
        position: 'fixed',
        right: 24,
        bottom: 'max(1.25rem, env(safe-area-inset-bottom))',
        zIndex: 50,
        width: 320,
        maxWidth: 'calc(100vw - 2rem)',
      }}
      className="relative overflow-hidden rounded-2xl bg-white p-4 shadow-xl ring-1 ring-black/5 animate-fade-in [animation-fill-mode:backwards] motion-reduce:animate-none"
    >
      <div className="mb-2 flex items-center justify-between gap-3">
        <StepDots current={stepNumber} total={totalSteps} />
        {!isLast && (
          <button
            type="button"
            onClick={onSkip}
            className="rounded text-[11px] font-semibold text-text-tertiary underline-offset-2 transition-colors hover:text-text-secondary hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#6366F1] focus-visible:ring-offset-1"
          >
            모두 건너뛰기
          </button>
        )}
      </div>

      <div className="flex items-start gap-3">
        <div className="flex-1">
          <strong className="mb-1 block text-[14px] font-bold text-text-primary">{step.title}</strong>
          <p className="text-[13px] font-medium leading-[1.55] text-text-secondary">{step.body}</p>
        </div>
        {showBounce && (
          <div
            aria-hidden="true"
            className="mt-0.5 flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-[#6366F1] to-[#8B5CF6] text-white shadow animate-tutorial-nudge motion-reduce:animate-none"
          >
            <ChevronDown size={18} aria-hidden="true" />
          </div>
        )}
      </div>

      <div className="mt-3 flex items-center justify-end">
        {/* TODO(design): variant 판단 보류 — 금지된 퍼플 그라디언트(from-[#6366F1] to-[#8B5CF6]) 사용 중, 교체 필요 */}
        <button
          type="button"
          onClick={onAdvance}
          className="inline-flex items-center gap-1 rounded-full bg-gradient-to-r from-[#6366F1] to-[#8B5CF6] px-4 py-1.5 text-[12px] font-bold text-white shadow-sm transition-shadow hover:shadow-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#6366F1] focus-visible:ring-offset-2"
        >
          {isLast ? '완료' : '다음'}
        </button>
      </div>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Step dots
// ---------------------------------------------------------------------------

interface StepDotsProps {
  current: number
  total: number
}

const StepDots = ({ current, total }: StepDotsProps) => (
  <div className="flex items-center gap-1" aria-label={`단계 ${current} / ${total}`}>
    {Array.from({ length: total }).map((_, i) => {
      const step = i + 1
      const isActive = step === current
      const isPast = step < current
      return (
        <span
          key={i}
          aria-hidden="true"
          className={`h-1.5 rounded-full transition-all duration-300 ${
            isActive
              ? 'w-5 bg-gradient-to-r from-[#6366F1] to-[#8B5CF6]'
              : isPast
                ? 'w-1.5 bg-[#6366F1]/50'
                : 'w-1.5 bg-gray-200'
          }`}
        />
      )
    })}
  </div>
)
