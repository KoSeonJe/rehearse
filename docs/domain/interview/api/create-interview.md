# API: 면접 세션 생성

> Endpoint: `POST /api/v1/interviews`
> Action: 사용자가 직무 / 레벨 / 면접 유형 / 시간 / (옵션) 이력서 PDF 를 입력해 모의면접 세션을 생성한다. 질문 생성은 비동기 이벤트로 트리거.
> 관련 테이블: `interview` (write) / `interview_interview_types` (write) / `interview_cs_sub_topics` (write)
> 관련 외부 의존: 없음 (PDF 텍스트 추출은 동기, AI 호출은 후속 이벤트 핸들러에서 진행)

---

## 입력

| 위치 | 필드 | 타입 | 제약 | 의미 |
|------|------|------|------|------|
| header | `Authorization` | Bearer | required | JWT |
| header | `Content-Type` | multipart/form-data | required | 이력서 PDF 동시 업로드 가능 |
| part | `request` | JSON `CreateInterviewRequest` | required | 면접 메타 |
| part.body | `position` | enum | required | `BACKEND` / `FRONTEND` / `FULLSTACK` / `DEVOPS` / `DATA` |
| part.body | `positionDetail` | string | optional, ≤100 | 직무 부가 설명 |
| part.body | `level` | enum | required | `InterviewLevel` |
| part.body | `interviewTypes` | enum[] | required, ≥1 | `InterviewType` 중 1개 이상 |
| part.body | `csSubTopics` | string[] | optional | CS 세부 주제 |
| part.body | `durationMinutes` | int | required, 5..120 | 면접 시간(분) |
| part.body | `techStack` | enum | optional | NULL 시 직무 디폴트 |
| part | `resumeFile` | multipart | optional | PDF, max 10MB (`spring.servlet.multipart.max-file-size: 10MB`) |

---

## 출력 (201 CREATED)

| 필드 | 타입 | 의미 |
|------|------|------|
| `id` | Long | 면접 PK |
| `publicId` | string | UUID. 외부 노출용 (`@PrePersist`) |
| `position` / `level` / `interviewTypes` / `csSubTopics` / `durationMinutes` / `techStack` | — | 입력 echo |
| `status` | enum | 초기값 = `READY` |
| `questionGenerationStatus` | enum | 초기값 = `PENDING` (이벤트 발행 후 비동기로 `GENERATING` → `COMPLETED`/`FAILED`) |
| `questionSets` | array | 생성 시점 = 빈 리스트 |
| `createdAt` | datetime | 생성 시각 |

## 출력 (4xx)

| 코드 | 에러 코드 | 조건 |
|------|----------|------|
| 400 | `INTERVIEW_010 INVALID_INTERVIEW_TYPES` | `interviewTypes` 비었음 |
| 400 | `INTERVIEW_007 INVALID_TECH_STACK` | `techStack` 이 `position` 의 허용 셋이 아님 |
| 400 | `RESUME_EXCLUSIVITY_VIOLATION` | `RESUME_BASED` + 다른 type 동시 / 또는 `RESUME_BASED` 없는데 PDF 첨부 |
| 400 | `RESUME_REQUIRED_FOR_RESUME_BASED` | `RESUME_BASED` 인데 PDF 미첨부 |
| 400 | `INVALID_FILE_EMPTY` | PDF 바이트 읽기 실패 |
| 400 | Bean Validation | `position` / `level` / `durationMinutes` / `interviewTypes` 검증 실패 |
| 401 | — | JWT 부재 / 만료 |

---

## 흐름

### 1. 입력 검증 (Controller)
- `@Valid` 로 Bean Validation (`@NotNull`, `@NotEmpty`, `@Min`, `@Max`, `@Size`).
- `@AuthenticationPrincipal Long userId` 추출.

### 2. 이력서 배타성 검증 (Service)
- `validateResumeExclusivity`:
  - `interviewTypes` 비었음 → `INVALID_INTERVIEW_TYPES`
  - `RESUME_BASED` 포함 + size > 1 → `RESUME_EXCLUSIVITY_VIOLATION`
  - `RESUME_BASED` 포함 + 파일 없음 → `RESUME_REQUIRED_FOR_RESUME_BASED`
  - `RESUME_BASED` 없음 + 파일 있음 → `RESUME_EXCLUSIVITY_VIOLATION`

### 3. 분기: 이력서 첨부 여부

#### 3-A. 이력서 PDF 있음 (`RESUME_BASED`)
1. `resumeFile.getBytes()` → `FileHasher.hash()` 로 SHA 해시 (사전 캐시 / 멱등 식별자)
2. `PdfTextExtractor.extract(resumeFile)` 동기 추출 (실패 시 즉시 4xx)

#### 3-B. 이력서 없음
1. `resumeText` / `resumeFileHash` = null

### 4. tech stack 호환성 검증
- `request.getTechStack().isAllowedFor(request.getPosition())` false → `INVALID_TECH_STACK (400)`

### 5. 저장
- `Interview.builder()` → `status = READY`, `questionGenerationStatus = PENDING` (생성자 디폴트)
- `interviewRepository.save(interview)` — `@PrePersist` 가 `public_id` UUID 생성
- ElementCollection `interview_interview_types` / `interview_cs_sub_topics` 동시 INSERT

### 6. 이벤트 발행
- `QuestionGenerationRequestedEvent` 발행:
  - 페이로드: `interviewId`, `userId`, `position`, `positionDetail`, `level`, `interviewTypes`, `csSubTopics`, `resumeText`, `resumeFileHash`, `durationMinutes`, `techStack`
- `question` 등 외부 도메인의 `@TransactionalEventListener(AFTER_COMMIT)` 가 비동기 질문 생성 수행 (본 도메인 책임 외).

### 7. 응답
- 201 + `InterviewResponse.from(saved, [])` (questionSets 빈 리스트)

---

## 조건 / 엣지

| 조건 | 동작 |
|------|------|
| 동시에 같은 사용자가 여러 면접 생성 | 허용 (제한 없음). 정책: 허용 유지 |
| PDF 추출 실패 (encrypted / scanned) | `PdfTextExtractor` 가 BusinessException → 4xx |
| 10MB 초과 PDF | Spring Multipart 가 차단 → `MaxUploadSizeExceededException` |
| `techStack = null` | `Interview.getEffectiveTechStack()` 가 직무 디폴트 적용 |
| `csSubTopics` 임의 문자열 | ⚠️ 서버 화이트리스트 미적용 (정책-코드 갭, 추적: #404 #3) |
| 이벤트 발행 실패 | 트랜잭션 롤백 → INSERT 취소 |
| 질문 생성 비동기 실패 | `questionGenerationStatus = FAILED`, `failure_reason` 기록 → retry-questions API 로 재시도 |

---

## 상태 전이

```
(없음) → READY (status, INSERT 시점)
(없음) → PENDING (questionGenerationStatus, 같은 INSERT 시점)
PENDING → GENERATING → COMPLETED | FAILED  (외부 도메인이 갱신)
```

---

## 관찰성

- **로그**: `InterviewCreationService` — `면접 세션 생성 완료 (질문 생성 이벤트 발행): id={}, position={}, level={}, types={}` (INFO)
- **메트릭**: 직접 발행 없음. 후속 질문 생성 단계에서 `AiCallMetrics` 가 기록.
- **알람**: 본 액션 단독 알람 없음. 질문 생성 FAILED 비율 추적은 별도 이슈.

---

## 연관 의존성

| 패키지 / 클래스 | 역할 | 관계 |
|----------------|------|------|
| `com.rehearse.api.infra.ai.PdfTextExtractor` | PDF → 텍스트 동기 추출 | calls |
| `com.rehearse.api.global.util.FileHasher` | 이력서 SHA 해시 | calls |
| `com.rehearse.api.domain.interview.event.QuestionGenerationRequestedEvent` | 질문 생성 트리거 | event-publisher |
| `com.rehearse.api.domain.resume.exception.ResumeErrorCode` | RESUME_EXCLUSIVITY / FILE_EMPTY 매핑 | reads |
| `org.springframework.context.ApplicationEventPublisher` | 이벤트 발행 | calls |

---

## 정책 출처

- 비즈니스 룰: `docs/domain/interview/schema.md` `interview` 섹션 (status / questionGenerationStatus / 배타성 / tech stack 제약)
- 임계값:
  - `durationMinutes` 5..120 (`CreateInterviewRequest:36-37`)
  - PDF 10MB (`application-dev.yml:76`)
- ❓TODO(사용자 확인): `csSubTopics` 화이트리스트 검증 → Issue #404 #3 (A안: 서버 측 enum 검증 도입 예정)
