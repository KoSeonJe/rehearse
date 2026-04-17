import { Progress } from '@/components/ui/progress'

interface FinishingOverlayProps {
  open: boolean
  stage: 'uploading' | 'saving' | 'finalizing'
  total: number
  completed: number
}

const STAGE_LABELS: Record<FinishingOverlayProps['stage'], string> = {
  uploading: '답변 영상 업로드 중',
  saving: '답변 데이터 저장 중',
  finalizing: '면접 결과 준비 중',
}

export const FinishingOverlay = ({ open, stage, total, completed }: FinishingOverlayProps) => {
  if (!open) return null

  const showProgress = stage === 'uploading' && total > 0
  const percent = showProgress ? Math.min(100, Math.round((completed / total) * 100)) : 0

  return (
    <div
      className="fixed inset-0 z-[60] flex items-center justify-center bg-black/80 backdrop-blur-sm animate-fade-in"
      role="status"
      aria-live="polite"
      aria-label="면접을 안전하게 종료하는 중"
    >
      <div className="w-full max-w-sm mx-4 rounded-2xl bg-[#2c2c2c] border border-[#3c4043] p-8 shadow-2xl">
        <div className="flex flex-col items-center gap-5">
          {/* 스피너 */}
          <div className="relative w-12 h-12">
            <div className="absolute inset-0 rounded-full border-4 border-[#3c4043]" />
            <div className="absolute inset-0 rounded-full border-4 border-blue-500 border-t-transparent animate-spin" />
          </div>

          {/* 제목 */}
          <div className="text-center">
            <h2 className="text-base font-medium text-white mb-1">
              면접을 안전하게 종료하고 있어요
            </h2>
            <p className="text-sm text-studio-text-secondary">
              잠시만 기다려 주세요. 화면을 닫지 말아 주세요.
            </p>
          </div>

          {/* Stage 메시지 */}
          <div className="w-full">
            <div className="flex items-center justify-between text-xs text-studio-text-secondary mb-2">
              <span>{STAGE_LABELS[stage]}</span>
              {showProgress && (
                <span className="tabular-nums">
                  {completed} / {total}
                </span>
              )}
            </div>

            {/* 진행률 바 (uploading stage 에만) */}
            {showProgress && (
              <Progress
                value={percent}
                aria-label="녹화 업로드 진행률"
                className="h-1.5 bg-[#3c4043] [&>div]:bg-blue-500"
              />
            )}

            {/* saving/finalizing 단계에선 무한 progress bar */}
            {!showProgress && (
              <div className="h-1.5 w-full rounded-full bg-[#3c4043] overflow-hidden relative">
                <div className="absolute inset-y-0 w-1/4 rounded-full bg-blue-500 animate-progress-loading" />
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
