# Plan 17: 개인정보 처리방침 페이지

> 상태: Completed (2026-04-14)
> 작성일: 2026-04-14

## Why

prod 런칭 전 개인정보 처리방침 페이지가 **서비스 내에 존재하지 않는다**. OAuth 로그인으로 Google/GitHub 이메일·프로필을 수집하고, 이력서 텍스트·면접 녹화 영상·STT 전사·비언어 분석 결과를 저장·처리하면서 처리방침 미공개는 다음 법적·운영적 리스크를 유발한다:

- 개인정보보호법 제30조(개인정보 처리방침 수립·공개) 미이행
- Google OAuth consent screen 심사 거절 사유 (Verification 필수 항목)
- 사용자 권리(열람·정정·삭제) 요청 창구 부재
- 제3자 처리 위탁(AWS, Anthropic, OpenAI, GCP) 고지 누락

footer에 `/privacy` 링크를 추가하고 공개 접근 가능한 정적 페이지를 배포한다.

## 생성/수정 파일

| 파일 | 작업 |
|---|---|
| `frontend/src/pages/privacy-policy-page.tsx` | 신규 — 개인정보 처리방침 본문 |
| `frontend/src/app.tsx` | `/privacy` 라우트 등록 (공개, 인증 불요) |
| `frontend/src/pages/home-page.tsx` | footer에 "개인정보 처리방침" 링크 추가 |

## 상세

### 본문 구성 (10개 섹션)

1. 총칙 + 시행일자 (2026-04-14)
2. 수집 항목: OAuth 이메일·이름·프로필 사진, 이력서 텍스트, 면접 녹화 WebM, STT 전사, 비언어 분석 결과, JWT 세션 쿠키
3. 수집 목적: AI 모의면접·피드백 생성, 서비스 개선 통계
4. 보관 기간:
   - 원본 영상: 30일 후 자동 삭제 (S3 Lifecycle)
   - 파생 데이터: 회원 탈퇴 시까지, 탈퇴 요청 시 30일 내 완전 삭제
5. 제3자 제공: 없음
6. 처리 위탁: AWS / Anthropic / OpenAI / Google Cloud — **모두 모델 학습 미수행 명시**
7. 이용자 권리: 열람·정정·삭제·처리정지 (`privacy@rehearse.co.kr` placeholder)
8. 쿠키: JWT `HttpOnly` `Secure` `SameSite=Lax` 1종, 추적 쿠키 없음
9. 안전성 조치: HTTPS, SSE-S3, OAuth, 최소 권한 IAM
10. 문의처 (이메일 placeholder, 실제 운영 이메일로 교체 필요 — 후속 작업)

### 베타 고지

본문 상단 amber 박스로 "본 서비스는 베타이며 예기치 못한 오류로 데이터 손실 가능. 중요 데이터는 별도 백업 권장" 문구.

### UI

- 최대 너비 제한된 중앙 본문(prose 스타일)
- 상단 헤더: Logo + "리허설" + BETA 배지 (plan-16 컴포넌트 재사용) + 뒤로가기 링크
- 목차 앵커 네비게이션 (스크롤 이동)
- Tailwind 기반, 기존 색/타이포 시스템 재사용

### 라우팅

`app.tsx`에서 wildcard `Navigate to="/"` 앞에 `<Route path="/privacy" element={<PrivacyPolicyPage />} />` 배치. `ProtectedRoute` 밖 → 로그인 없이 접근 가능.

### Footer 링크

home-page.tsx의 footer `© 2026 Rehearse. All rights reserved.` 옆/아래에 `<Link to="/privacy">개인정보 처리방침</Link>` 추가. 기존 스타일 유지.

## 담당 에이전트

- Implement: `frontend` (executor 위임 완료)

## 검증

- `cd frontend && npm run lint` 통과 ✅
- `cd frontend && npm run build` 통과 ✅
- `/privacy` 직접 접근 가능, 로그인 불요
- footer 링크 클릭 → 페이지 이동 + 뒤로가기 동작

## 후속

- `privacy@rehearse.co.kr` placeholder를 실제 운영 이메일로 교체 (런칭 전 필수)
- 이용약관(Terms of Service) 페이지는 별건
- Google OAuth consent screen publishing 심사 시 본 URL 제출
