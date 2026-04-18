import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { ChapterMarker } from '@/components/layout/chapter-marker'

describe('ChapterMarker', () => {
  it('index를 2자리 zero-padded 문자열로 렌더한다', () => {
    render(<ChapterMarker index={3} title="자기소개" />)
    expect(screen.getByText('03')).toBeInTheDocument()
  })

  it('index 숫자에 aria-hidden=true를 부여해 보조장비에서 숨긴다', () => {
    render(<ChapterMarker index={1} title="자기소개" />)
    const indexEl = screen.getByText('01')
    expect(indexEl).toHaveAttribute('aria-hidden', 'true')
  })

  it('title을 heading role로 노출한다', () => {
    render(<ChapterMarker index={2} title="프로젝트 설명" />)
    expect(screen.getByRole('heading', { name: '프로젝트 설명' })).toBeInTheDocument()
  })

  it('optional label을 렌더하거나 생략한다', () => {
    const { rerender } = render(<ChapterMarker index={1} title="t" label="BEHAVIORAL" />)
    expect(screen.getByText('BEHAVIORAL')).toBeInTheDocument()
    rerender(<ChapterMarker index={1} title="t" />)
    expect(screen.queryByText('BEHAVIORAL')).not.toBeInTheDocument()
  })
})
