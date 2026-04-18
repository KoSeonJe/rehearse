import type { DeviceTestStatus } from '@/types/device'
import { StatusIcon, StatusMessage } from '@/components/interview/device-test-utils'
import { statusBorder } from '@/components/interview/device-test-status'

const MIC_LEVEL_SCALE_FACTOR = 0.4
const MIC_LEVEL_MIN_HEIGHT = 8

interface MicTestRowProps {
  status: DeviceTestStatus
  micLevel: number
  onMicTest: () => void
  onReset?: () => void
}

export const MicTestRow = ({ status, micLevel, onMicTest, onReset }: MicTestRowProps) => {
  return (
    <div className={`flex items-center gap-5 rounded-2xl border bg-card p-5 transition-colors ${statusBorder(status)}`}>
      <div className="flex h-20 w-32 shrink-0 items-center justify-center rounded-2xl bg-surface">
        {(status === 'testing' || status === 'passed') ? (
          <div className="flex items-end justify-center gap-1 h-10">
            {[1, 2, 3, 4, 5, 6, 7].map((i) => (
              <div
                key={i}
                className="w-1.5 rounded-full bg-foreground transition-[height] duration-75"
                style={{
                  height: `${Math.max(MIC_LEVEL_MIN_HEIGHT, Math.min(micLevel * (i <= 4 ? i * MIC_LEVEL_SCALE_FACTOR : (8 - i) * MIC_LEVEL_SCALE_FACTOR), 100))}%`,
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

      <div className="flex min-w-0 flex-1 flex-col gap-1">
        <div className="flex items-center gap-2">
          <h3 className="text-sm font-extrabold text-text-primary">마이크</h3>
          <StatusIcon status={status} />
        </div>
        <p className="text-xs text-text-tertiary">
          {status === 'testing' ? '말해보세요...' : '음성 인식에 사용됩니다'}
        </p>
        <StatusMessage status={status} passedText="음성 감지 완료" />
        {status === 'testing' && (
          <div className="mt-1 h-1.5 w-full max-w-[200px] overflow-hidden rounded-full bg-slate-100">
            <div
              className="h-full rounded-full bg-foreground transition-[width] duration-75"
              style={{ width: `${Math.min(micLevel, 100)}%` }}
            />
          </div>
        )}
      </div>

      <div className="shrink-0">
        {status === 'idle' && (
          <button
            onClick={onMicTest}
            className="h-10 rounded-xl bg-surface px-5 text-xs font-bold text-text-secondary transition-colors hover:bg-slate-200 active:scale-95"
          >
            테스트
          </button>
        )}
        {status === 'passed' && onReset && (
          <button
            onClick={onReset}
            className="h-10 rounded-xl bg-surface px-5 text-xs font-bold text-text-secondary transition-colors hover:bg-slate-200 active:scale-95"
          >
            다시 테스트
          </button>
        )}
      </div>
    </div>
  )
}
