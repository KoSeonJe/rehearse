import { Sparkles } from 'lucide-react'

interface CoachNoteFabProps {
  onClick: () => void
  isLoading: boolean
}

export const CoachNoteFab = ({ onClick, isLoading }: CoachNoteFabProps) => {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={isLoading}
      aria-label="코치 노트 열기"
      aria-busy={isLoading ? 'true' : undefined}
      className={[
        'fixed bottom-6 left-6 z-40',
        'flex items-center gap-2 px-4 py-2.5 rounded-full',
        'bg-brand text-white',
        'shadow-[0_8px_24px_-8px_rgba(15,118,110,0.45)]',
        'transition-transform hover:-translate-y-0.5 active:translate-y-0',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand focus-visible:ring-offset-2',
        isLoading
          ? 'opacity-70 cursor-not-allowed'
          : 'hover:bg-brand-hover cursor-pointer',
      ].join(' ')}
    >
      {isLoading ? (
        <span
          className="h-4 w-4 rounded-full border-2 border-white/30 border-t-white animate-spin"
          aria-hidden="true"
        />
      ) : (
        <Sparkles size={16} aria-hidden="true" />
      )}
      <span className="hidden sm:inline text-[13px] font-semibold leading-none">
        {isLoading ? '코치 노트 준비 중' : '코치 노트'}
      </span>
    </button>
  )
}
