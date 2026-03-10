export type DeviceTestStatus = 'idle' | 'testing' | 'passed' | 'denied'

export interface DeviceTestState {
  camera: DeviceTestStatus
  microphone: DeviceTestStatus
  speaker: DeviceTestStatus
}
