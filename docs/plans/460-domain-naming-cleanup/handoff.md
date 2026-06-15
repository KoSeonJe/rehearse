# Handoff — 460-domain-naming-cleanup

> **수명**: 단명 (plan 종료 시 제거)
> **작성 시점**: Phase 1 완료 후 Phase 2 진입 대기
> **다음 세션**: plan 폴더 진입 시 **이 파일 먼저 읽음**

---

## 현재 상태

- **Phase 1**: 완료. BE PR #464, FE PR #465 모두 `develop` 에 머지.
- **이슈**: #460 closed.
- **develop HEAD**: `277ab55` (Phase 1 머지 반영 완료. 로컬 동기화 필요 시 `git pull origin develop`).
- 빌드 / 테스트: Phase 1 기준 통과 확인 완료.
- **Phase 2**: 미착수. 이 handoff 가 진입점.

---

## Phase 2 진입점

**1순위 (Blocking)**: 사용자에게 **Phase 2 우선순위 결정 요청**.

아래 6개 후보 항목(일부 보류 포함) 중 어떤 것부터 진행할지, 묶음 처리할지, 일부를 drop 할지 사용자 결정이 선행되어야 한다.

- 각 항목의 사유·컨텍스트는 "Phase 2 후보 작업" 섹션 참조.
- 결정 후 → 해당 항목 spec 작성 → 승인 → 구현 순으로 진행. 임의 착수 금지.

---

## Phase 2 후보 작업

아래 표는 `product-spec.md` 비스코프 섹션 (line 111~119) + 충돌 카탈로그 (line 54~64) 기준.

| # | 항목 | 비용 / 사유 | 추가 컨텍스트 |
|---|------|------------|--------------|
| 2 | `MODEL_ANSWER` enum 값 vs `Question.modelAnswer` 컬럼 정합성 | DB 마이그 검증 필요 | V46 마이그 (`drop_question_classification_meta` 등 `drop_*` 류) 단서 검증 필요. `ReferenceType.MODEL_ANSWER` 코드 값과 DB `question` 테이블 컬럼명 `model_answer` 매핑 확인 선행. |
| 3 | `EXPERIENCE` 단어 충돌 (`ReferenceType` 라벨 vs `FeedbackPerspective` 값) | 프롬프트 단어 변경 = LLM eval 회귀 비용 | RESUME 모범답변 품질 sprint (S1~S13 완료, S14 진입 예정) 결과에 영향 가능. 변경 전 LLM eval 회귀 비용 평가 필수. #12 와 묶음 검토 권장. |
| 10 | `AskedPerspectives` 3가지 타입 표현 — `record AskedPerspectives(List<Perspective> values)` + 변수명 `askedPerspectives` 41건 잔존 | 도메인 모델링 결정 선행 필요 | **보류 유지 권장.** Phase 1 에서 `record AskedPerspectives` 의 제네릭 타입만 `List<AnswerFeedbackPerspective>` 로 교체. 변수명 `askedPerspectives` 41건은 미터치 상태. 도메인 모델링 회의 / spec 선행 후 진입. |
| 11 | `ResumeMode` vs `*ModeHandler` vs `*Phase` 계층 명명 | 별도 plan 후보 | `docs/plans/458-resume-skeleton-redesign/` plan 이 활성화되면 해당 plan 과 묶음 처리 검토. 별도 진행 시 Resume 도메인 전체 구조 파악 선행 필요. |
| 12 | `ReferenceType` 코드 단어 vs 프롬프트 단어 불일치 | LLM eval 회귀 비용 | #3 과 동일 맥락. 프롬프트 입력 단어 변경 시 LLM 응답 품질 회귀 가능. RESUME 모범답변 sprint 영향 평가 후 #3 과 함께 진행 권장. |
| 13 | ArchUnit 룰 추가 — 충돌 재발 영구 차단 | Phase 1 회귀 감지 도입 후보 | Phase 1 에서 정리한 `Perspective → AnswerFeedbackPerspective`, `FeedbackPerspective → RubricCategory` 등 rename 결과가 회귀하지 않도록 ArchUnit 패키지/명명 룰로 영구 고정. 단독 진행 가능. 비용 낮음. |

---

## Phase 1 결과 요약 (참조용)

### 머지된 PR

| PR | 영역 | 내용 |
|----|------|------|
| #464 | BE | Phase 1 도메인 네이밍 정리 전체 (BE) |
| #465 | FE | Phase 1 와이어 동기화 (FE) |

### 적용된 rename 매핑

| 기존 식별자 | 변경 후 | 비고 |
|------------|--------|------|
| `interview.Perspective` | `AnswerFeedbackPerspective` | 파일 이동 없음 |
| `feedback.FeedbackPerspective` | `RubricCategory` | 파일 이동: `feedback/entity/` → `feedback/rubric/entity/` |
| `FollowUpExchange.answer` | `answerText` | 필드 + 생성자 + getter cascade 15 파일 + JSON 키 동시 변경 |
| `FollowUpRequest/Response.selectedPerspective` | `selectedAnswerFeedbackPerspective` | 필드 + JSON 키 |
| `AnswerResponse.feedbackPerspective` (top-level) | `rubricCategory` | JSON 키 포함 |
| `TimestampFeedbackResponse$TechnicalFeedback.perspective` (inner) | `rubricCategory` | `technicalFeedback` 객체 내부 |
| `QuestionSetCategory` | 삭제 → `InterviewType` 통합 | `CacheStrategy` 노출 수용 (YAGNI) |
| `formatPerspectives` 정의 5곳 / `toReferenceLabel` 정의 3곳 | `infra/ai/prompt/PromptFormatters` 단일 출처 | — |
| `RubricFamily.MappingRule.feedbackPerspective` + `_mapping.yaml` 키 | `rubricCategory` | — |

### 사용자 결정 누적 (Phase 1 확정, 변경 불가)

| 항목 | 결정 |
|------|------|
| `interview.Perspective` | → `AnswerFeedbackPerspective` |
| `feedback.FeedbackPerspective` | → `RubricCategory` (파일 이동 포함) |
| `FollowUpExchange.answer` | → `answerText` (cascade 15 파일 + JSON 키 동시) |
| `FollowUpRequest/Response.selectedPerspective` | → `selectedAnswerFeedbackPerspective` (필드 + JSON 키) |
| `AnswerResponse.feedbackPerspective` / `TechnicalFeedback.perspective` | → `rubricCategory` (JSON 키 포함) |
| `QuestionSetCategory` | 삭제 → `InterviewType` 통합 (`CacheStrategy` 노출 수용 = YAGNI) |
| `formatPerspectives` / `toReferenceLabel` | → `PromptFormatters` 단일 출처 |
| `RubricFamily.MappingRule.feedbackPerspective` + yaml 키 | → `rubricCategory` |
| 운영 윈도우 | BE+FE 즉시 연속 머지 수용 (JsonAlias 미사용) |

---

## 블로커 / 미해결

Phase 2 진입 전 사용자 결정이 필요한 항목.

- **#10 (`AskedPerspectives`)**: 도메인 모델링 회의 / spec 선행 전 코드 착수 금지. 변수명 `askedPerspectives` 41건 현 상태 유지.
- **#11 (`ResumeMode`)**: `docs/plans/458-resume-skeleton-redesign/` 활성화 여부 확인 후 묶음 여부 결정.
- **#3, #12 (프롬프트 단어)**: RESUME 모범답변 sprint (S14) 완료 후 진행 권장. 조기 진입 시 eval 회귀 비용 평가 필수.
- **#13 (ArchUnit)**: 단독 진행 가능. 사용자 우선순위 결정 후 착수.
- **#2 (MODEL_ANSWER)**: V46 마이그 단서 (`drop_*` 류) 검증 작업 선행 필요.

---

## 참고 명령

Phase 2 스코프 확정 후 사용할 검증 명령.

```bash
# develop 동기화
git pull origin develop

# Phase 1 rename 결과 회귀 확인 (Phase 2 착수 전 베이스라인)
grep -rn "QuestionSetCategory" backend/src/main/java
grep -rn "import com.rehearse.api.domain.feedback.entity.FeedbackPerspective" backend/src/main/java
grep -rEn "private static String (formatPerspectives|toReferenceLabel)" backend/src/main/java
grep -rn "selectedPerspective" backend/src/main/java frontend/src

# AskedPerspectives 변수명 잔존 현황 확인 (#10 관련)
grep -rn "askedPerspectives" backend/src/main/java

# V46 마이그 단서 확인 (#2 관련)
grep -rn "drop_" backend/src/main/resources/db/migration

# BE 빌드 + 테스트
cd backend
docker compose -f docker-compose.local.yml up -d
./gradlew build
./gradlew test

# FE 빌드 + 테스트
cd frontend
npm run lint && npm run build && npm run test
```

---

업데이트: 2026-05-09 — Phase 1 완료 후 Phase 2 진입 대기
