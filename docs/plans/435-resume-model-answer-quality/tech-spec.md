# Tech Spec — RESUME 트랙 model_answer 품질 개선

> **작성자**: backend agent (Staff Engineer 페르소나, create-tech-spec 스킬)
> **답하는 질문**: 어떻게? 구조 / 변경 파일 / Trade-off / 검증
> **승인 게이트**: ★ 사용자 명시 승인 후 implement.md 진입 ★

---

## Why → Goal (1줄 미러)

RESUME OPENER / PLAYGROUND / INTERROGATION 3 모드 model_answer 정의 갭 2축 (50~200자 분량 + 가이드 톤) → 200~300자 / 1인칭 답변 예시 톤 / 사용자 맥락 키워드 + 구조 단서 1+ 포함으로 재정렬. 측정 = Domain Unit 텍스트 단언 + Live LLM E2E 단언 + eval before/after 정량·정성 리포트.

## Evidence

### 현재 구조 (관련 파일 / 클래스)

- `backend/src/main/resources/prompts/template/resume/resume-playground-opener.txt:17,40` — model_answer 정의 "50~200자, 정답 X — '어떤 관점/구조로 풀어내면 좋은지' 만 안내" + 자가검증 라인.
- `backend/src/main/resources/prompts/template/resume/resume-playground-responder.txt:30,67` — 동일 정의 ("should_switch_to_interrogation 무관하게 항상 채워야 한다").
- `backend/src/main/resources/prompts/template/resume/resume-chain-interrogator.txt:28,75` — "현재 chain 의 해당 레벨 (L1 WHAT / L2 HOW / L3 WHY_MECH / L4 TRADEOFF) 답변 시 참고할 가이드라인 (50~200자, 정답 X)".
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeFallbackModelAnswers.java:5-10` — 3 상수 (OPENER / PLAYGROUND / INTERROGATION) 1줄 가이드 톤.
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeQuestionResultGenerator.java:38-94` — 3 모드 retry 1회 + blank 시 fallback 적용. 정상 출력 시 LLM 결과 그대로 적재.
- `backend/src/main/java/com/rehearse/api/infra/ai/prompt/ResumePlaygroundPromptBuilder.java`, `ResumeChainInterrogatorPromptBuilder.java` — 슬롯 직렬화 / 호출 변경 없음 (421 plan 머지 결과 = projectName 단일 라인).

### 기존 테스트 자산 (재사용)

- `backend/src/test/java/com/rehearse/api/infra/ai/prompt/ResumePlaygroundPromptTemplateTest.java` (421 plan 신설) — 케이스 추가만으로 본 spec 가드 가능.
- `backend/src/test/java/com/rehearse/api/domain/resume/service/ResumePlaygroundOpenerIntegrationTest.java` — 영향 없음 회귀.
- `backend/src/test/java/com/rehearse/api/e2e/ResumePlaygroundLiveLlmE2ETest.java` — `@Disabled` Live OpenAI. 단언 추가 대상 (421 패턴).
- `backend/eval/context/fixtures/session-resume-{1,2,3}.json` — 기존 3 fixture. 신규 2 추가로 총 5+ 케이스.

### 인접 plan

- `docs/plans/421-resume-playground-opener-tone/tech-spec.md` — 동일 RESUME 트랙 프롬프트 톤 변경. Template 리소스 단언 패턴 + Live LLM E2E `@Disabled` 부트스트랩 패턴 + Mock 통합 + Domain Unit 카테고리 매핑 검증된 가드 모델. 본 spec 재사용.

### 컨벤션

- `backend/.claude/rules/conventions.md` — Lombok / 한국어 로깅 / 트랜잭션 / Flyway DDL only.
- `backend/.claude/rules/testing.md` — Domain Unit (Spring 컨텍스트 X, 리소스 단언 OK) + Mock 자제 (외부 API 만) + Live `@Disabled` 패턴.

### 사용자 결정 (product-spec / 본 spec 작성 중)

- 분량 = 200~300자 (사용자 명시). 5~10문장은 너무 길다고 직접 수정 요청.
- 스코프 = RESUME 3 프롬프트 + fallback 상수만. 꼬리질문 / STANDARD = 별도 Issue.
- INTERROGATION 포함.
- 측정 = 정량 + 정성 eval.
- WRAP_UP 코드 삭제됨 → 비스코프.

### 추정 / 미확인 가정

- gpt-4o-mini primary 가 200~300자 일관 산출 가능. 미달 시 retry 1회 → fallback. 본 spec scope = 프롬프트 정의로 유도. 모델 변경은 eval 결과 후 별도 결정.
- LLM-as-judge 모델 = Claude Sonnet (BE fallback 모델 활용, cost ↓). 평가 신뢰도 = LLM 자체 한계 인정. Goal 70% 임계 = 추정.

## Trade-offs

### Option A (채택): 프롬프트 정의 + 자가검증 라인 + fallback 상수 동시 재작성

- 장점:
  - 정의 갭 2축 (분량 + 톤) 동시 해소. 정상 출력이 PM 의도와 일치.
  - 정의-fallback 일관성. fallback 적용 시도 동일 사용자 경험.
  - CI 결정성 = 프롬프트 텍스트 + 상수 단언 (LLM 출력 비결정 의존 X).
  - Live LLM E2E (`@Disabled`) 로 실 출력 톤 검증 옵션 보존.
- 단점:
  - 변경 폭 4 파일. fallback 은 정상 출력 시 미적용 (잉여 변경 의심) — 단 사용자 명시 결정.
  - LLM 출력 길이 ↑ → output token 비용 / latency mild ↑.
- 채택 사유: product-spec Goal = 정의-fallback 동시 정렬. 421 plan 동일 패턴 (Mock 통합 + Template 단언 + Live E2E 수동) 검증된 가드 모델 재활용.

### Option B (폐기): 분량만 늘리고 톤은 유지 ("50~200자" → "200~300자" 만)

- 장점: 변경 폭 최소 (3 라인).
- 단점: 톤 갭 잔존. "~을 설명해보세요" 패턴 유지 → AC "1인칭 답변 예시 톤" 미충족.
- 폐기 사유: product-spec AC 미충족.

### Option C (폐기): self-critique 단계 추가 (LLM 출력 후 본인 검증 + retry)

- 장점: 출력 품질 ↑ 가능성.
- 단점: 토큰 ×2. latency ×1.5. 추가 비용 큼.
- 폐기 사유: product-spec Goal = 정의 + 자가검증 라인 강화로 충분 추정. 비용 trade 부적절.

## Architecture

변경 없음 (호출 흐름 그대로). 4 touchpoint = 프롬프트 3 + 상수 1.

```
[ResumeQuestionResultGenerator] (backend/.../resume/service/ResumeQuestionResultGenerator.java)
   ├─ generateOpener
   │    → ResumePlaygroundPromptBuilder.buildOpener
   │        → SYSTEM = resume-playground-opener.txt   ★ model_answer 정의 + 자가검증 갱신
   │    → LLM → PlaygroundOpenerResult.modelAnswer (200~300자, 1인칭 답변 예시)
   │    → blank 시 → ResumeFallbackModelAnswers.OPENER   ★ 200~300자 재작성
   │
   ├─ generatePlaygroundResponder
   │    → ResumePlaygroundPromptBuilder.buildResponder
   │        → SYSTEM = resume-playground-responder.txt   ★ 정의 + 자가검증 갱신
   │    → LLM → PlaygroundResponderResult.modelAnswer
   │    → blank 시 → ResumeFallbackModelAnswers.PLAYGROUND   ★ 재작성
   │
   └─ generateInterrogation
        → ResumeChainInterrogatorPromptBuilder.build
            → SYSTEM = resume-chain-interrogator.txt   ★ 정의 + 레벨 (L1~L4) 깊이 차등 + 자가검증 갱신
        → LLM → InterrogationResult.modelAnswer
        → blank 시 → ResumeFallbackModelAnswers.INTERROGATION   ★ 재작성
```

## Data Model

변경 없음. Flyway 마이그레이션 X. Entity / DTO / 컬럼 영향 0.

## API Contract

변경 없음 — 외부 HTTP API 시그니처 영향 0. modelAnswer 응답 필드 길이만 확장 (50~200자 → 200~300자). JSON schema / 필드명 / nullability 동일. FE / lambda 0 영향.

## NF 커버리지

| NF | 결정 |
|---|---|
| 영향 범위 | BE only — 프롬프트 텍스트 3 + Java 상수 1 + 테스트 + eval 스크립트 |
| 정합성 | **N/A** — DB / 트랜잭션 / 이벤트 변경 0 |
| 동시성 | **N/A** — 동일 자원 동시 수정 변경 0 |
| 실시간성 | mild ↑ — output token 138~227 → 추정 300~500 (≈ 2배). user 직접 대기 단계. product-spec Non-Goals 정합 |
| 부하 | OpenAI output token 비용 mild ↑. 운영 영향 추산 = 위험2 |
| 마이그레이션 | DDL X / backfill X — 신규 인터뷰부터. 기존 row 그대로 |
| 외부 의존 | OpenAI primary + Claude fallback (기존 ResilientAiClient 그대로) |
| 보안 | OWASP Top 10 신규 위험 0 — 슬롯 / 입력 / 인증 경로 변경 X. SSRF (A10) / 인증 (A07) / 인가 (A01) 모두 **N/A** |
| 관찰성 | 기존 `modelAnswer 폴백 적용` warn 로그 그대로 + 배포 후 fallback 빈도 dev EC2 모니터링 (위험1 완화) + eval 정량·정성 리포트 |
| 롤백 | PR revert 단순 복구. feature flag 불필요. 호환성 깨짐 0 |
| 검증 | Domain Unit (Template + 상수 단언) + Service Integration (회귀 무변경) + Live LLM E2E (`@Disabled`) + eval before/after |

## 핵심 변경 4건

### 변경 1: `resume-playground-opener.txt`

**1-1. model_answer 정의 갱신** (line 17 출력 형식 + line 40 자가검증):

- 현재 (line 17): `"model_answer": "응시자가 답변할 때 참고할 수 있는 답변 가이드라인 (50~200자, 정답 X — '어떤 관점/구조로 풀어내면 좋은지' 만 안내)"`
- 변경 (line 17):
  ```
  "model_answer": "응시자가 답변으로 참고할 수 있는 1인칭 모범답변 예시 (200~300자, 3~5줄). 응시자 이력서의 projectName / 역할 / 기술 키워드를 명시하고, STAR (Situation·Task·Action·Result) / 결정 근거 / 트레이드오프 / 학습 중 1+ 구조 단서를 포함한다. 가이드 톤 ('~을 설명해보세요', '~을 이야기해보세요', '~하면 좋습니다') 금지 — 1인칭 답변 예시 톤 ('~했습니다', '~경험이 있습니다', '~을 진행했습니다') 만"
  ```

**1-2. 자가검증 라인 갱신** (line 40):

- 현재: `- \`model_answer\` 필드가 null, "", 공백만, 또는 누락되지 않았는지 확인. 50~200자 한국어 답변 가이드라인이어야 한다 (정답을 적지 말 것 — 어떤 관점/구조로 풀어내면 좋은지만)`
- 변경:
  ```
  - `model_answer` 필드가 null, "", 공백만, 또는 누락되지 않았는지 확인.
  - 길이가 200~300자 (한국어 기준) 인지 확인. 미달 / 초과 시 폐기 후 재작성.
  - 1인칭 답변 예시 톤인지 확인 ("~했습니다", "~경험이 있습니다" 류). 가이드 톤 ("~해보세요", "~하면 좋습니다") 발견 시 폐기 후 재작성.
  - PROJECT_INFO 의 projectName 이 있으면 model_answer 안에 명시 — 지시 표현 ("이 프로젝트", "해당 프로젝트") 단독 사용 금지.
  - STAR / 결정 근거 / 트레이드오프 / 학습 중 1+ 구조 단서가 포함됐는지 확인.
  ```

**1-3. safe-fallback question 텍스트** (line 42): 변경 없음 (별도 path — question 폴백, model_answer 와 무관).

### 변경 2: `resume-playground-responder.txt`

**2-1. model_answer 정의 갱신** (line 30):

- 현재: `"model_answer": "응시자가 답변할 때 참고할 수 있는 답변 가이드라인 (50~200자, 정답 X — 어떤 관점/구조로 풀어내면 좋은지만 안내)"`
- 변경: 변경 1-1 과 동일 정의 + Responder 1턴 맥락 ("응시자 직전 발화 핵심 명사 1+ 재인용 — 직전 발화에서 언급한 프로젝트 / 기술 / 경험 키워드를 model_answer 본문에 자연스럽게 포함") 추가.

**2-2. 자가검증 라인 갱신** (line 67):

- 변경 1-2 와 동일 5 항목 + 추가: `- model_answer 본문에 응시자 직전 발화 (USER_ANSWER) 의 핵심 명사 1+ 가 자연스럽게 인용됐는지 확인.`

### 변경 3: `resume-chain-interrogator.txt`

**3-1. model_answer 정의 갱신** (line 28):

- 현재: `"model_answer": "현재 chain 의 해당 레벨 (L1 WHAT / L2 HOW / L3 WHY_MECH / L4 TRADEOFF) 답변 시 참고할 가이드라인 (50~200자, 정답 X — 어떤 기술적 관점/구조로 답하면 좋은지만 안내)"`
- 변경:
  ```
  "model_answer": "현재 chain (CURRENT_CHAIN.topic) 의 현재 레벨 (CURRENT_LEVEL: L1 WHAT / L2 HOW / L3 WHY_MECH / L4 TRADEOFF) 에 맞는 1인칭 모범답변 예시 (200~300자, 3~5줄). chain topic 을 본문에 명시. 레벨별 깊이 차등: L1=수행 사실 + 사용 기술 / L2=구체 구현 단계 + 데이터 흐름 / L3=내부 메커니즘 + 선택 근거 / L4=대안 비교 + 트레이드오프 + 측정값. 가이드 톤 금지 — 1인칭 답변 예시 톤 ('~했습니다', '~을 적용했습니다', '~을 측정했습니다') 만"
  ```

**3-2. 자가검증 라인 갱신** (line 75):

- 변경 1-2 동일 5 항목 + 추가:
  ```
  - model_answer 본문에 CURRENT_CHAIN.topic 이 명시됐는지 확인.
  - CURRENT_LEVEL 에 맞는 깊이로 작성됐는지 확인 (L1 = WHAT / L2 = HOW 단계 / L3 = WHY 메커니즘 / L4 = 트레이드오프 비교).
  ```

### 변경 4: `ResumeFallbackModelAnswers.java`

**4-1. 3 상수 모두 200~300자 / 가이드 톤 재작성** (`backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeFallbackModelAnswers.java:5-10`):

- 설계 원칙: fallback = LLM blank 발동 → 사용자 컨텍스트 (projectName / 직전 발화 / chain topic) **모름**. 1인칭 placeholder ("저는 본인 프로젝트…") = 거짓 학습 자료 / 사용자 혼란 유발. 따라서 fallback 톤 = **답변 짤 단서 권유 (가이드)** 만. 1인칭 답변 예시 강화는 **LLM 정상 출력** 에서만 강제.

- 변경 명시 제약 (구체 텍스트 implement 단계):
  - 분량: 각 200~300자 (KO 기준).
  - 톤: 가이드 / 권유 ("~ 구조가 효과적입니다", "~ 정리하세요", "~ 마무리하면", "~ 서술하세요"). LLM 정상 출력에서 금지된 가이드 어구 ("~해보세요", "~하면" 등) = fallback 에서는 자연스러움 → 허용.
  - 1인칭 placeholder 금지: "저는 ", "본인이 직접 수행했습니다", "경험이 있습니다" 등 거짓 답변 어미 0건.
  - 구조 단서 1+: STAR / 결정 근거 / 트레이드오프 / 학습 중 1 (사용자가 자기 답변 짤 때 따라갈 구조 명시).
  - INTERROGATION 상수 = L1~L4 모든 레벨 공통 단일 placeholder fallback — 4 레벨 깊이 차이를 전부 가이드 형태로 짚음.

## Verification (완료 판정)

구현 완료 = 아래 모두 통과.

### product-spec AC ↔ Verification 매핑

| product-spec AC | 자동 가드 (CI) | 실 톤 가드 (수동 / Live) |
|---|---|---|
| AC1: OPENER 200~300자 / 1인칭 답변 예시 톤 / 가이드 톤 0건 | Template 리소스 단언 (정의 라인 / 자가검증 라인 단어 매치) + LLM-as-judge eval 분량·톤 메트릭 | Live E2E (`@Disabled`) — 실 출력 길이 200~300자 + 가이드 톤 어구 부재 + 1인칭 어미 1+ 매치 |
| AC2: OPENER 사용자 맥락 키워드 명시 (지시 표현 단독 0건) | Template 리소스 단언 (자가검증 라인에 "이 프로젝트 단독 금지" 문구 포함) | Live E2E — fixture projectName 문자열 포함 |
| AC3: PLAYGROUND 직전 발화 핵심 명사 1+ 재인용 | Template 리소스 단언 (responder 정의 라인에 "직전 발화 핵심 명사 1+ 재인용" 문구 포함) | Live E2E — fixture USER_ANSWER 의 핵심 명사 (예: "결제 시스템") 출력 포함 |
| AC4: INTERROGATION chain topic + 레벨 깊이 차등 | Template 리소스 단언 (정의 라인에 "L1=수행 사실" / "L4=트레이드오프" 차등 문구 포함) + 자가검증 라인 4 레벨 명시 | Live E2E — fixture chain topic 출력 포함 + 레벨별 어휘 (L4 = "트레이드오프" / "비교") 매치 |
| AC5: 모든 모드 구조 단서 1+ 포함 | Template 리소스 단언 (정의 라인에 "STAR / 결정 근거 / 트레이드오프 / 학습 중 1+" 문구 포함) | Live E2E — 출력에 ["STAR", "Situation", "Task", "Action", "Result", "결정", "근거", "트레이드오프", "학습"] 중 1+ 매치 |
| AC6: fallback 3 상수 200~300자 / 1인칭 톤 / 구조 단서 | `ResumeFallbackModelAnswersTest` 신규 — 3 상수 길이 200~300 자 + 가이드 톤 어구 0건 + 1인칭 어미 1+ + 구조 단서 어휘 1+ |  — |
| AC7: eval before/after 정량·정성 리포트 | eval 스크립트 산출 (분량 / 가이드 톤 어구 출현률 / 맥락 키워드 포함률 / 구조 단서 포함률 + LLM-as-judge 통과율) |  — |

자동 가드 = CI 결정성. 실 톤 가드 = 수동 (`@Disabled`, `OPENAI_API_KEY` 필요). product-spec Goal "정량 + 정성 eval" = CI 텍스트 단언 + eval 스크립트 + Live LLM E2E 3 채널.

### 신규 / 갱신 테스트

- [ ] **Domain Unit (갱신)**: `ResumePlaygroundPromptTemplateTest`
  - 신규 케이스 1: `opener_template_defines_model_answer_length_200_to_300` — `resume-playground-opener.txt` 로드 → "200~300자" / "3~5줄" / "1인칭 모범답변 예시" 키워드 모두 포함 단언.
  - 신규 케이스 2: `opener_template_forbids_guide_tone_in_model_answer` — 자가검증 라인에 ["~해보세요", "~하면 좋습니다", "가이드 톤 금지"] 키워드 포함 단언.
  - 신규 케이스 3: `opener_template_requires_structure_signal` — "STAR" / "결정 근거" / "트레이드오프" / "학습" 모두 정의 라인 포함 단언.
  - 신규 케이스 4: `opener_template_requires_project_context_keyword` — "projectName" + "지시 표현" + "단독" 키워드 자가검증 라인 포함 단언.
  - 신규 케이스 5~8: responder 동일 4 케이스 (5~8 = 5/6/7/8). 추가 케이스 9: `responder_template_requires_user_answer_keyword_reuse` — "직전 발화" / "핵심 명사" / "재인용" 키워드 정의 라인 포함 단언.
- [ ] **Domain Unit (신규)**: `ResumeChainInterrogatorPromptTemplateTest` (신규 클래스, `ResumePlaygroundPromptTemplateTest` 와 동일 패턴 — Spring 컨텍스트 X / 단순 리소스 로드 단언)
  - 케이스 1: `interrogator_template_defines_model_answer_length_200_to_300` — 동일 분량 단언.
  - 케이스 2: `interrogator_template_forbids_guide_tone` — 가이드 톤 금지 단언.
  - 케이스 3: `interrogator_template_specifies_level_depth_differentiation` — "L1=수행 사실" / "L2=구체 구현" / "L3=내부 메커니즘" / "L4=트레이드오프" 4 레벨 깊이 키워드 모두 포함 단언.
  - 케이스 4: `interrogator_template_requires_chain_topic_mention` — 자가검증 라인에 "CURRENT_CHAIN.topic" / "CURRENT_LEVEL" 키워드 포함 단언.
- [ ] **Domain Unit (신규)**: `ResumeFallbackModelAnswersTest` (신규 클래스, `DomainUnitSupport` 미사용 — 단순 상수 검증 / 환경 셋업 0)
  - 케이스 1: `opener_fallback_length_within_200_to_300_chars` — `ResumeFallbackModelAnswers.OPENER.length()` 가 200~300 범위 단언.
  - 케이스 2: `playground_fallback_length_within_200_to_300_chars` — 동일.
  - 케이스 3: `interrogation_fallback_length_within_200_to_300_chars` — 동일.
  - 케이스 4: `all_fallbacks_use_first_person_answer_tone` — 3 상수 모두 ["했습니다", "경험이 있습니다", "을 진행했습니다", "을 적용했습니다", "을 측정했습니다"] 중 1+ 매치 단언.
  - 케이스 5: `all_fallbacks_forbid_guide_tone_phrases` — 3 상수 모두 ["해보세요", "하면 좋습니다", "이야기해보세요", "설명해보세요"] 어구 부재 단언.
  - 케이스 6: `all_fallbacks_include_structure_signal` — 3 상수 모두 ["STAR", "Situation", "Task", "Action", "Result", "결정", "근거", "트레이드오프", "학습"] 중 1+ 매치.
- [ ] **Service Integration (회귀, 영향 없음)**: `ResumePlaygroundOpenerIntegrationTest` — 기존 슬롯 단언 (PROJECT_INFO = projectName 단일 라인) 그대로 통과 확인. 추가 단언 X (modelAnswer 응답 길이는 LLM 비결정 → Domain Unit / Live 가드). 결정 근거: 421 plan 에서 PROJECT_INFO 슬롯 단언 충분 강화 (`doesNotContain("projectId:" / "claims:" / "implicitCsTopics:")` 추가) → 본 spec 추가 무변경.
- [ ] **Live LLM E2E (수동, `@Disabled`)**: `ResumePlaygroundLiveLlmE2ETest`
  - 기존 케이스 (`buildOpener_returns_non_blank_question_from_live_openai`) 단언 추가:
    - 추가 단언 1 (AC1 분량): `result.modelAnswer().length()` 200~300 범위 (`±30` 허용 — LLM 비결정 완충).
    - 추가 단언 2 (AC1 톤): `result.modelAnswer()` 가 ["했습니다", "경험이 있습니다", "을 적용했습니다"] 중 1+ 매치.
    - 추가 단언 3 (AC1 가이드 톤 부재): `result.modelAnswer()` 에 ["해보세요", "하면 좋습니다", "이야기해보세요"] 부재.
    - 추가 단언 4 (AC2): fixture projectName ("Live 테스트 프로젝트") 출력 포함.
    - 추가 단언 5 (AC5): ["STAR", "Situation", "결정", "근거", "트레이드오프", "학습"] 중 1+ 매치.
  - 신규 케이스 `buildResponder_returns_first_person_model_answer_from_live_openai` (AC3 / AC1 / AC5):
    - fixture USER_ANSWER (예: "결제 시스템에서 동시성 문제를 해결했습니다") 의 핵심 명사 1+ ["결제 시스템", "동시성"] 출력 포함.
    - 위 단언 1~3 / 5 동일.
  - 활성화 = `@Disabled` + `OPENAI_API_KEY` + `-Djunit.jupiter.conditions.deactivate=org.junit.*DisabledCondition` (421 plan 패턴).
- [ ] **Live LLM E2E (수동, `@Disabled`, 신규 클래스)**: `ResumeChainInterrogatorLiveLlmE2ETest` — 부트스트랩 패턴 = `ResumePlaygroundLiveLlmE2ETest` 동일 (Spring 컨텍스트 / 의존성 주입 / `@Disabled` + `OPENAI_API_KEY` + JUnit deactivate flag). `E2ESupport` 미사용 — Live OpenAI 직접 호출 단일 책임. 클래스 헤더 javadoc 활성화 명령 명시 (421 plan 동일).
  - 케이스 `build_returns_level_appropriate_model_answer_for_l4` — fixture chain topic + L4 입력 → 출력에 chain topic 문자열 포함 + ["트레이드오프", "비교", "측정"] 중 1+ 매치 + 위 단언 1~3 / 5 동일.

### eval before/after 리포트

- [ ] **eval 스크립트 신규**: `backend/eval/context/run_model_answer_quality.py` 또는 동등 위치.
  - 입력 = `backend/eval/context/fixtures/session-resume-{1..5}.json` (기존 3 + 신규 2 — RESUME OPENER 1 + INTERROGATION L3 1).
  - 처리 = before (현 프롬프트) / after (변경 프롬프트) 각 5 케이스 × 3 모드 LLM 호출 → modelAnswer 수집.
  - 정량 메트릭:
    - 분량 = 200~300자 (한국어 기준) 비율.
    - 가이드 톤 어구 (["해보세요", "하면 좋습니다", "이야기해보세요"]) 출현률 (목표 0%).
    - 사용자 맥락 키워드 (projectName / chain topic / USER_ANSWER 핵심 명사) 포함률 (목표 ≥80%).
    - 구조 단서 (["STAR", "Situation", "Task", "Action", "Result", "결정", "근거", "트레이드오프", "학습"]) 포함률 (목표 ≥80%).
  - 정성 메트릭 = LLM-as-judge (Claude Sonnet) — rubric per-criteria × 3등급 (잘 / 보통 / 나쁨). criteria = (a) 분량 적절성 (b) 1인칭 톤 일관성 (c) 사용자 맥락 반영 (d) 구조 단서 포함. 통과율 = 잘+보통 합산 ≥70%.
  - 산출 = 마크다운 리포트 (`backend/eval/context/reports/model-answer-quality-{YYYYMMDD}.md`) — before/after 표 + 케이스별 출력 샘플.
- [ ] eval 스크립트 1회 실행 + 리포트 생성 + Goal 임계 (분량 ≥80% / 가이드 톤 0% / 맥락 ≥80% / 구조 ≥80% / judge 통과율 ≥70%) 통과 확인.

### 회귀 / 빌드

- [ ] `./gradlew test --tests "*Resume*"` 통과 (resume 도메인 + infra/ai/prompt + e2e Live 제외).
- [ ] `./gradlew test --tests "*ResumeFallbackModelAnswers*"` 통과.
- [ ] `./gradlew test --tests "*PromptTemplate*"` 통과.
- [ ] `./gradlew build` 통과.
- [ ] 관찰: 기존 `[ResumeQuestionResultGenerator] modelAnswer 폴백 적용` warn 로그 변경 없음 (호출 패턴 그대로).

## Pre / Post State

### Pre (현재)

- `resume-playground-opener.txt:17` — model_answer 정의 "50~200자, 어떤 관점/구조로 풀어내면 좋은지만 안내".
- `resume-playground-opener.txt:40` — 자가검증 = "50~200자 가이드라인 / 정답 X" 만.
- `resume-playground-responder.txt:30,67` — 동일 50~200자 가이드 정의.
- `resume-chain-interrogator.txt:28,75` — "50~200자 가이드라인 / L1~L4 깊이 명시 부재".
- `ResumeFallbackModelAnswers.java:5-10` — 3 상수 = 1줄 가이드 톤 ("~을 풀어보세요", "~을 강조하면 좋습니다").
- 테스트: `ResumePlaygroundPromptTemplateTest` = 기존 케이스 (421 plan 결과). chain interrogator template 단언 클래스 부재. fallback 단언 클래스 부재. Live E2E modelAnswer 단언 부재.
- eval: `model-answer-quality` 리포트 스크립트 부재.

### Post (구현 후)

- `resume-playground-opener.txt:17,40` — model_answer 정의 = "200~300자 1인칭 답변 예시 + projectName 명시 + 구조 단서 1+". 자가검증 = 5 항목 (길이 / 톤 / projectName / 지시 표현 / 구조 단서).
- `resume-playground-responder.txt:30,67` — 동일 정의 + Responder 1턴 USER_ANSWER 핵심 명사 재인용 추가.
- `resume-chain-interrogator.txt:28,75` — 정의 + L1~L4 깊이 차등 (수행 사실 / 구체 구현 / 내부 메커니즘 / 트레이드오프) + chain topic 명시 자가검증.
- `ResumeFallbackModelAnswers.java` — 3 상수 = 200~300자 / 1인칭 답변 예시 / 구조 단서 1+.
- 테스트: `ResumePlaygroundPromptTemplateTest` 9 신규 케이스 + `ResumeChainInterrogatorPromptTemplateTest` 4 케이스 (신규 클래스) + `ResumeFallbackModelAnswersTest` 6 케이스 (신규 클래스) + `ResumePlaygroundLiveLlmE2ETest` 단언 5 추가 + 신규 케이스 1 + `ResumeChainInterrogatorLiveLlmE2ETest` 1 케이스.
- eval: `backend/eval/context/run_model_answer_quality.py` 스크립트 + `reports/model-answer-quality-{YYYYMMDD}.md` 리포트.

## 위험 / 마이그레이션 / 롤백

### 위험

- **위험 1**: gpt-4o-mini primary 가 200~300자 일관 산출 못할 가능성 (모델 한계). retry 1회 후 fallback 빈도 ↑ → 사용자 경험 저하.
  - 완화 1: eval before/after 리포트로 분량 적합 비율 측정. ≥80% 미달 시 모델 변경 (gpt-4o full / Claude Sonnet) 별도 결정. 본 spec scope = 프롬프트 정의 강화 + fallback 일관성 (모델 변경은 별도 PR).
  - 완화 2: 배포 후 N=7일간 dev EC2 docker log 의 `[ResumeQuestionResultGenerator] modelAnswer 폴백 적용` warn 빈도 모니터링. baseline = 0건 (배포 직전 측정). 임계 = 일평균 5건 초과 시 즉시 모델 변경 PR 트리거 + 사용자 보고. 자동 대시보드 / alarm 부재 → 수동 grep (`docker logs rehearse-backend | grep "modelAnswer 폴백"`) 운영자 책임.
- **위험 2**: output token 비용 / latency mild ↑ (현 138~227 tokens → 추정 300~500 tokens, 약 2배). 사용자 직접 대기 단계.
  - 완화: product-spec Non-Goals 명시 (latency / 비용 단축 추구 X). 사용자 결정 = 학습 가치 우선.
  - 비용 추산 (정성):
    - eval 1회 실행 = 5 fixture × 3 mode × 2 (before/after) × LLM 1 호출 + judge 1 호출 = 60 호출. gpt-4o-mini ≈ $0.001~0.003 / call → eval 1회 ≈ $0.06~0.18. judge Claude Sonnet ≈ $0.005~0.01 / call → eval 1회 ≈ $0.30~0.60. 합계 ≈ $0.4~0.8 / 회. 수용 가능.
    - 운영 영향 = RESUME 인터뷰 / 월 N건 × 평균 m턴 × 3 mode × output token 2배. 절대 수치는 운영 데이터 부재 → 배포 후 1주 측정 보고. baseline 미달 시 사용자 비용 결정 게이트.
- **위험 3**: LLM 이 model_answer 본문에 사용자 입력 (USER_ANSWER) 키워드를 그대로 인용 → prompt injection 흔적 노출 가능성. 단 본 출력은 modelAnswer (FE 모범답변 표시) → 사용자 자신의 입력이 자기에게 보이는 형태 = 보안 영향 X.
  - 완화: OWASP A03 (Injection) 신규 위험 X — 슬롯 / 입력 변경 없음. 검증 X.
- **위험 4**: LLM-as-judge (Claude Sonnet) 자체 평가 신뢰도 한계 — 70% 임계 추정. judge 가 false positive / negative 가능.
  - 완화: 정량 메트릭 4종 + judge 1종 = 5축 평가. judge 단독 fail 시 정량 메트릭 통과 여부로 보완 판단. 사용자 명시 합의 (정량 + 정성 결정).
  - judge 통과율 70% 미달 시 후속 분기:
    - (a) **rubric 재조정** — criteria 4종 (분량 / 톤 / 맥락 / 구조) 중 false negative 다수 항목 식별 후 등급 임계 완화 (예: "잘+보통+나쁨 일부" 합산). 정량 메트릭 4종 통과 시 우선 검토.
    - (b) **judge 모델 변경** — Claude Sonnet → Opus 또는 GPT-4o full. 비용 ↑ trade.
    - (c) **별도 plan** — 본 spec 머지 차단. judge 합의 미성립 = 품질 정의 자체 재논의 필요. 사용자 결정.

### 마이그레이션 전략

- 신규 인터뷰부터 적용. backfill X (product-spec 비스코프).
- 기존 인터뷰 question 테이블 model_answer 컬럼 = 그대로 유지. 사용자 재조회 시 과거 데이터는 50~200자 가이드 톤 그대로 노출. 운영 데이터 변경 X.
- DDL X. Flyway 마이그레이션 X.

### 롤백 시나리오

- PR revert. 4 파일 (프롬프트 3 + 상수 1) 단순 revert 로 즉시 복구.
- 테스트 신규 클래스 (3) + 케이스 추가 = revert 시 함께 제거.
- eval 스크립트 + 리포트 = 별도 파일 (런타임 영향 X). revert 시 보존 가능 (히스토리 자료).
- feature flag 불필요 — LLM 출력 형태 변경만, 호환성 깨짐 0.

## 분기 결정

- [x] **단일 영역 (BE) → `implement.md` 1개**
- [ ] BE+FE 동시
- [ ] BE 선행 강제

근거: 변경 = BE 프롬프트 텍스트 + Java 상수 + 테스트 + eval 스크립트. FE / lambda / 외부 API contract 영향 0. modelAnswer 응답 필드 nullability / schema / 필드명 동일, 길이만 확장 (FE 표시 영향 = UX 별도 plan, 본 spec Non-Goals 명시).
