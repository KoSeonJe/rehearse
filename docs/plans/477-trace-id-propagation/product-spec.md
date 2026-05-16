# Product Spec — 분산 추적 ID (traceId) 전 구간 통일 + 로그 일원화

> **작성자**: 사용자 (PM 초안: Claude)
> **답하는 질문**: 왜? 무엇? 수용기준?
> **다음 단계**: 사용자 승인 → 구현 agent 가 `tech-spec.md` 작성

---

## 문제 상황 (Problem)

### 현재 상태

- 사용자 액션 1개 (예: 인터뷰 답변 영상 업로드) 는 다음 hop 으로 흩어진 로그를 만든다:
  - BE 진입 (presigned URL 발급 등 일반 사용자 API)
  - S3 업로드 (presigned URL 사용)
  - S3 event → Lambda 분석 진입
  - Lambda → BE 콜백 (분석 결과 수신)
  - BE 내부 ApplicationEvent 발행 + 비동기 / 트랜잭션 커밋 후 실행 리스너 다수
- 로그가 두 위치에 분산 적재: EC2 docker stdout (BE) / CloudWatch Logs (Lambda)
- MDC 식별자는 Lambda → BE 콜백 경로 한정으로만 적재됨 (`correlationId` 1개). Lambda 가 자체 ID 를 생성해 BE 발급 액션과 연결되지 않음

### 발생 증상

- 결함 발생 시 운영자가 timestamp / interviewId / questionSetId 로 사람이 짜맞춰 흐름 재구성
- 두 로그 위치를 별도 콘솔에서 조회 (EC2 SSH + Lambda CloudWatch) — grep 명령 동선 분리
- 비동기 리스너 진입 로그에는 호출 컨텍스트 식별자가 부재해 단서 끊김

### 사용자·운영 인지 채널

- 최근 PR #473 (Resume silent drop) / PR #474 (Resume planner / chain 정합성) 디버깅 시 로그 grep 어려움 호소
- 운영자 (개발자 본인) 가 직접 확인

---

## 왜 해야 하는가 (Why)

### 사용자 임팩트

- 직접 노출 X (운영 내부 도구). 단 결함 해소 시간 단축 → 사용자 회귀 빨라짐

### 운영 / 시스템 임팩트

- 결함 추적 시간 손실. 5xx / silent drop / 정합성 결함 분석 시 hop 별 로그 짜맞춤
- 인터뷰 진행 도중 결함 발생 시 root cause 식별 지연

### 외부 압력

- 향후 사용자 증가 시 한 액션의 로그 폭증 → 사람 짜맞춤 비용 비선형 증가
- 인프라 (Lambda + EC2 + S3 event) 구조상 hop 5개 이상 — 통합 식별자 없이는 확장 불가

---

## 해결 방향 (Approach)

PM 수준 high-level 방향. 구현 디테일은 tech-spec 영역.

### 핵심 접근

- **단일 식별자가 사용자 액션 시작 hop 에서 발급되어 모든 후속 hop 으로 전파**되도록 만든다. 자체 생성 지점 제거
- **로그 위치를 단일 콘솔에서 cross-group 조회 가능**하도록 EC2 BE stdout 을 CloudWatch 로 함께 적재. dev / prod 환경 분리 유지
- 운영자가 hop 별 다른 용어를 추측하지 않도록 **개념 명칭을 한 단어로 일원화**한다. 명명 / 헤더 / 식별자 표현 / 호환성 결정은 tech-spec 단계
- **개념 일원화 수반 비용 (사전 인지)**: 운영자 추적 동선 (검색 패턴 / 알람 룰 / 운영 노트) 일괄 갱신 필요 — 기존 동선 호환 보장 X 의도된 trade-off (개념 일원화 우선)

### phase 분리

- **phase 1 (epic 본 PR)**: 로그 일원화 (EC2 → CloudWatch dev/prod 분리) + traceId 전파 (BE 진입 ~ Lambda ~ BE 콜백 외부 경계)
- **phase 2 (별도 PR, 본 epic 내)**: Spring 내부 비동기 / 트랜잭션 커밋 후 실행 이벤트 리스너 진입 시 호출자 traceId 이어받기

### 대안 비교

- Loki + Promtail / OpenSearch — 운영 부담 (서버 1대 추가). 채택 X
- DataDog / Axiom 등 SaaS — 비용 부담. MVP scope 외. 채택 X
- CloudWatch (채택) — 이미 Lambda 측 사용 중. 인프라 변경 최소. cross-group 쿼리 지원 (Logs Insights)

---

## Evidence

- 코드 추적:
  - `backend/src/main/java/com/rehearse/api/global/config/InternalApiKeyFilter.java:24,48-50` — Lambda → BE 콜백 경로 한정 MDC 적재 (`correlationId`)
  - `backend/src/main/java/com/rehearse/api/infra/aws/AwsS3Service.java:34-46` — presigned PUT URL 발급. S3 object metadata 미주입
  - `lambda/analysis/handler.py:80-82` — Lambda 가 traceId 자체 생성 (BE 발급 액션과 연결 X)
  - `lambda/analysis/api_client.py:21-32` — `X-Correlation-Id` 헤더 송신 (Lambda → BE 콜백 동작)
  - `backend/src/main/resources/logback-spring.xml:5` — 로그 패턴 `[%X{correlationId:-}]`
  - `backend/src/main/java/com/rehearse/api/global/config/AsyncConfig.java:19` — VirtualThread executor. MDC 전파 wrapper 미적용
  - `backend/.../RubricScoringExecutorConfig.java`, `SessionFeedbackExecutorConfig.java` — 도메인별 별도 ThreadPerTask executor. 동일 사유
  - `backend/.../ResumeTurnEventPublisher.java:50`, `FeedbackService.java:48`, `InterviewCreationService.java:64`, `InterviewService.java:97`, `InterviewCompletionService.java:55`, `FollowUpTransactionHandler.java:166` — ApplicationEvent 발행
  - `backend/.../QuestionGenerationEventHandler.java:24`, `RubricScoringEventListener.java:33`, `SessionFeedbackEventListener.java:23,39` — `@Async` + `@TransactionalEventListener(AFTER_COMMIT)` 리스너
- 인프라:
  - `backend/docker-compose.prod.yml` — logging driver 미설정 (json-file 기본)
  - AWS Lambda 함수: `rehearse-analysis-{dev,prod}`, `rehearse-convert-{dev,prod}` (dev/prod 이미 분리)
- 사용자 발화 / 인접 plan:
  - PR #473 / #474 디버깅 호소 (트리아지 시 사용자 발화)
  - 인접 plan 없음 (신규 영역)

---

## Goal

사용자 / 운영자 입장 결과. 구현 완료 후 다음 세 가지가 관찰되어야 한다.

- [ ] **단일 액션 종단 추적성**: 운영자가 traceId 1개로 1회 cross-group 쿼리를 실행해 사용자 액션 1건의 전 흐름을 시간순 1개 결과 집합으로 받는다 (현재는 두 콘솔 + 사람 짜맞춤 필요)
- [ ] **환경 분리 운영성**: 운영자가 dev / prod 중 1개 환경 로그만 선택해 조회 가능 (혼선 차단)
- [ ] **개념 일원화 인지**: BE / Lambda / S3 metadata / 외부 헤더 / 로그 패턴이 동일한 `traceId` 명명을 사용한다 — 운영자가 hop 별 다른 용어 (correlationId / requestId / sessionId) 추측 없이 한 단어로 추적

---

## Non-Goals

이 작업이 추구하지 않는 가치 (혼동 방지).

- **디버깅 소요 시간 정량 측정** — 측정 인프라 부재. 정성 (단일 쿼리 가능 여부) 우선. "디버깅 시간 N분 → M분" 같은 정량 KPI 본 plan 의 성공 판정에 사용 X
- **로그 검색 응답 성능 최적화** — CloudWatch 기본 사양 수용
- **외부 사용자 노출 메트릭 / 대시보드** — 운영 내부 도구 성격
- **AI 호출 응답 본문 적재** — 보안 / 비용 검토 필요. 별도 의사결정

---

## 수용 기준 (Acceptance Criteria)

### 종단 시나리오 hop 정의 (검증 기준)

phase 1 / phase 2 의 "단일 traceId 전 hop 일치" 판정 시 다음 6 hop 을 표준 검증 흐름으로 사용. 시나리오 = "인터뷰 답변 영상 업로드 → Lambda 분석 → BE 콜백 → 비동기 후속 처리":

1. **BE 진입 hop** — 사용자 진입 API 진입 로그 (예: presigned URL 발급 요청)
2. **S3 객체 hop** — S3 에 업로드된 객체의 metadata / 식별 가능 데이터에 traceId 보유
3. **Lambda 진입 hop** — S3 event 받은 Lambda 자기 로그 첫 라인
4. **Lambda 외부 호출 hop** — Lambda 가 BE 로 보낸 콜백 요청 송신 로그
5. **BE 콜백 hop** — BE 가 Lambda 콜백 수신한 첫 진입 로그
6. **비동기 리스너 hop** — BE 내부 ApplicationEvent → @Async / @TransactionalEventListener(AFTER_COMMIT) 리스너 진입 로그

### phase 1 — 외부 경계 전파 + 로그 일원화 (hop 1~5)

- [ ] 사용자 진입 API (presigned URL 발급 포함 모든 사용자 노출 엔드포인트) 가 traceId 가 있는 요청을 받으면 그대로 사용하고, 없으면 생성한다 — hop 1 진입 로그에 traceId 등장
- [ ] presigned URL 로 업로드된 S3 객체가 발급 시점의 traceId 를 식별 가능한 형태로 보유한다 — hop 2 검증 (구현 형태: metadata / key / query — tech-spec 결정)
- [ ] Lambda 분석 함수가 S3 event 진입 시 객체로부터 traceId 를 추출해 자기 로그 전체에 일관되게 포함시킨다 (자체 생성 경로 제거) — hop 3 검증
- [ ] Lambda → BE 콜백 시 동일 traceId 가 헤더로 전달되어 송신측 (hop 4) / 수신측 (hop 5) 로그가 같은 값을 보유한다
- [ ] EC2 BE 컨테이너 stdout 이 CloudWatch 로 적재된다. dev 와 prod 가 분리된 로그 그룹을 사용한다
- [ ] CloudWatch Logs Insights 에서 BE + Lambda 로그 그룹을 동시 선택해 traceId 단일 값으로 cross-group 쿼리 시 hop 1, 3, 4, 5 결과가 시간순으로 반환된다 (hop 2 는 S3 콘솔 / API 별도 확인, hop 6 은 phase 2 완료 후 추가)
- [ ] **개념 일원화 영향 공지 완료** — 기존 운영자 추적 동선 (검색 패턴 / 알람 룰 / 운영 문서) 일괄 점검 결과 반영 (호환 보장 X 의도된 결정)

### phase 2 — 내부 비동기 / 이벤트 체인 전파 (hop 6)

- [ ] BE 내부 ApplicationEvent 발행 후 비동기 / 트랜잭션 커밋 후 실행 리스너 진입 로그 (hop 6) 가 호출자와 동일 traceId 로 적재된다
- [ ] 현재 비동기 진입점 4곳 (질문 생성 / dialogue 압축 / 루브릭 채점 / 세션 피드백) 의 리스너 진입 로그가 모두 호출자 traceId 와 일치한다
- [ ] traceId 전파 누락 신규 비동기 진입점 추가 시 회귀 탐지 가능 (회귀 테스트 1개 이상)
- [ ] phase 2 완료 후 종단 시나리오 cross-group 쿼리가 hop 1, 3, 4, 5, 6 모두 반환한다

---

## 비스코프 (Don't)

이번에 의도적으로 안 한다. 향후 별도 plan.

- **OpenTelemetry / Zipkin / DataDog APM 도입** — 자체 MDC + 헤더 + S3 metadata + CloudWatch 1차 채택. 향후 운영 부담 가시화 시 재검토
- **Grafana Loki / OpenSearch / ELK** — 별도 집계 인프라. CloudWatch 1차 채택
- **프론트엔드 X-Request-Id 발급 (사용자 브라우저 → BE)** — 향후 별도 plan. 본 plan 에서는 BE 진입에서 발급
- **로그 보존 기간 / 비용 정책 결정** — CloudWatch 기본값 수용. 운영 후 별도 결정
- **AI 호출 (OpenAI / Claude / Gemini) 외부 응답 본문 적재 / 마스킹 정책** — 보안 / 비용 별도 검토

---

## 참고

- 관련 Issue: #477
- 관련 PR: #473, #474 (디버깅 호소 컨텍스트)
- 인접 plan: 없음 (신규 영역)
- 외부 자료:
  - AWS Docker `awslogs` log driver 문서
  - CloudWatch Logs Insights cross log-group 쿼리 문서
