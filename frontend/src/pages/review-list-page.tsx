import { useMemo } from 'react'
import { Helmet } from 'react-helmet-async'
import { useSearchParams } from 'react-router-dom'
import { useReviewBookmarkList } from '@/hooks/use-review-bookmarks'
import { INTERVIEW_TYPE_LABELS, POSITION_INTERVIEW_TYPES } from '@/constants/interview-labels'
import { ReviewListHeader } from '@/components/review/review-list-header'
import { ReviewListFilterBar } from '@/components/review/review-list-filter-bar'
import { ReviewCategorySection } from '@/components/review/review-category-section'
import { ReviewEmptyState } from '@/components/review/review-empty-state'
import { AppShell } from '@/components/layout/app-shell'
import type { BookmarkStatus, ReviewBookmarkListItem } from '@/types/review-bookmark'
import type { Position, InterviewType } from '@/types/interview'

const VALID_STATUSES: BookmarkStatus[] = ['all', 'in_progress', 'resolved']
const VALID_POSITIONS: Position[] = ['BACKEND', 'FRONTEND', 'DEVOPS', 'DATA_ENGINEER', 'FULLSTACK']

const isValidStatus = (value: string | null): value is BookmarkStatus =>
  value !== null && (VALID_STATUSES as string[]).includes(value)

const isValidPosition = (value: string | null): value is Position =>
  value !== null && (VALID_POSITIONS as string[]).includes(value)

const isValidInterviewType = (value: string | null, position: Position): value is InterviewType =>
  value !== null && (POSITION_INTERVIEW_TYPES[position] as string[]).includes(value)

export const ReviewListPage = () => {
  const [searchParams, setSearchParams] = useSearchParams()

  const rawStatus = searchParams.get('status')
  const status: BookmarkStatus = isValidStatus(rawStatus) ? rawStatus : 'all'

  const rawPosition = searchParams.get('position')
  const positionFilter: Position | 'ALL' = isValidPosition(rawPosition) ? rawPosition : 'ALL'

  const rawType = searchParams.get('type')
  const interviewTypeFilter: InterviewType | 'ALL' =
    positionFilter !== 'ALL' && isValidInterviewType(rawType, positionFilter) ? rawType : 'ALL'

  const { data, isLoading, isError } = useReviewBookmarkList(status)

  const handleStatusChange = (newStatus: BookmarkStatus) => {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev)
      if (newStatus === 'all') {
        next.delete('status')
      } else {
        next.set('status', newStatus)
      }
      return next
    })
  }

  const handlePositionChange = (newPosition: Position | 'ALL') => {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev)
      if (newPosition === 'ALL') {
        next.delete('position')
        next.delete('type')
      } else {
        next.set('position', newPosition)
        next.delete('type')
      }
      return next
    })
  }

  const handleInterviewTypeChange = (newType: InterviewType | 'ALL') => {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev)
      if (newType === 'ALL') {
        next.delete('type')
      } else {
        next.set('type', newType)
      }
      return next
    })
  }

  const groupedSections = useMemo(() => {
    if (!data) return []

    let filtered: ReviewBookmarkListItem[] = data.items

    if (positionFilter !== 'ALL') {
      filtered = filtered.filter((item) => item.interviewPosition === positionFilter)
    }

    if (interviewTypeFilter !== 'ALL') {
      filtered = filtered.filter((item) => item.interviewType === interviewTypeFilter)
    }

    const grouped = new Map<string, ReviewBookmarkListItem[]>()
    for (const item of filtered) {
      const existing = grouped.get(item.interviewType) ?? []
      grouped.set(item.interviewType, [...existing, item])
    }

    if (status === 'all') {
      for (const [key, items] of grouped) {
        grouped.set(key, [...items].sort((a, b) => {
          const aResolved = a.resolvedAt !== null ? 1 : 0
          const bResolved = b.resolvedAt !== null ? 1 : 0
          return aResolved - bResolved
        }))
      }
    }

    const allTypes = Object.keys(INTERVIEW_TYPE_LABELS) as InterviewType[]

    return allTypes
      .filter((type) => grouped.has(type))
      .map((type) => ({
        key: type,
        label: INTERVIEW_TYPE_LABELS[type].label,
        items: grouped.get(type) ?? [],
      }))
  }, [data, positionFilter, interviewTypeFilter, status])

  const totalCount = data?.total ?? 0
  const hasData = totalCount > 0
  const isStatusFiltered = status !== 'all'
  const hasFilteredResults = groupedSections.length > 0 && groupedSections.some((s) => s.items.length > 0)
  const isFiltered = positionFilter !== 'ALL' || interviewTypeFilter !== 'ALL'

  return (
    <AppShell>
      <Helmet>
        <title>복습 목록 - 리허설</title>
        <meta name="robots" content="noindex, nofollow" />
      </Helmet>
      <ReviewListHeader total={totalCount} />

      {isLoading && (
        <div className="flex items-center justify-center py-24">
          <p className="text-[14px] text-text-tertiary">불러오는 중...</p>
        </div>
      )}

      {isError && (
        <div className="flex items-center justify-center py-24">
          <p className="text-[14px] text-error">
            목록을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.
          </p>
        </div>
      )}

      {!isLoading && !isError && (
        <>
          {(hasData || isStatusFiltered) && (
            <ReviewListFilterBar
              status={status}
              onStatusChange={handleStatusChange}
              positionFilter={positionFilter}
              onPositionChange={handlePositionChange}
              interviewTypeFilter={interviewTypeFilter}
              onInterviewTypeChange={handleInterviewTypeChange}
            />
          )}

          {!hasData && !isStatusFiltered ? (
            <ReviewEmptyState isFiltered={false} />
          ) : !hasFilteredResults ? (
            <ReviewEmptyState isFiltered={isFiltered || isStatusFiltered} />
          ) : (
            <div className="space-y-10">
              {groupedSections.map((section) => (
                <ReviewCategorySection
                  key={section.key}
                  label={section.label}
                  items={section.items}
                  currentStatusFilter={status}
                />
              ))}
            </div>
          )}
        </>
      )}
    </AppShell>
  )
}
