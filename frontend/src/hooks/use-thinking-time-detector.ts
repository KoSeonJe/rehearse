import { useCallback, useEffect, useRef } from 'react'

const THINKING_KEYWORDS = ['잠시만', '잠깐만', '생각 좀', '정리 좀', '생각할', '생각해']
const COOLDOWN_MS = 30_000

interface UseThinkingTimeDetectorOptions {
  interimText: string
  enabled: boolean
  onThinkingTimeRequested: () => void
}

export const useThinkingTimeDetector = ({
  interimText,
  enabled,
  onThinkingTimeRequested,
}: UseThinkingTimeDetectorOptions) => {
  const lastDetectedRef = useRef<number>(0)
  const onCallbackRef = useRef(onThinkingTimeRequested)

  useEffect(() => {
    onCallbackRef.current = onThinkingTimeRequested
  })

  const checkForThinkingKeyword = useCallback(
    (text: string) => {
      if (!enabled || !text) return

      const now = Date.now()
      if (now - lastDetectedRef.current < COOLDOWN_MS) return

      const matched = THINKING_KEYWORDS.some((keyword) => {
        const keywordIndex = text.indexOf(keyword)
        if (keywordIndex === -1) return false

        const before = text.slice(Math.max(0, keywordIndex - 5), keywordIndex)
        const hasNegation = before.includes('안') || before.includes('못')
        return !hasNegation
      })

      if (matched) {
        lastDetectedRef.current = now
        onCallbackRef.current()
      }
    },
    [enabled],
  )

  useEffect(() => {
    checkForThinkingKeyword(interimText)
  }, [interimText, checkForThinkingKeyword])

  const resetCooldown = useCallback(() => {
    lastDetectedRef.current = 0
  }, [])

  return { resetCooldown }
}
