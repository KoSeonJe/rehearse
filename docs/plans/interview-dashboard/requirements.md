# 면접 대시보드 — 요구사항 정의

> 상태: Draft
> 작성일: 2026-03-31

## Why

1. **Why?** — 현재 로그인한 사용자도 랜딩페이지만 보게 되어, 자신의 면접 이력을 확인할 수 없다. 과거 면접을 다시 보려면 URL을 직접 기억해야 하는 상황.
2. **Goal** — 로그인 사용자에게 면접 대시보드를 메인 화면으로 제공하여, 면접 이력 조회 / 새 면접 시작 / 미완료 면접 정리를 한 곳에서 할 수 있게 한다.
3. **Evidence** — MVP 파이프라인(녹화→분석→피드백)은 완성되었으나, 면접 목록 조회 기능이 없어 반복 사용이 어려운 상태. Sprint 0 완료 후 자연스러운 다음 단계.
4. **Trade-offs**
   - 통계는 현재 데이터로 바로 뽑을 수 있는 것만 (총/완료/이번주). 점수 기반 성장률, 기술 스택 태깅 등은 스키마 변경이 필요하므로 제외.
   - 소요시간은 실제 측정값이 아닌 설정 시간(`durationMinutes`) 표시. `startedAt` 필드 추가는 대시보드 스코프 밖.
   - 필터링/정렬은 데이터가 충분히 쌓이기 전까지 YAGNI. 최신순 고정.
   - COMPLETED 면접 삭제는 S3/Lambda/피드백 연쇄 정리가 필요하므로 별도 기능으로.

## 목표

- 로그인 사용자: `/` → `/dashboard` 리다이렉트
- 비로그인 사용자: `/` → 기존 랜딩페이지
- 대시보드에서 면접 이력 확인, 새 면접 시작, 미완료 면접 삭제 가능
- 피드백 페이지 상단에 면접 정보 헤더 추가

## 아키텍처 / 설계

### 라우팅 변경

```
/ (HomePage)
├── 비로그인 → 기존 랜딩페이지
└── 로그인 → /dashboard 리다이렉트

/dashboard (DashboardPage) [ProtectedRoute]
├── 통계 카드 (총 면접 / 완료 / 이번 주)
├── 새 면접 시작 버튼 → /interview/setup
└── 면접 카드 목록 (최신순)
    ├── COMPLETED → /interview/{publicId}/feedback
    ├── READY → /interview/{id}/ready (이어하기)
    └── IN_PROGRESS → 클릭 비활성 + 삭제 가능
```

### 백엔드 API

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/v1/interviews` | 내 면접 목록 (페이지네이션 구조) |
| GET | `/api/v1/interviews/stats` | 통계 (총/완료/이번주) |
| DELETE | `/api/v1/interviews/{id}` | 면접 삭제 (소유권 검증, COMPLETED 제외) |

### 면접 카드 표시 정보

- 포지션 (`position`, `positionDetail`)
- 카테고리 태그 (`interviewTypes`, `csSubTopics`)
- 날짜 (`createdAt`)
- 상태 배지 (`status`)
- 질문 수 (COUNT 서브쿼리)
- 설정 시간 (`durationMinutes`)

### 피드백 페이지 변경

- 상단에 면접 정보 헤더 추가 (포지션, 날짜, 설정 시간, 카테고리)
- "닫기" → "대시보드로 돌아가기"

## Scope

- **In**: 대시보드 페이지, 면접 목록/통계/삭제 API, 라우팅 분기, 피드백 페이지 헤더
- **Out**: 점수 기반 통계, 기술 스택 태깅, 필터링/정렬, COMPLETED 삭제, 실제 소요시간 측정

## 디자인 가이드

- 상세 디자인 명세: `plan-02-dashboard-page.md` 참조
- 기존 디자인 시스템 준수 (Minimalism & Swiss Style)
- 토큰: `tailwind.config.js` 정의 값 사용 (accent=#6366F1, surface=#F8FAFC 등)
- 폰트: Pretendard (본문) + JetBrains Mono (숫자)
- 아이콘: Lucide React (SVG), 이모지 사용 금지

## 제약조건 / 환경

- 면접 목록 API는 `userId` 기반 소유권 검증 필수
- 삭제 API는 COMPLETED 상태 면접 삭제 불가 (S3/피드백 연쇄 삭제 미구현)
- 질문 수는 목록 조회 시 COUNT 서브쿼리로 해결 (N+1 방지)
- 프론트 페이지네이션은 당장 구현하지 않되, 응답 구조는 페이지네이션 형태로 설계
