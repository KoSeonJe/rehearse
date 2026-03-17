#!/bin/bash
# 초기 Let's Encrypt 인증서 발급 스크립트
# EC2에서 1회만 수동 실행: bash init-letsencrypt.sh

set -e

DOMAIN="api-dev.rehearse.co.kr"
EMAIL="koseonje9@gmail.com"

echo "=== 1. 기존 nginx 컨테이너 중지 ==="
docker compose --env-file .env stop nginx 2>/dev/null || true

echo "=== 2. HTTP-only nginx 임시 설정 생성 ==="
cat > /tmp/nginx-http-only.conf <<'NGINX'
server {
    listen 80;
    server_name api-dev.rehearse.co.kr;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        return 200 'OK';
        add_header Content-Type text/plain;
    }
}
NGINX

echo "=== 3. HTTP-only nginx 기동 ==="
docker run -d --name rehearse-nginx-temp \
  -p 80:80 \
  -v /tmp/nginx-http-only.conf:/etc/nginx/conf.d/default.conf:ro \
  -v rehearse-certbot-www:/var/www/certbot \
  nginx:alpine

echo "=== 4. Certbot으로 인증서 발급 ==="
docker run --rm \
  -v rehearse-certbot-conf:/etc/letsencrypt \
  -v rehearse-certbot-www:/var/www/certbot \
  certbot/certbot certonly \
    --webroot \
    --webroot-path=/var/www/certbot \
    --email "$EMAIL" \
    --agree-tos \
    --no-eff-email \
    -d "$DOMAIN"

echo "=== 5. 임시 nginx 제거 ==="
docker stop rehearse-nginx-temp && docker rm rehearse-nginx-temp
rm -f /tmp/nginx-http-only.conf

echo "=== 6. docker compose 볼륨 연결 ==="
# certbot 볼륨을 compose 볼륨 이름에 맞게 매핑
# compose는 backend_certbot-conf / backend_certbot-www 이름을 사용
# 이미 같은 볼륨이면 스킵
if ! docker volume inspect backend_certbot-conf >/dev/null 2>&1; then
  echo "볼륨 이름 맞추기: rehearse-certbot-conf → backend_certbot-conf"
  docker volume create backend_certbot-conf
  docker run --rm \
    -v rehearse-certbot-conf:/src \
    -v backend_certbot-conf:/dst \
    alpine sh -c 'cp -a /src/. /dst/'
fi

if ! docker volume inspect backend_certbot-www >/dev/null 2>&1; then
  echo "볼륨 이름 맞추기: rehearse-certbot-www → backend_certbot-www"
  docker volume create backend_certbot-www
  docker run --rm \
    -v rehearse-certbot-www:/src \
    -v backend_certbot-www:/dst \
    alpine sh -c 'cp -a /src/. /dst/'
fi

echo "=== 7. docker compose 재시작 (SSL 포함) ==="
docker compose --env-file .env up -d

echo ""
echo "=== 완료! ==="
echo "확인: curl -f https://$DOMAIN/actuator/health"
