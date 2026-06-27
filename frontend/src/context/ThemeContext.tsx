/**
 * ThemeContext.tsx - Global Theme Provider
 *
 * Provides dark/light mode toggle across the entire app.
 * Wrap <App /> with <ThemeProvider> in main.tsx.
 *
 * Usage anywhere in the app:
 *   const { theme, toggleTheme } = useTheme()
 *
 * Theme is persisted to localStorage so it survives page refresh.
 */

import { createContext, useContext, useState, useEffect, ReactNode } from 'react'

export type Theme = 'dark' | 'light'

interface ThemeContextValue {
  theme: Theme
  toggleTheme: () => void
}

const ThemeContext = createContext<ThemeContextValue>({
  theme: 'dark',
  toggleTheme: () => {},
})

export function ThemeProvider({ children }: { children: ReactNode }) {
  // Read saved preference from localStorage, default to 'dark'
  const [theme, setTheme] = useState<Theme>(() => {
    const saved = localStorage.getItem('8085-theme')
    return (saved === 'light' || saved === 'dark') ? saved : 'dark'
  })

  // Apply 'data-theme' attribute to <html> so CSS variables switch
  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme)
    localStorage.setItem('8085-theme', theme)
  }, [theme])

  const toggleTheme = () => setTheme(prev => prev === 'dark' ? 'light' : 'dark')

  return (
    <ThemeContext.Provider value={{ theme, toggleTheme }}>
      {children}
    </ThemeContext.Provider>
  )
}

/** Hook to consume theme anywhere in the component tree */
export function useTheme() {
  return useContext(ThemeContext)
}
