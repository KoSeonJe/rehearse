/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      fontFamily: {
        /* ─── Quiet Rigor 타이포 시스템 (3 폰트 확정 — Cal Sans 제거) ─── */
        /* 의사결정 근거: Cal Sans는 Cal.com 정체성이지 Rehearse 정체성이 아니다.
           Fraunces를 영문 display의 단일 폰트로 승격해 editorial signature를 확보하고,
           Pretendard 굵기·자간 변주만으로 한글 위계를 구성한다. */
        sans: [
          'Pretendard Variable',
          'Pretendard',
          '-apple-system',
          'BlinkMacSystemFont',
          'system-ui',
          'sans-serif',
        ],
        /* serif: Fraunces — 영문 headline + 모든 숫자 마커의 단일 display */
        serif: [
          'Fraunces',
          'Georgia',
          'serif',
        ],
        /* display: Fraunces로 통일 (하위 호환 alias — font-display 잔존 surface 대비) */
        display: [
          'Fraunces',
          'Pretendard Variable',
          'Pretendard',
          'Georgia',
          'serif',
        ],
        mono: ['JetBrains Mono', 'Consolas', 'monospace'],
      },
      colors: {
        /* shadcn CSS 변수 참조 토큰 */
        background: 'hsl(var(--background))',
        foreground: 'hsl(var(--foreground))',
        card: {
          DEFAULT: 'hsl(var(--card))',
          foreground: 'hsl(var(--card-foreground))',
        },
        popover: {
          DEFAULT: 'hsl(var(--popover))',
          foreground: 'hsl(var(--popover-foreground))',
        },
        primary: {
          DEFAULT: 'hsl(var(--primary))',
          foreground: 'hsl(var(--primary-foreground))',
        },
        secondary: {
          DEFAULT: 'hsl(var(--secondary))',
          foreground: 'hsl(var(--secondary-foreground))',
        },
        muted: {
          DEFAULT: 'hsl(var(--muted))',
          foreground: 'hsl(var(--muted-foreground))',
        },
        accent: {
          DEFAULT: 'hsl(var(--accent))',
          foreground: 'hsl(var(--accent-foreground))',
        },
        destructive: {
          DEFAULT: 'hsl(var(--destructive))',
          foreground: 'hsl(var(--destructive-foreground))',
        },
        border: 'hsl(var(--border))',
        input: 'hsl(var(--input))',
        ring: 'hsl(var(--ring))',

        /* ─── Brand Point Color (2026-04-18 신규) ─── */
        /* Teal — 전역 시그니처. CTA, link, focus, selected, active 상태 */
        brand: {
          DEFAULT:    'hsl(var(--brand))',
          hover:      'hsl(var(--brand-hover))',
          bg:         'hsl(var(--brand-bg))',
          foreground: 'hsl(var(--brand-foreground))',
        },

        /* ─── Semantic 토큰 (Phase A 신규 — P0-1 BLOCKER) ─── */
        /* Tailwind 유틸리티 클래스 생성: bg-accent-editorial, text-signal-record 등 */
        'accent-editorial':    'hsl(var(--accent-editorial))',
        'accent-editorial-bg': 'hsl(var(--accent-editorial-bg))',
        'signal-record':       'hsl(var(--signal-record))',
        'signal-record-bg':    'hsl(var(--signal-record-bg))',
        'signal-warning':      'hsl(var(--signal-warning))',
        'signal-warning-bg':   'hsl(var(--signal-warning-bg))',
        'signal-success':      'hsl(var(--signal-success))',
        'signal-success-bg':   'hsl(var(--signal-success-bg))',
        /* interview-page 전용 배경 (P1-8) */
        'interview-stage':     'hsl(var(--interview-stage))',

        /* 기존 토큰 — teal neutral로 전환 (2026-04-18 warm cream 제거) */
        surface: '#FFFFFF',
        text: {
          primary: '#042f2e',   /* was #0F172A (slate-900) → teal-950 */
          secondary: '#164e4c', /* was #334155 (slate-700) → teal-800 */
          tertiary: '#5a7574',  /* was #64748B (slate-500) → teal-gray, matches muted-foreground */
        },
        success: { DEFAULT: '#10B981', light: '#ECFDF5' },
        warning: { DEFAULT: '#F59E0B', light: '#FFFBEB' },
        error: { DEFAULT: '#EF4444', light: '#FEF2F2' },
        /* studio / meet 블록 제거 (Phase C) — interview-page는 interview-stage + semantic 토큰 사용 */
      },
      boxShadow: {
        /* ─── 5단계 섀도우 — teal-tinted (2026-04-18 warm rgba 제거) ─── */
        /* 모든 shadow는 teal-950 rgba 기반 — 순백 배경 위에서 쿨톤 일관 유지 */
        'toss':    '0 8px 16px 0 rgba(4,47,46,0.05)',
        'toss-lg': '0 16px 32px 0 rgba(4,47,46,0.08)',
        'xs':  '0 1px 2px rgba(4,47,46,0.05)',
        'sm':  '0 1px 5px -2px rgba(4,47,46,0.08), 0 0 0 1px rgba(4,47,46,0.04)',
        'md':  '0 4px 12px -4px rgba(4,47,46,0.10), 0 0 0 1px rgba(4,47,46,0.06)',
        'lg':  '0 8px 24px -6px rgba(4,47,46,0.14), 0 0 0 1px rgba(4,47,46,0.08)',
      },
      borderRadius: {
        lg: 'var(--radius)',
        md: 'calc(var(--radius) - 2px)',
        sm: 'calc(var(--radius) - 4px)',
        /* ─── Quiet Rigor 차등 radius 스케일 (Phase A — §3.4) ─── */
        'xs':   '4px',   /* 인라인 badge, 작은 pill */
        /* sm: 8px — 기존 sm 재활용 */
        /* md: 12px — 기존 md 재활용 */
        'radius-lg':  '16px',  /* Sheet, drawer */
        'radius-xl':  '24px',  /* Modal, video-dock */
        'pill': '9999px',      /* Tag pill, 상태 indicator */
        /* 기존 시맨틱 토큰 유지 */
        card: '12px',
        badge: '999px',
        '2xl': '20px',
        '3xl': '24px',
        '4xl': '32px',
      },
      /* ─── 12-col grid + 1440px canvas (Phase A — §4.1) ─── */
      gridTemplateColumns: {
        '12': 'repeat(12, minmax(0, 1fr))',
      },
      maxWidth: {
        'canvas': '1440px',
      },
      keyframes: {
        /* fade-in: 8px→0 스크롤 진입 애니메이션 (ChapterMarker 용) */
        'fade-in': {
          from: { opacity: '0', transform: 'translateY(8px)' },
          to:   { opacity: '1', transform: 'translateY(0)' },
        },
        'progress-loading': {
          '0%': { transform: 'translateX(-100%)' },
          '100%': { transform: 'translateX(400%)' },
        },
        'bookmark-pop': {
          '0%':   { transform: 'scale(1)' },
          '40%':  { transform: 'scale(1.15)' },
          '100%': { transform: 'scale(1)' },
        },
        'toast-slide-in': {
          from: { transform: 'translateY(12px)', opacity: '0' },
          to:   { transform: 'translateY(0)',    opacity: '1' },
        },
        /* rec-pulse / ripple / tutorial-ring / tutorial-nudge — Phase C 제거 완료 */
        /* 모든 사용처 교체됨:
           - interview-page.tsx → RecLabel (opacity transition)
           - interviewer-avatar.tsx → border + fade-in alternate
           - review-coach-mark.tsx → static boxShadow ring */
      },
      animation: {
        'fade-in':         'fade-in 0.3s ease-out',
        'progress-loading':'progress-loading 1.5s ease-in-out infinite',
        'bookmark-pop':    'bookmark-pop 0.18s ease-out',
        'toast-slide-in':  'toast-slide-in 0.2s ease-out',
        /* rec-pulse / tutorial-ring / tutorial-nudge — Phase C 제거 완료 */
      },
    },
  },
  plugins: [require('tailwindcss-animate')],
}
