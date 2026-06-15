# Product Spec — RESUME 트랙 model_answer 품질 개선

> **작성자**: 사용자 (PM 자율 분석 초안)
> **답하는 질문**: 왜? 무엇? 수용기준?
> **다음 단계**: 사용자 승인 → 구현 agent 가 `tech-spec.md` 작성

---

## 문제 상황 (Problem)

- 현재 상태: RESUME 트랙 OPENER / PLAYGROUND / INTERROGATION 3 모드 모두 LLM 이 `model_answer` (모범답변) 필드를 반환. 정의 = "답변 가이드라인 (50~200자, 정답 X — 어떤 관점/구조로 풀어내면 좋은지만)". 정상 출력 시 1~2문장 가이드 톤 ("~을 설명해보세요").
- 발생 증상: dev 서버 interview id=26, q147~152 (2026-05-07). model_answer 가 짧고 성의 없음. 일반 가이드 톤. 사용자 fallback 의심할 정도.
  - q147 OPENER `model_answer`: "프로젝트에서 맡은 역할과 그에 따른 주요 작업, 팀 내에서의 협업 경험 등을 중심으로 설명하면 좋습니다."
  - q148 PLAYGROUND `model_answer`: "프로젝트에서 인상 깊었던 순간이나 도전 과제를 이야기해보세요. 어떤 상황이었고, 어떻게 대응했는지 구체적으로 설명하는 것이 좋습니다."
  - q149~152 동일 패턴
- 사용자 인지 채널: Issue #435 사용자 직접 보고. 재현 데이터 = dev EC2 docker log + DB question 테이블.

## 왜 해야 하는가 (Why)

- 사용자 임팩트: 모범답변 = 응시자가 "어떻게 답해야 좋은지" 학습 자료. 1~2문장 일반론 = 학습 가치 없음. 응시자 신뢰 저하.
- 운영 / 시스템 임팩트: completion tokens 138~227 (q148~152). 짧음 = LLM 정상 출력이 PM 의도와 어긋남.
- 외부 압력: 사용자 직접 호소 ("성의 없음", "fallback 의심"). 본 도메인 = 제품 핵심 경험 (모범답변 활용 가치).

## 해결 방향 (Approach)

PM 수준 high-level. **HOW 침범 금지** — tech-spec 영역.

- 핵심 접근: model_answer 정의 갭 2축 재정렬 — (a) 분량 정의를 더 길게, (b) "가이드" → "구체 모범답변 + 답변 구조 단서" 로 톤 전환
- 대안 비교:
  - 단순 프롬프트 글자수만 조정 → 채택 X. 톤 (가이드 vs 답변 예시) 갭 잔존
  - 정의 + 톤 + 자가검증 동시 재정렬 → **채택**. 정상 출력 자체가 PM 기대와 일치하도록
- 단계 분리: 단일 phase (RESUME 3 프롬프트 + fallback 상수). 다른 트랙 (STANDARD / 꼬리질문) = 별도 Issue.

## Evidence

- 코드 추적:
  - `backend/src/main/resources/prompts/template/resume/resume-playground-opener.txt:17` — model_answer 정의 "50~200자, 정답 X — 어떤 관점/구조로"
  - `backend/src/main/resources/prompts/template/resume/resume-playground-responder.txt:30` — 동일 정의
  - `backend/src/main/resources/prompts/template/resume/resume-chain-interrogator.txt:28` — "현재 chain 해당 레벨 답변 시 참고할 가이드라인 (50~200자, 정답 X)"
  - `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeFallbackModelAnswers.java` — fallback 상수 3개도 1줄 가이드 톤
  - `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeQuestionResultGenerator.java:47,69,94` — blank 시 fallback 적용. 정상 출력 시 LLM 결과 그대로 적재
- 운영 로그 / 메트릭: dev EC2 interview id=26 question_set id=128 q147~152. completion tokens 138~227. fallback 경고 (`modelAnswer 폴백 적용`) 미발동
- 사용자 발화: Issue #435 본문 + 직접 호소 ("1~2줄로 끝나서 성의없음")
- 인접 plan: 없음 (RESUME 트랙 model_answer 품질 신규 영역)

## Goal

- [ ] model_answer 분량 = **200~300자 / 3~5줄** (eval 셋 케이스의 ≥80%)
- [ ] 가이드 톤 어구 ("~을 설명해보세요", "~을 이야기해보세요", "~하면 좋습니다") 출현 비율 0%
- [ ] 사용자 고유 맥락 키워드 (projectName / 응시자 직전 발화 핵심 명사 / chain topic) 포함률 ≥80%
- [ ] 답변 구조 단서 (STAR / 결정 근거 / 트레이드오프 / 학습 중 1+) 포함률 ≥80%
- [ ] LLM-as-judge rubric 정성 평가 통과율 ≥70% (잘 / 보통 / 나쁨 3 등급 중 잘+보통)
- [ ] before/after eval 정량·정성 리포트 산출

## Non-Goals

- latency / 토큰 비용 단축 — 분량 ↑ 자연스러움. 비용 < 학습 가치
- model_answer FE 노출 디자인 (길이 제한 / 마크다운 / 펼치기 등) — UX 영역. 본 작업은 데이터 품질만
- model_answer 정확성 fact-check / 이력서 일관성 검증 — 별도 도메인

## 수용 기준 (Acceptance Criteria)

- [ ] RESUME OPENER 모범답변 분량 = 200~300자 / 3~5줄. **답변 구조 권유 가이드 톤** ("~ 구조가 효과적입니다", "~을 정리하세요", "~ 마무리하면") — 1인칭 답변 예시 ("저는 ~했습니다") 0건. 응시자 본인 경험은 본인만 정확히 알기에 거짓 placeholder 위험
- [ ] RESUME OPENER 모범답변 = 응시자 이력서 projectName / 역할 / 기술 키워드 명시 (지시 표현 "이 프로젝트" / "해당 프로젝트" 단독 사용 0건)
- [ ] RESUME PLAYGROUND 모범답변 = 응시자 직전 발화의 핵심 명사 1+ 재인용. **가이드 톤** (OPENER 와 동일 — 거짓 1인칭 placeholder 위험으로 권유 톤 강제)
- [ ] RESUME INTERROGATION 모범답변 = 현재 chain topic + 레벨 (L1 WHAT / L2 HOW / L3 WHY_MECH / L4 TRADEOFF) 맥락 반영한 **1인칭 답변 예시 톤** ("~했습니다", "~경험이 있습니다"). chain 안에서는 직전 응시자 발화 + topic 이 LLM 에 전달되어 구체 placeholder 생성 가능 (OPENER/PLAYGROUND 와 분기되는 사유)
- [ ] 모든 모드 모범답변 = 답변 구조 단서 1+ 포함 (STAR / 결정 근거 / 트레이드오프 / 학습 중 1+)
- [ ] fallback 상수 3개 (`ResumeFallbackModelAnswers.OPENER / PLAYGROUND / INTERROGATION`) 분량 (200~300자) 유지 · 톤 = **가이드** (LLM 실패 시 사용자 컨텍스트 모름 → 거짓 1인칭 답변 placeholder 금지. 답변 구조 / 관점 권유 톤만). 답변 구조 단서 (STAR / 결정 근거 / 트레이드오프 / 학습) 1+ 명시
- [ ] eval/context 셋 5+ 케이스 before/after 정량 (분량 / 가이드 톤 어구 출현 / 맥락 키워드 / 구조 단서 포함) + 정성 (LLM-as-judge) 비교 리포트 산출
- [ ] 회귀 테스트 통과 (기존 question / TTS 필드 정상 / JSON schema 위반 없음)

## 비스코프 (Don't)

- 꼬리질문 model_answer (`follow-up-concept.txt`, `follow-up-experience.txt`) 동일 갭 — 별도 Issue 권장 (발견 사항)
- STANDARD 트랙 초기 질문 model_answer (`question-generation.txt` CS=MODEL_ANSWER / RESUME=GUIDE 분기) — 별도 Issue 권장
- WRAP_UP 모범답변 — WRAP_UP 코드 자체 삭제됨
- model_answer 자동 정확성 검증 (이력서 일관성 / 사실 fact-check) — 별도 도메인
- prompt-cache TTL / 모델 변경 — 본 fix 범위 외

## 참고

- 관련 Issue: #435
- 관련 plan: 없음
- 관련 코드 (조사 진입점):
  - `backend/src/main/resources/prompts/template/resume/resume-playground-opener.txt`
  - `backend/src/main/resources/prompts/template/resume/resume-playground-responder.txt`
  - `backend/src/main/resources/prompts/template/resume/resume-chain-interrogator.txt`
  - `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeFallbackModelAnswers.java`
  - `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeQuestionResultGenerator.java`
- 발견 사항 (별도 Issue 권장):
  - `backend/src/main/resources/prompts/template/follow-up-concept.txt:67,158` — model_answer "2~4문장" 동일 갭
  - `backend/src/main/resources/prompts/template/follow-up-experience.txt:82,182` — 동일
  - `backend/src/main/resources/prompts/template/question-generation.txt:37-39` — STANDARD 트랙 분기, 분량 미명시 (실제 출력 검증 필요)
