import { Link } from 'react-router-dom'
import { ContentPageShell } from '@/components/content/content-page-shell'

const TITLE = 'AI 모의면접이란? 효과·장점·활용법 완벽 가이드 | 리허설'
const DESCRIPTION =
  'AI 모의면접의 정의, 사람 모의면접과의 차이, 5가지 장점과 효과적인 활용법까지 — 개발자 면접 준비를 위한 AI 모의면접 완전 가이드.'
const PATH = '/guide/ai-mock-interview'

const SITE_URL = import.meta.env.VITE_SITE_URL || 'https://rehearse.co.kr'

const articleJsonLd = {
  '@context': 'https://schema.org',
  '@type': 'Article',
  headline: 'AI 모의면접이란? 효과·장점·활용법 완벽 가이드',
  description: DESCRIPTION,
  inLanguage: 'ko-KR',
  author: { '@type': 'Organization', name: '리허설' },
  publisher: { '@type': 'Organization', name: '리허설' },
  mainEntityOfPage: `${SITE_URL}${PATH}`,
}

export const AiMockInterviewGuidePage = () => (
  <ContentPageShell
    title={TITLE}
    description={DESCRIPTION}
    canonicalPath={PATH}
    jsonLd={articleJsonLd}
  >
    <article className="prose prose-neutral max-w-none">
      <h1 className="text-3xl font-extrabold tracking-tight text-text-primary mb-6">
        AI 모의면접이란? 효과·장점·활용법 완벽 가이드
      </h1>
      <p className="text-base text-text-secondary leading-relaxed mb-8">
        AI 모의면접은 사람 면접관 없이도 실전과 유사한 면접 환경을 재현할 수 있는 가장 효율적인 학습 방식입니다.
        이 글은 AI 모의면접이 무엇인지, 사람 모의면접과 어떻게 다른지, 그리고 개발자 취업·이직 준비에 어떻게 활용해야 하는지를 정리했습니다.
      </p>

      <Section title="1. AI 모의면접이란">
        <p>
          AI 모의면접은 AI 면접관이 지원자의 이력서를 분석해 맞춤형 질문을 던지고, 답변을 영상·음성으로 녹화한 뒤 자동으로 피드백을 제공하는 면접 연습 방식입니다.
          사람 면접관과의 모의면접이 어렵거나 비용 부담이 큰 상황에서, 24시간 언제든 실전 환경을 재현할 수 있다는 점이 가장 큰 가치입니다.
        </p>
        <p>
          최근의 AI 모의면접 서비스는 단순히 질문을 던지는 챗봇을 넘어, 답변의 구조·키워드·비언어 시그널까지 분석합니다.
          개발자 채용처럼 깊이 있는 기술 답변과 커뮤니케이션이 동시에 평가되는 영역에서 특히 효용이 큽니다.
        </p>
      </Section>

      <Section title="2. 사람 모의면접과의 차이">
        <List
          items={[
            ['시간·비용', '사람 모의면접은 일정 조율과 비용이 부담. AI는 클릭 한 번으로 시작.'],
            ['객관성', '사람은 첫인상에 영향을 받음. AI는 동일한 기준(답변 구조, 키워드 일치도, 비언어 시그널)으로 평가.'],
            ['반복성', '같은 질문을 다시 받기 어려움. AI는 무제한 반복.'],
            ['데이터 누적', '사람의 코멘트는 휘발됨. AI는 영상·자막·점수가 기록되어 변화 추이 비교 가능.'],
          ]}
        />
      </Section>

      <Section title="3. AI 모의면접의 5가지 장점">
        <List
          items={[
            ['이력서 기반 맞춤 질문', '본인의 프로젝트·경력에서 파생된 구체적인 질문이 나옵니다. "리액트 경험"이 아니라 "당신이 한 X 프로젝트의 상태관리 결정 이유" 같은 디테일.'],
            ['타임스탬프 피드백', '"8분 14초의 답변에서 결론이 모호합니다"처럼 시점 단위로 코멘트를 받습니다.'],
            ['비언어 분석', '시선 처리·표정·목소리 톤 같은, 본인은 모르는 디테일을 측정.'],
            ['반복 학습 루프', '같은 질문을 다른 방식으로 답해 보며 개선 사이클을 만들 수 있습니다.'],
            ['자기 점검 데이터', '영상·답변 텍스트·점수 변화를 누적해 약점을 시각화.'],
          ]}
        />
      </Section>

      <Section title="4. 효과적으로 활용하는 법">
        <List
          items={[
            ['첫 회차는 평소 실력으로', '베이스라인을 정확히 만들고, 거기서 드러난 약점부터 공략하는 것이 효율적입니다.'],
            ['이력서를 자세히 작성', 'AI가 좋은 질문을 만들려면 입력의 디테일이 절대적으로 중요합니다.'],
            ['녹화 영상을 반드시 본다', '답변 내용보다 본인의 말투·시선·머뭇거림을 보는 것이 종종 더 충격적이고, 더 빠르게 개선됩니다.'],
            ['주 2~3회로 분산', '몰아서 하면 피로해서 학습 효과가 떨어집니다.'],
          ]}
        />
      </Section>

      <Section title="5. 자주 묻는 질문">
        <Faq
          q="AI 면접 점수가 실제 면접 합격을 보장하나요?"
          a="보장하지 않습니다. AI 평가는 일관된 기준선을 제공하는 도구이며, 최종 결정은 면접관의 주관과 조직 적합도가 크게 좌우합니다. AI는 '내가 어디서 자주 막히는지', '어떤 단어를 반복하는지'를 발견하는 거울로 활용하는 게 가장 효과적입니다."
        />
        <Faq
          q="얼마나 자주 연습해야 하나요?"
          a="실제 면접 일정 4주 전부터 주 2~3회, 한 번에 30~45분이 적당합니다. 기본기 다지기는 그보다 일찍 시작해도 좋습니다."
        />
        <Faq
          q="회사·직무에 따라 질문 스타일이 다른가요?"
          a="네. 리허설은 면접 타입(백엔드/프론트엔드/CS)을 선택할 수 있고, 이력서 기반으로 질문을 생성하므로 동일한 직무여도 사람마다 다른 질문이 나옵니다."
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

const Faq = ({ q, a }: { q: string; a: string }) => (
  <div className="mb-4">
    <p className="font-semibold text-text-primary mb-1">Q. {q}</p>
    <p className="text-text-secondary">{a}</p>
  </div>
)

const Cta = () => (
  <div className="mt-12 p-6 rounded-xl bg-gray-50 border border-border/50">
    <h3 className="text-lg font-bold mb-2">리허설로 AI 모의면접 시작하기</h3>
    <p className="text-sm text-text-secondary mb-4">
      이력서 업로드 → AI 면접 진행 → 영상 + 타임스탬프 피드백 — 30분이면 한 사이클을 끝낼 수 있습니다.
    </p>
    <Link to="/" className="inline-block px-4 py-2 bg-accent text-white rounded-md text-sm font-medium hover:opacity-90">
      바로 시작하기
    </Link>
  </div>
)
