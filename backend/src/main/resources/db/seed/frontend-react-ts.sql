-- React/TypeScript 시드 데이터 (180문항: LANGUAGE_FRAMEWORK 90 + UI_FRAMEWORK 90)

-- ============================================================
-- LANGUAGE_FRAMEWORK — JUNIOR (30문항)
-- cache_key: FRONTEND:JUNIOR:REACT_TS:LANGUAGE_FRAMEWORK
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'Virtual DOM이란 무엇이고 왜 사용하나요?', 'Virtual DOM이란 무엇이고 왜 사용하나요?', 'REACT_CORE',
 'Virtual DOM은 실제 DOM의 경량 사본으로, 상태 변경 시 새로운 Virtual DOM을 생성하여 이전 것과 비교(Diffing)합니다. 변경된 부분만 실제 DOM에 반영(Reconciliation)하여 불필요한 DOM 조작을 최소화합니다. 선언적 UI 프로그래밍을 가능하게 하면서도 충분히 좋은 성능을 제공하는 것이 핵심 가치입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'React의 Reconciliation 과정을 설명해주세요.', '리액트의 Reconciliation 과정을 설명해주세요.', 'REACT_CORE',
 'Reconciliation은 Virtual DOM 트리를 비교하여 최소한의 DOM 변경을 계산하는 과정입니다. 같은 타입의 엘리먼트는 속성만 업데이트하고, 다른 타입이면 서브트리를 교체합니다. 리스트에서는 key prop으로 요소를 식별하여 효율적으로 재배치합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:LANGUAGE_FRAMEWORK', '클래스 컴포넌트와 함수형 컴포넌트의 차이를 설명해주세요.', '클래스 컴포넌트와 함수형 컴포넌트의 차이를 설명해주세요.', 'REACT_CORE',
 '클래스 컴포넌트는 React.Component를 상속하고 render() 메서드와 생명주기 메서드를 사용합니다. 함수형 컴포넌트는 함수로 정의하고 Hooks로 상태와 생명주기를 관리합니다. 현재 React 공식 문서는 함수형 컴포넌트를 권장하며, 코드가 간결하고 로직 재사용이 용이합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'Hook의 규칙(Rules of Hooks)을 설명해주세요.', '훅의 규칙(Rules of Hooks)을 설명해주세요.', 'HOOKS',
 '1) 최상위에서만 호출해야 합니다(반복문, 조건문, 중첩 함수 안에서 호출 금지). 2) React 함수 컴포넌트 또는 커스텀 Hook에서만 호출해야 합니다. 이 규칙은 React가 Hook 호출 순서로 상태를 추적하기 때문이며, eslint-plugin-react-hooks로 검증합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'useState의 동작 원리를 설명해주세요.', 'useState의 동작 원리를 설명해주세요.', 'HOOKS',
 'useState는 컴포넌트에 상태 변수를 추가하는 Hook으로, [값, 세터] 배열을 반환합니다. 세터 함수 호출 시 리렌더링이 트리거됩니다. 상태 업데이트는 비동기적으로 배치 처리되며, 이전 상태에 기반할 때는 함수형 업데이트(prev => prev + 1)를 사용해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'useEffect의 의존성 배열(dependency array)의 역할을 설명해주세요.', 'useEffect의 의존성 배열(dependency array)의 역할을 설명해주세요.', 'HOOKS',
 '의존성 배열은 effect가 재실행되는 조건을 지정합니다. 빈 배열([])이면 마운트 시 한 번만 실행되고, 배열에 값이 있으면 해당 값 변경 시 실행됩니다. 배열을 생략하면 매 렌더링마다 실행됩니다. cleanup 함수를 반환하여 구독 해제나 타이머 정리를 수행합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'useEffect와 useLayoutEffect의 차이를 설명해주세요.', 'useEffect와 useLayoutEffect의 차이를 설명해주세요.', 'HOOKS',
 'useEffect는 브라우저 페인팅 후 비동기적으로 실행되므로 DOM을 변경하면 깜빡임이 발생할 수 있습니다. useLayoutEffect는 DOM 변경 후 페인팅 전에 동기적으로 실행됩니다. DOM 측정이나 스크롤 위치 조정처럼 레이아웃에 영향을 주는 작업에 useLayoutEffect를 사용하여 깜빡임을 방지합니다. 대부분의 경우 useEffect로 충분합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'Props Drilling 문제와 해결 방법을 설명해주세요.', 'Props Drilling 문제와 해결 방법을 설명해주세요.', 'STATE_MANAGEMENT',
 'Props Drilling은 중간 컴포넌트가 사용하지 않는 props를 자식에게 전달하기 위해 받아야 하는 문제입니다. 해결 방법으로 Context API(전역 상태), 상태 관리 라이브러리(Zustand, Redux), Composition 패턴(children prop 활용)이 있습니다. 컴포넌트 구조를 재설계하는 것이 가장 근본적인 해결책입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'key prop의 역할과 중요성을 설명해주세요.', 'key prop의 역할과 중요성을 설명해주세요.', 'REACT_CORE',
 'key는 리스트 렌더링 시 React가 각 요소를 고유하게 식별하는 데 사용됩니다. key가 없거나 index를 사용하면 요소 순서 변경 시 불필요한 리렌더링이나 상태 혼동이 발생합니다. 고유한 ID를 key로 사용해야 하며, key가 변경되면 컴포넌트가 언마운트 후 새로 마운트됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'Context API의 사용법과 한계를 설명해주세요.', 'Context 에이피아이의 사용법과 한계를 설명해주세요.', 'STATE_MANAGEMENT',
 'createContext로 컨텍스트를 생성하고, Provider로 값을 제공하며, useContext로 소비합니다. 전역 테마, 인증 정보 등 자주 변경되지 않는 데이터에 적합합니다. 한계로는 값이 변경되면 모든 소비 컴포넌트가 리렌더링되어 성능 문제가 발생할 수 있으며, 복잡한 상태 관리에는 부적합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'TypeScript에서 React 컴포넌트의 Props 타입을 정의하는 방법을 설명해주세요.', '타입스크립트에서 리액트 컴포넌트의 Props 타입을 정의하는 방법을 설명해주세요.', 'TYPESCRIPT',
 'interface 또는 type으로 Props 타입을 정의하고 컴포넌트 매개변수에 적용합니다. React.FC<Props> 사용은 현재 권장되지 않으며, 직접 타입 지정이 선호됩니다. children은 React.ReactNode, 이벤트는 React.MouseEvent<HTMLButtonElement> 등 React 제공 타입을 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'useRef의 두 가지 용도를 설명해주세요.', 'useRef의 두 가지 용도를 설명해주세요.', 'HOOKS',
 'useRef는 DOM 요소 접근(ref.current로 DOM 노드 직접 제어)과 렌더링 간 값 유지(변경해도 리렌더링 미발생)에 사용됩니다. useState와 달리 .current 변경이 리렌더링을 트리거하지 않아, 이전 값 저장, 타이머 ID 보관 등에 적합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'React에서 이벤트 처리 방식의 특징을 설명해주세요.', '리액트에서 이벤트 처리 방식의 특징을 설명해주세요.', 'REACT_CORE',
 'React는 SyntheticEvent로 브라우저 이벤트를 래핑하여 크로스 브라우저 호환성을 제공합니다. 이벤트 핸들러는 camelCase(onClick, onChange)로 작성하고 함수를 전달합니다. React 17부터 이벤트 위임이 root DOM이 아닌 렌더링 컨테이너에 등록됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'React의 단방향 데이터 흐름을 설명해주세요.', '리액트의 단방향 데이터 흐름을 설명해주세요.', 'REACT_CORE',
 'React는 부모에서 자식으로 props를 통해 데이터가 흐르는 단방향 데이터 흐름을 따릅니다. 자식이 부모 상태를 변경하려면 부모가 전달한 콜백 함수를 호출합니다. 이 패턴은 데이터 흐름을 예측 가능하게 하고 디버깅을 쉽게 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'React 컴포넌트의 생명주기를 Hook으로 어떻게 표현하나요?', '리액트 컴포넌트의 생명주기를 훅으로 어떻게 표현하나요?', 'HOOKS',
 'componentDidMount는 useEffect(fn, []), componentDidUpdate는 useEffect(fn, [deps]), componentWillUnmount는 useEffect에서 cleanup 함수 반환으로 대체합니다. shouldComponentUpdate는 React.memo로 대체합니다. getDerivedStateFromProps는 렌더링 중 setState로 대체할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'React에서 조건부 렌더링 방법들을 설명해주세요.', '리액트에서 조건부 렌더링 방법들을 설명해주세요.', 'REACT_CORE',
 'if/else 문으로 JSX를 반환하거나, 삼항 연산자(condition ? A : B), 논리 AND 연산자(condition && <Comp/>), 즉시 실행 함수(IIFE)를 사용합니다. null을 반환하면 아무것도 렌더링하지 않습니다. 복잡한 조건에는 별도 함수나 컴포넌트로 분리하는 것이 가독성에 좋습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'React에서 리스트 렌더링 시 주의사항을 설명해주세요.', '리액트에서 리스트 렌더링 시 주의사항을 설명해주세요.', 'REACT_CORE',
 'map()으로 배열을 JSX로 변환하며, 각 요소에 고유한 key를 부여해야 합니다. index를 key로 사용하면 요소 순서 변경 시 성능 저하와 상태 버그가 발생합니다. 대규모 리스트는 react-window나 react-virtuoso로 가상화하여 렌더링 성능을 개선합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'React에서 폼을 제어 컴포넌트로 관리하는 방법을 설명해주세요.', '리액트에서 폼을 제어 컴포넌트로 관리하는 방법을 설명해주세요.', 'REACT_CORE',
 '제어 컴포넌트는 입력값을 React 상태로 관리하며, value와 onChange를 통해 동기화합니다. 비제어 컴포넌트는 ref로 DOM에서 직접 값을 가져옵니다. 제어 컴포넌트가 유효성 검사, 동적 UI에 유리하며, react-hook-form은 비제어 방식으로 성능을 최적화합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'TypeScript의 기본 타입과 React에서 자주 쓰이는 타입을 설명해주세요.', '타입스크립트의 기본 타입과 리액트에서 자주 쓰이는 타입을 설명해주세요.', 'TYPESCRIPT',
 'string, number, boolean, null, undefined가 기본 타입이고, Array<T>, Record<K,V>, union(A | B) 등이 유틸리티 타입입니다. React에서는 ReactNode(렌더링 가능한 모든 것), ReactElement(JSX 요소), FC(함수 컴포넌트), ChangeEvent<HTMLInputElement> 등을 자주 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'React에서 에러 바운더리(Error Boundary)란 무엇인가요?', '리액트에서 에러 바운더리(Error Boundary)란 무엇인가요?', 'REACT_CORE',
 'Error Boundary는 자식 컴포넌트 트리에서 발생한 JavaScript 에러를 잡아 폴백 UI를 표시하는 클래스 컴포넌트입니다. getDerivedStateFromError와 componentDidCatch 메서드를 구현합니다. Hook으로는 아직 구현할 수 없으며, react-error-boundary 라이브러리가 많이 사용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'React.Fragment의 용도를 설명해주세요.', '리액트.Fragment의 용도를 설명해주세요.', 'REACT_CORE',
 'Fragment는 추가 DOM 노드 없이 여러 자식 요소를 그룹화합니다. <React.Fragment> 또는 축약형 <></>를 사용합니다. key가 필요한 경우 <React.Fragment key={id}>를 사용해야 합니다. 불필요한 div 래퍼를 제거하여 DOM 구조를 깔끔하게 유지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'React에서 상태 끌어올리기(Lifting State Up)를 설명해주세요.', '리액트에서 상태 끌어올리기(Lifting State Up)를 설명해주세요.', 'STATE_MANAGEMENT',
 '여러 자식 컴포넌트가 동일한 상태를 공유해야 할 때, 상태를 가장 가까운 공통 부모로 이동시키는 패턴입니다. 부모가 상태와 변경 함수를 props로 내려줍니다. React의 단방향 데이터 흐름 원칙에 따른 기본 패턴이며, 너무 많이 올리면 Props Drilling이 발생할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'React에서 Strict Mode의 역할을 설명해주세요.', '리액트에서 Strict Mode의 역할을 설명해주세요.', 'REACT_CORE',
 'StrictMode는 개발 모드에서만 작동하며, 잠재적 문제를 감지합니다. 안전하지 않은 생명주기 메서드 경고, 부수효과 감지를 위한 이중 렌더링, deprecated API 사용 경고를 제공합니다. 프로덕션 빌드에서는 아무 영향이 없으며, 코드 품질 향상에 도움됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'React Router의 기본 사용법을 설명해주세요.', '리액트 Router의 기본 사용법을 설명해주세요.', 'REACT_CORE',
 'BrowserRouter로 앱을 감싸고, Routes 안에 Route 컴포넌트로 경로와 컴포넌트를 매핑합니다. Link/NavLink로 페이지 이동하며, useNavigate로 프로그래밍 방식으로 이동합니다. useParams로 URL 파라미터를, useSearchParams로 쿼리 스트링을 가져옵니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'React에서 컴포넌트 합성(Composition) 패턴을 설명해주세요.', '리액트에서 컴포넌트 합성(Composition) 패턴을 설명해주세요.', 'REACT_CORE',
 'children prop을 활용하여 컴포넌트 내부에 다른 컴포넌트를 삽입하는 패턴입니다. 상속보다 합성을 선호하며, Layout 컴포넌트, Modal, Card 등에 활용됩니다. Render Props, Higher-Order Component(HOC)도 합성 패턴이지만, 현재는 커스텀 Hook이 대부분의 사례를 대체합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'useState와 useReducer의 차이와 선택 기준을 설명해주세요.', 'useState와 useReducer의 차이와 선택 기준을 설명해주세요.', 'HOOKS',
 'useState는 단순한 상태에 적합하고, useReducer는 복잡한 상태 로직(여러 값이 연관, 이전 상태에 의존)에 적합합니다. useReducer는 action 기반으로 상태 변경을 명시적으로 관리하며, 테스트가 용이합니다. 상태 업데이트 로직이 3개 이상의 케이스를 가지면 useReducer를 고려합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'TypeScript에서 union type과 intersection type의 차이를 설명해주세요.', '타입스크립트에서 union type과 intersection type의 차이를 설명해주세요.', 'TYPESCRIPT',
 'Union type(A | B)은 A 또는 B 중 하나의 타입이며, 공통 속성만 접근 가능합니다. 타입 가드(typeof, in, instanceof)로 좁힐 수 있습니다. Intersection type(A & B)은 A와 B 모두의 속성을 가진 타입입니다. Props 확장이나 여러 인터페이스를 결합할 때 유용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'React DevTools의 주요 기능을 설명해주세요.', '리액트 DevTools의 주요 기능을 설명해주세요.', 'REACT_CORE',
 'Components 탭에서 컴포넌트 트리, props, state, hooks를 실시간으로 확인하고 수정할 수 있습니다. Profiler 탭에서 렌더링 성능을 측정하고 불필요한 리렌더링을 찾습니다. Highlight Updates로 리렌더링되는 컴포넌트를 시각적으로 확인할 수 있어 성능 최적화에 필수적입니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- ============================================================
-- LANGUAGE_FRAMEWORK — MID (30문항)
-- cache_key: FRONTEND:MID:REACT_TS:LANGUAGE_FRAMEWORK
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:LANGUAGE_FRAMEWORK', 'useMemo와 useCallback의 차이와 사용 시점을 설명해주세요.', 'useMemo와 useCallback의 차이와 사용 시점을 설명해주세요.', 'HOOKS',
 'useMemo는 계산 결과 값을 메모이제이션하고, useCallback은 함수 참조를 메모이제이션합니다. 자식 컴포넌트에 콜백을 전달할 때 useCallback으로 불필요한 리렌더링을 방지하고, 비용이 큰 계산에 useMemo를 사용합니다. 과도한 메모이제이션은 오히려 성능을 저하시킬 수 있으므로 Profiler로 확인 후 적용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:LANGUAGE_FRAMEWORK', 'React.memo의 동작 원리와 한계를 설명해주세요.', '리액트.memo의 동작 원리와 한계를 설명해주세요.', 'PERFORMANCE',
 'React.memo는 props의 얕은 비교(shallow comparison)로 변경이 없으면 리렌더링을 건너뛰는 HOC입니다. 객체/배열 props는 매 렌더링마다 새 참조가 생성되므로 useMemo/useCallback과 함께 사용해야 효과적입니다. 두 번째 인자로 커스텀 비교 함수를 전달할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:LANGUAGE_FRAMEWORK', 'React의 배치 업데이트(Batching)를 설명해주세요.', '리액트의 배치 업데이트(Batching)를 설명해주세요.', 'REACT_CORE',
 'React 18부터 모든 상태 업데이트가 자동으로 배치됩니다(이전에는 이벤트 핸들러 내에서만). 여러 setState 호출을 하나의 리렌더링으로 묶어 성능을 최적화합니다. flushSync()로 배치를 무시하고 즉시 DOM을 업데이트할 수 있지만 성능상 권장되지 않습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:LANGUAGE_FRAMEWORK', '커스텀 Hook 설계 시 고려사항을 설명해주세요.', '커스텀 훅 설계 시 고려사항을 설명해주세요.', 'HOOKS',
 'use 접두사를 붙이고, 하나의 관심사에 집중합니다. 반환값은 [value, setter] 배열 또는 객체 형태로 일관성 있게 설계합니다. Hook 내부에서 다른 Hook을 호출할 수 있어 조합이 가능합니다. 제네릭 타입으로 유연성을 높이고, 테스트를 위해 의존성을 주입 가능하게 설계합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:LANGUAGE_FRAMEWORK', 'Redux와 Zustand의 차이와 선택 기준을 설명해주세요.', '리덕스와 주스탄드의 차이와 선택 기준을 설명해주세요.', 'STATE_MANAGEMENT',
 'Redux는 단일 스토어, 불변 상태, 액션/리듀서 패턴으로 엄격한 구조를 제공하며, 미들웨어(Thunk, Saga)와 DevTools가 강력합니다. Zustand는 훨씬 간결한 API로 보일러플레이트가 적고, 직접 상태를 변경(immer 내장)하며 번들 크기가 작습니다. 대규모 팀은 Redux, 중소 프로젝트는 Zustand가 적합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:LANGUAGE_FRAMEWORK', 'TypeScript 제네릭을 React Props에 적용하는 방법을 설명해주세요.', '타입스크립트 제네릭을 리액트 Props에 적용하는 방법을 설명해주세요.', 'TYPESCRIPT',
 '제네릭 컴포넌트는 <T>로 타입 매개변수를 받아 다양한 데이터 타입에 대응합니다. 예: function List<T>({ items, renderItem }: { items: T[], renderItem: (item: T) => ReactNode })로 범용 리스트를 만들 수 있습니다. 타입 추론으로 사용처에서 명시적 타입 지정 없이도 동작합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:LANGUAGE_FRAMEWORK', 'React의 렌더링 최적화 전략 5가지를 설명해주세요.', '리액트의 렌더링 최적화 전략 5가지를 설명해주세요.', 'PERFORMANCE',
 '1) React.memo로 불필요한 리렌더링 방지, 2) useMemo/useCallback으로 참조 안정화, 3) 상태를 사용하는 컴포넌트 가까이 배치(state colocation), 4) 리스트 가상화(react-window), 5) 코드 스플리팅(React.lazy + Suspense). Profiler로 병목을 먼저 찾고 최적화를 적용해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:LANGUAGE_FRAMEWORK', 'useRef와 useState의 차이와 각각의 사용 사례를 설명해주세요.', 'useRef와 useState의 차이와 각각의 사용 사례를 설명해주세요.', 'HOOKS',
 'useState는 값 변경 시 리렌더링을 트리거하고 UI에 반영됩니다. useRef는 .current를 변경해도 리렌더링하지 않습니다. 이전 값 저장, 타이머/구독 ID 보관, DOM 접근에 useRef를 사용하고, 화면에 표시되는 데이터에는 useState를 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:LANGUAGE_FRAMEWORK', 'TanStack Query(React Query)의 핵심 개념을 설명해주세요.', 'TanStack Query(리액트 Query)의 핵심 개념을 설명해주세요.', 'STATE_MANAGEMENT',
 'TanStack Query는 서버 상태를 관리하는 라이브러리로, 캐싱, 자동 리패칭, 낙관적 업데이트를 제공합니다. useQuery로 데이터를 조회하고 useMutation으로 변경합니다. staleTime으로 캐시 유효 시간을, gcTime으로 가비지 컬렉션 시간을 설정합니다. 서버/클라이언트 상태를 명확히 분리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:LANGUAGE_FRAMEWORK', 'React에서 불변성(Immutability)을 유지해야 하는 이유를 설명해주세요.', '리액트에서 불변성(Immutability)을 유지해야 하는 이유를 설명해주세요.', 'REACT_CORE',
 'React는 상태 변경을 참조 비교(===)로 감지하므로, 객체를 직접 수정하면 변경을 인식하지 못합니다. 스프레드 연산자({...obj}), Array.map/filter, 또는 Immer 라이브러리로 새 객체를 생성해야 합니다. 불변성은 예측 가능한 상태 관리, 시간 여행 디버깅, 성능 최적화(React.memo)의 기반입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:LANGUAGE_FRAMEWORK', 'Higher-Order Component(HOC)와 커스텀 Hook의 차이를 설명해주세요.', '하이어 오더 컴포넌트(HOC)와 커스텀 훅의 차이를 설명해주세요.', 'REACT_CORE',
 'HOC는 컴포넌트를 감싸 새 컴포넌트를 반환하는 함수로, 래퍼 지옥(Wrapper Hell)과 props 충돌 문제가 있습니다. 커스텀 Hook은 로직만 재사용하고 컴포넌트 구조에 영향을 주지 않아 더 깔끔합니다. 현재는 대부분의 경우 커스텀 Hook이 HOC를 대체합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:LANGUAGE_FRAMEWORK', 'React.lazy와 Suspense를 사용한 코드 스플리팅을 설명해주세요.', '리액트.lazy와 Suspense를 사용한 코드 스플리팅을 설명해주세요.', 'PERFORMANCE',
 'React.lazy(() => import(''./Component''))로 동적 임포트하고, Suspense의 fallback prop으로 로딩 UI를 지정합니다. 라우트 기반 스플리팅이 가장 효과적이며, 초기 번들 크기를 줄여 First Load 성능을 개선합니다. webpack/Vite가 자동으로 청크를 분리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:LANGUAGE_FRAMEWORK', 'React에서 Portal의 용도와 사용법을 설명해주세요.', '리액트에서 Portal의 용도와 사용법을 설명해주세요.', 'REACT_CORE',
 'createPortal(child, container)로 컴포넌트를 부모 DOM 계층 외부에 렌더링합니다. Modal, Tooltip, Dropdown 등 z-index나 overflow 제약을 벗어나야 하는 UI에 사용됩니다. Portal로 렌더링해도 React 트리의 이벤트 버블링은 유지됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:LANGUAGE_FRAMEWORK', 'TypeScript의 타입 가드(Type Guard) 패턴을 설명해주세요.', '타입스크립트의 타입 가드(Type Guard) 패턴을 설명해주세요.', 'TYPESCRIPT',
 'typeof(기본형), instanceof(클래스), in(프로퍼티 존재), 사용자 정의 타입 가드(is 키워드)로 Union 타입을 좁힙니다. Discriminated Union은 공통 프로퍼티(type, kind)로 구분합니다. API 응답의 성공/실패, 폼 필드 타입 분기 등에 활용되며 타입 안전성을 높입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:LANGUAGE_FRAMEWORK', 'React에서 useTransition과 useDeferredValue의 용도를 설명해주세요.', '리액트에서 useTransition과 useDeferredValue의 용도를 설명해주세요.', 'PERFORMANCE',
 'useTransition은 상태 업데이트를 non-urgent로 표시하여, 긴급한 업데이트(입력)를 우선 처리합니다. startTransition 내 setState는 낮은 우선순위로 처리됩니다. useDeferredValue는 값의 지연된 버전을 제공하여, 무거운 리렌더링을 debounce 없이 지연시킵니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:LANGUAGE_FRAMEWORK', 'Zustand의 핵심 API와 사용 패턴을 설명해주세요.', '주스탄드의 핵심 에이피아이와 사용 패턴을 설명해주세요.', 'STATE_MANAGEMENT',
 'create()로 스토어를 생성하고, set()으로 상태를 변경합니다. 컴포넌트에서 useStore(selector)로 필요한 상태만 선택하여 불필요한 리렌더링을 방지합니다. 미들웨어(devtools, persist, immer)로 기능을 확장할 수 있으며, Redux DevTools와도 연동됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:LANGUAGE_FRAMEWORK', 'React에서 Suspense의 동작 원리를 설명해주세요.', '리액트에서 Suspense의 동작 원리를 설명해주세요.', 'REACT_CORE',
 'Suspense는 자식 컴포넌트가 아직 준비되지 않은 경우(Promise throw) fallback을 표시합니다. React.lazy의 코드 스플리팅, TanStack Query의 suspense 모드, Next.js의 데이터 페칭에서 사용됩니다. 중첩 Suspense로 로딩 상태를 세밀하게 제어할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:LANGUAGE_FRAMEWORK', 'React에서 forwardRef의 용도와 사용법을 설명해주세요.', '리액트에서 forwardRef의 용도와 사용법을 설명해주세요.', 'HOOKS',
 'forwardRef는 부모가 자식 컴포넌트의 DOM 요소에 ref를 전달할 수 있게 합니다. 재사용 가능한 Input, Button 등 UI 컴포넌트에서 필수적입니다. useImperativeHandle과 함께 사용하면 자식이 노출하는 메서드를 제한할 수 있습니다. React 19에서는 props로 직접 ref를 전달할 수 있게 됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:LANGUAGE_FRAMEWORK', 'TypeScript의 Utility Types(Partial, Required, Pick, Omit)을 설명해주세요.', '타입스크립트의 Utility Types(Partial, Required, Pick, Omit)을 설명해주세요.', 'TYPESCRIPT',
 'Partial<T>는 모든 속성을 선택적으로, Required<T>는 필수로 변환합니다. Pick<T, K>는 특정 속성만 추출하고, Omit<T, K>는 특정 속성을 제외합니다. 폼 상태(Partial<FormData>), API 응답 변환, Props 확장 시 유용하며, 타입 중복을 줄여줍니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:LANGUAGE_FRAMEWORK', 'React 테스트 전략(Unit, Integration, E2E)을 설명해주세요.', '리액트 테스트 전략(Unit, Integration, 이투이)을 설명해주세요.', 'REACT_CORE',
 'Unit 테스트는 유틸리티 함수나 커스텀 Hook을 Jest로 테스트합니다. Integration 테스트는 React Testing Library로 컴포넌트 상호작용을 테스트하며, 구현 세부사항이 아닌 사용자 행동 기반으로 작성합니다. E2E는 Playwright/Cypress로 실제 브라우저에서 전체 흐름을 테스트합니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- ============================================================
-- LANGUAGE_FRAMEWORK — SENIOR (30문항)
-- cache_key: FRONTEND:SENIOR:REACT_TS:LANGUAGE_FRAMEWORK
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'React Fiber 아키텍처의 핵심 개념을 설명해주세요.', '리액트 Fiber 아키텍처의 핵심 개념을 설명해주세요.', 'ARCHITECTURE',
 'Fiber는 React 16에서 도입된 재조정(Reconciliation) 엔진으로, 작업을 작은 단위(Fiber 노드)로 나누어 중단/재개가 가능합니다. 자체 스케줄러(MessageChannel 기반)로 우선순위 스케줄링을 수행하며, 긴급한 업데이트(입력)가 비긴급 업데이트(리스트 렌더링)를 선점합니다. 이를 통해 Concurrent Rendering이 가능해졌습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'Concurrent Rendering과 Suspense의 관계를 설명해주세요.', 'Concurrent Rendering과 Suspense의 관계를 설명해주세요.', 'ARCHITECTURE',
 'Concurrent Rendering은 React가 여러 버전의 UI를 동시에 준비할 수 있게 합니다. Suspense는 비동기 작업의 로딩 상태를 선언적으로 처리하며, Concurrent Mode에서 Selective Hydration, Streaming SSR과 결합됩니다. useTransition, useDeferredValue가 우선순위를 제어하여 응답성을 유지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'React Server Component(RSC)의 개념과 장점을 설명해주세요.', '리액트 Server Component(RSC)의 개념과 장점을 설명해주세요.', 'ARCHITECTURE',
 'RSC는 서버에서만 실행되는 컴포넌트로, 번들에 포함되지 않아 클라이언트 JS 크기를 줄입니다. 서버에서 직접 DB/API 접근이 가능하고, 결과만 직렬화하여 클라이언트에 스트리밍합니다. ''use client'' 지시어로 클라이언트 컴포넌트를 구분합니다. Next.js App Router가 RSC를 기본으로 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'React Profiler를 활용한 성능 분석 방법을 설명해주세요.', '리액트 Profiler를 활용한 성능 분석 방법을 설명해주세요.', 'PERFORMANCE',
 'React DevTools Profiler로 렌더링 시간, 커밋 빈도, 컴포넌트별 렌더링 원인을 분석합니다. Flamegraph에서 느린 컴포넌트를 식별하고, "Why did this render?" 기능으로 불필요한 리렌더링 원인을 파악합니다. Profiler API의 onRender 콜백으로 프로덕션에서도 성능 메트릭을 수집할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'TypeScript strict mode의 각 옵션과 효과를 설명해주세요.', '타입스크립트 strict mode의 각 옵션과 효과를 설명해주세요.', 'TYPESCRIPT',
 'strictNullChecks(null/undefined 엄격 검사), noImplicitAny(암시적 any 금지), strictFunctionTypes(함수 매개변수 타입의 반공변성 검사), strictPropertyInitialization(클래스 속성 초기화 강제) 등이 포함됩니다. strict: true로 모두 활성화하면 런타임 에러를 컴파일 타임에 잡아 코드 안전성이 크게 향상됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'React의 Selective Hydration과 Streaming SSR을 설명해주세요.', '리액트의 Selective Hydration과 Streaming 에스에스알을 설명해주세요.', 'ARCHITECTURE',
 'Streaming SSR은 renderToPipeableStream으로 HTML을 청크 단위로 스트리밍하여 TTFB를 줄입니다. Selective Hydration은 Suspense 경계별로 독립적으로 하이드레이션하여, 사용자 상호작용이 있는 부분을 우선 처리합니다. 이로써 대규모 앱에서도 인터랙티브까지의 시간(TTI)을 크게 줄입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'React 상태 관리 아키텍처(Flux, Atomic, Proxy)를 비교해주세요.', '리액트 상태 관리 아키텍처(Flux, Atomic, Proxy)를 비교해주세요.', 'STATE_MANAGEMENT',
 'Flux 패턴(Redux)은 단방향 데이터 흐름, 중앙 스토어, 액션/리듀서로 예측 가능합니다. Atomic 패턴(Jotai, Recoil)은 독립적인 원자 단위로 상태를 관리하여 세밀한 구독이 가능합니다. Proxy 패턴(Valtio, MobX)은 객체를 직접 수정하면 자동으로 반응합니다. 앱 규모와 팀 선호도에 따라 선택합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'React 19의 주요 변경사항을 설명해주세요.', '리액트 19의 주요 변경사항을 설명해주세요.', 'REACT_CORE',
 'React Compiler(자동 메모이제이션)로 useMemo/useCallback이 불필요해집니다. Actions(useActionState, useFormStatus)로 폼/비동기 처리가 간소화됩니다. use() Hook으로 Promise/Context를 직접 읽고, ref가 props로 전달 가능해집니다. Document Metadata, Stylesheet 우선순위 지원도 추가됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'Micro Frontend 아키텍처를 React에서 구현하는 방법을 설명해주세요.', 'Micro Frontend 아키텍처를 리액트에서 구현하는 방법을 설명해주세요.', 'ARCHITECTURE',
 'Module Federation(webpack 5)으로 런타임에 원격 모듈을 로드하거나, Web Components로 프레임워크 독립 통합합니다. 공유 의존성 관리, 스타일 격리(Shadow DOM, CSS Modules), 라우팅 통합, 전역 상태 공유가 핵심 과제입니다. single-spa가 대표적인 오케스트레이션 프레임워크입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'TypeScript에서 Conditional Types과 infer 키워드를 설명해주세요.', '타입스크립트에서 Conditional Types과 infer 키워드를 설명해주세요.', 'TYPESCRIPT',
 'Conditional Type은 T extends U ? X : Y 형태로 타입 수준의 분기를 수행합니다. infer는 조건부 타입 내에서 타입을 추론합니다. ReturnType<T>는 T extends (...args: any) => infer R ? R : never로 구현됩니다. 복잡한 타입 변환, API 응답 타입 추출 등에 활용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'React에서 메모리 누수를 방지하는 방법을 설명해주세요.', '리액트에서 메모리 누수를 방지하는 방법을 설명해주세요.', 'PERFORMANCE',
 'useEffect cleanup에서 구독 해제, 타이머 정리, AbortController로 fetch 취소를 수행합니다. 이벤트 리스너를 제거하지 않거나, 언마운트된 컴포넌트에서 setState를 호출하면 메모리 누수가 발생합니다. WeakRef, FinalizationRegistry로 참조를 관리하고, Chrome DevTools Memory 탭으로 힙 스냅샷을 분석합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'React Testing Library의 철학과 best practice를 설명해주세요.', '리액트 Testing Library의 철학과 best practice를 설명해주세요.', 'REACT_CORE',
 '구현 세부사항이 아닌 사용자 관점에서 테스트합니다. getByRole, getByText 등 접근성 기반 쿼리를 우선 사용하고, testID는 최후 수단입니다. userEvent로 실제 사용자 상호작용을 시뮬레이션합니다. 컴포넌트 내부 상태나 메서드를 직접 테스트하지 않으며, 통합 테스트를 선호합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'Next.js의 App Router와 Pages Router의 차이를 설명해주세요.', 'Next.js의 App Router와 Pages Router의 차이를 설명해주세요.', 'ARCHITECTURE',
 'App Router는 RSC 기반으로 서버 컴포넌트가 기본이며, 레이아웃 중첩, 병렬 라우트, 인터셉팅 라우트를 지원합니다. Pages Router는 getServerSideProps/getStaticProps로 데이터를 페칭합니다. App Router는 스트리밍, 부분 렌더링, 서버 액션을 지원하여 더 세밀한 성능 최적화가 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'React에서 대규모 폼을 효율적으로 관리하는 전략을 설명해주세요.', '리액트에서 대규모 폼을 효율적으로 관리하는 전략을 설명해주세요.', 'PERFORMANCE',
 'react-hook-form의 비제어 컴포넌트 방식으로 리렌더링을 최소화합니다. Zod/Yup으로 스키마 기반 유효성 검사를 수행하고, useFieldArray로 동적 필드를 관리합니다. 폼 상태를 글로벌 스토어가 아닌 폼 라이브러리 내부에 유지하여 다른 컴포넌트 리렌더링을 방지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'React에서 에러 처리 전략(Error Handling)을 체계적으로 설명해주세요.', '리액트에서 에러 처리 전략(Error Handling)을 체계적으로 설명해주세요.', 'ARCHITECTURE',
 'Error Boundary로 렌더링 에러를 잡고 폴백 UI를 표시합니다. API 에러는 TanStack Query의 onError/isError로 처리하고, 전역 에러 핸들링은 window.onerror + Sentry로 모니터링합니다. Suspense + Error Boundary 조합으로 로딩/에러 상태를 선언적으로 관리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'React 앱의 번들 최적화 전략을 설명해주세요.', '리액트 앱의 번들 최적화 전략을 설명해주세요.', 'PERFORMANCE',
 'Tree-shaking(사용하지 않는 코드 제거), 코드 스플리팅(React.lazy), 동적 임포트, CSS 추출, 이미지 최적화(next/image)를 적용합니다. webpack-bundle-analyzer로 번들 구성을 분석하고, 큰 라이브러리를 경량 대안으로 교체합니다(moment→dayjs). Preload/Prefetch로 핵심 리소스를 우선 로드합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'TypeScript에서 Template Literal Types의 활용을 설명해주세요.', '타입스크립트에서 템플릿 리터럴 Types의 활용을 설명해주세요.', 'TYPESCRIPT',
 'Template Literal Types는 문자열 리터럴을 조합하여 새 타입을 생성합니다. type Route = \`/api/${string}\`으로 URL 패턴을 타입화하거나, 이벤트 이름(on${Capitalize<EventName>})을 자동 생성합니다. Mapped Types와 결합하여 객체 키를 동적으로 변환하는 패턴에 강력합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'React 앱의 접근성(a11y)을 체계적으로 보장하는 방법을 설명해주세요.', '리액트 앱의 접근성(접근성)을 체계적으로 보장하는 방법을 설명해주세요.', 'ARCHITECTURE',
 'WCAG 2.1 AA 기준을 따르며, 시맨틱 HTML, ARIA 속성, 키보드 네비게이션, 포커스 관리를 구현합니다. eslint-plugin-jsx-a11y로 정적 분석, axe-core로 런타임 검사, 스크린 리더(VoiceOver/NVDA) 테스트를 수행합니다. Headless UI 라이브러리(Radix, React Aria)는 접근성이 내장되어 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'React 앱의 국제화(i18n) 구현 전략을 설명해주세요.', '리액트 앱의 국제화(국제화) 구현 전략을 설명해주세요.', 'ARCHITECTURE',
 'react-intl 또는 next-intl로 메시지 포맷팅, 복수형, 날짜/숫자 로컬라이징을 처리합니다. 번역 파일은 JSON으로 관리하고, 네임스페이스로 분리합니다. SSR에서는 서버 측에서 로케일을 감지하고, 동적 임포트로 필요한 번역만 로드합니다. RTL(아랍어 등) 레이아웃도 고려해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- ============================================================
-- UI_FRAMEWORK — JUNIOR (30문항)
-- cache_key: FRONTEND:JUNIOR:REACT_TS:UI_FRAMEWORK
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:UI_FRAMEWORK', 'JSX란 무엇이고 JavaScript와 어떻게 다른가요?', 'JSX란 무엇이고 자바스크립트와 어떻게 다른가요?', 'COMPONENT',
 'JSX는 JavaScript의 확장 문법으로 HTML과 유사한 마크업을 JavaScript 안에서 작성할 수 있게 합니다. Babel이 JSX를 React.createElement() 호출로 변환합니다. class 대신 className, for 대신 htmlFor를 사용하며, 모든 태그는 닫혀야 합니다. 표현식은 {}로 감쌉니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:UI_FRAMEWORK', 'CSS Modules, styled-components, Tailwind CSS의 차이를 설명해주세요.', '씨에스에스 모듈, 스타일드-컴포넌트, Tailwind 씨에스에스의 차이를 설명해주세요.', 'STYLING',
 'CSS Modules는 클래스명을 자동으로 스코핑하여 충돌을 방지합니다. styled-components는 CSS-in-JS로 컴포넌트에 스타일을 캡슐화하고 동적 스타일링이 용이합니다. Tailwind CSS는 유틸리티 퍼스트 접근으로 클래스 조합으로 스타일링하며 일관된 디자인 시스템을 제공합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:UI_FRAMEWORK', 'React에서 인라인 스타일의 장단점을 설명해주세요.', '리액트에서 인라인 스타일의 장단점을 설명해주세요.', 'STYLING',
 '인라인 스타일은 style={{}} 객체로 작성하며, 동적 스타일 적용이 간편합니다. JavaScript 객체이므로 camelCase 속성명(backgroundColor)을 사용합니다. 단점으로 :hover 같은 의사 클래스 사용 불가, 미디어 쿼리 미지원, 캐싱 불가, 성능 이슈가 있어 실무에서는 제한적으로 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:UI_FRAMEWORK', 'React에서 이벤트 핸들링 시 주의사항을 설명해주세요.', '리액트에서 이벤트 핸들링 시 주의사항을 설명해주세요.', 'EVENT_HANDLING',
 'onClick={handler}처럼 함수 참조를 전달해야 하며, onClick={handler()}로 호출하면 렌더링 시 즉시 실행됩니다. 매개변수 전달은 onClick={() => handler(id)} 화살표 함수를 사용합니다. SyntheticEvent는 풀링되므로(React 17 이전) 비동기에서 사용 시 e.persist()가 필요했습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:UI_FRAMEWORK', '제어 컴포넌트(Controlled)와 비제어 컴포넌트(Uncontrolled)의 차이를 설명해주세요.', '제어 컴포넌트(Controlled)와 비제어 컴포넌트(Uncontrolled)의 차이를 설명해주세요.', 'COMPONENT',
 '제어 컴포넌트는 value와 onChange로 React 상태와 동기화하여 모든 입력을 React가 관리합니다. 비제어 컴포넌트는 ref로 DOM에서 직접 값을 가져옵니다. 제어 방식은 유효성 검사, 포맷팅에 유리하고, 비제어 방식은 성능이 좋아 대규모 폼에 적합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:UI_FRAMEWORK', '조건부 렌더링에서 && 연산자 사용 시 주의사항을 설명해주세요.', '조건부 렌더링에서 && 연산자 사용 시 주의사항을 설명해주세요.', 'COMPONENT',
 '{count && <Component/>}에서 count가 0이면 false가 아닌 0이 렌더링됩니다. 이를 방지하려면 {count > 0 && <Component/>} 또는 {!!count && <Component/>}로 명시적 boolean 변환을 해야 합니다. 삼항 연산자(count ? <Comp/> : null)가 더 안전합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:UI_FRAMEWORK', 'Tailwind CSS의 장단점을 설명해주세요.', 'Tailwind 씨에스에스의 장단점을 설명해주세요.', 'STYLING',
 '장점은 클래스명 고민 없이 빠른 스타일링, 일관된 디자인 토큰, PurgeCSS로 미사용 스타일 제거, 반응형 유틸리티(sm:, md:, lg:)입니다. 단점은 HTML이 장황해지고, 커스텀 디자인에 config 수정이 필요하며, 학습 곡선이 있습니다. cn() 유틸리티(clsx + tailwind-merge)로 조건부 클래스를 관리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:UI_FRAMEWORK', 'React에서 이미지를 최적화하는 방법을 설명해주세요.', '리액트에서 이미지를 최적화하는 방법을 설명해주세요.', 'COMPONENT',
 'Next.js의 Image 컴포넌트는 자동 리사이즈, WebP/AVIF 변환, lazy loading을 제공합니다. loading="lazy"로 뷰포트 진입 시 로드하고, width/height를 지정하여 CLS(Cumulative Layout Shift)를 방지합니다. SVG는 인라인으로, 아이콘은 스프라이트나 아이콘 라이브러리(Lucide)를 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:UI_FRAMEWORK', 'CSS Flexbox와 Grid의 차이와 선택 기준을 설명해주세요.', '씨에스에스 Flexbox와 Grid의 차이와 선택 기준을 설명해주세요.', 'STYLING',
 'Flexbox는 1차원(행 또는 열) 레이아웃에 적합하며 정렬, 순서 변경, 유연한 크기 조정에 강합니다. Grid는 2차원(행+열) 레이아웃에 적합하며 복잡한 그리드 구조를 선언적으로 정의합니다. 네비게이션, 카드 정렬에는 Flexbox, 전체 페이지 레이아웃에는 Grid를 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:UI_FRAMEWORK', 'React에서 모달(Modal) 컴포넌트를 구현할 때 고려사항을 설명해주세요.', '리액트에서 모달(모달) 컴포넌트를 구현할 때 고려사항을 설명해주세요.', 'COMPONENT',
 'Portal로 body에 렌더링하여 z-index/overflow 문제를 해결합니다. 배경 클릭과 ESC 키로 닫기, 포커스 트래핑(모달 내에서만 Tab 이동), 스크롤 잠금(body overflow hidden)을 구현해야 합니다. aria-modal, role="dialog"로 접근성을 보장하고, 열릴 때 첫 포커스 가능 요소에 포커스를 설정합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:UI_FRAMEWORK', 'React에서 반응형 디자인을 구현하는 방법을 설명해주세요.', '리액트에서 반응형 디자인을 구현하는 방법을 설명해주세요.', 'STYLING',
 'CSS 미디어 쿼리(@media)로 브레이크포인트별 스타일을 적용합니다. Tailwind의 반응형 접두사(sm:, md:, lg:)가 편리합니다. useMediaQuery 커스텀 Hook으로 JavaScript에서 뷰포트를 감지하여 조건부 렌더링할 수 있습니다. 모바일 퍼스트 접근(min-width)이 권장됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:UI_FRAMEWORK', 'CSS 변수(Custom Properties)를 React에서 활용하는 방법을 설명해주세요.', '씨에스에스 변수(커스텀 프로퍼티)를 리액트에서 활용하는 방법을 설명해주세요.', 'STYLING',
 ':root에 --primary-color 같은 CSS 변수를 정의하고 var(--primary-color)로 참조합니다. JavaScript에서 element.style.setProperty(''--color'', value)로 동적 변경이 가능합니다. 테마 전환, 다크 모드에 유용하며, Tailwind과 조합하면 디자인 토큰을 체계적으로 관리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:UI_FRAMEWORK', 'React에서 로딩 상태를 표시하는 다양한 방법을 설명해주세요.', '리액트에서 로딩 상태를 표시하는 다양한 방법을 설명해주세요.', 'COMPONENT',
 'isLoading 상태 변수로 조건부 렌더링(스피너, 스켈레톤)하거나, Suspense fallback으로 선언적 처리합니다. 스켈레톤 UI가 스피너보다 사용자 경험이 좋으며, 콘텐츠 레이아웃을 유지합니다. TanStack Query의 isLoading/isFetching으로 초기 로딩과 백그라운드 리패칭을 구분합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:UI_FRAMEWORK', 'React에서 토스트(Toast) 알림을 구현하는 방법을 설명해주세요.', '리액트에서 토스트(토스트) 알림을 구현하는 방법을 설명해주세요.', 'COMPONENT',
 'Portal로 화면 상단/하단에 렌더링하고, 타이머로 자동 닫힘을 구현합니다. Context API로 전역에서 toast.success()/error()를 호출 가능하게 합니다. react-hot-toast, sonner 같은 라이브러리는 큐잉, 애니메이션, 접근성을 내장합니다. aria-live="polite"로 스크린 리더에 알립니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:UI_FRAMEWORK', 'React에서 다크 모드를 구현하는 방법을 설명해주세요.', '리액트에서 다크 모드를 구현하는 방법을 설명해주세요.', 'STYLING',
 'CSS 변수 + data-theme 속성으로 테마를 전환합니다. Tailwind은 dark: 접두사로 다크 모드 스타일을 정의합니다. prefers-color-scheme 미디어 쿼리로 시스템 설정을 감지하고, localStorage에 사용자 선호를 저장합니다. Context로 테마 상태를 전역 관리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- ============================================================
-- UI_FRAMEWORK — MID (30문항)
-- cache_key: FRONTEND:MID:REACT_TS:UI_FRAMEWORK
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:UI_FRAMEWORK', '디자인 시스템 구축 시 고려사항을 설명해주세요.', '디자인 시스템 구축 시 고려사항을 설명해주세요.', 'DESIGN_SYSTEM',
 '디자인 토큰(색상, 타이포그래피, 스페이싱)을 정의하고, 원자적 컴포넌트(Button, Input)에서 복합 컴포넌트(Form, Card)로 계층화합니다. 접근성(WCAG 2.1 AA), 반응형, 테마 지원을 기본으로 하고, Storybook으로 문서화합니다. Chromatic으로 시각적 회귀 테스트를 자동화합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:UI_FRAMEWORK', 'Compound Component 패턴을 설명해주세요.', '컴파운드 컴포넌트 패턴을 설명해주세요.', 'DESIGN_SYSTEM',
 'Compound Component는 관련 컴포넌트들이 암묵적으로 상태를 공유하는 패턴입니다. <Select>, <Select.Option> 처럼 부모가 Context로 상태를 공유하고 자식이 소비합니다. API가 선언적이고 유연하며, Radix UI, Headless UI가 이 패턴을 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:UI_FRAMEWORK', 'Headless UI 라이브러리의 개념과 장점을 설명해주세요.', '헤드리스 유아이 라이브러리의 개념과 장점을 설명해주세요.', 'DESIGN_SYSTEM',
 'Headless UI는 로직(상태, 키보드 상호작용, 접근성)만 제공하고 스타일은 사용자에게 맡기는 라이브러리입니다. Radix UI, React Aria, Headless UI(Tailwind Labs)가 대표적입니다. 커스텀 디자인 시스템과 결합하기 쉽고, 접근성이 내장되어 직접 구현하는 것보다 안전합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:UI_FRAMEWORK', '웹 접근성(a11y)의 핵심 원칙과 React에서의 구현을 설명해주세요.', '웹 접근성(접근성)의 핵심 원칙과 리액트에서의 구현을 설명해주세요.', 'ACCESSIBILITY',
 'POUR 원칙: 인식 가능(Perceivable), 운용 가능(Operable), 이해 가능(Understandable), 견고(Robust). React에서는 시맨틱 HTML, ARIA 속성(role, aria-label, aria-describedby), 키보드 네비게이션(tabIndex, onKeyDown), 포커스 관리(autoFocus, focus trap)를 구현합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:UI_FRAMEWORK', 'Framer Motion을 활용한 React 애니메이션 구현을 설명해주세요.', 'Framer Motion을 활용한 리액트 애니메이션 구현을 설명해주세요.', 'ANIMATION',
 'motion.div에 initial, animate, exit props로 선언적 애니메이션을 정의합니다. AnimatePresence로 컴포넌트 언마운트 애니메이션을 처리하고, variants로 복잡한 시퀀스를 관리합니다. layout prop으로 레이아웃 변경 애니메이션, useScroll로 스크롤 기반 애니메이션을 구현합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:UI_FRAMEWORK', 'Storybook의 역할과 활용 방법을 설명해주세요.', 'Storybook의 역할과 활용 방법을 설명해주세요.', 'DESIGN_SYSTEM',
 'Storybook은 컴포넌트를 독립적으로 개발/테스트/문서화하는 도구입니다. Story 파일로 다양한 상태(기본, 에러, 로딩)를 정의하고, Controls addon으로 props를 동적 조작합니다. Chromatic으로 시각적 회귀 테스트, a11y addon으로 접근성 검사를 자동화합니다. 디자이너-개발자 협업 도구로도 활용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:UI_FRAMEWORK', 'React에서 가상 스크롤(Virtualization)을 구현하는 방법을 설명해주세요.', '리액트에서 가상 스크롤(버추얼라이제이션)을 구현하는 방법을 설명해주세요.', 'PERFORMANCE',
 'react-window나 @tanstack/react-virtual로 뷰포트에 보이는 항목만 렌더링합니다. 대규모 리스트(수천 개)에서 DOM 노드 수를 제한하여 메모리와 렌더링 성능을 개선합니다. 고정 높이(FixedSizeList)와 가변 높이(VariableSizeList)를 지원하며, 무한 스크롤과 결합할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:UI_FRAMEWORK', 'CSS-in-JS의 런타임 성능 이슈와 대안을 설명해주세요.', '씨에스에스-in-JS의 런타임 성능 이슈와 대안을 설명해주세요.', 'STYLING',
 'styled-components, Emotion은 런타임에 스타일을 생성/주입하여 초기 렌더링과 리렌더링 시 오버헤드가 있습니다. 제로 런타임 CSS-in-JS(vanilla-extract, Panda CSS, StyleX)는 빌드 타임에 CSS를 추출하여 런타임 비용을 제거합니다. Tailwind CSS도 제로 런타임이며 현재 가장 인기있는 대안입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:UI_FRAMEWORK', 'React에서 드래그 앤 드롭을 구현하는 방법을 설명해주세요.', '리액트에서 드래그 앤 드롭을 구현하는 방법을 설명해주세요.', 'COMPONENT',
 'HTML5 Drag and Drop API는 기본적이지만 제한적입니다. dnd-kit이나 @hello-pangea/dnd(react-beautiful-dnd 후속)가 선호됩니다. 접근성(키보드 드래그), 가상화 리스트 호환, 다중 리스트 간 이동을 지원합니다. 터치 디바이스에서도 동작하도록 포인터 이벤트를 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:UI_FRAMEWORK', 'React에서 테이블 컴포넌트를 효율적으로 구현하는 방법을 설명해주세요.', '리액트에서 테이블 컴포넌트를 효율적으로 구현하는 방법을 설명해주세요.', 'COMPONENT',
 '@tanstack/react-table(Headless)로 정렬, 필터링, 페이지네이션, 컬럼 리사이즈 로직을 처리하고 UI는 직접 구현합니다. 대규모 데이터는 가상화(react-window)와 서버 사이드 페이지네이션을 결합합니다. 접근성을 위해 시맨틱 table/thead/tbody 태그와 scope 속성을 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:UI_FRAMEWORK', 'React에서 Chart/데이터 시각화를 구현하는 방법을 설명해주세요.', '리액트에서 Chart/데이터 시각화를 구현하는 방법을 설명해주세요.', 'COMPONENT',
 'Recharts(선언적 React 네이티브), Chart.js + react-chartjs-2(캔버스 기반), D3.js(저수준 자유도)가 대표적입니다. 선/막대/파이 등 기본 차트는 Recharts가 간편하고, 복잡한 커스텀 시각화는 D3.js가 적합합니다. 반응형(ResponsiveContainer), 접근성(대체 텍스트), 성능(데이터 포인트 제한)을 고려합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:UI_FRAMEWORK', 'React에서 무한 스크롤을 구현하는 방법을 설명해주세요.', '리액트에서 무한 스크롤을 구현하는 방법을 설명해주세요.', 'COMPONENT',
 'Intersection Observer API로 목록 끝의 sentinel 요소가 뷰포트에 진입하면 다음 페이지를 로드합니다. TanStack Query의 useInfiniteQuery가 편리하며, getNextPageParam으로 다음 페이지를 자동 관리합니다. 가상화와 결합하여 DOM 노드 수를 제한하고, 로딩/에러/빈 상태를 처리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:UI_FRAMEWORK', 'React에서 애니메이션 성능을 최적화하는 방법을 설명해주세요.', '리액트에서 애니메이션 성능을 최적화하는 방법을 설명해주세요.', 'ANIMATION',
 'transform과 opacity만 사용하여 GPU 가속(Composite Layer)을 활용합니다. will-change로 브라우저에 최적화 힌트를 주고, requestAnimationFrame으로 프레임 동기화합니다. layout trigger(width, height 변경)를 피하고, FLIP(First, Last, Invert, Play) 기법으로 레이아웃 애니메이션을 최적화합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:UI_FRAMEWORK', 'shadcn/ui의 특징과 기존 UI 라이브러리와의 차이를 설명해주세요.', 'shadcn/ui의 특징과 기존 유아이 라이브러리와의 차이를 설명해주세요.', 'DESIGN_SYSTEM',
 'shadcn/ui는 npm 패키지가 아닌 복사-붙여넣기 방식으로 컴포넌트 소스 코드를 직접 프로젝트에 추가합니다. Radix UI(접근성) + Tailwind CSS(스타일)를 기반으로 하며, 코드를 직접 수정할 수 있어 커스터마이징이 자유롭습니다. 의존성이 줄고 번들에 사용하는 컴포넌트만 포함됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:UI_FRAMEWORK', 'React에서 키보드 네비게이션을 구현하는 방법을 설명해주세요.', '리액트에서 키보드 네비게이션을 구현하는 방법을 설명해주세요.', 'ACCESSIBILITY',
 'tabIndex로 포커스 순서를 관리하고, onKeyDown에서 Enter/Space(활성화), Arrow keys(목록 탐색), Escape(닫기)를 처리합니다. role="menu", role="option" 등 ARIA 역할에 맞는 키보드 패턴을 구현합니다. 포커스 트랩(모달), 로빙 탭인덱스(탭 목록)가 주요 패턴입니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- ============================================================
-- UI_FRAMEWORK — SENIOR (30문항)
-- cache_key: FRONTEND:SENIOR:REACT_TS:UI_FRAMEWORK
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:UI_FRAMEWORK', 'Micro Frontend 아키텍처의 스타일 격리 전략을 설명해주세요.', '마이크로 프론트엔드 아키텍처의 스타일 격리 전략을 설명해주세요.', 'ARCHITECTURE',
 'Shadow DOM으로 완전한 CSS 격리를 제공하지만 서드파티 스타일 적용이 어렵습니다. CSS Modules/CSS-in-JS는 클래스명 스코핑으로 충돌을 방지합니다. 네임스페이스 접두사(BEM)도 가능합니다. 글로벌 디자인 토큰은 CSS 변수로 공유하고, 리셋 CSS는 각 마이크로 앱이 독립적으로 적용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:UI_FRAMEWORK', 'Web Components를 React와 통합하는 방법을 설명해주세요.', 'Web Components를 리액트와 통합하는 방법을 설명해주세요.', 'ARCHITECTURE',
 'Web Components(Custom Elements + Shadow DOM + HTML Templates)는 프레임워크 독립적입니다. React에서 사용 시 ref로 DOM 속성을 설정하고, addEventListener로 커스텀 이벤트를 처리합니다. React 19에서는 Web Components 지원이 개선됩니다. 디자인 시스템의 프레임워크 독립 배포에 유용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:UI_FRAMEWORK', 'Core Web Vitals(LCP, FID, CLS)를 최적화하는 방법을 설명해주세요.', '코어 웹 바이탈(엘씨피, 에프아이디, 씨엘에스)를 최적화하는 방법을 설명해주세요.', 'PERFORMANCE',
 'LCP(Largest Contentful Paint): 히어로 이미지를 preload, CDN, 적절한 포맷(WebP/AVIF)으로 최적화합니다. INP(Interaction to Next Paint, FID를 대체): 메인 스레드 블록을 줄이고, 이벤트 핸들러를 경량화합니다. CLS(Cumulative Layout Shift): 이미지/광고에 크기를 명시하고, 동적 콘텐츠 삽입을 최소화합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:UI_FRAMEWORK', '번들 사이즈를 분석하고 최적화하는 방법을 설명해주세요.', '번들 사이즈를 분석하고 최적화하는 방법을 설명해주세요.', 'OPTIMIZATION',
 'webpack-bundle-analyzer 또는 source-map-explorer로 번들 구성을 시각화합니다. 큰 의존성을 경량 대안으로 교체(lodash→lodash-es 또는 개별 import), Tree-shaking 가능한 ESM 패키지 선택, 동적 임포트로 코드 스플리팅을 적용합니다. gzip/brotli 압축, CDN 캐싱도 필수입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:UI_FRAMEWORK', 'Tree-shaking의 원리와 효과적으로 활용하는 방법을 설명해주세요.', '트리 셰이킹의 원리와 효과적으로 활용하는 방법을 설명해주세요.', 'OPTIMIZATION',
 'Tree-shaking은 번들러가 정적 분석으로 사용되지 않는 export를 제거하는 최적화입니다. ESM(import/export)에서만 동작하며, CJS(require)는 불가합니다. package.json의 sideEffects: false로 부수효과 없음을 명시하고, 배럴 파일(index.ts)을 최소화합니다. 개별 import(import { map } from ''lodash-es'')가 효과적입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:UI_FRAMEWORK', 'Code Splitting 전략을 설명해주세요.', '코드 스플리팅 전략을 설명해주세요.', 'OPTIMIZATION',
 '라우트 기반(각 페이지별 청크), 컴포넌트 기반(모달, 차트 등 무거운 UI), 라이브러리 기반(vendor 청크 분리)이 있습니다. React.lazy + Suspense로 구현하고, preload/prefetch로 사용자 행동을 예측하여 미리 로드합니다. webpack의 splitChunks로 공통 모듈을 추출하여 캐싱 효율을 높입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:UI_FRAMEWORK', 'React 앱의 성능을 측정하고 모니터링하는 방법을 설명해주세요.', '리액트 앱의 성능을 측정하고 모니터링하는 방법을 설명해주세요.', 'PERFORMANCE',
 'Lighthouse CI로 빌드마다 Core Web Vitals를 측정하고, web-vitals 라이브러리로 실 사용자 데이터(RUM)를 수집합니다. React Profiler API로 컴포넌트 렌더링 시간을 추적하고, Performance Observer로 Long Tasks를 감지합니다. Sentry Performance, Datadog RUM으로 프로덕션 모니터링합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:UI_FRAMEWORK', 'Service Worker와 PWA를 React에서 구현하는 방법을 설명해주세요.', 'Service Worker와 PWA를 리액트에서 구현하는 방법을 설명해주세요.', 'ARCHITECTURE',
 'Service Worker는 네트워크 프록시로 오프라인 캐싱, 백그라운드 동기화, 푸시 알림을 제공합니다. Workbox로 캐싱 전략(Cache First, Network First, Stale While Revalidate)을 설정합니다. manifest.json으로 앱 메타데이터를 정의하고, beforeinstallprompt 이벤트로 설치를 유도합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:UI_FRAMEWORK', 'React에서 복잡한 애니메이션 시퀀스를 관리하는 방법을 설명해주세요.', '리액트에서 복잡한 애니메이션 시퀀스를 관리하는 방법을 설명해주세요.', 'ANIMATION',
 'Framer Motion의 variants와 staggerChildren으로 연쇄 애니메이션을 선언적으로 정의합니다. useAnimate로 명령형 시퀀스를 구성하고, LayoutGroup으로 컴포넌트 간 레이아웃 전환을 부드럽게 처리합니다. GSAP의 Timeline으로 정밀한 시퀀스를 제어할 수도 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:UI_FRAMEWORK', 'Design Token 시스템을 구축하는 방법을 설명해주세요.', '디자인 토큰 시스템을 구축하는 방법을 설명해주세요.', 'DESIGN_SYSTEM',
 'Design Token은 색상, 타이포그래피, 스페이싱 등 디자인 결정을 구조화된 데이터(JSON)로 표현합니다. Style Dictionary로 토큰을 CSS 변수, Tailwind config, iOS/Android 포맷으로 변환합니다. Figma Tokens 플러그인으로 디자인 도구와 코드를 동기화하고, 시맨틱 토큰(color.primary vs blue-500)으로 의도를 명확히 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:UI_FRAMEWORK', 'React 앱에서 SEO를 최적화하는 방법을 설명해주세요.', '리액트 앱에서 SEO를 최적화하는 방법을 설명해주세요.', 'ARCHITECTURE',
 'SSR(Next.js) 또는 SSG로 크롤러가 완전한 HTML을 받도록 합니다. next/head 또는 Helmet으로 메타 태그(title, description, og:*)를 페이지별로 설정합니다. 구조화된 데이터(JSON-LD), sitemap.xml, robots.txt를 제공하고, 시맨틱 HTML을 사용합니다. Core Web Vitals도 SEO 순위에 영향을 미칩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:UI_FRAMEWORK', 'React Native Web을 활용한 크로스 플랫폼 UI 전략을 설명해주세요.', '리액트 Native Web을 활용한 크로스 플랫폼 유아이 전략을 설명해주세요.', 'ARCHITECTURE',
 'React Native Web은 React Native 컴포넌트를 웹에서 렌더링하여 코드 공유를 극대화합니다. View, Text, StyleSheet API를 사용하고, Platform.select로 플랫폼별 분기합니다. 대안으로 Tamagui는 컴파일 타임 최적화로 성능을 개선하고, Expo가 통합 개발 환경을 제공합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:UI_FRAMEWORK', 'React에서 복잡한 폼 위저드(Multi-step Form)를 설계하는 방법을 설명해주세요.', '리액트에서 복잡한 폼 위저드(Multi-step Form)를 설계하는 방법을 설명해주세요.', 'COMPONENT',
 '각 스텝을 독립 컴포넌트로 분리하고, 전체 폼 상태를 Context나 Zustand로 관리합니다. 스텝 간 네비게이션, 유효성 검사(스텝별/전체), 이전 스텝으로 돌아가기, 진행률 표시를 구현합니다. URL과 동기화하여 새로고침 시에도 현재 스텝을 유지하고, 드래프트 저장으로 이탈 시 데이터를 보존합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:UI_FRAMEWORK', 'React 앱의 보안(XSS, CSRF) 방어 전략을 설명해주세요.', '리액트 앱의 보안(엑스에스에스, 씨에스아르에프) 방어 전략을 설명해주세요.', 'ARCHITECTURE',
 'React는 기본적으로 JSX 출력을 이스케이프하여 XSS를 방지하지만, dangerouslySetInnerHTML 사용 시 DOMPurify로 살균해야 합니다. CSP(Content Security Policy) 헤더로 인라인 스크립트를 차단합니다. CSRF는 SameSite=Strict 쿠키와 CSRF 토큰으로 방어합니다. 사용자 입력은 항상 서버에서 재검증합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:UI_FRAMEWORK', 'React 앱의 에러 모니터링과 로깅 전략을 설명해주세요.', '리액트 앱의 에러 모니터링과 로깅 전략을 설명해주세요.', 'ARCHITECTURE',
 'Sentry로 프로덕션 에러를 자동 수집하고, Source Map으로 원본 코드 위치를 추적합니다. Error Boundary에서 에러를 Sentry에 보고하고 사용자에게 폴백 UI를 제공합니다. 커스텀 로깅(console 래퍼)으로 개발/프로덕션 로그를 분리하고, 성능 메트릭(Web Vitals)도 함께 수집합니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- ============================================================
-- 추가 68문항 (LANGUAGE_FRAMEWORK 및 UI_FRAMEWORK 보충)
-- ============================================================

-- 추가 2문항 (LANGUAGE_FRAMEWORK JUNIOR)
INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'React에서 Portal을 사용하는 이유와 사용법을 설명해주세요.', '리액트에서 Portal을 사용하는 이유와 사용법을 설명해주세요.', 'REACT_CORE',
 'Portal은 부모 컴포넌트 DOM 계층 외부에 자식을 렌더링합니다. ReactDOM.createPortal(child, container)로 생성하며, 모달, 툴팁, 드롭다운처럼 z-index나 overflow:hidden에 영향을 받는 경우에 사용합니다. Portal 내부 이벤트는 React 트리를 따라 버블링되어 부모 컴포넌트에서 처리할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'React에서 lazy loading과 Suspense를 사용하는 방법을 설명해주세요.', '리액트에서 lazy loading과 Suspense를 사용하는 방법을 설명해주세요.', 'REACT_CORE',
 'React.lazy()로 컴포넌트를 동적 임포트하면 해당 번들을 필요할 때만 로드합니다. Suspense로 감싸면 로딩 중 fallback UI를 표시합니다. 라우트 레벨에서 적용하면 페이지별 코드 스플리팅이 가능하고 초기 번들 크기를 줄입니다. React 18의 Suspense는 데이터 페칭에도 활용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- 추가 10문항 (LANGUAGE_FRAMEWORK MID)
INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:LANGUAGE_FRAMEWORK', 'TypeScript의 Mapped Type과 Conditional Type을 React에서 활용하는 방법을 설명해주세요.', '타입스크립트의 Mapped Type과 Conditional Type을 리액트에서 활용하는 방법을 설명해주세요.', 'TYPESCRIPT',
 'Mapped Type은 기존 타입의 속성을 변환합니다(Readonly<T>, Partial<T> 등). Conditional Type은 조건에 따라 타입을 분기합니다(T extends U ? X : Y). React에서 Props의 특정 속성 유무에 따라 타입을 달리하거나(discriminated union), 컴포넌트 변형(variant)에 따른 Props 타입 추론에 활용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:LANGUAGE_FRAMEWORK', 'React 18의 Concurrent Features(useTransition, useDeferredValue)를 설명해주세요.', '리액트 18의 Concurrent Features(유즈트랜지션, 유즈디페드밸류)를 설명해주세요.', 'REACT_CORE',
 'useTransition은 상태 업데이트를 낮은 우선순위로 표시하여 UI가 즉각 반응하도록 합니다. isPending으로 전환 중 로딩 상태를 표시합니다. useDeferredValue는 값의 업데이트를 지연시켜 검색 입력처럼 타이핑 중 무거운 렌더링을 뒤로 미룹니다. 두 기능 모두 입력 응답성을 유지하면서 무거운 작업을 처리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:LANGUAGE_FRAMEWORK', 'Zustand의 구조와 Context API 대비 장점을 설명해주세요.', '주스탄드의 구조와 컨텍스트 에이피아이 대비 장점을 설명해주세요.', 'STATE_MANAGEMENT',
 'Zustand는 create()로 스토어를 정의하고, Hook으로 필요한 상태만 구독합니다. Context API와 달리 리렌더링을 구독한 상태만 트리거하므로 성능이 좋습니다. 미들웨어(devtools, persist, immer)로 기능을 확장하고, 스토어를 여러 개 만들어 도메인별로 분리할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:LANGUAGE_FRAMEWORK', 'react-hook-form의 동작 원리와 장점을 설명해주세요.', 'react-hook-form의 동작 원리와 장점을 설명해주세요.', 'HOOKS',
 'react-hook-form은 비제어 컴포넌트(ref 기반)로 입력값을 관리하여 리렌더링을 최소화합니다. register로 필드를 등록하고, handleSubmit으로 유효성 검사 후 제출을 처리합니다. watch는 필요한 필드만 구독합니다. Zod, Yup과 연동하면 스키마 기반 유효성 검사가 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:LANGUAGE_FRAMEWORK', 'TypeScript 타입 가드(Type Guard)의 종류와 사용법을 설명해주세요.', '타입스크립트 타입 가드(타입 가드)의 종류와 사용법을 설명해주세요.', 'TYPESCRIPT',
 'typeof(원시 타입), instanceof(클래스), in 연산자(속성 유무), 사용자 정의 타입 가드(is 키워드: x is T)로 타입을 좁힙니다. Discriminated Union은 공통 리터럴 속성(type: ''success'' | ''error'')으로 분기합니다. React에서 API 응답 검증이나 컴포넌트 Props 분기에 자주 활용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:LANGUAGE_FRAMEWORK', 'React에서 낙관적 업데이트(Optimistic Update)를 구현하는 방법을 설명해주세요.', '리액트에서 낙관적 업데이트(옵티미스틱 업데이트)를 구현하는 방법을 설명해주세요.', 'STATE_MANAGEMENT',
 '낙관적 업데이트는 서버 응답 전에 UI를 먼저 변경하여 즉각적인 피드백을 제공합니다. TanStack Query의 useMutation에서 onMutate에 임시 상태를 적용하고, onError에서 이전 상태로 롤백합니다. onSettled에서 서버 데이터로 최종 동기화합니다. 좋아요, 삭제 등 빠른 반응이 중요한 UI에 활용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:LANGUAGE_FRAMEWORK', 'React에서 WebSocket을 사용한 실시간 통신을 구현하는 방법을 설명해주세요.', '리액트에서 웹소켓을 사용한 실시간 통신을 구현하는 방법을 설명해주세요.', 'REACT_CORE',
 'useEffect에서 WebSocket 연결을 생성하고, 메시지 수신 시 상태를 업데이트합니다. cleanup 함수에서 ws.close()로 연결을 해제합니다. 커스텀 Hook(useWebSocket)으로 연결 로직을 캡슐화하고, 재연결 로직과 heartbeat를 구현합니다. Socket.IO 라이브러리는 폴백과 재연결을 자동 처리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:LANGUAGE_FRAMEWORK', 'React에서 Intersection Observer를 활용하는 패턴을 설명해주세요.', '리액트에서 Intersection Observer를 활용하는 패턴을 설명해주세요.', 'HOOKS',
 'Intersection Observer는 요소가 뷰포트에 진입/이탈하는 시점을 비동기로 감지합니다. useRef로 대상 요소를 참조하고, useEffect에서 observer를 생성하여 관찰합니다. 무한 스크롤(sentinel 요소 감지), 지연 로딩(이미지/컴포넌트), 스크롤 애니메이션 트리거에 활용합니다. 커스텀 Hook으로 재사용성을 높입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:LANGUAGE_FRAMEWORK', 'TypeScript utility types(Partial, Required, Pick, Omit, Record)을 설명해주세요.', '타입스크립트 utility types(Partial, Required, Pick, Omit, Record)을 설명해주세요.', 'TYPESCRIPT',
 'Partial<T>는 모든 속성을 선택적으로, Required<T>는 모두 필수로 만듭니다. Pick<T,K>는 특정 속성만, Omit<T,K>는 특정 속성을 제외한 타입을 만듭니다. Record<K,V>는 key-value 맵 타입입니다. React에서 Props의 일부만 받는 컴포넌트(Partial), 업데이트 페이로드(Pick) 등에 자주 활용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:LANGUAGE_FRAMEWORK', 'React의 Render Props 패턴과 커스텀 Hook의 차이를 설명해주세요.', '리액트의 렌더 프롭스 패턴과 커스텀 훅의 차이를 설명해주세요.', 'REACT_CORE',
 'Render Props는 함수를 prop으로 전달하여 렌더링 로직을 주입하는 패턴입니다. 동작 로직과 UI를 분리하지만 중첩이 깊어지는 단점이 있습니다. 커스텀 Hook은 로직만 분리하고 JSX는 컴포넌트가 담당하여 더 간결합니다. 대부분의 Render Props 사례는 커스텀 Hook으로 대체 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- 추가 11문항 (LANGUAGE_FRAMEWORK SENIOR)
INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'React Server Components(RSC)와 Client Components의 차이를 설명해주세요.', '리액트 서버 컴포넌트(RSC)와 Client Components의 차이를 설명해주세요.', 'REACT_CORE',
 'RSC는 서버에서만 실행되어 번들에 포함되지 않으며, 데이터베이스/파일시스템에 직접 접근할 수 있습니다. useState, useEffect, 브라우저 API를 사용할 수 없습니다. Client Components(''use client'')는 기존 React 컴포넌트입니다. RSC로 데이터 페칭을 서버로 이동시켜 초기 로딩을 최적화하고 번들 크기를 줄입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'React 컴파일러(React Forget)의 목적과 영향을 설명해주세요.', '리액트 컴파일러(리액트 포겟)의 목적과 영향을 설명해주세요.', 'REACT_CORE',
 'React Compiler는 useMemo, useCallback, React.memo를 자동으로 삽입하여 수동 메모이제이션 없이 성능을 최적화합니다. JavaScript 의미론 규칙을 지키는 코드에서 자동으로 동작합니다. 개발자는 메모이제이션 보일러플레이트 없이 순수한 로직에 집중할 수 있고, 코드가 간결해집니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:LANGUAGE_FRAMEWORK', '마이크로 프론트엔드 아키텍처에서 상태 공유 전략을 설명해주세요.', '마이크로 프론트엔드 아키텍처에서 상태 공유 전략을 설명해주세요.', 'ARCHITECTURE',
 '마이크로 프론트엔드 간 상태 공유는 URL/쿼리스트링(가장 단순), Custom Events/BroadcastChannel(브라우저 API), 공유 스토어 패키지(별도 npm 패키지로 상태 공유), 백엔드 동기화(각 앱이 서버에서 최신 상태 조회)로 구현합니다. 결합도를 최소화하는 이벤트 기반 방식이 마이크로 프론트엔드 철학에 부합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'React의 Fiber 아키텍처와 기존 Stack Reconciler의 차이를 설명해주세요.', '리액트의 파이버 아키텍처와 기존 Stack Reconciler의 차이를 설명해주세요.', 'REACT_CORE',
 '기존 Stack Reconciler는 동기적으로 Virtual DOM 트리를 순회하여 중단이 불가했습니다. Fiber는 작업을 작은 단위(fiber 노드)로 분할하고 우선순위를 부여하여 중단/재개가 가능합니다. 이를 통해 Concurrent Mode(useTransition, Suspense), Time Slicing(장시간 작업 분할)이 가능해졌습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'TypeScript의 infer 키워드와 고급 타입 추론을 설명해주세요.', '타입스크립트의 infer 키워드와 고급 타입 추론을 설명해주세요.', 'TYPESCRIPT',
 'infer는 Conditional Type 내에서 타입을 캡처합니다. ReturnType<T>는 infer R로 반환 타입을 추론합니다. React에서 컴포넌트 Props 타입 추출(ComponentProps<typeof MyComp>), HOC의 반환 타입 추론, 제네릭 유틸리티 타입 작성에 활용합니다. 타입 안전성을 유지하면서 코드 중복을 줄입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:LANGUAGE_FRAMEWORK', '대규모 React 애플리케이션의 폴더 구조 설계 전략을 설명해주세요.', '대규모 리액트 애플리케이션의 폴더 구조 설계 전략을 설명해주세요.', 'ARCHITECTURE',
 'Feature-based(기능별: /features/auth, /features/dashboard)로 응집도를 높이고, 공통 요소는 /shared, /components에 분리합니다. 도메인 레이어(entities, use-cases)와 UI 레이어를 분리하는 FSD(Feature-Sliced Design) 아키텍처도 대안입니다. 명확한 경계와 순환 의존성 방지가 핵심입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'React에서 메모리 누수를 방지하는 방법을 설명해주세요.', '리액트에서 메모리 누수를 방지하는 방법을 설명해주세요.', 'PERFORMANCE',
 'useEffect cleanup으로 구독, 타이머, 이벤트 리스너를 해제합니다. fetch 요청은 AbortController로 언마운트 시 취소합니다. React 18부터 언마운트 후 setState 경고가 제거되었지만, 불필요한 네트워크 요청 방지를 위해 cleanup은 여전히 중요합니다. 클로저가 큰 객체를 캡처하지 않도록 주의하고, Chrome DevTools Memory 탭으로 누수를 감지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'React에서 Server-Side Rendering(SSR)의 hydration 과정을 설명해주세요.', '리액트에서 서버-사이드 렌더링(SSR)의 하이드레이션 과정을 설명해주세요.', 'REACT_CORE',
 'SSR은 서버에서 HTML을 생성하여 초기 로딩을 빠르게 합니다. Hydration은 서버에서 렌더링된 HTML에 React가 이벤트 핸들러를 연결하는 과정입니다. React 18의 Selective Hydration은 스트리밍 SSR로 Suspense 경계별로 점진적으로 hydrate합니다. Hydration mismatch는 서버/클라이언트 렌더링 결과 불일치 시 발생합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'Module Federation을 활용한 마이크로 프론트엔드 구현을 설명해주세요.', 'Module Federation을 활용한 마이크로 프론트엔드 구현을 설명해주세요.', 'ARCHITECTURE',
 'Webpack Module Federation은 런타임에 원격 앱의 모듈을 동적으로 로드합니다. Host 앱이 Remote 앱의 컴포넌트를 지연 로드하며, 공유 의존성(react, react-dom)을 중복 없이 사용합니다. 빌드 시간 독립성을 유지하면서 컴포넌트를 공유할 수 있어 팀 단위 독립 배포가 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'React에서 테스트 전략(단위/통합/E2E)을 설계하는 방법을 설명해주세요.', '리액트에서 테스트 전략(단위/통합/이투이)을 설계하는 방법을 설명해주세요.', 'ARCHITECTURE',
 '단위 테스트(Jest + Testing Library): 개별 컴포넌트와 커스텀 Hook의 동작 검증. 통합 테스트(MSW + Testing Library): API 모킹 후 여러 컴포넌트의 상호작용 검증. E2E 테스트(Playwright/Cypress): 실제 브라우저에서 사용자 시나리오 검증. Testing Library는 구현이 아닌 사용자 관점에서 테스트를 권장합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:LANGUAGE_FRAMEWORK', 'React에서 상태 머신(XState)을 활용한 복잡한 UI 관리를 설명해주세요.', '리액트에서 상태 머신(XState)을 활용한 복잡한 유아이 관리를 설명해주세요.', 'STATE_MANAGEMENT',
 '상태 머신은 유한한 상태와 전이를 명시적으로 정의하여 불가능한 상태를 방지합니다. XState는 시각화 도구와 타입 안전성을 제공합니다. 인증 흐름(idle→loading→authenticated→error), 멀티스텝 폼, 복잡한 UI 워크플로에 적합합니다. useState의 boolean 플래그 조합보다 상태 폭발을 방지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- 추가 15문항 (UI_FRAMEWORK JUNIOR)
INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:UI_FRAMEWORK', 'Tailwind CSS의 특징과 장단점을 설명해주세요.', 'Tailwind 씨에스에스의 특징과 장단점을 설명해주세요.', 'CSS',
 'Tailwind CSS는 유틸리티 클래스 기반으로 HTML에 직접 스타일을 적용합니다. 커스텀 CSS 작성이 줄고, 디자인 시스템이 내장되어 일관성을 유지합니다. PurgeCSS로 사용하는 클래스만 빌드에 포함되어 번들이 작습니다. 단점으로는 클래스가 많아 HTML이 장황해지고, 팀 내 클래스명 암기가 필요합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:UI_FRAMEWORK', 'CSS-in-JS(styled-components, Emotion)의 개념과 장점을 설명해주세요.', '씨에스에스-인-제이에스(스타일드-컴포넌트, 이모션)의 개념과 장점을 설명해주세요.', 'CSS',
 'CSS-in-JS는 JavaScript 파일 안에서 CSS를 작성하여 컴포넌트와 스타일을 함께 관리합니다. 동적 스타일(props 기반), 자동 스코핑(클래스명 충돌 없음), 타입 안전성, 코드 분리(사용 안 된 스타일 자동 제거)가 장점입니다. 런타임 오버헤드가 있으며 SSR 설정이 복잡할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:UI_FRAMEWORK', 'Flexbox와 CSS Grid의 차이와 사용 시점을 설명해주세요.', 'Flexbox와 씨에스에스 Grid의 차이와 사용 시점을 설명해주세요.', 'CSS',
 'Flexbox는 1차원 레이아웃(행 또는 열)에 적합하며, 내비게이션 바, 카드 정렬에 사용합니다. CSS Grid는 2차원 레이아웃(행과 열 동시)에 적합하며, 전체 페이지 레이아웃, 갤러리에 사용합니다. 대부분의 현대 UI는 Grid로 큰 구조를 잡고 Flexbox로 세부 요소를 배치합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:UI_FRAMEWORK', 'CSS 모듈(CSS Modules)의 동작 원리와 장점을 설명해주세요.', '씨에스에스 모듈(씨에스에스 모듈)의 동작 원리와 장점을 설명해주세요.', 'CSS',
 'CSS Modules는 빌드 시 클래스명을 고유하게 변환(Button_btn__xK2jL)하여 전역 충돌을 방지합니다. 컴포넌트와 같은 폴더에 .module.css로 작성하고, import styles from ''./Button.module.css''로 가져옵니다. 추가 런타임 없이 순수 CSS를 사용할 수 있어 성능 오버헤드가 없습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:UI_FRAMEWORK', 'React에서 반응형 디자인을 구현하는 방법을 설명해주세요.', '리액트에서 반응형 디자인을 구현하는 방법을 설명해주세요.', 'RESPONSIVE',
 'CSS 미디어 쿼리(@media)로 화면 크기별 스타일을 정의하거나, Tailwind의 반응형 접두사(sm:, md:, lg:)를 사용합니다. useMediaQuery 커스텀 Hook이나 ResizeObserver로 JavaScript에서 브레이크포인트를 처리합니다. 모바일 퍼스트로 기본 스타일을 모바일로 작성하고 큰 화면에서 오버라이드합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:UI_FRAMEWORK', 'WAI-ARIA의 개념과 React에서 기본 접근성을 지키는 방법을 설명해주세요.', 'WAI-ARIA의 개념과 리액트에서 기본 접근성을 지키는 방법을 설명해주세요.', 'ACCESSIBILITY',
 'WAI-ARIA는 스크린리더 등 보조기술이 이해할 수 있도록 역할(role), 상태(aria-*), 속성을 추가하는 명세입니다. 시맨틱 HTML(button, nav, main)을 우선 사용하고, 비시맨틱 요소에만 ARIA를 추가합니다. 이미지는 alt, 폼은 label, 버튼은 의미있는 텍스트를 제공하고, 키보드만으로 모든 기능에 접근 가능해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:UI_FRAMEWORK', 'React에서 다크 모드를 구현하는 방법을 설명해주세요.', '리액트에서 다크 모드를 구현하는 방법을 설명해주세요.', 'CSS',
 'CSS 변수로 색상 토큰을 정의하고, [data-theme="dark"] 셀렉터로 오버라이드합니다. Tailwind의 dark: 접두사와 darkMode: ''class'' 설정을 사용할 수 있습니다. 시스템 설정은 prefers-color-scheme 미디어 쿼리로 감지하고, localStorage에 사용자 선택을 저장합니다. Context API나 Zustand로 테마 상태를 관리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:UI_FRAMEWORK', 'React에서 로딩 상태를 처리하는 패턴을 설명해주세요.', '리액트에서 로딩 상태를 처리하는 패턴을 설명해주세요.', 'COMPONENT',
 '스피너/스켈레톤 UI로 로딩을 시각화합니다. 스켈레톤은 실제 레이아웃과 비슷한 플레이스홀더로 체감 성능을 높입니다. Suspense의 fallback prop, TanStack Query의 isLoading 상태, 또는 커스텀 useAsync Hook으로 처리합니다. 에러 상태(isError)와 빈 상태(isEmpty)도 함께 처리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:UI_FRAMEWORK', 'CSS transition과 animation의 차이를 설명해주세요.', '씨에스에스 transition과 animation의 차이를 설명해주세요.', 'ANIMATION',
 'transition은 특정 CSS 속성이 변경될 때 시작과 끝 상태 사이를 부드럽게 보간합니다. JavaScript나 CSS 클래스 변경으로 트리거됩니다. animation(@keyframes)은 여러 단계를 정의하고 자동으로 재생되며, 반복, 방향 제어가 가능합니다. 단순한 상태 전환은 transition, 복잡한 시퀀스는 animation이 적합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:UI_FRAMEWORK', 'React에서 폼 유효성 검사를 구현하는 기본 방법을 설명해주세요.', '리액트에서 폼 유효성 검사를 구현하는 기본 방법을 설명해주세요.', 'COMPONENT',
 'HTML5 기본 유효성 검사(required, minLength, pattern)를 활용하거나, JavaScript에서 submit 시 검증합니다. 실시간 검증은 onChange나 onBlur 이벤트에서 처리합니다. 에러 메시지는 aria-describedby로 인풋과 연결하여 접근성을 지킵니다. react-hook-form + Zod 조합이 실무 표준입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:UI_FRAMEWORK', 'React에서 모달 컴포넌트를 구현하는 방법을 설명해주세요.', '리액트에서 모달 컴포넌트를 구현하는 방법을 설명해주세요.', 'COMPONENT',
 'Portal로 body에 렌더링하여 z-index 문제를 해결합니다. 배경 스크롤을 막고(overflow:hidden), 포커스 트랩으로 모달 안에서만 탭 이동이 되도록 합니다. ESC 키와 배경 클릭으로 닫을 수 있어야 합니다. aria-modal, role="dialog", aria-labelledby로 접근성을 지원하고, 오픈/클로즈 애니메이션도 추가합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:UI_FRAMEWORK', 'React에서 Toast/Notification 시스템을 구현하는 방법을 설명해주세요.', '리액트에서 토스트/노티피케이션 시스템을 구현하는 방법을 설명해주세요.', 'COMPONENT',
 'Portal로 body 최상단에 렌더링하고, Context API나 Zustand로 토스트 목록을 전역 관리합니다. 토스트는 고유 ID와 자동 사라짐(setTimeout), 수동 닫기를 지원합니다. 여러 토스트의 스택 표시와 애니메이션을 처리합니다. react-hot-toast, sonner 같은 라이브러리를 활용하면 빠르게 구현할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:UI_FRAMEWORK', '이미지 최적화 기법을 설명해주세요.', '이미지 최적화 기법을 설명해주세요.', 'PERFORMANCE',
 'WebP/AVIF 포맷으로 파일 크기를 줄이고, srcset으로 화면 크기별 적절한 해상도를 제공합니다. loading="lazy"로 뷰포트에 진입할 때만 로드하고, width/height 속성으로 CLS를 방지합니다. Next.js의 next/image는 자동 최적화, lazy loading, blur placeholder를 제공합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:UI_FRAMEWORK', 'React에서 테이블 컴포넌트를 만들 때 고려해야 할 사항을 설명해주세요.', '리액트에서 테이블 컴포넌트를 만들 때 고려해야 할 사항을 설명해주세요.', 'COMPONENT',
 '시맨틱 HTML(table, thead, tbody, th, td)로 스크린리더 접근성을 지원하고, th에 scope 속성을 추가합니다. 대용량 데이터는 가상화(TanStack Table + react-virtual)로 성능을 확보합니다. 정렬, 필터, 페이지네이션, 컬럼 리사이징, 고정 컬럼 기능을 요구사항에 맞게 구현합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:REACT_TS:UI_FRAMEWORK', 'React에서 드래그 앤 드롭을 구현하는 방법을 설명해주세요.', '리액트에서 드래그 앤 드롭을 구현하는 방법을 설명해주세요.', 'COMPONENT',
 'HTML5 Drag and Drop API(draggable, onDragStart, onDrop)로 기본 기능을 구현하거나, react-beautiful-dnd, dnd-kit 같은 라이브러리를 사용합니다. dnd-kit은 접근성(키보드 드래그), 터치 지원, 커스터마이징이 우수합니다. 드래그 중 시각적 피드백(ghost 이미지, 드롭 위치 표시)도 중요합니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- 추가 15문항 (UI_FRAMEWORK MID)
INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:UI_FRAMEWORK', 'Compound Component 패턴을 설명하고 예시를 들어주세요.', '컴파운드 컴포넌트 패턴을 설명하고 예시를 들어주세요.', 'COMPONENT',
 'Compound Component는 여러 컴포넌트가 Context를 통해 상태를 공유하면서 관련 UI를 구성하는 패턴입니다. Select.Root, Select.Trigger, Select.Content처럼 부모-자식이 암묵적으로 통신합니다. API 사용자는 내부 구조를 자유롭게 조합할 수 있습니다. Radix UI가 이 패턴의 대표적인 구현체입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:UI_FRAMEWORK', 'Storybook의 역할과 컴포넌트 개발 프로세스에서의 활용을 설명해주세요.', 'Storybook의 역할과 컴포넌트 개발 프로세스에서의 활용을 설명해주세요.', 'DESIGN_SYSTEM',
 'Storybook은 컴포넌트를 독립적으로 개발하고 문서화하는 도구입니다. 각 컴포넌트의 스토리(상태/변형)를 정의하여 시각적으로 검토합니다. 디자이너와 개발자의 소통 창구가 되고, 컴포넌트 라이브러리 문서로 활용합니다. Interaction Tests로 컴포넌트 단위 테스트도 수행할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:UI_FRAMEWORK', 'Headless UI 컴포넌트 라이브러리의 개념과 장점을 설명해주세요.', '헤드리스 유아이 컴포넌트 라이브러리의 개념과 장점을 설명해주세요.', 'DESIGN_SYSTEM',
 'Headless UI는 스타일 없이 동작(접근성, 키보드 탐색, ARIA)만 제공하는 컴포넌트 라이브러리입니다. Radix UI, Headless UI(Tailwind Labs), Ark UI가 대표적입니다. 디자인 시스템을 자유롭게 커스터마이징할 수 있으면서 접근성 로직을 재구현하지 않아도 됩니다. shadcn/ui는 Radix UI 기반 headless 컴포넌트입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:UI_FRAMEWORK', 'CSS Container Queries의 개념과 미디어 쿼리 대비 장점을 설명해주세요.', '씨에스에스 Container Queries의 개념과 미디어 쿼리 대비 장점을 설명해주세요.', 'RESPONSIVE',
 'Container Queries는 뷰포트 크기 대신 부모 컨테이너 크기를 기준으로 스타일을 변경합니다. 재사용 가능한 컴포넌트가 배치되는 컨텍스트에 따라 적응적으로 동작합니다. @container 규칙으로 정의하고, contain: inline-size로 컨테이너를 지정합니다. 현재 모든 주요 브라우저에서 지원됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:UI_FRAMEWORK', 'React에서 복잡한 데이터 테이블(TanStack Table)을 구현하는 방법을 설명해주세요.', '리액트에서 복잡한 데이터 테이블(탠스택 테이블)을 구현하는 방법을 설명해주세요.', 'COMPONENT',
 'TanStack Table은 헤드리스 테이블 라이브러리로 정렬, 필터, 페이지네이션, 그룹핑, 가상화를 지원합니다. useReactTable Hook으로 테이블 인스턴스를 생성하고, column definitions로 컬럼 설정을 정의합니다. 서버 사이드 처리(manualSorting, manualFiltering)로 대용량 데이터도 처리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:UI_FRAMEWORK', 'Framer Motion의 레이아웃 애니메이션(Layout Animation)을 설명해주세요.', 'Framer Motion의 레이아웃 애니메이션(Layout Animation)을 설명해주세요.', 'ANIMATION',
 'layout prop을 추가하면 컴포넌트 크기/위치 변경 시 자동으로 애니메이션됩니다. FLIP 기법을 내부적으로 사용하여 performant합니다. LayoutGroup으로 서로 다른 컴포넌트 간 레이아웃 애니메이션을 연결합니다. 탭 인디케이터, 리스트 재정렬, 카드 확장/축소에 효과적입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:UI_FRAMEWORK', '색상 대비, 포커스 표시, 스크린리더 지원을 포함한 접근성 체크리스트를 설명해주세요.', '색상 대비, 포커스 표시, 스크린리더 지원을 포함한 접근성 체크리스트를 설명해주세요.', 'ACCESSIBILITY',
 'WCAG 2.1 AA 기준: 색상 대비 4.5:1(일반 텍스트), 3:1(큰 텍스트). 포커스 스타일을 outline: none으로 제거하면 안 됩니다. 스크린리더 테스트(NVDA, VoiceOver), 키보드만으로 전체 기능 접근 가능 여부를 확인합니다. axe DevTools나 Lighthouse로 자동 검사하고, 수동 테스트로 보완합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:UI_FRAMEWORK', 'CSS 변수(Custom Properties)를 활용한 테마 시스템을 설계하는 방법을 설명해주세요.', '씨에스에스 변수(커스텀 프로퍼티)를 활용한 테마 시스템을 설계하는 방법을 설명해주세요.', 'DESIGN_SYSTEM',
 ':root에 기본 토큰을 정의하고, [data-theme="dark"]에서 오버라이드합니다. 시맨틱 토큰(--color-primary, --spacing-md)은 원시 토큰(--blue-500, --16px)을 참조합니다. JavaScript에서 getComputedStyle과 setProperty로 동적 변경이 가능합니다. CSS 변수는 상속되며 ShadowDOM도 관통합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:UI_FRAMEWORK', 'React에서 차트/시각화 라이브러리(Recharts, D3)를 사용하는 방법을 설명해주세요.', '리액트에서 차트/시각화 라이브러리(리차트, D3)를 사용하는 방법을 설명해주세요.', 'COMPONENT',
 'Recharts는 React 컴포넌트 기반으로 선언적이고 반응형을 지원하여 빠른 구현에 적합합니다. D3는 강력하지만 직접 DOM을 조작하므로 React와 통합 시 ref를 사용하거나, D3의 계산 기능만 사용하고 렌더링은 React SVG로 처리합니다. 대화형 차트는 이벤트 핸들러와 툴팁 구현이 중요합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:UI_FRAMEWORK', 'React에서 드래그로 크기 조절 가능한 컴포넌트(Resizable Panel)를 구현하는 방법을 설명해주세요.', '리액트에서 드래그로 크기 조절 가능한 컴포넌트(리사이저블 패널)를 구현하는 방법을 설명해주세요.', 'COMPONENT',
 'PointerEvents API로 드래그를 감지하고, 마우스/터치 통합 이벤트를 처리합니다. 패널 크기를 상태로 관리하고, 최소/최대 크기 제한을 적용합니다. CSS로 cursor를 변경하여 시각적 피드백을 제공합니다. react-resizable-panels 같은 라이브러리는 접근성과 키보드 지원을 내장합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:UI_FRAMEWORK', 'CSS Grid를 활용한 복잡한 레이아웃 구현 방법을 설명해주세요.', '씨에스에스 Grid를 활용한 복잡한 레이아웃 구현 방법을 설명해주세요.', 'CSS',
 'grid-template-areas로 레이아웃을 시각적으로 정의하고, 미디어 쿼리로 그리드 구조를 변경합니다. minmax(), auto-fill/auto-fit로 반응형 그리드를 구현합니다. subgrid로 중첩 그리드의 정렬을 부모 그리드에 맞출 수 있습니다. Dense packing(grid-auto-flow: dense)으로 빈 공간을 채웁니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:UI_FRAMEWORK', 'React에서 virtualization(가상화)을 구현하는 방법을 설명해주세요.', '리액트에서 virtualization(가상화)을 구현하는 방법을 설명해주세요.', 'PERFORMANCE',
 '가상화는 화면에 보이는 항목만 DOM에 렌더링하여 대용량 리스트 성능을 최적화합니다. TanStack Virtual(react-virtual), react-window, react-virtuoso가 주요 라이브러리입니다. 고정 크기(FixedSizeList)와 가변 크기(VariableSizeList)를 지원합니다. 그리드, 트리뷰에도 적용 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:UI_FRAMEWORK', 'React에서 국제화(i18n)를 구현하는 방법을 설명해주세요.', '리액트에서 국제화(국제화)를 구현하는 방법을 설명해주세요.', 'COMPONENT',
 'react-i18next로 번역 파일을 언어별로 관리하고, useTranslation Hook으로 컴포넌트에서 사용합니다. 날짜/숫자/통화는 Intl API로 로케일에 맞게 포맷합니다. RTL(우->좌) 언어는 dir="rtl"과 CSS logical properties로 지원합니다. 동적 번역 로드로 초기 번들을 최소화합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:UI_FRAMEWORK', 'React Testing Library를 활용한 컴포넌트 테스트 전략을 설명해주세요.', '리액트 Testing Library를 활용한 컴포넌트 테스트 전략을 설명해주세요.', 'COMPONENT',
 'Testing Library는 사용자 관점(screen.getByRole, getByText)에서 쿼리하도록 권장합니다. 구현 세부사항(클래스명, 내부 상태)이 아닌 사용자 행동(클릭, 입력)을 테스트합니다. userEvent로 실제 사용자 이벤트를 시뮬레이션합니다. MSW로 API를 모킹하면 통합 테스트가 더 신뢰할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:REACT_TS:UI_FRAMEWORK', 'React에서 파일 업로드 컴포넌트를 구현할 때 고려해야 할 사항을 설명해주세요.', '리액트에서 파일 업로드 컴포넌트를 구현할 때 고려해야 할 사항을 설명해주세요.', 'COMPONENT',
 '드래그 앤 드롭과 클릭 업로드 모두 지원하고, 파일 타입/크기 유효성 검사를 클라이언트에서 먼저 수행합니다. 대용량 파일은 청크 업로드(multipart)와 재개 가능한 업로드를 고려합니다. 업로드 진행률(XMLHttpRequest 또는 fetch)과 취소 기능을 제공합니다. 접근성을 위해 input[type=file]을 기반으로 구현합니다.',
 'MODEL_ANSWER', TRUE, NOW());

-- 추가 15문항 (UI_FRAMEWORK SENIOR)
INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:UI_FRAMEWORK', '엔터프라이즈 디자인 시스템을 구축하고 유지보수하는 전략을 설명해주세요.', '엔터프라이즈 디자인 시스템을 구축하고 유지보수하는 전략을 설명해주세요.', 'DESIGN_SYSTEM',
 '디자인 토큰(Style Dictionary), 컴포넌트 라이브러리(Storybook), 문서화(Storybook Docs), 버전 관리(npm, Changesets), 테스트(Chromatic 시각 회귀 테스트)를 갖춥니다. 모노레포(Turborepo, Nx)로 패키지를 관리하고, 자동화된 릴리스 파이프라인을 구성합니다. 채택률 모니터링과 사용성 피드백 루프도 중요합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:UI_FRAMEWORK', '시각 회귀 테스트(Visual Regression Testing)를 설계하는 방법을 설명해주세요.', '시각 회귀 테스트(비주얼 레그레션 테스팅)를 설계하는 방법을 설명해주세요.', 'DESIGN_SYSTEM',
 'Chromatic(Storybook 연동), Playwright/Cypress의 screenshot 비교로 UI 변경을 자동 감지합니다. PR마다 스크린샷을 캡처하고 기준 이미지와 diff를 비교합니다. 동적 콘텐츠(날짜, 랜덤값)를 마스킹하여 false positive를 줄입니다. CI 파이프라인에 통합하여 의도치 않은 UI 변경을 방지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:UI_FRAMEWORK', 'CSS Houdini와 Paint API를 활용한 고급 커스텀 CSS 구현을 설명해주세요.', '씨에스에스 Houdini와 Paint 에이피아이를 활용한 고급 커스텀 씨에스에스 구현을 설명해주세요.', 'CSS',
 'CSS Houdini는 CSS 엔진의 내부에 접근하는 API 집합입니다. Paint API(CSS.paintWorklet)로 canvas 기반 커스텀 배경/이미지를 CSS에서 사용합니다. Layout API로 커스텀 레이아웃 알고리즘을 정의합니다. Properties and Values API(@property)로 애니메이션 가능한 커스텀 속성을 정의하고 타입을 지정합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:UI_FRAMEWORK', 'React 앱의 접근성 자동화 테스트와 수동 감사 프로세스를 설명해주세요.', '리액트 앱의 접근성 자동화 테스트와 수동 감사 프로세스를 설명해주세요.', 'ACCESSIBILITY',
 '자동화: axe-core + jest-axe로 단위 테스트에 접근성 검사 통합, Lighthouse CI로 빌드마다 점수 측정, Playwright/Cypress에 axe 플러그인 통합. 수동 감사: NVDA/VoiceOver 스크린리더로 전체 사용자 흐름 테스트, 키보드 탐색 확인, 색상 대비 분석기 사용. WCAG 2.1 AA 준수를 목표로 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:UI_FRAMEWORK', 'React에서 canvas 기반 고성능 그래픽(WebGL, Three.js)을 통합하는 방법을 설명해주세요.', '리액트에서 canvas 기반 고성능 그래픽(웹지엘, 쓰리제이에스)을 통합하는 방법을 설명해주세요.', 'PERFORMANCE',
 'useRef로 canvas 요소를 참조하고 useEffect에서 WebGL 컨텍스트를 초기화합니다. React Three Fiber는 Three.js를 React 컴포넌트로 추상화합니다. requestAnimationFrame으로 렌더 루프를 관리하고, cleanup에서 해제합니다. React와 GL 렌더러 간의 상태 동기화를 최소화하여 성능을 유지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:UI_FRAMEWORK', 'Tailwind CSS의 성능 최적화와 커스터마이징 전략을 설명해주세요.', 'Tailwind 씨에스에스의 성능 최적화와 커스터마이징 전략을 설명해주세요.', 'CSS',
 'content 설정으로 PurgeCSS가 사용되지 않는 클래스를 제거합니다. theme.extend로 기존 스케일을 유지하면서 커스텀 토큰을 추가합니다. plugin API로 컴포넌트 클래스와 유틸리티를 추가합니다. clsx/tailwind-merge로 조건부 클래스 병합 시 중복/충돌을 해결합니다. tailwindcss-animate로 애니메이션을 추가합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:UI_FRAMEWORK', 'React 앱에서 국제화 접근성(i18n + a11y)을 동시에 지원하는 방법을 설명해주세요.', '리액트 앱에서 국제화 접근성(국제화 + 접근성)을 동시에 지원하는 방법을 설명해주세요.', 'ACCESSIBILITY',
 'lang 속성을 현재 언어로 설정하여 스크린리더가 올바른 발음 엔진을 사용하도록 합니다. RTL 언어는 dir="rtl"과 CSS logical properties(margin-inline-start vs margin-left)로 지원합니다. aria-label은 번역된 텍스트를 사용하고, 날짜/시간 포맷은 Intl API로 로케일에 맞게 처리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:UI_FRAMEWORK', 'React에서 고급 애니메이션 성능 프로파일링과 최적화를 설명해주세요.', '리액트에서 고급 애니메이션 성능 프로파일링과 최적화를 설명해주세요.', 'ANIMATION',
 'Chrome DevTools Performance 탭에서 Paint, Composite 레이어를 분석합니다. Rendering 패널에서 "Paint flashing", "Layer borders"로 불필요한 레이어를 발견합니다. will-change 남용 시 메모리를 과도하게 사용합니다. Compositor Thread에서만 실행되는 transform/opacity 애니메이션을 우선하고, GSAP는 자체 최적화를 제공합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:UI_FRAMEWORK', 'Next.js App Router의 Streaming과 Partial Prerendering(PPR)을 설명해주세요.', '넥스트제이에스 App Router의 Streaming과 파셜 프리렌더링(PPR)을 설명해주세요.', 'ARCHITECTURE',
 'Streaming은 서버에서 HTML을 청크별로 점진적으로 전송합니다. Suspense 경계 단위로 스트리밍하여 느린 데이터가 다른 빠른 컨텐츠를 블록하지 않습니다. PPR은 정적(prerendered)과 동적(streamed) 부분을 같은 페이지에 혼합합니다. 셸(shell)은 즉시 제공되고, 동적 데이터는 스트리밍됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:UI_FRAMEWORK', '마이크로 프론트엔드에서 공유 UI 컴포넌트 라이브러리를 관리하는 전략을 설명해주세요.', '마이크로 프론트엔드에서 공유 유아이 컴포넌트 라이브러리를 관리하는 전략을 설명해주세요.', 'ARCHITECTURE',
 '공유 컴포넌트를 별도 npm 패키지로 배포하거나, Module Federation으로 런타임 공유합니다. 버저닝을 엄격히 관리하고, 하위 호환성을 위해 deprecation 기간을 둡니다. Changesets으로 변경 내역을 관리하고, Chromatic으로 시각 회귀를 방지합니다. 각 마이크로 앱 팀이 업그레이드 시점을 자율적으로 결정합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:UI_FRAMEWORK', 'React에서 PDF 생성 및 인쇄 기능을 구현하는 방법을 설명해주세요.', '리액트에서 PDF 생성 및 인쇄 기능을 구현하는 방법을 설명해주세요.', 'COMPONENT',
 'react-pdf/renderer는 React 컴포넌트로 PDF를 선언적으로 생성합니다. 브라우저 인쇄는 @media print CSS로 인쇄 레이아웃을 별도 정의하고 불필요한 요소를 숨깁니다. html2canvas + jsPDF로 DOM을 이미지로 캡처하여 PDF로 변환합니다. 서버에서 Puppeteer로 헤드리스 Chrome을 사용하면 더 안정적입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:UI_FRAMEWORK', 'React 앱의 Critical CSS 최적화 전략을 설명해주세요.', '리액트 앱의 크리티컬 씨에스에스 최적화 전략을 설명해주세요.', 'PERFORMANCE',
 'Critical CSS는 Above-the-fold 렌더링에 필요한 CSS만 인라인으로 삽입하여 초기 렌더링을 빠르게 합니다. critters(webpack 플러그인)가 자동으로 추출합니다. Next.js는 CSS-in-JS(styled-jsx)를 서버에서 인라인합니다. 나머지 CSS는 비동기로 로드하고, 미사용 CSS는 PurgeCSS로 제거합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:UI_FRAMEWORK', 'Signals 기반 상태 관리(Preact Signals, Solid.js)와 React의 차이를 설명해주세요.', 'Signals 기반 상태 관리(프리액트 시그널, 솔리드제이에스)와 리액트의 차이를 설명해주세요.', 'ARCHITECTURE',
 'Signals는 세밀한 반응성으로 구독하는 컴포넌트만 리렌더링합니다. React의 Virtual DOM diffing 없이 직접 DOM을 업데이트합니다. @preact/signals-react를 사용하면 React에서도 Signal을 사용할 수 있습니다. 고빈도 업데이트(실시간 대시보드)에서 React보다 성능이 우수할 수 있지만, 생태계와 DevTools가 부족합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:UI_FRAMEWORK', '대규모 React 앱에서 런타임 성능 문제를 체계적으로 진단하는 방법을 설명해주세요.', '대규모 리액트 앱에서 런타임 성능 문제를 체계적으로 진단하는 방법을 설명해주세요.', 'PERFORMANCE',
 '1단계: Chrome DevTools Performance 탭으로 긴 프레임을 찾습니다. 2단계: React Profiler로 리렌더링 빈도와 시간을 측정합니다. 3단계: why-did-you-render 라이브러리로 불필요한 리렌더링 원인을 파악합니다. 4단계: 메모이제이션 적용 후 효과를 측정합니다. 증거 없이 최적화하지 않는 것이 원칙입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:REACT_TS:UI_FRAMEWORK', 'React에서 E2E 테스트(Playwright)를 CI/CD에 통합하는 전략을 설명해주세요.', '리액트에서 이투이 테스트(플레이라이트)를 씨아이 씨디에 통합하는 전략을 설명해주세요.', 'ARCHITECTURE',
 'Playwright는 멀티 브라우저(Chromium, Firefox, WebKit) 테스트를 지원하고 네트워크 모킹이 가능합니다. CI에서 docker로 브라우저 환경을 표준화합니다. 테스트를 스모크(핵심 플로우), 전체(회귀) 두 단계로 나눠 PR은 스모크, 병합 후 전체를 실행합니다. sharding으로 병렬 실행하여 속도를 높입니다.',
 'MODEL_ANSWER', TRUE, NOW());
