# DevLens 코드 컨벤션

> 유지보수와 협업을 위한 코딩 규칙. 모든 에이전트가 준수합니다.

---

## 커밋 규칙

### 커밋 단위
| 단위 | 예시 |
|------|------|
| **스펙/문서** | 기능 스펙, 팀 문서 업데이트 |
| **Backend 기능** | API + 엔티티 + 서비스 + 테스트 (하나의 기능 단위) |
| **Frontend 기능** | 컴포넌트 + 페이지 + 훅 (하나의 기능 단위) |
| **리팩토링** | 동작 변경 없는 구조 개선 |
| **버그 수정** | 이슈 단위 |
| **설정/인프라** | CI/CD, Docker, 빌드 설정 |

### 커밋 메시지
```
{type}: {한국어 요약}

- 상세 변경 내용 bullet
- 상세 변경 내용 bullet

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
```

| type | 용도 |
|------|------|
| `feat` | 새 기능 |
| `fix` | 버그 수정 |
| `refactor` | 동작 변경 없는 코드 개선 |
| `docs` | 문서, 스펙, 팀 문서 |
| `chore` | 설정, 의존성, 빌드 |
| `test` | 테스트 추가/수정 |
| `style` | 포맷팅, 세미콜론 등 (동작 무관) |

---

## Frontend 컨벤션

### 파일/네이밍
| 대상 | 규칙 | 예시 |
|------|------|------|
| 파일명 | kebab-case | `interview-setup-page.tsx` |
| 컴포넌트 | PascalCase | `InterviewSetupPage` |
| 훅 | camelCase + `use` 접두사 | `useCreateInterview` |
| 타입/인터페이스 | PascalCase | `InterviewResponse` |
| 상수 | UPPER_SNAKE_CASE | `LEVEL_LABELS` |
| CSS 클래스 | Tailwind 유틸리티 | 인라인 사용 |

### 디렉토리 구조
```
frontend/src/
├── components/
│   ├── ui/              # 범용 재사용 (Button, TextInput, etc.)
│   ├── interview/       # 면접 도메인 컴포넌트
│   ├── review/          # 피드백 리뷰 도메인
│   └── mediapipe/       # MediaPipe 도메인
├── hooks/               # 커스텀 훅 (use-*.ts)
├── stores/              # Zustand 스토어 (*-store.ts)
├── lib/                 # 유틸리티, API 클라이언트
├── pages/               # 페이지 컴포넌트 (*-page.tsx)
└── types/               # TypeScript 타입 정의 (*.ts)
```

### 컴포넌트 패턴
```typescript
// Props 인터페이스는 컴포넌트 파일 내부에 정의
interface ButtonProps {
  variant?: 'primary' | 'secondary' | 'ghost' | 'cta';
  disabled?: boolean;
  children: React.ReactNode;
}

// 화살표 함수 + 함수형 컴포넌트
const Button = ({ variant = 'primary', disabled, children }: ButtonProps) => {
  return <button>...</button>;
};

export default Button;
```

**금지:**
- `any` 타입
- `console.log` (커밋 전 제거)
- barrel export (`index.ts`) — 직접 import
- class 컴포넌트

### 상태 관리
| 범위 | 도구 | 용도 |
|------|------|------|
| 서버 상태 | TanStack Query | API 데이터, 캐싱, 재시도 |
| 전역 클라이언트 | Zustand | 면접 진행 상태, 비디오 플레이어 |
| 로컬 | useState/useReducer | 폼 입력, UI 토글 |

### API 연동 패턴
```typescript
// hooks/use-*.ts에 커스텀 훅으로 캡슐화
const useCreateInterview = () => {
  return useMutation({
    mutationFn: (data: CreateInterviewRequest) =>
      apiClient.post<InterviewResponse>('/api/v1/interviews', data),
  });
};
```

### 디자인 시스템
- 디자인 토큰: `.omc/notepads/team/design-tokens.md` 참조
- 색상: slate 모노톤 (하드코딩 금지, Tailwind 클래스 사용)
- 간격: 8px 그리드 (Tailwind spacing scale)
- 폰트: Pretendard (한국어 최적화)
- 반응형: 모바일 퍼스트 (`sm:`, `md:`, `lg:` 접두사)
- 접근성: WCAG 2.1 AA, focus-visible, aria 속성

---

## Backend 컨벤션

### 패키지 구조 (도메인 기반)
```
com.devlens.api/
├── domain/{feature}/       # 도메인별 분리
│   ├── controller/         # @RestController (HTTP 요청/응답)
│   ├── service/            # 비즈니스 로직, @Transactional
│   ├── repository/         # Spring Data JPA 인터페이스
│   ├── entity/             # JPA 엔티티, Enum
│   └── dto/                # 요청/응답 DTO (record 사용)
├── global/                 # 전역 공통
│   ├── config/             # Spring Configuration
│   ├── exception/          # 글로벌 예외 핸들러
│   └── common/             # ApiResponse, ErrorResponse
└── infra/                  # 외부 서비스 연동
    └── ai/                 # Claude API 클라이언트
```

### 네이밍
| 대상 | 규칙 | 예시 |
|------|------|------|
| 클래스 | PascalCase | `InterviewController` |
| 메서드 | camelCase | `createInterview()` |
| 상수 | UPPER_SNAKE_CASE | `MAX_QUESTIONS` |
| DB 테이블 | snake_case, 단수형 | `interview`, `interview_question` |
| DB 컬럼 | snake_case | `interview_type`, `created_at` |
| 패키지 | 소문자 | `com.devlens.api.domain.interview` |

### 계층 규칙
| 계층 | 책임 | 금지 사항 |
|------|------|----------|
| Controller | HTTP 처리, 입력 검증 (`@Valid`) | 비즈니스 로직, 직접 Repository 호출 |
| Service | 비즈니스 로직, 트랜잭션 | HTTP 관련 코드, 직접 DB 쿼리 |
| Repository | 데이터 접근 | 비즈니스 로직 |

### DTO 패턴
```java
// record 사용 (불변, 간결)
public record CreateInterviewRequest(
    @NotBlank @Size(max = 100) String position,
    @NotNull InterviewLevel level,
    @NotNull InterviewType interviewType
) {}
```

### 에러 처리
- `BusinessException` 상속하여 도메인별 예외 생성
- 에러 코드 체계: `{DOMAIN}_{3자리}` (예: `INTERVIEW_001`, `AI_001`)
- `GlobalExceptionHandler`에서 통일 에러 응답

### 응답 형식
```java
// 성공: ApiResponse.success(data)
// 실패: ErrorResponse (GlobalExceptionHandler가 자동 생성)
```

### 테스트
- 서비스 단위 테스트 필수 (핵심 비즈니스 로직)
- `@MockBean`으로 외부 의존성 격리
- 테스트 메서드명: `메서드명_조건_기대결과` (예: `createInterview_success`)

---

## 공통 금지 사항

- 프론트엔드에서 직접 Claude API 호출 (API Key 노출)
- 시크릿 하드코딩 (환경변수 사용)
- MVP DON'T 범위 기능 구현
- 불필요한 라이브러리 추가 (브라우저 네이티브 API 우선)
- 하드코딩된 색상/간격 값 (디자인 토큰 사용)
