# Implement (Backend) — rubric 출력 품질 회복 (단일 패턴 통일)

> **작성자**: backend agent (Staff Engineer 페르소나)
> **답하는 질문**: BE + Lambda 어떤 순서로 실행?
> **승인 게이트**: ★ `tech-spec.md` 사용자 명시 승인 후 시작 ★
> **대응 spec**: `product-spec.md` + `tech-spec.md`

---

## Phase 0: API Contract 확인

`tech-spec.md#api-contract` (Phase 2a Lambda → BE callback + BE → FE 응답) 확정 여부 확인.

- [x] Endpoint 경로 / 메서드 합의 — `POST /api/internal/interviews/{interviewId}/question-sets/{questionSetId}/feedback` 유지 + `GET /api/feedbacks/{interviewId}` 응답 확장
- [x] Request schema 합의 — `nonverbalScore = {vocal?, vision?}` 영역 키 분리 (TO-1 채택안)
- [x] Response schema 합의 — `nonverbalFeedback {rubricId, dimensions[]}` 신설 (verbal `technicalFeedback` 동일 구조)
- [x] Error 코드 매핑 합의 — Lambda 단 retry omit / BE 단 `[정상 skip] payloadNull` · `[결함 skip] areasEmpty|allInvalid` 로그 + 메트릭

미합의 → 즉시 STOP. tech-spec 갱신 후 재요청.

---

## Phase / Task 개요

| Task | Phase | 제목 | 구현 | 의존 | 링크 |
|------|-------|------|------|------|------|
| BE-01 | 1 | verbal scorer prompt 한국어 + verbatim 룰 | `prompt-engineer` | Phase 0 | [be-01](tasks/be-01-verbal-prompt-template.md) |
| BE-02 | 1 | `RubricScorerResponseValidator` + adapter retry + Micrometer | `backend` | BE-01 | [be-02](tasks/be-02-verbal-validator-retry.md) |
| BE-03 | 2a | Lambda analyzer prompt 전환 (dimension 채점 + raw 자연어 산출 지시 제거) | `prompt-engineer` | Phase 1 머지 + dev 검증 게이트 | [be-03](tasks/be-03-lambda-analyzer-prompt.md) |
| BE-04 | 2a | Lambda `dimension_validator.py` + handler 영역 키 페이로드 + 매퍼 삭제 | `backend` | BE-03 | [be-04](tasks/be-04-lambda-validator-handler.md) |
| BE-05 | 2a | `SaveFeedbackRequest` 재정의 + persister 재작성 + orphan 삭제 + ArchUnit | `backend` | BE-04 (Lambda 페이로드 확정) | [be-05](tasks/be-05-be-persister-orphan.md) |
| BE-06 | 2a | rubric YAML (composure 삭제 + 3차원 measurement/observable 확장) | `prompt-engineer` | BE-05 (orphan 검증 시점 정렬) | [be-06](tasks/be-06-rubric-yaml.md) |
| BE-07 | 2a | `TimestampFeedbackResponse.nonverbalFeedback` 신설 + `FeedbackService` group by | `backend` | BE-05 | [be-07](tasks/be-07-response-dto.md) |
| BE-08 | 3 | Flyway DROP 10컬럼 + entity / DTO / Response inner class cleanup | `backend` | Phase 2a/2b 머지 + dev 검증 게이트 | [be-08](tasks/be-08-flyway-cleanup.md) |

---

## PR 단위

| PR | 포함 Task | 머지 후 게이트 |
|----|-----------|----------------|
| PR#1 (Phase 1) | BE-01 + BE-02 | dev 인터뷰 1회 직접 검증 + 사용자 명시 승인 |
| PR#2 (Phase 2a) | BE-03 ~ BE-07 (BE + Lambda 묶음) | dev 인터뷰 1회 + 운영자 리뷰 게이트 (P1-E 9건 체크리스트) + 사용자 승인 |
| PR#3 (Phase 3) | BE-08 | dev 검증 (10컬럼 NULL + grep 0) + 사용자 승인 |

> Phase 2a = Lambda + BE 묶음 1 PR. `@JsonIgnoreProperties(ignoreUnknown=true)` 호환으로 BE 선행 / Lambda 선행 / 묶음 모두 가능 — 권장 = 묶음.

---

## FE 와 통합 시점

- **BE 선행 강제 = O** (tech-spec §분기결정).
- PR#2 (Phase 2a) 머지 + dev 검증 게이트 통과 → FE `implement-fe.md` Phase 2b 진입.
- FE 는 mock 진행 가능 (`@JsonIgnoreProperties` 호환), 단 통합 검증은 BE 머지 후.

---

## 통합 Verification

`tech-spec.md#verification` Phase 1 / 2a / 3 표 1:1 참조. 추가 회귀 신설 X.

핵심 통과 기준:
- AC-1/2 (verbal 한국어 + verbatim): `RubricScorerServiceTest` / `RubricScorerResponseValidatorTest` / `RubricScoringAdapterTest` green
- AC-3/4/5 (비언어 단일 패턴 + 3차원 + 적재 NOT NULL): Lambda pytest + `NonverbalScorePersisterTest` + `RubricCatalogTest` green
- AC-7 (저장소 정리): Flyway smoke + grep 0
- 빌드: `./gradlew build` + `cd lambda/analysis && pytest` green

---

## 리뷰 게이트 (MANDATORY)

구현 완료 직후 지정 리뷰어 실행 강제. 스킵 = 위반.

- [ ] `code-reviewer-backend` 실행 (구현 완료 직후 — 메인 세션 책임)
  - PR#1 / PR#2 / PR#3 각 PR 머지 직전 실행
  - Phase 2a Lambda 코드 (Python) 도 동일 리뷰어 (별도 Lambda 전담 리뷰어 부재)
- [ ] 컨벤션 위반 0건 (`backend/.claude/rules/conventions.md` + `testing.md`)
- [ ] Critical / Major 지적 = fix 반영 후 재리뷰
- [ ] Pre/Post State diff 일치 (`tech-spec.md#pre--post-state`)
- [ ] Phase 2a 운영자 리뷰 게이트 (P1-E) — `_dimensions.yaml` raw 9종 흡수 표현 9건 체크리스트 사용자 명시 승인
