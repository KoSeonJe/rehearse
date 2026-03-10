export type PermissionStatus = 'idle' | 'granted' | 'denied'

export interface DevicePermissions {
  camera: PermissionStatus
  microphone: PermissionStatus
}
