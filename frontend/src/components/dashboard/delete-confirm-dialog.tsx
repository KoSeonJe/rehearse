import { Button } from '@/components/ui/button'
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogCancel,
  AlertDialogAction,
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
      {/* TODO(plan-05): rounded-card/shadow-toss-lg → 일관성 감사 단계에서 rounded-lg/shadow-md 토큰으로 정리 */}
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
          <AlertDialogCancel asChild>
            <Button
              variant="secondary"
              size="sm"
              onClick={onCancel}
              disabled={isPending}
              className="flex-1 h-11 mt-0"
            >
              취소
            </Button>
          </AlertDialogCancel>
          <AlertDialogAction asChild>
            <Button
              variant="destructive"
              size="sm"
              onClick={onConfirm}
              disabled={isPending}
              className="flex-1 h-11"
            >
              {isPending ? '삭제 중...' : '삭제'}
            </Button>
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
