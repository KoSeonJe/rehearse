-- CS_FUNDAMENTAL MID 시드 데이터 (120문항: 자료구조30, 운영체제30, 네트워크30, 데이터베이스30)

-- ============================================================
-- 자료구조 (30문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'AVL 트리와 Red-Black 트리의 차이점을 설명해주세요.', '에이브이엘 트리와 Red-Black 트리의 차이점을 설명해주세요.', '자료구조',
 'AVL 트리는 모든 노드에서 좌우 서브트리 높이 차이가 최대 1인 엄격한 균형을 유지하여 탐색이 빠르지만 삽입/삭제 시 회전이 빈번합니다. Red-Black 트리는 색상 규칙으로 느슨한 균형을 유지하여 삽입/삭제가 빠릅니다. Java의 TreeMap과 HashMap의 버킷은 Red-Black 트리를 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'B-Tree와 B+Tree의 차이점을 설명해주세요.', '비트리와 비플러스트리의 차이점을 설명해주세요.', '자료구조',
 'B-Tree는 모든 노드에 키와 데이터를 저장하며, B+Tree는 리프 노드에만 데이터를 저장하고 내부 노드는 키만 가집니다. B+Tree의 리프 노드는 연결 리스트로 연결되어 범위 검색에 유리하며, 내부 노드에 더 많은 키를 저장할 수 있어 트리 높이가 낮습니다. 대부분의 RDBMS 인덱스는 B+Tree를 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'Trie 자료구조의 시간/공간 복잡도와 최적화 방법을 설명해주세요.', '트라이 자료구조의 시간 공간 복잡도와 최적화 방법을 설명해주세요.', '자료구조',
 'Trie는 문자열 길이 m에 대해 삽입/탐색이 O(m)이지만, 각 노드가 알파벳 크기만큼 포인터를 가져 공간이 O(ALPHABET_SIZE × n × m)입니다. 압축 Trie(Radix Tree)는 단일 자식 경로를 합쳐 공간을 절약하고, HashMap 기반 자식 저장으로 메모리를 최적화할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '세그먼트 트리(Segment Tree)의 개념과 활용 사례를 설명해주세요.', '세그먼트 트리의 개념과 활용 사례를 설명해주세요.', '자료구조',
 '세그먼트 트리는 배열의 구간 쿼리(합, 최소, 최대)를 O(log n)에 처리하는 트리 자료구조입니다. 빌드에 O(n), 쿼리와 업데이트에 O(log n)이 소요됩니다. Lazy Propagation으로 구간 업데이트도 O(log n)에 처리 가능합니다. 구간 합, 구간 최솟값, 히스토그램 문제 등에 활용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '우선순위 큐를 Heap이 아닌 다른 자료구조로 구현하면 어떤 차이가 있나요?', '우선순위 큐를 힙이 아닌 다른 자료구조로 구현하면 어떤 차이가 있나요?', '자료구조',
 '정렬 배열로 구현하면 삽입 O(n), 삭제 O(1)이고, 비정렬 배열은 삽입 O(1), 삭제 O(n)입니다. BST로 구현하면 균형 시 O(log n)이지만 최악 O(n)입니다. Heap은 삽입/삭제 모두 O(log n)을 보장하며 배열로 구현 가능하여 캐시 효율이 좋습니다. 피보나치 힙은 decrease-key가 amortized O(1)입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'DFS와 BFS의 시간/공간 복잡도 차이와 선택 기준을 설명해주세요.', '디에프에스와 비에프에스의 시간 공간 복잡도 차이와 선택 기준을 설명해주세요.', '자료구조',
 '두 알고리즘 모두 시간 복잡도는 O(V+E)이지만, 공간 복잡도에서 차이가 납니다. DFS는 재귀 깊이만큼 O(V) 스택을 사용하고, BFS는 큐에 같은 레벨 노드를 저장하므로 넓은 그래프에서 메모리 사용이 많습니다. 최단 경로는 BFS, 경로 탐색/사이클 검출은 DFS가 적합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '위상 정렬(Topological Sort)의 개념과 구현 방법을 설명해주세요.', '위상 정렬의 개념과 구현 방법을 설명해주세요.', '자료구조',
 '위상 정렬은 DAG(Directed Acyclic Graph)에서 선행 관계를 유지하면서 모든 노드를 선형 순서로 나열하는 알고리즘입니다. Kahn''s Algorithm(BFS, 진입차수 기반)과 DFS 기반 방법이 있습니다. 작업 스케줄링, 빌드 시스템 의존성 해결, 대학 수강 과목 순서 등에 활용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '최소 신장 트리(MST)를 구하는 알고리즘을 비교해주세요.', '최소 신장 트리를 구하는 알고리즘을 비교해주세요.', '자료구조',
 'Kruskal 알고리즘은 간선을 가중치 순으로 정렬하고 사이클이 생기지 않으면 추가하며 O(E log E)입니다. Union-Find로 사이클을 판별합니다. Prim 알고리즘은 임의의 노드에서 시작하여 최소 비용 간선을 추가하며, 우선순위 큐 사용 시 O(E log V)입니다. 밀집 그래프는 Prim, 희소 그래프는 Kruskal이 유리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '다익스트라와 벨만-포드 알고리즘의 차이를 설명해주세요.', '다익스트라와 벨만 포드 알고리즘의 차이를 설명해주세요.', '자료구조',
 '다익스트라는 음수 가중치가 없는 그래프에서 최단 경로를 O((V+E) log V)로 구합니다. 우선순위 큐를 사용하여 그리디하게 탐색합니다. 벨만-포드는 음수 가중치를 허용하며 O(VE)로 느리지만, 음수 사이클 검출이 가능합니다. 실무에서는 대부분 다익스트라가 사용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'Union-Find(Disjoint Set) 자료구조를 설명해주세요.', '유니온 파인드 자료구조를 설명해주세요.', '자료구조',
 'Union-Find는 서로소 집합을 관리하며 Find(대표 원소 찾기)와 Union(두 집합 합치기) 연산을 제공합니다. Path Compression과 Union by Rank 최적화를 적용하면 amortized O(α(n)) ≈ O(1)입니다. 그래프의 사이클 검출, Kruskal''s MST, 네트워크 연결 확인 등에 사용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '해시 함수의 좋은 조건과 충돌 최소화 방법을 설명해주세요.', '해시 함수의 좋은 조건과 충돌 최소화 방법을 설명해주세요.', '자료구조',
 '좋은 해시 함수는 균일 분포(Uniform Distribution), 빠른 계산, 눈사태 효과(입력 변화 시 출력 크게 변경)를 만족해야 합니다. 충돌 최소화를 위해 테이블 크기를 소수로, load factor를 적절히(0.7~0.8) 유지하고, 좋은 해시 함수(MurmurHash, xxHash)를 선택합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'Open Addressing의 탐사 방법(선형, 이차, 이중 해싱)을 비교해주세요.', '오픈 어드레싱의 탐사 방법, 선형, 이차, 이중 해싱을 비교해주세요.', '자료구조',
 '선형 탐사(h+1, h+2, ...)는 구현이 간단하지만 Primary Clustering(연속된 점유)이 발생합니다. 이차 탐사(h+1², h+2², ...)는 1차 군집을 완화하지만 2차 군집이 발생합니다. 이중 해싱(h1 + i×h2)은 두 번째 해시 함수로 탐사 간격을 결정하여 군집 문제를 최소화합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '그래프에서 사이클 검출 알고리즘을 설명해주세요.', '그래프에서 사이클 검출 알고리즘을 설명해주세요.', '자료구조',
 '무방향 그래프는 DFS에서 방문한 노드를 다시 만나면(부모 제외) 사이클이 존재합니다. Union-Find로도 간선 추가 시 같은 집합이면 사이클입니다. 방향 그래프는 DFS에서 노드를 WHITE(미방문), GRAY(진행중), BLACK(완료)으로 구분하여, GRAY 노드를 만나면 사이클(Back Edge)입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'Floyd-Warshall 알고리즘의 원리와 시간 복잡도를 설명해주세요.', '플로이드 워셜 알고리즘의 원리와 시간 복잡도를 설명해주세요.', '자료구조',
 'Floyd-Warshall은 모든 정점 쌍 간 최단 경로를 동적 프로그래밍으로 구하는 알고리즘입니다. 중간 정점 k를 거치는 것이 더 짧은지 비교하며 O(V³)의 시간 복잡도를 가집니다. 음수 가중치를 허용하고 음수 사이클도 검출할 수 있습니다. 작은 그래프에서 all-pairs 최단 경로에 적합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'Counting Sort와 Radix Sort의 원리와 적용 조건을 설명해주세요.', '카운팅 소트와 래딕스 소트의 원리와 적용 조건을 설명해주세요.', '자료구조',
 'Counting Sort는 각 값의 개수를 세어 정렬하며, 값의 범위 k에 대해 O(n+k)입니다. 범위가 작은 정수에 적합합니다. Radix Sort는 자릿수별로 Counting Sort를 적용하며 O(d(n+k))입니다. 둘 다 비교 기반이 아닌 분포 기반 정렬로, 특정 조건에서 O(n log n) 한계를 돌파합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '그래프 탐색에서 백트래킹(Backtracking)의 개념과 활용을 설명해주세요.', '그래프 탐색에서 백트래킹의 개념과 활용을 설명해주세요.', '자료구조',
 '백트래킹은 해를 탐색하다가 조건에 맞지 않으면 이전 단계로 돌아가 다른 경로를 시도하는 기법입니다. DFS 기반으로 구현하며, 가지치기(Pruning)로 불필요한 탐색을 줄입니다. N-Queens, 미로 탐색, 조합/순열 생성, 스도쿠 풀이 등에 활용됩니다. 최악의 경우 모든 경우를 탐색합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '이진 탐색의 변형 문제(Lower Bound, Upper Bound)를 설명해주세요.', '이진 탐색의 변형 문제, 로워 바운드, 어퍼 바운드를 설명해주세요.', '자료구조',
 'Lower Bound는 target 이상인 첫 번째 위치를, Upper Bound는 target 초과인 첫 번째 위치를 찾습니다. 중복이 있는 정렬 배열에서 특정 값의 범위를 구할 때 유용합니다. 구현 시 mid 계산, 경계 조건(left <= right vs left < right), 반환값 처리에 주의해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'Greedy 알고리즘과 Dynamic Programming의 차이를 설명해주세요.', '그리디 알고리즘과 다이나믹 프로그래밍의 차이를 설명해주세요.', '자료구조',
 'Greedy는 각 단계에서 최적의 선택을 하며 결정을 번복하지 않습니다. 탐욕 선택 속성과 최적 부분 구조가 필요합니다. DP는 모든 하위 문제의 해를 구하고 조합하여 최적해를 찾습니다. 겹치는 부분 문제가 있을 때 적합합니다. Greedy는 빠르지만 항상 최적해를 보장하지는 않습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'LRU(Least Recently Used) 캐시를 효율적으로 구현하는 방법을 설명해주세요.', '엘알유 캐시를 효율적으로 구현하는 방법을 설명해주세요.', '자료구조',
 'HashMap과 Doubly Linked List를 결합하면 get/put 모두 O(1)에 구현할 수 있습니다. HashMap은 key로 노드를 O(1) 조회하고, Doubly Linked List는 노드를 O(1)에 이동/삭제합니다. 접근 시 노드를 리스트 앞으로 이동하고, 용량 초과 시 리스트 끝의 노드를 제거합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'KMP 문자열 매칭 알고리즘의 원리를 설명해주세요.', '케이엠피 문자열 매칭 알고리즘의 원리를 설명해주세요.', '자료구조',
 'KMP는 패턴의 접두사와 접미사가 일치하는 정보(Failure Function)를 전처리하여, 불일치 시 비교 위치를 최적으로 이동합니다. 전처리 O(m), 검색 O(n)으로 총 O(n+m)의 시간 복잡도를 가집니다. 브루트포스 O(nm)보다 효율적이며, 텍스트 에디터의 찾기 기능 등에 활용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'Heap의 heapify 과정과 힙 정렬의 시간 복잡도를 분석해주세요.', '힙의 힙파이 과정과 힙 정렬의 시간 복잡도를 분석해주세요.', '자료구조',
 'Bottom-up heapify는 배열을 힙으로 변환하며 O(n)입니다(리프에서 시작하므로 대부분의 노드가 적은 높이에서 처리). 힙 정렬은 heapify O(n) + n번의 추출 O(n log n) = O(n log n)이며, in-place로 추가 공간이 O(1)입니다. 불안정 정렬이지만 최악의 경우에도 O(n log n)을 보장합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'Fenwick Tree(BIT)란 무엇이고 세그먼트 트리와 어떻게 다른가요?', '펜윅 트리란 무엇이고 세그먼트 트리와 어떻게 다른가요?', '자료구조',
 'Fenwick Tree는 구간 합 쿼리와 점 업데이트를 O(log n)에 처리하는 자료구조입니다. 세그먼트 트리보다 구현이 간단하고 메모리를 절반만 사용합니다. 하지만 구간 최솟값/최댓값 쿼리는 불가능하며, 구간 합에 특화되어 있습니다. 비트 연산으로 부모/자식을 탐색합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '강한 연결 요소(SCC)란 무엇이고 어떻게 구하나요?', '강한 연결 요소란 무엇이고 어떻게 구하나요?', '자료구조',
 '강한 연결 요소는 방향 그래프에서 모든 정점 쌍이 서로 도달 가능한 최대 부분 그래프입니다. Tarjan''s Algorithm은 DFS 한 번으로 SCC를 찾으며 O(V+E)입니다. Kosaraju''s Algorithm은 DFS 두 번(원본 + 역방향 그래프)으로 구합니다. 소셜 네트워크 분석, 웹 크롤링 등에 활용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'Amortized Analysis(분할 상환 분석)란 무엇인가요?', '어모타이즈드 애널리시스, 분할 상환 분석이란 무엇인가요?', '자료구조',
 '분할 상환 분석은 일련의 연산에서 최악의 단일 연산이 아닌 전체 연산의 평균 비용을 분석하는 기법입니다. 예를 들어 동적 배열의 확장은 단일 삽입이 O(n)이지만, n번 삽입의 총 비용이 O(n)이므로 amortized O(1)입니다. Aggregate, Accounting, Potential 방법이 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'Consistent Hashing의 개념과 활용 사례를 설명해주세요.', '컨시스턴트 해싱의 개념과 활용 사례를 설명해주세요.', '자료구조',
 'Consistent Hashing은 해시 링 위에 노드와 데이터를 배치하여, 노드 추가/제거 시 재배치되는 키를 최소화하는 기법입니다. 기존 해시는 노드 수 변경 시 모든 키가 재배치되지만, Consistent Hashing은 K/N 개만 이동합니다. 분산 캐시(Redis Cluster), CDN, 로드 밸런싱에 활용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '최소 공통 조상(LCA) 문제를 효율적으로 푸는 방법을 설명해주세요.', '최소 공통 조상 문제를 효율적으로 푸는 방법을 설명해주세요.', '자료구조',
 'Naive 방법은 두 노드를 같은 깊이로 맞추고 동시에 올라가며 O(n)입니다. Binary Lifting(Sparse Table)은 전처리 O(n log n), 쿼리 O(log n)으로 개선합니다. 오일러 투어 + 세그먼트 트리를 사용하면 쿼리 O(1)도 가능합니다. 네트워크 라우팅, 계층 구조 분석 등에 활용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'A* 알고리즘의 원리와 다익스트라와의 차이를 설명해주세요.', '에이 스타 알고리즘의 원리와 다익스트라와의 차이를 설명해주세요.', '자료구조',
 'A*는 f(n) = g(n) + h(n)으로 평가하여 목표까지의 예상 비용이 낮은 노드를 우선 탐색합니다. g(n)은 시작→현재 비용, h(n)은 현재→목표 휴리스틱입니다. 다익스트라는 h(n)=0인 A*의 특수 케이스입니다. h(n)이 admissible(과대평가 않음)하면 최적해를 보장합니다. 게임 경로 탐색, 네비게이션에 사용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '비트마스크(Bitmask)를 활용한 알고리즘 기법을 설명해주세요.', '비트마스크를 활용한 알고리즘 기법을 설명해주세요.', '자료구조',
 '비트마스크는 정수의 비트를 집합으로 활용하는 기법입니다. 원소 추가(|=), 삭제(&= ~), 포함 확인(&), 토글(^=) 연산을 O(1)에 수행합니다. DP에서 방문 상태 표현(외판원 문제), 부분 집합 열거, 권한 관리 등에 활용됩니다. 최대 64개(long) 원소까지 효율적으로 관리할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- ============================================================
-- 운영체제 (30문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '데드락의 4가지 필요 조건과 각 조건별 해결 방법을 설명해주세요.', '데드락의 4가지 필요 조건과 각 조건별 해결 방법을 설명해주세요.', '운영체제',
 '상호 배제(자원 공유 허용), 점유 대기(자원 일괄 요청), 비선점(자원 강제 회수), 순환 대기(자원 순서 정의)가 4가지 조건입니다. 예방은 조건 하나를 제거하고, 회피는 은행원 알고리즘으로 안전 상태를 유지합니다. 탐지는 자원 할당 그래프로 사이클을 검출하고 프로세스를 종료하여 복구합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '세마포어와 뮤텍스의 차이를 더 깊이 설명해주세요.', '세마포어와 뮤텍스의 차이를 더 깊이 설명해주세요.', '운영체제',
 '뮤텍스는 소유권 개념이 있어 락을 획득한 스레드만 해제할 수 있고, Priority Inheritance로 우선순위 역전을 해결합니다. 세마포어는 소유권이 없어 다른 스레드도 signal을 보낼 수 있으며, 카운팅으로 여러 자원을 관리합니다. 뮤텍스는 상호 배제에, 세마포어는 동기화 시그널링에 적합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '페이징과 세그먼테이션의 차이를 설명해주세요.', '페이징과 세그멘테이션의 차이를 설명해주세요.', '운영체제',
 '페이징은 고정 크기 페이지로 나누어 외부 단편화를 제거하지만 내부 단편화가 발생합니다. 세그먼테이션은 논리적 단위(코드, 데이터, 스택)로 나누어 가변 크기이므로 내부 단편화가 없지만 외부 단편화가 발생합니다. 현대 OS는 페이지드 세그먼테이션으로 두 방식을 결합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '가상 메모리의 요구 페이징(Demand Paging) 방식을 설명해주세요.', '가상 메모리의 요구 페이징 방식을 설명해주세요.', '운영체제',
 '요구 페이징은 프로세스 시작 시 모든 페이지를 로드하지 않고, 접근 시점에 필요한 페이지만 물리 메모리에 로드합니다. 페이지 테이블의 Valid/Invalid 비트로 존재 여부를 확인하고, 없으면 Page Fault가 발생하여 디스크에서 로드합니다. 초기 로딩 시간을 줄이고 메모리를 절약합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '페이지 교체 알고리즘(LRU, FIFO, LFU, Clock)을 비교해주세요.', '페이지 교체 알고리즘, 엘알유, 파이포, 엘에프유, 클록을 비교해주세요.', '운영체제',
 'FIFO는 가장 먼저 들어온 페이지를 교체하며 Belady''s Anomaly가 발생할 수 있습니다. LRU는 가장 오래 사용되지 않은 페이지를 교체하며 성능이 좋지만 구현 비용이 높습니다. LFU는 사용 빈도가 가장 낮은 페이지를 교체합니다. Clock(Second Chance)은 FIFO에 참조 비트를 추가하여 LRU를 근사합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'CPU 스케줄링 알고리즘(SJF, SRTF, MLFQ)을 비교해주세요.', '씨피유 스케줄링 알고리즘, 에스제이에프, 에스알티에프, 엠엘에프큐를 비교해주세요.', '운영체제',
 'SJF(Shortest Job First)는 비선점형으로 평균 대기 시간이 최소지만 실행 시간 예측이 어렵습니다. SRTF(Shortest Remaining Time First)는 SJF의 선점형 버전입니다. MLFQ(Multi-Level Feedback Queue)는 여러 우선순위 큐를 사용하여, 오래 실행되는 프로세스의 우선순위를 낮추어 응답성과 공정성을 균형 있게 제공합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'Thrashing의 원인과 해결 방법을 상세히 설명해주세요.', '스래싱의 원인과 해결 방법을 상세히 설명해주세요.', '운영체제',
 'Thrashing은 멀티프로그래밍 정도가 높아져 각 프로세스의 Working Set이 물리 메모리를 초과할 때 발생합니다. 페이지 폴트 빈도가 급증하고 CPU 이용률이 떨어집니다. 해결 방법으로 Working Set 모델(프로세스가 필요한 최소 프레임 보장), PFF(Page Fault Frequency) 조절, 프로세스 수 감소가 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'IPC 방법 중 공유 메모리와 메시지 패싱의 트레이드오프를 설명해주세요.', '아이피씨 방법 중 공유 메모리와 메시지 패싱의 트레이드오프를 설명해주세요.', '운영체제',
 '공유 메모리는 프로세스 간 메모리 영역을 공유하여 가장 빠르지만, 동기화(뮤텍스, 세마포어)가 필요합니다. 메시지 패싱은 커널을 통해 메시지를 전달하므로 동기화가 내장되어 안전하지만 복사 오버헤드가 있습니다. 대용량 데이터는 공유 메모리, 소량 제어 데이터는 메시지 패싱이 적합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'Race Condition을 해결하는 구체적인 방법들을 설명해주세요.', '레이스 컨디션을 해결하는 구체적인 방법들을 설명해주세요.', '운영체제',
 '뮤텍스/세마포어로 임계 영역을 보호하고, Compare-and-Swap(CAS) 같은 원자적 연산으로 락 없이 동기화합니다. Java에서는 synchronized, ReentrantLock, AtomicInteger, volatile 등을 제공합니다. 모니터(Monitor)는 뮤텍스와 조건 변수를 결합하여 고수준 동기화를 제공합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '우선순위 역전(Priority Inversion) 문제와 해결 방법을 설명해주세요.', '우선순위 역전 문제와 해결 방법을 설명해주세요.', '운영체제',
 '우선순위 역전은 낮은 우선순위 스레드가 자원을 점유하여 높은 우선순위 스레드가 대기하는 현상입니다. 중간 우선순위 스레드가 낮은 우선순위를 선점하면 더 심각해집니다. Priority Inheritance(낮은 스레드에 높은 우선순위 임시 부여)와 Priority Ceiling(자원에 최대 우선순위 설정)으로 해결합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '스핀락(Spinlock)과 뮤텍스의 차이를 설명해주세요.', '스핀락과 뮤텍스의 차이를 설명해주세요.', '운영체제',
 '스핀락은 락을 획득할 때까지 반복문으로 대기(Busy Waiting)하며, 컨텍스트 스위칭이 없어 짧은 임계 영역에서 효율적입니다. 뮤텍스는 락을 획득하지 못하면 스레드를 블록시키고 컨텍스트 스위칭이 발생하여, 긴 임계 영역에서 CPU 낭비를 줄입니다. 멀티코어에서 스핀락이 더 효과적일 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '멀티 레벨 페이지 테이블의 필요성을 설명해주세요.', '멀티 레벨 페이지 테이블의 필요성을 설명해주세요.', '운영체제',
 '32비트 주소 공간에서 4KB 페이지면 페이지 테이블 엔트리가 약 100만 개(4MB)입니다. 멀티 레벨 페이지 테이블은 사용하지 않는 영역의 테이블을 생성하지 않아 메모리를 절약합니다. 2단계 페이지 테이블은 디렉토리 + 테이블로 구성되며, 64비트 시스템은 4~5단계를 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'Copy-on-Write(COW) 기법을 설명해주세요.', '카피 온 라이트 기법을 설명해주세요.', '운영체제',
 'COW는 fork() 시 자식 프로세스가 부모의 메모리 페이지를 공유하고, 쓰기 시점에만 복사하는 최적화 기법입니다. fork() 후 exec()가 바로 호출되는 일반적인 패턴에서 불필요한 메모리 복사를 제거합니다. 페이지를 읽기 전용으로 설정하고 쓰기 시 Page Fault로 복사를 트리거합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'epoll, kqueue, select의 차이를 설명해주세요.', '이폴, 큐, 셀렉트의 차이를 설명해주세요.', '운영체제',
 'select는 모든 FD를 선형 탐색(O(n))하며 FD 수 제한(1024)이 있습니다. poll은 제한을 없앴지만 여전히 O(n)입니다. epoll(Linux)과 kqueue(BSD/macOS)는 이벤트 기반으로 O(1)이며 대규모 동시 연결에 적합합니다. Nginx, Node.js 등 고성능 서버가 epoll/kqueue를 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '리눅스의 프로세스 스케줄러(CFS)의 원리를 설명해주세요.', '리눅스의 프로세스 스케줄러, 씨에프에스의 원리를 설명해주세요.', '운영체제',
 'CFS(Completely Fair Scheduler)는 각 프로세스에 가상 실행 시간(vruntime)을 추적하여, vruntime이 가장 작은 프로세스를 먼저 실행합니다. Red-Black 트리로 관리하여 O(log n) 선택이 가능합니다. nice 값으로 가중치를 조절하며, 인터랙티브 프로세스에 유리한 공정한 CPU 분배를 목표합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '메모리 매핑 파일(Memory-Mapped File)이란 무엇인가요?', '메모리 매핑 파일이란 무엇인가요?', '운영체제',
 '메모리 매핑 파일은 파일의 내용을 프로세스의 가상 주소 공간에 매핑하여, 메모리 접근처럼 파일을 읽고 쓸 수 있게 하는 기법입니다. mmap() 시스템 콜로 구현하며, read/write 시스템 콜보다 커널-유저 복사가 없어 효율적입니다. 대용량 파일 처리, 프로세스 간 공유 메모리에 활용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '사용자 수준 스레드와 커널 수준 스레드의 차이를 설명해주세요.', '사용자 수준 스레드와 커널 수준 스레드의 차이를 설명해주세요.', '운영체제',
 '사용자 수준 스레드는 커널 개입 없이 라이브러리에서 관리하여 전환이 빠르지만, 하나가 블록되면 전체가 블록됩니다. 커널 수준 스레드는 OS가 관리하여 멀티코어 활용이 가능하지만 전환 비용이 높습니다. 현대 OS는 N:M 모델로 사용자 스레드를 커널 스레드에 매핑합니다. Java의 Virtual Thread도 이 원리입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '모니터(Monitor)의 개념과 동작 방식을 설명해주세요.', '모니터의 개념과 동작 방식을 설명해주세요.', '운영체제',
 '모니터는 뮤텍스와 조건 변수를 결합한 고수준 동기화 메커니즘입니다. 한 번에 하나의 스레드만 모니터 내 코드를 실행할 수 있고, 조건이 만족되지 않으면 wait()로 대기하고 다른 스레드가 notify()로 깨웁니다. Java의 synchronized, wait(), notify()가 모니터 패턴을 구현합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '디스크 스케줄링 알고리즘(FCFS, SSTF, SCAN, C-SCAN)을 비교해주세요.', '디스크 스케줄링 알고리즘, 에프씨에프에스, 에스에스티에프, 스캔, 씨 스캔을 비교해주세요.', '운영체제',
 'FCFS는 요청 순서대로 처리하여 공정하지만 비효율적입니다. SSTF는 현재 위치에서 가장 가까운 요청을 먼저 처리하여 평균 탐색 시간이 짧지만 기아 현상이 발생합니다. SCAN(엘리베이터)은 한 방향으로 이동하며 처리하고 끝에서 반전합니다. C-SCAN은 한 방향으로만 서비스하여 균일한 대기 시간을 제공합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'TLB(Translation Lookaside Buffer)의 역할과 동작을 설명해주세요.', '티엘비의 역할과 동작을 설명해주세요.', '운영체제',
 'TLB는 가상→물리 주소 변환을 캐싱하는 하드웨어 버퍼입니다. 페이지 테이블 접근은 메모리 참조가 필요하여 느리지만, TLB 히트 시 바로 물리 주소를 얻습니다. 일반적으로 TLB 히트율은 99% 이상입니다. 컨텍스트 스위칭 시 TLB 플러시가 필요하며 이것이 오버헤드의 주요 원인입니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- ============================================================
-- 네트워크 (30문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'TCP의 흐름 제어(Flow Control)와 혼잡 제어(Congestion Control)의 차이를 설명해주세요.', '티씨피의 흐름 제어와 혼잡 제어의 차이를 설명해주세요.', '네트워크',
 '흐름 제어는 수신자의 버퍼 오버플로를 방지하기 위해 슬라이딩 윈도우로 전송 속도를 조절합니다. 혼잡 제어는 네트워크 혼잡을 감지하고 전송 속도를 조절합니다. Slow Start → Congestion Avoidance → 손실 시 감소 과정을 거칩니다. 흐름 제어는 송수신자 간, 혼잡 제어는 네트워크 전체에 관한 것입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'TCP Slow Start와 Congestion Avoidance를 설명해주세요.', '티씨피 슬로우 스타트와 콩제션 어보이던스를 설명해주세요.', '네트워크',
 'Slow Start는 cwnd를 1 MSS에서 시작하여 ACK마다 2배로 증가시킵니다(지수 증가). ssthresh 도달 시 Congestion Avoidance로 전환하여 RTT마다 1 MSS씩 선형 증가합니다. 패킷 손실 시 Tahoe는 cwnd=1로 리셋, Reno는 Fast Recovery로 cwnd를 반으로 줄입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'URL 입력부터 화면 표시까지의 과정을 네트워크 관점에서 상세히 설명해주세요.', '유알엘 입력부터 화면 표시까지의 과정을 네트워크 관점에서 상세히 설명해주세요.', '네트워크',
 'URL 파싱 → 브라우저 캐시/OS 캐시 확인 → DNS 조회(재귀적) → TCP 3-way Handshake → TLS Handshake(HTTPS) → HTTP 요청 전송 → 서버 처리 → HTTP 응답(상태코드, 헤더, 바디) → HTML 파싱/DOM 구성 → CSS/JS/이미지 추가 요청(병렬) → 렌더 트리 구성 → 레이아웃/페인트 → 화면 표시.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'REST API 설계 원칙과 성숙도 모델(Richardson Maturity Model)을 설명해주세요.', '레스트 에이피아이 설계 원칙과 성숙도 모델을 설명해주세요.', '네트워크',
 'Level 0은 단일 엔드포인트, Level 1은 리소스 구분, Level 2는 HTTP 메서드 활용, Level 3은 HATEOAS(하이퍼미디어 링크)입니다. 설계 원칙으로 명사형 URI, 복수형 리소스, HTTP 상태코드 활용, 버전 관리, 필터링/페이징/정렬 파라미터, 일관된 응답 형식이 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'HTTP/1.1, HTTP/2, HTTP/3의 주요 차이를 설명해주세요.', '에이치티티피 1.1, 에이치티티피 2, 에이치티티피 3의 주요 차이를 설명해주세요.', '네트워크',
 'HTTP/1.1은 텍스트 기반, Keep-Alive, 파이프라이닝(제한적)을 제공합니다. HTTP/2는 바이너리 프레이밍, 멀티플렉싱(HOL Blocking 해결), 헤더 압축(HPACK), 서버 푸시를 제공합니다. HTTP/3는 TCP 대신 QUIC(UDP 기반)을 사용하여 연결 설정을 1-RTT로 줄이고 전송 계층 HOL Blocking을 해결합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'JWT(JSON Web Token)의 구조와 인증 과정을 설명해주세요.', '제이더블유티의 구조와 인증 과정을 설명해주세요.', '네트워크',
 'JWT는 Header(알고리즘, 타입), Payload(클레임: sub, exp, iat 등), Signature(Header+Payload를 비밀키로 서명)로 구성됩니다. 로그인 시 서버가 JWT를 발급하고, 이후 요청마다 Authorization 헤더에 Bearer 토큰으로 전송합니다. 서버는 서명을 검증하여 인증하므로 세션 저장이 불필요합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'SSL/TLS Handshake 과정을 상세히 설명해주세요.', '에스에스엘 티엘에스 핸드셰이크 과정을 상세히 설명해주세요.', '네트워크',
 '1) Client Hello(지원 암호화 스위트, 랜덤값), 2) Server Hello(선택된 암호화, 서버 인증서, 랜덤값), 3) 클라이언트가 인증서 검증(CA 체인), Pre-Master Secret을 서버 공개키로 암호화하여 전송, 4) 양쪽이 세션 키 생성, 5) Finished 메시지 교환. 이후 대칭키(세션 키)로 데이터를 암호화합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'CORS의 Preflight 요청이란 무엇이고 언제 발생하나요?', '코어스의 프리플라이트 요청이란 무엇이고 언제 발생하나요?', '네트워크',
 'Preflight는 실제 요청 전에 OPTIONS 메서드로 서버에 허용 여부를 확인하는 요청입니다. Content-Type이 application/json이거나, 커스텀 헤더가 있거나, PUT/DELETE 메서드 사용 시 발생합니다. 서버는 Access-Control-Allow-Origin, Methods, Headers 등으로 응답합니다. Simple Request(GET/POST + 기본 헤더)는 Preflight 없이 직접 요청됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'L4 로드 밸런싱과 L7 로드 밸런싱의 차이를 설명해주세요.', '엘4 로드 밸런싱과 엘7 로드 밸런싱의 차이를 설명해주세요.', '네트워크',
 'L4는 전송 계층에서 IP와 포트 기반으로 트래픽을 분산합니다. TCP 연결 수준에서 동작하여 빠르지만 콘텐츠 기반 라우팅이 불가합니다. L7은 응용 계층에서 HTTP 헤더, URL 경로, 쿠키 등을 기반으로 세밀한 분산이 가능합니다. AWS ALB는 L7, NLB는 L4 로드 밸런서입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'Blocking I/O, Non-blocking I/O, I/O Multiplexing의 차이를 설명해주세요.', '블로킹 아이오, 논블로킹 아이오, 아이오 멀티플렉싱의 차이를 설명해주세요.', '네트워크',
 'Blocking I/O는 작업 완료까지 스레드가 대기합니다. Non-blocking I/O는 즉시 반환하여 폴링으로 완료를 확인합니다. I/O Multiplexing(select/epoll)은 여러 FD를 하나의 스레드로 감시하여 이벤트 발생 시 처리합니다. Reactor 패턴(Netty, Node.js)은 I/O Multiplexing + 이벤트 루프로 고성능을 달성합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'OAuth 2.0의 인증 흐름(Authorization Code Grant)을 설명해주세요.', '오어스 2.0의 인증 흐름을 설명해주세요.', '네트워크',
 '1) 클라이언트가 Authorization Server로 리다이렉트(client_id, redirect_uri, scope 포함), 2) 사용자가 로그인/동의, 3) Authorization Code를 redirect_uri로 전달, 4) 클라이언트가 code + client_secret으로 Access Token 교환, 5) Access Token으로 Resource Server에 접근. Refresh Token으로 만료 시 재발급합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'gRPC와 REST API의 차이를 설명해주세요.', '지알피씨와 레스트 에이피아이의 차이를 설명해주세요.', '네트워크',
 'gRPC는 HTTP/2 기반으로 Protocol Buffers(바이너리 직렬화)를 사용하여 REST(JSON)보다 빠르고 가볍습니다. 양방향 스트리밍, 강타입 스키마(.proto), 코드 자동 생성을 지원합니다. REST는 브라우저 호환성, 사람이 읽기 쉬운 JSON, 유연한 스키마가 장점입니다. MSA 내부 통신은 gRPC, 외부 API는 REST가 일반적입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'TCP TIME_WAIT 상태의 존재 이유를 설명해주세요.', '티씨피 타임 웨이트 상태의 존재 이유를 설명해주세요.', '네트워크',
 'TIME_WAIT는 연결 종료 후 2MSL(Maximum Segment Lifetime) 동안 유지되는 상태입니다. 지연된 패킷이 새로운 연결에 영향을 주는 것을 방지하고, 마지막 ACK 손실 시 상대방의 FIN 재전송에 응답하기 위함입니다. 서버에서 TIME_WAIT가 과다하면 SO_REUSEADDR 옵션으로 포트 재사용을 허용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'TCP Nagle 알고리즘과 Delayed ACK의 상호작용 문제를 설명해주세요.', '티씨피 나글 알고리즘과 딜레이드 에이씨케이의 상호작용 문제를 설명해주세요.', '네트워크',
 'Nagle 알고리즘은 작은 패킷을 모아서 보내 네트워크 효율을 높입니다(미확인 데이터가 있으면 대기). Delayed ACK는 ACK를 지연시켜 데이터와 함께 보냅니다. 두 기능이 동시에 활성화되면 서로 기다리는 교착 상태로 지연이 발생합니다. TCP_NODELAY로 Nagle을 비활성화하여 해결합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'WebSocket과 SSE(Server-Sent Events)의 차이를 설명해주세요.', '웹소켓과 에스에스이의 차이를 설명해주세요.', '네트워크',
 'WebSocket은 양방향 전이중 통신으로 클라이언트와 서버 모두 메시지를 보낼 수 있습니다. SSE는 서버→클라이언트 단방향 통신으로 HTTP 위에서 동작합니다. SSE는 자동 재연결, 이벤트 ID 추적을 지원하며 구현이 간단합니다. 채팅은 WebSocket, 알림/피드는 SSE가 적합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'XSS(Cross-Site Scripting)와 CSRF(Cross-Site Request Forgery)의 차이를 설명해주세요.', '엑스에스에스와 씨에스알에프의 차이를 설명해주세요.', '네트워크',
 'XSS는 악성 스크립트를 웹 페이지에 삽입하여 사용자의 브라우저에서 실행시키는 공격입니다. 입력값 이스케이프, CSP 헤더로 방어합니다. CSRF는 인증된 사용자가 의도하지 않은 요청을 보내게 하는 공격입니다. CSRF 토큰, SameSite 쿠키, Referer 검증으로 방어합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'HTTP 캐시 전략(Cache-Control, ETag, Last-Modified)을 설명해주세요.', '에이치티티피 캐시 전략을 설명해주세요.', '네트워크',
 'Cache-Control 헤더로 캐시 정책을 설정합니다(max-age, no-cache, no-store, public/private). ETag는 콘텐츠의 해시값으로, If-None-Match 헤더와 비교하여 304 Not Modified를 반환합니다. Last-Modified는 수정 시간으로 If-Modified-Since로 확인합니다. 강한 검증(ETag)이 약한 검증(Last-Modified)보다 정확합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'DNS Round Robin의 한계와 대안을 설명해주세요.', '디엔에스 라운드 로빈의 한계와 대안을 설명해주세요.', '네트워크',
 'DNS Round Robin은 여러 IP를 순환 반환하여 부하를 분산하지만, 서버 상태를 확인하지 않아 장애 서버로도 트래픽이 전달됩니다. DNS 캐싱으로 분산이 불균일하고, 세션 유지가 어렵습니다. 대안으로 L4/L7 로드 밸런서(AWS ALB/NLB), 글로벌 서버 로드 밸런싱(GSLB), 헬스 체크 기반 DNS가 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '대칭키 암호화와 비대칭키 암호화의 차이를 설명해주세요.', '대칭키 암호화와 비대칭키 암호화의 차이를 설명해주세요.', '네트워크',
 '대칭키(AES, DES)는 암호화/복호화에 같은 키를 사용하여 빠르지만 키 전달 문제가 있습니다. 비대칭키(RSA, ECDSA)는 공개키와 개인키 쌍을 사용하여 키 전달이 안전하지만 느립니다. TLS에서는 비대칭키로 대칭키를 교환하고, 이후 대칭키로 데이터를 암호화하여 두 방식의 장점을 결합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'QUIC 프로토콜의 특징과 장점을 설명해주세요.', '퀵 프로토콜의 특징과 장점을 설명해주세요.', '네트워크',
 'QUIC는 UDP 위에 구현된 전송 프로토콜로 HTTP/3의 기반입니다. 0-RTT/1-RTT 연결 설정으로 지연을 줄이고, 스트림 단위 독립 전송으로 HOL Blocking을 해결합니다. 연결 마이그레이션(Connection ID 기반)으로 네트워크 전환 시에도 연결을 유지합니다. TLS 1.3이 내장되어 보안도 보장합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'TCP Fast Retransmit과 Fast Recovery를 설명해주세요.', '티씨피 패스트 리트랜스밋과 패스트 리커버리를 설명해주세요.', '네트워크',
 'Fast Retransmit은 3개의 중복 ACK를 받으면 타임아웃 전에 즉시 재전송하는 기법입니다. Fast Recovery(Reno)는 재전송 후 ssthresh를 cwnd/2로, cwnd를 ssthresh+3으로 설정하여 Slow Start로 돌아가지 않고 빠르게 회복합니다. Tahoe는 항상 Slow Start로 돌아가 성능이 낮습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'API Rate Limiting의 구현 방법을 설명해주세요.', '에이피아이 레이트 리미팅의 구현 방법을 설명해주세요.', '네트워크',
 'Token Bucket은 토큰이 일정 속도로 채워지고 요청마다 소비하여 버스트를 허용합니다. Leaky Bucket은 고정 속도로 처리하여 트래픽을 균일화합니다. Fixed Window는 시간 윈도우별 카운터로 간단하지만 경계에서 2배 트래픽이 가능합니다. Sliding Window Log/Counter는 이를 해결합니다. Redis로 분산 환경에서 구현합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'Content Negotiation이란 무엇인가요?', '컨텐트 네고시에이션이란 무엇인가요?', '네트워크',
 'Content Negotiation은 클라이언트와 서버가 리소스의 최적 표현을 결정하는 과정입니다. Accept(미디어 타입), Accept-Language(언어), Accept-Encoding(압축) 헤더를 사용합니다. 서버 구동 방식은 서버가 결정하고, 에이전트 구동 방식은 클라이언트가 선택합니다. API 버전 관리에도 활용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- ============================================================
-- 데이터베이스 (30문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '정규화(1NF~BCNF)의 각 단계를 설명하고 이상 현상과의 관계를 설명해주세요.', '정규화의 각 단계를 설명하고 이상 현상과의 관계를 설명해주세요.', '데이터베이스',
 '1NF는 모든 속성이 원자값이어야 합니다. 2NF는 부분 함수 종속을 제거합니다(복합키의 일부에만 종속되는 속성). 3NF는 이행 함수 종속을 제거합니다(A→B→C에서 A→C). BCNF는 모든 결정자가 후보키여야 합니다. 각 단계가 올라갈수록 삽입/수정/삭제 이상이 줄어들지만, 조인 비용이 증가합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '트랜잭션 격리 수준 4단계와 각 수준에서 발생할 수 있는 문제를 설명해주세요.', '트랜잭션 격리 수준 4단계와 각 수준에서 발생할 수 있는 문제를 설명해주세요.', '데이터베이스',
 'READ UNCOMMITTED는 Dirty/Non-Repeatable/Phantom Read 모두 가능합니다. READ COMMITTED는 Dirty Read만 방지합니다. REPEATABLE READ는 Dirty와 Non-Repeatable Read를 방지하지만 Phantom Read가 가능합니다(MySQL InnoDB는 갭 락으로 Phantom도 방지). SERIALIZABLE은 모두 방지하지만 성능이 낮습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'B+Tree 인덱스의 구조와 범위 검색에 유리한 이유를 설명해주세요.', '비 플러스 트리 인덱스의 구조와 범위 검색에 유리한 이유를 설명해주세요.', '데이터베이스',
 'B+Tree는 내부 노드에 키만, 리프 노드에 키+데이터 포인터를 저장합니다. 리프 노드가 연결 리스트로 연결되어 범위 검색 시 순차 탐색이 가능합니다. 내부 노드에 더 많은 키를 저장할 수 있어 트리 높이가 낮고(보통 3~4), 디스크 I/O가 줄어듭니다. 대부분의 RDBMS 인덱스가 B+Tree입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '실행 계획(EXPLAIN)을 분석하는 방법과 주요 지표를 설명해주세요.', '실행 계획을 분석하는 방법과 주요 지표를 설명해주세요.', '데이터베이스',
 'type은 접근 방법(system > const > eq_ref > ref > range > index > ALL 순으로 좋음)입니다. key는 실제 사용된 인덱스, rows는 예상 스캔 행 수입니다. Extra에서 Using index(커버링 인덱스), Using filesort(추가 정렬), Using temporary(임시 테이블) 등을 확인합니다. ALL이면 인덱스 추가를 검토합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '쿼리 최적화를 위한 인덱스 설계 전략을 설명해주세요.', '쿼리 최적화를 위한 인덱스 설계 전략을 설명해주세요.', '데이터베이스',
 '카디널리티가 높은 컬럼을 인덱스로 선택하고, WHERE/JOIN/ORDER BY에 자주 사용되는 컬럼에 생성합니다. 복합 인덱스는 선택도가 높은 컬럼을 앞에 배치하고, 최좌측 접두사 규칙을 고려합니다. 커버링 인덱스로 테이블 접근을 줄이고, 불필요한 인덱스는 쓰기 성능 저하를 유발하므로 제거합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '공유 락(S Lock)과 배타 락(X Lock)의 호환성을 설명해주세요.', '공유 락과 배타 락의 호환성을 설명해주세요.', '데이터베이스',
 'S Lock끼리는 호환되어 여러 트랜잭션이 동시에 읽기 가능합니다. X Lock은 S Lock, X Lock 모두와 호환되지 않아 쓰기 시 독점 접근합니다. 의도 락(IS, IX)은 테이블 수준에서 행 수준 락의 의도를 표시하여 충돌 검사를 효율화합니다. 데드락은 두 트랜잭션이 상대의 락을 기다릴 때 발생합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'MySQL의 InnoDB와 MyISAM의 차이를 설명해주세요.', '마이에스큐엘의 이노디비와 마이아이샘의 차이를 설명해주세요.', '데이터베이스',
 'InnoDB는 트랜잭션(ACID), 행 수준 락, 외래키, MVCC를 지원하며 MySQL 8.0의 기본 엔진입니다. MyISAM은 트랜잭션을 지원하지 않고 테이블 수준 락을 사용하지만, 전문 검색(Full-Text)과 COUNT(*)가 빠릅니다. 대부분의 경우 InnoDB가 권장되며, MyISAM은 읽기 전용 데이터에 제한적으로 사용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'Replication의 동기/비동기 방식과 트레이드오프를 설명해주세요.', '레플리케이션의 동기 비동기 방식과 트레이드오프를 설명해주세요.', '데이터베이스',
 '동기 복제는 모든 복제본에 쓰기가 완료되어야 커밋하므로 데이터 일관성이 보장되지만 지연이 발생합니다. 비동기 복제는 마스터에 쓰기 후 즉시 커밋하여 빠르지만, 장애 시 데이터 손실 가능성이 있습니다. 반동기(Semi-Sync)는 최소 하나의 복제본에 기록되면 커밋하여 균형을 맞춥니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'Sharding 전략(Hash, Range, Directory)을 비교해주세요.', '샤딩 전략을 비교해주세요.', '데이터베이스',
 'Hash Sharding은 키를 해싱하여 샤드를 결정하므로 균등 분산되지만 범위 쿼리가 어렵습니다. Range Sharding은 키 범위로 분할하여 범위 쿼리에 유리하지만 핫스팟이 발생할 수 있습니다. Directory Sharding은 매핑 테이블로 유연하지만 단일 장애점이 됩니다. Consistent Hashing은 노드 변경 시 재분배를 최소화합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '비정규화의 기법과 적용 시 고려사항을 설명해주세요.', '비정규화의 기법과 적용 시 고려사항을 설명해주세요.', '데이터베이스',
 '중복 컬럼 추가(조인 제거), 파생 컬럼(집계 결과 저장), 테이블 병합(1:1 관계), 테이블 분할(수직/수평)이 주요 기법입니다. 읽기 성능은 향상되지만 쓰기 시 중복 데이터 동기화 비용이 증가합니다. 트리거나 애플리케이션 로직으로 일관성을 유지해야 하며, 읽기/쓰기 비율을 고려하여 적용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'N+1 문제란 무엇이고 어떻게 해결하나요?', '엔 플러스 1 문제란 무엇이고 어떻게 해결하나요?', '데이터베이스',
 'N+1 문제는 1개의 쿼리로 N개의 엔티티를 조회한 후, 각 엔티티의 연관 데이터를 조회하기 위해 N개의 추가 쿼리가 발생하는 현상입니다. Fetch Join(JPQL JOIN FETCH), Entity Graph, Batch Size 설정으로 해결합니다. Lazy Loading이 기본인 JPA에서 자주 발생하며, 로그에서 쿼리 수를 모니터링해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'MVCC(Multi-Version Concurrency Control)의 개념을 설명해주세요.', '엠브이씨씨의 개념을 설명해주세요.', '데이터베이스',
 'MVCC는 데이터의 여러 버전을 유지하여 읽기와 쓰기가 서로 블록하지 않는 동시성 제어 기법입니다. 읽기 트랜잭션은 시작 시점의 스냅샷을 보고, 쓰기 트랜잭션은 새 버전을 생성합니다. InnoDB는 Undo Log로 이전 버전을 관리하며, PostgreSQL은 Tuple의 다중 버전을 유지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '인덱스가 사용되지 않는 경우(인덱스 무효화)를 설명해주세요.', '인덱스가 사용되지 않는 경우를 설명해주세요.', '데이터베이스',
 '인덱스 컬럼에 함수/연산을 적용하면(WHERE YEAR(date) = 2024) 인덱스를 사용할 수 없습니다. LIKE ''%keyword''(앞쪽 와일드카드), NOT/부정 조건, OR 조건(일부), 암시적 타입 변환, 테이블의 대부분(30% 이상)을 읽어야 하는 경우 옵티마이저가 Full Scan을 선택합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '데이터베이스 파티셔닝(Partitioning)이란 무엇인가요?', '데이터베이스 파티셔닝이란 무엇인가요?', '데이터베이스',
 '파티셔닝은 하나의 큰 테이블을 논리적으로 여러 파티션으로 나누어 관리하는 기법입니다. 범위(날짜), 리스트(카테고리), 해시(균등 분산), 복합 파티션 등이 있습니다. 쿼리 시 파티션 프루닝으로 필요한 파티션만 스캔하여 성능이 향상됩니다. 샤딩과 달리 하나의 DB 서버 내에서 동작합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'Slow Query를 분석하고 최적화하는 과정을 설명해주세요.', '슬로우 쿼리를 분석하고 최적화하는 과정을 설명해주세요.', '데이터베이스',
 'slow_query_log를 활성화하여 느린 쿼리를 수집합니다. EXPLAIN으로 실행 계획을 분석하고 Full Scan, filesort, temporary 여부를 확인합니다. 인덱스 추가/수정, 쿼리 리팩토링(서브쿼리→조인, SELECT * 제거), 페이지네이션 최적화(커서 기반), 캐싱(Redis) 적용 등으로 개선합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '커버링 인덱스(Covering Index)란 무엇인가요?', '커버링 인덱스란 무엇인가요?', '데이터베이스',
 '커버링 인덱스는 쿼리에 필요한 모든 컬럼이 인덱스에 포함되어 테이블을 접근하지 않고 인덱스만으로 결과를 반환하는 것입니다. EXPLAIN의 Extra에 "Using index"로 표시됩니다. 디스크 I/O를 크게 줄여 성능이 향상됩니다. 복합 인덱스에 SELECT 컬럼을 포함시켜 구성합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '낙관적 락(Optimistic Lock)과 비관적 락(Pessimistic Lock)의 차이를 설명해주세요.', '낙관적 락과 비관적 락의 차이를 설명해주세요.', '데이터베이스',
 '비관적 락은 데이터를 읽을 때 즉시 락을 걸어 다른 트랜잭션의 접근을 차단합니다(SELECT FOR UPDATE). 충돌이 많을 때 적합합니다. 낙관적 락은 락 없이 진행하고 커밋 시 버전 번호로 충돌을 감지하여 재시도합니다. 충돌이 드문 경우 성능이 더 좋으며, JPA의 @Version이 대표적입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'Redis의 데이터 구조와 활용 사례를 설명해주세요.', '레디스의 데이터 구조와 활용 사례를 설명해주세요.', '데이터베이스',
 'String(캐시, 카운터), Hash(사용자 프로필), List(타임라인, 큐), Set(태그, 좋아요 사용자), Sorted Set(리더보드, 랭킹), HyperLogLog(고유 방문자 수)를 제공합니다. 인메모리 저장으로 ms 단위 응답이 가능하며, TTL 설정, Pub/Sub, Lua 스크립트, 클러스터 모드를 지원합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '갭 락(Gap Lock)과 넥스트키 락(Next-Key Lock)을 설명해주세요.', '갭 락과 넥스트키 락을 설명해주세요.', '데이터베이스',
 '갭 락은 인덱스 레코드 사이의 간격(gap)에 거는 락으로, 새로운 행이 삽입되는 것을 방지합니다. 넥스트키 락은 레코드 락 + 갭 락의 결합으로, InnoDB의 REPEATABLE READ에서 Phantom Read를 방지합니다. 범위 조건(WHERE id > 10)에서 해당 범위의 갭에 락이 설정됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'WAL(Write-Ahead Logging)의 개념과 역할을 설명해주세요.', '더블유에이엘의 개념과 역할을 설명해주세요.', '데이터베이스',
 'WAL은 데이터 변경 전에 로그를 먼저 디스크에 기록하는 기법으로, 장애 복구의 기반입니다. 커밋 시 로그만 디스크에 쓰고, 실제 데이터 페이지는 나중에 체크포인트에서 반영합니다. 장애 시 WAL을 재생(Redo)하여 커밋된 트랜잭션을 복구하고, Undo로 미커밋 트랜잭션을 롤백합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'Connection Pool의 적절한 크기를 결정하는 방법을 설명해주세요.', '커넥션 풀의 적절한 크기를 결정하는 방법을 설명해주세요.', '데이터베이스',
 'HikariCP 공식에 따르면 최적 풀 크기 = (코어 수 × 2) + 유효 스핀들 수입니다. 너무 작으면 커넥션 대기 시간이 길어지고, 너무 크면 컨텍스트 스위칭과 DB 서버 부하가 증가합니다. 부하 테스트로 최적값을 찾고, 모니터링(대기 시간, 활성 커넥션 수)으로 조정합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'Full-Text Search 인덱스의 원리를 설명해주세요.', '풀 텍스트 서치 인덱스의 원리를 설명해주세요.', '데이터베이스',
 'Full-Text 인덱스는 텍스트를 토큰(단어)으로 분리하고, 각 토큰이 어떤 문서에 있는지 역인덱스(Inverted Index)로 저장합니다. B+Tree 인덱스로는 LIKE ''%keyword%'' 검색이 비효율적이지만, Full-Text 인덱스는 단어 단위 검색을 빠르게 수행합니다. MySQL은 InnoDB Full-Text, 전문 검색은 Elasticsearch가 더 강력합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '페이지네이션에서 Offset 방식과 Cursor 방식의 차이를 설명해주세요.', '페이지네이션에서 오프셋 방식과 커서 방식의 차이를 설명해주세요.', '데이터베이스',
 'Offset 방식(LIMIT 10 OFFSET 1000)은 구현이 간단하지만, 오프셋이 클수록 앞의 행을 모두 스캔하여 성능이 저하됩니다. Cursor 방식(WHERE id > last_id LIMIT 10)은 인덱스를 활용하여 일정한 성능을 유지합니다. 무한 스크롤이나 대량 데이터에는 Cursor 방식이 권장됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '데이터베이스 데드락을 감지하고 해결하는 방법을 설명해주세요.', '데이터베이스 데드락을 감지하고 해결하는 방법을 설명해주세요.', '데이터베이스',
 'InnoDB는 Wait-for Graph로 데드락을 자동 감지하고, 롤백 비용이 적은 트랜잭션을 선택하여 롤백합니다. 예방 방법으로는 인덱스 사용(락 범위 축소), 트랜잭션 순서 통일, 트랜잭션 시간 최소화, SELECT FOR UPDATE NOWAIT/SKIP LOCKED가 있습니다. SHOW ENGINE INNODB STATUS로 데드락 로그를 분석합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'Materialized View란 무엇이고 일반 View와 어떻게 다른가요?', '머티어라이즈드 뷰란 무엇이고 일반 뷰와 어떻게 다른가요?', '데이터베이스',
 '일반 View는 쿼리 정의만 저장하고 조회 시마다 실행하지만, Materialized View는 쿼리 결과를 물리적으로 저장합니다. 복잡한 집계/조인 쿼리의 결과를 캐싱하여 읽기 성능이 크게 향상됩니다. 단점은 데이터가 변경되면 수동/자동으로 갱신(REFRESH)해야 하며, 저장 공간이 필요합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'NoSQL 데이터베이스의 종류와 각각의 적합한 사용 사례를 설명해주세요.', '노에스큐엘 데이터베이스의 종류와 각각의 적합한 사용 사례를 설명해주세요.', '데이터베이스',
 'Key-Value(Redis, DynamoDB)는 세션, 캐시 등 단순 조회에 적합합니다. Document(MongoDB)는 유연한 스키마가 필요한 컨텐츠 관리에 적합합니다. Column-Family(Cassandra, HBase)는 대용량 시계열 데이터에 적합합니다. Graph(Neo4j)는 소셜 네트워크, 추천 시스템 등 관계 중심 데이터에 적합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- 추가 23문항 (목표 120문항 달성)

-- 자료구조 추가 (6문항)
INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '스택(Stack)과 큐(Queue)를 각각 두 개의 큐와 두 개의 스택으로 구현하는 방법을 설명해주세요.', '스택과 큐를 각각 두 개의 큐와 두 개의 스택으로 구현하는 방법을 설명해주세요.', '자료구조',
 '큐 두 개로 스택을 구현할 때 push는 빈 큐에 넣고 다른 큐 원소를 모두 옮기면 최신 원소가 앞에 위치합니다. pop은 O(1)이지만 push가 O(n)입니다. 반대로 push O(1), pop O(n)으로도 구현할 수 있습니다. 스택 두 개로 큐를 구현할 때 inStack에 push하고 outStack이 비면 inStack을 뒤집어 옮기는 방식으로 amortized O(1)을 달성합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '슬라이딩 윈도우(Sliding Window) 알고리즘의 원리와 활용 예를 설명해주세요.', '슬라이딩 윈도우 알고리즘의 원리와 활용 예를 설명해주세요.', '자료구조',
 '슬라이딩 윈도우는 배열이나 문자열에서 고정 또는 가변 크기의 구간을 이동하며 연산하는 기법으로, 중복 계산을 줄여 O(n²)을 O(n)으로 개선합니다. 최대 부분합, 최소 길이 부분 배열, 중복 없는 최장 부분 문자열 등의 문제에 활용됩니다. 투 포인터(Two Pointer)와 함께 사용하면 가변 윈도우 조건 처리가 용이합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '해시 테이블의 동적 리사이징(Dynamic Resizing) 과정을 설명해주세요.', '해시 테이블의 동적 리사이징 과정을 설명해주세요.', '자료구조',
 'load factor가 임계값(보통 0.75)을 초과하면 테이블 크기를 2배로 늘리고 모든 원소를 재해싱합니다. 이 과정이 O(n)이지만, 분할 상환 분석으로 삽입 연산의 평균 비용은 O(1)입니다. 줄이기(shrink)는 load factor가 0.25 미만일 때 절반으로 줄여 메모리를 회수합니다. Java의 HashMap은 초기 용량(16)과 load factor(0.75)가 기본값입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '모노토닉 스택(Monotonic Stack)의 개념과 활용 문제를 설명해주세요.', '모노토닉 스택의 개념과 활용 문제를 설명해주세요.', '자료구조',
 '모노토닉 스택은 원소가 단조 증가 또는 단조 감소 순서로 유지되는 스택입니다. 새 원소 삽입 시 조건을 위반하는 원소를 pop하면서 Next Greater Element, 히스토그램 최대 직사각형, 빗물 트래핑 문제 등을 O(n)에 해결합니다. 각 원소가 최대 한 번 push/pop 되어 전체 O(n)을 보장합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '동적 프로그래밍(DP)에서 메모이제이션과 타뷸레이션의 차이를 설명해주세요.', '동적 프로그래밍에서 메모이제이션과 타뷸레이션의 차이를 설명해주세요.', '자료구조',
 '메모이제이션(Top-Down)은 재귀 호출 시 이미 계산된 결과를 해시맵/배열에 저장하고 재사용하는 방식으로, 필요한 하위 문제만 계산합니다. 타뷸레이션(Bottom-Up)은 작은 하위 문제부터 순서대로 테이블을 채우는 반복문 방식으로, 스택 오버플로우 위험이 없고 캐시 효율이 좋습니다. 공간 최적화는 타뷸레이션에서 이전 행만 유지하는 방식으로 쉽게 적용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '이진 트리의 직렬화(Serialization)와 역직렬화(Deserialization) 방법을 설명해주세요.', '이진 트리의 직렬화와 역직렬화 방법을 설명해주세요.', '자료구조',
 '전위 순회(Pre-order)로 직렬화하면 null 노드를 특수 문자(#)로 표시하여 트리 구조를 완전히 복원할 수 있습니다. BFS 기반 레벨 순서 직렬화도 가능하며, LeetCode의 트리 표현 방식이 이를 사용합니다. 역직렬화 시 큐나 인덱스 포인터로 문자열을 순서대로 소비하며 트리를 재구성합니다. 후위 순회로도 가능하지만 중위 순회만으로는 고유한 트리를 복원할 수 없습니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- 운영체제 추가 (6문항)
INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '프로세스와 스레드의 메모리 구조 차이를 설명해주세요.', '프로세스와 스레드의 메모리 구조 차이를 설명해주세요.', '운영체제',
 '프로세스는 독립적인 메모리 공간(코드, 데이터, 힙, 스택)을 가지며 프로세스 간 메모리 격리가 됩니다. 같은 프로세스의 스레드들은 코드, 데이터, 힙 영역을 공유하고 각 스레드는 개별 스택과 레지스터를 가집니다. 공유 메모리 덕분에 스레드 간 통신이 빠르지만, 동기화 없이 접근하면 Race Condition이 발생합니다. 컨텍스트 스위칭은 스레드가 프로세스보다 빠릅니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '가상 메모리에서 페이지 폴트(Page Fault) 처리 과정을 설명해주세요.', '가상 메모리에서 페이지 폴트 처리 과정을 설명해주세요.', '운영체제',
 '페이지 폴트는 접근하려는 페이지가 물리 메모리에 없을 때 발생합니다. CPU가 페이지 폴트 인터럽트를 발생시키면 OS가 제어를 받아 디스크에서 해당 페이지를 빈 프레임에 로드합니다. 빈 프레임이 없으면 페이지 교체 알고리즘으로 희생 페이지를 선택하고, dirty 페이지면 스왑 영역에 기록 후 교체합니다. 페이지 테이블을 갱신하고 프로세스를 재시작합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '시스템 콜(System Call)의 동작 과정을 설명해주세요.', '시스템 콜의 동작 과정을 설명해주세요.', '운영체제',
 '시스템 콜은 사용자 모드 프로세스가 커널 기능을 호출하는 인터페이스입니다. 사용자 프로그램이 시스템 콜을 호출하면 소프트웨어 인터럽트(x86에서 int 0x80 또는 syscall 명령어)가 발생합니다. CPU 모드가 사용자 모드에서 커널 모드로 전환되고 레지스터에 시스템 콜 번호와 인자를 전달합니다. 커널이 처리를 완료하면 결과를 레지스터에 저장하고 사용자 모드로 복귀합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '컨텍스트 스위칭(Context Switching)의 비용과 최소화 방법을 설명해주세요.', '컨텍스트 스위칭의 비용과 최소화 방법을 설명해주세요.', '운영체제',
 '컨텍스트 스위칭 시 현재 프로세스/스레드의 레지스터, PC, 스택 포인터를 PCB/TCB에 저장하고 다음 실행 대상의 상태를 복원합니다. 비용 요소로는 CPU 시간(저장/복원), TLB 플러시(프로세스 스위칭 시), CPU 캐시 cold start 등이 있습니다. 최소화 방법으로는 스레드 수를 CPU 코어 수에 맞추기, CPU 어피니티 설정, 경량 코루틴/Green Thread 활용 등이 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '파이프(Pipe)와 소켓(Socket)의 차이를 설명해주세요.', '파이프와 소켓의 차이를 설명해주세요.', '운영체제',
 '파이프는 단방향(기명 파이프는 양방향 가능) IPC 메커니즘으로 부모-자식 프로세스 또는 같은 호스트 프로세스 간 통신에 사용됩니다. 소켓은 네트워크 스택을 통한 양방향 통신으로 다른 호스트 간에도 사용 가능하며 TCP/UDP 프로토콜을 선택할 수 있습니다. 로컬 통신에는 Unix Domain Socket이 TCP 소켓보다 빠르며 파일 시스템 경로를 주소로 사용합니다. 성능은 공유 메모리 > 파이프 > Unix 소켓 > TCP 소켓 순입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '인터럽트(Interrupt)의 종류와 처리 과정을 설명해주세요.', '인터럽트의 종류와 처리 과정을 설명해주세요.', '운영체제',
 '인터럽트는 하드웨어 인터럽트(외부 장치 신호), 소프트웨어 인터럽트(시스템 콜, int 명령어), 예외(Division by Zero, Page Fault 등)로 구분됩니다. 인터럽트 발생 시 현재 실행 정보를 스택에 저장하고, 인터럽트 벡터 테이블에서 해당 ISR(Interrupt Service Routine) 주소를 찾아 실행합니다. ISR 완료 후 저장된 실행 정보를 복원하고 원래 코드 실행을 재개합니다. 마스크 가능 인터럽트는 임시 비활성화가 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- 네트워크 추가 (6문항)
INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'TCP 3-way handshake와 4-way handshake를 설명해주세요.', '티씨피 3웨이 핸드셰이크와 4웨이 핸드셰이크를 설명해주세요.', '네트워크',
 '3-way handshake로 연결 수립: 클라이언트 SYN → 서버 SYN-ACK → 클라이언트 ACK. 이 과정에서 초기 시퀀스 번호(ISN)를 교환합니다. 4-way handshake로 연결 종료: 종료 요청자 FIN → 수신자 ACK → 수신자 FIN → 요청자 ACK. 요청자는 마지막 ACK 전송 후 TIME_WAIT 상태로 2MSL 대기하여 지연 패킷을 처리하고 상대방이 ACK를 받지 못한 경우 재전송에 대비합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'HTTP Keep-Alive와 커넥션 풀(Connection Pool)의 관계를 설명해주세요.', '에이치티티피 킵 얼라이브와 커넥션 풀의 관계를 설명해주세요.', '네트워크',
 'HTTP Keep-Alive(Persistent Connection)는 하나의 TCP 연결로 여러 HTTP 요청/응답을 처리하여 3-way handshake 반복 비용을 줄입니다. HTTP/1.1에서 기본 활성화되며 Connection: close로 비활성화 가능합니다. 커넥션 풀은 미리 생성된 TCP 연결을 재사용하는 클라이언트 사이드 최적화로, Keep-Alive와 결합하면 응답 지연을 크게 줄입니다. 풀 크기는 서버 최대 동시 연결 수와 클라이언트 부하를 고려하여 설정해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'CDN(Content Delivery Network)의 동작 원리와 캐싱 전략을 설명해주세요.', '씨디엔의 동작 원리와 캐싱 전략을 설명해주세요.', '네트워크',
 'CDN은 전 세계 엣지 서버에 콘텐츠를 복제하여 사용자와 가까운 서버에서 콘텐츠를 제공함으로써 지연 시간을 줄입니다. DNS 기반 라우팅으로 사용자를 가장 가까운 엣지 서버로 연결합니다. 캐싱 전략으로 Cache-Control 헤더의 max-age로 TTL을 설정하고, 갱신 시 캐시 키(URL + 버전 파라미터)를 변경하거나 캐시 퍼지 API를 사용합니다. 정적 자산은 긴 TTL, API 응답은 짧은 TTL 또는 no-store를 적용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'DNS 조회 과정을 재귀적 질의와 반복적 질의로 설명해주세요.', '디엔에스 조회 과정을 재귀적 질의와 반복적 질의로 설명해주세요.', '네트워크',
 'DNS 조회는 브라우저 캐시 → OS 캐시 → 로컬 DNS 서버(ISP 제공) 순으로 확인합니다. 재귀적 질의는 로컬 DNS 서버가 클라이언트를 대신하여 루트 → TLD → 권위 네임서버까지 대신 조회하고 최종 결과만 반환합니다. 반복적 질의는 각 서버가 다음 서버 주소만 알려주고 클라이언트가 직접 단계별 조회합니다. 실제로 클라이언트↔로컬DNS는 재귀적, 로컬DNS↔상위 서버는 반복적 방식을 혼용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'IP 서브넷팅(Subnetting)과 CIDR 표기법을 설명해주세요.', '아이피 서브넷팅과 씨아이디알 표기법을 설명해주세요.', '네트워크',
 'CIDR(Classless Inter-Domain Routing)은 IP 주소 뒤에 /prefix-length로 네트워크 부분 비트 수를 표시합니다. 예를 들어 192.168.1.0/24는 네트워크 부분 24비트, 호스트 부분 8비트로 256개(사용 가능 254개) 주소입니다. 서브넷 마스크 255.255.255.0과 동일합니다. VLSM(Variable Length Subnet Mask)으로 필요에 따라 다른 크기의 서브넷을 할당하여 IP 낭비를 줄입니다. 라우팅 테이블에서 Longest Prefix Match 규칙으로 경로를 선택합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'Reverse Proxy와 Forward Proxy의 차이를 설명해주세요.', '리버스 프록시와 포워드 프록시의 차이를 설명해주세요.', '네트워크',
 'Forward Proxy는 클라이언트 앞에 위치하여 클라이언트의 요청을 대신 외부 서버에 전달합니다. 클라이언트 익명성 확보, 접근 제어, 캐싱에 활용됩니다. Reverse Proxy는 서버 앞에 위치하여 클라이언트 요청을 내부 서버로 라우팅합니다. 로드 밸런싱, SSL 종료, 캐싱, 서버 IP 은폐에 활용됩니다. Nginx, HAProxy가 Reverse Proxy로 많이 사용됩니다. 클라이언트는 Reverse Proxy를 실제 서버로 인식합니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- 데이터베이스 추가 (5문항)
INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '복합 인덱스(Composite Index)의 설계 원칙과 컬럼 순서의 중요성을 설명해주세요.', '복합 인덱스의 설계 원칙과 컬럼 순서의 중요성을 설명해주세요.', '데이터베이스',
 '복합 인덱스는 여러 컬럼을 결합한 인덱스로, 컬럼 순서가 성능에 결정적입니다. Leftmost Prefix 규칙에 따라 (A, B, C) 인덱스는 A, (A, B), (A, B, C) 조건에서만 활용되고 B만 조건으로 사용하면 인덱스가 비효율적입니다. 선택도(Cardinality)가 높은 컬럼을 앞에 배치하면 인덱스 필터링 효율이 높아집니다. 동등 조건 컬럼을 앞에, 범위 조건 컬럼을 뒤에 배치하는 것이 기본 원칙입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '데이터베이스 뷰(View)의 개념과 사용 목적, 한계를 설명해주세요.', '데이터베이스 뷰의 개념과 사용 목적, 한계를 설명해주세요.', '데이터베이스',
 '뷰는 하나 이상의 테이블을 기반으로 한 가상 테이블로, 복잡한 쿼리를 단순화하고 특정 컬럼만 노출하여 보안을 강화합니다. 데이터는 실제로 저장되지 않으며 조회 시마다 기반 쿼리가 실행됩니다. 단순 뷰는 INSERT/UPDATE/DELETE가 가능하지만 GROUP BY, 집계 함수, DISTINCT, JOIN이 포함된 복잡한 뷰는 수정 불가입니다. Materialized View는 결과를 실제 저장하여 조회 성능을 높이지만 데이터 동기화 관리가 필요합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', 'INNER JOIN, LEFT JOIN, RIGHT JOIN, FULL OUTER JOIN의 차이를 설명해주세요.', '이너 조인, 레프트 조인, 라이트 조인, 풀 아우터 조인의 차이를 설명해주세요.', '데이터베이스',
 'INNER JOIN은 두 테이블 모두에 매칭되는 행만 반환합니다. LEFT JOIN은 왼쪽 테이블 전체와 오른쪽에서 매칭되는 행을, 매칭 없으면 NULL을 반환합니다. RIGHT JOIN은 그 반대입니다. FULL OUTER JOIN은 양쪽 테이블 모두를 포함하고 매칭 없는 쪽은 NULL로 채웁니다. MySQL은 FULL OUTER JOIN을 직접 지원하지 않아 LEFT JOIN UNION RIGHT JOIN으로 구현합니다. CROSS JOIN은 카르테시안 곱으로 두 테이블의 모든 조합을 반환합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '스토어드 프로시저(Stored Procedure)의 장단점을 설명해주세요.', '스토어드 프로시저의 장단점을 설명해주세요.', '데이터베이스',
 '스토어드 프로시저는 DB 서버에 미리 컴파일된 SQL 집합으로, 네트워크 왕복 횟수 감소, 실행 계획 재사용으로 성능이 유리합니다. 비즈니스 로직의 DB 집중화로 보안을 강화하고 권한 제어가 용이합니다. 단점으로는 버전 관리 어려움, 특정 DB 종속성, 복잡한 로직에서 디버깅이 어렵습니다. 애플리케이션 레이어에서 로직을 관리하는 현대 아키텍처(ORM 기반)에서는 사용이 줄어드는 추세입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('MID:CS_FUNDAMENTAL', '데이터베이스 트랜잭션의 ACID 속성을 각각 구체적인 예시로 설명해주세요.', '데이터베이스 트랜잭션의 에이씨아이디 속성을 각각 구체적인 예시로 설명해주세요.', '데이터베이스',
 'Atomicity(원자성)는 계좌 이체 시 출금과 입금이 모두 성공하거나 둘 다 롤백됩니다. Consistency(일관성)는 트랜잭션 전후로 DB가 정의된 무결성 제약(외래키, NOT NULL)을 유지합니다. Isolation(격리성)은 동시 실행 트랜잭션이 서로의 중간 상태를 볼 수 없어 직렬 실행과 동일한 결과를 보장합니다. Durability(지속성)는 커밋된 트랜잭션은 시스템 장애 후에도 WAL을 통해 복구 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());
