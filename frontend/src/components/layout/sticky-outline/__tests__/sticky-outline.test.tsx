import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { StickyOutline, type OutlineItem } from '@/components/layout/sticky-outline'

const ITEMS: OutlineItem[] = [
  { id: 'q1', label: '자기소개', index: 1 },
  { id: 'q2', label: '프로젝트 경험', index: 2, hasIssue: true },
  { id: 'q3', label: '기술 스택', index: 3 },
]

describe('StickyOutline.Desktop', () => {
  it('질문 목차 nav를 렌더한다', () => {
    render(<StickyOutline.Desktop items={ITEMS} activeId="q1" onSelect={vi.fn()} />)
    expect(screen.getByRole('navigation', { name: '질문 목차' })).toBeInTheDocument()
  })

  it('active 항목에 aria-current=true를 부여한다', () => {
    render(<StickyOutline.Desktop items={ITEMS} activeId="q2" onSelect={vi.fn()} />)
    const active = screen.getByRole('button', { name: /프로젝트 경험/ })
    expect(active).toHaveAttribute('aria-current', 'true')
  })

  it('hasIssue 항목에 signal-warning 인디케이터를 표시한다', () => {
    render(<StickyOutline.Desktop items={ITEMS} activeId="q1" onSelect={vi.fn()} />)
    expect(screen.getByLabelText('이슈 있음')).toBeInTheDocument()
  })

  it('버튼 클릭 시 onSelect 콜백을 호출한다', async () => {
    const onSelect = vi.fn()
    const user = userEvent.setup()
    render(<StickyOutline.Desktop items={ITEMS} activeId="q1" onSelect={onSelect} />)
    await user.click(screen.getByRole('button', { name: /기술 스택/ }))
    expect(onSelect).toHaveBeenCalledWith('q3')
  })
})

describe('StickyOutline.TabBar', () => {
  it('질문 탭 nav를 렌더한다', () => {
    render(<StickyOutline.TabBar items={ITEMS} activeId="q1" onSelect={vi.fn()} />)
    expect(screen.getByRole('navigation', { name: '질문 탭' })).toBeInTheDocument()
  })

  it('index 라벨(01, 02, 03)을 렌더한다', () => {
    render(<StickyOutline.TabBar items={ITEMS} activeId="q1" onSelect={vi.fn()} />)
    expect(screen.getByRole('button', { name: /01/ })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /03/ })).toBeInTheDocument()
  })
})

describe('StickyOutline.MobileSheet', () => {
  beforeEach(() => {
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      value: vi.fn().mockImplementation((query: string) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
      })),
    })
  })

  it('트리거 버튼을 렌더한다', () => {
    render(<StickyOutline.MobileSheet items={ITEMS} activeId="q1" onSelect={vi.fn()} />)
    expect(screen.getByRole('button', { name: '목차' })).toBeInTheDocument()
  })

  it('트리거 오픈 후 항목 선택 시 onSelect를 호출한다', async () => {
    const onSelect = vi.fn()
    const user = userEvent.setup()
    render(<StickyOutline.MobileSheet items={ITEMS} activeId="q1" onSelect={onSelect} />)
    await user.click(screen.getByRole('button', { name: '목차' }))
    const target = await screen.findByRole('button', { name: /자기소개/ })
    await user.click(target)
    expect(onSelect).toHaveBeenCalledWith('q1')
  })
})
