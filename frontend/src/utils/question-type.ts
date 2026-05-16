import type { Question, QuestionSetData, QuestionType } from '@/types/interview'

// 메인 질문으로 인식할 enum 집합.
// BE QuestionType.isMain() 은 TECH_MAIN / BEHAVIORAL_MAIN / RESUME_MAIN 을 main 으로 판단.
// FE 표시 단위 (질문 카드 / Q1 라벨 / 메인 질문 검색) 에서는 RESUME_OPENER 도 "한 세트의 진입 질문"으로
// 동등 취급한다.
const MAIN_QUESTION_TYPES: ReadonlySet<string> = new Set<QuestionType>([
  'TECH_MAIN',
  'BEHAVIORAL_MAIN',
  'RESUME_OPENER',
  'RESUME_MAIN',
])

// BE QuestionType.isFollowUp() 미러 — TECH_FOLLOWUP / BEHAVIORAL_FOLLOWUP / RESUME_FOLLOWUP.
const FOLLOWUP_QUESTION_TYPES: ReadonlySet<string> = new Set<QuestionType>([
  'TECH_FOLLOWUP',
  'BEHAVIORAL_FOLLOWUP',
  'RESUME_FOLLOWUP',
])

export const isMainQuestionType = (questionType: string | null | undefined): boolean => {
  if (questionType === null || questionType === undefined) return false
  return MAIN_QUESTION_TYPES.has(questionType)
}

export const isFollowupQuestionType = (questionType: string | null | undefined): boolean => {
  if (questionType === null || questionType === undefined) return false
  return FOLLOWUP_QUESTION_TYPES.has(questionType)
}

// 클라이언트 store 에 follow-up 질문을 추가할 때 사용할 enum 결정.
// BE FollowUpTransactionHandler.resolveFollowUpType 미러:
//   - main type 우선 (TECH_MAIN → TECH_FOLLOWUP, BEHAVIORAL_MAIN → BEHAVIORAL_FOLLOWUP)
//   - main 미발견 시 category fallback (BEHAVIORAL → BEHAVIORAL_FOLLOWUP, 그 외 → TECH_FOLLOWUP)
//   - RESUME_* 메인 (RESUME_OPENER 등) 도 fallback 경로로 TECH_FOLLOWUP 정착 (BE 동일).
export const resolveFollowUpQuestionType = (
  mainQuestionType: string | null | undefined,
  category: string | null | undefined,
): QuestionType => {
  if (mainQuestionType === 'TECH_MAIN') return 'TECH_FOLLOWUP'
  if (mainQuestionType === 'BEHAVIORAL_MAIN') return 'BEHAVIORAL_FOLLOWUP'
  return category === 'BEHAVIORAL' ? 'BEHAVIORAL_FOLLOWUP' : 'TECH_FOLLOWUP'
}

// QuestionSetData → 진행용 Question 으로 변환.
// 한 질문세트는 1개의 메인 질문 (TECH_MAIN / BEHAVIORAL_MAIN / RESUME_OPENER) 을 가진다는 BE 계약 전제.
// 메인이 없는 경우 (분석 진행/실패 등) 빈 content 로 fallback 하되, 자동 SKIPPED 회귀 방지를 위해
// 메인 검색 범위는 BE enum 7종 모두에 정합해야 한다.
export const deriveQuestionFromSet = (qSet: QuestionSetData, orderIndex: number): Question => {
  const mainQ = qSet.questions.find((q) => isMainQuestionType(q.questionType))
  return {
    id: mainQ?.id ?? qSet.id,
    content: mainQ?.questionText ?? '',
    ttsContent: mainQ?.ttsText ?? mainQ?.questionText ?? '',
    category: qSet.category,
    order: orderIndex,
  }
}
