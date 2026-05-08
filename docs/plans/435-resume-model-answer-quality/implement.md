# Implement — RESUME 트랙 model_answer 품질 개선

> **작성자**: Staff Engineer (create-implement-plan 스킬)
> **답하는 질문**: 어떤 순서로 실행?
> **사용 시점**: BE only 단일 영역. tech-spec.md 분기 결정 = `implement.md` 1개.
> **승인 게이트**: ★ 사용자 명시 승인 후 코드 작성 ★
> **후속**: 본 plan 머지 후 fast-follow plan = STANDARD 트랙 (`question-generation.txt` CS / 언어프레임워크 / 행동) + 꼬리질문 (`follow-up-concept.txt` / `follow-up-experience.txt`) 동일 가드 패턴 재사용 (별도 Issue / spec 폴더).

---

## Phase / Step 개요

| Phase | 제목 | 구현 에이전트 | 예상 PR | 의존 |
|-------|------|--------------|--------|------|
| 1 | RESUME 3 프롬프트 정의 + 자가검증 갱신 + Template Domain Unit | `backend` | PR 단일 | - |
| 2 | `ResumeFallbackModelAnswers` 3 상수 재작성 + Domain Unit 신규 | `backend` | PR 단일 (Phase 1 합본) | - (Phase 1 병렬 가능) |
| 3 | Live LLM E2E 단언 / 신규 + eval 스크립트 + before/after 리포트 | `backend` | PR 단일 (Phase 1+2 합본) | Phase 1 + 2 |

> Task 3개 / 본문 < 50줄 each → `tasks/` 분리 X. 단일 implement.md.
> 단일 PR 추정 — 4 파일 변경 + 테스트 묶음. Phase 1+2 병렬 가능, Phase 3 = Live + eval 산출물.

---

## Phase 1: RESUME 3 프롬프트 정의 + 자가검증 갱신 + Template Domain Unit

- **구현**: `backend` — RESUME OPENER / PLAYGROUND / INTERROGATION 3 프롬프트 텍스트 갱신 + Template 리소스 단언 가드 추가.

### 변경 파일

- `backend/src/main/resources/prompts/template/resume/resume-playground-opener.txt` — line 17 (model_answer 정의) + line 40 (자가검증 라인) 갱신. 200~300자 / 1인칭 답변 예시 톤 / projectName 명시 / 구조 단서 1+ / 가이드 톤 금지.
- `backend/src/main/resources/prompts/template/resume/resume-playground-responder.txt` — line 30 + line 67 동일 패턴 + Responder 1턴 USER_ANSWER 핵심 명사 재인용 추가.
- `backend/src/main/resources/prompts/template/resume/resume-chain-interrogator.txt` — line 28 + line 75 동일 패턴 + L1~L4 레벨 깊이 차등 (수행 사실 / 구체 구현 / 내부 메커니즘 / 트레이드오프) + chain topic 명시 자가검증.
- `backend/src/test/java/com/rehearse/api/infra/ai/prompt/ResumePlaygroundPromptTemplateTest.java` — 신규 케이스 9개 (opener 4 + responder 5). 분량 / 가이드 톤 금지 / 구조 단서 / projectName / USER_ANSWER 재인용 키워드 단언.
- `backend/src/test/java/com/rehearse/api/infra/ai/prompt/ResumeChainInterrogatorPromptTemplateTest.java` — **신규 클래스** (421 plan `ResumePlaygroundPromptTemplateTest` 동일 패턴 — Spring 컨텍스트 X / 단순 리소스 로드 단언 / `DomainUnitSupport` 미사용). 케이스 4개.

### 핵심 로직 / 변경 요약

- 프롬프트 3 파일 model_answer 정의 라인 (`"model_answer": "..."`) 교체:
  - 분량 = "200~300자, 3~5줄"
  - 톤 = "1인칭 모범답변 예시 ('~했습니다', '~경험이 있습니다')". 가이드 톤 ("~해보세요", "~하면 좋습니다") 금지 명시.
  - 구조 단서 = "STAR (Situation·Task·Action·Result) / 결정 근거 / 트레이드오프 / 학습 중 1+".
  - 맥락 키워드:
    - opener = projectName / 역할 / 기술 명시. 지시 표현 ("이 프로젝트") 단독 금지.
    - responder = USER_ANSWER 핵심 명사 1+ 재인용.
    - interrogator = CURRENT_CHAIN.topic 명시 + CURRENT_LEVEL (L1~L4) 깊이 차등.
- 자가검증 라인 = 5 항목 체크리스트 (길이 / 톤 / projectName 또는 chain topic / 구조 단서 / 지시 표현 단독 금지). responder = +1 (USER_ANSWER 재인용). interrogator = +2 (chain topic / level 깊이).
- Template Test 패턴:
  - 리소스 로드 (`new ClassPathResource("prompts/template/resume/...").getInputStream()` 으로 읽기 또는 `Files.readString` 활용 — 421 plan 기존 패턴 재사용).
  - `assertThat(content).contains("200~300자")` / `contains("1인칭")` / `contains("STAR")` / `contains("trade-off 어휘")` / `doesNotContain("50~200자")` (regression).
  - `@DisplayName` 한국어. `@Nested` "정의 라인" / "자가검증 라인" 그룹.

### 의존

- 선행 phase: 없음
- 외부 의존: 없음 (BE 리소스 + 테스트 자원만)

### Verification Hook

- 명령:
  - `./gradlew test --tests "com.rehearse.api.infra.ai.prompt.ResumePlaygroundPromptTemplateTest"`
  - `./gradlew test --tests "com.rehearse.api.infra.ai.prompt.ResumeChainInterrogatorPromptTemplateTest"`
- 통과 기준: 신규 13 케이스 (9 + 4) 모두 green. 기존 케이스 회귀 0건.
- 관찰 가능 동작: 리소스 파일 grep — `grep -n "200~300자" backend/src/main/resources/prompts/template/resume/resume-*.txt` = 3 매치.

### 커밋 메시지 (예상)

```
feat(BE): RESUME 프롬프트 model_answer 정의 200~300자/1인칭 톤 재정렬
```

---

## Phase 2: `ResumeFallbackModelAnswers` 3 상수 재작성 + Domain Unit 신규

- **구현**: `backend` — fallback 상수 3개 재작성 (정의 변경과 일관 톤). 상수 단언 클래스 신규.

### 변경 파일

- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeFallbackModelAnswers.java` — `OPENER` / `PLAYGROUND` / `INTERROGATION` 3 상수 재작성. 각 200~300자 / 1인칭 답변 예시 톤 / 구조 단서 1+ / 가이드 톤 어구 0건. INTERROGATION = L1~L4 placeholder 어휘 (chain topic / level 미지정 케이스 cover).
- `backend/src/test/java/com/rehearse/api/domain/resume/service/ResumeFallbackModelAnswersTest.java` — **신규 클래스**. `DomainUnitSupport` 미사용 (단순 상수 검증 / 환경 셋업 0). 케이스 6개.

### 핵심 로직 / 변경 요약

- 3 상수 텍스트 = tech-spec 변경 4 (`Architecture` 섹션) 제약 그대로:
  - 분량 200~300자 (KO 기준).
  - 톤 1인칭 답변 예시. 어미 = ["했습니다", "경험이 있습니다", "을 진행했습니다", "을 적용했습니다", "을 측정했습니다"] 중 1+.
  - 가이드 톤 어구 0건 = ["해보세요", "하면 좋습니다", "이야기해보세요", "설명해보세요"] 부재.
  - 구조 단서 1+ = ["STAR", "Situation", "Task", "Action", "Result", "결정", "근거", "트레이드오프", "학습"] 중 1+.
  - placeholder 어휘 사용 가능 (응시자 / 본인 프로젝트). chain topic / level 미지정 fallback 케이스.
- 텍스트 작성 시 fallback 발동 조건 (LLM 출력 blank → 상수 적용) 고려 — 응시자 입력 슬롯 (projectName / USER_ANSWER) 미포함 placeholder 톤.
- Test 패턴:
  - `@Nested` 시나리오 그룹 (분량 / 톤 / 가이드 톤 부재 / 구조 단서).
  - `assertThat(ResumeFallbackModelAnswers.OPENER.length()).isBetween(200, 300)`.
  - `assertThat(constant).containsAnyOf("했습니다", "경험이 있습니다", ...)` 1인칭 어미.
  - `assertThat(constant).doesNotContainAnyOf("해보세요", ...)` 가이드 톤 부재.
  - 모든 케이스 `@DisplayName` 한국어.

### 의존

- 선행 phase: 없음 (Phase 1 와 병렬 가능 — 동일 PR 묶기)
- 외부 의존: 없음

### Verification Hook

- 명령: `./gradlew test --tests "com.rehearse.api.domain.resume.service.ResumeFallbackModelAnswersTest"`
- 통과 기준: 신규 6 케이스 모두 green.
- 관찰 가능 동작: `wc -m` Java 파일 상수 텍스트 = 각 200~300 범위.

### 커밋 메시지 (예상)

```
feat(BE): ResumeFallbackModelAnswers 200~300자 1인칭 답변 예시 재작성
```

---

## Phase 3: Live LLM E2E 단언 / 신규 + eval 스크립트 + before/after 리포트

- **구현**: `backend` — Live OpenAI 실 출력 톤 가드 + eval 정량·정성 리포트 산출.

### 변경 파일

- `backend/src/test/java/com/rehearse/api/e2e/ResumePlaygroundLiveLlmE2ETest.java` — 기존 케이스 단언 5 추가 (분량 / 1인칭 톤 / 가이드 톤 부재 / projectName / 구조 단서). 신규 케이스 1 (`buildResponder_returns_first_person_model_answer_from_live_openai`).
- `backend/src/test/java/com/rehearse/api/e2e/ResumeChainInterrogatorLiveLlmE2ETest.java` — **신규 클래스**. 부트스트랩 = `ResumePlaygroundLiveLlmE2ETest` 동일 패턴 (`@Disabled` + `OPENAI_API_KEY` + JUnit deactivate flag). 케이스 1 (`build_returns_level_appropriate_model_answer_for_l4`).
- `backend/eval/context/run_model_answer_quality.py` — **신규**. before/after 5 fixture × 3 mode × 2 단계 LLM 호출 + judge 정성 평가 + 마크다운 리포트 산출.
- `backend/eval/context/fixtures/session-resume-4.json` — **신규 fixture**. RESUME OPENER 케이스 (projectName / 역할 / 기술 명시 fixture).
- `backend/eval/context/fixtures/session-resume-5.json` — **신규 fixture**. RESUME INTERROGATION L3 케이스 (chain topic + L3 WHY_MECH 입력).
- `backend/eval/context/reports/model-answer-quality-{YYYYMMDD}.md` — eval 1회 실행 산출 (커밋 포함).

### 핵심 로직 / 변경 요약

- Live E2E 단언 추가 (421 plan 동일 패턴):
  - `assertThat(result.modelAnswer().length()).isBetween(170, 330)` (LLM 비결정 ±30 완충).
  - `assertThat(result.modelAnswer()).containsAnyOf("했습니다", "경험이 있습니다", "을 적용했습니다")`.
  - `assertThat(result.modelAnswer()).doesNotContainAnyOf("해보세요", "하면 좋습니다", "이야기해보세요")`.
  - opener fixture projectName ("Live 테스트 프로젝트") 출력 포함 단언.
  - 구조 단서 어휘 1+ 매치 단언.
- responder 신규 케이스 = USER_ANSWER 핵심 명사 재인용 단언.
- interrogator 신규 케이스 = chain topic 출력 포함 + L4 어휘 (["트레이드오프", "비교", "측정"] 중 1+) 매치.
- `@Disabled` + 활성화 명령 클래스 헤더 javadoc 명시 (421 plan 패턴 그대로):
  ```
  @EnabledIfEnvironmentVariable(name="OPENAI_API_KEY", matches=".+")
  ./gradlew test --tests "ResumeChainInterrogatorLiveLlmE2ETest" \
    -Djunit.jupiter.conditions.deactivate=org.junit.*DisabledCondition
  ```
- eval 스크립트 (`run_model_answer_quality.py`):
  - 입력 = `session-resume-{1..5}.json` 5 fixture.
  - 처리 = before (현 prompt — Phase 1+2 머지 전 git checkout) / after (변경 prompt) 각 5 × 3 mode × LLM 1 호출 + judge 1 호출 = 60 호출.
  - 정량 메트릭 4종 = 분량 200~300자 비율 / 가이드 톤 어구 출현률 / 맥락 키워드 (projectName / chain topic / USER_ANSWER 핵심 명사) 포함률 / 구조 단서 포함률.
  - 정성 메트릭 = LLM-as-judge (Claude Sonnet) rubric per-criteria × 3등급. criteria 4종 (분량 / 톤 / 맥락 / 구조). 통과율 = 잘+보통 합산 ≥70%.
  - 산출 = `reports/model-answer-quality-{YYYYMMDD}.md` 마크다운 — before/after 표 + 케이스별 출력 샘플.
- 신규 fixture 2:
  - `session-resume-4.json` = OPENER 입력 (projectName "결제 시스템 리뉴얼" / 역할 "백엔드 리드" / 기술 "Spring Boot, Redis, Kafka").
  - `session-resume-5.json` = INTERROGATION L3 입력 (chain topic "동시성 제어" / level L3_WHY_MECH / USER_ANSWER fixture).

### 의존

- 선행 phase: Phase 1 + 2 (프롬프트 + fallback 변경 머지 후 after 측정 가능).
- 외부 의존:
  - `OPENAI_API_KEY` (Live E2E + eval before/after 호출).
  - `ANTHROPIC_API_KEY` (judge Claude Sonnet 호출).
  - eval 1회 실행 비용 ≈ $0.4~0.8 (tech-spec 위험 2 추산).

### Verification Hook

- 명령 (CI):
  - `./gradlew test --tests "*Resume*"` (Live `@Disabled` 자동 skip — CI 통과).
- 명령 (수동 / Live):
  - `./gradlew test --tests "ResumePlaygroundLiveLlmE2ETest" -Djunit.jupiter.conditions.deactivate=org.junit.*DisabledCondition`
  - `./gradlew test --tests "ResumeChainInterrogatorLiveLlmE2ETest" -Djunit.jupiter.conditions.deactivate=org.junit.*DisabledCondition`
- 명령 (eval):
  - `python3 backend/eval/context/run_model_answer_quality.py`
- 통과 기준:
  - Live E2E (수동) 신규 단언 모두 green.
  - eval 리포트 정량 임계 = 분량 ≥80% / 가이드 톤 0% / 맥락 ≥80% / 구조 ≥80% / judge 통과율 ≥70%.
  - 임계 미달 시 tech-spec 위험 4 분기 적용 (rubric 재조정 / judge 모델 변경 / 별도 plan).
- 관찰 가능 동작:
  - 배포 후 dev EC2 docker log `[ResumeQuestionResultGenerator] modelAnswer 폴백 적용` warn 빈도 모니터링 (baseline 0건 / 임계 일평균 5건).
  - 명령: `docker logs rehearse-backend | grep "modelAnswer 폴백" | wc -l` (운영자 수동 grep).

### 커밋 메시지 (예상)

```
test(BE): RESUME model_answer Live LLM E2E 단언 + eval 리포트 산출
```

(eval 산출 리포트 별도 커밋 옵션:)
```
docs(BE): RESUME model_answer eval before/after 리포트 추가
```

---

## 통합 Verification

- [ ] tech-spec.md Verification 항목 모두 통과 (`docs/plans/435-resume-model-answer-quality/tech-spec.md` 참조)
- [ ] `./gradlew test --tests "*Resume*"` 통과 (Live `@Disabled` 자동 skip)
- [ ] `./gradlew test --tests "*PromptTemplate*"` 통과
- [ ] `./gradlew test --tests "*ResumeFallbackModelAnswers*"` 통과
- [ ] `./gradlew build` 통과
- [ ] Live LLM E2E 수동 실행 단언 통과 (`OPENAI_API_KEY` 환경)
- [ ] eval 리포트 정량 임계 (분량 ≥80% / 가이드 톤 0% / 맥락 ≥80% / 구조 ≥80%) + 정성 (judge 통과율 ≥70%) 통과
- [ ] 배포 후 N=7일 dev EC2 fallback warn 빈도 baseline 0 / 임계 일평균 5건 미만 관찰
- [ ] 회귀 = `ResumePlaygroundOpenerIntegrationTest` 기존 슬롯 단언 무변경 통과

## 리뷰 게이트 (MANDATORY)

구현 완료 직후 지정 리뷰어 실행 강제. 스킵 = 위반.

- [ ] 지정 리뷰어 실행 (구현 완료 직후 — 메인 세션 책임)
  - BE only → `code-reviewer-backend`
- [ ] 컨벤션 위반 0건 (`backend/.claude/rules/conventions.md` + `backend/.claude/rules/testing.md`)
- [ ] Critical / Major 지적 = fix 반영 후 재리뷰
- [ ] Pre/Post State diff tech-spec.md 일치
