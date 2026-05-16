import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import RubricDimensionCard from '@/components/feedback/rubric-dimension-card'
import type { TechnicalDimensionFeedback } from '@/types/interview'

const buildDimension = (
  overrides: Partial<TechnicalDimensionFeedback>,
): TechnicalDimensionFeedback => ({
  dimension: 'fluency',
  score: 2,
  observation: '발화가 매끄럽게 이어졌습니다.',
  evidenceQuote: '단어 선택이 자연스럽습니다',
  ...overrides,
})

describe('RubricDimensionCard', () => {
  it('4개 필드 (dimension / score / observation / evidenceQuote) 정확 렌더', () => {
    render(<RubricDimensionCard dimension={buildDimension({})} />)

    expect(screen.getByText('fluency')).toBeInTheDocument()
    expect(screen.getByText('2점')).toBeInTheDocument()
    expect(screen.getByText('발화가 매끄럽게 이어졌습니다.')).toBeInTheDocument()
    expect(screen.getByText('단어 선택이 자연스럽습니다')).toBeInTheDocument()
  })

  it('score === null → "평가 제외" 노출', () => {
    render(<RubricDimensionCard dimension={buildDimension({ score: null })} />)

    expect(screen.getByText('평가 제외')).toBeInTheDocument()
  })

  it('observation === null → observation 영역 부재', () => {
    render(
      <RubricDimensionCard
        dimension={buildDimension({ observation: null, evidenceQuote: null })}
      />,
    )

    expect(screen.queryByText('발화가 매끄럽게 이어졌습니다.')).not.toBeInTheDocument()
  })

  it('evidenceQuote === null → blockquote 영역 부재', () => {
    render(<RubricDimensionCard dimension={buildDimension({ evidenceQuote: null })} />)

    expect(screen.queryByText('단어 선택이 자연스럽습니다')).not.toBeInTheDocument()
    expect(screen.getByText('발화가 매끄럽게 이어졌습니다.')).toBeInTheDocument()
  })
})
