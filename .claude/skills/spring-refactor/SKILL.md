---
name: spring-refactor
description: "Java 21 + Spring Boot 3.x 코드의 종합 리팩토링 스킬. 코드 컨벤션, 객체지향(OOP/SOLID), 클린코드, DDD 전술 패턴을 기반으로 분석하고 수정 방향을 제시한다. 대상이 클 경우 자동으로 세분화하여 단계별 리팩토링 계획을 수립한다. 사용자가 '리팩토링', 'refactor', '클린코드', 'clean code', '코드 개선', '코드 정리', '코드 품질', 'code quality', '컨벤션', 'convention', '객체지향', 'OOP', 'SOLID', 'DDD', '도메인 설계' 등을 언급하면 반드시 이 스킬을 사용한다. 단순 버그 수정이나 기능 추가가 아닌, 코드 구조와 설계 품질을 개선하려는 모든 요청에 적극적으로 트리거한다."
triggers:
  - 리팩토링
  - refactor
  - 클린코드
  - clean code
  - 코드 개선
  - 코드 정리
  - 코드 품질
  - code quality
  - 컨벤션
  - convention
  - 객체지향
  - oop
  - solid
  - ddd
  - 도메인 설계
  - 안티패턴
  - anti-pattern
  - 코드 리뷰
  - 코드 스멜
  - code smell
argument-hint: "<target: class, package, or 'all'> [--analyze-only] [--scope=entity|service|controller|full] [--depth=shallow|deep]"
---

# Java Spring 종합 리팩토링 스킬

## Purpose

Java 21 + Spring Boot 3.x 백엔드 코드를 **코드 컨벤션 + 클린코드 + OOP/SOLID + DDD 전술 패턴** 4개 축으로 분석하고, 구체적인 수정 방향을 제시한다.

리팩토링은 "코드를 예쁘게 만드는 것"이 아니라 **변경 비용을 낮추고, 버그 발생 가능성을 줄이며, 의도를 명확히 드러내는 것**이다. 이 스킬은 그 목적에 집중한다.

## Workflow

### Phase 0: 범위 파악 및 세분화

대상 코드의 규모를 먼저 측정한다. 이 단계가 중요한 이유는 한 번에 너무 많은 코드를 리팩토링하면 리뷰가 불가능하고 롤백도 어렵기 때문이다.

**규모 판단 기준:**

| 규모 | 기준 | 접근 방식 |
|-----|------|----------|
| Small | 클래스 1~3개 | 즉시 분석 → 수정 방향 제시 |
| Medium | 패키지 1개 (4~10개 클래스) | 레이어별로 나눠서 분석 (Entity → Service → Controller) |
| Large | 패키지 2개+ 또는 10개+ 클래스 | **세분화 필수**: 도메인 경계 기준으로 작업 단위 분리 후 우선순위 정하기 |

**Large 규모 세분화 절차:**

1. 도메인 패키지별로 그룹핑
2. 각 그룹의 코드 스멜 밀도를 간단히 스캔 (HIGH 안티패턴 개수)
3. 의존성 방향 파악 (어디서부터 고쳐야 파급이 적은지)
4. 작업 단위 분리 → 우선순위 매기기 → 단계별 계획 출력

```
## 세분화 계획

### 1단계: interview 도메인 (HIGH 5건, 의존성 최하위)
- Interview.java: @Setter 제거, 상태 전이 도메인 메서드화
- InterviewService → 책임 분리

### 2단계: questionset 도메인 (HIGH 3건, interview에 의존)
- QuestionSet.java: 컬렉션 캡슐화
- ...

### 3단계: global 인프라 (LOW 2건)
- ...
```

### Phase 1: 4축 분석

대상 코드를 읽고 아래 4개 축의 체크리스트를 실행한다. `--analyze-only` 옵션 시 여기서 종료.

각 축의 상세 체크리스트와 패턴은 `references/` 디렉토리에 있다:
- **코드 컨벤션**: `references/conventions.md` — 네이밍, 포맷, Spring 관용구
- **클린코드**: `references/clean-code.md` — 함수 크기, 추상화 수준, 가독성
- **OOP/SOLID**: `references/oop-solid.md` — 캡슐화, 책임 분리, 다형성
- **DDD 전술 패턴**: `references/ddd-patterns.md` — Aggregate, VO, Domain Event, Rich Model

분석 시 아래 순서로 진행:
1. Entity/도메인 모델 읽기 → DDD + OOP 축 분석
2. Service 읽기 → 클린코드 + OOP 축 분석
3. Controller 읽기 → 컨벤션 + 클린코드 축 분석
4. DTO/VO 읽기 → 컨벤션 축 분석
5. Repository 읽기 → 컨벤션 축 분석

### Phase 2: 분석 리포트 출력

```markdown
## 리팩토링 분석 리포트: {대상}

### 요약
- 분석 대상: {클래스/패키지명}
- 총 이슈: HIGH {n}건, MEDIUM {n}건, LOW {n}건
- 핵심 문제: {1줄 요약}

### 발견된 이슈 (심각도순)

#### [HIGH] 이슈명
- **축**: {코드 컨벤션 | 클린코드 | OOP/SOLID | DDD}
- **위치**: {ClassName.java:methodName()}
- **현재 문제**: {구체적으로 어떤 코드가 왜 문제인지}
- **수정 방향**: {어떻게 바꿔야 하는지, 코드 예시 포함}
- **영향 범위**: {이 변경이 다른 어떤 파일에 영향을 주는지}

#### [MEDIUM] ...
#### [LOW] ...

### 수정 우선순위 (권장 순서)
1. {가장 먼저 해야 할 것} — 이유: {왜 먼저인지}
2. ...

### 예상 변경 파일 목록
- {파일명} ({추가|수정|분리})
```

### Phase 3: 웹서치 (필요시에만)

아래 상황에서만 웹서치를 수행한다. 핵심 원칙은 이미 `references/`에 내장되어 있으므로 대부분의 경우 불필요하다.

**웹서치 트리거 조건:**
- Spring Boot / JPA의 **특정 버전 이슈**나 마이그레이션 관련 질문
- 새로운 라이브러리나 프레임워크 패턴에 대한 최신 베스트 프랙티스
- 내장 레퍼런스로 판단이 어려운 **엣지 케이스**
- 사용자가 명시적으로 "최신 트렌드", "요즘 방식" 등을 요청

웹서치 시:
1. 공식 문서 우선 (Spring docs, Baeldung, Java 공식)
2. 검색 결과의 날짜를 확인하여 오래된 정보 필터링
3. 검색 결과를 분석 리포트에 출처와 함께 반영

### Phase 4: 수정 방향 제시

분석 결과를 바탕으로 **구체적인 수정 방향**을 제시한다. 직접 코드를 수정하지 않고, 사용자가 판단할 수 있는 충분한 정보를 제공한다.

각 이슈에 대해:

```markdown
### 수정 방향: {이슈명}

**Before (현재 코드):**
```java
// 실제 프로젝트의 현재 코드 인용
```

**After (권장 방향):**
```java
// 리팩토링 후 예상 코드
```

**변경 이유:**
- {왜 이렇게 바꿔야 하는지 원칙 기반 설명}

**주의사항:**
- {변경 시 깨질 수 있는 것, 함께 수정해야 할 것}
```

사용자가 수정 방향을 확인하고 승인하면, 그때 실제 코드 수정을 진행한다.

---

## 안티패턴 탐지 통합 체크리스트

아래는 4개 축에서 가장 자주 발견되는 이슈를 통합한 빠른 스캔 체크리스트다.
상세 설명과 코드 예시는 각 `references/*.md` 파일에 있다.

| # | 안티패턴 | 축 | 심각도 | 탐지 시그널 |
|---|---------|---|--------|-----------|
| 1 | Transaction Script | DDD | HIGH | Service에 비즈니스 로직 집중, Entity에 행위 없음 |
| 2 | Feature Envy | OOP | HIGH | Service가 entity.getX() 체인으로 계산 후 setX() |
| 3 | God Class/Method | 클린코드 | HIGH | 15+ 메서드 Service, 30+ 줄 메서드 |
| 4 | Public Setter | DDD/OOP | HIGH | @Setter 또는 public setXxx() |
| 5 | Anemic Domain Model | DDD | HIGH | Entity = @Getter + @Setter + @NoArgsConstructor만 |
| 6 | Primitive Obsession | DDD | MEDIUM | String/Long이 도메인 개념 표현 |
| 7 | Exposed Mutable Collection | OOP | MEDIUM | getList()가 mutable List 반환 |
| 8 | 추상화 수준 불일치 | 클린코드 | MEDIUM | 한 메서드에 고수준/저수준 로직 혼재 |
| 9 | 네이밍 불일치 | 컨벤션 | MEDIUM | 동사/명사 혼용, 약어 불일치 |
| 10 | 매직 넘버/스트링 | 클린코드 | LOW | 하드코딩된 숫자/문자열 |
| 11 | Cross-Aggregate 객체 참조 | DDD | MEDIUM | ID가 아닌 Entity 직접 참조 |
| 12 | 조건문 남용 (if/switch) | OOP | MEDIUM | 타입별 분기 → 다형성 미적용 |
| 13 | 불필요한 주석 | 클린코드 | LOW | 코드가 설명하는 내용을 주석으로 반복 |
| 14 | Controller 비즈니스 로직 | 컨벤션 | HIGH | Controller에서 직접 비즈니스 판단 |
| 15 | DTO-Entity 직접 변환 | 컨벤션 | MEDIUM | Controller/Service에서 수동 매핑 산재 |

---

## Options

- `--analyze-only`: Phase 1~2만 실행 (분석 리포트만 출력, 수정 방향 미제시)
- `--scope=entity|service|controller|full`: 분석 범위 제한 (기본: full)
- `--depth=shallow|deep`: shallow는 HIGH만, deep은 전체 (기본: deep)
- `--web-search`: 강제로 웹서치 포함

## Examples

```
# 특정 Entity 분석만
/spring-refactor Interview --analyze-only

# 패키지 전체 리팩토링 분석
/spring-refactor com.rehearse.api.domain.interview

# 전체 프로젝트 분석 (세분화 자동 적용)
/spring-refactor all --depth=shallow

# Service 레이어만 집중 분석
/spring-refactor InterviewService --scope=service

# 최신 베스트 프랙티스 포함
/spring-refactor QuestionSet --web-search
```

## Notes

- **점진적 리팩토링**: 한 번에 모든 것을 고치지 않는다. 리포트의 우선순위를 따른다.
- **테스트 확인**: 리팩토링 전 기존 테스트 통과 여부를 확인한다. 테스트가 없으면 먼저 작성을 권장한다.
- **JPA 호환성**: `record`는 `@Entity`로 직접 사용 불가. VO로만 사용하고 `AttributeConverter` 또는 `@Embeddable`로 매핑.
- **한국어 출력**: 분석 리포트와 수정 방향 모두 한국어로 작성.
- **수정은 승인 후**: 분석 리포트 → 사용자 확인 → 수정 방향 제시 → 사용자 승인 → 실제 코드 수정 순서를 지킨다.
