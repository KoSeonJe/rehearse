# Implement — 이력서 기반 질문에 프로젝트명 포함

> **작성자**: backend agent
> **답하는 질문**: 어떤 순서로 실행?
> **사용 시점**: 단일 영역 (BE only). DDL 변경 없음. JSON shape additive 진화.
> **승인 게이트**: ★ 사용자 명시 승인 후 코드 작성 ★
> **관련 plan**: `docs/plans/412-resume-project-name/tech-spec.md`
> **관련 Issue**: #412

---

## Phase / Step 개요

| Phase | 제목 | 구현 에이전트 | 예상 PR | 의존 | 커밋 |
|-------|------|--------------|--------|------|------|
| 1 | skeleton 단일 진실 확립 (record / DTO / extractor prompt / mapProject) | `prompt-engineer` (1a: extractor 템플릿) → `backend` (1b: record/DTO/service/test) | PR #1 | - | `feat(BE): Resume Skeleton 에 projectName 추출 추가` |
| 2 | planner 정합성 (prompt 룰 + adapter mismatch 검증) | `prompt-engineer` (2a: planner 템플릿 룰 / Few-shot) → `backend` (2b: adapter/errorcode/test) | PR #1 | Phase 1 | `feat(BE): Resume planner projectName skeleton 우선 강제` |
| 3 | 다운스트림 prompt context 주입 (Playground / ChainInterrogator / WrapUp) | `prompt-engineer` (3a: 3 템플릿 placeholder + 지시) → `backend` (3b: builder/handler/hints/test) | PR #1 | Phase 1 | `feat(BE): Resume 질문 빌더에 projectName 컨텍스트 주입` |
| 4 | 통합 검증 (Live extractor + 토큰 측정 + 회귀) | `backend` | PR #1 | Phase 1-3 | `test(BE): Resume projectName 통합 검증 + 토큰 측정` |

> Task 4개. 단일 `implement.md` (분리 임계 미초과).
> **에이전트 매핑 근거** (`.claude/agents/*.md` description 매칭):
> - `prompt-engineer` — "design, optimize, test, or evaluate prompts for LLMs ... templates, ... A/B testing, ... cost optimization" → 본 plan 의 prompt template 5개 (`resume-extractor.txt`, `resume-interview-planner.txt`, `resume-playground-{opener,responder}.txt`, `resume-chain-interrogator.txt`, `resume-wrap-up.txt`) 룰 / Few-shot / placeholder 설계.
> - `backend` — "Backend 구현 설계 + 신규 구현 / 리팩토링 / 테스트 작성 전담. Java 21 + Spring Boot 3.x" → record / DTO / service / adapter / handler / test.
> - Phase 1-3 = 협업 phase (a → b 순차). prompt-engineer 가 template diff 작성 후 backend 가 Java 코드 + placeholder 연결 + 테스트.

---

## Phase 1: skeleton 단일 진실 확립

- **구현 1a**: `prompt-engineer` — `resume-extractor.txt` 출력 스키마 / 룰 / Few-shot 설계. 명시 명칭 + claims 요약 명칭 양 케이스 처리. 토큰 증가율 < 10% 유지.
- **구현 1b**: `backend` — Project record / ExtractedProject DTO / mapProject placeholder + WARN 로그 / ProjectTest / ResumeExtractionServiceTest 3 케이스 + TestFixtures 보강. 1a 산출물 (`project_name` snake_case 키) 그대로 매핑.

### 변경 파일

- `backend/src/main/java/com/rehearse/api/domain/resume/entity/Project.java` — record 시그니처 변경 `(projectId, projectName, claims, implicitCsTopics)`. canonical constructor 에서 `projectName` null / blank reject.
- `backend/src/main/java/com/rehearse/api/infra/ai/dto/ExtractedResumeSkeleton.java` — `ExtractedProject` 클래스에 `@JsonProperty("project_name") private String projectName;` 추가.
- `backend/src/main/resources/prompts/template/resume/resume-extractor.txt` — 출력 스키마에 `project_name` 키 추가 (필수). 룰 추가 ("이력서 명시 명칭 우선. 부재 시 claims 요약 한국어 명사구 10~20자 생성. 빈 문자열 금지"). Few-shot 예시 갱신 (명시 / 부재 양 케이스 1+).
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeExtractionService.java` — `mapProjects()` 인덱스 전달. `mapProject(raw, index)` 에서 `raw.projectName` 사용. null / blank 시 `"프로젝트 " + (index+1)` placeholder 주입 + WARN 로그 (`projectId=p1, fallbackIndex=1` 만, 명칭 본문 미노출).
- `backend/src/test/java/com/rehearse/api/domain/resume/entity/ProjectTest.java` — name 필수 invariant 단위 테스트 (Domain Unit / `DomainUnitSupport`).
- `backend/src/test/java/com/rehearse/api/domain/resume/service/ResumeExtractionServiceTest.java` — Mock fixture 3 케이스 (`ServiceIntegrationSupport` + `MockAiClient` stub):
  1. 다수 명시 프로젝트 → 모두 명시값 일치
  2. general 묶음 단일, 명칭 부재 → 요약 명칭 채움 (non-blank)
  3. 다수 + 일부 부재 → 부재 항목 placeholder index fallback
- `backend/src/test/java/com/rehearse/api/support/fixtures/TestFixtures.java` — Resume skeleton fixture 팩토리 보강 (projectName 명시 / 부재 케이스).

### 핵심 로직 / 변경 요약

```text
Project (record):
    invariant: projectName non-blank

ResumeExtractionService.mapProject(raw, index):
    name = raw.projectName
    if (name == null || name.isBlank()):
        name = "프로젝트 " + (index + 1)
        log.warn("[ResumeExtraction] projectName 부재 → placeholder 주입: projectId={}, fallbackIndex={}",
                 raw.projectId, index + 1)
    return new Project(raw.projectId, name, claims, chains)

extract() 로그 보강:
    long named = projects.stream().filter(p -> !p.projectName().startsWith("프로젝트 ")).count()
    log.info("이력서 추출 완료: resumeId={}, projects={}, named={}", ...)
```

extractor 프롬프트는 `project_name` 키 + 룰 + Few-shot 갱신만. 보안 블록 / snake_case / JSON-only 규칙 그대로.

### 의존

- 선행 phase: 없음
- 외부 의존: 없음 (`ResilientAiClient` / `MockAiClient` 기존 활용)

### Verification Hook

- 명령:
  ```
  ./gradlew test --tests "ProjectTest"
  ./gradlew test --tests "ResumeExtractionServiceTest"
  ```
- 통과 기준: 모든 케이스 green. 3 fixture 케이스 모두 projectName 검증 통과.
- 관찰 가능 동작: extractor INFO 로그에 `named=N` 출력. placeholder 주입 시 WARN 발생.

### 커밋 메시지 (예상)

```
feat(BE): Resume Skeleton 에 projectName 추출 추가
```

---

## Phase 2: planner 정합성

- **구현 2a**: `prompt-engineer` — `resume-interview-planner.txt` 룰 추가 ("project_name 은 RESUME_SKELETON 입력 그대로 사용. 임의 생성 / 번역 / 줄임 금지") + Few-shot 갱신 (skeleton 입력 형태 반영).
- **구현 2b**: `backend` — `ResumeInterviewPlanAdapter` mismatch 4분기 graceful + skeleton 우선 + WARN 로그 + 신규 `RESUME_PROJECT_NAME_INVALID` ErrorCode + `ResumePlanPreparationServiceTest` (일치 / mismatch 케이스).

### 변경 파일

- `backend/src/main/resources/prompts/template/resume/resume-interview-planner.txt` — 룰 추가:
  - "`project_name` 은 RESUME_SKELETON 입력의 해당 프로젝트 `project_name` 값을 그대로 사용. 임의 생성 / 번역 / 줄임 금지."
  - Few-shot 예시 갱신 (skeleton 입력에 project_name 포함된 형태로).
- `backend/src/main/java/com/rehearse/api/infra/ai/adapter/ResumeInterviewPlanAdapter.java` — `toDomain()` (또는 mapProjectPlan 류) 에서 raw planner 출력의 projectName 과 skeleton 의 Project.projectName 비교. 불일치 시 skeleton 값 사용 + WARN 로그 (projectId / 길이만 노출). 양쪽 blank → mapProject placeholder 가 사전 차단하므로 도달 불가, 도달 시 BusinessException (`ResumeErrorCode.PROJECT_NOT_FOUND_IN_SKELETON` 재사용 또는 신규 `RESUME_PROJECT_NAME_INVALID`).
- `backend/src/main/java/com/rehearse/api/domain/resume/exception/ResumeErrorCode.java` — 신규 `RESUME_PROJECT_NAME_INVALID(HttpStatus.INTERNAL_SERVER_ERROR, "RESUME_0XX", "Project 명칭이 유효하지 않습니다.")` (도달 가능성 ~0 의 안전망).
- `backend/src/test/java/com/rehearse/api/domain/resume/service/ResumePlanPreparationServiceTest.java` — fixture:
  - 일치 케이스 → ProjectPlan.projectName == skeleton.projectName
  - mismatch 케이스 (planner stub 이 다른 명칭 반환) → skeleton 값 우선 + WARN 로그 검증

### 핵심 로직 / 변경 요약

```text
ResumeInterviewPlanAdapter.mapProjectPlan(rawPlan, skeleton):
    skeletonProject = skeleton.findProject(rawPlan.projectId)
    skeletonName = skeletonProject.projectName  // non-blank 보장 (Phase 1)
    llmName = rawPlan.projectName

    if (llmName == null || llmName.isBlank()):
        finalName = skeletonName
    else if (!skeletonName.equals(llmName)):
        log.warn("[ResumeInterviewPlan] planner projectName mismatch: projectId={}, skeletonLen={}, llmLen={}, skeleton 우선",
                 rawPlan.projectId, skeletonName.length(), llmName.length())
        finalName = skeletonName
    else:
        finalName = skeletonName

    return new ProjectPlan(projectId, finalName, priority, ...)
```

### 의존

- 선행 phase: Phase 1 (Project.projectName non-blank invariant 필요)
- 외부 의존: 없음

### Verification Hook

- 명령:
  ```
  ./gradlew test --tests "ResumePlanPreparationServiceTest"
  ./gradlew test --tests "com.rehearse.api.domain.resume.service.*"
  ```
- 통과 기준: mismatch fixture 에서 WARN 로그 + skeleton 값 우선 검증 green. 기존 ResumeInterviewPlanValidator / Planner 회귀 통과.
- 관찰 가능 동작: 새 ErrorCode 정의 + adapter mismatch WARN 로그 포맷 (projectName 본문 미노출).

### 커밋 메시지 (예상)

```
feat(BE): Resume planner projectName skeleton 우선 강제
```

---

## Phase 3: 다운스트림 prompt context 주입

- **구현 3a**: `prompt-engineer` — `resume-playground-opener.txt` / `resume-playground-responder.txt` 지시문 보강 + `resume-chain-interrogator.txt` / `resume-wrap-up.txt` `<<<PROJECT_NAME>>>` placeholder 블록 신규 + 지시문 ("질문 텍스트에 PROJECT_NAME 자연스럽게 포함").
- **구현 3b**: `backend` — `ResumePlaygroundPromptBuilder.formatProjectInfo` 보강 + `ResumeChainInterrogatorPromptBuilder` 시그니처 확장 + `FocusHints` (Chain / WrapUp) `projectName` 필드 + `InterrogationModeHandler` projectName 조회 로직 + WrapUp 동일 패턴 + spy 기반 테스트 2 케이스.

### 변경 파일

- `backend/src/main/java/com/rehearse/api/infra/ai/prompt/ResumePlaygroundPromptBuilder.java` — `formatProjectInfo(project)` 에 `projectName` 추가:
  ```java
  return "projectId: " + project.projectId()
          + "\nprojectName: " + project.projectName()
          + "\nclaims: " + project.claims().size() + "개"
          + "\nimplicitCsTopics: " + project.implicitCsTopics().size() + "개";
  ```
- `backend/src/main/resources/prompts/template/resume/resume-playground-opener.txt` — 지시문 보강 ("PROJECT_INFO 의 projectName 을 질문 텍스트에 자연스럽게 포함시킬 것").
- `backend/src/main/resources/prompts/template/resume/resume-playground-responder.txt` — 동일 지시문 보강.
- `backend/src/main/java/com/rehearse/api/infra/ai/context/FocusHints.java` — `ResumeChainInterrogatorHints` record 에 `String projectName` 필드 추가. `ResumeWrapUpHints` 동일.
- `backend/src/main/java/com/rehearse/api/infra/ai/prompt/ResumeChainInterrogatorPromptBuilder.java` — `build()` 시그니처에 `String projectName` 추가하여 hints 에 전달.
- `backend/src/main/resources/prompts/template/resume/resume-chain-interrogator.txt` — 신규 placeholder 블록:
  ```
  <<<PROJECT_NAME>>>
  {{PROJECT_NAME}}
  <<<END_PROJECT_NAME>>>
  ```
  지시문에 "심문 질문 텍스트에 PROJECT_NAME 을 자연스럽게 포함" 추가.
- `backend/src/main/resources/prompts/template/resume/resume-wrap-up.txt` — 동일 패턴 (`PROJECT_NAME` placeholder + 지시).
- `backend/src/main/java/com/rehearse/api/domain/resume/service/InterrogationModeHandler.java` — `withLock` 내부에서 `tracker.getCurrentProjectId()` → `plan.projectPlans()` 에서 projectName 조회 → `promptBuilder.build(..., projectName)` 전달.
- WrapUp 핸들러 (`docs/domain/resume` 참고하여 위치 식별 후 동일 패턴) — projectName 조회 + 전달.
- `backend/src/test/java/com/rehearse/api/domain/resume/service/PlaygroundModeHandlerTest.java` — `ResumePlaygroundPromptBuilder` spy / 캡처. `formatProjectInfo` 결과에 `projectName` 포함 검증.
- `backend/src/test/java/com/rehearse/api/domain/resume/service/InterrogationModeHandlerTest.java` — `ResumeChainInterrogatorPromptBuilder` spy. hints 의 projectName 전달 검증.

### 핵심 로직 / 변경 요약

```text
InterrogationModeHandler.handle:
    currentProjectId = tracker.getCurrentProjectId()
    projectName = plan.projectPlans().stream()
        .filter(pp -> pp.projectId().equals(currentProjectId))
        .findFirst()
        .map(ProjectPlan::projectName)
        .orElseThrow(...)
    result = promptBuilder.build(..., projectName, ...)
```

`PROJECT_NAME` placeholder rendering 은 `InterviewContextBuilder` / `executeJson` 단의 hints → template substitution 메커니즘 그대로 활용 (FocusHints record 필드명 = placeholder 키).

### 의존

- 선행 phase: Phase 1 (Project.projectName non-blank), Phase 2 (ProjectPlan.projectName 정합 보장)
- 외부 의존: 없음

### Verification Hook

- 명령:
  ```
  ./gradlew test --tests "PlaygroundModeHandlerTest"
  ./gradlew test --tests "InterrogationModeHandlerTest"
  ./gradlew test --tests "com.rehearse.api.domain.resume.service.*"
  ```
- 통과 기준: spy 기반 projectName 전달 검증 green. 기존 Playground / Interrogation flow 통합 테스트 회귀 통과.
- 관찰 가능 동작: 다수 프로젝트 fixture 인터뷰에서 prompt builder 호출 캡처 → projectName 컨텍스트에 포함.

### 커밋 메시지 (예상)

```
feat(BE): Resume 질문 빌더에 projectName 컨텍스트 주입
```

---

## Phase 4: 통합 검증 (Live + 토큰 + 회귀)

- **구현**: `backend` — 실 LLM 으로 신 스키마 검증 + 추출 토큰 증가율 측정 + 전체 회귀 테스트 + 빌드. (prompt 변경 산출물은 Phase 1-3 a 단계에서 확정 — 본 phase 는 검증 전담.)

### 변경 파일

- `backend/src/test/java/com/rehearse/api/infra/ai/ResumeExtractorLiveTest.java` (신규 또는 기존 Live 테스트 클래스 보강) — `@Disabled` 기본 + `@EnabledIfEnvironmentVariable(name="RUN_LIVE_API", matches="true")`. 다수 프로젝트 fixture 1건 → 실 LLM 호출 → `projects[].projectName` 모두 non-blank 검증 (`InfraIntegrationSupport`).
- `backend/eval/context/measure_tokens.py` (기존 스크립트) — 변경 없이 실행. 출력 비교를 plan 폴더에 메모.

### 핵심 로직 / 변경 요약

- Live 테스트는 키 부재 시 자동 skip (testing.md 룰).
- 토큰 측정: `python3 backend/eval/context/measure_tokens.py` 실행 → before / after 차이 기록.
- 회귀: 도메인 전체 + e2e support 1회 풀 실행.

### 의존

- 선행 phase: Phase 1-3 모두 완료
- 외부 의존: `OPENAI_API_KEY` (Live), Python 3 (`measure_tokens.py`)

### Verification Hook

- 명령:
  ```
  ./gradlew build
  RUN_LIVE_API=true ./gradlew test --tests "ResumeExtractorLiveTest"
  python3 backend/eval/context/measure_tokens.py
  ./gradlew test --tests "com.rehearse.api.domain.resume.*"
  ```
- 통과 기준: 빌드 / 단위 / 통합 / Live 모두 green. 토큰 증가율 < 10% (단순 스키마 1키 + 룰 추가 수준).
- 관찰 가능 동작: Live 테스트에서 실 추출 결과 JSON 의 `projects[].project_name` 적재 확인.

### 커밋 메시지 (예상)

```
test(BE): Resume projectName 통합 검증 + 토큰 측정
```

---

## 통합 Verification

- [ ] tech-spec.md Verification 항목 모두 통과 (`docs/plans/412-resume-project-name/tech-spec.md` Verification 섹션 참조)
- [ ] 추가 회귀 체크: 기존 file_hash 캐시 hit 인 legacy skeleton 로드 시 mapProject placeholder 주입 → ProjectPlan invariant 통과 + 인터뷰 정상 진행 (수동 확인 1건)

## 리뷰 게이트 (MANDATORY)

구현 완료 직후 지정 리뷰어 실행 강제. 스킵 = 위반.

- [ ] 지정 리뷰어 실행 (구현 완료 직후 — 메인 세션 책임)
  - BE only → `code-reviewer-backend`
- [ ] 컨벤션 위반 0건 (`backend/.claude/rules/conventions.md` + `backend/.claude/rules/testing.md`)
- [ ] Critical / Major 지적 = fix 반영 후 재리뷰
- [ ] Pre/Post State diff 일치 (`tech-spec.md` Pre/Post 섹션과 매칭)
