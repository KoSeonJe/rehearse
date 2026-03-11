# Feature Specification: 면접 Setup 페이지 UX 리디자인

> **문서 ID**: PLAN-010
> **작성일**: 2026-03-10
> **상태**: Completed
> **우선순위**: P1 (Should-have)

---

## Why

현재 Setup 페이지의 문제점:

1. **직무가 자유 텍스트** — 뭘 입력해야 할지 모호, 직무별 맞춤 면접 유형 제공 불가
2. **면접 유형 고정 3개** — 직무마다 면접 주제가 다른데 동일한 CS/시스템설계/Behavioral만 제공
3. **복수 선택 불가** — 실제 면접은 혼합 출제가 일반적
4. **이력서 입력 없음** — AI가 개인화된 질문 생성 불가
5. **레벨 가치 불명확** — 어떤 차이를 만드는지 안내 없음

### 목표

- 직무 카드 선택 → 직무별 맞춤 면접 유형 자동 매핑
- 면접 유형 복수 선택 → 실제 면접처럼 혼합 출제
- 이력서 PDF 업로드 → 맞춤 질문 생성 (1회성, 저장 안함)
- 멀티스텝 위저드 → 한 화면에 다 넣지 않고 단계별 집중

### 근거

- 실제 기술 면접은 2~3개 유형이 혼합 출제됨
- 이력서 기반 질문이 면접 현실성을 크게 높임
- 직무 선택 → 면접 유형 자동 매핑으로 사용자 인지 부하 감소

---

## 데이터 모델 변경

### Position enum (신규)
`BACKEND`, `FRONTEND`, `DEVOPS`, `DATA_ENGINEER`, `FULLSTACK`, `OTHER`

### InterviewType enum (3개 → 11개)
- 공통: `CS_FUNDAMENTAL`, `BEHAVIORAL`, `RESUME_BASED`
- 백엔드: `JAVA_SPRING`, `SYSTEM_DESIGN`
- 프론트: `REACT_COMPONENT`, `BROWSER_PERFORMANCE`
- 데브옵스: `INFRA_CICD`, `CLOUD`
- 데이터: `DATA_PIPELINE`, `SQL_MODELING`

### CS 세부 주제
`DATA_STRUCTURE`, `OS`, `NETWORK`, `DATABASE` — CS_FUNDAMENTAL 선택 시 서브옵션

### API 변경
```
POST /api/v1/interviews (multipart/form-data)
- request: JSON (@RequestPart) — position, positionDetail, level, interviewTypes[], csSubTopics[]
- resumeFile: PDF (@RequestPart, optional, max 10MB)
```

---

## BE 변경

| 파일 | 변경 내용 |
|------|-----------|
| `build.gradle.kts` | PDFBox 3.0.4 추가 |
| `Position.java` | 신규 enum |
| `InterviewType.java` | 3→11개 확장 |
| `Interview.java` | position String→enum, interviewTypes 복수(쉼표 구분), positionDetail 추가 |
| `CreateInterviewRequest.java` | Position enum, List<InterviewType>, csSubTopics, positionDetail |
| `InterviewResponse.java` | 응답 필드 대응 |
| `InterviewController.java` | multipart/form-data 수신 |
| `InterviewService.java` | PDF 텍스트 추출 + AI 호출 시그니처 변경 |
| `PdfTextExtractor.java` | 신규 — PDFBox 텍스트 추출 (max 5000자) |
| `ClaudePromptBuilder.java` | 11개 유형별 가이드, CS 세부 주제, 이력서 반영, 질문 수 규칙 |
| `AiClient.java` | generateQuestions 6파라미터 |
| `ClaudeApiClient.java` | 새 시그니처 |
| `MockAiClient.java` | 새 시그니처 |

---

## FE 변경

| 파일 | 변경 내용 |
|------|-----------|
| `interview.ts` | Position, CsSubTopic 타입, 매핑 상수, LEVEL hint 추가 |
| `use-interviews.ts` | FormData multipart 전송 |
| `interview-setup-page.tsx` | 4단계 위저드 (직무→레벨→유형→이력서) |
| `interview-ready-page.tsx` | interviewTypes 복수 표시 |

---

## 검증

- [x] BE: `./gradlew clean test` 통과
- [x] FE: `npx tsc --noEmit` 통과
- [ ] 통합: setup → ready → conduct → feedback → report 전체 플로우
- [ ] 모바일/데스크톱 반응형 확인
