# Handoff — {Issue번호}-{slug}

> **수명**: 단명 (plan 종료 시 제거)
> **작성 시점**: 세션 종료 / 컨텍스트 30-40% 잔여
> **다음 세션**: plan 폴더 진입 시 **이 파일 먼저 읽음**

---

## 현재 상태

- 진행: `implement-be.md` Phase X 까지 완료
- 브랜치: `feat/xxx-yyy` (마지막 commit `abc1234`)
- 관련 PR: #42 (draft) / #43 (merged) / #44 (review)
- 빌드: 통과 / 실패 (사유)
- 테스트: 통과 / 실패 (사유)

## 다음 세션 시작점

구체 파일 / Phase / 명령. "이거 먼저" 1순위.

- 다음 작업: `implement-be.md` Phase X — {구체 작업명}
- 참조: `tech-spec.md#data-model`
- 첫 명령: `./gradlew test --tests XxxTest`
- 예상 변경 파일: `backend/src/main/java/.../Xxx.java`

## 미해결 질문 / Blocker

사용자 결정 필요 항목. 없으면 "없음".

- (있을 시) {질문} — 옵션 A / B / C, 추천: A. 사유:

## 컨텍스트 메모

다음 세션이 놓치면 안 되는 것. 함정 / 결정 / 환경.

- 함정: {예: X 컴포넌트 wrapper 가 children 두 번 렌더 — useMemo 필수}
- 결정: {예: A 안 채택, B 안 폐기. 사유: tech-spec.md#trade-offs}
- 환경: {예: Testcontainers MySQL 8 사용. local docker 필요}
- 임시 우회: {예: TODO(seonje, 2026-05-15) — 캐시 TTL 동적화}

## 참고 명령

자주 쓰는 명령 모음 (다음 세션 즉시 사용).

```bash
cd backend
./gradlew test --tests XxxTest
docker compose -f docker-compose.local.yml up -d
```

---

업데이트: YYYY-MM-DD (세션 종료 / 컨텍스트 잔여 X%)
