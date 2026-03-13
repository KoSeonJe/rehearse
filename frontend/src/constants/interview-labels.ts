import type { Position, Level, InterviewType, CsSubTopic } from '@/types/interview'

export const POSITION_LABELS: Record<Position, { label: string; description: string }> = {
  BACKEND: { label: '백엔드', description: '서버, API, 데이터베이스' },
  FRONTEND: { label: '프론트엔드', description: 'UI/UX, 브라우저, 반응형' },
  DEVOPS: { label: '데브옵스', description: 'CI/CD, 인프라, 클라우드' },
  DATA_ENGINEER: { label: '데이터 엔지니어', description: '파이프라인, ETL, 분석' },
  FULLSTACK: { label: '풀스택', description: '프론트 + 백엔드 통합' },
}

export const LEVEL_LABELS: Record<Level, { label: string; description: string; hint: string }> = {
  JUNIOR: { label: '주니어', description: '0-3년차', hint: '기본 개념 이해도 중심으로 질문합니다' },
  MID: { label: '미드', description: '3-7년차', hint: '실무 적용과 문제 해결 경험을 물어봅니다' },
  SENIOR: { label: '시니어', description: '7년차 이상', hint: '아키텍처 판단력과 기술 리더십을 평가합니다' },
}

export const INTERVIEW_TYPE_LABELS: Record<
  InterviewType,
  { label: string; description: string }
> = {
  CS_FUNDAMENTAL: { label: 'CS 기초', description: '자료구조, 알고리즘, OS, 네트워크, DB' },
  BEHAVIORAL: { label: '경험/협업', description: '팀 협업, 갈등 해결, 프로젝트 경험' },
  RESUME_BASED: { label: '이력서 기반', description: '이력서/포트폴리오 맞춤 질문' },
  JAVA_SPRING: { label: 'Java/Spring', description: 'JVM, Spring IoC/AOP, JPA' },
  FULLSTACK_JS: { label: 'JS 풀스택', description: 'Node.js + React, API 설계, DB 연동, 배포' },
  SYSTEM_DESIGN: { label: '시스템 설계', description: '아키텍처, 스케일링, 트레이드오프' },
  REACT_COMPONENT: { label: 'React/컴포넌트', description: '컴포넌트 설계, 상태 관리, 최적화' },
  BROWSER_PERFORMANCE: { label: '브라우저/웹 성능', description: '렌더링, 번들 최적화, Core Web Vitals' },
  INFRA_CICD: { label: '인프라/CI-CD', description: 'Docker, K8s, 파이프라인' },
  CLOUD: { label: '클라우드', description: 'AWS/GCP/Azure, 서버리스, IaC' },
  DATA_PIPELINE: { label: '데이터 파이프라인', description: 'ETL/ELT, 스트리밍, Spark' },
  SQL_MODELING: { label: 'SQL/모델링', description: '쿼리 최적화, 정규화, ERD' },
}

export const CS_SUB_TOPIC_LABELS: Record<CsSubTopic, string> = {
  DATA_STRUCTURE: '자료구조/알고리즘',
  OS: '운영체제',
  NETWORK: '네트워크',
  DATABASE: '데이터베이스',
}

export const POSITION_INTERVIEW_TYPES: Record<Position, InterviewType[]> = {
  BACKEND: ['CS_FUNDAMENTAL', 'JAVA_SPRING', 'SYSTEM_DESIGN', 'BEHAVIORAL', 'RESUME_BASED'],
  FRONTEND: ['CS_FUNDAMENTAL', 'REACT_COMPONENT', 'BROWSER_PERFORMANCE', 'BEHAVIORAL', 'RESUME_BASED'],
  DEVOPS: ['CS_FUNDAMENTAL', 'INFRA_CICD', 'CLOUD', 'BEHAVIORAL', 'RESUME_BASED'],
  DATA_ENGINEER: ['CS_FUNDAMENTAL', 'DATA_PIPELINE', 'SQL_MODELING', 'BEHAVIORAL', 'RESUME_BASED'],
  FULLSTACK: ['CS_FUNDAMENTAL', 'FULLSTACK_JS', 'REACT_COMPONENT', 'SYSTEM_DESIGN', 'BEHAVIORAL', 'RESUME_BASED'],
}
