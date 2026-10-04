import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { ThemeContext } from './ThemeContext'

const THEME_KEY = 'vv-theme'

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [dark, setDark] = useState(() => localStorage.getItem(THEME_KEY) === 'dark')

  // os tokens de cor ficam em .vv / .vv.dark no <html>
  useEffect(() => {
    document.documentElement.classList.toggle('dark', dark)
    localStorage.setItem(THEME_KEY, dark ? 'dark' : 'light')
  }, [dark])

  const toggle = useCallback(() => setDark((d) => !d), [])
  const value = useMemo(() => ({ dark, toggle }), [dark, toggle])
  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}
