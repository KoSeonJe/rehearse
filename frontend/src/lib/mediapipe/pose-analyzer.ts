import type { PoseLandmarkerResult } from '@mediapipe/tasks-vision'

interface PostureResult {
  shoulderTiltDegrees: number
  isTilted: boolean
}

const LEFT_SHOULDER = 11
const RIGHT_SHOULDER = 12

const TILT_THRESHOLD_DEGREES = 10

export const analyzePosture = (result: PoseLandmarkerResult): PostureResult | null => {
  if (!result.landmarks || result.landmarks.length === 0) {
    return null
  }

  const landmarks = result.landmarks[0]
  const leftShoulder = landmarks[LEFT_SHOULDER]
  const rightShoulder = landmarks[RIGHT_SHOULDER]

  if (!leftShoulder || !rightShoulder) return null

  const deltaY = rightShoulder.y - leftShoulder.y
  const deltaX = rightShoulder.x - leftShoulder.x

  const tiltRadians = Math.atan2(deltaY, deltaX)
  const tiltDegrees = tiltRadians * (180 / Math.PI)

  const isTilted = Math.abs(tiltDegrees) > TILT_THRESHOLD_DEGREES

  return {
    shoulderTiltDegrees: tiltDegrees,
    isTilted,
  }
}
