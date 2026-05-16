# Task BE-04 — Lambda `dimension_validator.py` + handler 영역 키 페이로드 + 매퍼 삭제

> **Phase**: 2a (Lambda + BE 묶음 PR)
> **답하는 질문**: Lambda analyzer 응답 검증 / retry / handler 페이로드 조립을 어떻게 구현?

---

## 목적

Lambda 측 dimension 단위 응답 검증 정책 (BE-02 `RubricScorerResponseValidator` 와 동일 케이스 fixture 공유) + retry 1회 + handler 영역 키 분리 페이로드 조립 (TO-1 채택안). `nonverbal_rubric_mapper.py` 폐기 + 자유서술 4종 / raw 9종 적재 line 삭제. deploy 스크립트에 rubric YAML build-time copy 추가.

## 에이전트

- **구현**: `backend` — Python 코드 / handler 페이로드 조립 / deploy 스크립트 갱신. (Lambda 전담 agent 부재 — backend agent 가 Python Lambda 도 담당, tech-spec §1045 명시).
- **리뷰**: PR#2 머지 직전 `code-reviewer-backend`.

## 변경 파일

- `lambda/analysis/analyzers/dimension_validator.py` — **신규** (1 모듈). gemini / vision analyzer 공유. 정책:
  - score ∈ {1, 2, 3}
  - observation @NotBlank + 한국어 음절 정규식 `[가-힣]` 1+ 매치
  - evidence_quote @NotBlank + transcript whitespace 정규화 후 substring
  - 위배 사유 enum (`InvalidScore` / `MissingObservation` / `NonKoreanObservation` / `MissingEvidence` / `EvidenceNotInTranscript`)
- `lambda/analysis/analyzers/gemini_analyzer.py` (BE-03 prompt 갱신 line 외)
  - `analyze_answer_audio(...)` 응답 후 dimension 단위 validator 호출 + retry 1회 (보강 prompt 메시지 추가) + 재실패 dimension omit + `[결함 skip] retry_failed stage=gemini-audio dimension=X field=Y` 로그
- `lambda/analysis/analyzers/vision_analyzer.py` (BE-03 prompt 갱신 line 외)
  - `analyze_frames(frame_paths, transcript)` 응답 후 동일 validation + retry 1회 + omit 로그 `stage=vision`
- `lambda/analysis/handler.py`
  - line 23-25: `from analyzers.nonverbal_rubric_mapper import NonverbalRubricMapper` import 제거 + 모듈 전역 `_RUBRIC_MAPPER` 제거
  - line 30-41: `_build_nonverbal_score(...)` 함수 **삭제**
  - line 268-321: `_run_gemini_pipeline` 의 timestamp_feedback 조립부 재작성
    - 삭제: `fb["nonverbalComment"]` / `fb["vocalComment"]` / `fb["attitudeComment"]` / `fb["overallComment"]` (자유서술 4종)
    - 삭제 (구현 진입 직전 `grep -n 'fb\\["' lambda/analysis/handler.py` 로 실제 적재 line 식별 후 부재 키 제거 — 부분집합 가능): `fb["speechPace"]` / `fb["toneConfidenceLevel"]` / `fb["emotionLabel"]` / `fb["speedVariance"]` / `fb["eyeContactLevel"]` / `fb["postureLevel"]` / `fb["expressionLabel"]` / `fb["gazeOnCameraRatio"]` / `fb["postureUnstableCount"]`
    - 유지: `fb["transcript"]` / `fb["fillerWords"]` / `fb["fillerWordCount"]` (필러 예외)
    - 신설: `fb["nonverbalScore"]` 영역 키 분리 조립 (TO-1):
      ```
      nonverbalScore = {}
      if gemini.nonverbalDimensions:
          nonverbalScore["vocal"] = {"dimensions": [
              {"dimension_ref": "fluency",         **gemini.nonverbalDimensions.fluency},
              {"dimension_ref": "confidence_tone", **gemini.nonverbalDimensions.confidence_tone},
          ]}
      if vision.nonverbalDimensions:
          nonverbalScore["vision"] = {"dimensions": [
              {"dimension_ref": "eye_contact_posture", **vision.nonverbalDimensions.eye_contact_posture},
          ]}
      fb["nonverbalScore"] = nonverbalScore if nonverbalScore else None
      ```
    - `api_client.save_feedback(...)` 호출 시 `isNonverbalCompleted=True` 유지 (부분 실패 graceful — FE 폴링 무한 로딩 차단)
  - line 326-444: `_run_legacy_pipeline` 동일 cleanup. **`nonverbalScore=None` 반환** (legacy 전용 dimension prompt 작성 비용 > 가치 — P1-B 결정 사유). FE Empty 카드 graceful.
- `lambda/analysis/analyzers/nonverbal_rubric_mapper.py` — **파일 삭제** (119 lines, plan-11 도입).
- `lambda/analysis/tests/test_nonverbal_rubric_mapper.py` (존재 시) — **파일 삭제**.
- `lambda/analysis/tests/test_gemini_analyzer.py` / `test_vision_analyzer.py` / `test_handler.py` — 신규 회귀 시나리오 (응답 스키마 / retry / omit / 영역 키 분리 / fb 키 부재).
- `lambda/analysis/tests/test_dimension_validator.py` — **신규**. BE-02 `RubricScorerResponseValidatorTest` 와 동일 fixture 케이스 (정책 drift 방지).
- `lambda/deploy.sh` 또는 `lambda/lambda-safe-deploy.sh` — rubric YAML build-time copy 추가:
  ```
  cp backend/src/main/resources/rubric/_dimensions.yaml      lambda/analysis/rubric/
  cp backend/src/main/resources/rubric/nonverbal-rubric.yaml lambda/analysis/rubric/
  ```
  배포 패키지에 포함되도록 zip 단계 합류.

## 핵심 로직 / 변경 요약

```
[Pre]  handler = 자유서술 4종 + 결정론 매퍼 점수 + raw 9종 적재
       analyzer = LLM 응답 검증 / retry 부재

[Post] analyzer = 응답 후 dimension_validator.validate(...) → 위배 시 retry 1회
                  → 재실패 dimension omit + 로그 + (Lambda 측 메트릭은 CloudWatch
                    logs metric filter 사용 — Micrometer 부재)
       handler = "vocal" / "vision" 영역 키만 부여해 analyzer 산출 동봉 +
                  4종 자유서술 / raw 9 키 적재 line 부재 + 필러 line 유지 +
                  nonverbal_rubric_mapper import / 호출 0
       deploy 스크립트 = BE rubric YAML build-time copy → Lambda prompt 가
                  채점 가이드로 인용 (단일 출처 = backend/src/main/resources/rubric/)
```

## 의존

- 선행: BE-03 (analyzer prompt 전환) — 본 Task 가 validator + handler 조립 책임
- 외부: 없음 (기존 Lambda runtime + Gemini + GPT-4o Vision)

## 테스트 케이스 (Lambda pytest)

- [ ] **validator** (`test_dimension_validator.py`):
  - BE-02 fixture 와 동일 케이스 (a)~(g) — 정책 drift 회귀
- [ ] **analyzer retry** (`test_gemini_analyzer.py` / `test_vision_analyzer.py`):
  - (a) 1차 위배 → 2차 retry 정상 → 정상 반환
  - (b) 1차/2차 모두 위배 → dimension omit + 로그
  - (c) 부분 omit → 응답 dict 에 유효 dimension 만 잔존
- [ ] **handler 응답 스키마** (`test_gemini_analyzer.py.test_response_has_nonverbal_dimensions_when_normal_audio`):
  - `transcript` / `vocal.fillerWords` / `vocal.fillerWordCount` 유지 + 자유서술 키 0 + vocal raw 4 키 0 + `nonverbalDimensions.{fluency, confidence_tone}` 단일 구조
- [ ] **handler 응답 스키마** (`test_vision_analyzer.py.test_response_has_eye_contact_posture_dimension`):
  - `analyze_frames(frames, transcript)` 호출 + 자유서술 키 0 + vision raw 5 키 0 + `nonverbalDimensions.eye_contact_posture` 단일 + `evidence_quote ∈ transcript`
- [ ] **handler payload** (`test_handler.py.test_payload_has_no_raw_metrics`):
  - `_run_gemini_pipeline` + `_run_legacy_pipeline` 결과 `fb` 에 raw 9 키 부재 + 자유서술 4 키 부재 + 필러 키 (`fillerWords` / `fillerWordCount`) 유지
- [ ] **handler 영역 키 분리** (`test_handler.py.test_area_split_assembly_in_both_pipelines`):
  - 두 파이프라인 모두 `fb["nonverbalScore"]["vocal"]["dimensions"]` (fluency / confidence_tone) + `fb["nonverbalScore"]["vision"]["dimensions"]` (eye_contact_posture) 영역 키 분리 조립 정확성
  - 부분 실패 (gemini 만 → `vocal` 키만 / vision 만 → `vision` 키만 / 둘 다 → `nonverbalScore=None`) + `isNonverbalCompleted=true` 유지
- [ ] **handler legacy**:
  - `_run_legacy_pipeline` → `nonverbalScore=None` 반환 assert + 자유서술 4 키 / raw 9 키 부재
- [ ] **dead code** (pytest collect):
  - `nonverbal_rubric_mapper.py` 파일 부재 + import grep 0

## 완료 기준

- [ ] 변경 파일 commit (논리 단위 분리 권장 — validator / analyzer retry / handler 조립 / 매퍼 삭제 / deploy 스크립트)
- [ ] PR#2 묶음 회귀 green (`cd lambda/analysis && pytest`)
- [ ] `nonverbal_rubric_mapper.py` import grep = 0 (`grep -rn "nonverbal_rubric_mapper" lambda/`)
- [ ] deploy 스크립트 dry-run 1회 — rubric YAML copy 정상 동작
- [ ] **`code-reviewer-backend` 실행** (PR#2 머지 직전, MANDATORY)

## 커밋 메시지

```
feat(lambda): dimension_validator + analyzer retry 1회
refactor(lambda): handler 영역 키 페이로드 + 매퍼 삭제
chore(lambda): rubric YAML build-time copy 추가
```

## 비고

- legacy pipeline (Whisper + GPT-4o) 의 dimension 산출 = 부재 결정 (P1-B). 사유: 정확도 보장 곤란 + legacy 사용 빈도 < 비용. FE Empty 카드 graceful.
- 부재 키 처리 (P0-C): 본 Task 진입 직전 `grep -n 'fb\\["' lambda/analysis/handler.py` 로 실제 적재 line 부재 키 식별 → 부재 키는 삭제 대상에서 제외.
- 메트릭: Lambda 측은 Micrometer 부재 → CloudWatch logs metric filter 또는 EMF (Embedded Metric Format). 본 spec 범위 = 로그만 (메트릭 백엔드 결정 = `tech-spec.md` 관찰성 NF, 기존 application 설정 따름).
- 보안 (A09): 로그 본문에 transcript / observation / evidence_quote 본문 미포함. `interviewId / questionId / stage / dimension / field / reason` 만.
