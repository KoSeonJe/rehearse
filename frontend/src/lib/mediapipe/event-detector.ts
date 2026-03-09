import type { NonVerbalEvent, NonVerbalEventType, Severity } from '../../types/interview'

interface DetectorState {
  gazeDeviationStart: number | null
  expressionFlatStart: number | null
  postureTiltStart: number | null
}

const THRESHOLDS = {
  GAZE_DEVIATION_DURATION_MS: 3000,
  GAZE_CENTER_TOLERANCE: 0.15,
  EXPRESSION_FLAT_DURATION_MS: 5000,
  POSTURE_TILT_DEGREES: 10,
  POSTURE_TILT_DURATION_MS: 3000,
}

export const createEventDetector = () => {
  const state: DetectorState = {
    gazeDeviationStart: null,
    expressionFlatStart: null,
    postureTiltStart: null,
  }

  const events: NonVerbalEvent[] = []

  const createEvent = (
    type: NonVerbalEventType,
    severity: Severity,
    description: string,
    value?: number,
    duration?: number,
  ): NonVerbalEvent => ({
    timestamp: Date.now(),
    type,
    severity,
    data: { description, value, duration },
  })

  const checkGaze = (gazeX: number, gazeY: number): NonVerbalEvent | null => {
    const isDeviated =
      Math.abs(gazeX) > THRESHOLDS.GAZE_CENTER_TOLERANCE ||
      Math.abs(gazeY) > THRESHOLDS.GAZE_CENTER_TOLERANCE

    if (isDeviated) {
      if (!state.gazeDeviationStart) {
        state.gazeDeviationStart = Date.now()
      } else if (Date.now() - state.gazeDeviationStart >= THRESHOLDS.GAZE_DEVIATION_DURATION_MS) {
        const duration = (Date.now() - state.gazeDeviationStart) / 1000
        state.gazeDeviationStart = Date.now()

        const direction = Math.abs(gazeX) > Math.abs(gazeY)
          ? gazeX > 0 ? '우측' : '좌측'
          : gazeY > 0 ? '하단' : '상단'

        const event = createEvent(
          'gaze',
          duration > 5 ? 'high' : 'medium',
          `시선이 ${direction}으로 이탈`,
          Math.sqrt(gazeX * gazeX + gazeY * gazeY),
          duration,
        )
        events.push(event)
        return event
      }
    } else {
      state.gazeDeviationStart = null
    }
    return null
  }

  const checkPosture = (shoulderTiltDegrees: number): NonVerbalEvent | null => {
    const isTilted = Math.abs(shoulderTiltDegrees) > THRESHOLDS.POSTURE_TILT_DEGREES

    if (isTilted) {
      if (!state.postureTiltStart) {
        state.postureTiltStart = Date.now()
      } else if (Date.now() - state.postureTiltStart >= THRESHOLDS.POSTURE_TILT_DURATION_MS) {
        const duration = (Date.now() - state.postureTiltStart) / 1000
        state.postureTiltStart = Date.now()

        const direction = shoulderTiltDegrees > 0 ? '우측' : '좌측'
        const event = createEvent(
          'posture',
          Math.abs(shoulderTiltDegrees) > 20 ? 'high' : 'medium',
          `어깨가 ${direction}으로 기울어짐`,
          shoulderTiltDegrees,
          duration,
        )
        events.push(event)
        return event
      }
    } else {
      state.postureTiltStart = null
    }
    return null
  }

  const getEvents = () => [...events]

  const reset = () => {
    state.gazeDeviationStart = null
    state.expressionFlatStart = null
    state.postureTiltStart = null
    events.length = 0
  }

  return { checkGaze, checkPosture, getEvents, reset }
}

export type EventDetector = ReturnType<typeof createEventDetector>
