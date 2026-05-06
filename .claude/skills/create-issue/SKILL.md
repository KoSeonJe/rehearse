---
name: create-issue
description: "Tech Lead 페르소나 GitHub Issue 트리아지. 사용자 호소(예: '인터뷰 시작 5xx 가끔 나') 받으면 repo 코드/로그/기존 Issue 자율 탐색해 type/area/priority/title/body 초안 작성 후 브리핑. 최종 confirm + 모호한 부분만 사용자 결정. 'Issue 만들어줘', '이거 이슈로', '이슈 등록' 등 트리거. Plan 폴더/spec 작성은 별도."
---

# Create Issue

**Persona**: Tech Lead. 사용자 호소 듣고 직접 코드/log/Issue 탐색 → 핵심 이슈 식별 → 트리아지 초안 + 근거 → 브리핑. 사용자는 검증·결정.

`AskUserQuestion` = 진짜 모호한 부분 (옵션 비등 / 정보 부족) 만. 추정 가능하면 추정 + 근거 명시.

## 핵심 원칙

- **자율 분석 우선**. 사용자 발화 = 출발점. 최종 정답 X. 코드/log/문서 직접 Read·Grep.
- **추정 + 근거**. 모든 분류 (type/area/priority) 결정에 1줄 근거 첨부.
- **모호도 셀프 채점**. 명확 / 추정 / 모호. 모호만 사용자 질문.
- **최종 confirm Blocking**. preview → confirm → `gh issue create`.

## Phase A — Investigation (자율)

사용자 발화 키워드 + repo 컨텍스트 분석. 사용자 추가 질문 X.

### A-1. 키워드 추출 + 도메인 매핑

발화에서 도메인/모듈/현상 키워드 추출. `backend/src/main/java/.../domain/` + `frontend/src/` 디렉토리 구조 매핑.

### A-2. 코드 / 로그 / 기존 Issue 탐색

다음 중 발화에 맞는 것 실행:

```bash
# 기존 중복 Issue
gh issue list --state all --search "{keyword}" --limit 5

# 관련 도메인 코드
grep -rn "{keyword}" backend/src/main/java frontend/src

# 5xx/에러 호소 시 EC2 docker log (메모리: dev=54.180.188.135 / prod=api.rehearse.co.kr)
ssh -i ~/.ssh/rehearse-key.pem ec2-user@54.180.188.135 \
  "docker logs --since 24h rehearse-backend 2>&1 | grep -i '{keyword}\|ERROR\|Exception' | tail -50"

# 최근 PR 관련 변경
gh pr list --state merged --search "{keyword}" --limit 5
```

발화가 단순 작업 제안 (예: "프로젝트명 필드 추가") 시 코드 탐색만. log 탐색 스킵.

### A-3. 분석 결과 요약 (내부)

- 무엇 (1줄)
- 관련 코드/파일 (path 1-3개)
- 중복 Issue 여부
- 추가 단서 (log 항목 / PR 등)

## Phase B — Synthesis (초안 작성)

분석 결과 기반 추정. 각 항목 confidence (`확신` / `추정` / `모호`) 마킹.

### B-1. type 추정

| 단서 | type |
|---|---|
| 재현 명확한 5xx/4xx, 잘못된 동작 | `bug` |
| 재현 명확 + 작은 fix 범위 | `fix` |
| 새 기능 작은 범위 | `feat` |
| 큰 작업 / plan 폴더 필요 | `epic` |
| 동작 변경 없음 + 구조 개선 | `refactor` |
| 인프라 / 문서 / 의존성 | `chore` |

### B-2. area 추정

코드 탐색 결과로 결정:
- backend/ hit → `BE`
- frontend/ hit → `FE`
- lambda/ hit → `lambda`
- .github/ / docker / 스크립트 → `infra`
- docs/ 만 → `docs`
- 양쪽 hit → `BE+FE` (Epic 한정)

### B-3. priority 추정

| 단서 | priority |
|---|---|
| 서비스 장애 / 데이터 손실 / 보안 | `P0` |
| 현재 sprint 핵심 / 사용자 다수 영향 | `P1` |
| 다음 sprint / 일부 사용자 / 운영 불편 | `P2` |
| backlog / 시간 날 때 | `P3` |

명확 단서 부재 시 `P2` 기본 + 모호 마킹.

### B-4. title + body 초안

type 별 prefix + body 자동 생성:

- **bug/fix**: 현상 / 재현 (log 단서 / 코드 라인) / 기대 / 실제 / 환경 / 추정 원인
- **feat/refactor/chore**: 목적 / 변경 영역 (path) / DoD 2-4
- **epic**: Why / Goal / 수용기준 / 비스코프 placeholder ("product-spec 단계에서 정밀화") / slug

body 끝에 **분석 메모** 섹션 자동 추가:
```
---
## 분석 메모 (자동 생성)
- 관련 코드: path:line
- 관련 PR/Issue: #N
- log 단서: ...
```

## Phase C — Briefing + 결정 게이트

다음 형식으로 1회 브리핑:

```
## Tech Lead 트리아지

**무엇**: {1줄}
**근거**: 
- 코드: {path:line}
- log: {단서}
- 기존 Issue: {중복 여부}

**제안 분류**:
- type: bug (확신 — 5xx 재현 가능, log Exception 식별)
- area: BE (확신 — InterviewService line 42)
- priority: P1 (추정 — 사용자 다수 영향, 단서 부족하면 P2)

**title 안**: [Bug] 인터뷰 시작 시 5xx 간헐 발생

**body 초안**: (위 generated body 표시)

**결정 필요**:
- priority P1/P2 — 빈도 데이터 없음. 어느 쪽?
```

`결정 필요` 항목만 `AskUserQuestion`. 없으면 바로 confirm:

```
question: "이 Issue 생성?"
options:
  - "생성 — 그대로"
  - "수정 — 어느 부분?"
  - "취소"
```

## Phase D — 생성

승인 후:

```bash
gh issue create \
  --title "{title}" \
  --label "type:{type},{area},priority:{P}" \
  --body "{body}"
```

라벨 부재 시 `gh label create` 자동 제안. 라벨 스킴:

- `type:epic` (#5319e7) / `type:feat` (#a2eeef) / `type:fix` (#fbca04) / `type:bug` (#d73a4a) / `type:chore` (#cfd3d7) / `type:refactor` (#bfdadc)
- `priority:P0` (#b60205) / `priority:P1` (#d93f0b) / `priority:P2` (#fbca04) / `priority:P3` (#0e8a16)
- area (BE/FE/infra/lambda/docs): 기존 사용

## 후속

- Epic 생성 시: "Issue #N 생성. plan 폴더 필요하면 `/create-product-spec` 호출 — 이 Issue 자동 추천됨."
- 기타: URL 출력.

## 스킬 경계

- 이 스킬 = **Issue 트리아지 + 등록**. 깊이 = type/area/priority 결정 가능 수준.
- product-spec 깊이 (수용기준 정밀화 / 측정 지표 / phase 분리 / YAGNI) = 별도 스킬.
- plan 폴더 자동 생성 X.

## 안티 패턴

- Phase A 자율 탐색 생략하고 사용자에게 다중 질문.
- 모든 분류 사용자에게 떠넘김 (옵션 다중선택 폭탄).
- 추정 근거 없이 분류 결정.
- 중복 Issue / 관련 PR 검색 생략.
- log 단서 있는데 본문에 미반영.
- 사용자 발화 그대로 title 사용 (정제 X).
- preview 생략하고 바로 `gh issue create`.
- product-spec 깊이 침범.
