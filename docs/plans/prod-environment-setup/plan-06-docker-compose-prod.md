# Plan 06: `docker-compose.prod.yml` + `nginx.prod.conf` 작성

> 상태: Draft
> 작성일: 2026-04-12
> 선행: Plan 01 (prod yml 보강)

## Why

dev와 prod는 동일한 `backend/docker-compose.yml`을 공유할 수 없다. 환경별로 다른 것들:

- 이미지 태그 (`:latest` vs `:prod`)
- `SPRING_PROFILES_ACTIVE` (`dev` vs `prod`)
- `S3_BUCKET` 환경변수 (하드코딩 제거)
- Nginx 설정 (`api-dev.rehearse.co.kr` vs `api.rehearse.co.kr` certbot 도메인)
- DB 볼륨명 (`mysql_data` vs `mysql_data_prod` — 동일 호스트에서 충돌 방지 + 식별성)
- Healthcheck 도메인

단일 compose + profile 분기 방식은 실수 여지가 커서(특히 `--env-file`을 잘못 지정하면 dev 구성이 prod 자원에 붙음) 물리적 파일 분리를 채택한다.

## 생성/수정 파일

| 파일 | 작업 |
|---|---|
| `backend/docker-compose.prod.yml` | 신규 작성 |
| `backend/nginx/nginx.prod.conf` | 신규 작성 (443 포함 본 설정) |
| `backend/nginx/nginx.cert-init.conf` | 신규 작성 (80-only, certbot 최초 발급용) |
| `backend/.env.example` | prod 섹션 주석 추가 (샘플 변수만) |

**EC2 내부에서는** `docker-compose.prod.yml`을 `docker-compose.yml`로 리네임해 올림 → compose 기본 파일 규약 준수 (plan-11의 SCP step에서 처리).

## 상세

### `backend/docker-compose.prod.yml`

```yaml
services:
  db:
    image: mysql:8.0
    container_name: rehearse-db
    environment:
      MYSQL_DATABASE: rehearse
      MYSQL_USER: ${DB_USERNAME}
      MYSQL_PASSWORD: ${DB_PASSWORD}
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD}
    ports:
      - "127.0.0.1:3306:3306"   # prod: localhost만 노출 (dev도 동일 검토 권장)
    volumes:
      - mysql_data_prod:/var/lib/mysql
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  backend:
    image: ${ECR_REGISTRY}/rehearse-backend:prod
    container_name: rehearse-backend
    expose:
      - "8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_URL: jdbc:mysql://db:3306/rehearse?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}
      CLAUDE_API_KEY: ${CLAUDE_API_KEY}
      CLAUDE_MODEL: ${CLAUDE_MODEL:-claude-sonnet-4-20250514}
      OPENAI_API_KEY: ${OPENAI_API_KEY}
      OPENAI_MODEL: ${OPENAI_MODEL:-gpt-4o-mini}
      INTERNAL_API_KEY: ${INTERNAL_API_KEY}
      JWT_SECRET: ${JWT_SECRET}
      FRONTEND_URL: https://rehearse.co.kr
      CORS_ALLOWED_ORIGINS: https://rehearse.co.kr,https://www.rehearse.co.kr
      AWS_REGION: ${AWS_REGION}
      # AWS 자격: EC2 IAM Role(IMDS) 경유 단일 경로. .env에 ACCESS KEY 두지 않음.
      # (env에 빈 값이라도 두면 DefaultCredentialsProvider가 env → IMDS 순서라 env 시도 후 실패 로그 발생)
      AWS_S3_BUCKET: rehearse-videos-prod
      GOOGLE_APPLICATION_CREDENTIALS: /app/gcp-credentials.json
      GOOGLE_TTS_ENABLED: "true"
      GOOGLE_TTS_VOICE_NAME: ${GOOGLE_TTS_VOICE_NAME:-ko-KR-Chirp3-HD-Schedar}
      GITHUB_CLIENT_ID: ${GITHUB_CLIENT_ID}
      GITHUB_CLIENT_SECRET: ${GITHUB_CLIENT_SECRET}
      GOOGLE_CLIENT_ID: ${GOOGLE_CLIENT_ID}
      GOOGLE_CLIENT_SECRET: ${GOOGLE_CLIENT_SECRET}
      ADMIN_PASSWORD: ${ADMIN_PASSWORD}
    volumes:
      - ./gcp-credentials.json:/app/gcp-credentials.json:ro
    depends_on:
      db:
        condition: service_healthy
    restart: unless-stopped

  nginx:
    image: nginx:alpine
    container_name: rehearse-nginx
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/conf.d/default.conf:ro
      - certbot-conf-prod:/etc/letsencrypt:ro
      - certbot-www-prod:/var/www/certbot:ro
    depends_on:
      - backend
    restart: unless-stopped

  certbot:
    image: certbot/certbot
    container_name: rehearse-certbot
    volumes:
      - certbot-conf-prod:/etc/letsencrypt
      - certbot-www-prod:/var/www/certbot
    # certbot은 renew만 수행. nginx reload는 host cron이 담당(plan-14 참조)
    # → docker.sock 마운트 회피, 권한 경계 명확.
    entrypoint: "/bin/sh -c 'trap exit TERM; while :; do certbot renew; sleep 12h & wait $${!}; done'"

volumes:
  mysql_data_prod:
  certbot-conf-prod:
  certbot-www-prod:
```

**dev `docker-compose.yml` 대비 차이**:

| 항목 | dev | prod |
|---|---|---|
| `backend.image` | `:latest` | `:prod` |
| `SPRING_PROFILES_ACTIVE` | `${SPRING_PROFILES_ACTIVE:-dev}` | `prod` (하드코딩) |
| `AWS_S3_BUCKET` | (not set, dev yml 기본값) | `rehearse-videos-prod` (하드코딩) |
| `FRONTEND_URL` | `.env` | `https://rehearse.co.kr` (하드코딩) |
| `CORS_ALLOWED_ORIGINS` | `.env` | `https://rehearse.co.kr,...` (하드코딩) |
| `db.ports` | `3306:3306` (외부 노출) | `127.0.0.1:3306:3306` (로컬만) |
| `mysql_data` 볼륨 | `mysql_data` | `mysql_data_prod` |
| `certbot-*` 볼륨 | `certbot-conf/www` | `certbot-conf-prod/www-prod` |

**하드코딩 의도**: prod는 런타임 오설정 여지를 줄여야 안전. `.env`에서 실수로 `SPRING_PROFILES_ACTIVE=dev`가 들어가도 prod compose는 prod로 강제.

### `backend/nginx/nginx.prod.conf`

Nginx는 prod EC2에서 **두 가지 역할**을 동시에 수행한다:
1. `api.rehearse.co.kr` → Spring Boot 프록시 (백엔드 API)
2. `rehearse.co.kr` (apex) → `https://www.rehearse.co.kr` 301 리디렉션 (가비아 apex CNAME 미지원 해소 — Plan 08 참조)

Let's Encrypt 인증서는 **멀티 도메인 1장**으로 `api.rehearse.co.kr` + `rehearse.co.kr` 둘 다 커버한다. 발급 디렉토리 이름은 `api.rehearse.co.kr` (certbot이 첫 `-d` 인자 기준으로 폴더 생성).

```nginx
# === apex (rehearse.co.kr) → www 301 리디렉션 전용 ===
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

# === api.rehearse.co.kr → Spring Boot 프록시 ===
server {
    listen 80;
    server_name api.rehearse.co.kr;

    # certbot challenge
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

    # 영상 업로드 대비
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

**dev `nginx.conf` 대비 차이**:
- `server_name`: `api-dev.rehearse.co.kr` → `api.rehearse.co.kr`
- **신규 추가**: `rehearse.co.kr` apex 리디렉션 server block 2개 (80 + 443) — 가비아 apex CNAME 제약 해결
- `ssl_certificate` 경로: 멀티 도메인 `/etc/letsencrypt/live/api.rehearse.co.kr/*` (apex도 SAN으로 포함)
- 나머지 API 블록 구조는 동일

### 환경변수 `.env.prod` 샘플

`backend/.env.example`에 prod 섹션 주석으로 추가:

```bash
# === PROD (EC2: ~/rehearse/backend/.env) ===
# SPRING_PROFILES_ACTIVE=prod  # compose에서 하드코딩
# DB_USERNAME=rehearse_prod
# DB_PASSWORD=<prod 전용 강력 비밀번호>
# DB_ROOT_PASSWORD=<prod 전용 root 비밀번호>
# CLAUDE_API_KEY=sk-ant-...
# OPENAI_API_KEY=sk-...
# INTERNAL_API_KEY=<32+ chars>
# JWT_SECRET=<256bit secret>
# AWS_REGION=ap-northeast-2
# AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY → prod는 EC2 IAM Role만 사용.
#   .env에 두지 않는다 (빈 값이어도 SDK가 env 경로를 먼저 시도해 오류 로그 발생)
# ECR_REGISTRY=776735194358.dkr.ecr.ap-northeast-2.amazonaws.com
# GITHUB_CLIENT_ID=<prod OAuth App>
# GITHUB_CLIENT_SECRET=<prod OAuth App>
# GOOGLE_CLIENT_ID=<prod OAuth Client>
# GOOGLE_CLIENT_SECRET=<prod OAuth Client>
# ADMIN_PASSWORD=<prod admin 비밀번호>
```

**주의**: prod backend는 EC2 IAM Role(IMDS) 단일 경로를 사용한다. `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY`는 `.env`에 **두지 않는다** — DefaultCredentialsProvider 체인이 env → sysProps → IMDS 순서라, 빈 값이라도 env에 존재하면 먼저 시도하다 `AmazonClientException` 로그가 발생한다. 최초 기동 후 backend 로그에 `software.amazon.awssdk.core.exception.SdkClientException: Unable to load credentials from any of the providers`가 없는지 확인.

### `backend/nginx/nginx.cert-init.conf` (최초 발급 전용, 80-only)

**이유**: `nginx.prod.conf`는 443 블록의 `ssl_certificate` 경로를 참조하므로, 인증서가 아직 없는 최초 기동 시 nginx 부팅이 실패한다. certbot HTTP-01 challenge를 위해 80 블록만 있는 별도 설정을 두고, 발급 성공 후 `nginx.prod.conf`로 교체한다.

```nginx
# === cert-init: 80 포트 ACME challenge만 ===
server {
    listen 80 default_server;
    server_name rehearse.co.kr api.rehearse.co.kr;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        return 404;
    }
}
```

**배포 방식**: 리포에 `backend/nginx/nginx.cert-init.conf`로 커밋. plan-13 Step 2에서 EC2에 scp 또는 `git pull` 후 `nginx/nginx.conf`로 복사 → nginx 기동 → certbot 발급 → `nginx.prod.conf`로 교체 → `docker compose restart nginx`.

### 최초 certbot 발급 절차 (컷오버 시점)

plan-13 Step 2 참조. 핵심 순서:
1. EC2에 `nginx.cert-init.conf`를 `nginx/nginx.conf`로 배치 → nginx 기동 (80 only)
2. `docker compose run --rm certbot certonly --webroot ... -d api.rehearse.co.kr -d rehearse.co.kr`
3. 발급 성공 확인 후 `nginx.prod.conf`를 `nginx/nginx.conf`로 교체
4. `docker compose restart nginx` → 443 활성화
5. host cron에 nginx reload 등록 (plan-14)

## 담당 에이전트

- Implement: `devops-engineer` — compose/nginx 작성
- Review: `architect-reviewer` — dev/prod 차이 일관성, 하드코딩 의도 검증

## 검증

- `docker compose -f docker-compose.prod.yml config` → 파싱 성공
- `docker compose -f docker-compose.prod.yml config --services` → `db backend nginx certbot` 4개
- nginx 문법 검증 (cert-init):
  `docker run --rm -v $(pwd)/nginx/nginx.cert-init.conf:/etc/nginx/conf.d/default.conf:ro nginx:alpine nginx -t`
  → `test is successful`
- nginx.prod.conf 문법 검증은 인증서 없이 불가 → 더미 self-signed 인증서 마운트로 smoke test (선택):
  ```bash
  openssl req -x509 -nodes -newkey rsa:2048 -days 1 \
    -subj "/CN=test" \
    -keyout /tmp/k.pem -out /tmp/c.pem
  docker run --rm \
    -v $(pwd)/nginx/nginx.prod.conf:/etc/nginx/conf.d/default.conf:ro \
    -v /tmp/c.pem:/etc/letsencrypt/live/api.rehearse.co.kr/fullchain.pem:ro \
    -v /tmp/k.pem:/etc/letsencrypt/live/api.rehearse.co.kr/privkey.pem:ro \
    nginx:alpine nginx -t
  ```
- dev `docker-compose.yml` diff로 의도적 차이만 남아있는지 확인
- `.env.example`에 prod 변수 주석 존재
- `nginx.cert-init.conf` 파일 존재 + 80 블록만 포함
- `progress.md` Task 6 → Completed
