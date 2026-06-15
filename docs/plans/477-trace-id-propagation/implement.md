# Implement — 분산 추적 ID (traceId) 전 구간 통일 + 로그 일원화

> **작성자**: backend agent
> **답하는 질문**: 어떤 순서로 실행?
> **사용 시점**: 다영역 통합 (BE + FE + Lambda + Infra). PR 별 영역 명시 + tasks/ 분리.
> **승인 게이트**: ★ 사용자 명시 승인 후 코드 작성 ★
> **신뢰 기준**: `tech-spec.md` (방금 재작성됨)

---

## PR 의존 관계

```
   PR-A (헤더 단일화 + MDC rename + filter + Lambda 헤더명)
     │
     ├──→ PR-B (S3 metadata 채널 + FE 헤더 송신 + Lambda head_object + convert copy_object)
     │       선행: PR-A (헤더명 정합)
     │
     ├──→ PR-C (CloudWatch awslogs driver + IAM + 운영 노트)  [PR-A 와 병렬 가능]
     │       선행: PR-A (WARN 메시지 패턴이 metric filter 대상)
     │
     └──→ PR-D (Hop 6 — ApplicationEvent traceId + Executor wrap)  [phase 2 별도 epic]
             선행: PR-A 머지 + dev/prod 배포 완료
```

| PR | 영역 | 예상 수정 파일 | 의존 |
|----|------|--------------|------|
| PR-A | BE main(2) + BE test(2) + Lambda(6) | 10 | - |
| PR-B | BE main(3) + BE test(2) + FE(3) + Lambda(4) | 12 | PR-A |
| PR-C | infra(2) + ops 노트(1) + AWS 콘솔(수동) | 3 + 콘솔 | PR-A |
| PR-D | BE main(8) + BE test(5) + docs(1) | 14 | PR-A 머지 후 |

---

## Phase / Task 개요

본 implement.md 는 PR 별 task 목록 + 의존 + Verification 요약. Task 상세는 `tasks/{pr}-{NN}-{slug}.md` 파일 참조.

### PR-A: 헤더 단일화 + MDC rename + 일반 사용자 API filter

| Task | 제목 | 영역 | 상세 |
|------|------|------|------|
| A1 | `TraceIdFilter` 신설 + Security filter chain 등록 | BE | [tasks/a-01-traceid-filter.md](tasks/a-01-traceid-filter.md) |
| A2 | `InternalApiKeyFilter` 헤더명 / MDC key rename | BE | [tasks/a-02-internal-filter-rename.md](tasks/a-02-internal-filter-rename.md) |
| A3 | `logback-spring.xml` 패턴 변경 | BE | [tasks/a-03-logback-pattern.md](tasks/a-03-logback-pattern.md) |
| A4 | Lambda analysis 헤더명 / 변수명 rename | Lambda | [tasks/a-04-lambda-analysis-rename.md](tasks/a-04-lambda-analysis-rename.md) |
| A5 | Lambda convert 헤더명 / 변수명 rename | Lambda | [tasks/a-05-lambda-convert-rename.md](tasks/a-05-lambda-convert-rename.md) |

**PR-A Verification 요약** (tech-spec.md §Verification PR-A):
- `./gradlew test --tests "com.rehearse.api.global.config.*Filter*Test"` green
- `cd lambda/analysis && pytest` green
- `cd lambda/convert && pytest` green
- 회귀 grep 0건: `correlationId` / `X-Correlation-Id` / `set_correlation_id` (BE main + lambda)
- logback 패턴 `%X{traceId:-}` 단일

**커밋 메시지 (예상)**:
- `feat(BE): TraceIdFilter 신설 + 사용자 API 진입 traceId MDC 적재`
- `refactor(BE): InternalApiKeyFilter 헤더명 X-Trace-Id 로 단일화`
- `chore(BE): logback 패턴 traceId 로 rename`
- `refactor(lambda): analysis 헤더명 X-Trace-Id 로 단일화`
- `refactor(lambda): convert 헤더명 X-Trace-Id 로 단일화`

---

### PR-B: S3 metadata 채널 + FE 헤더 송신 + Lambda head_object + convert copy_object

| Task | 제목 | 영역 | 상세 |
|------|------|------|------|
| B1 | `S3Service.generatePutPresignedUrl` 시그니처 변경 + metadata 주입 | BE | [tasks/b-01-s3service-metadata.md](tasks/b-01-s3service-metadata.md) |
| B2 | `UploadUrlResponse.traceId` 필드 추가 + `QuestionSetService` 전달 | BE | [tasks/b-02-upload-url-response.md](tasks/b-02-upload-url-response.md) |
| B3 | FE `useS3Upload` 시그니처 변경 + `x-amz-meta-trace-id` 헤더 송신 | FE | [tasks/b-03-fe-use-s3-upload.md](tasks/b-03-fe-use-s3-upload.md) |
| B4 | Lambda analysis `head_object` → metadata 추출 + 자체 생성 제거 | Lambda | [tasks/b-04-lambda-analysis-head-object.md](tasks/b-04-lambda-analysis-head-object.md) |
| B5 | Lambda convert `head_object` + `copy_object MetadataDirective=REPLACE` | Lambda | [tasks/b-05-lambda-convert-copy-object.md](tasks/b-05-lambda-convert-copy-object.md) |

**PR-B Verification 요약** (tech-spec.md §Verification PR-B):
- `QuestionSetServiceIntegrationTest#generateUploadUrl_traceId_propagation` green
- `AwsS3ServiceIntegrationTest#generatePutPresignedUrl_includesTraceMetadata_inSignedHeader` green
- FE `use-s3-upload.test.tsx` PUT 헤더 검증 green
- Lambda `test_handler.py` head_object 추출 / fallback / copy_object 케이스 green
- 수동 E2E: dev 1회 인터뷰 답변 업로드 → S3 객체 metadata `trace-id` 확인 → Lambda CloudWatch 동일 traceId 확인

**커밋 메시지 (예상)**:
- `feat(BE): presigned PUT URL 에 trace-id metadata 주입 + Response 노출`
- `feat(FE): S3 PUT 시 x-amz-meta-trace-id 헤더 송신`
- `feat(lambda): S3 metadata 에서 traceId 추출 (자체 생성 제거)`
- `feat(lambda): convert 출력 객체에 traceId metadata 복사`

---

### PR-C: CloudWatch awslogs driver + IAM + 운영 노트

| Task | 제목 | 영역 | 상세 |
|------|------|------|------|
| C1 | `backend/docker-compose.yml` dev awslogs driver | infra | [tasks/c-01-compose-dev.md](tasks/c-01-compose-dev.md) |
| C2 | `backend/docker-compose.prod.yml` prod awslogs driver | infra | [tasks/c-02-compose-prod.md](tasks/c-02-compose-prod.md) |
| C3 | EC2 IAM 정책 + 운영 노트 + metric filter / alarm 셋업 | infra + docs | [tasks/c-03-iam-ops-note.md](tasks/c-03-iam-ops-note.md) |

**PR-C Verification 요약** (tech-spec.md §Verification PR-C):
- `aws logs describe-log-groups --log-group-name-prefix /rehearse/backend/` → dev + prod 그룹 적재 확인
- dev 컨테이너 재기동 → 5분 내 stdout 라인 적재
- CloudWatch Logs Insights cross-group 쿼리 (`/rehearse/backend/dev` + `/aws/lambda/rehearse-analysis-dev`) hop 1, 3, 4, 5 시간순 반환
- metric filter alarm `RehearseMissingTraceId-{env}` 셋업 + 시뮬레이션 1회 ALARM 전환

**커밋 메시지 (예상)**:
- `chore(infra): EC2 BE stdout CloudWatch awslogs driver (dev/prod)`
- `docs(ops): traceId CloudWatch 적재 IAM / alert 운영 노트`

---

### PR-D: Hop 6 — ApplicationEvent traceId + Executor wrap (phase 2 별도 epic)

| Task | 제목 | 영역 | 상세 |
|------|------|------|------|
| D1 | `MdcContextExecutor` 신설 (Domain Unit 포함) | BE | [tasks/d-01-mdc-context-executor.md](tasks/d-01-mdc-context-executor.md) |
| D2 | 3 Executor bean wrap (`AsyncConfig` / `RubricScoringExecutorConfig` / `SessionFeedbackExecutorConfig`) | BE | [tasks/d-02-executor-wrap.md](tasks/d-02-executor-wrap.md) |
| D3 | 이벤트 record 4종 `traceId` 필드 추가 + 발행자 6곳 동시 수정 | BE | [tasks/d-03-event-records.md](tasks/d-03-event-records.md) |
| D4 | listener 4 메서드 (3 클래스) 진입 첫 줄 MDC 복원 | BE | [tasks/d-04-listener-mdc-restore.md](tasks/d-04-listener-mdc-restore.md) |
| D5 | Executor MDC 전파 통합 테스트 3종 + listener MDC 복원 테스트 4종 | BE | [tasks/d-05-integration-tests.md](tasks/d-05-integration-tests.md) |

**PR-D Verification 요약** (tech-spec.md §Verification PR-D):
- `MdcContextExecutorTest` 4 케이스 green
- `VtExecutorMdcPropagationTest` / `RubricScoringExecutorMdcPropagationTest` / `SessionFeedbackExecutorMdcPropagationTest` green
- 4 listener MDC 복원 통합 테스트 green
- `backend/AGENTS.md` 안내 1줄 추가
- dev 종단 시나리오 재실행: hop 1, 3, 4, 5, 6 모두 동일 traceId

**커밋 메시지 (예상)**:
- `feat(BE): MdcContextExecutor 신설 + 3 Executor MDC 전파 wrap`
- `feat(BE): ApplicationEvent 4종 traceId 필드 + 발행자 6곳 캡처`
- `feat(BE): @TransactionalEventListener 진입 MDC traceId 복원`
- `test(BE): Executor / listener MDC 전파 통합 테스트`
- `docs(BE): 신규 Executor bean 추가 시 wrap 안내`

---

## 통합 Verification

`tech-spec.md` §Verification (PR-A / B / C / D 각 절 + 통합 매핑 표) 모두 통과 시 plan 완료.

추가 회귀 체크:
- [ ] PR-A 머지 + Lambda safe-deploy 동시 완료 — in-flight WARN spike 5분 내 수렴 확인
- [ ] PR-B 머지 시 BE + FE + Lambda 동시 배포 — S3 PUT 403 SignatureDoesNotMatch 회귀 0건 확인
- [ ] PR-B 통합 시 **dev 실 S3 객체 metadata 캡처 (수동 1회)** — `aws s3api head-object --bucket <dev-bucket> --key <dev 업로드 객체 키>` 로 `Metadata.trace-id` 값 캡처. 자동 테스트 (signed header 검증) 외 실 S3 동작 1회 검증 필수. 캡처 결과 본 plan 폴더 (`docs/plans/477-trace-id-propagation/`) 에 텍스트 archive 권장 (선택)
- [ ] PR-C 후 dev / prod 환경 분리 log group 적재 확인
- [ ] PR-D 머지 후 hop 6 비동기 진입 로그에 호출자 traceId 일관 출력

---

## 운영 절차 (PR 머지 → 배포 → 가시화)

각 PR 머지 후 다음 절차 순서 강제. 어긋남 시 in-flight WARN spike / 업로드 실패 회귀 위험.

### PR-A (헤더 단일화 + Lambda 헤더명)

1. **PR-A 머지** (BE main 2 + Lambda 6 단일 PR)
2. **BE 이미지 빌드 + dev / prod 배포 완료 확인** — EC2 컨테이너 신규 이미지 가동 확인 (`docker ps` + `actuator/info`)
3. **Lambda safe-deploy 실행** — `cd lambda && ./lambda-safe-deploy.sh` (analysis + convert 양쪽). canary alias 전환 완료까지 대기
4. **CloudWatch metric filter 5분 감시** — `RehearseMissingTraceId-{env}` 알람 발화 없음 확인. WARN spike 5분 내 수렴 ≤ 임계치
5. 4단계 실패 시 BE / Lambda 이전 버전 동시 롤백 (PR-A §롤백 참조)

### PR-B (S3 metadata + FE 헤더 + Lambda head_object)

1. PR-B 머지 — BE + FE + Lambda 강결합 단일 PR
2. BE + FE + Lambda 동시 배포 — 부분 배포 windows 최소화 (FE 미배포 + BE 배포 = 신규 업로드 SignatureDoesNotMatch 403)
3. dev 환경 1회 업로드 수동 검증 — S3 콘솔 또는 `aws s3api head-object` 로 `Metadata.trace-id` 확인
4. Lambda CloudWatch 동일 traceId 출력 확인

### PR-C (CloudWatch awslogs driver)

1. PR-C 머지
2. EC2 IAM Role 정책 사전 적용 확인 (`logs:CreateLogStream` + `logs:PutLogEvents`)
3. dev 컨테이너 재기동 (`docker compose -f backend/docker-compose.yml up -d`) — 다운타임 < 1분
4. 5분 내 `/rehearse/backend/dev` 로그 그룹 적재 확인
5. prod 동일 절차 (별도 운영 시간 합의 후)

### PR-D (Hop 6 비동기 MDC 복원)

1. PR-D 머지
2. BE 배포 — Lambda 배포 무관 (코드 only)
3. dev 종단 시나리오 1회 실행 — hop 6 listener 진입 로그가 호출자 traceId 와 일치하는지 확인

---

## 리뷰 게이트 (MANDATORY)

각 PR 머지 전 지정 리뷰어 실행 강제. 스킵 = 위반.

- [ ] PR-A → `code-reviewer-backend` (BE 2 파일 + Lambda 6 파일 동시 점검. Lambda 전담 리뷰어 부재 — BE 리뷰어가 Python 변경 동반 검토)
- [ ] PR-B → `code-reviewer-backend` + `code-reviewer-frontend` **병렬** (단일 메시지 multiple tool_use)
- [ ] PR-C → `code-reviewer-backend` (infra YAML + 운영 노트)
- [ ] PR-D → `code-reviewer-backend`
- [ ] 컨벤션 위반 0건 (`backend/.claude/rules/conventions.md` + `backend/.claude/rules/testing.md`. FE 영역은 `frontend/.claude/rules/conventions.md`)
- [ ] Critical / Major 지적 = fix 반영 후 재리뷰
- [ ] Pre/Post State diff 일치 (`tech-spec.md` §Pre / Post State 기준)

---

## 미해소 결정 (코드 진입 전 사용자 확인 필요)

`tech-spec.md` §미확인 / 사용자 결정 필요 항목 단일 소스. 본 implement 진입 전 5개 항목 결정 필요:

1. 이벤트 페이로드 traceId 필드 = conventions.md 룰 예외 인정 여부
2. EC2 IAM Role CloudWatch Logs 권한 현황
3. CloudWatch metric filter alert SNS topic
4. `/api/v1/**` 외 TraceIdFilter 가드 범위 (oauth2 / health 제외 여부)
5. TraceIdFilter 응답 헤더 `X-Trace-Id` 동봉 여부
