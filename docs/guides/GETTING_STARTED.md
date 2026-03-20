# Rehearse 프로젝트 실행 가이드

## 사전 요구사항

| 도구 | 버전 | 확인 명령어 |
|------|------|------------|
| Java | 21+ | `java -version` |
| Node.js | 20+ | `node -v` |
| npm | 10+ | `npm -v` |
| Git | 2.x | `git --version` |

---

## Backend 실행

### 1. 환경변수 설정

```bash
cd backend
cp .env.example .env
```

`.env` 파일에 Claude API 키를 설정합니다:

```
CLAUDE_API_KEY=your-api-key-here
```

> 또는 `src/main/resources/application-dev.yml`에서 직접 설정 가능합니다.

### 2. 서버 실행

```bash
./gradlew bootRun
```

- 기본 포트: `http://localhost:8080`
- H2 콘솔: `http://localhost:8080/h2-console` (dev 프로필)
- Health Check: `http://localhost:8080/actuator/health`

### 3. 테스트

```bash
./gradlew test
```

---

## Frontend 실행

### 1. 의존성 설치

```bash
cd frontend
npm install
```

### 2. 개발 서버 실행

```bash
npm run dev
```

- 기본 포트: `http://localhost:5173`
- API 프록시: `/api/*` → `http://localhost:8080` (Vite 프록시 설정)

### 3. 빌드 / 타입 체크

```bash
npm run build          # TypeScript 컴파일 + Vite 빌드
npx tsc --noEmit       # 타입 체크만
```

---

## 전체 실행 순서

1. Backend 실행: `cd backend && ./gradlew bootRun`
2. Frontend 실행: `cd frontend && npm run dev`
3. 브라우저에서 `http://localhost:5173` 접속

---

## 주요 API 엔드포인트

| Method | URL | 설명 |
|--------|-----|------|
| POST | `/api/v1/interviews` | 면접 생성 (multipart/form-data) |
| GET | `/api/v1/interviews/{id}` | 면접 조회 |
| PATCH | `/api/v1/interviews/{id}/status` | 면접 상태 변경 |
| POST | `/api/v1/interviews/{id}/follow-up` | 후속질문 생성 |
| POST | `/api/v1/interviews/{interviewId}/question-sets/{qsId}/answers` | 답변 타임스탬프 저장 |
| POST | `/api/v1/interviews/{interviewId}/question-sets/{qsId}/upload-url` | S3 업로드 URL 발급 |
| GET | `/api/v1/interviews/{interviewId}/question-sets/{qsId}/status` | 분석 상태 조회 |
| GET | `/api/v1/interviews/{interviewId}/question-sets/{qsId}/feedback` | 질문세트 피드백 조회 |
| GET | `/api/v1/interviews/{interviewId}/question-sets/{qsId}/questions-with-answers` | 질문+답변 조회 |
| POST | `/api/v1/interviews/{interviewId}/question-sets/{qsId}/retry-analysis` | 분석 재시도 |
| GET | `/api/v1/interviews/{id}/report` | 종합 리포트 조회 |

---

## 트러블슈팅

### 카메라/마이크 접근 실패
- Chrome 브라우저 권장 (Web Speech API, MediaRecorder 호환)
- `chrome://settings/content/camera`에서 권한 확인
- HTTPS 또는 localhost에서만 미디어 접근 가능

### H2 콘솔 접속
- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:rehearse`
- Username: `sa`, Password: (없음)
