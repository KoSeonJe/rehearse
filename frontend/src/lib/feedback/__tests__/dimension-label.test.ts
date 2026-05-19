import { describe, expect, it } from 'vitest'
import { getDimensionLabel } from '@/lib/feedback/dimension-label'

describe('getDimensionLabel', () => {
  const expectedLabels: Array<[string, string]> = [
    ['problem_framing', '문제 정의'],
    ['technical_depth', '기술 깊이'],
    ['reasoning_communication', '설명력'],
    ['conceptual_accuracy', '개념 정확도'],
    ['practical_application', '실무 응용'],
    ['experience_concreteness', '경험 구체성'],
    ['collaboration_awareness', '협업 의식'],
    ['recovery_from_gaps', '답변 회복력'],
    ['factual_consistency', '사실 일관성'],
    ['chain_depth', '후속 깊이'],
    ['fluency', '유창함'],
    ['confidence_tone', '자신감'],
    ['eye_contact_posture', '시선'],
    ['composure', '차분함'],
  ]

  it.each(expectedLabels)('%s 키는 한국어 라벨 "%s" 를 반환', (key, label) => {
    expect(getDimensionLabel(key)).toBe(label)
  })

  it('14개 dimension 모두 매핑되어 있다', () => {
    expect(expectedLabels).toHaveLength(14)
  })

  it('등록되지 않은 키는 원본 문자열을 fallback 으로 반환', () => {
    expect(getDimensionLabel('unknown_dimension')).toBe('unknown_dimension')
    expect(getDimensionLabel('')).toBe('')
  })
})
