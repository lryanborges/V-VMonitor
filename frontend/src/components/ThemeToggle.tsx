import { useTheme } from '../theme/ThemeContext'
import { MoonIcon, SunIcon } from './icons'

export function ThemeToggle() {
  const { dark, toggle } = useTheme()
  return (
    <button
      className="btn ic g"
      type="button"
      onClick={toggle}
      aria-label="Alternar modo claro e escuro"
      title={dark ? 'Usar tema claro' : 'Usar tema escuro'}
    >
      {dark ? <SunIcon /> : <MoonIcon />}
    </button>
  )
}
