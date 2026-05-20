import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { SessionFeedbackModal } from '@/components/feedback/session-feedback-modal'
import type { SessionFeedbackData } from '@/types/session-feedback'

const baseData: SessionFeedbackData = {
  id: 1,
  interviewId: 100,
  status: 'COMPLETE',
  overall: {
    dimensionScores: {
      problem_framing: 4,
      confidence_tone: 3,
      composure: 5,
    },
    levelAssessment: 'Mid 수준',
    narrative: '전반적으로 안정적인 답변이었습니다.',
    coverage: '5문항 중 5문항 답변',
  },
  strengths: [
    {
      dimension: 'confidence_tone',
      observation: '자신감 있는 어조를 유지했습니다.',
      whyMatters: '신뢰도 상승',
    },
  ],
  gaps: [
    {
      dimension: 'eye_contact_posture',
      observation: '시선 처리가 불안정합니다.',
      levelGap: '−0.5단계',
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

const renderModal = (data: SessionFeedbackData) =>
  render(
    <SessionFeedbackModal
      isOpen
      onClose={() => {}}
      data={data}
      isLoading={false}
      isError={false}
    />,
  )

describe('SessionFeedbackModal — dimension 라벨 친화 표기', () => {
  it('분야별 점수 영역에서 영어 ref 대신 한국어 라벨을 노출', () => {
    renderModal(baseData)
    expect(screen.getByText('문제 정의')).toBeInTheDocument()
    expect(screen.getAllByText('자신감').length).toBeGreaterThanOrEqual(1)
    expect(screen.getByText('차분함')).toBeInTheDocument()
    expect(screen.queryByText('problem_framing')).not.toBeInTheDocument()
    expect(screen.queryByText('confidence_tone')).not.toBeInTheDocument()
    expect(screen.queryByText('composure')).not.toBeInTheDocument()
  })

  it('잘한 점 (StrengthItem) 의 dimension 도 한국어로 노출', () => {
    renderModal(baseData)
    // 점수 row + Strength 카드 양쪽에서 노출 → 2개 이상
    expect(screen.getAllByText('자신감').length).toBeGreaterThanOrEqual(2)
    expect(screen.queryByText('confidence_tone')).not.toBeInTheDocument()
  })

  it('개선할 점 (GapItem) 의 dimension 도 한국어로 노출', () => {
    renderModal(baseData)
    expect(screen.getByText('시선')).toBeInTheDocument()
    expect(screen.queryByText('eye_contact_posture')).not.toBeInTheDocument()
  })

  it('알 수 없는 dimension 키는 fallback 으로 원본 노출', () => {
    const dataWithUnknown: SessionFeedbackData = {
      ...baseData,
      overall: {
        ...baseData.overall!,
        dimensionScores: { custom_unknown_dim: 2 },
      },
      strengths: null,
      gaps: null,
    }
    renderModal(dataWithUnknown)
    expect(screen.getByText('custom_unknown_dim')).toBeInTheDocument()
  })
})
