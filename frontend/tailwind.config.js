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
        background: '#FFFFFF',
        surface: '#F8FAFC',
        border: '#E2E8F0',
        text: {
          primary: '#0F172A',   // Deep Charcoal (Our Signature Text)
          secondary: '#475569',
          tertiary: '#94A3B8',
        },
        accent: {
          DEFAULT: '#6366F1',   // Electric Violet (Our Brand Signature)
          hover: '#4F46E5',
          light: '#EEF2FF',
        },
        success: { DEFAULT: '#10B981', light: '#ECFDF5' },
        warning: { DEFAULT: '#F59E0B', light: '#FFFBEB' },
        error: { DEFAULT: '#EF4444', light: '#FEF2F2' },
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
      },
      animation: {
        'fade-in': 'fade-in 0.3s ease-out',
        'progress-loading': 'progress-loading 1.5s ease-in-out infinite',
      },
    },
  },
  plugins: [],
}
