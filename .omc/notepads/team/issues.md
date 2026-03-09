# 버그 / 이슈 트래킹

> 발견된 버그와 이슈를 기록합니다.
> 상태: 🔴 Open | 🟡 In Progress | 🟢 Resolved | ⚪ Won't Fix

---

## 이슈 템플릿

```
### [ISS-번호] 이슈 제목
- **상태**: 🔴 Open
- **발견일**: YYYY-MM-DD
- **발견자**: [에이전트/사용자]
- **심각도**: Critical / High / Medium / Low
- **설명**: 문제 상세
- **재현 방법**: 재현 단계
- **담당**: [에이전트]
- **해결**: (해결 후 기록)
```

---

## 이슈 목록

### [ISS-001] ClaudeApiClient에 타임아웃 설정 누락
- **상태**: 🟢 Resolved
- **발견일**: 2026-03-10
- **발견자**: QA
- **심각도**: High
- **설명**: RestClient 생성 시 connect/read 타임아웃이 미설정. 스펙에서 30초 타임아웃을 요구하나 현재 타임아웃 없음. Claude API 무응답 시 스레드 무한 대기 가능.
- **재현 방법**: Claude API가 응답을 지연시키는 상황에서 POST /api/v1/interviews 호출 시 스레드 무한 대기
- **위치**: `backend/src/main/java/com/devlens/api/infra/ai/ClaudeApiClient.java:37-39`
- **담당**: Backend
- **해결**: connect timeout 5초, read timeout 30초 설정 (ClientHttpRequestFactorySettings)

---

### [ISS-002] API 에러 응답 본문이 프론트엔드에서 파싱 불가
- **상태**: 🟢 Resolved
- **발견일**: 2026-03-10
- **발견자**: QA
- **심각도**: High
- **설명**: api-client.ts에서 HTTP 에러 시 응답 본문을 읽지 않고 statusText만 Error에 담음. interview-setup-page.tsx에서 이 문자열을 JSON.parse 시도하나 항상 실패. 서버의 필드별 validation 에러가 사용자에게 전달되지 않음.
- **재현 방법**: position을 101자 이상 입력 후 제출 -> "오류가 발생했습니다" 일반 메시지만 표시, 필드별 에러 미표시
- **위치**: `frontend/src/lib/api-client.ts:27`, `frontend/src/pages/interview-setup-page.tsx:46-48`
- **담당**: Frontend
- **해결**: ApiError 클래스 도입, 응답 본문 JSON 파싱 후 필드별 에러 표시

---

### [ISS-003] application-prod.yml에 Claude API 설정 누락
- **상태**: 🟢 Resolved
- **발견일**: 2026-03-10
- **발견자**: QA
- **심각도**: High
- **설명**: application-dev.yml에는 claude.api-key, claude.model 설정이 있으나 application-prod.yml에는 누락. 운영 프로필 기동 시 @Value 바인딩 실패로 BeanCreationException 발생, 서버 기동 불가.
- **재현 방법**: spring.profiles.active=prod로 서버 실행 시 기동 실패
- **위치**: `backend/src/main/resources/application-prod.yml`
- **담당**: Backend
- **해결**: 환경변수 바인딩 추가 (CLAUDE_API_KEY, CLAUDE_MODEL)

---

### [ISS-004] Interview 조회 시 추가 쿼리 발생 (1+1)
- **상태**: 🟢 Resolved
- **발견일**: 2026-03-10
- **발견자**: QA
- **심각도**: Medium
- **설명**: Interview.questions가 LAZY 로딩이고 fetch join 쿼리가 없어 조회 시 Interview SELECT + Questions SELECT 2회 쿼리 발생. 향후 목록 조회 추가 시 N+1 문제로 확대 가능.
- **재현 방법**: GET /api/v1/interviews/{id} 호출 후 SQL 로그 확인
- **위치**: `backend/src/main/java/com/devlens/api/domain/interview/repository/InterviewRepository.java`
- **담당**: Backend
- **해결**: findByIdWithQuestions JOIN FETCH 쿼리 추가, Service에서 사용하도록 변경

---

### [ISS-005] InterviewService 테스트 커버리지 부족
- **상태**: 🔴 Open
- **발견일**: 2026-03-10
- **발견자**: QA
- **심각도**: Medium
- **설명**: createInterview Claude API 실패 케이스, updateStatus 404 케이스 미테스트. InterviewController 통합 테스트(MockMvc) 전무하여 @Valid 검증, HTTP 상태코드 반환 등 미검증.
- **재현 방법**: 해당 없음 (테스트 누락)
- **위치**: `backend/src/test/java/com/devlens/api/domain/interview/service/InterviewServiceTest.java`
- **담당**: Backend
- **해결**: (미해결)
