-- V12: JUNIOR CS_FUNDAMENTAL 질문 Pool 시딩 (60문항: 자료구조15, 운영체제15, 네트워크15, 데이터베이스15)

-- ============================================================
-- 자료구조 (15문항)
-- ============================================================

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Array와 LinkedList의 차이점은 무엇인가요?', '자료구조', 1,
 '연속 메모리 vs 노드 참조 구조 이해, 시간 복잡도 비교, 캐시 지역성 언급',
 'Array는 연속된 메모리 공간에 데이터를 저장하여 인덱스를 통한 O(1) 접근이 가능하지만, 중간 삽입/삭제 시 O(n)의 시간이 소요됩니다. LinkedList는 각 노드가 다음 노드의 참조를 갖는 구조로, 삽입/삭제가 O(1)이지만 탐색에 O(n)이 걸립니다. 캐시 지역성(Cache Locality) 측면에서는 Array가 유리합니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Stack과 Queue의 차이점과 각각의 활용 사례를 설명해주세요.', '자료구조', 2,
 'LIFO/FIFO 개념 이해, 실제 활용 사례 제시, Java 구현체 언급',
 'Stack은 LIFO(Last In First Out) 구조로 함수 호출 스택, 뒤로가기 기능, 괄호 검사 등에 사용됩니다. Queue는 FIFO(First In First Out) 구조로 BFS 탐색, 작업 스케줄링, 메시지 큐 등에 활용됩니다. Java에서는 Stack 대신 Deque 사용이 권장됩니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'HashMap의 동작 원리를 설명해주세요.', '자료구조', 3,
 'hashCode 기반 버킷 인덱싱, 해시 충돌 처리(Chaining→Tree), load factor와 리사이징 이해',
 'HashMap은 key의 hashCode()를 기반으로 버킷 인덱스를 결정하고, 해당 버킷에 key-value 쌍을 저장합니다. 해시 충돌 시 Java 8 이전에는 Chaining(LinkedList)으로 처리했고, Java 8부터는 버킷 내 노드가 8개를 초과하면 Red-Black Tree로 변환하여 탐색 성능을 O(n)에서 O(log n)으로 개선합니다. 기본 load factor는 0.75이며, 이를 초과하면 리사이징이 발생합니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'HashTable과 HashMap의 차이점은 무엇인가요?', '자료구조', 4,
 '동기화 여부, null 허용, ConcurrentHashMap 대안 언급',
 'HashTable은 synchronized 키워드로 모든 메서드가 동기화되어 Thread-Safe하지만 성능이 낮고, null key/value를 허용하지 않습니다. HashMap은 비동기로 동작하여 성능이 우수하고, null key 1개와 null value를 허용합니다. 멀티스레드 환경에서는 ConcurrentHashMap 사용이 권장됩니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Tree와 Binary Search Tree(BST)에 대해 설명해주세요.', '자료구조', 5,
 'BST 속성 이해, 시간 복잡도, 편향 트리 문제와 자가 균형 트리 언급',
 'Tree는 계층적 구조를 나타내는 비선형 자료구조입니다. BST는 각 노드의 왼쪽 서브트리에는 더 작은 값, 오른쪽에는 더 큰 값이 위치하는 이진 트리입니다. 평균 탐색/삽입/삭제가 O(log n)이지만, 편향 트리의 경우 O(n)이 됩니다. 이를 방지하기 위해 AVL 트리, Red-Black 트리 같은 자가 균형 트리를 사용합니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Heap 자료구조에 대해 설명해주세요.', '자료구조', 6,
 '완전 이진 트리 기반, Max/Min Heap 속성, 시간 복잡도, PriorityQueue 연결',
 'Heap은 완전 이진 트리 기반으로, Max Heap은 부모가 자식보다 항상 크고, Min Heap은 부모가 항상 작은 구조입니다. 삽입과 삭제 모두 O(log n)이며, 최대/최소값 조회는 O(1)입니다. 우선순위 큐의 구현체로 사용되며, Java에서는 PriorityQueue가 Min Heap으로 구현되어 있습니다. 힙 정렬, 다익스트라 알고리즘 등에 활용됩니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Graph의 표현 방법(인접 행렬 vs 인접 리스트)과 차이점은?', '자료구조', 7,
 '공간 복잡도 비교, 희소/밀집 그래프 적합성, 연결 확인 시간 복잡도',
 '인접 행렬은 V×V 크기의 2차원 배열로, 두 노드 간 연결 여부를 O(1)에 확인할 수 있지만 공간 복잡도가 O(V²)입니다. 인접 리스트는 각 노드에 연결된 노드들의 리스트를 저장하여 공간 복잡도가 O(V+E)로 효율적입니다. 간선이 적은 희소 그래프에는 인접 리스트, 간선이 많은 밀집 그래프에는 인접 행렬이 유리합니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Hash Collision(해시 충돌) 해결 방법을 설명해주세요.', '자료구조', 8,
 'Chaining vs Open Addressing 구분, 각 방식의 세부 기법, Java HashMap 방식 언급',
 'Chaining은 같은 버킷에 LinkedList/Tree로 충돌된 요소들을 연결하는 방식입니다. Open Addressing은 충돌 시 다른 빈 버킷을 탐색하는 방식으로, Linear Probing(순차 탐색), Quadratic Probing(제곱 간격 탐색), Double Hashing(두 번째 해시 함수 사용) 등이 있습니다. Java의 HashMap은 Chaining 방식을 사용합니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Red-Black Tree의 특징과 사용 이유를 설명해주세요.', '자료구조', 9,
 '5가지 속성, 자가 균형 메커니즘(회전/색상변경), O(log n) 보장, Java 활용처',
 'Red-Black Tree는 자가 균형 이진 탐색 트리로, 각 노드가 Red 또는 Black 색상을 가지며 5가지 속성(루트는 Black, 리프(NIL)는 Black, Red 노드의 자식은 Black, 모든 경로의 Black 노드 수 동일 등)을 유지합니다. 삽입/삭제 시 회전과 색상 변경으로 균형을 유지하여 최악의 경우에도 O(log n)을 보장합니다. Java의 TreeMap, HashMap의 버킷(8개 초과 시) 등에 사용됩니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Trie(트라이) 자료구조에 대해 설명해주세요.', '자료구조', 10,
 '문자열 검색 특화, 시간 복잡도 O(L), 활용 사례, Compressed Trie 언급',
 'Trie는 문자열 검색에 특화된 트리 구조로, 각 노드가 문자 하나를 저장하며 루트부터 특정 노드까지의 경로가 하나의 문자열을 표현합니다. 문자열 검색이 O(L)(L=문자열 길이)로 매우 빠르며, 자동완성, 사전 구현, IP 라우팅 테이블 등에 활용됩니다. 공간 효율성을 높이기 위해 Compressed Trie(Patricia Trie)도 사용됩니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'ArrayList와 LinkedList는 언제 각각 사용해야 하나요?', '자료구조', 11,
 '실무 관점의 성능 비교, 캐시 지역성 이해, ArrayList 우선 권장 이유',
 'ArrayList는 인덱스 기반 조회가 빈번한 경우(O(1) 접근)에 적합합니다. 내부적으로 배열을 사용하여 캐시 지역성이 좋고 대부분의 경우 성능이 우수합니다. LinkedList는 리스트 앞쪽의 빈번한 삽입/삭제가 필요한 경우에 유리하지만, 실무에서는 ArrayList가 대부분의 시나리오에서 더 나은 성능을 보여 권장됩니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'B-Tree와 B+Tree의 차이점을 설명해주세요.', '자료구조', 12,
 'data 저장 위치 차이, 리프 노드 LinkedList, DB 인덱스 활용 이유',
 'B-Tree는 모든 노드에 key와 data를 저장하며, B+Tree는 리프 노드에만 data를 저장하고 내부 노드에는 key만 저장합니다. B+Tree의 리프 노드들은 LinkedList로 연결되어 범위 검색에 유리합니다. 내부 노드에 더 많은 key를 저장할 수 있어 트리 높이가 낮아지고 디스크 I/O가 줄어듭니다. 이러한 이유로 데이터베이스 인덱스에 B+Tree가 주로 사용됩니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Deque(덱)이란 무엇이며 어디에 사용되나요?', '자료구조', 13,
 'Double-Ended Queue 개념, Stack/Queue 대체, ArrayDeque 권장 이유',
 'Deque(Double-Ended Queue)는 양쪽 끝에서 삽입/삭제가 가능한 자료구조입니다. Stack과 Queue의 기능을 모두 수행할 수 있습니다. Java에서는 ArrayDeque가 대표적 구현체로, Stack보다 성능이 우수하여 Stack 대안으로 권장됩니다. 슬라이딩 윈도우 알고리즘, 작업 스케줄링 등에 활용됩니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '정렬 알고리즘들의 시간 복잡도를 비교해주세요.', '자료구조', 14,
 'O(n²) vs O(n log n) 구분, 각 정렬 특성, Java 내부 정렬 구현체 언급',
 '버블/선택/삽입 정렬은 평균·최악 O(n²)이지만 구현이 간단합니다. 머지 정렬은 항상 O(n log n)이며 안정 정렬이지만 O(n) 추가 공간이 필요합니다. 퀵 정렬은 평균 O(n log n), 최악 O(n²)이지만 캐시 효율이 좋아 실무에서 가장 빠릅니다. 힙 정렬은 항상 O(n log n)이며 추가 공간이 O(1)입니다. Java의 Arrays.sort()는 원시타입에 Dual-Pivot Quicksort, 객체에 TimSort(머지+삽입)를 사용합니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '시간 복잡도와 공간 복잡도의 개념을 설명해주세요.', '자료구조', 15,
 'Big-O 표기법, 시간/공간 트레이드오프, 메모이제이션 예시',
 '시간 복잡도는 입력 크기(n)에 대해 알고리즘의 연산 횟수 증가율을 나타내며, Big-O 표기법으로 최악의 경우를 표현합니다. 공간 복잡도는 알고리즘이 사용하는 메모리 양의 증가율입니다. 일반적으로 시간과 공간은 트레이드오프 관계이며, 상황에 따라 메모리를 더 사용하여 속도를 높이거나(메모이제이션), 메모리를 아끼면서 시간을 희생하기도 합니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

-- ============================================================
-- 운영체제 (15문항)
-- ============================================================

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '프로세스와 스레드의 차이점을 설명해주세요.', '운영체제', 1,
 '메모리 구조 차이(Code/Data/Stack/Heap), IPC vs 공유 메모리, 동기화 문제 인식',
 '프로세스는 운영체제로부터 독립된 메모리 공간(Code, Data, Stack, Heap)을 할당받는 실행 단위입니다. 스레드는 프로세스 내에서 Stack만 별도로 할당받고, Code, Data, Heap 영역을 공유하는 실행 단위입니다. 프로세스 간 통신(IPC)은 비용이 크지만, 스레드 간에는 공유 메모리를 통해 빠르게 통신할 수 있습니다. 단, 공유 자원 접근 시 동기화 문제가 발생할 수 있습니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Context Switching이란 무엇이며, 왜 비용이 발생하나요?', '운영체제', 2,
 'PCB 저장/복원 과정, 캐시 무효화, 프로세스 vs 스레드 전환 비용 차이',
 'Context Switching은 CPU가 현재 실행 중인 프로세스/스레드의 상태(PCB: 레지스터 값, PC, 스택 포인터 등)를 저장하고, 다음 프로세스/스레드의 상태를 복원하는 과정입니다. 이 과정에서 캐시 무효화(Cache Flush), PCB 저장/복원, 메모리 맵핑 교체 등으로 오버헤드가 발생합니다. 스레드 간 전환이 프로세스 간 전환보다 비용이 적은데, 메모리 공간을 공유하기 때문입니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '데드락(Deadlock)이란 무엇이며, 발생 조건 4가지를 설명해주세요.', '운영체제', 3,
 '4가지 조건 정확한 설명, 예방 방법(조건 깨뜨리기) 언급',
 '데드락은 두 개 이상의 프로세스가 서로가 점유한 자원을 기다리며 무한히 대기하는 상태입니다. 발생 조건 4가지는: 상호 배제(자원은 한 프로세스만 사용), 점유와 대기(자원을 보유한 채 다른 자원 대기), 비선점(다른 프로세스의 자원을 강제로 빼앗을 수 없음), 순환 대기(프로세스 간 자원 요청이 원형으로 구성)입니다. 이 중 하나라도 깨뜨리면 데드락을 예방할 수 있습니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '뮤텍스(Mutex)와 세마포어(Semaphore)의 차이점은?', '운영체제', 4,
 '소유권 개념 차이, 카운터 기반 동작, Binary Semaphore와 뮤텍스 비교',
 '뮤텍스는 1개의 스레드만 임계 영역에 접근할 수 있는 Locking 메커니즘으로, 소유권 개념이 있어 잠금을 획득한 스레드만 해제할 수 있습니다. 세마포어는 카운터 기반으로 동시 접근 가능한 스레드 수를 제어하며, 소유권이 없어 다른 스레드가 Signal할 수 있습니다. Binary Semaphore(0/1)는 뮤텍스와 유사하지만, 소유권 여부에서 차이가 있습니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '가상 메모리(Virtual Memory)란 무엇인가요?', '운영체제', 5,
 '가상/물리 주소 변환(MMU), 페이지 테이블, Swap 영역, 메모리 보호',
 '가상 메모리는 물리 메모리(RAM)의 크기와 관계없이 프로세스에 독립적인 가상 주소 공간을 제공하는 기술입니다. 페이지 테이블을 통해 가상 주소를 물리 주소로 변환(MMU)하며, 자주 사용되지 않는 페이지는 디스크(Swap 영역)로 내보냅니다. 이를 통해 물리 메모리보다 큰 프로그램을 실행할 수 있고, 프로세스 간 메모리 보호가 가능합니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '페이지 교체 알고리즘에 대해 설명해주세요.', '운영체제', 6,
 'FIFO/LRU/LFU/Optimal 비교, Belady''s Anomaly, 실무 사용 알고리즘',
 'Page Fault 발생 시 어떤 페이지를 교체할지 결정하는 알고리즘입니다. FIFO는 가장 먼저 들어온 페이지를 교체하지만 Belady''s Anomaly가 발생할 수 있습니다. LRU(Least Recently Used)는 가장 오래 사용되지 않은 페이지를 교체하며 실무에서 가장 많이 사용됩니다. LFU(Least Frequently Used)는 사용 빈도가 가장 낮은 페이지를 교체합니다. Optimal은 가장 오랫동안 사용되지 않을 페이지를 교체하지만 미래 예측이 불가하여 이론적 비교 기준으로 사용됩니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'CPU 스케줄링 알고리즘에 대해 설명해주세요.', '운영체제', 7,
 'FCFS/SJF/RR/Priority 비교, Convoy Effect/Starvation/Aging 개념',
 'FCFS(First Come First Served)는 도착 순서대로 처리하며 Convoy Effect 발생 가능합니다. SJF(Shortest Job First)는 실행 시간이 짧은 것부터 처리하여 평균 대기 시간이 최소이지만 기아(Starvation) 문제가 있습니다. Round Robin은 시간 할당량(Time Quantum)만큼 순환하며 실행하여 응답 시간이 우수합니다. Priority 스케줄링은 우선순위 기반이며, Aging으로 기아 문제를 해결합니다. 현대 OS는 Multi-Level Feedback Queue를 주로 사용합니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '사용자 모드와 커널 모드의 차이점을 설명해주세요.', '운영체제', 8,
 '권한 차이, 시스템 콜 전환 과정, 성능 최적화 관점',
 '사용자 모드는 일반 애플리케이션이 실행되는 제한된 권한의 모드로, 하드웨어 직접 접근이 불가합니다. 커널 모드는 OS 커널이 실행되는 모드로, 모든 시스템 자원에 접근 가능합니다. 프로그램이 파일 I/O, 네트워크 등 시스템 자원이 필요할 때 시스템 콜(System Call)을 통해 커널 모드로 전환됩니다. 이 전환에는 오버헤드가 발생하므로, 불필요한 시스템 콜을 줄이는 것이 성능 최적화의 핵심입니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'IPC(Inter-Process Communication) 방법들을 설명해주세요.', '운영체제', 9,
 '파이프/메시지큐/공유메모리/소켓/시그널 구분, 각 방식의 특성',
 '파이프(Pipe)는 부모-자식 프로세스 간 단방향 통신에 사용됩니다. 메시지 큐는 커널에 메시지를 저장하여 비동기 통신이 가능합니다. 공유 메모리(Shared Memory)는 프로세스 간 메모리 영역을 공유하여 가장 빠르지만 동기화가 필요합니다. 소켓은 네트워크 통신뿐 아니라 같은 호스트 내에서도 사용 가능하며, 시그널은 프로세스에 이벤트를 알리는 비동기 통신 방식입니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '동기(Synchronous)와 비동기(Asynchronous)의 차이점은?', '운영체제', 10,
 '동기/비동기 vs 블로킹/논블로킹 구분, Node.js 이벤트 루프 예시',
 '동기는 요청 후 결과가 올 때까지 대기하는 방식으로, 순서가 보장되지만 블로킹이 발생합니다. 비동기는 요청 후 결과를 기다리지 않고 다른 작업을 수행하며, 결과는 콜백/이벤트로 전달받습니다. 이와 별개로 블로킹/논블로킹은 제어권 반환 여부를 의미합니다. 예를 들어, 비동기+논블로킹 조합은 Node.js의 이벤트 루프에서 사용되며 높은 동시 처리가 가능합니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '멀티프로세스 vs 멀티스레드의 장단점을 비교해주세요.', '운영체제', 11,
 '안정성 vs 효율성 트레이드오프, Race Condition/Deadlock 위험 인식',
 '멀티프로세스는 독립된 메모리 공간으로 하나의 프로세스 오류가 다른 프로세스에 영향을 주지 않아 안정적이지만, 프로세스 간 통신 비용이 크고 Context Switching 오버헤드가 큽니다. 멀티스레드는 메모리를 공유하여 통신이 빠르고 자원 효율적이지만, 하나의 스레드 오류가 전체 프로세스에 영향을 주고 동기화 문제(Race Condition, Deadlock)가 발생할 수 있습니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Race Condition이란 무엇이며 어떻게 해결하나요?', '운영체제', 12,
 '공유 자원 동시 접근 문제, Java 동기화 도구들(synchronized, Lock, Atomic, ConcurrentHashMap)',
 'Race Condition은 두 개 이상의 스레드가 공유 자원에 동시에 접근하여 실행 순서에 따라 결과가 달라지는 상황입니다. 해결 방법으로는 Mutex/Semaphore로 임계 영역을 보호하는 방법, Java에서는 synchronized 키워드, ReentrantLock, Atomic 클래스(CAS 연산 기반), ConcurrentHashMap 같은 동시성 컬렉션을 사용할 수 있습니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '페이징(Paging)과 세그멘테이션(Segmentation)의 차이점은?', '운영체제', 13,
 '고정 크기 vs 가변 크기, 내부/외부 단편화, 현대 OS 방식',
 '페이징은 물리 메모리를 고정 크기(보통 4KB)의 프레임으로, 가상 메모리를 같은 크기의 페이지로 나눕니다. 내부 단편화가 발생할 수 있지만 외부 단편화는 없습니다. 세그멘테이션은 논리적 단위(Code, Data, Stack 등)로 가변 크기로 나누어 프로그래머 관점에서 자연스럽지만 외부 단편화가 발생합니다. 현대 OS는 페이징 기반에 세그멘테이션을 결합한 방식을 사용합니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Thrashing이란 무엇이며 어떻게 해결하나요?', '운영체제', 14,
 'Page Fault 빈발 → CPU 이용률 저하 이해, Working Set/PFF 해결 방법',
 'Thrashing은 프로세스가 실행보다 페이지 교체에 더 많은 시간을 소비하는 현상으로, 메모리가 부족하여 Page Fault가 빈번하게 발생할 때 나타납니다. CPU 이용률이 급격히 떨어지는 것이 특징입니다. 해결 방법으로는 Working Set 모델(자주 사용하는 페이지 집합을 메모리에 유지), PFF(Page Fault Frequency) 알고리즘, 물리 메모리 증설, 멀티프로그래밍 정도 조절 등이 있습니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '캐시 메모리의 동작 원리와 지역성(Locality)에 대해 설명해주세요.', '운영체제', 15,
 '시간적/공간적 지역성 구분, Array vs LinkedList 캐시 효율 비교',
 '캐시는 CPU와 메인 메모리 사이에 위치한 고속 메모리로, 자주 사용되는 데이터를 저장하여 메모리 접근 시간을 줄입니다. 시간적 지역성(Temporal Locality)은 최근 사용된 데이터가 다시 사용될 가능성이 높다는 원리이고, 공간적 지역성(Spatial Locality)은 사용된 데이터 근처의 데이터가 곧 사용될 가능성이 높다는 원리입니다. Array가 LinkedList보다 캐시 효율이 좋은 이유가 공간적 지역성 때문입니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

-- ============================================================
-- 네트워크 (15문항)
-- ============================================================

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'OSI 7계층과 TCP/IP 4계층에 대해 설명해주세요.', '네트워크', 1,
 '각 계층 이름과 역할, 캡슐화/역캡슐화, 두 모델 비교',
 'OSI 7계층은 물리-데이터링크-네트워크-전송-세션-표현-응용 계층으로 구성된 참조 모델입니다. TCP/IP 4계층은 실제 인터넷에서 사용되는 모델로 네트워크 인터페이스-인터넷-전송-응용 계층으로 구성됩니다. 각 계층은 독립적으로 동작하여 한 계층의 변경이 다른 계층에 영향을 주지 않습니다(캡슐화/역캡슐화).',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'TCP와 UDP의 차이점을 설명해주세요.', '네트워크', 2,
 '연결 지향/비연결, 신뢰성, 흐름/혼잡 제어, 활용 사례',
 'TCP는 연결 지향적 프로토콜로 3-way Handshake로 연결을 수립하고, 신뢰성 있는 데이터 전송(순서 보장, 재전송, 흐름 제어, 혼잡 제어)을 제공합니다. UDP는 비연결형 프로토콜로 신뢰성을 보장하지 않지만 오버헤드가 적어 빠릅니다. TCP는 HTTP, 이메일 등에, UDP는 실시간 스트리밍, DNS, 게임 등에 사용됩니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'TCP 3-way Handshake와 4-way Handshake를 설명해주세요.', '네트워크', 3,
 'SYN/ACK/FIN 시퀀스, TIME_WAIT 목적',
 '3-way Handshake(연결 수립): 클라이언트→서버: SYN(seq=x), 서버→클라이언트: SYN+ACK(seq=y, ack=x+1), 클라이언트→서버: ACK(ack=y+1). 4-way Handshake(연결 종료): 클라이언트→서버: FIN, 서버→클라이언트: ACK, 서버→클라이언트: FIN, 클라이언트→서버: ACK. 종료 시 클라이언트는 TIME_WAIT 상태에서 일정 시간 대기하여 지연된 패킷을 처리합니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'HTTP와 HTTPS의 차이점을 설명해주세요.', '네트워크', 4,
 'SSL/TLS 암호화, TLS Handshake, 대칭키/비대칭키, SEO 영향',
 'HTTP는 평문으로 데이터를 전송하여 도청, 변조, 위장의 위험이 있습니다. HTTPS는 SSL/TLS 프로토콜을 통해 데이터를 암호화하여 전송합니다. TLS Handshake 과정에서 서버 인증서 검증, 대칭키 교환이 이루어지며, 이후 대칭키로 데이터를 암호화합니다. 현대 웹에서는 HTTPS가 기본이며, SEO에도 영향을 줍니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'HTTP/1.1과 HTTP/2의 차이점을 설명해주세요.', '네트워크', 5,
 'HOL Blocking, 멀티플렉싱, 헤더 압축, 서버 푸시',
 'HTTP/1.1은 요청-응답이 순차적이어서 Head-of-Line Blocking 문제가 있고, 텍스트 기반 프로토콜입니다. HTTP/2는 바이너리 프레이밍, 멀티플렉싱(하나의 연결에서 여러 요청 동시 처리), 헤더 압축(HPACK), 서버 푸시 기능을 제공합니다. 이를 통해 레이턴시를 크게 줄이고 페이지 로딩 속도를 개선합니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'REST API의 특징과 설계 원칙을 설명해주세요.', '네트워크', 6,
 'Stateless, Cacheable, Uniform Interface, URI 설계 규칙',
 'REST는 자원(Resource)을 URI로 식별하고, HTTP 메서드(GET, POST, PUT, DELETE)로 CRUD 연산을 수행하는 아키텍처 스타일입니다. 주요 원칙은: Stateless(서버가 클라이언트 상태를 저장하지 않음), Cacheable(응답에 캐시 가능 여부 명시), Uniform Interface(일관된 인터페이스), Client-Server 분리입니다. URI는 명사형 복수로, 행위는 HTTP 메서드로 표현합니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'DNS 동작 과정을 설명해주세요.', '네트워크', 7,
 '재귀적 질의 과정(Root→TLD→Authoritative), 캐시/TTL, UDP 53번 포트',
 '도메인 이름을 IP 주소로 변환하는 과정입니다. 브라우저 캐시 확인, OS 캐시(hosts 파일), Local DNS 서버에 질의, 재귀적 질의: Root DNS → TLD DNS(.com) → Authoritative DNS 순으로 조회하여 IP를 반환합니다. 결과는 TTL 동안 캐시됩니다. DNS는 주로 UDP 53번 포트를 사용하며, 응답이 512바이트를 초과하면 TCP를 사용합니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '쿠키(Cookie)와 세션(Session)의 차이점은?', '네트워크', 8,
 '저장 위치, 보안, 분산 환경 세션 관리(Redis)',
 '쿠키는 클라이언트(브라우저)에 저장되는 작은 데이터로, 매 요청마다 서버에 전송됩니다. 세션은 서버에 저장되며, 클라이언트에는 Session ID만 쿠키로 전달합니다. 쿠키는 변조 위험이 있어 민감한 정보를 저장하면 안 되고, 세션은 서버 메모리를 사용하므로 사용자가 많으면 부하가 증가합니다. 분산 환경에서는 세션 클러스터링이나 Redis 기반 세션 저장소를 사용합니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'JWT(JSON Web Token)의 구조와 특징을 설명해주세요.', '네트워크', 9,
 'Header/Payload/Signature 구조, Stateless, Access+Refresh Token 전략',
 'JWT는 Header(알고리즘, 토큰 타입), Payload(클레임 데이터), Signature(서명) 세 부분으로 구성되며 각각 Base64로 인코딩됩니다. Stateless하여 서버가 별도의 세션 저장소 없이 토큰만으로 인증이 가능하고, 서버 확장성이 좋습니다. 단, 토큰이 탈취되면 만료 전까지 무효화가 어렵기 때문에, Access Token(짧은 만료)과 Refresh Token(긴 만료)을 조합하여 사용합니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '로드 밸런싱(Load Balancing)에 대해 설명해주세요.', '네트워크', 10,
 '알고리즘(RR, Weighted, Least Connection, IP Hash), L4 vs L7',
 '로드 밸런싱은 들어오는 트래픽을 여러 서버에 분산하여 가용성과 성능을 높이는 기술입니다. 알고리즘으로는 Round Robin(순차 배분), Weighted Round Robin(가중치 기반), Least Connection(연결 수가 적은 서버로), IP Hash(클라이언트 IP 기반) 등이 있습니다. L4 로드 밸런서는 전송 계층(IP/Port)에서, L7 로드 밸런서는 응용 계층(URL/Header)에서 동작합니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '웹 브라우저에 URL을 입력하면 일어나는 과정을 설명해주세요.', '네트워크', 11,
 'DNS→TCP→TLS→HTTP→렌더링 전체 흐름, 각 단계 설명',
 'URL 파싱(프로토콜, 도메인, 경로 분리), DNS 조회로 IP 주소 획득, TCP 3-way Handshake로 연결(HTTPS면 TLS Handshake 추가), HTTP 요청 전송, 서버가 요청 처리 후 HTTP 응답 반환, 브라우저가 HTML 파싱→DOM 트리 생성→CSSOM 생성→렌더 트리 구성→레이아웃→페인팅 과정으로 화면을 렌더링합니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'CDN(Content Delivery Network)이란 무엇인가요?', '네트워크', 12,
 '엣지 서버 캐싱, 레이턴시 감소, DDoS 방어, 정적/동적 콘텐츠',
 'CDN은 전 세계에 분산된 엣지 서버에 콘텐츠를 캐싱하여, 사용자에게 지리적으로 가장 가까운 서버에서 콘텐츠를 제공하는 시스템입니다. 이를 통해 레이턴시 감소, 원본 서버 부하 분산, DDoS 방어 등의 효과를 얻을 수 있습니다. 정적 콘텐츠(이미지, CSS, JS)뿐만 아니라 동적 콘텐츠 가속도 지원합니다. AWS CloudFront, Cloudflare 등이 대표적입니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'CORS(Cross-Origin Resource Sharing)란 무엇인가요?', '네트워크', 13,
 'Same-Origin Policy, Preflight 요청(OPTIONS), Access-Control-Allow-Origin 헤더',
 'CORS는 브라우저의 Same-Origin Policy(동일 출처 정책)를 우회하여, 다른 출처의 리소스에 접근할 수 있도록 허용하는 메커니즘입니다. 서버가 응답 헤더(Access-Control-Allow-Origin 등)로 허용할 출처를 지정합니다. Simple Request는 바로 요청이 가능하지만, PUT/DELETE 등은 Preflight 요청(OPTIONS 메서드)으로 먼저 허용 여부를 확인합니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'WebSocket과 HTTP의 차이점을 설명해주세요.', '네트워크', 14,
 '단방향 vs 양방향(Full-Duplex), HTTP Upgrade, 실시간 활용 사례',
 'HTTP는 요청-응답 기반의 단방향 통신으로, 클라이언트가 요청해야 서버가 응답합니다. WebSocket은 HTTP Upgrade를 통해 연결을 수립한 후, 양방향(Full-Duplex) 통신이 가능합니다. 지속적인 연결을 유지하여 실시간 데이터 전송에 적합하며, 채팅, 실시간 알림, 주식 시세 등에 활용됩니다. 연결 유지 비용이 있으므로 실시간성이 불필요한 경우 HTTP가 효율적입니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'TCP의 흐름 제어와 혼잡 제어의 차이점을 설명해주세요.', '네트워크', 15,
 '흐름 제어(Sliding Window) vs 혼잡 제어(Slow Start/CA/FR/FR) 구분',
 '흐름 제어(Flow Control)는 송신자와 수신자 사이의 데이터 처리 속도 차이를 조절하는 것으로, Sliding Window 방식으로 수신자의 버퍼 크기(Window Size)만큼만 전송합니다. 혼잡 제어(Congestion Control)는 네트워크 전체의 혼잡 상태를 고려하여 전송 속도를 조절하는 것으로, Slow Start, Congestion Avoidance, Fast Retransmit, Fast Recovery 알고리즘이 사용됩니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

-- ============================================================
-- 데이터베이스 (15문항)
-- ============================================================

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '인덱스(Index)란 무엇이며 왜 사용하나요?', '데이터베이스', 1,
 'B+Tree 기반, Full Table Scan vs Index Scan, 쓰기 성능 트레이드오프',
 '인덱스는 테이블의 검색 속도를 향상시키기 위한 자료구조로, 책의 목차와 유사합니다. 대부분 B+Tree 구조로 구현되며, WHERE 절의 조건 컬럼에 인덱스가 있으면 Full Table Scan 대신 Index Scan으로 O(log n)에 조회 가능합니다. 단, INSERT/UPDATE/DELETE 시 인덱스도 갱신해야 하므로 쓰기 성능이 저하되고 추가 저장 공간이 필요합니다. 따라서 읽기 빈도가 높은 컬럼에 선택적으로 사용합니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '트랜잭션(Transaction)의 ACID 속성을 설명해주세요.', '데이터베이스', 2,
 '4가지 속성 정확한 설명, 각 속성의 보장 메커니즘 이해',
 'Atomicity(원자성): 트랜잭션의 모든 연산이 완전히 수행되거나 전혀 수행되지 않아야 합니다. Consistency(일관성): 트랜잭션 전후로 데이터베이스의 무결성 제약 조건이 유지됩니다. Isolation(격리성): 동시에 실행되는 트랜잭션들이 서로 영향을 주지 않아야 합니다. Durability(지속성): 커밋된 트랜잭션의 결과는 시스템 장애가 발생해도 영구적으로 보존됩니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '트랜잭션 격리 수준(Isolation Level) 4가지를 설명해주세요.', '데이터베이스', 3,
 '각 수준별 이상 현상(Dirty/Non-Repeatable/Phantom Read), MySQL/PostgreSQL 기본값',
 'READ UNCOMMITTED: 커밋되지 않은 데이터도 읽을 수 있어 Dirty Read 발생. READ COMMITTED: 커밋된 데이터만 읽지만 Non-Repeatable Read 발생. REPEATABLE READ: 같은 트랜잭션 내 같은 데이터를 반복 읽어도 일관성 보장하지만 Phantom Read 발생 가능(MySQL InnoDB는 Gap Lock으로 방지). SERIALIZABLE: 완전한 격리로 동시성이 가장 낮음. MySQL 기본은 REPEATABLE READ, PostgreSQL은 READ COMMITTED입니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '정규화(Normalization)란 무엇이며 왜 필요한가요?', '데이터베이스', 4,
 '1NF~BCNF 단계, 이상 현상 방지, 반정규화 트레이드오프',
 '정규화는 데이터 중복을 최소화하고 이상(Anomaly) 현상을 방지하기 위해 테이블을 분해하는 과정입니다. 제1정규형(원자값), 제2정규형(부분 함수 종속 제거), 제3정규형(이행적 함수 종속 제거), BCNF(결정자가 모두 후보키)까지 주로 적용합니다. 과도한 정규화는 JOIN 증가로 성능 저하를 일으킬 수 있어, 실무에서는 읽기 성능을 위해 의도적으로 반정규화(Denormalization)하기도 합니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'JOIN의 종류와 차이점을 설명해주세요.', '데이터베이스', 5,
 'INNER/LEFT/RIGHT/FULL/CROSS JOIN 구분, 실무 사용 빈도',
 'INNER JOIN은 양쪽 테이블에 모두 존재하는 데이터만 반환합니다. LEFT(OUTER) JOIN은 왼쪽 테이블의 모든 행과, 매칭되는 오른쪽 데이터를 반환(없으면 NULL). RIGHT JOIN은 반대 방향입니다. FULL OUTER JOIN은 양쪽 모든 행을 반환합니다. CROSS JOIN은 카테시안 곱으로 모든 조합을 생성합니다. 실무에서는 INNER JOIN과 LEFT JOIN이 가장 많이 사용됩니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'N+1 문제란 무엇이며 어떻게 해결하나요?', '데이터베이스', 6,
 '문제 발생 원리, Fetch Join/@EntityGraph/Batch Size/DTO 조회 해결 방법',
 'N+1 문제는 1번의 쿼리로 N개의 데이터를 조회한 후, 각 데이터에 대해 추가로 N번의 쿼리가 발생하는 현상입니다. 예를 들어 게시글 10개를 조회하고 각 게시글의 작성자를 개별 조회하면 총 11번의 쿼리가 실행됩니다. 해결 방법: Fetch Join(JPQL JOIN FETCH)으로 한 번에 조회, @EntityGraph 사용, Batch Size 설정으로 IN절 묶어 조회, 필요한 데이터만 DTO로 직접 조회.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'NoSQL과 RDBMS의 차이점을 설명해주세요.', '데이터베이스', 7,
 '스키마/확장성/일관성 비교, NoSQL 유형 구분, CAP 이론',
 'RDBMS는 정형화된 스키마, 테이블 간 관계, SQL, ACID 트랜잭션을 제공하며 데이터 일관성이 중요한 경우에 적합합니다. NoSQL은 유연한 스키마, 수평적 확장(Sharding)이 용이하며, 대용량 분산 처리에 강합니다. Key-Value(Redis), Document(MongoDB), Column-Family(Cassandra), Graph(Neo4j) 등의 유형이 있습니다. CAP 이론에서 RDBMS는 CA, NoSQL은 CP/AP를 선택하는 경향이 있습니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '옵티마이저와 실행 계획(Execution Plan)에 대해 설명해주세요.', '데이터베이스', 8,
 'CBO, EXPLAIN 명령, type/rows/Extra 분석 방법',
 '옵티마이저는 SQL 쿼리를 가장 효율적으로 실행할 방법을 결정하는 DBMS의 핵심 구성 요소입니다. 비용 기반 옵티마이저(CBO)는 통계 정보(카디널리티, 선택도 등)를 기반으로 최적의 실행 계획을 산출합니다. EXPLAIN 명령으로 실행 계획을 확인할 수 있으며, type(ALL, index, range, ref, const), rows, Extra 등의 정보를 통해 쿼리 성능을 분석하고 최적화합니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'DB 락(Lock)의 종류와 동작 방식을 설명해주세요.', '데이터베이스', 9,
 'S-Lock/X-Lock, Row-Level Lock, Record/Gap/Next-Key Lock',
 '공유 락(Shared Lock/S-Lock)은 읽기 작업에 사용되며, 여러 트랜잭션이 동시에 획득 가능합니다. 배타 락(Exclusive Lock/X-Lock)은 쓰기 작업에 사용되며, 하나의 트랜잭션만 획득 가능합니다. MySQL InnoDB에서는 Row-Level Lock을 기본으로 사용하며, Record Lock, Gap Lock(범위 잠금), Next-Key Lock(Record+Gap)으로 세분화됩니다. 락 범위가 넓을수록 동시성은 떨어지지만 일관성은 높아집니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Redis란 무엇이며 어떤 경우에 사용하나요?', '데이터베이스', 10,
 '인메모리, 자료구조 지원, 캐시/분산락/Pub-Sub 활용, 영속성(RDB/AOF)',
 'Redis는 인메모리 Key-Value 저장소로, 데이터를 메모리에 저장하여 매우 빠른 읽기/쓰기가 가능합니다. String, List, Set, Hash, Sorted Set 등 다양한 자료구조를 지원합니다. 캐시(DB 조회 결과, 세션 저장), 분산 락, 메시지 브로커(Pub/Sub), 랭킹/리더보드(Sorted Set), Rate Limiter 등에 활용됩니다. 영속성을 위해 RDB 스냅샷과 AOF 로그 방식을 지원합니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '데이터베이스 파티셔닝과 샤딩의 차이점은?', '데이터베이스', 11,
 '단일 DB 내 분할 vs 다수 DB 분산, 수평/수직 파티셔닝, 샤딩 단점',
 '파티셔닝은 하나의 DB 내에서 대용량 테이블을 작은 단위로 분할하는 것으로, 수평 파티셔닝(행 기준)과 수직 파티셔닝(열 기준)이 있습니다. 샤딩은 여러 DB 서버에 데이터를 분산 저장하는 것으로, 수평 파티셔닝의 확장 개념입니다. 샤딩은 처리량을 선형적으로 확장할 수 있지만, 조인이 어렵고 데이터 재분배가 복잡하며 트랜잭션 관리가 어려워지는 단점이 있습니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'DB 커넥션 풀(Connection Pool)이란 무엇인가요?', '데이터베이스', 12,
 '커넥션 재사용, HikariCP, 풀 사이즈 결정 공식',
 '커넥션 풀은 미리 일정 수의 DB 커넥션을 생성해 풀에 보관하고, 요청 시 재사용하는 기법입니다. 커넥션 생성 비용(TCP Handshake, 인증 등)을 줄여 성능을 향상시킵니다. Java에서는 HikariCP가 가장 많이 사용되며, 최소/최대 커넥션 수, 유휴 시간, 대기 타임아웃 등을 설정합니다. 적절한 풀 사이즈 설정이 중요하며, 일반적으로 (CPU 코어 수 x 2) + 유효 디스크 수를 기준으로 합니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Slow Query를 최적화하는 방법을 설명해주세요.', '데이터베이스', 13,
 'EXPLAIN, 인덱스, 커버링 인덱스, No Offset 페이징, Slow Query 로그',
 'EXPLAIN으로 실행 계획 분석, 적절한 인덱스 추가(WHERE, JOIN, ORDER BY 컬럼), SELECT * 대신 필요한 컬럼만 조회, 서브쿼리를 JOIN으로 변환, LIMIT으로 결과 수 제한, 페이징 시 Cursor 기반(No Offset) 사용, 커버링 인덱스 활용(인덱스만으로 조회 완료), 불필요한 정렬 제거, 통계 정보 갱신(ANALYZE TABLE). 실무에서는 Slow Query 로그를 모니터링하여 지속적으로 개선합니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'MVCC(Multi-Version Concurrency Control)란 무엇인가요?', '데이터베이스', 14,
 'Undo Log, 스냅샷 읽기, 읽기/쓰기 비블로킹',
 'MVCC는 데이터를 변경할 때 이전 버전을 유지하여, 읽기 작업이 쓰기 작업을 블로킹하지 않도록 하는 동시성 제어 기법입니다. MySQL InnoDB는 Undo Log에 이전 버전을 보관하고, 각 트랜잭션은 시작 시점의 스냅샷을 기반으로 데이터를 읽습니다. 이를 통해 읽기와 쓰기가 서로를 블로킹하지 않아 동시 처리 성능이 향상됩니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'DB 레플리케이션(Replication)의 종류와 목적을 설명해주세요.', '데이터베이스', 15,
 'Master-Slave 구조, 읽기/쓰기 분리, 동기/비동기/반동기 비교',
 '레플리케이션은 DB를 복제하여 Master-Slave(Source-Replica) 구조를 만드는 것입니다. Master는 쓰기 처리, Slave는 읽기 처리를 분담하여 부하를 분산합니다. 동기식 레플리케이션은 데이터 일관성이 높지만 성능이 낮고, 비동기식은 빠르지만 지연(Replication Lag)이 발생할 수 있습니다. 반동기식(Semi-Sync)은 최소 하나의 Slave가 수신을 확인한 후 커밋하는 절충 방식입니다.',
 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

-- ============================================================
-- Vol.2 자료구조 (15문항 추가)
-- ============================================================

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'HashSet의 내부 동작 원리를 설명해주세요.', '자료구조', 16, 'HashMap 기반 구현, hashCode/equals 오버라이드 필요성, 중복 제거 원리', 'HashSet은 내부적으로 HashMap을 사용하여 구현됩니다. 요소를 추가하면 해당 요소를 HashMap의 key로, 더미 객체(PRESENT)를 value로 저장합니다. 따라서 중복을 허용하지 않으며, 순서를 보장하지 않습니다. 요소의 동등성 판단은 hashCode()와 equals()로 이루어지므로, 커스텀 객체를 HashSet에 넣을 때는 두 메서드를 반드시 오버라이드해야 합니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Comparable과 Comparator의 차이점은 무엇인가요?', '자료구조', 17, 'compareTo vs compare, 자연 정렬 vs 외부 정렬, 람다 활용', 'Comparable은 클래스 자체에 자연 정렬 기준을 정의하는 인터페이스로, compareTo() 메서드를 구현합니다. Comparator는 외부에서 별도의 정렬 기준을 정의하는 인터페이스로, compare() 메서드를 구현합니다. Comparable은 하나의 정렬 기준만 가능하지만, Comparator는 여러 정렬 기준을 유연하게 만들 수 있어 람다식과 함께 자주 사용됩니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Iterator와 for-each의 차이점은 무엇인가요?', '자료구조', 18, 'Syntactic Sugar 관계, ConcurrentModificationException, 순회 중 삭제', 'for-each는 내부적으로 Iterator를 사용하는 Syntactic Sugar입니다. Iterator는 hasNext(), next(), remove() 메서드를 제공하며, 순회 중 안전하게 요소를 제거할 수 있습니다. 반면 for-each 루프 내에서 컬렉션을 직접 수정하면 ConcurrentModificationException이 발생합니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '이진 탐색(Binary Search)의 동작 원리와 전제 조건은?', '자료구조', 19, 'O(log n), 정렬 전제 조건, 랜덤 접근 필요, Java API', '이진 탐색은 정렬된 배열에서 중간값과 타겟을 비교하여 탐색 범위를 절반씩 줄이는 알고리즘입니다. 시간 복잡도는 O(log n)입니다. 전제 조건으로 데이터가 반드시 정렬되어 있어야 하며, 인덱스 기반 랜덤 접근이 가능해야 합니다. Java에서는 Arrays.binarySearch()와 Collections.binarySearch()로 제공됩니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'TreeMap과 HashMap의 차이점을 설명해주세요.', '자료구조', 20, 'Red-Black Tree vs 해시 테이블, 정렬 보장, NavigableMap, 범위 검색', 'HashMap은 해시 테이블 기반으로 삽입/조회가 평균 O(1)이지만 순서를 보장하지 않습니다. TreeMap은 Red-Black Tree 기반으로 key가 자동 정렬되며, 삽입/조회가 O(log n)입니다. TreeMap은 범위 검색(subMap, headMap, tailMap)이 가능합니다. 순서가 필요하면 TreeMap, 성능이 우선이면 HashMap을 사용합니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'LinkedHashMap은 어떤 경우에 사용하나요?', '자료구조', 21, '삽입 순서/접근 순서 유지, LRU 캐시 구현, removeEldestEntry', 'LinkedHashMap은 HashMap에 이중 연결 리스트를 추가하여 삽입 순서(또는 접근 순서)를 유지하는 자료구조입니다. accessOrder 옵션을 true로 설정하면 LRU 캐시 구현에 활용할 수 있습니다. removeEldestEntry()를 오버라이드하면 최대 크기를 제한하는 간단한 캐시를 만들 수 있습니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '그래프 탐색에서 BFS와 DFS의 차이점을 설명해주세요.', '자료구조', 22, 'Queue vs Stack/재귀, 최단 경로 vs 경로 존재, 공간 복잡도', 'BFS(너비 우선 탐색)는 Queue를 사용하여 시작 노드에서 가까운 노드부터 탐색하며, 최단 경로 탐색에 유리합니다. DFS(깊이 우선 탐색)는 Stack 또는 재귀를 사용하여 한 경로를 끝까지 탐색한 후 백트래킹합니다. 경로 존재 여부는 DFS, 최단 경로는 BFS가 적합합니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '다익스트라(Dijkstra) 알고리즘의 원리와 한계를 설명해주세요.', '자료구조', 23, '그리디 + 우선순위 큐, 시간 복잡도, 음의 가중치 한계', '다익스트라는 시작 노드에서 모든 노드까지의 최단 경로를 구하는 그리디 알고리즘입니다. 우선순위 큐(Min Heap)를 사용하며 시간 복잡도는 O((V+E) log V)입니다. 음의 가중치 간선이 있으면 정상 동작하지 않으며, 이 경우 벨만-포드 알고리즘을 사용해야 합니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Union-Find(Disjoint Set) 자료구조에 대해 설명해주세요.', '자료구조', 24, 'Find/Union 연산, 경로 압축, 랭크 기반 합치기, 크루스칼 활용', 'Union-Find는 서로소 집합을 관리하는 자료구조로, 경로 압축(Path Compression)과 랭크 기반 합치기(Union by Rank)를 적용하면 연산당 거의 O(1)에 가깝습니다. 크루스칼 알고리즘에서 사이클 검출, 네트워크 연결 여부 판단 등에 활용됩니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '동적 프로그래밍(DP)의 개념과 적용 조건을 설명해주세요.', '자료구조', 25, '메모이제이션, 최적 부분 구조, 중복 부분 문제, Top-Down vs Bottom-Up', '동적 프로그래밍은 큰 문제를 작은 하위 문제로 나누어 풀고, 그 결과를 저장(메모이제이션)하여 중복 계산을 방지하는 기법입니다. 적용 조건은 최적 부분 구조와 중복 부분 문제입니다. Top-Down(재귀+메모이제이션)과 Bottom-Up(반복문+테이블) 방식이 있습니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Segment Tree란 무엇이며 언제 사용하나요?', '자료구조', 26, '구간 질의 O(log n), 업데이트+질의 동시 필요, Lazy Propagation', 'Segment Tree는 배열의 구간 합, 구간 최솟값/최댓값 등의 구간 질의를 O(log n)에 처리할 수 있는 트리 자료구조입니다. 배열의 값이 빈번하게 업데이트되면서 동시에 구간 질의가 필요한 경우에 적합합니다. Lazy Propagation을 적용하면 구간 업데이트도 O(log n)에 가능합니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '해시 함수의 좋은 조건은 무엇인가요?', '자료구조', 27, '결정적, 균일 분포, 눈사태 효과, Java String hashCode 31 이유', '좋은 해시 함수는 결정적(Deterministic)이고, 균일 분포를 보이며, 계산이 빠르고, 눈사태 효과(Avalanche Effect)가 있어야 합니다. Java의 String hashCode()는 31을 승수로 사용하는데, 홀수 소수여서 비트 분포가 고르고 컴파일러가 시프트 연산으로 최적화할 수 있기 때문입니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'ConcurrentHashMap의 동작 원리를 설명해주세요.', '자료구조', 28, 'Java 7 Segment Lock vs Java 8 CAS+synchronized, 읽기 무락', 'ConcurrentHashMap은 멀티스레드 환경에서 안전하게 사용할 수 있는 HashMap입니다. Java 8부터는 각 버킷의 첫 번째 노드에 대해 synchronized를 사용하고 CAS 연산을 활용하여 세밀한 락을 제공합니다. 읽기 연산은 락이 필요 없고, 쓰기 연산만 해당 버킷에 락을 걸어 높은 동시 처리 성능을 보장합니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '그리디(Greedy) 알고리즘과 DP의 차이점은?', '자료구조', 29, '탐욕적 선택 속성, 최적해 보장 조건, 거스름돈 vs 배낭 문제 예시', '그리디는 매 순간 최적의 선택을 하여 전체 최적해를 구하는 방식입니다. DP는 모든 경우를 고려하여 최적해를 보장합니다. 그리디는 탐욕적 선택 속성이 성립할 때만 사용 가능합니다. 거스름돈 문제는 그리디로 풀 수 있지만, 배낭 문제는 DP가 필요합니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Bloom Filter란 무엇인가요?', '자료구조', 30, '확률적 자료구조, False Positive, 공간 효율, Redis/크롤러 활용', 'Bloom Filter는 원소가 집합에 속하는지 확률적으로 판별하는 자료구조입니다. "없다"는 100% 정확하지만 "있다"는 거짓 양성(False Positive)이 발생할 수 있습니다. 공간 효율이 매우 좋아 Redis의 중복 체크, 웹 크롤러의 URL 중복 방문 방지 등에 활용됩니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

-- ============================================================
-- Vol.2 운영체제 (15문항 추가)
-- ============================================================

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'PCB(Process Control Block)란 무엇인가요?', '운영체제', 16, 'PCB 구성 요소, Context Switching과의 관계', 'PCB는 운영체제가 각 프로세스를 관리하기 위해 유지하는 자료구조입니다. PID, 프로세스 상태, PC, CPU 레지스터 값, 메모리 관리 정보, 스케줄링 정보 등을 포함합니다. Context Switching 시 현재 프로세스의 PCB를 저장하고 다음 프로세스의 PCB를 복원합니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '프로세스의 생명주기(상태 변화)를 설명해주세요.', '운영체제', 17, 'New→Ready→Running→Waiting→Terminated 전이, Preemption', '프로세스는 New → Ready → Running → Waiting → Terminated 상태를 거칩니다. Running에서 Time Quantum 만료 시 Ready로 돌아가고(Preemption), I/O 요청 시 Waiting으로 전환됩니다. I/O 완료 시 Waiting에서 Ready로 이동합니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '스핀락(Spinlock)이란 무엇이며 언제 사용하나요?', '운영체제', 18, 'Busy Waiting, 멀티코어 조건, 뮤텍스와 비교', '스핀락은 락을 획득할 때까지 반복적으로 확인(Busy Waiting)하는 동기화 기법입니다. Context Switching이 발생하지 않아 락 보유 시간이 매우 짧은 경우 오버헤드가 적습니다. 멀티코어 환경에서 효과적이며, 싱글코어에서는 CPU를 낭비하므로 부적합합니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'fork()와 exec()의 차이점을 설명해주세요.', '운영체제', 19, '프로세스 복제 vs 프로그램 교체, Copy-on-Write, fork+exec 패턴', 'fork()는 현재 프로세스를 복제하여 자식 프로세스를 생성합니다. Copy-on-Write로 실제 쓰기가 발생할 때까지 메모리 페이지를 공유합니다. exec()은 현재 프로세스의 메모리 공간을 새로운 프로그램으로 교체합니다. 보통 fork()로 자식을 만든 뒤 exec()으로 다른 프로그램을 실행하는 패턴을 사용합니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Copy-on-Write(COW)란 무엇인가요?', '운영체제', 20, 'fork 시 메모리 공유, 쓰기 시점 복사, exec 시 불필요한 복사 방지', 'Copy-on-Write는 fork() 시 부모와 자식 프로세스가 동일한 물리 메모리 페이지를 공유하다가, 어느 한쪽이 수정을 시도할 때 해당 페이지만 복사하는 기법입니다. fork() 후 바로 exec()을 호출하는 경우 불필요한 복사를 완전히 방지합니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '인터럽트(Interrupt)의 종류와 처리 과정을 설명해주세요.', '운영체제', 21, '하드웨어/소프트웨어 인터럽트, ISR, IVT, 우선순위', '인터럽트는 하드웨어 인터럽트(타이머, I/O 장치)와 소프트웨어 인터럽트(시스템 콜, 예외)로 나뉩니다. 처리 과정: 인터럽트 발생 → 현재 상태 저장 → IVT에서 ISR 주소 확인 → ISR 실행 → 상태 복원. 인터럽트에는 우선순위가 있어 선점이 가능합니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'TLB(Translation Lookaside Buffer)란 무엇인가요?', '운영체제', 22, '페이지 테이블 캐시, TLB Hit/Miss, Context Switching 시 Flush, ASID', 'TLB는 가상 주소에서 물리 주소로의 변환 결과를 캐싱하는 하드웨어 캐시입니다. TLB Hit 시 메모리 접근 없이 바로 물리 주소를 얻을 수 있어 성능이 크게 향상됩니다. Context Switching 시 TLB가 Flush되며, ASID를 사용하면 이를 완화할 수 있습니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '메모리 단편화(Fragmentation)의 종류와 해결 방법은?', '운영체제', 23, '내부/외부 단편화 구분, 압축, 페이징, 버디 시스템', '내부 단편화는 할당된 블록 내부의 미사용 공간, 외부 단편화는 흩어진 공간으로 연속 할당 불가한 상태입니다. 해결 방법으로 압축(Compaction), 페이징(고정 크기 분할), 버디 시스템(Buddy System) 등이 있습니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '파일 시스템의 inode란 무엇인가요?', '운영체제', 24, 'inode 메타데이터, 파일명은 디렉토리 엔트리, 하드링크 vs 심볼릭 링크', 'inode는 Unix/Linux 파일 시스템에서 파일의 메타데이터를 저장하는 자료구조입니다. 파일 이름은 포함하지 않으며 디렉토리 엔트리에서 매핑됩니다. 하드 링크는 같은 inode를 가리키고, 심볼릭 링크는 경로 문자열을 저장하는 별도의 inode입니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '스와핑(Swapping)이란 무엇인가요?', '운영체제', 25, 'Swap Out/In, Demand Paging, swappiness, Thrashing 연관', '스와핑은 메모리가 부족할 때 프로세스를 디스크의 Swap 영역으로 내보내고 필요할 때 다시 불러오는 기법입니다. 빈번하면 Thrashing으로 이어집니다. 리눅스에서는 swappiness 파라미터로 스왑 빈도를 조절할 수 있습니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '리눅스의 프로세스 vs 스레드 구현 방식을 설명해주세요.', '운영체제', 26, 'task_struct, clone() 플래그, LWP, 커널 스케줄링 동일', '리눅스에서는 프로세스와 스레드를 모두 task_struct로 관리합니다. clone() 시스템 콜의 플래그로 자원 공유 범위를 결정합니다. 스레드는 경량 프로세스(LWP)로 취급되며, 커널 스케줄러는 프로세스와 스레드를 동일하게 스케줄링합니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '교착 상태 해결 전략 4가지를 설명해주세요.', '운영체제', 27, '예방/회피/탐지/무시, 은행원 알고리즘, Ostrich Algorithm', '예방: 발생 조건 하나를 원천 차단. 회피: 은행원 알고리즘으로 안전 상태 유지. 탐지: 자원 할당 그래프로 데드락 탐지 후 해결. 무시: 발생 확률이 낮으면 무시(Ostrich Algorithm). 대부분의 현대 OS가 무시 방식을 사용합니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '실시간 운영체제(RTOS)란 무엇인가요?', '운영체제', 28, 'Hard/Soft Real-Time, 우선순위 역전, 결정적 스케줄링', 'RTOS는 정해진 시간 내에 작업을 완료하는 것을 보장하는 운영체제입니다. Hard Real-Time은 데드라인 초과가 시스템 실패이고, Soft Real-Time은 성능 저하로 이어집니다. 우선순위 역전 방지, 결정적 스케줄링, 낮은 인터럽트 지연시간이 핵심입니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'DMA(Direct Memory Access)란 무엇인가요?', '운영체제', 29, 'CPU 개입 없는 데이터 전송, Programmed I/O 비교, 인터럽트 완료 알림', 'DMA는 CPU의 개입 없이 I/O 장치가 메인 메모리에 직접 데이터를 전송하는 기술입니다. 전송이 완료되면 DMA 컨트롤러가 인터럽트로 CPU에 알립니다. 디스크 I/O, 네트워크 통신 등에서 필수적입니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '가상 메모리에서 Demand Paging이란 무엇인가요?', '운영체제', 30, '필요 시 적재, Page Fault 처리, 메모리 절약', 'Demand Paging은 프로세스 시작 시 모든 페이지를 적재하지 않고, 실제로 접근할 때 해당 페이지만 메모리에 로드하는 기법입니다. 메모리 사용량을 줄이고 프로세스 시작 시간을 단축합니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

-- ============================================================
-- Vol.2 네트워크 (15문항 추가)
-- ============================================================

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'IP 주소와 서브넷 마스크의 역할을 설명해주세요.', '네트워크', 16, 'IPv4/IPv6, 네트워크/호스트 부분, 서브넷팅, CIDR 표기', 'IP 주소는 네트워크에서 장치를 식별하는 논리적 주소입니다. 서브넷 마스크는 IP 주소에서 네트워크 부분과 호스트 부분을 구분합니다. 서브넷팅을 통해 네트워크를 효율적으로 분할하고 브로드캐스트 도메인을 줄여 트래픽을 관리합니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'ARP(Address Resolution Protocol)의 동작 과정을 설명해주세요.', '네트워크', 17, 'IP→MAC 변환, 브로드캐스트/유니캐스트, ARP 캐시, L2 범위', 'ARP는 IP 주소를 MAC 주소로 변환하는 프로토콜입니다. 브로드캐스트로 ARP Request를 전송하고 해당 호스트가 유니캐스트로 응답합니다. 결과는 ARP 캐시에 저장됩니다. 같은 네트워크(L2) 내에서만 동작합니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'NAT(Network Address Translation)란 무엇인가요?', '네트워크', 18, '사설/공인 IP 변환, Static/Dynamic/PAT, NAT 트래버설 문제', 'NAT는 사설 IP를 공인 IP로 변환하는 기술입니다. PAT/NAPT가 가장 일반적이며, IPv4 주소 부족 문제를 완화합니다. P2P 통신이나 WebRTC에서 NAT 트래버설 문제가 발생합니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'HTTP 메서드의 멱등성(Idempotency)이란 무엇인가요?', '네트워크', 19, 'GET/PUT/DELETE 멱등, POST 비멱등, 네트워크 재시도 안전성', '멱등성은 같은 요청을 여러 번 보내도 결과가 동일한 성질입니다. GET, PUT, DELETE는 멱등적이고, POST는 비멱등적입니다. 멱등성은 네트워크 장애 시 안전한 재시도를 가능하게 합니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'HTTP 상태 코드의 주요 분류를 설명해주세요.', '네트워크', 20, '1xx~5xx 분류, 주요 코드', '1xx(정보), 2xx(성공: 200/201/204), 3xx(리다이렉션: 301/302/304), 4xx(클라이언트 오류: 400/401/403/404/429), 5xx(서버 오류: 500/502/503).', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '프록시 서버(Forward Proxy vs Reverse Proxy)의 차이점은?', '네트워크', 21, '위치, 역할, Nginx 활용', 'Forward Proxy는 클라이언트 앞에서 접근 제어/캐싱/익명성을 제공합니다. Reverse Proxy는 서버 앞에서 로드 밸런싱/SSL 종료/캐싱을 담당합니다. Nginx는 대표적인 Reverse Proxy입니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'TCP Keep-Alive와 HTTP Keep-Alive의 차이점은?', '네트워크', 22, 'TCP 연결 유효성 확인 vs HTTP 연결 재사용', 'TCP Keep-Alive는 유휴 연결의 유효성을 확인하는 프로브 패킷입니다. HTTP Keep-Alive는 하나의 TCP 연결에서 여러 HTTP 요청/응답을 처리하는 기능입니다. HTTP/1.1에서 기본 활성화됩니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'gRPC와 REST의 차이점을 설명해주세요.', '네트워크', 23, 'Protocol Buffers vs JSON, HTTP/2 기반, 양방향 스트리밍', 'REST는 JSON 기반으로 가독성이 좋고 범용적입니다. gRPC는 Protocol Buffers와 HTTP/2 기반으로 빠르고 양방향 스트리밍을 지원합니다. 내부 통신에는 gRPC, 외부 API에는 REST가 주로 사용됩니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'SSL/TLS Handshake 과정을 설명해주세요.', '네트워크', 24, 'Client/Server Hello, 인증서 검증, 키 교환, 대칭키 생성', 'Client Hello → Server Hello(인증서 전송) → 인증서 CA 검증 → 키 교환(Pre-Master Secret 또는 ECDHE) → 대칭키 생성 → 이후 대칭키로 암호화 통신. 비대칭키는 키 교환에만 사용합니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '소켓(Socket) 통신의 동작 과정을 설명해주세요.', '네트워크', 25, 'socket→bind→listen→accept/connect→read/write→close', '서버: socket() → bind() → listen() → accept(). 클라이언트: socket() → connect(). 연결 후 read()/write()로 통신하고 close()로 종료합니다. accept() 시 새로운 소켓을 생성하여 다중 클라이언트를 처리합니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'HTTP 캐시의 동작 원리(ETag, Cache-Control)를 설명해주세요.', '네트워크', 26, 'Cache-Control 디렉티브, ETag/If-None-Match, 304 Not Modified', 'Cache-Control로 캐시 정책을 제어합니다(max-age, no-cache, no-store). ETag는 리소스 고유 식별자로, If-None-Match 헤더로 재검증하여 변경 없으면 304 Not Modified를 반환합니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'SYN Flood 공격이란 무엇이며 어떻게 방어하나요?', '네트워크', 27, 'TCP Handshake 악용, Half-Open 자원 고갈, SYN Cookie', 'SYN Flood는 대량의 SYN 패킷으로 서버의 Half-Open 연결 자원을 고갈시키는 DDoS 공격입니다. SYN Cookie, Rate Limiting, 방화벽 필터링으로 방어합니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'OAuth 2.0의 동작 흐름을 설명해주세요.', '네트워크', 28, 'Authorization Code Grant, Code→Token 교환', 'Authorization Code Grant: 사용자를 인가 서버로 리다이렉트 → 인증 후 Code 발급 → 서버 간 통신으로 Access Token 교환 → Token으로 리소스 서버 접근. Code를 중간에 두어 토큰이 브라우저에 노출되지 않습니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '네트워크에서 TTL(Time To Live)이란 무엇인가요?', '네트워크', 29, 'IP 패킷 루프 방지, 라우터 통과 시 감소, traceroute', 'TTL은 패킷의 무한 순환을 방지합니다. 라우터 통과 시 1씩 감소하며 0이 되면 폐기됩니다. traceroute는 TTL을 점진적으로 증가시켜 경유 라우터를 파악합니다. DNS에서의 TTL은 캐시 유효 시간입니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'HTTP/3와 QUIC 프로토콜에 대해 설명해주세요.', '네트워크', 30, 'UDP 기반, HOL Blocking 해결, TLS 1.3 내장, 0-RTT', 'HTTP/3는 UDP 기반의 QUIC 위에서 동작합니다. TCP의 HOL Blocking을 해결하고, TLS 1.3을 내장하여 0-RTT 재연결이 가능하며, 연결 마이그레이션으로 네트워크 변경 시에도 연결이 유지됩니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

-- ============================================================
-- Vol.2 데이터베이스 (15문항 추가)
-- ============================================================

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '클러스터드 인덱스와 논클러스터드 인덱스의 차이점은?', '데이터베이스', 16, '물리 정렬 여부, 테이블당 1개, PK=클러스터드(InnoDB)', '클러스터드 인덱스는 데이터가 인덱스 순서대로 물리적으로 정렬됩니다. 테이블당 하나만 가능하며 MySQL InnoDB에서는 PK가 클러스터드 인덱스입니다. 논클러스터드 인덱스는 여러 개 생성 가능하지만 추가 I/O가 발생합니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '커버링 인덱스(Covering Index)란 무엇인가요?', '데이터베이스', 17, 'Using index, 테이블 접근 불필요, 디스크 I/O 감소', '커버링 인덱스는 쿼리에 필요한 모든 컬럼이 인덱스에 포함되어 테이블 접근 없이 인덱스만으로 처리하는 것입니다. EXPLAIN에서 Using index로 표시됩니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '복합 인덱스에서 컬럼 순서가 중요한 이유는?', '데이터베이스', 18, '최좌선 접두사 규칙, 카디널리티 고려, 범위 조건', '복합 인덱스(A, B, C)에서 A 단독, A+B, A+B+C 조건에는 인덱스가 사용되지만 B 단독이나 B+C에는 사용되지 않습니다(최좌선 접두사 규칙). 카디널리티가 높은 컬럼을 앞에 배치해야 합니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '낙관적 락 vs 비관적 락의 차이점은?', '데이터베이스', 19, 'SELECT FOR UPDATE vs @Version, 충돌 빈도에 따른 선택', '비관적 락은 접근 시 즉시 락을 걸고(SELECT FOR UPDATE), 낙관적 락은 커밋 시 version으로 충돌을 감지합니다. 충돌이 빈번하면 비관적, 적으면 낙관적 락이 적합합니다. JPA에서는 @Version으로 구현합니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'WAL(Write-Ahead Logging)이란 무엇인가요?', '데이터베이스', 20, '변경 전 로그 기록, Redo/Undo, 순차 쓰기 성능, Durability', 'WAL은 데이터 변경 전에 로그를 먼저 기록하여 장애 시 Redo/Undo로 복구하는 기법입니다. 순차 쓰기여서 빠르고 ACID Durability를 보장합니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '데이터베이스의 뷰(View)란 무엇이며 왜 사용하나요?', '데이터베이스', 21, '가상 테이블, 쿼리 단순화, 보안, Materialized View', '뷰는 SELECT 결과를 테이블처럼 사용하는 가상 테이블입니다. 복잡한 쿼리를 단순화하고 보안을 강화합니다. Materialized View는 결과를 물리적으로 저장하여 읽기 성능을 향상시킵니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '데이터베이스에서 Deadlock이 발생하는 예시와 해결 방법은?', '데이터베이스', 22, '레코드 교차 락, InnoDB 자동 감지, 접근 순서 일관', '트랜잭션 A와 B가 서로의 레코드 락을 대기하면 데드락 발생. InnoDB는 자동 감지 후 비용이 적은 트랜잭션을 롤백합니다. 자원 접근 순서를 일관되게 유지하고 트랜잭션을 짧게 유지하여 예방합니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'EXPLAIN 실행 계획의 주요 항목들을 설명해주세요.', '데이터베이스', 23, 'type(const~ALL), rows, key, Extra', 'type은 const > eq_ref > ref > range > index > ALL 순으로 성능이 좋습니다. key는 사용된 인덱스, Extra의 Using index는 커버링 인덱스, Using filesort는 추가 정렬을 의미합니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Stored Procedure의 장단점을 설명해주세요.', '데이터베이스', 24, '네트워크 절감, 실행 계획 캐싱, DB 종속성, 디버깅 어려움', '장점: 네트워크 트래픽 감소, 실행 계획 캐싱, 보안. 단점: DB 종속, 유지보수 어려움, 디버깅 힘듦. 최근에는 애플리케이션 레벨에서 로직을 처리하는 추세입니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Soft Delete vs Hard Delete의 차이점과 고려사항은?', '데이터베이스', 25, 'is_deleted 플래그, 복구 가능, WHERE 조건 추가, JPA @SQLDelete', 'Hard Delete는 실제 삭제, Soft Delete는 is_deleted 플래그로 논리적 삭제입니다. Soft Delete는 복구 가능하지만 모든 쿼리에 조건 추가가 필요하고 테이블 크기가 증가합니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '데이터베이스 정규화에서 발생할 수 있는 이상 3가지는?', '데이터베이스', 26, '삽입/갱신/삭제 이상, 구체적 예시', '삽입 이상: 불필요한 데이터를 함께 입력해야 삽입 가능. 갱신 이상: 중복 데이터 중 일부만 수정되어 불일치 발생. 삭제 이상: 의도치 않은 다른 정보까지 삭제됩니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '분산 트랜잭션과 2PC를 설명해주세요.', '데이터베이스', 27, 'Prepare/Commit Phase, 코디네이터 장애, Saga 패턴', '2PC: Prepare 단계에서 참여자에게 커밋 가능 여부를 질의하고, 모두 OK하면 Commit, 하나라도 실패하면 Abort합니다. 코디네이터 장애 시 블로킹 발생. Saga 패턴으로 보완합니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'CAP 이론이란 무엇인가요?', '데이터베이스', 28, 'C/A/P 정의, CP vs AP 선택, 대표 DB', 'CAP 이론은 분산 시스템에서 Consistency, Availability, Partition Tolerance를 동시에 만족할 수 없다는 이론입니다. 실질적으로 CP(MongoDB)와 AP(Cassandra) 중 선택합니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '인덱스를 사용해도 성능이 안 나오는 경우는?', '데이터베이스', 29, '함수 적용, 묵시적 형변환, LIKE 와일드카드, 낮은 카디널리티', '인덱스 컬럼에 함수 적용, 묵시적 형변환, LIKE 앞 와일드카드, 낮은 카디널리티, 테이블 대부분 조회 시 인덱스가 무시될 수 있습니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());

INSERT INTO question_pool (cache_key, content, category, question_order, evaluation_criteria, model_answer, reference_type, follow_up_strategy, quality_score, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '페이지네이션에서 Offset vs Cursor 방식의 차이점은?', '데이터베이스', 30, 'Offset 성능 저하, Cursor 일정 성능, 대용량 권장', 'Offset 방식은 뒤로 갈수록 성능이 저하됩니다. Cursor 방식(WHERE id > last_id)은 어떤 페이지든 일정 성능을 보장하며 대용량 데이터에서 권장됩니다.', 'MODEL_ANSWER', 'PREPARED', 1.00, TRUE, NOW());
