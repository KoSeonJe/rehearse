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
  status: 'OK',
  ...overrides,
})

describe('RubricDimensionCard', () => {
  it('4개 필드 (dimension / score / observation / evidenceQuote) 정확 렌더', () => {
    render(<RubricDimensionCard dimension={buildDimension({})} />)

    expect(screen.getByText('유창함')).toBeInTheDocument()
    expect(screen.queryByText('fluency')).not.toBeInTheDocument()
    expect(screen.getByText('2점')).toBeInTheDocument()
    expect(screen.getByText('발화가 매끄럽게 이어졌습니다.')).toBeInTheDocument()
    expect(screen.getByText('단어 선택이 자연스럽습니다')).toBeInTheDocument()
  })

  it('dimension ref 가 영문 키여도 친화 한국어 라벨로 노출', () => {
    render(
      <RubricDimensionCard dimension={buildDimension({ dimension: 'problem_framing' })} />,
    )

    expect(screen.getByText('문제 정의')).toBeInTheDocument()
    expect(screen.queryByText('problem_framing')).not.toBeInTheDocument()
  })

  it('미매핑 dimension 키는 raw 문자열을 fallback 으로 노출', () => {
    render(
      <RubricDimensionCard dimension={buildDimension({ dimension: 'unknown_dim' })} />,
    )

    expect(screen.getByText('unknown_dim')).toBeInTheDocument()
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
