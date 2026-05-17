const DIMENSION_LABELS: Record<string, string> = {
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

export const getDimensionLabel = (key: string): string =>
  DIMENSION_LABELS[key] ?? key
