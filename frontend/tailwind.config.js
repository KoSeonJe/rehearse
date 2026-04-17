/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      fontFamily: {
        display: [
          'Cal Sans',
          'Pretendard Variable',
          'Pretendard',
          '-apple-system',
          'BlinkMacSystemFont',
          'system-ui',
          'sans-serif',
        ],
        sans: [
          'Pretendard Variable',
          'Pretendard',
          '-apple-system',
          'BlinkMacSystemFont',
          'system-ui',
          'sans-serif',
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

        /* 기존 토큰 유지 (Phase 3에서 개별 처리) */
        surface: '#FFFFFF',
        text: {
          primary: '#0F172A',
          secondary: '#334155',
          tertiary: '#64748B',
        },
        success: { DEFAULT: '#10B981', light: '#ECFDF5' },
        warning: { DEFAULT: '#F59E0B', light: '#FFFBEB' },
        error: { DEFAULT: '#EF4444', light: '#FEF2F2' },
        studio: {
          bg: '#202124',
          surface: '#2c2c2c',
          'surface-elevated': '#3c4043',
          border: '#3c4043',
          text: '#e8eaed',
          'text-secondary': '#9aa0a6',
        },
        meet: {
          green: '#00AC47',
          red: '#EA4335',
        },
      },
      boxShadow: {
        'toss': '0 8px 16px 0 rgba(0, 0, 0, 0.04)',
        'toss-lg': '0 16px 32px 0 rgba(0, 0, 0, 0.08)',
      },
      borderRadius: {
        lg: 'var(--radius)',
        md: 'calc(var(--radius) - 2px)',
        sm: 'calc(var(--radius) - 4px)',
        /* 시맨틱 radius 스케일 */
        card: '12px',
        badge: '999px',
        /* arbitrary 대체 토큰 */
        '2xl': '20px',
        '3xl': '24px',
        '4xl': '32px',
      },
      keyframes: {
        'fade-in': {
          '0%': { opacity: '0', transform: 'translateY(4px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        'progress-loading': {
          '0%': { transform: 'translateX(-100%)' },
          '100%': { transform: 'translateX(400%)' },
        },
        'glow-pulse': {
          '0%, 100%': { opacity: '0.4' },
          '50%': { opacity: '0.8' },
        },
        'rec-pulse': {
          '0%, 100%': { boxShadow: '0 0 0 0 rgba(239, 68, 68, 0)' },
          '50%': { boxShadow: '0 0 20px 4px rgba(239, 68, 68, 0.3)' },
        },
        'ripple': {
          '0%': { transform: 'scale(1)', opacity: '0.6' },
          '100%': { transform: 'scale(1.8)', opacity: '0' },
        },
        'bookmark-pop': {
          '0%': { transform: 'scale(1)' },
          '40%': { transform: 'scale(1.15)' },
          '100%': { transform: 'scale(1)' },
        },
        'toast-slide-in': {
          from: { transform: 'translateY(12px)', opacity: '0' },
          to: { transform: 'translateY(0)', opacity: '1' },
        },
        'tutorial-ring': {
          '0%, 100%': { boxShadow: '0 0 0 0 rgba(36, 36, 36, 0.22)' },
          '50%': { boxShadow: '0 0 0 5px rgba(36, 36, 36, 0)' },
        },
        'tutorial-nudge': {
          '0%, 100%': { transform: 'translateY(0)' },
          '50%': { transform: 'translateY(3px)' },
        },
      },
      animation: {
        'fade-in': 'fade-in 0.3s ease-out',
        'progress-loading': 'progress-loading 1.5s ease-in-out infinite',
        'glow-pulse': 'glow-pulse 3s ease-in-out infinite',
        'rec-pulse': 'rec-pulse 2s ease-in-out infinite',
        'ripple': 'ripple 2s ease-out infinite',
        'bookmark-pop': 'bookmark-pop 0.18s ease-out',
        'toast-slide-in': 'toast-slide-in 0.2s ease-out',
        'tutorial-ring': 'tutorial-ring 3s ease-in-out infinite',
        'tutorial-nudge': 'tutorial-nudge 2s ease-in-out infinite',
      },
    },
  },
  plugins: [require('tailwindcss-animate')],
}
