# Plan 00a: Codebase Inventory (Phase 0 선행) `[blocking]`

> 상태: Completed (S1, 2026-04-20)
> 작성일: 2026-04-20
> 주차: W1 초반 (1-2일)
> 해결 RC: RC3(코드 지도 부재), RC4(기존 테스트 커버리지 파악)
> 산출물: INVENTORY.md (380L) / TEST_BASELINE.md (249L) / IMPACT_MAP.md (364L)

## Why

최초 플랜이 `InterviewTurnService`·`InterviewSession` 같은 **실재하지 않는 클래스명**을 다수 포함했다(critic M4). 각 후속 plan이 실제 코드와 어긋난 채로 실행되면 실행자가 "이 클래스 어디 있지?" 탐색에 시간을 잃고 잘못된 위치에 코드를 삽입할 위험.

현재 실제 구조를 **한 번에 매핑한 인벤토리 문서**를 먼저 만들고 이 문서가 plan-01~10의 "수정 대상 파일" 섹션 Ground Truth가 되도록 한다. 테스트 커버리지도 동시 파악해 회귀 보호선을 설정.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `docs/plans/interview-quality-2026-04-20/INVENTORY.md` | 신규. 아래 5섹션 구성 |
| `docs/plans/interview-quality-2026-04-20/TEST_BASELINE.md` | 신규. 기존 테스트 리스트 + 커버리지 수치 |
| `docs/plans/interview-quality-2026-04-20/IMPACT_MAP.md` | 신규. plan-01~10 별 실제 touch 파일 확정 |

## 상세

### INVENTORY.md 섹션 구성

#### 1. AI Infrastructure (`backend/src/main/java/com/rehearse/api/infra/ai/`)
확인된 실재 클래스 (실측):
- `AiClient.java` (interface) — 현재 3개 메서드: `generateQuestions`, `generateFollowUpQuestion`, `generateFollowUpWithAudio`
- `ResilientAiClient.java` — Primary/Fallback 이중화
- `OpenAiClient.java`, `ClaudeApiClient.java`, `MockAiClient.java`
- `AiResponseParser.java`
- `PdfTextExtractor.java` ← **plan-05가 이 클래스 확장. 신규 생성 아님**
- `WhisperService.java`, `SttService.java`, `MockSttService.java`
- subpackages: `dto/`, `exception/`, `persona/`, `prompt/`

#### 2. Interview Domain (`backend/src/main/java/com/rehearse/api/domain/interview/`)
실재 서비스:
- `InterviewService.java` — aggregate 조작
- `InterviewCreationService.java`, `InterviewCompletionService.java`, `InterviewDeletionService.java`
- `InterviewFinder.java`, `InterviewQueryService.java`
- `FollowUpService.java` ← **plan-01의 Intent 분기가 여기 `generateFollowUp()` 앞에 삽입됨**
- `FollowUpTransactionHandler.java`

실재 엔티티: `Interview`, `InterviewLevel`, `InterviewStatus`, `InterviewType`, `Position`, `QuestionGenerationStatus`, `TechStack`
**`InterviewSession` 클래스 존재하지 않음** — aggregate root는 `Interview` 엔티티. 런타임 상태(covered_claims, chain_state, analysis_cache)는 별도 `InterviewRuntimeState`(신규, plan-00c)로 분리.

Subpackages: `controller/`, `dto/`, `entity/`, `event/`, `exception/`, `generation/`, `repository/`, `service/`, `vo/`

#### 3. Feedback Domain (`backend/src/main/java/com/rehearse/api/domain/feedback/`)
현재: `dto/`, `entity/`, `exception/`, `repository/`, `service/`
기존 Entity: `TimestampFeedback`, `QuestionSetFeedback` 등 (Lambda Gemini 결과 저장).
**plan-09가 신규 `SessionFeedback` 도입 시 기존과의 관계는 plan-00e에서 결정**.

**plan-08 (Rubric Family) 신규 패키지**:
- `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/` — `RubricDimension/Rubric/DimensionRef/DimensionScore/RubricScore/RubricLoader/RubricScorer`
- `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/entity/RubricScoreEntity.java` (V26 매핑)
- `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/repository/RubricScoreRepository.java`
- `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/dto/RubricScoreResponse.java`

#### 3.5. Rubric Resources (신규 디렉토리 — plan-08 Rubric Family)
`backend/src/main/resources/rubric/` (신설):
- `_dimensions.yaml` — 10차원 마스터 (D1~D10)
- `_mapping.yaml` — QuestionSetCategory/FeedbackPerspective/Resume 조합 → rubric_id 선언적 매핑
- `concept-cs-fundamental-rubric.yaml`, `lang-fw-backend-rubric.yaml`, `lang-fw-frontend-rubric.yaml`, `experience-backend-rubric.yaml`, `experience-collaboration-rubric.yaml`, `resume-backend-rubric.yaml`, `fallback-generic-rubric.yaml` (총 7개 루브릭 + 2개 마스터)

#### 3.6. Question/QuestionSet enum 확인 (plan-08 매핑 입력)
- `backend/src/main/java/com/rehearse/api/domain/questionset/entity/QuestionSetCategory.java` — 12개 값 (CS_FUNDAMENTAL / BEHAVIORAL / RESUME_BASED / LANGUAGE_FRAMEWORK / SYSTEM_DESIGN / FULLSTACK_STACK / UI_FRAMEWORK / BROWSER_PERFORMANCE / INFRA_CICD / CLOUD / DATA_PIPELINE / SQL_MODELING)
- `backend/src/main/java/com/rehearse/api/domain/feedback/entity/FeedbackPerspective.java` — TECHNICAL/BEHAVIORAL/EXPERIENCE
- `backend/src/main/java/com/rehearse/api/domain/question/entity/ReferenceType.java` — MODEL_ANSWER/GUIDE (TODO 03의 CONCEPT/EXPERIENCE와 **다름** — 매핑 시 주의)

#### 4. Question Domain
`backend/src/main/java/com/rehearse/api/domain/question/`: `dto/`, `entity/`, `exception/`, `repository/`. plan-05/06은 신규 `resume` 도메인을 따로 만들고 기존 question 도메인은 건드리지 않음.

#### 5. Resume Domain (미존재)
**`backend/src/main/java/com/rehearse/api/domain/resume/`** — 본 스프린트에서 신규 생성. plan-05/06/07이 여기 배치.

### TEST_BASELINE.md

실측해야 할 항목:
- `./gradlew test` 현재 통과/실패/skip 수
- 인터뷰 도메인 테스트 클래스 리스트 (예: `InterviewServiceTest`, `FollowUpServiceTest` 등 존재 여부)
- AI 인프라 테스트 (`ResilientAiClientTest` 등) 유무
- 통합 테스트 vs 단위 테스트 비율
- JaCoCo 커버리지 리포트가 이미 설정되어 있다면 `backend/build/reports/jacoco/` 경로 확인

**최소 보호선**: Phase 0 진입 전 커버리지 수치를 baseline으로 기록. Phase 1~4 각 plan 머지 시 이 수치 회귀 없음(`>= baseline - 1%p`) 검증.

### IMPACT_MAP.md

각 plan이 실제로 touch하는 파일을 재확인해 기록 (plan별 "생성/수정 파일" 섹션 업데이트용 원본):

예시 포맷:
```
## plan-01 Intent Classifier
### 신규 생성
- backend/src/main/resources/prompts/template/intent-classifier.txt
- backend/src/main/resources/prompts/template/clarify-response.txt
- backend/src/main/resources/prompts/template/giveup-response.txt
- backend/src/main/java/com/rehearse/api/infra/ai/prompt/IntentClassifierPromptBuilder.java
- backend/src/main/java/com/rehearse/api/infra/ai/prompt/ClarifyResponsePromptBuilder.java
- backend/src/main/java/com/rehearse/api/infra/ai/prompt/GiveUpResponsePromptBuilder.java
- backend/src/main/java/com/rehearse/api/domain/interview/service/IntentClassifier.java
- backend/src/main/java/com/rehearse/api/domain/interview/vo/IntentType.java

### 수정
- backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpService.java
  → generateFollowUp() 진입부에 intentClassifier.classify() 분기 삽입
- backend/src/main/resources/application.yml → rehearse.features.intent-classifier.*
```

## 담당 에이전트

- Implement: `oh-my-claudecode:explore` + `backend` — 실제 파일/클래스 조사 후 문서 작성
- Review: `architect-reviewer` — IMPACT_MAP이 각 plan의 명시된 목표와 일치하는지 검증

## 검증

1. `INVENTORY.md`의 모든 클래스명이 `find backend/src/main/java -name "*.java"` 결과와 일치(100%)
2. `TEST_BASELINE.md`의 테스트 수치가 `./gradlew test` 실제 출력과 일치
3. `IMPACT_MAP.md`에 plan-01~10의 **모든 수정 대상 파일**이 실제 경로로 존재(또는 "신규 생성" 명시)
4. 후속 plan 착수 시 이 3개 문서가 "single source of truth" 역할 — plan의 "생성/수정 파일" 표가 이와 불일치하면 plan이 틀린 것
5. `progress.md` 00a → Completed
