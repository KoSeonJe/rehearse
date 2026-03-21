# 프롬프트 재설계 — 요구사항 정의

> **상태**: Draft
> **작성일**: 2026-03-21
> **배경 문서**: `background/prompt-redesign.md` (v2), `background/prompt-optimized.md` (v3), `background/prompt-test-guide.md`

---

## 1. Why

### 1.1 현재 문제

| 문제 | 현재 상태 | 영향 |
|------|----------|------|
| 단일 페르소나 | "시니어 개발자 면접관" 하나로 모든 직무/스택 처리 | 직무별 전문성 차별화 불가 |
| 토큰 낭비 (~28%) | 12개 유형 가이드 전부 포함, 3개 레벨 가이드 전부 포함, CS 블록 항상 포함 | 불필요한 비용 + 프롬프트 집중도 저하 |
| 확장 불가 | 새 스택 추가 시 프롬프트 전체 수정 필요 | 유지보수 비용 증가 |
| Lambda 범용 프롬프트 | 언어 분석이 직무/스택 무관 범용 | 키워드 감지, 오용 지적 불가 |

### 1.2 목표

1. **Position × TechStack 2차원 페르소나** — 직무별 Base Profile + 스택별 Overlay 머지
2. **토큰 ~38% 절감** — 7가지 최적화 전략 적용 (조건부 블록, 페르소나 깊이 차등, 스키마/루브릭 압축)
3. **YAML 기반 확장 구조** — 새 스택 추가 시 overlay YAML + TechStack enum 값 추가로 확장 (FE 상수, Lambda dict도 동기 필요)
4. **Lambda 프롬프트 직무 인식** — 언어 분석에 스택별 키워드 사전 주입

### 1.3 성공 지표

| 지표 | 기대값 | 측정 방법 |
|------|-------|----------|
| 직무 특화 키워드 비율 | B > A by 20%+ | A/B 테스트 evaluate_results.py |
| 질문 깊이 점수 | B > A by 10%+ | 심화 패턴 카운트 |
| System Prompt 토큰 | ~38% 감소 | ClaudeApiClient 토큰 로그 |
| 기존 데이터 호환 | 100% | techStack=null 폴백 테스트 |

---

## 2. 설계 판단

| # | 판단 | 결정 | 근거 |
|---|------|------|------|
| D1 | Overlay 범위 | Position별 기본 스택 5개만 우선 구현 (JAVA_SPRING, REACT_TS, AWS_K8S, SPARK_AIRFLOW, REACT_SPRING) | 핵심 아키텍처 검증 + 기존 데이터 호환. 나머지 overlay는 추후 YAML만 추가 |
| D2 | A/B 테스트 시점 | 구현 완료 후 (Phase 6) | 실제 API 호출 기반 품질 비교 필요 |
| D3 | Lambda 데이터 전달 | Internal API 응답에 interview context 추가 | EventBridge 스키마 변경 불필요, 비침습적 |
| D4 | TechStack 선택 UX | optional (스킵 시 기본 스택 자동 적용) | UX 복잡도 최소화, 기존 사용자 영향 없음 |
| D5 | 프로필 저장 형식 | YAML 파일 (resources/prompts/) | 코드 재배포 없이 프롬프트 수정 가능, 가독성 우수 |
| D6 | 페르소나 깊이 전략 | FULL(질문생성), MEDIUM(후속질문), MINIMAL(언어분석) | 프롬프트 역할별 필요 컨텍스트 차등, v3 전략 1 |
| D7 | AiClient 인터페이스 | Parameter Object(Request DTO)로 래핑 | 현재 7~8개 파라미터 나열은 fat interface. 향후 필드 추가 시 시그니처 불변 |
| D8 | keyword_usage 저장 여부 | MVP에서는 저장하지 않음. verbal comment에 자연어로 반영 | DB 스키마 확장은 과도. 추후 피드백 뷰어 고도화 시 별도 Phase로 진행 |

---

## 3. 제약 조건

### 3.1 프로젝트 규칙 (CLAUDE.md)
- BE/FE 별도 PR 필수
- 커밋/PR 한국어
- TypeScript strict mode, `any` 금지
- CI 통과 필수 (Frontend CI: lint+build, Backend CI: test)

### 3.2 기술 제약
- 기존 데이터 (techStack=null) 하위 호환 필수 — `getEffectiveTechStack()` 폴백
- Lambda는 Python 3.12 + OpenAI API (GPT-4o)
- BE 프롬프트는 Claude API (claude-sonnet-4-20250514)
- Spring Boot 3.4: `@MockitoBean` 사용 (not `@MockBean`)
- DB 마이그레이션: Flyway V9 (현재 최신 V8)

### 3.3 축약 금지 항목 (v3 문서 명시)
- 기술 키워드 사전 — 모델 인식 정확도에 직접 영향
- 심화 방향 (followUpDepth) — 후속 질문 다양성 결정
- 선택된 면접 유형 가이드 — 질문 전문성 핵심
- JSON 필드 이름 — 파싱 호환성

---

## 4. PR 전략

| PR | 브랜치 | Phase | 내용 | 의존 |
|----|--------|-------|------|------|
| PR-1 | `feat/prompt-persona-foundation` | 1 | [BE] TechStack, DB V9, YAML, PersonaResolver | 없음 |
| PR-2 | `feat/prompt-builder-refactor` | 2 | [BE] 프롬프트 빌더 + 토큰 최적화 + AiClient 확장 | PR-1 |
| PR-3 | `feat/internal-api-interview-context` | 3 | [BE] Internal API context 추가 | PR-1 |
| PR-4 | `feat/lambda-verbal-prompt` | 4 | [Lambda] verbal/vision 프롬프트 최적화 | PR-3 |
| PR-5 | `feat/fe-tech-stack-selection` | 5 | [FE] TechStack 선택 위저드 | PR-2 |
| PR-6 | `feat/prompt-redesign-tests` | 6 | [BE] 통합 테스트 + A/B 결과 | PR-2, PR-4 |

---

## 5. 병렬 실행 구간

```
Phase 1 ──┬──> Phase 2 ──────> Phase 5 (FE)
           │                       │
           ├──> Phase 3 [parallel] ──> Phase 4 (Lambda)
           │                       │
           └───────────────────────┴──> Phase 6 (검증)
```

- **Phase 2 + Phase 3**: Phase 1 완료 후 동시 진행 가능
- **Phase 4 + Phase 5**: 각각 Phase 3, Phase 2 완료 후 동시 진행 가능
- Phase 1 내부: Task 1-1(enum/DB/entity) + Task 1-2(YAML/resolver) 병렬 가능

---

## 6. 위험 및 대응

| 위험 | 영향 | 대응 |
|------|------|------|
| YAML 로딩 실패 → 서버 기동 불가 | 높음 | PersonaResolver에 fallback + 로그 경고 |
| techStack=null 기존 데이터 호환 | 높음 | `getEffectiveTechStack()` 자동 폴백 + null 테스트 필수 |
| FE 위저드 스텝 증가 → UX 복잡도 | 중간 | TechStack optional + "기본 추천" 안내 |
| 토큰 절감 실제치 < 예상(38%) | 중간 | ClaudeApiClient 토큰 로그 모니터링 (L141) |
| A/B 테스트 5회는 통계적 유의성 부족 | 중간 | 경향 확인용, 확신 필요 시 30회+ 추가 |
| 이벤트/서비스 체인에 techStack 전파 누락 | 높음 | `QuestionGenerationRequestedEvent`, `InterviewService`, `QuestionGenerationService` 3곳 동시 수정 필수 |
| keyword_usage 필드가 BE 저장 체인에 없음 | 중간 | MVP에서는 저장하지 않기로 결정 (D8). 프롬프트에서 keyword_usage 요청 제거, comment에 자연어 반영 |
| position-techStack 호환성 미검증 시 잘못된 조합 저장 | 높음 | BE Service 레이어에서 `TechStack.isAllowedFor(position)` 검증 필수 |
