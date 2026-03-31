import { useEffect } from 'react'

interface DeleteConfirmDialogProps {
  isOpen: boolean
  onConfirm: () => void
  onCancel: () => void
  isPending: boolean
}

export const DeleteConfirmDialog = ({
  isOpen,
  onConfirm,
  onCancel,
  isPending,
}: DeleteConfirmDialogProps) => {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onCancel()
    }
    if (isOpen) {
      document.addEventListener('keydown', handleKeyDown)
    }
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [isOpen, onCancel])

  if (!isOpen) return null

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm px-4"
      onClick={onCancel}
    >
      <div
        className="bg-white rounded-card p-6 shadow-toss-lg max-w-sm w-full"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-labelledby="delete-dialog-title"
      >
        <h2
          id="delete-dialog-title"
          className="text-base font-extrabold text-text-primary"
        >
          면접을 삭제하시겠습니까?
        </h2>
        <p className="mt-2 text-sm text-text-secondary">
          삭제된 면접은 복구할 수 없습니다.
        </p>
        <div className="mt-6 flex gap-3">
          <button
            onClick={onCancel}
            disabled={isPending}
            className="flex-1 h-11 rounded-button border border-border text-sm font-bold text-text-secondary hover:bg-surface transition-all cursor-pointer disabled:opacity-50"
          >
            취소
          </button>
          <button
            onClick={onConfirm}
            disabled={isPending}
            className="flex-1 h-11 rounded-button bg-error text-white text-sm font-bold hover:opacity-90 active:scale-95 transition-all cursor-pointer disabled:opacity-50"
          >
            {isPending ? '삭제 중...' : '삭제'}
          </button>
        </div>
      </div>
    </div>
  )
}
