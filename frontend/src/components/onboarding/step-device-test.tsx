import type { DevicePermissions, PermissionStatus } from './types'

interface StepDeviceTestProps {
  permissions: DevicePermissions
  micLevel: number
  videoRef: React.RefObject<HTMLVideoElement | null>
}

const PermissionBadge = ({ status, label }: { status: PermissionStatus; label: string }) => {
  const icon = status === 'granted' ? '\u2705' : status === 'denied' ? '\u274C' : '\u23F3'
  const style =
    status === 'granted'
      ? 'bg-success-light text-success border-success/30'
      : status === 'denied'
        ? 'bg-error-light text-error border-error/30'
        : 'bg-background text-text-tertiary border-border'

  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-badge border px-3 py-1 text-xs font-medium ${style}`}
    >
      <span aria-hidden="true">{icon}</span>
      {label}
    </span>
  )
}

export const StepDeviceTest = ({ permissions, micLevel, videoRef }: StepDeviceTestProps) => {
  return (
    <div className="flex flex-col items-center">
      <h1 className="text-2xl font-semibold text-text-primary">
        카메라 &amp; 마이크 테스트
      </h1>
      <p className="mt-2 text-sm text-text-secondary">
        면접 녹화를 위해 카메라와 마이크 권한이 필요해요
      </p>

      <div className="mt-6 flex items-center gap-3">
        <PermissionBadge status={permissions.camera} label="카메라" />
        <PermissionBadge status={permissions.microphone} label="마이크" />
      </div>

      {/* Camera Preview */}
      <div className="mt-6 w-full max-w-sm overflow-hidden rounded-card border border-border bg-black">
        <div className="relative aspect-video w-full">
          <video
            ref={videoRef as React.RefObject<HTMLVideoElement>}
            autoPlay
            playsInline
            muted
            className="h-full w-full object-cover"
          />
          {permissions.camera === 'idle' && (
            <div className="absolute inset-0 flex items-center justify-center bg-black/80">
              <p className="text-sm text-text-tertiary">카메라 권한을 허용해주세요</p>
            </div>
          )}
          {permissions.camera === 'denied' && (
            <div className="absolute inset-0 flex items-center justify-center bg-black/80">
              <p className="text-sm text-error">카메라 접근이 거부되었습니다</p>
            </div>
          )}
        </div>
      </div>

      {/* Mic Level */}
      <div className="mt-4 w-full max-w-sm">
        <div className="mb-1 flex items-center justify-between">
          <span className="text-xs font-medium text-text-secondary">마이크 레벨</span>
          <span className="text-xs text-text-tertiary">
            {permissions.microphone === 'granted' ? '말해보세요' : '권한 필요'}
          </span>
        </div>
        <div className="h-2 w-full overflow-hidden rounded-badge bg-border">
          <div
            className="h-full rounded-badge bg-accent transition-all duration-75"
            style={{ width: `${Math.min(micLevel, 100)}%` }}
          />
        </div>
      </div>

      {permissions.camera === 'denied' || permissions.microphone === 'denied' ? (
        <p className="mt-4 max-w-sm text-center text-xs text-text-tertiary">
          브라우저 설정에서 카메라/마이크 권한을 허용한 뒤 페이지를 새로고침해주세요.
        </p>
      ) : null}
    </div>
  )
}
