/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      fontFamily: {
        display: ['"DM Serif Display"', 'serif'],
        sans: ['"DM Sans"', 'sans-serif'],
        mono: ['"JetBrains Mono"', 'monospace'],
      },
      animation: {
        'fade-in': 'fadeIn 0.3s ease-in-out',
        'slide-up': 'slideUp 0.4s ease-out',
        'slide-in-right': 'slideInRight 0.3s ease-out',
        'pulse-slow': 'pulse 2s cubic-bezier(0.4,0,0.6,1) infinite',
        'spin-slow': 'spin 3s linear infinite',
      },
      keyframes: {
        fadeIn: { '0%': { opacity: 0 }, '100%': { opacity: 1 } },
        slideUp: { '0%': { opacity: 0, transform: 'translateY(20px)' }, '100%': { opacity: 1, transform: 'translateY(0)' } },
        slideInRight: { '0%': { opacity: 0, transform: 'translateX(20px)' }, '100%': { opacity: 1, transform: 'translateX(0)' } },
      },
      colors: {
        // Portal themes defined in prompt but used as arbitrary colors here for easy utility access
        admin: {
          primary: '#C53030',
          secondary: '#2D3748',
          accent: '#744210',
        },
        staff: {
          primary: '#2C7A7B',
          secondary: '#2D3748',
          accent: '#276749',
        },
        member: {
          primary: '#B7791F',
          secondary: '#FFFFFF',
          accent: '#744210',
          bg: '#FFFBEB',
        }
      }
    },
  },
  plugins: [],
}
