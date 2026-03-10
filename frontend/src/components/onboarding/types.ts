export type JobField =
  | 'backend'
  | 'frontend'
  | 'fullstack'
  | 'devops'
  | 'data-ai'
  | 'mobile'

export interface JobOption {
  id: JobField
  label: string
  icon: React.ReactNode
}

export type PermissionStatus = 'idle' | 'granted' | 'denied'

export interface DevicePermissions {
  camera: PermissionStatus
  microphone: PermissionStatus
}
