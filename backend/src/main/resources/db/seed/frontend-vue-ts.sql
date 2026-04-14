-- frontend-vue-ts.sql
-- Vue.js/TypeScript 시드 데이터 (90문항: JUNIOR30, MID30, SENIOR30)
-- cache_key: FRONTEND:{Level}:VUE_TS:LANGUAGE_FRAMEWORK

-- ============================================================
-- JUNIOR (30문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue 2와 Vue 3의 주요 차이점은 무엇인가요?', '뷰 2와 뷰 3의 주요 차이점은 무엇인가요?', 'VUE_CORE',
 'Vue 3는 Composition API 도입, Proxy 기반 반응성 시스템, Tree-shaking 지원, Fragment/Teleport/Suspense 컴포넌트 추가 등이 핵심 변화입니다. Vue 2는 Object.defineProperty를 사용해 반응성을 구현했지만 Vue 3는 ES6 Proxy로 교체해 배열 인덱스 변경 등 기존 한계를 해소했습니다. 또한 TypeScript 지원이 대폭 개선되어 내부 코드 자체가 TypeScript로 작성되었고, 번들 사이즈도 현저히 줄었습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue의 반응성 시스템(Reactivity System)이란 무엇인가요?', '뷰의 반응성 시스템(Reactivity System)이란 무엇인가요?', 'VUE_CORE',
 'Vue의 반응성 시스템은 데이터가 변경될 때 이를 자동으로 감지하고 연관된 DOM을 업데이트하는 메커니즘입니다. Vue 3에서는 ES6 Proxy를 사용해 객체의 읽기/쓰기를 추적하며, 의존성 추적(track)과 트리거(trigger) 두 단계로 동작합니다. 상태가 변경되면 해당 상태에 의존하는 모든 effect(렌더 함수, computed, watch 등)가 자동으로 재실행됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Options API와 Composition API의 차이점을 설명해주세요.', 'Options 에이피아이와 Composition 에이피아이의 차이점을 설명해주세요.', 'VUE_CORE',
 'Options API는 data, methods, computed, watch 등 옵션 키로 컴포넌트 로직을 구성하여 직관적이지만 관련 로직이 여러 옵션에 흩어지는 단점이 있습니다. Composition API는 setup() 함수 안에서 관련 로직을 함께 묶어 재사용성과 타입 추론이 뛰어납니다. 대규모 컴포넌트나 로직 재사용이 많은 경우 Composition API가 유리하며, Vue 3에서는 두 방식 모두 지원합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'ref와 reactive의 차이점은 무엇인가요?', 'ref와 reactive의 차이점은 무엇인가요?', 'COMPOSITION_API',
 'ref는 원시값(string, number, boolean)을 포함한 모든 타입을 반응형으로 만들며, .value 프로퍼티로 접근합니다. reactive는 객체나 배열에 사용하며 Proxy로 감싸 .value 없이 직접 접근 가능합니다. 단, reactive는 원시값에 사용할 수 없고 구조 분해 시 반응성이 소실될 수 있으므로 toRefs()를 활용해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'computed와 watch의 차이점과 각각의 사용 시점을 설명해주세요.', 'computed와 watch의 차이점과 각각의 사용 시점을 설명해주세요.', 'COMPOSITION_API',
 'computed는 의존하는 반응형 데이터에서 파생되는 값을 선언적으로 계산하며 결과를 캐싱합니다. watch는 특정 반응형 데이터의 변화를 감지하고 사이드 이펙트(API 호출, 로깅 등)를 수행할 때 사용합니다. 값을 반환해야 하면 computed, 변화에 반응해 작업을 수행해야 하면 watch를 선택하는 것이 원칙입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'v-model 디렉티브의 동작 원리를 설명해주세요.', 'v-model 디렉티브의 동작 원리를 설명해주세요.', 'VUE_CORE',
 'v-model은 폼 요소와 데이터를 양방향 바인딩하는 문법 설탕(syntactic sugar)으로, 내부적으로 :value 바인딩과 @input 이벤트 리스너의 조합입니다. Vue 3에서는 커스텀 컴포넌트에서 modelValue prop과 update:modelValue 이벤트로 동작하며, 여러 v-model을 동시에 사용할 수 있습니다. v-model:title처럼 인자를 지정해 여러 값을 동시에 양방향 바인딩할 수도 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue 컴포넌트의 라이프사이클 훅을 순서대로 설명해주세요.', '뷰 컴포넌트의 라이프사이클 훅을 순서대로 설명해주세요.', 'VUE_CORE',
 'Vue 3 Composition API 기준으로 setup() 실행 → onBeforeMount → onMounted → onBeforeUpdate → onUpdated → onBeforeUnmount → onUnmounted 순서로 진행됩니다. onMounted는 DOM이 마운트된 후 호출되므로 DOM 접근이나 API 호출에 적합하고, onBeforeUnmount는 타이머나 이벤트 리스너 정리에 활용합니다. setup()은 가장 먼저 실행되므로 초기 상태 설정과 의존성 주입에 사용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Props와 Emit을 사용한 부모-자식 컴포넌트 통신 방법을 설명해주세요.', 'Props와 Emit을 사용한 부모-자식 컴포넌트 통신 방법을 설명해주세요.', 'VUE_CORE',
 '부모 컴포넌트는 Props를 통해 자식에게 데이터를 전달하며, 자식은 defineProps()로 받아 읽기 전용으로 사용합니다. 자식이 부모의 데이터를 변경해야 할 때는 Props를 직접 수정하지 않고, defineEmits()로 이벤트를 선언한 후 emit()으로 이벤트를 발생시켜 부모가 처리하게 합니다. 이 단방향 데이터 흐름은 컴포넌트 간 결합도를 낮추고 디버깅을 용이하게 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue에서 Slot의 역할과 사용법을 설명해주세요.', '뷰에서 Slot의 역할과 사용법을 설명해주세요.', 'VUE_CORE',
 'Slot은 부모 컴포넌트가 자식 컴포넌트의 템플릿 일부를 채울 수 있게 하는 콘텐츠 배포 메커니즘입니다. 기본 슬롯은 <slot> 태그로 선언하고, 이름 있는 슬롯은 <slot name="header">로 여러 영역을 지정할 수 있습니다. Scoped Slot을 사용하면 자식 데이터를 부모의 슬롯 콘텐츠에서 접근할 수 있어 재사용 가능한 컴포넌트 설계에 매우 유용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Pinia와 Vuex의 차이점은 무엇인가요?', '피니아와 뷰엑스의 차이점은 무엇인가요?', 'PINIA',
 'Pinia는 Vue 3 공식 상태관리 라이브러리로 Vuex보다 간결한 API를 제공합니다. Vuex는 mutations/actions를 분리해야 하지만 Pinia는 actions만으로 상태 변경이 가능해 boilerplate가 줄어듭니다. 또한 Pinia는 TypeScript 지원이 뛰어나고, DevTools 통합, 모듈 분리가 자연스러우며, Vue 2/3 모두 지원합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Pinia에서 store를 정의하고 사용하는 기본 방법을 설명해주세요.', '피니아에서 store를 정의하고 사용하는 기본 방법을 설명해주세요.', 'PINIA',
 'defineStore()를 사용해 고유 id, state(반환 함수), getters(computed 역할), actions(메서드)를 정의합니다. 컴포넌트에서는 const store = useXxxStore()로 store 인스턴스를 가져와 state와 actions에 접근합니다. storeToRefs()를 사용하면 반응성을 유지한 채로 구조 분해할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue Router의 기본 개념과 설정 방법을 설명해주세요.', '뷰 Router의 기본 개념과 설정 방법을 설명해주세요.', 'ROUTING',
 'Vue Router는 SPA에서 URL과 컴포넌트를 매핑하는 공식 라우팅 라이브러리입니다. createRouter()로 history 모드(createWebHistory 또는 createWebHashHistory)와 routes 배열을 설정하고, 앱에 use(router)로 플러그인을 등록합니다. 템플릿에서 <RouterView>로 현재 라우트 컴포넌트를 렌더링하고, <RouterLink>로 선언적 네비게이션을 구현합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue Router의 동적 라우팅(Dynamic Route)이란 무엇인가요?', '뷰 Router의 동적 라우팅(Dynamic Route)이란 무엇인가요?', 'ROUTING',
 '동적 라우팅은 /users/:id처럼 URL의 일부를 파라미터로 처리하는 방식입니다. 컴포넌트에서는 useRoute().params.id로 파라미터 값을 읽을 수 있습니다. props: true 옵션을 설정하면 라우트 파라미터가 컴포넌트의 Props로 자동 전달되어 컴포넌트가 라우터에 직접 의존하지 않아도 됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue Router의 Navigation Guard란 무엇이며 어떻게 사용하나요?', '뷰 Router의 Navigation Guard란 무엇이며 어떻게 사용하나요?', 'ROUTING',
 'Navigation Guard는 라우트 이동 전후에 실행되는 훅으로 인증, 권한 확인, 데이터 로딩 등에 활용합니다. 전역 가드(beforeEach), 라우트별 가드(beforeEnter), 컴포넌트 내 가드(onBeforeRouteLeave 등) 세 종류가 있습니다. next() 또는 return false/redirect로 이동을 허가하거나 차단할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'v-if와 v-show의 차이점을 설명해주세요.', 'v-if와 v-show의 차이점을 설명해주세요.', 'VUE_CORE',
 'v-if는 조건이 false일 때 DOM 요소를 완전히 제거하고 true일 때 다시 생성하므로, 초기 렌더링 비용이 있지만 숨겨진 상태에서의 메모리 비용이 없습니다. v-show는 display: none으로 CSS만 제어하여 DOM은 항상 유지하므로 토글 비용이 낮지만 항상 렌더링됩니다. 자주 토글되는 UI에는 v-show, 조건부 렌더링 빈도가 낮거나 초기 비렌더링이 중요한 경우 v-if가 적합합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue에서 key 속성은 왜 중요한가요?', '뷰에서 key 속성은 왜 중요한가요?', 'VUE_CORE',
 'key는 Vue의 Virtual DOM diffing 알고리즘이 각 노드를 식별하는 데 사용하는 힌트입니다. v-for에서 key가 없으면 Vue는 위치 기반으로 DOM을 재사용해 컴포넌트 상태가 잘못된 요소에 남는 버그가 발생할 수 있습니다. 고유하고 안정적인 key(예: id)를 제공하면 리스트 재정렬이나 삭제 시 올바른 DOM 업데이트가 보장됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'TypeScript에서 ref()를 사용할 때 타입을 지정하는 방법을 설명해주세요.', '타입스크립트에서 ref()를 사용할 때 타입을 지정하는 방법을 설명해주세요.', 'COMPOSITION_API',
 'ref<T>(initialValue) 형태로 제네릭을 명시하면 해당 타입의 Ref<T>가 생성됩니다. 예를 들어 ref<string>(\'\') 또는 ref<User | null>(null)처럼 사용합니다. 초기값으로 타입 추론이 가능한 경우 제네릭을 생략해도 되지만, null/undefined를 초기값으로 쓰거나 유니언 타입이 필요할 때는 명시적 타입 지정이 필수입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'defineProps()에서 TypeScript 타입을 정의하는 방법을 설명해주세요.', 'defineProps()에서 타입스크립트 타입을 정의하는 방법을 설명해주세요.', 'COMPOSITION_API',
 'Vue 3에서는 defineProps<{ title: string; count: number }>() 형태로 제네릭에 타입을 직접 전달하는 타입 기반 선언 방식을 사용합니다. 이 방식은 런타임 선언(객체 문법)과 동시에 사용할 수 없으며, 컴파일 시점에 런타임 props 옵션으로 변환됩니다. 기본값이 필요하면 withDefaults(defineProps<Props>(), { count: 0 })를 사용합니다. interface나 type alias로 Props 타입을 별도 정의할 수도 있으며, 같은 파일 내에 선언된 타입만 참조 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue에서 이벤트 수식어(Event Modifier)를 설명해주세요.', '뷰에서 이벤트 수식어(Event Modifier)를 설명해주세요.', 'VUE_CORE',
 '@click.prevent는 event.preventDefault()를 자동 호출하고, .stop은 event.stopPropagation()으로 이벤트 버블링을 막습니다. .once는 이벤트를 한 번만 처리하고, .passive는 스크롤 성능 최적화에 사용합니다. 수식어는 체이닝이 가능하며 핸들러 함수를 간결하게 유지할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue에서 Template Ref로 DOM 요소에 직접 접근하는 방법을 설명해주세요.', '뷰에서 Template Ref로 DOM 요소에 직접 접근하는 방법을 설명해주세요.', 'COMPOSITION_API',
 'Composition API에서는 const inputRef = ref<HTMLInputElement | null>(null)을 선언하고 템플릿에서 <input ref="inputRef">로 연결합니다. onMounted 이후 inputRef.value를 통해 DOM 요소에 접근할 수 있습니다. 컴포넌트 마운트 전에는 null이므로 옵셔널 체이닝이나 null 체크가 필요합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Pinia store에서 getter를 정의하고 사용하는 방법을 설명해주세요.', '피니아 store에서 getter를 정의하고 사용하는 방법을 설명해주세요.', 'PINIA',
 'Pinia의 getter는 Options Store에서 getters 객체로, Setup Store에서는 computed()로 정의합니다. getter는 state를 기반으로 파생된 값을 계산하며 결과가 캐싱됩니다. 컴포넌트에서는 store.getterName으로 접근하며, 다른 getter를 참조하거나 인자를 받는 getter도 정의할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue에서 watchEffect와 watch의 차이점을 설명해주세요.', '뷰에서 watchEffect와 watch의 차이점을 설명해주세요.', 'COMPOSITION_API',
 'watchEffect는 콜백 내부에서 사용된 모든 반응형 데이터를 자동으로 추적하고 즉시 실행됩니다. watch는 감시할 소스를 명시적으로 지정하며 이전 값과 새 값을 모두 받을 수 있고 기본적으로 lazy하게 동작합니다. 의존성을 자동 추적하려면 watchEffect, 특정 값 변화에 반응하거나 이전 값 비교가 필요하면 watch를 사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue 컴포넌트에서 async/await를 사용하는 방법을 설명해주세요.', '뷰 컴포넌트에서 async/await를 사용하는 방법을 설명해주세요.', 'COMPOSITION_API',
 'setup() 자체는 async로 선언할 수 있지만 <Suspense> 없이는 템플릿 렌더링 타이밍이 보장되지 않아 주의가 필요합니다. 일반적으로는 onMounted 훅 안에서 async 함수를 호출하거나, action 함수를 async로 선언해 API 호출을 처리합니다. 에러 처리를 위해 try/catch와 함께 로딩/에러 상태를 별도 ref로 관리하는 것이 권장됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue에서 전역 컴포넌트와 지역 컴포넌트의 차이점을 설명해주세요.', '뷰에서 전역 컴포넌트와 지역 컴포넌트의 차이점을 설명해주세요.', 'VUE_CORE',
 '전역 컴포넌트는 app.component()로 등록하면 모든 컴포넌트에서 import 없이 사용할 수 있지만, Tree-shaking이 불가능해 번들 사이즈가 커질 수 있습니다. 지역 컴포넌트는 사용하는 파일에서 import하고 components 옵션(Options API) 또는 <script setup>에서 직접 선언(Composition API)해 사용합니다. 성능과 유지보수를 위해 지역 등록이 권장됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue에서 v-for 디렉티브 사용 시 주의사항을 설명해주세요.', '뷰에서 v-for 디렉티브 사용 시 주의사항을 설명해주세요.', 'VUE_CORE',
 'v-for는 배열이나 객체를 순회해 반복 렌더링하며, 항상 고유한 :key를 함께 사용해야 합니다. 인덱스를 key로 사용하면 리스트 재정렬 시 불필요한 DOM 재생성이 발생할 수 있으므로 고유 id를 사용하는 것이 권장됩니다. 또한 v-if와 v-for를 같은 요소에 사용하는 것은 피해야 하며, 조건이 필요할 경우 wrapper 요소를 활용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue에서 커스텀 디렉티브(Custom Directive)란 무엇인가요?', '뷰에서 커스텀 디렉티브(Custom Directive)란 무엇인가요?', 'VUE_CORE',
 '커스텀 디렉티브는 DOM 엘리먼트에 저수준 접근이 필요할 때 개발자가 정의하는 디렉티브입니다. mounted, updated 등의 훅에서 el(DOM 요소), binding(값, 인자 등)을 받아 직접 DOM을 조작합니다. 예를 들어 v-focus는 마운트 시 자동으로 포커스를 주는 디렉티브로, 컴포넌트 추상화가 과도한 경우 직접 DOM 제어에 유용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue에서 Plugin이란 무엇이며 어떻게 만드나요?', '뷰에서 Plugin이란 무엇이며 어떻게 만드나요?', 'VUE_CORE',
 'Plugin은 install(app) 메서드를 가진 객체 또는 함수로, app.use()로 등록해 전역 기능을 추가합니다. 전역 컴포넌트/디렉티브 등록, app.config.globalProperties 확장, provide/inject 활용 등 다양한 용도로 사용됩니다. Vue Router, Pinia 같은 라이브러리도 플러그인 형태로 제공됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'TypeScript에서 interface와 type의 차이점을 설명해주세요.', '타입스크립트에서 interface와 type의 차이점을 설명해주세요.', 'VUE_CORE',
 'interface는 선언 병합(declaration merging)이 가능해 같은 이름으로 여러 번 선언하면 자동으로 합쳐지며, 클래스 구현(implements)에 주로 사용됩니다. type은 유니언, 인터섹션, 튜플 등 복잡한 타입 조합에 유연하고 원시 타입 별칭도 가능합니다. Vue 컴포넌트의 Props 타입 정의에는 두 방식 모두 사용 가능하며, 일관성 있게 팀 컨벤션을 정하는 것이 중요합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Pinia에서 action을 통해 비동기 처리를 하는 방법을 설명해주세요.', '피니아에서 action을 통해 비동기 처리를 하는 방법을 설명해주세요.', 'PINIA',
 'Pinia action은 일반 async 함수로 선언하면 비동기 처리가 가능합니다. action 내부에서 API를 호출하고 결과를 state에 저장하는 패턴이 일반적이며, try/catch로 에러를 처리합니다. Vuex와 달리 별도의 mutation 없이 action에서 직접 state를 수정할 수 있어 코드가 간결합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:JUNIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue에서 Transition 컴포넌트를 사용하는 방법을 설명해주세요.', '뷰에서 Transition 컴포넌트를 사용하는 방법을 설명해주세요.', 'VUE_CORE',
 '<Transition> 컴포넌트로 단일 요소의 진입/퇴장 애니메이션을 처리합니다. v-enter-from, v-enter-active, v-enter-to, v-leave-from, v-leave-active, v-leave-to 클래스를 CSS로 정의해 전환 효과를 구현합니다. name 속성으로 클래스 접두사를 변경할 수 있고, JavaScript 훅도 지원합니다.',
 'MODEL_ANSWER', TRUE, NOW());


-- ============================================================
-- MID (30문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:VUE_TS:LANGUAGE_FRAMEWORK', 'Composition API를 활용한 Composable(커스텀 훅) 작성 방법과 장점을 설명해주세요.', 'Composition 에이피아이를 활용한 Composable(커스텀 훅) 작성 방법과 장점을 설명해주세요.', 'COMPOSITION_API',
 'Composable은 use로 시작하는 함수로 반응형 상태와 로직을 캡슐화해 여러 컴포넌트에서 재사용합니다. 내부에서 ref, computed, watch 등을 사용하고 필요한 값과 함수를 반환합니다. Options API의 Mixin과 달리 네이밍 충돌, 암묵적 의존성, 타입 추론 부재 문제가 없어 대규모 프로젝트에서 더 명확한 코드 구조를 만들 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:VUE_TS:LANGUAGE_FRAMEWORK', 'Provide/Inject 패턴의 동작 원리와 사용 시점을 설명해주세요.', 'Provide/Inject 패턴의 동작 원리와 사용 시점을 설명해주세요.', 'COMPOSITION_API',
 'Provide/Inject는 조상 컴포넌트가 provide()로 값을 제공하고, 깊이 중첩된 자손이 inject()로 이를 받는 의존성 주입 패턴입니다. Props drilling 문제를 해결하며, InjectionKey<T>를 사용하면 타입 안전하게 주입할 수 있습니다. 단, 데이터 흐름이 불명확해질 수 있으므로 테마, 로케일, 앱 설정 등 광범위하게 공유되는 값에 적합하고, 컴포넌트 간 긴밀한 상태는 Pinia가 더 명확합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue 템플릿 컴파일 과정을 설명해주세요.', '뷰 템플릿 컴파일 과정을 설명해주세요.', 'VUE_CORE',
 'Vue 템플릿은 빌드 타임에 render 함수로 컴파일됩니다. 컴파일러는 템플릿을 파싱해 AST를 생성하고, 정적 노드 호이스팅(Static Hoisting)과 패치 플래그(Patch Flags) 최적화를 적용해 최종 render 함수를 생성합니다. 정적 콘텐츠는 호이스팅되어 재렌더링 시 비교 대상에서 제외되므로 Virtual DOM diffing 비용이 크게 감소합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:VUE_TS:LANGUAGE_FRAMEWORK', 'readonly()와 shallowRef()의 사용 목적을 설명해주세요.', 'readonly()와 shallowRef()의 사용 목적을 설명해주세요.', 'VUE_CORE',
 'readonly()는 반응형 객체를 읽기 전용으로 만들어 자식 컴포넌트나 composable이 부모의 state를 직접 변경하지 못하도록 방어합니다. shallowRef()는 .value의 교체만 추적하고 내부 깊은 속성 변화는 추적하지 않아, 대용량 불변 객체를 다룰 때 불필요한 반응성 오버헤드를 줄입니다. 두 API 모두 성능과 불변성 보장이 필요한 상황에서 의도적으로 반응성 범위를 제한하는 데 사용됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:VUE_TS:LANGUAGE_FRAMEWORK', 'Pinia에서 store를 모듈화하여 설계하는 전략을 설명해주세요.', '피니아에서 store를 모듈화하여 설계하는 전략을 설명해주세요.', 'PINIA',
 'Pinia는 단일 스토어 대신 도메인별로 여러 store를 나눠 설계하는 것을 권장합니다. 예를 들어 useAuthStore, useCartStore, useUserStore처럼 분리해 단일 책임 원칙을 따릅니다. store 간 참조가 필요하면 action 내부에서 다른 store를 직접 import해 사용할 수 있으며, 이는 Vuex의 module 시스템보다 간결합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue에서 Teleport 컴포넌트의 역할과 사용 사례를 설명해주세요.', '뷰에서 Teleport 컴포넌트의 역할과 사용 사례를 설명해주세요.', 'VUE_CORE',
 'Teleport는 컴포넌트 트리 구조를 유지하면서 실제 DOM 렌더링 위치를 다른 곳으로 이동시킵니다. <Teleport to="body">는 모달, 토스트, 툴팁처럼 z-index나 overflow 문제를 피해 body에 직접 렌더링해야 할 때 유용합니다. 컴포넌트 로직과 Props 연결은 원래 위치에서 유지되므로 부모와의 통신은 정상적으로 동작합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:VUE_TS:LANGUAGE_FRAMEWORK', 'v-if와 v-show 성능 차이를 실제 시나리오로 설명해주세요.', 'v-if와 v-show 성능 차이를 실제 시나리오로 설명해주세요.', 'PERFORMANCE',
 '탭 전환 UI처럼 빈번한 토글이 발생하는 경우 v-show가 적합합니다. DOM이 항상 존재하므로 초기 비용이 있지만 전환 시 display 속성만 바꿔 빠릅니다. 반면 인증된 사용자만 보는 관리자 패널처럼 초기 렌더링이 거의 없는 요소는 v-if가 유리하며, 사용자가 접근하지 않을 가능성이 높다면 초기 DOM 생성 비용을 절감할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:VUE_TS:LANGUAGE_FRAMEWORK', 'TypeScript로 Pinia store를 타입 안전하게 설계하는 방법을 설명해주세요.', '타입스크립트로 피니아 store를 타입 안전하게 설계하는 방법을 설명해주세요.', 'PINIA',
 'Setup Store 방식에서 반환 타입을 명시하거나 state 인터페이스를 별도로 정의합니다. storeToRefs()를 사용할 때 구조 분해된 ref가 올바른 타입을 유지하는지 확인이 필요합니다. defineStore()의 두 번째 인자를 Options 형식으로 사용할 경우 state() 반환 타입이 자동 추론되지만, 복잡한 중첩 타입은 명시적 인터페이스 정의가 안전합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue 컴포넌트에서 메모이제이션을 활용한 성능 최적화 방법을 설명해주세요.', '뷰 컴포넌트에서 메모이제이션을 활용한 성능 최적화 방법을 설명해주세요.', 'PERFORMANCE',
 'computed()는 의존 데이터가 변경되지 않으면 캐싱된 값을 반환해 불필요한 연산을 방지합니다. 자식 컴포넌트를 v-memo 디렉티브로 감싸면 지정 의존성이 변경될 때만 재렌더링됩니다. 또한 무거운 컴포넌트는 defineAsyncComponent()로 지연 로딩해 초기 번들을 분리하는 것이 효과적입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue에서 Error Boundary를 구현하는 방법을 설명해주세요.', '뷰에서 Error Boundary를 구현하는 방법을 설명해주세요.', 'VUE_CORE',
 'Vue에서는 onErrorCaptured 훅을 사용해 자식 컴포넌트에서 발생한 에러를 포착합니다. 에러 캡처 컴포넌트에서 onErrorCaptured를 등록하고 false를 반환하면 에러가 상위로 전파되지 않습니다. app.config.errorHandler로 전역 에러 핸들러를 설정하면 캐치되지 않은 에러를 모니터링 서비스로 전송하는 등 공통 처리가 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue Router에서 Lazy Loading을 구현하는 방법과 장점을 설명해주세요.', '뷰 Router에서 Lazy Loading을 구현하는 방법과 장점을 설명해주세요.', 'ROUTING',
 '라우트 컴포넌트를 () => import(''./views/UserView.vue'') 형태의 동적 import로 정의하면 해당 라우트 방문 시 청크가 로드됩니다. 이를 통해 초기 번들 크기를 줄여 첫 로딩 속도를 개선합니다. Vite에서는 파일 경로 기반으로 자동 청크명이 지정되며, rollupOptions.output.manualChunks로 직접 제어할 수도 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue에서 Suspense 컴포넌트의 역할과 사용 방법을 설명해주세요.', '뷰에서 Suspense 컴포넌트의 역할과 사용 방법을 설명해주세요.', 'VUE_CORE',
 '<Suspense>는 비동기 컴포넌트(async setup, defineAsyncComponent)의 로딩 완료를 기다리면서 fallback 콘텐츠를 표시합니다. #default 슬롯에 비동기 컴포넌트를, #fallback 슬롯에 로딩 표시를 배치합니다. 여러 비동기 컴포넌트가 중첩되어도 최상위 Suspense가 모두 해결될 때까지 fallback을 유지하므로 로딩 상태 관리가 단순해집니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue에서 Virtual DOM의 diffing 알고리즘을 설명해주세요.', '뷰에서 Virtual DOM의 diffing 알고리즘을 설명해주세요.', 'VUE_CORE',
 'Vue 3의 diffing은 같은 레벨의 노드를 비교하며, key가 같은 노드를 먼저 매칭합니다. 패치 플래그(Patch Flags)를 통해 동적 바인딩이 있는 노드만 선별적으로 비교해 Vue 2보다 효율이 높습니다. 블록 트리(Block Tree) 구조로 정적 노드를 건너뛰어 업데이트가 필요한 부분만 처리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue 3에서 Render Function과 JSX를 사용하는 방법을 설명해주세요.', '뷰 3에서 Render Function과 JSX를 사용하는 방법을 설명해주세요.', 'VUE_CORE',
 'h() 함수를 사용해 Render Function을 직접 작성하거나, JSX/TSX 문법(@vitejs/plugin-vue-jsx 플러그인 설치)을 사용할 수 있습니다. 렌더 함수는 동적 컴포넌트, 복잡한 로직 분기가 필요할 때 템플릿보다 유연합니다. TypeScript와 함께 JSX를 사용하면 컴포넌트 Props 타입 검사가 인라인으로 이루어집니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue에서 컴포넌트 테스트(Unit Test)를 작성하는 방법을 설명해주세요.', '뷰에서 컴포넌트 테스트(Unit Test)를 작성하는 방법을 설명해주세요.', 'VUE_CORE',
 'Vue Test Utils와 Vitest/Jest를 조합해 컴포넌트를 마운트하고 렌더 결과, Props, Emit, 사용자 이벤트를 검증합니다. mount() 또는 shallowMount()로 컴포넌트를 렌더링하고 wrapper.find(), wrapper.trigger(), wrapper.emitted()로 동작을 확인합니다. Pinia store는 createTestingPinia()로 목(mock) 상태를 주입해 격리된 단위 테스트를 작성합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue 프로젝트에서 환경변수를 관리하는 방법을 설명해주세요.', '뷰 프로젝트에서 환경변수를 관리하는 방법을 설명해주세요.', 'VUE_CORE',
 'Vite 기반 Vue 프로젝트에서는 .env, .env.development, .env.production 파일로 환경별 설정을 관리합니다. VITE_ 접두사가 붙은 변수만 클라이언트 코드에 노출되며 import.meta.env.VITE_API_URL로 접근합니다. 민감한 API 키는 VITE_ 접두사 없이 서버 사이드에서만 사용하거나, 백엔드를 통해 프록시해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue에서 컴포넌트 간 상태 동기화를 위한 전략을 설명해주세요.', '뷰에서 컴포넌트 간 상태 동기화를 위한 전략을 설명해주세요.', 'PINIA',
 '형제 컴포넌트 간 상태는 공통 부모를 통해 Lifting State Up하거나 Pinia store를 활용합니다. 서버 상태는 vue-query(TanStack Query)로 캐싱과 동기화를 관리하고, 로컬 UI 상태는 컴포넌트 내부 ref로 관리합니다. Pinia의 $subscribe로 store 변화를 구독해 로컬 스토리지 동기화 등 부수 효과를 처리할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue Router의 Scroll Behavior를 커스터마이즈하는 방법을 설명해주세요.', '뷰 Router의 Scroll Behavior를 커스터마이즈하는 방법을 설명해주세요.', 'ROUTING',
 'createRouter()의 scrollBehavior(to, from, savedPosition) 함수로 라우트 이동 시 스크롤 동작을 제어합니다. savedPosition이 있으면 뒤로 가기 시 이전 위치로 복원하고, 앵커(#hash)가 있으면 해당 요소로 스크롤할 수 있습니다. { behavior: ''smooth'' }를 반환하면 부드러운 스크롤 효과를 적용할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:VUE_TS:LANGUAGE_FRAMEWORK', 'Pinia에서 Plugin을 만드는 방법과 사용 사례를 설명해주세요.', '피니아에서 Plugin을 만드는 방법과 사용 사례를 설명해주세요.', 'PINIA',
 'Pinia Plugin은 pinia.use(myPlugin) 형태로 등록하며, 모든 store에 공통 기능을 추가합니다. 대표적인 사용 사례는 로컬 스토리지 퍼시스턴스, 로깅, 개발 도구 연동입니다. 플러그인 함수는 { store, app, pinia, options }를 받아 store에 프로퍼티나 메서드를 추가하거나 store 변화를 구독할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue에서 접근성(a11y)을 고려한 컴포넌트 개발 방법을 설명해주세요.', '뷰에서 접근성(a11y)을 고려한 컴포넌트 개발 방법을 설명해주세요.', 'VUE_CORE',
 'aria-label, role 속성을 적절히 사용하고, 키보드 탐색이 가능하도록 tabindex와 키보드 이벤트 핸들러를 구현합니다. 모달 열림 시 포커스 트랩(focus trap)을 적용하고 닫힐 때 원래 포커스 위치로 복원합니다. eslint-plugin-vuejs-accessibility를 설정해 개발 중 접근성 이슈를 자동으로 감지하는 것이 권장됩니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue에서 국제화(i18n)를 구현하는 방법을 설명해주세요.', '뷰에서 국제화(국제화)를 구현하는 방법을 설명해주세요.', 'VUE_CORE',
 'vue-i18n 라이브러리를 사용해 locale별 메시지 파일을 정의하고 $t(''key'') 또는 useI18n().t(''key'')로 번역된 문자열을 사용합니다. 복수형, 날짜/숫자 포맷, 동적 파라미터 등을 지원하며 lazy loading으로 필요한 locale만 로드할 수 있습니다. Composition API와 함께 useI18n() composable을 사용하면 타입 안전한 번역 키 접근도 가능합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue에서 코드 스플리팅(Code Splitting)을 구현하는 전략을 설명해주세요.', '뷰에서 코드 스플리팅(Code Splitting)을 구현하는 전략을 설명해주세요.', 'PERFORMANCE',
 '라우트 레벨 스플리팅은 동적 import를 통해 각 페이지를 별도 청크로 분리하는 가장 기본적인 방법입니다. defineAsyncComponent()로 컴포넌트 레벨 스플리팅도 가능하며, 무거운 컴포넌트(차트, 에디터 등)에 적합합니다. Vite의 rollupOptions.output.manualChunks로 공통 의존성을 별도 청크로 분리해 캐시 효율을 높이는 것도 중요한 전략입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue에서 Form Validation을 처리하는 방법을 설명해주세요.', '뷰에서 Form Validation을 처리하는 방법을 설명해주세요.', 'VUE_CORE',
 'VeeValidate 또는 Zod와 같은 스키마 검증 라이브러리와 Vue를 조합해 선언적 폼 검증을 구현합니다. VeeValidate의 useForm(), useField()는 Composition API와 자연스럽게 통합되며, Zod 스키마를 toTypedSchema()로 연결해 TypeScript 타입과 검증 규칙을 동시에 정의할 수 있습니다. 에러 메시지는 v-model로 바인딩된 error 상태를 통해 템플릿에 표시합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue에서 반응형 데이터를 다룰 때 흔한 실수와 해결 방법을 설명해주세요.', '뷰에서 반응형 데이터를 다룰 때 흔한 실수와 해결 방법을 설명해주세요.', 'COMPOSITION_API',
 '가장 흔한 실수는 reactive() 객체를 구조 분해해 반응성을 잃는 것으로, toRefs()로 해결합니다. ref() 값에 .value 없이 접근하거나 reactive의 속성을 새 변수에 할당하면 Proxy 연결이 끊어집니다. 또한 reactive() 객체 자체를 다른 값으로 재할당하면 원래 Proxy 참조가 사라져 반응성이 소실되므로, 속성 단위로 수정하거나 Object.assign을 사용해야 합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue Router에서 Meta 필드를 활용한 라우트 권한 처리를 설명해주세요.', '뷰 Router에서 Meta 필드를 활용한 라우트 권한 처리를 설명해주세요.', 'ROUTING',
 '라우트 정의에 meta: { requiresAuth: true, roles: [''admin''] } 형태로 메타 정보를 추가합니다. beforeEach 전역 가드에서 to.meta.requiresAuth를 확인하고 인증 상태에 따라 리다이렉트하거나 접근을 허용합니다. TypeScript에서는 RouteMeta 인터페이스를 확장(declare module)해 meta 필드에 타입을 부여합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue에서 CSS 모듈과 Scoped CSS의 차이점을 설명해주세요.', '뷰에서 씨에스에스 모듈과 Scoped 씨에스에스의 차이점을 설명해주세요.', 'VUE_CORE',
 'Scoped CSS는 <style scoped>로 선언하며 Vue 컴파일러가 해당 컴포넌트 요소에만 적용되도록 고유 data attribute를 추가합니다. CSS 모듈은 <style module>로 선언하며 클래스명이 해시화되어 $style.className으로 접근합니다. Scoped CSS는 작성이 직관적이지만 :deep() 선택자가 필요한 경우가 있고, CSS 모듈은 JavaScript에서 클래스명을 직접 참조해 동적 클래스 조작에 유리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue 프로젝트에서 ESLint와 Prettier를 설정하는 방법을 설명해주세요.', '뷰 프로젝트에서 ESLint와 Prettier를 설정하는 방법을 설명해주세요.', 'VUE_CORE',
 'eslint-plugin-vue와 @vue/eslint-config-typescript를 설치하고 extends에 plugin:vue/vue3-recommended, @vue/typescript/recommended를 설정합니다. Prettier와 충돌하지 않도록 eslint-config-prettier를 마지막에 extends에 추가합니다. Vite 프로젝트에서는 vite-plugin-eslint로 개발 서버 실행 중에도 lint 오류를 표시하고, husky + lint-staged로 커밋 전 자동 검사를 설정합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue에서 대용량 리스트 렌더링을 최적화하는 방법을 설명해주세요.', '뷰에서 대용량 리스트 렌더링을 최적화하는 방법을 설명해주세요.', 'PERFORMANCE',
 '수천 개 이상의 리스트는 가상 스크롤(Virtual Scrolling) 라이브러리(vue-virtual-scroller, @tanstack/vue-virtual)를 사용해 뷰포트에 보이는 항목만 DOM에 렌더링합니다. 리스트 항목 컴포넌트에 v-memo를 적용하면 해당 아이템 데이터가 변경될 때만 재렌더링됩니다. 페이지네이션 또는 무한 스크롤로 데이터를 청크 단위로 로드하는 것도 효과적입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue에서 emit 타입을 TypeScript로 정의하는 고급 방법을 설명해주세요.', '뷰에서 emit 타입을 타입스크립트로 정의하는 고급 방법을 설명해주세요.', 'COMPOSITION_API',
 'Vue 3.3+의 defineEmits<{ ''update:modelValue'': [value: string]; ''submit'': [payload: FormData] }>() 형식으로 named tuple 스타일로 선언할 수 있습니다. 이 방식은 각 이벤트의 인자 타입을 배열로 명시해 기존 함수 시그니처 방식보다 간결합니다. TypeScript가 적용된 프로젝트에서는 잘못된 이벤트명이나 인자 타입 불일치를 컴파일 시점에 즉시 검출해 런타임 오류를 방지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:MID:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue Router에서 Programmatic Navigation의 고급 패턴을 설명해주세요.', '뷰 Router에서 Programmatic Navigation의 고급 패턴을 설명해주세요.', 'ROUTING',
 'router.push()는 Promise를 반환하므로 await와 함께 사용해 네비게이션 완료 후 후속 작업을 처리할 수 있습니다. router.resolve()로 이동 없이 라우트 정보를 미리 계산하거나, router.addRoute()로 동적으로 라우트를 추가할 수도 있습니다. NavigationDuplicated 에러는 현재 경로와 동일한 경로로 이동할 때 발생하므로 catch로 처리하거나 이동 전에 현재 경로와 비교하는 가드를 추가합니다.',
 'MODEL_ANSWER', TRUE, NOW());


-- ============================================================
-- SENIOR (30문항)
-- ============================================================

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue 3 반응성 프록시의 내부 동작과 한계점을 설명해주세요.', '뷰 3 반응성 프록시의 내부 동작과 한계점을 설명해주세요.', 'VUE_CORE',
 'Vue 3는 ES Proxy로 객체를 래핑해 get 트랩에서 track(), set 트랩에서 trigger()를 호출합니다. Proxy는 원본 객체와 다른 참조를 가지므로 reactive()로 감싼 객체를 원본과 === 비교하면 다른 결과가 나옵니다. 또한 Map, Set은 별도 처리가 필요하고, WeakMap/WeakSet은 지원하지 않으며, 원시값은 Proxy로 감쌀 수 없어 ref()가 .value 래퍼를 사용하는 이유입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:VUE_TS:LANGUAGE_FRAMEWORK', '복잡한 Composition API Composable을 재사용 가능하게 설계하는 원칙을 설명해주세요.', '복잡한 Composition 에이피아이 Composable을 재사용 가능하게 설계하는 원칙을 설명해주세요.', 'VUE_CORE',
 '입력(인자)으로 MaybeRefOrGetter 타입을 받아 toValue()로 처리하면 ref와 일반 값을 모두 지원하는 유연한 composable을 만들 수 있습니다. 부수 효과는 onUnmounted에서 반드시 정리하고, effectScope()를 사용해 관련 effect를 그룹화해 한 번에 해제할 수 있습니다. composable이 너무 커지면 더 작은 단위로 분리하고, 컴포넌트 의존성을 주입받아 테스트 용이성을 높이는 설계가 중요합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue 프로젝트의 Tree-shaking과 번들 최적화 전략을 설명해주세요.', '뷰 프로젝트의 트리 셰이킹과 번들 최적화 전략을 설명해주세요.', 'PERFORMANCE',
 'Vue 3는 명명된 export를 사용해 사용하지 않는 기능이 번들에서 제거됩니다. Side effects가 없는 모듈에 /*#__PURE__*/ 주석을 추가하면 번들러가 안전하게 제거할 수 있습니다. 런타임 컴파일러를 사용하지 않으면 vue/dist/vue.esm-bundler.js 대신 vue.runtime.esm-bundler.js를 지정해 14KB를 절약할 수 있습니다. Rollup/Vite의 visualizer 플러그인으로 번들 구성을 분석하고 heavy dependency를 확인합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vite의 HMR(Hot Module Replacement) 동작 원리를 설명해주세요.', '바이트의 HMR(Hot Module Replacement) 동작 원리를 설명해주세요.', 'PERFORMANCE',
 'Vite는 Native ESM을 활용해 변경된 모듈만 교체합니다. 파일 변경 시 dev 서버가 WebSocket으로 브라우저에 알리고, 브라우저는 변경된 모듈만 동적 import로 다시 요청합니다. Vue SFC는 @vitejs/plugin-vue가 컴포넌트 핫 리로딩을 처리하며, setup() 상태는 새 모듈로 이전되지 않으므로 상태 초기화가 발생할 수 있습니다. accept() API로 모듈이 자신의 HMR을 직접 처리하거나 부모 모듈이 처리하도록 제어할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'defineAsyncComponent()를 활용한 고급 비동기 컴포넌트 패턴을 설명해주세요.', 'defineAsyncComponent()를 활용한 고급 비동기 컴포넌트 패턴을 설명해주세요.', 'VUE_CORE',
 'defineAsyncComponent()는 loader 함수 외에도 loadingComponent, errorComponent, delay, timeout, onError 옵션을 지원합니다. onError 콜백에서 재시도 로직(retry())을 구현해 네트워크 오류 시 자동 재로드가 가능합니다. Suspense와 조합하면 중첩된 비동기 컴포넌트의 로딩 상태를 단일 Suspense가 통합 관리하며, 에러 바운더리와 결합해 graceful degradation을 구현합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Nuxt 3의 Server Components와 Universal Rendering을 설명해주세요.', 'Nuxt 3의 Server Components와 Universal Rendering을 설명해주세요.', 'ARCHITECTURE',
 'Nuxt 3는 .server.vue 확장자로 서버에서만 렌더링되는 컴포넌트를 지원하며, 클라이언트 번들에 포함되지 않아 민감한 로직과 대용량 의존성을 서버에서 처리할 수 있습니다. Universal Rendering은 첫 요청을 SSR로 HTML을 반환하고 이후 CSR로 동작하는 방식으로 SEO와 성능을 모두 확보합니다. nuxt/image, nuxt/font 같은 모듈은 자동 최적화를 제공하며, useAsyncData(), useFetch()는 서버/클라이언트 중복 요청을 방지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue 3의 Custom Renderer API를 설명하고 사용 사례를 제시해주세요.', '뷰 3의 Custom Renderer 에이피아이를 설명하고 사용 사례를 제시해주세요.', 'ARCHITECTURE',
 'createRenderer()는 DOM 대신 다른 렌더링 타겟(Canvas, WebGL, 터미널 등)에서 Vue를 실행할 수 있게 합니다. createElementNode, patchProp, insert 등 플랫폼별 메서드를 구현해 Vue의 컴포넌트 시스템과 반응성 시스템만 재사용합니다. Three.js 기반의 3D 씬을 Vue 컴포넌트로 선언적으로 관리하는 TresJS, 네이티브 앱 렌더링 등이 대표적 사례입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue 프로젝트의 마이크로 프론트엔드 아키텍처 구현 방법을 설명해주세요.', '뷰 프로젝트의 마이크로 프론트엔드 아키텍처 구현 방법을 설명해주세요.', 'ARCHITECTURE',
 'Module Federation(Webpack/Vite)을 사용하면 독립 배포된 Vue 앱을 런타임에 원격으로 로드할 수 있습니다. Web Components(@vue/web-component-wrapper)로 Vue 컴포넌트를 Custom Element로 변환해 프레임워크 무관하게 통합할 수 있습니다. 상태 공유는 공유 Event Bus 또는 Custom Events를 통해 격리를 유지하면서 처리하고, 라우팅은 앱 셸 라우터가 하위 앱 마운트를 조율합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue 애플리케이션의 성능 측정과 최적화 워크플로우를 설명해주세요.', '뷰 애플리케이션의 성능 측정과 최적화 워크플로우를 설명해주세요.', 'PERFORMANCE',
 'Vue DevTools의 Performance 탭으로 컴포넌트 렌더링 시간을 측정하고, Chrome Performance 패널로 Long Task를 식별합니다. app.config.performance = true를 활성화하면 Vue 내부 타이밍을 브라우저 성능 API로 노출합니다. Core Web Vitals(LCP, FID/INP, CLS)를 기준으로 LCP는 SSR/이미지 최적화, INP는 이벤트 핸들러 최적화, CLS는 레이아웃 안정성으로 접근하며 lighthouse-ci로 CI에서 자동 측정합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue TypeScript 프로젝트의 타입 안전성을 극대화하는 고급 패턴을 설명해주세요.', '뷰 타입스크립트 프로젝트의 타입 안전성을 극대화하는 고급 패턴을 설명해주세요.', 'ARCHITECTURE',
 'Vue 3.3+의 defineModel()로 v-model의 타입 안전한 양방향 바인딩을 선언하고, Generics SFC(<script setup generic="T">)로 재사용 가능한 제네릭 컴포넌트를 작성합니다. 라우트 파라미터와 meta 타입은 declare module ''vue-router''로 확장하고, Pinia store는 인터페이스로 상태 구조를 명시합니다. ReturnType, ComponentProps 유틸리티 타입으로 컴포넌트의 Props 타입을 재사용합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue 프로젝트에서 상태 관리 아키텍처 선택 기준을 설명해주세요.', '뷰 프로젝트에서 상태 관리 아키텍처 선택 기준을 설명해주세요.', 'ARCHITECTURE',
 '컴포넌트 지역 상태는 ref/reactive, 컴포넌트 트리 내 공유는 Provide/Inject, 앱 전역 클라이언트 상태는 Pinia, 서버 상태(캐싱, 재요청)는 TanStack Query가 적합합니다. Pinia와 TanStack Query를 함께 사용하면 서버 상태와 클라이언트 상태를 명확히 분리할 수 있습니다. 상태 크기와 업데이트 빈도, 동기화 요구사항에 따라 계층별 도구를 선택하고 한 도구에 모든 상태를 집중시키는 것을 피합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue 컴포넌트 라이브러리를 만들 때 고려해야 할 사항을 설명해주세요.', '뷰 컴포넌트 라이브러리를 만들 때 고려해야 할 사항을 설명해주세요.', 'ARCHITECTURE',
 '라이브러리는 Vue를 peerDependency로 선언하고 번들에 포함하지 않아야 합니다. vite library mode로 빌드하면 ESM/CJS 두 포맷을 동시에 출력할 수 있습니다. TypeScript 타입 정의 파일(.d.ts)을 vite-plugin-dts로 자동 생성하고, 컴포넌트별 CSS 분리를 지원합니다. Storybook으로 컴포넌트를 문서화하고, Vitest로 단위 테스트, Playwright로 시각적 회귀 테스트를 구성합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue 3의 effectScope() API를 설명하고 고급 사용 사례를 제시해주세요.', '뷰 3의 effectScope() 에이피아이를 설명하고 고급 사용 사례를 제시해주세요.', 'VUE_CORE',
 'effectScope()는 내부에서 생성된 reactive effect(computed, watch, watchEffect)를 그룹화해 scope.stop()으로 한 번에 정리합니다. 컴포넌트 외부에서 전역 반응형 상태를 관리하는 composable에서, 필요할 때 시작하고 완전히 정리해야 하는 경우에 유용합니다. Pinia 내부도 effectScope를 사용해 store의 생명주기를 관리하며, 테스트에서 store를 완전히 초기화할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue 앱의 SSR(Server-Side Rendering) 구현 시 주의사항을 설명해주세요.', '뷰 앱의 에스에스알(Server-Side Rendering) 구현 시 주의사항을 설명해주세요.', 'ARCHITECTURE',
 'SSR에서는 서버와 클라이언트가 동일한 HTML을 생성해야 Hydration Mismatch가 발생하지 않습니다. window, document 같은 브라우저 전용 API는 onMounted나 isClient 체크 후 사용해야 합니다. 요청별 격리(request-scoped state)를 위해 서버 측 singleton store는 요청마다 새 인스턴스를 생성해야 합니다. Pinia는 SSR을 공식 지원하며 useSSRContext()와 함께 서버 상태를 클라이언트에 직렬화합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue 프로젝트의 CI/CD 파이프라인과 테스트 전략을 설명해주세요.', '뷰 프로젝트의 씨아이 씨디 파이프라인과 테스트 전략을 설명해주세요.', 'ARCHITECTURE',
 'Vitest로 composable/store 단위 테스트, Vue Test Utils로 컴포넌트 통합 테스트, Playwright로 E2E 테스트를 구성하는 3계층 테스트 전략이 적합합니다. CI에서 type-check(vue-tsc), lint(ESLint), unit test, build를 순차 실행하고 Playwright E2E는 병렬로 실행합니다. Chromatic 또는 percy로 시각적 회귀 테스트를 추가하고, lighthouse-ci로 성능 예산을 CI에서 자동 검증합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue Router의 Advanced Patterns(중첩 라우트, Parallel Views)를 설명해주세요.', '뷰 Router의 Advanced Patterns(중첩 라우트, Parallel Views)를 설명해주세요.', 'ARCHITECTURE',
 '중첩 라우트는 children 배열로 레이아웃 컴포넌트 내 <RouterView>를 통해 다단계 UI를 구성합니다. named views는 같은 URL에서 이름이 다른 여러 RouterView를 동시에 렌더링해 대시보드 레이아웃에 활용합니다. router.afterEach에서 페이지 타이틀 업데이트, route.meta에 레이아웃 정보를 담아 동적 레이아웃 컴포넌트를 선택하는 패턴도 시니어급 설계입니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue 프로젝트에서 WebSocket 실시간 통신을 구현하는 아키텍처를 설명해주세요.', '뷰 프로젝트에서 웹소켓 실시간 통신을 구현하는 아키텍처를 설명해주세요.', 'ARCHITECTURE',
 'WebSocket 연결은 싱글톤 composable(useWebSocket)로 관리하고, Pinia store에 서버 이벤트를 저장합니다. 재연결 로직(exponential backoff), 연결 상태 관리, 메시지 큐를 composable 내부에 캡슐화합니다. 컴포넌트 언마운트 시 구독 해제, 앱 전체 종료 시 WebSocket 연결 종료를 effectScope와 onUnmounted로 처리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue 앱의 번들 분석과 성능 예산(Performance Budget) 관리 방법을 설명해주세요.', '뷰 앱의 번들 분석과 성능 예산(Performance Budget) 관리 방법을 설명해주세요.', 'PERFORMANCE',
 'rollup-plugin-visualizer로 청크 구성을 분석하고, vite-bundle-analyzer로 의존성 크기를 시각화합니다. 성능 예산은 main bundle 200KB gzipped 이하, 라우트별 청크 50KB 이하 등을 팀에서 합의하고 CI에서 bundlesize 또는 size-limit으로 자동 검증합니다. 과도한 의존성은 lighthouse와 webpack-bundle-analyzer로 식별 후 동적 import 또는 경량 대체 라이브러리로 교체합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:VUE_TS:LANGUAGE_FRAMEWORK', '대규모 Vue 프로젝트의 폴더 구조와 아키텍처 설계 원칙을 설명해주세요.', '대규모 뷰 프로젝트의 폴더 구조와 아키텍처 설계 원칙을 설명해주세요.', 'ARCHITECTURE',
 '기능(feature) 기반 폴더 구조로 도메인별 컴포넌트, composable, store, API 호출을 응집시킵니다. 공유 UI 컴포넌트는 components/ui에, 도메인 독립 유틸리티는 lib/utils에 배치합니다. 레이어드 아키텍처를 적용해 API 클라이언트 계층 → 서비스/store 계층 → 컴포넌트 계층으로 의존성 방향을 단방향으로 유지합니다. 경계를 명확히 하기 위해 index.ts로 공개 API를 명시적으로 정의합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue 프로젝트에서 디자인 시스템과 컴포넌트 라이브러리를 통합하는 전략을 설명해주세요.', '뷰 프로젝트에서 디자인 시스템과 컴포넌트 라이브러리를 통합하는 전략을 설명해주세요.', 'ARCHITECTURE',
 '디자인 토큰을 CSS Custom Properties 또는 JS 상수로 정의하고 컴포넌트에서 참조해 일관성을 유지합니다. Headless UI 패턴(Radix Vue, Ark UI 등)으로 로직과 스타일을 분리하면 디자인 시스템 교체에 유연합니다. Storybook을 단일 진실 공급원으로 컴포넌트 문서화, 디자이너 협업, 시각적 테스트를 통합하고, Figma → Design Token → 코드 연동 파이프라인으로 디자인-개발 간 동기화를 자동화합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue 앱에서 메모리 누수를 방지하는 방법을 설명해주세요.', '뷰 앱에서 메모리 누수를 방지하는 방법을 설명해주세요.', 'PERFORMANCE',
 '이벤트 리스너, setInterval, WebSocket, IntersectionObserver 등은 반드시 onUnmounted에서 정리해야 합니다. watchEffect와 watch는 컴포넌트 언마운트 시 자동 정리되지만, 컴포넌트 외부(전역)에서 생성된 effect는 수동으로 stop()을 호출해야 합니다. 큰 데이터를 ref/reactive로 감싸면 반응성 시스템이 모든 속성을 추적해 메모리 압박이 생길 수 있으므로 markRaw()로 반응성에서 제외합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue 3의 @vue/reactivity 패키지를 프레임워크 외부에서 독립적으로 사용하는 방법을 설명해주세요.', '뷰 3의 어노테이션 vue/reactivity 패키지를 프레임워크 외부에서 독립적으로 사용하는 방법을 설명해주세요.', 'VUE_CORE',
 '@vue/reactivity는 Vue 컴포넌트 시스템 없이 반응성만 단독으로 사용할 수 있는 패키지입니다. Node.js 서버 사이드 상태 관리, 바닐라 JavaScript 프로젝트, 다른 프레임워크와의 통합에서 활용됩니다. ref, reactive, computed, effect를 import해 DOM 업데이트를 직접 구현하거나 상태 변화를 구독하는 것이 가능하며, Vue 컴포넌트 없이 반응형 데이터 파이프라인을 구축할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue 앱의 보안(XSS, CSRF) 처리 방법을 설명해주세요.', '뷰 앱의 보안(XSS, CSRF) 처리 방법을 설명해주세요.', 'ARCHITECTURE',
 'Vue 템플릿의 이중 중괄호 바인딩은 자동으로 HTML을 이스케이프하므로 XSS에 안전합니다. v-html은 신뢰할 수 없는 데이터에 절대 사용하지 않아야 하며, 필요하면 DOMPurify로 새니타이즈합니다. CSRF는 SameSite=Strict 쿠키와 X-CSRF-Token 헤더 조합으로 방어하고, Content-Security-Policy 헤더를 설정해 스크립트 실행 범위를 제한합니다. 의존성 취약점은 npm audit과 Dependabot으로 주기적으로 검사합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue 앱의 오프라인 지원과 PWA 구현 전략을 설명해주세요.', '뷰 앱의 오프라인 지원과 PWA 구현 전략을 설명해주세요.', 'ARCHITECTURE',
 'vite-plugin-pwa로 Service Worker를 자동 생성하고 Workbox 전략(Cache First, Network First 등)으로 리소스별 캐싱 정책을 정의합니다. IndexedDB(Dexie.js)로 오프라인 데이터를 영속화하고, 온라인 복귀 시 Background Sync로 미전송 데이터를 동기화합니다. Pinia 스토어를 pinia-plugin-persistedstate로 로컬 스토리지와 동기화해 앱 재시작 후에도 상태를 복원합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue 프로젝트에서 모노레포(Monorepo) 구성과 패키지 공유 전략을 설명해주세요.', '뷰 프로젝트에서 모노레포(Monorepo) 구성과 패키지 공유 전략을 설명해주세요.', 'ARCHITECTURE',
 'pnpm workspace 또는 Turborepo로 모노레포를 구성하고, 공통 UI 컴포넌트, 유틸리티, 타입 정의를 내부 패키지로 분리합니다. 각 패키지는 독립적으로 빌드/테스트되며, 변경된 패키지만 CI에서 처리하는 incremental build로 빌드 시간을 단축합니다. 내부 패키지의 타입은 TypeScript project references로 경계를 설정하고, 공개 API는 index.ts에서 명시적으로 export해 캡슐화를 유지합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue 앱에서 상태 변화의 Undo/Redo 기능을 구현하는 아키텍처를 설명해주세요.', '뷰 앱에서 상태 변화의 Undo/Redo 기능을 구현하는 아키텍처를 설명해주세요.', 'ARCHITECTURE',
 '커맨드 패턴으로 각 사용자 액션을 execute/undo 메서드를 가진 객체로 캡슐화하고, 실행된 커맨드 스택을 Pinia store에서 관리합니다. Immer.js를 사용하면 불변 상태 스냅샷을 효율적으로 생성해 히스토리 관리가 용이합니다. 메모리 사용을 제한하기 위해 히스토리 스택의 최대 크기를 설정하고, 복잡한 작업은 합성 커맨드(composite command)로 원자적으로 처리합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:VUE_TS:LANGUAGE_FRAMEWORK', '주니어 Vue 개발자에게 코드 리뷰 시 가장 자주 피드백하는 패턴과 개선 방법을 설명해주세요.', '주니어 뷰 개발자에게 코드 리뷰 시 가장 자주 피드백하는 패턴과 개선 방법을 설명해주세요.', 'ARCHITECTURE',
 '가장 흔한 이슈는 reactive() 구조 분해로 인한 반응성 손실, Pinia action 없이 컴포넌트에서 직접 store.state를 변경, 비동기 처리 없는 watch에서의 API 호출, 정리 없는 이벤트 리스너 등록입니다. 개선을 위해 팀 컨벤션 문서화, ESLint 규칙(eslint-plugin-vue, vue-ts 규칙) 자동 검사, 코드 리뷰 체크리스트 운영을 권장합니다. 복잡한 composable은 테스트 코드를 작성하며 설계의 적절성을 검증하도록 유도합니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue에서 nextTick의 내부 동작과 고급 활용 패턴을 설명해주세요.', '뷰에서 nextTick의 내부 동작과 고급 활용 패턴을 설명해주세요.', 'VUE_CORE',
 'nextTick은 Promise.resolve()를 기반으로 마이크로태스크 큐에 콜백을 예약해 Vue의 비동기 DOM 업데이트 사이클 완료 후 실행됩니다. 테스트 환경에서 await nextTick()은 컴포넌트 상태 변경 후 DOM 업데이트를 기다리는 표준 패턴입니다. Vue 3에서는 flushPromises()와 조합해 비동기 작업 전체가 완료된 후의 DOM 상태를 검증할 수 있습니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue 3의 Vapor Mode(컴파일 최적화)에 대해 설명해주세요.', '뷰 3의 Vapor Mode(컴파일 최적화)에 대해 설명해주세요.', 'PERFORMANCE',
 'Vapor Mode는 Vue 3에서 개발 중인 컴파일 전략으로 Virtual DOM을 사용하지 않고 템플릿을 직접 DOM 조작 코드로 컴파일합니다. 이를 통해 런타임 diffing 오버헤드를 제거하고 Solid.js 수준의 세밀한 반응성 업데이트를 구현합니다. 컴포넌트 단위로 opt-in 방식으로 적용할 수 있어 기존 코드베이스와 점진적으로 통합 가능하며, 특히 고빈도 업데이트가 발생하는 컴포넌트의 성능을 극적으로 향상시킵니다.',
 'MODEL_ANSWER', TRUE, NOW());

INSERT IGNORE INTO question_pool (cache_key, content, tts_content, category, model_answer, reference_type, is_active, created_at) VALUES
('FRONTEND:SENIOR:VUE_TS:LANGUAGE_FRAMEWORK', 'Vue 앱에서 국제화(i18n)와 지역화(l10n)를 확장 가능하게 아키텍처링하는 방법을 설명해주세요.', '뷰 앱에서 국제화(국제화)와 지역화(l10n)를 확장 가능하게 아키텍처링하는 방법을 설명해주세요.', 'ARCHITECTURE',
 'vue-i18n의 Composition API 모드와 TypeScript를 결합해 번역 키를 타입 안전하게 관리하며, 잘못된 키 접근을 컴파일 시점에 검출합니다. 언어 파일은 네임스페이스로 분리하고 라우트별 lazy loading을 적용해 초기 번들에 모든 언어를 포함하지 않습니다. RTL 언어 지원은 CSS logical properties를 사용하고, 숫자/날짜/통화 포맷은 Intl API를 래핑한 composable로 통일합니다.',
 'MODEL_ANSWER', TRUE, NOW());
