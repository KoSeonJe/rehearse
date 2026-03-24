# Plan 05: WhisperService timeout 설정

> 상태: Draft
> 작성일: 2026-03-23

## Why

`WhisperService`에서 `new RestTemplate()`을 timeout 없이 생성하고 있어, OpenAI 장애 시 무한 대기가 발생한다. VT 환경에서는 서버가 죽지는 않지만, VT가 무한히 쌓이면서 메모리가 증가한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/main/java/.../infra/ai/WhisperService.java` | RestTemplate에 connectTimeout/readTimeout 설정 |

## 상세

### 변경 전 (line 32)

```java
this.restTemplate = new RestTemplate();
```

### 변경 후

```java
var factory = new SimpleClientHttpRequestFactory();
factory.setConnectTimeout(Duration.ofSeconds(5));
factory.setReadTimeout(Duration.ofSeconds(60));
this.restTemplate = new RestTemplate(factory);
```

- connectTimeout: 5초 — 연결 자체가 안 되면 빠르게 실패
- readTimeout: 60초 — Whisper 오디오 처리 시간 고려 (긴 녹음 파일 대응)

## 담당 에이전트

- Implement: `backend` — timeout 설정
- Review: `code-reviewer` — timeout 값 적절성

## 검증

- `WhisperService` 생성자에서 timeout 설정 확인
- `./gradlew build` 성공
- `progress.md` 상태 업데이트 (Task 5 → Completed)
