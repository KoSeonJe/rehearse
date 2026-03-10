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
        background: '#F7F8FA',
        surface: '#FFFFFF',
        border: '#E5E8EB',
        text: {
          primary: '#191F28',
          secondary: '#6B7684',
          tertiary: '#AEB5BC',
        },
        accent: {
          DEFAULT: '#191F28',
          hover: '#333D4B',
          light: '#F2F4F6',
        },
        success: { DEFAULT: '#00C48C', light: '#E8FAF4' },
        warning: { DEFAULT: '#FFB84D', light: '#FFF6E5' },
        error: { DEFAULT: '#F04452', light: '#FFF0F1' },
        info: { DEFAULT: '#3182F6', light: '#EBF4FF' },
      },
      borderRadius: {
        card: '12px',
        button: '8px',
        badge: '999px',
      },
      keyframes: {},
      animation: {},
    },
  },
  plugins: [],
}
