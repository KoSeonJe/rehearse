import { Link } from 'react-router-dom'
import { ContentPageShell } from '@/components/content/content-page-shell'

const TITLE = '개발자 면접 준비 완벽 가이드 — 신입·주니어용 단계별 로드맵 | 리허설'
const DESCRIPTION =
  '개발자 면접 단계별 구조, 준비 체크리스트, 자주 묻는 기술 질문 카테고리, AI 모의면접 활용까지 — 신입·주니어 개발자를 위한 면접 준비 완전 가이드.'
const PATH = '/guide/developer-interview-prep'

const SITE_URL = import.meta.env.VITE_SITE_URL || 'https://rehearse.co.kr'

const articleJsonLd = {
  '@context': 'https://schema.org',
  '@type': 'Article',
  headline: '개발자 면접 준비 완벽 가이드 — 신입·주니어용 단계별 로드맵',
  description: DESCRIPTION,
  inLanguage: 'ko-KR',
  author: { '@type': 'Organization', name: '리허설' },
  publisher: { '@type': 'Organization', name: '리허설' },
  mainEntityOfPage: `${SITE_URL}${PATH}`,
}

export const DeveloperInterviewPrepGuidePage = () => (
  <ContentPageShell
    title={TITLE}
    description={DESCRIPTION}
    canonicalPath={PATH}
    jsonLd={articleJsonLd}
  >
    <article className="prose prose-neutral max-w-none">
      <h1 className="text-3xl font-extrabold tracking-tight text-text-primary mb-6">
        개발자 면접 준비 완벽 가이드 — 신입·주니어용 단계별 로드맵
      </h1>
      <p className="text-base text-text-secondary leading-relaxed mb-8">
        개발자 면접은 단계마다 평가 포인트가 다릅니다. 이 글은 서류 통과 이후의 기술 면접·인성 면접·임원 면접까지 단계별 구조와 준비 방법을, 신입·주니어 관점으로 정리했습니다.
      </p>

      <Section title="1. 개발자 면접의 단계 이해">
        <p>대부분의 개발자 채용 프로세스는 다음 단계로 진행됩니다:</p>
        <List
          items={[
            ['1차: 코딩 테스트 / 과제', '알고리즘 또는 미니 프로젝트. 과제 전형이 늘어나는 추세.'],
            ['2차: 기술 면접', '이력서 기반 깊이 면접 + CS 기초 + 시스템 설계 일부.'],
            ['3차: 컬처 핏 / 인성 면접', '협업 경험·갈등 해결 사례·동기 중심.'],
            ['4차: 임원 / 처우 면접', '회사 비전 핏, 처우 협상.'],
          ]}
        />
      </Section>

      <Section title="2. 단계별 준비 체크리스트">
        <Sub title="기술 면접">
          <List
            items={[
              ['이력서 디테일 준비', '프로젝트마다 "왜 이 기술을 골랐나", "어떤 트레이드오프를 봤나"를 1분 분량으로 답할 수 있어야 함.'],
              ['CS 기초 핵심', '자료구조/알고리즘, 운영체제, 네트워크, 데이터베이스 — 자주 나오는 30개 토픽.'],
              ['깊이 질문 대비', '"왜 그렇게 됐나요?"를 3단계 깊이까지 답할 수 있어야.'],
            ]}
          />
        </Sub>
        <Sub title="인성·컬처 면접">
          <List
            items={[
              ['STAR 구조', 'Situation–Task–Action–Result로 사례를 1분 30초 안에 말하는 연습.'],
              ['협업 사례 3개', '갈등·실수·성공 각 1개씩 준비.'],
              ['지원 동기', '회사 product를 직접 써본 경험으로 답하면 강함.'],
            ]}
          />
        </Sub>
      </Section>

      <Section title="3. 자주 묻는 기술 질문 카테고리">
        <List
          items={[
            ['언어 / 런타임', 'JS 이벤트 루프, JVM 메모리, Python GIL 등.'],
            ['프레임워크', 'React 렌더링, Spring 트랜잭션, Django ORM N+1 등.'],
            ['데이터베이스', '인덱스, 트랜잭션 격리수준, JOIN 비용.'],
            ['네트워크 / 시스템', 'HTTP 메서드, TCP 핸드셰이크, 캐시 계층.'],
            ['설계 / 아키텍처', '간단한 시스템 설계, 동시성, 장애 시나리오.'],
          ]}
        />
      </Section>

      <Section title="4. 실전 모의면접의 중요성">
        <p>
          기술 지식이 충분해도, 실전에서는 "말하는 순서·결론 위치·비언어 시그널"이 합격을 가릅니다.
          그래서 한 번이라도 본인이 답하는 모습을 영상으로 보는 경험이 결정적입니다.
          AI 모의면접은 이 영상·피드백 사이클을 가장 빠르게 만들 수 있는 도구입니다.
        </p>
        <p className="text-sm">
          관련: <Link to="/guide/ai-mock-interview" className="underline">AI 모의면접 완벽 가이드</Link>
        </p>
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

const Sub = ({ title, children }: { title: string; children: React.ReactNode }) => (
  <div className="mb-4">
    <h3 className="text-base font-semibold text-text-primary mb-2">{title}</h3>
    {children}
  </div>
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
    <h3 className="text-lg font-bold mb-2">리허설로 모의면접 시작하기</h3>
    <p className="text-sm text-text-secondary mb-4">
      백엔드/프론트엔드/CS 면접 타입을 골라 이력서 기반으로 맞춤 질문을 받아 보세요.
    </p>
    <Link
      to="/"
      className="inline-block px-4 py-2 bg-primary text-primary-foreground rounded-md text-sm font-medium hover:bg-primary/90"
    >
      바로 시작하기
    </Link>
  </div>
)
