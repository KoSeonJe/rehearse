import { useMemo } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ApiError } from '@/lib/api-client'
import {
  createBookmark,
  deleteBookmark,
  findBookmarkExists,
  listBookmarks,
  updateBookmarkStatus,
} from '@/api/review-bookmark'
import type {
  BookmarkExistsResponse,
  BookmarkStatus,
  ReviewBookmarkListResponse,
  ReviewBookmarkResponse,
} from '@/types/review-bookmark'

const OPTIMISTIC_BOOKMARK_ID = -1
const LIST_STATUSES: readonly BookmarkStatus[] = ['all', 'in_progress', 'resolved']

// ── Query key factory ──────────────────────────────────────────────────────

export const reviewBookmarkKeys = {
  all: ['review-bookmark'] as const,
  list: (status: BookmarkStatus) => ['review-bookmark', 'list', status] as const,
  existsForInterview: (interviewId: number) =>
    ['review-bookmark', 'exists', interviewId] as const,
}

// ── useBookmarkExistsForInterview ──────────────────────────────────────────

/**
 * Batch-fetches bookmark state for all timestampFeedback IDs in the current
 * interview. Returns a stable Map<timestampFeedbackId, bookmarkId> so each
 * FeedbackCard can look up its own bookmark state in O(1).
 */
export const useBookmarkExistsForInterview = (
  interviewId: number,
  tsfIds: number[],
) => {
  const { data, ...rest } = useQuery({
    queryKey: reviewBookmarkKeys.existsForInterview(interviewId),
    queryFn: () => findBookmarkExists(tsfIds),
    enabled: interviewId > 0 && tsfIds.length > 0,
    staleTime: 1000 * 60 * 5, // 5 minutes
  })

  const bookmarkIdMap = useMemo<Map<number, number>>(() => {
    if (!data) return new Map()
    return new Map(data.items.map((pair) => [pair.timestampFeedbackId, pair.bookmarkId]))
  }, [data])

  return { bookmarkIdMap, ...rest }
}

// ── useCreateBookmark ──────────────────────────────────────────────────────

interface CreateBookmarkVariables {
  timestampFeedbackId: number
  interviewId: number
}

interface CreateBookmarkContext {
  previousData: BookmarkExistsResponse | undefined
}

export const useCreateBookmark = () => {
  const queryClient = useQueryClient()

  return useMutation<
    ReviewBookmarkResponse,
    Error,
    CreateBookmarkVariables,
    CreateBookmarkContext
  >({
    mutationFn: ({ timestampFeedbackId }) => createBookmark(timestampFeedbackId),

    onMutate: async ({ timestampFeedbackId, interviewId }) => {
      const queryKey = reviewBookmarkKeys.existsForInterview(interviewId)
      await queryClient.cancelQueries({ queryKey })

      const previousData = queryClient.getQueryData<BookmarkExistsResponse>(queryKey)

      // Optimistic update: temporarily mark as bookmarked with a sentinel id
      queryClient.setQueryData<BookmarkExistsResponse>(queryKey, (old) => {
        if (!old) return old
        const alreadyExists = old.items.some(
          (p) => p.timestampFeedbackId === timestampFeedbackId,
        )
        if (alreadyExists) return old
        return {
          ...old,
          items: [
            ...old.items,
            { timestampFeedbackId, bookmarkId: OPTIMISTIC_BOOKMARK_ID },
          ],
        }
      })

      return { previousData }
    },

    onError: (error, { interviewId }, context) => {
      // 409 Conflict → server says already bookmarked, invalidate cache to sync real state
      if (error instanceof ApiError && error.status === 409) {
        void queryClient.invalidateQueries({
          queryKey: reviewBookmarkKeys.existsForInterview(interviewId),
        })
        return
      }

      if (context?.previousData !== undefined) {
        queryClient.setQueryData<BookmarkExistsResponse>(
          reviewBookmarkKeys.existsForInterview(interviewId),
          context.previousData,
        )
      }
    },

    onSuccess: (data, { timestampFeedbackId, interviewId }) => {
      // Replace sentinel id with real bookmarkId from server
      queryClient.setQueryData<BookmarkExistsResponse>(
        reviewBookmarkKeys.existsForInterview(interviewId),
        (old) => {
          if (!old) return old
          return {
            ...old,
            items: old.items.map((p) =>
              p.timestampFeedbackId === timestampFeedbackId
                ? { timestampFeedbackId, bookmarkId: data.id }
                : p,
            ),
          }
        },
      )
      void queryClient.invalidateQueries({ queryKey: reviewBookmarkKeys.all })
    },
  })
}

// ── useDeleteBookmark ──────────────────────────────────────────────────────

interface DeleteBookmarkVariables {
  bookmarkId: number
  timestampFeedbackId: number
  /** Provided from feedback page to keep exists-cache in sync; omitted from the global list page. */
  interviewId?: number
}

interface DeleteBookmarkContext {
  previousExists: BookmarkExistsResponse | undefined
  previousLists: Map<BookmarkStatus, ReviewBookmarkListResponse | undefined>
}

export const useDeleteBookmark = () => {
  const queryClient = useQueryClient()

  return useMutation<void, Error, DeleteBookmarkVariables, DeleteBookmarkContext>({
    mutationFn: ({ bookmarkId }) => deleteBookmark(bookmarkId),

    onMutate: async ({ bookmarkId, timestampFeedbackId, interviewId }) => {
      await queryClient.cancelQueries({ queryKey: reviewBookmarkKeys.all })

      // 1. Snapshot + optimistic removal from the per-interview exists cache
      let previousExists: BookmarkExistsResponse | undefined
      if (interviewId !== undefined && interviewId > 0) {
        const existsKey = reviewBookmarkKeys.existsForInterview(interviewId)
        previousExists = queryClient.getQueryData<BookmarkExistsResponse>(existsKey)
        queryClient.setQueryData<BookmarkExistsResponse>(existsKey, (old) => {
          if (!old) return old
          return {
            ...old,
            items: old.items.filter((p) => p.timestampFeedbackId !== timestampFeedbackId),
          }
        })
      }

      // 2. Snapshot + optimistic removal from every status-list cache
      const previousLists = new Map<BookmarkStatus, ReviewBookmarkListResponse | undefined>()
      for (const status of LIST_STATUSES) {
        const listKey = reviewBookmarkKeys.list(status)
        const prev = queryClient.getQueryData<ReviewBookmarkListResponse>(listKey)
        previousLists.set(status, prev)
        if (prev) {
          const nextItems = prev.items.filter((i) => i.id !== bookmarkId)
          if (nextItems.length !== prev.items.length) {
            queryClient.setQueryData<ReviewBookmarkListResponse>(listKey, {
              items: nextItems,
              total: nextItems.length,
            })
          }
        }
      }

      return { previousExists, previousLists }
    },

    onError: (_error, { interviewId }, context) => {
      if (!context) return

      if (interviewId !== undefined && interviewId > 0 && context.previousExists !== undefined) {
        queryClient.setQueryData<BookmarkExistsResponse>(
          reviewBookmarkKeys.existsForInterview(interviewId),
          context.previousExists,
        )
      }

      context.previousLists.forEach((prev, status) => {
        if (prev !== undefined) {
          queryClient.setQueryData<ReviewBookmarkListResponse>(
            reviewBookmarkKeys.list(status),
            prev,
          )
        }
      })
    },

    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: reviewBookmarkKeys.all })
    },
  })
}

// ── useUpdateBookmarkStatus ────────────────────────────────────────────────

interface UpdateBookmarkStatusVariables {
  id: number
  resolved: boolean
  status: BookmarkStatus
}

export const useUpdateBookmarkStatus = () => {
  const queryClient = useQueryClient()

  return useMutation<ReviewBookmarkResponse, Error, UpdateBookmarkStatusVariables>({
    mutationFn: ({ id, resolved }) => updateBookmarkStatus(id, resolved),

    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: reviewBookmarkKeys.all,
      })
    },
  })
}

// ── useReviewBookmarkList ──────────────────────────────────────────────────

export const useReviewBookmarkList = (status: BookmarkStatus) => {
  return useQuery({
    queryKey: reviewBookmarkKeys.list(status),
    queryFn: () => listBookmarks(status),
    staleTime: 1000 * 60 * 2, // 2 minutes
  })
}
