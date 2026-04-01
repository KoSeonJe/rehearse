import { useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { Sparkles, ArrowRight } from 'lucide-react'

const MOTIVATIONS = [
  { title: '오늘도 면접 연습 한 판?', subtitle: '작은 반복이 합격을 만듭니다.' },
  { title: '준비된 사람이 기회를 잡는다', subtitle: '지금 바로 한 세트 돌려보세요.' },
  { title: '어제보다 나은 답변을 만들어보세요', subtitle: '연습은 배신하지 않습니다.' },
  { title: '면접관 앞에서 당황하지 않으려면', subtitle: '지금 연습이 최고의 투자입니다.' },
  { title: '합격까지 남은 건 연습뿐', subtitle: '오늘 하나만 더 해보세요.' },
]

export const NewInterviewBanner = () => {
  const navigate = useNavigate()

  const motivation = useMemo(() => {
    const dayIndex = new Date().getDate() % MOTIVATIONS.length
    return MOTIVATIONS[dayIndex]
  }, [])

  return (
    <div
      onClick={() => navigate('/interview/setup')}
      className="group rounded-card border border-accent/20 bg-gradient-to-r from-accent/5 via-transparent to-accent/10 p-6 lg:p-8 cursor-pointer hover:border-accent/40 hover:shadow-toss transition-all duration-300"
    >
      <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
        <div className="flex items-start gap-4">
          <div className="flex-shrink-0 mt-0.5 w-10 h-10 rounded-xl bg-accent/10 flex items-center justify-center">
            <Sparkles size={20} className="text-accent" />
          </div>
          <div>
            <h3 className="text-base font-extrabold text-text-primary tracking-tight">
              {motivation.title}
            </h3>
            <p className="mt-1 text-sm text-text-secondary">
              {motivation.subtitle}
            </p>
          </div>
        </div>

        <button className="flex items-center gap-2 px-5 py-2.5 rounded-button bg-accent text-white text-sm font-bold hover:bg-accent-hover active:scale-95 transition-all duration-200 cursor-pointer whitespace-nowrap self-start lg:self-center group-hover:gap-3">
          면접 시작하기
          <ArrowRight size={16} className="transition-transform duration-200 group-hover:translate-x-0.5" />
        </button>
      </div>
    </div>
  )
}
