# Tech Spec — 꼬리질문·답변채점 질문 카테고리 3-way 분기

> **작성자**: 구현 agent (Staff Engineer 초안 — Claude)
> **답하는 질문**: 어떻게? 구조 / 데이터 / Trade-off / 검증
> **승인 게이트**: ★ 사용자 명시 승인 후 implement.md 진입 ★

---

## Why → Goal (1줄 미러)

CS 개념 질문에 경험 전제 꼬리질문이 붙는 결함을, 질문 카테고리(`QuestionType` → concept/experience/resume)를 Step A(답변 분석)·Step B(꼬리질문) 양쪽에서 분기해 제거한다.

## Evidence

- 현재 구조:
  - 판정: `FollowUpService.java:109` `isResumeTrack(QuestionType)` — 이미 `QuestionType` 접두사 기준. `context.currentMainQuestionType()` 으로 7값 보유.
  - Step A 체인: `FollowUpService.generateFollowUp` → `AudioTurnAnalysisService.analyze(..., isResumeTrack)` → `AudioTurnAnalyzer`(port) → `OpenAi/Claude/Mock AudioTurnAnalyzer` / `TextFallbackTurnAnalyzer` → `AnswerAnalysisService.analyze(..., isResumeTrack)` → `AnswerAnalyzer`(port) → `Resilient/OpenAi/Claude/Mock AnswerAnalyzer` → `AnswerAnalysisPromptBuilder.build(..., isResumeTrack)` (`trackLabel = isResumeTrack ? "RESUME" : "CS"`, `AnswerAnalysisPromptBuilder.java:37`).
  - Step B 체인: `FollowUpService` → `FollowUpQuestionService.write(mainQuestion, analysis)` → `FollowUpQuestionGenerator`(port) → `OpenAi/Claude FollowUpQuestionGenerator` → `FollowUpQuestionPromptBuilder.build(mainQuestion, analysis)` (`FollowUpQuestionPromptBuilder.java:23` — **카테고리 입력 없음**).
  - 템플릿 로딩: `PromptTemplateLoader` (`callType → classpath path` Map + `GLOBAL_CORE` prepend, `system(callType)`).
  - 프롬프트: `answer-analyzer.txt` (`TRACK: CS` 8키 / `TRACK: RESUME` 10키 strict 강제), `follow-up-generator-v3.txt` (단일, few-shot 3종 inline).
- 추정/미확인 가정:
  - `GeneratedAnswerAnalysisSchema`(CS 8키 / RESUME 10키 정의)는 **미사용 dead code**. `OpenAiAnswerAnalyzerClient.java:73` 가 `response_format: {"type":"json_object"}` 만 설정 (json_schema strict 아님) — dimension 키셋 강제는 프롬프트 텍스트가 담당. grep 결과 schema 의 `build/spec` 호출처 0건. → 본 작업은 스키마 미수정, 프롬프트로 키셋 분기. (발견 사항: dead code 별도 정리 대상.)
- 컨벤션 제약: `infra/` 내 prompt builder/adapter 구조 유지. 로깅 한국어 + ID 컨텍스트 (`backend/.claude/rules/conventions.md`). 테스트 카테고리 매핑 (`testing.md`).
- 루브릭 정합 기준 (이미 카테고리별 차원 분리): `concept-cs-fundamental-rubric.yaml`, `experience-collaboration-rubric.yaml`, `experience-technical-rubric.yaml`, `resume-rubric.yaml`.

## Trade-offs

### 카테고리 판정 소스

**Option A (채택): `QuestionType` 7값 → `QuestionCategory` enum 매핑**
- 장점: 결정적(LLM 0). 7값 접두사로 모든 케이스 구별. 기존 `isResumeTrack(QuestionType)` 와 동일 소스 → 일관.
- 단점: 신규 enum + 매핑 메서드 추가.
- 사유: referenceType(2값)·rubricCategory(3값) 단독으론 resume·concept 분리 불가 (product-spec Evidence). QuestionType 만 충분.

**Option B (폐기): rubricCategory 3값 기준**
- 폐기 사유: `RESUME_MAIN`(rubricCategory=TECHNICAL)이 `TECH_MAIN`(concept)과 충돌, resume 흩어짐 → 본 결함 재발.

### Step A 시그니처 변경 방식

**Option A (채택): `boolean isResumeTrack` → `QuestionCategory category` 전면 교체**
- 장점: 의미 명확(2-state→3-state). resume 판정이 category 안으로 흡수. 분기 일원화.
- 단점: 체인 6+ 메서드 시그니처 변경 (port/adapter/service/builder).
- 사유: boolean 유지 + category 별도 추가는 동일 정보 이중 표현 → 혼선. SRP.

**Option B (폐기): boolean 유지 + category 파라미터 추가**
- 폐기 사유: `isResumeTrack` 과 `category==RESUME` 가 동일 의미를 중복 표현. 정합성 깨질 여지.

### Step B 템플릿 구조

**Option A (채택): follow-up 템플릿 3개 파일 분리 (concept/experience/resume)**
- 장점: 카테고리별 few-shot·프레이밍 자기완결. `PromptTemplateLoader` 의 callType→file 패턴 그대로. product-spec/Issue 의도("3종 템플릿") 일치.
- 단점: 보안/출력 규칙 일부 중복 (단, `GLOBAL_CORE` 가 공통 prepend → 실질 중복 최소).
- 사유: 카테고리별 프레이밍 차이가 본질(개념 심화 vs 경험 구체화 vs 이력서 정합)이라 파일 분리가 가독·유지 유리.

**Option B (폐기): 단일 템플릿 + 카테고리 조건 블록 주입**
- 폐기 사유: 한 파일에 3종 few-shot·분기 지시 혼재 → LLM 프레이밍 누수 위험(현 결함과 동류). 분리가 안전.

## Architecture

단일 BE 영역. 스키마/이벤트/FE 무변경.

```
[FollowUpService.generateFollowUp]
   │  QuestionCategory category = QuestionCategory.from(context.currentMainQuestionType())
   │
   ├─(Step A)→ AudioTurnAnalysisService.analyze(..., category)
   │             → AudioTurnAnalyzer(port) → OpenAi/Claude/Mock, TextFallbackTurnAnalyzer
   │               → AnswerAnalysisService.analyze(..., category)
   │                 → AnswerAnalyzer(port) → Resilient/OpenAi/Claude/Mock
   │                   → AnswerAnalysisPromptBuilder.build(..., category)
   │                       └ CATEGORY 라벨 + 카테고리별 dimension 키셋 주입
   │                         → answer-analyzer.txt (3-way 키셋)
   │
   └─(Step B)→ FollowUpQuestionService.write(mainQuestion, analysis, category)
                 → FollowUpQuestionGenerator(port) → OpenAi/Claude
                   → FollowUpQuestionPromptBuilder.build(mainQuestion, analysis, category)
                       └ category → PromptTemplateLoader.system(callType)
                         → follow-up-concept.txt | follow-up-experience.txt | follow-up-resume.txt
```

### 신규: `QuestionCategory` enum + 매핑

위치: `domain/question/entity/QuestionCategory.java` (`QuestionType` 동거). 매핑은 `QuestionType.category()` 인스턴스 메서드 (소유 enum이 자기 분류 책임).

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

### 카테고리별 dimension 키셋 (answer-analyzer.txt strict 강제)

CONCEPT·EXPERIENCE 는 해당 카테고리 **루브릭 `uses_dimensions` 에 정확히 일치** (분석↔채점 차원 정합). RESUME 는 **현행 10키 그대로 유지** (본 결함 무관 → 회귀 0).

| dimension | CONCEPT | EXPERIENCE | RESUME (현행 유지) |
|---|:---:|:---:|:---:|
| problem_framing | — | ✅ | ✅ |
| technical_depth | ✅ | — | ✅ |
| reasoning_communication | ✅ | ✅ | ✅ |
| conceptual_accuracy | ✅ | — | ✅ |
| practical_application | ✅ | — | ✅ |
| experience_concreteness | **—** | ✅ | ✅ |
| collaboration_awareness | **—** | ✅ | ✅ |
| recovery_from_gaps | ✅ | — | ✅ |
| factual_consistency | — | — | ✅ |
| chain_depth | — | — | ✅ |
| **키 수** | **5** | **4** | **10** |

- **CONCEPT (5)** = concept 루브릭 union (`concept-cs-fundamental` 4키: `technical_depth, reasoning_communication, conceptual_accuracy, recovery_from_gaps`) + (`concept-lang-framework` 의 `practical_application` 1키). 합계 5키: `technical_depth, reasoning_communication, conceptual_accuracy, practical_application, recovery_from_gaps`. 단, CS_FUNDAMENTAL 면접 채점은 `concept-cs-fundamental` 루브릭(4키)으로 수행되므로 **`practical_application` 은 분석 키셋에만 존재하고 채점 루브릭에는 미반영** (꼬리질문 `weakest_dimension` 후보 선정 목적으로만 활용). 핵심: `experience_concreteness`·`collaboration_awareness` **제외** → 개념 질문에서 경험 차원이 `weakest_dimension` 후보에서 원천 배제 (product-spec Goal/AC 직격).
- **EXPERIENCE (4)** = `experience-collaboration-rubric.yaml` 정확 일치: `problem_framing, reasoning_communication, experience_concreteness, collaboration_awareness`.
- **RESUME (10)** = 현행 `TRACK: RESUME` 키셋 무변경 (CS 8키 + `factual_consistency` + `chain_depth`). 결함 뿌리는 CONCEPT 이고 RESUME 변경은 회귀 부담만 증가 → product-spec "resume 회귀 없음" AC 자연 충족. (분석 10키 vs resume 루브릭 5키 불일치는 기존 상태로, 본 작업 범위 외 — 발견 사항 참조.)

## Data Model

**스키마 변경 없음.** `QuestionType` 메타데이터 기존. dimension 키셋 강제는 프롬프트 텍스트(`answer-analyzer.txt`)이며 `response_format=json_object` (json_schema strict 미사용). Flyway 마이그레이션 없음.

신규 파일:
- `domain/question/entity/QuestionCategory.java`
- `resources/prompts/template/follow-up-concept.txt`
- `resources/prompts/template/follow-up-experience.txt`
- `resources/prompts/template/follow-up-resume.txt`

삭제 파일:
- `resources/prompts/template/follow-up-generator-v3.txt` (단일 의존 제거 — Issue AC)

### 메트릭 라벨 (Step B)

`ResilientFollowUpQuestionGenerator` 의 `AiCallMetrics.recordCall` callType 라벨을 `follow_up_generator_v3` → `follow_up_generator` 로 변경 (버전 접미사만 제거, 카테고리 미반영 — 단일 라벨). 사유: 본 작업 Non-Goal(품질 측정 분리)이고 카테고리별 메트릭 분리는 스코프 외. 템플릿 로딩용 `PromptTemplateLoader` callType 상수(`FOLLOW_UP_CONCEPT|EXPERIENCE|RESUME`)와는 별개 — 메트릭 라벨은 카테고리 무관 단일 유지.

## API Contract

**변경 없음.** `GeneratedFollowUp` 응답 schema 불변 → 어댑터·FE 호환 (product-spec AC). 단일 BE 영역이라 신규 endpoint 없음.

## Verification (완료 판정)

> product-spec P1 위임 항목(검증 기준) 본 절에서 확정.

- [ ] **Domain Unit** (`QuestionTypeTest`): 7값 → category 매핑 전수 검증 (`TECH_*`→CONCEPT / `BEHAVIORAL_*`→EXPERIENCE / `RESUME_*`→RESUME). `@DisplayName` 한국어. (testing.md Domain Unit)
- [ ] **Infra Integration** (`AnswerAnalysisPromptBuilderTest`): category=CONCEPT 빌드 결과 user fragment 에 `experience_concreteness`·`collaboration_awareness` 키 미포함(5키만) + CATEGORY 라벨 정확. EXPERIENCE 는 4키(`problem_framing, reasoning_communication, experience_concreteness, collaboration_awareness`). RESUME 는 현행 10키 유지(`factual_consistency`·`chain_depth` 포함).
- [ ] **Infra Integration** (`FollowUpQuestionPromptBuilderTest`): category 별 `PromptTemplateLoader.system(callType)` 가 3종 중 정확한 템플릿 선택. concept 시스템 프롬프트에 경험 전제 few-shot 미포함.
- [ ] **Infra Integration** (`PromptTemplateLoaderTest`): 신규 3 callType 로드 성공 + 제거된 `follow_up_generator_v3` callType 호출 시 `IllegalStateException`.
- [ ] **빌드**: `./gradlew build` 통과.
- [ ] **회귀 (수동 fixture)**: concept 질문 답변 fixture **3종**(개념 정확/개념 모호/개념 부분) Live 호출 시 생성 꼬리질문에 경험 전제 표현("선택하셨나요", "경험에서", "프로젝트에서") **0건**. experience/resume fixture 각 2종 기존 의도 유지 확인. (`@EnabledIfEnvironmentVariable RUN_LIVE_API`)
- [ ] **토큰 회귀**: 변경 후 `answer-analyzer` + follow-up 호출 prompt 토큰이 현 `follow-up-generator-v3` 단일 기준선(로그 `토큰 사용량 prompt=` 값) 대비 카테고리당 ±15% 이내. 초과 시 few-shot 축소.

## Pre / Post State

### Pre (현재)
- 판정: `isResumeTrack(QuestionType)` boolean (2-state).
- Step A: `AnswerAnalysisPromptBuilder` → `TRACK: CS|RESUME` (8/10키 2-way).
- Step B: `FollowUpQuestionPromptBuilder.build(mainQuestion, analysis)` → `follow-up-generator-v3.txt` 단일.
- 결함: concept 질문에 경험 dim weakest → 경험 전제 꼬리질문.

### Post (구현 후)
- 판정: `QuestionType.category()` → `QuestionCategory{CONCEPT,EXPERIENCE,RESUME}` (3-state).
- Step A: `AnswerAnalysisPromptBuilder.build(..., category)` → `CATEGORY: CONCEPT|EXPERIENCE|RESUME` + 카테고리별 키셋 (CONCEPT 5 / EXPERIENCE 4 / RESUME 10).
- Step B: `FollowUpQuestionPromptBuilder.build(mainQuestion, analysis, category)` → `follow-up-{concept|experience|resume}.txt` 선택.
- 결과: concept 답변 분석에 경험 dim 부재 → 경험 전제 꼬리질문 제거.

## 위험 / 마이그레이션 / 롤백

- **위험**: (1) 체인 시그니처 광범위 변경(6+ 파일) — 컴파일러가 누락 포착. (2) 프롬프트 few-shot 분리 시 LLM 출력 형식 회귀 — Verification 회귀/토큰 항목으로 차단. (3) EXPERIENCE(=기존 BEHAVIORAL 질문) 가 현 CS 8키 → 4키로 축소 → 해당 질문 weakest 선정 분포 변동 가능 (회귀 fixture 로 확인). RESUME 는 무변경이라 회귀 위험 없음. (4) **[구조적 미정합] 분석 카테고리 축(`QuestionType.category()`)과 채점 루브릭 선택 축(`InterviewType` + `rubricCategory`)은 서로 다른 분류 체계.** experience-technical 채점 루브릭 경로는 `recovery_from_gaps` 를 포함하고 `collaboration_awareness` 가 없는 반면, 분석 EXPERIENCE 키셋(4키: `problem_framing, reasoning_communication, experience_concreteness, collaboration_awareness`)은 반대 구성으로 차원이 다름. RESUME_OPENER 는 `QuestionType.category()` = RESUME → follow-up skip 처리라 실질 영향이 제한적이나, 분석↔채점 분류 체계 자체가 구조적으로 불일치함을 인식하고 후속 검토 대상으로 남김.
- **마이그레이션**: 없음 (스키마/데이터 불변, 무상태 프롬프트).
- **롤백**: feature flag 불필요. 코드 + 프롬프트 파일 git revert 단일 PR 롤백. 진행 중 면접 세션 영향 없음(턴 단위 무상태).

## 분기 결정

- [x] **단일 영역 → `implement.md` 1개** (BE only. FE 무관 — schema 불변. 마이그레이션 없음.)
- [ ] BE+FE 동시
- [ ] BE 선행 강제

## 발견 사항

- `GeneratedAnswerAnalysisSchema` (`infra/ai/schema/`) = 미사용 dead code (`response_format=json_object` 라 strict schema 미적용). 본 작업 범위 외 — 별도 정리 PR 권장 (simplicity 룰: 명시 요청 시만 삭제).
- 분석 단계(answer-analyzer) RESUME 10키 vs 채점 단계(resume 루브릭) 5키 불일치 = 기존 상태. 본 결함(개념 질문)과 무관해 무변경 유지. resume 분석↔채점 정합은 별도 작업 후보.
