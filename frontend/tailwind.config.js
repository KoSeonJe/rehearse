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
        background: '#FAFAF8',
        surface: '#FFFFFF',
        'surface-warm': '#F7F5F2',
        border: '#E8E4DF',
        'border-subtle': '#F0EDE8',
        text: {
          primary: '#1A1A2E',
          secondary: '#5A5A6E',
          tertiary: '#9A9AAE',
        },
        accent: {
          DEFAULT: '#1A1A2E',
          hover: '#2A2A42',
          teal: '#2DD4A8',
          'teal-dark': '#1CAD8A',
          'teal-light': '#E8FBF5',
          coral: '#FF8A6C',
          'coral-light': '#FFF0EB',
        },
        success: { DEFAULT: '#2DD4A8', light: '#E8FBF5' },
        warning: { DEFAULT: '#FFBA6C', light: '#FFF8F0' },
        error: { DEFAULT: '#FF6B6B', light: '#FFF0F0' },
      },
      boxShadow: {
        soft: '0 2px 8px rgba(26, 26, 46, 0.04)',
        medium: '0 8px 24px rgba(26, 26, 46, 0.08)',
        strong: '0 16px 48px rgba(26, 26, 46, 0.12)',
        'glow-teal': '0 8px 32px rgba(45, 212, 168, 0.15)',
        'glow-navy': '0 8px 32px rgba(26, 26, 46, 0.20)',
      },
      borderRadius: {
        card: '20px',
        button: '24px',
        badge: '999px',
      },
      keyframes: {
        'fade-in': {
          '0%': { opacity: '0', transform: 'translateY(4px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        'fade-up': {
          '0%': { opacity: '0', transform: 'translateY(30px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        'fade-left': {
          '0%': { opacity: '0', transform: 'translateX(30px)' },
          '100%': { opacity: '1', transform: 'translateX(0)' },
        },
        'fade-right': {
          '0%': { opacity: '0', transform: 'translateX(-30px)' },
          '100%': { opacity: '1', transform: 'translateX(0)' },
        },
        'scale-in': {
          '0%': { opacity: '0', transform: 'scale(0.95)' },
          '100%': { opacity: '1', transform: 'scale(1)' },
        },
        float: {
          '0%, 100%': { transform: 'translateY(0)' },
          '50%': { transform: 'translateY(-8px)' },
        },
        'progress-loading': {
          '0%': { transform: 'translateX(-100%)' },
          '100%': { transform: 'translateX(400%)' },
        },
      },
      animation: {
        'fade-in': 'fade-in 0.3s ease-out',
        'fade-up': 'fade-up 0.6s ease-out',
        'fade-left': 'fade-left 0.6s ease-out',
        'fade-right': 'fade-right 0.6s ease-out',
        'scale-in': 'scale-in 0.5s ease-out',
        float: 'float 3s ease-in-out infinite',
        'progress-loading': 'progress-loading 1.5s ease-in-out infinite',
      },
    },
  },
  plugins: [],
}
