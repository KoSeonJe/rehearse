# Plan 08: 가비아 DNS + ACM 인증서 구성 (Route53 미사용)

> 상태: Draft
> 작성일: 2026-04-12 (Route53 이관 기각, 가비아 유지로 재설계)

## Why

prod 서비스 도메인 바인딩과 HTTPS 인증서가 필요하다. **본 프로젝트는 Route53을 사용하지 않고 가비아 DNS를 유지한다** (dev 환경과 동일, 사용자 결정). 이로 인해 **가비아가 apex CNAME/ALIAS를 지원하지 않는 제약**을 해결해야 한다.

### 가비아 DNS 제약

- `rehearse.co.kr` apex에는 **CNAME 레코드를 둘 수 없다** (RFC 표준 제약)
- 가비아에는 Route53의 `ALIAS` 또는 다른 DNS의 `ANAME` 같은 apex CNAME-flattening 기능이 **없다**
- 가비아 **URL 포워딩** 기능은 HTTPS를 지원하지 않거나(평문 리디렉션) 프레임 포워딩(iframe)이라 SEO/SSL 손상
- CloudFront edge IP를 A 레코드에 직접 등록하는 방식은 AWS 공식 **비권장** (IP 가변)

### 해결 전략: EC2 nginx redirector + CloudFront는 www 전용

가비아에서 작동시키려면 apex 요청을 IP가 고정된 EC2로 보내고, EC2 nginx가 301 리디렉션으로 `www.rehearse.co.kr`로 보낸다. 이 EC2는 이미 `api.rehearse.co.kr`을 서빙 중인 prod EC2를 겸용한다.

```
사용자 → https://rehearse.co.kr (apex)
        └─→ 가비아 DNS A → prod EC2 Elastic IP
             └─→ Nginx 443 (rehearse.co.kr server block, Let's Encrypt)
                  └─→ 301 → https://www.rehearse.co.kr/...

사용자 → https://www.rehearse.co.kr (primary)
        └─→ 가비아 DNS CNAME → prod CloudFront d*.cloudfront.net
             └─→ S3 rehearse-frontend-prod

사용자 → https://api.rehearse.co.kr (API)
        └─→ 가비아 DNS A → prod EC2 Elastic IP
             └─→ Nginx 443 (api.rehearse.co.kr server block)
                  └─→ Spring Boot
```

**장점**:
- 가비아 DNS 그대로 유지, 이관 리스크 0
- apex 리디렉션이 nginx 수준에서 정상 작동 (HTTPS + 301)
- Let's Encrypt 멀티 도메인 인증서 1장으로 apex + api 커버

**단점**:
- apex 접속자는 EC2 리디렉션을 거침 → 100ms 내외 latency 추가 (실질 영향 미미)
- prod EC2 다운 시 apex 다운 (사용자가 www로 접속하면 정상 — 실무상 수용 가능)

## 생성/수정 파일

| 자원 / 파일 | 작업 |
|---|---|
| 가비아 DNS 레코드 | 신규 추가 (apex, www, api) |
| ACM 인증서 (us-east-1) | `www.rehearse.co.kr` 1장 발급 (CloudFront용) |
| DNS validation CNAME | 임시 추가 (ACM 발급용, 검증 후 유지) |
| Let's Encrypt 인증서 (EC2) | `api.rehearse.co.kr` + `rehearse.co.kr` 멀티 도메인 1장 (plan-13 컷오버 시 발급) |
| `backend/nginx/nginx.prod.conf` | apex 리디렉션 server block 추가 (plan-06 연동) |
| `docs/architecture/infrastructure-status.md` | DNS/ACM 항목 추가 |

**Route53 이관 관련 내용은 전부 제거됨.**

## 상세

### 단계 1: ACM 인증서 발급 (us-east-1, CloudFront용)

AWS 콘솔 → **Region: US East (N. Virginia) us-east-1** → Certificate Manager → Request certificate.

| 설정 | 값 |
|---|---|
| Certificate type | Public certificate |
| Fully qualified domain name | `www.rehearse.co.kr` |
| Additional names | (없음) |
| Validation method | DNS validation |
| Key algorithm | RSA 2048 |
| Tags | `Environment=prod`, `Project=rehearse`, `Usage=cloudfront` |

**왜 apex(`rehearse.co.kr`)를 SAN에 포함하지 않는가**: CloudFront에 apex를 alternate domain으로 등록할 수 없기 때문(가비아 apex CNAME 미지원). apex는 EC2 nginx가 담당 → Let's Encrypt로 별도 발급.

발급 요청 후 ACM이 표시하는 **CNAME 검증 레코드**를 가비아 DNS에 추가:

| 호스트 | 타입 | 값 |
|---|---|---|
| `_<hash>.www.rehearse.co.kr` | CNAME | `_<hash>.<acm-validation>.acm-validations.aws` |

전파 후 "Issued" 전환 확인. ARN 기록 → plan-07 CloudFront distribution에서 바인딩.

### 단계 2: 가비아 DNS 레코드 (prod)

가비아 콘솔 → 내 도메인 → DNS 관리 → 레코드 추가.

| 호스트 | 타입 | 값 | TTL | 비고 |
|---|---|---|---|---|
| `@` (apex) | **A** | `<prod EC2 Elastic IP>` | 300 | nginx 리디렉션 담당 |
| `www` | CNAME | `<prod CloudFront 도메인 d*.cloudfront.net>` | 300 | 실제 프론트 서빙 |
| `api` | A | `<prod EC2 Elastic IP>` | 300 | 백엔드 API |
| `dev` | CNAME | `d2n8xljv54hfw0.cloudfront.net` | (기존) | **변경 없음** (dev 유지) |
| `api-dev` | A | `54.180.188.135` | (기존) | **변경 없음** |

**주의**:
- 가비아 DNS UI에서 호스트 필드는 도메인 접미사 없이 입력 (`api`만, `api.rehearse.co.kr` 아님)
- apex는 보통 `@` 또는 빈 문자열로 입력 (가비아 UI 가이드 확인)
- TTL은 컷오버 전날 300s로 낮춤 → 컷오버 후 3600s 원복

**컷오버 전 TTL 낮춤 계획**:
- T-24h: `api`, `www`, `@` 레코드 TTL을 3600 → 300s로 수정 공지
- 컷오버 완료 후 24h 안정화 뒤 TTL 원복

### 단계 3: EC2 Let's Encrypt 인증서 발급 (plan-13 컷오버 시점)

`api.rehearse.co.kr`과 `rehearse.co.kr` 양쪽 A 레코드가 prod EC2 Elastic IP로 전파된 후:

```bash
# prod EC2 SSH 접속
ssh -i ~/.ssh/rehearse-prod-key.pem ubuntu@<prod-EIP>
cd ~/rehearse/backend

# Nginx 443 블록을 임시로 주석 처리한 초기 설정(nginx.cert-init.conf)으로 기동
# (또는 plan-06의 nginx.prod.conf 80-only 변형본 사용)
docker compose --env-file .env up -d nginx

# certbot 멀티 도메인 최초 발급 (HTTP-01 chall via webroot)
docker compose run --rm --entrypoint "\
  certbot certonly --webroot -w /var/www/certbot \
  --email <관리자 email> --agree-tos --no-eff-email \
  -d api.rehearse.co.kr \
  -d rehearse.co.kr" certbot

# 발급 확인 (멀티 도메인 1장)
docker exec rehearse-nginx ls /etc/letsencrypt/live/api.rehearse.co.kr/
#   fullchain.pem  privkey.pem  (certbot는 첫 도메인명으로 디렉토리 생성)
```

**발급 결과**: `/etc/letsencrypt/live/api.rehearse.co.kr/fullchain.pem` 한 장에 SAN으로 `api.rehearse.co.kr`과 `rehearse.co.kr` 둘 다 포함.

**주의**: `-d rehearse.co.kr`에는 `www` 포함하지 않음. www는 CloudFront + ACM(us-east-1)이 담당.

### 단계 4: Nginx 443 server block 교체

최초 발급 후 실제 prod nginx 설정으로 교체 (plan-06 참조, 아래는 apex 리디렉션 포함 버전):

```nginx
# === apex → www 301 리디렉션 전용 server block ===
server {
    listen 80;
    server_name rehearse.co.kr;
    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }
    location / {
        return 301 https://www.rehearse.co.kr$request_uri;
    }
}

server {
    listen 443 ssl http2;
    server_name rehearse.co.kr;

    ssl_certificate /etc/letsencrypt/live/api.rehearse.co.kr/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.rehearse.co.kr/privkey.pem;

    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_prefer_server_ciphers on;

    return 301 https://www.rehearse.co.kr$request_uri;
}

# === api.rehearse.co.kr API 프록시 ===
server {
    listen 80;
    server_name api.rehearse.co.kr;
    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }
    location / {
        return 301 https://$host$request_uri;
    }
}

server {
    listen 443 ssl http2;
    server_name api.rehearse.co.kr;

    ssl_certificate /etc/letsencrypt/live/api.rehearse.co.kr/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.rehearse.co.kr/privkey.pem;

    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_prefer_server_ciphers on;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384;
    ssl_session_timeout 1d;
    ssl_session_cache shared:SSL:10m;

    client_max_body_size 10M;

    location / {
        proxy_pass http://backend:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 300s;
        proxy_connect_timeout 10s;
        proxy_send_timeout 60s;
    }
}
```

**포인트**:
- 인증서 파일명은 `api.rehearse.co.kr` 디렉토리 아래에 있지만 SAN으로 apex도 커버
- apex server block은 **리디렉션만** 수행 (proxy_pass 없음)
- 80 포트 블록에도 `.well-known/acme-challenge/`를 노출해 certbot renew가 작동하도록 함

### 단계 5: certbot 자동 갱신 확인

plan-06의 `certbot` 서비스가 12시간마다 `certbot renew`를 실행한다. 멀티 도메인 인증서도 동일하게 갱신됨.

```bash
# 수동 dry-run (안전 확인)
docker compose run --rm --entrypoint "certbot renew --dry-run" certbot
```

### CloudFront 측 변경 (plan-07 연동)

`plan-07-cloudfront-frontend.md`의 Alternate domain names에서 **apex 제거**, `www.rehearse.co.kr`만 등록:

| 항목 | 변경 전 (초안) | 변경 후 |
|---|---|---|
| Alternate domain names | `rehearse.co.kr`, `www.rehearse.co.kr` | `www.rehearse.co.kr` |
| us-east-1 ACM SAN | apex + www | **www만** |

Plan 07 문서의 동일 섹션을 별도 업데이트 (본 plan의 "후행 영향" 섹션 참조).

## 검증

- `aws acm list-certificates --region us-east-1` → `www.rehearse.co.kr` Issued
- `dig www.rehearse.co.kr` → 신규 prod CloudFront 도메인 (d*.cloudfront.net)
- `dig rehearse.co.kr` → prod EC2 Elastic IP
- `dig api.rehearse.co.kr` → prod EC2 Elastic IP
- `dig dev.rehearse.co.kr` → 기존 dev CloudFront (회귀 없음)
- `dig api-dev.rehearse.co.kr` → `54.180.188.135` (회귀 없음)
- `curl -vI https://www.rehearse.co.kr` → 200, SSL 유효 (ACM us-east-1 인증서)
- `curl -vI https://rehearse.co.kr` → 301, Location: `https://www.rehearse.co.kr/`
- `curl -vI http://rehearse.co.kr` → 301, Location: `https://www.rehearse.co.kr/`
- `curl -vI https://api.rehearse.co.kr/actuator/health` → 200 (certbot 발급 완료 후)
- Let's Encrypt 인증서 SAN 확인:
  ```bash
  openssl s_client -connect api.rehearse.co.kr:443 -servername api.rehearse.co.kr </dev/null 2>/dev/null | openssl x509 -noout -text | grep -A 1 "Subject Alternative Name"
  # → DNS:api.rehearse.co.kr, DNS:rehearse.co.kr
  ```
- SSL Labs 스캔 `rehearse.co.kr`, `www.rehearse.co.kr`, `api.rehearse.co.kr` 모두 A 등급 이상
- 브라우저로 `https://rehearse.co.kr` 접속 → 자동 `https://www.rehearse.co.kr` 리디렉션 확인
- `progress.md` Task 8 → Completed

## 담당 에이전트

- Implement: `devops-engineer` — ACM 발급, 가비아 DNS 레코드 추가, nginx 설정
- Review: `architect-reviewer` — 리디렉션 흐름, 인증서 배치

## 후행 영향 (다른 plan 업데이트 필요)

본 plan이 Route53 이관 대신 가비아 유지로 변경되면서 다음 문서들이 영향받는다. 본 plan 머지와 동시에 업데이트:

- **`plan-06-docker-compose-prod.md`**: `nginx.prod.conf`에 apex 리디렉션 server block 추가 (본 문서 "단계 4" 내용 반영)
- **`plan-07-cloudfront-frontend.md`**: Alternate domain names에서 apex 제거, `www.rehearse.co.kr`만 등록
- **`plan-13-cutover-smoke-test.md`**: 컷오버 절차에서 "certbot 발급 도메인 2개" 명시, Route53 언급 제거
- **`requirements.md`**: "아키텍처 / 설계" 목표 After 다이어그램에서 apex 흐름 수정 (apex → EC2 nginx → 301 → www CloudFront)
