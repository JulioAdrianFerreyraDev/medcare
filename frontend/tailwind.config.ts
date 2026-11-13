import type { Config } from 'tailwindcss'

export default <Partial<Config>>{
  content: [
    './components/**/*.{vue,js,ts}',
    './layouts/**/*.vue',
    './pages/**/*.vue',
    './composables/**/*.{js,ts}',
    './plugins/**/*.{js,ts}',
    './app.vue',
    './error.vue'
  ],
  theme: {
    extend: {
      colors: {
        medical: {
          primary: '#0284c7',
          emerald: '#059669',
          amber: '#d97706',
          rose: '#e11d48'
        }
      }
    }
  },
  plugins: []
}
