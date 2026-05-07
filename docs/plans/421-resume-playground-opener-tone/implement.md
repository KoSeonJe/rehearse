# Implement — Resume Playground opener 톤 정합 + projectName 호명

> **작성자**: backend agent (Staff Engineer 페르소나, create-implement-plan 스킬)
> **답하는 질문**: 어떤 순서로 실행?
> **사용 시점**: 단일 영역 (BE only). FE / lambda 영향 없음.
> **승인 게이트**: ★ 사용자 명시 승인 후 코드 작성 ★

---

## Phase / Step 개요

| Phase | 제목 | 구현 에이전트 | 예상 PR | 의존 |
|-------|------|--------------|--------|------|
| 1 | 프롬프트 리소스 변경 (planner + opener + responder) | `backend` | 단일 PR (Phase 1~3 합본) | - |
| 2 | PROJECT_INFO 슬롯 단순화 (Builder) | `backend` | 동일 PR | Phase 1 |
| 3 | 테스트 갱신 / 신규 + Live E2E 보강 | `backend` | 동일 PR | Phase 2 |

> 논리 단위 = "Playground intro 톤". 단일 PR / 커밋 3개 분리 (관심사: 프롬프트 / 빌더 / 테스트). 롤백 = PR revert 1회.
> 분리 임계 (Task 8+ / Phase 50줄+) 미달 → tasks/ 분리 X.

---

## Phase 1: 프롬프트 리소스 변경

- **구현**: `backend` — Playground opener 톤 가이드 / 금지 어휘 / few-shot 교체. 텍스트 리소스만 변경.

### 변경 파일

- `backend/src/main/resources/prompts/template/resume/resume-interview-planner.txt` — Playground opener_question 가이드 헤더 추가 (`### 출력 스키마` 이전 삽입) + few-shot opener_question 교체 (line 82). 다중 프로젝트 매핑 규칙 (line 108-116) 영향 X.
- `backend/src/main/resources/prompts/template/resume/resume-playground-opener.txt` — 금지 어휘 6개 확장 (line 8) + 허용 톤 명확화 (line 10). 위임 가이드 (line 31-32) / 최종 폴백 (line 40) 보존.
- `backend/src/main/resources/prompts/template/resume/resume-playground-responder.txt` — 금지 어휘 6개 확장 (line 7) + Responder 1턴 의도 라인 추가 (line 6 다음).

### 핵심 로직 / 변경 요약

tech-spec "핵심 변경 4건" §변경 1~3 그대로. 텍스트 diff:

**1) planner.txt 가이드 헤더 (신규)**:
```
### Playground opener_question 톤 (강제)
- Playground = 응시자 맥락 파악 / 자유 회고 단계. opener_question 은 intro 톤만.
- 금지: "기술적 결정", "트레이드오프", "메커니즘", "어떻게 동작", "왜 그렇게 설계", "내부 원리" 류 narrow 기술 심문 어휘.
- 허용: "설명", "역할", "맡으셨", "흐름", "인상 깊은 / 어려웠던 경험" 류 intro / 감정·서사 oriented 어휘.
- project_name 명시된 경우 opener_question 자체에 자연스럽게 포함 (예: "{project_name} 프로젝트에서 어떤 역할을 맡으셨나요?").
```

**2) planner.txt few-shot 교체 (line 82)**:
- 전: `"opener_question": "이 프로젝트에서 가장 어려웠던 기술적 결정을 설명해주세요."`
- 후: `"opener_question": "__example_name__ 프로젝트에 대해 설명해주시고, 어떤 역할을 맡으셨는지 설명해주세요."`

**3) opener.txt 금지 어휘 확장 (line 8)**:
- 전: `**금지**: "왜 그렇게 설계했나요?", "내부 원리가 어떻게 되나요?" 류의 깊은 기술 심문 금지.`
- 후: `**금지**: 다음 어휘 / 의도 모두 금지 — "왜 그렇게 설계", "내부 원리", "기술적 결정", "트레이드오프", "메커니즘", "어떻게 동작" 등 narrow 기술 심문.`

**4) opener.txt 허용 톤 (line 10)**:
- 후: `**허용**: 역할 / 흐름 / 인상 깊은 경험 / 자유 회고 — intro 톤 한정. 응시자가 자유롭게 본인 경험을 구술할 수 있는 open question 만.`

**5) responder.txt 금지 어휘 (line 7)** + **Responder 1턴 의도 라인 (line 6 다음 신규)**:
```
- Responder 1턴 의도: 응시자 직전 발화에 대한 감정·서사 oriented 후속 질문 (어려웠던 / 인상 깊은 / 기억나는 순간 회상). 기술 narrow 금지.
```

### 의존

- 선행 phase: 없음
- 외부 의존: 없음 (텍스트 리소스만)

### Verification Hook

- 명령: `./gradlew test --tests "ResumeInterviewPlannerPromptBuilderTest" --tests "ResumePlaygroundPromptTemplateTest"` (Phase 3 에서 신규 / 갱신 후 그린)
- Phase 1 단독 검증: 빌드 통과 (`./gradlew compileJava`) + 텍스트 grep 으로 금지 어휘 6개 / 가이드 헤더 / few-shot 교체 확인.
- 통과 기준: 컴파일 통과. 텍스트 diff 가 tech-spec §변경 1~3 와 일치.

### 커밋 메시지 (예상)

```
fix(BE): Resume Playground opener 톤 가이드 + 금지 어휘 확장
```

---

## Phase 2: PROJECT_INFO 슬롯 단순화

- **구현**: `backend` — `formatProjectInfo()` 4 라인 → 1 라인. claims / topics 카운트 / projectId 라벨 제거.

### 변경 파일

- `backend/src/main/java/com/rehearse/api/infra/ai/prompt/ResumePlaygroundPromptBuilder.java` — `formatProjectInfo(Project project)` private static 메서드 본체 교체 (line 68-75). `buildOpener` (line 43) / `buildResponder` (line 58) 호출처 그대로. FocusHints 시그니처 변경 X.

### 핵심 로직 / 변경 요약

tech-spec §변경 4 그대로:

```java
private static String formatProjectInfo(Project project) {
    String name = project.projectName();
    String safeName = (name == null || name.isBlank()) ? "" : name;
    return "projectName: " + safeName;
}
```

### 의존

- 선행 phase: Phase 1 (프롬프트 가이드와 슬롯 정합)
- 외부 의존: 없음

### Verification Hook

- 명령: `./gradlew test --tests "ResumePlaygroundOpenerIntegrationTest"`
- 통과 기준: Phase 3 갱신 단언 (slot 카운트 라벨 부재) 통과. 컴파일 통과.

### 커밋 메시지 (예상)

```
refactor(BE): ResumePlaygroundPromptBuilder.formatProjectInfo projectName 만 주입
```

---

## Phase 3: 테스트 갱신 / 신규 + Live E2E 보강

- **구현**: `backend` — Domain Unit (Template / Planner) 신규 + Integration 갱신 + Live E2E 단언 추가. testing.md 카테고리 매핑.

### 변경 파일

- `backend/src/test/java/com/rehearse/api/domain/resume/service/ResumePlaygroundOpenerIntegrationTest.java` — 갱신.
  - 갱신 케이스 1: `opener_invocation_propagates_explicit_project_name_to_l4_fragment` — `userFragment.content()` 에 `projectName: {explicitName}` 포함 + `projectId:` / `claims:` / `implicitCsTopics:` `doesNotContain` 단언 추가.
  - 갱신 케이스 2: `opener_invocation_passes_blank_project_name_through_without_hallucination` — 동일 카운트 라벨 부재 단언 추가.
  - 신규 케이스: `responder_invocation_propagates_simplified_project_info` — `buildResponder` 호출 시 동일 단순화 검증.
- `backend/src/test/java/com/rehearse/api/infra/ai/prompt/ResumeInterviewPlannerPromptBuilderTest.java` — 갱신 (신규 케이스 2개).
  - `build_systemMessage_contains_playground_opener_tone_guide` — SYSTEM 텍스트에 "Playground opener_question 톤" 헤더 + "narrow 기술 심문" 포함 단언.
  - `build_fewshot_opener_question_uses_intro_tone` — few-shot 텍스트 ["역할", "맡으셨", "소개"] 중 1+ 매치 + ["기술적 결정", "트레이드오프"] 부재 단언.
- `backend/src/test/java/com/rehearse/api/infra/ai/prompt/ResumePlaygroundPromptTemplateTest.java` — **신규** (Domain Unit, Spring 컨텍스트 X, classpath 리소스 단언만). Support 미사용 사유 = 환경 셋업 반복 없음 (단일 리소스 로드 단언). testing.md §Domain Unit 카테고리.
  - `opener_template_includes_extended_forbidden_vocabulary` — opener.txt 로드 → 금지 어휘 6개 모두 포함 단언.
  - `responder_template_includes_extended_forbidden_vocabulary` — responder.txt 동일 단언.
  - `opener_template_includes_intro_allowed_tone_header` — 허용 톤 헤더 ("역할 / 흐름 / 인상 깊은 경험" 핵심 어구) 포함. AC3 Opener 의도 어휘군 가드.
  - `responder_template_includes_emotion_oriented_intent_line` — Responder 1턴 의도 라인 ("어려웠던 / 인상 깊은 / 기억나는" 키워드 1+) 포함. AC3 Responder 의도 어휘군 가드.
  - `opener_template_preserves_safe_fallback_text` — line 40 폴백 문자열 회귀 가드.
  - `opener_template_preserves_open_question_delegation_guide` — line 31-32 위임 가이드 핵심 문구 회귀 가드. AC4 보존.
- `backend/src/test/java/com/rehearse/api/e2e/ResumePlaygroundLiveLlmE2ETest.java` — 갱신 (`@Disabled` 유지, OPENAI_API_KEY 필요).
  - 기존 `buildOpener_returns_non_blank_question_from_live_openai` 에 단언 추가:
    - 단언 1 (AC1): fixture projectName ("Live 테스트 프로젝트") 포함.
    - 단언 2 (AC2): 금지 어휘 ["기술적 결정", "트레이드오프", "메커니즘", "어떻게 동작"] `doesNotContain`.
    - 단언 3 (AC3 Opener): 의도 어휘군 ["역할", "맡으셨", "설명", "소개"] 중 1+ `containsAnyOf`.
  - 신규 케이스: `buildResponder_returns_emotion_oriented_question_from_live_openai` — Live LLM Responder 호출 → 의도 어휘군 ["어려웠던", "기억", "인상", "경험"] 1+ 매치 + 금지 어휘 부재.

### 핵심 로직 / 변경 요약

tech-spec "신규 / 갱신 테스트" 섹션 그대로 매핑. AC ↔ Verification 표 (tech-spec.md:170-180) 1:1 일치.

### 의존

- 선행 phase: Phase 2 (Builder 단순화 후 슬롯 단언 의미 발생)
- 외부 의존: Live E2E 는 OPENAI_API_KEY 환경변수 + 수동 활성화 (`@Disabled` 우회 = `-Djunit.jupiter.conditions.deactivate=org.junit.*DisabledCondition`)

### Verification Hook

- 명령:
  - `./gradlew test --tests "ResumePlaygroundOpenerIntegrationTest"`
  - `./gradlew test --tests "ResumeInterviewPlannerPromptBuilderTest"`
  - `./gradlew test --tests "ResumePlaygroundPromptTemplateTest"`
  - `./gradlew test --tests "PlaygroundModeHandlerTest"` (회귀)
  - 전체: `./gradlew test`
  - Live (수동, CI 미실행): `OPENAI_API_KEY=... ./gradlew test --tests "ResumePlaygroundLiveLlmE2ETest" -Djunit.jupiter.conditions.deactivate=org.junit.*DisabledCondition`
- 통과 기준: CI 자동 가드 모두 green. Live 수동 단언 = 운영자 직접 실행 후 출력 확인 (선택).

### 커밋 메시지 (예상)

```
test(BE): Resume Playground intro 톤 회귀 가드 추가
```

---

## 통합 Verification

- [ ] tech-spec.md Verification 항목 모두 통과 (AC ↔ Verification 매핑 표 기준)
- [ ] `./gradlew test` 전체 그린
- [ ] `./gradlew build` 통과
- [ ] 회귀: `PlaygroundModeHandlerTest` 4 전환 조건 그린 (변경 영향 X 확인)
- [ ] 관찰: 기존 `[PlaygroundHandler] 오프너 생성: ...` 로그 포맷 변경 없음

## 리뷰 게이트 (MANDATORY)

구현 완료 직후 지정 리뷰어 실행 강제. 스킵 = 위반.

- [ ] 지정 리뷰어 실행 (구현 완료 직후 — 메인 세션 책임)
  - BE only → `code-reviewer-backend`
- [ ] 컨벤션 위반 0건 (`backend/.claude/rules/conventions.md` + `backend/.claude/rules/testing.md`)
- [ ] Critical / Major 지적 = fix 반영 후 재리뷰
- [ ] Pre/Post State diff 일치 (tech-spec.md "Pre / Post State" 섹션 기준)
