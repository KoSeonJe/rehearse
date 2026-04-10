-- JUNIOR SYSTEM DESIGN 질문 Pool 시딩 (30문항: 캐싱6, 메시지큐6, 마이크로서비스6, 로드밸런싱6, API설계6)
-- referenceType: MODEL_ANSWER (상세 기술 모범답변)
-- V20 이후 스키마 기준 (question_order, evaluation_criteria, follow_up_strategy, quality_score 컬럼 없음)

-- ============================================================
-- 캐싱 (6문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:SYSTEM_DESIGN', '캐싱(Caching)이란 무엇이고, 왜 사용하나요?', '캐싱',
 '캐싱은 자주 사용되는 데이터를 빠른 저장소(메모리 등)에 임시로 저장하여 반복 요청 시 원본 데이터 소스에 접근하지 않고 빠르게 응답하는 기법입니다.\n\n**사용 이유:**\n1. **성능 향상**: DB나 외부 API 호출 대신 메모리에서 응답 → 응답 시간 수십~수백ms 단축\n2. **부하 감소**: 원본 데이터 소스(DB)에 대한 요청 수 감소\n3. **비용 절감**: DB 쿼리 비용, 외부 API 호출 비용 절감\n4. **가용성 향상**: 원본 소스 장애 시 캐시에서 응답 가능\n\n**대표 사용 예시:**\n- 사용자 세션 정보\n- 자주 조회되는 상품 목록\n- 계산 비용이 큰 집계 데이터\n- 정적 콘텐츠(이미지, HTML)\n\n**주의사항:** 캐시는 데이터 일관성 문제를 유발할 수 있으므로, 변경이 잦은 데이터보다 읽기 빈도가 높고 변경이 적은 데이터에 적합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:SYSTEM_DESIGN', 'TTL(Time To Live)이란 무엇이고, 캐시에서 어떻게 활용되나요?', '캐싱',
 'TTL(Time To Live)은 캐시에 저장된 데이터가 유효한 시간을 의미합니다. TTL이 지나면 해당 캐시 항목은 자동으로 만료(expire)됩니다.\n\n**캐시에서의 TTL 활용:**\n```\n캐시 저장 시: SET user:123 {data} EX 3600  // 1시간 후 자동 만료\n```\n\n**TTL 설정 기준:**\n- **짧은 TTL (수초~수분)**: 실시간성이 중요한 데이터 (재고 수량, 가격 등)\n- **긴 TTL (수시간~수일)**: 변경 빈도 낮은 데이터 (카테고리 목록, 공지사항 등)\n- **TTL 없음**: 변경 시 직접 캐시를 갱신/삭제 (Cache Invalidation 방식)\n\n**TTL과 관련된 문제:**\n1. **TTL 만료 폭풍 (Thundering Herd)**: 다수의 캐시가 동시에 만료되어 DB에 급격한 부하 발생 → 만료 시간에 랜덤 지터(jitter) 추가로 해결\n2. **스테일 데이터**: TTL이 너무 길면 오래된 데이터를 반환할 수 있음\n\n**적정 TTL은 데이터 변경 빈도와 일관성 요구 수준을 고려하여 결정합니다.**',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:SYSTEM_DESIGN', 'Cache Hit와 Cache Miss의 차이는 무엇인가요?', '캐싱',
 '**Cache Hit**: 요청한 데이터가 캐시에 존재하는 경우. 캐시에서 즉시 반환하여 빠른 응답 가능.\n**Cache Miss**: 요청한 데이터가 캐시에 없는 경우. 원본 소스(DB)에서 데이터를 가져와야 함.\n\n**Cache Miss 발생 유형:**\n1. **Cold Miss (Compulsory Miss)**: 캐시가 처음 시작되어 데이터가 전혀 없을 때\n2. **Capacity Miss**: 캐시 공간이 가득 차서 기존 항목을 제거했을 때\n3. **Invalidation Miss**: TTL 만료 또는 명시적 삭제로 캐시가 무효화됐을 때\n\n**Cache Hit Ratio (캐시 적중률):**\n```\nHit Ratio = Cache Hits / (Cache Hits + Cache Misses) × 100%\n```\n일반적으로 80% 이상이면 효과적인 캐시 전략으로 봄.\n\n**Cache Miss 시 처리 흐름 (Cache-Aside 패턴):**\n```\n1. 캐시 조회\n2. Miss → DB 조회\n3. DB 결과를 캐시에 저장\n4. 클라이언트에 반환\n```\n\n**Cache Miss가 잦다면:** 캐시 전략 재검토(TTL 조정, 캐시 크기 증가, 대상 데이터 재선정) 필요.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:SYSTEM_DESIGN', 'LRU(Least Recently Used) 캐시 교체 정책이란 무엇인가요?', '캐싱',
 'LRU(Least Recently Used)는 캐시가 가득 찼을 때 가장 오랫동안 사용되지 않은 항목을 먼저 제거하는 캐시 교체 정책입니다.\n\n**동작 원리:**\n```\n캐시 크기: 3\n순서대로 접근: A, B, C, D\n\n[A] → 캐시: [A]\n[B] → 캐시: [A, B]\n[C] → 캐시: [A, B, C]\n[D] → 캐시 가득 참 → A(가장 오래 미사용) 제거 → 캐시: [B, C, D]\n[B] → Hit → 캐시: [C, D, B] (B가 최근 사용으로 이동)\n```\n\n**구현 방법:** HashMa + 이중 연결 리스트(O(1) 조회 및 이동)\n\n**다른 교체 정책과 비교:**\n| 정책 | 설명 | 특징 |\n|------|------|------|\n| LRU | 최근 미사용 제거 | 일반적으로 가장 효과적 |\n| LFU | 사용 빈도 낮은 것 제거 | 빈도 카운팅 오버헤드 |\n| FIFO | 먼저 들어온 것 제거 | 구현 단순, 성능 낮음 |\n| Random | 무작위 제거 | 예측 불가 |\n\n**Redis에서의 LRU:**\n```\nmaxmemory-policy allkeys-lru  # 모든 키에 LRU 적용\nmaxmemory-policy volatile-lru # TTL 설정된 키에만 LRU 적용\n```\n\nLRU는 "최근에 사용된 데이터는 곧 다시 사용될 것"이라는 시간적 지역성(Temporal Locality) 원리에 기반합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:SYSTEM_DESIGN', 'Redis와 Memcached의 기본적인 차이점은 무엇인가요?', '캐싱',
 'Redis와 Memcached는 모두 인메모리 캐시이지만 다음과 같은 차이가 있습니다.\n\n**기본 비교:**\n| 항목 | Redis | Memcached |\n|------|-------|----------|\n| 데이터 타입 | String, Hash, List, Set, ZSet 등 다양 | String만 지원 |\n| 영속성(Persistence) | RDB/AOF로 디스크 저장 가능 | 메모리만, 재시작 시 데이터 손실 |\n| 복제/클러스터 | 기본 지원 (Sentinel, Cluster) | 클라이언트 측 샤딩 필요 |\n| 트랜잭션 | MULTI/EXEC 지원 | 미지원 |\n| Pub/Sub | 지원 | 미지원 |\n| 성능 | 단일 스레드 기반, 매우 빠름 | 멀티스레드, 단순 읽기/쓰기에서 빠름 |\n\n**언제 선택할까:**\n- **Redis**: 다양한 자료구조, 영속성, 세션 관리, 분산 락, Pub/Sub이 필요할 때\n- **Memcached**: 단순 문자열 캐싱, 매우 빠른 처리량, 멀티스레드 활용이 목적일 때\n\n**실무에서는 Redis가 기능이 풍부해 대부분의 경우 Redis를 선택합니다.**\n\n**Redis 사용 예시 (Spring Boot):**\n```java\n@Cacheable(value = "users", key = "#id")\npublic User findById(Long id) {\n    return userRepository.findById(id).orElseThrow();\n}\n```',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:SYSTEM_DESIGN', 'Cache-Aside 패턴이란 무엇인가요?', '캐싱',
 'Cache-Aside(Lazy Loading) 패턴은 애플리케이션이 캐시와 데이터 저장소 간 데이터 동기화를 직접 관리하는 가장 일반적인 캐싱 패턴입니다.\n\n**동작 흐름:**\n```\n[읽기]\n1. 캐시 조회\n2. Hit → 캐시 데이터 반환\n3. Miss → DB 조회 → 캐시 저장 → 반환\n\n[쓰기]\n1. DB 업데이트\n2. 캐시 삭제 (또는 업데이트)\n```\n\n**장점:**\n- 실제 요청된 데이터만 캐시에 저장 → 메모리 효율적\n- 캐시 장애 시 DB로 폴백 가능\n- 구현이 단순하고 이해하기 쉬움\n\n**단점:**\n- 처음 요청 시 항상 Cache Miss 발생 (Cold Start)\n- 캐시와 DB 간 일관성 관리 필요\n- DB 업데이트 후 캐시 삭제를 빠뜨리면 스테일 데이터 반환\n\n**다른 패턴과 비교:**\n| 패턴 | 특징 |\n|------|------|\n| Cache-Aside | 앱이 직접 관리, 가장 범용적 |\n| Write-Through | 쓰기 시 캐시와 DB 동시 업데이트 |\n| Write-Behind | 쓰기 시 캐시만 업데이트, 비동기로 DB 반영 |\n| Read-Through | 캐시가 DB 조회를 대신 처리 |\n\n**스프링에서의 Cache-Aside 구현:**\n```java\n// @Cacheable = Cache-Aside 읽기\n// @CacheEvict = 캐시 무효화\n@Cacheable("products")\npublic Product getProduct(Long id) { ... }\n\n@CacheEvict("products")\npublic void updateProduct(Long id, ...) { ... }\n```',
 'MODEL_ANSWER', TRUE, NOW());

-- ============================================================
-- 메시지큐 (6문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:SYSTEM_DESIGN', '메시지 큐(Message Queue)란 무엇이고, 왜 사용하나요?', '메시지큐',
 '메시지 큐는 생산자(Producer)가 보낸 메시지를 소비자(Consumer)가 처리할 때까지 저장하는 비동기 통신 미들웨어입니다.\n\n**핵심 개념:**\n- **Producer**: 메시지를 생성하여 큐에 전송\n- **Queue/Topic**: 메시지를 저장하는 버퍼\n- **Consumer**: 큐에서 메시지를 꺼내 처리\n\n**사용하는 이유:**\n1. **비동기 처리**: 요청자가 응답을 기다리지 않고 즉시 다음 작업 진행\n2. **디커플링(Decoupling)**: 생산자와 소비자가 서로 알 필요 없음 → 독립적 개발/배포 가능\n3. **부하 완충**: 급격한 트래픽 증가 시 큐가 버퍼 역할 → 소비자가 처리 가능한 속도로 소화\n4. **내구성(Durability)**: 소비자 장애 시에도 메시지 보존 → 재처리 가능\n\n**대표 사용 예시:**\n- 이메일/SMS 발송\n- 이미지 변환/동영상 인코딩\n- 주문 처리 파이프라인\n- 로그 수집\n\n**대표 솔루션:** Kafka, RabbitMQ, AWS SQS, Redis Streams\n\n**메시지 큐 없을 때 문제:**\n```\n요청 → [서비스] → [이메일 서버 호출] → 이메일 서버 지연 → 사용자 응답 지연\n```\n**메시지 큐 있을 때:**\n```\n요청 → [서비스] → 큐에 저장 → 즉시 응답\n                  ↓\n           [이메일 워커] → 비동기 처리\n```',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:SYSTEM_DESIGN', '동기(Synchronous) 통신과 비동기(Asynchronous) 통신의 차이는 무엇인가요?', '메시지큐',
 '**동기 통신:**\n요청자가 응답이 올 때까지 대기하는 방식. 요청과 응답이 하나의 흐름으로 연결됩니다.\n\n```\n클라이언트 → [요청] → 서버\n클라이언트 ← [응답] ← 서버 (처리 완료 후)\n// 클라이언트는 응답이 올 때까지 블로킹\n```\n\n**비동기 통신:**\n요청자가 응답을 기다리지 않고 다음 작업을 진행. 결과는 나중에 콜백, 이벤트, 폴링 등으로 수신.\n\n```\n클라이언트 → [요청] → 큐/브로커\n클라이언트는 즉시 다음 작업 진행\n나중에: 서버 → [완료 이벤트] → 클라이언트\n```\n\n**비교표:**\n| 항목 | 동기 | 비동기 |\n|------|------|--------|\n| 응답 대기 | 필요 | 불필요 |\n| 결합도 | 강함 (상대 시스템 필요) | 느슨함 |\n| 처리 속도 | 직렬 | 병렬 가능 |\n| 구현 복잡도 | 낮음 | 높음 |\n| 장애 전파 | 쉬움 | 격리됨 |\n\n**언제 동기를 쓸까:**\n- 즉각적인 결과가 필요한 경우 (로그인, 결제 승인 확인)\n\n**언제 비동기를 쓸까:**\n- 처리 시간이 긴 작업 (파일 변환, 이메일 발송)\n- 결과를 즉시 알 필요 없는 경우\n- 대용량 이벤트 처리',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:SYSTEM_DESIGN', 'Kafka와 RabbitMQ의 기본적인 차이점을 설명해주세요.', '메시지큐',
 '**Kafka:**\n- **분산 스트리밍 플랫폼** — 대용량 이벤트 스트림 처리에 최적화\n- 메시지를 **로그 형태로 디스크에 영구 저장** (보존 기간 설정 가능)\n- Consumer가 **직접 오프셋을 관리** → 재처리 가능\n- 매우 높은 처리량(초당 수백만 메시지 가능)\n- Pull 방식: Consumer가 직접 메시지를 가져감\n\n**RabbitMQ:**\n- **전통적인 메시지 브로커** — 작업 큐, 라우팅에 최적화\n- 소비된 메시지는 **큐에서 삭제**\n- 복잡한 **라우팅 규칙** 지원 (Direct, Topic, Fanout, Headers Exchange)\n- Push 방식: 브로커가 Consumer에게 메시지 전달\n- 확인응답(ACK) 기반 메시지 처리 보장\n\n**비교 요약:**\n| 항목 | Kafka | RabbitMQ |\n|------|-------|----------|\n| 목적 | 스트리밍, 이벤트 소싱 | 작업 큐, 메시징 |\n| 메시지 보존 | 영구 (설정 기간) | 소비 후 삭제 |\n| 처리량 | 매우 높음 | 보통 |\n| 재처리 | 쉬움 | 어려움 |\n| 라우팅 | 단순 (토픽 파티션) | 복잡한 라우팅 |\n| 학습 곡선 | 높음 | 낮음 |\n\n**선택 기준:**\n- 로그, 이벤트 소싱, 대용량 스트림 → **Kafka**\n- 작업 큐, 복잡한 라우팅, 빠른 도입 → **RabbitMQ**',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:SYSTEM_DESIGN', '메시지 큐에서 메시지 유실을 방지하는 방법은 무엇인가요?', '메시지큐',
 '메시지 유실은 큐 시스템의 가장 중요한 문제 중 하나입니다. 다음 방법들로 방지할 수 있습니다.\n\n**1. 메시지 영속성 (Durability)**\n- RabbitMQ: 큐를 `durable=true`, 메시지를 `persistent` 모드로 설정\n- Kafka: 메시지를 디스크에 영구 저장 (기본 동작)\n\n**2. 확인 응답 (Acknowledgment / ACK)**\n```\nConsumer가 메시지 처리 완료 후 ACK 전송\n→ 브로커는 ACK 받기 전까지 메시지 보관\n→ ACK 없으면 다른 Consumer에게 재전달\n```\n\n**3. Kafka에서의 at-least-once 설정:**\n```properties\nacks=all                    # 모든 복제본에 저장 후 응답\nenable.auto.commit=false    # 수동 커밋으로 처리 완료 후 오프셋 커밋\nretries=3                   # 실패 시 재시도\n```\n\n**4. Dead Letter Queue (DLQ)**\n- 처리 실패한 메시지를 별도 큐에 보관 → 수동 재처리\n\n**5. 멱등성 (Idempotency) 보장**\n- 같은 메시지가 중복 처리되어도 결과가 동일하도록 Consumer 설계\n- 메시지 ID로 중복 처리 체크\n\n**at-least-once vs exactly-once:**\n| 보장 수준 | 설명 | 비고 |\n|---------|------|------|\n| at-most-once | 최대 1번 (유실 가능) | 빠름 |\n| at-least-once | 최소 1번 (중복 가능) | 일반적 선택 |\n| exactly-once | 정확히 1번 | 복잡, 느림 |',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:SYSTEM_DESIGN', '메시지 큐에서 Consumer가 처리할 수 없는 메시지는 어떻게 처리하나요?', '메시지큐',
 '처리할 수 없는 메시지(독소 메시지, Poison Message)를 처리하지 않으면 큐가 막히거나 Consumer가 무한 재시도에 빠질 수 있습니다.\n\n**Dead Letter Queue (DLQ) 패턴:**\n```\n[메인 큐] → Consumer 처리 실패\n         → 재시도 N회 후에도 실패\n         → [Dead Letter Queue] 이동\n         → 모니터링/알림/수동 검토\n```\n\n**DLQ 설정 예시 (RabbitMQ):**\n```\n큐 설정:\n- x-dead-letter-exchange: dlx.exchange\n- x-message-ttl: 60000 (ms)\n- x-max-length: 1000\n```\n\n**재시도 전략:**\n1. **즉시 재시도**: 일시적 오류에 적합\n2. **지수 백오프 (Exponential Backoff)**: 1초 → 2초 → 4초 → 8초...\n3. **고정 간격 재시도**: 일정 간격으로 재시도\n\n**Kafka에서의 재처리:**\n```\n실패 시 오프셋을 커밋하지 않음 → 자동 재처리\n또는 별도 retry-topic으로 이동 → 지연 재처리\n```\n\n**DLQ 운영 시 중요사항:**\n- DLQ 메시지에 대한 알림 설정 필수\n- 원인 파악 후 재처리 또는 폐기 결정\n- DLQ가 쌓이는 것 자체가 시스템 이상 신호',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:SYSTEM_DESIGN', '메시지 큐의 Consumer 수를 늘리면 어떤 효과가 있나요?', '메시지큐',
 '**Consumer 수 증가 효과:**\n\n**1. 처리 처리량(Throughput) 증가**\n```\nConsumer 1개: 초당 100건 처리\nConsumer 3개: 초당 약 300건 처리 (선형 확장)\n```\n\n**2. 처리 지연(Latency) 감소**\n- 큐에 메시지가 쌓이지 않아 대기 시간 단축\n\n**Kafka에서의 Consumer 수와 파티션:**\n- Consumer 수는 **파티션 수를 초과할 수 없음**\n- 파티션 3개 → 최대 3개의 Consumer가 병렬 처리\n- Consumer > 파티션이면 초과 Consumer는 유휴 상태\n\n```\n파티션: [P0] [P1] [P2]\nConsumer: [C0→P0] [C1→P1] [C2→P2]\n\n// Consumer 4개 추가 시\n[C0→P0] [C1→P1] [C2→P2] [C3→유휴]\n```\n\n**Consumer 수 결정 시 고려사항:**\n1. 메시지 처리 속도 vs 생산 속도 (처리량 목표)\n2. 메시지 순서 보장 필요 여부 (파티션 내에서만 순서 보장)\n3. Consumer 리소스 비용 (CPU, 메모리)\n4. DB 연결 풀 한계 (Consumer가 DB를 사용한다면)\n\n**Consumer Group:**\n- Kafka에서 같은 Consumer Group 내 Consumer들은 파티션을 나눠서 처리\n- 다른 Consumer Group은 동일 토픽을 독립적으로 소비 가능',
 'MODEL_ANSWER', TRUE, NOW());

-- ============================================================
-- 마이크로서비스 (6문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:SYSTEM_DESIGN', '마이크로서비스 아키텍처란 무엇이고, 모놀리식과 어떻게 다른가요?', '마이크로서비스',
 '**모놀리식 아키텍처:**\n모든 기능이 하나의 코드베이스/배포 단위로 구성된 전통적 방식.\n\n```\n[단일 애플리케이션]\n├── 사용자 모듈\n├── 주문 모듈\n├── 결제 모듈\n└── 알림 모듈\n→ 하나의 JAR/WAR로 배포\n```\n\n**마이크로서비스 아키텍처:**\n각 기능을 독립적인 서비스로 분리하여 개발/배포/운영하는 방식.\n\n```\n[사용자 서비스] [주문 서비스] [결제 서비스] [알림 서비스]\n     ↓               ↓              ↓              ↓\n  독립 DB        독립 DB        독립 DB        독립 DB\n```\n\n**비교:**\n| 항목 | 모놀리식 | 마이크로서비스 |\n|------|---------|---------------|\n| 배포 | 전체 재배포 | 서비스별 독립 배포 |\n| 확장성 | 전체 확장 | 필요한 서비스만 확장 |\n| 개발 속도 | 초기 빠름 | 팀 자율성으로 빠름 |\n| 복잡도 | 코드 복잡 | 운영 복잡 |\n| 장애 격리 | 낮음 | 높음 |\n| 기술 다양성 | 제한적 | 서비스별 독립 선택 |\n\n**마이크로서비스 단점:**\n- 네트워크 레이턴시 증가\n- 분산 트랜잭션 복잡도\n- 운영 인프라(K8s, 서비스 메시) 필요\n- 테스트 복잡도 증가\n\n**모놀리스가 더 나을 때:** 소규모 팀, 초기 서비스, 도메인이 불명확할 때',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:SYSTEM_DESIGN', '마이크로서비스 간 통신 방식에는 어떤 것들이 있나요?', '마이크로서비스',
 '마이크로서비스 간 통신은 크게 **동기 통신**과 **비동기 통신**으로 나뉩니다.\n\n**1. 동기 통신 (Synchronous)**\n\n**REST API (HTTP/HTTPS):**\n```\nService A → HTTP GET /orders/123 → Service B\n         ← 200 OK {order: ...}  ←\n```\n- 단순하고 범용적\n- 응답 대기로 레이턴시 누적 가능\n\n**gRPC:**\n```\n// Protocol Buffers 기반, HTTP/2\nOrderService.GetOrder(GetOrderRequest) → OrderResponse\n```\n- REST보다 빠름 (바이너리 직렬화)\n- 타입 안전성\n- 내부 서비스 간 통신에 적합\n\n**2. 비동기 통신 (Asynchronous)**\n\n**메시지 큐 (Kafka, RabbitMQ):**\n```\nService A → [주문완료 이벤트] → Kafka → Service B (결제)\n                                     → Service C (알림)\n```\n- 느슨한 결합\n- 장애 격리\n- 순서 처리 복잡\n\n**비교:**\n| 방식 | 결합도 | 레이턴시 | 복잡도 | 사용 케이스 |\n|------|--------|---------|--------|------------|\n| REST | 강함 | 낮음 | 낮음 | 즉각 응답 필요 |\n| gRPC | 강함 | 매우 낮음 | 중간 | 내부 고성능 통신 |\n| 메시지큐 | 느슨함 | 높음 | 높음 | 이벤트 처리, 비동기 |\n\n**실무 팁:** 사용자 응답이 필요한 흐름은 동기, 후처리(이메일, 집계)는 비동기를 권장합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:SYSTEM_DESIGN', 'API Gateway란 무엇이고 마이크로서비스에서 왜 필요한가요?', '마이크로서비스',
 'API Gateway는 클라이언트와 마이크로서비스 사이에 위치하는 단일 진입점(Single Entry Point)입니다.\n\n**API Gateway 없을 때:**\n```\n클라이언트 → [사용자 서비스:8001]\n            → [주문 서비스:8002]\n            → [결제 서비스:8003]\n// 클라이언트가 각 서비스 주소를 알아야 함\n```\n\n**API Gateway 있을 때:**\n```\n클라이언트 → [API Gateway:443]\n                  ↓ 라우팅\n           [사용자] [주문] [결제]\n```\n\n**API Gateway의 주요 기능:**\n1. **라우팅**: `/users` → 사용자 서비스, `/orders` → 주문 서비스\n2. **인증/인가**: JWT 토큰 검증을 Gateway에서 일괄 처리\n3. **Rate Limiting**: IP/사용자별 요청 수 제한\n4. **SSL 종료(SSL Termination)**: HTTPS → HTTP 변환\n5. **로드밸런싱**: 서비스 인스턴스 간 트래픽 분산\n6. **요청/응답 변환**: 프로토콜 변환, 데이터 집계\n7. **모니터링/로깅**: 중앙화된 요청 로그\n\n**대표 솔루션:**\n- AWS API Gateway\n- Kong\n- Nginx\n- Spring Cloud Gateway\n\n**주의사항:**\n- API Gateway는 SPOF(단일 장애점)가 될 수 있으므로 HA 구성 필요\n- 과도한 로직을 넣으면 병목 지점이 됨',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:SYSTEM_DESIGN', '서비스 디스커버리(Service Discovery)가 필요한 이유는 무엇인가요?', '마이크로서비스',
 '**문제 상황:**\n마이크로서비스 환경에서 서비스 인스턴스의 IP와 포트는 동적으로 변합니다(오토스케일링, 재시작, 배포 등). 하드코딩된 주소는 금방 무효화됩니다.\n\n```\n// 하드코딩 문제\nString orderServiceUrl = "http://192.168.1.10:8080"; // 재배포 시 IP 변경\n```\n\n**서비스 디스커버리:**\n서비스 인스턴스가 자신의 위치(IP:Port)를 레지스트리에 등록하고, 호출자가 레지스트리에서 주소를 조회하는 패턴.\n\n**동작 방식:**\n```\n1. 서비스 시작 시: [Order Service] → 레지스트리에 등록 (IP:포트)\n2. 서비스 호출 시: [Payment Service] → 레지스트리 조회 → Order Service 주소 획득\n3. 직접 통신: [Payment Service] → [Order Service]\n```\n\n**방식 비교:**\n| 방식 | 설명 | 예시 |\n|------|------|------|\n| Client-Side Discovery | 클라이언트가 직접 레지스트리 조회 | Eureka + Ribbon |\n| Server-Side Discovery | 로드밸런서가 레지스트리 조회 | AWS ALB, Kubernetes |\n\n**대표 솔루션:**\n- Kubernetes: 내장 서비스 디스커버리 (kube-dns)\n- Consul: HashiCorp 솔루션\n- Eureka: Netflix OSS, Spring Cloud에서 사용\n- etcd: 경량 키-값 스토어\n\n**헬스 체크(Health Check):**\n레지스트리는 주기적으로 서비스 상태를 확인하여 장애 인스턴스를 자동으로 제거합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:SYSTEM_DESIGN', '마이크로서비스에서 데이터베이스를 각 서비스별로 분리하는 이유는 무엇인가요?', '마이크로서비스',
 '**Database per Service 패턴:**\n각 마이크로서비스는 자신만의 데이터베이스를 소유하고, 다른 서비스는 API를 통해서만 데이터에 접근합니다.\n\n**공유 DB의 문제점:**\n```\n[사용자 서비스] ──┐\n[주문 서비스]   ──┼── [공유 DB] ← 모든 서비스가 직접 접근\n[결제 서비스]   ──┘\n\n문제:\n- 스키마 변경 시 모든 서비스 영향\n- DB가 SPOF(단일 장애점)가 됨\n- 서비스별 독립 배포 불가\n- DB 기술 선택의 자유 없음\n```\n\n**Database per Service 장점:**\n1. **느슨한 결합**: 서비스 독립 배포 가능\n2. **폴리글랏 퍼시스턴스**: 서비스별 최적 DB 선택 가능\n   - 사용자 서비스 → PostgreSQL\n   - 피드 서비스 → Cassandra (쓰기 최적화)\n   - 검색 서비스 → Elasticsearch\n3. **장애 격리**: 한 서비스 DB 장애가 다른 서비스에 영향 없음\n4. **독립 확장**: 필요한 서비스 DB만 스케일 업/아웃\n\n**단점과 해결책:**\n- **분산 트랜잭션 어려움** → Saga 패턴 사용\n- **조인 불가** → API Composition 또는 CQRS 패턴\n- **데이터 중복** → 이벤트 기반 동기화\n\n**결론:** 서비스 독립성을 위해 DB 분리는 마이크로서비스의 핵심 원칙입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:SYSTEM_DESIGN', 'Circuit Breaker 패턴이란 무엇인가요?', '마이크로서비스',
 'Circuit Breaker(회로 차단기) 패턴은 의존 서비스에 장애가 발생했을 때, 지속적인 호출을 차단하여 전체 시스템 장애로 이어지는 것을 방지하는 패턴입니다.\n\n**전기 회로 차단기에서 이름을 따온 패턴:**\n전류 과부하 → 차단기 작동 → 회로 보호 (나중에 복구)\n\n**Circuit Breaker 상태 전이:**\n```\n[Closed] ─── 실패 임계치 초과 ──→ [Open]\n   ↑                                  ↓\n   └──── 성공 ←── 일부 요청 허용 ── [Half-Open]\n```\n\n**각 상태 설명:**\n1. **Closed (정상)**: 모든 요청 통과, 실패 카운팅\n2. **Open (차단)**: 모든 요청 즉시 실패 반환 (Fail Fast), 일정 시간 후 Half-Open으로\n3. **Half-Open (복구 시도)**: 일부 요청만 통과, 성공 시 Closed, 실패 시 Open\n\n**장점:**\n- 장애 서비스에 대한 불필요한 호출 차단 → 리소스 절약\n- 빠른 실패(Fail Fast) → 사용자에게 즉시 오류 응답\n- 의존 서비스 장애의 연쇄 전파(Cascade Failure) 방지\n\n**Java 구현 (Resilience4j):**\n```java\n@CircuitBreaker(name = "orderService", fallbackMethod = "fallback")\npublic Order getOrder(Long id) {\n    return orderClient.getOrder(id);\n}\n\npublic Order fallback(Long id, Exception e) {\n    return Order.empty(); // 캐시 또는 기본값 반환\n}\n```',
 'MODEL_ANSWER', TRUE, NOW());

-- ============================================================
-- 로드밸런싱 (6문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:SYSTEM_DESIGN', '로드밸런싱(Load Balancing)이란 무엇이고 왜 필요한가요?', '로드밸런싱',
 '로드밸런싱은 들어오는 네트워크 트래픽을 여러 서버 인스턴스에 분산시키는 기술입니다.\n\n**로드밸런서가 없을 때:**\n```\n모든 요청 → [서버 1개]\n// 서버 1개에 모든 부하 집중 → 과부하, 장애 시 서비스 불가\n```\n\n**로드밸런서가 있을 때:**\n```\n요청 → [Load Balancer]\n              ↓ 분산\n     [서버1] [서버2] [서버3]\n```\n\n**로드밸런싱의 목적:**\n1. **고가용성(High Availability)**: 서버 1대 장애 시에도 서비스 지속\n2. **수평 확장(Horizontal Scaling)**: 서버 추가로 처리 용량 증가\n3. **성능 향상**: 요청이 여러 서버에 분산되어 응답 속도 개선\n4. **무중단 배포**: 롤링 배포 시 트래픽 점진적 전환\n\n**로드밸런서 종류:**\n| 레이어 | 설명 | 예시 |\n|--------|------|------|\n| L4 (Transport) | IP:Port 기반 분산 | AWS NLB |\n| L7 (Application) | HTTP/HTTPS 내용 기반 분산 | AWS ALB, Nginx |\n\n**헬스 체크:**\n로드밸런서는 주기적으로 서버 상태를 확인하여 장애 서버에는 트래픽을 보내지 않습니다.\n\n**대표 솔루션:**\n- AWS Elastic Load Balancer (ALB, NLB)\n- Nginx\n- HAProxy\n- Kubernetes Service',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:SYSTEM_DESIGN', '로드밸런싱 알고리즘의 종류와 각각의 특징을 설명해주세요.', '로드밸런싱',
 '**주요 로드밸런싱 알고리즘:**\n\n**1. Round Robin (라운드 로빈)**\n```\n요청1 → 서버1, 요청2 → 서버2, 요청3 → 서버3, 요청4 → 서버1...\n```\n- 순서대로 균등 분배\n- 모든 서버 성능이 동일할 때 적합\n- 구현이 가장 단순\n\n**2. Weighted Round Robin (가중치 라운드 로빈)**\n```\n서버1(weight=3): 요청1,2,3\n서버2(weight=1): 요청4\n```\n- 서버 성능(CPU, 메모리)에 비례한 가중치 설정\n- 고성능 서버에 더 많은 트래픽 배분\n\n**3. Least Connections (최소 연결)**\n```\n서버1: 현재 연결 10개\n서버2: 현재 연결 3개 ← 새 요청 배정\n서버3: 현재 연결 7개\n```\n- 현재 활성 연결 수가 가장 적은 서버에 배분\n- 처리 시간이 다양한 요청에 효과적\n\n**4. IP Hash**\n```\nhash(client_ip) % 서버수 = 서버 인덱스\n```\n- 동일 IP는 항상 동일 서버로 → 세션 유지(Sticky Session)\n- 서버 추가/제거 시 기존 매핑 변경\n\n**5. Least Response Time**\n- 응답 시간이 가장 짧은 서버에 배분\n- 실시간 성능 측정 필요\n\n**선택 기준:**\n- 단순한 경우: Round Robin\n- 서버 성능 차이: Weighted Round Robin\n- 긴 연결(WebSocket, DB): Least Connections\n- 세션 유지 필요: IP Hash 또는 Sticky Session',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:SYSTEM_DESIGN', 'L4 로드밸런서와 L7 로드밸런서의 차이점은 무엇인가요?', '로드밸런싱',
 '**OSI 모델 기준 분류:**\n\n**L4 로드밸런서 (Transport Layer):**\n- IP 주소와 포트 번호를 기반으로 트래픽 분산\n- 패킷 내용(HTTP 헤더, URL 등)을 보지 않음\n- **빠름** — 패킷 레벨 처리\n\n```\nL4 분산 기준:\n- 출발지/목적지 IP\n- TCP/UDP 포트\n- 예: 443포트 트래픽을 서버 3대에 분산\n```\n\n**L7 로드밸런서 (Application Layer):**\n- HTTP 요청 내용(URL, 헤더, 쿠키)을 기반으로 분산\n- 컨텐츠 기반 라우팅 가능\n- **더 많은 기능** — SSL 종료, 인증, 압축\n\n```\nL7 분산 기준:\n- URL 경로: /api/* → API 서버, /static/* → CDN\n- HTTP 헤더: X-Region: kr → 한국 서버\n- 쿠키: session-id → 동일 서버 (Sticky Session)\n```\n\n**비교표:**\n| 항목 | L4 | L7 |\n|------|----|----|  \n| 처리 단위 | 패킷 | HTTP 요청 |\n| 라우팅 기준 | IP, Port | URL, Header, Cookie |\n| 처리 속도 | 매우 빠름 | 상대적으로 느림 |\n| SSL 종료 | 불가 | 가능 |\n| 콘텐츠 기반 라우팅 | 불가 | 가능 |\n| 예시 | AWS NLB, HAProxy | AWS ALB, Nginx |\n\n**실무 활용:**\n- 고성능 TCP 서비스 → L4\n- 웹 서비스, API 서버 → L7 (경로 기반 라우팅, SSL 처리)',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:SYSTEM_DESIGN', 'Sticky Session이란 무엇이고, 어떤 문제가 있나요?', '로드밸런싱',
 '**Sticky Session (세션 유지 / Session Affinity):**\n동일 클라이언트의 요청을 항상 같은 서버로 보내는 로드밸런서 설정.\n\n**필요한 이유:**\n```\n서버1에 세션 저장 → 서버2로 요청이 가면 세션 없음 → 로그인 풀림\n```\n서버 메모리에 세션 정보를 저장하는 구조에서 세션 일관성을 위해 사용.\n\n**동작 방식:**\n```\n첫 요청: 클라이언트 → 로드밸런서 → 서버2 선택 → 쿠키에 서버2 정보 저장\n이후 요청: 쿠키 확인 → 항상 서버2로 라우팅\n```\n\n**Sticky Session의 문제점:**\n1. **불균등한 부하 분산**: 특정 서버에 트래픽 집중\n2. **서버 장애 시 세션 손실**: 서버2 다운 → 해당 사용자 세션 모두 만료\n3. **수평 확장 어려움**: 새 서버 추가 시 기존 세션 분산 안 됨\n\n**더 나은 해결책 — 세션 외부 저장소:**\n```\n[서버1] [서버2] [서버3]\n    ↓       ↓       ↓\n     [Redis 세션 저장소]\n// 어느 서버로 가도 동일한 세션 데이터 접근 가능\n```\n\n**결론:**\n- Sticky Session은 임시방편. 세션을 Redis 같은 외부 저장소에 저장하는 것이 더 나은 아키텍처.\n- Spring Session + Redis 조합으로 쉽게 구현 가능.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:SYSTEM_DESIGN', '수평 확장(Scale Out)과 수직 확장(Scale Up)의 차이는 무엇인가요?', '로드밸런싱',
 '**수직 확장 (Scale Up / Vertical Scaling):**\n기존 서버의 사양(CPU, RAM, 디스크)을 높이는 방식.\n\n```\n2vCPU 4GB → 8vCPU 32GB\n```\n\n**장점:** 구현 단순, 코드 변경 없음\n**단점:**\n- 하드웨어 한계 존재 (무한 증가 불가)\n- 단일 서버 장애 시 전체 서비스 중단\n- 업그레이드 시 다운타임 발생 가능\n- 고사양 장비일수록 비용 비효율적\n\n**수평 확장 (Scale Out / Horizontal Scaling):**\n서버 인스턴스 수를 늘리는 방식. 로드밸런서로 트래픽 분산.\n\n```\n서버 1대 → 서버 N대\n[LB] → [서버1] [서버2] [서버3]\n```\n\n**장점:**\n- 이론상 무한 확장 가능\n- 특정 서버 장애 시 다른 서버로 서비스 지속\n- 클라우드 환경에서 자동 확장(Auto Scaling) 가능\n- 비용 효율적 (저사양 서버 여러 대)\n\n**단점:**\n- 로드밸런서 필요\n- 상태 비저장(Stateless) 설계 필요 (세션, 로컬 캐시 문제)\n- 분산 트랜잭션, 데이터 일관성 복잡도 증가\n\n**실무 전략:**\n- 초기 서비스: Scale Up (단순)\n- 성장 후: Scale Out (유연성, 고가용성)\n- 보통 두 가지를 혼합 사용 (적당한 사양의 서버 여러 대)',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:SYSTEM_DESIGN', '헬스 체크(Health Check)란 무엇이고 로드밸런서에서 어떻게 활용되나요?', '로드밸런싱',
 '**헬스 체크 (Health Check):**\n로드밸런서가 주기적으로 서버의 상태를 확인하여 정상 서버에만 트래픽을 보내는 메커니즘.\n\n**헬스 체크 종류:**\n\n**1. TCP 헬스 체크:**\n```\n로드밸런서 → TCP 연결 시도 (포트 8080)\n연결 성공 → 정상\n연결 실패 → 비정상\n```\n가장 단순하지만 애플리케이션 내부 상태는 알 수 없음.\n\n**2. HTTP 헬스 체크:**\n```\n로드밸런서 → GET /health → 서버\n200 OK      → 정상\n그 외         → 비정상\n```\n애플리케이션 레벨 상태 확인 가능.\n\n**Spring Boot Actuator 헬스 체크:**\n```java\n// GET /actuator/health\n{\n  "status": "UP",\n  "components": {\n    "db": { "status": "UP" },\n    "redis": { "status": "UP" }\n  }\n}\n```\nDB, Redis, 디스크 공간 등 의존성 상태도 포함 가능.\n\n**헬스 체크 설정 파라미터:**\n```\n- Interval: 체크 주기 (예: 30초마다)\n- Timeout: 응답 대기 시간 (예: 5초)\n- Healthy Threshold: 정상 판정 성공 횟수 (예: 2회)\n- Unhealthy Threshold: 비정상 판정 실패 횟수 (예: 3회)\n```\n\n**중요성:**\n헬스 체크 없이는 장애 서버에도 트래픽이 계속 전달되어 사용자 오류가 발생합니다.\n로드밸런서는 비정상 서버를 감지하면 자동으로 트래픽 풀에서 제외합니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- ============================================================
-- API설계 (6문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:SYSTEM_DESIGN', 'RESTful API 설계 원칙은 무엇인가요?', 'API설계',
 'REST(Representational State Transfer)는 HTTP를 활용한 분산 시스템 아키텍처 원칙입니다.\n\n**REST 6가지 원칙:**\n1. **Client-Server**: 클라이언트와 서버 분리 → 독립적 개발 가능\n2. **Stateless**: 서버는 클라이언트 상태 저장 안 함 → 모든 요청이 자체 완결\n3. **Cacheable**: 응답에 캐시 가능 여부 명시\n4. **Uniform Interface**: 일관된 인터페이스 (리소스 기반 URL)\n5. **Layered System**: 계층화 (클라이언트는 중간 서버 존재 여부 모름)\n6. **Code on Demand** (선택): 서버가 코드 전달 가능\n\n**RESTful API 설계 규칙:**\n\n**URL 설계 (명사, 리소스 중심):**\n```\n// 좋음\nGET    /users          - 사용자 목록\nGET    /users/123      - 특정 사용자\nPOST   /users          - 사용자 생성\nPUT    /users/123      - 사용자 전체 수정\nPATCH  /users/123      - 사용자 일부 수정\nDELETE /users/123      - 사용자 삭제\n\n// 나쁨 (동사 사용)\nGET /getUser\nPOST /createUser\nGET /deleteUser?id=123\n```\n\n**HTTP 상태 코드 올바른 사용:**\n```\n200 OK          - 성공\n201 Created     - 리소스 생성 성공\n204 No Content  - 성공 (응답 바디 없음, 삭제 등)\n400 Bad Request - 잘못된 요청\n401 Unauthorized - 인증 필요\n403 Forbidden   - 권한 없음\n404 Not Found   - 리소스 없음\n500 Internal Error - 서버 오류\n```\n\n**버전 관리:**\n```\n/api/v1/users  # URL 경로 버전 (일반적)\n```',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:SYSTEM_DESIGN', 'HTTP 메서드(GET, POST, PUT, PATCH, DELETE)의 차이점과 올바른 사용법은?', 'API설계',
 '**HTTP 메서드 비교:**\n\n| 메서드 | 목적 | 멱등성 | 안전성 | 요청 바디 |\n|--------|------|--------|--------|----------|\n| GET | 조회 | O | O | X |\n| POST | 생성 | X | X | O |\n| PUT | 전체 수정/대체 | O | X | O |\n| PATCH | 일부 수정 | △ | X | O |\n| DELETE | 삭제 | O | X | X (선택) |\n\n**멱등성(Idempotent):** 동일 요청을 여러 번 해도 결과가 같음\n**안전성(Safe):** 서버 상태를 변경하지 않음\n\n**각 메서드 사용 예시:**\n```\nGET /products          → 상품 목록 조회\nGET /products/1        → 상품 1번 조회\n\nPOST /products         → 상품 생성 (ID 서버가 생성)\n{ "name": "노트북", "price": 1200000 }\n→ 201 Created, Location: /products/42\n\nPUT /products/1        → 상품 전체 교체\n{ "name": "노트북", "price": 1300000, "stock": 10 }\n// 바디에 없는 필드는 null로 처리\n\nPATCH /products/1      → 가격만 수정\n{ "price": 1300000 }\n// 다른 필드는 유지\n\nDELETE /products/1     → 상품 삭제\n→ 204 No Content\n```\n\n**PUT vs PATCH:**\n- PUT: 전체 리소스 교체 (없는 필드 → null/기본값)\n- PATCH: 지정한 필드만 수정\n\n**POST vs PUT:**\n- POST: 서버가 ID 생성 (`/products`)\n- PUT: 클라이언트가 ID 지정 (`/products/1`)',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:SYSTEM_DESIGN', 'API 버전 관리(Versioning)는 왜 필요하고 어떤 방식이 있나요?', 'API설계',
 '**API 버전 관리가 필요한 이유:**\nAPI를 변경하면 기존 클라이언트(앱, 파트너 서비스)가 오작동할 수 있습니다. 하위 호환성을 유지하면서 API를 발전시키기 위해 버전 관리가 필요합니다.\n\n**버전 관리 방식:**\n\n**1. URL Path 버전 (가장 일반적):**\n```\nGET /api/v1/users\nGET /api/v2/users\n```\n✅ 직관적, 캐시 가능, 탐색 쉬움\n❌ URL이 길어짐, REST 원칙과 약간 어긋남\n\n**2. Query Parameter 버전:**\n```\nGET /api/users?version=1\nGET /api/users?version=2\n```\n✅ URL 구조 유지\n❌ 선택적이라 누락 시 기본 버전 정책 필요\n\n**3. HTTP 헤더 버전:**\n```\nGET /api/users\nAccept: application/vnd.company.v2+json\n또는\nX-API-Version: 2\n```\n✅ URL 깔끔\n❌ 숨겨져 있어 직관적이지 않음, 브라우저 테스트 어려움\n\n**하위 호환 변경 (Breaking vs Non-Breaking):**\n```\n// Non-Breaking (버전 업 불필요)\n- 새 선택적 필드 추가\n- 새 엔드포인트 추가\n\n// Breaking (버전 업 필요)\n- 필드 삭제 또는 이름 변경\n- 응답 구조 변경\n- 필수 파라미터 추가\n```\n\n**실무 권장:**\n- URL Path 방식이 가장 범용적\n- Semantic Versioning: v1, v2 (마이너 변경은 같은 버전 유지)\n- 구 버전 Deprecation 정책을 명확히 공지',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:SYSTEM_DESIGN', 'API에서 페이지네이션(Pagination)은 왜 필요하고 어떻게 구현하나요?', 'API설계',
 '**페이지네이션이 필요한 이유:**\n전체 데이터를 한 번에 반환하면 응답 크기가 매우 크고, DB 부하와 네트워크 비용이 증가합니다.\n\n**페이지네이션 방식:**\n\n**1. Offset 기반 (Page-based):**\n```\nGET /products?page=2&size=20\n\n// SQL\nSELECT * FROM products LIMIT 20 OFFSET 40;\n```\n✅ 구현 단순, 특정 페이지 바로 접근 가능\n❌ 데이터 추가/삭제 시 항목 누락/중복 발생\n❌ OFFSET이 커질수록 DB 성능 저하\n\n**2. Cursor 기반 (Cursor-based / Keyset Pagination):**\n```\nGET /products?cursor=eyJpZCI6MTAwfQ==&size=20\n// cursor = 마지막으로 받은 항목의 인코딩된 ID\n\n// SQL\nSELECT * FROM products WHERE id > 100 LIMIT 20;\n```\n✅ 데이터 추가/삭제 영향 없음\n✅ 대용량 데이터에서 성능 일정\n❌ 특정 페이지 직접 접근 불가\n❌ 구현 복잡\n\n**응답 포맷 예시:**\n```json\n{\n  "data": [...],\n  "meta": {\n    "page": 2,\n    "size": 20,\n    "totalElements": 150,\n    "totalPages": 8,\n    "hasNext": true\n  }\n}\n```\n\n**언제 무엇을 쓸까:**\n- 관리자 페이지, 검색 결과 → Offset 기반\n- 피드, 타임라인, 무한 스크롤 → Cursor 기반\n\n**Spring Data JPA:**\n```java\nPage<Product> findAll(Pageable pageable);\n// PageRequest.of(page, size, Sort.by("createdAt").descending())\n```',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:SYSTEM_DESIGN', 'API 인증(Authentication)과 인가(Authorization)의 차이는 무엇인가요?', 'API설계',
 '**인증 (Authentication - "당신이 누구인가?"):**\n사용자의 신원을 확인하는 과정.\n\n```\n로그인 → [이메일 + 비밀번호] → 서버 확인 → "이 사람은 user@example.com이다"\n```\n\n**인가 (Authorization - "당신이 무엇을 할 수 있는가?"):**\n인증된 사용자가 특정 리소스에 접근할 권한이 있는지 확인하는 과정.\n\n```\n인증 완료 사용자 → /admin → "이 사용자는 ADMIN 역할이 없다" → 403 Forbidden\n```\n\n**순서:** 인증 → 인가 (인증 없이 인가 불가)\n\n**JWT 기반 인증 흐름:**\n```\n1. POST /auth/login {email, password}\n2. 서버: 검증 후 JWT 토큰 발급\n3. 클라이언트: Authorization 헤더에 포함\n4. 이후 요청: Authorization: Bearer {token}\n5. 서버: 토큰 검증 → 사용자 정보 추출 → 인가 확인\n```\n\n**HTTP 상태 코드 차이:**\n```\n401 Unauthorized  → 인증 실패 (로그인 안 됨 또는 토큰 만료)\n403 Forbidden     → 인가 실패 (로그인은 됐지만 권한 없음)\n```\n\n**Spring Security 기반:**\n```java\n// 인증\n@PostMapping("/login")\npublic TokenResponse login(@RequestBody LoginRequest req) { ... }\n\n// 인가\n@PreAuthorize("hasRole(''ADMIN'')")\n@GetMapping("/admin/users")\npublic List<User> getAllUsers() { ... }\n```',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:SYSTEM_DESIGN', 'API에서 에러 응답을 일관성 있게 설계하는 방법은 무엇인가요?', 'API설계',
 '일관된 에러 응답 포맷은 클라이언트 개발을 쉽게 하고 디버깅을 용이하게 합니다.\n\n**에러 응답 포맷 설계:**\n```json\n{\n  "status": "error",\n  "code": "USER_001",\n  "message": "사용자를 찾을 수 없습니다",\n  "timestamp": "2024-01-15T10:30:00Z",\n  "path": "/api/v1/users/999"\n}\n```\n\n**에러 코드 체계 (도메인 기반):**\n```\nAUTH_001: 인증 토큰이 없습니다\nAUTH_002: 토큰이 만료되었습니다\nUSER_001: 사용자를 찾을 수 없습니다\nUSER_002: 이미 존재하는 이메일입니다\nVALIDATION_001: 입력값이 유효하지 않습니다\n```\n\n**유효성 검증 에러 (복수 오류):**\n```json\n{\n  "status": "error",\n  "code": "VALIDATION_001",\n  "message": "입력값 검증 실패",\n  "errors": [\n    { "field": "email", "message": "올바른 이메일 형식이 아닙니다" },\n    { "field": "password", "message": "최소 8자 이상이어야 합니다" }\n  ]\n}\n```\n\n**Spring Boot 전역 예외 처리:**\n```java\n@RestControllerAdvice\npublic class GlobalExceptionHandler {\n\n    @ExceptionHandler(EntityNotFoundException.class)\n    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException e) {\n        return ResponseEntity.status(HttpStatus.NOT_FOUND)\n            .body(ErrorResponse.of("USER_001", e.getMessage()));\n    }\n\n    @ExceptionHandler(MethodArgumentNotValidException.class)\n    public ResponseEntity<ErrorResponse> handleValidation(...) { ... }\n}\n```\n\n**핵심 원칙:**\n1. 일관된 포맷 유지\n2. 의미 있는 에러 코드 (숫자만 사용 금지)\n3. 보안 정보 노출 금지 (스택 트레이스, SQL 쿼리 등)\n4. 클라이언트가 처리 가능한 수준의 상세 정보 제공',
 'MODEL_ANSWER', TRUE, NOW());
