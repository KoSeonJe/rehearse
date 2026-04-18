import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import BookmarkToggleButton from '@/components/feedback/bookmark-toggle-button'

// Mutable state shared between the mock factory and individual tests
const mockState = {
  createPending: false,
  deletePending: false,
}

const mockCreateMutate = vi.fn()
const mockDeleteMutate = vi.fn()
const mockNavigate = vi.fn()

vi.mock('react-router-dom', () => ({
  useNavigate: () => mockNavigate,
}))

vi.mock('@/hooks/use-review-bookmarks', () => ({
  useCreateBookmark: () => ({
    mutate: mockCreateMutate,
    isPending: mockState.createPending,
  }),
  useDeleteBookmark: () => ({
    mutate: mockDeleteMutate,
    isPending: mockState.deletePending,
  }),
}))

vi.mock('sonner', () => ({
  toast: vi.fn(),
}))

const BASE_PROPS = {
  timestampFeedbackId: 42,
  interviewId: 7,
  bookmarkId: undefined as number | undefined,
}

describe('BookmarkToggleButton', () => {
  beforeEach(() => {
    mockCreateMutate.mockReset()
    mockDeleteMutate.mockReset()
    mockNavigate.mockReset()
    mockState.createPending = false
    mockState.deletePending = false

    // jsdom does not implement window.matchMedia — stub it
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

  it('renders with aria-pressed false and 담기 aria-label when not bookmarked', () => {
    render(<BookmarkToggleButton {...BASE_PROPS} bookmarkId={undefined} />)

    const button = screen.getByRole('button')
    expect(button).toHaveAttribute('aria-pressed', 'false')
    expect(button).toHaveAttribute('aria-label', '복습 목록에 담기')
  })

  it('renders with aria-pressed true and 제거 aria-label when bookmarked', () => {
    render(<BookmarkToggleButton {...BASE_PROPS} bookmarkId={99} />)

    const button = screen.getByRole('button')
    expect(button).toHaveAttribute('aria-pressed', 'true')
    expect(button).toHaveAttribute('aria-label', '복습 목록에서 제거')
  })

  it('calls create mutation with timestampFeedbackId when clicked while unbookmarked', async () => {
    const user = userEvent.setup()
    render(<BookmarkToggleButton {...BASE_PROPS} bookmarkId={undefined} />)

    await user.click(screen.getByRole('button'))

    expect(mockCreateMutate).toHaveBeenCalledOnce()
    expect(mockCreateMutate).toHaveBeenCalledWith(
      expect.objectContaining({ timestampFeedbackId: 42 }),
      expect.any(Object),
    )
    expect(mockDeleteMutate).not.toHaveBeenCalled()
  })

  it('calls delete mutation with bookmarkId when clicked while bookmarked', async () => {
    const user = userEvent.setup()
    render(<BookmarkToggleButton {...BASE_PROPS} bookmarkId={99} />)

    await user.click(screen.getByRole('button'))

    expect(mockDeleteMutate).toHaveBeenCalledOnce()
    expect(mockDeleteMutate).toHaveBeenCalledWith(
      expect.objectContaining({ bookmarkId: 99, timestampFeedbackId: 42 }),
      expect.any(Object),
    )
    expect(mockCreateMutate).not.toHaveBeenCalled()
  })

  it('button is disabled and mutations are not called when create is pending', async () => {
    mockState.createPending = true
    const user = userEvent.setup()
    render(<BookmarkToggleButton {...BASE_PROPS} bookmarkId={undefined} />)

    const button = screen.getByRole('button')
    expect(button).toBeDisabled()

    await user.click(button)

    expect(mockCreateMutate).not.toHaveBeenCalled()
    expect(mockDeleteMutate).not.toHaveBeenCalled()
  })
})
