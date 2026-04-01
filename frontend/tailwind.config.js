/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      fontFamily: {
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
        background: '#F1F5F9',   // Slate-100 — 페이지 배경 (연한 회색 틴트)
        surface: '#FFFFFF',      // 카드/패널 배경 (깨끗한 흰색)
        border: '#E2E8F0',       // Slate-200 — 명확한 경계선
        text: {
          primary: '#0F172A',   // Deep Charcoal (Our Signature Text)
          secondary: '#334155',
          tertiary: '#64748B',
        },
        accent: {
          DEFAULT: '#6366F1',   // Electric Violet (Our Brand Signature)
          hover: '#4F46E5',
          light: '#EEF2FF',
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
        card: '20px',    // Toss uses more rounded corners
        button: '24px',
        badge: '999px',
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
      },
      animation: {
        'fade-in': 'fade-in 0.3s ease-out',
        'progress-loading': 'progress-loading 1.5s ease-in-out infinite',
        'glow-pulse': 'glow-pulse 3s ease-in-out infinite',
        'rec-pulse': 'rec-pulse 2s ease-in-out infinite',
        'ripple': 'ripple 2s ease-out infinite',
      },
    },
  },
  plugins: [],
}
