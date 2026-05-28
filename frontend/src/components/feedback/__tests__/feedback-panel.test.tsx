import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { FeedbackPanel } from '@/components/feedback/feedback-panel'
import type { QuestionWithAnswer, TimestampFeedback } from '@/types/interview'

const baseFeedback: TimestampFeedback = {
  id: 1,
  questionId: 10,
  questionType: 'TECH_MAIN',
  questionText: 'GC 동작 원리를 설명해보세요.',
  bestAnswer: null,
  startMs: 0,
  endMs: 30000,
  transcript: '세대별 GC 가 동작합니다.',
  technicalFeedback: {
    rubricCategory: 'TECHNICAL',
    rubricId: 'technical-v1',
    levelFlag: 'MID_EXPECTATION_MET',
    dimensions: [
      {
        dimension: 'conceptual_accuracy',
        score: 3,
        observation: '세대별 GC 개념을 정확히 설명했습니다.',
        evidenceQuote: '세대별 GC 가 동작합니다',
        status: 'OK',
      },
    ],
  },
  nonverbalFeedback: null,
  fillerWordCount: null,
  isAnalyzed: true,
}

const baseQuestion: QuestionWithAnswer = {
  questionId: 10,
  questionType: 'TECH_MAIN',
  questionText: 'GC 동작 원리를 설명해보세요.',
  bestAnswer: null,
  startMs: 0,
  endMs: 30000,
}

const renderPanel = (feedback: TimestampFeedback, onSeek: (ms: number) => void = () => {}) => {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <MemoryRouter>
      <QueryClientProvider client={client}>
        <FeedbackPanel
          feedbacks={[feedback]}
          questions={[baseQuestion]}
          selectedFeedbackId={feedback.id}
          onSeek={onSeek}
          interviewId={1}
          bookmarkIdsByTsfId={new Map()}
        />
      </QueryClientProvider>
    </MemoryRouter>,
  )
}

describe('FeedbackPanel', () => {
  it('fillerWordCount === 3 → "습관어 3회 감지" 노출', () => {
    renderPanel({ ...baseFeedback, fillerWordCount: 3 })

    expect(screen.getByText('습관어 3회 감지')).toBeInTheDocument()
  })

  it('fillerWordCount === 0 → 필러 배지 부재', () => {
    renderPanel({ ...baseFeedback, fillerWordCount: 0 })

    expect(screen.queryByText(/습관어.*감지/)).not.toBeInTheDocument()
  })

  it('fillerWordCount === null → 필러 배지 부재', () => {
    renderPanel({ ...baseFeedback, fillerWordCount: null })

    expect(screen.queryByText(/습관어.*감지/)).not.toBeInTheDocument()
  })

  it('Tab 네비게이션 부재 (role="tab" 0개)', () => {
    renderPanel(baseFeedback)

    expect(screen.queryAllByRole('tab')).toHaveLength(0)
  })

  it('ContentTab 단일 렌더 → verbal 차원 카드 노출', () => {
    renderPanel(baseFeedback)

    expect(screen.getByText('기술 피드백')).toBeInTheDocument()
    expect(screen.getByText('개념 정확도')).toBeInTheDocument()
    expect(screen.queryByText('conceptual_accuracy')).not.toBeInTheDocument()
    expect(screen.getByText('3점')).toBeInTheDocument()
  })

  it('타임스탬프 버튼 클릭 / 키보드 (Enter / Space) 로 seek 동작', async () => {
    const onSeek = vi.fn()
    renderPanel(baseFeedback, onSeek)
    const user = userEvent.setup()

    const seekButton = screen.getByRole('button', { name: /구간으로 이동/ })
    expect(seekButton.tagName).toBe('BUTTON')

    await user.click(seekButton)
    expect(onSeek).toHaveBeenCalledWith(baseFeedback.startMs)

    onSeek.mockClear()
    seekButton.focus()
    await user.keyboard('{Enter}')
    expect(onSeek).toHaveBeenCalledWith(baseFeedback.startMs)

    onSeek.mockClear()
    await user.keyboard(' ')
    expect(onSeek).toHaveBeenCalledWith(baseFeedback.startMs)
  })

  it('자유서술 텍스트 (긍정 / 부정 / 제안) 부재', () => {
    renderPanel({
      ...baseFeedback,
      nonverbalFeedback: {
        rubricId: 'nonverbal-v1',
        dimensions: [
          {
            dimension: 'fluency',
            score: 2,
            observation: '발화 매끄러움.',
            evidenceQuote: null,
            status: 'OK',
          },
        ],
      },
    })

    expect(screen.queryByText('잘한 점')).not.toBeInTheDocument()
    expect(screen.queryByText('아쉬운 점')).not.toBeInTheDocument()
    expect(screen.queryByText('이렇게 해보세요')).not.toBeInTheDocument()
  })
})
