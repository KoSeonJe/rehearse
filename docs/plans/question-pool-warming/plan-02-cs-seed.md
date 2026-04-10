# Plan 02: CS 기본 시드 데이터 작성

> 상태: Draft
> 작성일: 2026-04-10

## Why

CS_FUNDAMENTAL은 position-agnostic 타입(캐시키: `{Level}:CS_FUNDAMENTAL`)으로, 1벌의 데이터가 전 포지션을 커버한다. 면접에서 가장 기본이 되는 질문군이므로 최우선 시딩 대상이다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/main/resources/db/seed/cs-fundamental-junior.sql` | 기존 60개 통합 + 60개 추가 = 120개 (자료구조30, 운영체제30, 네트워크30, 데이터베이스30) |
| `backend/src/main/resources/db/seed/cs-fundamental-mid.sql` | 120개 신규 |
| `backend/src/main/resources/db/seed/cs-fundamental-senior.sql` | 120개 신규 |

기존 `db/seed/junior-cs-fundamental.sql`의 60개를 통합하고 네이밍을 일관되게 변경.

## 상세

### 캐시키 매핑

| 파일 | cache_key | category 분포 |
|------|-----------|--------------|
| cs-fundamental-junior.sql | `JUNIOR:CS_FUNDAMENTAL` | 자료구조×30, 운영체제×30, 네트워크×30, 데이터베이스×30 |
| cs-fundamental-mid.sql | `MID:CS_FUNDAMENTAL` | 동일 분포 |
| cs-fundamental-senior.sql | `SENIOR:CS_FUNDAMENTAL` | 동일 분포 |

### 질문 소스 (웹 리서치 결과 기반)

#### 자료구조 (빈출 순위)

**JUNIOR**: Array vs LinkedList, Stack/Queue 기본, HashMap 동작 원리, HashTable vs HashMap, BST, Heap, Graph 표현, Hash Collision, 정렬 알고리즘 비교, 시간복잡도 개념
**MID**: AVL/Red-Black Tree, B-Tree/B+Tree, Trie, 세그먼트 트리, 우선순위 큐 구현, DFS/BFS 비교, 위상 정렬, 최소 신장 트리, 최단 경로 알고리즘, Union-Find
**SENIOR**: LRU 캐시 설계, Concurrent 자료구조, Skip List, Bloom Filter, 분산 해시 테이블(DHT), 실시간 시스템 자료구조 선택, 대용량 데이터 정렬 전략

#### 운영체제 (빈출 순위)

**JUNIOR**: 프로세스 vs 스레드, 프로세스 상태, PCB, 컨텍스트 스위칭, 멀티프로세스 vs 멀티스레드, 메모리 계층 구조, 사용자 모드 vs 커널 모드, 인터럽트
**MID**: 데드락 4조건 + 해결 방법, 세마포어 vs 뮤텍스, 페이징 vs 세그먼테이션, 가상 메모리, 페이지 교체 알고리즘(LRU/FIFO/LFU), 스케줄링 알고리즘(RR/SJF/MLFQ), Thrashing, IPC, Race Condition
**SENIOR**: Microkernel vs Monolithic, TLB, NUMA, 캐시 일관성 프로토콜, 데드락 탐지 알고리즘(Banker's), 메모리 매핑 파일, Copy-on-Write, 리눅스 커널 스케줄러(CFS)

#### 네트워크 (빈출 순위)

**JUNIOR**: OSI 7계층, TCP vs UDP, HTTP vs HTTPS, DNS 동작, IP 주소 체계, 포트 개념, HTTP 메서드/상태코드, 쿠키 vs 세션, ARP
**MID**: TCP 3-way/4-way Handshake, 흐름 제어/혼잡 제어, URL 입력부터 화면 표시까지, REST 설계 원칙, HTTP/1.1 vs 2.0 vs 3.0, JWT 구조, SSL/TLS Handshake, CORS, 로드 밸런싱, Blocking/Non-blocking I/O
**SENIOR**: QUIC 프로토콜, Zero Trust Architecture, DDoS 방어, CDN 캐싱 전략, TCP Fast Open, WebSocket vs SSE, gRPC vs REST, 네트워크 파티션 처리

#### 데이터베이스 (빈출 순위)

**JUNIOR**: RDBMS vs NoSQL, Primary Key vs Foreign Key, 기본 SQL(SELECT/INSERT/UPDATE/DELETE), JOIN 종류, 관계(1:1, 1:N, N:N), NULL 처리, GROUP BY/HAVING
**MID**: 정규화(1NF~BCNF) + 이상 현상, 트랜잭션 ACID, 격리 수준 4단계, 인덱스 원리(B+Tree), 실행 계획(EXPLAIN), 쿼리 최적화, Locking(공유/배타), Replication, Sharding, 비정규화
**SENIOR**: CAP 이론, BASE 이론, 분산 트랜잭션(2PC/Saga), MVCC, Consistent Hashing, 쿼리 캐싱 전략, NoSQL 선택 기준, Elasticsearch 역인덱스, 시계열 DB, 데이터 웨어하우스

### INSERT 형식 (Plan 06 이후 스키마 기준)

```sql
INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('{cache_key}', '{질문}', '{카테고리}',
 '{모범 답변 3-5문장}',
 'MODEL_ANSWER', TRUE, NOW());
```

> Plan 06에서 `question_order`, `evaluation_criteria`, `follow_up_strategy`, `quality_score` 4개 컬럼이 삭제되므로 포함하지 않음.
> `INSERT IGNORE`로 중복 실행 시 에러 방지 (content+cache_key 기준).

## 담당 에이전트

- Implement: `backend` — SQL 파일 생성 (웹 리서치 기반 실제 빈출 질문 + 모범답변)
- Review: `code-reviewer` — 데이터 품질, 중복 검사, SQL 문법

## 검증

- 각 SQL 파일을 H2/MySQL에 실행하여 INSERT 성공 확인
- cache_key별 COUNT 확인: 각 120개
- category별 균등 분포 확인: 각 30개
- 기존 `junior-cs-fundamental.sql` 60개와 중복 없는지 확인
- `progress.md` 상태 업데이트 (Task 2 → Completed)
