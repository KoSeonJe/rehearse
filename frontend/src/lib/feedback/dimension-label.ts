const DIMENSION_LABELS: Record<string, string> = {
  conceptual_accuracy: '개념 정확도',
  applied_application: '실무 응용',
  system_design: '시스템 설계',
  communication: '커뮤니케이션',
  problem_definition: '문제 정의',
  evidence_quality: '근거 제시',
  delivery: '딜리버리',
  structure: '답변 구조',
  depth: '답변 깊이',
}

export const getDimensionLabel = (key: string): string =>
  DIMENSION_LABELS[key] ?? key
