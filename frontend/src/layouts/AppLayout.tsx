import { Link, Outlet } from 'react-router'
import { AccountMenu } from '../components/AccountMenu'
import { ThemeToggle } from '../components/ThemeToggle'
import './AppLayout.css'

/** Moldura das telas fora de um projeto: cabecalho com logo, tema e menu da conta. */
export function AppLayout() {
  return (
    <div className="app">
      <header className="app-header">
        <Link to="/projetos" className="app-brand" aria-label="V&V Monitor, ir para projetos">
          <svg viewBox="0 0 24 24" width="24" height="24" aria-hidden="true" style={{ color: 'var(--ac)' }}>
            <circle cx="6" cy="17" r="3.2" fill="none" stroke="currentColor" strokeWidth="2" />
            <circle cx="18" cy="7" r="3.2" fill="currentColor" />
            <path d="M8.6 15.2 15.4 8.8" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
          </svg>
          <span>V&amp;V Monitor</span>
        </Link>
        <div style={{ flex: 1 }} />
        <ThemeToggle />
        <AccountMenu />
      </header>
      <Outlet />
    </div>
  )
}
