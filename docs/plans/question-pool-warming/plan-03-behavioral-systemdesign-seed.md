# Plan 03: Behavioral + System Design 시드 데이터 작성

> 상태: Draft
> 작성일: 2026-04-10

## Why

BEHAVIORAL과 SYSTEM_DESIGN은 position-agnostic 타입으로, CS_FUNDAMENTAL과 마찬가지로 1벌의 데이터가 전 포지션을 커버한다. 인성 면접과 설계 면접은 실무 역량을 평가하는 핵심 영역이므로 시딩이 필요하다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/main/resources/db/seed/behavioral-junior.sql` | 기존 20개 통합 + 10개 추가 = 30개 |
| `backend/src/main/resources/db/seed/behavioral-mid.sql` | 30개 신규 |
| `backend/src/main/resources/db/seed/behavioral-senior.sql` | 30개 신규 |
| `backend/src/main/resources/db/seed/system-design-junior.sql` | 30개 신규 |
| `backend/src/main/resources/db/seed/system-design-mid.sql` | 30개 신규 |
| `backend/src/main/resources/db/seed/system-design-senior.sql` | 30개 신규 |

기존 `db/seed/junior-behavioral.sql`의 20개를 통합.

## 상세

### Behavioral 캐시키 및 카테고리

| 파일 | cache_key | category 분포 |
|------|-----------|--------------|
| behavioral-junior.sql | `JUNIOR:BEHAVIORAL` | 협업×6, 문제해결×6, 성장×6, 리더십×6, 커뮤니케이션×6 |
| behavioral-mid.sql | `MID:BEHAVIORAL` | 동일 분포 |
| behavioral-senior.sql | `SENIOR:BEHAVIORAL` | 동일 분포 |

### Behavioral 질문 소스 (웹 리서치 기반)

#### 협업
- JUNIOR: 팀 의견 충돌 해결, 코드 리뷰 피드백 수용, 새 팀원과 협업, 타 직군 협업 어려움, 동료 비판 대응
- MID: 팀 간 의견 중재, FE-BE 기술 차이 해결, 협업 프로세스 개선, 코드 충돌 최소화 전략
- SENIOR: 대규모 프로젝트 조율, 팀 문화 구축, 글로벌 팀 협업, 조직 변화 리드

#### 문제 해결
- JUNIOR: 예상치 못한 기술 문제 대응, 어려운 버그 수정, 일정 초과 시 대처
- MID: 프로덕션 장애 대응, 기술적 트레이드오프 결정, 성능 문제 진단/해결, 실패한 솔루션 전환
- SENIOR: 복잡한 아키텍처 문제 분해, 기술 부채 vs 신기능 균형, 스코프 변경 관리

#### 성장
- JUNIOR: 새 언어/프레임워크 학습, 약점 인식 개선, 프로젝트 회고, 학습 장벽 극복
- MID: 경력 전환점 프로젝트, 멘토 관계, 실패 경험 교훈, 지식 부족 극복
- SENIOR: 경력 전환 의사결정, 멘토링 경험, 기술 깊이-리더십 균형, 산업 변화 적응

#### 리더십
- JUNIOR: 책임감 발휘, 팀 내 역할, 지식 공유, 프로세스 개선 제안
- MID: 관행 개선, 저성과 팀원 대처, 팀원 성장 지원, 문제 주도적 해결
- SENIOR: 기술 비전 수립, 위임 경험, 어려운 의사결정, 조직 변화 리드

#### 커뮤니케이션
- JUNIOR: 비개발자에게 기술 설명, 코드 리뷰 이의 제기, 아이디어 제안
- MID: 이해관계자 간 요구사항 전달, 경영진 기술 설득, 상위 보고, 피드백 대화
- SENIOR: 조직 전체 기술 방향 설명, 경영진 설득, 비기술팀 소통 구축, 문서화 문화 도입

### Behavioral INSERT 형식 (Plan 06 이후 스키마 기준)

```sql
INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('{cache_key}', '{질문}', '{카테고리}',
 '[STAR 가이드]\nSituation: ...\nTask: ...\nAction: ...\nResult: ...\n\n핵심: ...',
 'GUIDE', TRUE, NOW());
```

**referenceType = 'GUIDE'** (모범답변이 아닌 답변 방향 가이드)
> `INSERT IGNORE`로 중복 실행 시 에러 방지.

### System Design 캐시키 및 카테고리

| 파일 | cache_key | category 분포 |
|------|-----------|--------------|
| system-design-junior.sql | `JUNIOR:SYSTEM_DESIGN` | 캐싱×6, 메시지큐×6, 마이크로서비스×6, 로드밸런싱×6, API설계×6 |
| system-design-mid.sql | `MID:SYSTEM_DESIGN` | 동일 분포 |
| system-design-senior.sql | `SENIOR:SYSTEM_DESIGN` | 동일 분포 |

### System Design 질문 소스

- JUNIOR: 캐싱 기본, TTL, 메시지큐 개념, 마이크로서비스 정의, 동기/비동기 통신, 로드밸런싱 알고리즘, API 게이트웨이
- MID: Write-Through vs Write-Behind, Redis vs Memcached, Kafka vs RabbitMQ, Saga 패턴, Circuit Breaker, 캐시 일관성, 캐시 관통, 서비스 디스커버리
- SENIOR: 캐싱 계층화, 분산 일관성 모델(Strong/Eventual), 네트워크 파티션 대응, Dead Letter Queue, 분산 트레이싱, 대규모 시스템 설계(URL Shortener, 채팅 시스템 등)

## 담당 에이전트

- Implement: `backend` — SQL 파일 생성
- Review: `code-reviewer` — 데이터 품질, STAR 가이드 완성도

## 검증

- SQL 실행 성공 확인
- Behavioral: cache_key별 30개, category별 6개 균등 분포
- System Design: cache_key별 30개, category별 6개 균등 분포
- 기존 `junior-behavioral.sql` 20개와 중복 없는지 확인
- `progress.md` 상태 업데이트 (Task 3 → Completed)
