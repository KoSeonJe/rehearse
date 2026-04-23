# Plan 08: Rubric Family Scorer (M3 턴 채점 — 카테고리별 루브릭)

> 상태: Draft
> 작성일: 2026-04-20 (2026-04-20 오후 Rubric Family 재설계 반영)
> 주차: W7 (plan-09/10과 병렬)
> 원본: `docs/todo/2026-04-20/03-m3-feedback-rubric.md` (개정판 — Rubric Family)
> 참조: `./REMEDIATION.md` Addendum, `/Users/koseonje/.claude/plans/2026-04-20-todo-wobbly-bengio.md` Addendum

## Why

TODO 03 개정판의 핵심 통찰: **"하나의 5차원 루브릭으로 모든 질문을 평가하면 부적절한 차원 점수가 섞여 전체 신뢰도가 무너진다."** 네트워크 'TCP 3-way handshake' 질문에 "Experience Concreteness"를 적용하는 건 말이 안 되고, '팀 갈등 해결' 질문에 "Conceptual Accuracy"도 이상. 질문 카테고리마다 **의미 있는 차원이 다르다**.

구글/메타/Tech Interview Handbook 같은 업계 사례도 전부 "코딩 루브릭 + 행동 루브릭 + 시스템 디자인 루브릭" 식으로 분리 운영. Rehearse도 동일 패턴 채택.

본 plan-08은 **10개 차원 사전(D1~D10) + 7개 카테고리별 루브릭 YAML + 자동 매핑(QuestionSetCategory + FeedbackPerspective)** 으로 재설계. 플랜 초안의 단일 rubric 구조는 폐기.

### 2026-04-22 결정: Content 평가의 유일 소스

현재 Lambda Gemini analyzer가 `verbal` + `technical` 블록을 통해 기술 내용 평가를 수행 중 (`technical.accuracyIssues`, `technical.coaching.{structure,improvement}`, `verbal.{positive,negative,suggestion}`). Gemini는 questionSetCategory, intentType, resumeMode, currentChainLevel, resume 체인 컨텍스트를 받지 않아 레벨/의도 기준 정확성 판정이 원천 불가능하고, Rubric D1~D10 중 D2/D3/D4/D6가 **Gemini 블록과 중복**.

→ **plan-08 Rubric Scorer가 Content(기술 내용) 평가의 유일 소스**로 확정. Lambda `verbal`/`technical` 블록 제거는 **plan-13** 이 담당 (본 plan과 plan-09 flag-on과 동시 cut-over).

**본 plan의 범위 확정**: 기술 내용 루브릭(D1~D10)만 담당. 비언어 루브릭(D11~D14)은 **plan-11**, Lambda content 제거는 **plan-13** 에서 분리 처리.

## 전제 (Phase 0 선행 필수)

- plan-00a `IMPACT_MAP.md` — 신규 `backend/src/main/resources/rubric/` 디렉토리 및 `domain/feedback/rubric/` 패키지 기록 확인
- plan-00b `AiClient.chat()` — 채점 호출 (`callType = "rubric_scorer"`)
- plan-00c `V26 rubric_score` 스키마 — `rubric_id`, `scored_dimensions`, `level_flag` 컬럼 포함하여 확정
- plan-02 `AnswerAnalysis` — claims/depth_score/missing_perspectives를 채점 입력으로 재활용
- plan-07 `ResumeMode` + `ChainStateTracker.currentLevel` — Resume Track 채점 시 mode-aware 규칙 적용

## 후속 (연계 plan)

- **plan-13 Lambda Content Removal**: 본 plan flag-on 시점과 동시 cut-over. Lambda `verbal`/`technical` 블록 제거 + `TimestampFeedback` 컬럼 drop (V28). 본 plan은 content 평가 인수, plan-13은 이전 소스 정리를 담당 (분리 이유: 응급 롤백 범위 축소).
- plan-09 Feedback Synthesizer: 본 plan의 `turn_scores`를 content 평가 유일 소스로 받아 5섹션 합성.
- plan-11 Nonverbal Rubric: 비언어 D11~D14 결정론 매퍼. **본 plan과 독립** (기술/비언어 섞지 말 것).

## 생성/수정 파일

### 2026-04-22 개정: 카테고리 중심 재편 (9개 → 8개 YAML)

`lang-fw-backend` / `lang-fw-frontend` / `experience-backend` / `resume-backend` 도메인 분기 제거. 차원·가중치·per_turn_rules 가 도메인별로 동일해 분리가 redundant 하고, domain 을 backend/frontend 2개로만 고정하면 devops/data/fullstack/AI 등 확장이 막힘. 도메인 차이는 질문 텍스트 자체(Spring vs React vs Airflow)가 담고 있어 LLM 이 구분 가능. `_mapping.yaml` 은 `QuestionSetCategory` + `FeedbackPerspective` + `resumeTrack` 만으로 라우팅.

### 신규 리소스 (8개 YAML)
| 파일 | 작업 |
|------|------|
| `backend/src/main/resources/rubric/_dimensions.yaml` | 10차원 마스터 (D1 Problem Framing ~ D10 Chain Depth). 각 차원에 1~3점 scoring rubric + observable 행동 예시 |
| `backend/src/main/resources/rubric/_mapping.yaml` | QuestionSetCategory + FeedbackPerspective + Resume Track → rubric_id 선언적 매핑 |
| `backend/src/main/resources/rubric/concept-cs-fundamental-rubric.yaml` | CS_FUNDAMENTAL (NETWORK/DB/OS 통합). 4차원(D2/D3/D4/D8). NETWORK/DB/OS 세분화는 추후 별건 |
| `backend/src/main/resources/rubric/concept-lang-framework-rubric.yaml` | LANGUAGE_FRAMEWORK + UI_FRAMEWORK 통합. 5차원(D2/D3/D4/D5/D8). domain 무관 |
| `backend/src/main/resources/rubric/experience-technical-rubric.yaml` | FeedbackPerspective=EXPERIENCE (domain 무관). 5차원(D1/D2(conditional)/D3/D6/D8) |
| `backend/src/main/resources/rubric/experience-collaboration-rubric.yaml` | BEHAVIORAL. 4차원(D1/D3/D6/D7) |
| `backend/src/main/resources/rubric/resume-rubric.yaml` | RESUME_BASED / Resume Track (domain 무관). 5차원(D2/D3/D6/D9/D10) + mode-aware (PLAYGROUND/INTERROGATION/WRAP_UP) |
| `backend/src/main/resources/rubric/fallback-generic-rubric.yaml` | 매핑 실패 시. SYSTEM_DESIGN/INFRA_CICD/CLOUD/DATA_PIPELINE/SQL_MODELING/BROWSER_PERFORMANCE/FULLSTACK_STACK 대응. 3차원(D2/D3/D8) |

### 신규 프롬프트
| 파일 | 작업 |
|------|------|
| `backend/src/main/resources/prompts/template/turn-rubric-scorer.txt` | 신규. `DIMENSIONS_TO_SCORE` 파라미터화 + `DIMENSION_DEFINITIONS` 동적 주입. 관찰 인용 강제 |

### 신규 Java 클래스 (`backend/src/main/java/com/rehearse/api/domain/feedback/rubric/`)
| 파일 | 작업 |
|------|------|
| `RubricDimension.java` | 신규 record. `id(D1~D10)`, `name`, `description`, `scoring(Map<Integer, ScoringLevel>)` |
| `RubricFamily.java` | 신규 싱글톤. `_dimensions.yaml` 로드 결과 + `_mapping.yaml` 규칙 |
| `Rubric.java` | 신규 record. `rubricId`, `usesDimensions(List<DimensionRef>)`, `perTurnRules`, `levelExpectations` |
| `DimensionRef.java` | 신규. `ref(D2)`, `weight`, `conditional(optional)` |
| `DimensionScore.java` | 신규 record. `score(1-3 or null)`, `observation`, `evidenceQuote` |
| `RubricScore.java` | 신규. `rubricId`, `scoredDimensions(List)`, `dimensionScores(Map<String, DimensionScore>)`, `levelFlag` |
| `RubricLoader.java` | 신규 `@Component`. YAML 로드 + `resolveFor(Question, QuestionSet, Interview)` 매핑 |
| `RubricScorer.java` | 신규 `@Service`. plan-00b `AiClient.chat()` 호출. mode-aware 차원 선택 |

### 신규 JPA / DTO
| 파일 | 작업 |
|------|------|
| `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/entity/RubricScoreEntity.java` | V26 매핑. `rubric_id`, `scored_dimensions`, `scores_json`, `level_flag` |
| `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/repository/RubricScoreRepository.java` | JPA Repository. `findByInterviewIdOrderByTurnId()`, `findByInterviewIdAndRubricId()` |
| `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/dto/RubricScoreResponse.java` | DTO — Entity 직접 반환 금지 (CLAUDE.md 규약) |

### 수정
| 파일 | 작업 |
|------|------|
| `backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpService.java` | 턴 종료 hook에서 `RubricScorer.score()` 호출 + V26 저장 |
| `backend/src/main/java/com/rehearse/api/domain/resume/ResumeInterviewOrchestrator.java` (plan-07) | Resume 세션 턴 종료 시 `ResumeMode` + `currentLevel` 전달하며 `RubricScorer.score()` 호출 |

### 신규 테스트
| 파일 | 작업 |
|------|------|
| `backend/src/test/java/.../RubricLoaderTest.java` | `QuestionSetCategory` 12개 값 전부에 대해 매핑 실패 0건 검증 |
| `backend/src/test/java/.../RubricScorerTest.java` | `DIMENSIONS_TO_SCORE` 바깥 차원 null 반환 검증. mode-aware 규칙 검증 |

## 상세

### 10차원 사전 (`_dimensions.yaml`)

TODO 03 개정판 섹션 "10개 차원의 전체 사전" 그대로 구현. 요약:
| ID | 차원 | 용도 |
|---|---|---|
| D1 | Problem Framing | 전제/제약/엣지케이스 확인 |
| D2 | Technical Depth | 원리/트레이드오프/대안 |
| D3 | Reasoning Communication | 단계적 사고 언어화 |
| D4 | Conceptual Accuracy | 기술적 정확성 |
| D5 | Practical Application | 실무 맥락/흔한 실수 |
| D6 | Experience Concreteness | 수치/본인 기여/Learnings |
| D7 | Collaboration Awareness | 다관점 이해 |
| D8 | Recovery from Gaps | 모르는 것 대처 |
| D9 | Factual Consistency | Resume 전용: 이력서 claim과 답변 일치 |
| D10 | Chain Depth | Resume 전용: L1~L4 도달 깊이 |

각 차원 1~3점 척도, 각 점수에 `observable` 행동 리스트 포함. 점수 매김의 ground truth.

### 매핑 테이블 (`_mapping.yaml`)

```yaml
rules:
  - when: { resumeTrack: true }
    use: resume-v1

  - when: { category: CS_FUNDAMENTAL }
    use: concept-cs-fundamental-v1

  - when: { category: [LANGUAGE_FRAMEWORK, UI_FRAMEWORK] }
    use: concept-lang-framework-v1

  - when: { category: BEHAVIORAL }
    use: experience-collaboration-v1

  - when: { feedbackPerspective: EXPERIENCE }
    use: experience-technical-v1

  - when: { category: RESUME_BASED }
    use: resume-v1

default: fallback-generic-v1
```

규칙은 **위에서 아래로** 평가. 첫 match 우선. 향후 NETWORK/DB/OS 세분화, devops/data 전용 rubric 추가 시 이 파일에 규칙만 추가.

### 카테고리 ↔ 현재 enum 매핑 확정

| `QuestionSetCategory` | 적용 Rubric | 비고 |
|---|---|---|
| CS_FUNDAMENTAL | concept-cs-fundamental-v1 | NETWORK/DB/OS 구분 없이 통합 (MVP) |
| LANGUAGE_FRAMEWORK | concept-lang-framework-v1 | domain 무관 (질문 텍스트가 Spring/React/Django 구분) |
| UI_FRAMEWORK | concept-lang-framework-v1 | LANGUAGE_FRAMEWORK 와 통합 |
| BEHAVIORAL | experience-collaboration-v1 | 협업/갈등 질문 |
| RESUME_BASED | resume-v1 | Resume Track 또는 직접 카테고리, domain 무관 |
| SYSTEM_DESIGN | fallback-generic-v1 | 전용 rubric 추후 별건 |
| FULLSTACK_STACK | fallback-generic-v1 | |
| BROWSER_PERFORMANCE | fallback-generic-v1 | |
| INFRA_CICD | fallback-generic-v1 | devops 전용 rubric 추후 |
| CLOUD | fallback-generic-v1 | |
| DATA_PIPELINE | fallback-generic-v1 | data 전용 rubric 추후 |
| SQL_MODELING | fallback-generic-v1 | |

FeedbackPerspective=EXPERIENCE 는 Category 기반 규칙보다 우선순위가 **낮음** (Category 가 우선 매칭). Category 가 지정되지 않은 엣지 케이스에만 fallback 으로 experience-technical-v1 적용. resumeTrack=true 는 모든 Category 규칙보다 우선.

### 카테고리별 루브릭 예시 (concept-cs-fundamental-rubric.yaml)

```yaml
rubric_id: concept-cs-fundamental-v1
description: CS 기초 (네트워크/DB/OS) 질문 평가 루브릭
applies_to:
  - category: CS_FUNDAMENTAL

uses_dimensions:
  - ref: D2  # Technical Depth
    weight: 0.30
  - ref: D3  # Reasoning Communication
    weight: 0.25
  - ref: D4  # Conceptual Accuracy
    weight: 0.30
  - ref: D8  # Recovery from Gaps
    weight: 0.15

per_turn_rules:
  on_intent_clarify: []
  on_intent_give_up: [D8]
  on_intent_answer: [D2, D3, D4, D8]

level_expectations:
  junior: { must_reach_2: [D4], must_reach_1: all }
  mid:    { must_reach_2: all, must_reach_3: [D4] }
  senior: { must_reach_3: [D2, D4], must_reach_2: all }
```

### Resume Rubric의 mode-aware per_turn_rules

```yaml
# resume-rubric.yaml (domain 무관)
rubric_id: resume-v1
applies_to:
  - resumeTrack: true
  - category: RESUME_BASED

uses_dimensions:
  - ref: D2, weight: 0.25
  - ref: D3, weight: 0.15
  - ref: D6, weight: 0.20
  - ref: D9, weight: 0.20   # Factual Consistency
  - ref: D10, weight: 0.20  # Chain Depth

per_turn_rules:
  on_playground_mode: [D6]                      # 놀이터는 구체성만
  on_interrogation_mode: [D2, D3, D9, D10]      # 심문은 깊이 + 사실 + 체인
  on_wrap_up_mode: [D10]                        # 회고는 체인 완결 중심 (plan-07 WRAP_UP)
  on_intent_give_up: [D8]
  on_intent_clarify: []

level_expectations:
  mid:    { must_reach_3: [D9] }                # 사실 일치 필수
  senior: { must_reach_3: [D9, D2, D10] }
```

### RubricLoader 구현

```java
@Component
@RequiredArgsConstructor
public class RubricLoader {
    private final ResourceLoader resourceLoader;
    private final ObjectMapper yamlMapper;
    private RubricFamily family;          // _dimensions.yaml + _mapping.yaml
    private Map<String, Rubric> rubrics;  // rubric_id → Rubric

    @PostConstruct
    void init() { /* 8개 YAML 전부 로드 + 매핑 규칙 파싱 */ }

    public Rubric resolveFor(Question q, QuestionSet qs, Interview interview) {
        RubricResolutionContext ctx = new RubricResolutionContext(
            interview.hasResumeSkeleton(),
            qs.getCategory(),
            q.getFeedbackPerspective()
        );
        String rubricId = family.getMapping().resolve(ctx);  // _mapping.yaml 규칙 순서대로 평가
        return rubrics.getOrDefault(rubricId, rubrics.get("fallback-generic-v1"));
    }

    public RubricDimension getDimension(String ref) { return family.getDimensions().get(ref); }
}
```

### RubricScorer 호출 시그니처 (mode-aware)

```java
public RubricScore score(
    Question question,
    QuestionSet questionSet,
    Interview interview,
    String userAnswer,
    AnswerAnalysis analysis,
    IntentType intent,
    @Nullable ResumeMode resumeMode,
    @Nullable Integer currentChainLevel,
    UserLevel userLevel
) {
    Rubric rubric = rubricLoader.resolveFor(question, questionSet, interview);
    List<String> dimensionsToScore = rubric.selectDimensions(intent, resumeMode);
    if (dimensionsToScore.isEmpty()) {
        return RubricScore.empty(rubric.getRubricId());  // CLARIFY 등
    }

    ChatRequest req = promptBuilder.build(
        question, userAnswer, analysis, rubric, dimensionsToScore,
        userLevel, resumeMode, currentChainLevel
    );
    ChatResponse resp = aiClient.chat(req);
    return parser.parse(resp, rubric);
}
```

### D9 Factual Consistency 채점 데이터 주입 (plan-07 연계)

Resume Track 세션에서 D9 채점 시 LLM이 "이력서 원문"을 참조해야 일치도 판정 가능. `RubricScorerPromptBuilder`가 `ResumeSkeleton` 의 해당 project/claim 섹션을 `ChatRequest.messages` 에 추가 주입 (plan-04 ContextBuilder의 Layer 4 Focus 와 동일 패턴).

D9은 **채점만 하고 분기 flag는 내지 않음** → plan-07 `resume-chain-interrogator.txt` 의 `fact_check_flag` 필드 삭제 결정과 양립.

### D10 Chain Depth 채점

`currentChainLevel` 값을 RubricScorer에 전달:
- 1 = L1(What)까지 도달
- 2 = L2(How) 또는 L3(Why-mech)까지
- 3 = L4(Tradeoff)까지

LLM이 매번 판단하는 게 아니라 **ChainStateTracker 값을 직접 사용** — 일관성 확보.

### 턴 채점 프롬프트 (turn-rubric-scorer.txt)

TODO 03 개정판 섹션 "턴 채점 프롬프트" 그대로. 핵심:
1. `DIMENSIONS_TO_SCORE` 에 명시된 차원만 채점. 외 차원은 출력에 포함하지 말 것
2. 관찰 기반 채점 (evidence_quote 필수)
3. 레벨 보정 (주니어/미드/시니어)
4. `AnswerAnalysis` 재활용
5. 채점 불가 시 score=null, observation에 사유

### 호출 시점 — **비동기 post-turn (사용자 응답 latency 밖)**

Rubric Scorer 는 **사용자 턴 응답 latency 경로에 들어가지 않는다**. 턴 종료 후 배경에서 실행:

- **트리거**: `FollowUpService.generateFollowUp()` 가 사용자에게 다음 질문 응답을 보낸 **직후** `ApplicationEventPublisher.publishEvent(TurnCompletedEvent)` 발행
- **리스너**: `@Async @TransactionalEventListener(phase = AFTER_COMMIT)` 로 `RubricScorer.score()` 실행 → V26 `rubric_score` 저장
- **실패 정책**: 채점 실패는 **턴 진행 차단하지 않음**. 저장 실패 시 `application.log` WARN + `rehearse.ai.rubric.failures` 카운터 증가. 누락된 턴은 plan-09 Synthesizer 가 "score=null" 로 처리
- **재시도**: 이벤트 리스너 단일 시도, 실패는 포기 (세션 종합 피드백은 나머지 턴 점수로도 생성 가능). 운영 이슈 시 admin 수동 backfill 가능

이유: Rubric 채점은 "다음 턴 응답"이 아니라 "세션 종합 피드백"의 재료. 사용자가 기다리는 동안 수행할 필요 없음. 따라서 **Aggregate Latency SLA (plan-01/02/03)에 포함되지 않음**.

### 모델 파라미터
- Primary: GPT-4o-mini / Fallback: Claude Haiku (채점은 경량 모델 충분)
- temperature: 0.2
- max_tokens: 1536 (차원 수에 따라 가변)
- callType: `"rubric_scorer"`, Prompt Caching: _dimensions.yaml 블록 (고정 부분)

### 설정 (application.yml 상수)
```yaml
rehearse:
  feedback-rubric:
    family-version: v1                  # _dimensions.yaml 버전
    fallback-rubric: fallback-generic   # 매핑 실패 시
    mode-aware: true                    # Resume Track의 playground/interrogation 분기
```

Feature Flag runtime toggle은 사용하지 않는다. 기본 활성화 경로로 단일화. plan-13 cut-over 시 `application-prod.yml` 에서 직접 활성화.

## 담당 에이전트

- Implement: `backend` — RubricLoader/Scorer/Entity/Repository 구현
- Implement: `prompt-engineer` — `_dimensions.yaml` 10차원 scoring rubric 작성 (1~3점 observable 행동) + turn-rubric-scorer.txt 프롬프트
- Implement: `database-architect` — `_mapping.yaml` 선언적 규칙 + `RubricResolver` 알고리즘 검토
- Review: `architect-reviewer` — Rubric/DimensionRef/Family 도메인 경계 SOLID
- Review: `code-reviewer` — YAML 파싱 방어, JSON 스키마 검증, DTO 분리 (Entity 직접 반환 금지)

## 검증

1. **YAML 로드**: 8개 YAML(_dimensions + _mapping + 6개 rubric) `./gradlew test --tests "RubricLoaderTest"` 통과
2. **매핑 커버리지**: `QuestionSetCategory` 12개 값 전부 + Resume Track 조합에 대해 매핑 실패 0건 (fallback도 정상 귀결)
3. **차원 적용 정확성**: `DIMENSIONS_TO_SCORE` 바깥 차원은 결과에서 null 또는 제외 (정규식 검증 100%)
4. **Resume Track mode-aware**: playground 턴에서 D6만, interrogation 턴에서 D2/D3/D9/D10 채점 — 통합 테스트
5. **D9 Factual Consistency**: 이력서 원문 vs 답변 불일치 5개 케이스에서 D9 score ≤ 2 정확 감지
6. **D10 Chain Depth**: `currentChainLevel=4`일 때 D10 score=3 반환 검증
7. **관찰 인용**: 모든 채점 결과에 `evidence_quote` 필수 (null 방지 — CLARIFY 턴 제외)
8. **Entity 직접 반환 금지**: 컨트롤러/서비스 반환 타입에 `RubricScoreEntity` 0건 (`RubricScoreResponse` DTO 사용)
9. **V26 영속화**: `rubric_id`, `scored_dimensions`, `level_flag` 컬럼에 실제 값 기록 확인
10. **Synthesizer 입력**: plan-09 Synthesizer 에 전달되는 `SCORES_BY_CATEGORY` 구조가 카테고리별로 그룹핑됨
11. **수동 비교 (MANUAL_AB_PROTOCOL.md)**: 스테이징에서 3~5건 세션 투입 후 Rubric 채점 결과가 기존 3줄 포맷 대비 관찰 인용 포함률 ≥ 95% (G2 자동 검증)
12. `progress.md` 08 → Completed
