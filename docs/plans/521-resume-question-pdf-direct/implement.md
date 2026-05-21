# Resume 질문 생성 — PDF 직접 입력 전환 + Follow-up skeleton 비활성

## Context

현재 이력서 기반 질문 생성 = 2단계.
1. PDF → LLM 추출 → `ResumeSkeleton` (구조화 JSON) → DB 저장
2. skeleton JSON → LLM → 메인질문 (opener + mains)

추출 단계에서 정보 손실 발생 가능 (LLM 이 PDF 일부 항목 누락 / 요약 / 오인식). 메인질문 품질 회귀 원인 후보. #515 PR 에서 skeleton 추출 시 PDF 직접 전달로 정확도 개선됨 — 동일 패턴을 질문 생성 단계까지 확장.

꼬리질문은 현재 `FollowUpQuestionWriter` 에서 skeleton.projects JSON 을 프롬프트에 주입. 그러나 mainQuestion + userAnswer + claims (AnswerAnalysis) 가 이미 답변 심화에 충분한 context. skeleton 주입 = 본 답변과 무관한 다른 프로젝트 정보 노출 → noise 가능. 또한 skeleton 자체가 LLM 추출물 → 부정확한 hint 가 꼬리질문 왜곡 위험.

목표: 메인질문 = PDF 직접 입력. 꼬리질문 = skeleton 의존 제거 (claim 기반). skeleton 추출 / DB / 코드는 보존 (복원 용이성).

## Goal

- `ResumeTrackInitiator` 가 PDF bytes 를 LLM 에 직접 전달해 opener 1개 + main N개 생성. skeleton 추출 호출 제거.
- `FollowUpQuestionWriter` skeleton 인자 항상 null. 프롬프트에서 `RESUME_SKELETON` 블록 빈 문자열 유지 (기존 null safe 동작 활용).
- 기존 `ResumeSkeleton` record / Entity / Repository / Persister / Sampler / Extractor / V24·V28 마이그레이션 = 코드 보존 (호출처만 제거).
- `InterviewService` 재시도 가드 (`RESUME_PLAN_RECOVERY_REQUIRED`) skeleton 부재 검증 로직 제거.
- 메인질문 LLM 호출 토큰 비용 합리 범위 유지 (PDF base64 1회 / 호출).
- `GeneratedResumeQuestionsSchema` (opener + main + depthType) 불변.

## Evidence

- `backend/src/main/java/com/rehearse/api/domain/question/service/ResumeTrackInitiator.java:54-131` — 현재 skeleton 의존 흐름
- `backend/src/main/java/com/rehearse/api/infra/ai/client/OpenAiResumeExtractorClient.java:41-65` — PDF base64 + Responses API (`input_file`) 호출 패턴 (재활용 대상)
- `backend/src/main/java/com/rehearse/api/infra/ai/adapter/OpenAiResumeSkeletonExtractor.java:55-64` — extractor adapter 호출 흐름
- `backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpQuestionWriter.java:36-83` — skeleton null safe 직렬화 이미 존재
- `backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpService.java:82-112` — skeleton 로드 위치
- `backend/src/main/java/com/rehearse/api/domain/interview/service/InterviewService.java:66-78` — skeleton 부재 가드
- `backend/src/main/java/com/rehearse/api/infra/ai/context/FocusHints.java:1-42` — Hints 시그니처
- `backend/src/main/java/com/rehearse/api/infra/ai/context/layer/FocusLayer.java:54-59` — 메인질문 프롬프트
- `backend/src/main/java/com/rehearse/api/infra/ai/schema/GeneratedResumeQuestionsSchema.java` — 응답 스키마 (불변)

## Trade-off

| 축 | 채택 | 포기 대안 | 사유 |
|---|---|---|---|
| skeleton 처리 | 호출 경로 바이패스 (코드/DB 보존) | 완전 제거 / Feature flag | 복원 용이 + simplicity 균형. flag = premature toggle |
| PDF 전달 경로 | 전용 클라이언트 신설 (extractor 패턴 복제) | AiClient.chat 확장 | AiClient 오염 회피. Claude fallback 미지원 PDF — 이중화 깨짐 위험. 단일 OpenAI 의존 명시 |
| follow-up skeleton | null 고정 | topicTag 노출 보강 / skeleton 유지 | claim + dimensionGaps + weakestDimension 이미 충분. 정보 중복 / noise 회피. 품질 회귀 발견 시 단계적 보강 |
| opener 생성 | 메인질문과 동일 LLM 호출 (변경 없음) | 별도 호출 분리 | OPENER_COUNT=1 고정 + follow-up skip → 분리 이득 없음 |

## Tasks

### Task 1: PDF 직접 입력용 전용 클라이언트 + adapter 신설

- Implement: `backend` — `OpenAiResumeQuestionGeneratorClient` (infra/ai/client) + `OpenAiResumeQuestionGenerator` adapter (infra/ai/adapter) + port 인터페이스 (`domain/question/models/service/ResumeQuestionGenerator`)
- Review: `code-reviewer-backend` — extractor 패턴 일관성 / port 책임 분리 / strict schema / 토큰 로깅 / 예외 매핑
- 핵심:
  - `OpenAiResumeExtractorClient` 구조 복제 (base64 PDF + Responses API `input_file` + `response_format=json_schema`)
  - 입력: systemPrompt + base64Pdf + openerCount + mainCount + position + techStack
  - 응답: `GeneratedResumeQuestions` (기존 DTO 재사용)
  - JSON Schema = `GeneratedResumeQuestionsSchema.spec()` (불변 재사용)
  - 설정: `OpenAiResumeQuestionGeneratorProperties` (model id / max tokens / temperature) — `application-*.yml` 매핑. extractor properties 패턴 복제

### Task 2: 메인질문 생성 프롬프트 재작성

- Implement: `backend` — `FocusLayer.buildResumeQuestionGenerator` 또는 신규 system prompt
- Review: `code-reviewer-backend` — 지시문 명확성 / opener·main 동시 생성 / depthType 강제 / 토큰 효율
- 핵심:
  - 기존 `<<<RESUME_SKELETON>>>` 블록 제거. PDF 직접 첨부 전제 지시문으로 교체
  - 지시문: "첨부된 이력서 PDF 를 직접 읽고 opener 1개 + main N개 JSON 응답. main 은 depthType 필수 (5종 강제)"
  - position / techStack / OPENER_COUNT / MAIN_COUNT / PRIMARY_PROJECT_NAME 유지 (PRIMARY_PROJECT_NAME = LLM 응답에 포함시키거나 제거 — 사용처 재검토 시 결정)
  - call_type 신규 정의 (예: `resume_question_generator_v2`) 토큰 측정 / 비용 분리

### Task 3: ResumeTrackInitiator 재배선

- Implement: `backend`
- Review: `code-reviewer-backend` — 트랜잭션 경계 / 실패 처리 / 로그 컨벤션 / SRP
- 핵심:
  - `ResumeIngestionService.ingestPdf` 호출 제거
  - `ResumeSkeletonSampler.sampleDecisions` 제거
  - `serializeSkeleton` / `primaryProjectName` 추출 로직 제거
  - 신규 port (`ResumeQuestionGenerator`) 주입 → `generate(pdfBytes, openerCount, mainCount, position, techStack)` 호출
  - 기존 `persistGenerated` / `transactionHandler.completeGeneration` 흐름 유지
  - `resumePdfBytes` 매개변수 → 신규 port 직접 전달

### Task 4: FollowUp skeleton 의존 제거

- Implement: `backend`
- Review: `code-reviewer-backend` — null safe / FocusHints 불변 / 회귀 테스트
- 핵심:
  - `FollowUpService.java:86` `resumeSkeletonStore.findByInterviewId(id)` 호출 제거. skeleton 변수 자체 제거
  - `FollowUpQuestionWriter.write(...)` 시그니처에서 `ResumeSkeleton` 매개변수 제거. `serializeSkeleton` 메서드 + `ObjectMapper` 의존 제거 (orphan 정리)
  - `FocusHints.FollowUpGeneratorV3Hints` 의 `resumeSkeletonJson` 필드 제거
  - `FocusLayer.java:152~160` follow-up 프롬프트에서 `<<<RESUME_SKELETON>>>` 블록 제거
  - 비고: ResumeSkeleton record / Entity / Repository / Persister / Sampler / Extractor / V24·V28 / FocusHints.ResumeQuestionGeneratorHints — **코드/스키마 보존** (호출처만 제거. dead code 발견 사항으로 보고)

### Task 5: InterviewService 재시도 가드 변경

- Implement: `backend`
- Review: `code-reviewer-backend` — 가드 누락 / 회귀 시나리오
- 핵심:
  - `InterviewService.java:66-78` 의 skeleton 존재 검증 제거
  - 재시도 시 PDF 재업로드 흐름이 보장되는지 확인 후 대체 가드 (예: 인터뷰 PDF 해시 보존 여부) 명시 또는 검증 자체 제거
  - 모호 시 사용자 확인 (가드 의도 = 인터뷰 재진입 시 컨텍스트 보존 여부)

### Task 6: 테스트 작성 / 갱신

- Implement: `backend` (구현과 동시 작성)
- Review: `code-reviewer-backend` — testing.md 카테고리 / Support 재사용 / Mock 정책
- 핵심:
  - **Domain Unit**: `ResumeTrackInitiator` (skeleton 호출 제거 동작) — Repository / 신규 port Mock 허용. 외부 LLM Mock
  - **Infra Integration**: `OpenAiResumeQuestionGeneratorClient` 2-tier (WireMock + Live `@Disabled` 기본)
  - **Service Integration**: `FollowUpService` skeleton 제거 후 follow-up 정상 생성 (TRUNCATE @BeforeEach)
  - **E2E**: 기존 면접 시작 → 메인질문 생성 → 답변 → 꼬리질문 시나리오 1건 (Mock LLM). Live 1건 (`RUN_LIVE_API=true` 시)
  - 기존 skeleton 의존 테스트 — skeleton 호출 검증 부분 제거. skeleton record 자체 테스트는 보존
  - `TestFixtures` 에 PDF bytes 픽스처 추가 (필요 시)

### Task 7: 운영 / 관측

- Implement: `backend`
- Review: `code-reviewer-backend` — 로깅 컨벤션
- 핵심:
  - `ResumeTrackInitiator` 로그 — skeleton 관련 키 제거. PDF 크기 / 응답 토큰 / 호출 시간 로그 추가
  - call_type 신규명 (`resume_question_generator_v2`) 토큰 측정 dashboard 반영 — 비용 회귀 모니터
  - 실패 케이스 (PDF 손상 / 4xx / 5xx) 분기 명시

## Verification

- 빌드: `./gradlew build` 통과
- 단위 테스트: `./gradlew test --tests "ResumeTrackInitiator*" "FollowUpService*" "FollowUpQuestionWriter*" "OpenAiResumeQuestionGenerator*" "InterviewService*"`
- E2E: `./gradlew test --tests "InterviewE2E*"` (Mock LLM). Live (`RUN_LIVE_API=true`) 1회 수동
- 회귀 체크:
  - 메인질문 생성 — opener 1개 + main N개 + 각 main `depthType` 채워짐 (#520 정합)
  - 꼬리질문 생성 — skeleton 없이 정상 생성 (이력서 안 올린 인터뷰 = CS 트랙에서도 깨짐 없는지 확인)
  - 인터뷰 재시도 — skeleton 부재 가드 제거 후 정상 흐름
- 운영 검증:
  - dev EC2 배포 후 실제 PDF 업로드 → 질문 생성 1회 시연
  - 로그 — call_type `resume_question_generator_v2` 토큰 / latency 기록 확인
  - dead code (보존된 skeleton 모듈) 컴파일 / 정적분석 경고 발생 안 함

## Pre/Post Implementation State

### Pre

- `ResumeTrackInitiator` = PDF → skeleton 추출 → skeleton JSON → LLM → 메인질문
- `FollowUpService` = skeleton 로드 → writer 에 전달 → 프롬프트 주입
- `FocusHints.FollowUpGeneratorV3Hints` = `resumeSkeletonJson` 필드 포함
- `InterviewService` = 재시도 시 skeleton 부재 = `RESUME_PLAN_RECOVERY_REQUIRED` throw
- 메인질문 LLM call_type = `resume_question_generator`
- DB `resume_skeleton` 테이블 = 인터뷰별 1행 저장

### Post

- `ResumeTrackInitiator` = PDF → 신규 client (Responses API + base64) → 메인질문 (opener + mains)
- `FollowUpService` = skeleton 로드 코드 없음. writer 인자 skeleton 없음
- `FocusHints.FollowUpGeneratorV3Hints` = `resumeSkeletonJson` 필드 제거
- `InterviewService` = skeleton 부재 가드 제거 (또는 대체 가드)
- 메인질문 LLM call_type = `resume_question_generator_v2`
- DB `resume_skeleton` 테이블 = 보존, 신규 row 발생 안 함 (호출 경로 없음)
- 보존 dead code (호출처 없는 모듈) 목록 = 보고 — `ResumeSkeleton` record / Entity / Repository / Persister / Sampler / `ResumeIngestionService` / `ResumeExtractionService` / `OpenAiResumeSkeletonExtractor` / `OpenAiResumeExtractorClient` / `OpenAiResumeSkeletonProperties` / `GeneratedResumeSkeleton` / `FocusHints.ResumeQuestionGeneratorHints` / `FocusLayer.buildResumeQuestionGenerator` / `SkeletonCallType` / `MockResumeSkeletonExtractor` / V24·V28 마이그레이션

## 발견 사항 / 결정 보류

- PDF 직접 입력 = OpenAI 단일 의존. Claude fallback 미적용 → `ResilientAiClient` 이중화 우회. 운영 OpenAI 장애 시 메인질문 생성 실패. 보강 필요 시 별도 task (예: PDF → 텍스트 추출 fallback) — 본 plan 범위 외
- `primaryProjectName` 사용처 = `FocusHints.ResumeQuestionGeneratorHints` 1곳. 메인질문 프롬프트에서만 활용. PDF 직접 전환 시 LLM 자율 추출 가능 → 필드 제거 검토. Task 2 구현 중 확정
- `InterviewService.java:75` 가드 의도 (인터뷰 재진입 시 컨텍스트 보존) 사용자 확인 필요 — Task 5 진입 전 명확화
- skeleton 보존 dead code 정리 시점 = 본 plan 범위 외. 별도 PR (PDF 전환 검증 후 stabilization 단계)
