import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ReviewListFilterBar } from '@/components/review/review-list-filter-bar'
import { POSITION_LABELS, POSITION_INTERVIEW_TYPES } from '@/constants/interview-labels'
import type { BookmarkStatus } from '@/types/review-bookmark'
import type { Position, InterviewType } from '@/types/interview'

const DEFAULT_PROPS = {
  status: 'all' as BookmarkStatus,
  onStatusChange: vi.fn(),
  positionFilter: 'ALL' as Position | 'ALL',
  onPositionChange: vi.fn(),
  interviewTypeFilter: 'ALL' as InterviewType | 'ALL',
  onInterviewTypeChange: vi.fn(),
}

describe('ReviewListFilterBar', () => {
  beforeEach(() => {
    DEFAULT_PROPS.onStatusChange.mockReset()
    DEFAULT_PROPS.onPositionChange.mockReset()
    DEFAULT_PROPS.onInterviewTypeChange.mockReset()
  })

  it('renders 3 status chips: 전체, 복습중, 복습완료', () => {
    render(<ReviewListFilterBar {...DEFAULT_PROPS} />)

    expect(screen.getByRole('button', { name: '전체' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '복습중' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '복습완료' })).toBeInTheDocument()
  })

  it('the chip matching current status has aria-pressed true', () => {
    render(<ReviewListFilterBar {...DEFAULT_PROPS} status="in_progress" />)

    expect(screen.getByRole('button', { name: '복습중' })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByRole('button', { name: '전체' })).toHaveAttribute('aria-pressed', 'false')
    expect(screen.getByRole('button', { name: '복습완료' })).toHaveAttribute('aria-pressed', 'false')
  })

  it('clicking a different chip calls onStatusChange with the correct value', async () => {
    const user = userEvent.setup()
    render(<ReviewListFilterBar {...DEFAULT_PROPS} status="all" />)

    await user.click(screen.getByRole('button', { name: '복습완료' }))

    expect(DEFAULT_PROPS.onStatusChange).toHaveBeenCalledOnce()
    expect(DEFAULT_PROPS.onStatusChange).toHaveBeenCalledWith('resolved')
  })

  it('position select has 전체 직무 option plus one per position', () => {
    render(<ReviewListFilterBar {...DEFAULT_PROPS} />)

    const select = screen.getByRole('combobox', { name: '직무 필터' })
    const options = Array.from(select.querySelectorAll('option'))

    const positionKeys = Object.keys(POSITION_LABELS)
    expect(options).toHaveLength(1 + positionKeys.length)
    expect(options[0]).toHaveValue('ALL')
    expect(options[0]).toHaveTextContent('전체 직무')
  })

  it('category select is disabled when no position is selected', () => {
    render(<ReviewListFilterBar {...DEFAULT_PROPS} positionFilter="ALL" />)

    const select = screen.getByRole('combobox', { name: '카테고리 필터' })
    expect(select).toBeDisabled()
  })

  it('category select shows interview types for the selected position', () => {
    render(<ReviewListFilterBar {...DEFAULT_PROPS} positionFilter="BACKEND" />)

    const select = screen.getByRole('combobox', { name: '카테고리 필터' })
    expect(select).not.toBeDisabled()

    const options = Array.from(select.querySelectorAll('option'))
    const backendTypes = POSITION_INTERVIEW_TYPES.BACKEND
    expect(options).toHaveLength(1 + backendTypes.length)

    backendTypes.forEach((type) => {
      expect(options.some((o) => o.value === type)).toBe(true)
    })
  })

  it('changing position calls onPositionChange', async () => {
    const user = userEvent.setup()
    render(<ReviewListFilterBar {...DEFAULT_PROPS} />)

    const select = screen.getByRole('combobox', { name: '직무 필터' })
    await user.selectOptions(select, 'BACKEND')

    expect(DEFAULT_PROPS.onPositionChange).toHaveBeenCalledOnce()
    expect(DEFAULT_PROPS.onPositionChange).toHaveBeenCalledWith('BACKEND')
  })

  it('changing category calls onInterviewTypeChange', async () => {
    const user = userEvent.setup()
    render(<ReviewListFilterBar {...DEFAULT_PROPS} positionFilter="BACKEND" />)

    const select = screen.getByRole('combobox', { name: '카테고리 필터' })
    await user.selectOptions(select, 'CS_FUNDAMENTAL')

    expect(DEFAULT_PROPS.onInterviewTypeChange).toHaveBeenCalledOnce()
    expect(DEFAULT_PROPS.onInterviewTypeChange).toHaveBeenCalledWith('CS_FUNDAMENTAL')
  })
})
