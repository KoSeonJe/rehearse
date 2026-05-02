// @vitest-environment node
import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import ContentTab from '@/components/feedback/content-tab'

describe('ContentTab', () => {
  it('shows timestamp technical rubric feedback for the answer', () => {
    const html = renderToStaticMarkup(
      <ContentTab
        technicalFeedback={{
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
        }}
      />,
    )

    expect(html).toContain('기술 피드백')
    expect(html).toContain('conceptual_accuracy')
    expect(html).toContain('3점')
    expect(html).toContain('세대별 GC 구조를 언급해 개념 정확도가 좋습니다.')
    expect(html).toContain('young 영역과 old 영역을 나눠 관리')
    expect(html).not.toContain('내용 피드백은 코치 노트에서 확인하세요')
  })

  it('shows a pending state when technical feedback is not ready', () => {
    const html = renderToStaticMarkup(<ContentTab technicalFeedback={null} />)

    expect(html).toContain('기술 피드백은 아직 준비 중입니다')
  })
})
