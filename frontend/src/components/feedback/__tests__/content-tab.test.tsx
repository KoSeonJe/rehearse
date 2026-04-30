// @vitest-environment node
import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import ContentTab from '@/components/feedback/content-tab'

describe('ContentTab', () => {
  it('shows the plan-13 session-feedback handoff state instead of timestamp Lambda content', () => {
    const html = renderToStaticMarkup(<ContentTab />)

    expect(html).toContain('내용 피드백은 종합 피드백에서 확인하세요')
    expect(html).not.toContain('답변 내용을 분석했어요')
    expect(html).not.toContain('틀린 내용이 있었어요')
    expect(html).not.toContain('다음엔 이렇게 해보세요')
  })
})
