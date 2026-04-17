import { useEffect } from 'react'
import { Button } from '@/components/ui/button'

interface DeleteConfirmDialogProps {
  isOpen: boolean
  onConfirm: () => void
  onCancel: () => void
  isPending: boolean
  itemLabel?: string
}

export const DeleteConfirmDialog = ({
  isOpen,
  onConfirm,
  onCancel,
  isPending,
  itemLabel = '면접',
}: DeleteConfirmDialogProps) => {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !isPending) onCancel()
    }
    if (isOpen) {
      document.addEventListener('keydown', handleKeyDown)
    }
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [isOpen, onCancel, isPending])

  if (!isOpen) return null

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm px-4"
      onClick={isPending ? undefined : onCancel}
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
          {`${itemLabel}을 삭제하시겠습니까?`}
        </h2>
        <p className="mt-2 text-sm text-text-secondary">
          {`삭제된 ${itemLabel}은 복구할 수 없습니다.`}
        </p>
        <div className="mt-6 flex gap-3">
          <Button
            variant="secondary"
            size="sm"
            onClick={onCancel}
            disabled={isPending}
            className="flex-1 h-11"
          >
            취소
          </Button>
          <Button
            variant="destructive"
            size="sm"
            onClick={onConfirm}
            disabled={isPending}
            className="flex-1 h-11"
          >
            {isPending ? '삭제 중...' : '삭제'}
          </Button>
        </div>
      </div>
    </div>
  )
}
