import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { SessionFeedbackModal } from '@/components/feedback/session-feedback-modal'
import type { SessionFeedbackData } from '@/types/session-feedback'

const baseData: SessionFeedbackData = {
  id: 1,
  interviewId: 100,
  status: 'COMPLETE',
  overall: {
    levelAssessment: 'Mid 수준',
    narrative: '전반적으로 안정적인 답변이었습니다.',
    coverage: '5문항 중 5문항 답변',
  },
  strengths: [
    {
      observation: '자신감 있는 어조를 유지했습니다.',
      whyMatters: '신뢰도 상승',
    },
  ],
  gaps: [
    {
      observation: '시선 처리가 불안정합니다.',
      concreteAction: '카메라 렌즈를 의식적으로 응시',
    },
  ],
  delivery: null,
  weekPlan: null,
  coverage: null,
  deliveryRetryable: false,
  lastFailureReason: null,
  retryAttempts: 0,
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
}

const renderModal = (data: SessionFeedbackData, isOpen = true) =>
  render(
    <SessionFeedbackModal
      isOpen={isOpen}
      onClose={() => {}}
      data={data}
      isLoading={false}
      isError={false}
    />,
  )

describe('SessionFeedbackModal — 서술형 렌더', () => {
  it('isOpen 이면 코치 노트 모달을 노출', () => {
    renderModal(baseData)
    expect(screen.getByRole('dialog')).toBeInTheDocument()
    expect(screen.getByText('코치 노트')).toBeInTheDocument()
  })

  it('한줄 평가의 레벨/서술/커버리지를 노출', () => {
    renderModal(baseData)
    expect(screen.getByText('Mid 수준')).toBeInTheDocument()
    expect(screen.getByText('전반적으로 안정적인 답변이었습니다.')).toBeInTheDocument()
    expect(screen.getByText('5문항 중 5문항 답변')).toBeInTheDocument()
  })

  it('잘한 점을 제목 없이 observation 본문 + whyMatters 보조줄로 렌더', () => {
    renderModal(baseData)
    expect(screen.getByText('잘한 점')).toBeInTheDocument()
    expect(screen.getByText('자신감 있는 어조를 유지했습니다.')).toBeInTheDocument()
    expect(screen.getByText('신뢰도 상승')).toBeInTheDocument()
  })

  it('개선할 점을 제목 없이 observation 본문 + concreteAction 화살표줄로 렌더', () => {
    renderModal(baseData)
    expect(screen.getByText('개선할 점')).toBeInTheDocument()
    expect(screen.getByText('시선 처리가 불안정합니다.')).toBeInTheDocument()
    expect(screen.getByText('→ 카메라 렌즈를 의식적으로 응시')).toBeInTheDocument()
  })

  it('차원별 점수 섹션을 노출하지 않음', () => {
    renderModal(baseData)
    expect(screen.queryByText('이번 면접 분야별 점수')).not.toBeInTheDocument()
    expect(screen.queryByText('/5')).not.toBeInTheDocument()
  })

  it('isOpen 이 false 면 모달을 노출하지 않음', () => {
    renderModal(baseData, false)
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })
})
