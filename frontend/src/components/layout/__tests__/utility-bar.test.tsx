import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { UtilityBar } from '@/components/layout/utility-bar'

describe('UtilityBar', () => {
  it('banner role 헤더를 렌더한다', () => {
    render(<UtilityBar />)
    expect(screen.getByRole('banner')).toBeInTheDocument()
  })

  it('chapter 텍스트를 표시한다', () => {
    render(<UtilityBar chapter="FEEDBACK · Q3 of 8" />)
    expect(screen.getByText('FEEDBACK · Q3 of 8')).toBeInTheDocument()
  })

  it('actions 슬롯을 우측에 렌더한다', () => {
    render(
      <UtilityBar
        actions={
          <button type="button" aria-label="설정">
            cog
          </button>
        }
      />,
    )
    expect(screen.getByRole('button', { name: '설정' })).toBeInTheDocument()
  })

  it('utility-bar-height CSS 토큰을 적용한다', () => {
    render(<UtilityBar />)
    const banner = screen.getByRole('banner')
    expect(banner.className).toContain('h-[var(--utility-bar-height)]')
  })
})
