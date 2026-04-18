import { Link } from 'react-router-dom'
import { ContentPageShell } from '@/components/content/content-page-shell'

const TITLE = '리허설 소개 — AI 모의면접으로 개발자 합격을 돕습니다'
const DESCRIPTION =
  '리허설은 개발자 면접 준비를 위한 AI 모의면접 플랫폼입니다. 우리가 만든 이유, 차별점, 앞으로의 방향을 소개합니다.'
const PATH = '/about'

const SITE_URL = import.meta.env.VITE_SITE_URL || 'https://rehearse.co.kr'

const aboutJsonLd = {
  '@context': 'https://schema.org',
  '@type': 'AboutPage',
  name: TITLE,
  description: DESCRIPTION,
  inLanguage: 'ko-KR',
  url: `${SITE_URL}${PATH}`,
  mainEntity: {
    '@type': 'Organization',
    name: '리허설',
    alternateName: 'Rehearse',
    url: `${SITE_URL}/`,
    description: 'AI 면접관과 진행하는 개발자 모의면접 플랫폼',
  },
}

export const AboutPage = () => (
  <ContentPageShell title={TITLE} description={DESCRIPTION} canonicalPath={PATH} jsonLd={aboutJsonLd}>
    {/* 히어로 배너 — editorial 톤의 over-line + 대형 headline + sub-copy */}
    <header className="mb-14">
      <p className="text-xs font-semibold uppercase tracking-[0.1em] text-brand mb-4">
        ABOUT · REHEARSE
      </p>
      <h1 className="text-4xl md:text-5xl font-extrabold tracking-tight text-text-primary leading-[1.15] mb-5">
        AI 모의면접으로<br />개발자 합격을 돕습니다
      </h1>
      <p className="text-lg text-text-secondary leading-relaxed max-w-2xl">
        혼자 거울 앞에서 연습하던 면접을, 24시간 이력서 기반 AI 면접관과 영상·비언어 피드백으로 대체합니다.
      </p>
    </header>

    {/* mini 타임라인 — "왜 만들었는지" 3단 */}
    <ol className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-14 pb-14 border-b border-border/40">
      {[
        { step: '01', title: '문제', desc: '실전 모의면접은 비싸고, 혼자 연습은 객관성이 없다' },
        { step: '02', title: '관찰', desc: '개발자 면접은 이력서와 프로젝트 깊이에서 결판난다' },
        { step: '03', title: '해결', desc: '이력서 분석 → 맞춤 follow-up → 타임스탬프 피드백' },
      ].map((item) => (
        <li key={item.step} className="flex flex-col gap-2">
          <span className="font-tabular text-xs font-semibold text-brand">
            {item.step}
          </span>
          <h3 className="text-base font-bold text-text-primary">{item.title}</h3>
          <p className="text-sm text-text-secondary leading-relaxed">{item.desc}</p>
        </li>
      ))}
    </ol>

    <Section title="우리가 만든 이유">
      <p>
        개발자 면접 준비는 늘 같은 벽에 부딪힙니다. 사람과의 모의면접은 시간도 비용도 많이 들고, 혼자 거울 앞에서 연습하면 본인의 약점을 발견하기 어렵습니다.
        리허설은 이 격차를 메우기 위해 만들어졌습니다 — AI 면접관이 24시간 이력서 기반 질문을 던지고, 영상·답변·비언어 시그널을 분석해 누구든 빠르게 자기 모습을 객관화할 수 있도록.
      </p>
    </Section>

    <Section title="차별점">
      <List
        items={[
          ['이력서 기반 follow-up', '정형 챗봇이 아니라, 본인 프로젝트에서 파생된 깊은 질문을 만듭니다.'],
          ['타임스탬프 피드백', '영상의 정확한 시점에 코멘트가 고정 — 점수보다 구체적이고 즉시 행동 가능.'],
          ['비언어 분석', '시선·표정·목소리 톤 변화를 자동 측정.'],
          ['반복 사이클', '같은 질문에 다른 답변을 시도하며 개선 곡선을 만들 수 있습니다.'],
        ]}
      />
    </Section>

    <Section title="누구를 위해 만들었나요">
      <List
        items={[
          ['취업 준비 개발자', '신입·주니어 면접에서 자기 프로젝트를 깊이 있게 설명해야 하는 분.'],
          ['이직 준비 개발자', '오랜만의 면접에서 감을 다시 잡고 싶은 분.'],
          ['부트캠프 수료생', '실전 면접 경험이 부족해 안전한 환경에서 반복 연습이 필요한 분.'],
        ]}
      />
    </Section>

    <Section title="앞으로의 방향">
      <p>
        리허설은 베타 기간 중에도 사용자 피드백을 가장 빠르게 반영합니다.
        앞으로 면접 직군 확장(모바일·데이터·DevOps), 회사·직무별 질문 풀, 더 정교한 비언어 분석을 차례로 도입할 예정입니다.
      </p>
    </Section>

    <div className="mt-12 p-6 rounded-xl bg-muted border border-border/50">
      <h3 className="text-lg font-bold mb-2">지금 바로 한 사이클 돌려보기</h3>
      <p className="text-sm text-text-secondary mb-4">베타 기간 무료. 이력서 업로드 → 30분이면 첫 영상 + 피드백.</p>
      <Link to="/" className="inline-block px-4 py-2 bg-brand text-brand-foreground rounded-md text-sm font-medium hover:bg-brand-hover">
        리허설 시작하기
      </Link>
    </div>
  </ContentPageShell>
)

const Section = ({ title, children }: { title: string; children: React.ReactNode }) => (
  <section className="mb-10">
    <h2 className="text-xl font-bold text-text-primary mb-3">{title}</h2>
    <div className="text-base text-text-secondary leading-relaxed space-y-3">{children}</div>
  </section>
)

const List = ({ items }: { items: [string, string][] }) => (
  <ul className="space-y-2 list-disc pl-5">
    {items.map(([k, v]) => (
      <li key={k}>
        <strong className="text-text-primary">{k}</strong> — {v}
      </li>
    ))}
  </ul>
)
