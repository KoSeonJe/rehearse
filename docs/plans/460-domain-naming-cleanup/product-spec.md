# Product Spec — 도메인 enum/필드/변환 네이밍 충돌 정리 (Phase 1)

> **작성자**: 사용자 (PM 분석 초안: Senior PM persona via `/create-product-spec`)
> **답하는 질문**: 왜? 무엇? 수용기준?
> **다음 단계**: 사용자 승인 → 구현 agent 가 `tech-spec.md` 작성

---

## 문제 상황 (Problem)

**같은 의미를 나타내는 코드 식별자 (클래스명·메서드명·변수명·enum·필드)가 여러 다른 이름으로 존재.** 일관된 표현 부재 → 새 작업 진입 시 어떤 이름이 정식인지, 어떤 enum 을 임포트해야 할지 매번 파악 비용 발생.

- 현재 상태:
  - `interview.Perspective` (7개) 와 `feedback.FeedbackPerspective` (3개) — 양쪽 존재. 같은 단어 다른 추상화.
  - `ReferenceType.MODEL_ANSWER` enum 값 + `Question.modelAnswer` 컬럼 — 같은 단어 다른 의미.
  - `EXPERIENCE` 단어 — ReferenceType 변환 라벨 + FeedbackPerspective 값 양쪽.
  - `formatPerspectives()` 4곳 / `toReferenceLabel()` 3곳 — 변환 코드 분산.
  - `InterviewType` (12) 와 `QuestionSetCategory` (12) — 양쪽 활성. 일부는 V21 마이그레이션으로 통합 흔적이지만 코드는 양쪽 임포트 사용 중.
  - DTO 답변 필드 — `answer` / `answerText` / `userAnswer` 혼용. 같은 `FollowUpRequest` 안에서도 혼용.
  - "asked perspective" 표현 — `AskedPerspectives` (record) / `List<String>` (FocusHints) / `String` (DTO) 3가지 방식.

- 발생 증상:
  - 신규 PromptBuilder / Adapter 작업 시 어느 enum 임포트할지 매번 확인 필요.
  - 변환 함수 수정 시 4곳 동기화 부담.
  - 코드 리뷰 / 페어링 시 단어 의미 묻는 시간 반복.

- 인지 채널:
  - 본인 PromptBuilder 작업 (RESUME 모범답변 품질 sprint) 도중 발견.
  - audit 결과 12개 충돌 카탈로그 (Issue #460 본문).

## 왜 해야 하는가 (Why)

- 사용자 임팩트: **없음** (외부 동작 변경 0).
- 개발 / 시스템 임팩트:
  - 신규 컨트리뷰터 / 본인 = enum 의미 추론 비용 매번 발생.
  - 변환 함수 4곳 동기화 누락 시 LLM 프롬프트 단어 불일치 → 모범답변 품질 회귀 위험 (이전 sprint S1~S13 결과 약화 가능).
  - 도메인 사전 부재 → 후속 LLM 품질 sprint / Resume skeleton redesign (#458) 진입 시 토양 약함.
- 외부 압력: 없음. **개발 부채 자발 청산**.

## 해결 방향 (Approach)

PM 수준 high-level. **2-phase 분할** — Phase 1 = 마이그/eval 부담 0 항목, Phase 2 = 별도 spec.

- 핵심 접근: **"같은 의미면 같은 이름"** 원칙으로 클래스명·메서드명·변수명·enum·DTO 필드를 일관된 표현으로 통일. 동일 의미 다른 이름 0 / 단일 출처 1 곳. 외부 동작 보존.
- 우선순위 축: 혼동 심각도 + 마이그 비용 두 축 동시 고려 (tech-spec 단계 Task 순서 결정 시 참조).
- Phase 1 (이 spec): 코드만 영향. DB 마이그 0, LLM 프롬프트 단어 변경 0.
- Phase 2 (별도 spec, 본 spec 비스코프): 마이그 / LLM eval 필요. 비스코프 섹션에 항목·사유 상세.

### Issue #460 카탈로그 12개 → Phase 매핑

| # | 카탈로그 | 처리 |
|---|----------|------|
| 1 | Perspective vs FeedbackPerspective 단어 충돌 | **Phase 1** |
| 2 | MODEL_ANSWER enum 값 vs modelAnswer 컬럼 | Phase 2 (마이그 검증 필요) |
| 3 | EXPERIENCE 단어 충돌 (라벨 vs 값) | Phase 2 (프롬프트 단어 변경 = LLM eval) |
| 4 | InterviewLevel vs CandidateLevel | **보류** (별도 epic 후보 — 도메인 경계 결정 선행 필요) |
| 5 | InterviewType vs QuestionSetCategory | **Phase 1** |
| 6 | Question 분류 enum 3계층 | **보류** (#5 처리 후 재평가) |
| 7 | DTO answer/answerText/userAnswer 혼용 | **Phase 1** |
| 8 | toReferenceLabel() 3곳 중복 | **Phase 1** (단어 변경 없이 단일 출처화만) |
| 9 | formatPerspectives() 4곳 중복 | **Phase 1** |
| 10 | AskedPerspectives 3가지 타입 표현 | **보류** (도메인 모델링 결정 선행 필요) |
| 11 | ResumeMode vs *ModeHandler vs *Phase 계층 | **별도 plan** (Resume skeleton redesign #458 와 묶일 가능성) |
| 12 | ReferenceType 코드 단어 vs 프롬프트 단어 불일치 | Phase 2 (프롬프트 단어 변경 = LLM eval) |

## Evidence

- Issue #460 본문 — 12개 충돌 카탈로그 (코드 audit = 검증 완료 / DB 컬럼 정합성 = 미검증, Phase 2 영역)
- 코드 추적:
  - `backend/src/main/java/com/rehearse/api/domain/interview/entity/Perspective.java` — 7개 enum
  - `backend/src/main/java/com/rehearse/api/domain/feedback/entity/FeedbackPerspective.java` — 3개 enum
  - `backend/src/main/java/com/rehearse/api/domain/question/entity/ReferenceType.java` — `MODEL_ANSWER, GUIDE`
  - `backend/src/main/java/com/rehearse/api/domain/question/entity/Question.java:34` — `modelAnswer` TEXT 컬럼
  - `backend/src/main/java/com/rehearse/api/domain/question/entity/QuestionSetCategory.java` — InterviewType 과 동일 12값
  - `backend/src/main/java/com/rehearse/api/infra/ai/prompt/AnswerAnalyzerPromptBuilder.java:60,70` — toReferenceLabel + formatPerspectives
  - `backend/src/main/java/com/rehearse/api/infra/ai/prompt/AudioTurnAnalyzerPromptBuilder.java:56,66` — 동일 변환 중복
  - `backend/src/main/java/com/rehearse/api/infra/ai/prompt/FollowUpPromptBuilder.java:70,126` — 동일 변환 중복
  - `backend/src/main/java/com/rehearse/api/domain/interview/dto/FollowUpRequest.java:21,34` — `answerText` / `answer` 혼용
- 마이그 단서: V21 (`convert_question_set_category_to_interview_type`), V46 (`drop_question_classification_meta`) — 부분 통합 흔적 + 추가 검증 여지 (Phase 2 영역)
- 인접 plan:
  - `427-standard-track-classification-enum` — 분류 enum 통합 흔적
  - `436-feedback-perspective-label-routing` — FeedbackPerspective 라우팅 작업 흔적
  - `458-resume-skeleton-redesign` — 본 정리 후 진입 시 토양 ↑

## Goal

측정 가능 결과. 동작 변경 0 작업 → 정량 지표 = 코드 수준 객관 grep / 테스트 결과. **"같은 의미면 같은 이름" 원칙의 boolean 검증.**

- [ ] 동등 로직 단일 출처 — 동일 의미의 변환 함수가 도메인 전체에서 1곳에만 존재 (grep 결과 동일 시그니처 0 또는 1)
- [ ] 동명 enum 충돌 0 — 한 파일에서 서로 다른 도메인의 동명 enum 을 동시 임포트하는 케이스 grep 결과 0건
- [ ] DTO 답변 필드 단일 명명 — 답변 텍스트를 표현하는 필드명이 도메인 전체에서 한 가지로 수렴 (동일 의미 다른 이름 grep 결과 0건)
- [ ] 분류 enum 단일 출처 — 동일 분류 의미를 표현하는 enum 이 둘 이상 동시 활성 0건. 분리 의미면 책임이 명명·문서로 명확
- [ ] 동작 변경 0 — 기존 BE 단위/통합/E2E 테스트 100% 통과. 본 phase 1 은 코드 식별자 변경만 — LLM 프롬프트 입력 단어는 변경 없음 (Phase 2 영역)

## Non-Goals

- 외부 사용자 체감 변화 추구 X — 동작 변경 0 작업.
- 성능 / latency 개선 X — 도메인 명명 / 추상화 정리만.
- 기능 추가 / 동작 변경 X — 의미 동등성 보존이 최우선.

## 수용 기준 (Acceptance Criteria)

이 phase 1 이 "완료" 라고 부를 수 있는 조건. **모두 grep / 테스트 결과 boolean 검증 가능.**

- [ ] 변환 함수 단일 출처 — 동일 의미 변환 로직이 도메인 전반에서 한 곳에만 존재. grep 으로 동일 시그니처 카운트 = 1.
- [ ] interview 도메인의 분석 관점 enum 이름이 feedback 도메인 enum 과 단어 중복 없는 이름으로 존재 (서로 다른 도메인 동명 enum 동시 임포트 grep = 0).
- [ ] DTO 답변 텍스트 필드가 도메인 전체에서 한 가지 이름으로 통일 (동일 개념 다른 이름 grep = 0).
- [ ] `QuestionSetCategory` 와 `InterviewType` — 동일 분류 의미면 단일 enum, 분리 의미면 책임이 명명·문서로 명확. 양쪽 동시 활성 + 동일 12값 상태 해소.
- [ ] 회귀 테스트 — BE 단위/통합/E2E 통과. 본 phase 1 은 코드 식별자만 변경 — 프롬프트 입력 단어 변경 없음 → LLM Live eval 별도 호출 불필요. 단, 회귀 발견 시 = **해당 항목 Phase 2 로 이관, 나머지만 머지**.

## 비스코프 (Don't)

이번 phase 1 의도적 절단. **Phase 2 별도 spec 후보**.

- ResumeMode enum 값 리네이밍 — `@Enumerated(STRING)` DB 저장. 마이그 + 데이터 정합성 검증 비용. 별도 spec.
- ReferenceType DB 컬럼 정합성 정리 — V46 마이그 단서 검증 필요. 코드-DB 일관성 별도 작업.
- 프롬프트 템플릿 단어 변경 (`CONCEPT` / `EXPERIENCE` 라벨 자체) — LLM 의미 동등성 회귀 비용. RESUME 모범답변 품질 sprint 영향 가능 → 별도 검증 spec.
- ArchUnit 룰 추가 — 충돌 재발 방지 영구 차단. 본 phase 후 회귀 감지 단계로 별도 도입.
- Resume 도메인 Mode / Phase / Handler 계층 명명 정리 — 별도 작업 (Resume skeleton redesign #458 와 묶을 가능성).

## 참고

- 관련 Issue: #460 (Epic)
- 관련 plan:
  - `docs/plans/427-standard-track-classification-enum/`
  - `docs/plans/436-feedback-perspective-label-routing/`
  - `docs/plans/458-resume-skeleton-redesign/` (후속 영향 가능)
- 외부 자료: 없음
