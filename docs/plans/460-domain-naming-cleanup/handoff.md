# Handoff — 460-domain-naming-cleanup

> **수명**: 단명 (plan 종료 시 제거)
> **작성 시점**: 세션 종료 / 컨텍스트 잔여 ~30%
> **다음 세션**: plan 폴더 진입 시 **이 파일 먼저 읽음**

---

## 현재 상태

- 진행: spec 작성 완료 (product-spec.md + tech-spec.md). **코드 변경 0**. implement 진입 승인 대기.
- 브랜치: 460 전용 브랜치 **미생성**. 현재 `feat/435-resume-model-answer-quality` (이전 작업 잔존).
- 관련 PR: 없음 (코드 미작성)
- 빌드: 해당 없음
- 테스트: 해당 없음

## 다음 세션 시작점

**1순위 (Blocker)**: 사용자에게 **tech-spec → implement 진입 명시 승인** 요청.

- 다음 작업: 승인 확인 후 → `feat/460-domain-naming-cleanup-be` 브랜치 생성 → `implement-be.md` / `implement-fe.md` 작성
- 참조: `docs/plans/460-domain-naming-cleanup/tech-spec.md` — "구현 작업 분해 (preview)" 섹션 (line 402~425)
- 첫 명령: 사용자 승인 수신 → `git-manager` 에이전트로 `feat/460-domain-naming-cleanup-be` 브랜치 생성 → `backend` 에이전트로 `implement-be.md` 작성, `frontend` 에이전트로 `implement-fe.md` 작성 (병렬 가능)
- 임계 확인: BE 10 task → `docs/plans/AGENTS.md §6` 임계 = 8 초과 → `tasks/be-NN-*.md` 분리 권장

## 미해결 질문 / Blocker

- **implement 진입 승인 대기** — `plan-mode.md` 룰 = "tech-spec 승인 없이 implement 시작 금지". 다음 세션 진입 시 사용자 명시 승인 받은 후 진행.

## 컨텍스트 메모

- **FollowUpExchange cascade 15 파일**: Lombok getter `.answer()` → `.answerText()` rename 시 컴파일 오류 발생. IDE IntelliJ Safe Rename 권장. 누락 시 컴파일 실패.
- **FE wire 현재 broken 아님**: `frontend/src/hooks/use-answer-flow.ts:342-346` 송신 키 `answer` = BE `FollowUpRequest$FollowUpExchange.answer` 와 일치 상태. Phase 1 에서 BE+FE 동시 `answerText` 로 변경.
- **AskedPerspectives 변수명 41건 잔존**: `record AskedPerspectives(List<Perspective> values)` 의 제네릭 타입만 Phase 1 변경 (`List<AnswerFeedbackPerspective>`). 변수명 `askedPerspectives` 41건 = product-spec 카탈로그 #10 "보류" → Phase 2 후보. Phase 1 에서 절대 미터치.
- **DB 안전성 검증 완료**: `question_set.category VARCHAR(50)` 값 = InterviewType 12값과 100% 일치. `@Enumerated(STRING)` 교체 시 DDL/DML 0.
- **머지 윈도우**: BE PR 머지 직후 FE PR 즉시 연속 머지 수용 (사용자 결정). JsonAlias 호환 레이어 미사용.
- **CacheStrategy 노출**: `InterviewType` 의 `CacheStrategy` 부속이 `question` 도메인에 노출됨. 현재 호출처 0건 → 사용자 결정 = 수용 (YAGNI). 발생 시 별도 처리.
- **Phase 2 비스코프** (이번 작업 절대 미터치): ResumeMode (#11), MODEL_ANSWER 컬럼 정합성 (#2), EXPERIENCE/CONCEPT 프롬프트 단어 (#3, #12), AskedPerspectives 표현 (#10).

### 사용자 결정 누적 (tech-spec 본문 반영 완료, 재진입 시 변경 X)

| 항목 | 결정 |
|------|------|
| `interview.Perspective` | → `AnswerFeedbackPerspective` |
| `feedback.FeedbackPerspective` | → `RubricCategory` (파일 이동: `feedback/entity/` → `feedback/rubric/entity/`) |
| `FollowUpExchange.answer` | → `answerText` (필드 + 생성자 + getter cascade 15 파일 + JSON 키 동시) |
| `FollowUpRequest/Response.selectedPerspective` | → `selectedAnswerFeedbackPerspective` (필드 + JSON 키) |
| `AnswerResponse.feedbackPerspective` (top-level) / `TimestampFeedbackResponse$TechnicalFeedback.perspective` (inner, `technicalFeedback` 객체 내부) | → `rubricCategory` (JSON 키 포함) |
| `QuestionSetCategory` | 삭제 → `InterviewType` 통합 (CacheStrategy 노출 수용 = YAGNI) |
| `formatPerspectives` 정의 5곳 / `toReferenceLabel` 정의 3곳 | → `infra/ai/prompt/PromptFormatters` 단일 출처 |
| `RubricFamily.MappingRule.feedbackPerspective` + `_mapping.yaml` 키 | → `rubricCategory` |
| 운영 윈도우 | BE+FE 즉시 연속 머지 수용 (JsonAlias 미사용) |

## 참고 명령

```bash
# 브랜치 생성 (승인 후 git-manager 위임)
git checkout -b feat/460-domain-naming-cleanup-be

# BE 빌드 + 테스트
cd backend
docker compose -f docker-compose.local.yml up -d
./gradlew build
./gradlew test

# grep 검증 (구현 후 수행)
grep -rn "QuestionSetCategory" backend/src/main/java
grep -rn "import com.rehearse.api.domain.feedback.entity.FeedbackPerspective" backend/src/main/java
grep -rEn "private static String (formatPerspectives|toReferenceLabel)" backend/src/main/java
grep -rn "selectedPerspective" backend/src/main/java frontend/src

# FE 빌드 + 테스트
cd frontend
npm run lint && npm run build && npm run test
```

---

업데이트: 2026-05-09 (사용자 결정 변경: FeedbackCategory → RubricCategory 일괄 반영)
