# Product Spec — Resume Playground opener 톤 정합 + projectName 호명

> **작성자**: 사용자 (PM 페르소나 초안: Claude)
> **답하는 질문**: 왜? 무엇? 수용기준?
> **다음 단계**: 사용자 승인 → 구현 agent 가 `tech-spec.md` 작성

---

## 문제 상황 (Problem)

- 현재 상태 (정상 동작 / 기존 흐름):
  - Resume 트랙 인터뷰 첫 질문 (Playground opener) 은 priority 1 프로젝트 1개에 대해 LLM 으로 생성됨.
  - Planner LLM 이 미리 뽑은 `opener_question` 을 ResumePlaygroundPromptBuilder LLM 이 자연어 변형 후 출력.
  - Playground 단계는 priority 1 프로젝트 1개에 한정 (`PlaygroundModeHandler.java:39, 132-134`). 다른 프로젝트들은 Interrogation 단계에서 chain 별 순회.

- 발생 증상 (재현: interview_id=25, dev):
  - `plan_json.opener_question` = "이 프로젝트에서 사용자 대기 시간을 단축하기 위해 어떤 기술적 결정을 내렸는지 설명해주세요."
  - 실제 `question_text` (id=146) = "이 프로젝트에서의 경험을 바탕으로, 사용자 대기 시간을 단축하기 위해 어떤 접근 방식을 사용했는지 자유롭게 이야기해 주실 수 있나요?"
  - 둘 다 narrow 기술 심문 톤 + projectName 호명 부재.
  - 응시자 = 어떤 프로젝트인지 즉시 인식 불가 + intro 단계 기대 (편안한 자유 회고) 와 톤 충돌.

- 사용자·운영 인지 채널: Issue #421 (운영 / dev 인터뷰 직접 재현).

## 왜 해야 하는가 (Why)

- 사용자 임팩트:
  - 인터뷰 첫 발화 = 라포 형성 / 맥락 형성 단계. narrow 기술 심문 톤 + 프로젝트 미식별 = 응시자 위축 + 프로젝트 헷갈림 → 자유 발화 실패.
  - 의도된 흐름 ("프로젝트 / 역할 설명 → 어려웠던 경험" 자유 회고) 부재.

- 운영 / 시스템 임팩트:
  - Playground 의도 ("맥락 + 자유 회고") 미달 → Responder 의 Interrogation 전환 판단 입력 빈약 → 전환 시점 오작동 가능.
  - 단계 책임 분리 위반 — Playground 첫 질문에서 기술 narrow = 두 번째 단계 (Interrogation = 기술 심화) 책임 침범.

- 외부 압력: Issue #421 dev 운영 재현 보고.

## 해결 방향 (Approach)

- Planner 출력 `opener_question` 톤 자체를 intro 톤으로 강제 — few-shot 예시 교체 + 가이드 문구 추가 ("Playground = intro 톤 / narrow 기술 심문 금지").
- Playground 단계 LLM (Builder) 가이드 강화 — 기술 narrow 발화 금지 어휘 확장 + 역할 / 흐름 / 감정·서사 oriented 허용 명시. Opener + Responder 양쪽 (Playground 단계 = intro 톤 일관성 보장. Responder 1턴까지 narrow 금지 동일 적용 → 첫 질문만 고치면 다음 턴에서 톤 깨질 위험 차단).
- LLM 입력 PROJECT_INFO 슬롯 단순화 — projectName 만 주입. claims text / topics 카운트 제거 (해당 정보는 Interrogation 단계 책임).

대안 비교 (간략):
- claims text 도 LLM 입력에 추가 → 기각. Playground 는 기술 narrow 안 함 → claims 노이즈.
- targetDomain 추가 주입 → 기각. Playground intro 톤에 직무 도메인 영향 미미. builder 시그니처 변경 비용만 발생.

단계 분리: 본 Issue = 단일 phase (3 touchpoint: planner.txt few-shot + opener.txt 가이드 + formatProjectInfo() 단순화).

## Evidence

- 코드:
  - `backend/src/main/resources/prompts/template/resume/resume-interview-planner.txt:82` — few-shot opener_question = narrow tech ("이 프로젝트에서 가장 어려웠던 기술적 결정을 설명해주세요").
  - `backend/src/main/resources/prompts/template/resume/resume-playground-opener.txt:8` — narrow 금지 어휘 일부 ("내부 원리", "왜 그렇게 설계") 만 명시. "기술적 결정", "트레이드오프", "메커니즘" 누락.
  - `backend/src/main/resources/prompts/template/resume/resume-playground-opener.txt:31-32` — projectName 누락 시 안전 폴백 가이드 ("가장 자신 있게 설명할 수 있는 프로젝트", "가장 많이 배운 프로젝트 경험" 등 open question 위임). 본 작업 회귀 단언 기준.
  - `backend/src/main/resources/prompts/template/resume/resume-playground-opener.txt:40` — 최종 안전 폴백 문자열 ("이 프로젝트에서 가장 인상 깊었던 경험을 자유롭게 이야기해주세요."). LLM 출력 불가 케이스에서 그대로 출력. 본 작업 보존 대상.
  - `backend/src/main/java/com/rehearse/api/infra/ai/prompt/ResumePlaygroundPromptBuilder.java:68-75` — `formatProjectInfo()` = projectId + projectName + claims 카운트 + topics 카운트. 카운트 노이즈 + 카운트만으론 LLM 이 프로젝트 정체성 빈약 인지.
  - `backend/src/main/java/com/rehearse/api/domain/resume/service/PlaygroundModeHandler.java:39, 132-134` — Playground = priority 1 프로젝트 1개 한정 정책 확인.

- 의존:
  - Issue #412 (CLOSED 2026-05-07) — projectName 적재 완료. `Project` record / `ProjectPlan` / `planner.txt` pass-through 가이드 / `opener.txt` 호명 가이드 모두 반영됨. 본 Issue 진행 차단 요소 없음.

## Goal

- [ ] Resume 트랙 첫 질문 (Playground opener) 텍스트에 priority 1 프로젝트 projectName 문자열 포함 — fixture 통합 테스트 단언
- [ ] Playground opener / Responder 출력 텍스트에 narrow 기술 심문 어휘 부재 — fixture 단언 (금지 어휘 목록 매치 X). 어휘 목록 출처 = interview_id=25 narrow 출력 + planner few-shot 톤 누출 패턴 기반 최소 안전망 (정밀도 우선). 의미 재현율 = 운영 모니터링 / 인간 검토 의존 (Non-Goals 참조).
- [ ] Playground LLM 입력 PROJECT_INFO 슬롯 = projectName 만 (claims / topics 카운트 부재) — 단위 테스트 단언

## Non-Goals

- Production LLM 출력 자연어 의미 자동 평가 — 사유: 비결정성 + 측정 도구 부재. fixture 단언 수준 한정.
- Playground 응답 latency 단축 — 사유: 톤 정합성 우선. 본 작업 부산물로 예상 (PROJECT_INFO 축소 → 입력 토큰 감소).

## 수용 기준 (Acceptance Criteria)

- [ ] 다중 프로젝트 mock 이력서 fixture 인터뷰 진행 시 첫 질문 텍스트에 priority 1 프로젝트 projectName 문자열 포함
- [ ] 첫 질문 + Playground Responder 1턴 텍스트에 narrow 어휘 부재. 금지 어휘 목록: ["내부 원리", "왜 그렇게 설계", "기술적 결정", "트레이드오프", "메커니즘", "어떻게 동작"]. fixture 통합 테스트 단언 (포함 시 실패). 어휘 목록 = 최소 안전망 (출처: 위 Goal #2 참조). 동의어 변종은 본 단언 범위 외 — 운영 모니터링 / 후속 Issue 영역.
- [ ] 첫 질문 + Playground Responder 1턴 텍스트가 다음 두 조건 **AND** 만족: (a) 위 금지 어휘 부재 AND (b) 의도된 어휘군 매치. 의도 어휘군 = Opener: ["역할", "맡으셨", "설명", "소개"] 중 1+ / Responder: ["어려웠던", "기억", "인상", "경험"] 중 1+. AND 조건 미충족 시 fixture 실패. (단일 어휘 우회 차단 + 단계별 의도 분리)
- [ ] projectName 누락 (legacy / null) 이력서 = 임의 명칭 생성 없이 안전 폴백 출력 — `resume-playground-opener.txt:31-32` (open question 위임 가이드) + `:40` (최종 폴백 문자열) 기존 가이드 보존 회귀 테스트. 본 작업으로 폴백 동작 변경 X.
- [ ] Playground LLM PROJECT_INFO 슬롯 입력 = projectName 만. claims 카운트 / topics 카운트 부재 — 단위 테스트 단언.

## 비스코프 (Don't)

- Interrogation / WrapUp 단계 톤 — 사유: 본 Issue 영역 외. 필요 시 별도 Issue.
- Skeleton 추출 단계 변경 — 사유: #412 완료. 본 Issue scope X.
- LLM 출력 의미 자동 평가 / 모니터링 시스템 — 사유: 별도 운영 작업.
- Playground 다중 프로젝트 진입 정책 변경 — 사유: 현 정책 (priority 1 1개 한정) 유지.
- Production 운영 데이터 backfill — 사유: 톤 fix = 신규 인터뷰부터 적용. 과거 인터뷰 회고 X.

## 참고

- 관련 Issue: #421 (본 작업), #412 (선행 의존, CLOSED 2026-05-07)
- 관련 plan: docs/plans/412-resume-project-name/
- 코드 단서: 위 Evidence 섹션
