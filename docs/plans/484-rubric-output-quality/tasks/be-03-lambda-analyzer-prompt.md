# Task BE-03 — Lambda analyzer prompt 전환 (dimension 채점 + raw 자연어 산출 지시 제거)

> **Phase**: 2a (Lambda + BE 묶음 PR)
> **답하는 질문**: gemini_analyzer / vision_analyzer LLM 호출 prompt 를 어떻게 dimension 채점 prompt 로 전환?

---

## 목적

`gemini_analyzer._ANSWER_SYSTEM_TEMPLATE` + `vision_analyzer._SYSTEM_PROMPT` 본문 = 자유서술 (positive/negative/suggestion) + raw 자연어 (말 속도 묘사 / 톤 라벨 / 시선 묘사 / 자세 묘사 / 표정 라벨) 산출 지시 제거. 직접 차원별 (score + 한국어 observation + transcript substring evidenceQuote) 만 산출하도록 전환. rubric YAML measurement / observable = LLM 채점 가이드로 prompt 안에 인용. LLM 호출 횟수 = 1회 유지. AC-3 / AC-4 회복의 prompt 단 책임.

## 에이전트

- **구현**: `prompt-engineer` — LLM 1회 호출 안에서 자유서술 + raw 산출 지시를 dimension 채점 지시로 전환하는 prompt 재설계. rubric 가이드 인용 위치 / few-shot 예시 / observation 톤 결정. 사용자 발화 ("raw 완전삭제. 점수는 dimension 기반으로 점수 생성") 의 prompt 단 구현.
- **리뷰**: PR#2 (Phase 2a) 머지 직전 `code-reviewer-backend` 통합 리뷰.

## 변경 파일

- `lambda/analysis/analyzers/gemini_analyzer.py`
  - `_FALLBACK_ANSWER` (line 29-43) — 자유서술 키 + vocal raw 4 키 제거 + `nonverbalDimensions: {fluency, confidence_tone}` 신설
  - `_ANSWER_SYSTEM_TEMPLATE` (line 45-99+) — **dimension 채점 prompt 로 전면 전환**:
    - 삭제: vocal/attitude/overall_delivery 자유서술 3섹션 산출 지시
    - 삭제: vocal raw 자연어 산출 지시 (`speechPace` 라벨 / `toneConfidenceLevel` enum / `emotionLabel` enum / `speedVariance` 수치 묘사 line 82 / 84-87 / 89-93 / 97 추정)
    - 유지: `transcript` + `vocal.fillerWords` + `vocal.fillerWordCount` 산출 지시 (필러 예외)
    - 신설: `nonverbalDimensions.{fluency, confidence_tone}` 각 `{score 1-3, observation 한국어 1~2문장, evidence_quote transcript substring}` 산출 지시
    - 신설: rubric YAML measurement / observable 인용 (build-time copy `lambda/analysis/rubric/_dimensions.yaml` 의 fluency / confidence_tone 섹션 — 채점 가이드)
- `lambda/analysis/analyzers/vision_analyzer.py`
  - `analyze_frames(frame_paths)` 시그니처 → `analyze_frames(frame_paths, transcript)` 확장 (evidence_quote source = transcript substring 룰 정합 — G3 결정)
  - `_SYSTEM_PROMPT` (line 48-99) — **dimension 채점 prompt 로 전면 전환**:
    - 삭제: positive/negative/suggestion 자유서술 산출 지시
    - 삭제: vision raw 자연어 산출 지시 (`postureLevel` 라벨 / `expressionLabel` enum / `eyeContactLevel` 라벨 / `gazeOnCameraRatio` 수치 / `postureUnstableCount` 카운트 line 68-83 / 85-88 / 90-93 / 95-97 추정)
    - 신설: `nonverbalDimensions.eye_contact_posture` `{score 1-3, observation 한국어, evidence_quote ∈ transcript}` 산출 지시
    - 신설: rubric YAML eye_contact_posture measurement / observable 인용 (시선 + 자세 + 표정 흡수 표현)
- `lambda/analysis/rubric/_dimensions.yaml` + `nonverbal-rubric.yaml` (deploy 스크립트 build-time copy 산물 — 본 Task 가 직접 작성 X, BE-06 결과를 deploy 스크립트가 copy. BE-04 의 deploy 스크립트 갱신 묶음)

## 핵심 로직 / 변경 요약

```
[Pre]  gemini prompt = audio → 4섹션 자유서술 (vocal/attitude/overall) + vocal raw 4 측정치
       vision prompt = frames → positive/negative/suggestion + vision raw 5 측정치
       → 자유서술 + raw 동시 산출 → 단일 출처 결정 불가

[Post] gemini prompt = "transcript + audio metadata + rubric 가이드 (fluency / confidence_tone)
                        → 차원별 (score 1-3 + 한국어 observation + transcript substring evidenceQuote)
                        + transcript + fillerWords + fillerWordCount 만 산출.
                        raw 자연어 (말 속도 묘사 / 톤 라벨 / 더듬 횟수 / 감정 라벨) 산출 X."
       vision prompt = "frame samples + transcript + rubric 가이드 (eye_contact_posture)
                        → eye_contact_posture 차원 (score + 한국어 observation +
                          transcript substring evidenceQuote) 만 산출.
                        raw 자연어 (시선 묘사 / 자세 묘사 / 표정 라벨) 산출 X."
       LLM 호출 횟수 = 1회 유지 (응답 토큰 -30%~+10% 추정)
```

## 의존

- 선행: Phase 1 머지 + dev 검증 게이트 통과 (사용자 명시 승인)
- 선행: BE-06 (rubric YAML 갱신) — prompt 가 가이드로 인용 → BE-06 의 measurement / observable 본문이 prompt 인용 본문 확정
- 외부: Gemini API + OpenAI GPT-4o Vision (기존 의존 그대로)

## 테스트 케이스 (BE-04 와 동일 PR 머지 — 본 Task 단독은 prompt + 응답 스키마 자산)

- [ ] **prompt 단위 테스트** (Lambda pytest):
  - (a) gemini prompt 본문에 "fluency" / "confidence_tone" + "score" + "observation" + "evidence_quote" + rubric measurement 키워드 ("필러" / "더듬" / "속도") substring 포함 assert
  - (b) gemini prompt 본문에 raw 자연어 산출 지시 키워드 (`speechPace` / `toneConfidenceLevel` / `emotionLabel` / `speedVariance`) substring 부재 assert
  - (c) vision prompt 본문에 "eye_contact_posture" + rubric measurement 키워드 ("시선" / "자세" / "표정") substring 포함 assert
  - (d) vision prompt 본문에 raw 자연어 산출 지시 키워드 (`eyeContactLevel` / `postureLevel` / `expressionLabel` / `gazeOnCameraRatio` / `postureUnstableCount`) substring 부재 assert
- [ ] **응답 스키마** (BE-04 와 묶음):
  - gemini 응답 stub → `nonverbalDimensions.{fluency, confidence_tone}` 각 `{score, observation, evidence_quote}` 구조 assert + 자유서술 키 부재 + vocal raw 4 키 부재
  - vision 응답 stub → `nonverbalDimensions.eye_contact_posture` 단일 구조 + 자유서술 / vision raw 5 키 부재 + `evidence_quote ∈ transcript` assert
- [ ] **max_tokens 실측** (P1-C, 구현 진입 직전):
  - dev 환경 dry-run 1회 → gemini / vision 응답 token 실측 → max_tokens 한도 사전 검증 → 부족 시 한도 조정 line 동일 PR 합류

## 완료 기준

- [ ] 변경 파일 commit (analyzer 2개 — gemini / vision 분리 커밋 권장)
- [ ] BE-04 / BE-05 / BE-06 / BE-07 와 PR#2 묶음 회귀 green (`cd lambda/analysis && pytest` + `./gradlew test`)
- [ ] prompt grep 검증: `grep -nE "(speechPace|toneConfidenceLevel|emotionLabel|speedVariance|eyeContactLevel|postureLevel|expressionLabel|gazeOnCameraRatio|postureUnstableCount)" lambda/analysis/analyzers/gemini_analyzer.py lambda/analysis/analyzers/vision_analyzer.py` 결과 = 0 매치 (raw 산출 지시 잔존 부재)
- [ ] **`code-reviewer-backend` 실행** (PR#2 머지 직전, MANDATORY)
- [ ] Phase 2a 검증 게이트: dev 인터뷰 1회 + `question_score_dimension` rubric_id="nonverbal-v1" 3차원 적재 확인 + 10컬럼 NULL 확인 + 운영자 리뷰 게이트 (9건 체크리스트) + 사용자 승인

## 커밋 메시지

```
refactor(lambda): gemini_analyzer dimension 채점 prompt 전환
refactor(lambda): vision_analyzer dimension 채점 prompt + transcript 입력 추가
```

## 비고

- max_tokens 실측 timing (P1-C) = 본 Task 구현 진입 직전 dev dry-run. 응답 길이 = 자유서술 3섹션 + raw 9 키 삭제 vs dimension 2-3 + observation/evidenceQuote 추가 → 추정 -30%~+10%. 실측 후 한도 조정 PR 같이.
- evidence_quote source 결정 (G3): vision 도 transcript substring 강제. analyzer 가 frame 만 보고 산출하던 기존과 달리 transcript 입력 추가 후 prompt 안에 "evidence_quote = transcript substring" 명시.
- rubric YAML 가이드 인용 = 단일 출처 = `_dimensions.yaml` (BE-06 산물). deploy 스크립트 (BE-04) 가 build-time copy.
- 보안 (OWASP A03): prompt 안 transcript 영역 마커 (`<<<USER_ANSWER>>>...` 또는 동등) 보존 — 사용자 발화 injection 차단.
