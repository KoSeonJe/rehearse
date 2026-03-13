# Rehearse 공통 컨벤션

> 유지보수와 협업을 위한 공통 규칙. 모든 에이전트가 준수합니다.
> FE 컨벤션: `frontend/CONVENTIONS.md` | BE 컨벤션: `backend/CONVENTIONS.md`
> FE 코딩 가이드: `frontend/CODING_GUIDE.md` | BE 코딩 가이드: `backend/CODING_GUIDE.md`

---

## 브랜치 전략

```
main (프로덕션) ← develop (통합) ← feat/{기능}-be, feat/{기능}-fe
```

| 브랜치 | 용도 | 머지 방식 |
|--------|------|-----------|
| `main` | 프로덕션 릴리스 | Merge Commit (마일스톤 단위) |
| `develop` | 통합 개발 (default) | — |
| `feat/{기능}-be` | Backend 기능 개발 | Squash Merge → develop |
| `feat/{기능}-fe` | Frontend 기능 개발 | Squash Merge → develop |
| `fix/{버그명}` | 버그 수정 | Squash Merge → develop |

### PR 규칙

- **제목**: `[BE|FE] {type}: {한국어 요약}`
- **크기**: 파일 10개 이하, 300줄 이하 권장
- **Backend/Frontend 혼합 금지** — 별도 PR로 분리
- Backend PR 먼저 머지 → Frontend PR이 연동
- 기능 브랜치는 항상 `develop`에서 분기

---

## 커밋 규칙

### 커밋 단위
| 단위 | 예시 |
|------|------|
| **스펙/문서** | 기능 스펙, 팀 문서 업데이트 |
| **Backend 기능** | API + 엔티티 + 서비스 + 테스트 (하나의 기능 단위) |
| **Frontend 기능** | 컴포넌트 + 페이지 + 훅 (하나의 기능 단위) |
| **리팩토링** | 동작 변경 없는 구조 개선 |
| **버그 수정** | 이슈 단위 |
| **설정/인프라** | CI/CD, Docker, 빌드 설정 |

### 커밋 메시지
```
{type}: {한국어 요약}

- 상세 변경 내용 bullet
- 상세 변경 내용 bullet

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
```

| type | 용도 |
|------|------|
| `feat` | 새 기능 |
| `fix` | 버그 수정 |
| `refactor` | 동작 변경 없는 코드 개선 |
| `docs` | 문서, 스펙, 팀 문서 |
| `chore` | 설정, 의존성, 빌드 |
| `test` | 테스트 추가/수정 |
| `style` | 포맷팅, 세미콜론 등 (동작 무관) |

---

## 공통 금지 사항

- 프론트엔드에서 직접 Claude API 호출 (API Key 노출)
- 시크릿 하드코딩 (환경변수 사용)
- MVP DON'T 범위 기능 구현
- 불필요한 라이브러리 추가 (브라우저 네이티브 API 우선)
- 하드코딩된 색상/간격 값 (디자인 토큰 사용)
