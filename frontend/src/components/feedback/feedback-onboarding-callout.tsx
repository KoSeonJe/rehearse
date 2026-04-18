import { useState } from 'react'
import { cn } from '@/lib/utils'

const STORAGE_KEY = 'rehearse.feedbackOnboarded'

interface FeedbackOnboardingCalloutProps {
  className?: string
}

const readInitialVisible = (): boolean => {
  if (typeof window === 'undefined') return false
  try {
    return !window.localStorage.getItem(STORAGE_KEY)
  } catch {
    return false
  }
}

/**
 * Dismissible onboarding callout for the feedback page.
 * Shown once — localStorage key suppresses on subsequent visits.
 * SSR-safe: initial visibility is computed via a lazy initializer.
 */
export const FeedbackOnboardingCallout = ({ className }: FeedbackOnboardingCalloutProps) => {
  const [visible, setVisible] = useState(readInitialVisible)

  const handleDismiss = () => {
    try {
      localStorage.setItem(STORAGE_KEY, '1')
    } catch {
      // ignore
    }
    setVisible(false)
  }

  if (!visible) return null

  return (
    <div
      role="note"
      className={cn(
        'relative flex items-start gap-4',
        'border-l-2 border-accent-editorial bg-accent-editorial/5 p-4 rounded-sm',
        className,
      )}
    >
      <div className="flex-1 min-w-0 space-y-1">
        <p className="text-[13px] font-semibold text-foreground">피드백 둘러보기</p>
        <p className="text-[13px] text-muted-foreground leading-relaxed">
          질문 목차에서 항목을 선택하면 해당 지점으로 이동해요. 마음에 드는 답변은 복습 목록에 담을 수 있어요.
        </p>
      </div>
      <button
        type="button"
        aria-label="안내 닫기"
        onClick={handleDismiss}
        className={cn(
          'shrink-0 flex items-center justify-center',
          'w-7 h-7 rounded-sm',
          'text-muted-foreground hover:text-foreground',
          'transition-colors duration-[var(--duration-fast)]',
        )}
      >
        <svg
          width="14"
          height="14"
          viewBox="0 0 14 14"
          fill="none"
          stroke="currentColor"
          strokeWidth="1.75"
          strokeLinecap="round"
        >
          <path d="M2 2l10 10M12 2L2 12" />
        </svg>
      </button>
    </div>
  )
}
