import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import ContentTab from '@/components/feedback/content-tab'
import type { TechnicalFeedback } from '@/types/interview'

const buildFeedback = (overrides: Partial<TechnicalFeedback>): TechnicalFeedback => ({
  rubricCategory: 'TECHNICAL',
  rubricId: 'cs-v1',
  levelFlag: 'MID_EXPECTATION_MET',
  dimensions: [
    {
      dimension: 'conceptual_accuracy',
      score: 3,
      observation: '세대별 GC 구조를 언급해 개념 정확도가 좋습니다.',
      evidenceQuote: 'young 영역과 old 영역을 나눠 관리',
    },
  ],
  ...overrides,
})

describe('ContentTab', () => {
  it('TECHNICAL 카테고리 → "기술 피드백" 라벨 + dimensions 노출', () => {
    render(<ContentTab technicalFeedback={buildFeedback({ rubricCategory: 'TECHNICAL' })} />)

    expect(screen.getByText('기술 피드백')).toBeInTheDocument()
    expect(screen.queryByText('경험 평가')).not.toBeInTheDocument()
    expect(screen.getByText('conceptual_accuracy')).toBeInTheDocument()
    expect(screen.getByText('3점')).toBeInTheDocument()
    expect(
      screen.getByText('세대별 GC 구조를 언급해 개념 정확도가 좋습니다.'),
    ).toBeInTheDocument()
    expect(screen.getByText('young 영역과 old 영역을 나눠 관리')).toBeInTheDocument()
    expect(screen.queryByText('기술 피드백은 아직 준비 중입니다.')).not.toBeInTheDocument()
  })

  it('EXPERIENCE 카테고리 → "경험 평가" 라벨 + dimensions 노출', () => {
    render(
      <ContentTab
        technicalFeedback={buildFeedback({
          rubricCategory: 'EXPERIENCE',
          rubricId: 'resume-v1',
          dimensions: [
            {
              dimension: 'experience_concreteness',
              score: 1,
              observation: '구체 수치 / 결과가 부족합니다.',
              evidenceQuote: null,
            },
          ],
        })}
      />,
    )

    expect(screen.getByText('경험 평가')).toBeInTheDocument()
    expect(screen.queryByText('기술 피드백')).not.toBeInTheDocument()
    expect(screen.getByText('experience_concreteness')).toBeInTheDocument()
    expect(screen.getByText('1점')).toBeInTheDocument()
    expect(screen.getByText('구체 수치 / 결과가 부족합니다.')).toBeInTheDocument()
  })

  it('BEHAVIORAL 카테고리 → "경험/협업" 라벨 + dimensions 노출', () => {
    render(
      <ContentTab
        technicalFeedback={buildFeedback({
          rubricCategory: 'BEHAVIORAL',
          rubricId: 'experience-collaboration-rubric',
          dimensions: [
            {
              dimension: 'collaboration_awareness',
              score: 2,
              observation: '협업 사례 묘사가 추상적입니다.',
              evidenceQuote: '팀과 잘 맞췄어요',
            },
          ],
        })}
      />,
    )

    expect(screen.getByText('경험/협업')).toBeInTheDocument()
    expect(screen.queryByText('기술 피드백')).not.toBeInTheDocument()
    expect(screen.queryByText('경험 평가')).not.toBeInTheDocument()
    expect(screen.getByText('collaboration_awareness')).toBeInTheDocument()
    expect(screen.getByText('2점')).toBeInTheDocument()
    expect(screen.getByText('협업 사례 묘사가 추상적입니다.')).toBeInTheDocument()
    expect(screen.getByText('팀과 잘 맞췄어요')).toBeInTheDocument()
    expect(screen.queryByText('해당 턴은 평가 대상이 아닙니다.')).not.toBeInTheDocument()
    expect(screen.queryByText('경험/협업 피드백은 아직 준비 중입니다.')).not.toBeInTheDocument()
  })

  it('rubricCategory null → "해당 턴은 평가 대상이 아닙니다." fallback', () => {
    render(<ContentTab technicalFeedback={buildFeedback({ rubricCategory: null })} />)

    expect(screen.getByText('해당 턴은 평가 대상이 아닙니다.')).toBeInTheDocument()
    expect(screen.queryByText('conceptual_accuracy')).not.toBeInTheDocument()
  })

  it('technicalFeedback === null → "해당 턴은 평가 대상이 아닙니다." fallback (회귀)', () => {
    render(<ContentTab technicalFeedback={null} />)

    expect(screen.getByText('해당 턴은 평가 대상이 아닙니다.')).toBeInTheDocument()
  })
})
