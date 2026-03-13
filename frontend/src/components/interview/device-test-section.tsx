import type { DeviceTestState } from '@/types/device'
import { CameraTestRow } from '@/components/interview/camera-test-row'
import { MicTestRow } from '@/components/interview/mic-test-row'
import { SpeakerTestRow } from '@/components/interview/speaker-test-row'

interface DeviceTestSectionProps {
  state: DeviceTestState
  micLevel: number
  videoRef: React.RefObject<HTMLVideoElement | null>
  onCameraTest: () => void
  onMicTest: () => void
  onSpeakerTest: () => void
  onConfirmSpeaker: () => void
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
      <CameraTestRow status={state.camera} videoRef={videoRef} onCameraTest={onCameraTest} />
      <MicTestRow status={state.microphone} micLevel={micLevel} onMicTest={onMicTest} />
      <SpeakerTestRow status={state.speaker} onSpeakerTest={onSpeakerTest} onConfirmSpeaker={onConfirmSpeaker} />
    </div>
  )
}
