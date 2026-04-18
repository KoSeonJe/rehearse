import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { PageGrid } from '@/components/layout/page-grid'

describe('PageGrid', () => {
  it('렌더된 자식을 12-col wrapper 안에 배치한다', () => {
    render(
      <PageGrid>
        <section data-testid="child">slot</section>
      </PageGrid>,
    )
    expect(screen.getByTestId('child')).toBeInTheDocument()
  })

  it('canvas max-width 및 반응형 grid 클래스를 적용한다', () => {
    const { container } = render(<PageGrid>content</PageGrid>)
    const wrapper = container.firstElementChild as HTMLElement
    expect(wrapper.className).toContain('max-w-canvas')
    expect(wrapper.className).toContain('grid-cols-4')
    expect(wrapper.className).toContain('md:grid-cols-8')
    expect(wrapper.className).toContain('lg:grid-cols-12')
  })

  it('as prop으로 semantic 엘리먼트를 교체한다', () => {
    const { container } = render(<PageGrid as="main">main</PageGrid>)
    expect(container.firstElementChild?.tagName).toBe('MAIN')
  })
})
