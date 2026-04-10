-- CS_FUNDAMENTAL JUNIOR 시드 데이터 (120문항: 자료구조30, 운영체제30, 네트워크30, 데이터베이스30)

-- ============================================================
-- 자료구조 (30문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Array와 LinkedList의 차이점은 무엇인가요?', '자료구조',
 'Array는 연속된 메모리 공간에 데이터를 저장하여 인덱스로 O(1) 접근이 가능하지만, 중간 삽입/삭제 시 O(n)입니다. LinkedList는 각 노드가 다음 노드의 참조를 갖는 구조로 삽입/삭제가 O(1)이지만 탐색에 O(n)이 걸립니다. 캐시 지역성 측면에서는 Array가 유리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Stack과 Queue의 차이점과 각각의 활용 사례를 설명해주세요.', '자료구조',
 'Stack은 LIFO(Last In First Out) 구조로 함수 호출 스택, 뒤로가기 기능, 괄호 검사 등에 사용됩니다. Queue는 FIFO(First In First Out) 구조로 BFS 탐색, 작업 스케줄링, 메시지 큐 등에 활용됩니다. Java에서는 Stack 대신 Deque 사용이 권장됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'HashMap의 동작 원리를 설명해주세요.', '자료구조',
 'HashMap은 key의 hashCode()를 기반으로 버킷 인덱스를 결정하고 key-value 쌍을 저장합니다. 해시 충돌 시 Java 8부터는 버킷 내 노드가 8개를 초과하면 Red-Black Tree로 변환하여 O(log n)으로 개선합니다. 기본 load factor는 0.75이며 이를 초과하면 리사이징이 발생합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'HashTable과 HashMap의 차이점은 무엇인가요?', '자료구조',
 'HashTable은 모든 메서드가 synchronized로 Thread-Safe하지만 성능이 낮고, null key/value를 허용하지 않습니다. HashMap은 비동기로 동작하여 성능이 우수하고 null key 1개와 null value를 허용합니다. 멀티스레드 환경에서는 ConcurrentHashMap 사용이 권장됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '이진 탐색 트리(BST)의 특징과 시간 복잡도를 설명해주세요.', '자료구조',
 'BST는 왼쪽 자식이 부모보다 작고 오른쪽 자식이 부모보다 큰 이진 트리입니다. 균형 잡힌 경우 탐색, 삽입, 삭제 모두 O(log n)이지만, 편향된 경우 O(n)까지 성능이 저하됩니다. 이를 해결하기 위해 AVL 트리나 Red-Black 트리 같은 자가 균형 트리가 사용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Heap 자료구조의 특징과 활용 사례를 설명해주세요.', '자료구조',
 'Heap은 완전 이진 트리 기반으로, Max Heap은 부모가 자식보다 크고 Min Heap은 부모가 자식보다 작습니다. 삽입과 삭제 모두 O(log n)이며, 최대/최소값 조회는 O(1)입니다. 우선순위 큐 구현, 힙 정렬, 다익스트라 알고리즘 등에 활용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Graph를 표현하는 두 가지 방법을 비교해주세요.', '자료구조',
 '인접 행렬은 2차원 배열로 간선을 표현하며 O(1)로 연결 여부를 확인할 수 있지만 공간 복잡도가 O(V²)입니다. 인접 리스트는 각 정점의 연결 정점을 리스트로 저장하여 공간 복잡도가 O(V+E)로 효율적입니다. 밀집 그래프는 인접 행렬, 희소 그래프는 인접 리스트가 유리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '해시 충돌(Hash Collision)이란 무엇이고 해결 방법은?', '자료구조',
 '해시 충돌은 서로 다른 키가 같은 해시값을 가져 같은 버킷에 매핑되는 현상입니다. 해결 방법으로는 Chaining(연결 리스트로 같은 버킷에 저장)과 Open Addressing(선형/이차 탐사, 이중 해싱으로 다른 빈 버킷 탐색)이 있습니다. Java HashMap은 Chaining 방식을 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '버블 정렬, 선택 정렬, 삽입 정렬의 차이점을 설명해주세요.', '자료구조',
 '버블 정렬은 인접 요소를 비교·교환하며 O(n²)입니다. 선택 정렬은 최소값을 찾아 앞으로 이동시키며 O(n²)이고 교환 횟수가 적습니다. 삽입 정렬은 정렬된 부분에 새 요소를 삽입하며, 거의 정렬된 데이터에서 O(n)으로 효율적입니다. 세 알고리즘 모두 공간 복잡도는 O(1)입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '시간 복잡도와 공간 복잡도의 개념을 설명해주세요.', '자료구조',
 '시간 복잡도는 입력 크기에 따른 알고리즘의 실행 시간 증가율을, 공간 복잡도는 메모리 사용량의 증가율을 나타냅니다. Big-O 표기법으로 최악의 경우를 표현하며, O(1) < O(log n) < O(n) < O(n log n) < O(n²) 순으로 증가합니다. 일반적으로 시간과 공간은 트레이드오프 관계입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '퀵 정렬(Quick Sort)의 동작 원리와 시간 복잡도를 설명해주세요.', '자료구조',
 '퀵 정렬은 피벗을 선택하고 피벗보다 작은 요소와 큰 요소로 분할한 뒤 재귀적으로 정렬합니다. 평균 시간 복잡도는 O(n log n)이지만, 이미 정렬된 배열에서 최악의 경우 O(n²)입니다. 피벗 선택 전략(중간값, 랜덤)으로 최악의 경우를 회피할 수 있으며, 실무에서 가장 많이 사용되는 정렬 알고리즘입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '병합 정렬(Merge Sort)의 동작 원리와 특징을 설명해주세요.', '자료구조',
 '병합 정렬은 배열을 반으로 나누고 각각 정렬한 뒤 병합하는 분할 정복 알고리즘입니다. 항상 O(n log n)의 안정적인 시간 복잡도를 가지며 안정 정렬(Stable Sort)입니다. 단점은 추가 메모리 O(n)이 필요하다는 것이며, LinkedList 정렬에 특히 적합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Deque(Double-Ended Queue)란 무엇인가요?', '자료구조',
 'Deque는 양쪽 끝에서 삽입과 삭제가 가능한 자료구조입니다. Stack과 Queue의 기능을 모두 포함하며 Java에서는 ArrayDeque 구현체가 권장됩니다. 앞/뒤 삽입·삭제 모두 O(1)이며, 슬라이딩 윈도우 알고리즘이나 작업 스케줄링에 활용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Tree와 Graph의 차이점은 무엇인가요?', '자료구조',
 'Tree는 사이클이 없는 연결 그래프로, 하나의 루트 노드에서 시작하여 계층 구조를 이룹니다. 간선 수는 항상 노드 수 - 1이며, 부모-자식 관계가 존재합니다. Graph는 사이클이 있을 수 있고 방향/무방향이 있으며, 노드 간 자유로운 연결이 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'ArrayList와 LinkedList를 어떤 상황에서 각각 사용해야 하나요?', '자료구조',
 'ArrayList는 인덱스 기반 접근이 빈번하고 데이터 크기가 예측 가능할 때 적합합니다. LinkedList는 삽입/삭제가 빈번하고 순차 접근 위주일 때 유리합니다. 하지만 실무에서는 캐시 지역성과 오버헤드를 고려하면 대부분 ArrayList가 더 나은 성능을 보입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Set 자료구조의 특징과 구현체 종류를 설명해주세요.', '자료구조',
 'Set은 중복을 허용하지 않는 자료구조입니다. HashSet은 해시 테이블 기반으로 O(1) 탐색이 가능하고, TreeSet은 Red-Black Tree 기반으로 정렬된 상태를 유지하며 O(log n)입니다. LinkedHashSet은 삽입 순서를 유지합니다. 중복 검사가 필요한 상황에서 주로 사용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Priority Queue의 개념과 구현 방법을 설명해주세요.', '자료구조',
 'Priority Queue는 우선순위가 높은 요소가 먼저 나오는 자료구조입니다. 일반적으로 Heap으로 구현하며, 삽입과 삭제가 O(log n)입니다. Java에서는 PriorityQueue 클래스를 사용하고, 기본적으로 Min Heap으로 동작합니다. 작업 스케줄링, 다익스트라 알고리즘 등에 활용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'DFS와 BFS의 차이점과 각각의 활용 사례를 설명해주세요.', '자료구조',
 'DFS(깊이 우선 탐색)는 한 경로를 끝까지 탐색한 뒤 백트래킹하며, Stack이나 재귀로 구현합니다. BFS(너비 우선 탐색)는 같은 깊이의 노드를 먼저 탐색하며 Queue로 구현합니다. DFS는 경로 탐색, 사이클 검출에, BFS는 최단 경로 탐색에 적합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '이진 트리의 순회 방법(전위, 중위, 후위)을 설명해주세요.', '자료구조',
 '전위 순회(Preorder)는 루트→왼쪽→오른쪽 순서로 트리 복사에 사용됩니다. 중위 순회(Inorder)는 왼쪽→루트→오른쪽 순서로 BST에서 정렬된 결과를 얻을 수 있습니다. 후위 순회(Postorder)는 왼쪽→오른쪽→루트 순서로 트리 삭제에 사용됩니다. 레벨 순회는 BFS를 이용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '재귀(Recursion)의 장단점과 주의사항을 설명해주세요.', '자료구조',
 '재귀는 함수가 자기 자신을 호출하는 기법으로, 코드가 간결하고 트리/그래프 탐색에 직관적입니다. 단점으로는 호출 스택 메모리 사용, 스택 오버플로 위험, 반복문 대비 오버헤드가 있습니다. 꼬리 재귀 최적화(TCO)나 메모이제이션으로 성능을 개선할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '안정 정렬(Stable Sort)과 불안정 정렬의 차이는 무엇인가요?', '자료구조',
 '안정 정렬은 같은 값을 가진 요소들의 원래 순서가 정렬 후에도 유지되는 알고리즘입니다. 병합 정렬, 삽입 정렬, 버블 정렬이 안정 정렬이며, 퀵 정렬과 힙 정렬은 불안정 정렬입니다. 다중 기준 정렬 시 안정 정렬이 중요하며, Java의 Arrays.sort()는 기본형에 퀵 정렬, 객체에 TimSort(안정)를 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Map 자료구조에서 key의 조건은 무엇인가요?', '자료구조',
 'Map의 key는 유일해야 하며, equals()와 hashCode()를 올바르게 구현해야 합니다. 두 객체가 equals()로 같으면 hashCode()도 같아야 합니다. 가변 객체를 key로 사용하면 해시값이 변경되어 데이터를 찾지 못할 수 있으므로 불변 객체(String, Integer 등)를 사용하는 것이 안전합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '배열에서 특정 요소를 탐색하는 방법들을 비교해주세요.', '자료구조',
 '선형 탐색은 처음부터 순차적으로 비교하며 O(n)입니다. 이진 탐색은 정렬된 배열에서 중간값과 비교하며 O(log n)입니다. 해시 기반 탐색은 O(1)이지만 추가 공간이 필요합니다. 정렬 여부와 데이터 크기에 따라 적절한 탐색 방법을 선택해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Trie 자료구조의 개념과 활용처를 설명해주세요.', '자료구조',
 'Trie는 문자열 집합을 효율적으로 저장하고 탐색하는 트리 자료구조입니다. 각 노드가 문자 하나를 나타내며, 문자열 길이 m에 대해 O(m)으로 탐색이 가능합니다. 자동 완성, 사전 구현, IP 라우팅 테이블 등에 활용됩니다. 공간 사용이 많다는 단점이 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Java에서 Collections.sort()와 Arrays.sort()의 차이점은?', '자료구조',
 'Arrays.sort()는 기본형 배열에 Dual-Pivot Quick Sort(불안정)를, 객체 배열에 TimSort(안정)를 사용합니다. Collections.sort()는 내부적으로 List를 배열로 변환한 후 Arrays.sort()를 호출하므로 TimSort를 사용합니다. TimSort는 병합 정렬과 삽입 정렬의 하이브리드로 최선 O(n), 최악 O(n log n)입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'ConcurrentHashMap이란 무엇이고 왜 사용하나요?', '자료구조',
 'ConcurrentHashMap은 멀티스레드 환경에서 안전하게 사용할 수 있는 HashMap입니다. HashTable과 달리 버킷 단위로 락을 걸어(Java 8에서는 CAS + synchronized) 높은 동시성을 제공합니다. null key/value를 허용하지 않으며, 읽기 작업에는 락이 필요 없어 성능이 우수합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'LinkedList에서 사이클 검출 방법을 설명해주세요.', '자료구조',
 'Floyd의 Tortoise and Hare 알고리즘을 사용합니다. 두 포인터를 두고, 느린 포인터는 한 칸씩, 빠른 포인터는 두 칸씩 이동합니다. 사이클이 있으면 두 포인터가 반드시 만나고, 없으면 빠른 포인터가 null에 도달합니다. 시간 O(n), 공간 O(1)로 효율적입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Queue를 Stack 두 개로 구현하는 방법을 설명해주세요.', '자료구조',
 'in 스택과 out 스택을 사용합니다. enqueue 시 in 스택에 push하고, dequeue 시 out 스택이 비어있으면 in의 모든 요소를 out으로 옮긴 후 pop합니다. 이렇게 하면 amortized O(1)의 시간 복잡도로 Queue를 구현할 수 있습니다. 각 요소가 최대 2번 이동하기 때문입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '동적 프로그래밍(DP)의 개념과 조건을 설명해주세요.', '자료구조',
 '동적 프로그래밍은 큰 문제를 작은 하위 문제로 나누고 결과를 저장하여 재사용하는 기법입니다. 최적 부분 구조(Optimal Substructure)와 겹치는 부분 문제(Overlapping Subproblems) 두 조건이 필요합니다. Top-down(메모이제이션)과 Bottom-up(타뷸레이션) 방식이 있으며, 피보나치, 배낭 문제 등에 활용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- ============================================================
-- 운영체제 (30문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '프로세스와 스레드의 차이점을 설명해주세요.', '운영체제',
 '프로세스는 독립적인 메모리 공간(코드, 데이터, 힙, 스택)을 가진 실행 단위입니다. 스레드는 프로세스 내에서 코드, 데이터, 힙을 공유하고 스택만 별도로 가집니다. 프로세스 간 통신은 IPC가 필요하지만, 스레드 간 통신은 공유 메모리로 가능하여 더 빠릅니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '프로세스의 상태와 전이 과정을 설명해주세요.', '운영체제',
 '프로세스는 생성(New), 준비(Ready), 실행(Running), 대기(Waiting), 종료(Terminated) 상태를 가집니다. Ready에서 CPU를 할당받으면 Running, I/O 요청 시 Waiting, 타임 슬라이스 만료 시 Ready로 전이됩니다. 이 상태 전이는 OS 스케줄러에 의해 관리됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'PCB(Process Control Block)란 무엇인가요?', '운영체제',
 'PCB는 운영체제가 프로세스를 관리하기 위한 정보를 저장하는 자료구조입니다. 프로세스 ID, 상태, 프로그램 카운터, CPU 레지스터, 메모리 관리 정보, 스케줄링 정보, I/O 상태 등을 포함합니다. 컨텍스트 스위칭 시 현재 프로세스의 PCB를 저장하고 다음 프로세스의 PCB를 로드합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '컨텍스트 스위칭(Context Switching)이란 무엇인가요?', '운영체제',
 '컨텍스트 스위칭은 CPU가 현재 프로세스를 중단하고 다른 프로세스를 실행하는 과정입니다. 현재 프로세스의 상태를 PCB에 저장하고 다음 프로세스의 상태를 PCB에서 로드합니다. 이 과정에서 오버헤드가 발생하며, 캐시 무효화가 성능 저하의 주요 원인입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '멀티프로세스와 멀티스레드의 차이점을 설명해주세요.', '운영체제',
 '멀티프로세스는 각 프로세스가 독립 메모리를 가져 안정적이지만 통신 비용이 높고 메모리 사용량이 많습니다. 멀티스레드는 메모리를 공유하여 통신이 빠르고 경량이지만, 동기화 문제가 발생할 수 있습니다. 웹 서버는 주로 멀티스레드, 크롬 브라우저는 멀티프로세스를 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '메모리 계층 구조를 설명해주세요.', '운영체제',
 '레지스터 → L1/L2/L3 캐시 → RAM(주기억장치) → SSD/HDD(보조기억장치) 순으로 속도가 느려지고 용량이 커집니다. CPU는 가까운 메모리부터 접근하며, 캐시 히트율이 성능에 큰 영향을 미칩니다. 이 구조는 자주 사용되는 데이터를 빠른 메모리에 유지하는 지역성(Locality) 원리를 활용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '사용자 모드와 커널 모드의 차이점은 무엇인가요?', '운영체제',
 '사용자 모드는 일반 응용 프로그램이 실행되는 모드로, 하드웨어에 직접 접근할 수 없습니다. 커널 모드는 OS가 실행되는 모드로 모든 자원에 접근 가능합니다. 시스템 콜을 통해 사용자 모드에서 커널 모드로 전환되며, 이는 보안과 안정성을 위한 보호 메커니즘입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '인터럽트(Interrupt)의 종류와 처리 과정을 설명해주세요.', '운영체제',
 '인터럽트는 하드웨어 인터럽트(I/O 완료, 타이머)와 소프트웨어 인터럽트(시스템 콜, 예외)로 나뉩니다. 인터럽트 발생 시 CPU는 현재 작업을 중단하고, 인터럽트 벡터 테이블에서 핸들러를 찾아 실행합니다. 처리 후 저장된 상태를 복원하여 원래 작업을 계속합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '시스템 콜(System Call)이란 무엇인가요?', '운영체제',
 '시스템 콜은 응용 프로그램이 운영체제의 커널 기능을 요청하는 인터페이스입니다. 파일 I/O(open, read, write), 프로세스 관리(fork, exec), 메모리 할당(mmap) 등이 있습니다. 사용자 모드에서 커널 모드로 전환되어 실행되며, 직접 하드웨어 접근을 막아 시스템을 보호합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'CPU 스케줄링이란 무엇이고 왜 필요한가요?', '운영체제',
 'CPU 스케줄링은 Ready Queue의 프로세스 중 다음에 CPU를 할당할 프로세스를 결정하는 과정입니다. 한정된 CPU 자원을 효율적으로 활용하고, 응답 시간을 최소화하며, 처리량을 극대화하기 위해 필요합니다. 선점형(Round Robin, SRTF)과 비선점형(FCFS, SJF) 스케줄링으로 나뉩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '데드락(Deadlock)이란 무엇인가요?', '운영체제',
 '데드락은 두 개 이상의 프로세스가 서로가 점유한 자원을 기다리며 무한히 대기하는 상태입니다. 상호 배제, 점유 대기, 비선점, 순환 대기 4가지 조건이 모두 만족될 때 발생합니다. 예방(조건 제거), 회피(은행원 알고리즘), 탐지 및 복구, 무시(타조 알고리즘) 방법으로 해결합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '가상 메모리(Virtual Memory)의 개념을 설명해주세요.', '운영체제',
 '가상 메모리는 물리 메모리보다 큰 주소 공간을 프로세스에 제공하는 기법입니다. 각 프로세스는 독립적인 가상 주소 공간을 가지며, 페이지 테이블을 통해 물리 주소로 변환됩니다. 필요한 페이지만 물리 메모리에 적재하는 요구 페이징(Demand Paging)으로 메모리를 효율적으로 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '페이징(Paging)이란 무엇인가요?', '운영체제',
 '페이징은 가상 메모리를 동일한 크기의 페이지로, 물리 메모리를 동일한 크기의 프레임으로 나누는 메모리 관리 기법입니다. 페이지 테이블로 가상→물리 주소를 매핑하며, 외부 단편화를 제거합니다. 일반적으로 페이지 크기는 4KB이며, 내부 단편화가 발생할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '내부 단편화와 외부 단편화의 차이를 설명해주세요.', '운영체제',
 '내부 단편화는 할당된 메모리 블록 내에서 사용되지 않는 공간이 발생하는 것입니다. 외부 단편화는 빈 공간의 총합은 충분하지만 연속되지 않아 할당할 수 없는 상태입니다. 페이징은 외부 단편화를 해결하고, 세그먼테이션은 내부 단편화를 해결합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '캐시 메모리의 지역성(Locality) 원리를 설명해주세요.', '운영체제',
 '시간적 지역성(Temporal Locality)은 최근 접근한 데이터에 다시 접근할 확률이 높다는 원리이고, 공간적 지역성(Spatial Locality)은 접근한 데이터 근처 데이터에 접근할 확률이 높다는 원리입니다. 이 원리 덕분에 캐시 히트율이 높아지고 메모리 접근 성능이 향상됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'fork()와 exec()의 차이를 설명해주세요.', '운영체제',
 'fork()는 현재 프로세스를 복제하여 자식 프로세스를 생성합니다. 부모와 자식은 동일한 코드를 가지지만 독립적인 메모리 공간을 가집니다. exec()는 현재 프로세스의 메모리를 새 프로그램으로 교체합니다. 일반적으로 fork() 후 자식에서 exec()를 호출하여 새 프로그램을 실행합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '동기(Synchronous)와 비동기(Asynchronous)의 차이를 설명해주세요.', '운영체제',
 '동기는 작업이 완료될 때까지 호출자가 대기하는 방식이고, 비동기는 작업을 요청한 후 완료를 기다리지 않고 다른 작업을 수행하는 방식입니다. 비동기는 콜백, Future/Promise, 이벤트 등으로 결과를 전달받습니다. I/O 작업에서 비동기 방식이 CPU 활용률을 높일 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Blocking과 Non-blocking의 차이를 설명해주세요.', '운영체제',
 'Blocking은 호출된 함수가 작업이 완료될 때까지 제어권을 반환하지 않는 방식입니다. Non-blocking은 호출 즉시 제어권을 반환하며 작업 완료 여부와 무관합니다. 동기/비동기는 작업 완료 통보 방식에 관한 것이고, Blocking/Non-blocking은 제어권 반환에 관한 것으로 독립적인 개념입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '세마포어(Semaphore)의 개념을 설명해주세요.', '운영체제',
 '세마포어는 공유 자원에 대한 접근을 제어하는 동기화 도구로, 카운터 값으로 동시 접근 가능한 스레드 수를 제한합니다. 이진 세마포어(0 또는 1)는 뮤텍스와 유사하게 상호 배제를 구현하고, 카운팅 세마포어는 여러 자원에 대한 접근을 제어합니다. P(wait)와 V(signal) 연산으로 동작합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '뮤텍스(Mutex)란 무엇이고 세마포어와 어떻게 다른가요?', '운영체제',
 '뮤텍스는 상호 배제를 위한 락으로, 한 번에 하나의 스레드만 임계 영역에 접근할 수 있습니다. 세마포어와 달리 뮤텍스는 소유권 개념이 있어 락을 획득한 스레드만 해제할 수 있습니다. 세마포어는 카운팅이 가능하여 여러 스레드의 동시 접근을 허용할 수 있지만, 뮤텍스는 항상 1개입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '임계 영역(Critical Section)이란 무엇인가요?', '운영체제',
 '임계 영역은 여러 스레드가 동시에 접근하면 데이터 일관성 문제가 발생할 수 있는 코드 영역입니다. 상호 배제(하나의 스레드만 진입), 진행(대기 중인 스레드가 진입 가능), 한정 대기(무한 대기 방지) 3가지 조건을 만족해야 합니다. 뮤텍스, 세마포어, 모니터 등으로 보호합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Race Condition이란 무엇인가요?', '운영체제',
 'Race Condition은 두 개 이상의 스레드가 공유 자원에 동시에 접근하여 실행 순서에 따라 결과가 달라지는 현상입니다. 예를 들어 두 스레드가 동시에 변수를 증가시키면 하나의 업데이트가 손실될 수 있습니다. 뮤텍스, 세마포어, synchronized 같은 동기화 메커니즘으로 방지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '페이지 폴트(Page Fault)란 무엇인가요?', '운영체제',
 '페이지 폴트는 프로세스가 접근하려는 페이지가 물리 메모리에 없을 때 발생하는 인터럽트입니다. 발생 시 OS가 디스크에서 해당 페이지를 물리 메모리로 로드하고, 빈 프레임이 없으면 페이지 교체 알고리즘(LRU 등)으로 교체합니다. 잦은 페이지 폴트는 Thrashing을 유발합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Thrashing이란 무엇인가요?', '운영체제',
 'Thrashing은 프로세스가 실제 작업보다 페이지 교체에 더 많은 시간을 소비하는 현상입니다. 물리 메모리가 부족하여 페이지 폴트가 지속적으로 발생할 때 나타납니다. Working Set 모델로 프로세스가 필요한 최소 페이지 수를 보장하거나, 멀티프로그래밍 정도를 줄여 해결합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'IPC(Inter-Process Communication)의 종류를 설명해주세요.', '운영체제',
 'IPC는 프로세스 간 데이터를 주고받는 메커니즘입니다. 파이프(단방향), 소켓(네트워크), 공유 메모리(가장 빠름), 메시지 큐, 시그널 등이 있습니다. 공유 메모리는 동기화가 필요하고, 소켓은 네트워크를 통한 원격 통신도 가능합니다. 용도에 따라 적절한 방법을 선택합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Thread Pool의 개념과 장점을 설명해주세요.', '운영체제',
 'Thread Pool은 미리 일정 수의 스레드를 생성해두고 작업이 들어올 때 재사용하는 패턴입니다. 스레드 생성/소멸 비용을 줄이고, 동시 스레드 수를 제한하여 시스템 자원을 보호합니다. Java에서는 ExecutorService를 통해 사용하며, 작업 큐와 결합하여 비동기 처리에 활용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'FCFS와 Round Robin 스케줄링의 차이를 설명해주세요.', '운영체제',
 'FCFS(First Come First Served)는 도착 순서대로 처리하는 비선점형 알고리즘으로, 구현이 간단하지만 Convoy Effect가 발생할 수 있습니다. Round Robin은 각 프로세스에 동일한 타임 퀀텀을 할당하는 선점형 알고리즘으로 응답 시간이 균일합니다. 타임 퀀텀이 너무 작으면 컨텍스트 스위칭 오버헤드가 증가합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '스와핑(Swapping)이란 무엇인가요?', '운영체제',
 '스와핑은 물리 메모리가 부족할 때 프로세스 전체를 디스크의 스왑 영역으로 이동시키는 기법입니다. 현재 실행 중이지 않은 프로세스를 스왑 아웃하고, 필요할 때 다시 스왑 인합니다. 가상 메모리의 페이징과 달리 프로세스 단위로 동작하며, 메모리 부족 시 멀티프로그래밍을 유지하는 데 사용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- ============================================================
-- 네트워크 (30문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'OSI 7계층 모델을 설명해주세요.', '네트워크',
 'OSI 7계층은 물리(비트 전송), 데이터링크(프레임, MAC), 네트워크(패킷, IP 라우팅), 전송(TCP/UDP, 세그먼트), 세션(연결 관리), 표현(암호화/압축), 응용(HTTP, FTP) 계층입니다. 각 계층은 독립적으로 동작하며, 데이터는 송신 시 캡슐화되고 수신 시 역캡슐화됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'TCP와 UDP의 차이점을 설명해주세요.', '네트워크',
 'TCP는 연결 지향적이며 3-way Handshake로 연결을 설정하고, 신뢰성 있는 데이터 전송을 보장합니다. 흐름 제어와 혼잡 제어를 제공하며 순서를 보장합니다. UDP는 비연결 지향적으로 핸드셰이크 없이 전송하며, 신뢰성 보장 없이 빠른 전송이 가능합니다. 스트리밍, DNS, 게임 등에 UDP가 사용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'HTTP와 HTTPS의 차이점은 무엇인가요?', '네트워크',
 'HTTP는 평문으로 데이터를 전송하여 도청이 가능하고, HTTPS는 SSL/TLS 암호화를 적용하여 보안을 제공합니다. HTTPS는 기본 포트 443을 사용하며, 서버 인증서를 통해 신원을 검증합니다. 현대 웹에서는 SEO와 보안을 위해 HTTPS가 필수이며, HTTP/2는 HTTPS에서만 지원하는 브라우저가 많습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'DNS(Domain Name System)의 동작 과정을 설명해주세요.', '네트워크',
 '브라우저가 도메인을 입력하면 먼저 로컬 캐시를 확인하고, 없으면 DNS 리졸버에 질의합니다. 리졸버는 루트 DNS → TLD DNS(.com) → 권한 DNS 서버 순으로 재귀적으로 질의하여 IP 주소를 얻습니다. 결과는 TTL 동안 캐싱되어 이후 질의 속도를 높입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'IP 주소 체계(IPv4, IPv6)를 설명해주세요.', '네트워크',
 'IPv4는 32비트 주소(약 43억 개)로 점으로 구분된 4개의 8비트 숫자로 표현합니다(예: 192.168.0.1). IPv6는 128비트 주소로 IPv4 고갈 문제를 해결하며, 콜론으로 구분된 8개의 16비트 16진수로 표현합니다. NAT, 서브넷팅으로 IPv4 주소를 효율적으로 사용하고 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '포트(Port)의 개념과 Well-Known 포트를 설명해주세요.', '네트워크',
 '포트는 하나의 IP 주소에서 여러 서비스를 구분하기 위한 논리적 번호(0-65535)입니다. Well-Known 포트(0-1023)에는 HTTP(80), HTTPS(443), SSH(22), FTP(21), DNS(53) 등이 있습니다. 1024-49151은 등록 포트, 49152-65535는 동적/사설 포트로 클라이언트가 임시 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'HTTP 메서드의 종류와 용도를 설명해주세요.', '네트워크',
 'GET은 리소스 조회(멱등), POST는 리소스 생성, PUT은 리소스 전체 수정(멱등), PATCH는 부분 수정, DELETE는 리소스 삭제(멱등)입니다. HEAD는 헤더만 조회하고, OPTIONS는 지원 메서드를 확인합니다. 멱등성은 여러 번 호출해도 결과가 동일한 성질로, API 설계에서 중요합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'HTTP 상태 코드의 분류를 설명해주세요.', '네트워크',
 '1xx는 정보 응답, 2xx는 성공(200 OK, 201 Created, 204 No Content), 3xx는 리다이렉션(301 영구, 302 임시), 4xx는 클라이언트 오류(400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found), 5xx는 서버 오류(500 Internal Error, 502 Bad Gateway, 503 Service Unavailable)입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '쿠키(Cookie)와 세션(Session)의 차이점을 설명해주세요.', '네트워크',
 '쿠키는 클라이언트 브라우저에 저장되는 작은 데이터로, HTTP 요청 시 자동으로 전송됩니다. 세션은 서버 측에 저장되며 세션 ID를 쿠키로 전달합니다. 쿠키는 변조 위험이 있고, 세션은 서버 메모리를 사용합니다. 분산 환경에서는 세션 클러스터링이나 Redis 같은 외부 저장소를 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'ARP(Address Resolution Protocol)의 동작 원리를 설명해주세요.', '네트워크',
 'ARP는 IP 주소를 MAC 주소로 변환하는 프로토콜입니다. 송신 호스트가 ARP 요청(브로드캐스트)을 보내면, 해당 IP를 가진 호스트가 자신의 MAC 주소로 응답(유니캐스트)합니다. 결과는 ARP 캐시에 저장되어 재사용됩니다. 같은 네트워크 내에서만 동작하며, 라우터를 넘을 때는 라우터의 MAC이 사용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'TCP 3-way Handshake 과정을 설명해주세요.', '네트워크',
 '클라이언트가 SYN 패킷을 보내고(SYN_SENT), 서버가 SYN+ACK로 응답하며(SYN_RECEIVED), 클라이언트가 ACK를 보내면(ESTABLISHED) 연결이 수립됩니다. 이 과정에서 시퀀스 번호를 교환하여 양방향 통신이 가능해집니다. 이후 데이터 전송이 시작됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'TCP 4-way Handshake(연결 종료) 과정을 설명해주세요.', '네트워크',
 '클라이언트가 FIN을 보내고(FIN_WAIT_1), 서버가 ACK로 응답(CLOSE_WAIT)하면 클라이언트는 FIN_WAIT_2로 전환됩니다. 서버가 데이터 전송을 완료하고 FIN을 보내면, 클라이언트가 ACK로 응답(TIME_WAIT)합니다. TIME_WAIT 상태에서 일정 시간 후 연결이 완전히 종료됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '서브넷 마스크(Subnet Mask)란 무엇인가요?', '네트워크',
 '서브넷 마스크는 IP 주소에서 네트워크 부분과 호스트 부분을 구분하는 32비트 값입니다. 예를 들어 255.255.255.0(/24)은 앞 24비트가 네트워크, 뒤 8비트가 호스트입니다. 서브넷팅을 통해 하나의 네트워크를 더 작은 서브넷으로 나눠 효율적으로 IP를 할당하고 브로드캐스트 도메인을 줄입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'NAT(Network Address Translation)란 무엇인가요?', '네트워크',
 'NAT는 사설 IP를 공인 IP로 변환하여 인터넷에 접속할 수 있게 하는 기술입니다. 하나의 공인 IP로 여러 기기가 인터넷을 사용할 수 있어 IPv4 주소 부족 문제를 완화합니다. 외부에서 내부 네트워크로 직접 접근이 차단되어 보안 효과도 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'DHCP(Dynamic Host Configuration Protocol)의 역할을 설명해주세요.', '네트워크',
 'DHCP는 네트워크에 연결되는 기기에 자동으로 IP 주소, 서브넷 마스크, 게이트웨이, DNS 서버 주소를 할당하는 프로토콜입니다. Discover → Offer → Request → Acknowledge(DORA) 과정을 거칩니다. 수동 설정 없이 네트워크 구성을 자동화하여 관리 편의성을 높입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '라우터와 스위치의 차이점은 무엇인가요?', '네트워크',
 '스위치는 데이터 링크 계층(L2)에서 MAC 주소를 기반으로 같은 네트워크 내 프레임을 전달합니다. 라우터는 네트워크 계층(L3)에서 IP 주소를 기반으로 서로 다른 네트워크 간 패킷을 전달합니다. 라우터는 라우팅 테이블을 사용하여 최적 경로를 결정합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'GET과 POST 메서드의 차이점을 설명해주세요.', '네트워크',
 'GET은 데이터를 URL 파라미터로 전송하여 길이 제한이 있고 캐싱이 가능하며, 조회용으로 멱등성을 가집니다. POST는 HTTP Body에 데이터를 담아 전송하여 길이 제한이 없고, 생성/수정 등 서버 상태를 변경합니다. 보안상 민감한 데이터는 GET 대신 POST를 사용해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'REST API의 특징을 설명해주세요.', '네트워크',
 'REST는 자원(Resource)을 URI로 식별하고 HTTP 메서드(GET/POST/PUT/DELETE)로 행위를 표현하는 아키텍처 스타일입니다. Stateless(서버가 클라이언트 상태를 저장하지 않음), 캐시 가능, 균일한 인터페이스, 계층화 시스템이 핵심 원칙입니다. 직관적이고 확장 가능한 API 설계에 널리 사용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'CORS(Cross-Origin Resource Sharing)란 무엇인가요?', '네트워크',
 'CORS는 웹 브라우저가 다른 출처(Origin)의 리소스에 접근할 수 있도록 허용하는 메커니즘입니다. 기본적으로 동일 출처 정책(SOP)이 적용되어 다른 도메인의 요청이 차단됩니다. 서버가 Access-Control-Allow-Origin 헤더로 허용할 출처를 지정하며, Preflight 요청(OPTIONS)으로 사전 검증합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'WebSocket이란 무엇인가요?', '네트워크',
 'WebSocket은 클라이언트와 서버 간 양방향 실시간 통신을 가능하게 하는 프로토콜입니다. HTTP 핸드셰이크로 연결을 업그레이드한 후 지속적인 연결을 유지합니다. HTTP의 요청-응답 패턴과 달리 서버가 클라이언트에 능동적으로 데이터를 전송할 수 있어, 채팅, 실시간 알림, 게임 등에 사용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'CDN(Content Delivery Network)이란 무엇인가요?', '네트워크',
 'CDN은 전 세계에 분산된 서버 네트워크를 통해 사용자와 가까운 위치에서 콘텐츠를 제공하는 시스템입니다. 정적 파일(이미지, CSS, JS)을 엣지 서버에 캐싱하여 응답 속도를 높이고, 원본 서버의 부하를 줄입니다. CloudFront, Cloudflare 등이 대표적인 CDN 서비스입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'SSL/TLS의 역할과 동작 과정을 간략히 설명해주세요.', '네트워크',
 'SSL/TLS는 통신 데이터를 암호화하여 도청, 변조, 위장을 방지하는 보안 프로토콜입니다. 핸드셰이크 과정에서 서버 인증서 검증, 세션 키 교환이 이루어지며, 이후 대칭키로 데이터를 암호화합니다. HTTPS는 HTTP 위에 TLS를 적용한 것으로 현대 웹의 필수 보안 요소입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'URL을 입력하면 브라우저에서 어떤 일이 일어나나요?', '네트워크',
 'URL 입력 → DNS 질의로 IP 획득 → TCP 3-way Handshake → (HTTPS면 TLS Handshake) → HTTP 요청 전송 → 서버 처리 후 응답 → HTML 파싱 → DOM 트리 구성 → CSS/JS 로드 → 렌더 트리 구성 → 페인팅 순서로 진행됩니다. 캐시, CDN, 로드 밸런서 등이 중간에 개입할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'VPN(Virtual Private Network)이란 무엇인가요?', '네트워크',
 'VPN은 공용 네트워크를 통해 가상의 사설 네트워크를 구성하는 기술입니다. 터널링과 암호화를 통해 데이터를 보호하며, 원격에서 회사 내부 네트워크에 안전하게 접속할 수 있습니다. IPSec, SSL VPN 등의 프로토콜을 사용하며, 지리적 제한 우회에도 활용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '프록시 서버(Proxy Server)의 역할을 설명해주세요.', '네트워크',
 '프록시 서버는 클라이언트와 서버 사이에서 중계 역할을 하는 서버입니다. 포워드 프록시는 클라이언트를 대신하여 요청하고(캐싱, 접근 제어), 리버스 프록시는 서버를 대신하여 응답합니다(로드 밸런싱, SSL 종료, 보안). Nginx, HAProxy가 대표적인 리버스 프록시입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '로드 밸런싱(Load Balancing)이란 무엇인가요?', '네트워크',
 '로드 밸런싱은 들어오는 트래픽을 여러 서버에 분산하여 처리 능력을 높이는 기술입니다. Round Robin, Least Connections, IP Hash 등의 알고리즘이 있습니다. L4(전송 계층, TCP/UDP 기반)와 L7(응용 계층, HTTP 기반) 로드 밸런서가 있으며, 고가용성과 확장성을 제공합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'HTTP Keep-Alive란 무엇인가요?', '네트워크',
 'HTTP Keep-Alive는 하나의 TCP 연결을 재사용하여 여러 HTTP 요청/응답을 처리하는 기능입니다. HTTP/1.0에서는 매 요청마다 연결을 맺었지만, HTTP/1.1에서는 기본적으로 Keep-Alive가 활성화됩니다. 연결 수립/종료 오버헤드를 줄여 성능을 향상시키지만, 서버 자원 점유 시간이 늘어날 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'HTTP/1.1과 HTTP/2의 주요 차이점은 무엇인가요?', '네트워크',
 'HTTP/2는 멀티플렉싱으로 하나의 연결에서 여러 요청을 동시에 처리하여 Head-of-Line Blocking 문제를 해결합니다. 헤더 압축(HPACK), 서버 푸시, 바이너리 프레이밍 등이 추가되었습니다. HTTP/1.1의 파이프라이닝은 순서 보장 문제가 있었지만, HTTP/2는 스트림으로 이를 해결했습니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- ============================================================
-- 데이터베이스 (30문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'RDBMS와 NoSQL의 차이점을 설명해주세요.', '데이터베이스',
 'RDBMS는 테이블 기반의 정형 데이터를 관리하며, SQL을 사용하고 ACID 트랜잭션을 보장합니다. NoSQL은 Key-Value, Document, Column-Family, Graph 등 다양한 모델을 제공하며, 스키마 유연성과 수평 확장에 유리합니다. 데이터 구조와 확장 요구사항에 따라 선택합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Primary Key와 Foreign Key의 차이를 설명해주세요.', '데이터베이스',
 'Primary Key는 테이블의 각 행을 고유하게 식별하는 컬럼으로, NULL을 허용하지 않고 중복이 불가능합니다. Foreign Key는 다른 테이블의 Primary Key를 참조하여 테이블 간 관계를 설정합니다. FK를 통해 참조 무결성을 보장하며, 부모 테이블에 없는 값을 입력하면 에러가 발생합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'SQL의 DDL, DML, DCL, TCL을 구분해주세요.', '데이터베이스',
 'DDL(Data Definition Language)은 CREATE, ALTER, DROP 등 스키마 정의입니다. DML(Data Manipulation Language)은 SELECT, INSERT, UPDATE, DELETE 등 데이터 조작입니다. DCL(Data Control Language)은 GRANT, REVOKE 등 권한 제어이고, TCL(Transaction Control Language)은 COMMIT, ROLLBACK 등 트랜잭션 제어입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'JOIN의 종류와 차이점을 설명해주세요.', '데이터베이스',
 'INNER JOIN은 양쪽 테이블에 일치하는 행만 반환합니다. LEFT JOIN은 왼쪽 테이블의 모든 행과 일치하는 오른쪽 행을 반환하고, 없으면 NULL입니다. RIGHT JOIN은 반대입니다. FULL OUTER JOIN은 양쪽 모든 행을 반환합니다. CROSS JOIN은 카테시안 곱으로 모든 조합을 생성합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '1:1, 1:N, N:M 관계를 설명해주세요.', '데이터베이스',
 '1:1 관계는 한 행이 다른 테이블의 한 행과만 대응됩니다(사용자-프로필). 1:N은 한 행이 여러 행과 대응됩니다(부서-직원). N:M은 양쪽 모두 여러 행과 대응되며, 중간 테이블(조인 테이블)로 구현합니다(학생-수업). 관계 설계는 데이터 무결성과 쿼리 성능에 영향을 미칩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'NULL의 의미와 처리 시 주의사항을 설명해주세요.', '데이터베이스',
 'NULL은 값이 없음(unknown)을 의미하며, 0이나 빈 문자열과 다릅니다. NULL과의 비교는 항상 UNKNOWN을 반환하므로 IS NULL/IS NOT NULL을 사용해야 합니다. 집계 함수(COUNT, SUM 등)는 NULL을 무시하며, COALESCE()나 IFNULL()로 기본값을 지정할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'GROUP BY와 HAVING의 차이를 설명해주세요.', '데이터베이스',
 'GROUP BY는 특정 컬럼을 기준으로 행을 그룹화하며 집계 함수(COUNT, SUM, AVG 등)와 함께 사용합니다. HAVING은 그룹화된 결과에 조건을 적용합니다. WHERE는 그룹화 전에 행을 필터링하고, HAVING은 그룹화 후에 그룹을 필터링합니다. 실행 순서: WHERE → GROUP BY → HAVING입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '인덱스(Index)란 무엇이고 왜 사용하나요?', '데이터베이스',
 '인덱스는 테이블의 데이터를 빠르게 검색하기 위한 자료구조로, 대부분 B+Tree로 구현됩니다. 인덱스가 없으면 Full Table Scan이 필요하지만, 인덱스를 사용하면 O(log n)으로 조회가 가능합니다. 단점으로 INSERT/UPDATE/DELETE 시 인덱스 갱신 오버헤드가 발생하고 추가 저장 공간이 필요합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '트랜잭션(Transaction)이란 무엇인가요?', '데이터베이스',
 '트랜잭션은 하나의 논리적 작업 단위로, 모두 성공하거나 모두 실패해야 합니다. ACID 속성을 가집니다: 원자성(Atomicity, 전체 또는 취소), 일관성(Consistency, 데이터 무결성 유지), 격리성(Isolation, 동시 트랜잭션 간 간섭 방지), 지속성(Durability, 커밋 후 영구 저장).',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '정규화(Normalization)란 무엇이고 왜 필요한가요?', '데이터베이스',
 '정규화는 데이터 중복을 최소화하고 이상 현상(삽입, 수정, 삭제 이상)을 방지하기 위해 테이블을 분리하는 과정입니다. 1NF(원자값), 2NF(부분 함수 종속 제거), 3NF(이행 함수 종속 제거), BCNF(모든 결정자가 후보키) 단계로 진행됩니다. 과도한 정규화는 조인 증가로 성능에 영향을 줄 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'ACID 속성 각각을 설명해주세요.', '데이터베이스',
 '원자성(Atomicity)은 트랜잭션 내 모든 연산이 전부 수행되거나 전부 취소되어야 합니다. 일관성(Consistency)은 트랜잭션 전후로 데이터베이스가 일관된 상태를 유지해야 합니다. 격리성(Isolation)은 동시 트랜잭션이 서로 영향을 주지 않아야 합니다. 지속성(Durability)은 커밋된 트랜잭션은 영구 반영됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '서브쿼리(Subquery)의 종류와 사용법을 설명해주세요.', '데이터베이스',
 '서브쿼리는 쿼리 안에 포함된 쿼리로, 스칼라 서브쿼리(단일값), 인라인 뷰(FROM절), 중첩 서브쿼리(WHERE절)로 나뉩니다. EXISTS는 서브쿼리 결과 존재 여부를, IN은 값 목록 포함 여부를 확인합니다. 상관 서브쿼리는 외부 쿼리의 값을 참조하며, 성능을 위해 JOIN으로 변환하는 것이 좋을 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'VIEW란 무엇이고 사용 이유는?', '데이터베이스',
 'VIEW는 하나 이상의 테이블로부터 유도된 가상 테이블로, 실제 데이터를 저장하지 않고 쿼리 정의만 저장합니다. 복잡한 쿼리를 단순화하고, 보안을 위해 특정 컬럼만 노출하며, 데이터 독립성을 제공합니다. 일반적으로 읽기 전용이며, Materialized View는 결과를 물리적으로 저장합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'UNION과 UNION ALL의 차이를 설명해주세요.', '데이터베이스',
 'UNION은 두 쿼리 결과를 합치면서 중복을 제거합니다. UNION ALL은 중복을 포함하여 모든 결과를 반환합니다. 중복 제거에 정렬 작업이 필요하므로 UNION ALL이 성능이 더 좋습니다. 중복이 없거나 중복 허용이 가능한 경우 UNION ALL을 사용하는 것이 권장됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'DELETE, TRUNCATE, DROP의 차이를 설명해주세요.', '데이터베이스',
 'DELETE는 DML로 조건에 맞는 행을 삭제하며 롤백이 가능하고 인덱스/로그가 유지됩니다. TRUNCATE는 DDL로 테이블의 모든 행을 삭제하며 롤백이 불가하고 빠릅니다. DROP은 DDL로 테이블 자체를 삭제합니다. WHERE 조건이 필요하면 DELETE, 전체 초기화는 TRUNCATE를 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '데이터베이스 격리 수준 4단계를 설명해주세요.', '데이터베이스',
 'READ UNCOMMITTED는 커밋되지 않은 데이터를 읽을 수 있어 Dirty Read가 발생합니다. READ COMMITTED는 커밋된 데이터만 읽어 Dirty Read를 방지합니다. REPEATABLE READ는 트랜잭션 내 같은 쿼리 결과가 동일하여 Non-Repeatable Read를 방지합니다. SERIALIZABLE은 가장 엄격하여 Phantom Read까지 방지하지만 성능이 낮습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Dirty Read, Non-Repeatable Read, Phantom Read를 설명해주세요.', '데이터베이스',
 'Dirty Read는 다른 트랜잭션이 커밋하지 않은 데이터를 읽는 현상입니다. Non-Repeatable Read는 같은 쿼리를 두 번 실행했을 때 다른 결과가 나오는 현상입니다. Phantom Read는 같은 조건의 쿼리에서 새로운 행이 추가/삭제되어 결과 집합이 달라지는 현상입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'ERD(Entity Relationship Diagram)란 무엇인가요?', '데이터베이스',
 'ERD는 데이터베이스의 엔티티(테이블), 속성(컬럼), 관계(FK)를 시각적으로 표현한 다이어그램입니다. 개체를 사각형, 속성을 타원, 관계를 마름모로 표현하며, 카디널리티(1:1, 1:N, N:M)를 표시합니다. 데이터베이스 설계 초기 단계에서 구조를 파악하고 소통하는 데 필수적입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '클러스터드 인덱스와 논클러스터드 인덱스의 차이는?', '데이터베이스',
 '클러스터드 인덱스는 테이블 데이터를 인덱스 순서대로 물리적으로 정렬하며, 테이블당 하나만 생성 가능합니다(주로 PK). 논클러스터드 인덱스는 별도의 인덱스 구조에 데이터의 포인터를 저장하며, 여러 개 생성 가능합니다. 범위 검색은 클러스터드, 특정 조건 검색은 논클러스터드가 유리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '데이터베이스 락(Lock)의 종류를 설명해주세요.', '데이터베이스',
 '공유 락(Shared Lock, S)은 읽기 작업에 사용되며 여러 트랜잭션이 동시에 획득할 수 있습니다. 배타 락(Exclusive Lock, X)은 쓰기 작업에 사용되며 다른 락과 호환되지 않습니다. 행 락(Row Lock)은 특정 행만, 테이블 락(Table Lock)은 전체 테이블을 잠급니다. 락 범위가 클수록 동시성이 낮아집니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'HAVING과 WHERE의 실행 순서 차이를 설명해주세요.', '데이터베이스',
 'SQL 실행 순서는 FROM → WHERE → GROUP BY → HAVING → SELECT → ORDER BY입니다. WHERE는 GROUP BY 전에 개별 행을 필터링하고, HAVING은 GROUP BY 후에 그룹을 필터링합니다. 따라서 집계 함수(COUNT, SUM 등)를 조건에 사용하려면 HAVING을 써야 하며, WHERE에서는 집계 함수를 사용할 수 없습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '저장 프로시저(Stored Procedure)란 무엇인가요?', '데이터베이스',
 '저장 프로시저는 데이터베이스에 저장된 SQL 문의 집합으로, 이름으로 호출하여 실행합니다. 네트워크 트래픽을 줄이고, 실행 계획을 캐싱하여 성능을 향상시킵니다. 비즈니스 로직을 DB에 캡슐화하고 보안을 강화할 수 있지만, 디버깅이 어렵고 DB 의존성이 높아지는 단점이 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'ORM(Object-Relational Mapping)이란 무엇인가요?', '데이터베이스',
 'ORM은 객체 지향 프로그래밍의 객체와 관계형 데이터베이스의 테이블을 매핑하는 기술입니다. SQL을 직접 작성하지 않고 객체를 통해 데이터를 조작할 수 있어 생산성이 높습니다. JPA(Hibernate), Django ORM, TypeORM 등이 있으며, 복잡한 쿼리는 네이티브 SQL이 더 효율적일 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '데이터베이스 커넥션 풀(Connection Pool)이란 무엇인가요?', '데이터베이스',
 '커넥션 풀은 미리 일정 수의 DB 연결을 생성해두고 재사용하는 기법입니다. 연결 생성/해제 오버헤드를 줄이고 응답 시간을 단축합니다. HikariCP가 Java에서 가장 많이 사용되며, 최대 연결 수, 유휴 시간, 검증 쿼리 등을 설정합니다. 적절한 풀 크기 설정이 성능에 중요합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '이상 현상(Anomaly) 3가지를 설명해주세요.', '데이터베이스',
 '삽입 이상은 불필요한 데이터를 함께 삽입해야 하는 문제입니다. 수정 이상은 데이터 중복으로 일부만 수정되어 불일치가 발생하는 문제입니다. 삭제 이상은 필요한 데이터가 원치 않게 함께 삭제되는 문제입니다. 이러한 이상 현상은 정규화를 통해 해결할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'EXPLAIN 명령어의 용도와 확인해야 할 항목은?', '데이터베이스',
 'EXPLAIN은 SQL 쿼리의 실행 계획을 확인하는 명령어입니다. type(ALL=풀스캔, ref, const 등), possible_keys(사용 가능한 인덱스), key(실제 사용된 인덱스), rows(스캔 예상 행 수), Extra(Using index, Using temporary 등)를 확인합니다. 쿼리 성능 튜닝의 첫 번째 단계입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'SQL Injection이란 무엇이고 어떻게 방지하나요?', '데이터베이스',
 'SQL Injection은 사용자 입력에 악의적인 SQL 구문을 삽입하여 데이터베이스를 조작하는 공격입니다. 방지 방법으로는 PreparedStatement(파라미터 바인딩)를 사용하여 입력값을 SQL 구문과 분리하고, 입력값 검증, 최소 권한 DB 계정 사용, ORM 사용 등이 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '반정규화(Denormalization)란 무엇이고 언제 사용하나요?', '데이터베이스',
 '반정규화는 정규화된 테이블을 성능 향상을 위해 의도적으로 중복을 허용하는 것입니다. 조인 비용이 높은 읽기 위주의 쿼리에서 성능을 개선할 수 있습니다. 중복 컬럼 추가, 파생 컬럼 추가, 테이블 병합 등의 방법이 있으며, 데이터 일관성 관리 비용이 증가하는 트레이드오프가 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Replication이란 무엇이고 왜 사용하나요?', '데이터베이스',
 'Replication은 데이터베이스를 복제하여 여러 서버에 동일한 데이터를 유지하는 기법입니다. Master-Slave 구조에서 Master는 쓰기, Slave는 읽기를 처리하여 읽기 부하를 분산합니다. 고가용성(장애 시 Slave 승격)과 백업, 지역별 읽기 성능 향상에도 활용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '샤딩(Sharding)이란 무엇인가요?', '데이터베이스',
 '샤딩은 대량의 데이터를 여러 데이터베이스에 수평으로 분할하여 저장하는 기법입니다. 해시 기반, 범위 기반, 디렉토리 기반 등의 전략이 있습니다. 단일 서버의 용량 한계를 넘어 확장할 수 있지만, 크로스 샤드 쿼리가 어렵고 데이터 재분배(리밸런싱)가 복잡합니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- 추가 3문항 (120문항 달성)

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '그래프의 방향 그래프와 무방향 그래프의 차이를 설명해주세요.', '자료구조',
 '무방향 그래프는 간선에 방향이 없어 양방향 이동이 가능하고, 방향 그래프는 간선에 방향이 있어 한 방향으로만 이동합니다. 무방향 그래프의 간선 수는 방향 그래프의 절반이며, 소셜 네트워크(무방향, 친구 관계)와 웹 링크(방향, 하이퍼링크)가 대표적 예시입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', 'Circular Queue(원형 큐)의 개념과 장점을 설명해주세요.', '자료구조',
 '원형 큐는 배열의 끝과 시작을 연결하여 원형으로 동작하는 큐입니다. 일반 배열 큐에서 dequeue 시 발생하는 앞쪽 빈 공간 낭비를 해결합니다. front와 rear 포인터를 모듈러 연산으로 관리하며, 운영체제의 CPU 스케줄링 버퍼, 데이터 스트림 버퍼에 활용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, category, model_answer, reference_type, is_active, created_at) VALUES
('JUNIOR:CS_FUNDAMENTAL', '선형 자료구조와 비선형 자료구조의 차이를 설명해주세요.', '자료구조',
 '선형 자료구조는 데이터가 순차적으로 나열되며 Array, LinkedList, Stack, Queue가 포함됩니다. 비선형 자료구조는 계층이나 그래프 형태로 Tree, Graph가 포함됩니다. 선형은 구현이 간단하고 순차 접근에 유리하며, 비선형은 복잡한 관계를 표현하고 탐색/검색에 강점이 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());
