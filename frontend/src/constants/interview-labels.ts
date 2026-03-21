import type { Position, Level, InterviewType, CsSubTopic, TechStack } from '@/types/interview'

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
  LANGUAGE_FRAMEWORK: { label: '언어/프레임워크', description: '선택한 기술스택의 언어 및 프레임워크 심화' },
  FULLSTACK_STACK: { label: '풀스택 기술', description: '선택한 풀스택 기술 통합, API 설계, 배포' },
  SYSTEM_DESIGN: { label: '시스템 설계', description: '아키텍처, 스케일링, 트레이드오프' },
  UI_FRAMEWORK: { label: 'UI 프레임워크', description: '선택한 UI 프레임워크 설계, 상태 관리, 최적화' },
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
  BACKEND: ['CS_FUNDAMENTAL', 'LANGUAGE_FRAMEWORK', 'SYSTEM_DESIGN', 'BEHAVIORAL', 'RESUME_BASED'],
  FRONTEND: ['CS_FUNDAMENTAL', 'UI_FRAMEWORK', 'BROWSER_PERFORMANCE', 'BEHAVIORAL', 'RESUME_BASED'],
  DEVOPS: ['CS_FUNDAMENTAL', 'INFRA_CICD', 'CLOUD', 'BEHAVIORAL', 'RESUME_BASED'],
  DATA_ENGINEER: ['CS_FUNDAMENTAL', 'DATA_PIPELINE', 'SQL_MODELING', 'BEHAVIORAL', 'RESUME_BASED'],
  FULLSTACK: ['CS_FUNDAMENTAL', 'FULLSTACK_STACK', 'UI_FRAMEWORK', 'SYSTEM_DESIGN', 'BEHAVIORAL', 'RESUME_BASED'],
}

export const TECH_STACK_LABELS: Record<TechStack, { label: string; description: string }> = {
  JAVA_SPRING: { label: 'Java/Spring', description: 'Spring Boot, JPA, JVM' },
  PYTHON_DJANGO: { label: 'Python/Django', description: 'Django, FastAPI, Celery' },
  NODE_NESTJS: { label: 'Node.js/NestJS', description: 'NestJS, Express, Prisma' },
  GO: { label: 'Go', description: '고루틴, 채널, net/http' },
  KOTLIN_SPRING: { label: 'Kotlin/Spring', description: 'Kotlin, Spring Boot, 코루틴' },
  REACT_TS: { label: 'React/TypeScript', description: 'React 18+, Next.js, TanStack' },
  VUE_TS: { label: 'Vue.js/TypeScript', description: 'Vue 3, Nuxt, Pinia' },
  SVELTE: { label: 'Svelte/SvelteKit', description: 'Svelte, SvelteKit' },
  ANGULAR: { label: 'Angular', description: 'Angular, RxJS, NgRx' },
  AWS_K8S: { label: 'AWS/Kubernetes', description: 'AWS, EKS, Terraform' },
  GCP: { label: 'GCP', description: 'GKE, Cloud Run, Pub/Sub' },
  AZURE: { label: 'Azure', description: 'AKS, Azure Functions' },
  SPARK_AIRFLOW: { label: 'Spark/Airflow', description: 'PySpark, Airflow DAG' },
  FLINK: { label: 'Flink', description: 'Apache Flink, 스트리밍' },
  DBT_SNOWFLAKE: { label: 'dbt/Snowflake', description: 'dbt, Snowflake, 모델링' },
  REACT_SPRING: { label: 'React + Spring', description: 'React FE + Spring BE' },
  REACT_NODE: { label: 'React + Node.js', description: 'React FE + Node BE' },
  NEXTJS_FULLSTACK: { label: 'Next.js Fullstack', description: 'Next.js App Router 풀스택' },
}

export const POSITION_TECH_STACKS: Record<Position, TechStack[]> = {
  BACKEND: ['JAVA_SPRING', 'PYTHON_DJANGO', 'NODE_NESTJS', 'GO', 'KOTLIN_SPRING'],
  FRONTEND: ['REACT_TS', 'VUE_TS', 'SVELTE', 'ANGULAR'],
  DEVOPS: ['AWS_K8S', 'GCP', 'AZURE'],
  DATA_ENGINEER: ['SPARK_AIRFLOW', 'FLINK', 'DBT_SNOWFLAKE'],
  FULLSTACK: ['REACT_SPRING', 'REACT_NODE', 'NEXTJS_FULLSTACK'],
}

export const POSITION_TECH_TYPE: Record<Position, InterviewType | null> = {
  BACKEND: 'LANGUAGE_FRAMEWORK',
  FRONTEND: 'UI_FRAMEWORK',
  FULLSTACK: 'FULLSTACK_STACK',
  DEVOPS: null,
  DATA_ENGINEER: null,
}

export const TECH_STACK_TYPE_LABELS: Record<TechStack, { label: string; description: string }> = {
  JAVA_SPRING:      { label: 'Java/Spring', description: 'JVM, Spring IoC/AOP, JPA' },
  PYTHON_DJANGO:    { label: 'Python/Django', description: 'Django, FastAPI, SQLAlchemy, Celery' },
  NODE_NESTJS:      { label: 'Node.js/NestJS', description: '이벤트 루프, NestJS DI, Prisma, TypeORM' },
  GO:               { label: 'Go', description: '고루틴, 채널, net/http, 동시성 패턴' },
  KOTLIN_SPRING:    { label: 'Kotlin/Spring', description: '코루틴, Spring Boot, 널 안전성' },
  REACT_TS:         { label: 'React/TypeScript', description: 'Hooks, 상태 관리, 렌더링 최적화' },
  VUE_TS:           { label: 'Vue.js/TypeScript', description: 'Composition API, Pinia, 반응성' },
  SVELTE:           { label: 'Svelte/SvelteKit', description: '반응성, 스토어, SSR' },
  ANGULAR:          { label: 'Angular', description: 'RxJS, DI, NgModule, 변경 감지' },
  AWS_K8S:          { label: 'AWS/Kubernetes', description: 'EKS, Terraform, 파이프라인' },
  GCP:              { label: 'GCP', description: 'GKE, Cloud Run, Pub/Sub' },
  AZURE:            { label: 'Azure', description: 'AKS, Functions, DevOps' },
  SPARK_AIRFLOW:    { label: 'Spark/Airflow', description: 'PySpark, DAG, ETL' },
  FLINK:            { label: 'Flink', description: '스트리밍, 윈도우, 상태 관리' },
  DBT_SNOWFLAKE:    { label: 'dbt/Snowflake', description: 'dbt 모델링, Snowflake, 분석' },
  REACT_SPRING:     { label: 'React + Spring', description: 'FE↔BE 통합, API 설계' },
  REACT_NODE:       { label: 'React + Node.js', description: 'MERN, 풀스택 JS, API' },
  NEXTJS_FULLSTACK: { label: 'Next.js Fullstack', description: 'App Router, RSC, 풀스택' },
}

export const getInterviewTypeLabel = (
  type: InterviewType,
  techStack: TechStack | null,
  position: Position,
): { label: string; description: string } => {
  const techType = POSITION_TECH_TYPE[position]
  if (techStack && techType && type === techType) {
    return TECH_STACK_TYPE_LABELS[techStack]
  }
  return INTERVIEW_TYPE_LABELS[type]
}
