export const INTERVIEW_TYPE_GROUPS = {
  CS_FUNDAMENTALS: {
    label: 'CS 기초',
    types: ['CS_FUNDAMENTAL'],
  },
  SYSTEM_DESIGN: {
    label: '시스템 설계',
    types: ['SYSTEM_DESIGN'],
  },
  LANGUAGE_FRAMEWORK: {
    label: '언어·프레임워크',
    types: ['LANGUAGE_FRAMEWORK', 'UI_FRAMEWORK', 'BROWSER_PERFORMANCE', 'FULLSTACK_STACK'],
  },
  INFRA_CLOUD: {
    label: '인프라·클라우드',
    types: ['INFRA_CICD', 'CLOUD'],
  },
  DATA: {
    label: '데이터',
    types: ['DATA_PIPELINE', 'SQL_MODELING'],
  },
  BEHAVIORAL: {
    label: '행동·경험',
    types: ['BEHAVIORAL', 'RESUME_BASED'],
  },
} as const

export type GroupKey = keyof typeof INTERVIEW_TYPE_GROUPS

export const getGroupKeyForType = (interviewType: string): GroupKey | 'OTHER' => {
  for (const [key, group] of Object.entries(INTERVIEW_TYPE_GROUPS)) {
    if ((group.types as readonly string[]).includes(interviewType)) {
      return key as GroupKey
    }
  }
  return 'OTHER'
}
