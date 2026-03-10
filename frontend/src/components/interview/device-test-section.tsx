import type { DeviceTestState, DeviceTestStatus } from '@/types/device'

interface DeviceTestSectionProps {
  state: DeviceTestState
  micLevel: number
  videoRef: React.RefObject<HTMLVideoElement | null>
  onCameraTest: () => void
  onMicTest: () => void
  onSpeakerTest: () => void
  onConfirmSpeaker: () => void
}

const StatusIcon = ({ status }: { status: DeviceTestStatus }) => {
  if (status === 'passed')
    return (
      <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-success/10">
        <svg className="h-4 w-4 text-success" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
        </svg>
      </span>
    )
  if (status === 'denied')
    return (
      <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-error/10">
        <svg className="h-4 w-4 text-error" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
        </svg>
      </span>
    )
  if (status === 'testing')
    return (
      <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-warning/10">
        <span className="h-3.5 w-3.5 animate-spin rounded-full border-2 border-warning border-t-transparent" />
      </span>
    )
  return (
    <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-surface">
      <span className="h-2.5 w-2.5 rounded-full bg-text-tertiary/40" />
    </span>
  )
}

const statusBorder = (status: DeviceTestStatus) => {
  if (status === 'passed') return 'border-success/30'
  if (status === 'denied') return 'border-error/30'
  if (status === 'testing') return 'border-warning/30'
  return 'border-border'
}

const StatusMessage = ({ status, passedText, deniedText }: { status: DeviceTestStatus; passedText: string; deniedText?: string }) => {
  if (status === 'testing') return <span className="text-xs font-bold text-warning">확인 중...</span>
  if (status === 'passed') return <span className="text-xs font-bold text-success">{passedText}</span>
  if (status === 'denied') return <span className="text-xs font-bold text-error">{deniedText ?? '브라우저 설정에서 허용 후 새로고침'}</span>
  return null
}

export const DeviceTestSection = ({
  state,
  micLevel,
  videoRef,
  onCameraTest,
  onMicTest,
  onSpeakerTest,
  onConfirmSpeaker,
}: DeviceTestSectionProps) => {
  return (
    <div className="flex flex-col gap-4">
      {/* Camera Row */}
      <div className={`flex items-center gap-5 rounded-[20px] border bg-white p-5 transition-all ${statusBorder(state.camera)}`}>
        {/* Preview */}
        <div className="relative h-20 w-32 shrink-0 overflow-hidden rounded-2xl bg-slate-950">
          <video
            ref={videoRef as React.RefObject<HTMLVideoElement>}
            autoPlay
            playsInline
            muted
            className="h-full w-full object-cover"
          />
          {state.camera !== 'passed' && (
            <div className="absolute inset-0 flex items-center justify-center bg-slate-900/80">
              <svg className="h-6 w-6 text-white/30" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
                <path strokeLinecap="round" strokeLinejoin="round" d="m15.75 10.5 4.72-4.72a.75.75 0 0 1 1.28.53v11.38a.75.75 0 0 1-1.28.53l-4.72-4.72M4.5 18.75h9a2.25 2.25 0 0 0 2.25-2.25v-9a2.25 2.25 0 0 0-2.25-2.25h-9A2.25 2.25 0 0 0 2.25 7.5v9a2.25 2.25 0 0 0 2.25 2.25Z" />
              </svg>
            </div>
          )}
        </div>

        {/* Info */}
        <div className="flex min-w-0 flex-1 flex-col gap-1">
          <div className="flex items-center gap-2">
            <h3 className="text-sm font-extrabold text-text-primary">카메라</h3>
            <StatusIcon status={state.camera} />
          </div>
          <p className="text-xs text-text-tertiary">영상 녹화에 사용됩니다</p>
          <StatusMessage status={state.camera} passedText="카메라 정상" />
        </div>

        {/* Action */}
        <div className="shrink-0">
          {state.camera === 'idle' && (
            <button
              onClick={onCameraTest}
              className="h-10 rounded-xl bg-surface px-5 text-xs font-bold text-text-secondary transition-all hover:bg-slate-200 active:scale-95"
            >
              테스트
            </button>
          )}
        </div>
      </div>

      {/* Microphone Row */}
      <div className={`flex items-center gap-5 rounded-[20px] border bg-white p-5 transition-all ${statusBorder(state.microphone)}`}>
        {/* Level Visualizer */}
        <div className="flex h-20 w-32 shrink-0 items-center justify-center rounded-2xl bg-surface">
          {(state.microphone === 'testing' || state.microphone === 'passed') ? (
            <div className="flex items-end justify-center gap-1 h-10">
              {[1, 2, 3, 4, 5, 6, 7].map((i) => (
                <div
                  key={i}
                  className="w-1.5 rounded-full bg-accent transition-all duration-75"
                  style={{
                    height: `${Math.max(8, Math.min(micLevel * (i <= 4 ? i * 0.4 : (8 - i) * 0.4), 100))}%`,
                  }}
                />
              ))}
            </div>
          ) : (
            <svg className="h-8 w-8 text-text-tertiary/30" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M12 18.75a6 6 0 006-6v-1.5m-6 7.5a6 6 0 01-6-6v-1.5m6 7.5v3.75m-3.75 0h7.5M12 15.75a3 3 0 01-3-3V4.5a3 3 0 116 0v8.25a3 3 0 01-3 3z" />
            </svg>
          )}
        </div>

        {/* Info */}
        <div className="flex min-w-0 flex-1 flex-col gap-1">
          <div className="flex items-center gap-2">
            <h3 className="text-sm font-extrabold text-text-primary">마이크</h3>
            <StatusIcon status={state.microphone} />
          </div>
          <p className="text-xs text-text-tertiary">
            {state.microphone === 'testing' ? '말해보세요...' : '음성 인식에 사용됩니다'}
          </p>
          <StatusMessage status={state.microphone} passedText="음성 감지 완료" />
          {state.microphone === 'testing' && (
            <div className="mt-1 h-1.5 w-full max-w-[200px] overflow-hidden rounded-full bg-slate-100">
              <div
                className="h-full rounded-full bg-accent transition-all duration-75"
                style={{ width: `${Math.min(micLevel, 100)}%` }}
              />
            </div>
          )}
        </div>

        {/* Action */}
        <div className="shrink-0">
          {state.microphone === 'idle' && (
            <button
              onClick={onMicTest}
              className="h-10 rounded-xl bg-surface px-5 text-xs font-bold text-text-secondary transition-all hover:bg-slate-200 active:scale-95"
            >
              테스트
            </button>
          )}
        </div>
      </div>

      {/* Speaker Row */}
      <div className={`flex items-center gap-5 rounded-[20px] border bg-white p-5 transition-all ${statusBorder(state.speaker)}`}>
        {/* Icon Area */}
        <div className="flex h-20 w-32 shrink-0 items-center justify-center rounded-2xl bg-surface">
          {state.speaker === 'testing' ? (
            <div className="flex items-center gap-1">
              {[1, 2, 3].map((i) => (
                <span
                  key={i}
                  className="inline-block h-3 w-3 animate-pulse rounded-full bg-warning"
                  style={{ animationDelay: `${i * 200}ms` }}
                />
              ))}
            </div>
          ) : (
            <svg className={`h-8 w-8 ${state.speaker === 'passed' ? 'text-success/50' : 'text-text-tertiary/30'}`} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M19.114 5.636a9 9 0 010 12.728M16.463 8.288a5.25 5.25 0 010 7.424M6.75 8.25l4.72-4.72a.75.75 0 011.28.53v15.88a.75.75 0 01-1.28.53l-4.72-4.72H4.51c-.88 0-1.704-.507-1.938-1.354A9.01 9.01 0 012.25 12c0-.83.112-1.633.322-2.396C2.806 8.756 3.63 8.25 4.51 8.25H6.75z" />
            </svg>
          )}
        </div>

        {/* Info */}
        <div className="flex min-w-0 flex-1 flex-col gap-1">
          <div className="flex items-center gap-2">
            <h3 className="text-sm font-extrabold text-text-primary">스피커</h3>
            <StatusIcon status={state.speaker} />
          </div>
          <p className="text-xs text-text-tertiary">
            {state.speaker === 'testing' ? '소리가 들리나요?' : '질문 음성 재생에 사용됩니다'}
          </p>
          <StatusMessage status={state.speaker} passedText="스피커 정상" />
        </div>

        {/* Action */}
        <div className="shrink-0">
          {state.speaker === 'idle' && (
            <button
              onClick={onSpeakerTest}
              className="h-10 rounded-xl bg-surface px-5 text-xs font-bold text-text-secondary transition-all hover:bg-slate-200 active:scale-95"
            >
              테스트
            </button>
          )}
          {state.speaker === 'testing' && (
            <button
              onClick={onConfirmSpeaker}
              className="h-10 rounded-xl bg-accent px-5 text-xs font-bold text-white transition-all hover:bg-accent/90 active:scale-95"
            >
              들려요
            </button>
          )}
        </div>
      </div>
    </div>
  )
}
