import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { ReadingColumn } from '@/components/layout/reading-column'

describe('ReadingColumn', () => {
  it('55ch max-width와 1.65 line-height 토큰을 적용한다', () => {
    const { container } = render(
      <ReadingColumn>
        <p>본문</p>
      </ReadingColumn>,
    )
    const wrapper = container.firstElementChild as HTMLElement
    expect(wrapper.className).toContain('max-w-[55ch]')
    expect(wrapper.className).toContain('text-[1.0625rem]/[1.65]')
  })

  it('자식 본문이 정상적으로 렌더된다', () => {
    render(
      <ReadingColumn>
        <p>피드백 본문입니다</p>
      </ReadingColumn>,
    )
    expect(screen.getByText('피드백 본문입니다')).toBeInTheDocument()
  })
})
