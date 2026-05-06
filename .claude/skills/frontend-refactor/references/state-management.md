# State Management Reference

## §1 Zustand

### 도메인별 스토어 분리

하나의 거대 스토어는 어떤 상태든 변경되면 모든 구독자가 리렌더된다. 도메인별로 분리하면 영향 범위가 좁아진다.

```tsx
// BAD
const useAppStore = create((set) => ({
  interviewId: null, user: null, isModalOpen: false,
}));

// GOOD
const useInterviewStore = create<InterviewStore>((set) => ({ ... }));
const useAuthStore = create<AuthStore>((set) => ({ ... }));
```

### Selector 필수

전체 스토어를 구독하면 무관한 상태 변경에도 리렌더가 발생한다. 개별 selector는 해당 값이 변경될 때만 리렌더를 트리거한다.

```tsx
// BAD: 전체 구독
const store = useInterviewStore();

// GOOD: 개별 selector
const phase = useInterviewStore((s) => s.phase);
const setPhase = useInterviewStore((s) => s.setPhase);
```

### getState() — stale closure 방지

이벤트 핸들러를 useCallback으로 메모이제이션하면 클로저 시점의 상태가 고정된다. getState()는 호출 시점의 최신 상태를 반환한다.

```tsx
const handleKeyDown = useCallback((e: KeyboardEvent) => {
  const { phase } = useInterviewStore.getState();
}, []);
```

### 불변 업데이트

Zustand는 Object.is로 변경을 감지한다. 직접 mutation하면 같은 참조라 변경을 감지하지 못한다.

```tsx
// BAD: mutation → 리렌더 안 됨
addItem: (item) => set((state) => { state.items.push(item); return state; }),

// GOOD: 새 배열 생성
addItem: (item: Item) => set((state) => ({ items: [...state.items, item] })),
```

---

## §2 TanStack Query

### Query Key 중앙화

매직 문자열은 오타 위험이 있고, 계층적 무효화가 불가능하다. 중앙화된 키 객체는 타입 안전성과 계층적 무효화를 제공한다.

```tsx
export const queryKeys = {
  interviews: {
    all: ['interviews'] as const,
    list: () => [...queryKeys.interviews.all, 'list'] as const,
    detail: (id: number) => [...queryKeys.interviews.all, 'detail', id] as const,
    questions: (id: number) => [...queryKeys.interviews.detail(id), 'questions'] as const,
  },
} as const;

// detail 무효화 → questions도 자동 무효화
queryClient.invalidateQueries({ queryKey: queryKeys.interviews.detail(id) });
```

### 커스텀 Query Hook

컴포넌트에 useQuery를 직접 사용하면 queryKey와 queryFn이 분산되어 관리가 어려워진다.

```tsx
// BAD
const { data } = useQuery({
  queryKey: ['interview', id],
  queryFn: () => fetch(`/api/v1/interviews/${id}`).then(r => r.json()),
});

// GOOD
export const useFetchInterview = (id: number) => {
  return useQuery({
    queryKey: queryKeys.interviews.detail(id),
    queryFn: () => apiClient.get<InterviewResponse>(`/api/v1/interviews/${id}`),
    enabled: !!id,
  });
};
```

### Mutation + Optimistic Update

사용자 액션 후 서버 응답을 기다리면 UI가 느리게 느껴진다. optimistic update는 즉시 UI를 갱신하고, 실패 시 롤백한다.

```tsx
export const useUpdateInterview = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: UpdateInterviewRequest) =>
      apiClient.patch<InterviewResponse>(`/api/v1/interviews/${data.id}`, data),

    onMutate: async (newData) => {
      await queryClient.cancelQueries({ queryKey: queryKeys.interviews.detail(newData.id) });
      const previous = queryClient.getQueryData(queryKeys.interviews.detail(newData.id));
      queryClient.setQueryData(
        queryKeys.interviews.detail(newData.id),
        (old: InterviewResponse) => ({ ...old, ...newData })
      );
      return { previous };
    },

    onError: (_err, variables, context) => {
      queryClient.setQueryData(queryKeys.interviews.detail(variables.id), context?.previous);
    },

    onSettled: (_data, _err, variables) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.interviews.detail(variables.id) });
    },
  });
};
```
