import type { DeviceTestStatus } from '@/types/device'
import { StatusIcon, StatusMessage } from '@/components/interview/device-test-utils'
import { statusBorder } from '@/components/interview/device-test-status'

interface SpeakerTestRowProps {
  status: DeviceTestStatus
  onSpeakerTest: () => void
  onConfirmSpeaker: () => void
  onReset?: () => void
}

export const SpeakerTestRow = ({ status, onSpeakerTest, onConfirmSpeaker, onReset }: SpeakerTestRowProps) => {
  return (
    <div className={`flex items-center gap-5 rounded-[20px] border bg-white p-5 transition-all ${statusBorder(status)}`}>
      <div className="flex h-20 w-32 shrink-0 items-center justify-center rounded-2xl bg-surface">
        {status === 'testing' ? (
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
          <svg className={`h-8 w-8 ${status === 'passed' ? 'text-success/50' : 'text-text-tertiary/30'}`} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M19.114 5.636a9 9 0 010 12.728M16.463 8.288a5.25 5.25 0 010 7.424M6.75 8.25l4.72-4.72a.75.75 0 011.28.53v15.88a.75.75 0 01-1.28.53l-4.72-4.72H4.51c-.88 0-1.704-.507-1.938-1.354A9.01 9.01 0 012.25 12c0-.83.112-1.633.322-2.396C2.806 8.756 3.63 8.25 4.51 8.25H6.75z" />
          </svg>
        )}
      </div>

      <div className="flex min-w-0 flex-1 flex-col gap-1">
        <div className="flex items-center gap-2">
          <h3 className="text-sm font-extrabold text-text-primary">스피커</h3>
          <StatusIcon status={status} />
        </div>
        <p className="text-xs text-text-tertiary">
          {status === 'testing' ? '소리가 들리나요?' : '질문 음성 재생에 사용됩니다'}
        </p>
        <StatusMessage status={status} passedText="스피커 정상" />
      </div>

      <div className="shrink-0">
        {status === 'idle' && (
          <button
            onClick={onSpeakerTest}
            className="h-10 rounded-xl bg-surface px-5 text-xs font-bold text-text-secondary transition-all hover:bg-slate-200 active:scale-95"
          >
            테스트
          </button>
        )}
        {status === 'testing' && (
          <button
            onClick={onConfirmSpeaker}
            className="h-10 rounded-xl bg-violet-legacy px-5 text-xs font-bold text-white transition-all hover:bg-violet-legacy/90 active:scale-95"
          >
            들려요
          </button>
        )}
        {status === 'passed' && onReset && (
          <button
            onClick={onReset}
            className="h-10 rounded-xl bg-surface px-5 text-xs font-bold text-text-secondary transition-all hover:bg-slate-200 active:scale-95"
          >
            다시 테스트
          </button>
        )}
      </div>
    </div>
  )
}
