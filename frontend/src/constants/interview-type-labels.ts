export const INTERVIEW_TYPE_LABELS: Record<string, string> = {
  CS_FUNDAMENTAL: 'CS 기초',
  SYSTEM_DESIGN: '시스템 설계',
  LANGUAGE_FRAMEWORK: '언어·프레임워크',
  UI_FRAMEWORK: 'UI 프레임워크',
  BROWSER_PERFORMANCE: '브라우저·성능',
  FULLSTACK_STACK: '풀스택',
  INFRA_CICD: '인프라·CI/CD',
  CLOUD: '클라우드',
  DATA_PIPELINE: '데이터 파이프라인',
  SQL_MODELING: 'SQL·모델링',
  BEHAVIORAL: '행동 면접',
  RESUME_BASED: '이력서 기반',
}

export const getInterviewTypeLabel = (type: string): string =>
  INTERVIEW_TYPE_LABELS[type] ?? type
