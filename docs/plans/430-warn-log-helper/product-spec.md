# Product Spec — 진행차단진단 WARN 로그 헬퍼 통합 + InterviewTrack enum 활용 + magic number 상수화

> **작성자**: 사용자 (PM 페르소나, Claude 보조)
> **답하는 질문**: 왜? 무엇? 수용기준?
> **다음 단계**: 사용자 승인 → 구현 agent 가 `tech-spec.md` 작성

본 spec 은 Issue #430 의 6개 부채 항목 중 **현 코드 기준 유효한 항목 (#2 + #3 + #6) 만 선별**해 다룬다. 나머지 (#1 / #4 / #5) 는 비스코프 항목에 사유 명시.

---

## 문제 상황 (Problem)

PR #429 (`Issue #423` IntentClassifier 전면 제거) 이후 잔존한 후속 리팩토링 부채. 도메인 동작 결함 X — 관측성 / 클린코드 / 확장성 부채.

- **현재 상태 (정상 동작)**:
  - 진행차단진단 WARN 로그 7개소가 `track=...`, `stage=...`, `reason=...` 키 스키마로 직접 `log.warn(...)` 호출. 메시지 포맷 수동 작성.
  - `InterviewTrack` enum 은 이미 존재 (`{CS, LANGUAGE, RESUME}`) — 그러나 WARN 로그 내에서 `track=RESUME` / `track=STANDARD` 문자열 literal 로 직접 작성됨 (enum 미사용).
  - `InterrogationModeHandler` 가 chain 최대 레벨 / default answer quality 를 raw int literal (`2`, `4`) 로 보유.

- **발생 증상 (관측 / 위험 시나리오)**:
  - 새 reason 추가 시 7개소 복붙. 한 곳 오타 → grep 패턴 부분 누락 → 운영 진단 시 일부 케이스 미식별.
  - 트랙 추가 / 명칭 변경 시 string literal grep 의존. 컴파일러 보호 없음.
  - chain 최대 레벨 정책 변경 시 호출부 magic number `4` 위치 grep 으로 찾아야 함 — 누락 시 정책-구현 불일치 silent breakage.

- **사용자 / 운영 인지 채널**: PR #429 1-3차 `code-reviewer-backend` 리뷰. 6건 부채 분류 후 별도 리팩토링 PR 영역으로 분리 결정.

## 왜 해야 하는가 (Why)

- **사용자 임팩트**: 직접 영향 0 (로그 / 상수 정리, 동작 변경 X).
- **운영 / 시스템 임팩트**:
  - 진행차단진단 로그는 사용자 답변 미반영 / questionId 누락 등 **P0 결함 진단 채널**. 한 곳만 grep 미스 = 운영 사각지대 → 결함 재발견 지연.
  - 트랙·reason 식별자 타입 안전성 부재 → 트랙 / 정책 추가·변경 시 회귀 위험.
  - magic number → 정책 변경 시 silent breakage 가능.
- **외부 압력**: PR #429 리뷰어 (code-reviewer-backend) 명시 재작업 요구. 클린코드 / 관측성 부채로 분류.

## 해결 방향 (Approach)

PM 수준 high-level 방향. 구현 디테일 (클래스 / 메서드 시그니처) 은 tech-spec 영역.

- **핵심 접근 1 — 진단 로그 발행 단일 진입점**: 진행차단진단 WARN 로그를 단일 발행 헬퍼로 통일. 호출부에서 식별자 + 컨텍스트만 전달.
- **핵심 접근 2 — 식별자 타입 안전화**: 트랙 / reason 식별자를 enum 또는 동등 수준 타입 안전 식별자로 표준화. 기존 `InterviewTrack` enum 재사용. `STANDARD` 매핑 / `CS`·`LANGUAGE` 와의 관계는 tech-spec 결정.
- **핵심 접근 3 — magic number 명명 상수화**: chain 최대 레벨 / default answer quality 등 정책 의미 가지는 숫자를 의미 있는 식별자로 노출.

대안 비교:
- (A) Marker 기반 SLF4J Marker + 헬퍼 — 미래 로그 라우팅 / 필터링 확장에 유리. 채택 가능.
- (B) 단순 정적 헬퍼 메서드 — 더 가벼움. 미래 확장 필요성 미확정.
- 채택 사유는 tech-spec 에서 trade-off 판단.

단계 분리 (PR 단위): 본 작업의 PR 분리 정책 (3건 단일 PR vs 항목별 분리) 은 **tech-spec 단계 사용자 결정 게이트로 둠**. 작은 PR 회귀 격리 vs 묶음 컨텍스트 보존 trade-off 는 tech-spec 작성 시 표면화.

## Evidence

- **코드 추적**:
  - `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeInterviewOrchestrator.java:102` — `[진행차단진단]` `publish-skip`
  - `ResumeInterviewOrchestrator.java:217` — `questionId-missing`
  - `ResumeInterviewOrchestrator.java:226` — `response-questionid-missing`
  - `ResumeInterviewOrchestrator.java:231` — `response-questionid-mismatch`
  - `ResumeTurnEventPublisher.java:32` — `questionId-missing`
  - `backend/src/main/java/com/rehearse/api/domain/interview/service/FollowUpService.java:86` — `analyzer-skip`
  - `FollowUpService.java:111` — `step-b-skip`
  - `backend/src/main/java/com/rehearse/api/domain/resume/service/InterrogationModeHandler.java:81` — `int answerQuality = analysis != null ? analysis.answerQuality() : 2;`
  - `InterrogationModeHandler.java:115` — `if (currentLevel < 4)`
  - `backend/src/main/java/com/rehearse/api/domain/interview/entity/InterviewTrack.java` — `enum InterviewTrack { CS, LANGUAGE, RESUME }` (이미 존재)
- **사용자 발화**: "지금 코드 변경 좀 있어 6개 중 처리해야할거 선별해서 작성해줘. wrap up은 없어졌거든."
- **인접 plan**: `docs/plans/423-intent-classifier-removal/` (PR #429 원본 plan)

## Goal

- [ ] 진행차단진단 WARN 로그 발행 호출부에서 `log.warn("[진행차단진단] ...")` 직접 string format 호출 0건 (단일 헬퍼 경유)
- [ ] 트랙 / reason 문자열 literal grep — 헬퍼 정의부 외 호출부 0건 (예: `grep "track=RESUME"`, `grep "publish-skip"` 호출부 부재)
- [ ] chain 최대 레벨 / default answer quality 의미 가지는 raw int literal 호출부 0건 (이름 있는 상수 사용)
- [ ] 기존 통합 / 단위 테스트 모두 통과 (회귀 0)
- [ ] 운영 로그에서 reason · track · stage 키 스키마 형태 변화 없음 (grep 패턴 호환)

## Non-Goals

- **latency / 성능 개선**: 본 작업은 관측성 일관성 / 확장성이 목표. 성능 변화 없음 / 측정 X.
- **운영 진단 채널 자체 재설계**: `[진행차단진단]` 마커 / 키 스키마 (`interviewId` / `track` / `stage` / `reason` / `turnIndex`) 그대로 유지. 포맷 변경 시 운영 grep 패턴 깨짐 위험 있음.
- **사용자 가시 동작 변경**: 응답 schema / 인터뷰 흐름 / FE 노출 변경 없음. 본 작업은 운영 / 클린코드 부채 한정. 동작 미세 개선 슬립 차단.

## 수용 기준 (Acceptance Criteria)

- [ ] 진행차단진단 WARN 로그가 발행되는 모든 시나리오 (`publish-skip`, `questionId-missing`, `response-questionid-missing`, `response-questionid-mismatch`, `analyzer-skip`, `step-b-skip` 6종) 에서 출력 메시지의 키 스키마 (`interviewId`, `track`, `stage`, `reason`, `turnIndex`) 가 변경 전과 동일 (운영 grep 패턴 호환).
- [ ] 추가 컨텍스트 키 (`type`, `handlerQuestionId`, `responseQuestionId`) 가 필요한 시나리오에서도 변경 전과 동일하게 출력됨.
- [ ] reason 식별자 / 트랙 식별자가 새 항목 추가될 때 한 곳에서만 정의 추가하면 모든 호출부에서 안전하게 사용 가능 (정의처 누락 시 컴파일 실패 또는 동등 수준 안전성).
- [ ] chain 최대 레벨 정책 변경 시 한 곳 수정으로 모든 호출부에 일관 반영.
- [ ] default answer quality 정책 변경 시 한 곳 수정으로 일관 반영.
- [ ] 기존 통합 테스트 / 단위 테스트 / 빌드 / 린트 모두 통과.
- [ ] 변경 전후 출력 메시지 동등성 검증 — 단위/통합 테스트에서 WARN 로그 메시지 포맷 / 키 스키마 assertion 통과 (`interviewId=...`, `track=...`, `stage=...`, `reason=...`, `turnIndex=...` 키 등장 + 변경 전 동일 값 출력). 라이브 grep 의존하지 않고 자동 회귀 가능.

## 비스코프 (Don't)

본 spec 은 Issue #430 의 6항목 중 유효 3건만 다룬다. 나머지는 다음 사유로 제외:

- **#1 `stage=wrap_up` underscore 잔존** — `ResumeMode` enum 에서 `WRAP_UP` 자체 제거됨 (`PLAYGROUND`, `INTERROGATION` 만 잔존). 잔존 mode 둘 다 단일 단어 → `name().toLowerCase()` 출력 시 underscore 발생 케이스 0. 자동 해소 — 별도 `logValue()` / sanitize 헬퍼 불필요.
- **#4 `ResumeModeHandler` 공통 인터페이스 도입** — WrapUp 제거로 mode 가 2개만 남음. 원래 #4 목적 (3+ mode + Map 디스패치) 약화. 사용자 판단 = 과한 설계로 제외. mode 추가 계획 구체화 시 별도 plan 에서 재평가.
- **#5 `ResumeModeTransitionPolicy` 명칭 (부수효과 분리)** — 현재 `ResumeModeTransitionPolicy.java` 메서드 1개 (`isHardTimeoutExceeded`). `runtimeStateStore.update` 호출 0건. 부수효과 이미 분리된 상태 (PR #429 흐름에서 자연 정리). 추가 작업 불필요.
- **DB 스키마 / API endpoint 시그니처 변경** — 본 작업 영역 외.
- **prompt 템플릿 변경** — PR #429 흡수 완료.
- **의도 분기 부활** — 의도 분기는 PR #429 에서 의도적으로 제거. 부활 금지.

## 참고

- 관련 Issue: #430 (Epic), PR #429 (`Issue #423` IntentClassifier 제거 — 머지 완료)
- 관련 plan: `docs/plans/423-intent-classifier-removal/`
- 리뷰 단서: PR #429 1-3차 `code-reviewer-backend` 리뷰 (3차 = APPROVE, 부채 항목 명시)
