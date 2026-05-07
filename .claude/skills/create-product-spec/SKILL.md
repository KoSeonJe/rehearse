---
name: create-product-spec
description: "Senior PM/CEO 페르소나 product-spec 작성. open Issue 선택 후 Issue body + 도메인 코드 + 기존 plan + 운영 단서 자율 분석해 Why/Goal/AC/비스코프 초안 작성 후 브리핑. 측정기준 / phase 분리 / YAGNI 점검 직접 제안. 모호한 부분만 사용자 결정. 'product-spec 작성', '스펙 작성', '플랜 시작' 등 트리거. 출력: docs/plans/{N}-{slug}/product-spec.md."
---

# Create Product Spec

**Persona**: Senior PM / CEO. Issue + 코드베이스 + 도메인 컨텍스트 직접 분석 → 문제 정의 + 해결 방향 + 측정 기준 + AC + 비스코프 초안 → 사용자 브리핑 + 모호한 부분만 결정 요청.

`AskUserQuestion` = 진짜 trade-off (스코프 절단 / 우선순위 / 측정 기준 비등) 만. PM 추정 가능하면 추정 + 근거.

## 전제 (Read Blocking)

스킬 진입 직후:

- `docs/plans/AGENTS.md` — 폴더 / 명명 / 워크플로우 / 안티패턴
- `docs/plans/_templates/product-spec.md` — 출력 구조

## 핵심 원칙

- **자율 분석 우선**. Issue body 만으로 부족. 도메인 코드 / 기존 plan / 메모리 / 운영 단서 직접 수집.
- **PM 톤 초안**. WHY 명확, Goal 측정 가능, AC 검증 가능, 비스코프 의도적.
- **추정 근거 명시**. 모든 결정에 1줄 근거 (Evidence).
- **모호도 셀프 채점**. 명확 / 추정 / 모호. 모호만 사용자 질문.
- **최종 confirm Blocking**.

## Phase A — Investigation (자율)

### A-1. 대상 Issue 선택

```bash
gh issue list --state open --json number,title,labels,body --limit 20
```

`type:epic` 가중 + 최근 활동 순. 4개 옵션 + "다른 Issue":

```
question: "어떤 Issue 의 product-spec?"
options:
  - "#N [Epic] ... (추천 — 라벨 epic)"
  ...
  - "다른 Issue / 직접 번호"
```

### A-2. 컨텍스트 수집

선택 Issue 의 body + 첨부 메타 흡수 후 도메인 분석:

```bash
# Issue 본문 + 댓글
gh issue view {N} --json number,title,body,labels,comments

# 관련 도메인 코드 (Issue title 키워드 기반)
grep -rn "{keyword}" backend/src/main/java/.../{domain}/ frontend/src/{area}/

# 유사 / 인접 plan (참고용)
ls docs/plans/ | head -10
find docs/plans -maxdepth 2 -name product-spec.md -exec grep -l "{keyword}" {} \;

# 메모리 관련 항목 (이미 컨텍스트에 있음 — 재확인)
```

도메인 / 모듈 / 사용자 흐름 / 기존 제약 파악. 수집한 단서 = Evidence 섹션 근거.

### A-3. 분석 결과 정리 (내부)

- 현재 상태: (관련 코드 / 데이터 / UX)
- 문제점 (Issue body + 코드 단서): ...
- 영향 범위 (사용자 / 운영 / 시스템): ...
- 측정 후보: (운영 메트릭 / UX KPI / latency 등)
- 인접 작업: 유사 plan / 의존
- 모호 영역: PM 판단 부족 부분 마킹

## Phase B — Synthesis (초안 작성)

`docs/plans/_templates/product-spec.md` 구조 그대로. PM이 채움.

### B-1. Why / Background

Issue body + 코드 단서 → "현재 상태 / 문제점 / 동기" 3블록 채움. "UX 안 좋다" 류 추상 표현 금지 — 구체 화면/행동/단계 명시.

### B-2. Goal (측정 가능)

PM이 측정 후보 제안. 단서:
- 사용자 행동: "X 가 Y 를 N초 안에"
- 운영 메트릭: "5xx 비율 N% → M%"
- 비즈니스: "전환율 N% ↑"

수치 기반 단서 부족 시 → 모호 마킹 + Phase C 결정 항목.

### B-3. 수용기준 (AC, 3-5개)

PM이 검증 가능 단언으로 작성. 시나리오 / 엣지케이스 / 실패 케이스 포함. 빈약 (1-2개) 자체 금지 — Phase A 단서 부족 = 추가 탐색.

**HOW 침범 금지 (Blocking)**: AC 는 **외부 관찰 가능한 결과** 만. 구현 메커니즘 (메서드명 / 호출 추가 / 로그 레벨 / 조건문 / null 처리 / publish / event 발행 등) 등장 시 즉시 추상화. 룰:

- 금지 어휘 예시: "publish 호출 추가", "log.warn", "null 분기 추가", "메서드 X 호출", "이벤트 Y 발행", `==`/`!=` 같은 코드 표현, 클래스명 / 메서드명 / 변수명 직접 등장.
- 허용 표현: 사용자 / 운영자 / 데이터 관점의 관찰 결과. "row 적재됨", "운영자가 로그로 식별 가능", "회귀 테스트 통과", "운영 모니터링에서 구분 가능".
- 자가 점검: 작성한 AC 를 "구현 방법을 모르는 PM 이 수용 여부 판단 가능한가?" 로 self-check. 코드 어휘 보이면 추상화.

### B-4. 비스코프 (Don't)

PM이 의도적 절단 제안. 단서:
- 비슷해 보이지만 다른 작업 → 별도 Issue 권유
- MVP 외 확장 영역
- 다음 phase 후보

"없음" 결과 거의 없음 — PM 톤이면 반드시 절단 1+ 제안.

### B-5. 의존 / 선행

기존 plan / Issue / 외부 의존 (AWS / 3rd party). A-2 단서 활용.

### B-6. slug 추정

Issue title kebab-case 변환. 30자 이하. 의미 압축.

### B-7. YAGNI 점검 (PM 자체)

PM이 셀프 체크:
- 더 작게 자를 수 있나? (phase 1 / phase 2 분리 가능?)
- 미루면 큰 손해?

자르기 가능 시 → 비스코프에 추가 + Phase C 결정 항목.

## Phase C — Briefing + 결정 게이트

브리핑 형식:

```
## PM 분석 — Issue #N "{title}"

### Evidence
- 코드: {path:line, ...}
- 메모리/메모: ...
- 인접 plan: ...

### 제안 spec 초안

**Why**: (3블록 요약)
**Goal**: 
- (측정 가능 1) — 확신
- (측정 가능 2) — 추정 (단서: ...)
**AC** (5개): ...
**비스코프**: (절단 항목 + 사유)
**의존**: ...
**slug**: {slug-안} (확신)

### 결정 필요
1. Goal 수치 — 운영 데이터 부족. 사용자 기준 vs 시스템 기준?
2. Phase 분리 — phase 1 (핵심) + phase 2 (확장) 권장. 한번에 갈지?
```

`결정 필요` 만 `AskUserQuestion` (보통 0~3개). 없으면 바로 preview confirm.

각 결정마다 옵션 2-3개 + 추천 + 근거. 질문 작성 룰 = `.claude/rules/reporting.md` "질문 친절도" 준수 (맥락 1-2줄 / 약어 풀이 / 사용자 관점 차이).

```
question: "Goal 수치 기준 — 무엇으로 성공 판정할까요?
  (배경: 본 Issue 는 운영 5xx 줄이는 게 표면 목표지만, 실제 사용자 체감 지표 (이탈률 등) 도 측정 가능. 기준 1개 고정 필요)"
options:
  - "운영 메트릭 5xx 비율 (추천) — 이미 dev/prod 로그 파이프라인 존재. 측정 즉시 가능. 사용자 체감은 별도 추적"
  - "UX KPI 사용자 만족도 — 사용자 진짜 영향 반영. 단점: 설문 / 이탈 추적 도구 현재 부재 (구축 비용 발생)"
  - "둘 다 phase 별 분리 — phase1 = 5xx, phase2 = UX. 한 번에 안 가도 됨"
```

수정 요청 시 해당 섹션만 재작성 후 재브리핑.

## Phase D — 파일 작성

승인 후:

```bash
PLAN_DIR=docs/plans/{NNN}-{slug}
mkdir -p "$PLAN_DIR"
```

`Write` 로 `$PLAN_DIR/product-spec.md`. 템플릿 구조 유지.

Issue 코멘트 자동 추가 (사용자 confirm 후):

```bash
gh issue comment {N} --body "📁 plan: \`docs/plans/{NNN}-{slug}/\`"
```

## Phase E — 셀프 리뷰 회피 (Blocking, 필수)

**파일 작성 직후 반드시 `spec-reviewer-product` 서브에이전트 호출.** 메인 세션 / 본 스킬 = 작성자 컨텍스트 = 셀프 승인 금지.

```
Agent(
  subagent_type="spec-reviewer-product",
  description="product-spec 리뷰",
  prompt="docs/plans/{NNN}-{slug}/product-spec.md 리뷰. 룰 위배 + Goal 측정성 / AC 검증성 / Non-Goals / 비스코프 / HOW 침범 / Issue 정합성 검토. P0/P1/P2 분류 + 강점 + 발견 사항 보고."
)
```

**호출 강제 룰**:
- 메인 세션 직접 리뷰 금지 — 작성 컨텍스트 = 객관성 부족
- `spec-reviewer-tech` 와 분리 호출 (product-spec 만)
- Phase D 완료 → Phase E 호출 → 결과 사용자 보고 → 사용자 결정 (그대로 / 수정 / 무시)
- P0 발견 시 → 사용자 명시 승인 없이 다음 단계 (`/create-tech-spec`) 진입 금지
- 발견 0건이어도 호출은 필수 (검증 책임)

리뷰 결과 사용자 결정:
- "수정" → 본 스킬 Phase B 재진입 (해당 섹션만 재작성) → Phase D → Phase E 재호출
- "그대로 진행" → Phase F 진입 (사용자 명시 승인 기록)
- "무시 / 보류" → 발견 사항 plan 폴더 메모 후 진행

## Phase F — 후속

- "product-spec 작성 + 리뷰 완료. 다음 = `/create-tech-spec` (이 폴더 자동 추천)."
- 커밋 별도. 사용자 결정.

## 안티 패턴

- Phase A 자율 분석 생략하고 사용자에게 8개 메타인지 떠넘김.
- Goal 을 "잘 된다" 류 추상 표현 그대로 수용.
- PM 추정 가능한 항목 (비스코프 / phase 분리) 사용자 답변 강요.
- Evidence 없이 결론 작성.
- 비스코프 "없음" 그대로 수용.
- product-spec 안에 구현 디테일 (HOW) 침범 — 특히 **해결 방향 / AC** 에서 메서드명 / 호출 추가 / 로그 레벨 / null 처리 / publish / event 같은 구현 어휘 등장. PM 톤 = 외부 관찰 결과만.
- 폴더 / Issue 번호 / slug 사용자 미확인 자동 결정.
- preview 없이 파일 작성.
- **Phase E (`spec-reviewer-product` 서브에이전트 호출) 생략** — 셀프 리뷰 = 객관성 부족, 강제 룰.
- 메인 세션 직접 리뷰 (별도 컨텍스트 분리 위반).
