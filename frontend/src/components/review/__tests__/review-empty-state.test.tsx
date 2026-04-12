import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { ReviewEmptyState } from '@/components/review/review-empty-state'

describe('ReviewEmptyState', () => {
  it('renders base empty state copy when isFiltered is false', () => {
    render(<ReviewEmptyState isFiltered={false} />)

    expect(screen.getByText('아직 담긴 답변이 없어요.')).toBeInTheDocument()
    expect(screen.getByText(/복습 목록에 담기/)).toBeInTheDocument()
  })

  it('renders filtered empty state copy when isFiltered is true', () => {
    render(<ReviewEmptyState isFiltered={true} />)

    expect(screen.getByText('해당 조건의 답변이 없어요.')).toBeInTheDocument()
    expect(
      screen.getByText('필터를 변경하거나 전체 목록을 확인해 보세요.'),
    ).toBeInTheDocument()
  })

  it('icon is present in the base (unfiltered) variant', () => {
    const { container } = render(<ReviewEmptyState isFiltered={false} />)

    // lucide renders an <svg> element; aria-hidden is set on it
    const svgs = container.querySelectorAll('svg[aria-hidden="true"]')
    expect(svgs.length).toBeGreaterThanOrEqual(1)
  })

  it('icon is present in the filtered variant', () => {
    const { container } = render(<ReviewEmptyState isFiltered={true} />)

    const svgs = container.querySelectorAll('svg[aria-hidden="true"]')
    expect(svgs.length).toBeGreaterThanOrEqual(1)
  })
})
