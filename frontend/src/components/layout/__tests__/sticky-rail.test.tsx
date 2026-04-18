import { describe, expect, it } from 'vitest'
import { render } from '@testing-library/react'
import { StickyRail } from '@/components/layout/sticky-rail'

describe('StickyRail', () => {
  it('aside 시맨틱 엘리먼트를 렌더한다', () => {
    const { container } = render(<StickyRail>content</StickyRail>)
    expect(container.firstElementChild?.tagName).toBe('ASIDE')
  })

  it('기본 col과 offset 토큰을 적용한다', () => {
    const { container } = render(<StickyRail>content</StickyRail>)
    const aside = container.firstElementChild as HTMLElement
    expect(aside.className).toContain('col-span-4')
    expect(aside.className).toContain('top-[var(--utility-bar-height)]')
    expect(aside.className).toContain('sticky')
  })

  it('col prop으로 다른 컬럼 점유를 허용한다', () => {
    const { container } = render(<StickyRail col="col-span-3">x</StickyRail>)
    const aside = container.firstElementChild as HTMLElement
    expect(aside.className).toContain('col-span-3')
  })
})
