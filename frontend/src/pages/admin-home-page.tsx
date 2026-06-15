import { Helmet } from 'react-helmet-async'
import { Link } from 'react-router-dom'
import { ChevronRight, MessageSquareText, Rows3 } from 'lucide-react'
import { Card } from '@/components/ui/card'

const ADMIN_LINKS = [
  {
    title: '서비스 피드백 관리',
    description: '사용자가 남긴 서비스 피드백과 별점, 작성 출처를 확인합니다.',
    href: '/admin/feedbacks',
    icon: MessageSquareText,
  },
  {
    title: '질문 풀 관리',
    description: '면접 질문 풀을 검색하고 운영에 필요한 질문을 추가합니다.',
    href: '/admin/question-pool',
    icon: Rows3,
  },
]

export const AdminHomePage = () => (
  <div className="min-h-screen bg-background text-text-primary">
    <Helmet>
      <title>관리자 메뉴 - 리허설</title>
      <meta name="robots" content="noindex, nofollow" />
    </Helmet>

    <main className="mx-auto max-w-3xl px-5 py-8 lg:px-10 lg:py-10">
      <div className="mb-6">
        <h1 className="text-xl font-extrabold tracking-tight text-text-primary">관리자 메뉴</h1>
        <p className="mt-1 text-sm text-text-secondary">주요 운영 화면으로 이동합니다.</p>
      </div>

      <nav aria-label="관리자 메뉴">
        <div className="flex flex-col gap-3">
          {ADMIN_LINKS.map((item) => {
            const Icon = item.icon

            return (
              <Card
                key={item.href}
                className="overflow-hidden border border-border bg-surface shadow-sm transition-colors hover:bg-muted/60"
              >
                <Link
                  to={item.href}
                  className="flex min-h-24 items-center gap-4 px-4 py-4 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 sm:px-5"
                >
                  <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-lg bg-brand-bg text-brand">
                    <Icon size={20} aria-hidden="true" />
                  </span>
                  <span className="min-w-0 flex-1">
                    <span className="block text-sm font-bold text-text-primary">{item.title}</span>
                    <span className="mt-1 block text-sm leading-relaxed text-text-secondary">
                      {item.description}
                    </span>
                  </span>
                  <ChevronRight className="shrink-0 text-text-tertiary" size={18} aria-hidden="true" />
                </Link>
              </Card>
            )
          })}
        </div>
      </nav>
    </main>
  </div>
)
