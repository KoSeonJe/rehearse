# Plan 04: dev 드레인·레거시 삭제·원샷 컷오버 + E2E 검증

> 상태: Draft
> 작성일: 2026-04-12
> 선행: Plan 02 (백엔드), Plan 03 (Lambda 신규 전용)

## Why

Plan 03 결정으로 Lambda는 **신규 스키마(`interviews/raw/...`)만 처리**하고 레거시 키는 `Skipped`로 즉시 반환한다. 따라서 구·신이 공존하는 dev 환경을 순차 배포로 전환할 수 없다. 순차 전환 시 다음 어느 시점이든 업로드가 처리되지 않는 구간이 생긴다:

| 순서 | 문제 |
|---|---|
| 백엔드만 먼저 배포 | 새 키가 old Lambda의 `startswith("videos/")`에 걸려 **처리 안 됨** |
| Lambda만 먼저 배포 | old 백엔드가 계속 `videos/...` 키를 생성 → new Lambda가 **Skipped 반환** → 처리 안 됨 |
| 동시 배포 (다른 파이프라인) | 수 초~수 분의 gap 동안 일부 업로드 유실 가능 |

결론: **다운타임을 계획해서 원샷 컷오버**한다. dev는 실사용자 트래픽이 없으므로 정해진 시간에 freeze → drain → delete → deploy → verify 순으로 진행한다. 사용자 요청 "오래 걸리더라도 신규 전용으로"에 정합.

## 생성/수정 파일

| 파일 | 작업 |
|---|---|
| (인프라·배포 집행) | 코드 변경 없음 |
| `docs/plans/s3-key-schema-redesign/progress.md` | 컷오버 로그 기록 |
| `docs/plans/prod-environment-setup/requirements.md` | 선행 작업 Completed 링크 갱신 |
| `docs/plans/prod-environment-setup/plan-03-s3-eventbridge-mediaconvert.md` | 이미 신규 prefix 기반으로 작성됨 — 검증만 |
| `docs/plans/prod-environment-setup/progress.md` | 선행 작업 완료 상태 반영 |
| `docs/architecture/s3-key-schema.md` | Changelog에 "v1.0 dev 적용 완료" append |

## 상세

### 전체 타임라인 (예상 90분)

```
T-24h   : 팀 공지 ("내일 dev 메인터넌스 XX:XX ~ XX:XX, S3 키 스키마 전환")
T+0:00  : Freeze 선언 (Slack/Notion)
T+0:05  : In-flight 작업 확인 (진행 중 면접 완료 대기)
T+0:15  : Drain 상태 점검 (analysis_status 테이블)
T+0:25  : dev S3 레거시 prefix 삭제
T+0:35  : Lambda analysis/convert 배포 + alias 전환 (동시)
T+0:45  : 백엔드 배포 (deploy-dev.yml merge)
T+0:55  : E2E smoke test
T+1:10  : 후속 모니터링 24h
T+1:15  : Unfreeze
```

### Pre-Cutover 체크리스트 (T-24h ~ T+0)

- [ ] Plan 01 SSOT 문서 머지 완료 (`docs/architecture/s3-key-schema.md`)
- [ ] Plan 02 백엔드 PR 준비 완료 (merge는 T+0:45에 집행) — CI 그린, 리뷰 승인만 남은 상태
- [ ] Plan 03 Lambda zip 빌드 완료, `lambda-safe-deploy.sh`로 **새 버전 publish까지 수행** (alias 미전환 상태로 대기)
- [ ] **publish 버전 번호를 runbook에 기록** — cutover runbook에 직전/신규 버전 쌍 명시 (롤백 A단계에 필수)
  ```
  rehearse-analysis-dev: prev=<N>, new=<N+1>
  rehearse-convert-dev:  prev=<N>, new=<N+1>
  ```
- [ ] FK 관계 최신 migration까지 재검증 (`grep -l "FOREIGN KEY" backend/src/main/resources/db/migration/*.sql`) — Step 4 TRUNCATE 순서가 여전히 유효한지 확인
- [ ] dev 환경 공지 (팀 Slack, 시작 24h 전 + 1h 전 리마인드)
- [ ] 컷오버 담당자 지정 (1 drivers + 1 shadow)
- [ ] Rollback plan 재확인 (아래 Rollback 섹션 — 매트릭스 A~D)

### Step 1 — Freeze 선언 (T+0:00)

Slack/Notion에 공지:
```
🚧 [DEV FREEZE] 2026-04-XX HH:MM ~ HH+1:30
- 대상: dev 환경 (dev.rehearse.co.kr, api-dev.rehearse.co.kr)
- 영향: 면접 녹화/분석 파이프라인 일시 중단
- 이유: S3 키 스키마 v1.0 전환
- 완료 후 전원 신규 스키마로 동작
```

팀원에게 dev 사용 중단 요청. prod 영향 없음 명시.

### Step 2 — Drain (T+0:05 ~ T+0:25)

In-flight 레거시 처리를 마무리한다. 최대 20분 대기 (Lambda 최대 timeout 15분 + 여유).

```bash
# SSH 또는 로컬에서 dev DB 접속
ssh -i ~/.ssh/rehearse-key.pem ubuntu@54.180.188.135
docker exec rehearse-db mysql -u root -p"$DB_ROOT_PASSWORD" rehearse -e "
  SELECT analysis_status, COUNT(*)
  FROM question_set_analysis
  WHERE analysis_status NOT IN ('COMPLETED', 'FAILED')
  GROUP BY analysis_status;
"
```

**Drain 완료 조건**:
- 위 쿼리 결과가 빈 set (진행 중 분석 0건)
- S3 `videos/` prefix에서 지난 10분 간 신규 put 이벤트 0건 (CloudTrail 또는 S3 Access Log)
- `aws s3 ls s3://rehearse-videos-dev/videos/ --recursive | wc -l` 값이 10분 전 조회와 동일

드레인이 20분 안에 끝나지 않으면 freeze window 연장 또는 해당 분석을 FAILED로 강제 마크 후 진행.

### Step 3 — 레거시 데이터 삭제 (T+0:25 ~ T+0:35)

dev S3 버킷에서 레거시 prefix를 일괄 삭제한다. dev는 테스트 데이터이므로 소실 허용.

```bash
# 1. 삭제 전 최종 카운트 기록
aws s3 ls s3://rehearse-videos-dev/videos/ --recursive --summarize | tail -2
aws s3 ls s3://rehearse-videos-dev/analysis-backup/ --recursive --summarize | tail -2

# 2. 레거시 prefix 삭제 (되돌릴 수 없음 — 버저닝 ON이면 delete marker만 생성됨, 복구 가능)
aws s3 rm s3://rehearse-videos-dev/videos/ --recursive
aws s3 rm s3://rehearse-videos-dev/analysis-backup/ --recursive

# 3. 삭제 확인
aws s3 ls s3://rehearse-videos-dev/videos/ --recursive | wc -l  # → 0
aws s3 ls s3://rehearse-videos-dev/analysis-backup/ --recursive | wc -l  # → 0
```

**주의**:
- dev 버킷에 Versioning이 켜져 있다면 `aws s3 rm`은 delete marker만 찍고 이전 버전은 남음. 완전 삭제가 필요하면 `aws s3api delete-objects` + versionId 필요. 본 컷오버는 **delete marker만으로 충분** (신규 Lambda는 listObjects가 아닌 이벤트 기반이라 delete marker는 무시됨)
- `interviews/` prefix는 손대지 않음 (아직 비어있을 것)

### Step 4 — DB `file_metadata` 레거시 행 정리 (T+0:25와 병렬 진행 가능)

파일 실 객체 삭제와 일관성 맞추기 위해 `file_metadata` 테이블에서 레거시 `videos/` prefix를 가진 행들을 정리한다.

```sql
-- 삭제 전 조회
SELECT COUNT(*) FROM file_metadata WHERE s3_key LIKE 'videos/%';
SELECT COUNT(*) FROM file_metadata WHERE streaming_s3_key LIKE 'videos/%';

-- 선택 1: 소프트 정리 (status만 ARCHIVED로 표시)  
--   → file_metadata.status enum에 ARCHIVED 추가가 필요하므로 범위 증가. 기각

-- 선택 2: 하드 삭제 (본 플랜 채택)
DELETE FROM question_set_analysis WHERE question_set_id IN (
  SELECT qs.id FROM question_set qs
  INNER JOIN file_metadata fm ON fm.id = qs.file_metadata_id  -- 실제 FK 구조는 코드에서 확인
  WHERE fm.s3_key LIKE 'videos/%'
);
-- 또는 보다 안전하게: dev DB 전체 truncate (dev는 test data)
```

**권장**: dev DB의 `file_metadata`, `question_set_analysis`, `question_set`, `interview` 테이블을 **모두 truncate**하는 것이 가장 단순하고 일관성 있다. dev 테스트 데이터는 재생성 가능하므로 보존 가치 낮음.

```sql
SET FOREIGN_KEY_CHECKS = 0;
-- 파생·하위 엔티티 먼저
TRUNCATE TABLE timestamp_feedback;        -- V4: FK → question_set_feedback
TRUNCATE TABLE question_set_feedback;     -- V4: FK → question_set
TRUNCATE TABLE question_set_analysis;     -- V14: FK → question_set ON DELETE CASCADE
TRUNCATE TABLE question_set_answer;       -- V4: FK → question (answer_repository 대상)
TRUNCATE TABLE question;                  -- V4: FK → question_set
-- 상위 엔티티
TRUNCATE TABLE question_set;              -- V4: FK → interview, file_metadata
TRUNCATE TABLE file_metadata;             -- V4: UNIQUE(s3_key) — 레거시 행 일괄 제거
TRUNCATE TABLE interview;                 -- V1: FK → users (users는 유지)
SET FOREIGN_KEY_CHECKS = 1;
```

**검증된 FK 구조** (2026-04-12 실측, V1/V4/V14 migration 기반):
- `question_set.file_metadata_id` → `file_metadata.id` (`fk_question_set_file_metadata`)
- `question_set.interview_id` → `interview.id` (`fk_question_set_interview`)
- `question_set_analysis.question_set_id` → `question_set.id` (`fk_qs_analysis_question_set ON DELETE CASCADE`)
- `question.question_set_id` → `question_set.id` (`fk_question_question_set`)
- `question_set_answer.question_id` → `question.id` (`fk_answer_question`)
- `question_set_feedback.question_set_id` → `question_set.id` (`fk_qs_feedback_question_set`)
- `timestamp_feedback.question_set_feedback_id` → `question_set_feedback.id` (`fk_ts_feedback_qs_feedback`)
- `interview.user_id` → `users.id` (V17 — **users 테이블은 truncate 대상 아님**)

**주의**:
- `users`는 truncate하지 **않는다** — smoke test에서 OAuth 로그인 재실행 없이 기존 계정으로 재사용 가능
- 다만 `interview`를 truncate하므로 기존 인터뷰 히스토리는 소실됨 (dev 테스트 데이터 원칙상 허용)
- 최신 migration 추가 시 FK 관계 재검증 필수 (cutover 30분 전 `grep -l "FOREIGN KEY" backend/src/main/resources/db/migration/*.sql` 재실행 권장)

### Step 5 — Lambda 전환 (T+0:35 ~ T+0:40)

Plan 03에서 이미 새 버전은 publish되어 있다(alias 미전환). 여기서 alias를 전환해 **동시에** 활성화.

```bash
# analysis
aws lambda update-alias \
  --function-name rehearse-analysis-dev \
  --name live \
  --function-version <publish된 버전>

# convert
aws lambda update-alias \
  --function-name rehearse-convert-dev \
  --name live \
  --function-version <publish된 버전>
```

두 alias 전환 사이 gap은 수 초. 이 시점 이후 EventBridge 이벤트는 신규 스키마만 처리한다.

### Step 6 — 백엔드 배포 (T+0:40 ~ T+0:45)

Plan 02의 PR을 `develop`에 merge → `deploy-dev.yml` 자동 실행. EC2 `rehearse-backend` 컨테이너가 새 이미지로 교체되면서 `S3KeyGenerator`가 활성화되고 presigned URL이 `interviews/raw/...` 키를 발급하기 시작.

컨테이너 교체 확인:
```bash
curl -sf https://api-dev.rehearse.co.kr/actuator/health  # → UP
docker exec rehearse-backend cat /app/BOOT-INF/classes/application.yml 2>/dev/null \
  || echo "image updated"
```

### Step 7 — E2E Smoke Test (T+0:45 ~ T+1:10)

#### Scenario 1: 신규 업로드 경로

**사전 조건**: Step 4에서 `users` 테이블은 truncate하지 않았으므로 기존 테스트 계정으로 GitHub OAuth 로그인 가능. 만약 팀 정책상 users도 truncate한 경우, OAuth 콜백 핸들러가 신규 사용자 자동 생성하는 경로를 먼저 검증할 것 (GitHub OAuth client_id/secret은 dev 환경 그대로 유지).

- [ ] `https://dev.rehearse.co.kr` 접속, GitHub 로그인
- [ ] 이력서 업로드 → 면접 Setup → 질문 생성
- [ ] 면접 녹화 시작 → 답변 1개 기록 → 종료
- [ ] DevTools Network: presigned PUT URL 요청 확인 → 응답의 key가 `interviews/raw/2026/MM/DD/{iid}/{qsid}/{uuid}.webm` 포맷
- [ ] S3 업로드 완료 후 CloudWatch Logs 확인:
  ```
  aws logs tail /aws/lambda/rehearse-convert-dev --since 5m
  aws logs tail /aws/lambda/rehearse-analysis-dev --since 5m
  ```
  - Convert: `[Convert] Triggered: key=interviews/raw/...` → MediaConvert job 생성 로그
  - Analysis: `[Analysis] Triggered: key=interviews/raw/...` → 다운로드 + 파이프라인 로그
- [ ] S3에 `interviews/mp4/...` 객체 생성 확인
- [ ] 피드백 UI 렌더 (타임스탬프 동기화, verbal/nonverbal 코멘트)
- [ ] DB `file_metadata.s3_key` 컬럼이 신규 포맷인지 확인
- [ ] DB `file_metadata.streaming_s3_key` 컬럼이 `interviews/mp4/...` 포맷인지 확인

#### Scenario 2: 미매칭 키 Skipped 동작 검증

**⚠️ 중요 — S3 업로드로 검증 불가**: 신규 EventBridge 규칙은 `prefix: "interviews/raw/"`로 필터링된다 (Plan 01 §5). 따라서 `videos/999/qs_999.webm` 같은 레거시 prefix 객체를 S3에 업로드해도 **EventBridge 이벤트 자체가 발생하지 않아 Lambda가 트리거되지 않는다**. Skipped 로그는 0건으로 나와 검증 실패로 오인 가능. 대신 **`aws lambda invoke`로 이벤트 페이로드를 직접 주입**해 Skipped 경로를 검증한다.

- [ ] 레거시 키 페이로드 직접 invoke — convert:
  ```bash
  aws lambda invoke \
    --function-name rehearse-convert-dev \
    --qualifier live \
    --payload '{"detail":{"bucket":{"name":"rehearse-videos-dev"},"object":{"key":"videos/999/qs_999.webm"}}}' \
    --cli-binary-format raw-in-base64-out \
    /tmp/convert-skipped.json
  cat /tmp/convert-skipped.json
  # 기대: {"statusCode":200,"body":"Skipped: not a v1 raw key"}
  ```
- [ ] 레거시 키 페이로드 직접 invoke — analysis:
  ```bash
  aws lambda invoke \
    --function-name rehearse-analysis-dev \
    --qualifier live \
    --payload '{"detail":{"bucket":{"name":"rehearse-videos-dev"},"object":{"key":"videos/999/qs_999.webm"}}}' \
    --cli-binary-format raw-in-base64-out \
    /tmp/analysis-skipped.json
  cat /tmp/analysis-skipped.json
  # 기대: {"statusCode":200,"body":"Skipped: not a v1 raw key"}
  ```
- [ ] 추가 케이스 — MP4 파생 키 / 피드백 키 / garbage 키 각각 동일 패턴으로 invoke해 **모두 Skipped 반환** 확인:
  - `interviews/mp4/2026/04/12/1/1/abcdef012345.mp4`
  - `interviews/feedback/2026/04/12/1/1/abcdef012345.json`
  - `random/garbage.txt`
- [ ] CloudWatch Logs에서 `[Convert][Skipped]`, `[Analysis][Skipped]` 각 1줄만 기록된 것 확인
  ```bash
  aws logs tail /aws/lambda/rehearse-convert-dev --since 5m | grep -c "Skipped"   # 4건 (위 invoke 수만큼)
  aws logs tail /aws/lambda/rehearse-analysis-dev --since 5m | grep -c "Skipped"  # 4건
  ```
- [ ] 처리 경로 진입 로그 0건 (convert: `create_mediaconvert_job` 미호출 / analysis: `get_answers` 미호출)
- [ ] 파이프라인 DB 상태 변화 0건 (`question_set_analysis` 신규 행 0)

**Note**: S3에 실제 레거시 파일을 업로드하지 않으므로 Step 3의 레거시 삭제 상태를 훼손하지 않는다.

#### Scenario 3: 실패 피드백 S3 백업 경로 (선택)
- [ ] `api_client.save_feedback()`이 의도적 실패하도록 테스트 환경 토글 (또는 mock endpoint) → `backup_to_s3`가 `interviews/feedback/YYYY/MM/DD/{iid}/{qsid}/{uuid}.json`에 기록되는지 확인

### Step 8 — Unfreeze + 24h 모니터링 (T+1:10 ~)

Slack 공지:
```
✅ [DEV UNFREEZE] S3 키 스키마 v1.0 전환 완료
- 모든 신규 업로드: interviews/raw/...
- Lambda는 신규 스키마만 처리 (레거시 Skipped)
- 24h 모니터링 중
```

**24h 모니터링 항목**:
- CloudWatch `/aws/lambda/rehearse-analysis-dev` Skipped 로그 증가 추이 (0에 수렴해야 정상)
- CloudWatch `/aws/lambda/rehearse-convert-dev` Skipped 로그 증가 추이
- Lambda Errors 메트릭
- `file_metadata.s3_key` 신규 포맷 비율 (100%여야 함)
- EventBridge DLQ (구성되어 있다면) 메시지 0
- Skipped 로그에 **예상치 못한 키**가 찍히면 즉시 조사

### Step 9 — prod 문서 업데이트 (T+24h 안정 후)

다음 문서를 Plan 04의 Completed 사실을 반영해 갱신:

1. **`docs/plans/prod-environment-setup/requirements.md`**
   - 맨 위 "선행 프로젝트 완료" 섹션 추가: "`s3-key-schema-redesign` 2026-04-XX Completed"
   - Task 3 (S3 + EventBridge) 비고에 "신규 v1.0 스키마 전용" 명시
2. **`docs/plans/prod-environment-setup/plan-03-s3-eventbridge-mediaconvert.md`**
   - 이미 신규 prefix 기반으로 작성되어 있음 — 검토만 수행
   - Lifecycle 정책 5-prefix 테이블 확인, `s3-key-schema.md` SSOT 링크 추가
3. **`docs/plans/prod-environment-setup/progress.md`**
   - 상단 "선행 프로젝트" 섹션에 Completed 링크 기록
4. **`docs/architecture/s3-key-schema.md`** Changelog
   ```
   ## Changelog
   - v1.0 (2026-04-12) — 초기 정의
   - v1.0-dev (2026-04-XX) — dev 적용 완료, 레거시 prefix 삭제
   ```

### Rollback Plan

#### 롤백 의사결정 매트릭스

| 실패 시점 | 결정 | 복구 액션 |
|---|---|---|
| T+0:35 Lambda alias 전환 직후 (BE 미배포) | **즉시 alias 원복** | 아래 A |
| T+0:45 BE 배포 직후 (~5분 이내) | **alias + BE 동시 원복** | 아래 A + B |
| T+0:55 E2E 실패 (신규 키 DB 유입 시작) | **전면 재truncate 후 재배포** | 아래 A + B + C |
| T+1:10 이후 간헐 오류 | **전면 롤백보다 forward-fix 우선** | hotfix PR, alias는 유지 |

#### A. Lambda alias 원복 (수 초)
```bash
# 이전 버전 확인
PREV_ANALYSIS=$(aws lambda list-versions-by-function --function-name rehearse-analysis-dev \
  --query 'Versions[-2].Version' --output text)
PREV_CONVERT=$(aws lambda list-versions-by-function --function-name rehearse-convert-dev \
  --query 'Versions[-2].Version' --output text)

aws lambda update-alias --function-name rehearse-analysis-dev --name live --function-version "$PREV_ANALYSIS"
aws lambda update-alias --function-name rehearse-convert-dev --name live --function-version "$PREV_CONVERT"
```
- **중요**: `lambda-safe-deploy.sh`가 직전 버전을 publish했음을 가정. cutover 직전 publish 버전 번호를 문서화해둘 것 (컷오버 담당자가 runbook에 기록)

#### B. 백엔드 revert
```bash
git revert <Plan 02 merge commit>
git push origin develop   # deploy-dev.yml 자동 실행
# EC2 컨테이너 교체 약 2-3분
curl -sf https://api-dev.rehearse.co.kr/actuator/health   # UP 확인
```

#### C. 신규 키 orphan 정리 (롤백 후 DB/S3 재정합)

롤백 시점에 `file_metadata`가 이미 신규 키(`interviews/raw/...`)로 일부 채워진 경우, 레거시 백엔드·Lambda는 이 행을 처리할 수 없다 (prefix 불일치). 수동 정리:

```sql
-- 신규 키로 만들어진 orphan 식별
SELECT COUNT(*) FROM file_metadata WHERE s3_key LIKE 'interviews/raw/%';
SELECT COUNT(*) FROM file_metadata WHERE streaming_s3_key LIKE 'interviews/mp4/%';
```

```bash
-- S3 신규 prefix 객체 수집
aws s3 ls s3://rehearse-videos-dev/interviews/ --recursive --summarize | tail -2
```

복구 방법 2가지:
1. **전면 재truncate** (권장, 가장 단순):
   - Step 4의 TRUNCATE 블록 재실행 + `aws s3 rm s3://rehearse-videos-dev/interviews/ --recursive`
   - dev 테스트 데이터이므로 소실 허용 원칙과 정합
2. **부분 정리** (진행 중 작업 보존 필요 시):
   ```sql
   DELETE FROM file_metadata WHERE s3_key LIKE 'interviews/%';
   -- 연쇄적으로 question_set.file_metadata_id NULL 처리 필요 — CASCADE 미설정
   UPDATE question_set SET file_metadata_id = NULL WHERE file_metadata_id IN (...);
   ```

#### D. 삭제된 레거시 데이터 복구 시도 (원칙적 불가)

- dev는 재생성 허용 — 복구를 **시도하지 않는다**
- 이론상: 레거시 `videos/` 객체는 Versioning delete marker만 찍힌 상태이므로 `aws s3api list-object-versions --bucket rehearse-videos-dev --prefix videos/` + `delete-object --version-id <delete-marker-id>`로 이전 버전 복원 가능. 단, DB의 `file_metadata`는 truncate되어 매핑이 유실되므로 **업로드만 복원하고 메타데이터는 재구성 불가**. 투자 가치 낮음

### "오래 걸리더라도" 반영

- 드레인 여유 20분 (Lambda 최대 timeout 15분 + 5분 여유)
- 팀 공지 24시간 전 (긴급 요청자 없는지 확인)
- 후속 모니터링 24시간 (prod 전환 전 최소 안정화 기간)
- 총 예상 소요: **선행 작업 총 3~5일** (Plan 01~04 순차 + 컷오버 + 안정화)

## 담당 에이전트

- Implement: `devops-engineer` — 드레인·삭제·Lambda 전환·배포 집행
- Implement: `qa` — E2E smoke test 수행
- Review: `architect-reviewer` — 컷오버 순서·롤백 경로 검증
- Review: `code-reviewer` — 삭제 명령 안전성 (prod 버킷 오접근 차단 재확인)

## 검증

- Pre-cutover 체크리스트 전부 ✅
- 드레인 후 in-flight 분석 0건
- S3 `videos/`, `analysis-backup/` prefix 객체 0건
- DB `file_metadata` 레거시 키 보유 행 0건
- Lambda alias `live` 신규 버전 포인트
- 백엔드 헬스 200
- Scenario 1~3 전부 Pass
- 24h 모니터링 중 예상치 못한 Skipped 로그 0건
- prod-environment-setup 3개 문서 + s3-key-schema.md Changelog 업데이트
- `progress.md` Task 4 → Completed, requirements.md 상태 → Completed
