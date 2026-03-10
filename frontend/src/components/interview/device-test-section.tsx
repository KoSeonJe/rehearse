import type { DevicePermissions, PermissionStatus } from '@/types/device'

interface DeviceTestSectionProps {
  permissions: DevicePermissions
  micLevel: number
  videoRef: React.RefObject<HTMLVideoElement | null>
}

const PermissionBadge = ({ status, label }: { status: PermissionStatus; label: string }) => {
  const icon = status === 'granted' ? '✅' : status === 'denied' ? '❌' : '⏳'
  const style =
    status === 'granted'
      ? 'bg-success-light/50 text-success border-success/20'
      : status === 'denied'
        ? 'bg-error-light/50 text-error border-error/20'
        : 'bg-background text-text-tertiary border-border/50'

  return (
    <span
      className={`inline-flex items-center gap-2 rounded-full border px-4 py-1.5 text-[11px] font-bold tracking-tight transition-all duration-300 ${style}`}
    >
      <span className="text-[10px]" aria-hidden="true">{icon}</span>
      {label}
    </span>
  )
}

export const DeviceTestSection = ({ permissions, micLevel, videoRef }: DeviceTestSectionProps) => {
  return (
    <div className="flex flex-col items-center w-full">
      <div className="flex items-center gap-3">
        <PermissionBadge status={permissions.camera} label="카메라 권한" />
        <PermissionBadge status={permissions.microphone} label="마이크 권한" />
      </div>

      {/* Camera Preview */}
      <div className="mt-8 relative w-full max-w-lg">
        <div className="overflow-hidden rounded-[32px] border border-border bg-slate-950 shadow-toss-lg">
          <div className="relative aspect-video w-full">
            <video
              ref={videoRef as React.RefObject<HTMLVideoElement>}
              autoPlay
              playsInline
              muted
              className="h-full w-full object-cover grayscale-[10%]"
            />

            {permissions.camera !== 'granted' && (
              <div className="absolute inset-0 flex items-center justify-center bg-slate-900/90 backdrop-blur-sm">
                <p className="font-bold text-white/60 text-sm">
                  {permissions.camera === 'denied' ? '카메라 접근이 거부되었습니다' : '카메라 권한을 허용해 주세요'}
                </p>
              </div>
            )}

            {/* Mic Pulse Overlay */}
            <div className="absolute bottom-6 right-6">
              <div className="flex items-center gap-2 h-10 px-4 bg-white/10 backdrop-blur-md rounded-2xl border border-white/10">
                <div className="flex items-end gap-0.5 h-4">
                  {[1, 2, 3, 4, 5].map((i) => (
                    <div
                      key={i}
                      className="w-[3px] bg-accent rounded-full transition-all duration-75"
                      style={{ height: `${Math.max(4, Math.min(micLevel * (i * 0.3), 100))}%` }}
                    />
                  ))}
                </div>
                <span className="text-[10px] font-black text-white/80 uppercase tracking-widest">Active</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Mic Status Label */}
      <div className="mt-6 flex flex-col items-center gap-2">
        <p className="text-sm font-extrabold text-text-primary">
          {permissions.microphone === 'granted' ? (micLevel > 10 ? '목소리가 잘 들려요!' : '한번 말씀해 보세요') : '마이크 권한이 필요해요'}
        </p>
        <div className="h-1 w-24 bg-slate-100 rounded-full overflow-hidden">
          <div
            className="h-full bg-accent transition-all duration-75"
            style={{ width: `${Math.min(micLevel, 100)}%` }}
          />
        </div>
      </div>

      {/* Denied Help Message */}
      {(permissions.camera === 'denied' || permissions.microphone === 'denied') && (
        <p className="mt-6 max-w-sm text-center text-xs font-bold text-text-tertiary leading-relaxed">
          브라우저 설정에서 카메라/마이크 권한을 허용한 뒤<br />
          페이지를 새로고침해 주세요.
        </p>
      )}
    </div>
  )
}
