# Tech Spec — rubric 출력 품질 회복 (단일 패턴 통일)

> **작성자**: backend agent (Staff Engineer 페르소나)
> **답하는 질문**: 어떻게? 구조 / API / 데이터 / Trade-off
> **승인 게이트**: ★ 사용자 명시 승인 후 implement-be.md / implement-fe.md 진입 ★
> **대응 product-spec**: `docs/plans/484-rubric-output-quality/product-spec.md`
> **상태**: 재설계 (product-spec 확장 반영)

---

## Why → Goal (1줄 미러)

면접 유형 → 매핑된 루브릭 → 그 루브릭으로 차원별 (점수 + 한국어 코칭 문장 + 사용자 답변 발췌) 단일 구조 산출. verbal / 비언어 모두 동일. 자유서술 4종 / 결정론 보조 매퍼 / **원시 측정치 9종 (vocal 4: speechPace, toneConfidenceLevel, emotionLabel, speedVariance / vision 5: eyeContactLevel, postureLevel, expressionLabel, gazeOnCameraRatio, postureUnstableCount)** / 압박 안정성 차원 = 응답·코드·DB·화면 모두에서 부재 (예외: 필러 카운트 배지 1건은 결과 화면 유지 — 사용자 명시 결정). **raw 9종은 산출 자체가 부재** — Lambda 의 gemini / vision analyzer LLM 호출이 raw 자연어 (말 속도 묘사 / 톤 라벨 / 더듬 횟수 / 시선 묘사 / 자세 묘사 / 표정 라벨) 를 생성하는 단계 없이, **직접 차원별 (점수 1-3 + 한국어 observation + transcript substring evidenceQuote) 만 산출** (사용자 명시 2026-05-16: "내부 raw를 꼭 응답 받아야해? 그냥 dimension에 결정된 내용만 응답받으면 안되나. raw 완전삭제. 점수는 dimension 기반으로 점수 생성"). rubric YAML 의 measurement / observable 정의 = LLM 의 **채점 가이드** 역할 (이전 갱신 = "내부 채점 입력" 표현 → 본 갱신 = "LLM 채점 가이드"). 단 필러 카운트 (`fillerWordCount`) 만 raw 신호 예외 유지 — gemini_analyzer 가 dimension 산출과 별도로 필러 횟수 산출 + 응답 / DB 잔존 (FE 배지용). 음성 분석기 (Gemini) / 영상 분석기 (vision) 각자 자기 영역 차원 채점 → Lambda 가 영역 키 분리 페이로드로 동봉 → BE 가 차원 row insert (조립 = trivial) → FE 단일 흐름 표시. **LLM 호출 횟수 = 1회 유지** (latency / 비용 변동 X). prompt 만 dimension 채점 prompt 로 전환.

---

## Evidence

### 현재 구조 (코드 진입점 / 경로)

#### verbal scorer (BE)

- `backend/src/main/java/com/rehearse/api/infra/ai/prompt/RubricScorerPromptBuilder.java:35-87, 94-98` — system prompt 본문 영어. `@Value("${rehearse.rubric-scorer.temperature:0.2}")` + `maxTokens` `@Value` 주입. `buildSystemPrompt()` 가 영어 단일 문자열.
- `backend/src/main/resources/prompts/template/turn-rubric-scorer.txt:6-72` — 모든 instructional 텍스트 영어. line 34 `evidence_quote MUST be a verbatim excerpt (≤40 words) from the candidate answer` 룰 존재하나 약함. line 43-53 Output Schema 영어 예시 + line 56-72 한국어 Scoring Example 혼재 = LLM 출력 언어 비결정 학습 신호.
- `backend/src/main/resources/rubric/_dimensions.yaml:130-141` — `experience_concreteness` 의 L1 observable (`"팀에서 했어요 수준"`) 이 prompt 내 dimension 정의문으로 주입 → LLM 이 evidence_quote 후보로 그대로 재인용 (DB 30/30 확인).
- `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/service/RubricScorer.java:33-63` — verbal scorer 진입점. `Rubric.selectDimensions(resumeMode)` → `promptBuilder.build(...)` → `adapter.adapt(aiClient, request, rubric, dimensionsToScore)` → `RubricScoringResult` 반환.
- `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/entity/DimensionScore.java:1-24` — record `(Integer score, String observation, String evidenceQuote)`. `@JsonCreator of(...)` 정적 팩토리. `notApplicable(reason)` 헬퍼 = `(null, reason, null)`.

#### 비언어 산출 (Lambda + BE 적재)

- `lambda/analysis/handler.py:23-25` — `from analyzers.nonverbal_rubric_mapper import NonverbalRubricMapper` + `_RUBRIC_MAPPER = NonverbalRubricMapper()` 모듈 전역.
- `lambda/analysis/handler.py:30-41` — `_build_nonverbal_score(verbal_dict, vision_dict, prev_score, difficulty)` = 결정론 매퍼 진입.
- `lambda/analysis/handler.py:268-321` — `_run_gemini_pipeline` 의 timestamp_feedback 조립부. `fb["vocalComment"] = _comment_block(vocal)` (line 287), `fb["attitudeComment"] = _comment_block(attitude)` (line 290), `fb["nonverbalComment"] = _comment_block(vision)` (line 302), `fb["overallComment"] = _comment_block(gemini.get("overall_delivery"))` (line 304), `fb["nonverbalScore"] = score` (line 319). 자유서술 4종 + 결정론 매퍼 점수 동시 적재 = 본 epic 폐기 대상.
- `lambda/analysis/handler.py:326-444` — `_run_legacy_pipeline` (Whisper + GPT-4o 폴백) 도 동일 패턴. 본 epic cleanup 동일 적용.
- `lambda/analysis/analyzers/gemini_analyzer.py:29-43` — `_FALLBACK_ANSWER` 의 `"vocal" / "attitude" / "overall_delivery"` 키 각각 `{positive, negative, suggestion}` 자유서술 구조. **vocal raw 측정치 필드** = `fillerWords` / `speechPace` / `toneConfidenceLevel` / `emotionLabel` / `speedVariance` (line 32-36 — 추정, 구현 직전 grep 검증 필요). 본 epic 에서 응답 스키마 = 차원별 (score + observation + evidence_quote) 단일 구조로 교체 + **raw 측정치 (필러 제외) 산출 단계 자체 제거** (이전 갱신 = "응답 노출 제거" 표현 → 본 갱신 = "산출 자체 부재").
- `lambda/analysis/analyzers/gemini_analyzer.py:45-66+` — `_ANSWER_SYSTEM_TEMPLATE` = "오디오만 근거로 4개 섹션을 JSON 한 번에 출력" 지시. **vocal raw 신호 산출 지시 위치 (추정)**: speechPace (line 82), toneConfidenceLevel (line 84-87), emotionLabel (line 89-93), speedVariance (line 97) — 구현 직전 grep 으로 prompt 본문 line 재확인 필요. fluency / confidence_tone 차원 채점에 필요한 입력 신호 (transcript / 어미 / 속도·리듬 / 필러 / 감정 누설 / 태도) 는 LLM 이 audio 직접 청취하여 dimension prompt 안에서 사용 → raw 자연어 산출 단계 부재한 채 dimension (점수 + observation + evidenceQuote) 만 산출.
- `lambda/analysis/analyzers/vision_analyzer.py:48-99` — `_SYSTEM_PROMPT` = "자세·손/제스처·표정·신체 안정성만 평가". **vision raw 신호 산출 지시 위치 (추정)**: postureLevel (line 68-83), expressionLabel (line 85-88), eyeContactLevel (line 90-93), gazeOnCameraRatio + postureUnstableCount (line 95-97) — 구현 직전 grep 으로 prompt 본문 line 재확인 필요. eye_contact_posture 차원 채점에 필요한 입력 신호 (frame samples / 시선·자세·표정) 는 LLM 이 frame 직접 인지하여 dimension prompt 안에서 사용. 응답에는 자유서술 (positive/negative/suggestion) + raw 측정치 5종 동거 (현행). **본 epic 에서 raw 산출 단계 자체 폐기** (사용자 명시 "raw 완전삭제. 점수는 dimension 기반으로 점수 생성"). 응답·prompt·analyzer 어디에도 raw 자연어 산출 단계 부재.
- `lambda/analysis/handler.py:268-321` — `_run_gemini_pipeline` 의 timestamp_feedback 조립부에서 raw 측정치 적재 위치 (추정): vocal raw (line 281-286 추정) = `fb["speechPace"] / fb["toneConfidenceLevel"] / fb["emotionLabel"] / fb["speedVariance"]`, vision raw (line 293-301 추정) = `fb["eyeContactLevel"] / fb["postureLevel"] / fb["expressionLabel"] / fb["gazeOnCameraRatio"] / fb["postureUnstableCount"]`. **구현 진입 직전 `grep -n 'fb\["' lambda/analysis/handler.py` 로 실제 적재 line 재확인 후, 부재 키 (handler 에 실제 `fb["..."]` 적재 line 이 존재하지 않는 키) 는 본 epic Phase 2a 삭제 대상에서 제거**. 본 epic Phase 2a 에서 잔존 raw 적재 line 모두 삭제 (필러 line 278-280 추정 만 유지).
- `lambda/analysis/analyzers/nonverbal_rubric_mapper.py` (119 lines, plan-11 도입) — 결정론 보조 매퍼. fluency = filler 횟수 기준, composure = 이전 turn 차원 비교 + 난이도 분기. 본 epic 폐기 대상.
- `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/service/NonverbalScorePersister.java:42-93` — `nonverbalRubricScorer.score(item.getNonverbalScore(), category, track, item.getResumeMode(), difficulty)` 호출 → `DimensionScore.of(score, null, null)` 4개 변환 → `questionScorePersister.saveNonverbal`. observation/evidence NULL 의도 명시. `[정상 skip] payloadNull` / `[결함 skip] scoreEmpty` 로그 분기 (plan-472 도입).
- `backend/src/main/java/com/rehearse/api/domain/feedback/rubric/service/NonverbalRubricScorer.java` — weights resolver. composure 만 weight 영향. 본 epic 폐기 대상 (orphan 확인 필요: main 호출처 = `NonverbalScorePersister:25` 단독).
- `backend/src/main/java/com/rehearse/api/domain/feedback/dto/SaveFeedbackRequest.java:34-82` — `TimestampFeedbackItem.nonverbalScore: NonverbalScore`. `NonverbalScore = {Integer fluency, Integer confidenceTone (@JsonProperty "confidence_tone"), Integer eyeContactPosture (@JsonProperty "eye_contact_posture"), Integer composure, Map<String,Object> rawSignals}` int 4개 + rawSignals. observation/evidence 필드 부재. `CommentBlock {positive, negative, suggestion}` inner class. `nonverbalComment / overallComment / vocalComment / attitudeComment` 모두 `CommentBlock`.
- `backend/src/main/java/com/rehearse/api/domain/feedback/entity/TimestampFeedback.java:48-49, 51-52, 69-73, 75-102` — `nonverbalComment / overallComment / vocalComment / attitudeComment` 모두 `@Column(columnDefinition = "TEXT")`. `@Builder` 생성자 시그니처 4종 모두 받음 = drop 시 동시 정리 필요.

#### 비언어 차원 정의 (rubric YAML)

- `backend/src/main/resources/rubric/_dimensions.yaml:251-321` — fluency / confidence_tone / eye_contact_posture 세 차원 정의. 측정 기준 = 결정론 매퍼 input (filler count / tone label / gazeOnCameraRatio / postureUnstableCount). 본 epic = LLM 채점 전환 + fluency 정의 확장 (떨림·끊김 흡수) + eye_contact_posture 정의 확장 (표정 흡수).
- `backend/src/main/resources/rubric/_dimensions.yaml:323-345` — composure 정의. `measurement: fluency/confidence_tone/eye_contact_posture previous-turn comparison when difficulty >= medium`. 본 epic 폐기 대상 (정의 자체 제거).
- `backend/src/main/resources/rubric/nonverbal-rubric.yaml:1-40` — `nonverbal-v1` rubric. `uses_dimensions` 4개 + `per_turn_rules.default = [fluency, confidence_tone, eye_contact_posture]` / `medium_or_hard = [..., composure]`. `level_expectations` 도 composure 포함. 본 epic = `composure` 참조 모두 제거 + `uses_dimensions` 3개 (각 weight 0.333... 또는 사용자 결정 weight) + `per_turn_rules` 단일 (난이도 분기 폐기).

#### FE 결과 화면

- `frontend/src/components/feedback/feedback-panel.tsx:34, 46-55, 145-176` — `FeedbackTab = 'content' | 'delivery'`. `isDeliveryAvailable = delivery.nonverbal !== null || vocal !== null || attitudeComment !== null`. `Tabs` 2-tab 구조 (content + delivery). 본 epic = delivery 탭 폐지 + content 단일.
- `frontend/src/components/feedback/content-tab.tsx:1-119` — `ContentTab` props `{ technicalFeedback: TechnicalFeedback | null, questionType: string | null }`. `isCardable` 분기 = `rubricCategory ∈ {TECHNICAL, EXPERIENCE, BEHAVIORAL}`. dimensions map → score + observation + evidenceQuote 카드 렌더 (line 86-112). 본 epic = nonverbal rubric 카드 그룹 추가 (동일 카드 컴포넌트 재사용).
- `frontend/src/components/feedback/delivery-tab.tsx:1-170` — `DeliveryTab` props `{ delivery: DeliveryFeedback | null }`. nonverbal section (line 72-106) = LevelBadge 시선/자세/표정 + StructuredComment + 원시 측정치. vocal section (line 108-156) = LevelBadge 속도/자신감/감정 + filler badges + StructuredComment. attitudeComment section (line 55-70). 본 epic = **파일 전체 삭제**.
- `frontend/src/types/interview.ts:154, 166-198` — `DeliveryFeedback.nonverbal: NonverbalFeedback | null` (line 167) + `vocal` + `attitudeComment`. `TechnicalDimensionFeedback {dimension, score, observation, evidenceQuote}` (line 175-176). `TimestampFeedback.delivery: DeliveryFeedback | null` (line 197) + `technicalFeedback: TechnicalFeedback | null` (line 198).

#### Response DTO

- `backend/src/main/java/com/rehearse/api/domain/feedback/dto/TimestampFeedbackResponse.java:39-91` — `CommentBlock` + `DeliveryFeedback {nonverbal: NonverbalFeedback, vocal: VocalFeedback, attitudeComment: CommentBlock}` + `NonverbalFeedback` + `VocalFeedback` + `TechnicalFeedback {rubricCategory, rubricId, dimensions: List<TechnicalDimensionFeedback>}` + `TechnicalDimensionFeedback {dimension, score, observation, evidenceQuote}`. `from(TimestampFeedback)` 정적 팩토리.

### DB 직접 조회 (dev rehearse-db, 2026-05-16)

- 이력서 루브릭 채점 30건: 코칭 문장 한국어=0 / 영문=30 (100% ASCII).
- 답변 발췌 반복 인용 사례: `"팀에서 했어요 수준"` (루브릭 L1 정의문 / `_dimensions.yaml:130-141` 정합), `"리허설 프로젝트에 대해 좀 더 구체적으로..."` (질문 본문).
- 비언어 채점 30건: observation / evidence 30/30 NULL (코드상 의도 = `DimensionScore.of(score, null, null)`).
- `timestamp_feedback.{nonverbalComment, vocalComment, attitudeComment, overallComment}` 4종 TEXT 컬럼 = 정상 풍부 한국어 JSON 적재 (= 폐기 대상 데이터).
- 1건 (id 30) verbal evidence empty — `#472` retry 실패 잔존, 본 epic 비스코프.

### 사용자 발화 (특정 결정 근거)

- "면접 유형에 따라 매핑되는 루브릭은 다르다. 매핑되면 그 루브릭에 따라 피드백을 생성한다" — 단일 패턴 본질.
- "기존에 생성하던 비언어 코멘트를 제거하고, 루브릭 기준 바탕으로 피드백을 남겨주면 좋겠거든" — 자유서술 4종 폐기 + rubric 단일화.
- "evidence quote도 이상해. 팀에서 했어요 수준, redis 썼어요, 이것만 엄청 반복되고 있잖" — verbal evidence verbatim 강제.
- "어 루브릭 배고 싹다 제거해" — 자유서술 / 원시 측정치 / 보조 매퍼 모두 폐기 명시.
- "유창성에서 떨림 끊김 같이 평가 안해? 표정 평가안하나? 압박 이런거 없어도 될것같은데 따로 분리하자머" — composure 폐기 + fluency 정의 확장 (떨림·끊김 흡수) + eye_contact_posture 정의 확장 (표정 흡수).
- "Lambda = 두 분석기 결과 따로 BE 전송 → BE 가 조립" — 음성 분석기 / 영상 분석기 각자 자기 영역 차원 채점 산출, BE 가 조립.
- "추가로 변경된 데이터 모델에 따른 프론트엔드 디자인 변경 계획도 추가해줘" — FE 표시 통일 (DeliveryTab 폐지).

### 추정 / 미확인 가정

- **G1 (Lambda 책임 분리 입력 충분성)**: gemini_analyzer (audio) 가 fluency + confidence_tone 채점에 필요한 신호 (transcript / filler / 어미 / 속도·리듬 / 감정 누설) 보유. vision_analyzer (frames) 가 eye_contact_posture 채점에 필요한 신호 (postureLevel / expressionLabel / eyeContactLevel / gazeOnCameraRatio / postureUnstableCount) 보유. → 응답 스키마만 확장 (LLM 추가 호출 0).
- **G2 (응답 토큰 영향)**: gemini 응답 = vocal/attitude/overall_delivery 자유서술 3섹션 삭제 + 차원 2개 (fluency / confidence_tone) 추가. 토큰 순증 추정 -30%~+10% (자유서술 다수 문장 vs 차원 2개 short text). vision 응답 = positive/negative/suggestion 자유서술 삭제 + 차원 1개 (eye_contact_posture) 추가. 추정 ±0%. Phase 2a 구현 시 실측 후 max_tokens 재조정 필요.
- **G3 (evidence_quote source 차이)**: verbal scorer / Gemini audio = transcript substring (사용자 발화). vision = transcript substring (영상 신호를 사용자 발화에 연결) 또는 발화 시간 구간 표기. **본 spec 결정: 모든 비언어 차원 evidence_quote = transcript substring 강제** (영상 신호도 발화 흐름과 연결되는 발췌로 표현 — product-spec 본질 one-liner "사용자 답변 발췌" 정합). vision_analyzer 에 transcript 입력 추가 필요.
- **G4 (Lambda → BE 페이로드 구조)**: 두 분석기 응답을 Lambda handler 가 **하나의 페이로드 안에 두 영역으로 분리 적재** 후 BE 1회 callback. Lambda → BE 호출 2회 분리 (음성 PUT + 영상 PUT) 는 폐기 (트랜잭션 / 이벤트 정합 복잡 ↑). BE 는 두 영역을 받아 조립.
- **G5 (재시도 사유 영속)**: 사용자 결정 = "운영 로그 + 메트릭". DB audit 컬럼 신설 X. 단순화 우선.
- **G6 (FE Empty 처리)**: Lambda 응답에서 음성/영상 한 쪽 dimension 누락 = `nonverbalFeedback.dimensions` 부분 array. FE 는 차원 카드 표시 + 누락 영역 "이번 답변 비언어 분석 일부 실패" 1줄 안내. 전체 실패 = `nonverbalFeedback = null` + Empty 카드.
- **G7 (백필)**: product-spec 비스코프 A — 과거 데이터 (영어 코칭 / anchor 인용 / 자유서술 4종 / composure row / rubric_id="nonverbal") 모두 backfill 안 함. 신규 인터뷰부터 적용.

---

## Trade-offs

### TO-1. Lambda → BE 조립 책임 위치 — Lambda 내부 단일 Map 사전 조립 vs BE 가 영역 키별로 단순 row insert

#### Option A (채택, **사용자 명시 결정 — 2026-05-16**) — Lambda 가 두 분석기 응답을 **단일 BE callback 페이로드 안에 음성/영상 영역 키로 분리** (`nonverbalScore.vocal.dimensions[]` + `nonverbalScore.vision.dimensions[]`). BE 가 두 영역 키 dimension 을 그대로 `question_score_dimension` 차원 row 로 insert (조립 = 단순 row 적재 = trivial)
- 장점: Lambda = 두 분석기 산출을 영역 키만 부여해 페이로드에 동봉 (사전 Map 합치기 조립 로직 X). BE = 영역 키 순회 → dimension row insert 만. 책임 명확 (Lambda = 분석 결과 운반, BE = 적재). 한 쪽 영역 실패 = 해당 영역 키 omit 또는 `null` → BE 는 받은 영역만 적재 + `isNonverbalCompleted=true` 유지 (FE 폴링 종료 메커니즘 그대로). HTTP callback 1회 흐름 그대로 (트랜잭션 / 이벤트 / 멱등 정합 변화 0).
- 단점: BE persister 가 dimension key 의미를 알아야 적재 (`fluency` / `confidence_tone` = vocal, `eye_contact_posture` = vision). 이 매핑은 rubric YAML (`uses_dimensions` 3개) + 영역 키 자체로 명시 → BE 결합도는 dimension key set 까지로 제한.
- 채택 사유: **사용자 발화 "현재 question_score, question_score_dimension이 각각의 차원마다 하나의 raw로 더해지고 있는 그냥 데이터 쌓으면 되는거아니야?" (2026-05-16) — 조립 = trivial row insert 명시.** Lambda 안에서 dimensions Map 사전 합치기는 불필요한 책임. 사용자 발화 "Lambda = 두 분석기 결과 따로 BE 전송 → BE 가 조립" 의 "조립" = BE 단의 row 적재 책임 해석으로 확정. 이전 작성안 (Lambda 내부 단일 `dimensions` Map 사전 조립) **폐기**.

#### Option B (폐기, 이전 작성안) — Lambda 가 dimensions Map 을 사전 조립 (`nonverbalScore.dimensions.{fluency, confidence_tone, eye_contact_posture}` 단일 Map). BE 는 영역 인지 불필요
- 장점: BE 는 dimension key 만 보고 적재 (영역 인지 불필요).
- 단점: Lambda 가 두 분석기 산출을 합치는 사전 조립 책임 보유. 음성/영상 어느 분석기 산물인지 추적 손실 (운영 로그·디버깅에서 분석기 식별 어려움).
- 폐기 사유: 사용자 명시 결정 (Option A) 위배.

#### Option C (폐기) — Lambda 가 음성/영상 각각 별도 callback (`POST /.../feedback/voice` + `POST /.../feedback/vision`)
- 장점: 분석기 책임 = endpoint 1:1 매핑. 한 쪽 실패 = 다른 쪽 callback 만 도달.
- 단점: BE 가 두 callback 도달 순서 / 멱등 / merge 책임. `QuestionSetFeedback` aggregate 트랜잭션 분리 = listener / aggregate 일관성 부담 ↑. Phase 2a 변경 면적 ↑.
- 폐기 사유: 정합 비용 > 책임 명확성 이득. 사용자 발화도 "조립" 책임의 위치까지 명시 안 함 + 단일 callback 흐름 유지가 단순.

#### Option D (폐기) — Lambda 1 분석기 (gemini + vision 통합 LLM 호출)
- 장점: 응답 1개. 조립 책임 0.
- 단점: 비언어 음성 + 영상 단일 호출 = 입력 modality 혼재 + prompt 비대화 + 책임 단일성 위배 (사용자 명시 "두 분석기 분리" 정면 거부).
- 폐기 사유: 사용자 명시 결정 위배.

### TO-2. 검증 실패 재시도 정책 — 1회 vs 무재시도 vs 다회

#### Option A (채택) — dimension 단위 retry 1회 + 재실패 시 해당 dimension omit + 운영 로그 + 메트릭
- 장점: 평소 호출 비용 +0 (위배 시만 +1 round). 부분 실패 = 차원 단위 fault isolation (재채택은 위배 dimension 한정). 운영 메트릭 (`rubric_retry_failed_total{stage,dimension,field}`) 로 위배율 측정 → 임계 초과 시 prompt 추가 튜닝 결정 가능.
- 단점: 재시도 latency +1 round. 위배 1건만 발생해도 retry hint 만 추가해 전체 prompt 를 한 번 더 호출 → **전체 dimension prompt 토큰 비용 1회 추가** (재채택은 위배 dimension 만). 재시도 후에도 실패 시 데이터 누락 → 사용자는 부분 카드만 봄 (FE Empty 카드 폴백 필요).
- 채택 사유: 사용자 결정 = "운영 로그 + 메트릭으로 충분". AC-5 (적재 누락률 0% 보장 단, 검증 실패 시 자동 회복 경로 동작) 정합. retry 0 시 위배율 직접 노출 = AC 위배 가능성 ↑. retry 다회 = 비용 폭주 위험 + 단순화 위배.

#### Option B (폐기) — 무재시도, 검증 실패 = 즉시 omit
- 장점: 호출 비용 변동 0. 코드 단순.
- 단점: prompt 단 위배 시 즉시 데이터 손실 → 사용자 노출 결함 빈도 ↑. AC-5 "검증 실패 시 자동 회복 경로 동작" 미충족.
- 폐기 사유: AC-5 정합 위배.

#### Option C (폐기) — turn 전체 재호출 다회 (3회)
- 장점: 검증 실패 회복 견고성 ↑.
- 단점: 호출 비용 turn 당 최대 4× (1차 + retry 3). dimension A 만 위배해도 전체 turn 재호출 = 비용 / latency 비효율.
- 폐기 사유: 비용 ↑. Non-Goals C (비용 최적화) 정면 위배 위험.

### TO-3. 4종 TEXT 컬럼 DROP 시점 — Phase 2 동일 PR vs Phase 3 별도 PR

#### Option A (채택) — Phase 3 별도 PR (Phase 2 안정화 후)
- 장점: Phase 2 머지 후 신규 row 가 4종 컬럼 100% NULL 인지 dev 검증 → Phase 3 DROP. 롤백 시 Phase 2 만 revert = 컬럼 잔존 → 데이터 손실 0.
- 단점: PR 1개 추가. 운영 절차 1 단계 ↑.
- 채택 사유: product-spec §승인 게이트 (Phase 2b 머지 + 안정화 → Phase 3 진입) 정면. 안전성 우선 + `.claude/rules/simplicity.md` (작은 PR).

#### Option B (폐기) — Phase 2 동일 PR 안에서 DROP
- 장점: PR 1개 적음.
- 단점: 머지 게이트 (BE + Lambda + FE + DDL) 4-way 묶음. 롤백 시 컬럼 복구 마이그레이션 필요 → 데이터 손실 위험. product-spec 게이트 위배.
- 폐기 사유: 안전성 비용 > PR 1개 절약 이득.

### TO-4. raw 측정치 운영 디버깅 trace 위치 — Lambda CloudWatch 로그만 vs BE admin endpoint 유지

#### Option A (채택, **사용자 명시 결정 — 2026-05-16**) — raw 신호 산출 자체 부재. 운영 디버깅은 dimension observation / evidenceQuote + Lambda CloudWatch (LLM 응답 원본) 로 trace
- 장점: BE entity / DTO / DB schema 단순화 (자유서술 4 + entity 영속 raw 6 = **10컬럼** Phase 3 일괄 DROP. `speedVariance` / `gazeOnCameraRatio` / `postureUnstableCount` 는 entity 영속 부재로 DDL 대상 외). FE 디자인 통일 (필러 배지 1건 외 raw 노출 없음 = 단일 패턴 본질 정합). LLM 채점 책임 단일화 → "어느 쪽이 진짜 평가인가" 사용자 혼란 0. **raw 자연어 산출 단계 부재 → analyzer prompt / 응답 / payload / DB / FE 모두에서 raw 형태 자산 0** (이전 갱신 = "Lambda 내부 채점 입력으로만 유지" 표현 → 본 갱신 = "산출 자체 부재").
- 단점: 운영 디버깅 시 raw 신호 trace 가 LLM 응답 원본 (CloudWatch JSON dump) 의존. dimension observation 이 LLM 의 raw 인지 결과를 한국어 코칭 문장 형태로 표현 → "왜 이 점수인가" 의 1차 근거. raw 수치 자체 (postureUnstableCount=N 등) 는 산출 부재이므로 운영자도 확보 불가. BE admin dashboard 부재.
- 채택 사유: 사용자 명시 결정 2026-05-16 — "내부 raw를 꼭 응답 받아야해? 그냥 dimension에 결정된 내용만 응답받으면 안되나. raw 완전삭제. 점수는 dimension 기반으로 점수 생성". 단일 패턴 = "면접 유형 → 루브릭 → 피드백" 본질 정합. raw 산출 잔존 = 보조 경로 증식 = 본 epic 폐기 대상 (적용 면 2 정합). 운영 디버깅 빈도 < 단일 패턴 가치.

#### Option B (폐기) — BE admin endpoint 유지 (별도 raw 측정치 read-only API)
- 장점: 운영자 BE 인증 한 곳에서 raw 신호 + dimension score 동시 trace.
- 단점: BE 영속 entity / DTO / DB schema 10컬럼 (자유서술 4 + entity 영속 raw 6) 유지 → 본 epic 의 적용 면 2 ("자유서술 + 원시 측정치 + 보조 매퍼 = 모두 폐기") 정면 위배. admin endpoint 자체가 보조 경로 신설 = 단일 패턴 본질 훼손.
- 폐기 사유: 사용자 명시 결정 정면 위배 + 단일 패턴 본질 훼손.

#### Option C (폐기) — Lambda 응답에 raw 유지 + BE 영속만 폐기
- 장점: Lambda 응답 페이로드에서 운영자가 raw 신호 + dimension 동시 확인.
- 단점: BE callback payload schema 가 raw 키 유지 → DTO 잔존 → schema cleanup PR 면적 ↑. Lambda 응답 자체가 운영 trace 채널이 되면 운영자가 callback payload log 를 별도 추출해야 함 — CloudWatch 로그와 trace 흐름 분리 = 운영 부담 ↑.
- 폐기 사유: 단일 패턴 본질 (적용 면 2) 정합 부족 + 운영 trace 흐름 분리.

### TO-5. Lambda LLM 호출 prompt 전환 방식 — dimension 채점 prompt 1회 호출 vs LLM 호출 2회 (raw → dimension) vs LLM raw 산출 + 결정론 매핑

#### Option A (채택, **사용자 명시 결정 — 2026-05-16**) — gemini / vision 의 1회 LLM 호출 prompt 자체를 dimension 채점 prompt 로 전환. raw 산출 단계 부재
- 장점: LLM 호출 횟수 = 1회 유지 (latency / 비용 변동 X). prompt 본문 = "transcript + audio metadata (또는 frame samples) → 차원별 (점수 1-3 + 한국어 observation + transcript substring evidenceQuote) 만 산출". raw 자연어 (말 속도 묘사 / 톤 라벨 / 시선 묘사 / 표정 라벨) 산출 지시 부재. rubric YAML 의 measurement / observable 정의가 LLM 의 **채점 가이드** 역할 (prompt 안에 rubric 가이드 인용 — 단일 출처는 `_dimensions.yaml`). **이전 갱신 = "raw 신호 LLM 내부 채점 입력으로 유지" 표현은 본 채택안과 충돌 → 본 갱신에서 전면 교체** (raw 산출 자체 부재).
- 단점: dimension observation 의 질이 raw 인지 충실도에 의존. raw 가 명시적 중간 산출물이 아니므로 dimension 미흡 시 prompt 가이드 (rubric measurement / observable) 만 손볼 수 있음. 디버깅 시 "이 dimension 점수의 1차 근거" 가 observation 문장 + Lambda CloudWatch (LLM 응답 원본) 의존.
- 채택 사유: 사용자 명시 2026-05-16 "raw 완전삭제. 점수는 dimension 기반으로 점수 생성". 본질 one-liner ("루브릭 → 피드백") 정합. raw 산출 잔존 = 보조 경로 증식 = 본 epic 폐기 대상.

#### Option B (폐기) — LLM 호출 2회 (1차 raw 산출 → 2차 raw → dimension 매핑)
- 장점: raw 인지 → dimension 채점 책임 분리. dimension 결과 미흡 시 raw 응답으로 1차 디버깅 가능.
- 단점: LLM 호출 2회 = latency / 비용 × 2. raw 산출 단계 = 본 epic 의 적용 면 2 ("원시 측정치 폐기") 우회 패턴. 본질 one-liner 단일 패턴 위배.
- 폐기 사유: 사용자 명시 ("raw 완전삭제") 정면 위배 + 비용 / latency 비효율.

#### Option C (폐기) — LLM raw 산출 + 결정론 보조 매퍼로 dimension 도출 (plan-11 패턴 재활용)
- 장점: dimension 점수 산출 결정론. 회귀 안정.
- 단점: 보조 매퍼 = 본 epic 의 적용 면 2 ("결정론 보조 매퍼 폐기") 정면 위배. 사용자 명시 "어 루브릭 배고 싹다 제거해" 충돌.
- 폐기 사유: 사용자 명시 결정 정면 위배.

---

## Architecture

### Phase 1 — verbal scorer 한국어 출력 + verbatim 강제 + DTO validation + retry 1회 (BE only)

```
[POST /turn (verbal 채점 트리거)]
   ↓
[RubricScoringEventListener @TransactionalEventListener(AFTER_COMMIT) @Async]
   ↓
[RubricScorer.score]
   ├─ rubricLoader.resolveFor(question, questionSet, interview) → Rubric
   ├─ rubric.selectDimensions(resumeMode) → List<String>
   └─ promptBuilder.build(...) → ChatRequest
         ├─ [변경] system: "당신은 한국어로 코칭하는 면접 평가자입니다.
         │       observation 은 한국어 1~2문장, evidence_quote 는
         │       사용자 답변 substring 만 인용."
         ├─ [변경] template `turn-rubric-scorer.txt`:
         │     ├─ Scoring Rules #3 강화: "evidence_quote MUST be a verbatim
         │     │     substring from <<<USER_ANSWER>>>...<<<END_USER_ANSWER>>>.
         │     │     Quoting the question, rubric definition, or any other
         │     │     text is FORBIDDEN."
         │     ├─ Scoring Rules #4 신설: "observation MUST be in Korean
         │     │     (한국어 1~2문장)."
         │     ├─ Output Schema 예시 (line 43-53) 한국어로 교체
         │     └─ Scoring Example (line 56-72) 한국어 유지
         └─ adapter.adapt(aiClient, request, rubric, dimensionsToScore) → RubricScoringResult
              │     ↓
              │  [신규] dimension 단위 응답 검증 (RubricScoringAdapter 내부,
              │         정책 = `RubricScorerResponseValidator` 단일 클래스):
              │     ├─ score ∈ {1, 2, 3}
              │     ├─ observation @NotBlank
              │     │   + 한국어 음절 정규식 `[\\uAC00-\\uD7A3]` 1+ 매치
              │     │     (자모 단독 ㄱ-ㅎ / ㅏ-ㅣ 불포함. 영문/숫자/코드 토큰
              │     │     은 동반 허용 — 한국어 음절 1+ 존재만 강제)
              │     └─ evidence_quote @NotBlank
              │         + userAnswer.contains(evidence_quote)
              │           (양측 whitespace 정규화 (`\\s+` → 단일 공백) 후 substring 검사)
              │           ↓
              │     ┌─ 통과 → DimensionScore 적재
              │     └─ 위배 → [retry 1회, 위배 dimension 한정 — 재채택만]
              │           ├─ retry hint 는 위배 dimension 만 명시. 어댑터는
              │           │   전체 prompt 를 한 번 더 호출하여 전체 응답을
              │           │   받되, 1차 통과 dimension 은 1차 값을 유지하고
              │           │   위배 dimension 만 retry 응답으로 재채택.
              │           │   (사용자 메시지 추가: "이전 응답이 검증 규칙 위배:
              │           │   {field}={실패사유}. 한국어 1+ 음절 / 답변 substring
              │           │   강제 룰을 재준수하여 재작성하세요.")
              │           │   → 전체 dimension prompt 토큰 비용 1회 추가
              │           ├─ 통과 → 정상 적재
              │           └─ 재실패 → log.warn("[결함 skip] retry_failed
              │                       stage=verbal interviewId={} questionId={}
              │                       dimension={} field={} reason={}",...)
              │                       (로그 본문에 evidence_quote / observation
              │                        본문 포함 금지 — A09 정합)
              │                       + Micrometer counter
              │                         rubric_retry_failed_total{stage=verbal,dimension,field} ++
              │                       + 해당 dimension row 미적재
              ↓
       [QuestionScorePersister.saveRubric(@Transactional)]
         → question_score_dimension (rubric_id=resume-v1 / behavioral-v1 / technical-v1,
                                     observation 한국어 NOT NULL, evidence verbatim NOT NULL)
```

변경 포인트 (Phase 1):
- `backend/src/main/java/com/rehearse/api/infra/ai/prompt/RubricScorerPromptBuilder.java:94-98` — `buildSystemPrompt()` 한국어 교체.
- `backend/src/main/resources/prompts/template/turn-rubric-scorer.txt:6-72` — 4 변경 (Rules #3 강화 / #4 신설 / Schema 예시 한국어 / 영어 예시 제거).
- `backend/src/main/java/com/rehearse/api/infra/ai/adapter/RubricScoringAdapter.java` — dimension 단위 검증 + retry 1회 (보강 prompt).
- **`backend/src/main/java/com/rehearse/api/infra/ai/adapter/RubricScorerResponseValidator.java` (신규)** — 정책 단일화. score / observation 한국어 음절 / evidence_quote substring 검증 + 위배 사유 enum (`InvalidScore`, `MissingObservation`, `NonKoreanObservation`, `MissingEvidence`, `EvidenceNotInUserAnswer`) 반환. **Phase 1 verbal + Phase 2a 비언어 LLM 호출 시 본 클래스 재사용 (BE 단 단일 출처)**. 위치 = `infra/ai/adapter/` (도메인 무관 정책).
  - **Lambda 대칭 (P1-D 정합)**: `lambda/analysis/analyzers/dimension_validator.py` (신규, 1 모듈) — gemini / vision 분석기 공유. 동일 정책 (한국어 음절 / transcript substring / score 1-3).
  - **정책 동기화 절차 (P1-D)**: `RubricScorerResponseValidator.java` + `dimension_validator.py` = **동일 PR 변경 강제** (review checklist 항목: "validator 정책 변경 시 BE / Lambda 양쪽 동시 갱신 여부 확인"). 정책 명세 단일 출처 = `_dimensions.yaml` 의 `score` 정의 (1-3) + 본 tech-spec 의 transcript substring / 한국어 음절 룰. 정책 drift 방지 = BE / Lambda validator 단위 테스트 동일 케이스 fixture 공유 (한국어 음절 매치 / 자모 단독 / evidenceQuote substring 매치 / whitespace 정규화 = 동일 입출력 표).
- **rubric YAML 단일 출처 (P1)**: Lambda 가 prompt 안에 dimension description / level expectations 인용할 필요 시 = **BE `_dimensions.yaml` + `nonverbal-rubric.yaml` 을 Lambda build 시점에 copy** (deploy 스크립트에서 `cp backend/src/main/resources/rubric/*.yaml lambda/analysis/rubric/`). 본 spec 범위 명시 — 별도 단일 출처 plan 분기 X (Phase 2a 묶음 PR 안에서 deploy 스크립트 갱신). Lambda 가 yaml 을 직접 인용하지 않고 prompt template 안에 인라인하는 방식 (현 `nonverbal_rubric_mapper.py` 잔존 시) 은 폐기.
- Micrometer counter 추가 (`global/config/` 기존 메트릭 설정 따름). DB audit 컬럼 신설 X (TO 외 결정 — 단순화).
- **보안 (A09 정합, P1)** — `RubricScoringAdapter` / `NonverbalScorePersister` / Lambda analyzer 모든 위배 로그에서 `evidence_quote` / `observation` 본문 미포함. 로그 컨텍스트 = `interviewId / questionId / stage / dimension / field / reason` 만. `security.md` A09 강제.

### Phase 2a — Lambda 응답 스키마 + BE 적재 (Lambda + BE 묶음 PR)

#### Lambda 응답 스키마 변경

```
[Lambda handler._run_gemini_pipeline (and _run_legacy_pipeline)]
   │
   ├─ gemini_analyzer.analyze_answer_audio(...) (Gemini, audio)
   │     ├─ 응답 스키마 변경 (★ raw 산출 단계 자체 부재 — 사용자 결정 2026-05-16
   │     │                  "raw 완전삭제. 점수는 dimension 기반으로 점수 생성"):
   │     │   삭제 (자유서술):
   │     │     vocal.{positive,negative,suggestion},
   │     │     attitude.{positive,negative,suggestion},
   │     │     overall_delivery.{positive,negative,suggestion}
   │     │   삭제 (vocal raw 측정치 — 산출 단계 자체 폐기, prompt 본문에 raw 자연어
   │     │         산출 지시 부재):
   │     │     vocal.speechPace, vocal.toneConfidenceLevel,
   │     │     vocal.emotionLabel, vocal.speedVariance
   │     │   유지: transcript, vocal.fillerWords, vocal.fillerWordCount
   │     │         (★ 필러만 예외 — 사용자 명시 "필러는 좋은 것 같아. 필러 감지하는거랑
   │     │           필러 카운트는 유지해줘. 필러만." gemini_analyzer 가 dimension 산출과
   │     │           별도로 필러 횟수 산출 + 응답 키 잔존. FE 배지용)
   │     │   신설: nonverbalDimensions: {
   │     │           fluency:         {score, observation, evidence_quote},
   │     │           confidence_tone: {score, observation, evidence_quote}
   │     │         }
   │     │   ※ prompt 본문 = "transcript + audio metadata → fluency / confidence_tone
   │     │     차원 채점 (점수 1-3 + 한국어 observation + transcript substring
   │     │     evidenceQuote) 만 산출. raw 자연어 (말 속도 묘사 / 톤 라벨 / 더듬 횟수 /
   │     │     감정 라벨) 산출 X." LLM 호출 횟수 = 1회 유지 (기존과 동일).
   │     │   ※ rubric YAML (`_dimensions.yaml`) 의 fluency / confidence_tone
   │     │     measurement / observable 정의 = LLM 의 **채점 가이드** 역할
   │     │     (prompt 안에 rubric 가이드 인용). 단일 출처 = `_dimensions.yaml`.
   │     ├─ [신규] dimension 단위 검증 (Phase 1 동일 정책):
   │     │      observation 한국어 / evidence_quote ∈ transcript / score 1~3
   │     │      위배 → retry 1회 → 재실패 시 dimension omit
   │     └─ 로그 `[결함 skip] retry_failed stage=gemini-audio dimension=X field=Y`
   │
   ├─ vision_analyzer.analyze_frames(frame_paths, transcript=...)  ★ transcript 입력 추가
   │     ├─ 응답 스키마 변경 (★ raw 산출 단계 자체 부재 — 사용자 결정 2026-05-16):
   │     │   삭제 (자유서술): positive, negative, suggestion
   │     │   삭제 (vision raw 측정치 — 산출 단계 자체 폐기, prompt 본문에 raw 자연어
   │     │         산출 지시 부재):
   │     │     eyeContactLevel, postureLevel, expressionLabel,
   │     │     gazeOnCameraRatio, postureUnstableCount
   │     │   신설: nonverbalDimensions: {
   │     │           eye_contact_posture: {score, observation, evidence_quote}
   │     │         }
   │     │   ※ prompt 본문 = "frame samples + transcript → eye_contact_posture 차원
   │     │     채점 (점수 1-3 + 한국어 observation + transcript substring
   │     │     evidenceQuote) 만 산출. raw 자연어 (시선 묘사 / 자세 묘사 / 표정 라벨)
   │     │     산출 X." LLM 호출 횟수 = 1회 유지.
   │     │   ※ rubric YAML 의 eye_contact_posture measurement / observable 정의 =
   │     │     LLM 채점 가이드. evidence_quote source = transcript substring (G3 결정 —
   │     │     영상 신호도 사용자 발화 흐름에 연결되는 발췌로 표현).
   │     ├─ [신규] dimension 단위 검증 + retry 1회 (Phase 1 동일)
   │     │      evidence_quote source = transcript substring (G3 결정)
   │     └─ 로그 `[결함 skip] retry_failed stage=vision dimension=X field=Y`
   │
   ├─ handler 조립 (timestamp_feedbacks list):
   │   fb["transcript"]      = gemini.transcript
   │   fb["fillerWords"]     = gemini.vocal.fillerWords        # 유지 (필러 예외)
   │   fb["fillerWordCount"] = gemini.vocal.fillerWordCount    # 유지 (필러 예외)
   │
   │   ──── 4종 자유서술 적재 제거 (Phase 2a) ────
   │   삭제: fb["vocalComment"], fb["attitudeComment"],
   │         fb["nonverbalComment"], fb["overallComment"]
   │   삭제: import nonverbal_rubric_mapper, _build_nonverbal_score(...)
   │
   │   ──── raw 측정치 적재 제거 (Phase 2a — ★ 사용자 결정 2026-05-16
   │        "raw 완전삭제. 점수는 dimension 기반으로 점수 생성") ────
   │   삭제 (vocal raw): fb["speechPace"], fb["toneConfidenceLevel"],
   │                      fb["emotionLabel"], fb["speedVariance"]
   │   삭제 (vision raw): fb["eyeContactLevel"], fb["postureLevel"],
   │                       fb["expressionLabel"], fb["gazeOnCameraRatio"],
   │                       fb["postureUnstableCount"]
   │   ※ analyzer LLM 호출 prompt 가 raw 자연어 산출 단계 자체를 제거하므로
   │     handler 조립부에 raw 적재 line 도 동시 부재. fillerWords / fillerWordCount
   │     만 raw 신호 중 예외 유지.
   │   ※ 부재 키 처리 (P0-C): 위 9개 raw 키 중 일부는 현 `handler.py` 에 `fb["..."]`
   │     적재 line 자체가 존재하지 않을 수 있음 (analyzer 응답만 보유, handler
   │     조립부 미반영). **구현 진입 직전 `grep -n 'fb\["' lambda/analysis/handler.py`
   │     로 실제 적재 line 부재 키를 식별 후, 부재 키는 삭제 대상에서 제거** (이미
   │     없는 line 을 삭제하려 하면 빈 변경). 본 spec 의 9개 키 목록은 entity /
   │     prompt 기준 완전 집합 — handler 조립부 실제 적재는 부분집합 가능.
   │
   │   ──── 비언어 영역 키 분리 페이로드 (Lambda 사전 Map 합치기 X) ────
   │   # TO-1 채택안: Lambda = 음성/영상 영역 키만 부여해 분석기 산출을
   │   #             그대로 동봉. dimensions Map 사전 조립 책임 부재.
   │   nonverbalScore = {}
   │   if gemini.nonverbalDimensions:
   │       nonverbalScore["vocal"] = {
   │           "dimensions": [
   │               # 각 원소 = {dimension_ref, score, observation, evidence_quote}
   │               {"dimension_ref": "fluency",
   │                **gemini.nonverbalDimensions.fluency},
   │               {"dimension_ref": "confidence_tone",
   │                **gemini.nonverbalDimensions.confidence_tone},
   │           ]
   │       }
   │   if vision.nonverbalDimensions:
   │       nonverbalScore["vision"] = {
   │           "dimensions": [
   │               {"dimension_ref": "eye_contact_posture",
   │                **vision.nonverbalDimensions.eye_contact_posture},
   │           ]
   │       }
   │
   │   fb["nonverbalScore"] = nonverbalScore if nonverbalScore else None
   │   # 둘 다 부재 = None (전체 실패)
   │   # 한 분석기만 산출 = 해당 영역 키만 존재 (부분 실패)
   │   # BE 가 받은 영역 키 순회하여 row insert (조립 = trivial)
   │
   └─ api_client.save_feedback(timestamp_feedbacks, isVerbalCompleted=True, isNonverbalCompleted=True)
        # 분석기 부분 실패에도 isNonverbalCompleted=true (FE 폴링 무한 로딩 차단)
```

#### BE 수신 + 적재

```
[POST /api/internal/interviews/{id}/question-sets/{qsId}/feedback]
   ↓
[FeedbackController → FeedbackService.saveFeedback @Transactional]
   ├─ feedback / timestampFeedback row insert
   │     - nonverbal_comment / overall_comment / vocal_comment / attitude_comment
   │       4개 컬럼: Phase 2a 에서 NULL 만 입력
   │       (entity 필드 Phase 2a 잔존 — Phase 3 에서 DROP)
   └─ event publish (FeedbackSavedEvent) AFTER_COMMIT
         ↓
   [Async listener] → NonverbalScorePersister.persistAll(...)
         └─ persistOne(...) ★ 재작성 (TO-1 영역 키 분리 페이로드 수신)
               ├─ payload = item.getNonverbalScore()   // 신규: {vocal?, vision?}
               ├─ 분기:
               │   (a) payload == null
               │       → log.info("[정상 skip] payloadNull
               │                  interviewId={}, questionId={}", ...) return
               │   (b) payload.vocal == null && payload.vision == null
               │       → log.warn("[결함 skip] areasEmpty ...") return
               │   (c) 두 영역 키 dimension 을 단일 Map 으로 머지 + 유효성 필터:
               │       Map<String, DimensionScore> dims = new LinkedHashMap<>();
               │       Stream.of(payload.getVocal(), payload.getVision())
               │             .filter(Objects::nonNull)
               │             .flatMap(area -> area.getDimensions().stream())
               │             .filter(d -> d.getScore() != null
               │                          && d.getObservation() != null
               │                          && d.getEvidenceQuote() != null)
               │             .forEach(d -> dims.put(d.getDimensionRef(),
               │                            new DimensionScore(d.getScore(),
               │                                               d.getObservation(),
               │                                               d.getEvidenceQuote())));
               │   (d) dims 비었으면 [결함 skip] allInvalid 로그 return
               │
               │   ※ rubric_id 주입 = 호출자 (NonverbalScorePersister) 책임.
               │     QuestionScorePersister 하드코딩 "nonverbal" → 호출자에서
               │     "nonverbal-v1" 전달 (P0-2: QuestionScorePersister 시그니처
               │     변경 → rubricId 인자 추가, saveNonverbal 메서드 폐기).
               └─ questionScorePersister.saveRubric(
                       question.getId(), interview.getId(),
                       "nonverbal-v1",  // rubric_id 명시 주입
                       null,            // levelFlag (nonverbal 미사용)
                       dims
                  )
                  → DB question_score.rubric_id="nonverbal-v1"
                    question_score_dimension (observation 한국어 /
                                              evidence transcript substring NOT NULL)
```

변경 포인트 (Phase 2a — Lambda):
- `lambda/analysis/handler.py:23-25, 30-41, 268-321, 326-444` — `NonverbalRubricMapper` import / `_build_nonverbal_score` / 4종 자유서술 적재 / `nonverbalScore` 결정론 매퍼 호출 모두 제거. `dims` 조립으로 교체. `_run_legacy_pipeline` 의 dimension 산출 = `nonverbalScore=null` 반환 (Empty 카드) — 결정 사유: Whisper + GPT-4o Vision 조합으로 fluency 정확도 보장 곤란 + legacy 사용 빈도 < 비용 (legacy 전용 dimension prompt 작성 + 회귀 보호). gemini pipeline 만 dimension 산출 + legacy 는 verbal scorer 만 정상 동작 + FE Empty 카드 graceful (P1-B 결정).
- `lambda/analysis/analyzers/gemini_analyzer.py` — `_FALLBACK_ANSWER` 갱신 (자유서술 제거 + `nonverbalDimensions` 신설). `_ANSWER_SYSTEM_TEMPLATE` 갱신 = **dimension 채점 prompt 로 전면 전환** (자유서술 4개 섹션 + raw 자연어 산출 지시 모두 삭제 → transcript + filler 산출 + nonverbalDimensions 2차원). prompt 본문에 raw 자연어 (말 속도 묘사 / 톤 라벨 / 더듬 횟수 / 감정 라벨) 산출 지시 부재 검증 (prompt 단위 테스트로 보호). dimension 단위 validation + retry 1회.
- `lambda/analysis/analyzers/vision_analyzer.py` — `analyze_frames(frame_paths)` 시그니처 변경 `analyze_frames(frame_paths, transcript)`. `_SYSTEM_PROMPT` 갱신 = **dimension 채점 prompt 로 전면 전환** (자유서술 + raw 자연어 산출 지시 삭제 + `nonverbalDimensions.eye_contact_posture` 신설, "표정·시선·자세" 신호 흡수 가이드 추가). prompt 본문에 raw 자연어 (시선 묘사 / 자세 묘사 / 표정 라벨) 산출 지시 부재 검증. evidence_quote = transcript substring 룰. validation + retry 1회.
- `lambda/analysis/analyzers/nonverbal_rubric_mapper.py` — **파일 삭제**. `lambda/analysis/tests/test_nonverbal_rubric_mapper.py` (존재 시) 삭제.
- `lambda/analysis/tests/test_vision_analyzer.py` / `test_gemini_analyzer.py` / `test_handler.py` — 신규 응답 스키마 / retry / omit 시나리오 테스트 추가. **prompt 단위 테스트 신규**: prompt 본문에 "dimension 채점 가이드" 인용 (rubric measurement 키워드) 존재 + raw 산출 지시 키워드 ("speechPace" / "toneConfidenceLevel" / "emotionLabel" / "eyeContactLevel" / "postureLevel" / "expressionLabel" / "gazeOnCameraRatio" / "postureUnstableCount" / "speedVariance") 부재 assert.
- **max_tokens 실측 timing (P1-C)**: 구현 진입 직전 dev 환경에서 gemini / vision 각 1회 dry-run 호출 (응답 token 실측) → max_tokens 한도가 신규 응답 길이 (자유서술 3섹션 + raw 9개 삭제 + dimension 2-3개 + observation/evidenceQuote 추가) 를 수용하는지 사전 검증 → 부족 시 한도 조정 후 PR 작성. 위험 항목 (Phase 2a Lambda 응답 토큰 영향) 와 동일 절차.

변경 포인트 (Phase 2a — BE):

- **`backend/src/main/java/com/rehearse/api/domain/feedback/dto/SaveFeedbackRequest.java:65-73` — `NonverbalScore` inner class 재정의 (TO-1 영역 키 분리)**:
  ```java
  @Getter @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class NonverbalScore {
      @Valid private AreaScore vocal;   // gemini_analyzer 산출
      @Valid private AreaScore vision;  // vision_analyzer 산출
  }

  @Getter @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class AreaScore {
      @Valid private List<DimensionScoreItem> dimensions;
  }

  @Getter @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class DimensionScoreItem {
      @JsonProperty("dimension_ref")
      @NotBlank private String dimensionRef;
      @NotNull @Min(1) @Max(3) private Integer score;
      @NotBlank private String observation;
      @JsonProperty("evidence_quote")
      @NotBlank private String evidenceQuote;
  }
  ```
  기존 `fluency / confidenceTone / eyeContactPosture / composure / rawSignals` 필드 제거. P1 (Bean Validation 깊이) 반영 — record + 필드 어노테이션.
- `backend/src/main/java/com/rehearse/api/domain/feedback/dto/SaveFeedbackRequest.java:40-43` — `CommentBlock nonverbalComment / overallComment / vocalComment / attitudeComment` 4개 필드 잔존 (Phase 2a). Lambda 는 이 4개 키를 페이로드에서 omit 시작 → BE `@JsonIgnoreProperties(ignoreUnknown=true)` 영향 0. Phase 3 에서 필드 제거.

- **`backend/src/main/java/com/rehearse/api/domain/feedback/rubric/service/NonverbalScorePersister.java:42-93` — `persistOne` 재작성 (TO-1 + P0-2 rubric_id 주입)**:
  - `NonverbalRubricScorer.score(...)` 호출 제거.
  - `payload.vocal` + `payload.vision` 두 영역 키 dimension 을 머지 + 유효성 필터 (`score / observation / evidence_quote` NOT NULL).
  - `questionScorePersister.saveRubric(questionId, interviewId, "nonverbal-v1", null, dims)` 명시 호출 (rubric_id 인자 주입).
  - `resolveTrack` 폐기 (track 컨텍스트 = Lambda 책임).

- **`backend/src/main/java/com/rehearse/api/domain/feedback/score/service/QuestionScorePersister.java:55-84` — P0-2 결함 해소**:
  - **현 상태**: `saveNonverbal(...)` 메서드가 `rubric_id="nonverbal"` 하드코딩 (line 58) + `findByQuestionIdAndRubricId` 중복 체크 키도 `"nonverbal"` 사용. 본 spec 에서는 `rubric_id="nonverbal-v1"` 적재 의도 (Lambda + rubric YAML 정합).
  - **조치**: `saveNonverbal(...)` 메서드 **삭제**. 호출자 (`NonverbalScorePersister`) 가 기존 `saveRubric(questionId, interviewId, rubricId, levelFlag, dims)` 를 `rubricId="nonverbal-v1"` / `levelFlag=null` 로 호출.
  - 이유: 단일 책임 분리 (hardcoded rubric_id 회피 — 호출자가 의미 주입). `saveRubric` 메서드가 이미 동일 시그니처 + idempotent 중복 체크 보유.
  - 영향 검증: `grep -rn "saveNonverbal" backend/src` 결과 = `NonverbalScorePersister` 단독 호출처 + 관련 테스트만. 호출처 동일 PR 갱신.

- **`backend/src/main/java/com/rehearse/api/domain/feedback/rubric/service/NonverbalRubricScorer.java` + `NonverbalContextWeightsLoader.java` + 의존 test 3 파일 — orphan 확인 후 삭제**. orphan 검증 = `grep -rn "NonverbalRubricScorer\\|NonverbalContextWeightsLoader" backend/src` 결과 main 호출처 단일 (`NonverbalScorePersister`) 확인 후 동일 PR 삭제.

- **`backend/src/main/resources/rubric/_dimensions.yaml:251-321` — fluency / confidence_tone / eye_contact_posture 3차원 measurement + observable 확장 (★ raw 신호 흡수 — 사용자 결정 2026-05-16)**:
  - **fluency** (`:251-273`):
    - description 확장 "필러 워드가 답변 전달을 방해하지 않는가" → "필러·말 더듬·발화 속도 변동이 답변 전달을 방해하지 않는가" (떨림·끊김 + 말빠르기 흡수).
    - measurement `verbal.filler_word_count` → "필러 횟수 + 더듬·재시작 빈도 + 발화 속도 변동" 통합 정의 (raw `speechPace` / `speedVariance` 흡수).
    - scoring observable 확장: 기존 "필러 워드 N회" 항목에 "더듬·재시작 빈도", "발화 속도 변동 (`speechPace=빠름/느림` 또는 `speedVariance ≥ 0.30`)" 신호 추가. L1/L2/L3 각각 1줄 추가.
  - **confidence_tone** (`:275-297`):
    - description 유지 "톤과 발화 속도 변동이 자신감 있게 유지되는가".
    - measurement `verbal.tone_label + verbal.speedVariance` → "톤 안정성 (단정형 어미 / 끝맺음 명확 / 음량 떨림) + 발화 속도 분산" 통합 정의 (raw `toneConfidenceLevel` / `emotionLabel` / `speedVariance` 흡수).
    - scoring observable 확장: 기존 "톤이 HESITANT" / "톤이 CONFIDENT" 같은 enum 라벨 의존 표현을 관찰 어휘 (단정형 어미 / 추측형 어미 / 음량 떨림 / 끝 흐림) 로 재표현. L1/L2/L3 각각 1줄 보강.
  - **eye_contact_posture** (`:299-321`):
    - description 확장 "시선과 자세가 안정적으로 유지되는가" → "시선·자세·표정이 안정적으로 유지되는가" (표정 흡수).
    - measurement `vision.gazeOnCameraRatio + vision.postureUnstableCount` → "카메라 응시 비율 + 자세 안정성 (흔들림·기울임 빈도) + 표정 안정성 (NERVOUS / UNCERTAIN / NEUTRAL 등)" 통합 정의 (raw `eyeContactLevel` / `postureLevel` / `expressionLabel` / `gazeOnCameraRatio` / `postureUnstableCount` 모두 흡수).
    - scoring observable 확장: 기존 "카메라 응시 비율 0.70 초과" / "자세 불안정 횟수 N회" 외에 "표정이 NERVOUS / UNCERTAIN 으로 30% 이상 유지" (L1), "표정 단서 평탄 또는 일부 긴장" (L2), "표정 ENGAGED / CONFIDENT 또는 NEUTRAL 이완" (L3) 신호 추가.
  - **composure** (`:323-345`) — **정의 자체 삭제** (사용자 명시 "압박 이런거 없어도 될것같은데").
  - 운영자 리뷰 검증 항목: 위 measurement / observable 확장 표현이 raw 신호 5종 (vision) + 4종 (vocal) 을 모두 포함하는지 작성자 + 사용자가 별도 검토 (회귀 테스트로는 "특정 substring 포함" 만 보호 — 완전성 검증은 사람 리뷰).
- `backend/src/main/resources/rubric/nonverbal-rubric.yaml:1-40` — composure 참조 모두 제거 (`uses_dimensions` 4개 → 3개, weight 재분배 1/3 각, `per_turn_rules` 단일 default 만, `medium_or_hard` block 삭제, `level_expectations.must_reach_*` 에서 composure 제거). `data_source: lambda.nonverbalScore.d11/d12/d13` 라인은 Lambda 응답 영역 키 분리 후에도 의미 동일 — `nonverbalScore.vocal.dimensions[]` / `nonverbalScore.vision.dimensions[]` 의 `dimension_ref` 키로 라우팅. 별도 변경 불요.

- **`backend/src/main/java/com/rehearse/api/domain/feedback/dto/TimestampFeedbackResponse.java` — P0-3 nonverbalFeedback 조립 메커니즘 결정**:
  - **현 구조**: `from(feedback, questionScore, dimensions)` 정적 팩토리 = entity 1개 + `QuestionScore` 1개 + `QuestionScoreDimension` 리스트 1개 입력 (단일 rubric). `QuestionSetFeedbackResponse.from` (`:33-40`) 가 `questionScoreByQuestionId` Map + `dimensionsByQuestionScoreId` Map 으로 N개 timestamp 조립.
  - **결정 (옵션 (a) — Service 단 조회 + DTO 시그니처 확장)**: nonverbal 도 동일 패턴. `QuestionSetFeedbackResponse.from` 호출 시점에 `questionScoreByQuestionId` 대신 `Map<Long, List<QuestionScore>>` (questionId → verbal/nonverbal 2개) + 동일 `dimensionsByQuestionScoreId` 전달. `TimestampFeedbackResponse.from` 시그니처 = `from(TimestampFeedback feedback, List<QuestionScore> questionScores, Map<Long, List<QuestionScoreDimension>> dimsByScoreId)` 로 확장.
  - **이유**: `conventions.md` 계층 책임 = "Response DTO 가 repository 호출 금지" → service 단 조회 + DTO 조립 입력 일괄 전달. Assembler 별도 클래스 도입 (옵션 (b)) = 단발성 코드에 인터페이스 추가 = `simplicity.md` 위배.
  - **`FeedbackService.getByInterviewId`** (또는 `QuestionSetFeedbackResponse` 빌드 진입점) 갱신: `QuestionScoreRepository.findAllByInterviewId` 결과 group by questionId → `Map<Long, List<QuestionScore>>` 구성. dimension 도 동일.
  - **`TimestampFeedbackResponse` 갱신**:
    - 신규 inner class `NonverbalFeedbackV2` (이름 충돌 회피 — 기존 `NonverbalFeedback` 는 4컬럼 read 잔존, Phase 3 삭제) = `{rubricId, dimensions: List<TechnicalDimensionFeedback>}`. `dimensions` 원소 타입 = 기존 `TechnicalDimensionFeedback` 재사용 (verbal 과 동일 형태).
    - 신규 필드 `nonverbalFeedback` 추가.
    - `from(feedback, questionScores, dimsByScoreId)` 시그니처에서 questionScores 순회 → `rubricId="nonverbal-v1"` 매치 시 nonverbalFeedback 빌드, 그 외 verbal rubric (resume-v1 / behavioral-v1 / technical-v1) 매치 시 technicalFeedback 빌드.
    - **legacy nonverbal row skip 정책 (P0-D)**: `rubricId ∉ {resume-v1, behavioral-v1, technical-v1, nonverbal-v1}` row 는 silently skip + DEBUG 로그 (`legacy rubric_id 응답 미포함 rubricId={}`). product-spec 비스코프 A 의 backfill 미수행 결정과 정합 — 기존 `rubric_id="nonverbal"` (보조 매퍼 산물) row 또는 향후 dev 누적 잔재 row 가 응답에 포함되면 FE 단일 카드 패턴 깨짐. skip + DEBUG 로그로 응답 정합 보호 + 운영자 관찰 채널 유지.
    - 결정: 응답 JSON 키 `nonverbalFeedback` (Phase 2a 신규). 기존 `NonverbalFeedback` 클래스명 충돌 → Phase 2a 에서 inner class 명을 `NonverbalRubricFeedback` 으로 (혹은 기존 `NonverbalFeedback` 의 4컬럼 read inner 를 `LegacyNonverbalFeedback` 으로 rename + 응답 JSON 키 동일 보존) — **구현 시 컴파일 단계에서 충돌 회피 방식 1개 선택. 본 spec 결정 = inner class 명 `NonverbalRubricFeedback`, 응답 JSON 키 `nonverbalFeedback`**.
  - **`QuestionSetFeedbackResponse.from`** 시그니처 갱신 (Map 타입 변경): `Map<Long, QuestionScore>` → `Map<Long, List<QuestionScore>>`. 호출자 (`FeedbackService`) 동일 PR 갱신.

### Phase 2b — FE DeliveryTab 폐지 + ContentTab 단일 흐름 (FE PR)

```
[FeedbackCard / FeedbackPanel]
   ── 변경 전 ──
   Tabs (value: 'content' | 'delivery')
     ├─ ContentTab  (technicalFeedback)
     └─ DeliveryTab (delivery)

   ── 변경 후 ──
   ContentTab  (technicalFeedback + nonverbalFeedback)
     ├─ verbal 영역 (rubric 카드 그룹)
     │   └─ RubricDimensionCard 컴포넌트 재사용
     └─ 비언어 영역 (rubric 카드 그룹, nonverbalFeedback ≠ null)
         ├─ 헤더 "비언어 평가" + rubricId
         ├─ dimensions[fluency / confidence_tone / eye_contact_posture]
         │   → RubricDimensionCard
         └─ 부분 누락 (dimensions.length < 3):
             1줄 안내 "이번 답변 비언어 분석 일부 실패"
         전체 누락 (nonverbalFeedback === null):
             Empty 카드 "분석 실패 — 점수 없음"
```

변경 포인트 (Phase 2b — FE):
- `frontend/src/types/interview.ts:154, 166-198` — `DeliveryFeedback.nonverbal: NonverbalFeedback | null` 필드 제거 (interface 자체 = vocal / attitudeComment 잔존, Phase 3 에서 전체 제거). `TimestampFeedback` 에 `nonverbalFeedback: NonverbalFeedback | null` 신설 (구조 = `TechnicalFeedback` 동일: `{rubricId, dimensions: TechnicalDimensionFeedback[]}`).
- `frontend/src/components/feedback/rubric-dimension-card.tsx` — **신규 컴포넌트**. props `{ dimension: TechnicalDimensionFeedback }` — 현 `content-tab.tsx:86-112` JSX 추출. verbal / 비언어 동일 컴포넌트 재사용. Stateless ≥ 40 줄 룰 정합.
- `frontend/src/components/feedback/content-tab.tsx:1-119` — props 확장 `{ technicalFeedback, nonverbalFeedback, questionType }`. verbal 카드 그룹 + 비언어 카드 그룹 렌더. `RubricDimensionCard` 재사용. RESUME_OPENER / isCardable Empty 분기 보존. 비언어 영역 분기:
  - `nonverbalFeedback === null` → "분석 실패 — 점수 없음" Empty 카드.
  - `nonverbalFeedback.dimensions.length < 3` → 부분 카드 + "이번 답변 비언어 분석 일부 실패" 1줄 안내.
  - 정상 3차원 → 카드 3개.
- **`frontend/src/components/feedback/feedback-panel.tsx:34, 46-55, 145-176` — Tab 제거 + 필러 카운트 배지 유지 (사용자 결정 #2)**:
  - `FeedbackTab` type 제거. `Tabs` / `TabsList` / `TabsTrigger` / `TabsContent` 제거 (단일 ContentTab 렌더). `isDeliveryAvailable` / `effectiveTab` 로직 제거. `DeliveryTab` import 제거.
  - **`feedback-panel.tsx:111-117` 습관어 N회 감지 배지 = 유지** (사용자 명시 "필러는 좋은 것 같아. 필러 감지하는거랑 필러 카운트는 유지해줘. 필러만."). 단 source 경로 변경: 기존 `feedback.delivery?.vocal?.fillerWordCount` → `feedback.fillerWordCount` (Top-level 필드 사용). 근거 = `TimestampFeedback` entity (`backend/.../TimestampFeedback.java:37 private Integer fillerWordCount`) + Response DTO (`TimestampFeedbackResponse.java:66`) 가 이미 top-level `fillerWordCount` 필드 보유. `frontend/src/types/interview.ts:159 fillerWordCount: number | null` 도 이미 존재.
  - **검증 가이드 (P1-A)**: 구현 진입 직전 `grep -n 'fillerWordCount' backend/src/main/java/com/rehearse/api/domain/feedback/dto/TimestampFeedbackResponse.java` 로 top-level 필드 노출 line 재확인. 만약 응답 DTO 의 `from(...)` 정적 팩토리에서 `fillerWordCount` 가 nested (`delivery.vocal.fillerWordCount`) 로만 노출되고 top-level 부재 시 = Phase 2a-BE 변경 범위에 top-level 노출 추가 합류 (`TimestampFeedbackResponse` 의 `from(...)` 에 `fillerWordCount` top-level 필드 빌드 line 추가). FE Phase 2b 는 BE 의 top-level 노출 확정 후 진행.
  - `delivery.vocal` 경로 의존 제거 시 같이 정리할 다른 line 없음 (highlightFillers / FILLER_WORDS 배열은 transcript 시각화용 — 독립 유지).
- **P0-4 — FE 폴링 종료 메커니즘 명시 (사용자 결정 #1 부분 실패 정합)**:
  - **폴링 종료 = `analysisStatus` 단일 출처**. `frontend/src/hooks/use-question-sets.ts:34-67` `useQuestionSetStatus` / `useAllQuestionSetStatuses` 의 `refetchInterval: enabled ? N000 : false` 는 호출자에서 `enabled` 토글로 종료. 종료 분기 = `frontend/src/pages/interview-analysis-page.tsx:225-227` `isTerminal(s) = s.analysisStatus ∈ {COMPLETED, PARTIAL, FAILED, SKIPPED}` + `allTerminal` 로직.
  - `analysisStatus` 는 BE `QuestionSetAnalysis.completeAnalysis(verbalCompleted, nonverbalCompleted)` (`backend/.../QuestionSetAnalysis.java:86-93`) 에서 두 플래그 조합으로 결정 (`true/true → COMPLETED`, `false/false → FAILED`, 한 쪽만 → `PARTIAL`).
  - **Lambda 부분 실패 (한 분석기만 산출) 시 = `isNonverbalCompleted=true` 유지** (Phase 2a Lambda handler 결정). BE `completeAnalysis` 가 `analysisStatus=COMPLETED` (또는 verbal 미완 시 PARTIAL) 로 전이 → FE 폴링 종료 → `useQuestionSetFeedback` (line 70-84) staleTime=Infinity 로 결과 캐시.
  - **Phase 2b 시점에 본 hook / page 코드 변경 0**. 폴링 종료 기준이 `analysisStatus` 이고 `delivery.*` 필드에 의존하지 않음 → P0-4 차단 사유 부재.
  - **추가 회귀 테스트 (Phase 2b)**: `interview-analysis-page.test.tsx` 또는 `use-question-sets.test.tsx` 에 "Lambda 부분 실패 (nonverbal 한 분석기만 산출) 페이로드 수신 시 폴링이 `analysisStatus=COMPLETED` 도달로 종료됨" 시나리오 추가 (msw 로 status endpoint 점진 응답 stub). content-tab 회귀 (부분 dimensions 카드 표시) 와 분리해 폴링 종료만 검증.
- `frontend/src/components/feedback/delivery-tab.tsx` — **파일 삭제**.
- `frontend/src/components/feedback/__tests__/content-tab.test.tsx` — 신규 / 갱신: 비언어 rubric 카드 노출 / 부분 누락 안내 / 전체 누락 Empty 카드 회귀 시나리오. RTL + msw.
- `frontend/src/components/feedback/__tests__/rubric-dimension-card.test.tsx` — 신규 컴포넌트 unit/integration 테스트.
- `frontend/src/components/feedback/__tests__/feedback-panel.test.tsx` — 신규 / 갱신: **필러 카운트 배지 회귀 (사용자 결정 #2)** — `fillerWordCount=3` 일 때 "습관어 3회 감지" 텍스트 노출 assert. `fillerWordCount=null` 또는 `0` 일 때 배지 부재 assert. 탭 부재 (`screen.queryByRole('tab')` = 0) assert.

### Phase 3 — 폐기 저장소 / 자산 cleanup (Flyway + 코드 + 테스트, 별도 PR)

```
[Flyway V{N}__drop_timestamp_feedback_freetext_and_raw_columns.sql]
   └─ ALTER TABLE timestamp_feedback
        -- 자유서술 4종
        DROP COLUMN nonverbal_comment,
        DROP COLUMN overall_comment,
        DROP COLUMN vocal_comment,
        DROP COLUMN attitude_comment,
        -- ★ raw 측정치 6종 (실측 entity 영속, 사용자 결정 2026-05-16)
        DROP COLUMN speech_pace,
        DROP COLUMN tone_confidence_level,
        DROP COLUMN emotion_label,
        DROP COLUMN eye_contact_level,
        DROP COLUMN posture_level,
        DROP COLUMN expression_label;
        -- 필러 (filler_word_count, filler_words) 는 예외 유지.
        -- speed_variance / gaze_on_camera_ratio / posture_unstable_count =
        -- entity 영속 부재 → DDL 대상 부재 (Lambda 응답 부재로 처리 완결).
        -- 컬럼명은 실제 entity @Column(name=...) 으로 확인 후 작성 (P0-A).

[BE TimestampFeedback entity]
   └─ 자유서술 4 필드 (nonverbalComment / overallComment / vocalComment / attitudeComment)
      + raw 6 필드 (speechPace / toneConfidenceLevel / emotionLabel
                   / eyeContactLevel / postureLevel / expressionLabel)
      + @Builder param 제거. fillerWordCount / fillerWords 만 유지.

[BE SaveFeedbackRequest.TimestampFeedbackItem]
   └─ CommentBlock 4종 필드 + raw 필드 (DTO 잔존 키 = entity 영속 6 + Lambda
      응답 부재로 비활성된 3 합집합 — 구현 진입 직전 grep 으로 실제 필드 확인 후
      모두 제거). CommentBlock inner class 도 참조 0 = 삭제.
      fillerWordCount / fillerWords 만 유지.

[BE TimestampFeedbackResponse]
   └─ DeliveryFeedback / NonverbalFeedback / VocalFeedback / CommentBlock
      inner class + raw 필드 잔존 참조 검증 후 삭제.
      from() 정적 팩토리에서 자유서술 4컬럼 + raw 6컬럼 read 코드 제거.

[FE types/interview.ts]
   └─ DeliveryFeedback / NonverbalFeedback / VocalFeedback / CommentBlock
      type 삭제. TimestampFeedback.delivery 필드 제거.
      isCommentBlockEmpty / StructuredComment / LevelBadge / format-feedback-level
      잔존 참조 grep 0 검증 후 미사용 파일 삭제.

[Lambda] 변경 0 (Phase 2a 에서 이미 raw 산출 단계 부재 + 자유서술 산출 중단)
```

선결 조건 (게이트):
- Phase 2a/2b 머지 후 dev 신규 인터뷰 ≥ 3회 (각 ≥ 5 turn) 실행하여 **10컬럼 (자유서술 4 + entity 영속 raw 6) = 100% NULL** 확인. `speed_variance` / `gaze_on_camera_ratio` / `posture_unstable_count` 는 entity 영속 부재 → 확인 대상 X.
- `grep -r "nonverbal_comment\\|nonverbalComment\\|vocal_comment\\|vocalComment\\|attitude_comment\\|attitudeComment\\|overall_comment\\|overallComment" backend/src frontend/src lambda` 결과 = 본 phase 변경 대상 외 0 매치 검증.
- `grep -r "speechPace\\|toneConfidenceLevel\\|emotionLabel\\|speedVariance\\|eyeContactLevel\\|postureLevel\\|expressionLabel\\|gazeOnCameraRatio\\|postureUnstableCount" backend/src frontend/src` 결과 = 본 phase 변경 대상 외 0 매치 검증 (Lambda 는 raw 산출 단계 폐기 후이므로 매치 0 기대 — `_dimensions.yaml` 의 measurement / observable 본문에 가이드 표현으로 잔존 가능, 그 외 코드 매치는 0).

---

## Data Model

### Phase 1 — 변경 없음
신규 column / table / index 0. retry audit DB 컬럼 신설 X (단순화 우선, TO 외 결정).

### Phase 2a — 변경 없음 (data schema)
- Lambda 응답 스키마 변경 + BE DTO 스키마 변경만. DB column 영향 0.
- `timestamp_feedback.nonverbal_comment / overall_comment / vocal_comment / attitude_comment` 4컬럼 = Phase 2a 후 신규 row 100% NULL (Lambda 가 더 이상 산출 안 함). Phase 3 DROP 전 잔존.
- **raw 측정치 entity 필드 (★ 사용자 결정 2026-05-16)**:
  - 현 `TimestampFeedback` entity 가 raw 측정치를 영속하는 컬럼 보유 시 (예: `speech_pace`, `tone_confidence_level`, `emotion_label`, `speed_variance`, `eye_contact_level`, `posture_level`, `expression_label`, `gaze_on_camera_ratio`, `posture_unstable_count`) → Lambda 가 Phase 2a 이후 omit 시작 → 신규 row 해당 컬럼 = 100% NULL.
  - **entity 필드 자체 폐기는 Phase 3 schema cleanup 합류** (Flyway DROP COLUMN). Phase 2a 시점에는 entity 잔존 + Lambda omit → DB NULL 자연 도출. `SaveFeedbackRequest.TimestampFeedbackItem` 의 raw 필드 (`speechPace` 등) 도 동일 — Phase 2a 잔존 + `@JsonIgnoreProperties(ignoreUnknown=true)` 호환, Phase 3 삭제.
  - 필러 (`fillerWordCount`, `fillerWords`) 는 entity / DTO / DB 모두 유지 (사용자 명시 예외).
- `question_score_dimension` 신규 row 의 `rubric_id` = `"nonverbal-v1"` (Lambda 가 응답 dimensions 산출 + BE 가 적재). 기존 `rubric_id="nonverbal"` row (보조 매퍼 산물) backfill 안 함 — product-spec 비스코프 A.
- composure 차원 row = Phase 2a 머지 후 신규 row 0건 (Lambda 가 산출 안 함). 기존 dev row 잔존 (백필 안 함).

### Phase 3 — Flyway DDL (DROP COLUMN)

```sql
-- V{N}__drop_timestamp_feedback_freetext_and_raw_columns.sql
-- Flyway 룰: DDL only (.claude/rules/conventions.md). DML 금지.
-- 컬럼명 = 실측 entity `TimestampFeedback.java` 의 @Column 이름 기준
-- (eye_contact_level / posture_level / expression_label / speech_pace /
--  tone_confidence_level / emotion_label = 실측 raw 6개).
-- speedVariance / gazeOnCameraRatio / postureUnstableCount 는 entity 영속
-- 컬럼이 부재 → DROP 대상에서 제외 (Lambda 응답 부재만으로 처리 완결).

ALTER TABLE timestamp_feedback
    -- 자유서술 4종
    DROP COLUMN nonverbal_comment,
    DROP COLUMN overall_comment,
    DROP COLUMN vocal_comment,
    DROP COLUMN attitude_comment,
    -- ★ raw 측정치 6종 (사용자 결정 2026-05-16 — 루브릭 measurement 흡수)
    -- vocal raw (entity 영속 3개)
    DROP COLUMN speech_pace,
    DROP COLUMN tone_confidence_level,
    DROP COLUMN emotion_label,
    -- vision raw (entity 영속 3개)
    DROP COLUMN eye_contact_level,
    DROP COLUMN posture_level,
    DROP COLUMN expression_label;
    -- ※ filler_word_count, filler_words 는 DROP 대상 부재 (예외 유지).
    -- ※ speed_variance / gaze_on_camera_ratio / posture_unstable_count =
    --    entity 영속 컬럼 부재 (Lambda 응답에만 존재 — Phase 2a 응답 제거로 처리 완결)
```

- V 번호 = 머지 직전 latest + 1 (Flyway naming 룰).
- DROP 컬럼 = 자유서술 4종 + raw 측정치 6종 = 총 **10컬럼**. Phase 2a 머지 후 모두 신규 row NULL 보장 → 동시 제거 안전.
- **컬럼명 실측 검증 (P0-A 정합)**: DDL 작성 직전 entity (`backend/src/main/java/com/rehearse/api/domain/feedback/entity/TimestampFeedback.java`) 의 `@Column(name=...)` 또는 `@Column` 기본명 (필드명 → snake_case 변환) 으로 실측 컬럼명 확정. 본 spec 실측 (2026-05-16 entity Read 결과) = 자유서술 4 (`nonverbal_comment` / `overall_comment` / `vocal_comment` / `attitude_comment`) + raw 6 (`eye_contact_level` / `posture_level` / `expression_label` / `speech_pace` / `tone_confidence_level` / `emotion_label`) + 필러 2 (`filler_word_count` / `filler_words`). entity 부재 raw 3 (`speedVariance` / `gazeOnCameraRatio` / `postureUnstableCount`) = DDL 부재 (Lambda 응답에만 잔존했던 키로, Phase 2a 응답 제거 시점에 처리 완결).
- 과거 row 의 10컬럼 데이터 = 영구 손실 (product-spec 비스코프 A — backfill 안 함 = 자연 도출). 사용자 명시 결정 = "싹다 제거".
- 회귀 안전 순서: Phase 2a 머지 → dev ≥ 3회 10컬럼 NULL 확인 → 코드 grep 0 검증 → Phase 3 PR 머지.

---

## API Contract

### Phase 1 — 변경 없음 (BE only, prompt + validation 만 변경)

### Phase 2a — Lambda → BE callback payload

#### Endpoint
`POST /api/internal/interviews/{interviewId}/question-sets/{questionSetId}/feedback`

#### Request (변경)

```jsonc
{
  "questionSetComment": "...",
  "timestampFeedbacks": [
    {
      "questionId": 123,
      "startMs": 0,
      "endMs": 30000,
      "transcript": "...",

      // ★ 자유서술 4종 키 omit (Lambda 가 더 이상 산출 안 함)
      //   "nonverbalComment", "overallComment", "vocalComment", "attitudeComment"
      // BE 가 @JsonIgnoreProperties(ignoreUnknown=true) 로 잔존 키 수신 시 무시
      // Phase 3 에서 BE DTO 필드 자체 삭제

      // ★ raw 측정치 키 전면 omit (Phase 2a — 사용자 결정 2026-05-16)
      //   vocal raw : "speechPace", "toneConfidenceLevel", "emotionLabel", "speedVariance"
      //   vision raw: "eyeContactLevel", "postureLevel", "expressionLabel",
      //               "gazeOnCameraRatio", "postureUnstableCount"
      //   → analyzer LLM 호출 prompt 가 raw 자연어 산출 지시 미포함. 산출 자체 부재.
      //   → BE 측 entity / DTO / DB 어디에도 raw 형태로 잔존 X (필러 예외).

      // 필러만 유지 (FE 배지 표시용 — 사용자 명시 "필러는 좋은 것 같아. 필러만.")
      "fillerWordCount": 3,
      "fillerWords": ["음", "어"],

      "difficulty": "medium",
      "resumeMode": "INTERROGATION",

      // ★ 비언어 차원 영역 키 분리 (TO-1: Lambda = 분석기별 영역 키 동봉,
      //   BE = 영역 키 순회 + 차원 row insert).
      "nonverbalScore": {
        "vocal": {
          "dimensions": [
            {"dimension_ref": "fluency",         "score": 2, "observation": "한국어 1~2문장", "evidence_quote": "transcript 발췌"},
            {"dimension_ref": "confidence_tone", "score": 3, "observation": "...",            "evidence_quote": "..."}
          ]
        },
        "vision": {
          "dimensions": [
            {"dimension_ref": "eye_contact_posture", "score": 2, "observation": "...", "evidence_quote": "..."}
          ]
        }
      }
    }
  ],
  "isVerbalCompleted": true,
  "isNonverbalCompleted": true   // Lambda 부분 실패 시에도 true 유지 (FE 폴링 무한 로딩 차단)
}
```

#### 부분 실패 케이스

음성 분석기만 산출 (영상 실패):
```jsonc
"nonverbalScore": {
  "vocal": {
    "dimensions": [
      {"dimension_ref": "fluency",         "score": 2, "observation": "...", "evidence_quote": "..."},
      {"dimension_ref": "confidence_tone", "score": 3, "observation": "...", "evidence_quote": "..."}
    ]
  }
  // vision 키 omit (또는 null)
}
```

영상만 산출 (음성 실패):
```jsonc
"nonverbalScore": {
  "vision": {
    "dimensions": [
      {"dimension_ref": "eye_contact_posture", "score": 2, "observation": "...", "evidence_quote": "..."}
    ]
  }
}
```

두 분석기 모두 실패:
```jsonc
"nonverbalScore": null
// 또는 nonverbalScore 자체 omit (BE @JsonIgnoreProperties 으로 동등 처리)
```

#### Error contract

- Lambda 단 dimension 검증 (응답 후): observation 한국어 / evidence transcript substring / score 1-3. 위배 → retry 1회. 재실패 dimension omit + Lambda 로그 `[결함 skip] retry_failed stage={gemini-audio|vision} dimension=X field=Y` + 메트릭 `nonverbal_retry_failed_total{stage,dimension,field}`. Lambda → BE HTTP 응답 = 200 유지 (페이로드만 omit).
- BE 단 `NonverbalScorePersister.persistOne` 분기 (TO-1 영역 키 분리):
  - `payload == null` → `[정상 skip] payloadNull` 로그 + row 미적재.
  - `payload.vocal == null && payload.vision == null` → `[결함 skip] areasEmpty` 로그 + row 미적재.
  - 두 영역 키 dimension 머지 후 유효성 필터 (score / observation / evidence_quote NOT NULL) 결과 empty → `[결함 skip] allInvalid` 로그 + row 미적재.
  - 일부 dimension 만 유효 → 유효 dimension 만 적재 (fault isolation). `question_score.rubric_id="nonverbal-v1"` 단일 row + `question_score_dimension` N row.
- HTTP 400 — `SaveFeedbackRequest` schema 위배 (Bean Validation 실패).
- HTTP 200 + listener `[결함 skip]` — Lambda 페이로드 정상 수신 but 검증 실패. FE 응답 `nonverbalFeedback: null` (전체 실패) 또는 부분 dimensions (부분 실패).
- 404 — interview / questionSet 미존재 (기존 동일).
- 500 — listener 예외 (기존 동일).

### Phase 2a — BE → FE 응답 (`GET /api/feedbacks/{interviewId}`)

기존 `TimestampFeedbackResponse` + 신규 `nonverbalFeedback`:

```jsonc
{
  "id": 123,
  "startMs": 0,
  "endMs": 30000,
  "transcript": "...",
  "isAnalyzed": true,
  "fillerWordCount": 3,

  "technicalFeedback": {
    "rubricCategory": "TECHNICAL",
    "rubricId": "technical-v1",
    "dimensions": [
      {"dimension": "principle_grasp", "score": 2, "observation": "...", "evidenceQuote": "..."}
    ]
  },

  // ★ 신규 — verbal `technicalFeedback` 동일 구조 (Java inner class 명 = NonverbalRubricFeedback,
  //   JSON 키 = "nonverbalFeedback"). dimensions 원소 = 기존 TechnicalDimensionFeedback 재사용.
  "nonverbalFeedback": {
    "rubricId": "nonverbal-v1",
    "dimensions": [
      {"dimension": "fluency",             "score": 2, "observation": "...", "evidenceQuote": "..."},
      {"dimension": "confidence_tone",     "score": 3, "observation": "...", "evidenceQuote": "..."},
      {"dimension": "eye_contact_posture", "score": 2, "observation": "...", "evidenceQuote": "..."}
    ]
  },

  // delivery 잔존 (Phase 2b 에서 nonverbal 필드만 빼고, Phase 3 에서 전체 제거)
  "delivery": {
    "nonverbal": null,            // Phase 2b 부터 항상 null
    "vocal": {...},               // Phase 2a 잔존 — Phase 3 제거
    "attitudeComment": null       // Phase 2a 부터 null (Lambda 산출 중단)
  }
}
```

### Phase 3 — Cleanup 후 응답

```jsonc
{
  // ... 기존 필드
  "technicalFeedback": {...},
  "nonverbalFeedback": {...}
  // delivery 필드 자체 제거
}
```

#### 호환 / 머지 순서

- `SaveFeedbackRequest` / `TimestampFeedbackItem` 에 `@JsonIgnoreProperties(ignoreUnknown = true)` (line 16, 33) 보유 → Phase 2a Lambda 머지 전 BE 가 신규 `dimensions` 키 무시 (적재 안 함) / Phase 2a Lambda 머지 후 BE 가 잔존 `nonverbalComment` 등 4개 키 무시.
- 머지 순서:
  1. **Phase 1 (BE only)** — verbal scorer 한국어 + validation + retry.
  2. **Phase 1 검증 게이트** — dev 인터뷰 1회 + 사용자 승인.
  3. **Phase 2a (Lambda + BE 묶음 PR)** — 응답 스키마 + DTO + persister + response DTO + rubric YAML cleanup + composure 자산 삭제. BE 선행 또는 묶음 모두 가능 (ignoreUnknown 호환).
  4. **Phase 2a 검증 게이트** — dev 인터뷰 1회 + 사용자 승인.
  5. **Phase 2b (FE PR)** — DeliveryTab 폐지 + ContentTab 비언어 카드 + Empty 분기 + 테스트.
  6. **Phase 2b 검증 게이트** — dev 결과 화면 직접 확인 + 사용자 승인.
  7. **Phase 3 (BE + FE + Flyway cleanup PR)** — 4종 컬럼 DROP + entity / DTO / FE types / Lambda 잔존 import 정리.

---

## NF 결정 (11개)

| NF | 결정 | 단서 | confidence |
|---|---|---|---|
| 영향 범위 | BE + Lambda + FE + Flyway 4-way (Phase 별 분리) | RubricScorerPromptBuilder / vision_analyzer / gemini_analyzer / NonverbalScorePersister / SaveFeedbackRequest / TimestampFeedbackResponse / TimestampFeedback entity / nonverbal_rubric_mapper / FE feedback-panel + content-tab + delivery-tab + types + Flyway DDL | 확신 |
| 정합성 | verbal scorer = `RubricScoringEventListener` AFTER_COMMIT async, validation + retry 1회 = 동일 listener 컨텍스트 내. nonverbal persister = `FeedbackSavedEvent` AFTER_COMMIT async listener 컨텍스트 내 `QuestionScorePersister.saveRubric(... "nonverbal-v1" ...)` 단독 `@Transactional` (P0-2: `saveNonverbal` 삭제 후 단일 진입점). Lambda 부분 실패 시 `isNonverbalCompleted=true` 유지 + `nonverbalFeedback` 부분 dimensions/null → FE 폴링 종료 (`analysisStatus` 도달, P0-4) + Empty 카드 표시 | RubricScoringEventListener / FeedbackEventListener / QuestionScorePersister `@Transactional` / G6 | 확신 |
| 실시간성 | 비실시간. Lambda 비동기 + scorer async. 사용자 즉시 대기 없음. **FE 폴링 종료 = `analysisStatus ∈ {COMPLETED, PARTIAL, FAILED, SKIPPED}` 단일 출처** (`interview-analysis-page.tsx:225-227` `isTerminal` + `useAllQuestionSetStatuses` enabled 토글). `delivery.*` 의존 부재 (P0-4 차단 사유 부재) | listener `@Async` + `@TransactionalEventListener(AFTER_COMMIT)` + `QuestionSetAnalysis.completeAnalysis` 상태 전이 | 확신 |
| 부하 | (Phase 1) prompt 토큰 +50-100 (한국어 룰 + verbatim 강조). GPT-4o-mini 128K context 대비 영향 0. retry 발동 시 위배 dimension 한정 +1 round. (Phase 2a) Lambda 응답 토큰 변화: gemini = 자유서술 3섹션 (vocal/attitude/overall) 삭제 + 차원 2개 신설. vision = 자유서술 (positive/negative/suggestion) 삭제 + 차원 1개 신설. 추정 +0% ~ -20% (자유서술 다수 문장 vs short observation/evidence). retry 발동 시 dimension 한정 +1 round | gemini_analyzer 응답 스키마 / vision_analyzer 응답 스키마 / retry 정책 dimension scope | 보통 |
| 동시성 | 단일 turn 처리 컨텍스트 내부. 동시 turn racing 영향 없음. NonverbalScorePersister listener async = 트랜잭션 내 `saveRubric("nonverbal-v1", ...)` 호출 단일 | listener async 격리 | 확신 |
| 마이그레이션 | Phase 1/2a = DB 변경 0. Phase 3 = Flyway DDL 1건 (4컬럼 DROP). 백필 안 함 (신규 row 부터 적용). rubric_id="nonverbal" 과거 row 백필 안 함. composure 과거 row 백필 안 함. product-spec 비스코프 A | conventions.md Flyway 룰 + product-spec 비스코프 A | 확신 |
| 외부 의존 | OpenAI GPT-4o-mini (verbal scorer) + Gemini (Lambda audio) + OpenAI GPT-4o Vision (Lambda frames). 신규 의존 0. 모델 ID = `application-*.yml` / Lambda config 따름 (하드코딩 금지) | 기존 의존 그대로 | 확신 |
| 보안 | OWASP A03 (Injection): prompt 안 `<<<USER_ANSWER>>>` 마커 룰 보존. A04 (Insecure Design): Lambda + BE 양단 dimension validation + retry 1회 + 실패 dimension omit. evidence_quote = transcript substring 강제 (LLM 출력 injection 차단). A09 (Logging): 재시도 사유 로그 = 한국어 + 도메인 ID (interviewId / questionId / stage / dimension / field). 민감정보 (transcript 일부) 로그 시 길이 제한 권장 | template line 1-4 보존 + adapter validation + persister 로그 패턴 (plan-472) | 확신 |
| 관찰성 | 로그 분류: `[정상 skip] payloadNull` / `[결함 skip] dimensionsEmpty` / `[결함 skip] allInvalid` / `[결함 skip] retry_failed stage=X dimension=Y field=Z`. 메트릭: `rubric_retry_failed_total{stage=verbal,dimension,field}` (BE) + `nonverbal_retry_failed_total{stage=gemini-audio|vision,dimension,field}` (Lambda). 메트릭 백엔드 = 기존 application 설정 따름 (Datadog 또는 동등) | NonverbalScorePersister plan-472 로그 패턴 확장 + Micrometer counter | 확신 |
| 롤백 | Phase 별 분리 → 각 단계 단독 revert. Phase 1 revert = prompt + adapter 원상복구 (DB 영향 0). Phase 2a revert = BE DTO + persister + Lambda + rubric YAML 원상복구 (DB 영향 0). Phase 2b revert = FE 만 (DeliveryTab 복원). Phase 3 revert = DDL 역마이그레이션 (`ADD COLUMN`) 단, 과거 데이터 복구 불가 (DROP 시점 손실) | conventions.md Flyway 룰 | 확신 |
| 검증 | 자동화 회귀 테스트 = 단일 채널. 카테고리 매핑: Domain Unit (rubric YAML 로더 / prompt builder / `RubricScorerResponseValidator`) + Service Integration (RubricScorer + NonverbalScorePersister + QuestionScorePersister + FeedbackService) + Infra Integration (`RubricScoringAdapter` retry 시나리오 + validator) + Repository (Flyway smoke) + Lambda pytest (gemini / vision / handler / dimension_validator) + ArchUnit (orphan 감지) + FE Integration (content-tab / rubric-dimension-card / feedback-panel filler / interview-analysis-page polling) + Migration smoke (Phase 3) | testing.md / Lambda pytest / Vitest+RTL+msw / ArchUnit | 확신 |

모호 항목 0. NF 11개 모두 충족 + Phase 별 책임 분리.

---

## Verification (완료 판정)

AC 8건과 1:1 매핑 + 카테고리 명시. 통과 기준 = 회귀 테스트 단일 채널.

### Phase 1 (BE verbal scorer)

| AC | 카테고리 | 테스트 케이스 | 통과 기준 |
|---|---|---|---|
| AC-1 | Service Integration | `RubricScorerServiceTest.korean_observation_when_normal_response` (`extends ServiceIntegrationSupport`) — LLM 응답 stub (한국어 observation) → `DimensionScore.observation` 한국어 음절 정규식 `[\\uAC00-\\uD7A3]` 1+ 매치 assert. 자모 단독 (`ㄱ-ㅎ` / `ㅏ-ㅣ`) 만 있는 응답 = 위배 처리 회귀 케이스 추가 | observation = 한국어 음절 1+, ASCII-only / 자모-only 0건 |
| AC-1 / AC-2 validator | Infra Integration | `RubricScorerResponseValidatorTest extends InfraIntegrationSupport` (또는 Domain Unit) — (a) observation 영문 단독 → 위배 `NonKoreanObservation`. (b) observation 영문+한국어 1음절 동반 → 통과. (c) evidence_quote = 루브릭 정의문 (`"팀에서 했어요 수준"`) substring → 위배 `EvidenceNotInUserAnswer`. (d) evidence_quote = userAnswer substring (whitespace 정규화 후) → 통과 | validator 단일 정책 정합 |
| AC-2 | Service Integration | `RubricScorerServiceTest.evidence_is_user_answer_substring_when_normal_response` — `userAnswer.contains(evidenceQuote)` (whitespace 정규화) assert + rubric anchor 정의문 (`"팀에서 했어요 수준"`) substring 미포함 assert | evidenceQuote ∈ userAnswer 발췌, 정의문 / 질문 본문 인용 0건 |
| AC-1 / AC-2 retry | Infra Integration | `RubricScoringAdapterTest extends InfraIntegrationSupport` — (a) 1차 위배 (영어 observation) → 2차 retry 정상 → 정상 적재. (b) 1차/2차 모두 위배 → 해당 dimension row 미적재 + `[결함 skip] retry_failed` 로그 + 메트릭 counter 증가. (c) dimension A 위배 + B 정상 = fault isolation | retry 1회 동작 + 메트릭 counter (`rubric_retry_failed_total{stage=verbal,dimension,field}`) +1 |
| AC-1 prompt | Domain Unit | `RubricScorerPromptBuilderTest extends DomainUnitSupport` — `buildSystemPrompt()` 반환에 "한국어" substring 포함 + `build()` 결과 prompt 에 "verbatim substring" 룰 포함 | 한국어 출력 룰 / verbatim 룰 prompt 내 명시 |
| AC-1 template | Domain Unit | template 로드 후 영어 Output Schema 예시 (`"The candidate explained the principle..."`) substring 부재 assert | 영어 예시 0 |
| AC-7 검증 게이트 | dev 인터뷰 1회 (사용자 수행) | verbal 출력 한국어 + evidence verbatim 직접 확인 후 Phase 2 진입 명시 승인 | 사용자 명시 승인 |
| 회귀 | `./gradlew test --tests "*Rubric*"` 전체 | 기존 verbal scorer 회귀 0 | green |
| 빌드 | `./gradlew build` | — | green |

### Phase 2a (Lambda + BE)

| AC | 카테고리 | 테스트 케이스 | 통과 기준 |
|---|---|---|---|
| AC-3 Lambda | Lambda pytest | `test_gemini_analyzer.py.test_response_has_nonverbal_dimensions_when_normal_audio` — 응답 stub 검증: `transcript` 유지 + `vocal.fillerWords` / `vocal.fillerWordCount` 유지 + **자유서술 (`positive/negative/suggestion`) 키 부재** + **vocal raw 측정치 키 (`speechPace`, `toneConfidenceLevel`, `emotionLabel`, `speedVariance`) 응답 부재** assert + `nonverbalDimensions.{fluency, confidence_tone}` 각 `{score, observation, evidence_quote}` 구조 assert | 자유서술 키 0 + vocal raw 키 0 (필러 제외) + dimensions 2차원 단일 구조 |
| AC-3 Lambda | Lambda pytest | `test_vision_analyzer.py.test_response_has_eye_contact_posture_dimension` — `analyze_frames(frames, transcript)` 호출 시 자유서술 키 부재 + **vision raw 측정치 키 (`eyeContactLevel`, `postureLevel`, `expressionLabel`, `gazeOnCameraRatio`, `postureUnstableCount`) 응답 부재** assert + `nonverbalDimensions.eye_contact_posture` 단일 구조 + `evidence_quote ∈ transcript` assert | 자유서술 키 0 + vision raw 키 0 + 차원 1개 (영상 영역) |
| AC-3 Lambda handler | Lambda pytest | `test_handler.py.test_payload_has_no_raw_metrics` — `_run_gemini_pipeline` + `_run_legacy_pipeline` 결과 `fb` dict 에 raw 키 9종 (`speechPace`, `toneConfidenceLevel`, `emotionLabel`, `speedVariance`, `eyeContactLevel`, `postureLevel`, `expressionLabel`, `gazeOnCameraRatio`, `postureUnstableCount`) 모두 부재 assert. **단 필러 키 (`fillerWords`, `fillerWordCount`) 는 존재** assert (예외 유지) | BE 페이로드 raw 키 0 + 필러 키 유지 |
| AC-3 BE callback | Service Integration | `FeedbackServiceTest.raw_metric_columns_remain_null_when_lambda_omits_them` — 신규 페이로드 (raw 측정치 키 9종 omit) 수신 → `TimestampFeedback` entity 의 **영속 raw 6 컬럼** (`speech_pace` / `tone_confidence_level` / `emotion_label` / `eye_contact_level` / `posture_level` / `expression_label` — 실제 컬럼명 = entity `@Column(name)` 으로 식별) + 자유서술 4 컬럼 모두 NULL DB assert. `fillerWordCount` / `fillerWords` 만 적재 assert. `speedVariance` / `gazeOnCameraRatio` / `postureUnstableCount` = entity 영속 부재 → BE DB NULL assert 대상에서 제외 (Lambda 응답 부재만 별도 Lambda assert 로 보호). | **10컬럼** NULL (자유서술 4 + 영속 raw 6) + 필러 정상 적재 |
| AC-3 / AC-5 retry | Lambda pytest | `test_*_analyzer.py.retry_omit_scenarios` — (a) 1차 위배 → 2차 retry 정상. (b) 1차/2차 모두 위배 → dimension omit + `[결함 skip] retry_failed` 로그. (c) 부분 omit 응답 = handler 가 그대로 BE 페이로드 전달 | retry 1회 동작 + omit 정책 동작 |
| AC-3 / AC-8 handler | Lambda pytest | `test_handler.py.test_area_split_assembly_in_both_pipelines` — `_run_gemini_pipeline` + `_run_legacy_pipeline` 모두 `fb["nonverbalScore"]["vocal"]["dimensions"]` (fluency / confidence_tone 원소) + `fb["nonverbalScore"]["vision"]["dimensions"]` (eye_contact_posture 원소) **영역 키 분리** 조립 정확성 assert (TO-1 영역 키 분리). 4종 자유서술 키 (`nonverbalComment / vocalComment / attitudeComment / overallComment`) `fb` dict 부재 assert. 부분 실패 (gemini 만 → `vocal` 키만 존재 / vision 만 → `vision` 키만 존재 / 둘 다 → `nonverbalScore=None`) 시 영역 키 구성 assert + `isNonverbalCompleted=true` 유지 assert | 자유서술 적재 0 + 영역 키 분리 + 부분 실패 graceful |
| Lambda dead code | Lambda pytest collect | `nonverbal_rubric_mapper.py` import grep = 0 + 파일 부재 | import 0 |
| AC-4 / AC-5 BE | Service Integration | `NonverbalScorePersisterTest extends ServiceIntegrationSupport` — 5 시나리오: (1) 정상 vocal+vision 영역 키 동시 수신 → 3차원 dims 머지 → `question_score.rubric_id="nonverbal-v1"` 1 row + `question_score_dimension` 3 row + observation/evidence NOT NULL DB assert. (2) payload null = row 0 + `[정상 skip] payloadNull`. (3) vocal/vision 둘 다 null = row 0 + `[결함 skip] areasEmpty`. (4) vocal 만 산출 (vision null) = `question_score` 1 row + dimension 2 row (fluency / confidence_tone) + fault isolation. (5) 한 dimension 만 observation null = 해당 dimension skip + 나머지만 적재 + `[결함 skip] allInvalid` 부재. composure dimension row 0건 (rubric YAML 정의 부재). **rubric_id="nonverbal" (legacy) 신규 row 0건 assert** (P0-2 saveNonverbal 삭제 검증) | 3차원 정확 적재 + composure row 0 + 부분 실패 fault isolation + rubric_id="nonverbal-v1" 단일 |
| AC-4 / AC-5 BE | Service Integration | `QuestionScorePersisterTest.saveRubric_with_nonverbal_v1_rubric_id` — 호출자가 rubric_id="nonverbal-v1" 주입 시 정상 적재 + idempotent 중복 호출 시 row 1개 유지 (`findByQuestionIdAndRubricId` 정합) assert. (`saveNonverbal` 호출 테스트 = **삭제** — 메서드 자체 부재) | saveRubric 단일 진입점 + rubric_id 주입 정합 |
| AC-3 BE | Service Integration | `FeedbackServiceTest.commentBlock_fields_remain_null_when_lambda_omits_them` — 신규 페이로드 (`nonverbalComment` 등 4종 키 omit) 수신 → `TimestampFeedback.{nonverbalComment, overallComment, vocalComment, attitudeComment}` 모두 NULL DB assert | 4컬럼 NULL |
| BE Orphan | grep + 빌드 | `grep -rn "NonverbalRubricScorer\\|NonverbalContextWeightsLoader" backend/src` = 0 + `./gradlew compileJava` green | orphan 삭제 검증 |
| BE Orphan ArchUnit (P1) | ArchUnit | `backend/src/test/java/com/rehearse/api/arch/NonverbalOrphanArchTest.java` (신규) — `classes().that().haveSimpleName("NonverbalRubricScorer").or().haveSimpleName("NonverbalContextWeightsLoader").should().notExist()` (또는 classes 가 비어있어야 함을 확인하는 룰). Lambda 측 `nonverbal_rubric_mapper.py` 동일 패턴은 pytest collect 검증으로 대체. 본 spec 범위 = BE ArchUnit 1건 | ArchUnit green |
| AC-4 rubric YAML | Domain Unit | `RubricCatalogTest.no_composure_dimension_when_nonverbal_v1_loaded` — `nonverbal-v1` rubric `selectDimensions` 결과에 `composure` 부재 assert. fluency description / measurement 에 "떨림" / "끊김" / "속도" substring 1+ 포함 assert (raw `speechPace` / `speedVariance` 흡수 표현 검증). confidence_tone measurement 에 "톤" + "속도" substring 포함 assert (raw `toneConfidenceLevel` / `emotionLabel` / `speedVariance` 흡수). eye_contact_posture description / measurement 에 "표정" + "시선" + "자세" substring 포함 assert (raw `expressionLabel` / `eyeContactLevel` / `gazeOnCameraRatio` / `postureUnstableCount` 흡수). 운영자 리뷰 보완 검증 | composure 정의 부재 + 3차원 measurement 에 raw 신호 흡수 표현 명시 |
| 운영자 리뷰 게이트 (P1-E) | 사용자 명시 승인 (체크리스트 9건) | `_dimensions.yaml` diff 가 vocal raw 4 신호 (`speechPace` / `toneConfidenceLevel` / `emotionLabel` / `speedVariance`) + vision raw 5 신호 (`eyeContactLevel` / `postureLevel` / `expressionLabel` / `gazeOnCameraRatio` / `postureUnstableCount`) 의 측정 표현을 포함하는지 작성자가 9건 체크리스트 작성 + 사용자가 항목별 명시 승인. 게이트 시점 = Phase 2a 머지 게이트 = (a) 회귀 테스트 green + (b) 사용자 운영자 리뷰 승인 동시. 책임자 = 작성자 (체크리스트 작성) + 사용자 (명시 승인) | 9건 모두 승인 |
| 회귀 | `./gradlew test --tests "*Rubric*" / "*Feedback*" / "*Nonverbal*"` + `cd lambda/analysis && pytest` | — | green |
| 빌드 | `./gradlew build` | — | green |
| Phase 2a 검증 게이트 | dev 인터뷰 1회 (사용자 수행) | DB `question_score_dimension` rubric_id="nonverbal-v1" 3차원 적재 확인 + **10컬럼 NULL** (자유서술 4 + entity 영속 raw 6) 확인 + 사용자 승인 | 사용자 명시 승인 |

### Phase 2b (FE)

| AC | 카테고리 | 테스트 케이스 | 통과 기준 |
|---|---|---|---|
| AC-6 단일 흐름 | FE Integration (Vitest + RTL + msw) | `content-tab.test.tsx.shows_verbal_and_nonverbal_cards_when_both_present` — `technicalFeedback` + `nonverbalFeedback` 정상 데이터 → verbal rubric 카드 + 비언어 rubric 카드 3개 (fluency / confidence_tone / eye_contact_posture) 렌더 assert. tab navigation 부재 (`screen.queryByRole('tab')` 0) assert. 자유서술 텍스트 (`긍정/부정/제안`) 부재 assert | tabs 0 + 비언어 카드 3개 + 자유서술 0 |
| AC-6 / AC-8 부분 실패 | FE Integration | `content-tab.test.tsx.shows_partial_warning_when_dimensions_lt_3` — `nonverbalFeedback.dimensions.length=2` → 카드 2개 + "비언어 분석 일부 실패" 1줄 안내 노출 | 부분 카드 + 안내 |
| AC-6 / AC-8 전체 실패 | FE Integration | `content-tab.test.tsx.shows_empty_card_when_nonverbal_feedback_null` — `nonverbalFeedback === null` → "분석 실패 — 점수 없음" Empty 카드 렌더 | Empty 카드 |
| AC-6 컴포넌트 단위 | FE Integration | `rubric-dimension-card.test.tsx` — props `{ dimension: {dimension, score, observation, evidenceQuote} }` → score + observation + evidence 렌더 assert | 카드 단위 회귀 |
| AC-6 DeliveryTab 폐지 | FE 코드 grep | `grep -r "DeliveryTab\\|delivery-tab" frontend/src` = 0 + `delivery-tab.tsx` 파일 부재 | DeliveryTab 참조 0 |
| AC-6 필러 배지 유지 (사용자 결정 #2) | FE Integration | `feedback-panel.test.tsx.shows_filler_count_badge_when_filler_present` — `fillerWordCount=3` → "습관어 3회 감지" 텍스트 노출 assert. `fillerWordCount=null` 또는 `0` → 배지 부재 assert. source 경로 = `feedback.fillerWordCount` (top-level) | 필러 카운트 배지 회귀 |
| AC-8 폴링 종료 (P0-4) | FE Integration | `interview-analysis-page.test.tsx.polling_stops_when_analysis_status_terminal` — Lambda 부분 실패 페이로드 수신 후 BE `analysisStatus=COMPLETED` 또는 `PARTIAL` 응답 시 `useAllQuestionSetStatuses` 가 종료 (msw status endpoint 점진 응답: PENDING → ANALYZING → COMPLETED 시퀀스 + `refetchInterval` 호출 횟수 종료 검증). `delivery.*` 필드 의존 부재 → P0-4 차단 사유 부재 회귀 보호 | 폴링 종료 + delivery 의존 부재 |
| 회귀 | `npm run test` 전체 + `npm run lint` + `npm run build` | — | green |
| Phase 2b 검증 게이트 | dev 결과 화면 직접 확인 (사용자 수행) | 단일 흐름 / 자유서술 카드 부재 / 원시 측정치 카드 부재 (필러 카운트 배지 1건은 유지) / composure 카드 부재 / 비언어 3차원 카드 노출 / 부분 실패 안내 / 폴링 종료 확인 + 사용자 승인 | 사용자 명시 승인 |

### Phase 3 (Cleanup)

| AC | 카테고리 | 테스트 케이스 | 통과 기준 |
|---|---|---|---|
| AC-7 Schema | Repository (Testcontainers + Flyway) | `TimestampFeedbackRepositoryTest extends RepositorySupport` — Flyway V{N} 적용 후 `timestamp_feedback` 컬럼 목록에 자유서술 4컬럼 (`nonverbal_comment / overall_comment / vocal_comment / attitude_comment`) + **raw 측정치 6컬럼 (`speech_pace / tone_confidence_level / emotion_label / eye_contact_level / posture_level / expression_label`)** 부재 assert. **필러 컬럼 (`filler_word_count`, `filler_words`) 은 존재 assert** (예외 유지). `speed_variance` / `gaze_on_camera_ratio` / `posture_unstable_count` = entity 영속 부재 → DDL 대상 외 (assert 대상 외) | **10컬럼** DROP + 필러 유지 |
| AC-7 BE grep | grep | `grep -r "nonverbal_comment\\|nonverbalComment\\|vocal_comment\\|vocalComment\\|attitude_comment\\|attitudeComment\\|overall_comment\\|overallComment\\|speechPace\\|toneConfidenceLevel\\|emotionLabel\\|speedVariance\\|eyeContactLevel\\|postureLevel\\|expressionLabel\\|gazeOnCameraRatio\\|postureUnstableCount" backend/src` = 0 매치 | 자유서술 + raw 참조 0 |
| AC-7 FE grep | grep | 동일 패턴 `frontend/src` = 0 매치. `CommentBlock / DeliveryFeedback / NonverbalFeedback / VocalFeedback / StructuredComment / LevelBadge` 참조 0 검증 (잔존 시 별도 보고 후 삭제) | 참조 0 |
| AC-7 Lambda grep | grep | 자유서술 패턴 `lambda/analysis/handler.py` = 0 매치 (Phase 2a 이미 0 — 더블체크). **raw 측정치 패턴 `lambda/analysis/handler.py` 의 `fb["..."]` 적재부 = 0 매치** (필러 line 만 잔존). analyzer 내부 (`gemini_analyzer.py` / `vision_analyzer.py`) raw 산출 코드는 채점 입력으로 유지 = grep 매치 정상 | handler payload raw 키 0 + 필러만 유지 |
| 회귀 | `./gradlew test` 전체 + `npm run test` + `pytest` 전체 | — | green |
| Migration smoke | Flyway boot | `./gradlew bootRun --args='--spring.profiles.active=local'` 부팅 시 V{N} 적용 + 정상 기동 | Flyway 적용 성공 |

### 공통

- `./gradlew build` + `npm run build` + `pytest` 모두 green.
- 컨벤션 준수: BE `conventions.md` (Flyway DDL only / `@Transactional` 위치 / Entity 직접 반환 금지 / Lombok / 한국어 로그) + BE `testing.md` (E2E ≤5% / TRUNCATE cleanup / Mock 정책) + FE `conventions.md` (`any` 금지 / props ≤2 / 동적 Tailwind 금지) + FE `architecture.md` (apiClient 단일 진입점) + FE `testing.md` (행위 테스트 / 경계만 Mock).

---

## Pre / Post State

### Phase 1

#### Pre
- `RubricScorerPromptBuilder.buildSystemPrompt()` = 영어. template Output Schema 영어 예시 + 한국어 Scoring Example 혼재. evidence_quote verbatim 룰 약함.
- `RubricScoringAdapter` = LLM 응답 매핑 후 검증 / retry 정책 부재.
- DB: `question_score_dimension` rubric_id ∈ {resume-v1, behavioral-v1, technical-v1} observation 100% 영어 + evidence anchor 인용.

#### Post
- `buildSystemPrompt()` 한국어. template 한국어 단일 + verbatim 룰 강화. dimension 단위 validation (`observation` 한국어 / `evidence_quote` ∈ userAnswer / `score` 1-3) + 위배 dimension retry 1회 + 재실패 시 `[결함 skip] retry_failed` 로그 + Micrometer counter (`rubric_retry_failed_total{stage=verbal,dimension,field}`).
- DB: 신규 row observation 한국어 + evidence transcript substring. ASCII-only / anchor 인용 0건 (회귀 테스트 보호).
- 기존 row 백필 안 함.

### Phase 2a

#### Pre
- `lambda/analysis/handler.py` `_run_gemini_pipeline` + `_run_legacy_pipeline` 양쪽이 자유서술 4종 (`nonverbalComment / overallComment / vocalComment / attitudeComment`) + 결정론 매퍼 점수 (`nonverbalScore = {fluency, confidence_tone, eye_contact_posture, composure} int 4개`) 동시 적재.
- `gemini_analyzer` 응답 = transcript + vocal/attitude/overall_delivery 자유서술 3섹션 + raw vocal.
- `vision_analyzer` 응답 = positive/negative/suggestion 자유서술 + raw vision metrics.
- `nonverbal_rubric_mapper.py` (Python, 119 lines) 결정론 매퍼 잔존.
- `NonverbalScorePersister` 가 `NonverbalRubricScorer.score(...)` 호출 → `DimensionScore.of(score, null, null)` 변환 적재 (4 row, observation/evidence NULL, composure 조건부).
- `NonverbalRubricScorer` + `NonverbalContextWeightsLoader` (main 2) + 의존 test 3 파일 잔존.
- `SaveFeedbackRequest.NonverbalScore = {fluency, confidenceTone, eyeContactPosture, composure, rawSignals}` int 4개.
- `_dimensions.yaml` 에 composure 정의 잔존. `nonverbal-rubric.yaml` 에 composure 참조 잔존.
- DB: `question_score_dimension` rubric_id ∈ {"nonverbal-v1" (보조 매퍼 또는 본 변경 후 신규), "nonverbal" (보조 매퍼 잔존)} observation/evidence 100% NULL, composure row 일부 (medium+ turn).
- `TimestampFeedback` entity = 4컬럼 (`nonverbalComment / overallComment / vocalComment / attitudeComment`) TEXT 풍부.

#### Post
- Lambda gemini_analyzer 응답 = transcript + **vocal.fillerWords / vocal.fillerWordCount** (필러 예외) + `nonverbalDimensions.{fluency, confidence_tone}` 단일 구조. 자유서술 (vocal.positive/negative/suggestion 등) 0. **vocal raw 측정치 (speechPace / toneConfidenceLevel / emotionLabel / speedVariance) = 산출 단계 자체 부재** — analyzer LLM prompt 가 raw 자연어 산출 지시 미포함, rubric YAML measurement 가 LLM 채점 가이드 역할. validation = `dimension_validator.py` 공유 모듈 (한국어 음절 / transcript substring / score 1-3) + retry 1회 (보강 prompt).
- Lambda vision_analyzer 응답 = `nonverbalDimensions.eye_contact_posture` 단일 구조. 자유서술 0. **vision raw 측정치 (eyeContactLevel / postureLevel / expressionLabel / gazeOnCameraRatio / postureUnstableCount) = 산출 단계 자체 부재** — analyzer LLM prompt 가 raw 자연어 산출 지시 미포함, rubric YAML measurement 가 LLM 채점 가이드 역할. validation = 동일 모듈.
- **Lambda handler 가 두 분석기 응답을 음성/영상 영역 키로 분리 페이로드 동봉** (TO-1 채택안). `fb["nonverbalScore"] = {vocal: {dimensions: [...]}, vision: {dimensions: [...]}}` 또는 부분 / `None`. 자유서술 4종 키 부재. `_run_legacy_pipeline` 동일 cleanup. `nonverbal_rubric_mapper.py` 삭제.
- **BE `SaveFeedbackRequest.NonverbalScore = {vocal?: AreaScore, vision?: AreaScore}`** (영역 키 분리). `AreaScore = {dimensions: List<DimensionScoreItem>}`. `DimensionScoreItem = {dimension_ref, score, observation, evidence_quote}` + Bean Validation 어노테이션.
- **BE `NonverbalScorePersister.persistOne` = 두 영역 키 dimension 머지 + 유효성 필터 + `questionScorePersister.saveRubric(... "nonverbal-v1" ...)` 호출** (rubric_id 명시 주입). `NonverbalRubricScorer` 호출 0. dimension 단위 fault isolation. composure dimension 신규 row 0건.
- **BE `QuestionScorePersister.saveNonverbal(...)` 메서드 삭제** (P0-2). `saveRubric` 단일 진입점. rubric_id 하드코딩 (`"nonverbal"`) 제거.
- BE `NonverbalRubricScorer` / `NonverbalContextWeightsLoader` (main 2) + 의존 test 3 파일 삭제.
- BE `_dimensions.yaml` = composure 정의 삭제 + **fluency / confidence_tone / eye_contact_posture 3차원 measurement + observable 확장 (raw 신호 흡수)**:
  - fluency description / measurement = "필러 + 더듬·끊김 + 발화 속도 변동" (raw `speechPace` / `speedVariance` 흡수).
  - confidence_tone measurement = "톤 안정성 + 발화 속도 분산" (raw `toneConfidenceLevel` / `emotionLabel` / `speedVariance` 흡수).
  - eye_contact_posture description / measurement = "시선 + 자세 + 표정" (raw `eyeContactLevel` / `postureLevel` / `expressionLabel` / `gazeOnCameraRatio` / `postureUnstableCount` 흡수).
  - scoring observable 도 raw 신호 표현 추가 (각 차원 L1/L2/L3 에 1줄씩 보강).
- BE `nonverbal-rubric.yaml` = `uses_dimensions` 3개 (composure 제거, weight 1/3 각 또는 사용자 결정) + `per_turn_rules` 단일 default + `level_expectations` 에서 composure 제거.
- **BE entity / DTO 의 raw 측정치 필드 잔존 (Phase 2a 시점)**: `TimestampFeedback` entity 의 영속 raw 필드 6개 (`speechPace` / `toneConfidenceLevel` / `emotionLabel` / `eyeContactLevel` / `postureLevel` / `expressionLabel`) + `SaveFeedbackRequest.TimestampFeedbackItem` 의 raw 필드 (DTO 잔존 키, entity 영속 합집합 또는 일부 = 구현 진입 직전 grep 확인) = Phase 2a 잔존, Lambda 산출 부재 → DB 신규 row 영속 컬럼 100% NULL. Phase 3 schema cleanup 합류. `speedVariance` / `gazeOnCameraRatio` / `postureUnstableCount` = entity 영속 부재 → Phase 3 schema cleanup 대상 외.
- **BE 응답 DTO (`TimestampFeedbackResponse`) 의 raw 측정치 노출** = Phase 2a 시점 잔존 (entity read 결과 NULL → 응답 필드 NULL). FE Phase 2b 에서 표시 제거. Phase 3 에서 응답 필드 자체 삭제 합류.
- **BE `TimestampFeedbackResponse` 에 `nonverbalFeedback {rubricId, dimensions}` 신설** (verbal `technicalFeedback` 와 동일). inner class 명 = `NonverbalRubricFeedback` (기존 `NonverbalFeedback` 충돌 회피). `from(feedback, questionScores, dimsByScoreId)` 시그니처 확장 — `questionScores: List<QuestionScore>` 입력 (verbal + nonverbal 동시 포함). `QuestionSetFeedbackResponse.from` 의 Map 타입 `Map<Long, QuestionScore>` → `Map<Long, List<QuestionScore>>` 변경. `FeedbackService` 갱신 (group by questionId).
- BE `TimestampFeedback` entity 4컬럼 = 잔존 (Phase 2a 시점, Phase 3 에서 DROP). 신규 row 4컬럼 = 100% NULL.
- DB: `question_score.rubric_id="nonverbal-v1"` 신규 row (단일 row) + `question_score_dimension` 신규 row 3차원 + observation 한국어 / evidence transcript substring NOT NULL. composure 신규 row 0건.
- BE 단일 validator (`RubricScorerResponseValidator`) + Lambda 단일 validator (`dimension_validator.py`) — 검증 정책 단일 출처. rubric YAML build-time copy 로 Lambda 동기화.
- 기존 row 백필 안 함.

### Phase 2b

#### Pre
- FE `feedback-panel.tsx` = `Tabs` 2-tab 구조 (content + delivery). `delivery-tab.tsx` (170 lines) 자유서술 카드 + 원시 측정치 카드 + LevelBadge 렌더. `content-tab.tsx` (119 lines) verbal rubric 단일.
- FE types `DeliveryFeedback.nonverbal` 등 잔존.

#### Post
- FE `feedback-panel.tsx` = `Tabs` 제거, `ContentTab` 단일 렌더. `delivery-tab.tsx` 파일 삭제.
- FE `content-tab.tsx` = verbal + 비언어 rubric 카드 그룹 2개. `RubricDimensionCard` 신규 컴포넌트 재사용.
- FE `types/interview.ts` = `TimestampFeedback.nonverbalFeedback: NonverbalFeedback | null` 신설. `DeliveryFeedback.nonverbal` 필드 제거 (interface 자체 vocal/attitudeComment 잔존 — Phase 3 전체 제거).
- 부분 실패 = "비언어 분석 일부 실패" 1줄. 전체 실패 = "분석 실패 — 점수 없음" Empty.
- tab navigation / DeliveryTab 참조 / 원시 측정치 카드 / 자유서술 카드 / composure 카드 = 화면 트리에서 부재.
- **필러 카운트 배지 (`feedback-panel.tsx:111-117` "습관어 N회 감지") 유지 (사용자 결정 #2)**. source 경로만 `feedback.delivery?.vocal?.fillerWordCount` → `feedback.fillerWordCount` (top-level 필드, `TimestampFeedbackResponse.fillerWordCount` 이미 존재) 로 단순화.
- **폴링 종료 메커니즘 변경 0 (P0-4)**. `useAllQuestionSetStatuses` / `interview-analysis-page.tsx` 의 `isTerminal(analysisStatus)` 기반 종료 로직은 본 Phase 영향 받지 않음 (delivery.* 의존 부재 확인). 신규 회귀 테스트로 부분 실패 시 폴링 종료 보호.

### Phase 3

#### Pre
- DB `timestamp_feedback` = 자유서술 4컬럼 (`nonverbal_comment / overall_comment / vocal_comment / attitude_comment`) + **entity 영속 raw 측정치 6컬럼 (vocal 3: `speech_pace`, `tone_confidence_level`, `emotion_label` / vision 3: `eye_contact_level`, `posture_level`, `expression_label`)** 잔존 (Phase 2a 후 10컬럼 모두 신규 row 100% NULL). 정확 컬럼명은 실제 entity `@Column(name)` 으로 확인 (P0-A). `speed_variance` / `gaze_on_camera_ratio` / `posture_unstable_count` = entity 영속 컬럼 부재 (Lambda 응답에만 존재했던 키, Phase 2a 응답 부재로 처리 완결).
- BE `TimestampFeedback` entity = 4 자유서술 필드 + 6 raw 측정치 필드 + `@Builder` param 잔존. `SaveFeedbackRequest.TimestampFeedbackItem` = 4 `CommentBlock` + raw 필드 잔존 (실제 필드 = 구현 직전 grep 확인 후 정리). `TimestampFeedbackResponse.DeliveryFeedback / NonverbalFeedback / VocalFeedback / CommentBlock` inner class + raw 필드 잔존.
- FE `types/interview.ts` = `DeliveryFeedback / NonverbalFeedback / VocalFeedback / CommentBlock` type 잔존. `isCommentBlockEmpty / StructuredComment / LevelBadge / format-feedback-level` 잔존 (Phase 2b 후 사용 0 가능).

#### Post
- DB 10컬럼 DROP (Flyway V{N}, 자유서술 4 + entity 영속 raw 6).
- BE entity / DTO / Response DTO 잔존 필드 (자유서술 4 + raw 6) + inner class 정리. `CommentBlock` 참조 0 = 삭제. raw 필드 (`speechPace` 등 entity 영속 6개) entity / DTO / Response inner class (`VocalFeedback`, `NonverbalFeedback`) 동시 제거.
- FE types / 잔존 utility (`isCommentBlockEmpty` 등) 참조 grep 0 검증 후 삭제.
- 필러 (`fillerWordCount`, `fillerWords`) 만 entity / DTO / DB / 응답 / FE 모두 유지 (예외).
- Lambda 변경 0 (Phase 2a 에서 이미 산출 중단).

---

## 위험 / 마이그레이션 / 롤백

### 위험

- **Phase 1 prompt 변경 후 LLM 출력 회귀** — 한국어 강제 + verbatim 강제 prompt 가 채점 정확도 저하 가능. 검증 게이트 (AC-1/2 회귀 테스트) + retry 1회 + 메트릭 운영 감시.
- **Phase 1 prompt 토큰 증가** — ~50-100 token. GPT-4o-mini 128K 대비 영향 0.
- **Phase 1/2a retry 1회 비용** — retry hint 는 위배 dimension 만 명시하지만 어댑터는 전체 prompt 를 1회 재호출 (전체 dimension prompt 토큰 비용 추가, 재채택은 위배 dimension 한정). 위배율은 메트릭 (`rubric_retry_failed_total{stage,dimension,field}` + `nonverbal_retry_failed_total`) 으로 측정. 임계 (예: 10%+) 초과 시 prompt 추가 튜닝 또는 DB audit 컬럼 도입 재논의 (본 spec 비스코프).
- **Phase 2a Lambda 응답 토큰 영향** — gemini 자유서술 3섹션 + raw 4 키 삭제 vs 차원 2개 (observation + evidenceQuote) 추가 = 추정 -30%~+10%. vision 자유서술 + raw 5 키 삭제 vs 차원 1개 추가 = 추정 ±0%. **max_tokens 실측 timing (P1-C)**: 구현 진입 직전 dev 1회 dry-run (gemini / vision 응답 token 실측) → max_tokens 한도 사전 조정 후 PR 작성. 불충분 시 PR 분리 (token 한도 조정 PR 선행).
- **Phase 2a Lambda 책임 분리 (TO-1 영역 키 분리)** — gemini = audio 차원 2개 (fluency / confidence_tone, payload `vocal` 키), vision = frames 차원 1개 (eye_contact_posture, payload `vision` 키). 한 분석기 실패 시 다른 영역 키만 존재 (혹은 `null`) → BE 가 받은 영역 키 dimension 만 row insert (trivial) → FE 부분 카드 노출. 두 분석기 모두 실패 시 `nonverbalScore=null` + Empty 카드.
- **Phase 2a fluency 정의 확장 (떨림·끊김 + 속도 변동 흡수)** — gemini_analyzer LLM 호출 prompt 가 transcript + audio 직접 청취 신호로 떨림·끊김 + 속도 변동 채점 가능성. 미흡 시 LLM observation 이 filler 만 묘사 → 사용자 인지 "정의는 떨림인데 코칭은 filler" 불일치. 회귀 테스트로 description / measurement substring 검증 + 운영 dev 확인 게이트.
- **Phase 2a confidence_tone 정의 (raw `toneConfidenceLevel` / `emotionLabel` 흡수)** — 기존 enum 라벨 의존 표현을 관찰 어휘 (단정형 어미 / 음량 떨림) 로 재표현. 미흡 시 동일 패턴.
- **Phase 2a eye_contact_posture 정의 확장 (표정 + 시선·자세 raw 흡수)** — vision_analyzer LLM 호출 prompt 가 frame 직접 인지로 시선·자세·표정 채점 → dimension observation 으로 코칭 변환. 미흡 시 동일 패턴.
- **Phase 2a raw 측정치 산출 단계 부재 (★ 사용자 결정 2026-05-16)** — vocal raw 4종 + vision raw 5종 = 총 9개 키 prompt / 응답 / payload / DB / FE 어디에도 부재. analyzer LLM 호출 prompt 가 raw 자연어 산출 지시 미포함 (이전 갱신 = "Lambda 내부 채점 입력으로 유지" 표현은 본 갱신에서 폐기). 운영 디버깅 시 raw 신호 트레이스 = LLM 응답 원본 (CloudWatch JSON dump) + dimension observation 의존. TO-5 결정 참고.
- **Phase 2a composure 정의 제거** — 사용자 명시 결정. composure 신규 row 0건 → 과거 row 잔존 (백필 안 함). FE 표시에서 composure 카드 자연 부재.
- **Phase 2a BE+Lambda 묶음 PR 머지 동기화** — `@JsonIgnoreProperties(ignoreUnknown=true)` 호환 덕에 BE 선행 / Lambda 선행 / 묶음 모두 가능. 권장 = 묶음 PR (`feat(BE+Lambda): ...` 또는 분리 PR 동일 시점 머지). FE Phase 2b 는 별도 PR.
- **Phase 2b DeliveryTab 폐지 후 잔존 자산** — `LevelBadge / StructuredComment / format-feedback-level / isCommentBlockEmpty` 등 utility 참조 0 가능. Phase 2b 에서 검증 (`grep` 0 확인) 후 Phase 3 에서 일괄 삭제. Phase 2b 시점에 일부 잔존 가능 (Composition / FeedbackList 등 외부 참조 잔존 시).
- **Phase 3 DROP 후 운영 데이터 영구 손실** — 4컬럼 JSON 풍부 데이터 영구 삭제. product-spec 비스코프 A 명시 결정. 사용자 명시 재확인 게이트 권장 (Phase 3 진입 전).
- **부분 실패 graceful 회귀** — `isNonverbalCompleted=true` + `nonverbalFeedback=null` (전체) 또는 부분 dimensions = FE 폴링 종료 + Empty/부분 카드 표시. 무한 로딩 회귀 회피.

### 마이그레이션 전략

- **Phase 1**: 코드 변경만. DB 영향 0. 신규 row 부터 한국어 / verbatim 적용.
- **Phase 2a**: 코드 + rubric YAML 변경만. DB 컬럼 schema 영향 0. 신규 row 부터 3차원 + observation/evidence NOT NULL.
- **Phase 2b**: FE only. DB / API 영향 0.
- **Phase 3**: Flyway V{N} DDL `DROP COLUMN` × 4. DML 금지 (`conventions.md`). 과거 4컬럼 JSON 데이터 = 영구 손실 (product-spec 비스코프 A).

### 롤백 시나리오

- **Phase 1 revert** — `RubricScorerPromptBuilder` / template / `RubricScoringAdapter` 원상복구. 신규 row 다시 영어로 회귀. retry / 메트릭 코드 제거.
- **Phase 2a revert** — BE DTO (`NonverbalScore` 영역 키 분리 → 기존 4-int 필드) + persister (`saveRubric` 호출 → `saveNonverbal` 복원) + `QuestionScorePersister.saveNonverbal` 메서드 복원 + Lambda 페이로드 (영역 키 분리 → 기존 자유서술 적재) + rubric YAML (composure 복원) + `NonverbalRubricScorer` / `nonverbal_rubric_mapper.py` 파일 복원. DB 영향 0. 신규 row 다시 결정론 매퍼 + 자유서술 4종 적재.
- **Phase 2b revert** — FE 만 (DeliveryTab 파일 복원 + `feedback-panel.tsx` 탭 구조 복원 + `content-tab.tsx` 비언어 영역 제거 + types 원복). BE / Lambda 영향 0.
- **Phase 3 revert** — Flyway 역마이그레이션 (`ALTER TABLE timestamp_feedback ADD COLUMN nonverbal_comment TEXT, ADD COLUMN ...`). 과거 데이터 복구 불가 (DROP 시점 손실). entity / DTO / FE types 필드 복원.

---

## 분기 결정

- [x] **BE + Lambda + FE 3-way + Flyway** → `implement-be.md` + `implement-fe.md` (Lambda 는 BE 강결합 = `implement-be.md` 에 포함)
- **BE/Lambda 선행 강제 = O** (Phase 2a DTO breaking change → FE 신규 응답 의존).

### implement-be.md 범위

- **Phase 1 (BE only)**: `RubricScorerPromptBuilder.buildSystemPrompt()` 한국어 + `turn-rubric-scorer.txt` 정합 + `RubricScoringAdapter` dimension validation + retry 1회 (보강 prompt) + Micrometer counter + 신규 단일 validator (`RubricScorerResponseValidator`). Domain Unit + Service Integration + Infra Integration 테스트.
- **Phase 2a-Lambda**: `gemini_analyzer` 응답 스키마 + prompt 정비 + dimension 단위 validation + retry 1회. `vision_analyzer` 동일 + transcript 입력 추가. 단일 validator `dimension_validator.py` 신규 + 두 analyzer 공유. `handler._run_gemini_pipeline` + `_run_legacy_pipeline` 4종 자유서술 적재 제거 + **영역 키 분리 페이로드 조립** (`nonverbalScore.vocal.dimensions[]` + `nonverbalScore.vision.dimensions[]`) + `nonverbal_rubric_mapper.py` import / 호출 제거 + 파일 삭제. deploy 스크립트에 BE rubric YAML build-time copy 추가 (단일 출처). pytest (gemini / vision / handler / dimension_validator).
- **Phase 2a-BE**: `SaveFeedbackRequest.NonverbalScore` 재정의 (`{vocal?, vision?}` + `AreaScore` + `DimensionScoreItem` Bean Validation). `NonverbalScorePersister.persistOne` 재작성 (두 영역 키 머지 + 유효성 필터 + `saveRubric` 호출). **`QuestionScorePersister.saveNonverbal(...)` 메서드 삭제 (P0-2)** + 호출자 `saveRubric(... "nonverbal-v1" ...)` 갱신. `NonverbalRubricScorer` / `NonverbalContextWeightsLoader` (main 2) + 의존 test 3 파일 동일 PR 삭제 (orphan 확인). **`TimestampFeedbackResponse.nonverbalFeedback` 신설 (P0-3) + `from(feedback, questionScores, dimsByScoreId)` 시그니처 확장**. `QuestionSetFeedbackResponse.from` Map 타입 `Map<Long, List<QuestionScore>>` 로 변경 + `FeedbackService` group by questionId 갱신. `_dimensions.yaml` composure 삭제 + fluency / eye_contact_posture description 확장. `nonverbal-rubric.yaml` composure 참조 제거 + weight 재분배. ArchUnit orphan test (P1) 신규. Service Integration + Domain Unit + Infra Integration (validator).
- **Phase 3-BE**: Flyway `V{N}__drop_timestamp_feedback_freetext_and_raw_columns.sql` (DROP COLUMN × **10** — 자유서술 4 + entity 영속 raw 6: `speech_pace` / `tone_confidence_level` / `emotion_label` / `eye_contact_level` / `posture_level` / `expression_label`). `TimestampFeedback` entity 필드 (자유서술 4 + raw 6: `speechPace` / `toneConfidenceLevel` / `emotionLabel` / `eyeContactLevel` / `postureLevel` / `expressionLabel`) + `@Builder` param 제거. `SaveFeedbackRequest.TimestampFeedbackItem` 자유서술 4 `CommentBlock` 필드 + raw 필드 (구현 직전 grep 으로 실제 잔존 필드 확인 후 모두 제거) 제거. `CommentBlock` inner class 삭제. `TimestampFeedbackResponse.DeliveryFeedback / NonverbalFeedback / VocalFeedback / CommentBlock` inner class 잔존 grep 0 검증 후 삭제. `from(TimestampFeedback)` 정적 팩토리 정리. 필러 (`fillerWordCount`, `fillerWords`) 만 entity / DTO / Response 유지. Repository (Flyway smoke) + 회귀. **컬럼명 검증 (P0-A)**: 실제 entity `@Column(name=...)` 으로 raw 측정치 컬럼명 확인 후 DDL 작성. `speedVariance` / `gazeOnCameraRatio` / `postureUnstableCount` = entity 영속 부재 → DDL / entity 제거 대상 외 (DTO 잔존 시 동일 PR 정리).

### implement-fe.md 범위

- **Phase 2b**: `types/interview.ts` `TimestampFeedback.nonverbalFeedback` 신설 + `DeliveryFeedback.nonverbal` 필드 제거. `components/feedback/rubric-dimension-card.tsx` 신규 컴포넌트. `content-tab.tsx` props 확장 + verbal + 비언어 카드 그룹 + Empty / 부분 안내 분기. `feedback-panel.tsx` `Tabs` 제거 + 단일 `ContentTab` 렌더 + delivery 의존 line 정리. **필러 카운트 배지 (line 111-117) source = `feedback.fillerWordCount` top-level 로 변경 후 유지** (사용자 결정 #2). `delivery-tab.tsx` 파일 삭제. `__tests__/content-tab.test.tsx` + `rubric-dimension-card.test.tsx` + `feedback-panel.test.tsx` (필러 배지 회귀) + `interview-analysis-page.test.tsx` 또는 `use-question-sets.test.tsx` (P0-4 폴링 종료 회귀) 신규/갱신 (Vitest + RTL + msw).
- **Phase 3-FE**: `types/interview.ts` `DeliveryFeedback / NonverbalFeedback / VocalFeedback / CommentBlock` type 삭제 + `TimestampFeedback.delivery` 필드 제거. `isCommentBlockEmpty / StructuredComment / LevelBadge / format-feedback-level` 잔존 참조 grep 0 검증 후 미사용 파일 삭제. 회귀 (`npm run test` + `npm run lint` + `npm run build`).
