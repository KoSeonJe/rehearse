import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import ContentTab from '@/components/feedback/content-tab'
import type { ContentFeedback } from '@/types/interview'

const buildContent = (overrides?: Partial<ContentFeedback>): ContentFeedback => ({
  verbalComment: {
    positive: '핵심 개념을 정확히 짚었어요.',
    negative: '예시가 부족했어요.',
    suggestion: '구체적인 경험을 덧붙여보세요.',
  },
  accuracyIssues: [{ claim: 'GC는 동기적으로 동작한다', correction: '대부분 비동기로 동작한다' }],
  coaching: { structure: 'STAR 구조로 정리하면 좋아요.', improvement: '수치를 추가하세요.' },
  ...overrides,
})

describe('ContentTab', () => {
  it('content === null → "AI가 분석하고 있어요" Empty 노출', () => {
    render(<ContentTab content={null} />)
    expect(screen.getByText('AI가 분석하고 있어요')).toBeInTheDocument()
  })

  it('모든 섹션 비어있음 → "내용 분석 정보가 없습니다" 노출', () => {
    render(
      <ContentTab
        content={{
          verbalComment: { positive: null, negative: null, suggestion: null },
          accuracyIssues: [],
          coaching: { structure: null, improvement: null },
        }}
      />,
    )
    expect(screen.getByText('내용 분석 정보가 없습니다')).toBeInTheDocument()
  })

  it('verbalComment 정상 → 답변 내용 분석 섹션 + 코멘트 노출', () => {
    render(<ContentTab content={buildContent({ accuracyIssues: [], coaching: null })} />)
    expect(screen.getByText('답변 내용을 분석했어요')).toBeInTheDocument()
    expect(screen.getByText('핵심 개념을 정확히 짚었어요.')).toBeInTheDocument()
    expect(screen.getByText('예시가 부족했어요.')).toBeInTheDocument()
  })

  it('accuracyIssues 존재 → 틀린 내용 섹션 + claim/correction 노출', () => {
    render(
      <ContentTab
        content={buildContent({
          verbalComment: null,
          coaching: null,
        })}
      />,
    )
    expect(screen.getByText('틀린 내용이 있었어요')).toBeInTheDocument()
    expect(screen.getByText('GC는 동기적으로 동작한다')).toBeInTheDocument()
    expect(screen.getByText('대부분 비동기로 동작한다')).toBeInTheDocument()
  })

  it('coaching 존재 → 코칭 섹션 + structure/improvement 노출', () => {
    render(
      <ContentTab
        content={buildContent({
          verbalComment: null,
          accuracyIssues: [],
        })}
      />,
    )
    expect(screen.getByText('다음엔 이렇게 해보세요')).toBeInTheDocument()
    expect(screen.getByText('STAR 구조로 정리하면 좋아요.')).toBeInTheDocument()
    expect(screen.getByText('수치를 추가하세요.')).toBeInTheDocument()
  })

  it('차원 점수 카드(1~5점) 부재 — 루브릭 제거 확인', () => {
    render(<ContentTab content={buildContent()} />)
    expect(screen.queryByText('3점')).not.toBeInTheDocument()
    expect(screen.queryByText('개념 정확도')).not.toBeInTheDocument()
    expect(screen.queryByText('비언어 평가')).not.toBeInTheDocument()
  })
})
