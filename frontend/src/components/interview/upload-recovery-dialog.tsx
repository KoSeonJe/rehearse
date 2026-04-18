interface UploadRecoveryDialogProps {
  open: boolean
  failedCount: number
  onConfirm: () => void
  onCancel: () => void
}

export const UploadRecoveryDialog = ({
  open,
  failedCount,
  onConfirm,
  onCancel,
}: UploadRecoveryDialogProps) => {
  if (!open) return null

  return (
    <div
      className="fixed inset-0 z-[70] flex items-center justify-center bg-black/60"
      role="dialog"
      aria-modal="true"
      aria-labelledby="upload-recovery-dialog-title"
    >
      <div
        className="w-full max-w-sm mx-4 rounded-radius-lg bg-card border border-foreground/10 p-6 shadow-lg animate-fade-in"
        onClick={(e) => e.stopPropagation()}
      >
        <h2
          id="upload-recovery-dialog-title"
          className="text-base font-semibold text-foreground mb-2"
        >
          답변 영상 {failedCount}개가 업로드되지 않았어요
        </h2>
        <p className="text-sm text-muted-foreground leading-relaxed mb-6">
          네트워크를 확인한 뒤 다시 시도하는 것을 권장해요.
          <br />
          그래도 종료하면 해당 답변의 피드백이 생성되지 않을 수 있어요.
        </p>
        <div className="flex justify-end gap-2">
          <button
            onClick={onCancel}
            className="cursor-pointer h-11 min-w-11 px-4 rounded-pill text-sm font-medium text-muted-foreground hover:bg-foreground/5 transition-colors"
          >
            계속하기
          </button>
          <button
            onClick={onConfirm}
            className="cursor-pointer h-11 min-w-11 px-4 rounded-pill bg-foreground text-sm font-medium text-background hover:bg-foreground/90 transition-colors active:scale-95"
          >
            그래도 종료
          </button>
        </div>
      </div>
    </div>
  )
}
