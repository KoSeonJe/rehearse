import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import ContentTab from '@/components/feedback/content-tab'
import type { NonverbalFeedback, TechnicalFeedback } from '@/types/interview'

const buildTechnical = (overrides: Partial<TechnicalFeedback>): TechnicalFeedback => ({
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

const buildNonverbal = (overrides?: Partial<NonverbalFeedback>): NonverbalFeedback => ({
  rubricId: 'nonverbal-v1',
  dimensions: [
    {
      dimension: 'fluency',
      score: 2,
      observation: '발화가 끊김 없이 이어졌습니다.',
      evidenceQuote: '단어 선택이 자연스러웠습니다',
    },
    {
      dimension: 'confidence_tone',
      score: 3,
      observation: '톤이 일관되어 자신감이 느껴졌습니다.',
      evidenceQuote: null,
    },
    {
      dimension: 'eye_contact_posture',
      score: 2,
      observation: '시선이 카메라를 향했습니다.',
      evidenceQuote: null,
    },
  ],
  ...overrides,
})

describe('ContentTab', () => {
  it('TECHNICAL 카테고리 → "기술 피드백" 라벨 + dimensions 노출', () => {
    render(
      <ContentTab
        technicalFeedback={buildTechnical({ rubricCategory: 'TECHNICAL' })}
        nonverbalFeedback={null}
        questionType="TECH_MAIN"
      />,
    )

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
        technicalFeedback={buildTechnical({
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
        nonverbalFeedback={null}
        questionType="RESUME_PLAYGROUND"
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
        technicalFeedback={buildTechnical({
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
        nonverbalFeedback={null}
        questionType="BEHAVIORAL_MAIN"
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

  it('rubricCategory null → verbal fallback 노출', () => {
    render(
      <ContentTab
        technicalFeedback={buildTechnical({ rubricCategory: null })}
        nonverbalFeedback={null}
        questionType="TECH_MAIN"
      />,
    )

    expect(screen.getByText('해당 턴은 평가 대상이 아닙니다.')).toBeInTheDocument()
    expect(screen.queryByText('conceptual_accuracy')).not.toBeInTheDocument()
  })

  it('technicalFeedback === null → verbal fallback 노출 (회귀)', () => {
    render(
      <ContentTab
        technicalFeedback={null}
        nonverbalFeedback={null}
        questionType="MAIN"
      />,
    )

    expect(screen.getByText('해당 턴은 평가 대상이 아닙니다.')).toBeInTheDocument()
  })

  it('questionType="RESUME_OPENER" → OPENER 안내 카드만 노출 (비언어 영역 부재)', () => {
    render(
      <ContentTab
        technicalFeedback={null}
        nonverbalFeedback={buildNonverbal()}
        questionType="RESUME_OPENER"
      />,
    )

    expect(screen.getByText('이 단계는 채점 대상이 아닙니다.')).toBeInTheDocument()
    expect(
      screen.getByText('면접 도입 단계 답변은 점수 채점에 사용되지 않습니다.'),
    ).toBeInTheDocument()
    expect(screen.queryByText('비언어 평가')).not.toBeInTheDocument()
  })

  it('questionType="RESUME_PLAYGROUND" + 양쪽 null → verbal fallback + 비언어 분석 실패 동시 노출', () => {
    render(
      <ContentTab
        technicalFeedback={null}
        nonverbalFeedback={null}
        questionType="RESUME_PLAYGROUND"
      />,
    )

    expect(screen.getByText('해당 턴은 평가 대상이 아닙니다.')).toBeInTheDocument()
    expect(screen.queryByText('이 단계는 채점 대상이 아닙니다.')).not.toBeInTheDocument()
    expect(screen.getByText('비언어 평가')).toBeInTheDocument()
    expect(screen.getByText('분석 실패 — 점수 없음')).toBeInTheDocument()
  })

  it('verbal + 비언어 정상 → 카드 합산 4개 + tab role 부재', () => {
    render(
      <ContentTab
        technicalFeedback={buildTechnical({ rubricCategory: 'TECHNICAL' })}
        nonverbalFeedback={buildNonverbal()}
        questionType="TECH_MAIN"
      />,
    )

    expect(screen.getByText('기술 피드백')).toBeInTheDocument()
    expect(screen.getByText('비언어 평가')).toBeInTheDocument()
    expect(screen.getByText('conceptual_accuracy')).toBeInTheDocument()
    expect(screen.getByText('fluency')).toBeInTheDocument()
    expect(screen.getByText('confidence_tone')).toBeInTheDocument()
    expect(screen.getByText('eye_contact_posture')).toBeInTheDocument()
    expect(screen.queryByRole('tab')).not.toBeInTheDocument()
    expect(screen.queryByText('긍정')).not.toBeInTheDocument()
    expect(screen.queryByText('이번 답변 비언어 분석 일부 실패')).not.toBeInTheDocument()
  })

  it('비언어 dimensions.length === 2 → 부분 카드 2개 + "일부 실패" 안내', () => {
    render(
      <ContentTab
        technicalFeedback={buildTechnical({ rubricCategory: 'TECHNICAL' })}
        nonverbalFeedback={buildNonverbal({
          dimensions: [
            {
              dimension: 'fluency',
              score: 2,
              observation: '발화 매끄러움.',
              evidenceQuote: null,
            },
            {
              dimension: 'confidence_tone',
              score: 3,
              observation: '톤 안정.',
              evidenceQuote: null,
            },
          ],
        })}
        questionType="TECH_MAIN"
      />,
    )

    expect(screen.getByText('비언어 평가')).toBeInTheDocument()
    expect(screen.getByText('fluency')).toBeInTheDocument()
    expect(screen.getByText('confidence_tone')).toBeInTheDocument()
    expect(screen.queryByText('eye_contact_posture')).not.toBeInTheDocument()
    expect(screen.getByText('이번 답변 비언어 분석 일부 실패')).toBeInTheDocument()
  })

  it('비언어 nonverbalFeedback === null → "분석 실패 — 점수 없음" Empty 카드', () => {
    render(
      <ContentTab
        technicalFeedback={buildTechnical({ rubricCategory: 'TECHNICAL' })}
        nonverbalFeedback={null}
        questionType="TECH_MAIN"
      />,
    )

    expect(screen.getByText('비언어 평가')).toBeInTheDocument()
    expect(screen.getByText('분석 실패 — 점수 없음')).toBeInTheDocument()
    expect(screen.queryByText('이번 답변 비언어 분석 일부 실패')).not.toBeInTheDocument()
  })

  it('questionType="RESUME_INTERROGATION" + technicalFeedback 정상 → dimension 카드 회귀', () => {
    render(
      <ContentTab
        technicalFeedback={buildTechnical({
          rubricCategory: 'EXPERIENCE',
          rubricId: 'resume-rubric',
          dimensions: [
            {
              dimension: 'technical_depth',
              score: 4,
              observation: '아키텍처 트레이드오프를 구체적으로 설명했습니다.',
              evidenceQuote: 'CQRS 적용으로 read 부하 분리',
            },
          ],
        })}
        nonverbalFeedback={null}
        questionType="RESUME_INTERROGATION"
      />,
    )

    expect(screen.getByText('경험 평가')).toBeInTheDocument()
    expect(screen.getByText('technical_depth')).toBeInTheDocument()
    expect(screen.getByText('4점')).toBeInTheDocument()
    expect(
      screen.getByText('아키텍처 트레이드오프를 구체적으로 설명했습니다.'),
    ).toBeInTheDocument()
    expect(screen.queryByText('이 단계는 채점 대상이 아닙니다.')).not.toBeInTheDocument()
  })
})
