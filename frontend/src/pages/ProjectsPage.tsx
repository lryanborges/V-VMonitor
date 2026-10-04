import { useAuth } from '../auth/AuthContext'
import { Logo } from '../components/Logo'
import { ThemeToggle } from '../components/ThemeToggle'
import { LogoutIcon } from '../components/icons'

/** Provisoria: a listagem de projetos (RF3) entra quando o backend de projetos estiver pronto. */
export function ProjectsPage() {
  const { user, signOut } = useAuth()

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <header
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '12px 24px',
          borderBottom: '1px solid var(--bd)',
          background: 'var(--sf)',
        }}
      >
        <Logo />
        <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
          <ThemeToggle />
          <button className="btn g" type="button" onClick={signOut}>
            <LogoutIcon />
            Sair
          </button>
        </div>
      </header>

      <main style={{ flex: 1, padding: '40px 48px', maxWidth: 1200 }}>
        <h1 style={{ margin: 0, fontSize: 28, letterSpacing: '-0.02em' }}>Projetos</h1>
        <p style={{ margin: '6px 0 0', color: 'var(--mt)' }}>
          Olá, {user?.name}. A listagem de projetos chega na próxima etapa do backend.
        </p>
      </main>
    </div>
  )
}
