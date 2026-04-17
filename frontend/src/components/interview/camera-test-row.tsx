import type { DeviceTestStatus } from '@/types/device'
import { StatusIcon, StatusMessage } from '@/components/interview/device-test-utils'
import { statusBorder } from '@/components/interview/device-test-status'

interface CameraTestRowProps {
  status: DeviceTestStatus
  videoRef: React.RefObject<HTMLVideoElement | null>
  onCameraTest: () => void
}

export const CameraTestRow = ({ status, videoRef, onCameraTest }: CameraTestRowProps) => {
  return (
    <div className={`flex items-center gap-5 rounded-[20px] border bg-card p-5 transition-colors ${statusBorder(status)}`}>
      <div className="relative h-20 w-32 shrink-0 overflow-hidden rounded-2xl bg-slate-950">
        <video
          ref={videoRef as React.RefObject<HTMLVideoElement>}
          autoPlay
          playsInline
          muted
          className="h-full w-full object-cover"
        />
        {status !== 'passed' && (
          <div className="absolute inset-0 flex items-center justify-center bg-slate-900/80">
            <svg className="h-6 w-6 text-white/30" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
              <path strokeLinecap="round" strokeLinejoin="round" d="m15.75 10.5 4.72-4.72a.75.75 0 0 1 1.28.53v11.38a.75.75 0 0 1-1.28.53l-4.72-4.72M4.5 18.75h9a2.25 2.25 0 0 0 2.25-2.25v-9a2.25 2.25 0 0 0-2.25-2.25h-9A2.25 2.25 0 0 0 2.25 7.5v9a2.25 2.25 0 0 0 2.25 2.25Z" />
            </svg>
          </div>
        )}
      </div>

      <div className="flex min-w-0 flex-1 flex-col gap-1">
        <div className="flex items-center gap-2">
          <h3 className="text-sm font-extrabold text-text-primary">카메라</h3>
          <StatusIcon status={status} />
        </div>
        <p className="text-xs text-text-tertiary">영상 녹화에 사용됩니다</p>
        <StatusMessage status={status} passedText="카메라 정상" />
      </div>

      <div className="shrink-0">
        {status === 'idle' && (
          <button
            onClick={onCameraTest}
            className="h-10 rounded-xl bg-surface px-5 text-xs font-bold text-text-secondary transition-colors hover:bg-slate-200 active:scale-95"
          >
            테스트
          </button>
        )}
      </div>
    </div>
  )
}
