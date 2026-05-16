# Task BE-06 — rubric YAML 갱신 (`_dimensions.yaml` composure 삭제 + 3차원 measurement / observable 확장 + `nonverbal-rubric.yaml` 정리)

> **Phase**: 2a (Lambda + BE 묶음 PR)
> **답하는 질문**: rubric YAML 단일 출처를 어떻게 갱신해 LLM 채점 가이드 + raw 신호 9종 흡수 표현을 모두 담을지?

---

## 목적

`_dimensions.yaml` 의 composure 정의 삭제 + fluency / confidence_tone / eye_contact_posture 3차원 measurement + observable 본문을 확장해 raw 신호 9종 (vocal 4: speechPace / toneConfidenceLevel / emotionLabel / speedVariance / vision 5: eyeContactLevel / postureLevel / expressionLabel / gazeOnCameraRatio / postureUnstableCount) 의 측정 표현을 모두 흡수. `nonverbal-rubric.yaml` 에서 composure 참조 모두 제거 + weight 재분배. 이 YAML 본문이 BE rubric 카탈로그 + Lambda analyzer prompt 의 채점 가이드 인용원 = 단일 출처. AC-4 회복의 가이드 단 책임 + 운영자 리뷰 게이트 (P1-E) 입력.

## 에이전트

- **구현**: `prompt-engineer` — measurement / observable 한국어 표현 / L1·L2·L3 신호 어휘 / raw 9종 흡수 매핑 / 표정·시선·자세 통합 표현 설계. 사용자 결정 ("raw 완전삭제. rubric measurement 가 LLM 채점 가이드 역할") 의 YAML 단 구현.
- **리뷰**: PR#2 머지 직전 `code-reviewer-backend` (YAML 자산 = backend resources / BE 카탈로그 로더 + Lambda prompt 인용 양쪽 영향 → backend reviewer 가 통합 검토).

## 변경 파일

- `backend/src/main/resources/rubric/_dimensions.yaml`
  - line 251-273 `fluency`:
    - description: "필러 워드가 답변 전달을 방해하지 않는가" → "필러·말 더듬·발화 속도 변동이 답변 전달을 방해하지 않는가"
    - measurement: `verbal.filler_word_count` → "필러 횟수 + 더듬·재시작 빈도 + 발화 속도 변동 (이전 raw `speechPace` / `speedVariance` 흡수)"
    - scoring L1/L2/L3 observable 각각 1줄 추가 (더듬·재시작 빈도 / 속도 변동 신호)
  - line 275-297 `confidence_tone`:
    - description 유지 "톤과 발화 속도 변동이 자신감 있게 유지되는가"
    - measurement: `verbal.tone_label + verbal.speedVariance` → "톤 안정성 (단정형 어미 / 끝맺음 명확 / 음량 떨림) + 발화 속도 분산 (이전 raw `toneConfidenceLevel` / `emotionLabel` / `speedVariance` 흡수)"
    - scoring L1/L2/L3 observable 어휘 재표현 (enum 라벨 → 관찰 어휘. 단정형 / 추측형 / 음량 떨림 / 끝 흐림)
  - line 299-321 `eye_contact_posture`:
    - description: "시선과 자세가 안정적으로 유지되는가" → "시선·자세·표정이 안정적으로 유지되는가"
    - measurement: `vision.gazeOnCameraRatio + vision.postureUnstableCount` → "카메라 응시 비율 + 자세 안정성 (흔들림·기울임 빈도) + 표정 안정성 (NERVOUS / UNCERTAIN / NEUTRAL 등 — 이전 raw `eyeContactLevel` / `postureLevel` / `expressionLabel` / `gazeOnCameraRatio` / `postureUnstableCount` 흡수)"
    - scoring L1/L2/L3 observable 표정 신호 1줄씩 추가 (L1 NERVOUS / UNCERTAIN 30% 이상 / L2 평탄 또는 일부 긴장 / L3 ENGAGED / CONFIDENT 또는 NEUTRAL 이완)
  - line 323-345 `composure` 섹션 **전체 삭제** (정의 폐기 — 사용자 명시 "압박 이런거 없어도 될것같은데").
- `backend/src/main/resources/rubric/nonverbal-rubric.yaml` (line 1-40)
  - `uses_dimensions` 4개 → 3개 (composure 제거). weight 1/3 각 또는 명시 동등 분배.
  - `per_turn_rules.default = [fluency, confidence_tone, eye_contact_posture]` 단일 (난이도 분기 폐기 — `medium_or_hard` block 삭제).
  - `level_expectations.must_reach_*` 에서 composure 참조 제거.
  - `data_source` 라인 의미 동일 (영역 키 분리 페이로드 `nonverbalScore.vocal.dimensions[]` / `vision.dimensions[]` 의 `dimension_ref` 키로 라우팅).
- `backend/src/test/java/.../RubricCatalogTest.java` — 추가 시나리오 (composure 부재 / 3차원 measurement substring 포함).
- (build-time copy 대상) `lambda/analysis/rubric/_dimensions.yaml` + `lambda/analysis/rubric/nonverbal-rubric.yaml` — BE-04 의 deploy 스크립트가 본 Task 산물을 build-time copy. 본 Task 가 Lambda 측 파일 직접 작성 X.

## 핵심 로직 / 변경 요약

```
[Pre]  _dimensions.yaml = 4차원 (fluency / confidence_tone / eye_contact_posture / composure).
       measurement 본문 = 결정론 매퍼 input 명 (filler count / tone label / gazeOnCameraRatio)
       → LLM 채점 가이드 역할 미흡. raw 9종 측정 표현 부재.
       nonverbal-rubric.yaml = uses_dimensions 4개 + medium_or_hard 분기.

[Post] _dimensions.yaml = 3차원. composure 정의 삭제.
       measurement 본문 = LLM 채점 가이드 표현 (필러·더듬·속도 / 톤·속도 분산 /
       시선·자세·표정) — raw 9종 측정 표현 흡수.
       L1/L2/L3 observable 어휘 = 관찰 가능한 신호 (enum 라벨 의존 제거).
       nonverbal-rubric.yaml = uses_dimensions 3개 (각 weight 1/3) + default 단일.
```

## 의존

- 선행: 없음 (Phase 2a 진입 후 BE-03 prompt 갱신과 병렬 작성 가능 — prompt 가 본 YAML 본문 인용)
- 후행: BE-04 deploy 스크립트 build-time copy 가 본 YAML 산물 의존. BE-03 analyzer prompt 가 본 YAML measurement / observable 본문 인용.
- 외부: 없음.

## 테스트 케이스

- [ ] **`RubricCatalogTest.no_composure_dimension_when_nonverbal_v1_loaded`** (Domain Unit, `extends DomainUnitSupport`):
  - `nonverbal-v1` rubric `selectDimensions(...)` 결과에 `composure` 부재 assert
  - fluency description / measurement 본문에 "떨림" / "끊김" / "속도" substring 1+ 포함 assert (raw `speechPace` / `speedVariance` 흡수 표현 검증)
  - confidence_tone measurement 본문에 "톤" + "속도" substring 포함 assert (raw `toneConfidenceLevel` / `emotionLabel` / `speedVariance` 흡수)
  - eye_contact_posture description / measurement 본문에 "표정" + "시선" + "자세" substring 포함 assert (raw `expressionLabel` / `eyeContactLevel` / `gazeOnCameraRatio` / `postureUnstableCount` 흡수)
- [ ] **`nonverbal-rubric.yaml` 로더 회귀**:
  - `uses_dimensions.length == 3` assert
  - `per_turn_rules.default = [fluency, confidence_tone, eye_contact_posture]` assert
  - `medium_or_hard` 키 부재 assert
- [ ] **운영자 리뷰 게이트 (P1-E) — 9건 체크리스트** (사용자 명시 승인):
  - (a) fluency measurement 에 raw `speechPace` 측정 표현 흡수
  - (b) fluency measurement 에 raw `speedVariance` 측정 표현 흡수
  - (c) confidence_tone measurement 에 raw `toneConfidenceLevel` 측정 표현 흡수
  - (d) confidence_tone measurement 에 raw `emotionLabel` 측정 표현 흡수
  - (e) confidence_tone measurement 에 raw `speedVariance` 측정 표현 흡수 (fluency 와 중복 흡수 — 평가 축 다름)
  - (f) eye_contact_posture measurement 에 raw `eyeContactLevel` 측정 표현 흡수
  - (g) eye_contact_posture measurement 에 raw `postureLevel` 측정 표현 흡수
  - (h) eye_contact_posture measurement 에 raw `expressionLabel` 측정 표현 흡수
  - (i) eye_contact_posture measurement 에 raw `gazeOnCameraRatio` / `postureUnstableCount` 측정 표현 흡수
  - 책임자: prompt-engineer (체크리스트 작성) + 사용자 (항목별 명시 승인)
  - 게이트 시점: Phase 2a 머지 게이트 = 회귀 테스트 green + 사용자 명시 승인 동시

## 완료 기준

- [ ] 변경 파일 commit (단일 커밋 권장 — YAML 2개 묶음, `_dimensions.yaml` + `nonverbal-rubric.yaml`)
- [ ] PR#2 묶음 회귀 green (`./gradlew test --tests "*Rubric*"`)
- [ ] composure grep 검증: `grep -rn "composure" backend/src/main/resources/rubric/` 결과 = 0 (정의 + 참조 모두 부재)
- [ ] **`code-reviewer-backend` 실행** (PR#2 머지 직전, MANDATORY)
- [ ] **운영자 리뷰 게이트 9건 사용자 명시 승인** (P1-E, MANDATORY)

## 커밋 메시지

```
refactor(BE): _dimensions.yaml 3차원 measurement 확장 + composure 삭제
refactor(BE): nonverbal-rubric.yaml composure 참조 제거 + weight 재분배
```

(또는 단일)

```
refactor(BE): rubric YAML 3차원 단일화 + raw 신호 측정 표현 흡수
```

## 비고

- measurement / observable 본문 = Lambda analyzer prompt (`gemini_analyzer._ANSWER_SYSTEM_TEMPLATE` + `vision_analyzer._SYSTEM_PROMPT`) 가 채점 가이드로 인용. 본 YAML 본문이 단일 출처 → Lambda build-time copy (BE-04 deploy 스크립트) 로 동기화. 코드 / 문서 2곳 동기화 회피.
- 9종 raw 신호 완전성 검증 = 회귀 테스트 (특정 substring 포함) + 사용자 명시 승인 (체크리스트 9건) 이중 보호.
- 보안 (OWASP A03): YAML 본문에 사용자 발화 / 동적 substitution 부재 → injection 표면 부재.
