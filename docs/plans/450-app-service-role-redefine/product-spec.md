# Product Spec — App Service 역할 재정의 및 도메인별 점진 리팩토링

> **작성자**: PM (Claude)
> **답하는 질문**: 왜? 무엇? 수용기준?
> **다음 단계**: 사용자 승인 → backend agent 가 `tech-spec.md` 작성
> **관련 Issue**: #450

---

## 문제 상황 (Problem)

**현재 상태**:
- Controller → App Service 2단 호출. App Service 가 "트랜잭션 경계 + 조립" 역할 컨벤션 (`backend/.claude/rules/conventions.md` "계층 책임" 표) 보유.
- 일부 App Service 가 비즈니스 흐름 + cross-domain 호출 + 조건 분기 + 데이터 가공 혼재. 메서드 본문 50줄+ 흐름 파악 어려움.

**관찰된 증상**:
- `FollowUpService` (220줄, 의존 13개) — `generateFollowUp()` 본문에 audio 가드 / context 로드 / runtime state init / Resume 라우팅 분기 / askedPerspectives 조립 / analyzer 호출 / SKIP 분기 / Step B 호출 5+ 책임 혼재. 같은 클래스에 `resolveResumeSkeleton` / `resolveInterviewPlan` 등 cross-domain `*Persister` / `*RuntimeCache` / `*Planner` 직접 호출.
- `QuestionSetService` (215줄, 의존 10개) — 타 도메인 Repository (`feedbackRepository`, `questionScoreRepository`, `questionScoreDimensionRepository`, `fileMetadataRepository`) 직접 주입 후 데이터 조립.
- `ResumeInterviewOrchestrator` 등 일부 도메인은 이미 핸들러 / Policy / EventPublisher 분해 진행 → 패턴 부재가 아닌 **이행 부재** 가 문제.

**인지 채널**:
- 코드 리뷰 시 "이 메서드 흐름 파악 어렵다" 반복 피드백.
- 도메인 변경 시 cross-domain Repository 의존 때문에 영향 범위 추적 비대.

## 왜 해야 하는가 (Why)

**개발자 임팩트**:
- App Service 메서드 한 화면에 안 들어옴 → 신규 합류자 / 본인 재방문 시 흐름 파악 시간 증가.
- 비즈니스 로직 변경 = 거대 메서드 안 한 줄 수정 → 사이드이펙트 위험 ↑.

**시스템 임팩트**:
- 도메인 경계 흐려짐 → cross-domain Repository 직접 주입이 양산되며 한 도메인 변경이 다른 도메인 Service 깨뜨릴 위험 ↑.
- 테스트 작성 어려움 — App Service 단일 테스트가 5개 도메인 mock 필요.

**사용자 / 운영 임팩트**:
- 직접 사용자 영향 = **없음**. 본 Epic = 내부 코드 품질 only.
- 간접 효과: 도메인 경계 명확화 → 향후 기능 변경 시 회귀 빈도 ↓ (정량 측정 없음, 추정).
- 운영 비용 절감 = 측정 안 함. 본 Epic ROI = "개발 비용 절감 + 룰-코드 갭 해소" 로 한정.

**외부 압력**:
- Issue #450 사용자 발화 — "Facade 우려, 부분적 리팩 선호". 4단 계층 추가 거부.
- conventions.md 룰 존재하나 (App Service = 조립자 / Domain Service = 책임 명사) **이행 부재**. 룰과 코드 갭 누적.

## 해결 방향 (Approach)

**핵심 접근**: Facade 신설 X. 기존 App Service 를 **조립자 (orchestrator only)** 로 한정 + 비즈니스 구현은 **Domain Service / Policy / Pipeline / Coach** 등 책임 명사 클래스로 추출. 도메인 일괄 리팩토링.

**Phase 분리**:
- **Phase 0 (선행)**: `conventions.md` "Service 책임 분리" 섹션 보강 — App Service 정의 / 금지 사항 / Before-After 예시 / cross-domain 정책 + `*Finder` 통일 명문화.
- **Phase 1 (Pilot = 베스트 프렉티스 확립)**: `interview` 도메인을 룰 적용해 **모범 형태로 리팩**. 결과물 (`FollowUpService` 조립자화 + Domain Service / `*Finder` 추출 패턴) = 후속 도메인 참고 기준.
- **Phase 2 (Rollout 일괄)**: `resume` / `question` / `feedback` / `auth` 등 나머지 도메인을 **한 번에 일괄 변경 후 단일 PR 제출**. Pilot 결과 베스트 프렉티스 동일 적용. 본 Epic = Phase 2 머지 시점 종료.

**Pilot 정의 (중요)**:
- Pilot = "현재 상태 양호 도메인" 이 아님. **리팩 후 결과물이 베스트 프렉티스 기준** 이 되는 도메인.
- `interview` 채택 사유:
  1. 위반 패턴 다양 (`FollowUpService` cross-domain `*Persister` / `*RuntimeCache` / `*Planner` 직접 호출, 거대 메서드, 책임 혼재) → 룰 적용 사례 풍부.
  2. `ResumeInterviewOrchestrator` 처럼 핸들러 분해 진행된 부분 양호 코드 공존 → 리팩 전후 대비 명확.
  3. 코어 도메인 (인터뷰 진행) → 회귀 위험 통제 검증 가능.

**대안 비교**:
- (탈락) Facade 계층 신설: Controller → Facade → Service → Domain Service 4단 → 단순 호출 위임 비대화.
- (탈락) 도메인별 sub-Issue 분리 후속 처리: Pilot 머지 후 sub-Issue 정체 시 일관성 깨짐 위험. 사용자 결정 = 일괄 처리.
- (채택) Pilot 1개 + 룰 보강 선행 → 패턴 확립 → 나머지 도메인 일괄 PR.

**Cross-domain Repository 직접 주입 정책 (Phase 0 룰 보강 인풋)**:
- 현재 conventions.md = **허용** ("app service → 하위 Repository 직접 접근 허용"). Pilot 진행 중 발견되는 위반은 **룰 위반 아님**.
- 본 Epic 에서 **룰 자체를 강화**. cross-domain 데이터 조회는 **해당 도메인의 `*Finder` 경유** 강제. Repository 직접 주입 / `*Persister` / `*RuntimeCache` 직접 호출 금지.
- 정확한 룰 본문 / 예외 조건 = tech-spec + `conventions.md` 편집 시 확정.
- 추정: cross-domain Repository 직접 주입 위반 사례는 `QuestionSetService` 외에도 산재 가능성. Phase 2 일괄 변경 시 전수 확인.

**Domain Service 명명 정책 (Phase 0 룰 보강 인풋)**:
- Domain Service 접미사 = **책임 명사** (`Generator` / `Coach` / `Policy` / `Pipeline` / `Persister` / `RuntimeCache` 등) 유지.
- **Cross-domain 조회 전용 진입점만 `*Finder` 접미사 통일**. 예: `InterviewFinder`, `ResumeFinder`.
- 정확한 룰 본문 (내부 조회 자유 / App Service 시각 구분 / CQRS 거부 사유) = `conventions.md` 편집 시 확정.

## Evidence

**코드 추적**:
- `backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpService.java:43-219` — 220줄 / 의존 13개 / cross-domain `*Persister` / `*RuntimeCache` / `*Planner` 직접 호출.
- `backend/src/main/java/com/rehearse/api/domain/question/service/QuestionSetService.java` — 215줄 / 타 도메인 Repository 5개 직접 주입.
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeInterviewOrchestrator.java` — 핸들러 / Policy / EventPublisher 분해 진행됨 → Pilot 가치 ↓ (이미 양호).
- `backend/.claude/rules/conventions.md` "계층 책임" 표 존재. "Service 책임 분리" 명시 섹션 부재. cross-domain Repository 직접 접근 허용 룰 존재 (재검토 필요).

**Issue / 사용자 발화**:
- Issue #450 본문 — App Service Facade 한정 + Domain Service 추출 방향 명시.
- 사용자 발화 — "Facade 우려, 4단 회피, 부분적 리팩 선호".
- 사용자 결정 (본 spec 작성 중) — Pilot = `interview`. Cross-domain Repository 정리 본 Epic 포함. 패키지 분리 X (역할만 분리). 명명 = 책임 명사 + `*Finder` 통일. Phase 2 = 일괄 변경 단일 PR.

**도메인 비교 (Pilot 선정 근거)**:

| 도메인 대표 | 라인 | 의존 | Cross-domain 호출 | Pilot 적합도 |
|---|---|---|---|---|
| `FollowUpService` (interview) | 220 | 13 | resumeOrchestrator + *Persister + *RuntimeCache 직접 | **상 (Pilot 채택)** |
| `QuestionSetService` (question) | 215 | 10 | feedback / file / score Repository 5개 | 중 |
| `ResumeInterviewOrchestrator` (resume) | (미측정) | (미측정) | 이미 분해 진행 | 하 |
| `feedback` / `auth` 도메인 | (미측정) | (미측정) | (추정 — 미분석) | (미측정, Phase 2 일괄 변경 시 전수 확인) |

**Pilot 적합도 평가 기준**: (1) 위반 패턴 다양성 (2) 코어 도메인 / 회귀 위험 통제 검증 가치 (3) 결과물의 일반성.

## Goal

- [ ] **G1**: `backend/.claude/rules/conventions.md` 에 "Service 책임 분리" 섹션 보강. App Service 정의 / 금지 사항 / Before-After 예시 / cross-domain 정책 명문화. 룰 단독 읽고 신규 합류자가 App Service 작성 가능.
- [ ] **G2**: `interview` 도메인 Pilot — App Service 메서드 흐름 파악 가능 수준. **측정 = 사용자 직접 검토 + 승인** (수치 지표 도입 X).
- [ ] **G3**: 전 도메인 cross-domain Repository / `*Persister` / `*RuntimeCache` / `*Planner` 직접 주입 = 0. 타 도메인 데이터 조회 = `*Finder` 경유.
- [ ] **G4**: Phase 2 일괄 변경 머지 시점에 `resume` / `question` / `feedback` / `auth` 모든 도메인이 베스트 프렉티스 (Pilot 결과물) 패턴 적용 완료.
- [ ] **G5**: 리팩 후 기존 테스트 전부 그린 + 추출된 Domain Service 단위 테스트 추가 (`testing.md` Domain Unit 카테고리).

## Non-Goals

이 작업이 **추구하지 않는 가치** (혼동 방지).

- **Latency / 성능 개선** — 사유: 본 Epic 은 가독성 / 도메인 경계 정합성. 기존 흐름 동등 동작 우선.
- **테스트 커버리지 일괄 상승** — 사유: 추출된 Domain Service 단위 테스트만. 기존 미테스트 영역 일괄 보강 X.
- **사용자 체감 가치** — 사유: 본 Epic = 내부 코드 품질. 사용자 임팩트 추구 X.

## 수용 기준 (Acceptance Criteria)

**Phase 0 (룰 보강)**:
- [ ] `backend/.claude/rules/conventions.md` 에 "Service 책임 분리" 섹션 신설 또는 기존 표 확장. App Service 책임 정의 + 금지 사항 (HTTP 의존 / 깊은 비즈로직 / cross-domain Repository 직접 주입) 포함.
- [ ] Before-After 예시 2쌍 이상: (1) cross-domain Repository 직접 주입 → `*Finder` 경유 (2) 거대 App Service 메서드 → Domain Service 추출.
- [ ] **Cross-domain 조회 정책 명문화**: 타 도메인 데이터 조회는 해당 도메인 `*Finder` 경유 강제. Repository / `*Persister` / `*RuntimeCache` / `*Planner` 직접 주입 금지. 예외 조건 명시.
- [ ] **Domain Service 명명 룰 명문화**: 책임 명사 접미사 유지. Cross-domain 조회 진입점만 `*Finder` 통일. 클래스 접미사 표 (`conventions.md` "클래스 접미사") 에 `*Finder` 항목 추가.

**Phase 1 (interview Pilot)**:
- [ ] `FollowUpService` 가 흐름 조립자로 한정. 메서드 본문에서 분기 / 데이터 조립 / 외부 도메인 데이터 직접 fetch 제거 확인.
- [ ] `interview` 도메인 App Service 가 타 도메인 `*Persister` / `*RuntimeCache` / `*Planner` / Repository 직접 주입 = 0. cross-domain 조회 = `*Finder` 경유.
- [ ] 추출된 Domain Service / Policy / Pipeline / Coach 클래스가 책임 명사 접미사 컨벤션 준수.
- [ ] 기존 `interview` 도메인 테스트 전부 그린.
- [ ] 추출된 Domain Service 단위 테스트 추가 (Domain Unit 카테고리).
- [ ] **회귀 검증**: interview 도메인 Service Integration (인터뷰 시작 / FollowUp 생성 / Resume 라우팅) 그린 + dev 환경 1회 수동 시연 (인터뷰 1회 완주).
- [ ] **사용자 직접 검토 통과**: `FollowUpService` / 추출된 Domain Service / `*Finder` 코드 사용자 확인 + 승인.

**Phase 2 (Rollout 일괄)**:
- [ ] `resume` / `question` / `feedback` / `auth` 모든 도메인이 Phase 1 Pilot 동일 패턴 적용.
- [ ] 전 도메인 cross-domain Repository / `*Persister` / `*RuntimeCache` / `*Planner` 직접 주입 = 0.
- [ ] 전 도메인 추출 Domain Service 단위 테스트 + 기존 Service Integration 전부 그린.
- [ ] **회귀 검증**: 각 도메인 핵심 플로우 dev 환경 수동 시연 (인터뷰 / 이력서 / 피드백 / 인증 각 1회).
- [ ] 단일 PR 제출 (BE/FE 분리 룰 준수 — BE 단독 PR).
- [ ] 사용자 직접 검토 통과 + 머지.

## 비스코프 (Don't)

이번에 의도적으로 안 하는 **범위 외 작업**.

- **Facade 계층 신설** — 사유: 사용자 발화 명시 거부. 4단 계층 회피.
- **Controller / Repository / Entity 변경** — 사유: Issue #450 본문 명시. Service 계층 한정.
- **신규 기능 추가** — 사유: 본 Epic = 리팩 only.
- **패키지 분리** (`service/app/` + `service/domain/`) — 사유: 사용자 결정. 클래스 접미사로 구분, 패키지 이동 X.
- **수치 측정 지표 도입** (cyclomatic / 라인 수 자동 게이트) — 사유: 사용자 결정. 직접 검토로 판정.
- **Phase 2 도메인을 도메인별 PR 로 분리** — 사유: 사용자 결정. 일괄 단일 PR.

## 참고

- 관련 Issue: #450
- 관련 룰: `backend/.claude/rules/conventions.md` (계층 책임 / 클래스 접미사 / cross-domain 정책)
- 관련 plan: `docs/plans/405-questionset-package-unification/`, `docs/plans/460-domain-naming-cleanup/` (도메인 정리 선행 plan — 진행 상태는 tech-spec 단계 확인).
- Pilot 대상 코드:
  - `backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpService.java`
  - `backend/src/main/java/com/rehearse/api/domain/interview/service/` (전체 20 클래스 — 분해 대상 식별 tech-spec 단계)
- Phase 2 대상 도메인: `resume` / `question` / `feedback` / `auth` (구체 클래스 식별 = tech-spec 단계)
