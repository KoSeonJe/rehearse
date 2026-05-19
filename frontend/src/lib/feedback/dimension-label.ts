export type DimensionKey =
  | 'problem_framing'
  | 'technical_depth'
  | 'reasoning_communication'
  | 'conceptual_accuracy'
  | 'practical_application'
  | 'experience_concreteness'
  | 'collaboration_awareness'
  | 'recovery_from_gaps'
  | 'factual_consistency'
  | 'chain_depth'
  | 'fluency'
  | 'confidence_tone'
  | 'eye_contact_posture'
  | 'composure'

const DIMENSION_LABELS: Record<DimensionKey, string> = {
  problem_framing: '문제 정의',
  technical_depth: '기술 깊이',
  reasoning_communication: '설명력',
  conceptual_accuracy: '개념 정확도',
  practical_application: '실무 응용',
  experience_concreteness: '경험 구체성',
  collaboration_awareness: '협업 의식',
  recovery_from_gaps: '답변 회복력',
  factual_consistency: '사실 일관성',
  chain_depth: '후속 깊이',
  fluency: '유창함',
  confidence_tone: '자신감',
  eye_contact_posture: '시선',
  composure: '차분함',
}

const isDimensionKey = (key: string): key is DimensionKey =>
  key in DIMENSION_LABELS

// 미매핑 키는 raw 반환 — BE 신규 dimension 추가 시 매핑 누락 신호
export const getDimensionLabel = (key: string): string =>
  isDimensionKey(key) ? DIMENSION_LABELS[key] : key
