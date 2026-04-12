import { ReviewBookmarkCard } from '@/components/review/review-bookmark-card'
import type { ReviewBookmarkListItem, BookmarkStatus } from '@/types/review-bookmark'

interface ReviewCategorySectionProps {
  label: string
  items: ReviewBookmarkListItem[]
  currentStatusFilter: BookmarkStatus
}

export const ReviewCategorySection = ({
  label,
  items,
  currentStatusFilter,
}: ReviewCategorySectionProps) => {
  const sectionId = `section-${label.replace(/[^a-zA-Z0-9]/g, '-')}`

  return (
    <section aria-labelledby={sectionId}>
      <div className="flex items-center gap-2.5 mb-3">
        <h2 id={sectionId} className="text-[15px] font-bold text-text-primary">
          {label}
        </h2>
        <span className="text-[12px] font-semibold text-accent bg-accent-light rounded-full px-2.5 py-0.5">
          {items.length}
        </span>
      </div>
      <div className="space-y-2">
        {items.map((item) => (
          <ReviewBookmarkCard
            key={item.id}
            item={item}
            currentStatusFilter={currentStatusFilter}
          />
        ))}
      </div>
    </section>
  )
}
