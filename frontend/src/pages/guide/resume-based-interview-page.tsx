import { Link } from 'react-router-dom'
import { ContentPageShell } from '@/components/content/content-page-shell'

const TITLE = '이력서 기반 맞춤 면접 — 좋은 이력서가 좋은 면접을 만든다 | 리허설'
const DESCRIPTION =
  '이력서 기반 맞춤 면접이 무엇이고, 어떤 항목이 자주 파고들어지는지, AI 모의면접에 좋은 입력을 주는 방법까지 — 이력서를 면접 준비의 무기로 만드는 실용 가이드.'
const PATH = '/guide/resume-based-interview'

const SITE_URL = import.meta.env.VITE_SITE_URL || 'https://rehearse.co.kr'

const articleJsonLd = {
  '@context': 'https://schema.org',
  '@type': 'Article',
  headline: '이력서 기반 맞춤 면접 — 좋은 이력서가 좋은 면접을 만든다',
  description: DESCRIPTION,
  inLanguage: 'ko-KR',
  author: { '@type': 'Organization', name: '리허설' },
  publisher: { '@type': 'Organization', name: '리허설' },
  mainEntityOfPage: `${SITE_URL}${PATH}`,
}

export const ResumeBasedInterviewGuidePage = () => (
  <ContentPageShell
    title={TITLE}
    description={DESCRIPTION}
    canonicalPath={PATH}
    jsonLd={articleJsonLd}
  >
    <article className="prose prose-neutral max-w-none">
      <h1 className="text-3xl font-extrabold tracking-tight text-text-primary mb-6">
        이력서 기반 맞춤 면접 — 좋은 이력서가 좋은 면접을 만든다
      </h1>
      <p className="text-base text-text-secondary leading-relaxed mb-8">
        면접에서 "지원자에 대한 질문"의 90%는 이력서에서 출발합니다.
        이력서가 모호하면 모호한 질문이, 이력서가 구체적이면 깊은 질문이 들어옵니다.
        이 글은 이력서 기반 면접의 작동 원리와, AI 모의면접에 좋은 입력을 주는 방법을 정리합니다.
      </p>

      <Section title="1. 이력서 기반 면접이란">
        <p>
          이력서 기반 면접은 면접관(또는 AI)이 지원자의 이력서·포트폴리오·프로젝트 경험을 읽고, 거기서 파생된 질문을 던지는 방식입니다.
          모범답안이 정해진 일반 질문보다, 지원자의 실제 경험을 검증할 수 있어서 최근 채용에서 비중이 커지고 있습니다.
        </p>
      </Section>

      <Section title="2. 자주 파고드는 이력서 항목 5가지">
        <List
          items={[
            ['프로젝트의 "왜"', '왜 이 기술을 선택했는지, 다른 대안은 검토했는지.'],
            ['트레이드오프', '편의성과 성능, 속도와 품질 사이에서 어떤 결정을 했는지.'],
            ['숫자와 임팩트', '"성능 개선"이 아니라 "응답시간 800ms → 200ms" 같은 정량적 임팩트.'],
            ['실패와 회복', '잘 안 됐던 경험과 거기서 무엇을 배웠는지.'],
            ['협업 방식', '코드 리뷰·커뮤니케이션·갈등 해결 사례.'],
          ]}
        />
      </Section>

      <Section title="3. 이력서를 면접용으로 다듬는 5가지 원칙">
        <List
          items={[
            ['모든 문장에 동사', '"OO 담당" 보다 "OO를 설계·구현·릴리즈" 처럼 행동 중심.'],
            ['숫자로 말하기', '비율·시간·사용자 수 등 정량 지표를 한 문장에 1개 이상.'],
            ['사용자 언어', '내부 용어 대신 면접관이 즉시 이해할 수 있는 표현.'],
            ['결정 근거 포함', '"OO 도입"으로 끝나지 말고 "OO를 도입한 이유" 한 줄 추가.'],
            ['최신 정보 우선', '최근 6~12개월 경험을 상단 배치.'],
          ]}
        />
      </Section>

      <Section title="4. AI 모의면접에 좋은 입력 주는 법">
        <p>
          리허설 같은 AI 모의면접은 입력의 품질이 곧 출력 질문의 품질입니다.
          다음을 의식해서 이력서를 작성하면 훨씬 깊이 있는 질문을 받습니다.
        </p>
        <List
          items={[
            ['프로젝트당 3~5문장', '한 줄 요약은 일반적인 질문만 유발. 디테일이 들어가야 깊은 질문이 생성됨.'],
            ['핵심 기술 스택 명시', '"백엔드"가 아니라 "Spring Boot 3 + JPA + MySQL"처럼 구체적으로.'],
            ['역할 명확화', '팀 규모·본인 비중·담당 영역을 분리.'],
          ]}
        />
      </Section>

      <Cta />
    </article>
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

const Cta = () => (
  <div className="mt-12 p-6 rounded-xl bg-muted border border-border/50">
    <h3 className="text-lg font-bold mb-2">이력서를 업로드하고 맞춤 면접 받아보기</h3>
    <p className="text-sm text-text-secondary mb-4">
      이력서를 한 번 업로드하면 AI가 본인 경험에 맞춘 질문을 즉시 만들어 줍니다.
    </p>
    <Link
      to="/"
      className="inline-block px-4 py-2 bg-primary text-primary-foreground rounded-md text-sm font-medium hover:bg-primary/90"
    >
      바로 시작하기
    </Link>
  </div>
)
