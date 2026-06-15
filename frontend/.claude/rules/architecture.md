# Frontend Architecture

런타임 / 데이터 흐름 / 시스템 경계 단일 소스. 코드 컨벤션 = `conventions.md`.

---

## 진입점

```
main.tsx → HelmetProvider → QueryClientProvider (retry:1, staleTime:5m, refetchOnFocus:false)
        → BrowserRouter → App → TooltipProvider → Routes + LoginModal + Toaster(sonner)
```

`App` 마운트 전역 훅:
- `usePostLoginRedirect` — OAuth 콜백 후 원래 경로 복귀.
- `useAuthInterceptor` — `auth:unauthorized` CustomEvent → LoginModal.
- `useCrossTabSync` — `localStorage` storage 이벤트로 로그아웃 동기화.

---

## 라우팅 가드

| 가드 | 라우트 |
|------|-------|
| Public | `/`, `/about`, `/faq`, `/privacy`, `/guide/*` |
| `ProtectedRoute` | `/dashboard`, `/interview/*`, `/review-list` |
| `PasswordProtectedRoute` | `/admin/feedbacks` |

코드 분할 미적용 (eager import). `lazy()` 도입 시 본 문서 갱신.

---

## API 통신 (`lib/api-client.ts`)

- 단일 진입점 `apiClient` (fetch 래퍼). axios X. **직접 fetch 금지**.
- Base = `VITE_API_URL`. Dev = vite proxy `/api` → `http://localhost:8080`.
- `credentials: 'include'` — JWT **HttpOnly 쿠키** (FE 토큰 직접 보유 X).
- 401 + ≠ `/auth/me` → `auth:unauthorized` 이벤트 발행 → LoginModal.
- 에러 = `ApiError(status, code, message, errors[])`. 호출처에서 `code` 분기.
- 도메인 모듈 = `api/{domain}.ts`. 훅에서 호출.

> Backend `ResilientAiClient` 단일 진입점의 FE 대칭.

---

## 녹화 → 업로드 흐름

```
MediaRecorder (WebM)
  ├─ useMediaRecorder / useMediaStream / useAudioCapture
  ├─ IndexedDB 캐시 (lib/video-storage.ts, key=`{interviewId}-{questionSetId}`)
  └─ Backend → presigned PUT URL
       └─ useS3Upload — XHR PUT (지수백오프 3회, progress, beforeunload 가드)
            └─ S3 ObjectCreated → EventBridge → Lambda 분석 (FE 비관여)
```

- **Lambda / OpenAI / Claude 직접 호출 X** — 모든 외부 시스템 backend 경유.
- IndexedDB = 업로드 실패 / 새로고침 복원용.

---

## 상태

| Store | 용도 | persist |
|-------|------|---------|
| `auth-store` | LoginModal / redirect | X |
| `interview-store` | 인터뷰 phase | (확인 필요) |
| TanStack Query | 서버 데이터 (`AUTH_QUERY_KEY` 등) | staleTime 5m |

Cross-tab = `localStorage` storage 이벤트. BroadcastChannel 미사용.

---

## 환경 변수

| 키 | 의미 |
|----|------|
| `VITE_API_URL` | 백엔드 base (dev 빈 값 → vite proxy) |
| `VITE_APP_ENV` | `development` / `production` |
| `VITE_SITE_URL` | SEO canonical / og:url / sitemap |

`VITE_*` = 번들 노출 → **secret 절대 X**.

---

## 알림 / 에러

- Toast = sonner (`Toaster` App 루트).
- 401 = LoginModal (위).
- **전역 ErrorBoundary 없음** — 도입 시 본 문서 갱신.

---

## 시스템 경계

| 시스템 | FE 호출 |
|-------|---------|
| Backend (Spring) | `apiClient` REST + JWT 쿠키 |
| S3 | presigned PUT (`useS3Upload`) |
| Lambda | **X** (S3 → EventBridge 비동기) |
| OpenAI / Claude | **X** (backend 경유) |

---

## 진입 체크리스트

1. 라우트 가드 (Public / Protected / PasswordProtected) 결정.
2. API = `apiClient` + `api/{domain}.ts`.
3. 서버 데이터 = TanStack Query / 글로벌 = Zustand.
4. 녹화 / 업로드 = 위 흐름 준수.
5. 에러 = `ApiError.code` 분기, 메시지 = sonner.
