# Survey Snapshot — 2026-04-08

plan-01 / plan-02에서 수집한 현 상태 스냅샷. plan-06 롤백 기준점.

## 1. CloudFront Distribution

```
Id:     E2FQDE3SA90LO8
Status: Deployed
Aliases:
  - dev.rehearse.co.kr
  - rehearse.co.kr          ← plan-06에서 제거 대상
Cert:   arn:aws:acm:us-east-1:776735194358:certificate/536869da-eff0-4d96-84bf-12226d91f612
DefaultRoot: index.html
Logging: DISABLED            ← plan-06 안정화 게이트 전에 활성화 결정 필요 (별건)
```

### CustomErrorResponses (plan-06에서 보존 필수)
```json
{
  "Quantity": 2,
  "Items": [
    {"ErrorCode": 403, "ResponsePagePath": "/index.html", "ResponseCode": "200", "ErrorCachingMinTTL": 10},
    {"ErrorCode": 404, "ResponsePagePath": "/index.html", "ResponseCode": "200", "ErrorCachingMinTTL": 10}
  ]
}
```
SPA 라우팅 필수. plan-06 alias 제거 시 이 설정이 드롭되지 않는지 콘솔에서 확인.

## 2. ACM Certificates (us-east-1)

| ARN (tail) | Domain | SANs | InUse | NotAfter |
|---|---|---|---|---|
| `...a193aa3a` | dev.rehearse.co.kr | dev + `*.rehearse.co.kr` (wildcard) | ❌ false | 2026-10-01 |
| `...c666977c` | rehearse.co.kr | apex only | ❌ false | 2026-10-10 |
| **`...536869da`** | rehearse.co.kr | **apex + dev** (SAN) | ✅ **true** | 2026-10-10 |

**사용 중인 인증서는 `536869da`** — SAN에 apex와 dev를 모두 포함. plan-06에서 apex alias만 제거하면 되고 인증서 교체 불필요. 남는 두 인증서는 prod 구축 시 재활용 가능 (특히 `a193aa3a`의 wildcard는 서브도메인 확장에 유용).

## 3. DNS (가비아, 권위 NS 기준)

```
dev.rehearse.co.kr      CNAME  d2n8xljv54hfw0.cloudfront.net.  (TTL 929s 관측)
rehearse.co.kr          A      3.168.167.{27,42,50,115}        (CloudFront)
api-dev.rehearse.co.kr  A      54.180.188.135                   (EC2)
```
- apex가 CloudFront를 향하는 A 레코드 형태 → 가비아 CNAME-at-apex 미지원으로 추정
- plan-06에서 `@`(apex) A 레코드 삭제 대상. `dev`, `api-dev`는 유지

## 4. Google OAuth Client

**확인 필요 (사용자 콘솔 직접 조회)**
- Authorized JavaScript origins: ?
- Authorized redirect URIs: ?
  - 사용자 확인: `http://api-dev.rehearse.co.kr/login/oauth2/code/google` 등록 확인됨
  - plan-03 Step 1에서 `https://` 버전 추가 예정
  - plan-06에서 `http://` 버전 제거 예정

## 5. GitHub OAuth App

**확인 필요 (사용자 콘솔 직접 조회)**
- Homepage URL: ?
- Authorization callback URL: 현재 `http://api-dev.rehearse.co.kr/login/oauth2/code/github`로 추정
- plan-05 Step 3-ter에서 `https://` 버전으로 즉시 교체 예정 (GitHub는 1개만 등록 가능)

## 6. EC2 `~/rehearse/backend/.env` (SSH 확인)

```
SPRING_PROFILES_ACTIVE=dev
CORS_ALLOWED_ORIGINS=https://dev.rehearse.co.kr,https://rehearse.co.kr
# FRONTEND_URL  ← 없음 (application-dev.yml fallback 사용 = apex)
```
plan-05 Step 2에서 `FRONTEND_URL=https://dev.rehearse.co.kr` 추가 예정.

## 7. Backend 상태 (curl 실측)

```
$ curl -sI -X OPTIONS https://api-dev.rehearse.co.kr/api/interviews \
    -H "Origin: https://dev.rehearse.co.kr" -H "Access-Control-Request-Method: GET"
HTTP/1.1 200
Access-Control-Allow-Origin: https://dev.rehearse.co.kr    ← 이미 병행 허용
Access-Control-Max-Age: 3600                                ← preflight 캐시 1h

$ curl -sI https://api-dev.rehearse.co.kr/oauth2/authorization/google
Location: https://accounts.google.com/...&redirect_uri=http://api-dev.rehearse.co.kr/login/oauth2/code/google
         ^^^^^ 현재 HTTP (forward-headers-strategy 미설정)
```

## 롤백 기준점 (v1)

만약 작업 중 롤백이 필요하면:
1. CloudFront Distribution `E2FQDE3SA90LO8`의 Aliases를 `[dev.rehearse.co.kr, rehearse.co.kr]`로 복구
2. Cert는 `...536869da` 유지 (변경 없었음)
3. CustomErrorResponses는 위 JSON 그대로 복구
4. 가비아 DNS: apex A 레코드(`3.168.167.{27,42,50,115}`) 복구
5. EC2 `.env`: `FRONTEND_URL` 제거 또는 apex로 설정, `CORS_ALLOWED_ORIGINS`에 apex 재추가
6. Google OAuth Client: http callback 재추가, dev JS origin은 유지해도 무방
7. GitHub OAuth App callback: `http://api-dev.rehearse.co.kr/login/oauth2/code/github`로 복구
