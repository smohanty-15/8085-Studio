/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      fontFamily: {
        mono: ['JetBrains Mono', 'Fira Code', 'Consolas', 'monospace'],
      },
      colors: {
        // Dark theme palette inspired by real oscilloscope screens
        bg: {
          primary:   '#0D1117',
          secondary: '#161B22',
          tertiary:  '#21262D',
          panel:     '#1C2128',
        },
        accent: {
          green:  '#39D353',  // classic terminal green
          yellow: '#F0C674',
          blue:   '#58A6FF',
          red:    '#FF6B6B',
          orange: '#FFA657',
        },
        border: {
          DEFAULT: '#30363D',
          bright:  '#484F58',
        },
        text: {
          primary:   '#E6EDF3',
          secondary: '#8B949E',
          muted:     '#484F58',
        }
      }
    },
  },
  plugins: [],
}
