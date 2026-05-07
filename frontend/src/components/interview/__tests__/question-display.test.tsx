import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QuestionDisplay } from '@/components/interview/question-display'
import type { FollowUpResponse, Question } from '@/types/interview'

const BASE_QUESTION: Question = {
  id: 1,
  content: 'Spring Boot의 IoC 컨테이너에 대해 설명해주세요.',
  ttsContent: null,
  category: 'CS_FUNDAMENTAL',
  order: 0,
}

const BASE_FOLLOW_UP: FollowUpResponse = {
  questionId: 2,
  question: '실무 적용 사례를 들어주실 수 있나요?',
  ttsQuestion: null,
  reason: '경험 기반 심화',
  type: 'DEEP_DIVE',
  skip: false,
  followUpExhausted: false,
}

describe('QuestionDisplay', () => {
  it('정상 type(DEEP_DIVE)에 매핑된 한국어 라벨(심화) 표시', () => {
    const followUp: FollowUpResponse = { ...BASE_FOLLOW_UP, type: 'DEEP_DIVE' }
    render(
      <QuestionDisplay
        question={BASE_QUESTION}
        currentIndex={0}
        totalCount={3}
        followUp={followUp}
      />,
    )

    expect(screen.getByText('심화')).toBeInTheDocument()
  })

  it('알 수 없는 type 수신 시 fallback 라벨(안내) 표시 + 본문 정상 노출', () => {
    const followUp = {
      ...BASE_FOLLOW_UP,
      type: 'UNKNOWN_TYPE_FROM_BE' as FollowUpResponse['type'],
    }
    render(
      <QuestionDisplay
        question={BASE_QUESTION}
        currentIndex={0}
        totalCount={3}
        followUp={followUp}
      />,
    )

    expect(screen.getByText('안내')).toBeInTheDocument()
    expect(screen.queryByText('UNKNOWN_TYPE_FROM_BE')).not.toBeInTheDocument()
    expect(screen.getByText('실무 적용 사례를 들어주실 수 있나요?')).toBeInTheDocument()
  })

  it('isFollowUpLoading 시 로딩 안내 노출 + 후속 질문 카드 미노출', () => {
    render(
      <QuestionDisplay
        question={BASE_QUESTION}
        currentIndex={0}
        totalCount={3}
        followUp={BASE_FOLLOW_UP}
        isFollowUpLoading
      />,
    )

    expect(screen.getByText('질문을 생각하고 있습니다...')).toBeInTheDocument()
    expect(screen.queryByText('심화')).not.toBeInTheDocument()
  })
})
