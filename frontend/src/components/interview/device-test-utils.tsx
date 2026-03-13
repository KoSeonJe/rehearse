import type { DeviceTestStatus } from '@/types/device'

export const StatusIcon = ({ status }: { status: DeviceTestStatus }) => {
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

export const StatusMessage = ({ status, passedText, deniedText }: { status: DeviceTestStatus; passedText: string; deniedText?: string }) => {
  if (status === 'testing') return <span className="text-xs font-bold text-warning">확인 중...</span>
  if (status === 'passed') return <span className="text-xs font-bold text-success">{passedText}</span>
  if (status === 'denied') return <span className="text-xs font-bold text-error">{deniedText ?? '브라우저 설정에서 허용 후 새로고침'}</span>
  return null
}
