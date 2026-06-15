# Implement — 꼬리질문·답변채점 질문 카테고리 3-way 분기

> **작성자**: 구현 agent (Staff Engineer 초안 — Claude)
> **답하는 질문**: 어떤 순서로 실행?
> **사용 시점**: 단일 영역(BE only). schema 불변·FE 무관·마이그레이션 없음 (tech-spec 분기결정).
> **승인 게이트**: ★ 사용자 명시 승인 후 코드 작성 ★

---

## Phase / Step 개요

| Phase | 제목 | 구현 에이전트 | 예상 PR | 의존 |
|-------|------|--------------|--------|------|
| 1 | `QuestionCategory` enum + `QuestionType.category()` 매핑 | `backend` | #N | - |
| 2 | Step A 3-way (답변 분석 체인 `isResumeTrack`→`category` + `answer-analyzer.txt` 키셋) | `backend` | #N | Phase 1 |
| 3 | Step B 3-way (꼬리질문 체인 `category` 추가 + 템플릿 3분리 + loader + v3 삭제) | `backend` | #N | Phase 1 |

> 3 Phase (< 8) · 단일 Phase 본문 < 50줄 → `tasks/` 분리 안 함. 단일 파일.
> **컴파일-green 순서 강제**: Phase 1 additive → Phase 2 Step A 전 체인 일괄 교체(green) → Phase 3 Step B 체인 일괄 추가(green). Phase 2/3 은 Phase 1 의존, 상호 독립(병렬 가능하나 동일 agent 순차 권장).

---

## Phase 1: `QuestionCategory` enum + `QuestionType.category()` 매핑

- **구현**: `backend` — 카테고리 판정 소스 도메인 enum + 자기 분류 메서드.

### 변경 파일
- `backend/src/main/java/com/rehearse/api/domain/question/entity/QuestionCategory.java` — 신규. `CONCEPT, EXPERIENCE, RESUME` 3값 enum.
- `backend/src/main/java/com/rehearse/api/domain/question/entity/QuestionType.java` — `category()` 인스턴스 메서드 추가 (7값 switch → 3 카테고리).
- `backend/src/test/java/com/rehearse/api/domain/question/entity/QuestionTypeTest.java` — 신규 (또는 기존에 케이스 추가). 7값 → category 전수 매핑 검증.

### 핵심 로직 / 변경 요약
```java
public enum QuestionCategory { CONCEPT, EXPERIENCE, RESUME }

// QuestionType
public QuestionCategory category() {
    return switch (this) {
        case TECH_MAIN, TECH_FOLLOWUP -> QuestionCategory.CONCEPT;
        case BEHAVIORAL_MAIN, BEHAVIORAL_FOLLOWUP -> QuestionCategory.EXPERIENCE;
        case RESUME_OPENER, RESUME_MAIN, RESUME_FOLLOWUP -> QuestionCategory.RESUME;
    };
}
```
- switch 가 enum exhaustive → 신규 QuestionType 추가 시 컴파일 강제 (미분류 0, AC 충족).

### 의존
- 선행 phase: 없음
- 외부 의존: 없음 (순수 도메인)

### Verification Hook
- 명령: `./gradlew test --tests "*QuestionTypeTest"`
- 통과 기준: 7값 전수 매핑 케이스 green. `@DisplayName` 한국어.

### 커밋 메시지 (예상)
```
feat(BE): QuestionCategory enum + QuestionType.category() 3-way 매핑 추가
```

---

## Phase 2: Step A 3-way — 답변 분석 체인 + answer-analyzer.txt 키셋

- **구현**: `backend` — `isResumeTrack`(boolean) → `QuestionCategory`(3-state) 전 체인 교체 + 카테고리별 dimension 키셋 분기.

### 변경 파일
**체인 시그니처 (`boolean isResumeTrack` → `QuestionCategory category`)**:
- `backend/.../domain/interview/models/service/AnswerAnalyzer.java` — port `analyze(...)` 시그니처.
- `backend/.../domain/interview/models/service/AudioTurnAnalyzer.java` — port `analyze(...)` 시그니처.
- `backend/.../domain/interview/service/AnswerAnalysisService.java` — `analyze(...)` 파라미터 + 전파.
- `backend/.../domain/interview/service/AudioTurnAnalysisService.java` — `analyze(...)` + textFallback 전파.
- `backend/.../domain/interview/service/TextFallbackTurnAnalyzer.java` — `analyze(...)` 파라미터.
- `backend/.../infra/ai/adapter/OpenAiAnswerAnalyzer.java` — port 구현 + builder 호출.
- `backend/.../infra/ai/adapter/ClaudeAnswerAnalyzer.java` — port 구현 + builder 호출.
- `backend/.../infra/ai/adapter/ResilientAnswerAnalyzer.java` — delegate 전파.
- `backend/.../infra/ai/MockAnswerAnalyzer.java` — 시그니처 맞춤.
- `backend/.../infra/ai/adapter/OpenAiAudioTurnAnalyzer.java` — port 구현 전파.
- `backend/.../infra/ai/MockAudioTurnAnalyzer.java` — 시그니처 맞춤.
- `backend/.../infra/ai/prompt/AnswerAnalysisPromptBuilder.java` — `build(..., category)`. `TRACK: CS|RESUME` → `CATEGORY: CONCEPT|EXPERIENCE|RESUME` 라벨.

**진입점**:
- `backend/.../domain/interview/service/FollowUpService.java` — `QuestionCategory category = context.currentMainQuestionType().category()` 도출, Step A 호출에 전달. `isResumeTrack(QuestionType)` private static 메서드 **제거** (대체됨). `RESUME_OPENER` skip 분기(line 53)는 `QuestionType` 직접 비교라 유지.

**프롬프트**:
- `backend/src/main/resources/prompts/template/answer-analyzer.txt` — `TRACK:` 라벨 규칙 → `CATEGORY:` 3-way 키셋. CONCEPT 5키(`technical_depth, reasoning_communication, conceptual_accuracy, practical_application, recovery_from_gaps`) / EXPERIENCE 4키(`problem_framing, reasoning_communication, experience_concreteness, collaboration_awareness`) / RESUME 10키(현행 유지). few-shot 예시 라벨 `TRACK:`→`CATEGORY:` 정합 + CONCEPT 예시의 경험 차원 키 제거.

**테스트 (시그니처 전파 갱신 + 신규)**:
- `backend/src/test/.../infra/ai/adapter/OpenAiAnswerAnalyzerTest.java`, `ClaudeAnswerAnalyzerTest.java`, `ResilientAnswerAnalyzerTest.java`, `MockAnswerAnalyzerTest.java`, `OpenAiAudioTurnAnalyzerTest.java`, `MockAudioTurnAnalyzerTest.java` — `category` 인자 갱신.
- `backend/src/test/.../infra/ai/prompt/AnswerAnalysisPromptBuilderTest.java` — 신규/갱신. CONCEPT 빌드 결과 user fragment 에 `experience_concreteness`·`collaboration_awareness` 미포함(5키) + `CATEGORY: CONCEPT` 라벨. EXPERIENCE 4키. RESUME 10키(`factual_consistency`·`chain_depth` 포함).

### 핵심 로직 / 변경 요약
- `AnswerAnalysisPromptBuilder.buildUserFragment`: `String categoryLabel = category.name()` → `"CATEGORY: " + categoryLabel`. 키셋 강제는 템플릿 텍스트가 담당 (현행 `response_format=json_object`, strict schema 미사용 — schema 클래스 미수정).
- 키셋 정합 기준 = 루브릭 YAML (CONCEPT=concept 루브릭 union, EXPERIENCE=experience-collaboration 정확 일치). RESUME 무변경 → resume 회귀 0.

### 의존
- 선행 phase: Phase 1 (`QuestionCategory`)
- 외부 의존: 없음 (Live LLM 은 Verification 수동 fixture 단계)

### Verification Hook
- 명령: `./gradlew test --tests "*AnswerAnalysisPromptBuilderTest" --tests "*AnswerAnalyzerTest" --tests "*AudioTurnAnalyzerTest"` + `./gradlew build`
- 통과 기준: CONCEPT 5키/경험 차원 부재 검증 green, 체인 컴파일 green.
- 관찰 가능 동작: CONCEPT 카테고리 답변 분석 시 `weakest_dimension` 후보에서 경험 전제 차원 원천 배제 (Verification 수동 fixture 는 통합 절에서).

### 커밋 메시지 (예상)
```
refactor(BE): 답변 분석 체인 isResumeTrack→QuestionCategory 3-way 전환 + answer-analyzer 키셋 분기
```

---

## Phase 3: Step B 3-way — 꼬리질문 체인 + 템플릿 3분리 + loader

- **구현**: `backend` — Step B 에 `category` 입력 추가 + 카테고리별 follow-up 템플릿 선택 + v3 단일 의존 제거.

### 변경 파일
**체인 시그니처 (`category` 파라미터 추가)**:
- `backend/.../domain/interview/models/service/FollowUpQuestionGenerator.java` — port `generate(..., category)`.
- `backend/.../domain/interview/service/FollowUpQuestionService.java` — `write(..., category)` + 전파.
- `backend/.../infra/ai/adapter/OpenAiFollowUpQuestionGenerator.java` — port 구현 + builder 호출.
- `backend/.../infra/ai/adapter/ClaudeFollowUpQuestionGenerator.java` — port 구현 + builder 호출.
- `backend/.../infra/ai/MockFollowUpQuestionGenerator.java` — 시그니처 맞춤.
- `backend/.../infra/ai/adapter/ResilientFollowUpQuestionGenerator.java` — delegate 전파 + 메트릭 라벨 `follow_up_generator_v3` → `follow_up_generator` (단일 라벨, 카테고리 미반영 — tech-spec §메트릭 라벨).
- `backend/.../infra/ai/prompt/FollowUpQuestionPromptBuilder.java` — `build(mainQuestion, analysis, category)`. category → `PromptTemplateLoader.system(callType)` 3종 중 선택.

**진입점**:
- `backend/.../domain/interview/service/FollowUpService.java` — Phase 2 에서 도출한 `category` 를 `followUpQuestionService.write(..., category)` 에 전달.

**템플릿 로더**:
- `backend/.../infra/ai/prompt/PromptTemplateLoader.java` — `FOLLOW_UP_GENERATOR_V3` 상수·매핑 제거. `FOLLOW_UP_CONCEPT`/`FOLLOW_UP_EXPERIENCE`/`FOLLOW_UP_RESUME` 3 상수 + `TEMPLATE_PATHS` 3 엔트리 추가. (`Map.of` → 엔트리 4개 = `ANSWER_ANALYZER` + 3.)

**프롬프트 (신규 3 + 삭제 1)**:
- `backend/src/main/resources/prompts/template/follow-up-concept.txt` — 신규. 개념 심화/명료화 프레이밍. 경험 전제 few-shot 미포함. CONCEPT 5키 dimension 기준.
- `backend/src/main/resources/prompts/template/follow-up-experience.txt` — 신규. 경험 구체화 프레이밍. EXPERIENCE 4키 기준 (현 v3 의 경험 계열 룰 이관).
- `backend/src/main/resources/prompts/template/follow-up-resume.txt` — 신규. 이력서 정합 프레이밍. RESUME 10키 기준 (현 v3 의 resume few-shot 이관, factual_consistency/chain_depth 유지).
- `backend/src/main/resources/prompts/template/follow-up-generator-v3.txt` — **삭제** (Issue AC).

**테스트 (시그니처 전파 + 신규)**:
- `backend/src/test/.../infra/ai/adapter/OpenAiFollowUpQuestionGeneratorTest.java`, `ClaudeFollowUpQuestionGeneratorTest.java`, `ResilientFollowUpQuestionGeneratorTest.java`, `MockFollowUpQuestionGeneratorTest.java` — `category` 인자 갱신.
- `backend/src/test/.../infra/ai/prompt/FollowUpQuestionPromptBuilderTest.java` — 신규/갱신. category 별 정확한 템플릿 선택. CONCEPT 시스템 프롬프트에 경험 전제 few-shot 미포함.
- `backend/src/test/.../infra/ai/prompt/PromptTemplateLoaderTest.java` — 신규/갱신. 3 신규 callType 로드 성공 + 제거된 `follow_up_generator_v3` callType `system()` 호출 시 `IllegalStateException`.

### 핵심 로직 / 변경 요약
```java
// FollowUpQuestionPromptBuilder
String callType = switch (category) {
    case CONCEPT -> PromptTemplateLoader.FOLLOW_UP_CONCEPT;
    case EXPERIENCE -> PromptTemplateLoader.FOLLOW_UP_EXPERIENCE;
    case RESUME -> PromptTemplateLoader.FOLLOW_UP_RESUME;
};
String system = templateLoader.system(callType);
```
- `GeneratedFollowUp` 응답 schema 불변 → 어댑터·FE 호환 (AC).

### 의존
- 선행 phase: Phase 1 (`QuestionCategory`). Phase 2 와 독립 (Step A/B 분리). FollowUpService 최종 wiring 은 Phase 2·3 모두 머지 후 정합.
- 외부 의존: 없음

### Verification Hook
- 명령: `./gradlew test --tests "*FollowUpQuestionPromptBuilderTest" --tests "*PromptTemplateLoaderTest" --tests "*FollowUpQuestionGeneratorTest"` + `./gradlew build`
- 통과 기준: 3 callType 로드 + v3 제거 예외 green, CONCEPT 템플릿 경험 few-shot 부재 green, 빌드 green.

### 커밋 메시지 (예상)
```
feat(BE): 꼬리질문 체인 QuestionCategory 분기 + follow-up 템플릿 3분리, v3 단일 의존 제거
```

---

## 통합 Verification

전체 작업 완료 판정. 상세 = `tech-spec.md` Verification 섹션 참조.

- [ ] tech-spec.md Verification 항목 모두 통과 (Domain Unit / Infra Integration 3종 / 빌드).
- [ ] **회귀 (수동 fixture, `RUN_LIVE_API=true`)**: concept 질문 답변 fixture 3종(개념 정확/모호/부분) Live 호출 → 생성 꼬리질문에 경험 전제 표현("선택하셨나요", "경험에서", "프로젝트에서") **0건**. experience/resume fixture 각 2종 기존 의도 유지.
- [ ] **토큰 회귀**: `answer-analyzer` + follow-up prompt 토큰이 현 `follow-up-generator-v3` 단일 기준선 대비 카테고리당 ±15% 이내 (로그 `토큰 사용량 prompt=`).
- [ ] `GeneratedFollowUp` schema 불변 확인 (어댑터·FE 무변경 호환).

## 리뷰 게이트 (MANDATORY)

구현 완료 직후 지정 리뷰어 실행 강제. 스킵 = 위반.

- [ ] 지정 리뷰어 실행 (구현 완료 직후 — 메인 세션 책임)
  - BE only → `code-reviewer-backend`
- [ ] 컨벤션 위반 0건 (`backend/.claude/rules/conventions.md` + `testing.md`)
- [ ] Critical / Major 지적 = fix 반영 후 재리뷰
- [ ] Pre/Post State diff 일치 (tech-spec.md Pre/Post State 절 기준)
