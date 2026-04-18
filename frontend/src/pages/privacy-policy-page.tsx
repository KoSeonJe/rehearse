import { Link } from 'react-router-dom'
import { UtilityBar } from '@/components/layout/utility-bar'

const Section = ({ id, title, children }: { id: string; title: string; children: React.ReactNode }) => (
  <section id={id} className="scroll-mt-8">
    <h2 className="text-base font-bold text-text-primary mb-3">{title}</h2>
    <div className="text-sm text-text-secondary leading-relaxed space-y-2">{children}</div>
  </section>
)

export const PrivacyPolicyPage = () => {
  return (
    <div className="min-h-screen bg-background text-text-primary">
      <UtilityBar
        chapter="PRIVACY POLICY"
        actions={
          <Link
            to="/"
            className="text-xs font-medium text-muted-foreground underline underline-offset-4 decoration-muted-foreground/40 hover:text-foreground hover:decoration-foreground/60 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-foreground/30 rounded-sm"
          >
            ← 홈으로
          </Link>
        }
      />

      <main className="mx-auto max-w-3xl px-5 md:px-8 py-12">
        {/* 제목 */}
        <div className="mb-8">
          <h1 className="text-2xl font-extrabold tracking-tight text-text-primary mb-2">
            개인정보 처리방침
          </h1>
          <p className="text-sm text-text-tertiary">시행일자: 2026년 4월 14일</p>
        </div>

        {/* 베타 서비스 고지 */}
        <div className="mb-8 rounded-xl border border-amber-200 bg-amber-50 px-5 py-4">
          <p className="text-sm font-medium text-amber-800">
            본 서비스는 베타 서비스이며, 예기치 못한 오류로 일부 데이터 손실이 발생할 수 있습니다.
            중요한 데이터는 사용자가 별도 백업하시기 바랍니다.
          </p>
        </div>

        {/* 목차 */}
        <nav className="mb-10 rounded-xl border border-border bg-surface px-5 py-5">
          <p className="text-xs font-bold text-text-tertiary uppercase tracking-wider mb-3">목차</p>
          <ol className="space-y-1.5 text-sm">
            {[
              ['#general', '1. 총칙'],
              ['#items', '2. 수집 항목'],
              ['#purpose', '3. 수집 목적'],
              ['#retention', '4. 보관 기간'],
              ['#third-party', '5. 제3자 제공'],
              ['#delegation', '6. 처리 위탁'],
              ['#rights', '7. 이용자 권리'],
              ['#cookies', '8. 쿠키'],
              ['#security', '9. 안전성 조치'],
              ['#contact', '10. 문의처'],
            ].map(([href, label]) => (
              <li key={href}>
                <a
                  href={href}
                  className="text-text-secondary hover:text-primary transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-1 rounded-sm"
                >
                  {label}
                </a>
              </li>
            ))}
          </ol>
        </nav>

        {/* 본문 */}
        <div className="space-y-10 divide-y divide-border/50">

          <Section id="general" title="1. 총칙">
            <p>
              Rehearse(이하 "서비스")는 이용자의 개인정보를 소중히 여기며, 「개인정보 보호법」 등 관련 법령을 준수합니다.
              본 방침은 서비스가 수집하는 개인정보의 항목, 목적, 보관 기간, 이용자 권리 등을 설명합니다.
            </p>
            <p>
              본 서비스는 현재 베타 서비스로 운영 중이며, 개인정보 처리방침은 서비스 변경에 따라 업데이트될 수 있습니다.
              변경 시 시행일 7일 전 서비스 내 공지를 통해 안내합니다.
            </p>
          </Section>

          <Section id="items" title="2. 수집 항목">
            <p>서비스는 다음의 개인정보를 수집합니다.</p>
            <ul className="list-disc list-inside space-y-1 pl-2">
              <li>OAuth 로그인(Google/GitHub): 이메일 주소, 이름, 프로필 사진 URL</li>
              <li>이력서 텍스트 (면접 준비 시 직접 입력)</li>
              <li>면접 녹화 영상 (WebM 형식, S3 저장)</li>
              <li>STT 전사 텍스트 (음성 인식 결과)</li>
              <li>비언어 분석 결과 (표정·자세 분석 데이터)</li>
              <li>브라우저 세션 쿠키 (JWT 기반 인증 토큰)</li>
            </ul>
          </Section>

          <Section id="purpose" title="3. 수집 목적">
            <ul className="list-disc list-inside space-y-1 pl-2">
              <li>AI 모의면접 기능 제공 (이력서 기반 질문 생성, 면접 진행)</li>
              <li>면접 피드백 생성 (Claude AI를 통한 답변 분석 및 평가)</li>
              <li>STT 및 비언어 분석 결과 제공</li>
              <li>서비스 개선을 위한 통계 분석 (개인 식별 불가 집계 데이터만 활용)</li>
            </ul>
          </Section>

          <Section id="retention" title="4. 보관 기간">
            <ul className="list-disc list-inside space-y-1 pl-2">
              <li>
                <strong>원본 면접 영상:</strong> 업로드 후 30일 경과 시 S3 Lifecycle 정책에 의해 자동 삭제
              </li>
              <li>
                <strong>파생 분석 결과 및 AI 피드백:</strong> 회원 탈퇴 시까지 보관. 탈퇴 요청 후 30일 이내 완전 삭제
              </li>
              <li>
                <strong>법정 보관 의무 데이터:</strong> 관련 법령에서 정한 기간 동안 보관
              </li>
            </ul>
          </Section>

          <Section id="third-party" title="5. 제3자 제공">
            <p>
              서비스는 이용자의 개인정보를 제3자에게 판매하거나 공유하지 않습니다.
              법령에 의한 요청 또는 이용자의 명시적 동의가 있는 경우에 한해 제공될 수 있습니다.
            </p>
          </Section>

          <Section id="delegation" title="6. 처리 위탁">
            <p>서비스는 다음 업체에 개인정보 처리를 위탁합니다.</p>
            <div className="overflow-x-auto">
              <table className="w-full text-sm border border-border rounded-lg overflow-hidden mt-2">
                <thead className="bg-surface">
                  <tr>
                    <th className="text-left px-4 py-2.5 font-semibold text-text-primary border-b border-border">위탁업체</th>
                    <th className="text-left px-4 py-2.5 font-semibold text-text-primary border-b border-border">위탁 업무</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border/50">
                  <tr>
                    <td className="px-4 py-2.5">Amazon Web Services (AWS)</td>
                    <td className="px-4 py-2.5">영상·데이터 저장 (S3), 서버 운영 (EC2)</td>
                  </tr>
                  <tr>
                    <td className="px-4 py-2.5">Anthropic</td>
                    <td className="px-4 py-2.5">Claude API — 면접 질문 및 피드백 생성</td>
                  </tr>
                  <tr>
                    <td className="px-4 py-2.5">OpenAI</td>
                    <td className="px-4 py-2.5">Whisper STT (음성 전사), GPT-4o Vision (비언어 분석)</td>
                  </tr>
                  <tr>
                    <td className="px-4 py-2.5">Google Cloud</td>
                    <td className="px-4 py-2.5">Text-to-Speech (면접 질문 음성 변환)</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <p className="mt-3 text-xs text-text-tertiary">
              위 위탁업체들은 Rehearse가 제공한 데이터로 자체 AI 모델 학습을 수행하지 않습니다.
            </p>
          </Section>

          <Section id="rights" title="7. 이용자 권리">
            <p>이용자는 언제든지 다음의 권리를 행사할 수 있습니다.</p>
            <ul className="list-disc list-inside space-y-1 pl-2">
              <li>개인정보 열람 요청</li>
              <li>개인정보 정정·삭제 요청</li>
              <li>개인정보 처리 정지 요청</li>
              <li>회원 탈퇴 및 데이터 완전 삭제 요청</li>
            </ul>
            <p className="mt-2">
              권리 행사는 아래 문의처로 이메일 요청 시 확인 후 처리합니다. 법령에 따라 처리가 제한될 수 있습니다.
            </p>
          </Section>

          <Section id="cookies" title="8. 쿠키">
            <p>
              서비스는 로그인 상태 유지를 위해 JWT 기반 세션 쿠키를 사용합니다.
              해당 쿠키는 <code className="text-xs bg-surface px-1.5 py-0.5 rounded font-mono">HttpOnly</code>,{' '}
              <code className="text-xs bg-surface px-1.5 py-0.5 rounded font-mono">Secure</code>,{' '}
              <code className="text-xs bg-surface px-1.5 py-0.5 rounded font-mono">SameSite=Lax</code> 속성이
              적용되어 XSS·CSRF 위험을 최소화합니다.
            </p>
            <p>
              서비스는 광고 추적, 행동 분석, 제3자 마케팅 목적의 쿠키를 사용하지 않습니다.
            </p>
          </Section>

          <Section id="security" title="9. 안전성 조치">
            <ul className="list-disc list-inside space-y-1 pl-2">
              <li>HTTPS를 통한 전송 구간 암호화</li>
              <li>S3 SSE-S3를 통한 저장 데이터 암호화</li>
              <li>OAuth 2.0 기반 인증 (비밀번호 직접 저장 없음)</li>
              <li>최소 권한 원칙을 적용한 AWS IAM 정책</li>
              <li>원본 영상 30일 자동 삭제 (S3 Lifecycle)</li>
            </ul>
          </Section>

          <Section id="contact" title="10. 문의처">
            <p>개인정보 관련 문의, 열람·정정·삭제 요청은 아래로 연락해주세요.</p>
            <p className="mt-2">
              <strong>이메일:</strong>{' '}
              <a
                href="mailto:a01039261344@gmail.com"
                className="text-primary hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-1 rounded-sm"
              >
                a01039261344@gmail.com
              </a>
            </p>
          </Section>

        </div>

        {/* 하단 */}
        <div className="mt-12 pt-6 border-t border-border flex items-center justify-between gap-4">
          <p className="text-xs text-text-tertiary">
            &copy; 2026 Rehearse. All rights reserved.
          </p>
          <Link
            to="/"
            className="text-xs font-medium text-text-secondary transition-colors hover:text-text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 rounded-sm"
          >
            홈으로 돌아가기
          </Link>
        </div>
      </main>
    </div>
  )
}
