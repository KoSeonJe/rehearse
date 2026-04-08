# Plan 05: EC2 `.env` 런타임 전환 + E2E 검증

> 상태: Draft (v2, 2026-04-08 개정)
> 작성일: 2026-04-08
> 의존: plan-02, plan-03, plan-04 모두 완료 [blocking]

## Why

이 태스크가 **실질적인 전환 시점**이다. 인프라(CloudFront / DNS / ACM)는 이미 apex+dev 병행 상태이고, plan-02(검증) · plan-03(OAuth 확인) · plan-04(코드 PR 머지)가 선행되면, 남은 작업은 EC2 런타임 환경변수 `FRONTEND_URL`과 `CORS_ALLOWED_ORIGINS`를 새 도메인으로 교체하는 것뿐이다. 이 순간부터 OAuth 성공 리다이렉트가 `dev.rehearse.co.kr`로 이동하고, 백엔드가 새 Origin을 공식 허용한다.

전환 중 기존 apex 접속자가 깨지지 않도록 **CORS는 apex도 병행 허용**한다 (plan-06에서 좁힘). 이후 전체 서비스 플로우에 대해 E2E 검증을 수행한다.

### 알려진 side-effect (수용)
- **기존 apex 로그인 세션은 무효화**된다. JWT 쿠키가 host-only(`OAuth2SuccessHandler.java:38-43`, Domain 미설정)이므로 `rehearse.co.kr`의 쿠키는 `dev.rehearse.co.kr` 쿠키 jar로 이전되지 않는다. 사용자는 dev 도메인에서 **1회 재로그인** 필요.
- 대안: 쿠키 Domain을 `.rehearse.co.kr`로 확장하면 이전 가능하지만 보안 경계가 넓어지고 추후 prod 분리 시 혼선 → 기각.
- 액션: 전환 직전 서비스 공지/배너(선택)로 재로그인 필요 사실 안내.

### OAuth https 마이그레이션 동시 진행
plan-04에서 `server.forward-headers-strategy: framework`가 추가됐으므로, 본 PR이 EC2에 배포되는 순간부터 Spring은 OAuth `redirect_uri`를 `https://`로 생성한다. 이 변경의 안전 배포 절차:
1. plan-03 Step 1에서 Google OAuth Client에 https callback **추가** (http는 유지) — 사전 완료
2. plan-04 PR 머지 → deploy-dev 워크플로우 → EC2 새 이미지 배포 (이 본 태스크의 일부)
3. **Step 2-bis**: 배포 직후 즉시 GitHub OAuth App callback URL을 http → https로 **교체** (GitHub는 1개만 등록 가능)
4. plan-06에서 Google OAuth Client http callback **제거**

## 생성/수정 파일

| 리소스 | 작업 |
|------|------|
| EC2 `~/rehearse/backend/.env` | `FRONTEND_URL`, `CORS_ALLOWED_ORIGINS` 교체 (`.env.bak-*` 백업 생성) |
| EC2 docker compose | `backend` 컨테이너 재기동 |
| CloudFront Distribution `d2n8xljv54hfw0` | plan-02에서 준비한 invalidation `/*` 집행 (아직 안 했다면 지금) |
| `docs/plans/dev-domain-restore/survey-snapshot.md` | 전환 후 런타임 상태 기록 |

## 상세

### 1. 선행 확인 (Gate)
```bash
# plan-02 결과 확인
curl -I https://dev.rehearse.co.kr   # HTTP/2 200 + CloudFront 헤더

# plan-03 결과 확인
# Google OAuth Authorized JS origins에 dev.rehearse.co.kr 포함됨 (수동)

# plan-04 결과 확인 — develop 머지 + 배포 성공
gh run list --workflow=deploy-dev.yml --branch develop --limit 3
# 최근 run이 success 여야 하고, 해당 PR 머지 커밋이 포함되어야 함

# CloudFront invalidation 집행 (plan-02에서 유보했다면 지금)
aws cloudfront create-invalidation --distribution-id $DIST_ID --paths "/*"
```
위 4가지가 전부 OK일 때만 진행. 하나라도 실패 시 중단하고 해당 plan으로 복귀.

### 2. EC2 접속 및 `.env` 편집
```bash
ssh -i ~/.ssh/rehearse-key.pem ubuntu@54.180.188.135
cd ~/rehearse/backend

# 백업
cp .env .env.bak-$(date +%Y%m%d-%H%M%S)

# 변경 전 상태 기록 (survey-snapshot용)
grep -E '^(FRONTEND_URL|CORS_ALLOWED_ORIGINS)=' .env

# 편집 (vi/nano)
vi .env
```

**변경 내용**
```
FRONTEND_URL=https://dev.rehearse.co.kr
CORS_ALLOWED_ORIGINS=https://dev.rehearse.co.kr,https://rehearse.co.kr
```
- `FRONTEND_URL`은 단일 교체 → OAuth 성공 리다이렉트가 dev로
- `CORS_ALLOWED_ORIGINS`는 **dev + apex 병행** → 전환 중 apex 접속자의 API 호출도 허용 (plan-06에서 좁힘)

### 3. 백엔드 재기동
```bash
docker compose --env-file .env up -d backend
# (compose.yml이 env_file: .env를 갖고 있다면 --env-file은 생략 가능)

# 헬스체크 (deploy-dev.yml과 동일한 로직)
for i in $(seq 1 12); do
  sleep 5
  if docker exec rehearse-backend curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "Health OK (attempt $i)"
    break
  fi
  echo "Attempt $i/12..."
done
```

### 3-bis. OAuth redirect_uri가 https로 생성되는지 검증
```bash
curl -sI "https://api-dev.rehearse.co.kr/oauth2/authorization/google" | grep -i location
# 기대: Location: https://accounts.google.com/...&redirect_uri=https://api-dev.rehearse.co.kr/login/oauth2/code/google
#       (기존 http://가 https://로 바뀌었는지 확인)

curl -sI "https://api-dev.rehearse.co.kr/oauth2/authorization/github" | grep -i location
# 기대: Location: https://github.com/login/oauth/authorize?...&redirect_uri=https://api-dev.rehearse.co.kr/login/oauth2/code/github
```
**둘 다 https://로 나오지 않으면** `forward-headers-strategy` 설정이 반영되지 않은 것 → 배포 누락 의심, 롤백 후 plan-04 PR/배포 재확인.

### 3-ter. GitHub OAuth App callback URL 즉시 교체
**배포 직후 즉시 수행 (다운타임 최소화)**

1. GitHub Settings → Developer settings → OAuth Apps → 해당 앱
2. **Authorization callback URL**:
   ```
   Before: http://api-dev.rehearse.co.kr/login/oauth2/code/github
   After:  https://api-dev.rehearse.co.kr/login/oauth2/code/github
   ```
3. Update application

이 시점에 GitHub 로그인이 ~10초간 실패할 수 있음 (콘솔 저장 ↔ 새 callback 적용 race). 문제 발생 시 즉시 다시 http로 되돌리고 plan-04 배포를 롤백.

### 4. 1차 자동 검증
```bash
# 로컬 맥에서
curl -s https://api-dev.rehearse.co.kr/actuator/health | jq .
# 기대: {"status":"UP"}

curl -i -X OPTIONS https://api-dev.rehearse.co.kr/api/interviews \
  -H "Origin: https://dev.rehearse.co.kr" \
  -H "Access-Control-Request-Method: GET"
# 기대: Access-Control-Allow-Origin: https://dev.rehearse.co.kr
# 기대: Access-Control-Max-Age: 3600

curl -i -X OPTIONS https://api-dev.rehearse.co.kr/api/interviews \
  -H "Origin: https://rehearse.co.kr" \
  -H "Access-Control-Request-Method: GET"
# 기대: Access-Control-Allow-Origin: https://rehearse.co.kr (전환 중 병행 허용)
```

### 5. E2E 수동 검증 (브라우저)
`https://dev.rehearse.co.kr`에서 시크릿 창 / 새 세션으로 수행:

| # | 시나리오 | 기대 결과 |
|---|---|---|
| 1 | 메인 페이지 로드 | 200 OK, React SPA 정상 렌더, 새 번들 해시 로드 |
| 2 | GitHub 로그인 | `api-dev.../oauth2/authorization/github` → GitHub → `api-dev.../login/oauth2/code/github` → 최종 `dev.rehearse.co.kr/...` + JWT 쿠키 세팅 |
| 3 | Google 로그인 | 동일한 플로우, `origin_mismatch` 없음 |
| 4 | 인터뷰 생성 | `POST /api/interviews` 200, CORS 통과 |
| 5 | 영상 녹화 → S3 업로드 | presigned URL 요청 성공, S3 PUT 성공 |
| 6 | Lambda 분석 결과 수신 | 백엔드 콜백(`API_SERVER_URL`) 정상, 피드백 조회 가능 |
| 7 | 피드백 페이지 | timestamp 주석/영상 동기화 정상 |
| 8 | DevTools Network | 모든 요청이 `dev.rehearse.co.kr`/`api-dev.rehearse.co.kr`로, apex 호출 0건 |
| 9 | **apex 접근 regression 체크** | 별도 시크릿 창에서 `https://rehearse.co.kr` 접속 → 페이지는 로드되지만 재로그인 유도(host-only 쿠키) 되고 API 호출 CORS 통과 |

### 5-bis. 면접관 비디오 기능 (별건, 사전 안내)
`interviewer-video.tsx:8-11`의 mp4 URL 4개는 **원래 작동하지 않는 상태**다 (S3에 파일 부재 → SPA index.html fallback). 본 플랜 범위 밖이므로 검증 체크리스트에서 제외. 도메인 전환 후에도 이 기능은 여전히 작동하지 않음을 사전 공유 — 본 전환이 원인으로 오해되지 않도록 주의.

### 6. 관측성 확인
```bash
ssh -i ~/.ssh/rehearse-key.pem ubuntu@54.180.188.135 \
  "docker logs rehearse-backend --tail 200"
```
다음 패턴이 없는지 확인:
- `CORS` 차단 로그
- `OAuth2AuthenticationException`
- `redirect_uri_mismatch`
- 500/503 스파이크

최소 30분 tail해서 지속 모니터링 (`docker logs -f`).

### 7. 실패 시 롤백
```bash
cd ~/rehearse/backend
cp .env.bak-<timestamp> .env
docker compose up -d backend
# CloudFront/DNS/OAuth는 그대로 두고 런타임만 apex로 복귀
```
롤백 트리거:
- OAuth 로그인 실패율 > 10%
- CORS 차단 에러 빈발
- CloudFront 5xx 스파이크
- 위 중 하나라도 15분 이상 지속

## 담당 에이전트

- Implement: `devops-engineer` — SSH, `.env` 편집, docker compose 재기동, invalidation 집행
- Review: `qa` — E2E 시나리오 체크리스트 검증, host-only 쿠키 re-login 동작 확인
- Review: `code-reviewer` — EC2 쉘/설정 변경의 원자성과 롤백 가능성 검토

## 검증

- [ ] 섹션 3-bis curl 2개 전부 `https://` redirect_uri 확인
- [ ] 섹션 3-ter GitHub OAuth App callback URL `https://`로 교체 완료
- [ ] 섹션 4(자동 검증) 3개 curl 전부 통과
- [ ] 섹션 5(E2E 수동) 9개 시나리오 전부 통과 (#5-bis는 제외)
- [ ] EC2 로그에 CORS/OAuth 에러 없음 (30분 이상)
- [ ] 5xx 스파이크 없음
- [ ] `.env.bak-*` 백업 파일이 EC2에 남아 있음 (롤백 자료)
- [ ] `survey-snapshot.md`에 전환 전/후 런타임 env 값 기록
- [ ] `progress.md` Task 5 → Completed (완료 시각 기록 — plan-06 안정화 게이트 계산 기준점)
