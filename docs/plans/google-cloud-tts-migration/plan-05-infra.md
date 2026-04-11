# Plan 05: GCP 서비스 계정 발급 + EC2 배포

> 상태: Draft
> 작성일: 2026-04-11

## Why

Google Cloud TTS를 운영 환경에서 사용하려면 최소 권한을 가진 서비스 계정이 필요. EC2에 JSON 키를 안전하게 전달하고 docker-compose 재기동.

## 선행 조건

- GCP 프로젝트: `gen-lang-client-0294632335`
- Cloud Text-to-Speech API: **활성화 완료** (로컬 확인됨)
- 로컬 `gcloud` CLI 설치 및 `gcloud auth login` 완료

## 생성/수정 파일

| 위치 | 작업 |
|------|------|
| GCP | 서비스 계정 생성 + IAM 역할 부여 + JSON 키 발급 |
| 로컬 `backend/gcp-credentials.json` | 서비스 계정 JSON (gitignored) |
| EC2 `~/rehearse/backend/gcp-credentials.json` | JSON scp 업로드 |
| EC2 `~/rehearse/backend/.env` | `ELEVENLABS_*` 제거 |

## 상세

### 1. 서비스 계정 생성

```bash
PROJECT_ID="gen-lang-client-0294632335"
SA_NAME="rehearse-tts"
SA_EMAIL="${SA_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"

gcloud iam service-accounts create $SA_NAME \
  --display-name="Rehearse TTS Service Account" \
  --project=$PROJECT_ID
```

### 2. IAM 역할 부여 (최소 권한)

```bash
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/cloudtts.user"
```

> 참고: `roles/cloudtts.user`는 Text-to-Speech 합성만 허용하는 최소 권한. `roles/editor` 같은 광범위 권한은 피할 것.

### 3. JSON 키 발급

```bash
gcloud iam service-accounts keys create \
  ~/dev/devlens/backend/gcp-credentials.json \
  --iam-account=$SA_EMAIL

# 권한 제한 (선택)
chmod 600 ~/dev/devlens/backend/gcp-credentials.json
```

**반드시 `.gitignore`에 포함되어 있는지 확인** (Plan 04에서 처리).

### 4. 로컬 Docker 테스트 (선택)

```bash
cd ~/dev/devlens/backend
docker compose up -d backend
curl -X POST http://localhost:8080/api/v1/tts \
  -H 'Content-Type: application/json' \
  -d '{"text":"테스트"}' --output /tmp/tts-local.mp3
file /tmp/tts-local.mp3
```

### 5. EC2 배포

```bash
# JSON 키 업로드
scp -i ~/.ssh/rehearse-key.pem \
  ~/dev/devlens/backend/gcp-credentials.json \
  ubuntu@54.180.188.135:~/rehearse/backend/gcp-credentials.json

# EC2 접속 후 권한 설정 + .env 업데이트
ssh -i ~/.ssh/rehearse-key.pem ubuntu@54.180.188.135
chmod 600 ~/rehearse/backend/gcp-credentials.json
vi ~/rehearse/backend/.env
# ELEVENLABS_API_KEY, ELEVENLABS_VOICE_ID 라인 삭제

# 백엔드 재기동
cd ~/rehearse/backend
docker compose pull backend
docker compose up -d backend
docker compose logs -f backend  # GoogleTtsConfig 활성 로그 확인
```

### 6. 스모크 테스트

```bash
# 헬스체크
curl https://api-dev.rehearse.co.kr/actuator/health

# TTS 엔드포인트
curl -X POST https://api-dev.rehearse.co.kr/api/v1/tts \
  -H 'Content-Type: application/json' \
  -d '{"text":"안녕하세요, 저는 AI 면접관입니다"}' \
  --output /tmp/tts-ec2.mp3
file /tmp/tts-ec2.mp3  # 정상이면 "Audio file with ID3"
```

## 담당 에이전트

- Implement: `devops-engineer` — GCP 서비스 계정 발급 + EC2 배포
- Review: `architect-reviewer` — 권한 최소화, 시크릿 전달 경로 점검

## 검증

- `gcloud iam service-accounts list --project=gen-lang-client-0294632335` 에 `rehearse-tts` 존재
- EC2 `~/rehearse/backend/gcp-credentials.json` 존재 + `chmod 600`
- `docker compose logs backend | grep -i "texttospeech\|google.tts"` 에러 없음
- `curl https://api-dev.rehearse.co.kr/api/v1/tts` MP3 응답 정상
- `progress.md` 상태 업데이트 (Task 5 → Completed)
