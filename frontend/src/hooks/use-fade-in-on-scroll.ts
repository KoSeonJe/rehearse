import { useEffect, useRef, useState } from 'react'

interface UseFadeInOnScrollOptions {
  threshold?: number
  duration?: number
}

export const useFadeInOnScroll = <T extends HTMLElement = HTMLDivElement>(
  options: UseFadeInOnScrollOptions = {},
) => {
  const { threshold = 0.1 } = options
  const ref = useRef<T>(null)
  const [isVisible, setIsVisible] = useState(false)

  useEffect(() => {
    const element = ref.current
    if (!element) return

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setIsVisible(true)
          observer.unobserve(element)
        }
      },
      { threshold },
    )

    observer.observe(element)

    return () => {
      observer.disconnect()
    }
  }, [threshold])

  const style: React.CSSProperties = {
    opacity: isVisible ? 1 : 0,
    transform: isVisible ? 'translateY(0px)' : 'translateY(20px)',
    transition: 'opacity 600ms ease-out, transform 600ms ease-out',
  }

  return { ref, style, isVisible }
}
