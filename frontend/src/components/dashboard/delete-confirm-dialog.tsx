import { Button } from '@/components/ui/button'
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogDescription,
  AlertDialogFooter,
} from '@/components/ui/alert-dialog'

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
  const handleOpenChange = (open: boolean) => {
    if (!open && !isPending) onCancel()
  }

  return (
    <AlertDialog open={isOpen} onOpenChange={handleOpenChange}>
      <AlertDialogContent className="bg-white rounded-card shadow-toss-lg border-none max-w-sm">
        <AlertDialogHeader>
          <AlertDialogTitle className="text-base font-extrabold text-text-primary">
            {`${itemLabel}을 삭제하시겠습니까?`}
          </AlertDialogTitle>
          <AlertDialogDescription className="text-sm text-text-secondary">
            {`삭제된 ${itemLabel}은 복구할 수 없습니다.`}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter className="flex gap-3 sm:space-x-0">
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
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
