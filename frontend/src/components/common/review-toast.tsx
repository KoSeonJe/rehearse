import { toast } from 'sonner'
import { TOAST_SEEN_PREFIX } from '@/constants/review-bookmark'

const AUTO_DISMISS_MS = 3000

export function showReviewToast(
  timestampFeedbackId: number,
  navigate: (path: string) => void,
): void {
  try {
    localStorage.setItem(`${TOAST_SEEN_PREFIX}${timestampFeedbackId}`, '1')
  } catch {
    // localStorage unavailable — ignore
  }

  toast('복습 목록에 담겼어요.', {
    duration: AUTO_DISMISS_MS,
    action: {
      label: '보러가기',
      onClick: () => navigate('/review-list'),
    },
  })
}
