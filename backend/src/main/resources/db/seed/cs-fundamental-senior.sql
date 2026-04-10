-- CS_FUNDAMENTAL SENIOR 시드 데이터 (120문항: 자료구조30, 운영체제30, 네트워크30, 데이터베이스30)

-- ============================================================
-- 자료구조 (30문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'LRU 캐시를 O(1) 시간 복잡도로 설계하는 방법과 동시성 처리를 설명해주세요.', '자료구조',
 'HashMap + Doubly Linked List로 get/put O(1)을 구현합니다. 동시성 처리를 위해 ReadWriteLock(읽기 다수 허용, 쓰기 배타적) 또는 ConcurrentLinkedHashMap을 사용합니다. 세그먼트 기반 락으로 경합을 줄이고, Caffeine 라이브러리는 Window TinyLFU로 적중률과 동시성을 모두 최적화합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'ConcurrentHashMap의 내부 구현(Java 8+)을 설명해주세요.', '자료구조',
 'Java 8에서 ConcurrentHashMap은 세그먼트 락 대신 버킷 단위 synchronized + CAS를 사용합니다. 빈 버킷은 CAS로 삽입하고, 충돌 시 해당 버킷의 첫 노드를 synchronized로 잠급니다. 노드 수 8 이상이면 트리화(TreeBin)됩니다. size()는 CounterCell 배열로 분산 카운팅하여 경합을 최소화합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'Skip List의 구조와 장단점을 Red-Black Tree와 비교해주세요.', '자료구조',
 'Skip List는 다층 연결 리스트로, 상위 레이어는 노드를 건너뛰어 O(log n) 탐색을 제공합니다. Red-Black Tree 대비 구현이 간단하고, 락프리(Lock-Free) 동시성 구현이 용이합니다. Redis의 Sorted Set이 Skip List를 사용합니다. 단점은 랜덤 레벨 할당으로 최악 성능 보장이 확률적이라는 것입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'Bloom Filter의 원리와 False Positive 확률 제어 방법을 설명해주세요.', '자료구조',
 'Bloom Filter는 비트 배열과 k개의 해시 함수를 사용하여 집합 멤버십을 확률적으로 판별합니다. 삽입 시 k개 위치를 1로 설정하고, 조회 시 모두 1이면 "아마 존재"입니다. False Positive은 가능하지만 False Negative은 불가능합니다. m(비트 수) = -n·ln(p)/(ln2)², k = (m/n)·ln2로 최적화합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', '분산 해시 테이블(DHT)의 구조와 Chord 프로토콜을 설명해주세요.', '자료구조',
 'DHT는 키-값 쌍을 여러 노드에 분산 저장하는 P2P 시스템입니다. Chord는 노드를 해시 링에 배치하고, 각 노드가 Finger Table로 O(log N) 홉에 키를 찾습니다. 노드 가입/이탈 시 후임자만 영향받아 O(log² N)에 안정화됩니다. BitTorrent, IPFS 등 분산 시스템에서 사용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', '실시간 시스템에서 자료구조 선택 시 고려사항을 설명해주세요.', '자료구조',
 '실시간 시스템은 최악 시간 복잡도(WCET)가 중요합니다. 해시 테이블은 평균 O(1)이지만 리사이징 시 O(n)이므로 부적합합니다. Red-Black Tree(O(log n) 보장), 고정 크기 배열, 링 버퍼가 적합합니다. 메모리 할당도 동적 할당 대신 메모리 풀을 사용하고, GC가 없는 언어가 선호됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', '대용량 데이터(수십 GB) 정렬 전략을 설명해주세요.', '자료구조',
 'External Merge Sort를 사용합니다. 메모리에 들어가는 청크 단위로 정렬(Internal Sort)한 후 디스크에 기록하고, k-way Merge로 병합합니다. 디스크 I/O 최소화를 위해 버퍼 크기를 최적화하고, 비동기 I/O로 읽기/쓰기를 파이프라이닝합니다. MapReduce에서는 Shuffle Sort가 분산 정렬을 수행합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'Lock-Free 자료구조의 원리와 ABA 문제를 설명해주세요.', '자료구조',
 'Lock-Free 자료구조는 CAS(Compare-and-Swap) 연산으로 경합 없이 동시 접근을 허용합니다. ABA 문제는 CAS에서 값이 A→B→A로 변경되어 원래 A로 착각하는 현상입니다. AtomicStampedReference(버전 태그)나 Hazard Pointer로 해결합니다. Lock-Free Queue(Michael-Scott), Lock-Free Stack 등이 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'Cuckoo Hashing의 원리와 장점을 설명해주세요.', '자료구조',
 'Cuckoo Hashing은 두 개의 해시 함수와 테이블을 사용합니다. 삽입 시 한 테이블의 위치가 차있으면 기존 항목을 다른 테이블로 밀어내며(뻐꾸기처럼), 이 과정이 연쇄적으로 발생합니다. 최악 조회 시간이 O(1)로 보장되며(항상 두 위치만 확인), Concurrent Cuckoo Hashing은 고성능 해시 테이블에 사용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'Count-Min Sketch의 원리와 활용 사례를 설명해주세요.', '자료구조',
 'Count-Min Sketch는 스트리밍 데이터에서 빈도를 근사적으로 추정하는 확률적 자료구조입니다. d개의 해시 함수와 w×d 카운터 배열을 사용하며, 쿼리 시 d개 위치의 최솟값을 반환합니다. 과대평가는 가능하지만 과소평가는 불가합니다. 네트워크 트래픽 분석, 추천 시스템, Top-K 빈출 항목에 활용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'Persistent Data Structure(영속 자료구조)의 개념을 설명해주세요.', '자료구조',
 '영속 자료구조는 수정 시 이전 버전을 유지하면서 새 버전을 생성합니다. 구조적 공유(Structural Sharing)로 변경된 경로만 복사하여 O(log n) 공간/시간에 새 버전을 생성합니다. Git의 트리 구조, Clojure의 Persistent Vector/Map, React의 불변 상태 관리가 이 원리를 활용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'R-Tree의 구조와 공간 인덱싱 활용을 설명해주세요.', '자료구조',
 'R-Tree는 다차원 공간 데이터를 인덱싱하는 트리로, 각 노드가 최소 경계 사각형(MBR)을 가집니다. 공간 쿼리(범위 검색, 최근접 이웃)를 효율적으로 처리합니다. PostGIS, MongoDB의 2dsphere 인덱스가 R-Tree 변형을 사용합니다. GIS, 게임 충돌 감지, 지도 서비스에서 활용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'Wavelet Tree의 구조와 응용을 설명해주세요.', '자료구조',
 'Wavelet Tree는 문자열에서 rank, select, range 쿼리를 O(log σ)에 처리하는 자료구조입니다(σ는 알파벳 크기). 이진 트리 구조로 각 노드가 비트벡터를 저장합니다. 문서 검색, 범위 빈도 쿼리, 양자화된 범위 최솟값/최댓값 등 고급 텍스트 알고리즘에서 활용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'LSM(Log-Structured Merge) Tree의 구조와 쓰기 최적화 원리를 설명해주세요.', '자료구조',
 'LSM Tree는 MemTable(인메모리, 정렬)에 쓰기 후 SSTable(디스크, 불변)로 플러시하는 구조입니다. 쓰기가 순차적이라 랜덤 I/O가 없어 쓰기 성능이 우수합니다. 읽기는 MemTable → L0 → L1 → ... 순으로 탐색하며, Bloom Filter로 가속합니다. Compaction으로 레벨 간 병합합니다. LevelDB, RocksDB, Cassandra가 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'CRDT(Conflict-free Replicated Data Type)의 개념과 종류를 설명해주세요.', '자료구조',
 'CRDT는 분산 시스템에서 동시 수정이 발생해도 자동으로 병합 가능한 자료구조입니다. G-Counter(증가만), PN-Counter(증감), G-Set(추가만), OR-Set(추가/삭제), LWW-Register(마지막 쓰기 승리) 등이 있습니다. 최종 일관성을 보장하며, 실시간 협업 편집기(Figma), 분산 데이터베이스(Riak)에서 사용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'HyperLogLog의 원리와 카디널리티 추정 방법을 설명해주세요.', '자료구조',
 'HyperLogLog는 대규모 집합의 고유 원소 수를 O(1) 공간으로 근사 추정하는 확률적 자료구조입니다. 해시값의 선행 0비트 수의 최대값으로 추정하며, 여러 레지스터의 조화 평균으로 정확도를 높입니다. 12KB 메모리로 수십억 개의 고유값을 2% 오차로 추정합니다. Redis의 PFADD/PFCOUNT가 대표적입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'Suffix Array와 Suffix Tree의 차이와 활용을 설명해주세요.', '자료구조',
 'Suffix Tree는 문자열의 모든 접미사를 트리로 구성하여 패턴 매칭을 O(m)에 수행하지만, 공간이 O(n×σ)입니다. Suffix Array는 접미사를 정렬한 배열로 O(n) 공간이며, LCP 배열과 결합하면 Suffix Tree와 동등한 기능을 제공합니다. 게놈 분석, 텍스트 압축, 중복 검출에 활용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'Merkle Tree의 구조와 블록체인에서의 활용을 설명해주세요.', '자료구조',
 'Merkle Tree는 리프 노드가 데이터 블록의 해시이고, 내부 노드가 자식 해시의 해시인 이진 트리입니다. 루트 해시만 비교하면 전체 데이터 무결성을 O(log n)에 검증할 수 있습니다. 블록체인에서 트랜잭션 검증, Git에서 커밋 무결성, P2P 파일 동기화(Cassandra Anti-Entropy)에 사용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', '분산 정렬(Distributed Sort)의 접근 방법을 설명해주세요.', '자료구조',
 'MapReduce Sort는 Map에서 키 기반 파티셔닝, Shuffle에서 네트워크 전송, Reduce에서 정렬/병합합니다. 샘플 기반 파티셔닝(TeraSort)은 데이터를 샘플링하여 균등한 범위를 결정합니다. 외부 병합 정렬의 분산 버전도 사용됩니다. 데이터 편향(Skew) 처리가 핵심 과제이며, 리파티셔닝이나 결합기(Combiner)로 해결합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'Quotient Filter의 원리와 Bloom Filter와의 비교를 설명해주세요.', '자료구조',
 'Quotient Filter는 해시값을 몫(quotient)과 나머지(remainder)로 분리하여 저장합니다. Bloom Filter와 유사한 확률적 멤버십 테스트를 제공하지만, 삭제가 가능하고, 디스크 친화적이며, 합집합(Merge)이 가능합니다. 캐시 지역성이 좋아 SSD 기반 스토리지에서 Bloom Filter보다 유리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'X-fast Trie와 Y-fast Trie의 원리를 설명해주세요.', '자료구조',
 'X-fast Trie는 Trie에 해시 테이블을 결합하여 정수 키에 대해 predecessor/successor를 O(log log U)에 찾습니다(U는 키 범위). 하지만 O(U) 공간이 필요합니다. Y-fast Trie는 X-fast Trie를 기반으로 BST를 결합하여 O(n) 공간, O(log log U) 연산을 달성합니다. IP 라우팅, 우선순위 큐에 활용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'B-epsilon Tree의 구조와 쓰기 최적화 원리를 설명해주세요.', '자료구조',
 'B-epsilon Tree는 B-Tree의 변형으로, 각 내부 노드에 메시지 버퍼를 두어 쓰기를 배치 처리합니다. epsilon 파라미터로 쓰기/읽기 성능 비율을 조절합니다. 버퍼가 가득 차면 자식으로 플러시합니다. BetrFS 파일 시스템, TokuDB가 이 구조를 사용하여 랜덤 쓰기 성능을 크게 개선합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', '대규모 그래프 처리 시스템(Pregel, GraphX)의 접근 방식을 설명해주세요.', '자료구조',
 'Pregel(Google)은 BSP(Bulk Synchronous Parallel) 모델로, 각 정점이 메시지를 주고받으며 슈퍼스텝 단위로 동기화됩니다. Vertex-Centric 프로그래밍으로 "Think Like a Vertex" 패러다임입니다. GraphX(Spark)는 RDD 기반으로 그래프를 테이블로 표현합니다. PageRank, 커뮤니티 탐지, 최단 경로 등에 사용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'Approximate Nearest Neighbor(ANN) 검색 알고리즘을 비교해주세요.', '자료구조',
 'LSH(Locality-Sensitive Hashing)는 유사한 벡터가 같은 버킷에 매핑되도록 해싱합니다. HNSW(Hierarchical Navigable Small World)는 다층 그래프로 O(log n) 탐색을 제공하며, 현재 가장 성능이 좋습니다. IVF(Inverted File)는 클러스터링 기반입니다. 벡터 DB(Pinecone, Milvus, pgvector)에서 LLM 임베딩 검색에 사용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- ============================================================
-- 운영체제 (30문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'Microkernel과 Monolithic Kernel의 차이와 트레이드오프를 설명해주세요.', '운영체제',
 'Monolithic Kernel(Linux)은 모든 서비스(파일시스템, 드라이버, 네트워크)가 커널 공간에서 실행되어 성능이 좋지만, 버그가 전체 시스템을 크래시시킵니다. Microkernel(Minix, QNX)은 최소 기능만 커널에 두고 나머지를 사용자 공간 서비스로 분리하여 안정적이지만, IPC 오버헤드가 있습니다. 하이브리드(macOS XNU)가 절충안입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'TLB의 구조와 컨텍스트 스위칭 시 성능 영향을 설명해주세요.', '운영체제',
 'TLB는 ASID(Address Space ID) 태그로 프로세스별 매핑을 구분합니다. ASID가 없으면 컨텍스트 스위칭 시 전체 TLB 플러시가 필요합니다. PCID(Process Context ID, x86)로 플러시를 최소화합니다. TLB 미스 시 페이지 테이블 워크(PTW)가 발생하며, 하드웨어 PTW(x86)와 소프트웨어 PTW(MIPS)가 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'NUMA(Non-Uniform Memory Access) 아키텍처의 특징과 최적화를 설명해주세요.', '운영체제',
 'NUMA는 각 CPU가 로컬 메모리에 빠르게 접근하고 원격 메모리에 느리게 접근하는 아키텍처입니다. numactl로 메모리 정책(bind, interleave, preferred)을 설정하고, OS는 프로세스를 로컬 메모리가 있는 노드에 배치합니다. 데이터베이스, JVM은 NUMA 인식 메모리 할당으로 원격 접근을 최소화합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', '캐시 일관성 프로토콜(MESI)을 설명해주세요.', '운영체제',
 'MESI 프로토콜은 멀티코어 캐시 일관성을 유지합니다. Modified(변경됨, 유일), Exclusive(독점, 미변경), Shared(공유, 미변경), Invalid(무효) 4가지 상태를 가집니다. 한 코어가 쓰기 시 다른 코어의 캐시 라인을 Invalid로 전환합니다. False Sharing은 다른 데이터가 같은 캐시 라인에 있어 불필요한 무효화가 발생하는 문제입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'Banker''s Algorithm(은행원 알고리즘)의 원리와 한계를 설명해주세요.', '운영체제',
 '은행원 알고리즘은 자원 요청 시 안전 상태(Safe State)를 확인하여 데드락을 회피합니다. 프로세스의 최대 자원 요구량을 미리 알아야 하며, 안전 시퀀스가 존재하는지 O(m×n²)으로 확인합니다. 한계는 최대 요구량 사전 파악 어려움, 프로세스 수/자원 수 고정, 높은 계산 비용으로 실무에서 거의 사용되지 않습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', '메모리 매핑 파일의 고급 활용과 주의사항을 설명해주세요.', '운영체제',
 'mmap은 대용량 파일을 효율적으로 처리하고, 프로세스 간 공유 메모리로 활용됩니다. MAP_PRIVATE(COW)로 독립 복사본을, MAP_SHARED로 공유 쓰기를 설정합니다. 주의사항으로 페이지 폴트 비용, msync()로 명시적 디스크 동기화, 파일 크기 변경 시 재매핑(mremap) 필요, SIGBUS 시그널 처리가 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'Copy-on-Write의 고급 활용과 커널 수준 구현을 설명해주세요.', '운영체제',
 'COW는 fork() 외에도 스냅샷 파일시스템(Btrfs, ZFS), Docker 이미지 레이어, Redis의 BGSAVE(자식 프로세스로 RDB 저장)에서 사용됩니다. 커널은 PTE(Page Table Entry)를 읽기 전용으로 설정하고, 쓰기 시 Page Fault 핸들러에서 새 페이지를 할당하고 복사합니다. Huge Pages에서 COW는 2MB 단위로 복사되어 오버헤드가 클 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'CFS(Completely Fair Scheduler)의 한계와 대안을 설명해주세요.', '운영체제',
 'CFS는 CPU-bound와 I/O-bound 작업을 구분하지 못하고, Red-Black Tree의 O(log n) 삽입이 대규모 프로세스에서 오버헤드가 됩니다. EEVDF(Earliest Eligible Virtual Deadline First)는 Linux 6.6에서 CFS를 대체하여 지연 시간 공정성을 개선합니다. 실시간 작업은 SCHED_FIFO/SCHED_RR로 별도 관리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'io_uring의 원리와 기존 I/O 모델 대비 장점을 설명해주세요.', '운영체제',
 'io_uring(Linux 5.1+)은 submission/completion 두 개의 링 버퍼를 커널과 공유하여, 시스템 콜 없이 I/O를 제출/완료할 수 있습니다. 기존 epoll + read/write는 시스템 콜 오버헤드가 있지만, io_uring은 배치 처리와 폴링 모드로 이를 제거합니다. 고성능 DB(ScyllaDB), 웹 서버에서 채택 중입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'cgroups와 namespace의 차이와 컨테이너에서의 역할을 설명해주세요.', '운영체제',
 'namespace는 프로세스의 시스템 리소스 뷰를 격리합니다(PID, Network, Mount, UTS, IPC, User). cgroups는 프로세스 그룹의 리소스 사용량(CPU, 메모리, I/O, 네트워크)을 제한합니다. Docker 컨테이너는 namespace로 격리하고 cgroups로 제한하여, 가상머신 없이 경량 격리를 제공합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'Huge Pages의 장점과 사용 시 고려사항을 설명해주세요.', '운영체제',
 'Huge Pages(2MB/1GB)는 기본 4KB 대비 TLB 엔트리 수를 줄여 TLB 미스를 크게 감소시킵니다. 대용량 메모리를 사용하는 DB(Oracle, MySQL), JVM에서 성능을 개선합니다. Transparent Huge Pages(THP)는 자동 관리하지만, 할당 지연과 Compaction 오버헤드가 있어 Redis 등에서는 비활성화가 권장됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'RCU(Read-Copy-Update)의 원리와 Linux 커널에서의 활용을 설명해주세요.', '운영체제',
 'RCU는 읽기가 빈번하고 쓰기가 드문 경우의 동기화 기법입니다. 읽기는 락 없이 수행하고, 쓰기는 복사본을 수정 후 포인터를 원자적으로 교체합니다. 이전 복사본은 Grace Period(모든 읽기 완료) 후 해제합니다. Linux 커널의 라우팅 테이블, 프로세스 목록 등에서 사용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'eBPF(Extended Berkeley Packet Filter)의 개념과 활용을 설명해주세요.', '운영체제',
 'eBPF는 커널을 수정하지 않고 커널 공간에서 샌드박스된 프로그램을 실행하는 기술입니다. Verifier가 안전성을 검증하고, JIT 컴파일로 네이티브 성능을 제공합니다. 네트워크 필터링(XDP), 시스템 추적(bpftrace), 보안 모니터링(Falco), 관측성(Cilium, Pixie)에 활용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'Memory Ordering과 Memory Barrier의 개념을 설명해주세요.', '운영체제',
 'CPU는 성능을 위해 명령어를 재정렬(Out-of-Order Execution)합니다. 멀티코어에서 이로 인해 다른 코어가 의도와 다른 순서로 메모리 변경을 관찰할 수 있습니다. Memory Barrier(fence)는 재정렬을 방지합니다. Acquire(이후 명령 선행 불가), Release(이전 명령 후행 불가), Full Barrier가 있으며, Java volatile이 이를 보장합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'Spectre/Meltdown 취약점의 원리와 OS 수준 대응을 설명해주세요.', '운영체제',
 'Meltdown은 추측 실행(Speculative Execution)으로 커널 메모리를 사이드 채널로 읽는 취약점입니다. KPTI(Kernel Page Table Isolation)로 커널/사용자 페이지 테이블을 분리하여 대응합니다. Spectre는 분기 예측을 악용하며 Retpoline, 마이크로코드 업데이트로 대응합니다. 두 취약점 모두 성능 오버헤드가 발생합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'DPDK(Data Plane Development Kit)의 원리와 장점을 설명해주세요.', '운영체제',
 'DPDK는 커널 네트워크 스택을 우회(Kernel Bypass)하여 사용자 공간에서 직접 NIC에 접근합니다. 인터럽트 대신 폴링(PMD), Hugepage 메모리, 락프리 링 버퍼를 사용하여 100Gbps급 패킷 처리가 가능합니다. 커널 컨텍스트 스위칭, 복사, 인터럽트 오버헤드를 제거합니다. NFV, 방화벽, 로드 밸런서에 사용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'Page Cache와 Buffer Cache의 차이와 통합을 설명해주세요.', '운영체제',
 'Page Cache는 파일 데이터를 페이지 단위로 캐싱하고, Buffer Cache는 블록 디바이스 데이터를 버퍼 단위로 캐싱합니다. Linux 2.4부터 두 캐시가 통합되어 Page Cache에서 관리합니다. Writeback(지연 쓰기)으로 디스크 I/O를 배치하고, vm.dirty_ratio로 Dirty Page 비율을 제어합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'seccomp과 AppArmor/SELinux의 차이와 컨테이너 보안에서의 역할을 설명해주세요.', '운영체제',
 'seccomp은 프로세스가 호출할 수 있는 시스템 콜을 필터링합니다. Docker는 기본 seccomp 프로파일로 위험한 시스템 콜(reboot, mount)을 차단합니다. AppArmor/SELinux는 MAC(Mandatory Access Control)으로 파일, 네트워크 등 리소스 접근을 제어합니다. 컨테이너 보안은 namespace + cgroups + seccomp + MAC의 다층 방어입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'OOM Killer의 동작 원리와 제어 방법을 설명해주세요.', '운영체제',
 'Linux의 OOM Killer는 메모리 부족 시 oom_score가 가장 높은 프로세스를 종료합니다. oom_score는 메모리 사용량, 스왑 사용량, 프로세스 우선순위 등으로 계산됩니다. oom_score_adj(-1000~1000)로 보호/우선 종료를 설정합니다. vm.overcommit_memory로 메모리 오버커밋 정책을 제어하고, cgroups로 프로세스별 메모리 한도를 설정합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'CPU 캐시 최적화(Cache-Oblivious vs Cache-Aware)를 설명해주세요.', '운영체제',
 'Cache-Aware 알고리즘은 캐시 크기와 라인 크기를 명시적으로 활용합니다(타일링, 블록 행렬 곱). Cache-Oblivious 알고리즘은 캐시 파라미터를 모르고도 최적 성능을 달성합니다(van Emde Boas 레이아웃). False Sharing 방지(패딩), 데이터 정렬(alignment), SoA vs AoS 레이아웃 선택이 실무에서 중요합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'futex(Fast Userspace Mutex)의 원리를 설명해주세요.', '운영체제',
 'futex는 경합이 없을 때 사용자 공간에서 CAS만으로 락을 처리하고, 경합 시에만 커널 시스템 콜(FUTEX_WAIT/FUTEX_WAKE)을 호출합니다. pthread_mutex, Java의 ReentrantLock, Go의 sync.Mutex가 내부적으로 futex를 사용합니다. 경합이 드문 경우(fast path) 시스템 콜 오버헤드를 완전히 제거합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'Kernel Same-page Merging(KSM)의 원리와 활용을 설명해주세요.', '운영체제',
 'KSM은 동일한 내용의 메모리 페이지를 하나로 병합하여 메모리를 절약합니다. 주기적으로 페이지를 스캔하고 내용이 같으면 COW로 공유합니다. KVM 가상화에서 여러 VM이 같은 OS 이미지를 실행할 때 큰 메모리 절약 효과가 있습니다. CPU 오버헤드와 보안(사이드 채널) 트레이드오프가 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- ============================================================
-- 네트워크 (30문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'QUIC 프로토콜의 내부 구조와 TCP 대비 장점을 상세히 설명해주세요.', '네트워크',
 'QUIC는 UDP 위에 신뢰성, 흐름 제어, 혼잡 제어를 구현합니다. 스트림 단위 독립 전송으로 TCP HOL Blocking을 제거하고, Connection ID로 IP 변경 시에도 연결을 유지합니다. TLS 1.3이 내장되어 1-RTT(초기) / 0-RTT(재접속) 연결이 가능합니다. Cubic/BBR 혼잡 제어를 사용자 공간에서 구현할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'Zero Trust Architecture의 원칙과 구현 방법을 설명해주세요.', '네트워크',
 'Zero Trust는 "절대 신뢰하지 않고, 항상 검증"하는 보안 모델입니다. 네트워크 위치가 아닌 ID 기반 인증, 마이크로 세그먼테이션, 최소 권한 원칙, 지속적 검증이 핵심입니다. 구현 요소로 IAM, 서비스 메시(mTLS), 마이크로 세그먼테이션, SASE, 지속적 모니터링이 있습니다. BeyondCorp(Google)이 대표적 사례입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'DDoS 공격 유형과 방어 전략을 설명해주세요.', '네트워크',
 'Volumetric(대역폭 소진: UDP Flood, Amplification), Protocol(프로토콜 취약점: SYN Flood, Ping of Death), Application(L7: HTTP Flood, Slowloris) 유형이 있습니다. 방어로 CDN/Anycast 분산, Rate Limiting, SYN Cookie, WAF, BGP Blackhole, 클라우드 스크러빙 센터를 사용합니다. 다층 방어가 필수입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'CDN 캐싱 전략과 캐시 무효화 방법을 설명해주세요.', '네트워크',
 'CDN은 Origin Pull(요청 시 가져옴)과 Push(사전 배포) 방식으로 캐싱합니다. Cache-Control, Surrogate-Key, Vary 헤더로 세밀하게 제어합니다. 무효화 방법으로 TTL 만료, Purge API(URL 단위), Tag 기반 Purge(관련 콘텐츠 일괄), Stale-While-Revalidate(만료 직전 백그라운드 갱신)가 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'TCP Fast Open(TFO)의 원리와 보안 고려사항을 설명해주세요.', '네트워크',
 'TFO는 SYN 패킷에 데이터를 포함하여 첫 요청의 RTT를 줄입니다. 서버가 쿠키(TFO Cookie)를 발급하고, 이후 클라이언트가 SYN+데이터+쿠키를 보내면 서버가 즉시 데이터를 처리합니다. 리플레이 공격 위험이 있어 멱등성 있는 요청에만 안전합니다. Linux net.ipv4.tcp_fastopen으로 설정합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'WebSocket vs SSE vs gRPC Streaming의 선택 기준을 설명해주세요.', '네트워크',
 'WebSocket은 양방향 실시간(채팅, 게임), SSE는 서버→클라이언트 단방향(알림, 피드), gRPC Streaming은 서비스 간 양방향 스트리밍(마이크로서비스)에 적합합니다. WebSocket은 프록시/방화벽 이슈, SSE는 연결 수 제한(HTTP/1.1에서 6개), gRPC는 브라우저 직접 지원 불가가 한계입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'gRPC vs REST 선택 기준을 성능, 유지보수, 생태계 관점에서 비교해주세요.', '네트워크',
 'gRPC는 바이너리 직렬화(Protobuf)로 3-10배 빠르고, HTTP/2 멀티플렉싱, 양방향 스트리밍, 강타입 코드 생성이 장점입니다. REST는 브라우저 호환, JSON 가독성, 도구 생태계(Swagger, Postman), 유연한 스키마가 장점입니다. MSA 내부는 gRPC, 외부 API는 REST, 양쪽 필요 시 gRPC-Gateway로 병행합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'BGP(Border Gateway Protocol)의 역할과 BGP Hijacking을 설명해주세요.', '네트워크',
 'BGP는 인터넷의 AS(Autonomous System) 간 라우팅 경로를 교환하는 프로토콜입니다. BGP Hijacking은 잘못된 경로 광고로 트래픽을 가로채는 공격입니다. RPKI(Resource Public Key Infrastructure)로 경로의 출처를 검증하고, BGP Monitoring으로 이상 감지합니다. 2018년 Google Cloud 사례가 유명합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'mTLS(Mutual TLS)의 원리와 서비스 메시에서의 활용을 설명해주세요.', '네트워크',
 'mTLS는 서버뿐 아니라 클라이언트도 인증서를 제시하여 양방향 인증을 수행합니다. Istio, Linkerd 같은 서비스 메시는 사이드카 프록시가 자동으로 mTLS를 설정하여 서비스 간 통신을 암호화합니다. SPIFFE/SPIRE로 서비스 ID를 관리하고, 인증서 자동 로테이션을 수행합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'TCP BBR(Bottleneck Bandwidth and RTT) 혼잡 제어의 원리를 설명해주세요.', '네트워크',
 'BBR은 패킷 손실이 아닌 실측 대역폭과 RTT를 기반으로 전송 속도를 조절합니다. 네트워크의 병목 대역폭(BtlBw)과 최소 RTT(RTprop)를 추정하여 최적 전송율을 유지합니다. Cubic 대비 높은 대역폭 활용률과 낮은 지연을 제공하며, Google이 YouTube, GCP에서 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'Network Partition 시 분산 시스템의 대응 전략을 설명해주세요.', '네트워크',
 'CAP 정리에 따라 파티션 발생 시 일관성(CP) 또는 가용성(AP) 중 하나를 선택해야 합니다. CP 시스템(ZooKeeper)은 소수 파티션의 쓰기를 거부합니다. AP 시스템(Cassandra)은 모든 파티션에서 쓰기를 허용하고 후에 충돌 해소(Last-Write-Wins, CRDT)합니다. Split-Brain 방지를 위해 Quorum, Fencing Token을 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'DNS over HTTPS(DoH)와 DNS over TLS(DoT)의 차이와 의의를 설명해주세요.', '네트워크',
 'DoT(포트 853)는 TLS로 DNS를 암호화하여 도청을 방지하지만, 전용 포트로 차단이 쉽습니다. DoH(포트 443)는 HTTPS 트래픽과 구분 불가하여 검열 우회에 유리합니다. 둘 다 DNS 쿼리의 프라이버시를 보호합니다. 단점으로 중앙 집중 DNS 제공자(Google, Cloudflare) 의존성, 기업 네트워크 보안 정책 충돌이 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'Anycast의 원리와 CDN/DNS에서의 활용을 설명해주세요.', '네트워크',
 'Anycast는 동일한 IP 주소를 여러 위치의 서버에 할당하여, 라우팅 프로토콜(BGP)이 가장 가까운 서버로 트래픽을 보내는 기술입니다. CDN의 엣지 서버, Root DNS 서버가 Anycast를 사용합니다. DDoS 방어에도 효과적(트래픽 자동 분산)입니다. 단점은 TCP 연결 유지가 어려울 수 있다는 것입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'SRv6(Segment Routing over IPv6)의 개념과 장점을 설명해주세요.', '네트워크',
 'SRv6는 IPv6 확장 헤더에 세그먼트 리스트를 포함하여 경로를 소스에서 명시적으로 지정합니다. MPLS 대비 IPv6 네이티브로 동작하여 별도 레이블 관리가 불필요합니다. 네트워크 프로그래밍(VPN, 서비스 체이닝, 트래픽 엔지니어링)이 가능하며, 클라우드 사업자(Google, Meta)에서 채택 중입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'HTTP/3의 0-RTT 연결과 Replay Attack 위험을 설명해주세요.', '네트워크',
 'HTTP/3(QUIC)의 0-RTT는 이전 연결의 세션 키를 재사용하여 첫 패킷부터 데이터를 전송합니다. 하지만 0-RTT 데이터는 Replay Attack에 취약합니다. 공격자가 0-RTT 패킷을 캡처하여 재전송할 수 있으므로, 멱등성 있는 요청(GET)에만 사용하거나 서버에서 Single-Use Ticket으로 방어합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'ECMP(Equal-Cost Multi-Path)와 트래픽 엔지니어링을 설명해주세요.', '네트워크',
 'ECMP는 동일 비용의 여러 경로로 트래픽을 분산하여 대역폭을 증가시킵니다. 5-tuple(src/dst IP, port, protocol) 해싱으로 동일 플로우를 같은 경로로 유지합니다. 트래픽 엔지니어링은 MPLS-TE, SR-TE로 특정 경로를 명시하여 혼잡을 회피합니다. Spine-Leaf 토폴로지에서 ECMP는 필수적입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'Service Mesh(Istio, Linkerd)의 아키텍처와 장단점을 설명해주세요.', '네트워크',
 'Service Mesh는 사이드카 프록시(Envoy)를 각 서비스에 배치하여 트래픽을 제어합니다. Control Plane(Istio의 istiod)이 정책을 관리하고, Data Plane(사이드카)이 실행합니다. mTLS, 트래픽 라우팅, 서킷 브레이커, 분산 추적, Rate Limiting을 서비스 코드 변경 없이 제공합니다. 리소스 오버헤드와 복잡성이 단점입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'XDP(eXpress Data Path)의 원리와 네트워크 성능 최적화를 설명해주세요.', '네트워크',
 'XDP는 eBPF를 활용하여 NIC 드라이버 수준에서 패킷을 처리합니다. 커널 네트워크 스택 진입 전에 DROP, PASS, REDIRECT, TX를 결정하여 오버헤드를 최소화합니다. DDoS 방어(초기 필터링), 로드 밸런싱(Facebook Katran), 방화벽에서 수백만 PPS를 처리합니다. DPDK과 달리 커널 안에서 동작하여 기존 스택과 공존 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'Connection Draining과 Graceful Shutdown의 구현을 설명해주세요.', '네트워크',
 'Graceful Shutdown 시 새 요청 수신을 중단하고 진행 중인 요청을 완료한 후 종료합니다. 로드 밸런서에서 Connection Draining은 대상을 Unhealthy로 표시하고 기존 연결의 타임아웃을 허용합니다. K8s에서는 preStop hook + terminationGracePeriodSeconds로 Pod 종료를 제어합니다. SIGTERM 핸들러에서 구현합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'Envoy Proxy의 아키텍처와 핵심 기능을 설명해주세요.', '네트워크',
 'Envoy는 L3/L4/L7 프록시로 Istio의 Data Plane입니다. 이벤트 루프 기반 비동기 아키텍처, 핫 리스타트(무중단 설정 변경), xDS API(동적 설정)를 제공합니다. 로드 밸런싱, 서킷 브레이커, Retry/Timeout, Rate Limiting, 분산 추적(Zipkin/Jaeger), gRPC/HTTP2 네이티브 지원이 핵심 기능입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'DPDK와 RDMA의 차이와 고성능 네트워크에서의 활용을 설명해주세요.', '네트워크',
 'DPDK는 소프트웨어 방식으로 커널을 우회하여 패킷을 처리합니다. RDMA(Remote Direct Memory Access)는 하드웨어(InfiniBand, RoCE)를 사용하여 원격 메모리에 CPU 개입 없이 직접 접근합니다. RDMA는 지연이 극도로 낮고(μs 단위), HPC, 분산 스토리지(Ceph)에 사용됩니다. DPDK는 범용 NIC에서 동작하여 접근성이 높습니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- ============================================================
-- 데이터베이스 (30문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'CAP 정리와 PACELC 확장 모델을 설명해주세요.', '데이터베이스',
 'CAP 정리는 분산 시스템에서 Consistency, Availability, Partition Tolerance를 동시에 만족할 수 없다고 합니다. PACELC는 Partition 시 A/C 선택에 더해, Else(정상)일 때 Latency/Consistency 트레이드오프를 추가합니다. DynamoDB는 PA/EL, MongoDB는 PA/EC, HBase는 PC/EC입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'BASE 이론과 Eventual Consistency의 구현 방법을 설명해주세요.', '데이터베이스',
 'BASE는 Basically Available(기본적 가용), Soft State(일시적 불일치 허용), Eventually Consistent(최종 일관성)입니다. 구현 방법으로 Read Repair(읽기 시 불일치 수정), Anti-Entropy(주기적 동기화), Gossip Protocol(노드 간 상태 전파)이 있습니다. Cassandra, DynamoDB가 대표적인 BASE 시스템입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', '분산 트랜잭션의 2PC와 Saga 패턴을 비교해주세요.', '데이터베이스',
 '2PC(Two-Phase Commit)는 Coordinator가 Prepare → Commit/Rollback을 순서대로 실행하여 강한 일관성을 보장하지만, Coordinator 장애 시 블록킹됩니다. Saga는 각 서비스가 로컬 트랜잭션을 실행하고 실패 시 보상 트랜잭션(Compensating Transaction)으로 롤백합니다. Saga는 가용성이 높지만 일시적 불일치가 발생합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'MVCC의 구현 방식을 InnoDB와 PostgreSQL에서 비교해주세요.', '데이터베이스',
 'InnoDB는 Undo Log에 이전 버전을 저장하고, DB_TRX_ID/DB_ROLL_PTR로 버전 체인을 구성합니다. 커밋 후 Undo Log를 Purge합니다. PostgreSQL은 각 행에 xmin/xmax(트랜잭션 ID)를 저장하고, 이전 버전이 테이블 내에 존재합니다. 그래서 VACUUM으로 Dead Tuple을 정리해야 합니다. InnoDB가 공간 효율적이고, PostgreSQL은 구현이 단순합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'Consistent Hashing을 활용한 데이터베이스 샤딩과 리밸런싱을 설명해주세요.', '데이터베이스',
 'Consistent Hashing은 해시 링에 노드와 데이터를 배치하여, 노드 추가/제거 시 K/N 키만 재배치합니다. Virtual Node(vnode)로 데이터 분포를 균등하게 합니다. Cassandra는 Consistent Hashing으로 자동 파티셔닝하며, DynamoDB는 Virtual Partition으로 리밸런싱합니다. 핫키 문제는 sub-partitioning이나 scatter-gather로 해결합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', '쿼리 캐싱 전략과 Cache Invalidation 패턴을 설명해주세요.', '데이터베이스',
 'Cache-Aside(Lazy Loading)는 캐시 미스 시 DB 조회 후 캐시에 저장합니다. Write-Through는 쓰기 시 캐시와 DB를 동시에 업데이트합니다. Write-Behind는 캐시에 먼저 쓰고 비동기로 DB에 반영합니다. Invalidation은 TTL, 이벤트 기반(CDC), 태그 기반이 있습니다. 캐시 관통(Cache Penetration)은 Bloom Filter로, 캐시 쇄도(Stampede)는 락으로 방지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'NoSQL 데이터베이스 선택 기준과 각 유형의 데이터 모델 특성을 설명해주세요.', '데이터베이스',
 'Key-Value(Redis, DynamoDB)는 단순 조회에 최적이며 파티셔닝이 쉽습니다. Document(MongoDB)는 계층 데이터를 JSON으로 유연하게 저장하며 스키마 변경이 자유롭습니다. Wide-Column(Cassandra)은 쓰기 집약적 시계열 데이터에 적합합니다. Graph(Neo4j)는 관계 탐색이 O(1) 홉이며 추천/소셜에 적합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'Elasticsearch의 역인덱스(Inverted Index) 구조를 상세히 설명해주세요.', '데이터베이스',
 '역인덱스는 각 토큰(단어)이 어떤 문서에 포함되어 있는지를 매핑합니다. Analyzer(토크나이저 + 필터)가 텍스트를 토큰으로 분리합니다. Term Dictionary는 정렬된 토큰 목록이고, Posting List는 각 토큰의 문서 ID 목록입니다. FST(Finite State Transducer)로 메모리 효율적 사전 탐색을 제공합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', '시계열 데이터베이스(TSDB)의 특성과 저장 최적화를 설명해주세요.', '데이터베이스',
 'TSDB(InfluxDB, TimescaleDB, Prometheus)는 시간순 데이터의 쓰기 최적화, 시간 기반 쿼리/집계, 자동 다운샘플링을 제공합니다. 저장 최적화로 열(Column) 기반 저장, 델타 인코딩, Gorilla 압축(Facebook), LSM Tree를 사용합니다. 모니터링, IoT, 금융 시계열에 적합하며, Retention Policy로 오래된 데이터를 자동 삭제합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', '데이터 웨어하우스와 데이터 레이크의 차이와 Lakehouse 아키텍처를 설명해주세요.', '데이터베이스',
 '데이터 웨어하우스(Redshift, BigQuery)는 정형 데이터를 ETL로 적재하여 빠른 분석 쿼리를 제공합니다. 데이터 레이크(S3+Athena)는 비정형 포함 원본 데이터를 저장합니다. Lakehouse(Databricks, Delta Lake)는 레이크에 ACID 트랜잭션, 스키마 강제, 인덱싱을 추가하여 두 장점을 결합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'Change Data Capture(CDC)의 구현 방법과 활용을 설명해주세요.', '데이터베이스',
 'CDC는 데이터베이스 변경 사항을 실시간으로 캡처합니다. 로그 기반(Debezium이 MySQL binlog/PostgreSQL WAL을 Kafka로 전송), 트리거 기반, 타임스탬프 기반이 있습니다. 실시간 데이터 동기화, 캐시 무효화, 이벤트 소싱, 검색 인덱스 갱신, 마이크로서비스 간 데이터 전파에 활용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'Raft 합의 알고리즘의 원리와 Paxos 대비 장점을 설명해주세요.', '데이터베이스',
 'Raft는 Leader Election, Log Replication, Safety 세 부분으로 합의를 달성합니다. Leader가 클라이언트 요청을 받아 로그를 복제하고, 과반 수 확인 후 커밋합니다. Leader 장애 시 나머지 노드가 새 Leader를 선출합니다. Paxos 대비 이해/구현이 쉽고, etcd, CockroachDB, TiKV가 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'CockroachDB와 Spanner의 분산 SQL 아키텍처를 설명해주세요.', '데이터베이스',
 'Google Spanner는 TrueTime(GPS + 원자 시계)으로 글로벌 트랜잭션 순서를 보장합니다. CockroachDB는 HLC(Hybrid Logical Clock)로 TrueTime 없이 유사한 보장을 제공합니다. 둘 다 Raft 기반 복제, 분산 ACID 트랜잭션, 자동 샤딩을 지원하며, CAP에서 CP를 선택합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'Quorum 기반 복제에서 R+W>N 조건의 의미와 트레이드오프를 설명해주세요.', '데이터베이스',
 'N은 복제본 수, R은 읽기 정족수, W는 쓰기 정족수입니다. R+W>N이면 읽기와 쓰기 집합이 반드시 겹쳐 최신 데이터를 읽을 수 있습니다(강한 일관성). R=1, W=N은 빠른 읽기, R=N, W=1은 빠른 쓰기를 제공합니다. Sloppy Quorum(Hinted Handoff)은 가용성을 높이지만 일관성이 약해집니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'Vector Database의 아키텍처와 LLM 활용을 설명해주세요.', '데이터베이스',
 'Vector DB(Pinecone, Milvus, Weaviate, pgvector)는 고차원 벡터의 유사도 검색에 최적화됩니다. HNSW, IVF+PQ(Product Quantization) 인덱스로 ANN 검색을 수행합니다. RAG(Retrieval Augmented Generation)에서 LLM에 관련 문서를 제공하여 Hallucination을 줄이고, 시맨틱 검색, 추천 시스템에 활용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', '대규모 마이그레이션(스키마 변경) 시 무중단 전략을 설명해주세요.', '데이터베이스',
 '대규모 테이블의 ALTER TABLE은 락이 오래 걸립니다. pt-online-schema-change(Percona)는 새 테이블 생성 → 트리거로 동기화 → 전환합니다. gh-ost(GitHub)는 binlog 기반으로 트리거 없이 마이그레이션합니다. Expand-Contract 패턴(새 컬럼 추가 → 양쪽 쓰기 → 마이그레이션 → 이전 컬럼 삭제)으로 무중단을 보장합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'Multi-Tenant 데이터베이스 설계 전략을 비교해주세요.', '데이터베이스',
 'Database-per-Tenant은 완전 격리되지만 관리 비용이 높습니다. Schema-per-Tenant은 연결 풀 공유로 효율적이지만 스키마 마이그레이션이 복잡합니다. Shared Table(tenant_id 컬럼)은 가장 효율적이지만 데이터 격리가 약하고 Row-Level Security가 필요합니다. 대부분 SaaS는 소수 대형 테넌트는 별도 DB, 나머지는 Shared Table을 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'Write-Ahead Log(WAL)의 성능 최적화와 Group Commit을 설명해주세요.', '데이터베이스',
 'WAL의 fsync는 트랜잭션 커밋마다 디스크 쓰기가 필요하여 병목입니다. Group Commit은 짧은 시간 내 여러 트랜잭션의 WAL을 모아 한 번의 fsync로 처리합니다. MySQL의 binlog_group_commit_sync_delay, PostgreSQL의 commit_delay가 이를 제어합니다. 배터리 백업 캐시(BBU)가 있으면 fsync 비용을 크게 줄입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'Vitess와 같은 데이터베이스 프록시의 아키텍처와 장점을 설명해주세요.', '데이터베이스',
 'Vitess(YouTube/PlanetScale)는 MySQL 앞에 프록시를 두어 수평 확장을 지원합니다. VTGate(쿼리 라우팅), VTTablet(MySQL 래퍼), Topology Service(메타데이터)로 구성됩니다. 자동 샤딩, 온라인 DDL, 커넥션 풀링, 쿼리 리라이트를 애플리케이션 변경 없이 제공합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('SENIOR:CS_FUNDAMENTAL', 'Columnar Storage와 Row Storage의 차이와 OLAP에서의 장점을 설명해주세요.', '데이터베이스',
 'Row Storage는 행 단위로 저장하여 OLTP(삽입/조회)에 적합합니다. Columnar Storage는 열 단위로 저장하여 집계 쿼리(SUM, AVG) 시 필요한 열만 읽어 I/O를 줄입니다. 같은 타입 데이터가 연속되어 압축률이 높습니다(Parquet, ORC). ClickHouse, Redshift, BigQuery가 컬럼 스토어를 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());
