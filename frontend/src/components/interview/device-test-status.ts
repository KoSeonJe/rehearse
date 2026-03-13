import type { DeviceTestStatus } from '@/types/device'

export const statusBorder = (status: DeviceTestStatus) => {
  if (status === 'passed') return 'border-success/30'
  if (status === 'denied') return 'border-error/30'
  if (status === 'testing') return 'border-warning/30'
  return 'border-border'
}
