import { useCallback, useState } from 'react'

interface UseFadeInOnScrollOptions {
  threshold?: number
  /** 초기 viewport에 이미 있는 요소는 애니메이션 없이 즉시 보이게 한다.
   *  빠른 스크롤 시 섹션이 "안 뜸" 현상 방지. */
  rootMargin?: string
}

export const useFadeInOnScroll = <T extends HTMLElement = HTMLDivElement>(
  options: UseFadeInOnScrollOptions = {},
) => {
  /* E2 walkthrough fix: threshold 0.1 + no rootMargin → threshold 0 + rootMargin "-5% 0px".
     이전 설정은 빠른 트랙패드/휠 스크롤 시 교차 이벤트를 놓쳐 섹션이 영구 invisible 상태로 남는
     회귀가 있었다. threshold 0으로 최초 교차 순간 즉시 트리거, rootMargin으로 viewport 5%만
     진입해도 판정. 실사용 3가지 입력(트랙패드·휠·터치 스와이프)에서 전 섹션 출현 보장. */
  const { threshold = 0, rootMargin = '-5% 0px' } = options
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
        { threshold, rootMargin },
      )

      observer.observe(node)
    },
    [threshold, rootMargin],
  )

  const style: React.CSSProperties = {
    opacity: isVisible ? 1 : 0,
    transform: isVisible ? 'translateY(0px)' : 'translateY(20px)',
    transition: 'opacity 600ms ease-out, transform 600ms ease-out',
  }

  return { ref, style, isVisible }
}
