import { createContext, useContext } from 'react'

export interface ThemeContextValue {
  dark: boolean
  toggle: () => void
}

export const ThemeContext = createContext<ThemeContextValue | null>(null)

export function useTheme(): ThemeContextValue {
  const context = useContext(ThemeContext)
  if (!context) throw new Error('useTheme deve ser usado dentro de <ThemeProvider>')
  return context
}
