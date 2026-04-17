import { useState } from 'react'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { Spinner } from '@/components/ui/spinner'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { useAdminFeedbacks } from '@/hooks/use-service-feedback'
import type { FeedbackSource } from '@/types/service-feedback'

const PAGE_SIZE = 20

const formatDate = (isoString: string): string => {
  const date = new Date(isoString)
  const yyyy = date.getFullYear()
  const mm = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const min = String(date.getMinutes()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd} ${hh}:${min}`
}

const truncate = (text: string, maxLength: number): string => {
  if (text.length <= maxLength) return text
  return text.slice(0, maxLength) + '...'
}

const renderStars = (rating: number | null): string => {
  if (rating === null) return '-'
  return '★'.repeat(rating) + '☆'.repeat(5 - rating)
}

const SourceBadge = ({ source }: { source: FeedbackSource }) => {
  if (source === 'AUTO_POPUP') {
    return (
      <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-700">
        자동
      </span>
    )
  }
  return (
    <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-700">
      자발적
    </span>
  )
}

export const AdminFeedbacksPage = () => {
  const [page, setPage] = useState(0)

  const { data, isLoading } = useAdminFeedbacks(page, PAGE_SIZE)

  const feedbackList = data?.data?.content ?? []
  const totalPages = data?.data?.totalPages ?? 0
  const totalElements = data?.data?.totalElements ?? 0

  return (
    <div className="min-h-screen bg-background text-text-primary">
      <div className="max-w-5xl mx-auto">
        <main className="px-5 py-8 lg:px-10 lg:py-10">
          <div className="mb-6">
            <h1 className="text-xl font-extrabold text-text-primary tracking-tight">피드백 관리</h1>
            {!isLoading && (
              <p className="mt-1 text-sm text-text-secondary">
                총 {totalElements}개의 피드백
              </p>
            )}
          </div>

          {/* 데스크탑: 테이블 뷰 */}
          <div className="hidden lg:block">
            {isLoading ? (
              <div className="flex items-center justify-center py-20">
                <Spinner className="h-8 w-8" />
              </div>
            ) : feedbackList.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-20 text-text-secondary">
                <p className="text-base font-medium">아직 피드백이 없습니다</p>
              </div>
            ) : (
              <Card className="bg-surface border border-border shadow-sm overflow-hidden">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-border bg-background">
                      <th className="text-left px-5 py-3 font-semibold text-text-secondary">작성자</th>
                      <th className="text-left px-5 py-3 font-semibold text-text-secondary">내용</th>
                      <th className="text-left px-5 py-3 font-semibold text-text-secondary">별점</th>
                      <th className="text-left px-5 py-3 font-semibold text-text-secondary">출처</th>
                      <th className="text-left px-5 py-3 font-semibold text-text-secondary">작성일</th>
                    </tr>
                  </thead>
                  <tbody>
                    {feedbackList.map((item) => (
                      <tr key={item.id} className="border-b border-border/50 last:border-0 hover:bg-background/50 transition-colors">
                        <td className="px-5 py-3.5 text-text-primary font-medium">{item.userName}</td>
                        <td className="px-5 py-3.5 text-text-secondary max-w-xs">{truncate(item.content, 50)}</td>
                        <td className="px-5 py-3.5 text-amber-400 font-medium">{renderStars(item.rating)}</td>
                        <td className="px-5 py-3.5">
                          <SourceBadge source={item.source} />
                        </td>
                        <td className="px-5 py-3.5 text-text-tertiary text-xs">{formatDate(item.createdAt)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </Card>
            )}
          </div>

          {/* 모바일: 카드 뷰 */}
          <div className="lg:hidden">
            {isLoading ? (
              <div className="flex items-center justify-center py-20">
                <Spinner className="h-8 w-8" />
              </div>
            ) : feedbackList.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-20 text-text-secondary">
                <p className="text-base font-medium">아직 피드백이 없습니다</p>
              </div>
            ) : (
              <div className="flex flex-col gap-3">
                {feedbackList.map((item) => (
                  <Card key={item.id} className="bg-surface border border-border shadow-sm p-4">
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-sm font-semibold text-text-primary">{item.userName}</span>
                      <SourceBadge source={item.source} />
                    </div>
                    <p className="text-sm text-text-secondary mb-3">{truncate(item.content, 50)}</p>
                    <div className="flex items-center justify-between">
                      <span className="text-amber-400 text-sm">{renderStars(item.rating)}</span>
                      <span className="text-xs text-text-tertiary">{formatDate(item.createdAt)}</span>
                    </div>
                  </Card>
                ))}
              </div>
            )}
          </div>

          {/* 페이지네이션 */}
          {!isLoading && totalPages > 1 && (
            <div className="flex items-center justify-center gap-3 mt-6">
              <Button
                variant="secondary"
                size="sm"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="rounded-xl"
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
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="rounded-xl"
              >
                다음
                <ChevronRight size={16} />
              </Button>
            </div>
          )}
        </main>
      </div>
    </div>
  )
}
