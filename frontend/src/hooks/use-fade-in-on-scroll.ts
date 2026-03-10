import { useCallback, useState } from 'react'

interface UseFadeInOnScrollOptions {
  threshold?: number
}

export const useFadeInOnScroll = <T extends HTMLElement = HTMLDivElement>(
  options: UseFadeInOnScrollOptions = {},
) => {
  const { threshold = 0.1 } = options
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

  const style: React.CSSProperties = {
    opacity: isVisible ? 1 : 0,
    transform: isVisible ? 'translateY(0px)' : 'translateY(20px)',
    transition: 'opacity 600ms ease-out, transform 600ms ease-out',
  }

  return { ref, style, isVisible }
}
