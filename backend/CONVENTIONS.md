# Backend 컨벤션

> 유지보수와 협업을 위한 백엔드 코딩 규칙.
> 공통 규칙(브랜치, 커밋, PR)은 `docs/conventions.md` 참조.
> 실전 구현 가이드는 `CODING_GUIDE.md` 참조.

---

## 패키지 구조 (도메인 기반)

```
com.rehearse.api/
├── domain/{feature}/       # 도메인별 분리
│   ├── controller/         # @RestController (HTTP 요청/응답)
│   ├── service/            # 비즈니스 로직, @Transactional
│   ├── repository/         # Spring Data JPA 인터페이스
│   ├── entity/             # JPA 엔티티, Enum
│   ├── dto/                # 요청/응답 DTO
│   └── exception/          # 도메인별 ErrorCode enum
├── global/                 # 전역 공통
│   ├── config/             # Spring Configuration
│   ├── exception/          # 글로벌 예외 핸들러
│   └── common/             # ApiResponse, ErrorResponse
└── infra/                  # 외부 서비스 연동
    └── ai/                 # Claude API 클라이언트
```

## 네이밍

| 대상 | 규칙 | 예시 |
|------|------|------|
| 클래스 | PascalCase | `InterviewController` |
| 메서드 | camelCase | `createInterview()` |
| 상수 | UPPER_SNAKE_CASE | `MAX_QUESTIONS` |
| DB 테이블 | snake_case, 단수형 | `interview`, `interview_question` |
| DB 컬럼 | snake_case | `interview_type`, `created_at` |
| 패키지 | 소문자 | `com.rehearse.api.domain.interview` |

## 계층 규칙

| 계층 | 책임 | 금지 사항 |
|------|------|----------|
| Controller | HTTP 처리, 입력 검증 (`@Valid`) | 비즈니스 로직, 직접 Repository 호출 |
| Service | 비즈니스 로직, 트랜잭션 | HTTP 관련 코드, 직접 DB 쿼리 |
| Repository | 데이터 접근 | 비즈니스 로직 |

## DTO 패턴

```java
// Request: @Getter + @NoArgsConstructor + Jakarta Validation
@Getter
@NoArgsConstructor
public class CreateInterviewRequest {
    @NotNull(message = "직무를 선택해주세요.")
    private Position position;
    // ...
}

// Response: @Getter + @Builder + static from() 팩토리 메서드
```

> 상세 패턴은 `CODING_GUIDE.md` 섹션 6 참조

## 에러 처리

- `BusinessException` + `ErrorCode` 인터페이스로 도메인별 예외 생성
- 에러 코드 체계: `{DOMAIN}_{3자리}` (예: `INTERVIEW_001`, `AI_001`)
- `GlobalExceptionHandler`에서 통일 에러 응답

## 응답 형식

```java
// 성공: ApiResponse.ok(data)
// 실패: ErrorResponse (GlobalExceptionHandler가 자동 생성)
```

## 테스트

- 서비스 단위 테스트 필수 (핵심 비즈니스 로직)
- `@MockitoBean`으로 외부 의존성 격리 (Spring Boot 3.4+, `@MockBean` deprecated)
- 테스트 메서드명: `메서드명_조건_기대결과` (예: `createInterview_success`)
