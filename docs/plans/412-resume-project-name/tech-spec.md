# Tech Spec — 이력서 기반 질문에 프로젝트명 포함

> **작성자**: backend agent (Staff Engineer 페르소나)
> **답하는 질문**: 어떻게? 구조 / API / 데이터 / Trade-off
> **승인 게이트**: ★ 사용자 명시 승인 후 implement.md 진입 ★

---

## Why → Goal (1줄 미러)

이력서 추출 단계부터 다운스트림 (planner / 질문 빌더) 전 구간에 projectName 을 first-class 데이터로 흘려, 다수 프로젝트 인터뷰에서 "이 프로젝트" 식 지시 표현을 제거하고 면접자 식별 가능성을 확보한다.

## Evidence

- 현재 구조:
  - `backend/src/main/java/com/rehearse/api/domain/resume/entity/Project.java:5-9` — record 에 name 필드 부재.
  - `backend/src/main/java/com/rehearse/api/infra/ai/dto/ExtractedResumeSkeleton.java:32-41` — `ExtractedProject` DTO 에 `project_name` 키 부재.
  - `backend/src/main/resources/prompts/template/resume/resume-extractor.txt:18-42` — 추출 스키마에 프로젝트명 키 없음. Few-shot 예시도 부재.
  - `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeExtractionService.java:97-101` — `mapProject()` 매핑 누락.
  - `backend/src/main/java/com/rehearse/api/domain/resume/entity/ProjectPlan.java:17-19` — invariant `projectName != null && !blank`. 현재 LLM hallucinate 로 통과 중.
  - `backend/src/main/java/com/rehearse/api/infra/ai/prompt/ResumePlaygroundPromptBuilder.java:67-71` — `formatProjectInfo()` = projectId + claims size 만, name 부재.
  - `backend/src/main/java/com/rehearse/api/infra/ai/prompt/ResumeChainInterrogatorPromptBuilder.java:33-49` — chainTopic / level / quality 만 hints. project context 부재.
  - `backend/src/main/resources/prompts/template/resume/resume-chain-interrogator.txt:33-52` — `CURRENT_CHAIN` placeholder 만, project 정보 placeholder 없음.
  - `backend/src/main/resources/prompts/template/resume/resume-interview-planner.txt:71` — Few-shot `"project_name": "예시 프로젝트"` placeholder.
  - `backend/src/main/resources/db/migration/` — skeleton 테이블 = `skeleton_json` JSON column. DDL 변경 불필요.
- 외부 레퍼런스: 없음 (도메인 내부 결함).
- 사용자 발화 (특정 결정 근거):
  - 명칭 부재 시 fallback = claims 기반 LLM 요약 명칭.
  - backfill = null 허용 + 신규만 적용.
  - legacy skeleton 로드 시 ProjectPlan invariant 처리 = "프로젝트 N" placeholder 주입 (추출 영역 단일 책임).
  - 측정 = Mock fixture 통합 테스트.
- 추정 / 미확인 가정:
  - planner LLM 의 hallucinate 빈도 = 실측 미수집. Few-shot placeholder 모방 가능성을 추정으로 명시 (product-spec Evidence).
  - chain-interrogator / wrap-up 프롬프트가 project context 미주입 상태에서 어느 정도 비율로 지시 표현을 사용하는지는 fixture 통합 테스트로 측정.
  - `ExtractedResumeSkeleton.ExtractedProject` 클래스에 `@JsonIgnoreProperties(ignoreUnknown = true)` 이미 적용 (`ExtractedResumeSkeleton.java:31`) → 롤백 시 신규 적재된 `project_name` 키 무시 안전.

## Trade-offs

### Option A (채택): 추출 단계 단일 책임 — extractor LLM 이 명칭 보장

- 장점:
  - 추출 단계가 단일 진실 (single source of truth) → 다운스트림은 입력값 그대로 사용 (hallucinate 차단 + 룰 단순).
  - 명시 명칭 + 명칭 부재 fallback (claims 요약) 모두 LLM 자연어 → 사용자 친화적 한국어 명칭.
  - planner / playground / interrogator 가 동일한 projectName 보장 (skeleton 단일 소스).
- 단점:
  - extractor 프롬프트 / DTO / 매핑 / 다운스트림 prompt 전 구간 동시 변경 → 변경 범위 넓음.
  - LLM 요약 명칭은 비결정성 (fixture 검증 시 부분 매칭 필요).
- 사유:
  - 근본 원인 (skeleton 단계 누락) 제거가 product-spec WHY 직접 충족. 다운스트림 단발 fix 는 hallucinate 통과 구조를 그대로 두는 한계.

### Option B (폐기): planner 단계에서만 fallback 보강

- 장점: 변경 범위 작음 (planner prompt + adapter 만).
- 단점:
  - extractor 단계 누락 그대로 → 다른 다운스트림 (playground / interrogator) 은 여전히 컨텍스트 부재.
  - planner LLM 이 skeleton 입력 부재 시 어떻게 채울지 비결정 (placeholder 모방 / 자체 추정 / 사용자 환각 혼재).
- 폐기 사유: 표면 fix. 다운스트림 전파 불가 + factuality 약화 지속.

### Option C (폐기): app code 단 fallback (Java 단순 슬라이스)

- 장점: LLM 호출 1건 절감 / 결정성.
- 단점: claims[0].text 슬라이스 명칭은 자연스러움 부재 (예: "주문 조회 API p95 응답 800ms → Redis Cache-Aside" 같은 긴 설명 일부 잘림).
- 폐기 사유: extractor 가 이미 claims 컨텍스트를 보유 → 동일 프롬프트 내 요약 명칭 1줄 추가 비용 낮음. 자연스러움 우선.

## NF (11개)

| NF | 결정 | 근거 / 비고 |
|---|---|---|
| 영향 범위 | BE only | grep 결과 FE 파싱 영향 없음 (질문 텍스트 자연어만 변동) |
| 정합성 | 강 — extractor 단일 소스, planner adapter 가 skeleton 우선 강제 | hallucinate 차단 이중 (prompt 룰 + adapter 검증) |
| 실시간성 | 추출 1회 / 인터뷰. 평균 latency 변동 미미 | 스키마 1키 + 룰 추가 (gpt-4o-mini 토큰 영향 < 5%) |
| 부하 | extractor 토큰 소폭 증가. LLM latency 영향 무시 가능 | 운영 중인 extractor 호출 패턴 동일. 신규 호출 추가 없음 |
| 동시성 | skeleton 캐시 (file_hash) race 영향 없음 | mapProject 변경은 추출 결과 매핑 단 — race 노출 없음 (기존 동시성 모델 그대로) |
| 마이그레이션 | DDL X. JSON shape additive 진화 | 기존 row = mapProject placeholder graceful |
| 외부 의존 | OpenAI / Claude (기존 동일) | `ResilientAiClient` 단일 진입점 유지 |
| 보안 | 입력 변화 없음 (이력서 동일). **로깅 민감정보 (A09)**: projectName WARN 로그 → projectId 만 노출, projectName 자체는 로그 본문에서 제외 | 이력서 텍스트 일부가 LLM 요약 명칭에 포함될 가능성 → PII 회피. mismatch 로그도 길이 / hash 만 노출 |
| 관찰성 | 적재율 INFO 로그 (`projects=N, named=N`) + placeholder fallback / mismatch WARN | extractor / adapter 두 지점 |
| 롤백 | revert 안전. 신규 row `project_name` 키 = `@JsonIgnoreProperties(ignoreUnknown=true)` 무시 | feature flag 불필요 |
| 검증 | Domain Unit + Service Integration (Mock fixture 3) + Repository (JSON round-trip) + Live extractor 1건 (`RUN_LIVE_API`) + 토큰 측정 (`eval/context/measure_tokens.py`) | testing.md 카테고리 매핑 |

## Architecture

```
[ResumeExtractionService.extract]
       │
       ▼ buildChatRequest (resume-extractor.txt)
[OpenAI / Claude (ResilientAiClient)]
       │  ── 출력 스키마: projects[].project_name 필수
       │  ── 명시 명칭 → 그대로. 부재 → claims 요약 한국어 명칭
       ▼
[AiResponseParser → ExtractedResumeSkeleton]
       │  ── ExtractedProject.projectName 채움
       ▼
[ResumeExtractionService.mapProject]
       │  ── name=null 감지 시 "프로젝트 {index+1}" placeholder 주입 (legacy graceful)
       ▼
[Project record (name 추가, nullable 미허용 — placeholder 로 채움)]
       ▼
[ResumeSkeletonPersister → resume_skeleton.skeleton_json (JSON shape 진화)]
       │
       │  (인터뷰 시작)
       ▼
[ResumePlanPreparationService → ResumeInterviewPlanner]
       │  ── prompt 룰: project_name 은 SKELETON 입력값 그대로. 임의 생성·번역 금지
       ▼
[ResumeInterviewPlanAdapter → ProjectPlan(projectName)]
       │
       ├─→ [PlaygroundModeHandler]
       │       └─ ResumePlaygroundPromptBuilder.formatProjectInfo
       │              = projectId + projectName + claims size
       │       └─ resume-playground-opener.txt PROJECT_INFO 안에 자연 노출
       │
       ├─→ [InterrogationModeHandler]
       │       └─ ResumeChainInterrogatorPromptBuilder
       │              + ResumeChainInterrogatorHints 에 projectName 추가
       │       └─ resume-chain-interrogator.txt 신규 PROJECT_NAME placeholder
       │
       └─→ [WrapUp 핸들러]
               └─ ResumeWrapUpHints 에 projectName 추가
               └─ resume-wrap-up.txt placeholder 추가
```

핵심 제약: hallucinate 차단은 두 곳에서 동시 강제 — (1) planner 프롬프트 룰, (2) ResumeInterviewPlanAdapter 매핑 검증 (skeleton 의 projectName 과 일치 / 불일치 시 skeleton 값 우선 + WARN 로그).

Adapter mismatch 양쪽 처리 (모든 분기 graceful):
- 양쪽 동일 → 그대로.
- planner 값만 채워짐 (skeleton null — 이론상 발생 불가, 추출 단 placeholder 보장) → skeleton mapProject 단계에서 placeholder 주입 후 invariant 통과 → adapter 도달 시 skeleton non-null 보장.
- 양쪽 다 blank / null → invariant 위반 → `ResumeErrorCode.PROJECT_NOT_FOUND_IN_SKELETON` 또는 신규 `RESUME_PROJECT_NAME_INVALID` 로 BusinessException 발생. (mapProject 가 사전 차단하므로 도달 가능성 ~0).
- 불일치 → skeleton 우선 + WARN.

트랜잭션: `ResumeExtractionService.extract` 는 외부 LLM 호출 + 도메인 매핑 만 — 기존 컨벤션상 `@Transactional` 미적용 (조회·저장 없음). `ResumeSkeletonPersister` 가 별도 트랜잭션 경계. 변경 불필요.

## Data Model

DDL 변경 없음. JSON column shape 진화.

### `Project` record

```java
public record Project(
        String projectId,
        String projectName,
        List<ResumeClaim> claims,
        List<InterrogationChain> implicitCsTopics
) {
    public Project {
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalArgumentException("projectId 는 필수입니다.");
        }
        if (projectName == null || projectName.isBlank()) {
            throw new IllegalArgumentException("projectName 은 필수입니다.");
        }
        claims = claims == null ? List.of() : List.copyOf(claims);
        implicitCsTopics = implicitCsTopics == null ? List.of() : List.copyOf(implicitCsTopics);
    }
    // ...
}
```

`projectName` 비허용 (blank 거부) — placeholder 주입은 매핑 단에서 수행 후 invariant 통과.

### `ExtractedResumeSkeleton.ExtractedProject` DTO

```java
@JsonProperty("project_name")
private String projectName;
```

### Persisted JSON shape (예시)

```json
{
  "resume_id": "r_a1b2c3d4",
  "projects": [
    {
      "project_id": "p1",
      "project_name": "주문 API 캐싱 프로젝트",
      "claims": [...],
      "implicit_cs_topics": [...]
    }
  ]
}
```

기존 row (project_name 부재) 로드 시 `mapProject()` placeholder 주입 → 도메인 invariant 통과 후 메모리상 graceful. 영구 backfill 은 비스코프.

## API Contract

본 spec = BE only. 공개 API 시그니처 변경 없음. 인터뷰 / FollowUp 응답 본문은 동일.

> 참고: 다음 응답 텍스트의 자연어가 "이 프로젝트" → "{projectName} 프로젝트" 로 변경됨 (FE 파싱 영향 없음).

## Verification (완료 판정)

- [ ] **Domain Unit** (`DomainUnitSupport`):
  - `ProjectTest` — name 필수 invariant (null / blank reject + 정상 생성 path).
- [ ] **Service Integration** (`ServiceIntegrationSupport` — TRUNCATE @BeforeEach, 외부 API 만 Mock):
  - `ResumeExtractionServiceTest` Mock fixture 3 케이스 (`MockAiClient` 응답 stub):
    1. 다수 명시 프로젝트 → `projects[].projectName` 모두 명시값 일치.
    2. general 묶음 (project 1개, 명칭 부재) → `projectName` LLM 요약 명칭 채움 (non-blank 검증).
    3. 다수 프로젝트 + 일부 명칭 부재 → 부재 항목만 요약 명칭 fallback (Java placeholder 검증).
  - `ResumePlanPreparationServiceTest` — planner 출력 `ProjectPlan.projectName` 이 skeleton 의 `Project.projectName` 과 일치 (mismatch fixture 시 skeleton 우선 + WARN 로그).
  - `PlaygroundModeHandlerTest` (`ServiceIntegrationSupport`) — `ResumePlaygroundPromptBuilder` 를 spy 또는 캡처. `formatProjectInfo` 결과 문자열에 `projectName` 포함.
  - `InterrogationModeHandlerTest` (`ServiceIntegrationSupport`) — prompt builder hints 에 projectName 전달 검증.
- [ ] **Repository** (`RepositorySupport`):
  - `ResumeSkeletonRepositoryTest` (직접 작성 쿼리 있을 시) — JSON shape 직렬화 / 역직렬화 round-trip + `project_name` 존재 / 부재 양 케이스.
- [ ] **Infra Integration (Live)** (`@EnabledIfEnvironmentVariable RUN_LIVE_API=true`):
  - 실 LLM 으로 추출 1건 (다수 프로젝트 fixture) → `projects[].project_name` 모두 채워짐.
- [ ] **빌드 / 린트**: `./gradlew build`.
- [ ] **토큰 측정**: `python3 backend/eval/context/measure_tokens.py` — extractor 프롬프트 변경 전후 토큰 비교 (증가율 기록).
- [ ] **관찰 가능 동작** (보안 룰 A09 준수 — projectName 자체는 로그 본문에서 제외, 길이 / hash / projectId 만 노출):
  - extractor INFO 로그: `이력서 추출 완료: resumeId=..., projects=N, named=N` (적재율).
  - mapProject placeholder 주입 시 WARN: `[ResumeExtraction] projectName 부재 → placeholder 주입: projectId=p1, fallbackIndex=1` (실제 명칭 미노출).
  - planner adapter mismatch 감지 시 WARN: `[ResumeInterviewPlan] planner projectName mismatch: projectId=p1, skeletonLen=12, llmLen=8, skeleton 우선` (실제 명칭 미노출).
- [ ] **회귀 체크**:
  - 기존 `ResumeFollowUpService` / `ResumePlaygroundFlowTest` / `ResumeInterrogationFlowTest` 통합 테스트 모두 통과.
  - skeleton 캐시 적중 / 미적중 양 경로 정상.

## Pre / Post State

### Pre (현재)

- `Project` record `(projectId, claims, implicitCsTopics)` — name 부재.
- `ExtractedProject` DTO `(project_id, claims, implicit_cs_topics)` — project_name 부재.
- `resume-extractor.txt` 출력 스키마에 `project_name` 키 없음.
- `ResumeExtractionService.mapProject()` — name 매핑 없음.
- `ProjectPlan.projectName` invariant 필수, 입력 부재 → planner LLM hallucinate.
- `ResumePlaygroundPromptBuilder.formatProjectInfo` = projectId + claims 수 만.
- `ResumeChainInterrogatorPromptBuilder.build` hints = chainTopic / level / quality / answer / consecutiveStay (project 부재).
- `resume-chain-interrogator.txt` placeholder = `CURRENT_CHAIN` / `CURRENT_LEVEL` / `ANSWER_QUALITY` / `USER_ANSWER` / `CONSECUTIVE_STAY_COUNT`.
- 다수 프로젝트 인터뷰 질문 = "이 프로젝트" / "해당 프로젝트".

### Post (구현 후)

- `Project` record `(projectId, projectName, claims, implicitCsTopics)` — name 필수 invariant.
- `ExtractedProject` DTO `+@JsonProperty("project_name") String projectName`.
- `resume-extractor.txt` 출력 스키마 + 룰:
  - `projects[].project_name` 키 추가 (string, 필수).
  - 룰: "이력서에 명시된 프로젝트명이 있으면 그대로. 없으면 claims 요약 기반 짧은 한국어 명칭 (10~20자) 생성. 명칭 = 명사구. 빈 문자열 금지."
  - Few-shot 예시 갱신.
- `ResumeExtractionService.mapProject()` — `raw.projectName` 사용. null / blank 시 `"프로젝트 " + (index+1)` placeholder 주입 + WARN 로그. index = `mapProjects` 의 0-based 순서값. 동일 skeleton 내 placeholder 명칭 중복 발생 가능성은 `(projectId, fallbackIndex)` 조합으로 추적 (projectId 가 unique 보장).
- `resume-interview-planner.txt` 룰 추가:
  - "`project_name` 은 RESUME_SKELETON 입력의 해당 프로젝트 `project_name` 값을 그대로 사용. 임의 생성 / 번역 / 줄임 금지."
- `ResumeInterviewPlanAdapter` — planner 출력 projectName 이 skeleton.projectName 과 다를 경우 skeleton 값 우선 + WARN 로그.
- `ResumePlaygroundPromptBuilder.formatProjectInfo`:
  ```java
  return "projectId: " + project.projectId()
          + "\nprojectName: " + project.projectName()
          + "\nclaims: " + project.claims().size() + "개"
          + "\nimplicitCsTopics: " + project.implicitCsTopics().size() + "개";
  ```
- `ResumeChainInterrogatorHints` + builder + prompt:
  - hints record 에 `String projectName` 추가.
  - `resume-chain-interrogator.txt` 신규 placeholder `<<<PROJECT_NAME>>> {{PROJECT_NAME}} <<<END_PROJECT_NAME>>>` + 지시문 ("질문 텍스트에 프로젝트명을 자연스럽게 포함시키세요.").
  - InterrogationModeHandler 가 ChainStateTracker.currentProjectId → InterviewPlan.projectPlans 에서 projectName 조회 → builder 전달.
- `ResumeWrapUpHints` + `resume-wrap-up.txt` 동일 패턴 (projectName 주입).
- 다수 프로젝트 인터뷰 질문 = `"{projectName} 프로젝트에서 ..."` 자연 포함.

## 위험 / 마이그레이션 / 롤백

- **위험**:
  1. 추출 토큰 사용량 소폭 증가 (스키마 1키 + 룰 + Few-shot). gpt-4o-mini 가격 영향 미미. → 측정 (extractor 평균 토큰 비교).
  2. 기존 row 로드 시 placeholder 명칭 ("프로젝트 1") = UX 부자연. → product-spec 비스코프 (운영 backfill 별도 Issue).
  3. LLM 요약 명칭 비결정 → fixture 통합 테스트가 정확 일치 대신 non-blank + 길이 / 형태 검증.
  4. planner mismatch → adapter 가 skeleton 우선 강제 + WARN. 데이터 정합성 유지.
- **마이그레이션 전략**:
  - DDL X. JSON shape 진화 (additive 키). 기존 row 호환 (placeholder graceful).
  - 신규 인터뷰부터 정상 적재. file_hash 캐시 hit 인 기존 row = mapProject placeholder fallback.
- **롤백 시나리오**:
  - 코드 revert 시 신규 적재 row 의 `project_name` 키는 `@JsonIgnoreProperties(ignoreUnknown=true)` 설정으로 무시됨 (ExtractedResumeSkeleton 클래스 단). 회귀 무해.
  - extractor 프롬프트만 revert 가능 (DDL X).

## 분기 결정

- [x] **단일 영역 (BE)** → `implement.md` 1개.
- [ ] BE+FE 동시.
- [ ] BE 선행 강제.

FE 영향: 질문 텍스트 자연어만 변경 (프로젝트명 포함). FE 파싱 / UI 변경 불필요.
