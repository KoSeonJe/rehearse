# Product Spec — Resume 질문 생성 프롬프트 직무 적합성 + 깊이 강제

> **작성자**: 사용자 + PM (Claude)
> **답하는 질문**: 왜? 무엇? 수용기준?
> **다음 단계**: 사용자 승인 → 구현 agent 가 `tech-spec.md` 작성

---

## 문제 상황 (Problem)

### 현재 상태

Resume 트랙은 응시자 이력서 PDF → skeleton(JSON) → main 질문 N개 + opener 1개 형태로 면접 질문 일괄 생성. 시스템 프롬프트 본문 = 단일 raw 템플릿. 직무 (백엔드 / 프론트엔드 / 데브옵스 / 데이터 엔지니어 / 풀스택) 무관 동일 본문.

비교: 일반 인터뷰 트랙 (사용자가 직무 + 스택 선택해 진행하는 별도 면접 흐름) 은 직무별 페르소나 + 평가 관점 + 심화 방향이 시스템 프롬프트에 주입됨. Resume 트랙은 같은 자산을 보유하고도 미활용.

### 발생 증상

- 생성 main 질문이 "왜 X 기술을 사용하셨나요?" / "X 의 장점은 무엇인가요?" / "X 를 선택한 이유는?" 류 표층 질문에 머묾.
- 응시자 이력서 분량 / 깊이와 무관하게 동일 패턴 반복. 응시자가 본인 시스템 구성 / 트레이드오프 / 수치를 이력서에 명시해도 그 깊이를 끌어내는 질문 부재.
- 응시자 직무 (예: 프론트엔드) 와 무관하게 백엔드 편향 어휘 (트랜잭션 / 동시성 등) 가 빈번 (추정: 사용자 1인 보고 기반, 정량 측정 없음).
- 이력서 본문에 응시자가 명시한 트레이드오프 / 수치 측정 환경 / 비교 대안이 풍부해도 skeleton 추출 단계에서 `decisions` 한 줄 요약 + `tech_stack` 키워드 배열로 휘발 → 깊은 질문 생성 단계가 활용할 재료 자체 부재.

### 발견 채널

- 사용자 직접 호소 (본 세션) — "왜 X 썼나요" 류 표층 질문 다발 보고.
- 일반 트랙 vs Resume 트랙 시스템 프롬프트 구성 비교 시 persona/overlay 자산 활용 격차 발견.
- 인접 plan `458-resume-skeleton-redesign` (closed) 에서 skeleton schema 단순화 완료 — 본 작업은 그 위 질문 생성 단계 품질 작업.

---

## 왜 해야 하는가 (Why)

### 사용자 임팩트 (1순위)

- 응시자가 본인 직무에 맞는 깊이 있는 질문을 받음 — "내 이력서 + 직무 보고 면접한다" 신뢰.
- 표층 질문 (선택 이유 / 장점 묻기) 비중 감소 — 응시자 답변의 학습 가치 증가.
- 면접 결과 (질문 + 답변 회고) 의 직무 특화 어휘 / 깊이가 응시자 자기 진단 자료로 기능.

### 운영 / 시스템 임팩트 (부산 효과)

- 표층 질문 비중 = 사용자 이탈 / 신뢰 저하 위험. 정량 측정 안 되더라도 사용자 보고 채널 (Issue / 직접 호소) 통한 결함 시그널 감소 기대.
- 보조 효과: 기존 자산 (직무별 페르소나 yaml 5종 + 스택 overlay yaml 14종) 을 일반 트랙만 활용 중. Resume 트랙 적용 시 이중 관리 / 갱신 비용 감소 (사용자 가치는 아니나 운영 부산물).

### 외부 압력

- Interview Quality Sprint 연속선. S13 까지 완료, S14 핵심 후보.
- 사용자 직접 호소 (본 세션) = 우선 처리 신호.

---

## 해결 방향 (Approach)

PM 수준 high-level. **HOW 침범 금지** — tech-spec 영역.

### 핵심 접근

Resume 트랙 질문 생성 파이프라인을 세 축으로 보강. 단일 작업 단위 (1 PR) 로 통합.

**축 A — 직무 적합성 확보**:
- 기존 직무별 페르소나 + 평가 관점 + 심화 방향 자산을 Resume 트랙 시스템 프롬프트에 반영.
- 응시자 직무 / 스택 (인터뷰 생성 시점 사용자 선택값) 에 따라 질문 어휘 / 깊이가 분기되도록 함.

**축 B — 좋은 질문 정의 강제**:
- 면접 깊이 5 유형 (트레이드오프 / 한계 시나리오 / 수치 검증 / 대안 비교 / 동작 원리) 을 main 질문 분배 룰로 도입.
- 표층 질문 패턴 ("왜 X 사용 / 선택 / 채택", "X 의 장점") 을 명시적 금지 항목으로 시스템 프롬프트에 표면화.
- 생성된 main 질문 단건마다 어느 깊이 유형에 해당하는지 사용자가 외부에서 식별 가능 (보존 수단 = tech-spec 결정).

**축 C — Skeleton 표현력 풍부화**:
- 이력서 추출 단계에서 응시자가 명시한 의사결정 근거 / 비교 대안 / 수치 측정 환경 / 트레이드오프를 단순 문자열 한 줄 요약이 아니라 구조화된 형태로 보존.
- 추출 충실도 룰 (이력서 명시 정보만 / 창작 금지) 유지하면서 깊이 신호 보존 가능.
- 기존 skeleton 필드 (직무 / 사용 기술 / 시스템 구성 / 의사결정 등) 보존 — 구버전 데이터 회귀 미발생.

**축 결합 흐름**:
- 축 C 산출물 (skeleton 깊이 신호) 은 축 A+B 시스템 프롬프트가 5 깊이 유형 (특히 트레이드오프 / 수치 / 대안 비교) 분배 시 인용 재료로 사용. 셋이 결합되어야 표층 질문 감소 효과 발생.

### 대안 비교 (간략)

- **축 A 만 (직무 적합성)** — 폐기. 본문 자체 약점 (표층 질문 유도 문구) 잔존.
- **축 B 만 (깊이 강제)** — 폐기. 직무 무관 동일 깊이 가이드 → 백엔드 편향 잔존.
- **축 A + B (축 C 제외)** — 폐기. skeleton 한 줄 요약 한계로 깊이 질문 재료 부족 → 5 유형 분배 강제해도 모델이 만들 재료 없음.
- **축 A+B 선행 PR + 축 C 후행 PR 분리** — 폐기. 회귀 격리 장점 있으나 사용자가 통합 채택 결정. 채택 사유: skeleton 저장 = MySQL JSON 컬럼 + `@JsonIgnoreProperties` = 구버전 호환 안전 (사용자 확인) → 분리 PR 의 회귀 격리 가치보다 통합 진행 속도 우선.
- **채택**: 축 A + 축 B + 축 C 동시. 자산 재활용 (신규 yaml X) + 본문 재설계 + skeleton 풍부화 한 PR.

### 단계 분리

축 A+B+C 단일 PR phase 로 통합 (사용자 결정). 효과 측정 후 후속 결정.

---

## Evidence

### 코드 추적

- `backend/src/main/resources/prompts/template/resume/resume-question-generator.txt:1-28` — 본문 28줄, "tech_stack / decisions 인용한 구체 질문" 지시. 깊이 유형 분류 / 금지 패턴 / 예시 부재.
- `backend/src/main/java/com/rehearse/api/infra/ai/context/layer/FixedContextLayer.java:103-116` — Resume 트랙 시스템 프롬프트 = GLOBAL_CORE + 템플릿 raw. 토큰 치환 없음.
- `backend/src/main/java/com/rehearse/api/infra/ai/prompt/QuestionGenerationPromptBuilder.java:41-66` — 일반 트랙은 페르소나 / 평가 관점 / 인터뷰 유형 가이드 토큰 치환 보유. **자산 존재 + 활용 패턴 검증됨**.
- `backend/src/main/resources/prompts/base/{backend,frontend,devops,data-engineer,fullstack}.yaml` (5개) + `backend/src/main/resources/prompts/overlay/{domain}/*.yaml` (14개) — 직무 × 스택 자산 총 19개.
- `backend/src/main/java/com/rehearse/api/infra/ai/dto/GeneratedResumeQuestions.java:19-29` — 현재 main 질문 페이로드 = question / tts_question / best_answer 3 필드. 깊이 유형 메타 부재.
- `backend/src/main/java/com/rehearse/api/domain/question/service/ResumeTrackInitiator.java:51-67` — initiate() 시그니처 = `interviewId, resumeFileHash, resumePdfBytes, durationMinutes`. 직무 / 스택 미전파 (Interview 엔티티 조회로 확보 가능 추정).
- `backend/src/main/resources/prompts/template/resume/resume-extractor.txt:1-87` — 추출 본문 28+ 줄. `decisions: ["X vs Y → 채택, 사유 1줄"]` 단순 문자열 배열. 수치 / 트레이드오프 / 대안 비교 구조 부재.
- `backend/src/main/java/com/rehearse/api/domain/resume/entity/ResumeSkeleton.java` — record 클래스 `@JsonIgnoreProperties(ignoreUnknown = true)` 보유. 새 필드 추가 시 구버전 skeleton JSON 역직렬화 안전.
- `backend/src/main/java/com/rehearse/api/domain/resume/entity/ResumeSkeletonEntity.java` — `skeleton_json` 컬럼 `columnDefinition = "JSON"`. 스키마 변경 시 DDL 마이그레이션 불필요.

### 인접 plan

- `458-resume-skeleton-redesign` (CLOSED) — skeleton schema 4-field 단순화 + 표현력 보존 phase 1 진행 후 closed. 본 plan 축 C = 표현력 보존 방향 연장 (기존 필드 위에 깊이 신호 추가).
- `435-resume-model-answer-quality` (CLOSED) — model_answer 톤 fix. 본 plan = main 질문 자체 깊이 작업, 영역 다름.
- `421-resume-playground-opener-tone` — opener 톤 작업. 본 plan = main 우선, opener 영향 = tech-spec 단계 확인.

### 메모리 / 기타

- Interview Quality Sprint S1~S13 완료. 본 plan = S14 후보 (메모리: `project_interview_quality`).
- Issue 신규 생성 없음 (사용자 결정). 폴더 prefix = `00y-` (00x 기존 사용).

---

## Goal

수동 샘플링 기반 검증. 자동 측정 인프라 신설 X. **검증 주체 = 사용자 (본 plan 작성자) 단독**.

- [ ] 같은 이력서 PDF 로 백엔드 / 프론트엔드 직무 각각 인터뷰 생성 시 생성 main 질문 본문이 직무별로 다른 어휘 / 관점을 보임 (사용자 5건 샘플 비교에서 직무별 차이 외부 관찰 가능).
- [ ] 동일 이력서 5회 호출 생성 main 질문 중 표층 패턴 ("왜 X 사용 / 선택 / 채택", "X 의 장점") 일치 main 질문이 5건 묶음 기준 1건 이하 (절대 카운트).
- [ ] 생성된 main 질문 단건마다 5 깊이 유형 중 어느 유형에 해당하는지 사용자가 외부에서 식별 가능.
- [ ] 단일 인터뷰 main 질문 묶음 5건 샘플 검토 시 한 깊이 유형 편중이 명백히 없음 (한 유형 ≥4건 시 편중으로 간주, 채택 사유: 5건 묶음 = 균등 1건씩 + 1유형만 2건 허용).
- [ ] 추출된 skeleton 에 응시자가 이력서에 명시한 4종 깊이 신호 (의사결정 근거 / 비교 대안 / 수치 / 트레이드오프) 보존 — 3건 이력서 fixture 각각에 대해 이력서가 실제 명시한 신호 종류 기준 누락 ≤ 1건.
- [ ] 기존 Resume 트랙 인터뷰 생성 흐름이 사용자 체감 5초 이내 변동 내 완료되며 사용자가 회귀 (실패 / 빈 응답 / opener 누락) 경험하지 않음.

---

## Non-Goals

본 plan 이 **추구하지 않는 가치** (작업 절단 항목 = 비스코프, 분리).

- 정량 측정 자동화 / 통계적 신뢰성 = 본 plan 가치 아님 — 수동 샘플링으로 충분.
- 전 직무 (BE/FE/DevOps/데이터/풀스택) 균등 깊이 보장 = 본 plan 가치 아님 — 직무 분기 자체가 가치, 직무별 결과 품질 편차 허용.
- 표층 질문 0% = 본 plan 가치 아님 — 명백한 감소면 충분.
- 응시자 답변 reactive 깊이 추적 = 본 plan 가치 아님 — main 질문 생성 단계만.

---

## 수용 기준 (Acceptance Criteria)

검증 주체 = 사용자 (본 plan 작성자) 단독.

- [ ] 같은 이력서 PDF + 직무 백엔드 / 프론트엔드 각각으로 인터뷰 생성 시, 사용자가 main 질문 5건씩 비교했을 때 직무 특화 어휘 / 관점 차이가 명백히 외부 관찰 가능.
- [ ] 사용자가 저장된 main 질문 단건을 봤을 때 5 깊이 유형 (트레이드오프 / 한계 / 수치 / 대안 / 원리) 중 어느 유형에 해당하는지 외부에서 식별 가능.
- [ ] 동일 이력서로 5회 인터뷰 생성 후 main 질문을 사용자가 검토했을 때 표층 패턴 일치 main 질문이 5건 묶음 기준 1건 이하.
- [ ] 단일 인터뷰 main 질문 묶음 5건 표본 검토 시 한 깊이 유형 ≥4건 점유하는 케이스 부재.
- [ ] 사용자가 저장된 skeleton 을 봤을 때 응시자 이력서 본문에 명시된 의사결정 근거 / 비교 대안 / 수치 / 트레이드오프가 단순 한 줄 요약이 아니라 구조화된 형태로 외부 관찰 가능 (3건 이력서 샘플 검토 기준).
- [ ] 단일 인터뷰 main 질문 묶음 5건 중 트레이드오프 / 수치 / 대안 비교 유형 질문이 skeleton 신규 깊이 신호 내용을 인용하는 케이스 ≥ 1건 — 사용자가 main 질문 본문과 skeleton 내용 대조 시 외부 관찰 가능.
- [ ] 구버전 skeleton 데이터 (신규 깊이 신호 필드 부재) 가 적재된 기존 인터뷰 1건을 dev 환경에서 결과 화면으로 조회 시 정상 표시 — 회귀 미발생.
- [ ] Resume 트랙 인터뷰 생성 흐름이 변경 후에도 사용자 시연 1회 성공 — opener / main 적재 완료, 빈 응답 / 실패 / 시간 초과 미발생.
- [ ] before / after 샘플 비교 결과 (사용자 5건씩 직무 × 패턴 점검) 가 plan 폴더에 기록.

---

## 비스코프 (Don't)

본 plan **범위 외 작업** (향후 별도 plan 후보).

- 직무 enum 확장 (모바일 / ML 등) — 현재 5종 유지.
- 자동 측정 / eval 리포트 산출 / 운영 알림 인프라 구축 (축 C skeleton 보존률 자동 측정 포함 — 본 plan 은 수동 샘플링).
- A/B 비교 / feature flag 인프라.
- 일반 인터뷰 트랙 (사용자가 직무 + 유형 선택해 진행하는 별도 면접 흐름) 프롬프트 변경.
- 꼬리질문 (응시자 답변 reactive 후속 질문) 트랙 깊이 유형 분류.
- Opener 질문 깊이 유형 분류 (워밍업 성격, main 만 적용).

---

## 참고

- 관련 Issue: 신규 생성 X (사용자 결정, 본 세션 토론 기반 진행)
- 인접 plan:
  - `docs/plans/458-resume-skeleton-redesign/` (CLOSED) — skeleton 단순화 완료
  - `docs/plans/435-resume-model-answer-quality/` (CLOSED) — model_answer 품질 fix
  - `docs/plans/421-resume-playground-opener-tone/` — opener 톤
- 자산:
  - `backend/src/main/resources/prompts/base/*.yaml` (직무 페르소나 5종 — 축 A)
  - `backend/src/main/resources/prompts/overlay/{domain}/*.yaml` (스택 overlay 14종 — 축 A)
  - `backend/src/main/resources/prompts/template/resume/resume-question-generator.txt` (질문 생성 본문 — 축 A+B)
  - `backend/src/main/resources/prompts/template/resume/resume-extractor.txt` (skeleton 추출 본문 — 축 C)
  - `backend/src/main/java/com/rehearse/api/domain/resume/entity/ResumeSkeleton.java` (record + `@JsonIgnoreProperties` — 축 C 호환)
- Sprint: Interview Quality Sprint S14 후보
