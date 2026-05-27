# Product Spec — 꼬리질문·답변채점 질문 카테고리 3-way 분기

> **작성자**: 사용자 (PM 초안 — Claude)
> **답하는 질문**: 왜? 무엇? 수용기준?
> **다음 단계**: 사용자 승인 → 구현 agent 가 `tech-spec.md` 작성

---

## 문제 상황 (Problem)

- **현재 상태 (정상 흐름)**: 면접 한 턴은 두 LLM 호출로 동작한다.
  - Step A — 답변 분석(`answer-analyzer.txt`): 사용자 답변을 평가 차원(dimension)별 부족도(0~3)로 채점하고 가장 부족한 차원 1개(`weakest_dimension`)를 선정한다. 현재 트랙 라벨은 2종(`TRACK: CS` / `TRACK: RESUME`)뿐이다.
  - Step B — 꼬리질문 생성(`follow-up-generator-v3.txt`): Step A 가 뽑은 `weakest_dimension` 을 보완하는 꼬리질문 1개를 만든다. 템플릿은 카테고리 구분 없이 단일이다.
- **발생 증상**:
  - CS 개념 설명형 질문(예: "JVM GC 동작 방식 설명")에도 "~을 선택하셨나요?" / "~경험에서" 식 경험 전제 꼬리질문이 생성된다. 개념 질문엔 부적합(개념 심화/명료화가 맞음).
  - 뿌리 원인: Step A 의 CS 트랙 차원 셋(8종)에 `experience_concreteness`(경험 구체성)·`collaboration_awareness`(협업 인지)가 포함되어 있다. 개념 질문 답변에도 이 경험 차원이 평가되고 `weakest_dimension` 으로 선정될 수 있으며, Step B 는 그 차원을 보완하려 경험 전제 질문을 낸다.
- **인지 채널**: 면접 진행 중 생성된 꼬리질문 검수 + 로그(`weakestDimension`, `dimensionGaps` 기록)에서 확인.

## 왜 해야 하는가 (Why)

- **사용자 임팩트**: 개념 질문에 동문서답성 경험 꼬리질문이 붙으면 면접 몰입이 깨지고 제품 신뢰도가 떨어진다. CS 개념 트랙의 핵심 가치(개념 깊이 검증)가 훼손된다.
- **시스템 임팩트**: 루브릭 YAML 은 이미 카테고리별로 차원이 분리(`concept-cs-*` / `experience-*` / `resume-rubric`)되어 있는데, 프롬프트 2계층(Step A/B)이 단일·2-way 라 카테고리-차원 매핑이 끊겨 있다. 채점 단계와 꼬리질문 단계의 차원 인식이 불일치한다.
- **외부 압력**: 인터뷰 품질 스프린트 연속 작업. 선행 PR #518(ContextLayer 제거)에서 follow-up 경로를 `PromptTemplateLoader` 직접 조립으로 정리해 둔 상태 — 본 작업의 사전 정리 완료.

## 해결 방향 (Approach)

- **핵심 접근**: 질문 카테고리를 `QuestionType`(7값) 메타데이터에서 결정적으로 도출(LLM 분류 0)해, Step A(답변 분석)와 Step B(꼬리질문 생성) 모두를 concept / experience / resume 3종으로 분기한다.
  - concept = 정답 있는 CS 개념 질문 (개념 정확성·심화 평가)
  - experience = 경험·협업 질문 (경험 구체성·협업 인지 평가)
  - resume = 이력서 트랙 질문 (사실 일치·체인 깊이 평가)
- **판정 소스 = `QuestionType`**: `TECH_*`→concept, `BEHAVIORAL_*`→experience, `RESUME_*`→resume. (이슈 본문의 "referenceType 기반"은 부정확 — referenceType 은 2값(MODEL_ANSWER/GUIDE)뿐이라 resume·behavioral 이 둘 다 GUIDE 로 묶여 분리 불가. `QuestionType` 7값이 접두사로 모든 케이스를 구별하며, 기존 코드(`FollowUpService` 의 resume 판정)도 이미 `QuestionType` 기준이다.)
- **Step A·B 동시 분기**: Step B 템플릿만 고치면 Step A 가 여전히 경험 차원을 weakest 로 뽑아 미흡하다. Step A 의 평가 차원 셋도 카테고리별로 분리해, 개념 질문에선 경험 전제 차원이 애초에 weakest 후보에서 빠지도록 한다(뿌리 차단). 카테고리별 차원 구성은 기존 루브릭 YAML 정의를 정합 기준으로 재사용한다.
- **대안 비교**: rubricCategory(3값) 기준 분기는 `RESUME_MAIN`(rubricCategory=TECHNICAL)이 `TECH_MAIN`(concept)과 충돌하고 resume 이 여러 카테고리로 흩어져 분리 불가 → 기각. QuestionType 채택.
- **단계**: 단일 작업(Step A+B 한 묶음). Step B 만 분리하면 뿌리 미해결로 목표 미달이라 phase 분리하지 않는다.

## Evidence

- 코드 추적:
  - `backend/.../interview/service/FollowUpService.java:109` — `isResumeTrack(QuestionType)` 이미 `QuestionType` 접두사 기준. `context.currentMainQuestionType()` 으로 7값 보유 → 3-way 직접 도출 가능.
  - `backend/.../question/entity/QuestionType.java` — 7값(`TECH_MAIN/FOLLOWUP`, `BEHAVIORAL_MAIN/FOLLOWUP`, `RESUME_OPENER/MAIN/FOLLOWUP`).
  - `backend/.../question/entity/ReferenceType.java` — 2값(`MODEL_ANSWER`, `GUIDE`). resume·behavioral 둘 다 GUIDE → 이슈 본문 전제 반증.
  - `backend/.../infra/ai/prompt/AnswerAnalysisPromptBuilder.java:37` — 현재 `trackLabel = isResumeTrack ? "RESUME" : "CS"` (2-way).
  - `backend/.../infra/ai/prompt/FollowUpQuestionPromptBuilder.java:23` — `build(mainQuestion, analysis)` 시그니처에 카테고리 입력 없음.
  - `backend/.../infra/ai/schema/GeneratedAnswerAnalysisSchema.java` — `CS_DIMENSION_KEYS`(8) / `RESUME_DIMENSION_KEYS`(10) 2-way. CS 8키에 `experience_concreteness`·`collaboration_awareness` 포함 → 개념 질문 경험 차원 평가 원인.
  - 프롬프트: `backend/src/main/resources/prompts/template/answer-analyzer.txt`(2-way 키셋 정의), `follow-up-generator-v3.txt`(단일).
  - 루브릭(이미 카테고리별 분리): `concept-cs-fundamental-rubric.yaml`(technical_depth, reasoning_communication, conceptual_accuracy, recovery_from_gaps), `experience-collaboration-rubric.yaml`(problem_framing, reasoning_communication, experience_concreteness, collaboration_awareness), `resume-rubric.yaml`(technical_depth, reasoning_communication, experience_concreteness, factual_consistency, chain_depth).
- 호출부 전파 범위(영향): `OpenAi/Claude AnswerAnalyzer`, `OpenAi/Claude FollowUpQuestionGenerator`, `ResilientAnswerAnalyzer`, `Mock*` 어댑터, `AnswerAnalysisService`/`AudioTurnAnalysisService`/`FollowUpService`.
- 인접 plan: `docs/plans/518-contextlayer-removal/` (선행 정리). 분기 역사: #295(2-모드 도입) → #423(IntentClassifier 제거, 분기 소실).

## Goal

- [ ] 질문 카테고리가 `QuestionType` 메타데이터에서 concept/experience/resume 3종으로 결정적으로 도출된다 (LLM 분류 호출 0, 동일 입력에 대해 매핑 오판 0).
- [ ] CS 개념 질문(`TECH_*`)의 답변 분석에서 경험 전제 차원(`experience_concreteness`, `collaboration_awareness`)이 `weakest_dimension` 으로 선정되지 않는다.
- [ ] CS 개념 질문 꼬리질문에 경험 전제 프레이밍이 사라지고 개념 심화/명료화 질문만 생성된다 (회귀 검증 fixture 기준 경험 전제 발생 0건).
- [ ] experience / resume 질문의 기존 평가 축·꼬리질문 품질이 회귀하지 않는다.

## Non-Goals

- 꼬리질문/채점 품질의 전반적 상향 — 목표는 카테고리 정합성(개념↔개념) 회복이지 품질 점수 자체 개선이 아니다.
- 루브릭 차원 정의(dimension) 자체의 재설계 — 기존 YAML 정의를 정합 기준으로 재사용만 한다.

## 수용 기준 (Acceptance Criteria)

- [ ] `TECH_MAIN`/`TECH_FOLLOWUP` → concept, `BEHAVIORAL_MAIN`/`BEHAVIORAL_FOLLOWUP` → experience, `RESUME_OPENER`/`RESUME_MAIN`/`RESUME_FOLLOWUP` → resume 로 매핑된다 (모든 7값 커버, 미분류 없음).
- [ ] concept 카테고리 답변 분석 결과의 차원 평가에 경험 전제 차원이 포함되지 않는다 (개념 질문 답변 입력 시 `weakest_dimension` 이 경험 전제 차원으로 나오지 않음을 검증).
- [ ] concept 카테고리 꼬리질문이 개념 심화/명료화 프레이밍으로 생성된다 (회귀 fixture 답변 세트에서 경험 전제 표현 미출현).
- [ ] experience / resume 카테고리 꼬리질문·차원 평가가 변경 전과 동일 의도로 동작한다 (resume 의 사실 일치·체인 깊이 차원 유지).
- [ ] `GeneratedFollowUp` 응답 schema(필드 구성)가 불변이라 기존 어댑터·프론트엔드가 변경 없이 호환된다.
- [ ] `follow-up-generator-v3.txt` 단일 의존이 제거된 뒤 토큰 사용량·출력 형식 회귀 검증을 통과한다.

## 비스코프 (Don't)

- LLM 기반 카테고리 분류기 도입 — 메타데이터(`QuestionType`)로 충분. 재도입 금지.
- DB 스키마 변경 — 카테고리 메타데이터(`QuestionType`)는 기존 존재.
- 루브릭 YAML 차원 정의 변경 — 이미 카테고리별 분리 완료, 본 작업은 프롬프트 2계층을 거기에 맞추는 것.
- nonverbal(비언어) 분석 경로 — 본 변경 범위 외.
- 채점(turn-rubric-scorer) 단계 — 이미 RubricLoader 가 카테고리별 차원을 적용 중. 본 작업은 Step A(answer-analyzer)·Step B(follow-up) 한정.

## 참고

- 관련 Issue: #539 (Epic)
- 선행 plan: `docs/plans/518-contextlayer-removal/`
- 분기 역사: #295(2-모드 도입), #423(IntentClassifier 제거)
- slug: `follow-up-category-split`
