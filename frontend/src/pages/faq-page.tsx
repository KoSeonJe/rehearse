import { Link } from 'react-router-dom'
import { ContentPageShell } from '@/components/content/content-page-shell'

const TITLE = '리허설 자주 묻는 질문 (FAQ) | AI 모의면접 플랫폼'
const DESCRIPTION =
  '리허설 AI 모의면접 사용법, 영상·이력서 데이터 처리, AI 피드백 정확도, 비용 등 자주 묻는 질문 모음.'
const PATH = '/faq'

const FAQS: { q: string; a: string }[] = [
  {
    q: '리허설은 어떤 서비스인가요?',
    a: 'AI 면접관이 이력서 기반 맞춤 질문을 던지고, 답변을 영상으로 녹화·분석해 타임스탬프 단위로 피드백을 주는 개발자용 AI 모의면접 플랫폼입니다.',
  },
  {
    q: 'AI가 정말 면접관처럼 질문하나요?',
    a: '네. 이력서를 분석해 본인 프로젝트에서 파생된 후속 질문(follow-up)을 자동 생성합니다. 답변에 따라 다음 질문이 달라지므로 정형화된 챗봇과 다릅니다.',
  },
  {
    q: '제 영상과 이력서는 어떻게 처리되나요?',
    a: '영상은 본인 계정에서만 조회 가능하며, 이력서는 AI 질문 생성 외 용도로 사용되지 않습니다. 자세한 내용은 개인정보 처리방침을 참고하세요.',
  },
  {
    q: '비용은 얼마인가요?',
    a: '현재 베타 기간으로 무료 제공 중입니다. 정식 출시 시 요금제가 도입될 예정이며, 베타 사용자에게는 별도 안내드립니다.',
  },
  {
    q: '얼마나 많이 연습해야 효과가 있나요?',
    a: '실제 면접 4주 전부터 주 2~3회, 한 번에 30~45분이 권장 패턴입니다. 영상 회고가 핵심이므로 횟수보다 본인 영상을 자주 돌려보는 것이 더 중요합니다.',
  },
  {
    q: 'AI 점수가 실제 합격을 보장하나요?',
    a: '보장하지 않습니다. AI 평가는 일관된 기준선으로 약점을 발견하는 도구이며, 최종 결정은 면접관의 주관과 컬처 핏이 크게 좌우합니다.',
  },
  {
    q: '어떤 직군을 지원하나요?',
    a: '백엔드, 프론트엔드, CS 기초 면접을 지원합니다. 모바일·데이터·DevOps 등은 로드맵에 있으며 베타 피드백을 반영해 확장합니다.',
  },
  {
    q: '면접 도중 끊겼는데 이어할 수 있나요?',
    a: '진행 중인 세션은 임시 저장됩니다. 다만 안정성을 위해 한 세션을 30분 내 마무리하시길 권장합니다.',
  },
  {
    q: '비언어 분석은 정확한가요?',
    a: '시선·표정·목소리 같은 비언어 시그널은 절대 평가가 아닌 상대 변화 추적용으로 활용하시는 것이 적절합니다. 같은 본인의 다른 회차와 비교하는 데 가장 효과적입니다.',
  },
  {
    q: '피드백은 누가 작성하나요?',
    a: '답변 품질·구조 피드백은 Claude(LLM)가, 비언어 분석은 영상·음성 처리 모델이 생성합니다. 둘 다 사람이 아닌 자동 분석 결과입니다.',
  },
]

const faqJsonLd = {
  '@context': 'https://schema.org',
  '@type': 'FAQPage',
  mainEntity: FAQS.map((f) => ({
    '@type': 'Question',
    name: f.q,
    acceptedAnswer: { '@type': 'Answer', text: f.a },
  })),
}

export const FaqPage = () => (
  <ContentPageShell title={TITLE} description={DESCRIPTION} canonicalPath={PATH} jsonLd={faqJsonLd}>
    <h1 className="text-3xl font-extrabold tracking-tight text-text-primary mb-6">자주 묻는 질문</h1>
    <p className="text-base text-text-secondary leading-relaxed mb-10">
      리허설을 사용하면서 자주 받는 질문을 모았습니다. 추가 문의는 홈 화면 하단의 피드백 채널을 이용해 주세요.
    </p>

    <div className="space-y-6">
      {FAQS.map((f) => (
        <div key={f.q} className="border-b border-border/40 pb-5">
          <h2 className="text-base font-bold text-text-primary mb-2">Q. {f.q}</h2>
          <p className="text-sm text-text-secondary leading-relaxed">{f.a}</p>
        </div>
      ))}
    </div>

    <div className="mt-12 p-6 rounded-xl bg-muted border border-border/50">
      <h3 className="text-lg font-bold mb-2">바로 시작해 보기</h3>
      <p className="text-sm text-text-secondary mb-4">베타 기간 무료 — 이력서 업로드 후 30분이면 한 사이클이 끝납니다.</p>
      <Link to="/" className="inline-block px-4 py-2 bg-primary text-primary-foreground rounded-md text-sm font-medium hover:bg-primary/90">
        리허설 시작하기
      </Link>
    </div>
  </ContentPageShell>
)
