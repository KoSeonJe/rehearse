import { Card } from '@/components/ui/card'
import { useFadeInOnScroll } from '@/hooks/use-fade-in-on-scroll'

interface CheckItem {
  title: string
  description: string
}

const CHECK_ITEMS: CheckItem[] = [
  {
    title: '카메라 위치 조정',
    description: '눈높이에 맞추고, 어깨부터 머리가 보이게 프레이밍하세요',
  },
  {
    title: '마이크 테스트',
    description: '헤드셋이나 외장 마이크를 권장합니다. 사전에 음량을 확인하세요',
  },
  {
    title: '조용하고 밝은 환경',
    description: '배경 소음이 적고 얼굴에 조명이 고르게 비치는 곳이 좋아요',
  },
  {
    title: '이력서 준비',
    description: 'PDF 형식의 최신 이력서를 준비하세요. AI가 내용을 분석합니다',
  },
  {
    title: 'Chrome 브라우저 사용',
    description: '데스크톱 Chrome에 최적화되어 있습니다. 카메라·마이크 권한을 허용해주세요',
  },
  {
    title: '불필요한 탭·알림 끄기',
    description: '다른 탭과 알림을 닫으면 녹화 품질이 올라가고 집중도도 높아집니다',
  },
]

const CheckIcon = () => (
  <svg
    width="16"
    height="16"
    viewBox="0 0 16 16"
    fill="none"
    aria-hidden="true"
  >
    <path
      d="M13.5 4.5L6.5 11.5L2.5 7.5"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
  </svg>
)

export const BeforeYouStartSection = () => {
  const { ref, style } = useFadeInOnScroll<HTMLElement>()

  return (
    <section
      ref={ref}
      style={style}
      className="bg-background py-32"
    >
      <div className="max-w-4xl mx-auto px-5 md:px-8">
        <div className="text-center mb-16">
          <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-4">
            PREPARATION
          </p>
          <h2 className="text-3xl md:text-4xl font-extrabold tracking-tighter text-text-primary">
            시작 전, 이것만 준비하세요
          </h2>
          <p className="text-lg text-text-secondary mt-4">
            더 정확한 분석과 피드백을 위한 간단한 체크리스트
          </p>
        </div>

        <div
          className="grid grid-cols-1 md:grid-cols-2 gap-4"
          role="list"
          aria-label="면접 준비 체크리스트"
        >
          {CHECK_ITEMS.map((item) => (
            <Card
              key={item.title}
              className="bg-surface border border-border p-6 shadow-sm"
              role="listitem"
            >
              <div className="flex items-start gap-4">
                <div
                  className="h-8 w-8 shrink-0 rounded-xl bg-secondary flex items-center justify-center text-text-primary"
                  aria-hidden="true"
                >
                  <CheckIcon />
                </div>
                <div>
                  <p className="text-base font-bold text-text-primary">{item.title}</p>
                  <p className="text-sm text-text-secondary mt-1">{item.description}</p>
                </div>
              </div>
            </Card>
          ))}
        </div>
      </div>
    </section>
  )
}
