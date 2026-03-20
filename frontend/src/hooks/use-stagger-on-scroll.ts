import { useCallback, useState } from 'react'

interface UseStaggerOnScrollOptions {
  staggerDelay?: number
  threshold?: number
}

export const useStaggerOnScroll = <T extends HTMLElement = HTMLDivElement>(
  options: UseStaggerOnScrollOptions = {},
) => {
  const { staggerDelay = 100, threshold = 0.1 } = options
  const [isVisible, setIsVisible] = useState(false)

  const ref = useCallback(
    (node: T | null) => {
      if (!node) return

      const observer = new IntersectionObserver(
        ([entry]) => {
          if (entry.isIntersecting) {
            setIsVisible(true)
            observer.unobserve(node)
          }
        },
        { threshold },
      )

      observer.observe(node)
    },
    [threshold],
  )

  const getItemStyle = (index: number): React.CSSProperties => ({
    opacity: isVisible ? 1 : 0,
    transform: isVisible ? 'translateY(0px)' : 'translateY(20px)',
    transition: `opacity 500ms ease-out ${index * staggerDelay}ms, transform 500ms ease-out ${index * staggerDelay}ms`,
  })

  return { ref, isVisible, getItemStyle }
}
