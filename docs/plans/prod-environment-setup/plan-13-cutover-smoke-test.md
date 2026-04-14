# Plan 13: 최초 컷오버 + E2E 스모크 테스트

> 상태: Draft
> 작성일: 2026-04-12
> 선행: Plan 01~12 모두 완료

## Why

plan-01~12가 각 영역에서 prod 자원을 준비하지만, 실제로 `https://rehearse.co.kr`과 `https://api.rehearse.co.kr`을 **처음으로 사용자 트래픽에 노출**하는 작업은 본 플랜에서 일괄 실행한다. 순서가 섞이면 다음 장애가 발생한다:

- certbot 발급 전 443 노출 → TLS handshake 실패
- DNS 전파 전 OAuth 콜백 등록 → 콘솔 검증 실패
- Flyway 미완료 상태에서 프론트 트래픽 유입 → 500 폭증
- dev 회귀 (apex/www 레코드가 dev CloudFront로 갔던 이력이 있어 DNS 이관 중 혼선)

컷오버 직전·직후 체크리스트와 E2E 시나리오를 명시하고, 문제 발생 시 즉시 dev로 되돌릴 수 있는 rollback path를 준비한다.

## 생성/수정 파일

| 자원 / 파일 | 작업 |
|---|---|
| (인프라 단계 집행만) | 코드 변경 없음 |
| `docs/architecture/infrastructure-status.md` | prod 전환 완료 후 최신화 |

## 상세

### Pre-cutover 체크리스트 (컷오버 전날까지 전부 Green)

- [ ] **plan-01** — `application-prod.yml` 보강 PR merge 완료 (`./gradlew test`)
- [ ] **plan-02** — prod EC2 인스턴스 기동, SSH 접속, Docker/AWS CLI 확인, `gcp-credentials.json` 업로드
- [ ] **plan-03** — `rehearse-videos-prod` S3 + EventBridge 규칙 + DLQ 생성
- [ ] **plan-04** — `rehearse-analysis-prod`, `rehearse-convert-prod` Lambda alias `live` 생성, smoke invoke 성공
- [ ] **plan-05** — ECR Lifecycle Policy 적용
- [ ] **plan-06** — `docker-compose.prod.yml` + `nginx.prod.conf` 리포 merge
- [ ] **plan-07** — `rehearse-frontend-prod` S3 + prod CloudFront distribution 생성 (인증서 바인딩 전 `Deployed` 상태)
- [ ] **plan-08** — ACM us-east-1 `www.rehearse.co.kr` Issued, CloudFront에 인증서 바인딩, **가비아 DNS 레코드 추가** (apex A=EC2 EIP, www CNAME=CloudFront, api A=EC2 EIP)
- [ ] **plan-09** — Google / GitHub OAuth prod client 생성, redirect URI 등록
- [ ] **plan-10** — GitHub Environments `development` / `production` 분리, prod secrets 입력, required reviewer 등록
- [ ] **plan-11** — `deploy-prod.yml` merge (단 `main`에 첫 push 전까지 대기)
- [ ] prod `.env.prod` 파일 작성 완료, EC2 `/home/ubuntu/rehearse/backend/.env`로 scp 업로드
- [ ] prod `INTERNAL_API_KEY`, `JWT_SECRET` 신규 발급·저장
- [ ] DNS TTL 낮춤 (300s) — 컷오버 전날 아침 이전 (기존 TTL 만료 대기 고려)
- [ ] **유지보수 페이지 사전 준비**: `maintenance.html` 작성 후 `rehearse-frontend-prod` 루트에 업로드. CloudFront Custom Error Response 403/404 → `/maintenance.html`(200)로 **임시 변경 가능한 상태**로 확인 (롤백 시 즉시 전환용)
- [ ] **plan-06 `nginx.cert-init.conf` 파일**이 리포에 머지되어 있고 EC2에서 접근 가능
- [ ] **docker-compose.prod.yml, nginx.prod.conf, nginx.cert-init.conf**가 EC2 `~/rehearse/backend/`에 최신 버전으로 배치 (git pull 또는 scp)

### Cutover Day 순서

**시간대**: 사용자 영향 최소화 시간 (평일 오전 10시 또는 주말 점심)
**예상 소요**: 60~90분 (DNS 전파 포함)
**다운타임**: 최대 30분 (첫 EC2 기동 + certbot + Flyway 순차)

#### Step 1: GitHub `main` 브랜치 준비 (00:00~00:05)
```bash
git checkout main
git merge --no-ff develop
git push origin main
```
→ `deploy-prod.yml` 트리거 → GitHub Actions에서 reviewer approval 대기 상태 진입. **이 시점에서는 approve 하지 말 것**. 인프라 단계 먼저 진행.

#### Step 2: certbot 멀티 도메인 발급 (00:05~00:15)
```bash
ssh -i ~/.ssh/rehearse-prod-key.pem ubuntu@<prod-EIP>
cd ~/rehearse/backend

# 0. 사전 배치 상태 재확인 (pre-cutover 체크리스트에서 이미 배치되어야 함)
ls nginx/nginx.cert-init.conf nginx/nginx.prod.conf docker-compose.yml
# 셋 다 존재해야 함. 누락 시 즉시 중단하고 scp/git pull로 복구.

# 1. cert-init 설정을 활성 nginx.conf로 배치 (80-only, ssl 지시어 없음)
cp nginx/nginx.cert-init.conf nginx/nginx.conf

# 2. nginx만 먼저 기동 (db/backend는 Step 4에서)
docker compose --env-file .env up -d nginx

# 3. DNS 전파 검증 (양쪽 필수 — HTTP-01 challenge는 A 레코드 기반)
dig +short api.rehearse.co.kr   # prod EC2 EIP와 일치해야 함
dig +short rehearse.co.kr        # prod EC2 EIP와 일치해야 함
# 불일치 시 가비아 DNS 전파 대기 후 재확인

# 4. api.rehearse.co.kr + rehearse.co.kr 멀티 도메인 1장 발급
docker compose run --rm --entrypoint "\
  certbot certonly --webroot -w /var/www/certbot \
  --email <관리자> --agree-tos --no-eff-email \
  -d api.rehearse.co.kr \
  -d rehearse.co.kr" certbot

# 5. 발급 확인 (certbot는 첫 도메인명으로 디렉토리 생성)
#    → certbot 컨테이너는 one-shot이므로 볼륨을 새 컨테이너로 마운트해 조회
docker run --rm -v rehearse-backend_certbot-conf-prod:/etc/letsencrypt:ro \
  alpine ls /etc/letsencrypt/live/api.rehearse.co.kr/
# fullchain.pem privkey.pem ...

# 6. SAN 확인 (apex도 포함되어야 함)
docker run --rm -v rehearse-backend_certbot-conf-prod:/etc/letsencrypt:ro \
  alpine:latest sh -c "apk add --no-cache openssl >/dev/null && \
    openssl x509 -in /etc/letsencrypt/live/api.rehearse.co.kr/fullchain.pem -noout -text" \
  | grep -A 1 "Subject Alternative Name"
# → DNS:api.rehearse.co.kr, DNS:rehearse.co.kr
```

**중요**: 이 단계는 `api.rehearse.co.kr` **와** `rehearse.co.kr` 양쪽 DNS A 레코드가 prod EC2 Elastic IP로 전파된 상태여야 성공 (apex도 HTTP-01 challenge 통과 필요). Pre-cutover에서 plan-08 가비아 DNS 레코드 추가 + `dig` 검증 완료했어야 함.

**주의**:
- `-d rehearse.co.kr`에는 `www` 포함 안 함. www는 CloudFront가 us-east-1 ACM 인증서로 독립 처리.
- `nginx.cert-init.conf`는 `ssl_certificate` 지시어가 **없는** 80-only 설정이어야 함(plan-06). 443 블록 포함 시 인증서 부재로 nginx 부팅 실패.
- 볼륨명 `rehearse-backend_certbot-conf-prod`는 compose 프로젝트명 prefix 기반. `docker volume ls` 로 실제 이름 확인 후 사용.

#### Step 3: Nginx 443 활성화 + 재시작 (00:15~00:18)
```bash
# 1. cert-init → prod 본 설정 교체
cp nginx/nginx.prod.conf nginx/nginx.conf

# 2. nginx 재시작 (신규 443 블록 + 방금 발급한 인증서 로드)
docker compose --env-file .env restart nginx

# 3. 컨테이너 내 nginx -t로 문법 최종 확인
docker exec rehearse-nginx nginx -t
# → syntax is ok + test is successful

# 4. 외부에서 HTTPS 확인 (아직 backend 미기동이므로 502/503 예상)
curl -vI https://api.rehearse.co.kr
#   → SSL handshake 성공 + upstream 502 예상 (정상)
curl -vI https://rehearse.co.kr
#   → 301 Location: https://www.rehearse.co.kr/ (apex 리디렉션 정상)
```

**실패 시**: `nginx -t`가 실패하면 cert-init 설정으로 원복(`cp nginx/nginx.cert-init.conf nginx/nginx.conf && docker compose restart nginx`) 후 `nginx.prod.conf` 디버깅. 인증서 경로 오타가 가장 흔한 원인.

#### Step 4: Flyway 최초 마이그레이션 + 시드 데이터 (00:18~00:35)
→ plan-12 절차 그대로 집행. DB → backend 순차 기동 + 로그 감시 + **시드 데이터 적용**.

```bash
docker compose --env-file .env up -d db
# healthy 대기
docker compose --env-file .env up -d backend
docker compose --env-file .env logs -f backend | grep -i flyway
# 모든 마이그레이션 success=1 확인

# 시드 데이터 적용 (plan-12 "시드 데이터 적용" 섹션 참고)
# 주의: --default-character-set=utf8mb4 필수 (누락 시 한글 이중 인코딩)
for f in /tmp/seed/*.sql; do
  [[ "$(basename $f)" == "README.md" ]] && continue
  docker exec -i rehearse-db mysql -u rehearse -p"$DB_PASSWORD" --default-character-set=utf8mb4 rehearse < "$f"
done
```

#### Step 5: GitHub Actions approval (00:30~00:35)
- Actions 페이지에서 Step 1의 run 찾기
- `Review deployments` → `Approve and deploy`
- Frontend S3 sync + CloudFront invalidation 자동 실행 확인
- Backend ECR push + EC2 pull/up 자동 실행 확인 (Step 4에서 이미 올라가 있으므로 이미지만 업데이트)

**주의**: Step 4에서 backend를 이미 올렸고 Step 5에서 `deploy-prod.yml`이 다시 up을 시도한다. 멱등이므로 문제 없지만, 이 순서가 직관과 반대이므로 **본 플랜에서는 Step 4를 건너뛰고 Step 5가 최초 기동까지 담당**하는 변형도 가능. 선택:

- **옵션 A (권장)**: Step 4를 수동으로 먼저 실행 → Flyway 실패 시 GitHub Actions에 영향 없이 복구 가능
- **옵션 B**: Step 4 skip, Step 5가 최초 기동. Flyway 실패 시 Actions job 실패 + 복구 어려움

→ **옵션 A 선택**.

#### Step 6: 공인 Health check (00:35~00:38)
```bash
curl -sI https://api.rehearse.co.kr/actuator/health
# HTTP/2 200
# {"status":"UP"}
```

#### Step 7: 프론트 접근 확인 (00:38~00:40)
```bash
curl -sI https://rehearse.co.kr | head -5
# HTTP/2 200
curl -sI https://www.rehearse.co.kr | head -5
# HTTP/2 200
```
브라우저로 `https://rehearse.co.kr` 접속 → SPA 로드 확인, 콘솔 에러 없음, Network 탭 `api.rehearse.co.kr` 호출 확인.

### E2E Smoke Test (00:40~01:10)

#### Scenario 1: 비로그인 홈
- [ ] `https://rehearse.co.kr` → 메인 페이지 렌더
- [ ] DevTools Console: 에러 0
- [ ] DevTools Network: `api.rehearse.co.kr` 호출만 (dev/apex 없음)

#### Scenario 2: GitHub OAuth 로그인
- [ ] `Sign in with GitHub` 클릭 → `github.com/login/oauth/authorize?...` 리다이렉트
- [ ] 승인 후 `https://api.rehearse.co.kr/login/oauth2/code/github?code=...` 응답
- [ ] 최종 `https://rehearse.co.kr/...` 리다이렉트, JWT 쿠키 세팅 (DevTools Application → Cookies 확인)
- [ ] 사용자 프로필 표시

#### Scenario 3: Google OAuth 로그인
- [ ] Scenario 2와 동일, Google provider

#### Scenario 4: 면접 생성 + 완주 (가장 중요)
- [ ] 이력서 업로드 또는 직접 입력
- [ ] 면접 Setup → 질문 생성 확인 (Claude API 호출 → `api.rehearse.co.kr` 성공)
- [ ] 면접 시작 → 마이크/카메라 권한 허용
- [ ] 녹화 진행 → MediaRecorder → 업로드 시작
- [ ] S3 `rehearse-videos-prod/raw/*.webm` 업로드 확인 (S3 콘솔 또는 `aws s3 ls`)
- [ ] EventBridge → `rehearse-analysis-prod` 트리거 확인 (CloudWatch Logs)
- [ ] `rehearse-convert-prod` MediaConvert job 확인
- [ ] 피드백 UI 렌더 (타임스탬프 동기화, Nonverbal 분석 결과)

#### Scenario 5: 로그아웃
- [ ] 로그아웃 → JWT 쿠키 삭제 → 홈 리다이렉트

### Rollback 경로

**사전 준비 (Pre-cutover 필수)**:
1. **유지보수 페이지 업로드**: `maintenance.html`(정적 HTML, 503 안내 + 재방문 시각)을 `s3://rehearse-frontend-prod/maintenance.html`에 미리 업로드
2. **CloudFront Custom Error Response 사전 설정**: prod distribution에 Custom Error Response를 2종 등록하되, **롤백 시 활성화**할 수 있도록 TTL 0 + path `/maintenance.html` 프리셋을 프로파일로 준비
3. **DNS 원복 스크립트**: 가비아 DNS UI 조작 순서를 문서화(어떤 레코드를 무엇으로 되돌릴지 명시)한 `docs/guides/cutover-rollback.md` 작성

**Cutover 중 실패 시 (Decision Tree)**:

#### Case A: DNS 전환 전 실패 (Step 1~4)
- 사용자 영향 **0** — 가비아 DNS가 아직 변경되지 않아 트래픽이 prod로 가지 않음
- 조치: EC2 컨테이너 중지(`docker compose down`), Actions run 취소, 원인 분석

#### Case B: certbot 발급 실패 (Step 2)
- DNS는 전환되었지만 HTTPS 불가 상태
- 1차 조치: DNS 레코드 전파 재확인, `-d` 도메인 오타 확인, rate limit(5회/주) 초과 여부 확인
- 2차 조치: 가비아 DNS에서 `@`(apex), `www`, `api` 레코드를 **전부 삭제** → 사용자는 NXDOMAIN (dev는 무영향)
- 원복 후 재시도 일정 공지

#### Case C: Flyway 실패 (Step 4)
- backend 미기동 상태, nginx는 502 응답
- 1차 조치: plan-12 "실패 시 복구 절차" Case 1 (부분 실패) 또는 Case 2 (DB 전체 초기화)
- 2차 조치(복구 불가 시): Case D로 에스컬레이션

#### Case D: 배포 후 서비스 장애 (Step 5 이후, 사용자 트래픽 유입 중)
**복구 우선순위**: 사용자 영향 차단 > 원인 분석 > 재시도

1. **즉시(≤2분)**: CloudFront Custom Error Response 활성화 → 모든 요청에 `/maintenance.html` 반환
   ```bash
   aws cloudfront update-distribution --id <prod-dist-id> \
     --distribution-config file://maintenance-mode.json
   aws cloudfront create-invalidation --distribution-id <prod-dist-id> --paths "/*"
   ```
2. **동시에**: prod EC2 nginx의 api 블록을 **503 정적 응답**으로 교체 (사전 준비한 `nginx.maintenance.conf`)
   ```nginx
   location / { return 503; add_header Content-Type "application/json"; return 503 '{"error":"maintenance"}'; }
   ```
3. **분석 후**: 문제 해결 또는 dev로의 **정식** 폴백 (아래 Case E)

#### Case E: 완전 dev 폴백 (최후의 수단, 복구 ≥ 30분)
- dev CloudFront distribution에 임시로 `www.rehearse.co.kr` alias 추가 (us-east-1 ACM dev 인증서에 `www` SAN 추가 필요 → 재발급 30~60분)
- 가비아 DNS `www` CNAME을 `d2n8xljv54hfw0.cloudfront.net`으로 복원
- apex A 레코드 제거
- **실무상 이 경로는 30분 다운타임 목표를 초과**하므로 "시간적 여유가 있을 때만" 선택. 그 외는 Case D 유지보수 페이지로 시간 확보 후 fix-forward.

**롤백 사후 절차**:
- 실패 원인 분석 → postmortem 작성 (`docs/guides/prod-runbook.md` 템플릿 사용)
- Retry 재시도 일정 공지 (Slack / Notion)
- 실패 재발 방지 조치를 본 plan 문서에 추가

### Post-cutover (01:10~01:30)

- [ ] DNS TTL 원복 (3600s)
- [ ] `docs/architecture/infrastructure-status.md` 업데이트 (prod 자원 ARN/도메인/ID 전부)
- [ ] `progress.md` Task 13 → Completed
- [ ] `requirements.md` 상태 → `Completed`
- [ ] 팀에 prod 런칭 공지 (Slack/Notion/이메일)

## 담당 에이전트

- Implement: `devops-engineer` — 인프라 단계 집행
- Implement: `qa` — E2E 스모크 테스트 수행
- Review: `architect-reviewer` — 순서·의존성·롤백 경로 검증

## 검증

- Pre-cutover 체크리스트 전 항목 ✅
- Cutover 중 각 Step 성공
- E2E Scenario 1~5 전부 Pass
- Rollback path 문서 존재·검토 완료
- 컷오버 종료 시점에 `dev` 환경 회귀 없음 확인 (dev 접속·로그인·면접 완주)
- `progress.md` Task 13 → Completed
