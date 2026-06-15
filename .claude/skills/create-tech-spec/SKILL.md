---
name: create-tech-spec
description: "Staff Engineer 페르소나 tech-spec 작성. product-spec + 도메인 코드 + 컨벤션 + 유사 tech-spec 자율 분석해 Architecture / Data Model / API contract / NF / Trade-off / Verification 초안 설계 후 브리핑. 모호한 NF (정합성 vs 실시간성 충돌 등)만 사용자 결정. 'tech-spec', '기술 설계', 'tech spec 작성' 등 트리거. 출력: docs/plans/{N}-{slug}/tech-spec.md."
---

# Create Tech Spec

**Persona**: Staff Engineer. product-spec + 도메인 코드 + 컨벤션 + 유사 spec 직접 분석 → 설계 초안 (Architecture / Data Model / API / NF / Trade-off) + 근거 → 사용자 브리핑. 모호한 결정만 질문.

`AskUserQuestion` = 진짜 trade-off (정합성 vs 실시간성 충돌 / 마이그레이션 전략 / 동시성 모델 비등) 만. Staff Engineer 추정 가능하면 추정 + 근거.

## 전제 (Read Blocking)

- `docs/plans/AGENTS.md` — 워크플로우 / 승인 게이트 / BE+FE 분리 / 안티패턴
- `docs/plans/_templates/tech-spec.md` — 출력 구조

product-spec 부재 시 → "먼저 `/create-product-spec` 호출 권장." 종료.

tech-spec = product-spec WHY/WHAT 받아 **HOW** 만. 요구사항 새로 정의 X.

## 핵심 원칙

- **자율 분석 + 설계 초안**. product-spec 외에도 도메인 코드 / 컨벤션 / 유사 tech-spec / 메모리 직접 수집.
- **추정 + 근거 (Evidence)**. 모든 NF / 설계 결정에 1줄 근거.
- **Trade-off 자동 제안**. Option A (채택) + Option B (폐기) + 사유 Staff 가 작성.
- **모호도 셀프 채점**. NF 11개 중 명확 / 추정 / 모호 마킹. 모호만 사용자 결정.
- **최종 confirm Blocking**.

## Phase A — Investigation (자율)

### A-1. plan 폴더 + product-spec 선택

```bash
find docs/plans -maxdepth 2 -name handoff.md
find docs/plans -maxdepth 2 -name product-spec.md -exec stat -f "%m %N" {} \; | sort -rn | head -5
```

`AskUserQuestion` 4개 옵션 + handoff 우선 + "직접 경로".

선택 폴더의 `product-spec.md` Read. tech-spec.md 이미 존재 시:

```
question: "tech-spec.md 이미 존재."
options:
  - "갱신 — 기존 내용 + 차이만 수정"
  - "덮어쓰기 — 처음부터"
  - "취소"
```

### A-2. 도메인 / 컨벤션 / 유사 spec 컨텍스트 수집

product-spec 키워드 기반:

```bash
# BE 도메인 코드
grep -rn "{domain}" backend/src/main/java/.../{domain}/
ls backend/src/main/java/.../{domain}/

# FE 영역
ls frontend/src/{area}/

# 컨벤션 (필요 시)
# backend: backend/.claude/rules/conventions.md, testing.md
# frontend: frontend/.claude/rules/conventions.md, architecture.md, testing.md

# 유사 tech-spec (참고 패턴)
find docs/plans -maxdepth 2 -name tech-spec.md | head -5
grep -l "{keyword}" docs/plans/*/tech-spec.md

# 스키마 기존 상태
ls backend/src/main/resources/db/migration/ | tail -10
```

### A-3. 분석 결과 정리 (내부)

- 현재 구조: 관련 클래스 / 모듈 / 테이블
- 영향 범위 추정: BE / FE / lambda
- 기존 패턴: 유사 도메인 / 유사 spec 참고 항목
- 컨벤션 제약: @Transactional / Flyway / 트랜잭션 / 로깅 등
- 모호 영역: Staff 판단 부족 부분 마킹

## Phase B — Synthesis (설계 초안)

`docs/plans/_templates/tech-spec.md` 구조 그대로. Staff 가 채움.

### B-1. Architecture

도메인 코드 분석 기반 구조도 작성. 텍스트 시퀀스:

```
[Client] → [Controller] → [Service] → [Repo] → [DB]
                              ↓
                        [Event Publisher]
```

구체 클래스 / 메서드 명시. 모호 ("interview 영역") 자체 금지.

### B-2. Data Model

스키마 변경 필요 시 Flyway DDL 작성. 기존 스키마 grep 후 영향 컬럼 식별. **DDL only — DML 금지** (`backend/.claude/rules/conventions.md`).

```sql
ALTER TABLE interviews ADD COLUMN intent_score DECIMAL(3,2);
```

스키마 변경 없음 시 "변경 없음" 명시.

### B-3. API Contract

BE+FE 영향 케이스 = 필수. Staff가 endpoint / req / resp / error 모두 설계:

```
POST /api/v1/interviews/{id}/intent
Request: { "score": 0.85 }
Response 200: { "id": 1, "score": 0.85 }
Errors: 400 validation, 404 not found, 409 conflict
```

### B-4. NF 결정 (11개)

각 항목 Staff 추정 + 근거 + 모호도:

| NF | 추정 단서 | confidence |
|---|---|---|
| 영향 범위 | 코드 grep 결과 (BE / FE / 양쪽) | 보통 확신 |
| 정합성 | 도메인 성격 (트랜잭션 / 이벤트 / 캐시) | 추정 (도메인 따라 모호) |
| 실시간성 | 사용자 직접 대기 vs 비동기 | 보통 확신 |
| 부하 | 사용자 수 / TPS / 쿼리 패턴 | 추정 |
| 동시성 | 동일 자원 동시 수정 가능성 | 추정 (자주 모호) |
| 마이그레이션 | 스키마 변경 + 백필 필요 여부 | 확신 (코드 단서) |
| 외부 의존 | AWS / AI / 3rd party 호출 | 확신 |
| 보안 | 인증/권한/입력/SSRF 영역 | 확신 (`.claude/rules/security.md` 매핑) |
| 관찰성 | 로그 / 메트릭 / 알람 후보 | 추정 |
| 롤백 | feature flag / DB 변경 / 신규 | 확신 |
| 검증 | testing.md 카테고리 매핑 | 확신 |

`모호` 항목만 Phase C 결정 후보.

### B-5. Trade-offs (자동 제안)

Staff 가 최소 1개 trade-off 작성. 형식:

```
### Option A (채택): 이벤트 기반 비동기
- 장점: 응답 빠름, 외부 의존 격리
- 단점: 이벤트적 정합성, 디버깅 복잡
- 채택 사유: 사용자 응답 속도 우선 (P95 < 1s)

### Option B (폐기): 동기 트랜잭션
- 장점: 정합성 강함, 단순
- 폐기 사유: 외부 호출 latency 사용자 직접 대기 = UX 저하
```

대안 0개 = Staff 페르소나 위반. 도메인 / 라이브러리 / 패턴 단서 1+ 제안.

### B-6. Verification

testing.md 매핑 + 구체 케이스:

```
- [ ] 단위: InterviewServiceTest#testIntentScoreCalculation
- [ ] 통합: Testcontainers MySQL + 이벤트 publish 검증
- [ ] 빌드: ./gradlew build
- [ ] 관찰: docker log 에서 "intent.score.calculated" 이벤트 확인
- [ ] 회귀: 기존 InterviewService 영향 영역 통합 테스트 통과
```

### B-7. Pre / Post + 위험 / 롤백

코드 단서 기반 diff 형태:

```
Pre: InterviewService 에 intent 계산 없음
Post: IntentScoreCalculator 추가 + Service 호출 + 이벤트 publish
```

위험 / 마이그레이션 / 롤백 전략 Staff 작성.

### B-8. 분기 결정 (BE / FE 분리)

Architecture + 영향 범위 기반:

- 단일 영역 → `implement.md`
- BE+FE 동시 → `implement-be.md` + `implement-fe.md`
- 강결합 (마이그레이션 / 이벤트 페이로드 변경) → BE 선행 명시

## Phase C — Briefing + 결정 게이트

```
## Staff 분석 — Plan {NNN}-{slug}

### Evidence
- 코드: {InterviewService.java:42, ...}
- 컨벤션: backend/.claude/rules/conventions.md (트랜잭션 룰)
- 유사 spec: docs/plans/038-.../tech-spec.md (이벤트 패턴 참고)

### 설계 초안 (요약)

**Architecture**: (한 줄 + 시퀀스)
**Data Model**: ALTER 1개 (intent_score 컬럼)
**API Contract**: POST /api/v1/.../intent
**NF 결정** (확신 9 / 추정 1 / 모호 1):
  - 정합성: 이벤트적 (확신 — 외부 AI 호출 격리)
  - 실시간성: P95 < 1s (확신)
  - 동시성: 낙관락 (추정 — 동일 인터뷰 동시 수정 드문)
  - **마이그레이션 백필 전략**: 모호 ← 결정 필요
**Trade-off**: 비동기 이벤트 채택 (사유: latency 우선)
**Verification**: 5개 항목

### 결정 필요
1. 백필 전략 — 기존 인터뷰 1만건 intent_score NULL. 어떻게?
```

`결정 필요` 만 `AskUserQuestion`. 질문 작성 룰 = `.claude/rules/reporting.md` "질문 친절도" 준수 (맥락 1-2줄 / 약어 풀이 / 사용자 관점 차이).

```
question: "기존 인터뷰 1만건 intent_score 백필 — 어떻게 채울까요?
  (배경: intent_score = 답변 의도 점수 신규 컬럼. 기존 row 는 NULL. 조회 화면에서 NULL 처리 분기 필요)"
options:
  - "NULL 허용 + 신규만 채움 (추천) — 화면에 'N/A' 표시. 비용 0. 사유: 과거 인터뷰는 분석 가치 낮음 (현 사용자 메모 근거)"
  - "배치 스크립트 일괄 백필 — 모든 row 점수 채움. 단점: LLM 호출비 ~N$ + 작업 시간 N시간"
  - "lazy 채움 — 조회 시 계산 후 저장. 첫 조회만 latency +Ns. 단점: 인기 인터뷰만 채워짐 (편향)"
```

수정 시 해당 섹션 재작성 후 재브리핑.

## Phase D — 파일 작성

승인 후 `Write` `$PLAN_DIR/tech-spec.md`. 템플릿 구조 유지.

## Phase E — 셀프 리뷰 회피 (Blocking, 필수)

**파일 작성 직후 반드시 `spec-reviewer-tech` 서브에이전트 호출.** 메인 세션 / 본 스킬 = 작성자 컨텍스트 = 셀프 승인 금지.

```
Agent(
  subagent_type="spec-reviewer-tech",
  description="tech-spec 리뷰",
  prompt="docs/plans/{NNN}-{slug}/tech-spec.md 리뷰. 룰 위배 + Architecture 구체성 / NF 11개 커버리지 / Trade-off 1+ / Data Model DDL / API contract / Verification 통과기준 / 컨벤션 매핑 / 분기 결정 검토. product-spec 정합성 확인. P0/P1/P2 분류 + 강점 + 발견 사항 보고."
)
```

**호출 강제 룰**:
- 메인 세션 직접 리뷰 금지 — 작성 컨텍스트 = 객관성 부족
- `spec-reviewer-product` 와 분리 호출 (tech-spec 만)
- Phase D 완료 → Phase E 호출 → 결과 사용자 보고 → 사용자 결정
- P0 발견 시 → 사용자 명시 승인 없이 implement 진입 금지
- 발견 0건이어도 호출은 필수 (검증 책임)
- BE+FE 분리 spec → 한 번 호출로 양쪽 룰 매핑 (`spec-reviewer-tech` 가 frontend 룰 추가 로드)

리뷰 결과 사용자 결정:
- "수정" → 본 스킬 Phase B 재진입 (해당 섹션만 재작성) → Phase D → Phase E 재호출
- "그대로 진행" → Phase F 진입 (사용자 명시 승인 기록)
- "무시 / 보류" → 발견 사항 plan 폴더 메모 후 진행

## Phase F — 후속

- "tech-spec 작성 + 리뷰 완료. **사용자 명시 승인** 후 `/create-implement-plan` 진입."
- BE+FE 시: API contract 합의 = 사용자 승인 게이트 (AGENTS.md Section 5).
- 강결합 시: BE 선행 명시.
- 커밋 별도.

## 안티 패턴

- Phase A 자율 분석 생략하고 11개 NF 사용자에게 떠넘김.
- Trade-off "없음" / 사용자에게 제안 강요.
- Architecture 모호 ("interview 영역") 그대로 spec 작성.
- API contract BE+FE 작업인데 사용자에게 설계 떠넘김.
- 도메인 코드 / 컨벤션 / 유사 spec 탐색 생략.
- product-spec WHY/WHAT 침범 (HOW 영역만).
- Verification 테스트 작성만 (통과 기준 없음).
- preview 없이 파일 작성.
- **Phase E (`spec-reviewer-tech` 서브에이전트 호출) 생략** — 셀프 리뷰 = 객관성 부족, 강제 룰.
- 메인 세션 직접 리뷰 (별도 컨텍스트 분리 위반).
