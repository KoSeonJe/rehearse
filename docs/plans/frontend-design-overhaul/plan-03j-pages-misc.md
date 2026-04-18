# Plan 03j: 정보/정적 페이지 일괄 재정렬

> 상태: Draft
> 작성일: 2026-04-17

## Why

정보/정적 페이지는 변경 빈도가 낮고 복잡도가 낮아 일괄 처리 가능. 그러나 일관성 감사(Phase 5)에서 누락되지 않도록 이번 Phase에 명시적으로 포함.

## 대상 페이지

- `frontend/src/pages/about/**`
- `frontend/src/pages/faq/**`
- `frontend/src/pages/guide/**` (하위 페이지 포함)
- `frontend/src/pages/privacy-policy/**`
- `frontend/src/pages/admin-feedbacks/**`
- `frontend/src/pages/not-found/**`

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| 위 6개 페이지 디렉토리 | heading 계층, 본문 typography, 링크 스타일, empty/error 정렬 |

## 상세

### 공통 작업

- `prose` 느낌 본문 typography: Pretendard body 16~18px, line-height 1.6
- Heading scale: h1 48px Cal Sans / h2 32px / h3 24px (영문 제목 있을 때), 한글은 Pretendard 대응
- 링크: `--link` 토큰(#0099ff) + underline
- 리스트 들여쓰기/간격 정렬

### 페이지별 특이사항

- **guide/**: 단계별 가이드 — 카드 + 순서 있는 섹션 (3d Card 결과 활용)
- **admin-feedbacks**: 관리자 전용 — 데이터 테이블/리스트 정렬 (shadcn `Table` 필요 시 설치)
- **privacy-policy**: 법적 문구 — 가독성 최우선, 본문 max-width 제한
- **not-found**: 모노크롬 미니멀, 링크 1개만

## 담당 에이전트

- Implement: `frontend` + `designer`
- Review: `designer` — typography 스케일 일관성
- Review: `code-reviewer` — 링크 라우팅 회귀

## 검증

- `npm run lint/build/test` green
- 각 페이지 수동 순회
- 링크 클릭 → 정상 라우팅
- `progress.md` Task 3j → Completed

## 체크포인트

페이지별 스크린샷 묶음 보고 → 사용자 승인 후 Phase 4 진입.
