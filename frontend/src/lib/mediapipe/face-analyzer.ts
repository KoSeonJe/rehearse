import type { FaceLandmarkerResult } from '@mediapipe/tasks-vision'

interface GazeResult {
  x: number
  y: number
  isDeviated: boolean
}

const LEFT_IRIS = [468, 469, 470, 471, 472]
const RIGHT_IRIS = [473, 474, 475, 476, 477]
const LEFT_EYE_INNER = 133
const LEFT_EYE_OUTER = 33
const RIGHT_EYE_INNER = 362
const RIGHT_EYE_OUTER = 263

export const analyzeGaze = (result: FaceLandmarkerResult): GazeResult | null => {
  if (!result.faceLandmarks || result.faceLandmarks.length === 0) {
    return null
  }

  const landmarks = result.faceLandmarks[0]

  const leftIrisCenter = getCenter(LEFT_IRIS.map((i) => landmarks[i]))
  const rightIrisCenter = getCenter(RIGHT_IRIS.map((i) => landmarks[i]))

  const leftEyeWidth = Math.abs(landmarks[LEFT_EYE_OUTER].x - landmarks[LEFT_EYE_INNER].x)
  const rightEyeWidth = Math.abs(landmarks[RIGHT_EYE_OUTER].x - landmarks[RIGHT_EYE_INNER].x)

  const leftEyeCenter = {
    x: (landmarks[LEFT_EYE_OUTER].x + landmarks[LEFT_EYE_INNER].x) / 2,
    y: (landmarks[LEFT_EYE_OUTER].y + landmarks[LEFT_EYE_INNER].y) / 2,
  }
  const rightEyeCenter = {
    x: (landmarks[RIGHT_EYE_OUTER].x + landmarks[RIGHT_EYE_INNER].x) / 2,
    y: (landmarks[RIGHT_EYE_OUTER].y + landmarks[RIGHT_EYE_INNER].y) / 2,
  }

  const leftGazeX = leftEyeWidth > 0 ? (leftIrisCenter.x - leftEyeCenter.x) / leftEyeWidth : 0
  const rightGazeX = rightEyeWidth > 0 ? (rightIrisCenter.x - rightEyeCenter.x) / rightEyeWidth : 0

  const gazeX = (leftGazeX + rightGazeX) / 2
  const gazeY = ((leftIrisCenter.y - leftEyeCenter.y) + (rightIrisCenter.y - rightEyeCenter.y)) / 2 * 5

  const TOLERANCE = 0.15
  const isDeviated = Math.abs(gazeX) > TOLERANCE || Math.abs(gazeY) > TOLERANCE

  return { x: gazeX, y: gazeY, isDeviated }
}

const getCenter = (points: Array<{ x: number; y: number }>) => {
  const sum = points.reduce(
    (acc, p) => ({ x: acc.x + p.x, y: acc.y + p.y }),
    { x: 0, y: 0 },
  )
  return { x: sum.x / points.length, y: sum.y / points.length }
}
