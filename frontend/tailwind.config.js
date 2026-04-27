/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        bg: '#0a0e14',
        surface: '#131923',
        surface2: '#1a2332',
        border: '#1e2a3a',
        accent: {
          red: '#ff3b4e',
          orange: '#ff8c2e',
          green: '#2ecc71',
          blue: '#3b82f6',
          yellow: '#fbbf24',
        },
      },
      fontFamily: {
        sans: ['Nunito', 'sans-serif'],
        mono: ['JetBrains Mono', 'monospace'],
      },
    },
  },
  plugins: [],
};
