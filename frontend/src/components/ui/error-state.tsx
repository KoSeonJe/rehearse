import { Button } from '@/components/ui/button'
import { Character } from '@/components/ui/character'
import { cn } from '@/lib/utils'

export interface ErrorStateAction {
  label: string
  onClick: () => void
  variant?: 'default' | 'outline' | 'ghost'
}

interface ErrorStateProps {
  /** 큰 헤드라인 — "문제가 발생했어요", "세션을 찾을 수 없습니다" 등 */
  title: string
  /** 헤드라인 아래 보조 설명 — 원인별 구체적 안내 */
  description?: string
  /** 하단 액션 버튼 목록. 워크스루 원칙: 에러 페이지에 "홈으로 돌아가기"만
   *  있는 페이지 0개 → 최소 2개 대안 경로 제공 권장. */
  actions?: ErrorStateAction[]
  /** 시각 톤 — 기본 confused, 404 등에서는 다른 mood 사용 가능 */
  mood?: 'confused' | 'thinking' | 'happy'
  className?: string
}

/** 공용 에러 상태 컴포넌트.
 *
 * T1.6 walkthrough fix: interview-ready/feedback/analysis 페이지의 에러 처리가
 * 각자 다른 인라인 구현으로 분산되어 있고, 대부분 "홈으로 돌아가기" 단일 버튼이라
 * 사용자가 복구 경로를 모른다. 이 컴포넌트로 일관된 UX와 최소 2개 액션을 강제한다.
 */
export const ErrorState = ({
  title,
  description,
  actions = [],
  mood = 'confused',
  className,
}: ErrorStateProps) => (
  <div
    className={cn('mx-auto max-w-2xl px-5 pt-24 text-center', className)}
    role="alert"
    aria-live="polite"
  >
    <Character mood={mood} size={120} className="mx-auto mb-8" />
    <h1 className="text-3xl font-extrabold text-text-primary">{title}</h1>
    {description && (
      <p className="mt-4 text-text-secondary">{description}</p>
    )}
    {actions.length > 0 && (
      <div className="mt-12 flex flex-col gap-3">
        {actions.map((action, index) => (
          <Button
            key={action.label}
            variant={action.variant ?? (index === 0 ? 'default' : 'outline')}
            size="lg"
            fullWidth
            onClick={action.onClick}
            className="rounded-3xl font-black"
          >
            {action.label}
          </Button>
        ))}
      </div>
    )}
  </div>
)
