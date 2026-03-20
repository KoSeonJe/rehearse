import { useState } from 'react'
import { useFadeInOnScroll } from '@/hooks/use-fade-in-on-scroll'

interface FaqItem {
  question: string
  answer: string
}

const FAQ_ITEMS: FaqItem[] = [
  {
    question: '리허설은 무료인가요?',
    answer: '네, 현재 모든 기능을 무료로 이용할 수 있습니다.',
  },
  {
    question: '회원가입이 필요한가요?',
    answer: '아니요, 별도 회원가입 없이 바로 면접을 시작할 수 있습니다.',
  },
  {
    question: '어떤 직무의 면접을 연습할 수 있나요?',
    answer: '백엔드, 프론트엔드 등 개발 직무를 중심으로 지원하며, 이력서 내용에 맞춰 질문이 생성됩니다.',
  },
  {
    question: '이력서는 저장되나요?',
    answer: '아니요, 업로드한 이력서는 질문 생성에만 사용되며 즉시 삭제됩니다. 별도로 저장하거나 보관하지 않습니다.',
  },
  {
    question: '녹화된 영상은 어디에 저장되나요?',
    answer: '영상은 안전한 클라우드 스토리지에 저장되며, 본인만 열람할 수 있습니다.',
  },
  {
    question: '모바일에서도 사용할 수 있나요?',
    answer: '데스크톱 Chrome 브라우저에 최적화되어 있습니다. 카메라와 마이크 접근이 필요합니다.',
  },
]

const ChevronDownIcon = () => (
  <svg
    width="20"
    height="20"
    viewBox="0 0 20 20"
    fill="none"
    aria-hidden="true"
  >
    <path
      d="M5 7.5L10 12.5L15 7.5"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
  </svg>
)

export const FaqSection = () => {
  const { ref, style } = useFadeInOnScroll<HTMLElement>()
  const [openIndex, setOpenIndex] = useState<number | null>(null)

  const toggle = (i: number) => {
    setOpenIndex((prev) => (prev === i ? null : i))
  }

  return (
    <section
      ref={ref}
      style={style}
      className="bg-surface-warm py-32"
    >
      <div className="max-w-3xl mx-auto px-5 md:px-8">
        <div className="text-center mb-16">
          <p className="label-wide text-accent-teal-dark mb-4">
            FAQ
          </p>
          <h2 className="text-3xl md:text-4xl heading-section text-text-primary">
            자주 묻는 질문
          </h2>
        </div>

        <dl>
          {FAQ_ITEMS.map((item, i) => {
            const isOpen = openIndex === i

            return (
              <div key={item.question} className="border-b border-border">
                <dt>
                  <button
                    type="button"
                    aria-expanded={isOpen}
                    aria-controls={`faq-answer-${i}`}
                    className="w-full py-6 cursor-pointer flex items-center justify-between gap-4 select-none text-left"
                    onClick={() => toggle(i)}
                  >
                    <span className="text-lg font-bold text-text-primary">
                      {item.question}
                    </span>
                    <span
                      className={`shrink-0 text-text-tertiary transition-transform duration-300 ${isOpen ? 'rotate-180' : ''}`}
                    >
                      <ChevronDownIcon />
                    </span>
                  </button>
                </dt>
                <dd
                  id={`faq-answer-${i}`}
                  role="region"
                  aria-hidden={!isOpen}
                  className={`overflow-hidden transition-all duration-300 ${isOpen ? 'max-h-60 pb-6' : 'max-h-0'}`}
                >
                  <p className={`text-base text-text-secondary leading-relaxed ${isOpen ? 'bg-accent-teal/5 rounded-lg px-4 py-3' : ''}`}>
                    {item.answer}
                  </p>
                </dd>
              </div>
            )
          })}
        </dl>
      </div>
    </section>
  )
}
