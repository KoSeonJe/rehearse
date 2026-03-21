import type { Position, Level, CsSubTopic } from '@/types/interview'

export type Step = 1 | 2 | 3 | 4 | 5

export const TOTAL_STEPS = 5

export const POSITIONS: Position[] = ['BACKEND', 'FRONTEND', 'DEVOPS', 'DATA_ENGINEER', 'FULLSTACK']
export const LEVELS: Level[] = ['JUNIOR', 'MID', 'SENIOR']
export const CS_SUB_TOPICS: CsSubTopic[] = ['DATA_STRUCTURE', 'OS', 'NETWORK', 'DATABASE']
export const MAX_FILE_SIZE = 10 * 1024 * 1024 // 10MB

export const DURATION_PRESETS = [
  { minutes: 15, label: '15분', description: '빠른 연습' },
  { minutes: 30, label: '30분', description: '기본 면접' },
  { minutes: 45, label: '45분', description: '심화 면접' },
  { minutes: 60, label: '60분', description: '풀 면접' },
] as const
