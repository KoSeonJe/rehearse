export const PROGRESS_STEPS = [
  { key: 'PENDING_UPLOAD', label: '대기', fullLabel: '업로드 대기 중' },
  { key: 'EXTRACTING', label: '추출', fullLabel: '영상 처리 중' },
  { key: 'ANALYZING', label: '분석', fullLabel: 'AI가 답변을 분석 중' },
  { key: 'FINALIZING', label: '생성', fullLabel: '종합 피드백 생성 중' },
] as const

export const getProgressIndex = (analysisStatus: string | null): number => {
  if (!analysisStatus) return -1
  return PROGRESS_STEPS.findIndex((s) => s.key === analysisStatus)
}

export const getProgressLabel = (analysisStatus: string | null): string => {
  if (!analysisStatus) return '대기 중'
  const step = PROGRESS_STEPS.find((s) => s.key === analysisStatus)
  if (step) return step.fullLabel
  return '대기 중'
}
