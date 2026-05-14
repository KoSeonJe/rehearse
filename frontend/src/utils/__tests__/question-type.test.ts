import { describe, it, expect } from 'vitest'
import {
  isMainQuestionType,
  isFollowupQuestionType,
  resolveFollowUpQuestionType,
  deriveQuestionFromSet,
} from '@/utils/question-type'
import type { QuestionSetData } from '@/types/interview'

const buildQuestionSet = (overrides: Partial<QuestionSetData> = {}): QuestionSetData => ({
  id: 100,
  category: 'CS_FUNDAMENTAL',
  orderIndex: 0,
  analysisStatus: 'PENDING',
  failureReason: null,
  questions: [],
  ...overrides,
})

describe('isMainQuestionType — BE 표준 트랙 메인 인식', () => {
  it('TECH_MAIN / BEHAVIORAL_MAIN / RESUME_OPENER 는 메인으로 분류된다', () => {
    expect(isMainQuestionType('TECH_MAIN')).toBe(true)
    expect(isMainQuestionType('BEHAVIORAL_MAIN')).toBe(true)
    expect(isMainQuestionType('RESUME_OPENER')).toBe(true)
  })

  it('FOLLOWUP 계열과 RESUME 진행 단계는 메인이 아니다', () => {
    expect(isMainQuestionType('TECH_FOLLOWUP')).toBe(false)
    expect(isMainQuestionType('BEHAVIORAL_FOLLOWUP')).toBe(false)
    expect(isMainQuestionType('RESUME_PLAYGROUND')).toBe(false)
    expect(isMainQuestionType('RESUME_INTERROGATION')).toBe(false)
  })

  it('null / undefined / 알 수 없는 값은 메인이 아니다', () => {
    expect(isMainQuestionType(null)).toBe(false)
    expect(isMainQuestionType(undefined)).toBe(false)
    expect(isMainQuestionType('MAIN')).toBe(false)
    expect(isMainQuestionType('UNKNOWN')).toBe(false)
  })
})

describe('isFollowupQuestionType — BE 트랙별 후속질문 인식', () => {
  it('TECH_FOLLOWUP / BEHAVIORAL_FOLLOWUP 모두 후속질문으로 분류된다', () => {
    expect(isFollowupQuestionType('TECH_FOLLOWUP')).toBe(true)
    expect(isFollowupQuestionType('BEHAVIORAL_FOLLOWUP')).toBe(true)
  })

  it('메인 / RESUME 진행 단계 / null 은 후속질문이 아니다', () => {
    expect(isFollowupQuestionType('TECH_MAIN')).toBe(false)
    expect(isFollowupQuestionType('RESUME_OPENER')).toBe(false)
    expect(isFollowupQuestionType('RESUME_PLAYGROUND')).toBe(false)
    expect(isFollowupQuestionType(null)).toBe(false)
  })
})

describe('resolveFollowUpQuestionType — 트랙 컨텍스트로 후속질문 enum 결정', () => {
  it('TECH 트랙 메인 → TECH_FOLLOWUP', () => {
    expect(resolveFollowUpQuestionType('TECH_MAIN', 'CS_FUNDAMENTAL')).toBe('TECH_FOLLOWUP')
  })

  it('BEHAVIORAL 트랙 메인 → BEHAVIORAL_FOLLOWUP', () => {
    expect(resolveFollowUpQuestionType('BEHAVIORAL_MAIN', 'BEHAVIORAL')).toBe('BEHAVIORAL_FOLLOWUP')
  })

  it('main 미발견 + category=BEHAVIORAL → BEHAVIORAL_FOLLOWUP (BE fallback 미러)', () => {
    expect(resolveFollowUpQuestionType(null, 'BEHAVIORAL')).toBe('BEHAVIORAL_FOLLOWUP')
  })

  it('main 미발견 + category != BEHAVIORAL → TECH_FOLLOWUP', () => {
    expect(resolveFollowUpQuestionType(null, 'CS_FUNDAMENTAL')).toBe('TECH_FOLLOWUP')
    expect(resolveFollowUpQuestionType(null, 'LANGUAGE_FRAMEWORK')).toBe('TECH_FOLLOWUP')
  })

  it('RESUME_OPENER 메인 → TECH_FOLLOWUP (BE fallback 경로 정합)', () => {
    expect(resolveFollowUpQuestionType('RESUME_OPENER', 'RESUME_BASED')).toBe('TECH_FOLLOWUP')
  })
})

describe('deriveQuestionFromSet — 트랙별 메인 질문 derive (자동 SKIPPED 회귀 방지)', () => {
  it('TECH_MAIN 만 포함된 CS_FUNDAMENTAL 세트 → 메인 questionText 추출', () => {
    const qSet = buildQuestionSet({
      id: 11,
      category: 'CS_FUNDAMENTAL',
      questions: [
        { id: 1, questionType: 'TECH_MAIN', questionText: 'GC 동작 원리를 설명해주세요', bestAnswer: null, orderIndex: 0 },
      ],
    })
    const derived = deriveQuestionFromSet(qSet, 0)
    expect(derived.content).toBe('GC 동작 원리를 설명해주세요')
    expect(derived.id).toBe(1)
    expect(derived.category).toBe('CS_FUNDAMENTAL')
  })

  it('BEHAVIORAL_MAIN 만 포함된 BEHAVIORAL 세트 → 메인 questionText 추출', () => {
    const qSet = buildQuestionSet({
      id: 22,
      category: 'BEHAVIORAL',
      questions: [
        { id: 5, questionType: 'BEHAVIORAL_MAIN', questionText: '갈등 상황 경험을 말씀해주세요', bestAnswer: null, orderIndex: 0 },
      ],
    })
    const derived = deriveQuestionFromSet(qSet, 1)
    expect(derived.content).toBe('갈등 상황 경험을 말씀해주세요')
    expect(derived.id).toBe(5)
    expect(derived.order).toBe(1)
  })

  it('RESUME_OPENER 포함 세트 → 메인 questionText 추출', () => {
    const qSet = buildQuestionSet({
      id: 33,
      category: 'RESUME_BASED',
      questions: [
        { id: 8, questionType: 'RESUME_OPENER', questionText: '자기소개 부탁드립니다', bestAnswer: null, orderIndex: 0 },
      ],
    })
    const derived = deriveQuestionFromSet(qSet, 2)
    expect(derived.content).toBe('자기소개 부탁드립니다')
    expect(derived.id).toBe(8)
  })

  it('LANGUAGE_FRAMEWORK 트랙 (TECH_MAIN + TECH_FOLLOWUP) → 메인만 추출, content 비어 있지 않음', () => {
    const qSet = buildQuestionSet({
      id: 44,
      category: 'LANGUAGE_FRAMEWORK',
      questions: [
        { id: 10, questionType: 'TECH_MAIN', questionText: 'Spring transaction propagation 을 설명해주세요', bestAnswer: null, orderIndex: 0 },
        { id: 11, questionType: 'TECH_FOLLOWUP', questionText: 'REQUIRES_NEW 동작 예시', bestAnswer: null, orderIndex: 1 },
      ],
    })
    const derived = deriveQuestionFromSet(qSet, 0)
    expect(derived.content).toBe('Spring transaction propagation 을 설명해주세요')
    expect(derived.content.length).toBeGreaterThan(0)
    expect(derived.id).toBe(10)
  })

  it('메인 미발견 (이상 케이스) → content 빈 문자열 + id 는 세트 id 로 fallback', () => {
    const qSet = buildQuestionSet({
      id: 99,
      questions: [
        { id: 1, questionType: 'TECH_FOLLOWUP', questionText: '단독 후속질문', bestAnswer: null, orderIndex: 0 },
      ],
    })
    const derived = deriveQuestionFromSet(qSet, 0)
    expect(derived.content).toBe('')
    expect(derived.id).toBe(99)
  })
})
