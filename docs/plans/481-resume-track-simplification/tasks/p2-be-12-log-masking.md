# Task 12 — LLM payload 로그 마스킹 정책 점검 + 추가

> **위치**: `tasks/p2-be-12-log-masking.md`
> **답하는 질문**: ResumeSkeleton 개인정보 LLM 로그 어떻게 마스킹?

---

## 목적

`ResumeSkeleton` payload (사용자 직무 / 프로젝트명 / 회사명 포함) 가 LLM 호출 logger INFO 레벨에 기록되지 않도록 마스킹 정책 점검 + 부재 시 추가. OWASP A09 (Security Logging Failures) 준수.

## 에이전트

- **구현**: `backend` — infra/ai/logging 마스킹 헬퍼 grep + 부재 시 추가 + INFO/DEBUG 레벨 분리 + 회귀 테스트
- **리뷰**: `code-reviewer-backend` — 보안 정책 / 로그 레벨 / 마스킹 적용 범위

## 변경 파일

- `backend/src/main/java/com/rehearse/api/infra/ai/logging/` — 마스킹 헬퍼 grep (위치 명 implement 단 정정)
- `backend/src/main/java/com/rehearse/api/infra/ai/logging/AiCallLogger.java` (또는 동등) — 마스킹 헬퍼 부재 시 추가
- `backend/src/main/java/com/rehearse/api/domain/question/service/ResumeTrackInitiator.java` — INFO 로그 = `resumeId` / `call_type` / `fileHash` 만, payload 본문 = DEBUG 레벨
- `backend/src/test/.../ResumeTrackInitiatorLoggingTest.java` — logback capture 로 INFO 로그 payload 본문 부재 검증

## 핵심 로직

```java
// AiCallLogger (After)
public class AiCallLogger {
    public void logCall(String callType, String resumeId, String fileHash, String prompt) {
        log.info("call_type={} resume_id={} file_hash={}", callType, resumeId, fileHash);
        log.debug("call_type={} prompt={}", callType, prompt);  // payload = DEBUG
    }
}

// ResumeTrackInitiator (After)
aiCallLogger.logCall("resume_question_generator", skeleton.resumeId(), skeleton.fileHash(), prompt);
```

## 의존
- 선행 Task: 05 (ResumeTrackInitiator 재작성)
- 외부: logback (운영 미적용 DEBUG 레벨)

## 테스트 케이스
- [ ] `ResumeTrackInitiatorLoggingTest` — INFO 로그 capture 시 `projects[].topic` / `projects[].description` 본문 부재
- [ ] DEBUG 로그 capture 시 payload 본문 포함 (개발 환경 디버깅 가능)
- [ ] `application-prod.yml` 로그 레벨 = INFO (DEBUG payload 운영 미적용)
- [ ] `application-dev.yml` 로그 레벨 = INFO 기본 (운영자 자율 DEBUG 전환)

## 완료 기준
- [ ] 마스킹 헬퍼 위치 확정 + 정책 적용
- [ ] logback capture 회귀 통과
- [ ] OWASP A09 보고 형식 = 부재 시 `.claude/rules/security.md` 형식 사용자 알림 (현재 마스킹 부재 발견 시)
- [ ] code-reviewer-backend 실행

## 커밋 메시지

```
chore(BE): LLM 호출 로그 마스킹 정책 - ResumeSkeleton payload DEBUG 레벨
```

## 비고

- `infra/ai/logging/` 디렉토리 grep 결과 마스킹 헬퍼 부재 시 = 보안 위반 보고 (`.claude/rules/security.md` 형식)
- 현재 INFO 로그가 payload 기록 중이면 = OWASP A09 위배 즉시 보고 + 사용자 결정
