import { useState } from 'react'
import { Helmet } from 'react-helmet-async'
import { ChevronLeft, ChevronRight, Plus } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Spinner } from '@/components/ui/spinner'
import {
  INTERVIEW_TYPE_LABELS,
  LEVEL_LABELS,
  POSITION_LABELS,
  POSITION_TECH_STACKS,
  TECH_STACK_LABELS,
} from '@/constants/interview-labels'
import {
  useAdminQuestionPools,
  useCreateAdminQuestionPool,
  useUpdateAdminQuestionPool,
} from '@/hooks/use-admin-question-pool'
import type { InterviewType, Level, Position, TechStack } from '@/types/interview'
import type {
  AdminQuestionPoolFilters,
  AdminQuestionPoolItem,
  CreateQuestionPoolRequest,
  UpdateQuestionPoolRequest,
} from '@/types/question-pool'

const PAGE_SIZE = 20

const EMPTY_FILTERS: AdminQuestionPoolFilters = {
  cacheKey: '',
  category: '',
  isActive: '',
  keyword: '',
}

const EMPTY_CREATE_FORM: CreateQuestionPoolRequest = {
  cacheKey: '',
  content: '',
  ttsContent: '',
  category: '',
  bestAnswer: '',
}

interface CacheKeyCriteria {
  interviewType: InterviewType
  level: Level
  position: Position
  techStack: TechStack
}

const EMPTY_CACHE_KEY_CRITERIA: CacheKeyCriteria = {
  interviewType: 'CS_FUNDAMENTAL',
  level: 'JUNIOR',
  position: 'BACKEND',
  techStack: 'JAVA_SPRING',
}

const POSITION_AGNOSTIC_TYPES: InterviewType[] = ['CS_FUNDAMENTAL', 'BEHAVIORAL']

const ADMIN_INTERVIEW_TYPES: InterviewType[] = [
  'CS_FUNDAMENTAL',
  'BEHAVIORAL',
  'LANGUAGE_FRAMEWORK',
  'FULLSTACK_STACK',
  'SYSTEM_DESIGN',
  'UI_FRAMEWORK',
  'BROWSER_PERFORMANCE',
  'INFRA_CICD',
  'CLOUD',
  'DATA_PIPELINE',
  'SQL_MODELING',
]

const LEVELS: Level[] = ['JUNIOR', 'MID', 'SENIOR']

const POSITIONS: Position[] = ['BACKEND', 'FRONTEND', 'DEVOPS', 'DATA_ENGINEER', 'FULLSTACK']

const OTHER_SUBJECT_VALUE = 'OTHER'

const SUBJECT_OPTIONS_BY_INTERVIEW_TYPE: Partial<Record<InterviewType, string[]>> = {
  CS_FUNDAMENTAL: ['자료구조/알고리즘', '운영체제', '네트워크', '데이터베이스'],
  BEHAVIORAL: ['협업', '갈등 해결', '프로젝트 경험', '성장 경험'],
  SYSTEM_DESIGN: ['아키텍처', '확장성', '장애 대응', '트레이드오프'],
  BROWSER_PERFORMANCE: ['렌더링', '번들 최적화', 'Core Web Vitals', '브라우저 API'],
  INFRA_CICD: ['Docker', 'Kubernetes', 'CI/CD', '모니터링'],
  CLOUD: ['AWS', 'GCP', 'Azure', '서버리스'],
  DATA_PIPELINE: ['ETL/ELT', '스트리밍', 'Airflow', 'Spark'],
  SQL_MODELING: ['SQL 최적화', '정규화', '인덱스', '트랜잭션'],
}

const SUBJECT_OPTIONS_BY_TECH_STACK: Record<TechStack, string[]> = {
  JAVA_SPRING: ['Java', 'JVM', 'Spring', 'JPA', '트랜잭션', '테스트'],
  PYTHON_DJANGO: ['Python', 'Django', 'FastAPI', 'Celery', '테스트'],
  NODE_NESTJS: ['Node.js', 'NestJS', '이벤트 루프', 'Prisma', '테스트'],
  GO: ['Go', '고루틴', '채널', 'net/http', '동시성'],
  KOTLIN_SPRING: ['Kotlin', 'Spring', '코루틴', 'JPA', '테스트'],
  REACT_TS: ['React', 'TypeScript', 'Hooks', '상태 관리', '렌더링 최적화'],
  VUE_TS: ['Vue', 'TypeScript', 'Composition API', 'Pinia', '반응성'],
  SVELTE: ['Svelte', 'SvelteKit', '반응성', '스토어', 'SSR'],
  ANGULAR: ['Angular', 'RxJS', 'DI', '상태 관리', '변경 감지'],
  AWS_K8S: ['AWS', 'Kubernetes', 'Terraform', 'EKS', '배포'],
  GCP: ['GCP', 'GKE', 'Cloud Run', 'Pub/Sub', '배포'],
  AZURE: ['Azure', 'AKS', 'Functions', 'DevOps', '배포'],
  SPARK_AIRFLOW: ['Spark', 'Airflow', 'DAG', 'ETL', 'PySpark'],
  FLINK: ['Flink', '스트리밍', '윈도우', '상태 관리', '장애 처리'],
  DBT_SNOWFLAKE: ['dbt', 'Snowflake', '모델링', 'SQL 최적화', '데이터 품질'],
  REACT_SPRING: ['React', 'Spring', 'API 설계', '인증', '배포'],
  REACT_NODE: ['React', 'Node.js', 'API 설계', '인증', '배포'],
  NEXTJS_FULLSTACK: ['Next.js', 'App Router', 'RSC', 'API Routes', '배포'],
}

const isPositionAgnosticType = (type: InterviewType) => POSITION_AGNOSTIC_TYPES.includes(type)

const generateCacheKey = ({ interviewType, level, position, techStack }: CacheKeyCriteria) => {
  if (isPositionAgnosticType(interviewType)) {
    return `${level}:${interviewType}`
  }
  return `${position}:${level}:${techStack}:${interviewType}`
}

const getSubjectOptions = (criteria: CacheKeyCriteria) => {
  if (!isPositionAgnosticType(criteria.interviewType)) {
    return SUBJECT_OPTIONS_BY_TECH_STACK[criteria.techStack]
  }
  return SUBJECT_OPTIONS_BY_INTERVIEW_TYPE[criteria.interviewType] ?? []
}

const formatDate = (isoString: string): string => {
  const date = new Date(isoString)
  const yyyy = date.getFullYear()
  const mm = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const min = String(date.getMinutes()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd} ${hh}:${min}`
}

const truncate = (text: string | null, maxLength: number): string => {
  if (!text) return '-'
  if (text.length <= maxLength) return text
  return `${text.slice(0, maxLength)}...`
}

const statusLabel = (isActive: boolean) => (isActive ? '활성' : '비활성')

const toUpdateForm = (item: AdminQuestionPoolItem): UpdateQuestionPoolRequest => ({
  cacheKey: item.cacheKey,
  content: item.content,
  ttsContent: item.ttsContent ?? '',
  category: item.category ?? '',
  bestAnswer: item.bestAnswer ?? '',
  isActive: item.isActive,
})

const DetailField = ({
  label,
  value,
  className = '',
  valueClassName = '',
}: {
  label: string
  value: string
  className?: string
  valueClassName?: string
}) => (
  <div className={className}>
    <dt className="text-xs font-semibold text-text-tertiary">{label}</dt>
    <dd className={`mt-1 whitespace-pre-wrap text-sm leading-relaxed text-text-primary ${valueClassName}`}>
      {value || '-'}
    </dd>
  </div>
)

interface QuestionPoolTableProps {
  items: AdminQuestionPoolItem[]
  selectedIds: Set<number>
  onToggleSelect: (id: number) => void
  onOpenDetail: (item: AdminQuestionPoolItem) => void
}

const QuestionPoolTable = ({ items, selectedIds, onToggleSelect, onOpenDetail }: QuestionPoolTableProps) => (
  <Card className="overflow-hidden border border-border bg-surface shadow-sm">
    <table className="w-full text-sm">
      <thead>
        <tr className="border-b border-border bg-background">
          <th className="w-12 px-4 py-3 text-left font-semibold text-text-secondary">
            <span className="sr-only">선택</span>
          </th>
          <th className="px-4 py-3 text-left font-semibold text-text-secondary">캐시 키</th>
          <th className="w-24 whitespace-nowrap px-4 py-3 text-left font-semibold text-text-secondary">세부 주제</th>
          <th className="px-4 py-3 text-left font-semibold text-text-secondary">질문</th>
          <th className="px-4 py-3 text-left font-semibold text-text-secondary">모범답안</th>
          <th className="w-16 whitespace-nowrap px-4 py-3 text-left font-semibold text-text-secondary">상태</th>
          <th className="px-4 py-3 text-left font-semibold text-text-secondary">생성일</th>
        </tr>
      </thead>
      <tbody>
        {items.map((item) => (
          <tr
            key={item.id}
            tabIndex={0}
            onClick={() => onOpenDetail(item)}
            onKeyDown={(event) => {
              if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault()
                onOpenDetail(item)
              }
            }}
            className="cursor-pointer border-b border-border/50 transition-colors last:border-0 hover:bg-muted/50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          >
            <td className="px-4 py-3">
              <input
                type="checkbox"
                aria-label={`${item.cacheKey} 선택`}
                checked={selectedIds.has(item.id)}
                onChange={() => onToggleSelect(item.id)}
                onClick={(event) => event.stopPropagation()}
                className="h-4 w-4"
              />
            </td>
            <td className="px-4 py-3 font-medium text-text-primary">{item.cacheKey}</td>
            <td className="w-24 whitespace-nowrap px-4 py-3 text-text-secondary">{item.category ?? '-'}</td>
            <td className="max-w-xs px-4 py-3 text-text-secondary">{truncate(item.content, 48)}</td>
            <td className="max-w-xs px-4 py-3 text-text-tertiary">{truncate(item.bestAnswer, 48)}</td>
            <td className="w-16 whitespace-nowrap px-4 py-3 text-text-secondary">{statusLabel(item.isActive)}</td>
            <td className="px-4 py-3 text-xs text-text-tertiary">{formatDate(item.createdAt)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  </Card>
)

interface QuestionPoolCardsProps {
  items: AdminQuestionPoolItem[]
  selectedIds: Set<number>
  onToggleSelect: (id: number) => void
  onOpenDetail: (item: AdminQuestionPoolItem) => void
}

const QuestionPoolCards = ({ items, selectedIds, onToggleSelect, onOpenDetail }: QuestionPoolCardsProps) => (
  <div className="flex flex-col gap-3">
    {items.map((item) => (
      <Card
        key={item.id}
        role="button"
        tabIndex={0}
        onClick={() => onOpenDetail(item)}
        onKeyDown={(event) => {
          if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault()
            onOpenDetail(item)
          }
        }}
        className="cursor-pointer border border-border bg-surface p-4 shadow-sm transition-colors hover:bg-muted/50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
      >
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-start gap-3">
            <input
              type="checkbox"
              aria-label={`${item.cacheKey} 선택`}
              checked={selectedIds.has(item.id)}
              onChange={() => onToggleSelect(item.id)}
              onClick={(event) => event.stopPropagation()}
              className="mt-0.5 h-4 w-4"
            />
            <div>
            <p className="text-sm font-semibold text-text-primary">{item.cacheKey}</p>
            <p className="mt-1 text-xs text-text-tertiary">{item.category ?? '-'}</p>
            </div>
          </div>
          <span className="text-xs font-medium text-text-secondary">{statusLabel(item.isActive)}</span>
        </div>
        <p className="mt-3 text-sm text-text-secondary">{truncate(item.content, 80)}</p>
        <p className="mt-2 text-xs text-text-tertiary">{truncate(item.bestAnswer, 96)}</p>
        <p className="mt-3 text-xs text-text-tertiary">{formatDate(item.createdAt)}</p>
      </Card>
    ))}
  </div>
)

export const AdminQuestionPoolPage = () => {
  const [page, setPage] = useState(0)
  const [draftFilters, setDraftFilters] = useState<AdminQuestionPoolFilters>(EMPTY_FILTERS)
  const [appliedFilters, setAppliedFilters] = useState<AdminQuestionPoolFilters>(EMPTY_FILTERS)
  const [isCreateOpen, setIsCreateOpen] = useState(false)
  const [createForm, setCreateForm] = useState<CreateQuestionPoolRequest>(EMPTY_CREATE_FORM)
  const [cacheKeyCriteria, setCacheKeyCriteria] = useState<CacheKeyCriteria>(EMPTY_CACHE_KEY_CRITERIA)
  const [selectedSubject, setSelectedSubject] = useState('')
  const [customSubject, setCustomSubject] = useState('')
  const [selectedQuestion, setSelectedQuestion] = useState<AdminQuestionPoolItem | null>(null)
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set())
  const [isEditMode, setIsEditMode] = useState(false)
  const [editForm, setEditForm] = useState<UpdateQuestionPoolRequest | null>(null)

  const { data, isLoading } = useAdminQuestionPools(appliedFilters, page, PAGE_SIZE)
  const createMutation = useCreateAdminQuestionPool()
  const updateMutation = useUpdateAdminQuestionPool()

  const items = data?.data?.content ?? []
  const selectedItems = items.filter((item) => selectedIds.has(item.id))
  const totalElements = data?.data?.totalElements ?? 0
  const totalPages = data?.data?.totalPages ?? 0
  const generatedCacheKey = generateCacheKey(cacheKeyCriteria)
  const shouldShowPositionFields = !isPositionAgnosticType(cacheKeyCriteria.interviewType)
  const availableTechStacks = POSITION_TECH_STACKS[cacheKeyCriteria.position]
  const subjectOptions = getSubjectOptions(cacheKeyCriteria)
  const shouldShowCustomSubject = selectedSubject === OTHER_SUBJECT_VALUE
  const resolvedSubject = shouldShowCustomSubject ? customSubject.trim() : selectedSubject

  const handleFilterChange = (field: keyof AdminQuestionPoolFilters, value: string) => {
    setDraftFilters((current) => ({ ...current, [field]: value }))
  }

  const handleCreateFormChange = (field: keyof CreateQuestionPoolRequest, value: string) => {
    setCreateForm((current) => ({ ...current, [field]: value }))
  }

  const handleCriteriaChange = (field: keyof CacheKeyCriteria, value: string) => {
    setCacheKeyCriteria((current) => {
      if (field === 'position') {
        const nextPosition = value as Position
        return {
          ...current,
          position: nextPosition,
          techStack: POSITION_TECH_STACKS[nextPosition][0],
        }
      }
      return { ...current, [field]: value }
    })
    setSelectedSubject('')
    setCustomSubject('')
  }

  const handleSearch = () => {
    setAppliedFilters(draftFilters)
    setPage(0)
  }

  const handleReset = () => {
    setDraftFilters(EMPTY_FILTERS)
    setAppliedFilters(EMPTY_FILTERS)
    setPage(0)
  }

  const handleCreateSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!createForm.content.trim() || createMutation.isPending) {
      return
    }

    createMutation.mutate({
      ...createForm,
      cacheKey: generatedCacheKey,
      category: resolvedSubject,
    }, {
      onSuccess: () => {
        setCreateForm(EMPTY_CREATE_FORM)
        setCacheKeyCriteria(EMPTY_CACHE_KEY_CRITERIA)
        setSelectedSubject('')
        setCustomSubject('')
        setIsCreateOpen(false)
      },
    })
  }

  const handleOpenDetail = (item: AdminQuestionPoolItem) => {
    setSelectedQuestion(item)
    setEditForm(toUpdateForm(item))
    setIsEditMode(false)
  }

  const handleDetailOpenChange = (open: boolean) => {
    if (!open) {
      setSelectedQuestion(null)
      setEditForm(null)
      setIsEditMode(false)
    }
  }

  const handleToggleSelect = (id: number) => {
    setSelectedIds((current) => {
      const next = new Set(current)
      if (next.has(id)) {
        next.delete(id)
      } else {
        next.add(id)
      }
      return next
    })
  }

  const handleEditFormChange = (field: keyof UpdateQuestionPoolRequest, value: string | boolean) => {
    setEditForm((current) => {
      if (!current) return current
      return { ...current, [field]: value }
    })
  }

  const handleUpdateSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!selectedQuestion || !editForm || updateMutation.isPending) {
      return
    }

    updateMutation.mutate({ id: selectedQuestion.id, request: editForm }, {
      onSuccess: () => handleDetailOpenChange(false),
    })
  }

  const handleBulkStatusChange = async (event: React.ChangeEvent<HTMLSelectElement>) => {
    const nextValue = event.target.value
    if (selectedItems.length === 0 || updateMutation.isPending || !nextValue) {
      return
    }

    const nextIsActive = nextValue === 'true'
    await Promise.all(
      selectedItems.map((item) =>
        updateMutation.mutateAsync({
          id: item.id,
          request: {
            ...toUpdateForm(item),
            isActive: nextIsActive,
          },
        }),
      ),
    )
    setSelectedIds(new Set())
  }

  const handleToggleDetailStatus = () => {
    if (!selectedQuestion || updateMutation.isPending) {
      return
    }

    updateMutation.mutate({
      id: selectedQuestion.id,
      request: {
        ...toUpdateForm(selectedQuestion),
        isActive: !selectedQuestion.isActive,
      },
    }, {
      onSuccess: () => handleDetailOpenChange(false),
    })
  }

  return (
    <div className="min-h-screen bg-background text-text-primary">
      <Helmet>
        <title>질문 풀 관리 - 리허설</title>
        <meta name="robots" content="noindex, nofollow" />
      </Helmet>

      <main className="mx-auto max-w-6xl px-5 py-8 lg:px-10 lg:py-10">
        <div className="mb-6 flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <h1 className="text-xl font-extrabold tracking-tight text-text-primary">질문 풀 관리</h1>
            {!isLoading && (
              <p className="mt-1 text-sm text-text-secondary">총 {totalElements}개의 질문</p>
            )}
          </div>
          <div className="flex flex-col gap-2 sm:flex-row">
            <label className="sr-only" htmlFor="bulk-status-change">
              선택 상태 변경
            </label>
            <select
              id="bulk-status-change"
              aria-label="선택 상태 변경"
              value=""
              onChange={handleBulkStatusChange}
              disabled={selectedIds.size === 0 || updateMutation.isPending}
              className="flex h-10 rounded-md border border-input bg-background px-3 py-2 text-sm shadow-sm disabled:cursor-not-allowed disabled:opacity-50"
            >
              <option value="">선택 상태 변경</option>
              <option value="true">활성으로 변경</option>
              <option value="false">비활성으로 변경</option>
            </select>
            <Button onClick={() => setIsCreateOpen(true)} className="gap-2">
              <Plus size={16} />
              새 질문 추가
            </Button>
          </div>
        </div>

        <Card className="mb-5 border border-border bg-surface p-4 shadow-sm">
          <div className="grid gap-3 lg:grid-cols-5">
            <div>
              <Label htmlFor="cache-key-filter">캐시 키</Label>
              <Input
                id="cache-key-filter"
                value={draftFilters.cacheKey}
                onChange={(event) => handleFilterChange('cacheKey', event.target.value)}
                className="mt-1"
              />
            </div>
            <div>
              <Label htmlFor="category-filter">세부 주제</Label>
              <Input
                id="category-filter"
                value={draftFilters.category}
                onChange={(event) => handleFilterChange('category', event.target.value)}
                className="mt-1"
              />
            </div>
            <div>
              <Label htmlFor="active-filter">활성 상태</Label>
              <select
                id="active-filter"
                value={draftFilters.isActive}
                onChange={(event) => handleFilterChange('isActive', event.target.value)}
                className="mt-1 flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
              >
                <option value="">전체</option>
                <option value="true">활성</option>
                <option value="false">비활성</option>
              </select>
            </div>
            <div>
              <Label htmlFor="keyword-filter">검색어</Label>
              <Input
                id="keyword-filter"
                value={draftFilters.keyword}
                onChange={(event) => handleFilterChange('keyword', event.target.value)}
                className="mt-1"
              />
            </div>
            <div className="flex items-end gap-2">
              <Button type="button" onClick={handleSearch} className="flex-1 whitespace-nowrap">
                검색
              </Button>
              <Button
                type="button"
                variant="secondary"
                onClick={handleReset}
                className="flex-1 whitespace-nowrap"
              >
                초기화
              </Button>
            </div>
          </div>
        </Card>

        {isLoading ? (
          <div className="flex items-center justify-center py-20">
            <Spinner className="h-8 w-8" />
          </div>
        ) : items.length === 0 ? (
          <div className="flex items-center justify-center py-20 text-text-secondary">
            조건에 맞는 질문이 없습니다
          </div>
        ) : (
          <>
            <div className="hidden lg:block">
              <QuestionPoolTable
                items={items}
                selectedIds={selectedIds}
                onToggleSelect={handleToggleSelect}
                onOpenDetail={handleOpenDetail}
              />
            </div>
            <div className="lg:hidden">
              <QuestionPoolCards
                items={items}
                selectedIds={selectedIds}
                onToggleSelect={handleToggleSelect}
                onOpenDetail={handleOpenDetail}
              />
            </div>
          </>
        )}

        {!isLoading && totalPages > 1 && (
          <div className="mt-6 flex items-center justify-center gap-3">
            <Button
              variant="secondary"
              size="sm"
              onClick={() => setPage((current) => Math.max(0, current - 1))}
              disabled={page === 0}
            >
              <ChevronLeft size={16} />
              이전
            </Button>
            <span className="text-sm font-medium text-text-primary">
              {page + 1} / {totalPages}
            </span>
            <Button
              variant="secondary"
              size="sm"
              onClick={() => setPage((current) => Math.min(totalPages - 1, current + 1))}
              disabled={page >= totalPages - 1}
            >
              다음
              <ChevronRight size={16} />
            </Button>
          </div>
        )}
      </main>

      <Dialog open={isCreateOpen} onOpenChange={setIsCreateOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>새 질문 추가</DialogTitle>
            <DialogDescription>면접 조건을 선택하면 캐시 키가 자동으로 생성됩니다.</DialogDescription>
          </DialogHeader>
          <form onSubmit={handleCreateSubmit} className="space-y-4">
            <div className="grid gap-3 sm:grid-cols-2">
              <div>
                <Label htmlFor="create-interview-type">면접 유형</Label>
                <select
                  id="create-interview-type"
                  value={cacheKeyCriteria.interviewType}
                  onChange={(event) => handleCriteriaChange('interviewType', event.target.value)}
                  className="mt-1 flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                >
                  {ADMIN_INTERVIEW_TYPES.map((type) => (
                    <option key={type} value={type}>
                      {INTERVIEW_TYPE_LABELS[type].label}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <Label htmlFor="create-level">레벨</Label>
                <select
                  id="create-level"
                  value={cacheKeyCriteria.level}
                  onChange={(event) => handleCriteriaChange('level', event.target.value)}
                  className="mt-1 flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                >
                  {LEVELS.map((level) => (
                    <option key={level} value={level}>
                      {LEVEL_LABELS[level].label}
                    </option>
                  ))}
                </select>
              </div>
              {shouldShowPositionFields && (
                <>
                  <div>
                    <Label htmlFor="create-position">직무</Label>
                    <select
                      id="create-position"
                      value={cacheKeyCriteria.position}
                      onChange={(event) => handleCriteriaChange('position', event.target.value)}
                      className="mt-1 flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                    >
                      {POSITIONS.map((position) => (
                        <option key={position} value={position}>
                          {POSITION_LABELS[position].label}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <Label htmlFor="create-tech-stack">기술스택</Label>
                    <select
                      id="create-tech-stack"
                      value={cacheKeyCriteria.techStack}
                      onChange={(event) => handleCriteriaChange('techStack', event.target.value)}
                      className="mt-1 flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                    >
                      {availableTechStacks.map((techStack) => (
                        <option key={techStack} value={techStack}>
                          {TECH_STACK_LABELS[techStack].label}
                        </option>
                      ))}
                    </select>
                  </div>
                </>
              )}
            </div>
            <div>
              <Label htmlFor="generated-cache-key">생성될 캐시 키</Label>
              <Input
                id="generated-cache-key"
                value={generatedCacheKey}
                readOnly
                className="mt-1 font-mono text-xs"
              />
            </div>
            <div>
              <Label htmlFor="create-subject">세부 주제</Label>
              <select
                id="create-subject"
                value={selectedSubject}
                onChange={(event) => setSelectedSubject(event.target.value)}
                className="mt-1 flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
              >
                <option value="">선택 안 함</option>
                {subjectOptions.map((subject) => (
                  <option key={subject} value={subject}>
                    {subject}
                  </option>
                ))}
                <option value={OTHER_SUBJECT_VALUE}>기타</option>
              </select>
            </div>
            {shouldShowCustomSubject && (
              <div>
                <Label htmlFor="create-custom-subject">직접 입력할 세부 주제</Label>
                <Input
                  id="create-custom-subject"
                  value={customSubject}
                  onChange={(event) => setCustomSubject(event.target.value)}
                  className="mt-1"
                />
              </div>
            )}
            <div>
              <Label htmlFor="create-content">질문 본문</Label>
              <Input
                id="create-content"
                value={createForm.content}
                onChange={(event) => handleCreateFormChange('content', event.target.value)}
                required
                className="mt-1"
              />
            </div>
            <div>
              <Label htmlFor="create-tts-content">TTS 문구</Label>
              <Input
                id="create-tts-content"
                value={createForm.ttsContent}
                onChange={(event) => handleCreateFormChange('ttsContent', event.target.value)}
                className="mt-1"
              />
            </div>
            <div>
              <Label htmlFor="create-best-answer">모범답안</Label>
              <Input
                id="create-best-answer"
                value={createForm.bestAnswer}
                onChange={(event) => handleCreateFormChange('bestAnswer', event.target.value)}
                className="mt-1"
              />
            </div>
            <DialogFooter>
              <Button
                type="button"
                variant="secondary"
                onClick={() => setIsCreateOpen(false)}
              >
                취소
              </Button>
              <Button
                type="submit"
                disabled={!createForm.content.trim() || createMutation.isPending}
              >
                추가
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={selectedQuestion !== null} onOpenChange={handleDetailOpenChange}>
        <DialogContent className="max-h-screen overflow-y-auto sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>질문 상세</DialogTitle>
            <DialogDescription>질문 풀 row의 전체 내용을 확인하고 수정합니다.</DialogDescription>
          </DialogHeader>

          {selectedQuestion && !isEditMode && (
            <div className="space-y-5">
              <dl className="grid gap-4 sm:grid-cols-2">
                <DetailField
                  label="캐시 키"
                  value={selectedQuestion.cacheKey}
                  className="sm:col-span-2"
                  valueClassName="break-all font-mono text-xs"
                />
                <DetailField label="세부 주제" value={selectedQuestion.category ?? ''} />
                <DetailField label="상태" value={statusLabel(selectedQuestion.isActive)} />
                <DetailField label="생성일" value={formatDate(selectedQuestion.createdAt)} />
                <div className="sm:col-span-2">
                  <DetailField label="질문" value={selectedQuestion.content} />
                </div>
                <div className="sm:col-span-2">
                  <DetailField label="TTS 문구" value={selectedQuestion.ttsContent ?? ''} />
                </div>
                <div className="sm:col-span-2">
                  <DetailField label="모범답안" value={selectedQuestion.bestAnswer ?? ''} />
                </div>
              </dl>
              <DialogFooter>
                <Button type="button" variant="secondary" onClick={handleToggleDetailStatus}>
                  {selectedQuestion.isActive ? '비활성으로 변경' : '활성으로 변경'}
                </Button>
                <Button type="button" onClick={() => setIsEditMode(true)}>
                  수정
                </Button>
              </DialogFooter>
            </div>
          )}

          {selectedQuestion && isEditMode && editForm && (
            <form onSubmit={handleUpdateSubmit} className="space-y-4">
              <div>
                <Label htmlFor="edit-cache-key">수정 캐시 키</Label>
                <Input
                  id="edit-cache-key"
                  value={editForm.cacheKey}
                  onChange={(event) => handleEditFormChange('cacheKey', event.target.value)}
                  required
                  className="mt-1 font-mono text-xs"
                />
              </div>
              <div>
                <Label htmlFor="edit-category">수정 세부 주제</Label>
                <Input
                  id="edit-category"
                  value={editForm.category}
                  onChange={(event) => handleEditFormChange('category', event.target.value)}
                  className="mt-1"
                />
              </div>
              <div>
                <Label htmlFor="edit-content">수정 질문 본문</Label>
                <textarea
                  id="edit-content"
                  value={editForm.content}
                  onChange={(event) => handleEditFormChange('content', event.target.value)}
                  required
                  className="mt-1 flex min-h-24 w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                />
              </div>
              <div>
                <Label htmlFor="edit-tts-content">수정 TTS 문구</Label>
                <textarea
                  id="edit-tts-content"
                  value={editForm.ttsContent}
                  onChange={(event) => handleEditFormChange('ttsContent', event.target.value)}
                  className="mt-1 flex min-h-20 w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                />
              </div>
              <div>
                <Label htmlFor="edit-best-answer">수정 모범답안</Label>
                <textarea
                  id="edit-best-answer"
                  value={editForm.bestAnswer}
                  onChange={(event) => handleEditFormChange('bestAnswer', event.target.value)}
                  className="mt-1 flex min-h-24 w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                />
              </div>
              <div>
                <Label htmlFor="edit-active">수정 활성 상태</Label>
                <select
                  id="edit-active"
                  value={String(editForm.isActive)}
                  onChange={(event) => handleEditFormChange('isActive', event.target.value === 'true')}
                  className="mt-1 flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                >
                  <option value="true">활성</option>
                  <option value="false">비활성</option>
                </select>
              </div>
              <DialogFooter>
                <Button type="button" variant="secondary" onClick={() => setIsEditMode(false)}>
                  취소
                </Button>
                <Button type="submit" disabled={!editForm.content.trim() || updateMutation.isPending}>
                  저장
                </Button>
              </DialogFooter>
            </form>
          )}
        </DialogContent>
      </Dialog>
    </div>
  )
}
