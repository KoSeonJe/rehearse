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
        background: '#FAFAFA',
        surface: '#FFFFFF',
        border: '#E8E8E8',
        text: {
          primary: '#191F28',
          secondary: '#6B7684',
          tertiary: '#AEB5BC',
        },
        accent: {
          DEFAULT: '#FF6B4A',
          hover: '#E5593B',
          light: '#FFF0ED',
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
      keyframes: {
        float: {
          '0%, 100%': { transform: 'translateY(0px)' },
          '50%': { transform: 'translateY(-6px)' },
        },
      },
      animation: {
        float: 'float 3s ease-in-out infinite',
      },
    },
  },
  plugins: [],
}
