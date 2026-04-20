# Interview Quality 2026-04-20 — 계획 검증 리포트

> 작성일: 2026-04-21
> 범위: 14개 plan (00a~12) + 6개 지원 문서 + 실제 코드 교차 검증
> 목적: (1) AI 모델 드리프트 방지 확인, (2) 개선 논거 실효성 검증, (3) 장애/회귀 리스크 도출
> Source plan: `/Users/koseonje/.claude/plans/interview-quality-wild-pond.md`

---

## Section A — AI 모델 드리프트 전수 점검

**현재 스택 (CLAUDE.md + application-prod.yml 기준)**
- Backend Primary: `gpt-4o-mini` (OpenAI)
- Backend Fallback: `claude-sonnet-4-20250514` (Claude, 일반)
- Backend Follow-up 전용 상수: `claude-haiku-4-5-20251001` (ClaudeApiClient.FOLLOW_UP_MODEL)
- Lambda: Gemini 주력 + OpenAI Whisper/Vision

### A1. plan × 모델 사용 매트릭스

| plan | 지정 라인 | 모델 ID | 형태 | 스택 일치? |
|------|----------|---------|------|-----------|
| 01 | 57 | gpt-4o-mini / claude-haiku | application.yml 참조 | ✅ |
| 02 | 47 | gpt-4o-mini / claude-haiku | application.yml 참조 | ✅ |
| 03 | 55 | gpt-4o-mini | application.yml 참조 | ✅ |
| 04 | 60 | gpt-4o-mini (Compactor) | application.yml 참조 | ✅ |
| **05** | **58** | **gpt-4o** + **claude-sonnet** | **modelOverride 하드코딩** | ⚠️ |
| 06 | 62 | gpt-4o-mini | application.yml 참조 | ✅ |
| 07 | 81~84 | plan-01/02/04 재사용 | 참조 | ✅ |
| 08 | 288 | gpt-4o-mini | application.yml 참조 | ✅ |
| **09** | **87** | gpt-4o-mini 기본 **/ gpt-4o flag** | 선택적 업그레이드 | ⚠️ |
| 10 | — | 다중 (측정용) | 테스트 | ✅ |
| 11 | — | LLM 호출 0 (결정론) | — | ✅ |
| 12 | — | 해당 없음 | — | ✅ |

### A2. 교차 검증 — plan-00b modelOverride 구현

`OpenAiClient.java` / `ClaudeApiClient.java` 는 `req.modelOverride()` 가 비어있으면 `application.yml` 값을 사용. 기본값은 `gpt-4o-mini` / `claude-sonnet-4-20250514` → **스택 유지 기본 동작 OK**.

### A3. Findings

| ID | 심각도 | 문제 | 근거 |
|----|--------|------|------|
| **A-F1** | Major | plan-05 Resume Extractor 가 `modelOverride="gpt-4o"` 로 **gpt-4o-mini → gpt-4o 업그레이드** 하드코딩. 사용자 요구 "사용하던 모델 그대로" 위반 | plan-05:58 |
| **A-F2** | Major | plan-05 fallback 도 `claude-sonnet` 로 명시 (기존 사용 버전과 일치하지만 Haiku 가 아닌 Sonnet 고정). 코스트 정당화 근거 미기재 (월 $500 추정만) | plan-05:58 |
| **A-F3** | Minor | plan-09 Synthesizer 가 "flag 로 gpt-4o 승격 가능" 옵션 포함. 기본값은 gpt-4o-mini 라 즉시 드리프트는 아니지만 flag 활성 시 모델 변경 발생 | plan-09:87 |
| **A-F4** | Minor | 10+개 plan 모두 `modelOverride` 미지정 → application.yml 기본값 사용. 현 스택 유지 확인 | 실측 |

### A4. 조치 제안

1. **plan-05 교정 (필수)**: `modelOverride="gpt-4o"` 를 **제거** 하거나 `${rehearse.features.resume-extractor.model:gpt-4o-mini}` 형태의 **application.yml 참조**로 변경. 기본값은 `gpt-4o-mini` 유지하고 품질 부족 확인 시 flag 로 ON.
2. **plan-09 교정 (권장)**: flag 기본값 `false` 명시 + Exit Criteria 에 "flag OFF 상태로도 J3 목표 달성" 조건 추가.
3. `application-prod.yml` 에 `rehearse.features.*.model` 필드 표준화 → 모든 plan 이 하드코딩 없이 설정 기반 모델 선택.

---

## Section B — Latency / Cost 누적 SLA 분석

### B1. 턴당 LLM 호출 체인 (기본 경로, Non-Resume, 모든 flag ON)

```
[사용자 답변]
  → L1 Intent Classifier         (plan-01, gpt-4o-mini, ~500ms)  [1]
  → L2 Answer Analyzer           (plan-02, gpt-4o-mini, ~1.5s)   [2]
  → L3 Follow-up Generator v3    (plan-03, gpt-4o-mini, +2s)     [3]
  → L4 Rubric Scorer (턴 종료)    (plan-08, gpt-4o-mini, ~1.5s)   [4]
  → (비동기) DialogueCompactor    (plan-04, 백그라운드)            [-]
```

### B2. 개별 SLA vs 누적 SLA

| 단계 | plan 개별 SLA | 인용 |
|------|--------------|------|
| Intent | +500ms | plan-01 |
| Analyzer | +1.5s | plan-02 |
| Follow-up v3 | +2s (누적) | plan-03 |
| Rubric Scorer | 미명시 | plan-08 |
| **누적 p95 추정** | **≥ 4.5s** | — |

### B3. Findings

| ID | 심각도 | 문제 |
|----|--------|------|
| **B-F1** | **Critical** | 턴당 4회 순차 LLM 호출인데 **aggregate p95 SLA 미정의**. baseline ~1~2s 가 4~5s 로 증가하면 UX 관점 regression. 각 plan 의 개별 SLA 는 선언됐으나 합쳐서 평가되지 않음 |
| **B-F2** | Major | plan-08 Rubric Scorer 의 **turn-level latency 허용치 선언 없음**. 턴마다 호출이면 Follow-up 응답 직후 block 될 수 있음 (또는 비동기 후처리?) — plan-08 본문에 명시 필요 |
| **B-F3** | Major | plan-04 DialogueCompactor 는 비동기 백그라운드 (+$0.001/호출) 로 잘 설계됐으나, **10턴 넘는 세션에서 압축 지연 > 3턴이면 동기 fallback** → 장기 세션 마지막 턴 latency 스파이크 가능 (plan-04:61) |
| **B-F4** | Minor | plan-11 은 LLM 0 (결정론 매퍼) → latency 영향 없음. Lambda 비언어 분석 자체는 기존 Gemini 경로 재사용 |

### B4. 코스트 재산정 (10턴 세션 × GPT-4o-mini 기준)

| 호출 | tokens (in+out) | $/call | 10턴 합계 |
|------|----------------|--------|----------|
| Intent ×10 | 500+100 | ~$0.0001 | $0.001 |
| Analyzer ×10 | 2000+500 | ~$0.0005 | $0.005 |
| Follow-up ×10 | 3000+1000 | ~$0.001 | $0.01 |
| Rubric ×10 | 1500+500 | ~$0.0004 | $0.004 |
| Compactor ×1.5 | 2000+500 | ~$0.0005 | $0.0008 |
| **세션 합계** | | | **~$0.021** |

**월 1만 세션 = $210 / 월 10만 세션 = $2.1k** — 수용 가능 범위. Resume Track 에서 A-F1 (gpt-4o 사용) 시 세션당 ~$0.15 → 월 10k 세션 = $1.5k 추가.

### B5. 조치 제안

1. **각 plan 에 aggregate p95 SLA 추가** (권고치: 4s). plan-01/02/03/08 상단에 "누적 SLA: 4s" 섹션.
2. plan-08 Rubric Scorer 를 **비동기 post-turn 경로**로 명시 — 사용자 응답 latency 에 영향 없도록.
3. plan-04 Compactor 동기 fallback 임계(3턴)를 5턴으로 완화 또는 명시적 timeout.

---

## Section C — 현재 코드 대비 plan 본문 불일치 (교정 패치)

### C1. 검증된 불일치 (grep + find 실측)

**존재하지 않는 클래스** (find 결과: 전부 미존재):
- `FollowUpQuestion.java` ❌
- `InterviewSession.java` ❌
- `InterviewTurnService.java` ❌
- `InterviewCompletedEvent.java` ❌ (별도 확인 필요)

### C2. plan 본문 교정 지시

| Plan | 라인 | 현재 문구 (Before) | 교정 문구 (After) | 증거 |
|------|-----|--------------------|-------------------|------|
| **plan-03** | 23 | `domain/interview/FollowUpQuestion.java` 수정. targetClaimIdx 필드 추가 | **경로 미확정**. `FollowUpQuestion` 클래스가 실재하지 않음 → 실제 FollowUp DTO 위치 확인 후 수정. 가능한 후보: `domain/interview/dto/FollowUpResponse.java` 또는 신규 생성 | find 결과 |
| **plan-06** | 23 | `domain/interview/InterviewSession.java` 수정. InterviewPlan 참조 추가 | `domain/interview/runtime/InterviewRuntimeState.java` (plan-00c 산출) 에 `interviewPlanRef` 필드 추가. InterviewSession 클래스 없음 | INVENTORY:107 |
| **plan-06** | 80 | 검증 항목: Plan 이 `InterviewSession` 에 정상 직렬화 | 검증 항목: Plan 이 `InterviewRuntimeState` 또는 V25 `interview_plan` 테이블에 정상 저장 | INVENTORY:46 |
| **plan-07** | 107 | Review: architect-reviewer — 기존 `InterviewTurnService` 확장 vs 분리 판단 | Review: architect-reviewer — `FollowUpService` 확장 vs `ResumeInterviewOrchestrator` 분리 판단 | plan-00a:108 |
| **plan-07** | 124 | `./gradlew test --tests "InterviewTurnServiceTest"` | `./gradlew test --tests "FollowUpServiceTest"` | HANDOFF:20 |

### C3. 이미 IMPACT_MAP 에서 교정됐으나 plan 본문 미반영 항목

REMEDIATION.md §Root Cause 3 는 "plan 본문 편집은 각 plan 실행 직전 executor 에게 위임"이라 명시했음. **현재 S3 이후 실행 예정인 plan-03/06/07 의 실제 PR 생성 직전 먼저 문서 edit PR 이 필요**. 그렇지 않으면 서브에이전트가 잘못된 경로를 또 만든다.

### C4. Findings

| ID | 심각도 | 항목 |
|----|--------|------|
| **C-F1** | **Critical** | plan-06 본문에 `InterviewSession` 2회 참조 (23, 80) — 실재 않는 클래스 기반으로 영속화 설계 기재. S7 실행 시 잘못된 경로 |
| **C-F2** | Major | plan-07 본문에 `InterviewTurnService` 2회 참조 (107, 124) — 리뷰 기준/테스트 커맨드 오류 |
| **C-F3** | Major | plan-03 `FollowUpQuestion` 경로 미확정 — IMPACT_MAP:147 도 "경로 확인 필요" 로만 표기. 실행 전 grep 필수 |

---

## Section D — 설계 결함 / 장애 시나리오

### D1. Flyway 버전 충돌 — 해소 ✅

- 현재 마이그레이션 최대: **V23** (`V23__add_tts_text_columns.sql`)
- plan-00c V24~V27 + plan-11 V28 → **충돌 없음**. 사전 확인 완료.

### D2. Multi-Node Session Drift (Caffeine in-memory)

**현재 배포 토폴로지 확인**:
- Terraform 파일 없음
- K8s manifests 없음
- `infra/dev-instance-scheduler.yaml` 만 존재 (EC2 스케줄링)
- `backend/docker-compose.yml`, `.prod.yml` → 단일 컨테이너 배포

**결론**: 현재 **단일 EC2 인스턴스 배포** → Caffeine in-memory 안전.

| ID | 심각도 | 항목 |
|----|--------|------|
| **D-F1** | Minor (현재) | Caffeine 2h TTL 정상 동작. 단, **향후 scale-out 시 Redis 이관 반드시 필요**. plan-00c 본문에 "현재 단일 인스턴스 전제, multi-node 이전에 Redis 선행" 경고 명시 권고 |

### D3. Lambda Nonverbal 스키마 호환성 ❗

**plan-11 요구 필드 vs 현재 Lambda 실측**:

| 필드 | 필요한 곳 | 현재 존재? | 유사명 |
|------|----------|-----------|--------|
| `speed_variance` | D11 Fluency | ❌ 없음 | — |
| `gaze_on_camera_ratio` | D13 Eye Contact | ❌ 없음 | `eyeContactLevel` (enum) |
| `posture_unstable_count` | D13 Posture | ❌ 없음 | `postureLevel` (enum) |

| ID | 심각도 | 항목 |
|----|--------|------|
| **D-F2** | **Critical** | plan-11 은 Lambda 출력에 **3개 신규 필드를 전제**하지만 실제 `verbal_analyzer.py` / `vision_analyzer.py` / `*_prompt_factory.py` 에는 **전부 부재**. 기존 enum (`GOOD/AVERAGE/NEEDS_IMPROVEMENT`) 만으로는 결정론 threshold 매핑 (`gaze<0.3→1, gaze>0.7→3`) 불가능 |
| **D-F3** | Major | plan-11:19 "필드 없을 가능성" 으로 표기 + "프롬프트 개편 Out-of-Scope" → **실행 시 공백 발생**. Gemini 프롬프트 개편 없이 plan-11 착수 불가 |

**조치 제안**:
- plan-11 을 **3-phase 분리**: (a) Gemini 프롬프트 확장 (신규 plan-11a), (b) Lambda mapper 구현 (기존 plan-11), (c) Backend 저장 및 context_weights (기존 plan-11 유지)
- 또는 기존 enum → 수치 변환 fallback (`GOOD=0.8, AVERAGE=0.5, NEEDS=0.2` 등) 로 D13 우회 구현

### D4. Verbal/Vision 비동기 Timeout 계약

- `lambda/analysis/handler.py` 는 `failure_reason` / `failure_detail` 필드로 에러 전달 (TIMEOUT/API_ERROR/TRANSCRIPTION_ERROR/VISION_ERROR/INTERNAL_ERROR)
- `isVerbalCompleted` / `isNonverbalCompleted` boolean flag 존재

| ID | 심각도 | 항목 |
|----|--------|------|
| **D-F4** | Major | plan-09 가 "10분 timeout 시 Delivery null 유지" 로 설계했지만, **현재 Lambda 가 명시적 에러 필드를 제공함에도 plan-09 본문에 소비 경로 미기재**. → Lambda 실패 vs 진행 중 vs 도착 안 함 구분 가능한데 쓰지 않음. plan-09 에 `failure_reason` 기반 partial feedback 전략 추가 필요 |

### D5. D9 Factual Consistency Dead Score

- plan-08: D9 (이력서 claim vs 답변 일치) 점수 저장
- plan-07: `resume-chain-interrogator.txt` 에서 `fact_check_flag` **삭제** 확정 → 실시간 분기 사용 안 함
- plan-09: D9 점수는 "종합 피드백 narrative" 에만 반영

| ID | 심각도 | 항목 |
|----|--------|------|
| **D-F5** | Minor | D9 는 의도적 deferral (post-session 만). Resume Track 의 핵심 가치("이력서-답변 불일치 즉시 지적")와 어긋나지만 MVP 범위 축소 결정. 정책적 선택 OK. 단, plan-07 본문에 **"D9 는 post-session 만, in-session 분기 없음"** 주석 명시 권고 |

### D6. Feature Flag OFF 기본값 회귀 방어

- S2 (00b) 에서 `rehearse.features.*` 기본값 **false** 확인
- 모든 신규 경로는 flag OFF 시 기존 FollowUpService 로직 유지
- S2 테스트 결과: **643 tests / 0 failures** (baseline 606 → +37 그린)

| ID | 심각도 | 항목 |
|----|--------|------|
| **D-F6** | ✅ 해소 | Flag OFF 기본 상태에서 회귀 0. S2 테스트로 검증됨 |

---

## Section E — 종합 판정 및 우선순위 수정 지시

### E1. Critical (착수 전 필수 해결)

| # | Finding | 조치 | 영향 plan |
|---|---------|------|----------|
| 1 | **A-F1** gpt-4o 모델 드리프트 | plan-05:58 에서 `modelOverride="gpt-4o"` 제거 → application.yml 기본(`gpt-4o-mini`) 사용 또는 `rehearse.features.resume-extractor.model` flag 경유 | plan-05 |
| 2 | **B-F1** 턴당 4회 LLM 호출 aggregate SLA 부재 | 각 plan 에 "누적 p95 ≤ 4s" SLA 명시 | plan-01/02/03/08 |
| 3 | **C-F1** plan-06 `InterviewSession` 2회 오참조 | plan-06 본문 edit: `InterviewRuntimeState` / V25 로 전환 | plan-06 |
| 4 | **D-F2** plan-11 필드 3개 Lambda 부재 | plan-11 을 prompt 개편 단계와 분리하거나 enum→수치 fallback 정의 | plan-11 |

### E2. Major (해당 plan 실행 전 해결)

- **A-F2** plan-05 Sonnet fallback 코스트 가드 추가
- **B-F2** plan-08 Rubric Scorer 비동기 경로 명시
- **B-F3** plan-04 Compactor 동기 fallback 임계 명시
- **C-F2** plan-07 `InterviewTurnService` 2회 오참조 수정
- **C-F3** plan-03 `FollowUpQuestion` 실제 경로 확인 후 교정
- **D-F3** plan-11 Gemini 프롬프트 개편 선행 plan-11a 분리
- **D-F4** plan-09 Lambda `failure_reason` 소비 전략 추가

### E3. Minor (문서 주석 수준)

- **A-F3** plan-09 flag 기본 false 명시
- **A-F4** 전체 모델 드리프트 검증 ✅
- **D-F1** plan-00c Redis 이관 조건 명시
- **D-F5** plan-07 D9 in-session 미사용 주석

### E4. 이미 해소된 항목

- ✅ **D-F6** Flag OFF 기본 회귀 방어 (S2 643 테스트 그린)
- ✅ **D2** Flyway V24~V28 번호 충돌 없음 (현재 V23)
- ✅ **A-F4** 대부분 plan 이 application.yml 참조로 스택 유지

### E5. GO/NO-GO 판정

| Phase | 판정 | 조건 |
|-------|------|------|
| Phase 0 (S3~S5, plan-00c~00f) | **GO** | 이미 설계 정합. 00c 의 Redis 주석만 추가 |
| Phase 1 (S6, plan-01) | **GO** | plan-01 은 교정 불필요, S2 인프라 위에서 착수 가능 |
| Phase 2 (S7, plan-02/03) | **Conditional GO** | plan-03 `FollowUpQuestion` 경로 확인 필요 |
| Phase 3 (S8~, plan-04/05/06/07) | **Conditional GO** | **plan-05 모델 드리프트 제거 필수**, plan-06/07 `InterviewSession`/`InterviewTurnService` 오참조 수정 필수 |
| Phase 4 (S10~, plan-08/09/10/11) | **NO-GO (plan-11 만)** | **plan-11 실행 전 Gemini 프롬프트 개편 선행 plan 필요**. plan-08/09/10 은 Conditional GO |

### E6. 종합 결론

1. **AI 모델 드리프트**: plan-05 의 `gpt-4o` 하드코딩 1건이 유일한 Critical 드리프트. 그 외 전부 `application.yml` 참조 = 현재 스택 유지. → **plan-05 수정만으로 사용자 요구 충족**.
2. **개선 논거 실효성**: 5대 아픈 지점 개선 설계는 전반적으로 논리적. 단, 턴당 4회 LLM 호출 누적 latency 가 UX 를 해칠 위험이 정량화되지 않음 → aggregate SLA 추가로 해결 가능.
3. **회귀/장애**: S2 까지의 인프라 (00a, 00b) 는 회귀 없음. Phase 3 의 Resume Track 과 Phase 4 의 plan-11 이 가장 큰 미해결 리스크. plan-11 은 Lambda 스키마 선행이 없으면 실행 불가.

**스프린트 전체는 "핵심 구조적 개선"으로 진행 가치 있음. 단 plan-05 / plan-11 의 문서 교정과 aggregate SLA 추가 3가지가 착수 전 블로커.**

---

## 참조

- 마스터 플랜: `requirements.md`
- 진행 상태: `progress.md`
- Critic 대응: `REMEDIATION.md`
- 코드 지도: `INVENTORY.md`, `IMPACT_MAP.md`
- 세션 로그: `HANDOFF.md`
- 검증 방법 플랜: `/Users/koseonje/.claude/plans/interview-quality-wild-pond.md`
