import { Skeleton } from '@/components/ui/skeleton'

export const QuestionCardSkeleton = () => {
  return (
    <li
      className="rounded-lg border border-gray-200 bg-white p-5"
      aria-hidden="true"
    >
      <div className="flex items-center gap-3">
        <Skeleton className="h-7 w-7 rounded-full" />
        <Skeleton className="h-5 w-16" />
      </div>
      <Skeleton className="mt-3 h-4 w-full" />
      <Skeleton className="mt-2 h-4 w-3/4" />
    </li>
  )
}
