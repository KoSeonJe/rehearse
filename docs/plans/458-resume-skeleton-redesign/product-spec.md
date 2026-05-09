# Product Spec — Resume 면접 표현력 재설계 + 답변 reactive 꼬리질문

> **작성자**: 사용자 + PM (Claude)
> **답하는 질문**: 왜? 무엇? 수용기준?
> **다음 단계**: 사용자 승인 → 구현 agent 가 `tech-spec.md` 작성

---

## 문제 상황 (Problem)

### 현재 상태

이력서 → Skeleton(JSON) → Plan(JSON) → 질문 / 모범답변 생성 파이프라인이 resume track 으로 운영 중. 면접 흐름은 사전 정의된 4단 심화 체인 (개념 → 절차 → 원리 → 트레이드오프) 을 순차 따라감. 답변 점수 (LEVEL_UP / STAY / CHAIN_SWITCH) 만 흐름 제어에 사용.

비교: standard track 은 답변에서 새 단서 추출 → 동적 꼬리질문 생성 메커니즘 보유. resume track 은 부재.

### 발생 증상

- 응시자 이력서에 풍부한 메타 (사용 기술 / 본인 역할 / 시스템 구성 / 결정 사유 / 비교 대안) 존재해도 Skeleton 추출 단계에서 대부분 휘발 → 면접 산출물에 본인 맥락 인용 없음.
- 응시자가 답변 도중 새 깊이 노출 (예: "선택지 X 와 Y 비교했다") 해도 사전 정의 4단 체인 그대로 진행. 답변에서 던진 새 단서를 다음 질문이 따라가지 않음.
- 응시자 인지: "내 이력서 풍부한데 면접관이 일반론 질문만. 답변에서 더 깊이 꺼내도 무시한다."

### 발견 채널

- 운영 시연 회고 + Issue #458 자체 분석 (Live LLM 산출물 vs 이력서 원문 어휘 비교).
- 인접 plan: `435-resume-model-answer-quality` (현 브랜치) — 모범답변 1인칭 톤 fix 진행. 본 plan = 더 큰 표현력 재설계 영역.

---

## 왜 해야 하는가 (Why)

### 사용자 임팩트

- 응시자 이력서 메타 (사용 기술 / 본인 역할 / 결정 사유) 가 면접 질문·답변 흐름에 살아남음 → "내 이력서 보고 면접한다" 신뢰.
- 답변에서 새로 노출한 깊이 (트레이드오프 / 비교 대안) 가 다음 질문에 이어짐 → "면접관이 내 답변 듣고 따라온다" 체감.

### 운영 / 시스템 임팩트

- 모범답변 generic fallback 발동률 측정 가능 + 감소.
- 동일 이력서 재추출 시 결과 변동성 (LLM 합성 식별자 hallucinate) 감소.

### 외부 압력

- Interview Quality Sprint S14 진행 중. resume track 표현력 = 다음 핵심 작업 단위.

---

## 해결 방향 (Approach)

PM 수준 high-level 만. 구현 디테일 (필드명 / 클래스명 / prompt 구조) = tech-spec 영역.

### 핵심 접근 (2 phase)

**phase 1 — 이력서 → 면접 산출물 표현력 (이번 plan 우선 완료)**:
- 이력서 메타 (기술 / 역할 / 시스템 구성 / 결정 사유 등 면접 깊이 직결 정보) 가 면접 산출물까지 살아남도록 추출 단계 정보 손실 해소.
- 사용 안 되는 부산 정보 / 안정적 식별 어려운 LLM 합성 키 의존 정리.

**phase 2 — 답변 reactive 꼬리질문 (standard 트랙 재활용 우선)**:
- 응시자 답변에서 새 깊이 / 비교 대안 / 가정을 reactive 인지 → 다음 질문이 해당 단서 인용.
- **채택 방식 = standard 트랙 기존 답변 분석기 / 후속 질문 빌더 재사용**. resume 트랙은 이력서 Skeleton 컨텍스트만 추가 주입.
- 신설 분석기 = 폐기 옵션 (재활용 검증 후 부족분만 한정 보강).
- "절단 가능" 옵션 없음 — phase 2 = 본 plan 핵심 가치.

### 대안 비교 (간략)

- **한 번에 표현력 + 꼬리질문 + 일반론 prompt 정밀화 + 모범답변 prompt 풍부화 전부** — 폐기. 변경 폭 과대 / 롤백 난이도 ↑.
- **표현력만 이번 plan + 꼬리질문 별도 plan** — 폐기. 사용자 핵심 발화 = "꼬리질문" → 본 plan 의도와 분리 시 의미 약화.
- **phase 2 신설 분석기** — 폐기. standard 트랙 기존 답변 분석기 재활용으로 사이즈 1/3 가능. 신설 = YAGNI.
- **phase 0 사용자 보이스 검증 (인앱 설문 + prompt-only spike)** — 폐기. 사용자 결정 = 본 epic 가치 확신, 검증 단계 생략.
- **채택**: phase 1 표현력 인프라 + phase 2 standard 재활용 꼬리질문. 양 phase 본 plan 핵심.

---

## Evidence

- 도메인 코드 스캔: resume Skeleton 보유 메타 = 후보 레벨 / 타깃 도메인 / 프로젝트 / 우선순위 매핑. 사용 기술 / 본인 역할 / 시스템 구성 같은 깊이 신호 메타 부재 (확인).
- 추출 prompt: 이력서 → 4단 chain 강제 instruction 그대로 (확인).
- 모범답변 fallback 분기 3개 (oepner / playground / interrogation) 다 generic 본문 (확인).
- 부산 필드 (심화 키워드 후보 배열) 사용 site = Issue 본문 주장 0건. 본 spec 추정 — tech-spec 단계 재확인 필요.
- standard track 답변 분석기 / 후속 질문 빌더 보유 — phase 2 채택안 후보 (재활용 vs 신설 = tech-spec 결정).
- 메모리: Interview Quality Sprint S1~S13 완료. S14 진입 = 본 plan.
- 인접 plan: `docs/plans/435-resume-model-answer-quality/` — 모범답변 fallback 톤 1인칭 진행. 본 plan 과 경계 = "fallback 톤" (#435) vs "표현력 인프라 + 꼬리질문" (본 plan).

---

## Goal

측정 가능 결과. 측정 도구 = Live LLM E2E 회귀 (사용자 결정).

### phase 1 (표현력)

- [ ] **메타 보존**: 이력서에 사용 기술 / 본인 역할 / 시스템 구성 / 결정 사유 명시된 fixture 3+ 건에 대해, 면접 산출물 (질문 또는 모범답변) 에 해당 어휘군 (정확 어휘 또는 동의어) 등장. fixture 별 ≥ 1회 인용 단언 통과.
- [ ] **모범답변 본인 어휘 포함률**: Live LLM E2E fixture 3+ 건 중 ≥ 80% 가 응시자 이력서 어휘 (프로젝트명 / 사용 기술 / 본인 역할 중 1개 이상) 포함. LLM 비결정성 완충 = 어휘군 매칭.
- [ ] **모범답변 generic fallback 발동**: 회귀 테스트 fixture 정상 흐름 시 발동 0건 (fallback 코드 진입 자체 0회).
- [ ] **부산 정보 / 합성 키 의존 cleanup**: 정리 대상 항목이 코드 / prompt / 데이터 어디에도 잔존 0건. 회귀 테스트 + 빌드 통과.

### phase 2 (reactive 꼬리질문 — standard 재활용)

- [ ] **답변 따라가기**: 응시자 답변이 사전 체인 외 새 어휘 / 비교 대안 노출하는 fixture 시나리오에서, 다음 질문이 해당 어휘군 인용. **통과 단위 = fixture 개수 (서로 다른 이력서 fixture 3+ 건, 각 fixture 1회 실행, ≥ 2건 통과)**. LLM 비결정성 완충 = 어휘군 매칭 (정확 어휘 + 동의어).
- [ ] **standard 트랙 분석기 재활용**: resume 트랙 꼬리질문 흐름이 standard 트랙 기존 답변 분석기 호출 기반 (전용 신설 0개).
- [ ] **회귀 무영향**: 답변 새 깊이 부재 fixture = 기존 chain 진행 동일 (회귀 테스트 통과). standard 트랙 회귀 영향 없음.

---

## Non-Goals

추구하지 않는 가치 (혼동 방지). 출처 = (사용자 결정) / (PM 추천 — 사용자 확인 대기).

- **일반론 질문 자체 제거** (사용자 결정) — 사유: 사용자 명시 발화 = "일반론 질문 OK, 거기서 안 끝나는 게 핵심". 일반론 자체 척결은 목표 아님. 핵심 = phase 2 꼬리질문.
- **모범답변 prompt 컨텍스트 풍부화 일반** (PM 추천) — 사유: 인접 plan #435 영역. 본 plan 핵심 = 표현력 인프라 + 꼬리질문. 모범답변 prompt 정밀화는 #435 종료 후 별도 검토.
- **standard track 동등 깊이 즉시 도달** (PM 추천) — 사유: phase 2 채택안 따라 단계적. standard 와 코드 동치성 목표 아님.

---

## 수용 기준 (Acceptance Criteria)

외부 관찰 결과만. 구현 메커니즘 미언급.

### phase 1 (표현력)

- [ ] 이력서 메타 (기술 / 역할 / 시스템 구성) 명시된 fixture 에서, 면접 질문 또는 모범답변 본문에 해당 어휘군 등장 (Live LLM E2E 단언 통과).
- [ ] 동일 이력서 재추출 2회 결과 비교 시, claim 단위 식별 안정 (LLM 합성 식별자 의존 시 발생하는 ID 변동 0건).
- [ ] 정리 대상 부산 정보 / 합성 키 의존 = 코드 / prompt / 데이터 잔존 0건 (회귀 빌드 + 테스트 통과).
- [ ] 기존 resume track Live LLM E2E 회귀 통과 (현 회귀 스위트 + 본 plan 신규 fixture).
- [ ] 기존 운영 데이터 호환 정책이 tech-spec 마이그레이션 절에 명시 (산출물 존재 = AC).

### phase 2 (reactive 꼬리질문)

- [ ] 응시자 답변이 사전 체인 외 새 어휘 / 비교 대안 노출하는 fixture 에서, 다음 질문이 해당 어휘군 인용 (Live LLM E2E 단언 통과, **fixture 3+ 건 중 ≥ 2건 통과** — 비결정성 완충 = 어휘군 매칭).
- [ ] 답변 새 깊이 부재 fixture 에서 기존 chain 진행 회귀 영향 없음.
- [ ] phase 2 = standard 트랙 답변 분석기 재활용 형태로 구현 (전용 신설 폐기). tech-spec Trade-off 채택 사유 명시.

---

## 비스코프 (Don't)

이번 의도적 제외. 향후 별도 plan / Issue.

- **일반론 질문 prompt 재작성 (교과서 질문 → 본인 인용 강제)** — 사유: 사용자 발화 매핑 결과 의도 외. 별도 가치 검토 시 신규 Issue.
- **체인 깊이 상한 (최대 단계) 변경** — 사유: phase 2 reactive 꼬리질문이 깊이 한계 다른 경로로 풀어줄 가능성. 우선 phase 2 결과 보고 별도 결정.
- **모범답변 prompt 이력서 원문 inject 등 풍부화 작업** — 사유: 인접 plan #435 영역. 중첩 회피.
- **운영 메트릭 자동화 (대시보드 / 배치 측정 도구 신설)** — 사유: Goal 측정 인프라 = Live E2E 회귀 (사용자 결정).
- **resume 전용 답변 분석기 신설** — 사유: standard 트랙 기존 분석기 재활용 결정 (사용자 확정). 신설 = YAGNI.
- **phase 0 사용자 보이스 검증 (인앱 만족도 설문 / prompt-only spike)** — 사유: 사용자 결정 = 본 epic 가치 확신. 검증 단계 생략.

---

## 의존 / 선행

- 인접 plan `435-resume-model-answer-quality` 머지 후 진입 권장 (모범답변 fallback 톤 base 적용).
- BE 단독 작업 가정. tech-spec 1차 task 로 외부 API contract 영향 (FE 노출 여부) 확인.
- 기존 운영 데이터 (이미 추출된 Skeleton row) 호환 정책 = tech-spec 마이그레이션 절 결정.

---

## 참고

- 관련 Issue: [#458](https://github.com/.../issues/458) (Epic / P1 / BE)
- 인접 plan: `docs/plans/435-resume-model-answer-quality/`
- 메모리: Interview Quality Sprint (S14 진입)
- 폴더 slug: `458-resume-skeleton-redesign`
