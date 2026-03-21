# Phase 5: [FE] 기술스택 선택 UI

> **상태**: TODO
> **브랜치**: `feat/fe-tech-stack-selection`
> **PR**: PR-5 → develop
> **의존**: Phase 2 (PR-2 머지 후 — BE API에 techStack 필드 + position-techStack 서버 검증 포함)
> **병렬**: Phase 4와 동시 진행 가능

---

## 개요

면접 Setup 위저드를 4단계 → 5단계로 확장하여 TechStack 선택 스텝을 추가한다.
TechStack 선택은 **optional** — 스킵 시 Position 기본 스택이 자동 적용된다.

### 스텝 변경

```
기존: Position(1) → Level(2) → Duration(3) → InterviewType(4)
변경: Position(1) → TechStack(2) → Level(3) → Duration(4) → InterviewType(5)
```

---

## Task 5-1: TechStack 타입 + 상수 정의

- **Implement**: `frontend`
- **Review**: `code-reviewer`

### 파일 및 변경 사항

#### 1. TechStack 타입 추가

수정: `frontend/src/types/interview.ts`

```typescript
// L1 근처, Position 아래에 추가
export type TechStack =
  | 'JAVA_SPRING'
  | 'PYTHON_DJANGO'
  | 'NODE_NESTJS'
  | 'GO'
  | 'KOTLIN_SPRING'
  | 'REACT_TS'
  | 'VUE_TS'
  | 'SVELTE'
  | 'ANGULAR'
  | 'AWS_K8S'
  | 'GCP'
  | 'AZURE'
  | 'SPARK_AIRFLOW'
  | 'FLINK'
  | 'DBT_SNOWFLAKE'
  | 'REACT_SPRING'
  | 'REACT_NODE'
  | 'NEXTJS_FULLSTACK'
```

CreateInterviewRequest에 techStack 추가 (L156~L163):

```typescript
export interface CreateInterviewRequest {
  position: Position
  positionDetail?: string
  level: Level
  interviewTypes: InterviewType[]
  durationMinutes: number
  csSubTopics?: CsSubTopic[]
  techStack?: TechStack  // 추가 — optional, 미선택 시 서버에서 기본 스택 적용
}
```

InterviewSession에 techStack 추가 (L141~L154):

```typescript
export interface InterviewSession {
  // ... 기존 필드
  techStack?: TechStack | null  // 추가
}
```

#### 2. TechStack 라벨 + Position 매핑

수정: `frontend/src/constants/interview-labels.ts`

```typescript
import type { Position, Level, InterviewType, CsSubTopic, TechStack } from '@/types/interview'

// 기존 코드 유지...

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

// Position별 허용 TechStack 목록 (첫 번째가 기본 스택)
export const POSITION_TECH_STACKS: Record<Position, TechStack[]> = {
  BACKEND: ['JAVA_SPRING', 'PYTHON_DJANGO', 'NODE_NESTJS', 'GO', 'KOTLIN_SPRING'],
  FRONTEND: ['REACT_TS', 'VUE_TS', 'SVELTE', 'ANGULAR'],
  DEVOPS: ['AWS_K8S', 'GCP', 'AZURE'],
  DATA_ENGINEER: ['SPARK_AIRFLOW', 'FLINK', 'DBT_SNOWFLAKE'],
  FULLSTACK: ['REACT_SPRING', 'REACT_NODE', 'NEXTJS_FULLSTACK'],
}
```

#### 3. Setup 상수 업데이트

수정: `frontend/src/constants/setup.ts`

```typescript
import type { Position, Level, CsSubTopic, TechStack } from '@/types/interview'

export type Step = 1 | 2 | 3 | 4 | 5  // 변경: 4 → 5

export const TOTAL_STEPS = 5  // 변경: 4 → 5

// 기존 코드 유지...
```

---

## Task 5-2: StepTechStack 컴포넌트 생성

- **Implement**: `frontend-developer`
- **Review**: `designer` — UI/UX 일관성, 기존 StepPosition과 동일한 카드 스타일

### 파일

- 신규: `frontend/src/components/setup/step-tech-stack.tsx`

### 구현 상세

```tsx
import type { Position, TechStack } from '@/types/interview'
import { POSITION_TECH_STACKS, TECH_STACK_LABELS } from '@/constants/interview-labels'

interface StepTechStackProps {
  position: Position
  techStack: TechStack | null
  onSelect: (techStack: TechStack | null) => void
}

export const StepTechStack = ({ position, techStack, onSelect }: StepTechStackProps) => {
  const availableStacks = POSITION_TECH_STACKS[position]
  const defaultStack = availableStacks[0]  // 첫 번째가 기본 스택

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-semibold text-gray-900">기술 스택 선택</h2>
        <p className="mt-1 text-sm text-gray-500">
          선택하지 않으면 <strong>{TECH_STACK_LABELS[defaultStack].label}</strong>이
          기본 적용됩니다.
        </p>
      </div>

      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
        {availableStacks.map((stack) => {
          const { label, description } = TECH_STACK_LABELS[stack]
          const isSelected = techStack === stack
          const isDefault = stack === defaultStack

          return (
            <button
              key={stack}
              onClick={() => onSelect(isSelected ? null : stack)}
              className={/* StepPosition과 동일한 카드 스타일 */}
            >
              <span className="font-medium">{label}</span>
              {isDefault && (
                <span className="text-xs text-coral-500 font-medium">기본</span>
              )}
              <span className="text-xs text-gray-400">{description}</span>
            </button>
          )
        })}
      </div>

      {/* 선택하지 않음 = null → 기본 스택 자동 적용 안내 */}
      {!techStack && (
        <p className="text-xs text-gray-400 text-center">
          기본 스택({TECH_STACK_LABELS[defaultStack].label})으로 면접이 진행됩니다.
        </p>
      )}
    </div>
  )
}
```

**UX 설계 포인트:**
- 카드 클릭으로 선택/해제 토글 (해제 시 null = 기본 스택)
- 기본 스택에 "기본" 배지 표시
- 선택하지 않아도 다음 스텝 진행 가능 (canNext = true)
- 기존 StepPosition과 동일한 카드 그리드 스타일

---

## Task 5-3: 위저드 상태 관리 + 페이지 수정

- **Implement**: `frontend`
- **Review**: `code-reviewer`

### 파일

#### 1. use-interview-setup.ts 수정

수정: `frontend/src/hooks/use-interview-setup.ts`

변경 사항:
- **techStack 상태 추가**: `const [techStack, setTechStack] = useState<TechStack | null>(null)`
- **position 변경 시 techStack 리셋**: position이 바뀌면 `setTechStack(null)`
- **스텝 번호 이동**: Level은 Step 3, Duration은 Step 4, InterviewType은 Step 5
- **canNext 업데이트**: Step 2 (TechStack)은 항상 true (optional)
- **handleSubmit**: request에 `techStack` 포함 (null이 아닌 경우에만)

```typescript
// 상태 추가
const [techStack, setTechStack] = useState<TechStack | null>(null)

// position 변경 시 techStack 리셋
const handlePositionSelect = (pos: Position) => {
  setPosition(pos)
  setTechStack(null)  // 추가
  // ... 기존 로직
}

// canNext 업데이트
const canNext = useMemo(() => {
  switch (step) {
    case 1: return !!position
    case 2: return true  // TechStack은 optional
    case 3: return !!level
    case 4: return durationMinutes >= 5 && durationMinutes <= 120
    case 5: return interviewTypes.length > 0
    default: return false
  }
}, [step, position, level, durationMinutes, interviewTypes])

// handleSubmit
const request: CreateInterviewRequest = {
  position: position!,
  level: level!,
  interviewTypes,
  durationMinutes,
  ...(positionDetail && { positionDetail }),
  ...(csSubTopics.length > 0 && { csSubTopics }),
  ...(techStack && { techStack }),  // 추가
}
```

#### 2. interview-setup-page.tsx 수정

수정: `frontend/src/pages/interview-setup-page.tsx`

변경 사항:
- Step 2에 `<StepTechStack>` 렌더링
- 기존 Step 2(Level) → Step 3, Step 3(Duration) → Step 4, Step 4(InterviewType) → Step 5

```tsx
// step 렌더링
{step === 1 && <StepPosition ... />}
{step === 2 && <StepTechStack
  position={position!}
  techStack={techStack}
  onSelect={setTechStack}
/>}
{step === 3 && <StepLevel ... />}
{step === 4 && <StepDuration ... />}
{step === 5 && <StepInterviewType ... />}
```

#### 3. SetupProgressBar 수정 (있는 경우)

- 5단계 프로그레스 바로 업데이트
- 스텝 라벨: 직무 → 기술스택 → 레벨 → 시간 → 유형

---

## 검증

### FE CI

```bash
cd frontend && npm run lint && npm run build
```

### 수동 테스트 시나리오

1. **Position 선택 → TechStack 표시 확인**
   - BACKEND 선택 → Java/Spring, Python/Django, Node.js/NestJS, Go, Kotlin/Spring 5개 표시
   - FRONTEND 선택 → React/TS, Vue.js/TS, Svelte, Angular 4개 표시

2. **TechStack 스킵**
   - TechStack 선택하지 않고 "다음" → 정상 진행
   - 최종 제출 시 request에 techStack 필드 없음 (또는 undefined)

3. **TechStack 선택 후 Position 변경**
   - BACKEND → JAVA_SPRING 선택 → 이전으로 돌아가 FRONTEND로 변경
   - TechStack이 null로 리셋되고 Step 2에서 Frontend 스택 표시

4. **API 요청 확인**
   - 개발자 도구 Network에서 POST /api/v1/interviews 요청 확인
   - techStack 선택 시: `{"position":"BACKEND","techStack":"PYTHON_DJANGO",...}`
   - techStack 미선택 시: techStack 필드 없음

5. **반응형 확인**
   - 모바일 (< 640px): 2열 그리드
   - 데스크탑: 3열 그리드
