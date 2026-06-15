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
  content: {
    verbalComment: {
      positive: '세대별 GC 개념을 정확히 설명했어요.',
      negative: null,
      suggestion: null,
    },
    accuracyIssues: [],
    coaching: null,
  },
  delivery: null,
  overallComment: null,
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

const buildDelivery = (fillerWordCount: number | null): NonNullable<TimestampFeedback['delivery']> => ({
  nonverbal: null,
  vocal: {
    fillerWords: '["음","어"]',
    fillerWordCount,
    speechPace: 'FAST',
    toneConfidenceLevel: 'GOOD',
    emotionLabel: 'CONFIDENT',
    vocalComment: null,
  },
  attitudeComment: null,
})

describe('FeedbackPanel', () => {
  it('Content/Delivery 2탭 노출', () => {
    renderPanel({ ...baseFeedback, delivery: buildDelivery(null) })

    expect(screen.getByRole('tab', { name: '내 답변은 어땠을까' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: '어떤 인상을 줬을까' })).toBeInTheDocument()
  })

  it('delivery 데이터 부재 → Delivery 탭 비활성', () => {
    renderPanel({ ...baseFeedback, delivery: null })

    const deliveryTab = screen.getByRole('tab', { name: '어떤 인상을 줬을까' })
    expect(deliveryTab).toBeDisabled()
  })

  it('Delivery 탭 클릭 → 습관어 감지 횟수 노출', async () => {
    const user = userEvent.setup()
    renderPanel({ ...baseFeedback, delivery: buildDelivery(3) })

    await user.click(screen.getByRole('tab', { name: '어떤 인상을 줬을까' }))
    expect(await screen.findByText('습관어가 3회 감지됐어요')).toBeInTheDocument()
  })

  it('내 답변 영역 습관어 감지 횟수 — delivery.vocal.fillerWordCount 기반', () => {
    renderPanel({ ...baseFeedback, delivery: buildDelivery(5) })

    expect(screen.getByText('습관어 5회 감지')).toBeInTheDocument()
  })

  it('fillerWordCount === 0 → 내 답변 영역 습관어 표시 부재', () => {
    renderPanel({ ...baseFeedback, delivery: buildDelivery(0) })

    expect(screen.queryByText(/습관어 \d+회 감지/)).not.toBeInTheDocument()
  })

  it('차원 점수 카드(1~5점) 부재 — 루브릭 제거 확인', () => {
    renderPanel(baseFeedback)

    expect(screen.queryByText('3점')).not.toBeInTheDocument()
    expect(screen.queryByText('기술 피드백')).not.toBeInTheDocument()
    expect(screen.queryByText('개념 정확도')).not.toBeInTheDocument()
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
})
