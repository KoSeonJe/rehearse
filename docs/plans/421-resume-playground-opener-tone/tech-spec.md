# Tech Spec — Resume Playground opener 톤 정합 + projectName 호명

> **작성자**: backend agent (Staff Engineer 페르소나, create-tech-spec 스킬)
> **답하는 질문**: 어떻게? 구조 / 변경 파일 / Trade-off / 검증
> **승인 게이트**: ★ 사용자 명시 승인 후 implement.md 진입 ★

---

## Why → Goal (1줄 미러)

Playground opener / Responder LLM 출력 톤을 intro / 자유 회고로 강제 + priority 1 프로젝트 projectName 호명 보장. Goal 측정 = fixture 단언 (프롬프트 가이드 + few-shot 텍스트 + 슬롯 직렬화).

## Evidence

- 현재 구조 (관련 파일 / 클래스):
  - `backend/src/main/resources/prompts/template/resume/resume-interview-planner.txt:74-105` — few-shot opener_question = narrow tech 톤. Playground 가이드 헤더 부재.
  - `backend/src/main/resources/prompts/template/resume/resume-playground-opener.txt:5-10, 31-32, 40` — 금지 어휘 일부만 ("내부 원리", "왜 그렇게 설계"). 폴백 가이드 (line 31-32 위임 / line 40 최종 폴백 문자열) 보존 대상.
  - `backend/src/main/resources/prompts/template/resume/resume-playground-responder.txt:5-10` — Responder 금지 어휘 동일 누락.
  - `backend/src/main/java/com/rehearse/api/infra/ai/prompt/ResumePlaygroundPromptBuilder.java:68-75` — `formatProjectInfo()` projectId + projectName + claims 카운트 + topics 카운트 (4 라인).
  - `backend/src/main/java/com/rehearse/api/infra/ai/context/layer/FocusLayer.java:70-83` — `<<<PROJECT_INFO>>>` 슬롯 직렬화. `projectInfo` 문자열 그대로 주입 → 빌더 단순화 시 자연스럽게 반영.
  - `backend/src/main/java/com/rehearse/api/domain/resume/service/PlaygroundModeHandler.java:39, 132-134` — Playground = priority 1 1 프로젝트 한정 (변경 영향 X).
- 기존 테스트 자산:
  - `backend/src/test/java/com/rehearse/api/domain/resume/service/ResumePlaygroundOpenerIntegrationTest.java` — L4 PROJECT_INFO 슬롯 검증 3 케이스. 단순화 후 단언 갱신.
  - `backend/src/test/java/com/rehearse/api/infra/ai/prompt/ResumeInterviewPlannerPromptBuilderTest.java` — slot replacement 단언. few-shot 텍스트 단언 추가 가능.
  - `backend/src/test/java/com/rehearse/api/domain/resume/service/PlaygroundModeHandlerTest.java` — 4 전환 조건 (변경 영향 X).
  - `backend/src/test/java/com/rehearse/api/e2e/ResumePlaygroundLiveLlmE2ETest.java` — `@Disabled` Live OpenAI. 수동 톤 단언 추가 대상.
- 컨벤션:
  - `backend/.claude/rules/conventions.md` — Lombok / 한국어 로깅 / Domain Unit 우선.
  - `backend/.claude/rules/testing.md` — Domain Unit + Integration + E2E 카테고리 매핑. Mock 자제 (외부 API 만).
- 사용자 결정 (product-spec / 본 spec 작성 중):
  - PROJECT_INFO 단순화 — projectName 만. claims / targetDomain LLM 입력 X (Playground = intro 단계, 기술 narrow X).
  - Responder Playground 1턴까지 scope 포함 — intro 톤 일관성.
  - Live LLM E2E 수동 단언 추가 — 실 톤 검증 채널 보존.

## Trade-offs

### Option A (채택): Mock 통합 + 프롬프트 텍스트 단언 + Live LLM E2E (수동) 단언 추가
- 장점:
  - CI 결정성 확보 (Mock 통합 + Domain Unit + 텍스트 리소스 단언으로 회귀 가드).
  - 회귀 가드 = 프롬프트 가이드 / few-shot 텍스트 / 금지 어휘 헤더 자체 단언 → LLM 출력 비결정 의존 X.
  - Live E2E (`@Disabled`, OPENAI_API_KEY 필요) 로 실제 톤 검증 옵션 보존.
- 단점:
  - Mock 단언 = "프롬프트에 가이드 / 금지 어휘 / intro few-shot 포함" 만 보장. 실제 LLM 출력 톤은 Live 수동 의존.
- 채택 사유: product-spec Goal #2 / Non-Goals ("자동 의미 평가 부재") + 어휘 목록 = 최소 안전망 정의 정합. CI 결정성 우선. 사용자 명시 결정 (Live E2E 추가).

### Option B (폐기): Mock LLM 응답 stub 에 narrow 어휘 박은 단언
- 장점: 단순 구현.
- 단점: stub 자체에 어휘 박는 단언 = 회귀 가드 의미 0 (테스트 작성자가 stub 만 안 바꾸면 통과).
- 폐기 사유: product-spec AC 의 회귀 탐지 의도 미충족.

### Option C (폐기): Live LLM 만 (CI 강제)
- 장점: 실제 톤 검증.
- 단점: 비용 + 비결정 + CI 신뢰성 ↓.
- 폐기 사유: 운영 비용 / 결정성 트레이드오프 부적절.

## Architecture

변경 없음 (touchpoint = 4건, 호출 흐름 그대로):

```
[ResumeInterviewPlannerPromptBuilder] (backend/src/main/java/com/rehearse/api/infra/ai/prompt/ResumeInterviewPlannerPromptBuilder.java)
   → resume-interview-planner.txt (★ few-shot 교체 + Playground opener_question 가이드 헤더 추가)
   → LLM → ProjectPlan.playgroundPhase.openerQuestion (intro 톤)
                     │
                     ▼
[PlaygroundModeHandler.handleOpener] (backend/.../resume/service/PlaygroundModeHandler.java:35-62)
   → ResumePlaygroundPromptBuilder.buildOpener
       → formatProjectInfo(Project)  ★ 1 라인 (projectName 만) ← 단순화
       → FocusLayer.buildResumePlaygroundOpener
           → <<<PROJECT_INFO>>> 슬롯 = "projectName: {name}"
       → SYSTEM = resume-playground-opener.txt  ★ 금지 어휘 확장
   → LLM → PlaygroundOpenerResult.question (projectName 호명 + intro 톤)

[PlaygroundModeHandler.handle (Responder 1턴)]
   → ResumePlaygroundPromptBuilder.buildResponder
       → formatProjectInfo(Project)  ★ 동일 단순화
       → FocusLayer.buildResumePlaygroundResponder
           → <<<PROJECT_INFO>>> 슬롯 = "projectName: {name}"
       → SYSTEM = resume-playground-responder.txt  ★ 금지 어휘 확장
   → LLM → PlaygroundResponderResult.question (감정·서사 oriented)
```

## Data Model

변경 없음. 스키마 / Entity / DTO 모두 그대로.

```
(Flyway 신규 마이그레이션 없음)
```

## API Contract

변경 없음 (외부 HTTP API 시그니처 영향 X — 내부 LLM 프롬프트 + Java private 메서드만).

## 핵심 변경 4건

### 변경 1: `resume-interview-planner.txt`

**1-1. 가이드 섹션 추가** (현 `## 작업 / ### 선정 원칙` 이후, `### 출력 스키마` 이전 삽입):

```
### Playground opener_question 톤 (강제)
- Playground = 응시자 맥락 파악 / 자유 회고 단계. opener_question 은 intro 톤만.
- 금지: "기술적 결정", "트레이드오프", "메커니즘", "어떻게 동작", "왜 그렇게 설계", "내부 원리" 류 narrow 기술 심문 어휘.
- 허용: "설명", "역할", "맡으셨", "흐름", "인상 깊은 / 어려웠던 경험" 류 intro / 감정·서사 oriented 어휘.
- project_name 명시된 경우 opener_question 자체에 자연스럽게 포함 (예: "{project_name} 프로젝트에서 어떤 역할을 맡으셨나요?").
```

**1-2. few-shot opener_question 교체** (line 82, 다중 프로젝트 매핑 발췌 line 108-116 영향 없음):

- 현재 (line 82): `"opener_question": "이 프로젝트에서 가장 어려웠던 기술적 결정을 설명해주세요."`
- 변경: `"opener_question": "__example_name__ 프로젝트에서 어떤 역할을 맡으셨고, 전체 흐름을 자유롭게 소개해주세요."`

### 변경 2: `resume-playground-opener.txt:5-10`

**2-1. 금지 어휘 확장** (line 8 교체):

- 현재: `**금지**: "왜 그렇게 설계했나요?", "내부 원리가 어떻게 되나요?" 류의 깊은 기술 심문 금지.`
- 변경: `**금지**: 다음 어휘 / 의도 모두 금지 — "왜 그렇게 설계", "내부 원리", "기술적 결정", "트레이드오프", "메커니즘", "어떻게 동작" 등 narrow 기술 심문.`

**2-2. 허용 톤 명확화** (line 10 보강):

- 현재: `**허용**: 이 프로젝트에서 어떤 역할이었는지, 가장 기억에 남는 경험, 전체 흐름 소개 요청.`
- 변경: `**허용**: 역할 / 흐름 / 인상 깊은 경험 / 자유 회고 — intro 톤 한정. 응시자가 자유롭게 본인 경험을 구술할 수 있는 open question 만.`

**2-3. 폴백 가이드 (line 31-32, 40)**: 변경 없음 (보존 대상 — 회귀 단언 기준).

### 변경 3: `resume-playground-responder.txt:5-10`

**3-1. 금지 어휘 확장** (line 7 교체):

- 현재: `**금지**: "왜 그렇게 설계했나요?", "내부 원리가 어떻게 되나요?" 류의 깊은 기술 심문.`
- 변경: `**금지**: "왜 그렇게 설계", "내부 원리", "기술적 결정", "트레이드오프", "메커니즘", "어떻게 동작" 등 narrow 기술 심문 어휘 / 의도 모두.`

**3-2. Responder 1턴 의도 명시** (line 6 다음 신규 라인 추가):

```
- Responder 1턴 의도: 응시자 직전 발화에 대한 감정·서사 oriented 후속 질문 (어려웠던 / 인상 깊은 / 기억나는 순간 회상). 기술 narrow 금지.
```

### 변경 4: `ResumePlaygroundPromptBuilder.formatProjectInfo()`

**4-1. 메서드 단순화** (`backend/src/main/java/com/rehearse/api/infra/ai/prompt/ResumePlaygroundPromptBuilder.java:68-75`):

- 현재 (4 라인 반환):
  ```java
  private static String formatProjectInfo(Project project) {
      String name = project.projectName();
      String safeName = (name == null || name.isBlank()) ? "" : name;
      return "projectId: " + project.projectId()
              + "\nprojectName: " + safeName
              + "\nclaims: " + project.claims().size() + "개"
              + "\nimplicitCsTopics: " + project.implicitCsTopics().size() + "개";
  }
  ```
- 변경 (1 라인 반환):
  ```java
  private static String formatProjectInfo(Project project) {
      String name = project.projectName();
      String safeName = (name == null || name.isBlank()) ? "" : name;
      return "projectName: " + safeName;
  }
  ```
- 영향 호출처: `buildOpener` (line 43) / `buildResponder` (line 58) 그대로. FocusHints 시그니처 변경 없음.

## Verification (완료 판정)

구현 완료 = 아래 모두 통과.

### product-spec AC ↔ Verification 매핑

| product-spec AC | 자동 가드 (CI) | 실 톤 가드 (수동 / Live) |
|---|---|---|
| AC1: 첫 질문에 priority 1 projectName 포함 | Integration 슬롯 단언 (PROJECT_INFO = "projectName: {name}") | Live E2E 단언 1 (실 LLM 출력에 fixture projectName 포함) |
| AC2: narrow 어휘 부재 (Opener + Responder 1턴) | Template 리소스 단언 (opener.txt / responder.txt 금지 어휘 6개 포함) + Planner few-shot 단언 (intro 톤 / narrow 부재) | Live E2E 단언 2 (실 출력 doesNotContain 금지 어휘 4개) |
| AC3: AND (금지 어휘 부재 AND 의도 어휘군 매치) — Opener: "역할/맡으셨/설명/소개" 1+ / Responder: "어려웠던/기억/인상/경험" 1+ | Template 리소스 단언 (허용 톤 헤더 / Responder 1턴 의도 라인 포함) + Planner few-shot 의도 어휘 매치 단언 | Live E2E 단언 3 (Opener / Responder 분리 어휘군 매치) |
| AC4: projectName 누락 폴백 보존 | Template 리소스 단언 — `opener.txt:31-32` (위임 가이드 핵심 문구) + `:40` (최종 폴백 문자열) 양쪽 |  — |
| AC5: PROJECT_INFO 슬롯 = projectName 만 | Integration 단언 (slot = `projectName:` 라인 포함 + `projectId:` / `claims:` / `implicitCsTopics:` `doesNotContain`) |  — |

자동 가드 = CI 결정성. 실 톤 가드 = 수동 (`@Disabled`, `OPENAI_API_KEY` 필요). product-spec Goal #1 / #2 의 "fixture 단언" = CI 가드 (입력 슬롯 / 프롬프트 텍스트 회귀 차단), 실 출력 톤 = Live 수동 검증 채널 (Non-Goals "자동 의미 평가 부재" 정합).

### 신규 / 갱신 테스트

- [ ] **Domain Unit (신규)**: `backend/src/test/java/com/rehearse/api/infra/ai/prompt/ResumePlaygroundPromptBuilderTest.java` — `formatProjectInfo()` 직접 검증은 `private static` → 통합 테스트 슬롯 단언으로 대체 (아래 Integration 항목 참조). 본 클래스 미신설.
- [ ] **Integration (Service, 갱신)**: `ResumePlaygroundOpenerIntegrationTest`
  - 갱신 케이스 1: `opener_invocation_propagates_explicit_project_name_to_l4_fragment` — assert 변경: `userFragment.content()` 가 `"projectName: " + explicitName` 포함 + `"projectId:"` / `"claims:"` / `"implicitCsTopics:"` **부재** 단언 추가.
  - 갱신 케이스 2: `opener_invocation_passes_blank_project_name_through_without_hallucination` — assert 변경: `userFragment.content()` 가 `"projectName: "` (trailing 공백 / 줄바꿈 무관) 포함 + 카운트 라벨 부재 단언.
  - 신규 케이스: `responder_invocation_propagates_simplified_project_info` — `buildResponder` 호출 시 `<<<PROJECT_INFO>>>` 슬롯도 동일 단순화 검증.
- [ ] **Domain Unit (신규)**: `ResumeInterviewPlannerPromptBuilderTest` 추가 케이스
  - `build_systemMessage_contains_playground_opener_tone_guide` — 빌드된 SYSTEM 텍스트에 "Playground opener_question 톤" 헤더 + "narrow 기술 심문" 키워드 포함 단언.
  - `build_fewshot_opener_question_uses_intro_tone` — few-shot 텍스트에 ["역할", "맡으셨", "소개"] 중 1+ 매치 + ["기술적 결정", "트레이드오프"] 부재 단언.
- [ ] **Domain Unit (신규)**: `ResumePlaygroundPromptTemplateTest` (신규 클래스, classpath 리소스 단언, Spring 컨텍스트 X — testing.md §Domain Unit 카테고리. Support 미사용 사유 = 단순 리소스 로드 단언만 수행, 환경 셋업 반복 없음)
  - `opener_template_includes_extended_forbidden_vocabulary` — `resume-playground-opener.txt` 클래스패스 로드 → ["기술적 결정", "트레이드오프", "메커니즘", "어떻게 동작"] 모두 포함 단언.
  - `responder_template_includes_extended_forbidden_vocabulary` — 동일 단언 (responder 파일).
  - `opener_template_includes_intro_allowed_tone_header` — 허용 톤 헤더 ("역할 / 흐름 / 인상 깊은 경험" 류 핵심 어구) 포함 단언. AC3 Opener 의도 어휘군 가드.
  - `responder_template_includes_emotion_oriented_intent_line` — Responder 1턴 의도 라인 ("어려웠던 / 인상 깊은 / 기억나는" 키워드 1+ 매치) 포함 단언. AC3 Responder 의도 어휘군 가드.
  - `opener_template_preserves_safe_fallback_text` — 회귀 가드 — line 40 폴백 문자열 ("이 프로젝트에서 가장 인상 깊었던 경험을 자유롭게 이야기해주세요.") 그대로 포함.
  - `opener_template_preserves_open_question_delegation_guide` — 회귀 가드 — line 31-32 위임 가이드 핵심 문구 ("가장 자신 있게 설명할 수 있는 프로젝트" / "임의 명칭" 류 부분 매치). AC4 위임 가이드 보존.
- [ ] **Live E2E (수동, `@Disabled`)**: `ResumePlaygroundLiveLlmE2ETest` 단언 추가 (기존 `buildOpener_returns_non_blank_question_from_live_openai`)
  - 추가 단언 1 (AC1): `result.question()` 에 fixture projectName ("Live 테스트 프로젝트") 문자열 포함.
  - 추가 단언 2 (AC2): `result.question()` 에 금지 어휘 목록 (["기술적 결정", "트레이드오프", "메커니즘", "어떻게 동작"]) 부재 — `assertThat(result.question()).doesNotContain(...)`.
  - 추가 단언 3 (AC3 Opener): `result.question()` 이 의도 어휘군 ["역할", "맡으셨", "설명", "소개"] 중 1+ 매치 — `assertThat(result.question()).containsAnyOf(...)`.
  - 추가 단언 4 (AC3 Responder, 신규 케이스 `buildResponder_returns_emotion_oriented_question_from_live_openai`): Live LLM 으로 Responder 호출 → 출력에 의도 어휘군 ["어려웠던", "기억", "인상", "경험"] 중 1+ 매치 + 금지 어휘 부재.
  - 실행 = 수동. 활성화 = `@Disabled` 어노테이션 유지 + `OPENAI_API_KEY` 환경변수 + JUnit 비활성 조건 우회 (`-Djunit.jupiter.conditions.deactivate=org.junit.*DisabledCondition`) — 기존 클래스 헤더 javadoc 명시 패턴 그대로. CI 미실행.

### 회귀 / 빌드

- [ ] `PlaygroundModeHandlerTest` 4 전환 조건 통과 (변경 영향 X 확인).
- [ ] 빌드 / 린트: `./gradlew test` (resume 도메인 + infra/ai/prompt 영역) → `./gradlew build`.
- [ ] 관찰: 기존 `[PlaygroundHandler] 오프너 생성: interviewId=..., projectId=..., questionId=...` 로그 변경 없음.

## Pre / Post State

### Pre (현재)
- `resume-interview-planner.txt`: few-shot opener_question = narrow tech ("기술적 결정"). Playground 가이드 헤더 부재.
- `resume-playground-opener.txt`: 금지 어휘 = "내부 원리" / "왜 그렇게 설계" 만.
- `resume-playground-responder.txt`: 금지 어휘 동일 일부만.
- `ResumePlaygroundPromptBuilder.formatProjectInfo()`: 4 라인 (projectId + projectName + claims 카운트 + topics 카운트).
- `ResumePlaygroundOpenerIntegrationTest`: PROJECT_INFO 슬롯 단언 = `projectName:` 라인 검증만 (카운트 라벨 부재 단언 없음).

### Post (구현 후)
- `resume-interview-planner.txt`: Playground opener_question 톤 가이드 헤더 추가 + few-shot opener_question intro 톤 ("__example_name__ 프로젝트에서 어떤 역할을 맡으셨고..."). 다중 프로젝트 매핑 규칙 (line 108-116) 영향 없음.
- `resume-playground-opener.txt`: 금지 어휘 = 6개 (확장). 허용 톤 (역할 / 흐름 / 인상 깊은 경험) 명시.
- `resume-playground-responder.txt`: 금지 어휘 = 6개. Responder 1턴 의도 (감정·서사 oriented) 명시.
- `ResumePlaygroundPromptBuilder.formatProjectInfo()`: 1 라인 (`"projectName: {safeName}"`).
- 테스트: PROJECT_INFO 카운트 부재 단언 추가 + Planner few-shot intro 톤 단언 + Template 리소스 금지 어휘 단언 + Live E2E 수동 단언 보강.

## 위험 / 마이그레이션 / 롤백

- 위험 1: planner.txt few-shot 교체로 LLM 이 다른 단계 (Interrogation chain) 톤 오염 가능 — **확인 결과 별도 프롬프트 (`resume-chain-interrogator.txt`)** 사용 → 영향 X.
- 위험 2: PROJECT_INFO 단순화 후 LLM 이 프로젝트 정체성 더 빈약 인지 → 본 작업 의도 (intro 톤 강화) 와 부합. claim text 노이즈 제거가 의도된 효과. 별도 회귀 X.
- 위험 3: 기존 `ResumePlaygroundOpenerIntegrationTest` 의 `projectName: \n` (newline) 문자열 매칭 단언 = FocusLayer wrap (`buildResumePlaygroundOpener` 가 `projectInfo + "\n<<<END_PROJECT_INFO>>>"` 로 감싸 줌) 덕분에 단순화 후에도 우연히 통과할 수 있음. 본 작업 의도 = 카운트 라벨 부재 + 단일 라인을 강하게 보장하는 것 → 의도 강화 위해 `doesNotContain("projectId:" / "claims:" / "implicitCsTopics:")` 단언 추가 (Verification 항목 참조). 기존 contains 단언은 호환성 보존 차원에서 유지 가능.
- 마이그레이션 전략: 신규 인터뷰부터 적용. backfill X (product-spec 비스코프). 운영 backfill 데이터 변경 X.
- 롤백 시나리오: PR revert. 4건 변경 = 단일 PR / 단일 커밋 분리 X (논리 단위 = "Playground intro 톤"). feature flag 불필요. 코드 / 프롬프트 단순 revert 로 즉시 복구.

## 분기 결정

- [x] **단일 영역 (BE) → `implement.md` 1개**
- [ ] BE+FE 동시
- [ ] BE 선행 강제

근거: 변경 = BE 프롬프트 텍스트 + Java private 메서드 / 테스트. FE / lambda / 외부 API 시그니처 영향 0. API contract 변경 X.
