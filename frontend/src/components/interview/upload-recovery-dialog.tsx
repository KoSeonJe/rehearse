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
        className="w-full max-w-sm mx-4 rounded-2xl bg-[#2c2c2c] border border-[#3c4043] p-6 shadow-2xl animate-fade-in"
        onClick={(e) => e.stopPropagation()}
      >
        <h2
          id="upload-recovery-dialog-title"
          className="text-base font-medium text-white mb-2"
        >
          답변 영상 {failedCount}개가 업로드되지 않았어요
        </h2>
        <p className="text-sm text-studio-text-secondary leading-relaxed mb-6">
          네트워크를 확인한 뒤 다시 시도하는 것을 권장해요.
          <br />
          그래도 종료하면 해당 답변의 피드백이 생성되지 않을 수 있어요.
        </p>
        <div className="flex justify-end gap-2">
          <button
            onClick={onCancel}
            className="cursor-pointer h-9 px-4 rounded-full text-sm font-medium text-blue-400 hover:bg-blue-400/10 transition-all"
          >
            계속하기
          </button>
          <button
            onClick={onConfirm}
            className="cursor-pointer h-9 px-4 rounded-full bg-blue-500 text-sm font-medium text-white transition-all hover:bg-blue-600 active:scale-95"
          >
            그래도 종료
          </button>
        </div>
      </div>
    </div>
  )
}
