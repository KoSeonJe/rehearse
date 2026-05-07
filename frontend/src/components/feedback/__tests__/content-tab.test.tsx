// @vitest-environment node
import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import ContentTab from '@/components/feedback/content-tab'
import type { TechnicalFeedback } from '@/types/interview'

const buildFeedback = (overrides: Partial<TechnicalFeedback>): TechnicalFeedback => ({
  perspective: 'TECHNICAL',
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
  it('TECHNICAL perspective → "기술 피드백" 라벨 + dimensions 노출', () => {
    const html = renderToStaticMarkup(
      <ContentTab technicalFeedback={buildFeedback({ perspective: 'TECHNICAL' })} />,
    )

    expect(html).toContain('기술 피드백')
    expect(html).not.toContain('경험 평가')
    expect(html).toContain('conceptual_accuracy')
    expect(html).toContain('3점')
    expect(html).toContain('세대별 GC 구조를 언급해 개념 정확도가 좋습니다.')
    expect(html).toContain('young 영역과 old 영역을 나눠 관리')
  })

  it('EXPERIENCE perspective → "경험 평가" 라벨 + dimensions 노출', () => {
    const html = renderToStaticMarkup(
      <ContentTab
        technicalFeedback={buildFeedback({
          perspective: 'EXPERIENCE',
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

    expect(html).toContain('경험 평가')
    expect(html).not.toContain('>기술 피드백<')
    expect(html).toContain('experience_concreteness')
    expect(html).toContain('1점')
    expect(html).toContain('구체 수치 / 결과가 부족합니다.')
  })

  it('BEHAVIORAL perspective → fallback "준비 중" 노출', () => {
    const html = renderToStaticMarkup(
      <ContentTab
        technicalFeedback={buildFeedback({
          perspective: 'BEHAVIORAL',
          dimensions: [
            {
              dimension: 'collaboration',
              score: 2,
              observation: '협업 사례 묘사가 추상적입니다.',
              evidenceQuote: null,
            },
          ],
        })}
      />,
    )

    expect(html).toContain('기술 피드백은 아직 준비 중입니다')
    expect(html).not.toContain('collaboration')
  })

  it('perspective null → fallback "준비 중" 노출', () => {
    const html = renderToStaticMarkup(
      <ContentTab technicalFeedback={buildFeedback({ perspective: null })} />,
    )

    expect(html).toContain('기술 피드백은 아직 준비 중입니다')
    expect(html).not.toContain('conceptual_accuracy')
  })

  it('technicalFeedback === null → fallback "준비 중" 노출 (회귀)', () => {
    const html = renderToStaticMarkup(<ContentTab technicalFeedback={null} />)

    expect(html).toContain('기술 피드백은 아직 준비 중입니다')
  })
})
