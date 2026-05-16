# Task BE-02 — verbal `RubricScorerResponseValidator` + adapter retry 1회 + Micrometer counter

> **Phase**: 1 (BE only)
> **답하는 질문**: prompt 통과 후에도 LLM 위배 응답이 들어올 때 어떻게 dimension 단위로 회복?

---

## 목적

dimension 단위 응답 검증 (score 1-3 / observation 한국어 음절 1+ / evidence_quote ∈ userAnswer) + 위배 시 retry 1회 (보강 prompt) + 재실패 시 dimension omit + 메트릭 counter. AC-5 (적재 누락률 0% — 검증 실패 시 자동 회복 경로) 보장. validator 클래스는 Phase 2a Lambda 측 (`dimension_validator.py`) 과 정책 단일 출처 짝.

## 에이전트

- **구현**: `backend` — adapter 계층 retry 정책 / Micrometer 메트릭 / Bean Validation 위치 결정.
- **리뷰**: PR#1 머지 직전 `code-reviewer-backend`.

## 변경 파일

- `backend/src/main/java/com/rehearse/api/infra/ai/adapter/RubricScorerResponseValidator.java` — **신규**. 정책 단일 출처. 위배 사유 enum (`InvalidScore` / `MissingObservation` / `NonKoreanObservation` / `MissingEvidence` / `EvidenceNotInUserAnswer`) 반환.
- `backend/src/main/java/com/rehearse/api/infra/ai/adapter/RubricScoringAdapter.java` — dimension 단위 검증 호출 + retry 1회 분기 + `[결함 skip] retry_failed` 로그 + Micrometer counter 호출.
- `backend/src/main/resources/application*.yml` (필요 시) — 메트릭 backend 설정 기존 따름. 신규 키 추가 0.
- `backend/src/test/java/.../RubricScorerResponseValidatorTest.java` — **신규** (Domain Unit 또는 Infra Integration — `DomainUnitSupport` 충분).
- `backend/src/test/java/.../RubricScoringAdapterTest.java` — **신규** (`InfraIntegrationSupport`). retry 시나리오.
- `backend/src/test/java/.../RubricScorerServiceTest.java` — Service Integration 시나리오 추가 (한국어 observation / verbatim evidence / fault isolation).

## 핵심 로직 / 변경 요약

```
RubricScorerResponseValidator.validate(dimensionKey, score, observation, evidenceQuote, userAnswer)
  ├─ score ∈ {1,2,3}                                          → 위배 InvalidScore
  ├─ observation @NotBlank + 한국어 음절 정규식 [\\uAC00-\\uD7A3] 1+ 매치
  │    (자모 단독 ㄱ-ㅎ / ㅏ-ㅣ 불포함. 영문/숫자 동반 허용)   → 위배 NonKoreanObservation
  ├─ evidence_quote @NotBlank                                  → 위배 MissingEvidence
  └─ userAnswer.contains(normalize(evidenceQuote))
       (whitespace `\\s+` → 단일 공백 정규화 후 substring)     → 위배 EvidenceNotInUserAnswer

RubricScoringAdapter.adapt(...)
  ├─ 1차 LLM 호출 → dimension 별 validate
  ├─ 통과 dimension → DimensionScore 적재
  └─ 위배 dimension → retry 1회 (보강 prompt: 위배 dimension + field + reason 명시 사용자 메시지)
        ├─ 통과 → 정상 적재
        └─ 재실패 → log.warn("[결함 skip] retry_failed
                              stage=verbal interviewId={} questionId={}
                              dimension={} field={} reason={}",...)
                    (로그 본문에 evidence_quote / observation 본문 미포함 — A09)
                    + Micrometer counter rubric_retry_failed_total{stage=verbal,dimension,field}++
                    + 해당 dimension row 미적재 (fault isolation)
```

## 의존

- 선행: BE-01 (prompt 갱신 — 1차 위배율 자체를 낮춤)
- 외부: Micrometer (기존 `global/config/` 설정 그대로)

## 테스트 케이스

- [ ] **validator (Domain Unit)**:
  - (a) observation 영문 단독 → 위배 `NonKoreanObservation`
  - (b) observation 영문 + 한국어 1음절 동반 → 통과
  - (c) observation 자모 단독 (`ㄱ-ㅎ` / `ㅏ-ㅣ`) → 위배 `NonKoreanObservation`
  - (d) evidence_quote = 루브릭 정의문 (`"팀에서 했어요 수준"`) substring → 위배 `EvidenceNotInUserAnswer`
  - (e) evidence_quote = userAnswer substring (whitespace 정규화 후) → 통과
  - (f) score=0 / 4 → 위배 `InvalidScore`
  - (g) observation null / "" → `MissingObservation`
- [ ] **adapter retry (Infra Integration)**:
  - (a) 1차 위배 (영어 observation) → 2차 retry 정상 → 정상 적재 + 메트릭 미증가
  - (b) 1차/2차 모두 위배 → 해당 dimension row 미적재 + `[결함 skip] retry_failed` 로그 + 메트릭 +1
  - (c) dimension A 위배 + B 정상 → A retry → B 정상 적재 (fault isolation 회귀)
  - (d) 메트릭 라벨 = `stage=verbal,dimension={key},field={fieldName}` 정확성 assert
- [ ] **service (Service Integration)**:
  - 한국어 observation / verbatim evidence assert + rubric anchor 정의문 substring 미포함 assert
- [ ] **회귀**: `./gradlew test --tests "*Rubric*"` 전체 green

## 완료 기준

- [ ] 변경 파일 commit (논리 단위 1-2 커밋 — validator 신규 / adapter retry 둘로 분리 권장)
- [ ] PR#1 묶음 회귀 green (`./gradlew test` + `./gradlew build`)
- [ ] 메트릭 counter `rubric_retry_failed_total{stage=verbal,dimension,field}` 정상 등록 (Micrometer registry 조회 검증)
- [ ] **`code-reviewer-backend` 실행** (PR#1 머지 직전, MANDATORY)
- [ ] Phase 1 검증 게이트: dev 인터뷰 1회 직접 검증 + 한국어 observation / verbatim evidence DB 확인 + 사용자 명시 승인 (Phase 2 진입 차단 게이트)

## 커밋 메시지

```
feat(BE): RubricScorerResponseValidator 신규 + adapter retry 1회
```

(또는 분리)

```
feat(BE): RubricScorerResponseValidator dimension 검증 정책 신규
feat(BE): RubricScoringAdapter retry 1회 + 메트릭 counter
```

## 비고

- validator 위치 = `infra/ai/adapter/` (도메인 무관 정책 — Phase 2a Lambda 측 `dimension_validator.py` 와 정책 단일 출처).
- Phase 2a 진입 시 Lambda 측 validator 와 fixture 동일 케이스 표 공유 (정책 drift 방지 — tech-spec §244 P1-D).
- 보안 (A09): 로그 컨텍스트 = `interviewId / questionId / stage / dimension / field / reason` 만. transcript / observation / evidenceQuote 본문 미포함.
